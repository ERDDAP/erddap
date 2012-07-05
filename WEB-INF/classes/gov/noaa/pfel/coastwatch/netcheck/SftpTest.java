/* 
 * SftpTest Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;


/**
 * This deals with one type of netCheck test: the ability to 
 * send a local file to a remote computer by SFTP, rename it,
 * bring it back, and verify that the contents are unchanged.
 *
 * <p>See Java docs for NetCheck class regarding SSR.sftp and the classes it needs
 * access to.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-08-16
 *
 */
public class SftpTest extends NetCheckTest {


    private String hostName;
    private String userName;
    private String password; //it is important that this be private
    private String localDirectory;
    private String remoteDirectory;
    private String fileName;

    /**
     * This constructor loads the information for a test with information from the xmlReader.
     * The xmlReader.getNextTag() should have just returned with the
     * initial reference to this class.
     * The method will read information until xmlReader.getNextTag()
     * returns the close tag for the reference to this class.
     *
     * @param xmlReader
     * @throws Exception if trouble
     */
    public SftpTest(SimpleXMLReader xmlReader) throws Exception {
        String errorIn = String2.ERROR + " in SftpTest constructor: ";

        //ensure the xmlReader is just starting with this class
        Test.ensureEqual(xmlReader.allTags(), "<netCheck><sftpTest>", 
            errorIn + "incorrect initial tags.");
        
        //read the xml properties file
        xmlReader.nextTag();
        String tags = xmlReader.allTags();
        int iteration = 0;
        while (!tags.equals("<netCheck></sftpTest>") && iteration++ < 1000000) {
            //process the tags
            if (verbose) String2.log(tags + xmlReader.content());
            if      (tags.equals("<netCheck><sftpTest><title>")) {} 
            else if (tags.equals("<netCheck><sftpTest></title>")) 
                title = xmlReader.content();
            else if (tags.equals("<netCheck><sftpTest><hostName>")) {} 
            else if (tags.equals("<netCheck><sftpTest></hostName>")) 
                hostName = xmlReader.content();
            else if (tags.equals("<netCheck><sftpTest><userName>")) {} 
            else if (tags.equals("<netCheck><sftpTest></userName>")) 
                userName = xmlReader.content();
            else if (tags.equals("<netCheck><sftpTest><localDirectory>")) {} 
            else if (tags.equals("<netCheck><sftpTest></localDirectory>")) 
                localDirectory = xmlReader.content();
            else if (tags.equals("<netCheck><sftpTest><remoteDirectory>")) {} 
            else if (tags.equals("<netCheck><sftpTest></remoteDirectory>")) 
                remoteDirectory = xmlReader.content();
            else if (tags.equals("<netCheck><sftpTest><fileName>")) {} 
            else if (tags.equals("<netCheck><sftpTest></fileName>")) 
                fileName = xmlReader.content();
            else if (tags.equals("<netCheck><sftpTest><mustRespondWithinSeconds>")) {} 
            else if (tags.equals("<netCheck><sftpTest></mustRespondWithinSeconds>"))
                mustRespondWithinSeconds = String2.parseDouble(xmlReader.content());
            else if (tags.equals("<netCheck><sftpTest><emailStatusTo>")) {} 
            else if (tags.equals("<netCheck><sftpTest></emailStatusTo>")) 
                emailStatusTo.add(xmlReader.content());
            else if (tags.equals("<netCheck><sftpTest><emailStatusHeadlinesTo>")) {} 
            else if (tags.equals("<netCheck><sftpTest></emailStatusHeadlinesTo>")) 
                emailStatusHeadlinesTo.add(xmlReader.content());
            else if (tags.equals("<netCheck><sftpTest><emailChangesTo>")) {} 
            else if (tags.equals("<netCheck><sftpTest></emailChangesTo>")) 
                emailChangesTo.add(xmlReader.content());
            else if (tags.equals("<netCheck><sftpTest><emailChangeHeadlinesTo>")) {} 
            else if (tags.equals("<netCheck><sftpTest></emailChangeHeadlinesTo>")) 
                emailChangeHeadlinesTo.add(xmlReader.content());
            else throw new RuntimeException(errorIn + "unrecognized tags: " + tags);
            
            //get the next tags
            xmlReader.nextTag();
            tags = xmlReader.allTags();
        }

        //ensure that the required values are set
        ensureValid();
    }

