package com.cohort.util;

import java.io.BufferedReader;
import java.io.StringReader;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class XMLTests {

  /**
   * Test the methods in this class.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  void basicTest() throws Exception {
    String2.log("\n*** XML.basicTest");

    // test removeHTMLTags
    String2.log("test removeHTMLTags");
    Test.ensureEqual(
        XML.removeHTMLTags(
            "Hi, <strong>erd.data&amp;</strong>! <a href=\"http://someUrl\">Click here!</a>"),
        "Hi, erd.data&! [ http://someUrl ] Click here!",
        "a");

    // test encodeAsXML
    String2.log("test encode");
    Test.ensureEqual(
        XML.encodeAsTerminal("Hi &<>\"°\u1234Bob"), "Hi &amp;&lt;&gt;&quot;°&#x1234;Bob", "XML");
    Test.ensureEqual(
        XML.encodeAsXML("Hi &<>\"°\u1234Bob"),
        "Hi &amp;&lt;&gt;&quot;°" + ((char) 4660) + "Bob",
        "XML");
    Test.ensureEqual(
        XML.encodeAsHTML("Hi &<>\"°\u1234Bob"), "Hi &amp;&lt;&gt;&quot;&deg;&#x1234;Bob", "HTML");
    Test.ensureEqual(
        XML.encodeAsHTMLAttribute("Hi &<>\"°\u1234Bob"),
        "Hi&#x20;&#x26;&#x3c;&#x3e;&#x22;&#xb0;&#x1234;Bob",
        "HTML");

    // test decodeEntities
    String2.log("test decodeEntities"); // 037 tests leading 0, which is valid
    Test.ensureEqual(
        XML.decodeEntities("Hi&#037;&#37;&#x025;&#x25; &amp;&lt;&gt;&quot;&nbsp;&#176;&deg;"),
        "Hi%%%% &<>\"\u00a0°°",
        "decode");

    for (int ch = 0; ch < 260; ch++) {
      if (ch >= 128 && ch < 160) // don't test Windows-1252 characters
      continue;
      char ch1 = (char) ch;
      String ch2 = XML.decodeEntities(XML.encodeAsTerminal("" + ch1));
      if (ch2.length() > 0 && ch != 160) // #160=nbsp decodes as #20=' '
      Test.ensureEqual(ch2, "" + ch1, "XML encode/decode ch=" + ch);
      ch2 = XML.decodeEntities(XML.encodeAsHTML("" + ch1));
      if (ch2.length() > 0) Test.ensureEqual(ch2, "" + ch1, "HTML encode/decode ch=" + ch);
    }

    // test textToXMLName
    String2.log("test textToXMLName");
    Test.ensureEqual(XML.textToXMLName("3 my._-te#st"), "_3_my._-test", "e");

    // test substitute
    String doc =
        "<!-- comment --> "
            + "<tag1>a<tag2>bb</tag2>ccc<tag3>dddd<tag4>eeee</tag4></tag3></tag1>f<!--comment-->g";
    String sub[] = new String[] {"<tag1><tag2>Nate", "<tag1><tag2><tag3><tag4>Nancy"};
    StringBuilder sb = new StringBuilder(doc);
    XML.substitute(sb, sub);
    Test.ensureEqual(
        sb.toString(),
        "<!-- comment --> "
            + "<tag1>a<tag2>Nate</tag2>ccc<tag3>dddd<tag4>Nancy</tag4></tag3></tag1>f<!--comment-->g",
        "f");

    sub = new String[] {"<tag1><tag2> <tag3><tag4>Nancy"}; // test non-contiguous
    sb = new StringBuilder(doc);
    XML.substitute(sb, sub);
    Test.ensureEqual(
        sb.toString(),
        "<!-- comment --> "
            + "<tag1>a<tag2> <tag3><tag4>Nancy</tag2>ccc<tag3>dddd<tag4>eeee</tag4></tag3></tag1>f<!--comment-->g",
        "g");

    // test removeComments
    sb = new StringBuilder(doc);
    XML.removeComments(sb);
    Test.ensureEqual(
        sb.toString(),
        " " + "<tag1>a<tag2>bb</tag2>ccc<tag3>dddd<tag4>eeee</tag4></tag3></tag1>fg",
        "h");

    // ***** tests of XPath
    XPath xPath = XML.getXPath();
    Document document =
        XML.parseXml(
            new BufferedReader(
                new StringReader(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                        + "<testr>\n"
                        + "  <level1 att1=\"value1\" att2=\"value 2\" > level 1 &amp; <!-- comment -->text  \n"
                        + "  </level1>\n"
                        + "  <levela />\n"
                        + // "empty tag" appears as two tags, begin and end
                        "  <levelb> <!-- < > --> stuff</levelb>\n"
                        + "  <level1>test of level 1</level1>\n"
                        + "\n"
                        + "\n"
                        + "\n"
                        + "</testr>")),
            false);

    // test of XPath with valid xml
    NodeList nodeList = XML.getNodeList(document, xPath, "/*");
    Test.ensureEqual(nodeList.getLength(), 1, "");
    Test.ensureEqual(nodeList.item(0).getNodeName(), "testr", "get root node");

    // get all <level1> tags
    nodeList = XML.getNodeList(document, xPath, "/testr/level1");
    Test.ensureEqual(nodeList.getLength(), 2, "");
    // get all contained text (even for sub elements)
    Test.ensureEqual(XML.getTextContent(nodeList.item(0)), "level 1 & text", "get text");
    Test.ensureEqual(XML.getTextContent(nodeList.item(1)), "test of level 1", "get text");
    Test.ensureEqual(
        XML.getTextContent(XML.getFirstNode(document, xPath, "/testr/level1")),
        "level 1 & text",
        "get text");
    Test.ensureEqual(XML.getTextContent(null), "", "get text");
    Test.ensureEqual(
        XML.getTextContent1(document, xPath, "/testr/level1"), "level 1 & text", "get text");

    // get all <level1> tags that have an att1 attribute
    nodeList = XML.getNodeList(document, xPath, "/testr/level1[@att1]");
    Test.ensureEqual(nodeList.getLength(), 1, "");
    Test.ensureEqual(XML.getAttribute(nodeList.item(0), "att1"), "value1", "get attribute");
    Test.ensureEqual(XML.getAttribute(nodeList.item(0), "zz"), null, "get attribute");

    Test.ensureEqual(
        XML.getAttribute(XML.getFirstNode(document, xPath, "/testr/level1[@att1]"), "att1"),
        "value1",
        "get attribute");
    Test.ensureEqual(XML.getAttribute(null, "att1"), null, "get attribute");

    // get all <level1> tags that have an att1 and att2 attributes
    nodeList = XML.getNodeList(document, xPath, "/testr/level1[@att1 and @att2]");
    Test.ensureEqual(nodeList.getLength(), 1, "");

    // get all <level1> tags that have an att1 attribute = "value1"
    // there are also options for contains and startsWith
    nodeList = XML.getNodeList(document, xPath, "/testr/level1[@att1='value1']");
    Test.ensureEqual(nodeList.getLength(), 1, "");

    // test minimalEncodeSpaces
    Test.ensureEqual(XML.minimalEncodeSpaces(""), "", "");
    Test.ensureEqual(XML.minimalEncodeSpaces(" "), "\u00A0", "");
    Test.ensureEqual(XML.minimalEncodeSpaces("  "), "\u00A0\u00A0", "");
    Test.ensureEqual(XML.minimalEncodeSpaces(" a "), "\u00A0a\u00A0", "");
    Test.ensureEqual(XML.minimalEncodeSpaces("  a  "), "\u00A0\u00A0a\u00A0\u00A0", "");
    Test.ensureEqual(XML.minimalEncodeSpaces(" ab "), "\u00A0ab\u00A0", "");
    Test.ensureEqual(XML.minimalEncodeSpaces("a b"), "a b", "");
    Test.ensureEqual(XML.minimalEncodeSpaces(" a  b "), "\u00A0a\u00A0\u00A0b\u00A0", "");
    Test.ensureEqual(
        XML.minimalEncodeSpaces("  a   bc d  "),
        "\u00A0\u00A0a\u00A0\u00A0\u00A0bc d\u00A0\u00A0",
        "");

    String2.log("XML.test finished successfully.");
  }
}
