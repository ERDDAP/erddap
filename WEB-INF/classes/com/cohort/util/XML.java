/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohortsoftware.com or contact CoHortSoftware@gmail.com.
 */
package com.cohort.util;

import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.io.StringSubstitutorReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/** This has some static methods to facilitate reading and writing an XML file. */
public class XML {

  /**
   * For each character 0 - 255, these indicate how the character should appear in HTML content. See
   * HTML &amp; XHTML book, Appendix F. XML is same as HTML for 0-127 (see
   * https://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
   * ) &quot; and &#39; are encoded to be safe (see encodeAsXML comments) and consistent with
   * encodeAsXML.
   */
  public static final ImmutableList<String> HTML_ENTITIES =
      ImmutableList.of(
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "&#9;", // 0..  tab
          "\n",
          "",
          "",
          "\r",
          "",
          "",
          "",
          "",
          "",
          "", // 10..
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "", // 20..
          "",
          "",
          " ",
          "!",
          "&quot;",
          "#",
          "$",
          "&#37;",
          "&amp;",
          "&#39;", // 30..   //% re percent encoding
          "(",
          ")",
          "*",
          "+",
          ",",
          "-",
          ".",
          "/",
          "0",
          "1", // 40..
          "2",
          "3",
          "4",
          "5",
          "6",
          "7",
          "8",
          "9",
          ":",
          ";", // 50..
          "&lt;",
          "=",
          "&gt;",
          "?",
          "@",
          "A",
          "B",
          "C",
          "D",
          "E", // 60..
          "F",
          "G",
          "H",
          "I",
          "J",
          "K",
          "L",
          "M",
          "N",
          "O", // 70..
          "P",
          "Q",
          "R",
          "S",
          "T",
          "U",
          "V",
          "W",
          "X",
          "Y", // 80..
          "Z",
          "[",
          "\\",
          "]",
          "^",
          "_",
          "`",
          "a",
          "b",
          "c", // 90..
          "d",
          "e",
          "f",
          "g",
          "h",
          "i",
          "j",
          "k",
          "l",
          "m", // 100..
          "n",
          "o",
          "p",
          "q",
          "r",
          "s",
          "t",
          "u",
          "v",
          "w", // 110..
          "x",
          "y",
          "z",
          "{",
          "|",
          "}",
          "~",
          "", // 120..
          // Assume 128-159 are from Windows https://en.wikipedia.org/wiki/Windows-1252
          // So convert them to ASCII (a few) or HTML character entity.
          //                                                upArrow  upDownArrow
          //                          Zcaron
          "&euro;",
          "",
          ",",
          "&fnof;",
          ",,",
          "&hellip;",
          "&#8224;",
          "&#8225;",
          "^",
          "&permil;",
          "&Scaron;",
          "&lsaquo;",
          "&OElig;",
          "",
          "&#381;",
          "", // 128..
          "",
          "'",
          "'",
          "&quot;",
          "&quot;",
          "&bull;",
          "&ndash;",
          "&mdash;",
          "~",
          "&trade;",
          "&scaron;",
          "&rsaquo;",
          "&oelig;",
          "",
          "&#382;",
          "&Yuml;", // 144
          "&nbsp;",
          "&iexcl;",
          "&cent;",
          "&pound;",
          "&curren;", // 160
          "&yen;",
          "&brvbar;",
          "&sect;",
          "&uml;",
          "&copy;", // 165
          "&ordf;",
          "&laquo;",
          "&not;",
          "&shy;",
          "&reg;", // 170
          "&macr;",
          "&deg;",
          "&plusmn;",
          "&sup2;",
          "&sup3;", // 175..
          "&acute;",
          "&micro;",
          "&para;",
          "&middot;",
          "&cedil;", // 180
          "&sup1;",
          "&ordm;",
          "&raquo;",
          "&frac14;",
          "&frac12;", // 185..
          "&frac34;",
          "&iquest;",
          "&Agrave;",
          "&Aacute;",
          "&Acirc;", // 190
          "&Atilde;",
          "&Auml;",
          "&Aring;",
          "&AElig;",
          "&Ccedil;", // 195..
          "&Egrave;",
          "&Eacute;",
          "&Ecirc;",
          "&Euml;",
          "&Igrave;", // 200
          "&Iacute;",
          "&Icirc;",
          "&Iuml;",
          "&ETH;",
          "&Ntilde;", // 205..
          "&Ograve;",
          "&Oacute;",
          "&Ocirc;",
          "&Otilde;",
          "&Ouml;", // 210
          "&times;",
          "&Oslash;",
          "&Ugrave;",
          "&Uacute;",
          "&Ucirc;", // 215..
          "&Uuml;",
          "&Yacute;",
          "&THORN;",
          "&szlig;",
          "&agrave;", // 220
          "&aacute;",
          "&acirc;",
          "&atilde;",
          "&auml;",
          "&aring;", // 225..
          "&aelig;",
          "&ccedil;",
          "&egrave;",
          "&eacute;",
          "&ecirc;", // 230
          "&euml;",
          "&igrave;",
          "&iacute;",
          "&icirc;",
          "&iuml;", // 235..
          "&eth;",
          "&ntilde;",
          "&ograve;",
          "&oacute;",
          "&ocirc;", // 240
          "&otilde;",
          "&ouml;",
          "&divide;",
          "&oslash;",
          "&ugrave;", // 245..
          "&uacute;",
          "&ucirc;",
          "&uuml;",
          "&yacute;",
          "&thorn;", // 250
          "&yuml;"); // 255

