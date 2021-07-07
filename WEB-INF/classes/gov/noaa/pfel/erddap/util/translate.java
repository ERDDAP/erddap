package gov.noaa.pfel.erddap.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.lang.String;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.checkerframework.common.reflection.qual.GetClass;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;

import com.cohort.util.XML;
import com.google.cloud.translate.*;

import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.api.gax.longrunning.OperationFuture;

import com.google.cloud.translate.v3.TranslateTextGlossaryConfig;
import com.google.cloud.translate.v3.CreateGlossaryMetadata;
import com.google.cloud.translate.v3.CreateGlossaryRequest;
import com.google.cloud.translate.v3.GcsSource;
import com.google.cloud.translate.v3.Glossary;
import com.google.cloud.translate.v3.GlossaryInputConfig;
import com.google.cloud.translate.v3.GlossaryName;
import com.google.cloud.translate.v3.GetGlossaryRequest;

import com.google.cloud.translate.v3.DeleteGlossaryRequest;
import com.google.cloud.translate.v3.DeleteGlossaryMetadata;
import com.google.cloud.translate.v3.DeleteGlossaryResponse;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslationServiceClient;
import java.util.ArrayList;
import java.util.List;

public class translate {
    
    private static final String ERDDAPprojectId = "expanded-agent-317002";
    private static final String ERDDAPglossaryId = "ERDDAP-glossary";
    private static final String glossaryURL = "gs://erddap-glossary/erddap-glossary.csv";
    // to be used with the translator
    private static final String location = "us-central1";
    private static final LocationName parent = LocationName.of(ERDDAPprojectId, location);
    private static final GlossaryName glossaryName = GlossaryName.of(ERDDAPprojectId, location, ERDDAPglossaryId);
    private static TranslateTextGlossaryConfig glossaryConfig =
            TranslateTextGlossaryConfig.newBuilder().setGlossary(glossaryName.toString()).build();

    // path to message.xml
    private static final String translatePath = translate.class.getClassLoader().getName();
    private static final String messagePath = "C:/Users/tczqz/Desktop/New folder/apache-tomcat-9.0.46/webapps/erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/messages.xml";
    private static final String oldMessagePath = "C:/Users/tczqz/Desktop/New folder/apache-tomcat-9.0.46/webapps/erddap/WEB-INF/classes/gov/noaa/pfel/erddap/util/messages-Copy.xml";

    
    private static final String[] languageCodeList = {"zh-cn"};
    private static HashSet<String> doNotTranslateSet = new HashSet<String>(Arrays.asList(
        //* all tags that match the regular expression:  <EDDGrid.*Example> ,
        "/EDDGridErddapUrlExample", "/EDDGridIdExample", "/EDDGridDimensionExample", "/EDDGridNoHyperExample", "/EDDGridDimNamesExample", "/EDDGridDataTimeExample", "/EDDGridDataValueExample",
        "/EDDGridDataIndexExample", "/EDDGridGraphExample", "/EDDGridMapExample", "/EDDGridMatlabPlotExample",
        //* all tags that match the regular expression:  <EDDTable.*Example> ,
        "/EDDTableErddapUrlExample", "/EDDTableIdExample", "/EDDTableVariablesExample", "/EDDTableConstraintsExample", "/EDDTableDataTimeExample", "/EDDTableDataValueExample",
        "/EDDTableGraphExample", "/EDDTableMapExample", "/EDDTableMatlabPlotExample",
        //URL and standard names
        "/DEFAULT_commonStandardNames", "/palettes", "/pdfWidths", "/pdfHeights", "/questionMarkImageFile", "/signedToUnsignedAttNames", "/sparqlP01toP02pre", "/sparqlP01toP02post",
        "/startHeadHtml5", "/startBodyHtml5", "/endBodyHtml5", "/standardizeUdunits", "/ucumToUdunits", "/udunitsToUcum", "/updateUrls",
        "/advr_dataStructure", "/advr_cdm_data_type", "/advr_class", "/inotifyFixCommands",
        //abreviations
        "admKeywords", "advl_datasetID", "/extensionsNoRangeRequests", 
        // others
        "/legal", "/imageWidths", "/imageHeights"
    ));
    private static String[] messageFormatEntities = {"{0}", "{1}","''"};
    private static int translationCounter = 0;


