/*
 * NetCheckTest Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.netcheck;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.util.ArrayList;

/**
 * NetCheckTest describes the methods which NetCheck XxxTest classes must have.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-08-16
 */
public abstract class NetCheckTest {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /** A constant value used to separate an error message's short section and long section. */
  public static final String END_SHORT_SECTION = "[END_SHORT_SECTION]\n";

  protected String title;
  protected double mustRespondWithinSeconds = Double.NaN;
  protected ArrayList emailStatusTo = new ArrayList();
  protected ArrayList emailStatusHeadlinesTo = new ArrayList();
  protected ArrayList emailChangesTo = new ArrayList();
  protected ArrayList emailChangeHeadlinesTo = new ArrayList();

  /**
   * The gets the title of this test (usually <40 characters).
   *
   * @return the title of this test
   */
  public String getTitle() {
    return title;
  }

  /**
   * The sets the time out for this test.
   *
   * @param seconds (use Double.NaN for no limit)
   */
  public void setMustRespondWithinSeconds(double seconds) {
    mustRespondWithinSeconds = seconds;
  }

  /**
   * The gets the time out for this test.
   *
   * @return mustRespondWithinSeconds (may be NaN!)
   */
  public double getMustRespondWithinSeconds() {
    return mustRespondWithinSeconds;
  }

  /**
   * This gets the list of email addresses for Status results.
   *
   * @return emailStatusTo
   */
  public ArrayList getEmailStatusTo() {
    return emailStatusTo;
  }

  /**
   * This adds an email address to emailStatusTo.
   *
   * @param emailAddress
   */
  public void addEmailStatusTo(String emailAddress) {
    String errorIn = String2.ERROR + " in NetCheckTest.addEmailStatusTo: ";
    Test.ensureNotNull(emailAddress, errorIn + " emailAddress is null.");
    Test.ensureTrue(emailAddress.length() > 0, errorIn + " emailAddress must not be \"\".");
    emailStatusTo.add(emailAddress);
  }

  /**
   * This gets the list of email addresses for Status headlines.
   *
   * @return emailStatusHeadlinesTo
   */
  public ArrayList getEmailStatusHeadlinesTo() {
    return emailStatusHeadlinesTo;
  }

  /**
   * This adds an email address to emailStatusHeadlinesTo.
   *
   * @param emailAddress
   */
  public void addEmailStatusHeadlinesTo(String emailAddress) {
    String errorIn = String2.ERROR + " in NetCheckTest.addEmailStatusHeadlinesTo: ";
    Test.ensureNotNull(emailAddress, errorIn + " emailAddress is null.");
    Test.ensureTrue(emailAddress.length() > 0, errorIn + " emailAddress must not be \"\".");
    emailStatusHeadlinesTo.add(emailAddress);
  }

  /**
   * This gets the list of email addresses for changed results only.
   *
   * @return emailChangesTo
   */
  public ArrayList getEmailChangesTo() {
    return emailChangesTo;
  }

  /**
   * This adds an email address to emailChangesTo.
   *
   * @param emailAddress
   */
  public void addEmailChangesTo(String emailAddress) {
    String errorIn = String2.ERROR + " in NetCheckTest.addEmailChangesTo: ";
    Test.ensureNotNull(emailAddress, errorIn + " emailAddress is null.");
    Test.ensureTrue(emailAddress.length() > 0, errorIn + " emailAddress must not be \"\".");
    emailChangesTo.add(emailAddress);
  }

  /**
   * This gets the list of email addresses for changed result headlines only.
   *
   * @return emailChangeHeadlinesTo
   */
  public ArrayList getEmailChangeHeadlinesTo() {
    return emailChangeHeadlinesTo;
  }

  /**
   * This adds an email address to emailChangeHeadlinesTo.
   *
   * @param emailAddress
   */
  public void addEmailChangeHeadlinesTo(String emailAddress) {
    String errorIn = String2.ERROR + " in NetCheckTest.addEmailChangeHeadlinesTo: ";
    Test.ensureNotNull(emailAddress, errorIn + " emailAddress is null.");
    Test.ensureTrue(emailAddress.length() > 0, errorIn + " emailAddress must not be \"\".");
    emailChangeHeadlinesTo.add(emailAddress);
  }

  /**
   * This does the test and returns an error string ("" if no error). This does not send out emails.
   * This won't throw an Exception.
   *
   * @return an error string ("" if no error). If there is an error, this will end with '\n'. If the
   *     error has a short section followed by a longer section, SftpTest.END_SHORT_SECTION will
   *     separate the two sections.
   */
  public abstract String test();

  /**
   * This returns a description of this test (suitable for putting at the top of an error message),
   * with " " at the start and \n at the end.
   *
   * @return a description
   */
  public abstract String getDescription();
}