  public static final HashMap<String, Character> ENTITY_TO_CHAR_HASHMAP = new HashMap<>();

  static {
    Test.ensureEqual(HTML_ENTITIES.size(), 256, "HTML_ENTITIES.length");
    for (int i = 0; i < 256; i++) {
      if (i >= 128 && i < 160) // but not the Windows-1252 characters
      continue;
      String ent = HTML_ENTITIES.get(i);
      if (ent.length() > 0) ENTITY_TO_CHAR_HASHMAP.put(ent, (char) i);
    }
  }

  /**
   * This returns a String with the HTML tags removed and common entities (&amp; &lt; &gt; &quot;
   * &nbsp;) converted to the original characters.
   *
   * @param htmlString
   * @return the plain text version
   */
  public static String removeHTMLTags(String htmlString) {
    // copy non-tags to a StringBuilder
    int htmlStringLength = htmlString.length();
    StringBuilder sb = new StringBuilder();
    int po = 0; // next char to be read
    while (po < htmlStringLength) {
      char ch = htmlString.charAt(po++);

      // is it the start of a tag? skip the tag
      if (ch == '<') {
        int po1 = po - 1;
        while (po < htmlStringLength && ch != '>') {
          ch = htmlString.charAt(po++);
        }
        // save href from <a> or <img>
        String tag = htmlString.substring(po1, po);
        String href = String2.extractCaptureGroup(tag, "href=\"(.*?)\"", 1);
        if (String2.isUrl(href)) // just show if it is a complete URL, not if relative fragment
        sb.append(
              (sb.length() > 0 && !String2.isWhite(sb.charAt(sb.length() - 1)) ? " " : "")
                  + "[ "
                  + href
                  + " ] ");
      } else sb.append(ch);
    }
    return decodeEntities(sb.toString());
  }

  /**
   * This replaces chars 0 - 255 with their corresponding HTML_ENTITY and higher chars with the hex
   * numbered entity.
   *
   * <p>char 0 - 127 are encoded same as encodeAsXML. <br>
   * char &gt;=256 are encoded as &#xhhhh (! although they don't need to be since HTML docs are
   * UTF-8 docs).
   *
   * @param plainText the string to be encoded. If null, this throws exception.
   * @return the encoded, 7-bit ASCII string. SSR.sendEmail relies on result being 7-bit.
   */
  public static String encodeAsHTML(String plainText) {
    // FUTURE: should it convert pairs of spaces to sp + &nbsp;  ?
    int size = plainText.length();
    StringBuilder output = new StringBuilder(size * 2);

    for (int i = 0; i < size; i++) {
      int chi = plainText.charAt(i); // note: int
      if (chi <= 255) output.append(HTML_ENTITIES.get(chi));
      else output.append("&#x").append(Integer.toHexString(chi)).append(";");
    }

    return output.toString();
  }

  /**
   * If encodeAsHTML is true, this encodes as HTML; otherwise it returns the original string.
   *
   * @param s
   * @param encodeAsHTML
   * @return If encodeAsHTML is true, this encodes as HTML; otherwise it returns the original
   *     string.
   */
  public static String encodeAsHTML(String s, boolean encodeAsHTML) {
    return encodeAsHTML ? encodeAsHTML(s) : s;
  }

