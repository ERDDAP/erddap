package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.junit.jupiter.api.BeforeAll;
import tags.TagIncompleteTest;
import testDataset.Initialization;

class EDDTableFromOBISTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * This tests generateDatasetsXml.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    String2.log("\n*** EDDTableFromOBIS.testGenerateDatasetsXml");
    // testVerboseOn();
    String results, expected;

    try {
      results =
          EDDTableFromOBIS.generateDatasetsXml(
                  "http://iobis.marine.rutgers.edu/digir2/DiGIR.php",
                  "OBIS-SEAMAP",
                  EDDTableFromOBIS.DEFAULT_RELOAD_EVERY_N_MINUTES,
                  "dhyrenbach@duke.edu",
                  null)
              + "\n";

      // GenerateDatasetsXml
      String gdxResults =
          new GenerateDatasetsXml()
              .doIt(
                  new String[] {
                    "-verbose",
                    "EDDTableFromOBIS",
                    "http://iobis.marine.rutgers.edu/digir2/DiGIR.php",
                    "OBIS-SEAMAP",
                    "" + EDDTableFromOBIS.DEFAULT_RELOAD_EVERY_N_MINUTES,
                    "dhyrenbach@duke.edu",
                    "-1"
                  }, // defaultStandardizeWhat
                  false); // doIt loop?
      Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

      String tDatasetID =
          EDDTableFromNcFiles.suggestDatasetID("http://iobis.marine.rutgers.edu/digir2/DiGIR.php");
      expected =
          "<dataset type=\"EDDTableFromOBIS\" datasetID=\""
              + tDatasetID
              + "\" active=\"true\">\n"
              + "    <sourceUrl>http://iobis.marine.rutgers.edu/digir2/DiGIR.php</sourceUrl>\n"
              + "    <sourceCode>OBIS-SEAMAP</sourceCode>\n"
              + "    <sourceNeedsExpandedFP_EQ>true</sourceNeedsExpandedFP_EQ>\n"
              + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
              + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
              + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
              + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
              + "    -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"cdm_data_type\">Point</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
              + "        <att name=\"creator_email\">dhyrenbach@duke.edu</att>\n"
              + "        <att name=\"creator_name\">DHYRENBACH</att>\n"
              + "        <att name=\"creator_type\">person</att>\n"
              + "        <att name=\"creator_url\">https://marine.rutgers.edu/main/</att>\n"
              + "        <att name=\"infoUrl\">http://iobis.marine.rutgers.edu/digir2/DiGIR.php</att>\n"
              + "        <att name=\"institution\">DUKE</att>\n"
              + "        <att name=\"keywords\">area, assessment, biogeographic, data, digir.php, duke, information, marine, monitoring, obis, obis-seamap, ocean, program, rutgers, seamap, server, southeast, system</att>\n"
              + "        <att name=\"license\">[standard]</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
              + "        <att name=\"summary\">Ocean Biogeographic Information System (OBIS)-Southeast Area Monitoring &amp; Assessment Program (SEAMAP) Data from the OBIS Server at RUTGERS MARINE.\n"
              + "\n"
              + "[OBIS_SUMMARY]</att>\n"
              + "        <att name=\"title\">OBIS-SEAMAP Data from the OBIS Server at RUTGERS MARINE (DiGIR.php)</att>\n"
              + "    </addAttributes>\n"
              + "</dataset>\n"
              + "\n\n";
      Test.ensureEqual(results, expected, "results=\n" + results);

      // ensure it is ready-to-use by making a dataset from it
      //   String tDatasetID = "rutgers_marine_6cb4_a970_1d67";
      EDD.deleteCachedDatasetInfo(tDatasetID);
      EDD edd = EDDTableFromOBIS.oneFromXmlFragment(null, results);
      Test.ensureEqual(edd.datasetID(), tDatasetID, "");
      Test.ensureEqual(
          edd.title(), "OBIS-SEAMAP Data from the OBIS Server at RUTGERS MARINE (DiGIR.php)", "");
      Test.ensureEqual(
          String2.toCSSVString(edd.dataVariableDestinationNames()),
          "longitude, latitude, altitude, time, ID, BasisOfRecord, BoundingBox, "
              + "CatalogNumber, Citation, Class, CollectionCode, Collector, "
              + "CollectorNumber, ContinentOcean, CoordinatePrecision, Country, "
              + "County, DateLastModified, DayCollected, DayIdentified, DepthRange, "
              + "EndDayCollected, EndJulianDay, EndLatitude, EndLongitude, EndMonthCollected, "
              + "EndTimeofDay, EndYearCollected, Family, FieldNumber, GMLFeature, Genus, "
              + "IdentifiedBy, IndividualCount, InstitutionCode, JulianDay, Kingdom, "
              + "LifeStage, Locality, MaximumDepth, MaximumElevation, MinimumElevation, "
              + "MonthCollected, MonthIdentified, Notes, ObservedIndividualCount, "
              + "ObservedWeight, Order, Phylum, PreparationType, PreviousCatalogNumber, "
              + "RecordURL, RelatedCatalogItem, RelationshipType, SampleSize, ScientificName, "
              + "ScientificNameAuthor, Sex, Source, Species, StartDayCollected, "
              + "StartJulianDay, StartLatitude, StartLongitude, StartMonthCollected, "
              + "StartTimeofDay, StartYearCollected, Start_EndCoordinatePrecision, "
              + "StateProvince, Subgenus, Subspecies, Temperature, TimeOfDay, TimeZone, "
              + "TypeStatus, YearCollected, YearIdentified",
          "");

    } catch (Throwable t) {
      throw new RuntimeException("Unexpected EDDTableFromOBIS.testGenerateDatasetsXml error.", t);
    }
  }

  /** rutgers obis, failing since 2011-01 */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testRutgers() throws Throwable {
    // testVerboseOn();
    int language = 0;
    // DigirHelper.verbose = true;
    // DigirHelper.reallyVerbose = true;
    // TableXmlHandler.verbose = true;

    String name, tName, results, tResults, expected, userDapQuery;
    String error = "";
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    // Is Rutgers obis down/not responding?
    // Send an email to obissupport@marine.rutgers.edu, for example:
    // I note that http://iobis.marine.rutgers.edu/digir2/DiGIR.php recently started
    // to return a Proxy Error message instead of an xml response. Has the OBIS
    // server moved, or is it down, or...?
    // Thank you for looking into this.
    try {
      EDDTable obis =
          (EDDTable) EDDTableFromOBIS.oneFromDatasetsXml(null, "rutgersGhmp"); // should work

      // getEmpiricalMinMax just do once
      // globecBottle.getEmpiricalMinMax(language, "2002-07-01", "2002-09-01", false,
      // true);
      // if (true) System.exit(1);

      // .das das isn't affected by userDapQuery
      userDapQuery = "&Genus=\"Macrocystis\"";
      tName =
          obis.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              obis.className(),
              ".das");
      results =
          String2.annotatedString(
              File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
      // String2.log(results);
      expected =
          "Attributes {[10]\n"
              + " s {[10]\n"
              + "  longitude {[10]\n"
              + "    String _CoordinateAxisType \"Lon\";[10]\n"
              + "    Float64 actual_range -180.0, 180.0;[10]\n"
              + "    String axis \"X\";[10]\n"
              + "    String ioos_category \"Location\";[10]\n"
              + "    String long_name \"Longitude\";[10]\n"
              + "    String standard_name \"longitude\";[10]\n"
              + "    String units \"degrees_east\";[10]\n"
              + "  }[10]\n"
              + "  latitude {[10]\n";
      tResults = results.substring(0, Math.min(results.length(), expected.length()));
      Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);
      expected =
          "  YearIdentified {[10]\n"
              + "    String comment \"The year portion of the date when the Collection Item was identified; as four digits [-9999..9999], e.g., 1906, 2002.\";[10]\n"
              + "    String ioos_category \"Time\";[10]\n"
              + "  }[10]\n"
              + " }[10]\n"
              + "  NC_GLOBAL {[10]\n"
              + "    String cdm_data_type \"Point\";[10]\n"
              + "    String citation \"Living marine legacy of Gwaii Haanas. I: Marine plant baseline to 1999 and plant-related management issues.\";[10]\n"
              + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";[10]\n"
              + "    String creator_email \"SloanNormPCA@DFO-MPO.GC.CA\";[10]\n"
              + "    Float64 Easternmost_Easting 180.0;[10]\n"
              + "    Float64 geospatial_lat_max 90.0;[10]\n"
              + "    Float64 geospatial_lat_min -90.0;[10]\n"
              + "    String geospatial_lat_units \"degrees_north\";[10]\n"
              + "    Float64 geospatial_lon_max 180.0;[10]\n"
              + "    Float64 geospatial_lon_min -180.0;[10]\n"
              + "    String geospatial_lon_units \"degrees_east\";[10]\n"
              + "    String geospatial_vertical_positive \"up\";[10]\n"
              + "    String geospatial_vertical_units \"m\";[10]\n"
              + "    String history \""
              + today
              + " http://iobis.marine.rutgers.edu/digir2/DiGIR.php[10]\n"
              + today
              + " "
              + EDStatic.erddapUrl
              + // in tests, always use non-https url
              "/tabledap/rutgersGhmp.das\";[10]\n"
              + "    String infoUrl \"http://gcmd.nasa.gov/KeywordSearch/Metadata.do?Portal=caobis&MetadataType=0&KeywordPath=&MetadataView=Full&EntryId=OBIS.Gwaii_MarPlants\";[10]\n"
              + "    String institution \"DFO Canada\";[10]\n"
              + "    String keywords \"Aquatic Habitat, Marine Biology, Marine Plants\";[10]\n"
              + "    String license \"The data may be used and redistributed for free but is not intended[10]\n"
              + "for legal use, since it may contain inaccuracies. Neither the data[10]\n"
              + "Contributor, ERD, NOAA, nor the United States Government, nor any[10]\n"
              + "of their employees or contractors, makes any warranty, express or[10]\n"
              + "implied, including warranties of merchantability and fitness for a[10]\n"
              + "particular purpose, or assumes any legal liability for the accuracy,[10]\n"
              + "completeness, or usefulness, of this information.[10]\n"
              + "[10]\n"
              + "By using OBIS data, I agree that, in any publication or presentation[10]\n"
              + "of any sort based wholly or in part on such data, I will:[10]\n"
              + "[10]\n"
              + "1. Acknowledge the use of specific records from contributing databases[10]\n"
              + "in the form appearing in the GLOBAL 'citation' attribute thereof (if any);[10]\n"
              + "and acknowledge the use of the OBIS facility in one of the following[10]\n"
              + "prescribed forms:[10]\n"
              + "[10]\n"
              + "For OBIS website:[10]\n"
              + "[10]\n"
              + "Ocean Biogeographic Information System. [date accessed][10]\n"
              + "http://iobis.marine.rutgers.edu/digir2/DiGIR.php[10]\n"
              + "[10]\n"
              + "For data used:[10]\n"
              + "[10]\n"
              + "Author, initials. Database title. Retrieved [date accessed] from[10]\n"
              + "http://iobis.marine.rutgers.edu/digir2/DiGIR.php[10]\n"
              + "[10]\n"
              + "Example:[10]\n"
              + "[10]\n"
              + "Stocks, K. SeamountsOnline: an online information system for seamount[10]\n"
              + "biology. Version 3.1. Retrieved [date accessed] from[10]\n"
              + "http://iobis.marine.rutgers.edu/digir2/DiGIR.php.[10]\n"
              + "[10]\n"
              + "2. For information purposes, provide to SloanNormPCA@DFO-MPO.GC.CA[10]\n"
              + "the full citation of any publication I make (printed or electronic)[10]\n"
              + "that cites OBIS or any constituent part.[10]\n"
              + "[10]\n"
              + "3. Recognize the limitations of data in OBIS:[10]\n"
              + "OBIS is comparable to a scientific journal that makes data freely[10]\n"
              + "available on the internet. Thus the geographic and taxonomic scope,[10]\n"
              + "and quantity of data provided, depend on the scientists and[10]\n"
              + "organizations that provide data. However, in contrast to data in a[10]\n"
              + "journal, the 'reader' can select and combine data in OBIS from a[10]\n"
              + "variety of sources. OBIS and its users give feedback on data quality[10]\n"
              + "and possible errors to data providers. Because data providers are[10]\n"
              + "willing to correct errors, the quality of the data will increase in[10]\n"
              + "time. How OBIS provides quality assurance, who is primarily[10]\n"
              + "responsible for data published in OBIS (its owners), issues to be[10]\n"
              + "considered in using the data, and known gaps in the data, are[10]\n"
              + "described below.[10]\n"
              + "[10]\n"
              + "Quality assurance[10]\n"
              + "[10]\n"
              + "Only data from authoritative scientists and science organizations[10]\n"
              + "approved by OBIS are served. All data are subject to quality control[10]\n"
              + "procedures before publication, and at regular intervals, with data[10]\n"
              + "providers informed of any discrepancies and potential errors (e.g.[10]\n"
              + "species names spelt incorrectly, mapping errors). OBIS also benefits[10]\n"
              + "from user peer-review and feedback to identify technical, geographic,[10]\n"
              + "and taxonomic errors in data served. However, although errors will[10]\n"
              + "exist as they do in any publication, OBIS is confident that the data[10]\n"
              + "are the best available in electronic form. That said, the user needs[10]\n"
              + "sufficient knowledge to judge the appropriate use of the data, i.e.[10]\n"
              + "for what purpose it is fit.[10]\n"
              + "[10]\n"
              + "Many of the data published through OBIS have voucher specimens in[10]\n"
              + "institutional collections and museums, images of observations, and[10]\n"
              + "the original identifier of the specimens is often credited or will[10]\n"
              + "be contactable from the data custodian.[10]\n"
              + "[10]\n"
              + "Data ownership[10]\n"
              + "[10]\n"
              + "Data providers retain ownership of the data provided. OBIS does not[10]\n"
              + "own or control or limit the use of any data or products accessible[10]\n"
              + "through its website. Accordingly, it does not take responsibility[10]\n"
              + "for the quality of such data or products, or the use that people may[10]\n"
              + "make of them.[10]\n"
              + "[10]\n"
              + "Data use[10]\n"
              + "[10]\n"
              + "Appropriate caution is necessary in the interpretation of results[10]\n"
              + "derived from OBIS. Users must recognize that the analysis and[10]\n"
              + "interpretation of data require background knowledge and expertise[10]\n"
              + "about marine biodiversity (including ecosystems and taxonomy).[10]\n"
              + "Users should be aware of possible errors, including in the use of[10]\n"
              + "species names, geo-referencing, data handling, and mapping. They[10]\n"
              + "should cross-check their results for possible errors, and qualify[10]\n"
              + "their interpretation of any results accordingly.[10]\n"
              + "[10]\n"
              + "Users should be aware that OBIS is a gateway to a system of databases[10]\n"
              + "distributed around the world. More information on OBIS data is[10]\n"
              + "available from the data sources websites and contact persons. Users[10]\n"
              + "should email any questions concerning OBIS data or tools (e.g. maps)[10]\n"
              + "to the appropriate contact person and copy this request to[10]\n"
              + "SloanNormPCA@DFO-MPO.GC.CA .[10]\n"
              + "[10]\n"
              + "Data gaps[10]\n"
              + "[10]\n"
              + "Major gaps in data and knowledge about the oceans are reflected in[10]\n"
              + "OBIS' data coverage. Note the following:[10]\n"
              + "Most of the planet is more than 1 km under water: this deep sea is[10]\n"
              + "the least surveyed part of our world.[10]\n"
              + "Coastal areas have been adequately sampled only for the distribution[10]\n"
              + "of most vertebrates (birds, mammals, reptiles, larger fish).[10]\n"
              + "The oceans have been better sampled in the northern than the[10]\n"
              + "southern hemisphere, as reflected in the distribution of data in[10]\n"
              + "OBIS.[10]\n"
              + "Most marine species have not yet been recognized or named. A major[10]\n"
              + "effort is required to describe marine species, especially[10]\n"
              + "invertebrates and deep-sea organisms.[10]\n"
              + "Of the marine species that have been described, some have been[10]\n"
              + "discovered to be several species, and others combined into single[10]\n"
              + "species. Thus, there are changes in the application of species names[10]\n"
              + "over time. A checklist of all current marine species names is not[10]\n"
              + "available but it is estimated that 230,000 have been described.[10]\n"
              + "Only about half of these names have been organized into global[10]\n"
              + "species checklists. OBIS includes distribution data on (a) many of[10]\n"
              + "these validated names and (b) additional names that remain to be[10]\n"
              + "organized into global species checklists. Thus, OBIS has some[10]\n"
              + "distribution data for about one third of the known marine species.[10]\n"
              + "Some species distribution data are not available in any form, as[10]\n"
              + "they have not have been published nor made available for databases.[10]\n"
              + "Only some of the recently collected, and less of the older published,[10]\n"
              + "data have been entered into databases. Thus databases are incomplete.[10]\n"
              + "Of existing databases, many are not connected to OBIS.[10]\n"
              + "[10]\n"
              + "You can help address these data gaps by (a) recognizing and[10]\n"
              + "encouraging scientists and organizations to make their data available[10]\n"
              + "online so they are accessible to OBIS, and (b) advocating for and[10]\n"
              + "carrying out field surveys and taxonomic studies designed to fill[10]\n"
              + "geographic and taxonomic gaps in knowledge.[10]\n"
              + "\";[10]\n"
              + "    Float64 Northernmost_Northing 90.0;[10]\n"
              + "    String sourceUrl \"http://iobis.marine.rutgers.edu/digir2/DiGIR.php\";[10]\n"
              + "    Float64 Southernmost_Northing -90.0;[10]\n"
              + "    String standard_name_vocabulary \"CF Standard Name Table v55\";[10]\n"
              + "    String subsetVariables \"ScientificName\";[10]\n"
              + "    String summary \"The database covers the Haida Gwaii archipelago on the West Coast of Canada, including all species of the Haida Gwaii region from any published source, accessible collection and unpublished observations from scientists.Lists all marine plant species and maps their distributions from the first records (1911) to 1999 and includes 348 seaweed and 4 seagrass species from 456 intertidal to shallow subtidal locations. This inventory had detailed regional starting points (Hawkes et al. 1978; Scagel et al. 1993) and >90% of the plant species are represented by specimens in the Phycological Herbarium of the University of British Columbia Botany Department. OBIS Schema concepts implemented in this data set are:DateLastModified, InstitutionCode, CollectionCode, CatalogNumber, ScientificName, Phylum, Class, Order, Family, Genus, Species, Subspecies, ScientificNameAuthor, YearCollected, MonthCollected, DayCollected, Country, Locality, Longitude, Latitude, Citation, DepthRange. For OBIS Schema concept details see http://www.iobis.org/tech/provider/[10]\n"
              + "[10]\n"
              + "DiGIR is an engine which takes XML requests for data and returns a data[10]\n"
              + "subset stored as XML data (as defined in a schema). For more DiGIR[10]\n"
              + "information, see http://digir.sourceforge.net/ ,[10]\n"
              + "http://diveintodigir.ecoforge.net/draft/digirdive.html ,[10]\n"
              + "and http://digir.net/prov/prov_manual.html .[10]\n"
              + "A list of Digir providers is at[10]\n"
              + "http://bigdig.ecoforge.net/wiki/SchemaStatus .[10]\n"
              + "[10]\n"
              + "Darwin is the original schema for use with the DiGIR engine.[10]\n"
              + "[10]\n"
              + "The Ocean Biogeographic Information System (OBIS) schema extends[10]\n"
              + "Darwin. For more OBIS info, see http://www.iobis.org .[10]\n"
              + "See the OBIS schema at http://www.iobis.org/tech/provider/questions .[10]\n"
              + "[10]\n"
              + "Queries: Although OBIS datasets have many variables, most variables[10]\n"
              + "have few values.  The only queries that are likely to succeed MUST[10]\n"
              + "include a constraint for Genus= and MAY include constraints for[10]\n"
              + "Species=, longitude, latitude, and time.[10]\n"
              + "[10]\n"
              + "Most OBIS datasets return a maximum of 1000 rows of data per request.[10]\n"
              + "The limitation is imposed by the OBIS administrators.[10]\n"
              + "[10]\n"
              + "Available Genera (and number of records): Acrochaetium (5),[10]\n"
              + "Acrosiphonia (30), Acrothrix (1), Agarum (40), Ahnfeltia (13),[10]\n"
              + "Alaria (144), Amplisiphonia (3), Analipus (27), Antithamnion (33),[10]\n"
              + "Antithamnionella (2), Audouinella (4), Bangia (11),[10]\n"
              + "Batrachospermum (1), Blidingia (6), Bolbocoleon (1),[10]\n"
              + "Bonnemaisonia (10), Bossiella (57), Botryocladia (4),[10]\n"
              + "Calliarthron (30), Callithamnion (57), Callophyllis (66),[10]\n"
              + "Capsosiphon (4), Ceramium (55), Chaetomorpha (2), Chondracanthus (35),[10]\n"
              + "Cladophora (65), Clathromorphum (4), Codium (174), Coilodesme (5),[10]\n"
              + "Collinsiella (6), Colpomenia (26), Constantinea (14), Corallina (122),[10]\n"
              + "Costaria (89), Cryptonemia (4), Cryptopleura (27),[10]\n"
              + "Cryptosiphonia (70), Cumagloia (3), Cymathere (45), Cystoseira (7),[10]\n"
              + "Delesseria (15), Derbesia (18), Desmarestia (99), Dictyosiphon (2),[10]\n"
              + "Dictyota (19), Dilsea (4), Ectocarpus (20), Egregia (135),[10]\n"
              + "Eisenia (3), Elachista (9), Endocladia (88), Enteromorpha (95),[10]\n"
              + "Erythrocladia (3), Erythrophyllum (12), Erythrotrichia (8),[10]\n"
              + "Eudesme (5), Euthora (3), Farlowia (8), Fauchea (18), Feldmannia (2),[10]\n"
              + "Fryeella (6), Fucus (213), Gelidium (4), Gloiopeltis (61),[10]\n"
              + "Gloiosiphonia (1), Gracilaria (14), Grateloupia (1),[10]\n"
              + "Halosaccion (129), Halymenia (6), Haplogloia (8), Harveyella (1),[10]\n"
              + "Hecatonema (1), Hedophyllum (116), Herposiphonia (14),[10]\n"
              + "Heterosiphonia (15), Hildenbrandia (12), Hollenbergia (8),[10]\n"
              + "Hymenena (27), Kallymeniopsis (2), Laminaria (238), Leathesia (95),[10]\n"
              + "Lessoniopsis (67), Lithophyllum (7), Lithothamnium (88),[10]\n"
              + "Lithothrix (1), Lola (1), Macrocystis (124), Melanosiphon (7),[10]\n"
              + "Melobesia (4), Membranoptera (17), Mesophyllum (19), Microcladia (53),[10]\n"
              + "Monostroma (10), Myriogramme (1), Myrionema (4), Navicula (1),[10]\n"
              + "Nemalion (2), Neodilsea (1), Neoptilota (18), Nienburgia (2),[10]\n"
              + "Nitophyllum (13), Odonthalia (134), Opuntiella (26), Palmaria (26),[10]\n"
              + "Pelvetiopsis (1), Percursaria (1), Petalonia (8), Peyssonnelia (11),[10]\n"
              + "Phaeosaccion (2), Phaeostrophion (2), Phycodrys (3),[10]\n"
              + "Phyllospadix (117), Pikea (6), Pilayella (17), Pleonosporium (2),[10]\n"
              + "Pleurophycus (42), Plocamium (52), Polyneura (39), Polysiphonia (68),[10]\n"
              + "Porphyra (147), Porphyropsis (5), Prasiola (9), Prionitis (53),[10]\n"
              + "Pseudolithophyllum (5), Pterochondria (9), Pterosiphonia (51),[10]\n"
              + "Pterygophora (21), Ptilota (46), Pugetia (11), Punctaria (2),[10]\n"
              + "Ralfsia (24), Rhizoclonium (10), Rhodochorton (5), Rhodomela (7),[10]\n"
              + "Rhodophysema (2), Rhodoptilum (4), Rhodymenia (6), Rosenvingiella (1),[10]\n"
              + "Ruppia (9), Salicornia (10), Sarcodiotheca (12), Sargassum (19),[10]\n"
              + "Saundersella (10), Schizymenia (8), Scinaia (12), Scytosiphon (11),[10]\n"
              + "Smithora (15), Soranthera (23), Sphacelaria (5), Spongomorpha (13),[10]\n"
              + "Spongonema (3), Stictyosiphon (2), Stylonema (1), Syringoderma (6),[10]\n"
              + "Trentepohlia (1), Turnerella (1), Ulothrix (6), Ulva (154),[10]\n"
              + "Ulvella (1), Urospora (2), Verrucaria (7), Zostera (82)\";[10]\n"
              + "    String title \"OBIS - Gwaii Haanas Marine Plants (OBIS Canada)\";[10]\n"
              + "    Float64 Westernmost_Easting -180.0;[10]\n"
              + "  }[10]\n"
              + "}[10]\n"
              + "[end]";
      int tpo = results.indexOf(expected.substring(0, 10));
      Test.ensureEqual(results.substring(tpo), expected, "");

      // .csv
      tName =
          obis.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              obis.className(),
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          // 2010-07-20 -132.4223 changed to -132.422 in 3 places
          // 2010-08-10 changed back
          "longitude, latitude, altitude, time, ID, BasisOfRecord, BoundingBox, CatalogNumber, Citation, Class, CollectionCode, Collector, CollectorNum"
              + "ber, ContinentOcean, CoordinatePrecision, Country, County, DateLastModified, DayCollected, DayIdentified, DepthRange, EndDayCollected, EndJu"
              + "lianDay, EndLatitude, EndLongitude, EndMonthCollected, EndTimeofDay, EndYearCollected, Family, FieldNumber, GMLFeature, Genus, IdentifiedBy,"
              + " IndividualCount, InstitutionCode, JulianDay, Kingdom, LifeStage, Locality, MaximumDepth, MaximumElevation, MinimumElevation, MonthCollected"
              + ", MonthIdentified, Notes, ObservedIndividualCount, ObservedWeight, Order, Phylum, PreparationType, PreviousCatalogNumber, RecordURL, Related"
              + "CatalogItem, RelationshipType, SampleSize, ScientificName, ScientificNameAuthor, Sex, Source, Species, StartDayCollected, StartJulianDay, St"
              + "artLatitude, StartLongitude, StartMonthCollected, StartTimeofDay, StartYearCollected, Start_EndCoordinatePrecision, StateProvince, Subgenus,"
              + " Subspecies, Temperature, TimeOfDay, TimeZone, TypeStatus, YearCollected, YearIdentified\n"
              + "degrees_east, degrees_north, m, UTC, , , , , , , , , , , m, , , UTC, , , , , , degrees_north, degrees_east, , hours, , , , , , , count, , , , ,"
              + " , m, m, m, , , , count, kg, , , , , , , , , , , , , , , , degrees_north, degrees_east, , hours, , m, , , , degree_C, hours, , , , \n"
              + "-131.66368, 52.65172, NaN, 1992-01-01T00:00:00Z, BIO:GHMP:100-MACRINT, D, , 100-MACRINT, \"Harper, John R., William T. Austin, Mary Morris, P"
              + ". Douglas Reimer and Richard Reitmeier. 1994. Ecological Classification of Gwaii Haanas  Biophysical Inventory of Coastal Resources\", Phaeop"
              + "hyceae, GHMP, , , , NaN, Canada, , 2003-02-05T17:00:00Z, NaN, NaN, , NaN, NaN, 52.65172, -131.66368, NaN, NaN, 1992, Lessoniaceae, , , Macro"
              + "cystis, , NaN, BIO, NaN, Plantae, , 90, NaN, NaN, NaN, NaN, NaN, , NaN, NaN, Laminariales, Phaeophyta, , , , , , , Macrocystis integrifolia,"
              + " Bory, , , integrifolia, NaN, NaN, 52.65172, -131.66368, NaN, NaN, 1992, NaN, , , , NaN, NaN, , , 1992, NaN\n"
              + "-132.4223, 53.292, NaN, 1981-01-01T00:00:00Z, BIO:GHMP:10036-MACRINT, D, , 10036-MACRINT, Various. Ongoing. University of British Columbia H"
              + "erbarium (UBC) Algae Collection, Phaeophyceae, GHMP, , , , NaN, Canada, , 2003-02-05T17:00:00Z, NaN, NaN, , NaN, NaN, 53.292, -132.4223, NaN"
              + ", NaN, 1981, Lessoniaceae, , , Macrocystis, , NaN, BIO, NaN, Plantae, , \"head of rennell sound,queen charlotte islands\", NaN, NaN, NaN, NaN,"
              + " NaN, , NaN, NaN, Laminariales, Phaeophyta, , , , , , , Macrocystis integrifolia, Bory, , , integrifolia, NaN, NaN, 53.292, -132.4223, NaN, "
              + "NaN, 1981, NaN, , , , NaN, NaN, , , 1981, NaN\n";
      tResults = results.substring(0, Math.min(results.length(), expected.length()));
      Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

      // .csv String =
      userDapQuery =
          "longitude,latitude,time,ID,Genus,Species&Genus=\"Macrocystis\""
              + "&longitude>-134&longitude<-131&latitude>53&latitude<55&time<1973-01-01"; // Carcharodon";
      tName =
          obis.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              obis.className() + "latlon",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude, latitude, time, ID, Genus, Species\n"
              + "degrees_east, degrees_north, UTC, , , \n"
              + "-132.9562, 53.5321, 1963-01-01T00:00:00Z, BIO:GHMP:10041-MACRINT, Macrocystis, integrifolia\n"
              + "-133.00264, 54.1732, 1963-01-01T00:00:00Z, BIO:GHMP:10071-MACRINT, Macrocystis, integrifolia\n"
              + "-133.03497, 54.1846, 1965-01-01T00:00:00Z, BIO:GHMP:10118-MACRINT, Macrocystis, integrifolia\n"
              + "-131.92552, 53.04651, 1972-01-01T00:00:00Z, BIO:GHMP:183-MACRINT, Macrocystis, integrifolia\n"
              + "-131.91228, 53.05358, 1972-01-01T00:00:00Z, BIO:GHMP:184-MACRINT, Macrocystis, integrifolia\n"
              + "-131.8887, 53.05573, 1972-01-01T00:00:00Z, BIO:GHMP:185-MACRINT, Macrocystis, integrifolia\n"
              + "-131.89433, 53.05788, 1972-01-01T00:00:00Z, BIO:GHMP:186-MACRINT, Macrocystis, integrifolia\n"
              + "-131.87213, 53.06344, 1972-01-01T00:00:00Z, BIO:GHMP:187-MACRINT, Macrocystis, integrifolia\n"
              + "-131.76837, 53.02226, 1972-01-01T00:00:00Z, BIO:GHMP:188-MACRINT, Macrocystis, integrifolia\n"
              + "-131.67632, 53.01082, 1972-01-01T00:00:00Z, BIO:GHMP:189-MACRINT, Macrocystis, integrifolia\n"
              + "-132.01529, 53.24198, 1972-01-01T00:00:00Z, BIO:GHMP:190-MACRINT, Macrocystis, integrifolia\n"
              + "-132.00137, 53.24404, 1972-01-01T00:00:00Z, BIO:GHMP:191-MACRINT, Macrocystis, integrifolia\n"
              + "-131.98787, 53.25524, 1972-01-01T00:00:00Z, BIO:GHMP:192-MACRINT, Macrocystis, integrifolia\n"
              + "-131.9922, 53.24963, 1972-01-01T00:00:00Z, BIO:GHMP:193-MACRINT, Macrocystis, integrifolia\n"
              + "-131.97739, 53.25858, 1972-01-01T00:00:00Z, BIO:GHMP:194-MACRINT, Macrocystis, integrifolia\n"
              + "-131.9993, 53.21888, 1972-01-01T00:00:00Z, BIO:GHMP:195-MACRINT, Macrocystis, integrifolia\n"
              + "-132.02905, 53.2174, 1972-01-01T00:00:00Z, BIO:GHMP:196-MACRINT, Macrocystis, integrifolia\n"
              + "-132.04869, 53.22366, 1972-01-01T00:00:00Z, BIO:GHMP:197-MACRINT, Macrocystis, integrifolia\n"
              + "-132.08171, 53.22519, 1972-01-01T00:00:00Z, BIO:GHMP:198-MACRINT, Macrocystis, integrifolia\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      /*
       * TROUBLE This test fails because obis returns max=1000 records, mostly
       * irrelevant,
       * so standardize results table removes all but 1 record.
       * ???So do test for max records and throw exception if that is what is returned
       * (because it implies partial response)???
       * //.csv similar test String > <
       * userDapQuery =
       * "longitude,latitude,time,ID,Genus,Species&Genus>\"Mac\"&Genus<=\"Mad\"" +
       * "&longitude>-134&longitude<-131&latitude>53&latitude<55&time<1973-01-01";
       * //Carcharodon";
       * tName = obis.makeNewFileForDapQuery(null, null, userDapQuery,
       * EDStatic.fullTestCacheDirectory,
       * obis.className() + "latlon", ".csv");
       * results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory +
       * tName);
       * //String2.log(results);
       * Test.ensureEqual(results, expected, "\nresults=\n" + results);
       */

      // .csv similar test String > <
      userDapQuery =
          "longitude,latitude,time,ID,Genus,Species&Genus=\"Macrocystis\"&Species>\"inte\"&Species<=\"intf\""
              + "&longitude>-134&longitude<-131&latitude>53&latitude<55&time<1973-01-01"; // Carcharodon";
      tName =
          obis.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              obis.className() + "latlon",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // .csv similar test String regex
      userDapQuery =
          "longitude,latitude,time,ID,Genus,Species&Genus=\"Macrocystis\"&Species=~\"(zztop|integ.*)\""
              + "&longitude>-134&longitude<-131&latitude>53&latitude<55&time<1973-01-01"; // Carcharodon";
      tName =
          obis.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              obis.className() + "latlon",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException(
          "Unexpected error",
          // 2010-07-27 to 2011-01 failed with\n" +
          // " java.net.ConnectException: Connection refused: connect",
          t);
    }
  }

  /** fishbase stopped working in 2009-01 */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testFishbase() throws Throwable {
    // testVerboseOn();
    int language = 0;
    // DigirHelper.verbose = true;
    // DigirHelper.reallyVerbose = true;
    // TableXmlHandler.verbose = true;

    String name, tName, results, tResults, expected, userDapQuery;
    String error = "";
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    try {
      EDDTable fishbase =
          (EDDTable) EDDTableFromOBIS.oneFromDatasetsXml(null, "fishbaseObis"); // should work
      userDapQuery =
          "longitude,latitude,time,ID,Genus,Species,Citation&Genus=\"Carcharodon\"&time>=1990-01-01";
      tName =
          fishbase.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              fishbase.className() + "FishBaseGraph",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude, latitude, time, ID, Genus, Species, Citation\n"
              + "degrees_east, degrees_north, UTC, , , , \n"
              + "26.5833333333333, -33.7166666666667, 1996-01-01T00:00:00Z, RUSI:36670:RUSI 50000, Carcharodon, carcharias, \n"
              + "NaN, NaN, 1997-01-01T00:00:00Z, IGFA:40637:IGFA 751-2090, Carcharodon, carcharias, \n"
              + "NaN, NaN, 1991-01-01T00:00:00Z, IGFA:40637:IGFA 751-2088, Carcharodon, carcharias, \n"
              + "NaN, NaN, 1994-01-01T00:00:00Z, IGFA:40637:IGFA 751-2089, Carcharodon, carcharias, \n"
              + "131.016666666667, -4.48333333333333, 1991-09-10T00:00:00Z, ZMUC:40919:ZMUC P 0566, Carcharodon, carcharias, \n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // data for mapExample
      tName =
          fishbase.makeNewFileForDapQuery(
              language,
              null,
              null,
              "longitude,latitude&Genus=Carcharodon&longitude!=NaN",
              EDStatic.fullTestCacheDirectory,
              fishbase.className() + "Map",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude, latitude\n"
              + "degrees_east, degrees_north\n"
              + "-18.0, 15.0\n"
              + "55.0, -20.0\n"
              + "5.0, 40.0\n"
              + "5.0, 40.0\n"
              + "43.8536111195882, -33.1513888835907\n"
              + "3.71666666666667, 43.4166666666667\n"
              + "55.0, -20.0\n"
              + "-122.889722, 38.140556\n"
              + "-118.683333, 34.033333\n"
              + "8.95, 44.4167\n"
              + "25.7, -34.0333333333333\n"
              + "31.05, -29.8666666666667\n"
              + "26.5833333333333, -33.7166666666667\n"
              + "-73.0, 39.0\n"
              + "152.083333333333, -32.7\n"
              + "151.566666666667, -33.0833333333333\n"
              + "25.5666666666667, -33.9333333333333\n"
              + "133.666666666667, -32.1166666666667\n"
              + "134.2, -32.7833333333333\n"
              + "131.016666666667, -4.48333333333333\n"
              + "-66.75, 44.6666666666667\n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

      // expected error didn't occur!
      String2.pressEnterToContinue(
          "\n" + MustBe.getStackTrace() + "An expected error didn't occur at the above location.");

    } catch (Throwable t) {
      String2.pressEnterToContinue(
          MustBe.throwableToString(t) + "\nExpected obis fishbase error (since ~2009-01-20).");
    }
  }

  /** This works but not useful. seamap is split into ~150 chunks. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testSeamap() throws Throwable {
    // testVerboseOn();
    int language = 0;
    // DigirHelper.verbose = true;
    // DigirHelper.reallyVerbose = true;
    // TableXmlHandler.verbose = true;

    String name, tName, results, tResults, expected, userDapQuery;
    String error = "";
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    /*
     * <dataset type="EDDTableFromOBIS" datasetID="dukeSeamap">
     * <sourceUrl>http://seamap.env.duke.edu/digir/DiGIR.php</sourceUrl>
     * <sourceCode>obis-seamap</sourceCode>
     * <addAttributes>
     * <att name="citation">Read, A. J. and P. Halpin. Editors. 2003. OBIS-SEAMAP.
     * World Wide Web electronic publication. http://www.obis.env.duke.edu, April
     * 2003</att>
     * <att name="creator_email">dhyrenbach@duke.edu</att>
     * <att name="infoUrl">http://obis.env.duke.edu</att>
     * <att name="institution">Duke</att>
     * <att name="keywords">marine mammal, bird, turtle, seabird, sea turtle, OBIS,
     * marine, whale, dolphin, seal, pinniped</att>
     * <att name="license">[standard]</att>
     * <att name="summary">
     * The Ocean Biogeographic Information System - Spatial Ecological Analysis of
     * Megavertabrate Populations (OBIS-SEAMAP) is a spatially referenced database
     * of data related to marine mammals, birds and turtles. This project is one of
     * several nodes comprising the Ocean Biogeographic Information System (OBIS).
     *
     * [OBIS_SUMMARY]
     *
     * Available Genera (and number of records): Actitis (13),
     * Aechmophorus (13015), Alca (362), Alle (8858), Anas (20383),
     * Anous (66), Anser (1), Aphriza (17), Aptenodytes (43),
     * Arctocephalus (20), Ardea (8542), Arenaria (141), Aythya (17396),
     * Balaenoptera (9140), Berardius (61), Brachyramphus (2530),
     * Branta (3335), Brevoortia (149), Bucephala (98937), Bulweria (32),
     * Buteo (31), Calidris (185), Callorhinus (1799), Calonectris (2217),
     * Caranx (24), Caretta (18679), Casmerodius (2), Catharacta (1022),
     * Cathartes (5), Catoptrophorus (14), Cephalorhynchus (26),
     * Cepphus (11227), Cerorhinca (8842), Cetorhinus (472), Charadrius (47),
     * Chelonia (1192), Chen (83), Chionis (7), Chlidonias (809),
     * Chloroscombrus (1), Circus (51), Clangula (3525), Colaptes (5),
     * Columba (48), Corvus (3119), Coryphaena (120), Cubaris (2047),
     * Cygnus (301), Cystophora (22), Daption (1688), Delphinapterus (9),
     * Delphinus (3255), Dermochelys (473), Diomedea (2779), Egretta (5),
     * Enhydra (2079), Eretmochelys (2), Erignathus (43),
     * Eschrichtius (1648), Eubalaena (808), Eudyptes (79), Eumetopias (293),
     * Falco (15), Feresa (77), Fratercula (2463), Fregata (560),
     * Fregetta (791), Fulica (32), Fulmarus (42217), Gallinago (3),
     * Garrodia (25), Gavia (15422), Genus (1364), Globicephala (2506),
     * Grampus (2352), Gygis (14), Haematopus (162), Haliaeetus (2433),
     * Halichoerus (9487), Halobaena (557), Heteroscelus (4), Himantopus (2),
     * Histrio (271), Histrionicus (4151), Hydrobates (165), Hyperoodon (52),
     * Indopacetus (4), Kogia (415), Lagenodelphis (38),
     * Lagenorhynchus (2948), Larus (138165), Lepidochelys (1637),
     * Limnodromus (12), Limosa (34), Lissodelphis (570), Lontra (75),
     * Lophodytes (1045), Macronectes (1412), Manta (53), Megaceryle (833),
     * Megaptera (12415), Melanitta (54800), Mergus (15475),
     * Mesoplodon (331), Mirounga (7728), Mola (2757), Morus (8944),
     * Myliobatis (2), Nesofregetta (1), Numenius (16), Oceanites (9448),
     * Oceanodroma (11690), Odobenus (146), Opisthonema (1), Orcinus (423),
     * Oxyura (756), Pachyptila (3407), Pagodroma (454), Pagophila (1305),
     * Pagophilus (47), Pandion (100), Patagioenas (8), Pelagodroma (18),
     * Pelecanoides (410), Pelecanus (3148), Peponocephala (43),
     * Phaethon (209), Phalacrocorax (21676), Phalacrocorax Spp. (1962),
     * Phalaropus (10516), Phoca (9040), Phocoena (14526),
     * Phocoenoides (4385), Phoebastria (12166), Phoebetria (1035),
     * Physeter (1603), Pluvialis (53), Podiceps (9543), Podilymbus (3),
     * Pogonias (1), Polysticta (8), Prionace (464), Procellaria (2723),
     * Pseudobulweria (14), Pseudorca (76), Pterodroma (2128),
     * Ptychoramphus (11032), Puffinus (40941), Pusa (125), Pygoscelis (480),
     * Rachycentron (6), Recurvirostra (14), Rhincodon (18), Rhinoptera (55),
     * Rissa (26672), Rynchops (3), Scomberomorus (18), Somateria (568),
     * Spheniscus (13), Sphyrna (317), Stenella (5011), Steno (268),
     * Stercorarius (7383), Sterna (6974), Stomolophus (3), Sula (831),
     * Synthliboramphus (849), Thalassoica (898), Thunnus (19),
     * Trichechus (1), Tringa (1083), Tursiops (12455), Uria (48880),
     * Ursus (25), Xiphias (16), Zalophus (4668), Ziphius (455)
     * </att>
     * <att name="title">OBIS - SEAMAP (Duke) - marine mammals, birds and
     * turtles</att>
     * </addAttributes>
     * </dataset>
     */

    try {
      EDDTable dukeSeamap = (EDDTable) EDDTableFromOBIS.oneFromDatasetsXml(null, "dukeSeamap");
      userDapQuery =
          "longitude,latitude,time,ID,Genus,Species,Citation&Genus=\"Carcharodon\"&time>=1990-01-01";
      tName =
          dukeSeamap.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              EDStatic.fullTestCacheDirectory,
              dukeSeamap.className() + "duke",
              ".csv");
      results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
      // String2.log(results);
      expected =
          "longitude, latitude, time, ID, Genus, Species, Citation\n"
              + "degrees_east, degrees_north, UTC, , , , \n"
              + "26.5833333333333, -33.7166666666667, 1996-01-01T00:00:00Z, RUSI:36670:RUSI 50000, Carcharodon, carcharias, \n"
              + "NaN, NaN, 1997-01-01T00:00:00Z, IGFA:40637:IGFA 751-2090, Carcharodon, carcharias, \n"
              + "NaN, NaN, 1991-01-01T00:00:00Z, IGFA:40637:IGFA 751-2088, Carcharodon, carcharias, \n"
              + "NaN, NaN, 1994-01-01T00:00:00Z, IGFA:40637:IGFA 751-2089, Carcharodon, carcharias, \n"
              + "131.016666666667, -4.48333333333333, 1991-09-10T00:00:00Z, ZMUC:40919:ZMUC P 0566, Carcharodon, carcharias, \n";
      Test.ensureEqual(results, expected, "\nresults=\n" + results);

    } catch (Throwable t) {
      throw new RuntimeException("Unexpected dukeSeamap error.", t);
    }
  }

  /** I have never gotten argos or other aadc datasets to work. */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testArgos() throws Throwable {

    // testVerboseOn();
    int language = 0;
    // DigirHelper.verbose = true;
    // DigirHelper.reallyVerbose = true;
    // TableXmlHandler.verbose = true;

    String name, tName, results, tResults, expected, userDapQuery;
    String error = "";
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    EDDTable argos = (EDDTable) EDDTableFromOBIS.oneFromDatasetsXml(null, "aadcArgos");
    userDapQuery =
        "longitude,latitude,time,ID,Genus,Species&Genus=\"Aptenodytes\"&time<=2008-01-01";
    tName =
        argos.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            argos.className() + "Argos",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }
}
