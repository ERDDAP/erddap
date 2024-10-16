
import argostranslate.package
import argostranslate.translate
from lxml import etree
# This assumes you are running it from the project base directory not the translation directory.

# Notes for future use. I believe this system may have some edge case problems (adding spaces to href).
# There's an extension for argostranslate that is supposed to help handle translating html:
# pip install translatehtml
# However I ran into other issues with it. Since I'm not re-translating all existing messages,
# this problem is puntable for now. When translating HTML it will be worth more careful checking
# of the output to make sure it's good.

# NOTES These are machine translations, and so are inherently imperfect. The use of ERDDAP and
# domain-related jargon makes it even harder. Most of the translated text hasn't even be
# read/proofed by a human. This doesn't attempt to translate messages from lower level code,
# e.g., PrimitiveArray, String2, Math2, HtmlWidgets, MustBe. You would have to add a complex system
# of setting arrays for each stock error message and then passing languageCode into to each method
# so if an error occurred, the translated message would be generated. But even that is trouble if a
# mid-level procedure looks for a specific static error message. Even some ERDDAP-related things like
# accessibleVia... are just English. Most error messages are just English, although the start of the
# message, e.g., "Query Error" may appear translated and in English. Several tags used on the 3rd(?)
# page of the Data Provider Form (e.g., dpt_standardName) maybe shouldn't be translated since they
# refer to a specific CF or ACDD attribute (and the explanation is translated). Several tags can be
# specified in datasets.xml (e.g., &lt;startHeadHtml5&gt;, &lt;standardLicense&lt;). The definitions
# there only affect the English language version. This is not ideal. Many tags get translated by this
# system but only the English version is used, e.g., advl_datasetID. See "NOT TRANSLATED" in
# EDStatic.java for most of these. Language=0 must be English ("en"). Many places in the code
# specify EDStatic....Ar[0] so that only the English version of the tag is used.
# 
# FUTURE? Things to think about. Make it so that, after running Translate, all changes for each
# language are captured separately, so an editor for that language can review them. Disable/change
# the system for removing space after odd '"' and before even '"'? Or just fix problems in tags
# where it has problems (usually some root cause). Need to note max line length in non-html tags?
# Then apply to translated text? There is no dontTranslate for non-html. Is it needed? How do it?
# Do more testing of non-html, e.g., {0}, line breaks, etc. Is there a way to force <submit> to be
# the computer-button meaning? E.g., I think German should be Senden (not "einreichen"). Should the
# language list be the translated language names? Translate EDStatic.errorFromDataSource and use it
# with bilingual()?

from_code = "en"
message_format_entities = ["{0}", "{1}", "''"]
start_no_translate = "<span translate=\"no\">"
end_no_translate = "</span>"
escaped_amp = "&amp;"
util_dir = "src/main/resources/gov/noaa/pfel/erddap/util/"
translate_count = 0
translate_limit = -1 # set this to 0/-1 to translate all

# Download and install Argos Translate package
argostranslate.package.update_package_index()
available_packages = argostranslate.package.get_available_packages()

language_code_list  = [
    # "en", # Don't do en->en translation
    "bn",
    "zh", # was "zh-CN"
    "zt", # was "zh-TW"
    "cs",
    "da",
    "nl",
    "fi",
    "fr",
    "de",
    "el",
    #"gu", # not supported in argos
    "hi",
    "hu",
    "id",
    "ga",
    "it",
    "ja",
    "ko",
    #"mr", # not supported in argos
    "nb", # was "no"
    "pl",
    "pt",
    #"pa", # not supported in argos
    "ro",
    "ru",
    "es",
    #"sw", # not supported in argos
    "sv",
    "tl",
    "th",
    "tr",
    "uk",
    "ur",
    #"vi" # not supported in argos
]

# For testing
#language_code_list  = [
#    "fr",
#]

