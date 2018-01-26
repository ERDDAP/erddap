/* 
 * TestSSR Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Properties;

/**
 * This is a Java program to test all of the methods in SSR.
 *
 * @author Robert Simons  bob.simons@noaa.gov  January 2005
 */
public class TestSSR {

    static String classPath = String2.getClassPath(); //with / separator and / at the end

    /**
     * Run all of the tests which are operating system independent.
     */
    public static void runNonUnixTests() throws Throwable {
        String sar[];

        String2.log("\n*** TestSSR"); 
        String2.log("This must be run in a command line window so passwords can be entered!");

        /*
        //sendSoap
        String soap = SSR.sendSoap(
            "http://services.xmethods.net:80/soap/servlet/rpcrouter",
            "    <ns1:getTemp xmlns:ns1=\"urn:xmethods-Temperature\"\n" +
            "      SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "        <zipcode xsi:type=\"xsd:string\">93950</zipcode>\n" +
            "     </ns1:getTemp>\n",
            "");
        String2.log(soap);
        System.exit(0);
        */

        //percentDecode(String query) 
        String s = "~`!@#$%^&*()_-+=|\\{[}]:;\"'<,>.?/ a\nA°1"; //after A is degree #176/B0
        Test.ensureEqual(SSR.percentEncode(s),                  //it is utf-8'd then % encoded
            //note that I modified Java code so ' ' becomes %20, not +
            "%7E%60%21%40%23%24%25%5E%26*%28%29_-%2B%3D%7C%5C%7B%5B%7D%5D%3A%3B%22%27%3C%2C%3E.%3F%2F%20a%0AA%C2%B01",
            ""); //It encodes ~!*()' 
        Test.ensureEqual(SSR.minimalPercentEncode(s),//                                        
            "~%60!%40%23%24%25%5E%26*()_-%2B%3D%7C%5C%7B%5B%7D%5D%3A%3B%22'%3C%2C%3E.%3F%2F%20a%0AA%C2%B01", "");
        Test.ensureEqual(SSR.percentDecode("%2B%20%3Aq*~%3F%3D%26%25"), "+ :q*~?=&%", "");

        s = "AZaZ09 \t\r\n`";
        Test.ensureEqual(SSR.minimalPercentEncode(s), 
            "AZaZ09%20%09%0D%0A%60", "");
        Test.ensureEqual(SSR.percentEncode(s), 
            "AZaZ09%20%09%0D%0A%60", ""); 
        Test.ensureEqual(SSR.percentDecode("AZaZ09%20%09%0D%0A%60"), s, "");

        s = "~!@#$%^&*()";
        Test.ensureEqual(SSR.minimalPercentEncode(s), 
            "~!%40%23%24%25%5E%26*()", "");
        Test.ensureEqual(SSR.percentEncode(s), 
            "%7E%21%40%23%24%25%5E%26*%28%29", "");
        Test.ensureEqual(SSR.percentDecode("%7E%21%40%23%24%25%5E%26*%28%29"), s, "");

        s = "-_=+\\|[{]};";
        Test.ensureEqual(SSR.minimalPercentEncode(s), 
            "-_%3D%2B%5C%7C%5B%7B%5D%7D%3B", "");
        Test.ensureEqual(SSR.percentEncode(s), 
            "-_%3D%2B%5C%7C%5B%7B%5D%7D%3B", "");
        Test.ensureEqual(SSR.percentDecode("-_%3D%2B%5C%7C%5B%7B%5D%7D%3B"), s, "");

        s = ":'\",<.>/?";
        Test.ensureEqual(SSR.minimalPercentEncode(s), 
            "%3A'%22%2C%3C.%3E%2F%3F", "");
        Test.ensureEqual(SSR.percentEncode(s), 
            "%3A%27%22%2C%3C.%3E%2F%3F", "");
        Test.ensureEqual(SSR.percentDecode("%3A%27%22%2C%3C.%3E%2F%3F"), s, "");

        /* 2014-08-05 DEACTIVATED BECAUSE NOT USED. IF NEEDED, SWITCH TO Apache commons-net???
        //sftp
        String2.log("test sftp");
        String password = String2.getPasswordFromSystemIn(String2.beep(1) +
            "cwatch password for coastwatch computer (enter \"\" to skip the test)? ");
        if (password.length() > 0) {
            try {
                String fileName = "Rainbow.cpt";
                StringBuilder cmds = new StringBuilder(
                    "lcd " + SSR.getTempDirectory() + "\n" +
                    "cd /u00/cwatch/bobtemp\n" +  //on coastwatch computer; don't use String2.testU00Dir
                    "get " + fileName);
                File2.delete(SSR.getTempDirectory() + fileName);
                SSR.sftp("coastwatch.pfeg.noaa.gov", "cwatch", password, cmds.toString());
                Test.ensureEqual(File2.length(SSR.getTempDirectory() + fileName), 214, "a");
                File2.delete(SSR.getTempDirectory() + fileName);
            } catch (Exception e) {
                String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                    "\nUnexpected error."); 
            }
        }
        Math2.sleep(3000); //allow j2ssh to finish closing and writing messages
        */

        //SSR.windowsSftp never worked (authentication trouble) and SSR.sftp is
        //  better anyway because it is Java-based and therefore platform independent.
        //SSR.windowsSftp("coastwatch.pfeg.noaa.gov", "cwatch", password, 
        //    "\\temp\\", "/usr/local/jakarta-tomcat-5.5.4/webapps/cwexperimental/WEB-INF/secure/", 
        //    new String[]{"btemplate.xml"}, //send
        //    new String[]{}, 10); //receive

        //dosShell
        String2.log("test dosShell");
        String tempGif = SSR.getContextDirectory() + //with / separator and / at the end
            "images/temp.gif";
        File2.delete(tempGif);
        try {
            Test.ensureEqual(
                String2.toNewlineString(SSR.dosShell(
                    "\"C:\\Program Files (x86)\\ImageMagick-6.8.0-Q16\\convert\" " +
                    SSR.getContextDirectory() + //with / separator and / at the end
                        "images/subtitle.jpg " +
                    tempGif, 10).toArray()),
                "", "dosShell a");
            Test.ensureTrue(File2.isFile(tempGif), "dosShell b");
        } catch (Exception e) {
            Test.knownProblem(
                "IMAGEMAGICK NOT SET UP ON BOB'S DELL M4700.",
                MustBe.throwableToString(e));
        }
        File2.delete(tempGif);

        //cutChar
        String2.log("test cutChar");
        Test.ensureEqual( SSR.cutChar("abcd", 2, 3), "bc", "a");
        Test.ensureEqual( SSR.cutChar("abcd", 1, 4), "abcd", "b");
        Test.ensureEqual( SSR.cutChar("abcd", 0, 4), "abcd", "c");
        Test.ensureEqual( SSR.cutChar("abcd", 1, 5), "abcd", "d");
        Test.ensureEqual( SSR.cutChar("abcd", 3, 3), "c", "e");
        Test.ensureEqual( SSR.cutChar("abcd", 4, 1), "", "f");
        Test.ensureEqual( SSR.cutChar("abcd", -2, 0), "", "g");

        Test.ensureEqual( SSR.cutChar("abcd", 1), "abcd", "a");
        Test.ensureEqual( SSR.cutChar("abcd", 0), "abcd", "b");
        Test.ensureEqual( SSR.cutChar("abcd", -1), "abcd", "c");
        Test.ensureEqual( SSR.cutChar("abcd", 2), "bcd", "d");
        Test.ensureEqual( SSR.cutChar("abcd", 4), "d", "e");
        Test.ensureEqual( SSR.cutChar("abcd", 5), "", "f");

        //make a big chunk of text
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000000; i++) 
            sb.append("This is a really, really, long line of text to text compression speed.\n");
        String longText = sb.toString();
        sb = null;
        String testMB = "  " + (longText.length() / Math2.BytesPerMB) + " MB";

