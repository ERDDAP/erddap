/* 
 * SSR Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.ByteArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.String2Log;
import com.cohort.util.String2LogFactory;
import com.cohort.util.Test;

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

//these need access to the mail.jar (from JavaMail api; I'm using 1.5.1 (Nov 2013, latest as of June 2014))
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;


//directy use hdf libraries:
//import ncsa.hdf.object.*;     // the common object package
//import ncsa.hdf.object.h4.*;  // the HDF4 implementation

// 2014-08-05 J2SSH DEACTIVATED BECAUSE NOT USED. IF NEEDED, SWITCH TO Apache commons-net???
//for sftp
//import com.sshtools.j2ssh.SshClient;
//import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
//import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
//import com.sshtools.j2ssh.io.UnsignedInteger32;
//import com.sshtools.j2ssh.session.SessionChannelClient;
//import com.sshtools.j2ssh.sftp.FileAttributes;
//import com.sshtools.j2ssh.sftp.SftpFile;
//import com.sshtools.j2ssh.sftp.SftpFileOutputStream;
//import com.sshtools.j2ssh.SftpClient;
//import com.sshtools.j2ssh.configuration.ConfigurationLoader;

/**
 * This Shell Script Replacement class has static methods to facilitate 
 * using Java programs in place of Unix Shell Scripts.
 * The parameters often follow the TCSH and Unix conventions, notably:
 * counting most things as 1,2,3... (not 0,1,2...).
 *
 * <p>In general, methods assume they will be successful and throw 
 * an Exception if not. So, if error occurs, it will be obvious.
 *
 * <p>Goals for SSR and for converting CoastWatch shell scripts to Java:
 * <ul>
 * <li>Make the programs easier to work with from Java programs,
 *     notably web applications.
 * <li>Make the Java programs readable to people familiar with the 
 *     shell scripts.
 * <li>To make it easy to port from shell scripts to Java programs.
 * <li>Do more error checking. The scripts do minimal checking and
 *     sometimes may generate incorrect or blank graphs without notifying us.
 *     This lets us know when a problem occurs.
 * <li>Generate descriptive error messages to help us find
 *     and fix the problems. 
 * <li>Make the Java programs plug-in replacements for the scripts.
 *     To this end, each script is replaced by one Java program.
 *     The Java programs can be called from the command line
 *     (via mini-script files) with the same parameters.
 * <li>Add features to facilitate testing.
 *     Notably, the ability to specify different directories from the 
 *     command line.
 *     Also, the creation of a test file for each script so that 
 *     one or all of the scripts can be tested in an automated way.
 * <li>Generation of statistics. E.g., how many graphs were created.
 *     Again, to facilitate monitoring the system to watch for trouble.
 *     (e.g., usually x files are generated, but x-320 were generated this time)
 * </ul>
 *
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-02-10
 *
 * <p>Changes:
 * <ul>
 * </ul>
 */
public class SSR {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean reallyVerbose = false;
    public static boolean debugMode = false;

    private static String contextDirectory; //lazy creation by getContextDirectory
    private static String tempDirectory; //lazy creation by getTempDirectory

    /**
     * Returns a String which is a substring of the current string.
     * This checks for and deals with bad first and last values.
     *
     * @param s the string
     * @param first the first character to be extracted (1..)
     * @param last the last character to be extracted (1..)
     * @return the extracted String (or "")
     */
    public static String cutChar(String s, int first, int last) {
        int size = s.length();

        if (first < 1)
            first = 1;
        if (last > size)
            last = size;
        return first > last? "" : s.substring(first -1, last); //last is exclusive
    }

    /**
     * Returns a String which is a substring at the end of the current string, 
     * starting at <tt>first</tt>.
     * This checks for and deals with a bad first values.
     *
     * @param s the string
     * @param first the first character to be extracted (1..)
     * @return the extracted String (or "")
     */
    public static String cutChar(String s, int first) {
        return cutChar(s, first, s.length());
    }

    /**
     * This finds the first line in the specified text file which startsWith 
     * 'start'.
     * 
     * @param fileName the name of the text file
     * @param start the text which must be at the beginning of the line
     * @return the first line in the specified text file which startsWith 
     *     'start' (or null if none found).
     * @throws IOException if error opening, reading, or closing the file
     */
    public static String getFirstLineStartsWith(String fileName, String start) 
            throws java.io.IOException {
        BufferedReader bufferedReader = 
            new BufferedReader(new FileReader(fileName));       
        String s;
        while ((s = bufferedReader.readLine()) != null) { //null = end-of-file
            //String2.log(s);
            if (s.startsWith(start)) {
                bufferedReader.close();
                return s;
            }
        }
        bufferedReader.close();
        return null;
    }

    /**
     * This finds the first line in the specified text file which matches
     * 'regex'.
     * 
     * @param fileName the name of the text file
     * @param regex the regex text to be matched
     * @return the first line in the specified text file which matches regex
     *     (or null if none found).
     * @throws IOException if error opening, reading, or closing the file
     */
    public static String getFirstLineMatching(String fileName, String regex) 
            throws java.io.IOException {
        BufferedReader bufferedReader = 
            new BufferedReader(new FileReader(fileName));       
        String s;
        Pattern pattern = Pattern.compile(regex);
        while ((s = bufferedReader.readLine()) != null) { //null = end-of-file
            //String2.log(s);
            if (pattern.matcher(s).matches()) {
                bufferedReader.close();
                return s;
            }
        }
        bufferedReader.close();
        return null;
    }

    /**
     * This runs the specified command with dosShell (if String2.OSISWindows)
     * or cShell (otherwise).
     *
     * @param commandLine  with file names specified with forward slashes
     * @param timeOutSeconds (use 0 for no timeout)
     * @return an ArrayList of Strings with the output from the program 
     *   (or null if there is a fatal error)
     * @throws Exception if exitStatus of cmd is not 0 (or other fatal error) 
     */
    public static ArrayList dosOrCShell(String commandLine, int timeOutSeconds) throws Exception {
        if (String2.OSIsWindows) {
            commandLine = String2.replaceAll(commandLine, "/", "\\");
            return dosShell(commandLine, timeOutSeconds);  
        } else {
            return cShell(  commandLine, timeOutSeconds);  
        }
    }

    /**
     * This is a variant of shell() for DOS command lines.
     *
     * @param commandLine the command line to be executed (for example, "myprogram <filename>")
     *    with backslashes
     * @param timeOutSeconds (use 0 for no timeout)
     * @return an ArrayList of Strings with the output from the program 
     *   (or null if there is a fatal error)
     * @throws Exception if exitStatus of cmd is not 0 (or other fatal error) 
     * @see #shell
     */
    public static ArrayList dosShell(String commandLine, int timeOutSeconds) throws Exception {
        if (verbose) String2.log("dosShell        in: " + commandLine);
        PipeToStringArray outCatcher = new PipeToStringArray();
        PipeToStringArray errCatcher = new PipeToStringArray();
        
//        int exitValue = shell(String2.tokenize("cmd.exe /C " + commandLine), 
        int exitValue = shell(new String[]{"cmd.exe", "/C", commandLine}, 
            outCatcher, errCatcher, timeOutSeconds);

        //collect and print results (or throw exception)
        String err = errCatcher.getString();
        if (verbose || err.length() > 0 || exitValue != 0) {
            String s = 
                "dosShell       cmd: " + commandLine + "\n" +
                "dosShell exitValue: " + exitValue   + "\n" +
                "dosShell       err: " + err         + (err.length() > 0? "" : "\n") +
                "dosShell       out: " + outCatcher.getString();
            if (exitValue == 0)
                String2.log(s);
            else 
                throw new Exception(
                    String2.ERROR + " in SSR.dosShell:\n" + s);
        }
        return outCatcher.getArrayList();
    }


    /**
     * This is a variant of shell() for cShell command lines.
     *
     * @param commandLine the command line to be executed (for example, "myprogram <filename>")
     * @param timeOutSeconds (use 0 for no timeout)
     * @return an ArrayList of Strings with the output from the program 
     *   (or null if there is a fatal error)
     * @throws Exception if exitStatus of cmd is not 0 (or other fatal error) 
     * @see #shell
     */
    public static ArrayList cShell(String commandLine, int timeOutSeconds) throws Exception {
        if (verbose) String2.log("cShell        in: " + commandLine);
        PipeToStringArray outCatcher = new PipeToStringArray();
        PipeToStringArray errCatcher = new PipeToStringArray();
        
        int exitValue = shell(new String[]{"/bin/csh", "-c", commandLine}, 
            outCatcher, errCatcher, timeOutSeconds);

        //collect and print results (or throw exception)
        String err = errCatcher.getString();
        if (verbose || err.length() > 0 || exitValue != 0) {
            String s = 
                "cShell       cmd: " + commandLine + "\n" +
                "cShell exitValue: " + exitValue   + "\n" +
                "cShell       err: " + err         + (err.length() > 0? "" : "\n");// +
//                "cShell       out: " + outCatcher.getString();
            if (exitValue == 0)
                String2.log(s);
            else 
                throw new Exception(
                    String2.ERROR + " in SSR.cShell:\n" + s);
        }
        return outCatcher.getArrayList();
    }

    /**
     * This is a variant of shell() for cShell command lines.
     *
     * @param commandLine the command line to be executed (for example, "myprogram <filename>")
     * @param outStream an outputStream to capture the results.
     *    This does not close the outStream afterwards.
     *    (Use "null" if you don't want to capture out.)
     * @param errStream an outputStream to capture the error.
     *    This does not close the errStream afterwards.
     *    (Use "null" if you don't want to capture err.)
     * @param timeOutSeconds (use 0 for no timeout)
     * @return the exitValue
     * @throws Exception but unlike the other shell commands, this 
     *     doesn't throw an exception just because exitValue != 0.
     * @see #shell
     */
    public static int cShell(String commandLine, OutputStream outStream, 
            OutputStream errStream, int timeOutSeconds) throws Exception {
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

        //call shell() 
        int exitValue = shell(new String[]{"/bin/csh", "-c", commandLine}, 
                outCatcher, errCatcher, timeOutSeconds);

        //if I created the streams, close them
        if (outBAOS != null)
            outBAOS.close();
        if (errBAOS != null)
            errBAOS.close();

        //collect and print results (or throw exception)
        String err = errBAOS == null? "" : errBAOS.toString();
        if (verbose || err.length() > 0 || exitValue != 0) {
            String2.log( 
                "cShell       cmd: " + commandLine + "\n" +
                "cShell exitValue: " + exitValue   + "\n" +
                "cShell       err: " + (errBAOS == null? "[unknown]" : err));
        }

        return exitValue;
    }

