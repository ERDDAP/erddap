/*
 * Subscriptions Copyright 2008, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.IntArray;
import com.cohort.array.StringArray;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This class maintains the subscriptions for people who want to be notified when a dataset has
 * changed.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-12-01
 */
public class Subscriptions {
  // FUTURE: more efficient to switch this from synchronized to using
  // java.util.concurrent.locks.ReentrantReadWriteLock.
  // (I think ConcurrentHashMap not enough since often a few data structures need to be modified
  // atomically.)
  // But currently, it is low-use so no need to switch.

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  // these constants are reflected in constructor
  // note that these mustn't change (else trouble for existing subscriptions file)
  public static final int statusColumn = 0; // binaryByte 1
  public static final int creationMinuteColumn = 1; // int 4
  public static final int keyColumn = 2; // int 4
  public static final int datasetIDColumn = 3; // String 80
  public static final int emailColumn = 4; // String 120
  public static final int actionColumn = 5; // String 4000

  // max lengths -- changing these will change/damage(?) the persistentTable -- see below
  public static final int DATASETID_LENGTH = 80;
  public static final int EMAIL_LENGTH = 120;
  public static final int ACTION_LENGTH = 4000;

  public static final byte STATUS_EMPTY = (byte) ' '; // best since it is default for empty row
  public static final byte STATUS_PENDING = (byte) 'P';
  public static final byte STATUS_VALID = (byte) 'V';

  public static final String INDEX_HTML = "subscriptions/index.html";
  // this is also in messages.xml <subscriptionValidateHtml>
  public static final String ADD_HTML = "subscriptions/add.html";
  public static final String VALIDATE_HTML = "subscriptions/validate.html";
  public static final String REMOVE_HTML = "subscriptions/remove.html";
  // this is also in messages.xml <subscriptionRemoveHtml>
  public static final String LIST_HTML = "subscriptions/list.html";

  // *** things set by constructor
  /** Has subscription info. */
  protected int maxMinutesPending;

  protected String preferredErddapUrl; // preferrably the https url
  protected PersistentTable persistentTable;
  protected HashSet<String> emailBlacklist = new HashSet();

  /**
   * From the point of view of a dataset: which subscriptions are for a given dataset.
   * key=datasetID, value=HashSet of persistentTable row numbers
   */
  protected HashMap<String, HashSet> datasetSubscriptions = new HashMap();

  /**
   * From the point of view of email addresses: which subscriptions are related to that email
   * address. key=email, value=HashSet of persistentTable row numbers for valid and pending
   * subscriptions
   */
  protected HashMap<String, HashSet> emailSubscriptions = new HashMap();

  /** key=comboKey, value=persistentTable Integer row number */
  protected HashMap<String, Integer> pendingSubscriptions = new HashMap();

  /** key=comboKey, value=persistentTable Integer row number */
  protected HashMap<String, Integer> validSubscriptions = new HashMap();

  /**
   * The constructor for Subscriptions. If fullFuleName exists, the info will be loaded. If there is
   * a need for system repair: call subscriptions.close(), fix the file, use new Subscriptions().
   *
   * <p>If exception occurs in constructor, it will stop erddap construction and force admin to fix
   * or delete the persistentTable file.
   *
   * @param fullFileName the full file name for the subscriptions
   * @param maxHoursPending the maximum number of hours before a pending subscription can be deleted
   *     (e.g., 48).
   * @param tPreferredErddapUrl the https (best) or the http (next best) url for erddap.
   */
  public Subscriptions(String fullFileName, int maxHoursPending, String tPreferredErddapUrl)
      throws IOException {

    persistentTable =
        new PersistentTable(
            fullFileName,
            "rw",
            new int[] {
              PersistentTable.BINARY_BYTE_LENGTH,
              PersistentTable.INT_LENGTH,
              PersistentTable.INT_LENGTH,
              DATASETID_LENGTH,
              EMAIL_LENGTH,
              ACTION_LENGTH
            });
    maxMinutesPending = 60 * Math.max(1, maxHoursPending);
    preferredErddapUrl = tPreferredErddapUrl;

    // setup subscription hashmaps
    // constructor is only called in one thread -- no need to synchronize
    int nValid = 0;
    int nRows = persistentTable.nRows();
    for (int row = 0; row < nRows; row++) {
      byte status = readStatus(row);
      if (status == STATUS_VALID) {
        // addToSubscriptions
        addDatasetSubscription(readDatasetID(row), row);
        addEmailSubscription(readEmail(row), row);
        addPVSubscription(validSubscriptions, readComboKey(row), row);
        nValid++;
      } else if (status == STATUS_PENDING) {
        addPVSubscription(pendingSubscriptions, readComboKey(row), row);
      }
    }
    removeOldPending();
    if (verbose) String2.log("Subscriptions loaded successfully. nValid=" + nValid);
  }

