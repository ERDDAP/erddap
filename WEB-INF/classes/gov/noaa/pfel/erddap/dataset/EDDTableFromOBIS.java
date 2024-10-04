/*
 * EDDTableFromOBIS Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.DigirHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.EDDTableFromOBISHandler;
import gov.noaa.pfel.erddap.handlers.SaxHandlerClass;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.util.Arrays;
import java.util.HashSet;

/**
 * This class represents a table of data from a DiGIR/OBIS source. See
 * gov.noaa.pfel.coastwatch.pointdata.DigirHelper for more information on DiGIR and OBIS.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-10-22
 */
@SaxHandlerClass(EDDTableFromOBISHandler.class)
public class EDDTableFromOBIS extends EDDTable {

  protected String sourceCode;
  protected int nFixedVariables;

  public static final String STANDARD_INFO_URL = "http://www.iobis.org";

  /**
   * license modified slightly from http://www.iobis.org/data/policy/citation/ . Note
   * &amp;sourceUrl; and &amp;creator_email; are used in constructor to customize the license.
   */
  public static final String OBIS_LICENSE =
      "By using OBIS data, I agree that, in any publication or presentation\n"
          + "of any sort based wholly or in part on such data, I will:\n"
          + "\n"
          + "1. Acknowledge the use of specific records from contributing databases\n"
          + "in the form appearing in the GLOBAL 'citation' attribute thereof (if any);\n"
          + "and acknowledge the use of the OBIS facility in one of the following\n"
          + "prescribed forms:\n"
          + "\n"
          + "For OBIS website:\n"
          + "\n"
          + "Ocean Biogeographic Information System. [date accessed]\n"
          + "&sourceUrl;\n"
          + "\n"
          + "For data used:\n"
          + "\n"
          + "Author, initials. Database title. Retrieved [date accessed] from\n"
          + "&sourceUrl;\n"
          + "\n"
          + "Example:\n"
          + "\n"
          + "Stocks, K. SeamountsOnline: an online information system for seamount\n"
          + "biology. Version 3.1. Retrieved [date accessed] from\n"
          + "&sourceUrl;.\n"
          + "\n"
          + "2. For information purposes, provide to &creator_email;\n"
          + "the full citation of any publication I make (printed or electronic)\n"
          + "that cites OBIS or any constituent part.\n"
          + "\n"
          + "3. Recognize the limitations of data in OBIS:\n"
          + "OBIS is comparable to a scientific journal that makes data freely\n"
          + "available on the internet. Thus the geographic and taxonomic scope,\n"
          + "and quantity of data provided, depend on the scientists and\n"
          + "organizations that provide data. However, in contrast to data in a\n"
          + "journal, the 'reader' can select and combine data in OBIS from a\n"
          + "variety of sources. OBIS and its users give feedback on data quality\n"
          + "and possible errors to data providers. Because data providers are\n"
          + "willing to correct errors, the quality of the data will increase in\n"
          + "time. How OBIS provides quality assurance, who is primarily\n"
          + "responsible for data published in OBIS (its owners), issues to be\n"
          + "considered in using the data, and known gaps in the data, are\n"
          + "described below.\n"
          + "\n"
          + "Quality assurance\n"
          + "\n"
          + "Only data from authoritative scientists and science organizations\n"
          + "approved by OBIS are served. All data are subject to quality control\n"
          + "procedures before publication, and at regular intervals, with data\n"
          + "providers informed of any discrepancies and potential errors (e.g.\n"
          + "species names spelt incorrectly, mapping errors). OBIS also benefits\n"
          + "from user peer-review and feedback to identify technical, geographic,\n"
          + "and taxonomic errors in data served. However, although errors will\n"
          + "exist as they do in any publication, OBIS is confident that the data\n"
          + "are the best available in electronic form. That said, the user needs\n"
          + "sufficient knowledge to judge the appropriate use of the data, i.e.\n"
          + "for what purpose it is fit.\n"
          + "\n"
          + "Many of the data published through OBIS have voucher specimens in\n"
          + "institutional collections and museums, images of observations, and\n"
          + "the original identifier of the specimens is often credited or will\n"
          + "be contactable from the data custodian.\n"
          + "\n"
          + "Data ownership\n"
          + "\n"
          + "Data providers retain ownership of the data provided. OBIS does not\n"
          + "own or control or limit the use of any data or products accessible\n"
          + "through its website. Accordingly, it does not take responsibility\n"
          + "for the quality of such data or products, or the use that people may\n"
          + "make of them.\n"
          + "\n"
          + "Data use\n"
          + "\n"
          + "Appropriate caution is necessary in the interpretation of results\n"
          + "derived from OBIS. Users must recognize that the analysis and\n"
          + "interpretation of data require background knowledge and expertise\n"
          + "about marine biodiversity (including ecosystems and taxonomy).\n"
          + "Users should be aware of possible errors, including in the use of\n"
          + "species names, geo-referencing, data handling, and mapping. They\n"
          + "should cross-check their results for possible errors, and qualify\n"
          + "their interpretation of any results accordingly.\n"
          + "\n"
          + "Users should be aware that OBIS is a gateway to a system of databases\n"
          + "distributed around the world. More information on OBIS data is\n"
          + "available from the data sources websites and contact persons. Users\n"
          + "should email any questions concerning OBIS data or tools (e.g. maps)\n"
          + "to the appropriate contact person and copy this request to\n"
          + "&creator_email; .\n"
          + "\n"
          + "Data gaps\n"
          + "\n"
          + "Major gaps in data and knowledge about the oceans are reflected in\n"
          + "OBIS' data coverage. Note the following:\n"
          + "Most of the planet is more than 1 km under water: this deep sea is\n"
          + "the least surveyed part of our world.\n"
          + "Coastal areas have been adequately sampled only for the distribution\n"
          + "of most vertebrates (birds, mammals, reptiles, larger fish).\n"
          + "The oceans have been better sampled in the northern than the\n"
          + "southern hemisphere, as reflected in the distribution of data in\n"
          + "OBIS.\n"
          + "Most marine species have not yet been recognized or named. A major\n"
          + "effort is required to describe marine species, especially\n"
          + "invertebrates and deep-sea organisms.\n"
          + "Of the marine species that have been described, some have been\n"
          + "discovered to be several species, and others combined into single\n"
          + "species. Thus, there are changes in the application of species names\n"
          + "over time. A checklist of all current marine species names is not\n"
          + "available but it is estimated that 230,000 have been described.\n"
          + "Only about half of these names have been organized into global\n"
          + "species checklists. OBIS includes distribution data on (a) many of\n"
          + "these validated names and (b) additional names that remain to be\n"
          + "organized into global species checklists. Thus, OBIS has some\n"
          + "distribution data for about one third of the known marine species.\n"
          + "Some species distribution data are not available in any form, as\n"
          + "they have not have been published nor made available for databases.\n"
          + "Only some of the recently collected, and less of the older published,\n"
          + "data have been entered into databases. Thus databases are incomplete.\n"
          + "Of existing databases, many are not connected to OBIS.\n"
          + "\n"
          + "You can help address these data gaps by (a) recognizing and\n"
          + "encouraging scientists and organizations to make their data available\n"
          + "online so they are accessible to OBIS, and (b) advocating for and\n"
          + "carrying out field surveys and taxonomic studies designed to fill\n"
          + "geographic and taxonomic gaps in knowledge.\n";

