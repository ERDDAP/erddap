/*
 * TestSSR Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import tags.TagAWS;
import tags.TagIncompleteTest;
import tags.TagPassword;

/**
 * This is a Java program to test all of the methods in SSR.
 *
 * @author Robert Simons was bob.simons@noaa.gov, now BobSimons2.00@gmail.com January 2005
 */
public class TestSSR {

  /** Run all of the tests which are operating system independent. */
  @org.junit.jupiter.api.Test
  @TagPassword
  void runNonUnixTests() throws Throwable {
    String sar[];

    String2.log("\n*** TestSSR");
    String2.log("This must be run in a command line window so passwords can be entered!");

    /*
     * //sendSoap
     * String soap = SSR.sendSoap(
     * "http://services.xmethods.net:80/soap/servlet/rpcrouter",
     * "    <ns1:getTemp xmlns:ns1=\"urn:xmethods-Temperature\"\n" +
     * "      SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
     * +
     * "        <zipcode xsi:type=\"xsd:string\">93950</zipcode>\n" +
     * "     </ns1:getTemp>\n",
     * "");
     * String2.log(soap);
     * System.exit(0);
     */

    // percentDecode(String query)
    String s = "~`!@#$%^&*()_-+=|\\{[}]:;\"'<,>.?/ a\nA°1"; // after A is degree #176/B0
    Test.ensureEqual(
        SSR.percentEncode(s), // it is utf-8'd then % encoded
        // note that I modified Java code so ' ' becomes %20, not +
        "%7E%60%21%40%23%24%25%5E%26*%28%29_-%2B%3D%7C%5C%7B%5B%7D%5D%3A%3B%22%27%3C%2C%3E.%3F%2F%20a%0AA%C2%B01",
        ""); // It encodes ~!*()'
    Test.ensureEqual(
        SSR.minimalPercentEncode(s), //
        "~%60!%40%23%24%25%5E%26*()_-%2B%3D%7C%5C%7B%5B%7D%5D%3A%3B%22%27%3C%2C%3E.%3F%2F%20a%0AA%C2%B01",
        "");
    Test.ensureEqual(SSR.percentDecode("%2B%20%3Aq*~%3F%3D%26%25"), "+ :q*~?=&%", "");

    s = "AZaz09 \t\r\n`";
    Test.ensureEqual(SSR.minimalPercentEncode(s), "AZaz09%20%09%0D%0A%60", "");
    Test.ensureEqual(SSR.percentEncode(s), "AZaz09%20%09%0D%0A%60", "");
    Test.ensureEqual(SSR.percentDecode("AZaz09%20%09%0D%0A%60"), s, "");

    s = "~!@#$%^&*()";
    Test.ensureEqual(SSR.minimalPercentEncode(s), "~!%40%23%24%25%5E%26*()", "");
    Test.ensureEqual(SSR.percentEncode(s), "%7E%21%40%23%24%25%5E%26*%28%29", "");
    Test.ensureEqual(SSR.percentDecode("%7E%21%40%23%24%25%5E%26*%28%29"), s, "");

    s = "-_=+\\|[{]};";
    Test.ensureEqual(SSR.minimalPercentEncode(s), "-_%3D%2B%5C%7C%5B%7B%5D%7D%3B", "");
    Test.ensureEqual(SSR.percentEncode(s), "-_%3D%2B%5C%7C%5B%7B%5D%7D%3B", "");
    Test.ensureEqual(SSR.percentDecode("-_%3D%2B%5C%7C%5B%7B%5D%7D%3B"), s, "");

    s = ":'\",<.>/?";
    Test.ensureEqual(SSR.minimalPercentEncode(s), "%3A%27%22%2C%3C.%3E%2F%3F", "");
    Test.ensureEqual(SSR.percentEncode(s), "%3A%27%22%2C%3C.%3E%2F%3F", "");
    Test.ensureEqual(SSR.percentDecode("%3A%27%22%2C%3C.%3E%2F%3F"), s, "");

    /*
     * 2014-08-05 DEACTIVATED BECAUSE NOT USED. IF NEEDED, SWITCH TO Apache
     * commons-net???
     * //sftp
     * String2.log("test sftp");
     * String password = String2.getPasswordFromSystemIn(//String2.beep(1) +
     * "cwatch password for coastwatch computer (enter \"\" to skip the test)? ");
     * if (password.length() > 0) {
     * String fileName = "Rainbow.cpt";
     * StringBuilder cmds = new StringBuilder(
     * "lcd " + SSR.getTempDirectory() + "\n" +
     * "cd /u00/cwatch/bobtemp\n" + //on coastwatch computer; don't use
     * String2.testU00Dir
     * "get " + fileName);
     * File2.delete(SSR.getTempDirectory() + fileName);
     * SSR.sftp("coastwatch.pfeg.noaa.gov", "cwatch", password, cmds.toString());
     * Test.ensureEqual(File2.length(SSR.getTempDirectory() + fileName), 214, "a");
     * File2.delete(SSR.getTempDirectory() + fileName);
     * }
     * Math2.sleep(3000); //allow j2ssh to finish closing and writing messages
     */

    // SSR.windowsSftp never worked (authentication trouble) and SSR.sftp is
    // better anyway because it is Java-based and therefore platform independent.
    // SSR.windowsSftp("coastwatch.pfeg.noaa.gov", "cwatch", password,
    // "\\temp\\",
    // "/usr/local/jakarta-tomcat-5.5.4/webapps/cwexperimental/WEB-INF/secure/",
    // new String[]{"btemplate.xml"}, //send
    // new String[]{}, 10); //receive

    // dosShell
    String2.log("test dosShell");
    String tempGif =
        EDStatic.getWebInfParentDirectory()
            + // with / separator and / at the end
            "images/temp.gif";
    File2.delete(tempGif);
    try {
      Test.ensureEqual(
          String2.toNewlineString(
              SSR.dosShell(
                      "\"C:\\Program Files (x86)\\ImageMagick-6.8.0-Q16\\convert\" "
                          + EDStatic.getWebInfParentDirectory()
                          + // with / separator and / at the end
                          "images/subtitle.jpg "
                          + tempGif,
                      10)
                  .toArray()),
          "",
          "dosShell a");
      Test.ensureTrue(File2.isFile(tempGif), "dosShell b");
    } catch (Exception e) {
      Test.knownProblem(
          "IMAGEMAGICK NOT SET UP ON BOB'S DELL M4700 or Lenovo.", MustBe.throwableToString(e));
    }
    File2.delete(tempGif);

    // cutChar
    String2.log("test cutChar");
    Test.ensureEqual(SSR.cutChar("abcd", 2, 3), "bc", "a");
    Test.ensureEqual(SSR.cutChar("abcd", 1, 4), "abcd", "b");
    Test.ensureEqual(SSR.cutChar("abcd", 0, 4), "abcd", "c");
    Test.ensureEqual(SSR.cutChar("abcd", 1, 5), "abcd", "d");
    Test.ensureEqual(SSR.cutChar("abcd", 3, 3), "c", "e");
    Test.ensureEqual(SSR.cutChar("abcd", 4, 1), "", "f");
    Test.ensureEqual(SSR.cutChar("abcd", -2, 0), "", "g");

    Test.ensureEqual(SSR.cutChar("abcd", 1), "abcd", "a");
    Test.ensureEqual(SSR.cutChar("abcd", 0), "abcd", "b");
    Test.ensureEqual(SSR.cutChar("abcd", -1), "abcd", "c");
    Test.ensureEqual(SSR.cutChar("abcd", 2), "bcd", "d");
    Test.ensureEqual(SSR.cutChar("abcd", 4), "d", "e");
    Test.ensureEqual(SSR.cutChar("abcd", 5), "", "f");

    // make a big chunk of text
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1000000; i++)
      sb.append("This is a really, really, long line of text to text compression speed.\n");
    String longText = sb.toString();
    sb = null;
    String testMB = "  " + (longText.length() / Math2.BytesPerMB) + " MB";