  /** This returns this ERDDAP's preferred baseUrl. */
  public String preferredErddapUrl() {
    return preferredErddapUrl;
  }

  /**
   * This flushes and closes the persistentTable. Future operations on this instance will fail. If
   * the program crashes, similar things are done automatically.
   */
  public synchronized void close() throws IOException {
    if (persistentTable != null) {
      try {
        persistentTable.close();
      } catch (Exception e) {
      }
      persistentTable = null;
    }
  }

  /**
   * Users of this class shouldn't call this -- use close() instead. Java calls this when an object
   * is no longer used, just before garbage collection.
   */
  protected synchronized void finalize() throws Throwable {
    try { // extra insurance
      close();
    } catch (Throwable t) {
    }
    super.finalize();
  }

  /**
   * This sets the email blacklist (any previous list is thrown out). <br>
   * Use the name "*" to blacklist an entire domain, e.g., *@domain.com . <br>
   * All valid and pending subscriptions for these email addresses will be removed immediately. <br>
   * No email is sent to user to notify them. <br>
   * Future requests from these email addresses will be rejected. <br>
   * This shouldn't throw an Exception.
   *
   * @param blacklistCsv a comma-separated list of email addresses (spaces before and after the
   *     commas are ignored)
   * @return the number of valid and pending subscriptions which were removed.
   */
  public synchronized int setEmailBlacklist(String blacklistCsv) {
    int nRemoved = 0;
    try {
      // set up new blacklist
      StringArray sa = StringArray.fromCSV(blacklistCsv);
      // append common disposable email addresses
      sa.add(
          new String[] {
            // from https://www.guerrillamail.com/
            "*@sharklasers.com",
            "*@grr.la",
            "*@guerrillamail.biz",
            "*@guerrillamail.com",
            "*@guerrillamail.de",
            "*@guerrillamail.net",
            "*@guerrillamail.org",
            "*@guerrillamailblock.com",
            "*@pokemail.net",
            "*@spam4.me",
            // from https://temp-mail.org/en/
            "*@geronra.com", // I suspect it changes often
            // from https://getnada.com/
            "*@getnada.com",
            // from https://www.mailinator.com/
            "*@mailinator.com",
            // from https://app.inboxbear.com/login
            "*@inboxbear.com"
          });
      // Unfortunately there are several services that obviously frequently
      // change the domain of the email addresses, so no way to block them.
      emailBlacklist = new HashSet();
      for (int i = 0; i < sa.size(); i++) {
        String email = sa.get(i).toLowerCase(); // to do case insensitive test if on blacklist
        if (String2.isSomething(email) && email.indexOf('@') >= 0) // very loose test
        emailBlacklist.add(email);
      }

      // then go through list of subscribers emails, remove any on blacklist
      // key=email, value=HashSet of persistentTable row numbers for valid and pending subscriptions
      // */
      Set<String> emailSet = emailSubscriptions.keySet();
      for (String email : emailSet) {
        try {
          ensureEmailValid(email); // throws Throwable if it or *@domain.com is on blacklist
        } catch (Throwable t2) {
          // remove subscriptions for that email
          IntArray rows = getSortedEmailSubscriptions(email);
          if (rows != null) {
            for (int which = 0; which < rows.size(); which++) {
              int row = rows.get(which);
              remove(row, readKey(row));
              nRemoved++;
            }
          }
        }
      }

    } catch (Throwable eb) {
      String2.log("Error in Subscriptions.setEmailBlacklist:\n" + MustBe.throwableToString(eb));
    }
    if (verbose)
      String2.log(
          "Subscriptions.setEmailBlacklist removed "
              + nRemoved
              + " valid and pending subscriptions.");
    return nRemoved;
  }

