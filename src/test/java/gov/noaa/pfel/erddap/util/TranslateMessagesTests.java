package gov.noaa.pfel.erddap.util;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashSet;

import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import tags.TagLocalERDDAP;
import tags.TagPassword;

class TranslateMessagesTests {

  /**
   * This checks if a short dontTranslate (DAP) exists before a long
   * dontTranslate.
   * This is REQUIRED for my use of START_SPAN/STOP_SPAN to not translate some
   * text;
   * otherwise, there can be e.g., 2 START_SPAN in a row, and that messes up
   * undoing the system.
   */
  @org.junit.jupiter.api.Test
  void testDontTranslateOrdering() throws Exception {
    String2.log("\n*** TranslateMessages.testDontTranslateOrdering()");
    StringBuilder errors = new StringBuilder();
    for (int i1 = 0; i1 < TranslateMessages.nDontTranslate; i1++) {
      String s1 = TranslateMessages.dontTranslate[i1];
      for (int i2 = i1 + 1; i2 < TranslateMessages.nDontTranslate; i2++) {
        if (TranslateMessages.dontTranslate[i2].indexOf(s1) >= 0)
          errors.append("dontTranslate[" + i1 + "]=" + s1 + " must not be before [" + i2 + "]="
              + TranslateMessages.dontTranslate[i2] + "\n");
      }
    }
    if (errors.length() > 0)
      throw new RuntimeException("Errors from testDontTranslateOrdering:\n" +
          errors.toString());
  }

  /**
   * This tests that the connections to Google are correct with simple test code
   * from Google.
   */
  @org.junit.jupiter.api.Test
  @TagPassword
  void testGoogleSampleCode() throws Exception {
    String2.log("\n*** TranslateMessages.testGoogleSampleCode()");
    Test.ensureEqual(
        TranslateMessages.googleSampleCode(TranslateMessages.googleProjectId, "de",
            "The variable name \"sst\" converts to the full name \"Sea Surface Temperature\"."),
        // 2022-01-28 was \"sst\"
        // 2022-01-28 was \"Sea Surface Temperature\"
        "Der Variablenname \u201esst\u201c wird in den vollständigen Namen \u201eMeeresoberflächentemperatur\u201c umgewandelt.",
        "");
  }

  /**
   * Use this method to look for closing tags with HTML content by not using CDATA
   * which causes problems (so switch them to use CDATA).
   *
   * @throws Exception when something goes wrong
   */
  @org.junit.jupiter.api.Test
  void findTagsMissingCDATA() throws Exception {
    String2.log("\n*** TranslateMessages.findTagsMissingCDATA()");
    SimpleXMLReader xmlReader = new SimpleXMLReader(new FileInputStream(TranslateMessages.messagesXmlFileName));
    int nTagsProcessed = 0;
    int nBad = 0;
    while (true) {
      xmlReader.nextTag();
      nTagsProcessed++;
      String rawContent = xmlReader.rawContent().trim(); // rawContent isn't already trimmed!
      // skip comments
      if (rawContent.startsWith("<!--") && rawContent.endsWith("-->"))
        continue;
      if (Arrays.stream(TranslateMessages.HTMLEntities).anyMatch(rawContent::contains) &&
          !TranslateMessages.isHtml(rawContent)) { // html content must be within cdata
        String2.log(nBad + " HTMLEntities >>" + rawContent + "<<");
        nBad++;
      }
      if (xmlReader.stackSize() == 3) { // tag not within cdata is trouble, e.g., <erddapMessages><someTag><kbd>
        String2.log(nBad + " stackSize >> " + xmlReader.allTags());
        nBad++;
      }
      if (xmlReader.stackSize() == 0) // done
        break;
    }
    xmlReader.close();
    String2.log("\nfindTagsMissingCDATA finished. nTagsProcessed=" + nTagsProcessed);
    if (nBad > 0)
      throw new RuntimeException("ERROR: findTagsMissingCDATA found " + nBad + " bad tags (above). FIX THEM!");
  }

