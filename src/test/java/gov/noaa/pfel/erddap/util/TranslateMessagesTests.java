package gov.noaa.pfel.erddap.util;

import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import java.io.FileInputStream;
import java.util.Arrays;
import tags.TagPassword;

class TranslateMessagesTests {

  /**
   * This checks if a short dontTranslate (DAP) exists before a long dontTranslate. This is REQUIRED
   * for my use of START_SPAN/STOP_SPAN to not translate some text; otherwise, there can be e.g., 2
   * START_SPAN in a row, and that messes up undoing the system.
   */
  @org.junit.jupiter.api.Test
  void testDontTranslateOrdering() throws Exception {
    String2.log("\n*** TranslateMessages.testDontTranslateOrdering()");
    StringBuilder errors = new StringBuilder();
    for (int i1 = 0; i1 < TranslateMessages.nDontTranslate; i1++) {
      String s1 = TranslateMessages.dontTranslate[i1];
      for (int i2 = i1 + 1; i2 < TranslateMessages.nDontTranslate; i2++) {
        if (TranslateMessages.dontTranslate[i2].indexOf(s1) >= 0)
          errors.append(
              "dontTranslate["
                  + i1
                  + "]="
                  + s1
                  + " must not be before ["
                  + i2
                  + "]="
                  + TranslateMessages.dontTranslate[i2]
                  + "\n");
      }
    }
    if (errors.length() > 0)
      throw new RuntimeException("Errors from testDontTranslateOrdering:\n" + errors.toString());
  }

  /** This tests that the connections to Google are correct with simple test code from Google. */
  @org.junit.jupiter.api.Test
  @TagPassword // Can be run with Google Cloud api credentials
  void testGoogleSampleCode() throws Exception {
    String2.log("\n*** TranslateMessages.testGoogleSampleCode()");
    Test.ensureEqual(
        TranslateMessages.googleSampleCode(
            TranslateMessages.googleProjectId,
            "de",
            "The variable name \"sst\" converts to the full name \"Sea Surface Temperature\"."),
        // 2022-01-28 was \"sst\"
        // 2022-01-28 was \"Sea Surface Temperature\"
        "Der Variablenname \u201esst\u201c wird in den vollständigen Namen \u201eSea Surface Temperature\u201c umgewandelt.",
        "");
  }

  /**
   * Use this method to look for closing tags with HTML content by not using CDATA which causes
   * problems (so switch them to use CDATA).
   *
   * @throws Exception when something goes wrong
   */
  @org.junit.jupiter.api.Test
  void findTagsMissingCDATA() throws Exception {
    String2.log("\n*** TranslateMessages.findTagsMissingCDATA()");
    SimpleXMLReader xmlReader =
        new SimpleXMLReader(TranslateMessages.messagesXmlFileName.openStream());
    int nTagsProcessed = 0;
    int nBad = 0;
    while (true) {
      xmlReader.nextTag();
      nTagsProcessed++;
      String rawContent = xmlReader.rawContent().trim(); // rawContent isn't already trimmed!
      // skip comments
      if (rawContent.startsWith("<!--") && rawContent.endsWith("-->")) continue;
      if (Arrays.stream(TranslateMessages.HTMLEntities).anyMatch(rawContent::contains)
          && !TranslateMessages.isHtml(rawContent)) { // html content must be within cdata
        String2.log(nBad + " HTMLEntities >>" + rawContent + "<<");
        nBad++;
      }
      if (xmlReader.stackSize()
          == 3) { // tag not within cdata is trouble, e.g., <erddapMessages><someTag><kbd>
        String2.log(nBad + " stackSize >> " + xmlReader.allTags());
        nBad++;
      }
      if (xmlReader.stackSize() == 0) // done
      break;
    }
    xmlReader.close();
    String2.log("\nfindTagsMissingCDATA finished. nTagsProcessed=" + nTagsProcessed);
    if (nBad > 0)
      throw new RuntimeException(
          "ERROR: findTagsMissingCDATA found " + nBad + " bad tags (above). FIX THEM!");
  }

