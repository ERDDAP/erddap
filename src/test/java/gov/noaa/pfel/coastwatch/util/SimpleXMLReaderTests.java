package gov.noaa.pfel.coastwatch.util;

import com.cohort.util.String2;
import com.cohort.util.Test;
import java.io.ByteArrayInputStream;

class SimpleXMLReaderTests {
  /** This performs a unit test of this class. */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {

    SimpleXMLReader xmlReader;
    String2.log("SimpleXMLReader will now intentionally throw and catch several exceptions.");
    String results, expected;

    // test invalid start of xml
    String error = "";
    try {
      xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(String2.toByteArray("<testa>\n" + "</testa>\n" + "")));
    } catch (Exception e) {
      error = e.toString();
    }
    Test.ensureTrue(error.indexOf(" should have started with \"<?xml \"") > 0, "error=" + error);

    // test invalid end of xml tag
    error = "";
    try {
      xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(
                  String2.toByteArray("<?xml  \n" + ">\n" + "<testa></testa>\n" + "")));
    } catch (Exception e) {
      error = e.toString();
    }
    Test.ensureTrue(error.indexOf(" should have started with \"<?xml \"") > 0, "error=" + error);

    // test first tag is end tag
    error = "";
    try {
      xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(
                  String2.toByteArray(
                      "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" + "</testb>\n" + "")));
      xmlReader.nextTag();
    } catch (Exception e) {
      error = e.toString();
    }
    Test.ensureTrue(
        error.indexOf("End tag </testb> is the only tag on the stack.") > 0, "error=" + error);

    // test non-matching end tag
    error = "";
    try {
      xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(
                  String2.toByteArray(
                      "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
                          + "<testc></bob>\n"
                          + "")));
      xmlReader.nextTag();
      xmlReader.nextTag();
    } catch (Exception e) {
      error = e.toString();
    }
    Test.ensureTrue(
        error.indexOf(" End tag </bob> doesn't have a matching start tag.") > 0, "error=" + error);

    // test no end tag
    error = "";
    try {
      xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(
                  String2.toByteArray(
                      "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" + "<testc>\n" + "")));
      xmlReader.nextTag();
      xmlReader.nextTag();
    } catch (Exception e) {
      error = e.toString();
    }
    expected =
        "java.lang.Exception: ERROR in XML file on line #3: Unexpected end of file with non-empty stack: <testc>\n"
            + "  tag = \n"
            + "  content = \n"
            + "  exception = SimpleXMLReader.getNextTag:\n"
            + "ERROR:\n"
            + "\n"
            + "java.lang.Exception: end of file";
    Test.ensureEqual(error.substring(0, expected.length()), expected, "error=" + error);

    // test un-closed comment
    error = "";
    try {
      xmlReader =
          new SimpleXMLReader(
              new ByteArrayInputStream(
                  String2.toByteArray(
                      "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
                          + "<testd><!--\n"
                          + "</testd>")),
              "testd"); // reads first tag
      xmlReader.nextTag(); // should throw exception
    } catch (Exception e) {
      error = e.toString();
    }
    Test.ensureTrue(error.indexOf("Unclosed comment") > 0, "error=" + error);
    String2.log("That was the last Expected Exception.");

    // test valid xml
    String testXml =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
            + "<?xml-stylesheet type=\"text/xsl\" href=\"../../style/eml/eml-2.0.0.xsl\"?>\n"
            + "<testr>\n"
            + "  <level1 att1=value1 att2=\"value 2\" > \n level 1 \r&amp; <!-- comment < > -->text  \r\n"
            + "  </level1>\n"
            + "  <levela/>\n"
            + // "empty tag" appears as two tags, begin and end
            "  <levelb> stuff <![CDATA[cdata e.g., html content: <kbd>&amp;&gt;&lt;&something;</kbd> stuff]]></levelb>\n"
            + "\n"
            + "\n"
            + "\n"
            + "</testr attr=\"valr\">";
    xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(testXml)));
    Test.ensureEqual(xmlReader.stackSize(), 0, "a");
    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 1, "a");
    Test.ensureEqual(xmlReader.topTag(), "testr", "a");
    Test.ensureEqual(xmlReader.content(), "", "a");
    Test.ensureEqual(xmlReader.allTags(), "<testr>", "a");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 2, "b");
    Test.ensureEqual(xmlReader.topTag(), "level1", "b");
    Test.ensureEqual(xmlReader.content(), "", "b");
    Test.ensureEqual(xmlReader.allTags(), "<testr><level1>", "b");
    Test.ensureEqual(xmlReader.attributeNames().length, 2, "b");
    Test.ensureEqual(xmlReader.attributeValue("att1"), "value1", "b");
    Test.ensureEqual(xmlReader.attributeValue("att2"), "value 2", "b");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 2, "c");
    Test.ensureEqual(xmlReader.topTag(), "/level1", "c");
    Test.ensureEqual(xmlReader.content(), "level 1 & text", "c");
    Test.ensureEqual(xmlReader.allTags(), "<testr></level1>", "c");
    Test.ensureEqual(xmlReader.attributeNames().length, 0, "c");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 2, "q");
    Test.ensureEqual(xmlReader.topTag(), "levela", "q");
    Test.ensureEqual(xmlReader.content(), "", "q");
    Test.ensureEqual(xmlReader.allTags(), "<testr><levela>", "q");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 2, "qb");
    Test.ensureEqual(xmlReader.topTag(), "/levela", "qb");
    Test.ensureEqual(xmlReader.content(), "", "qb");
    Test.ensureEqual(xmlReader.allTags(), "<testr></levela>", "qb");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 2, "d");
    Test.ensureEqual(xmlReader.topTag(), "levelb", "d");
    Test.ensureEqual(xmlReader.content(), "", "d");
    Test.ensureEqual(xmlReader.allTags(), "<testr><levelb>", "d");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 2, "e");
    Test.ensureEqual(xmlReader.topTag(), "/levelb", "e");
    Test.ensureEqual(
        xmlReader.content(),
        "stuff cdata e.g., html content: <kbd>&amp;&gt;&lt;&something;</kbd> stuff",
        "e");
    Test.ensureEqual(xmlReader.allTags(), "<testr></levelb>", "e");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 1, "f");
    Test.ensureEqual(xmlReader.topTag(), "/testr", "f");
    Test.ensureEqual(xmlReader.content(), "", "f");
    Test.ensureEqual(xmlReader.allTags(), "</testr>", "f");
    Test.ensureEqual(xmlReader.attributeValue("attr"), "valr", "b");

    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.stackSize(), 0, "g");
    Test.ensureEqual(xmlReader.topTag(), null, "g");
    Test.ensureEqual(xmlReader.content(), "", "g");
    Test.ensureEqual(xmlReader.allTags(), "", "g");
    xmlReader.close();

    // skipToStackSize();
    // "<testr>\n" +
    // " <level1 att1=value1 att2=\"value 2\" > \n level 1 \r&amp; <!-- comment < >
    // -->text \r\n" +
    // " </level1>\n" +
    String2.log("test skipToClosingTag()");
    xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(testXml)));
    xmlReader.nextTag();
    Test.ensureEqual(xmlReader.topTag(), "testr", "k");
    xmlReader.skipToStackSize(xmlReader.stackSize());
    Test.ensureEqual(xmlReader.topTag(), "/testr", "f");
    Test.ensureEqual(xmlReader.content(), "", "f");
    Test.ensureEqual(xmlReader.allTags(), "</testr>", "f");
    Test.ensureEqual(xmlReader.attributeValue("attr"), "valr", "b");
    xmlReader.close();

    // readDocBookAsPlainText
    testXml =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n"
            + "<DocBook><section>\n"
            + "<title>This Is A Title</title>\n"
            + "<subtitle>This Is A Subtitle</subtitle>\n"
            + "<para>This is some text <citetitle>My Cite Title</citetitle>,\n"
            + "Please read <ulink url=\"http://...\">My Book</ulink> because it's great.\n"
            + "This is a <value>value</value> <emphasis>em</emphasis>\n"
            + "H<subscript>sub</subscript><superscript>sup</superscript>.\n"
            + "</para>\n"
            + "<literalLayout>This is a\n"
            + "literalLayout.</literalLayout>\n"
            + "<orderedlist><listitem> item 1 </listitem><listitem>  item 2</listitem></orderedlist>Some text.\n"
            + "</section></DocBook>";
    xmlReader =
        new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(testXml)), "DocBook");
    results = xmlReader.readDocBookAsPlainText();
    Test.ensureEqual(
        results,
        "This Is A Title\n"
            + "\n"
            + "This Is A Subtitle\n"
            + "\n"
            + "This is some text \"My Cite Title\" ,\n"
            + "Please read My Book (http://...) because it's great.\n"
            + "This is a [value]value[/value]  [emphasis]em[/emphasis] H[subscript]sub[/subscript] [superscript]sup[/superscript].\n"
            + "\n"
            + "This is a\n"
            + "literalLayout.\n"
            + "\n"
            + "* item 1\n"
            + "\n"
            + "* item 2\n"
            + "\n"
            + "Some text.",
        "results=" + results);

    String2.log("SimpleXMLReader.tests's tests finished successfully.\n");
  }
}
