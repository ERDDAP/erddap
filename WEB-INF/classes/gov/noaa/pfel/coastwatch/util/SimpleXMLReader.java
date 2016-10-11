/* This file is Copyright (c) 2005 Robert Simons (CoHortSoftware@gmail.com).
 * See the MIT/X-like license in LICENSE.txt.
 * For more information visit www.cohort.com or contact CoHortSoftware@gmail.com.
 */
package gov.noaa.pfel.coastwatch.util;

import com.cohort.array.Attributes;
import com.cohort.array.StringArray;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

/** 
 * This facilitates reading a simple XML file.
 * The file can have comments (begin with "&lt;!--" and end with "--&gt;").
 *
 * See two related tools in FileVisitorNDLS which use this: tallyXml() and 
 * findMatchingContentInXml().
 */
public class SimpleXMLReader {

    private Reader reader;
    private StringArray stack = new StringArray();
    private StringArray attributeNames = new StringArray();
    private StringArray attributeValues = new StringArray();
    private boolean itsOwnEndTag = false;
    private StringBuilder allTags = new StringBuilder();
    private StringBuilder contentBuffer = new StringBuilder();
    private String content = "";
    private String endWhiteSpace = "";
    private StringBuilder tagBuffer = new StringBuilder();
    private long lineNumber = 1, tagNumber = 0;

    /**
     * This constructor ensures that the first tag starts with "&lt;?xml " and 
     * ends with "?&gt;", and it uses the encoding information.
     * Typical: "&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;".
     *
     * @param inputStream   it need not be buffered
     * @throws Exception if trouble
     */
    public SimpleXMLReader(InputStream inputStream) throws Exception {

        StringBuilder sb = new StringBuilder();
        int b = inputStream.read();
        while (b != '>') {
            sb.append((char)b);
            b = inputStream.read();
            if (b < 0) {
                throwException(
                    "Unexpected end of file while looking for end of first tag=\"" + sb.toString() + "\".");
            } else if (b == '\n') lineNumber++;
        }
        sb.append((char)b);
        if (sb.substring(0, 6).equals("<?xml ") && sb.charAt(sb.length() - 2) == '?') {
        } else {
            throwException("The first XML tag=\"" + sb.toString() + 
                "\" should have started with \"<?xml \" and ended with \"?>\".");
        }

        //deal with encoding
        String encoding = "";
        int po1 = sb.indexOf("encoding=\"");
        if (po1 > 0) {
            po1 += 10;
            int po2 = sb.indexOf("\"", po1);
            if (po2 >= 0) {
                encoding = sb.substring(po1, po2);
            }
        }
       
        //make the reader with the proper encoding
        //String2.log("SimpleXMLReader constructor found encoding=" + encoding);
        reader = new BufferedReader(encoding.length() == 0?
            new InputStreamReader(inputStream) :
            new InputStreamReader(inputStream, encoding));
    }

    /**
     * This constructor ensures that the first tag starts with "&lt;?xml " and 
     * ends with "?&gt;", and it uses the encoding information.
     * Typical: "&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;".
     * This constructor also ensures that the root tag is as specified.
     * (Obviously) the resulting simpleXMLReader has just read the rootTag.
     *
     * @param inputStream   it need not be buffered
     * @param rootTag  e.g., "erddapDatasets"
     * @throws Exception if trouble
     */
    public SimpleXMLReader(InputStream inputStream, String rootTag) throws Exception {
        this(inputStream);
        nextTag();
        String tags = allTags();
        if (!tags.equals("<" + rootTag + ">")) 
            throwException("First tag=" + tags + " should have been <" + rootTag + ">.");
    }

    /* *  NOT YET TESTED
     * This constructor works with a reader 
     * (and doesn't ensure that the first tag starts with "&lt;?xml " which should
     * already have been consumed).
     *
     * @param reader   it need not be buffered
     * @throws Exception if trouble
     */
    //public SimpleXMLReader(Reader tReader) throws Exception {
    //    reader = tReader;
    //}

    /**
     * This returns the current line number in the source xml file.
     *
     * @return the current line number in the source xml file.
     */
    public long lineNumber() {
        return lineNumber;
    }

