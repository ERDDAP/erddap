package gov.noaa.pfel.erddap.util;

import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.XML;
import com.google.cloud.translate.v3.GlossaryName;
import com.google.cloud.translate.v3.LocationName;
import com.google.cloud.translate.v3.TranslateTextGlossaryConfig;
import com.google.cloud.translate.v3.TranslateTextRequest;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.Translation;
import com.google.cloud.translate.v3.TranslationServiceClient;
import com.google.cloud.translate.v3.TranslationServiceSettings;
import com.google.common.io.Resources;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class translates messages.xml into other languages, e.g., messages-de.xml. See the important
 * documentation links at the bottom of
 * https://cloud.google.com/translate/docs/advanced/quickstart#translate_v3_translate_text.java
 * notably https://cloud.google.com/translate/troubleshooting
 *
 * <p>The initial work was by Qi Z, who worked on this for the Google Summer of Code (GSoC) in 2021.
 * Bob Simons then extensively revised that.
 *
 * <p>Instruction for Translating From Qi's 2021-09-29 email:
 *
 * <p>I followed the Google Cloud Translation Setup Guide at
 * https://cloud.google.com/translate/docs/setup . I will try to summarize the steps as below:
 *
 * <p>1. Set up a Google account and activate the Google Cloud Services at https://cloud.google.com
 * . Also set up billing information. [Bob set this up with personal gmail account, since NOAA
 * account doesn't allow it(!) ]
 *
 * <p>2. In the Google Cloud Console, create a new Google Cloud project. Navigate to the "APIs" then
 * the "library" tab under the new project, search for "Cloud Translation API" and enable it. [Bob's
 * project: erddap-noaa-erd ]
 *
 * <p>3. Create a new service account for the project. In the Cloud Console, go to the Create
 * service account page. It will ask you which project you want to create a service account for, and
 * you want to select the project you just created. Enter the service account details. I personally
 * chose "owner" as the "service account role", but some other roles should also work fine. [Bob's
 * service account: translate@erddap-noaa-erd.iam.gserviceaccount.com ]
 *
 * <p>4. Select this service account and create a new key. Download the .json key file. Put this
 * .json in your environment variable settings. Detailed Description for step 3 and 4 can be found
 * on https://cloud.google.com/translate/docs/setup#windows (This guide is for computers with a
 * Windows system.) [Bob used instructions at
 * https://phoenixnap.com/kb/windows-set-environment-variable but that didn't work. So I ran from a
 * DOS window: set GOOGLE_APPLICATION_CREDENTIALS=C:\Users\BobSi\erddap-noaa-erd-5941b340777e.json
 * but that didn't work. (Both set the environment variable, but the code still gave an error
 * indicating the credentials were missing.) So now I set via a file reference in translateClient()
 * below. That works. In Credentials, I created an API key (restricted to my ip address and
 * translation api) then set GOOGLE_API_KEY=myKey Qi did this. I don't know if it is necessary.]
 *
 * <p>[Bob Added: 4a. Go to Google Cloud Console > IAM Add the service account email address (e.g.,
 * translate@erddap-noaa-erd.iam.gserviceaccount.com ) from client-email in my json credentials file
 * as a principal for the project and "Add"ed roles: Cloud Translation API Admin, Cloud Translation
 * API Editor, and Cloud Translation API User. This solves IAM permissions errors. This is based on
 * 1st and 2nd answer at
 * https://stackoverflow.com/questions/52332247/permissiondenied-403-iam-permission-dialogflow-intents-list
 * ]
 *
 * <p>After step 1-4 we have set up the Google Cloud Project and account. You can try to run some
 * simple translations to ensure that you have set up the API and keys properly. Example snippets
 * can be found at https://cloud.google.com/translate/docs/samples/translate-v3-translate-text.
 *
 * <p>5. Update googleProjectId, etc below. [Bob: done ]
 *
 * <p>6. Update languageCodeList field to be an array of "en", plus the target language codes of the
 * translation. [Bob: done]
 *
 * <p>Now the translation system is ready to use. The Google Cloud account has been set up, and the
 * target languages are chosen, and we have set the output path. We can make final modifications in
 * this class and run main()!
 *
 * <p>7. TranslateMessages.translate() will output translated messages.xml, each named
 * "new-messages-<languageCode>.xml", the <languageCode> is the language code in that .xml file. To
 * reuse the translated messages-langCode.xml to reduce the cost, you should rename
 * "new-messages-<languageCode>.xml" to "messages-<languageCode>.xml", so when you run main() next
 * time, it will reuse the current translation.
 *
 * <p>NOTES These are machine translations, and so are inherently imperfect. The use of ERDDAP and
 * domain-related jargon makes it even harder (although we do use a glossary for that type of
 * problem). Most of the translated text hasn't even be read/proofed by a human. This doesn't
 * attempt to translate messages from lower level code, e.g., PrimitiveArray, String2, Math2,
 * HtmlWidgets, MustBe. You would have to add a complex system of setting arrays for each stock
 * error message and then passing languageCode into to each method so if an error occurred, the
 * translated message would be generated. But even that is trouble if a mid-level procedure looks
 * for a specific static error message. Even some ERDDAP-related things like accessibleVia... are
 * just English. Most error messages are just English, although the start of the message, e.g.,
 * "Query Error" may appear translated and in English. Several tags used on the 3rd(?) page of the
 * Data Provider Form (e.g., dpt_standardName) maybe shouldn't be translated since they refer to a
 * specific CF or ACDD attribute (and the explanation is translated). Several tags can be specified
 * in datasets.xml (e.g., &lt;startHeadHtml5&gt;, &lt;standardLicense&lt;). The definitions there
 * only affect the English language version. This is not ideal. Many tags get translated by this
 * system but only the English version is used, e.g., advl_datasetID. See "NOT TRANSLATED" in
 * EDStatic.java for most of these. Language=0 must be English ("en"). Many places in the code
 * specify EDStatic....Ar[0] so that only the English version of the tag is used.
 *
 * <p>FUTURE? Things to think about. Make it so that, after running Translate, all changes for each
 * language are captured separately, so an editor for that language can review them. Disable/change
 * the system for removing space after odd '"' and before even '"'? Or just fix problems in tags
 * where it has problems (usually some root cause). Need to note max line length in non-html tags?
 * Then apply to translated text? There is no dontTranslate for non-html. Is it needed? How do it?
 * Do more testing of non-html, e.g., {0}, line breaks, etc. Is there a way to force <submit> to be
 * the computer-button meaning? E.g., I think German should be Senden (not "einreichen"). Should the
 * language list be the translated language names? Translate EDStatic.errorFromDataSource and use it
 * with bilingual()?
 */
public class TranslateMessages {
  private static boolean debugMode = true;
  private static String credentialsFileName =
      "C:\\Users\\BobSi\\erddap-noaa-erd-5941b340777e.json"; // for windows, use backslashes
  private static TranslationServiceClient translationClient = null;

  // THIS IS IRRELEVANT, because this class no longer uses the glossary.
  protected static String googleProjectId = "erddap-noaa-erd";
  private static String glossaryId = "ERDDAP-glossary";
  private static String glossaryBucket = "bob-erddap-glossary";
  private static String glossaryURL = "gs://" + glossaryBucket + "/erddap-glossary.csv"; // bucket
  // !!! Use glossaryLocation="global" to not use our glossary. If using ours, Google Translate
  // requires us-central1.
  private static String glossaryLocation = "us-central1";
  private static LocationName parent = LocationName.of(googleProjectId, glossaryLocation);
  private static GlossaryName glossaryName =
      null; // was GlossaryName.of(googleProjectId, glossaryLocation, glossaryId);
  private static TranslateTextGlossaryConfig glossaryConfig = null;

  // was TranslateTextGlossaryConfig.newBuilder().setGlossary(glossaryName.toString()).build();

