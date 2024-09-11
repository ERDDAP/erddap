package gov.noaa.pfel.erddap.dataset;

import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeAll;
import tags.TagExternalOther;
import testDataset.EDDTestDataset;
import testDataset.Initialization;

// None of the thests in this class currently run. They were all disabled before the JUnit
// migration.
// Tests were migrated over and not changed. They all rely on SOS server and all SOS tests were
// previously disabled.
class EDDTableTests {
  @BeforeAll
  static void init() {
    Initialization.edStatic();
  }

  /** Test SOS server using cwwcNDBCMet. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testSosNdbcMet() throws Throwable {
    String2.log("\n*** EDDTable.testSosNdbcMet()");
    EDDTable eddTable = (EDDTable) EDDTable.oneFromDatasetsXml(null, "cwwcNDBCMet");
    String dir = EDStatic.fullTestCacheDirectory;
    String sosQuery, fileName, results, expected;
    int language = 0;
    java.io.StringWriter writer;
    ByteArrayOutputStream baos;
    OutputStreamSourceSimple osss;
    String fullPhenomenaDictionaryUrl =
        EDStatic.erddapUrl
            + "/sos/"
            + eddTable.datasetID
            + "/"
            + EDDTable.sosPhenomenaDictionaryUrl;

    // GetCapabilities
    String2.log("\n+++ GetCapabilities");
    writer = new java.io.StringWriter();
    HashMap<String, String> queryMap =
        EDD.userQueryHashMap(
            "seRvIcE=SOS&ReQueSt=GetCapabilities&sEctIons=gibberish,All",
            true); // true=names toLowerCase
    eddTable.sosGetCapabilities(language, queryMap, writer, null);
    results = writer.toString();
    expected =
        "<?xml version=\"1.0\"?>\n"
            + "<Capabilities\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns=\"http://www.opengis.net/sos/1.0\"\n"
            + "  xmlns:sos=\"http://www.opengis.net/sos/1.0\"\n"
            + "  xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n"
            + "  xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
            + "  xmlns:tml=\"http://www.opengis.net/tml\"\n"
            + "  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n"
            + "  xmlns:myorg=\"http://www.myorg.org/features\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/sos/1.0 http://schemas.opengis.net/sos/1.0.0/sosAll.xsd\"\n"
            + "  version=\"1.0.0\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <ows:ServiceIdentification>\n"
            + "    <ows:Title>SOS for NDBC Standard Meteorological Buoy Data</ows:Title>\n"
            + "    <ows:Abstract>The National Data Buoy Center (NDBC) distributes meteorological data from \n"
            + "moored buoys maintained by NDBC and others. Moored buoys are the weather \n"
            + "sentinels of the sea. They are deployed in the coastal and offshore waters \n"
            + "from the western Atlantic to the Pacific Ocean around Hawaii, and from the \n"
            + "Bering Sea to the South Pacific. NDBC&#39;s moored buoys measure and transmit \n"
            + "barometric pressure; wind direction, speed, and gust; air and sea \n"
            + "temperature; and wave energy spectra from which significant wave height, \n"
            + "dominant wave period, and average wave period are derived. Even the \n"
            + "direction of wave propagation is measured on many moored buoys. \n"
            + "\n"
            + "The data is from NOAA NDBC. It has been reformatted by NOAA Coastwatch, West Coast Node.\n"
            + "\n"
            + "This dataset has both historical data (quality controlled, before 2009-12-\n"
            + "01T00:00:00) and near real time data (less quality controlled, from 2009-12-\n"
            + "01T00:00:00 on).</ows:Abstract>\n"
            + "    <ows:Keywords>\n"
            + "      <ows:Keyword>EARTH SCIENCE</ows:Keyword>\n"
            + "      <ows:Keyword>Oceans</ows:Keyword>\n"
            + "    </ows:Keywords>\n"
            + "    <ows:ServiceType codeSpace=\"http://opengeospatial.net\">OGC:SOS</ows:ServiceType>\n"
            + "    <ows:ServiceTypeVersion>1.0.0</ows:ServiceTypeVersion>\n"
            + "    <ows:Fees>NONE</ows:Fees>\n"
            + "    <ows:AccessConstraints>NONE</ows:AccessConstraints>\n"
            + "  </ows:ServiceIdentification>\n"
            + "  <ows:ServiceProvider>\n"
            + "    <ows:ProviderName>NOAA Environmental Research Division</ows:ProviderName>\n"
            + "    <ows:ProviderSite xlink:href=\"http://localhost:8080/cwexperimental\"/>\n"
            + "    <ows:ServiceContact>\n"
            + "      <ows:IndividualName>Bob Simons</ows:IndividualName>\n"
            + "      <ows:ContactInfo>\n"
            + "        <ows:Phone>\n"
            + "          <ows:Voice>831-658-3205</ows:Voice>\n"
            + "        </ows:Phone>\n"
            + "        <ows:Address>\n"
            + "          <ows:DeliveryPoint>1352 Lighthouse Ave.</ows:DeliveryPoint>\n"
            + "          <ows:City>Pacific Grove</ows:City>\n"
            + "          <ows:AdministrativeArea>CA</ows:AdministrativeArea>\n"
            + "          <ows:PostalCode>93950</ows:PostalCode>\n"
            + "          <ows:Country>USA</ows:Country>\n"
            + "          <ows:ElectronicMailAddress>erd.data@noaa.gov</ows:ElectronicMailAddress>\n"
            + "        </ows:Address>\n"
            + "      </ows:ContactInfo>\n"
            + "    </ows:ServiceContact>\n"
            + "  </ows:ServiceProvider>\n"
            + "  <ows:OperationsMetadata>\n"
            + "    <ows:Operation name=\"GetCapabilities\">\n"
            + "      <ows:DCP>\n"
            + "        <ows:HTTP>\n"
            + "          <ows:Get xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/server\"/>\n"
            + "        </ows:HTTP>\n"
            + "      </ows:DCP>\n"
            + "      <ows:Parameter name=\"Sections\">\n"
            + "        <ows:AllowedValues>\n"
            + "          <ows:Value>ServiceIdentification</ows:Value>\n"
            + "          <ows:Value>ServiceProvider</ows:Value>\n"
            + "          <ows:Value>OperationsMetadata</ows:Value>\n"
            + "          <ows:Value>Contents</ows:Value>\n"
            + "          <ows:Value>All</ows:Value>\n"
            + "        </ows:AllowedValues>\n"
            + "      </ows:Parameter>\n"
            + "    </ows:Operation>\n"
            + "    <ows:Operation name=\"GetObservation\">\n"
            + "      <ows:DCP>\n"
            + "        <ows:HTTP>\n"
            + "          <ows:Get xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/server\"/>\n"
            + "        </ows:HTTP>\n"
            + "      </ows:DCP>\n"
            + "      <ows:Parameter name=\"observedProperty\">\n"
            + "        <ows:AllowedValues>\n"
            + "          <ows:Value>cwwcNDBCMet</ows:Value>\n"
            + "        </ows:AllowedValues>\n"
            + "      </ows:Parameter>\n"
            + "    </ows:Operation>\n"
            + "    <ows:Operation name=\"DescribeSensor\">\n"
            + "      <ows:DCP>\n"
            + "        <ows:HTTP>\n"
            + "          <ows:Get xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/server\"/>\n"
            + "        </ows:HTTP>\n"
            + "      </ows:DCP>\n"
            + "      <ows:Parameter name=\"outputFormat\">\n"
            + "        <ows:AllowedValues>\n"
            + "          <ows:Value>text/xml;subtype=\"sensorML/1.0.0\"</ows:Value>\n"
            + "        </ows:AllowedValues>\n"
            + "      </ows:Parameter>\n"
            + "    </ows:Operation>\n"
            + "    <ows:Parameter name=\"service\">\n"
            + "      <ows:AllowedValues>\n"
            + "        <ows:Value>SOS</ows:Value>\n"
            + "      </ows:AllowedValues>\n"
            + "    </ows:Parameter>\n"
            + "    <ows:Parameter name=\"version\">\n"
            + "      <ows:AllowedValues>\n"
            + "        <ows:Value>1.0.0</ows:Value>\n"
            + "      </ows:AllowedValues>\n"
            + "    </ows:Parameter>\n"
            + "  </ows:OperationsMetadata>\n"
            + "  <Contents>\n"
            + "    <ObservationOfferingList>\n"
            + "      <ObservationOffering gml:id=\"network-cwwcNDBCMet\">\n"
            + "        <gml:description>network cwwcNDBCMet</gml:description>\n"
            + "        <gml:name>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</gml:name>\n"
            + "        <gml:srsName>urn:ogc:def:crs:epsg::4326</gml:srsName>\n"
            + "        <gml:boundedBy>\n"
            + "          <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "            <gml:lowerCorner>-27.7 -177.58</gml:lowerCorner>\n"
            + "            <gml:upperCorner>70.4 179.02</gml:upperCorner>\n"
            + "          </gml:Envelope>\n"
            + "        </gml:boundedBy>\n"
            + "        <time>\n"
            + "          <gml:TimePeriod>\n"
            + "            <gml:beginPosition>1970-02-26T20:00:00Z</gml:beginPosition>\n"
            + "            <gml:endPosition>2010-01-08T20:00:00Z</gml:endPosition>\n"
            + "          </gml:TimePeriod>\n"
            + "        </time>\n"
            + "        <procedure xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:23020:\"/>\n"
            + "        <procedure xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:31201:\"/>\n"
            + "        <procedure xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:32012:\"/>\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, results.substring(0, 7000));

    // String2.log("\n...\n");
    // String2.log(results.substring(60000, 67000));
    // String2.log("\n...\n");
    String2.log(results.substring(results.length() - 5000));
    expected =
        // " </ObservationOffering>\n" +
        "      <ObservationOffering gml:id=\"Station-YRSV2\">\n"
            + "        <gml:description>Station YRSV2</gml:description>\n"
            + "        <gml:name>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:YRSV2:</gml:name>\n"
            + "        <gml:srsName>urn:ogc:def:crs:epsg::4326</gml:srsName>\n"
            + "        <gml:boundedBy>\n"
            + "          <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "            <gml:lowerCorner>37.4142 -76.7125</gml:lowerCorner>\n"
            + "            <gml:upperCorner>37.4142 -76.7125</gml:upperCorner>\n"
            + "          </gml:Envelope>\n"
            + "        </gml:boundedBy>\n"
            + "        <time>\n"
            + "          <gml:TimePeriod>\n"
            + "            <gml:beginPosition>2007-07-10T08:00:00Z</gml:beginPosition>\n"
            + "            <gml:endPosition>2010-01-08T11:00:00Z</gml:endPosition>\n"
            + "          </gml:TimePeriod>\n"
            + "        </time>\n"
            + "        <procedure xlink:href=\"urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:YRSV2:cwwcNDBCMet\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wd\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspd\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#gst\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wvht\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dpd\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#apd\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#mwd\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#bar\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#atmp\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wtmp\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dewp\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#vis\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#ptdy\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#tide\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspu\"/>\n"
            + "        <observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspv\"/>\n"
            + "        <featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n"
            + "        <responseFormat>text/xml;schema=&quot;ioos/0.6.1&quot;</responseFormat>\n"
            + "        <responseFormat>application/ioos+xml;version=0.6.1</responseFormat>\n"
            + "        <responseFormat>application/com-xml</responseFormat>\n"
            + "        <responseFormat>text/csv</responseFormat>\n"
            + "        <responseFormat>application/json;subtype=geojson</responseFormat>\n"
            + "        <responseFormat>text/html</responseFormat>\n"
            + "        <responseFormat>application/json</responseFormat>\n"
            + "        <responseFormat>application/x-matlab-mat</responseFormat>\n"
            + "        <responseFormat>application/x-netcdf</responseFormat>\n"
            + "        <responseFormat>text/tab-separated-values</responseFormat>\n"
            + "        <responseFormat>application/xhtml+xml</responseFormat>\n"
            + "        <responseFormat>application/vnd.google-earth.kml+xml</responseFormat>\n"
            + "        <responseFormat>application/pdf</responseFormat>\n"
            + "        <responseFormat>image/png</responseFormat>\n"
            + "        <resultModel>om:Observation</resultModel>\n"
            +
            // " <responseMode>inline</responseMode>\n" +
            // " <responseMode>out-of-band</responseMode>\n" +
            "      </ObservationOffering>\n"
            + "    </ObservationOfferingList>\n"
            + "  </Contents>\n"
            + "</Capabilities>\n";
    int po = results.indexOf(expected.substring(0, 50));
    if (po < 0) String2.log(results);
    Test.ensureEqual(results.substring(po), expected, results.substring(results.length() - 5000));

    // phenomenaDictionary
    String2.log("\n+++ phenomenaDictionary");
    writer = new java.io.StringWriter();
    eddTable.sosPhenomenaDictionary(writer);
    results = writer.toString();
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<gml:Dictionary gml:id=\"PhenomenaDictionary0.6.1\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.2\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/swe/1.0.2 http://schemas.opengis.net/sweCommon/1.0.2/phenomenon.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\"\n"
            + // GONE
            "  >\n"
            + "  <gml:description>Dictionary of observed phenomena for cwwcNDBCMet.</gml:description>\n"
            + "  <gml:identifier codeSpace=\"urn:ioos:phenomena:1.0.0.127.cwwcNDBCMet\">PhenomenaDictionary</gml:identifier>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"wd\">\n"
            + "      <gml:description>Wind Direction</gml:description>\n"
            +
            // will they switch from marinemetadata (GONE) to
            // http://mmisw.org/ont/cf/parameter/ ?
            "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">wind_from_direction</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"wspd\">\n"
            + "      <gml:description>Wind Speed</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">wind_speed</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"gst\">\n"
            + "      <gml:description>Wind Gust Speed</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">wind_speed_of_gust</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"wvht\">\n"
            + "      <gml:description>Wave Height</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_significant_height</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"dpd\">\n"
            + "      <gml:description>Wave Period, Dominant</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_period</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"apd\">\n"
            + "      <gml:description>Wave Period, Average</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_period</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"mwd\">\n"
            + "      <gml:description>Wave Direction</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_swell_wave_to_direction</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"bar\">\n"
            + "      <gml:description>Air Pressure</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">air_pressure_at_sea_level</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"atmp\">\n"
            + "      <gml:description>Air Temperature</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">air_temperature</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"wtmp\">\n"
            + "      <gml:description>SST</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">sea_surface_temperature</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"dewp\">\n"
            + "      <gml:description>Dewpoint Temperature</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">dew_point_temperature</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"vis\">\n"
            + "      <gml:description>Station Visibility</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">visibility_in_air</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"ptdy\">\n"
            + "      <gml:description>Pressure Tendency</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">tendency_of_air_pressure</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"tide\">\n"
            + "      <gml:description>Water Level</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">surface_altitude</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"wspu\">\n"
            + "      <gml:description>Wind Speed, Zonal</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">eastward_wind</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:Phenomenon gml:id=\"wspv\">\n"
            + "      <gml:description>Wind Speed, Meridional</gml:description>\n"
            + "      <gml:identifier codeSpace=\"http://marinemetadata.org/cf\">northward_wind</gml:identifier>\n"
            + "    </swe:Phenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "  <gml:definitionMember >\n"
            + "    <swe:CompositePhenomenon gml:id=\"cwwcNDBCMet\" dimension=\"16\">\n"
            + "      <gml:description>NDBC Standard Meteorological Buoy Data</gml:description>\n"
            + "      <gml:identifier codeSpace=\"urn:ioos:phenomena:1.0.0.127.cwwcNDBCMet\">cwwcNDBCMet</gml:identifier>\n"
            + "      <swe:component xlink:href=\"#wd\"/>\n"
            + "      <swe:component xlink:href=\"#wspd\"/>\n"
            + "      <swe:component xlink:href=\"#gst\"/>\n"
            + "      <swe:component xlink:href=\"#wvht\"/>\n"
            + "      <swe:component xlink:href=\"#dpd\"/>\n"
            + "      <swe:component xlink:href=\"#apd\"/>\n"
            + "      <swe:component xlink:href=\"#mwd\"/>\n"
            + "      <swe:component xlink:href=\"#bar\"/>\n"
            + "      <swe:component xlink:href=\"#atmp\"/>\n"
            + "      <swe:component xlink:href=\"#wtmp\"/>\n"
            + "      <swe:component xlink:href=\"#dewp\"/>\n"
            + "      <swe:component xlink:href=\"#vis\"/>\n"
            + "      <swe:component xlink:href=\"#ptdy\"/>\n"
            + "      <swe:component xlink:href=\"#tide\"/>\n"
            + "      <swe:component xlink:href=\"#wspu\"/>\n"
            + "      <swe:component xlink:href=\"#wspv\"/>\n"
            + "    </swe:CompositePhenomenon>\n"
            + "  </gml:definitionMember>\n"
            + "</gml:Dictionary>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // DescribeSensor all
    // https://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor
    // &service=SOS&version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
    // &sensorID=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0
    // stored as /programs/sos/ndbcSosCurrentsDescribeSensor90810.xml
    String2.log("\n+++ DescribeSensor all");
    writer = new java.io.StringWriter();
    eddTable.sosDescribeSensor(language, null, eddTable.datasetID, writer);
    results = writer.toString();
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<sml:SensorML\n"
            + "  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n"
            + "  version=\"1.0.1\"\n"
            + "  >\n"
            + "  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n"
            + "  <sml:member>\n"
            + "    <sml:System gml:id=\"network-cwwcNDBCMet\">\n"
            + "      <gml:description>NDBC Standard Meteorological Buoy Data, network-cwwcNDBCMet</gml:description>\n"
            + "      <sml:keywords>\n"
            + "        <sml:KeywordList>\n"
            + "          <sml:keyword>EARTH SCIENCE</sml:keyword>\n"
            + "          <sml:keyword>Oceans</sml:keyword>\n"
            + "        </sml:KeywordList>\n"
            + "      </sml:keywords>\n"
            + "\n"
            + "      <sml:identification>\n"
            + "        <sml:IdentifierList>\n"
            + "          <sml:identifier name=\"Station ID\">\n"
            + "            <sml:Term definition=\"urn:ioos:identifier:NOAA:stationID:\">\n"
            + "              <sml:codeSpace xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "              <sml:value>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "          <sml:identifier name=\"Short Name\">\n"
            + "            <sml:Term definition=\"urn:ogc:def:identifier:OGC:shortName\">\n"
            + "              <sml:value>cwwcNDBCMet</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "        </sml:IdentifierList>\n"
            + "      </sml:identification>\n"
            + "\n"
            + "      <sml:classification>\n"
            + "        <sml:ClassifierList>\n"
            + "          <sml:classifier name=\"System Type Identifier\">\n"
            + "            <sml:Term definition=\"urn:ioos:classifier:NOAA:systemTypeID\">\n"
            + "              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n"
            + "              <sml:value>Platform</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:classifier>\n"
            + "        </sml:ClassifierList>\n"
            + "      </sml:classification>\n"
            + "\n"
            + "      <sml:validTime>\n"
            + "        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n"
            + "          <gml:beginPosition>1970-02-26T20:00:00Z</gml:beginPosition>\n"
            + "          <gml:endPosition>2010-01-08T20:00:00Z</gml:endPosition>\n"
            + "        </gml:TimePeriod>\n"
            + "      </sml:validTime>\n"
            + "\n"
            + "      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n"
            + "        <sml:ResponsibleParty>\n"
            + "          <sml:individualName>Bob Simons</sml:individualName>\n"
            + "          <sml:organizationName>NOAA Environmental Research Division</sml:organizationName>\n"
            + "          <sml:contactInfo>\n"
            + "            <sml:phone>\n"
            + "              <sml:voice>831-658-3205</sml:voice>\n"
            + "            </sml:phone>\n"
            + "            <sml:address>\n"
            + "              <sml:deliveryPoint>1352 Lighthouse Ave.</sml:deliveryPoint>\n"
            + "              <sml:city>Pacific Grove</sml:city>\n"
            + "              <sml:administrativeArea>CA</sml:administrativeArea>\n"
            + "              <sml:postalCode>93950</sml:postalCode>\n"
            + "              <sml:country>USA</sml:country>\n"
            + "              <sml:electronicMailAddress>bob.simons@noaa.gov</sml:electronicMailAddress>\n"
            + "            </sml:address>\n"
            + "            <sml:onlineResource xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "          </sml:contactInfo>\n"
            + "        </sml:ResponsibleParty>\n"
            + "      </sml:contact>\n"
            + "\n"
            + "      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n"
            + "        <sml:Document>\n"
            + "          <gml:description>Web page with background information from the source of this dataset</gml:description>\n"
            + "          <sml:format>text/html</sml:format>\n"
            + "          <sml:onlineResource xlink:href=\"https://www.ndbc.noaa.gov/\" />\n"
            + "        </sml:Document>\n"
            + "      </sml:documentation>\n"
            + "\n"
            + "      <sml:components>\n"
            + "        <sml:ComponentList>\n"
            + "          <sml:component name=\"wd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wd\">\n"
            + "              <gml:description>network-cwwcNDBCMet-wd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wspd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wspd\">\n"
            + "              <gml:description>network-cwwcNDBCMet-wspd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wspd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wspd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"gst Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-gst\">\n"
            + "              <gml:description>network-cwwcNDBCMet-gst Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:gst\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"gst\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wvht Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wvht\">\n"
            + "              <gml:description>network-cwwcNDBCMet-wvht Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wvht\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wvht\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"dpd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-dpd\">\n"
            + "              <gml:description>network-cwwcNDBCMet-dpd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:dpd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"dpd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"apd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-apd\">\n"
            + "              <gml:description>network-cwwcNDBCMet-apd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:apd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"apd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"mwd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-mwd\">\n"
            + "              <gml:description>network-cwwcNDBCMet-mwd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:mwd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"mwd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"bar Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-bar\">\n"
            + "              <gml:description>network-cwwcNDBCMet-bar Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:bar\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"bar\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"atmp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-atmp\">\n"
            + "              <gml:description>network-cwwcNDBCMet-atmp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:atmp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"atmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wtmp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wtmp\">\n"
            + "              <gml:description>network-cwwcNDBCMet-wtmp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wtmp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wtmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"dewp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-dewp\">\n"
            + "              <gml:description>network-cwwcNDBCMet-dewp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:def:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:dewp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"dewp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"vis Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-vis\">\n"
            + "              <gml:description>network-cwwcNDBCMet-vis Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:vis\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"vis\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n"
            + "                      <swe:uom code=\"km\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"ptdy Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-ptdy\">\n"
            + "              <gml:description>network-cwwcNDBCMet-ptdy Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:ptdy\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"ptdy\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"tide Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-tide\">\n"
            + "              <gml:description>network-cwwcNDBCMet-tide Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:tide\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"tide\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wspu Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wspu\">\n"
            + "              <gml:description>network-cwwcNDBCMet-wspu Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wspu\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wspu\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wspv Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-wspv\">\n"
            + "              <gml:description>network-cwwcNDBCMet-wspv Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:wspv\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wspv\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"cwwcNDBCMet Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-network-cwwcNDBCMet-cwwcNDBCMet\">\n"
            + "              <gml:description>network-cwwcNDBCMet Platform</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet:cwwcNDBCMet\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wspd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"gst\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wvht\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"dpd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"apd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"mwd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"bar\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"atmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wtmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"dewp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"vis\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n"
            + "                      <swe:uom code=\"km\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"ptdy\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"tide\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wspu\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wspv\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "        </sml:ComponentList>\n"
            + "      </sml:components>\n"
            + "    </sml:System>\n"
            + "  </sml:member>\n"
            + "</sml:SensorML>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // DescribeSensor 41004
    String2.log("\n+++ DescribeSensor 41004");
    writer = new java.io.StringWriter();
    eddTable.sosDescribeSensor(language, null, "41004", writer);
    results = writer.toString();
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<sml:SensorML\n"
            + "  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n"
            + "  version=\"1.0.1\"\n"
            + "  >\n"
            + "  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n"
            + "  <sml:member>\n"
            + "    <sml:System gml:id=\"Station-41004\">\n"
            + "      <gml:description>NDBC Standard Meteorological Buoy Data, Station-41004</gml:description>\n"
            + "      <sml:keywords>\n"
            + "        <sml:KeywordList>\n"
            + "          <sml:keyword>EARTH SCIENCE</sml:keyword>\n"
            + "          <sml:keyword>Oceans</sml:keyword>\n"
            + "        </sml:KeywordList>\n"
            + "      </sml:keywords>\n"
            + "\n"
            + "      <sml:identification>\n"
            + "        <sml:IdentifierList>\n"
            + "          <sml:identifier name=\"Station ID\">\n"
            + "            <sml:Term definition=\"urn:ioos:identifier:url:stationID\">\n"
            + "              <sml:codeSpace xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "              <sml:value>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "          <sml:identifier name=\"Short Name\">\n"
            + "            <sml:Term definition=\"urn:ogc:def:identifier:OGC:shortName\">\n"
            + "              <sml:value>41004</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "        </sml:IdentifierList>\n"
            + "      </sml:identification>\n"
            + "\n"
            + "      <sml:classification>\n"
            + "        <sml:ClassifierList>\n"
            + "          <sml:classifier name=\"Parent Network\">\n"
            + "            <sml:Term definition=\"urn:ioos:classifier:url:parentNetwork\">\n"
            + "              <sml:codeSpace xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "              <sml:value>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:classifier>\n"
            + "          <sml:classifier name=\"System Type Identifier\">\n"
            + "            <sml:Term definition=\"urn:ioos:classifier:URL:systemTypeID\">\n"
            + "              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n"
            + "              <sml:value>Platform</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:classifier>\n"
            + "        </sml:ClassifierList>\n"
            + "      </sml:classification>\n"
            + "\n"
            + "      <sml:validTime>\n"
            + "        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n"
            + "          <gml:beginPosition>1978-06-27T13:00:00Z</gml:beginPosition>\n"
            + "          <gml:endPosition>2010-01-08T19:00:00Z</gml:endPosition>\n"
            + "        </gml:TimePeriod>\n"
            + "      </sml:validTime>\n"
            + "\n"
            + "      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n"
            + "        <sml:ResponsibleParty>\n"
            + "          <sml:individualName>Bob Simons</sml:individualName>\n"
            + "          <sml:organizationName>NOAA Environmental Research Division</sml:organizationName>\n"
            + "          <sml:contactInfo>\n"
            + "            <sml:phone>\n"
            + "              <sml:voice>831-658-3205</sml:voice>\n"
            + "            </sml:phone>\n"
            + "            <sml:address>\n"
            + "              <sml:deliveryPoint>1352 Lighthouse Ave.</sml:deliveryPoint>\n"
            + "              <sml:city>Pacific Grove</sml:city>\n"
            + "              <sml:administrativeArea>CA</sml:administrativeArea>\n"
            + "              <sml:postalCode>93950</sml:postalCode>\n"
            + "              <sml:country>USA</sml:country>\n"
            + "              <sml:electronicMailAddress>bob.simons@noaa.gov</sml:electronicMailAddress>\n"
            + "            </sml:address>\n"
            + "            <sml:onlineResource xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "          </sml:contactInfo>\n"
            + "        </sml:ResponsibleParty>\n"
            + "      </sml:contact>\n"
            + "\n"
            + "      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n"
            + "        <sml:Document>\n"
            + "          <gml:description>Web page with background information from the source of this dataset</gml:description>\n"
            + "          <sml:format>text/html</sml:format>\n"
            + "          <sml:onlineResource xlink:href=\"https://www.ndbc.noaa.gov/\" />\n"
            + "        </sml:Document>\n"
            + "      </sml:documentation>\n"
            + "\n"
            + "      <sml:location>\n"
            + "        <gml:Point srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"Station_LatLon\">\n"
            + "          <gml:coordinates>32.5 -79.09</gml:coordinates>\n"
            + "        </gml:Point>\n"
            + "      </sml:location>\n"
            + "\n"
            + "      <sml:components>\n"
            + "        <sml:ComponentList>\n"
            + "          <sml:component name=\"wd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wd\">\n"
            + "              <gml:description>Station-41004-wd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wspd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wspd\">\n"
            + "              <gml:description>Station-41004-wspd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wspd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wspd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"gst Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-gst\">\n"
            + "              <gml:description>Station-41004-gst Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:gst\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"gst\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wvht Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wvht\">\n"
            + "              <gml:description>Station-41004-wvht Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wvht\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wvht\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"dpd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-dpd\">\n"
            + "              <gml:description>Station-41004-dpd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:dpd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"dpd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"apd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-apd\">\n"
            + "              <gml:description>Station-41004-apd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:apd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"apd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"mwd Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-mwd\">\n"
            + "              <gml:description>Station-41004-mwd Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:mwd\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"mwd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"bar Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-bar\">\n"
            + "              <gml:description>Station-41004-bar Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:bar\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"bar\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"atmp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-atmp\">\n"
            + "              <gml:description>Station-41004-atmp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:atmp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"atmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wtmp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wtmp\">\n"
            + "              <gml:description>Station-41004-wtmp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wtmp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wtmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"dewp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-dewp\">\n"
            + "              <gml:description>Station-41004-dewp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:dewp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"dewp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"vis Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-vis\">\n"
            + "              <gml:description>Station-41004-vis Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:vis\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"vis\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n"
            + "                      <swe:uom code=\"km\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"ptdy Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-ptdy\">\n"
            + "              <gml:description>Station-41004-ptdy Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:ptdy\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"ptdy\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"tide Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-tide\">\n"
            + "              <gml:description>Station-41004-tide Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:tide\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"tide\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wspu Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wspu\">\n"
            + "              <gml:description>Station-41004-wspu Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wspu\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wspu\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"wspv Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wspv\">\n"
            + "              <gml:description>Station-41004-wspv Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wspv\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wspv\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "          <sml:component name=\"cwwcNDBCMet Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-cwwcNDBCMet\">\n"
            + "              <gml:description>Station-41004 Platform</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:cwwcNDBCMet\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_from_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wspd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"gst\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/wind_speed_of_gust\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wvht\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_significant_height\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"dpd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"apd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_period\">\n"
            + "                      <swe:uom code=\"s\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"mwd\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_swell_wave_to_direction\">\n"
            + "                      <swe:uom code=\"degrees_true\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"bar\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_pressure_at_sea_level\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"atmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/air_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wtmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"dewp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/dew_point_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"vis\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/visibility_in_air\">\n"
            + "                      <swe:uom code=\"km\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"ptdy\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/tendency_of_air_pressure\">\n"
            + "                      <swe:uom code=\"hPa\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"tide\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/surface_altitude\">\n"
            + "                      <swe:uom code=\"m\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wspu\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/eastward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                  <sml:output name=\"wspv\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/northward_wind\">\n"
            + "                      <swe:uom code=\"m s-1\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "        </sml:ComponentList>\n"
            + "      </sml:components>\n"
            + "    </sml:System>\n"
            + "  </sml:member>\n"
            + "</sml:SensorML>\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // DescribeSensor wtmp
    // https://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor
    // &service=SOS&version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
    // &sensorID=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0
    // stored as /programs/sos/ndbcSosCurrentsDescribeSensor90810.xml
    String2.log("\n+++ DescribeSensor 41004:wtmp");
    writer = new java.io.StringWriter();
    eddTable.sosDescribeSensor(language, null, "41004", writer);
    results = writer.toString();
    String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<sml:SensorML\n"
            + "  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n"
            + "  version=\"1.0.1\"\n"
            + "  >\n"
            + "  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n"
            + "  <sml:member>\n"
            + "    <sml:System gml:id=\"Station-41004-wtmp\">\n"
            + "      <gml:description>NDBC Standard Meteorological Buoy Data, Station-41004-wtmp</gml:description>\n"
            + "      <sml:keywords>\n"
            + "        <sml:KeywordList>\n"
            + "          <sml:keyword>EARTH SCIENCE</sml:keyword>\n"
            + "          <sml:keyword>Oceans</sml:keyword>\n"
            + "        </sml:KeywordList>\n"
            + "      </sml:keywords>\n"
            + "\n"
            + "      <sml:identification>\n"
            + "        <sml:IdentifierList>\n"
            + "          <sml:identifier name=\"Station ID\">\n"
            + "            <sml:Term definition=\"urn:ioos:identifier:URL:stationID\">\n"
            + "              <sml:codeSpace xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "              <sml:value>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "          <sml:identifier name=\"Short Name\">\n"
            + "            <sml:Term definition=\"urn:ogc:def:identifier:OGC:shortName\">\n"
            + "              <sml:value>41004:wtmp</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "        </sml:IdentifierList>\n"
            + "      </sml:identification>\n"
            + "\n"
            + "      <sml:classification>\n"
            + "        <sml:ClassifierList>\n"
            + "          <sml:classifier name=\"Parent Network\">\n"
            + "            <sml:Term definition=\"urn:ioos:classifier:URL:parentNetwork\">\n"
            + "              <sml:codeSpace xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "              <sml:value>urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:classifier>\n"
            + "          <sml:classifier name=\"System Type Identifier\">\n"
            + "            <sml:Term definition=\"urn:ioos:classifier:URL:systemTypeID\">\n"
            + "              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n"
            + "              <sml:value>Platform</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:classifier>\n"
            + "        </sml:ClassifierList>\n"
            + "      </sml:classification>\n"
            + "\n"
            + "      <sml:validTime>\n"
            + "        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n"
            + "          <gml:beginPosition>1978-06-27T13:00:00Z</gml:beginPosition>\n"
            + "          <gml:endPosition>2010-01-08T19:00:00Z</gml:endPosition>\n"
            + "        </gml:TimePeriod>\n"
            + "      </sml:validTime>\n"
            + "\n"
            + "      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n"
            + "        <sml:ResponsibleParty>\n"
            + "          <sml:individualName>Bob Simons</sml:individualName>\n"
            + "          <sml:organizationName>NOAA Environmental Research Division</sml:organizationName>\n"
            + "          <sml:contactInfo>\n"
            + "            <sml:phone>\n"
            + "              <sml:voice>831-658-3205</sml:voice>\n"
            + "            </sml:phone>\n"
            + "            <sml:address>\n"
            + "              <sml:deliveryPoint>1352 Lighthouse Ave.</sml:deliveryPoint>\n"
            + "              <sml:city>Pacific Grove</sml:city>\n"
            + "              <sml:administrativeArea>CA</sml:administrativeArea>\n"
            + "              <sml:postalCode>93950</sml:postalCode>\n"
            + "              <sml:country>USA</sml:country>\n"
            + "              <sml:electronicMailAddress>bob.simons@noaa.gov</sml:electronicMailAddress>\n"
            + "            </sml:address>\n"
            + "            <sml:onlineResource xlink:href=\"http://localhost:8080/cwexperimental\" />\n"
            + "          </sml:contactInfo>\n"
            + "        </sml:ResponsibleParty>\n"
            + "      </sml:contact>\n"
            + "\n"
            + "      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n"
            + "        <sml:Document>\n"
            + "          <gml:description>Web page with background information from the source of this dataset</gml:description>\n"
            + "          <sml:format>text/html</sml:format>\n"
            + "          <sml:onlineResource xlink:href=\"https://www.ndbc.noaa.gov/\" />\n"
            + "        </sml:Document>\n"
            + "      </sml:documentation>\n"
            + "\n"
            + "      <sml:location>\n"
            + "        <gml:Point srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"Station_LatLon\">\n"
            + "          <gml:coordinates>32.5 -79.09</gml:coordinates>\n"
            + "        </gml:Point>\n"
            + "      </sml:location>\n"
            + "\n"
            + "      <sml:components>\n"
            + "        <sml:ComponentList>\n"
            + "          <sml:component name=\"wtmp Instrument\">\n"
            + "            <sml:System gml:id=\"sensor-Station-41004-wtmp\">\n"
            + "              <gml:description>Station-41004-wtmp Sensor</gml:description>\n"
            + "              <sml:identification xlink:href=\"urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:wtmp\" />\n"
            + "              <sml:documentation xlink:href=\"http://localhost:8080/cwexperimental/info/cwwcNDBCMet.html\" />\n"
            + "              <sml:outputs>\n"
            + "                <sml:OutputList>\n"
            + "                  <sml:output name=\"wtmp\">\n"
            + "                    <swe:Quantity definition=\"http://marinemetadata.org/cf/sea_surface_temperature\">\n"
            + "                      <swe:uom code=\"degree_C\" />\n"
            + "                    </swe:Quantity>\n"
            + "                  </sml:output>\n"
            + "                </sml:OutputList>\n"
            + "              </sml:outputs>\n"
            + "            </sml:System>\n"
            + "          </sml:component>\n"
            + "        </sml:ComponentList>\n"
            + "      </sml:components>\n"
            + "    </sml:System>\n"
            + "  </sml:member>\n"
            + "</sml:SensorML>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ???!!!
    // test sosQueryToDapQuery() more
    // ???write tests of invalid queries?
    // need test of xml response for currents (depths)
    // support var vs. var plot?
    // need test of png of sticks (ensure 2 vars have same units?)

    /*  */