do_not_translate_tags = [
    # These are tags that shouldn't be translated, minus the surrounding < and >.
    # all tags that match the regular expresion:  <EDDGrid.*Example> ,
    "EDDGridErddapUrlExample",
    "EDDGridIdExample",
    "EDDGridDimensionExample",
    "EDDGridNoHyperExample",
    "EDDGridDimNamesExample",
    "EDDGridDataTimeExample",
    "EDDGridDataValueExample",
    "EDDGridDataIndexExample",
    "EDDGridGraphExample",
    "EDDGridMapExample",
    "EDDGridMatlabPlotExample",
    # all tags that match the regular expression:  <EDDTable.*Example> ,
    "EDDTableErddapUrlExample",
    "EDDTableIdExample",
    "EDDTableVariablesExample",
    "EDDTableConstraintsExample",
    "EDDTableDataTimeExample",
    "EDDTableDataValueExample",
    "EDDTableGraphExample",
    "EDDTableMapExample",
    "EDDTableMatlabPlotExample",
    # Other
    "admKeywords",
    "admSubsetVariables",
    "advl_datasetID",
    "advr_dataStructure",
    "advr_cdm_data_type",
    "advr_class",
    "DEFAULT_commonStandardNames",
    "EDDIso19115",
    "EDDTableFromHttpGetDatasetDescription",
    "EDDTableFromHttpGetAuthorDescription",
    "EDDTableFromHttpGetTimestampDescription",
    "extensionsNoRangeRequests",
    "htmlTableMaxMB",
    "imageWidths",
    "imageHeights",
    "inotifyFixCommands",
    "legal",
    "palettes",
    "pdfWidths",
    "pdfHeights",
    "questionMarkImageFile",
    "signedToUnsignedAttNames",
    "sparqlP01toP02pre",
    "sparqlP01toP02post",
    "standardizeUdunits",
    "startBodyHtml5",
    "startHeadHtml5",
    "theShortDescriptionHtml",
    "ucumToUdunits",
    "udunitsToUcum",
    "updateUrls",
    # don't translate the comment before these opening tags (hence, no leading '/')
    "admKeywords",
    "advl_datasetID",
    "startBodyHtml5"
]

dont_translate_strings = [
    # !!!ESSENTIAL: if a short phrase (DAP) is in a long phrase (ERDDAP), the long phrase must come
    # first.
    # main() below has a test for this.
    # phrases in quotes
    "\" since \"",
    "\"{0}\"",
    "\"{1}\"",
    "\"{count}\"",
    "\"\*\*\"",
    "\"[in_i]\"",
    "\"[todd'U]\"",
    "\"%{vol}\"",
    "\"&amp;units=...\"",
    "\"&C;\"",
    "\"&micro;\"", # otherwise it is often dropped from the translation.   Only used in one place
    # in messages.xml.
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

    # lots of things that shouldn't be translated are within <kbd> and <strong>
    "<kbd>\"long_name=Sea Surface Temperature\"</kbd>",
    "<kbd>{0} : {1}</kbd>",
    "<kbd>{0}</kbd>",
    "<kbd>{1} : {2}</kbd>",
    "<kbd>{41008,41009,41010}</kbd>",
    "<kbd>#1</kbd>",
    # there are some <kbd>&pseudoEntity;</kbd>  The translation system REQUIRES that "pseudoEntity"
    # only use [a-zA-Z0-9].
    "<kbd>&adminEmail;</kbd>",
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
    "<kbd>https://www.yourWebSite.com?department=R%26D&amp;action=rerunTheModel</kbd>",
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
    # "<kbd>(last)</kbd>" is above
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
    # "<kbd><i>units</i> since <i>basetime</i></kbd>" is above
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

    # All psuedo entities (used for param names, proper nouns, substitutions)
    #  MUST be here by themselves
    #  OR in <kbd>&pseudoEntity;</kbd> above
    #  so code in postProcessHtml works correctly.
    # postProcessHtml() REQUIRES that "pseudoEntity" only use [a-zA-Z0-9].
    "&acceptEncodingHtml;",
    "&acceptEncodingHtmlh3tErddapUrl;",
    "&adminContact;",
    "&advancedSearch;",
    "&algorithm;",
    "&bgcolor;",
    "&BroughtToYouBy;",
    "&C;",
    # above is <kbd>&category;</kbd>
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

    # these <tag>s were gathered by code in main that matches a regex in messages.xml
    "&lt;/att&gt;",
    "&lt;addAttributes&gt;",
    "&lt;subsetVariables&gt;",
    "&lt;time_precision&gt;",
    "&lt;units_standard&gt;",
    "&lt;updateUrls&gt;",
    "&lt;",
    "&makeAGraphListRef;",
    "&makeAGraphRef;",
    "&nbsp;",
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
    # above is <kbd>&safeEmail;</kbd>
    "&sampleUrl;",
    "&secondPart;",
    # above is <kbd>&searchButton;</kbd>
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

    # things that are never translated
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
    ".jsonlCSV", # must be after the .jsonlCSV1
    ".jsonlKVP",
    ".json", # must be after the longer versions
    ".kml",
    ".mat",
    ".nccsv",
    ".nc", # must be after .nccsv
    ".tar",
    ".tgz",
    ".tsv",
    ".xhtml",
    ".zip",
    ".z",

    # text (proper nouns, parameter names, phrases, etc) that shouldn't be translated
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
    # "DAP",  is below, after OPeNDAP
    "d, day, days,",
    "datasetID/variable/algorithm/nearby", # before datasetID
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
    "Earth Science &amp; Atmosphere &amp; Atmospheric Pressure &amp; Atmospheric Pressure Measurements",
    "Earth Science &amp; Atmosphere &amp; Atmospheric Pressure &amp; Sea Level Pressure",
    "Earth Science &amp; Atmosphere &amp; Atmospheric Pressure &amp; Static Pressure",
    "EDDGrid",
    "encodeURIComponent()",
    "endTime",
    "ERDDAP", # before ERD and DAP
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
    # "gzip", #is below after x-gzip
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
    "now-7days", # before now-
    "now-",
    "ODV .txt",
    "OGC",
    "OOSTethys",
    "OPeNDAP",
    "DAP", # out of place, so that it is after ERDDAP and OPeNDAP
    "OpenID",
    "OpenLayers",
    "OpenSearch",
    "Oracle",
    "orderBy(\"stationID, time\")", # before orderBy
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
    "orderBy", # must be after the longer versions
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
    "RESTful", # before REST
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
    "SWFSC", # before WFS
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
    "gzip", # must be after x-gzip
    "yr, yrs, year, or years",
    "yyyy-MM-ddTHH:mm:ssZ", # before yyyy-MM-dd
    "yyyy-MM-dd",
    "Zulu"
]