    private String[] watchlist = {"<convertKeywordsIntro> <doWithGraphs> <ConvertTimeNotes> </convertOceanicAtmosphericAcronymsService>",
    "/convertOceanicAtmosphericAcronymsService /convertInterpolateNotes /convertUnitsComparison /convertTimeNotes /theLongDescriptionHtml /filesDocumentation"};
    public static void main(String[] args) {
        System.out.println(translatePath);
        Translate translator = TranslateOptions.getDefaultInstance().getService();
        //"<fileHelpGrid_esriAscii>Download an ISO-8859-1 ESRI ASCII file (lat lon data only; lon must be all below or all above 180).</fileHelpGrid_esriAscii>", false));

        String[] HTMLEntities = {"&lt;", "&gt;", "&reg;", "&quot;", "&amp;", "$nbsp;"};
        System.out.println("main() Working Directory = " + System.getProperty("user.dir"));
        

        //deleteGlossary(ERDDAPprojectId, ERDDAPglossaryId);
        //createGlossary(ERDDAPprojectId, ERDDAPglossaryId, Arrays.asList("en","zh-cn","de"));
        
        // This function will detect the tags such that its tag name follows "<EDDGrid.*Example>" or "<EDDTable.*Example>"
        // and add the tags to the doNotTranslateSet
        // Currently not needed because I have found all tags following the pattern (as 6/23/21) and hard coded them to the set
        // addDoNotTranslateSet();
        
        /*
        try {
            HashMap<String, String> original = getXMLTagMap("CDATATest-Original.xml");
            HashMap<String, String> chineseMessages = getXMLTagMap("new-messages-zh-cn.xml");
            HashMap<String, String> messages = getXMLTagMap(messagePath);
            //FileWriter f = new FileWriter("translated.xml");
            //String value =  messages.get("/convertOceanicAtmosphericAcronymsService").substring(9, messages.get("/convertOceanicAtmosphericAcronymsService").length() - 3);
            //f.write(translsadsadateTextV2(translator, "en", "zh-cn", modifyRawContent(value), true));
            //f.write(translateTexasdasdtV2(translator, "en", "zh-cn", modifyRawContent(value), true));
            //f.close();

            //updateGlossary(ERDDAPprojectId, ERDDAPglossaryId, Arrays.asList("en","zh-cn","de"));
            String s = "<li>任何人都可以使用 ERDDAP 的<a rel=\"help\" href=\"&erddapUrl;/slide&amp;sorter.html\">Slide Sorter</a>";
            
            
            String temp = rewriteHStr(translateTextV3("en", "zh-cn", s, true), false);
            System.out.println(temp);
            //deleteGlossary(ERDDAPprojectId, ERDDAPglossaryId);
            //createGlossary(ERDDAPprojectId, ERDDAPglossaryId, Arrays.asList("en","zh-cn","de"));
        
            
            //System.out.println(translatasdasdeTextV2(translator, "en", "zh-cn", "&curlPlaceholder;", true));

        } catch (Exception e) {
            e.printStackTrace();
        }
        */
        //deleteGlossary(ERDDAPprojectId, ERDDAPglossaryId);
        //createGlossary(ERDDAPprojectId, ERDDAPglossaryId, Arrays.asList("en","zh-cn","de"));
        /*
        try {
            updateGlossary(ERDDAPprojectId, ERDDAPglossaryId, Arrays.asList("en","zh-cn","de"));
            SimpleXMLReader xmlReader = getSimpleXMLReader(messagePath);
            //myWriter is a reader used solely for testing purposes
            FileWriter myWriter = new FileWriter("messageTesting.xml");
            HashMap<String, String> previousMessageMap = getXMLTagMap(oldMessagePath);            
            HashMap<String, String>[] translatedTagMaps = (HashMap<String,String>[]) new HashMap[languageCodeList.length];

            for (int j = 0; j < languageCodeList.length; j++) {
                translatedTagMaps[j] = getXMLTagMap("messages-" + languageCodeList[j] + ".xml");
            }

            // add xml description
            myWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            
            FileWriter[] fileWriters = new FileWriter[languageCodeList.length];
            for (int i = 0; i < languageCodeList.length; i++) {
                //2
                fileWriters[i] = new FileWriter("new-messages-" + languageCodeList[i] + "2.xml");
                fileWriters[i].write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); 
            }
            //1101 + 100
            for (int i = 0; i < 1401; i++) {
                //testing what is in xmlReader
                xmlReader.nextTag();
            }
            
            for (int i = 0; i < 100; i++) {
                //testing what is in xmlReader
                xmlReader.nextTag();
                String toTranslate = xmlReader.rawContent();
                String tagName = xmlReader.topTag();

                if (toTranslate.trim().equals("") || toTranslate.equals("") || toTranslate.equals("\n")) {
                    // empty content, do not waste api resources
                    //System.out.println(tagName + " has empty content");
                    myWriter.write(toTranslate);
                    for (int j = 0; j < languageCodeList.length; j++) {
                        fileWriters[j].write(xmlReader.rawContent()); 
                    }
                } else if (doNotTranslateSet.contains(tagName)) {
                    // if the tag is one of the tags we do not want to translate
                    System.out.println(tagName + " is designed to not be translated");
                    myWriter.write(toTranslate);
                    for (int j = 0; j < languageCodeList.length; j++) {
                        fileWriters[j].write(xmlReader.rawContent()); 
                    }
                } else {
                    boolean modified = !previousMessageMap.getOrDefault(tagName, "DNE").equals(xmlReader.rawContent());
                    boolean[] translated = new boolean[languageCodeList.length];
                    for (int j = 0; j < translated.length; j++) {
                        translated[j] = !translatedTagMaps[j].getOrDefault(tagName, "DNE").equals("DNE");
                    }
                    if (toTranslate.startsWith("<!--")) {
                        //if it's a comment, we need to remove the comment syntax and insert a new line character before the tag
                        //also consider the possibility of multiple comments before one tag
                        int commentStart = toTranslate.indexOf("<!--") + 4;
                        int commentEnd = toTranslate.indexOf("-->");
                        String comment = toTranslate.substring(commentStart, commentEnd);
                        // "\n<!--" + translatedText + "-->\n" will be the full translated comment
                        myWriter.write("\n<!--" + comment + "-->\n");
                            //if this tag has not been modified
                        for (int j = 0; j < languageCodeList.length; j++) {
                            if (!modified && translated[j]) {
                                //if we have the a non-null map with a translated message with same tag
                                fileWriters[j].write("\n" + translatedTagMaps[j].get(tagName) + "\n"); 
                            } else {
                                while (commentStart != 3) {
                                    comment = toTranslate.substring(commentStart, commentEnd);
                                    String res = "\n<!--"
                                        + translateTextV3("en", languageCodeList[j], comment, false) 
                                        + "-->"; 
                                    res = rewriteHStr(res, false);
                                    fileWriters[j].write(res);
                                    commentStart = toTranslate.indexOf("<!--", commentEnd) + 4;
                                    commentEnd = toTranslate.indexOf("-->", commentStart);
                                }
                                //newline before next tag
                                fileWriters[j].write("\n");
                            }
                        }    
                    } else if (toTranslate.startsWith("<![CDATA[")) {
                        //CDATA tag    
                        myWriter.write(toTranslate);
                        toTranslate = modifyRawContent(toTranslate);
                        toTranslate = toTranslate.substring(9, toTranslate.length() - 3);

                        boolean messageFormat = Arrays.stream(messageFormatEntities).anyMatch(toTranslate::contains);

                        for (int j = 0; j < languageCodeList.length; j++) {
                            if (!modified && translated[j]) {
                                //if this tag has not been modified but already translated
                                fileWriters[j].write(
                                    translatedTagMaps[j].get(tagName)
                                    );
                            } else {
                                String res = translateTextV3("en", languageCodeList[j], toTranslate, true);
                                res = rewriteHStr(res, messageFormat);
                                if (tagName.equals("/convertOceanicAtmosphericAcronymsService")) {
                                    res = convertOceanicAtmosphericAcronymsService(res);
                                }
                                fileWriters[j].write("<![CDATA[" + res + "]]>");
                            }
                        }
                    } else {
                        // normal non CDATA tag
                        // modification might be needed on converting html entities

                        myWriter.write(toTranslate);

                        boolean html = Arrays.stream(HTMLEntities).anyMatch(toTranslate::contains);
                        boolean messageFormat = Arrays.stream(messageFormatEntities).anyMatch(toTranslate::contains);

                        for (int j = 0; j < languageCodeList.length; j++) {
                            if (!modified && translated[j]) {
                                fileWriters[j].write(
                                    translatedTagMaps[j].get(tagName)
                                    ); 
                            } else {
                                String res;
                                if (html) {
                                    toTranslate = modifyRawContent(toTranslate);
                                    res = translateTextV3("en", languageCodeList[j], toTranslate, true);
                                    res = rewriteHStr(res, false);
                                } else {
                                    res = translateTextV3("en", languageCodeList[j], toTranslate, false);
                                }
                                
                                if (messageFormat) {
                                    res = res.replaceAll("''", "doubleQuotePlaceholder");
                                    res = res.replaceAll("'", "''");
                                    res = res.replaceAll("doubleQuotePlaceholder", "''");
                                }
                                // htmlTableMaxMessage is a normal tag with HTML encoding, so the line breaks are removed
                                if (tagName.equals("/htmlTableMaxMessage")) {
                                    int breakPoint = res.indexOf("&gt;");
                                    res = res.substring(0, breakPoint + 4) + "\n" + res.substring(breakPoint + 4, res.length());
                                    breakPoint = res.indexOf("&gt;", breakPoint + 5);
                                    res = res.substring(0, breakPoint + 4) + "\n" + res.substring(breakPoint + 4, res.length());
                                }
                                fileWriters[j].write(res);
                            }
                        }   
                    }
                }

                //after writing the translated content, we write the <tag>
                myWriter.write("<"+ tagName + ">");                    
                for (int j = 0; j < languageCodeList.length; j++) {
                    fileWriters[j].write("<" + tagName + ">"); 
                }

                if (xmlReader.isEndTag()) {
                    // if the tag is an end tag, we add a new line charcater at the end of the tag
                    myWriter.write("\n");
                    for (int j = 0; j < languageCodeList.length; j++) {
                        fileWriters[j].write("\n"); 
                    }
                }
                // breaks the loop at the end of xml file
                if (xmlReader.allTags().length() == 0) {
                    break;
                }
            }
            // close filewriters
            xmlReader.close();
            for (int j = 0; j < languageCodeList.length; j++) {
                fileWriters[j].close();
            }
            myWriter.close();
            
        } catch (Exception e) {
           System.out.println("An error occurred in main()");
           e.printStackTrace();
        }
        */
        System.out.println("translator called " + translationCounter + " times.");
    }
    /**
     * using SimpleXMLReader to read the XML tags
     * @param path is the path to the target xml file
     * @return a SimpleXMLReader Object to read the xml file
     */
    public static SimpleXMLReader getSimpleXMLReader(String path){
        SimpleXMLReader xmlReader = null;
        //StringArray tagArray = new StringArray();
        try {
            InputStream messagesStream = new FileInputStream(path);
            xmlReader = new SimpleXMLReader(messagesStream);
        } catch (IOException e) {
            return null;
        } catch (Exception e) {
            System.out.println("An Exception occurred in readXMLtags()");
            e.printStackTrace();
        }
        return xmlReader;
    }