  /**
   * For security reasons, for text that will be used as an HTML or XML attribute, this replaces
   * non-alphanumeric characters with HTML Entity &amp;#xHHHH; format. See HTML Attribute Encoding
   * at https://owasp.org/www-pdf-archive/OWASP_Cheatsheets_Book.pdf pg 188, section 25.4 "Encoding
   * Type: HTML Attribute Encoding Encoding Mechanism: Except for alphanumeric characters, escape
   * all characters with the HTML Entity &#xHH; format, including spaces. (HH = Hex Value)". On the
   * need to escape HTML attributes: http://wonko.com/post/html-escaping
   *
   * @param plainText the string to be encoded. If null, this throws exception.
   * @return the encoded string
   */
  public static String encodeAsHTMLAttribute(String plainText) {
    int size = plainText.length();
    StringBuilder output = new StringBuilder(size * 2);

    for (int i = 0; i < size; i++) {
      int chi = plainText.charAt(i); // note: int
      if (String2.isDigitLetter(chi)) output.append((char) chi);
      else output.append("&#x").append(Integer.toHexString(chi)).append(";");
    }

    return output.toString();
  }

  /**
   * This is the standard encodeAsXML which leaves chars &gt;=256 as is.
   *
   * @param plainText
   */
  public static String encodeAsXML(String plainText) {
    return encodeAsXMLOpt(plainText, false);
  }

  /**
   * This replaces '&amp;', '&lt;', '&gt;', '"', ''' in the string with "&amp;amp;", "&amp;lt;",
   * "&amp;gt;", "&amp;quot;", "&amp;#39;" so plainText can be safely stored as a quoted string
   * within XML.
   *
   * <p>XML is same as HTML for 0-127 (see
   * https://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
   * ) And see "XML in a Nutshell" book, pg 20 for info on these 5 character encodings (and no
   * others).
   *
   * <p>This is part of preventing Cross-site-scripting security vulnerability (which allows hacker
   * to insert his javascript into pages returned by server). See Tomcat (Definitive Guide) pg 147.
   *
   * @param plainText the string to be encoded. If null, this throws exception.
   * @param encodeHighChar if true, char &gt;255 are encoded as &#xhhhh;
   * @return the encoded string
   */
  public static String encodeAsXMLOpt(String plainText, boolean encodeHighChar) {
    // future should it:* Pairs of spaces are converted to sp + &nbsp;.
    int size = plainText.length();
    StringBuilder output = new StringBuilder(size * 2);

    for (int i = 0; i < size; i++) {
      int chi = plainText.charAt(i); // note: int
      if (chi <= 127)
        // converting " is important to prevent cross site scripting;
        // it prevents attacker from closing href="..." quotes
        // [No. That's in a parameter. It shouldn't be needed for ordinary XML content.]
        output.append(HTML_ENTITIES.get(chi));
      else if (encodeHighChar && chi > 255)
        output.append("&#x").append(Integer.toHexString(chi)).append(";");
      else output.append((char) chi);
    }

    return output.toString();
  }

  /**
   * This encodes spaces as (char)160 (nbsp) when they are leading, trailing, or more than 1
   * consecutive. #160 (instead of &amp;nbsp; [not supported in XML] or &amp;#160;) is fine because
   * that is the character for a non-break-space. When the stream is encoded as UTF-8, it is
   * appropriately encoded.
   *
   * <p>This is reasonable for HTML, but not recommended for xhtml(?).
   *
   * @param s
   * @return s with some spaces encoded as (char)160 (nbsp)
   */
  public static String minimalEncodeSpaces(String s) {
    // String2.log("s=\"" + s + "\"");
    int sLength = s.length();
    if (sLength == 0) return s;

    // count spaces at end
    int nSpacesAtEnd = 0;
    while (nSpacesAtEnd < sLength && s.charAt(sLength - (nSpacesAtEnd + 1)) == ' ') nSpacesAtEnd++;
    StringBuilder sb = new StringBuilder();

    if (nSpacesAtEnd < sLength) {

      // leading spaces
      int tsLength = sLength - nSpacesAtEnd;
      int po = 0;
      while (po < tsLength && s.charAt(po) == ' ') {
        sb.append((char) 160); // "&nbsp;"
        po++;
      }

      // internal more than 1 consecutive
      while (po < tsLength - 1) { // -1 so safe to look at po+1
        if (s.charAt(po) == ' ' && s.charAt(po + 1) == ' ') {
          while (po < tsLength - 1 && s.charAt(po) == ' ') {
            sb.append((char) 160); // "&nbsp;"
            po++;
          }
        } else {
          sb.append(s.charAt(po++));
        }
      }
      sb.append(s.charAt(tsLength - 1));
    }

    // trailing spaces
    sb.append(String.valueOf((char) 160).repeat(Math.max(0, nSpacesAtEnd))); // "&nbsp;"

    return sb.toString();
  }