    /* *
     * This is a variant of shell() for calling programs directly.
     *
     * @param commandLine the command line to be executed (for example, "myprogram <filename>")
     * @return an ArrayList of Strings with the output from the program 
     *   (or null if there is a fatal error)
     * @throws Exception if exitStatus of cmd is not 0 (or other fatal error) 
     * @see shell
     */
/*    public static ArrayList cShell(String commandLine[]) throws Exception {
        if (verbose) 
            String2.log.fine("cShell        in: " + String2.toCSSVString(commandLine));
        String[] commandLine2 = new String[commandLine.length + 2];
        commandLine2[0] = "/bin/csh";
        commandLine2[1] = "-c";
        System.arraycopy(commandLine, 0, commandLine2, 2, commandLine.length);
        return shell("cShell", commandLine2);
    }
*/
    /**
     * This runs and waits for the specified cShell command line (including 
     * parameters, pipes, and other redirection (e.g., >>).
     * This is highly OS dependent -- so it is better to not call this.
     * 
     * <p>If you want to ignore exitValue!=0, try/catch the call to cShell.
     *
     * @param cmd the command to be executed (for example, 
     *    new String[]{"rm", "-F", "<filename>"}).
     *  If this is a system command, the first two elements should be 
     *     "/bin/csh", "-c" (for cShell),
     *     "/bin/sh", "-c" ((?)for bash),
     *     "cmd.exe", "/C" (for recent Windows),
     *     "command.com", "/C" (if osName.equals("Windows 95")).
     * @param outPipe a PipeTo to catch stdOut from the process
     * @param errPipe a PipeTo to catch stdErr from the process
     * @param timeOutSeconds (use -1 for no time out)
     * @return the exitValue (Integer.MAX_VALUE indicates time out)
     * @throws Exception  
     */
    public static int shell(String cmd[], PipeTo outPipe, PipeTo errPipe,
        int timeOutSeconds)  throws Exception {
        
        //execute the command
        //why need separate /bin/sh and -c? 
        //  so that params are passed to the cmd program and not to /bin/csh
        //  see http://www.mountainstorm.com/publications/javazine.html
        //what does -c do?  see Linux In a Nutshell pg 623 for a weak explanation
        if (verbose) String2.log("SSR.shell cmd[]=\n" + String2.toNewlineString(cmd));
        Process process = Runtime.getRuntime().exec(cmd);   

        //capture the output
        outPipe.setInputStream(process.getInputStream());
        errPipe.setInputStream(process.getErrorStream());
        outPipe.start(); //start the threads
        errPipe.start();
                                    
        //wait till done
        int exitValue = Integer.MAX_VALUE; // = process.waitFor();
        boolean done = false;
        long time = 0;
        long timeOutMillis = timeOutSeconds * 1000L;
        while (!done) {
            try {
                exitValue = process.exitValue(); //throws exception if process not done
                done = true;
            } catch (Exception e) {
                if (timeOutSeconds > 0 && time >= timeOutMillis) {
                    done = true;
                    process.destroy();
                }
                Math2.sleep(60); //give the process some time to work before trying again
                time += 60;
            }
        }

        //force stream closure (needed to generate bufferedReader read null)
        process.getInputStream().close();
        if (timeOutSeconds > 0 && time >= timeOutMillis) 
            errPipe.print(
                String2.ERROR + ": shell command (" + String2.toCSSVString(cmd) + ") timed out (" + timeOutSeconds + " s).\n");
        process.getErrorStream().close();

        return exitValue;
    }

    /**
     * Put the specified files in a zip file (without directory info).
     * See  http://javaalmanac.com/egs/java.util.zip/CreateZip.html .
     * If a file named zipDirName already exists, it is overwritten.
     * 
     * @param zipDirName the full name for the .zip file (path + name + ".zip")
     * @param dirNames the full names of the files to be put in the zip file.
     *    These can use forward or backslashes as directory separators.
     * @param timeOutSeconds (use -1 for no time out)
     * @throws Exception if trouble
     */
    public static void zip(String zipDirName, String dirNames[], 
        int timeOutSeconds) throws Exception {

        zip(zipDirName, dirNames, timeOutSeconds, false, ""); 
    }

    /**
     * Put the specified files in a zip file (with some directory info).
     * See  http://javaalmanac.com/egs/java.util.zip/CreateZip.html .
     * If a file named zipDirName already exists, it is overwritten.
     * 
     * @param zipDirName the full name for the .zip file (path + name + ".zip")
     * @param dirNames the full names of the files to be put in the zip file.
     *    These can use forward or backslashes as directory separators.
     * @param timeOutSeconds (use -1 for no time out)
     * @param removeDirPrefix the prefix to be removed from the start of 
     *    each dir name (ending with a slash)
     * @throws Exception if trouble
     */
    public static void zip(String zipDirName, String dirNames[], 
        int timeOutSeconds, String removeDirPrefix) throws Exception {

        zip(zipDirName, dirNames, timeOutSeconds, true, removeDirPrefix);
    }

