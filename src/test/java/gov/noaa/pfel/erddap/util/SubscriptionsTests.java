package gov.noaa.pfel.erddap.util;

import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import org.junit.jupiter.api.BeforeAll;
import testDataset.Initialization;

class SubscriptionsTests {

  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests the methods in this class. This can be run when the local ERDDAP is running -- it
   * uses a different/temporary subscriptions file.
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Throwable {
    // String2.log("\n*** Subscriptions.basicTest");
    // verbose = true;
    // reallyVerbose = true;
    String results, expected;
    int key = -1;
    boolean ok = true;
    String sampleDatasetID = "pmelTao";
    String sampleEmail = "john.smith@company.com";

    // ensure hashset will work correctly
    // hashset.add is based on equals test,
    // see
    // https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Collection.html
    Test.ensureTrue(Integer.valueOf(17).equals(Integer.valueOf(17)), "");

    // test empty system
    String ffName = EDStatic.fullTestCacheDirectory + "subscriptionsV1.txt";
    File2.delete(ffName);
    Subscriptions sub = new Subscriptions(ffName, 72, EDStatic.erddapHttpsUrl);
    Test.ensureEqual(sub.persistentTable.nRows(), 0, "");
    Test.ensureEqual(sub.datasetSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.emailSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.validSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.pendingSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.validate(0, 12345), String2.ERROR + ": There is no subscriptionID=0.", "");
    Test.ensureEqual(sub.listActions(sampleDatasetID).toString(), "", "");
    results = sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
    Test.ensureEqual(
        results,
        "ERDDAP received your request for a list of your valid and pending subscriptions.\n"
            + "\n"
            + "Currently, you have no valid or pending subscriptions.",
        "results=\n" + results);
    Test.ensureEqual(sub.remove(0, 12345), String2.ERROR + ": There is no subscriptionID=0.", "");
    results = sub.listSubscriptions();
    Test.ensureEqual(
        results,
        "List of Valid and Pending Subscriptions:\n"
            + "(nEmailAddress=0, nPendingSubscriptions=0, nValidSubscriptions=0)\n"
            + "\n",
        "results=\n" + results);

    // add invalid
    results = "";
    try {
      sub.add(sampleDatasetID, sampleEmail, "nonsense");
    } catch (Throwable t) {
      results = t.getMessage();
    }
    Test.ensureEqual(
        results,
        String2.ERROR
            + ": action=nonsense must begin with \"http://\", \"https://\" or \"mailto://\".",
        "results=\n" + results);

    // add a subscription (twice -- no change)
    for (int i = 0; i < 2; i++) {
      int row = sub.add(sampleDatasetID, sampleEmail, null);
      Test.ensureEqual(row, 0, "");
      results = sub.getInvitation("(unknownIPAddress)", row);
      if (i == 0) key = sub.readKey(row);
      else Test.ensureEqual(key, sub.readKey(row), "");
      expected =
          "ERDDAP received your request to subscribe to\n"
              + "datasetID=pmelTao\n"
              + "with action=mailto:"
              + sampleEmail
              + "\n"
              + "\n"
              + "That subscription isn't valid yet.\n"
              + "If the subscription isn't validated soon, it will be deleted.\n"
              + "So if you don't want the subscription, you don't have to do anything.\n"
              + "\n"
              + "To validate the subscription, visit\n"
              + "/erddap/subscriptions/validate.html?subscriptionID=0&key="
              + key
              + "\n"
              + "\n"
              + "\n"
              + "*****\n"
              + "Now or in the future, you can delete that subscription (unsubscribe) with\n"
              + "/erddap/subscriptions/remove.html?subscriptionID=0&key="
              + key
              + "\n"
              + "\n"
              + "You can request an email with a list of all of your valid and pending subscriptions with this URL:\n"
              + "/erddap/subscriptions/list.html?email=john.smith@company.com\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      Test.ensureEqual(sub.persistentTable.nRows(), 1, "");
      Test.ensureEqual(sub.datasetSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.emailSubscriptions.size(), 1, "");
      Test.ensureEqual(sub.pendingSubscriptions.size(), 1, "");
      Test.ensureEqual(sub.validSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.listActions(sampleDatasetID).toString(), "", "");
      results = sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
      Test.ensureEqual(
          results,
          "ERDDAP received your request for a list of your valid and pending subscriptions.\n"
              + "\n"
              + "Your valid and pending subscriptions are:\n"
              + "\n"
              + "datasetID:      pmelTao\n"
              + "action:         mailto:john.smith@company.com\n"
              + "status:         pending\n"
              + "to validate:    /erddap/subscriptions/validate.html?subscriptionID=0&key="
              + key
              + "\n"
              + "\n"
              + "Note that pending subscriptions that aren't validated soon will be deleted.\n"
              + "\n"
              + "\n"
              + "*****\n"
              + "You can request an email with a list of all of your valid and pending subscriptions with this URL:\n"
              + "/erddap/subscriptions/list.html?email=john.smith@company.com\n",
          "results=\n" + results);
    }

    // validate (twice -- 2nd time no change)
    for (int i = 0; i < 2; i++) {
      Test.ensureEqual(
          sub.remove(0, key + 1),
          String2.ERROR + ": For subscriptionID=0, " + (key + 1) + " is not the right key.",
          "");
      Test.ensureEqual(sub.validate(0, key), "", "");
      Test.ensureEqual(sub.persistentTable.nRows(), 1, "");
      Test.ensureEqual(sub.datasetSubscriptions.size(), 1, "");
      Test.ensureEqual(sub.emailSubscriptions.size(), 1, "");
      Test.ensureEqual(sub.pendingSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.validSubscriptions.size(), 1, "");
      Test.ensureEqual(sub.listActions(sampleDatasetID).toString(), "mailto:" + sampleEmail, "");
      results = sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
      Test.ensureEqual(
          results,
          "ERDDAP received your request for a list of your valid and pending subscriptions.\n"
              + "\n"
              + "Your valid and pending subscriptions are:\n"
              + "\n"
              + "datasetID:      pmelTao\n"
              + "action:         mailto:john.smith@company.com\n"
              + "status:         valid\n"
              + "to unsubscribe: /erddap/subscriptions/remove.html?subscriptionID=0&key="
              + key
              + "\n"
              + "\n"
              + "Note that pending subscriptions that aren't validated soon will be deleted.\n"
              + "\n"
              + "\n"
              + "*****\n"
              + "You can request an email with a list of all of your valid and pending subscriptions with this URL:\n"
              + "/erddap/subscriptions/list.html?email=john.smith@company.com\n",
          "results=\n" + results);
    }

    // remove (twice -- 2nd time no change)
    for (int i = 0; i < 2; i++) {
      Test.ensureEqual(
          sub.remove(0, key + 1),
          (i == 0
              ? String2.ERROR + ": For subscriptionID=0, " + (key + 1) + " is not the right key."
              : ""), // no
          // error
          // 2nd
          // time
          "");
      Test.ensureEqual(sub.remove(0, key), "", "");
      Test.ensureEqual(sub.persistentTable.nRows(), 1, "");
      Test.ensureEqual(sub.datasetSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.emailSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.pendingSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.validSubscriptions.size(), 0, "");
      Test.ensureEqual(sub.listActions(sampleDatasetID).toString(), "", "");
      results = sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
      Test.ensureEqual(
          results,
          "ERDDAP received your request for a list of your valid and pending subscriptions.\n"
              + "\n"
              + "Currently, you have no valid or pending subscriptions.",
          "results=\n" + results);
    }

    // **** add several
    sub.add(sampleDatasetID, sampleEmail, null);
    sub.add(sampleDatasetID, sampleEmail, "http://www.google.com");
    sub.add("rPmelTao", sampleEmail, null);
    sub.add("rPmelTao", sampleEmail, "http://www.yahoo.com");
    sub.add("rPmelTao", "jane.smith@company.com", "http://www.yahoo.com");
    Test.ensureEqual(sub.persistentTable.nRows(), 5, "");
    Test.ensureEqual(sub.datasetSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.emailSubscriptions.size(), 2, "");
    Test.ensureEqual(sub.pendingSubscriptions.size(), 5, "");
    Test.ensureEqual(sub.validSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.listActions(sampleDatasetID).toString(), "", "");

    int key0 = sub.readKey(0);
    int key1 = sub.readKey(1);
    int key2 = sub.readKey(2);
    int key3 = sub.readKey(3);
    int key4 = sub.readKey(4);
    String2.log(
        "key0=" + key0 + " key1=" + key1 + " key2=" + key2 + " key3=" + key3 + " key4=" + key4);
    results = sub.listSubscriptions();
    Test.ensureEqual(
        results,
        "List of Valid and Pending Subscriptions:\n"
            + "(nEmailAddress=2, nPendingSubscriptions=5, nValidSubscriptions=0)\n"
            + "\n"
            + "jane.smith@company.com\n"
            + "rPmelTao             pending http://www.yahoo.com                /erddap/subscriptions/remove.html?subscriptionID=4&key="
            + key4
            + "\n"
            + "\n"
            + "john.smith@company.com\n"
            + "pmelTao              pending mailto:john.smith@company.com       /erddap/subscriptions/remove.html?subscriptionID=0&key="
            + key0
            + "\n"
            + "pmelTao              pending http://www.google.com               /erddap/subscriptions/remove.html?subscriptionID=1&key="
            + key1
            + "\n"
            + "rPmelTao             pending mailto:john.smith@company.com       /erddap/subscriptions/remove.html?subscriptionID=2&key="
            + key2
            + "\n"
            + "rPmelTao             pending http://www.yahoo.com                /erddap/subscriptions/remove.html?subscriptionID=3&key="
            + key3
            + "\n"
            + "\n",
        "results=\n" + results);

    // validate
    Test.ensureEqual(sub.validate(0, key0), "", "");
    Test.ensureEqual(sub.validate(1, key1), "", "");
    Test.ensureEqual(sub.validate(2, key2), "", "");
    Test.ensureEqual(sub.validate(3, key3), "", "");
    Test.ensureEqual(sub.validate(4, key4), "", "");
    Test.ensureEqual(sub.persistentTable.nRows(), 5, "");
    Test.ensureEqual(sub.datasetSubscriptions.size(), 2, "");
    Test.ensureEqual(sub.emailSubscriptions.size(), 2, "");
    Test.ensureEqual(sub.pendingSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.validSubscriptions.size(), 5, "");
    StringArray actions = sub.listActions(sampleDatasetID); // order varies
    Test.ensureTrue(actions.indexOf("http://www.google.com") >= 0, "actions=" + actions);
    Test.ensureTrue(actions.indexOf("mailto:john.smith@company.com") >= 0, "actions=" + actions);
    results = sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
    Test.ensureEqual(
        results,
        "ERDDAP received your request for a list of your valid and pending subscriptions.\n"
            + "\n"
            + "Your valid and pending subscriptions are:\n"
            + "\n"
            + "datasetID:      pmelTao\n"
            + "action:         mailto:john.smith@company.com\n"
            + "status:         valid\n"
            + "to unsubscribe: /erddap/subscriptions/remove.html?subscriptionID=0&key="
            + key0
            + "\n"
            + "\n"
            + "datasetID:      pmelTao\n"
            + "action:         http://www.google.com\n"
            + "status:         valid\n"
            + "to unsubscribe: /erddap/subscriptions/remove.html?subscriptionID=1&key="
            + key1
            + "\n"
            + "\n"
            + "datasetID:      rPmelTao\n"
            + "action:         mailto:john.smith@company.com\n"
            + "status:         valid\n"
            + "to unsubscribe: /erddap/subscriptions/remove.html?subscriptionID=2&key="
            + key2
            + "\n"
            + "\n"
            + "datasetID:      rPmelTao\n"
            + "action:         http://www.yahoo.com\n"
            + "status:         valid\n"
            + "to unsubscribe: /erddap/subscriptions/remove.html?subscriptionID=3&key="
            + key3
            + "\n"
            + "\n"
            + "Note that pending subscriptions that aren't validated soon will be deleted.\n"
            + "\n"
            + "\n"
            + "*****\n"
            + "You can request an email with a list of all of your valid and pending subscriptions with this URL:\n"
            + "/erddap/subscriptions/list.html?email=john.smith@company.com\n",
        "results=\n" + results);

    // remove
    Test.ensureEqual(sub.remove(0, key0), "", "");
    Test.ensureEqual(sub.remove(1, key1), "", "");
    Test.ensureEqual(sub.remove(2, key2), "", "");
    Test.ensureEqual(sub.remove(3, key3), "", "");
    Test.ensureEqual(sub.remove(4, key4), "", "");
    Test.ensureEqual(sub.persistentTable.nRows(), 5, "");
    Test.ensureEqual(sub.datasetSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.emailSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.pendingSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.validSubscriptions.size(), 0, "");
    Test.ensureEqual(sub.listActions(sampleDatasetID).toString(), "", "");
    results = sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
    Test.ensureEqual(
        results,
        "ERDDAP received your request for a list of your valid and pending subscriptions.\n"
            + "\n"
            + "Currently, you have no valid or pending subscriptions.",
        "results=\n" + results);

    // emailBlacklist
    sub.add(sampleDatasetID, sampleEmail, "http://www.google.com");
    Test.ensureEqual(sub.setEmailBlacklist(""), 0, "");
    Test.ensureEqual(sub.setEmailBlacklist("a@b.com, *@c.com, " + sampleEmail), 1, "");
    results = "";
    try {
      sub.add(sampleDatasetID, sampleEmail, "http://www.google.com");
    } catch (Throwable t) {
      results = t.getMessage();
    }
    Test.ensureEqual(
        results,
        String2.ERROR + ": \"john.smith@company.com\" is on the email blacklist.",
        "results=\n" + results);

    results = "";
    try {
      sub.add(sampleDatasetID, "c@c.com", "http://www.google.com");
    } catch (Throwable t) {
      results = t.getMessage();
    }
    Test.ensureEqual(
        results,
        String2.ERROR + ": \"c@c.com\" is on the email blacklist.",
        "results=\n" + results);

    results = "";
    try {
      sub.listSubscriptions("(unknownIPAddress)", sampleEmail);
    } catch (Throwable t) {
      results = t.getMessage();
    }
    Test.ensureEqual(
        results,
        String2.ERROR + ": \"john.smith@company.com\" is on the email blacklist.",
        "results=\n" + results);

    Test.ensureEqual(sub.setEmailBlacklist(""), 0, "");

    sub.close();
  }
}