        //zip without directory info
        String2.log("\n* test zip without dir info" + testMB);
        String middleDir = "gov/noaa/pfel/coastwatch/";
        String zipDir = classPath + middleDir;
        String zipName = "TestSSR.zip";
        String fileName = "TestSSR.txt";
        //write a longText file
        Test.ensureEqual(String2.writeToFile(zipDir + fileName, longText), "", "SSR.zip a");
        //make the zip file
        File2.delete(zipDir + zipName);
        long time1 = System.currentTimeMillis();
        SSR.zip(zipDir + zipName, new String[]{zipDir + fileName}, 10);
        time1 = System.currentTimeMillis() - time1;
        File2.delete(zipDir + fileName);
        //unzip the zip file
        long time2 = System.currentTimeMillis();
        SSR.unzip(zipDir + zipName, zipDir, //note: extract to zipDir, since it doesn't include dir
            false, 10, null); //false 'ignoreDirectoryInfo', but there is none
        time2 = System.currentTimeMillis() - time2;
        //ensure results are as expected
        String[] results = String2.readFromFile(zipDir + fileName);
        Test.ensureEqual(results[0], "", "SSR.zip b");
        Test.ensureEqual(results[1], longText, "SSR.zip c");
        String2.log("zip+unzip time=" + (time1 + time2) + "ms  (Java 1.7M4700 967ms, 1.6 4000-11000ms)");
        File2.delete(zipDir + zipName);
        File2.delete(zipDir + fileName);