    /**
     * Put the specified files in a zip file (without directory info).
     * See  http://javaalmanac.com/egs/java.util.zip/CreateZip.html .
     * If a file named zipDirName already exists, it is overwritten.
     * 
     * @param zipDirName the full name for the .zip file (path + name + ".zip")
     * @param dirNames the full names of the files to be put in the zip file.
     *    These can use forward or backslashes as directory separators.
     * @param timeOutSeconds (use -1 for no time out)
     * @param includeDirectoryInfo set this to false if you don't want
     *   any dir invo stored with the files
     * @param removeDirPrefix if includeDirectoryInfo is true, 
     *    this is the prefix to be removed from the start of 
     *    each dir name (ending with a slash).
     *    If includeDirectoryInfo is false, this is removed.
     * @throws Exception if trouble
     */
    private static void zip(String zipDirName, String dirNames[], 
        int timeOutSeconds, boolean includeDirectoryInfo, String removeDirPrefix) 
        throws Exception {

        //validate
        long tTime = System.currentTimeMillis();
        if (includeDirectoryInfo) {
            //ensure slash at end of removeDirPrefix
            if ("\\/".indexOf(removeDirPrefix.charAt(removeDirPrefix.length() - 1)) < 0)
                throw new IllegalArgumentException(String2.ERROR + 
                    " in SSR.zip: removeDirPrefix must end with a slash.");

            //ensure dirNames start with removeDirPrefix
            for (int i = 0; i < dirNames.length; i++)
                if (!dirNames[i].startsWith(removeDirPrefix))
                    throw new IllegalArgumentException(String2.ERROR + " in SSR.zip: dirName[" + 
                        i + "] doesn't start with " + removeDirPrefix + ".");
        }

        //if Linux, it is faster to use the zip utility
        //I don't know how to include just partial dir info with Linux,
        //  since I can't cd to that directory.
        if (String2.OSIsLinux && !includeDirectoryInfo) {
            //-j: don't include dir info
            if (verbose) String2.log("Using Linux's zip to make " + zipDirName);
            File2.delete(zipDirName); //delete any exiting .zip file of that name
            cShell("zip -j " + zipDirName + " " + String2.toSSVString(dirNames), 
                timeOutSeconds); 
            if (verbose) String2.log("  zip done. TIME=" + 
                (System.currentTimeMillis() - tTime) + "\n");
            return;
        }

        //for all other operating systems...
        if (verbose) String2.log("Using Java's zip to make " + zipDirName);
        //create the ZIP file
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipDirName));
    
        //create a buffer for reading the files
        byte[] buf = new byte[4096];
    
        //compress the files
        for (int i = 0; i < dirNames.length; i++) {
            FileInputStream in = new FileInputStream(dirNames[i]);
    
            //add ZIP entry to output stream
            String tName = includeDirectoryInfo? 
                dirNames[i].substring(removeDirPrefix.length()): //already validated above
                File2.getNameAndExtension(dirNames[i]); 
            out.putNextEntry(new ZipEntry(tName));
    
            //transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
    
            //complete the entry
            out.closeEntry();
            in.close();
        }
    
        //close the ZIP file
        out.close();
        if (verbose) String2.log("  zip done. TIME=" + 
            (System.currentTimeMillis() - tTime) + "\n");
    }
    
    /**
     * Put the specified files in a gz file (without directory info).
     * If a file named gzipDirName already exists, it is overwritten.
     * 
     * @param gzipDirName the full name for the .gz file (path + name + ".gz")
     * @param dirNames the full names of the files to be put in the gz file.
     *    These can use forward or backslashes as directory separators.
     *    CURRENTLY LIMITED TO 1 FILE.
     * @param timeOutSeconds (use -1 for no time out)
     * @throws Exception if trouble
     */
    public static void gzip(String gzipDirName, String dirNames[], 
        int timeOutSeconds) throws Exception {

        gzip(gzipDirName, dirNames, timeOutSeconds, false, ""); 
    }

    /**
     * Put the specified files in a gzip file (with some directory info).
     * If a file named gzipDirName already exists, it is overwritten.
     * 
     * @param gzipDirName the full name for the .gz file (path + name + ".gz")
     * @param dirNames the full names of the files to be put in the gzip file.
     *    These can use forward or backslashes as directory separators.
     *    CURRENTLY LIMITED TO 1 FILE.
     * @param timeOutSeconds (use -1 for no time out)
     * @param removeDirPrefix the prefix to be removed from the start of 
     *    each dir name (ending with a slash)
     * @throws Exception if trouble
     */
    public static void gzip(String gzipDirName, String dirNames[], 
        int timeOutSeconds, String removeDirPrefix) throws Exception {

        gzip(gzipDirName, dirNames, timeOutSeconds, true, removeDirPrefix);
    }

    /**
     * Put the specified files in a gzip file (without directory info).
     * If a file named gzipDirName already exists, it is overwritten.
     * 
     * @param gzipDirName the full name for the .zip file (path + name + ".gz")
     * @param dirNames the full names of the files to be put in the gzip file.
     *    These can use forward or backslashes as directory separators.
     *    CURRENTLY LIMITED TO 1 FILE.
     * @param timeOutSeconds (use -1 for no time out)
     * @param includeDirectoryInfo set this to false if you don't want
     *   any dir invo stored with the files
     * @param removeDirPrefix if includeDirectoryInfo is true, 
     *    this is the prefix to be removed from the start of 
     *    each dir name (ending with a slash).
     *    If includeDirectoryInfo is false, this is removed.
     * @throws Exception if trouble
     */
    private static void gzip(String gzipDirName, String dirNames[], 
        int timeOutSeconds, boolean includeDirectoryInfo, String removeDirPrefix) 
        throws Exception {

        //validate
        if (includeDirectoryInfo) {
            //ensure slash at end of removeDirPrefix
            if ("\\/".indexOf(removeDirPrefix.charAt(removeDirPrefix.length() - 1)) < 0)
                throw new IllegalArgumentException(String2.ERROR + 
                    " in SSR.gzip: removeDirPrefix must end with a slash.");

            //ensure dirNames start with removeDirPrefix
            for (int i = 0; i < dirNames.length; i++)
                if (!dirNames[i].startsWith(removeDirPrefix))
                    throw new IllegalArgumentException(String2.ERROR + " in SSR.zip: dirName[" + 
                        i + "] doesn't start with " + removeDirPrefix + ".");
        }

        //if Linux, it is faster to use the zip utility
        //I don't know how to include just partial dir info with Linux,
        //  since I can't cd to that directory.
        /*if (String2.OSIsLinux && !includeDirectoryInfo) {
            //-j: don't include dir info
            if (verbose) String2.log("Using Linux's zip");
            File2.delete(zipDirName); //delete any exiting .zip file of that name
            cShell("zip -j " + zipDirName + " " + String2.toSSVString(dirNames), 
                timeOutSeconds); 
            return;
        }*/

        //for all other operating systems...
        //create the ZIP file
        long tTime = System.currentTimeMillis();
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzipDirName));
    
        //create a buffer for reading the files
        byte[] buf = new byte[4096];
    
        //compress the files
        for (int i = 0; i < 1; i++) { //i < dirNames.length; i++) {
            FileInputStream in = new FileInputStream(dirNames[i]);
    
            //add ZIP entry to output stream
            String tName = includeDirectoryInfo? 
                dirNames[i].substring(removeDirPrefix.length()): //already validated above
                File2.getNameAndExtension(dirNames[i]); 
            //out.putNextEntry(new ZipEntry(tName));
    
            //transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
    
            //complete the entry
            //out.closeEntry();
            in.close();
        }
    
        //close the GZIP file
        out.close();
        if (verbose) String2.log("  gzip done. TIME=" + 
            (System.currentTimeMillis() - tTime) + "\n");
    }
    
    /**
     * Extract all of the files from a zip file to 
     * the base directory.
     * Any existing files of the same name are overwritten.
     *
     * @param fullZipName (with .zip at end)
     * @param baseDir (with slash at end)
     * @param ignoreZipDirectories if true, the directories (if any)
     *    of the files in the .zip file are ignored, and
     *    all files are stored in baseDir itself.
     *    If false, new directories will be created as needed.
     * @param timeOutSeconds (use -1 for no time out)
     * @throws Exception
     */
    public static void unzip(String fullZipName, String baseDir, 
        boolean ignoreZipDirectories, int timeOutSeconds) throws Exception {

        //if Linux, it is faster to use the zip utility
        long tTime = System.currentTimeMillis();
        if (String2.OSIsLinux) {
            //-d: the directory to put the files in
            if (verbose) String2.log("Using Linux's unzip on " + fullZipName);
            cShell("unzip -o " + //-o overwrites existing files without asking
                (ignoreZipDirectories? "-j " : "") +
                fullZipName + " " +
                "-d " + baseDir.substring(0, baseDir.length() - 1),  //remove trailing slash   necessary?
                timeOutSeconds);
        } else {
            //use Java's zip procedures for all other operating systems
            if (verbose) String2.log("Using Java's unzip on " + fullZipName);
            ZipInputStream in = new ZipInputStream(new FileInputStream(fullZipName));
        
            //create a buffer for reading the files
            byte[] buf = new byte[4096];
        
            //unzip the files
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {

                //isDirectory?
                String name = entry.getName();
                if (verbose) String2.log("  unzipping " + name);
                if (entry.isDirectory()) {
                    if (ignoreZipDirectories) {
                    } else {
                        File tDir = new File(baseDir + name);
                        if (!tDir.exists())
                            tDir.mkdirs();
                    }
                } else {
                    //open an output file
                    //???do I need to make the directory???
                    if (ignoreZipDirectories) 
                        name = File2.getNameAndExtension(name); //remove dir info
                    OutputStream out = new FileOutputStream(baseDir + name);
    
                    //transfer bytes from the .zip file to the output file
                    //in.read reads from current zipEntry
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer, 0, buf.length)) > 0) {
                        out.write(buffer, 0, bytesRead);
                    }

                    //close the output file
                    out.close();
                }

                //close this entry
                in.closeEntry();

                //get the next entry
                entry = in.getNextEntry();
            }

            //close the input file
            in.close();
        }

        if (verbose) String2.log("  unzip done. TIME=" + 
            (System.currentTimeMillis() - tTime) + "\n");
    }

    /**
     * Extract the ONE file from a .gz file to 
     * the base directory.
     * An existing file of the same name will be overwritten.
     *
     * @param fullGzName (with .gz at end)
     * @param baseDir (with slash at end)
     * @param ignoreGzDirectories if true, the directories (if any)
     *    of the files in the .gz file are ignored, and
     *    all files are stored in baseDir itself.
     *    If false, new directories will be created as needed.
     *    CURRENTLY, ONLY 'TRUE' IS SUPPORTED. THE FILE IS ALWAYS GIVEN THE NAME
     *    fullGzName.substring(0, fullGzName.length() - 3).
     * @param timeOutSeconds (use -1 for no time out)
     * @throws Exception
     */
    public static void unGzip(String fullGzName, String baseDir, 
        boolean ignoreGzDirectories, int timeOutSeconds) throws Exception {

        //if Linux, it is faster to use the zip utility
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
            //use Java's gzip procedures for all other operating systems
            if (verbose) String2.log("Java's ungzip " + fullGzName);
            GZIPInputStream in = new GZIPInputStream(new FileInputStream(fullGzName));
        
            //create a buffer for reading the files
            byte[] buf = new byte[4096];
        
            ////unzip the files
            //ZipEntry entry = in.getNextEntry();
            //while (entry != null) {
                String ext = File2.getExtension(fullGzName); //should be .gz
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
                    //open an output file
                    //???do I need to make the directory???
                    if (ignoreGzDirectories) 
                        name = File2.getNameAndExtension(name); //remove dir info
                    OutputStream out = new FileOutputStream(baseDir + name);
    
                    //transfer bytes from the .zip file to the output file
                    //in.read reads from current zipEntry
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer, 0, buf.length)) > 0) {
                        out.write(buffer, 0, bytesRead);
                    }

                    //close the output file
                    out.close();
                }

                ////close this entry
                //in.closeEntry();

                ////get the next entry
                //entry = in.getNextEntry();
            //}

            //close the input file
            in.close();
        }
        if (verbose) String2.log("  ungzip done. TIME=" + 
            (System.currentTimeMillis() - tTime) + "\n");

    }

    /**
     * Unzip oldDir + oldName.zip  (a zip containing one file: oldName) 
     * and rename it to newDir + newName.
     * If newDir + newName already exists, it is File2.'touch'ed.
     *
     * @param oldDir (with slash at end)
     * @param oldName (with .zip at end)
     * @param newDir (with slash at end)
     * @param newName
     * @param timeOutSeconds (use 0 for no timeout)
     * @throws Exception
     */
    public static void unzipRename(String oldDir, String oldName, 
            String newDir, String newName, int timeOutSeconds) throws Exception {

        //already exists?
        if (File2.touch(newDir + newName)) {
            String2.log("SSR.unzipRename is reusing " + newName);
            return;
        }

        //unzip the file
        unzip(oldDir + oldName, newDir, true, timeOutSeconds);

        //rename the file
        String oldNameNoZip = oldName.substring(0, oldName.length() - 4);
        if (!oldNameNoZip.equals(newName))
            File2.rename(newDir, oldNameNoZip, newName); 
    }

    /**
     * This opens a ZipOutputStream with one entry (with the fileName, but no data).
     * This is not wrapped in a BufferedOutputStream, since it often doesn't need
     * to be.
     * 
     * @param zipDirName the full name for the .zip file (path + name + ".zip")
     * @param fileName the file name to be put in the zip file.
     *    Your choice: with directory info or not.
     *    Use forward directory separators [I'm pretty sure].
     * @return the ZipOutputStream (or null if trouble)
     */
    public static ZipOutputStream startZipOutputStream(String zipDirName, String fileName) {
        
        try {
            // Create the ZIP file
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipDirName));
        
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(fileName));

            return out;
        } catch (Exception e) {
            return null;
        }        
    }
    


    /**
     * THIS DOES NOT YET WORK PERFECTLY (BUT CLOSE). I DIDN'T SPEND TIME TRYING.
     * This returns the PostScript code to embed
     * an eps file in another PostScript file.
     * 
     * @param left
     * @param bottom
     * @param angle the angle to rotate (degrees)
     * @param xScale
     * @param yScale
     * @param bBoxLLX  the incoming file's bounding box lower left X
     * @param bBoxLLY
     * @param epsContents
     * @return the PostScript code to embed an eps file in another PostScript file.
     */
    public static String embedEPSF(double left, double bottom, double angle,
            double xScale, double yScale, double bBoxLLX, double bBoxLLY,
            String epsContents) { 
        return 
        //This is from PostScript Language Reference Manual 2nd ed, pg 726 
        //(in "Appendix H: Encapsulated PostScript File Format - Version 3.0"
        //BeginEPSF and EndEPSF were definitions (to be put in the prolog), 
        //but the def stuff is commented out here so this can be used inline
        //in case you don't have access to the prolog of the ps file you are creating.
        //"/BeginEPSF { %def\n" +                         //prepare for EPS file
        "  /b4_inc_state save def\n" +                 //save state for cleanup
        "  /dict_count countdictstack def\n" +
        "  /op_count count 1 sub def\n" +              //count objects on op stack
        "  userdict begin\n" +                         //make userdict current dict
        "  /showpage {} def\n" +                       //redifine showpage to be null
        "  0 setgray 0 setlinecap\n" +       
        "  1 setlinewidth 0 setlinejoin\n" +       
        "  10 setmiterlimit [] 0 setdash newpath\n" +       
        "  /languagelevel where\n" +                   //if not equal to 1 then
        "  {pop languagelevel\n" +                     //set strokeadjust and
        "  1 ne\n" +                                   //overprint to their defaults
        "    {false setstrokeadjust false setoverprint\n" + 
        "    } if\n" +       
        "  } if\n" +       
        //"  } bind def\n" +       
    
        //the code that actually embeds the content
        left + " " + bottom + " translate\n" +
        angle + " rotate\n" +
        xScale + " " + yScale + " scale\n" +
        bBoxLLX + " " + bBoxLLY + " translate\n" +
        epsContents + "\n" +

        //"/EndEPSF { %def \n" +
        "  count op_count sub {pop} repeat\n" +       
        "  countdictstack dict_count sub {end} repeat\n" + //clean up dict stack       
        "  b4_inc_state restore\n" +       
        //"} bind def\n" +       
        "";
    }
    
    /**
     * This converts an email address with dots and 'at' signs into
     * a safer form (to hide from spam spiders) for display on the web.
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
     * @param requestedFilesMap (map with key=String (e.g., fileName), 
     *     value=IntObject with frequency info).
     *     If it needs to be thread-safe, use ConcurrentHashMap.
     * @return a string with the topN items in a table
     */
    public static String getTopN(int printTopN, String header,
            Map requestedFilesMap) {
        //printTopNMostRequested
        //many of these will be artifacts: e.g., initial default file
        StringBuilder sb = new StringBuilder();
        if (printTopN > 0 && !requestedFilesMap.isEmpty()) {
            //topN will be kept as sorted ascending, so best will be at end 
            String topN[] = new String[printTopN]; 
            Arrays.fill(topN, "\t");
            int worst = 0;
            int nActive = 0;
            Iterator it = requestedFilesMap.keySet().iterator();
            while (it.hasNext()) {
               String key = (String)it.next();
               int value = ((IntObject)requestedFilesMap.get(key)).i;
               if (value <= 0) 
                   continue;
               if (nActive < printTopN ||
                   value > worst) {
                   if (nActive < printTopN)
                       nActive++;
                   if (value < worst)
                       worst = value;
                   String ts = String2.right("" + value, 9) + "  " + key;
                   int where = Arrays.binarySearch(topN, ts);
                   if (where >= 0)
                       //it is already in the array -- shouldn't ever happen 
                       sb.append(String2.ERROR + ": SSR.getTopN wants to insert \"" + ts + "\"\n" +
                           "at " + where + ", where values are\n" +
                           String2.toNewlineString(topN) + "\n");
                   else {
                       //make 'where' positively stated 
                       where = -where - 2;  //would be -1, but worst is always thrown out

                       //'where' may be -1 if tie for worst and file name sorts it at beginning
                       if (where >= 0 ) {
                           //open up a space (worst is always thrown out)
                           System.arraycopy(topN, 1, topN, 0, where);

                           //insert it
                           topN[where] = ts;
                       }
                   }
               }
            }

            //print the most requested .grd files
            sb.append(printTopN + header + "\n");
            for (int i = printTopN - 1; i >= 0; i--) {
                if (!topN[i].equals("\t"))
                    sb.append(String2.right("#" + (printTopN - i), 7) + topN[i] + "\n");
            }
        }
        return sb.toString();
    }


    /**
     * This runs a series matlab commands, ending with 'exit' or 'quit'.
     * This is set up for Linux computers and uses cShell commands.
     * 
     * @param fullControlName the name of the file with the 
     *    commands for Matlabs command line (one per line).
     * @param fullOutputName the name for the file that will be 
     *    created to hold the Matlab output.
     * @param timeOutSeconds (use -1 for no time out)
     * @throws Exception if trouble
     */
    public static void runMatlab(String fullControlName, String fullOutputName,
            int timeOutSeconds) throws Exception {
        //this is Dave Foley's trick in his /u00/chump/matcom script 
        Exception e = null;
        try {
            SSR.cShell("set OLDDISPLAY = $DISPLAY", 1);
            SSR.cShell("unsetenv DISPLAY", 1);
            SSR.cShell("nohup matlab < " + fullControlName + " >! " + fullOutputName, //">!" writes to a new file 
                timeOutSeconds); 
        } catch (Exception e2) {
            e = e2;
        }

        //The purpose of try/catch above is to ensure this gets done.
        //Problem was: DISPLAY was unset, then error occurred and DISPLAY was never reset.
        SSR.cShell("setenv DISPLAY $OLDDISPLAY", 1); 

        if (e != null)
            throw new Exception(e);
    }

    /**
     * This procedure sends an email. For example,
     * <pre>sendEmail("mail.server.name", 25, "joe.smith", password, "joe@smith.com", "sue@smith.com",
     *           "re: dinner", "How about at 7?");
     * </pre>
     *
     * <p>This code uses /libs/mail.jar from the JavaMail API (currently 1.5.1).
     * This requires additions to javac's and java's -cp: 
     * ";C:\programs\tomcat\webapps\cwexperimental\WEB-INF\lib\mail.jar"
     * The mail.jar files are available from Sun
     * (see http://www.oracle.com/technetwork/java/javamail/index.html).
     * The software is copyrighted by Sun, but Sun grants anyone 
     * the right to freely redistribute the binary .jar files.
     * The source code is also available from Sun.
     *
     * <p> See the javamail examples 
     * notably [c:/programs/]javamail-1.4.3/demo/msgsend.java .
     * Some programming information is from Java Almanac: 
     * http://javaalmanac.com/egs/javax.mail/SendApp.html
     * and http://www.websina.com/bugzero/kb/sunmail-properties.html
     * <br>See also http://kickjava.com/2872.htm
     *
     * <p>If this fails with "Connection refused" error, 
     *   make sure McAffee "Virus Scan Console :
     *   Access Protection Properties : Anti Virus Standard Protections :
     *   Prevent mass mailing worms from sending mail" is un-checked.
     *
     * @param smtpHost the name of the outgoing mail server
     * @param smtpPort port to be used, usually 25 or 587
     * @param userName for the mail server
     * @param password for the mail server 
     *    (may be null or "" if only sending to local email account)
     * @param properties  additional properties to be used by Java mail
     *     stored as pairs of Strings within a String (separated by |'s)
     *     e.g., "mail.smtp.starttls.enable|true|param2|value2".
     *     Use null or "" is there are none.
     *     See http://javamail.kenai.com/nonav/javadocs/com/sun/mail/smtp/package-summary.html
     * @param fromAddress the email address the email is coming from
     *   (usually the same as the userName)
     * @param toAddresses a comma-separated list of the email addresses the email is going to.
     *   (They will be sent as separate emails.)
     *    If all or one is null or "" or "null", it's a silent error.
     * @param subject The subject is sent with UTF-8 encoding, 
     *    so any Unicode characters are (theoretically) ok.
     * @param content The content is sent with UTF-8 encoding, 
     *    so any Unicode characters are (theoretically) ok.
     * @throws Exception if trouble
     */
    public static void sendEmail(String smtpHost, int smtpPort,
        String userName, String password, String properties, 
        String fromAddress, String toAddresses,
        String subject, String content) throws Exception {

        //String2.log("SSR.sendEmail host=" + smtpHost + " port=" + smtpPort + "\n" +
        //    "  properties=" + properties + "\n" +
        //    "  userName=" + userName + " password=" + (password.length() > 0? "[present]" : "[absent]") + "\n" + 
        //    "  toAddress=" + toAddress);

        if (toAddresses == null || toAddresses.length() == 0 || toAddresses.equals("null")) {
            String2.log("SSR.sendEmail: no toAddresses");
            return;
        }
        if (smtpPort < 0 || smtpPort == Integer.MAX_VALUE) 
            throw new Exception(String2.ERROR + " in sendEmail: smtpPort=" + smtpPort +
                " is invalid.");
        //I'm not sure if System.getProperties returns a new Properties 
        //  or a reference to the same system properties object
        //  so use new Properties to make System properties just the defaults
        //  so this is thread safe.
        //The Oracle example uses System.getProperties, but I think it is
        //  unneccessary and exposes information needlessly.
        //Properties props = new Properties(System.getProperties()); 
        Properties props = new Properties(); 
        if (properties != null && properties.trim().length() > 0) {
            String sar[] = String2.split(properties, '|');
            int n = (sar.length / 2) * 2;
            for (int i = 0; i < n; i += 2)
                props.setProperty(sar[i], sar[i+1]);
        }
        props.setProperty("mail.smtp.host", smtpHost);
        props.setProperty("mail.smtp.port", "" + smtpPort);


        //get a session
        Session session;   
        if (password == null || password.length() == 0) {
            session = Session.getInstance(props, null);
        } else {
            //use non-default Session.getInstance (not shared) for password sessions
            props.setProperty("mail.smtp.auth", "true");
            props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            session = Session.getInstance(props);
        }
        if (debugMode) session.setDebug(true);

        Transport smtpTransport = null;
        if (password != null && password.length() > 0) {
            smtpTransport = session.getTransport("smtp");
            smtpTransport.connect(smtpHost, userName, password);
        }

        // Send the message
        String addresses[] = toAddresses.split(",");
        for (int add = 0; add < addresses.length; add++) {
            String toAddress = addresses[add].trim();
            if (toAddress.length() == 0 || toAddress.equals("null"))
                continue;

            // Construct the message
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromAddress));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress, false));
            msg.setSubject(subject, "UTF-8");
            msg.setText(content, "UTF-8"); 
            msg.setHeader("X-Mailer", "msgsend");
            msg.setSentDate(new Date());
            msg.saveChanges();  //do last.  don't forget this

            try {
                if (password == null || password.length() == 0)
                    Transport.send(msg);
                else 
                    smtpTransport.sendMessage(msg, msg.getAllRecipients());
            } catch (Exception e) {
                String2.log(MustBe.throwableWithMessage("SSR.sendEmail", "to=" + toAddress, e));
            }
        }
        if (smtpTransport != null)
            smtpTransport.close();    
    }

    /**
     * This adds compression to the outputStream if Accept-Encoding 
     * indicates it is supported.
     * This accepts gzip, x-gzip, and deflate compressed responses
     * (not compress or x-compress as they seem harder to work with because of 'Entries').
     * For info on compressed responses, see: 
     * http://www.websiteoptimization.com/speed/tweak/compress/ 
     * and the related test site: http://www.webperformance.org/compression/ .
     *
     * @param outputStream urlString
     * @return a (decompressed if necessary) InputStream
     */
