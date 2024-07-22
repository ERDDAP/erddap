/*
 * HttpTest Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import java.util.ArrayList;

/**
 * This deals with one type of netCheck test: validity of an HTTP request.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-08-16
 */
public class HttpTest extends NetCheckTest {

  private String url;
  private ArrayList<String> responseMustInclude = new ArrayList();
  private ArrayList<String> responseMustNotInclude = new ArrayList();

  /**
   * This constructor loads the information for a test with information from the xmlReader. The
   * xmlReader.getNextTag() should have just returned with the initial reference to this class. The
   * method will read information until xmlReader.getNextTag() returns the close tag for the
   * reference to this class.
   *
   * @param xmlReader
   * @throws Exception if trouble
   */
  public HttpTest(SimpleXMLReader xmlReader) throws Exception {
    String errorIn = String2.ERROR + " in HttpTest constructor: ";

    // ensure the xmlReader is just starting with this class
    Test.ensureEqual(
        xmlReader.allTags(), "<netCheck><httpTest>", errorIn + "incorrect initial tags.");

    // read the xml properties file
    xmlReader.nextTag();
    String tags = xmlReader.allTags();
    int iteration = 0;
    while (!tags.equals("<netCheck></httpTest>") && iteration++ < 1000000) {
      // process the tags
      if (verbose) String2.log(tags + xmlReader.content());
      if (tags.equals("<netCheck><httpTest><title>")) {
      } else if (tags.equals("<netCheck><httpTest></title>")) title = xmlReader.content();
      else if (tags.equals("<netCheck><httpTest><url>")) {
      } else if (tags.equals("<netCheck><httpTest></url>")) url = xmlReader.content();
      else if (tags.equals("<netCheck><httpTest><mustRespondWithinSeconds>")) {
      } else if (tags.equals("<netCheck><httpTest></mustRespondWithinSeconds>"))
        mustRespondWithinSeconds = String2.parseDouble(xmlReader.content());
      else if (tags.equals("<netCheck><httpTest><responseMustInclude>")) {
      } else if (tags.equals("<netCheck><httpTest></responseMustInclude>"))
        responseMustInclude.add(xmlReader.content());
      else if (tags.equals("<netCheck><httpTest><responseMustNotInclude>")) {
      } else if (tags.equals("<netCheck><httpTest></responseMustNotInclude>"))
        responseMustNotInclude.add(xmlReader.content());
      else if (tags.equals("<netCheck><httpTest><emailStatusTo>")) {
      } else if (tags.equals("<netCheck><httpTest></emailStatusTo>"))
        emailStatusTo.add(xmlReader.content());
      else if (tags.equals("<netCheck><httpTest><emailStatusHeadlinesTo>")) {
      } else if (tags.equals("<netCheck><httpTest></emailStatusHeadlinesTo>"))
        emailStatusHeadlinesTo.add(xmlReader.content());
      else if (tags.equals("<netCheck><httpTest><emailChangesTo>")) {
      } else if (tags.equals("<netCheck><httpTest></emailChangesTo>"))
        emailChangesTo.add(xmlReader.content());
      else if (tags.equals("<netCheck><httpTest><emailChangeHeadlinesTo>")) {
      } else if (tags.equals("<netCheck><httpTest></emailChangeHeadlinesTo>"))
        emailChangeHeadlinesTo.add(xmlReader.content());
      else throw new RuntimeException(errorIn + "unrecognized tags: " + tags);

      // get the next tags
      xmlReader.nextTag();
      tags = xmlReader.allTags();
    }

    // ensure that the required values are set
    ensureValid();
  }

  /**
   * A constructor for setting up the test. You can set other values with their setXxx or addXxx
   * methods of the superclass NetCheckTest and addResponseMustInclude and
   * addResponseMustNotInclude.
   *
   * @param title e.g., THREDDS Opendap GAssta
   * @param url the url, already percent encoded as needed.
   * @throws Exception if trouble
   */
  public HttpTest(String title, String url) throws Exception {

    String errorIn = String2.ERROR + " in HttpTest constructor: ";

    // required
    this.title = title;
    this.url = url;

    // ensure that the required values are set
    ensureValid();
  }

  /**
   * This is used by the constructors to ensure that all the required values were set.
   *
   * @throws Exception if trouble
   */
  public void ensureValid() throws Exception {
    String errorIn = String2.ERROR + " in HttpTest.ensureValid: ";

    // ensure that required items were set
    Test.ensureTrue(
        title != null && title.length() > 0,
        errorIn + "<netCheck><opendapTest><title> was not specified.\n");
    Test.ensureTrue(
        url != null && url.length() > 0,
        errorIn + "<netCheck><opendapTest><url> was not specified.\n");
  }