  /**
   * Use addEmailSubscription or addDatasetSubscription instead of this! This adds Integer(row) to
   * the hashset associated with key in the emailSubscriptions or datasetSubscriptions map. Here,
   * key is the key for the map (not the subscription key).
   *
   * @return true if row was already in the hashset.
   */
  protected synchronized boolean _addSubscription(HashMap map, String key, int row) {
    HashSet rowNumbers = (HashSet) map.get(key);
    if (rowNumbers == null) {
      rowNumbers = new HashSet();
      map.put(key, rowNumbers);
    }
    return rowNumbers.add(Integer.valueOf(row));
  }

  protected synchronized boolean addEmailSubscription(String email, int row) {
    return _addSubscription(emailSubscriptions, email, row);
  }

  protected synchronized boolean addDatasetSubscription(String datasetID, int row) {
    return _addSubscription(datasetSubscriptions, datasetID, row);
  }

  /**
   * Use removeEmailSubscription or removeDatasetSubscription instead of this! This removes
   * Integer(row) from the hashset associated with the key in the emailSubscriptions or
   * datasetSubscriptions map. Here, key is the key for the map (not the subscription key).
   *
   * @return true if the row was in the hashset.
   */
  protected synchronized boolean _removeSubscription(
      HashMap<String, HashSet> map, String key, int row) {
    HashSet rowNumbers = map.get(key);
    if (rowNumbers == null) return false;
    boolean result = rowNumbers.remove(Integer.valueOf(row));
    if (result && rowNumbers.size() == 0) map.remove(key);
    return result;
  }

  protected synchronized boolean removeEmailSubscription(String email, int row) {
    return _removeSubscription(emailSubscriptions, email, row);
  }

  protected synchronized boolean removeDatasetSubscription(String datasetID, int row) {
    return _removeSubscription(datasetSubscriptions, datasetID, row);
  }

  /**
   * Use getSortedEmailSubscriptions or getSortedDatasetSubscriptions instead of this! This returns
   * a sorted IntArray with the row numbers from a hashSet from emailSubscriptions or
   * datasetSubscriptions.
   *
   * @return a sorted IntArray with persistent table row numbers, or null (if key not found)
   */
  protected synchronized IntArray _getSortedSubscriptions(
      HashMap<String, HashSet> map, String key) {
    HashSet hashSet = map.get(key);
    if (hashSet == null) return null;
    IntArray rows = new IntArray();
    Iterator<Integer> it = hashSet.iterator();
    while (it.hasNext()) rows.add(it.next().intValue());
    rows.sort();
    return rows;
  }

  protected synchronized IntArray getSortedEmailSubscriptions(String email) {
    return _getSortedSubscriptions(emailSubscriptions, email);
  }

  protected synchronized IntArray getSortedDatasetSubscriptions(String datasetID) {
    return _getSortedSubscriptions(datasetSubscriptions, datasetID);
  }

  /**
   * This adds comboKey = Integer(row) to the pendingSubscriptions or validSubscriptions map.
   *
   * @return true if the row was already in the hashmap.
   */
  protected synchronized boolean addPVSubscription(HashMap map, String comboKey, int row) {
    return map.put(comboKey, Integer.valueOf(row)) != null;
  }