/*    public static OutputStream getResponseOutputStream(OutputStream outputStream, ) {
        try {
            URL turl = new URL(urlString); 
            HttpURLConnection conn = (HttpURLConnection)turl.openConnection();
            conn.setRequestProperty("Accept-Encoding", 
                "gzip, x-gzip, compress, x-compress, deflate");
            //log("request: " + String2.toString(conn.getRequestProperties()));
            conn.connect();      
            String encoding = conn.getContentEncoding();
            InputStream is = conn.getInputStream();
            printError("url = " + urlString + "\n" +  //diagnostic
                "encoding=" + encoding + "\n" +
                "BeginContent");
            if (encoding != null) {
                encoding = encoding.toLowerCase();
                //if (encoding.indexOf("compress") >= 0)
                //    is = new ZipInputStream(is);
                //else 
                if (encoding.indexOf("gzip") >= 0)
                    is = new GZIPInputStream(is);
                else if (encoding.indexOf("deflate") >= 0)
                    is = new InflaterInputStream(is);
            }
            return is;
        } catch (Exception e) {
            String2.log.fine(MustBe.throwableToString(e));
            return null;
        }
    }
*/

    /**
     * This does minimal percentEncoding for form item names and values in urls.
     * 
     * @param nameOrValue   not yet percentEncoded
     * @return the encoded query string.
     *    If query is null, this returns "".
     * @throws Exception if trouble
     */
    public static String minimalPercentEncode(String nameOrValue) throws Exception {
        if (nameOrValue == null)
            return "";
        StringBuilder sb = new StringBuilder();
        int nvLength = nameOrValue.length();
        for (int po = 0; po < nvLength; po++) {
            char ch = nameOrValue.charAt(po);
            if      (ch == '%') sb.append("%25");
            else if (ch == '&') sb.append("%26");  //needed to distinguish & in value in &var=value
            else if (ch == '"') sb.append("%22");  //helps with urls in javascript code
            else if (ch == '\'')sb.append("%27");  //helps with urls in javascript code
            else if (ch == '=') sb.append("%3D");  //needed to distinguish = in value in &var=value
            else if (ch == '#') sb.append("%23");  //needed for SOS    added 2009-09-28
            else if (ch == '+') sb.append("%2B");  //before handling ' '
            else if (ch == ' ') sb.append("%20");  //safer than '+'
            else if (ch == '<') sb.append("%3C");
            else if (ch == '>') sb.append("%3E");
            //see slide 7 of https://www.owasp.org/images/b/ba/AppsecEU09_CarettoniDiPaola_v0.8.pdf
            //reserved=; / ? : @ & = + $ ,
            else if (ch == ';') sb.append("%3B");
            else if (ch == '/') sb.append("%2F");
            else if (ch == '?') sb.append("%3F");
            //else if (ch == ':') sb.append("%3A");
            else if (ch == '@') sb.append("%40");
            else if (ch == '$') sb.append("%24");
            else if (ch < 32 || ch >= 127) sb.append(percentEncode("" + ch)); //this handles any unicode char via utf-8
            else sb.append(ch);
        }
        return sb.toString();
    }

    /**
     * This encodes an Http GET query by converting special characters to %HH 
     * (where H is a hexadecimal digit) and by converting ' ' to '+'.
     * <br>This should be use RARELY. See minimalPercentEncode for most uses.
     * <br>This is used on names and values just prior to contacting a url.
     * <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     * <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * 
     * @param query   not yet percentEncoded
     * @return the encoded query string.
     *    If query is null, this returns "".
     * @throws Exception if trouble
     */
    public static String percentEncode(String query) throws Exception {
        if (query == null)
            return "";
        return URLEncoder.encode(query, "UTF-8");
    }

    /**
     * This decodes an Http GET query by converting all %HH (where H is a hexadecimal digit)
     * to the corresponding character and by converting '+' to ' '.
     * This is used by a server program before parsing the incoming query.
     * 
     * @param query  already percentEncoded
     * @return the decoded query string.
     *    If query is null, this returns "".
     * @throws Exception if trouble
     */
    public static String percentDecode(String query) throws Exception {
        if (query == null)
            return "";
        //query = String2.replaceAll(query, "+", " "); //URLDecoder doesn't do this.  2010-10-27 Yes it does.
        return URLDecoder.decode(query, "UTF-8");

        /*was StringBuilder sb = new StringBuilder(query);
        String2.replaceAll(sb, "+", " "); //do first
        int po = sb.indexOf("%");
        while (po >= 0) {
            if (po <= sb.length() - 3 &&
                String2.isHexDigit(sb.charAt(po + 1)) &&
                String2.isHexDigit(sb.charAt(po + 2))) {
                sb.setCharAt(po, (char)Integer.parseInt(sb.substring(po + 1, po + 3), 16));
                sb.delete(po + 1, po + 3);
            }
            po++;
            po = sb.indexOf("%", po);
        }
        return sb.toString();
        */
    }

    /**
     * This just connects to (pings) the urlString but doesn't read from the input stream
     * (usually for cases where it is a big security risk).
     * Note that some urls return an endless stream of random digits,
     * so reading a malicious inputStream would be trouble.
     * Besides, for this purpuse, the caller doesn't care what the response is.
     *
     * @param urlString   The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @param timeOutMillis
     * @throws Exception if trouble
     */
    public static void touchUrl(String urlString, int timeOutMillis) throws Exception {
        //tests show that getting the inputStream IS necessary (but reading it is not)
        Object[] oar = getUrlConnInputStream(urlString, timeOutMillis);  
        InputStream in = (InputStream)oar[1];
        //it doesn't seem necessary to read even 1 byte (if available)
        //if (in.available() > 0)
        //    in.read();
        in.close();
    }
                                            

    /**
     * This gets an uncompressed inputStream from a url.
     * [I.e., This doesn't try for compression, so the inputStream won't be compressed,
     * so this doesn't even try to decompress it.]
     * This has a 2 minute timeout to initiate the connection
     * and 10 minute read timeout.
     *
     * @param urlString   The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @return an InputStream from the url
     * @throws Exception if trouble
     */
    public static InputStream getUncompressedUrlInputStream(String urlString) 
            throws Exception {
        if (reallyVerbose) String2.log("getUncompressedUrlInputStream\n" +
            urlString);  
        URL turl = new URL(urlString); 
        HttpURLConnection conn = (HttpURLConnection)turl.openConnection();
        conn.setConnectTimeout(120000); //2 minute timeout
        conn.setReadTimeout(10 * Calendar2.SECONDS_PER_MINUTE * 1000); //ten minutes, in ms
        conn.connect();      
        return conn.getInputStream();        
    } 

    /**
     * This gets the response from a url.
     * This is useful for short responses.
     *
     * @param urlString The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @return a String with the response.   Lines will always be separated by \n only.
     * @throws Exception if error occurs
     */
    public static String getUncompressedUrlResponseString(String urlString) throws Exception {
        try {
            InputStream is = getUncompressedUrlInputStream(urlString); 
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = in.readLine()) != null) {
                sb.append(s);
                sb.append('\n');
            }
            in.close();
            return sb.toString();
        } catch (Exception e) {
            throw new Exception(String2.ERROR + " from url=" + urlString + " : " + e.toString());
        }
    } 


    /**
     * This downloads a file as bytes from a Url and saves it as a file.
     * If there is a failure, this doesn't try to delete the parially written file.
     * If the file is zipped, it will stay zipped.
     * Note that you may get a error-404-file-not-found error message stored in the file.
     *
     * @param urlString urlString pointing to the information.  
     *   The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @param fullFileName the full name for the file to be created.
     *   If the directory doesn't already exist, it will be created.
     * @param tryToUseCompression If true, the request indicates compression
     *   is acceptable and the input stream will do the decompression.
     *   'false' is safer if the file may be already compressed (e.g., .gz or .zip)
     *   because this won't try to unzip the file.
     * @throws Exception if trouble
     */
    public static void downloadFile(String urlString,
            String fullFileName, boolean tryToUseCompression) throws Exception {
        //first, ensure dir exists
        File2.makeDirectory(File2.getDirectory(fullFileName));
        InputStream in = tryToUseCompression? 
            getUrlInputStream(urlString) :             
            getUncompressedUrlInputStream(urlString);  
        OutputStream out = new FileOutputStream(fullFileName);
        byte buffer[] = new byte[32768];
        int nBytes = in.read(buffer);
        while (nBytes > 0) {
            out.write(buffer, 0, nBytes);
            nBytes = in.read(buffer);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * This gets the inputStream from a url.
     * This solicits and accepts gzip, x-gzip, and deflate compressed responses
     * (not compress or x-compress as they seem harder to work with because of 'Entries').
     * For info on compressed responses, see: 
     * http://www.websiteoptimization.com/speed/tweak/compress/ 
     * and the related test site: http://www.webperformance.org/compression/ .
     *
     * @param urlString   The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @param timeOutMillis the time out for opening a connection in milliseconds  
     *    (use -1 to get high default, currently 10 minutes)
     * @return Object[2], [0]=UrlConnection, [1]=a (decompressed if necessary) InputStream
     * @throws Exception if trouble
     */
    public static Object[] getUrlConnInputStream(String urlString, int timeOutMillis) throws Exception {
//EEK! this doesn't deal with character encoding; the information (in conn) is lost.
        if (reallyVerbose) 
            String2.log("getUrlConnInputStream " + urlString);  
        URL turl = new URL(urlString); 
        URLConnection conn = turl.openConnection();
        conn.setRequestProperty("Accept-Encoding", 
            "gzip, x-gzip, deflate"); //compress, x-compress, 
        //String2.log("request: " + String2.toString(conn.getRequestProperties()));
        if (timeOutMillis <= 0)
            timeOutMillis = 10 * Calendar2.SECONDS_PER_MINUTE * 1000; //ten minutes, in ms
        conn.setConnectTimeout(timeOutMillis);
        //I think setReadTimeout is any period of inactivity.
        conn.setReadTimeout(10 * Calendar2.SECONDS_PER_MINUTE * 1000); //ten minutes, in ms
        conn.connect();      
        String encoding = conn.getContentEncoding();
        InputStream is = conn.getInputStream();
        //String2.log("url = " + urlString + "\n" +  //diagnostic
        //  "  headerFields=" + String2.toString(conn.getHeaderFields()));
        //    "encoding=" + encoding + "\n" +
        //    "BeginContent");
        if (encoding != null) {
            encoding = encoding.toLowerCase();
            //if (encoding.indexOf("compress") >= 0)
            //    is = new ZipInputStream(is);
            //else 
            if (encoding.indexOf("gzip") >= 0)
                is = new GZIPInputStream(is);
            else if (encoding.indexOf("deflate") >= 0)
                is = new InflaterInputStream(is);
        }
        return new Object[]{conn, is};
    }

    /** For compatibility with older code. It uses a timeout of 120 seconds.
     * This tries to use compression.
     *
     * @param urlString The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     */
    public static InputStream getUrlInputStream(String urlString) throws Exception {
        return (InputStream)(getUrlConnInputStream(urlString, 120000)[1]); 
    }

    /**
     * This gets the response from a url.
     * This is useful for short responses.
     * This tries to use compression.
     *
     * @param urlString The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @return a String[] with the response (one string per line of the file).
     * @throws Exception if error occurs
     */
    public static String[] getUrlResponse(String urlString) throws Exception {
        try {
            InputStream is = getUrlInputStream(urlString); 
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> al = new ArrayList();
            String s;
            while ((s = in.readLine()) != null) {
                al.add(s);
            }
            in.close();
            return al.toArray(new String[0]);
        } catch (Exception e) {
            throw new Exception(String2.ERROR + " from url=" + urlString + " : " + e.toString());
        }
    } 

    /**
     * This gets the response from a url as one newline separated String.
     * This is useful for short responses.
     * This tries to use compression.
     *
     * @param urlString   The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @return a String with the response.
     * @throws Exception if error occurs
     */
    public static String getUrlResponseString(String urlString) throws Exception {
        return new String(getUrlResponseBytes(urlString));  //define charset???!!! 
    }

    /**
     * This gets the bytes from a url.
     * This tries to use compression.
     *
     * @param urlString The query MUST be already percentEncoded as needed.
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @return a byte[] with the response.
     * @throws Exception if error occurs
     */
    public static byte[] getUrlResponseBytes(String urlString) throws Exception {
        try {
            byte buffer[] = new byte[1024];
            InputStream is = getUrlInputStream(urlString); 
            ByteArray ba = new ByteArray();
            int available = is.available();
            while (available >= 0) { //0 is ok
                int getN = is.read(buffer, 0, Math.min(1024, Math.max(1, available))); //1.. avail .. 1024
                if (getN == -1) //eof
                    break;
                ba.add(buffer, 0, getN);
            }
            is.close();
            return ba.toArray();
        } catch (Exception e) {
            //String2.log(e.toString());
            throw new Exception(String2.ERROR + " from url=" + urlString + " : " + e.toString());
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
     * @param fileName
     * @return a byte[] with the response.
     * @throws Exception if error occurs
     */
    public static byte[] getFileBytes(String fileName) throws Exception {
        InputStream is = null; 
        try {
            byte buffer[] = new byte[1024];
            is = new FileInputStream(fileName); 
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int s; (s=is.read(buffer)) != -1; ) 
                baos.write(buffer, 0, s);
            return baos.toByteArray();
        } catch (Exception e) {
            //String2.log(e.toString());
            throw new Exception("ERROR while reading file=" + fileName + " : " + e.toString());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable t) {
                }
            }
        }
    } 


    public static void genericTunnelTest(int nTimes, String baseUrl, String varName) throws Exception {
        //currently, GAssta hday on otter has time dimension size is 1392
        //currently, GAssta hday on oceanwatch has time dimension size is 2877
        int nTimePoints = 1000;
        System.out.println("\nSSR.genericTunnelTest(" + nTimes + ", " + baseUrl + ")");
        long elapsedTime = System.currentTimeMillis();
        java.util.Random random = new java.util.Random();

        //run the test
        for (int time = 0; time < nTimes; time++) {
            //String2.log("iteration #" + time);
            int tIndex = random.nextInt(nTimePoints);
            int xIndex = random.nextInt(52);
            int yIndex = random.nextInt(52);
            String ts = getUrlResponseString(  
                baseUrl + "?" + varName +
                "[" + tIndex + ":1:" + tIndex + "]" +
                "[0:1:0]" +
                "[" + yIndex + ":1:" + yIndex + "]" +
                "[" + xIndex + ":1:" + xIndex + "]");
            //if (time == 0) System.out.println(ts);
        }
        System.out.println("SSR.threddsTunnelTest done.  TIME=" + 
            (System.currentTimeMillis() - elapsedTime) + " ms\n");
    }

    /**
     * I NEVER GOT THIS WORKING. 
     * This posts information to a url then captures the response.
     * From http://javaalmanac.com/egs/java.net/PostSocket.html?l=new
     * 
     * @param hostName e.g., hostname.com
     * @param app e.g., /servlet/SomeServlet
     * @param data the data to be sent; the keys and values MUST be already percentEncoded as needed.
     *    e.g., key1=value1&key2=value2
     *   <br>See http://en.wikipedia.org/wiki/Percent-encoding .
     *   <br>Note that reserved characters only need to be percent encoded in special circumstances (not always).
     * @return a String[] with the response.
     *   If an error occurs, this will return String[2]{null, error message}.
     */
/*    public static String[] postHTMLForm(
        String host, String app, String[] data) {
        //String hostName, String app, String[] data) {
        //String urlString, Object[] parameters) {
        //String hostName, String app, String data) {
        try {

            //http://www.informit.com/guides/content.asp?g=java&seqNum=44  
            //The httpClient attempts require commons-httpclient... and commons-logging.jar
            //  to be in the /lib directory and in the -cp for jikes and java
            //;C:\programs\tomcat\webapps\cwexperimental\WEB-INF\lib\commons-httpclient-2.0.2.jar;C:\programs\tomcat\webapps\cwexperimental\WEB-INF\lib\commons-logging.jar
            //The .jar files (and related files) can be downloaded from Apache's Jakarta project.
            int port = 80;
            HttpClient client = new HttpClient();
            PostMethod postMethod = new PostMethod(host + app);

            //Configure the form parameters
            for (int i = 0; i < data.length/2; i++) 
                postMethod.addParameter(data[i/2], data[i/2 + 1]);

            //include the cookies   
            GetMethod getMethod = new GetMethod(host + app);
            int statusCode = client.executeMethod(getMethod);
            String2.log("getHTMLForm getStatusText=" + getMethod.getStatusText());            
            getMethod.releaseConnection();
            CookieSpec cookieSpec = CookiePolicy.getDefaultSpec();
            String2.log("n client cookies= " + client.getState().getCookies().length);
            //Cookie[] cookies = cookieSpec.match(
            //    host, port, app, false, client.getState().getCookies()); //false for using secure
            //if (cookies == null) 
            //    String2.log("no cookies found");
            //else {
            //    String2.log(cookies.length + " cookies found");
            //    for (int i = 0; i < cookies.length; i++)
            //        client.getState().addCookie(cookies[i]);
           // }

            //Execute the POST method
            statusCode = client.executeMethod(postMethod);
            String2.log("postHTMLForm getStatusText=" + postMethod.getStatusText());
            if (statusCode == -1) 
                return new String[]{null, "SSR.postHTMLForm statusCode=-1 status=" + postMethod.getStatusText()};
            String content = postMethod.getResponseBodyAsString(); //before releaseConnection
            postMethod.releaseConnection();
            return String2.splitNoTrim(content, '\n');

/*            //cobbled together from 
            //http://svn.apache.org/viewcvs.cgi/jakarta/commons/proper/httpclient/trunk/src/examples/FormLoginDemo.java?view=markup        
            //and http://svn.apache.org/viewcvs.cgi/jakarta/commons/proper/httpclient/trunk/src/examples/TrivialApp.java?view=markup
            int port = 80;
            HttpClient client = new HttpClient();
            client.getHostConfiguration().setHost(hostName, port, "http");
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
            // 'developer.java.sun.com' has cookie compliance problems
            // Their session cookie's domain attribute is in violation of the RFC2109
            // We have to resort to using compatibility cookie policy

            //check for cookies 
            GetMethod authget = new GetMethod(app); //"/servlet/SessionServlet");
            client.executeMethod(authget);
            if (verbose) String2.log("Login form get: " + authget.getStatusLine().toString()); 
            // release any connection resources used by the method
            authget.releaseConnection();
            // See if we got any cookies
            CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
            Cookie[] initcookies = cookiespec.match(
                hostName, port, "/", false, client.getState().getCookies());
            if (verbose) String2.log("Initial set of cookies:");    
            if (initcookies.length == 0) {
                if (verbose) String2.log("(None)");    
            } else {
                for (int i = 0; i < initcookies.length; i++) {
                    if (verbose) String2.log("- " + initcookies[i].toString());    
                }
            }
            
            // Prepare post parameters
            PostMethod authpost = new PostMethod("/servlet/SessionServlet");
            NameValuePairs pairs[] = new NameValuePairs[data.length / 2];
            for (int i = 0; i < data.length/2; i++) 
                pairs[i] = new NameValuePair(i/2, i/2 + 1);
            authpost.setRequestBody(pairs);
            
            client.executeMethod(authpost);
            if (verbose) String2.log("form post: " + authpost.getStatusLine().toString()); 

            //execute the method
            String responseBody = null;
            client.executeMethod(authPost);
            responseBody = authPost.getResponseBodyAsString();

            //write out the request headers
            //String2.log("*** Request ***");
            //String2.log("Request Path: " + authPost.getPath());
            //String2.log("Request Query: " + authPost.getQueryString());
            //Header[] requestHeaders = authPost.getRequestHeaders();
            //for (int i=0; i<requestHeaders.length; i++){
            //    System.out.print(requestHeaders[i]);
            //}

            //write out the response headers
            //String2.log("*** Response ***");
            //String2.log("Status Line: " + authPost.getStatusLine());
            //Header[] responseHeaders = authPost.getResponseHeaders();
            //for (int i=0; i<responseHeaders.length; i++){
            //    System.out.print(responseHeaders[i]);
            //}

            //write out the response body
            //String2.log("*** Response Body ***");
            String2.log(responseBody);

            //clean up the connection resources
            authPost.releaseConnection();
            // */ 
    

            /* //from http://www.devx.com/Java/Article/17679/0/page/3
            InputStream is = com.myjavatools.web.ClientHttpRequest.post(new URL(urlString), parameters);

            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> al = new ArrayList();
            String s;
            while ((s = in.readLine()) != null) {
                al.add(s);
            }
            in.close();
            return al.toArray(new String[0]);
            // */

            /* //from java almanac
            // Create a socket to the host
            int port = 80;
            InetAddress addr = InetAddress.getByName(hostName);
            Socket socket = new Socket(addr, port);
        
            // Send header
            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(), "UTF8"));
            wr.write("POST " + app + " HTTP/1.0\r\n");
            wr.write("Content-Length: " + data + "\r\n");
            wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
            wr.write("\r\n");
        
            // Send data
            wr.write(data);
            wr.flush();
        
            // Get response
            BufferedReader rd = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
            ArrayList<String> al = new ArrayList();
            String line;
            while ((line = rd.readLine()) != null) {
                al.add(line);
            }
            wr.close();
            rd.close();
            return al.toArray(new String[0]);
            // */
/*        } catch (Exception e) {
            return new String[]{null, MustBe.throwable("SSR.postHTMLForm", e)};
        }
    }
*/


    /**
     * 2014-08-05 DEACTIVATED BECAUSE NOT USED. IF NEEDED, SWITCH TO Apache commons-net???
     * This opens a secure socket shell connection.
     * When you are done using it, call sshClient.disconnect();
     * 
     * <p>This uses code from the J2SSH project.
     * See the "import com.sshtools.j2ssh...." statements.
     * Based on c:\programs\j2ssh.examples\SftpConnect.java (License=LGPL)
     * which is Copyright (C) 2002 Lee David Painter (lee@sshtools.com).
     * The source code files for the J2SSH project are distributed by  
     * SSHTools (see http://sourceforge.net/projects/sshtools/ ).
     * j2ssh needs the com.sshttools... 
     * (Apache license; I'm using version 0.2.2, which is old (2003) but the most recent available).
     *
     * <p>Compiling the j2ssh classes was troublesome. 
     * Try to compile it. 
     * You see that you need to do a case sensitive change of "enum" to 
     * "enumeration" in two classes
     * and compile it with javac (not jikes), although still tons of warnings.
     * Also, I changed "com.sshtools.j2ssh.transport.ConsoleKnownHostsKeyVerification.java"
     * so it doesn't print unknown host info (in onUnknownHost())
     * and always assumes "yes" for "Do you want to allow this host key?" (in getResponse()).
     *
     * <p>J2SSH uses the Apache Jakarta Commons Logging project (Apache license).
     * The files are in the netcdfAll.jar.
     * !!!The commons logging system and your logger must be initialized before using this class.
     * It is usually set by the main program (e.g., NetCheck).
     * For a simple solution, use String2.setupCommonsLogging(-1); 
     *
     * @param hostName
     * @param userName
     * @param password
     * @return SshClient
     * @throws Exception if trouble
     *
    public static SshClient getSshClient(String hostName, String userName, String password)
         throws Exception {

        //Make a client connection
        ConfigurationLoader.initialize(false);
        SshClient ssh = new SshClient();
        ssh.connect(hostName);

        //Create a password authentication instance
        PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
        pwd.setUsername(userName);
        pwd.setPassword(password);

        //Try the authentication
        int result = ssh.authenticate(pwd);

        //Evaluate the result
        String states[] = {"0=?", "1=READY", "2=FAILED", "3=PARTIAL", "4=COMPLETE", "5=CANCELLED"};
        Test.ensureEqual(result, AuthenticationProtocolState.COMPLETE, 
            "Secure Socket Shell authentication failed. host=" + hostName +
            " user=" + userName + " result=" + result + "=" +
            (result >= 0 && result < states.length? states[result] : "(?)") + ".");

        //The connection is authenticated; we can now do some real work!
        return ssh;
    }

    /**
     * 2014-08-05 DEACTIVATED BECAUSE NOT USED. IF NEEDED, SWITCH TO Apache commons-net???
     * This sends and/or receives files via secure ftp.
     * For sending, the values in the parallel arrays 
     * sendLocalDirs, sendNames, sendRemoteDirs, specify which
     * localDir+name is sent to removeDir+name.
     * For receiving, the values in the parallel arrays 
     * receiveLocalDirs, receiveNames, receiveRemoteDirs, specify which
     * remoteDir+name is sent to localDir+name.
     * 
     * @param hostName the name of the remote computer, e.g., "coastwatch.pfeg.noaa.gov"
     * @param userName
     * @param password
     * @param commands contains a series of commands, separated by
     *  '\n's. Each command is a String[] with elements: 0=command, 1=param1, 2=param2 (where the number of parameters may be 0 or more). 
     *  Currently supported commands are:
     *  <ul>
     *  <li> "cd [remoteDir]" (changes the remote directory)
     *  <li> "lcd [localDir]" (changes the local directory)
     *  <li> "put [fileName]" (puts fileName from the local directory into the remote directory)
     *  <li> "get [fileName]" (gets fileName from the remote directory and places it in the local directory)
     *  <li> "mkdir [dirName]" (makes a remote directory; error if the dir already exists)
     *  <li> "mkdirs [dirName]" (makes a remote directory or directories; no error if the dir already exists)
     *  <li> "rm [path]" (removes a file or directory on the remote computer; error if path doesn't exist or dir not empty)
     *  <li> "rms [path]" (removes a file or directory on the remote computer; no error if path doesn't exist)
     *  <li> "chown [int uid] [path]" (changes ownership of the remote directory or file)
     *  <li> "chgrp [int gid] [path]" (changes the numeric group id for the new remote group)
     *  <li> "chmod [int permissions] [path]" (changes who can read, change or execute a file)
     *  <li> "rename [oldpath] [newpath]" (renames a remote file or directory)
     *  <li> "symlink [path] [link]" (creates a symbolic link on the remote computer)
     *  <li> "" (a blank line is ignored)
     *  </ul>
     *  Each of the commands throws an Exception if there is trouble.
     *  Warning: this currently doesn't deal with &gt;1 space between
     *  items in a command, or with quoted parameters
     *  (don't use quotes) or with spaces in parameters.
     *  See com/sshtools/j2ssh/SftpClient for details (including a few methods 
     *  more complex methods that I didn't implement but that could be implemented).
     * @throws Exception if trouble
     *
    public static void sftp(String hostName, String userName, String password, 
        String commands) throws Exception {

        //connect (this may throw an exception)
        SshClient sshClient = getSshClient(hostName, userName, password);
        SftpClient sftp = null;

        String[] commandAr = String2.splitNoTrim(commands, '\n');

        try {
            sftp = sshClient.openSftpClient();

            for (int i = 0; i < commandAr.length; i++) {
                String command = commandAr[i];
                String parameter1 = "";
                String parameter2 = "";
                int po = command.indexOf(' ');
                if (po > 0) {
                    parameter1 = command.substring(po + 1);
                    command = command.substring(0, po);
                }
                po = parameter1.indexOf(' ');  
                if (po > 0) {
                    parameter2 = parameter1.substring(po + 1);
                    parameter1 = parameter1.substring(0, po);
                }
                if      (command.equals("cd")) sftp.cd(parameter1);
                else if (command.equals("lcd")) sftp.lcd(parameter1);
                else if (command.equals("put")) sftp.put(parameter1);
                else if (command.equals("get")) sftp.get(parameter1);
                else if (command.equals("mkdir")) sftp.mkdir(parameter1);
                else if (command.equals("mkdirs")) sftp.mkdirs(parameter1);
                else if (command.equals("rm")) sftp.rm(parameter1);
                else if (command.equals("rms")) {
                    try {
                        sftp.rm(parameter1);
                    } catch (Exception e) {} //ignore any errors
                } else if (command.equals("chown")) sftp.chown(String2.parseInt(parameter1), parameter2);
                else if (command.equals("chgrp")) sftp.chgrp(String2.parseInt(parameter1), parameter2);
                else if (command.equals("chmod")) sftp.chmod(String2.parseInt(parameter1), parameter2);
                else if (command.equals("rename")) sftp.rename(parameter1, parameter2);
                else if (command.equals("symlink")) sftp.symlink(parameter1, parameter2);
                else if (command.equals("")) {}
                else throw new IllegalArgumentException(
                    String2.ERROR + " in SSR.sftp: unrecognized command #" + 
                    i + " (" + commandAr[i] + ").");
            }

        } catch (Exception e) {
            throw new Exception(e);            
        } finally {
            //close the session
            if (sftp != null) 
                sftp.quit();
            sshClient.disconnect();
        }
    }
    */

    /**
     * This returns the directory that is the application's root
     * (with forward slashes and a trailing slash, 
     * e.g., c:/programs/tomcat/webapps/cwexperimental/).
     * Tomcat calls this the ContextDirectory.
     * This only works if these classes are installed
     * underneath Tomcat (with "WEB-INF/" 
     * the start of things to be removed from classPath).
     *
     * @return the contextDirectory (with / separator and / at the end)
     * @throws Exception if trouble
     */
    public static String getContextDirectory() {
        if (contextDirectory == null) {
            String classPath = String2.getClassPath(); //with / separator and / at the end
            int po = classPath.indexOf("/WEB-INF/");
            contextDirectory = classPath.substring(0, po + 1);
        }

        return contextDirectory;
    }

    /**
     * This returns a directory for temporary files
     * (with forward slashes and a trailing slash, 
     * currently: <contextDirectory>WEB-INF/temp/).
     * This only works if these classes are installed
     * underneath Tomcat (or a path with "WEB-INF/", 
     * the start of things to be removed from classPath).
     *
     * @return the tempDirectory 
     * @throws Exception if trouble
     */
    public static String getTempDirectory() {
        if (tempDirectory == null) {
            getContextDirectory(); 
            String tdir = contextDirectory + "WEB-INF/temp/";
            //make it, because Git doesn't track empty dirs
            File2.makeDirectory(tdir);            
            //then set it if successful
            tempDirectory = tdir;
        }

        return tempDirectory;
    }



    /**
     * A simple, static class to display a URL in the system browser.
     * Copied with minimal changes from 
     *   http://www.javaworld.com/javaworld/javatips/jw-javatip66.html.
     *
     * <p>Under Unix, the system browser is hard-coded to be 'netscape'.
     * Netscape must be in your PATH for this to work.  This has been
     * tested with the following platforms: AIX, HP-UX and Solaris.
     *
     * <p>Under Windows, this will bring up the default browser under windows.
     * This has been tested under Windows 95/98/NT.
     *
     * <p>Examples:
     *   BrowserControl.displayURL("http://www.javaworld.com")
     *   BrowserControl.displayURL("file://c:\\docs\\index.html")
     *   BrowserContorl.displayURL("file:///user/joe/index.html");
     * 
     * <p>Note - you must include the url type -- either "http://" or
     * "file://".
     *
     * <p>2011-03-08 Before, this threw Exception if trouble. Now it doesn't.
     *
     * @param url the file's url (the url must start with either "http://"  or
     * "file://").
     * 
     */
    public static void displayInBrowser(String url) {
        try {
            String cmd = null;
            if (String2.OSIsWindows) {
                // The default system browser under windows.
                String WIN_PATH = "rundll32";
                // The flag to display a url.
                String WIN_FLAG = "url.dll,FileProtocolHandler";
                // cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
                cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
                Process p = Runtime.getRuntime().exec(cmd);
            } else {
                // Under Unix, Netscape has to be running for the "-remote"
                // command to work.  So, we try sending the command and
                // check for an exit value.  If the exit command is 0,
                // it worked, otherwise we need to start the browser.
                // cmd = 'netscape -remote openURL(http://www.javaworld.com)'
                // The default browser under unix.
                String UNIX_PATH = "netscape";
                // The flag to display a url.
                String UNIX_FLAG = "-remote openURL";
                cmd = UNIX_PATH + " " + UNIX_FLAG + "(" + url + ")";
                Process p = Runtime.getRuntime().exec(cmd);

                // wait for exit code -- if it's 0, command worked,
                // otherwise we need to start the browser up.
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    // Command failed, start up the browser
                    // cmd = 'netscape http://www.javaworld.com'
                    cmd = UNIX_PATH + " "  + url;
                    p = Runtime.getRuntime().exec(cmd);
                }
            }
        } catch (Throwable t) {
            String2.log(String2.ERROR + " while trying to displayInBrowser(" + url + "):\n" +
                MustBe.throwableToString(t) +
                (String2.OSIsWindows? "" :
                    "(On non-Windows computers, Netscape must be in your PATH for this to work.)"));
        }
    }



    /** 
     * This is a one time method to change the names of chl2 files in chla .zip's
     * to chla. This unzips, renames, re-zips the files.
     *
     * @param zipDir the dir with the chla .zip files
     * @param emptyDir needs to be an empty temporary directory
     */
    public static void changeChl2ToChla(String zipDir, String emptyDir) {
        String2.log("SSR.changeChl2ToChla zipDir=" + zipDir + " emptyDir=" + emptyDir);

        //get the names of all the chla files in zipDir
        String names[] = RegexFilenameFilter.fullNameList(zipDir, ".+chla.+\\.zip");

        //for each file
        int countRenamed = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                //unzip to temp dir
                unzip(names[i], emptyDir, true, 10);

                //if internal file was already chla, delete internal file and continue
                String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
                Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
                if (tNames[0].indexOf("chla") >= 0) {
                    File2.delete(emptyDir + tNames[0]);
                    continue;
                }
                String2.log("changing " + tNames[0]);

                //rename internal file
                Test.ensureTrue(tNames[0].indexOf("chl2") >= 0, "tNames[0] not chl2 file!");
                String newName = String2.replaceAll(tNames[0], "chl2", "chla");
                File2.rename(emptyDir, tNames[0], newName); 

                //delete old zip file
                File2.delete(names[i]);
                 
                //make new zip file
                zip(names[i], new String[]{emptyDir + newName}, 
                    10, false, ""); //false = don't include dir names 

                //delete internal file
                File2.delete(newName);

                countRenamed++;

            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }

            //empty the directory
            String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
            for (int j = 0; j < tNames2.length; j++)
                File2.delete(emptyDir + tNames2[j]);

        }
        String2.log("successfully changed " + countRenamed + " out of " + names.length + " chla .zip files.");

    }

    /** 
     * This is a one time method to change the names of GH files in GA .zip's
     * to GA. This unzips, renames, re-zips the files into their correct original location.
     *
     * @param zipDir the dir with the chla .zip files
     * @param emptyDir needs to be an empty temporary directory
     */
    public static void changeGHToGA(String zipDir, String emptyDir) {
        String2.log("SSR.changeGHToGA zipDir=" + zipDir + " emptyDir=" + emptyDir);

        //get the names of all the GA files in zipDir
        String names[] = RegexFilenameFilter.fullNameList(zipDir, "GA.+\\.zip");

        //for each file
        int countRenamed = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                //unzip to temp dir
                unzip(names[i], emptyDir, true, 10);

                //if internal file was already GA, delete internal file and continue
                String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
                Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
                if (tNames[0].startsWith("GA")) {
                    File2.delete(emptyDir + tNames[0]);
                    continue;
                }
                String2.log("changing " + tNames[0]);

                //rename internal file
                Test.ensureTrue(tNames[0].startsWith("GH"), "tNames[0] not GH file!");
                String newName = "GA" + tNames[0].substring(2);
                File2.rename(emptyDir, tNames[0], newName); 

                //delete old zip file
                File2.delete(names[i]);
                 
                //make new zip file
                zip(names[i], new String[]{emptyDir + newName}, 
                    10, false, ""); //false = don't include dir names 

                //delete internal file
                File2.delete(newName);

                countRenamed++;

            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }

            //empty the directory
            String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
            for (int j = 0; j < tNames2.length; j++)
                File2.delete(emptyDir + tNames2[j]);

        }
        String2.log("successfully changed " + countRenamed + " out of " + names.length + " GA .zip files.");

    }

    /** 
     * This is a one time method to change the names of GA0 files in GA .zip's
     * to GA20. This unzips, renames, re-zips the files into their correct original location.
     *
     * @param zipDir the dir with the chla .zip files
     * @param emptyDir needs to be an empty temporary directory
     */
    public static void changeGA0ToGA20(String zipDir, String emptyDir) {
        String2.log("SSR.changeGA0ToGA20 zipDir=" + zipDir + " emptyDir=" + emptyDir);

        //get the names of all the GA files in zipDir
        String names[] = RegexFilenameFilter.fullNameList(zipDir, "GA.+\\.zip");

        //for each file
        int countRenamed = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                //unzip to temp dir
                unzip(names[i], emptyDir, true, 10);

                //if internal file was already GA, delete internal file and continue
                String tNames[] = RegexFilenameFilter.list(emptyDir, ".+");
                Test.ensureEqual(tNames.length, 1, "nFiles in .zip not 1!");
                if (tNames[0].startsWith("GA20")) {
                    File2.delete(emptyDir + tNames[0]);
                    continue;
                }
                String2.log("changing " + tNames[0]);

                //rename internal file
                Test.ensureTrue(tNames[0].startsWith("GA0"), "tNames[0] not GA0 file!");
                String newName = "GA2" + tNames[0].substring(2);
                File2.rename(emptyDir, tNames[0], newName); 

                //delete old zip file
                File2.delete(names[i]);
                 
                //make new zip file
                zip(names[i], new String[]{emptyDir + newName}, 
                    10, false, ""); //false = don't include dir names 

                //delete internal file
                File2.delete(newName);

                countRenamed++;

            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }

            //empty the directory
            String tNames2[] = RegexFilenameFilter.list(emptyDir, ".+");
            for (int j = 0; j < tNames2.length; j++)
                File2.delete(emptyDir + tNames2[j]);

        }
        String2.log("successfully changed " + countRenamed + " out of " + names.length + " GA0 .zip files.");

    }

    /** 
     * This is a one time method to enclose each of the files in a directory
     * in its own zip file.
     *
     * @param dir 
     */
    public static void zipEach(String dir) {
        String2.log("SSR.zipEach dir=" + dir);

        //get the names of all the files dir
        String names[] = RegexFilenameFilter.fullNameList(dir, ".+");

        //for each file
        int countRenamed = 0;
        for (int i = 0; i < names.length; i++) {
            try {
                //make new zip file
                zip(names[i] + ".zip", new String[]{names[i]}, 
                    10, false, ""); //false = don't include dir names 

                countRenamed++;

            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }

        }
        String2.log("successfully zipped " + countRenamed + " out of " + names.length + " .zip files.");

    }

    /** 
     * THIS IS NOT TESTED -- IT MAY NOT WORK.
     * This wraps the xml in a SOAP envelope, POSTs it to the url, 
     * and returns the response.
     * !This doesn't yet support character encodings -- for now, stick to ASCII.
     * 
     * Adapted from Bob DuCharme's SOAPClient4XG
     * (http://www-128.ibm.com/developerworks/xml/library/x-soapcl/ , Listing 1).
     *
     * @param urlString where the xml will be sent
     * @param xml the xml to be sent (without the soap envelope)
     * @param soapAction use "" for no specific action
     * @return the raw soap envelope response (as in inputStream)
     * @throws Exception if trouble
     */