    // zip without directory info
    String2.log("\n* test zip without dir info" + testMB);
    String zipDir = EDStatic.getWebInfParentDirectory() + "temp/";
    String zipName = "TestSSR.zip";
    String fileName = "TestSSR.txt";
    // write a longText file
    Test.ensureEqual(File2.writeToFile88591(zipDir + fileName, longText), "", "SSR.zip a");
    // make the zip file
    File2.delete(zipDir + zipName);
    long time1 = System.currentTimeMillis();
    SSR.zip(zipDir + zipName, new String[] {zipDir + fileName}, 10);
    time1 = System.currentTimeMillis() - time1;
    File2.delete(zipDir + fileName);
    // unzip the zip file
    long time2 = System.currentTimeMillis();
    SSR.unzip(
        zipDir + zipName,
        zipDir, // note: extract to zipDir, since it doesn't include dir
        false,
        10,
        null); // false 'ignoreDirectoryInfo', but there is none
    time2 = System.currentTimeMillis() - time2;
    // ensure results are as expected
    String[] results = File2.readFromFile88591(zipDir + fileName);
    Test.ensureEqual(results[0], "", "SSR.zip b");
    Test.ensureEqual(results[1], longText, "SSR.zip c");
    String2.log(
        "zip+unzip time=" + (time1 + time2) + "ms  (Java 1.7M4700 967ms, 1.6 4000-11000ms)");
    File2.delete(zipDir + zipName);
    File2.delete(zipDir + fileName);