  /**
   * This test the translation of a difficult HTML test case.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagPassword
  void testTranslateHtml() throws Exception {
    String2.log("\n*** TranslateMessages.testTranslateHtml()");
    String raw = // a devious string with numerous of test cases
        "<![CDATA[<br>&bull; &lt;subsetVariables&gt; wasn't specified. Is x&gt;90?\nTest &mdash; test2? Test3 &ndash; test4?\n"
            + // 90?\n tests ?[whitespace]
            "<br>Does test5 &rarr; test6? \"&micro;\" means micro. Temperature is &plusmn;5 degrees.\n" +
            "<br>This is a question? This is an answer.\n" + // even here, Google removes the space after
                                                             // the '?'!
            "<br>E = &sum;(w Y)/&sum;(w). This is a &middot; (middot).\n" + // E =... is a dontTranslate
                                                                            // string
            "<h1>ERDDAP</h1> [standardShortDescriptionHtml] Add \"&amp;units=...\" to the query. \n" +
            "<br>Add \"<kbd>&amp;time&gt;now-7days</kbd>\" to the query.\n" + // This is a dontTranslate
                                                                              // string
            "<br><kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>\n" + // test
                                                                                                       // &amp;
                                                                                                       // in
                                                                                                       // dontTranslate
                                                                                                       // phrase
            "<br>Convert &amp; into %26.\n" + // &amp; by itself
            "<br>This is version=&wmsVersion; and <kbd>&adminEmail;</kbd>.\n" + // both are in dontTranslate
            "See <a rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">Data Provider Form</a>.\n"
            + // &entity; in <a href="">
            "See <img rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">.\n]]"; // &entity; in
                                                                                            // <a href="">
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    boolean html = TranslateMessages.isHtml(raw);
    boolean messageFormat = TranslateMessages.isMessageFormat(raw);
    StringBuilder results = new StringBuilder("RESULTS: html=" + html + " messageFormat=" + messageFormat + "\n" +
        "en=" + raw);
    String tLang[] = { "en", "de" }; // {"en", "de"}; {"en", "zh-cn", "de", "hi", "ja", "ru", "th"}; or
                                     // languageCodeList
    for (int ti = 1; ti < tLang.length; ti++) { // skip 0="en"
      String2.log("lan[" + ti + "]=" + tLang[ti]);
      results.append(
          "\n\n" + tLang[ti] + "=" + TranslateMessages.lowTranslateTag(raw, tLang[ti], html, messageFormat));
    }
    String2.setClipboardString(results.toString());
    String2.log("The results are on the clipboard.");
    Test.ensureEqual(results.toString(),
        "RESULTS: html=true messageFormat=false\n" +
            "en=<![CDATA[<br>&bull; &lt;subsetVariables&gt; wasn't specified. Is x&gt;90?\n" +
            "Test &mdash; test2? Test3 &ndash; test4?\n" +
            "<br>Does test5 &rarr; test6? \"&micro;\" means micro. Temperature is &plusmn;5 degrees.\n" +
            "<br>This is a question? This is an answer.\n" +
            "<br>E = &sum;(w Y)/&sum;(w). This is a &middot; (middot).\n" +
            "<h1>ERDDAP</h1> [standardShortDescriptionHtml] Add \"&amp;units=...\" to the query. \n" +
            "<br>Add \"<kbd>&amp;time&gt;now-7days</kbd>\" to the query.\n" +
            "<br><kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>\n" +
            "<br>Convert &amp; into %26.\n" +
            "<br>This is version=&wmsVersion; and <kbd>&adminEmail;</kbd>.\n" +
            "See <a rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">Data Provider Form</a>.\n"
            +
            "See <img rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">.\n" +
            "]]\n" +
            "\n" +
            "de=<![CDATA[\n" +
            "<br>• &lt;subsetVariables&gt; wurde nicht angegeben.\n" +
            "Ist x &gt; 90?\n" +
            "Test — test2?\n" +
            "Test3 – Test4?\n" +
            "<br>Bedeutet test5 → test6?\n" +
            "&quot;µ&quot; bedeutet Mikro.\n" +
            "Die Temperatur beträgt ±5 Grad.\n" +
            "<br>Das ist eine Frage?\n" +
            "Dies ist eine Antwort.\n" +
            "<br>E = ∑(w Y)/∑(w) .\n" +
            "Dies ist ein · (Mittelpunkt).\n" +
            "<h1>ERDDAP</h1>\n" +
            "[standardShortDescriptionHtml] Fügen Sie der Abfrage &quot;&amp;units=...&quot; hinzu.\n" +
            "<br>Fügen Sie der Abfrage &quot;<kbd>&amp;time&gt;now-7days</kbd>&quot; hinzu.\n" +
            "<br><kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>\n" +
            "<br>Wandle &amp; in %26 um.\n" +
            "<br>Dies ist version= &wmsVersion; und <kbd>&adminEmail;</kbd> .\n" +
            "Siehe\n" +
            "<a rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">Datenanbieterformular</a> .\n"
            +
            "Sehen<img rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">.\n" +
            "]]", // !Bad: missing space after Sehen
        // chinese zh-cn translation often varies a little, so don't test it
        results.toString());
    // The results seem to change a little sometimes!
    // debugMode = oDebugMode;
  }

  /**
   * This test the translation of a difficult HTML test case.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagPassword
  void testTranslateComment() throws Exception {
    String2.log("\n*** TranslateMessages.testTranslateComment()");
    String raw = "<!-- This tests a devious comment with <someTag> and <b> and {0}\nand a newline. -->";
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    boolean html = TranslateMessages.isHtml(raw);
    boolean messageFormat = TranslateMessages.isMessageFormat(raw);
    StringBuilder results = new StringBuilder("RESULTS: html=" + html + " messageFormat=" + messageFormat + "\n" +
        "en=" + raw);
    String tLang[] = { "en", "de" }; // , "zh-cn"}; //Chinese results vary and are hard to test
    for (int ti = 1; ti < tLang.length; ti++) { // skip 0="en"
      String2.log("lan[" + ti + "]=" + tLang[ti]);
      results.append(
          "\n\n" + tLang[ti] + "=" + TranslateMessages.lowTranslateTag(raw, tLang[ti], html, messageFormat));
    }
    String2.setClipboardString(results.toString());
    String2.log("The results are on the clipboard.");
    Test.ensureEqual(results.toString(),
        "RESULTS: html=false messageFormat=true\n" +
            "en=<!-- This tests a devious comment with <someTag> and <b> and {0}\n" +
            "and a newline. -->\n" +
            "\n" +
            "de=&lt;!-- Dies testet einen hinterhältigen Kommentar mit &lt;someTag&gt; und &lt;b&gt; und {0}\n"
            +
            "und ein Zeilenumbruch. --&gt;",
        results.toString());
    // debugMode = oDebugMode;
  }

  /**
   * This test the translation of difficult plain text which uses MessageFormat.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagPassword
  void testTranslatePlainText() throws Exception {
    String2.log("\n*** TranslateMessages.testTranslatePlainText()");
    String raw = // a devious string with numerous of test cases
        "To download tabular data from ERDDAP''s RESTful services via &tErddapUrl;,\n" +
            "make sure latitude >30 and <50, or \"BLANK\". Add & before every constraint.\n" +
            "For File type, choose one of\n" +
            "the non-image {0} (anything but .kml, .pdf, or .png).\n";
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    boolean html = TranslateMessages.isHtml(raw);
    boolean messageFormat = TranslateMessages.isMessageFormat(raw);
    StringBuilder results = new StringBuilder("RESULTS: html=" + html + " messageFormat=" + messageFormat + "\n" +
        "en=" + raw);
    String tLang[] = { "en", "de" }; // , "zh-cn"}; //chinese results vary and are hard to test
    for (int ti = 1; ti < tLang.length; ti++) { // skip 0="en"
      String2.log("lan[" + ti + "]=" + tLang[ti]);
      results.append(
          "\n\n" + tLang[ti] + "=" + TranslateMessages.lowTranslateTag(raw, tLang[ti], html, messageFormat));
    }
    String2.setClipboardString(results.toString());
    String2.log("The results (encoded for storage in XML) are on the clipboard.");
    Test.ensureEqual(results.toString(),
        "RESULTS: html=false messageFormat=true\n" +
            "en=To download tabular data from ERDDAP''s RESTful services via &tErddapUrl;,\n" +
            "make sure latitude >30 and <50, or \"BLANK\". Add & before every constraint.\n" +
            "For File type, choose one of\n" +
            "the non-image {0} (anything but .kml, .pdf, or .png).\n" +
            "\n" +
            "\n" +
            "de=Um tabellarische Daten von den RESTful-Diensten von ERDDAP über &tErddapUrl; herunterzuladen,\n"
            +
            "Stellen Sie sicher, dass der Breitengrad &gt;30 und &lt;50 oder &quot;BLANK&quot; ist. Fügen Sie &amp; vor jeder Einschränkung hinzu.\n"
            +
            "Wählen Sie für Dateityp einen der folgenden aus\n" +
            "das Nicht-Bild {0} (alles außer .kml, .pdf oder .png).\n",

        results.toString());
    // debugMode = oDebugMode;
  }

  /**
   * This checks lots of webpages on localhost ERDDAP for uncaught special text
   * (&amp;term; or ZtermZ).
   * This REQUIRES localhost ERDDAP be running with at least
   * <datasetsRegex>(etopo.*|jplMURSST41|cwwcNDBCMet)</datasetsRegex>.
   */
  @org.junit.jupiter.api.Test
  @TagLocalERDDAP
  void checkForUncaughtSpecialText() throws Exception {
    String2.log("\n*** TranslateMessages.checkForUncaughtSpecialText()\n" +
        "THIS REQUIRES localhost ERDDAP with at least (etopo.*|jplMURSST41|cwwcNDBCMet)");
    String tErddapUrl = "http://localhost:8080/cwexperimental/de/";
    String pages[] = {
        "index.html",
        "categorize/cdm_data_type/grid/index.html?page=1&itemsPerPage=1000",
        "convert/index.html",
        "convert/oceanicAtmosphericAcronyms.html",
        "convert/fipscounty.html",
        "convert/keywords.html",
        "convert/time.html",
        "convert/units.html",
        "convert/urls.html",
        "convert/oceanicAtmosphericVariableNames.html",
        "dataProviderForm.html",
        "dataProviderForm1.html",
        "dataProviderForm2.html",
        "dataProviderForm3.html",
        "dataProviderForm4.html",
        // "download/AccessToPrivateDatasets.html",
        // "download/changes.html",
        // "download/EDDTableFromEML.html",
        // "download/grids.html",
        // "download/NCCSV.html",
        // "download/NCCSV_1.00.html",
        // "download/setup.html",
        // "download/setupDatasetsXml.html",
        "files/",
        "files/cwwcNDBCMet/",
        "files/documentation.html",
        "griddap/documentation.html",
        "griddap/jplMURSST41.graph",
        "griddap/jplMURSST41.html",
        "info/index.html?page=1&itemsPerPage=1000",
        "info/cwwcNDBCMet/index.html",
        "information.html",
        "opensearch1.1/index.html",
        "rest.html",
        "search/index.html?page=1&itemsPerPage=1000&searchFor=sst",
        "slidesorter.html",
        "subscriptions/index.html",
        "subscriptions/add.html",
        "subscriptions/validate.html",
        "subscriptions/list.html",
        "subscriptions/remove.html",
        "tabledap/documentation.html",
        "tabledap/cwwcNDBCMet.graph",
        "tabledap/cwwcNDBCMet.html",
        "tabledap/cwwcNDBCMet.subset",
        "wms/documentation.html",
        "wms/jplMURSST41/index.html" };
    StringBuilder results = new StringBuilder();
    for (int i = 0; i < pages.length; i++) {
      String content;
      String sa[];
      HashSet<String> hs;
      try {
        content = SSR.getUrlResponseStringUnchanged(tErddapUrl + pages[i]);
      } catch (Exception e) {
        results.append("\n* Trouble: " + e.toString() + "\n");
        continue;
      }

      // Look for ZsomethingZ
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(Z[a-zA-Z0-9]Z)", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " ZtermsZ :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for &something; that are placeholders that should have been replaced by
      // replaceAll().
      // There are some legit uses in changes.html, setup.html, and
      // setupDatasetsXml.html.
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(&amp;[a-zA-Z]+?;)", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " &entities; :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for {0}, {1}, etc that should have been replaced by replaceAll().
      // There are some legit values on setupDatasetsXml.html in regexes ({nChar}:
      // 12,14,4,6,7,8).
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(\\{\\d+\\})", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " {#} :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }
    }
    if (results.length() > 0)
      throw new RuntimeException(results.toString());
  }

}