        //zip with directory info
        String2.log("\n* test zip with dir info" + testMB);
        //write a longText file
        Test.ensureEqual(String2.writeToFile(zipDir + fileName, longText), "", "SSR.zip a");
        //make the zip file
        File2.delete(zipDir + zipName);
        time1 = System.currentTimeMillis();
        SSR.zip(zipDir + zipName, new String[]{zipDir + fileName}, 10, classPath);
        time1 = System.currentTimeMillis() - time1;
        File2.delete(zipDir + fileName);
        //unzip the zip file
        time2 = System.currentTimeMillis();
        SSR.unzip(zipDir + zipName, classPath, //note: extract to classPath, since includes dir
            false, 10, null); //false 'ignoreDirectoryInfo'
        time2 = System.currentTimeMillis() - time2;
        //ensure results are as expected
        results = String2.readFromFile(zipDir + fileName);
        Test.ensureEqual(results[0], "", "SSR.zip b");
        Test.ensureEqual(results[1], longText, "SSR.zip c");  
        String2.log("zip+unzip (w directory info) time=" + (time1 + time2) + 
            "ms  (Java 1.7M4700 937, 1.6 ~4000-11000ms)");
        File2.delete(zipDir + zipName);
        File2.delete(zipDir + fileName);

        //gzip without directory info
        for (int rep = 0; rep < 2; rep++) {
            String2.log("\n* test gzip without dir info" + testMB);
            middleDir = "gov/noaa/pfel/coastwatch/";
            String gzipDir = classPath + middleDir;
            String gzipName = "TestSSRG.txt.gz";
            fileName = "TestSSRG.txt";
            //write a longText file
            Test.ensureEqual(String2.writeToFile(gzipDir + fileName, longText), "", "SSR.gz a");
            //make the gzip file
            File2.delete(gzipDir + gzipName);
            time1 = System.currentTimeMillis();
            SSR.gzip(gzipDir + gzipName, new String[]{gzipDir + fileName}, 10); //don't include dir info
            time1 = System.currentTimeMillis() - time1;
            File2.delete(gzipDir + fileName);
            //unzip the gzip file
            time2 = System.currentTimeMillis();
            SSR.unGzip(gzipDir + gzipName, gzipDir, //note: extract to classPath, since doesn't include dir
                true, 10); //false 'ignoreDirectoryInfo'
            time2 = System.currentTimeMillis() - time2;
            //ensure results are as expected
            results = String2.readFromFile(gzipDir + fileName);
            Test.ensureEqual(results[0], "", "SSR.gz b");
            Test.ensureEqual(results[1], longText, "SSR.z c");
            String2.log("gzip+ungzip time=" + (time1 + time2) + 
                "ms  (Java 1.7M4700 780-880ms, 1.6 ~4000-11000ms)"); 
            File2.delete(gzipDir + gzipName);
            File2.delete(gzipDir + fileName);
        }

        testEmail();
        