  /** This is the order the languages will appear in the list shown to users in ERRDDAP */
  public static String[] languageList = {
    // arbitrarily selected from most commonly used:
    // https://www.visualcapitalist.com/100-most-spoken-languages/
    // Chinese-CN=Simplified  Chinese-TW=Traditional -- I wanted shorter names
    // I got main Indian languages, but not all.
    // I got few of the Eastern European languages.
    // Google supports Tagalog but not Filipino (the official language)
    // Arabic is hard. (right to left etc)  Translated bits seemed out-of-order.
    // in English:
    "English",
    "Bengali",
    "Chinese-CN",
    "Chinese-TW",
    "Czech",
    "Danish",
    "Dutch",
    "Finnish",
    "French",
    "German",
    "Greek",
    "Gujarati",
    "Hindi",
    "Hungarian",
    "Indonesian",
    "Irish",
    "Italian",
    "Japanese",
    "Korean",
    "Marathi",
    "Norwegian",
    "Polish",
    "Portuguese",
    "Punjabi",
    "Romanian",
    "Russian",
    "Spanish",
    "Swahili",
    "Swedish",
    "Tagalog",
    "Thai",
    "Turkish",
    "Ukrainian",
    "Urdu",
    "Vietnamese"
  };

  public static String[] languageCodeList = {
    // list of available languages: https://cloud.google.com/translate/docs/languages
    "en",
    "bn",
    "zh-CN",
    "zh-TW",
    "cs",
    "da",
    "nl",
    "fi",
    "fr",
    "de",
    "el",
    "gu",
    "hi",
    "hu",
    "id",
    "ga",
    "it",
    "ja",
    "ko",
    "mr",
    "no",
    "pl",
    "pt",
    "pa",
    "ro",
    "ru",
    "es",
    "sw",
    "sv",
    "tl",
    "th",
    "tr",
    "uk",
    "ur",
    "vi"
  };

  // For testing with just one language, use these mini lists.
  // To do all languages, comment these out.
  static {
    // languageList     = new String[]{"English", "German"};
    // languageCodeList = new String[]{"en",      "de"};
  }

  // path
  public static String utilDir = "gov/noaa/pfel/erddap/util/";
  public static URL translatedMessagesDir = Resources.getResource(utilDir + "translatedMessages/");
  protected static URL messagesXmlFileName = Resources.getResource(utilDir + "messages.xml");
  private static URL oldMessagesXmlFileName = Resources.getResource(utilDir + "translatedMessages/messagesOld.xml");

  // translation settings
  private static HashSet<String> doNotTranslateSet =
      new HashSet<String>(
          Arrays.asList(
              // These are tags that shouldn't be translated, minus the surrounding < and >.
              // * all tags that match the regular expresion:  <EDDGrid.*Example> ,
              "/EDDGridErddapUrlExample",
              "/EDDGridIdExample",
              "/EDDGridDimensionExample",
              "/EDDGridNoHyperExample",
              "/EDDGridDimNamesExample",
              "/EDDGridDataTimeExample",
              "/EDDGridDataValueExample",
              "/EDDGridDataIndexExample",
              "/EDDGridGraphExample",
              "/EDDGridMapExample",
              "/EDDGridMatlabPlotExample",
              // * all tags that match the regular expression:  <EDDTable.*Example> ,
              "/EDDTableErddapUrlExample",
              "/EDDTableIdExample",
              "/EDDTableVariablesExample",
              "/EDDTableConstraintsExample",
              "/EDDTableDataTimeExample",
              "/EDDTableDataValueExample",
              "/EDDTableGraphExample",
              "/EDDTableMapExample",
              "/EDDTableMatlabPlotExample",
              // Other
              "/admKeywords",
              "/admSubsetVariables",
              "/advl_datasetID",
              "/advr_dataStructure",
              "/advr_cdm_data_type",
              "/advr_class",
              "/DEFAULT_commonStandardNames",
              "/EDDIso19115",
              "/EDDTableFromHttpGetDatasetDescription",
              "/EDDTableFromHttpGetAuthorDescription",
              "/EDDTableFromHttpGetTimestampDescription",
              "/extensionsNoRangeRequests",
              "/htmlTableMaxMB",
              "/imageWidths",
              "/imageHeights",
              "/inotifyFixCommands",
              "/langCode",
              "/legal",
              "/palettes",
              "/pdfWidths",
              "/pdfHeights",
              "/questionMarkImageFile",
              "/signedToUnsignedAttNames",
              "/sparqlP01toP02pre",
              "/sparqlP01toP02post",
              "/standardizeUdunits",
              "/startBodyHtml5",
              "/startHeadHtml5",
              "/theShortDescriptionHtml",
              "/ucumToUdunits",
              "/udunitsToUcum",
              "/updateUrls",
              // don't translate the comment before these opening tags (hence, no leading '/')
              "admKeywords",
              "advl_datasetID",
              "startBodyHtml5"));
  private static String[] messageFormatEntities = {"{0}", "{1}", "''"};
  protected static String[] HTMLEntities = {
    "<p>", "<br>", "</a>", "<kbd>", "<strong>", "<li>"
  }; // 2021-12-21 was also "&lt;", "&gt;", "&quot;", "&amp;", but they are used in plain text in
  // xml
  private static int translationCounter =
      0; // should be in translate() but easier to have it here and static
  private static String verboseLanguage =
      ""; // should be in translate() but easier to have it here and static