    /**
     * Use this method to generate a HashMap of tags in a translated message.xml file
     * @param xmlReader a SimpleXMLReader object set to the target xml file
     * @return A HashMap<key=tag name, value=[comment, tag content]>
     * @throws IOException when something went wrong in accessing the xml file, returns null
     */
    public static HashMap<String, String> getXMLTagMap(SimpleXMLReader xmlReader) throws IOException{        
        HashMap<String, String> resultMap = new HashMap<String, String>();
        if (xmlReader == null) {
            return resultMap;
        }
        try {
            while (true) {
                xmlReader.nextTag();
                resultMap.put(xmlReader.topTag(), xmlReader.rawContent());
                String tags = xmlReader.allTags();
                // breaks the loop at the end of xml file, currently not needed because we are working on first a few tags
                if (tags.length() == 0) {
                    break;
                }
            }
            xmlReader.close();
            return resultMap;
        } catch (IOException e) {
            System.out.println("An IOException occurred in readXMLtags()");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An Exception occurred in readXMLtags()");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Use this method to generate a HashMap of tags in a translated message.xml file
     * @param path the path of the xml file
     * @return A HashMap <key=tagname, value=[comment, tag content]>
     */
    public static HashMap<String, String> getXMLTagMap(String path) throws IOException{        
        return getXMLTagMap(getSimpleXMLReader(path));
    }

    
    /**
     * This function will detect the tags such that its tag name follows "</EDDGrid.*Example>" or "</EDDTable.*Example>" pattern
     * and add the tags to the doNotTranslateSet.
     * Call this before the translation process
     * Currently not needed because Qi have found all tags following the pattern (as 6/23/21) and hard coded them to the set
     */
    private static void addDoNotTranslateSet() {
        try {
            SimpleXMLReader xmlReader = getSimpleXMLReader(messagePath);
            xmlReader.nextTag();
            while (true) {
                xmlReader.nextTag();
                /*
                if (doNotTranslateSet.contains(xmlReader.topTag())) {
                    System.out.println("tag: " + xmlReader.topTag());
                }
                */
                if (xmlReader.allTags().length() == 0) {
                    break;
                }
                if (xmlReader.topTag().endsWith("Example") &&
                    (
                        xmlReader.topTag().startsWith("/EDDGrid") || xmlReader.topTag().startsWith("/EDDTable")
                    )) {
                        doNotTranslateSet.add(xmlReader.topTag());
                }

            }
        } catch (Exception e) {
            System.out.println("An exception occured in addDoNotTranslateSet()");
            e.printStackTrace();
        }
    }
    /**
     * Uses java's built in DOM to read tag information in XML. Only used in testing process
     * @return message.xml in string form
     * @throws IOException
     */
    private static void readXMLtagsDOM() throws IOException{
        try { 
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); 
            DocumentBuilder db = dbf.newDocumentBuilder(); 
            // use parse() to read message.xml
            Document document = db.parse(messagePath); 
            NodeList nl = document.getElementsByTagName("*");
            // check the amount of tags we have
            System.out.println("We have " + nl.getLength() + " tags");
            for(int i = 0; i < 5; i++) { 
                Node tag = nl.item(i); 
                // get the attributes in the tag
                NamedNodeMap nnm = tag.getAttributes(); 
                for(int j = 0; j < nnm.getLength(); j++) { 
                    System.out.println(nnm.item(j).getNodeName() + ":" + nnm.item(j).getNodeValue());
                }
                // next step would be translating the tag values, I assume this is not difficult, but I need to figure out a way to preserve the order of tags
                // System.out.println(tag.getTextContent());
                /*
                The code below checks child nodes, but there's not any tags with child node in the file so it's commented out.

                NodeList childNodes = tag.getChildNodes(); 
                // check child node
                System.out.println(tag.getNodeName() + " has " + childNodes.getLength() + "child nodes"); 
                for(int m = 0; m < childNodes.getLength(); m++) { 
                    // print if the choldnode is an ELEMENT type
                    if(childNodes.item(m).getNodeType() == Node.ELEMENT_NODE) { 
                        System.out.println(childNodes.item(m).getNodeName() +  ":" + childNodes.item(m).getFirstChild().getNodeValue()); 
                    } 
                }
                */
            } 
        } 
        catch (ParserConfigurationException e) { 
            System.out.println("ParserConfigurationException occured in readXMLtags()");
            e.printStackTrace(); 
        } catch (IOException e) {
            System.out.println("IOException occured in readXMLtags()");
            e.printStackTrace(); 
        } catch (SAXException e) {
            System.out.println("SAXException occured in readXMLtags()");
            e.printStackTrace(); 
        }
    }
    
    /**
     * Modify the input string so that strings like "&nbsp;", "\n",
     * "bob dot simons at noaa gov" can be preserved correctly by the translator
     * Call this before sending the text to the translator
     * @param s the rawContent text of a CDATA tag
     * @return the modified text
     */
    public static String modifyRawContent(String rawContent) {
        String res = rawContent.replaceAll("&nbsp;", "<br />");
        res = res.replaceAll("&amp;", "&amp;amp;");
        res = res.replaceAll("\n", " &newLinePlaceHolder; ");

        //content within a tag cannot be used in glossary directly
        res = res.replaceAll("<kbd>algorithm</kbd>", "kbdalgorithm");
        res = res.replaceAll("<kbd>Nearest</kbd>", "kbdnearest");
        res = res.replaceAll("<kbd>Mean</kbd>", "kbdmean");
        res = res.replaceAll("<kbd>Bilinear</kbd>", "kbdbilinear");
        res = res.replaceAll("<kbd>nearyby</kbd>", "kbdnearby");
        res = res.replaceAll("<kbd>SD</kbd>", "kbdSD");
        res = res.replaceAll("<kbd>Median</kbd>", "kbdmedian");
        res = res.replaceAll("<kbd>Scaled</kbd>", "kbdscaled");
        res = res.replaceAll("<kbd>keywords</kbd>", "kbdkeywords");
        
        res = res.replaceAll("http<strong>s</strong>", "httpsStrong");

        res = res.replaceAll("<strong>lines</strong>", "strongLines");
        res = res.replaceAll("<strong>linesAndMarkers</strong>", "strongLinesAndMarkers");
        res = res.replaceAll("<strong>markers</strong>","strongMarkers");
        res = res.replaceAll("<strong>sticks</strong>", "strongSticks");
        res = res.replaceAll("<strong>surface</strong>", "strongSurface");
        res = res.replaceAll("<strong>vector</strong>", "strongVectors");

        res = res.replaceAll("<kbd>-<i>excludedWord</i></kbd>", "kbdExcludedWord");
        res = res.replaceAll("<kbd>&quot;wind speed&quot;</kbd>", "kbdQuoteWindSpeed");
        res = res.replaceAll("<kbd>-&quot;<i>excluded phrase</i>&quot;</kbd>", "kbdquoteExcludedPhrase");
        res = res.replaceAll("<kbd>AND</kbd>", "kbdAND");
        res = res.replaceAll("<kbd>&quot;datasetID=<i>erd</i>&quot;</kbd>", "kbdQuoteDatasetIDERD");
        res = res.replaceAll("<kbd>spee</kbd>", "kbdSpee");
        res = res.replaceAll("<kbd>speed</kbd>", "kbdSpeed");
        res = res.replaceAll("<kbd>WindSpeed</kbd>", "kbdWindSpeed");
        
        return res;
    }


    /**
     * The Google Translator API returns encoded HTML. Use this method to decode and rewrite the HTML code.
     * Do not use it on non-CDATA tags
     * @param s the translated text returned by Google Translator.
     * @param messageFormat if the original string uses message format
     * @return the text ready to be written in the xml file
     */
    private static String rewriteHStr(String s, boolean messageFormat) {
        String res = XML.decodeEntities(s);
        //revert the changes madein modifyRawContent
        res = res.replaceAll("<br />", "&nbsp;");
        res = res.replaceAll(" &newLinePlaceHolder;", "&newLinePlaceHolder;");
        res = res.replaceAll("&newLinePlaceHolder; ", "&newLinePlaceHolder;");
        res = res.replaceAll("&newLinePlaceHolder;", "\n");


        //prevent the &amp; from being decoded in URLs of <a> tags
        // needs modification
        int currIndex = res.indexOf("href", 0);
        int leftQuote;
        int righttQuote;
        while (currIndex != -1) {
            leftQuote = res.indexOf('"', currIndex);
            righttQuote = res.indexOf('"', leftQuote + 1);
            //address1 is the original
            String address1 = res.substring(leftQuote, righttQuote);
            StringBuffer address2 = new StringBuffer(address1);
            if (address1.contains("&") && !address1.contains("&amp;")) {
                int ampIndex = address2.indexOf("&");
                while (ampIndex != -1) {
                    if (!address2.substring(ampIndex, ampIndex + 7).equals("&erddap")) {
                        address2 = address2.replace(ampIndex, ampIndex + 1, "&amp;");
                        System.out.println("replaced in " + s);
                    }
                    ampIndex = address2.indexOf("&", ampIndex + 1);
                }
            }
            res = res.replace(address1, address2.toString());
            currIndex = res.indexOf("href", righttQuote);
        }
        //deal with message format issues
        if (messageFormat) {
            res = res.replaceAll("''", "doubleSingleQuotePlaceholder");
            res = res.replaceAll("'", "''");
            res = res.replaceAll("doubleSingleQuotePlaceholder", "''");
        }

        // Sometimes the translator returns something like <img <a herf = ""> src =""> ...
        // This ensures that <img ...> has proper syntax
        StringBuffer sb = new StringBuffer(res);

        // currIndex = sb.indexOf("<img");
        // int ltIndex = sb.indexOf("<", currIndex + 2);
        // int rtIndex = sb.indexOf(">", currIndex + 4);
        // while (currIndex != -1) {
        //     if (ltIndex < rtIndex) {
        //         sb.replace(ltIndex, rtIndex + 1, "");
        //     }
        //     currIndex = sb.indexOf("<img", currIndex + 1);        
        //     ltIndex = sb.indexOf("<", currIndex + 2);
        //     rtIndex = sb.indexOf(">", currIndex);
        // }

        // Sometimes the translator returns something like <a href=""> </a> <img src =""></a> ...
        // This lopp ensures that the extra </a> between <a> and <img> is removed
        int currAIndex = sb.indexOf("<a ");
        int nextAIndex = sb.indexOf("<a ", currAIndex + 2);
        int aEndTagIndex = sb.indexOf("</a>", currAIndex);
        int imgIndex = sb.indexOf("<img", currAIndex);
        // while (currAIndex != -1) {
        //     //System.out.printf("currIndex: %d, nextAIndex: %d, aEndTagIndex: %d, imgIndex: %d\n", currAIndex,nextAIndex,aEndTagIndex,imgIndex);
        //     if (imgIndex < nextAIndex) {
        //         // the current a tag contains an image
        //         if (aEndTagIndex < imgIndex) {
        //             sb.replace(aEndTagIndex, aEndTagIndex + 4, "");
        //             System.out.println("</a> removed at " + sb.substring(aEndTagIndex, aEndTagIndex + 20));
        //         }
        //     }
        //     currAIndex = sb.indexOf("<a ", currAIndex + 3);
        //     nextAIndex = sb.indexOf("<a ", currAIndex + 1);
        //     aEndTagIndex = sb.indexOf("</a>", currAIndex);
        //     imgIndex = sb.indexOf("<img", currAIndex);
        // }
        res = sb.toString();
        return res;
    }
    /**
     * convertOceanicAtmosphericAcronymsService is a that requires specific rule
     * start from A-Za-z0-9_-!.~''()*
     * @param s the translated and rewritten rawContent of /convertOceanicAtmosphericAcronymsService
     * @return the String ready to write into messages.xml
     */
    private static String convertOceanicAtmosphericAcronymsService(String s) {
        StringBuffer res = new StringBuffer(s);
        int root = res.indexOf("A-Za-z0-9_-!.~''()*");
        // ...''&''
        int toReplace = res.indexOf("&", root);
        res.replace(toReplace, toReplace + 1, "&amp;");
        //& into %26
        toReplace = res.indexOf("&", toReplace + 2);
        res.replace(toReplace, toReplace + 1, "&amp;");

        return res.toString();
    }

    /**
     * This method will translate the input text to the target language and output a String
     * This method uses a free engine, proven to have poor quality. But it's fast and free.
     * @param langFrom source text language code in UTF-8
     * @param langTo target language code in UTF-8
     * @param text the text String to be translated
     * @return the translated text
     * @throws IOException potential exception if something is wrong
     */
    private static String translateTextBudgetVer(String langFrom, String langTo, String text) throws IOException {
        String urlStr = "https://script.google.com/macros/s/AKfycbyoLoNhi3R9QD-yIrdjXMYN_ltP1ctX1vhC_UQioQOMexfXlL3NQr4NUeLIzlPWTyil/exec" +
                "?q=" + URLEncoder.encode(text, "UTF-8") +
                "&target=" + langTo +
                "&source=" + langFrom +
                "&format=html";
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            return response.toString();
        } catch (Exception e) {
            System.out.println("An error occured in translate()");
            System.out.println(e.getMessage());
        }
        return response.toString();
    }
    