  /**
   * This removes comboKey = Integer(row) from the pendingSubscriptions or validSubscriptions map.
   *
   * @return true if the row was in the hashmap.
   */
  protected synchronized boolean removePVSubscription(HashMap map, String comboKey) {
    return map.remove(comboKey) != null;
  }

  protected synchronized void writeStatus(int row, byte status) throws IOException {
    persistentTable.writeBinaryByte(statusColumn, row, status);
  }

  protected synchronized void writeCreationMinute(int row, int minute) throws IOException {
    persistentTable.writeInt(creationMinuteColumn, row, minute);
  }

  protected synchronized void writeKey(int row, int key) throws IOException {
    persistentTable.writeInt(keyColumn, row, key);
  }

  protected synchronized void writeDatasetID(int row, String id) throws IOException {
    persistentTable.writeString(datasetIDColumn, row, id);
  }

  protected synchronized void writeEmail(int row, String email) throws IOException {
    persistentTable.writeString(emailColumn, row, email);
  }

  protected synchronized void writeAction(int row, String action) throws IOException {
    persistentTable.writeString(actionColumn, row, action);
  }

  public synchronized byte readStatus(int row) throws IOException {
    return persistentTable.readBinaryByte(statusColumn, row);
  }

  public synchronized int readCreationMinute(int row) throws IOException {
    return persistentTable.readInt(creationMinuteColumn, row);
  }

  public synchronized int readKey(int row) throws IOException {
    return persistentTable.readInt(keyColumn, row);
  }

  public synchronized String readDatasetID(int row) throws IOException {
    return persistentTable.readString(datasetIDColumn, row);
  }

  public synchronized String readEmail(int row) throws IOException {
    return persistentTable.readString(emailColumn, row);
  }

  public synchronized String readAction(int row) throws IOException {
    return persistentTable.readString(actionColumn, row);
  }

  /** This returns datasetID\nemail\naction for a given row. */
  public synchronized String readComboKey(int row) throws IOException {
    return comboKey(readDatasetID(row), readEmail(row), readAction(row));
  }

  public synchronized String comboKey(String datasetID, String email, String action) {
    return datasetID + "\n" + email + "\n" + action;
  }

  /**
   * This returns a message commonly put at the end of emails with a reference to the url to request
   * a list of valid and pending subscriptions.
   */
  public String messageToRequestList(String email) {
    return "You can request an email with a list of all of your valid and pending subscriptions with this URL:\n"
        + preferredErddapUrl
        + "/"
        + LIST_HTML
        + "?email="
        + email
        + "\n";
  }

  /**
   * This tests that an email address is valid (syntax and blacklist).
   *
   * @param email
   * @return an error message or "" if no error.
   */
  public synchronized String testEmailValid(String email) {
    if (!String2.isEmailAddress(email)
        || email.startsWith("nobody@")
        || email.startsWith("your.name")
        || email.startsWith("your.email"))
      return String2.ERROR + ": \"" + email + "\" is not a valid email address.";
    if (email.length() > EMAIL_LENGTH)
      return String2.ERROR
          + ": emailAddress="
          + email
          + " has more than "
          + EMAIL_LENGTH
          + " characters.";
    int atPo = email.indexOf('@');
    // String2.log(">>email=" + email + "\n>>emailBlacklist=" + emailBlacklist.toString());
    // if (atPo > 0) String2.log(">>email2=*" + email.substring(atPo));
    email = email.toLowerCase(); // for testing purposes
    if (emailBlacklist.contains(email)
        || (atPo > 0
            && emailBlacklist.contains("*" + email.substring(atPo)))) // e.g., *@example.com
    return String2.ERROR + ": \"" + email + "\" is on the email blacklist.";
    return "";
  }

  /**
   * This ensures that an email address is valid.
   *
   * @param email
   * @throws Throwable if it isn't valid
   */
  public synchronized void ensureEmailValid(String email) throws Exception {
    String msg = testEmailValid(email);
    if (msg.length() > 0) throw new SimpleException(msg);
  }