  // This is the super important system for identifying text not to be translated.
  // Qi (glossary) and I (&term; and &trm;) fiddled around with other systems,
  // but they fail when translating different languages.
  // But this web page https://cloud.google.com/translate/troubleshooting
  // identifies the proper way to do it.
  // See their use in preProcessHtml() and postProcessHtml().
  private static final String START_SPAN = "<span translate=\"no\">";
  private static final String STOP_SPAN = "</span>";
  protected static final String[] dontTranslate = {
    // !!!ESSENTIAL: if a short phrase (DAP) is in a long phrase (ERDDAP), the long phrase must come
    // first.
    // main() below has a test for this.
    // phrases in quotes
    "\" since \"",
    "\"{0}\"",
    "\"{1}\"",
    "\"{count}\"",
    "\"**\"",
    "\"[in_i]\"",
    "\"[todd'U]\"",
    "\"%{vol}\"",
    "\"&amp;units=...\"", // &amp; was converted to &amp;amp;   //Qi did this. Bob doesn't.
    "\"&C;\"",
    "\"&micro;\"", // otherwise it is often dropped from the translation.   Only used in one place
    // in messages.xml.
    "\"BLANK\"",
    "\"c/s\"",
    "\"CA, Monterey\"",
    "\"Cel\"",
    "\"Coastlines\"",
    "\"comment\"",
    "\"content-encoding\"",
    "\"count\"",
    "\"days since Jan 1, 1900\"",
    "\"deg\"",
    "\"deg{north}\"",
    "\"degree\"",
    "\"degree_north\"",
    "\"extended\"",
    "\"farad\"",
    "\"files\"",
    "\"gram\"",
    "\"hours since 0001-01-01\"",
    "\"import\"",
    "\"institution\"",
    "\"J\"",
    "\"joule\"",
    "\"joules\"",
    "\"kg.m2.s-2\"",
    "\"kilo\"",
    "\"LakesAndRivers\"",
    "\"Land\"",
    "\"last\"",
    "\"Linear\"",
    "\"Log\"",
    "\"log\"",
    "\"long_name\"",
    "\"m s-1\"",
    "\"m.s^-1\"",
    "\"meter per second\"",
    "\"meters/second\"",
    "\"mo_g\"",
    "\"months since 1970-01-01\"",
    "\"months since\"",
    "\"Nations\"",
    "\"per\"",
    "\"PER\"",
    "\"Range\"",
    "\"s{since 1970-01-01T00:00:00Z}\"",
    "\"Sea Surface Temperature\"",
    "\"searchFor=wind%20speed\"",
    "\"seconds since\"",
    "\"seconds since 1970-01-01\"",
    "\"seconds since 1970-01-01T00:00:00Z\"",
    "\"since\"",
    "\"SOS\"",
    "\"sos\"",
    "\"sst\"",
    "\"States\"",
    "\"stationID,time/1day,10\"",
    "\"time\"",
    "\"times\"",
    "\"times 1000\"",
    "\"title\"",
    "\"years since\"",
    "'u'",
    "'/'",
    "'*'",
    "'^'",
    "'='",

    // lots of things that shouldn't be translated are within <kbd> and <strong>
    // (and Google Translate German doesn't handle <kbd> and <strong> well/correctly)
    "<kbd>\"long_name=Sea Surface Temperature\"</kbd>",
    "<kbd>{0} : {1}</kbd>",
    "<kbd>{0}</kbd>",
    "<kbd>{1} : {2}</kbd>",
    "<kbd>{41008,41009,41010}</kbd>",
    "<kbd>#1</kbd>",
    // there are some <kbd>&pseudoEntity;</kbd>  The translation system REQUIRES that "pseudoEntity"
    // only use [a-zA-Z0-9].
    "<kbd>&adminEmail;</kbd>",
    // &amp; was converted to &amp;amp;   //Qi did this. Bob doesn't.
    "<kbd>&amp;addVariablesWhere(\"<i>attName</i>\",\"<i>attValue</i>\")</kbd>",
    "<kbd>&amp;</kbd>",
    "<kbd>&amp;stationID%3E=%2241004%22</kbd>",
    "<kbd>&amp;stationID&gt;=\"41004\"</kbd>",
    "<kbd>&amp;time&gt;now-7days</kbd>",
    "<kbd>&amp;units(\"UDUNITS\")</kbd>",
    "<kbd>&amp;units(\"UCUM\")</kbd>",
    "<kbd>&category;</kbd>",
    "<kbd>&lt;att name=\"units\"&gt;days since -4712-01-01T00:00:00Z&lt;/att&gt;</kbd>",
    "<kbd>&lt;units_standard&gt;</kbd>",
    "<kbd>&quot;wind speed&quot;</kbd>",
    "<kbd>&quot;datasetID=<i>erd</i>&quot;</kbd>",
    "<kbd>&safeEmail;</kbd>",
    "<kbd>&searchButton;</kbd>",
    "<kbd>(last)</kbd>",
    "<kbd>(unknown)</kbd>",
    "<kbd>--compressed</kbd>",
    "<kbd>-999</kbd>",
    "<kbd>-g</kbd>",
    "<kbd>-o <i>fileDir/fileName.ext</i></kbd>",
    "<kbd>-<i>excludedWord</i></kbd>",
    "<kbd>-&quot;<i>excluded phrase</i>&quot;</kbd>",
    "<kbd>01</kbd>",
    "<kbd>2014</kbd>",
    "<kbd>2020-06-12T06:17:00Z</kbd>",
    "<kbd><i>attName</i>=<i>attValue</i></kbd>",
    "<kbd><i>attName=attValue</i></kbd>",
    "<kbd><i>erddapUrl</i></kbd>",
    "<kbd><i>units</i> since <i>basetime</i></kbd>",
    "<kbd>air_pressure</kbd>",
    "<kbd>algorithm</kbd>",
    "<kbd>altitude</kbd>",
    "<kbd>attribute=value</kbd>",
    "<kbd>AND</kbd>",
    "<kbd>Back</kbd>",
    "<kbd>Bilinear</kbd>",
    "<kbd>Bilinear/4</kbd>",
    "<kbd>bob dot simons at noaa dot gov</kbd>",
    "<kbd>boolean</kbd>",
    "<kbd>Bypass this form</kbd>",
    "<kbd>byte</kbd>",
    "<kbd>Cel</kbd>",
    "<kbd>CMC0.2deg-CMC-L4-GLOB-v2.0</kbd>",
    "<kbd>cmd</kbd>",
    "<kbd>count</kbd>",
    "<kbd>curl --compressed \"<i>erddapUrl</i>\" -o <i>fileDir/fileName#1.ext</i></kbd>",
    "<kbd>curl --compressed -g \"<i>erddapUrl</i>\" -o <i>fileDir/fileName.ext</i></kbd>",
    "<kbd>datasetID</kbd>",
    "<kbd>datasetID/variable/algorithm/nearby</kbd>",
    "<kbd>days since 2010-01-01</kbd>",
    "<kbd>deflate</kbd>",
    "<kbd>degC</kbd>",
    "<kbd>degF</kbd>",
    "<kbd>degK</kbd>",
    "<kbd>degree_C</kbd>",
    "<kbd>degree_F</kbd>",
    "<kbd>degrees_east</kbd>",
    "<kbd>degrees_north</kbd>",
    "<kbd>depth</kbd>",
    "<kbd>double</kbd>",
    "<kbd>File : Open</kbd>",
    "<kbd>File : Save As</kbd>",
    "<kbd>File Type</kbd>",
    "<kbd>File type</kbd>",
    "<kbd>float</kbd>",
    "<kbd>fullName=National Oceanic and Atmospheric Administration</kbd>",
    "<kbd>fullName=National%20Oceanic%20and%20Atmospheric%20Administration</kbd>",
    "<kbd>graph</kbd>",
    "<kbd>Graph Type</kbd>",
    "<kbd>Grid</kbd>",
    "<kbd>HTTP 404 Not-Found</kbd>",
    "<kbd>https://spray.ucsd.edu</kbd>",
    "<kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>", // &amp; was
    // converted
    // to
    // &amp;amp;
    //   //Qi
    // did this.
    // Bob
    // doesn't.
    "<kbd>Identifier</kbd>",
    "<kbd>In 8x</kbd>",
    "<kbd>InverseDistance2</kbd>",
    "<kbd>InverseDistance4</kbd>",
    "<kbd>InverseDistance6</kbd>",
    "<kbd>InverseDistance</kbd>",
    "<kbd>int</kbd>",
    "<kbd>John Smith</kbd>",
    "<kbd>jplMURSST41/analysed_sst/Bilinear/4</kbd>",
    "<kbd>jplMURSST41_analysed_sst_Bilinear_4</kbd>",
    "<kbd>Just generate the URL</kbd>",
    "<kbd>keywords</kbd>",
    "<kbd>last</kbd>",
    // "<kbd>(last)</kbd>" is above
    "<kbd>latitude</kbd>",
    "<kbd>Location</kbd>",
    "<kbd>long</kbd>",
    "<kbd>longitude</kbd>",
    "<kbd>maximum=37.0</kbd>",
    "<kbd>mean</kbd>",
    "<kbd>Mean</kbd>",
    "<kbd>Median</kbd>",
    "<kbd>minimum=32.0</kbd>",
    "<kbd>NaN</kbd>",
    "<kbd>nearby</kbd>",
    "<kbd>Nearest</kbd>",
    "<kbd>No animals were harmed during the collection of this data.</kbd>",
    "<kbd>NOAA NMFS SWFSC</kbd>",
    "<kbd>now-7days</kbd>",
    "<kbd>Ocean Color</kbd>",
    "<kbd>org.ghrsst</kbd>",
    "<kbd>Other</kbd>",
    "<kbd>Point</kbd>",
    "<kbd>Profile</kbd>",
    "<kbd>protocol=griddap</kbd>",
    "<kbd>protocol=tabledap</kbd>",
    "<kbd>Redraw the Graph</kbd>",
    "<kbd>Refine ...</kbd>",
    "<kbd>Scaled</kbd>",
    "<kbd>SD</kbd>",
    "<kbd>sea_water_temperature</kbd>",
    "<kbd>short</kbd>",
    "<kbd>Simons, R.A. 2022. ERDDAP. https://coastwatch.pfeg.noaa.gov/erddap . Monterey, CA: NOAA/NMFS/SWFSC/ERD.</kbd>",
    "<kbd>spee</kbd>",
    "<kbd>speed</kbd>",
    "<kbd>Spray Gliders, Scripps Institution of Oceanography</kbd>",
    "<kbd>[standard]</kbd>",
    "<kbd>STANDARDIZE_UDUNITS=<i>udunitsString</i></kbd>",
    "<kbd>Start:Stop</kbd>",
    "<kbd>Start:Stride:Stop</kbd>",
    "<kbd>Start</kbd>",
    "<kbd>Stop</kbd>",
    "<kbd>Stride</kbd>",
    "<kbd>String</kbd>",
    "<kbd>Submit</kbd>",
    "<kbd>Subset</kbd>",
    "<kbd>Taxonomy</kbd>",
    "<kbd>testOutOfDate</kbd>",
    "<kbd>text=<i>some%20percent-encoded%20text</i></kbd>",
    "<kbd>Time</kbd>",
    "<kbd>time</kbd>",
    "<kbd>time&gt;now-2days</kbd>",
    "<kbd>time&gt;max(time)-2days</kbd>",
    "<kbd>timestamp</kbd>",
    "<kbd>TimeSeries</kbd>",
    "<kbd>TimeSeriesProfile</kbd>",
    "<kbd>title=Spray Gliders, Scripps Institution of Oceanography</kbd>",
    "<kbd>Trajectory</kbd>",
    "<kbd>TrajectoryProfile</kbd>",
    "<kbd>true</kbd>",
    "<kbd>UCUM=<i>ucumString</i></kbd>",
    "<kbd>units=degree_C</kbd>",
    // "<kbd><i>units</i> since <i>basetime</i></kbd>" is above
    "<kbd>Unknown</kbd>",
    "<kbd>URL/action</kbd>",
    "<kbd>variable</kbd>",
    "<kbd>view the URL</kbd>",
    "<kbd>Water Temperature</kbd>",
    "<kbd>waterTemp</kbd>",
    "<kbd>WindSpeed</kbd>",
    "<kbd>wt</kbd>",
    "<kbd>your.name@yourOrganization.org</kbd>",
    "<kbd>yyyy-MM-ddTHH:mm:ssZ</kbd>",
    "<pre>curl --compressed -g \"https://coastwatch.pfeg.noaa.gov/erddap/files/cwwcNDBCMet/nrt/NDBC_41008_met.nc\" -o ndbc/41008.nc</pre>",
    "<pre>curl --compressed \"https://coastwatch.pfeg.noaa.gov/erddap/files/cwwcNDBCMet/nrt/NDBC_{41008,41009,41010}_met.nc\" -o ndbc/#1.nc</pre>",

    // All psuedo entities (used for param names, proper nouns, substitutions)
    //  MUST be here by themselves
    //  OR in <kbd>&pseudoEntity;</kbd> above
    //  so code in postProcessHtml works correctly.
    // postProcessHtml() REQUIRES that "pseudoEntity" only use [a-zA-Z0-9].
    "&acceptEncodingHtml;",
    "&acceptEncodingHtmlh3tErddapUrl;",
    "&adminContact;",
    "&advancedSearch;",
    "&algorithm;",
    "&bgcolor;",
    "&BroughtToYouBy;",
    "&C;",
    // above is <kbd>&category;</kbd>
    "&convertTimeReference;",
    "&cookiesHelp;",
    "&dataFileTypeInfo1;",
    "&dataFileTypeInfo2;",
    "&descriptionUrl;",
    "&datasetListRef;",
    "&e0;",
    "&EasierAccessToScientificData;",
    "&elevation;",
    "&encodedDefaultPIppQuery;",
    "&erddapIs;",
    "&erddapUrl;",
    "&erddapVersion;",
    "&exceptions;",
    "&externalLinkHtml;",
    "&F;",
    "&FALSE;",
    "&format;",
    "&fromInfo;",
    "&g;",
    "&griddapExample;",
    "&headingType;",
    "&height;",
    "&htmlQueryUrl;",
    "&htmlQueryUrlWithSpaces;",
    "&htmlTooltipImage;",
    "&info;",
    "&initialHelp;",
    "&jsonQueryUrl;",
    "&langCode;",
    "&language;",
    "&layers;",
    "&license;",
    "&likeThis;",
    "&loginInfo;",

    // these <tag>s were gathered by code in main that matches a regex in messages.xml
    "&lt;/att&gt;",
    "&lt;addAttributes&gt;",
    "&lt;subsetVariables&gt;",
    "&lt;time_precision&gt;",
    "&lt;units_standard&gt;",
    "&lt;updateUrls&gt;",
    "&makeAGraphListRef;",
    "&makeAGraphRef;",
    "&niceProtocol;",
    "&NTU;",
    "&offerValidMinutes;",
    "&partNumberA;",
    "&partNumberB;",
    "&plainLinkExamples1;",
    "&plainLinkExamples2;",
    "&plainLinkExamples3;",
    "&plainLinkExamples4;",
    "&plainLinkExamples5;",
    "&plainLinkExamples6;",
    "&plainLinkExamples7;",
    "&plainLinkExamples8;",
    "&protocolName;",
    "&PSU;",
    "&requestFormatExamplesHtml;",
    "&requestGetCapabilities;",
    "&requestGetMap;",
    "&resultsFormatExamplesHtml;",
    // above is <kbd>&safeEmail;</kbd>
    "&sampleUrl;",
    "&secondPart;",
    // above is <kbd>&searchButton;</kbd>
    "&serviceWMS;",
    "&sheadingType;",
    "&ssUse;",
    "&standardLicense;",
    "&styles;",
    "&subListUrl;",
    "&tabledapExample;",
    "&tagline;",
    "&tEmailAddress;",
    "&tErddapUrl;",
    "&time;",
    "&transparentTRUEFALSE;",
    "&TRUE;",
    "&tTimestamp;",
    "&tWmsGetCapabilities130;",
    "&tWmsOpaqueExample130Replaced;",
    "&tWmsOpaqueExample130;",
    "&tWmsTransparentExample130Replaced;",
    "&tWmsTransparentExample130;",
    "&tYourName;",
    "&unitsStandard;",
    "&variable;",
    "&version;",
    "&versionLink;",
    "&versionResponse;",
    "&versionStringLink;",
    "&versionStringResponse;",
    "&widgetEmailAddress;",
    "&widgetFrequencyOptions;",
    "&widgetGriddedOptions;",
    "&widgetSelectGroup;",
    "&widgetSubmitButton;",
    "&widgetTabularOptions;",
    "&widgetYourName;",
    "&width;",
    "&wmsVersion;",
    "&wmsManyDatasets;",
    "&WMSSEPARATOR;",
    "&WMSSERVER;",

    // &amp;#37;,  //Bob doesn't understand why Qi did this. #37='7'
    // see test at bottom. German loses &sum; but everything else is okay
    //        "<br>&bull;",
    //        "&bull;",
    //        "&gt;",
    //        "&lt;",
    //        "&mdash;",
    //        "&micro;",
    //        "&middot;",
    //        "&ndash;",
    //        "&plusmn;",
    //        "&rarr;",
    //        "&sum;",   //the one equation using &sum; (which is trouble in German) is handled
    // below

    // things that are never translated
    "{ }",
    "{east}",
    "{north}",
    "{NTU}",
    "{PSU}",
    "{true}",
    "{west}",
    "( )",
    "(Davis, 1986, eq 5.67, page 367)",
    "(Nephelometric Turbidity Unit)",
    "(OPeN)DAP",
    "(Practical Salinity Units)",
    "[ ]",
    "[standardContact]",
    "[standardDataLicenses]",
    "[standardDisclaimerOfEndorsement]",
    "[standardDisclaimerOfExternalLinks]",
    "[standardPrivacyPolicy]",
    "[standardShortDescriptionHtml]",
    "@noaa.gov",
    ".bz2",
    ".fileType",
    ".gzip",
    ".gz",
    ".hdf",
    ".htmlTable",
    ".itx",
    ".jsonlCSV1",
    ".jsonlCSV", // must be after the .jsonlCSV1
    ".jsonlKVP",
    ".json", // must be after the longer versions
    ".kml",
    ".mat",
    ".nccsv",
    ".nc", // must be after .nccsv
    ".tar",
    ".tgz",
    ".tsv",
    ".xhtml",
    ".zip",
    ".z",

    // text (proper nouns, parameter names, phrases, etc) that shouldn't be translated
    "1230768000 seconds since 1970-01-01T00:00:00Z",
    "2452952 \"days since -4712-01-01\"",
    "2009-01-21T23:00:00Z",
    "60000=AS=AMERICA SAMOA",
    "64000=FM=FEDERATED STATES OF MICRONESIA",
    "66000=GU=GUAM",
    "68000=MH=MARSHALL ISLANDS",
    "69000=MP=NORTHERN MARIANA ISLANDS",
    "70000=PW=PALAU",
    "72000=PR=PUERTO RICO",
    "74000=UM=U.S. MINOR OUTLYING ISLANDS",
    "78000=VI=VIRGIN ISLANDS OF THE UNITED STATES",
    "AJAX",
    "algorithm=Nearest",
    "algorithms for oligotrophic oceans: A novel approach",
    "allDatasets",
    "ArcGIS for Server",
    "ArcGIS",
    "Ardour",
    "Audacity",
    "Awesome ERDDAP",
    "based on three-band reflectance difference, J. Geophys.",
    "beginTime",
    "bob dot simons at noaa dot gov",
    "bob.simons at noaa.gov",
    "C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a",
    "categoryAttributes",
    "centeredTime",
    "Chronological Julian Dates (CJD)",
    "COARDS",
    "colorBarMaximum",
    "colorBarMinimum",
    "Conda",
    "content-encoding",
    "curl",
    // "DAP",  is below, after OPeNDAP
    "d, day, days,",
    "datasetID/variable/algorithm/nearby", // before datasetID
    "datasetID",
    "datasets.xml",
    "Davis, J.C. 1986. Statistics and Data Analysis in Geology, 2nd Ed. John Wiley and Sons. New York, New York.",
    "days since 2010-01-01",
    "deflate",
    "degree_C",
    "degree_F",
    "degrees_east",
    "degrees_north",
    "DODS",
    "DOI",
    "E = &sum;(w Y)/&sum;(w)",
    "Earth Science &amp; Atmosphere &amp; Atmospheric Pressure &amp; Atmospheric Pressure Measurements", // &amp; was converted to &amp;amp;   //Qi did this. Bob doesn't.
    "Earth Science &amp; Atmosphere &amp; Atmospheric Pressure &amp; Sea Level Pressure",
    "Earth Science &amp; Atmosphere &amp; Atmospheric Pressure &amp; Static Pressure",
    "EDDGrid",
    "encodeURIComponent()",
    "endTime",
    "ERDDAP", // before ERD and DAP
    "erd dot data at noaa dot gov",
    "erd.data at noaa.gov",
    "ERD",
    "ESPRESSO",
    "ESPreSSO",
    "ESRI .asc",
    "ESRI GeoServices REST",
    "excludedWord",
    "Ferret",
    "FileInfo.com",
    "fileType={0}",
    "FIPS",
    "GetCapabilities",
    "GetMap",
    "Gimp",
    "GNOME",
    "Google Charts",
    "Google Earth",
    "Google Visualization",
    // "gzip", //is below after x-gzip
    "h, hr, hrs, hour, hours,",
    "HDF",
    "http<strong>s</strong>",
    "https://coastwatch.pfeg.noaa.gov/erddap/files/jplMURSST41/.csv",
    "https://coastwatch.pfeg.noaa.gov/erddap/files/jplMURSST41/",
    "HTTP GET",
    "Hyrax",
    "InverseDistance",
    "IOOS DIF SOS",
    "IOOS Animal Telemetry Network",
    "IrfanView",
    "Java",
    "java.net.URLEncoder",
    "Leaflet",
    "long_name",
    "m, min, mins, minute, minutes,",
    "mashups",
    "Matlab",
    "maximum=37.0",
    "minimum=32.0",
    "mon, mons, month, months,",
    "ms, msec, msecs, millis, millisecond, milliseconds,",
    "NASA's Panoply",
    "National Oceanic and Atmospheric Administration",
    "NCO",
    "Ncview",
    "Nearest, Bilinear, Scaled",
    "NetCDF",
    "NMFS",
    "NOAA",
    "now-7days", // before now-
    "now-",
    "ODV .txt",
    "OGC",
    "OOSTethys",
    "OPeNDAP",
    "DAP", // out of place, so that it is after ERDDAP and OPeNDAP
    "OpenID",
    "OpenLayers",
    "OpenSearch",
    "Oracle",
    "orderBy(\"stationID, time\")", // before orderBy
    "orderByClosest(\"stationID, time/2hours\")",
    "orderByCount(\"stationID, time/1day\")",
    "orderByMax(\"stationID, time/1day\")",
    "orderByMax(\"stationID, time/1day, 10\")",
    "orderByMax(\"stationID, time/1day, temperature\")",
    "orderByMinMax(\"stationID, time/1day, temperature\")",
    "orderByClosest",
    "orderByCount",
    "orderByLimit",
    "orderByMax",
    "orderByMean",
    "orderByMinMax",
    "orderBy", // must be after the longer versions
    "Panoply",
    "Photoshop",
    "position={1}",
    "Practical Salinity Units",
    "protocol=griddap",
    "protocol=tabledap",
    "PSU",
    "Pull",
    "Push",
    "Python",
    "Res., 117, C01011, doi:10.1029/2011JC007395.",
    "RESTful", // before REST
    "REST",
    "ROA",
    "RSS",
    "s, sec, secs, second, seconds,",
    "Satellite Application Facility",
    "Sea Surface Temperature",
    "searchEngine=lucene",
    "SOAP+XML",
    "SOS",
    "sst",
    "stationID",
    "StickX",
    "StickY",
    "<strong>lines</strong>",
    "<strong>linesAndMarkers</strong>",
    "<strong>markers</strong>",
    "<strong>sticks</strong>",
    "<strong>surface</strong>",
    "<strong>vectors</strong>",
    "subsetVariables",
    "Surface Skin Temperature",
    "SWFSC", // before WFS
    "Synthetic Aperture Focusing",
    "tabledap",
    "Todd",
    "uDig",
    "UDUNITS",
    "Unidata",
    "URN",
    "WCS",
    "week, weeks,",
    "WFS",
    "wget",
    "Wikipedia",
    "WMS",
    "x-gzip",
    "gzip", // must be after x-gzip
    "yr, yrs, year, or years",
    "yyyy-MM-ddTHH:mm:ssZ", // before yyyy-MM-dd
    "yyyy-MM-dd",
    "Zulu"
  };
  protected static final int nDontTranslate = dontTranslate.length;