  public static final String OBIS_SUMMARY =
      "DiGIR is an engine which takes XML requests for data and returns a data\n"
          + "subset stored as XML data (as defined in a schema). For more DiGIR\n"
          + "information, see http://digir.sourceforge.net/ ,\n"
          + "http://diveintodigir.ecoforge.net/draft/digirdive.html ,\n"
          + "and http://digir.net/prov/prov_manual.html .\n"
          + "A list of Digir providers is at\n"
          + "http://bigdig.ecoforge.net/wiki/SchemaStatus .\n"
          + "\n"
          + "Darwin is the original schema for use with the DiGIR engine.\n"
          + "\n"
          + "The Ocean Biogeographic Information System (OBIS) schema extends\n"
          + "Darwin. For more OBIS info, see http://www.iobis.org .\n"
          + "See the OBIS schema at http://www.iobis.org/tech/provider/questions .\n"
          + "\n"
          + "Queries: Although OBIS datasets have many variables, most variables\n"
          + "have few values.  The only queries that are likely to succeed MUST\n"
          + "include a constraint for Genus= and MAY include constraints for\n"
          + "Species=, longitude, latitude, and time.\n"
          + "\n"
          + "Most OBIS datasets return a maximum of 1000 rows of data per request.\n"
          + "The limitation is imposed by the OBIS administrators.";