    // zip with directory info
    String2.log("\n* test zip with dir info" + testMB);
    // write a longText file
    Test.ensureEqual(File2.writeToFile88591(zipDir + fileName, longText), "", "SSR.zip a");
    // make the zip file
    File2.delete(zipDir + zipName);
    time1 = System.currentTimeMillis();
    SSR.zip(zipDir + zipName, new String[] {zipDir + fileName}, 10, zipDir);
    time1 = System.currentTimeMillis() - time1;
    File2.delete(zipDir + fileName);
    // unzip the zip file
    time2 = System.currentTimeMillis();
    SSR.unzip(zipDir + zipName, zipDir, false, 10, null); // false 'ignoreDirectoryInfo'
    time2 = System.currentTimeMillis() - time2;
    // ensure results are as expected
    results = File2.readFromFile88591(zipDir + fileName);
    Test.ensureEqual(results[0], "", "SSR.zip b");
    Test.ensureEqual(results[1], longText, "SSR.zip c");
    String2.log(
        "zip+unzip (w directory info) time="
            + (time1 + time2)
            + "ms  (Java 1.7M4700 937, 1.6 ~4000-11000ms)");
    File2.delete(zipDir + zipName);
    File2.delete(zipDir + fileName);

    // gzip without directory info
    for (int rep = 0; rep < 2; rep++) {
      String2.log("\n* test gzip without dir info" + testMB);
      String gzipDir = EDStatic.getWebInfParentDirectory() + "temp/";
      String gzipName = "TestSSRG.txt.gz";
      fileName = "TestSSRG.txt";
      // write a longText file
      Test.ensureEqual(File2.writeToFile88591(gzipDir + fileName, longText), "", "SSR.gz a");
      // make the gzip file
      File2.delete(gzipDir + gzipName);
      time1 = System.currentTimeMillis();
      SSR.gzip(gzipDir + gzipName, new String[] {gzipDir + fileName}, 10); // don't include dir info
      time1 = System.currentTimeMillis() - time1;
      File2.delete(gzipDir + fileName);
      // unzip the gzip file
      time2 = System.currentTimeMillis();
      SSR.unGzip(
          gzipDir + gzipName,
          gzipDir, // note: extract to classPath, since doesn't include dir
          true,
          10); // false 'ignoreDirectoryInfo'
      time2 = System.currentTimeMillis() - time2;
      // ensure results are as expected
      results = File2.readFromFile88591(gzipDir + fileName);
      Test.ensureEqual(results[0], "", "SSR.gz b");
      Test.ensureEqual(results[1], longText, "SSR.z c");
      String2.log(
          "gzip+ungzip time="
              + (time1 + time2)
              + "ms  (Java 1.7M4700 780-880ms, 1.6 ~4000-11000ms)");
      File2.delete(gzipDir + gzipName);
      File2.delete(gzipDir + fileName);
    }