        //getURLResponse (which uses getURLInputStream)
        //future: test various compressed url's
        String2.log("test getURLResponse");
        try {
            sar = SSR.getUrlResponseLines("https://www.pfeg.noaa.gov/"); //"http://www.cohort.com");
            Test.ensureEqual(
                String2.lineContaining(sar, "Disclaimer and Privacy Policy") == -1, //"A free RPN scientific calculator applet") == -1,
                false, "Response=" + String2.toNewlineString(sar));
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.pressEnterToContinue("\nRecover from failure?");
        }

        //test non-existent file
        long rTime = System.currentTimeMillis();
        try {
            sar = SSR.getUrlResponseLines("http://coastwatch.pfeg.noaa.gov/zzz.html");
            throw new Throwable("shouldn't get here.");
        } catch (Exception e) { //not throwable
            String2.log("SSR.getUrlResponse for non existent url time=" + (System.currentTimeMillis() - rTime) + "ms");
            //String2.pressEnterToContinue();
        } catch (Throwable t) {
            Test.error(t.toString()); //converts it to Exception and stops the testing
        }

        //note there is no continuity (session cookie isn't being sent)
        //but you can put as many params on one line as needed (from any screen)
        //and put edit=... to determine which screen gets returned
        try {
            sar = SSR.getUrlResponseLines("http://coastwatch.pfeg.noaa.gov/coastwatch/CWBrowser.jsp?edit=Grid+Data");
            String2.log("****beginResponse\n" + String2.toNewlineString(sar) + "\n****endResponse");
            Test.ensureNotEqual(String2.lineContaining(sar, "Download the grid data:"), -1, "e");
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "\nUnexpected error."); 
        }

        //postHTMLForm     (always right after contact the web site above)
        //I NEVER GOT THIS WORKING. JUST USE 'GET' TESTS ABOVE
//        String2.log("test postHTMLForm");

        //for apache commons version
//        sar = SSR.postHTMLForm("http://coastwatch.pfeg.noaa.gov/cwexperimental/", "CWBrowser.jsp",
//            new String[]{"edit", "Bathymetry"});

        //for devx version
        //sar = SSR.postHTMLForm("http://coastwatch.pfeg.noaa.gov/cwexperimental/CWBrowser.jsp",
        //    new Object[]{"bathymetry", "false", "edit", "Bathymetry"});

        //for java almanac version
        //sar = SSR.postHTMLForm("coastwatch.pfeg.noaa.gov", "/cwexperimental/CWBrowser.jsp",
        //     "edit=Bathymetry");