  /**
   * This test the translation of a difficult HTML test case.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagPassword // Can be run with Google Cloud api credentials
  void testTranslateHtml() throws Exception {
    String2.log("\n*** TranslateMessages.testTranslateHtml()");
    String raw = // a devious string with numerous of test cases
        "<![CDATA[<br>&bull; &lt;subsetVariables&gt; wasn't specified. Is x&gt;90?\nTest &mdash; test2? Test3 &ndash; test4?\n"
            + // 90?\n tests ?[whitespace]
            "<br>Does test5 &rarr; test6? \"&micro;\" means micro. Temperature is &plusmn;5 degrees.\n"
            + "<br>This is a question? This is an answer.\n"
            + // even here, Google removes the space after
            // the '?'!
            "<br>E = &sum;(w Y)/&sum;(w). This is a &middot; (middot).\n"
            + // E =... is a dontTranslate
            // string
            "<h1>ERDDAP</h1> [standardShortDescriptionHtml] Add \"&amp;units=...\" to the query. \n"
            + "<br>Add \"<kbd>&amp;time&gt;now-7days</kbd>\" to the query.\n"
            + // This is a dontTranslate
            // string
            "<br><kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>\n"
            + // test
            // &amp;
            // in
            // dontTranslate
            // phrase
            "<br>Convert &amp; into %26.\n"
            + // &amp; by itself
            "<br>This is version=&wmsVersion; and <kbd>&adminEmail;</kbd>.\n"
            + // both are in dontTranslate
            "See <a rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">Data Provider Form</a>.\n"
            + // &entity; in <a href="">
            "See <img rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">.\n]]"; // &entity; in
    // <a href="">
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    boolean html = TranslateMessages.isHtml(raw);
    boolean messageFormat = TranslateMessages.isMessageFormat(raw);
    StringBuilder results =
        new StringBuilder(
            "RESULTS: html=" + html + " messageFormat=" + messageFormat + "\n" + "en=" + raw);
    String tLang[] = {
      "en", "de"
    }; // {"en", "de"}; {"en", "zh-cn", "de", "hi", "ja", "ru", "th"}; or
    // languageCodeList
    for (int ti = 1; ti < tLang.length; ti++) { // skip 0="en"
      String2.log("lan[" + ti + "]=" + tLang[ti]);
      results.append(
          "\n\n"
              + tLang[ti]
              + "="
              + TranslateMessages.lowTranslateTag(raw, tLang[ti], html, messageFormat));
    }
    String2.setClipboardString(results.toString());
    String2.log("The results are on the clipboard.");
    Test.ensureEqual(
        results.toString(),
        "RESULTS: html=true messageFormat=false\n"
            + //
            "en=<![CDATA[<br>&bull; &lt;subsetVariables&gt; wasn't specified. Is x&gt;90?\n"
            + //
            "Test &mdash; test2? Test3 &ndash; test4?\n"
            + //
            "<br>Does test5 &rarr; test6? \"&micro;\" means micro. Temperature is &plusmn;5 degrees.\n"
            + //
            "<br>This is a question? This is an answer.\n"
            + //
            "<br>E = &sum;(w Y)/&sum;(w). This is a &middot; (middot).\n"
            + //
            "<h1>ERDDAP</h1> [standardShortDescriptionHtml] Add \"&amp;units=...\" to the query. \n"
            + //
            "<br>Add \"<kbd>&amp;time&gt;now-7days</kbd>\" to the query.\n"
            + //
            "<br><kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>\n"
            + //
            "<br>Convert &amp; into %26.\n"
            + //
            "<br>This is version=&wmsVersion; and <kbd>&adminEmail;</kbd>.\n"
            + //
            "See <a rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">Data Provider Form</a>.\n"
            + //
            "See <img rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">.\n"
            + //
            "]]\n"
            + //
            "\n"
            + //
            "de=<![CDATA[\n"
            + //
            "<br>• &lt;subsetVariables&gt; wurde nicht angegeben.\n"
            + //
            "Ist x&gt;90?\n"
            + //
            "Test – Test2?\n"
            + //
            "Test3 – Test4?\n"
            + //
            "<br>Ist test5 → test6?\n"
            + //
            "&quot;µ&quot; steht für Mikro.\n"
            + //
            "Die Temperatur beträgt ±5 Grad.\n"
            + //
            "<br>Das ist eine Frage?\n"
            + //
            "Das ist eine Antwort.\n"
            + //
            "<br>Dies ist ein · (middot E = ∑(w Y)/∑(w) .\n"
            + //
            "<h1>ERDDAP</h1>\n"
            + //
            "[standardShortDescriptionHtml] Fügen Sie der Abfrage &quot;&amp;units=...&quot; hinzu.\n"
            + //
            "<br>Fügen Sie der Abfrage &quot;<kbd>&amp;time&gt;now-7days</kbd>&quot; hinzu.\n"
            + //
            "<br><kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>\n"
            + //
            "<br>Konvertieren Sie &amp; in %26.\n"
            + //
            "<br>Dies ist version= &wmsVersion; und <kbd>&adminEmail;</kbd> .\n"
            + //
            "Siehe\n"
            + //
            "<a rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">Datenproviderformular</a> .\n"
            + //
            "Siehe<img rel=\"bookmark\" href=\"&tErddapUrl;/dataProviderForm1.html\">.\n"
            + //
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
  @TagPassword // Can be run with Google Cloud api credentials
  void testTranslateComment() throws Exception {
    String2.log("\n*** TranslateMessages.testTranslateComment()");
    String raw =
        "<!-- This tests a devious comment with <someTag> and <b> and {0}\nand a newline. -->";
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    boolean html = TranslateMessages.isHtml(raw);
    boolean messageFormat = TranslateMessages.isMessageFormat(raw);
    StringBuilder results =
        new StringBuilder(
            "RESULTS: html=" + html + " messageFormat=" + messageFormat + "\n" + "en=" + raw);
    String tLang[] = {"en", "de"}; // , "zh-cn"}; //Chinese results vary and are hard to test
    for (int ti = 1; ti < tLang.length; ti++) { // skip 0="en"
      String2.log("lan[" + ti + "]=" + tLang[ti]);
      results.append(
          "\n\n"
              + tLang[ti]
              + "="
              + TranslateMessages.lowTranslateTag(raw, tLang[ti], html, messageFormat));
    }
    String2.setClipboardString(results.toString());
    String2.log("The results are on the clipboard.");
    Test.ensureEqual(
        results.toString(),
        "RESULTS: html=false messageFormat=true\n"
            + "en=<!-- This tests a devious comment with <someTag> and <b> and {0}\n"
            + "and a newline. -->\n"
            + "\n"
            + "de=&lt;!-- Dies testet einen hinterhältigen Kommentar mit &lt;someTag&gt; und &lt;b&gt; und {0}\n"
            + "und einer neuen Zeile. --&gt;",
        results.toString());
    // debugMode = oDebugMode;
  }

  /**
   * This test the translation of difficult plain text which uses MessageFormat.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagPassword // Can be run with Google Cloud api credentials
  void testTranslatePlainText() throws Exception {
    String2.log("\n*** TranslateMessages.testTranslatePlainText()");
    String raw = // a devious string with numerous of test cases
        "To download tabular data from ERDDAP''s RESTful services via &tErddapUrl;,\n"
            + "make sure latitude >30 and <50, or \"BLANK\". Add & before every constraint.\n"
            + "For File type, choose one of\n"
            + "the non-image {0} (anything but .kml, .pdf, or .png).\n";
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    boolean html = TranslateMessages.isHtml(raw);
    boolean messageFormat = TranslateMessages.isMessageFormat(raw);
    StringBuilder results =
        new StringBuilder(
            "RESULTS: html=" + html + " messageFormat=" + messageFormat + "\n" + "en=" + raw);
    String tLang[] = {"en", "de"}; // , "zh-cn"}; //chinese results vary and are hard to test
    for (int ti = 1; ti < tLang.length; ti++) { // skip 0="en"
      String2.log("lan[" + ti + "]=" + tLang[ti]);
      results.append(
          "\n\n"
              + tLang[ti]
              + "="
              + TranslateMessages.lowTranslateTag(raw, tLang[ti], html, messageFormat));
    }
    String2.setClipboardString(results.toString());
    String2.log("The results (encoded for storage in XML) are on the clipboard.");
    Test.ensureEqual(
        results.toString(),
        "RESULTS: html=false messageFormat=true\n"
            + "en=To download tabular data from ERDDAP''s RESTful services via &tErddapUrl;,\n"
            + "make sure latitude >30 and <50, or \"BLANK\". Add & before every constraint.\n"
            + "For File type, choose one of\n"
            + "the non-image {0} (anything but .kml, .pdf, or .png).\n"
            + "\n"
            + "\n"
            + "de=Um tabellarische Daten von ERDDAPs RESTful-Diensten über &tErddapUrl; herunterzuladen,\n"
            + "stellen Sie sicher, dass der Breitengrad &gt;30 und &lt;50 oder „LEER“ ist. Fügen Sie vor jeder Einschränkung ein &amp; hinzu.\n"
            + "\n"
            + "Wählen Sie als Dateityp einen\n"
            + "der Nicht-Bild-{0} (alles außer .kml, .pdf oder .png).\n",
        results.toString());
    // debugMode = oDebugMode;
  }
}
