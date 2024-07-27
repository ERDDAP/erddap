/*
 * PauseTest Copyright 2009, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

/**
 * This looks like a test, but just pauses for n seconds.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-07-20
 */
public class PauseTest extends NetCheckTest {

  private int nSeconds;

  /**
   * This constructor loads the information for a test with information from the xmlReader. The
   * xmlReader.getNextTag() should have just returned with the initial reference to this class. The
   * method will read information until xmlReader.getNextTag() returns the close tag for the
   * reference to this class.
   *
   * @param xmlReader
   * @throws Exception if trouble
   */
  public PauseTest(SimpleXMLReader xmlReader) throws Exception {
    String errorIn = String2.ERROR + " in PauseTest constructor: ";

    // ensure the xmlReader is just starting with this class
    Test.ensureEqual(
        xmlReader.allTags(), "<netCheck><pauseTest>", errorIn + "incorrect initial tags.");

    // read the xml properties file
    xmlReader.nextTag();
    String tags = xmlReader.allTags();
    int iteration = 0;
    while (!tags.equals("<netCheck></pauseTest>") && iteration++ < 1000000) {
      // process the tags
      if (verbose) String2.log(tags + xmlReader.content());
      if (tags.equals("<netCheck><pauseTest><title>")) {
      } else if (tags.equals("<netCheck><pauseTest></title>")) title = xmlReader.content();
      else if (tags.equals("<netCheck><pauseTest><nSeconds>")) {
      } else if (tags.equals("<netCheck><pauseTest></nSeconds>"))
        nSeconds = String2.parseInt(xmlReader.content());
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
   * methods of the superclass NetCheckTest.
   *
   * @param title e.g., Pause for 10 seconds
   * @param nSeconds
   * @throws Exception if trouble
   */
  public PauseTest(String title, int nSeconds) throws Exception {

    // required
    this.title = title;
    this.nSeconds = nSeconds;

    // ensure that the required values are set
    ensureValid();
  }

  /**
   * This is used by the constructors to ensure that all the required values were set.
   *
   * <p>This is unusual in that it asks for the password via SystemIn. This is the only way to get
   * the password into this class. This approach prevents passwords from being stored in code (e.g.,
   * unit tests).
   *
   * @throws Exception if trouble
   */
  public void ensureValid() throws Exception {
    String errorIn = String2.ERROR + " in PauseTest.ensureValid: ";

    // ensure that required items were set
    Test.ensureEqual(
        title == null || title.length() == 0,
        false,
        errorIn + "<netCheck><pauseTest><title> was not specified.\n");
    Test.ensureTrue(
        nSeconds <= 600,
        errorIn + "<netCheck><pauseTest><nSeconds>=" + nSeconds + " must be less than 600.");
  }

  /**
   * This does the test and returns an error string ("" if no error). This does not send out emails.
   * This won't throw an Exception.
   *
   * @return an error string (always "").
   */
  @Override
  public String test() {
    Math2.sleep(nSeconds * 1000L);
    return "";
  }

  /**
   * This returns a description of this test (suitable for putting at the top of an error message),
   * with " " at the start and \n at the end.
   *
   * @return a description
   */
  @Override
  public String getDescription() {
    return "  nSeconds: " + nSeconds + "\n";
  }
}
