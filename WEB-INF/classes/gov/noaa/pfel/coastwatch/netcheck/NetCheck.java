/*
 * NetCheck Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * This is a command line application which tests the ongoing availability and proper functioning of
 * websites, ftp servers, OPeNDAP servers, and other web applications.
 *
 * <p>It is best if this program is run on a computer which is far from the computers it is testing.
 * Then, if the computers being tested lose electrical power or aren't accessible from the web
 * (e.g., ip routing problems), this program will detect the problems. If you run this program on a
 * local computer, consider using a service like www.siteuptime.com (free if used simplistically)
 * which complements NetCheck by checking if a computer is available (via ping) every 30 minutes.
 *
 * <p>A log file will be created with the name of the xml file + ".log".
 *
 * <p>For Opendap, this program uses Java DAP 1.1.7. See https://www.opendap.org/download/index.html
 * for more information. The .java and .class files for this are in the classes/dods directory.
 *
 * <p>For reading/writing .nc files, get netcdfAll-......jar from ftp://ftp.unidata.ucar.edu/pub and
 * copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar. Put it in the classpath for the
 * compiler and for Java.
 *
 * <p>To send emails, this program uses the JavaMail API and the JavaBeans Activation Framework
 * extension or JAF (javax.activation). See util/SSR.sendEmail for more information. The required
 * mail.jar file is freely available from Sun
 * (https://www.oracle.com/technetwork/java/javamail/index.html) and can be redistributed freely.
 *
 * <p>WARNING! When a test times out, its thread is stopped which may cause a memory leak. If the
 * memory use goes over memoryWarningMB, an email will be sent to the administrator.
 *
 * <p>The pass% reported in the status reports (every minutesBetweenStatusReports) indicates the
 * pass% since the last status report.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-08-16
 */
public class NetCheck {
  // FUTURE:
  // * after failure, immediately try again; require 2 failures to report the error?
  // * support tests of interaction with html forms, e.g., test make hdf file
  //  I would need to accept and hold cookies (e.g., for LAS, too)
  // * notify via email (but really SMS cell phones): don't send between 11pm and 7am?
  // * For industrial strength approach, I could run multiple tests
  //  simultaneously (each in its own thread).

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static final String passPercent = "(Passed=";
  public static final String CurrentStatusPASS = "Status=PASS ";
  public static final String CurrentStatusFAIL = "Status=FAIL ";
  public static final String CHANGED = " changed!) ";

  private double minutesBetweenTests;
  private int minutesBetweenStatusReports;
  private double mustRespondWithinSeconds;
  private String smtpServer;
  private int smtpPort;
  private String smtpUser;
  private String smtpPassword; // it is important that this be private
  private String smtpProperties;
  private String smtpFromAddress;
  private int memoryWarningMB;
  // emailAdministrator is just one person, but is an ArrayList to standardize subscriber lists
  private ArrayList emailAdministrator = new ArrayList();
  private ArrayList emailStatusTo = new ArrayList();
  private ArrayList emailStatusHeadlinesTo = new ArrayList();
  private ArrayList emailChangesTo = new ArrayList();
  private ArrayList emailChangeHeadlinesTo = new ArrayList();
  private ArrayList netCheckTests = new ArrayList();
  private String lastResult[]; // initially all ""
  private int nPass[];
  private int nTry[];
  private boolean testMode = false;

  /**
   * <br>
   * Version 1.1 included the ability to create the tests via parameters (in addition to the
   * original xmlReader method). 2006-07-13
   */
  public static final double version = 1.1;