  /**
   * This returns the translationClient.
   *
   * <p>Google sample code says: Initialize client that will be used to send requests. This client
   * only needs to be created once, and can be reused for multiple requests. After completing all of
   * your requests, call the "close" method on the client to safely clean up any remaining
   * background resources. https://cloud.google.com/docs/authentication/getting-started
   */
  private static TranslationServiceClient translationClient() throws Exception {
    if (translationClient != null) return translationClient;

    // GoogleCredentials credentials = GoogleCredentials.fromStream(new
    // FileInputStream(credentialsFileName))
    //     .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
    TranslationServiceSettings translationServiceSettings =
        TranslationServiceSettings.newBuilder()
            // .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build();
    translationClient = TranslationServiceClient.create(translationServiceSettings);
    return translationClient;
  }

  /**
   * This is the main high level procedure which compares the tag values in messagesOld.xml (from
   * previous version of ERDDAP) to messages.xml (from new version of ERDDAP) to determine which
   * tags in messages.xml need to be translated to make/update messages-lang.xml, and which already
   * translated messages can be reused.
   *
   * @param justTranslateNTags The number of tags you want to translate, e.g., 5 for tests, or a
   *     huge number (or -1) to translate all tags.
   * @param tVerboseLanguage Set this to a language code, e.g., "de", if you want so have the
   *     translations for that language printed to System.out. Or use "all" to see all translations
   *     for all languages. Or use "" for no diagnostic messages.
   * @throws Throwable if trouble
   */
  public static void translate(int justTranslateNTags, String tVerboseLanguage) throws Exception {
    String2.log("*** TranslateMessages.translate");
    String2.log("utilDir=" + utilDir);
    verboseLanguage = tVerboseLanguage;

    // this didn't work for Bob
    // SSR.dosOrCShell("set GOOGLE_APPLICATION_CREDENTIALS=" + credentialsFileName, 10);

    Writer fileWriters[] = null;
    translationCounter =
        0; // reset the counter (better if this were a local variable, but that is awkward)
    try {

      // read the current and old messages.xml file
      SimpleXMLReader xmlReader = new SimpleXMLReader(messagesXmlFileName.openStream());
      String oldMessagesXmlFilePath = File2.accessResourceFile(oldMessagesXmlFileName.getPath());
      HashMap<String, String> previousMessageMap = getXMLTagMap(oldMessagesXmlFilePath);

      // read the current messages-[langCode].xml files (if they exists)
      HashMap<String, String>[] translatedTagMaps =
          (HashMap<String, String>[]) new HashMap[languageCodeList.length];
      for (int languagei = 1; languagei < languageCodeList.length; languagei++) {
        String fileName = Paths.get(translatedMessagesDir + "messages-" + languageCodeList[languagei] + ".xml").toString();
        translatedTagMaps[languagei] =
            File2.isFile(fileName) ? getXMLTagMap(fileName) : new HashMap();
      }

      // create the new-messages-[langCode].xml files
      fileWriters = new Writer[languageCodeList.length];
      for (int languagei = 1; languagei < languageCodeList.length; languagei++) {
        Path newMessagePath = Paths.get(translatedMessagesDir + "new-messages-" + languageCodeList[languagei] + ".xml");
        fileWriters[languagei] = Files.newBufferedWriter(newMessagePath, StandardCharsets.UTF_8);
        fileWriters[languagei].write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      }
      // This for-loop tests the translation output of first justTranslateNTags.
      // The break command below will be executed when there are no tags left
      if (justTranslateNTags < 0) justTranslateNTags = Integer.MAX_VALUE;
      while (true) {
        // testing what is in xmlReader
        xmlReader.nextTag();
        String tagName = xmlReader.topTag();
        if (translationCounter >= justTranslateNTags || tagName == null) // no more tags
        break;
        String toTranslate = xmlReader.rawContent();

        if (toTranslate.trim().equals("")) {
          // empty content, do not waste api resources
          // String2.log(tagName + " has empty content");
          for (int languagei = 1; languagei < languageCodeList.length; languagei++) {
            fileWriters[languagei].write(xmlReader.rawContent());
            // after writing the translated content, we write the <tag>
            fileWriters[languagei].write("<" + tagName + ">");
            if (xmlReader.isEndTag()) {
              // if the tag is an end tag, we add a new line character at the end of the tag
              fileWriters[languagei].write("\n");
            }
          }
        } else if (doNotTranslateSet.contains(tagName)) {
          // if the tag is one of the tags we do not want to translate
          String2.log(tagName + " is designed to not be translated");
          for (int languagei = 1; languagei < languageCodeList.length; languagei++) {
            if (tagName.equals("/langCode")) {
              // langCode is determined individually
              fileWriters[languagei].write(languageCodeList[languagei]);
            } else {
              fileWriters[languagei].write(xmlReader.rawContent());
            }
            // after writing the translated content, we write the <tag>
            fileWriters[languagei].write("<" + tagName + ">");
            if (xmlReader.isEndTag()) {
              // if the tag is an end tag, we add a new line character at the end of the tag
              fileWriters[languagei].write("\n");
            }
          }
        } else {
          boolean modified =
              !previousMessageMap.getOrDefault(tagName, "DNE").equals(xmlReader.rawContent());
          for (int languagei = 1; languagei < languageCodeList.length; languagei++) {
            boolean translated =
                !translatedTagMaps[languagei].getOrDefault(tagName, "DNE").equals("DNE");
            if (!modified && translated) {
              // just keep the existing translation
              fileWriters[languagei].write(translatedTagMaps[languagei].get(tagName));
            } else {
              // translate it
              boolean html = isHtml(toTranslate);
              boolean messageFormat = isMessageFormat(toTranslate);
              String result =
                  translateTag(toTranslate, languageCodeList[languagei], html, messageFormat);
              fileWriters[languagei].write(result);
            }
            // after writing the translated content, we write the <tag>
            fileWriters[languagei].write("<" + tagName + ">");
            if (xmlReader.isEndTag()) {
              // if the tag is an end tag, we add a new line character at the end of the tag
              fileWriters[languagei].write("\n");
            }
          }
        }
      }
      // close filewriters
      xmlReader.close();
      for (int languagei = 1; languagei < languageCodeList.length; languagei++) {
        fileWriters[languagei].close();
      }
    } catch (Exception e) {
      String2.log("An error occurred in main():");
      e.printStackTrace();
    }
    translationClient().close();
    translationClient = null;
    String2.log("\n*** translate() finished. nTagsTranslatedByGoogle=" + translationCounter);
  }

