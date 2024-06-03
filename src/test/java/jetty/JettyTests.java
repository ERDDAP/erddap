package jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.StringComparatorIgnoreCase;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Image2Tests;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import dods.dap.DAS;
import dods.dap.DConnect;
import dods.dap.DDS;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.pointdata.TableTests;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridFromDap;
import gov.noaa.pfel.erddap.dataset.EDDGridFromErddap;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableCopy;
import gov.noaa.pfel.erddap.dataset.EDDTableFromDapSequence;
import gov.noaa.pfel.erddap.dataset.EDDTableFromNcFiles;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import org.eclipse.jetty.ee10.webapp.WebAppContext;

import tags.TagImageComparison;
import tags.TagJetty;
import testDataset.EDDTestDataset;
import testDataset.Initialization;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;

class JettyTests {

  @TempDir
  private static Path TEMP_DIR;

  private static Server server;
  private static Integer PORT = 8080;

  @BeforeAll
  public static void setUp() throws Throwable {
    Initialization.edStatic();
    EDDTestDataset.generateDatasetsXml();

    server = new Server(PORT);

    WebAppContext context = new WebAppContext();
    ResourceFactory resourceFactory = ResourceFactory.of(context);
    Resource baseResource = resourceFactory
        .newResource(Path.of(System.getProperty("user.dir")).toAbsolutePath().toUri());
    context.setBaseResource(baseResource);
    context.setContextPath("/");
    context.setParentLoaderPriority(true);
    server.setHandler(context);

    server.start();
    // Make a request of the server to make sure it starts loading the datasets
    SSR.getUrlResponseStringUnchanged("http://localhost:" + PORT + "/erddap");

    Thread.sleep(10 * 60 * 1000);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    server.stop();
  }

  /** Test the metadata */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testMetadataIso19115() throws Exception {
    String results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/iso19115/xml/erdMWchla1day_iso19115.xml");
    String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
        "<gmi:MI_Metadata  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
        "  xsi:schemaLocation=\"https://www.isotc211.org/2005/gmi https://data.noaa.gov/resources/iso19139/schema.xsd\"\n"
        + //
        "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" + //
        "  xmlns:gco=\"http://www.isotc211.org/2005/gco\"\n" + //
        "  xmlns:gmd=\"http://www.isotc211.org/2005/gmd\"\n" + //
        "  xmlns:gmx=\"http://www.isotc211.org/2005/gmx\"\n" + //
        "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n" + //
        "  xmlns:gss=\"http://www.isotc211.org/2005/gss\"\n" + //
        "  xmlns:gts=\"http://www.isotc211.org/2005/gts\"\n" + //
        "  xmlns:gsr=\"http://www.isotc211.org/2005/gsr\"\n" + //
        "  xmlns:gmi=\"http://www.isotc211.org/2005/gmi\"\n" + //
        "  xmlns:srv=\"http://www.isotc211.org/2005/srv\">\n" + //
        "  <gmd:fileIdentifier>\n" + //
        "    <gco:CharacterString>erdMWchla1day</gco:CharacterString>\n" + //
        "  </gmd:fileIdentifier>\n" + //
        "  <gmd:language>\n" + //
        "    <gmd:LanguageCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:LanguageCode\" codeListValue=\"eng\">eng</gmd:LanguageCode>\n"
        + //
        "  </gmd:language>\n" + //
        "  <gmd:characterSet>\n" + //
        "    <gmd:MD_CharacterSetCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CharacterSetCode\" codeListValue=\"UTF8\">UTF8</gmd:MD_CharacterSetCode>\n"
        + //
        "  </gmd:characterSet>\n" + //
        "  <gmd:hierarchyLevel>\n" + //
        "    <gmd:MD_ScopeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n"
        + //
        "  </gmd:hierarchyLevel>\n" + //
        "  <gmd:hierarchyLevel>\n" + //
        "    <gmd:MD_ScopeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"service\">service</gmd:MD_ScopeCode>\n"
        + //
        "  </gmd:hierarchyLevel>\n" + //
        "  <gmd:contact>\n" + //
        "    <gmd:CI_ResponsibleParty>\n" + //
        "      <gmd:individualName>\n" + //
        "        <gco:CharacterString>ERDDAP Jetty Developer</gco:CharacterString>\n" + //
        "      </gmd:individualName>\n" + //
        "      <gmd:organisationName>\n" + //
        "        <gco:CharacterString>ERDDAP Jetty Install</gco:CharacterString>\n" + //
        "      </gmd:organisationName>\n" + //
        "      <gmd:contactInfo>\n" + //
        "        <gmd:CI_Contact>\n" + //
        "          <gmd:phone>\n" + //
        "            <gmd:CI_Telephone>\n" + //
        "              <gmd:voice>\n" + //
        "                <gco:CharacterString>555-555-5555</gco:CharacterString>\n" + //
        "              </gmd:voice>\n" + //
        "            </gmd:CI_Telephone>\n" + //
        "          </gmd:phone>\n" + //
        "          <gmd:address>\n" + //
        "            <gmd:CI_Address>\n" + //
        "              <gmd:deliveryPoint>\n" + //
        "                <gco:CharacterString>123 Irrelevant St.</gco:CharacterString>\n" + //
        "              </gmd:deliveryPoint>\n" + //
        "              <gmd:city>\n" + //
        "                <gco:CharacterString>Nowhere</gco:CharacterString>\n" + //
        "              </gmd:city>\n" + //
        "              <gmd:administrativeArea>\n" + //
        "                <gco:CharacterString>AK</gco:CharacterString>\n" + //
        "              </gmd:administrativeArea>\n" + //
        "              <gmd:postalCode>\n" + //
        "                <gco:CharacterString>99504</gco:CharacterString>\n" + //
        "              </gmd:postalCode>\n" + //
        "              <gmd:country>\n" + //
        "                <gco:CharacterString>USA</gco:CharacterString>\n" + //
        "              </gmd:country>\n" + //
        "              <gmd:electronicMailAddress>\n" + //
        "                <gco:CharacterString>nobody@example.com</gco:CharacterString>\n" + //
        "              </gmd:electronicMailAddress>\n" + //
        "            </gmd:CI_Address>\n" + //
        "          </gmd:address>\n" + //
        "        </gmd:CI_Contact>\n" + //
        "      </gmd:contactInfo>\n" + //
        "      <gmd:role>\n" + //
        "        <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n"
        + //
        "      </gmd:role>\n" + //
        "    </gmd:CI_ResponsibleParty>\n" + //
        "  </gmd:contact>\n" + //
        "  <gmd:dateStamp>\n" + //
        "    <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "  </gmd:dateStamp>\n" + //
        "  <gmd:metadataStandardName>\n" + //
        "    <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for Imagery and Gridded Data</gco:CharacterString>\n"
        + //
        "  </gmd:metadataStandardName>\n" + //
        "  <gmd:metadataStandardVersion>\n" + //
        "    <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>\n" + //
        "  </gmd:metadataStandardVersion>\n" + //
        "  <gmd:spatialRepresentationInfo>\n" + //
        "    <gmd:MD_GridSpatialRepresentation>\n" + //
        "      <gmd:numberOfDimensions>\n" + //
        "        <gco:Integer>NUMBER</gco:Integer>\n" + //
        "      </gmd:numberOfDimensions>\n" + //
        "      <gmd:axisDimensionProperties>\n" + //
        "        <gmd:MD_Dimension>\n" + //
        "          <gmd:dimensionName>\n" + //
        "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"column\">column</gmd:MD_DimensionNameTypeCode>\n"
        + //
        "          </gmd:dimensionName>\n" + //
        "          <gmd:dimensionSize>\n" + //
        "            <gco:Integer>NUMBER</gco:Integer>\n" + //
        "          </gmd:dimensionSize>\n" + //
        "          <gmd:resolution>\n" + //
        "            <gco:Measure uom=\"deg&#x7b;east&#x7d;\">0.0125</gco:Measure>\n" + //
        "          </gmd:resolution>\n" + //
        "        </gmd:MD_Dimension>\n" + //
        "      </gmd:axisDimensionProperties>\n" + //
        "      <gmd:axisDimensionProperties>\n" + //
        "        <gmd:MD_Dimension>\n" + //
        "          <gmd:dimensionName>\n" + //
        "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"row\">row</gmd:MD_DimensionNameTypeCode>\n"
        + //
        "          </gmd:dimensionName>\n" + //
        "          <gmd:dimensionSize>\n" + //
        "            <gco:Integer>NUMBER</gco:Integer>\n" + //
        "          </gmd:dimensionSize>\n" + //
        "          <gmd:resolution>\n" + //
        "            <gco:Measure uom=\"deg&#x7b;north&#x7d;\">0.0125</gco:Measure>\n" + //
        "          </gmd:resolution>\n" + //
        "        </gmd:MD_Dimension>\n" + //
        "      </gmd:axisDimensionProperties>\n" + //
        "      <gmd:axisDimensionProperties>\n" + //
        "        <gmd:MD_Dimension>\n" + //
        "          <gmd:dimensionName>\n" + //
        "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n"
        + //
        "          </gmd:dimensionName>\n" + //
        "          <gmd:dimensionSize>\n" + //
        "            <gco:Integer>NUMBER</gco:Integer>\n" + //
        "          </gmd:dimensionSize>\n" + //
        "          <gmd:resolution gco:nilReason=\"inapplicable\"/>\n" + //
        "        </gmd:MD_Dimension>\n" + //
        "      </gmd:axisDimensionProperties>\n" + //
        "      <gmd:axisDimensionProperties>\n" + //
        "        <gmd:MD_Dimension>\n" + //
        "          <gmd:dimensionName>\n" + //
        "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"temporal\">temporal</gmd:MD_DimensionNameTypeCode>\n"
        + //
        "          </gmd:dimensionName>\n" + //
        "          <gmd:dimensionSize>\n" + //
        "            <gco:Integer>NUMBER</gco:Integer>\n" + //
        "          </gmd:dimensionSize>\n" + //
        "          <gmd:resolution>\n" + //
        "            <gco:Measure uom=\"s\">VALUE</gco:Measure>\n" + //
        "          </gmd:resolution>\n" + //
        "        </gmd:MD_Dimension>\n" + //
        "      </gmd:axisDimensionProperties>\n" + //
        "      <gmd:cellGeometry>\n" + //
        "        <gmd:MD_CellGeometryCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CellGeometryCode\" codeListValue=\"area\">area</gmd:MD_CellGeometryCode>\n"
        + //
        "      </gmd:cellGeometry>\n" + //
        "      <gmd:transformationParameterAvailability gco:nilReason=\"unknown\"/>\n" + //
        "    </gmd:MD_GridSpatialRepresentation>\n" + //
        "  </gmd:spatialRepresentationInfo>\n" + //
        "  <gmd:identificationInfo>\n" + //
        "    <gmd:MD_DataIdentification id=\"DataIdentification\">\n" + //
        "      <gmd:citation>\n" + //
        "        <gmd:CI_Citation>\n" + //
        "          <gmd:title>\n" + //
        "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</gco:CharacterString>\n"
        + //
        "          </gmd:title>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:identifier>\n" + //
        "            <gmd:MD_Identifier>\n" + //
        "              <gmd:authority>\n" + //
        "                <gmd:CI_Citation>\n" + //
        "                  <gmd:title>\n" + //
        "                    <gco:CharacterString>localhost:8080</gco:CharacterString>\n" + //
        "                  </gmd:title>\n" + //
        "                  <gmd:date gco:nilReason=\"inapplicable\"/>\n" + //
        "                </gmd:CI_Citation>\n" + //
        "              </gmd:authority>\n" + //
        "              <gmd:code>\n" + //
        "                <gco:CharacterString>erdMWchla1day</gco:CharacterString>\n" + //
        "              </gmd:code>\n" + //
        "            </gmd:MD_Identifier>\n" + //
        "          </gmd:identifier>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NOAA NMFS SWFSC ERD</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo>\n" + //
        "                <gmd:CI_Contact>\n" + //
        "                  <gmd:address>\n" + //
        "                    <gmd:CI_Address>\n" + //
        "                      <gmd:electronicMailAddress>\n" + //
        "                        <gco:CharacterString>erd.data@noaa.gov</gco:CharacterString>\n" + //
        "                      </gmd:electronicMailAddress>\n" + //
        "                    </gmd:CI_Address>\n" + //
        "                  </gmd:address>\n" + //
        "                  <gmd:onlineResource>\n" + //
        "                    <gmd:CI_OnlineResource>\n" + //
        "                      <gmd:linkage>\n" + //
        "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</gmd:URL>\n" + //
        "                      </gmd:linkage>\n" + //
        "                      <gmd:protocol>\n" + //
        "                        <gco:CharacterString>information</gco:CharacterString>\n" + //
        "                      </gmd:protocol>\n" + //
        "                      <gmd:applicationProfile>\n" + //
        "                        <gco:CharacterString>web browser</gco:CharacterString>\n" + //
        "                      </gmd:applicationProfile>\n" + //
        "                      <gmd:name>\n" + //
        "                        <gco:CharacterString>Background Information</gco:CharacterString>\n" + //
        "                      </gmd:name>\n" + //
        "                      <gmd:description>\n" + //
        "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
        + //
        "                      </gmd:description>\n" + //
        "                      <gmd:function>\n" + //
        "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                      </gmd:function>\n" + //
        "                    </gmd:CI_OnlineResource>\n" + //
        "                  </gmd:onlineResource>\n" + //
        "                </gmd:CI_Contact>\n" + //
        "              </gmd:contactInfo>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NASA GSFC (OBPG)</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"contributor\">contributor</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "        </gmd:CI_Citation>\n" + //
        "      </gmd:citation>\n" + //
        "      <gmd:abstract>\n" + //
        "        <gco:CharacterString>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.</gco:CharacterString>\n"
        + //
        "      </gmd:abstract>\n" + //
        "      <gmd:credit>\n" + //
        "        <gco:CharacterString>NOAA NESDIS COASTWATCH, NOAA SWFSC ERD</gco:CharacterString>\n" + //
        "      </gmd:credit>\n" + //
        "      <gmd:pointOfContact>\n" + //
        "        <gmd:CI_ResponsibleParty>\n" + //
        "          <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "          <gmd:organisationName>\n" + //
        "            <gco:CharacterString>NOAA NMFS SWFSC ERD</gco:CharacterString>\n" + //
        "          </gmd:organisationName>\n" + //
        "          <gmd:contactInfo>\n" + //
        "            <gmd:CI_Contact>\n" + //
        "              <gmd:address>\n" + //
        "                <gmd:CI_Address>\n" + //
        "                  <gmd:electronicMailAddress>\n" + //
        "                    <gco:CharacterString>erd.data@noaa.gov</gco:CharacterString>\n" + //
        "                  </gmd:electronicMailAddress>\n" + //
        "                </gmd:CI_Address>\n" + //
        "              </gmd:address>\n" + //
        "              <gmd:onlineResource>\n" + //
        "                <gmd:CI_OnlineResource>\n" + //
        "                  <gmd:linkage>\n" + //
        "                    <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</gmd:URL>\n" + //
        "                  </gmd:linkage>\n" + //
        "                  <gmd:protocol>\n" + //
        "                    <gco:CharacterString>information</gco:CharacterString>\n" + //
        "                  </gmd:protocol>\n" + //
        "                  <gmd:applicationProfile>\n" + //
        "                    <gco:CharacterString>web browser</gco:CharacterString>\n" + //
        "                  </gmd:applicationProfile>\n" + //
        "                  <gmd:name>\n" + //
        "                    <gco:CharacterString>Background Information</gco:CharacterString>\n" + //
        "                  </gmd:name>\n" + //
        "                  <gmd:description>\n" + //
        "                    <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
        + //
        "                  </gmd:description>\n" + //
        "                  <gmd:function>\n" + //
        "                    <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                  </gmd:function>\n" + //
        "                </gmd:CI_OnlineResource>\n" + //
        "              </gmd:onlineResource>\n" + //
        "            </gmd:CI_Contact>\n" + //
        "          </gmd:contactInfo>\n" + //
        "          <gmd:role>\n" + //
        "            <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n"
        + //
        "          </gmd:role>\n" + //
        "        </gmd:CI_ResponsibleParty>\n" + //
        "      </gmd:pointOfContact>\n" + //
        "      <gmd:descriptiveKeywords>\n" + //
        "        <gmd:MD_Keywords>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>aqua</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>chemistry</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>chla</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>chlorophyll</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>chlorophyll-a</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>coast</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>coastwatch</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>color</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>concentration</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>data</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>degrees</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>experimental</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>imaging</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>moderate</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>modis</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>MWchla</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>national</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>noaa</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>node</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>npp</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>ocean</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>ocean color</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>oceans</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>orbiting</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>partnership</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>polar</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>polar-orbiting</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>resolution</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>sea</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>seawater</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>spectroradiometer</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>US</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>water</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>wcn</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>west</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:type>\n" + //
        "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
        + //
        "          </gmd:type>\n" + //
        "          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n" + //
        "        </gmd:MD_Keywords>\n" + //
        "      </gmd:descriptiveKeywords>\n" + //
        "      <gmd:descriptiveKeywords>\n" + //
        "        <gmd:MD_Keywords>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</gco:CharacterString>\n"
        + //
        "          </gmd:keyword>\n" + //
        "          <gmd:type>\n" + //
        "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
        + //
        "          </gmd:type>\n" + //
        "          <gmd:thesaurusName>\n" + //
        "            <gmd:CI_Citation>\n" + //
        "              <gmd:title>\n" + //
        "                <gco:CharacterString>GCMD Science Keywords</gco:CharacterString>\n" + //
        "              </gmd:title>\n" + //
        "              <gmd:date gco:nilReason=\"unknown\"/>\n" + //
        "            </gmd:CI_Citation>\n" + //
        "          </gmd:thesaurusName>\n" + //
        "        </gmd:MD_Keywords>\n" + //
        "      </gmd:descriptiveKeywords>\n" + //
        "      <gmd:descriptiveKeywords>\n" + //
        "        <gmd:MD_Keywords>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>CoastWatch (https://coastwatch.noaa.gov/)</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:type>\n" + //
        "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"project\">project</gmd:MD_KeywordTypeCode>\n"
        + //
        "          </gmd:type>\n" + //
        "          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n" + //
        "        </gmd:MD_Keywords>\n" + //
        "      </gmd:descriptiveKeywords>\n" + //
        "      <gmd:descriptiveKeywords>\n" + //
        "        <gmd:MD_Keywords>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>time</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>altitude</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>latitude</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>longitude</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:keyword>\n" + //
        "            <gco:CharacterString>concentration_of_chlorophyll_in_sea_water</gco:CharacterString>\n" + //
        "          </gmd:keyword>\n" + //
        "          <gmd:type>\n" + //
        "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
        + //
        "          </gmd:type>\n" + //
        "          <gmd:thesaurusName>\n" + //
        "            <gmd:CI_Citation>\n" + //
        "              <gmd:title>\n" + //
        "                <gco:CharacterString>CF Standard Name Table v70</gco:CharacterString>\n" + //
        "              </gmd:title>\n" + //
        "              <gmd:date gco:nilReason=\"unknown\"/>\n" + //
        "            </gmd:CI_Citation>\n" + //
        "          </gmd:thesaurusName>\n" + //
        "        </gmd:MD_Keywords>\n" + //
        "      </gmd:descriptiveKeywords>\n" + //
        "      <gmd:resourceConstraints>\n" + //
        "        <gmd:MD_LegalConstraints>\n" + //
        "          <gmd:useLimitation>\n" + //
        "            <gco:CharacterString>The data may be used and redistributed for free but is not intended\n"
        + //
        "for legal use, since it may contain inaccuracies. Neither the data\n" + //
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" + //
        "of their employees or contractors, makes any warranty, express or\n" + //
        "implied, including warranties of merchantability and fitness for a\n" + //
        "particular purpose, or assumes any legal liability for the accuracy,\n" + //
        "completeness, or usefulness, of this information.</gco:CharacterString>\n" + //
        "          </gmd:useLimitation>\n" + //
        "        </gmd:MD_LegalConstraints>\n" + //
        "      </gmd:resourceConstraints>\n" + //
        "      <gmd:aggregationInfo>\n" + //
        "        <gmd:MD_AggregateInformation>\n" + //
        "          <gmd:aggregateDataSetName>\n" + //
        "            <gmd:CI_Citation>\n" + //
        "              <gmd:title>\n" + //
        "                <gco:CharacterString>CoastWatch (https://coastwatch.noaa.gov/)</gco:CharacterString>\n"
        + //
        "              </gmd:title>\n" + //
        "              <gmd:date gco:nilReason=\"inapplicable\"/>\n" + //
        "            </gmd:CI_Citation>\n" + //
        "          </gmd:aggregateDataSetName>\n" + //
        "          <gmd:associationType>\n" + //
        "            <gmd:DS_AssociationTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n"
        + //
        "          </gmd:associationType>\n" + //
        "          <gmd:initiativeType>\n" + //
        "            <gmd:DS_InitiativeTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n"
        + //
        "          </gmd:initiativeType>\n" + //
        "        </gmd:MD_AggregateInformation>\n" + //
        "      </gmd:aggregationInfo>\n" + //
        "      <gmd:aggregationInfo>\n" + //
        "        <gmd:MD_AggregateInformation>\n" + //
        "          <gmd:aggregateDataSetIdentifier>\n" + //
        "            <gmd:MD_Identifier>\n" + //
        "              <gmd:authority>\n" + //
        "                <gmd:CI_Citation>\n" + //
        "                  <gmd:title>\n" + //
        "                    <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>\n" + //
        "                  </gmd:title>\n" + //
        "                  <gmd:date gco:nilReason=\"inapplicable\"/>\n" + //
        "                </gmd:CI_Citation>\n" + //
        "              </gmd:authority>\n" + //
        "              <gmd:code>\n" + //
        "                <gco:CharacterString>Grid</gco:CharacterString>\n" + //
        "              </gmd:code>\n" + //
        "            </gmd:MD_Identifier>\n" + //
        "          </gmd:aggregateDataSetIdentifier>\n" + //
        "          <gmd:associationType>\n" + //
        "            <gmd:DS_AssociationTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n"
        + //
        "          </gmd:associationType>\n" + //
        "          <gmd:initiativeType>\n" + //
        "            <gmd:DS_InitiativeTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n"
        + //
        "          </gmd:initiativeType>\n" + //
        "        </gmd:MD_AggregateInformation>\n" + //
        "      </gmd:aggregationInfo>\n" + //
        "      <gmd:language>\n" + //
        "        <gco:CharacterString>eng</gco:CharacterString>\n" + //
        "      </gmd:language>\n" + //
        "      <gmd:topicCategory>\n" + //
        "        <gmd:MD_TopicCategoryCode>geoscientificInformation</gmd:MD_TopicCategoryCode>\n" + //
        "      </gmd:topicCategory>\n" + //
        "      <gmd:extent>\n" + //
        "        <gmd:EX_Extent id=\"boundingExtent\">\n" + //
        "          <gmd:geographicElement>\n" + //
        "            <gmd:EX_GeographicBoundingBox id=\"boundingGeographicBoundingBox\">\n" + //
        "              <gmd:extentTypeCode>\n" + //
        "                <gco:Boolean>1</gco:Boolean>\n" + //
        "              </gmd:extentTypeCode>\n" + //
        "              <gmd:westBoundLongitude>\n" + //
        "                <gco:Decimal>-155.0</gco:Decimal>\n" + //
        "              </gmd:westBoundLongitude>\n" + //
        "              <gmd:eastBoundLongitude>\n" + //
        "                <gco:Decimal>-105.0</gco:Decimal>\n" + //
        "              </gmd:eastBoundLongitude>\n" + //
        "              <gmd:southBoundLatitude>\n" + //
        "                <gco:Decimal>22.0</gco:Decimal>\n" + //
        "              </gmd:southBoundLatitude>\n" + //
        "              <gmd:northBoundLatitude>\n" + //
        "                <gco:Decimal>51.0</gco:Decimal>\n" + //
        "              </gmd:northBoundLatitude>\n" + //
        "            </gmd:EX_GeographicBoundingBox>\n" + //
        "          </gmd:geographicElement>\n" + //
        "          <gmd:temporalElement>\n" + //
        "            <gmd:EX_TemporalExtent id=\"boundingTemporalExtent\">\n" + //
        "              <gmd:extent>\n" + //
        "                <gml:TimePeriod gml:id=\"DI_gmdExtent_timePeriod_id\">\n" + //
        "                  <gml:description>seconds</gml:description>\n" + //
        "                  <gml:beginPosition>2002-07-04T12:00:00Z</gml:beginPosition>\n" + //
        "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n" + //
        "                </gml:TimePeriod>\n" + //
        "              </gmd:extent>\n" + //
        "            </gmd:EX_TemporalExtent>\n" + //
        "          </gmd:temporalElement>\n" + //
        "          <gmd:verticalElement>\n" + //
        "            <gmd:EX_VerticalExtent>\n" + //
        "              <gmd:minimumValue><gco:Real>0.0</gco:Real></gmd:minimumValue>\n" + //
        "              <gmd:maximumValue><gco:Real>0.0</gco:Real></gmd:maximumValue>\n" + //
        "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" + //
        "            </gmd:EX_VerticalExtent>\n" + //
        "          </gmd:verticalElement>\n" + //
        "        </gmd:EX_Extent>\n" + //
        "      </gmd:extent>\n" + //
        "    </gmd:MD_DataIdentification>\n" + //
        "  </gmd:identificationInfo>\n" + //
        "  <gmd:identificationInfo>\n" + //
        "    <srv:SV_ServiceIdentification id=\"ERDDAP-griddap\">\n" + //
        "      <gmd:citation>\n" + //
        "        <gmd:CI_Citation>\n" + //
        "          <gmd:title>\n" + //
        "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</gco:CharacterString>\n"
        + //
        "          </gmd:title>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NOAA NMFS SWFSC ERD</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo>\n" + //
        "                <gmd:CI_Contact>\n" + //
        "                  <gmd:address>\n" + //
        "                    <gmd:CI_Address>\n" + //
        "                      <gmd:electronicMailAddress>\n" + //
        "                        <gco:CharacterString>erd.data@noaa.gov</gco:CharacterString>\n" + //
        "                      </gmd:electronicMailAddress>\n" + //
        "                    </gmd:CI_Address>\n" + //
        "                  </gmd:address>\n" + //
        "                  <gmd:onlineResource>\n" + //
        "                    <gmd:CI_OnlineResource>\n" + //
        "                      <gmd:linkage>\n" + //
        "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</gmd:URL>\n" + //
        "                      </gmd:linkage>\n" + //
        "                      <gmd:protocol>\n" + //
        "                        <gco:CharacterString>information</gco:CharacterString>\n" + //
        "                      </gmd:protocol>\n" + //
        "                      <gmd:applicationProfile>\n" + //
        "                        <gco:CharacterString>web browser</gco:CharacterString>\n" + //
        "                      </gmd:applicationProfile>\n" + //
        "                      <gmd:name>\n" + //
        "                        <gco:CharacterString>Background Information</gco:CharacterString>\n" + //
        "                      </gmd:name>\n" + //
        "                      <gmd:description>\n" + //
        "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
        + //
        "                      </gmd:description>\n" + //
        "                      <gmd:function>\n" + //
        "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                      </gmd:function>\n" + //
        "                    </gmd:CI_OnlineResource>\n" + //
        "                  </gmd:onlineResource>\n" + //
        "                </gmd:CI_Contact>\n" + //
        "              </gmd:contactInfo>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NASA GSFC (OBPG)</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"contributor\">contributor</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "        </gmd:CI_Citation>\n" + //
        "      </gmd:citation>\n" + //
        "      <gmd:abstract>\n" + //
        "        <gco:CharacterString>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.</gco:CharacterString>\n"
        + //
        "      </gmd:abstract>\n" + //
        "      <srv:serviceType>\n" + //
        "        <gco:LocalName>ERDDAP griddap</gco:LocalName>\n" + //
        "      </srv:serviceType>\n" + //
        "      <srv:extent>\n" + //
        "        <gmd:EX_Extent>\n" + //
        "          <gmd:geographicElement>\n" + //
        "            <gmd:EX_GeographicBoundingBox>\n" + //
        "              <gmd:extentTypeCode>\n" + //
        "                <gco:Boolean>1</gco:Boolean>\n" + //
        "              </gmd:extentTypeCode>\n" + //
        "              <gmd:westBoundLongitude>\n" + //
        "                <gco:Decimal>-155.0</gco:Decimal>\n" + //
        "              </gmd:westBoundLongitude>\n" + //
        "              <gmd:eastBoundLongitude>\n" + //
        "                <gco:Decimal>-105.0</gco:Decimal>\n" + //
        "              </gmd:eastBoundLongitude>\n" + //
        "              <gmd:southBoundLatitude>\n" + //
        "                <gco:Decimal>22.0</gco:Decimal>\n" + //
        "              </gmd:southBoundLatitude>\n" + //
        "              <gmd:northBoundLatitude>\n" + //
        "                <gco:Decimal>51.0</gco:Decimal>\n" + //
        "              </gmd:northBoundLatitude>\n" + //
        "            </gmd:EX_GeographicBoundingBox>\n" + //
        "          </gmd:geographicElement>\n" + //
        "          <gmd:temporalElement>\n" + //
        "            <gmd:EX_TemporalExtent>\n" + //
        "              <gmd:extent>\n" + //
        "                <gml:TimePeriod gml:id=\"ED_gmdExtent_timePeriod_id\">\n" + //
        "                  <gml:description>seconds</gml:description>\n" + //
        "                  <gml:beginPosition>2002-07-04T12:00:00Z</gml:beginPosition>\n" + //
        "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n" + //
        "                </gml:TimePeriod>\n" + //
        "              </gmd:extent>\n" + //
        "            </gmd:EX_TemporalExtent>\n" + //
        "          </gmd:temporalElement>\n" + //
        "          <gmd:verticalElement>\n" + //
        "            <gmd:EX_VerticalExtent>\n" + //
        "              <gmd:minimumValue><gco:Real>0.0</gco:Real></gmd:minimumValue>\n" + //
        "              <gmd:maximumValue><gco:Real>0.0</gco:Real></gmd:maximumValue>\n" + //
        "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" + //
        "            </gmd:EX_VerticalExtent>\n" + //
        "          </gmd:verticalElement>\n" + //
        "        </gmd:EX_Extent>\n" + //
        "      </srv:extent>\n" + //
        "      <srv:couplingType>\n" + //
        "        <srv:SV_CouplingType codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
        + //
        "      </srv:couplingType>\n" + //
        "      <srv:containsOperations>\n" + //
        "        <srv:SV_OperationMetadata>\n" + //
        "          <srv:operationName>\n" + //
        "            <gco:CharacterString>ERDDAPgriddapDatasetQueryAndAccess</gco:CharacterString>\n" + //
        "          </srv:operationName>\n" + //
        "          <srv:DCP gco:nilReason=\"unknown\"/>\n" + //
        "          <srv:connectPoint>\n" + //
        "            <gmd:CI_OnlineResource>\n" + //
        "              <gmd:linkage>\n" + //
        "                <gmd:URL>http://localhost:8080/erddap/griddap/erdMWchla1day</gmd:URL>\n" + //
        "              </gmd:linkage>\n" + //
        "              <gmd:protocol>\n" + //
        "                <gco:CharacterString>ERDDAP:griddap</gco:CharacterString>\n" + //
        "              </gmd:protocol>\n" + //
        "              <gmd:name>\n" + //
        "                <gco:CharacterString>ERDDAP-griddap</gco:CharacterString>\n" + //
        "              </gmd:name>\n" + //
        "              <gmd:description>\n" + //
        "                <gco:CharacterString>ERDDAP's griddap service (a flavor of OPeNDAP) for gridded data. Add different extensions (e.g., .html, .graph, .das, .dds) to the base URL for different purposes.</gco:CharacterString>\n"
        + //
        "              </gmd:description>\n" + //
        "              <gmd:function>\n" + //
        "                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
        + //
        "              </gmd:function>\n" + //
        "            </gmd:CI_OnlineResource>\n" + //
        "          </srv:connectPoint>\n" + //
        "        </srv:SV_OperationMetadata>\n" + //
        "      </srv:containsOperations>\n" + //
        "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n" + //
        "    </srv:SV_ServiceIdentification>\n" + //
        "  </gmd:identificationInfo>\n" + //
        "  <gmd:identificationInfo>\n" + //
        "    <srv:SV_ServiceIdentification id=\"OPeNDAP\">\n" + //
        "      <gmd:citation>\n" + //
        "        <gmd:CI_Citation>\n" + //
        "          <gmd:title>\n" + //
        "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</gco:CharacterString>\n"
        + //
        "          </gmd:title>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NOAA NMFS SWFSC ERD</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo>\n" + //
        "                <gmd:CI_Contact>\n" + //
        "                  <gmd:address>\n" + //
        "                    <gmd:CI_Address>\n" + //
        "                      <gmd:electronicMailAddress>\n" + //
        "                        <gco:CharacterString>erd.data@noaa.gov</gco:CharacterString>\n" + //
        "                      </gmd:electronicMailAddress>\n" + //
        "                    </gmd:CI_Address>\n" + //
        "                  </gmd:address>\n" + //
        "                  <gmd:onlineResource>\n" + //
        "                    <gmd:CI_OnlineResource>\n" + //
        "                      <gmd:linkage>\n" + //
        "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</gmd:URL>\n" + //
        "                      </gmd:linkage>\n" + //
        "                      <gmd:protocol>\n" + //
        "                        <gco:CharacterString>information</gco:CharacterString>\n" + //
        "                      </gmd:protocol>\n" + //
        "                      <gmd:applicationProfile>\n" + //
        "                        <gco:CharacterString>web browser</gco:CharacterString>\n" + //
        "                      </gmd:applicationProfile>\n" + //
        "                      <gmd:name>\n" + //
        "                        <gco:CharacterString>Background Information</gco:CharacterString>\n" + //
        "                      </gmd:name>\n" + //
        "                      <gmd:description>\n" + //
        "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
        + //
        "                      </gmd:description>\n" + //
        "                      <gmd:function>\n" + //
        "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                      </gmd:function>\n" + //
        "                    </gmd:CI_OnlineResource>\n" + //
        "                  </gmd:onlineResource>\n" + //
        "                </gmd:CI_Contact>\n" + //
        "              </gmd:contactInfo>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NASA GSFC (OBPG)</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"contributor\">contributor</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "        </gmd:CI_Citation>\n" + //
        "      </gmd:citation>\n" + //
        "      <gmd:abstract>\n" + //
        "        <gco:CharacterString>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.</gco:CharacterString>\n"
        + //
        "      </gmd:abstract>\n" + //
        "      <srv:serviceType>\n" + //
        "        <gco:LocalName>OPeNDAP</gco:LocalName>\n" + //
        "      </srv:serviceType>\n" + //
        "      <srv:extent>\n" + //
        "        <gmd:EX_Extent>\n" + //
        "          <gmd:geographicElement>\n" + //
        "            <gmd:EX_GeographicBoundingBox>\n" + //
        "              <gmd:extentTypeCode>\n" + //
        "                <gco:Boolean>1</gco:Boolean>\n" + //
        "              </gmd:extentTypeCode>\n" + //
        "              <gmd:westBoundLongitude>\n" + //
        "                <gco:Decimal>-155.0</gco:Decimal>\n" + //
        "              </gmd:westBoundLongitude>\n" + //
        "              <gmd:eastBoundLongitude>\n" + //
        "                <gco:Decimal>-105.0</gco:Decimal>\n" + //
        "              </gmd:eastBoundLongitude>\n" + //
        "              <gmd:southBoundLatitude>\n" + //
        "                <gco:Decimal>22.0</gco:Decimal>\n" + //
        "              </gmd:southBoundLatitude>\n" + //
        "              <gmd:northBoundLatitude>\n" + //
        "                <gco:Decimal>51.0</gco:Decimal>\n" + //
        "              </gmd:northBoundLatitude>\n" + //
        "            </gmd:EX_GeographicBoundingBox>\n" + //
        "          </gmd:geographicElement>\n" + //
        "          <gmd:temporalElement>\n" + //
        "            <gmd:EX_TemporalExtent>\n" + //
        "              <gmd:extent>\n" + //
        "                <gml:TimePeriod gml:id=\"OD_gmdExtent_timePeriod_id\">\n" + //
        "                  <gml:description>seconds</gml:description>\n" + //
        "                  <gml:beginPosition>2002-07-04T12:00:00Z</gml:beginPosition>\n" + //
        "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n" + //
        "                </gml:TimePeriod>\n" + //
        "              </gmd:extent>\n" + //
        "            </gmd:EX_TemporalExtent>\n" + //
        "          </gmd:temporalElement>\n" + //
        "          <gmd:verticalElement>\n" + //
        "            <gmd:EX_VerticalExtent>\n" + //
        "              <gmd:minimumValue><gco:Real>0.0</gco:Real></gmd:minimumValue>\n" + //
        "              <gmd:maximumValue><gco:Real>0.0</gco:Real></gmd:maximumValue>\n" + //
        "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" + //
        "            </gmd:EX_VerticalExtent>\n" + //
        "          </gmd:verticalElement>\n" + //
        "        </gmd:EX_Extent>\n" + //
        "      </srv:extent>\n" + //
        "      <srv:couplingType>\n" + //
        "        <srv:SV_CouplingType codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
        + //
        "      </srv:couplingType>\n" + //
        "      <srv:containsOperations>\n" + //
        "        <srv:SV_OperationMetadata>\n" + //
        "          <srv:operationName>\n" + //
        "            <gco:CharacterString>OPeNDAPDatasetQueryAndAccess</gco:CharacterString>\n" + //
        "          </srv:operationName>\n" + //
        "          <srv:DCP gco:nilReason=\"unknown\"/>\n" + //
        "          <srv:connectPoint>\n" + //
        "            <gmd:CI_OnlineResource>\n" + //
        "              <gmd:linkage>\n" + //
        "                <gmd:URL>http://localhost:8080/erddap/griddap/erdMWchla1day</gmd:URL>\n" + //
        "              </gmd:linkage>\n" + //
        "              <gmd:protocol>\n" + //
        "                <gco:CharacterString>OPeNDAP:OPeNDAP</gco:CharacterString>\n" + //
        "              </gmd:protocol>\n" + //
        "              <gmd:name>\n" + //
        "                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" + //
        "              </gmd:name>\n" + //
        "              <gmd:description>\n" + //
        "                <gco:CharacterString>An OPeNDAP service for gridded data. Add different extensions (e.g., .html, .das, .dds) to the base URL for different purposes.</gco:CharacterString>\n"
        + //
        "              </gmd:description>\n" + //
        "              <gmd:function>\n" + //
        "                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
        + //
        "              </gmd:function>\n" + //
        "            </gmd:CI_OnlineResource>\n" + //
        "          </srv:connectPoint>\n" + //
        "        </srv:SV_OperationMetadata>\n" + //
        "      </srv:containsOperations>\n" + //
        "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n" + //
        "    </srv:SV_ServiceIdentification>\n" + //
        "  </gmd:identificationInfo>\n" + //
        "  <gmd:identificationInfo>\n" + //
        "    <srv:SV_ServiceIdentification id=\"OGC-WMS\">\n" + //
        "      <gmd:citation>\n" + //
        "        <gmd:CI_Citation>\n" + //
        "          <gmd:title>\n" + //
        "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</gco:CharacterString>\n"
        + //
        "          </gmd:title>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:date>\n" + //
        "            <gmd:CI_Date>\n" + //
        "              <gmd:date>\n" + //
        "                <gco:Date>YYYY-MM-DD</gco:Date>\n" + //
        "              </gmd:date>\n" + //
        "              <gmd:dateType>\n" + //
        "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n"
        + //
        "              </gmd:dateType>\n" + //
        "            </gmd:CI_Date>\n" + //
        "          </gmd:date>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NOAA NMFS SWFSC ERD</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo>\n" + //
        "                <gmd:CI_Contact>\n" + //
        "                  <gmd:address>\n" + //
        "                    <gmd:CI_Address>\n" + //
        "                      <gmd:electronicMailAddress>\n" + //
        "                        <gco:CharacterString>erd.data@noaa.gov</gco:CharacterString>\n" + //
        "                      </gmd:electronicMailAddress>\n" + //
        "                    </gmd:CI_Address>\n" + //
        "                  </gmd:address>\n" + //
        "                  <gmd:onlineResource>\n" + //
        "                    <gmd:CI_OnlineResource>\n" + //
        "                      <gmd:linkage>\n" + //
        "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</gmd:URL>\n" + //
        "                      </gmd:linkage>\n" + //
        "                      <gmd:protocol>\n" + //
        "                        <gco:CharacterString>information</gco:CharacterString>\n" + //
        "                      </gmd:protocol>\n" + //
        "                      <gmd:applicationProfile>\n" + //
        "                        <gco:CharacterString>web browser</gco:CharacterString>\n" + //
        "                      </gmd:applicationProfile>\n" + //
        "                      <gmd:name>\n" + //
        "                        <gco:CharacterString>Background Information</gco:CharacterString>\n" + //
        "                      </gmd:name>\n" + //
        "                      <gmd:description>\n" + //
        "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
        + //
        "                      </gmd:description>\n" + //
        "                      <gmd:function>\n" + //
        "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                      </gmd:function>\n" + //
        "                    </gmd:CI_OnlineResource>\n" + //
        "                  </gmd:onlineResource>\n" + //
        "                </gmd:CI_Contact>\n" + //
        "              </gmd:contactInfo>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "          <gmd:citedResponsibleParty>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>NASA GSFC (OBPG)</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo gco:nilReason=\"missing\"/>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"contributor\">contributor</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:citedResponsibleParty>\n" + //
        "        </gmd:CI_Citation>\n" + //
        "      </gmd:citation>\n" + //
        "      <gmd:abstract>\n" + //
        "        <gco:CharacterString>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.</gco:CharacterString>\n"
        + //
        "      </gmd:abstract>\n" + //
        "      <srv:serviceType>\n" + //
        "        <gco:LocalName>Open Geospatial Consortium Web Map Service (WMS)</gco:LocalName>\n" + //
        "      </srv:serviceType>\n" + //
        "      <srv:extent>\n" + //
        "        <gmd:EX_Extent>\n" + //
        "          <gmd:geographicElement>\n" + //
        "            <gmd:EX_GeographicBoundingBox>\n" + //
        "              <gmd:extentTypeCode>\n" + //
        "                <gco:Boolean>1</gco:Boolean>\n" + //
        "              </gmd:extentTypeCode>\n" + //
        "              <gmd:westBoundLongitude>\n" + //
        "                <gco:Decimal>-155.0</gco:Decimal>\n" + //
        "              </gmd:westBoundLongitude>\n" + //
        "              <gmd:eastBoundLongitude>\n" + //
        "                <gco:Decimal>-105.0</gco:Decimal>\n" + //
        "              </gmd:eastBoundLongitude>\n" + //
        "              <gmd:southBoundLatitude>\n" + //
        "                <gco:Decimal>22.0</gco:Decimal>\n" + //
        "              </gmd:southBoundLatitude>\n" + //
        "              <gmd:northBoundLatitude>\n" + //
        "                <gco:Decimal>51.0</gco:Decimal>\n" + //
        "              </gmd:northBoundLatitude>\n" + //
        "            </gmd:EX_GeographicBoundingBox>\n" + //
        "          </gmd:geographicElement>\n" + //
        "          <gmd:temporalElement>\n" + //
        "            <gmd:EX_TemporalExtent>\n" + //
        "              <gmd:extent>\n" + //
        "                <gml:TimePeriod gml:id=\"WMS_gmdExtent_timePeriod_id\">\n" + //
        "                  <gml:description>seconds</gml:description>\n" + //
        "                  <gml:beginPosition>2002-07-04T12:00:00Z</gml:beginPosition>\n" + //
        "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n" + //
        "                </gml:TimePeriod>\n" + //
        "              </gmd:extent>\n" + //
        "            </gmd:EX_TemporalExtent>\n" + //
        "          </gmd:temporalElement>\n" + //
        "          <gmd:verticalElement>\n" + //
        "            <gmd:EX_VerticalExtent>\n" + //
        "              <gmd:minimumValue><gco:Real>0.0</gco:Real></gmd:minimumValue>\n" + //
        "              <gmd:maximumValue><gco:Real>0.0</gco:Real></gmd:maximumValue>\n" + //
        "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" + //
        "            </gmd:EX_VerticalExtent>\n" + //
        "          </gmd:verticalElement>\n" + //
        "        </gmd:EX_Extent>\n" + //
        "      </srv:extent>\n" + //
        "      <srv:couplingType>\n" + //
        "        <srv:SV_CouplingType codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
        + //
        "      </srv:couplingType>\n" + //
        "      <srv:containsOperations>\n" + //
        "        <srv:SV_OperationMetadata>\n" + //
        "          <srv:operationName>\n" + //
        "            <gco:CharacterString>GetCapabilities</gco:CharacterString>\n" + //
        "          </srv:operationName>\n" + //
        "          <srv:DCP gco:nilReason=\"unknown\"/>\n" + //
        "          <srv:connectPoint>\n" + //
        "            <gmd:CI_OnlineResource>\n" + //
        "              <gmd:linkage>\n" + //
        "                <gmd:URL>http://localhost:8080/erddap/wms/erdMWchla1day/request?service=WMS&amp;version=1.3.0&amp;request=GetCapabilities</gmd:URL>\n"
        + //
        "              </gmd:linkage>\n" + //
        "              <gmd:protocol>\n" + //
        "                <gco:CharacterString>OGC:WMS</gco:CharacterString>\n" + //
        "              </gmd:protocol>\n" + //
        "              <gmd:name>\n" + //
        "                <gco:CharacterString>OGC-WMS</gco:CharacterString>\n" + //
        "              </gmd:name>\n" + //
        "              <gmd:description>\n" + //
        "                <gco:CharacterString>Open Geospatial Consortium Web Map Service (WMS)</gco:CharacterString>\n"
        + //
        "              </gmd:description>\n" + //
        "              <gmd:function>\n" + //
        "                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
        + //
        "              </gmd:function>\n" + //
        "            </gmd:CI_OnlineResource>\n" + //
        "          </srv:connectPoint>\n" + //
        "        </srv:SV_OperationMetadata>\n" + //
        "      </srv:containsOperations>\n" + //
        "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n" + //
        "    </srv:SV_ServiceIdentification>\n" + //
        "  </gmd:identificationInfo>\n" + //
        "  <gmd:contentInfo>\n" + //
        "    <gmi:MI_CoverageDescription>\n" + //
        "      <gmd:attributeDescription gco:nilReason=\"unknown\"/>\n" + //
        "      <gmd:contentType>\n" + //
        "        <gmd:MD_CoverageContentTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CoverageContentTypeCode\" codeListValue=\"physicalMeasurement\">physicalMeasurement</gmd:MD_CoverageContentTypeCode>\n"
        + //
        "      </gmd:contentType>\n" + //
        "      <gmd:dimension>\n" + //
        "        <gmd:MD_Band>\n" + //
        "          <gmd:sequenceIdentifier>\n" + //
        "            <gco:MemberName>\n" + //
        "              <gco:aName>\n" + //
        "                <gco:CharacterString>chlorophyll</gco:CharacterString>\n" + //
        "              </gco:aName>\n" + //
        "              <gco:attributeType>\n" + //
        "                <gco:TypeName>\n" + //
        "                  <gco:aName>\n" + //
        "                    <gco:CharacterString>float</gco:CharacterString>\n" + //
        "                  </gco:aName>\n" + //
        "                </gco:TypeName>\n" + //
        "              </gco:attributeType>\n" + //
        "            </gco:MemberName>\n" + //
        "          </gmd:sequenceIdentifier>\n" + //
        "          <gmd:descriptor>\n" + //
        "            <gco:CharacterString>Concentration Of Chlorophyll In Sea Water</gco:CharacterString>\n" + //
        "          </gmd:descriptor>\n" + //
        "          <gmd:units xlink:href=\"https://unitsofmeasure.org/ucum.html#mg&#x2e;m&#x2d;3\"/>\n" + //
        "        </gmd:MD_Band>\n" + //
        "      </gmd:dimension>\n" + //
        "    </gmi:MI_CoverageDescription>\n" + //
        "  </gmd:contentInfo>\n" + //
        "  <gmd:distributionInfo>\n" + //
        "    <gmd:MD_Distribution>\n" + //
        "      <gmd:distributor>\n" + //
        "        <gmd:MD_Distributor>\n" + //
        "          <gmd:distributorContact>\n" + //
        "            <gmd:CI_ResponsibleParty>\n" + //
        "              <gmd:individualName>\n" + //
        "                <gco:CharacterString>ERDDAP Jetty Developer</gco:CharacterString>\n" + //
        "              </gmd:individualName>\n" + //
        "              <gmd:organisationName>\n" + //
        "                <gco:CharacterString>ERDDAP Jetty Install</gco:CharacterString>\n" + //
        "              </gmd:organisationName>\n" + //
        "              <gmd:contactInfo>\n" + //
        "                <gmd:CI_Contact>\n" + //
        "                  <gmd:phone>\n" + //
        "                    <gmd:CI_Telephone>\n" + //
        "                      <gmd:voice>\n" + //
        "                        <gco:CharacterString>555-555-5555</gco:CharacterString>\n" + //
        "                      </gmd:voice>\n" + //
        "                    </gmd:CI_Telephone>\n" + //
        "                  </gmd:phone>\n" + //
        "                  <gmd:address>\n" + //
        "                    <gmd:CI_Address>\n" + //
        "                      <gmd:deliveryPoint>\n" + //
        "                        <gco:CharacterString>123 Irrelevant St.</gco:CharacterString>\n" + //
        "                      </gmd:deliveryPoint>\n" + //
        "                      <gmd:city>\n" + //
        "                        <gco:CharacterString>Nowhere</gco:CharacterString>\n" + //
        "                      </gmd:city>\n" + //
        "                      <gmd:administrativeArea>\n" + //
        "                        <gco:CharacterString>AK</gco:CharacterString>\n" + //
        "                      </gmd:administrativeArea>\n" + //
        "                      <gmd:postalCode>\n" + //
        "                        <gco:CharacterString>99504</gco:CharacterString>\n" + //
        "                      </gmd:postalCode>\n" + //
        "                      <gmd:country>\n" + //
        "                        <gco:CharacterString>USA</gco:CharacterString>\n" + //
        "                      </gmd:country>\n" + //
        "                      <gmd:electronicMailAddress>\n" + //
        "                        <gco:CharacterString>nobody@example.com</gco:CharacterString>\n" + //
        "                      </gmd:electronicMailAddress>\n" + //
        "                    </gmd:CI_Address>\n" + //
        "                  </gmd:address>\n" + //
        "                </gmd:CI_Contact>\n" + //
        "              </gmd:contactInfo>\n" + //
        "              <gmd:role>\n" + //
        "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"distributor\">distributor</gmd:CI_RoleCode>\n"
        + //
        "              </gmd:role>\n" + //
        "            </gmd:CI_ResponsibleParty>\n" + //
        "          </gmd:distributorContact>\n" + //
        "          <gmd:distributorFormat>\n" + //
        "            <gmd:MD_Format>\n" + //
        "              <gmd:name>\n" + //
        "                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n" + //
        "              </gmd:name>\n" + //
        "              <gmd:version>\n" + //
        "                <gco:CharacterString>DAP/2.0</gco:CharacterString>\n" + //
        "              </gmd:version>\n" + //
        "            </gmd:MD_Format>\n" + //
        "          </gmd:distributorFormat>\n" + //
        "          <gmd:distributorTransferOptions>\n" + //
        "            <gmd:MD_DigitalTransferOptions>\n" + //
        "              <gmd:onLine>\n" + //
        "                <gmd:CI_OnlineResource>\n" + //
        "                  <gmd:linkage>\n" + //
        "                    <gmd:URL>http://localhost:8080/erddap/griddap/erdMWchla1day.html</gmd:URL>\n" + //
        "                  </gmd:linkage>\n" + //
        "                  <gmd:protocol>\n" + //
        "                    <gco:CharacterString>order</gco:CharacterString>\n" + //
        "                  </gmd:protocol>\n" + //
        "                  <gmd:name>\n" + //
        "                    <gco:CharacterString>Data Subset Form</gco:CharacterString>\n" + //
        "                  </gmd:name>\n" + //
        "                  <gmd:description>\n" + //
        "                    <gco:CharacterString>ERDDAP's version of the OPeNDAP .html web page for this dataset. Specify a subset of the dataset and download the data via OPeNDAP or in many different file types.</gco:CharacterString>\n"
        + //
        "                  </gmd:description>\n" + //
        "                  <gmd:function>\n" + //
        "                    <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                  </gmd:function>\n" + //
        "                </gmd:CI_OnlineResource>\n" + //
        "              </gmd:onLine>\n" + //
        "            </gmd:MD_DigitalTransferOptions>\n" + //
        "          </gmd:distributorTransferOptions>\n" + //
        "          <gmd:distributorTransferOptions>\n" + //
        "            <gmd:MD_DigitalTransferOptions>\n" + //
        "              <gmd:onLine>\n" + //
        "                <gmd:CI_OnlineResource>\n" + //
        "                  <gmd:linkage>\n" + //
        "                    <gmd:URL>http://localhost:8080/erddap/griddap/erdMWchla1day.graph</gmd:URL>\n" + //
        "                  </gmd:linkage>\n" + //
        "                  <gmd:protocol>\n" + //
        "                    <gco:CharacterString>order</gco:CharacterString>\n" + //
        "                  </gmd:protocol>\n" + //
        "                  <gmd:name>\n" + //
        "                    <gco:CharacterString>Make-A-Graph Form</gco:CharacterString>\n" + //
        "                  </gmd:name>\n" + //
        "                  <gmd:description>\n" + //
        "                    <gco:CharacterString>ERDDAP's Make-A-Graph .html web page for this dataset. Create an image with a map or graph of a subset of the data.</gco:CharacterString>\n"
        + //
        "                  </gmd:description>\n" + //
        "                  <gmd:function>\n" + //
        "                    <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"mapDigital\">mapDigital</gmd:CI_OnLineFunctionCode>\n"
        + //
        "                  </gmd:function>\n" + //
        "                </gmd:CI_OnlineResource>\n" + //
        "              </gmd:onLine>\n" + //
        "            </gmd:MD_DigitalTransferOptions>\n" + //
        "          </gmd:distributorTransferOptions>\n" + //
        "        </gmd:MD_Distributor>\n" + //
        "      </gmd:distributor>\n" + //
        "    </gmd:MD_Distribution>\n" + //
        "  </gmd:distributionInfo>\n" + //
        "  <gmd:metadataMaintenance>\n" + //
        "    <gmd:MD_MaintenanceInformation>\n" + //
        "      <gmd:maintenanceAndUpdateFrequency gco:nilReason=\"unknown\"/>\n" + //
        "      <gmd:maintenanceNote>\n" + //
        "        <gco:CharacterString>This record was created from dataset metadata by ERDDAP Version 2.23</gco:CharacterString>\n"
        + //
        "      </gmd:maintenanceNote>\n" + //
        "    </gmd:MD_MaintenanceInformation>\n" + //
        "  </gmd:metadataMaintenance>\n" + //
        "</gmi:MI_Metadata>\n";
    results = results.replaceAll("<gco:Date>....-..-..</gco:Date>", "<gco:Date>YYYY-MM-DD</gco:Date>");
    results = results.replaceAll("<gco:Measure uom=\\\"s\\\">[0-9]+.[0-9]+</gco:Measure>", "<gco:Measure uom=\"s\">VALUE</gco:Measure>");
    results = results.replaceAll("<gml:endPosition>....-..-..T..:00:00Z</gml:endPosition>", "<gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>");
    results = results.replaceAll("<gco:Integer>[0-9]+</gco:Integer>", "<gco:Integer>NUMBER</gco:Integer>");
    Test.ensureEqual(results, expected, "results=" + results);

    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/");
    expected = "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n" + //
        "<tr><th><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n"
        + //
        "<tr><th colspan=\"5\"><hr></th></tr>\n" + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/back.gif\" alt=\"[DIR]\"></td><td><a href=\"&#x2e;&#x2e;\">Parent Directory</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"fgdc&#x2f;\">fgdc/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"iso19115&#x2f;\">iso19115/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><th colspan=\"5\"><hr></th></tr>\n" + //
        "</table>\n" + //
        "3 directories, 0 files";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);

    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/iso19115/");
    expected = "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n" + //
        "<tr><th><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n"
        + //
        "<tr><th colspan=\"5\"><hr></th></tr>\n" + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/back.gif\" alt=\"[DIR]\"></td><td><a href=\"&#x2e;&#x2e;\">Parent Directory</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"xml&#x2f;\">xml/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><th colspan=\"5\"><hr></th></tr>\n" + //
        "</table>\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);

    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/iso19115/xml/");
    expected = "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/xml.gif\" alt=\"[XML]\"></td><td><a rel=\"bookmark\" href=\"erdMH1chlamday&#x5f;iso19115&#x2e;xml\">erdMH1chlamday&#x5f;iso19115&#x2e;xml</a></td><td class=\"R\">DD-MMM-YYYY HH:mm</td><td class=\"R\">53721</td><td>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)</td></tr>";
    results = results.replaceAll("..-...-.... ..:..", "DD-MMM-YYYY HH:mm");
    Test.ensureTrue(results.indexOf(expected) > 0, "No erdMH1chlamday found, results=" + results);
  }

  /** Test the metadata */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testMetadataFgdc() throws Exception {
    String results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/fgdc/xml/erdMWchla1day_fgdc.xml");
    String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
        "<metadata xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.ngdc.noaa.gov/metadata/published/xsd/ngdcSchema/schema.xsd\" >\n"
        + //
        "  <idinfo>\n" + //
        "    <datsetid>localhost:8080:erdMWchla1day</datsetid>\n" + //
        "    <citation>\n" + //
        "      <citeinfo>\n" + //
        "        <origin>\n" + //
        "Project: CoastWatch (https://coastwatch.noaa.gov/)\n" + //
        "Name: NOAA NMFS SWFSC ERD\n" + //
        "Email: erd.data@noaa.gov\n" + //
        "Institution: NOAA NMFS SWFSC ERD\n" + //
        "InfoURL: https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html\n" + //
        "Source URL: (local files)\n" + //
        "        </origin>\n" + //
        "        <origin_cntinfo>\n" + //
        "          <cntinfo>\n" + //
        "            <cntorgp>\n" + //
        "              <cntorg>NOAA NMFS SWFSC ERD</cntorg>\n" + //
        "              <cntper>NOAA NMFS SWFSC ERD</cntper>\n" + //
        "            </cntorgp>\n" + //
        "            <cntemail>erd.data@noaa.gov</cntemail>\n" + //
        "          </cntinfo>\n" + //
        "        </origin_cntinfo>\n" + //
        "        <pubdate>YYYYMMDD</pubdate>\n" + //
        "        <title>Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</title>\n"
        + //
        "        <edition>Unknown</edition>\n" + //
        "        <geoform>raster digital data</geoform>\n" + //
        "        <pubinfo>\n" + //
        "          <pubplace>Nowhere, AK, USA</pubplace>\n" + //
        "          <publish>ERDDAP, version 2.23, at ERDDAP Jetty Install</publish>\n" + //
        "          <publish_cntinfo>\n" + //
        "            <cntinfo>\n" + //
        "              <cntorgp>\n" + //
        "                <cntorg>ERDDAP Jetty Install</cntorg>\n" + //
        "                <cntper>ERDDAP Jetty Developer</cntper>\n" + //
        "              </cntorgp>\n" + //
        "              <cntpos>Software Engineer</cntpos>\n" + //
        "              <cntaddr>\n" + //
        "                <addrtype>Mailing and Physical Address</addrtype>\n" + //
        "                <address>123 Irrelevant St.</address>\n" + //
        "                <city>Nowhere</city>\n" + //
        "                <state>AK</state>\n" + //
        "                <postal>99504</postal>\n" + //
        "                <country>USA</country>\n" + //
        "              </cntaddr>\n" + //
        "              <cntvoice>555-555-5555</cntvoice>\n" + //
        "              <cntemail>nobody@example.com</cntemail>\n" + //
        "            </cntinfo>\n" + //
        "          </publish_cntinfo>\n" + //
        "        </pubinfo>\n" + //
        "        <onlink>http://localhost:8080/erddap/griddap/erdMWchla1day.html</onlink>\n" + //
        "        <onlink>http://localhost:8080/erddap/griddap/erdMWchla1day.graph</onlink>\n" + //
        "        <onlink>http://localhost:8080/erddap/wms/erdMWchla1day/request</onlink>\n" + //
        "        <CI_OnlineResource>\n" + //
        "          <linkage>http://localhost:8080/erddap/griddap/erdMWchla1day.html</linkage>\n" + //
        "          <name>Download data: Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</name>\n"
        + //
        "          <description>A web page for specifying a subset of the dataset and downloading data in any of several file formats.</description>\n"
        + //
        "          <function>download data</function>\n" + //
        "        </CI_OnlineResource>\n" + //
        "        <CI_OnlineResource>\n" + //
        "          <linkage>http://localhost:8080/erddap/griddap/erdMWchla1day.graph</linkage>\n" + //
        "          <name>Make a graph or map: Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</name>\n"
        + //
        "          <description>A web page for creating a graph or map of the data.</description>\n" + //
        "          <function>download graph or map</function>\n" + //
        "        </CI_OnlineResource>\n" + //
        "        <CI_OnlineResource>\n" + //
        "          <linkage>http://localhost:8080/erddap/griddap/erdMWchla1day</linkage>\n" + //
        "          <name>OPeNDAP service: Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</name>\n"
        + //
        "          <description>The base URL for the OPeNDAP service.  Add .html to get a web page with a form to download data. Add .dds to get the dataset's structure. Add .das to get the dataset's metadata. Add .dods to download data via the OPeNDAP protocol.</description>\n"
        + //
        "          <function>OPeNDAP</function>\n" + //
        "        </CI_OnlineResource>\n" + //
        "        <CI_OnlineResource>\n" + //
        "          <linkage>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</linkage>\n" + //
        "          <name>Background information: Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</name>\n"
        + //
        "          <description>Background information for the dataset.</description>\n" + //
        "          <function>background information</function>\n" + //
        "        </CI_OnlineResource>\n" + //
        "        <CI_OnlineResource>\n" + //
        "          <linkage>http://localhost:8080/erddap/wms/erdMWchla1day/request</linkage>\n" + //
        "          <name>WMS service: Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL, 2002-present (1 Day Composite)</name>\n"
        + //
        "          <description>The base URL for the WMS service for this dataset.</description>\n" + //
        "          <function>WMS</function>\n" + //
        "        </CI_OnlineResource>\n" + //
        "        <lworkcit>\n" + //
        "          <citeinfo>\n" + //
        "            <origin>CoastWatch (https://coastwatch.noaa.gov/)</origin>\n" + //
        "          </citeinfo>\n" + //
        "        </lworkcit>\n" + //
        "      </citeinfo>\n" + //
        "    </citation>\n" + //
        "    <descript>\n" + //
        "      <abstract>NOAA CoastWatch distributes chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  The algorithm currently used in processing the water leaving radiance to chlorophyll concentration has not yet been accepted as science quality.  In addition, assumptions are made in the atmospheric correction in order to provide the data in a timely manner.</abstract>\n"
        + //
        "      <purpose>Unknown</purpose>\n" + //
        "      <supplinf>https://coastwatch.pfeg.noaa.gov/infog/MW_chla_las.html</supplinf>\n" + //
        "    </descript>\n"; //
        results = results.replaceAll("<pubdate>........</pubdate>", "<pubdate>YYYYMMDD</pubdate>");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/fgdc");
    expected = "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n" + //
        "<tr><th><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n"
        + //
        "<tr><th colspan=\"5\"><hr></th></tr>\n" + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/back.gif\" alt=\"[DIR]\"></td><td><a href=\"&#x2e;&#x2e;\">Parent Directory</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"xml&#x2f;\">xml/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
        + //
        "<tr><th colspan=\"5\"><hr></th></tr>\n" + //
        "</table>\n" + //
        "2 directories, 0 files";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);

    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/metadata/fgdc/xml/");
    expected = "<tr><td><img class=\"B\" src=\"http://localhost:8080/erddap/images/fileIcons/xml.gif\" alt=\"[XML]\"></td><td><a rel=\"bookmark\" href=\"erdMH1chlamday&#x5f;fgdc&#x2e;xml\">erdMH1chlamday&#x5f;fgdc&#x2e;xml</a></td><td class=\"R\">DD-MMM-YYYY HH:mm</td><td class=\"R\">71098</td><td>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)</td></tr>\n";
    ;
    results = results.replaceAll("..-...-.... ..:..", "DD-MMM-YYYY HH:mm");
    Test.ensureTrue(results.indexOf(expected) > 0, "No erdMH1chlamday found, results=" + results);
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testSitemap() throws Exception {
    String results = SSR.getUrlResponseStringUnchanged("http://localhost:" + PORT + "/erddap/sitemap.xml");
    String expected = "<?xml version='1.0' encoding='UTF-8'?>\n" + //
        "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/categorize/index.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/index.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/oceanicAtmosphericAcronyms.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/oceanicAtmosphericVariableNames.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/fipscounty.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/keywords.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/time.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/units.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/convert/urls.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/griddap/documentation.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/griddap/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/images/embed.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/index.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/info/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/information.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/metadata/fgdc/xml/</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.3</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/metadata/iso19115/xml/</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.3</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/legal.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/rest.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/search/advanced.html?page=1&#x26;itemsPerPage=1000000000</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/search/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/slidesorter.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/subscriptions/index.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/subscriptions/add.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/subscriptions/validate.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/subscriptions/list.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/subscriptions/remove.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/tabledap/documentation.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/tabledap/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/wms/documentation.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/wms/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.7</priority>\n" + //
        "</url>\n";

    results = results.replaceAll("<lastmod>....-..-..</lastmod>", "<lastmod>YYYY-MM-DD</lastmod>");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);

    String expected2 = "<url>\n" + //
        "<loc>http://localhost:8080/erddap/griddap/erdMWchla1day.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/info/erdMWchla1day/index.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/griddap/erdMWchla1day.graph</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.5</priority>\n" + //
        "</url>\n" + //
        "\n" + //
        "<url>\n" + //
        "<loc>http://localhost:8080/erddap/wms/erdMWchla1day/index.html</loc>\n" + //
        "<lastmod>YYYY-MM-DD</lastmod>\n" + //
        "<changefreq>monthly</changefreq>\n" + //
        "<priority>0.3</priority>\n" + //
        "</url>\n";
    int startIndex = results.indexOf(expected2.substring(0, 72));
    Test.ensureEqual(results.substring(startIndex, startIndex + expected2.length()), expected2,
        "results=" + results);
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testInterpolate() throws Exception {
    // TODO get a request that has actual interpolation in it
    String results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT
            + "/erddap/convert/interpolate.json?TimeLatLonTable=time%2Clatitude%2Clongitude%0A2006-04-17T06%3A00%3A00Z%2C35.580%2C-122.550%0A2006-04-17T12%3A00%3A00Z%2C35.576%2C-122.553%0A2006-04-17T18%3A00%3A00Z%2C35.572%2C-122.568%0A2007-01-02T00%3A00%3A00Z%2C35.569%2C-122.571%0A&requestCSV=erdMH1chla8day%2Fchlorophyll%2FBilinear%2F4");
    String expected = "{\n" + //
        "  \"table\": {\n" + //
        "    \"columnNames\": [\"time\", \"latitude\", \"longitude\", \"erdMH1chla8day_chlorophyll_Bilinear_4\"],\n" + //
        "    \"columnTypes\": [\"String\", \"double\", \"double\", \"double\"],\n" + //
        "    \"rows\": [\n" + //
        "      [\"2006-04-17T06:00:00Z\", 35.58, -122.55, null],\n" + //
        "      [\"2006-04-17T12:00:00Z\", 35.576, -122.553, null],\n" + //
        "      [\"2006-04-17T18:00:00Z\", 35.572, -122.568, null],\n" + //
        "      [\"2007-01-02T00:00:00Z\", 35.569, -122.571, null]\n" + //
        "    ]\n" + //
        "  }\n" + //
        "}\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // Request an html page, to test the html generation
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT
            + "/erddap/convert/interpolate.htmlTable?TimeLatLonTable=time%2Clatitude%2Clongitude%0A2006-04-17T06%3A00%3A00Z%2C35.580%2C-122.550%0A2006-04-17T12%3A00%3A00Z%2C35.576%2C-122.553%0A2006-04-17T18%3A00%3A00Z%2C35.572%2C-122.568%0A2007-01-02T00%3A00%3A00Z%2C35.569%2C-122.571%0A&requestCSV=erdMH1chla8day%2Fchlorophyll%2FBilinear%2F4");
    expected = "<table class=\"erd commonBGColor nowrap\">\n" + //
        "<tr>\n" + //
        "<th>time\n" + //
        "<th>latitude\n" + //
        "<th>longitude\n" + //
        "<th>erdMH1chla8day_chlorophyll_Bilinear_4\n" + //
        "</tr>\n" + //
        "<tr>\n" + //
        "<td>2006-04-17T06:00:00Z\n" + //
        "<td class=\"R\">35.58\n" + //
        "<td class=\"R\">-122.55\n" + //
        "<td>\n" + //
        "</tr>\n" + //
        "<tr>\n" + //
        "<td>2006-04-17T12:00:00Z\n" + //
        "<td class=\"R\">35.576\n" + //
        "<td class=\"R\">-122.553\n" + //
        "<td>\n" + //
        "</tr>\n" + //
        "<tr>\n" + //
        "<td>2006-04-17T18:00:00Z\n" + //
        "<td class=\"R\">35.572\n" + //
        "<td class=\"R\">-122.568\n" + //
        "<td>\n" + //
        "</tr>\n" + //
        "<tr>\n" + //
        "<td>2007-01-02T00:00:00Z\n" + //
        "<td class=\"R\">35.569\n" + //
        "<td class=\"R\">-122.571\n" + //
        "<td>\n" + //
        "</tr>\n" + //
        "</table>\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);
  }

  /* TableTests */

  /**
   * Test saveAsJson and readJson.
   *
   * @throws Exception if trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testJson() throws Exception {
    // String2.log("\n***** Table.testJson");
    // Table.verbose = true;
    // Table.reallyVerbose = true;

    // generate some data
    Table table = TableTests.getTestTable(true, true);

    // write it to a file
    String fileName = TEMP_DIR.toAbsolutePath().toString() + "/tempTable.json";
    table.saveAsJson(fileName, 0, true);
    // String2.log(fileName + "=\n" + File2.readFromFile(fileName)[1]);
    // Test.displayInBrowser("file://" + fileName); //.json

    // read it from the file
    String results = File2.directReadFromUtf8File(fileName);
    Test.ensureEqual(results,
        "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Time\", \"Longitude\", \"Latitude\", \"Double Data\", \"Long Data\", \"Int Data\", \"Short Data\", \"Byte Data\", \"Char Data\", \"String Data\"],\n"
            +
            "    \"columnTypes\": [\"String\", \"int\", \"float\", \"double\", \"long\", \"int\", \"short\", \"byte\", \"char\", \"String\"],\n"
            +
            "    \"columnUnits\": [\"UTC\", \"degrees_east\", \"degrees_north\", \"doubles\", \"longs\", \"ints\", \"shorts\", \"bytes\", \"chars\", \"Strings\"],\n"
            +
            "    \"rows\": [\n" +
            "      [\"1970-01-01T00:00:00Z\", -3, 1.0, -1.0E300, -2000000000000000, -2000000000, -32000, -120, \",\", \"a\"],\n"
            +
            "      [\"2005-08-31T16:01:02Z\", -2, 1.5, 3.123, 2, 2, 7, 8, \"\\\"\", \"bb\"],\n" +
            "      [\"2005-11-02T18:04:09Z\", -1, 2.0, 1.0E300, 2000000000000000, 2000000000, 32000, 120, \"\\u20ac\", \"ccc\"],\n"
            +
            "      [null, null, null, null, null, null, null, null, \"\", \"\"]\n" +
            "    ]\n" +
            "  }\n" +
            "}\n",
        results);

    // read it
    Table table2 = new Table();
    table2.readJson(fileName);
    Test.ensureTrue(table.equals(table2), "");

    // finally
    File2.delete(fileName);

    // ******************* test readErddapInfo
    // String tUrl = "https://coastwatch.pfeg.noaa.gov/erddap2";
    // http://localhost:" + PORT + "/erddap/info/pmelTaoDySst/index.json
    String tUrl = "http://localhost:" + PORT + "/erddap";
    table.readErddapInfo(tUrl + "/info/pmelTaoDySst/index.json");
    String ncHeader = table.getNCHeader("row");
    Test.ensureEqual(table.globalAttributes().getString("cdm_data_type"), "TimeSeries", ncHeader);
    Test.ensureEqual(table.globalAttributes().getString("title"),
        "TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature",
        ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").size(), 3, ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").getString(0),
        "This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.",
        ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").getString(1),
        "This dataset is a product of the TAO Project Office at NOAA/PMEL.",
        ncHeader);
    Test.ensureLinesMatch(table.globalAttributes().get("history").getString(2),
        "20\\d{2}-\\d{2}-\\d{2} Bob Simons at NOAA/NMFS/SWFSC/ERD \\(bob.simons@noaa.gov\\) " +
            "fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  "
            +
            "Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.",
        ncHeader);
    Test.ensureEqual(table.nColumns(), 10, ncHeader);
    Test.ensureEqual(table.findColumnNumber("longitude"), 3, ncHeader);
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "degrees_east", ncHeader);
    int t25Col = table.findColumnNumber("T_25");
    Test.ensureTrue(t25Col > 0, ncHeader);
    Test.ensureEqual(table.columnAttributes(t25Col).getString("ioos_category"), "Temperature", ncHeader);
    Test.ensureEqual(table.columnAttributes(t25Col).getString("units"), "degree_C", ncHeader);
  }

  /* FileVisitorDNLS */

  /**
   * This tests a WAF-related (Web Accessible Folder) methods on an ERDDAP "files"
   * directory.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testErddap1FilesWAF2() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testErddapFilesWAF2()\n");

    // *** test localhost
    String2.log("\nThis test requires erdMWchla1day in localhost erddap.");
    String url = "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/";
    String tFileNameRegex = "MW200219.*\\.nc(|\\.gz)";
    boolean tRecursive = true;
    String tPathRegex = ".*";
    boolean tDirsToo = true;
    Table table = FileVisitorDNLS.makeEmptyTable();
    StringArray dirs = (StringArray) table.getColumn(0);
    StringArray names = (StringArray) table.getColumn(1);
    LongArray lastModifieds = (LongArray) table.getColumn(2);
    LongArray sizes = (LongArray) table.getColumn(3);

    // * test all features
    String results = FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
        url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo,
        dirs, names, lastModifieds, sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    table.removeColumn("lastModified");
    table.removeColumn("size");
    results = table.dataToString();
    String expected = "directory,name\n" +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002190_2002190_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002191_2002191_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002192_2002192_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002193_2002193_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002194_2002194_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002195_2002195_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002196_2002196_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002197_2002197_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002198_2002198_chla.nc\n"
        +
        "http://localhost:" + PORT + "/erddap/files/erdMWchla1day/,MW2002199_2002199_chla.nc\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /* TestSSR */

  /**
   * Test posting info and getting response.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testPostFormGetResponseString() throws Exception {
    String s = SSR.postFormGetResponseString(
        "https://coastwatch.pfeg.noaa.gov/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=jplmursst41");
    String2.log("\nSSR.testPostFormGetResponseString() result:\n" + s);
    Test.ensureTrue(s.indexOf("Do a Full Text Search for Datasets:") >= 0, "");
    Test.ensureTrue(s.indexOf("Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global") >= 0,
        "");
    Test.ensureTrue(s.indexOf("ERDDAP, Version") >= 0, "");

    // 2018-10-24 I verified that
    // * This request appears as a POST (not GET) in tomcat's
    // localhost_access_lot[date].txt
    // * The parameters don't appear in that file (whereas they do for GET requests)
    // * The parameters don't appear in ERDDAP log (whereas they do for GET
    // requests),
    // * and it is labelled as a POST request.
    s = SSR.postFormGetResponseString(
        "http://localhost:" + PORT
            + "/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=jplmursst41");
    String2.log("\nSSR.testPostFormGetResponseString() result:\n" + s);
    Test.ensureTrue(s.indexOf("Do a Full Text Search for Datasets:") >= 0, "");
    Test.ensureTrue(s.indexOf("Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global") >= 0,
        "This test requires MUR 4.1 in the local host ERDDAP.");
    Test.ensureTrue(s.indexOf("ERDDAP, Version") >= 0, "");
  }

  /* TranslateMessages */

  /**
   * This checks lots of webpages on localhost ERDDAP for uncaught special text
   * (&amp;term; or ZtermZ).
   * This REQUIRES localhost ERDDAP be running with at least
   * <datasetsRegex>(etopo.*|jplMURSST41|cwwcNDBCMet)</datasetsRegex>.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void checkForUncaughtSpecialText() throws Exception {
    String2.log("\n*** TranslateMessages.checkForUncaughtSpecialText()\n" +
        "THIS REQUIRES localhost ERDDAP with at least (etopo.*|jplMURSST41|cwwcNDBCMet)");
    String tErddapUrl = "http://localhost:" + PORT + "/erddap/de/";
    String pages[] = {
        "index.html",
        "categorize/cdm_data_type/grid/index.html?page=1&itemsPerPage=1000",
        "convert/index.html",
        "convert/oceanicAtmosphericAcronyms.html",
        "convert/fipscounty.html",
        "convert/keywords.html",
        "convert/time.html",
        // "convert/units.html", // Disable this check because &C is in translations
        // wrong (this is a minor mistake not an incorrect translation)
        "convert/urls.html",
        "convert/oceanicAtmosphericVariableNames.html",
        "dataProviderForm.html",
        "dataProviderForm1.html",
        "dataProviderForm2.html",
        "dataProviderForm3.html",
        "dataProviderForm4.html",
        // "download/AccessToPrivateDatasets.html",
        // "download/changes.html",
        // "download/EDDTableFromEML.html",
        // "download/grids.html",
        // "download/NCCSV.html",
        // "download/NCCSV_1.00.html",
        // "download/setup.html",
        // "download/setupDatasetsXml.html",
        "files/",
        "files/cwwcNDBCMet/",
        "files/documentation.html",
        "griddap/documentation.html",
        "griddap/jplMURSST41.graph",
        "griddap/jplMURSST41.html",
        // "info/index.html?page=1&itemsPerPage=1000", // Descriptions of datasets may
        // contain char patterns
        "info/cwwcNDBCMet/index.html",
        "information.html",
        "opensearch1.1/index.html",
        "rest.html",
        "search/index.html?page=1&itemsPerPage=1000&searchFor=sst",
        "slidesorter.html",
        "subscriptions/index.html",
        "subscriptions/add.html",
        "subscriptions/validate.html",
        "subscriptions/list.html",
        "subscriptions/remove.html",
        "tabledap/documentation.html",
        "tabledap/cwwcNDBCMet.graph",
        "tabledap/cwwcNDBCMet.html",
        "tabledap/cwwcNDBCMet.subset",
        "wms/documentation.html",
        "wms/jplMURSST41/index.html" };
    StringBuilder results = new StringBuilder();
    for (int i = 0; i < pages.length; i++) {
      String content;
      String sa[];
      HashSet<String> hs;
      try {
        content = SSR.getUrlResponseStringUnchanged(tErddapUrl + pages[i]);
      } catch (Exception e) {
        results.append("\n* Trouble: " + e.toString() + "\n");
        continue;
      }

      // Look for ZsomethingZ
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(Z[a-zA-Z0-9]Z)", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " ZtermsZ :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for &something; that are placeholders that should have been replaced by
      // replaceAll().
      // There are some legit uses in changes.html, setup.html, and
      // setupDatasetsXml.html.
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(&amp;[a-zA-Z]+?;)", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " &entities; :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for {0}, {1}, etc that should have been replaced by replaceAll().
      // There are some legit values on setupDatasetsXml.html in regexes ({nChar}:
      // 12,14,4,6,7,8).
      hs = new HashSet();
      hs.addAll(Arrays.asList(
          String2.extractAllCaptureGroupsAsHashSet(content, "(\\{\\d+\\})", 1).toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " {#} :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }
    }
    if (results.length() > 0)
      throw new RuntimeException(results.toString());
  }

  /* Erddap */

  /**
   * This is used by Bob to do simple tests of the basic Erddap services
   * from the ERDDAP at EDStatic.erddapUrl. It assumes Bob's test datasets are
   * available.
   *
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testErddapBasic() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testBasic");
    int po;
    int language = 0;
    EDStatic.sosActive = false; // currently, never true because sos is unfinished //some other tests may have
                                // left this as true

    // home page
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl); // redirects to index.html
    expected = "The small effort to set up ERDDAP brings many benefits.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/"); // redirects to index.html
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // test version info (opendap spec section 7.2.5)
    // "version" instead of datasetID
    expected = "Core Version: DAP/2.0\n" +
        "Server Version: dods/3.7\n" +
        "ERDDAP_version: " + EDStatic.erddapVersion + "\n";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // "version.txt"
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/version.txt");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/version.txt");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ".ver"
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/etopo180.ver");
    Test.ensureEqual(results, expected, "results=\n" + results);
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.ver");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // help
    expected = "griddap to Request Data and Graphs from Gridded Datasets";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMHchla8day.help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    expected = "tabledap to Request Data and Graphs from Tabular Datasets";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.help");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // error 404
    results = "";
    try {
      SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/gibberish");
    } catch (Throwable t) {
      results = t.toString();
    }
    Test.ensureTrue(results.indexOf("java.io.FileNotFoundException") >= 0, "results=\n" + results);

    // info list all datasets
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Argo float vertical profiles from Coriolis Global Data Assembly Centres") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.csv?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") < 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Argo float vertical profiles from Coriolis Global Data Assembly Centres") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.tsv");
    Test.ensureTrue(results.indexOf("\t") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    // search
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Do a Full Text Search for Datasets") >= 0, "results=\n" + results);
    // index.otherFileType must have ?searchFor=...

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">Global Temperature and Salinity Profile Programme (GTSPP) Data, 1985-present\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">GLOBEC NEP Rosette Bottle Data (2002)") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.htmlTable?" +
        EDStatic.defaultPIppQuery + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Global Temperature and Salinity Profile Programme (GTSPP) Data, 1985-present") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.html?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
            ">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\n") > 0,
        "results=\n" + results);

    // .json
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel+sst");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
        +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
        +
        "    \"rows\": [\n" +
        "" +
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n"
        + //
        "      [\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"],\n"
        + //
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"],\n"
        + //
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n"
        +
        "    ]\n" +
        "  }\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .json with jsonp
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.json?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel+sst&.jsonp=fnName");
    expected = "fnName({\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
        +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
        +
        "    \"rows\": [\n" +
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n"
        + //
        "      [\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"],\n"
        + //
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"],\n"
        + //
        "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n"
        + //
        "    ]\n" +
        "  }\n" +
        "}\n" +
        ")";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // and read the header to see the mime type
    results = String2.toNewlineString(
        SSR.dosOrCShell("curl -i \"" + EDStatic.erddapUrl + "/search/index.json?" +
            EDStatic.defaultPIppQuery + "&searchFor=tao+pmel&.jsonp=fnName\"",
            120).toArray());
    po = results.indexOf("HTTP");
    results = results.substring(po);
    po = results.indexOf("chunked");
    results = results.substring(0, po + 7);
    expected = "HTTP/1.1 200 OK\n" +
        "Server: Jetty(12.0.8)\n" +
        "Date: Today\n" +
        "Content-Type: application/javascript;charset=utf-8\n" +
        "Content-Encoding: identity\n" +
        "Transfer-Encoding: chunked";
    results = results.replaceAll("Date: ..., .. [a-zA-Z]+ .... ..:..:.. ...\n", "Date: Today\n");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV1
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV1?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "[\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"]\n"
        +
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAdcp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP\", \"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "u_1205 (Eastward Sea Water Velocity, cm/s)\\n" + //
        "QU_5205 (Eastward Sea Water Velocity Quality)\\n" + //
        "v_1206 (Northward Sea Water Velocity, cm/s)\\n" + //
        "QV_5206 (Northward Sea Water Velocity Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAdcp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAdcp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAdcp/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAdcp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAdcp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAdcp\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyCur/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents\", \"This dataset has daily Currents data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "U_320 (Eastward Sea Water Velocity, cm s-1)\\n" + //
        "V_321 (Northward Sea Water Velocity, cm s-1)\\n" + //
        "CS_300 (Sea Water Velocity, cm s-1)\\n" + //
        "CD_310 (Direction of Sea Water Velocity, degrees_true)\\n" + //
        "QCS_5300 (Current Speed Quality)\\n" + //
        "QCD_5310 (Current Direction Quality)\\n" + //
        "SCS_6300 (Current Speed Source)\\n" + //
        "CIC_7300 (Current Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyCur_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyCur_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyCur/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyCur.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyCur&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyCur\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyT/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Temperature\", \"This dataset has daily Temperature data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_20 (Sea Water Temperature, degree_C)\\n" + //
        "QT_5020 (Temperature Quality)\\n" + //
        "ST_6020 (Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyT_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyT_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyT/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyT.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyT&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyT\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyW/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind\", \"This dataset has daily Wind data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "WU_422 (Zonal Wind, m s-1)\\n" + //
        "WV_423 (Meridional Wind, m s-1)\\n" + //
        "WS_401 (Wind Speed, m s-1)\\n" + //
        "QWS_5401 (Wind Speed Quality)\\n" + //
        "SWS_6401 (Wind Speed Source)\\n" + //
        "WD_410 (Wind Direction, degrees_true)\\n" + //
        "QWD_5410 (Wind Direction Quality)\\n" + //
        "SWD_6410 (Wind Direction Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyW_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyW_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyW/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyW.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyW&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyW\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Position\", \"This dataset has daily Position data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LON_502 (Precise Longitude, degree_east)\\n" + //
        "QX_5502 (Longitude Quality)\\n" + //
        "LAT_500 (Precise Latitude, degree_north)\\n" + //
        "QY_5500 (Latitude Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyPos/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyPos\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyS/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Salinity\", \"This dataset has daily Salinity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
        "QS_5041 (Salinity Quality)\\n" + //
        "SS_6041 (Salinity Source)\\n" + //
        "SIC_8041 (Salinity Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyS_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyS_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyS/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyS.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyS&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyS\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEvap/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Evaporation\", \"This dataset has daily Evaporation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "E_250 (Evaporation, MM/HR)\\n" + //
        "QE_5250 (Evaporation Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEvap_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEvap_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEvap/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEvap.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEvap&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEvap\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyTau/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress\", \"This dataset has daily Wind Stress data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "TX_442 (Zonal Wind Stress, N/m2)\\n" + //
        "TY_443 (Meridional Wind Stress, N/m2)\\n" + //
        "TAU_440 (Wind Stress, N/m2)\\n" + //
        "TD_445 (Wind Stress Direction, degrees_true)\\n" + //
        "QTAU_5440 (Wind Stress Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyTau_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyTau_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyTau/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyTau.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyTau&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyTau\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"This dataset has monthly Position data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LON_502 (Precise Longitude, degree_east)\\n" + //
        "QX_5502 (Longitude Quality)\\n" + //
        "LAT_500 (Precise Latitude, degree_north)\\n" + //
        "QY_5500 (Latitude Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoMonPos\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyIso/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, 20C Isotherm Depth\", \"This dataset has daily 20C Isotherm Depth data (the depth at which the ocean temperature is 20C) from the \\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "ISO_6 (20C Isotherm Depth, m)\\n" + //
        "QI_5006 (20C Depth Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyIso_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyIso_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyIso/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyIso.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyIso&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyIso\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"This dataset has daily Air Temperature data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "AT_21 (Air Temperature, degree_C)\\n" + //
        "QAT_5021 (Air Temperature Quality)\\n" + //
        "SAT_6021 (Air Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAirt\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyDyn/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height\", \"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "DYN_13 (Dynamic Height, dyn-cm)\\n" + //
        "QD_5013 (Dynamic Height Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyDyn_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyDyn_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyDyn/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyDyn.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyDyn&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyDyn\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyHeat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Heat Content\", \"This dataset has daily Heat Content data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "HTC_130 (Heat Content, 10**10 J m-2)\\n" + //
        "HTC_5130 (Heat Content Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyHeat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyHeat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyHeat/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyHeat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyHeat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyHeat\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQlat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Latent Heat Flux\", \"This dataset has daily Latent Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QL_137 (Latent Heat Flux, W m-2)\\n" + //
        "QQL_5137 (Latent Heat Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQlat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQlat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQlat/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQlat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQlat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQlat\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRh/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Relative Humidity\", \"This dataset has daily Relative Humidity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RH_910 (Relative Humidity, percent)\\n" + //
        "QRH_5910 (Relative Humidity Quality)\\n" + //
        "SRH_6910 (Relative Humidity Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRh_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRh_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRh/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRh.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRh&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRh\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQsen/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Sensible Heat Flux\", \"This dataset has daily Sensible Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QS_138 (Sensible Heat Flux, W m-2)\\n" + //
        "QQS_5138 (Sensible Heat Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQsen_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQsen_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQsen/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQsen.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQsen&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQsen\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySss/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity\", \"This dataset has daily Sea Surface Salinity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
        "QS_5041 (Salinity Quality)\\n" + //
        "SS_6041 (Salinity Source)\\n" + //
        "SIC_7041 (Salinity Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySss_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySss_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySss/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySss.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySss&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySss\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Heat Flux Due To Rain\", \"This dataset has daily Heat Flux Due To Rain data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QR_139 (Heat Flux Due To Rain, W m-2)\\n" + //
        "QQR_5139 (Heat Flux Due To Rain Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRf/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRf\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRain/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation\", \"This dataset has daily Precipitation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RN_485 (Precipitation, MM/HR)\\n" + //
        "QRN_5485 (Precipitation Quality)\\n" + //
        "SRN_6485 (Precipitation Source)\\n" + //
        "RNS_486 (Precipitation Standard Deviation, MM/HR)\\n" + //
        "RNP_487 (Percent Time Raining, percent)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRain_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRain_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRain/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRain.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRain&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRain\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Buoyancy Flux\", \"This dataset has daily Buoyancy Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "BF_191 (Buoyancy Flux, 10**6 kg m-2 s-1)\\n" + //
        "QBF_5191 (Buoyancy Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBf/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBf\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLw/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Longwave Radiation\", \"This dataset has daily Incoming Longwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "Ql_136 (Incoming Longwave Radiation, W m-2)\\n" + //
        "QLW_5136 (Longwave Radiation Quality)\\n" + //
        "SLW_6136 (Longwave Radiation Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLw_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLw_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLw/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLw.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLw&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLw\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Total Heat Flux\", \"This dataset has daily Total Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QT_210 (Total Heat Flux, W/M**2)\\n" + //
        "QQ0_5210 (Total Heat Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQnet/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQnet\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRad/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Downgoing Shortwave Radiation\", \"This dataset has daily Downgoing Shortwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RD_495 (Downgoing Shortwave Radiation, W/M**2)\\n" + //
        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
        "SSW_6495 (Shortwave Radiation Source)\\n" + //
        "RDS_496 (Shortwave Radiation Standard Deviation, W/M**2)\\n" + //
        "RDP_497 (Shortwave Radiation Peak, W/M**2)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRad_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRad_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRad/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRad.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRad&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRad\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Net Shortwave Radiation\", \"This dataset has daily Net Shortwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "SWN_1495 (Net Shortwave Radiation, W/M**2)\\n" + //
        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySwnet/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySwnet\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEmp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Evaporation Minus Precipitation\", \"This dataset has daily Evaporation Minus Precipitation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "EMP_251 (Evaporation Minus Precipitation, mm/hr)\\n" + //
        "QEMP_5251 (Evaporation Minus Precipitation Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEmp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEmp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEmp/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEmp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEmp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEmp\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1998-present, Barometric (Air) Pressure\", \"This dataset has daily Barometric (Air) Pressure data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "BP_915 (Barometric (Air) Pressure, hPa)\\n" + //
        "QBP_5915 (Barometric (Air) Pressure Quality)\\n" + //
        "SBP_6915 (Barometric (Air) Pressure Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBp/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBp\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation\", \"This dataset has daily Net Longwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LWN_1136 (Net Longwave Radiation, W m-2)\\n" + //
        "QLW_5136 (Longwave Radiation Quality)\\n" + //
        "SLW_6136 (Longwave Radiation Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLwnet/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLwnet\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"\", \"http://localhost:8080/erddap/files/testTableWithDepth/\", \"This is EDDTableWithDepth\", \"This is the summary\\n"
        + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "AT_21 (Air Temperature, degree_C)\\n" + //
        "QAT_5021 (Air Temperature Quality)\\n" + //
        "SAT_6021 (Air Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"testTableWithDepth\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlCSV?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAdcp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP\", \"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "u_1205 (Eastward Sea Water Velocity, cm/s)\\n" + //
        "QU_5205 (Eastward Sea Water Velocity Quality)\\n" + //
        "v_1206 (Northward Sea Water Velocity, cm/s)\\n" + //
        "QV_5206 (Northward Sea Water Velocity Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAdcp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAdcp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAdcp/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAdcp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAdcp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAdcp\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyCur/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents\", \"This dataset has daily Currents data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "U_320 (Eastward Sea Water Velocity, cm s-1)\\n" + //
        "V_321 (Northward Sea Water Velocity, cm s-1)\\n" + //
        "CS_300 (Sea Water Velocity, cm s-1)\\n" + //
        "CD_310 (Direction of Sea Water Velocity, degrees_true)\\n" + //
        "QCS_5300 (Current Speed Quality)\\n" + //
        "QCD_5310 (Current Direction Quality)\\n" + //
        "SCS_6300 (Current Speed Source)\\n" + //
        "CIC_7300 (Current Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyCur_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyCur_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyCur/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyCur.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyCur&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyCur\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyT.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyT/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Temperature\", \"This dataset has daily Temperature data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_20 (Sea Water Temperature, degree_C)\\n" + //
        "QT_5020 (Temperature Quality)\\n" + //
        "ST_6020 (Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyT_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyT_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyT/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyT.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyT&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyT\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyW.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyW/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind\", \"This dataset has daily Wind data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "WU_422 (Zonal Wind, m s-1)\\n" + //
        "WV_423 (Meridional Wind, m s-1)\\n" + //
        "WS_401 (Wind Speed, m s-1)\\n" + //
        "QWS_5401 (Wind Speed Quality)\\n" + //
        "SWS_6401 (Wind Speed Source)\\n" + //
        "WD_410 (Wind Direction, degrees_true)\\n" + //
        "QWD_5410 (Wind Direction Quality)\\n" + //
        "SWD_6410 (Wind Direction Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyW_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyW_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyW/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyW.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyW&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyW\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Position\", \"This dataset has daily Position data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LON_502 (Precise Longitude, degree_east)\\n" + //
        "QX_5502 (Longitude Quality)\\n" + //
        "LAT_500 (Precise Latitude, degree_north)\\n" + //
        "QY_5500 (Latitude Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyPos/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyPos\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyS.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyS/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Salinity\", \"This dataset has daily Salinity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
        "QS_5041 (Salinity Quality)\\n" + //
        "SS_6041 (Salinity Source)\\n" + //
        "SIC_8041 (Salinity Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyS_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyS_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyS/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyS.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyS&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyS\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEvap/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Evaporation\", \"This dataset has daily Evaporation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "E_250 (Evaporation, MM/HR)\\n" + //
        "QE_5250 (Evaporation Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEvap_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEvap_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEvap/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEvap.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEvap&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEvap\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyTau/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress\", \"This dataset has daily Wind Stress data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "TX_442 (Zonal Wind Stress, N/m2)\\n" + //
        "TY_443 (Meridional Wind Stress, N/m2)\\n" + //
        "TAU_440 (Wind Stress, N/m2)\\n" + //
        "TD_445 (Wind Stress Direction, degrees_true)\\n" + //
        "QTAU_5440 (Wind Stress Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyTau_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyTau_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyTau/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyTau.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyTau&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyTau\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySsd/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySsd\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"This dataset has monthly Position data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LON_502 (Precise Longitude, degree_east)\\n" + //
        "QX_5502 (Longitude Quality)\\n" + //
        "LAT_500 (Precise Latitude, degree_north)\\n" + //
        "QY_5500 (Latitude Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoMonPos\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyIso/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, 20C Isotherm Depth\", \"This dataset has daily 20C Isotherm Depth data (the depth at which the ocean temperature is 20C) from the \\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "ISO_6 (20C Isotherm Depth, m)\\n" + //
        "QI_5006 (20C Depth Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyIso_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyIso_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyIso/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyIso.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyIso&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyIso\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"This dataset has daily Air Temperature data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "AT_21 (Air Temperature, degree_C)\\n" + //
        "QAT_5021 (Air Temperature Quality)\\n" + //
        "SAT_6021 (Air Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAirt\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyDyn/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height\", \"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "DYN_13 (Dynamic Height, dyn-cm)\\n" + //
        "QD_5013 (Dynamic Height Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyDyn_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyDyn_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyDyn/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyDyn.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyDyn&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyDyn\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyHeat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Heat Content\", \"This dataset has daily Heat Content data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "HTC_130 (Heat Content, 10**10 J m-2)\\n" + //
        "HTC_5130 (Heat Content Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyHeat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyHeat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyHeat/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyHeat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyHeat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyHeat\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQlat/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Latent Heat Flux\", \"This dataset has daily Latent Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QL_137 (Latent Heat Flux, W m-2)\\n" + //
        "QQL_5137 (Latent Heat Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQlat_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQlat_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQlat/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQlat.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQlat&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQlat\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRh/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Relative Humidity\", \"This dataset has daily Relative Humidity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RH_910 (Relative Humidity, percent)\\n" + //
        "QRH_5910 (Relative Humidity Quality)\\n" + //
        "SRH_6910 (Relative Humidity Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRh_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRh_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRh/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRh.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRh&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRh\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQsen/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Sensible Heat Flux\", \"This dataset has daily Sensible Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QS_138 (Sensible Heat Flux, W m-2)\\n" + //
        "QQS_5138 (Sensible Heat Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQsen_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQsen_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQsen/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQsen.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQsen&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQsen\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySss.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySss/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity\", \"This dataset has daily Sea Surface Salinity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
        "QS_5041 (Salinity Quality)\\n" + //
        "SS_6041 (Salinity Source)\\n" + //
        "SIC_7041 (Salinity Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySss_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySss_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySss/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySss.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySss&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySss\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Heat Flux Due To Rain\", \"This dataset has daily Heat Flux Due To Rain data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QR_139 (Heat Flux Due To Rain, W m-2)\\n" + //
        "QQR_5139 (Heat Flux Due To Rain Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRf/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRf\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRain/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation\", \"This dataset has daily Precipitation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RN_485 (Precipitation, MM/HR)\\n" + //
        "QRN_5485 (Precipitation Quality)\\n" + //
        "SRN_6485 (Precipitation Source)\\n" + //
        "RNS_486 (Precipitation Standard Deviation, MM/HR)\\n" + //
        "RNP_487 (Percent Time Raining, percent)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRain_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRain_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRain/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRain.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRain&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRain\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBf/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Buoyancy Flux\", \"This dataset has daily Buoyancy Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "BF_191 (Buoyancy Flux, 10**6 kg m-2 s-1)\\n" + //
        "QBF_5191 (Buoyancy Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBf_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBf_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBf/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBf.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBf&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBf\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLw/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Longwave Radiation\", \"This dataset has daily Incoming Longwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "Ql_136 (Incoming Longwave Radiation, W m-2)\\n" + //
        "QLW_5136 (Longwave Radiation Quality)\\n" + //
        "SLW_6136 (Longwave Radiation Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLw_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLw_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLw/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLw.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLw&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLw\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyQnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Total Heat Flux\", \"This dataset has daily Total Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QT_210 (Total Heat Flux, W/M**2)\\n" + //
        "QQ0_5210 (Total Heat Flux Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyQnet/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyQnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyQnet\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyD/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyD\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyRad/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Downgoing Shortwave Radiation\", \"This dataset has daily Downgoing Shortwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RD_495 (Downgoing Shortwave Radiation, W/M**2)\\n" + //
        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
        "SSW_6495 (Shortwave Radiation Source)\\n" + //
        "RDS_496 (Shortwave Radiation Standard Deviation, W/M**2)\\n" + //
        "RDP_497 (Shortwave Radiation Peak, W/M**2)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRad_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRad_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyRad/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyRad.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRad&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyRad\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Net Shortwave Radiation\", \"This dataset has daily Net Shortwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "SWN_1495 (Net Shortwave Radiation, W/M**2)\\n" + //
        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySwnet/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySwnet\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyEmp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Evaporation Minus Precipitation\", \"This dataset has daily Evaporation Minus Precipitation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "EMP_251 (Evaporation Minus Precipitation, mm/hr)\\n" + //
        "QEMP_5251 (Evaporation Minus Precipitation Quality)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEmp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEmp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyEmp/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyEmp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEmp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyEmp\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyBp/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1998-present, Barometric (Air) Pressure\", \"This dataset has daily Barometric (Air) Pressure data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "BP_915 (Barometric (Air) Pressure, hPa)\\n" + //
        "QBP_5915 (Barometric (Air) Pressure Quality)\\n" + //
        "SBP_6915 (Barometric (Air) Pressure Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBp_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBp_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyBp/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyBp.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBp&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyBp\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyLwnet/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation\", \"This dataset has daily Net Longwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LWN_1136 (Net Longwave Radiation, W m-2)\\n" + //
        "QLW_5136 (Longwave Radiation Quality)\\n" + //
        "SLW_6136 (Longwave Radiation Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLwnet_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLwnet_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyLwnet/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyLwnet.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLwnet&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyLwnet\"]\n"
        + //
        "[\"\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"\", \"http://localhost:8080/erddap/files/testTableWithDepth/\", \"This is EDDTableWithDepth\", \"This is the summary\\n"
        + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "AT_21 (Air Temperature, degree_C)\\n" + //
        "QAT_5021 (Air Temperature Quality)\\n" + //
        "SAT_6021 (Air Temperature Source)\\n" + //
        "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"testTableWithDepth\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlKVP
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.jsonlKVP?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    expected = "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAdcp.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyAdcp/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1988-2020, ADCP\", \"Summary\":\"This dataset has daily Acoustic Doppler Current Profiler (ADCP) water currents data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  ADCP data are available only after mooring recoveries, which are scheduled on an annual basis.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "u_1205 (Eastward Sea Water Velocity, cm/s)\\n" + //
        "QU_5205 (Eastward Sea Water Velocity Quality)\\n" + //
        "v_1206 (Northward Sea Water Velocity, cm/s)\\n" + //
        "QV_5206 (Northward Sea Water Velocity Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAdcp_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAdcp_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyAdcp/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyAdcp.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAdcp&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyAdcp\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyCur\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyCur.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyCur/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Currents\", \"Summary\":\"This dataset has daily Currents data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "U_320 (Eastward Sea Water Velocity, cm s-1)\\n" + //
        "V_321 (Northward Sea Water Velocity, cm s-1)\\n" + //
        "CS_300 (Sea Water Velocity, cm s-1)\\n" + //
        "CD_310 (Direction of Sea Water Velocity, degrees_true)\\n" + //
        "QCS_5300 (Current Speed Quality)\\n" + //
        "QCD_5310 (Current Direction Quality)\\n" + //
        "SCS_6300 (Current Speed Source)\\n" + //
        "CIC_7300 (Current Instrument Code)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyCur_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyCur_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyCur/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyCur.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyCur&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyCur\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyT.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyT\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyT.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyT/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Temperature\", \"Summary\":\"This dataset has daily Temperature data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_20 (Sea Water Temperature, degree_C)\\n" + //
        "QT_5020 (Temperature Quality)\\n" + //
        "ST_6020 (Temperature Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyT_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyT_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyT/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyT.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyT&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyT\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyW.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyW\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyW.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyW/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Wind\", \"Summary\":\"This dataset has daily Wind data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "WU_422 (Zonal Wind, m s-1)\\n" + //
        "WV_423 (Meridional Wind, m s-1)\\n" + //
        "WS_401 (Wind Speed, m s-1)\\n" + //
        "QWS_5401 (Wind Speed Quality)\\n" + //
        "SWS_6401 (Wind Speed Source)\\n" + //
        "WD_410 (Wind Direction, degrees_true)\\n" + //
        "QWD_5410 (Wind Direction Quality)\\n" + //
        "SWD_6410 (Wind Direction Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyW_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyW_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyW/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyW.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyW&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyW\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyPos\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyPos.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyPos/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Position\", \"Summary\":\"This dataset has daily Position data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LON_502 (Precise Longitude, degree_east)\\n" + //
        "QX_5502 (Longitude Quality)\\n" + //
        "LAT_500 (Precise Latitude, degree_north)\\n" + //
        "QY_5500 (Latitude Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyPos_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyPos_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyPos/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyPos.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyPos&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyPos\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyS.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyS\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyS.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyS/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Salinity\", \"Summary\":\"This dataset has daily Salinity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
        "QS_5041 (Salinity Quality)\\n" + //
        "SS_6041 (Salinity Source)\\n" + //
        "SIC_8041 (Salinity Instrument Code)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyS_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyS_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyS/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyS.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyS&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyS\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEvap.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyEvap/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Evaporation\", \"Summary\":\"This dataset has daily Evaporation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "E_250 (Evaporation, MM/HR)\\n" + //
        "QE_5250 (Evaporation Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEvap_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEvap_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyEvap/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyEvap.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEvap&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyEvap\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyTau\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyTau.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyTau/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Wind Stress\", \"Summary\":\"This dataset has daily Wind Stress data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "TX_442 (Zonal Wind Stress, N/m2)\\n" + //
        "TY_443 (Meridional Wind Stress, N/m2)\\n" + //
        "TAU_440 (Wind Stress, N/m2)\\n" + //
        "TD_445 (Wind Stress Direction, degrees_true)\\n" + //
        "QTAU_5440 (Wind Stress Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyTau_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyTau_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyTau/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyTau.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyTau&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyTau\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySsd\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySsd.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySsd/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sigma-Theta\", \"Summary\":\"This dataset has daily Sigma-Theta (Potential Density Anomaly) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySsd_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySsd_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySsd/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySsd.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySsd&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySsd\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"Summary\":\"This dataset has monthly Position data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LON_502 (Precise Longitude, degree_east)\\n" + //
        "QX_5502 (Longitude Quality)\\n" + //
        "LAT_500 (Precise Latitude, degree_north)\\n" + //
        "QY_5500 (Latitude Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoMonPos\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyIso\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyIso.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyIso/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, 20C Isotherm Depth\", \"Summary\":\"This dataset has daily 20C Isotherm Depth data (the depth at which the ocean temperature is 20C) from the \\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "ISO_6 (20C Isotherm Depth, m)\\n" + //
        "QI_5006 (20C Depth Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyIso_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyIso_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyIso/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyIso.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyIso&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyIso\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"Summary\":\"This dataset has daily Air Temperature data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "AT_21 (Air Temperature, degree_C)\\n" + //
        "QAT_5021 (Air Temperature Quality)\\n" + //
        "SAT_6021 (Air Temperature Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyAirt\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyDyn.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyDyn/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Dynamic Height\", \"Summary\":\"This dataset has daily Dynamic Height data (a measure of the elevation of the sea level, calculated by integrating the specific volume anomaly of the sea water between the sea surface and 500 m depth) from the \\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "DYN_13 (Dynamic Height, dyn-cm)\\n" + //
        "QD_5013 (Dynamic Height Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyDyn_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyDyn_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyDyn/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyDyn.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyDyn&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyDyn\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyHeat.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyHeat/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1980-present, Heat Content\", \"Summary\":\"This dataset has daily Heat Content data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "HTC_130 (Heat Content, 10**10 J m-2)\\n" + //
        "HTC_5130 (Heat Content Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyHeat_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyHeat_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyHeat/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyHeat.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyHeat&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyHeat\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQlat.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyQlat/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Latent Heat Flux\", \"Summary\":\"This dataset has daily Latent Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QL_137 (Latent Heat Flux, W m-2)\\n" + //
        "QQL_5137 (Latent Heat Flux Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQlat_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQlat_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyQlat/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyQlat.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQlat&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyQlat\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRh\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRh.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRh/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Relative Humidity\", \"Summary\":\"This dataset has daily Relative Humidity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RH_910 (Relative Humidity, percent)\\n" + //
        "QRH_5910 (Relative Humidity Quality)\\n" + //
        "SRH_6910 (Relative Humidity Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRh_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRh_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRh/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRh.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRh&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRh\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQsen.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyQsen/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1989-present, Sensible Heat Flux\", \"Summary\":\"This dataset has daily Sensible Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QS_138 (Sensible Heat Flux, W m-2)\\n" + //
        "QQS_5138 (Sensible Heat Flux Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQsen_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQsen_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyQsen/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyQsen.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQsen&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyQsen\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySss.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySss\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySss.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySss/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1992-present, Sea Surface Salinity\", \"Summary\":\"This dataset has daily Sea Surface Salinity data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "S_41 (Sea Water Practical Salinity, PSU)\\n" + //
        "QS_5041 (Salinity Quality)\\n" + //
        "SS_6041 (Salinity Source)\\n" + //
        "SIC_7041 (Salinity Instrument Code)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySss_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySss_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySss/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySss.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySss&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySss\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRf\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRf.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRf/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Heat Flux Due To Rain\", \"Summary\":\"This dataset has daily Heat Flux Due To Rain data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QR_139 (Heat Flux Due To Rain, W m-2)\\n" + //
        "QQR_5139 (Heat Flux Due To Rain Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRf_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRf_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRf/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRf.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRf&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRf\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRain\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRain.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRain/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Precipitation\", \"Summary\":\"This dataset has daily Precipitation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RN_485 (Precipitation, MM/HR)\\n" + //
        "QRN_5485 (Precipitation Quality)\\n" + //
        "SRN_6485 (Precipitation Source)\\n" + //
        "RNS_486 (Precipitation Standard Deviation, MM/HR)\\n" + //
        "RNP_487 (Percent Time Raining, percent)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRain_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRain_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRain/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRain.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRain&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRain\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBf\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBf.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyBf/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Buoyancy Flux\", \"Summary\":\"This dataset has daily Buoyancy Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "BF_191 (Buoyancy Flux, 10**6 kg m-2 s-1)\\n" + //
        "QBF_5191 (Buoyancy Flux Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBf_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBf_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyBf/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyBf.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBf&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyBf\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLw\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLw.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyLw/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Longwave Radiation\", \"Summary\":\"This dataset has daily Incoming Longwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "Ql_136 (Incoming Longwave Radiation, W m-2)\\n" + //
        "QLW_5136 (Longwave Radiation Quality)\\n" + //
        "SLW_6136 (Longwave Radiation Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLw_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLw_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyLw/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyLw.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLw&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyLw\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyQnet.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyQnet/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Total Heat Flux\", \"Summary\":\"This dataset has daily Total Heat Flux data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "QT_210 (Total Heat Flux, W/M**2)\\n" + //
        "QQ0_5210 (Total Heat Flux Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyQnet_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyQnet_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyQnet/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyQnet.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyQnet&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyQnet\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySst\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "T_25 (Sea Surface Temperature, degree_C)\\n" + //
        "QT_5025 (Sea Surface Temperature Quality)\\n" + //
        "ST_6025 (Sea Surface Temperature Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rlPmelTaoDySst\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyD.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyD\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyD.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyD/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1987-present, Potential Density Anomaly\", \"Summary\":\"This dataset has daily Potential Density Anomaly (sigma-theta) data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "STH_71 (Sigma-Theta, kg m-3)\\n" + //
        "QST_5071 (Sigma-Theta Quality)\\n" + //
        "SST_6071 (Sigma-Theta Source)\\n" + //
        "DIC_7071 (Sigma-Theta Instrument Code)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyD_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyD_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyD/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyD.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyD&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyD\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRad\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyRad.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyRad/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Downgoing Shortwave Radiation\", \"Summary\":\"This dataset has daily Downgoing Shortwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "RD_495 (Downgoing Shortwave Radiation, W/M**2)\\n" + //
        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
        "SSW_6495 (Shortwave Radiation Source)\\n" + //
        "RDS_496 (Shortwave Radiation Standard Deviation, W/M**2)\\n" + //
        "RDP_497 (Shortwave Radiation Peak, W/M**2)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyRad_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyRad_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyRad/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyRad.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyRad&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyRad\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySwnet.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySwnet/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1991-present, Net Shortwave Radiation\", \"Summary\":\"This dataset has daily Net Shortwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "SWN_1495 (Net Shortwave Radiation, W/M**2)\\n" + //
        "QSW_5495 (Shortwave Radiation Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySwnet_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySwnet_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySwnet/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySwnet.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySwnet&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySwnet\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyEmp.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyEmp/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1997-present, Evaporation Minus Precipitation\", \"Summary\":\"This dataset has daily Evaporation Minus Precipitation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "EMP_251 (Evaporation Minus Precipitation, mm/hr)\\n" + //
        "QEMP_5251 (Evaporation Minus Precipitation Quality)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyEmp_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyEmp_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyEmp/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyEmp.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyEmp&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyEmp\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBp\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyBp.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyBp/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1998-present, Barometric (Air) Pressure\", \"Summary\":\"This dataset has daily Barometric (Air) Pressure data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "BP_915 (Barometric (Air) Pressure, hPa)\\n" + //
        "QBP_5915 (Barometric (Air) Pressure Quality)\\n" + //
        "SBP_6915 (Barometric (Air) Pressure Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyBp_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyBp_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyBp/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyBp.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyBp&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyBp\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyLwnet.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyLwnet/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 2000-present, Net Longwave Radiation\", \"Summary\":\"This dataset has daily Net Longwave Radiation data from the\\n"
        + //
        "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n" + //
        "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n" + //
        "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n" + //
        "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
        + //
        "https://www.pmel.noaa.gov/gtmba/mission .\\n" + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "LWN_1136 (Net Longwave Radiation, W m-2)\\n" + //
        "QLW_5136 (Longwave Radiation Quality)\\n" + //
        "SLW_6136 (Longwave Radiation Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyLwnet_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyLwnet_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyLwnet/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyLwnet.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyLwnet&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyLwnet\"}\n"
        + //
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/testTableWithDepth/\", \"Title\":\"This is EDDTableWithDepth\", \"Summary\":\"This is the summary\\n"
        + //
        "\\n" + //
        "cdm_data_type = TimeSeries\\n" + //
        "VARIABLES:\\n" + //
        "array\\n" + //
        "station\\n" + //
        "wmo_platform_code\\n" + //
        "longitude (Nominal Longitude, degrees_east)\\n" + //
        "latitude (Nominal Latitude, degrees_north)\\n" + //
        "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n" + //
        "depth (m)\\n" + //
        "AT_21 (Air Temperature, degree_C)\\n" + //
        "QAT_5021 (Air Temperature Quality)\\n" + //
        "SAT_6021 (Air Temperature Source)\\n" + //
        "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"testTableWithDepth\"}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/search/index.tsv?" +
        EDStatic.defaultPIppQuery + "&searchFor=tao+pmel");
    Test.ensureTrue(
        results.indexOf(
            "\t\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\t") > 0,
        "results=\n" + results);

    // categorize
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">standard_name\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.json");
    Test.ensureEqual(results,
        "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
            "    \"columnTypes\": [\"String\", \"String\"],\n" +
            "    \"rows\": [\n" +
            "      [\"cdm_data_type\", \"http://localhost:" + PORT
            + "/erddap/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"institution\", \"http://localhost:" + PORT
            + "/erddap/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"ioos_category\", \"http://localhost:" + PORT
            + "/erddap/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"keywords\", \"http://localhost:" + PORT
            + "/erddap/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"long_name\", \"http://localhost:" + PORT
            + "/erddap/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"standard_name\", \"http://localhost:" + PORT
            + "/erddap/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"variableName\", \"http://localhost:" + PORT
            + "/erddap/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            +
            "    ]\n" +
            "  }\n" +
            "}\n",
        "results=\n" + results);

    // json with jsonp
    String jsonp = "myFunctionName";
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/index.json?.jsonp=" + SSR.percentEncode(jsonp));
    Test.ensureEqual(results,
        jsonp + "(" +
            "{\n" +
            "  \"table\": {\n" +
            "    \"columnNames\": [\"Categorize\", \"URL\"],\n" +
            "    \"columnTypes\": [\"String\", \"String\"],\n" +
            "    \"rows\": [\n" +
            "      [\"cdm_data_type\", \"http://localhost:" + PORT
            + "/erddap/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"institution\", \"http://localhost:" + PORT
            + "/erddap/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"ioos_category\", \"http://localhost:" + PORT
            + "/erddap/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"keywords\", \"http://localhost:" + PORT
            + "/erddap/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"long_name\", \"http://localhost:" + PORT
            + "/erddap/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"standard_name\", \"http://localhost:" + PORT
            + "/erddap/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            +
            "      [\"variableName\", \"http://localhost:" + PORT
            + "/erddap/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            +
            "    ]\n" +
            "  }\n" +
            "}\n" +
            ")",
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">sea_water_temperature\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/index.json");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"sea_water_temperature\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/institution/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">ioos_category\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">noaa_coastwatch_west_coast_node\n") >= 0,
        "results=\n" + results);

    results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/institution/index.tsv"));
    Test.ensureTrue(results.indexOf("Category[9]URL[10]") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "noaa_coastwatch_west_coast_node[9]http://localhost:" + PORT
            + "/erddap/categorize/institution/noaa_coastwatch_west_coast_node/index.tsv?page=1&itemsPerPage=1000[10]") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/sea_water_temperature/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">erdGlobecBottle\n") >= 0,
        "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
        "/categorize/standard_name/sea_water_temperature/index.json");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", " +
        (EDStatic.sosActive ? "\"sos\", " : "") +
        (EDStatic.wcsActive ? "\"wcs\", " : "") +
        (EDStatic.wmsActive ? "\"wms\", " : "") +
        (EDStatic.filesActive ? "\"files\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"Accessible\", " : "") +
        "\"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", " +
        (EDStatic.subscriptionSystemActive ? "\"Email\", " : "") +
        "\"Institution\", \"Dataset ID\"],\n" +
        "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", " +
        (EDStatic.sosActive ? "\"String\", " : "") +
        (EDStatic.wcsActive ? "\"String\", " : "") +
        (EDStatic.wmsActive ? "\"String\", " : "") +
        (EDStatic.filesActive ? "\"String\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"String\", " : "") +
        "\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", " +
        (EDStatic.subscriptionSystemActive ? "\"String\", " : "") +
        "\"String\", \"String\"],\n" +
        "    \"rows\": [\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected = "http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle.subset\", " +
        "\"http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle\", " +
        "\"http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle.graph\", " +
        (EDStatic.sosActive ? "\"\", " : "") + // currently, it isn't made available via sos
        (EDStatic.wcsActive ? "\"\", " : "") +
        (EDStatic.wmsActive ? "\"\", " : "") +
        (EDStatic.filesActive ? "\"http://localhost:" + PORT + "/erddap/files/erdGlobecBottle/\", " : "") +
        (EDStatic.authentication.length() > 0 ? "\"public\", " : "") +
        "\"GLOBEC NEP Rosette Bottle Data (2002)\", \"GLOBEC (GLOBal " +
        "Ocean ECosystems Dynamics) NEP (Northeast Pacific)\\nRosette Bottle Data from " +
        "New Horizon Cruise (NH0207: 1-19 August 2002).\\nNotes:\\nPhysical data " +
        "processed by Jane Fleischbein (OSU).\\nChlorophyll readings done by " +
        "Leah Feinberg (OSU).\\nNutrient analysis done by Burke Hales (OSU).\\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\\n" +
        "secondary sensor pair was used in final processing of CTD data for\\n" +
        "most stations because the primary had more noise and spikes. The\\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\\n" +
        "multiple spikes or offsets in the secondary pair.\\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\\n" +
        "data developed by Burke Hales (OSU).\\n" +
        "Operation Detection Limits for Nutrient Concentrations\\n" +
        "Nutrient  Range         Mean    Variable         Units\\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\\n" +
        "Dates and Times are UTC.\\n\\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\\n\\n" +
        // was "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\\n\\n"
        // +
        "Inquiries about how to access this data should be directed to\\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\\n\\n" +
        "cdm_data_type = TrajectoryProfile\\n" +
        "VARIABLES:\\ncruise_id\\n... (24 more variables)\\n\", " +
        "\"http://localhost:" + PORT + "/erddap/metadata/fgdc/xml/erdGlobecBottle_fgdc.xml\", " +
        "\"http://localhost:" + PORT + "/erddap/metadata/iso19115/xml/erdGlobecBottle_iso19115.xml\", " +
        "\"http://localhost:" + PORT + "/erddap/info/erdGlobecBottle/index.json\", " +
        "\"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\", " + // was
                                                                                // "\"http://www.globec.org/\",
                                                                                // " +
        "\"http://localhost:" + PORT + "/erddap/rss/erdGlobecBottle.rss\", " +
        (EDStatic.subscriptionSystemActive
            ? "\"http://localhost:" + PORT
                + "/erddap/subscriptions/add.html?datasetID=erdGlobecBottle&showErrors=false&email=\", "
            : "")
        +
        "\"GLOBEC\", \"erdGlobecBottle\"],";
    po = results.indexOf("http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle");
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // griddap
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of griddap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        ">Daily MUR SST, Interim near-real-time (nrt) product") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">jplMURSST41\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/index.json?" +
        EDStatic.defaultPIppQuery + "");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Daily MUR SST, Interim near-real-time (nrt) product") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"jplMURSST41\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/jplMURSST41.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Sea Ice Area Fraction") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/jplMURSST41.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Sea Ice Area Fraction") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // tabledap
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.html?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of tabledap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdGlobecBottle\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/index.json?" +
        EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"GLOBEC NEP Rosette Bottle Data (2002)\"") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"erdGlobecBottle\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Filled Square") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // files
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/");
    Test.ensureTrue(
        results.indexOf(
            "ERDDAP's \"files\" system lets you browse a virtual file system and download source data files.") >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("cwwcNDBCMet") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directories") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/cwwcNDBCMet/nrt/");
    Test.ensureTrue(results.indexOf("NDBC Standard Meteorological Buoy Data") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make a graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NDBC&#x5f;41008&#x5f;met&#x2e;nc") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    String localName = EDStatic.fullTestCacheDirectory + "NDBC_41008_met.nc";
    File2.delete(localName);
    SSR.downloadFile( // throws Exception if trouble
        EDStatic.erddapUrl + "/files/cwwcNDBCMet/nrt/NDBC_41008_met.nc",
        localName, true); // tryToUseCompression
    Test.ensureTrue(File2.isFile(localName),
        "/files download failed. Not found: localName=" + localName);
    File2.delete(localName);

    // sos
    if (EDStatic.sosActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of SOS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">NDBC Standard Meteorological Buoy Data") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">cwwcNDBCMet") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"NDBC Standard Meteorological Buoy Data\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"cwwcNDBCMet\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "available via ERDDAP's Sensor Observation Service (SOS) web service.") >= 0,
          "results=\n" + results);

      String sosUrl = EDStatic.erddapUrl + "/sos/cwwcNDBCMet/" + EDDTable.sosServer;
      results = SSR.getUrlResponseStringUnchanged(sosUrl + "?service=SOS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<ows:ServiceIdentification>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("<ows:Get xlink:href=\"" + sosUrl + "?\"/>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("</Capabilities>") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
          + PORT + "/erddap/sos/index.html?page=1&itemsPerPage=1000\n"
          +
          "(Error {\n" +
          "    code=404;\n" +
          "    message=\"Not Found: The \\\"SOS\\\" system has been disabled on this ERDDAP.\";\n" +
          "})\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected,
          "results=\n" + results);
    }

    // wcs
    if (EDStatic.wcsActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Datasets Which Can Be Accessed via WCS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
              ">Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)</td>") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMHchla8day<") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
              "\"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(
          "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.") >= 0,
          "results=\n" + results);

      String wcsUrl = EDStatic.erddapUrl + "/wcs/erdMHchla8day/" + EDDGrid.wcsServer;
      results = SSR.getUrlResponseStringUnchanged(wcsUrl + "?service=WCS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<CoverageOfferingBrief>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("<lonLatEnvelope srsName") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("</WCS_Capabilities>") >= 0, "results=\n" + results);
    } else {
      // wcs is inactive
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf(
          "java.io.FileNotFoundException: http://localhost:" + PORT
              + "/erddap/wcs/index.html?page=1&itemsPerPage=1000") >= 0,
          "results=\n" + results);
    }

    // wms
    if (EDStatic.wmsActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of WMS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results
              .indexOf(
                  ">Chlorophyll-a, Aqua MODIS, NPP, 0.0125&deg;, West US, EXPERIMENTAL, 2002-present (1 Day Composite)\n") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMH1chla1day\n") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.json?" +
          EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
              "\"Chlorophyll-a, Aqua MODIS, NPP, 0.0125\\u00b0, West US, EXPERIMENTAL, 2002-present (1 Day Composite)\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMH1chla1day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("display of registered and superimposed map-like views") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/erdMH1chla1day/index.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
              "ERDDAP - Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite) - WMS") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("on-the-fly by ERDDAP's") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("longitude") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/index.html?" +
            EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0,
          "results=\n" + results);
    }

    // results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
    // "/categorize/standard_name/index.html");
    // Test.ensureTrue(results.indexOf(">sea_water_temperature<") >= 0,
    // "results=\n" + results);

    // validate the various GetCapabilities documents
    /*
     * NOT ACTIVE
     * String s = https://xmlvalidation.com/ ".../xml/validate/?lang=en" +
     * "&url=" + EDStatic.erddapUrl + "/wms/" + EDD.WMS_SERVER + "?service=WMS&" +
     * "request=GetCapabilities&version=";
     * Test.displayInBrowser(s + "1.1.0");
     * Test.displayInBrowser(s + "1.1.1");
     * Test.displayInBrowser(s + "1.3.0");
     */

    // more information
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/information.html");
    Test.ensureTrue(results.indexOf(
        "ERDDAP a solution to everyone's data distribution / data access problems?") >= 0,
        "results=\n" + results);

    // subscriptions
    if (EDStatic.subscriptionSystemActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/index.html");
      Test.ensureTrue(results.indexOf("Add a new subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Validate a subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List your subscriptions") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Remove a subscription") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/add.html");
      Test.ensureTrue(results.indexOf(
          "To add a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/validate.html");
      Test.ensureTrue(results.indexOf(
          "To validate a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/list.html");
      Test.ensureTrue(results.indexOf(
          "To request an email with a list of your subscriptions, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/subscriptions/remove.html");
      Test.ensureTrue(results.indexOf(
          "To remove a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
            "/subscriptions/index.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0,
          "results=\n" + results);
    }

    // slideSorter
    if (EDStatic.slideSorterActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
          "/slidesorter.html");
      Test.ensureTrue(results.indexOf(
          "Your slides will be lost when you close this browser window, unless you:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl +
            "/slidesorter.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0,
          "results=\n" + results);
    }

    // embed a graph (always at coastwatch)
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/images/embed.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "Embed a Graph in a Web Page") >= 0,
        "results=\n" + results);

    // Computer Programs
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/rest.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "ERDDAP's RESTful Web Services") >= 0,
        "results=\n" + results);

    // list of services
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.csv");
    expected = "Resource,URL\n" +
        "info,http://localhost:" + PORT + "/erddap/info/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "search,http://localhost:" + PORT + "/erddap/search/index.csv?" + EDStatic.defaultPIppQuery
        + "&searchFor=\n" +
        "categorize,http://localhost:" + PORT + "/erddap/categorize/index.csv?" + EDStatic.defaultPIppQuery
        + "\n" +
        "griddap,http://localhost:" + PORT + "/erddap/griddap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        "tabledap,http://localhost:" + PORT + "/erddap/tabledap/index.csv?" + EDStatic.defaultPIppQuery + "\n" +
        (EDStatic.sosActive
            ? "sos,http://localhost:" + PORT + "/erddap/sos/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "wcs,http://localhost:" + PORT + "/erddap/wcs/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "wms,http://localhost:" + PORT + "/erddap/wms/index.csv?" + EDStatic.defaultPIppQuery + "\n"
            : "");
    // subscriptions?
    // converters?
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.htmlTable?" +
        EDStatic.defaultPIppQuery);
    expected = EDStatic.startHeadHtml(0, EDStatic.erddapUrl((String) null, language), "Resources") + "\n" +
        "</head>\n" +
        EDStatic.startBodyHtml(0, null, "index.html", EDStatic.defaultPIppQuery) + // 2022-11-22 .htmlTable
                                                                                   // converted to
                                                                                   // .html to avoid user
                                                                                   // requesting all
                                                                                   // data in a dataset if they
                                                                                   // change
                                                                                   // language
        "&nbsp;<br>\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>Resource\n" +
        "<th>URL\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>info\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;info&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
        + PORT + "/erddap/info/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>search\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;search&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000&#x26;searchFor&#x3d;\">http://localhost:"
        + PORT + "/erddap/search/index.htmlTable?page=1&amp;itemsPerPage=1000&amp;searchFor=</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>categorize\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;categorize&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
        + PORT + "/erddap/categorize/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>griddap\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;griddap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
        + PORT + "/erddap/griddap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>tabledap\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;tabledap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
        + PORT + "/erddap/tabledap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        (EDStatic.sosActive ? "<tr>\n" +
            "<td>sos\n" +
            "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;sos&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
            + PORT + "/erddap/sos/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            +
            "</tr>\n" : "")
        +
        "<tr>\n" +
        "<td>wms\n" +
        "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;wms&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
        + PORT + "/erddap/wms/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
        +
        "</tr>\n" +
        "</table>\n" +
        EDStatic.endBodyHtml(0, EDStatic.erddapUrl((String) null, language), (String) null) + "\n" +
        "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.json");
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"Resource\", \"URL\"],\n" +
        "    \"columnTypes\": [\"String\", \"String\"],\n" +
        "    \"rows\": [\n" +
        "      [\"info\", \"http://localhost:" + PORT + "/erddap/info/index.json?page=1&itemsPerPage=1000\"],\n"
        +
        "      [\"search\", \"http://localhost:" + PORT
        + "/erddap/search/index.json?page=1&itemsPerPage=1000&searchFor=\"],\n"
        +
        "      [\"categorize\", \"http://localhost:" + PORT
        + "/erddap/categorize/index.json?page=1&itemsPerPage=1000\"],\n"
        +
        "      [\"griddap\", \"http://localhost:" + PORT
        + "/erddap/griddap/index.json?page=1&itemsPerPage=1000\"],\n" +
        "      [\"tabledap\", \"http://localhost:" + PORT
        + "/erddap/tabledap/index.json?page=1&itemsPerPage=1000\"]"
        + (EDStatic.sosActive || EDStatic.wcsActive || EDStatic.wmsActive ? "," : "") + "\n" +
        (EDStatic.sosActive
            ? "      [\"sos\", \"http://localhost:" + PORT
                + "/erddap/sos/index.json?page=1&itemsPerPage=1000\"]"
                + (EDStatic.wcsActive || EDStatic.wmsActive ? "," : "") + "\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "      [\"wcs\", \"http://localhost:" + PORT
                + "/erddap/wcs/index.json?page=1&itemsPerPage=1000\"]"
                + (EDStatic.wmsActive ? "," : "") + "\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "      [\"wms\", \"http://localhost:" + PORT
                + "/erddap/wms/index.json?page=1&itemsPerPage=1000\"]\n"
            : "")
        +
        // subscriptions?
        "    ]\n" +
        "  }\n" +
        "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.tsv"));
    expected = "Resource[9]URL[10]\n" +
        "info[9]http://localhost:" + PORT + "/erddap/info/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "search[9]http://localhost:" + PORT
        + "/erddap/search/index.tsv?page=1&itemsPerPage=1000&searchFor=[10]\n" +
        "categorize[9]http://localhost:" + PORT + "/erddap/categorize/index.tsv?page=1&itemsPerPage=1000[10]\n"
        +
        "griddap[9]http://localhost:" + PORT + "/erddap/griddap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        "tabledap[9]http://localhost:" + PORT + "/erddap/tabledap/index.tsv?page=1&itemsPerPage=1000[10]\n" +
        (EDStatic.sosActive
            ? "sos[9]http://localhost:" + PORT + "/erddap/sos/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        (EDStatic.wcsActive
            ? "wcs[9]http://localhost:" + PORT + "/erddap/wcs/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        (EDStatic.wmsActive
            ? "wms[9]http://localhost:" + PORT + "/erddap/wms/index.tsv?page=1&itemsPerPage=1000[10]\n"
            : "")
        +
        "[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.xhtml");
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
        "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
        "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
        "  <title>Resources</title>\n" +
        "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:" + PORT
        + "/erddap/images/erddap2.css\" />\n"
        +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>Resource</th>\n" +
        "<th>URL</th>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>info</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/info/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>search</td>\n" +
        "<td>http://localhost:" + PORT
        + "/erddap/search/index.xhtml?page=1&amp;itemsPerPage=1000&amp;searchFor=</td>\n"
        +
        "</tr>\n" +
        "<tr>\n" +
        "<td>categorize</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/categorize/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>griddap</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/griddap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td>tabledap</td>\n" +
        "<td>http://localhost:" + PORT + "/erddap/tabledap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
        "</tr>\n" +
        (EDStatic.sosActive ? "<tr>\n" +
            "<td>sos</td>\n" +
            "<td>http://localhost:" + PORT + "/erddap/sos/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        (EDStatic.wcsActive ? "<tr>\n" +
            "<td>wcs</td>\n" +
            "<td>http://localhost:" + PORT + "/erddap/wcs/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        (EDStatic.wmsActive ? "<tr>\n" +
            "<td>wms</td>\n" +
            "<td>http://localhost:" + PORT + "/erddap/wms/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n" +
            "</tr>\n" : "")
        +
        "</table>\n" +
        "</body>\n" +
        "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /**
   * This checks that the links on the specified web page return error 200.
   *
   * @param tUrl the web page to be checked
   * @throws RuntimeException for
   */
  void testForBrokenLinks(String tUrl) throws Exception {

    String2.log("\nSSR.testForBrokenLinks(" + tUrl + ")");
    String regex = "\"(https?://.+?)\"";
    Pattern pattern = Pattern.compile(regex);
    String lines[] = SSR.getUrlResponseLines(tUrl);
    StringBuilder log = new StringBuilder();
    int errorCount = 0;
    HashSet<String> tried = new HashSet();
    String skip[] = new String[] {
        "http://",
        "http://127.0.0.1:8080/manager/html/", // will always fail this test
        "http://127.0.0.1:8080/erddap/status.html", // will always fail this test
        "https://127.0.0.1:8443/cwexperimental/login.html", // the links to log in (upper right of most web
                                                            // pages) will
                                                            // fail on my test computer
        "https://192.168.31.18/",
        "https://basin.ceoe.udel.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path
                                                         // building
                                                         // failed:
                                                         // sun.security.provider.certpath.SunCertPathBuilderException:
                                                         // unable to find valid certification path to requested
                                                         // target
        "http://coastwatch.pfeg.noaa.gov:8080/", // java.net.SocketTimeoutException: Connect timed out
        "https://coastwatch.pfeg.noaa.gov/erddap/files/cwwcNDBCMet/nrt/NDBC_{41008,41009,41010}_met.nc", // intended
                                                                                                         // for
                                                                                                         // curl
                                                                                                         // (globbing)
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/pmelTaoDySst.csv?longitude,latitude,time,station,wmo_platform_code,T_25&time>=2015-05-23T12:00:00Z&time<=2015-05-31T12:00:00Z", // always
                                                                                                                                                                                          // fails
                                                                                                                                                                                          // because
                                                                                                                                                                                          // of
                                                                                                                                                                                          // invalid
                                                                                                                                                                                          // character
        "https://dev.mysql.com", // fails here, but works in browser
        "https://myhsts.org", // javax.net.ssl.SSLHandshakeException: No subject alternative DNS name matching
                              // myhsts.org found.
        "https://gcoos5.geos.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path
                                                          // building
                                                          // failed:
                                                          // sun.security.provider.certpath.SunCertPathBuilderException:
                                                          // unable to find valid certification path to
                                                          // requested target
        "https://gcoos4.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX path building
                                                     // failed:
                                                     // sun.security.provider.certpath.SunCertPathBuilderException:
                                                     // unable to find valid certification path to requested
                                                     // target
        "https://github.com/ERDDAP/", // fails here, but works in browser
        "http://localhost:8080/manager/html/", // will always fail this test
        "https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core", // it's clever: no
                                                                                           // follow
        "https://mvnrepository.com/artifact/com.codahale.metrics/metrics-core/3.0.2", // it's clever: no follow
        "https://mvnrepository.com/artifact/org.postgresql/postgresql", // it's clever: no follow
        "https://mvnrepository.com/", // it's clever: no follow
        "https://noaa-goes17.s3.us-east-1.amazonaws.com", // will always fail
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/", // will always fail
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/ABI-L1b-RadC/2019/235/22/OR_ABI-L1b-RadC-M6C01_G17_s20192352201196_e20192352203569_c20192352204013.nc", // always
                                                                                                                                                                // fails
        "https://whatismyipaddress.com/ip-lookup", // it's clever: no follow
        "https://www.adobe.com", // fails here, but works in browser
        "https://dev.mysql.com/doc/refman/8.0/en/time-zone-support.html#time-zone-leap-seconds", // failes in
                                                                                                 // test, works
                                                                                                 // in brownser
        "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHchla8day.timeGaps", // dataset not found
    };
    // https://unitsofmeasure.org/ucum.html fails in tests because of certificate,
    // but succeeds in my browser. Others are like this, too.
    for (int linei = 0; linei < lines.length; linei++) {
      String urls[] = String2.extractAllCaptureGroupsAsHashSet(lines[linei], pattern, 1).toArray(new String[0]);
      for (int urli = 0; urli < urls.length; urli++) {
        // just try a given url once
        if (tried.contains(urls[urli]))
          continue;
        tried.add(urls[urli]);

        String ttUrl = XML.decodeEntities(urls[urli]);
        if (String2.indexOf(skip, ttUrl) >= 0)
          continue;
        String msg = null;
        try {
          Object[] o3 = SSR.getUrlConnBufferedInputStream(ttUrl,
              20000, false, true); // timeOutMillis, requestCompression, touchMode
          if (o3[0] == null) {
            ((InputStream) o3[1]).close();
            throw new IOException("The URL for SSR.testForBrokenLinks can't be an AWS S3 URL.");
          }
          HttpURLConnection conn = (HttpURLConnection) (o3[0]);
          int code = conn.getResponseCode();
          if (code != 200)
            msg = " code=" + code + " " + ttUrl;
        } catch (Exception e) {
          msg = " code=ERR " + ttUrl + " error=\n" +
              e.toString() + "\n";
        }
        if (msg != null) {
          String fullMsg = "#" + ++errorCount + " line=" + String2.left("" + (linei + 1), 4) + msg;
          String2.log(fullMsg);
          log.append(fullMsg + "\n");
        }
      }
    }
    if (log.length() > 0)
      throw new RuntimeException(
          "\nSSR.testForBrokenLinks(" + tUrl + ") found:\n" +
              log.toString());
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testForBrokenLinks() throws Exception {
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/oceanicAtmosphericAcronyms.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/fipscounty.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/keywords.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/time.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/units.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/urls.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/oceanicAtmosphericVariableNames.html");

    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/AccessToPrivateDatasets.html");
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/download/changes.html"); // todo re-enable, a couple links seem to
    // be broken, needs more investigation
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/EDDTableFromEML.html");
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/grids.html"); // todo re-enable, link to storage mojo about google might be gone
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/NCCSV.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/NCCSV_1.00.html");
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/download/setup.html"); // todo re-enable, a number of links are
    // broken
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/download/setupDatasetsXml.html"); // todo re-enable, a number of
    // links are broken

    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/information.html"); // todo rtech link breaks, but its already
    // commented out, remove fully?
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/rest.html"); // todo re-enable, rganon broken
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/griddap/documentation.html"); // todo re-enable multiple links error
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/tabledap/documentation.html"); // todo re-enable, several broken
    // links
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/files/documentation.html"); // todo re-enable, adobe link failes to
    // load
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/wms/documentation.html"); // todo re-enable esri link is broken (and
    // others)
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/images/erddapTalk/TablesAndGrids.html"); // doesn't exist?
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/images/erddapTalk/erdData.html"); // doesn't exist
  }

  /**
   * This test the json-ld responses from the ERDDAP at EDStatic.erddapUrl.
   * It assumes jplMURSST41 datasets is available.
   *
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testJsonld() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String results, expected;
    String2.log("\n*** Erddap.testJsonld");
    int po;

    // info list all datasets
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/index.html?" +
        EDStatic.defaultPIppQuery);

    // json-ld all datasets
    expected = "<script type=\"application/ld+json\">\n" +
        "{\n" +
        "  \"@context\": \"http://schema.org\",\n" +
        "  \"@type\": \"DataCatalog\",\n" +
        "  \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n" +
        "  \"url\": \"http://localhost:" + PORT + "/erddap\",\n" +
        "  \"publisher\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"ERDDAP Jetty Install\",\n" +
        "    \"address\": {\n" +
        "      \"@type\": \"PostalAddress\",\n" +
        "      \"addressCountry\": \"USA\",\n" +
        "      \"addressLocality\": \"123 Irrelevant St., Nowhere\",\n" +
        "      \"addressRegion\": \"AK\",\n" +
        "      \"postalCode\": \"99504\"\n" +
        "    },\n" +
        "    \"telephone\": \"555-555-5555\",\n" +
        "    \"email\": \"nobody@example.com\",\n" +
        "    \"sameAs\": \"http://example.com\"\n"
        +
        "  },\n" +
        "  \"fileFormat\": [\n" +
        "    \"application/geo+json\",\n" +
        "    \"application/json\",\n" +
        "    \"text/csv\"\n" +
        "  ],\n" +
        "  \"isAccessibleForFree\": \"True\",\n" +
        "  \"dataset\": [\n" +
        "    {\n" +
        "      \"@type\": \"Dataset\",\n" +
        "      \"name\": \"";
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    {\n" +
        "      \"@type\": \"Dataset\",\n" +
        "      \"name\": \"Fluorescence Line Height, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)\",\n"
        +
        "      \"sameAs\": \"http://localhost:" + PORT + "/erddap/info/erdMH1cflh1day/index.html\"\n" +
        "    }";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // json-ld 1 dataset
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/jplMURSST41/index.html");
    expected = "<script type=\"application/ld+json\">\n" +
        "{\n" +
        "  \"@context\": \"http://schema.org\",\n" +
        "  \"@type\": \"Dataset\",\n" +
        "  \"name\": \"Multi-scale Ultra-high Resolution (MUR) SST Analysis fv04.1, Global, 0.01°, 2002-present, Daily\",\n"
        +
        "  \"headline\": \"jplMURSST41\",\n" +
        "  \"description\": \"This is a merged, multi-sensor L4 Foundation Sea Surface Temperature (SST) analysis product from Jet Propulsion Laboratory (JPL). This daily, global, Multi-scale, Ultra-high Resolution (MUR) Sea Surface Temperature (SST) 1-km data set, Version 4.1, is produced at JPL under the NASA MEaSUREs program. For details, see https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1 . This dataset is part of the Group for High-Resolution Sea Surface Temperature (GHRSST) project. The data for the most recent 7 days is usually revised everyday.  The data for other days is sometimes revised.\\n"
        +
        "_NCProperties=version=2,netcdf=4.7.4,hdf5=1.8.12\\n" +
        "acknowledgement=Please acknowledge the use of these data with the following statement:  These data were provided by JPL under support by NASA MEaSUREs program.\\n"
        +
        "cdm_data_type=Grid\\n" +
        "comment=MUR = \\\"Multi-scale Ultra-high Resolution\\\"\\n" +
        "Conventions=CF-1.6, COARDS, ACDD-1.3\\n" +
        "Easternmost_Easting=180.0\\n" +
        "file_quality_level=3\\n" +
        "gds_version_id=2.0\\n" +
        "geospatial_lat_max=89.99\\n" +
        "geospatial_lat_min=-89.99\\n" +
        "geospatial_lat_resolution=0.01\\n" +
        "geospatial_lat_units=degrees_north\\n" +
        "geospatial_lon_max=180.0\\n" +
        "geospatial_lon_min=-179.99\\n" +
        "geospatial_lon_resolution=0.01\\n" +
        "geospatial_lon_units=degrees_east\\n" +
        "history=created at nominal 4-day latency; replaced nrt (1-day latency) version.\\n" +
        "Data is downloaded daily from https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/ to NOAA NMFS SWFSC ERD by erd.data@noaa.gov .\\n"
        +
        "The data for the most recent 7 days is usually revised everyday. The data for other days is sometimes revised.\\n"
        +
        "id=MUR-JPL-L4-GLOB-v04.1\\n" +
        "infoUrl=https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\\n" +
        "institution=NASA JPL\\n" +
        "keywords_vocabulary=GCMD Science Keywords\\n" +
        "naming_authority=org.ghrsst\\n" +
        "netcdf_version_id=4.1\\n" +
        "Northernmost_Northing=89.99\\n" +
        "platform=Terra, Aqua, GCOM-W, MetOp-B, Buoys/Ships\\n" +
        "processing_level=L4\\n" +
        "project=NASA Making Earth Science Data Records for Use in Research Environments (MEaSUREs) Program\\n"
        +
        "references=https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\\n" +
        "sensor=MODIS, AMSR2, AVHRR, in-situ\\n" +
        "source=MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRRMTB_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\\n" +
        "sourceUrl=(local files)\\n" +
        "Southernmost_Northing=-89.99\\n" +
        "spatial_resolution=0.01 degrees\\n" +
        "standard_name_vocabulary=CF Standard Name Table v70\\n" +
        "testOutOfDate=now-3days\\n" +
        "time_coverage_end=yyyy-mm-ddT09:00:00Z\\n" +
        "time_coverage_start=yyyy-mm-ddT09:00:00Z\\n" +
        "Westernmost_Easting=-179.99\",\n" +
        "  \"url\": \"http://localhost:" + PORT + "/erddap/griddap/jplMURSST41.html\",\n" +
        "  \"includedInDataCatalog\": {\n" +
        "    \"@type\": \"DataCatalog\",\n" +
        "    \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n" +
        "    \"sameAs\": \"http://localhost:" + PORT + "/erddap\"\n" +
        "  },\n" +
        "  \"keywords\": [\n" +
        "    \"analysed\",\n" +
        "    \"analysed_sst\",\n" +
        "    \"analysis\",\n" +
        "    \"analysis_error\",\n" +
        "    \"area\",\n" +
        "    \"binary\",\n" +
        "    \"composite\",\n" +
        "    \"daily\",\n" +
        "    \"data\",\n" +
        "    \"day\",\n" +
        "    \"deviation\",\n" +
        "    \"distribution\",\n" +
        "    \"Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature\",\n" +
        "    \"error\",\n" +
        "    \"estimated\",\n" +
        "    \"field\",\n" +
        "    \"final\",\n" +
        "    \"foundation\",\n" +
        "    \"fraction\",\n" +
        "    \"ghrsst\",\n" +
        "    \"high\",\n" +
        "    \"ice\",\n" +
        "    \"ice distribution\",\n" +
        "    \"identifier\",\n" +
        "    \"jet\",\n" +
        "    \"jpl\",\n" +
        "    \"laboratory\",\n" +
        "    \"land\",\n" +
        "    \"land_binary_mask\",\n" +
        "    \"mask\",\n" +
        "    \"multi\",\n" +
        "    \"multi-scale\",\n" +
        "    \"mur\",\n" +
        "    \"nasa\",\n" +
        "    \"ocean\",\n" +
        "    \"oceans\",\n" +
        "    \"product\",\n" +
        "    \"propulsion\",\n" +
        "    \"resolution\",\n" +
        "    \"scale\",\n" +
        "    \"sea\",\n" +
        "    \"sea ice area fraction\",\n" +
        "    \"sea/land\",\n" +
        "    \"sea_ice_fraction\",\n" +
        "    \"sea_surface_foundation_temperature\",\n" +
        "    \"sst\",\n" +
        "    \"standard\",\n" +
        "    \"statistics\",\n" +
        "    \"surface\",\n" +
        "    \"temperature\",\n" +
        "    \"time\",\n" +
        "    \"ultra\",\n" +
        "    \"ultra-high\"\n" +
        "  ],\n" +
        "  \"license\": \"These data are available free of charge under the JPL PO.DAAC data policy.\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\",\n"
        +
        "  \"variableMeasured\": [\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"time\",\n" +
        "      \"alternateName\": \"reference time of sst field\",\n" +
        "      \"description\": \"reference time of sst field\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"T\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"Nominal time of analyzed fields\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"reference time of sst field\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"time\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"time_origin\",\n" +
        "          \"value\": \"01-JAN-1970 00:00:00\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": \"yyyy-mm-ddT09:00:00Z\",\n" + // changes
        "      \"minValue\": \"yyyy-mm-ddT09:00:00Z\",\n" +
        "      \"propertyID\": \"time\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"latitude\",\n" +
        "      \"alternateName\": \"Latitude\",\n" +
        "      \"description\": \"Latitude\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Lat\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"Y\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Location\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Latitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"latitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 90\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -90\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": 89.99,\n" +
        "      \"minValue\": -89.99,\n" +
        "      \"propertyID\": \"latitude\",\n" +
        "      \"unitText\": \"degrees_north\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"longitude\",\n" +
        "      \"alternateName\": \"Longitude\",\n" +
        "      \"description\": \"Longitude\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"axis\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_CoordinateAxisType\",\n" +
        "          \"value\": \"Lon\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axis\",\n" +
        "          \"value\": \"X\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Location\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Longitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"longitude\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 180\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -180\n" +
        "        }\n" +
        "      ],\n" +
        "      \"maxValue\": 180,\n" +
        "      \"minValue\": -179.99,\n" +
        "      \"propertyID\": \"longitude\",\n" +
        "      \"unitText\": \"degrees_east\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"analysed_sst\",\n" +
        "      \"alternateName\": \"Analysed Sea Surface Temperature\",\n" +
        "      \"description\": \"Analysed Sea Surface Temperature\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -7.768000000000001\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 32\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"\\\"Final\\\" version using Multi-Resolution Variational Analysis (MRVA) method for interpolation\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Analysed Sea Surface Temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"MODIS_T-JPL, MODIS_A-JPL, AMSR2-REMSS, AVHRRMTB_G-NAVO, iQUAM-NOAA/NESDIS, Ice_Conc-OSISAF\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"sea_surface_foundation_temperature\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 57.767\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": -7.767000000000003\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"sea_surface_foundation_temperature\",\n" +
        "      \"unitText\": \"degree_C\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"analysis_error\",\n" +
        "      \"alternateName\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
        "      \"description\": \"Estimated Error Standard Deviation of analysed_sst\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -327.68\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 5\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Statistics\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Estimated Error Standard Deviation of analysed_sst\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 327.67\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 0\n" +
        "        }\n" +
        "      ],\n" +
        "      \"unitText\": \"degree_C\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"mask\",\n" +
        "      \"alternateName\": \"Sea/Land Field Composite Mask\",\n" +
        "      \"description\": \"Sea/Land Field Composite Mask\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -128\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 20\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"mask can be used to further filter the data.\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_masks\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"flag_meanings\",\n" +
        "          \"value\": \"open_sea land open_lake open_sea_with_ice_in_the_grid open_lake_with_ice_in_the_grid\"\n"
        +
        "        },\n" +
        // " {\n" +
        // " \"@type\": \"PropertyValue\",\n" +
        // " \"name\": \"flag_values\",\n" +
        // " \"value\": 1\n" +
        // " },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Identifier\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Sea/Land Field Composite Mask\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"GMT \\\"grdlandmask\\\", ice flag from sea_ice_fraction data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"land_binary_mask\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 31\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 1\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"land_binary_mask\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"@type\": \"PropertyValue\",\n" +
        "      \"name\": \"sea_ice_fraction\",\n" +
        "      \"alternateName\": \"Sea Ice Area Fraction\",\n" +
        "      \"description\": \"Sea Ice Area Fraction\",\n" +
        "      \"valueReference\": [\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"axisOrDataVariable\",\n" +
        "          \"value\": \"data\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"_FillValue\",\n" +
        "          \"value\": -1.28\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMaximum\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"colorBarMinimum\",\n" +
        "          \"value\": 0\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"comment\",\n" +
        "          \"value\": \"ice fraction is a dimensionless quantity between 0 and 1; it has been interpolated by a nearest neighbor approach; EUMETSAT OSI-SAF files used: ice_conc_nh_polstere-100_multi_yyyymmdd1200.nc, ice_conc_sh_polstere-100_multi_yyyymmdd1200.nc.\"\n"
        +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"ioos_category\",\n" +
        "          \"value\": \"Ice Distribution\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"long_name\",\n" +
        "          \"value\": \"Sea Ice Area Fraction\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"source\",\n" +
        "          \"value\": \"EUMETSAT OSI-SAF, copyright EUMETSAT\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"standard_name\",\n" +
        "          \"value\": \"sea_ice_area_fraction\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_max\",\n" +
        "          \"value\": 1\n" +
        "        },\n" +
        "        {\n" +
        "          \"@type\": \"PropertyValue\",\n" +
        "          \"name\": \"valid_min\",\n" +
        "          \"value\": 0\n" +
        "        }\n" +
        "      ],\n" +
        "      \"propertyID\": \"sea_ice_area_fraction\",\n" +
        "      \"unitText\": \"1\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"creator\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"JPL MUR SST project\",\n" +
        "    \"email\": \"ghrsst@podaac.jpl.nasa.gov\",\n" +
        "    \"sameAs\": \"https://podaac.jpl.nasa.gov/dataset/MUR-JPL-L4-GLOB-v4.1\"\n" +
        "  },\n" +
        "  \"publisher\": {\n" +
        "    \"@type\": \"Organization\",\n" +
        "    \"name\": \"GHRSST Project Office\",\n" +
        "    \"email\": \"ghrsst-po@nceo.ac.uk\",\n" +
        "    \"sameAs\": \"https://www.ghrsst.org\"\n" +
        "  },\n" +
        "  \"dateCreated\": \"yyyy-mm-ddThh:mm:ssZ\",\n" + // changes
        "  \"identifier\": \"org.ghrsst/MUR-JPL-L4-GLOB-v04.1\",\n" +
        "  \"version\": \"04.1\",\n" +
        "  \"temporalCoverage\": \"yyyy-mm-ddT09:00:00Z/yyyy-mm-ddT09:00:00Z\",\n" + // end date changes
        "  \"spatialCoverage\": {\n" +
        "    \"@type\": \"Place\",\n" +
        "    \"geo\": {\n" +
        "      \"@type\": \"GeoShape\",\n" +
        "      \"box\": \"-89.99 -179.99 89.99 180\"\n" +
        "    }\n" +
        "  }\n" +
        "}\n" +
        "</script>\n";

    results = results.replaceAll(
        "time_coverage_end=....-..-..T09:00:00Z",
        "time_coverage_end=yyyy-mm-ddT09:00:00Z");
    results = results.replaceAll("....-..-..T09:00:00Z", "yyyy-mm-ddT09:00:00Z");
    results = results.replaceAll("dateCreated\\\": \\\"....-..-..T..:..:..Z",
        "dateCreated\\\": \\\"yyyy-mm-ddThh:mm:ssZ");
    results = results.replaceAll("100_multi_........1200", "100_multi_yyyymmdd1200");
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(results.substring(po, Math.min(results.length(), po + expected.length())),
        expected, "results=\n" + results);
  }

  /**
   * This is used by Bob to do simple tests of Advanced Search.
   * 
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testAdvancedSearch() throws Throwable {
    Erddap.verbose = true;
    Erddap.reallyVerbose = true;
    EDD.testVerboseOn();
    String htmlUrl = EDStatic.erddapUrl + "/search/advanced.html?page=1&itemsPerPage=1000";
    String csvUrl = EDStatic.erddapUrl + "/search/advanced.csv?page=1&itemsPerPage=1000";
    String expected = "cwwcNDBCMet";
    String expected2, query, results;
    String2.log("\n*** Erddap.testAdvancedSearch\n" +
        "This assumes localhost ERDDAP is running with at least cwwcNDBCMet.");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
        "&searchFor=CWWCndbc",
        "&protocol=TAbleDAp",
        "&standard_name=sea_surface_WAVE_significant_height",
        "&minLat=0&maxLat=45",
        "&minLon=-135&maxLon=-120",
        "&minTime=now-20years&maxTime=now-19years" };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
    }

    // valid for .html but error for .csv: protocol
    query = "&searchFor=CWWCndbc&protocol=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Your query produced no matching results. (protocol=gibberish)\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: standard_name
    query = "&searchFor=CWWCndbc&standard_name=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Your query produced no matching results. (standard_name=gibberish)\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minLat > &maxLat
    query = "&searchFor=CWWCndbc&minLat=45&maxLat=0";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: minLat=45.0 > maxLat=0.0\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minTime > &maxTime
    query = "&searchFor=CWWCndbc&minTime=now-10years&maxTime=now-11years";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: minTime=now-10years > maxTime=now-11years\";\n" +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

  }

  /* EDDTableFromNcFiles */

  /**
   * The basic tests of this class.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGlobec() throws Throwable {
    // testVerboseOn();
    int language = 0;
    String2.log("\n*** EDDTableFromNcFiles.testGlobec");
    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String dir = TEMP_DIR.toAbsolutePath().toString() + "/";
    String error = "";
    EDV edv;
    int po, epo;
    // 12 is enough to check day. Hard to check min:sec and hour is more likely to
    // be different
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 12);

    // *** test things that should throw exceptions
    StringArray rv = new StringArray();
    StringArray cv = new StringArray();
    StringArray co = new StringArray();
    StringArray cv2 = new StringArray();

    String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
    userDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03";
    String encodedUserDapQuery = "longitude,NO3,time,ship&latitude%3E0&altitude%3E-5&time%3E=2002-08-03";
    String regexDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03" +
        "&longitude" + PrimitiveArray.REGEX_OP + "\".*11.*\"";

    // testGlobecBottle is like erdGlobecBottle, but with the addition
    // of a fixed value altitude=0 variable
    EDDTable globecBottle = (EDDTable) EDDTestDataset.gettestGlobecBottle(); // should work

    // getEmpiricalMinMax just do once
    // globecBottle.getEmpiricalMinMax(language, "2002-07-01", "2002-09-01", false,
    // true);
    // if (true) System.exit(1);

    // *** test valid queries
    String2.log("\n*** EDDTableFromNcFiles.test valid queries \n");
    globecBottle.parseUserDapQuery(language, "longitude,NO3", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), "longitude, NO3", "");
    Test.ensureEqual(cv.toString(), "", "");
    Test.ensureEqual(co.toString(), "", "");
    Test.ensureEqual(cv2.toString(), "", "");

    globecBottle.parseUserDapQuery(language, "longitude,NO3&altitude=0", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), "longitude, NO3", "");
    Test.ensureEqual(cv.toString(), "altitude", "");
    Test.ensureEqual(co.toString(), "=", "");
    Test.ensureEqual(cv2.toString(), "0", "");

    // test: no resultsVariables interpreted as all resultsVariables
    String allVars = "cruise_id, ship, cast, longitude, latitude, altitude, time, bottle_posn, " +
        "chl_a_total, chl_a_10um, phaeo_total, phaeo_10um, sal00, sal11, temperature0, " +
        "temperature1, fluor_v, xmiss_v, PO4, N_N, NO3, Si, NO2, NH4, oxygen, par";

    globecBottle.parseUserDapQuery(language, "", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), allVars, "");
    Test.ensureEqual(cv.toString(), "", "");
    Test.ensureEqual(co.toString(), "", "");
    Test.ensureEqual(cv2.toString(), "", "");

    globecBottle.parseUserDapQuery(language, "&altitude%3E=0", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), allVars, "");
    Test.ensureEqual(cv.toString(), "altitude", "");
    Test.ensureEqual(co.toString(), ">=", "");
    Test.ensureEqual(cv2.toString(), "0", "");

    globecBottle.parseUserDapQuery(language, "s", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), allVars, "");
    Test.ensureEqual(cv.toString(), "", "");
    Test.ensureEqual(co.toString(), "", "");
    Test.ensureEqual(cv2.toString(), "", "");

    globecBottle.parseUserDapQuery(language, "s&s.altitude=0", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), allVars, "");
    Test.ensureEqual(cv.toString(), "altitude", "");
    Test.ensureEqual(co.toString(), "=", "");
    Test.ensureEqual(cv2.toString(), "0", "");

    globecBottle.parseUserDapQuery(language, "s.longitude,s.altitude&s.altitude=0", rv, cv, co, cv2, false);
    Test.ensureEqual(rv.toString(), "longitude, altitude", "");
    Test.ensureEqual(cv.toString(), "altitude", "");
    Test.ensureEqual(co.toString(), "=", "");
    Test.ensureEqual(cv2.toString(), "0", "");

    // Tests of time related to "now" -- Many fail because this dataset has no
    // recent data.
    GregorianCalendar gc;
    String s;

    gc = Calendar2.newGCalendarZulu();
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now", rv, cv, co, cv2, false); // non-regex
                                                                                         // EDVTimeStamp
                                                                                         // conValues
                                                                                         // will be
                                                                                         // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 14)
              + ".{5}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.SECOND, -7);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now-7seconds", rv, cv, co, cv2, false); // non-regex
                                                                                                  // EDVTimeStamp
                                                                                                  // conValues
                                                                                                  // will
                                                                                                  // be
                                                                                                  // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 14)
              + ".{5}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.MINUTE, -5);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now-5minutes", rv, cv, co, cv2, false); // non-regex
                                                                                                  // EDVTimeStamp
                                                                                                  // conValues
                                                                                                  // will
                                                                                                  // be
                                                                                                  // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 14)
              + ".{5}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.HOUR_OF_DAY, -4);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now-4hours", rv, cv, co, cv2, false); // non-regex
                                                                                                // EDVTimeStamp
                                                                                                // conValues
                                                                                                // will
                                                                                                // be
                                                                                                // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results, // This fails rarely (at minute transitions). Just rerun it.
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.DATE, -2);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now-2days", rv, cv, co, cv2, false); // non-regex
                                                                                               // EDVTimeStamp
                                                                                               // conValues
                                                                                               // will
                                                                                               // be
                                                                                               // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.MONTH, -3);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now-3months", rv, cv, co, cv2, false); // non-regex
                                                                                                 // EDVTimeStamp
                                                                                                 // conValues
                                                                                                 // will
                                                                                                 // be
                                                                                                 // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.YEAR, -2);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(language, "time&time=now-2years", rv, cv, co, cv2, false); // non-regex
                                                                                                // EDVTimeStamp
                                                                                                // conValues
                                                                                                // will
                                                                                                // be
                                                                                                // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(results,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "\\(time=" + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: " +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    // longitude converted to lon
    // altitude is fixed value, so test done by getSourceQueryFromDapQuery
    // time test deferred till later, so 'datetime_epoch' included in sourceQuery
    // also, lat is added to var list because it is used in constraints
    globecBottle.getSourceQueryFromDapQuery(language, userDapQuery, rv, cv, co, cv2);
    Test.ensureEqual(
        EDDTableFromNcFiles.formatAsDapQuery(rv.toArray(), cv.toArray(), co.toArray(),
            cv2.toArray()),
        "lon100,no3,datetime_epoch,ship,lat100&lat100>0&datetime_epoch>=1.0283328E9",
        "Unexpected sourceDapQuery from userDapQuery=" + userDapQuery);

    // test invalid queries
    try {
      // lon is the source name
      globecBottle.getSourceQueryFromDapQuery(language, "lon,cast", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: Unrecognized variable=\"lon\".",
        "error=" + error);

    error = "";
    try {
      // a variable can't be listed twice
      globecBottle.getSourceQueryFromDapQuery(language, "cast,longitude,cast,latitude", rv, cv, co,
          cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: variable=cast is listed twice in the results variables list.",
        "error=" + error);

    error = "";
    try {
      // if s is used, it must be the only request var
      globecBottle.getSourceQueryFromDapQuery(language, "s.latitude,s", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: If s is requested, it must be the only requested variable.",
        "error=" + error);

    error = "";
    try {
      // zztop isn't valid variable
      globecBottle.getSourceQueryFromDapQuery(language, "cast,zztop", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: Unrecognized variable=\"zztop\".", "error=" + error);

    error = "";
    try {
      // alt is fixedValue=0 so will always return NO_DATA
      globecBottle.getSourceQueryFromDapQuery(language, "cast&altitude<-1", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Your query produced no matching results. (altitude<-1 is outside of the variable's actual_range: 0 to 0)",
        "error=" + error);

    error = "";
    try {
      // lon isn't a valid var
      globecBottle.getSourceQueryFromDapQuery(language, "NO3, Si&lon=0", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: Unrecognized constraint variable=\"lon\".",
        "error=" + error);

    error = "";
    try {
      // should be ==, not =
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&altitude==0", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: Use '=' instead of '==' in constraints.",
        "error=" + error);

    error = "";
    try {
      // regex operator should be =~, not ~=
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&altitude~=(0|1.*)", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: Use '=~' instead of '~=' in constraints.",
        "error=" + error);

    error = "";
    try {
      // string regex values must be in quotes
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&ship=New_Horizon", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: For constraints of String variables, the right-hand-side value must be surrounded by double quotes. Bad constraint: ship=New_Horizon",
        "error=" + error);

    error = "";
    try {
      // numeric variable regex values must be in quotes
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&altitude=~(0|1.*)", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: For =~ constraints of numeric variables, " +
            "the right-hand-side value must be surrounded by double quotes. Bad constraint: altitude=~(0|1.*)",
        "error=" + error);

    error = "";
    try {
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&altitude=0|longitude>-180",
          rv, cv, co, cv2); // invalid query format caught as invalid NaN
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(String2.split(error, '\n')[0],
        "SimpleException: Query error: Numeric tests of NaN must use \"NaN\", " +
            "not value=\"0|longitude>-180\".",
        "error=" + error);

    results = Calendar2.epochSecondsToIsoStringTZ(Calendar2.nowStringToEpochSeconds("now-1 day"))
        .substring(0, 16);
    expected = Calendar2.epochSecondsToIsoStringTZ(Calendar2.nowStringToEpochSeconds("now-1day"))
        .substring(0, 16);
    Test.ensureEqual(results, expected, "");

    error = "";
    String nowQ[] = { "nowa", "now-", "now-5.5days", "now-5date", "now-9dayss" };
    for (int i = 0; i < nowQ.length; i++) {
      try {
        globecBottle.getSourceQueryFromDapQuery(language, "time&time=" + nowQ[i],
            rv, cv, co, cv2); // invalid query format caught as invalid NaN
      } catch (Throwable t) {
        error = MustBe.throwableToString(t);
      }
      Test.ensureEqual(String2.split(error, '\n')[0],
          "SimpleException: Query error: Invalid \"now\" constraint: \"" + nowQ[i]
              + "\". " +
              "Timestamp constraints with \"now\" must be in " +
              "the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\""
              +
              " (or singular units).",
          "error=" + error);
    }

    // time tests are perfectly precise: actual_range 1.02272886e+9, 1.02978828e+9;
    // but now have 1 second fudge
    try {
      globecBottle.getSourceQueryFromDapQuery(language, "&time=1.022728858e9", rv, cv, co, cv2); // sec
                                                                                                 // -2
                                                                                                 // in
                                                                                                 // last+1
                                                                                                 // digit
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      error = t.toString();
      Test.ensureEqual(error,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "(time=2002-05-30T03:20:58Z is outside of the variable's actual_range: "
              +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z)",
          "");
    }

    try {
      globecBottle.getSourceQueryFromDapQuery(language, "&time=1.029788282e9", rv, cv, co, cv2); // max
                                                                                                 // +2
                                                                                                 // in
                                                                                                 // last+1
                                                                                                 // digit
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      error = t.toString();
      Test.ensureEqual(error,
          "com.cohort.util.SimpleException: Your query produced no matching results. " +
              "(time=2002-08-19T20:18:02Z is outside of the variable's actual_range: "
              +
              "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z)",
          "");
    }

    // impossible queries lon range is float -126.2, -124.1
    String impossibleQuery[] = new String[] {
        "&longitude!=NaN&longitude<=NaN",
        "&longitude!=NaN&longitude>=NaN",
        "&longitude!=NaN&longitude=NaN",
        "&longitude=NaN&longitude!=NaN",
        "&longitude=NaN&longitude<3.0",
        "&longitude=NaN&longitude<=3.0",
        "&longitude=NaN&longitude>-125.0",
        "&longitude=NaN&longitude>=-125.0",
        "&longitude=NaN&longitude=-125.0",
        "&longitude<2.0&longitude=NaN",
        "&longitude<=2.0&longitude=NaN",
        "&longitude<=2.0&longitude<=NaN",
        "&longitude<-126.0&longitude>-125.0",
        "&longitude<=-126.0&longitude>=-125.0",
        "&longitude<=-126.0&longitude=-125.0",
        "&longitude>-125.0&longitude=NaN",
        "&longitude>=-125.0&longitude=NaN",
        "&longitude>=-125.0&longitude<=NaN",
        "&longitude>-125.0&longitude=-126.0",
        "&longitude>=-125.0&longitude=-126.0",
        "&longitude>=-125.0&longitude<=-126.0",
        "&longitude=-125.0&longitude<=NaN",
        "&longitude=-125.0&longitude>=NaN",
        "&longitude=-125.0&longitude=NaN",
        "&longitude=-126.0&longitude>-125.0",
        "&longitude=-126.0&longitude>=-125.0",
        "&longitude=-125.0&longitude<-126.0",
        "&longitude=-125.0&longitude<=-126.0",
        "&longitude=-125.0&longitude=-126.0",
        "&longitude=-126.0&longitude!=-126.0"
    };
    for (int i = 0; i < impossibleQuery.length; i++) {
      error = "";
      try {
        globecBottle.getSourceQueryFromDapQuery(language, impossibleQuery[i], rv, cv, co, cv2);
      } catch (Throwable t) {
        error = MustBe.throwableToString(t);
      }
      Test.ensureEqual(String2.split(error, '\n')[0],
          "SimpleException: Query error: " +
              String2.replaceAll(impossibleQuery[i].substring(1), "&",
                  " and ")
              +
              " will never both be true.",
          "error=" + error);
    }
    // possible queries lon range is float -126.2, -124.1
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=-126.2", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<=-126.2", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<-126.2", rv, cv, co, cv2); // good: fuzzy
                                                                                             // test
                                                                                             // allows it
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=-124.1", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>=-124.1", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>-124.1", rv, cv, co, cv2); // good: fuzzy
                                                                                             // test
                                                                                             // allows it
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<-126.0&longitude<=-125.0", rv, cv, co,
        cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>=-126.0&longitude>-125.0", rv, cv, co,
        cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=-126.0&longitude<-125.0", rv, cv, co,
        cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>-126.0&longitude=-125.0", rv, cv, co,
        cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude!=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<-126.0&longitude!=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude!=NaN&longitude=-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=NaN&longitude<=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<=NaN&longitude=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=NaN&longitude!=-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude!=-125.0&longitude=NaN", rv, cv, co, cv2);
    // time tests are perfectly precise: actual_range 1.02272886e+9, 1.02978828e+9;
    globecBottle.getSourceQueryFromDapQuery(language, "&time=1.02272886e9", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&time<1.02272886e9", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&time<=1.02272886e9", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&time=1.02978828e9", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&time>1.02978828e9", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&time>=1.02978828e9", rv, cv, co, cv2);

    // *** test dapInstructions
    // StringWriter sw = new StringWriter();
    // writeGeneralDapHtmlDocument(language, EDStatic.erddapUrl, sw); //for testing,
    // use the non-https url
    // results = sw.toString();
    // expected = "Requests for Tabular Data in ";
    // Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    // expected = "In ERDDAP, time variables always have the name \"" +
    // EDV.TIME_NAME + "\"";
    // Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // test sliderCsvValues
    edv = globecBottle.findDataVariableByDestinationName("longitude");
    results = edv.sliderCsvValues();
    expected = "-126.2, -126.19, -126.18, -126.17, -126.16,";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = ".13, -124.12, -124.11, -124.1";
    Test.ensureEqual(results.substring(results.length() - expected.length()), expected,
        "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("latitude");
    results = edv.sliderCsvValues();
    expected = "41.9, 41.92, 41.94, 41.96, 41.98";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "44.56, 44.58, 44.6, 44.62, 44.64, 44.65";
    Test.ensureEqual(results.substring(results.length() - expected.length()), expected,
        "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("altitude");
    results = edv.sliderCsvValues();
    expected = "0"; // 0
    Test.ensureEqual(results, expected, "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("time");
    results = edv.sliderCsvValues();
    // 2002-05-30T03:21:00Z 2002-08-19T20:18:00Z
    expected = "\"2002-05-30T03:21:00Z\", \"2002-05-30T12:00:00Z\", \"2002-05-31\", \"2002-05-31T12:00:00Z\",";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "\"2002-08-19\", \"2002-08-19T12:00:00Z\", \"2002-08-19T20:18:00Z\"";
    Test.ensureEqual(results.substring(results.length() - expected.length()), expected,
        "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("phaeo_total");
    results = edv.sliderCsvValues();
    expected = "-3.111, -3, -2.8, -2.6, -2.4, -2.2,"; // -3.111
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "32.8, 33, 33.2, 33.4, 33.6, 33.821"; // 33.821
    Test.ensureEqual(results.substring(results.length() - expected.length()), expected,
        "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("cruise_id");
    results = edv.sliderCsvValues();
    expected = null;
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** print ncdump of file (useful for diagnosing problems in next section)
    tName = "/u00/data/points/globec/Globec_bottle_data_2002.nc";
    String2.log(tName + "\n" + NcHelper.ncdump(tName, "-h"));

    // *** test getting das for entire dataset
    String2.log("\n*** EDDTableFromNcFiles.test das dds for entire dataset\n");
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, "", dir,
        globecBottle.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    String expectedDas1 = // see OpendapHelper.EOL for comments
        "Attributes {\n" +
            " s {\n" +
            "  cruise_id {\n" +
            "    String cf_role \"trajectory_id\";\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Cruise ID\";\n" +
            "  }\n" +
            "  ship {\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Ship\";\n" +
            "  }\n" +
            "  cast {\n" +
            "    Int16 _FillValue 32767;\n" +
            "    Int16 actual_range 1, 127;\n" +
            "    Float64 colorBarMaximum 140.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Cast Number\";\n" +
            "    Int16 missing_value 32767;\n" +
            "  }\n" +
            "  longitude {\n" +
            "    String _CoordinateAxisType \"Lon\";\n" +
            "    Float32 _FillValue 327.67;\n" +
            "    Float32 actual_range -126.2, -124.1;\n" +
            "    String axis \"X\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Longitude\";\n" +
            "    Float32 missing_value 327.67;\n" +
            "    String standard_name \"longitude\";\n" +
            "    String units \"degrees_east\";\n" +
            "  }\n" +
            "  latitude {\n" +
            "    String _CoordinateAxisType \"Lat\";\n" +
            "    Float32 _FillValue 327.67;\n" +
            "    Float32 actual_range 41.9, 44.65;\n" +
            "    String axis \"Y\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Latitude\";\n" +
            "    Float32 missing_value 327.67;\n" +
            "    String standard_name \"latitude\";\n" +
            "    String units \"degrees_north\";\n" +
            "  }\n" +
            "  altitude {\n" +
            "    String _CoordinateAxisType \"Height\";\n" +
            "    String _CoordinateZisPositive \"up\";\n" +
            "    Int32 actual_range 0, 0;\n" +
            "    String axis \"Z\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Altitude\";\n" +
            "    String positive \"up\";\n" +
            "    String standard_name \"altitude\";\n" +
            "    String units \"m\";\n" +
            "  }\n" +
            "  time {\n" +
            "    String _CoordinateAxisType \"Time\";\n" +
            "    Float64 actual_range 1.02272886e+9, 1.02978828e+9;\n" +
            "    String axis \"T\";\n" +
            "    String cf_role \"profile_id\";\n" +
            "    String ioos_category \"Time\";\n" +
            "    String long_name \"Time\";\n" +
            "    String standard_name \"time\";\n" +
            "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
            "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "  }\n" +
            "  bottle_posn {\n" +
            "    Byte _FillValue 127;\n" +
            "    String _Unsigned \"false\";\n" + // ERDDAP adds
            "    Byte actual_range 0, 12;\n" +
            "    Float64 colorBarMaximum 12.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Bottle Number\";\n" +
            "    Byte missing_value -128;\n" +
            "  }\n" +
            "  chl_a_total {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range -2.602, 40.17;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Chlorophyll-a\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            +
            "    String units \"ug L-1\";\n" +
            "  }\n";
    Test.ensureEqual(results.substring(0, expectedDas1.length()), expectedDas1, "\nresults=\n" + results);

    String expectedDas2 = "String id \"Globec_bottle_data_2002\";\n" +
        "    String infoUrl \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n"
        +
        "    String institution \"GLOBEC\";\n" +
        "    String keywords \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
        +
        "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
        "    String license \"The data may be used and redistributed for free but is not intended\n"
        +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "    Float64 Northernmost_Northing 44.65;\n" +
        "    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
        "    Float64 Southernmost_Northing 41.9;\n" +
        "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
        "    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
        "    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n"
        +
        "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
        "Notes:\n" +
        "Physical data processed by Jane Fleischbein (OSU).\n" +
        "Chlorophyll readings done by Leah Feinberg (OSU).\n" +
        "Nutrient analysis done by Burke Hales (OSU).\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
        "secondary sensor pair was used in final processing of CTD data for\n" +
        "most stations because the primary had more noise and spikes. The\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
        "multiple spikes or offsets in the secondary pair.\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\n" +
        "data developed by Burke Hales (OSU).\n" +
        "Operation Detection Limits for Nutrient Concentrations\n" +
        "Nutrient  Range         Mean    Variable         Units\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
        "Dates and Times are UTC.\n" +
        "\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\n" +
        "\n" +
        "Inquiries about how to access this data should be directed to\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
        "    String time_coverage_end \"2002-08-19T20:18:00Z\";\n" +
        "    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
        "    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
        "    Float64 Westernmost_Easting -126.2;\n" +
        "  }\n" +
        "}\n";
    po = results.indexOf(expectedDas2.substring(0, 17));
    Test.ensureEqual(results.substring(Math.max(0, po)), expectedDas2, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, "", dir,
        globecBottle.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    String cruise_id;\n" +
        "    String ship;\n" +
        "    Int16 cast;\n" +
        "    Float32 longitude;\n" +
        "    Float32 latitude;\n" +
        "    Int32 altitude;\n" +
        "    Float64 time;\n" +
        "    Byte bottle_posn;\n" +
        "    Float32 chl_a_total;\n" +
        "    Float32 chl_a_10um;\n" +
        "    Float32 phaeo_total;\n" +
        "    Float32 phaeo_10um;\n" +
        "    Float32 sal00;\n" +
        "    Float32 sal11;\n" +
        "    Float32 temperature0;\n" +
        "    Float32 temperature1;\n" +
        "    Float32 fluor_v;\n" +
        "    Float32 xmiss_v;\n" +
        "    Float32 PO4;\n" +
        "    Float32 N_N;\n" +
        "    Float32 NO3;\n" +
        "    Float32 Si;\n" +
        "    Float32 NO2;\n" +
        "    Float32 NH4;\n" +
        "    Float32 oxygen;\n" +
        "    Float32 par;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, "", dir,
        globecBottle.className() + "_Entire", ".html");
    // Test.displayInBrowser("file://" + dir + tName);

    // *** test make data files
    String2.log("\n*** EDDTableFromNcFiles.test make DATA FILES\n");

    // .asc
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".asc");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected = "Dataset {[10]\n" +
        "  Sequence {[10]\n" +
        "    Float32 longitude;[10]\n" +
        "    Float32 NO3;[10]\n" +
        "    Float64 time;[10]\n" +
        "    String ship;[10]\n" +
        "  } s;[10]\n" +
        "} s;[10]\n" +
        "---------------------------------------------[10]\n" +
        "s.longitude, s.NO3, s.time, s.ship[10]\n" +
        "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"[10]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"[10]\n"; // row with missing value has source
                                                                       // missing
                                                                       // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1, 24.45, 1.02978828E9, \"New_Horizon\"[10]\n[end]"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery,
        dir, globecBottle.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; // row with missing value has "NaN" missing
                                                                 // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csvp and &units("UCUM")
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery + "&units(\"UCUM\")",
        dir,
        globecBottle.className() + "_Data", ".csvp");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude (deg{east}),NO3 (umol.l-1),time (UTC),ship\n" +
        "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; // row with missing value has "NaN" missing
                                                                 // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv0
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery,
        dir,
        globecBottle.className() + "_Data", ".csv0");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; // row with missing value has "NaN" missing
                                                                 // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv test of datasetName.dataVarName notation
    String dotDapQuery = "s.longitude,altitude,NO3,s.time,ship" +
        "&s.latitude>0&altitude>-5&s.time>=2002-08-03";
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, dotDapQuery, dir,
        globecBottle.className() + "_DotNotation", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,altitude,NO3,time,ship\n" +
        "degrees_east,m,micromoles L-1,UTC,\n" +
        "-124.4,0,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,0,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,0,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected = "-124.1,0,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv test of regex on numeric variable
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, regexDapQuery, dir,
        globecBottle.className() + "_NumRegex", ".csv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-125.11,33.91,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,26.61,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,10.8,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,8.42,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,6.34,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,1.29,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,0.02,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,0.0,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,10.81,2002-08-09T05:03:00Z,New_Horizon\n" +
        "-125.11,42.39,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,33.84,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,27.67,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,15.93,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,8.69,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,4.6,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,2.17,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,8.61,2002-08-18T23:49:00Z,New_Horizon\n" +
        "-125.11,0.64,2002-08-18T23:49:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv test of String=
    String tDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5" +
        "&time>=2002-08-07T00&time<=2002-08-07T06&ship=\"New_Horizon\"";
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, tDapQuery, dir,
        globecBottle.className() + "_StrEq", ".csv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv test of String< >
    tDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5" +
        "&time>=2002-08-07T00&time<=2002-08-07T06&ship>\"Nev\"&ship<\"Nex\"";
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, tDapQuery, dir,
        globecBottle.className() + "_GTLT", ".csv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv test of String regex
    // !!! I also tested this with
    // <sourceCanConstrainStringRegex>~=</sourceCanConstrainStringRegex>
    // but it fails:
    // Exception in thread "main" dods.dap.DODSException: "Your Query Produced No
    // Matching Results."
    // and it isn't an encoding problem, opera encodes unencoded request as
    // https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.dods?lon,NO3,datetime_epoch,ship,lat&lat%3E0&datetime_epoch%3E=1.0286784E9&datetime_epoch%3C=1.0287E9&ship~=%22(zztop|.*Horiz.*)%22
    // which fails the same way
    // Other simpler regex tests succeed.
    // It seems that the regex syntax is different for drds than erddap/java.
    // So recommend that people not say that drds servers can constrain String regex
    tDapQuery = "longitude,NO3,time,ship&latitude>0" +
        "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\"(zztop|.*Horiz.*)\""; // source fails
                                                                                // with this
    // "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\".*Horiz.*\""; //source
    // works with this
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, tDapQuery, dir,
        globecBottle.className() + "_regex", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n" +
        "-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n" +
        "-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dataTable
    tDapQuery = "longitude,NO3,time,ship&latitude>0" +
        "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\"(zztop|.*Horiz.*)\""; // source fails
                                                                                // with this
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, tDapQuery, dir,
        globecBottle.className() + "_regex_dataTable", ".dataTable");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = // note that month is 0-based
        "{\"cols\":[{\"id\":\"longitude\",\"label\":\"longitude (degrees_east) \",\"pattern\":\"\",\"type\":\"number\"},{\"id\":\"NO3\",\"label\":\"NO3 (micromoles L-1) \",\"pattern\":\"\",\"type\":\"number\"},{\"id\":\"time\",\"label\":\"time\",\"pattern\":\"\",\"type\":\"datetime\"},{\"id\":\"ship\",\"label\":\"ship\",\"pattern\":\"\",\"type\":\"string\"}],\n"
            +
            "\"rows\": [\n" +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":34.54,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":29.98,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":17.24,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":12.74,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":11.43,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":null,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":9.74,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":5.62,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":4.4,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":4.21,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":35.28,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":30.87,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":25.2,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":20.66,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":null,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":null,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":10.85,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":5.44,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            +
            "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":4.69,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]}\n"
            +
            "    ]\n" +
            "  }\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .das das isn't affected by userDapQuery
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    Test.ensureEqual(
        results.substring(0, expectedDas1.length()), expectedDas1, "results=\n" + results);

    int tpo = results.indexOf(expectedDas2.substring(0, 17));
    Test.ensureEqual(results.substring(Math.max(tpo, 0)), expectedDas2, "results=\n" + results);

    // .dds
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".dds");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected = "Dataset {[10]\n" +
        "  Sequence {[10]\n" +
        "    Float32 longitude;[10]\n" +
        "    Float32 NO3;[10]\n" +
        "    Float64 time;[10]\n" +
        "    String ship;[10]\n" +
        "  } s;[10]\n" +
        "} s;[10]\n" +
        "[end]";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dods
    // tName = globecBottle.makeNewFileForDapQuery(language, null, null,
    // userDapQuery, dir,
    // globecBottle.className() + "_Data", ".dods");
    // Test.displayInBrowser("file://" + dir + tName);
    try {
      String2.log("\ndo .dods test");
      String tUrl = EDStatic.erddapUrl + // in tests, always use non-https url
          "/tabledap/" + globecBottle.datasetID();
      // for diagnosing during development:
      // String2.log(String2.annotatedString(SSR.getUrlResponseStringUnchanged(
      // "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
      // String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseStringUnchanged(tUrl +
      // ".das?" + userDapQuery));
      // String2.log("\nDODS RESPONSE=" +
      // String2.annotatedString(SSR.getUrlResponseStringUnchanged(tUrl + ".dods?" +
      // userDapQuery)));

      // test if table.readOpendapSequence works with Erddap opendap server
      // !!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in
      // tests, always use non-https url
      // !!!THIS IS NOT JUST A LOCAL TEST!!!
      Table tTable = new Table();
      tTable.readOpendapSequence(tUrl + "?" + encodedUserDapQuery, false); // 2016-12-07 non-encoded
                                                                           // no longer
                                                                           // works: http error 400:
                                                                           // malformed
                                                                           // request
      Test.ensureEqual(tTable.globalAttributes().getString("title"),
          "GLOBEC NEP Rosette Bottle Data (2002)", "");
      Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
      Test.ensureEqual(tTable.getColumnNames(), new String[] { "longitude", "NO3", "time", "ship" },
          "");
      Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
      Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
      Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
      Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
      String2.log("  .dods test succeeded");
    } catch (Throwable t) {
      throw new RuntimeException("*** This test requires " + globecBottle.datasetID() +
          " in localhost ERDDAP.", t);
    }

    // .esriCsv
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        "&time>=2002-08-03",
        dir,
        "testEsri5", ".esriCsv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "cruise_id,ship,cast,X,Y,altitude,date,time,bottle_pos,chl_a_tota,chl_a_10um,phaeo_tota,phaeo_10um,sal00,sal11,temperatur,temperatuA,fluor_v,xmiss_v,PO4,N_N,NO3,Si,NO2,NH4,oxygen,par\n"
        +
        "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,1,-9999.0,-9999.0,-9999.0,-9999.0,33.9939,33.9908,7.085,7.085,0.256,0.518,2.794,35.8,35.7,71.11,0.093,0.037,-9999.0,0.1545\n"
        +
        "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,2,-9999.0,-9999.0,-9999.0,-9999.0,33.8154,33.8111,7.528,7.53,0.551,0.518,2.726,35.87,35.48,57.59,0.385,0.018,-9999.0,0.1767\n"
        +
        "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,3,1.463,-9999.0,1.074,-9999.0,33.5858,33.5834,7.572,7.573,0.533,0.518,2.483,31.92,31.61,48.54,0.307,0.504,-9999.0,0.3875\n"
        +
        "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,4,2.678,-9999.0,1.64,-9999.0,33.2905,33.2865,8.093,8.098,1.244,0.518,2.262,27.83,27.44,42.59,0.391,0.893,-9999.0,0.7674\n"
        +
        "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,5,4.182,-9999.0,2.363,-9999.0,33.2871,33.2863,8.157,8.141,1.458,0.518,2.202,26.15,25.73,40.25,0.424,1.204,-9999.0,0.7609\n"
        +
        "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,6,7.601,-9999.0,3.959,-9999.0,33.3753,33.3678,11.733,11.73,3.685,0.518,1.092,8.96,8.75,16.31,0.211,1.246,-9999.0,1.9563\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // .geoJson mapDapQuery so lon and lat are in query
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, mapDapQuery, dir,
        globecBottle.className() + "_DataGJ", ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "{\n" +
        "  \"type\": \"FeatureCollection\",\n" +
        "  \"propertyNames\": [\"NO3\", \"time\"],\n" +
        "  \"propertyUnits\": [\"micromoles L-1\", \"UTC\"],\n" +
        "  \"features\": [\n" +
        "{\"type\": \"Feature\",\n" +
        "  \"geometry\": {\n" +
        "    \"type\": \"Point\",\n" +
        "    \"coordinates\": [-124.4, 44.0] },\n" +
        "  \"properties\": {\n" +
        "    \"NO3\": 35.7,\n" +
        "    \"time\": \"2002-08-03T01:29:00Z\" }\n" +
        "},\n" +
        "{\"type\": \"Feature\",\n" +
        "  \"geometry\": {\n" +
        "    \"type\": \"Point\",\n" +
        "    \"coordinates\": [-124.4, 44.0] },\n" +
        "  \"properties\": {\n" +
        "    \"NO3\": 35.48,\n" +
        "    \"time\": \"2002-08-03T01:29:00Z\" }\n" +
        "},\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    expected = "{\"type\": \"Feature\",\n" +
        "  \"geometry\": {\n" +
        "    \"type\": \"Point\",\n" +
        "    \"coordinates\": [-124.1, 44.65] },\n" +
        "  \"properties\": {\n" +
        "    \"NO3\": 24.45,\n" +
        "    \"time\": \"2002-08-19T20:18:00Z\" }\n" +
        "}\n" +
        "  ],\n" +
        "  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
        "}\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .geoJson just lon and lat in response
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03",
        dir, globecBottle.className() + "_DataGJLL", ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "{\n" +
        "  \"type\": \"MultiPoint\",\n" +
        "  \"coordinates\": [\n" +
        "[-124.4, 44.0],\n" +
        "[-124.4, 44.0],\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    expected = "[-124.1, 44.65]\n" +
        "  ],\n" +
        "  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
        "}\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .geoJson with jsonp
    String jsonp = "myFunctionName";
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03" + "&.jsonp="
            + SSR.percentEncode(jsonp),
        dir, globecBottle.className() + "_DataGJLL", ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = jsonp + "(" +
        "{\n" +
        "  \"type\": \"MultiPoint\",\n" +
        "  \"coordinates\": [\n" +
        "[-124.4, 44.0],\n" +
        "[-124.4, 44.0],\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    expected = "[-124.1, 44.65]\n" +
        "  ],\n" +
        "  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" +
        "}\n" +
        ")";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .htmlTable
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".htmlTable");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = EDStatic.startHeadHtml(language, EDStatic.erddapUrl((String) null, language),
        "EDDTableFromNcFiles_Data") + "\n" +
        "</head>\n" +
        EDStatic.startBodyHtml(language, null, "tabledap/testGlobecBottle.html", userDapQuery) + // 2022-11-22
                                                                                                 // .htmlTable
                                                                                                 // converted
                                                                                                 // to
                                                                                                 // .html
                                                                                                 // to
                                                                                                 // avoid
                                                                                                 // user
                                                                                                 // requesting
                                                                                                 // all
                                                                                                 // data
                                                                                                 // in
                                                                                                 // a
                                                                                                 // dataset
                                                                                                 // if
                                                                                                 // they
                                                                                                 // change
                                                                                                 // language
        "&nbsp;<br>\n" +
        // HtmlWidgets.BACK_BUTTON +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>longitude\n" +
        "<th>NO3\n" +
        "<th>time\n" +
        "<th>ship\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<th>degrees_east\n" +
        "<th>micromoles L-1\n" +
        "<th>UTC\n" +
        "<th>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td class=\"R\">-124.4\n" +
        "<td class=\"R\">35.7\n" +
        "<td>2002-08-03T01:29:00Z\n" +
        "<td>New_Horizon\n" +
        "</tr>\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);
    expected = // row with missing value has "&nbsp;" missing value
        "<tr>\n" +
            "<td class=\"R\">-124.1\n" +
            "<td class=\"R\">24.45\n" +
            "<td>2002-08-19T20:18:00Z\n" +
            "<td>New_Horizon\n" +
            "</tr>\n" +
            "</table>\n" +
            EDStatic.endBodyHtml(language,
                EDStatic.erddapUrl((String) null, language),
                (String) null)
            + "\n" +
            "</html>\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .json
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".json");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n" +
        "    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n" +
        "    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n" +
        "    \"rows\": [\n" +
        "      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n" +
        "      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "      [-125, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"],\n"; // row with missing value
                                                                                   // has
                                                                                   // "null". Before
                                                                                   // 2018-05-17 was
                                                                                   // -125.0.
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        "    ]\n" +
        "  }\n" +
        "}\n"; // last rows
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .json with jsonp query
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp),
        dir,
        globecBottle.className() + "_Data", ".json");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = jsonp + "(" +
        "{\n" +
        "  \"table\": {\n" +
        "    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n" +
        "    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n" +
        "    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n" +
        "    \"rows\": [\n" +
        "      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n" +
        "      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        "    ]\n" +
        "  }\n" +
        "}\n" +
        ")"; // last rows
    Test.ensureEqual(results.substring(results.length() - expected.length()),
        expected, "\nresults=\n" + results);

    // .jsonlCSV1
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".jsonlCSV1");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "[\"longitude\", \"NO3\", \"time\", \"ship\"]\n" +
        "[-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n" +
        "[-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "[-125, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"]\n"; // row with missing value has
                                                                            // "null"
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "[-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .jsonlCSV
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".jsonlCSV");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "[-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n" +
        "[-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "[-125, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"]\n"; // row with missing value has
                                                                            // "null"
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "[-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .jsonlCSV with jsonp query
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp),
        dir,
        globecBottle.className() + "_Data", ".jsonlCSV");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = jsonp + "(\n" +
        "[-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n" +
        "[-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "[-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" +
        ")";
    Test.ensureEqual(results.substring(results.length() - expected.length()),
        expected, "\nresults=\n" + results);

    // .jsonlKVP
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".jsonlKVP");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "{\"longitude\":-124.4, \"NO3\":35.7, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n"
        +
        "{\"longitude\":-124.4, \"NO3\":35.48, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "{\"longitude\":-125, \"NO3\":null, \"time\":\"2002-08-18T13:03:00Z\", \"ship\":\"New_Horizon\"}\n"; // row
                                                                                                                    // with
                                                                                                                    // missing
                                                                                                                    // value
                                                                                                                    // has
                                                                                                                    // "null"
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "{\"longitude\":-124.1, \"NO3\":24.45, \"time\":\"2002-08-19T20:18:00Z\", \"ship\":\"New_Horizon\"}\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .jsonlKVP with jsonp query
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp),
        dir,
        globecBottle.className() + "_Data", ".jsonlKVP");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = jsonp + "(\n" +
        "{\"longitude\":-124.4, \"NO3\":35.7, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n"
        +
        "{\"longitude\":-124.4, \"NO3\":35.48, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "{\"longitude\":-124.1, \"NO3\":24.45, \"time\":\"2002-08-19T20:18:00Z\", \"ship\":\"New_Horizon\"}\n"
        +
        ")";
    Test.ensureEqual(results.substring(results.length() - expected.length()),
        expected, "\nresults=\n" + results);

    // .mat [I can't test that missing value is NaN.]
    // octave> load('c:/temp/tabledap/EDDTableFromNcFiles_Data.mat');
    // octave> testGlobecBottle
    // 2010-07-14 Roy can read this file in Matlab, previously. text didn't show up.
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, regexDapQuery, dir,
        globecBottle.className() + "_Data", ".mat");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.hexDump(dir + tName, 1000000);
    // String2.log(results);
    Test.ensureEqual(
        results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation
                                                                  // dateTime
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" +
            "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            +
            "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            +
            "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            +
            // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 4d 6f , Created on: Mo
            // |\n" +
            // "6e 20 44 65 63 20 38 20 31 32 3a 34 35 3a 34 36 n Dec 8 12:45:46
            // |\n" +
            // "20 32 30 30 38 20 20 20 20 20 20 20 20 20 20 20 2008 |\n" +
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            +
            "00 00 00 0e 00 00 04 58   00 00 00 06 00 00 00 08          X         |\n"
            +
            "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            +
            "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 10                    |\n"
            +
            "74 65 73 74 47 6c 6f 62   65 63 42 6f 74 74 6c 65   testGlobecBottle |\n"
            +
            "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 80                    |\n"
            +
            "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n"
            +
            "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            +
            "4e 4f 33 00 00 00 00 00   00 00 00 00 00 00 00 00   NO3              |\n"
            +
            "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            +
            "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n"
            +
            "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            +
            "73 68 69 70 00 00 00 00   00 00 00 00 00 00 00 00   ship             |\n"
            +
            "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            +
            "00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n"
            +
            "00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            +
            "00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            +
            "00 00 00 07 00 00 00 48   c2 fa 38 52 c2 fa 38 52          H  8R  8R |\n"
            +
            "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            +
            "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            +
            "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            +
            "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            +
            "00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n"
            +
            "00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            +
            "00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            +
            "00 00 00 07 00 00 00 48   42 07 a3 d7 41 d4 e1 48          HB   A  H |\n"
            +
            "41 2c cc cd 41 06 b8 52   40 ca e1 48 3f a5 1e b8   A,  A  R@  H?    |\n"
            +
            "3c a3 d7 0a 00 00 00 00   41 2c f5 c3 42 29 8f 5c   <       A,  B) \\ |\n"
            +
            "42 07 5c 29 41 dd 5c 29   41 7e e1 48 41 0b 0a 3d   B \\)A \\)A~ HA  = |\n"
            +
            "40 93 33 33 40 0a e1 48   41 09 c2 8f 3f 23 d7 0a   @ 33@  HA   ?#   |\n"
            +
            "00 00 00 0e 00 00 00 c0   00 00 00 06 00 00 00 08                    |\n"
            +
            "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            +
            "00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            +
            "00 00 00 09 00 00 00 90   41 ce a9 a6 82 00 00 00           A        |\n"
            +
            "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            +
            "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            +
            "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            +
            "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            +
            "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            +
            "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            +
            "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            +
            "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            +
            "41 ce b0 19 36 00 00 00   00 00 00 0e 00 00 01 c0   A   6            |\n"
            +
            "00 00 00 06 00 00 00 08   00 00 00 04 00 00 00 00                    |\n"
            +
            "00 00 00 05 00 00 00 08   00 00 00 12 00 00 00 0b                    |\n"
            +
            "00 00 00 01 00 00 00 00   00 00 00 04 00 00 01 8c                    |\n"
            +
            "00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n"
            +
            "00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n"
            +
            "00 4e 00 4e 00 65 00 65   00 65 00 65 00 65 00 65    N N e e e e e e |\n"
            +
            "00 65 00 65 00 65 00 65   00 65 00 65 00 65 00 65    e e e e e e e e |\n"
            +
            "00 65 00 65 00 65 00 65   00 77 00 77 00 77 00 77    e e e e w w w w |\n"
            +
            "00 77 00 77 00 77 00 77   00 77 00 77 00 77 00 77    w w w w w w w w |\n"
            +
            "00 77 00 77 00 77 00 77   00 77 00 77 00 5f 00 5f    w w w w w w _ _ |\n"
            +
            "00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n"
            +
            "00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n"
            +
            "00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n"
            +
            "00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n"
            +
            "00 48 00 48 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    H H o o o o o o |\n"
            +
            "00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n"
            +
            "00 6f 00 6f 00 6f 00 6f   00 72 00 72 00 72 00 72    o o o o r r r r |\n"
            +
            "00 72 00 72 00 72 00 72   00 72 00 72 00 72 00 72    r r r r r r r r |\n"
            +
            "00 72 00 72 00 72 00 72   00 72 00 72 00 69 00 69    r r r r r r i i |\n"
            +
            "00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n"
            +
            "00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n"
            +
            "00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n"
            +
            "00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n"
            +
            "00 7a 00 7a 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    z z o o o o o o |\n"
            +
            "00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n"
            +
            "00 6f 00 6f 00 6f 00 6f   00 6e 00 6e 00 6e 00 6e    o o o o n n n n |\n"
            +
            "00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 6e 00 6e    n n n n n n n n |\n"
            +
            "00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 00 00 00    n n n n n n     |\n",
        "\nresults=\n" + results);

    // .nc
    // !!! This is also a test of missing_value and _FillValue both active
    String tUserDapQuery = "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15";
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, tUserDapQuery,
        dir, globecBottle.className() + "_Data", ".nc");
    results = NcHelper.ncdump(dir + tName, "");
    String tHeader1 = "netcdf EDDTableFromNcFiles_Data.nc {\n" +
        "  dimensions:\n" +
        "    row = 100;\n" +
        "    ship_strlen = 11;\n" +
        "  variables:\n" +
        "    float longitude(row=100);\n" +
        "      :_CoordinateAxisType = \"Lon\";\n" +
        "      :_FillValue = 327.67f; // float\n" +
        "      :actual_range = -125.67f, -124.8f; // float\n" +
        "      :axis = \"X\";\n" +
        "      :ioos_category = \"Location\";\n" +
        "      :long_name = \"Longitude\";\n" +
        "      :missing_value = 327.67f; // float\n" +
        "      :standard_name = \"longitude\";\n" +
        "      :units = \"degrees_east\";\n" +
        "\n" +
        "    float NO3(row=100);\n" +
        "      :_FillValue = -99.0f; // float\n" +
        "      :actual_range = 0.46f, 34.09f; // float\n" +
        "      :colorBarMaximum = 50.0; // double\n" +
        "      :colorBarMinimum = 0.0; // double\n" +
        "      :ioos_category = \"Dissolved Nutrients\";\n" +
        "      :long_name = \"Nitrate\";\n" +
        "      :missing_value = -9999.0f; // float\n" +
        "      :standard_name = \"mole_concentration_of_nitrate_in_sea_water\";\n" +
        "      :units = \"micromoles L-1\";\n" +
        "\n" +
        "    double time(row=100);\n" +
        "      :_CoordinateAxisType = \"Time\";\n" +
        "      :actual_range = 1.02928674E9, 1.02936804E9; // double\n" +
        "      :axis = \"T\";\n" +
        "      :cf_role = \"profile_id\";\n" +
        "      :ioos_category = \"Time\";\n" +
        "      :long_name = \"Time\";\n" +
        "      :standard_name = \"time\";\n" +
        "      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
        "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
        "\n" +
        "    char ship(row=100, ship_strlen=11);\n" +
        "      :_Encoding = \"ISO-8859-1\";\n" +
        "      :ioos_category = \"Identifier\";\n" +
        "      :long_name = \"Ship\";\n" +
        "\n" +
        "  // global attributes:\n" +
        "  :cdm_data_type = \"TrajectoryProfile\";\n" +
        "  :cdm_profile_variables = \"cast, longitude, latitude, time\";\n" +
        "  :cdm_trajectory_variables = \"cruise_id, ship\";\n" +
        "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" +
        "  :Easternmost_Easting = -124.8f; // float\n" +
        "  :featureType = \"TrajectoryProfile\";\n" +
        "  :geospatial_lat_units = \"degrees_north\";\n" +
        "  :geospatial_lon_max = -124.8f; // float\n" +
        "  :geospatial_lon_min = -125.67f; // float\n" +
        "  :geospatial_lon_units = \"degrees_east\";\n" +
        "  :geospatial_vertical_positive = \"up\";\n" +
        "  :geospatial_vertical_units = \"m\";\n" +
        "  :history = \"" + today;
    tResults = results.substring(0, tHeader1.length());
    Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);

    // + " https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    String tHeader2 = "/tabledap/testGlobecBottle.nc?longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15\";\n"
        +
        "  :id = \"Globec_bottle_data_2002\";\n" +
        "  :infoUrl = \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n" +
        "  :institution = \"GLOBEC\";\n" +
        "  :keywords = \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
        +
        "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
        "  :license = \"The data may be used and redistributed for free but is not intended\n" +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n" +
        "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" +
        "  :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
        "  :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
        "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
        "Notes:\n" +
        "Physical data processed by Jane Fleischbein (OSU).\n" +
        "Chlorophyll readings done by Leah Feinberg (OSU).\n" +
        "Nutrient analysis done by Burke Hales (OSU).\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
        "secondary sensor pair was used in final processing of CTD data for\n" +
        "most stations because the primary had more noise and spikes. The\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
        "multiple spikes or offsets in the secondary pair.\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\n" +
        "data developed by Burke Hales (OSU).\n" +
        "Operation Detection Limits for Nutrient Concentrations\n" +
        "Nutrient  Range         Mean    Variable         Units\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
        "Dates and Times are UTC.\n" +
        "\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\n" +
        "\n" +
        "Inquiries about how to access this data should be directed to\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
        "  :time_coverage_end = \"2002-08-14T23:34:00Z\";\n" +
        "  :time_coverage_start = \"2002-08-14T00:59:00Z\";\n" +
        "  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
        "  :Westernmost_Easting = -125.67f; // float\n";
    int tPo = results.indexOf(tHeader2.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo, tPo + tHeader2.length()), tHeader2,
        "results=\n" + results);

    expected = " data:\n" +
        "    longitude = \n" +
        "      {-124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2}\n"
        +
        "    NO3 = \n" +
        "      {33.66, 30.43, 28.22, 26.4, 25.63, 23.54, 22.38, 20.15, 33.55, 31.48, 24.93, -99.0, 21.21, 20.54, 17.87, -9999.0, 16.32, 33.61, 33.48, 30.7, 27.05, 25.13, 24.5, 23.95, 16.0, 14.42, 33.28, 28.3, 26.74, 24.96, 23.78, 20.76, 17.72, 16.01, 31.22, 27.47, 13.28, 10.66, 9.61, 8.36, 6.53, 2.86, 0.96, 34.05, 29.47, 18.87, 15.17, 13.84, 9.61, 4.95, 3.46, 34.09, 23.29, 16.01, 10.35, 7.72, 4.37, 2.97, 27.25, 29.98, 22.56, 9.82, 9.19, 6.57, 5.23, 3.81, 0.96, 30.08, 19.88, 8.44, 4.59, 2.67, 1.53, 0.94, 0.47, 30.73, 20.28, 10.61, 7.48, 6.53, 4.51, 3.04, 1.36, 0.89, 32.21, 23.75, 12.04, 7.67, 5.73, 1.14, 1.02, 0.46, 33.16, 27.33, 15.16, 9.7, 9.47, 8.66, 7.65, 4.84}\n"
        +
        "    time = \n" +
        "      {1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9}\n"
        +
        "    ship =   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\"\n"
        +
        "}\n";
    tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected, "results=\n" + results);

    // .ncHeader
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".ncHeader");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    String2.log(results);

    tResults = results.substring(0, tHeader1.length());
    Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);

    expected = tHeader2 + "}\n";
    tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected, "results=\n" + results);

    // .odvTxt
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        "&latitude>0&time>=2002-08-03",
        dir, globecBottle.className() + "_ODV", ".odvTxt");
    String2.log("ODV fileName=" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    results = results.replaceAll("<CreateTime>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}",
        "<CreateTime>9999-99-99T99:99:99");
    expected = "//<Creator>https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics</Creator>\n" +
        "//<CreateTime>9999-99-99T99:99:99</CreateTime>\n" +
        "//<Encoding>UTF-8</Encoding>\n" +
        "//<Software>ERDDAP - Version " + EDStatic.erddapVersion + "</Software>\n" +
        "//<Source>http://localhost:8080/erddap/tabledap/testGlobecBottle.html</Source>\n" +
        "//<Version>ODV Spreadsheet V4.6</Version>\n" +
        "//<DataField>GeneralField</DataField>\n" +
        "//<DataType>Profiles</DataType>\n" +
        "//<MetaVariable>label=\"Cruise\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" comment=\"Cruise ID\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"Station\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"Type\" value_type=\"TEXT:2\" is_primary_variable=\"F\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"yyyy-mm-ddThh:mm:ss.sss\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Time\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"Longitude [degrees_east]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Longitude\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"Latitude [degrees_north]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Latitude\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"ship\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" comment=\"Ship\" </MetaVariable>\n"
        +
        "//<MetaVariable>label=\"cast\" value_type=\"SHORT\" is_primary_variable=\"F\" comment=\"Cast Number\" </MetaVariable>\n"
        +
        "//<DataVariable>label=\"altitude [m]\" value_type=\"INTEGER\" is_primary_variable=\"F\" comment=\"Altitude\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"bottle_posn\" value_type=\"SIGNED_BYTE\" is_primary_variable=\"F\" comment=\"Bottle Number\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"chl_a_total [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Chlorophyll-a\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"chl_a_10um [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Chlorophyll-a after passing 10um screen\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"phaeo_total [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Total Phaeopigments\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"phaeo_10um [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Phaeopigments 10um\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"sal00 [PSU]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Practical Salinity from T0 and C0 Sensors\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"sal11 [PSU]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Practical Salinity from T1 and C1 Sensors\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"temperature0 [degree_C]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Sea Water Temperature from T0 Sensor\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"temperature1 [degree_C]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Sea Water Temperature from T1 Sensor\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"fluor_v [volts]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Fluorescence Voltage\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"xmiss_v [volts]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Transmissivity Voltage\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"PO4 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Phosphate\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"N_N [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Nitrate plus Nitrite\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"NO3 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Nitrate\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"Si [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Silicate\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"NO2 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Nitrite\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"NH4 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Ammonium\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"oxygen [mL L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Oxygen\" </DataVariable>\n"
        +
        "//<DataVariable>label=\"par [volts]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Photosynthetically Active Radiation\" </DataVariable>\n"
        +
        "Cruise\tStation\tType\tyyyy-mm-ddThh:mm:ss.sss\tLongitude [degrees_east]\tLatitude [degrees_north]\tship\tcast\taltitude [m]\tbottle_posn\tchl_a_total [ug L-1]\tchl_a_10um [ug L-1]\tphaeo_total [ug L-1]\tphaeo_10um [ug L-1]\tsal00 [PSU]\tsal11 [PSU]\ttemperature0 [degree_C]\ttemperature1 [degree_C]\tfluor_v [volts]\txmiss_v [volts]\tPO4 [micromoles L-1]\tN_N [micromoles L-1]\tNO3 [micromoles L-1]\tSi [micromoles L-1]\tNO2 [micromoles L-1]\tNH4 [micromoles L-1]\toxygen [mL L-1]\tpar [volts]\n"
        +
        "nh0207\t\t*\t2002-08-03T01:29:00.000Z\t-124.4\t44.0\tNew_Horizon\t20\t0\t1\t\t\t\t\t33.9939\t33.9908\t7.085\t7.085\t0.256\t0.518\t2.794\t35.8\t35.7\t71.11\t0.093\t0.037\t\t0.1545\n"
        +
        "nh0207\t\t*\t2002-08-03T01:29:00.000Z\t-124.4\t44.0\tNew_Horizon\t20\t0\t2\t\t\t\t\t33.8154\t33.8111\t7.528\t7.53\t0.551\t0.518\t2.726\t35.87\t35.48\t57.59\t0.385\t0.018\t\t0.1767\n"
        +
        "nh0207\t\t*\t2002-08-03T01:29:00.000Z\t-124.4\t44.0\tNew_Horizon\t20\t0\t3\t1.463\t\t1.074\t\t33.5858\t33.5834\t7.572\t7.573\t0.533\t0.518\t2.483\t31.92\t31.61\t48.54\t0.307\t0.504\t\t0.3875\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected,
        "\nresults=\n" + results);

    // .tsv
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".tsv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude\tNO3\ttime\tship\n" +
        "degrees_east\tmicromoles L-1\tUTC\t\n" +
        "-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; // row with missing value has "NaN"
                                                                   // missing value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .tsvp
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".tsvp");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude (degrees_east)\tNO3 (micromoles L-1)\ttime (UTC)\tship\n" +
        "-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; // row with missing value has "NaN"
                                                                   // missing value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .tsv0
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".tsv0");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected = "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; // row with missing value has "NaN"
                                                                   // missing value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .xhtml
    tName = globecBottle.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        globecBottle.className() + "_Data", ".xhtml");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
        "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
        "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
        "  <title>EDDTableFromNcFiles_Data</title>\n" +
        "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/erddap/images/erddap2.css\" />\n"
        +
        "</head>\n" +
        "<body>\n" +
        "\n" +
        "&nbsp;\n" +
        "<table class=\"erd commonBGColor nowrap\">\n" +
        "<tr>\n" +
        "<th>longitude</th>\n" +
        "<th>NO3</th>\n" +
        "<th>time</th>\n" +
        "<th>ship</th>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<th>degrees_east</th>\n" +
        "<th>micromoles L-1</th>\n" +
        "<th>UTC</th>\n" +
        "<th></th>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "<td class=\"R\">-124.4</td>\n" +
        "<td class=\"R\">35.7</td>\n" +
        "<td>2002-08-03T01:29:00Z</td>\n" +
        "<td>New_Horizon</td>\n" +
        "</tr>\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = // row with missing value has "" missing value
        "<tr>\n" +
            "<td class=\"R\">-124.1</td>\n" +
            "<td class=\"R\">24.45</td>\n" +
            "<td>2002-08-19T20:18:00Z</td>\n" +
            "<td>New_Horizon</td>\n" +
            "</tr>\n" +
            "</table>\n" +
            "</body>\n" +
            "</html>\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // data for mapExample
    tName = globecBottle.makeNewFileForDapQuery(language, null, null,
        "longitude,latitude&time>=2002-08-03&time<=2002-08-04",
        dir,
        globecBottle.className() + "Map", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "longitude,latitude\n" +
        "degrees_east,degrees_north\n" +
        "-124.4,44.0\n" +
        "-124.6,44.0\n" +
        "-124.8,44.0\n" +
        "-125.0,44.0\n" +
        "-125.2,44.0\n" +
        "-125.4,44.0\n" +
        "-125.6,43.8\n" +
        "-125.86,43.5\n" +
        "-125.63,43.5\n" +
        "-125.33,43.5\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    try {

      // test treat itself as a dataset
      EDDTable eddTable2 = new EDDTableFromDapSequence(
          "erddapGlobecBottle", // String tDatasetID,
          null, null, null, null, null, null, "", "", null, null,
          new Object[][] { // dataVariables: sourceName, addAttributes
              { "longitude", null, null },
              { "latitude", null, null },
              { "altitude", null, null },
              { "time", null, null },
              { "ship", null, null },
              { "cruise_id", null, null },
              { "cast", null, null },
              { "bottle_posn", null, null },
              { "chl_a_total", null, null },
              { "chl_a_10um", null, null },
              { "phaeo_total", null, null },
              { "phaeo_10um", null, null },
              { "sal00", null, null },
              { "sal11", null, null },
              { "temperature0", null, null },
              { "temperature1", null, null },
              { "fluor_v", null, null },
              { "xmiss_v", null, null },
              { "PO4", null, null },
              { "N_N", null, null },
              { "NO3", null, null },
              { "Si", null, null },
              { "NO2", null, null },
              { "NH4", null, null },
              { "oxygen", null, null },
              { "par", null, null } },
          60, // int tReloadEveryNMinutes,
          EDStatic.erddapUrl + // in tests, always use non-https url
              "/tabledap/testGlobecBottle", // sourceUrl);
          "s", null, // outerSequenceName innerSequenceName
          true, // NeedsExpandedFP_EQ
          true, // sourceCanConstrainStringEQNE
          true, // sourceCanConstrainStringGTLT
          PrimitiveArray.REGEX_OP,
          false);

      // .xhtml from local dataset made from Erddap
      tName = eddTable2.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
          eddTable2.className() + "_Itself", ".xhtml");
      // Test.displayInBrowser("file://" + dir + tName);
      results = File2.directReadFromUtf8File(dir + tName);
      // String2.log(results);
      expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
          "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
          "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" +
          "<head>\n" +
          "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"
          +
          "  <title>EDDTableFromDapSequence_Itself</title>\n" +
          "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/erddap/images/erddap2.css\" />\n"
          +
          "</head>\n" +
          "<body>\n" +
          "\n" +
          "&nbsp;\n" +
          "<table class=\"erd commonBGColor nowrap\">\n" +
          "<tr>\n" +
          "<th>longitude</th>\n" +
          "<th>NO3</th>\n" +
          "<th>time</th>\n" +
          "<th>ship</th>\n" +
          "</tr>\n" +
          "<tr>\n" +
          "<th>degrees_east</th>\n" +
          "<th>micromoles L-1</th>\n" +
          "<th>UTC</th>\n" +
          "<th></th>\n" +
          "</tr>\n" +
          "<tr>\n" +
          "<td class=\"R\">-124.4</td>\n" +
          "<td class=\"R\">35.7</td>\n" +
          "<td>2002-08-03T01:29:00Z</td>\n" +
          "<td>New_Horizon</td>\n" +
          "</tr>\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    } catch (Throwable t) {
      throw new RuntimeException(
          "*** This test requires erddapGlobecBottle in localhost ERDDAP.", t);
    }
    // */

  } // end of testBasic

  @org.junit.jupiter.api.Test
  @TagJetty
  void testNetcdf() throws Throwable {

    // use testGlobecBottle which has fixed altitude=0, not erdGlobecBottle
    int language = 0;
    EDDTable globecBottle = (EDDTableFromNcFiles) EDDTestDataset.gettestGlobecBottle(); // should work
    String tUrl = EDStatic.erddapUrl + // in tests, always use non-https url
        "/tabledap/" + globecBottle.datasetID();
    String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&altitude>-5&time>=2002-08-03";
    String results, expected;

    // TEST READING AS OPENDAP SERVER -- how specify constraint expression?
    // test reading via netcdf-java similar to .dods test
    // tName = globecBottle.makeNewFileForDapQuery(language, null, null,
    // userDapQuery,
    // dir, globecBottle.className() + "_Data", ".dods");
    // Test.displayInBrowser("file://" + dir + tName);
    {
      String2.log("\n*** EDDTableFromNcFiles.testNctcdf do netcdf-java opendap test");
      // !!!THIS READS DATA FROM LOCAL ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!!
      // //in tests, always use non-https url
      // !!!THIS IS NOT JUST A READ-FROM-FILE TEST!!!
      try (NetcdfFile nc = NetcdfDatasets.openFile(tUrl, null)) {
        results = NcHelper.ncdump(nc, "-h");
        // 2016-05-10 lots of little formatting changes
        // including nc.toString now adds cr at end of line (at least on Windows):
        results = String2.replaceAll(results, "\r", "");
        expected = "netcdf testGlobecBottle {\n" +
            "  variables:\n" +
            "\n" + // 2009-02-26 this line was added with switch to netcdf-java 4.0
            "    Structure {\n" +
            "      String cruise_id;\n" +
            "        :cf_role = \"trajectory_id\";\n" +
            "        :ioos_category = \"Identifier\";\n" +
            "        :long_name = \"Cruise ID\";\n" +
            "      String ship;\n" +
            "        :ioos_category = \"Identifier\";\n" +
            "        :long_name = \"Ship\";\n" +
            "      short cast;\n" +
            "        :_FillValue = 32767S; // short\n" +
            "        :actual_range = 1S, 127S; // short\n" +
            "        :colorBarMaximum = 140.0; // double\n" +
            "        :colorBarMinimum = 0.0; // double\n" +
            "        :ioos_category = \"Identifier\";\n" +
            "        :long_name = \"Cast Number\";\n" +
            "        :missing_value = 32767S; // short\n" +
            "      float longitude;\n" +
            "        :_CoordinateAxisType = \"Lon\";\n" +
            "        :_FillValue = 327.67f; // float\n" +
            "        :actual_range = -126.2f, -124.1f; // float\n" +
            "        :axis = \"X\";\n" +
            "        :ioos_category = \"Location\";\n" +
            "        :long_name = \"Longitude\";\n" +
            "        :missing_value = 327.67f; // float\n" +
            "        :standard_name = \"longitude\";\n" +
            "        :units = \"degrees_east\";\n" +
            "      float latitude;\n" +
            "        :_CoordinateAxisType = \"Lat\";\n" +
            "        :_FillValue = 327.67f; // float\n" +
            "        :actual_range = 41.9f, 44.65f; // float\n" +
            "        :axis = \"Y\";\n" +
            "        :ioos_category = \"Location\";\n" +
            "        :long_name = \"Latitude\";\n" +
            "        :missing_value = 327.67f; // float\n" +
            "        :standard_name = \"latitude\";\n" +
            "        :units = \"degrees_north\";\n" +
            "      int altitude;\n" +
            "        :_CoordinateAxisType = \"Height\";\n" +
            "        :_CoordinateZisPositive = \"up\";\n" +
            "        :actual_range = 0, 0; // int\n" +
            "        :axis = \"Z\";\n" +
            "        :ioos_category = \"Location\";\n" +
            "        :long_name = \"Altitude\";\n" +
            "        :positive = \"up\";\n" +
            "        :standard_name = \"altitude\";\n" +
            "        :units = \"m\";\n" +
            "      double time;\n" +
            "        :_CoordinateAxisType = \"Time\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected,
            "RESULTS=\n" + results);

        expected = "  :time_coverage_start = \"2002-05-30T03:21:00Z\";\n" +
            "  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
            "  :Westernmost_Easting = -126.2; // double\n" +
            "}\n";
        Test.ensureEqual(results.substring(results.indexOf("  :time_coverage_start")), expected,
            "RESULTS=\n" + results);

        Attributes attributes = new Attributes();
        NcHelper.getGroupAttributes(nc.getRootGroup(), attributes);
        Test.ensureEqual(attributes.getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)",
            "");

        // get attributes for a dimension
        Variable ncLat = nc.findVariable("s.latitude");
        attributes.clear();
        NcHelper.getVariableAttributes(ncLat, attributes);
        Test.ensureEqual(attributes.getString("units"), EDV.LAT_UNITS, "");

        // get attributes for grid variable
        Variable ncChl = nc.findVariable("s.chl_a_total");
        attributes.clear();
        NcHelper.getVariableAttributes(ncChl, attributes);
        Test.ensureEqual(attributes.getString("standard_name"),
            "concentration_of_chlorophyll_in_sea_water",
            "");

        // get sequence data
        // it's awkward. Do later if needed.
        // ???How specify constraint expression?
      }
    }

    // OTHER APPROACH: GET .NC FILE -- HOW SPECIFY CONSTRAINT EXPRESSION???
    // Test.displayInBrowser("file://" + dir + tName);
    if (false) {
      try {
        String2.log("\n*** do netcdf-java .nc test");
        // !!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON COASTWATCH CWEXPERIMENTAL!!!
        // !!!THIS IS NOT JUST A LOCAL TEST!!!
        try (NetcdfFile nc = NetcdfDatasets.openFile(tUrl + ".nc?" + mapDapQuery, null)) {
          results = nc.toString();
          expected = "zz";
          Test.ensureEqual(results.substring(0, expected.length()), expected,
              "RESULTS=\n" + results);
        }

      } catch (Throwable t) {
        throw new Exception(MustBe.throwableToString(t) +
            "\nError accessing " + EDStatic.erddapUrl + // in tests, always use
                                                        // non-https url
            " via netcdf-java.");
      }
    }
  }

  /* EDDTableFromNccsvFiles */

  /**
   * This tests actual_range in .nccsvMetadata and .nccsv responses.
   * This requires pmelTaoDySst and rPmelTaoDySst in localhost erddap.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testActualRange() throws Throwable {
    // String2.log("\n****************** EDDTableFromNccsv.testActualRange\n" +
    // "!!!This test requires pmelTaoDySst and rlPmelTaoDySst in localhost
    // ERDDAP.\n");
    // testVerboseOn();
    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String baseUrl = "http://localhost:" + PORT + "/erddap/tabledap/pmelTaoDySst";
    String rbaseUrl = "http://localhost:" + PORT + "/erddap/tabledap/rlPmelTaoDySst";

    // *** test getting .nccsvMetadata for entire dataset
    tQuery = ".nccsvMetadata";
    expected = "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n" +
        "*GLOBAL*,cdm_data_type,TimeSeries\n" +
        "*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n"
        +
        "*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,creator_type,group\n" +
        "*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n" +
        "*GLOBAL*,Easternmost_Easting,357.0d\n" +
        "*GLOBAL*,featureType,TimeSeries\n" +
        "*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,geospatial_lat_max,21.0d\n" +
        "*GLOBAL*,geospatial_lat_min,-25.0d\n" +
        "*GLOBAL*,geospatial_lat_units,degrees_north\n" +
        "*GLOBAL*,geospatial_lon_max,357.0d\n" +
        "*GLOBAL*,geospatial_lon_min,0.0d\n" +
        "*GLOBAL*,geospatial_lon_units,degrees_east\n" +
        "*GLOBAL*,geospatial_vertical_max,15.0d\n" +
        "*GLOBAL*,geospatial_vertical_min,1.0d\n" +
        "*GLOBAL*,geospatial_vertical_positive,down\n" +
        "*GLOBAL*,geospatial_vertical_units,m\n" +
        "*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n"
        +
        "dddd-dd-dd Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.\"\n"
        +
        "*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n"
        +
        "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
        "*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n"
        +
        "*GLOBAL*,Northernmost_Northing,21.0d\n" +
        "*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n"
        +
        "*GLOBAL*,sourceUrl,(local files)\n" +
        "*GLOBAL*,Southernmost_Northing,-25.0d\n" +
        "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
        "*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n" +
        "*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n"
        +
        "*GLOBAL*,testOutOfDate,now-3days\n" +
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n" +
        "*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n" +
        "*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n"
        +
        "*GLOBAL*,Westernmost_Easting,0.0d\n" +
        "array,*DATA_TYPE*,String\n" +
        "array,ioos_category,Identifier\n" +
        "array,long_name,Array\n" +
        "station,*DATA_TYPE*,String\n" +
        "station,cf_role,timeseries_id\n" +
        "station,ioos_category,Identifier\n" +
        "station,long_name,Station\n" +
        "wmo_platform_code,*DATA_TYPE*,int\n" +
        "wmo_platform_code,actual_range,13001i,CHANGESi\n" +
        "wmo_platform_code,ioos_category,Identifier\n" +
        "wmo_platform_code,long_name,WMO Platform Code\n" +
        "wmo_platform_code,missing_value,2147483647i\n" +
        "longitude,*DATA_TYPE*,float\n" +
        "longitude,_CoordinateAxisType,Lon\n" +
        "longitude,actual_range,0.0f,357.0f\n" +
        "longitude,axis,X\n" +
        "longitude,epic_code,502i\n" +
        "longitude,ioos_category,Location\n" +
        "longitude,long_name,Nominal Longitude\n" +
        "longitude,missing_value,1.0E35f\n" +
        "longitude,standard_name,longitude\n" +
        "longitude,type,EVEN\n" +
        "longitude,units,degrees_east\n" +
        "latitude,*DATA_TYPE*,float\n" +
        "latitude,_CoordinateAxisType,Lat\n" +
        "latitude,actual_range,-25.0f,21.0f\n" +
        "latitude,axis,Y\n" +
        "latitude,epic_code,500i\n" +
        "latitude,ioos_category,Location\n" +
        "latitude,long_name,Nominal Latitude\n" +
        "latitude,missing_value,1.0E35f\n" +
        "latitude,standard_name,latitude\n" +
        "latitude,type,EVEN\n" +
        "latitude,units,degrees_north\n" +
        "time,*DATA_TYPE*,String\n" +
        "time,_CoordinateAxisType,Time\n" +
        "time,actual_range,1977-11-03T12:00:00Z\\ndddd-dd-ddT12:00:00Z\n" + // stop time changes
        "time,axis,T\n" +
        "time,ioos_category,Time\n" +
        "time,long_name,Centered Time\n" +
        "time,point_spacing,even\n" +
        "time,standard_name,time\n" +
        "time,time_origin,01-JAN-1970 00:00:00\n" +
        "time,type,EVEN\n" +
        "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
        "depth,*DATA_TYPE*,float\n" +
        "depth,_CoordinateAxisType,Height\n" +
        "depth,_CoordinateZisPositive,down\n" +
        "depth,actual_range,1.0f,15.0f\n" +
        "depth,axis,Z\n" +
        "depth,epic_code,3i\n" +
        "depth,ioos_category,Location\n" +
        "depth,long_name,Depth\n" +
        "depth,missing_value,1.0E35f\n" +
        "depth,positive,down\n" +
        "depth,standard_name,depth\n" +
        "depth,type,EVEN\n" +
        "depth,units,m\n" +
        "T_25,*DATA_TYPE*,float\n" +
        "T_25,_FillValue,1.0E35f\n" +
        "T_25,actual_range,17.12f,35.4621f\n" +
        "T_25,colorBarMaximum,32.0d\n" +
        "T_25,colorBarMinimum,0.0d\n" +
        "T_25,epic_code,25i\n" +
        "T_25,generic_name,temp\n" +
        "T_25,ioos_category,Temperature\n" +
        "T_25,long_name,Sea Surface Temperature\n" +
        "T_25,missing_value,1.0E35f\n" +
        "T_25,name,T\n" +
        "T_25,standard_name,sea_surface_temperature\n" +
        "T_25,units,degree_C\n" +
        "QT_5025,*DATA_TYPE*,float\n" +
        "QT_5025,_FillValue,1.0E35f\n" +
        "QT_5025,actual_range,0.0f,5.0f\n" +
        "QT_5025,colorBarContinuous,false\n" +
        "QT_5025,colorBarMaximum,6.0d\n" +
        "QT_5025,colorBarMinimum,0.0d\n" +
        "QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n"
        +
        "QT_5025,epic_code,5025i\n" +
        "QT_5025,generic_name,qt\n" +
        "QT_5025,ioos_category,Quality\n" +
        "QT_5025,long_name,Sea Surface Temperature Quality\n" +
        "QT_5025,missing_value,1.0E35f\n" +
        "QT_5025,name,QT\n" +
        "ST_6025,*DATA_TYPE*,float\n" +
        "ST_6025,_FillValue,1.0E35f\n" +
        "ST_6025,actual_range,0.0f,5.0f\n" +
        "ST_6025,colorBarContinuous,false\n" +
        "ST_6025,colorBarMaximum,8.0d\n" +
        "ST_6025,colorBarMinimum,0.0d\n" +
        "ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n"
        +
        "ST_6025,epic_code,6025i\n" +
        "ST_6025,generic_name,st\n" +
        "ST_6025,ioos_category,Other\n" +
        "ST_6025,long_name,Sea Surface Temperature Source\n" +
        "ST_6025,missing_value,1.0E35f\n" +
        "ST_6025,name,ST\n" +
        "\n" +
        "*END_METADATA*\n";

    // note that there is actual_range info
    results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "\\*GLOBAL\\*,time_coverage_end,....-..-..T12:00:00Z\n",
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results = results.replaceAll(
        "time,actual_range,1977-11-03T12:00:00Z\\\\n....-..-..T12:00:00Z\n",
        "time,actual_range,1977-11-03T12:00:00Z\\\\ndddd-dd-ddT12:00:00Z\n");
    results = results.replaceAll("wmo_platform_code,actual_range,13001i,[0-9]+i",
        "wmo_platform_code,actual_range,13001i,CHANGESi");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "\\*GLOBAL\\*,time_coverage_end,....-..-..T12:00:00Z\n",
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results = results.replaceAll(
        "time,actual_range,1977-11-03T12:00:00Z\\\\n....-..-..T12:00:00Z\n",
        "time,actual_range,1977-11-03T12:00:00Z\\\\ndddd-dd-ddT12:00:00Z\n");
    results = results.replaceAll("wmo_platform_code,actual_range,13001i,[0-9]+i",
        "wmo_platform_code,actual_range,13001i,CHANGESi");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test getting .nccsv
    tQuery = ".nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05";
    expected = "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n" +
        "*GLOBAL*,cdm_data_type,TimeSeries\n" +
        "*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n"
        +
        "*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,creator_type,group\n" +
        "*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n" +
        "*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n" +
        "*GLOBAL*,Easternmost_Easting,357.0d\n" +
        "*GLOBAL*,featureType,TimeSeries\n" +
        "*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n" +
        "*GLOBAL*,geospatial_lat_max,21.0d\n" +
        "*GLOBAL*,geospatial_lat_min,-25.0d\n" +
        "*GLOBAL*,geospatial_lat_units,degrees_north\n" +
        "*GLOBAL*,geospatial_lon_max,357.0d\n" +
        "*GLOBAL*,geospatial_lon_min,0.0d\n" +
        "*GLOBAL*,geospatial_lon_units,degrees_east\n" +
        "*GLOBAL*,geospatial_vertical_max,15.0d\n" +
        "*GLOBAL*,geospatial_vertical_min,1.0d\n" +
        "*GLOBAL*,geospatial_vertical_positive,down\n" +
        "*GLOBAL*,geospatial_vertical_units,m\n" +
        "*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n";
    // +
    // "dddd-dd-dd Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully
    // refreshed ERD's copy of this dataset by downloading all of the .cdf files
    // from the PMEL TAO FTP site. Since then, the dataset has been partially
    // refreshed everyday by downloading and merging the latest version of the last
    // 25 days worth of data.\\n";
    // "2017-05-26T18:30:46Z (local files)\\n" +
    // "2017-05-26T18:30:46Z
    expected2 = "http://localhost:" + PORT
        + "/erddap/tabledap/pmelTaoDySst.nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05\"\n"
        +
        "*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n" +
        "*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n"
        +
        "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n" +
        "*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n"
        +
        "*GLOBAL*,Northernmost_Northing,21.0d\n" +
        "*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n" +
        "*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n"
        +
        "*GLOBAL*,sourceUrl,(local files)\n" +
        "*GLOBAL*,Southernmost_Northing,-25.0d\n" +
        "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n" +
        "*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n" +
        "*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n"
        +
        "*GLOBAL*,testOutOfDate,now-3days\n" +
        "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n" +
        "*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n" +
        "*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n"
        +
        "*GLOBAL*,Westernmost_Easting,0.0d\n" +
        "array,*DATA_TYPE*,String\n" +
        "array,ioos_category,Identifier\n" +
        "array,long_name,Array\n" +
        "station,*DATA_TYPE*,String\n" +
        "station,cf_role,timeseries_id\n" +
        "station,ioos_category,Identifier\n" +
        "station,long_name,Station\n" +
        "wmo_platform_code,*DATA_TYPE*,int\n" +
        "wmo_platform_code,ioos_category,Identifier\n" +
        "wmo_platform_code,long_name,WMO Platform Code\n" +
        "wmo_platform_code,missing_value,2147483647i\n" +
        "longitude,*DATA_TYPE*,float\n" +
        "longitude,_CoordinateAxisType,Lon\n" +
        "longitude,axis,X\n" +
        "longitude,epic_code,502i\n" +
        "longitude,ioos_category,Location\n" +
        "longitude,long_name,Nominal Longitude\n" +
        "longitude,missing_value,1.0E35f\n" +
        "longitude,standard_name,longitude\n" +
        "longitude,type,EVEN\n" +
        "longitude,units,degrees_east\n" +
        "latitude,*DATA_TYPE*,float\n" +
        "latitude,_CoordinateAxisType,Lat\n" +
        "latitude,axis,Y\n" +
        "latitude,epic_code,500i\n" +
        "latitude,ioos_category,Location\n" +
        "latitude,long_name,Nominal Latitude\n" +
        "latitude,missing_value,1.0E35f\n" +
        "latitude,standard_name,latitude\n" +
        "latitude,type,EVEN\n" +
        "latitude,units,degrees_north\n" +
        "time,*DATA_TYPE*,String\n" +
        "time,_CoordinateAxisType,Time\n" +
        "time,axis,T\n" +
        "time,ioos_category,Time\n" +
        "time,long_name,Centered Time\n" +
        "time,point_spacing,even\n" +
        "time,standard_name,time\n" +
        "time,time_origin,01-JAN-1970 00:00:00\n" +
        "time,type,EVEN\n" +
        "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n" +
        "depth,*DATA_TYPE*,float\n" +
        "depth,_CoordinateAxisType,Height\n" +
        "depth,_CoordinateZisPositive,down\n" +
        "depth,axis,Z\n" +
        "depth,epic_code,3i\n" +
        "depth,ioos_category,Location\n" +
        "depth,long_name,Depth\n" +
        "depth,missing_value,1.0E35f\n" +
        "depth,positive,down\n" +
        "depth,standard_name,depth\n" +
        "depth,type,EVEN\n" +
        "depth,units,m\n" +
        "T_25,*DATA_TYPE*,float\n" +
        "T_25,_FillValue,1.0E35f\n" +
        "T_25,colorBarMaximum,32.0d\n" +
        "T_25,colorBarMinimum,0.0d\n" +
        "T_25,epic_code,25i\n" +
        "T_25,generic_name,temp\n" +
        "T_25,ioos_category,Temperature\n" +
        "T_25,long_name,Sea Surface Temperature\n" +
        "T_25,missing_value,1.0E35f\n" +
        "T_25,name,T\n" +
        "T_25,standard_name,sea_surface_temperature\n" +
        "T_25,units,degree_C\n" +
        "QT_5025,*DATA_TYPE*,float\n" +
        "QT_5025,_FillValue,1.0E35f\n" +
        "QT_5025,colorBarContinuous,false\n" +
        "QT_5025,colorBarMaximum,6.0d\n" +
        "QT_5025,colorBarMinimum,0.0d\n" +
        "QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n"
        +
        "QT_5025,epic_code,5025i\n" +
        "QT_5025,generic_name,qt\n" +
        "QT_5025,ioos_category,Quality\n" +
        "QT_5025,long_name,Sea Surface Temperature Quality\n" +
        "QT_5025,missing_value,1.0E35f\n" +
        "QT_5025,name,QT\n" +
        "ST_6025,*DATA_TYPE*,float\n" +
        "ST_6025,_FillValue,1.0E35f\n" +
        "ST_6025,colorBarContinuous,false\n" +
        "ST_6025,colorBarMaximum,8.0d\n" +
        "ST_6025,colorBarMinimum,0.0d\n" +
        "ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n"
        +
        "ST_6025,epic_code,6025i\n" +
        "ST_6025,generic_name,st\n" +
        "ST_6025,ioos_category,Other\n" +
        "ST_6025,long_name,Sea Surface Temperature Source\n" +
        "ST_6025,missing_value,1.0E35f\n" +
        "ST_6025,name,ST\n" +
        "\n" +
        "*END_METADATA*\n" +
        "array,station,wmo_platform_code,longitude,latitude,time,depth,T_25,QT_5025,ST_6025\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-01T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-02T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-03T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-04T12:00:00Z,1.0,1.0E35,0.0,0.0\n" +
        "*END_DATA*\n";

    // note no actual_range info
    results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "time_coverage_end,....-..-..T12:00:00Z\n",
        "time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf(expected2.substring(0, 100));
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery); // difference: 'r'baseUrl
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results = results.replaceAll(
        "time_coverage_end,....-..-..T12:00:00Z\n",
        "time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf(expected2.substring(0, 100));
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);
  }

  /** EDDGridFromDap */

  /**
   * This tests a depth axis variable. This requires
   * hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagImageComparison
  void testGridWithDepth2_LonPM180() throws Throwable {
    String2.log("\n*** EDDGridFromDap.testGridWithDepth2_LonPM180");
    String results, expected, tName;
    int po;
    int language = 0;

    // test generateDatasetsXml -- It should catch z variable and convert to
    // altitude.
    // !!! I don't have a test dataset with real altitude data that isn't already
    // called altitude!
    /*
     * String url =
     * "http://www.marine.csiro.au/dods/nph-dods/dods-data/bl/BRAN2.1/bodas/19921014.bodas_ts.nc";
     * results = generateDatasetsXml(true, url,
     * null, null, null, 10080, null);
     * po = results.indexOf("<sourceName>z</sourceName>");
     * Test.ensureTrue(po >= 0, "results=\n" + results);
     * expected =
     * "<sourceName>z</sourceName>\n" +
     * "        <destinationName>depth</destinationName>\n" +
     * "        <!-- sourceAttributes>\n" +
     * "            <att name=\"cartesian_axis\">Z</att>\n" +
     * "            <att name=\"long_name\">Depth</att>\n" +
     * "            <att name=\"positive\">down</att>\n" +
     * "            <att name=\"units\">m</att>\n" +
     * "        </sourceAttributes -->\n" +
     * "        <addAttributes>\n" +
     * "            <att name=\"ioos_category\">Location</att>\n" +
     * "            <att name=\"standard_name\">depth</att>\n" +
     * "        </addAttributes>\n" +
     * "    </axisVariable>";
     * Test.ensureEqual(results.substring(po, po + expected.length()), expected,
     * "results=\n" + results);
     */

    // Test that constructor of EDVDepthGridAxis added proper metadata for depth
    // variable.
    // EDDGrid gridDataset = (EDDGrid) EDDGridFromDap.oneFromDatasetsXml(null,
    // "hawaii_d90f_20ee_c4cb_LonPM180");
    EDDGrid gridDataset = (EDDGrid) EDDTestDataset.gethawaii_d90f_20ee_c4cb_LonPM180();

    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
        EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth2", ".das");
    results = File2.directReadFrom88591File(
        EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("depth {");
    Test.ensureTrue(po >= 0, "results=\n" + results);
    expected = "depth {\n" +
        "    String _CoordinateAxisType \"Height\";\n" +
        "    String _CoordinateZisPositive \"down\";\n" +
        "    Float64 actual_range 5.01, 5375.0;\n" + // 2014-01-17 was 5.0, 5374.0
        "    String axis \"Z\";\n" +
        "    String ioos_category \"Location\";\n" +
        "    String long_name \"Depth\";\n" +
        "    String positive \"down\";\n" +
        "    String standard_name \"depth\";\n" +
        "    String units \"m\";\n" +
        "  }";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // FGDC should deal with depth correctly
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
        EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth2", ".fgdc");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("<vertdef>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<vertdef>\n" +
        "      <depthsys>\n" +
        "        <depthdn>Unknown</depthdn>\n" +
        "        <depthres>Unknown</depthres>\n" +
        "        <depthdu>meters</depthdu>\n" +
        "        <depthem>Explicit depth coordinate included with horizontal coordinates</depthem>\n"
        +
        "      </depthsys>\n" +
        "    </vertdef>\n" +
        "  </spref>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // ISO 19115 should deal with depth correctly
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "",
        EDStatic.fullTestCacheDirectory, "EDDGridLonPM180_testGridWithDepth2", ".iso19115");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);

    po = results.indexOf(
        "codeListValue=\"vertical\">");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n" +
        "          </gmd:dimensionName>\n" +
        "          <gmd:dimensionSize>\n" +
        "            <gco:Integer>40</gco:Integer>\n" +
        "          </gmd:dimensionSize>\n" +
        "          <gmd:resolution>\n" +
        "            <gco:Measure uom=\"m\">137.69205128205127</gco:Measure>\n" + // 2014-01-17
                                                                                  // was
                                                                                  // 137.66666666666666
        "          </gmd:resolution>\n" +
        "        </gmd:MD_Dimension>\n" +
        "      </gmd:axisDimensionProperties>\n";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    po = results.indexOf(
        "<gmd:EX_VerticalExtent>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<gmd:EX_VerticalExtent>\n" +
        "              <gmd:minimumValue><gco:Real>-5375.0</gco:Real></gmd:minimumValue>\n" +
        "              <gmd:maximumValue><gco:Real>-5.01</gco:Real></gmd:maximumValue>\n" +
        "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n" +
        "            </gmd:EX_VerticalExtent>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // test WMS 1.1.0 service getCapabilities from localhost erddap
    String2.log("\nTest WMS 1.1.0 getCapabilities\n" +
        "!!! This test requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP!!!");
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "service=WMS&request=GetCapabilities&version=1.1.0");
    po = results.indexOf("</Layer>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "</Layer>\n" +
        "      <Layer>\n" +
        "        <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180</Title>\n"
        +
        "        <SRS>EPSG:4326</SRS>\n" +
        "        <LatLonBoundingBox minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" />\n"
        +
        "        <BoundingBox SRS=\"EPSG:4326\" minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" resx=\"0.5\" resy=\"0.5\" />\n"
        +
        "        <Dimension name=\"time\" units=\"ISO8601\" />\n" +
        "        <Dimension name=\"elevation\" units=\"EPSG:5030\" />\n" +
        // 2014-01-24 default was 2008-12-15
        "        <Extent name=\"time\" default=\"2010-12-15T00:00:00Z\" >1871-01-15T00:00:00Z,1871-02-15T00:00:00Z,1871-03-15T00:00:00Z,";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    po = results.indexOf("<Extent name=\"elevation\"");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<Extent name=\"elevation\" default=\"-5375.0\" >-5.01,-15.07,-25.28,-35.76,-46.61,-57.98,-70.02,-82.92,-96.92,-112.32,-129.49,-148.96,-171.4,-197.79,-229.48,-268.46,-317.65,-381.39,-465.91,-579.31,-729.35,-918.37,-1139.15,-1378.57,-1625.7,-1875.11,-2125.01,-2375.0,-2625.0,-2875.0,-3125.0,-3375.0,-3625.0,-3875.0,-4125.0,-4375.0,-4625.0,-4875.0,-5125.0,-5375.0</Extent>\n"
        +
        "        <Attribution>\n" +
        "          <Title>TAMU/UMD</Title>\n" +
        "          <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n" +
        "            xlink:type=\"simple\"\n" +
        "            xlink:href=\"https://www.atmos.umd.edu/~ocean/\" />\n" +
        "        </Attribution>\n" +
        "        <Layer opaque=\"1\" >\n" +
        "          <Name>hawaii_d90f_20ee_c4cb_LonPM180:temp</Name>\n" +
        "          <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180 - temp</Title>\n"
        +
        "        </Layer>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // WMS 1.1.0 elevation=-5
    String baseName = "EDDGridLonPM180_TestGridWithDepth2110e5";
    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // WMS 1.1.0 default elevation
    baseName = "EDDGridLonPM180_TestGridWithDepth2110edef";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080" +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // test WMS 1.3.0 service getCapabilities from localhost erddap
    String2.log("\nTest WMS 1.3.0 getCapabilities\n" +
        "!!! This test requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP!!!");
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "service=WMS&request=GetCapabilities&version=1.3.0");

    po = results.indexOf("</Layer>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "</Layer>\n" +
        "      <Layer>\n" +
        "        <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180</Title>\n"
        +
        "        <CRS>CRS:84</CRS>\n" +
        "        <CRS>EPSG:4326</CRS>\n" +
        "        <EX_GeographicBoundingBox>\n" +
        "          <westBoundLongitude>-179.75</westBoundLongitude>\n" +
        "          <eastBoundLongitude>179.75</eastBoundLongitude>\n" +
        "          <southBoundLatitude>-75.25</southBoundLatitude>\n" +
        "          <northBoundLatitude>89.25</northBoundLatitude>\n" +
        "        </EX_GeographicBoundingBox>\n" +
        "        <BoundingBox CRS=\"EPSG:4326\" minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" resx=\"0.5\" resy=\"0.5\" />\n"
        +
        "        <Dimension name=\"time\" units=\"ISO8601\" multipleValues=\"0\" nearestValue=\"1\" default=\"2010-12-15T00:00:00Z\" >1871-01-15T00:00:00Z,1871-02-15T00:00:00Z,";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    po = results.indexOf("<Dimension name=\"elevation\"");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected = "<Dimension name=\"elevation\" units=\"CRS:88\" unitSymbol=\"m\" multipleValues=\"0\" nearestValue=\"1\" default=\"-5375.0\" >-5.01,-15.07,-25.28,-35.76,-46.61,-57.98,-70.02,-82.92,-96.92,-112.32,-129.49,-148.96,-171.4,-197.79,-229.48,-268.46,-317.65,-381.39,-465.91,-579.31,-729.35,-918.37,-1139.15,-1378.57,-1625.7,-1875.11,-2125.01,-2375.0,-2625.0,-2875.0,-3125.0,-3375.0,-3625.0,-3875.0,-4125.0,-4375.0,-4625.0,-4875.0,-5125.0,-5375.0</Dimension>";
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // WMS 1.3.0 elevation=-5
    // 2022-07-07 trouble with wms png request, so test underlying data request
    // first
    tName = gridDataset.makeNewFileForDapQuery(language, null, null,
        "temp%5B(2008-11-15)%5D%5B(5)%5D%5B(-75):100:(75)%5D%5B(-90):100:(63.6)%5D",
        EDStatic.fullTestCacheDirectory,
        "EDDGridLonPM180_testGridWithDepthPreWMS", ".csv");
    results = File2.directReadFrom88591File(
        EDStatic.fullTestCacheDirectory + tName);
    expected = "time,depth,latitude,longitude,temp\n" +
        "UTC,m,degrees_north,degrees_east,degree_C\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,-89.75,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,-39.75,-1.9969722\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,10.25,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,-74.75,60.25,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,-89.75,19.052225\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,-39.75,22.358824\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,10.25,17.43544\n" +
        "2008-11-15T00:00:00Z,5.01,-24.75,60.25,23.83485\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,-89.75,26.235065\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,-39.75,25.840372\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,10.25,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,25.25,60.25,27.425127\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,-89.75,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,-39.75,NaN\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,10.25,4.0587144\n" +
        "2008-11-15T00:00:00Z,5.01,75.25,60.25,-0.4989917\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // (see section above) now request troubling wms png
    baseName = "EDDGridLonPM180_TestGridWithDepth2130e5";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-75,-90,75,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // WMS 1.1.0 default elevation
    baseName = "EDDGridLonPM180_TestGridWithDepth2130edef";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080" +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-75,-90,75,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // test lat beyond dataset range (changed from -75:75 above to -80:80 here)
    baseName = "EDDGridLonPM180_BeyondRange";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:" + PORT + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?" +
            "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            +
            "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            +
            "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES=" +
            "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName, false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");
  }

  /** This tests saveAsKml. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testKml() throws Throwable {
    // testVerboseOn();
    int language = 0;

    EDDGridFromDap gridDataset = (EDDGridFromDap) EDDTestDataset.gettestActualRange();
    String name, tName, results, expected;
    String dir = EDStatic.fullTestCacheDirectory;

    // overall kml
    tName = gridDataset.makeNewFileForDapQuery(language, null, null,
        "SST[800][][]",
        dir, gridDataset.className() + "_testKml", ".kml");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" + //
            "<Document>\n" + //
            "  <name>NOAA Coral Reef Watch 25km Ocean Acidification, Caribbean, Preliminary, 0.25°, 2016-present</name>\n" + //
            "  <description><![CDATA[Time: 2018-03-13T00:00:00Z<br />\n" + //
            "Data courtesy of USDOC/NOAA Coral Reef Watch<br />\n" + //
            "<a href=\"http://localhost:8080/erddap/griddap/testActualRange.html?SST\">Download data from this dataset.</a><br />\n" + //
            "    ]]></description>\n" + //
            "  <Region>\n" + //
            "    <Lod><minLodPixels>2</minLodPixels></Lod>\n" + //
            "    <LatLonAltBox>\n" + //
            "      <west>-90.125</west>\n" + //
            "      <east>-59.875</east>\n" + //
            "      <south>14.875</south>\n" + //
            "      <north>30.125</north>\n" + //
            "    </LatLonAltBox>\n" + //
            "  </Region>\n" + //
            "  <GroundOverlay>\n" + //
            "    <drawOrder>1</drawOrder>\n" + //
            "    <Icon>\n" + //
            "      <href>http://localhost:8080/erddap/griddap/testActualRange.transparentPng?SST%5B(2018-03-13T00%3A00%3A00Z)%5D%5B(14.875)%3A1%3A(30.125)%5D%5B(-90.125)%3A1%3A(-59.875)%5D</href>\n" + //
            "    </Icon>\n" + //
            "    <LatLonBox>\n" + //
            "      <west>-90.125</west>\n" + //
            "      <east>-59.875</east>\n" + //
            "      <south>14.875</south>\n" + //
            "      <north>30.125</north>\n" + //
            "    </LatLonBox>\n" + //
            "  </GroundOverlay>\n" + //
            "  <ScreenOverlay id=\"Logo\">\n" + //
            "    <description>http://localhost:8080/erddap</description>\n" + //
            "    <name>Logo</name>\n" + //
            "    <Icon><href>http://localhost:8080/erddap/images/nlogo.gif</href></Icon>\n" + //
            "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" + //
            "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" + //
            "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" + //
            "  </ScreenOverlay>\n" + //
            "</Document>\n" + //
            "</kml>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // a quadrant
    tName = gridDataset.makeNewFileForDapQuery(language, null, null,
        "SST[1500][(15.0):(30.0)][(-75.0):(-60.0)]",
        dir, gridDataset.className() + "_testKml2", ".kml");
    results = File2.directReadFromUtf8File(dir + tName);
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" + //
            "<Document>\n" + //
            "  <name>NOAA Coral Reef Watch 25km Ocean Acidification, Caribbean, Preliminary, 0.25°, 2016-present</name>\n" + //
            "  <description><![CDATA[Time: 2020-04-23T00:00:00Z<br />\n" + //
            "Data courtesy of USDOC/NOAA Coral Reef Watch<br />\n" + //
            "<a href=\"http://localhost:8080/erddap/griddap/testActualRange.html?SST\">Download data from this dataset.</a><br />\n" + //
            "    ]]></description>\n" + //
            "  <Region>\n" + //
            "    <Lod><minLodPixels>2</minLodPixels></Lod>\n" + //
            "    <LatLonAltBox>\n" + //
            "      <west>-74.875</west>\n" + //
            "      <east>-59.875</east>\n" + //
            "      <south>15.125</south>\n" + //
            "      <north>30.125</north>\n" + //
            "    </LatLonAltBox>\n" + //
            "  </Region>\n" + //
            "  <GroundOverlay>\n" + //
            "    <drawOrder>1</drawOrder>\n" + //
            "    <Icon>\n" + //
            "      <href>http://localhost:8080/erddap/griddap/testActualRange.transparentPng?SST%5B(2020-04-23T00%3A00%3A00Z)%5D%5B(15.125)%3A1%3A(30.125)%5D%5B(-74.875)%3A1%3A(-59.875)%5D</href>\n" + //
            "    </Icon>\n" + //
            "    <LatLonBox>\n" + //
            "      <west>-74.875</west>\n" + //
            "      <east>-59.875</east>\n" + //
            "      <south>15.125</south>\n" + //
            "      <north>30.125</north>\n" + //
            "    </LatLonBox>\n" + //
            "  </GroundOverlay>\n" + //
            "  <ScreenOverlay id=\"Logo\">\n" + //
            "    <description>http://localhost:8080/erddap</description>\n" + //
            "    <name>Logo</name>\n" + //
            "    <Icon><href>http://localhost:8080/erddap/images/nlogo.gif</href></Icon>\n" + //
            "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" + //
            "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n" + //
            "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n" + //
            "  </ScreenOverlay>\n" + //
            "</Document>\n" + //
            "</kml>\n";;
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testOpendap() throws Throwable {
    String2.log("\n****************** EDDGridFromDap test opendap\n" +
        "!!!THIS READS DATA FROM SERVER RUNNING ON COASTWATCH: erdMHchla8day on " +
        EDStatic.erddapUrl + "!!!"); // in tests, always non-https url
    // testVerboseOn();
    String results, expected, tName;
    int tPo;
    String userDapQuery = "chlorophyll[(2007-02-06)][][(29):10:(50)][(225):10:(247)]";
    String graphDapQuery = "chlorophyll[0:10:200][][(29)][(225)]";
    String mapDapQuery = "chlorophyll[200][][(29):(50)][(225):(247)]"; // stride irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    int language = 0;

    // get das and dds
    String threddsUrl = "http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4";
    String erddapUrl = EDStatic.erddapUrl + "/griddap/hawaii_d90f_20ee_c4cb"; // in tests, always non-https url
    DConnect threddsConnect = new DConnect(threddsUrl, true, 1, 1);
    DConnect erddapConnect = new DConnect(erddapUrl, true, 1, 1); // in tests, always non-https url
    DAS das = erddapConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    DDS dds = erddapConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    PrimitiveArray tpas[], epas[];

    // get global attributes
    Attributes attributes = new Attributes();
    OpendapHelper.getAttributes(das, "GLOBAL", attributes);
    Test.ensureEqual(attributes.getString("contributor_name"), null, "");
    Test.ensureEqual(attributes.getString("keywords"),
          "circulation, currents, density, depths, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water",
        "");

    // get attributes for a dimension
    attributes.clear();
    OpendapHelper.getAttributes(das, "latitude", attributes);
    Test.ensureEqual(attributes.getString("coordsys"), null, "");
    Test.ensureEqual(attributes.get("fraction_digits"), null, ""); // test if
                                                                                            // stored in
                                                                                            // correct
                                                                                            // form

    // get attributes for grid variable
    attributes.clear();
    OpendapHelper.getAttributes(das, "salt", attributes);
    Test.ensureEqual(attributes.getString("standard_name"), "sea_water_practical_salinity",
        "");
    Test.ensureEqual(attributes.getString("units"), "PSU", "");

    // test get dimension data - all
    String threddsQuery = "lat";
    String erddapQuery = "latitude";
    String2.log("\nFrom thredds:\n" + String2.annotatedString(
        SSR.getUrlResponseStringUnchanged(threddsUrl + ".asc?" + threddsQuery)));
    String2.log("\nFrom erddap:\n" + String2.annotatedString(
        SSR.getUrlResponseStringUnchanged(erddapUrl + ".asc?" + erddapQuery))); // in tests,
                                                                                // always
                                                                                // non-https
                                                                                // url
    tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsQuery);
    epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + erddapQuery);
    Test.ensureEqual(tpas[0].size(), 330, "");
    Test.ensureEqual(epas[0].size(), 330, "");
    Test.ensureEqual(tpas[0].getDouble(0), -75.25, "");
    Test.ensureEqual(epas[0].getDouble(0), -75.25, "");
    Test.ensureEqual(tpas[0].getDouble(300), 74.75, "");
    Test.ensureEqual(epas[0].getDouble(300), 74.75, "");

    // test get dimension data - part
    threddsQuery = SSR.percentEncode("lat[10:2:20]");
    erddapQuery = SSR.percentEncode("latitude[10:2:20]");
    String2.log("\nFrom thredds:\n" + String2.annotatedString(
        SSR.getUrlResponseStringUnchanged(threddsUrl + ".asc?" + threddsQuery)));
    String2.log("\nFrom erddap:\n" + String2.annotatedString(
        SSR.getUrlResponseStringUnchanged(erddapUrl + ".asc?" + erddapQuery))); // in tests,
                                                                                // always
                                                                                // non-https
                                                                                // url
    tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsQuery);
    epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + erddapQuery);
    Test.ensureEqual(tpas[0].toString(),
        "-70.25, -69.25, -68.25, -67.25, -66.25, -65.25",
        "");
    Test.ensureEqual(epas[0].toString(),
        "-70.25, -69.25, -68.25, -67.25, -66.25, -65.25",
        "");

    // get grid data
    // chlorophyll[177][0][2080:20:2500][4500:20:4940]
    String threddsUserDapQuery = SSR.percentEncode("salt[177][0][8:2:10][350]");
    String griddapUserDapQuery = SSR.percentEncode("salt[177][0][8:2:10][350]");
    String2.log("\nFrom thredds:\n" + String2.annotatedString(
        SSR.getUrlResponseStringUnchanged(threddsUrl + ".asc?" + threddsUserDapQuery)));
    String2.log("\nFrom erddap:\n" + String2.annotatedString(
        SSR.getUrlResponseStringUnchanged(erddapUrl + ".asc?" + griddapUserDapQuery))); // in
                                                                                        // tests,
                                                                                        // always
                                                                                        // non-https
                                                                                        // url

    // corresponding time varies, so just make sure they match
    tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsUserDapQuery);
    epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + griddapUserDapQuery);
    // Test.ensureEqual(epas[1], tpas[1], ""); // time
    Test.ensureEqual(epas[2], tpas[2], ""); // alt
    Test.ensureEqual(epas[3], tpas[3], ""); // lat
    Test.ensureEqual(epas[4], tpas[4], ""); // lon
    Test.ensureEqual(epas[0], tpas[0], ""); // data
    String tTime = Calendar2.epochSecondsToIsoStringTZ(tpas[1].getDouble(0));
    float tData1 = tpas[0].getFloat(0);
    float tData2 = tpas[0].getFloat(1);

    // *** test that EDDGridFromDAP works via netcdf-java library
    String2.log("\n****************** EDDGridFromDap test netcdf-java\n");
    NetcdfFile nc = NetcdfDatasets.openFile(EDStatic.erddapUrl + "/griddap/hawaii_d90f_20ee_c4cb", null); // in
                                                                                                  // tests,
                                                                                                  // always
                                                                                                  // non-https
                                                                                                  // url
    try {
      results = nc.toString();
      results = NcHelper.decodeNcDump(results); // added with switch to netcdf-java 4.0
      String tUrl = String2.replaceAll(EDStatic.erddapUrl, "http:", "dods:"); // in tests, always
                                                                              // non-https url
      expected = // these are regex lines
          "netcdf hawaii_d90f_20ee_c4cb {\n" + //
                        "  dimensions:\n" + //
                        "    time = 1680;\n" + //
                        "    depth = 40;\n" + //
                        "    latitude = 330;\n" + //
                        "    longitude = 720;\n" + //
                        "  variables:\n" + //
                        "    double time(time=1680);\n" + //
                        "      :_CoordinateAxisType = \"Time\";\n" + //
                        "      :actual_range = -3.122928E9, 1.2923712E9; // double\n" + //
                        "      :axis = \"T\";\n" + //
                        "      :ioos_category = \"Time\";\n" + //
                        "      :long_name = \"Centered Time\";\n" + //
                        "      :standard_name = \"time\";\n" + //
                        "      :time_origin = \"01-JAN-1970 00:00:00\";\n" + //
                        "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" + //
                        "\n" + //
                        "    double depth(depth=40);\n" + //
                        "      :_CoordinateAxisType = \"Height\";\n" + //
                        "      :_CoordinateZisPositive = \"down\";\n" + //
                        "      :actual_range = 5.01, 5375.0; // double\n" + //
                        "      :axis = \"Z\";\n" + //
                        "      :ioos_category = \"Location\";\n" + //
                        "      :long_name = \"Depth\";\n" + //
                        "      :positive = \"down\";\n" + //
                        "      :standard_name = \"depth\";\n" + //
                        "      :units = \"m\";\n" + //
                        "\n" + //
                        "    double latitude(latitude=330);\n" + //
                        "      :_CoordinateAxisType = \"Lat\";\n" + //
                        "      :actual_range = -75.25, 89.25; // double\n" + //
                        "      :axis = \"Y\";\n" + //
                        "      :ioos_category = \"Location\";\n" + //
                        "      :long_name = \"Latitude\";\n" + //
                        "      :standard_name = \"latitude\";\n" + //
                        "      :units = \"degrees_north\";\n" + //
                        "\n" + //
                        "    double longitude(longitude=720);\n" + //
                        "      :_CoordinateAxisType = \"Lon\";\n" + //
                        "      :actual_range = 0.25, 359.75; // double\n" + //
                        "      :axis = \"X\";\n" + //
                        "      :ioos_category = \"Location\";\n" + //
                        "      :long_name = \"Longitude\";\n" + //
                        "      :standard_name = \"longitude\";\n" + //
                        "      :units = \"degrees_east\";\n" + //
                        "\n" + //
                        "    float temp(time=1680, depth=40, latitude=330, longitude=720);\n" + //
                        "      :_CoordinateAxes = \"time depth latitude longitude \";\n" + //
                        "      :_FillValue = -9.99E33f; // float\n" + //
                        "      :colorBarMaximum = 32.0; // double\n" + //
                        "      :colorBarMinimum = 0.0; // double\n" + //
                        "      :ioos_category = \"Temperature\";\n" + //
                        "      :long_name = \"Sea Water Temperature\";\n" + //
                        "      :missing_value = -9.99E33f; // float\n" + //
                        "      :standard_name = \"sea_water_temperature\";\n" + //
                        "      :units = \"degree_C\";\n" + //
                        "\n" + //
                        "    float salt(time=1680, depth=40, latitude=330, longitude=720);\n" + //
                        "      :_CoordinateAxes = \"time depth latitude longitude \";\n" + //
                        "      :_FillValue = -9.99E33f; // float\n" + //
                        "      :colorBarMaximum = 37.0; // double\n" + //
                        "      :colorBarMinimum = 32.0; // double\n" + //
                        "      :ioos_category = \"Salinity\";\n" + //
                        "      :long_name = \"Sea Water Practical Salinity\";\n" + //
                        "      :missing_value = -9.99E33f; // float\n" + //
                        "      :standard_name = \"sea_water_practical_salinity\";\n" + //
                        "      :units = \"PSU\";\n" + //
                        "\n" + //
                        "    float u(time=1680, depth=40, latitude=330, longitude=720);\n" + //
                        "      :_CoordinateAxes = \"time depth latitude longitude \";\n" + //
                        "      :_FillValue = -9.99E33f; // float\n" + //
                        "      :colorBarMaximum = 0.5; // double\n" + //
                        "      :colorBarMinimum = -0.5; // double\n" + //
                        "      :ioos_category = \"Currents\";\n" + //
                        "      :long_name = \"Eastward Sea Water Velocity\";\n" + //
                        "      :missing_value = -9.99E33f; // float\n" + //
                        "      :standard_name = \"eastward_sea_water_velocity\";\n" + //
                        "      :units = \"m s-1\";\n" + //
                        "\n" + //
                        "    float v(time=1680, depth=40, latitude=330, longitude=720);\n" + //
                        "      :_CoordinateAxes = \"time depth latitude longitude \";\n" + //
                        "      :_FillValue = -9.99E33f; // float\n" + //
                        "      :colorBarMaximum = 0.5; // double\n" + //
                        "      :colorBarMinimum = -0.5; // double\n" + //
                        "      :ioos_category = \"Currents\";\n" + //
                        "      :long_name = \"Northward Sea Water Velocity\";\n" + //
                        "      :missing_value = -9.99E33f; // float\n" + //
                        "      :standard_name = \"northward_sea_water_velocity\";\n" + //
                        "      :units = \"m s-1\";\n" + //
                        "\n" + //
                        "    float w(time=1680, depth=40, latitude=330, longitude=720);\n" + //
                        "      :_CoordinateAxes = \"time depth latitude longitude \";\n" + //
                        "      :_FillValue = -9.99E33f; // float\n" + //
                        "      :colorBarMaximum = 1.0E-5; // double\n" + //
                        "      :colorBarMinimum = -1.0E-5; // double\n" + //
                        "      :comment = \"WARNING: Please use this variable's data with caution.\";\n" + //
                        "      :ioos_category = \"Currents\";\n" + //
                        "      :long_name = \"Upward Sea Water Velocity\";\n" + //
                        "      :missing_value = -9.99E33f; // float\n" + //
                        "      :standard_name = \"upward_sea_water_velocity\";\n" + //
                        "      :units = \"m s-1\";\n" + //
                        "\n" + //
                        "  // global attributes:\n" + //
                        "  :cdm_data_type = \"Grid\";\n" + //
                        "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n" + //
                        "  :dataType = \"Grid\";\n" + //
                        "  :defaultDataQuery = \"temp[last][0][0:last][0:last],salt[last][0][0:last][0:last],u[last][0][0:last][0:last],v[last][0][0:last][0:last],w[last][0][0:last][0:last]\";\n" + //
                        "  :defaultGraphQuery = \"temp[last][0][0:last][0:last]&.draw=surface&.vars=longitude|latitude|temp\";\n" + //
                        "  :documentation = \"http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php\";\n" + //
                        "  :Easternmost_Easting = 359.75; // double\n" + //
                        "  :geospatial_lat_max = 89.25; // double\n" + //
                        "  :geospatial_lat_min = -75.25; // double\n" + //
                        "  :geospatial_lat_resolution = 0.5; // double\n" + //
                        "  :geospatial_lat_units = \"degrees_north\";\n" + //
                        "  :geospatial_lon_max = 359.75; // double\n" + //
                        "  :geospatial_lon_min = 0.25; // double\n" + //
                        "  :geospatial_lon_resolution = 0.5; // double\n" + //
                        "  :geospatial_lon_units = \"degrees_east\";\n" + //
                        "  :history = \"Tue Apr 30 05:42:52 HST 2024 : imported by GrADS Data Server 2.0\n";
      // int po = results.indexOf(":history = \"NASA GSFC (OBPG)\n");
      // Test.ensureTrue(po > 0, "RESULTS=\n" + results);
      // Test.ensureLinesMatch(results.substring(0, po + 29), expected, "RESULTS=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      expected =
      "  :infoUrl = \"https://www.atmos.umd.edu/~ocean/\";\n" + //
                "  :institution = \"TAMU/UMD\";\n" + //
                "  :keywords = \"circulation, currents, density, depths, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water\";\n" + //
                "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" + //
                "  :license = \"The data may be used and redistributed for free but is not intended\n" + //
                "for legal use, since it may contain inaccuracies. Neither the data\n" + //
                "Contributor, ERD, NOAA, nor the United States Government, nor any\n" + //
                "of their employees or contractors, makes any warranty, express or\n" + //
                "implied, including warranties of merchantability and fitness for a\n" + //
                "particular purpose, or assumes any legal liability for the accuracy,\n" + //
                "completeness, or usefulness, of this information.\";\n" + //
                "  :Northernmost_Northing = 89.25; // double\n" + //
                "  :sourceUrl = \"http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4\";\n" + //
                "  :Southernmost_Northing = -75.25; // double\n" + //
                "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" + //
                "  :summary = \"Simple Ocean Data Assimilation (SODA) version 2.2.4 - A reanalysis of ocean \n" + //
                "climate. SODA uses the GFDL modular ocean model version 2.2. The model is \n" + //
                "forced by observed surface wind stresses from the COADS data set (from 1958 \n" + //
                "to 1992) and from NCEP (after 1992). Note that the wind stresses were \n" + //
                "detrended before use due to inconsistencies with observed sea level pressure \n" + //
                "trends. The model is also constrained by constant assimilation of observed \n" + //
                "temperatures, salinities, and altimetry using an optimal data assimilation \n" + //
                "technique. The observed data comes from: 1) The World Ocean Atlas 1994 which \n" + //
                "contains ocean temperatures and salinities from mechanical \n" + //
                "bathythermographs, expendable bathythermographs and conductivity-temperature-\n" + //
                "depth probes. 2) The expendable bathythermograph archive 3) The TOGA-TAO \n" + //
                "thermistor array 4) The Soviet SECTIONS tropical program 5) Satellite \n" + //
                "altimetry from Geosat, ERS/1 and TOPEX/Poseidon. \n" + //
                "We are now exploring an eddy-permitting reanalysis based on the Parallel \n" + //
                "Ocean Program POP-1.4 model with 40 levels in the vertical and a 0.4x0.25 \n" + //
                "degree displaced pole grid (25 km resolution in the western North \n" + //
                "Atlantic).  The first version of this we will release is SODA1.2, a \n" + //
                "reanalysis driven by ERA-40 winds covering the period 1958-2001 (extended to \n" + //
                "the current year using available altimetry).\";\n" + //
                "  :time_coverage_end = \"2010-12-15T00:00:00Z\";\n" + //
                "  :time_coverage_start = \"1871-01-15T00:00:00Z\";\n" + //
                "  :title = \"SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths)\";\n" + //
                "  :Westernmost_Easting = 0.25; // double\n" + //
                "}\n";
      Test.ensureEqual(results.substring(results.indexOf("  :infoUrl =")), expected,
          "RESULTS=\n" + results);

      attributes.clear();
      NcHelper.getGroupAttributes(nc.getRootGroup(), attributes);
      Test.ensureEqual(attributes.getString("institution"), "TAMU/UMD", "");
      Test.ensureEqual(attributes.getString("keywords"),
          "circulation, currents, density, depths, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water",
          "found=" + attributes.getString("keywords"));

      // get attributes for a dimension
      Variable ncLat = nc.findVariable("latitude");
      attributes.clear();
      NcHelper.getVariableAttributes(ncLat, attributes);
      Test.ensureEqual(attributes.getString("coordsys"), null, "");
      Test.ensureEqual(attributes.get("fraction_digits"), null, "");

      // get attributes for grid variable
      Variable ncSalt = nc.findVariable("salt");
      attributes.clear();
      NcHelper.getVariableAttributes(ncSalt, attributes);
      Test.ensureEqual(attributes.getString("standard_name"),
          "sea_water_practical_salinity", "");
      Test.ensureEqual(attributes.getString("units"), "PSU", "");

      // test get dimension data - all
      PrimitiveArray pa = NcHelper.getPrimitiveArray(ncLat);
      Test.ensureEqual(pa.elementType(), PAType.DOUBLE, "");
      Test.ensureEqual(pa.size(), 330, "");
      Test.ensureEqual(pa.getDouble(0), -75.25, "");
      Test.ensureEqual(pa.getDouble(329), 89.25, "");

      // test get dimension data - part
      pa = NcHelper.getPrimitiveArray(ncLat, 10, 20);
      Test.ensureEqual(pa.elementType(), PAType.DOUBLE, "");
      Test.ensureEqual(pa.size(), 11, "");
      Test.ensureEqual(pa.getDouble(0), -70.25, "");
      Test.ensureEqual(pa.getDouble(10), -65.25, "");

      // get grid data
      pa = NcHelper.get4DValues(ncSalt, 600, 300, 0, 170, 190); // x,y,z,t1,t2
      Test.ensureEqual(pa.elementType(), PAType.FLOAT, "");
      String2.log("pa=" + pa);
      Test.ensureEqual(pa.size(), 21, "");
      // pre 2010-10-26 was 0.113f
      // pre 2012-08-17 was 0.12906f
      Test.ensureEqual(pa.getFloat(0), 32.992302f, "");
      Test.ensureEqual(pa.getFloat(1), 33.015766f, "");

    } finally {
      nc.close();
    }

    // *** test that EDDGridFromDap can treat itself as a datasource
    String2.log("\n*** EDDGridFromDap test can treat itself as a datasource\n");
    ArrayList tDataVariables = new ArrayList();

    EDDGrid eddGrid2 = new EDDGridFromDap(
        "erddapChlorophyll", // String tDatasetID,
        null, null, true,
        null, null, null, null, null, null,
        null,
        new Object[][] {
            { // dataVariables[dvIndex][0=sourceName, 1=destName, 2=addAttributes]
                "salt", null, null } },
        60, // int tReloadEveryNMinutes,
        -1, // updateEveryNMillis,
        erddapUrl, -1, true); // sourceUrl, nThreads, dimensionValuesInMemory); //in tests,
                              // always non-https
                              // url

    // .xhtml
    tName = eddGrid2.makeNewFileForDapQuery(language, null, null, griddapUserDapQuery,
        EDStatic.fullTestCacheDirectory, eddGrid2.className() + "_Itself", ".xhtml");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" + //
            "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" + //
            "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n" + //
            "<head>\n" + //
            "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" + //
            "  <title>EDDGridFromDap_Itself</title>\n" + //
            "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:8080/erddap/images/erddap2.css\" />\n" + //
            "</head>\n" + //
            "<body>\n" + //
            "\n" + //
            "&nbsp;\n" + //
            "<table class=\"erd commonBGColor nowrap\">\n" + //
            "<tr>\n" + //
            "<th>time</th>\n" + //
            "<th>depth</th>\n" + //
            "<th>latitude</th>\n" + //
            "<th>longitude</th>\n" + //
            "<th>salt</th>\n" + //
            "</tr>\n" + //
            "<tr>\n" + //
            "<th>UTC</th>\n" + //
            "<th>m</th>\n" + //
            "<th>degrees_north</th>\n" + //
            "<th>degrees_east</th>\n" + //
            "<th>PSU</th>\n" + //
            "</tr>\n" + //
            "<tr>\n" + //
            "<td>1885-10-15T00:00:00Z</td>\n" + //
            "<td class=\"R\">5.01</td>\n" + //
            "<td class=\"R\">-71.25</td>\n" + //
            "<td class=\"R\">175.25</td>\n" + //
            "<td class=\"R\">34.12381</td>\n" + //
            "</tr>\n" + //
            "<tr>\n" + //
            "<td>1885-10-15T00:00:00Z</td>\n" + //
            "<td class=\"R\">5.01</td>\n" + //
            "<td class=\"R\">-70.25</td>\n" + //
            "<td class=\"R\">175.25</td>\n" + //
            "<td class=\"R\">34.12087</td>\n" + //
            "</tr>\n" + //
            "</table>\n" + //
            "</body>\n" + //
            "</html>\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /** EDDGridFromErddap */

  /**
   * testGenerateDatasetsXml
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testEDDGridFromErddapGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();

    // test local generateDatasetsXml
    String results = EDDGridFromErddap.generateDatasetsXml(EDStatic.erddapUrl, false) + "\n"; // in tests, always
                                                                                              // non-https url
    String2.log("results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults = (new GenerateDatasetsXml()).doIt(new String[] { "-verbose",
        "EDDGridFromErddap", EDStatic.erddapUrl, "false" },
        false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String expected = "<dataset type=\"EDDGridFromErddap\" datasetID=\"localhost_e87c_42d8_b49d\" active=\"true\">\n"
        + //
        "    <!-- Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite) -->\n"
        + //
        "    <sourceUrl>http://localhost:8080/erddap/griddap/erdMH1chla8day</sourceUrl>\n" + //
        "</dataset>\n";
    int po = results.indexOf(expected.substring(0, 80));
    if (po < 0)
      throw new RuntimeException(results);
    // String2.log("results=" + results);
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "<!-- Of the datasets above, the following datasets are EDDGridFromErddap's at the remote ERDDAP.\n";
    po = results.indexOf(expected.substring(0, 20));
    if (po < 0)
      String2.log("results=" + results);
    Test.ensureEqual(results.substring(po, po + expected.length()), expected, "results=\n" + results);
    Test.ensureTrue(results.indexOf("rMH1chla8day", po) > 0,
        "results=\n" + results +
            "\nTHIS TEST REQUIRES rMH1chla8day TO BE ACTIVE ON THE localhost ERDDAP.");

    // ensure it is ready-to-use by making a dataset from it
    String tDatasetID = "localhost_04bf_84c9_022b";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDGridFromErddap.oneFromXmlFragment(null, results);
    String2.log(
        "\n!!! The first dataset will vary, depending on which are currently active!!!\n" +
            "title=" + edd.title() + "\n" +
            "datasetID=" + edd.datasetID() + "\n" +
            "vars=" + String2.toCSSVString(edd.dataVariableDestinationNames()));
    Test.ensureEqual(edd.title(),
        "Audio data from a local source.", "");
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()),
        "channel_1", "");
  }

  /** This does some basic tests. */
  @ParameterizedTest
  @ValueSource(booleans = { true })
  @TagJetty
  void testEDDGridFromErddapBasic(boolean testLocalErddapToo) throws Throwable {
    // testVerboseOn();
    int language = 0;
    EDDGridFromErddap gridDataset;
    String name, tName, axisDapQuery, query, results, expected, expected2, error;
    int tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String userDapQuery = SSR
        .minimalPercentEncode("chlorophyll[(2003-01-17)][(29.020832)][(-147.97917):1:(-147.8)]");
    String graphDapQuery = SSR.minimalPercentEncode("chlorophyll[0:10:200][][(29)][(225)]");
    String mapDapQuery = SSR.minimalPercentEncode("chlorophyll[200][][(29):(50)][(225):(247)]"); // stride
                                                                                                 // irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    String localUrl = EDStatic.erddapUrl + "/griddap/rMH1chla8day"; // in tests, always non-https url

    gridDataset = (EDDGridFromErddap) EDDGridFromErddap.oneFromDatasetsXml(null, "rMH1chla8day");

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDGridFromErddap test entire dataset\n");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "Attributes {\n" + //
        "  time {\n" + //
        "    String _CoordinateAxisType \"Time\";\n" + //
        "    Float64 actual_range MIN, MAX;\n" +
        "    String axis \"T\";\n" + //
        "    String ioos_category \"Time\";\n" + //
        "    String long_name \"Centered Time\";\n" + //
        "    String standard_name \"time\";\n" + //
        "    String time_origin \"01-JAN-1970 00:00:00\";\n" + //
        "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" + //
        "  }\n" + //
        "  latitude {\n" + //
        "    String _CoordinateAxisType \"Lat\";\n" + //
        "    Float32 actual_range MIN, MAX;\n" + //
        "    String axis \"Y\";\n" + //
        "    String ioos_category \"Location\";\n" + //
        "    String long_name \"Latitude\";\n" + //
        "    String standard_name \"latitude\";\n" + //
        "    String units \"degrees_north\";\n" + //
        "    Float32 valid_max 90.0;\n" + //
        "    Float32 valid_min -90.0;\n" + //
        "  }\n" + //
        "  longitude {\n" + //
        "    String _CoordinateAxisType \"Lon\";\n" + //
        "    Float32 actual_range MIN, MAX;\n" + //
        "    String axis \"X\";\n" + //
        "    String ioos_category \"Location\";\n" + //
        "    String long_name \"Longitude\";\n" + //
        "    String standard_name \"longitude\";\n" + //
        "    String units \"degrees_east\";\n" + //
        "    Float32 valid_max 180.0;\n" + //
        "    Float32 valid_min -180.0;\n" + //
        "  }\n" + //
        "  chlorophyll {\n" + //
        "    Float32 _FillValue NaN;\n" + //
        "    Float64 colorBarMaximum 30.0;\n" + //
        "    Float64 colorBarMinimum 0.03;\n" + //
        "    String colorBarScale \"Log\";\n" + //
        "    String ioos_category \"Ocean Color\";\n" + //
        "    String long_name \"Mean Chlorophyll a Concentration\";\n" + //
        "    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n"
        + //
        "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" + //
        "    String units \"mg m-3\";\n" + //
        "    Float32 valid_max 100.0;\n" + //
        "    Float32 valid_min 0.001;\n" + //
        "  }\n" + //
        "  NC_GLOBAL {\n" + //
        "    String _lastModified \"YYYY-MM-DDTHH:mm:ss.000Z\";\n" + //
        // " String _NCProperties \"version=2,netcdf=4.7.3,hdf5=1.12.0,\";\n" + //
        "    String cdm_data_type \"Grid\";\n" + //
        "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n" + //
        "    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n" + //
        "    String creator_name \"NASA/GSFC/OBPG\";\n" + //
        "    String creator_type \"group\";\n" + //
        "    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n" + //
        "    String date_created \"YYYY-MM-DDTHH:mm:ss.000Z\";\n" + //
        "    Float64 Easternmost_Easting 179.9792;\n" + //
        "    Float64 geospatial_lat_max 89.97916;\n" + //
        "    Float64 geospatial_lat_min MIN;\n" + //
        // " Float64 geospatial_lat_resolution 0.04166666589488307;\n" + //
        "    String geospatial_lat_units \"degrees_north\";\n" + //
        "    Float64 geospatial_lon_max 179.9792;\n" + //
        "    Float64 geospatial_lon_min -179.9792;\n" + //
        // " Float64 geospatial_lon_resolution 0.041666674383609215;\n" + //
        "    String geospatial_lon_units \"degrees_east\";\n" + //
        "    String grid_mapping_name \"latitude_longitude\";\n" +
        "    String history \"Files downloaded daily from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA SWFSC ERD (erd.data@noaa.gov)\n"
        + //
        "ERDDAP adds the time dimension.\n" + //
        "Direct read of HDF4 file through CDM library\n"; //
    // "2024-05-07T19:17:02Z (local files)\n" + //
    // "2024-05-07T19:17:02Z
    // http://localhost:8080/erddap/griddap/rMH1chla8day.das\";\n" + //

    expected2 = "    String identifier_product_doi \"10.5067/AQUA/MODIS_OC.2014.0\";\n" + //
        "    String identifier_product_doi_authority \"https://dx.doi.org\";\n" + //
        "    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\";\n" + //
        "    String institution \"NASA/GSFC OBPG\";\n" + //
        "    String instrument \"MODIS\";\n" + //
        "    String keywords \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, flight, goddard, group, gsfc, image, imaging, L3, level, level-3, mapped, moderate, modis, nasa, obpg, ocean, ocean color, oceans, oci, optics, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, time, water\";\n"
        + //
        "    String keywords_vocabulary \"GCMD Science Keywords\";\n" + //
        "    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n"
        + //
        "    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
        + //
        "The data may be used and redistributed for free but is not intended\n" + //
        "for legal use, since it may contain inaccuracies. Neither the data\n" + //
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" + //
        "of their employees or contractors, makes any warranty, express or\n" + //
        "implied, including warranties of merchantability and fitness for a\n" + //
        "particular purpose, or assumes any legal liability for the accuracy,\n" + //
        "completeness, or usefulness, of this information.\";\n" + //
        "    String map_projection \"Equidistant Cylindrical\";\n" + //
        "    String measure \"Mean\";\n" + //
        "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n" + //
        "    Float64 Northernmost_Northing 89.97916;\n" + //
        "    String platform \"Aqua\";\n" + //
        "    String processing_level \"L3 Mapped\";\n" + //
        "    String processing_version \"2014.0\";\n" + //
        "    String product_name \"A20030092003016.L3m_8D_CHL_chlor_a_4km.nc\";\n" + //
        "    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" + //
        "    String publisher_email \"erd.data@noaa.gov\";\n" + //
        "    String publisher_name \"NOAA NMFS SWFSC ERD\";\n" + //
        "    String publisher_type \"institution\";\n" + //
        "    String publisher_url \"https://www.pfeg.noaa.gov\";\n" + //
        "    String sourceUrl \"(local files)\";\n" + //
        "    Float64 Southernmost_Northing -89.97918;\n" + //
        "    String spatialResolution \"4.60 km\";\n" + //
        "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" + //
        "    String summary \"This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.\";\n"
        + //
        "    String temporal_range \"8-day\";\n" + //
        "    String testOutOfDate \"now-30days\";\n" + //
        "    String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ\";\n" + //
        "    String time_coverage_start \"2003-01-05T00:00:00Z\";\n" + //
        "    String title \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\";\n"
        + //
        "    Float64 Westernmost_Easting -179.9792;\n" + //
        "  }\n" + //
        "}\n";
    // tPo = results.indexOf("history \"NASA GSFC (OBPG)");
    // Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
    results = results.replaceAll("Float64 actual_range -?[0-9]+.[0-9]+e?.[0-9]?, -?[0-9]+.[0-9]+e?.[0-9]?;",
        "Float64 actual_range MIN, MAX;");
    results = results.replaceAll("Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
        "Float32 actual_range MIN, MAX;");
    results = results.replaceAll("String _lastModified \"....-..-..T..:..:...000Z",
        "String _lastModified \"YYYY-MM-DDTHH:mm:ss.000Z");
    results = results.replaceAll("String date_created \"....-..-..T..:..:...000Z",
        "String date_created \"YYYY-MM-DDTHH:mm:ss.000Z");
    results = results.replaceAll("Float64 geospatial_lat_min -?[0-9]+.[0-9]+", "Float64 geospatial_lat_min MIN");
    results = results.replaceAll("String time_coverage_end \\\"....-..-..T..:..:..Z",
        "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
    Test.ensureEqual(results.substring(0, expected.length()), expected,
        "\nresults=\n" + results);
    // Test.ensureLinesMatch(results.substring(0, tPo + 25), expected,
    // "\nresults=\n" + results);

    tPo = results.indexOf("    String identifier_product_doi");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".das");
      results = results.replaceAll("Float64 actual_range -?[0-9]+.[0-9]+e?.[0-9]?, -?[0-9]+.[0-9]+e?.[0-9]?;",
          "Float64 actual_range MIN, MAX;");
      results = results.replaceAll("Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
          "Float32 actual_range MIN, MAX;");
      results = results.replaceAll("String _lastModified \"....-..-..T..:..:...000Z",
          "String _lastModified \"YYYY-MM-DDTHH:mm:ss.000Z");
      results = results.replaceAll("String date_created \"....-..-..T..:..:...000Z",
          "String date_created \"YYYY-MM-DDTHH:mm:ss.000Z");
      results = results.replaceAll("Float64 geospatial_lat_min -?[0-9]+.[0-9]+",
          "Float64 geospatial_lat_min MIN");
      results = results.replaceAll("String time_coverage_end \\\"....-..-..T..:..:..Z",
          "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
      // tPo = results.indexOf("history \"NASA GSFC (OBPG)");
      // Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
      // Test.ensureLinesMatch(results.substring(0, tPo + 25), expected,
      // "\nresults=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected,
          "\nresults=\n" + results);

      tPo = results.indexOf("    String identifier_product_doi");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
      Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);
    }

    // *** test getting dds for entire dataset
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "", EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "Dataset {\n" + //
        "  Float64 time[time = TIME];\n" + //
        "  Float32 latitude[latitude = LATITUDE];\n" + //
        "  Float32 longitude[longitude = LONGITUDE];\n" + //
        "  GRID {\n" + //
        "    ARRAY:\n" + //
        "      Float32 chlorophyll[time = TIME][latitude = LATITUDE][longitude = LONGITUDE];\n" + //
        "    MAPS:\n" + //
        "      Float64 time[time = TIME];\n" + //
        "      Float32 latitude[latitude = LATITUDE];\n" + //
        "      Float32 longitude[longitude = LONGITUDE];\n" + //
        "  } chlorophyll;\n" + //
        "} rMH1chla8day;\n";
    results = results.replaceAll("time = [0-9]+", "time = TIME");
    results = results.replaceAll("latitude = [0-9]+", "latitude = LATITUDE");
    results = results.replaceAll("longitude = [0-9]+", "longitude = LONGITUDE");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".dds");
      results = results.replaceAll("time = [0-9]+", "time = TIME");
      results = results.replaceAll("latitude = [0-9]+", "latitude = LATITUDE");
      results = results.replaceAll("longitude = [0-9]+", "longitude = LONGITUDE");
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    // ********************************************** test getting axis data

    // .asc
    String2.log("\n*** EDDGridFromErddap test get .ASC axis data\n");
    query = "time%5B0:1:1%5D,longitude%5Blast%5D";
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Axis", ".asc");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Float64 time[time = TIME];\n" +
        "  Float32 longitude[longitude = LONGITUDE];\n" +
        "} "; // r
    expected2 = "MH1chla8day;\n" +
        "---------------------------------------------\n" +
        "Data:\n" +
        "time[TIME]\n" +
        "VAL1, VAL2\n" +
        "longitude[LONGITUDE]\n" +
        "179.97917\n";
    results = results.replaceAll("time = [0-9]+", "time = TIME");
    results = results.replaceAll("time.[0-9]+.", "time[TIME]");
    results = results.replaceAll("longitude = [0-9]+", "longitude = LONGITUDE");
    results = results.replaceAll("longitude.[0-9]+.", "longitude[LONGITUDE]");
    results = results.replaceAll("-?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+\n", "VAL1, VAL2\n");
    Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      // 2018-08-08 this is an important test of SSR.getUrlResponseStringUnchanged
      // following a redirect from http to https,
      // which was previously not supported in SSR
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".asc?" + query);
      results = results.replaceAll("time = [0-9]+", "time = TIME");
      results = results.replaceAll("time.[0-9]+.", "time[TIME]");
      results = results.replaceAll("longitude = [0-9]+", "longitude = LONGITUDE");
      results = results.replaceAll("longitude.[0-9]+.", "longitude[LONGITUDE]");
      results = results.replaceAll("-?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+\n", "VAL1, VAL2\n");
      Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
    }

    // .csv
    String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
    query = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Axis", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected = "time,longitude\n" +
        "UTC,degrees_east\n" +
        "2003-01-05T00:00:00Z,179.97917\n" + //
        "2003-01-13T00:00:00Z,NaN\n";
    // "2002-07-08T00:00:00Z,360.0\n" +
    // "2004-09-25T00:00:00Z,NaN\n" +
    // "2006-12-15T00:00:00Z,NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv?" + query);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    // .csv test of gridName.axisName notation
    String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
    query = SSR.minimalPercentEncode("chlorophyll.time[0:1:1],chlorophyll.longitude[last]");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_AxisG.A", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    // expected = "time,longitude\n" +
    // "UTC,degrees_east\n" +
    // "2002-07-08T00:00:00Z,360.0\n" +
    // "2004-09-25T00:00:00Z,NaN\n" +
    // "2006-12-15T00:00:00Z,NaN\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv?" + query);
      Test.ensureEqual(results, expected, "\nresults=\n" + results);
    }

    // .dods
    String2.log("\n*** EDDGridFromErddap test get .DODS axis data\n");
    query = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Axis", ".dods");
    results = String2.annotatedString(File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log(results);
    expected = "Dataset {[10]\n" +
        "  Float64 time[time = 2];[10]\n" +
        "  Float32 longitude[longitude = 1];[10]\n" +
        "} "; // r
    expected2 = "MH1chla8day;[10]\n" +
        "[10]\n" +
        "Data:[10]\n" +
        "[0][0][0][2][0][0][0][2]A[207][11][186][192][0][0][0]A[207][17][0][192][0][0][0][0][0][0][1][0][0][0][1]C3[250][171][end]";
    Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = String2.annotatedString(SSR.getUrlResponseStringUnchanged(localUrl + ".dods?" + query));
      Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
    }

    // .mat
    // octave> load('c:/temp/griddap/EDDGridFromErddap_Axis.mat');
    // octave> erdMHchla8day
    String matlabAxisQuery = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    String2.log("\n*** EDDGridFromErddap test get .MAT axis data\n");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, matlabAxisQuery,
        EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Axis", ".mat");
    String2.log(".mat test file is " + EDStatic.fullTestCacheDirectory + tName);
    results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
    String2.log(results);
    expected = "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n" + //
        "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n" + //
        "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n" + //
        "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n" + //
        // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 57 65 , Created on: We |\n" + //
        // "64 20 4d 61 79 20 38 20 32 30 3a 31 34 3a 31 34 d May 8 20:14:14 |\n" + //
        // "20 32 30 32 34 20 20 20 20 20 20 20 20 20 20 20 2024 |\n" + //
        "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n" + //
        "00 00 00 0e 00 00 01 10   00 00 00 06 00 00 00 08                    |\n" + //
        "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" + //
        "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0c                    |\n" + //
        "72 4d 48 31 63 68 6c 61   38 64 61 79 00 00 00 00   rMH1chla8day     |\n" + //
        "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 40                  @ |\n" + //
        "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n" + //
        "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" + //
        "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n" + //
        "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n" + //
        "00 00 00 0e 00 00 00 40   00 00 00 06 00 00 00 08          @         |\n" + //
        "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n" + //
        "00 00 00 02 00 00 00 01   00 00 00 01 00 00 00 00                    |\n" + //
        "00 00 00 09 00 00 00 10   41 cf 0b ba c0 00 00 00           A        |\n" + //
        "41 cf 11 00 c0 00 00 00   00 00 00 0e 00 00 00 38   A              8 |\n" + //
        "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n" + //
        "00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n" + //
        "00 00 00 01 00 00 00 00   00 00 00 07 00 00 00 04                    |\n" + //
        "43 33 fa ab 00 00 00 00   C3                                         |\n";
    Test.ensureEqual(
        results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation dateTime
        expected, "RESULTS(" + EDStatic.fullTestCacheDirectory + tName + ")=\n" + results);

    // .ncHeader
    String2.log("\n*** EDDGridFromErddap test get .NCHEADER axis data\n");
    query = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, query, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Axis", ".ncHeader");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // "netcdf EDDGridFromErddap_Axis.nc {\n" +
        // " dimensions:\n" +
        // " time = 3;\n" + // (has coord.var)\n" + //changed when switched to
        // netcdf-java 4.0, 2009-02-23
        // " longitude = 1;\n" + // (has coord.var)\n" + //but won't change for
        // testLocalErddapToo until next release
        "  variables:\n" +
            "    double time(time=2);\n" +
            "      :_CoordinateAxisType = \"Time\";\n" +
            "      :actual_range = MIN, MAX; // double\n" + // up-to-date
            "      :axis = \"T\";\n" +
            // " :fraction_digits = 0; // int\n" +
            "      :ioos_category = \"Time\";\n" +
            "      :long_name = \"Centered Time\";\n" +
            "      :standard_name = \"time\";\n" +
            "      :time_origin = \"01-JAN-1970 00:00:00\";\n" +
            "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "\n" +
            "    float longitude(longitude=1);\n" +
            "      :_CoordinateAxisType = \"Lon\";\n" +
            "      :actual_range = MIN, MAX; // float\n" +
            "      :axis = \"X\";\n" +
            // " :coordsys = \"geographic\";\n" +
            // " :fraction_digits = 4; // int\n" +
            "      :ioos_category = \"Location\";\n" +
            "      :long_name = \"Longitude\";\n" +
            // " :point_spacing = \"even\";\n" +
            "      :standard_name = \"longitude\";\n" +
            "      :units = \"degrees_east\";\n" +
            "      :valid_max = 180.0f; // float\n" + //
            "      :valid_min = -180.0f; // float\n" +
            "\n" +
            "  // global attributes:\n";
    // " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n";
    results = results.replaceAll(":actual_range = -?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+",
        ":actual_range = MIN, MAX");
    results = results.replaceAll(":actual_range = -?[0-9]+.[0-9]+f, -?[0-9]+.[0-9]+f",
        ":actual_range = MIN, MAX");
    tPo = results.indexOf("  variables:\n");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected, "RESULTS=\n" + results);
    expected2 = "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\";\n"
        +
        "  :Westernmost_Easting = 179.9792f; // float\n" +
        "}\n";
    Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".ncHeader?" + query);
      results = results.replaceAll(":actual_range = -?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+",
          ":actual_range = MIN, MAX");
      results = results.replaceAll(":actual_range = -?[0-9]+.[0-9]+f, -?[0-9]+.[0-9]+f",
          ":actual_range = MIN, MAX");
      tPo = results.indexOf("  variables:\n");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureEqual(results.substring(tPo, tPo + expected.length()), expected,
          "\nresults=\n" + results);
      Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
    }

    // ********************************************** test getting grid data
    // .csv
    String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, userDapQuery, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // missing values are "NaN"
               // pre 2010-10-26 was
        "time,latitude,longitude,chlorophyll\n" + //
            "UTC,degrees_north,degrees_east,mg m-3\n" + //
            "2003-01-13T00:00:00Z,29.02083,-147.97917,NaN\n" + //
            "2003-01-13T00:00:00Z,29.02083,-147.9375,NaN\n" + //
            "2003-01-13T00:00:00Z,29.02083,-147.89583,NaN\n" + //
            "2003-01-13T00:00:00Z,29.02083,-147.85417,NaN\n" + //
            "2003-01-13T00:00:00Z,29.02083,-147.8125,NaN\n";
    // "time, altitude, latitude, longitude, chlorophyll\n" +
    // "UTC, m, degrees_north, degrees_east, mg m-3\n" +
    // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n" +
    // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n" +
    // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677, 0.099\n"
    // +
    // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434, 0.118\n"
    // +
    // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n" +
    // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953, 0.091\n";
    // "time,altitude,latitude,longitude,chlorophyll\n" +
    // "UTC,m,degrees_north,degrees_east,mg m-3\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
    // pre 2012-08-17 was
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n";
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    // expected2 =
    // pre 2010-10-26 was
    // "2007-02-06T00:00:00Z, 0.0, 49.407270201435495, 232.06852644982058, 0.37\n";
    // pre 2012-08-17 was
    // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
    // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
    // Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv?" + userDapQuery);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
      // Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
    }

    // .csv test gridName.gridName notation
    String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, "chlorophyll." + userDapQuery,
        EDStatic.fullTestCacheDirectory, gridDataset.className() + "_DotNotation", ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // expected =
    // // pre 2010-10-26 was
    // // "time, altitude, latitude, longitude, chlorophyll\n" +
    // // "UTC, m, degrees_north, degrees_east, mg m-3\n" +
    // // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 224.98437319134158, NaN\n"
    // +
    // // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.40108808889917, NaN\n"
    // +
    // // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 225.81780298645677,
    // 0.099\n"
    // // +
    // // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.23451788401434,
    // 0.118\n"
    // // +
    // // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 226.65123278157193, NaN\n"
    // +
    // // "2007-02-06T00:00:00Z, 0.0, 28.985876360268577, 227.06794767912953,
    // 0.091\n";
    // "time,altitude,latitude,longitude,chlorophyll\n" +
    // "UTC,m,degrees_north,degrees_east,mg m-3\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,224.98437319134158,NaN\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.40108808889917,NaN\n" +
    // // pre 2012-08-17 was
    // // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.10655\n"
    // +
    // //
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12478\n";
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,225.81780298645677,0.11093\n" +
    // "2007-02-06T00:00:00Z,0.0,28.985876360268577,226.23451788401434,0.12439\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "RESULTS=\n" + results);
    // expected2 =
    // pre 2010-10-26 was
    // "2007-02-06T00:00:00Z, 0.0, 49.407270201435495, 232.06852644982058, 0.37\n";
    // pre 2012-08-17 was
    // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.58877\n";
    // "2007-02-06T00:00:00Z,0.0,49.407270201435495,232.06852644982058,0.56545\n";
    // Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".csv" + "?chlorophyll." + userDapQuery);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
      // Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
    }

    // .nc
    String2.log("\n*** EDDGridFromErddap test get .NC data\n");
    tName = gridDataset.makeNewFileForDapQuery(language, null, null, userDapQuery, EDStatic.fullTestCacheDirectory,
        gridDataset.className() + "_Data", ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    expected = "netcdf EDDGridFromErddap_Data.nc {\n" + //
        "  dimensions:\n" + //
        "    time = 1;\n" + //
        "    latitude = 1;\n" + //
        "    longitude = 5;\n" + //
        "  variables:\n" + //
        "    double time(time=1);\n" + //
        "      :_CoordinateAxisType = \"Time\";\n" + //
        "      :actual_range = 1.042416E9, 1.042416E9; // double\n" + //
        "      :axis = \"T\";\n" + //
        "      :ioos_category = \"Time\";\n" + //
        "      :long_name = \"Centered Time\";\n" + //
        "      :standard_name = \"time\";\n" + //
        "      :time_origin = \"01-JAN-1970 00:00:00\";\n" + //
        "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" + //
        "\n" + //
        "    float latitude(latitude=1);\n" + //
        "      :_CoordinateAxisType = \"Lat\";\n" + //
        "      :actual_range = 29.02083f, 29.02083f; // float\n" + //
        "      :axis = \"Y\";\n" + //
        "      :ioos_category = \"Location\";\n" + //
        "      :long_name = \"Latitude\";\n" + //
        "      :standard_name = \"latitude\";\n" + //
        "      :units = \"degrees_north\";\n" + //
        "      :valid_max = 90.0f; // float\n" + //
        "      :valid_min = -90.0f; // float\n" + //
        "\n" + //
        "    float longitude(longitude=5);\n" + //
        "      :_CoordinateAxisType = \"Lon\";\n" + //
        "      :actual_range = -147.97917f, -147.8125f; // float\n" + //
        "      :axis = \"X\";\n" + //
        "      :ioos_category = \"Location\";\n" + //
        "      :long_name = \"Longitude\";\n" + //
        "      :standard_name = \"longitude\";\n" + //
        "      :units = \"degrees_east\";\n" + //
        "      :valid_max = 180.0f; // float\n" + //
        "      :valid_min = -180.0f; // float\n" + //
        "\n" + //
        "    float chlorophyll(time=1, latitude=1, longitude=5);\n" + //
        "      :_FillValue = NaNf; // float\n" + //
        "      :colorBarMaximum = 30.0; // double\n" + //
        "      :colorBarMinimum = 0.03; // double\n" + //
        "      :colorBarScale = \"Log\";\n" + //
        "      :ioos_category = \"Ocean Color\";\n" + //
        "      :long_name = \"Mean Chlorophyll a Concentration\";\n" + //
        "      :references = \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n"
        + //
        "      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n" + //
        "      :units = \"mg m-3\";\n" + //
        "      :valid_max = 100.0f; // float\n" + //
        "      :valid_min = 0.001f; // float\n" + //
        "\n" + //
        "  // global attributes:\n" + //
        "  :_lastModified = \"YYYY-MM-DDTHH:mm:ss.000Z\";\n" + //
        "  :cdm_data_type = \"Grid\";\n" + //
        "  :Conventions = \"CF-1.6, COARDS, ACDD-1.3\";\n" + //
        "  :creator_email = \"data@oceancolor.gsfc.nasa.gov\";\n" + //
        "  :creator_name = \"NASA/GSFC/OBPG\";\n" + //
        "  :creator_type = \"group\";\n" + //
        "  :creator_url = \"https://oceandata.sci.gsfc.nasa.gov\";\n" + //
        "  :date_created = \"YYYY-MM-DDTHH:mm:ss.000Z\";\n" + //
        "  :Easternmost_Easting = -147.8125f; // float\n" + //
        "  :geospatial_lat_max = 29.02083f; // float\n" + //
        "  :geospatial_lat_min = 29.02083f; // float\n" + //
        "  :geospatial_lat_units = \"degrees_north\";\n" + //
        "  :geospatial_lon_max = -147.8125f; // float\n" + //
        "  :geospatial_lon_min = -147.97917f; // float\n" + //
        "  :geospatial_lon_units = \"degrees_east\";\n" + //
        "  :grid_mapping_name = \"latitude_longitude\";\n";
    // tPo = results.indexOf(":history = \"NASA GSFC (OBPG)");
    // Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    results = results.replaceAll("\\\"....-..-..T..:..:...000Z\\\"", "\"YYYY-MM-DDTHH:mm:ss.000Z\"");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected = // note original missing values
        "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\";\n" + //
            "  :institution = \"NASA/GSFC OBPG\";\n" + //
            "  :instrument = \"MODIS\";\n" + //
            "  :keywords = \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, flight, goddard, group, gsfc, image, imaging, L3, level, level-3, mapped, moderate, modis, nasa, obpg, ocean, ocean color, oceans, oci, optics, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, time, water\";\n"
            + //
            "  :keywords_vocabulary = \"GCMD Science Keywords\";\n" + //
            "  :l2_flag_names = \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n"
            + //
            "  :license = \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
            + //
            "The data may be used and redistributed for free but is not intended\n" + //
            "for legal use, since it may contain inaccuracies. Neither the data\n" + //
            "Contributor, ERD, NOAA, nor the United States Government, nor any\n" + //
            "of their employees or contractors, makes any warranty, express or\n" + //
            "implied, including warranties of merchantability and fitness for a\n" + //
            "particular purpose, or assumes any legal liability for the accuracy,\n" + //
            "completeness, or usefulness, of this information.\";\n" + //
            "  :map_projection = \"Equidistant Cylindrical\";\n" + //
            "  :measure = \"Mean\";\n" + //
            "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" + //
            "  :Northernmost_Northing = 29.02083f; // float\n" + //
            "  :platform = \"Aqua\";\n" + //
            "  :processing_level = \"L3 Mapped\";\n" + //
            "  :processing_version = \"2014.0\";\n" + //
            "  :product_name = \"A20030092003016.L3m_8D_CHL_chlor_a_4km.nc\";\n" + //
            "  :project = \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n" + //
            "  :publisher_email = \"erd.data@noaa.gov\";\n" + //
            "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n" + //
            "  :publisher_type = \"institution\";\n" + //
            "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n" + //
            "  :sourceUrl = \"(local files)\";\n" + //
            "  :Southernmost_Northing = 29.02083f; // float\n" + //
            "  :spatialResolution = \"4.60 km\";\n" + //
            "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n" + //
            "  :summary = \"This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.\";\n"
            + //
            "  :temporal_range = \"8-day\";\n" + //
            "  :testOutOfDate = \"now-30days\";\n" + //
            "  :time_coverage_end = \"2003-01-13T00:00:00Z\";\n" + //
            "  :time_coverage_start = \"2003-01-13T00:00:00Z\";\n" + //
            "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\";\n"
            + //
            "  :Westernmost_Easting = -147.97917f; // float\n" + //
            "\n" + //
            "  data:\n" + //
            "    time = \n" + //
            "      {1.042416E9}\n" + //
            "    latitude = \n" + //
            "      {29.02083}\n" + //
            "    longitude = \n" + //
            "      {-147.97917, -147.9375, -147.89583, -147.85417, -147.8125}\n" + //
            "    chlorophyll = \n" + //
            "      {\n" + //
            "        {\n" + //
            "          {NaN, NaN, NaN, NaN, NaN}\n" + //
            "        }\n" + //
            "      }\n" + //
            "}\n";

    tPo = results.indexOf("  :infoUrl");
    // int tPo2 = results.indexOf("1.20716},", tPo + 1);
    // if (tPo < 0 || tPo2 < 0)
    // String2.log("tPo=" + tPo + " tPo2=" + tPo2 + " results=\n" + results);

    Test.ensureEqual(results.substring(tPo), expected, "RESULTS=\n" + results);
    /* */
  }

  /**
   * This tests the /files/ "files" system.
   * This requires nceiPH53sstn1day and testGridFromErddap in the local ERDDAP.
   *
   * EDDGridFromNcFiles.testFiles() has more tests than any other testFiles().
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGridFromErddapFiles() throws Throwable {

    String2.log("\n*** EDDGridFromErddap.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/.csv
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/.csv"));
    Test.ensureTrue(results.indexOf("Name,Last modified,Size,Description[10]") == 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "nceiPH53sstd1day/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Daytime (1 Day Composite)\"[10]") > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "rMH1chla8day/,NaN,NaN,\"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\"[10]") > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("documentation.html,") > 0, "results=\n" + results);

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/.csv");
    results = results.replaceAll(",.............,", ",lastMod,");
    expected = "Name,lastMod,Size,Description\n" +
        "A20030012003008.L3m_8D_CHL_chlor_a_4km.nc,lastMod,29437837,\n" + //
        "A20030092003016.L3m_8D_CHL_chlor_a_4km.nc,lastMod,30795913,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/");
    Test.ensureTrue(results.indexOf("A20030012003008&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("A20030092003016&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc") > 0, "results=\n" + results);

    // get /files/datasetID //missing trailing slash will be redirected
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/rMH1chla8day");
    Test.ensureTrue(results.indexOf("A20030012003008&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc") > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("A20030092003016&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc") > 0,
        "results=\n" + results);

    // Todo get a dataset here with a subdir
    // get /files/datasetID/subdir/.csv
    // results = SSR.getUrlResponseStringNewline(
    //     "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/1994/.csv");
    // expected = "Name,Last modified,Size,Description\n" +
    //     "data/,NaN,NaN,\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // // get /files/datasetID/subdir/subdir.csv
    // results = SSR.getUrlResponseStringNewline(
    //     "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/1994/data/.csv");
    // expected = "Name,Last modified,Size,Description\n" +
    //     "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc,1471330800000,12484412,\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // // download a file in root -- none available

    // // download a file in subdir
    // results = String2.annotatedString(SSR.getUrlResponseStringNewline(
    //     "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/1994/data/" +
    //         "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc")
    //     .substring(0, 50));
    // expected = "[137]HDF[10]\n" +
    //     "[26][10]\n" +
    //     "[2][8][8][0][0][0][0][0][0][0][0][0][255][255][255][255][255][255][255][255]<[127][190][0][0][0][0][0]0[0][0][0][0][0][0][0][199](*yOHD[end]";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // query with // at start fails
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "HTTP ERROR 400 Ambiguous URI empty segment";
    Test.ensureTrue(results.indexOf(expected) > 0, "results=\n" + results);

    // query with // later fails
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/rMH1chla8day//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureTrue(results.indexOf(expected) > 0, "results=\n" + results);

    // query with /../ fails
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/../");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:" + PORT + "/erddap/files/rMH1chla8day/../\n"
        +
        "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: /../ is not allowed!\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent datasetID
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download an existent subdirectory but without trailing slash
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/GLsubdir");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: ERROR from url=http://localhost:" + PORT + "/erddap/files/rMH1chla8day/GLsubdir : "
        +
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
        "http://localhost:" + PORT + "/erddap/files/erdMH1chla8day/GLsubdir\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: GLsubdir .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
        "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/gibberish/\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in root
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: ERROR from url=http://localhost:" + PORT + "/erddap/files/rMH1chla8day/gibberish.csv : "
        +
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
        "http://localhost:" + PORT + "/erddap/files/erdMH1chla8day/gibberish.csv\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existent subdir
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/GLsubdir/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: ERROR from url=http://localhost:" + PORT + "/erddap/files/rMH1chla8day/GLsubdir/gibberish.csv : "
        +
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: " +
        "http://localhost:" + PORT + "/erddap/files/erdMH1chla8day/GLsubdir/gibberish.csv\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

  }

  /** EDDGridFromEtopo */

  /**
   * This tests the /files/ "files" system.
   * This requires etopo180 or etopo360 in the localhost ERDDAP.
   *
   * @param tDatasetID etopo180 or etopo360
   */
  @ParameterizedTest
  @ValueSource(strings = { "etopo180", "etopo360" })
  @TagJetty
  void testEtopoGridFiles(String tDatasetID) throws Throwable {

    String2.log("\n*** EDDGridFromEtopo.testFiles(" + tDatasetID + ")\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/.csv");
    expected = "Name,Last modified,Size,Description\n" +
        "etopo1_ice_g_i2.bin,1642733858000,466624802,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/");
    Test.ensureTrue(results.indexOf("etopo1&#x5f;ice&#x5f;g&#x5f;i2&#x2e;bin") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">466624802<") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv

    // download a file in root
    String2.log("This test takes ~30 seconds.");
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/etopo1_ice_g_i2.bin");
    results = String2.annotatedString(results.substring(0, 50));
    expected = "|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239][end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // download a file in subdir

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/"
        + tDatasetID + "/gibberish/\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/"
        + tDatasetID + "/gibberish.csv\n" +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
  }

  /** ErddapTests */

  /**
   * This is used by Bob to do simple tests of Search.
   * This requires a running local ERDDAP with erdMHchla8day and rMHchla8day
   * (among others which will be not matched).
   * This can be used with searchEngine=original or lucene.
   *
   * @throws exception if trouble.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testSearch() throws Throwable {
    // Erddap.verbose = true;
    // Erddap.reallyVerbose = true;
    // EDD.testVerboseOn();
    String htmlUrl = EDStatic.erddapUrl + "/search/index.html?page=1&itemsPerPage=1000";
    String csvUrl = EDStatic.erddapUrl + "/search/index.csv?page=1&itemsPerPage=1000";
    String expected = "pmelTaoDySst";
    String expected2, query, results;
    int count;
    String2.log("\n*** Erddap.testSearch\n" +
        "This assumes localhost ERDDAP is running with erdMHchla8day and rMHchla8day (among others which will be not matched).");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
        // "&searchFor=erdMHchla8day",
        "&searchFor=" + SSR.minimalPercentEncode("tao daily sea surface temperature -air"),
        "&searchFor=pmelTaoDySst", // lucene fail?
        "&searchFor=DySst", // lucene fail?
        // "&searchFor=" + SSR.minimalPercentEncode(
        // "\"sourceUrl=https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH/chla/8day\n\"")
    };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      count = String2.countAll(results, "NOAA PMEL");
      Test.ensureEqual(count, 4, "results=\n" + results + "i=" + i); // one in help, plus one per dataset

      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      count = String2.countAll(results, "NOAA PMEL");
      Test.ensureEqual(count, 2, "results=\n" + results + "i=" + i); // one per dataset
    }

    // query with no matches: valid for .html but error for .csv: protocol
    query = "&searchFor=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    expected = "Your query produced no matching results.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 = "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: Your query produced no matching results. Check the spelling of the word(s) you searched for.\";\n"
        +
        "})";
    Test.ensureTrue(results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

  }

  /** EDDGridCopy */
  /**
   * This tests the /files/ "files" system.
   * This requires testGridCopy in the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGridFiles() throws Throwable {
    // String2.log("\n*** EDDGridCopy.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/.csv");
    expected = "Name,lastModTime,Size,Description\n" +
        "subfolder/,NaN,NaN,\n" +
        "erdQSwind1day_20080101_03.nc.gz,lastModTime,10478645,\n" +
        "erdQSwind1day_20080104_07.nc,lastModTime,49790172,\n";
    results = results.replaceAll(",.............,", ",lastModTime,");
    if (results.length() < expected.length()) {
      throw new Exception(results);
    }
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/");
    Test.ensureTrue(results.indexOf("erdQSwind1day&#x5f;20080104&#x5f;07&#x2e;nc") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">49790172<") > 0, "results=\n" + results);

    // download a file in root
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/erdQSwind1day_20080104_07.nc")
        .substring(0, 50));
    expected = "CDF[1][0][0][0][0][0][0][0][10]\n" +
        "[0][0][0][4][0][0][0][4]time[0][0][0][4][0][0][0][8]altitude[0][0][0][1][0][0][0][8]la[end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/testGriddedNcFiles/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/testGriddedNcFiles/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** EDDGridLon0360 */

  /**
   * This tests a dataset that is initially all LT0.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagImageComparison
  void testLT0() throws Throwable {
    // String2.log("\n****************** EDDGridLon0360.testLT0()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;

    EDDGrid eddGrid = (EDDGrid) EDDTestDataset.gettest_erdVHNchlamday_Lon0360();

    tName = eddGrid.makeNewFileForDapQuery(language, null, null, "", dir,
        eddGrid.className() + "_LT0_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("time = \\d{2,3}", "time = ddd");
    expected = "Dataset {\n" +
        "  Float64 time[time = ddd];\n" +
        "  Float64 altitude[altitude = 1];\n" +
        "  Float64 latitude[latitude = 11985];\n" +
        "  Float64 longitude[longitude = 9333];\n" +
        "  GRID {\n" +
        "    ARRAY:\n" +
        "      Float32 chla[time = ddd][altitude = 1][latitude = 11985][longitude = 9333];\n" +
        "    MAPS:\n" +
        "      Float64 time[time = ddd];\n" +
        "      Float64 altitude[altitude = 1];\n" +
        "      Float64 latitude[latitude = 11985];\n" +
        "      Float64 longitude[longitude = 9333];\n" +
        "  } chla;\n" +
        "} test_erdVHNchlamday_Lon0360;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName = eddGrid.makeNewFileForDapQuery(language, null, null, "", dir,
        eddGrid.className() + "_LT0_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected = "longitude {\n" +
        "    String _CoordinateAxisType \"Lon\";\n" +
        "    Float64 actual_range 180.00375, 249.99375;\n" +
        "    String axis \"X\";\n" +
        "    String coordsys \"geographic\";\n" +
        "    String ioos_category \"Location\";\n" +
        "    String long_name \"Longitude\";\n" +
        "    String point_spacing \"even\";\n" +
        "    String standard_name \"longitude\";\n" +
        "    String units \"degrees_east\";\n" +
        "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    expected = "    Float64 geospatial_lon_max 249.99375;\n" +
        "    Float64 geospatial_lon_min 180.00375;\n" +
        "    Float64 geospatial_lon_resolution 0.007500000000000001;\n" +
        "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    expected = "    Float64 Westernmost_Easting 180.00375;\n" +
        "  }\n" +
        "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(results.substring(po, po + expected.length()), expected,
        "results=\n" + results);

    // lon values
    userDapQuery = "longitude[0:1000:9332]";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        eddGrid.className() + "_LT0_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, 180.00375, 187.50375, 195.00375, 202.50375, 210.00375, 217.50375, 225.00375, 232.50375, 240.00375, 247.50375, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire lon range
    userDapQuery = "chla[(2015-03-16)][][(89.77152):5500:(-0.10875)][(180.00375):4500:(249.99375)]"; // [-179.99625:4500:-110.00625]
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        eddGrid.className() + "_LT0_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // from same request from cw erddap: erdVHNchlamday
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(89.77125):5500:(-0.10875)%5D%5B(-179.99625):4500:(-110.00625)%5D
        // but add 360 to the lon values to get the values below
        "time,altitude,latitude,longitude,chla\n" +
            "UTC,m,degrees_north,degrees_east,mg m^-3\n" +
            "2015-03-16T00:00:00Z,0.0,89.77125,180.00375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,89.77125,213.75375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,89.77125,247.50375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,48.52125,180.00375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,48.52125,213.75375,0.29340988\n" +
            "2015-03-16T00:00:00Z,0.0,48.52125,247.50375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,7.27125,180.00375,NaN\n" +
            "2015-03-16T00:00:00Z,0.0,7.27125,213.75375,0.08209898\n" +
            "2015-03-16T00:00:00Z,0.0,7.27125,247.50375,0.12582141\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon subset
    userDapQuery = "chla[(2015-03-16)][][(10):500:(0)][(200):2000:(240)]";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery, dir,
        eddGrid.className() + "_LT0_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(10):500:(0)%5D%5B(-160):2000:(-120)%5D
        // but add 360 to get lon values below
        "time,altitude,latitude,longitude,chla\n" +
            "UTC,m,degrees_north,degrees_east,mg m^-3\n" +
            "2015-03-16T00:00:00Z,0.0,10.00125,199.99875,0.03348998\n" +
            "2015-03-16T00:00:00Z,0.0,10.00125,214.99875,0.06315609\n" +
            "2015-03-16T00:00:00Z,0.0,10.00125,229.99875,0.06712352\n" +
            "2015-03-16T00:00:00Z,0.0,6.25125,199.99875,0.089674756\n" +
            "2015-03-16T00:00:00Z,0.0,6.25125,214.99875,0.12793668\n" +
            "2015-03-16T00:00:00Z,0.0,6.25125,229.99875,0.15159287\n" +
            "2015-03-16T00:00:00Z,0.0,2.50125,199.99875,0.16047283\n" +
            "2015-03-16T00:00:00Z,0.0,2.50125,214.99875,0.15874723\n" +
            "2015-03-16T00:00:00Z,0.0,2.50125,229.99875,0.12635218\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "chla[(2015-03-16)][][][]&.land=under";
    String baseName = eddGrid.className() + "_NEPacificNowLon0360";
    tName = eddGrid.makeNewFileForDapQuery(language, null, null, userDapQuery,
        Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR), baseName, ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

    // test of /files/ system for fromErddap in local host dataset
    String2.log("\n*** The following test requires test_erdVHNchlamday_Lon0360 in the localhost ERDDAP.");
    results = SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/files/test_erdVHNchlamday_Lon0360/");
    expected = "VHN2015060&#x5f;2015090&#x5f;chla&#x2e;nc";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // String2.log("\n*** EDDGridLon0360.testLT0() finished.");
  }

  /** EDDTableCopy */

  /**
   * The basic tests of this class (erdGlobecBottle).
   * 
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagImageComparison
  void testTableCopyBasic() throws Throwable {
    // testVerboseOn();
    int language = 0;

    String name, tName, results, tResults, expected, expected2, expected3, userDapQuery, tQuery;
    String tDir = EDStatic.fullTestCacheDirectory;
    String error = "";
    int epo, tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String mapDapQuery = "longitude,latitude,NO3,time&latitude>0&time>=2002-08-03";
    userDapQuery = "longitude,NO3,time,ship&latitude%3E0&time%3E=2002-08-03";

    EDDTable edd = (EDDTableCopy) EDDTestDataset.gettestTableCopy();

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDTableCopy.test das dds for entire dataset\n");
    tName = edd.makeNewFileForDapQuery(language, null, null, "",
        tDir, edd.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = // see OpendapHelper.EOL for comments
        "Attributes {\n" +
            " s {\n" +
            "  cruise_id {\n" +
            "    String cf_role \"trajectory_id\";\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Cruise ID\";\n" +
            "  }\n" +
            "  ship {\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Ship\";\n" +
            "  }\n" +
            "  cast {\n" +
            "    Int16 _FillValue 32767;\n" +
            "    Int16 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 140.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Identifier\";\n" +
            "    String long_name \"Cast Number\";\n" +
            "    Int16 missing_value 32767;\n" +
            "  }\n" +
            "  longitude {\n" +
            "    String _CoordinateAxisType \"Lon\";\n" +
            "    Float32 _FillValue 327.67;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    String axis \"X\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Longitude\";\n" +
            "    Float32 missing_value 327.67;\n" +
            "    String standard_name \"longitude\";\n" +
            "    String units \"degrees_east\";\n" +
            "  }\n" +
            "  latitude {\n" +
            "    String _CoordinateAxisType \"Lat\";\n" +
            "    Float32 _FillValue 327.67;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    String axis \"Y\";\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Latitude\";\n" +
            "    Float32 missing_value 327.67;\n" +
            "    String standard_name \"latitude\";\n" +
            "    String units \"degrees_north\";\n" +
            "  }\n" +
            "  time {\n" +
            "    String _CoordinateAxisType \"Time\";\n" +
            "    Float64 actual_range MIN, MAX;\n" +
            "    String axis \"T\";\n" +
            "    String cf_role \"profile_id\";\n" +
            "    String ioos_category \"Time\";\n" +
            "    String long_name \"Time\";\n" +
            "    String standard_name \"time\";\n" +
            "    String time_origin \"01-JAN-1970 00:00:00\";\n" +
            "    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
            "  }\n" +
            "  bottle_posn {\n" +
            "    String _CoordinateAxisType \"Height\";\n" +
            "    Byte _FillValue 127;\n" +
            "    String _Unsigned \"false\";\n" +
            "    Byte actual_range 0, 12;\n" +
            "    String axis \"Z\";\n" +
            "    Float64 colorBarMaximum 12.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Location\";\n" +
            "    String long_name \"Bottle Number\";\n" +
            "    Byte missing_value -128;\n" +
            "  }\n" +
            "  chl_a_total {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Chlorophyll-a\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  chl_a_10um {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Chlorophyll-a after passing 10um screen\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  phaeo_total {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Total Phaeopigments\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  phaeo_10um {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 30.0;\n" +
            "    Float64 colorBarMinimum 0.03;\n" +
            "    String colorBarScale \"Log\";\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Phaeopigments 10um\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"ug L-1\";\n" +
            "  }\n" +
            "  sal00 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 37.0;\n" +
            "    Float64 colorBarMinimum 32.0;\n" +
            "    String ioos_category \"Salinity\";\n" +
            "    String long_name \"Practical Salinity from T0 and C0 Sensors\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_practical_salinity\";\n" +
            "    String units \"PSU\";\n" +
            "  }\n" +
            "  sal11 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 37.0;\n" +
            "    Float64 colorBarMinimum 32.0;\n" +
            "    String ioos_category \"Salinity\";\n" +
            "    String long_name \"Practical Salinity from T1 and C1 Sensors\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_practical_salinity\";\n" +
            "    String units \"PSU\";\n" +
            "  }\n" +
            "  temperature0 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 32.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Temperature\";\n" +
            "    String long_name \"Sea Water Temperature from T0 Sensor\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_temperature\";\n" +
            "    String units \"degree_C\";\n" +
            "  }\n" +
            "  temperature1 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 32.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Temperature\";\n" +
            "    String long_name \"Sea Water Temperature from T1 Sensor\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"sea_water_temperature\";\n" +
            "    String units \"degree_C\";\n" +
            "  }\n" +
            "  fluor_v {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 5.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Fluorescence Voltage\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"volts\";\n" +
            "  }\n" +
            "  xmiss_v {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 5.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Optical Properties\";\n" +
            "    String long_name \"Transmissivity Voltage\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"volts\";\n" +
            "  }\n" +
            "  PO4 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 4.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Phosphate\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_phosphate_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  N_N {\n" +
            "    Float32 _FillValue -99.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 50.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Nitrate plus Nitrite\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  NO3 {\n" +
            "    Float32 _FillValue -99.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 50.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Nitrate\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_nitrate_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  Si {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 50.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Silicate\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_silicate_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  NO2 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 1.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Nitrite\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_nitrite_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  NH4 {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 5.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved Nutrients\";\n" +
            "    String long_name \"Ammonium\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"mole_concentration_of_ammonium_in_sea_water\";\n" +
            "    String units \"micromoles L-1\";\n" +
            "  }\n" +
            "  oxygen {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 10.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Dissolved O2\";\n" +
            "    String long_name \"Oxygen\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n" +
            "    String units \"mL L-1\";\n" +
            "  }\n" +
            "  par {\n" +
            "    Float32 _FillValue -9999999.0;\n" +
            "    Float32 actual_range MIN, MAX;\n" +
            "    Float64 colorBarMaximum 3.0;\n" +
            "    Float64 colorBarMinimum 0.0;\n" +
            "    String ioos_category \"Ocean Color\";\n" +
            "    String long_name \"Photosynthetically Active Radiation\";\n" +
            "    Float32 missing_value -9999.0;\n" +
            "    String units \"volts\";\n" +
            "  }\n" +
            " }\n" +
            "  NC_GLOBAL {\n" +
            "    String cdm_altitude_proxy \"bottle_posn\";\n" +
            "    String cdm_data_type \"TrajectoryProfile\";\n" +
            "    String cdm_profile_variables \"cast, longitude, latitude, time\";\n" +
            "    String cdm_trajectory_variables \"cruise_id, ship\";\n" +
            "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
            "    Float64 Easternmost_Easting -124.1;\n" +
            "    String featureType \"TrajectoryProfile\";\n" +
            "    Float64 geospatial_lat_max 44.65;\n" +
            "    Float64 geospatial_lat_min 41.9;\n" +
            "    String geospatial_lat_units \"degrees_north\";\n" +
            "    Float64 geospatial_lon_max -124.1;\n" +
            "    Float64 geospatial_lon_min -126.2;\n" +
            "    String geospatial_lon_units \"degrees_east\";\n";
    // " String history \"" + today + " 2012-07-29T19:11:09Z (local files; contact
    // erd.data@noaa.gov)\n"; //date is from last created file, so varies sometimes
    // today + "
    // https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecBottle.das"; //\n"
    // +
    // today + " https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
    // today + "
    // http://localhost:" + PORT + "/erddap/tabledap/rGlobecBottle.das\";\n" +
    expected2 = "    String infoUrl \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n" +
        "    String institution \"GLOBEC\";\n" +
        "    String keywords \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
        +
        "    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
        "    String license \"The data may be used and redistributed for free but is not intended\n" +
        "for legal use, since it may contain inaccuracies. Neither the data\n" +
        "Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
        "of their employees or contractors, makes any warranty, express or\n" +
        "implied, including warranties of merchantability and fitness for a\n" +
        "particular purpose, or assumes any legal liability for the accuracy,\n" +
        "completeness, or usefulness, of this information.\";\n" +
        "    Float64 Northernmost_Northing 44.65;\n" +
        "    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n" +
        "    Float64 Southernmost_Northing 41.9;\n" +
        "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n" +
        "    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n" +
        "    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n" +
        "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n" +
        "Notes:\n" +
        "Physical data processed by Jane Fleischbein (OSU).\n" +
        "Chlorophyll readings done by Leah Feinberg (OSU).\n" +
        "Nutrient analysis done by Burke Hales (OSU).\n" +
        "Sal00 - salinity calculated from primary sensors (C0,T0).\n" +
        "Sal11 - salinity calculated from secondary sensors (C1,T1).\n" +
        "secondary sensor pair was used in final processing of CTD data for\n" +
        "most stations because the primary had more noise and spikes. The\n" +
        "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n" +
        "multiple spikes or offsets in the secondary pair.\n" +
        "Nutrient samples were collected from most bottles; all nutrient data\n" +
        "developed from samples frozen during the cruise and analyzed ashore;\n" +
        "data developed by Burke Hales (OSU).\n" +
        "Operation Detection Limits for Nutrient Concentrations\n" +
        "Nutrient  Range         Mean    Variable         Units\n" +
        "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n" +
        "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n" +
        "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n" +
        "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n" +
        "Dates and Times are UTC.\n" +
        "\n" +
        "For more information, see https://www.bco-dmo.org/dataset/2452\n" +
        "\n" +
        "Inquiries about how to access this data should be directed to\n" +
        "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n" +
        "    String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ\";\n" +
        "    String time_coverage_start \"2002-05-30T03:21:00Z\";\n" +
        "    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n" +
        "    Float64 Westernmost_Easting -126.2;\n" +
        "  }\n" +
        "}\n";

    results = results.replaceAll("Int16 actual_range [0-9]+, [0-9]+;", "Int16 actual_range MIN, MAX;");
    results = results.replaceAll("Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
        "Float32 actual_range MIN, MAX;");
    results = results.replaceAll("Float64 actual_range -?[0-9].[0-9]+e[+][0-9], -?[0-9].[0-9]+e[+][0-9];",
        "Float64 actual_range MIN, MAX;");
    results = results.replaceAll("String time_coverage_end \\\"....-..-..T..:..:..Z",
        "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf("    String infoUrl ");
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName = edd.makeNewFileForDapQuery(language, null, null, "", tDir,
        edd.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    String cruise_id;\n" +
        "    String ship;\n" +
        "    Int16 cast;\n" +
        "    Float32 longitude;\n" +
        "    Float32 latitude;\n" +
        "    Float64 time;\n" +
        "    Byte bottle_posn;\n" +
        "    Float32 chl_a_total;\n" +
        "    Float32 chl_a_10um;\n" +
        "    Float32 phaeo_total;\n" +
        "    Float32 phaeo_10um;\n" +
        "    Float32 sal00;\n" +
        "    Float32 sal11;\n" +
        "    Float32 temperature0;\n" +
        "    Float32 temperature1;\n" +
        "    Float32 fluor_v;\n" +
        "    Float32 xmiss_v;\n" +
        "    Float32 PO4;\n" +
        "    Float32 N_N;\n" +
        "    Float32 NO3;\n" +
        "    Float32 Si;\n" +
        "    Float32 NO2;\n" +
        "    Float32 NH4;\n" +
        "    Float32 oxygen;\n" +
        "    Float32 par;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName = edd.makeNewFileForDapQuery(language, null, null, "", tDir,
        edd.className() + "_Entire", ".html");
    results = File2.directReadFromUtf8File(tDir + tName);
    expected = "<option>.png - View a standard, medium-sized .png image file with a graph or map.";
    expected2 = "    String _CoordinateAxisType &quot;Lon&quot;;";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    // Test.displayInBrowser("file://" + tDir + tName);

    // *** test make data files
    String2.log("\n****************** EDDTableCopy.test make DATA FILES\n");

    // .asc
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
        edd.className() + "_Data", ".asc");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    Float32 longitude;\n" +
        "    Float32 NO3;\n" +
        "    Float64 time;\n" +
        "    String ship;\n" +
        "  } s;\n" +
        "} s;\n" +
        "---------------------------------------------\n" +
        "s.longitude, s.NO3, s.time, s.ship\n" +
        "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"\n";
    expected2 = "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"\n"; // row with missing value has source missing
                                                                    // value
    expected3 = "-124.57, 19.31, 1.02939792E9, \"New_Horizon\"\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected3) > 0, "\nresults=\n" + results); // last row in erdGlobedBottle, not
                                                                               // last
                                                                               // here

    // .csv
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
        edd.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "longitude,NO3,time,ship\n" +
        "degrees_east,micromoles L-1,UTC,\n" +
        "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n" +
        "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    expected2 = "-124.8,NaN,2002-08-03T07:17:00Z,New_Horizon\n"; // row with missing value has source missing value
    expected3 = "-124.57,19.31,2002-08-15T07:52:00Z,New_Horizon\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected3) > 0, "\nresults=\n" + results); // last row in erdGlobedBottle, not
                                                                               // last
                                                                               // here

    // .dds
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
        edd.className() + "_Data", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = "Dataset {\n" +
        "  Sequence {\n" +
        "    Float32 longitude;\n" +
        "    Float32 NO3;\n" +
        "    Float64 time;\n" +
        "    String ship;\n" +
        "  } s;\n" +
        "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dods
    // tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
    // edd.className() + "_Data", ".dods");
    // Test.displayInBrowser("file://" + tDir + tName);
    String2.log("\ndo .dods test");
    String tUrl = EDStatic.erddapUrl + // in tests, always use non-https url
        "/tabledap/" + edd.datasetID();
    // for diagnosing during development:
    // String2.log(String2.annotatedString(SSR.getUrlResponseStringUnchanged(
    // "https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt.dods?stn_id&unique()")));
    // String2.log("\nDAS RESPONSE=" + SSR.getUrlResponseStringUnchanged(tUrl +
    // ".das?" + userDapQuery));
    // String2.log("\nDODS RESPONSE=" +
    // String2.annotatedString(SSR.getUrlResponseStringUnchanged(tUrl + ".dods?" +
    // userDapQuery)));

    // test if table.readOpendapSequence works with Erddap opendap server
    // !!!THIS READS DATA FROM ERDDAP SERVER RUNNING ON EDStatic.erddapUrl!!! //in
    // tests, always use non-https url
    // !!!THIS IS NOT JUST A LOCAL TEST!!!
    Table tTable = new Table();
    tTable.readOpendapSequence(tUrl + "?" + userDapQuery, false);
    Test.ensureEqual(tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
    Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
    Test.ensureEqual(tTable.getColumnNames(), new String[] { "longitude", "NO3", "time", "ship" }, "");
    Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
    Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
    Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
    Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
    String2.log("  .dods test succeeded");

    // test .png
    String baseName = edd.className() + "_GraphM";
    tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery,
        Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR), baseName, ".png");
    // Test.displayInBrowser("file://" + tDir + tName);
    Image2Tests.testImagesIdentical(
        tName,
        baseName + ".png",
        baseName + "_diff.png");

  } // end of testBasic

  /**
   * This tests the /files/ "files" system.
   * This requires testTableCopy in the localhost ERDDAP.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testTableCopyFiles() throws Throwable {

    String2.log("\n*** EDDTableCopy.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/.csv");
    expected = "Name,Last modified,Size,Description\n" +
        "nh0207/,NaN,NaN,\n" +
        "w0205/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/");
    Test.ensureTrue(results.indexOf("nh0207&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("nh0207/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("w0205&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("w0205/") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/.csv");
    expected = "Name,lastModTime,Size,Description\n" +
        "1.nc,lastModTime,14384,\n" +
        "10.nc,lastModTime,15040,\n" +
        "100.nc,lastModTime,14712,\n";
    results = results.replaceAll(",.............,", ",lastModTime,");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // download a file in root

    // download a file in subdir
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/100.nc").substring(0, 50));
    expected = "CDF[1][0][0][0][0][0][0][0][10]\n" +
        "[0][0][0][3][0][0][0][3]row[0][0][0][0][6][0][0][0][16]cruise_id_strlen[0][0][end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/testTableCopy/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/testTableCopy/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT
        + "/erddap/files/testTableCopy/nh0207/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** EDDGridFromNcFiles */

  /**
   * This test the speed of all types of responses.
   * This test is in this class because the source data is in a file,
   * so it has reliable access speed.
   * This gets a pretty big chunk of data.
   *
   * @param firstTest 0..
   * @param lastTest  (inclusive) Any number greater than last available is
   *                  interpreted as last available.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testSpeed() throws Throwable {
    int firstTest = 0;
    int lastTest = 1000;
    // String2.log("\n*** EDDGridFromNcFiles.testSpeed\n" +
    // "THIS REQUIRES THE testGriddedNcFiles DATASET TO BE IN LOCALHOST ERDDAP!!!\n"
    // +
    // SgtUtil.isBufferedImageAccelerated() + "\n");
    int language = 0;
    // gc and sleep to give computer time to catch up from previous tests
    for (int i = 0; i < 4; i++)
      Math2.gc("EDDGridFromNcFiles.testSpeed (between tests)", 5000);
    // boolean oReallyVerbose = reallyVerbose;
    // reallyVerbose = false;
    String outName;
    // 2017-10-13 I switched from getFile to curl
    // The advantage is: curl will detect if outputstream isn't being closed.
    // 2018-05-17 problems with curl, switched to SSR.downloadFile
    String baseRequest = "http://localhost:" + PORT +"/erddap/griddap/testGriddedNcFiles";
    String userDapQuery = "?" + SSR.minimalPercentEncode("y_wind[(2008-01-07T12:00:00Z)][0][][0:719]") + // 719
                                                                                                         // avoids
                                                                                                         // esriAsc
                                                                                                         // cross
                                                                                                         // lon=180
        "&.vec="; // avoid get cached response
    String baseName = "EDDGridFromNcFilesTestSpeed";
    String baseOut = EDStatic.fullTestCacheDirectory + baseName;
    ArrayList al;
    int timeOutSeconds = 120;
    String extensions[] = new String[] {
        ".asc", ".csv", ".csvp", ".csv0",
        ".das", ".dds", ".dods", ".esriAscii",
        ".graph", ".html", ".htmlTable", // .help not available at this level
        ".json", ".jsonlCSV", ".jsonlCSV1", ".jsonlKVP", ".mat",
        ".nc", ".ncHeader",
        ".nccsv", ".nccsvMetadata", ".ncoJson",
        ".odvTxt", ".timeGaps",
        ".tsv", ".tsvp", ".tsv0",
        ".xhtml",
        ".geotif", ".kml",
        ".smallPdf", ".pdf", ".largePdf",
        ".smallPng", ".png", ".largePng",
        ".transparentPng" };
    int expectedMs[] = new int[] {
        // 2017-10-13 I added 200 ms with change from getFile to curl
        // 2018-05-17 I adjusted (e.g., small files 200ms faster) with switch to
        // SSR.downloadFile
        // now Lenovo was Java 1.8/M4700 //was Java 1.6 times //was java 1.5 times
        550, 1759, 1635, 1544, // 734, 6391, 6312, ? //1250, 9750, 9562, ?
        // why is esriAscii so slow??? was ~9000 for a long time. Then jumped to 23330.
        15, 15, 663, 12392, // 15, 15, 109/156, 16875 //15, 15, 547, 18859
        40, 25, 477, // 63, 47, 2032, //93, 31, ...,
        1843, 1568, 1568, 2203, 666, // 6422, ., ., 203, //9621, ., ., 625,
        173, 117, // 234, 250, //500, 500,
        3485, 16, 390,
        2446, 13, // 9547, ? //13278, ?
        1411, 1411, 1411, // 6297, 6281, ?, //8766, 8844, ?,
        2204, // but really slow if hard drive is busy! //8625, //11469,
        500, 20, // 656, 110, //687, 94, //Java 1.7 was 390r until change to new netcdf-Java
        266, 976, 1178, // 860, 2859, 3438, //2188, 4063, 3797, //small varies greatly
        131, 209, 459, // 438, 468, 1063, //438, 469, 1188, //small varies greatly
        720 }; // 1703 //2359};
    int bytes[] = new int[] {
        5875592, 23734053, 23734063, 23733974,
        6006, 303, 2085486, 4701074,
        60787, 60196, 11980027,
        31827797, 28198736, 28198830, 54118736, 2085800,
        2090600, 5285,
        25961828, 5244, 5877820,
        26929890, 58,
        23734053, 23734063, 23733974,
        69372795,
        523113, 3601,
        478774, 2189656, 2904880,
        33930, 76777, 277494, // small png flips between 26906 and 30764, updated to 33930
        335307 };

    // warm up
    boolean tryToCompress = true;
    outName = baseOut + "Warmup.csvp.csv";
    SSR.downloadFile(baseRequest + ".csvp" + userDapQuery + Math2.random(1000), outName, tryToCompress);
    // was al = SSR.dosShell(baseRequest + ".csvp" + userDapQuery +
    // Math2.random(1000) +
    // " -o " + outName, timeOutSeconds);
    // String2.log(String2.toNewlineString(al.toArray()));

    outName = baseOut + "Warmup.png.png";
    SSR.downloadFile(baseRequest + ".png" + userDapQuery + Math2.random(1000), outName, tryToCompress);
    // al = SSR.dosShell(baseRequest + ".png" + userDapQuery + Math2.random(1000) +
    // " -o " + outName, timeOutSeconds);

    outName = baseOut + "Warmup.pdf.pdf";
    SSR.downloadFile(baseRequest + ".pdf" + userDapQuery + Math2.random(1000), outName, tryToCompress);
    // al = SSR.dosShell(baseRequest + ".pdf" + userDapQuery + Math2.random(1000) +
    // " -o " + outName, timeOutSeconds);

    lastTest = Math.min(lastTest, extensions.length - 1);
    for (int ext = firstTest; ext <= lastTest; ext++) {
      // String2.pressEnterToContinue("");
      // Math2.sleep(3000);
      String dotExt = extensions[ext];
      // try {
      String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + ": " +
          dotExt + " speed\n");
      long time = 0, cLength = 0;
      int chance = 0;
      // for (int chance = 0; chance < 3; chance++) {
        Math2.gcAndWait("EDDGridFromNcFiles (between tests)"); // in a test
        time = System.currentTimeMillis();
        outName = baseOut + chance + dotExt;
        SSR.downloadFile(baseRequest + dotExt + userDapQuery + Math2.random(1000), outName, tryToCompress);
        // al = SSR.dosShell(baseRequest + dotExt + userDapQuery + Math2.random(1000) +
        // " -o " + outName, timeOutSeconds);

        time = System.currentTimeMillis() - time;
        cLength = File2.length(outName);
        String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext +
            " chance#" + chance + ": " + dotExt + " done.\n  " +
            cLength + " bytes (" + bytes[ext] +
            ").  time=" + time + "ms (expected=" + expectedMs[ext] + ")\n");
        // Math2.sleep(3000);

        // if not too slow or too fast, break
        // if (time > 1.5 * Math.max(50, expectedMs[ext]) ||
        //     time < (expectedMs[ext] <= 50 ? 0 : 0.5) * expectedMs[ext]) {
        //   // give it another chance
        // } else {
          // break;
        // }
      // }

      // size test
      Test.ensureTrue(cLength > 0.9 * bytes[ext],
          "File shorter than expected.  observed=" +
              cLength + " expected=~" + bytes[ext] +
              "\n" + outName);
      Test.ensureTrue(cLength < 1.1 * bytes[ext],
          "File longer than expected.  observed=" +
              cLength + " expected=~" + bytes[ext] +
              "\n" + outName);

      // time test
      // TODO check performance in a better way
      // if (time > 1.5 * Math.max(50, expectedMs[ext]))
      //   throw new SimpleException(
      //       "Slower than expected. observed=" + time +
      //           " expected=~" + expectedMs[ext] + " ms.");
      // if (expectedMs[ext] >= 50 && time < 0.5 * expectedMs[ext])
      //   throw new SimpleException(
      //       "Faster than expected! observed=" + time +
      //           " expected=~" + expectedMs[ext] + " ms.");

      // display last image
      if (ext == extensions.length - 1) {
        File2.rename(outName, outName + ".png");
        // Test.displayInBrowser(outName + ".png"); // complicated to switch to testImagesIdentical
      }

      // } catch (Exception e) {
      // String2.pressEnterToContinue(MustBe.throwableToString(e) +
      // "Unexpected error for Test#" + ext + ": " + dotExt + ".");
      // }
    }
    // reallyVerbose = oReallyVerbose;
  }

  /**
   * This tests the /files/ "files" system.
   * This requires nceiPH53sstn1day in the local ERDDAP.
   *
   * EDDGridFromNcFiles.testFiles() has more tests than any other testFiles().
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGridFromNcFiles() throws Throwable {

    String2.log("\n*** EDDGridFromNcFiles.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int language = 0;
    int po;

    // get /files/.csv
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/.csv"));
    Test.ensureTrue(results.indexOf("Name,Last modified,Size,Description[10]") == 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(
        "nceiPH53sstn1day/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Nighttime (1 Day Composite)\"[10]") > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("testTableAscii/,NaN,NaN,The Title for testTableAscii[10]") > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("documentation.html,") > 0, "results=\n" + results);

    // get /files/datasetID/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/.csv");
    expected = "Name,Last modified,Size,Description\n" +
        "1981/,NaN,NaN,\n" +
        "1994/,NaN,NaN,\n" +
        "2020/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/");
    Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1981/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

    // get /files/datasetID //missing trailing slash will be redirected
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day");
    Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1981/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/1994/.csv");
    expected = "Name,Last modified,Size,Description\n" +
        "data/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/subdir/subdir.csv
    results = SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/1994/data/.csv");
    results = results.replaceAll(",.............,", ",lastMod,");
    expected = "Name,lastMod,Size,Description\n" +
        "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc,lastMod,12484412,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // download a file in root -- none available

    // download a file in subdir
    results = String2.annotatedString(SSR.getUrlResponseStringNewline(
        "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/1994/data/" +
            "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc")
        .substring(0, 50));
    expected = "[137]HDF[10]\n" +
        "[26][10]\n" +
        "[2][8][8][0][0][0][0][0][0][0][0][0][255][255][255][255][255][255][255][255]<[127][190][0][0][0][0][0]0[0][0][0][0][0][0][0][199](*yOHD[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query with // at start fails
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/erddap/files//.csv\n" + //
                "(<html>\n" + //
                "<head>\n" + //
                "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=ISO-8859-1\"/>\n" + //
                "<title>Error 400 Ambiguous URI empty segment</title>\n" + //
                "</head>\n" + //
                "<body>\n" + //
                "<h2>HTTP ERROR 400 Ambiguous URI empty segment</h2>\n" + //
                "<table>\n" + //
                "<tr><th>URI:</th><td>/badURI</td></tr>\n" + //
                "<tr><th>STATUS:</th><td>400</td></tr>\n" + //
                "<tr><th>MESSAGE:</th><td>Ambiguous URI empty segment</td></tr>\n" + //
                "</table>\n" + //
                "<hr/><a href=\"https://eclipse.org/jetty\">Powered by Jetty:// 12.0.8</a><hr/>\n" + //
                "\n" + //
                "</body>\n" + //
                "</html>)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query with // later fails
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:8080/erddap/files/nceiPH53sstn1day//.csv\n" + //
                "(<html>\n" + //
                "<head>\n" + //
                "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=ISO-8859-1\"/>\n" + //
                "<title>Error 400 Ambiguous URI empty segment</title>\n" + //
                "</head>\n" + //
                "<body>\n" + //
                "<h2>HTTP ERROR 400 Ambiguous URI empty segment</h2>\n" + //
                "<table>\n" + //
                "<tr><th>URI:</th><td>/badURI</td></tr>\n" + //
                "<tr><th>STATUS:</th><td>400</td></tr>\n" + //
                "<tr><th>MESSAGE:</th><td>Ambiguous URI empty segment</td></tr>\n" + //
                "</table>\n" + //
                "<hr/><a href=\"https://eclipse.org/jetty\">Powered by Jetty:// 12.0.8</a><hr/>\n" + //
                "\n" + //
                "</body>\n" + //
                "</html>)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query with /../ fails
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/../");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=400 for URL: http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/../\n"
        +
        "(Error {\n" +
        "    code=400;\n" +
        "    message=\"Bad Request: Query error: /../ is not allowed!\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent datasetID
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a existent subdirectory but without trailing slash
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/GLsubdir");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/GLsubdir\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: GLsubdir .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/gibberish/\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: Resource not found: directory=gibberish/\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in root
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existent subdir
    try {
      results = SSR.getUrlResponseStringNewline(
          "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/GLsubdir/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/GLsubdir/gibberish.csv\n"
        +
        "(Error {\n" +
        "    code=404;\n" +
        "    message=\"Not Found: File not found: gibberish.csv .\";\n" +
        "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }
}