    /**
     * This returns the number of times nextTag has been called.
     *
     * @return the number of times nextTag has been called.
     */
    public long tagNumber() {
        return tagNumber;
    }

    /**
     * This returns the requested item from the stack of tags.
     * Call this right after nextTag().
     *
     * @param item 
     * @return the requested tag (not including the '&lt;' and '&gt;') or null if 
     *   the item number is invalid.   
     */
    public String tag(int item) {
        if (item < 0 || item >= stack.size()) 
            return null;
        return stack.get(item);
    }
        
    /**
     * This returns the top item on the stack of tags.
     * Call this right after nextTag().
     *
     * @return the requested tag (not including the '&lt;' and '&gt;') or null if 
     *   the stack is empty
     */
    public String topTag() {
        if (stack.size() == 0) 
            return null;
        return stack.get(stack.size() - 1);
    }

    /**
     * This returns the size of the stack of tags.
     * Call this right after nextTag().
     *
     * @return the size of the stack.
     * It will only be 0 at the end of the file (or possibly if an error occurred in getNextTag).
     */
    public int stackSize() {
        return stack.size();
    }

    /**
     * This returns the stack of tags formatted as "<tag0><tag1><tag2>".
     *
     * @return the stack of tags formatted as "<tag0><tag1><tag2>".
     */
    public String allTags() {
        return allTags.toString();
    }
        

    /**
     * This returns the trim'd content which occurred right before that last tag.
     * Call this right after nextTag().
     * So this is normally called right after an end tag.
     *
     * @return the content from right before the end tag (or null if end-of-file).
     *   Common entities (&amp;amp; &amp;lt; &amp;gt; &amp;quot;) are converted
     *   to the original characters.
     *   &amp;nbsp; is converted to a regular space.   
     *   All CR (#13) are removed. All LF (#10) are intact.
     *   If no content, this will be "" (not null).
     */
    public String content() {
        return content;
    }

    /**
     * This returns the whitespace right before the last tag.
     * Call this right after nextTag().
     * This should rarely be needed
     *
     * @return the whitespace right before the last tag.
     *   If none, this will be "" (not null).
     */
    public String endWhiteSpace() {
        return endWhiteSpace;
    }

    /**
     * Get an array of attributeNames (may be length=0) for the last tag.
     * Call this right after nextTag().
     *
     * @return an array of attributeNames (may be length=0) for the last tag.
     */
    public String[] attributeNames() {
        return attributeNames.toArray();
    }
     
    /**
     * Get the value of a specific attribute of the last tag.
     * Call this right after nextTag().
     *
     * @param attributeName the name of an attribute (e.g., color)
     * @return the value of the specified attributeName (e.g., "0xFFFFFF"),
     *   or null if not found.
     */
    public String attributeValue(String attributeName) {
        int po = attributeNames.indexOf(attributeName, 0);
        if (po < 0) 
            return null;
        return attributeValues.get(po);
    }

    /**
     * Get the attributes in an Attributes object.
     * Call this right after nextTag().
     * This is a copy of the attributes information, not the native data structure.
     *
     * @return the attributes in an Attributes object.
     */
    public Attributes attributes() {
        return attributes(new Attributes());
    }
     
    /**
     * For convenience, this re-uses atts to get the attributes in an Attributes object.
     * Call this right after nextTag().
     *
     * @return the attributes in the Attributes object.
     */
    public Attributes attributes(Attributes atts) {
        atts.clear();
        for (int i = 0; i < attributeNames.size(); i++) 
            atts.add(attributeNames.get(i), attributeValues.get(i));
        return atts;
    }
     