  /**
   * Use this method to generate a HashMap of tags in a translated message.xml file.
   *
   * @param xmlReader a SimpleXMLReader object set to the target xml file
   * @return A HashMap<key=tag name, value=[comment, tag content]>
   * @throws Exception when something goes wrong
   */
  public static HashMap<String, String> getXMLTagMap(SimpleXMLReader xmlReader) throws Exception {
    HashMap<String, String> resultMap = new HashMap<String, String>();
    if (xmlReader == null) {
      return resultMap;
    }
    while (true) {
      xmlReader.nextTag();
      resultMap.put(xmlReader.topTag(), xmlReader.rawContent());
      if (xmlReader.stackSize() == 0) // done
      break;
    }
    xmlReader.close();
    return resultMap;
  }

  /**
   * This indicates if the rawContent is not a comment and has HTML content (i.e., has CDATA
   * markers).
   *
   * @param rawContent
   * @return true if the rawContent is HTML content (i.e., has CDATA markers).
   */
  public static boolean isHtml(String rawContent) {
    return rawContent
        .trim()
        .startsWith(
            "<![CDATA["); // findTagsMissingCDATA() ensures that all tags with HTMLEntities start
    // with "<![CDADA["
    // was Arrays.stream(HTMLEntities).anyMatch(rawContent::contains);
  }