  /**
   * This removes any pending subscriptions older than maxMinutesPending.
   *
   * @return the number of subscriptions removed.
   */
  public synchronized int removeOldPending() throws IOException {
    int oldestPendingAllowed =
        (int) (System.currentTimeMillis() / 60000) - maxMinutesPending; // safe
    int nPending = 0, nRemoved = 0;
    int nRows = persistentTable.nRows();
    Iterator it = pendingSubscriptions.keySet().iterator();
    while (it.hasNext()) {
      int row = pendingSubscriptions.get(it.next()).intValue();
      int creationMinute = (row < 0 || row >= nRows) ? -1 : readCreationMinute(row);
      if (creationMinute == Integer.MAX_VALUE || creationMinute < oldestPendingAllowed) {
        String email = readEmail(row); // before clearRow
        persistentTable.clearRow(row); // relies on STATUS_EMPTY=' ');
        it.remove(); // removePVSubscription(pendingSubscriptions...
        removeEmailSubscription(email, row); // after clearRow
        nRemoved++;
      } else {
        nPending++;
      }
    }
    persistentTable.flush();
    if (verbose)
      String2.log("Subscriptions.removeOldPending nPending=" + nPending + " nRemoved=" + nRemoved);
    return nRemoved;
  }

  /**
   * This adds a subscription (pending, not yet validated) and returns the row number. Any existing,
   * identical, pending subscription is refreshed (perhaps user lost invitation email). Any
   * existing, identical, valid subscription is kept. "identical" means datasetID, email, and action
   * are identical; so someone (one email) can have multiple actions for a given dataset.
   *
   * @param datasetID The caller should have checked that the dataset exists. This doesn't check.
   * @param email The email address of the subsriber.
   * @param action This should be either a url (starting with "http://", "https://" or "mailto:").
   *     This does not check that a mailto address matches the email address.
   * @return the row number of the subscription (it may be old pending, new pending, or already
   *     valid)
   * @throws Throwable if trouble (e.g., invalid datasetID, email address, or action). An existing
   *     pending or valid subscription is not trouble.
   */
  public synchronized int add(String datasetID, String email, String action) throws Throwable {

    // check if parameters valid
    if (!String2.isFileNameSafe(datasetID))
      throw new Exception(String2.ERROR + ": \"" + datasetID + "\" is not a valid datasetID.");
    if (datasetID.length() > DATASETID_LENGTH)
      throw new Exception(
          String2.ERROR
              + ": datasetID=\""
              + datasetID
              + "\" has more than "
              + DATASETID_LENGTH
              + " characters.");

    ensureEmailValid(email);

    if (action == null || action.length() == 0) action = "mailto:" + email;
    // be strict now; open to other actions case-by-case
    if (action.startsWith("mailto:") && action.length() > "mailto:".length()) {
    } else if (action.startsWith("http://") && action.length() > "http://".length()) {
    } else if (action.startsWith("https://") && action.length() > "https://".length()) {
    } else {
      throw new Exception(
          String2.ERROR
              + ": action="
              + action
              + " must begin with \"http://\", \"https://\" or \"mailto://\".");
    }
    if (action.length() > ACTION_LENGTH)
      throw new Exception(
          String2.ERROR
              + ": action="
              + action
              + " has more than "
              + ACTION_LENGTH
              + " characters.");

    // try to find identical pending or valid subscription
    String comboKey = comboKey(datasetID, email, action);
    int currentMinute = (int) (System.currentTimeMillis() / 60000); // safe
    Integer rowInteger = pendingSubscriptions.get(comboKey);
    int row = -1;
    if (rowInteger != null) {
      // it's already pending
      row = rowInteger.intValue();
      writeCreationMinute(row, currentMinute); // refresh it
      persistentTable.flush();
    } else {
      rowInteger = validSubscriptions.get(comboKey);
      if (rowInteger == null) {
        // it's new
        // find first empty row
        int nRows = persistentTable.nRows();
        row = 0;
        while (row < nRows && readStatus(row) != STATUS_EMPTY) row++;
        // need to add a row?
        if (row == nRows) persistentTable.addRows(1);

        // store the info
        writeCreationMinute(row, currentMinute);
        writeKey(row, Math2.random(Integer.MAX_VALUE)); // new key
        writeDatasetID(row, datasetID);
        writeEmail(row, email);
        writeAction(row, action);
        persistentTable.flush();
        writeStatus(row, STATUS_PENDING); // do last, after flush, in case of trouble
        persistentTable.flush();
        addPVSubscription(pendingSubscriptions, comboKey, row);
        addEmailSubscription(email, row);
      } else {
        // it's already valid
        row = rowInteger.intValue();
      }
    }
    return row;
  }