    // *** observations for 1 station, all vars CSV response
    String endOfRequest = "sos/cwwcNDBCMet/get"; // is that right? does it matter here?
    String sosQuery1 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:"
            + "&observedProperty=cwwcNDBCMet"
            + "&responseFormat=text/xml;schema=%22ioos/0.6.1%22"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
    String sosQuery1Csv =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:"
            + // long
            "&observedProperty="
            + fullPhenomenaDictionaryUrl
            + "#cwwcNDBCMet"
            + // long
            "&responseFormat=text/csv"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
    String2.log("\n+++ GetObservations for 1 station  CSV\n" + sosQuery1);
    String dapQuery1[] = eddTable.sosQueryToDapQuery(language, null, sosQuery1);
    String2.log("\nsosQuery1=" + sosQuery1 + "\n\ndapQuery1=" + dapQuery1[0]);
    Test.ensureEqual(
        dapQuery1[0],
        "&station=%2241004%22&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z",
        "");
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery1Csv, "someIPAddress", null, osss, dir, "testSos1Sta");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        // from eddTableSos.testNdbcSosWind
        // "longitude, latitude, station_id, altitude, time, WindSpeed, WindDirection,
        // WindVerticalVelocity, WindGust\n" +
        // "degrees_east, degrees_north, , m, UTC, m s-1, degrees_true, m s-1, m s-1\n"
        // +
        // "-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN,
        // 2008-08-01T00:50:00Z, 10.1, 229.0, NaN, 12.6\n" +
        // "-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN,
        // 2008-08-01T01:50:00Z, 9.3, 232.0, NaN, 11.3\n" +
        // "-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN,
        // 2008-08-01T02:50:00Z, 7.8, 237.0, NaN, 11.5\n" +
        // "-79.09, 32.5, urn:ioos:station:noaa.nws.ndbc:41004:, NaN,
        // 2008-08-01T03:50:00Z, 8.0, 236.0, NaN, 9.3\n";

        "longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n"
            + "degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n"
            + "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #1: observations for 1 station, all vars, sos XML response
    String2.log("\n+++ GetObservations for 1 station  XML\n" + sosQuery1);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery1, "someIPAddress", null, osss, dir, "testSos1");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:CompositeObservation xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n"
            + "  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n"
            + "  <gml:name>NDBC Standard Meteorological Buoy Data, Station 41004</gml:name>\n"
            + "  <gml:boundedBy>\n"
            + "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "      <gml:lowerCorner>32.5 -79.09</gml:lowerCorner>\n"
            + "      <gml:upperCorner>32.5 -79.09</gml:upperCorner>\n"
            + "    </gml:Envelope>\n"
            + "  </gml:boundedBy>\n"
            + "  <om:samplingTime>\n"
            + "    <gml:TimePeriod gml:id=\"ST\">\n"
            + "      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n"
            + "      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n"
            + "    </gml:TimePeriod>\n"
            + "  </om:samplingTime>\n"
            + "  <om:procedure>\n"
            + "    <om:Process>\n"
            + "      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n"
            + "        <gml:valueComponents>\n"
            + "          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n"
            + "          <ioos:ContextArray gml:id=\"StationArray\">\n"
            + "            <gml:valueComponents>\n"
            + "              <ioos:CompositeContext gml:id=\"Station1Info\">\n"
            + "                <gml:valueComponents>\n"
            + "                  <ioos:StationName>Station - 41004</ioos:StationName>\n"
            + "                  <ioos:Organization>NOAA NDBC, CoastWatch WCN</ioos:Organization>\n"
            + "                  <ioos:StationId>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</ioos:StationId>\n"
            + "                  <gml:Point gml:id=\"Station1LatLon\">\n"
            + "                    <gml:pos>32.5 -79.09</gml:pos>\n"
            + "                  </gml:Point>\n"
            + "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n"
            + "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n"
            + "                  <ioos:Count name=\"Station1NumberOfSensors\">1</ioos:Count>\n"
            + "                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n"
            + "                    <gml:valueComponents>\n"
            + "                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n"
            + "                        <gml:valueComponents>\n"
            + "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:cwwcNDBCMet</ioos:SensorId>\n"
            + "                        </gml:valueComponents>\n"
            + "                      </ioos:CompositeContext>\n"
            + "                    </gml:valueComponents>\n"
            + "                  </ioos:ContextArray>\n"
            + "                </gml:valueComponents>\n"
            + "              </ioos:CompositeContext>\n"
            + "            </gml:valueComponents>\n"
            + "          </ioos:ContextArray>\n"
            + "        </gml:valueComponents>\n"
            + "      </ioos:CompositeContext>\n"
            + "    </om:Process>\n"
            + "  </om:procedure>\n"
            + "  <om:observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n"
            + "  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n"
            + "  <om:result>\n"
            + "    <ioos:Composite gml:id=\"cwwcNDBCMetPointCollectionTimeSeriesDataObservations\">\n"
            + "      <gml:valueComponents>\n"
            + "        <ioos:Count name=\"NumberOfObservationsPoints\">1</ioos:Count>\n"
            + "        <ioos:Array gml:id=\"cwwcNDBCMetPointCollectionTimeSeries\">\n"
            + "          <gml:valueComponents>\n"
            + "            <ioos:Composite gml:id=\"Station1TimeSeriesRecord\">\n"
            + "              <gml:valueComponents>\n"
            + "                <ioos:Count name=\"Station1NumberOfObservationsTimes\">2</ioos:Count>\n"
            + "                <ioos:Array gml:id=\"Station1TimeSeries\">\n"
            + "                  <gml:valueComponents>\n"
            + "                    <ioos:Composite gml:id=\"Station1T1Point\">\n"
            + "                      <gml:valueComponents>\n"
            + "                        <ioos:CompositeContext gml:id=\"Station1T1ObservationConditions\" processDef=\"#Station1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <gml:TimeInstant gml:id=\"Station1T1Time\">\n"
            + "                              <gml:timePosition>2008-08-01T00:00:00Z</gml:timePosition>\n"
            + "                            </gml:TimeInstant>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeContext>\n"
            + "                        <ioos:CompositeValue gml:id=\"Station1T1PointObservation\" processDef=\"#Station1Sensor1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:Quantity name=\"wd\" uom=\"degrees_true\">225</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wspd\" uom=\"m s-1\">10.9</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"gst\" uom=\"m s-1\">14.0</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wvht\" uom=\"m\">1.66</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"dpd\" uom=\"s\">5.26</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"apd\" uom=\"s\">4.17</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"mwd\" uom=\"degrees_true\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"bar\" uom=\"hPa\">1007.6</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"dewp\" uom=\"degree_C\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"vis\" uom=\"km\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"ptdy\" uom=\"hPa\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"tide\" uom=\"m\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"wspu\" uom=\"m s-1\">7.7</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wspv\" uom=\"m s-1\">7.7</ioos:Quantity>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeValue>\n"
            + "                      </gml:valueComponents>\n"
            + "                    </ioos:Composite>\n"
            + "                    <ioos:Composite gml:id=\"Station1T2Point\">\n"
            + "                      <gml:valueComponents>\n"
            + "                        <ioos:CompositeContext gml:id=\"Station1T2ObservationConditions\" processDef=\"#Station1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <gml:TimeInstant gml:id=\"Station1T2Time\">\n"
            + "                              <gml:timePosition>2008-08-01T01:00:00Z</gml:timePosition>\n"
            + "                            </gml:TimeInstant>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeContext>\n"
            + "                        <ioos:CompositeValue gml:id=\"Station1T2PointObservation\" processDef=\"#Station1Sensor1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:Quantity name=\"wd\" uom=\"degrees_true\">229</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wspd\" uom=\"m s-1\">10.1</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"gst\" uom=\"m s-1\">12.6</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wvht\" uom=\"m\">1.68</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"dpd\" uom=\"s\">5.56</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"apd\" uom=\"s\">4.36</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"mwd\" uom=\"degrees_true\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"bar\" uom=\"hPa\">1008.0</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"dewp\" uom=\"degree_C\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"vis\" uom=\"km\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"ptdy\" uom=\"hPa\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"tide\" uom=\"m\" xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
            + "                            <ioos:Quantity name=\"wspu\" uom=\"m s-1\">7.6</ioos:Quantity>\n"
            + "                            <ioos:Quantity name=\"wspv\" uom=\"m s-1\">6.6</ioos:Quantity>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeValue>\n"
            + "                      </gml:valueComponents>\n"
            + "                    </ioos:Composite>\n"
            + "                  </gml:valueComponents>\n"
            + "                </ioos:Array>\n"
            + "              </gml:valueComponents>\n"
            + "            </ioos:Composite>\n"
            + "          </gml:valueComponents>\n"
            + "        </ioos:Array>\n"
            + "      </gml:valueComponents>\n"
            + "    </ioos:Composite>\n"
            + "  </om:result>\n"
            + "</om:CompositeObservation>\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #1b: observations for 1 station, all vars, no time -> last time, CSV
    // response
    String sosQuery1b =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:"
            + "&observedProperty=cwwcNDBCMet"
            + "&responseFormat=text/csv";
    String2.log("\n+++ 1b: GetObservations for 1 station  CSV\n" + sosQuery1b);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery1b, "someIPAddress", null, osss, dir, "testSos1b");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected = // changes when I update ndbc
        "longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n"
            + "degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n"
            + "-79.09, 32.5, 2010-01-08T19:00:00Z, 41004, 300, 8.0, 9.0, 1.4, 7.0, 4.8, NaN, 1015.2, 6.6, 20.0, 2.2, NaN, -1.5, NaN, 6.9, -4.0\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #2: observations for all stations, all vars, and with BBOX (but just same
    // 1 station) (csv)
    // featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
    String2.log("\n+++ GetObservations with BBOX (1 station)");
    String sosQuery2 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet"
            + "&observedProperty=cwwcNDBCMet"
            + // short
            "&responseFormat=text/csv"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z"
            + "&featureOfInterest=BBOX:-79.10,32.4,-79.08,32.6";
    String dapQuery2[] = eddTable.sosQueryToDapQuery(language, null, sosQuery2);
    String2.log("\nsosQuery2=" + sosQuery2 + "\n\ndapQuery2=" + dapQuery2[0]);
    Test.ensureEqual(
        dapQuery2[0],
        "&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z"
            + "&longitude>=-79.1&longitude<=-79.08&latitude>=32.4&latitude<=32.6",
        "");
    writer = new java.io.StringWriter();
    String2.log("query: " + sosQuery2);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery2, "someIPAddress", null, osss, dir, "testSos2");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n"
            + "degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n"
            + "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #2b: observations for all stations, 2 vars, and with BBOX (but just same
    // 1 station) (csv)
    // featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
    String2.log("\n+++ 2b: GetObservations, 2 vars, with BBOX (1 station)");
    String sosQuery2b =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet"
            + "&observedProperty=atmp,"
            + fullPhenomenaDictionaryUrl
            + "#wtmp"
            + // short and long
            "&responseFormat=text/csv"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z"
            + "&featureOfInterest=BBOX:-79.10,32.4,-79.08,32.6";
    String dapQuery2b[] = eddTable.sosQueryToDapQuery(language, null, sosQuery2b);
    String2.log("\nsosQuery2b=" + sosQuery2b + "\n\ndapQuery2b=" + dapQuery2b[0]);
    Test.ensureEqual(
        dapQuery2b[0],
        "longitude,latitude,time,station,atmp,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z"
            + "&longitude>=-79.1&longitude<=-79.08&latitude>=32.4&latitude<=32.6",
        "");
    writer = new java.io.StringWriter();
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery2b, "someIPAddress", null, osss, dir, "testSos2b");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "longitude, latitude, time, station, atmp, wtmp\n"
            + "degrees_east, degrees_north, UTC, , degree_C, degree_C\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 27.8, 27.9\n"
            + "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 27.8, 27.9\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #2c: observations for all stations, 2 vars, and with BBOX (but just same
    // 1 station) (xml)
    // featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
    String2.log("\n+++ 2c: GetObservations, 2 vars, with BBOX (1 station)");
    String sosQuery2c =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:network:1.0.0.127.cwwcNDBCMet:cwwcNDBCMet"
            + "&observedProperty=atmp,"
            + fullPhenomenaDictionaryUrl
            + "#wtmp"
            + // short and long
            "&responseFormat=text/xml;schema=%22ioos/0.6.1%22"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z"
            + "&featureOfInterest=BBOX:-79.10,32.4,-79.08,32.6";
    String dapQuery2c[] = eddTable.sosQueryToDapQuery(language, null, sosQuery2c);
    String2.log("\nsosQuery2c=" + sosQuery2c + "\n\ndapQuery2c=" + dapQuery2c[0]);
    Test.ensureEqual(
        dapQuery2c[0],
        "longitude,latitude,time,station,atmp,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z"
            + "&longitude>=-79.1&longitude<=-79.08&latitude>=32.4&latitude<=32.6",
        "");
    writer = new java.io.StringWriter();
    String2.log("query: " + sosQuery2c);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery2c, "someIPAddress", null, osss, dir, "testSos2c");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:CompositeObservation xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n"
            + "  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n"
            + "  <gml:name>NDBC Standard Meteorological Buoy Data, network cwwcNDBCMet</gml:name>\n"
            + "  <gml:boundedBy>\n"
            + "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "      <gml:lowerCorner>32.5 -79.09</gml:lowerCorner>\n"
            + "      <gml:upperCorner>32.5 -79.09</gml:upperCorner>\n"
            + "    </gml:Envelope>\n"
            + "  </gml:boundedBy>\n"
            + "  <om:samplingTime>\n"
            + "    <gml:TimePeriod gml:id=\"ST\">\n"
            + "      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n"
            + "      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n"
            + "    </gml:TimePeriod>\n"
            + "  </om:samplingTime>\n"
            + "  <om:procedure>\n"
            + "    <om:Process>\n"
            + "      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n"
            + "        <gml:valueComponents>\n"
            + "          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n"
            + "          <ioos:ContextArray gml:id=\"StationArray\">\n"
            + "            <gml:valueComponents>\n"
            + "              <ioos:CompositeContext gml:id=\"Station1Info\">\n"
            + "                <gml:valueComponents>\n"
            + "                  <ioos:StationName>Station - 41004</ioos:StationName>\n"
            + "                  <ioos:Organization>NOAA NDBC, CoastWatch WCN</ioos:Organization>\n"
            + "                  <ioos:StationId>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</ioos:StationId>\n"
            + "                  <gml:Point gml:id=\"Station1LatLon\">\n"
            + "                    <gml:pos>32.5 -79.09</gml:pos>\n"
            + "                  </gml:Point>\n"
            + "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n"
            + "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n"
            + "                  <ioos:Count name=\"Station1NumberOfSensors\">2</ioos:Count>\n"
            + "                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n"
            + "                    <gml:valueComponents>\n"
            + "                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n"
            + "                        <gml:valueComponents>\n"
            + "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:atmp</ioos:SensorId>\n"
            + "                        </gml:valueComponents>\n"
            + "                      </ioos:CompositeContext>\n"
            + "                      <ioos:CompositeContext gml:id=\"Station1Sensor2Info\">\n"
            + "                        <gml:valueComponents>\n"
            + "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:wtmp</ioos:SensorId>\n"
            + "                        </gml:valueComponents>\n"
            + "                      </ioos:CompositeContext>\n"
            + "                    </gml:valueComponents>\n"
            + "                  </ioos:ContextArray>\n"
            + "                </gml:valueComponents>\n"
            + "              </ioos:CompositeContext>\n"
            + "            </gml:valueComponents>\n"
            + "          </ioos:ContextArray>\n"
            + "        </gml:valueComponents>\n"
            + "      </ioos:CompositeContext>\n"
            + "    </om:Process>\n"
            + "  </om:procedure>\n"
            + "  <om:observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n"
            + "  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n"
            + "  <om:result>\n"
            + "    <ioos:Composite gml:id=\"cwwcNDBCMetPointCollectionTimeSeriesDataObservations\">\n"
            + "      <gml:valueComponents>\n"
            + "        <ioos:Count name=\"NumberOfObservationsPoints\">1</ioos:Count>\n"
            + "        <ioos:Array gml:id=\"cwwcNDBCMetPointCollectionTimeSeries\">\n"
            + "          <gml:valueComponents>\n"
            + "            <ioos:Composite gml:id=\"Station1TimeSeriesRecord\">\n"
            + "              <gml:valueComponents>\n"
            + "                <ioos:Count name=\"Station1NumberOfObservationsTimes\">2</ioos:Count>\n"
            + "                <ioos:Array gml:id=\"Station1TimeSeries\">\n"
            + "                  <gml:valueComponents>\n"
            + "                    <ioos:Composite gml:id=\"Station1T1Point\">\n"
            + "                      <gml:valueComponents>\n"
            + "                        <ioos:CompositeContext gml:id=\"Station1T1ObservationConditions\" processDef=\"#Station1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <gml:TimeInstant gml:id=\"Station1T1Time\">\n"
            + "                              <gml:timePosition>2008-08-01T00:00:00Z</gml:timePosition>\n"
            + "                            </gml:TimeInstant>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeContext>\n"
            + "                        <ioos:CompositeValue gml:id=\"Station1T1Sensor1PointObservation\" processDef=\"#Station1Sensor1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeValue>\n"
            + "                        <ioos:CompositeValue gml:id=\"Station1T1Sensor2PointObservation\" processDef=\"#Station1Sensor2Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeValue>\n"
            + "                      </gml:valueComponents>\n"
            + "                    </ioos:Composite>\n"
            + "                    <ioos:Composite gml:id=\"Station1T2Point\">\n"
            + "                      <gml:valueComponents>\n"
            + "                        <ioos:CompositeContext gml:id=\"Station1T2ObservationConditions\" processDef=\"#Station1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <gml:TimeInstant gml:id=\"Station1T2Time\">\n"
            + "                              <gml:timePosition>2008-08-01T01:00:00Z</gml:timePosition>\n"
            + "                            </gml:TimeInstant>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeContext>\n"
            + "                        <ioos:CompositeValue gml:id=\"Station1T2Sensor1PointObservation\" processDef=\"#Station1Sensor1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:Quantity name=\"atmp\" uom=\"degree_C\">27.8</ioos:Quantity>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeValue>\n"
            + "                        <ioos:CompositeValue gml:id=\"Station1T2Sensor2PointObservation\" processDef=\"#Station1Sensor2Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:Quantity name=\"wtmp\" uom=\"degree_C\">27.9</ioos:Quantity>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeValue>\n"
            + "                      </gml:valueComponents>\n"
            + "                    </ioos:Composite>\n"
            + "                  </gml:valueComponents>\n"
            + "                </ioos:Array>\n"
            + "              </gml:valueComponents>\n"
            + "            </ioos:Composite>\n"
            + "          </gml:valueComponents>\n"
            + "        </ioos:Array>\n"
            + "      </gml:valueComponents>\n"
            + "    </ioos:Composite>\n"
            + "  </om:result>\n"
            + "</om:CompositeObservation>\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    /*
     * //#2d get out-of-band
     * String2.
     * log("\n+++ 2d: GetObservations for 1 station, 2 obsProp, out-of-band xml");
     * baos = new ByteArrayOutputStream();
     * osss = new OutputStreamSourceSimple(baos);
     * eddTable.sosGetObservation(language, endOfRequest, sosQuery2b +
     * "&responseMode=out-of-band", null, osss, dir, "testSos2d");
     * results = baos.toString(File2.UTF_8);
     * String2.log(results);
     * expected =
     * "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
     * "<om:CompositeObservation xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n" +
     * "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" +
     * "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n" +
     * "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +
     * "  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n" +
     * "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n" +
     * "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n"
     * +
     * "  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n" +
     * "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
     * +
     * "  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n"
     * +
     * "  <gml:name>NDBC Standard Meteorological Buoy Data, network cwwcNDBCMet</gml:name>\n"
     * +
     * "  <gml:boundedBy>\n" +
     * "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" +
     * "      <gml:lowerCorner>32.4 -79.1</gml:lowerCorner>\n" +
     * "      <gml:upperCorner>32.6 -79.08</gml:upperCorner>\n" +
     * "    </gml:Envelope>\n" +
     * "  </gml:boundedBy>\n" +
     * "  <om:samplingTime>\n" +
     * "    <gml:TimePeriod gml:id=\"ST\">\n" +
     * "      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n" +
     * "      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n" +
     * "    </gml:TimePeriod>\n" +
     * "  </om:samplingTime>\n" +
     * "  <om:procedure>\n" +
     * "    <om:Process>\n" +
     * "      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n" +
     * "        <gml:valueComponents>\n" +
     * "          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n" +
     * "          <ioos:ContextArray gml:id=\"StationArray\">\n" +
     * "            <gml:valueComponents>\n" +
     * "              <ioos:CompositeContext gml:id=\"Station1Info\">\n" +
     * "                <gml:valueComponents>\n" +
     * "                  <ioos:StationName>Station - 41004</ioos:StationName>\n" +
     * "                  <ioos:Organization>NOAA NDBC, CoastWatch WCN</ioos:Organization>\n"
     * +
     * "                  <ioos:StationId>urn:ioos:Station:1.0.0.127.cwwcNDBCMet:41004:</ioos:StationId>\n"
     * +
     * "                  <gml:Point gml:id=\"Station1LatLon\">\n" +
     * "                    <gml:pos>-79.09 32.5</gml:pos>\n" +
     * "                  </gml:Point>\n" +
     * "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n"
     * +
     * "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n"
     * +
     * "                  <ioos:Count name=\"Station1NumberOfSensors\">2</ioos:Count>\n"
     * +
     * "                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n" +
     * "                    <gml:valueComponents>\n" +
     * "                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n"
     * +
     * "                        <gml:valueComponents>\n" +
     * "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:atmp</ioos:SensorId>\n"
     * +
     * "                        </gml:valueComponents>\n" +
     * "                      </ioos:CompositeContext>\n" +
     * "                      <ioos:CompositeContext gml:id=\"Station1Sensor2Info\">\n"
     * +
     * "                        <gml:valueComponents>\n" +
     * "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.cwwcNDBCMet:41004:wtmp</ioos:SensorId>\n"
     * +
     * "                        </gml:valueComponents>\n" +
     * "                      </ioos:CompositeContext>\n" +
     * "                    </gml:valueComponents>\n" +
     * "                  </ioos:ContextArray>\n" +
     * "                </gml:valueComponents>\n" +
     * "              </ioos:CompositeContext>\n" +
     * "            </gml:valueComponents>\n" +
     * "          </ioos:ContextArray>\n" +
     * "        </gml:valueComponents>\n" +
     * "      </ioos:CompositeContext>\n" +
     * "    </om:Process>\n" +
     * "  </om:procedure>\n" +
     * "  <om:observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n"
     * +
     * "  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n" +
     * "  <om:result xlink:href=\"http://localhost:8080/cwexperimental/tabledap/cwwcNDBCMet.csv?longitude,latitude,time,station,atmp,wtmp&amp;time&gt;=2008-08-01T00:00:00Z&amp;time&lt;=2008-08-01T01:00:00Z&amp;longitude&gt;=-79.1&amp;longitude&lt;=-79.08&amp;latitude&gt;=32.4&amp;latitude&lt;=32.6\"/>\n"
     * +
     * "</om:CompositeObservation>\n";
     * Test.ensureEqual(results, expected, "\nresults=\n" + results);
     */

    // *** #3: observations for all stations, all vars, and with BBOX (multiple
    // stations)
    // featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
    String sosQuery3 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=cwwcNDBCMet"
            + // short
            "&observedProperty=cwwcNDBCMet"
            + "&responseFormat=text/csv"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z"
            + "&featureOfInterest=BBOX:-79.9,32.4,-79.0,33.0";
    String dapQuery3[] = eddTable.sosQueryToDapQuery(language, null, sosQuery3);
    String2.log("\nsosQuery3=" + sosQuery3 + "\n\ndapQuery3=" + dapQuery3[0]);
    Test.ensureEqual(
        dapQuery3[0],
        "&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z"
            + "&longitude>=-79.9&longitude<=-79.0&latitude>=32.4&latitude<=33.0",
        "");
    String2.log("\n+++ GetObservations with BBOX (multiple stations)");
    writer = new java.io.StringWriter();
    String2.log("query: " + sosQuery3);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery3, "someIPAddress", null, osss, dir, "testSos3");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n"
            + "degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n"
            + "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n"
            + "-79.63, 32.81, 2008-08-01T00:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n"
            + "-79.63, 32.81, 2008-08-01T01:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n"
            + "-79.89, 32.68, 2008-08-01T00:00:00Z, FBIS1, 237, 6.4, 9.0, NaN, NaN, NaN, NaN, 1009.5, 28.7, NaN, 24.8, NaN, NaN, NaN, 5.4, 3.5\n"
            + "-79.89, 32.68, 2008-08-01T01:00:00Z, FBIS1, 208, 1.3, 1.5, NaN, NaN, NaN, NaN, 1010.1, 27.1, NaN, 24.9, NaN, NaN, NaN, 0.6, 1.1\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #4 all stations, no obsProp -> all vars, text/csv
    String sosQuery4 = // no obsProp
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=cwwcNDBCMet"
            + // short
            "&responseFormat=text/csv"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z"
            + "&featureOfInterest=BBOX:-79.9,32.4,-79.0,33.0";
    String dapQuery4[] = eddTable.sosQueryToDapQuery(language, null, sosQuery4);
    String2.log("\nsosQuery4=" + sosQuery4 + "\n\ndapQuery4=" + dapQuery4[0]);
    Test.ensureEqual(
        dapQuery4[0],
        "&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z&longitude>=-79.9&longitude<=-79.0&latitude>=32.4&latitude<=33.0",
        "");
    writer = new java.io.StringWriter();
    String2.log("query: " + sosQuery4);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery4, "someIPAddress", null, osss, dir, "testSos4");
    results = baos.toString(File2.UTF_8);
    expected =
        "longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar, atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n"
            + "degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s, degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26, 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n"
            + "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56, 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n"
            + "-79.63, 32.81, 2008-08-01T00:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n"
            + "-79.63, 32.81, 2008-08-01T01:00:00Z, 41029, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n"
            + "-79.89, 32.68, 2008-08-01T00:00:00Z, FBIS1, 237, 6.4, 9.0, NaN, NaN, NaN, NaN, 1009.5, 28.7, NaN, 24.8, NaN, NaN, NaN, 5.4, 3.5\n"
            + "-79.89, 32.68, 2008-08-01T01:00:00Z, FBIS1, 208, 1.3, 1.5, NaN, NaN, NaN, NaN, 1010.1, 27.1, NaN, 24.9, NaN, NaN, NaN, 0.6, 1.1\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** #5: 1 station, 1 obsProp csv
    String sosQuery5csv =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=41004"
            + // short
            "&observedProperty="
            + fullPhenomenaDictionaryUrl
            + "#wtmp"
            + // long
            "&responseFormat=text/csv"
            + "&eventTime=2008-07-25T00:00:00Z/2008-08-01T00:00:00Z";
    String sosQuery5png =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=41004"
            + // short
            "&observedProperty=wtmp"
            + // short
            "&responseFormat=image/png"
            + "&eventTime=2008-07-25T00:00:00Z/2008-08-01T00:00:00Z";

    String dapQuery5[] = eddTable.sosQueryToDapQuery(language, null, sosQuery5csv);
    String2.log("\nsosQuery5csv=" + sosQuery5csv + "\n\ndapQuery5=" + dapQuery5[0]);
    Test.ensureEqual(
        dapQuery5[0],
        "longitude,latitude,time,station,wtmp&station=%2241004%22"
            + "&time>=2008-07-25T00:00:00Z&time<=2008-08-01T00:00:00Z",
        "");
    String2.log("\n+++ 5csv: GetObservations for 1 station, 1 obsProp\n" + sosQuery5csv);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery5csv, "someIPAddress", null, osss, dir, "testSos5csv");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "longitude, latitude, time, station, wtmp\n"
            + "degrees_east, degrees_north, UTC, , degree_C\n"
            + "-79.09, 32.5, 2008-07-25T00:00:00Z, 41004, 28.0\n"
            + "-79.09, 32.5, 2008-07-25T01:00:00Z, 41004, 27.9\n"
            + "-79.09, 32.5, 2008-07-25T02:00:00Z, 41004, 27.8\n"
            + "-79.09, 32.5, 2008-07-25T03:00:00Z, 41004, 27.6\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "-79.09, 32.5, 2008-07-31T21:00:00Z, 41004, 28.0\n"
            + "-79.09, 32.5, 2008-07-31T22:00:00Z, 41004, 28.0\n"
            + "-79.09, 32.5, 2008-07-31T23:00:00Z, 41004, 27.9\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 27.9\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "\nresults=\n" + results);

    // #5png get png -> time series
    String2.log("\n+++ 5png: GetObservations for 1 station, 1 obsProp\n" + sosQuery5png);
    dapQuery5 = eddTable.sosQueryToDapQuery(language, null, sosQuery5png);
    String2.log("\nsosQuery5png=" + sosQuery5png + "\n\ndapQuery5=" + dapQuery5[0]);
    Test.ensureEqual(
        dapQuery5[0],
        "time,wtmp&station=%2241004%22"
            + "&time>=2008-07-25T00:00:00Z&time<=2008-08-01T00:00:00Z"
            + "&.draw=linesAndMarkers&.marker=5|4&.color=0xFF9900",
        "");
    String dapQuery = eddTable.sosQueryToDapQuery(language, null, sosQuery5png)[0];
    fileName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            dapQuery,
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_testSos5png",
            ".png");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + fileName);

    // *** #6 all stations, 1 obsProp,
    String sosQuery6csv = // no obsProp
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=cwwcNDBCMet"
            + // short
            "&observedProperty=wtmp"
            + "&responseFormat=text/csv"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
    String dapQuery6[] = eddTable.sosQueryToDapQuery(language, null, sosQuery6csv);
    String2.log("\nsosQuery6csv=" + sosQuery6csv + "\n\ndapQuery6csv=" + dapQuery6[0]);
    Test.ensureEqual(
        dapQuery6[0],
        "longitude,latitude,time,station,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z",
        "");
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery6csv, "someIPAddress", null, osss, dir, "testSos6csv");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        "longitude, latitude, time, station, wtmp\n"
            + "degrees_east, degrees_north, UTC, , degree_C\n"
            + "-85.38, -19.62, 2008-08-01T00:00:00Z, 32012, NaN\n"
            + "-85.38, -19.62, 2008-08-01T01:00:00Z, 32012, NaN\n"
            + "-72.66, 34.68, 2008-08-01T00:00:00Z, 41001, NaN\n"
            + "-72.66, 34.68, 2008-08-01T01:00:00Z, 41001, NaN\n"
            + "-75.35, 32.31, 2008-08-01T00:00:00Z, 41002, NaN\n"
            + "-75.35, 32.31, 2008-08-01T01:00:00Z, 41002, NaN\n"
            + "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 27.9\n"
            + "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 27.9\n"
            + "-80.87, 31.4, 2008-08-01T00:00:00Z, 41008, 27.1\n"
            + "-80.87, 31.4, 2008-08-01T01:00:00Z, 41008, 27.0\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "-76.48, 37.23, 2008-08-01T00:00:00Z, YKTV2, 28.2\n"
            + "-76.48, 37.23, 2008-08-01T01:00:00Z, YKTV2, 28.3\n"
            + "-76.7125, 37.4142, 2008-08-01T00:00:00Z, YRSV2, NaN\n"
            + "-76.7125, 37.4142, 2008-08-01T01:00:00Z, YRSV2, NaN\n";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "\nresults=\n" + results);

    String sosQuery6png = // no obsProp
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=cwwcNDBCMet"
            + // short
            "&observedProperty=wtmp"
            + "&responseFormat=image/png"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
    dapQuery6 = eddTable.sosQueryToDapQuery(language, null, sosQuery6png);
    String2.log("\nsosQuery6png=" + sosQuery6png + "\n\ndapQuery6png=" + dapQuery6[0]);
    Test.ensureEqual(
        dapQuery6[0],
        "longitude,latitude,wtmp&time>=2008-08-01T00:00:00Z&time<=2008-08-01T01:00:00Z&.draw=markers&.marker=5|4",
        "");
    fileName =
        eddTable.makeNewFileForDapQuery(
            language,
            null,
            null,
            dapQuery6[0],
            EDStatic.fullTestCacheDirectory,
            eddTable.className() + "_testSos6png",
            ".png");
    Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + fileName);
    /*  */

  }

  /** Test SOS server using ndbcSosCurrents. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testSosCurrents() throws Throwable {
    String2.log("\n*** EDDTable.testSosCurrents()");
    EDDTable eddTable = (EDDTable) EDDTable.oneFromDatasetsXml(null, "ndbcSosCurrents");
    String dir = EDStatic.fullTestCacheDirectory;
    String sosQuery, fileName, results, expected;
    int language = 0;
    java.io.StringWriter writer;
    ByteArrayOutputStream baos;
    OutputStreamSourceSimple osss;

    // GetCapabilities
    String2.log("\n+++ GetCapabilities");
    writer = new java.io.StringWriter();
    HashMap<String, String> queryMap =
        EDD.userQueryHashMap(
            "seRvIcE=SOS&ReQueSt=GetCapabilities&sEctIons=gibberish,All",
            true); // true=names toLowerCase
    eddTable.sosGetCapabilities(language, queryMap, writer, null);
    results = writer.toString();
    String2.log(results.substring(0, 7000));
    String2.log("\n...\n");
    String2.log(results.substring(45000, 55000));
    String2.log("\n...\n");
    String2.log(results.substring(results.length() - 3000));

    // *** observations for 1 station (via bbox), 2 times
    String endOfRequest = "sos/cwwcNDBCMet/get"; // is that right? does it matter here?
    String sosQuery1 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:network:1.0.0.127.ndbcSosCurrents:ndbcSosCurrents"
            + "&observedProperty=ndbcSosCurrents"
            + "&responseFormat=text/csv"
            + "&eventTime=2008-06-01T14:00:00Z/2008-06-01T14:30:00Z"
            + "&featureOfInterest=BBOX:-88,29.1,-87.9,29.2"; // <min_lon>,<min_lat>,<max_lon>,<max_lat>

    String2.log("\n+++ GetObservations for 1 station (via BBOX)\n" + sosQuery1);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery1, "someIPAddress", null, osss, dir, "testSosCurBB");
    /*
     * from eddTableSos.testNdbcSosCurrents
     * "&longitude=-87.94&latitude>=29.1&latitude<29.2&time>=2008-06-01T14:00&time<=2008-06-01T14:30",
     * "station_id, longitude, latitude, altitude, time, CurrentDirection, CurrentSpeed\n"
     * +
     * ", degrees_east, degrees_north, m, UTC, degrees_true, cm s-1\n" +
     * "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -56.8, 2008-06-01T14:03:00Z, 83, 30.2\n"
     * +
     * "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -88.8, 2008-06-01T14:03:00Z, 96, 40.5\n"
     * +
     * "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -120.8, 2008-06-01T14:03:00Z, 96, 40.7\n"
     * +
     * "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -152.8, 2008-06-01T14:03:00Z, 96, 35.3\n"
     * +
     * "urn:ioos:station:noaa.nws.ndbc:42376:, -87.94, 29.16, -184.8, 2008-06-01T14:03:00Z, 89, 31.9\n"
     * +
     */
    results = baos.toString(File2.UTF_8);
    // String2.log(results);
    expected =
        "longitude, latitude, station_id, altitude, time, CurrentDirection, CurrentSpeed\n"
            + "degrees_east, degrees_north, , m, UTC, degrees_true, cm s-1\n"
            + "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -56.8, 2008-06-01T14:03:00Z, 83, 30.2\n"
            + "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -88.8, 2008-06-01T14:03:00Z, 96, 40.5\n"
            + "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -120.8, 2008-06-01T14:03:00Z, 96, 40.7\n"
            + "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -152.8, 2008-06-01T14:03:00Z, 96, 35.3\n"
            + "-87.94, 29.16, urn:ioos:station:noaa.nws.ndbc:42376:, -184.8, 2008-06-01T14:03:00Z, 89, 31.9\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // *** same query, but via offering=(station) instead of via bbox
    // observations for 1 station, 2 times
    String sosQuery2 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:Station:1.0.0.127.ndbcSosCurrents:42376:"
            + "&observedProperty=ndbcSosCurrents"
            + "&responseFormat=text/csv"
            + "&eventTime=2008-06-01T14:00:00Z/2008-06-01T14:30:00Z";

    String2.log("\n+++ GetObservations for 1 station (via offering=(station))\n" + sosQuery2);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery2, "someIPAddress", null, osss, dir, "testSosCurSta");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    // expected = same data
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // *** same query, but return as sos xml
    // observations for 1 station, 2 times
    String sosQuery3 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=42376"
            + // and switch to short offering name
            "&observedProperty=ndbcSosCurrents"
            + "&responseFormat="
            + SSR.minimalPercentEncode("text/xml;schema=\"ioos/0.6.1\"")
            + "&eventTime=2008-06-01T14:00:00Z/2008-06-01T14:30:00Z";

    String2.log("\n+++ GetObservations for 1 station (via offering=(station))\n" + sosQuery3);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery3, "someIPAddress", null, osss, dir, "testSosCurSta2");
    results = baos.toString(File2.UTF_8);
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:CompositeObservation xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 http://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n"
            + "  gml:id=\"ndbcSosCurrentsTimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>ndbcSosCurrents observations at a series of times</gml:description>\n"
            + "  <gml:name>Buoy Data from the NOAA NDBC SOS Server - Currents, Station 42376</gml:name>\n"
            + "  <gml:boundedBy>\n"
            + "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "      <gml:lowerCorner>29.16 -87.94</gml:lowerCorner>\n"
            + "      <gml:upperCorner>29.16 -87.94</gml:upperCorner>\n"
            + "    </gml:Envelope>\n"
            + "  </gml:boundedBy>\n"
            + "  <om:samplingTime>\n"
            + "    <gml:TimePeriod gml:id=\"ST\">\n"
            + "      <gml:beginPosition>2008-06-01T14:03:00Z</gml:beginPosition>\n"
            + "      <gml:endPosition>2008-06-01T14:23:00Z</gml:endPosition>\n"
            + "    </gml:TimePeriod>\n"
            + "  </om:samplingTime>\n"
            + "  <om:procedure>\n"
            + "    <om:Process>\n"
            + "      <ioos:CompositeContext gml:id=\"SensorMetadata\">\n"
            + "        <gml:valueComponents>\n"
            + "          <ioos:Count name=\"NumberOfStations\">1</ioos:Count>\n"
            + "          <ioos:ContextArray gml:id=\"StationArray\">\n"
            + "            <gml:valueComponents>\n"
            + "              <ioos:CompositeContext gml:id=\"Station1Info\">\n"
            + "                <gml:valueComponents>\n"
            + "                  <ioos:StationName>Station - urn:ioos:station:noaa.nws.ndbc:42376:</ioos:StationName>\n"
            + "                  <ioos:Organization>NOAA NDBC</ioos:Organization>\n"
            + "                  <ioos:StationId>urn:ioos:Station:1.0.0.127.ndbcSosCurrents:urn:ioos:station:noaa.nws.ndbc:42376:</ioos:StationId>\n"
            + "                  <gml:Point gml:id=\"Station1LatLon\">\n"
            + "                    <gml:pos>29.16 -87.94</gml:pos>\n"
            + "                  </gml:Point>\n"
            + "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n"
            + "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n"
            + "                  <ioos:Count name=\"Station1NumberOfSensors\">1</ioos:Count>\n"
            + "                  <ioos:ContextArray gml:id=\"Station1SensorArray\">\n"
            + "                    <gml:valueComponents>\n"
            + "                      <ioos:CompositeContext gml:id=\"Station1Sensor1Info\">\n"
            + "                        <gml:valueComponents>\n"
            + "                          <ioos:SensorId>urn:ioos:sensor:1.0.0.127.ndbcSosCurrents:urn:ioos:station:noaa.nws.ndbc:42376:ndbcSosCurrents</ioos:SensorId>\n"
            + "                        </gml:valueComponents>\n"
            + "                      </ioos:CompositeContext>\n"
            + "                    </gml:valueComponents>\n"
            + "                  </ioos:ContextArray>\n"
            + "                </gml:valueComponents>\n"
            + "              </ioos:CompositeContext>\n"
            + "            </gml:valueComponents>\n"
            + "          </ioos:ContextArray>\n"
            + "        </gml:valueComponents>\n"
            + "      </ioos:CompositeContext>\n"
            + "    </om:Process>\n"
            + "  </om:procedure>\n"
            + "  <om:observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/ndbcSosCurrents/phenomenaDictionary.xml#ndbcSosCurrents\"/>\n"
            + "  <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n"
            + "  <om:result>\n"
            + "    <ioos:Composite gml:id=\"ndbcSosCurrentsPointCollectionTimeSeriesDataObservations\">\n"
            + "      <gml:valueComponents>\n"
            + "        <ioos:Count name=\"NumberOfObservationsPoints\">1</ioos:Count>\n"
            + "        <ioos:Array gml:id=\"ndbcSosCurrentsPointCollectionTimeSeries\">\n"
            + "          <gml:valueComponents>\n"
            + "            <ioos:Composite gml:id=\"Station1TimeSeriesRecord\">\n"
            + "              <gml:valueComponents>\n"
            + "                <ioos:Count name=\"Station1NumberOfObservationsTimes\">2</ioos:Count>\n"
            + "                <ioos:Array gml:id=\"Station1ProfileTimeSeries\">\n"
            + "                  <gml:valueComponents>\n"
            + "                    <ioos:Composite gml:id=\"Station1T1Profile\">\n"
            + "                      <gml:valueComponents>\n"
            + "                        <ioos:CompositeContext gml:id=\"Station1T1ObservationConditions\" processDef=\"#Station1Info\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <gml:TimeInstant gml:id=\"Station1T1Time\">\n"
            + "                              <gml:timePosition>2008-06-01T14:03:00Z</gml:timePosition>\n"
            + "                            </gml:TimeInstant>\n"
            + "                          </gml:valueComponents>\n"
            + "                        </ioos:CompositeContext>\n"
            + "                        <ioos:Count name=\"Station1T1NumberOfBinObservations\">32</ioos:Count>\n"
            + "                        <ioos:ValueArray gml:id=\"Station1T1ProfileObservations\">\n"
            + "                          <gml:valueComponents>\n"
            + "                            <ioos:CompositeValue gml:id=\"Station1T1Bin1Obs\" processDef=\"#Station1Sensor1Info\">\n"
            + "                              <gml:valueComponents>\n"
            + "                                <ioos:Context name=\"altitude\" uom=\"m\">-1048.8</ioos:Context>\n"
            + "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">0</ioos:Quantity>\n"
            + "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">0.0</ioos:Quantity>\n"
            + "                              </gml:valueComponents>\n"
            + "                            </ioos:CompositeValue>\n"
            + "                            <ioos:CompositeValue gml:id=\"Station1T1Bin2Obs\" processDef=\"#Station1Sensor1Info\">\n"
            + "                              <gml:valueComponents>\n"
            + "                                <ioos:Context name=\"altitude\" uom=\"m\">-1016.8</ioos:Context>\n"
            + "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">0</ioos:Quantity>\n"
            + "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">0.0</ioos:Quantity>\n"
            + "                              </gml:valueComponents>\n"
            + "                            </ioos:CompositeValue>\n"
            + "                            <ioos:CompositeValue gml:id=\"Station1T1Bin3Obs\" processDef=\"#Station1Sensor1Info\">\n"
            + "                              <gml:valueComponents>\n"
            + "                                <ioos:Context name=\"altitude\" uom=\"m\">-984.8</ioos:Context>\n"
            + "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">0</ioos:Quantity>\n"
            + "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">0.0</ioos:Quantity>\n"
            + "                              </gml:valueComponents>\n"
            + "                            </ioos:CompositeValue>\n"
            + "                            <ioos:CompositeValue gml:id=\"Station1T1Bin4Obs\" processDef=\"#Station1Sensor1Info\">\n"
            + "                              <gml:valueComponents>\n"
            + "                                <ioos:Context name=\"altitude\" uom=\"m\">-952.8</ioos:Context>\n"
            + "                                <ioos:Quantity name=\"CurrentDirection\" uom=\"degrees_true\">75</ioos:Quantity>\n"
            + "                                <ioos:Quantity name=\"CurrentSpeed\" uom=\"cm s-1\">4.3</ioos:Quantity>\n"
            + "                              </gml:valueComponents>\n"
            + "                            </ioos:CompositeValue>\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
  }

  /**
   * Test SOS server using gomoosBuoy. This caused an internal error in EDDTableFromSos. So this
   * helped me fix the bug.
   */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testSosGomoos() throws Throwable {
    String2.log("\n*** EDDTable.testSosGomoos()");
    EDDTable eddTable = (EDDTable) EDDTable.oneFromDatasetsXml(null, "gomoosBuoy");
    String dir = EDStatic.fullTestCacheDirectory;
    String sosQuery, fileName, results, expected;
    int language = 0;
    java.io.StringWriter writer;
    ByteArrayOutputStream baos;
    OutputStreamSourceSimple osss;
    String endOfRequest = "sos/cwwcNDBCMet/get"; // is that right? does it matter here?

    // *** observations for 1 station
    // request that caused error:
    /// erddap2/sos/gomoosBuoy/server?service=SOS&version=1.0.0&request=GetObservation
    // &offering=urn:ioos:Station:noaa.pfeg.coastwatch.gomoosBuoy::A01
    // &observedProperty=gomoosBuoy&eventTime=2009-08-25T00:00:00Z/2009-09-01T18:23:51Z
    // &responseFormat=text/xml;schema%3D%22ioos/0.6.1%22
    String sosQuery1 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:Station:1.0.0.127.gomoosBuoy::A01"
            + "&observedProperty=gomoosBuoy"
            + "&responseFormat=text/csv"
            + "&eventTime=2009-08-25T00:00:00Z/2009-09-01T18:23:51Z";

    String2.log("\n+++ GetObservations for 1 station\n" + sosQuery1);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery1, "someIPAddress", null, osss, dir, "testGomoos");
    results = baos.toString(File2.UTF_8);
    // String2.log(results);
    expected =
        "longitude (deg{east}), latitude (deg{north}), station_id, altitude (m), time (UTC), air_temperature (Cel), chlorophyll (mg.m-3), direction_of_sea_water_velocity (deg{true}), dominant_wave_period (s), sea_level_pressure (mbar), sea_water_density (kg.m-3), sea_water_electrical_conductivity (S.m-1), sea_water_salinity ({psu}), sea_water_speed (cm.s-1), sea_water_temperature (Cel), wave_height (m), visibility_in_air (m), wind_from_direction (deg{true}), wind_gust (m.s-1), wind_speed (m.s-1)\n"
            + "-70.5680705627165, 42.5221700538877, A01, 4.0, 2009-08-25T00:00:00Z, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 1.69000005722046, 3.05599999427795, 1.46200001239777\n"
            + "-70.5680705627165, 42.5221700538877, A01, 3.0, 2009-08-25T00:00:00Z, 20.9699993133545, NaN, NaN, NaN, 1016.49780273438, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN\n"
            + "-70.5680705627165, 42.5221700538877, A01, 0.0, 2009-08-25T00:00:00Z, NaN, NaN, NaN, 8.0, NaN, NaN, NaN, NaN, NaN, NaN, 1.00613415, NaN, NaN, NaN, NaN\n"
            + "-70.5680705627165, 42.5221700538877, A01, -1.0, 2009-08-25T00:00:00Z, NaN, NaN, NaN, NaN, NaN, 21.1049213409424, 43.431999206543, 30.5694179534912, NaN, 21.0799999237061, NaN, NaN, NaN, NaN, NaN\n";
    Test.ensureEqual(
        results.substring(0, expected.length()),
        expected,
        "\nresults=\n" + results.substring(0, Math.min(5000, results.length())));
  }

  /** Test the Oostethys-style SOS data response using cwwcNDBCMet. */
  @org.junit.jupiter.api.Test
  @TagExternalOther
  void testSosOostethys() throws Throwable {
    String2.log("\n*** EDDTable.testSosOostethys()");
    EDDTable eddTable = (EDDTable) EDDTable.oneFromDatasetsXml(null, "cwwcNDBCMet");
    String dir = EDStatic.fullTestCacheDirectory;
    String sosQuery, fileName, results, expected;
    int language = 0;
    java.io.StringWriter writer;
    ByteArrayOutputStream baos;
    OutputStreamSourceSimple osss;
    String endOfRequest = "sos/cwwcNDBCMet/get"; // is that right? does it matter here?

    // *** observations for 1 station, all vars CSV response
    String sosQuery1 =
        "service=SOS&version=1.0.0&request=GetObservation"
            + "&offering=urn:ioos:Station:1.0.0.127.cwwcNDBCMet::41004"
            + "&observedProperty=cwwcNDBCMet"
            + "&responseFormat=application/com-xml"
            + "&eventTime=2008-08-01T00:00:00Z/2008-08-01T01:00:00Z";
    String2.log("\n+++ GetObservations for 1 station \n" + sosQuery1);
    baos = new ByteArrayOutputStream();
    osss = new OutputStreamSourceSimple(baos);
    eddTable.sosGetObservation(
        language, endOfRequest, sosQuery1, "someIPAddress", null, osss, dir, "testSos1Sta");
    results = baos.toString(File2.UTF_8);
    String2.log(results);
    expected =
        // "longitude, latitude, time, station, wd, wspd, gst, wvht, dpd, apd, mwd, bar,
        // atmp, wtmp, dewp, vis, ptdy, tide, wspu, wspv\n" +
        // "degrees_east, degrees_north, UTC, , degrees_true, m s-1, m s-1, m, s, s,
        // degrees_true, hPa, degree_C, degree_C, degree_C, km, hPa, m, m s-1, m s-1\n"
        // +
        // "-79.09, 32.5, 2008-08-01T00:00:00Z, 41004, 225, 10.9, 14.0, 1.66, 5.26,
        // 4.17, NaN, 1007.6, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.7, 7.7\n" +
        // "-79.09, 32.5, 2008-08-01T01:00:00Z, 41004, 229, 10.1, 12.6, 1.68, 5.56,
        // 4.36, NaN, 1008.0, 27.8, 27.9, NaN, NaN, NaN, NaN, 7.6, 6.6\n";
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:Observation\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/0\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/om ../../../../../../ows4/schema0/swe/branches/swe-ows4-demo/om/current/commonObservation.xsd\"\n"
            + "  gml:id=\"cwwcNDBCMetTimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>cwwcNDBCMet observations at a series of times</gml:description>\n"
            + "  <gml:name>NDBC Standard Meteorological Buoy Data, Station 41004</gml:name>\n"
            + "  <gml:location>\n"
            + "    <gml:Point gml:id=\"OBSERVATION_LOCATION\" srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "      <gml:coordinates>32.5 -79.09</gml:coordinates>\n"
            + "    </gml:Point>\n"
            + "  </gml:location>\n"
            + "  <om:time>\n"
            + "    <gml:TimePeriod gml:id=\"DATA_TIME\">\n"
            + "      <gml:beginPosition>2008-08-01T00:00:00Z</gml:beginPosition>\n"
            + "      <gml:endPosition>2008-08-01T01:00:00Z</gml:endPosition>\n"
            + "    </gml:TimePeriod>\n"
            + "  </om:time>\n"
            + "    <om:procedure xlink:href=\"urn:ioos:sensor:1.0.0.127.cwwcNDBCMet::41004:cwwcNDBCMet\"/>\n"
            + "    <om:observedProperty xlink:href=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#cwwcNDBCMet\"/>\n"
            + "    <om:featureOfInterest xlink:href=\"urn:cgi:Feature:CGI:EarthOcean\"/>\n"
            + "    <om:resultDefinition>\n"
            + "        <swe:DataBlockDefinition>\n"
            + "            <swe:components name=\"cwwcNDBCMetDataFor41004\">\n"
            + "                <swe:DataRecord>\n"
            + "                    <swe:field name=\"time\">\n"
            + "                        <swe:Time definition=\"urn:ogc:phenomenon:time:iso8601\"/>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"latitude\">\n"
            + "                        <swe:Quantity definition=\"urn:ogc:phenomenon:latitude:wgs84\">\n"
            + "                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"longitude\">\n"
            + "                        <swe:Quantity definition=\"urn:ogc:phenomenon:longitude:wgs84\">\n"
            + "                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"wd\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wd\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#degrees_true\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"wspd\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspd\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"gst\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#gst\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"wvht\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wvht\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#m\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"dpd\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dpd\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#s\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"apd\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#apd\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#s\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"mwd\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#mwd\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#degrees_true\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"bar\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#bar\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#hPa\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"atmp\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#atmp\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#degree_C\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"wtmp\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wtmp\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#degree_C\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"dewp\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#dewp\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#degree_C\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"vis\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#vis\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#km\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"ptdy\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#ptdy\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#hPa\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"tide\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#tide\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#m\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"wspu\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspu\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"wspv\">\n"
            + "                        <swe:Quantity definition=\"http://localhost:8080/cwexperimental/sos/cwwcNDBCMet/phenomenaDictionary.xml#wspv\">\n"
            + "                            <swe:uom xlink:href=\"urn:erddap.def:units#m s-1\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                </swe:DataRecord>\n"
            + "            </swe:components>\n"
            + "            <swe:encoding>\n"
            + "                <swe:AsciiBlock tokenSeparator=\",\" blockSeparator=\" \" decimalSeparator=\".\"/>\n"
            + "            </swe:encoding>\n"
            + "        </swe:DataBlockDefinition>\n"
            + "    </om:resultDefinition>\n"
            + "    <om:result>2008-08-01T00:00:00Z,32.5,-79.09,225,10.9,14.0,1.66,5.26,4.17,,1007.6,27.8,27.9,,,,,7.7,7.7 "
            + "2008-08-01T01:00:00Z,32.5,-79.09,229,10.1,12.6,1.68,5.56,4.36,,1008.0,27.8,27.9,,,,,7.6,6.6</om:result>\n"
            + "</om:Observation>\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);
  }

  /** Test characters. */
  @org.junit.jupiter.api.Test
  void testCharacters() throws Throwable {
    EDDTable eddTable = (EDDTable) EDDTestDataset.gettest_chars();
    String dir = EDStatic.fullTestCacheDirectory;
    String results, expected;
    int language = 0;

    // .csv for one lat,lon,time
    String userDapQuery = "row%2Ccharacters";
    String tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddTable.className() + "_1Station", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "row,characters\n"
            + //
            ",\n"
            + //
            "1,\"0x20:  , 0x21: !, 0x22: \"\", 0x23: #, 0x24: $, 0x25: %, 0x26: &, 0x27: ', 0x28: (, 0x29: ), 0x2a: *, 0x2b: +, 0x2c: ,, 0x2d: -, 0x2e: ., 0x2f: /, 0x30: 0, 0x31: 1, 0x32: 2, 0x33: 3, 0x34: 4, 0x35: 5, 0x36: 6, 0x37: 7, 0x38: 8, 0x39: 9, 0x3a: :, 0x3c: <, 0x3d: =, 0x3e: >, 0x3f: ?, 0x40: @, 0x41: A, 0x42: B, 0x43: C, 0x44: D, 0x45: E, 0x46: F, 0x47: G, 0x48: H, 0x49: I, 0x4a: J, 0x4b: K, 0x4c: L, 0x4d: M, 0x4e: N, 0x4f: O, 0x50: P, 0x51: Q, 0x52: R, 0x53: S, 0x54: T, 0x55: U, 0x56: V, 0x57: W, 0x58: X, 0x59: Y, 0x5a: Z, 0x5b: [, 0x5c: \\\\, 0x5d: ], 0x5e: ^, 0x5f: _, 0x60: `, 0x61: a, 0x62: b, 0x63: c, 0x64: d, 0x65: e, 0x66: f, 0x67: g, 0x68: h, 0x69: i, 0x6a: j, 0x6b: k, 0x6c: l, 0x6d: m, 0x6e: n, 0x6f: o, 0x70: p, 0x71: q, 0x72: r, 0x73: s, 0x74: t, 0x75: u, 0x76: v, 0x77: w, 0x78: x, 0x79: y, 0x7a: z, 0x7b: {, 0x7c: |, 0x7d: }, 0x7e: ~, 0x7f: \\u007f, 0xa0: \\u00a0, 0xa1: \\u00a1, 0xa2: \\u00a2, 0xa3: \\u00a3, 0xa4: \\u00a4, 0xa5: \\u00a5, 0xa6: \\u00a6, 0xa7: \\u00a7, 0xa8: \\u00a8, 0xa9: \\u00a9, 0xaa: \\u00aa, 0xab: \\u00ab, 0xac: \\u00ac, 0xad: \\u00ad, 0xae: \\u00ae, 0xaf: \\u00af, 0xb0: \\u00b0, 0xb1: \\u00b1, 0xb2: \\u00b2, 0xb3: \\u00b3, 0xb4: \\u00b4, 0xb5: \\u00b5, 0xb6: \\u00b6, 0xb7: \\u00b7, 0xb8: \\u00b8, 0xb9: \\u00b9, 0xba: \\u00ba, 0xbb: \\u00bb, 0xbc: \\u00bc, 0xbd: \\u00bd, 0xbe: \\u00be, 0xbf: \\u00bf, 0xc0: \\u00c0, 0xc1: \\u00c1, 0xc2: \\u00c2, 0xc3: \\u00c3, 0xc4: \\u00c4, 0xc5: \\u00c5, 0xc6: \\u00c6, 0xc7: \\u00c7, 0xc8: \\u00c8, 0xc9: \\u00c9, 0xca: \\u00ca, 0xcb: \\u00cb, 0xcc: \\u00cc, 0xcd: \\u00cd, 0xce: \\u00ce, 0xcf: \\u00cf, 0xd0: \\u00d0, 0xd1: \\u00d1, 0xd2: \\u00d2, 0xd3: \\u00d3, 0xd4: \\u00d4, 0xd5: \\u00d5, 0xd6: \\u00d6, 0xd7: \\u00d7, 0xd8: \\u00d8, 0xd9: \\u00d9, 0xda: \\u00da, 0xdb: \\u00db, 0xdc: \\u00dc, 0xdd: \\u00dd, 0xde: \\u00de, 0xdf: \\u00df, 0xe0: \\u00e0, 0xe1: \\u00e1, 0xe2: \\u00e2, 0xe3: \\u00e3, 0xe4: \\u00e4, 0xe5: \\u00e5, 0xe6: \\u00e6, 0xe7: \\u00e7, 0xe8: \\u00e8, 0xe9: \\u00e9, 0xea: \\u00ea, 0xeb: \\u00eb, 0xec: \\u00ec, 0xed: \\u00ed, 0xee: \\u00ee, 0xef: \\u00ef, 0xf0: \\u00f0, 0xf1: \\u00f1, 0xf2: \\u00f2, 0xf3: \\u00f3, 0xf4: \\u00f4, 0xf5: \\u00f5, 0xf6: \\u00f6, 0xf7: \\u00f7, 0xf8: \\u00f8, 0xf9: \\u00f9, 0xfa: \\u00fa, 0xfb: \\u00fb, 0xfc: \\u00fc, 0xfd: \\u00fd, 0xfe: \\u00fe, 0xff: \\u00ff\"\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
  }
}