    /**
     * A constructor for setting up the test.
     * You can set other values with their setXxx or addXxx methods of the
     * superclass NetCheckTest.
     *
     * @param title e.g., THREDDS Opendap GAssta
     * @param hostName
     * @param userName
     * @param localDirectory
     * @param remoteDirectory
     * @param fileName
     * @throws Exception if trouble
     */
    public SftpTest(String title, String hostName, String userName, 
        String localDirectory, String remoteDirectory, String fileName) throws Exception {

        String errorIn = String2.ERROR + " in SftpTest constructor: ";
    
        //required 
        this.title = title;
        this.hostName = hostName;
        this.userName = userName;
        this.localDirectory = localDirectory;
        this.remoteDirectory = remoteDirectory;
        this.fileName = fileName;

        //ensure that the required values are set
        ensureValid();
    }

    /**
     * This is used by the constructors to ensure that all the required values were set.
     *
     * <p>This is unusual in that it asks for the password via SystemIn.
     * This is the only way to get the password into this class.
     * This approach prevents passwords from being stored in code (e.g., unit tests).
     *
     * @throws Exception if trouble
     */
    public void ensureValid() throws Exception {
        String errorIn = String2.ERROR + " in SftpTest.ensureValid: ";

        //ensure that required items were set
        Test.ensureEqual(title == null || title.length() == 0, false, 
            errorIn + "<netCheck><sftpTest><title> was not specified.\n");
        Test.ensureEqual(hostName == null || hostName.length() == 0, false,
            errorIn + "<netCheck><sftpTest><hostName> was not specified.\n");
        Test.ensureEqual(userName == null || userName.length() == 0, false,
            errorIn + "<netCheck><sftpTest><userName> was not specified.\n");
        Test.ensureEqual(localDirectory == null || localDirectory.length() == 0, false,
            errorIn + "<netCheck><sftpTest><localDirectory> was not specified.\n");
        Test.ensureTrue(localDirectory.endsWith("/") || localDirectory.endsWith("\\"), 
            errorIn + "<netCheck><sftpTest><localDirectory> must end with a (back)slash.\n");
        Test.ensureTrue(remoteDirectory != null && remoteDirectory.length() > 0, 
            errorIn + "<netCheck><sftpTest><remoteDirectory> was not specified.\n");
        Test.ensureTrue(remoteDirectory.endsWith("/") || remoteDirectory.endsWith("\\"),
            errorIn + "<netCheck><sftpTest><remoteDirectory> must end with a (back)slash.\n");
        Test.ensureTrue(fileName != null && fileName.length() > 0,
            errorIn + "<netCheck><sftpTest><fileName> was not specified.\n");
        Test.ensureTrue(File2.isFile(localDirectory + fileName), 
            errorIn + "<netCheck><sftpTest><fileName> The file (" + 
            localDirectory + fileName + ") doesn't exist.\n");
        //mustRespondWithinSeconds... )    //not required
        //emailResultsTo.size()      == 0) {} //not required
        //emailChangesTo.size()   == 0) {} //not required
        //emailResultHeadlinesTo.size()      == 0) {} //not required
        //emailChangeHeadlinesTo.size()      == 0) {} //not required

        //get the password
        password = String2.getPasswordFromSystemIn(
            "Sftp account password for " + userName + " on " + hostName + "? "); 
        Test.ensureEqual(password == null || password.length() == 0, false,
            errorIn + "You must specify a password.\n");

    }

