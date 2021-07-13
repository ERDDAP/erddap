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
    // to be used with the translator
    private static final String ERDDAPprojectId = "expanded-agent-317002";
    private static final String ERDDAPglossaryId = "ERDDAP-glossary";
    private static final String glossaryURL = "gs://erddap-glossary/erddap-glossary.csv";
    private static final String location = "us-central1";
    private static final LocationName parent = LocationName.of(ERDDAPprojectId, location);
    private static final GlossaryName glossaryName = GlossaryName.of(ERDDAPprojectId, location, ERDDAPglossaryId);
    private static TranslateTextGlossaryConfig glossaryConfig =
            TranslateTextGlossaryConfig.newBuilder().setGlossary(glossaryName.toString()).build();
    
    // path
    private static final String utilFolderPath = translate.class.getResource("").getPath().toString();
    private static final String translatedFolderPath = utilFolderPath + "translatedMessages/";
    private static final String messagePath = utilFolderPath + "messages.xml";
    private static final String oldMessagePath = translatedFolderPath + "messages-Copy.xml";
    
    //translation settings
    private static final String[] languageCodeList = {"zh-cn"};
    private static HashSet<String> doNotTranslateSet = new HashSet<String>(Arrays.asList(
        //* all tags that match the regular expresion:  <EDDGrid.*Example> ,
        "/EDDGridErddapUrlExample", "/EDDGridIdExample", "/EDDGridDimensionExample", "/EDDGridNoHyperExample", "/EDDGridDimNamesExample", "/EDDGridDataTimeExample", "/EDDGridDataValueExample",
        "/EDDGridDataIndexExample", "/EDDGridGraphExample", "/EDDGridMapExample", "/EDDGridMatlabPlotExample",
        //* all tags that match the regular expression:  <EDDTable.*Example> ,
        "/EDDTableErddapUrlExample", "/EDDTableIdExample", "/EDDTableVariablesExample", "/EDDTableConstraintsExample", "/EDDTableDataTimeExample", "/EDDTableDataValueExample",
        "/EDDTableGraphExample", "/EDDTableMapExample", "/EDDTableMatlabPlotExample",
        //URL, HTML, and standard names
        "/DEFAULT_commonStandardNames", "/palettes", "/pdfWidths", "/pdfHeights", "/questionMarkImageFile", "/signedToUnsignedAttNames", "/sparqlP01toP02pre", "/sparqlP01toP02post",
        "/startHeadHtml5", "/startBodyHtml5", "/standardizeUdunits", "/ucumToUdunits", "/udunitsToUcum", "/updateUrls",
        "/advr_dataStructure", "/advr_cdm_data_type", "/advr_class", "/inotifyFixCommands", 
        //abreviations
        "admKeywords", "advl_datasetID", "/extensionsNoRangeRequests", 
        // others
        "/legal", "/imageWidths", "/imageHeights", "/langCode"
    ));
    private static String[] messageFormatEntities = {"{0}", "{1}","''"};
    private static int translationCounter = 0;

    // watchlist: <convertKeywordsIntro> <doWithGraphs> <ConvertTimeNotes> </convertOceanicAtmosphericAcronymsService>
    // <convertOceanicAtmosphericAcronymsService> <convertInterpolateNotes> <convertUnitsComparison> </convertTimeNotes/ </theLongDescriptionHtml> </filesDocumentation>
    public static void main(String[] args) {
        String[] HTMLEntities = {"&lt;", "&gt;", "&quot;", "&amp;", "<p>", "<br>", "</a>"};
        //System.out.println("main() Working Directory = " + System.getProperty("user.dir"));
        //System.out.println(translate.class.getResource("").getPath().toString());
        
        // This function will detect the tags such that its tag name follows "<EDDGrid.*Example>" or "<EDDTable.*Example>"
        // and add the tags to the doNotTranslateSet
        // Currently not needed because I have found all tags following the pattern (as 6/23/21) and hard coded them to the set
        // addDoNotTranslateSet();

        // uncomment the try-catch statement below to begin the translation
        /*
        try {
            //preferablly the languageCodeList should be used as the 3rd parameter of updateGlossray(), but right now 
            //I'm testing the languages one by one, so languageCodeList contains few items, thus the parameter in updateGlossray() is hard-coded
            //updateGlossary(ERDDAPprojectId, ERDDAPglossaryId, Arrays.asList("en","zh-cn","de"));
            SimpleXMLReader xmlReader = getSimpleXMLReader(messagePath);
            //myWriter is a reader used solely for testing purposes
            //FileWriter myWriter = new FileWriter(translatedFolderPath + "messageTesting.xml");
            HashMap<String, String> previousMessageMap = getXMLTagMap(oldMessagePath);            
            HashMap<String, String>[] translatedTagMaps = (HashMap<String,String>[]) new HashMap[languageCodeList.length];

            for (int j = 0; j < languageCodeList.length; j++) {
                translatedTagMaps[j] = getXMLTagMap(translatedFolderPath + "messages-" + languageCodeList[j] + ".xml");
            }

            // add xml description
            // myWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            
            FileWriter[] fileWriters = new FileWriter[languageCodeList.length];
            for (int i = 0; i < languageCodeList.length; i++) {
                //2
                fileWriters[i] = new FileWriter(translatedFolderPath + "new-messages-" + languageCodeList[i] + ".xml");
                fileWriters[i].write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); 
            }
            //this for-loop tests the translation output of first 100 tags.
            //To translate all tags, make it a while(true) loop. The break command will be executed when there are no tags left
            for (int i = 0; i < 100; i++) {
                //testing what is in xmlReader
                xmlReader.nextTag();
                if (xmlReader.allTags().length() == 0) {
                    break;
                }
                String toTranslate = xmlReader.rawContent();
                String tagName = xmlReader.topTag();

                if (toTranslate.trim().equals("") || toTranslate.equals("") || toTranslate.equals("\n")) {
                    // empty content, do not waste api resources
                    //System.out.println(tagName + " has empty content");
                    //myWriter.write(toTranslate);
                    for (int j = 0; j < languageCodeList.length; j++) {
                        fileWriters[j].write(xmlReader.rawContent()); 
                    }
                } else if (doNotTranslateSet.contains(tagName)) {
                    // if the tag is one of the tags we do not want to translate
                    System.out.println(tagName + " is designed to not be translated");
                    //myWriter.write(toTranslate);
                    for (int j = 0; j < languageCodeList.length; j++) {
                        if (tagName.equals("/langCode")) {
                            //langCode is determined individually
                            fileWriters[j].write(languageCodeList[j]);
                        } else {
                            fileWriters[j].write(xmlReader.rawContent()); 
                        }
                    }
                } else {
                    boolean modified = !previousMessageMap.getOrDefault(tagName, "DNE").equals(xmlReader.rawContent());
                    boolean[] translated = new boolean[languageCodeList.length];
                    boolean html = Arrays.stream(HTMLEntities).anyMatch(toTranslate::contains);
                    boolean messageFormat = Arrays.stream(messageFormatEntities).anyMatch(toTranslate::contains);
                    for (int j = 0; j < translated.length; j++) {
                        translated[j] = !translatedTagMaps[j].getOrDefault(tagName, "DNE").equals("DNE");
                        if (!modified && translated[j]) {
                            fileWriters[j].write(previousMessageMap.get(tagName));
                        } else {
                            fileWriters[j].write(translateTag(toTranslate, languageCodeList[j], html, messageFormat));
                        }
                        //after writing the translated content, we write the <tag>
                        fileWriters[j].write("<" + tagName + ">");
                        if (xmlReader.isEndTag()) {
                            // if the tag is an end tag, we add a new line charcater at the end of the tag
                            //myWriter.write("\n");
                            fileWriters[j].write("\n"); 
                        }
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
            //myWriter.close();
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
     * Uses java's built in DOM to read tag information in XML. Only used in testing process.
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
     * also make the content detectable by the glossary (this step requires some hard-code)
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
     * @param original this method will prevent wrongly decoded "&amp;" in URLs by comparing the passed
     * in translated text with the original unmodified text.
     * @return the text ready to be written in the xml file
     */
    private static String rewriteHStr(String s, boolean messageFormat, String original) {
        String res = XML.decodeEntities(s);
        //revert the changes made in modifyRawContent
        res = res.replaceAll("<br />", "&nbsp;");
        res = res.replaceAll(" &newLinePlaceHolder;", "&newLinePlaceHolder;");
        res = res.replaceAll("&newLinePlaceHolder; ", "&newLinePlaceHolder;");
        res = res.replaceAll("&newLinePlaceHolder;", "\n");

        //remove an extra whitespace after <p>, <li>, <strong>, <br>, and before <img...
        res = res.replaceAll("<p> ", "<p>");
        res = res.replaceAll("<br> ", "<br>");
        res = res.replaceAll("<li> ", "<li>");
        res = res.replaceAll("<strong> ", "<strong>");
        res = res.replaceAll(" <img", "<img");
        


        //prevent the &amp; from being decoded in URLs of <a> tags
        StringBuffer sb = new StringBuffer(res);
        int originalAmpIndex = original.indexOf("&");
        int currAmpIndex = sb.indexOf("&", 0);
        while (originalAmpIndex != -1 && currAmpIndex != -1) {
            try {
                if (
                    !sb.substring(currAmpIndex, currAmpIndex + 5).equals("&amp;")
                    && original.substring(originalAmpIndex, originalAmpIndex + 5).equals("&amp;")
                ) {
                    sb.replace(currAmpIndex, currAmpIndex + 1, "&amp;");
                }
            } catch (Exception e) {
                System.out.println("errors on " + original);
                e.printStackTrace();
                break;
            }
            currAmpIndex = sb.indexOf("&", currAmpIndex + 1);
            originalAmpIndex = original.indexOf("&", originalAmpIndex + 1);
        }
        //remove the extra spaces around &quot;'s
        int quotCount = 0;
        int quotIndex = sb.indexOf("&quot;");
        while (quotIndex != -1) {
            if (quotCount % 2 == 0) {
                //first quote in quote pairs
                if (sb.substring(quotIndex, quotIndex + 7).equals("&quot; ")) {
                    sb.replace(quotIndex, quotIndex + 7, "&quot;");
                }
            } else {
                //second quote in quote pairs
                if (sb.substring(quotIndex - 1, quotIndex + 6).equals(" &quot;")) {
                    sb.replace(quotIndex - 1, quotIndex + 6, "&quot;");
                }
            }
            quotIndex = sb.indexOf("&quot;", quotIndex + 1);
            quotCount++;
        }
        //deal with message format issues
        res = sb.toString();
        if (messageFormat) {
            res = res.replaceAll("''", "doubleSingleQuotePlaceholder");
            res = res.replaceAll("'", "''");
            res = res.replaceAll("doubleSingleQuotePlaceholder", "''");
        }
        return res;
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
    @Deprecated
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
    @Deprecated
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

    /**
     * Method provided by Google to create a glossary
     * @param projectId Google Cloud Porject ID. Use the one stated at top of translate.java
     * @param glossaryId Glossary ID. Use the one stated at top of translate.java
     * @param languageCodes Language code in the glossray, order from left to right in the glossary.csv
     */
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
     * @param projectId The project ID. Use the one stated at top of translate.java.
     * @param glossaryId The glossary ID. Use the one stated at top of translate.java.
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
    /**
     * Update the glossary by deleting it, and recreate one with the data in the Google Cloud Bucket
     * Then modify the glossaryConfiguration
     * @param projectId Google Cloud Porject ID. Use the one stated at top of translate.java
     * @param glossaryId Glossary ID. Use the one stated at top of translate.java
     * @param languageCodes Language code in the glossray, order left to right
     */
    private static void updateGlossary(String projectId, String glossaryId, List<String> languageCodes) {
        deleteGlossary(projectId, glossaryId);
        createGlossary(projectId, glossaryId, languageCodes);
        glossaryConfig = TranslateTextGlossaryConfig.newBuilder().setGlossary(glossaryName.toString()).build();
    }

    
    /**
     * Translate the given tag. Only use this method when it's needed, i.e. tags not in doNotTranslateSet, or can be trimmed to be ""
     * @param rawContent rawContent of the given tag. Include CDATA and comment syntax.
     * @param tagName the name of the tag, without "<" and ">"
     * @param languageCode the languageCode of the targeted language
     * @param html if the tag contains HTML
     * @param messageFormat if the tag uses messageFormat
     * @return a translated string to write in place of the rawContent
     */
    private static String translateTag(String rawContent, String languageCode, boolean html, boolean messageFormat) {
        if (rawContent.startsWith("<!--")) {
            String toWrite = "";
            //if it's a comment, we need to remove the comment syntax and insert a new line character before the tag
            //also consider the possibility of multiple comments before one tag
            int commentStart = rawContent.indexOf("<!--") + 4;
            int commentEnd = rawContent.indexOf("-->");
            String comment = rawContent.substring(commentStart, commentEnd);
            // "\n<!--" + translatedText + "-->\n" will be the full translated comment
            //myWriter.write("\n<!--" + comment + "-->\n");
            while (commentStart != 3) {
                comment = rawContent.substring(commentStart, commentEnd);
                String res;
                if (html) {
                    res = modifyRawContent(comment);
                    res = "\n<!--"
                    + translateTextV3("en", languageCode, res, html) 
                    + "-->";
                    res = rewriteHStr(res, false, comment);
                } else {
                    res = "\n<!--"
                    + translateTextV3("en", languageCode, comment, html) 
                    + "-->";
                }
                /* we do not need deal with messageFormat in comments.
                if (messageFormat) {
                    res = res.replaceAll("''", "doubleQuotePlaceholder");
                    res = res.replaceAll("'", "''");
                    res = res.replaceAll("doubleQuotePlaceholder", "''");
                }
                */
                toWrite += res;
                commentStart = rawContent.indexOf("<!--", commentEnd) + 4;
                commentEnd = rawContent.indexOf("-->", commentStart);
            }
            //newline before next tag
            toWrite += "\n";
            return toWrite;
        } else if (rawContent.startsWith("<![CDATA[")) {
            //CDATA tag    
            //myWriter.write(toTranslate);
            String res = rawContent.substring(9, rawContent.length() - 3);
            res = modifyRawContent(res);
            res = translateTextV3("en", languageCode, res, true);
            res = rewriteHStr(res, messageFormat, rawContent);

            if (messageFormat) {
                res = res.replaceAll("''", "doubleQuotePlaceholder");
                res = res.replaceAll("'", "''");
                res = res.replaceAll("doubleQuotePlaceholder", "''");
            }
            return "<![CDATA[" + res + "]]>";
        } else {
            String res;
            if (html) {
                res = modifyRawContent(rawContent);
                res = translateTextV3("en", languageCode, res, true);
                res = rewriteHStr(res, false, rawContent);
            } else {
                res = translateTextV3("en", languageCode, rawContent, false);
            }
            if (messageFormat) {
                res = res.replaceAll("''", "doubleQuotePlaceholder");
                res = res.replaceAll("'", "''");
                res = res.replaceAll("doubleQuotePlaceholder", "''");
            }
            return res;
        }
    }
}
