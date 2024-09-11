/*
 * EmailThread Copyright 2022, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.sun.mail.smtp.SMTPTransport;
import gov.noaa.pfel.coastwatch.util.SSR;
import jakarta.mail.Session;
import java.util.concurrent.TimeUnit;

/**
 * This sends emails which are queued in the EDStatic.emailList.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2022-08-27
 */
public class EmailThread extends Thread {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  // set while running
  private long lastStartTime = -1; // -1 if session not active
  public long lastSessionMillis = -1; // duration

  public static long defaultSleepMillis = 15000;
  public static long sleepMillis = defaultSleepMillis;

  /** The constructor. EmailThread uses email variables in EDStatic. */
  public EmailThread(int tNextEmail) {
    EDStatic.nextEmail = tNextEmail;
    EDStatic.lastFinishedEmail = tNextEmail - 1;
    setName("EmailThread");
  }

  /**
   * This returns elapsed time for the current email session (or -1 if no email session is running).
   */
  public long elapsedTime() {
    return lastStartTime == -1 ? -1 : System.currentTimeMillis() - lastStartTime;
  }

  /** This repeatedly: sleeps for sleepMillis, then sends all pending emails. */
  @Override
  public void run() {

    while (true) {

      // sleep
      Math2.sleep(sleepMillis);

      // check isInterrupted
      if (isInterrupted()) {
        String2.log(
            "%%% EmailThread was interrupted at " + Calendar2.getCurrentISODateTimeStringLocalTZ());
        return; // only return (stop thread) if interrupted
      }

      // THIS MIMICS SSR.sendEmail, but allows for sending many emails in one session

      // if no emails pending, continue
      if (EDStatic.nextEmail >= EDStatic.emailList.size()) continue;

      // get the SSR.emailLock
      try {
        if (!SSR.emailLock.tryLock(10, TimeUnit.SECONDS)) {
          String2.log(
              "%%% EmailThread ERROR: failed to get emailLock at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ());
          continue;
        }
      } catch (InterruptedException e) {
        String2.log("%%% EmailThread was interrupted.");
        return; // only return (stop thread) if interrupted
      }

      Session session = null;
      SMTPTransport smtpTransport = null;
      int nEmailsPerSession = 0;
      try { // with SSR.emailLock

        lastStartTime = System.currentTimeMillis();

        // get a session and smtpTransport
        String2.log(
            "%%% EmailThread openEmailSession at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());
        Object oar[] =
            SSR.openEmailSession(
                EDStatic.emailSmtpHost,
                EDStatic.emailSmtpPort, // throws Exception
                EDStatic.emailUserName,
                EDStatic.emailPassword,
                EDStatic.emailProperties);
        session = (Session) oar[0];
        smtpTransport = (SMTPTransport) oar[1];

        // send each of the emails
        while (EDStatic.nextEmail < EDStatic.emailList.size()) {

          // get email spec off emailList
          // Do these things quickly to keep internal consistency
          String emailOA[] = null;
          synchronized (EDStatic.emailList) {
            nEmailsPerSession++;
            EDStatic.nextEmail++;
            emailOA = EDStatic.emailList.get(EDStatic.nextEmail - 1);

            // treat it as immediately done.   Failures below won't be retried. I worry about queue
            // accumlating forever.
            EDStatic.lastFinishedEmail = EDStatic.nextEmail - 1;
            EDStatic.emailList.set(EDStatic.nextEmail - 1, null); // throw away the email info (gc)
          }

          // send one email
          long oneEmailTime = System.currentTimeMillis();
          try {
            SSR.lowSendEmail(
                session,
                smtpTransport,
                EDStatic.emailFromAddress,
                emailOA[0],
                emailOA[1],
                emailOA[2]); // toAddresses, subject, content);

            // email sent successfully
            oneEmailTime = System.currentTimeMillis() - oneEmailTime;
            String2.distributeTime(oneEmailTime, EDStatic.emailThreadSucceededDistribution24);
            String2.distributeTime(oneEmailTime, EDStatic.emailThreadSucceededDistributionTotal);
            String2.log(
                "%%% EmailThread successfully sent email #"
                    + (EDStatic.nextEmail - 1)
                    + " to "
                    + emailOA[0]
                    + ". elapsedTime="
                    + oneEmailTime
                    + "ms"
                    + (oneEmailTime > 10000 ? " (>10s!)" : ""));

          } catch (InterruptedException e) {
            String2.log("%%% EmailThread was interrupted.");
            return; // only return (stop thread) if interrupted

          } catch (Exception e) {
            // sending email failed
            oneEmailTime = System.currentTimeMillis() - oneEmailTime;
            String2.distributeTime(oneEmailTime, EDStatic.emailThreadFailedDistribution24);
            String2.distributeTime(oneEmailTime, EDStatic.emailThreadFailedDistributionTotal);
            String2.log(
                "%%% EmailThread ERROR sending email #"
                    + (EDStatic.nextEmail - 1)
                    + " to "
                    + emailOA[0]
                    + ". elapsedTime="
                    + oneEmailTime
                    + "ms"
                    + (oneEmailTime > 10000 ? " (>10s!)" : "")
                    + "\n"
                    + MustBe.throwableToString(e));
          }
        }

        // reset sleepMillis because openEmailSession succeeded
        sleepMillis = defaultSleepMillis;

        String2.log(
            "%%% EmailThread session finished after email #"
                + (EDStatic.nextEmail - 1)
                + " at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ());

      } catch (InterruptedException e) {
        String2.log("%%% EmailThread was interrupted.");
        return; // only return (stop thread) if interrupted

      } catch (Exception e) {
        // email session failed  //normally only if failed to start the session
        // tally as failure with time=0 (also shows up as nEmails/session = 0)
        String2.distributeTime(0, EDStatic.emailThreadFailedDistribution24);
        String2.distributeTime(0, EDStatic.emailThreadFailedDistributionTotal);
        String2.log(
            "%%% EmailThread session ERROR at email #"
                + (EDStatic.nextEmail - 1)
                + " at "
                + Calendar2.getCurrentISODateTimeStringLocalTZ()
                + "\n"
                + MustBe.throwableToString(e));

        // openEmailSession and other failures: wait longer before try again to avoid e.g.,
        //  "jakarta.mail.AuthenticationFailedException: 454 4.7.0 Too many login attempts, please
        // try again later."
        if (sleepMillis * 2 < 5 * Calendar2.MILLIS_PER_MINUTE) sleepMillis *= 2;

      } finally {
        lastStartTime = -1;
        try {
          if (smtpTransport != null) smtpTransport.close();
        } catch (Throwable t) {
        }
        try {
          SSR.emailLock.unlock(); // This should be locked.  If not, this throws an
          // IllegaMonitorStateException.
        } catch (Throwable t) {
        }

        // note: failed session shows up as 0 emailsPerSession
        String2.distributeCount(nEmailsPerSession, EDStatic.emailThreadNEmailsDistribution24);
        String2.distributeCount(nEmailsPerSession, EDStatic.emailThreadNEmailsDistributionTotal);

        // if >=200 pending emails, dump the first 100 of them
        try {
          synchronized (EDStatic.emailList) {
            if (EDStatic.emailList.size() - EDStatic.nextEmail >= 200) {
              int oNextEmail = EDStatic.nextEmail;
              while (EDStatic.emailList.size() - EDStatic.nextEmail > 100) {
                EDStatic.emailList.set(EDStatic.nextEmail++, null);
              }
              EDStatic.lastFinishedEmail = EDStatic.nextEmail - 1;
              String2.log(
                  "%%% EmailThread ERROR: I'm having trouble sending emails, so I dumped emails #"
                      + oNextEmail
                      + " through "
                      + (EDStatic.nextEmail - 2)
                      + ".");
            }
          }
        } catch (Throwable t) {
        }
      }
    } // while (true)
  }
}