  /**
   * This returns an email-able invitation for a pending subscription or email indicating the
   * subsciption is already valid (or throws Throwable if row is invalid or empty).
   *
   * @param ipAddress requestor's ipAddress (use "(unknownIPAddress)" if not known)
   * @param row the row number in persistentTable
   * @return text of email invication to be sent
   * @throws Throwable if trouble (e.g., row is invalid)
   */
  public synchronized String getInvitation(String ipAddress, int row) throws Throwable {

    if (row >= 0 && row < persistentTable.nRows()) {
      byte status = readStatus(row);
      if (status == STATUS_PENDING || status == STATUS_VALID) {

        StringBuilder sb =
            new StringBuilder(
                // 2014-09-24 I changed this to not show ipAddress for security reasons
                // Is a hacker somehow using this to report back ipAddress of just hacked computers?
                "ERDDAP received your request "
                    + // a -> your
                    // from " + ipAddress + " referencing your email address
                    "to subscribe to"
                    + "\ndatasetID="
                    + readDatasetID(row)
                    + "\nwith action="
                    + readAction(row)
                    + "\n");

        if (status == STATUS_VALID) sb.append("\nThat subscription is alread valid!\n");
        else
          sb.append(
              "\nThat subscription isn't valid yet."
                  + "\nIf the subscription isn't validated soon, it will be deleted."
                  + "\nSo if you don't want the subscription, you don't have to do anything."
                  + "\n"
                  + "\nTo validate the subscription, visit"
                  + "\n"
                  + preferredErddapUrl
                  + "/"
                  + VALIDATE_HTML
                  + "?subscriptionID="
                  + row
                  + "&key="
                  + readKey(row)
                  + "\n");

        sb.append(
            "\n\n*****"
                + "\nNow or in the future, you can delete that subscription (unsubscribe) with"
                + "\n"
                + preferredErddapUrl
                + "/"
                + REMOVE_HTML
                + "?subscriptionID="
                + row
                + "&key="
                + readKey(row)
                + "\n"
                + "\n");
        sb.append(messageToRequestList(readEmail(row)));
        return sb.toString();
      }
    }

    throw new Exception("There is no such subscription.");
  }

  /**
   * This tries to validate a subscription which has been added. Validating a second time is ok.
   *
   * @param row
   * @param key
   * @return error message if subscription not found or key is wrong (which doesn't invalidate
   *     already valid subscription) or "" if subscription was already or is now valid.
   */
  public synchronized String validate(int row, int key) throws Throwable {

    int nRows = persistentTable.nRows();
    if (row < 0 || row >= nRows) return String2.ERROR + ": There is no subscriptionID=" + row + ".";

    byte status = readStatus(row);
    if (status == STATUS_EMPTY)
      return String2.ERROR + ": The subscription request (if any) has expired.";
    if (status == STATUS_VALID) // ignore key
    return "";

    int storedKey = readKey(row);
    if (storedKey != key)
      return String2.ERROR + ": For subscriptionID=" + row + ", " + key + " is not the right key.";
    writeStatus(row, STATUS_VALID);
    persistentTable.flush();
    String comboKey = readComboKey(row);
    removePVSubscription(pendingSubscriptions, comboKey);
    addPVSubscription(validSubscriptions, comboKey, row);
    addDatasetSubscription(readDatasetID(row), row);
    // emailSubscriptions doesn't change
    return "";
  }