    /**
     * This method utilizes Google Cloud Translation-Basic service to translate the input text to the target language.
     * However, this does not support the glossary (keep target words untranslated) feature.
     * Use translateTextV3 instead.
     * @param Translator a Translate object
     * @param langFrom source language
     * @param langTo target language 
     * @param text to be translated
     * @param html if the text contains html entities to be preserved
     * @return the translated text
     */
    private static String translateTextV2(Translate Translator, String langFrom, String langTo, String text, boolean html) {
        translationCounter++;
        if (html) {
            return Translator.translate(
            text,
            Translate.TranslateOption.sourceLanguage(langFrom),
            Translate.TranslateOption.targetLanguage(langTo),
            Translate.TranslateOption.format("html"))
            .getTranslatedText();
        } else {
            return Translator.translate(
            text,
            Translate.TranslateOption.sourceLanguage(langFrom),
            Translate.TranslateOption.targetLanguage(langTo),
            Translate.TranslateOption.format("text"))
            .getTranslatedText();
        }
    }

    /**
     * This method utilizes Google Cloud Translation-Advanced service to translate the input text to the target language.
     * This uses the glossary feature to keep selected words untranslated
     * @param text to be translated
     * @param languageCode target language code
     * @param html if the text contains html entities to be preserved
     * @return the translated text
     */