/*    public static InputStream getSoapInputStream(String urlString, String xml, 
        String soapAction) throws Exception {

        //put xml in soap wrapper
        String soapXml = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >\n" +
            "  <soapenv:Body>\n" +
            xml +
            "  </soapenv:Body>\n" +
            "</soapenv:Envelope>\n";

        //create the connection where we're going to send the file
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        HttpURLConnection httpConn = (HttpURLConnection)connection;

        //set the appropriate HTTP parameters
        httpConn.setRequestProperty("Content-Length", "" + soapXml.length());
        httpConn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");  //utf-8, not true
        httpConn.setRequestProperty("SOAPAction", soapAction == null? "" : soapAction);
        
        httpConn.setRequestMethod("POST");
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);

        //send the XML
        OutputStream out = httpConn.getOutputStream();
        out.write(String2.toByteArray(soapXml));   //!!!should be to encoded to UTF-8
        out.close();

        //read the response
        return httpConn.getInputStream();
    } 
*/
    /** 
     * THIS IS NOT TESTED -- IT MAY NOT WORK.
     * This wraps the xml in a SOAP envelope, POSTs it to the url, 
     * and returns the response as a newline separated string.
     * !This doesn't yet support character encodings -- for now, stick to ASCII.
     * 
     * Adapted from Bob DuCharme's SOAPClient4XG
     * (http://www-128.ibm.com/developerworks/xml/library/x-soapcl/ , Listing 1).
     *
     * @param urlString where the xml will be sent
     * @param xml the xml to be sent (without the soap envelope)
     * @param soapAction use "" for no specific action
     * @return the raw soap envelope response (a newline-separated string)
     * @throws Exception if trouble
     */
