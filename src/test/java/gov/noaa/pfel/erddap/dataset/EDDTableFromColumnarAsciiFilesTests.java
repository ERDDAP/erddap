package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import tags.TagIncompleteTest;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

class EDDTableFromColumnarAsciiFilesTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /**
   * testGenerateDatasetsXmlFromEML. This is not a good test of suggestTestOutOfDate, except that it
   * tests that a dataset with old data doesn't get a recommended value.
   */
  @org.junit.jupiter.api.Test
  @TagIncompleteTest
  void testGenerateDatasetsXmlFromEML() throws Throwable {
    // testVerboseOn();
    int language = 0;

    int which = 6;
    String emlDir =
        Path.of(
                    EDDTableFromColumnarAsciiFilesTests.class
                        .getResource("/largePoints/lterSbc/")
                        .toURI())
                .toString()
            + "/";
    String startUrl =
        "https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc."
            + which; // original test: .17
    EDStatic.developmentMode = true;
    String results =
        EDDTableFromColumnarAsciiFiles.generateDatasetsXmlFromEML(
                false, // pauseForErrors,
                emlDir,
                startUrl,
                true, // useLocalFilesIfPresent,
                "lterSbc",
                "US/Pacific",
                -1)
            + "\n"; // accessibleTo, local time_zone, standardizeWhat,

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromEML",
                  emlDir,
                  startUrl,
                  "true", // Use local files if present (true|false)"
                  "lterSbc",
                  "US/Pacific",
                  "-1"
                }, // accessibleTo, local time_zone, defaultStandardizeWhat
                false); // doIt loop?
    EDStatic.developmentMode = false;

    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String expected =
        "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"knb_lter_sbc_6_t1\" active=\"true\">\n"
            + "    <accessibleTo>lterSbc</accessibleTo>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
            + "    <defaultDataQuery>&amp;time&gt;=min(time)&amp;time&lt;=max(time)</defaultDataQuery>\n"
            + "    <defaultGraphQuery>&amp;time&gt;=min(time)&amp;time&lt;=max(time)&amp;.marker=1|5</defaultGraphQuery>\n"
            + "    <fileDir>"
            + Path.of(
                    EDDTableFromColumnarAsciiFilesTests.class
                        .getResource("/largePoints/lterSbc/")
                        .toURI())
                .toString()
            + "/"
            + "</fileDir>\n"
            + "    <fileNameRegex>sbclter_stream_chemistry_allyears_registered_stations_20150926\\.csv</fileNameRegex>\n"
            + "    <recursive>false</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <charset>ISO-8859-1</charset>\n"
            + "    <columnNamesRow>1</columnNamesRow>\n"
            + "    <firstDataRow>2</firstDataRow>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <sortFilesBySourceNames></sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"acknowledgement\">Funding: NSF Awards OCE-9982105, OCE-0620276, OCE-1232779</att>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_address\">Department of Ecology, Evolution and Marine Biology, University of California, Santa Barbara, CA, 93106-9620, US</att>\n"
            + "        <att name=\"creator_email\">john.melack@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"creator_name\">John M Melack</att>\n"
            + "        <att name=\"creator_type\">person</att>\n"
            + "        <att name=\"creator_url\">https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc.6</att>\n"
            + "        <att name=\"date_created\">2015-09-26</att>\n"
            + "        <att name=\"doi\">10.6073/pasta/6d015c56b343adf6b8003e9d82a35f70</att>\n"
            + "        <att name=\"doi_authority\">http://doi.org</att>\n"
            + "        <att name=\"geographicCoverage\">AB00: Arroyo Burro, Arroyo Burro at Cliff Drive: Arroyo Burro, Arroyo Burro at Cliff Drive. BoundingCoordinates(westLongitude=-119.74021, eastLongitude=-119.74021, northLatitude=34.40505027, southLatitude=34.40505027)\n"
            + "\n"
            + "AT07: Atascadero Creek at Puente Street: Atascadero Creek, Atascadero at Puente. BoundingCoordinates(westLongitude=-119.78414, eastLongitude=-119.78414, northLatitude=34.43226, southLatitude=34.43226)\n"
            + "\n"
            + "BC02: Bell Canyon Creek at Winchester Canyon Road culvert: BC02, Bell Canyon Creek, Bell Canyon Creek at Winchester Canyon Road culvert. BoundingCoordinates(westLongitude=-119.90563, eastLongitude=-119.90563, northLatitude=34.43854, southLatitude=34.43854)\n"
            + "\n"
            + "DV01: Devereaux Creek at Devereaux Slough inflow: DV01, Devereaux Creek, Devereaux Creek at Devereaux Slough inflow. BoundingCoordinates(westLongitude=-119.87406, eastLongitude=-119.87406, northLatitude=34.41761, southLatitude=34.41761)\n"
            + "\n"
            + "GV01: GV01, Gaviota Creek, Gaviota at Hwy 101 South Rest Stop Exit: GV01, Gaviota Creek, Gaviota at Hwy 101 South Rest Stop Exit. BoundingCoordinates(westLongitude=-120.22917, eastLongitude=-120.22917, northLatitude=34.4855, southLatitude=34.4855)\n"
            + "\n"
            + "HO00: HO00, Arroyo Hondo Creek, Arroyo Hondo at Upstream Side of 101 Bridge: HO00, Arroyo Hondo Creek, Arroyo Hondo at Upstream Side of 101 Bridge. BoundingCoordinates(westLongitude=-120.14122, eastLongitude=-120.14122, northLatitude=34.4752858, southLatitude=34.4752858)\n"
            + "\n"
            + "MC00: MC00, Mission Creek, Mission at Montecito St: MC00, Mission Creek, Mission at Montecito St. BoundingCoordinates(westLongitude=-119.69499, eastLongitude=-119.69499, northLatitude=34.41307303, southLatitude=34.41307303)\n"
            + "\n"
            + "MC06: Mission Creek at Rocky Nook, USGS 11119745: Mission Creek at Rocky Nook, USGS 11119745. BoundingCoordinates(westLongitude=-119.71244, eastLongitude=-119.71244, northLatitude=34.44072, southLatitude=34.44072)\n"
            + "\n"
            + "ON02: ON02, San Onofre Creek, San Onofre Creek at Highway 101 North culvert: ON02, San Onofre Creek, San Onofre Creek at Highway 101 North culvert. BoundingCoordinates(westLongitude=-120.28885, eastLongitude=-120.28885, northLatitude=34.472, southLatitude=34.472)\n"
            + "\n"
            + "RG01: RG01, Refugio Creek, Refugio at Hwy 101 Bridge: RG01, Refugio Creek, Refugio at Hwy 101 Bridge. BoundingCoordinates(westLongitude=-120.06932, eastLongitude=-120.06932, northLatitude=34.46573164, southLatitude=34.46573164)\n"
            + "\n"
            + "RS02: RS02, Rattlesnake Creek, Rattlesnake at Las Canoas Bridge: RS02, Rattlesnake Creek, Rattlesnake at Las Canoas Bridge. BoundingCoordinates(westLongitude=-119.69222, eastLongitude=-119.69222, northLatitude=34.45761111, southLatitude=34.45761111)\n"
            + "\n"
            + "SP02: San Pedro Creek at Stow Canyon Park, , USGS 11120520: San Pedro Creek at Stow Canyon Park, , USGS 11120520. BoundingCoordinates(westLongitude=-119.84028, eastLongitude=-119.84028, northLatitude=34.44861, southLatitude=34.44861)\n"
            + "\n"
            + "TO02: TO02, Tecolote Creek at Vereda Galeria, Goleta: TO02, Tecolote Creek at Vereda Galeria, Goleta. BoundingCoordinates(westLongitude=-119.917915, eastLongitude=-119.917915, northLatitude=34.440614, southLatitude=34.440614)</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">34.4855</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">34.40505027</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">-119.69222</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">-120.28885</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"id\">knb_lter_sbc_6_t1</att>\n"
            + "        <att name=\"infoUrl\">https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc.6</att>\n"
            + "        <att name=\"institution\">Santa Barbara Coastal LTER</att>\n"
            + "        <att name=\"keywords\">all, ammonia, ammonium, area, barbara, carbon, chemistry, coastal, code, concentration, cond, data, dissolved, dissolved nutrients, drainage, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Ammonia, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Nitrate, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Phosphate, land, lter, micromolesperliter, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_phosphate_in_sea_water, n02, nh4, NH4_uM, nitrate, nitrogen, no3, NO3_uM, nutrients, ocean, oceans, ongoing, particulate, phosphate, phosphorus, po4, PO4_uM, registered, santa, sbc, science, sea, seawater, since, site_code, solids, spec, Spec_Cond_uS_per_cm, stations, stream, suspended, TDN_uM, TDP_uM, time, total, TPC_uM, TPN_uM, tpp, TPP_uM, TSS_mg_per_L, us/cm, water, waypoint, years</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"language\">english</att>\n"
            + "        <att name=\"license\">Metadata &quot;all&quot; access is allowed for principal=&quot;uid=SBC,o=LTER,dc=ecoinformatics,dc=org&quot;.\n"
            + "Metadata &quot;read&quot; access is allowed for principal=&quot;public&quot;.\n"
            + "Data &quot;all&quot; access is allowed for principal=&quot;uid=SBC,o=LTER,dc=ecoinformatics,dc=org&quot;.\n"
            + "Data &quot;read&quot; access is allowed for principal=&quot;public&quot;.\n"
            + "\n"
            + "Intellectual Rights:\n"
            + "* The user of SBC LTER data agrees to contact the data owner (i.e., the SBC investigator responsible for data) prior to publishing. Where appropriate, users whose projects are integrally dependent on SBC LTER\n"
            + "data are encouraged to consider collaboration and/or co-authorship with the data owner.\n"
            + "\n"
            + "* The user agrees to cite SBC LTER in all publications that use SBC LTER data by including the following statement in the Acknowledgments: &quot;Data were provided by the Santa Barbara Coastal LTER, funded by the US National Science Foundation (OCE-1232779)&quot;.\n"
            + "\n"
            + "* The user agrees to send the full citation of any publication using SBC LTER data to sbclter@msi.ucsb.edu\n"
            + "\n"
            + "* Users are prohibited from selling or redistributing any data provided by SBC LTER.\n"
            + "\n"
            + "* Extensive efforts are made to ensure that online data are accurate and up to date, but SBC LTER will not take responsibility for any errors that may exist.\n"
            + "\n"
            + "* The user agrees also to adhere to the Data Use Agreement of the Long Term Ecological Research Network.\n"
            + "\n"
            + "* Any violation of the terms of this agreement will result in immediate forfeiture of the data and loss of access privileges to other SBC LTER data sets.\n"
            + "\n"
            + "* SBC LTER is committed to protecting the privacy and accuracy of your confidential information. See our Privacy Policy for more information.</att>\n"
            + "        <att name=\"metadata_link\">https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc.6</att>\n"
            + "        <att name=\"methods\">*** Method #1:\n"
            + "* Title: Stream Chemistry - Sample Collection Protocol\n"
            + "* Description: Stream Chemistry Sample Collection\n"
            + "\n"
            + "Samples for stream chemistry are collected according to the following\n"
            + "protocol\n"
            + "* Creator: Melack\n"
            + "* URL: https://sbclter.msi.ucsb.edu/external/Land/Protocols/Stream_Chemistry/Melack_20090526_SBCLTER_Stream_Chemistry_Sample_Collection.pdf\n"
            + "\n"
            + "*** Method #2:\n"
            + "* Title: Stream Chemistry Sample - Laboratory Analysis Protocol\n"
            + "* Description: Laboratory Processing\n"
            + "\n"
            + "Samples for stream chemistry are processed according to the following\n"
            + "protocol\n"
            + "* Creator: Melack\n"
            + "* Creator: Schimel\n"
            + "* URL: https://sbclter.msi.ucsb.edu/external/Land/Protocols/Stream_Chemistry/Melack_Schimel_20090529_SBCLTER_Laboratory_Analyses.pdf</att>\n"
            + "        <att name=\"program\">LTER</att>\n"
            + "        <att name=\"project\">Santa Barbara Coastal Long Term Ecological Research Project</att>\n"
            + "        <att name=\"project_abstract\">The primary research objective of the Santa Barbara Coastal LTER is to investigate\n"
            + "the importance of land and ocean processes in structuring giant kelp\n"
            + "([emphasis]Macrocystis pyrifera[/emphasis]) forest ecosystems. As in many temperate\n"
            + "regions, the shallow rocky reefs in the Santa Barbara Channel, California, are dominated\n"
            + "by giant kelp forests. Because of their close proximity to shore, kelp forests are\n"
            + "influenced by physical and biological processes occurring on land as well as in the open\n"
            + "ocean. SBC LTER research focuses on measuring and modeling the patterns, transport, and\n"
            + "processing of material constituents (e.g., nutrients, carbon, sediment, organisms, and\n"
            + "pollutants) from terrestrial watersheds and the coastal ocean to these reefs.\n"
            + "Specifically, we are examining the effects of these material inputs on the primary\n"
            + "production of kelp, and the population dynamics, community structure, and trophic\n"
            + "interactions of kelp forest ecosystems.</att>\n"
            + "        <att name=\"project_funding\">NSF Awards OCE-9982105, OCE-0620276, OCE-1232779</att>\n"
            + "        <att name=\"project_personnel_1_address\">Marine Science Institute, University of California, Santa Barbara, California, 93106-6150, United States</att>\n"
            + "        <att name=\"project_personnel_1_email\">reed@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_1_name\">Dr. Daniel Reed</att>\n"
            + "        <att name=\"project_personnel_1_role\">Principal Investigator</att>\n"
            + "        <att name=\"project_personnel_2_address\">Bren School of Environmental Science and Management, University of California, Santa Barbara, California, 93106-5131, United States</att>\n"
            + "        <att name=\"project_personnel_2_email\">melack@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_2_name\">Dr. John Melack</att>\n"
            + "        <att name=\"project_personnel_2_role\">Co-principal Investigator</att>\n"
            + "        <att name=\"project_personnel_3_address\">Ecology, Evolution and Marine Biology, University of California, Santa Barbara, California, 93106-9620, United States</att>\n"
            + "        <att name=\"project_personnel_3_email\">holbrook@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_3_name\">Dr. Sally Holbrook</att>\n"
            + "        <att name=\"project_personnel_3_role\">Co-principal Investigator</att>\n"
            + "        <att name=\"project_personnel_4_address\">Institute for Computational Earth System Science, University of California, Santa Barbara, California, 93106-3060, United States</att>\n"
            + "        <att name=\"project_personnel_4_email\">davey@icess.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_4_name\">Dr. David Siegel</att>\n"
            + "        <att name=\"project_personnel_4_role\">Co-principal Investigator</att>\n"
            + "        <att name=\"publisher_address\">Marine Science Institute, University of California, Santa Barbara, CA, 93106, USA</att>\n"
            + "        <att name=\"publisher_email\">sbclter@msi.ucsb.edu</att>\n"
            + "        <att name=\"publisher_name\">Santa Barbara Coastal LTER</att>\n"
            + "        <att name=\"publisher_type\">institution</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">site_code, NH4_uM, PO4_uM, TDP_uM</att>\n"
            + "        <att name=\"summary\">SBC LTER: Land: Stream chemistry in the Santa Barbara Coastal drainage area, ongoing since 2000. Stream chemistry, registered stations, all years. stream water chemistry at REGISTERED SBC stations. Registered stations are geo-located in metadata.</att>\n"
            + "        <att name=\"time_coverage_end\">2014-09-17</att>\n"
            + "        <att name=\"time_coverage_start\">2000-10-01</att>\n"
            + "        <att name=\"title\">SBC LTER: Land: Stream chemistry in the Santa Barbara Coastal drainage area, ongoing since 2000. Stream chemistry, registered stations, all years</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>site_code</sourceName>\n"
            + "        <destinationName>site_code</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"columnNameInSourceFile\">site_code</att>\n"
            + "            <att name=\"comment\">2 letter site ID + 2-numbers reflecting relative distance upstream\n"
            + "AB00 = Arroyo Burro, Arroyo Burro at Cliff Drive\n"
            + "AT07 = Atascadero Creek, Atascadero at Puente\n"
            + "BC02 = Bell Canyon Creek, Bell Canyon Creek at Winchester Canyon Road culvert\n"
            + "DV01 = Devereaux Creek, Devereaux Creek at Devereaux Slough inflow\n"
            + "GV01 = Gaviota, Gaviota at Hwy 101 South Rest Stop Exit\n"
            + "HO00 = Arroyo Hondo, Arroyo Hondo at Upstream Side of 101 Bridge\n"
            + "MC00 = Mission Creek, Mission at Montecito St\n"
            + "MC06 = Mission Creek, Mission at Rocky Nook. Site established by the USGS\n"
            + "ON02 = San Onofre Creek, San Onofre Creek at Highway 101 North culvert\n"
            + "RG01 = Refugio Creek, Refugio at Hwy 101 Bridge\n"
            + "RS02 = Rattlesnake Creek, Rattlesnake at Las Canoas Bridge\n"
            + "SP02 = San Pedro Creek at Stow Canyon Park, Goleta. Site established by the USGS\n"
            + "TO02 = Tecolote Creek, at Vereda Galeria, Goleta</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">SBC Waypoint Code</att>\n"
            + "            <att name=\"missing_value\">NO SAMPLE</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>timestamp_local</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"columnNameInSourceFile\">timestamp_local</att>\n"
            + "            <att name=\"comment\">In the source file: Date sample was collected in Pacific Standard Time. offset to UTC is -08:00</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"missing_value\">9997-04-06T00:00:00-08:00</att>\n"
            + "            <att name=\"source_name\">timestamp_local</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n"
            + "            <att name=\"time_zone\">US/Pacific</att>\n"
            + "            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>nh4_uM</sourceName>\n"
            + "        <destinationName>NH4_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">600.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">nh4_uM</att>\n"
            + "            <att name=\"comment\">Ammonium (measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n"
            + "            <att name=\"long_name\">NH4 uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"standard_name\">mole_concentration_of_ammonium_in_sea_water</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>no3_uM</sourceName>\n"
            + "        <destinationName>NO3_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">5000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">no3_uM</att>\n"
            + "            <att name=\"comment\">Nitrate (measured as nitrite + nitrate measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n"
            + "            <att name=\"long_name\">NO3 uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"standard_name\">mole_concentration_of_nitrate_in_sea_water</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>po4_uM</sourceName>\n"
            + "        <destinationName>PO4_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">po4_uM</att>\n"
            + "            <att name=\"comment\">Phosphorus (measured as soluble reactive phosphorus SRP measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n"
            + "            <att name=\"long_name\">PO4 uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"standard_name\">mole_concentration_of_phosphate_in_sea_water</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tdn_uM</sourceName>\n"
            + "        <destinationName>TDN_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">3000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tdn_uM</att>\n"
            + "            <att name=\"comment\">Total dissolved nitrogen (dissolved organic nitrogen plus nitrate and nitrate plus ammonium measured in micro-moles per leter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Dissolved Nitrogen uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tdp_uM</sourceName>\n"
            + "        <destinationName>TDP_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">150.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tdp_uM</att>\n"
            + "            <att name=\"comment\">Total dissolved phosphorus (dissolved organic phosphorus plus phosphate measured in micro-moles per leter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Dissolved Phosphorus uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tpc_uM</sourceName>\n"
            + "        <destinationName>TPC_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1500000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tpc_uM</att>\n"
            + "            <att name=\"comment\">Total particulate carbon (particulate organic carbon measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Particulate Carbon micromolesPerLiter</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tpn_uM</sourceName>\n"
            + "        <destinationName>TPN_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tpn_uM</att>\n"
            + "            <att name=\"comment\">Total particulate nitrogen (which can be assumed to be particulate organic nitrogen measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Particulate Nitrogen micromolesPerLiter</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tpp_uM</sourceName>\n"
            + "        <destinationName>TPP_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">6000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tpp_uM</att>\n"
            + "            <att name=\"comment\">Total particulate phosphorus (measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">TPP micromolesPerLiter</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tss_mgperLiter</sourceName>\n"
            + "        <destinationName>TSS_mg_per_L</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">600000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tss_mgperLiter</att>\n"
            + "            <att name=\"comment\">Total suspended solids measured in milligrams per liter (mg/L)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Suspended Solids</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">milligram per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>spec_cond_uSpercm</sourceName>\n"
            + "        <destinationName>Spec_Cond_uS_per_cm</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">50000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-10000.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">spec_cond_uSpercm</att>\n"
            + "            <att name=\"comment\">Specific conductivity (measured at 25 deg C in micro-Siemens per cm, uS/cm, (equivalent in magnitude to the older unit, umhos/cm)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Spec_Cond_uS/cm</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">siemens per centimeter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n"
            + "\n"
            + "<dataset type=\"EDDTableFromAsciiFiles\" datasetID=\"knb_lter_sbc_6_t2\" active=\"true\">\n"
            + "    <accessibleTo>lterSbc</accessibleTo>\n"
            + "    <reloadEveryNMinutes>10080</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>-1</updateEveryNMillis>\n"
            + "    <defaultDataQuery>&amp;time&gt;=min(time)&amp;time&lt;=max(time)</defaultDataQuery>\n"
            + "    <defaultGraphQuery>&amp;time&gt;=min(time)&amp;time&lt;=max(time)&amp;.marker=1|5</defaultGraphQuery>\n"
            + "    <fileDir>"
            + Path.of(
                    EDDTableFromColumnarAsciiFilesTests.class
                        .getResource("/largePoints/lterSbc/")
                        .toURI())
                .toString()
            + "/"
            + "</fileDir>\n"
            + "    <fileNameRegex>sbclter_stream_chemistry_allyears_non_registered_stations_20120229\\.csv</fileNameRegex>\n"
            + "    <recursive>false</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <charset>ISO-8859-1</charset>\n"
            + "    <columnNamesRow>1</columnNamesRow>\n"
            + "    <firstDataRow>2</firstDataRow>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <sortFilesBySourceNames></sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <accessibleViaFiles>true</accessibleViaFiles>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"acknowledgement\">Funding: NSF Awards OCE-9982105, OCE-0620276, OCE-1232779</att>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_address\">Department of Ecology, Evolution and Marine Biology, University of California, Santa Barbara, CA, 93106-9620, US</att>\n"
            + "        <att name=\"creator_email\">john.melack@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"creator_name\">John M Melack</att>\n"
            + "        <att name=\"creator_type\">person</att>\n"
            + "        <att name=\"creator_url\">https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc.6</att>\n"
            + "        <att name=\"date_created\">2015-09-26</att>\n"
            + "        <att name=\"doi\">10.6073/pasta/6d015c56b343adf6b8003e9d82a35f70</att>\n"
            + "        <att name=\"doi_authority\">http://doi.org</att>\n"
            + "        <att name=\"geographicCoverage\">AB00: Arroyo Burro, Arroyo Burro at Cliff Drive: Arroyo Burro, Arroyo Burro at Cliff Drive. BoundingCoordinates(westLongitude=-119.74021, eastLongitude=-119.74021, northLatitude=34.40505027, southLatitude=34.40505027)\n"
            + "\n"
            + "AT07: Atascadero Creek at Puente Street: Atascadero Creek, Atascadero at Puente. BoundingCoordinates(westLongitude=-119.78414, eastLongitude=-119.78414, northLatitude=34.43226, southLatitude=34.43226)\n"
            + "\n"
            + "BC02: Bell Canyon Creek at Winchester Canyon Road culvert: BC02, Bell Canyon Creek, Bell Canyon Creek at Winchester Canyon Road culvert. BoundingCoordinates(westLongitude=-119.90563, eastLongitude=-119.90563, northLatitude=34.43854, southLatitude=34.43854)\n"
            + "\n"
            + "DV01: Devereaux Creek at Devereaux Slough inflow: DV01, Devereaux Creek, Devereaux Creek at Devereaux Slough inflow. BoundingCoordinates(westLongitude=-119.87406, eastLongitude=-119.87406, northLatitude=34.41761, southLatitude=34.41761)\n"
            + "\n"
            + "GV01: GV01, Gaviota Creek, Gaviota at Hwy 101 South Rest Stop Exit: GV01, Gaviota Creek, Gaviota at Hwy 101 South Rest Stop Exit. BoundingCoordinates(westLongitude=-120.22917, eastLongitude=-120.22917, northLatitude=34.4855, southLatitude=34.4855)\n"
            + "\n"
            + "HO00: HO00, Arroyo Hondo Creek, Arroyo Hondo at Upstream Side of 101 Bridge: HO00, Arroyo Hondo Creek, Arroyo Hondo at Upstream Side of 101 Bridge. BoundingCoordinates(westLongitude=-120.14122, eastLongitude=-120.14122, northLatitude=34.4752858, southLatitude=34.4752858)\n"
            + "\n"
            + "MC00: MC00, Mission Creek, Mission at Montecito St: MC00, Mission Creek, Mission at Montecito St. BoundingCoordinates(westLongitude=-119.69499, eastLongitude=-119.69499, northLatitude=34.41307303, southLatitude=34.41307303)\n"
            + "\n"
            + "MC06: Mission Creek at Rocky Nook, USGS 11119745: Mission Creek at Rocky Nook, USGS 11119745. BoundingCoordinates(westLongitude=-119.71244, eastLongitude=-119.71244, northLatitude=34.44072, southLatitude=34.44072)\n"
            + "\n"
            + "ON02: ON02, San Onofre Creek, San Onofre Creek at Highway 101 North culvert: ON02, San Onofre Creek, San Onofre Creek at Highway 101 North culvert. BoundingCoordinates(westLongitude=-120.28885, eastLongitude=-120.28885, northLatitude=34.472, southLatitude=34.472)\n"
            + "\n"
            + "RG01: RG01, Refugio Creek, Refugio at Hwy 101 Bridge: RG01, Refugio Creek, Refugio at Hwy 101 Bridge. BoundingCoordinates(westLongitude=-120.06932, eastLongitude=-120.06932, northLatitude=34.46573164, southLatitude=34.46573164)\n"
            + "\n"
            + "RS02: RS02, Rattlesnake Creek, Rattlesnake at Las Canoas Bridge: RS02, Rattlesnake Creek, Rattlesnake at Las Canoas Bridge. BoundingCoordinates(westLongitude=-119.69222, eastLongitude=-119.69222, northLatitude=34.45761111, southLatitude=34.45761111)\n"
            + "\n"
            + "SP02: San Pedro Creek at Stow Canyon Park, , USGS 11120520: San Pedro Creek at Stow Canyon Park, , USGS 11120520. BoundingCoordinates(westLongitude=-119.84028, eastLongitude=-119.84028, northLatitude=34.44861, southLatitude=34.44861)\n"
            + "\n"
            + "TO02: TO02, Tecolote Creek at Vereda Galeria, Goleta: TO02, Tecolote Creek at Vereda Galeria, Goleta. BoundingCoordinates(westLongitude=-119.917915, eastLongitude=-119.917915, northLatitude=34.440614, southLatitude=34.440614)</att>\n"
            + "        <att name=\"geospatial_lat_max\" type=\"double\">34.4855</att>\n"
            + "        <att name=\"geospatial_lat_min\" type=\"double\">34.40505027</att>\n"
            + "        <att name=\"geospatial_lat_units\">degrees_north</att>\n"
            + "        <att name=\"geospatial_lon_max\" type=\"double\">-119.69222</att>\n"
            + "        <att name=\"geospatial_lon_min\" type=\"double\">-120.28885</att>\n"
            + "        <att name=\"geospatial_lon_units\">degrees_east</att>\n"
            + "        <att name=\"id\">knb_lter_sbc_6_t2</att>\n"
            + "        <att name=\"infoUrl\">https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc.6</att>\n"
            + "        <att name=\"institution\">Santa Barbara Coastal LTER</att>\n"
            + "        <att name=\"keywords\">all, ammonia, ammonium, area, barbara, carbon, chemistry, coastal, code, concentration, cond, data, dissolved, dissolved nutrients, drainage, earth, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Ammonia, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Nitrate, Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Phosphate, land, lter, micromolesperliter, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_phosphate_in_sea_water, n02, nh4, NH4_uM, nitrate, nitrogen, no3, NO3_uM, non, non-registered, nutrients, ocean, oceans, ongoing, particulate, phosphate, phosphorus, po4, PO4_uM, registered, santa, sbc, science, sea, seawater, since, site_code, solids, spec, Spec_Cond_uS_per_cm, stations, stream, suspended, TDN_uM, TDP_uM, time, total, TPC_uM, TPN_uM, tpp, TPP_uM, TSS_mg_per_L, us/cm, water, waypoint, years</att>\n"
            + "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n"
            + "        <att name=\"language\">english</att>\n"
            + "        <att name=\"license\">Metadata &quot;all&quot; access is allowed for principal=&quot;uid=SBC,o=LTER,dc=ecoinformatics,dc=org&quot;.\n"
            + "Metadata &quot;read&quot; access is allowed for principal=&quot;public&quot;.\n"
            + "Data &quot;all&quot; access is allowed for principal=&quot;uid=SBC,o=LTER,dc=ecoinformatics,dc=org&quot;.\n"
            + "Data &quot;read&quot; access is allowed for principal=&quot;public&quot;.\n"
            + "\n"
            + "Intellectual Rights:\n"
            + "* The user of SBC LTER data agrees to contact the data owner (i.e., the SBC investigator responsible for data) prior to publishing. Where appropriate, users whose projects are integrally dependent on SBC LTER\n"
            + "data are encouraged to consider collaboration and/or co-authorship with the data owner.\n"
            + "\n"
            + "* The user agrees to cite SBC LTER in all publications that use SBC LTER data by including the following statement in the Acknowledgments: &quot;Data were provided by the Santa Barbara Coastal LTER, funded by the US National Science Foundation (OCE-1232779)&quot;.\n"
            + "\n"
            + "* The user agrees to send the full citation of any publication using SBC LTER data to sbclter@msi.ucsb.edu\n"
            + "\n"
            + "* Users are prohibited from selling or redistributing any data provided by SBC LTER.\n"
            + "\n"
            + "* Extensive efforts are made to ensure that online data are accurate and up to date, but SBC LTER will not take responsibility for any errors that may exist.\n"
            + "\n"
            + "* The user agrees also to adhere to the Data Use Agreement of the Long Term Ecological Research Network.\n"
            + "\n"
            + "* Any violation of the terms of this agreement will result in immediate forfeiture of the data and loss of access privileges to other SBC LTER data sets.\n"
            + "\n"
            + "* SBC LTER is committed to protecting the privacy and accuracy of your confidential information. See our Privacy Policy for more information.</att>\n"
            + "        <att name=\"metadata_link\">https://sbclter.msi.ucsb.edu/external/InformationManagement/eml_2018_erddap/knb-lter-sbc.6</att>\n"
            + "        <att name=\"methods\">*** Method #1:\n"
            + "* Title: Stream Chemistry - Sample Collection Protocol\n"
            + "* Description: Stream Chemistry Sample Collection\n"
            + "\n"
            + "Samples for stream chemistry are collected according to the following\n"
            + "protocol\n"
            + "* Creator: Melack\n"
            + "* URL: https://sbclter.msi.ucsb.edu/external/Land/Protocols/Stream_Chemistry/Melack_20090526_SBCLTER_Stream_Chemistry_Sample_Collection.pdf\n"
            + "\n"
            + "*** Method #2:\n"
            + "* Title: Stream Chemistry Sample - Laboratory Analysis Protocol\n"
            + "* Description: Laboratory Processing\n"
            + "\n"
            + "Samples for stream chemistry are processed according to the following\n"
            + "protocol\n"
            + "* Creator: Melack\n"
            + "* Creator: Schimel\n"
            + "* URL: https://sbclter.msi.ucsb.edu/external/Land/Protocols/Stream_Chemistry/Melack_Schimel_20090529_SBCLTER_Laboratory_Analyses.pdf</att>\n"
            + "        <att name=\"program\">LTER</att>\n"
            + "        <att name=\"project\">Santa Barbara Coastal Long Term Ecological Research Project</att>\n"
            + "        <att name=\"project_abstract\">The primary research objective of the Santa Barbara Coastal LTER is to investigate\n"
            + "the importance of land and ocean processes in structuring giant kelp\n"
            + "([emphasis]Macrocystis pyrifera[/emphasis]) forest ecosystems. As in many temperate\n"
            + "regions, the shallow rocky reefs in the Santa Barbara Channel, California, are dominated\n"
            + "by giant kelp forests. Because of their close proximity to shore, kelp forests are\n"
            + "influenced by physical and biological processes occurring on land as well as in the open\n"
            + "ocean. SBC LTER research focuses on measuring and modeling the patterns, transport, and\n"
            + "processing of material constituents (e.g., nutrients, carbon, sediment, organisms, and\n"
            + "pollutants) from terrestrial watersheds and the coastal ocean to these reefs.\n"
            + "Specifically, we are examining the effects of these material inputs on the primary\n"
            + "production of kelp, and the population dynamics, community structure, and trophic\n"
            + "interactions of kelp forest ecosystems.</att>\n"
            + "        <att name=\"project_funding\">NSF Awards OCE-9982105, OCE-0620276, OCE-1232779</att>\n"
            + "        <att name=\"project_personnel_1_address\">Marine Science Institute, University of California, Santa Barbara, California, 93106-6150, United States</att>\n"
            + "        <att name=\"project_personnel_1_email\">reed@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_1_name\">Dr. Daniel Reed</att>\n"
            + "        <att name=\"project_personnel_1_role\">Principal Investigator</att>\n"
            + "        <att name=\"project_personnel_2_address\">Bren School of Environmental Science and Management, University of California, Santa Barbara, California, 93106-5131, United States</att>\n"
            + "        <att name=\"project_personnel_2_email\">melack@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_2_name\">Dr. John Melack</att>\n"
            + "        <att name=\"project_personnel_2_role\">Co-principal Investigator</att>\n"
            + "        <att name=\"project_personnel_3_address\">Ecology, Evolution and Marine Biology, University of California, Santa Barbara, California, 93106-9620, United States</att>\n"
            + "        <att name=\"project_personnel_3_email\">holbrook@lifesci.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_3_name\">Dr. Sally Holbrook</att>\n"
            + "        <att name=\"project_personnel_3_role\">Co-principal Investigator</att>\n"
            + "        <att name=\"project_personnel_4_address\">Institute for Computational Earth System Science, University of California, Santa Barbara, California, 93106-3060, United States</att>\n"
            + "        <att name=\"project_personnel_4_email\">davey@icess.ucsb.edu</att>\n"
            + "        <att name=\"project_personnel_4_name\">Dr. David Siegel</att>\n"
            + "        <att name=\"project_personnel_4_role\">Co-principal Investigator</att>\n"
            + "        <att name=\"publisher_address\">Marine Science Institute, University of California, Santa Barbara, CA, 93106, USA</att>\n"
            + "        <att name=\"publisher_email\">sbclter@msi.ucsb.edu</att>\n"
            + "        <att name=\"publisher_name\">Santa Barbara Coastal LTER</att>\n"
            + "        <att name=\"publisher_type\">institution</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"subsetVariables\">site_code, NH4_uM, PO4_uM, TDP_uM, TPP_uM</att>\n"
            + "        <att name=\"summary\">SBC LTER: Land: Stream chemistry in the Santa Barbara Coastal drainage area, ongoing since 2000. Stream chemistry, non-registered stations, all years. stream water chemistry at NON_REGISTERED stations</att>\n"
            + "        <att name=\"time_coverage_end\">2014-09-17</att>\n"
            + "        <att name=\"time_coverage_start\">2000-10-01</att>\n"
            + "        <att name=\"title\">SBC LTER: Land: Stream chemistry in the Santa Barbara Coastal drainage area, ongoing since 2000. Stream chemistry, non-registered stations, all years</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>site_code</sourceName>\n"
            + "        <destinationName>site_code</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"columnNameInSourceFile\">site_code</att>\n"
            + "            <att name=\"comment\">2 letter site ID + 2-numbers reflecting relative distance upstream</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">SBC Waypoint Code</att>\n"
            + "            <att name=\"missing_value\">NO SAMPLE</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>timestamp_local</sourceName>\n"
            + "        <destinationName>time</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"columnNameInSourceFile\">timestamp_local</att>\n"
            + "            <att name=\"comment\">In the source file: Date sample was collected in Pacific Standard Time. ISO format, with offset to UTC included</att>\n"
            + "            <att name=\"ioos_category\">Time</att>\n"
            + "            <att name=\"long_name\">Time</att>\n"
            + "            <att name=\"missing_value\">9997-04-06T00:00:00</att>\n"
            + "            <att name=\"source_name\">timestamp_local</att>\n"
            + "            <att name=\"standard_name\">time</att>\n"
            + "            <att name=\"time_precision\">1970-01-01T00:00:00Z</att>\n"
            + "            <att name=\"time_zone\">US/Pacific</att>\n"
            + "            <att name=\"units\">yyyy-MM-dd&#39;T&#39;HH:mm:ss</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>nh4_uM</sourceName>\n"
            + "        <destinationName>NH4_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">3000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">nh4_uM</att>\n"
            + "            <att name=\"comment\">Ammonium (measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n"
            + "            <att name=\"long_name\">NH4 uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"standard_name\">mole_concentration_of_ammonium_in_sea_water</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>no3_uM</sourceName>\n"
            + "        <destinationName>NO3_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">no3_uM</att>\n"
            + "            <att name=\"comment\">Nitrate (measured as nitrite + nitrate measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n"
            + "            <att name=\"long_name\">NO3 uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"standard_name\">mole_concentration_of_nitrate_in_sea_water</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>po4_uM</sourceName>\n"
            + "        <destinationName>PO4_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1500.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">po4_uM</att>\n"
            + "            <att name=\"comment\">Phosphorus (measured as soluble reactive phosphorus SRP measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Dissolved Nutrients</att>\n"
            + "            <att name=\"long_name\">PO4 uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"standard_name\">mole_concentration_of_phosphate_in_sea_water</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tdn_uM</sourceName>\n"
            + "        <destinationName>TDN_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">15000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tdn_uM</att>\n"
            + "            <att name=\"comment\">Total dissolved nitrogen (dissolved organic nitrogen plus nitrate and nitrate plus ammonium measured in micro-moles per leter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Dissolved Nitrogen uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tdp_uM</sourceName>\n"
            + "        <destinationName>TDP_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">1500.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tdp_uM</att>\n"
            + "            <att name=\"comment\">Total dissolved phosphorus (dissolved organic phosphorus plus phosphate measured in micro-moles per leter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Dissolved Phosphorus uM</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tpc_uM</sourceName>\n"
            + "        <destinationName>TPC_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">800000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tpc_uM</att>\n"
            + "            <att name=\"comment\">Total particulate carbon (particulate organic carbon measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Particulate Carbon micromolesPerLiter</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tpn_uM</sourceName>\n"
            + "        <destinationName>TPN_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">40000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tpn_uM</att>\n"
            + "            <att name=\"comment\">Total particulate nitrogen (which can be assumed to be particulate organic nitrogen measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Particulate Nitrogen micromolesPerLiter</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tpp_uM</sourceName>\n"
            + "        <destinationName>TPP_uM</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">8000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tpp_uM</att>\n"
            + "            <att name=\"comment\">Total particulate phosphorus (measured in micro-moles per liter)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">TPP micromolesPerLiter</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">micromole per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>tss_mgperLiter</sourceName>\n"
            + "        <destinationName>TSS_mg_per_L</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">300000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">tss_mgperLiter</att>\n"
            + "            <att name=\"comment\">Total suspended solids measured in milligrams per liter (mg/L)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Total Suspended Solids</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">milligram per liter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>spec_cond_uSpercm</sourceName>\n"
            + "        <destinationName>Spec_Cond_uS_per_cm</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"colorBarMaximum\" type=\"double\">100000.0</att>\n"
            + "            <att name=\"colorBarMinimum\" type=\"double\">-20000.0</att>\n"
            + "            <att name=\"columnNameInSourceFile\">spec_cond_uSpercm</att>\n"
            + "            <att name=\"comment\">Specific conductivity (measured at 25 deg C in micro-Siemens per cm, uS/cm, (equivalent in magnitude to the older unit, umhos/cm)</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">Spec_Cond_uS/cm</att>\n"
            + "            <att name=\"missing_value\" type=\"float\">-999.0</att>\n"
            + "            <att name=\"units\">siemens per centimeter</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n"
            + "\n\n";

    Test.ensureEqual(results, expected, "results=\n" + results);

    // ensure it is ready-to-use by making a dataset from it
    String tDatasetID = "knb_lter_sbc_6_t1";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDTableFromColumnarAsciiFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");

    String userDapQuery = "";
    String tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_eml_1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "site_code,time,NH4_uM,NO3_uM,PO4_uM,TDN_uM,TDP_uM,TPC_uM,TPN_uM,TPP_uM,TSS_mg_per_L,Spec_Cond_uS_per_cm\n"
            + ",UTC,micromole per liter,micromole per liter,micromole per liter,micromole per liter,micromole per liter,micromole per liter,micromole per liter,micromole per liter,milligram per liter,siemens per centimeter\n"
            + "RG01,2000-10-23T07:00:00Z,0.6,112.0,NaN,137.7,0.7,NaN,NaN,NaN,NaN,NaN\n"
            + "AB00,2000-10-23T07:00:00Z,0.3,92.4,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "RG01,2000-10-26T07:00:00Z,0.0,241.5,NaN,300.3,1.5,NaN,NaN,NaN,NaN,NaN\n"
            + "AB00,2000-10-26T07:00:00Z,11.9,66.2,NaN,103.6,1.7,NaN,NaN,NaN,NaN,NaN\n"
            + "AB00,2000-10-26T07:00:00Z,2.1,56.6,19.6,127.9,26.2,NaN,NaN,NaN,NaN,NaN\n"
            + "MC06,2000-11-04T08:00:00Z,0.5,0.4,1.1,14.4,2.5,NaN,NaN,NaN,NaN,NaN\n"
            + "MC00,2000-11-04T08:00:00Z,0.0,121.6,2.7,154.7,3.1,NaN,NaN,NaN,NaN,NaN\n"
            + "AB00,2000-11-12T08:00:00Z,15.0,77.1,0.7,125.8,1.6,NaN,NaN,NaN,NaN,2067.0\n"
            + "HO00,2000-11-12T08:00:00Z,0.0,0.0,0.1,22.1,0.8,NaN,NaN,NaN,NaN,NaN\n"
            + "AB00,2000-11-18T08:00:00Z,11.9,83.6,1.4,121.7,1.2,NaN,NaN,NaN,NaN,2041.0\n";
    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "\nresults=\n" + results.substring(0, expected.length()));
  }

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  void testGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();
    int language = 0;

    String testResourceDir =
        Path.of(EDDTableFromColumnarAsciiFilesTests.class.getResource("/data/").toURI()).toString()
            + "/";

    Attributes externalAddAttributes = new Attributes();
    externalAddAttributes.add("title", "New Title!");
    // public static String generateDatasetsXml(String tFileDir, String
    // tFileNameRegex,
    // String sampleFileName,
    // String charset, int columnNamesRow, int firstDataRow, int
    // tReloadEveryNMinutes,
    // String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
    // String tColumnNameForExtract, //no tSortedColumnSourceName,
    // String tSortFilesBySourceNames,
    // String tInfoUrl, String tInstitution, String tSummary, String tTitle,
    // standardizeWhat, cacheFromUrl,
    // Attributes externalAddGlobalAttributes)
    String results =
        EDDTableFromColumnarAsciiFiles.generateDatasetsXml(
                testResourceDir,
                "columnarAsciiNoComments\\.txt",
                testResourceDir + "columnarAsciiNoComments.txt",
                null,
                3,
                4,
                1440,
                "",
                "",
                "",
                "",
                "",
                "https://www.ndbc.noaa.gov/",
                "NOAA NDBC",
                "The new summary!",
                "The Newer Title!",
                -1,
                "", // defaultStandardizeWhat, cacheFromUrl
                externalAddAttributes)
            + "\n";

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromColumnarAsciiFiles",
                  testResourceDir,
                  "columnarAsciiNoComments\\.txt",
                  testResourceDir + "columnarAsciiNoComments.txt",
                  "",
                  "3",
                  "4",
                  "1440",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "https://www.ndbc.noaa.gov/",
                  "NOAA NDBC",
                  "The new summary!",
                  "The Newer Title!",
                  "-1",
                  ""
                }, // defaultStandardizeWhat, cacheFromUrl
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");
    String suggDatasetID =
        EDDTableFromAwsXmlFiles.suggestDatasetID(testResourceDir + "columnarAsciiNoComments\\.txt");
    String expected =
        "<!-- NOTE! Since the source files don't have any metadata, you must add metadata\n"
            + "  below, notably 'units' for each of the dataVariables. -->\n"
            + "<dataset type=\"EDDTableFromColumnarAsciiFiles\" datasetID=\""
            + suggDatasetID
            + "\" active=\"true\">\n"
            + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
            + "    <updateEveryNMillis>10000</updateEveryNMillis>\n"
            + "    <fileDir>"
            + testResourceDir
            + "</fileDir>\n"
            + "    <fileNameRegex>columnarAsciiNoComments\\.txt</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <charset>ISO-8859-1</charset>\n"
            + "    <columnNamesRow>3</columnNamesRow>\n"
            + "    <firstDataRow>4</firstDataRow>\n"
            + "    <standardizeWhat>0</standardizeWhat>\n"
            + "    <sortFilesBySourceNames></sortFilesBySourceNames>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n"
            + "    <!-- sourceAttributes>\n"
            + "    </sourceAttributes -->\n"
            + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
            + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
            + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
            + "    -->\n"
            + "    <addAttributes>\n"
            + "        <att name=\"cdm_data_type\">Other</att>\n"
            + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
            + "        <att name=\"creator_email\">webmaster.ndbc@noaa.gov</att>\n"
            + "        <att name=\"creator_name\">NOAA NDBC</att>\n"
            + "        <att name=\"creator_type\">institution</att>\n"
            + "        <att name=\"creator_url\">https://www.ndbc.noaa.gov/</att>\n"
            + "        <att name=\"infoUrl\">https://www.ndbc.noaa.gov/</att>\n"
            + "        <att name=\"institution\">NOAA NDBC</att>\n"
            + "        <att name=\"keywords\">aBoolean, aByte, aChar, aDouble, aFloat, aLong, anInt, aShort, aString, boolean, buoy, byte, center, char, data, double, float, int, long, national, ndbc, newer, noaa, short, string, title</att>\n"
            + "        <att name=\"license\">[standard]</att>\n"
            + "        <att name=\"sourceUrl\">(local files)</att>\n"
            + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
            + "        <att name=\"summary\">The new summary! NOAA National Data Buoy Center (NDBC) data from a local source.</att>\n"
            + "        <att name=\"title\">The Newer Title!</att>\n"
            + "    </addAttributes>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aString</sourceName>\n"
            + "        <destinationName>aString</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A String</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">0</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">9</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aChar</sourceName>\n"
            + "        <destinationName>aChar</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Char</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">9</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">15</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aBoolean</sourceName>\n"
            + "        <destinationName>aBoolean</destinationName>\n"
            + "        <dataType>String</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Boolean</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">15</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">24</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aByte</sourceName>\n"
            + "        <destinationName>aByte</destinationName>\n"
            + "        <dataType>byte</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"byte\">127</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Byte</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">24</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">30</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aShort</sourceName>\n"
            + "        <destinationName>aShort</destinationName>\n"
            + "        <dataType>short</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Short</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">30</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">37</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>anInt</sourceName>\n"
            + "        <destinationName>anInt</destinationName>\n"
            + "        <dataType>int</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"int\">2147483647</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">An Int</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">37</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">45</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aLong</sourceName>\n"
            + "        <destinationName>aLong</destinationName>\n"
            + "        <dataType>long</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"long\">9223372036854775807</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Long</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">45</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">57</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aFloat</sourceName>\n"
            + "        <destinationName>aFloat</destinationName>\n"
            + "        <dataType>float</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"float\">NaN</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Float</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">57</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">66</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "    <dataVariable>\n"
            + "        <sourceName>aDouble</sourceName>\n"
            + "        <destinationName>aDouble</destinationName>\n"
            + "        <dataType>double</dataType>\n"
            + "        <!-- sourceAttributes>\n"
            + "        </sourceAttributes -->\n"
            + "        <addAttributes>\n"
            + "            <att name=\"_FillValue\" type=\"double\">NaN</att>\n"
            + "            <att name=\"ioos_category\">Unknown</att>\n"
            + "            <att name=\"long_name\">A Double</att>\n"
            + "            <att name=\"startColumn\" type=\"int\">66</att>\n"
            + "            <att name=\"stopColumn\" type=\"int\">84</att>\n"
            + "        </addAttributes>\n"
            + "    </dataVariable>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ensure it is ready-to-use by making a dataset from it
    EDD.deleteCachedDatasetInfo(suggDatasetID);
    EDD edd = EDDTableFromColumnarAsciiFiles.oneFromXmlFragment(null, results);
    Test.ensureEqual(edd.datasetID(), suggDatasetID, "");
    Test.ensureEqual(edd.title(), "The Newer Title!", "");
    Test.ensureEqual(
        String2.toCSSVString(edd.dataVariableDestinationNames()),
        "aString, aChar, aBoolean, aByte, aShort, anInt, aLong, aFloat, aDouble",
        "destinationNames");

    String userDapQuery = "";
    String tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            edd.className() + "_1",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n"
            + ",,,,,,,,\n"
            + "abcdef,Ab,t,24,24000,24000000,240000000000,2.4,2.412345678987654\n"
            + "short:,,,NaN,NaN,NaN,NaN,NaN,NaN\n"
            + "fg,F,true,11,12001,1200000,12000000000,1.21,1.0E200\n"
            + "h,H,1,12,12002,120000,1200000000,1.22,2.0E200\n"
            + "i,I,TRUE,13,12003,12000,120000000,1.23,3.0E200\n"
            + "j,J,f,14,12004,1200,12000000,1.24,4.0E200\n"
            + "k,K,false,15,12005,120,1200000,1.25,5.0E200\n"
            + "l,L,0,16,12006,12,120000,1.26,6.0E200\n"
            + "m,M,FALSE,17,12007,121,12000,1.27,7.0E200\n"
            + "n,N,8,18,12008,122,1200,1.28,8.0E200\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /**
   * This tests the methods in this class with a 1D dataset. This tests skipHeaderToRegex and
   * skipLinesRegex.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testBasic() throws Throwable {
    // String2.log("\n*** EDDTableFromColumnarAsciiFiles.testBasic()\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to
    // check min:sec.
    String testDir = EDStatic.fullTestCacheDirectory;

    String id = "testTableColumnarAscii";
    EDDTableFromColumnarAsciiFiles.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettestTableColumnarAscii();

    // *** test getting das for entire dataset
    String2.log("\nEDDTableFromColumnarAsciiFiles test das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", testDir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + " s {\n"
            + "  fileName {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"File Name\";\n"
            + "  }\n"
            + "  five {\n"
            + "    Float32 actual_range 5.0, 5.0;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"Five\";\n"
            + "  }\n"
            + "  aString {\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A String\";\n"
            + "  }\n"
            + "  aChar {\n"
            + "    String actual_range \"A\nN\";\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Char\";\n"
            + "  }\n"
            + "  aBoolean {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 1;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Boolean\";\n"
            + "  }\n"
            + "  aByte {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 11, 24;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Byte\";\n"
            + "  }\n"
            + "  aShort {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Int16 actual_range 12001, 24000;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Short\";\n"
            + "  }\n"
            + "  anInt {\n"
            + "    Int32 _FillValue 2147483647;\n"
            + "    Int32 actual_range 12, 24000000;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"An Int\";\n"
            + "  }\n"
            + "  aLong {\n"
            + "    Float64 _FillValue 9223372036854775807;\n"
            + "    Float64 actual_range 1200, 240000000000;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Long\";\n"
            + "  }\n"
            + "  aFloat {\n"
            + "    Float32 actual_range 1.21, 2.4;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Float\";\n"
            + "  }\n"
            + "  aDouble {\n"
            + "    Float64 actual_range 2.412345678987654, 8.0e+200;\n"
            + "    String ioos_category \"Unknown\";\n"
            + "    String long_name \"A Double\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_data_type \"Other\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    String creator_name \"NOAA NDBC\";\n"
            + "    String creator_url \"https://www.ndbc.noaa.gov/\";\n"
            + "    String history \""
            + today;
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // "2014-12-04T19:15:21Z (local files)
    // 2014-12-04T19:15:21Z
    // http://localhost:8080/cwexperimental/tabledap/testTableColumnarAscii.das";
    expected =
        "    String infoUrl \"https://www.ndbc.noaa.gov/\";\n"
            + "    String institution \"NOAA NDBC\";\n"
            + "    String keywords \"boolean, byte, char, double, float, int, long, ndbc, newer, noaa, short, string, title\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    String sourceUrl \"(local files)\";\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"aString, aChar, aBoolean, aByte, aShort, anInt, aLong, aFloat, aDouble, five, fileName\";\n"
            + "    String summary \"The new summary!\";\n"
            + "    String title \"The Newer Title!\";\n"
            + "  }\n"
            + "}\n";
    int tPo = results.indexOf(expected.substring(0, 20));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", testDir, eddTable.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String fileName;\n"
            + "    Float32 five;\n"
            + "    String aString;\n"
            + "    String aChar;\n"
            + "    Byte aBoolean;\n"
            + "    Byte aByte;\n"
            + "    Int16 aShort;\n"
            + "    Int32 anInt;\n"
            + "    Float64 aLong;\n"
            + "    Float32 aFloat;\n"
            + "    Float64 aDouble;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // does aBoolean know it's a boolean?
    Test.ensureTrue(
        eddTable.findVariableByDestinationName("aBoolean").isBoolean(),
        "Is aBoolean edv.isBoolean() true?");

    // .csv for all
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, testDir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "fileName,five,aString,aChar,aBoolean,aByte,aShort,anInt,aLong,aFloat,aDouble\n"
            + ",,,,,,,,,,\n"
            + "columnarAsciiWithComments,5.0,abcdef,A,1,24,24000,24000000,240000000000,2.4,2.412345678987654\n"
            + "columnarAsciiWithComments,5.0,fg,F,1,11,12001,1200000,12000000000,1.21,1.0E200\n"
            + "columnarAsciiWithComments,5.0,h,H,1,12,12002,120000,1200000000,1.22,2.0E200\n"
            + "columnarAsciiWithComments,5.0,i,I,1,13,12003,12000,120000000,1.23,3.0E200\n"
            + "columnarAsciiWithComments,5.0,j,J,0,14,12004,1200,12000000,1.24,4.0E200\n"
            + "columnarAsciiWithComments,5.0,k,K,0,15,12005,120,1200000,1.25,5.0E200\n"
            + "columnarAsciiWithComments,5.0,l,L,0,16,12006,12,120000,1.26,6.0E200\n"
            + "columnarAsciiWithComments,5.0,m,M,0,17,12007,121,12000,1.27,7.0E200\n"
            + "columnarAsciiWithComments,5.0,n,N,1,18,12008,122,1200,1.28,8.0E200\n"
            + "columnarAsciiWithComments,5.0,short:,,NaN,NaN,NaN,NaN,NaN,NaN,NaN\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // only subsetVars
    userDapQuery = "fileName,five";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, testDir, eddTable.className() + "_sv", ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    expected =
        "fileName,five\n"
            + ",\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n"
            + "columnarAsciiWithComments,5.0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // subset of variables, constrain boolean and five
    userDapQuery = "anInt,fileName,five,aBoolean&aBoolean=1&five=5";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, testDir, eddTable.className() + "_conbool", ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    expected =
        "anInt,fileName,five,aBoolean\n"
            + ",,,\n"
            + "24000000,columnarAsciiWithComments,5.0,1\n"
            + "1200000,columnarAsciiWithComments,5.0,1\n"
            + "120000,columnarAsciiWithComments,5.0,1\n"
            + "12000,columnarAsciiWithComments,5.0,1\n"
            + "122,columnarAsciiWithComments,5.0,1\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    String2.log("\n*** EDDTableFromColumnarAsciiFiles.testBasic() finished successfully\n");
  }

  /**
   * This tests reading glerl .dat files with Year[space]Day. Test file is from
   * https://coastwatch.glerl.noaa.gov/statistic/statistic.html then clicking on "Average GLSEA
   * Surface Water Temperature Data" / 1995 to get
   * https://coastwatch.glerl.noaa.gov/ftp/glsea/avgtemps/1995/glsea-temps1995.dat stored as
   * /erddapTest/ascii/glsea-temps1995.dat
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testGlerl() throws Throwable {
    // String2.log("\n*** EDDTableFromColumnarAsciiFiles.testGlerl()\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to
    // check min:sec.
    String testDir = EDStatic.fullTestCacheDirectory;

    String dataDir =
        Path.of(EDDTableFromColumnarAsciiFilesTests.class.getResource("/data/").toURI()).toString();

    // one time for avg temp
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromColumnarAsciiFiles",
                  dataDir + "/ascii/",
                  "glsea-temps1995.dat", // avg temp
                  "",
                  "",
                  "8",
                  "11", // avg temp
                  "10080",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "https://coastwatch.glerl.noaa.gov/statistic/statistic.html",
                  "NOAA GLERL",
                  "Daily lake average surface water temperature from Great Lakes Surface Environmental Analysis maps.",
                  "Great Lakes Average Surface Water Temperature, Daily",
                  "-1",
                  ""
                }, // defaultStandardizeWhat, cacheFromUrl
                false); // doIt loop?
    String2.setClipboardString(results);
    // String2.pressEnterToContinue(results);

    // one time for ice concentration
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromColumnarAsciiFiles",
                  dataDir + "/glerl/",
                  "g2008_2009_ice.dat", // ice
                  "",
                  "",
                  "6",
                  "9", // ice
                  "10080",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "https://coastwatch.glerl.noaa.gov/statistic/statistic.html",
                  "NOAA GLERL",
                  "Daily lake average surface water temperature from Great Lakes Surface Environmental Analysis maps.",
                  "Great Lakes Average Surface Water Temperature, Daily",
                  "-1",
                  ""
                }, // defaultStandardizeWhat, cacheFromUrl
                false); // doIt loop?
    String2.setClipboardString(results);
    // String2.pressEnterToContinue(results);

    // * make the temperature dataset
    String id = "glerlAvgTemp";
    EDDTableFromColumnarAsciiFiles.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.getglerlAvgTemp();

    // .csv for all
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, testDir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "time,Superior,Michigan,Huron,Erie,Ontario,St_Clair\n"
            + "UTC,degree_C,degree_C,degree_C,degree_C,degree_C,degree_C\n"
            + "1994-10-25T00:00:00Z,9.04,13.19,11.76,15.74,12.15,NaN\n"
            + "1994-10-26T00:00:00Z,8.87,13.12,10.81,14.16,11.05,NaN\n"
            + "1994-10-27T00:00:00Z,8.87,13.13,10.82,14.17,11.06,NaN\n"
            + "1994-10-28T00:00:00Z,8.88,12.15,11.65,13.67,10.98,NaN\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    expected =
        "2015-12-26T00:00:00Z,4.06,5.89,5.99,7.33,7.02,6.24\n"
            + "2015-12-27T00:00:00Z,4.04,5.73,5.92,6.94,6.92,6.05\n"
            + "2015-12-28T00:00:00Z,4.02,5.58,5.85,6.57,6.78,5.74\n"
            + "2015-12-29T00:00:00Z,4.0,5.44,5.73,6.14,6.58,5.25\n"
            + "2015-12-30T00:00:00Z,3.97,5.29,5.59,5.79,6.34,4.74\n"
            + "2015-12-31T00:00:00Z,3.95,5.22,5.47,5.55,6.14,4.23\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "\nresults=\n" + results);

    // make sure 1 and only 1 row of data for each year
    for (int year = 1994; year <= 2015; year++) {
      userDapQuery = "&time=\"" + year + "-11-01\"";
      tName =
          eddTable.makeNewFileForDapQuery(
              language,
              null,
              null,
              userDapQuery,
              testDir,
              eddTable.className() + "_" + year,
              ".nc");
      Table table = new Table();
      table.readFlatNc(testDir + tName, null, 0); // standardizeWhat=0
      String2.log(table.dataToString());
      Test.ensureEqual(table.nRows(), 1, "year=" + year);
    }

    // * make the Ice dataset
    id = "glerlIce";
    EDDTableFromColumnarAsciiFiles.deleteCachedDatasetInfo(id);
    eddTable = (EDDTable) EDDTestDataset.getglerlIce();

    // .csv for all
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, testDir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "time,Superior,Michigan,Huron,Erie,Ontario,St_Clair,GL_Total\n"
            + "UTC,percent,percent,percent,percent,percent,percent,percent\n"
            + "2008-12-09T00:00:00Z,2.1,2.12,5.58,0.42,0.24,34.56,2.76\n"
            + "2008-12-11T00:00:00Z,2.08,2.29,6.24,0.63,0.27,15.33,2.9\n"
            + "2008-12-15T00:00:00Z,3.65,4.24,8.64,7.76,1.05,24.88,5.25\n"
            + "2008-12-18T00:00:00Z,4.94,7.66,10.39,6.93,1.4,53.04,6.97\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    String2.log("\n*** EDDTableFromColumnarAsciiFiles.testGlerl() finished successfully\n");
  }

  /**
   * This tests reading glerl .dat files with no column names. Test file is from
   * https://coastwatch.glerl.noaa.gov/statistic/statistic.html then clicking on "Long term average
   * surface water temperature (Data)" 1992/2014 Superior to get
   * https://coastwatch.glerl.noaa.gov/statistic/dat/avgtemps-s_1992-2014.dat stored as
   * /erddapTest/ascii/avgtemps-s_1992-2014.dat
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  void testGlerl2() throws Throwable {
    // String2.log("\n*** EDDTableFromColumnarAsciiFiles.testGlerl2()\n");
    int language = 0;
    // testVerboseOn();
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    EDV edv;
    String today =
        Calendar2.getCurrentISODateTimeStringZulu()
            .substring(0, 14); // 14 is enough to check hour. Hard
    // to
    // check min:sec.
    String testDir = EDStatic.fullTestCacheDirectory;

    // one time
    results =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {
                  "-verbose",
                  "EDDTableFromColumnarAsciiFiles",
                  Path.of(EDDTestDataset.class.getResource("/data/ascii/").toURI()).toString()
                      + "/",
                  "avgtemps-s_1992-2014.dat",
                  "",
                  "",
                  "0",
                  "1",
                  "10080",
                  "",
                  "",
                  "",
                  "",
                  "",
                  "https://coastwatch.glerl.noaa.gov/statistic/statistic.html",
                  "NOAA GLERL",
                  "Great Lakes long term average surface water temperature, daily.",
                  "Great Lakes Long Term Average Surface Water Temperature, Daily",
                  "-1",
                  ""
                }, // defaultStandardizeWhat, cacheFromUrl
                false); // doIt loop?
    String2.setClipboardString(results);
    // String2.pressEnterToContinue(results);

    // make the dataset
    String id = "glerlLTAvgTemp";
    EDDTableFromColumnarAsciiFiles.deleteCachedDatasetInfo(id);
    EDDTable eddTable = (EDDTable) EDDTestDataset.getglerlLTAvgTemp();

    // .csv for all
    userDapQuery = "";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, testDir, eddTable.className() + "_all", ".csv");
    results = File2.directReadFrom88591File(testDir + tName);
    // String2.log(results);
    expected =
        "lake,dayOfYear,temperature\n"
            + ",count,degree_C\n"
            + "s,1,3.3175\n"
            + "s,2,3.2395\n"
            + "s,3,3.2215\n"
            + "s,4,3.2295\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    String2.log("\n*** EDDTableFromColumnarAsciiFiles.testGlerl() finished successfully\n");
  }
}