    // getURLResponse (which uses getURLInputStream)
    // FUTURE: test various compressed url's
    String2.log("test getURLResponse");
    try {
      sar = SSR.getUrlResponseLines("https://coastwatch.pfeg.noaa.gov/erddap/index.html");
      Test.ensureEqual(
          String2.lineContaining(
                  sar, "ERDDAP is a data server that gives you a simple, consistent way")
              == -1,
          false,
          "Response=" + String2.toNewlineString(sar));
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      String2.pressEnterToContinue("\nRecover from failure?");
    }

    // test non-existent file
    long rTime = System.currentTimeMillis();
    try {
      sar = SSR.getUrlResponseLines("https://coastwatch.pfeg.noaa.gov/zzz.html");
      throw new Throwable("shouldn't get here.");
    } catch (Exception e) { // not throwable
      String2.log(
          "SSR.getUrlResponse for non existent url time="
              + (System.currentTimeMillis() - rTime)
              + "ms");
      // String2.pressEnterToContinue();
    } catch (Throwable t) {
      Test.error(t.toString()); // converts it to Exception and stops the testing
    }

    // note there is no continuity (session cookie isn't being sent)
    // but you can put as many params on one line as needed (from any screen)
    // and put edit=... to determine which screen gets returned
    sar =
        SSR.getUrlResponseLines(
            "https://coastwatch.pfeg.noaa.gov/coastwatch/CWBrowser.jsp?edit=Grid+Data");
    String2.log("****beginResponse\n" + String2.toNewlineString(sar) + "\n****endResponse");
    Test.ensureNotEqual(String2.lineContaining(sar, "Download the grid data:"), -1, "e");

    // postHTMLForm (always right after contact the website above)
    // I NEVER GOT THIS WORKING. JUST USE 'GET' TESTS ABOVE
    // String2.log("test postHTMLForm");

    // for apache commons version
    // sar = SSR.postHTMLForm("https://coastwatch.pfeg.noaa.gov/cwexperimental/",
    // "CWBrowser.jsp",
    // new String[]{"edit", "Bathymetry"});

    // for devx version
    // sar =
    // SSR.postHTMLForm("https://coastwatch.pfeg.noaa.gov/cwexperimental/CWBrowser.jsp",
    // new Object[]{"bathymetry", "false", "edit", "Bathymetry"});

    // for java almanac version
    // sar = SSR.postHTMLForm("coastwatch.pfeg.noaa.gov",
    // "/cwexperimental/CWBrowser.jsp",
    // "edit=Bathymetry");

    // String2.log("****beginResponse\n" + String2.toNewlineString(sar) +
    // "\n****endResponse");
    // Test.ensureNotEqual(String2.lineContaining(sar, "1) Draw bathymetry lines:"),
    // -1, "a");

    // getFirstLineStartsWith
    String2.log("test getFirstLineStartsWith");
    Path tFilePath = Paths.get("testSSR.txt");
    URL tFileName = tFilePath.toUri().toURL();
    File2.writeToFile88591(tFilePath.toString(), "This is\na file\nwith a few lines.");
    Test.ensureEqual(
        SSR.getFirstLineStartsWith(tFileName, File2.ISO_8859_1, "with "), "with a few lines.", "a");
    Test.ensureEqual(SSR.getFirstLineStartsWith(tFileName, File2.ISO_8859_1, "hi "), null, "b");