//        String2.log("****beginResponse\n" + String2.toNewlineString(sar) + "\n****endResponse");
//        Test.ensureNotEqual(String2.lineContaining(sar, "1) Draw bathymetry lines:"), -1, "a");

        //getFirstLineStartsWith
        String2.log("test getFirstLineStartsWith");
        String tFileName = classPath + "testSSR.txt";
        String2.writeToFile(tFileName, "This is\na file\nwith a few lines.");
        Test.ensureEqual(SSR.getFirstLineStartsWith(tFileName, "with "), "with a few lines.", "a");
        Test.ensureEqual(SSR.getFirstLineStartsWith(tFileName, "hi "), null, "b");

        //getFirstLineMatching
        String2.log("test getFirstLineMatching");
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, ".*?i.*"),        "This is",           "a"); //find first of many matches
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, "^a.*"),          "a file",            "b"); //start of line
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, ".*?\\sfew\\s.*"),"with a few lines.", "c"); //containing
        Test.ensureEqual(SSR.getFirstLineMatching(tFileName, "q"),             null,                "d"); //no match

        Test.ensureTrue(File2.delete(tFileName), "delete " + tFileName);

        //getContextDirectory
        String2.log("test getContextDirectory current=" + 
            SSR.getContextDirectory()); //with / separator and / at the end
        //there is no way to test this and have it work with different installations
        //test for my computer (comment out on other computers):
        //ensureEqual(String2.getContextDirectory(), //with / separator and / at the end
        //  "C:/programs/_tomcat/webapps/cwexperimental/", "a");
        //wimpy test, but works on all computers
        Test.ensureNotNull(SSR.getContextDirectory(), //with / separator and / at the end
            "contextDirectory");

        //getTempDirectory
        String2.log("test getTempDirectory current=" + SSR.getTempDirectory());
        //wimpy test
        Test.ensureEqual(SSR.getTempDirectory(), SSR.getContextDirectory() + "WEB-INF/temp/", "a");


        //done 
        String2.log("\nDone. All non-Unix tests passed!");

    }

    /**
     * If this fails with "Connection refused" error, make sure McAffee "Virus Scan Console :
     *   Access Protection Properties : Anti Virus Standard Protections :
     *   Prevent mass mailing worms from sending mail" is un-checked.
     */
    public static void testEmail() throws Exception {

        String emailServer, emailPort, emailProperties, emailUser,
            emailPassword, emailReplyToAddress, emailToAddresses;
        SSR.debugMode = true;

        //*** sendEmail via Google   uses starttls authentication
        emailServer = String2.getStringFromSystemIn(
            "\n\n***gmail email server (e.g., smtp.gmail.com)? ");
        if (emailServer.length() == 0) emailServer = "smtp.gmail.com";

        emailPort = String2.getStringFromSystemIn(
            "gmail email port (e.g., 465 or 587 (default))? ");
        if (emailPort.length() == 0) emailPort = "587";

        emailUser = String2.getStringFromSystemIn( 
            "gmail email user (e.g., erd.data@noaa.gov)? ");
        if (emailUser.length() == 0) emailUser = "erd.data@noaa.gov"; 

        emailPassword = String2.getPasswordFromSystemIn(
            "gmail email password\n" +
            "(e.g., password (or \"\" to skip this test)? ");
        
        if (emailPassword.length() > 0) {
            emailReplyToAddress = String2.getStringFromSystemIn( 
                "gmail email Reply To address (e.g., erd.data@noaa.gov)? ");
            if (emailReplyToAddress.length() == 0) emailReplyToAddress = "erd.data@noaa.gov";

            emailToAddresses = String2.getStringFromSystemIn(
                "1+ email To addresses (e.g., bob.simons@noaa.gov,CoHortSoftware@gmail.com,null)? ");
            if (emailToAddresses.length() == 0) emailToAddresses = "bob.simons@noaa.gov,CoHortSoftware@gmail.com";

            try {
                String2.log("test gmail email " + emailToAddresses); 
                SSR.sendEmail(emailServer, String2.parseInt(emailPort), emailUser, emailPassword, 
                    "mail.smtp.starttls.enable|true",
                    emailReplyToAddress, emailToAddresses,
                    "gmail email test", "This is a gmail email test from TestSSR.");
            } catch (Exception e) {
                String2.pressEnterToContinue(MustBe.throwableToString(e)); 
            }
        }
        SSR.debugMode = false;
    }


    /** 
     * Test email. 
     * If this fails with "Connection refused" error, make sure McAffee "Virus Scan Console :
     *   Access Protection Properties : Anti Virus Standard Protections :
     *   Prevent mass mailing worms from sending mail" is un-checked.
     * 
     */
    public static void testEmail(String emailUser, String password) throws Exception {

        String title = "Email Test from TestSSR";
        String content = "This is an email test (local) from user=" + emailUser + " in TestSSR.";
        String2.log("\n*** " + content);
        SSR.debugMode = true;
        if (true) {
            String emailServer = "smtp.gmail.com";
            //Thunderbird used 465.  587 is what we're supposed to use. Dave Nordello opened it up.
            int emailPort = 587; 
            SSR.sendEmail(emailServer, emailPort, emailUser, password, 
                "mail.smtp.starttls.enable|true", 
                emailUser, "erd.data@noaa.gov,CoHortSoftware@gmail.com,null", title, content);
        }
        SSR.debugMode = false;
    }


    /**
     * Run all of the tests which are dependent on Unix.
     */
    public static void runUnixTests() throws Exception {
        //cShell
        String2.log("test cShell");
        //Test.ensureEqual(toNewlineString(SSR.cShell("")), "", "a");

        String2.log("Done. All Unix tests passed!");
    }

    /**
     * Run all of the tests
     */
    public static void main(String args[]) throws Throwable {
        SSR.verbose = true;
/* for releases, this line should have open/close comment */
        runNonUnixTests();
        
//        runUnixTests();
    }

}