  /**
   * This adds and validates a subscription (this method is used for pre-authorized subscriptions,
   * e.g., in datasets.xml). It is okay if an identical already-valid subscription already exists.
   *
   * @param datasetID
   * @param email
   * @param action
   * @return the row number of the newly (or already) valid subscription
   * @throws Throwable if invalid datasetID, email, or action (see add()).
   */
  public synchronized int addAndValidate(String datasetID, String email, String action)
      throws Throwable {

    int row = add(datasetID, email, action);
    String error = validate(row, readKey(row));
    if (error.length() != 0) throw new Exception(error);
    return row;
  }

  /**
   * This removes a pending or valid subscription (this method is used internally). It is ok of the
   * subscription isn't found.
   *
   * @param datasetID
   * @param email
   * @param action
   * @return true if it existed
   * @throws Throwable if error reading file. But if datasetID+email+action not found, it isn't an
   *     exception.
   */
  public synchronized boolean remove(String datasetID, String email, String action)
      throws Throwable {

    String comboKey = comboKey(datasetID, email, action);
    Integer rowInteger = validSubscriptions.get(comboKey);
    if (rowInteger == null) rowInteger = pendingSubscriptions.get(comboKey);
    if (rowInteger == null) return false;
    int row = rowInteger.intValue();
    remove(row, readKey(row));
    return true;
  }

  /**
   * This removes a pending or valid subscription. It is ok of the subscription isn't found.
   *
   * @param row
   * @param key
   * @return error message if trouble (row is invalid, or subscription was found (validated or
   *     pending), but key is wrong) or "" if no error.
   */
  public synchronized String remove(int row, int key) throws Throwable {

    int nRows = persistentTable.nRows();
    if (row < 0 || row >= nRows) return String2.ERROR + ": There is no subscriptionID=" + row + ".";

    byte status = readStatus(row);
    if (status == STATUS_EMPTY) return "";

    int storedKey = readKey(row);
    if (storedKey != key)
      return String2.ERROR + ": For subscriptionID=" + row + ", " + key + " is not the right key.";
    String datasetID = readDatasetID(row); // read things before clearRow
    String email = readEmail(row);
    String action = readAction(row);
    String comboKey = comboKey(datasetID, email, action);
    persistentTable.clearRow(row); // relies on STATUS_EMPTY=' ');
    persistentTable.flush();
    if (status == STATUS_PENDING) removePVSubscription(pendingSubscriptions, comboKey);
    if (status == STATUS_VALID) {
      removePVSubscription(validSubscriptions, comboKey);
      removeDatasetSubscription(datasetID, row);
    }
    removeEmailSubscription(email, row);
    return "";
  }

  /**
   * This lists the actions from validated subscriptions for a given dataset.
   *
   * @param datasetID
   * @return a StringArray of actions (urls or email:(email address)) (or String[0] if none) The
   *     order may vary.
   * @throws Throwable if trouble
   */
  public synchronized StringArray listActions(String datasetID) throws Throwable {
    // get sorted list (idea: it doesn't take long to sort ints in memory
    //  and then access in subscriptions file is in row order (faster?))
    StringArray sa = new StringArray();
    IntArray rows = getSortedDatasetSubscriptions(datasetID);
    if (rows == null) return sa;
    for (int i = 0; i < rows.size(); i++) sa.add(readAction(rows.get(i)));
    if (reallyVerbose) String2.log("Subscriptions.listActions(" + datasetID + ")=" + sa);
    return sa;
  }