def find_and_install_langauge_package(target):
    package_to_install = next(
        filter(
            lambda x: x.from_code == from_code and x.to_code == target, available_packages
        )
    )
    argostranslate.package.install_from_path(package_to_install.download())

def is_index_in_tag(string, index, opening_tag, closing_tag):
    """
    Checks if a given index in a string is within a specific tag.

    Args:
        string: The string to search.
        index: The index to check.
        opening_tag: The opening tag (e.g., "<div>").
        closing_tag: The closing tag (e.g., "</div>").

    Returns:
        bool: True if the index is within a tag, False otherwise.
    """

    tag_stack = []
    for i, char in enumerate(string):
        if i == index:
            return len(tag_stack) != 0
        if char == opening_tag[0]:
            if string[i:i+len(opening_tag)] == opening_tag:
                tag_stack.append(i)
        if char == closing_tag[0]:
            if len(tag_stack) > 0 and string[i:i+len(closing_tag)] == closing_tag:
                tag_stack.pop()

    return False 

def preprocess_html(text):
    for no_translate in dont_translate_strings:
        index = 0
        while index > -1:
            index = text.find(no_translate, index)
            if index > -1:
                if not is_index_in_tag(text, index, start_no_translate, end_no_translate) and not is_index_in_tag(text, index, "<a", ">") and not is_index_in_tag(text, index, "<img", ">"):
                    text = text[0:index] + start_no_translate + no_translate + end_no_translate + text[index+len(no_translate):]
                    index = index + len(no_translate) + len(start_no_translate) + len(end_no_translate)
                else:
                    # this was within a no translate span, move on.
                    index = index + len(no_translate)
    return text

def postprocess_html(text):
    index = 0
    while index > -1:
        index = text.find(escaped_amp, index)
        if index > -1:
            # Unescape &amp; that are within a no translate span.
            if is_index_in_tag(text, index, start_no_translate, end_no_translate):
                text = text[0:index] + "&" + text[index+len(escaped_amp):]
                index = index + 1
            else:
                index = index + len(escaped_amp)
    # text = text.replace(start_no_translate + "&amp;", start_no_translate + "&")
    # text = text.replace(start_no_translate + "<kbd>&amp;", start_no_translate + "<kbd>&")
    index = 0
    while index > -1:
        index = text.find(start_no_translate, index)
        end_span = text.find(end_no_translate, index)
        if index > -1 and end_span > -1:
            text = text[0:index] + text[index+len(start_no_translate):end_span] + text[end_span+len(end_no_translate):]
    return text