    // getFirstLineMatching
    String2.log("test getFirstLineMatching");
    Test.ensureEqual(
        SSR.getFirstLineMatching(tFileName, File2.ISO_8859_1, ".*?i.*"),
        "This is",
        "a"); // find first
    // of many
    // matches
    Test.ensureEqual(
        SSR.getFirstLineMatching(tFileName, File2.ISO_8859_1, "^a.*"),
        "a file",
        "b"); // start of line
    Test.ensureEqual(
        SSR.getFirstLineMatching(tFileName, File2.ISO_8859_1, ".*?\\sfew\\s.*"),
        "with a few lines.",
        "c"); // containing
    Test.ensureEqual(
        SSR.getFirstLineMatching(tFileName, File2.ISO_8859_1, "q"), null, "d"); // no match

    Test.ensureTrue(File2.delete(tFilePath.toString()), "delete " + tFileName);

    // getContextDirectory
    String2.log(
        "test getContextDirectory current="
            + EDStatic.getWebInfParentDirectory()); // with / separator and / at the end
    // there is no way to test this and have it work with different installations
    // test for my computer (comment out on other computers):
    // ensureEqual(String2.getContextDirectory(), //with / separator and / at the
    // end
    // "C:/programs/_tomcat/webapps/cwexperimental/", "a");
    // wimpy test, but works on all computers
    Test.ensureNotNull(
        EDStatic.getWebInfParentDirectory(), // with / separator and / at the end
        "contextDirectory");

    // getTempDirectory
    String2.log("test getTempDirectory current=" + SSR.getTempDirectory());
    // wimpy test
    Test.ensureEqual(
        SSR.getTempDirectory(), EDStatic.getWebInfParentDirectory() + "WEB-INF/temp/", "a");

