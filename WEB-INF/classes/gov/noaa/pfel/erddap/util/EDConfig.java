package gov.noaa.pfel.erddap.util;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2;
import com.cohort.util.Math2;
import com.cohort.util.ResourceBundle2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.http.CorsResponseFilter;
import gov.noaa.pfel.erddap.util.Metrics.FeatureFlag;
import java.awt.Color;
import java.awt.Image;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public class EDConfig {
  /**
   * These are options used to control behavior for testing. They should be their default values
   * during normal operation. Better encapsulation of EDStatic initilization would mean we can get
   * rid of these.
   */
  public boolean skipEmailThread = false;

  public boolean forceSynchronousLoading = false;

  /**
   * This is almost always false. During development, Bob sets this to true. No one else needs to.
   * If true, ERDDAP uses setup2.xml and datasets2.xml (and messages2.xml if it exists).
   */
  public boolean developmentMode = false;

  // End debug config

  /**
   * contentDirectory is the local directory on this computer, e.g., [tomcat]/content/erddap/ It
   * will have a slash at the end.
   */
  public String contentDirectory;

  // fgdc and iso19115XmlDirectory are used for virtual URLs.
  public static final String fgdcXmlDirectory = "metadata/fgdc/xml/"; // virtual
  public static final String iso19115XmlDirectory = "metadata/iso19115/xml/"; // virtual
  public static final String IMAGES_DIR = "images/";
  private static final String PUBLIC_DIR = "public/";
  public final String fullPaletteDirectory;
  public final String fullPublicDirectory;
  public final String imageDir; // local directory on this computer
  public final String fullDatasetDirectory; // all the Directory's have slash at end
  public final String fullFileVisitorDirectory;
  public final String fullCacheDirectory;
  public final String fullDecompressedDirectory;
  public final String fullDecompressedGenerateDatasetsXmlDirectory;
  public final String fullLogsDirectory;
  public final String fullCopyDirectory;
  public final String fullLuceneDirectory;
  public final String fullResetFlagDirectory;
  public final String fullBadFilesFlagDirectory;
  public final String fullHardFlagDirectory;
  public final String fullCptCacheDirectory;
  public final String fullPlainFileNcCacheDirectory;
  public final String fullSgtMapTopographyCacheDirectory;
  public final String fullTestCacheDirectory;
  public final String fullWmsCacheDirectory;

  /**
   * These values are loaded from the [contentDirectory]setup.xml file. See comments in the
   * [contentDirectory]setup.xml file.
   */
  public final String baseUrl;

  public final String baseHttpsUrl; // won't be null, may be "(not specified)"
  public String bigParentDirectory;
  public final String mqttConfigFolder;
  public final String mqttDataFolder;
  public final String mqttExtensionsFolder;
  public final String adminInstitution;
  public final String adminInstitutionUrl;
  public final String adminIndividualName;
  public final String adminPosition;
  public final String adminPhone;
  public final String adminAddress;
  public final String adminCity;
  public final String adminStateOrProvince;
  public final String adminPostalCode;
  public final String adminCountry;
  public final String adminEmail;
  public final String accessConstraints;
  public final String accessRequiresAuthorization;
  public final String fees;
  public final String keywords;
  public final String units_standard;

  public
  String /* For the wcs examples, pick one of your grid datasets that has longitude and latitude axes.
         The sample variable must be a variable in the sample grid dataset.
         The bounding box values are minx,miny,maxx,maxy.
         */ wcsSampleDatasetID = "jplMURSST41";
  public String wcsSampleVariable = "analysed_sst";
  public String wcsSampleBBox = "-179.98,-89.98,179.98,89.98";
  public String wcsSampleAltitude = "0";
  public String wcsSampleTime = "2002-06-01T09:00:00Z";

  public String /* For the wms examples, pick one of your grid datasets that has longitude
      and latitude axes.
      The sample variable must be a variable in the sample grid dataset.
      The bounding box values are minx,miny,maxx,maxy.
      The default for wmsActive is "true".
      */
      wmsSampleDatasetID = "jplMURSST41";
  public String wmsSampleVariable = "analysed_sst";
  public String /* The bounding box values are minLongitude,minLatitude,maxLongitude,maxLatitude.
         Longitude values within -180 to 180, or 0 to 360, are now okay. */
      wmsSampleBBox110 = "-179.99,-89.99,180.0,89.99";
  public String wmsSampleBBox130 = "-89.99,-179.99,89.99,180.0";
  public String wmsSampleTime = "2002-06-01T09:00:00Z";
  public String sosFeatureOfInterest;
  public String sosUrnBase;
  public String sosBaseGmlName;
  public String sosStandardNamePrefix;
  public String
      authentication; // will be one of "", "custom", "email", "google", "orcid", "oauth2". If
  public final String // baseHttpsUrl doesn't start with https:, this will be "".
      datasetsRegex;
  public final String emailEverythingToCsv;
  public final String emailDailyReportToCsv;
  public final String emailSubscriptionsFrom;
  public final String flagKeyKey;
  public final String fontFamily;
  public final String googleClientID; // if authentication=google or oauth2, this will be something
  public final String orcidClientID; // if authentication=orcid  or oauth2, this will be something
  public final String
      orcidClientSecret; // if authentication=orcid  or oauth2, this will be something
  public final String googleEarthLogoFile;
  public final String highResLogoImageFile;
  public final String lowResLogoImageFile;
  public final String passwordEncoding; // will be one of "MD5", "UEPMD5", "SHA256", "UEPSHA256"
  public final String searchEngine;
  public final String warName;

  public String accessibleViaNC4; // "" if accessible, else message why not
  public final int lowResLogoImageFileWidth;
  public final int lowResLogoImageFileHeight;
  public final int highResLogoImageFileWidth;
  public final int highResLogoImageFileHeight;
  public final int googleEarthLogoFileWidth;
  public final int googleEarthLogoFileHeight;

  public final String emailSmtpHost;
  public final String emailUserName;
  public final String emailFromAddress;
  public final String emailPassword;
  public final String emailProperties;
  public int emailSmtpPort = 0; // <=0 means inactive
  @FeatureFlag public boolean emailIsActive = false; // ie if actual emails will be sent

  // things that were in setup.xml (discouraged) and are now in datasets.xml (v2.00+)
  public static final int DEFAULT_cacheMinutes = 60;
  public static final String DEFAULT_drawLandMask = "under";
  public static final int DEFAULT_graphBackgroundColorInt = 0xffccccff;
  public static final int DEFAULT_loadDatasetsMinMinutes = 15;
  public static final int DEFAULT_loadDatasetsMaxMinutes = 60;
  public static final String DEFAULT_logLevel = "info"; // warning|info|all
  public static final int DEFAULT_partialRequestMaxBytes =
      490000000; // this is just below tds default <opendap><binLimit> of 500MB
  public static final int DEFAULT_partialRequestMaxCells = 10000000;
  public static final int DEFAULT_slowDownTroubleMillis = 1000;
  public static final int DEFAULT_unusualActivity = 10000;
  public static final int DEFAULT_updateMaxEvents = 10;
  public static final int DEFAULT_unusualActivityFailPercent = 25;
  public static final boolean DEFAULT_showLoadErrorsOnStatusPage = true;
  public static final int DEFAULT_lowMemCacheGbLimit = 4;

  // Mqtt default configs
  public static final String DEFAULT_MQTT_HOST = "localhost";
  public static final int DEFAULT_MQTT_PORT = 1883;
  public static final String DEFAULT_MQTT_CLIENT = "erddap-client";
  public static final boolean DEFAULT_SSL = false;
  public static final int DEFAULT_KEEP_ALIVE = 60;
  public static final boolean DEFAULT_CLEAN_START = false;
  public static final int DEFAULT_SESSION_EXPIRY = 10;
  public static final int DEFAULT_CONNECTION_TIMEOUT = 10;
  public static final boolean DEFAULT_AUTO_RECONNECT = true;

  public long cacheMillis = DEFAULT_cacheMinutes * Calendar2.MILLIS_PER_MINUTE;
  public long cacheClearMillis = cacheMillis / 4;
  public int lowMemCacheGbLimit = DEFAULT_lowMemCacheGbLimit;
  public String drawLandMask = DEFAULT_drawLandMask;
  public boolean emailDiagnosticsToErdData = true;
  public Color graphBackgroundColor = new Color(DEFAULT_graphBackgroundColorInt, true); // hasAlpha
  public long loadDatasetsMinMillis = DEFAULT_loadDatasetsMinMinutes * Calendar2.MILLIS_PER_MINUTE;
  public long loadDatasetsMaxMillis = DEFAULT_loadDatasetsMaxMinutes * Calendar2.MILLIS_PER_MINUTE;
  // logLevel handled specially by setLogLevel
  public int partialRequestMaxBytes = DEFAULT_partialRequestMaxBytes;
  public int partialRequestMaxCells = DEFAULT_partialRequestMaxCells;
  public int slowDownTroubleMillis = DEFAULT_slowDownTroubleMillis;
  public int unusualActivity = DEFAULT_unusualActivity;
  public int updateMaxEvents = DEFAULT_updateMaxEvents;
  public int unusualActivityFailPercent = DEFAULT_unusualActivityFailPercent;

  public final String[] categoryAttributes; // as it appears in metadata (and used for hashmap)
  public final String[] categoryAttributesInURLs; // fileNameSafe (as used in URLs)
  public final boolean[] categoryIsGlobal;
  public int variableNameCategoryAttributeIndex = -1;

  // these are all non-null if in awsS3Output mode, otherwise all are null
  public String awsS3OutputBucketUrl = null; // ends in slash
  public String awsS3OutputBucket = null; // the short name of the bucket
  public S3TransferManager awsS3OutputTransferManager = null;
  public boolean useAwsCrt;
  public boolean useAwsAnonymous;

  public final String corsAllowHeaders;
  public final String[] corsAllowOrigin;

  public final int logMaxSizeMB;
  public String deploymentInfo;
  // Booleans
  public boolean usePrometheusMetrics = true;
  @FeatureFlag public final boolean listPrivateDatasets;
  @FeatureFlag public final boolean subscriptionSystemActive;
  @FeatureFlag public final boolean convertersActive;
  @FeatureFlag public final boolean slideSorterActive;
  @FeatureFlag public final boolean fgdcActive;
  @FeatureFlag public final boolean iso19115Active;
  @FeatureFlag public final boolean jsonldActive;
  @FeatureFlag public final boolean geoServicesRestActive;
  @FeatureFlag public final boolean filesActive;
  @FeatureFlag public final boolean defaultAccessibleViaFiles;
  @FeatureFlag public final boolean dataProviderFormActive;
  @FeatureFlag public final boolean outOfDateDatasetsActive;
  @FeatureFlag public final boolean politicalBoundariesActive;
  @FeatureFlag public final boolean wmsClientActive;
  @FeatureFlag public final boolean enableMqttBroker;
  @FeatureFlag public boolean sosActive;
  @FeatureFlag public final boolean wcsActive;
  @FeatureFlag public final boolean wmsActive;
  @FeatureFlag public boolean quickRestart;
  @FeatureFlag public final boolean subscribeToRemoteErddapDataset;
  @FeatureFlag public boolean showLoadErrorsOnStatusPage = DEFAULT_showLoadErrorsOnStatusPage;
  @FeatureFlag public boolean useHtmlTemplates;
  @FeatureFlag public boolean useHtmlTemplates;

  @FeatureFlag
  public
  boolean // if useLuceneSearchEngine=false (a setting, or after error), original search engine will
      // be
      // used
      useLuceneSearchEngine;

  public final String mqttServerHost;
  public final int mqttServerPort;
  public final String mqttClientId;
  public final String mqttUserName;
  public final String mqttPassword;
  public final boolean mqttSsl;
  public final int mqttKeepAlive;
  public final boolean mqttCleanStart;
  public final int mqttSessionExpiry;
  public final int mqttConnectionTimeout;
  public final boolean mqttAutomaticReconnect;

  @FeatureFlag public final boolean variablesMustHaveIoosCategory;
  @FeatureFlag public boolean useSaxParser;
  @FeatureFlag public boolean publishMqttNotif;
  @FeatureFlag public boolean enableEnvParsing;
  @FeatureFlag public boolean updateSubsRssOnFileChanges;
  @FeatureFlag public final boolean useEddReflection;
  @FeatureFlag public boolean enableCors;
  @FeatureFlag public boolean includeNcCFSubsetVariables;
  @FeatureFlag public boolean ncHeaderMakeFile = false;
  @FeatureFlag public boolean useSisISO19115 = false;
  @FeatureFlag public boolean useSisISO19139 = false;
  @FeatureFlag public boolean useHeadersForUrl = true;
  @FeatureFlag public boolean generateCroissantSchema = true;
  @FeatureFlag public boolean touchThreadOnlyWhenItems = true;
  @FeatureFlag public boolean taskCacheClear = true;
  @FeatureFlag public boolean useNcMetadataForFileTable = true;
  @FeatureFlag public boolean backgroundCreateSubsetTables = true;

  public EDConfig(String webInfParentDirectory) throws Exception {
    fullPaletteDirectory = webInfParentDirectory + "WEB-INF/cptfiles/";
    fullPublicDirectory = webInfParentDirectory + PUBLIC_DIR;
    imageDir = webInfParentDirectory + IMAGES_DIR; // local directory on this computer

    skipEmailThread = Boolean.parseBoolean(System.getProperty("skipEmailThread"));
    // **** find contentDirectory
    String ecd = "erddapContentDirectory"; // the name of the environment variable
    String errorInMethod;
    contentDirectory = System.getProperty(ecd);
    if (contentDirectory == null) {
      // Or, it must be sibling of webapps
      // e.g., c:/programs/_tomcat/webapps/erddap/WEB-INF/classes/[these classes]
      // On windows, contentDirectory may have spaces as %20(!)
      contentDirectory = File2.getClassPath(); // access a resource folder
      int po = contentDirectory.indexOf("/webapps/");
      contentDirectory =
          contentDirectory.substring(0, po) + "/content/erddap/"; // exception if po=-1
    } else {
      contentDirectory = File2.addSlash(contentDirectory);
    }
    Test.ensureTrue(
        File2.isDirectory(contentDirectory),
        "contentDirectory (" + contentDirectory + ") doesn't exist.");

    // **** setup.xml  *************************************************************
    // This is read BEFORE messages.xml. If that is a problem for something,
    //  defer reading it in setup and add it to the messages section.
    // read static Strings from setup.xml
    String setupFileName = contentDirectory + "setup" + (developmentMode ? "2" : "") + ".xml";
    errorInMethod = "ERROR while reading " + setupFileName + ": ";
    ResourceBundle2 setup = ResourceBundle2.fromXml(XML.parseXml(setupFileName, false, true));
    Map<String, String> ev = System.getenv();

    // logLevel may be: warning, info(default), all
    EDStatic.setLogLevel(getSetupEVString(setup, ev, "logLevel", DEFAULT_logLevel));

    usePrometheusMetrics = getSetupEVBoolean(setup, ev, "usePrometheusMetrics", true);

    bigParentDirectory = getSetupEVNotNothingString(setup, ev, "bigParentDirectory", "");
    bigParentDirectory = File2.addSlash(bigParentDirectory);
    Path bpd = Path.of(bigParentDirectory);
    if (!bpd.isAbsolute()) {
      if (!File2.isDirectory(bigParentDirectory)) {
        bigParentDirectory = webInfParentDirectory + bigParentDirectory;
      }
    }
    Test.ensureTrue(
        File2.isDirectory(bigParentDirectory),
        "bigParentDirectory (" + bigParentDirectory + ") doesn't exist.");

    // Mqtt Brokder directories
    mqttConfigFolder = getSetupEVString(setup, ev, "mqttConfigFolder", "");
    mqttDataFolder = getSetupEVString(setup, ev, "mqttDataFolder", "");
    mqttExtensionsFolder = getSetupEVString(setup, ev, "mqttExtensionsFolder", "");

    // email  (do early on so email can be sent if trouble later in this method)
    emailSmtpHost = getSetupEVString(setup, ev, "emailSmtpHost", (String) null);
    emailSmtpPort = getSetupEVInt(setup, ev, "emailSmtpPort", 25);
    emailUserName = getSetupEVString(setup, ev, "emailUserName", (String) null);
    emailPassword = getSetupEVString(setup, ev, "emailPassword", (String) null);
    emailProperties = getSetupEVString(setup, ev, "emailProperties", (String) null);
    emailFromAddress = getSetupEVString(setup, ev, "emailFromAddress", (String) null);
    emailEverythingToCsv = getSetupEVString(setup, ev, "emailEverythingTo", ""); // won't be null
    emailDailyReportToCsv =
        Optional.ofNullable(getSetupEVString(setup, ev, "emailDailyReportTo", (String) null))
            .orElse(getSetupEVString(setup, ev, "emailDailyReportsTo", ""));
    emailIsActive = // ie if actual emails will be sent
        String2.isSomething(emailSmtpHost)
            && emailSmtpPort > 0
            && String2.isSomething(emailUserName)
            && String2.isSomething(emailPassword)
            && String2.isEmailAddress(emailFromAddress);

    String tsar[] = String2.split(emailEverythingToCsv, ',');
    if (emailEverythingToCsv.length() > 0)
      for (String s : tsar)
        if (!String2.isEmailAddress(s)
            || s.startsWith("your.")) // prohibit the default email addresses
        throw new RuntimeException(
              "setup.xml error: invalid email address=" + s + " in <emailEverythingTo>.");
    emailSubscriptionsFrom = tsar.length > 0 ? tsar[0] : ""; // won't be null

    tsar = String2.split(emailDailyReportToCsv, ',');
    if (emailDailyReportToCsv.length() > 0) {
      for (String s : tsar)
        if (!String2.isEmailAddress(s)
            || s.startsWith("your.")) // prohibit the default email addresses
        throw new RuntimeException(
              "setup.xml error: invalid email address=" + s + " in <emailDailyReportTo>.");
    }

    // *** set up directories  //all with slashes at end
    // before 2011-12-30, was fullDatasetInfoDirectory datasetInfo/; see conversion below
    fullDatasetDirectory = bigParentDirectory + "dataset/";
    fullFileVisitorDirectory = fullDatasetDirectory + "_FileVisitor/";
    File2.deleteAllFiles(
        fullFileVisitorDirectory); // no temp file list can be active at ERDDAP restart
    fullCacheDirectory = bigParentDirectory + "cache/";
    fullDecompressedDirectory = bigParentDirectory + "decompressed/";
    fullDecompressedGenerateDatasetsXmlDirectory =
        bigParentDirectory + "decompressed/GenerateDatasetsXml/";
    fullResetFlagDirectory = bigParentDirectory + "flag/";
    fullBadFilesFlagDirectory = bigParentDirectory + "badFilesFlag/";
    fullHardFlagDirectory = bigParentDirectory + "hardFlag/";
    fullLogsDirectory = bigParentDirectory + "logs/";
    fullCopyDirectory = bigParentDirectory + "copy/";
    fullLuceneDirectory = bigParentDirectory + "lucene/";

    Test.ensureTrue(
        File2.isDirectory(fullPaletteDirectory),
        "fullPaletteDirectory (" + fullPaletteDirectory + ") doesn't exist.");
    errorInMethod =
        "ERROR while creating directories: "; // File2.makeDir throws exception if failure
    File2.makeDirectory(fullPublicDirectory); // make it, because Git doesn't track empty dirs
    File2.makeDirectory(fullDatasetDirectory);
    File2.makeDirectory(fullCacheDirectory);
    File2.makeDirectory(fullDecompressedDirectory);
    File2.makeDirectory(fullDecompressedGenerateDatasetsXmlDirectory);
    File2.makeDirectory(fullResetFlagDirectory);
    File2.makeDirectory(fullBadFilesFlagDirectory);
    File2.makeDirectory(fullHardFlagDirectory);
    File2.makeDirectory(fullLogsDirectory);
    File2.makeDirectory(fullCopyDirectory);
    File2.makeDirectory(fullLuceneDirectory);

    String2.log(
        "bigParentDirectory="
            + bigParentDirectory
            + String2.lineSeparator
            + "webInfParentDirectory="
            + webInfParentDirectory);

    // make some subdirectories of fullCacheDirectory
    // '_' distinguishes from dataset cache dirs
    errorInMethod = "ERROR while creating directories: ";
    fullCptCacheDirectory = fullCacheDirectory + "_cpt/";
    fullPlainFileNcCacheDirectory = fullCacheDirectory + "_plainFileNc/";
    fullSgtMapTopographyCacheDirectory = fullCacheDirectory + "_SgtMapTopography/";
    fullTestCacheDirectory = fullCacheDirectory + "_test/";
    fullWmsCacheDirectory =
        fullCacheDirectory + "_wms/"; // for all-datasets WMS and subdirs for non-data layers
    File2.makeDirectory(fullCptCacheDirectory);
    File2.makeDirectory(fullPlainFileNcCacheDirectory);
    File2.makeDirectory(fullSgtMapTopographyCacheDirectory);
    File2.makeDirectory(fullTestCacheDirectory);
    File2.makeDirectory(fullWmsCacheDirectory);
    File2.makeDirectory(fullWmsCacheDirectory + "Land"); // includes LandMask
    File2.makeDirectory(fullWmsCacheDirectory + "Coastlines");
    File2.makeDirectory(fullWmsCacheDirectory + "LakesAndRivers");
    File2.makeDirectory(fullWmsCacheDirectory + "Nations");
    File2.makeDirectory(fullWmsCacheDirectory + "States");

    // get other info from setup.xml
    errorInMethod = "ERROR while reading " + setupFileName + ": ";
    baseUrl = getSetupEVNotNothingString(setup, ev, "baseUrl", errorInMethod);
    baseHttpsUrl =
        getSetupEVString(
            setup, ev, "baseHttpsUrl", "(not specified)"); // not "" (to avoid relative urls)
    categoryAttributes =
        String2.split(getSetupEVNotNothingString(setup, ev, "categoryAttributes", ""), ',');
    int nCat = categoryAttributes.length;
    categoryAttributesInURLs = new String[nCat];
    categoryIsGlobal = new boolean[nCat]; // initially all false
    for (int cati = 0; cati < nCat; cati++) {
      String cat = categoryAttributes[cati];
      if (cat.startsWith("global:")) {
        categoryIsGlobal[cati] = true;
        cat = cat.substring(7);
        categoryAttributes[cati] = cat;
      } else if (cat.equals("institution")) { // legacy special case
        categoryIsGlobal[cati] = true;
      }
      categoryAttributesInURLs[cati] = String2.modifyToBeFileNameSafe(cat);
    }
    variableNameCategoryAttributeIndex = String2.indexOf(categoryAttributes, "variableName");

    String wmsActiveString = getSetupEVString(setup, ev, "wmsActive", "");
    wmsActive = !String2.isSomething(wmsActiveString) || String2.parseBoolean(wmsActiveString);
    wmsSampleDatasetID = getSetupEVString(setup, ev, "wmsSampleDatasetID", wmsSampleDatasetID);
    wmsSampleVariable = getSetupEVString(setup, ev, "wmsSampleVariable", wmsSampleVariable);
    wmsSampleBBox110 = getSetupEVString(setup, ev, "wmsSampleBBox110", wmsSampleBBox110);
    wmsSampleBBox130 = getSetupEVString(setup, ev, "wmsSampleBBox130", wmsSampleBBox130);
    wmsSampleTime = getSetupEVString(setup, ev, "wmsSampleTime", wmsSampleTime);

    adminInstitution = getSetupEVNotNothingString(setup, ev, "adminInstitution", errorInMethod);
    adminInstitutionUrl =
        getSetupEVNotNothingString(setup, ev, "adminInstitutionUrl", errorInMethod);
    adminIndividualName =
        getSetupEVNotNothingString(setup, ev, "adminIndividualName", errorInMethod);
    adminPosition = getSetupEVNotNothingString(setup, ev, "adminPosition", errorInMethod);
    adminPhone = getSetupEVNotNothingString(setup, ev, "adminPhone", errorInMethod);
    adminAddress = getSetupEVNotNothingString(setup, ev, "adminAddress", errorInMethod);
    adminCity = getSetupEVNotNothingString(setup, ev, "adminCity", errorInMethod);
    adminStateOrProvince =
        getSetupEVNotNothingString(setup, ev, "adminStateOrProvince", errorInMethod);
    adminPostalCode = getSetupEVNotNothingString(setup, ev, "adminPostalCode", errorInMethod);
    adminCountry = getSetupEVNotNothingString(setup, ev, "adminCountry", errorInMethod);
    adminEmail = getSetupEVNotNothingString(setup, ev, "adminEmail", errorInMethod);

    if (adminInstitution.startsWith("Your"))
      throw new RuntimeException("setup.xml error: invalid <adminInstitution>=" + adminInstitution);
    if (!adminInstitutionUrl.startsWith("http") || !String2.isUrl(adminInstitutionUrl))
      throw new RuntimeException(
          "setup.xml error: invalid <adminInstitutionUrl>=" + adminInstitutionUrl);
    if (adminIndividualName.startsWith("Your"))
      throw new RuntimeException(
          "setup.xml error: invalid <adminIndividualName>=" + adminIndividualName);
    // if (adminPosition.length() == 0)
    //    throw new RuntimeException("setup.xml error: invalid <adminPosition>=" + adminPosition);
    if (adminPhone.indexOf("999-999") >= 0)
      throw new RuntimeException("setup.xml error: invalid <adminPhone>=" + adminPhone);
    if (adminAddress.equals("123 Main St."))
      throw new RuntimeException("setup.xml error: invalid <adminAddress>=" + adminAddress);
    if (adminCity.equals("Some Town"))
      throw new RuntimeException("setup.xml error: invalid <adminCity>=" + adminCity);
    // if (adminStateOrProvince.length() == 0)
    //    throw new RuntimeException("setup.xml error: invalid <adminStateOrProvince>=" +
    // adminStateOrProvince);
    if (adminPostalCode.equals("99999"))
      throw new RuntimeException("setup.xml error: invalid <adminPostalCode>=" + adminPostalCode);
    // if (adminCountry.length() == 0)
    //    throw new RuntimeException("setup.xml error: invalid <adminCountry>=" + adminCountry);
    if (!String2.isEmailAddress(adminEmail)
        || adminEmail.startsWith("your.")) // prohibit default adminEmail
    throw new RuntimeException("setup.xml error: invalid <adminEmail>=" + adminEmail);

    accessConstraints = getSetupEVNotNothingString(setup, ev, "accessConstraints", errorInMethod);
    accessRequiresAuthorization =
        getSetupEVNotNothingString(setup, ev, "accessRequiresAuthorization", errorInMethod);
    fees = getSetupEVNotNothingString(setup, ev, "fees", errorInMethod);
    keywords = getSetupEVNotNothingString(setup, ev, "keywords", errorInMethod);

    awsS3OutputBucketUrl = getSetupEVString(setup, ev, "awsS3OutputBucketUrl", (String) null);
    if (!String2.isSomething(awsS3OutputBucketUrl)) awsS3OutputBucketUrl = null;

    if (awsS3OutputBucketUrl != null) {

      // ensure that it is valid
      awsS3OutputBucketUrl = File2.addSlash(awsS3OutputBucketUrl);
      String bro[] = String2.parseAwsS3Url(awsS3OutputBucketUrl);
      if (bro == null)
        throw new RuntimeException(
            "The value of <awsS3OutputBucketUrl> specified in setup.xml doesn't match this regular expression: "
                + String2.AWS_S3_REGEX());

      awsS3OutputBucket = bro[0];
      String region = bro[1];

      // build the awsS3OutputTransferManager
      awsS3OutputTransferManager = SSR.buildS3TransferManager(region);

      // note that I could set LifecycleRule(s) for the bucket via
      // awsS3OutputClient.putBucketLifecycleConfiguration
      // but LifecycleRule precision seems to be days, not e.g., minutes
      // So make my own system
    }

    // optional parameter to disable AWS Common Runtime
    useAwsCrt = getSetupEVBoolean(setup, ev, "useAwsCrt", true);
    useAwsAnonymous = getSetupEVBoolean(setup, ev, "useAwsAnonymous", false);

    units_standard = getSetupEVString(setup, ev, "units_standard", "UDUNITS");

    fgdcActive = getSetupEVBoolean(setup, ev, "fgdcActive", true);
    iso19115Active = getSetupEVBoolean(setup, ev, "iso19115Active", true);
    jsonldActive = getSetupEVBoolean(setup, ev, "jsonldActive", true);
    // until geoServicesRest is finished, it is always inactive
    geoServicesRestActive =
        false; // getSetupEVBoolean(setup, ev,          "geoServicesRestActive",      false);
    filesActive = getSetupEVBoolean(setup, ev, "filesActive", true);
    defaultAccessibleViaFiles =
        getSetupEVBoolean(
            setup, ev, "defaultAccessibleViaFiles", false); // false matches historical behavior
    dataProviderFormActive = getSetupEVBoolean(setup, ev, "dataProviderFormActive", true);
    outOfDateDatasetsActive = getSetupEVBoolean(setup, ev, "outOfDateDatasetsActive", true);
    politicalBoundariesActive = getSetupEVBoolean(setup, ev, "politicalBoundariesActive", true);
    wmsClientActive = getSetupEVBoolean(setup, ev, "wmsClientActive", true);
    enableMqttBroker = getSetupEVBoolean(setup, ev, "enableMqttBroker", false);

    // until SOS is finished, it is always inactive
    sosActive = false; //        sosActive                  = getSetupEVBoolean(setup, ev,
    // "sosActive",                  false);
    if (sosActive) {
      sosFeatureOfInterest =
          getSetupEVNotNothingString(setup, ev, "sosFeatureOfInterest", errorInMethod);
      sosStandardNamePrefix =
          getSetupEVNotNothingString(setup, ev, "sosStandardNamePrefix", errorInMethod);
      sosUrnBase = getSetupEVNotNothingString(setup, ev, "sosUrnBase", errorInMethod);

      // make the sosGmlName, e.g., https://coastwatch.pfeg.noaa.gov -> gov.noaa.pfeg.coastwatch
      sosBaseGmlName = baseUrl;
      int po = sosBaseGmlName.indexOf("//");
      if (po > 0) sosBaseGmlName = sosBaseGmlName.substring(po + 2);
      po = sosBaseGmlName.indexOf(":");
      if (po > 0) sosBaseGmlName = sosBaseGmlName.substring(0, po);
      StringArray sbgn = new StringArray(String2.split(sosBaseGmlName, '.'));
      sbgn.reverse();
      sosBaseGmlName = String2.toSVString(sbgn.toArray(), ".", false);
    }

    // until it is finished, it is always inactive
    wcsActive =
        false; // getSetupEVBoolean(setup, ev,          "wcsActive",                  false);

    authentication = getSetupEVString(setup, ev, "authentication", "");
    datasetsRegex = getSetupEVString(setup, ev, "datasetsRegex", ".*");
    drawLandMask = getSetupEVString(setup, ev, "drawLandMask", (String) null); // new name
    if (drawLandMask == null) // 2014-08-28 changed defaults below to "under". It will be in v1.48
    drawLandMask =
          getSetupEVString(
              setup, ev, "drawLand", DEFAULT_drawLandMask); // old name. DEFAULT...="under"
    int tdlm = SgtMap.drawLandMask_OPTIONS.indexOf(drawLandMask);
    if (tdlm < 1) drawLandMask = DEFAULT_drawLandMask; // "under"
    flagKeyKey = getSetupEVNotNothingString(setup, ev, "flagKeyKey", errorInMethod);
    if (flagKeyKey.toUpperCase().indexOf("CHANGE THIS") >= 0)
      // really old default: "A stitch in time saves nine. CHANGE THIS!!!"
      // current default:    "CHANGE THIS TO YOUR FAVORITE QUOTE"
      throw new RuntimeException(
          String2.ERROR
              + ": You must change the <flagKeyKey> in setup.xml to a new, unique, non-default value. "
              + "NOTE that this will cause the flagKeys used by your datasets to change. "
              + "Any subscriptions using the old flagKeys will need to be redone.");
    fontFamily = getSetupEVString(setup, ev, "fontFamily", "DejaVu Sans");
    graphBackgroundColor =
        new Color(
            String2.parseInt(
                getSetupEVString(
                    setup, ev, "graphBackgroundColor", "" + DEFAULT_graphBackgroundColorInt)),
            true); // hasAlpha
    googleClientID = getSetupEVString(setup, ev, "googleClientID", (String) null);
    orcidClientID = getSetupEVString(setup, ev, "orcidClientID", (String) null);
    orcidClientSecret = getSetupEVString(setup, ev, "orcidClientSecret", (String) null);
    googleEarthLogoFile =
        getSetupEVNotNothingString(setup, ev, "googleEarthLogoFile", errorInMethod);
    highResLogoImageFile =
        getSetupEVNotNothingString(setup, ev, "highResLogoImageFile", errorInMethod);
    listPrivateDatasets = getSetupEVBoolean(setup, ev, "listPrivateDatasets", false);
    logMaxSizeMB =
        Math2.minMax(1, 2000, getSetupEVInt(setup, ev, "logMaxSizeMB", 20)); // 2048MB=2GB

    // v2.00: these are now also in datasets.xml
    cacheMillis = getSetupEVInt(setup, ev, "cacheMinutes", DEFAULT_cacheMinutes) * 60000L;
    cacheClearMillis =
        getSetupEVInt(setup, ev, "cacheClearMinutes", DEFAULT_cacheMinutes / 4) * 60000L;
    touchThreadOnlyWhenItems = getSetupEVBoolean(setup, ev, "touchThreadOnlyWhenItems", true);
    taskCacheClear = getSetupEVBoolean(setup, ev, "taskCacheClear", true);
    useNcMetadataForFileTable = getSetupEVBoolean(setup, ev, "useNcMetadataForFileTable", true);
    backgroundCreateSubsetTables =
        getSetupEVBoolean(setup, ev, "backgroundCreateSubsetTables", true);
    lowMemCacheGbLimit = getSetupEVInt(setup, ev, "lowMemCacheGbLimit", DEFAULT_lowMemCacheGbLimit);
    loadDatasetsMinMillis =
        Math.max(
                1,
                getSetupEVInt(setup, ev, "loadDatasetsMinMinutes", DEFAULT_loadDatasetsMinMinutes))
            * 60000L;
    loadDatasetsMaxMillis =
        getSetupEVInt(setup, ev, "loadDatasetsMaxMinutes", DEFAULT_loadDatasetsMaxMinutes) * 60000L;
    loadDatasetsMaxMillis = Math.max(loadDatasetsMinMillis * 2, loadDatasetsMaxMillis);
    partialRequestMaxBytes =
        getSetupEVInt(setup, ev, "partialRequestMaxBytes", DEFAULT_partialRequestMaxBytes);
    partialRequestMaxCells =
        getSetupEVInt(setup, ev, "partialRequestMaxCells", DEFAULT_partialRequestMaxCells);
    unusualActivity = getSetupEVInt(setup, ev, "unusualActivity", DEFAULT_unusualActivity);
    showLoadErrorsOnStatusPage =
        getSetupEVBoolean(
            setup, ev, "showLoadErrorsOnStatusPage", DEFAULT_showLoadErrorsOnStatusPage);

    lowResLogoImageFile =
        getSetupEVNotNothingString(setup, ev, "lowResLogoImageFile", errorInMethod);
    quickRestart = getSetupEVBoolean(setup, ev, "quickRestart", true);
    passwordEncoding = getSetupEVString(setup, ev, "passwordEncoding", "UEPSHA256");
    searchEngine = getSetupEVString(setup, ev, "searchEngine", "original");

    subscribeToRemoteErddapDataset =
        getSetupEVBoolean(setup, ev, "subscribeToRemoteErddapDataset", true);
    subscriptionSystemActive = getSetupEVBoolean(setup, ev, "subscriptionSystemActive", true);
    convertersActive = getSetupEVBoolean(setup, ev, "convertersActive", true);
    useSaxParser = getSetupEVBoolean(setup, ev, "useSaxParser", false);
    publishMqttNotif = getSetupEVBoolean(setup, ev, "publishMqttNotif", false);
    enableEnvParsing = getSetupEVBoolean(setup, ev, "enableEnvParsing", true);
    updateSubsRssOnFileChanges = getSetupEVBoolean(setup, ev, "updateSubsRssOnFileChanges", true);
    useEddReflection = getSetupEVBoolean(setup, ev, "useEddReflection", true);
    enableCors = getSetupEVBoolean(setup, ev, "enableCors", false);
    corsAllowHeaders =
        getSetupEVString(setup, ev, "corsAllowHeaders", CorsResponseFilter.DEFAULT_ALLOW_HEADERS);
    corsAllowOrigin =
        String2.split(
            String2.toLowerCase(getSetupEVString(setup, ev, "corsAllowOrigin", (String) null)),
            ',');
    useHeadersForUrl = getSetupEVBoolean(setup, ev, "useHeadersForUrl", true);
    slideSorterActive = getSetupEVBoolean(setup, ev, "slideSorterActive", true);
    variablesMustHaveIoosCategory =
        getSetupEVBoolean(setup, ev, "variablesMustHaveIoosCategory", true);
    warName = getSetupEVString(setup, ev, "warName", "erddap");
    includeNcCFSubsetVariables = getSetupEVBoolean(setup, ev, "includeNcCFSubsetVariables", false);
    ncHeaderMakeFile = getSetupEVBoolean(setup, ev, "ncHeaderMakeFile", false);
    useHtmlTemplates = getSetupEVBoolean(setup, ev, "useHtmlTemplates", true);
    useSisISO19115 = getSetupEVBoolean(setup, ev, "useSisISO19115", false);
    useSisISO19139 = getSetupEVBoolean(setup, ev, "useSisISO19139", false);
    generateCroissantSchema = getSetupEVBoolean(setup, ev, "generateCroissantSchema", true);
    deploymentInfo = getSetupEVString(setup, ev, "deploymentInfo", "");
    // Mqtt flags initialization
    mqttServerHost = getSetupEVString(setup, ev, "mqttServerHost", DEFAULT_MQTT_HOST);
    mqttServerPort = getSetupEVInt(setup, ev, "mqttServerPort", DEFAULT_MQTT_PORT);
    mqttClientId = getSetupEVString(setup, ev, "mqttClientId", DEFAULT_MQTT_CLIENT);
    mqttUserName = getSetupEVString(setup, ev, "mqttUserName", "");
    mqttPassword = getSetupEVString(setup, ev, "mqttPassword", "");
    mqttSsl = getSetupEVBoolean(setup, ev, "mqttSsl", DEFAULT_SSL);
    mqttKeepAlive = getSetupEVInt(setup, ev, "mqttKeepAlive", DEFAULT_KEEP_ALIVE);
    mqttCleanStart = getSetupEVBoolean(setup, ev, "mqttCleanStart", DEFAULT_CLEAN_START);
    mqttSessionExpiry = getSetupEVInt(setup, ev, "mqttSessionExpiry", DEFAULT_SESSION_EXPIRY);
    mqttConnectionTimeout =
        getSetupEVInt(setup, ev, "mqttConnectionTimeout", DEFAULT_CONNECTION_TIMEOUT);
    mqttAutomaticReconnect =
        getSetupEVBoolean(setup, ev, "mqttAutomaticReconnect", DEFAULT_AUTO_RECONNECT);

    copyContentImagesToWebApps();
    // ensure images exist and get their sizes
    Image tImage = Image2.getImage(imageDir + lowResLogoImageFile, 10000, false);
    lowResLogoImageFileWidth = tImage.getWidth(null);
    lowResLogoImageFileHeight = tImage.getHeight(null);
    tImage = Image2.getImage(imageDir + highResLogoImageFile, 10000, false);
    highResLogoImageFileWidth = tImage.getWidth(null);
    highResLogoImageFileHeight = tImage.getHeight(null);
    tImage = Image2.getImage(imageDir + googleEarthLogoFile, 10000, false);
    googleEarthLogoFileWidth = tImage.getWidth(null);
    googleEarthLogoFileHeight = tImage.getHeight(null);

    lazyInitializeStatics();
  }

  private void copyContentImagesToWebApps() {
    // copy all <contentDirectory>images/ (and subdirectories) files to imageDir (and
    // subdirectories)
    String tFiles[] =
        RegexFilenameFilter.recursiveFullNameList(contentDirectory + "images/", ".+", false);
    String2.log("contentDirectory: " + contentDirectory);
    for (String file : tFiles) {
      int tpo = file.indexOf("/images/");
      String2.log("copying file: " + file);
      if (tpo < 0) tpo = file.indexOf("\\images\\");
      if (tpo < 0) {
        String2.log("'/images/' not found in images/ file: " + file);
        continue;
      }
      String tName = file.substring(tpo + 8);
      File2.copy(contentDirectory + "images/" + tName, imageDir + tName);
    }
    // copy all <contentDirectory>cptfiles/ files to cptfiles
    tFiles = RegexFilenameFilter.list(contentDirectory + "cptfiles/", ".+\\.cpt"); // not recursive
    for (String tFile : tFiles) {
      File2.copy(contentDirectory + "cptfiles/" + tFile, fullPaletteDirectory + tFile);
    }
  }

  private void lazyInitializeStatics() {
    FileVisitorDNLS.FILE_VISITOR_DIRECTORY = fullFileVisitorDirectory;
    SgtMap.drawPoliticalBoundaries = politicalBoundariesActive;
  }

  /**
   * This gets a string from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private String getSetupEVString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      return value;
    }
    return setup.getString(paramName, tDefault);
  }

  /**
   * This gets a boolean from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private boolean getSetupEVBoolean(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, boolean tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
    if (value != null) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      return String2.parseBoolean(value);
    }
    return setup.getBoolean(paramName, tDefault);
  }

  /**
   * This gets an int from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param tDefault the default value
   * @return the desired value (or the default if it isn't defined anywhere)
   */
  private int getSetupEVInt(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, int tDefault) {
    String value = ev.get("ERDDAP_" + paramName);
    if (value != null) {
      int valuei = String2.parseInt(value);
      if (valuei < Integer.MAX_VALUE) {
        String2.log("got " + paramName + " from ERDDAP_" + paramName);
        return valuei;
      }
    }
    return setup.getInt(paramName, tDefault);
  }

  /**
   * This gets a string from setup.xml or environmentalVariables (preferred).
   *
   * @param setup from setup.xml
   * @param ev from System.getenv()
   * @param paramName If present in ev, it will be ERDDAP_paramName.
   * @param errorInMethod the start of an Error message
   * @return the desired value
   * @throws RuntimeException if there is no value for key
   */
  private String getSetupEVNotNothingString(
      ResourceBundle2 setup, Map<String, String> ev, String paramName, String errorInMethod) {
    String value = ev.get("ERDDAP_" + paramName);
    if (String2.isSomething(value)) {
      String2.log("got " + paramName + " from ERDDAP_" + paramName);
      return value;
    }
    return setup.getNotNothingString(paramName, errorInMethod);
  }
}