  /**
   * The constructor. A log file called NetCheck.log will be created in the root directory.
   *
   * @param xmlFileName the full name of the xml file with the setup info.
   * @throws Exception if trouble
   */
  public NetCheck(String xmlFileName, boolean testMode) throws Exception {
    // setupLog
    this.testMode = testMode;
    Test.ensureTrue( // do first, so no log file is created if .xml doesn't exist
        File2.isFile(xmlFileName),
        String2.ERROR + ": the .xml file (" + xmlFileName + ") was not found.");

    // route calls to a logger to com.cohort.util.String2Log
    String2.setupCommonsLogging(-1);

    String2.setupLog(
        true, false, xmlFileName + ".log", true, String2.logFileDefaultMaxSize); // append
    String2.log(
        "*** Starting NetCheck "
            + Calendar2.getCurrentISODateTimeStringLocalTZ()
            + "\n"
            + "logFile="
            + String2.logFileName()
            + "\n"
            + String2.standardHelpAboutMessage());
    HttpTest.verbose = verbose;
    // SftpTest.verbose = verbose;
    SSR.verbose = false;
    File2.verbose = true;

    // run setup
    setup(xmlFileName);
    String2.log("");

    // run the tests
    int pass = 0;
    int passesPerStatusReport =
        Math.max(1, Math2.roundToInt(minutesBetweenStatusReports / minutesBetweenTests));
    while (true) {
      long time = System.currentTimeMillis();
      test(pass % passesPerStatusReport == 0, pass == 0);
      Math2.gcAndWait("NetCheck (between tests)");
      Math2.gcAndWait("NetCheck (between tests)"); // NetCheck before get memoryString
      String2.log(Math2.memoryString());
      time = System.currentTimeMillis() - time;
      String2.log("Sleeping...\n");
      Math2.sleep(
          Math.max(
              0,
              Math.round(minutesBetweenTests * 60 * 1000) - time)); // NetCheck pause between runs
      pass++;
    }
  }