    /**
     * This does the test and returns an error string ("" if no error).
     * This does not send out emails.
     * This won't throw an Exception.
     *
     * @return an error string ("" if no error).
     *    If there is an error, this will end with '\n'.
     *    If the error has a short section followed by a longer section,
     *    NetCheckTest.END_SHORT_SECTION will separate the two sections.
     *    
     */
    public String test() {
        int random1 = Math2.random(Integer.MAX_VALUE);
        int random2 = Math2.random(Integer.MAX_VALUE);
        while (random1 == random2) 
            random2 = Math2.random(Integer.MAX_VALUE);
 
        String tempFileName1 = fileName + "." + random1;
        String tempFileName2 = fileName + "." + random2;
        try {
            //copy fileName to tempFileName1
            File2.copy(localDirectory + fileName, localDirectory + tempFileName1);

            //do the test
            long time = System.currentTimeMillis();
            String command = 
                "lcd " + localDirectory + "\n" + //cd local dir
                "cd " + remoteDirectory + "\n" + //cd remote dir
                "put " + tempFileName1 + "\n" + //upload the file
                "rename " + tempFileName1 + " " + tempFileName2 + "\n" + //rename it
                "get " + tempFileName2 + "\n" + //download the temp file
                "rm " + tempFileName2; //delete the remote file (temp version)
            try {
                SSR.sftp(hostName, userName, password, command);
            } catch (Exception e) {
                String error = String2.replaceAll(MustBe.throwableToString(e), "\n", "\n    ");
                return "  " + String2.ERROR + " from SSR.sftp: " + error + "\n";
            }
            time = System.currentTimeMillis() - time;
            
            //check mustRespondWithinSeconds
            StringBuilder errorSB = new StringBuilder();
            if (Math2.isFinite(mustRespondWithinSeconds) && time > mustRespondWithinSeconds * 1000) {
                errorSB.append("  " + String2.ERROR + ": response time (" + (time/1000.0) + 
                    " s) was too slow (mustRespondWithinSeconds = " + 
                    mustRespondWithinSeconds + ").\n");
            }          

            //check if the file changed
            long differAt = File2.whereDifferent(localDirectory + tempFileName1,
                localDirectory + tempFileName2);
            if (differAt >= 0) 
                errorSB.append("  " + String2.ERROR + ": the returned file is different at byte #" +
                    differAt +".\n");

            //if there was trouble, include info (at the start) of the error message
            if (errorSB.length() > 0) {
                errorSB.insert(0, getDescription());
            }

            return errorSB.toString();
        } catch (Exception e) {

            return MustBe.throwable("SftpTest.test\n" + getDescription(), e);
        } finally {
            try {
                //delete the temp files
                File2.delete(localDirectory + tempFileName1);
                File2.delete(localDirectory + tempFileName2);
            } catch (Exception e2) {
                //don't care
            }
        }
    }

    /**
     * This returns a description of this test (suitable for putting at
     * the top of an error message), with "  " at the start and \n at the end.
     *
     * @return a description
     */
    public String getDescription() {
        return 
            "  hostName: " + hostName + "\n" +
            "  userName: " + userName + "\n" +
            "  localDirectory: " + localDirectory + "\n" +
            "  remoteDirectory: " + remoteDirectory + "\n" +
            "  fileName: " + fileName + "\n";
    }

    /**
     * THIS TEST DOESN'T WORK - orpheus Shell authentication started failing ~2010-06.
     * A unit test of this class.
     *
     * @throws Exception if trouble
     */
    public static void unitTest() throws Exception {
        String2.log("\n*** netcheck.SftpTest");
        long time = System.currentTimeMillis();
        SftpTest sftpTest = new SftpTest("Orpheus SFTP Functionality", 
            "orpheus.pfeg.noaa.gov", //url
            "cwatch", //user
            String2.getClassPath() + "gov/noaa/pfel/coastwatch/netcheck/", //localDir
            "/Volumes/PFEL/FTPServer/FTPRoot/uploads/cwatch/", //remoteDir
            "SftpTestFile"); //fileName
        sftpTest.setMustRespondWithinSeconds(30);
        String2.log(sftpTest.getDescription());
        String error = sftpTest.test();
        Test.ensureEqual(error, "", String2.ERROR + " in SftpTest.unitTest:\n" + error);
        String2.log("netcheck.SftpTest finished successfully   time=" + 
            (System.currentTimeMillis() - time));
    }


}