  /**
   * This lists the validated and not validated subscriptions for a given email.
   *
   * @param ipAddress requestor's ipAddress (use "(unknownIPAddress)" if not known)
   * @param email
   * @return a string suitable for an email
   * @throws Throwable if trouble (e.g., email is on the blacklist)
   */
  public synchronized String listSubscriptions(String ipAddress, String email) throws Throwable {
    StringBuilder sb =
        new StringBuilder(
            // 2014-09-24 I changed this to not show ipAddress for security reasons
            // Is a hacker somehow using this to report back ipAddress of just hacked computers?
            "ERDDAP received your request"
                + // a -> your
                // from " + ipAddress +
                " for a list of your valid and pending subscriptions.\n\n");

    ensureEmailValid(email);

    // get the row numbers
    IntArray rows = getSortedEmailSubscriptions(email);
    if (rows == null || rows.size() == 0) {
      sb.append("Currently, you have no valid or pending subscriptions.");
      return sb.toString();
    }

    // format the results   (in a format which should be easy for a computer program to parse)
    sb.append("Your valid and pending subscriptions are:");
    for (int which = 0; which < rows.size(); which++) {
      int row = rows.get(which);
      byte status = readStatus(row);
      sb.append(
          "\n"
              + "\ndatasetID:      "
              + readDatasetID(row)
              + "\naction:         "
              + readAction(row)
              + "\nstatus:         "
              + (status == STATUS_VALID ? "valid" : "pending"));
      if (status == STATUS_VALID)
        sb.append(
            "\nto unsubscribe: "
                + preferredErddapUrl
                + "/"
                + REMOVE_HTML
                + "?subscriptionID="
                + row
                + "&key="
                + readKey(row));
      else
        sb.append(
            "\nto validate:    "
                + preferredErddapUrl
                + "/"
                + VALIDATE_HTML
                + "?subscriptionID="
                + row
                + "&key="
                + readKey(row));
    }
    sb.append(
        "\n\nNote that pending subscriptions that aren't validated soon will be deleted.\n"
            + "\n\n*****\n"
            + messageToRequestList(email));
    if (reallyVerbose)
      String2.log("Subscriptions.listSubscriptions(" + email + ")=\n" + sb.toString());
    return sb.toString();
  }

  /**
   * This lists all of the validated and not validated subscriptions, sorted (case insensitive) by
   * email.
   *
   * @return a string suitable for Daily Report
   * @throws Throwable if trouble
   */
  public synchronized String listSubscriptions() throws Throwable {

    String emails[] = String2.toStringArray(emailSubscriptions.keySet().toArray());
    Arrays.sort(emails, String2.STRING_COMPARATOR_IGNORE_CASE);
    StringBuilder sb =
        new StringBuilder(
            "List of Valid and Pending Subscriptions:\n"
                + "(nEmailAddress="
                + emails.length
                + ", nPendingSubscriptions="
                + pendingSubscriptions.size()
                + ", nValidSubscriptions="
                + validSubscriptions.size()
                + ")\n\n");
    for (int i = 0; i < emails.length; i++) {

      // get the row numbers
      IntArray rows = getSortedEmailSubscriptions(emails[i]);
      if (rows == null || rows.size() == 0) {
        emailSubscriptions.remove(emails[i]); // shouldn't happen.  fix it by removing the key
        continue;
      }

      // format the results
      sb.append(emails[i] + "\n");
      for (int which = 0; which < rows.size(); which++) {
        int row = rows.get(which);
        byte status = readStatus(row);
        sb.append(
            String2.left(readDatasetID(row), 20)
                + (status == STATUS_VALID ? " valid   " : " pending ")
                + String2.left(readAction(row), 35)
                + " "
                + preferredErddapUrl
                + "/"
                + REMOVE_HTML
                + "?subscriptionID="
                + row
                + "&key="
                + readKey(row)
                + "\n");
      }
      sb.append('\n');
    }
    if (reallyVerbose) String2.log("Subscriptions.listSubscriptions()=\n" + sb.toString());
    return sb.toString();
  }

  /** for diagnostics */
  public synchronized String toString(int row) throws IOException {
    return (char) readStatus(row)
        + ", "
        + readCreationMinute(row)
        + ", "
        +
        // readKey(row) + ", " +
        readDatasetID(row)
        + ", "
        + readEmail(row)
        + ", "
        + readAction(row);
  }
}