    // done
    String2.log("\nDone. All non-Unix tests passed!");
  }

  /**
   * If this fails with "Connection refused" error, make sure McAffee "Virus Scan Console : Access
   * Protection Properties : Anti Virus Standard Protections : Prevent mass mailing worms from
   * sending mail" is un-checked.
   */
  @org.junit.jupiter.api.Test
  @TagPassword
  void testEmail() throws Exception {

    String emailServer,
        emailPort,
        emailProperties,
        emailUser,
        emailPassword,
        emailReplyToAddress,
        emailToAddresses;
    SSR.debugMode = true;

    // *** sendEmail via Google uses starttls authentication
    emailServer =
        String2.getStringFromSystemIn("\n\n***gmail email server (e.g., smtp.gmail.com)? ");
    if (emailServer.length() == 0) emailServer = "smtp.gmail.com";

    emailPort = String2.getStringFromSystemIn("gmail email port (e.g., 465 or 587 (default))? ");
    if (emailPort.length() == 0) emailPort = "587";

    emailUser = String2.getStringFromSystemIn("gmail email user (e.g., erd.data@noaa.gov)? ");
    if (emailUser.length() == 0) emailUser = "erd.data@noaa.gov";

    emailPassword =
        String2.getPasswordFromSystemIn(
            "gmail email password\n"
                + "(e.g., password (or \"\" to skip this test. Bob: use 'application specific password')? ");

    if (emailPassword.length() > 0) {
      emailReplyToAddress =
          String2.getStringFromSystemIn("gmail email Reply To address (e.g., erd.data@noaa.gov)? ");
      if (emailReplyToAddress.length() == 0) emailReplyToAddress = "erd.data@noaa.gov";

      emailToAddresses =
          String2.getStringFromSystemIn(
              "1+ email To addresses (e.g., BobSimons2.00@gmail.com,CoHortSoftware@gmail.com)? ");
      if (emailToAddresses.length() == 0)
        emailToAddresses = "BobSimons2.00@gmail.com,CoHortSoftware@gmail.com";

      try {
        String2.log("test gmail email " + emailToAddresses);
        SSR.sendEmail(
            emailServer,
            String2.parseInt(emailPort),
            emailUser,
            emailPassword,
            "mail.smtp.starttls.enable|true",
            emailReplyToAddress,
            emailToAddresses,
            "gmail email test", // Euro
            "This is a gmail email test from TestSSR with embedded special characters < > & û \u20ac .\nSecond line.");
      } catch (Exception e) {
        String2.pressEnterToContinue(MustBe.throwableToString(e));
      }
    }
    SSR.debugMode = false;
  }

  /**
   * Test email. If this fails with "Connection refused" error, make sure McAffee "Virus Scan
   * Console : Access Protection Properties : Anti Virus Standard Protections : Prevent mass mailing
   * worms from sending mail" is un-checked.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testEmail(String emailUser, String password) throws Exception {

    String title = "Email Test from TestSSR";
    String content = "This is an email test (local) from user=" + emailUser + " in TestSSR.";
    String2.log("\n*** " + content);
    SSR.debugMode = true;
    if (true) {
      String emailServer = "smtp.gmail.com";
      // Thunderbird used 465. 587 is what we're supposed to use. Dave Nordello opened
      // it up.
      int emailPort = 587;
      SSR.sendEmail(
          emailServer,
          emailPort,
          emailUser,
          password,
          "mail.smtp.starttls.enable|true",
          emailUser,
          "erd.data@noaa.gov,CoHortSoftware@gmail.com,null",
          title,
          content);
    }
    SSR.debugMode = false;
  }

  /** Run all of the tests which are dependent on Unix. */
  @org.junit.jupiter.api.Test
  void runUnixTests() throws Exception {
    // cShell
    String2.log("test cShell");
    // Test.ensureEqual(toNewlineString(SSR.cShell("")), "", "a");

    String2.log("Done. All Unix tests passed!");
  }

  /** Runs some AWS S3-related tests. */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testAwsS3() throws Exception {

    String2.log(
        "\n*** TestSSR.testAwsS3() -- THIS TEST REQUIRES testPrivateAwsS3MediaFiles in localhost erddap.");
    String localFile20 = "/u00/data/points/testMediaFiles/ShouldWork/noaa20.gif";
    String privateAwsSource20 =
        "https://bobsimonsdata.s3.us-east-1.amazonaws.com/testMediaFiles/noaa20.gif";
    String privateErddapSource20 =
        "http://localhost:8080/cwexperimental/files/testPrivateAwsS3MediaFiles/noaa20.gif";
    BufferedInputStream bis;
    BufferedOutputStream bos;
    String dest = File2.getSystemTempDirectory() + "testS3";
    String results, expected;

    // String2.isRemote() and String2.isTrulyRemote()
    Test.ensureEqual(String2.isRemote(localFile20), false, "");
    Test.ensureEqual(String2.isTrulyRemote(localFile20), false, "");
    Test.ensureEqual(String2.isRemote(privateAwsSource20), true, "");
    Test.ensureEqual(String2.isTrulyRemote(privateAwsSource20), false, "");
    Test.ensureEqual(String2.isRemote(privateErddapSource20), true, "");
    Test.ensureEqual(String2.isTrulyRemote(privateErddapSource20), true, "");

    String bro[] = String2.parseAwsS3Url(privateAwsSource20);
    Test.ensureEqual(bro[0], "bobsimonsdata", "");
    Test.ensureEqual(bro[1], "us-east-1", "");
    Test.ensureEqual(bro[2], "testMediaFiles/noaa20.gif", "");

    bro =
        String2.parseAwsS3Url(
            "http://some.bucket.s3-website-us-east-1.amazonaws.com/some/object.gif");
    Test.ensureEqual(bro[0], "some.bucket", "");
    Test.ensureEqual(bro[1], "us-east-1", "");
    Test.ensureEqual(bro[2], "some/object.gif", "");

    // private file length
    Test.ensureEqual(File2.length(localFile20), 1043, "");
    Test.ensureEqual(File2.length(privateAwsSource20), 1043, "");

    // private file lastModified
    Test.ensureEqual(File2.getLastModified(localFile20), 1302534310000L, "");
    Test.ensureEqual(
        File2.getLastModified(privateAwsSource20),
        1620243246000L,
        ""); // the instant I put it in S3

    // a public bucket/file should allow access via a simple https request
    results = "okay";
    try {
      SSR.touchUrl(
          "https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/doi.txt",
          5000,
          false); // handleS3ViaSDK=false
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureEqual(
        results,
        "okay",
        "A public S3 file should be accessible (e.g., touchUrl) via a simple URL.");

    // a private bucket/file shouldn't allow access via a simple https request
    results = "shouldn't happen";
    try {
      SSR.touchUrl(privateAwsSource20, 5000, false); // handleS3ViaSDK=false
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=403 for URL: https://bobsimonsdata.s3.us-east-1.amazonaws.com/testMediaFiles/noaa20.gif\n";
    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "A private S3 file shouldn't be accessible (e.g., touchUrl) via a simple URL (even with credentials on this computer). results="
            + results);

    // byte range private
    Test.ensureTrue(File2.copy(privateAwsSource20, dest + "1a"), "");
    results = File2.hexDump(dest + "1a", 96);
    String expected1 =
        "47 49 46 38 39 61 14 00   14 00 f7 00 00 14 3a 8c   GIF89a        :  |\n"
            + "2c a2 d4 94 d2 ec 4c 6e   a4 8c a2 c4 04 86 d4 34   ,     Ln       4 |\n"
            + "56 9c cc ea f4 6c 86 b4   64 ba e4 84 c6 e4 24 4a   V    l  d     $J |\n"
            + "94 44 aa dc 14 96 d4 e4   f6 fc ac ba d4 cc d2 e4    D               |\n"
            + "64 7e ac 44 62 9c 34 a6   dc 7c 92 bc bc ca dc 14   d~ Db 4  |       |\n"
            + "42 8c 5c 76 ac 9c ae cc   74 c2 e4 14 96 dc fc fe   B \\v    t        |\n";
    Test.ensureEqual(results, expected1, "results=\n" + results);

    // range request
    Test.ensureTrue(File2.copy(privateAwsSource20, dest + "2a", 16, 95), "");
    results = File2.hexDump(dest + "2a", 1000); // read all
    String expected2 = // same but without 1st row (16 bytes)
        "2c a2 d4 94 d2 ec 4c 6e   a4 8c a2 c4 04 86 d4 34   ,     Ln       4 |\n"
            + "56 9c cc ea f4 6c 86 b4   64 ba e4 84 c6 e4 24 4a   V    l  d     $J |\n"
            + "94 44 aa dc 14 96 d4 e4   f6 fc ac ba d4 cc d2 e4    D               |\n"
            + "64 7e ac 44 62 9c 34 a6   dc 7c 92 bc bc ca dc 14   d~ Db 4  |       |\n"
            + "42 8c 5c 76 ac 9c ae cc   74 c2 e4 14 96 dc fc fe   B \\v    t        |\n";
    Test.ensureEqual(results, expected2, "results=\n" + results);

    // byte range private
    bis =
        (BufferedInputStream)
            SSR.getUrlConnBufferedInputStream(
                    privateErddapSource20, 30000, false, false, 0, -1, false)[1];
    bos = new BufferedOutputStream(new FileOutputStream(dest + "1b"));
    File2.copy(bis, bos, 0, -1);
    bis.close();
    bos.close();
    results = File2.hexDump(dest + "1b", 96);
    Test.ensureEqual(results, expected1, "results=\n" + results);

    // range request
    bis =
        (BufferedInputStream)
            SSR.getUrlConnBufferedInputStream(
                    privateErddapSource20, 30000, false, false, 16, 95, false)[1]; // 16 to 95
    bos = new BufferedOutputStream(new FileOutputStream(dest + "2b"));
    File2.copy(bis, bos, 0, -1);
    bis.close();
    bos.close();
    results = File2.hexDump(dest + "2b", 1000); // all
    Test.ensureEqual(results, expected2, "results=\n" + results);

    // range request
    bis =
        (BufferedInputStream)
            SSR.getUrlConnBufferedInputStream(
                    privateErddapSource20, 30000, false, false, 16, -1, false)[1]; // 16 to end
    bos = new BufferedOutputStream(new FileOutputStream(dest + "2b2"));
    File2.copy(bis, bos, 0, -1);
    bis.close();
    bos.close();
    results = File2.hexDump(dest + "2b2", 80);
    Test.ensureEqual(results, expected2, "results=\n" + results);
  }

  /** Tests Aws TransferManager. */
  @org.junit.jupiter.api.Test
  @TagAWS
  void testAwsTransferManager() throws Exception {

    // *** test a lot of AWS S3 actions on a private AWS bucket
    // delete a file on S3 to ensure it doesn't exist (ignore result)
    String origLocal =
        Path.of(TestSSR.class.getResource("data/ascii/standardizeWhat1.csv").toURI()).toString();
    String tempLocal = File2.getSystemTempDirectory() + "testAwsS3.csv";
    String awsUrl = "https://bobsimonsdata.s3.us-east-1.amazonaws.com/testMediaFiles/testAwsS3.csv";
    // bucket is publicly readible in a browser via http but not https
    String awsUrl2 =
        "http://bob.simons.public.output.s3-website-us-east-1.amazonaws.com/testAwsS3.csv";
    String content;

    // delete files I will create
    File2.deleteAwsS3File(awsUrl);
    File2.delete(tempLocal);

    // isAwsS3File
    Test.ensureEqual(File2.isAwsS3File(awsUrl), false, "");

    // upload a file to S3
    SSR.uploadFileToAwsS3(null, origLocal, awsUrl, "text/csv");

    // length of Aws S3 file
    Test.ensureEqual(File2.length(awsUrl), 44, "");

    // isAwsS3File
    Test.ensureEqual(File2.isAwsS3File(awsUrl), true, "");

    // download a file from S3
    SSR.downloadFile(awsUrl, tempLocal, false); // tryToUseCompression is ignored

    // read local file
    content = File2.directReadFrom88591File(tempLocal);
    Test.ensureEqual(
        content,
        "date,data\n" + "20100101000000,1\n" + "20100102000000,2\n",
        "content=" + String2.annotatedString(content));

    // delete files I created
    File2.deleteAwsS3File(awsUrl);
    File2.delete(tempLocal);

    // isAwsS3File
    Test.ensureEqual(File2.isAwsS3File(awsUrl), false, "");

    // ************** again but with awsUrl2
    // delete files I will create
    File2.deleteAwsS3File(awsUrl2);
    File2.delete(tempLocal);

    // isAwsS3File
    Test.ensureEqual(File2.isAwsS3File(awsUrl2), false, "");

    // upload a file to S3
    // fails here.
    // "software.amazon.awssdk.crt.s3.CrtS3RuntimeException: Retry cannot be
    // attempted
    // because the maximum number of retries has been exceeded.
    // AWS_IO_MAX_RETRIES_EXCEEDED(1069)
    // at com.amazonaws.s3.S3NativeClient$3.onFinished(S3NativeClient.java:313)"
    // a permissions problem?
    SSR.uploadFileToAwsS3(null, origLocal, awsUrl2, "text/csv");

    // length of Aws S3 file
    Test.ensureEqual(File2.length(awsUrl2), 44, "");

    // isAwsS3File
    Test.ensureEqual(File2.isAwsS3File(awsUrl2), true, "");

    // download a file from S3
    SSR.downloadFile(awsUrl2, tempLocal, false); // tryToUseCompression is ignored

    // read local file
    content = File2.directReadFrom88591File(tempLocal);
    Test.ensureEqual(
        content,
        "date,data\n" + "20100101000000,1\n" + "20100102000000,2\n",
        "content=" + String2.annotatedString(content));

    // delete files I created
    File2.deleteAwsS3File(awsUrl2);
    File2.delete(tempLocal);

    // isAwsS3File
    Test.ensureEqual(File2.isAwsS3File(awsUrl2), false, "");
  }

  /** Run all of the tests */
  public static void main(String args[]) throws Throwable {
    SSR.verbose = true;

    // runUnixTests();
  }
}