  /**
   * This indicates if rawContent uses the MessageFormat system ({0}, {1}).
   *
   * @param rawContent
   * @return true if the rawContent uses the MessageFormat system.
   */
  public static boolean isMessageFormat(String rawContent) {
    return Arrays.stream(messageFormatEntities).anyMatch(rawContent::contains);
  }

  /**
   * Use this method to generate a HashMap of tags in a translated message.xml file.
   *
   * @param fileName the fileName of the xml file
   * @return A HashMap <key=tagname, value=[comment, tag content]>
   * @throws Exception if trouble
   */
  public static HashMap<String, String> getXMLTagMap(String fileName) throws Exception {
    String2.log("getXMLTagMap(" + fileName + ")");
    return getXMLTagMap(new SimpleXMLReader(new FileInputStream(fileName)));
  }

  /**
   * Make changes to HTML content before sending to Google Translate. Call this before sending the
   * text to the translator. Notably, encode some words and phrases to non-words so they won't be
   * translated. (Qi used the glossary for this, but Google Translate often didn't honor it reliably
   * with German.)
   *
   * @param rawContent the rawContent text of a CDATA tag
   * @return the modified text
   */
  public static String preProcessHtml(String rawContent) {

    if (debugMode) String2.log("\n>> before preProcessHtml=" + rawContent + "\n");

    // handle separately:
    // rawContent = String2.replaceAll(rawContent, "&amp;",  "&amp;amp;");   //&amp; was converted
    // to &amp;amp;   //Qi did this. Bob doesn't.
    rawContent =
        String2.replaceAll(
            rawContent, "?[\\s]", "?&nbsp;"); // because Google usually removes the space
    rawContent = String2.replaceAll(rawContent, "&nbsp;", "<br />");

    // This is obviously very inefficient, but I don't (quickly) see another way to
    // ensure there are no nested <span>'s: e.g., catch ERDDAP and then catch DAP.
    // To a human, the speed is imperceptible. So it isn't a problem.
    // This REQUIRES that there are no short strings (DAP) in the dontTranslate list
    // befor longer strings containing them (ERDDAP),
    // hence the checking code at the beginning of main().
    StringBuilder sb = new StringBuilder();
    int rcLength = rawContent.length();
    for (int po = 0; po < rcLength; po++) {
      char rcChar = rawContent.charAt(po);

      // is this start of <a ...> or <img ...>? if so skip to '>'
      if (rcChar == '<'
          && po < rcLength - 4
          && (rawContent.substring(po, po + 2).equals("<a")
              || // usually "<a "   but sometimes "<a\n"
              rawContent
                  .substring(po, po + 4)
                  .equals("<img"))) { // usually "<img " but sometimes "<img\n"
        sb.append('<');
        po++;
        // skip to '>'
        while (po < rcLength && rawContent.charAt(po) != '>') {
          sb.append(rawContent.charAt(po));
          po++;
        }
        if (po == rcLength) // end of rawContent: shouldn't happen
        break;
        sb.append('>'); // and of <a ...>
        continue;
      }

      // does a dontTranslate[i] start at this po?
      boolean found = false;
      for (int i = 0; i < nDontTranslate; i++) {
        String dontTrans = dontTranslate[i];
        int dontTransLength = dontTrans.length();
        // String2.log("rcLength=" + rcLength + " po=" + po + " dontTrans=" + dontTrans);
        if (rcChar == dontTrans.charAt(0)
            && // quick reject increases speed
            po + dontTransLength <= rcLength
            && rawContent.substring(po, po + dontTransLength).equals(dontTrans)) {
          found = true;
          sb.append(START_SPAN + dontTrans + STOP_SPAN);
          po += dontTransLength - 1; // -1 to negate the coming po++
          break;
        }
      }
      if (!found) sb.append(rcChar);
    }

    if (debugMode) String2.log("\n>> after preProcessHtml=" + sb.toString() + "\n");
    return sb.toString();
  }