  /**
   * This constructs an EDDTableFromOBIS based on the information in an .xml file.
   *
   * @param erddap if known in this context, else null
   * @param xmlReader with the &lt;erddapDatasets&gt;&lt;dataset type="EDDTableFromOBIS"&gt; having
   *     just been read.
   * @return an EDDTableFromOBIS. When this returns, xmlReader will have just read
   *     &lt;erddapDatasets&gt;&lt;/dataset&gt; .
   * @throws Throwable if trouble
   */
  @EDDFromXmlMethod
  public static EDDTableFromOBIS fromXml(Erddap erddap, SimpleXMLReader xmlReader)
      throws Throwable {

    // data to be obtained (or not)
    if (verbose) String2.log("\n*** constructing EDDTableFromOBIS(xmlReader)...");
    String tDatasetID = xmlReader.attributeValue("datasetID");
    Attributes tGlobalAttributes = null;
    String tLocalSourceUrl = null, tSourceCode = null;
    int tReloadEveryNMinutes = Integer.MAX_VALUE;
    String tAccessibleTo = null;
    String tGraphsAccessibleTo = null;
    StringArray tOnChange = new StringArray();
    String tFgdcFile = null;
    String tIso19115File = null;
    String tSosOfferingPrefix = null;
    double tLongitudeSourceMinimum = Double.NaN;
    double tLongitudeSourceMaximum = Double.NaN;
    double tLatitudeSourceMinimum = Double.NaN;
    double tLatitudeSourceMaximum = Double.NaN;
    double tAltitudeSourceMinimum = Double.NaN;
    double tAltitudeSourceMaximum = Double.NaN;
    String tTimeSourceMinimum = "";
    String tTimeSourceMaximum = "";
    boolean tSourceNeedsExpandedFP_EQ = true;
    String tDefaultDataQuery = null;
    String tDefaultGraphQuery = null;
    String tAddVariablesWhere = null;

    // process the tags
    String startOfTags = xmlReader.allTags();
    int startOfTagsN = xmlReader.stackSize();
    int startOfTagsLength = startOfTags.length();
    while (true) {
      xmlReader.nextTag();
      String tags = xmlReader.allTags();
      String content = xmlReader.content();
      // if (reallyVerbose) String2.log("  tags=" + tags + content);
      if (xmlReader.stackSize() == startOfTagsN) break; // the </dataset> tag
      String localTags = tags.substring(startOfTagsLength);

      // try to make the tag names as consistent, descriptive and readable as possible
      if (localTags.equals("<addAttributes>")) tGlobalAttributes = getAttributesFromXml(xmlReader);
      else if (localTags.equals("<sourceUrl>")) {
      } else if (localTags.equals("</sourceUrl>")) tLocalSourceUrl = content;
      else if (localTags.equals("<sourceCode>")) {
      } else if (localTags.equals("</sourceCode>")) tSourceCode = content;
      else if (localTags.equals("<accessibleTo>")) {
      } else if (localTags.equals("</accessibleTo>")) tAccessibleTo = content;
      else if (localTags.equals("<graphsAccessibleTo>")) {
      } else if (localTags.equals("</graphsAccessibleTo>")) tGraphsAccessibleTo = content;
      else if (localTags.equals("<reloadEveryNMinutes>")) {
      } else if (localTags.equals("</reloadEveryNMinutes>"))
        tReloadEveryNMinutes = String2.parseInt(content);
      else if (localTags.equals("<longitudeSourceMinimum>")) {
      } else if (localTags.equals("</longitudeSourceMinimum>"))
        tLongitudeSourceMinimum = String2.parseDouble(content);
      else if (localTags.equals("<longitudeSourceMaximum>")) {
      } else if (localTags.equals("</longitudeSourceMaximum>"))
        tLongitudeSourceMaximum = String2.parseDouble(content);
      else if (localTags.equals("<latitudeSourceMinimum>")) {
      } else if (localTags.equals("</latitudeSourceMinimum>"))
        tLatitudeSourceMinimum = String2.parseDouble(content);
      else if (localTags.equals("<latitudeSourceMaximum>")) {
      } else if (localTags.equals("</latitudeSourceMaximum>"))
        tLatitudeSourceMaximum = String2.parseDouble(content);
      else if (localTags.equals("<altitudeSourceMinimum>")) {
      } else if (localTags.equals("</altitudeSourceMinimum>"))
        tAltitudeSourceMinimum = String2.parseDouble(content);
      else if (localTags.equals("<altitudeSourceMaximum>")) {
      } else if (localTags.equals("</altitudeSourceMaximum>"))
        tAltitudeSourceMaximum = String2.parseDouble(content);
      else if (localTags.equals("<timeSourceMinimum>")) {
      } else if (localTags.equals("</timeSourceMinimum>")) tTimeSourceMinimum = content;
      else if (localTags.equals("<timeSourceMaximum>")) {
      } else if (localTags.equals("</timeSourceMaximum>")) tTimeSourceMaximum = content;
      else if (localTags.equals("<sourceNeedsExpandedFP_EQ>")) {
      } else if (localTags.equals("</sourceNeedsExpandedFP_EQ>"))
        tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
      else if (localTags.equals("<onChange>")) {
      } else if (localTags.equals("</onChange>")) tOnChange.add(content);
      else if (localTags.equals("<fgdcFile>")) {
      } else if (localTags.equals("</fgdcFile>")) tFgdcFile = content;
      else if (localTags.equals("<iso19115File>")) {
      } else if (localTags.equals("</iso19115File>")) tIso19115File = content;
      else if (localTags.equals("<sosOfferingPrefix>")) {
      } else if (localTags.equals("</sosOfferingPrefix>")) tSosOfferingPrefix = content;
      else if (localTags.equals("<defaultDataQuery>")) {
      } else if (localTags.equals("</defaultDataQuery>")) tDefaultDataQuery = content;
      else if (localTags.equals("<defaultGraphQuery>")) {
      } else if (localTags.equals("</defaultGraphQuery>")) tDefaultGraphQuery = content;
      else if (localTags.equals("<addVariablesWhere>")) {
      } else if (localTags.equals("</addVariablesWhere>")) tAddVariablesWhere = content;
      else xmlReader.unexpectedTagException();
    }

    return new EDDTableFromOBIS(
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        tLocalSourceUrl,
        tSourceCode,
        tReloadEveryNMinutes,
        tLongitudeSourceMinimum,
        tLongitudeSourceMaximum,
        tLatitudeSourceMinimum,
        tLatitudeSourceMaximum,
        tAltitudeSourceMinimum,
        tAltitudeSourceMaximum,
        tTimeSourceMinimum,
        tTimeSourceMaximum,
        tSourceNeedsExpandedFP_EQ);
  }