/*    public static String getSoapString(String urlString, String xml, String soapAction) throws Exception {

        InputStream is = getSoapInputStream(urlString, xml, soapAction);
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder(); 
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            response.append('\n');
        }
        in.close();

        return response.toString();
    } 
*/
    /** 
     * This POSTs the content to the url, and returns the response.
     * 
     * @param urlString where the content will be sent
     * @param mimeType e.g., "text/xml"
     * @param encoding e.g., "UTF-8"
     * @param content the content to be sent
     * @throws Exception if trouble
     */
    public static InputStream getPostInputStream(String urlString, 
        String mimeType, String encoding, String content) throws Exception {

        //create the connection where we're going to send the file
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();

        //set the appropriate HTTP parameters
        //con.setRequestProperty("Content-Length", "" + content.length()); //not required, and I'm confused about pre/post encoding length
        con.setRequestProperty("Content-Type", mimeType);
        con.setRequestProperty("Content-Encoding", encoding);        
        con.setRequestProperty("Accept-Encoding", 
            "gzip, x-gzip, deflate"); //no compress, x-compress, since zip Entries are hard to deal with
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setDoInput(true);

        //send the content
        OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream(), encoding);
        writer.write(content);  
        writer.flush();
        writer.close();

        //read the response
        InputStream is = con.getInputStream();
        String responseEncoding = con.getContentEncoding();
        String2.log("  getPostInputStream url = " + urlString +   //diagnostic
            //"  headerFields=" + String2.toString(con.getHeaderFields()) +
            " encoding=" + responseEncoding);
        if (responseEncoding != null) {
            responseEncoding = responseEncoding.toLowerCase();
            //if (encoding.indexOf("compress") >= 0)
            //    is = new ZipInputStream(is);
            //else 
            if (responseEncoding.indexOf("gzip") >= 0) {
                //String2.log("using gzip!");
                is = new GZIPInputStream(is);
            } else if (responseEncoding.indexOf("deflate") >= 0) {
                //String2.log("using deflate!");
                is = new InflaterInputStream(is);
            }
        }
        return is;
    } 

    /* A test setup for  getPostInputStream */
    /*public static void testPost() throws Exception {
        //see examples at http://demo.transducerml.org:8080/ogc/
        String url, content;
        url = "http://demo.transducerml.org:8080/ogc/sos";
        content = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<GetObservation xmlns=\"http://www.opengeospatial.net/sos\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:ows=\"http://www.opengeospatial.net/ows\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengeospatial.net/sos ../sosGetObservation.xsd\" service=\"SOS\" version=\"0.0.31\"> \n" +
            "<offering>IRIS01_WEATHER</offering> \n" +
            "<eventTime> \n" +
            "<ogc:After><gml:TimeInstant><gml:timePosition indeterminatePosition=\"now\"></gml:timePosition></gml:TimeInstant></ogc:After> \n" +
            "</eventTime> \n" +
            "<observedProperty>WEATHER_OBSERVABLES</observedProperty> \n" +
            "<resultFormat>text/xml; subtype=tml/1.0</resultFormat> \n" +
            "</GetObservation>\n";

        copy(getPostInputStream(url, "text/xml", "UTF-8", content), System.out);
    }*/


    /**
     * Copy from source to outputStream.
     *
     * @param source May be local fileName or a URL.
     * @param out At the end, out is flushed, but not closed
     * @return true if successful
     */
    public static boolean copy(String source, OutputStream out) {
        if (source.startsWith("http://") ||
            source.startsWith("https://") ||  //untested.
            source.startsWith("ftp://")) {    //untested. presumably anonymous
            //URL
            InputStream in = null;
            try {
                in = (InputStream)(getUrlConnInputStream(source, 
                    120000)[1]); //timeOutMillis. throws Exception
                copy(in, out);
                return true;
            } catch (Exception e) {
                String2.log(String2.ERROR + " in SSR.copy(source=" + source + ")\n" +
                    MustBe.throwableToString(e));
                return false;
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (Exception e2) {
                } 
            }

        } else {
            //presumably a file
            return File2.copy(source, out);
        }
    }


    /** 
     * A method to copy the info from an inputStream to an outputStream.
     * Based on E.R. Harold's book "Java I/O".
     *
     * @param in At the end, in isn't closed.
     * @param out At the end, out is flushed, but not closed
     * @throws IOException if trouble
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {

        // do not allow other threads to read from the
        // input or write to the output while copying is taking place
        synchronized (in) {
            synchronized (out) {
                byte[] buffer = new byte[32768];
                int nRead;
                while ((nRead = in.read(buffer)) >= 0) //0 shouldn't happen. -1=end of file
                    out.write(buffer, 0, nRead);
                out.flush();
            }
        }
    } 

    //public static void main(String args[]) {
        //usage 
        //cd /usr/local/jakarta-tomcat-5.5.4/webapps/cwexperimental/WEB-INF/
        //java -cp ./classes:./lib/mail.jar:./lib/netcdf-latest.jar:./lib/slf4j-jdk14.jar:./lib/nlog4j-1.2.21.jar gov.noaa.pfel.coastwatch.util.SSR /u00/data/GA/hday/grd/
        //the new files replace the old files
        //changeGA0ToGA20(args[0], "/u00/cwatch/bobtemp/");
    //}
}