    /**
     * Get the attributes as a comma-separated name=value String (for diagnostic purposes).
     * Call this right after nextTag().
     *
     * @return the attributes as a comma-separated name=value String (for diagnostic purposes).
     */
    public String attributesCSV() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attributeNames.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(attributeNames.get(i) + "=\"" + attributeValues.get(i) + "\"");
        }
        return sb.toString();
    }
     

    /**
     * This reads to the next tag and adds the tag to the stack of tags.
     * Afterwards, you can get info from tag(i), stackSize(), content(),
     *    getAttributeNames(), and/or attributeValue().
     * Afterwards, there will always be a tag on the stack, unless end-of-file.
     * Afterwards, if the next tag was a closing tag, the opening tag
     * will be removed from the stack and the closing tag will be added 
     * (and it will be removed by getNextTag the next time you call it).
     * So, you should stop calling this if an exception is thrown
     * or if there are no tags on the stack after calling this (normal end-of-file).
     *
     * <p>A "empty element" (e.g., &lt;levelb /&gt;) appears here as two tags:
     * a begin tag and an end tag.
     *
     * @throws Exception if trouble (e.g., poorly formed XML)
     */
    public void nextTag() throws Exception {
        tagNumber++;
        //clear things that are always cleared
        attributeNames.clear();
        attributeValues.clear();
        contentBuffer.setLength(0); 

        //was previous tag itsOwnEndTag?
        if (itsOwnEndTag) {
            String tagString = stack.get(stack.size() - 1);
            allTags.delete(
                allTags.length() - (tagString.length() + 2), //2: <>
                allTags.length()); 
            allTags.append("</" + tagString + ">");
            stack.set(stack.size() - 1, "/" + tagString);
            itsOwnEndTag = false;
            return;
        }

        //if there is a end tag on top of the stack, remove it from stack and allTags
        if (stack.size() >= 1 && topTag().startsWith("/")) {
            allTags.delete(
                allTags.length() - topTag().length() - 2, //2: <>
                allTags.length()); 
            stack.remove(stack.size() - 1);
        }

        //get the next tag
        //tagBuffer holds everything between < and > 
        try {
            do {
                tagBuffer.setLength(0); //must be inside the do loop

                //read 'content' to start of tag "<"
                int iCh = reader.read(); 
                if (iCh < 0) throw new Exception("end of file"); 
                else if (iCh == 10) lineNumber++;
                char ch = (char)iCh;
                while (ch != '<') {
                    if (ch != '\r') contentBuffer.append(ch);
                    iCh = reader.read(); 
                    if (iCh < 0) throw new Exception("end of file");
                    else if (iCh == 10) lineNumber++;
                    ch = (char)iCh;
                }

                //read to end of tag ">" (or end of comment tag "-->") (or end of cdata <![CDATA[  ]]>
                boolean done = false;
                while (!done) {
                    iCh = reader.read(); 
                    if (iCh < 0) throw new Exception("end of file");
                    else if (iCh == 10) lineNumber++;
                    ch = (char)iCh;
                    while (ch != '>') {
                        tagBuffer.append(ch);
                        iCh = reader.read(); 
                        if (iCh < 0) throw new Exception("end of file"); 
                        else if (iCh == 10) lineNumber++;
                        ch = (char)iCh;
                    }
                    //String2.log("tagBuffer=[" + tagBuffer.toString()+"]");
                    done = true;
                    //is it a comment
                    if (tagBuffer.length() >= 5 &&
                        tagBuffer.substring(0, 3).equals("!--")) { //it is a comment
                        if (tagBuffer.substring(tagBuffer.length() - 2, tagBuffer.length()).equals("--")) {
                            //end of comment
                            tagBuffer.setLength(0);
                        } else {
                            //not yet end of comment
                            done = false; 
                        }
                    }

                    //is it CDATA
                    if (tagBuffer.length() >= 10 &&
                        tagBuffer.substring(0, 8).equals("![CDATA[")) { //it is CDATA
                        if (tagBuffer.substring(tagBuffer.length() - 2).equals("]]")) {
                            //end of CDATA, transfer to contentBuffer
                            contentBuffer.append(tagBuffer.substring(8, tagBuffer.length() - 2));
                            tagBuffer.setLength(0);
                        } else {                   
                            tagBuffer.append('>');
                            done = false; //not yet end of cdata
                        }
                    }
                }

            //if tag was a comment or cdata, so read more content until the next tag
            } while (tagBuffer.length() == 0);

        } catch (Exception e) { //probably end of file            
            //close it down
            close();

            //look for better explanation of the problem  then end-of-file
            //unclosed comment?
            if (tagBuffer.length() >= 3 && 
                tagBuffer.substring(0, 3).equals("!--") && //it is a comment
                !tagBuffer.substring(tagBuffer.length() - 2, tagBuffer.length()).equals("--")) //it isn't closed
                throwException("Unclosed comment: " + tagBuffer + "\n" +
                    "  stack = " + allTags());

            //stack not empty?
            if (stackSize() > 0) 
                throwException(
                    "Unexpected end of file with non-empty stack: " +
                        allTags() + "\n" +
                    "  tag = " + tagBuffer.toString() + "\n" +
                    "  content = " + content() + "\n" +
                    "  exception = " + MustBe.throwable("SimpleXMLReader.getNextTag", e));

            tagBuffer.setLength(0);
        }

        //deal with 2nd or 3rd ?xml tag at the top
        //<?xml-stylesheet type="text/xsl" href="../../style/eml/eml-2.0.0.xsl"?>
        //String2.log("tag #" + tagNumber + " tagBuffer=" + tagBuffer);
        if (tagNumber == 1 && String2.startsWith(tagBuffer, "?xml")) {
            tagNumber--;
            nextTag();
            return;
        }

        //cleanup
        String2.trim(tagBuffer);
        if (tagBuffer.length() == 0) {
            if (stackSize() > 0) //[5/16/06 I don't understand. But empty tag occurs with stackSize 0 at end]
                throwException("Empty tag when stack = " + allTags());
        } else {
            //check if itsOwnEndTag
            int tagBufferLength = tagBuffer.length();
            if (tagBuffer.charAt(tagBuffer.length() - 1) == '/') {
                itsOwnEndTag = true;
                tagBufferLength--;
                tagBuffer.setLength(tagBufferLength);
            }

            //extract the tagString
            String tagString = null;
            int po = 0;
            while (po < tagBufferLength && !String2.isWhite(tagBuffer.charAt(po)))
                po++;
            if (po == tagBufferLength) 
                tagString = tagBuffer.toString();
            else {
                tagString = tagBuffer.substring(0, po);

                //extract the attributeNames and attributeValues
                while (true) {
                    //eat whitespace
                    while (po < tagBufferLength && String2.isWhite(tagBuffer.charAt(po)))
                        po++;
                    if (po == tagBufferLength)
                        break; //break out of while

                    //get the attributeName
                    int po2 = tagBuffer.indexOf("=", po);
                    if (po2 <= po) 
                        throwException("An attribute name in tag <" + tagBuffer + 
                            "> wasn't followed by =\"value\".");
                    String tAttributeName = tagBuffer.substring(po, po2);
                    po = po2 + 1;

                    //get the attributeValue
                    String tAttributeValue = null;
                    if (po == tagBufferLength)
                        break; //break out of while                  
                    char char0 = tagBuffer.charAt(po);
                    if (char0 == '"' || char0 == '\'') {
                        //value is quoted     
                        //look for matching end quote
                        po++; //points to first char of value
                        po2 = tagBuffer.indexOf("" + char0, po);
                        if (po2 < 0) 
                            throwException("A quoted attribute value in tag <" + tagBuffer + 
                                "> has no end quotes.");
                        tAttributeValue = tagBuffer.substring(po, po2);
                        po = po2 + 1;
                    } else {
                        //value is not quoted   //technically not allowed; all should be quoted
                        po2 = tagBuffer.indexOf(" ", po + 1);
                        if (po2 < 0)
                            po2 = tagBufferLength;
                        tAttributeValue =  tagBuffer.substring(po, po2);
                        po = po2;
                    }

                    //save the attributeName and value 
                    attributeNames.add(tAttributeName);
                    attributeValues.add(XML.decodeEntities(tAttributeValue));
                } 
            }

            
            //is it an end tag?  
            if (itsOwnEndTag) {
                //itsOwnEndTag can follow a start tag or an end tag
                //this needs more work
            } else if (tagString.charAt(0) == '/') {
                //a regular end tag
                //make sure it has matching start tag, then remove the start tag
                if (stackSize() == 0) 
                    throwException("End tag <" + tagBuffer + "> is the only tag on the stack.");
                if (!topTag().equals(tagString.substring(1))) 
                    throwException("End tag <" + tagBuffer + 
                        "> doesn't have a matching start tag.\n" +
                        "  stack = " + allTags());

                //all is well; remove the start tag
                allTags.delete(
                    allTags.length() - topTag().length() - 2, //2: <>
                    allTags.length()); 
                stack.remove(stack.size() - 1);
            }       

            //add the new tag to the stack and allTags
            allTags.append("<" + tagString + ">");
            stack.add(tagString);
        }
        //trim then decode, not the other way around 
        //(decode converts nbsp to ' ', and trim would remove the spaces at beginning or end)
        int wi = contentBuffer.length();
        while (wi > 0 && Character.isWhitespace(contentBuffer.charAt(wi - 1)))
            wi--;
        endWhiteSpace = contentBuffer.substring(wi);
        content = XML.decodeEntities(String2.trim(contentBuffer).toString());

    }

    /**
     * This skips efficiently until the stackSize is toStackSize.
     * E.g., call this after an opening tag is read in order to skip efficiently 
     * to the matching closing tag by calling skipToStackSize(stackSize()).
     */
    public void skipToStackSize(int toStackSize) throws Exception {
        while (true) {
            nextTag();
            if (stack.size() == toStackSize)
                return;
        }
    }

    /**
     * This throws the standard "Unexpected tag" Exception.
     * This also calls close().
     *
     * @throws Exception
     */
    public void unexpectedTagException() throws Exception {
        throwException("Unexpected tag=" + allTags() + " content=\"" + content() + "\".");
    }

    /**
     * This throws a Exception (prefaced by "ERROR on xml line #...: ").
     * This also calls close().
     *
     * @throws Exception
     */
    public void throwException(String message) throws Exception {
        close();
        throw new Exception(String2.ERROR + " in XML file on line #" + lineNumber +
            ": " + message);
    }

    /**
     * This closes the reader and sets it to null.  
     * Any further calls to getNextTag will throw an exception.
     *
     */
    public void close() {
        try {
            if (reader != null)
                reader.close();
        } catch (Exception e) {
        }
        reader = null;
    }

    /** 
     * Returns true if the system is still open.
     * @return true if the system is still open.
     */
    public boolean isOpen() {
        return reader != null;
    }

    /**
     * This reads EML-like simple DocBook textType info 
     * and converts it to plain text without tags.
     * Call this before reading the first textType tag (e.g., para or section).
     * See example of DocBook at http://docbook.org/docs/howto/howto.xml
     * This is a helper method for generateDatasetsXmlFromEML.
     * see https://knb.ecoinformatics.org/#external//emlparser/docs/eml-2.1.1/./eml-text.html#TextType
     *
     * <p>For now, this is imperfect. 
     * It doesn't catch whitespace at the beginning of content properly.
     * But it is a simple approach that works quite well otherwise.
     * 
     * @return the text content as plain text without tags.
     *  This will return after reading the close tag corresponding to the current allTags.
     */
    public String readDocBookAsPlainText() throws Exception {
        int startStackSize = stackSize();
        StringBuilder sb = new StringBuilder("\n\n");
        String ulinkUrl = null;
        boolean inLiteralLayout = false;
        while (true) {
            nextTag();
            if (startStackSize == stackSize()) {
                //remove whitespace at beginning and end
                String2.trim(sb);
                String2.replaceAll(sb, "] )", "])");
                String2.replaceAll(sb, "] ,", "],");
                String2.replaceAll(sb, "] .", "].");
                return sb.toString();
            }
            String tTag = topTag();
            String tContent = content();
            //standardize on \n
            if (tContent.indexOf('\n') >= 0)
                 tContent = String2.replaceAll(tContent, "\r", "");
            else tContent = String2.replaceAll(tContent, "\r", "\n");
            if (!inLiteralLayout) { 
                //consolidate spaces
                tContent = String2.combineSpaces(tContent);
                tContent = String2.replaceAll(tContent, "\n ", "\n");
                tContent = String2.replaceAll(tContent, " \n", "\n");
            }
            String tEndWhiteSpace = //retained before inline tags
                endWhiteSpace.indexOf('\n') >= 0? "\n" :
                endWhiteSpace.length() > 0? " " : "";
           
            if (tTag.equals( "itemizedlist") ||
                tTag.equals("/itemizedlist") ||
                tTag.equals( "listitem") ||
                tTag.equals("/listitem") ||
                tTag.equals( "literalLayout") ||
                tTag.equals("/literalLayout") ||
                tTag.equals( "orderedlist") ||
                tTag.equals("/orderedlist") ||
                tTag.equals( "para") ||
                tTag.equals("/para") ||
                tTag.equals( "section") ||
                tTag.equals("/section") ||
                tTag.equals( "subtitle") ||
                tTag.equals("/subtitle") ||
                tTag.equals( "title") ||
                tTag.equals("/title")) {

                //append tContent. Not usually any before start tags, but possible.
                sb.append(tContent);  

                //ensure start of new line
                int sbl = sb.length();
                if ((sbl >= 2 && sb.substring(sbl-2).equals("\n\n")) ||
                    (sbl >= 4 && sb.substring(sbl-4).equals("\n\n* "))) {
                } else {
                    sb.append("\n\n");
                }
                if (tTag.equals("listitem"))
                    sb.append("* ");

                if (tTag.equals("literalLayout"))
                    inLiteralLayout = true;
                else if (tTag.equals("/literalLayout"))
                    inLiteralLayout = false;

            } else if (tTag.equals("citetitle")) {
                //e.g., please read <citeTitle>My Book</citeTitle>
                sb.append(tContent + tEndWhiteSpace + "\""); 

            } else if (tTag.equals("/citetitle")) {
                //e.g., please read <citeTitle>My Book</citeTitle>
                sb.append(tContent + "\" "); //sp since xmlReader will eat it

            } else if (tTag.equals("ulink")) {
                //grab the url
                ulinkUrl = attributeValue("url");
                sb.append(tContent + tEndWhiteSpace);

            } else if (tTag.equals("/ulink")) {
                //e.g.:  <ulink url="...">My Book<ulink>
                //write: My Book (url)
                sb.append(tContent);
                if (String2.isSomething(ulinkUrl))
                    sb.append(" (" + ulinkUrl + ") "); //sp since xmlRead will eat them
                ulinkUrl = null;

            } else if (tTag.startsWith("/")) {
                //end tags: add tContent and convert tTag to non-xml-tag
                //e.g., </emphasis> </subscript> </superscript>
                //! Add space after. Not ideal. Next char is not known now and trim'd later.
                sb.append(tContent + tEndWhiteSpace + "[" + tTag + "] ");

            } else {
                //add tContent and convert tTag to non-xml-tag
                //e.g., <emphasis> <subscript> <superscript>
                sb.append(tContent + tEndWhiteSpace + "[" + tTag + "]"); 

            }
        }
    }

    /**
     * This performs a unit test of this class.
     *
     */
    public static void test() throws Exception {

        SimpleXMLReader xmlReader;
        String2.log("SimpleXMLReader will now intentionally throw and catch several exceptions.");
        String results, expected;
        
        //test invalid start of xml
        String error = "";
        try {
            xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(
                "<testa>\n" +
                "</testa>\n" +
                "")));
        } catch (Exception e) {
            error = e.toString();
        }
        Test.ensureTrue(error.indexOf(" should have started with \"<?xml \"") > 0, "error=" + error);

        //test invalid end of xml tag
        error = "";
        try {
            xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(
                "<?xml  \n" +
                ">\n" +
                "<testa></testa>\n" +
                "")));
        } catch (Exception e) {
            error = e.toString();
        }
        Test.ensureTrue(error.indexOf(" should have started with \"<?xml \"") > 0, "error=" + error);

        //test first tag is end tag
        error = "";
        try {
            xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                "</testb>\n" +
                "")));
            xmlReader.nextTag();
        } catch (Exception e) {
            error = e.toString();
        }
        Test.ensureTrue(error.indexOf("End tag </testb> is the only tag on the stack.") > 0, "error=" + error);

        //test non-matching end tag
        error = "";
        try {
            xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                "<testc></bob>\n" +
                "")));
            xmlReader.nextTag();
            xmlReader.nextTag();
        } catch (Exception e) {
            error = e.toString();
        }
        Test.ensureTrue(error.indexOf(" End tag </bob> doesn't have a matching start tag.") > 0, "error=" + error);

        //test no end tag
        error = "";
        try {
            xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                "<testc>\n" +
                "")));
            xmlReader.nextTag();
            xmlReader.nextTag();
        } catch (Exception e) {
            error = e.toString();
        }