  /**
   * Use this to post process the translated HTML which was generated by Google Translate.
   *
   * @param s the translated HTML returned by Google Translator.
   * @param original this method will prevent wrongly decoded "&amp;" in URLs by comparing s with
   *     the original unmodified text.
   * @return the text ready to be written in the xml file
   */
  private static String postProcessHtml(String s, String original) {
    if (debugMode) String2.log("\n>> before postProcessHtml=" + s + "\n");

    // Google Translate converts all &psuedoentity; to &amp;psuedoentity; (to make valid HTML).
    // They are now in START_SPAN/STOP_SPAN pairs.
    // So undo that change.
    StringBuilder sb =
        new StringBuilder(s); // working with StringBuilder (not String) is much faster
    String2.replaceAll(sb, START_SPAN + "&amp;", START_SPAN + "&");
    // and some also within <kbd></kbd> pairs
    String2.regexReplaceAll(sb, START_SPAN + "<kbd>(&amp;)[a-zA-Z0-9]+;</kbd>" + STOP_SPAN, 1, "&");

    // remove pairs of START_SPAN and STOP_SPAN
    int po = 0;
    while (true) {
      po = sb.indexOf(START_SPAN, po);
      if (po < 0) break;
      sb.replace(po, po + START_SPAN.length(), "");

      po = sb.indexOf(STOP_SPAN, po);
      if (po < 0) // shouldn't happen
      break;
      sb.replace(po, po + STOP_SPAN.length(), "");
    }

    // undo other changes made in preProcessHtml (in opposite order of preProcessHtml)
    String2.replaceAll(sb, "<br />", "&nbsp;");
    String2.replaceAll(
        sb, "?&nbsp;", "? "); // ? in   . //my system because Google usually removes the space
    //        String2.replaceAll(sb, "?&nbsp;",  "? ");    //? in
    //        String2.replaceAll(sb, "?&nbsp;",  "? ");    //? in
    //        String2.replaceAll(sb, "?&nbsp;",  "? ");    //? in
    // String2.replaceAll(sb, "&amp;amp;", "&amp;");    //&amp; was converted to &amp;amp;   //Qi
    // did this. Bob doesn't.

    // remove an extra whitespace after <p>, <li>, <strong>, <br>
    String2.replaceAll(sb, "\"> ", "\">"); // eg space after <a href="...">
    String2.replaceAll(sb, "> )", ">)");
    String2.replaceAll(sb, " ,", ",");
    String2.replaceAll(sb, "„ ", "„"); // German open  quote
    String2.replaceAll(sb, " “", "“"); // German close quote
    String2.replaceAll(sb, "<br> ", "<br>");
    String2.replaceAll(sb, "<p> ", "<p>");
    String2.replaceAll(sb, "<ol> ", "<ol>");
    String2.replaceAll(sb, "<ul> ", "<ul>");
    String2.replaceAll(sb, "<li> ", "<li>");
    String2.replaceAll(sb, "</ol> ", "</ol>");
    String2.replaceAll(sb, "</ul> ", "</ul>");
    String2.replaceAll(sb, "<th> ", "<th>");
    String2.replaceAll(sb, "<td> ", "<td>");
    String2.replaceAll(sb, "<strong> ", "<strong>");
    String2.replaceAll(sb, "<td> ", "<td>");
    String2.replaceAll(sb, "<h1> ", "<h1>");
    String2.replaceAll(sb, "<h2> ", "<h2>");
    String2.replaceAll(sb, "<h3> ", "<h3>");
    String2.replaceAll(sb, "&nbsp; ", "&nbsp;");

    String2.replaceAll(sb, "( )", "(  )"); // do "( " below, but not for "( )"
    String2.replaceAll(sb, "( ", "(");
    // replace fancy left/right " with plain " ?

    // add \n to useful places so HTML is formatted reasonably nicely (not just one long line)
    String2.replaceAll(sb, "<h", "\n<h");
    String2.replaceAll(sb, "</h1> ", "</h1>\n");
    String2.replaceAll(sb, "</h2> ", "</h2>\n");
    String2.replaceAll(sb, "</h3> ", "</h3>\n");
    String2.replaceAll(sb, "<br>", "\n<br>");
    String2.replaceAll(sb, "<p>", "\n<p>");
    String2.replaceAll(sb, "<ol>", "\n<ol>");
    String2.replaceAll(sb, "<ul>", "\n<ul>");
    String2.replaceAll(sb, "<li>", "\n<li>");
    String2.replaceAll(sb, "</ul>", "\n</ul>\n");
    String2.replaceAll(sb, "</ol>", "\n</ol>\n");
    String2.replaceAll(sb, "<table", "\n<table"); // no > because it often has params
    String2.replaceAll(sb, "<tr", "\n<tr"); // no > because it often has params
    String2.replaceAll(sb, "</tr", "\n</tr"); // no > because it often has params
    String2.replaceAll(sb, "<th", "\n<th"); // no > because it often has params
    String2.replaceAll(sb, "<td", "\n<td"); // no > because it often has params
    String2.replaceAll(sb, " <a ", "\n<a ");
    String2.replaceAll(sb, "</pre> ", "</pre>\n");
    String2.replaceAll(sb, ": ", ":\n");
    String2.replaceAll(sb, ".  ", ".\n");
    String2.replaceAll(sb, ". ", ".\n");
    String2.replaceAll(sb, "? ", "?\n");

    String2.replaceAll(sb, "。 ", "。\n"); // chinese period (but often not followed by space)

    String2.replaceAll(sb, "z.\nB.\n", "z.B. "); // but undo break of up German z. B.
    String2.replaceAll(sb, "\n\n", "\n"); // finally, consolidate if too many \n

    /*
    //Qi did this. Bob doesn't.
    //prevent the &amp; from being decoded in URLs of <a> tags.
    int originalAmpIndex = original.indexOf("&");
    int currAmpIndex = sb.indexOf("&", 0);
    while (originalAmpIndex != -1 && currAmpIndex != -1) {
        if (
            !sb.substring(currAmpIndex, currAmpIndex + 5).equals("&amp;")
            && original.substring(originalAmpIndex, originalAmpIndex + 5).equals("&amp;")
        ) {
            sb.replace(currAmpIndex, currAmpIndex + 1, "&amp;");
        }
        currAmpIndex = sb.indexOf("&", currAmpIndex + 1);
        originalAmpIndex = original.indexOf("&", originalAmpIndex + 1);
    }
    */

    // Google changed "something" into " something ", so remove the extra spaces just inside the
    // &quot;'s.
    // Not Perfect! "files" documentation and other places have a stray " which then gets this out
    // of sync sometimes.
    int quotCount = 0;
    int quotIndex = sb.indexOf("&quot;");
    while (quotIndex != -1) {
      if (quotCount % 2 == 0) {
        // first quote in a quote pair
        if (quotIndex + 7 <= sb.length()
            && sb.substring(quotIndex, quotIndex + 7).equals("&quot; ")) {
          sb.replace(quotIndex + 6, quotIndex + 7, "");
        }
      } else {
        // second quote in a quote pair
        if (quotIndex + 6 <= sb.length()
            && sb.substring(quotIndex - 1, quotIndex + 6).equals(" &quot;")) {
          sb.replace(quotIndex - 1, quotIndex, "");
        }
      }
      quotIndex = sb.indexOf("&quot;", quotIndex + 1);
      quotCount++;
    }

    return sb.toString();
  }