  /**
   * This reloads the set up information from the NetCheck.xml file.
   *
   * @param xmlFileName the full name of the xml file with the setup info
   * @throws Exception if trouble
   */
  private void setup(String xmlFileName) throws Exception {
    String errorIn = String2.ERROR + " in NetCheck.setup: ";

    // reset to defaults
    minutesBetweenTests = Double.NaN;
    minutesBetweenStatusReports = Integer.MAX_VALUE;
    smtpServer = "";
    smtpPort = Integer.MAX_VALUE;
    smtpUser = null;
    smtpPassword = null;
    smtpProperties = null;
    smtpFromAddress = null;
    memoryWarningMB = 50;
    emailStatusTo.clear();
    emailStatusHeadlinesTo.clear();
    emailChangesTo.clear();
    emailChangeHeadlinesTo.clear();
    netCheckTests.clear();

    // read the xml properties file
    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(xmlFileName));
    try {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      int iteration = 0;
      while (!tags.equals("</netCheck>") && iteration++ < 1000000) {
        // process the tags
        // String2.log(tags + xmlReader.content());
        if (tags.equals("<netCheck>")) {
        } else if (tags.equals("<netCheck><setup>")) {
        } else if (tags.equals("<netCheck></setup>")) {
        } else if (tags.equals("<netCheck><setup><minutesBetweenTests>")) {
        } else if (tags.equals("<netCheck><setup></minutesBetweenTests>"))
          minutesBetweenTests = String2.parseDouble(xmlReader.content());
        else if (tags.equals("<netCheck><setup><minutesBetweenStatusReports>")) {
        } else if (tags.equals("<netCheck><setup></minutesBetweenStatusReports>"))
          minutesBetweenStatusReports = String2.parseInt(xmlReader.content());
        else if (tags.equals("<netCheck><setup><mustRespondWithinSeconds>")) {
        } else if (tags.equals("<netCheck><setup></mustRespondWithinSeconds>"))
          mustRespondWithinSeconds = String2.parseDouble(xmlReader.content());
        else if (tags.equals("<netCheck><setup><smtpServer>")) {
        } else if (tags.equals("<netCheck><setup></smtpServer>")) smtpServer = xmlReader.content();
        else if (tags.equals("<netCheck><setup><smtpPort>")) {
        } else if (tags.equals("<netCheck><setup></smtpPort>"))
          smtpPort = String2.parseInt(xmlReader.content());
        else if (tags.equals("<netCheck><setup><smtpProperties>")) {
        } else if (tags.equals("<netCheck><setup></smtpProperties>"))
          smtpProperties = xmlReader.content();
        else if (tags.equals("<netCheck><setup><smtpUser>")) {
        } else if (tags.equals("<netCheck><setup></smtpUser>")) smtpUser = xmlReader.content();
        else if (tags.equals("<netCheck><setup><smtpPassword>")) {
        } else if (tags.equals("<netCheck><setup></smtpPassword>"))
          smtpPassword = xmlReader.content();
        else if (tags.equals("<netCheck><setup><smtpFromAddress>")) {
        } else if (tags.equals("<netCheck><setup></smtpFromAddress>"))
          smtpFromAddress = xmlReader.content();
        else if (tags.equals("<netCheck><setup><emailStatusTo>")) {
        } else if (tags.equals("<netCheck><setup></emailStatusTo>"))
          emailStatusTo.add(xmlReader.content());
        else if (tags.equals("<netCheck><setup><emailStatusHeadlinesTo>")) {
        } else if (tags.equals("<netCheck><setup></emailStatusHeadlinesTo>"))
          emailStatusHeadlinesTo.add(xmlReader.content());
        else if (tags.equals("<netCheck><setup><emailChangesTo>")) {
        } else if (tags.equals("<netCheck><setup></emailChangesTo>"))
          emailChangesTo.add(xmlReader.content());
        else if (tags.equals("<netCheck><setup><emailChangeHeadlinesTo>")) {
        } else if (tags.equals("<netCheck><setup></emailChangeHeadlinesTo>"))
          emailChangeHeadlinesTo.add(xmlReader.content());
        else if (tags.equals("<netCheck><setup><testMode>")) {
        } else if (tags.equals("<netCheck><setup></testMode>"))
          testMode = String2.parseBoolean(xmlReader.content());
        else if (tags.equals("<netCheck><setup><memoryWarningMB>")) {
        } else if (tags.equals("<netCheck><setup></memoryWarningMB>"))
          memoryWarningMB = String2.parseInt(xmlReader.content());
        else if (tags.equals("<netCheck><httpTest>")) {
          // create a new httpTest
          // this reads all the tags until </httpTest>
          netCheckTests.add(new HttpTest(xmlReader));

        } else if (tags.equals("<netCheck><opendapTest>")) {
          // create a new opendapTest
          // this reads all the tags until </opendapTest>
          netCheckTests.add(new OpendapTest(xmlReader));

        } else if (tags.equals("<netCheck><pauseTest>")) {
          // create a new pauseTest
          // this reads all the tags until </pauseTest>
          netCheckTests.add(new PauseTest(xmlReader));

          // 2014-08-05 DEACTIVATED BECAUSE NOT USED. IF NEEDED, SWITCH TO Apache commons-net???
          // } else if (tags.equals("<netCheck><sftpTest>")) {
          //        //create a new sftpTest
          //        //this reads all the tags until </sftpTest>
          //        netCheckTests.add(new SftpTest(xmlReader));

        } else throw new RuntimeException(errorIn + "unrecognized tags: " + tags);

        // get the next tags
        xmlReader.nextTag();
        tags = xmlReader.allTags();
      }
    } finally {
      xmlReader.close();
    }
    if (verbose)
      String2.log(
          "\n*** Done reading .xml file ***"
              + "\nemailChangesTo="
              + emailChangesTo
              + "\nemailChangeHeadlinesTo="
              + emailChangeHeadlinesTo);

    // set optional items to default
    if (!Double.isFinite(minutesBetweenTests)) minutesBetweenTests = 5; // the default
    if (minutesBetweenStatusReports == Integer.MAX_VALUE)
      minutesBetweenStatusReports = 240; // the default
    if (!Double.isFinite(mustRespondWithinSeconds) || mustRespondWithinSeconds <= 0)
      mustRespondWithinSeconds = 30; // the default
    if (smtpPort == Integer.MAX_VALUE) smtpPort = 25; // the default

    // ensure that required items were set
    Test.ensureTrue(
        smtpServer != null && smtpServer.length() > 0,
        errorIn + "<netCheck><setup><smtpServer> was not specified.\n");
    Test.ensureTrue(
        smtpUser != null && smtpUser.length() > 0,
        errorIn + "<netCheck><setup><smtpUser> was not specified.\n");
    Test.ensureTrue(
        smtpFromAddress != null && smtpFromAddress.length() > 0,
        errorIn + "<netCheck><setup><smtpFromAddress> was not specified.\n");
    // emailStatusTo.size()             == 0) {} //not required
    // emailStatusHeadlinesTo.size()    == 0) {} //not required
    // emailChangesTo.size()          == 0) {} //not required
    // emailChangeHeadlinesTo.size() == 0) {} //not required

