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
      """
                  By using OBIS data, I agree that, in any publication or presentation
                  of any sort based wholly or in part on such data, I will:

                  1. Acknowledge the use of specific records from contributing databases
                  in the form appearing in the GLOBAL 'citation' attribute thereof (if any);
                  and acknowledge the use of the OBIS facility in one of the following
                  prescribed forms:

                  For OBIS website:

                  Ocean Biogeographic Information System. [date accessed]
                  &sourceUrl;

                  For data used:

                  Author, initials. Database title. Retrieved [date accessed] from
                  &sourceUrl;

                  Example:

                  Stocks, K. SeamountsOnline: an online information system for seamount
                  biology. Version 3.1. Retrieved [date accessed] from
                  &sourceUrl;.

                  2. For information purposes, provide to &creator_email;
                  the full citation of any publication I make (printed or electronic)
                  that cites OBIS or any constituent part.

                  3. Recognize the limitations of data in OBIS:
                  OBIS is comparable to a scientific journal that makes data freely
                  available on the internet. Thus the geographic and taxonomic scope,
                  and quantity of data provided, depend on the scientists and
                  organizations that provide data. However, in contrast to data in a
                  journal, the 'reader' can select and combine data in OBIS from a
                  variety of sources. OBIS and its users give feedback on data quality
                  and possible errors to data providers. Because data providers are
                  willing to correct errors, the quality of the data will increase in
                  time. How OBIS provides quality assurance, who is primarily
                  responsible for data published in OBIS (its owners), issues to be
                  considered in using the data, and known gaps in the data, are
                  described below.

                  Quality assurance

                  Only data from authoritative scientists and science organizations
                  approved by OBIS are served. All data are subject to quality control
                  procedures before publication, and at regular intervals, with data
                  providers informed of any discrepancies and potential errors (e.g.
                  species names spelt incorrectly, mapping errors). OBIS also benefits
                  from user peer-review and feedback to identify technical, geographic,
                  and taxonomic errors in data served. However, although errors will
                  exist as they do in any publication, OBIS is confident that the data
                  are the best available in electronic form. That said, the user needs
                  sufficient knowledge to judge the appropriate use of the data, i.e.
                  for what purpose it is fit.

                  Many of the data published through OBIS have voucher specimens in
                  institutional collections and museums, images of observations, and
                  the original identifier of the specimens is often credited or will
                  be contactable from the data custodian.

                  Data ownership

                  Data providers retain ownership of the data provided. OBIS does not
                  own or control or limit the use of any data or products accessible
                  through its website. Accordingly, it does not take responsibility
                  for the quality of such data or products, or the use that people may
                  make of them.

                  Data use

                  Appropriate caution is necessary in the interpretation of results
                  derived from OBIS. Users must recognize that the analysis and
                  interpretation of data require background knowledge and expertise
                  about marine biodiversity (including ecosystems and taxonomy).
                  Users should be aware of possible errors, including in the use of
                  species names, geo-referencing, data handling, and mapping. They
                  should cross-check their results for possible errors, and qualify
                  their interpretation of any results accordingly.

                  Users should be aware that OBIS is a gateway to a system of databases
                  distributed around the world. More information on OBIS data is
                  available from the data sources websites and contact persons. Users
                  should email any questions concerning OBIS data or tools (e.g. maps)
                  to the appropriate contact person and copy this request to
                  &creator_email; .

                  Data gaps

                  Major gaps in data and knowledge about the oceans are reflected in
                  OBIS' data coverage. Note the following:
                  Most of the planet is more than 1 km under water: this deep sea is
                  the least surveyed part of our world.
                  Coastal areas have been adequately sampled only for the distribution
                  of most vertebrates (birds, mammals, reptiles, larger fish).
                  The oceans have been better sampled in the northern than the
                  southern hemisphere, as reflected in the distribution of data in
                  OBIS.
                  Most marine species have not yet been recognized or named. A major
                  effort is required to describe marine species, especially
                  invertebrates and deep-sea organisms.
                  Of the marine species that have been described, some have been
                  discovered to be several species, and others combined into single
                  species. Thus, there are changes in the application of species names
                  over time. A checklist of all current marine species names is not
                  available but it is estimated that 230,000 have been described.
                  Only about half of these names have been organized into global
                  species checklists. OBIS includes distribution data on (a) many of
                  these validated names and (b) additional names that remain to be
                  organized into global species checklists. Thus, OBIS has some
                  distribution data for about one third of the known marine species.
                  Some species distribution data are not available in any form, as
                  they have not have been published nor made available for databases.
                  Only some of the recently collected, and less of the older published,
                  data have been entered into databases. Thus databases are incomplete.
                  Of existing databases, many are not connected to OBIS.

                  You can help address these data gaps by (a) recognizing and
                  encouraging scientists and organizations to make their data available
                  online so they are accessible to OBIS, and (b) advocating for and
                  carrying out field surveys and taxonomic studies designed to fill
                  geographic and taxonomic gaps in knowledge.
                  """;

  public static final String OBIS_SUMMARY =
      """
                  DiGIR is an engine which takes XML requests for data and returns a data
                  subset stored as XML data (as defined in a schema). For more DiGIR
                  information, see http://digir.sourceforge.net/ ,
                  http://diveintodigir.ecoforge.net/draft/digirdive.html ,
                  and http://digir.net/prov/prov_manual.html .
                  A list of Digir providers is at
                  http://bigdig.ecoforge.net/wiki/SchemaStatus .

                  Darwin is the original schema for use with the DiGIR engine.

                  The Ocean Biogeographic Information System (OBIS) schema extends
                  Darwin. For more OBIS info, see http://www.iobis.org .
                  See the OBIS schema at http://www.iobis.org/tech/provider/questions .

                  Queries: Although OBIS datasets have many variables, most variables
                  have few values.  The only queries that are likely to succeed MUST
                  include a constraint for Genus= and MAY include constraints for
                  Species=, longitude, latitude, and time.

                  Most OBIS datasets return a maximum of 1000 rows of data per request.
                  The limitation is imposed by the OBIS administrators.""";

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
      switch (localTags) {
        case "<addAttributes>" -> tGlobalAttributes = getAttributesFromXml(xmlReader);
        case "<sourceUrl>" -> {}
        case "</sourceUrl>" -> tLocalSourceUrl = content;
        case "<sourceCode>" -> {}
        case "</sourceCode>" -> tSourceCode = content;
        case "<accessibleTo>" -> {}
        case "</accessibleTo>" -> tAccessibleTo = content;
        case "<graphsAccessibleTo>" -> {}
        case "</graphsAccessibleTo>" -> tGraphsAccessibleTo = content;
        case "<reloadEveryNMinutes>" -> {}
        case "</reloadEveryNMinutes>" -> tReloadEveryNMinutes = String2.parseInt(content);
        case "<longitudeSourceMinimum>" -> {}
        case "</longitudeSourceMinimum>" -> tLongitudeSourceMinimum = String2.parseDouble(content);
        case "<longitudeSourceMaximum>" -> {}
        case "</longitudeSourceMaximum>" -> tLongitudeSourceMaximum = String2.parseDouble(content);
        case "<latitudeSourceMinimum>" -> {}
        case "</latitudeSourceMinimum>" -> tLatitudeSourceMinimum = String2.parseDouble(content);
        case "<latitudeSourceMaximum>" -> {}
        case "</latitudeSourceMaximum>" -> tLatitudeSourceMaximum = String2.parseDouble(content);
        case "<altitudeSourceMinimum>" -> {}
        case "</altitudeSourceMinimum>" -> tAltitudeSourceMinimum = String2.parseDouble(content);
        case "<altitudeSourceMaximum>" -> {}
        case "</altitudeSourceMaximum>" -> tAltitudeSourceMaximum = String2.parseDouble(content);
        case "<timeSourceMinimum>" -> {}
        case "</timeSourceMinimum>" -> tTimeSourceMinimum = content;
        case "<timeSourceMaximum>" -> {}
        case "</timeSourceMaximum>" -> tTimeSourceMaximum = content;
        case "<sourceNeedsExpandedFP_EQ>" -> {}
        case "</sourceNeedsExpandedFP_EQ>" ->
            tSourceNeedsExpandedFP_EQ = String2.parseBoolean(content);
        case "<onChange>" -> {}
        case "</onChange>" -> tOnChange.add(content);
        case "<fgdcFile>" -> {}
        case "</fgdcFile>" -> tFgdcFile = content;
        case "<iso19115File>" -> {}
        case "</iso19115File>" -> tIso19115File = content;
        case "<sosOfferingPrefix>" -> {}
        case "</sosOfferingPrefix>" -> tSosOfferingPrefix = content;
        case "<defaultDataQuery>" -> {}
        case "</defaultDataQuery>" -> tDefaultDataQuery = content;
        case "<defaultGraphQuery>" -> {}
        case "</defaultGraphQuery>" -> tDefaultGraphQuery = content;
        case "<addVariablesWhere>" -> {}
        case "</addVariablesWhere>" -> tAddVariablesWhere = content;
        default -> xmlReader.unexpectedTagException();
      }
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
    for (String tSourceName : tVarNames) {
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
      for (String metadatum : metadata) {
        int eqPo = metadatum.indexOf('='); // first instance of '='
        Test.ensureTrue(
            eqPo > 0,
            errorInMethod + "Invalid metadata for " + prefix + tSourceName + " : " + metadatum);
        tSourceAtt.set(metadatum.substring(0, eqPo), metadatum.substring(eqPo + 1));
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
          && GTLT_OPERATORS.indexOf(constraintOps.get(c)) >= 0) {
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
      int po = DigirHelper.COP_SYMBOLS.indexOf(constraintOps.get(i));
      if (po >= 0) constraintOps.set(i, DigirHelper.COP_NAMES.get(po));
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