  /**
   * The constructor. This is simpler than other EDDTable subclasses because the dataVariables and
   * other attributes are the same for all OBIS servers.
   *
   * @param tDatasetID is a very short string identifier (recommended: [A-Za-z][A-Za-z0-9_]* ) for
   *     this dataset. See EDD.datasetID().
   * @param tAccessibleTo is a comma separated list of 0 or more roles which will have access to
   *     this dataset. <br>
   *     If null, everyone will have access to this dataset (even if not logged in). <br>
   *     If "", no one will have access to this dataset.
   * @param tOnChange 0 or more actions (starting with http://, https://, or mailto: ) to be done
   *     whenever the dataset changes significantly
   * @param tFgdcFile This should be the fullname of a file with the FGDC that should be used for
   *     this dataset, or "" (to cause ERDDAP not to try to generate FGDC metadata for this
   *     dataset), or null (to allow ERDDAP to try to generate FGDC metadata for this dataset).
   * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
   * @param tAddGlobalAttributes are global attributes which will be added to (and take precedence
   *     over) the data source's global attributes. This may be null if you have nothing to add. The
   *     combined global attributes must include:
   *     <ul>
   *       <li>"title" - the short (&lt; 80 characters) description of the dataset
   *       <li>"institution" - the source of the data (best if &lt; 50 characters so it fits in a
   *           graph's legend).
   *     </ul>
   *     <br>
   *     The constructor sets "cdm_data_type" to CDM_POINT. <br>
   *     If not present, the standard "infoUrl" will be added (a url with information about this
   *     data set). <br>
   *     Special case: If not present, the standard "OBIS_SUMMARY" will be added (a longer
   *     description of the dataset; it may have newline characters (usually at &lt;= 72 chars per
   *     line)). If summary is present, the standard OBIS_SUMMARY will be substituted for
   *     "[OBIS_SUMMARY]". <br>
   *     Special case: value="null" causes that item to be removed from combinedGlobalAttributes.
   *     <br>
   *     Special case: if combinedGlobalAttributes name="license", any instance of "[standard]" will
   *     be converted to the EDStatic.standardLicense plus EDDTableFromOBIS OBIS_LICENSE. <br>
   *     Special case: addGlobalAttributes must have a name="creator_email" value=AnEmailAddress for
   *     users to contact regarding publications that use the data in order to comply with license.
   *     A suitable email address can be found by reading the XML response from the sourceURL. <br>
   *     Special case: I manually add the list of available "Genus" values to the "summary"
   *     metadata. I get the list from DigirHelper.getObisInventoryString(), specifically some
   *     one-time code in DigirHelper.test().
   * @param tLocalSourceUrl the url to which requests are sent e.g.,
   *     http://iobis.marine.rutgers.edu/digir2/DiGIR.php
   * @param tSourceCode the obis name for the source, e.g., GHMP. If you read the xml response from
   *     the sourceUrl, this is the name from the &lt;resource&gt;&lt;code&gt; tag.
   * @param tReloadEveryNMinutes indicates how often the source should be checked for new data.
   * @param tLonMin in source units (use Double.NaN if not known). [I use
   *     eddTable.getEmpiricalMinMax(language, "2007-02-01", "2007-02-01", false, true); below to
   *     get it.]
   * @param tLonMax see tLonMin description.
   * @param tLatMin see tLonMin description.
   * @param tLatMax see tLonMin description.
   * @param tAltMin see tLonMin description.
   * @param tAltMax see tLonMin description.
   * @param tTimeMin in EDVTimeStamp.ISO8601TZ_FORMAT, or "" if not known.
   * @param tTimeMax in EDVTimeStamp.ISO8601TZ_FORMAT, or "" if not known
   * @param tSourceNeedsExpandedFP_EQ
   * @throws Throwable if trouble
   */
  public EDDTableFromOBIS(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      String tAddVariablesWhere,
      Attributes tAddGlobalAttributes,
      String tLocalSourceUrl,
      String tSourceCode,
      int tReloadEveryNMinutes,
      double tLonMin,
      double tLonMax,
      double tLatMin,
      double tLatMax,
      double tAltMin,
      double tAltMax,
      String tTimeMin,
      String tTimeMax,
      boolean tSourceNeedsExpandedFP_EQ)
      throws Throwable {

    if (verbose) String2.log("\n*** constructing EDDTableFromOBIS " + tDatasetID);
    long constructionStartMillis = System.currentTimeMillis();
    String errorInMethod = "Error in EDDTableFromOBIS(" + tDatasetID + ") constructor:\n";

    // save some of the parameters
    className = "EDDTableFromOBIS";
    datasetID = tDatasetID;
    setAccessibleTo(tAccessibleTo);
    setGraphsAccessibleTo(tGraphsAccessibleTo);
    onChange = tOnChange;
    fgdcFile = tFgdcFile;
    iso19115File = tIso19115File;
    sosOfferingPrefix = tSosOfferingPrefix;
    defaultDataQuery = tDefaultDataQuery;
    defaultGraphQuery = tDefaultGraphQuery;
    setReloadEveryNMinutes(tReloadEveryNMinutes);
    if (tAddGlobalAttributes == null) tAddGlobalAttributes = new Attributes();
    if (tAddGlobalAttributes.getString("Conventions") == null)
      tAddGlobalAttributes.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
    if (tAddGlobalAttributes.getString("infoUrl") == null)
      tAddGlobalAttributes.add("infoUrl", STANDARD_INFO_URL);
    String tSummary = tAddGlobalAttributes.getString("summary");
    tAddGlobalAttributes.add(
        "summary",
        tSummary == null
            ? OBIS_SUMMARY
            : String2.replaceAll(tSummary, "[OBIS_SUMMARY]", OBIS_SUMMARY));
    String tCreator_email = tAddGlobalAttributes.getString("creator_email");
    Test.ensureNotNothing(
        tCreator_email,
        "The global addAttributes must include 'creator_email' for users to contact regarding "
            + "publications that use the data in order to comply with license. "
            + "A suitable email address can be found by reading the XML response from the sourceURL.");
    tAddGlobalAttributes.add("cdm_data_type", CDM_POINT);
    tAddGlobalAttributes.add("standard_name_vocabulary", "CF Standard Name Table v55");
    addGlobalAttributes = tAddGlobalAttributes;
    addGlobalAttributes.set("sourceUrl", convertToPublicSourceUrl(tLocalSourceUrl));
    localSourceUrl = tLocalSourceUrl;
    sourceCode = tSourceCode;

    sourceGlobalAttributes = new Attributes();
    combinedGlobalAttributes =
        new Attributes(addGlobalAttributes, sourceGlobalAttributes); // order is important
    String tLicense = combinedGlobalAttributes.getString("license");
    if (tLicense != null) {
      tLicense =
          String2.replaceAll(
              tLicense,
              "[standard]",
              EDStatic.standardLicense
                  + "\n\n"
                  + String2.replaceAll(OBIS_LICENSE, "&sourceUrl;", tLocalSourceUrl));
      tLicense = String2.replaceAll(tLicense, "&creator_email;", tCreator_email);
      combinedGlobalAttributes.set("license", tLicense);
    }
    combinedGlobalAttributes.removeValue("\"null\"");

    // souceCanConstrain:
    sourceNeedsExpandedFP_EQ = tSourceNeedsExpandedFP_EQ;
    sourceCanConstrainNumericData =
        CONSTRAIN_PARTIAL; // everything but time since from several source vars
    sourceCanConstrainStringData = CONSTRAIN_PARTIAL; // only = !=
    sourceCanConstrainStringRegex = ""; // Digir has simplistic regex support

    // get the darwin and obis variables    (remove prefix so sortable)
    String origNames[] = DigirHelper.getDarwin2ObisVariables(); // don't modify these
    String tVarNames[] = new String[origNames.length];
    String darwinPre = DigirHelper.DARWIN_PREFIX + ":";
    String obisPre = DigirHelper.OBIS_PREFIX + ":";
    for (int v = 0; v < tVarNames.length; v++) {
      if (origNames[v].startsWith(darwinPre))
        tVarNames[v] = origNames[v].substring(darwinPre.length());
      else if (origNames[v].startsWith(obisPre))
        tVarNames[v] = origNames[v].substring(obisPre.length());
      else throw new IllegalArgumentException("Unexpected prefix for tVarName=" + origNames[v]);
    }
    Arrays.sort(tVarNames);

    // make the standard variables
    nFixedVariables = 5;
    dataVariables =
        new EDV[tVarNames.length + nFixedVariables - 3]; // -3(Longitude, Latitude, MinimumDepth)
    lonIndex = 0;
    dataVariables[lonIndex] =
        new EDVLon(
            datasetID,
            "darwin:Longitude",
            null,
            null,
            "double",
            PAOne.fromDouble(Double.isNaN(tLonMin) ? -180 : tLonMin),
            PAOne.fromDouble(Double.isNaN(tLonMax) ? 180 : tLonMax));
    latIndex = 1;
    dataVariables[latIndex] =
        new EDVLat(
            datasetID,
            "darwin:Latitude",
            null,
            null,
            "double",
            PAOne.fromDouble(Double.isNaN(tLatMin) ? -90 : tLatMin),
            PAOne.fromDouble(Double.isNaN(tLatMax) ? 90 : tLatMax));
    altIndex = 2;
    depthIndex = -1; // 2012-12-20 consider using depth, not altitude!!!
    Attributes altAtts = new Attributes();
    altAtts.add("comment", "Created from the darwin:MinimumDepth variable.");
    altAtts.add("scale_factor", -1.0);
    altAtts.add("units", "m");
    dataVariables[altIndex] =
        new EDVAlt(
            datasetID,
            "darwin:MinimumDepth",
            altAtts,
            null,
            "double",
            PAOne.fromDouble(-tAltMin),
            PAOne.fromDouble(-tAltMax));
    timeIndex = 3;
    dataVariables[timeIndex] =
        new EDVTime(
            datasetID,
            "TIME",
            new Attributes()
                .add("actual_range", new StringArray(new String[] {tTimeMin, tTimeMax}))
                .add(
                    "comment",
                    "Created from the darwin:YearCollected-darwin:MonthCollected-darwin:DayCollected and darwin:TimeOfDay variables.")
                .add("units", EDV.TIME_UNITS),
            // estimate actual_range?
            null,
            "double"); // this constructor gets source / sets destination actual_range
    dataVariables[4] =
        new EDV(
            datasetID,
            "ID",
            null,
            new Attributes()
                .add(
                    "comment",
                    "Created from the [darwin:InstitutionCode]:[darwin:CollectionCode]:[darwin:CatalogNumber] variables.")
                .add("ioos_category", "Identifier"),
            null,
            "String");
    // no need to call setActualRangeFromDestinationMinMax() since they are NaNs

    // make the other variables
    int tv = nFixedVariables;
    for (int v = 0; v < tVarNames.length; v++) {
      String tSourceName = tVarNames[v];
      String tDestName = tSourceName;
      if (tDestName == null || tDestName.trim().length() == 0) tDestName = tSourceName;
      Attributes tAddAtt = new Attributes();

      // skip Lon and Lat since handled above
      if (tSourceName.equals("Longitude")
          || tSourceName.equals("Latitude")
          || tSourceName.equals("MinimumDepth")) continue;

      // get info
      String info, prefix;
      info = DigirHelper.digirDarwin2Properties.getString(tSourceName, null);
      if (info == null) {
        info = DigirHelper.digirObisProperties.getString(tSourceName, null);
        prefix = obisPre;
      } else {
        prefix = darwinPre;
      }
      Test.ensureNotNull(info, errorInMethod + "No info found for variable=" + tSourceName);
      String infoArray[] = String2.split(info, '\f');

      // get tSourceType
      String tSourceType = infoArray[0];
      boolean isTimeStamp = tSourceType.equals("dateTime");
      if (isTimeStamp) {
        tSourceType = "String";
        tAddAtt.add("units", Calendar2.ISO8601TZ_FORMAT);
      }

      // get sourceAtt
      Attributes tSourceAtt = new Attributes();
      String metadata[] = String2.split(infoArray[1], '`');
      for (int i = 0; i < metadata.length; i++) {
        int eqPo = metadata[i].indexOf('='); // first instance of '='
        Test.ensureTrue(
            eqPo > 0,
            errorInMethod + "Invalid metadata for " + prefix + tSourceName + " : " + metadata[i]);
        tSourceAtt.set(metadata[i].substring(0, eqPo), metadata[i].substring(eqPo + 1));
      }
      // if (reallyVerbose) String2.log("v=" + v + " source=" + prefix+tSourceName +
      //    " destName=" + tDestName + " type=" + tSourceType + " sourceAtt=\n" + tSourceAtt);

      // make the variable
      if (isTimeStamp) {
        dataVariables[tv] =
            new EDVTimeStamp(
                datasetID,
                prefix + tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // this constructor gets source / sets destination actual_range
        tv++;
      } else {
        dataVariables[tv] =
            new EDV(
                datasetID,
                prefix + tSourceName,
                tDestName,
                tSourceAtt,
                tAddAtt,
                tSourceType); // the constructor that reads source actual_range
        dataVariables[tv].setActualRangeFromDestinationMinMax();
        tv++;
      }
    }

    // make addVariablesWhereAttNames and addVariablesWhereAttValues
    makeAddVariablesWhereAttNamesAndValues(tAddVariablesWhere);

    // ensure the setup is valid
    ensureValid();

    // finally
    long cTime = System.currentTimeMillis() - constructionStartMillis;
    if (verbose)
      String2.log(
          (debugMode ? "\n" + toString() : "")
              + "\n*** EDDTableFromOBIS "
              + datasetID
              + " constructor finished. TIME="
              + cTime
              + "ms"
              + (cTime >= 600000 ? "  (>10m!)" : cTime >= 10000 ? "  (>10s!)" : "")
              + "\n");
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  @Override
  public boolean knowsActualRange() {
    return false;
  } // because data is from a remote service

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter. See the EDDTable method documentation.
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  @Override
  public void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    // get the sourceDapQuery (a query that the source can handle)
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    getSourceQueryFromDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues); // timeStamp constraints other than regex are epochSeconds

    // special case: 1 variable, no constraints, &distinct()
    if (resultsVariables.size() == 1
        && String2.indexOf(
                new String[] {
                  "darwin:Longitude", "darwin:Latitude", "darwin:MinimumDepth", "TIME", "ID"
                },
                resultsVariables.get(0))
            < 0
        && constraintVariables.size() == 0
        && userDapQuery.indexOf("&distinct()") >= 0) {

      // get data via getInventoryTable
      Table table =
          DigirHelper.getInventoryTable(
              DigirHelper.OBIS_VERSION,
              DigirHelper.OBIS_PREFIXES,
              DigirHelper.OBIS_XMLNSES,
              DigirHelper.OBIS_XSDES,
              new String[] {sourceCode},
              localSourceUrl,
              new String[0],
              new String[0],
              new String[0], // filter
              resultsVariables.get(0));
      table.removeColumn(2);
      table.removeColumn(0);
      standardizeResultsTable(language, requestUrl, userDapQuery, table);
      tableWriter.writeAllAndFinish(table);
      return;
    }

    // further prune constraints
    // sourceCanConstrainNumericData = CONSTRAIN_PARTIAL; //everything but time since from several
    // source vars
    // sourceCanConstrainStringData  = CONSTRAIN_PARTIAL; //only = !=,  nothing for stationID
    // sourceCanConstrainStringRegex = ""; //Digir has simplistic regex support
    // work backwards since deleting some
    for (int c = constraintVariables.size() - 1; c >= 0; c--) {
      String constraintVariable = constraintVariables.get(c);
      int dv = String2.indexOf(dataVariableSourceNames(), constraintVariable);
      EDV edv = dataVariables[dv];
      if (edv instanceof EDVTimeStamp || dv == 4) {
        // remove time and ID constraints
        constraintVariables.remove(c);
        constraintOps.remove(c);
        constraintValues.remove(c);

      } else if (edv.sourceDataPAType() == PAType.STRING
          && String2.indexOf(GTLT_OPERATORS, constraintOps.get(c)) >= 0) {
        // remove >, >=, <, <= ops for String variables
        constraintVariables.remove(c);
        constraintOps.remove(c);
        constraintValues.remove(c);
      }
    }

    // String sourceDapQuery = formatAsDapQuery(resultsVariables.toArray(),
    //    constraintVariables.toArray(), constraintOps.toArray(),
    //    constraintValues.toArray());

    // remove xyztID from resultsVariables  (see includeXYZT below)
    for (int dv = 0; dv < nFixedVariables; dv++) {
      int po = resultsVariables.indexOf(dataVariables[dv].sourceName());
      if (po >= 0) resultsVariables.remove(po);
    }

    // convert constraintOps to words
    for (int i = 0; i < constraintOps.size(); i++) {
      int po = String2.indexOf(DigirHelper.COP_SYMBOLS, constraintOps.get(i));
      if (po >= 0) constraintOps.set(i, DigirHelper.COP_NAMES[po]);
      else throw new IllegalArgumentException("Unexpected constraintOp=" + constraintOps.get(i));
    }

    // Read all data, then write to tableWriter.
    // I can't split into subsets because I don't know which variable
    //  to constrain or how to constrain it (it would change with different
    //  userDapQuery's).
    Table table = new Table();
    try {
      DigirHelper.searchObis(
          new String[] {sourceCode},
          localSourceUrl,
          constraintVariables.toArray(),
          constraintOps.toArray(),
          constraintValues.toArray(),
          table,
          true, // true=includeXYZTID because I often want T and ID (otherwise hard to get)
          resultsVariables.toArray());
    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}

      // if OutOfMemoryError or too much data, rethrow t
      String tToString = t.toString();
      if (Thread.currentThread().isInterrupted()
          || t instanceof InterruptedException
          || t instanceof OutOfMemoryError
          || tToString.indexOf(Math2.memoryTooMuchData) >= 0
          || tToString.indexOf(Math2.TooManyOpenFiles) >= 0) throw t;

      throw new Throwable(EDStatic.errorFromDataSource + tToString, t);
    }
    // if (reallyVerbose) String2.log(table.toString());
    table.setColumnName(table.findColumnNumber("LON"), "darwin:Longitude");
    table.setColumnName(table.findColumnNumber("LAT"), "darwin:Latitude");
    table.setColumnName(table.findColumnNumber("DEPTH"), "darwin:MinimumDepth");
    standardizeResultsTable(language, requestUrl, userDapQuery, table);
    tableWriter.writeAllAndFinish(table);
  }

  /**
   * This generates a datasets.xml entry for an EDDTableFromOBIS.
   * The XML can then be edited by hand and added to the datasets.xml file.
   *
   * @param tLocalSourceUrl
   * @param tSourceCode  If you read the XML response from the sourceUrl, the source code (e.g., GHMP)
   *     is the value from one of the <resource><code> tags.
   * @param tReloadEveryNMinutes
   * @param tCreatorEmail  A suitable email address can be found by reading the XML response from the sourceURL.
   * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found.
   *    If no trouble, then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tLocalSourceUrl,
      String tSourceCode,
      int tReloadEveryNMinutes,
      String tCreatorEmail,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    tLocalSourceUrl = EDStatic.updateUrls(tLocalSourceUrl); // http: to https:
    String2.log(
        "\n*** EDDTableFromOBIS.generateDatasetsXml"
            + "\nlocalSourceUrl="
            + tLocalSourceUrl
            + " tSourceCode="
            + tSourceCode
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\ncreatorEmail="
            + tCreatorEmail
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    // !!! this could try to read sourceAttributes from the source, but it currently doesn't
    //  (partly because it would be slow)
    String tPublicSourceUrl = convertToPublicSourceUrl(tLocalSourceUrl);

    // globalAttributes
    if (externalAddGlobalAttributes == null) externalAddGlobalAttributes = new Attributes();
    String tTitle =
        tSourceCode + " Data from the OBIS Server at " + suggestInstitution(tPublicSourceUrl);
    if (tCreatorEmail != null && tCreatorEmail.length() > 0)
      externalAddGlobalAttributes.add("creator_email", tCreatorEmail);
    if (externalAddGlobalAttributes.getString("infoUrl") == null)
      externalAddGlobalAttributes.add("infoUrl", tPublicSourceUrl);
    if (externalAddGlobalAttributes.getString("summary") == null)
      externalAddGlobalAttributes.add("summary", tTitle + ".\n\n[OBIS_SUMMARY]");
    if (externalAddGlobalAttributes.getString("title") == null)
      externalAddGlobalAttributes.add("title", tTitle);

    // add global attributes in the dataAddTable
    Attributes addAtts =
        makeReadyToUseAddGlobalAttributesForDatasetsXml(
            new Attributes(),
            // another cdm_data_type could be better; this is ok
            "Point",
            tLocalSourceUrl,
            externalAddGlobalAttributes,
            new HashSet());

    // don't use suggestSubsetVariables since sourceTable not really available

    // generate the datasets.xml
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<dataset type=\"EDDTableFromOBIS\" datasetID=\""
            + suggestDatasetID(tPublicSourceUrl)
            + "\" active=\"true\">\n"
            + "    <sourceUrl>"
            + XML.encodeAsXML(tLocalSourceUrl)
            + "</sourceUrl>\n"
            + "    <sourceCode>"
            + XML.encodeAsXML(tSourceCode)
            + "</sourceCode>\n"
            + "    <sourceNeedsExpandedFP_EQ>true</sourceNeedsExpandedFP_EQ>\n"
            + // always safe to use true
            "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            +
            // "    <longitudeSourceMinimum>...</longitudeSourceMinimum>     //all of the Min and
            // Max are optional
            // "    <longitudeSourceMaximum>...</longitudeSourceMaximum>
            // "    <latitudeSourceMinimum>...</latitudeSourceMinimum>
            // "    <latitudeSourceMaximum>...</latitudeSourceMaximum>
            // "    <altitudeSourceMinimum>...</altitudeSourceMinimum>
            // "    <altitudeSourceMaximum>...</altitudeSourceMaximum>
            // "    <timeSourceMinimum>...</timeSourceMinimum>    //YYYY-MM-DDThh:mm:ssZ
            // "    <timeSourceMaximum>...</timeSourceMaximum>
            cdmSuggestion()
            + writeAttsForDatasetsXml(true, addAtts, "    ")
            + "</dataset>\n"
            + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    return sb.toString();
  }
}