expected = 
"java.lang.Exception: ERROR in XML file on line #3: Unexpected end of file with non-empty stack: <testc>\n" +
"  tag = \n" +
"  content = \n" +
"  exception = SimpleXMLReader.getNextTag:\n" +
"ERROR:\n" +
"\n" +
"java.lang.Exception: end of file";
        Test.ensureEqual(error.substring(0, expected.length()), expected, "error=" + error);

        //test un-closed comment
        error = "";
        try {
            xmlReader = new SimpleXMLReader(new ByteArrayInputStream(String2.toByteArray(
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                "<testd><!--\n" +
                "</testd>")), "testd"); //reads first tag
            xmlReader.nextTag(); //should throw exception
        } catch (Exception e) {
            error = e.toString();
        }
        Test.ensureTrue(error.indexOf("Unclosed comment") > 0, "error=" + error);
        String2.log("That was the last Expected Exception.");

        //test valid xml
        String testXml = 
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
            "<?xml-stylesheet type=\"text/xsl\" href=\"../../style/eml/eml-2.0.0.xsl\"?>\n" +
            "<testr>\n" +
            "  <level1 att1=value1 att2=\"value 2\" > \n level 1 \r&amp; <!-- comment < > -->text  \r\n" +
            "  </level1>\n" +
            "  <levela/>\n" +   //"empty tag" appears as two tags, begin and end
            "  <levelb> stuff <![CDATA[cdata < > stuff]]></levelb>\n" + 
            "\n" +
            "\n" +
            "\n" +
            "</testr attr=\"valr\">";
        xmlReader = new SimpleXMLReader(new ByteArrayInputStream(
            String2.toByteArray(testXml)));
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
        Test.ensureEqual(xmlReader.content(), "stuff cdata < > stuff", "e");
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

        //skipToStackSize();
        //"<testr>\n" +
        //"  <level1 att1=value1 att2=\"value 2\" > \n level 1 \r&amp; <!-- comment < > -->text  \r\n" +
        //"  </level1>\n" +
        String2.log("test skipToClosingTag()");
        xmlReader = new SimpleXMLReader(new ByteArrayInputStream(
            String2.toByteArray(testXml)));
        xmlReader.nextTag();
        Test.ensureEqual(xmlReader.topTag(), "testr", "k");
        xmlReader.skipToStackSize(xmlReader.stackSize());
        Test.ensureEqual(xmlReader.topTag(), "/testr", "f");
        Test.ensureEqual(xmlReader.content(), "", "f");
        Test.ensureEqual(xmlReader.allTags(), "</testr>", "f");
        Test.ensureEqual(xmlReader.attributeValue("attr"), "valr", "b");
        xmlReader.close();

        //readDocBookAsPlainText