  /**
   * This is like encodeAsHTML but adds specific line breaks (&lt;br&gt;).
   *
   * @param plainText
   * @param maxLineLength if lines are longer, they are broken
   */
  public static String encodeAsPreHTML(String plainText, int maxLineLength) {
    String s = String2.noLongLinesAtSpace(plainText, maxLineLength, "");
    s = encodeAsHTML(s); // after noLongLines so tags aren't broken
    s = String2.replaceAll(s, "\r", "");
    s = String2.replaceAll(s, "\n", "<br>"); // after encodeAsHTML;
    return s;
  }

  /**
   * This is like encodeAsHTML but adds specific line breaks (&lt;br&gt;). This variant doesn't call
   * noLongLinesAtSpace
   *
   * @param plainText
   */
  public static String encodeAsPreHTML(String plainText) {
    String s = encodeAsHTML(plainText);
    s = String2.replaceAll(s, "\r", "");
    s = String2.replaceAll(s, "\n", "<br>"); // after encodeAsHTML;
    return s;
  }

  /**
   * This replaces HTML character entities (and the XML subset) (e.g., "&amp;amp;", "&amp;lt;",
   * "&amp;gt;", "&amp;quot;", etc.) in the string with characters (e.g., '&amp;', '&lt;', '&gt;',
   * '"', etc.) so the original string can be recovered. Before 2017-10-04 (version 1.82)
   * "&amp;nbsp;" was decoded to regular ' '; now left intact. Unrecognized/invalid entities are
   * left intact so appear as e.g., &amp;#A;.
   *
   * @param s the string to be decoded
   * @return the decoded string
   */
  public static String decodeEntities(String s) {
    int size = s.length();
    StringBuilder output = new StringBuilder(size * 2);

    int i = 0;
    while (i < size) {
      char ch = s.charAt(i++);

      if (ch == '&') {
        int po = s.indexOf(';', i);
        if (po > 0 && po < i + 80) {
          String entity = s.substring(i - 1, po + 1);
          if (entity.charAt(1) == '#') { // e.g., &#37;
            String num = entity.substring(2, entity.length() - 1);
            if (num.length() == 0) {
              // falls through, so shown as &#;
            } else if (num.charAt(0) == 'x') {
              num = "0" + num; // xhhh  hex number -> 0xhhh
            }
            int v =
                String2.parseInt(
                    num); // this relies on leading 0's being ignored -> decimal (not octal)
            output.append(
                v < Character.MAX_VALUE
                    ? "" + (char) v
                    : entity); // show intact original entity as plain text
          } else {
            // search HTML_ENTITIES
            Character CH = ENTITY_TO_CHAR_HASHMAP.get(entity);
            if (CH == null) output.append(entity); // leave intact
            else // do separately to avoid promoting to String
            output.append(CH.charValue());
          }
          i = po + 1;
        } else { // no closing ';' close by!  leave & in place
          output.append(ch);
        }
      } else output.append(ch);
    }
    return output.toString();
  }

  /**
   * Parse an XML file and return a DOM Document. If validating is true, the XML is validated
   * against the DTD specified by DOCTYPE in the file.
   *
   * @param fileName e.g., c:/temp/test.xml
   * @param validating use true to validate the file against the DTD specified in the file.
   * @return a DOM Document
   * @throws Exception if trouble
   */
  public static Document parseXml(String fileName, boolean validating, boolean withEnvSubstitutions)
      throws Exception {
    try (BufferedReader reader = File2.getDecompressedBufferedFileReader(fileName, File2.UTF_8)) {
      InputSource source;
      if (withEnvSubstitutions) {
        source = new InputSource(new StringSubstitutorReader(reader, new StringSubstitutor()));
      } else {
        source = new InputSource(reader);
      }
      return parseXml(source, validating);
    }
  }