  /**
   * A low-level version of translateTag(), which is used by translateTag.
   *
   * @param rawContent the content to be translated
   * @param languageCode e.g., de for German
   * @param html indicates if the content is html code
   * @param messageFormat indicates if the content is for Message.format(), e.g., has {0} as a
   *     substitution placeholder.
   * @return the translated content
   */
  static String lowTranslateTag(
      String rawContent, String languageCode, boolean html, boolean messageFormat)
      throws Exception {
    if (debugMode) String2.log(">> lowTranslateTag langCode=" + languageCode + " isHtml=" + html);
    String result = rawContent;
    if (messageFormat) {
      // convert all '' to '
      result = result.replaceAll("''", "'");
    }
    if (html) result = preProcessHtml(result);
    result = translateTextV3("en", languageCode, result, html);
    if (html) {
      result = postProcessHtml(result, rawContent);
    } else {
      result =
          XML.encodeAsXML(
              result); // e.g., convert > to &gt; in preparation for storage in the outgoing XML
      // document

      // but then change &amp;tErddapUrl; to be &tErddapUrl;
      Pattern p = Pattern.compile("&amp;[a-zA-Z]+;");
      while (true) {
        Matcher m = p.matcher(result);
        if (!m.find()) break;
        result =
            result.substring(0, m.start() + 1)
                + // remove the "amp;"
                result.substring(m.start() + 5);
      }
    }

    if (messageFormat) {
      // convert all ' to '' (but don't convert '' to ''''!)
      result = result.replaceAll("''", "'"); // so do this trick first
      result = result.replaceAll("'", "''");
    }

    if ("all".equals(verboseLanguage) || languageCode.equals(verboseLanguage))
      String2.log(
          "\n{{ lowTranslate  in: "
              + rawContent
              + "\n"
              + "\n}} lowTranslate out: "
              + result
              + "\n");

    return result;
  }

  /**
   * This method utilizes Google Cloud Translation-Advanced service to translate the input text to
   * the target language. [WAS: This uses the glossary feature to keep selected words untranslated.]
   * This is based on the sample Google code in the next method.
   *
   * @param languageFrom The source language code
   * @param languageTo The target language code
   * @param text The text (rawContent) to be translated
   * @param html true if the text is html (has CDATA markers (which should be true if it has HTML
   *     tags))
   * @return the translated text
   * @throws Exception
   */
  private static String translateTextV3(
      String languageFrom, String languageTo, String text, boolean html) throws Exception {
    if (text.length() > 30000) {
      int breakPoint = text.indexOf("<p>", 20000); // all the long texts have <p>
      if (breakPoint < 0) // won't be needed, but to be safe...
      breakPoint = 20000;
      return translateTextV3(languageFrom, languageTo, text.substring(0, breakPoint), html)
          + translateTextV3(
              languageFrom, languageTo, text.substring(breakPoint, text.length()), html);
    }

    // Supported Mime Types: https://cloud.google.com/translate/docs/supported-formats
    if (debugMode)
      String2.log(
          ">> translateTextV3 lang=" + languageTo + " html=" + html + " text=[[[" + text + "]]]");
    TranslateTextRequest request =
        TranslateTextRequest.newBuilder()
            .setParent(parent.toString())
            .setMimeType(html ? "text/html" : "text/plain")
            .setSourceLanguageCode(languageFrom) // set to English by default
            .setTargetLanguageCode(languageTo)
            .addContents(text)
            // !!! 2021-10-22 I no longer use the glossary  .setGlossaryConfig(glossaryConfig)
            .build();

    TranslateTextResponse response = translationClient().translateText(request);
    translationCounter++;
    String res = "";
    // Qi had  for (Translation translation : response.getGlossaryTranslationsList()) {
    for (Translation translation : response.getTranslationsList()) {
      res += translation.getTranslatedText();
    }
    if ("all".equals(verboseLanguage) || languageTo.equals(verboseLanguage))
      String2.log(
          "\n{{{ translateTextV3  in: " + text + "\n" + "\n}}} translateTextV3 out: " + res + "\n");

    return res;
  }

  /**
   * This is simple sample test code from Google.
   * https://cloud.google.com/translate/docs/advanced/quickstart It can be called directly to test
   * authentication and permissions (i.e., setup stuff).
   */
  public static String googleSampleCode(String projectId, String targetLanguage, String text)
      throws IOException {

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background resources.
    try (TranslationServiceClient client = TranslationServiceClient.create()) {
      // Supported Locations: `global`, [glossary location], or [model location]
      // Glossaries must be hosted in `us-central1`
      // Custom Models must use the same location as your model. (us-central1)
      LocationName parent = LocationName.of(projectId, "global");

      // Supported Mime Types: https://cloud.google.com/translate/docs/supported-formats
      TranslateTextRequest request =
          TranslateTextRequest.newBuilder()
              .setParent(parent.toString())
              .setMimeType("text/plain")
              .setTargetLanguageCode(targetLanguage)
              .addContents(text)
              .build();

      TranslateTextResponse response = client.translateText(request);

      // Display the translation for each input text provided
      String result = "";
      for (Translation translation : response.getTranslationsList())
        result += translation.getTranslatedText();
      return result;
    }
  }

  /**
   * Translate the given tag. Only use this method when it's needed, i.e. tags not in
   * doNotTranslateSet, or can be trimmed to be "".
   *
   * @param rawContent rawContent of the given tag. Include CDATA and comment syntax.
   * @param languageCode the languageCode of the targeted language
   * @param html if the tag contains HTML
   * @param messageFormat if the tag uses messageFormat
   * @return a translated string to write in place of the rawContent
   * @throws Exception if trouble
   */
  private static String translateTag(
      String rawContent, String languageCode, boolean html, boolean messageFormat)
      throws Exception {
    if (rawContent.startsWith("<!--")) {
      // rawContent is a comment,
      // so we need to remove the comment syntax and insert a new line character before the tag
      // also consider the possibility of multiple comments before one tag
      String toWrite = "";
      messageFormat = false; // comments never use messageFormat
      int commentEnd = 0; // to get us started
      while (true) {
        int commentStart =
            rawContent.indexOf("<!-- ", commentEnd) + 4; // so -1 (not found) becomes 3
        if (commentStart == 3) break;
        commentEnd = rawContent.indexOf("-->", commentStart);
        if (commentEnd < 0) { // shouldn't happen
          commentEnd = rawContent.length();
          String2.log("ERROR: no end to comment=" + rawContent);
        }
        toWrite +=
            "\n<!-- "
                + lowTranslateTag(
                    rawContent.substring(commentStart, commentEnd),
                    languageCode,
                    html,
                    messageFormat)
                + " -->";
      }
      // newline before next tag
      toWrite += "\n";
      return toWrite;

    } else if (rawContent.startsWith("<![CDATA[")) {
      // rawContent is a CDATA tag
      html = true;
      return "<![CDATA[\n"
          + lowTranslateTag(
              rawContent.substring(9, rawContent.length() - 3), languageCode, html, messageFormat)
          + "\n]]>";

    } else {
      return lowTranslateTag(
          rawContent,
          languageCode,
          html,
          messageFormat); // html should be false, but findTagsMissingCDATA(); checked for that
    }
  }

  /******************* TESTS **************************************/

  /**
   * This extracts all instances of some regex from the messages.xml file.
   *
   * @param regex The first capture group is the part that will be extracted. Common regexes:
   *     ".*(\\&[a-zA-Z0-9]+?;).*", ".*(<kbd>.*</kbd>).*", ".*(&lt;\\w*&gt;).*"
   * @return a String[]
   */
  public static String[] extractRegex(String regex) throws Exception {
    String s = new String(Resources.toByteArray(messagesXmlFileName), StandardCharsets.UTF_8);
    String2.log(s);
    String oa[] = String2.extractAllCaptureGroupsAsHashSet(s, regex, 1).toArray(new String[0]);
    Arrays.sort(oa, new StringComparatorIgnoreCase());
    String2.log(String2.toNewlineString(oa));
    String2.log("nFound=" + oa.length);
    return oa;
  }

  /** Un/comment code below, then run this to do things with this class. */
  public static void main(String args[]) throws Throwable {
    String2.log("*** TranslateMessages.main()");

    // Run the TranslateMessagesTests before this which include:
    // testDontTranslateOrdering();
    // findTagsMissingCDATA();
    // *** system unit tests
    // testGoogleSampleCode();
    // testTranslateHtml();
    // testTranslateComment();
    // testTranslatePlainText();
    // And the JettyTest
    // checkForUncaughtSpecialText();

    // *** Uncomment this to translate all of messages.xml.
    // To work with just one language (e.g., German), uncomment "use these mini lists" above
    // If you like the translation, rename new-messages-[langCode].xml to be messages-[langCode].xml
    // If you like the translation for all languages, copy messages.xml to be messagesOld.xml.
    translate(
        -1,
        "all"); // params: justTranslateNTags (-1 for all), verboseLanguage (e.g., "" (none), "de"
    // (German) or "all")

    String2.log("*** TranslateMessages.main() finished.");
  }
}