    // ask for password if it wasn't specified
    if (smtpPassword == null || smtpPassword.length() == 0) {
      smtpPassword = String2.getPasswordFromSystemIn("Email account password? ");
      Test.ensureTrue(
          smtpPassword.length() > 0,
          errorIn + "You must specify a password here or in netcheck.xml's <smtpPassword>.\n");
    }

    // set up info arrays (initial values are important)
    lastResult = new String[netCheckTests.size()];
    Arrays.fill(lastResult, "");
    nPass = new int[netCheckTests.size()]; // filled with 0's
    nTry = new int[netCheckTests.size()]; // filled with 0's

    emailAdministrator.add(smtpFromAddress);
    String2.log("Version " + version);
  }

  /**
   * This does the tests and sends the email notifications.
   *
   * @param doStatusReport set this to true to generate a status report
   * @param firstStatusReport set this to true if doStatusReport=true and this is the first status
   *     report
   * @return an error string (e.g., an unexpected error while running the tests; "" if no error)
   */
  public String test(boolean doStatusReport, boolean firstStatusReport) {
    String2.log("testMode=" + testMode);
    if (doStatusReport) String2.log("Status Report:");
    String errorInNoColon = String2.ERROR + " in NetCheck.test";
    String errorIn = errorInNoColon + ": ";
    StringBuilder errorSB = new StringBuilder();
    String result[] = new String[netCheckTests.size()];
    Arrays.fill(result, "");

    try {
      // set up the email lists
      ArrayList<String> emailRecipients = new ArrayList();
      ArrayList<StringBuilder> emailContents = new ArrayList(); // parallels emailRecipients

      // do the tests
      for (int i = 0; i < netCheckTests.size(); i++) {

        // do a test in a separate thread
        NetCheckTest netCheckTest = (NetCheckTest) netCheckTests.get(i);
        if (verbose)
          String2.log(
              "\n"
                  + netCheckTest.getTitle()
                  + "    "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ());
        if (netCheckTest instanceof PauseTest) {
          netCheckTest.test();
          continue;
        }
        double tMustRespondWithinSeconds = netCheckTest.getMustRespondWithinSeconds();
        if (!Double.isFinite(tMustRespondWithinSeconds) || tMustRespondWithinSeconds <= 0)
          tMustRespondWithinSeconds = mustRespondWithinSeconds;
        // if (verbose) String2.log("mustRespondWithinSeconds=" + tMustRespondWithinSeconds");
        TestThread testThread = new TestThread(netCheckTest);
        testThread.start();
        Math2.sleep(100); // give testThread a chance to get started
        int joinMillis = Math2.roundToInt(tMustRespondWithinSeconds * 1000);
        testThread.join(joinMillis);
        String warning = "";
        boolean wasStopped = false;
        if (testThread.isAlive()) {
          // if (verbose) String2.log("joinMillis=" + joinMillis + " trying to stop thread at " +
          //    Calendar2.getCurrentISODateTimeStringLocalTZ());
          wasStopped = true;
          testThread.interrupt();
          testThread.join(5000); // wait 5 seconds more
          if (testThread.isAlive()) {
            testThread.stop(); // stop it
            testThread.join(5000); // wait 5 seconds more
            warning = "  WARNING: thread '" + netCheckTest.getTitle() + "' was stopped.\n";
          } else {
            warning = "  NOTE: thread.interrupt worked.\n";
          }
        }
        result[i] = testThread.getResult();
        if (wasStopped
            || result[i] == null) // it will often be null if thread was interrupted or stopped
        result[i] =
              netCheckTest.getDescription()
                  + "  "
                  + String2.ERROR
                  + ": no response (mustRespondWithinSeconds = "
                  + tMustRespondWithinSeconds
                  + ").\n";

        boolean pass = result[i].length() == 0;
        if (pass) nPass[i]++;
        nTry[i]++;

        // did the result change?
        int po = result[i].indexOf(NetCheckTest.END_SHORT_SECTION);
        if (po < 0) po = result[i].length();
        String shortResult = result[i].substring(0, po);

        if (lastResult[i] == null) lastResult[i] = "";
        po = lastResult[i].indexOf(NetCheckTest.END_SHORT_SECTION);
        if (po < 0) po = lastResult[i].length();
        String shortLastResult = lastResult[i].substring(0, po);
        // if (verbose) String2.log("shortResult    =" + shortResult);
        // String2.log("shortLastResult=" + shortLastResult);

        // generate the headline
        String headline = pass ? CurrentStatusPASS : CurrentStatusFAIL;
        boolean changed = false;
        if (doStatusReport && firstStatusReport) {
          // don't say CHANGED on first round of tests
        } else if ((shortResult.length() == 0) && (shortLastResult.length() == 0)) {
          // no errors
        } else if ((result[i].length() == 0) ^ (lastResult[i].length() == 0)) {
          headline += "(status " + CHANGED;
          changed = true;
        } else if (!shortResult.equals(shortLastResult)) {
          headline += "(message" + CHANGED;
          changed = true;
        }
        if (doStatusReport && !firstStatusReport)
          headline += passPercent + String2.right((nPass[i] * 100 / nTry[i]) + "%", 4) + ") ";
        headline += netCheckTest.getTitle() + "\n";
        HashSet<String> notifiedReThisTest = new HashSet();

        // addResults to emails to subscribers
        if (testMode) {
          // tell administrator (and only administrator) everything
          addResults(
              emailRecipients,
              emailContents,
              emailAdministrator,
              notifiedReThisTest,
              headline + result[i]);

        } else {
          // Do things in this order to be more likely to send full error message than just headline
          //  in cases where subscriber has >1 subscriptions for this test.

          // Add all results to email to each subscriber, so people see status of all tests they are
          // interested in.
          // Email will only be sent (see below) if doStatusReport or someChanged.

          // first,
          // add changes subscribers for this test
          addResults(
              emailRecipients,
              emailContents,
              netCheckTest.getEmailChangesTo(),
              notifiedReThisTest,
              headline + result[i]);
          addResults(
              emailRecipients,
              emailContents,
              netCheckTest.getEmailChangeHeadlinesTo(),
              notifiedReThisTest,
              headline + shortResult);

          // add changes subscribers for all tests
          addResults(
              emailRecipients,
              emailContents,
              emailChangesTo,
              notifiedReThisTest,
              headline + result[i]);
          addResults(
              emailRecipients,
              emailContents,
              emailChangeHeadlinesTo,
              notifiedReThisTest,
              headline + shortResult);

          // second,
          // add Status subscribers for this test
          addResults(
              emailRecipients,
              emailContents,
              netCheckTest.getEmailStatusTo(),
              notifiedReThisTest,
              headline + result[i]);
          addResults(
              emailRecipients,
              emailContents,
              netCheckTest.getEmailStatusHeadlinesTo(),
              notifiedReThisTest,
              headline + shortResult);

          // add Status subscribers for all tests
          addResults(
              emailRecipients,
              emailContents,
              emailStatusTo,
              notifiedReThisTest,
              headline + result[i]);
          addResults(
              emailRecipients,
              emailContents,
              emailStatusHeadlinesTo,
              notifiedReThisTest,
              headline + shortResult);
        }

        // print results to log and screen
        String2.logNoNewline(headline + (verbose ? result[i] : "") + warning);
      }

      // send the emails
      String footer = "\nEnd of report (" + Calendar2.getCurrentISODateTimeStringLocalTZ() + ").\n";
      String2.log(footer);
      for (int i = 0; i < emailRecipients.size(); i++) {
        StringBuilder contents = emailContents.get(i);
        if (doStatusReport) contents.insert(0, "Status Report:\n\n");
        contents.append(footer);
        boolean allPassed = contents.indexOf(CurrentStatusFAIL) < 0;
        boolean someChanged = contents.indexOf(CHANGED) >= 0;
        String subject = "NetCheck Report: " + (allPassed ? "All PASSED" : "Some FAILED") + "\n";
        try {
          if (doStatusReport || someChanged) {
            if (verbose) String2.log("sending email to " + emailRecipients.get(i));
            SSR.sendEmail(
                smtpServer,
                smtpPort,
                smtpUser,
                smtpPassword,
                smtpProperties,
                smtpFromAddress,
                emailRecipients.get(i),
                subject,
                contents.toString());
          }
        } catch (Exception e) {
          errorSB.append(MustBe.throwable(errorIn, e));
        }
      }

    } catch (Exception e) {
      errorSB.append(MustBe.throwable(errorInNoColon, e));
    }

    // copy result[] to lastResult[]
    lastResult = result; // just copy, since result is new each time

    // if doStatusReport, clear the arrays
    if (doStatusReport) {
      Arrays.fill(nPass, 0);
      Arrays.fill(nTry, 0);
    }

    // if error occurred, send email to smtpFromAddress
    if (errorSB.length() > 0) {
      String2.log(errorSB.toString());
      try {
        SSR.sendEmail(
            smtpServer,
            smtpPort,
            smtpUser,
            smtpPassword,
            smtpProperties,
            smtpFromAddress,
            smtpFromAddress,
            String2.ERROR + " while running NetCheck tests",
            errorSB.toString());
      } catch (Exception e) {
        errorSB.append(MustBe.throwable(errorIn, e));
      }
    }

    // if memory use is >100 MB, send email to smtpFromAddress
    long memoryMB = Math2.getMemoryInUse() / 1000000;
    if (memoryMB >= memoryWarningMB) {
      try {
        String tMessage = "Warning: NetCheck is using " + memoryMB + " MB.";
        String2.log(tMessage);
        SSR.sendEmail(
            smtpServer,
            smtpPort,
            smtpUser,
            smtpPassword,
            smtpProperties,
            smtpFromAddress,
            smtpFromAddress,
            tMessage,
            tMessage
                + "  When a test times out, the thread is stopped and this may cause a memory leak.  "
                + "Please consider restarting NetCheck.");
      } catch (Exception e) {
        errorSB.append(MustBe.throwable(errorIn, e));
      }
    }

    return errorSB.toString();
  }

  /**
   * For each subscriber, this looks for the subscriber in notifiedReThisTest and emailRecipients
   * (of this test), adds subscriber if not there, and appends results to the emailContents for that
   * recipient.
   *
   * @param emailRecipients is a list of subscribers who are going to receive emails
   * @param emailContents is a list of email contents, for each subscriber on the emailRecipients
   *     list
   * @param subscribers is a list of subscribers to one test
   * @param notifiedReThisTest is a hashset of subscribers who have been notified about this test's
   *     results
   * @param result is the result string for one test
   */
  private static void addResults(
      ArrayList<String> emailRecipients,
      ArrayList<StringBuilder> emailContents,
      ArrayList<String> subscribers,
      HashSet<String> notifiedReThisTest,
      String result) {

    for (int i = 0; i < subscribers.size(); i++) {
      // subscriber already notified re this test?
      String subscriber = subscribers.get(i);
      if (notifiedReThisTest.add(
          subscriber)) { // true if subscriber wasn't already in set, i.e. new

        // look for subscriber in the list
        int po = String2.indexOf(emailRecipients.toArray(), subscriber);
        if (po >= 0) {
          // the recipient is already in the list
          emailContents.get(po).append(result);
        } else {
          // add the recepient to the list
          emailRecipients.add(subscriber);
          StringBuilder sb = new StringBuilder(result);
          emailContents.add(sb);
        }
      }
    }
  }

  /**
   * A unit test of the XxxTest classes used by NetCheck.
   *
   * @throws Exception if trouble
   */
  public static void unitTest() throws Exception {
    HttpTest.unitTest();
    OpendapTest.unitTest();
    // SftpTest.unitTest(); //orpheus Shell authentication started failing ~2010-06
  }

  /**
   * This runs NetCheck.
   *
   * @param args must be the full name of the setup xml file
   */
  public static void main(String args[]) throws Exception {
    NetCheck.verbose = true;

    boolean testMode = false;
    String xmlName = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].toLowerCase().equals("-testmode")) testMode = true;
      else if (args[i].endsWith(".xml")) xmlName = args[i];
    }

    if (xmlName == null) {
      String2.log("Usage: netcheck [-testmode] <setupFileName.xml> ");
      String2.log("The order of items on the command line is not important.");
      String2.log("The name of the setup file must end in \".xml\".");
      System.exit(0);
    }

    NetCheck netCheck = new NetCheck(xmlName, testMode);
  }
}