  /**
   * Parse an XML file and return a DOM Document. If validating is true, the XML is validated
   * against the DTD specified by DOCTYPE in the file.
   *
   * @param resourceFile XML resource file
   * @param validating use true to validate the file against the DTD specified in the file.
   * @return a DOM Document
   * @throws Exception if trouble
   */
  public static Document parseXml(URL resourceFile, boolean validating) throws Exception {
    try (InputStream decompressedStream = File2.getDecompressedBufferedInputStream(resourceFile);
        InputStreamReader reader = new InputStreamReader(decompressedStream, StandardCharsets.UTF_8)) {
      return parseXml(new InputSource(new BufferedReader(reader)), validating);
    }
  }

  /**
   * Parse XML from a Reader and return a DOM Document. If validating is true, the XML is validated
   * against the DTD specified by DOCTYPE in the file.
   *
   * @param input
   * @param validating use true to validate the file against the DTD specified in the file.
   * @return a DOM Document
   * @throws Exception if trouble
   */
  public static Document parseXml(Reader input, boolean validating) throws Exception {
    return parseXml(new InputSource(input), validating);
  }

  /**
   * Parse XML from a Reader and return a DOM Document. If validating is true, the XML is validated
   * against the DTD specified by DOCTYPE in the file.
   *
   * @param inputSource
   * @param validating use true to validate the file against the DTD specified in the file.
   * @return a DOM Document
   * @throws Exception if trouble
   */
  public static Document parseXml(InputSource inputSource, boolean validating) throws Exception {
    // create a builder factory
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(validating);

    // create the builder and parse the file
    return factory.newDocumentBuilder().parse(inputSource);
  }

  /**
   * Returns an xPath object.
   *
   * @return an XPath object.
   */
  public static XPath getXPath() {
    return javax.xml.xpath.XPathFactory.newInstance().newXPath();
  }

  /**
   * This gets a nodeList for an XPath query. <br>
   * See
   * https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/javax/xml/xpath/package-summary.html
   * <br>
   * See javadoc for xpath <br>
   * See examples at http://javaalmanac.com/egs/org.w3c.dom/xpath_GetElemByAttr.html?l=rel <br>
   * See examples at http://javaalmanac.com/egs/org.w3c.dom/xpath_GetAbsElem.html?l=rel
   *
   * @param item usually a Document from parseXml() above, but may be a NodeList or a Node.
   * @param xPath from getXPath()
   * @param xPathQuery e.g., "/testr/level1". See XPath documentation: https://www.w3.org/TR/xpath
   * @return the NodeList of matching Nodes (it may be of length 0)
   * @throws Exception if trouble
   */
  public static NodeList getNodeList(Object item, XPath xPath, String xPathQuery) throws Exception {
    // NODESET maps to an actual NodeList
    return (NodeList) xPath.evaluate(xPathQuery, item, javax.xml.xpath.XPathConstants.NODESET);
  }

  /**
   * This gets the first node matching an XPath query.
   *
   * @param item usually a Document from parseXml() above, but may be a NodeList or a Node.
   * @param xPath from getXPath()
   * @param xPathQuery e.g., "/testr/level1". See XPath documentation: https://www.w3.org/TR/xpath
   * @return the first node matching an XPath query (or null if none).
   */
  public static Node getFirstNode(Object item, XPath xPath, String xPathQuery) throws Exception {
    // NODESET maps to an actual NodeList
    NodeList nodeList = getNodeList(item, xPath, xPathQuery);
    return nodeList.getLength() == 0 ? null : nodeList.item(0);
  }

  /**
   * This returns the value of the specified attribute of a node.
   *
   * @param node
   * @param attributeName
   * @return attribute's value (null if node=null or attributeName not present)
   */
  public static String getAttribute(Node node, String attributeName) {
    if (node == null) return null;
    Node att = node.getAttributes().getNamedItem(attributeName);
    if (att == null) return null;
    return att.getNodeValue();
  }

  /**
   * This returns the text content contained in this node (and all subelements), with leading and
   * trailing white space removed.
   *
   * @param node
   * @return the text content contained in this node (and all subelements), with leading and
   *     trailing white space removed. This won't return null. This may return "" (if node is null
   *     or no content).
   */
  public static String getTextContent(Node node) {
    if (node == null) return "";
    String s = node.getTextContent();
    int length = s.length();
    int firstValid = 0;
    while (firstValid < length && String2.isWhite(s.charAt(firstValid))) firstValid++;
    int lastValid = length - 1;
    while (lastValid > firstValid && String2.isWhite(s.charAt(lastValid))) lastValid--;
    return s.substring(firstValid, lastValid + 1);
  }
}