def escape_text(text):
    text = text.replace("&", "&amp;")
    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    text = text.replace('"', "&quot;")
    text = text.replace("'", "&#39;")
    return text

def translate_tag(text, to_code, key):
    message_format = not key.startswith("comment") and any(entity in text for entity in message_format_entities)
    if message_format:
        text = text.replace("''", "'")
    if text.startswith("<![CDATA["):
        text = preprocess_html(text)
        translated_text = argostranslate.translate.translate(text[9:-3], from_code=from_code, to_code=to_code)
        translated_text = "<![CDATA[\n" + translated_text + "\n]]>"
        translated_text = postprocess_html(translated_text)
    else:
        translated_text = argostranslate.translate.translate(text, from_code=from_code, to_code=to_code)

    if message_format:
        translated_text = translated_text.replace("''", "'")
        translated_text = translated_text.replace("'", "''")

    return translated_text

def make_tag_dict(path):
    tags_dict = {}
    tree = etree.parse(path, etree.XMLParser(strip_cdata=False))
    root = tree.getroot()
    for i in range(len(root)):
    # for element in root.iter():
        element = root[i]
        if isinstance(element.tag, str):
            if "<![CDATA[" in str(etree.tostring(element)):
                tags_dict[element.tag] = "<![CDATA[" + element.text + "]]>"
            else:
                tags_dict[element.tag] = element.text
        else:
            if i + 1 < len(root) and isinstance(root[i+1].tag, str):
                tags_dict["comment" + root[i+1].tag] = element.text
            elif i > 0 and isinstance(root[i-1].tag, str):
                tags_dict["comment_back" + root[i-1].tag] = element.text
            else:
                tags_dict["comment_untagged_" + str(i)] = element.text
    return tags_dict

old_tags_dict = make_tag_dict(util_dir + "translatedMessages/messagesOld.xml")
messages_dict = make_tag_dict(util_dir + "messages.xml")

for lang_code in language_code_list:
    code_for_filename = lang_code
    if lang_code == "zh":
        code_for_filename = "zh-CN"
    if lang_code == "zt":
        code_for_filename = "zh-TW"
    if lang_code == "nb":
        code_for_filename = "no"
    print("Translating to language: " + lang_code)
    find_and_install_langauge_package(lang_code)

    translated_tag_dict = {}
    try:
        translated_tag_dict = make_tag_dict(util_dir + "translatedMessages/messages-" + code_for_filename + ".xml")
    except Exception as e:
        print(f"Read file error: {e}")

    for key in messages_dict.keys():
        if translate_count >= translate_limit and translate_limit > 0:
            break
        to_translate = messages_dict[key]
        if not to_translate or to_translate.strip() == "":
            translated_tag_dict[key]=""
        elif key == "langCode":
            translated_tag_dict[key] = lang_code
        elif key in do_not_translate_tags:
            translated_tag_dict[key] = to_translate
        elif key in old_tags_dict and key in translated_tag_dict and messages_dict[key] == old_tags_dict[key]:
            # already translated and not modified
            continue
        else:
            # do the translation
            print("translating key: " + key)
            print(str(key in old_tags_dict) + " " + str(key in translated_tag_dict))
            if key in old_tags_dict and key in translated_tag_dict:
                print(str(messages_dict[key] == old_tags_dict[key]))
            translated_tag_dict[key] = translate_tag(to_translate, lang_code, key)
        translate_count = translate_count + 1
    with open(util_dir + "translatedMessages/messages-" + code_for_filename + ".xml", "w", encoding='utf-8') as translated_file:
        # Writing data to a file
        translated_file.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        translated_file.write("<erddapMessages>")

        # Iterate over the messages keys to have text in the same order as the original messages file.
        for key in messages_dict.keys():
            to_write = translated_tag_dict[key]
            if not to_write:
                to_write = ""
            if key.startswith("comment"):
                translated_file.write("\n<!--" + to_write + "-->\n")
            else:
                if not to_write.startswith("<![CDATA["):
                    to_write = escape_text(to_write)
                translated_file.write("<" + key + ">" + to_write + "</" + key + ">\n")
        translated_file.write("</erddapMessages>\n")
        