  /**
   * For use after the constructor and before test(), this adds another String that must be in the
   * HTTP response.
   *
   * @param mustInclude
   */
  public void addResponseMustInclude(String mustInclude) {
    String errorIn = String2.ERROR + " in HttpTest.addResponseMustInclude: ";
    Test.ensureNotNull(mustInclude, errorIn + " mustInclude is null.");
    Test.ensureTrue(mustInclude.length() > 0, errorIn + " mustInclude must not be \"\".");
    responseMustInclude.add(mustInclude);
  }

  /**
   * For use after the constructor and before test(), this adds another String that must not be in
   * the HTTP response.
   *
   * @param mustNotInclude
   */
  public void addResponseMustNotInclude(String mustNotInclude) {
    String errorIn = String2.ERROR + " in HttpTest.addResponseMustNotInclude: ";
    Test.ensureNotNull(mustNotInclude, errorIn + " mustNotInclude is null.");
    Test.ensureTrue(mustNotInclude.length() > 0, errorIn + " mustNotInclude must not be \"\".");
    responseMustNotInclude.add(mustNotInclude);
  }

  /**
   * This does the test and returns an error string ("" if no error). This does not send out emails.
   * This won't throw an Exception.
   *
   * @return an error string ("" if no error). If there is an error, this will end with '\n'. If the
   *     error has a short section followed by a longer section, NetCheckTest.END_SHORT_SECTION will
   *     separate the two sections.
   */
  @Override
  public String test() {
    try {
      // get the response
      long time = System.currentTimeMillis();
      String response = SSR.getUrlResponseStringUnchanged(url);
      time = System.currentTimeMillis() - time;

      // check mustRespondWithinSeconds
      StringBuilder errorSB = new StringBuilder();
      if (Double.isFinite(mustRespondWithinSeconds) && time > mustRespondWithinSeconds * 1000) {
        errorSB.append(
            "  "
                + String2.ERROR
                + ": response time ("
                + (time / 1000.0)
                + " s) was too slow (mustRespondWithinSeconds = "
                + mustRespondWithinSeconds
                + ").\n");
      }

      // check for responseMustInclude
      for (int i = 0; i < responseMustInclude.size(); i++) {
        String required = responseMustInclude.get(i);
        if (response.indexOf(required) < 0)
          errorSB.append("  " + String2.ERROR + ": response must include \"" + required + "\".\n");
      }

      // check for responseMustNotInclude
      for (int i = 0; i < responseMustNotInclude.size(); i++) {
        String undesired = responseMustNotInclude.get(i);
        if (response.indexOf(undesired) >= 0)
          errorSB.append(
              "  " + String2.ERROR + ": response must not include \"" + undesired + "\".\n");
      }

      // if there was trouble, include the url (at the start) and response (at the end) of the error
      // message
      if (errorSB.length() > 0) {
        errorSB.insert(0, getDescription());
        errorSB.append(END_SHORT_SECTION + "  **** Begin HTTP response.\n    ");
        String2.replaceAll(response, "\n", "\n    ");
        errorSB.append(response);
        errorSB.append("\n  **** End HTTP response.\n\n");
      }

      return errorSB.toString();
    } catch (Exception e) {
      return MustBe.throwable("HttpTest.test\n" + getDescription(), e);
    }
  }

  /**
   * This returns a description of this test (suitable for putting at the top of an error message),
   * with " " at the start and \n at the end.
   *
   * @return a description
   */
  @Override
  public String getDescription() {
    return "  URL: " + url + "\n";
  }

  /**
   * A unit test of this class.
   *
   * @throws Exception if trouble
   */
  public static void unitTest() throws Exception {
    String2.log("\n*** netcheck.HttpTest");
    long time = System.currentTimeMillis();
    HttpTest httpTest =
        new HttpTest(
            "OceanWatch LAS",
            // "https://oceanwatch.pfeg.noaa.gov/");   //old
            "http://las.pfeg.noaa.gov/oceanWatch/oceanwatch_safari.php"); // new (8/16/06)
    httpTest.addResponseMustInclude("AVHRR");
    httpTest.addResponseMustNotInclude("ZZTop");
    String2.log(httpTest.getDescription());
    String error = httpTest.test();
    Test.ensureEqual(error, "", String2.ERROR + " in HttpTest.unitTest:\n" + error);
    String2.log(
        "netcheck.HttpTest finished successfully   time="
            + (System.currentTimeMillis() - time)
            + "ms");
  }
}