    private static String translateTextV3(String languageFrom, String languageTo, String text, boolean HTML) {
        String res = "";
        if (text.length() > 30000) {
            int breakPoint = text.indexOf("<p>", 20000);
            return translateTextV3(languageFrom, languageTo, text.substring(0, breakPoint),HTML)
                + translateTextV3(languageFrom, languageTo, text.substring(breakPoint, text.length()),HTML);
        }
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            // Supported Mime Types: https://cloud.google.com/translate/docs/supported-formats
            TranslateTextRequest request;
            if (HTML)  {
                request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/html")
                    .setSourceLanguageCode(languageFrom) //set to English by default
                    .setTargetLanguageCode(languageTo) 
                    .addContents(text)
                    .setGlossaryConfig(glossaryConfig)
                    .build();
            } else {
                request = TranslateTextRequest.newBuilder()
                    .setParent(parent.toString())
                    .setMimeType("text/plain")
                    .setSourceLanguageCode(languageFrom) //set to English by default
                    .setTargetLanguageCode(languageTo) 
                    .addContents(text)
                    .setGlossaryConfig(glossaryConfig)
                    .build();
            }
            TranslateTextResponse response = client.translateText(request);
            translationCounter++;
            // Display the translation for each input text provided
            for (Translation translation : response.getGlossaryTranslationsList()) {
                res += translation.getTranslatedText();
            }
        } catch (Exception e) {
            System.out.println("Translation V3 error");
            e.printStackTrace();
        }
        return res;
    }

    private static void createGlossary(
      String projectId, String glossaryId, List<String> languageCodes){

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
            // Supported Locations: `global`, [glossary location], or [model location]
            // Glossaries must be hosted in `us-central1`
            // Custom Models must use the same location as your model. (us-central1)

            // Supported Languages: https://cloud.google.com/translate/docs/languages
            Glossary.LanguageCodesSet languageCodesSet =
                Glossary.LanguageCodesSet.newBuilder().addAllLanguageCodes(languageCodes).build();

            // Configure the source of the file from a GCS bucket
            GcsSource gcsSource = GcsSource.newBuilder().setInputUri(glossaryURL).build();
            GlossaryInputConfig inputConfig = GlossaryInputConfig.newBuilder().setGcsSource(gcsSource).build();

            Glossary glossary =
                Glossary.newBuilder()
                    .setName(glossaryName.toString())
                    .setLanguageCodesSet(languageCodesSet)
                    .setInputConfig(inputConfig)
                    .build();

            CreateGlossaryRequest request =
                CreateGlossaryRequest.newBuilder()
                    .setParent(parent.toString())
                    .setGlossary(glossary)
                    .build();

            // Start an asynchronous request
            OperationFuture<Glossary, CreateGlossaryMetadata> future =
                client.createGlossaryAsync(request);
            System.out.println("Creating Glossary...");
            Glossary response = future.get();
            System.out.println("Created Glossary.");
            System.out.printf("Glossary name: %s\n", response.getName());
            System.out.printf("Entry count: %s\n", response.getEntryCount());
            System.out.printf("Input URI: %s\n", response.getInputConfig().getGcsSource().getInputUri());
            System.out.printf("Language Code Set: %s\n", response.getLanguageCodesSet().getLanguageCodesList());
            
        } catch (Exception e) {
            System.out.println("Exception on createGlossary");
            e.printStackTrace();
        }
    }


        /**
         * Delete the target glossary. Method provided by Google.
         * @param projectId the project ID
         * @param glossaryId the glossary ID
         */
    private static void deleteGlossary(String projectId, String glossaryId) {
        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (TranslationServiceClient client = TranslationServiceClient.create()) {
        // Supported Locations: `global`, [glossary location], or [model location]
        // Glossaries must be hosted in `us-central1`
        // Custom Models must use the same location as your model. (us-central1)
            GlossaryName glossaryName = GlossaryName.of(projectId, "us-central1", glossaryId);
            DeleteGlossaryRequest request =
            DeleteGlossaryRequest.newBuilder().setName(glossaryName.toString()).build();

        // Start an asynchronous request
            System.out.format("Deleting Glossary...");
            OperationFuture<DeleteGlossaryResponse, DeleteGlossaryMetadata> future =
            client.deleteGlossaryAsync(request);
            DeleteGlossaryResponse response = future.get();
            System.out.format("Deleted Glossary: %s\n", response.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void updateGlossary(String projectId, String glossaryId, List<String> languageCodes) {
        deleteGlossary(projectId, glossaryId);
        createGlossary(projectId, glossaryId, languageCodes);
        glossaryConfig = TranslateTextGlossaryConfig.newBuilder().setGlossary(glossaryName.toString()).build();
    }
}