testXml = 
"<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
"<DocBook><section>\n" +
"<title>This Is A Title</title>\n" +
"<subtitle>This Is A Subtitle</subtitle>\n" +
"<para>This is some text <citetitle>My Cite Title</citetitle>,\n" +
"Please read <ulink url=\"http://...\">My Book</ulink> because it's great.\n" +
"This is a <value>value</value> <emphasis>em</emphasis>\n" +
"H<subscript>sub</subscript><superscript>sup</superscript>.\n" +
"</para>\n" +
"<literalLayout>This is a\n" +
"literalLayout.</literalLayout>\n" +
"<orderedlist><listitem> item 1 </listitem><listitem>  item 2</listitem></orderedlist>Some text.\n" +
"</section></DocBook>";
        xmlReader = new SimpleXMLReader(new ByteArrayInputStream(
            String2.toByteArray(testXml)), "DocBook");
        results = xmlReader.readDocBookAsPlainText();
        Test.ensureEqual(results, 
"This Is A Title\n" +
"\n" +
"This Is A Subtitle\n" +
"\n" +
"This is some text \"My Cite Title\" ,\n" +
"Please read My Book (http://...) because it's great.\n" +
"This is a [value]value[/value]  [emphasis]em[/emphasis] H[subscript]sub[/subscript] [superscript]sup[/superscript].\n" +
"\n" +
"This is a\n" +
"literalLayout.\n" +
"\n" +
"* item 1\n" +
"\n" +
"* item 2\n" +
"\n" +
"Some text.", 
            "results=" + results);

        
        String2.log("SimpleXMLReader.tests's tests finished successfully.\n"); 
    }
}