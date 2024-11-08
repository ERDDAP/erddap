package jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import gov.noaa.pfel.erddap.dataset.EDDGridFromEtopo;
import gov.noaa.pfel.erddap.dataset.EDDGridLon0360;
import gov.noaa.pfel.erddap.dataset.EDDGridLonPM180;
import gov.noaa.pfel.erddap.dataset.EDDGridSideBySide;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableCopy;
import gov.noaa.pfel.erddap.dataset.EDDTableFromAsciiFiles;
import gov.noaa.pfel.erddap.dataset.EDDTableFromDapSequence;
import gov.noaa.pfel.erddap.dataset.EDDTableFromEDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTableFromErddap;
import gov.noaa.pfel.erddap.dataset.EDDTableFromNcFiles;
import gov.noaa.pfel.erddap.handlers.SaxHandler;
import gov.noaa.pfel.erddap.handlers.SaxParsingContext;
import gov.noaa.pfel.erddap.handlers.TopLevelHandler;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tags.TagFlaky;
import tags.TagImageComparison;
import tags.TagJetty;
import tags.TagThredds;
import testDataset.EDDTestDataset;
import testDataset.Initialization;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

@TagJetty
class JettyTests {

  @TempDir private static Path TEMP_DIR;

  private static Server server;
  private static Integer PORT = 8080;

  @BeforeAll
  public static void setUp() throws Throwable {
    Initialization.edStatic();
    EDDTestDataset.generateDatasetsXml();

    server = new Server(PORT);

    WebAppContext context = new WebAppContext();
    ResourceFactory resourceFactory = ResourceFactory.of(context);
    Resource baseResource =
        resourceFactory.newResource(
            Path.of(System.getProperty("user.dir")).toAbsolutePath().toUri());
    context.setBaseResource(baseResource);
    context.setContextPath("/");
    context.setParentLoaderPriority(true);
    server.setHandler(context);

    server.start();

    // Delay the tests to give the server a chance to load all of the data.
    // If the cache/data folder is cold some machines might need longer. If
    // all of the data is already loaded on the machine, this can probably be
    // shortened.
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
    String results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:"
                + PORT
                + "/erddap/metadata/iso19115/xml/erdMH1chla1day_iso19115.xml");
    String expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<gmi:MI_Metadata  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + //
            "  xsi:schemaLocation=\"https://www.isotc211.org/2005/gmi https://data.noaa.gov/resources/iso19139/schema.xsd\"\n"
            + //
            "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
            + //
            "  xmlns:gco=\"http://www.isotc211.org/2005/gco\"\n"
            + //
            "  xmlns:gmd=\"http://www.isotc211.org/2005/gmd\"\n"
            + //
            "  xmlns:gmx=\"http://www.isotc211.org/2005/gmx\"\n"
            + //
            "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + //
            "  xmlns:gss=\"http://www.isotc211.org/2005/gss\"\n"
            + //
            "  xmlns:gts=\"http://www.isotc211.org/2005/gts\"\n"
            + //
            "  xmlns:gsr=\"http://www.isotc211.org/2005/gsr\"\n"
            + //
            "  xmlns:gmi=\"http://www.isotc211.org/2005/gmi\"\n"
            + //
            "  xmlns:srv=\"http://www.isotc211.org/2005/srv\">\n"
            + //
            "  <gmd:fileIdentifier>\n"
            + //
            "    <gco:CharacterString>erdMH1chla1day</gco:CharacterString>\n"
            + //
            "  </gmd:fileIdentifier>\n"
            + //
            "  <gmd:language>\n"
            + //
            "    <gmd:LanguageCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:LanguageCode\" codeListValue=\"eng\">eng</gmd:LanguageCode>\n"
            + //
            "  </gmd:language>\n"
            + //
            "  <gmd:characterSet>\n"
            + //
            "    <gmd:MD_CharacterSetCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CharacterSetCode\" codeListValue=\"UTF8\">UTF8</gmd:MD_CharacterSetCode>\n"
            + //
            "  </gmd:characterSet>\n"
            + //
            "  <gmd:hierarchyLevel>\n"
            + //
            "    <gmd:MD_ScopeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n"
            + //
            "  </gmd:hierarchyLevel>\n"
            + //
            "  <gmd:hierarchyLevel>\n"
            + //
            "    <gmd:MD_ScopeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"service\">service</gmd:MD_ScopeCode>\n"
            + //
            "  </gmd:hierarchyLevel>\n"
            + //
            "  <gmd:contact>\n"
            + //
            "    <gmd:CI_ResponsibleParty>\n"
            + //
            "      <gmd:individualName>\n"
            + //
            "        <gco:CharacterString>ERDDAP Jetty Developer</gco:CharacterString>\n"
            + //
            "      </gmd:individualName>\n"
            + //
            "      <gmd:organisationName>\n"
            + //
            "        <gco:CharacterString>ERDDAP Jetty Install</gco:CharacterString>\n"
            + //
            "      </gmd:organisationName>\n"
            + //
            "      <gmd:contactInfo>\n"
            + //
            "        <gmd:CI_Contact>\n"
            + //
            "          <gmd:phone>\n"
            + //
            "            <gmd:CI_Telephone>\n"
            + //
            "              <gmd:voice>\n"
            + //
            "                <gco:CharacterString>555-555-5555</gco:CharacterString>\n"
            + //
            "              </gmd:voice>\n"
            + //
            "            </gmd:CI_Telephone>\n"
            + //
            "          </gmd:phone>\n"
            + //
            "          <gmd:address>\n"
            + //
            "            <gmd:CI_Address>\n"
            + //
            "              <gmd:deliveryPoint>\n"
            + //
            "                <gco:CharacterString>123 Irrelevant St.</gco:CharacterString>\n"
            + //
            "              </gmd:deliveryPoint>\n"
            + //
            "              <gmd:city>\n"
            + //
            "                <gco:CharacterString>Nowhere</gco:CharacterString>\n"
            + //
            "              </gmd:city>\n"
            + //
            "              <gmd:administrativeArea>\n"
            + //
            "                <gco:CharacterString>AK</gco:CharacterString>\n"
            + //
            "              </gmd:administrativeArea>\n"
            + //
            "              <gmd:postalCode>\n"
            + //
            "                <gco:CharacterString>99504</gco:CharacterString>\n"
            + //
            "              </gmd:postalCode>\n"
            + //
            "              <gmd:country>\n"
            + //
            "                <gco:CharacterString>USA</gco:CharacterString>\n"
            + //
            "              </gmd:country>\n"
            + //
            "              <gmd:electronicMailAddress>\n"
            + //
            "                <gco:CharacterString>nobody@example.com</gco:CharacterString>\n"
            + //
            "              </gmd:electronicMailAddress>\n"
            + //
            "            </gmd:CI_Address>\n"
            + //
            "          </gmd:address>\n"
            + //
            "        </gmd:CI_Contact>\n"
            + //
            "      </gmd:contactInfo>\n"
            + //
            "      <gmd:role>\n"
            + //
            "        <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n"
            + //
            "      </gmd:role>\n"
            + //
            "    </gmd:CI_ResponsibleParty>\n"
            + //
            "  </gmd:contact>\n"
            + //
            "  <gmd:dateStamp>\n"
            + //
            "    <gco:Date>YYYY-MM-DD</gco:Date>\n"
            + //
            "  </gmd:dateStamp>\n"
            + //
            "  <gmd:metadataStandardName>\n"
            + //
            "    <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for Imagery and Gridded Data</gco:CharacterString>\n"
            + //
            "  </gmd:metadataStandardName>\n"
            + //
            "  <gmd:metadataStandardVersion>\n"
            + //
            "    <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>\n"
            + //
            "  </gmd:metadataStandardVersion>\n"
            + //
            "  <gmd:spatialRepresentationInfo>\n"
            + //
            "    <gmd:MD_GridSpatialRepresentation>\n"
            + //
            "      <gmd:numberOfDimensions>\n"
            + //
            "        <gco:Integer>NUMBER</gco:Integer>\n"
            + //
            "      </gmd:numberOfDimensions>\n"
            + //
            "      <gmd:axisDimensionProperties>\n"
            + //
            "        <gmd:MD_Dimension>\n"
            + //
            "          <gmd:dimensionName>\n"
            + //
            "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"column\">column</gmd:MD_DimensionNameTypeCode>\n"
            + //
            "          </gmd:dimensionName>\n"
            + //
            "          <gmd:dimensionSize>\n"
            + //
            "            <gco:Integer>NUMBER</gco:Integer>\n"
            + //
            "          </gmd:dimensionSize>\n"
            + //
            "          <gmd:resolution>\n"
            + //
            "            <gco:Measure uom=\"deg&#x7b;east&#x7d;\">measureValue</gco:Measure>\n"
            + //
            "          </gmd:resolution>\n"
            + //
            "        </gmd:MD_Dimension>\n"
            + //
            "      </gmd:axisDimensionProperties>\n"
            + //
            "      <gmd:axisDimensionProperties>\n"
            + //
            "        <gmd:MD_Dimension>\n"
            + //
            "          <gmd:dimensionName>\n"
            + //
            "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"row\">row</gmd:MD_DimensionNameTypeCode>\n"
            + //
            "          </gmd:dimensionName>\n"
            + //
            "          <gmd:dimensionSize>\n"
            + //
            "            <gco:Integer>NUMBER</gco:Integer>\n"
            + //
            "          </gmd:dimensionSize>\n"
            + //
            "          <gmd:resolution>\n"
            + //
            "            <gco:Measure uom=\"deg&#x7b;north&#x7d;\">measureValue</gco:Measure>\n"
            + //
            "          </gmd:resolution>\n"
            + //
            "        </gmd:MD_Dimension>\n"
            + //
            "      </gmd:axisDimensionProperties>\n"
            + //
            "      <gmd:axisDimensionProperties>\n"
            + //
            "        <gmd:MD_Dimension>\n"
            + //
            "          <gmd:dimensionName>\n"
            + //
            "            <gmd:MD_DimensionNameTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" codeListValue=\"temporal\">temporal</gmd:MD_DimensionNameTypeCode>\n"
            + //
            "          </gmd:dimensionName>\n"
            + //
            "          <gmd:dimensionSize>\n"
            + //
            "            <gco:Integer>NUMBER</gco:Integer>\n"
            + //
            "          </gmd:dimensionSize>\n"
            + //
            "          <gmd:resolution>\n"
            + //
            "            <gco:Measure uom=\"s\">VALUE</gco:Measure>\n"
            + //
            "          </gmd:resolution>\n"
            + //
            "        </gmd:MD_Dimension>\n"
            + //
            "      </gmd:axisDimensionProperties>\n"
            + //
            "      <gmd:cellGeometry>\n"
            + //
            "        <gmd:MD_CellGeometryCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CellGeometryCode\" codeListValue=\"area\">area</gmd:MD_CellGeometryCode>\n"
            + //
            "      </gmd:cellGeometry>\n"
            + //
            "      <gmd:transformationParameterAvailability gco:nilReason=\"unknown\"/>\n"
            + //
            "    </gmd:MD_GridSpatialRepresentation>\n"
            + //
            "  </gmd:spatialRepresentationInfo>\n"
            + //
            "  <gmd:identificationInfo>\n"
            + //
            "    <gmd:MD_DataIdentification id=\"DataIdentification\">\n"
            + //
            "      <gmd:citation>\n"
            + //
            "        <gmd:CI_Citation>\n"
            + //
            "          <gmd:title>\n"
            + //
            "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</gco:CharacterString>\n"
            + //
            "          </gmd:title>\n"
            + //
            "          <gmd:date>\n"
            + //
            "            <gmd:CI_Date>\n"
            + //
            "              <gmd:date>\n"
            + //
            "                <gco:Date>YYYY-MM-DD</gco:Date>\n"
            + //
            "              </gmd:date>\n"
            + //
            "              <gmd:dateType>\n"
            + //
            "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
            + //
            "              </gmd:dateType>\n"
            + //
            "            </gmd:CI_Date>\n"
            + //
            "          </gmd:date>\n"
            + //
            "          <gmd:identifier>\n"
            + //
            "            <gmd:MD_Identifier>\n"
            + //
            "              <gmd:authority>\n"
            + //
            "                <gmd:CI_Citation>\n"
            + //
            "                  <gmd:title>\n"
            + //
            "                    <gco:CharacterString>localhost:8080</gco:CharacterString>\n"
            + //
            "                  </gmd:title>\n"
            + //
            "                  <gmd:date gco:nilReason=\"inapplicable\"/>\n"
            + //
            "                </gmd:CI_Citation>\n"
            + //
            "              </gmd:authority>\n"
            + //
            "              <gmd:code>\n"
            + //
            "                <gco:CharacterString>erdMH1chla1day</gco:CharacterString>\n"
            + //
            "              </gmd:code>\n"
            + //
            "            </gmd:MD_Identifier>\n"
            + //
            "          </gmd:identifier>\n"
            + //
            "          <gmd:citedResponsibleParty>\n"
            + //
            "            <gmd:CI_ResponsibleParty>\n"
            + //
            "              <gmd:individualName gco:nilReason=\"missing\"/>\n"
            + //
            "              <gmd:organisationName>\n"
            + //
            "                <gco:CharacterString>NASA/GSFC/OBPG</gco:CharacterString>\n"
            + //
            "              </gmd:organisationName>\n"
            + //
            "              <gmd:contactInfo>\n"
            + //
            "                <gmd:CI_Contact>\n"
            + //
            "                  <gmd:address>\n"
            + //
            "                    <gmd:CI_Address>\n"
            + //
            "                      <gmd:electronicMailAddress>\n"
            + //
            "                        <gco:CharacterString>data@oceancolor.gsfc.nasa.gov</gco:CharacterString>\n"
            + //
            "                      </gmd:electronicMailAddress>\n"
            + //
            "                    </gmd:CI_Address>\n"
            + //
            "                  </gmd:address>\n"
            + //
            "                  <gmd:onlineResource>\n"
            + //
            "                    <gmd:CI_OnlineResource>\n"
            + //
            "                      <gmd:linkage>\n"
            + //
            "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</gmd:URL>\n"
            + //
            "                      </gmd:linkage>\n"
            + //
            "                      <gmd:protocol>\n"
            + //
            "                        <gco:CharacterString>information</gco:CharacterString>\n"
            + //
            "                      </gmd:protocol>\n"
            + //
            "                      <gmd:applicationProfile>\n"
            + //
            "                        <gco:CharacterString>web browser</gco:CharacterString>\n"
            + //
            "                      </gmd:applicationProfile>\n"
            + //
            "                      <gmd:name>\n"
            + //
            "                        <gco:CharacterString>Background Information</gco:CharacterString>\n"
            + //
            "                      </gmd:name>\n"
            + //
            "                      <gmd:description>\n"
            + //
            "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
            + //
            "                      </gmd:description>\n"
            + //
            "                      <gmd:function>\n"
            + //
            "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                      </gmd:function>\n"
            + //
            "                    </gmd:CI_OnlineResource>\n"
            + //
            "                  </gmd:onlineResource>\n"
            + //
            "                </gmd:CI_Contact>\n"
            + //
            "              </gmd:contactInfo>\n"
            + //
            "              <gmd:role>\n"
            + //
            "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
            + //
            "              </gmd:role>\n"
            + //
            "            </gmd:CI_ResponsibleParty>\n"
            + //
            "          </gmd:citedResponsibleParty>\n"
            + //
            "        </gmd:CI_Citation>\n"
            + //
            "      </gmd:citation>\n"
            + //
            "      <gmd:abstract>\n"
            + //
            "        <gco:CharacterString>This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.</gco:CharacterString>\n"
            + //
            "      </gmd:abstract>\n"
            + //
            "      <gmd:credit gco:nilReason=\"missing\"/>\n"
            + //
            "      <gmd:pointOfContact>\n"
            + //
            "        <gmd:CI_ResponsibleParty>\n"
            + //
            "          <gmd:individualName gco:nilReason=\"missing\"/>\n"
            + //
            "          <gmd:organisationName>\n"
            + //
            "            <gco:CharacterString>NASA/GSFC/OBPG</gco:CharacterString>\n"
            + //
            "          </gmd:organisationName>\n"
            + //
            "          <gmd:contactInfo>\n"
            + //
            "            <gmd:CI_Contact>\n"
            + //
            "              <gmd:address>\n"
            + //
            "                <gmd:CI_Address>\n"
            + //
            "                  <gmd:electronicMailAddress>\n"
            + //
            "                    <gco:CharacterString>data@oceancolor.gsfc.nasa.gov</gco:CharacterString>\n"
            + //
            "                  </gmd:electronicMailAddress>\n"
            + //
            "                </gmd:CI_Address>\n"
            + //
            "              </gmd:address>\n"
            + //
            "              <gmd:onlineResource>\n"
            + //
            "                <gmd:CI_OnlineResource>\n"
            + //
            "                  <gmd:linkage>\n"
            + //
            "                    <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</gmd:URL>\n"
            + //
            "                  </gmd:linkage>\n"
            + //
            "                  <gmd:protocol>\n"
            + //
            "                    <gco:CharacterString>information</gco:CharacterString>\n"
            + //
            "                  </gmd:protocol>\n"
            + //
            "                  <gmd:applicationProfile>\n"
            + //
            "                    <gco:CharacterString>web browser</gco:CharacterString>\n"
            + //
            "                  </gmd:applicationProfile>\n"
            + //
            "                  <gmd:name>\n"
            + //
            "                    <gco:CharacterString>Background Information</gco:CharacterString>\n"
            + //
            "                  </gmd:name>\n"
            + //
            "                  <gmd:description>\n"
            + //
            "                    <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
            + //
            "                  </gmd:description>\n"
            + //
            "                  <gmd:function>\n"
            + //
            "                    <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                  </gmd:function>\n"
            + //
            "                </gmd:CI_OnlineResource>\n"
            + //
            "              </gmd:onlineResource>\n"
            + //
            "            </gmd:CI_Contact>\n"
            + //
            "          </gmd:contactInfo>\n"
            + //
            "          <gmd:role>\n"
            + //
            "            <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n"
            + //
            "          </gmd:role>\n"
            + //
            "        </gmd:CI_ResponsibleParty>\n"
            + //
            "      </gmd:pointOfContact>\n"
            + //
            "      <gmd:descriptiveKeywords>\n"
            + //
            "        <gmd:MD_Keywords>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>algorithm</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>biology</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>center</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>chemistry</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>chlorophyll</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>color</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>concentration</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>data</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>ecology</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>flight</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>goddard</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>group</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>gsfc</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>image</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>imaging</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>L3</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>laboratory</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>level</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>level-3</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>mapped</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>mass</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>moderate</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>modis</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>nasa</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>ocean</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>ocean color</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>oceans</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>oci</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>optics</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>processing</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>resolution</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>sea</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>seawater</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>smi</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>space</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>spectroradiometer</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>standard</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>water</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:type>\n"
            + //
            "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
            + //
            "          </gmd:type>\n"
            + //
            "          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n"
            + //
            "        </gmd:MD_Keywords>\n"
            + //
            "      </gmd:descriptiveKeywords>\n"
            + //
            "      <gmd:descriptiveKeywords>\n"
            + //
            "        <gmd:MD_Keywords>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>Earth Science &gt; Oceans &gt; Ocean Optics &gt; Ocean Color</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>Earth Science &gt; Oceans &gt; Ocean Chemistry &gt; Chlorophyll</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:type>\n"
            + //
            "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
            + //
            "          </gmd:type>\n"
            + //
            "          <gmd:thesaurusName>\n"
            + //
            "            <gmd:CI_Citation>\n"
            + //
            "              <gmd:title>\n"
            + //
            "                <gco:CharacterString>GCMD Science Keywords</gco:CharacterString>\n"
            + //
            "              </gmd:title>\n"
            + //
            "              <gmd:date gco:nilReason=\"unknown\"/>\n"
            + //
            "            </gmd:CI_Citation>\n"
            + //
            "          </gmd:thesaurusName>\n"
            + //
            "        </gmd:MD_Keywords>\n"
            + //
            "      </gmd:descriptiveKeywords>\n"
            + //
            "      <gmd:descriptiveKeywords>\n"
            + //
            "        <gmd:MD_Keywords>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>Ocean Biology Processing Group (NASA/GSFC/OBPG)</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:type>\n"
            + //
            "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"project\">project</gmd:MD_KeywordTypeCode>\n"
            + //
            "          </gmd:type>\n"
            + //
            "          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n"
            + //
            "        </gmd:MD_Keywords>\n"
            + //
            "      </gmd:descriptiveKeywords>\n"
            + //
            "      <gmd:descriptiveKeywords>\n"
            + //
            "        <gmd:MD_Keywords>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>time</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>latitude</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>longitude</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:keyword>\n"
            + //
            "            <gco:CharacterString>concentration_of_chlorophyll_in_sea_water</gco:CharacterString>\n"
            + //
            "          </gmd:keyword>\n"
            + //
            "          <gmd:type>\n"
            + //
            "            <gmd:MD_KeywordTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
            + //
            "          </gmd:type>\n"
            + //
            "          <gmd:thesaurusName>\n"
            + //
            "            <gmd:CI_Citation>\n"
            + //
            "              <gmd:title>\n"
            + //
            "                <gco:CharacterString>CF Standard Name Table v70</gco:CharacterString>\n"
            + //
            "              </gmd:title>\n"
            + //
            "              <gmd:date gco:nilReason=\"unknown\"/>\n"
            + //
            "            </gmd:CI_Citation>\n"
            + //
            "          </gmd:thesaurusName>\n"
            + //
            "        </gmd:MD_Keywords>\n"
            + //
            "      </gmd:descriptiveKeywords>\n"
            + //
            "      <gmd:resourceConstraints>\n"
            + //
            "        <gmd:MD_LegalConstraints>\n"
            + //
            "          <gmd:useLimitation>\n"
            + //
            "            <gco:CharacterString>https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
            + //
            "The data may be used and redistributed for free but is not intended\n"
            + //
            "for legal use, since it may contain inaccuracies. Neither the data\n"
            + //
            "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + //
            "of their employees or contractors, makes any warranty, express or\n"
            + //
            "implied, including warranties of merchantability and fitness for a\n"
            + //
            "particular purpose, or assumes any legal liability for the accuracy,\n"
            + //
            "completeness, or usefulness, of this information.</gco:CharacterString>\n"
            + //
            "          </gmd:useLimitation>\n"
            + //
            "        </gmd:MD_LegalConstraints>\n"
            + //
            "      </gmd:resourceConstraints>\n"
            + //
            "      <gmd:aggregationInfo>\n"
            + //
            "        <gmd:MD_AggregateInformation>\n"
            + //
            "          <gmd:aggregateDataSetName>\n"
            + //
            "            <gmd:CI_Citation>\n"
            + //
            "              <gmd:title>\n"
            + //
            "                <gco:CharacterString>Ocean Biology Processing Group (NASA/GSFC/OBPG)</gco:CharacterString>\n"
            + //
            "              </gmd:title>\n"
            + //
            "              <gmd:date gco:nilReason=\"inapplicable\"/>\n"
            + //
            "            </gmd:CI_Citation>\n"
            + //
            "          </gmd:aggregateDataSetName>\n"
            + //
            "          <gmd:associationType>\n"
            + //
            "            <gmd:DS_AssociationTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n"
            + //
            "          </gmd:associationType>\n"
            + //
            "          <gmd:initiativeType>\n"
            + //
            "            <gmd:DS_InitiativeTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n"
            + //
            "          </gmd:initiativeType>\n"
            + //
            "        </gmd:MD_AggregateInformation>\n"
            + //
            "      </gmd:aggregationInfo>\n"
            + //
            "      <gmd:aggregationInfo>\n"
            + //
            "        <gmd:MD_AggregateInformation>\n"
            + //
            "          <gmd:aggregateDataSetIdentifier>\n"
            + //
            "            <gmd:MD_Identifier>\n"
            + //
            "              <gmd:authority>\n"
            + //
            "                <gmd:CI_Citation>\n"
            + //
            "                  <gmd:title>\n"
            + //
            "                    <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>\n"
            + //
            "                  </gmd:title>\n"
            + //
            "                  <gmd:date gco:nilReason=\"inapplicable\"/>\n"
            + //
            "                </gmd:CI_Citation>\n"
            + //
            "              </gmd:authority>\n"
            + //
            "              <gmd:code>\n"
            + //
            "                <gco:CharacterString>Grid</gco:CharacterString>\n"
            + //
            "              </gmd:code>\n"
            + //
            "            </gmd:MD_Identifier>\n"
            + //
            "          </gmd:aggregateDataSetIdentifier>\n"
            + //
            "          <gmd:associationType>\n"
            + //
            "            <gmd:DS_AssociationTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n"
            + //
            "          </gmd:associationType>\n"
            + //
            "          <gmd:initiativeType>\n"
            + //
            "            <gmd:DS_InitiativeTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n"
            + //
            "          </gmd:initiativeType>\n"
            + //
            "        </gmd:MD_AggregateInformation>\n"
            + //
            "      </gmd:aggregationInfo>\n"
            + //
            "      <gmd:language>\n"
            + //
            "        <gco:CharacterString>eng</gco:CharacterString>\n"
            + //
            "      </gmd:language>\n"
            + //
            "      <gmd:topicCategory>\n"
            + //
            "        <gmd:MD_TopicCategoryCode>geoscientificInformation</gmd:MD_TopicCategoryCode>\n"
            + //
            "      </gmd:topicCategory>\n"
            + //
            "      <gmd:extent>\n"
            + //
            "        <gmd:EX_Extent id=\"boundingExtent\">\n"
            + //
            "          <gmd:geographicElement>\n"
            + //
            "            <gmd:EX_GeographicBoundingBox id=\"boundingGeographicBoundingBox\">\n"
            + //
            "              <gmd:extentTypeCode>\n"
            + //
            "                <gco:Boolean>1</gco:Boolean>\n"
            + //
            "              </gmd:extentTypeCode>\n"
            + //
            "              <gmd:westBoundLongitude>\n"
            + //
            "                <gco:Decimal>-179.9792</gco:Decimal>\n"
            + //
            "              </gmd:westBoundLongitude>\n"
            + //
            "              <gmd:eastBoundLongitude>\n"
            + //
            "                <gco:Decimal>179.9792</gco:Decimal>\n"
            + //
            "              </gmd:eastBoundLongitude>\n"
            + //
            "              <gmd:southBoundLatitude>\n"
            + //
            "                <gco:Decimal>-89.97918</gco:Decimal>\n"
            + //
            "              </gmd:southBoundLatitude>\n"
            + //
            "              <gmd:northBoundLatitude>\n"
            + //
            "                <gco:Decimal>89.97916</gco:Decimal>\n"
            + //
            "              </gmd:northBoundLatitude>\n"
            + //
            "            </gmd:EX_GeographicBoundingBox>\n"
            + //
            "          </gmd:geographicElement>\n"
            + //
            "          <gmd:temporalElement>\n"
            + //
            "            <gmd:EX_TemporalExtent id=\"boundingTemporalExtent\">\n"
            + //
            "              <gmd:extent>\n"
            + //
            "                <gml:TimePeriod gml:id=\"DI_gmdExtent_timePeriod_id\">\n"
            + //
            "                  <gml:description>seconds</gml:description>\n"
            + //
            "                  <gml:beginPosition>2003-01-01T12:00:00Z</gml:beginPosition>\n"
            + //
            "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n"
            + //
            "                </gml:TimePeriod>\n"
            + //
            "              </gmd:extent>\n"
            + //
            "            </gmd:EX_TemporalExtent>\n"
            + //
            "          </gmd:temporalElement>\n"
            + //
            "        </gmd:EX_Extent>\n"
            + //
            "      </gmd:extent>\n"
            + //
            "    </gmd:MD_DataIdentification>\n"
            + //
            "  </gmd:identificationInfo>\n"
            + //
            "  <gmd:identificationInfo>\n"
            + //
            "    <srv:SV_ServiceIdentification id=\"ERDDAP-griddap\">\n"
            + //
            "      <gmd:citation>\n"
            + //
            "        <gmd:CI_Citation>\n"
            + //
            "          <gmd:title>\n"
            + //
            "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</gco:CharacterString>\n"
            + //
            "          </gmd:title>\n"
            + //
            "          <gmd:date>\n"
            + //
            "            <gmd:CI_Date>\n"
            + //
            "              <gmd:date>\n"
            + //
            "                <gco:Date>YYYY-MM-DD</gco:Date>\n"
            + //
            "              </gmd:date>\n"
            + //
            "              <gmd:dateType>\n"
            + //
            "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
            + //
            "              </gmd:dateType>\n"
            + //
            "            </gmd:CI_Date>\n"
            + //
            "          </gmd:date>\n"
            + //
            "          <gmd:citedResponsibleParty>\n"
            + //
            "            <gmd:CI_ResponsibleParty>\n"
            + //
            "              <gmd:individualName gco:nilReason=\"missing\"/>\n"
            + //
            "              <gmd:organisationName>\n"
            + //
            "                <gco:CharacterString>NASA/GSFC/OBPG</gco:CharacterString>\n"
            + //
            "              </gmd:organisationName>\n"
            + //
            "              <gmd:contactInfo>\n"
            + //
            "                <gmd:CI_Contact>\n"
            + //
            "                  <gmd:address>\n"
            + //
            "                    <gmd:CI_Address>\n"
            + //
            "                      <gmd:electronicMailAddress>\n"
            + //
            "                        <gco:CharacterString>data@oceancolor.gsfc.nasa.gov</gco:CharacterString>\n"
            + //
            "                      </gmd:electronicMailAddress>\n"
            + //
            "                    </gmd:CI_Address>\n"
            + //
            "                  </gmd:address>\n"
            + //
            "                  <gmd:onlineResource>\n"
            + //
            "                    <gmd:CI_OnlineResource>\n"
            + //
            "                      <gmd:linkage>\n"
            + //
            "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</gmd:URL>\n"
            + //
            "                      </gmd:linkage>\n"
            + //
            "                      <gmd:protocol>\n"
            + //
            "                        <gco:CharacterString>information</gco:CharacterString>\n"
            + //
            "                      </gmd:protocol>\n"
            + //
            "                      <gmd:applicationProfile>\n"
            + //
            "                        <gco:CharacterString>web browser</gco:CharacterString>\n"
            + //
            "                      </gmd:applicationProfile>\n"
            + //
            "                      <gmd:name>\n"
            + //
            "                        <gco:CharacterString>Background Information</gco:CharacterString>\n"
            + //
            "                      </gmd:name>\n"
            + //
            "                      <gmd:description>\n"
            + //
            "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
            + //
            "                      </gmd:description>\n"
            + //
            "                      <gmd:function>\n"
            + //
            "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                      </gmd:function>\n"
            + //
            "                    </gmd:CI_OnlineResource>\n"
            + //
            "                  </gmd:onlineResource>\n"
            + //
            "                </gmd:CI_Contact>\n"
            + //
            "              </gmd:contactInfo>\n"
            + //
            "              <gmd:role>\n"
            + //
            "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
            + //
            "              </gmd:role>\n"
            + //
            "            </gmd:CI_ResponsibleParty>\n"
            + //
            "          </gmd:citedResponsibleParty>\n"
            + //
            "        </gmd:CI_Citation>\n"
            + //
            "      </gmd:citation>\n"
            + //
            "      <gmd:abstract>\n"
            + //
            "        <gco:CharacterString>This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.</gco:CharacterString>\n"
            + //
            "      </gmd:abstract>\n"
            + //
            "      <srv:serviceType>\n"
            + //
            "        <gco:LocalName>ERDDAP griddap</gco:LocalName>\n"
            + //
            "      </srv:serviceType>\n"
            + //
            "      <srv:extent>\n"
            + //
            "        <gmd:EX_Extent>\n"
            + //
            "          <gmd:geographicElement>\n"
            + //
            "            <gmd:EX_GeographicBoundingBox>\n"
            + //
            "              <gmd:extentTypeCode>\n"
            + //
            "                <gco:Boolean>1</gco:Boolean>\n"
            + //
            "              </gmd:extentTypeCode>\n"
            + //
            "              <gmd:westBoundLongitude>\n"
            + //
            "                <gco:Decimal>-179.9792</gco:Decimal>\n"
            + //
            "              </gmd:westBoundLongitude>\n"
            + //
            "              <gmd:eastBoundLongitude>\n"
            + //
            "                <gco:Decimal>179.9792</gco:Decimal>\n"
            + //
            "              </gmd:eastBoundLongitude>\n"
            + //
            "              <gmd:southBoundLatitude>\n"
            + //
            "                <gco:Decimal>-89.97918</gco:Decimal>\n"
            + //
            "              </gmd:southBoundLatitude>\n"
            + //
            "              <gmd:northBoundLatitude>\n"
            + //
            "                <gco:Decimal>89.97916</gco:Decimal>\n"
            + //
            "              </gmd:northBoundLatitude>\n"
            + //
            "            </gmd:EX_GeographicBoundingBox>\n"
            + //
            "          </gmd:geographicElement>\n"
            + //
            "          <gmd:temporalElement>\n"
            + //
            "            <gmd:EX_TemporalExtent>\n"
            + //
            "              <gmd:extent>\n"
            + //
            "                <gml:TimePeriod gml:id=\"ED_gmdExtent_timePeriod_id\">\n"
            + //
            "                  <gml:description>seconds</gml:description>\n"
            + //
            "                  <gml:beginPosition>2003-01-01T12:00:00Z</gml:beginPosition>\n"
            + //
            "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n"
            + //
            "                </gml:TimePeriod>\n"
            + //
            "              </gmd:extent>\n"
            + //
            "            </gmd:EX_TemporalExtent>\n"
            + //
            "          </gmd:temporalElement>\n"
            + //
            "        </gmd:EX_Extent>\n"
            + //
            "      </srv:extent>\n"
            + //
            "      <srv:couplingType>\n"
            + //
            "        <srv:SV_CouplingType codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
            + //
            "      </srv:couplingType>\n"
            + //
            "      <srv:containsOperations>\n"
            + //
            "        <srv:SV_OperationMetadata>\n"
            + //
            "          <srv:operationName>\n"
            + //
            "            <gco:CharacterString>ERDDAPgriddapDatasetQueryAndAccess</gco:CharacterString>\n"
            + //
            "          </srv:operationName>\n"
            + //
            "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
            + //
            "          <srv:connectPoint>\n"
            + //
            "            <gmd:CI_OnlineResource>\n"
            + //
            "              <gmd:linkage>\n"
            + //
            "                <gmd:URL>http://localhost:8080/erddap/griddap/erdMH1chla1day</gmd:URL>\n"
            + //
            "              </gmd:linkage>\n"
            + //
            "              <gmd:protocol>\n"
            + //
            "                <gco:CharacterString>ERDDAP:griddap</gco:CharacterString>\n"
            + //
            "              </gmd:protocol>\n"
            + //
            "              <gmd:name>\n"
            + //
            "                <gco:CharacterString>ERDDAP-griddap</gco:CharacterString>\n"
            + //
            "              </gmd:name>\n"
            + //
            "              <gmd:description>\n"
            + //
            "                <gco:CharacterString>ERDDAP's griddap service (a flavor of OPeNDAP) for gridded data. Add different extensions (e.g., .html, .graph, .das, .dds) to the base URL for different purposes.</gco:CharacterString>\n"
            + //
            "              </gmd:description>\n"
            + //
            "              <gmd:function>\n"
            + //
            "                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
            + //
            "              </gmd:function>\n"
            + //
            "            </gmd:CI_OnlineResource>\n"
            + //
            "          </srv:connectPoint>\n"
            + //
            "        </srv:SV_OperationMetadata>\n"
            + //
            "      </srv:containsOperations>\n"
            + //
            "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n"
            + //
            "    </srv:SV_ServiceIdentification>\n"
            + //
            "  </gmd:identificationInfo>\n"
            + //
            "  <gmd:identificationInfo>\n"
            + //
            "    <srv:SV_ServiceIdentification id=\"OPeNDAP\">\n"
            + //
            "      <gmd:citation>\n"
            + //
            "        <gmd:CI_Citation>\n"
            + //
            "          <gmd:title>\n"
            + //
            "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</gco:CharacterString>\n"
            + //
            "          </gmd:title>\n"
            + //
            "          <gmd:date>\n"
            + //
            "            <gmd:CI_Date>\n"
            + //
            "              <gmd:date>\n"
            + //
            "                <gco:Date>YYYY-MM-DD</gco:Date>\n"
            + //
            "              </gmd:date>\n"
            + //
            "              <gmd:dateType>\n"
            + //
            "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
            + //
            "              </gmd:dateType>\n"
            + //
            "            </gmd:CI_Date>\n"
            + //
            "          </gmd:date>\n"
            + //
            "          <gmd:citedResponsibleParty>\n"
            + //
            "            <gmd:CI_ResponsibleParty>\n"
            + //
            "              <gmd:individualName gco:nilReason=\"missing\"/>\n"
            + //
            "              <gmd:organisationName>\n"
            + //
            "                <gco:CharacterString>NASA/GSFC/OBPG</gco:CharacterString>\n"
            + //
            "              </gmd:organisationName>\n"
            + //
            "              <gmd:contactInfo>\n"
            + //
            "                <gmd:CI_Contact>\n"
            + //
            "                  <gmd:address>\n"
            + //
            "                    <gmd:CI_Address>\n"
            + //
            "                      <gmd:electronicMailAddress>\n"
            + //
            "                        <gco:CharacterString>data@oceancolor.gsfc.nasa.gov</gco:CharacterString>\n"
            + //
            "                      </gmd:electronicMailAddress>\n"
            + //
            "                    </gmd:CI_Address>\n"
            + //
            "                  </gmd:address>\n"
            + //
            "                  <gmd:onlineResource>\n"
            + //
            "                    <gmd:CI_OnlineResource>\n"
            + //
            "                      <gmd:linkage>\n"
            + //
            "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</gmd:URL>\n"
            + //
            "                      </gmd:linkage>\n"
            + //
            "                      <gmd:protocol>\n"
            + //
            "                        <gco:CharacterString>information</gco:CharacterString>\n"
            + //
            "                      </gmd:protocol>\n"
            + //
            "                      <gmd:applicationProfile>\n"
            + //
            "                        <gco:CharacterString>web browser</gco:CharacterString>\n"
            + //
            "                      </gmd:applicationProfile>\n"
            + //
            "                      <gmd:name>\n"
            + //
            "                        <gco:CharacterString>Background Information</gco:CharacterString>\n"
            + //
            "                      </gmd:name>\n"
            + //
            "                      <gmd:description>\n"
            + //
            "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
            + //
            "                      </gmd:description>\n"
            + //
            "                      <gmd:function>\n"
            + //
            "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                      </gmd:function>\n"
            + //
            "                    </gmd:CI_OnlineResource>\n"
            + //
            "                  </gmd:onlineResource>\n"
            + //
            "                </gmd:CI_Contact>\n"
            + //
            "              </gmd:contactInfo>\n"
            + //
            "              <gmd:role>\n"
            + //
            "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
            + //
            "              </gmd:role>\n"
            + //
            "            </gmd:CI_ResponsibleParty>\n"
            + //
            "          </gmd:citedResponsibleParty>\n"
            + //
            "        </gmd:CI_Citation>\n"
            + //
            "      </gmd:citation>\n"
            + //
            "      <gmd:abstract>\n"
            + //
            "        <gco:CharacterString>This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.</gco:CharacterString>\n"
            + //
            "      </gmd:abstract>\n"
            + //
            "      <srv:serviceType>\n"
            + //
            "        <gco:LocalName>OPeNDAP</gco:LocalName>\n"
            + //
            "      </srv:serviceType>\n"
            + //
            "      <srv:extent>\n"
            + //
            "        <gmd:EX_Extent>\n"
            + //
            "          <gmd:geographicElement>\n"
            + //
            "            <gmd:EX_GeographicBoundingBox>\n"
            + //
            "              <gmd:extentTypeCode>\n"
            + //
            "                <gco:Boolean>1</gco:Boolean>\n"
            + //
            "              </gmd:extentTypeCode>\n"
            + //
            "              <gmd:westBoundLongitude>\n"
            + //
            "                <gco:Decimal>-179.9792</gco:Decimal>\n"
            + //
            "              </gmd:westBoundLongitude>\n"
            + //
            "              <gmd:eastBoundLongitude>\n"
            + //
            "                <gco:Decimal>179.9792</gco:Decimal>\n"
            + //
            "              </gmd:eastBoundLongitude>\n"
            + //
            "              <gmd:southBoundLatitude>\n"
            + //
            "                <gco:Decimal>-89.97918</gco:Decimal>\n"
            + //
            "              </gmd:southBoundLatitude>\n"
            + //
            "              <gmd:northBoundLatitude>\n"
            + //
            "                <gco:Decimal>89.97916</gco:Decimal>\n"
            + //
            "              </gmd:northBoundLatitude>\n"
            + //
            "            </gmd:EX_GeographicBoundingBox>\n"
            + //
            "          </gmd:geographicElement>\n"
            + //
            "          <gmd:temporalElement>\n"
            + //
            "            <gmd:EX_TemporalExtent>\n"
            + //
            "              <gmd:extent>\n"
            + //
            "                <gml:TimePeriod gml:id=\"OD_gmdExtent_timePeriod_id\">\n"
            + //
            "                  <gml:description>seconds</gml:description>\n"
            + //
            "                  <gml:beginPosition>2003-01-01T12:00:00Z</gml:beginPosition>\n"
            + //
            "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n"
            + //
            "                </gml:TimePeriod>\n"
            + //
            "              </gmd:extent>\n"
            + //
            "            </gmd:EX_TemporalExtent>\n"
            + //
            "          </gmd:temporalElement>\n"
            + //
            "        </gmd:EX_Extent>\n"
            + //
            "      </srv:extent>\n"
            + //
            "      <srv:couplingType>\n"
            + //
            "        <srv:SV_CouplingType codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
            + //
            "      </srv:couplingType>\n"
            + //
            "      <srv:containsOperations>\n"
            + //
            "        <srv:SV_OperationMetadata>\n"
            + //
            "          <srv:operationName>\n"
            + //
            "            <gco:CharacterString>OPeNDAPDatasetQueryAndAccess</gco:CharacterString>\n"
            + //
            "          </srv:operationName>\n"
            + //
            "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
            + //
            "          <srv:connectPoint>\n"
            + //
            "            <gmd:CI_OnlineResource>\n"
            + //
            "              <gmd:linkage>\n"
            + //
            "                <gmd:URL>http://localhost:8080/erddap/griddap/erdMH1chla1day</gmd:URL>\n"
            + //
            "              </gmd:linkage>\n"
            + //
            "              <gmd:protocol>\n"
            + //
            "                <gco:CharacterString>OPeNDAP:OPeNDAP</gco:CharacterString>\n"
            + //
            "              </gmd:protocol>\n"
            + //
            "              <gmd:name>\n"
            + //
            "                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n"
            + //
            "              </gmd:name>\n"
            + //
            "              <gmd:description>\n"
            + //
            "                <gco:CharacterString>An OPeNDAP service for gridded data. Add different extensions (e.g., .html, .das, .dds) to the base URL for different purposes.</gco:CharacterString>\n"
            + //
            "              </gmd:description>\n"
            + //
            "              <gmd:function>\n"
            + //
            "                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
            + //
            "              </gmd:function>\n"
            + //
            "            </gmd:CI_OnlineResource>\n"
            + //
            "          </srv:connectPoint>\n"
            + //
            "        </srv:SV_OperationMetadata>\n"
            + //
            "      </srv:containsOperations>\n"
            + //
            "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n"
            + //
            "    </srv:SV_ServiceIdentification>\n"
            + //
            "  </gmd:identificationInfo>\n"
            + //
            "  <gmd:identificationInfo>\n"
            + //
            "    <srv:SV_ServiceIdentification id=\"OGC-WMS\">\n"
            + //
            "      <gmd:citation>\n"
            + //
            "        <gmd:CI_Citation>\n"
            + //
            "          <gmd:title>\n"
            + //
            "            <gco:CharacterString>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</gco:CharacterString>\n"
            + //
            "          </gmd:title>\n"
            + //
            "          <gmd:date>\n"
            + //
            "            <gmd:CI_Date>\n"
            + //
            "              <gmd:date>\n"
            + //
            "                <gco:Date>YYYY-MM-DD</gco:Date>\n"
            + //
            "              </gmd:date>\n"
            + //
            "              <gmd:dateType>\n"
            + //
            "                <gmd:CI_DateTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
            + //
            "              </gmd:dateType>\n"
            + //
            "            </gmd:CI_Date>\n"
            + //
            "          </gmd:date>\n"
            + //
            "          <gmd:citedResponsibleParty>\n"
            + //
            "            <gmd:CI_ResponsibleParty>\n"
            + //
            "              <gmd:individualName gco:nilReason=\"missing\"/>\n"
            + //
            "              <gmd:organisationName>\n"
            + //
            "                <gco:CharacterString>NASA/GSFC/OBPG</gco:CharacterString>\n"
            + //
            "              </gmd:organisationName>\n"
            + //
            "              <gmd:contactInfo>\n"
            + //
            "                <gmd:CI_Contact>\n"
            + //
            "                  <gmd:address>\n"
            + //
            "                    <gmd:CI_Address>\n"
            + //
            "                      <gmd:electronicMailAddress>\n"
            + //
            "                        <gco:CharacterString>data@oceancolor.gsfc.nasa.gov</gco:CharacterString>\n"
            + //
            "                      </gmd:electronicMailAddress>\n"
            + //
            "                    </gmd:CI_Address>\n"
            + //
            "                  </gmd:address>\n"
            + //
            "                  <gmd:onlineResource>\n"
            + //
            "                    <gmd:CI_OnlineResource>\n"
            + //
            "                      <gmd:linkage>\n"
            + //
            "                        <gmd:URL>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</gmd:URL>\n"
            + //
            "                      </gmd:linkage>\n"
            + //
            "                      <gmd:protocol>\n"
            + //
            "                        <gco:CharacterString>information</gco:CharacterString>\n"
            + //
            "                      </gmd:protocol>\n"
            + //
            "                      <gmd:applicationProfile>\n"
            + //
            "                        <gco:CharacterString>web browser</gco:CharacterString>\n"
            + //
            "                      </gmd:applicationProfile>\n"
            + //
            "                      <gmd:name>\n"
            + //
            "                        <gco:CharacterString>Background Information</gco:CharacterString>\n"
            + //
            "                      </gmd:name>\n"
            + //
            "                      <gmd:description>\n"
            + //
            "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
            + //
            "                      </gmd:description>\n"
            + //
            "                      <gmd:function>\n"
            + //
            "                        <gmd:CI_OnLineFunctionCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                      </gmd:function>\n"
            + //
            "                    </gmd:CI_OnlineResource>\n"
            + //
            "                  </gmd:onlineResource>\n"
            + //
            "                </gmd:CI_Contact>\n"
            + //
            "              </gmd:contactInfo>\n"
            + //
            "              <gmd:role>\n"
            + //
            "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
            + //
            "              </gmd:role>\n"
            + //
            "            </gmd:CI_ResponsibleParty>\n"
            + //
            "          </gmd:citedResponsibleParty>\n"
            + //
            "        </gmd:CI_Citation>\n"
            + //
            "      </gmd:citation>\n"
            + //
            "      <gmd:abstract>\n"
            + //
            "        <gco:CharacterString>This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.</gco:CharacterString>\n"
            + //
            "      </gmd:abstract>\n"
            + //
            "      <srv:serviceType>\n"
            + //
            "        <gco:LocalName>Open Geospatial Consortium Web Map Service (WMS)</gco:LocalName>\n"
            + //
            "      </srv:serviceType>\n"
            + //
            "      <srv:extent>\n"
            + //
            "        <gmd:EX_Extent>\n"
            + //
            "          <gmd:geographicElement>\n"
            + //
            "            <gmd:EX_GeographicBoundingBox>\n"
            + //
            "              <gmd:extentTypeCode>\n"
            + //
            "                <gco:Boolean>1</gco:Boolean>\n"
            + //
            "              </gmd:extentTypeCode>\n"
            + //
            "              <gmd:westBoundLongitude>\n"
            + //
            "                <gco:Decimal>-179.9792</gco:Decimal>\n"
            + //
            "              </gmd:westBoundLongitude>\n"
            + //
            "              <gmd:eastBoundLongitude>\n"
            + //
            "                <gco:Decimal>179.9792</gco:Decimal>\n"
            + //
            "              </gmd:eastBoundLongitude>\n"
            + //
            "              <gmd:southBoundLatitude>\n"
            + //
            "                <gco:Decimal>-89.97918</gco:Decimal>\n"
            + //
            "              </gmd:southBoundLatitude>\n"
            + //
            "              <gmd:northBoundLatitude>\n"
            + //
            "                <gco:Decimal>89.97916</gco:Decimal>\n"
            + //
            "              </gmd:northBoundLatitude>\n"
            + //
            "            </gmd:EX_GeographicBoundingBox>\n"
            + //
            "          </gmd:geographicElement>\n"
            + //
            "          <gmd:temporalElement>\n"
            + //
            "            <gmd:EX_TemporalExtent>\n"
            + //
            "              <gmd:extent>\n"
            + //
            "                <gml:TimePeriod gml:id=\"WMS_gmdExtent_timePeriod_id\">\n"
            + //
            "                  <gml:description>seconds</gml:description>\n"
            + //
            "                  <gml:beginPosition>2003-01-01T12:00:00Z</gml:beginPosition>\n"
            + //
            "                  <gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>\n"
            + //
            "                </gml:TimePeriod>\n"
            + //
            "              </gmd:extent>\n"
            + //
            "            </gmd:EX_TemporalExtent>\n"
            + //
            "          </gmd:temporalElement>\n"
            + //
            "        </gmd:EX_Extent>\n"
            + //
            "      </srv:extent>\n"
            + //
            "      <srv:couplingType>\n"
            + //
            "        <srv:SV_CouplingType codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
            + //
            "      </srv:couplingType>\n"
            + //
            "      <srv:containsOperations>\n"
            + //
            "        <srv:SV_OperationMetadata>\n"
            + //
            "          <srv:operationName>\n"
            + //
            "            <gco:CharacterString>GetCapabilities</gco:CharacterString>\n"
            + //
            "          </srv:operationName>\n"
            + //
            "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
            + //
            "          <srv:connectPoint>\n"
            + //
            "            <gmd:CI_OnlineResource>\n"
            + //
            "              <gmd:linkage>\n"
            + //
            "                <gmd:URL>http://localhost:8080/erddap/wms/erdMH1chla1day/request?service=WMS&amp;version=1.3.0&amp;request=GetCapabilities</gmd:URL>\n"
            + //
            "              </gmd:linkage>\n"
            + //
            "              <gmd:protocol>\n"
            + //
            "                <gco:CharacterString>OGC:WMS</gco:CharacterString>\n"
            + //
            "              </gmd:protocol>\n"
            + //
            "              <gmd:name>\n"
            + //
            "                <gco:CharacterString>OGC-WMS</gco:CharacterString>\n"
            + //
            "              </gmd:name>\n"
            + //
            "              <gmd:description>\n"
            + //
            "                <gco:CharacterString>Open Geospatial Consortium Web Map Service (WMS)</gco:CharacterString>\n"
            + //
            "              </gmd:description>\n"
            + //
            "              <gmd:function>\n"
            + //
            "                <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
            + //
            "              </gmd:function>\n"
            + //
            "            </gmd:CI_OnlineResource>\n"
            + //
            "          </srv:connectPoint>\n"
            + //
            "        </srv:SV_OperationMetadata>\n"
            + //
            "      </srv:containsOperations>\n"
            + //
            "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n"
            + //
            "    </srv:SV_ServiceIdentification>\n"
            + //
            "  </gmd:identificationInfo>\n"
            + //
            "  <gmd:contentInfo>\n"
            + //
            "    <gmi:MI_CoverageDescription>\n"
            + //
            "      <gmd:attributeDescription gco:nilReason=\"unknown\"/>\n"
            + //
            "      <gmd:contentType>\n"
            + //
            "        <gmd:MD_CoverageContentTypeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CoverageContentTypeCode\" codeListValue=\"physicalMeasurement\">physicalMeasurement</gmd:MD_CoverageContentTypeCode>\n"
            + //
            "      </gmd:contentType>\n"
            + //
            "      <gmd:dimension>\n"
            + //
            "        <gmd:MD_Band>\n"
            + //
            "          <gmd:sequenceIdentifier>\n"
            + //
            "            <gco:MemberName>\n"
            + //
            "              <gco:aName>\n"
            + //
            "                <gco:CharacterString>chlorophyll</gco:CharacterString>\n"
            + //
            "              </gco:aName>\n"
            + //
            "              <gco:attributeType>\n"
            + //
            "                <gco:TypeName>\n"
            + //
            "                  <gco:aName>\n"
            + //
            "                    <gco:CharacterString>float</gco:CharacterString>\n"
            + //
            "                  </gco:aName>\n"
            + //
            "                </gco:TypeName>\n"
            + //
            "              </gco:attributeType>\n"
            + //
            "            </gco:MemberName>\n"
            + //
            "          </gmd:sequenceIdentifier>\n"
            + //
            "          <gmd:descriptor>\n"
            + //
            "            <gco:CharacterString>Mean Chlorophyll a Concentration</gco:CharacterString>\n"
            + //
            "          </gmd:descriptor>\n"
            + //
            "          <gmd:units xlink:href=\"https://unitsofmeasure.org/ucum.html#mg&#x2e;m&#x2d;3\"/>\n"
            + //
            "        </gmd:MD_Band>\n"
            + //
            "      </gmd:dimension>\n"
            + //
            "    </gmi:MI_CoverageDescription>\n"
            + //
            "  </gmd:contentInfo>\n"
            + //
            "  <gmd:distributionInfo>\n"
            + //
            "    <gmd:MD_Distribution>\n"
            + //
            "      <gmd:distributor>\n"
            + //
            "        <gmd:MD_Distributor>\n"
            + //
            "          <gmd:distributorContact>\n"
            + //
            "            <gmd:CI_ResponsibleParty>\n"
            + //
            "              <gmd:individualName>\n"
            + //
            "                <gco:CharacterString>ERDDAP Jetty Developer</gco:CharacterString>\n"
            + //
            "              </gmd:individualName>\n"
            + //
            "              <gmd:organisationName>\n"
            + //
            "                <gco:CharacterString>ERDDAP Jetty Install</gco:CharacterString>\n"
            + //
            "              </gmd:organisationName>\n"
            + //
            "              <gmd:contactInfo>\n"
            + //
            "                <gmd:CI_Contact>\n"
            + //
            "                  <gmd:phone>\n"
            + //
            "                    <gmd:CI_Telephone>\n"
            + //
            "                      <gmd:voice>\n"
            + //
            "                        <gco:CharacterString>555-555-5555</gco:CharacterString>\n"
            + //
            "                      </gmd:voice>\n"
            + //
            "                    </gmd:CI_Telephone>\n"
            + //
            "                  </gmd:phone>\n"
            + //
            "                  <gmd:address>\n"
            + //
            "                    <gmd:CI_Address>\n"
            + //
            "                      <gmd:deliveryPoint>\n"
            + //
            "                        <gco:CharacterString>123 Irrelevant St.</gco:CharacterString>\n"
            + //
            "                      </gmd:deliveryPoint>\n"
            + //
            "                      <gmd:city>\n"
            + //
            "                        <gco:CharacterString>Nowhere</gco:CharacterString>\n"
            + //
            "                      </gmd:city>\n"
            + //
            "                      <gmd:administrativeArea>\n"
            + //
            "                        <gco:CharacterString>AK</gco:CharacterString>\n"
            + //
            "                      </gmd:administrativeArea>\n"
            + //
            "                      <gmd:postalCode>\n"
            + //
            "                        <gco:CharacterString>99504</gco:CharacterString>\n"
            + //
            "                      </gmd:postalCode>\n"
            + //
            "                      <gmd:country>\n"
            + //
            "                        <gco:CharacterString>USA</gco:CharacterString>\n"
            + //
            "                      </gmd:country>\n"
            + //
            "                      <gmd:electronicMailAddress>\n"
            + //
            "                        <gco:CharacterString>nobody@example.com</gco:CharacterString>\n"
            + //
            "                      </gmd:electronicMailAddress>\n"
            + //
            "                    </gmd:CI_Address>\n"
            + //
            "                  </gmd:address>\n"
            + //
            "                </gmd:CI_Contact>\n"
            + //
            "              </gmd:contactInfo>\n"
            + //
            "              <gmd:role>\n"
            + //
            "                <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" codeListValue=\"distributor\">distributor</gmd:CI_RoleCode>\n"
            + //
            "              </gmd:role>\n"
            + //
            "            </gmd:CI_ResponsibleParty>\n"
            + //
            "          </gmd:distributorContact>\n"
            + //
            "          <gmd:distributorFormat>\n"
            + //
            "            <gmd:MD_Format>\n"
            + //
            "              <gmd:name>\n"
            + //
            "                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n"
            + //
            "              </gmd:name>\n"
            + //
            "              <gmd:version>\n"
            + //
            "                <gco:CharacterString>DAP/2.0</gco:CharacterString>\n"
            + //
            "              </gmd:version>\n"
            + //
            "            </gmd:MD_Format>\n"
            + //
            "          </gmd:distributorFormat>\n"
            + //
            "          <gmd:distributorTransferOptions>\n"
            + //
            "            <gmd:MD_DigitalTransferOptions>\n"
            + //
            "              <gmd:onLine>\n"
            + //
            "                <gmd:CI_OnlineResource>\n"
            + //
            "                  <gmd:linkage>\n"
            + //
            "                    <gmd:URL>http://localhost:8080/erddap/griddap/erdMH1chla1day.html</gmd:URL>\n"
            + //
            "                  </gmd:linkage>\n"
            + //
            "                  <gmd:protocol>\n"
            + //
            "                    <gco:CharacterString>order</gco:CharacterString>\n"
            + //
            "                  </gmd:protocol>\n"
            + //
            "                  <gmd:name>\n"
            + //
            "                    <gco:CharacterString>Data Subset Form</gco:CharacterString>\n"
            + //
            "                  </gmd:name>\n"
            + //
            "                  <gmd:description>\n"
            + //
            "                    <gco:CharacterString>ERDDAP's version of the OPeNDAP .html web page for this dataset. Specify a subset of the dataset and download the data via OPeNDAP or in many different file types.</gco:CharacterString>\n"
            + //
            "                  </gmd:description>\n"
            + //
            "                  <gmd:function>\n"
            + //
            "                    <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                  </gmd:function>\n"
            + //
            "                </gmd:CI_OnlineResource>\n"
            + //
            "              </gmd:onLine>\n"
            + //
            "            </gmd:MD_DigitalTransferOptions>\n"
            + //
            "          </gmd:distributorTransferOptions>\n"
            + //
            "          <gmd:distributorTransferOptions>\n"
            + //
            "            <gmd:MD_DigitalTransferOptions>\n"
            + //
            "              <gmd:onLine>\n"
            + //
            "                <gmd:CI_OnlineResource>\n"
            + //
            "                  <gmd:linkage>\n"
            + //
            "                    <gmd:URL>http://localhost:8080/erddap/griddap/erdMH1chla1day.graph</gmd:URL>\n"
            + //
            "                  </gmd:linkage>\n"
            + //
            "                  <gmd:protocol>\n"
            + //
            "                    <gco:CharacterString>order</gco:CharacterString>\n"
            + //
            "                  </gmd:protocol>\n"
            + //
            "                  <gmd:name>\n"
            + //
            "                    <gco:CharacterString>Make-A-Graph Form</gco:CharacterString>\n"
            + //
            "                  </gmd:name>\n"
            + //
            "                  <gmd:description>\n"
            + //
            "                    <gco:CharacterString>ERDDAP's Make-A-Graph .html web page for this dataset. Create an image with a map or graph of a subset of the data.</gco:CharacterString>\n"
            + //
            "                  </gmd:description>\n"
            + //
            "                  <gmd:function>\n"
            + //
            "                    <gmd:CI_OnLineFunctionCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" codeListValue=\"mapDigital\">mapDigital</gmd:CI_OnLineFunctionCode>\n"
            + //
            "                  </gmd:function>\n"
            + //
            "                </gmd:CI_OnlineResource>\n"
            + //
            "              </gmd:onLine>\n"
            + //
            "            </gmd:MD_DigitalTransferOptions>\n"
            + //
            "          </gmd:distributorTransferOptions>\n"
            + //
            "        </gmd:MD_Distributor>\n"
            + //
            "      </gmd:distributor>\n"
            + //
            "    </gmd:MD_Distribution>\n"
            + //
            "  </gmd:distributionInfo>\n"
            + //
            "  <gmd:dataQualityInfo>\n"
            + //
            "    <gmd:DQ_DataQuality>\n"
            + //
            "      <gmd:scope>\n"
            + //
            "        <gmd:DQ_Scope>\n"
            + //
            "          <gmd:level>\n"
            + //
            "            <gmd:MD_ScopeCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n"
            + //
            "          </gmd:level>\n"
            + //
            "        </gmd:DQ_Scope>\n"
            + //
            "      </gmd:scope>\n"
            + //
            "      <gmd:lineage>\n"
            + //
            "        <gmd:LI_Lineage>\n"
            + //
            "          <gmd:statement>\n"
            + //
            "            <gco:CharacterString>Files downloaded daily from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA SWFSC ERD (erd.data@noaa.gov)\n"
            + //
            "ERDDAP adds the time dimension.\n"
            + //
            "Direct read of HDF4 file through CDM library</gco:CharacterString>\n"
            + //
            "          </gmd:statement>\n"
            + //
            "        </gmd:LI_Lineage>\n"
            + //
            "      </gmd:lineage>\n"
            + //
            "    </gmd:DQ_DataQuality>\n"
            + //
            "  </gmd:dataQualityInfo>\n"
            + //
            "  <gmd:metadataMaintenance>\n"
            + //
            "    <gmd:MD_MaintenanceInformation>\n"
            + //
            "      <gmd:maintenanceAndUpdateFrequency gco:nilReason=\"unknown\"/>\n"
            + //
            "      <gmd:maintenanceNote>\n"
            + //
            "        <gco:CharacterString>This record was created from dataset metadata by ERDDAP Version "
            + EDStatic.erddapVersion
            + "</gco:CharacterString>\n"
            + //
            "      </gmd:maintenanceNote>\n"
            + //
            "    </gmd:MD_MaintenanceInformation>\n"
            + //
            "  </gmd:metadataMaintenance>\n"
            + //
            "</gmi:MI_Metadata>\n";
    results =
        results.replaceAll("<gco:Date>....-..-..</gco:Date>", "<gco:Date>YYYY-MM-DD</gco:Date>");
    results =
        results.replaceAll(
            "<gco:Measure uom=\\\"s\\\">[0-9]+.[0-9]+</gco:Measure>",
            "<gco:Measure uom=\"s\">VALUE</gco:Measure>");
    results =
        results.replaceAll(
            "<gco:Measure uom=\\\"s\\\">.*</gco:Measure>",
            "<gco:Measure uom=\"s\">VALUE</gco:Measure>");
    results =
        results.replaceAll(
            "<gml:endPosition>....-..-..T..:00:00Z</gml:endPosition>",
            "<gml:endPosition>YYYY-MM-DDThh:00:00Z</gml:endPosition>");
    results =
        results.replaceAll(
            "<gco:Integer>[0-9]+</gco:Integer>", "<gco:Integer>NUMBER</gco:Integer>");
    results = results.replaceAll(">-?[0-9]+.[0-9]+</gco:Measure>", ">measureValue</gco:Measure>");
    Test.ensureEqual(results, expected, "results=" + results);

    results = SSR.getUrlResponseStringUnchanged("http://localhost:" + PORT + "/erddap/metadata/");
    expected =
        "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n"
            + //
            "<tr><th><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n"
            + //
            "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/back.gif\" alt=\"[DIR]\"></td><td><a href=\"&#x2e;&#x2e;\">Parent Directory</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"fgdc&#x2f;\">fgdc/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"iso19115&#x2f;\">iso19115/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + //
            "</table>\n"
            + //
            "3 directories, 0 files";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:" + PORT + "/erddap/metadata/iso19115/");
    expected =
        "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n"
            + //
            "<tr><th><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n"
            + //
            "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/back.gif\" alt=\"[DIR]\"></td><td><a href=\"&#x2e;&#x2e;\">Parent Directory</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"xml&#x2f;\">xml/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + //
            "</table>\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:" + PORT + "/erddap/metadata/iso19115/xml/");
    expected =
        "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/xml.gif\" alt=\"[XML]\"></td><td><a rel=\"bookmark\" href=\"erdMH1chlamday&#x5f;iso19115&#x2e;xml\">erdMH1chlamday&#x5f;iso19115&#x2e;xml</a></td><td class=\"R\">DD-MMM-YYYY HH:mm</td><td class=\"R\">53723</td><td>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)</td></tr>";
    results = results.replaceAll("..-...-.... ..:..", "DD-MMM-YYYY HH:mm");
    Test.ensureTrue(results.indexOf(expected) > 0, "No erdMH1chlamday found, results=" + results);
  }

  /** Test the metadata */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testMetadataFgdc() throws Exception {
    String results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:" + PORT + "/erddap/metadata/fgdc/xml/erdMH1chla1day_fgdc.xml");
    String expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<metadata xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.ngdc.noaa.gov/metadata/published/xsd/ngdcSchema/schema.xsd\" >\n"
            + //
            "  <idinfo>\n"
            + //
            "    <datsetid>localhost:"
            + PORT
            + ":erdMH1chla1day</datsetid>\n"
            + //
            "    <citation>\n"
            + //
            "      <citeinfo>\n"
            + //
            "        <origin>\n"
            + //
            "Project: Ocean Biology Processing Group (NASA/GSFC/OBPG)\n"
            + //
            "Name: NASA/GSFC/OBPG\n"
            + //
            "Email: data@oceancolor.gsfc.nasa.gov\n"
            + //
            "Institution: NOAA NMFS SWFSC ERD\n"
            + //
            "InfoURL: https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\n"
            + //
            "Source URL: (local files)\n"
            + //
            "        </origin>\n"
            + //
            "        <origin_cntinfo>\n"
            + //
            "          <cntinfo>\n"
            + //
            "            <cntorgp>\n"
            + //
            "              <cntorg>NOAA NMFS SWFSC ERD</cntorg>\n"
            + //
            "              <cntper>NASA/GSFC/OBPG</cntper>\n"
            + //
            "            </cntorgp>\n"
            + //
            "            <cntemail>data@oceancolor.gsfc.nasa.gov</cntemail>\n"
            + //
            "          </cntinfo>\n"
            + //
            "        </origin_cntinfo>\n"
            + //
            "        <pubdate></pubdate>\n"
            + //
            "        <title>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</title>\n"
            + //
            "        <edition>Unknown</edition>\n"
            + //
            "        <geoform>raster digital data</geoform>\n"
            + //
            "        <pubinfo>\n"
            + //
            "          <pubplace>Nowhere, AK, USA</pubplace>\n"
            + //
            "          <publish>ERDDAP, version "
            + EDStatic.erddapVersion
            + ", at ERDDAP Jetty Install</publish>\n"
            + //
            "          <publish_cntinfo>\n"
            + //
            "            <cntinfo>\n"
            + //
            "              <cntorgp>\n"
            + //
            "                <cntorg>ERDDAP Jetty Install</cntorg>\n"
            + //
            "                <cntper>ERDDAP Jetty Developer</cntper>\n"
            + //
            "              </cntorgp>\n"
            + //
            "              <cntpos>Software Engineer</cntpos>\n"
            + //
            "              <cntaddr>\n"
            + //
            "                <addrtype>Mailing and Physical Address</addrtype>\n"
            + //
            "                <address>123 Irrelevant St.</address>\n"
            + //
            "                <city>Nowhere</city>\n"
            + //
            "                <state>AK</state>\n"
            + //
            "                <postal>99504</postal>\n"
            + //
            "                <country>USA</country>\n"
            + //
            "              </cntaddr>\n"
            + //
            "              <cntvoice>555-555-5555</cntvoice>\n"
            + //
            "              <cntemail>nobody@example.com</cntemail>\n"
            + //
            "            </cntinfo>\n"
            + //
            "          </publish_cntinfo>\n"
            + //
            "        </pubinfo>\n"
            + //
            "        <onlink>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day.html</onlink>\n"
            + //
            "        <onlink>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day.graph</onlink>\n"
            + //
            "        <onlink>http://localhost:"
            + PORT
            + "/erddap/wms/erdMH1chla1day/request</onlink>\n"
            + //
            "        <CI_OnlineResource>\n"
            + //
            "          <linkage>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day.html</linkage>\n"
            + //
            "          <name>Download data: Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</name>\n"
            + //
            "          <description>A web page for specifying a subset of the dataset and downloading data in any of several file formats.</description>\n"
            + //
            "          <function>download data</function>\n"
            + //
            "        </CI_OnlineResource>\n"
            + //
            "        <CI_OnlineResource>\n"
            + //
            "          <linkage>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day.graph</linkage>\n"
            + //
            "          <name>Make a graph or map: Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</name>\n"
            + //
            "          <description>A web page for creating a graph or map of the data.</description>\n"
            + //
            "          <function>download graph or map</function>\n"
            + //
            "        </CI_OnlineResource>\n"
            + //
            "        <CI_OnlineResource>\n"
            + //
            "          <linkage>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day</linkage>\n"
            + //
            "          <name>OPeNDAP service: Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</name>\n"
            + //
            "          <description>The base URL for the OPeNDAP service.  Add .html to get a web page with a form to download data. Add .dds to get the dataset's structure. Add .das to get the dataset's metadata. Add .dods to download data via the OPeNDAP protocol.</description>\n"
            + //
            "          <function>OPeNDAP</function>\n"
            + //
            "        </CI_OnlineResource>\n"
            + //
            "        <CI_OnlineResource>\n"
            + //
            "          <linkage>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</linkage>\n"
            + //
            "          <name>Background information: Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</name>\n"
            + //
            "          <description>Background information for the dataset.</description>\n"
            + //
            "          <function>background information</function>\n"
            + //
            "        </CI_OnlineResource>\n"
            + //
            "        <CI_OnlineResource>\n"
            + //
            "          <linkage>http://localhost:"
            + PORT
            + "/erddap/wms/erdMH1chla1day/request</linkage>\n"
            + //
            "          <name>WMS service: Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)</name>\n"
            + //
            "          <description>The base URL for the WMS service for this dataset.</description>\n"
            + //
            "          <function>WMS</function>\n"
            + //
            "        </CI_OnlineResource>\n"
            + //
            "        <lworkcit>\n"
            + //
            "          <citeinfo>\n"
            + //
            "            <origin>Ocean Biology Processing Group (NASA/GSFC/OBPG)</origin>\n"
            + //
            "          </citeinfo>\n"
            + //
            "        </lworkcit>\n"
            + //
            "      </citeinfo>\n"
            + //
            "    </citation>\n"
            + //
            "    <descript>\n"
            + //
            "      <abstract>This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA&#39;s Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.</abstract>\n"
            + //
            "      <purpose>Unknown</purpose>\n"
            + //
            "      <supplinf>https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html</supplinf>\n"
            + //
            "    </descript>\n"; //
    results = results.replaceAll("<pubdate>........</pubdate>", "<pubdate>YYYYMMDD</pubdate>");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);
    results =
        SSR.getUrlResponseStringUnchanged("http://localhost:" + PORT + "/erddap/metadata/fgdc");
    expected =
        "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n"
            + //
            "<tr><th><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/blank.gif\" alt=\"[ICO]\"></th><th><a href=\"?C=N;O=D\">Name</a></th><th><a href=\"?C=M;O=A\">Last modified</a></th><th><a href=\"?C=S;O=A\">Size</a></th><th><a href=\"?C=D;O=A\">Description</a></th></tr>\n"
            + //
            "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/back.gif\" alt=\"[DIR]\"></td><td><a href=\"&#x2e;&#x2e;\">Parent Directory</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/dir.gif\" alt=\"[DIR]\"></td><td><a href=\"xml&#x2f;\">xml/</a></td><td class=\"R\">-</td><td class=\"R\">-</td><td></td></tr>\n"
            + //
            "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + //
            "</table>\n"
            + //
            "2 directories, 0 files";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:" + PORT + "/erddap/metadata/fgdc/xml/");
    expected =
        "<tr><td><img class=\"B\" src=\"http://localhost:"
            + PORT
            + "/erddap/images/fileIcons/xml.gif\" alt=\"[XML]\"></td><td><a rel=\"bookmark\" href=\"erdMH1chlamday&#x5f;fgdc&#x2e;xml\">erdMH1chlamday&#x5f;fgdc&#x2e;xml</a></td><td class=\"R\">DD-MMM-YYYY HH:mm</td><td class=\"R\">SIZE</td><td>Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)</td></tr>\n";
    results = results.replaceAll("..-...-.... ..:..", "DD-MMM-YYYY HH:mm");
    results = results.replaceAll(">[0-9]+<", ">SIZE<");
    Test.ensureTrue(results.indexOf(expected) > 0, "No erdMH1chlamday found, results=" + results);
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testSitemap() throws Exception {
    String results =
        SSR.getUrlResponseStringUnchanged("http://localhost:" + PORT + "/erddap/sitemap.xml");
    String expected =
        "<?xml version='1.0' encoding='UTF-8'?>\n"
            + //
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/categorize/index.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/index.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/oceanicAtmosphericAcronyms.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/oceanicAtmosphericVariableNames.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/fipscounty.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/keywords.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/time.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/units.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/convert/urls.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/griddap/documentation.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/griddap/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/images/embed.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/index.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/info/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/information.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/metadata/fgdc/xml/</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.3</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/metadata/iso19115/xml/</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.3</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/legal.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/rest.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/search/advanced.html?page=1&#x26;itemsPerPage=1000000000</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/search/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/slidesorter.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/subscriptions/index.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/subscriptions/add.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/subscriptions/validate.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/subscriptions/list.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/subscriptions/remove.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/tabledap/documentation.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/tabledap/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/wms/documentation.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/wms/index.html?page=1&#x26;itemsPerPage=1000000000</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.7</priority>\n"
            + //
            "</url>\n";

    results = results.replaceAll("<lastmod>....-..-..</lastmod>", "<lastmod>YYYY-MM-DD</lastmod>");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);

    String expected2 =
        "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/info/erdMH1chla1day/index.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla1day.graph</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.5</priority>\n"
            + //
            "</url>\n"
            + //
            "\n"
            + //
            "<url>\n"
            + //
            "<loc>http://localhost:"
            + PORT
            + "/erddap/wms/erdMH1chla1day/index.html</loc>\n"
            + //
            "<lastmod>YYYY-MM-DD</lastmod>\n"
            + //
            "<changefreq>monthly</changefreq>\n"
            + //
            "<priority>0.3</priority>\n"
            + //
            "</url>\n";
    int startIndex = results.indexOf(expected2.substring(0, 72));
    Test.ensureEqual(
        results.substring(startIndex, startIndex + expected2.length()),
        expected2,
        "results=" + results);
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testInterpolate() throws Exception {
    // TODO get a request that has actual interpolation in it
    String results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:"
                + PORT
                + "/erddap/convert/interpolate.json?TimeLatLonTable=time%2Clatitude%2Clongitude%0A2006-04-17T06%3A00%3A00Z%2C35.580%2C-122.550%0A2006-04-17T12%3A00%3A00Z%2C35.576%2C-122.553%0A2006-04-17T18%3A00%3A00Z%2C35.572%2C-122.568%0A2007-01-02T00%3A00%3A00Z%2C35.569%2C-122.571%0A&requestCSV=erdMH1chla8day%2Fchlorophyll%2FBilinear%2F4");
    String expected =
        "{\n"
            + //
            "  \"table\": {\n"
            + //
            "    \"columnNames\": [\"time\", \"latitude\", \"longitude\", \"erdMH1chla8day_chlorophyll_Bilinear_4\"],\n"
            + //
            "    \"columnTypes\": [\"String\", \"double\", \"double\", \"double\"],\n"
            + //
            "    \"rows\": [\n"
            + //
            "      [\"2006-04-17T06:00:00Z\", 35.58, -122.55, null],\n"
            + //
            "      [\"2006-04-17T12:00:00Z\", 35.576, -122.553, null],\n"
            + //
            "      [\"2006-04-17T18:00:00Z\", 35.572, -122.568, null],\n"
            + //
            "      [\"2007-01-02T00:00:00Z\", 35.569, -122.571, null]\n"
            + //
            "    ]\n"
            + //
            "  }\n"
            + //
            "}\n";
    Test.ensureEqual(results, expected, "results=" + results);

    // Request an html page, to test the html generation
    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:"
                + PORT
                + "/erddap/convert/interpolate.htmlTable?TimeLatLonTable=time%2Clatitude%2Clongitude%0A2006-04-17T06%3A00%3A00Z%2C35.580%2C-122.550%0A2006-04-17T12%3A00%3A00Z%2C35.576%2C-122.553%0A2006-04-17T18%3A00%3A00Z%2C35.572%2C-122.568%0A2007-01-02T00%3A00%3A00Z%2C35.569%2C-122.571%0A&requestCSV=erdMH1chla8day%2Fchlorophyll%2FBilinear%2F4");
    expected =
        "<table class=\"erd commonBGColor nowrap\">\n"
            + //
            "<tr>\n"
            + //
            "<th>time\n"
            + //
            "<th>latitude\n"
            + //
            "<th>longitude\n"
            + //
            "<th>erdMH1chla8day_chlorophyll_Bilinear_4\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<td>2006-04-17T06:00:00Z\n"
            + //
            "<td class=\"R\">35.58\n"
            + //
            "<td class=\"R\">-122.55\n"
            + //
            "<td>\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<td>2006-04-17T12:00:00Z\n"
            + //
            "<td class=\"R\">35.576\n"
            + //
            "<td class=\"R\">-122.553\n"
            + //
            "<td>\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<td>2006-04-17T18:00:00Z\n"
            + //
            "<td class=\"R\">35.572\n"
            + //
            "<td class=\"R\">-122.568\n"
            + //
            "<td>\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<td>2007-01-02T00:00:00Z\n"
            + //
            "<td class=\"R\">35.569\n"
            + //
            "<td class=\"R\">-122.571\n"
            + //
            "<td>\n"
            + //
            "</tr>\n"
            + //
            "</table>\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "No table found, results=" + results);
  }

  /* TableTests */

  /**
   * This is a test of readOpendapSequence. Test cases from Roy: GLOBEC VPT:
   * stn_id=loaddods('https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?stn_id&unique()');
   * abund=loaddods('-F','https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_vpt?abund_m3&stn_id="NH05"');
   * GLOBEC Bottle:
   * month=loaddods('-F','https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?month&unique()');
   * [oxy
   * temp]=loaddods('-F','https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month="5"');
   * GLOBEC MOC1:
   * [abund,lon,lat]=loaddods('-F','https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long');
   * [abund1,lon1,lat1]=loaddods('-F','https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1?abund_m3,lat,long&program="MESO_1"');
   * I note that loaddods documentation is at https://www.opendap.org/user/mgui-html/mgui_36.html
   * and -F says to convert all strings to floats. "unique()" seems to just return unique values.
   *
   * @throws Exception of trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testOpendapSequence() throws Exception {

    // 2013-03-18 I changed from oceanwatch datasets (no longer available) to
    // coastwatch erddap datasets
    // String2.log("\n*** Table.testOpendapSequence");
    // Table.verbose = true;
    // Table.reallyVerbose = true;
    Table table = new Table();
    int nRows;
    String url;
    float lon, lat;
    String results, expected;

    // 2016-07-25 test for Kevin's dataset: from remote erddap
    url =
        "https://ferret.pmel.noaa.gov/pmel/erddap/tabledap/ChukchiSea_454a_037a_fcf4?"
            + "prof,id,cast,cruise,time,longitude,lon360,latitude&time%3E=2012-09-04&time%3C=2012-09-07&distinct()";
    table.readOpendapSequence(url, false); // boolean: skipDapSpacerRows
    results = table.toString(3);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 2 ;\n"
            + "\tid_strlen = 10 ;\n"
            + "\tcast_strlen = 3 ;\n"
            + "\tcruise_strlen = 6 ;\n"
            + "variables:\n"
            + "\tdouble prof(row) ;\n"
            + "\t\tprof:actual_range = 1.0, 1.0 ;\n"
            + "\t\tprof:axis = \"E\" ;\n"
            + "\t\tprof:long_name = \"Prof\" ;\n"
            + "\t\tprof:point_spacing = \"even\" ;\n"
            + "\tchar id(row, id_strlen) ;\n"
            + "\t\tid:cf_role = \"profile_id\" ;\n"
            + "\t\tid:long_name = \"profile id\" ;\n"
            + "\tchar cast(row, cast_strlen) ;\n"
            + "\t\tcast:colorBarMaximum = 100.0 ;\n"
            + "\t\tcast:colorBarMinimum = 0.0 ;\n"
            + "\t\tcast:long_name = \"cast number\" ;\n"
            + "\tchar cruise(row, cruise_strlen) ;\n"
            + "\t\tcruise:long_name = \"Cruise name\" ;\n"
            + "\tdouble time(row) ;\n"
            + "\t\ttime:_CoordinateAxisType = \"Time\" ;\n"
            + "\t\ttime:actual_range = 1.28368572E9, 1.34697582E9 ;\n"
            + "\t\ttime:axis = \"T\" ;\n"
            + "\t\ttime:ioos_category = \"Time\" ;\n"
            + "\t\ttime:long_name = \"Time\" ;\n"
            + "\t\ttime:standard_name = \"time\" ;\n"
            + "\t\ttime:time_origin = \"01-JAN-1970 00:00:00\" ;\n"
            + "\t\ttime:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tfloat longitude(row) ;\n"
            + "\t\tlongitude:_CoordinateAxisType = \"Lon\" ;\n"
            + "\t\tlongitude:_FillValue = -1.0E34f ;\n"
            + "\t\tlongitude:actual_range = -174.6603f, -157.1593f ;\n"
            + "\t\tlongitude:axis = \"X\" ;\n"
            + "\t\tlongitude:colorBarMaximum = 180.0 ;\n"
            + "\t\tlongitude:colorBarMinimum = -180.0 ;\n"
            + "\t\tlongitude:ioos_category = \"Location\" ;\n"
            + "\t\tlongitude:long_name = \"station longitude\" ;\n"
            + "\t\tlongitude:missing_value = -1.0E34f ;\n"
            + "\t\tlongitude:standard_name = \"longitude\" ;\n"
            + "\t\tlongitude:units = \"degrees_east\" ;\n"
            + "\tfloat lon360(row) ;\n"
            + "\t\tlon360:_FillValue = -1.0E34f ;\n"
            + "\t\tlon360:actual_range = 185.3397f, 202.8407f ;\n"
            + "\t\tlon360:colorBarMaximum = 180.0 ;\n"
            + "\t\tlon360:colorBarMinimum = -180.0 ;\n"
            + "\t\tlon360:long_name = \"station longitude 360\" ;\n"
            + "\t\tlon360:missing_value = -1.0E34f ;\n"
            + "\t\tlon360:standard_name = \"longitude\" ;\n"
            + "\t\tlon360:units = \"degrees_east\" ;\n"
            + "\tfloat latitude(row) ;\n"
            + "\t\tlatitude:_CoordinateAxisType = \"Lat\" ;\n"
            + "\t\tlatitude:_FillValue = -1.0E34f ;\n"
            + "\t\tlatitude:actual_range = 54.34184f, 73.11517f ;\n"
            + "\t\tlatitude:axis = \"Y\" ;\n"
            + "\t\tlatitude:colorBarMaximum = 90.0 ;\n"
            + "\t\tlatitude:colorBarMinimum = -90.0 ;\n"
            + "\t\tlatitude:history = \"From mb1101c070.nc_copy\" ;\n"
            + "\t\tlatitude:ioos_category = \"Location\" ;\n"
            + "\t\tlatitude:long_name = \"station latitude\" ;\n"
            + "\t\tlatitude:missing_value = -1.0E34f ;\n"
            + "\t\tlatitude:standard_name = \"latitude\" ;\n"
            + "\t\tlatitude:units = \"degrees_north\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:CAST = \"070\" ;\n"
            + "\t\t:cdm_data_type = \"Profile\" ;\n"
            + "\t\t:cdm_profile_variables = \"prof, id, cast, cruise, time, longitude, lon360, latitude\" ;\n"
            + "\t\t:Conventions = \"CF-1.6, COARDS, ACDD-1.3\" ;\n"
            + "\t\t:COORD_SYSTEM = \"GEOGRAPHICAL\" ;\n"
            + "\t\t:creation_date = \"14:48 21-JUN-2013\" ;\n"
            + "\t\t:creator_email = \"peggy.sullivan@noaa.gov\" ;\n"
            + "\t\t:creator_name = \"Phyllis Stabeno, Margaret Sullivan\" ;\n"
            + "\t\t:CRUISE = \"Ch2011\" ;\n"
            + "\t\t:DATA_CMNT = \"Data from Seasoft File chaoz2011070.cnv\" ;\n"
            + "\t\t:DATA_TYPE = \"CTD\" ;\n"
            + "\t\t:Easternmost_Easting = -157.1593 ;\n"
            + "\t\t:EPIC_FILE_GENERATOR = \"SEASOFT2EPIC_CTD (Version 1.37, 14-Oct-2011)\" ;\n"
            + "\t\t:featureType = \"Profile\" ;\n"
            + "\t\t:geospatial_lat_max = 73.11517 ;\n"
            + "\t\t:geospatial_lat_min = 54.34184 ;\n"
            + "\t\t:geospatial_lat_units = \"degrees_north\" ;\n"
            + "\t\t:geospatial_lon_max = -157.1593 ;\n"
            + "\t\t:geospatial_lon_min = -174.6603 ;\n"
            + "\t\t:geospatial_lon_units = \"degrees_east\" ;\n"
            + "\t\t:geospatial_vertical_max = 166.0 ;\n"
            + "\t\t:geospatial_vertical_min = 0.0 ;\n"
            + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
            + "\t\t:geospatial_vertical_units = \"m\" ;\n"
            + "\t\t:history = \"FERRET V7  13-Jun-16\n";
    // 2016-10-03T22:25:01Z (local files)
    // 2016-10-03T22:25:01Z
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "https://data.pmel.noaa.gov/pmel/erddap/tabledap/ChukchiSea_454a_037a_fcf4.das\" ;\n"
            + "\t\t:infoUrl = \"www.ecofoci.noaa.gov\" ;\n"
            + "\t\t:INST_TYPE = \"Sea-Bird CTD SBE911\" ;\n"
            + "\t\t:institution = \"PMEL EcoFOCI\" ;\n"
            + "\t\t:keywords = \"2010-2012, active, ammonia, ammonium, bottle, calibration, cast, chemistry, chlorophyll, chukchi, concentration, concentration_of_chlorophyll_in_sea_water, cooperative, cruise, data, density, depth, dissolved, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Salinity/Density > Salinity, ecofoci, environmental, factory, fisheries, fisheries-oceanography, flourescence, foci, fraction, fractional, fractional_saturation_of_oxygen_in_sea_water, investigations, laboratory, latitude, lon360, longitude, marine, ml/l, mmoles, mmoles/kg, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_dissolved_molecular_oxygen_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, molecular, n02, name, nh4, niskin, nitrate, nitrite, no2, no3, noaa, number, nutrients, O2, ocean, ocean_chlorophyll_a_concentration_factoryCal, ocean_chlorophyll_fluorescence_raw, ocean_dissolved_oxygen_concentration_1_mLperL, ocean_dissolved_oxygen_concentration_1_mMperkg, ocean_dissolved_oxygen_concentration_2_mLperL, ocean_dissolved_oxygen_concentration_2_mMperkg, ocean_oxygen_saturation_1, ocean_practical_salinity_1, ocean_practical_salinity_2, ocean_sigma_t, ocean_temperature_1, ocean_temperature_2, oceanography, oceans, oxygen, pacific, percent, phosphate, photosynthetically, photosynthetically_active_radiation, pmel, po4, practical, prof, profile, pss, pss-78, psu, radiation, raw, salinity, saturation, scale, sea, sea_water_ammonium_concentration, sea_water_nitrate_concentration, sea_water_nitrite_concentration, sea_water_nutrient_bottle_number, sea_water_phosphate_concentration, sea_water_practical_salinity, sea_water_silicate_concentration, seawater, sigma, sigma-t, silicate, station, temperature, time, unit, volume, volume_fraction_of_oxygen_in_sea_water, water\" ;\n"
            + "\t\t:keywords_vocabulary = \"GCMD Science Keywords\" ;\n"
            + "\t\t:license = \"Data Licenses / Data Usage Restrictions\n"
            + "The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\n"
            + "\n"
            + "Unless otherwise noted, data served through this ERDDAP server are \n"
            + "not quality controlled.  Users may need to do quality control when using \n"
            + "these data. These data are made available at the users own risk.\" ;\n"
            + "\t\t:Northernmost_Northing = 73.11517 ;\n"
            + "\t\t:PROG_CMNT1 = \"cat_ctd v1.36 06Aug2010\" ;\n"
            + "\t\t:PROG_CMNT2 = \"Variables Extrapolated from 2 db to 0\" ;\n"
            + "\t\t:sourceUrl = \"(local files)\" ;\n"
            + "\t\t:Southernmost_Northing = 54.34184 ;\n"
            + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v29\" ;\n"
            + "\t\t:STATION_NAME = \"Unimak3\" ;\n"
            + "\t\t:subsetVariables = \"prof, id, cast, cruise, time, longitude, lon360, latitude\" ;\n"
            + "\t\t:summary = \"Pacific Marine Environmental Laboratory (PMEL) Fisheries-Oceanography Cooperative Investigations (FOCI) Chukchi Sea. PMEL EcoFOCI data from a local source.\" ;\n"
            + "\t\t:time_coverage_end = \"2012-09-06T23:57:00Z\" ;\n"
            + "\t\t:time_coverage_start = \"2010-09-05T11:22:00Z\" ;\n"
            + "\t\t:title = \"PMEL EcoFOCI Chukchi Sea profile data, 2010-2012\" ;\n"
            + "\t\t:WATER_DEPTH = 0 ;\n"
            + "\t\t:WATER_MASS = \"A\" ;\n"
            + "\t\t:Westernmost_Easting = -174.6603 ;\n"
            + "}\n"
            + "prof,id,cast,cruise,time,longitude,lon360,latitude\n"
            + "1.0,aq1201c069,069,aq1201,1.34697456E9,-164.0447,195.9553,56.866\n"
            + "1.0,aq1201c070,070,aq1201,1.34697582E9,-164.049,195.951,56.864\n";
    int po = results.indexOf(expected.substring(0, 60));
    Test.ensureEqual(results.substring(Math.max(0, po)), expected, "results=\n" + results);

    url =
        "https://data.pmel.noaa.gov/pmel/erddap/tabledap/ChukchiSea_454a_037a_fcf4?"
            + "prof,id,cast,cruise,time,longitude,lon360,latitude&time%3E=2012-09-04&time%3C=2012-09-07";
    table.readOpendapSequence(url, false); // boolean: skipDapSpacerRows
    results = table.toString(3);
    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 2 ;\n"
            + "\tid_strlen = 10 ;\n"
            + "\tcast_strlen = 3 ;\n"
            + "\tcruise_strlen = 6 ;\n"
            + "variables:\n"
            + "\tdouble prof(row) ;\n"
            + "\t\tprof:actual_range = 1.0, 1.0 ;\n"
            + "\t\tprof:axis = \"E\" ;\n"
            + "\t\tprof:long_name = \"Prof\" ;\n"
            + "\t\tprof:point_spacing = \"even\" ;\n"
            + "\tchar id(row, id_strlen) ;\n"
            + "\t\tid:cf_role = \"profile_id\" ;\n"
            + "\t\tid:long_name = \"profile id\" ;\n"
            + "\tchar cast(row, cast_strlen) ;\n"
            + "\t\tcast:colorBarMaximum = 100.0 ;\n"
            + "\t\tcast:colorBarMinimum = 0.0 ;\n"
            + "\t\tcast:long_name = \"cast number\" ;\n"
            + "\tchar cruise(row, cruise_strlen) ;\n"
            + "\t\tcruise:long_name = \"Cruise name\" ;\n"
            + "\tdouble time(row) ;\n"
            + "\t\ttime:_CoordinateAxisType = \"Time\" ;\n"
            + "\t\ttime:actual_range = 1.28368572E9, 1.34697582E9 ;\n"
            + "\t\ttime:axis = \"T\" ;\n"
            + "\t\ttime:ioos_category = \"Time\" ;\n"
            + "\t\ttime:long_name = \"Time\" ;\n"
            + "\t\ttime:standard_name = \"time\" ;\n"
            + "\t\ttime:time_origin = \"01-JAN-1970 00:00:00\" ;\n"
            + "\t\ttime:units = \"seconds since 1970-01-01T00:00:00Z\" ;\n"
            + "\tfloat longitude(row) ;\n"
            + "\t\tlongitude:_CoordinateAxisType = \"Lon\" ;\n"
            + "\t\tlongitude:_FillValue = -1.0E34f ;\n"
            + "\t\tlongitude:actual_range = -174.6603f, -157.1593f ;\n"
            + "\t\tlongitude:axis = \"X\" ;\n"
            + "\t\tlongitude:colorBarMaximum = 180.0 ;\n"
            + "\t\tlongitude:colorBarMinimum = -180.0 ;\n"
            + "\t\tlongitude:ioos_category = \"Location\" ;\n"
            + "\t\tlongitude:long_name = \"station longitude\" ;\n"
            + "\t\tlongitude:missing_value = -1.0E34f ;\n"
            + "\t\tlongitude:standard_name = \"longitude\" ;\n"
            + "\t\tlongitude:units = \"degrees_east\" ;\n"
            + "\tfloat lon360(row) ;\n"
            + "\t\tlon360:_FillValue = -1.0E34f ;\n"
            + "\t\tlon360:actual_range = 185.3397f, 202.8407f ;\n"
            + "\t\tlon360:colorBarMaximum = 180.0 ;\n"
            + "\t\tlon360:colorBarMinimum = -180.0 ;\n"
            + "\t\tlon360:long_name = \"station longitude 360\" ;\n"
            + "\t\tlon360:missing_value = -1.0E34f ;\n"
            + "\t\tlon360:standard_name = \"longitude\" ;\n"
            + "\t\tlon360:units = \"degrees_east\" ;\n"
            + "\tfloat latitude(row) ;\n"
            + "\t\tlatitude:_CoordinateAxisType = \"Lat\" ;\n"
            + "\t\tlatitude:_FillValue = -1.0E34f ;\n"
            + "\t\tlatitude:actual_range = 54.34184f, 73.11517f ;\n"
            + "\t\tlatitude:axis = \"Y\" ;\n"
            + "\t\tlatitude:colorBarMaximum = 90.0 ;\n"
            + "\t\tlatitude:colorBarMinimum = -90.0 ;\n"
            + "\t\tlatitude:history = \"From mb1101c070.nc_copy\" ;\n"
            + "\t\tlatitude:ioos_category = \"Location\" ;\n"
            + "\t\tlatitude:long_name = \"station latitude\" ;\n"
            + "\t\tlatitude:missing_value = -1.0E34f ;\n"
            + "\t\tlatitude:standard_name = \"latitude\" ;\n"
            + "\t\tlatitude:units = \"degrees_north\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:CAST = \"070\" ;\n"
            + "\t\t:cdm_data_type = \"Profile\" ;\n"
            + "\t\t:cdm_profile_variables = \"prof, id, cast, cruise, time, longitude, lon360, latitude\" ;\n"
            + "\t\t:Conventions = \"CF-1.6, COARDS, ACDD-1.3\" ;\n"
            + "\t\t:COORD_SYSTEM = \"GEOGRAPHICAL\" ;\n"
            + "\t\t:creation_date = \"14:48 21-JUN-2013\" ;\n"
            + "\t\t:creator_email = \"peggy.sullivan@noaa.gov\" ;\n"
            + "\t\t:creator_name = \"Phyllis Stabeno, Margaret Sullivan\" ;\n"
            + "\t\t:CRUISE = \"Ch2011\" ;\n"
            + "\t\t:DATA_CMNT = \"Data from Seasoft File chaoz2011070.cnv\" ;\n"
            + "\t\t:DATA_TYPE = \"CTD\" ;\n"
            + "\t\t:Easternmost_Easting = -157.1593 ;\n"
            + "\t\t:EPIC_FILE_GENERATOR = \"SEASOFT2EPIC_CTD (Version 1.37, 14-Oct-2011)\" ;\n"
            + "\t\t:featureType = \"Profile\" ;\n"
            + "\t\t:geospatial_lat_max = 73.11517 ;\n"
            + "\t\t:geospatial_lat_min = 54.34184 ;\n"
            + "\t\t:geospatial_lat_units = \"degrees_north\" ;\n"
            + "\t\t:geospatial_lon_max = -157.1593 ;\n"
            + "\t\t:geospatial_lon_min = -174.6603 ;\n"
            + "\t\t:geospatial_lon_units = \"degrees_east\" ;\n"
            + "\t\t:geospatial_vertical_max = 166.0 ;\n"
            + "\t\t:geospatial_vertical_min = 0.0 ;\n"
            + "\t\t:geospatial_vertical_positive = \"down\" ;\n"
            + "\t\t:geospatial_vertical_units = \"m\" ;\n"
            + "\t\t:history = \"FERRET V7  13-Jun-16\n";
    // 2016-10-03T22:32:26Z (local files)
    // 2016-10-03T22:32:26Z
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "https://data.pmel.noaa.gov/pmel/erddap/tabledap/ChukchiSea_454a_037a_fcf4.das\" ;\n"
            + "\t\t:infoUrl = \"www.ecofoci.noaa.gov\" ;\n"
            + "\t\t:INST_TYPE = \"Sea-Bird CTD SBE911\" ;\n"
            + "\t\t:institution = \"PMEL EcoFOCI\" ;\n"
            + "\t\t:keywords = \"2010-2012, active, ammonia, ammonium, bottle, calibration, cast, chemistry, chlorophyll, chukchi, concentration, concentration_of_chlorophyll_in_sea_water, cooperative, cruise, data, density, depth, dissolved, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Salinity/Density > Salinity, ecofoci, environmental, factory, fisheries, fisheries-oceanography, flourescence, foci, fraction, fractional, fractional_saturation_of_oxygen_in_sea_water, investigations, laboratory, latitude, lon360, longitude, marine, ml/l, mmoles, mmoles/kg, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_dissolved_molecular_oxygen_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, molecular, n02, name, nh4, niskin, nitrate, nitrite, no2, no3, noaa, number, nutrients, O2, ocean, ocean_chlorophyll_a_concentration_factoryCal, ocean_chlorophyll_fluorescence_raw, ocean_dissolved_oxygen_concentration_1_mLperL, ocean_dissolved_oxygen_concentration_1_mMperkg, ocean_dissolved_oxygen_concentration_2_mLperL, ocean_dissolved_oxygen_concentration_2_mMperkg, ocean_oxygen_saturation_1, ocean_practical_salinity_1, ocean_practical_salinity_2, ocean_sigma_t, ocean_temperature_1, ocean_temperature_2, oceanography, oceans, oxygen, pacific, percent, phosphate, photosynthetically, photosynthetically_active_radiation, pmel, po4, practical, prof, profile, pss, pss-78, psu, radiation, raw, salinity, saturation, scale, sea, sea_water_ammonium_concentration, sea_water_nitrate_concentration, sea_water_nitrite_concentration, sea_water_nutrient_bottle_number, sea_water_phosphate_concentration, sea_water_practical_salinity, sea_water_silicate_concentration, seawater, sigma, sigma-t, silicate, station, temperature, time, unit, volume, volume_fraction_of_oxygen_in_sea_water, water\" ;\n"
            + "\t\t:keywords_vocabulary = \"GCMD Science Keywords\" ;\n"
            + "\t\t:license = \"Data Licenses / Data Usage Restrictions\n"
            + "The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\n"
            + "\n"
            + "Unless otherwise noted, data served through this ERDDAP server are \n"
            + "not quality controlled.  Users may need to do quality control when using \n"
            + "these data. These data are made available at the users own risk.\" ;\n"
            + "\t\t:Northernmost_Northing = 73.11517 ;\n"
            + "\t\t:PROG_CMNT1 = \"cat_ctd v1.36 06Aug2010\" ;\n"
            + "\t\t:PROG_CMNT2 = \"Variables Extrapolated from 2 db to 0\" ;\n"
            + "\t\t:sourceUrl = \"(local files)\" ;\n"
            + "\t\t:Southernmost_Northing = 54.34184 ;\n"
            + "\t\t:standard_name_vocabulary = \"CF Standard Name Table v29\" ;\n"
            + "\t\t:STATION_NAME = \"Unimak3\" ;\n"
            + "\t\t:subsetVariables = \"prof, id, cast, cruise, time, longitude, lon360, latitude\" ;\n"
            + "\t\t:summary = \"Pacific Marine Environmental Laboratory (PMEL) Fisheries-Oceanography Cooperative Investigations (FOCI) Chukchi Sea. PMEL EcoFOCI data from a local source.\" ;\n"
            + "\t\t:time_coverage_end = \"2012-09-06T23:57:00Z\" ;\n"
            + "\t\t:time_coverage_start = \"2010-09-05T11:22:00Z\" ;\n"
            + "\t\t:title = \"PMEL EcoFOCI Chukchi Sea profile data, 2010-2012\" ;\n"
            + "\t\t:WATER_DEPTH = 0 ;\n"
            + "\t\t:WATER_MASS = \"A\" ;\n"
            + "\t\t:Westernmost_Easting = -174.6603 ;\n"
            + "}\n"
            + "prof,id,cast,cruise,time,longitude,lon360,latitude\n"
            + "1.0,aq1201c069,069,aq1201,1.34697456E9,-164.0447,195.9553,56.866\n"
            + "1.0,aq1201c070,070,aq1201,1.34697582E9,-164.049,195.951,56.864\n";
    po = results.indexOf(expected.substring(0, 60));
    Test.ensureEqual(results.substring(Math.max(0, po)), expected, "results=\n" + results);

    // ************* SINGLE SEQUENCE *****************************
    // read data from opendap
    table.readOpendapSequence(
        "http://localhost:8080/erddap/tabledap/erdGlobecMoc1?abund_m3,latitude,longitude", false);
    results = table.toString(5);
    // String2.log(results);

    nRows = 3763; // 2013-0620 was 3779;
    Test.ensureEqual(table.nColumns(), 3, "");
    Test.ensureEqual(table.nRows(), nRows, "");

    expected =
        "{\n"
            + "dimensions:\n"
            + "\trow = 3763 ;\n"
            + "variables:\n"
            + "\tfloat abund_m3(row) ;\n"
            + "\t\tabund_m3:_FillValue = -9999999.0f ;\n"
            + "\t\tabund_m3:actual_range = 0.0f, 9852.982f ;\n"
            + "\t\tabund_m3:colorBarMaximum = 1000.0 ;\n"
            + "\t\tabund_m3:colorBarMinimum = 0.0 ;\n"
            + "\t\tabund_m3:comment = \"Density [individuals per m3]\" ;\n"
            + "\t\tabund_m3:ioos_category = \"Other\" ;\n"
            + "\t\tabund_m3:long_name = \"Abundance\" ;\n"
            + "\t\tabund_m3:missing_value = -9999999.0f ;\n"
            + "\t\tabund_m3:units = \"count m-3\" ;\n"
            + "\tfloat latitude(row) ;\n"
            + "\t\tlatitude:_CoordinateAxisType = \"Lat\" ;\n"
            + "\t\tlatitude:_FillValue = 214748.36f ;\n"
            + "\t\tlatitude:actual_range = 42.4733f, 44.6517f ;\n"
            + "\t\tlatitude:axis = \"Y\" ;\n"
            + "\t\tlatitude:ioos_category = \"Location\" ;\n"
            + "\t\tlatitude:long_name = \"Latitude\" ;\n"
            + "\t\tlatitude:missing_value = 214748.36f ;\n"
            + "\t\tlatitude:standard_name = \"latitude\" ;\n"
            + "\t\tlatitude:units = \"degrees_north\" ;\n"
            + "\tfloat longitude(row) ;\n"
            + "\t\tlongitude:_CoordinateAxisType = \"Lon\" ;\n"
            + "\t\tlongitude:_FillValue = 214748.36f ;\n"
            + "\t\tlongitude:actual_range = -125.1167f, -124.175f ;\n"
            + "\t\tlongitude:axis = \"X\" ;\n"
            + "\t\tlongitude:ioos_category = \"Location\" ;\n"
            + "\t\tlongitude:long_name = \"Longitude\" ;\n"
            + "\t\tlongitude:missing_value = 214748.36f ;\n"
            + "\t\tlongitude:standard_name = \"longitude\" ;\n"
            + "\t\tlongitude:units = \"degrees_east\" ;\n"
            + "\n"
            + "// global attributes:\n"
            + "\t\t:cdm_data_type = \"Trajectory\" ;\n"
            + "\t\t:cdm_trajectory_variables = \"cruise_id\" ;\n"
            + "\t\t:Conventions = \"COARDS, CF-1.6, ACDD-1.3\" ;\n"
            + "\t\t:Easternmost_Easting = -124.175 ;\n"
            + "\t\t:featureType = \"Trajectory\" ;\n"
            + "\t\t:geospatial_lat_max = 44.6517 ;\n"
            + "\t\t:geospatial_lat_min = 42.4733 ;\n"
            + "\t\t:geospatial_lat_units = \"degrees_north\" ;\n"
            + "\t\t:geospatial_lon_max = -124.175 ;\n"
            + "\t\t:geospatial_lon_min = -125.1167 ;\n"
            + "\t\t:geospatial_lon_units = \"degrees_east\" ;\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "\t\t:time_coverage_end = \"2002-05-30T15:22:00Z\" ;\n"
            + "\t\t:time_coverage_start = \"2000-04-12T04:00:00Z\" ;\n"
            + "\t\t:title = \"GLOBEC NEP MOCNESS Plankton (MOC1) Data, 2000-2002\" ;\n"
            + "\t\t:Westernmost_Easting = -125.1167 ;\n"
            + "}\n"
            +
            // " Row abund_m3 latitude longitude\n" +
            // " 0 3.698225E-3 44.651699 -124.650002\n" + 2013-06-20 was
            // " 1 7.26257E-2 44.651699 -124.650002\n" +
            // " 2 1.100231E-3 42.504601 -125.011299\n" +
            // " 3 7.889546E-2 42.501801 -124.705803\n" +
            // " 4 3.416457 42.5033 -124.845001\n";
            "abund_m3,latitude,longitude\n"
            + "0.003688676,44.6517,-124.65\n"
            + "0.003688676,44.6517,-124.65\n"
            + "0.011066027,44.6517,-124.65\n"
            + "0.014754703,44.6517,-124.65\n"
            + "0.014754703,44.6517,-124.65\n"
            + "...\n";
    po = results.indexOf(expected.substring(0, 19));
    Test.ensureEqual(results.substring(Math.max(po, 0)), expected, "results=\n" + results);
    /*
     * on oceanwatch, was
     * "{\n" +
     * "dimensions:\n" +
     * "\trow = 3779 ;\n" +
     * "variables:\n" +
     * "\tfloat latitude(row) ;\n" +
     * "\t\tlat:long_name = \"Latitude\" ;\n" +
     * "\tfloat long(row) ;\n" +
     * "\t\tlong:long_name = \"Longitude\" ;\n" +
     * "\tfloat abund_m3(row) ;\n" +
     * "\t\tabund_m3:long_name = \"Abundance m3\" ;\n" +
     * "\n" +
     * "// global attributes:\n" +
     * "}\n" +
     * "    Row             lat           long       abund_m3\n" +
     * "      0       44.651699    -124.650002     3.69822E-3\n" +
     * "      1       44.651699    -124.650002     7.26257E-2\n" +
     * "      2       42.504601    -125.011002     1.10023E-3\n" +
     * "      3       42.501801    -124.706001     7.88955E-2\n" +
     * "      4         42.5033    -124.845001        3.41646\n";
     */

    url = "http://localhost:8080/erddap/tabledap/erdGlobecVpt?station_id&distinct()";
    table.readOpendapSequence(url, false);
    // String2.log(table.toString(3));
    // source has no global metadata
    Test.ensureEqual(table.nColumns(), 1, "");
    Test.ensureEqual(table.nRows(), 77, "");
    Test.ensureEqual(table.getColumnName(0), "station_id", "");
    Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Station ID", "");
    Test.ensureEqual(table.getStringData(0, 0), "BO01", "");
    Test.ensureEqual(table.getStringData(0, 1), "BO02", "");

    url = "http://localhost:8080/erddap/tabledap/erdGlobecVpt?abund_m3&station_id=%22NH05%22";
    table.readOpendapSequence(url, false);
    // String2.log(table.toString(3));
    // source has no global metadata
    Test.ensureEqual(table.nColumns(), 1, "");
    Test.ensureEqual(table.nRows(), 2400, "");
    Test.ensureEqual(table.getColumnName(0), "abund_m3", "");
    Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Abundance", "");
    Test.ensureEqual(table.getFloatData(0, 0), 11.49f, "");
    Test.ensureEqual(table.getFloatData(0, 1), 74.720001f, "");

    url = "http://localhost:8080/erddap/tabledap/erdGlobecBottle?cruise_id&distinct()";
    table.readOpendapSequence(url, false);
    // String2.log(table.toString(1000000));
    // source has no global metadata
    Test.ensureEqual(table.nColumns(), 1, "");
    Test.ensureEqual(table.nRows(), 2, "");
    Test.ensureEqual(table.getColumnName(0), "cruise_id", "");
    Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Cruise ID", "");
    Test.ensureEqual(table.getStringData(0, 0), "nh0207", "");
    Test.ensureEqual(table.getStringData(0, 1), "w0205", "");

    url =
        "http://localhost:8080/erddap/tabledap/erdGlobecMoc1?abund_m3,latitude,longitude&program=%22MESO_1%22";
    table.readOpendapSequence(url, false);
    results = table.dataToString();
    // String2.log(results);
    expected =
        /*
         * oceanwatch was
         * ...
         * "    Row             lat           long       abund_m3\n" +
         * "      0       44.651699    -124.650002        10.7463\n" +
         * "      1       44.651699    -124.650002     1.40056E-2\n" +
         * "      2       44.651699    -124.650002       0.252101\n";
         */
        "abund_m3,latitude,longitude\n"
            +
            // "10.746269,44.6517,-124.65\n" + //pre 2013-06-20 was
            // "0.014005602,44.6517,-124.65\n" +
            // "0.25210083,44.6517,-124.65\n";
            "0.003688676,44.6517,-124.65\n"
            + "0.003688676,44.6517,-124.65\n"
            + "0.011066027,44.6517,-124.65\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // test getting all data
    // modified from above so it returns lots of data
    // nRows=16507 nColumns=28 readTime=5219 ms processTime=94 ms
    url = "http://localhost:8080/erddap/tabledap/erdGlobecVpt";
    table.readOpendapSequence(url, false);
    results = table.dataToString(5);
    // on oceanwatch, was
    // Row datetime datetime_utc datetime_utc_e year program cruise_id cast_no
    // stn_id
    // lat long lat1000 lon1000 water_depth sample_id min_sample_dep max_sample_dep
    // month_local day_local
    // time_local d_n_flag gear_type gear_area_m2 gear_mesh vol_filt counter_id
    // comments perc_counted lo
    // cal_code nodc_code genus_species life_stage abund_m3
    // 0 2001-04-25 21: 2001-04-25 22: 988261731 2001 NH EL010403 2 NH05 44.650
    // 002 -124.169998 44650 -124170 60 1 0 55 4 25
    // -9999 -9999 VPT 0.19635 0.202 14.46 WTP -9999 1.1 611
    // 8010204# 6118010204 CALANUS_MARSHA 3;_CIII 11.49
    expected =
        "cruise_id,longitude,latitude,time,cast_no,station_id,abund_m3,comments,counter_id,d_n_flag,gear_area,gear_mesh,gear_type,genus_species,life_stage,local_code,max_sample_depth,min_sample_depth,nodc_code,perc_counted,program,sample_id,vol_filt,water_depth\n"
            + "EL010403,-124.17,44.65,9.88261731E8,0,NH05,11.49,-9999,WTP,-9999,0.19635,0.202,VPT,CALANUS_MARSHALLAE,3;_CIII,6118010204#,55,0,6118010204,1.1,NH,0,14.46,60\n"
            + "EL010403,-124.17,44.65,9.88261731E8,0,NH05,74.72,-9999,WTP,-9999,0.19635,0.202,VPT,BIVALVIA,Veliger,55V,55,0,55,1.1,NH,0,14.46,60\n"
            + "EL010403,-124.17,44.65,9.88261731E8,0,NH05,57.48,-9999,WTP,-9999,0.19635,0.202,VPT,POLYCHAETA,Larva,5001LV,55,0,5001,1.1,NH,0,14.46,60\n"
            + "EL010403,-124.17,44.65,9.88261731E8,0,NH05,74.72,-9999,WTP,-9999,0.19635,0.202,VPT,GASTROPODA,Veliger,51V,55,0,51,1.1,NH,0,14.46,60\n"
            + "EL010403,-124.17,44.65,9.88261731E8,0,NH05,11.49,-9999,WTP,-9999,0.19635,0.202,VPT,CALANUS_MARSHALLAE,1;_CI,6118010204!,55,0,6118010204,1.1,NH,0,14.46,60\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // *************
    // String2.log("\n*** Table.testOpendapSequence subset data via tests");

    // read data from opendap
    table = new Table();
    table.readOpendapSequence(
        // resulting url (for asc) is:
        // https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_MOC1.asc?abund_m3,lat,long&abund_m3>=0.248962651&abund_m3<=0.248962653
        // Opera browser changes > to %3E and < to %3C
        "http://localhost:8080/erddap/tabledap/erdGlobecMoc1",
        new String[] {"abund_m3"},
        new double[] {0.24896}, // new double[]{0.248962651}, //the last 2 rows only
        new double[] {0.24897}, // new double[]{0.248962653},
        new String[] {"abund_m3", "latitude", "longitude"},
        false);

    results = table.dataToString();
    expected =
        "abund_m3,latitude,longitude\n"
            + "0.24896266,44.6517,-124.65\n"
            + "0.24896266,44.6517,-124.65\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // ************* TWO-LEVEL SEQUENCE *****************************
    /*
     * 2014-08-06 TEST REMOVED BECAUSE dataset is gone
     *
     *
     * //note that I was UNABLE to get .asc responses for these dapper urls while
     * poking around.
     * //but straight dods request (without ".dods") works.
     * //I'm testing Lynn's old ndbc data because I can independently generate test
     * info
     * // from my ndbc files.
     * //Starting url from Roy: http://las.pfeg.noaa.gov/dods/
     * //See in DChart: http://las.pfeg.noaa.gov/dchart
     * //Test info from my cached ndbc file
     * /u00/data/points/ndbcMet2HistoricalTxt/46022h2004.txt
     * //YYYY MM DD hh WD WSPD GST WVHT DPD APD MWD BAR ATMP WTMP DEWP VIS TIDE
     * //2004 01 01 00 270 2.0 3.1 3.11 16.67 9.80 999 1010.7 999.0 999.0 999.0 99.0
     * 99.00
     * //2004 01 01 01 120 5.9 7.3 3.29 16.67 10.12 999 1010.4 999.0 999.0 999.0
     * 99.0 99.00
     * //test attributes are from
     * "http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp.das"
     * //http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp?location.LON,
     * location.LAT,location.DEPTH,location.profile.TIME,location.profile.WSPD1,
     * location.profile.BAR1&location.LON>=235.3&location.LON<=235.5&location.LAT>=
     * 40.7&location.LAT<=40.8&location.profile.TIME>=1072915200000&location.profile
     * .TIME<=1072920000000
     * lon = 235.460007f; //exact values from just get LON and LAT values available
     * lat = 40.779999f;
     * long time1 = Calendar2.newGCalendarZulu(2004, 1, 1).getTimeInMillis();
     * long time2 = time1 + Calendar2.MILLIS_PER_HOUR;
     * //was http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp
     * //was http://las.pfeg.noaa.gov/dods/ndbcMet/ndbcMet_time_series.cdp?" +
     * url = "http://oceanview.pfeg.noaa.gov/dods/ndbcMet/ndbcMet_time_series.cdp?"
     * +
     * "location.LON,location.LAT,location.DEPTH,location.profile.TIME,location.profile.WSPD,location.profile.BAR"
     * +
     * "&location.LON>=" + (lon - .01f) + "&location.LON<=" + (lon + .01f) + //I
     * couldn't catch lon with "="
     * "&location.LAT>=" + (lat - .01f) + "&location.LAT<=" + (lat + .01f) + //I
     * couldn't catch lat with "="
     * "&location.profile.TIME>=" + (time1 - 1) +
     * "&location.profile.TIME<=" + (time2 + 1);
     * String2.log("url=" + url);
     * table.readOpendapSequence(url, false);
     * String2.log(table.toString());
     * Test.ensureEqual(table.nColumns(), 6, "");
     * Test.ensureEqual(table.nRows(), 2, "");
     * int latCol = table.findColumnNumber("LAT");
     * int lonCol = table.findColumnNumber("LON");
     * Test.ensureTrue(latCol >= 0 && latCol < 2, "latCol=" + latCol);
     * Test.ensureTrue(lonCol >= 0 && lonCol < 2, "lonCol=" + lonCol);
     * Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
     * Test.ensureEqual(table.getColumnName(3), "TIME", "");
     * int barCol = table.findColumnNumber("BAR");
     * int wspdCol = table.findColumnNumber("WSPD");
     * Test.ensureEqual(table.getColumn(latCol).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(3).elementTypeString(), "double", "");
     * Test.ensureEqual(table.getColumn(wspdCol).elementTypeString(), "float", "");
     * Test.ensureEqual(table.globalAttributes().getString("Conventions"),
     * "epic-insitu-1.0", "");
     * Test.ensureEqual(table.globalAttributes().get("lat_range").toString(),
     * "-27.7000007629395, 70.4000015258789", "");
     * Test.ensureEqual(table.getFloatData(latCol, 0), lat, "");
     * Test.ensureEqual(table.getFloatData(latCol, 1), lat, "");
     * Test.ensureEqual(table.getFloatData(lonCol, 0), lon, "");
     * Test.ensureEqual(table.getFloatData(lonCol, 1), lon, "");
     * //outer attributes...
     * Test.ensureEqual(table.columnAttributes(latCol).getString("units"),
     * "degrees_north", "");
     * Test.ensureEqual(table.columnAttributes(latCol).getString("long_name"),
     * "latitude", "");
     * Test.ensureEqual(table.columnAttributes(latCol).getDouble("missing_value"),
     * Double.NaN, "");
     * Test.ensureEqual(table.columnAttributes(latCol).getString("axis"), "Y", "");
     * Test.ensureEqual(table.getDoubleData(3, 0), time1, "");
     * Test.ensureEqual(table.getDoubleData(3, 1), time2, "");
     * //inner attributes...
     * Test.ensureEqual(table.columnAttributes(3).getString("units"),
     * "msec since 1970-01-01 00:00:00 GMT", "");
     * Test.ensureEqual(table.columnAttributes(3).getString("long_name"), "time",
     * "");
     * Test.ensureEqual(table.columnAttributes(3).getDouble("missing_value"),
     * Double.NaN, "");
     * Test.ensureEqual(table.columnAttributes(3).getString("axis"), "T", "");
     * Test.ensureEqual(table.getFloatData(barCol, 0), 1010.7f, ""); //bar
     * Test.ensureEqual(table.getFloatData(barCol, 1), 1010.4f, "");
     * Test.ensureEqual(table.getFloatData(wspdCol, 0), 2.0f, ""); //wspd
     * Test.ensureEqual(table.getFloatData(wspdCol, 1), 5.9f, "");
     *
     */

    /*
     * 2014-08-06 INACTIVE BECAUSE DATASET NO LONGER AVAILABLE
     * //This Calcofi test simply verifies that the results now are as they were
     * when
     * // I wrote the test (circular logic).
     * //I hope this test is better than ndbc test above,
     * // since hopefully longer lived (since ndbc data may not be around forever).
     * //target data
     * // Row time lat lon depth English_sole_LarvaeCount
     * // 10 947320140000 32.341667 241.445007 203.800003 NaN
     * // 11 947320140000 32.341667 241.445007 NaN NaN
     * lat = 32.341667f;
     * lon = 241.445f;
     * long time = 947320140000L;
     * //Starting url from roy: http://las.pfeg.noaa.gov/dods/
     * //see info via url without query, but plus .dds or .das
     * //was las.pfeg.noaa.gov
     * url = "http://oceanview.pfeg.noaa.gov/dods/CalCOFI/Biological.cdp?" +
     * "location.lon,location.lat,location.time,location.profile.depth,location.profile.Line,location.profile.Disintegrated_fish_larvae_LarvaeCount"
     * +
     * "&location.lon>=" + (lon - .01f) + "&location.lon<=" + (lon + .01f) + //I
     * couldn't catch lon with "="
     * "&location.lat>=" + (lat - .01f) + "&location.lat<=" + (lat + .01f) + //I
     * couldn't catch lat with "="
     * "&location.time>=" + (time - 1);
     * table.readOpendapSequence(url, false);
     *
     * String2.log(table.toString());
     * int latCol = table.findColumnNumber("lat");
     * int lonCol = table.findColumnNumber("lon");
     * Test.ensureTrue(latCol >= 0, "latCol=" + latCol);
     * Test.ensureTrue(lonCol >= 0, "lonCol=" + lonCol);
     * Test.ensureEqual(table.nColumns(), 6, "");
     * Test.ensureEqual(table.nRows(), 31, "");
     * Test.ensureEqual(table.getColumnName(0), "time", ""); //not in order I
     * requested! they are in dataset order
     * Test.ensureEqual(table.getColumnName(latCol), "lat", "");
     * Test.ensureEqual(table.getColumnName(lonCol), "lon", "");
     * Test.ensureEqual(table.getColumnName(3), "Line", "");
     * Test.ensureEqual(table.getColumnName(4),
     * "Disintegrated_fish_larvae_LarvaeCount", "");
     * Test.ensureEqual(table.getColumnName(5), "depth", "");
     * Test.ensureEqual(table.getColumn(0).elementTypeString(), "double", "");
     * Test.ensureEqual(table.getColumn(latCol).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(lonCol).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(3).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(4).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(5).elementTypeString(), "float", "");
     * //global attributes
     * Test.ensureEqual(table.globalAttributes().getString("Conventions"),
     * "epic-insitu-1.0", "");
     * Test.ensureEqual(table.globalAttributes().getInt("total_profiles_in_dataset")
     * , 6407, "");
     * //test of outer attributes
     * Test.ensureEqual(table.columnAttributes(0).getString("units"),
     * "msec since 1970-01-01 00:00:00 GMT", "");
     * Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "time",
     * "");
     * Test.ensureEqual(table.columnAttributes(0).getDouble("missing_value"),
     * Double.NaN, "");
     * Test.ensureEqual(table.columnAttributes(0).getString("axis"), "T", "");
     * //test of inner attributes
     * Test.ensureEqual(table.columnAttributes(4).getString("long_name"),
     * "disintegrated fish larvae larvae count", "");
     * Test.ensureEqual(table.columnAttributes(4).getFloat("missing_value"),
     * Float.NaN, "");
     * Test.ensureEqual(table.columnAttributes(4).getString("units"),
     * "number of larvae", "");
     *
     * //test of results
     * //NOTE that data from different inner sequences is always separated by a row
     * of NaNs
     * // in the ending inner sequence's info.
     * // I believe Dapper is doing this. See more comments below.
     * // Row time lat lon Disintegrated_ Line depth
     * // 0 947320140000 32.341667 241.445007 NaN 93.300003 203.800003
     * // 1 947320140000 32.341667 241.445007 NaN NaN NaN
     * // 2 955184100000 32.348331 241.448334 NaN 93.300003 215.800003
     * // 30 1099482480000 32.345001 241.445007 NaN 93.300003 198.899994
     * Test.ensureEqual(table.getDoubleData(0, 0), 947320140000L, "");
     * Test.ensureEqual(table.getFloatData(latCol, 0), 32.341667f, "");
     * Test.ensureEqual(table.getFloatData(lonCol, 0), 241.445007f, "");
     * Test.ensureEqual(table.getFloatData(3, 0), 93.300003f, "");
     * Test.ensureEqual(table.getFloatData(4, 0), Float.NaN, "");
     * Test.ensureEqual(table.getFloatData(5, 0), 203.800003f, "");
     *
     * Test.ensureEqual(table.getDoubleData(0, 1), 947320140000L, "");
     * Test.ensureEqual(table.getFloatData(latCol, 1), 32.341667f, "");
     * Test.ensureEqual(table.getFloatData(lonCol, 1), 241.445007f, "");
     * Test.ensureEqual(table.getFloatData(3, 1), Float.NaN, "");
     * Test.ensureEqual(table.getFloatData(4, 1), Float.NaN, "");
     * Test.ensureEqual(table.getFloatData(5, 1), Float.NaN, "");
     *
     * Test.ensureEqual(table.getDoubleData(0, 30), 1099482480000L, "");
     * Test.ensureEqual(table.getFloatData(latCol, 30), 32.345001f, "");
     * Test.ensureEqual(table.getFloatData(lonCol, 30), 241.445007f, "");
     * Test.ensureEqual(table.getFloatData(3, 30), 93.300003f, "");
     * Test.ensureEqual(table.getFloatData(4, 30), Float.NaN, "");
     * Test.ensureEqual(table.getFloatData(5, 30), 198.899994f, "");
     *
     *
     * //*** visual test: is dapper returning the NAN row at the end of every
     * innerSequence (true)
     * // or is that the way it is in the files? (false)
     * lon = 235.460007f; //exact values from just get LON and LAT values available
     * lat = 40.779999f;
     * long time1 = Calendar2.newGCalendarZulu(2004, 1, 1).getTimeInMillis();
     * long time2 = time1 + Calendar2.MILLIS_PER_HOUR;
     * //was http://las.pfeg.noaa.gov/dods/ndbc/all_noaa_time_series.cdp
     * url = "http://oceanview.pfeg.noaa.gov/dods/ndbcMet/ndbcMet_time_series.cdp?"
     * +
     * "location.LON,location.LAT,location.DEPTH,location.profile.TIME,location.profile.WSPD,location.profile.BAR"
     * +
     * "&location.LON>=" + (lon - 5f) + "&location.LON<=" + (lon + 5f) +
     * "&location.LAT>=" + (lat - 5f) + "&location.LAT<=" + (lat + 5f) +
     * "&location.profile.TIME>=" + (time1 - 1) +
     * "&location.profile.TIME<=" + (time2 + 1);
     * table.readOpendapSequence(url, false);
     * String2.log(table.toString());
     */

    /*
     * //THIS WORKS, BUT TAKES ~40 SECONDS!!! so don't do all the time
     * //see questions below.
     * //This gets all the valid English_sole_LarvaeCount data.
     * //UNFORTUNATELY, you can't put constraint on non-axis variable,
     * // so I have to get all data and then filter the results.
     * //This test simply verifies that the results now are as they were when
     * // I wrote the test (circular logic).
     * //I had hoped this test would be better than ndbc test above,
     * // since hopefully longer lived (since ndbc data may not be around forever).
     * //Starting url from roy: http://las.pfeg.noaa.gov/dods/
     * url = "http://las.pfeg.noaa.gov/dods/CalCOFI/Biological.cdp?" +
     * "location.lon,location.lat,location.time,location.profile.depth,location.profile.English_sole_LarvaeCount";
     * table.readOpendapSequence(url);
     * String2.log("raw results nRows=" + table.nRows());
     * //just keep rows with larvaeCounts >= 0
     * table.subset(new int[]{4}, new double[]{0}, new double[]{1e300});
     *
     * String2.log(table.toString());
     * Test.ensureEqual(table.nColumns(), 5, "");
     * Test.ensureEqual(table.nRows(), 98, "");
     * Test.ensureEqual(table.getColumnName(0), "time", ""); //not in order I
     * requested! they are in dataset order
     * Test.ensureEqual(table.getColumnName(1), "lat", "");
     * Test.ensureEqual(table.getColumnName(2), "lon", "");
     * Test.ensureEqual(table.getColumnName(3), "depth", "");
     * Test.ensureEqual(table.getColumnName(4), "English_sole_LarvaeCount", "");
     * Test.ensureEqual(table.getColumn(0).elementTypeString(), "double", "");
     * Test.ensureEqual(table.getColumn(1).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(2).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(3).elementTypeString(), "float", "");
     * Test.ensureEqual(table.getColumn(4).elementTypeString(), "float", "");
     * //global attributes
     * Test.ensureEqual(table.globalAttributes().getString("Conventions"),
     * "epic-insitu-1.0", "");
     * Test.ensureEqual(table.globalAttributes().getInt("total_profiles_in_dataset")
     * , 6407, "");
     * //test of outer attributes
     * Test.ensureEqual(table.columnAttributes(0).getString("units"),
     * "msec since 1970-01-01 00:00:00 GMT", "");
     * Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "time",
     * "");
     * Test.ensureEqual(table.columnAttributes(0).getDouble("missing_value"),
     * Double.NaN, "");
     * Test.ensureEqual(table.columnAttributes(0).getString("axis"), "T", "");
     * //test of inner attributes
     * Test.ensureEqual(table.columnAttributes(4).getString("long_name"),
     * "parophrys vetulus larvae count", "");
     * Test.ensureEqual(table.columnAttributes(4).getFloat("missing_value"),
     * Float.NaN, "");
     * Test.ensureEqual(table.columnAttributes(4).getString("units"),
     * "number of larvae", "");
     *
     * //test of results
     * Test.ensureEqual(table.getDoubleData(0, 0), 955657380000L, "");
     * Test.ensureEqual(table.getFloatData(1, 0), 33.485001f, "");
     * Test.ensureEqual(table.getFloatData(2, 0), 242.231659f, "");
     * Test.ensureEqual(table.getFloatData(3, 0), 210.800003f, "");
     * Test.ensureEqual(table.getFloatData(4, 0), 1, "");
     *
     * Test.ensureEqual(table.getDoubleData(0, 1), 955691700000L, "");
     * Test.ensureEqual(table.getFloatData(1, 1), 33.825001f, "");
     * Test.ensureEqual(table.getFloatData(2, 1), 241.366669f, "");
     * Test.ensureEqual(table.getFloatData(3, 1), 208.5f, "");
     * Test.ensureEqual(table.getFloatData(4, 1), 6, "");
     *
     * Test.ensureEqual(table.getDoubleData(0, 97), 923900040000L, "");
     * Test.ensureEqual(table.getFloatData(1, 97), 34.976665f, "");
     * Test.ensureEqual(table.getFloatData(2, 97), 237.334991f, "");
     * Test.ensureEqual(table.getFloatData(3, 97), 24.1f, "");
     * Test.ensureEqual(table.getFloatData(4, 97), 4, "");
     *
     * /*
     */
    /**
     * [Bob talked to Lynn about this. Conclusions below.] 1) If I just get all lon,lat,time,depth,
     * and English_sole_LarvaeCount (was http://las.pfeg...) url =
     * "http://oceanview.pfeg.noaa.gov/dods/CalCOFI/Biological.cdp?" +
     * "location.lon,location.lat,location.time,location.profile.depth,location.profile.English_sole_LarvaeCount";
     * it looks like each time,lat,lon combo has a data row and a NaN row. ???Is this a real NaN
     * row, or a mistake in my code (e.g., end of sequence beginning of next). [I believe it is real
     * and added by Dapper.] Note that some sole counts below are non-NaN.
     *
     * <p>Row time lat lon depth English_sole_LarvaeCount 0 947255160000 32.955002 242.695007 71.5
     * NaN 1 947255160000 32.955002 242.695007 NaN NaN 2 947264520000 32.913334 242.606659
     * 207.600006 NaN 3 947264520000 32.913334 242.606659 NaN NaN 4 947275680000 32.848335
     * 242.471664 211.699997 NaN 5 947275680000 32.848335 242.471664 NaN NaN 6 947290920000 32.68
     * 242.126663 195.899994 NaN 7 947290920000 32.68 242.126663 NaN NaN 8 947306040000 32.513332
     * 241.790009 208.100006 NaN 9 947306040000 32.513332 241.790009 NaN NaN 10 947320140000
     * 32.341667 241.445007 203.800003 NaN 11 947320140000 32.341667 241.445007 NaN NaN 12
     * 947343360000 32.18 241.110001 209.5 NaN 13 947343360000 32.18 241.110001 NaN NaN 14
     * 947359140000 32.006668 240.764999 215.5 NaN 15 947359140000 32.006668 240.764999 NaN NaN 2)
     * do all time,lat,lon combo's just have one depth? If so, then why set up this way? Just to
     * match dapper convention (must have z or t outside and t or z inside)? [I believe so.]
     *
     * <p>3) Since it appears that the 150(?) variables were only measured rarely, it seems hugely
     * wasteful to allocate space for them. And worse, since a query use constraints on non-axis
     * variables, one can't simply ask for ... &English_sole_LarvaeCount>=0 to find
     * time,lat,lon,depth where there are valid values of English_sole_LarvaeCount. And requesting
     * all data rows (so I can then filtering on my end) takes ~40 seconds for 98 rows of data.
     * [Wasteful, but I believe Roy did it this way to conform to Dapper Conventions so data easily
     * served by Dapper/DChart, see http://oceanview.pfeg.noaa.gov/dchart (was http://las.pfeg...).]
     *
     * <p>4) There are no 0 values for English_sole_LarvaeCount. So how can one tell if people
     * looked for English_sole_Larvae but didn't find any? Are NaN's to be treated as 0 for this
     * data set? [It looks like 0 values are lumped in with NaNs.]
     *
     * <p>5) Why is number of larvae (units="number of larvae") a float and not an int? [Because all
     * variables are floats for simplicity (maybe for matlab or fortran).]
     */
  }

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
    Test.ensureEqual(
        results,
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"Time\", \"Longitude\", \"Latitude\", \"Double Data\", \"Long Data\", \"Int Data\", \"Short Data\", \"Byte Data\", \"Char Data\", \"String Data\"],\n"
            + "    \"columnTypes\": [\"String\", \"int\", \"float\", \"double\", \"long\", \"int\", \"short\", \"byte\", \"char\", \"String\"],\n"
            + "    \"columnUnits\": [\"UTC\", \"degrees_east\", \"degrees_north\", \"doubles\", \"longs\", \"ints\", \"shorts\", \"bytes\", \"chars\", \"Strings\"],\n"
            + "    \"rows\": [\n"
            + "      [\"1970-01-01T00:00:00Z\", -3, 1.0, -1.0E300, -2000000000000000, -2000000000, -32000, -120, \",\", \"a\"],\n"
            + "      [\"2005-08-31T16:01:02Z\", -2, 1.5, 3.123, 2, 2, 7, 8, \"\\\"\", \"bb\"],\n"
            + "      [\"2005-11-02T18:04:09Z\", -1, 2.0, 1.0E300, 2000000000000000, 2000000000, 32000, 120, \"\\u20ac\", \"ccc\"],\n"
            + "      [null, null, null, null, null, null, null, null, \"\", \"\"]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n",
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
    Test.ensureEqual(
        table.globalAttributes().getString("title"),
        "TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature",
        ncHeader);
    Test.ensureEqual(table.globalAttributes().get("history").size(), 3, ncHeader);
    Test.ensureEqual(
        table.globalAttributes().get("history").getString(0),
        "This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.",
        ncHeader);
    Test.ensureEqual(
        table.globalAttributes().get("history").getString(1),
        "This dataset is a product of the TAO Project Office at NOAA/PMEL.",
        ncHeader);
    Test.ensureLinesMatch(
        table.globalAttributes().get("history").getString(2),
        "20\\d{2}-\\d{2}-\\d{2} Bob Simons at NOAA/NMFS/SWFSC/ERD \\(bob.simons@noaa.gov\\) "
            + "fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  "
            + "Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.",
        ncHeader);
    Test.ensureEqual(table.nColumns(), 10, ncHeader);
    Test.ensureEqual(table.findColumnNumber("longitude"), 3, ncHeader);
    Test.ensureEqual(table.columnAttributes(3).getString("units"), "degrees_east", ncHeader);
    int t25Col = table.findColumnNumber("T_25");
    Test.ensureTrue(t25Col > 0, ncHeader);
    Test.ensureEqual(
        table.columnAttributes(t25Col).getString("ioos_category"), "Temperature", ncHeader);
    Test.ensureEqual(table.columnAttributes(t25Col).getString("units"), "degree_C", ncHeader);
  }

  /* FileVisitorDNLS */

  /** This tests a WAF-related (Web Accessible Folder) methods on an ERDDAP "files" directory. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testErddapFilesWAF() throws Throwable {
    String2.log("\n*** FileVisitorDNLS.testErddapFilesWAF()\n");

    // test with trailing /
    // This also tests redirect to https!
    String url = "http://localhost:" + PORT + "/erddap/files/fedCalLandings/";
    String tFileNameRegex = "194\\d\\.nc";
    boolean tRecursive = true;
    String tPathRegex = ".*/(3|4)/.*";
    boolean tDirsToo = true;
    Table table = FileVisitorDNLS.makeEmptyTable();
    StringArray dirs = (StringArray) table.getColumn(0);
    StringArray names = (StringArray) table.getColumn(1);
    LongArray lastModifieds = (LongArray) table.getColumn(2);
    LongArray sizes = (LongArray) table.getColumn(3);
    String results, expected;
    Table tTable;

    // * test all features
    results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url,
            tFileNameRegex,
            tRecursive,
            tPathRegex,
            tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    table.removeColumn("lastModified");
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,,\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1940.nc,14916\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1941.nc,17380\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1942.nc,20548\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1943.nc,17280\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1944.nc,12748\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1945.nc,15692\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1946.nc,17028\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1947.nc,11576\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1948.nc,12876\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1949.nc,15268\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,,\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1940.nc,285940\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1941.nc,337768\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1942.nc,298608\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1943.nc,175940\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1944.nc,215864\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1945.nc,195056\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1946.nc,239444\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1947.nc,190272\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1948.nc,263084\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1949.nc,352240\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    Test.ensureEqual(Calendar2.epochSecondsToIsoStringTZ(1262881740), "2010-01-07T16:29:00Z", "");

    // test via oneStep
    tTable = FileVisitorDNLS.oneStep(url, tFileNameRegex, tRecursive, tPathRegex, tDirsToo);
    tTable.removeColumn("lastModified");
    results = tTable.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);

    // * test !dirsToo
    table.removeAllRows();
    results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url, tFileNameRegex, tRecursive, tPathRegex, false, dirs, names, lastModifieds, sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1940.nc,14916\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1941.nc,17380\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1942.nc,20548\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1943.nc,17280\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1944.nc,12748\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1945.nc,15692\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1946.nc,17028\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1947.nc,11576\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1948.nc,12876\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1949.nc,15268\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1940.nc,285940\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1941.nc,337768\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1942.nc,298608\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1943.nc,175940\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1944.nc,215864\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1945.nc,195056\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1946.nc,239444\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1947.nc,190272\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1948.nc,263084\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1949.nc,352240\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep
    tTable = FileVisitorDNLS.oneStep(url, tFileNameRegex, tRecursive, tPathRegex, false);
    tTable.removeColumn("lastModified");
    results = tTable.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);

    // * test subdir
    table.removeAllRows();
    results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url + "3", // test no trailing /
            tFileNameRegex,
            tRecursive,
            tPathRegex,
            tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    results = table.dataToString();
    expected =
        "directory,name,size\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1940.nc,14916\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1941.nc,17380\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1942.nc,20548\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1943.nc,17280\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1944.nc,12748\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1945.nc,15692\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1946.nc,17028\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1947.nc,11576\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1948.nc,12876\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1949.nc,15268\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep
    tTable = FileVisitorDNLS.oneStep(url + "3", tFileNameRegex, tRecursive, tPathRegex, tDirsToo);
    tTable.removeColumn("lastModified");
    results = tTable.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);

    // * test file regex that won't match
    table.removeAllRows();
    results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url, // test no trailing /
            "zztop",
            tRecursive,
            tPathRegex,
            tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    results = table.dataToString();
    expected = // just dirs
        "directory,name,size\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,,\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep
    tTable = FileVisitorDNLS.oneStep(url, "zztop", tRecursive, tPathRegex, tDirsToo);
    tTable.removeColumn("lastModified");
    results = tTable.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);

    // * that should be the same as !recursive
    table.removeAllRows();
    results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url, // test no trailing /
            tFileNameRegex,
            false,
            tPathRegex,
            tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    results = table.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test via oneStep
    tTable = FileVisitorDNLS.oneStep(url, tFileNameRegex, false, tPathRegex, tDirsToo);
    tTable.removeColumn("lastModified");
    results = tTable.dataToString();
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** This tests a WAF-related (Web Accessible Folder) methods on an ERDDAP "files" directory. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testErddap1FilesWAF2() throws Throwable {
    // String2.log("\n*** FileVisitorDNLS.testErddapFilesWAF2()\n");

    // *** test localhost
    // String2.log("\nThis test requires erdMH1chla1day in localhost erddap.");
    String url = "http://localhost:" + PORT + "/erddap/files/erdMH1chla1day/";
    String tFileNameRegex = "A20.*\\.nc(|\\.gz)";
    boolean tRecursive = true;
    String tPathRegex = ".*";
    boolean tDirsToo = true;
    Table table = FileVisitorDNLS.makeEmptyTable();
    StringArray dirs = (StringArray) table.getColumn(0);
    StringArray names = (StringArray) table.getColumn(1);
    LongArray lastModifieds = (LongArray) table.getColumn(2);
    LongArray sizes = (LongArray) table.getColumn(3);

    // * test all features
    String results =
        FileVisitorDNLS.addToWAFUrlList( // returns a list of errors or ""
            url,
            tFileNameRegex,
            tRecursive,
            tPathRegex,
            tDirsToo,
            dirs,
            names,
            lastModifieds,
            sizes);
    Test.ensureEqual(results, "", "results=\n" + results);
    table.removeColumn("lastModified");
    table.removeColumn("size");
    results = table.dataToString();
    String expected =
        "directory,name\n"
            + "http://localhost:8080/erddap/files/erdMH1chla1day/,A2003001.L3m_DAY_CHL_chlor_a_4km.nc\n"
            + //
            "http://localhost:8080/erddap/files/erdMH1chla1day/,A2003002.L3m_DAY_CHL_chlor_a_4km.nc\n"
            + //
            "http://localhost:8080/erddap/files/erdMH1chla1day/,A2016291.L3m_DAY_CHL_chlor_a_4km.nc\n";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /* TestSSR */

  /** Test posting info and getting response. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testPostFormGetResponseString() throws Exception {
    String s =
        SSR.postFormGetResponseString(
            "http://localhost:"
                + PORT
                + "/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=nceiPH53sstd1day");
    String2.log("\nSSR.testPostFormGetResponseString() result:\n" + s);
    Test.ensureTrue(s.indexOf("Do a Full Text Search for Datasets:") >= 0, "");
    Test.ensureTrue(
        s.indexOf(
                "Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.3 L3-Collated (L3C) sea surface temperature.")
            >= 0,
        "");
    Test.ensureTrue(s.indexOf("ERDDAP, Version") >= 0, "");

    // 2018-10-24 I verified that
    // * This request appears as a POST (not GET) in tomcat's
    // localhost_access_lot[date].txt
    // * The parameters don't appear in that file (whereas they do for GET requests)
    // * The parameters don't appear in ERDDAP log (whereas they do for GET
    // requests),
    // * and it is labelled as a POST request.
    s =
        SSR.postFormGetResponseString(
            "http://localhost:"
                + PORT
                + "/erddap/search/index.html?page=1&itemsPerPage=1000&searchFor=nceiPH53sstd1day");
    String2.log("\nSSR.testPostFormGetResponseString() result:\n" + s);
    Test.ensureTrue(s.indexOf("Do a Full Text Search for Datasets:") >= 0, "");
    Test.ensureTrue(
        s.indexOf(
                "Advanced Very High Resolution Radiometer (AVHRR) Pathfinder Version 5.3 L3-Collated (L3C) sea surface temperature.")
            >= 0,
        "");
    Test.ensureTrue(s.indexOf("ERDDAP, Version") >= 0, "");
  }

  /* TranslateMessages */

  /**
   * This checks lots of webpages on localhost ERDDAP for uncaught special text (&amp;term; or
   * ZtermZ). This REQUIRES localhost ERDDAP be running with at least
   * <datasetsRegex>(etopo.*|jplMURSST41|glerlAvgTemp)</datasetsRegex>.
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void checkForUncaughtSpecialText() throws Exception {
    String2.log(
        "\n*** TranslateMessages.checkForUncaughtSpecialText()\n"
            + "THIS REQUIRES localhost ERDDAP with at least (etopo.*|jplMURSST41|glerlAvgTemp)");
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
      "files/glerlAvgTemp/",
      "files/documentation.html",
      "griddap/documentation.html",
      "griddap/nceiPH53sstd1day.graph",
      "griddap/nceiPH53sstd1day.html",
      // "info/index.html?page=1&itemsPerPage=1000", // Descriptions of datasets may
      // contain char patterns
      "info/glerlAvgTemp/index.html",
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
      "tabledap/glerlAvgTemp.graph",
      "tabledap/glerlAvgTemp.html",
      "tabledap/testJsonlCSV.subset",
      "wms/documentation.html",
      "wms/nceiPH53sstd1day/index.html"
    };
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
      hs.addAll(
          Arrays.asList(
              String2.extractAllCaptureGroupsAsHashSet(content, "(Z[a-zA-Z0-9]Z)", 1)
                  .toArray(new String[0])));
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
      hs.addAll(
          Arrays.asList(
              String2.extractAllCaptureGroupsAsHashSet(content, "(&amp;[a-zA-Z]+?;)", 1)
                  .toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append(
            "\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " &entities; :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }

      // Look for {0}, {1}, etc that should have been replaced by replaceAll().
      // There are some legit values on setupDatasetsXml.html in regexes ({nChar}:
      // 12,14,4,6,7,8).
      hs = new HashSet();
      hs.addAll(
          Arrays.asList(
              String2.extractAllCaptureGroupsAsHashSet(content, "(\\{\\d+\\})", 1)
                  .toArray(new String[0])));
      sa = hs.toArray(new String[0]);
      if (sa.length > 0) {
        results.append("\n* url=" + tErddapUrl + pages[i] + " has " + sa.length + " {#} :\n");
        Arrays.sort(sa, new StringComparatorIgnoreCase());
        results.append(String2.toNewlineString(sa));
      }
    }
    if (results.length() > 0) throw new RuntimeException(results.toString());
  }

  /* Erddap */

  /**
   * This is used by Bob to do simple tests of the basic Erddap services from the ERDDAP at
   * EDStatic.erddapUrl. It assumes Bob's test datasets are available.
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
    EDStatic.sosActive =
        false; // currently, never true because sos is unfinished //some other tests may have
    // left this as true

    // home page
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl); // redirects to index.html
    expected = "The small effort to set up ERDDAP brings many benefits.";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/"); // redirects to index.html
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // test version info (opendap spec section 7.2.5)
    // "version" instead of datasetID
    expected =
        "Core Version: DAP/2.0\n"
            + "Server Version: dods/3.7\n"
            + "ERDDAP_version: "
            + EDStatic.erddapVersion
            + "\n";
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
    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.ver");
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

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/documentation.html");
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.help");
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
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/info/index.html?" + EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Argo float vertical profiles from Coriolis Global Data Assembly Centres")
            >= 0,
        "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/info/index.csv?" + EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") < 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Argo float vertical profiles from Coriolis Global Data Assembly Centres")
            >= 0,
        "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdGlobecBottle/index.tsv");
    Test.ensureTrue(results.indexOf("\t") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ioos_category") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Location") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("long_name") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Cast Number") >= 0, "results=\n" + results);

    // search
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/search/index.html?" + EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Do a Full Text Search for Datasets") >= 0, "results=\n" + results);
    // index.otherFileType must have ?searchFor=...

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.html?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">MODIS Aqua, Level-3 SMI, Global, 4km") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.htmlTable?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=all");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "MODIS Aqua, Level-3 SMI, Global, 4km, Particulate Organic Carbon, 2003-present (1 Day Composite)")
            >= 0,
        "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.html?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                ">TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\n")
            > 0,
        "results=\n" + results);

    // .json
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.json?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel+sst");
    expected =
        "{\n"
            + //
            "  \"table\": {\n"
            + //
            "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
            + //
            "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
            + //
            "    \"rows\": [\n"
            + //
            "      [\"\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/pmelTaoDySst\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:"
            + PORT
            + "/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:"
            + PORT
            + "/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:"
            + PORT
            + "/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:"
            + PORT
            + "/erddap/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:"
            + PORT
            + "/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:"
            + PORT
            + "/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n"
            + //
            "      [\"\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:"
            + PORT
            + "/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:"
            + PORT
            + "/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:"
            + PORT
            + "/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:"
            + PORT
            + "/erddap/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:"
            + PORT
            + "/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:"
            + PORT
            + "/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
            + //
            "    ]\n"
            + //
            "  }\n"
            + //
            "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .json with jsonp
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.json?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel+sst&.jsonp=fnName");
    expected =
        "fnName({\n"
            + //
            "  \"table\": {\n"
            + //
            "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"],\n"
            + //
            "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\"],\n"
            + //
            "    \"rows\": [\n"
            + //
            "      [\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"],\n"
            + //
            "      [\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.json\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
            + //
            "    ]\n"
            + //
            "  }\n"
            + //
            "}\n"
            + //
            ")";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // and read the header to see the mime type
    results =
        String2.toNewlineString(
            SSR.dosOrCShell(
                    "curl -i \""
                        + EDStatic.erddapUrl
                        + "/search/index.json?"
                        + EDStatic.defaultPIppQuery
                        + "&searchFor=tao+pmel&.jsonp=fnName\"",
                    120)
                .toArray());
    po = results.indexOf("HTTP");
    results = results.substring(po);
    po = results.indexOf("chunked");
    results = results.substring(0, po + 7);
    expected =
        "HTTP/1.1 200 OK\n"
            + "Server: Jetty(12.0.14)\n"
            + "Date: Today\n"
            + "Content-Type: application/javascript;charset=utf-8\n"
            + "Content-Encoding: identity\n"
            + "Transfer-Encoding: chunked";
    results = results.replaceAll("Date: ..., .. [a-zA-Z]+ .... ..:..:.. ...\n", "Date: Today\n");
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV1
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.jsonlCSV1?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel");
    expected =
        "[\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", \"wms\", \"files\", \"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", \"Email\", \"Institution\", \"Dataset ID\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"This dataset has monthly Position data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "LON_502 (Precise Longitude, degree_east)\\n"
            + //
            "QX_5502 (Longitude Quality)\\n"
            + //
            "LAT_500 (Precise Latitude, degree_north)\\n"
            + //
            "QY_5500 (Latitude Quality)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoMonPos\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"This dataset has daily Air Temperature data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "AT_21 (Air Temperature, degree_C)\\n"
            + //
            "QAT_5021 (Air Temperature Quality)\\n"
            + //
            "SAT_6021 (Air Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAirt\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"\", \"http://localhost:8080/erddap/files/testTableWithDepth/\", \"This is EDDTableWithDepth\", \"This is the summary\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "AT_21 (Air Temperature, degree_C)\\n"
            + //
            "QAT_5021 (Air Temperature Quality)\\n"
            + //
            "SAT_6021 (Air Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlCSV1\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"testTableWithDepth\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlCSV
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.jsonlCSV?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel");
    expected =
        "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"This dataset has monthly Position data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "LON_502 (Precise Longitude, degree_east)\\n"
            + //
            "QX_5502 (Longitude Quality)\\n"
            + //
            "LAT_500 (Precise Latitude, degree_north)\\n"
            + //
            "QY_5500 (Latitude Quality)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoMonPos\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"This dataset has daily Air Temperature data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "AT_21 (Air Temperature, degree_C)\\n"
            + //
            "QAT_5021 (Air Temperature Quality)\\n"
            + //
            "SAT_6021 (Air Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDyAirt\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"pmelTaoDySst\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"\", \"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"rlPmelTaoDySst\"]\n"
            + //
            "[\"\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"\", \"http://localhost:8080/erddap/files/testTableWithDepth/\", \"This is EDDTableWithDepth\", \"This is the summary\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "AT_21 (Air Temperature, degree_C)\\n"
            + //
            "QAT_5021 (Air Temperature Quality)\\n"
            + //
            "SAT_6021 (Air Temperature Source)\\n"
            + //
            "\", \"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlCSV\", \"https://www.pmel.noaa.gov/gtmba/mission\", \"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"testTableWithDepth\"]\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // .jsonlKVP
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.jsonlKVP?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel");
    expected =
        "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoMonPos.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoMonPos/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Monthly, 1980-present, Position\", \"Summary\":\"This dataset has monthly Position data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Monthly data is an average of daily values collected during a month.  A minimum of 15 daily values are required to compute a monthly average.  This dataset contains realtime and delayed mode data (see the 'source' variable).  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "LON_502 (Precise Longitude, degree_east)\\n"
            + //
            "QX_5502 (Longitude Quality)\\n"
            + //
            "LAT_500 (Precise Latitude, degree_north)\\n"
            + //
            "QY_5500 (Latitude Quality)\\n"
            + //
            "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoMonPos_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoMonPos_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoMonPos/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoMonPos.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoMonPos&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoMonPos\"}\n"
            + //
            "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDyAirt.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDyAirt/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Air Temperature\", \"Summary\":\"This dataset has daily Air Temperature data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "AT_21 (Air Temperature, degree_C)\\n"
            + //
            "QAT_5021 (Air Temperature Quality)\\n"
            + //
            "SAT_6021 (Air Temperature Source)\\n"
            + //
            "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDyAirt_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDyAirt_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDyAirt/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDyAirt.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDyAirt&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDyAirt\"}\n"
            + //
            "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/pmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/pmelTaoDySst/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/pmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/pmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/pmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/pmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=pmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"pmelTaoDySst\"}\n"
            + //
            "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/rlPmelTaoDySst.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/rlPmelTaoDySst/\", \"Title\":\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\", \"Summary\":\"This dataset has daily Sea Surface Temperature (SST) data from the\\n"
            + //
            "TAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\n"
            + //
            "RAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\n"
            + //
            "PIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\n"
            + //
            "arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\n"
            + //
            "https://www.pmel.noaa.gov/gtmba/mission .\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "T_25 (Sea Surface Temperature, degree_C)\\n"
            + //
            "QT_5025 (Sea Surface Temperature Quality)\\n"
            + //
            "ST_6025 (Sea Surface Temperature Source)\\n"
            + //
            "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/rlPmelTaoDySst_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/rlPmelTaoDySst_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/rlPmelTaoDySst/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/rlPmelTaoDySst.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=rlPmelTaoDySst&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"rlPmelTaoDySst\"}\n"
            + //
            "{\"griddap\":\"\", \"Subset\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth.subset\", \"tabledap\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth\", \"Make A Graph\":\"http://localhost:8080/erddap/tabledap/testTableWithDepth.graph\", \"wms\":\"\", \"files\":\"http://localhost:8080/erddap/files/testTableWithDepth/\", \"Title\":\"This is EDDTableWithDepth\", \"Summary\":\"This is the summary\\n"
            + //
            "\\n"
            + //
            "cdm_data_type = TimeSeries\\n"
            + //
            "VARIABLES:\\n"
            + //
            "array\\n"
            + //
            "station\\n"
            + //
            "wmo_platform_code\\n"
            + //
            "longitude (Nominal Longitude, degrees_east)\\n"
            + //
            "latitude (Nominal Latitude, degrees_north)\\n"
            + //
            "time (Centered Time, seconds since 1970-01-01T00:00:00Z)\\n"
            + //
            "depth (m)\\n"
            + //
            "AT_21 (Air Temperature, degree_C)\\n"
            + //
            "QAT_5021 (Air Temperature Quality)\\n"
            + //
            "SAT_6021 (Air Temperature Source)\\n"
            + //
            "\", \"FGDC\":\"http://localhost:8080/erddap/metadata/fgdc/xml/testTableWithDepth_fgdc.xml\", \"ISO 19115\":\"http://localhost:8080/erddap/metadata/iso19115/xml/testTableWithDepth_iso19115.xml\", \"Info\":\"http://localhost:8080/erddap/info/testTableWithDepth/index.jsonlKVP\", \"Background Info\":\"https://www.pmel.noaa.gov/gtmba/mission\", \"RSS\":\"http://localhost:8080/erddap/rss/testTableWithDepth.rss\", \"Email\":\"http://localhost:8080/erddap/subscriptions/add.html?datasetID=testTableWithDepth&showErrors=false&email=\", \"Institution\":\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\", \"Dataset ID\":\"testTableWithDepth\"}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl
                + "/search/index.tsv?"
                + EDStatic.defaultPIppQuery
                + "&searchFor=tao+pmel");
    Test.ensureTrue(
        results.indexOf(
                "\t\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\t")
            > 0,
        "results=\n" + results);

    // categorize
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/categorize/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">standard_name\n") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/categorize/index.json");
    Test.ensureEqual(
        results,
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"Categorize\", \"URL\"],\n"
            + "    \"columnTypes\": [\"String\", \"String\"],\n"
            + "    \"rows\": [\n"
            + "      [\"cdm_data_type\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"institution\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"ioos_category\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"keywords\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"long_name\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"standard_name\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"variableName\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n",
        "results=\n" + results);

    // json with jsonp
    String jsonp = "myFunctionName";
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/categorize/index.json?.jsonp=" + SSR.percentEncode(jsonp));
    Test.ensureEqual(
        results,
        jsonp
            + "("
            + "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"Categorize\", \"URL\"],\n"
            + "    \"columnTypes\": [\"String\", \"String\"],\n"
            + "    \"rows\": [\n"
            + "      [\"cdm_data_type\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/cdm_data_type/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"institution\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/institution/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"ioos_category\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/ioos_category/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"keywords\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/keywords/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"long_name\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/long_name/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"standard_name\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/standard_name/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"variableName\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/variableName/index.json?page=1&itemsPerPage=1000\"]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + ")",
        "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/categorize/standard_name/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">sea_water_temperature\n") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/categorize/standard_name/index.json");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"sea_water_temperature\"") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/categorize/institution/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">ioos_category\n") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">noaa_coastwatch_west_coast_node\n") >= 0, "results=\n" + results);

    results =
        String2.annotatedString(
            SSR.getUrlResponseStringUnchanged(
                EDStatic.erddapUrl + "/categorize/institution/index.tsv"));
    Test.ensureTrue(results.indexOf("Category[9]URL[10]") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "noaa_coastwatch_west_coast_node[9]http://localhost:"
                    + PORT
                    + "/erddap/categorize/institution/noaa_coastwatch_west_coast_node/index.tsv?page=1&itemsPerPage=1000[10]")
            >= 0,
        "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/categorize/standard_name/sea_water_temperature/index.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdGlobecBottle\n") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/categorize/standard_name/sea_water_temperature/index.json");
    expected =
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"griddap\", \"Subset\", \"tabledap\", \"Make A Graph\", "
            + (EDStatic.sosActive ? "\"sos\", " : "")
            + (EDStatic.wcsActive ? "\"wcs\", " : "")
            + (EDStatic.wmsActive ? "\"wms\", " : "")
            + (EDStatic.filesActive ? "\"files\", " : "")
            + (EDStatic.authentication.length() > 0 ? "\"Accessible\", " : "")
            + "\"Title\", \"Summary\", \"FGDC\", \"ISO 19115\", \"Info\", \"Background Info\", \"RSS\", "
            + (EDStatic.subscriptionSystemActive ? "\"Email\", " : "")
            + "\"Institution\", \"Dataset ID\"],\n"
            + "    \"columnTypes\": [\"String\", \"String\", \"String\", \"String\", "
            + (EDStatic.sosActive ? "\"String\", " : "")
            + (EDStatic.wcsActive ? "\"String\", " : "")
            + (EDStatic.wmsActive ? "\"String\", " : "")
            + (EDStatic.filesActive ? "\"String\", " : "")
            + (EDStatic.authentication.length() > 0 ? "\"String\", " : "")
            + "\"String\", \"String\", \"String\", \"String\", \"String\", \"String\", \"String\", "
            + (EDStatic.subscriptionSystemActive ? "\"String\", " : "")
            + "\"String\", \"String\"],\n"
            + "    \"rows\": [\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    expected =
        "http://localhost:"
            + PORT
            + "/erddap/tabledap/erdGlobecBottle.subset\", "
            + "\"http://localhost:"
            + PORT
            + "/erddap/tabledap/erdGlobecBottle\", "
            + "\"http://localhost:"
            + PORT
            + "/erddap/tabledap/erdGlobecBottle.graph\", "
            + (EDStatic.sosActive ? "\"\", " : "")
            + // currently, it isn't made available via sos
            (EDStatic.wcsActive ? "\"\", " : "")
            + (EDStatic.wmsActive ? "\"\", " : "")
            + (EDStatic.filesActive
                ? "\"http://localhost:" + PORT + "/erddap/files/erdGlobecBottle/\", "
                : "")
            + (EDStatic.authentication.length() > 0 ? "\"public\", " : "")
            + "\"GLOBEC NEP Rosette Bottle Data (2002)\", \"GLOBEC (GLOBal "
            + "Ocean ECosystems Dynamics) NEP (Northeast Pacific)\\nRosette Bottle Data from "
            + "New Horizon Cruise (NH0207: 1-19 August 2002).\\nNotes:\\nPhysical data "
            + "processed by Jane Fleischbein (OSU).\\nChlorophyll readings done by "
            + "Leah Feinberg (OSU).\\nNutrient analysis done by Burke Hales (OSU).\\n"
            + "Sal00 - salinity calculated from primary sensors (C0,T0).\\n"
            + "Sal11 - salinity calculated from secondary sensors (C1,T1).\\n"
            + "secondary sensor pair was used in final processing of CTD data for\\n"
            + "most stations because the primary had more noise and spikes. The\\n"
            + "primary pair were used for cast #9, 24, 48, 111 and 150 due to\\n"
            + "multiple spikes or offsets in the secondary pair.\\n"
            + "Nutrient samples were collected from most bottles; all nutrient data\\n"
            + "developed from samples frozen during the cruise and analyzed ashore;\\n"
            + "data developed by Burke Hales (OSU).\\n"
            + "Operation Detection Limits for Nutrient Concentrations\\n"
            + "Nutrient  Range         Mean    Variable         Units\\n"
            + "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\\n"
            + "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\\n"
            + "Si        0.13-0.24     0.16    Silicate         micromoles per liter\\n"
            + "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\\n"
            + "Dates and Times are UTC.\\n\\n"
            + "For more information, see https://www.bco-dmo.org/dataset/2452\\n\\n"
            +
            // was "http://cis.whoi.edu/science/bcodmo/dataset.cfm?id=10180&flag=view\\n\\n"
            // +
            "Inquiries about how to access this data should be directed to\\n"
            + "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\\n\\n"
            + "cdm_data_type = TrajectoryProfile\\n"
            + "VARIABLES:\\ncruise_id\\n... (24 more variables)\\n\", "
            + "\"http://localhost:"
            + PORT
            + "/erddap/metadata/fgdc/xml/erdGlobecBottle_fgdc.xml\", "
            + "\"http://localhost:"
            + PORT
            + "/erddap/metadata/iso19115/xml/erdGlobecBottle_iso19115.xml\", "
            + "\"http://localhost:"
            + PORT
            + "/erddap/info/erdGlobecBottle/index.json\", "
            + "\"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\", "
            + // was
            // "\"http://www.globec.org/\",
            // " +
            "\"http://localhost:"
            + PORT
            + "/erddap/rss/erdGlobecBottle.rss\", "
            + (EDStatic.subscriptionSystemActive
                ? "\"http://localhost:"
                    + PORT
                    + "/erddap/subscriptions/add.html?datasetID=erdGlobecBottle&showErrors=false&email=\", "
                : "")
            + "\"GLOBEC\", \"erdGlobecBottle\"],";
    po = results.indexOf("http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle");
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // griddap
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/griddap/index.html?" + EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of griddap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                ">MODIS Aqua, Level-3 Standard Mapped Image (SMI), Global, 4km, Particulate Organic Carbon (POC) (1 Day Composite)")
            >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdMPOC1day\n") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/griddap/index.json?" + EDStatic.defaultPIppQuery + "");
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "MODIS Aqua, Level-3 Standard Mapped Image (SMI), Global, 4km, Particulate Organic Carbon (POC) (1 Day Composite)")
            >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"erdMPOC1day\"") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMPOC1day.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(Centered Time, UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Particulate Organic Carbon") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/griddap/erdMPOC1day.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Particulate Organic Carbon") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // tabledap
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/tabledap/index.html?" + EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("List of tabledap Datasets") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(">GLOBEC NEP Rosette Bottle Data (2002)\n") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">erdGlobecBottle\n") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/tabledap/index.json?" + EDStatic.defaultPIppQuery);
    Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("\"GLOBEC NEP Rosette Bottle Data (2002)\"") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("\"erdGlobecBottle\"") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("(UTC)") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Just&#x20;generate&#x20;the&#x20;URL&#x3a;") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/tabledap/erdGlobecBottle.graph");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("NO3") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Filled Square") >= 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("Download&#x20;the&#x20;Data&#x20;or&#x20;an&#x20;Image") >= 0,
        "results=\n" + results);

    // files
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/");
    Test.ensureTrue(
        results.indexOf(
                "ERDDAP's \"files\" system lets you browse a virtual file system and download source data files.")
            >= 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("glerlAvgTemp") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directories") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/files/testTableAscii/subdir/");
    Test.ensureTrue(results.indexOf("The Title for testTableAscii") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Make a graph") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("WARNING!") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Last modified") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("46012&#x5f;2005&#x2e;csv") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("directory") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP, Version") >= 0, "results=\n" + results);

    String localName = EDStatic.fullTestCacheDirectory + "46012_2005.csv";
    File2.delete(localName);
    SSR.downloadFile( // throws Exception if trouble
        EDStatic.erddapUrl + "/files/testTableAscii/subdir/46012_2005.csv",
        localName,
        true); // tryToUseCompression
    Test.ensureTrue(
        File2.isFile(localName), "/files download failed. Not found: localName=" + localName);
    File2.delete(localName);

    // sos
    if (EDStatic.sosActive) {
      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/sos/index.html?" + EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of SOS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(">NDBC Standard Meteorological Buoy Data") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">glerAvgTemp") >= 0, "results=\n" + results);

      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/sos/index.json?" + EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("\"NDBC Standard Meteorological Buoy Data\"") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"glerAvgTemp\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/sos/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("available via ERDDAP's Sensor Observation Service (SOS) web service.")
              >= 0,
          "results=\n" + results);

      String sosUrl = EDStatic.erddapUrl + "/sos/glerAvgTemp/" + EDDTable.sosServer;
      results = SSR.getUrlResponseStringUnchanged(sosUrl + "?service=SOS&request=GetCapabilities");
      Test.ensureTrue(results.indexOf("<ows:ServiceIdentification>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("<ows:Get xlink:href=\"" + sosUrl + "?\"/>") >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("</Capabilities>") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results =
            SSR.getUrlResponseStringUnchanged(
                EDStatic.erddapUrl + "/sos/index.html?" + EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      expected =
          "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
              + PORT
              + "/erddap/sos/index.html?page=1&itemsPerPage=1000\n"
              + "(Error {\n"
              + "    code=404;\n"
              + "    message=\"Not Found: The \\\"SOS\\\" system has been disabled on this ERDDAP.\";\n"
              + "})\n";
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    }

    // wcs
    if (EDStatic.wcsActive) {
      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/wcs/index.html?" + EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("Datasets Which Can Be Accessed via WCS") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS</th>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
                  ">Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)</td>")
              >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMHchla8day<") >= 0, "results=\n" + results);

      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/wcs/index.json?" + EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
                  "\"Chlorophyll-a, Aqua MODIS, NPP, Global, Science Quality (8 Day Composite)\"")
              >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMHchla8day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wcs/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
                  "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.")
              >= 0,
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
        results =
            SSR.getUrlResponseStringUnchanged(
                EDStatic.erddapUrl + "/wcs/index.html?" + EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(
          results.indexOf(
                  "java.io.FileNotFoundException: http://localhost:"
                      + PORT
                      + "/erddap/wcs/index.html?page=1&itemsPerPage=1000")
              >= 0,
          "results=\n" + results);
    }

    // wms
    if (EDStatic.wmsActive) {
      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/wms/index.html?" + EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List of WMS Datasets") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">Title\n") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf(">RSS\n") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
                  ">Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present")
              >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf(">erdMH1chla1day\n") >= 0, "results=\n" + results);

      results =
          SSR.getUrlResponseStringUnchanged(
              EDStatic.erddapUrl + "/wms/index.json?" + EDStatic.defaultPIppQuery);
      Test.ensureTrue(results.indexOf("\"table\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"Title\"") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"RSS\"") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
                  "\"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present")
              >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("\"erdMH1chla1day\"") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/documentation.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("display of registered and superimposed map-like views") >= 0,
          "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);

      results =
          SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/wms/erdMH1chla1day/index.html");
      Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf(
                  "ERDDAP - Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite) - WMS")
              >= 0,
          "results=\n" + results);
      Test.ensureTrue(results.indexOf("Data Access Form") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Make A Graph") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("on-the-fly by ERDDAP's") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("longitude") >= 0, "results=\n" + results);
      Test.ensureTrue(
          results.indexOf("Three Ways to Make Maps with WMS") >= 0, "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results =
            SSR.getUrlResponseStringUnchanged(
                EDStatic.erddapUrl + "/wms/index.html?" + EDStatic.defaultPIppQuery);
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(
          results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0,
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
    Test.ensureTrue(
        results.indexOf("ERDDAP a solution to everyone's data distribution / data access problems?")
            >= 0,
        "results=\n" + results);

    // subscriptions
    if (EDStatic.subscriptionSystemActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/subscriptions/index.html");
      Test.ensureTrue(results.indexOf("Add a new subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Validate a subscription") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("List your subscriptions") >= 0, "results=\n" + results);
      Test.ensureTrue(results.indexOf("Remove a subscription") >= 0, "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/subscriptions/add.html");
      Test.ensureTrue(
          results.indexOf("To add a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results =
          SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/subscriptions/validate.html");
      Test.ensureTrue(
          results.indexOf("To validate a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);

      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/subscriptions/list.html");
      Test.ensureTrue(
          results.indexOf(
                  "To request an email with a list of your subscriptions, please fill out this form:")
              >= 0,
          "results=\n" + results);

      results =
          SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/subscriptions/remove.html");
      Test.ensureTrue(
          results.indexOf("To remove a (another) subscription, please fill out this form:") >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results =
            SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/subscriptions/index.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(
          results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0,
          "results=\n" + results);
    }

    // slideSorter
    if (EDStatic.slideSorterActive) {
      results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/slidesorter.html");
      Test.ensureTrue(
          results.indexOf(
                  "Your slides will be lost when you close this browser window, unless you:")
              >= 0,
          "results=\n" + results);
    } else {
      results = "Shouldn't get here.";
      try {
        results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/slidesorter.html");
      } catch (Throwable t) {
        results = MustBe.throwableToString(t);
      }
      Test.ensureTrue(
          results.indexOf("Server returned HTTP response code: 500 for URL:") >= 0,
          "results=\n" + results);
    }

    // embed a graph (always at coastwatch)
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/images/embed.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("Embed a Graph in a Web Page") >= 0, "results=\n" + results);

    // Computer Programs
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/rest.html");
    Test.ensureTrue(results.indexOf("</html>") >= 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("ERDDAP's RESTful Web Services") >= 0, "results=\n" + results);

    // list of services
    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.csv");
    expected =
        "Resource,URL\n"
            + "info,http://localhost:"
            + PORT
            + "/erddap/info/index.csv?"
            + EDStatic.defaultPIppQuery
            + "\n"
            + "search,http://localhost:"
            + PORT
            + "/erddap/search/index.csv?"
            + EDStatic.defaultPIppQuery
            + "&searchFor=\n"
            + "categorize,http://localhost:"
            + PORT
            + "/erddap/categorize/index.csv?"
            + EDStatic.defaultPIppQuery
            + "\n"
            + "griddap,http://localhost:"
            + PORT
            + "/erddap/griddap/index.csv?"
            + EDStatic.defaultPIppQuery
            + "\n"
            + "tabledap,http://localhost:"
            + PORT
            + "/erddap/tabledap/index.csv?"
            + EDStatic.defaultPIppQuery
            + "\n"
            + (EDStatic.sosActive
                ? "sos,http://localhost:"
                    + PORT
                    + "/erddap/sos/index.csv?"
                    + EDStatic.defaultPIppQuery
                    + "\n"
                : "")
            + (EDStatic.wcsActive
                ? "wcs,http://localhost:"
                    + PORT
                    + "/erddap/wcs/index.csv?"
                    + EDStatic.defaultPIppQuery
                    + "\n"
                : "")
            + (EDStatic.wmsActive
                ? "wms,http://localhost:"
                    + PORT
                    + "/erddap/wms/index.csv?"
                    + EDStatic.defaultPIppQuery
                    + "\n"
                : "");
    // subscriptions?
    // converters?
    Test.ensureEqual(results, expected, "results=\n" + results);

    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/index.htmlTable?" + EDStatic.defaultPIppQuery);
    expected =
        EDStatic.startHeadHtml(0, EDStatic.erddapUrl((String) null, language), "Resources")
            + "\n"
            + "</head>\n"
            + EDStatic.startBodyHtml(0, null, "index.html", EDStatic.defaultPIppQuery)
            + // 2022-11-22 .htmlTable
            // converted to
            // .html to avoid user
            // requesting all
            // data in a dataset if they
            // change
            // language
            "&nbsp;<br>\n"
            + "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>Resource\n"
            + "<th>URL\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>info\n"
            + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;info&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
            + PORT
            + "/erddap/info/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>search\n"
            + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;search&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000&#x26;searchFor&#x3d;\">http://localhost:"
            + PORT
            + "/erddap/search/index.htmlTable?page=1&amp;itemsPerPage=1000&amp;searchFor=</a>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>categorize\n"
            + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;categorize&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
            + PORT
            + "/erddap/categorize/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>griddap\n"
            + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;griddap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
            + PORT
            + "/erddap/griddap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>tabledap\n"
            + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;tabledap&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
            + PORT
            + "/erddap/tabledap/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            + "</tr>\n"
            + (EDStatic.sosActive
                ? "<tr>\n"
                    + "<td>sos\n"
                    + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;sos&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
                    + PORT
                    + "/erddap/sos/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
                    + "</tr>\n"
                : "")
            + "<tr>\n"
            + "<td>wms\n"
            + "<td><a href=\"http&#x3a;&#x2f;&#x2f;localhost&#x3a;8080&#x2f;erddap&#x2f;wms&#x2f;index&#x2e;htmlTable&#x3f;page&#x3d;1&#x26;itemsPerPage&#x3d;1000\">http://localhost:"
            + PORT
            + "/erddap/wms/index.htmlTable?page=1&amp;itemsPerPage=1000</a>\n"
            + "</tr>\n"
            + "</table>\n"
            + EDStatic.endBodyHtml(0, EDStatic.erddapUrl((String) null, language), (String) null)
            + "\n"
            + "</html>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.json");
    expected =
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"Resource\", \"URL\"],\n"
            + "    \"columnTypes\": [\"String\", \"String\"],\n"
            + "    \"rows\": [\n"
            + "      [\"info\", \"http://localhost:"
            + PORT
            + "/erddap/info/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"search\", \"http://localhost:"
            + PORT
            + "/erddap/search/index.json?page=1&itemsPerPage=1000&searchFor=\"],\n"
            + "      [\"categorize\", \"http://localhost:"
            + PORT
            + "/erddap/categorize/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"griddap\", \"http://localhost:"
            + PORT
            + "/erddap/griddap/index.json?page=1&itemsPerPage=1000\"],\n"
            + "      [\"tabledap\", \"http://localhost:"
            + PORT
            + "/erddap/tabledap/index.json?page=1&itemsPerPage=1000\"]"
            + (EDStatic.sosActive || EDStatic.wcsActive || EDStatic.wmsActive ? "," : "")
            + "\n"
            + (EDStatic.sosActive
                ? "      [\"sos\", \"http://localhost:"
                    + PORT
                    + "/erddap/sos/index.json?page=1&itemsPerPage=1000\"]"
                    + (EDStatic.wcsActive || EDStatic.wmsActive ? "," : "")
                    + "\n"
                : "")
            + (EDStatic.wcsActive
                ? "      [\"wcs\", \"http://localhost:"
                    + PORT
                    + "/erddap/wcs/index.json?page=1&itemsPerPage=1000\"]"
                    + (EDStatic.wmsActive ? "," : "")
                    + "\n"
                : "")
            + (EDStatic.wmsActive
                ? "      [\"wms\", \"http://localhost:"
                    + PORT
                    + "/erddap/wms/index.json?page=1&itemsPerPage=1000\"]\n"
                : "")
            +
            // subscriptions?
            "    ]\n"
            + "  }\n"
            + "}\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results =
        String2.annotatedString(
            SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.tsv"));
    expected =
        "Resource[9]URL[10]\n"
            + "info[9]http://localhost:"
            + PORT
            + "/erddap/info/index.tsv?page=1&itemsPerPage=1000[10]\n"
            + "search[9]http://localhost:"
            + PORT
            + "/erddap/search/index.tsv?page=1&itemsPerPage=1000&searchFor=[10]\n"
            + "categorize[9]http://localhost:"
            + PORT
            + "/erddap/categorize/index.tsv?page=1&itemsPerPage=1000[10]\n"
            + "griddap[9]http://localhost:"
            + PORT
            + "/erddap/griddap/index.tsv?page=1&itemsPerPage=1000[10]\n"
            + "tabledap[9]http://localhost:"
            + PORT
            + "/erddap/tabledap/index.tsv?page=1&itemsPerPage=1000[10]\n"
            + (EDStatic.sosActive
                ? "sos[9]http://localhost:"
                    + PORT
                    + "/erddap/sos/index.tsv?page=1&itemsPerPage=1000[10]\n"
                : "")
            + (EDStatic.wcsActive
                ? "wcs[9]http://localhost:"
                    + PORT
                    + "/erddap/wcs/index.tsv?page=1&itemsPerPage=1000[10]\n"
                : "")
            + (EDStatic.wmsActive
                ? "wms[9]http://localhost:"
                    + PORT
                    + "/erddap/wms/index.tsv?page=1&itemsPerPage=1000[10]\n"
                : "")
            + "[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/index.xhtml");
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
            + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"
            + "  <title>Resources</title>\n"
            + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:"
            + PORT
            + "/erddap/images/erddap2.css\" />\n"
            + "</head>\n"
            + "<body>\n"
            + "\n"
            + "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>Resource</th>\n"
            + "<th>URL</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>info</td>\n"
            + "<td>http://localhost:"
            + PORT
            + "/erddap/info/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>search</td>\n"
            + "<td>http://localhost:"
            + PORT
            + "/erddap/search/index.xhtml?page=1&amp;itemsPerPage=1000&amp;searchFor=</td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>categorize</td>\n"
            + "<td>http://localhost:"
            + PORT
            + "/erddap/categorize/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>griddap</td>\n"
            + "<td>http://localhost:"
            + PORT
            + "/erddap/griddap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td>tabledap</td>\n"
            + "<td>http://localhost:"
            + PORT
            + "/erddap/tabledap/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
            + "</tr>\n"
            + (EDStatic.sosActive
                ? "<tr>\n"
                    + "<td>sos</td>\n"
                    + "<td>http://localhost:"
                    + PORT
                    + "/erddap/sos/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
                    + "</tr>\n"
                : "")
            + (EDStatic.wcsActive
                ? "<tr>\n"
                    + "<td>wcs</td>\n"
                    + "<td>http://localhost:"
                    + PORT
                    + "/erddap/wcs/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
                    + "</tr>\n"
                : "")
            + (EDStatic.wmsActive
                ? "<tr>\n"
                    + "<td>wms</td>\n"
                    + "<td>http://localhost:"
                    + PORT
                    + "/erddap/wms/index.xhtml?page=1&amp;itemsPerPage=1000</td>\n"
                    + "</tr>\n"
                : "")
            + "</table>\n"
            + "</body>\n"
            + "</html>\n";
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
    String skip[] =
        new String[] {
          "http://",
          "http://127.0.0.1:8080/manager/html/", // will always fail this test
          "http://127.0.0.1:8080/erddap/status.html", // will always fail this test
          "https://127.0.0.1:8443/cwexperimental/login.html", // the links to log in (upper right of
          // most web
          // pages) will
          // fail on my test computer
          "https://192.168.31.18/",
          "https://basin.ceoe.udel.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException:
          // PKIX path
          // building
          // failed:
          // sun.security.provider.certpath.SunCertPathBuilderException:
          // unable to find valid certification path to requested
          // target
          "https://coastwatch.pfeg.noaa.gov/erddap/images/favicon.ico", // don't spam coastwatch
          "https://coastwatch.pfeg.noaa.gov/erddap/legal.html",
          "https://coastwatch.pfeg.noaa.gov/erddap/legal.html#privacyPolicy",
          "https://coastwatch.pfeg.noaa.gov/erddap/index.html",
          "http://coastwatch.pfeg.noaa.gov:8080/", // java.net.SocketTimeoutException: Connect timed
          // out
          "http://localhost:"
              + PORT
              + "/erddap/files/cwwcNDBCMet/nrt/NDBC_{41008,41009,41010}_met.nc", // intended
          // for
          // curl
          // (globbing)
          "http://localhost:"
              + PORT
              + "/erddap/tabledap/pmelTaoDySst.csv?longitude,latitude,time,station,wmo_platform_code,T_25&time>=2015-05-23T12:00:00Z&time<=2015-05-31T12:00:00Z", // always
          // fails
          // because
          // of
          // invalid
          // character
          "https://dev.mysql.com", // fails here, but works in browser
          "https://myhsts.org", // javax.net.ssl.SSLHandshakeException: No subject alternative DNS
          // name matching
          // myhsts.org found.
          "https://gcoos5.geos.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException:
          // PKIX path
          // building
          // failed:
          // sun.security.provider.certpath.SunCertPathBuilderException:
          // unable to find valid certification path to
          // requested target
          "https://gcoos4.tamu.edu/erddap/index.html", // javax.net.ssl.SSLHandshakeException: PKIX
          // path building
          // failed:
          // sun.security.provider.certpath.SunCertPathBuilderException:
          // unable to find valid certification path to requested
          // target
          "https://github.com/ERDDAP/", // fails here, but works in browser
          "http://localhost:" + PORT + "/manager/html/", // will always fail this test
          "https://mvnrepository.com/artifact/com.datastax.cassandra/cassandra-driver-core", // it's
          // clever: no
          // follow
          "https://mvnrepository.com/artifact/com.codahale.metrics/metrics-core/3.0.2", // it's
          // clever:
          // no follow
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
          "http://localhost:"
              + PORT
              + "/erddap/griddap/erdMHchla8day.timeGaps", // dataset not found
          "https://linux.die.net/man/1/ncdump", // fail, works in browser
        };
    // https://unitsofmeasure.org/ucum.html fails in tests because of certificate,
    // but succeeds in my browser. Others are like this, too.
    for (int linei = 0; linei < lines.length; linei++) {
      String urls[] =
          String2.extractAllCaptureGroupsAsHashSet(lines[linei], pattern, 1).toArray(new String[0]);
      for (int urli = 0; urli < urls.length; urli++) {
        // just try a given url once
        if (tried.contains(urls[urli])) continue;
        tried.add(urls[urli]);

        String ttUrl = XML.decodeEntities(urls[urli]);
        if (String2.indexOf(skip, ttUrl) >= 0) continue;
        String msg = attemptUrlConnectionWithRetry(ttUrl);
        if (msg != null) {
          String fullMsg = "#" + ++errorCount + " line=" + String2.left("" + (linei + 1), 4) + msg;
          String2.log(fullMsg);
          log.append(fullMsg + "\n");
        }
      }
    }
    if (log.length() > 0)
      throw new RuntimeException(
          "\nSSR.testForBrokenLinks(" + tUrl + ") found:\n" + log.toString());
  }

  private String attemptUrlConnectionWithRetry(String ttUrl) {
    String msg = null;
    int RETRY_ATTEMPTS = 5;
    for (int i = 0; i <= RETRY_ATTEMPTS; i++) {
      try {
        Object[] o3 =
            SSR.getUrlConnBufferedInputStream(
                ttUrl, 20000, false, true); // timeOutMillis, requestCompression, touchMode
        if (o3[0] == null) {
          ((InputStream) o3[1]).close();
          throw new IOException("The URL for SSR.testForBrokenLinks can't be an AWS S3 URL.");
        }
        HttpURLConnection conn = (HttpURLConnection) o3[0];
        int code = conn.getResponseCode();
        if (code != 200) msg = " code=" + code + " " + ttUrl;
      } catch (Exception e) {
        msg = " code=ERR " + ttUrl + " error=\n" + e.toString() + "\n";
      }
      if (msg == null || i == RETRY_ATTEMPTS) {
        continue;
      } else {
        try {
          Thread.sleep(1000 * (i + 1) * (i + 1));
          msg = null;
        } catch (InterruptedException e) {
          return msg;
        }
      }
    }
    return msg;
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testForBrokenLinks() throws Exception {
    this.testForBrokenLinks(
        "http://localhost:" + PORT + "/erddap/convert/oceanicAtmosphericAcronyms.html");
    // At least one server linked to seems to go down frequently, so commenting out for now.
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/fipscounty.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/keywords.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/time.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/units.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/convert/urls.html");
    this.testForBrokenLinks(
        "http://localhost:" + PORT + "/erddap/convert/oceanicAtmosphericVariableNames.html");

    this.testForBrokenLinks(
        "http://localhost:" + PORT + "/erddap/download/AccessToPrivateDatasets.html");
    // this.testForBrokenLinks("http://localhost:" + PORT +
    // "/erddap/download/changes.html"); // todo re-enable, a couple links seem to
    // be broken, needs more investigation
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/EDDTableFromEML.html");
    this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/download/grids.html");
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
    // this.testForBrokenLinks("http://localhost:" + PORT + "/erddap/rest.html"); //
    // todo re-enable, rganon broken
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
   * This test the json-ld responses from the ERDDAP at EDStatic.erddapUrl. It assumes jplMURSST41
   * datasets is available.
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
    results =
        SSR.getUrlResponseStringUnchanged(
            EDStatic.erddapUrl + "/info/index.html?" + EDStatic.defaultPIppQuery);

    // json-ld all datasets
    expected =
        "<script type=\"application/ld+json\">\n"
            + "{\n"
            + "  \"@context\": \"http://schema.org\",\n"
            + "  \"@type\": \"DataCatalog\",\n"
            + "  \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n"
            + "  \"url\": \"http://localhost:"
            + PORT
            + "/erddap\",\n"
            + "  \"publisher\": {\n"
            + "    \"@type\": \"Organization\",\n"
            + "    \"name\": \"ERDDAP Jetty Install\",\n"
            + "    \"address\": {\n"
            + "      \"@type\": \"PostalAddress\",\n"
            + "      \"addressCountry\": \"USA\",\n"
            + "      \"addressLocality\": \"123 Irrelevant St., Nowhere\",\n"
            + "      \"addressRegion\": \"AK\",\n"
            + "      \"postalCode\": \"99504\"\n"
            + "    },\n"
            + "    \"telephone\": \"555-555-5555\",\n"
            + "    \"email\": \"nobody@example.com\",\n"
            + "    \"sameAs\": \"http://example.com\"\n"
            + "  },\n"
            + "  \"fileFormat\": [\n"
            + "    \"application/geo+json\",\n"
            + "    \"application/json\",\n"
            + "    \"text/csv\"\n"
            + "  ],\n"
            + "  \"isAccessibleForFree\": \"True\",\n"
            + "  \"dataset\": [\n"
            + "    {\n"
            + "      \"@type\": \"Dataset\",\n"
            + "      \"name\": \"";
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    {\n"
            + "      \"@type\": \"Dataset\",\n"
            + "      \"name\": \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)\",\n"
            + "      \"sameAs\": \"http://localhost:"
            + PORT
            + "/erddap/info/erdMH1chla1day/index.html\"\n"
            + "    }";
    po = results.indexOf(expected.substring(0, 80));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // json-ld 1 dataset
    results =
        SSR.getUrlResponseStringUnchanged(EDStatic.erddapUrl + "/info/erdMH1chla1day/index.html");
    expected =
        "<script type=\"application/ld+json\">\n"
            + //
            "{\n"
            + //
            "  \"@context\": \"http://schema.org\",\n"
            + //
            "  \"@type\": \"Dataset\",\n"
            + //
            "  \"name\": \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (1 Day Composite)\",\n"
            + //
            "  \"headline\": \"erdMH1chla1day\",\n"
            + //
            "  \"description\": \"This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.\\n"
            + //
            "_lastModified=YYYY-MM-DDThh:mm:ss.000Z\\n"
            + //
            "cdm_data_type=Grid\\n"
            + //
            "Conventions=CF-1.6, COARDS, ACDD-1.3\\n"
            + //
            "Easternmost_Easting=179.9792\\n"
            + //
            "geospatial_lat_max=89.97916\\n"
            + //
            "geospatial_lat_min=-89.97918\\n"
            + //
            "geospatial_lat_units=degrees_north\\n"
            + //
            "geospatial_lon_max=179.9792\\n"
            + //
            "geospatial_lon_min=-179.9792\\n"
            + //
            "geospatial_lon_units=degrees_east\\n"
            + //
            "grid_mapping_name=latitude_longitude\\n"
            + //
            "history=Files downloaded daily from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA SWFSC ERD (erd.data@noaa.gov)\\n"
            + //
            "ERDDAP adds the time dimension.\\n"
            + //
            "Direct read of HDF4 file through CDM library\\n"
            + //
            "identifier_product_doi=10.5067/AQUA/MODIS_OC.2014.0\\n"
            + //
            "identifier_product_doi_authority=https://dx.doi.org\\n"
            + //
            "infoUrl=https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\\n"
            + //
            "institution=NOAA NMFS SWFSC ERD\\n"
            + //
            "instrument=MODIS\\n"
            + //
            "keywords_vocabulary=GCMD Science Keywords\\n"
            + //
            "l2_flag_names=ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\\n"
            + //
            "map_projection=Equidistant Cylindrical\\n"
            + //
            "measure=Mean\\n"
            + //
            "naming_authority=gov.noaa.pfeg.coastwatch\\n"
            + //
            "Northernmost_Northing=89.97916\\n"
            + //
            "platform=Aqua\\n"
            + //
            "processing_level=L3 Mapped\\n"
            + //
            "processing_version=VERSION\\n"
            + //
            "product_name=AYYYYMMDD.L3m_DAY_CHL_chlor_a_4km.nc\\n"
            + //
            "project=Ocean Biology Processing Group (NASA/GSFC/OBPG)\\n"
            + //
            "sourceUrl=(local files)\\n"
            + //
            "Southernmost_Northing=-89.97918\\n"
            + //
            "spatialResolution=DIST km\\n"
            + //
            "standard_name_vocabulary=CF Standard Name Table v70\\n"
            + //
            "temporal_range=day\\n"
            + //
            "testOutOfDate=now-3days\\n"
            + //
            "time_coverage_end=2016-10-17T12:00:00Z\\n"
            + //
            "time_coverage_start=2003-01-01T12:00:00Z\\n"
            + //
            "Westernmost_Easting=-179.9792\",\n"
            + //
            "  \"url\": \"http://localhost:8080/erddap/griddap/erdMH1chla1day.html\",\n"
            + //
            "  \"includedInDataCatalog\": {\n"
            + //
            "    \"@type\": \"DataCatalog\",\n"
            + //
            "    \"name\": \"ERDDAP Data Server at ERDDAP Jetty Install\",\n"
            + //
            "    \"sameAs\": \"http://localhost:8080/erddap\"\n"
            + //
            "  },\n"
            + //
            "  \"keywords\": [\n"
            + //
            "    \"algorithm\",\n"
            + //
            "    \"biology\",\n"
            + //
            "    \"center\",\n"
            + //
            "    \"chemistry\",\n"
            + //
            "    \"chlor_a\",\n"
            + //
            "    \"chlorophyll\",\n"
            + //
            "    \"color\",\n"
            + //
            "    \"concentration\",\n"
            + //
            "    \"data\",\n"
            + //
            "    \"Earth Science > Oceans > Ocean Chemistry > Chlorophyll\",\n"
            + //
            "    \"Earth Science > Oceans > Ocean Optics > Ocean Color\",\n"
            + //
            "    \"ecology\",\n"
            + //
            "    \"flight\",\n"
            + //
            "    \"goddard\",\n"
            + //
            "    \"group\",\n"
            + //
            "    \"gsfc\",\n"
            + //
            "    \"image\",\n"
            + //
            "    \"imaging\",\n"
            + //
            "    \"L3\",\n"
            + //
            "    \"laboratory\",\n"
            + //
            "    \"level\",\n"
            + //
            "    \"level-3\",\n"
            + //
            "    \"mapped\",\n"
            + //
            "    \"mass\",\n"
            + //
            "    \"mass_concentration_chlorophyll_concentration_in_sea_water\",\n"
            + //
            "    \"moderate\",\n"
            + //
            "    \"modis\",\n"
            + //
            "    \"nasa\",\n"
            + //
            "    \"ocean\",\n"
            + //
            "    \"ocean color\",\n"
            + //
            "    \"oceans\",\n"
            + //
            "    \"oci\",\n"
            + //
            "    \"optics\",\n"
            + //
            "    \"processing\",\n"
            + //
            "    \"resolution\",\n"
            + //
            "    \"sea\",\n"
            + //
            "    \"seawater\",\n"
            + //
            "    \"smi\",\n"
            + //
            "    \"space\",\n"
            + //
            "    \"spectroradiometer\",\n"
            + //
            "    \"standard\",\n"
            + //
            "    \"time\",\n"
            + //
            "    \"water\"\n"
            + //
            "  ],\n"
            + //
            "  \"license\": \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\\n"
            + //
            "The data may be used and redistributed for free but is not intended\\n"
            + //
            "for legal use, since it may contain inaccuracies. Neither the data\\n"
            + //
            "Contributor, ERD, NOAA, nor the United States Government, nor any\\n"
            + //
            "of their employees or contractors, makes any warranty, express or\\n"
            + //
            "implied, including warranties of merchantability and fitness for a\\n"
            + //
            "particular purpose, or assumes any legal liability for the accuracy,\\n"
            + //
            "completeness, or usefulness, of this information.\",\n"
            + //
            "  \"variableMeasured\": [\n"
            + //
            "    {\n"
            + //
            "      \"@type\": \"PropertyValue\",\n"
            + //
            "      \"name\": \"time\",\n"
            + //
            "      \"alternateName\": \"Centered Time\",\n"
            + //
            "      \"description\": \"Centered Time\",\n"
            + //
            "      \"valueReference\": [\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axisOrDataVariable\",\n"
            + //
            "          \"value\": \"axis\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"_CoordinateAxisType\",\n"
            + //
            "          \"value\": \"Time\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axis\",\n"
            + //
            "          \"value\": \"T\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"ioos_category\",\n"
            + //
            "          \"value\": \"Time\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"long_name\",\n"
            + //
            "          \"value\": \"Centered Time\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"standard_name\",\n"
            + //
            "          \"value\": \"time\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"time_origin\",\n"
            + //
            "          \"value\": \"01-JAN-1970 00:00:00\"\n"
            + //
            "        }\n"
            + //
            "      ],\n"
            + //
            "      \"maxValue\": \"2016-10-17T12:00:00Z\",\n"
            + //
            "      \"minValue\": \"2003-01-01T12:00:00Z\",\n"
            + //
            "      \"propertyID\": \"time\"\n"
            + //
            "    },\n"
            + //
            "    {\n"
            + //
            "      \"@type\": \"PropertyValue\",\n"
            + //
            "      \"name\": \"latitude\",\n"
            + //
            "      \"alternateName\": \"Latitude\",\n"
            + //
            "      \"description\": \"Latitude\",\n"
            + //
            "      \"valueReference\": [\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axisOrDataVariable\",\n"
            + //
            "          \"value\": \"axis\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"_CoordinateAxisType\",\n"
            + //
            "          \"value\": \"Lat\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axis\",\n"
            + //
            "          \"value\": \"Y\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"ioos_category\",\n"
            + //
            "          \"value\": \"Location\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"long_name\",\n"
            + //
            "          \"value\": \"Latitude\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"standard_name\",\n"
            + //
            "          \"value\": \"latitude\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"valid_max\",\n"
            + //
            "          \"value\": 90\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"valid_min\",\n"
            + //
            "          \"value\": -90\n"
            + //
            "        }\n"
            + //
            "      ],\n"
            + //
            "      \"maxValue\": 89.97916,\n"
            + //
            "      \"minValue\": -89.97918,\n"
            + //
            "      \"propertyID\": \"latitude\",\n"
            + //
            "      \"unitText\": \"degrees_north\"\n"
            + //
            "    },\n"
            + //
            "    {\n"
            + //
            "      \"@type\": \"PropertyValue\",\n"
            + //
            "      \"name\": \"longitude\",\n"
            + //
            "      \"alternateName\": \"Longitude\",\n"
            + //
            "      \"description\": \"Longitude\",\n"
            + //
            "      \"valueReference\": [\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axisOrDataVariable\",\n"
            + //
            "          \"value\": \"axis\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"_CoordinateAxisType\",\n"
            + //
            "          \"value\": \"Lon\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axis\",\n"
            + //
            "          \"value\": \"X\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"ioos_category\",\n"
            + //
            "          \"value\": \"Location\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"long_name\",\n"
            + //
            "          \"value\": \"Longitude\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"standard_name\",\n"
            + //
            "          \"value\": \"longitude\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"valid_max\",\n"
            + //
            "          \"value\": 180\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"valid_min\",\n"
            + //
            "          \"value\": -180\n"
            + //
            "        }\n"
            + //
            "      ],\n"
            + //
            "      \"maxValue\": 179.9792,\n"
            + //
            "      \"minValue\": -179.9792,\n"
            + //
            "      \"propertyID\": \"longitude\",\n"
            + //
            "      \"unitText\": \"degrees_east\"\n"
            + //
            "    },\n"
            + //
            "    {\n"
            + //
            "      \"@type\": \"PropertyValue\",\n"
            + //
            "      \"name\": \"chlorophyll\",\n"
            + //
            "      \"alternateName\": \"Mean Chlorophyll a Concentration\",\n"
            + //
            "      \"description\": \"Mean Chlorophyll a Concentration\",\n"
            + //
            "      \"valueReference\": [\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"axisOrDataVariable\",\n"
            + //
            "          \"value\": \"data\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"_FillValue\",\n"
            + //
            "          \"value\": null\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"colorBarMaximum\",\n"
            + //
            "          \"value\": 30\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"colorBarMinimum\",\n"
            + //
            "          \"value\": 0.03\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"colorBarScale\",\n"
            + //
            "          \"value\": \"Log\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"ioos_category\",\n"
            + //
            "          \"value\": \"Ocean Color\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"long_name\",\n"
            + //
            "          \"value\": \"Mean Chlorophyll a Concentration\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"references\",\n"
            + //
            "          \"value\": \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"standard_name\",\n"
            + //
            "          \"value\": \"concentration_of_chlorophyll_in_sea_water\"\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"valid_max\",\n"
            + //
            "          \"value\": 100\n"
            + //
            "        },\n"
            + //
            "        {\n"
            + //
            "          \"@type\": \"PropertyValue\",\n"
            + //
            "          \"name\": \"valid_min\",\n"
            + //
            "          \"value\": 0.001\n"
            + //
            "        }\n"
            + //
            "      ],\n"
            + //
            "      \"propertyID\": \"concentration_of_chlorophyll_in_sea_water\",\n"
            + //
            "      \"unitText\": \"mg m-3\"\n"
            + //
            "    }\n"
            + //
            "  ],\n"
            + //
            "  \"creator\": {\n"
            + //
            "    \"@type\": \"Organization\",\n"
            + //
            "    \"name\": \"NASA/GSFC/OBPG\",\n"
            + //
            "    \"email\": \"data@oceancolor.gsfc.nasa.gov\",\n"
            + //
            "    \"sameAs\": \"https://oceandata.sci.gsfc.nasa.gov\"\n"
            + //
            "  },\n"
            + //
            "  \"publisher\": {\n"
            + //
            "    \"@type\": \"Organization\",\n"
            + //
            "    \"name\": \"NOAA NMFS SWFSC ERD\",\n"
            + //
            "    \"email\": \"erd.data@noaa.gov\",\n"
            + //
            "    \"sameAs\": \"https://www.pfeg.noaa.gov\"\n"
            + //
            "  },\n"
            + //
            "  \"dateCreated\": \"2016-10-18T06:45:00.000Z\",\n"
            + //
            "  \"identifier\": \"erdMH1chla1day\",\n"
            + //
            "  \"temporalCoverage\": \"2003-01-01T12:00:00Z/2016-10-17T12:00:00Z\",\n"
            + //
            "  \"spatialCoverage\": {\n"
            + //
            "    \"@type\": \"Place\",\n"
            + //
            "    \"geo\": {\n"
            + //
            "      \"@type\": \"GeoShape\",\n"
            + //
            "      \"box\": \"-89.97918 -179.9792 89.97916 179.9792\"\n"
            + //
            "    }\n"
            + //
            "  }\n"
            + //
            "}\n"
            + //
            "</script>\n";
    results =
        results.replaceAll(
            "time_coverage_end=....-..-..T09:00:00Z", "time_coverage_end=yyyy-mm-ddT09:00:00Z");
    results = results.replaceAll("....-..-..T09:00:00Z", "yyyy-mm-ddT09:00:00Z");
    results =
        results.replaceAll(
            "dateCreated\\\": \\\"....-..-..T..:..:..Z",
            "dateCreated\\\": \\\"yyyy-mm-ddThh:mm:ssZ");
    results = results.replaceAll("100_multi_........1200", "100_multi_yyyymmdd1200");
    results =
        results.replaceAll(
            "_lastModified=....-..-..T..:..:...000Z", "_lastModified=YYYY-MM-DDThh:mm:ss.000Z");
    results =
        results.replaceAll(
            "processing_version=[0-9]+.[0-9].?.?.?.?\\\\n", "processing_version=VERSION\\\\n");
    results =
        results.replaceAll(
            "product_name=A[0-9]+.L3m_DAY_CHL_chlor_a_4km.nc",
            "product_name=AYYYYMMDD.L3m_DAY_CHL_chlor_a_4km.nc");
    results = results.replaceAll("spatialResolution=[0-9]+.[0-9]+ km", "spatialResolution=DIST km");
    po = Math.max(0, results.indexOf(expected.substring(0, 80)));
    Test.ensureEqual(
        results.substring(po, Math.min(results.length(), po + expected.length())),
        expected,
        "results=\n" + results);
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
    String expected = "pmelTaoMonPos";
    String expected2, query, results;
    String2.log(
        "\n*** Erddap.testAdvancedSearch\n"
            + "This assumes localhost ERDDAP is running with at least glerAvgTemp.");
    int po;

    // test valid search string, values are case-insensitive
    query = "";
    String goodQueries[] = {
      "&searchFor=pmelTao",
      "&protocol=TAbleDAp",
      "&short_name=depth",
      "&minLat=-45&maxLat=45",
      "&minLon=-25&maxLon=25",
      "&minTime=now-3years&maxTime=now-1years"
    };
    for (int i = 0; i < goodQueries.length; i++) {
      query += goodQueries[i];
      results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
      Test.ensureTrue(results.indexOf(expected) >= 0, "i=" + i + " results=\n" + results);
    }

    // valid for .html but error for .csv: protocol
    query = "&searchFor=pmelTao&protocol=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 =
        "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (protocol=gibberish)\";\n"
            + "})";
    Test.ensureTrue(
        results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: standard_name
    query = "&searchFor=pmelTao&standard_name=gibberish";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 =
        "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Your query produced no matching results. (standard_name=gibberish)\";\n"
            + "})";
    Test.ensureTrue(
        results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minLat > &maxLat
    query = "&searchFor=pmelTao&minLat=45&maxLat=0";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 =
        "(Error {\n"
            + "    code=400;\n"
            + "    message=\"Bad Request: Query error: minLat=45.0 > maxLat=0.0\";\n"
            + "})";
    Test.ensureTrue(
        results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));

    // valid for .html but error for .csv: &minTime > &maxTime
    query = "&searchFor=pmelTao&minTime=now-10years&maxTime=now-11years";
    results = SSR.getUrlResponseStringUnchanged(htmlUrl + query);
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);
    try {
      results = SSR.getUrlResponseStringUnchanged(csvUrl + query);
    } catch (Throwable t) {
      results = t.toString();
    }
    expected2 =
        "(Error {\n"
            + "    code=400;\n"
            + "    message=\"Bad Request: Query error: minTime=now-10years > maxTime=now-11years\";\n"
            + "})";
    Test.ensureTrue(
        results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));
  }

  /* EDDTableFromNcFiles */

  /** The basic tests of this class. */
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
    String encodedUserDapQuery =
        "longitude,NO3,time,ship&latitude%3E0&altitude%3E-5&time%3E=2002-08-03";
    String regexDapQuery =
        "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-03"
            + "&longitude"
            + PrimitiveArray.REGEX_OP
            + "\".*11.*\"";

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
    String allVars =
        "cruise_id, ship, cast, longitude, latitude, altitude, time, bottle_posn, "
            + "chl_a_total, chl_a_10um, phaeo_total, phaeo_10um, sal00, sal11, temperature0, "
            + "temperature1, fluor_v, xmiss_v, PO4, N_N, NO3, Si, NO2, NH4, oxygen, par";

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

    globecBottle.parseUserDapQuery(
        language, "s.longitude,s.altitude&s.altitude=0", rv, cv, co, cv2, false);
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
      globecBottle.parseUserDapQuery(
          language, "time&time=now", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 14)
              + ".{5}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.SECOND, -7);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(
          language, "time&time=now-7seconds", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will
      // be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 14)
              + ".{5}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.MINUTE, -5);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(
          language, "time&time=now-5minutes", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will
      // be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 14)
              + ".{5}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.HOUR_OF_DAY, -4);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(
          language, "time&time=now-4hours", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will
      // be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results, // This fails rarely (at minute transitions). Just rerun it.
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.DATE, -2);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(
          language, "time&time=now-2days", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will
      // be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.MONTH, -3);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(
          language, "time&time=now-3months", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will
      // be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    gc = Calendar2.newGCalendarZulu();
    gc.add(Calendar2.YEAR, -2);
    s = Calendar2.formatAsISODateTimeT(gc);
    try {
      globecBottle.parseUserDapQuery(
          language, "time&time=now-2years", rv, cv, co, cv2, false); // non-regex
      // EDVTimeStamp
      // conValues
      // will
      // be
      // ""+epochSeconds
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      results = t.toString();
      Test.ensureLinesMatch(
          results,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "\\(time="
              + s.substring(0, 17)
              + ".{2}Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z\\)",
          "results=\n" + results);
    }

    // longitude converted to lon
    // altitude is fixed value, so test done by getSourceQueryFromDapQuery
    // time test deferred till later, so 'datetime_epoch' included in sourceQuery
    // also, lat is added to var list because it is used in constraints
    globecBottle.getSourceQueryFromDapQuery(language, userDapQuery, rv, cv, co, cv2);
    Test.ensureEqual(
        EDDTableFromNcFiles.formatAsDapQuery(
            rv.toArray(), cv.toArray(), co.toArray(), cv2.toArray()),
        "lon100,no3,datetime_epoch,ship,lat100&lat100>0&datetime_epoch>=1.0283328E9",
        "Unexpected sourceDapQuery from userDapQuery=" + userDapQuery);

    // test invalid queries
    try {
      // lon is the source name
      globecBottle.getSourceQueryFromDapQuery(language, "lon,cast", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: Unrecognized variable=\"lon\".",
        "error=" + error);

    error = "";
    try {
      // a variable can't be listed twice
      globecBottle.getSourceQueryFromDapQuery(
          language, "cast,longitude,cast,latitude", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: variable=cast is listed twice in the results variables list.",
        "error=" + error);

    error = "";
    try {
      // if s is used, it must be the only request var
      globecBottle.getSourceQueryFromDapQuery(language, "s.latitude,s", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: If s is requested, it must be the only requested variable.",
        "error=" + error);

    error = "";
    try {
      // zztop isn't valid variable
      globecBottle.getSourceQueryFromDapQuery(language, "cast,zztop", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: Unrecognized variable=\"zztop\".",
        "error=" + error);

    error = "";
    try {
      // alt is fixedValue=0 so will always return NO_DATA
      globecBottle.getSourceQueryFromDapQuery(language, "cast&altitude<-1", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Your query produced no matching results. (altitude<-1 is outside of the variable's actual_range: 0 to 0)",
        "error=" + error);

    error = "";
    try {
      // lon isn't a valid var
      globecBottle.getSourceQueryFromDapQuery(language, "NO3, Si&lon=0", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: Unrecognized constraint variable=\"lon\".",
        "error=" + error);

    error = "";
    try {
      // should be ==, not =
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&altitude==0", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: Use '=' instead of '==' in constraints.",
        "error=" + error);

    error = "";
    try {
      // regex operator should be =~, not ~=
      globecBottle.getSourceQueryFromDapQuery(
          language, "NO3,Si&altitude~=(0|1.*)", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: Use '=~' instead of '~=' in constraints.",
        "error=" + error);

    error = "";
    try {
      // string regex values must be in quotes
      globecBottle.getSourceQueryFromDapQuery(language, "NO3,Si&ship=New_Horizon", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: For constraints of String variables, the right-hand-side value must be surrounded by double quotes. Bad constraint: ship=New_Horizon",
        "error=" + error);

    error = "";
    try {
      // numeric variable regex values must be in quotes
      globecBottle.getSourceQueryFromDapQuery(
          language, "NO3,Si&altitude=~(0|1.*)", rv, cv, co, cv2);
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: For =~ constraints of numeric variables, "
            + "the right-hand-side value must be surrounded by double quotes. Bad constraint: altitude=~(0|1.*)",
        "error=" + error);

    error = "";
    try {
      globecBottle.getSourceQueryFromDapQuery(
          language,
          "NO3,Si&altitude=0|longitude>-180",
          rv,
          cv,
          co,
          cv2); // invalid query format caught as invalid NaN
    } catch (Throwable t) {
      error = MustBe.throwableToString(t);
    }
    Test.ensureEqual(
        String2.split(error, '\n')[0],
        "SimpleException: Query error: Numeric tests of NaN must use \"NaN\", "
            + "not value=\"0|longitude>-180\".",
        "error=" + error);

    results =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.nowStringToEpochSeconds("now-1 day"))
            .substring(0, 16);
    expected =
        Calendar2.epochSecondsToIsoStringTZ(Calendar2.nowStringToEpochSeconds("now-1day"))
            .substring(0, 16);
    Test.ensureEqual(results, expected, "");

    error = "";
    String nowQ[] = {"nowa", "now-", "now-5.5days", "now-5date", "now-9dayss"};
    for (int i = 0; i < nowQ.length; i++) {
      try {
        globecBottle.getSourceQueryFromDapQuery(
            language,
            "time&time=" + nowQ[i],
            rv,
            cv,
            co,
            cv2); // invalid query format caught as invalid NaN
      } catch (Throwable t) {
        error = MustBe.throwableToString(t);
      }
      Test.ensureEqual(
          String2.split(error, '\n')[0],
          "SimpleException: Query error: Invalid \"now\" constraint: \""
              + nowQ[i]
              + "\". "
              + "Timestamp constraints with \"now\" must be in "
              + "the form \"now[+|-positiveInteger[millis|seconds|minutes|hours|days|months|years]]\""
              + " (or singular units).",
          "error=" + error);
    }

    // time tests are perfectly precise: actual_range 1.02272886e+9, 1.02978828e+9;
    // but now have 1 second fudge
    try {
      globecBottle.getSourceQueryFromDapQuery(
          language, "&time=1.022728858e9", rv, cv, co, cv2); // sec
      // -2
      // in
      // last+1
      // digit
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      error = t.toString();
      Test.ensureEqual(
          error,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "(time=2002-05-30T03:20:58Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z)",
          "");
    }

    try {
      globecBottle.getSourceQueryFromDapQuery(
          language, "&time=1.029788282e9", rv, cv, co, cv2); // max
      // +2
      // in
      // last+1
      // digit
      throw new SimpleException("shouldn't get here");
    } catch (Throwable t) {
      error = t.toString();
      Test.ensureEqual(
          error,
          "com.cohort.util.SimpleException: Your query produced no matching results. "
              + "(time=2002-08-19T20:18:02Z is outside of the variable's actual_range: "
              + "2002-05-30T03:21:00Z to 2002-08-19T20:18:00Z)",
          "");
    }

    // impossible queries lon range is float -126.2, -124.1
    String impossibleQuery[] =
        new String[] {
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
      Test.ensureEqual(
          String2.split(error, '\n')[0],
          "SimpleException: Query error: "
              + String2.replaceAll(impossibleQuery[i].substring(1), "&", " and ")
              + " will never both be true.",
          "error=" + error);
    }
    // possible queries lon range is float -126.2, -124.1
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=-126.2", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<=-126.2", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude<-126.2", rv, cv, co, cv2); // good: fuzzy
    // test
    // allows it
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude=-124.1", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>=-124.1", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude>-124.1", rv, cv, co, cv2); // good: fuzzy
    // test
    // allows it
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude<-126.0&longitude<=-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude>=-126.0&longitude>-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude=-126.0&longitude<-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude>-126.0&longitude=-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude>=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude<=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(language, "&longitude!=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude<-126.0&longitude!=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude!=NaN&longitude=-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude=NaN&longitude<=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude<=NaN&longitude=NaN", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude=NaN&longitude!=-125.0", rv, cv, co, cv2);
    globecBottle.getSourceQueryFromDapQuery(
        language, "&longitude!=-125.0&longitude=NaN", rv, cv, co, cv2);
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
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("latitude");
    results = edv.sliderCsvValues();
    expected = "41.9, 41.92, 41.94, 41.96, 41.98";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "44.56, 44.58, 44.6, 44.62, 44.64, 44.65";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("altitude");
    results = edv.sliderCsvValues();
    expected = "0"; // 0
    Test.ensureEqual(results, expected, "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("time");
    results = edv.sliderCsvValues();
    // 2002-05-30T03:21:00Z 2002-08-19T20:18:00Z
    expected =
        "\"2002-05-30T03:21:00Z\", \"2002-05-30T12:00:00Z\", \"2002-05-31\", \"2002-05-31T12:00:00Z\",";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "\"2002-08-19\", \"2002-08-19T12:00:00Z\", \"2002-08-19T20:18:00Z\"";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("phaeo_total");
    results = edv.sliderCsvValues();
    expected = "-3.111, -3, -2.8, -2.6, -2.4, -2.2,"; // -3.111
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);
    expected = "32.8, 33, 33.2, 33.4, 33.6, 33.821"; // 33.821
    Test.ensureEqual(
        results.substring(results.length() - expected.length()), expected, "results=\n" + results);

    edv = globecBottle.findDataVariableByDestinationName("cruise_id");
    results = edv.sliderCsvValues();
    expected = null;
    Test.ensureEqual(results, expected, "results=\n" + results);

    // *** print ncdump of file (useful for diagnosing problems in next section)
    tName = "/u00/data/points/globec/Globec_bottle_data_2002.nc";
    String2.log(tName + "\n" + NcHelper.ncdump(tName, "-h"));

    // *** test getting das for entire dataset
    String2.log("\n*** EDDTableFromNcFiles.test das dds for entire dataset\n");
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, "", dir, globecBottle.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    String expectedDas1 = // see OpendapHelper.EOL for comments
        "Attributes {\n"
            + " s {\n"
            + "  cruise_id {\n"
            + "    String cf_role \"trajectory_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Cruise ID\";\n"
            + "  }\n"
            + "  ship {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Ship\";\n"
            + "  }\n"
            + "  cast {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Int16 actual_range 1, 127;\n"
            + "    Float64 colorBarMaximum 140.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Cast Number\";\n"
            + "    Int16 missing_value 32767;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 _FillValue 327.67;\n"
            + "    Float32 actual_range -126.2, -124.1;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    Float32 missing_value 327.67;\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 _FillValue 327.67;\n"
            + "    Float32 actual_range 41.9, 44.65;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    Float32 missing_value 327.67;\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  altitude {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"up\";\n"
            + "    Int32 actual_range 0, 0;\n"
            + "    String axis \"Z\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Altitude\";\n"
            + "    String positive \"up\";\n"
            + "    String standard_name \"altitude\";\n"
            + "    String units \"m\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range 1.02272886e+9, 1.02978828e+9;\n"
            + "    String axis \"T\";\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  bottle_posn {\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + // ERDDAP adds
            "    Byte actual_range 0, 12;\n"
            + "    Float64 colorBarMaximum 12.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Bottle Number\";\n"
            + "    Byte missing_value -128;\n"
            + "  }\n"
            + "  chl_a_total {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range -2.602, 40.17;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Chlorophyll-a\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n";
    Test.ensureEqual(
        results.substring(0, expectedDas1.length()), expectedDas1, "\nresults=\n" + results);

    String expectedDas2 =
        "String id \"Globec_bottle_data_2002\";\n"
            + "    String infoUrl \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n"
            + "    String institution \"GLOBEC\";\n"
            + "    String keywords \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 44.65;\n"
            + "    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n"
            + "    Float64 Southernmost_Northing 41.9;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n"
            + "    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n"
            + "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n"
            + "Notes:\n"
            + "Physical data processed by Jane Fleischbein (OSU).\n"
            + "Chlorophyll readings done by Leah Feinberg (OSU).\n"
            + "Nutrient analysis done by Burke Hales (OSU).\n"
            + "Sal00 - salinity calculated from primary sensors (C0,T0).\n"
            + "Sal11 - salinity calculated from secondary sensors (C1,T1).\n"
            + "secondary sensor pair was used in final processing of CTD data for\n"
            + "most stations because the primary had more noise and spikes. The\n"
            + "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n"
            + "multiple spikes or offsets in the secondary pair.\n"
            + "Nutrient samples were collected from most bottles; all nutrient data\n"
            + "developed from samples frozen during the cruise and analyzed ashore;\n"
            + "data developed by Burke Hales (OSU).\n"
            + "Operation Detection Limits for Nutrient Concentrations\n"
            + "Nutrient  Range         Mean    Variable         Units\n"
            + "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n"
            + "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n"
            + "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n"
            + "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n"
            + "Dates and Times are UTC.\n"
            + "\n"
            + "For more information, see https://www.bco-dmo.org/dataset/2452\n"
            + "\n"
            + "Inquiries about how to access this data should be directed to\n"
            + "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n"
            + "    String time_coverage_end \"2002-08-19T20:18:00Z\";\n"
            + "    String time_coverage_start \"2002-05-30T03:21:00Z\";\n"
            + "    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n"
            + "    Float64 Westernmost_Easting -126.2;\n"
            + "  }\n"
            + "}\n";
    po = results.indexOf(expectedDas2.substring(0, 17));
    Test.ensureEqual(results.substring(Math.max(0, po)), expectedDas2, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, "", dir, globecBottle.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String cruise_id;\n"
            + "    String ship;\n"
            + "    Int16 cast;\n"
            + "    Float32 longitude;\n"
            + "    Float32 latitude;\n"
            + "    Int32 altitude;\n"
            + "    Float64 time;\n"
            + "    Byte bottle_posn;\n"
            + "    Float32 chl_a_total;\n"
            + "    Float32 chl_a_10um;\n"
            + "    Float32 phaeo_total;\n"
            + "    Float32 phaeo_10um;\n"
            + "    Float32 sal00;\n"
            + "    Float32 sal11;\n"
            + "    Float32 temperature0;\n"
            + "    Float32 temperature1;\n"
            + "    Float32 fluor_v;\n"
            + "    Float32 xmiss_v;\n"
            + "    Float32 PO4;\n"
            + "    Float32 N_N;\n"
            + "    Float32 NO3;\n"
            + "    Float32 Si;\n"
            + "    Float32 NO2;\n"
            + "    Float32 NH4;\n"
            + "    Float32 oxygen;\n"
            + "    Float32 par;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, "", dir, globecBottle.className() + "_Entire", ".html");
    // Test.displayInBrowser("file://" + dir + tName);

    // *** test make data files
    String2.log("\n*** EDDTableFromNcFiles.test make DATA FILES\n");

    // .asc
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".asc");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  Sequence {[10]\n"
            + "    Float32 longitude;[10]\n"
            + "    Float32 NO3;[10]\n"
            + "    Float64 time;[10]\n"
            + "    String ship;[10]\n"
            + "  } s;[10]\n"
            + "} s;[10]\n"
            + "---------------------------------------------[10]\n"
            + "s.longitude, s.NO3, s.time, s.ship[10]\n"
            + "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"[10]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"[10]\n"; // row with missing value has source
    // missing
    // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1, 24.45, 1.02978828E9, \"New_Horizon\"[10]\n[end]"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; // row with missing value has "NaN"
    // missing
    // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csvp and &units("UCUM")
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery + "&units(\"UCUM\")",
            dir,
            globecBottle.className() + "_Data",
            ".csvp");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude (deg{east}),NO3 (umol.l-1),time (UTC),ship\n"
            + "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; // row with missing value has "NaN"
    // missing
    // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv0
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".csv0");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "-124.82,NaN,2002-08-17T00:49:00Z,New_Horizon\n"; // row with missing value has "NaN"
    // missing
    // value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv test of datasetName.dataVarName notation
    String dotDapQuery =
        "s.longitude,altitude,NO3,s.time,ship" + "&s.latitude>0&altitude>-5&s.time>=2002-08-03";
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            dotDapQuery,
            dir,
            globecBottle.className() + "_DotNotation",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,altitude,NO3,time,ship\n"
            + "degrees_east,m,micromoles L-1,UTC,\n"
            + "-124.4,0,35.7,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,0,35.48,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,0,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected = "-124.1,0,24.45,2002-08-19T20:18:00Z,New_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .csv test of regex on numeric variable
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            regexDapQuery,
            dir,
            globecBottle.className() + "_NumRegex",
            ".csv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-125.11,33.91,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,26.61,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,10.8,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,8.42,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,6.34,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,1.29,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,0.02,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,0.0,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,10.81,2002-08-09T05:03:00Z,New_Horizon\n"
            + "-125.11,42.39,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,33.84,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,27.67,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,15.93,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,8.69,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,4.6,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,2.17,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,8.61,2002-08-18T23:49:00Z,New_Horizon\n"
            + "-125.11,0.64,2002-08-18T23:49:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv test of String=
    String tDapQuery =
        "longitude,NO3,time,ship&latitude>0&altitude>-5"
            + "&time>=2002-08-07T00&time<=2002-08-07T06&ship=\"New_Horizon\"";
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, tDapQuery, dir, globecBottle.className() + "_StrEq", ".csv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .csv test of String< >
    tDapQuery =
        "longitude,NO3,time,ship&latitude>0&altitude>-5"
            + "&time>=2002-08-07T00&time<=2002-08-07T06&ship>\"Nev\"&ship<\"Nex\"";
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, tDapQuery, dir, globecBottle.className() + "_GTLT", ".csv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
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
    tDapQuery =
        "longitude,NO3,time,ship&latitude>0"
            + "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\"(zztop|.*Horiz.*)\""; // source
    // fails
    // with this
    // "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\".*Horiz.*\""; //source
    // works with this
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, tDapQuery, dir, globecBottle.className() + "_regex", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-124.8,34.54,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,29.98,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,17.24,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,12.74,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,11.43,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,NaN,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,9.74,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,5.62,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,4.4,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-124.8,4.21,2002-08-07T01:52:00Z,New_Horizon\n"
            + "-125.0,35.28,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,30.87,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,25.2,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,20.66,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,NaN,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,10.85,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,5.44,2002-08-07T03:43:00Z,New_Horizon\n"
            + "-125.0,4.69,2002-08-07T03:43:00Z,New_Horizon\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dataTable
    tDapQuery =
        "longitude,NO3,time,ship&latitude>0"
            + "&time>=2002-08-07T00&time<=2002-08-07T06&ship=~\"(zztop|.*Horiz.*)\""; // source
    // fails
    // with this
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            tDapQuery,
            dir,
            globecBottle.className() + "_regex_dataTable",
            ".dataTable");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = // note that month is 0-based
        "{\"cols\":[{\"id\":\"longitude\",\"label\":\"longitude (degrees_east) \",\"pattern\":\"\",\"type\":\"number\"},{\"id\":\"NO3\",\"label\":\"NO3 (micromoles L-1) \",\"pattern\":\"\",\"type\":\"number\"},{\"id\":\"time\",\"label\":\"time\",\"pattern\":\"\",\"type\":\"datetime\"},{\"id\":\"ship\",\"label\":\"ship\",\"pattern\":\"\",\"type\":\"string\"}],\n"
            + "\"rows\": [\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":34.54,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":29.98,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":17.24,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":12.74,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":11.43,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":null,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":9.74,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":5.62,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":4.4,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-124.8,\"f\":null},{\"v\":4.21,\"f\":null},{\"v\":\"Date(2002, 7, 7, 1, 52, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":35.28,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":30.87,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":25.2,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":20.66,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":null,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":null,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":10.85,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":5.44,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]},\n"
            + "{\"c\":[{\"v\":-125.0,\"f\":null},{\"v\":4.69,\"f\":null},{\"v\":\"Date(2002, 7, 7, 3, 43, 0, 0)\",\"f\":null},{\"v\":\"New_Horizon\",\"f\":null}]}\n"
            + "    ]\n"
            + "  }\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .das das isn't affected by userDapQuery
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    Test.ensureEqual(
        results.substring(0, expectedDas1.length()), expectedDas1, "results=\n" + results);

    int tpo = results.indexOf(expectedDas2.substring(0, 17));
    Test.ensureEqual(results.substring(Math.max(tpo, 0)), expectedDas2, "results=\n" + results);

    // .dds
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".dds");
    results = String2.annotatedString(File2.directReadFrom88591File(dir + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  Sequence {[10]\n"
            + "    Float32 longitude;[10]\n"
            + "    Float32 NO3;[10]\n"
            + "    Float64 time;[10]\n"
            + "    String ship;[10]\n"
            + "  } s;[10]\n"
            + "} s;[10]\n"
            + "[end]";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dods
    // tName = globecBottle.makeNewFileForDapQuery(language, null, null,
    // userDapQuery, dir,
    // globecBottle.className() + "_Data", ".dods");
    // Test.displayInBrowser("file://" + dir + tName);
    try {
      String2.log("\ndo .dods test");
      String tUrl =
          EDStatic.erddapUrl
              + // in tests, always use non-https url
              "/tabledap/"
              + globecBottle.datasetID();
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
      Test.ensureEqual(
          tTable.globalAttributes().getString("title"),
          "GLOBEC NEP Rosette Bottle Data (2002)",
          "");
      Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
      Test.ensureEqual(
          tTable.getColumnNames(), new String[] {"longitude", "NO3", "time", "ship"}, "");
      Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
      Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
      Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
      Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
      String2.log("  .dods test succeeded");
    } catch (Throwable t) {
      throw new RuntimeException(
          "*** This test requires " + globecBottle.datasetID() + " in localhost ERDDAP.", t);
    }

    // .esriCsv
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, "&time>=2002-08-03", dir, "testEsri5", ".esriCsv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "cruise_id,ship,cast,X,Y,altitude,date,time,bottle_pos,chl_a_tota,chl_a_10um,phaeo_tota,phaeo_10um,sal00,sal11,temperatur,temperatuA,fluor_v,xmiss_v,PO4,N_N,NO3,Si,NO2,NH4,oxygen,par\n"
            + "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,1,-9999.0,-9999.0,-9999.0,-9999.0,33.9939,33.9908,7.085,7.085,0.256,0.518,2.794,35.8,35.7,71.11,0.093,0.037,-9999.0,0.1545\n"
            + "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,2,-9999.0,-9999.0,-9999.0,-9999.0,33.8154,33.8111,7.528,7.53,0.551,0.518,2.726,35.87,35.48,57.59,0.385,0.018,-9999.0,0.1767\n"
            + "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,3,1.463,-9999.0,1.074,-9999.0,33.5858,33.5834,7.572,7.573,0.533,0.518,2.483,31.92,31.61,48.54,0.307,0.504,-9999.0,0.3875\n"
            + "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,4,2.678,-9999.0,1.64,-9999.0,33.2905,33.2865,8.093,8.098,1.244,0.518,2.262,27.83,27.44,42.59,0.391,0.893,-9999.0,0.7674\n"
            + "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,5,4.182,-9999.0,2.363,-9999.0,33.2871,33.2863,8.157,8.141,1.458,0.518,2.202,26.15,25.73,40.25,0.424,1.204,-9999.0,0.7609\n"
            + "nh0207,New_Horizon,20,-124.4,44.0,0,2002-08-03,1:29:00 am,6,7.601,-9999.0,3.959,-9999.0,33.3753,33.3678,11.733,11.73,3.685,0.518,1.092,8.96,8.75,16.31,0.211,1.246,-9999.0,1.9563\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // .geoJson mapDapQuery so lon and lat are in query
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            mapDapQuery,
            dir,
            globecBottle.className() + "_DataGJ",
            ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "{\n"
            + "  \"type\": \"FeatureCollection\",\n"
            + "  \"propertyNames\": [\"NO3\", \"time\"],\n"
            + "  \"propertyUnits\": [\"micromoles L-1\", \"UTC\"],\n"
            + "  \"features\": [\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.4, 44.0] },\n"
            + "  \"properties\": {\n"
            + "    \"NO3\": 35.7,\n"
            + "    \"time\": \"2002-08-03T01:29:00Z\" }\n"
            + "},\n"
            + "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.4, 44.0] },\n"
            + "  \"properties\": {\n"
            + "    \"NO3\": 35.48,\n"
            + "    \"time\": \"2002-08-03T01:29:00Z\" }\n"
            + "},\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    expected =
        "{\"type\": \"Feature\",\n"
            + "  \"geometry\": {\n"
            + "    \"type\": \"Point\",\n"
            + "    \"coordinates\": [-124.1, 44.65] },\n"
            + "  \"properties\": {\n"
            + "    \"NO3\": 24.45,\n"
            + "    \"time\": \"2002-08-19T20:18:00Z\" }\n"
            + "}\n"
            + "  ],\n"
            + "  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n"
            + "}\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .geoJson just lon and lat in response
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03",
            dir,
            globecBottle.className() + "_DataGJLL",
            ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "{\n"
            + "  \"type\": \"MultiPoint\",\n"
            + "  \"coordinates\": [\n"
            + "[-124.4, 44.0],\n"
            + "[-124.4, 44.0],\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    expected =
        "[-124.1, 44.65]\n" + "  ],\n" + "  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n" + "}\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .geoJson with jsonp
    String jsonp = "myFunctionName";
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude&latitude>0&altitude>-5&time>=2002-08-03"
                + "&.jsonp="
                + SSR.percentEncode(jsonp),
            dir,
            globecBottle.className() + "_DataGJLL",
            ".geoJson");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        jsonp
            + "("
            + "{\n"
            + "  \"type\": \"MultiPoint\",\n"
            + "  \"coordinates\": [\n"
            + "[-124.4, 44.0],\n"
            + "[-124.4, 44.0],\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    expected =
        "[-124.1, 44.65]\n"
            + "  ],\n"
            + "  \"bbox\": [-126.0, 41.9, -124.1, 44.65]\n"
            + "}\n"
            + ")";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .htmlTable
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            globecBottle.className() + "_Data",
            ".htmlTable");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        EDStatic.startHeadHtml(
                language, EDStatic.erddapUrl((String) null, language), "EDDTableFromNcFiles_Data")
            + "\n"
            + "</head>\n"
            + EDStatic.startBodyHtml(language, null, "tabledap/testGlobecBottle.html", userDapQuery)
            + // 2022-11-22
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
            "&nbsp;<br>\n"
            +
            // HtmlWidgets.BACK_BUTTON +
            "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>longitude\n"
            + "<th>NO3\n"
            + "<th>time\n"
            + "<th>ship\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>degrees_east\n"
            + "<th>micromoles L-1\n"
            + "<th>UTC\n"
            + "<th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-124.4\n"
            + "<td class=\"R\">35.7\n"
            + "<td>2002-08-03T01:29:00Z\n"
            + "<td>New_Horizon\n"
            + "</tr>\n";
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);
    expected = // row with missing value has "&nbsp;" missing value
        "<tr>\n"
            + "<td class=\"R\">-124.1\n"
            + "<td class=\"R\">24.45\n"
            + "<td>2002-08-19T20:18:00Z\n"
            + "<td>New_Horizon\n"
            + "</tr>\n"
            + "</table>\n"
            + EDStatic.endBodyHtml(
                language, EDStatic.erddapUrl((String) null, language), (String) null)
            + "\n"
            + "</html>\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // .json
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".json");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n"
            + "    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n"
            + "    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n"
            + "    \"rows\": [\n"
            + "      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n"
            + "      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "      [-125, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"],\n"; // row with missing
    // value
    // has
    // "null". Before
    // 2018-05-17 was
    // -125.0.
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected =
        "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"; // last rows
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .json with jsonp query
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp),
            dir,
            globecBottle.className() + "_Data",
            ".json");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        jsonp
            + "("
            + "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"longitude\", \"NO3\", \"time\", \"ship\"],\n"
            + "    \"columnTypes\": [\"float\", \"float\", \"String\", \"String\"],\n"
            + "    \"columnUnits\": [\"degrees_east\", \"micromoles L-1\", \"UTC\", null],\n"
            + "    \"rows\": [\n"
            + "      [-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n"
            + "      [-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"],\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "      [-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + ")"; // last rows
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "\nresults=\n" + results);

    // .jsonlCSV1
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            globecBottle.className() + "_Data",
            ".jsonlCSV1");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "[\"longitude\", \"NO3\", \"time\", \"ship\"]\n"
            + "[-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n"
            + "[-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "[-125, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"]\n"; // row with missing value has
    // "null"
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "[-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .jsonlCSV
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            globecBottle.className() + "_Data",
            ".jsonlCSV");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "[-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n"
            + "[-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "[-125, null, \"2002-08-18T13:03:00Z\", \"New_Horizon\"]\n"; // row with missing value has
    // "null"
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "[-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .jsonlCSV with jsonp query
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp),
            dir,
            globecBottle.className() + "_Data",
            ".jsonlCSV");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        jsonp
            + "(\n"
            + "[-124.4, 35.7, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n"
            + "[-124.4, 35.48, \"2002-08-03T01:29:00Z\", \"New_Horizon\"]\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = "[-124.1, 24.45, \"2002-08-19T20:18:00Z\", \"New_Horizon\"]\n" + ")";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "\nresults=\n" + results);

    // .jsonlKVP
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            globecBottle.className() + "_Data",
            ".jsonlKVP");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "{\"longitude\":-124.4, \"NO3\":35.7, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n"
            + "{\"longitude\":-124.4, \"NO3\":35.48, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "{\"longitude\":-125, \"NO3\":null, \"time\":\"2002-08-18T13:03:00Z\", \"ship\":\"New_Horizon\"}\n"; // row
    // with
    // missing
    // value
    // has
    // "null"
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected =
        "{\"longitude\":-124.1, \"NO3\":24.45, \"time\":\"2002-08-19T20:18:00Z\", \"ship\":\"New_Horizon\"}\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .jsonlKVP with jsonp query
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery + "&.jsonp=" + SSR.percentEncode(jsonp),
            dir,
            globecBottle.className() + "_Data",
            ".jsonlKVP");
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        jsonp
            + "(\n"
            + "{\"longitude\":-124.4, \"NO3\":35.7, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n"
            + "{\"longitude\":-124.4, \"NO3\":35.48, \"time\":\"2002-08-03T01:29:00Z\", \"ship\":\"New_Horizon\"}\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected =
        "{\"longitude\":-124.1, \"NO3\":24.45, \"time\":\"2002-08-19T20:18:00Z\", \"ship\":\"New_Horizon\"}\n"
            + ")";
    Test.ensureEqual(
        results.substring(results.length() - expected.length()),
        expected,
        "\nresults=\n" + results);

    // .mat [I can't test that missing value is NaN.]
    // octave> load('c:/temp/tabledap/EDDTableFromNcFiles_Data.mat');
    // octave> testGlobecBottle
    // 2010-07-14 Roy can read this file in Matlab, previously. text didn't show up.
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, regexDapQuery, dir, globecBottle.className() + "_Data", ".mat");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.hexDump(dir + tName, 1000000);
    // String2.log(results);
    Test.ensureEqual(
        results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation
        // dateTime
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            +
            // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 4d 6f , Created on: Mo
            // |\n" +
            // "6e 20 44 65 63 20 38 20 31 32 3a 34 35 3a 34 36 n Dec 8 12:45:46
            // |\n" +
            // "20 32 30 30 38 20 20 20 20 20 20 20 20 20 20 20 2008 |\n" +
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + "00 00 00 0e 00 00 04 58   00 00 00 06 00 00 00 08          X         |\n"
            + "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 10                    |\n"
            + "74 65 73 74 47 6c 6f 62   65 63 42 6f 74 74 6c 65   testGlobecBottle |\n"
            + "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 80                    |\n"
            + "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "4e 4f 33 00 00 00 00 00   00 00 00 00 00 00 00 00   NO3              |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "73 68 69 70 00 00 00 00   00 00 00 00 00 00 00 00   ship             |\n"
            + "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + "00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n"
            + "00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 48   c2 fa 38 52 c2 fa 38 52          H  8R  8R |\n"
            + "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            + "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            + "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            + "c2 fa 38 52 c2 fa 38 52   c2 fa 38 52 c2 fa 38 52     8R  8R  8R  8R |\n"
            + "00 00 00 0e 00 00 00 78   00 00 00 06 00 00 00 08          x         |\n"
            + "00 00 00 07 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 07 00 00 00 48   42 07 a3 d7 41 d4 e1 48          HB   A  H |\n"
            + "41 2c cc cd 41 06 b8 52   40 ca e1 48 3f a5 1e b8   A,  A  R@  H?    |\n"
            + "3c a3 d7 0a 00 00 00 00   41 2c f5 c3 42 29 8f 5c   <       A,  B) \\ |\n"
            + "42 07 5c 29 41 dd 5c 29   41 7e e1 48 41 0b 0a 3d   B \\)A \\)A~ HA  = |\n"
            + "40 93 33 33 40 0a e1 48   41 09 c2 8f 3f 23 d7 0a   @ 33@  HA   ?#   |\n"
            + "00 00 00 0e 00 00 00 c0   00 00 00 06 00 00 00 08                    |\n"
            + "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + "00 00 00 12 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + "00 00 00 09 00 00 00 90   41 ce a9 a6 82 00 00 00           A        |\n"
            + "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            + "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            + "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            + "41 ce a9 a6 82 00 00 00   41 ce a9 a6 82 00 00 00   A       A        |\n"
            + "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            + "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            + "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            + "41 ce b0 19 36 00 00 00   41 ce b0 19 36 00 00 00   A   6   A   6    |\n"
            + "41 ce b0 19 36 00 00 00   00 00 00 0e 00 00 01 c0   A   6            |\n"
            + "00 00 00 06 00 00 00 08   00 00 00 04 00 00 00 00                    |\n"
            + "00 00 00 05 00 00 00 08   00 00 00 12 00 00 00 0b                    |\n"
            + "00 00 00 01 00 00 00 00   00 00 00 04 00 00 01 8c                    |\n"
            + "00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n"
            + "00 4e 00 4e 00 4e 00 4e   00 4e 00 4e 00 4e 00 4e    N N N N N N N N |\n"
            + "00 4e 00 4e 00 65 00 65   00 65 00 65 00 65 00 65    N N e e e e e e |\n"
            + "00 65 00 65 00 65 00 65   00 65 00 65 00 65 00 65    e e e e e e e e |\n"
            + "00 65 00 65 00 65 00 65   00 77 00 77 00 77 00 77    e e e e w w w w |\n"
            + "00 77 00 77 00 77 00 77   00 77 00 77 00 77 00 77    w w w w w w w w |\n"
            + "00 77 00 77 00 77 00 77   00 77 00 77 00 5f 00 5f    w w w w w w _ _ |\n"
            + "00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n"
            + "00 5f 00 5f 00 5f 00 5f   00 5f 00 5f 00 5f 00 5f    _ _ _ _ _ _ _ _ |\n"
            + "00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n"
            + "00 48 00 48 00 48 00 48   00 48 00 48 00 48 00 48    H H H H H H H H |\n"
            + "00 48 00 48 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    H H o o o o o o |\n"
            + "00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n"
            + "00 6f 00 6f 00 6f 00 6f   00 72 00 72 00 72 00 72    o o o o r r r r |\n"
            + "00 72 00 72 00 72 00 72   00 72 00 72 00 72 00 72    r r r r r r r r |\n"
            + "00 72 00 72 00 72 00 72   00 72 00 72 00 69 00 69    r r r r r r i i |\n"
            + "00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n"
            + "00 69 00 69 00 69 00 69   00 69 00 69 00 69 00 69    i i i i i i i i |\n"
            + "00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n"
            + "00 7a 00 7a 00 7a 00 7a   00 7a 00 7a 00 7a 00 7a    z z z z z z z z |\n"
            + "00 7a 00 7a 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    z z o o o o o o |\n"
            + "00 6f 00 6f 00 6f 00 6f   00 6f 00 6f 00 6f 00 6f    o o o o o o o o |\n"
            + "00 6f 00 6f 00 6f 00 6f   00 6e 00 6e 00 6e 00 6e    o o o o n n n n |\n"
            + "00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 6e 00 6e    n n n n n n n n |\n"
            + "00 6e 00 6e 00 6e 00 6e   00 6e 00 6e 00 00 00 00    n n n n n n     |\n",
        "\nresults=\n" + results);

    // .nc
    // !!! This is also a test of missing_value and _FillValue both active
    String tUserDapQuery =
        "longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15";
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, tUserDapQuery, dir, globecBottle.className() + "_Data", ".nc");
    results = NcHelper.ncdump(dir + tName, "");
    String tHeader1 =
        "netcdf EDDTableFromNcFiles_Data.nc {\n"
            + "  dimensions:\n"
            + "    row = 100;\n"
            + "    ship_strlen = 11;\n"
            + "  variables:\n"
            + "    float longitude(row=100);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :_FillValue = 327.67f; // float\n"
            + "      :actual_range = -125.67f, -124.8f; // float\n"
            + "      :axis = \"X\";\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :missing_value = 327.67f; // float\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    float NO3(row=100);\n"
            + "      :_FillValue = -99.0f; // float\n"
            + "      :actual_range = 0.46f, 34.09f; // float\n"
            + "      :colorBarMaximum = 50.0; // double\n"
            + "      :colorBarMinimum = 0.0; // double\n"
            + "      :ioos_category = \"Dissolved Nutrients\";\n"
            + "      :long_name = \"Nitrate\";\n"
            + "      :missing_value = -9999.0f; // float\n"
            + "      :standard_name = \"mole_concentration_of_nitrate_in_sea_water\";\n"
            + "      :units = \"micromoles L-1\";\n"
            + "\n"
            + "    double time(row=100);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = 1.02928674E9, 1.02936804E9; // double\n"
            + "      :axis = \"T\";\n"
            + "      :cf_role = \"profile_id\";\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    char ship(row=100, ship_strlen=11);\n"
            + "      :_Encoding = \"ISO-8859-1\";\n"
            + "      :ioos_category = \"Identifier\";\n"
            + "      :long_name = \"Ship\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :cdm_data_type = \"TrajectoryProfile\";\n"
            + "  :cdm_profile_variables = \"cast, longitude, latitude, time\";\n"
            + "  :cdm_trajectory_variables = \"cruise_id, ship\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :Easternmost_Easting = -124.8f; // float\n"
            + "  :featureType = \"TrajectoryProfile\";\n"
            + "  :geospatial_lat_units = \"degrees_north\";\n"
            + "  :geospatial_lon_max = -124.8f; // float\n"
            + "  :geospatial_lon_min = -125.67f; // float\n"
            + "  :geospatial_lon_units = \"degrees_east\";\n"
            + "  :geospatial_vertical_positive = \"up\";\n"
            + "  :geospatial_vertical_units = \"m\";\n"
            + "  :history = \""
            + today;
    tResults = results.substring(0, tHeader1.length());
    Test.ensureEqual(tResults, tHeader1, "\nresults=\n" + results);

    // + " https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
    // today + " " + EDStatic.erddapUrl + //in tests, always use non-https url
    String tHeader2 =
        "/tabledap/testGlobecBottle.nc?longitude,NO3,time,ship&latitude>0&altitude>-5&time>=2002-08-14&time<=2002-08-15\";\n"
            + "  :id = \"Globec_bottle_data_2002\";\n"
            + "  :infoUrl = \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n"
            + "  :institution = \"GLOBEC\";\n"
            + "  :keywords = \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :sourceUrl = \"(local files; contact erd.data@noaa.gov)\";\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :subsetVariables = \"cruise_id, ship, cast, longitude, latitude, time\";\n"
            + "  :summary = \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n"
            + "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n"
            + "Notes:\n"
            + "Physical data processed by Jane Fleischbein (OSU).\n"
            + "Chlorophyll readings done by Leah Feinberg (OSU).\n"
            + "Nutrient analysis done by Burke Hales (OSU).\n"
            + "Sal00 - salinity calculated from primary sensors (C0,T0).\n"
            + "Sal11 - salinity calculated from secondary sensors (C1,T1).\n"
            + "secondary sensor pair was used in final processing of CTD data for\n"
            + "most stations because the primary had more noise and spikes. The\n"
            + "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n"
            + "multiple spikes or offsets in the secondary pair.\n"
            + "Nutrient samples were collected from most bottles; all nutrient data\n"
            + "developed from samples frozen during the cruise and analyzed ashore;\n"
            + "data developed by Burke Hales (OSU).\n"
            + "Operation Detection Limits for Nutrient Concentrations\n"
            + "Nutrient  Range         Mean    Variable         Units\n"
            + "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n"
            + "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n"
            + "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n"
            + "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n"
            + "Dates and Times are UTC.\n"
            + "\n"
            + "For more information, see https://www.bco-dmo.org/dataset/2452\n"
            + "\n"
            + "Inquiries about how to access this data should be directed to\n"
            + "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n"
            + "  :time_coverage_end = \"2002-08-14T23:34:00Z\";\n"
            + "  :time_coverage_start = \"2002-08-14T00:59:00Z\";\n"
            + "  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n"
            + "  :Westernmost_Easting = -125.67f; // float\n";
    int tPo = results.indexOf(tHeader2.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, tPo + tHeader2.length()), tHeader2, "results=\n" + results);

    expected =
        " data:\n"
            + "    longitude = \n"
            + "      {-124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.8, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -124.9, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.0, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.43, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.66, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.67, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.5, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2, -125.2}\n"
            + "    NO3 = \n"
            + "      {33.66, 30.43, 28.22, 26.4, 25.63, 23.54, 22.38, 20.15, 33.55, 31.48, 24.93, -99.0, 21.21, 20.54, 17.87, -9999.0, 16.32, 33.61, 33.48, 30.7, 27.05, 25.13, 24.5, 23.95, 16.0, 14.42, 33.28, 28.3, 26.74, 24.96, 23.78, 20.76, 17.72, 16.01, 31.22, 27.47, 13.28, 10.66, 9.61, 8.36, 6.53, 2.86, 0.96, 34.05, 29.47, 18.87, 15.17, 13.84, 9.61, 4.95, 3.46, 34.09, 23.29, 16.01, 10.35, 7.72, 4.37, 2.97, 27.25, 29.98, 22.56, 9.82, 9.19, 6.57, 5.23, 3.81, 0.96, 30.08, 19.88, 8.44, 4.59, 2.67, 1.53, 0.94, 0.47, 30.73, 20.28, 10.61, 7.48, 6.53, 4.51, 3.04, 1.36, 0.89, 32.21, 23.75, 12.04, 7.67, 5.73, 1.14, 1.02, 0.46, 33.16, 27.33, 15.16, 9.7, 9.47, 8.66, 7.65, 4.84}\n"
            + "    time = \n"
            + "      {1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02928674E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02929106E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.02930306E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.029309E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02931668E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02932484E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02933234E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934002E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02934632E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02935214E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936018E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9, 1.02936804E9}\n"
            + "    ship =   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\",   \"New_Horizon\"\n"
            + "}\n";
    tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // .ncHeader
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            globecBottle.className() + "_Data",
            ".ncHeader");
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
        expected,
        "results=\n" + results);

    // .odvTxt
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            "&latitude>0&time>=2002-08-03",
            dir,
            globecBottle.className() + "_ODV",
            ".odvTxt");
    String2.log("ODV fileName=" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    results =
        results.replaceAll(
            "<CreateTime>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}",
            "<CreateTime>9999-99-99T99:99:99");
    expected =
        "//<Creator>https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics</Creator>\n"
            + "//<CreateTime>9999-99-99T99:99:99</CreateTime>\n"
            + "//<Encoding>UTF-8</Encoding>\n"
            + "//<Software>ERDDAP - Version "
            + EDStatic.erddapVersion
            + "</Software>\n"
            + "//<Source>http://localhost:"
            + PORT
            + "/erddap/tabledap/testGlobecBottle.html</Source>\n"
            + "//<Version>ODV Spreadsheet V4.6</Version>\n"
            + "//<DataField>GeneralField</DataField>\n"
            + "//<DataType>Profiles</DataType>\n"
            + "//<MetaVariable>label=\"Cruise\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" comment=\"Cruise ID\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"Station\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"Type\" value_type=\"TEXT:2\" is_primary_variable=\"F\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"yyyy-mm-ddThh:mm:ss.sss\" value_type=\"DOUBLE\" is_primary_variable=\"F\" comment=\"Time\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"Longitude [degrees_east]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Longitude\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"Latitude [degrees_north]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Latitude\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"ship\" value_type=\"INDEXED_TEXT\" is_primary_variable=\"F\" comment=\"Ship\" </MetaVariable>\n"
            + "//<MetaVariable>label=\"cast\" value_type=\"SHORT\" is_primary_variable=\"F\" comment=\"Cast Number\" </MetaVariable>\n"
            + "//<DataVariable>label=\"altitude [m]\" value_type=\"INTEGER\" is_primary_variable=\"F\" comment=\"Altitude\" </DataVariable>\n"
            + "//<DataVariable>label=\"bottle_posn\" value_type=\"SIGNED_BYTE\" is_primary_variable=\"F\" comment=\"Bottle Number\" </DataVariable>\n"
            + "//<DataVariable>label=\"chl_a_total [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Chlorophyll-a\" </DataVariable>\n"
            + "//<DataVariable>label=\"chl_a_10um [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Chlorophyll-a after passing 10um screen\" </DataVariable>\n"
            + "//<DataVariable>label=\"phaeo_total [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Total Phaeopigments\" </DataVariable>\n"
            + "//<DataVariable>label=\"phaeo_10um [ug L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Phaeopigments 10um\" </DataVariable>\n"
            + "//<DataVariable>label=\"sal00 [PSU]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Practical Salinity from T0 and C0 Sensors\" </DataVariable>\n"
            + "//<DataVariable>label=\"sal11 [PSU]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Practical Salinity from T1 and C1 Sensors\" </DataVariable>\n"
            + "//<DataVariable>label=\"temperature0 [degree_C]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Sea Water Temperature from T0 Sensor\" </DataVariable>\n"
            + "//<DataVariable>label=\"temperature1 [degree_C]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Sea Water Temperature from T1 Sensor\" </DataVariable>\n"
            + "//<DataVariable>label=\"fluor_v [volts]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Fluorescence Voltage\" </DataVariable>\n"
            + "//<DataVariable>label=\"xmiss_v [volts]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Transmissivity Voltage\" </DataVariable>\n"
            + "//<DataVariable>label=\"PO4 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Phosphate\" </DataVariable>\n"
            + "//<DataVariable>label=\"N_N [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Nitrate plus Nitrite\" </DataVariable>\n"
            + "//<DataVariable>label=\"NO3 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Nitrate\" </DataVariable>\n"
            + "//<DataVariable>label=\"Si [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Silicate\" </DataVariable>\n"
            + "//<DataVariable>label=\"NO2 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Nitrite\" </DataVariable>\n"
            + "//<DataVariable>label=\"NH4 [micromoles L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Ammonium\" </DataVariable>\n"
            + "//<DataVariable>label=\"oxygen [mL L-1]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Oxygen\" </DataVariable>\n"
            + "//<DataVariable>label=\"par [volts]\" value_type=\"FLOAT\" is_primary_variable=\"F\" comment=\"Photosynthetically Active Radiation\" </DataVariable>\n"
            + "Cruise\tStation\tType\tyyyy-mm-ddThh:mm:ss.sss\tLongitude [degrees_east]\tLatitude [degrees_north]\tship\tcast\taltitude [m]\tbottle_posn\tchl_a_total [ug L-1]\tchl_a_10um [ug L-1]\tphaeo_total [ug L-1]\tphaeo_10um [ug L-1]\tsal00 [PSU]\tsal11 [PSU]\ttemperature0 [degree_C]\ttemperature1 [degree_C]\tfluor_v [volts]\txmiss_v [volts]\tPO4 [micromoles L-1]\tN_N [micromoles L-1]\tNO3 [micromoles L-1]\tSi [micromoles L-1]\tNO2 [micromoles L-1]\tNH4 [micromoles L-1]\toxygen [mL L-1]\tpar [volts]\n"
            + "nh0207\t\t*\t2002-08-03T01:29:00.000Z\t-124.4\t44.0\tNew_Horizon\t20\t0\t1\t\t\t\t\t33.9939\t33.9908\t7.085\t7.085\t0.256\t0.518\t2.794\t35.8\t35.7\t71.11\t0.093\t0.037\t\t0.1545\n"
            + "nh0207\t\t*\t2002-08-03T01:29:00.000Z\t-124.4\t44.0\tNew_Horizon\t20\t0\t2\t\t\t\t\t33.8154\t33.8111\t7.528\t7.53\t0.551\t0.518\t2.726\t35.87\t35.48\t57.59\t0.385\t0.018\t\t0.1767\n"
            + "nh0207\t\t*\t2002-08-03T01:29:00.000Z\t-124.4\t44.0\tNew_Horizon\t20\t0\t3\t1.463\t\t1.074\t\t33.5858\t33.5834\t7.572\t7.573\t0.533\t0.518\t2.483\t31.92\t31.61\t48.54\t0.307\t0.504\t\t0.3875\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

    // .tsv
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".tsv");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude\tNO3\ttime\tship\n"
            + "degrees_east\tmicromoles L-1\tUTC\t\n"
            + "-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected =
        "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; // row with missing value has "NaN"
    // missing value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .tsvp
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".tsvp");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude (degrees_east)\tNO3 (micromoles L-1)\ttime (UTC)\tship\n"
            + "-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected =
        "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; // row with missing value has "NaN"
    // missing value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .tsv0
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".tsv0");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected = "-124.4\t35.7\t2002-08-03T01:29:00Z\tNew_Horizon\n";
    Test.ensureTrue(results.indexOf(expected) == 0, "\nresults=\n" + results);
    expected =
        "-124.8\tNaN\t2002-08-03T07:17:00Z\tNew_Horizon\n"; // row with missing value has "NaN"
    // missing value
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    expected = "-124.1\t24.45\t2002-08-19T20:18:00Z\tNew_Horizon\n"; // last row
    Test.ensureTrue(results.endsWith(expected), "\nresults=\n" + results);

    // .xhtml
    tName =
        globecBottle.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, globecBottle.className() + "_Data", ".xhtml");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
            + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
            + "<head>\n"
            + "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"
            + "  <title>EDDTableFromNcFiles_Data</title>\n"
            + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:"
            + PORT
            + "/erddap/images/erddap2.css\" />\n"
            + "</head>\n"
            + "<body>\n"
            + "\n"
            + "&nbsp;\n"
            + "<table class=\"erd commonBGColor nowrap\">\n"
            + "<tr>\n"
            + "<th>longitude</th>\n"
            + "<th>NO3</th>\n"
            + "<th>time</th>\n"
            + "<th>ship</th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<th>degrees_east</th>\n"
            + "<th>micromoles L-1</th>\n"
            + "<th>UTC</th>\n"
            + "<th></th>\n"
            + "</tr>\n"
            + "<tr>\n"
            + "<td class=\"R\">-124.4</td>\n"
            + "<td class=\"R\">35.7</td>\n"
            + "<td>2002-08-03T01:29:00Z</td>\n"
            + "<td>New_Horizon</td>\n"
            + "</tr>\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    expected = // row with missing value has "" missing value
        "<tr>\n"
            + "<td class=\"R\">-124.1</td>\n"
            + "<td class=\"R\">24.45</td>\n"
            + "<td>2002-08-19T20:18:00Z</td>\n"
            + "<td>New_Horizon</td>\n"
            + "</tr>\n"
            + "</table>\n"
            + "</body>\n"
            + "</html>\n";
    tResults = results.substring(results.length() - expected.length());
    Test.ensureEqual(tResults, expected, "\ntResults=\n" + tResults);

    // data for mapExample
    tName =
        globecBottle.makeNewFileForDapQuery(
            language,
            null,
            null,
            "longitude,latitude&time>=2002-08-03&time<=2002-08-04",
            dir,
            globecBottle.className() + "Map",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "longitude,latitude\n"
            + "degrees_east,degrees_north\n"
            + "-124.4,44.0\n"
            + "-124.6,44.0\n"
            + "-124.8,44.0\n"
            + "-125.0,44.0\n"
            + "-125.2,44.0\n"
            + "-125.4,44.0\n"
            + "-125.6,43.8\n"
            + "-125.86,43.5\n"
            + "-125.63,43.5\n"
            + "-125.33,43.5\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    try {

      // test treat itself as a dataset
      EDDTable eddTable2 =
          new EDDTableFromDapSequence(
              "erddapGlobecBottle", // String tDatasetID,
              null,
              null,
              null,
              null,
              null,
              null,
              "",
              "",
              null,
              null,
              new Object[][] { // dataVariables: sourceName, addAttributes
                {"longitude", null, null},
                {"latitude", null, null},
                {"altitude", null, null},
                {"time", null, null},
                {"ship", null, null},
                {"cruise_id", null, null},
                {"cast", null, null},
                {"bottle_posn", null, null},
                {"chl_a_total", null, null},
                {"chl_a_10um", null, null},
                {"phaeo_total", null, null},
                {"phaeo_10um", null, null},
                {"sal00", null, null},
                {"sal11", null, null},
                {"temperature0", null, null},
                {"temperature1", null, null},
                {"fluor_v", null, null},
                {"xmiss_v", null, null},
                {"PO4", null, null},
                {"N_N", null, null},
                {"NO3", null, null},
                {"Si", null, null},
                {"NO2", null, null},
                {"NH4", null, null},
                {"oxygen", null, null},
                {"par", null, null}
              },
              60, // int tReloadEveryNMinutes,
              EDStatic.erddapUrl
                  + // in tests, always use non-https url
                  "/tabledap/testGlobecBottle", // sourceUrl);
              "s",
              null, // outerSequenceName innerSequenceName
              true, // NeedsExpandedFP_EQ
              true, // sourceCanConstrainStringEQNE
              true, // sourceCanConstrainStringGTLT
              PrimitiveArray.REGEX_OP,
              false);

      // .xhtml from local dataset made from Erddap
      tName =
          eddTable2.makeNewFileForDapQuery(
              language, null, null, userDapQuery, dir, eddTable2.className() + "_Itself", ".xhtml");
      // Test.displayInBrowser("file://" + dir + tName);
      results = File2.directReadFromUtf8File(dir + tName);
      // String2.log(results);
      expected =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
              + "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
              + "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
              + "<head>\n"
              + "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"
              + "  <title>EDDTableFromDapSequence_Itself</title>\n"
              + "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:"
              + PORT
              + "/erddap/images/erddap2.css\" />\n"
              + "</head>\n"
              + "<body>\n"
              + "\n"
              + "&nbsp;\n"
              + "<table class=\"erd commonBGColor nowrap\">\n"
              + "<tr>\n"
              + "<th>longitude</th>\n"
              + "<th>NO3</th>\n"
              + "<th>time</th>\n"
              + "<th>ship</th>\n"
              + "</tr>\n"
              + "<tr>\n"
              + "<th>degrees_east</th>\n"
              + "<th>micromoles L-1</th>\n"
              + "<th>UTC</th>\n"
              + "<th></th>\n"
              + "</tr>\n"
              + "<tr>\n"
              + "<td class=\"R\">-124.4</td>\n"
              + "<td class=\"R\">35.7</td>\n"
              + "<td>2002-08-03T01:29:00Z</td>\n"
              + "<td>New_Horizon</td>\n"
              + "</tr>\n";
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
    EDDTable globecBottle =
        (EDDTableFromNcFiles) EDDTestDataset.gettestGlobecBottle(); // should work
    String tUrl =
        EDStatic.erddapUrl
            + // in tests, always use non-https url
            "/tabledap/"
            + globecBottle.datasetID();
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
        expected =
            "netcdf testGlobecBottle {\n"
                + "  variables:\n"
                + "\n"
                + // 2009-02-26 this line was added with switch to netcdf-java 4.0
                "    Structure {\n"
                + "      String cruise_id;\n"
                + "        :cf_role = \"trajectory_id\";\n"
                + "        :ioos_category = \"Identifier\";\n"
                + "        :long_name = \"Cruise ID\";\n"
                + "      String ship;\n"
                + "        :ioos_category = \"Identifier\";\n"
                + "        :long_name = \"Ship\";\n"
                + "      short cast;\n"
                + "        :_FillValue = 32767S; // short\n"
                + "        :actual_range = 1S, 127S; // short\n"
                + "        :colorBarMaximum = 140.0; // double\n"
                + "        :colorBarMinimum = 0.0; // double\n"
                + "        :ioos_category = \"Identifier\";\n"
                + "        :long_name = \"Cast Number\";\n"
                + "        :missing_value = 32767S; // short\n"
                + "      float longitude;\n"
                + "        :_CoordinateAxisType = \"Lon\";\n"
                + "        :_FillValue = 327.67f; // float\n"
                + "        :actual_range = -126.2f, -124.1f; // float\n"
                + "        :axis = \"X\";\n"
                + "        :ioos_category = \"Location\";\n"
                + "        :long_name = \"Longitude\";\n"
                + "        :missing_value = 327.67f; // float\n"
                + "        :standard_name = \"longitude\";\n"
                + "        :units = \"degrees_east\";\n"
                + "      float latitude;\n"
                + "        :_CoordinateAxisType = \"Lat\";\n"
                + "        :_FillValue = 327.67f; // float\n"
                + "        :actual_range = 41.9f, 44.65f; // float\n"
                + "        :axis = \"Y\";\n"
                + "        :ioos_category = \"Location\";\n"
                + "        :long_name = \"Latitude\";\n"
                + "        :missing_value = 327.67f; // float\n"
                + "        :standard_name = \"latitude\";\n"
                + "        :units = \"degrees_north\";\n"
                + "      int altitude;\n"
                + "        :_CoordinateAxisType = \"Height\";\n"
                + "        :_CoordinateZisPositive = \"up\";\n"
                + "        :actual_range = 0, 0; // int\n"
                + "        :axis = \"Z\";\n"
                + "        :ioos_category = \"Location\";\n"
                + "        :long_name = \"Altitude\";\n"
                + "        :positive = \"up\";\n"
                + "        :standard_name = \"altitude\";\n"
                + "        :units = \"m\";\n"
                + "      double time;\n"
                + "        :_CoordinateAxisType = \"Time\";\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

        expected =
            "  :time_coverage_start = \"2002-05-30T03:21:00Z\";\n"
                + "  :title = \"GLOBEC NEP Rosette Bottle Data (2002)\";\n"
                + "  :Westernmost_Easting = -126.2; // double\n"
                + "}\n";
        Test.ensureEqual(
            results.substring(results.indexOf("  :time_coverage_start")),
            expected,
            "RESULTS=\n" + results);

        Attributes attributes = new Attributes();
        NcHelper.getGroupAttributes(nc.getRootGroup(), attributes);
        Test.ensureEqual(
            attributes.getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");

        // get attributes for a dimension
        Variable ncLat = nc.findVariable("s.latitude");
        attributes.clear();
        NcHelper.getVariableAttributes(ncLat, attributes);
        Test.ensureEqual(attributes.getString("units"), EDV.LAT_UNITS, "");

        // get attributes for grid variable
        Variable ncChl = nc.findVariable("s.chl_a_total");
        attributes.clear();
        NcHelper.getVariableAttributes(ncChl, attributes);
        Test.ensureEqual(
            attributes.getString("standard_name"), "concentration_of_chlorophyll_in_sea_water", "");

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
          Test.ensureEqual(
              results.substring(0, expected.length()), expected, "RESULTS=\n" + results);
        }

      } catch (Throwable t) {
        throw new Exception(
            MustBe.throwableToString(t)
                + "\nError accessing "
                + EDStatic.erddapUrl
                + // in tests, always use
                // non-https url
                " via netcdf-java.");
      }
    }
  }

  /** This tests makeCopyFileTasks. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testMakeCopyFileTasks() throws Exception {

    // String2.log("\n*** testMakeCopyFileTasks\n" +
    // "This requires fedCalLandings in localhost ERDDAP.");

    int language = 0;

    boolean testMode = false;
    boolean tRecursive = true;
    boolean tDirectoriesToo = false;
    String tDatasetID = "myDatasetID";
    String tSourceUrl = "http://localhost:" + PORT + "/erddap/files/fedCalLandings/";
    String tPathRegex = ".*/(3|4)/.*";
    String tFileNameRegex = "19\\d7\\.nc"; // e.g. 1937, 1947
    String tLocalDir =
        Path.of(JettyTests.class.getResource("/data/points/testEDDTableCopyFiles2/").toURI())
            .toString()
            .replaceAll("\\\\", "/");
    String results, expected;

    // delete all local files
    File2.deleteAllFiles(tLocalDir, true, true);

    // what does oneStep see in source?
    String2.log("What does one step see in source?");

    results =
        FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                tSourceUrl, tFileNameRegex, tRecursive, tPathRegex, tDirectoriesToo)
            .dataToString();
    results = results.replaceAll(",.............,", ",lastMod,");
    expected =
        "directory,name,lastModified,size\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1937.nc,lastMod,24672\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/3/,1947.nc,lastMod,11576\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1937.nc,lastMod,373192\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1947.nc,lastMod,190272\n"
            + "http://localhost:"
            + PORT
            + "/erddap/files/fedCalLandings/4/,1977.nc,lastMod,229724\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // what does oneStep see locally?
    String2.log("What does one step see in tLocalDir=" + tLocalDir + " ?");
    FileVisitorDNLS.verbose = true;
    FileVisitorDNLS.reallyVerbose = true;
    results =
        FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                tLocalDir, tFileNameRegex, tRecursive, tPathRegex, tDirectoriesToo)
            .dataToString();
    expected = "directory,name,lastModified,size\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // download the files
    Test.ensureEqual(
        EDStatic.makeCopyFileTasks(
            "EDDTableFromNcFiles",
            EDStatic.DefaultMaxMakeCopyFileTasks,
            tDatasetID,
            tSourceUrl,
            tFileNameRegex,
            tRecursive,
            tPathRegex,
            tLocalDir),
        5,
        "nFilesToDownload");
    Math2.sleep(10000);
    // String2.pressEnterToContinue("Hopefully the first download tasks finished.");

    // Use ".*" for the path regex to make the test compatible with windows systems.
    // The issue is that Windows sometimes uses / and sometimes uses \ as a path separator.
    // There should only be files in the restricted directories because the entire dir was
    // deletex above, and in fact testing this way actually verifies that.
    results =
        FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                tLocalDir, tFileNameRegex, tRecursive, ".*", false)
            .dataToString();
    results = results.replaceAll(",.............,", ",lastMod,");
    expected =
        "directory,name,lastModified,size\n"
            + tLocalDir
            + "/3/,1937.nc,lastMod,24672\n"
            + tLocalDir
            + "/3/,1947.nc,lastMod,11576\n"
            + tLocalDir
            + "/4/,1937.nc,lastMod,373192\n"
            + tLocalDir
            + "/4/,1947.nc,lastMod,190272\n"
            + tLocalDir
            + "/4/,1977.nc,lastMod,229724\n";
    Test.ensureEqual(results, expected, "tPathRegex: " + tPathRegex + "\nresults=\n" + results);

    // delete a file
    File2.delete(tLocalDir + "/3/1947.nc");
    // change time of file
    File2.setLastModified(tLocalDir + "/4/1947.nc", 0);

    // refresh the local copy
    Test.ensureEqual(
        EDStatic.makeCopyFileTasks(
            "EDDTableFromNcFiles",
            EDStatic.DefaultMaxMakeCopyFileTasks,
            tDatasetID,
            tSourceUrl,
            tFileNameRegex,
            tRecursive,
            tPathRegex,
            tLocalDir),
        2,
        "nFilesToDownload");
    Math2.sleep(3000);
    // String2.pressEnterToContinue("Hopefully the download tasks finished.");

    results =
        FileVisitorDNLS.oneStep( // throws IOException if "Too many open files"
                tLocalDir, tFileNameRegex, tRecursive, tPathRegex, false)
            .dataToString();
    // expected = same
    results = results.replaceAll(",.............,", ",lastMod,");
    Test.ensureEqual(results, expected, "results=\n" + results);

    String2.log("\n*** testMakeCopyFileTasks finished successfully");
  }

  /** This tests generateDatasetsXml. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testCopyFilesGenerateDatasetsXml() throws Throwable {

    // String2.log("\n*** EDDTableFromNcFiles.testCopyFilesGenerateDatasetsXml");
    int language = 0;
    String dataDir =
        File2.addSlash(
            Path.of(JettyTests.class.getResource("/data/points/testEDDTableCopyFiles3/").toURI())
                .toString());
    String fileNameRegex = "1937\\.nc";
    File2.deleteAllFiles(dataDir, true, true);
    // FileVisitorDNLS.verbose = true;
    // FileVisitorDNLS.reallyVerbose = true;
    // FileVisitorDNLS.debugMode = true;

    try {
      // based on fedCalLandings
      String results =
          EDDTableFromNcFiles.generateDatasetsXml(
                  dataDir,
                  fileNameRegex,
                  "",
                  "",
                  -1,
                  "",
                  "",
                  "",
                  "",
                  "",
                  "region year",
                  "",
                  "",
                  "",
                  "",
                  -1, // defaultStandardizeWhat
                  "http://localhost:" + PORT + "/erddap/files/fedCalLandings/", // cacheFromUrl
                  null)
              + "\n";

      // GenerateDatasetsXml
      String gdxResults =
          new GenerateDatasetsXml()
              .doIt(
                  new String[] {
                    "-verbose",
                    "EDDTableFromNcFiles",
                    dataDir,
                    fileNameRegex,
                    "",
                    "",
                    "-1",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "region year",
                    "",
                    "",
                    "",
                    "",
                    "-1", // defaultStandardizeWhat
                    "http://localhost:" + PORT + "/erddap/files/fedCalLandings/"
                  }, // cacheFromUrl
                  false); // doIt loop?
      Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

      String tDatasetID = EDDTableFromNcFiles.suggestDatasetID(dataDir + fileNameRegex);
      String expected =
          "<dataset type=\"EDDTableFromNcFiles\" datasetID=\""
              + tDatasetID
              + "\" active=\"true\">\n"
              + "    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n"
              + "    <cacheFromUrl>http://localhost:"
              + PORT
              + "/erddap/files/fedCalLandings/</cacheFromUrl>\n"
              + "    <fileDir>"
              + dataDir
              + "</fileDir>\n"
              + "    <fileNameRegex>"
              + fileNameRegex
              + "</fileNameRegex>\n"
              + "    <recursive>true</recursive>\n"
              + "    <pathRegex>.*</pathRegex>\n"
              + "    <metadataFrom>last</metadataFrom>\n"
              + "    <standardizeWhat>0</standardizeWhat>\n"
              + "    <sortedColumnSourceName>time</sortedColumnSourceName>\n"
              + "    <sortFilesBySourceNames>region year</sortFilesBySourceNames>\n"
              + "    <fileTableInMemory>false</fileTableInMemory>\n"
              + "    <!-- sourceAttributes>\n"
              + "        <att name=\"id\">1937</att>\n"
              + "        <att name=\"observationDimension\">row</att>\n"
              + "    </sourceAttributes -->\n"
              + "    <!-- Please specify the actual cdm_data_type (TimeSeries?) and related info below, for example...\n"
              + "        <att name=\"cdm_timeseries_variables\">station_id, longitude, latitude</att>\n"
              + "        <att name=\"subsetVariables\">station_id, longitude, latitude</att>\n"
              + "    -->\n"
              + "    <addAttributes>\n"
              + "        <att name=\"cdm_data_type\">Other</att>\n"
              + "        <att name=\"Conventions\">COARDS, CF-1.10, ACDD-1.3</att>\n"
              + "        <att name=\"infoUrl\">???</att>\n"
              + "        <att name=\"institution\">???</att>\n"
              + "        <att name=\"keywords\">area, block, category, caught, comments, data, description, group, imported, local, market, market_category, month, nominal, nominal_species, pounds, region, region_caught, row, source, species, species_group, taxonomy, time, year</att>\n"
              + "        <att name=\"license\">[standard]</att>\n"
              + "        <att name=\"sourceUrl\">(local files)</att>\n"
              + "        <att name=\"standard_name_vocabulary\">CF Standard Name Table v70</att>\n"
              + "        <att name=\"subsetVariables\">region, year, imported</att>\n"
              + "        <att name=\"summary\">Data from a local source.</att>\n"
              + "        <att name=\"title\">Data from a local source.</att>\n"
              + "    </addAttributes>\n"
              + "    <dataVariable>\n"
              + //
              "        <sourceName>row</sourceName>\n"
              + //
              "        <destinationName>row</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"_FillValue\" type=\"short\">32767</att>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Row</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>region</sourceName>\n"
              + //
              "        <destinationName>region</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Location</att>\n"
              + //
              "            <att name=\"long_name\">Region</att>\n"
              + //
              "            <att name=\"missing_value\" type=\"short\">-9999</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>year</sourceName>\n"
              + //
              "        <destinationName>year</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Time</att>\n"
              + //
              "            <att name=\"long_name\">Year</att>\n"
              + //
              "            <att name=\"missing_value\" type=\"short\">-9999</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>market_category</sourceName>\n"
              + //
              "        <destinationName>market_category</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Market Category</att>\n"
              + //
              "            <att name=\"missing_value\" type=\"short\">-9999</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>month</sourceName>\n"
              + //
              "        <destinationName>month</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Time</att>\n"
              + //
              "            <att name=\"long_name\">Month</att>\n"
              + //
              "            <att name=\"missing_value\" type=\"short\">-9999</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>block</sourceName>\n"
              + //
              "        <destinationName>block</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Block</att>\n"
              + //
              "            <att name=\"missing_value\" type=\"short\">-9999</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>pounds</sourceName>\n"
              + //
              "        <destinationName>pounds</destinationName>\n"
              + //
              "        <dataType>int</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "            <att name=\"units\">pounds</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Pounds</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>area</sourceName>\n"
              + //
              "        <destinationName>area</destinationName>\n"
              + //
              "        <dataType>String</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Area</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>imported</sourceName>\n"
              + //
              "        <destinationName>imported</destinationName>\n"
              + //
              "        <dataType>String</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Imported</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>region_caught</sourceName>\n"
              + //
              "        <destinationName>region_caught</destinationName>\n"
              + //
              "        <dataType>short</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"int\">-9999</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Location</att>\n"
              + //
              "            <att name=\"long_name\">Region Caught</att>\n"
              + //
              "            <att name=\"missing_value\" type=\"short\">-9999</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>time</sourceName>\n"
              + //
              "        <destinationName>time</destinationName>\n"
              + //
              "        <dataType>double</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "            <att name=\"missing_value\" type=\"double\">-1.0E30</att>\n"
              + //
              "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Time</att>\n"
              + //
              "            <att name=\"long_name\">Time</att>\n"
              + //
              "            <att name=\"standard_name\">time</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>description</sourceName>\n"
              + //
              "        <destinationName>description</destinationName>\n"
              + //
              "        <dataType>String</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Description</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>nominal_species</sourceName>\n"
              + //
              "        <destinationName>nominal_species</destinationName>\n"
              + //
              "        <dataType>String</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Taxonomy</att>\n"
              + //
              "            <att name=\"long_name\">Nominal Species</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>species_group</sourceName>\n"
              + //
              "        <destinationName>species_group</destinationName>\n"
              + //
              "        <dataType>String</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Taxonomy</att>\n"
              + //
              "            <att name=\"long_name\">Species Group</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "    <dataVariable>\n"
              + //
              "        <sourceName>comments</sourceName>\n"
              + //
              "        <destinationName>comments</destinationName>\n"
              + //
              "        <dataType>String</dataType>\n"
              + //
              "        <!-- sourceAttributes>\n"
              + //
              "        </sourceAttributes -->\n"
              + //
              "        <addAttributes>\n"
              + //
              "            <att name=\"ioos_category\">Unknown</att>\n"
              + //
              "            <att name=\"long_name\">Comments</att>\n"
              + //
              "        </addAttributes>\n"
              + //
              "    </dataVariable>\n"
              + //
              "</dataset>\n"
              + "\n\n";

      Test.ensureEqual(results, expected, "results=\n" + results);

      EDD edd = EDDTableFromNcFiles.oneFromXmlFragment(null, results);

      // String tDatasetID = "testEDDTableCopyFiles_e52a_9290_6c34";
      EDD.deleteCachedDatasetInfo(tDatasetID);
      try {
        edd = EDDTableFromNcFiles.oneFromXmlFragment(null, results);
      } catch (Exception e) {
        // Nothing wront here, it's common for this to thow an error and then kick off the file
        // copies.
      }
      Math2.sleep(5000);
      // String2.pressEnterToContinue(
      // "\n*** When the tasks are finished, press Enter.");
      edd = EDDTableFromNcFiles.oneFromXmlFragment(null, results);

      Test.ensureEqual(edd.datasetID(), tDatasetID, "");
      Test.ensureEqual(edd.title(), "Data from a local source.", "");
      Test.ensureEqual(
          String2.toCSSVString(edd.dataVariableDestinationNames()),
          "row, region, year, market_category, month, block, pounds, area, imported, region_caught, time, description, nominal_species, species_group, comments",
          "");
      // String2.pressEnterToContinue(
      // "\n*** Dataset should have reloaded with no tasks. It passed the tests.");

    } catch (Throwable t) {
      throw new Exception(MustBe.throwableToString(t) + "\nError using generateDatasetsXml.");
    }
    // FileVisitorDNLS.verbose = true;
    // FileVisitorDNLS.reallyVerbose = true;
    // FileVisitorDNLS.debugMode = false;
  }

  /**
   * This tests the methods in this class.
   *
   * @throws Throwable if trouble
   */
  @TagJetty
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void testCopyFilesBasic(boolean deleteDataFiles) throws Throwable {

    String2.log("\n*** testCopyFilesBasic(" + deleteDataFiles + ")\n");
    int language = 0;
    // testVerboseOn();
    // FileVisitorDNLS.verbose = true;
    // FileVisitorDNLS.reallyVerbose = true;
    // FileVisitorDNLS.debugMode = true;

    String name, tName, results, tResults, expected, userDapQuery, tQuery;
    String error = "";
    int po;
    EDV edv;

    String today =
        Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); // 14 is enough to check
    // hour. Hard
    // to check min:sec.
    String tDir = TEMP_DIR.toAbsolutePath().toString() + "/";
    String id = "testEDDTableCopyFiles";
    if (deleteDataFiles) {
      File2.deleteAllFiles(
          JettyTests.class.getResource("/data/points/testEDDTableCopyFiles").getPath());
      EDDTableFromNcFiles.deleteCachedDatasetInfo(id);
    }

    EDDTable eddTable;
    eddTable = (EDDTable) EDDTestDataset.gettestEDDTableCopyFiles();

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDTableCopyFiles  das and dds for entire dataset\n");
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", tDir, eddTable.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + //
            " s {\n"
            + //
            "  row {\n"
            + //
            "    String _Unsigned \"false\";\n"
            + //
            "    Byte actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Row\";\n"
            + //
            "  }\n"
            + //
            "  region {\n"
            + //
            "    Int16 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Location\";\n"
            + //
            "    String long_name \"Region\";\n"
            + //
            "    Int16 missing_value -9999;\n"
            + //
            "  }\n"
            + //
            "  year {\n"
            + //
            "    Int16 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Time\";\n"
            + //
            "    String long_name \"Year\";\n"
            + //
            "    Int16 missing_value -9999;\n"
            + //
            "  }\n"
            + //
            "  market_category {\n"
            + //
            "    Int16 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Market Category\";\n"
            + //
            "    Int16 missing_value -9999;\n"
            + //
            "  }\n"
            + //
            "  month {\n"
            + //
            "    Int16 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Time\";\n"
            + //
            "    String long_name \"Month\";\n"
            + //
            "    Int16 missing_value -9999;\n"
            + //
            "  }\n"
            + //
            "  block {\n"
            + //
            "    Int16 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Block\";\n"
            + //
            "    Int16 missing_value -9999;\n"
            + //
            "  }\n"
            + //
            "  pounds {\n"
            + //
            "    Int32 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Pounds\";\n"
            + //
            "    Int32 missing_value -9999;\n"
            + //
            "    String units \"pounds\";\n"
            + //
            "  }\n"
            + //
            "  area {\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Area\";\n"
            + //
            "  }\n"
            + //
            "  imported {\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Imported\";\n"
            + //
            "  }\n"
            + //
            "  region_caught {\n"
            + //
            "    Int16 actual_range MIN, MAX;\n"
            + //
            "    String ioos_category \"Location\";\n"
            + //
            "    String long_name \"Region Caught\";\n"
            + //
            "    Int16 missing_value -9999;\n"
            + //
            "  }\n"
            + //
            "  time {\n"
            + //
            "    String _CoordinateAxisType \"Time\";\n"
            + //
            "    Float64 actual_range 1.90512e+8, 2.19456e+8;\n"
            + //
            "    String axis \"T\";\n"
            + //
            "    String ioos_category \"Time\";\n"
            + //
            "    String long_name \"Time\";\n"
            + //
            "    String standard_name \"time\";\n"
            + //
            "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + //
            "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + //
            "  }\n"
            + //
            "  description {\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Description\";\n"
            + //
            "  }\n"
            + //
            "  nominal_species {\n"
            + //
            "    String ioos_category \"Taxonomy\";\n"
            + //
            "    String long_name \"Nominal Species\";\n"
            + //
            "  }\n"
            + //
            "  species_group {\n"
            + //
            "    String ioos_category \"Taxonomy\";\n"
            + //
            "    String long_name \"Species Group\";\n"
            + //
            "  }\n"
            + //
            "  comments {\n"
            + //
            "    String ioos_category \"Unknown\";\n"
            + //
            "    String long_name \"Comments\";\n"
            + //
            "  }\n"
            + //
            " }\n"
            + //
            "  NC_GLOBAL {\n"
            + //
            "    String cdm_data_type \"Other\";\n"
            + //
            "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + //
            "    String history";
    results = results.replaceAll("Byte actual_range [0-9]+, [0-9]+", "Byte actual_range MIN, MAX");
    results =
        results.replaceAll("Int16 actual_range [0-9]+, [0-9]+", "Int16 actual_range MIN, MAX");
    results =
        results.replaceAll("Int32 actual_range [0-9]+, [0-9]+", "Int32 actual_range MIN, MAX");
    tResults = results.substring(0, Math.min(results.length(), expected.length()));
    Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

    // + " https://data.nodc.noaa.gov/thredds/catalog/nmsp/wcos/catalog.xml\n" +
    // today + " http://127.0.0.1:8080/cwexperimental/tabledap/
    expected =
        "String id \"1977\";\n"
            + //
            "    String infoUrl \"???\";\n"
            + //
            "    String institution \"???\";\n"
            + //
            "    String keywords \"area, block, category, caught, comments, data, description, group, imported, local, market, market_category, month, nominal, nominal_species, pounds, region, region_caught, row, source, species, species_group, taxonomy, time, year\";\n"
            + //
            "    String license \"The data may be used and redistributed for free but is not intended\n"
            + //
            "for legal use, since it may contain inaccuracies. Neither the data\n"
            + //
            "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + //
            "of their employees or contractors, makes any warranty, express or\n"
            + //
            "implied, including warranties of merchantability and fitness for a\n"
            + //
            "particular purpose, or assumes any legal liability for the accuracy,\n"
            + //
            "completeness, or usefulness, of this information.\";\n"
            + //
            "    String observationDimension \"row\";\n"
            + //
            "    String sourceUrl \"(local files)\";\n"
            + //
            "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + //
            "    String subsetVariables \"region, year, area, imported, region_caught, comments\";\n"
            + //
            "    String summary \"Data from a local source.\";\n"
            + //
            "    String time_coverage_end \"1976-12-15T00:00:00Z\";\n"
            + //
            "    String time_coverage_start \"1976-01-15T00:00:00Z\";\n"
            + //
            "    String title \"Data from a local source.\";\n"
            + //
            "  }\n"
            + //
            "}";
    int tPo = results.indexOf(expected.substring(0, 17));
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, Math.min(results.length(), tPo + expected.length())),
        expected,
        "results=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, "", tDir, eddTable.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Byte row;\n"
            + "    Int16 region;\n"
            + "    Int16 year;\n"
            + "    Int16 market_category;\n"
            + "    Int16 month;\n"
            + "    Int16 block;\n"
            + "    Int32 pounds;\n"
            + "    String area;\n"
            + "    String imported;\n"
            + "    Int16 region_caught;\n"
            + "    Float64 time;\n"
            + "    String description;\n"
            + "    String nominal_species;\n"
            + "    String species_group;\n"
            + "    String comments;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test make data files
    String2.log("\n****************** EDDTableCopyFiles make DATA FILES\n");

    // .csv
    userDapQuery = "area&distinct()";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, eddTable.className() + "_areaList", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Central California\n"
            + "Northern California\n"
            + "Oregon\n"
            + "Southern California\n"
            + "Washington\n";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);

    // .csv for one area and time range
    userDapQuery =
        "&area=\"Central California\"&nominal_species=\"ABLN\"&time>=1976-01-01&time<=1976-04-01";
    tName =
        eddTable.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, eddTable.className() + "_1areaGTLT", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "row,region,year,market_category,month,block,pounds,area,imported,region_caught,time,description,nominal_species,species_group,comments\n"
            + //
            ",,,,,,pounds,,,,UTC,,,,\n"
            + //
            "101,4,1976,702,1,400,540,Central California,N,4,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "102,4,1976,702,1,464,352,Central California,N,4,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "103,4,1976,702,1,472,2463,Central California,N,4,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,4,1976,702,3,455,296,Central California,N,4,1976-03-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,4,1976,702,3,464,409,Central California,N,4,1976-03-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,4,1976,702,3,472,3212,Central California,N,4,1976-03-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "109,5,1976,702,1,500,169,Central California,N,5,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "110,5,1976,702,1,657,675,Central California,N,5,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,1,600,236,Central California,N,6,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,1,607,1699,Central California,N,6,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,1,615,8379,Central California,N,6,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,1,623,1706,Central California,N,6,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,1,637,574,Central California,N,6,1976-01-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,3,600,146,Central California,N,6,1976-03-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n"
            + //
            "NaN,6,1976,702,3,615,5533,Central California,N,6,1976-03-15T00:00:00Z,\"Abalone, red\",ABLN,INV,\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    String2.log("\n*** testCopyFilesBasic() finished successfully.");
    // FileVisitorDNLS.verbose = false;
    // FileVisitorDNLS.reallyVerbose = false;
    // FileVisitorDNLS.debugMode = false;
    /* */
  }

  /* EDDTableFromNccsvFiles */

  /**
   * This tests actual_range in .nccsvMetadata and .nccsv responses. This requires pmelTaoDySst and
   * rPmelTaoDySst in localhost erddap.
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
    // note that there is actual_range info
    results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,_FillValue,1.0E35f\n" : "")
            + "*GLOBAL*,cdm_data_type,TimeSeries\n"
            + "*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,CREATION_DATE,hh:mm  D-MMM-YYYY\n" : "")
            + "*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n"
            + "*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n"
            + "*GLOBAL*,creator_type,group\n"
            + "*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n"
            + "*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n"
            + "*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n"
            + "*GLOBAL*,Easternmost_Easting,357.0d\n"
            + "*GLOBAL*,featureType,TimeSeries\n"
            + "*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n"
            + "*GLOBAL*,geospatial_lat_max,21.0d\n"
            + "*GLOBAL*,geospatial_lat_min,-25.0d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,357.0d\n"
            + "*GLOBAL*,geospatial_lon_min,0.0d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,geospatial_vertical_max,15.0d\n"
            + "*GLOBAL*,geospatial_vertical_min,1.0d\n"
            + "*GLOBAL*,geospatial_vertical_positive,down\n"
            + "*GLOBAL*,geospatial_vertical_units,m\n"
            + "*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n"
            + "dddd-dd-dd Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully refreshed ERD's copy of this dataset by downloading all of the .cdf files from the PMEL TAO FTP site.  Since then, the dataset has been partially refreshed everyday by downloading and merging the latest version of the last 25 days worth of data.\"\n"
            + "*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n"
            + "*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n"
            + "*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,missing_value,1.0E35f\n" : "")
            + "*GLOBAL*,Northernmost_Northing,21.0d\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,platform_code,CODE\n" : "")
            + "*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n"
            + "*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,-25.0d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n"
            + "*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n"
            + "*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n"
            + "*GLOBAL*,testOutOfDate,now-3days\n"
            + "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n"
            + "*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n"
            + "*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n"
            + "*GLOBAL*,Westernmost_Easting,0.0d\n"
            + "array,*DATA_TYPE*,String\n"
            + "array,ioos_category,Identifier\n"
            + "array,long_name,Array\n"
            + "station,*DATA_TYPE*,String\n"
            + "station,cf_role,timeseries_id\n"
            + "station,ioos_category,Identifier\n"
            + "station,long_name,Station\n"
            + "wmo_platform_code,*DATA_TYPE*,int\n"
            + "wmo_platform_code,actual_range,13001i,CHANGESi\n"
            + "wmo_platform_code,ioos_category,Identifier\n"
            + "wmo_platform_code,long_name,WMO Platform Code\n"
            + "wmo_platform_code,missing_value,2147483647i\n"
            + "longitude,*DATA_TYPE*,float\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,actual_range,0.0f,357.0f\n"
            + "longitude,axis,X\n"
            + "longitude,epic_code,502i\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Nominal Longitude\n"
            + "longitude,missing_value,1.0E35f\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,type,EVEN\n"
            + "longitude,units,degrees_east\n"
            + "latitude,*DATA_TYPE*,float\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,actual_range,-25.0f,21.0f\n"
            + "latitude,axis,Y\n"
            + "latitude,epic_code,500i\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Nominal Latitude\n"
            + "latitude,missing_value,1.0E35f\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,type,EVEN\n"
            + "latitude,units,degrees_north\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,actual_range,1977-11-03T12:00:00Z\\ndddd-dd-ddT12:00:00Z\n"
            + // stop time changes
            "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Centered Time\n"
            + (results.indexOf("time,point_spacing,even") > -1 ? "time,point_spacing,even\n" : "")
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,type,EVEN\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "depth,*DATA_TYPE*,float\n"
            + "depth,_CoordinateAxisType,Height\n"
            + "depth,_CoordinateZisPositive,down\n"
            + "depth,actual_range,1.0f,15.0f\n"
            + "depth,axis,Z\n"
            + "depth,epic_code,3i\n"
            + "depth,ioos_category,Location\n"
            + "depth,long_name,Depth\n"
            + "depth,missing_value,1.0E35f\n"
            + "depth,positive,down\n"
            + "depth,standard_name,depth\n"
            + "depth,type,EVEN\n"
            + "depth,units,m\n"
            + "T_25,*DATA_TYPE*,float\n"
            + (results.indexOf("T_25,_FillValue,1.0E35f") > -1 ? "T_25,_FillValue,1.0E35f\n" : "")
            + "T_25,actual_range,17.12f,35.4621f\n"
            + "T_25,colorBarMaximum,32.0d\n"
            + "T_25,colorBarMinimum,0.0d\n"
            + "T_25,epic_code,25i\n"
            + "T_25,generic_name,temp\n"
            + "T_25,ioos_category,Temperature\n"
            + "T_25,long_name,Sea Surface Temperature\n"
            + "T_25,missing_value,1.0E35f\n"
            + "T_25,name,T\n"
            + "T_25,standard_name,sea_surface_temperature\n"
            + "T_25,units,degree_C\n"
            + "QT_5025,*DATA_TYPE*,float\n"
            + (results.indexOf("QT_5025,_FillValue,1.0E35f") > -1
                ? "QT_5025,_FillValue,1.0E35f\n"
                : "")
            + "QT_5025,actual_range,0.0f,5.0f\n"
            + "QT_5025,colorBarContinuous,false\n"
            + "QT_5025,colorBarMaximum,6.0d\n"
            + "QT_5025,colorBarMinimum,0.0d\n"
            + "QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n"
            + "QT_5025,epic_code,5025i\n"
            + "QT_5025,generic_name,qt\n"
            + "QT_5025,ioos_category,Quality\n"
            + "QT_5025,long_name,Sea Surface Temperature Quality\n"
            + "QT_5025,missing_value,1.0E35f\n"
            + "QT_5025,name,QT\n"
            + "ST_6025,*DATA_TYPE*,float\n"
            + (results.indexOf("ST_6025,_FillValue,1.0E35f") > -1
                ? "ST_6025,_FillValue,1.0E35f\n"
                : "")
            + "ST_6025,actual_range,0.0f,5.0f\n"
            + "ST_6025,colorBarContinuous,false\n"
            + "ST_6025,colorBarMaximum,8.0d\n"
            + "ST_6025,colorBarMinimum,0.0d\n"
            + "ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n"
            + "ST_6025,epic_code,6025i\n"
            + "ST_6025,generic_name,st\n"
            + "ST_6025,ioos_category,Other\n"
            + "ST_6025,long_name,Sea Surface Temperature Source\n"
            + "ST_6025,missing_value,1.0E35f\n"
            + "ST_6025,name,ST\n"
            + "\n"
            + "*END_METADATA*\n";

    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,time_coverage_end,....-..-..T12:00:00Z\n",
            "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,CREATION_DATE,..:..  .-...-....\n",
            "*GLOBAL*,CREATION_DATE,hh:mm  D-MMM-YYYY\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,platform_code,[a-zA-Z0-9]+", "*GLOBAL*,platform_code,CODE");
    results =
        results.replaceAll(
            "time,actual_range,1977-11-03T12:00:00Z\\\\n....-..-..T12:00:00Z\n",
            "time,actual_range,1977-11-03T12:00:00Z\\\\ndddd-dd-ddT12:00:00Z\n");
    results =
        results.replaceAll(
            "wmo_platform_code,actual_range,13001i,[0-9]+i",
            "wmo_platform_code,actual_range,13001i,CHANGESi");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery);
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,time_coverage_end,....-..-..T12:00:00Z\n",
            "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,CREATION_DATE,..:..  .-...-....\n",
            "*GLOBAL*,CREATION_DATE,hh:mm  D-MMM-YYYY\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,platform_code,[a-zA-Z0-9]+", "*GLOBAL*,platform_code,CODE");
    results =
        results.replaceAll(
            "time,actual_range,1977-11-03T12:00:00Z\\\\n....-..-..T12:00:00Z\n",
            "time,actual_range,1977-11-03T12:00:00Z\\\\ndddd-dd-ddT12:00:00Z\n");
    results =
        results.replaceAll(
            "wmo_platform_code,actual_range,13001i,[0-9]+i",
            "wmo_platform_code,actual_range,13001i,CHANGESi");
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test getting .nccsv
    tQuery = ".nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05";
    // note no actual_range info
    results = SSR.getUrlResponseStringUnchanged(baseUrl + tQuery);
    expected =
        "*GLOBAL*,Conventions,\"COARDS, CF-1.6, ACDD-1.3, NCCSV-1.2\"\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,_FillValue,1.0E35f\n" : "")
            + "*GLOBAL*,cdm_data_type,TimeSeries\n"
            + "*GLOBAL*,cdm_timeseries_variables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,CREATION_DATE,hh:mm  D-MMM-YYYY\n" : "")
            + "*GLOBAL*,creator_email,Dai.C.McClurg@noaa.gov\n"
            + "*GLOBAL*,creator_name,GTMBA Project Office/NOAA/PMEL\n"
            + "*GLOBAL*,creator_type,group\n"
            + "*GLOBAL*,creator_url,https://www.pmel.noaa.gov/gtmba/mission\n"
            + "*GLOBAL*,Data_Source,Global Tropical Moored Buoy Array Project Office/NOAA/PMEL\n"
            + "*GLOBAL*,defaultGraphQuery,\"longitude,latitude,T_25&time>=now-7days\"\n"
            + "*GLOBAL*,Easternmost_Easting,357.0d\n"
            + "*GLOBAL*,featureType,TimeSeries\n"
            + "*GLOBAL*,File_info,Contact: Dai.C.McClurg@noaa.gov\n"
            + "*GLOBAL*,geospatial_lat_max,21.0d\n"
            + "*GLOBAL*,geospatial_lat_min,-25.0d\n"
            + "*GLOBAL*,geospatial_lat_units,degrees_north\n"
            + "*GLOBAL*,geospatial_lon_max,357.0d\n"
            + "*GLOBAL*,geospatial_lon_min,0.0d\n"
            + "*GLOBAL*,geospatial_lon_units,degrees_east\n"
            + "*GLOBAL*,geospatial_vertical_max,15.0d\n"
            + "*GLOBAL*,geospatial_vertical_min,1.0d\n"
            + "*GLOBAL*,geospatial_vertical_positive,down\n"
            + "*GLOBAL*,geospatial_vertical_units,m\n"
            + "*GLOBAL*,history,\"This dataset has data from the TAO/TRITON, RAMA, and PIRATA projects.\\nThis dataset is a product of the TAO Project Office at NOAA/PMEL.\\n";
    // +
    // "dddd-dd-dd Bob Simons at NOAA/NMFS/SWFSC/ERD (bob.simons@noaa.gov) fully
    // refreshed ERD's copy of this dataset by downloading all of the .cdf files
    // from the PMEL TAO FTP site. Since then, the dataset has been partially
    // refreshed everyday by downloading and merging the latest version of the last
    // 25 days worth of data.\\n";
    // "2017-05-26T18:30:46Z (local files)\\n" +
    // "2017-05-26T18:30:46Z
    expected2 =
        "http://localhost:"
            + PORT
            + "/erddap/tabledap/pmelTaoDySst.nccsv?&station=%220n125w%22&time%3E=2010-01-01&time%3C=2010-01-05\"\n"
            + "*GLOBAL*,infoUrl,https://www.pmel.noaa.gov/gtmba/mission\n"
            + "*GLOBAL*,institution,\"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\"\n"
            + "*GLOBAL*,keywords,\"buoys, centered, daily, depth, Earth Science > Oceans > Ocean Temperature > Sea Surface Temperature, identifier, noaa, ocean, oceans, pirata, pmel, quality, rama, sea, sea_surface_temperature, source, station, surface, tao, temperature, time, triton\"\n"
            + "*GLOBAL*,keywords_vocabulary,GCMD Science Keywords\n"
            + "*GLOBAL*,license,\"Request for Acknowledgement: If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\\n\\nThe data may be used and redistributed for free but is not intended\\nfor legal use, since it may contain inaccuracies. Neither the data\\nContributor, ERD, NOAA, nor the United States Government, nor any\\nof their employees or contractors, makes any warranty, express or\\nimplied, including warranties of merchantability and fitness for a\\nparticular purpose, or assumes any legal liability for the accuracy,\\ncompleteness, or usefulness, of this information.\"\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,missing_value,1.0E35f\n" : "")
            + "*GLOBAL*,Northernmost_Northing,21.0d\n"
            + (EDStatic.useSaxParser ? "*GLOBAL*,platform_code,CODE\n" : "")
            + "*GLOBAL*,project,\"TAO/TRITON, RAMA, PIRATA\"\n"
            + "*GLOBAL*,Request_for_acknowledgement,\"If you use these data in publications or presentations, please acknowledge the GTMBA Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: GTMBA Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\"\n"
            + "*GLOBAL*,sourceUrl,(local files)\n"
            + "*GLOBAL*,Southernmost_Northing,-25.0d\n"
            + "*GLOBAL*,standard_name_vocabulary,CF Standard Name Table v70\n"
            + "*GLOBAL*,subsetVariables,\"array, station, wmo_platform_code, longitude, latitude, depth\"\n"
            + "*GLOBAL*,summary,\"This dataset has daily Sea Surface Temperature (SST) data from the\\nTAO/TRITON (Pacific Ocean, https://www.pmel.noaa.gov/gtmba/ ),\\nRAMA (Indian Ocean, https://www.pmel.noaa.gov/gtmba/pmel-theme/indian-ocean-rama ), and\\nPIRATA (Atlantic Ocean, https://www.pmel.noaa.gov/gtmba/pirata/ )\\narrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\\nhttps://www.pmel.noaa.gov/gtmba/mission .\"\n"
            + "*GLOBAL*,testOutOfDate,now-3days\n"
            + "*GLOBAL*,time_coverage_end,dddd-dd-ddT12:00:00Z\n"
            + "*GLOBAL*,time_coverage_start,1977-11-03T12:00:00Z\n"
            + "*GLOBAL*,title,\"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, 1977-present, Sea Surface Temperature\"\n"
            + "*GLOBAL*,Westernmost_Easting,0.0d\n"
            + "array,*DATA_TYPE*,String\n"
            + "array,ioos_category,Identifier\n"
            + "array,long_name,Array\n"
            + "station,*DATA_TYPE*,String\n"
            + "station,cf_role,timeseries_id\n"
            + "station,ioos_category,Identifier\n"
            + "station,long_name,Station\n"
            + "wmo_platform_code,*DATA_TYPE*,int\n"
            + "wmo_platform_code,ioos_category,Identifier\n"
            + "wmo_platform_code,long_name,WMO Platform Code\n"
            + "wmo_platform_code,missing_value,2147483647i\n"
            + "longitude,*DATA_TYPE*,float\n"
            + "longitude,_CoordinateAxisType,Lon\n"
            + "longitude,axis,X\n"
            + "longitude,epic_code,502i\n"
            + "longitude,ioos_category,Location\n"
            + "longitude,long_name,Nominal Longitude\n"
            + "longitude,missing_value,1.0E35f\n"
            + "longitude,standard_name,longitude\n"
            + "longitude,type,EVEN\n"
            + "longitude,units,degrees_east\n"
            + "latitude,*DATA_TYPE*,float\n"
            + "latitude,_CoordinateAxisType,Lat\n"
            + "latitude,axis,Y\n"
            + "latitude,epic_code,500i\n"
            + "latitude,ioos_category,Location\n"
            + "latitude,long_name,Nominal Latitude\n"
            + "latitude,missing_value,1.0E35f\n"
            + "latitude,standard_name,latitude\n"
            + "latitude,type,EVEN\n"
            + "latitude,units,degrees_north\n"
            + "time,*DATA_TYPE*,String\n"
            + "time,_CoordinateAxisType,Time\n"
            + "time,axis,T\n"
            + "time,ioos_category,Time\n"
            + "time,long_name,Centered Time\n"
            + (results.indexOf("time,point_spacing,even") > -1 ? "time,point_spacing,even\n" : "")
            + "time,standard_name,time\n"
            + "time,time_origin,01-JAN-1970 00:00:00\n"
            + "time,type,EVEN\n"
            + "time,units,yyyy-MM-dd'T'HH:mm:ssZ\n"
            + "depth,*DATA_TYPE*,float\n"
            + "depth,_CoordinateAxisType,Height\n"
            + "depth,_CoordinateZisPositive,down\n"
            + "depth,axis,Z\n"
            + "depth,epic_code,3i\n"
            + "depth,ioos_category,Location\n"
            + "depth,long_name,Depth\n"
            + "depth,missing_value,1.0E35f\n"
            + "depth,positive,down\n"
            + "depth,standard_name,depth\n"
            + "depth,type,EVEN\n"
            + "depth,units,m\n"
            + "T_25,*DATA_TYPE*,float\n"
            + (results.indexOf("T_25,_FillValue,1.0E35f") > -1 ? "T_25,_FillValue,1.0E35f\n" : "")
            + "T_25,colorBarMaximum,32.0d\n"
            + "T_25,colorBarMinimum,0.0d\n"
            + "T_25,epic_code,25i\n"
            + "T_25,generic_name,temp\n"
            + "T_25,ioos_category,Temperature\n"
            + "T_25,long_name,Sea Surface Temperature\n"
            + "T_25,missing_value,1.0E35f\n"
            + "T_25,name,T\n"
            + "T_25,standard_name,sea_surface_temperature\n"
            + "T_25,units,degree_C\n"
            + "QT_5025,*DATA_TYPE*,float\n"
            + (results.indexOf("QT_5025,_FillValue,1.0E35f") > -1
                ? "QT_5025,_FillValue,1.0E35f\n"
                : "")
            + "QT_5025,colorBarContinuous,false\n"
            + "QT_5025,colorBarMaximum,6.0d\n"
            + "QT_5025,colorBarMinimum,0.0d\n"
            + "QT_5025,description,\"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QT_5025>=1 and QT_5025<=3.\"\n"
            + "QT_5025,epic_code,5025i\n"
            + "QT_5025,generic_name,qt\n"
            + "QT_5025,ioos_category,Quality\n"
            + "QT_5025,long_name,Sea Surface Temperature Quality\n"
            + "QT_5025,missing_value,1.0E35f\n"
            + "QT_5025,name,QT\n"
            + "ST_6025,*DATA_TYPE*,float\n"
            + (results.indexOf("ST_6025,_FillValue,1.0E35f") > -1
                ? "ST_6025,_FillValue,1.0E35f\n"
                : "")
            + "ST_6025,colorBarContinuous,false\n"
            + "ST_6025,colorBarMaximum,8.0d\n"
            + "ST_6025,colorBarMinimum,0.0d\n"
            + "ST_6025,description,\"Source Codes:\\n0 = No Sensor, No Data\\n1 = Real Time (Telemetered Mode)\\n2 = Derived from Real Time\\n3 = Temporally Interpolated from Real Time\\n4 = Source Code Inactive at Present\\n5 = Recovered from Instrument RAM (Delayed Mode)\\n6 = Derived from RAM\\n7 = Temporally Interpolated from RAM\"\n"
            + "ST_6025,epic_code,6025i\n"
            + "ST_6025,generic_name,st\n"
            + "ST_6025,ioos_category,Other\n"
            + "ST_6025,long_name,Sea Surface Temperature Source\n"
            + "ST_6025,missing_value,1.0E35f\n"
            + "ST_6025,name,ST\n"
            + "\n"
            + "*END_METADATA*\n"
            + "array,station,wmo_platform_code,longitude,latitude,time,depth,T_25,QT_5025,ST_6025\n"
            + "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-01T12:00:00Z,1.0,1.0E35,0.0,0.0\n"
            + "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-02T12:00:00Z,1.0,1.0E35,0.0,0.0\n"
            + "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-03T12:00:00Z,1.0,1.0E35,0.0,0.0\n"
            + "TAO/TRITON,0n125w,51011,235.0,0.0,2010-01-04T12:00:00Z,1.0,1.0E35,0.0,0.0\n"
            + "*END_DATA*\n";

    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,CREATION_DATE,..:..  .-...-....\n",
            "*GLOBAL*,CREATION_DATE,hh:mm  D-MMM-YYYY\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,platform_code,[a-zA-Z0-9]+", "*GLOBAL*,platform_code,CODE");
    results =
        results.replaceAll(
            "time_coverage_end,....-..-..T12:00:00Z\n", "time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf(expected2.substring(0, 100));
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    results = SSR.getUrlResponseStringUnchanged(rbaseUrl + tQuery); // difference: 'r'baseUrl
    results = results.replaceAll("....-..-.. Bob Simons", "dddd-dd-dd Bob Simons");
    results =
        results.replaceAll(
            "time_coverage_end,....-..-..T12:00:00Z\n", "time_coverage_end,dddd-dd-ddT12:00:00Z\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,CREATION_DATE,..:..  .-...-....\n",
            "*GLOBAL*,CREATION_DATE,hh:mm  D-MMM-YYYY\n");
    results =
        results.replaceAll(
            "\\*GLOBAL\\*,platform_code,[a-zA-Z0-9]+", "*GLOBAL*,platform_code,CODE");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf(expected2.substring(0, 100));
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);
  }

  /** EDDGridFromDap */

  /**
   * This tests a depth axis variable. This requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in
   * localhost ERDDAP.
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

    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            "EDDGridLonPM180_testGridWithDepth2",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("depth {");
    Test.ensureTrue(po >= 0, "results=\n" + results);
    expected =
        "depth {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    String _CoordinateZisPositive \"down\";\n"
            + "    Float64 actual_range 5.01, 5375.0;\n"
            + // 2014-01-17 was 5.0, 5374.0
            "    String axis \"Z\";\n"
            + (EDStatic.useSaxParser
                ? "    String grads_dim \"z\";\n" + "    String grads_mapping \"levels\";\n"
                : "")
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Depth\";\n"
            + (EDStatic.useSaxParser
                ? "    Float64 maximum 5375.0;\n"
                    + "    Float64 minimum 5.01;\n"
                    + "    String name \"Depth\";\n"
                : "")
            + "    String positive \"down\";\n"
            + (EDStatic.useSaxParser ? "    Float32 resolution 137.69205;\n" : "")
            + "    String standard_name \"depth\";\n"
            + "    String units \"m\";\n"
            + "  }";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // FGDC should deal with depth correctly
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            "EDDGridLonPM180_testGridWithDepth2",
            ".fgdc");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    po = results.indexOf("<vertdef>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "<vertdef>\n"
            + "      <depthsys>\n"
            + "        <depthdn>Unknown</depthdn>\n"
            + "        <depthres>Unknown</depthres>\n"
            + "        <depthdu>meters</depthdu>\n"
            + "        <depthem>Explicit depth coordinate included with horizontal coordinates</depthem>\n"
            + "      </depthsys>\n"
            + "    </vertdef>\n"
            + "  </spref>";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // ISO 19115 should deal with depth correctly
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            "EDDGridLonPM180_testGridWithDepth2",
            ".iso19115");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);

    po = results.indexOf("codeListValue=\"vertical\">");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n"
            + "          </gmd:dimensionName>\n"
            + "          <gmd:dimensionSize>\n"
            + "            <gco:Integer>40</gco:Integer>\n"
            + "          </gmd:dimensionSize>\n"
            + "          <gmd:resolution>\n"
            + "            <gco:Measure uom=\"m\">137.69205128205127</gco:Measure>\n"
            + // 2014-01-17
            // was
            // 137.66666666666666
            "          </gmd:resolution>\n"
            + "        </gmd:MD_Dimension>\n"
            + "      </gmd:axisDimensionProperties>\n";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    po = results.indexOf("<gmd:EX_VerticalExtent>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "<gmd:EX_VerticalExtent>\n"
            + "              <gmd:minimumValue><gco:Real>-5375.0</gco:Real></gmd:minimumValue>\n"
            + "              <gmd:maximumValue><gco:Real>-5.01</gco:Real></gmd:maximumValue>\n"
            + "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n"
            + "            </gmd:EX_VerticalExtent>";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // test WMS 1.1.0 service getCapabilities from localhost erddap
    String2.log(
        "\nTest WMS 1.1.0 getCapabilities\n"
            + "!!! This test requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP!!!");
    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:"
                + PORT
                + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
                + "service=WMS&request=GetCapabilities&version=1.1.0");
    po = results.indexOf("</Layer>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "</Layer>\n"
            + "      <Layer>\n"
            + "        <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180</Title>\n"
            + "        <SRS>EPSG:4326</SRS>\n"
            + "        <LatLonBoundingBox minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" />\n"
            + "        <BoundingBox SRS=\"EPSG:4326\" minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" resx=\"0.5\" resy=\"0.5\" />\n"
            + "        <Dimension name=\"time\" units=\"ISO8601\" />\n"
            + "        <Dimension name=\"elevation\" units=\"EPSG:5030\" />\n"
            +
            // 2014-01-24 default was 2008-12-15
            "        <Extent name=\"time\" default=\"2010-12-15T00:00:00Z\" >1871-01-15T00:00:00Z,1871-02-15T00:00:00Z,1871-03-15T00:00:00Z,";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    po = results.indexOf("<Extent name=\"elevation\"");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "<Extent name=\"elevation\" default=\"-5375.0\" >-5.01,-15.07,-25.28,-35.76,-46.61,-57.98,-70.02,-82.92,-96.92,-112.32,-129.49,-148.96,-171.4,-197.79,-229.48,-268.46,-317.65,-381.39,-465.91,-579.31,-729.35,-918.37,-1139.15,-1378.57,-1625.7,-1875.11,-2125.01,-2375.0,-2625.0,-2875.0,-3125.0,-3375.0,-3625.0,-3875.0,-4125.0,-4375.0,-4625.0,-4875.0,-5125.0,-5375.0</Extent>\n"
            + "        <Attribution>\n"
            + "          <Title>TAMU/UMD</Title>\n"
            + "          <OnlineResource xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "            xlink:type=\"simple\"\n"
            + "            xlink:href=\"https://www.atmos.umd.edu/~ocean/\" />\n"
            + "        </Attribution>\n"
            + "        <Layer opaque=\"1\" >\n"
            + "          <Name>hawaii_d90f_20ee_c4cb_LonPM180:temp</Name>\n"
            + "          <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180 - temp</Title>\n"
            + "        </Layer>";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // WMS 1.1.0 elevation=-5
    String baseName = "EDDGridLonPM180_TestGridWithDepth2110e5";
    String obsDir = Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR);
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:"
            + PORT
            + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
            + "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            + "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            + "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES="
            + "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName,
        false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // WMS 1.1.0 default elevation
    baseName = "EDDGridLonPM180_TestGridWithDepth2110edef";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:"
            + PORT
            + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
            + "EXCEPTIONS=INIMAGE&VERSION=1.1.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            + "&TIME=2008-11-15T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080"
            + "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES="
            + "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName,
        false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test WMS 1.3.0 service getCapabilities from localhost erddap
    String2.log(
        "\nTest WMS 1.3.0 getCapabilities\n"
            + "!!! This test requires hawaii_d90f_20ee_c4cb_LonPM180 dataset in localhost ERDDAP!!!");
    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:"
                + PORT
                + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
                + "service=WMS&request=GetCapabilities&version=1.3.0");

    po = results.indexOf("</Layer>");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "</Layer>\n"
            + "      <Layer>\n"
            + "        <Title>SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths), Lon+/-180</Title>\n"
            + "        <CRS>CRS:84</CRS>\n"
            + "        <CRS>EPSG:4326</CRS>\n"
            + "        <EX_GeographicBoundingBox>\n"
            + "          <westBoundLongitude>-179.75</westBoundLongitude>\n"
            + "          <eastBoundLongitude>179.75</eastBoundLongitude>\n"
            + "          <southBoundLatitude>-75.25</southBoundLatitude>\n"
            + "          <northBoundLatitude>89.25</northBoundLatitude>\n"
            + "        </EX_GeographicBoundingBox>\n"
            + "        <BoundingBox CRS=\"EPSG:4326\" minx=\"-179.75\" miny=\"-75.25\" maxx=\"179.75\" maxy=\"89.25\" resx=\"0.5\" resy=\"0.5\" />\n"
            + "        <Dimension name=\"time\" units=\"ISO8601\" multipleValues=\"0\" nearestValue=\"1\" default=\"2010-12-15T00:00:00Z\" >1871-01-15T00:00:00Z,1871-02-15T00:00:00Z,";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    po = results.indexOf("<Dimension name=\"elevation\"");
    Test.ensureTrue(po >= 0, "po=-1 results=\n" + results);
    expected =
        "<Dimension name=\"elevation\" units=\"CRS:88\" unitSymbol=\"m\" multipleValues=\"0\" nearestValue=\"1\" default=\"-5375.0\" >-5.01,-15.07,-25.28,-35.76,-46.61,-57.98,-70.02,-82.92,-96.92,-112.32,-129.49,-148.96,-171.4,-197.79,-229.48,-268.46,-317.65,-381.39,-465.91,-579.31,-729.35,-918.37,-1139.15,-1378.57,-1625.7,-1875.11,-2125.01,-2375.0,-2625.0,-2875.0,-3125.0,-3375.0,-3625.0,-3875.0,-4125.0,-4375.0,-4625.0,-4875.0,-5125.0,-5375.0</Dimension>";
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // WMS 1.3.0 elevation=-5
    // 2022-07-07 trouble with wms png request, so test underlying data request
    // first
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "temp%5B(2008-11-15)%5D%5B(5)%5D%5B(-75):100:(75)%5D%5B(-90):100:(63.6)%5D",
            EDStatic.fullTestCacheDirectory,
            "EDDGridLonPM180_testGridWithDepthPreWMS",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time,depth,latitude,longitude,temp\n"
            + "UTC,m,degrees_north,degrees_east,degree_C\n"
            + "2008-11-15T00:00:00Z,5.01,-74.75,-89.75,NaN\n"
            + "2008-11-15T00:00:00Z,5.01,-74.75,-39.75,-1.9969722\n"
            + "2008-11-15T00:00:00Z,5.01,-74.75,10.25,NaN\n"
            + "2008-11-15T00:00:00Z,5.01,-74.75,60.25,NaN\n"
            + "2008-11-15T00:00:00Z,5.01,-24.75,-89.75,19.052225\n"
            + "2008-11-15T00:00:00Z,5.01,-24.75,-39.75,22.358824\n"
            + "2008-11-15T00:00:00Z,5.01,-24.75,10.25,17.43544\n"
            + "2008-11-15T00:00:00Z,5.01,-24.75,60.25,23.83485\n"
            + "2008-11-15T00:00:00Z,5.01,25.25,-89.75,26.235065\n"
            + "2008-11-15T00:00:00Z,5.01,25.25,-39.75,25.840372\n"
            + "2008-11-15T00:00:00Z,5.01,25.25,10.25,NaN\n"
            + "2008-11-15T00:00:00Z,5.01,25.25,60.25,27.425127\n"
            + "2008-11-15T00:00:00Z,5.01,75.25,-89.75,NaN\n"
            + "2008-11-15T00:00:00Z,5.01,75.25,-39.75,NaN\n"
            + "2008-11-15T00:00:00Z,5.01,75.25,10.25,4.0587144\n"
            + "2008-11-15T00:00:00Z,5.01,75.25,60.25,-0.4989917\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // (see section above) now request troubling wms png
    baseName = "EDDGridLonPM180_TestGridWithDepth2130e5";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:"
            + PORT
            + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
            + "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            + "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            + "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES="
            + "&BBOX=-75,-90,75,63.6&WIDTH=256&HEIGHT=256",
        tName,
        false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // WMS 1.1.0 default elevation
    baseName = "EDDGridLonPM180_TestGridWithDepth2130edef";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:"
            + PORT
            + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
            + "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            + "&TIME=2008-11-15T00%3A00%3A00Z&TRANSPARENT=true&BGCOLOR=0x808080"
            + "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES="
            + "&BBOX=-75,-90,75,63.6&WIDTH=256&HEIGHT=256",
        tName,
        false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test lat beyond dataset range (changed from -75:75 above to -80:80 here)
    baseName = "EDDGridLonPM180_BeyondRange";
    tName = obsDir + baseName + ".png";
    SSR.downloadFile(
        "http://localhost:"
            + PORT
            + "/erddap/wms/hawaii_d90f_20ee_c4cb_LonPM180/request?"
            + "EXCEPTIONS=INIMAGE&VERSION=1.3.0&SRS=EPSG%3A4326&LAYERS=hawaii_d90f_20ee_c4cb_LonPM180%3Atemp"
            + "&TIME=2008-11-15T00%3A00%3A00Z&ELEVATION=-5.0&TRANSPARENT=true&BGCOLOR=0x808080"
            + "&FORMAT=image%2Fpng&SERVICE=WMS&REQUEST=GetMap&STYLES="
            + "&BBOX=-80,-90,80,63.6&WIDTH=256&HEIGHT=256",
        tName,
        false);
    // Test.displayInBrowser("file://" + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
  }

  /** This tests saveAsKml. */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagThredds // external server is failing to respond, so disable the test for now
  void testKml() throws Throwable {
    // testVerboseOn();
    int language = 0;

    EDDGridFromDap gridDataset = (EDDGridFromDap) EDDTestDataset.gettestActualRange();
    String name, tName, results, expected;
    String dir = EDStatic.fullTestCacheDirectory;

    // overall kml
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "SST[800][][]",
            dir,
            gridDataset.className() + "_testKml",
            ".kml");
    // Test.displayInBrowser("file://" + dir + tName);
    results = File2.directReadFromUtf8File(dir + tName);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
            + //
            "<Document>\n"
            + //
            "  <name>NOAA Coral Reef Watch 25km Ocean Acidification, Caribbean, Preliminary, 0.25°, 2016-present</name>\n"
            + //
            "  <description><![CDATA[Time: 2018-03-13T00:00:00Z<br />\n"
            + //
            "Data courtesy of USDOC/NOAA Coral Reef Watch<br />\n"
            + //
            "<a href=\"http://localhost:"
            + PORT
            + "/erddap/griddap/testActualRange.html?SST\">Download data from this dataset.</a><br />\n"
            + //
            "    ]]></description>\n"
            + //
            "  <Region>\n"
            + //
            "    <Lod><minLodPixels>2</minLodPixels></Lod>\n"
            + //
            "    <LatLonAltBox>\n"
            + //
            "      <west>-90.125</west>\n"
            + //
            "      <east>-59.875</east>\n"
            + //
            "      <south>14.875</south>\n"
            + //
            "      <north>30.125</north>\n"
            + //
            "    </LatLonAltBox>\n"
            + //
            "  </Region>\n"
            + //
            "  <GroundOverlay>\n"
            + //
            "    <drawOrder>1</drawOrder>\n"
            + //
            "    <Icon>\n"
            + //
            "      <href>http://localhost:"
            + PORT
            + "/erddap/griddap/testActualRange.transparentPng?SST%5B(2018-03-13T00%3A00%3A00Z)%5D%5B(14.875)%3A1%3A(30.125)%5D%5B(-90.125)%3A1%3A(-59.875)%5D</href>\n"
            + //
            "    </Icon>\n"
            + //
            "    <LatLonBox>\n"
            + //
            "      <west>-90.125</west>\n"
            + //
            "      <east>-59.875</east>\n"
            + //
            "      <south>14.875</south>\n"
            + //
            "      <north>30.125</north>\n"
            + //
            "    </LatLonBox>\n"
            + //
            "  </GroundOverlay>\n"
            + //
            "  <ScreenOverlay id=\"Logo\">\n"
            + //
            "    <description>http://localhost:"
            + PORT
            + "/erddap</description>\n"
            + //
            "    <name>Logo</name>\n"
            + //
            "    <Icon><href>http://localhost:"
            + PORT
            + "/erddap/images/nlogo.gif</href></Icon>\n"
            + //
            "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n"
            + //
            "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n"
            + //
            "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n"
            + //
            "  </ScreenOverlay>\n"
            + //
            "</Document>\n"
            + //
            "</kml>\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // a quadrant
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "SST[1500][(15.0):(30.0)][(-75.0):(-60.0)]",
            dir,
            gridDataset.className() + "_testKml2",
            ".kml");
    results = File2.directReadFromUtf8File(dir + tName);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
            + //
            "<Document>\n"
            + //
            "  <name>NOAA Coral Reef Watch 25km Ocean Acidification, Caribbean, Preliminary, 0.25°, 2016-present</name>\n"
            + //
            "  <description><![CDATA[Time: 2020-04-23T00:00:00Z<br />\n"
            + //
            "Data courtesy of USDOC/NOAA Coral Reef Watch<br />\n"
            + //
            "<a href=\"http://localhost:"
            + PORT
            + "/erddap/griddap/testActualRange.html?SST\">Download data from this dataset.</a><br />\n"
            + //
            "    ]]></description>\n"
            + //
            "  <Region>\n"
            + //
            "    <Lod><minLodPixels>2</minLodPixels></Lod>\n"
            + //
            "    <LatLonAltBox>\n"
            + //
            "      <west>-74.875</west>\n"
            + //
            "      <east>-59.875</east>\n"
            + //
            "      <south>15.125</south>\n"
            + //
            "      <north>30.125</north>\n"
            + //
            "    </LatLonAltBox>\n"
            + //
            "  </Region>\n"
            + //
            "  <GroundOverlay>\n"
            + //
            "    <drawOrder>1</drawOrder>\n"
            + //
            "    <Icon>\n"
            + //
            "      <href>http://localhost:"
            + PORT
            + "/erddap/griddap/testActualRange.transparentPng?SST%5B(2020-04-23T00%3A00%3A00Z)%5D%5B(15.125)%3A1%3A(30.125)%5D%5B(-74.875)%3A1%3A(-59.875)%5D</href>\n"
            + //
            "    </Icon>\n"
            + //
            "    <LatLonBox>\n"
            + //
            "      <west>-74.875</west>\n"
            + //
            "      <east>-59.875</east>\n"
            + //
            "      <south>15.125</south>\n"
            + //
            "      <north>30.125</north>\n"
            + //
            "    </LatLonBox>\n"
            + //
            "  </GroundOverlay>\n"
            + //
            "  <ScreenOverlay id=\"Logo\">\n"
            + //
            "    <description>http://localhost:"
            + PORT
            + "/erddap</description>\n"
            + //
            "    <name>Logo</name>\n"
            + //
            "    <Icon><href>http://localhost:"
            + PORT
            + "/erddap/images/nlogo.gif</href></Icon>\n"
            + //
            "    <overlayXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n"
            + //
            "    <screenXY x=\"0.005\" y=\".04\" xunits=\"fraction\" yunits=\"fraction\"/>\n"
            + //
            "    <size x=\"0\" y=\"0\" xunits=\"pixels\" yunits=\"pixels\"/>\n"
            + //
            "  </ScreenOverlay>\n"
            + //
            "</Document>\n"
            + //
            "</kml>\n";
    ;
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void testOpendap() throws Throwable {
    String2.log(
        "\n****************** EDDGridFromDap test opendap\n"
            + "!!!THIS READS DATA FROM SERVER RUNNING ON COASTWATCH: erdMHchla8day on "
            + EDStatic.erddapUrl
            + "!!!"); // in tests, always non-https url
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
    String erddapUrl =
        EDStatic.erddapUrl + "/griddap/hawaii_d90f_20ee_c4cb"; // in tests, always non-https url
    DConnect threddsConnect = new DConnect(threddsUrl, true, 1, 1);
    DConnect erddapConnect = new DConnect(erddapUrl, true, 1, 1); // in tests, always non-https url
    DAS das = erddapConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    DDS dds = erddapConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    PrimitiveArray tpas[], epas[];

    // get global attributes
    Attributes attributes = new Attributes();
    OpendapHelper.getAttributes(das, "GLOBAL", attributes);
    Test.ensureEqual(attributes.getString("contributor_name"), null, "");
    Test.ensureEqual(
        attributes.getString("keywords"),
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
    Test.ensureEqual(attributes.getString("standard_name"), "sea_water_practical_salinity", "");
    Test.ensureEqual(attributes.getString("units"), "PSU", "");

    // test get dimension data - all
    String threddsQuery = "lat";
    String erddapQuery = "latitude";
    String2.log(
        "\nFrom thredds:\n"
            + String2.annotatedString(
                SSR.getUrlResponseStringUnchanged(threddsUrl + ".asc?" + threddsQuery)));
    String2.log(
        "\nFrom erddap:\n"
            + String2.annotatedString(
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
    String2.log(
        "\nFrom thredds:\n"
            + String2.annotatedString(
                SSR.getUrlResponseStringUnchanged(threddsUrl + ".asc?" + threddsQuery)));
    String2.log(
        "\nFrom erddap:\n"
            + String2.annotatedString(
                SSR.getUrlResponseStringUnchanged(erddapUrl + ".asc?" + erddapQuery))); // in tests,
    // always
    // non-https
    // url
    tpas = OpendapHelper.getPrimitiveArrays(threddsConnect, "?" + threddsQuery);
    epas = OpendapHelper.getPrimitiveArrays(erddapConnect, "?" + erddapQuery);
    Test.ensureEqual(tpas[0].toString(), "-70.25, -69.25, -68.25, -67.25, -66.25, -65.25", "");
    Test.ensureEqual(epas[0].toString(), "-70.25, -69.25, -68.25, -67.25, -66.25, -65.25", "");

    // get grid data
    // chlorophyll[177][0][2080:20:2500][4500:20:4940]
    String threddsUserDapQuery = SSR.percentEncode("salt[177][0][8:2:10][350]");
    String griddapUserDapQuery = SSR.percentEncode("salt[177][0][8:2:10][350]");
    String2.log(
        "\nFrom thredds:\n"
            + String2.annotatedString(
                SSR.getUrlResponseStringUnchanged(threddsUrl + ".asc?" + threddsUserDapQuery)));
    String2.log(
        "\nFrom erddap:\n"
            + String2.annotatedString(
                SSR.getUrlResponseStringUnchanged(
                    erddapUrl + ".asc?" + griddapUserDapQuery))); // in
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
    NetcdfFile nc =
        NetcdfDatasets.openFile(EDStatic.erddapUrl + "/griddap/hawaii_d90f_20ee_c4cb", null); // in
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
          "netcdf hawaii_d90f_20ee_c4cb {\n"
              + //
              "  dimensions:\n"
              + //
              "    time = 1680;\n"
              + //
              "    depth = 40;\n"
              + //
              "    latitude = 330;\n"
              + //
              "    longitude = 720;\n"
              + //
              "  variables:\n"
              + //
              "    double time(time=1680);\n"
              + //
              "      :_CoordinateAxisType = \"Time\";\n"
              + //
              "      :actual_range = -3.122928E9, 1.2923712E9; // double\n"
              + //
              "      :axis = \"T\";\n"
              + //
              (EDStatic.useSaxParser
                  ? "      :grads_dim = \"t\";\n"
                      + //
                      "      :grads_mapping = \"linear\";\n"
                      + "      :grads_min = \"00z15jan1871\";\n"
                      + "      :grads_size = \"1680\";\n"
                      + "      :grads_step = \"1mo\";\n"
                  : "")
              + "      :ioos_category = \"Time\";\n"
              + //
              "      :long_name = \"Centered Time\";\n"
              + (EDStatic.useSaxParser
                  ? "      :maximum = \"00z15dec2010\";\n"
                      + "      :minimum = \"00z15jan1871\";\n"
                      + "      :resolution = 30.43657f; // float\n"
                  : "")
              + "      :standard_name = \"time\";\n"
              + //
              "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
              + //
              "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
              + //
              "\n"
              + //
              "    double depth(depth=40);\n"
              + //
              "      :_CoordinateAxisType = \"Height\";\n"
              + //
              "      :_CoordinateZisPositive = \"down\";\n"
              + //
              "      :actual_range = 5.01, 5375.0; // double\n"
              + //
              "      :axis = \"Z\";\n"
              + //
              (EDStatic.useSaxParser
                  ? "      :grads_dim = \"z\";\n" + "      :grads_mapping = \"levels\";\n"
                  : "")
              + "      :ioos_category = \"Location\";\n"
              + "      :long_name = \"Depth\";\n"
              + (EDStatic.useSaxParser
                  ? "      :maximum = 5375.0; // double\n"
                      + "      :minimum = 5.01; // double\n"
                      + "      :name = \"Depth\";\n"
                  : "")
              + "      :positive = \"down\";\n"
              + (EDStatic.useSaxParser ? "      :resolution = 137.69205f; // float\n" : "")
              + "      :standard_name = \"depth\";\n"
              + //
              "      :units = \"m\";\n"
              + //
              "\n"
              + //
              "    double latitude(latitude=330);\n"
              + //
              "      :_CoordinateAxisType = \"Lat\";\n"
              + //
              "      :actual_range = -75.25, 89.25; // double\n"
              + //
              "      :axis = \"Y\";\n"
              + //
              (EDStatic.useSaxParser
                  ? "      :grads_dim = \"y\";\n"
                      + "      :grads_mapping = \"linear\";\n"
                      + "      :grads_size = \"330\";\n"
                  : "")
              + "      :ioos_category = \"Location\";\n"
              + "      :long_name = \"Latitude\";\n"
              + (EDStatic.useSaxParser
                  ? "      :maximum = 89.25; // double\n"
                      + "      :minimum = -75.25; // double\n"
                      + "      :resolution = 0.5f; // float\n"
                  : "")
              + "      :standard_name = \"latitude\";\n"
              + //
              "      :units = \"degrees_north\";\n"
              + //
              "\n"
              + //
              "    double longitude(longitude=720);\n"
              + //
              "      :_CoordinateAxisType = \"Lon\";\n"
              + //
              "      :actual_range = 0.25, 359.75; // double\n"
              + //
              "      :axis = \"X\";\n"
              + //
              (EDStatic.useSaxParser
                  ? "      :grads_dim = \"x\";\n"
                      + "      :grads_mapping = \"linear\";\n"
                      + "      :grads_size = \"720\";\n"
                  : "")
              + "      :ioos_category = \"Location\";\n"
              + "      :long_name = \"Longitude\";\n"
              + (EDStatic.useSaxParser
                  ? "      :maximum = 359.75; // double\n"
                      + "      :minimum = 0.25; // double\n"
                      + "      :resolution = 0.5f; // float\n"
                  : "")
              + //
              "      :standard_name = \"longitude\";\n"
              + //
              "      :units = \"degrees_east\";\n"
              + //
              "\n"
              + //
              "    float temp(time=1680, depth=40, latitude=330, longitude=720);\n"
              + //
              "      :_CoordinateAxes = \"time depth latitude longitude \";\n"
              + //
              "      :_FillValue = -9.99E33f; // float\n"
              + //
              "      :colorBarMaximum = 32.0; // double\n"
              + //
              "      :colorBarMinimum = 0.0; // double\n"
              + //
              "      :ioos_category = \"Temperature\";\n"
              + //
              "      :long_name = \"Sea Water Temperature\";\n"
              + //
              "      :missing_value = -9.99E33f; // float\n"
              + //
              "      :standard_name = \"sea_water_temperature\";\n"
              + //
              "      :units = \"degree_C\";\n"
              + //
              "\n"
              + //
              "    float salt(time=1680, depth=40, latitude=330, longitude=720);\n"
              + //
              "      :_CoordinateAxes = \"time depth latitude longitude \";\n"
              + //
              "      :_FillValue = -9.99E33f; // float\n"
              + //
              "      :colorBarMaximum = 37.0; // double\n"
              + //
              "      :colorBarMinimum = 32.0; // double\n"
              + //
              "      :ioos_category = \"Salinity\";\n"
              + //
              "      :long_name = \"Sea Water Practical Salinity\";\n"
              + //
              "      :missing_value = -9.99E33f; // float\n"
              + //
              "      :standard_name = \"sea_water_practical_salinity\";\n"
              + //
              "      :units = \"PSU\";\n"
              + //
              "\n"
              + //
              "    float u(time=1680, depth=40, latitude=330, longitude=720);\n"
              + //
              "      :_CoordinateAxes = \"time depth latitude longitude \";\n"
              + //
              "      :_FillValue = -9.99E33f; // float\n"
              + //
              "      :colorBarMaximum = 0.5; // double\n"
              + //
              "      :colorBarMinimum = -0.5; // double\n"
              + //
              "      :ioos_category = \"Currents\";\n"
              + //
              "      :long_name = \"Eastward Sea Water Velocity\";\n"
              + //
              "      :missing_value = -9.99E33f; // float\n"
              + //
              "      :standard_name = \"eastward_sea_water_velocity\";\n"
              + //
              "      :units = \"m s-1\";\n"
              + //
              "\n"
              + //
              "    float v(time=1680, depth=40, latitude=330, longitude=720);\n"
              + //
              "      :_CoordinateAxes = \"time depth latitude longitude \";\n"
              + //
              "      :_FillValue = -9.99E33f; // float\n"
              + //
              "      :colorBarMaximum = 0.5; // double\n"
              + //
              "      :colorBarMinimum = -0.5; // double\n"
              + //
              "      :ioos_category = \"Currents\";\n"
              + //
              "      :long_name = \"Northward Sea Water Velocity\";\n"
              + //
              "      :missing_value = -9.99E33f; // float\n"
              + //
              "      :standard_name = \"northward_sea_water_velocity\";\n"
              + //
              "      :units = \"m s-1\";\n"
              + //
              "\n"
              + //
              "    float w(time=1680, depth=40, latitude=330, longitude=720);\n"
              + //
              "      :_CoordinateAxes = \"time depth latitude longitude \";\n"
              + //
              "      :_FillValue = -9.99E33f; // float\n"
              + //
              "      :colorBarMaximum = 1.0E-5; // double\n"
              + //
              "      :colorBarMinimum = -1.0E-5; // double\n"
              + //
              "      :comment = \"WARNING: Please use this variable's data with caution.\";\n"
              + //
              "      :ioos_category = \"Currents\";\n"
              + //
              "      :long_name = \"Upward Sea Water Velocity\";\n"
              + //
              "      :missing_value = -9.99E33f; // float\n"
              + //
              "      :standard_name = \"upward_sea_water_velocity\";\n"
              + //
              "      :units = \"m s-1\";\n"
              + //
              "\n"
              + //
              "  // global attributes:\n"
              + //
              "  :cdm_data_type = \"Grid\";\n"
              + //
              "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
              + //
              "  :dataType = \"Grid\";\n"
              + //
              "  :defaultDataQuery = \"temp[last][0][0:last][0:last],salt[last][0][0:last][0:last],u[last][0][0:last][0:last],v[last][0][0:last][0:last],w[last][0][0:last][0:last]\";\n"
              + //
              "  :defaultGraphQuery = \"temp[last][0][0:last][0:last]&.draw=surface&.vars=longitude|latitude|temp\";\n"
              + //
              "  :documentation = \"http://apdrc.soest.hawaii.edu/datadoc/soda_2.2.4.php\";\n"
              + //
              "  :Easternmost_Easting = 359.75; // double\n"
              + //
              "  :geospatial_lat_max = 89.25; // double\n"
              + //
              "  :geospatial_lat_min = -75.25; // double\n"
              + //
              "  :geospatial_lat_resolution = 0.5; // double\n"
              + //
              "  :geospatial_lat_units = \"degrees_north\";\n"
              + //
              "  :geospatial_lon_max = 359.75; // double\n"
              + //
              "  :geospatial_lon_min = 0.25; // double\n"
              + //
              "  :geospatial_lon_resolution = 0.5; // double\n"
              + //
              "  :geospatial_lon_units = \"degrees_east\";\n"
              + //
              "  :history = \"DDD MMM dd hh:mm:ss ZZZ YYYY : imported by GrADS Data Server 2.0\n";
      results =
          results.replaceAll(
              "\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\w{3} \\d{4}",
              "DDD MMM dd hh:mm:ss ZZZ YYYY");
      // int po = results.indexOf(":history = \"NASA GSFC (OBPG)\n");
      // Test.ensureTrue(po > 0, "RESULTS=\n" + results);
      // Test.ensureLinesMatch(results.substring(0, po + 29), expected, "RESULTS=\n" +
      // results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

      expected =
          "  :infoUrl = \"https://www.atmos.umd.edu/~ocean/\";\n"
              + //
              "  :institution = \"TAMU/UMD\";\n"
              + //
              "  :keywords = \"circulation, currents, density, depths, Earth Science > Oceans > Ocean Circulation > Ocean Currents, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, eastward, eastward_sea_water_velocity, means, monthly, northward, northward_sea_water_velocity, ocean, oceans, pop, salinity, sea, sea_water_practical_salinity, sea_water_temperature, seawater, soda, tamu, temperature, umd, upward, upward_sea_water_velocity, velocity, water\";\n"
              + //
              "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
              + //
              "  :license = \"The data may be used and redistributed for free but is not intended\n"
              + //
              "for legal use, since it may contain inaccuracies. Neither the data\n"
              + //
              "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
              + //
              "of their employees or contractors, makes any warranty, express or\n"
              + //
              "implied, including warranties of merchantability and fitness for a\n"
              + //
              "particular purpose, or assumes any legal liability for the accuracy,\n"
              + //
              "completeness, or usefulness, of this information.\";\n"
              + //
              "  :Northernmost_Northing = 89.25; // double\n"
              + //
              "  :sourceUrl = \"http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4\";\n"
              + //
              "  :Southernmost_Northing = -75.25; // double\n"
              + //
              "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
              + //
              "  :summary = \"Simple Ocean Data Assimilation (SODA) version 2.2.4 - A reanalysis of ocean \n"
              + //
              "climate. SODA uses the GFDL modular ocean model version 2.2. The model is \n"
              + //
              "forced by observed surface wind stresses from the COADS data set (from 1958 \n"
              + //
              "to 1992) and from NCEP (after 1992). Note that the wind stresses were \n"
              + //
              "detrended before use due to inconsistencies with observed sea level pressure \n"
              + //
              "trends. The model is also constrained by constant assimilation of observed \n"
              + //
              "temperatures, salinities, and altimetry using an optimal data assimilation \n"
              + //
              "technique. The observed data comes from: 1) The World Ocean Atlas 1994 which \n"
              + //
              "contains ocean temperatures and salinities from mechanical \n"
              + //
              "bathythermographs, expendable bathythermographs and conductivity-temperature-\n"
              + //
              "depth probes. 2) The expendable bathythermograph archive 3) The TOGA-TAO \n"
              + //
              "thermistor array 4) The Soviet SECTIONS tropical program 5) Satellite \n"
              + //
              "altimetry from Geosat, ERS/1 and TOPEX/Poseidon. \n"
              + //
              "We are now exploring an eddy-permitting reanalysis based on the Parallel \n"
              + //
              "Ocean Program POP-1.4 model with 40 levels in the vertical and a 0.4x0.25 \n"
              + //
              "degree displaced pole grid (25 km resolution in the western North \n"
              + //
              "Atlantic).  The first version of this we will release is SODA1.2, a \n"
              + //
              "reanalysis driven by ERA-40 winds covering the period 1958-2001 (extended to \n"
              + //
              "the current year using available altimetry).\";\n"
              + //
              "  :time_coverage_end = \"2010-12-15T00:00:00Z\";\n"
              + //
              "  :time_coverage_start = \"1871-01-15T00:00:00Z\";\n"
              + //
              "  :title = \"SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths)\";\n"
              + //
              "  :Westernmost_Easting = 0.25; // double\n"
              + //
              "}\n";
      Test.ensureEqual(
          results.substring(results.indexOf("  :infoUrl =")), expected, "RESULTS=\n" + results);

      attributes.clear();
      NcHelper.getGroupAttributes(nc.getRootGroup(), attributes);
      Test.ensureEqual(attributes.getString("institution"), "TAMU/UMD", "");
      Test.ensureEqual(
          attributes.getString("keywords"),
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
      Test.ensureEqual(attributes.getString("standard_name"), "sea_water_practical_salinity", "");
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

    EDDGrid eddGrid2 =
        new EDDGridFromDap(
            "erddapChlorophyll", // String tDatasetID,
            null,
            null,
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new Object[][] {
              { // dataVariables[dvIndex][0=sourceName, 1=destName, 2=addAttributes]
                "salt", null, null
              }
            },
            60, // int tReloadEveryNMinutes,
            -1, // updateEveryNMillis,
            erddapUrl,
            -1,
            true); // sourceUrl, nThreads, dimensionValuesInMemory); //in tests,
    // always non-https
    // url

    // .xhtml
    tName =
        eddGrid2.makeNewFileForDapQuery(
            language,
            null,
            null,
            griddapUserDapQuery,
            EDStatic.fullTestCacheDirectory,
            eddGrid2.className() + "_Itself",
            ".xhtml");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
            + //
            "  \"https://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + //
            "<html xmlns=\"https://www.w3.org/1999/xhtml\">\n"
            + //
            "<head>\n"
            + //
            "  <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n"
            + //
            "  <title>EDDGridFromDap_Itself</title>\n"
            + //
            "  <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost:"
            + PORT
            + "/erddap/images/erddap2.css\" />\n"
            + //
            "</head>\n"
            + //
            "<body>\n"
            + //
            "\n"
            + //
            "&nbsp;\n"
            + //
            "<table class=\"erd commonBGColor nowrap\">\n"
            + //
            "<tr>\n"
            + //
            "<th>time</th>\n"
            + //
            "<th>depth</th>\n"
            + //
            "<th>latitude</th>\n"
            + //
            "<th>longitude</th>\n"
            + //
            "<th>salt</th>\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<th>UTC</th>\n"
            + //
            "<th>m</th>\n"
            + //
            "<th>degrees_north</th>\n"
            + //
            "<th>degrees_east</th>\n"
            + //
            "<th>PSU</th>\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<td>1885-10-15T00:00:00Z</td>\n"
            + //
            "<td class=\"R\">5.01</td>\n"
            + //
            "<td class=\"R\">-71.25</td>\n"
            + //
            "<td class=\"R\">175.25</td>\n"
            + //
            "<td class=\"R\">34.12381</td>\n"
            + //
            "</tr>\n"
            + //
            "<tr>\n"
            + //
            "<td>1885-10-15T00:00:00Z</td>\n"
            + //
            "<td class=\"R\">5.01</td>\n"
            + //
            "<td class=\"R\">-70.25</td>\n"
            + //
            "<td class=\"R\">175.25</td>\n"
            + //
            "<td class=\"R\">34.12087</td>\n"
            + //
            "</tr>\n"
            + //
            "</table>\n"
            + //
            "</body>\n"
            + //
            "</html>\n";
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /** EDDGridFromErddap */

  /** testGenerateDatasetsXml */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testEDDGridFromErddapGenerateDatasetsXml() throws Throwable {
    // testVerboseOn();

    // test local generateDatasetsXml
    String results =
        EDDGridFromErddap.generateDatasetsXml(EDStatic.erddapUrl, false) + "\n"; // in tests, always
    // non-https url
    String2.log("results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {"-verbose", "EDDGridFromErddap", EDStatic.erddapUrl, "false"},
                false); // doIt loop?
    Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

    String expected =
        "<dataset type=\"EDDGridFromErddap\" datasetID=\"localhost_e87c_42d8_b49d\" active=\"true\">\n"
            + //
            "    <!-- Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite) -->\n"
            + //
            "    <sourceUrl>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMH1chla8day</sourceUrl>\n"
            + //
            "</dataset>\n";
    int po = results.indexOf(expected.substring(0, 80));
    if (po < 0) throw new RuntimeException(results);
    // String2.log("results=" + results);
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "<!-- Of the datasets above, the following datasets are EDDGridFromErddap's at the remote ERDDAP.\n";
    po = results.indexOf(expected.substring(0, 20));
    if (po < 0) String2.log("results=" + results);
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("rMH1chla8day", po) > 0,
        "results=\n"
            + results
            + "\nTHIS TEST REQUIRES rMH1chla8day TO BE ACTIVE ON THE localhost ERDDAP.");

    // ensure it is ready-to-use by making a dataset from it
    String tDatasetID = "localhost_04bf_84c9_022b";
    EDD.deleteCachedDatasetInfo(tDatasetID);
    EDD edd = EDDGridFromErddap.oneFromXmlFragment(null, results);
    String2.log(
        "\n!!! The first dataset will vary, depending on which are currently active!!!\n"
            + "title="
            + edd.title()
            + "\n"
            + "datasetID="
            + edd.datasetID()
            + "\n"
            + "vars="
            + String2.toCSSVString(edd.dataVariableDestinationNames()));
    Test.ensureEqual(edd.title(), "Audio data from a local source.", "");
    Test.ensureEqual(edd.datasetID(), tDatasetID, "");
    Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), "channel_1", "");
  }

  /** This does some basic tests. */
  @ParameterizedTest
  @ValueSource(booleans = {true})
  @TagJetty
  void testEDDGridFromErddapBasic(boolean testLocalErddapToo) throws Throwable {
    // testVerboseOn();
    int language = 0;
    EDDGridFromErddap gridDataset;
    String name, tName, axisDapQuery, query, results, expected, expected2, error;
    int tPo;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);
    String userDapQuery =
        SSR.minimalPercentEncode("chlorophyll[(2003-01-17)][(29.020832)][(-147.97917):1:(-147.8)]");
    String graphDapQuery = SSR.minimalPercentEncode("chlorophyll[0:10:200][][(29)][(225)]");
    String mapDapQuery =
        SSR.minimalPercentEncode("chlorophyll[200][][(29):(50)][(225):(247)]"); // stride
    // irrelevant
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    String localUrl =
        EDStatic.erddapUrl + "/griddap/rMH1chla8day"; // in tests, always non-https url

    gridDataset = (EDDGridFromErddap) EDDGridFromErddap.oneFromDatasetsXml(null, "rMH1chla8day");

    // *** test getting das for entire dataset
    String2.log("\n****************** EDDGridFromErddap test entire dataset\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Entire",
            ".das");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Attributes {\n"
            + //
            "  time {\n"
            + //
            "    String _CoordinateAxisType \"Time\";\n"
            + //
            "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"T\";\n"
            + //
            "    String ioos_category \"Time\";\n"
            + //
            "    String long_name \"Centered Time\";\n"
            + //
            "    String standard_name \"time\";\n"
            + //
            "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + //
            "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + //
            "  }\n"
            + //
            "  latitude {\n"
            + //
            "    String _CoordinateAxisType \"Lat\";\n"
            + //
            "    Float32 actual_range MIN, MAX;\n"
            + //
            "    String axis \"Y\";\n"
            + //
            "    String ioos_category \"Location\";\n"
            + //
            "    String long_name \"Latitude\";\n"
            + //
            "    String standard_name \"latitude\";\n"
            + //
            "    String units \"degrees_north\";\n"
            + //
            "    Float32 valid_max 90.0;\n"
            + //
            "    Float32 valid_min -90.0;\n"
            + //
            "  }\n"
            + //
            "  longitude {\n"
            + //
            "    String _CoordinateAxisType \"Lon\";\n"
            + //
            "    Float32 actual_range MIN, MAX;\n"
            + //
            "    String axis \"X\";\n"
            + //
            "    String ioos_category \"Location\";\n"
            + //
            "    String long_name \"Longitude\";\n"
            + //
            "    String standard_name \"longitude\";\n"
            + //
            "    String units \"degrees_east\";\n"
            + //
            "    Float32 valid_max 180.0;\n"
            + //
            "    Float32 valid_min -180.0;\n"
            + //
            "  }\n"
            + //
            "  chlorophyll {\n"
            + //
            "    Float32 _FillValue NaN;\n"
            + //
            "    Float64 colorBarMaximum 30.0;\n"
            + //
            "    Float64 colorBarMinimum 0.03;\n"
            + //
            "    String colorBarScale \"Log\";\n"
            + //
            "    String ioos_category \"Ocean Color\";\n"
            + //
            "    String long_name \"Mean Chlorophyll a Concentration\";\n"
            + //
            "    String references \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n"
            + //
            "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + //
            "    String units \"mg m-3\";\n"
            + //
            "    Float32 valid_max 100.0;\n"
            + //
            "    Float32 valid_min 0.001;\n"
            + //
            "  }\n"
            + //
            "  NC_GLOBAL {\n"
            + //
            "    String _lastModified \"YYYY-MM-DDTHH:mm:ss.000Z\";\n"
            + //
            // " String _NCProperties \"version=2,netcdf=4.7.3,hdf5=1.12.0,\";\n" + //
            "    String cdm_data_type \"Grid\";\n"
            + //
            "    String Conventions \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + //
            "    String creator_email \"data@oceancolor.gsfc.nasa.gov\";\n"
            + //
            "    String creator_name \"NASA/GSFC/OBPG\";\n"
            + //
            "    String creator_type \"group\";\n"
            + //
            "    String creator_url \"https://oceandata.sci.gsfc.nasa.gov\";\n"
            + //
            "    String date_created \"YYYY-MM-DDTHH:mm:ss.000Z\";\n"
            + //
            "    Float64 Easternmost_Easting 179.9792;\n"
            + //
            "    Float64 geospatial_lat_max 89.97916;\n"
            + //
            "    Float64 geospatial_lat_min MIN;\n"
            + //
            // " Float64 geospatial_lat_resolution 0.04166666589488307;\n" + //
            "    String geospatial_lat_units \"degrees_north\";\n"
            + //
            "    Float64 geospatial_lon_max 179.9792;\n"
            + //
            "    Float64 geospatial_lon_min -179.9792;\n"
            + //
            // " Float64 geospatial_lon_resolution 0.041666674383609215;\n" + //
            "    String geospatial_lon_units \"degrees_east\";\n"
            + //
            "    String grid_mapping_name \"latitude_longitude\";\n"
            + "    String history \"Files downloaded daily from https://oceandata.sci.gsfc.nasa.gov/MODIS-Aqua/L3SMI to NOAA SWFSC ERD (erd.data@noaa.gov)\n"
            + //
            "ERDDAP adds the time dimension.\n"
            + //
            "Direct read of HDF4 file through CDM library\n"; //
    // "2024-05-07T19:17:02Z (local files)\n" + //
    // "2024-05-07T19:17:02Z
    // http://localhost:" + PORT + "/erddap/griddap/rMH1chla8day.das\";\n" + //

    expected2 =
        "    String identifier_product_doi \"10.5067/AQUA/MODIS_OC.2014.0\";\n"
            + //
            "    String identifier_product_doi_authority \"https://dx.doi.org\";\n"
            + //
            "    String infoUrl \"https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\";\n"
            + //
            "    String institution \"NASA/GSFC OBPG\";\n"
            + //
            "    String instrument \"MODIS\";\n"
            + //
            "    String keywords \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, flight, goddard, group, gsfc, image, imaging, L3, level, level-3, mapped, moderate, modis, nasa, obpg, ocean, ocean color, oceans, oci, optics, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, time, water\";\n"
            + //
            "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + //
            "    String l2_flag_names \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n"
            + //
            "    String license \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
            + //
            "The data may be used and redistributed for free but is not intended\n"
            + //
            "for legal use, since it may contain inaccuracies. Neither the data\n"
            + //
            "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + //
            "of their employees or contractors, makes any warranty, express or\n"
            + //
            "implied, including warranties of merchantability and fitness for a\n"
            + //
            "particular purpose, or assumes any legal liability for the accuracy,\n"
            + //
            "completeness, or usefulness, of this information.\";\n"
            + //
            "    String map_projection \"Equidistant Cylindrical\";\n"
            + //
            "    String measure \"Mean\";\n"
            + //
            "    String naming_authority \"gov.noaa.pfeg.coastwatch\";\n"
            + //
            "    Float64 Northernmost_Northing 89.97916;\n"
            + //
            "    String platform \"Aqua\";\n"
            + //
            "    String processing_level \"L3 Mapped\";\n"
            + //
            "    String processing_version \"2014.0\";\n"
            + //
            "    String product_name \"A20030092003016.L3m_8D_CHL_chlor_a_4km.nc\";\n"
            + //
            "    String project \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n"
            + //
            "    String publisher_email \"erd.data@noaa.gov\";\n"
            + //
            "    String publisher_name \"NOAA NMFS SWFSC ERD\";\n"
            + //
            "    String publisher_type \"institution\";\n"
            + //
            "    String publisher_url \"https://www.pfeg.noaa.gov\";\n"
            + //
            "    String sourceUrl \"(local files)\";\n"
            + //
            "    Float64 Southernmost_Northing -89.97918;\n"
            + //
            "    String spatialResolution \"4.60 km\";\n"
            + //
            "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + //
            "    String summary \"This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.\";\n"
            + //
            "    String temporal_range \"8-day\";\n"
            + //
            "    String testOutOfDate \"now-30days\";\n"
            + //
            "    String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ\";\n"
            + //
            "    String time_coverage_start \"2003-01-05T00:00:00Z\";\n"
            + //
            "    String title \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\";\n"
            + //
            "    Float64 Westernmost_Easting -179.9792;\n"
            + //
            "  }\n"
            + //
            "}\n";
    // tPo = results.indexOf("history \"NASA GSFC (OBPG)");
    // Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9]+.[0-9]+e?.[0-9]?, -?[0-9]+.[0-9]+e?.[0-9]?;",
            "Float64 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
            "Float32 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "String _lastModified \"....-..-..T..:..:...000Z",
            "String _lastModified \"YYYY-MM-DDTHH:mm:ss.000Z");
    results =
        results.replaceAll(
            "String date_created \"....-..-..T..:..:...000Z",
            "String date_created \"YYYY-MM-DDTHH:mm:ss.000Z");
    results =
        results.replaceAll(
            "Float64 geospatial_lat_min -?[0-9]+.[0-9]+", "Float64 geospatial_lat_min MIN");
    results =
        results.replaceAll(
            "String time_coverage_end \\\"....-..-..T..:..:..Z",
            "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    // Test.ensureLinesMatch(results.substring(0, tPo + 25), expected,
    // "\nresults=\n" + results);

    tPo = results.indexOf("    String identifier_product_doi");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".das");
      results =
          results.replaceAll(
              "Float64 actual_range -?[0-9]+.[0-9]+e?.[0-9]?, -?[0-9]+.[0-9]+e?.[0-9]?;",
              "Float64 actual_range MIN, MAX;");
      results =
          results.replaceAll(
              "Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
              "Float32 actual_range MIN, MAX;");
      results =
          results.replaceAll(
              "String _lastModified \"....-..-..T..:..:...000Z",
              "String _lastModified \"YYYY-MM-DDTHH:mm:ss.000Z");
      results =
          results.replaceAll(
              "String date_created \"....-..-..T..:..:...000Z",
              "String date_created \"YYYY-MM-DDTHH:mm:ss.000Z");
      results =
          results.replaceAll(
              "Float64 geospatial_lat_min -?[0-9]+.[0-9]+", "Float64 geospatial_lat_min MIN");
      results =
          results.replaceAll(
              "String time_coverage_end \\\"....-..-..T..:..:..Z",
              "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
      // tPo = results.indexOf("history \"NASA GSFC (OBPG)");
      // Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
      // Test.ensureLinesMatch(results.substring(0, tPo + 25), expected,
      // "\nresults=\n" + results);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);

      tPo = results.indexOf("    String identifier_product_doi");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=" + results);
      Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);
    }

    // *** test getting dds for entire dataset
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "",
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Entire",
            ".dds");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + //
            "  Float64 time[time = TIME];\n"
            + //
            "  Float32 latitude[latitude = LATITUDE];\n"
            + //
            "  Float32 longitude[longitude = LONGITUDE];\n"
            + //
            "  GRID {\n"
            + //
            "    ARRAY:\n"
            + //
            "      Float32 chlorophyll[time = TIME][latitude = LATITUDE][longitude = LONGITUDE];\n"
            + //
            "    MAPS:\n"
            + //
            "      Float64 time[time = TIME];\n"
            + //
            "      Float32 latitude[latitude = LATITUDE];\n"
            + //
            "      Float32 longitude[longitude = LONGITUDE];\n"
            + //
            "  } chlorophyll;\n"
            + //
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
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".asc");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Float64 time[time = TIME];\n"
            + "  Float32 longitude[longitude = LONGITUDE];\n"
            + "} "; // r
    expected2 =
        "MH1chla8day;\n"
            + "---------------------------------------------\n"
            + "Data:\n"
            + "time[TIME]\n"
            + "VAL1, VAL2\n"
            + "longitude[LONGITUDE]\n"
            + "179.97917\n";
    results = results.replaceAll("time = [0-9]+", "time = TIME");
    results = results.replaceAll("time.[0-9]+.", "time[TIME]");
    results = results.replaceAll("longitude = [0-9]+", "longitude = LONGITUDE");
    results = results.replaceAll("longitude.[0-9]+.", "longitude[LONGITUDE]");
    results =
        results.replaceAll("-?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+\n", "VAL1, VAL2\n");
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
      results =
          results.replaceAll("-?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+\n", "VAL1, VAL2\n");
      Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
    }

    // .csv
    String2.log("\n*** EDDGridFromErddap test get .CSV axis data\n");
    query = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    // Test.displayInBrowser("file://" + EDStatic.fullTestCacheDirectory + tName);
    expected =
        "time,longitude\n"
            + "UTC,degrees_east\n"
            + "2003-01-05T00:00:00Z,179.97917\n"
            + //
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
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_AxisG.A",
            ".csv");
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
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".dods");
    results =
        String2.annotatedString(
            File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName));
    // String2.log(results);
    expected =
        "Dataset {[10]\n"
            + "  Float64 time[time = 2];[10]\n"
            + "  Float32 longitude[longitude = 1];[10]\n"
            + "} "; // r
    expected2 =
        "MH1chla8day;[10]\n"
            + "[10]\n"
            + "Data:[10]\n"
            + "[0][0][0][2][0][0][0][2]A[207][11][186][192][0][0][0]A[207][17][0][192][0][0][0][0][0][0][1][0][0][0][1]C3[250][171][end]";
    Test.ensureEqual(results, expected + "r" + expected2, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results =
          String2.annotatedString(SSR.getUrlResponseStringUnchanged(localUrl + ".dods?" + query));
      Test.ensureEqual(results, expected + "erd" + expected2, "\nresults=\n" + results);
    }

    // .mat
    // octave> load('c:/temp/griddap/EDDGridFromErddap_Axis.mat');
    // octave> erdMHchla8day
    String matlabAxisQuery = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    String2.log("\n*** EDDGridFromErddap test get .MAT axis data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            matlabAxisQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".mat");
    String2.log(".mat test file is " + EDStatic.fullTestCacheDirectory + tName);
    results = File2.hexDump(EDStatic.fullTestCacheDirectory + tName, 1000000);
    String2.log(results);
    expected =
        "4d 41 54 4c 41 42 20 35   2e 30 20 4d 41 54 2d 66   MATLAB 5.0 MAT-f |\n"
            + //
            "69 6c 65 2c 20 43 72 65   61 74 65 64 20 62 79 3a   ile, Created by: |\n"
            + //
            "20 67 6f 76 2e 6e 6f 61   61 2e 70 66 65 6c 2e 63    gov.noaa.pfel.c |\n"
            + //
            "6f 61 73 74 77 61 74 63   68 2e 4d 61 74 6c 61 62   oastwatch.Matlab |\n"
            + //
            // "2c 20 43 72 65 61 74 65 64 20 6f 6e 3a 20 57 65 , Created on: We |\n" + //
            // "64 20 4d 61 79 20 38 20 32 30 3a 31 34 3a 31 34 d May 8 20:14:14 |\n" + //
            // "20 32 30 32 34 20 20 20 20 20 20 20 20 20 20 20 2024 |\n" + //
            "20 20 20 20 00 00 00 00   00 00 00 00 01 00 4d 49                 MI |\n"
            + //
            "00 00 00 0e 00 00 01 10   00 00 00 06 00 00 00 08                    |\n"
            + //
            "00 00 00 02 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + //
            "00 00 00 01 00 00 00 01   00 00 00 01 00 00 00 0c                    |\n"
            + //
            "72 4d 48 31 63 68 6c 61   38 64 61 79 00 00 00 00   rMH1chla8day     |\n"
            + //
            "00 04 00 05 00 00 00 20   00 00 00 01 00 00 00 40                  @ |\n"
            + //
            "74 69 6d 65 00 00 00 00   00 00 00 00 00 00 00 00   time             |\n"
            + //
            "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + //
            "6c 6f 6e 67 69 74 75 64   65 00 00 00 00 00 00 00   longitude        |\n"
            + //
            "00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00                    |\n"
            + //
            "00 00 00 0e 00 00 00 40   00 00 00 06 00 00 00 08          @         |\n"
            + //
            "00 00 00 06 00 00 00 00   00 00 00 05 00 00 00 08                    |\n"
            + //
            "00 00 00 02 00 00 00 01   00 00 00 01 00 00 00 00                    |\n"
            + //
            "00 00 00 09 00 00 00 10   41 cf 0b ba c0 00 00 00           A        |\n"
            + //
            "41 cf 11 00 c0 00 00 00   00 00 00 0e 00 00 00 38   A              8 |\n"
            + //
            "00 00 00 06 00 00 00 08   00 00 00 07 00 00 00 00                    |\n"
            + //
            "00 00 00 05 00 00 00 08   00 00 00 01 00 00 00 01                    |\n"
            + //
            "00 00 00 01 00 00 00 00   00 00 00 07 00 00 00 04                    |\n"
            + //
            "43 33 fa ab 00 00 00 00   C3                                         |\n";
    Test.ensureEqual(
        results.substring(0, 71 * 4) + results.substring(71 * 7), // remove the creation dateTime
        expected,
        "RESULTS(" + EDStatic.fullTestCacheDirectory + tName + ")=\n" + results);

    // .ncHeader
    String2.log("\n*** EDDGridFromErddap test get .NCHEADER axis data\n");
    query = SSR.minimalPercentEncode("time[0:1:1],longitude[last]");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            query,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Axis",
            ".ncHeader");
    results = File2.directReadFromUtf8File(EDStatic.fullTestCacheDirectory + tName);
    // String2.log(results);
    expected =
        // "netcdf EDDGridFromErddap_Axis.nc {\n" +
        // " dimensions:\n" +
        // " time = 3;\n" + // (has coord.var)\n" + //changed when switched to
        // netcdf-java 4.0, 2009-02-23
        // " longitude = 1;\n" + // (has coord.var)\n" + //but won't change for
        // testLocalErddapToo until next release
        "  variables:\n"
            + "    double time(time=2);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = MIN, MAX; // double\n"
            + // up-to-date
            "      :axis = \"T\";\n"
            +
            // " :fraction_digits = 0; // int\n" +
            "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Centered Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    float longitude(longitude=1);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = MIN, MAX; // float\n"
            + "      :axis = \"X\";\n"
            +
            // " :coordsys = \"geographic\";\n" +
            // " :fraction_digits = 4; // int\n" +
            "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            +
            // " :point_spacing = \"even\";\n" +
            "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "      :valid_max = 180.0f; // float\n"
            + //
            "      :valid_min = -180.0f; // float\n"
            + "\n"
            + "  // global attributes:\n";
    // " :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n";
    results =
        results.replaceAll(
            ":actual_range = -?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+",
            ":actual_range = MIN, MAX");
    results =
        results.replaceAll(
            ":actual_range = -?[0-9]+.[0-9]+f, -?[0-9]+.[0-9]+f", ":actual_range = MIN, MAX");
    tPo = results.indexOf("  variables:\n");
    Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    Test.ensureEqual(
        results.substring(tPo, tPo + expected.length()), expected, "RESULTS=\n" + results);
    expected2 =
        "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\";\n"
            + "  :Westernmost_Easting = 179.9792f; // float\n"
            + "}\n";
    Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);

    if (testLocalErddapToo) {
      results = SSR.getUrlResponseStringUnchanged(localUrl + ".ncHeader?" + query);
      results =
          results.replaceAll(
              ":actual_range = -?[0-9]+.[0-9]+E[0-9]+, -?[0-9]+.[0-9]+E[0-9]+",
              ":actual_range = MIN, MAX");
      results =
          results.replaceAll(
              ":actual_range = -?[0-9]+.[0-9]+f, -?[0-9]+.[0-9]+f", ":actual_range = MIN, MAX");
      tPo = results.indexOf("  variables:\n");
      Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
      Test.ensureEqual(
          results.substring(tPo, tPo + expected.length()), expected, "\nresults=\n" + results);
      Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
    }

    // ********************************************** test getting grid data
    // .csv
    String2.log("\n*** EDDGridFromErddap test get .CSV data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".csv");
    results = File2.directReadFrom88591File(EDStatic.fullTestCacheDirectory + tName);
    expected = // missing values are "NaN"
        // pre 2010-10-26 was
        "time,latitude,longitude,chlorophyll\n"
            + //
            "UTC,degrees_north,degrees_east,mg m-3\n"
            + //
            "2003-01-13T00:00:00Z,29.02083,-147.97917,NaN\n"
            + //
            "2003-01-13T00:00:00Z,29.02083,-147.9375,NaN\n"
            + //
            "2003-01-13T00:00:00Z,29.02083,-147.89583,NaN\n"
            + //
            "2003-01-13T00:00:00Z,29.02083,-147.85417,NaN\n"
            + //
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
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            "chlorophyll." + userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_DotNotation",
            ".csv");
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
      results =
          SSR.getUrlResponseStringUnchanged(localUrl + ".csv" + "?chlorophyll." + userDapQuery);
      Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
      // Test.ensureTrue(results.indexOf(expected2) > 0, "RESULTS=\n" + results);
    }

    // .nc
    String2.log("\n*** EDDGridFromErddap test get .NC data\n");
    tName =
        gridDataset.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            EDStatic.fullTestCacheDirectory,
            gridDataset.className() + "_Data",
            ".nc");
    results = NcHelper.ncdump(EDStatic.fullTestCacheDirectory + tName, "");
    expected =
        "netcdf EDDGridFromErddap_Data.nc {\n"
            + //
            "  dimensions:\n"
            + //
            "    time = 1;\n"
            + //
            "    latitude = 1;\n"
            + //
            "    longitude = 5;\n"
            + //
            "  variables:\n"
            + //
            "    double time(time=1);\n"
            + //
            "      :_CoordinateAxisType = \"Time\";\n"
            + //
            "      :actual_range = 1.042416E9, 1.042416E9; // double\n"
            + //
            "      :axis = \"T\";\n"
            + //
            "      :ioos_category = \"Time\";\n"
            + //
            "      :long_name = \"Centered Time\";\n"
            + //
            "      :standard_name = \"time\";\n"
            + //
            "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + //
            "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + //
            "\n"
            + //
            "    float latitude(latitude=1);\n"
            + //
            "      :_CoordinateAxisType = \"Lat\";\n"
            + //
            "      :actual_range = 29.02083f, 29.02083f; // float\n"
            + //
            "      :axis = \"Y\";\n"
            + //
            "      :ioos_category = \"Location\";\n"
            + //
            "      :long_name = \"Latitude\";\n"
            + //
            "      :standard_name = \"latitude\";\n"
            + //
            "      :units = \"degrees_north\";\n"
            + //
            "      :valid_max = 90.0f; // float\n"
            + //
            "      :valid_min = -90.0f; // float\n"
            + //
            "\n"
            + //
            "    float longitude(longitude=5);\n"
            + //
            "      :_CoordinateAxisType = \"Lon\";\n"
            + //
            "      :actual_range = -147.97917f, -147.8125f; // float\n"
            + //
            "      :axis = \"X\";\n"
            + //
            "      :ioos_category = \"Location\";\n"
            + //
            "      :long_name = \"Longitude\";\n"
            + //
            "      :standard_name = \"longitude\";\n"
            + //
            "      :units = \"degrees_east\";\n"
            + //
            "      :valid_max = 180.0f; // float\n"
            + //
            "      :valid_min = -180.0f; // float\n"
            + //
            "\n"
            + //
            "    float chlorophyll(time=1, latitude=1, longitude=5);\n"
            + //
            "      :_FillValue = NaNf; // float\n"
            + //
            "      :colorBarMaximum = 30.0; // double\n"
            + //
            "      :colorBarMinimum = 0.03; // double\n"
            + //
            "      :colorBarScale = \"Log\";\n"
            + //
            "      :ioos_category = \"Ocean Color\";\n"
            + //
            "      :long_name = \"Mean Chlorophyll a Concentration\";\n"
            + //
            "      :references = \"Hu, C., Lee Z., and Franz, B.A. (2012). Chlorophyll-a algorithms for oligotrophic oceans: A novel approach based on three-band reflectance difference, J. Geophys. Res., 117, C01011, doi:10.1029/2011JC007395.\";\n"
            + //
            "      :standard_name = \"concentration_of_chlorophyll_in_sea_water\";\n"
            + //
            "      :units = \"mg m-3\";\n"
            + //
            "      :valid_max = 100.0f; // float\n"
            + //
            "      :valid_min = 0.001f; // float\n"
            + //
            "\n"
            + //
            "  // global attributes:\n"
            + //
            "  :_lastModified = \"YYYY-MM-DDTHH:mm:ss.000Z\";\n"
            + //
            "  :cdm_data_type = \"Grid\";\n"
            + //
            "  :Conventions = \"CF-1.6, COARDS, ACDD-1.3\";\n"
            + //
            "  :creator_email = \"data@oceancolor.gsfc.nasa.gov\";\n"
            + //
            "  :creator_name = \"NASA/GSFC/OBPG\";\n"
            + //
            "  :creator_type = \"group\";\n"
            + //
            "  :creator_url = \"https://oceandata.sci.gsfc.nasa.gov\";\n"
            + //
            "  :date_created = \"YYYY-MM-DDTHH:mm:ss.000Z\";\n"
            + //
            "  :Easternmost_Easting = -147.8125f; // float\n"
            + //
            "  :geospatial_lat_max = 29.02083f; // float\n"
            + //
            "  :geospatial_lat_min = 29.02083f; // float\n"
            + //
            "  :geospatial_lat_units = \"degrees_north\";\n"
            + //
            "  :geospatial_lon_max = -147.8125f; // float\n"
            + //
            "  :geospatial_lon_min = -147.97917f; // float\n"
            + //
            "  :geospatial_lon_units = \"degrees_east\";\n"
            + //
            "  :grid_mapping_name = \"latitude_longitude\";\n";
    // tPo = results.indexOf(":history = \"NASA GSFC (OBPG)");
    // Test.ensureTrue(tPo >= 0, "tPo=-1 results=\n" + results);
    results =
        results.replaceAll("\\\"....-..-..T..:..:...000Z\\\"", "\"YYYY-MM-DDTHH:mm:ss.000Z\"");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "RESULTS=\n" + results);

    expected = // note original missing values
        "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/MH1_chla_las.html\";\n"
            + //
            "  :institution = \"NASA/GSFC OBPG\";\n"
            + //
            "  :instrument = \"MODIS\";\n"
            + //
            "  :keywords = \"algorithm, biology, center, chemistry, chlor_a, chlorophyll, color, concentration, concentration_of_chlorophyll_in_sea_water, data, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Optics > Ocean Color, flight, goddard, group, gsfc, image, imaging, L3, level, level-3, mapped, moderate, modis, nasa, obpg, ocean, ocean color, oceans, oci, optics, processing, resolution, sea, seawater, smi, space, spectroradiometer, standard, time, water\";\n"
            + //
            "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + //
            "  :l2_flag_names = \"ATMFAIL,LAND,HILT,HISATZEN,STRAYLIGHT,CLDICE,COCCOLITH,LOWLW,CHLWARN,CHLFAIL,NAVWARN,MAXAERITER,ATMWARN,HISOLZEN,NAVFAIL,FILTER,HIGLINT\";\n"
            + //
            "  :license = \"https://science.nasa.gov/earth-science/earth-science-data/data-information-policy/\n"
            + //
            "The data may be used and redistributed for free but is not intended\n"
            + //
            "for legal use, since it may contain inaccuracies. Neither the data\n"
            + //
            "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + //
            "of their employees or contractors, makes any warranty, express or\n"
            + //
            "implied, including warranties of merchantability and fitness for a\n"
            + //
            "particular purpose, or assumes any legal liability for the accuracy,\n"
            + //
            "completeness, or usefulness, of this information.\";\n"
            + //
            "  :map_projection = \"Equidistant Cylindrical\";\n"
            + //
            "  :measure = \"Mean\";\n"
            + //
            "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + //
            "  :Northernmost_Northing = 29.02083f; // float\n"
            + //
            "  :platform = \"Aqua\";\n"
            + //
            "  :processing_level = \"L3 Mapped\";\n"
            + //
            "  :processing_version = \"2014.0\";\n"
            + //
            "  :product_name = \"A20030092003016.L3m_8D_CHL_chlor_a_4km.nc\";\n"
            + //
            "  :project = \"Ocean Biology Processing Group (NASA/GSFC/OBPG)\";\n"
            + //
            "  :publisher_email = \"erd.data@noaa.gov\";\n"
            + //
            "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n"
            + //
            "  :publisher_type = \"institution\";\n"
            + //
            "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n"
            + //
            "  :sourceUrl = \"(local files)\";\n"
            + //
            "  :Southernmost_Northing = 29.02083f; // float\n"
            + //
            "  :spatialResolution = \"4.60 km\";\n"
            + //
            "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + //
            "  :summary = \"This dataset has Level 3, Standard Mapped Image, 4km, chlorophyll-a concentration data from NASA's Aqua Spacecraft.  Measurements are gathered by the Moderate Resolution Imaging Spectroradiometer (MODIS) carried aboard the spacecraft.  This is Science Quality data.  This is the August 2015 version of this dataset.\";\n"
            + //
            "  :temporal_range = \"8-day\";\n"
            + //
            "  :testOutOfDate = \"now-30days\";\n"
            + //
            "  :time_coverage_end = \"2003-01-13T00:00:00Z\";\n"
            + //
            "  :time_coverage_start = \"2003-01-13T00:00:00Z\";\n"
            + //
            "  :title = \"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\";\n"
            + //
            "  :Westernmost_Easting = -147.97917f; // float\n"
            + //
            "\n"
            + //
            "  data:\n"
            + //
            "    time = \n"
            + //
            "      {1.042416E9}\n"
            + //
            "    latitude = \n"
            + //
            "      {29.02083}\n"
            + //
            "    longitude = \n"
            + //
            "      {-147.97917, -147.9375, -147.89583, -147.85417, -147.8125}\n"
            + //
            "    chlorophyll = \n"
            + //
            "      {\n"
            + //
            "        {\n"
            + //
            "          {NaN, NaN, NaN, NaN, NaN}\n"
            + //
            "        }\n"
            + //
            "      }\n"
            + //
            "}\n";

    tPo = results.indexOf("  :infoUrl");
    // int tPo2 = results.indexOf("1.20716},", tPo + 1);
    // if (tPo < 0 || tPo2 < 0)
    // String2.log("tPo=" + tPo + " tPo2=" + tPo2 + " results=\n" + results);

    Test.ensureEqual(results.substring(tPo), expected, "RESULTS=\n" + results);
    /* */
  }

  /**
   * This tests the /files/ "files" system. This requires nceiPH53sstn1day and testGridFromErddap in
   * the local ERDDAP.
   *
   * <p>EDDGridFromNcFiles.testFiles() has more tests than any other testFiles().
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGridFromErddapFiles() throws Throwable {

    String2.log("\n*** EDDGridFromErddap.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/.csv
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/.csv"));
    Test.ensureTrue(
        results.indexOf("Name,Last modified,Size,Description[10]") == 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "nceiPH53sstd1day/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Daytime (1 Day Composite)\"[10]")
            > 0,
        "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "rMH1chla8day/,NaN,NaN,\"Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (8 Day Composite)\"[10]")
            > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("documentation.html,") > 0, "results=\n" + results);

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/.csv");
    results = results.replaceAll(",.............,", ",lastMod,");
    expected =
        "Name,lastMod,Size,Description\n"
            + "A20030012003008.L3m_8D_CHL_chlor_a_4km.nc,lastMod,29437837,\n"
            + //
            "A20030092003016.L3m_8D_CHL_chlor_a_4km.nc,lastMod,30795913,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/rMH1chla8day/");
    Test.ensureTrue(
        results.indexOf(
                "A20030012003008&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc")
            > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") > 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "A20030092003016&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc")
            > 0,
        "results=\n" + results);

    // get /files/datasetID //missing trailing slash will be redirected
    results =
        SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/rMH1chla8day");
    Test.ensureTrue(
        results.indexOf(
                "A20030012003008&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc")
            > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("Parent Directory") > 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "A20030092003016&#x2e;L3m&#x5f;8D&#x5f;CHL&#x5f;chlor&#x5f;a&#x5f;4km&#x2e;nc")
            > 0,
        "results=\n" + results);

    // Todo get a dataset here with a subdir
    // get /files/datasetID/subdir/.csv
    // results = SSR.getUrlResponseStringNewline(
    // "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/1994/.csv");
    // expected = "Name,Last modified,Size,Description\n" +
    // "data/,NaN,NaN,\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // // get /files/datasetID/subdir/subdir.csv
    // results = SSR.getUrlResponseStringNewline(
    // "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/1994/data/.csv");
    // expected = "Name,Last modified,Size,Description\n" +
    // "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc,1471330800000,12484412,\n";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // // download a file in root -- none available

    // // download a file in subdir
    // results = String2.annotatedString(SSR.getUrlResponseStringNewline(
    // "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/1994/data/" +
    // "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc")
    // .substring(0, 50));
    // expected = "[137]HDF[10]\n" +
    // "[26][10]\n" +
    // "[2][8][8][0][0][0][0][0][0][0][0][0][255][255][255][255][255][255][255][255]<[127][190][0][0][0][0][0]0[0][0][0][0][0][0][0][199](*yOHD[end]";
    // Test.ensureEqual(results, expected, "results=\n" + results);

    // query with // at start fails
    try {
      results = SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected = "HTTP ERROR 400 Ambiguous URI empty segment";
    Test.ensureTrue(results.indexOf(expected) > 0, "results=\n" + results);

    // query with // later fails
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/rMH1chla8day//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    Test.ensureTrue(results.indexOf(expected) > 0, "results=\n" + results);

    // query with /../ fails
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/../");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=400 for URL: http://localhost:"
            + PORT
            + "/erddap/files/rMH1chla8day/../\n"
            + "(Error {\n"
            + "    code=400;\n"
            + "    message=\"Bad Request: Query error: /../ is not allowed!\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent datasetID
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download an existent subdirectory but without trailing slash
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/GLsubdir");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: ERROR from url=http://localhost:"
            + PORT
            + "/erddap/files/rMH1chla8day/GLsubdir : "
            + "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:"
            + PORT
            + "/erddap/files/erdMH1chla8day/GLsubdir\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: GLsubdir .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:"
            + PORT
            + "/erddap/files/rMH1chla8day/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in root
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: ERROR from url=http://localhost:"
            + PORT
            + "/erddap/files/rMH1chla8day/gibberish.csv : "
            + "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:"
            + PORT
            + "/erddap/files/erdMH1chla8day/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existent subdir
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/rMH1chla8day/GLsubdir/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: ERROR from url=http://localhost:"
            + PORT
            + "/erddap/files/rMH1chla8day/GLsubdir/gibberish.csv : "
            + "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: "
            + "http://localhost:"
            + PORT
            + "/erddap/files/erdMH1chla8day/GLsubdir/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** EDDGridFromEtopo */

  /**
   * This tests the /files/ "files" system. This requires etopo180 or etopo360 in the localhost
   * ERDDAP.
   *
   * @param tDatasetID etopo180 or etopo360
   */
  @ParameterizedTest
  @ValueSource(strings = {"etopo180", "etopo360"})
  @TagJetty
  void testEtopoGridFiles(String tDatasetID) throws Throwable {

    String2.log("\n*** EDDGridFromEtopo.testFiles(" + tDatasetID + ")\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    String etopoFilePath = EDStatic.getWebInfParentDirectory() + "WEB-INF/ref/etopo1_ice_g_i2.bin";
    long etopoLastModifiedMillis = File2.getLastModified(etopoFilePath);

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/.csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "etopo1_ice_g_i2.bin,"
            + etopoLastModifiedMillis
            + ",466624802,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/");
    Test.ensureTrue(
        results.indexOf("etopo1&#x5f;ice&#x5f;g&#x5f;i2&#x2e;bin") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">466624802<") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv

    // download a file in root
    String2.log("This test takes ~30 seconds.");
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/etopo1_ice_g_i2.bin");
    results = String2.annotatedString(results.substring(0, 50));
    expected =
        "|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239]|[239][end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // download a file in subdir

    // try to download a non-existent dataset
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/"
            + tDatasetID
            + "/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/" + tDatasetID + "/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/"
            + tDatasetID
            + "/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
  }

  /** ErddapTests */

  /**
   * This is used by Bob to do simple tests of Search. This requires a running local ERDDAP with
   * erdMHchla8day and rMHchla8day (among others which will be not matched). This can be used with
   * searchEngine=original or lucene.
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
    String2.log(
        "\n*** Erddap.testSearch\n"
            + "This assumes localhost ERDDAP is running with erdMHchla8day and rMHchla8day (among others which will be not matched).");
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
      Test.ensureEqual(
          count, 4, "results=\n" + results + "i=" + i); // one in help, plus one per dataset

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
    expected2 =
        "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: Your query produced no matching results. Check the spelling of the word(s) you searched for.\";\n"
            + "})";
    Test.ensureTrue(
        results.indexOf(expected2) >= 0, "results=\n" + String2.annotatedString(results));
  }

  /** EDDGridCopy */
  /** This tests the /files/ "files" system. This requires testGridCopy in the localhost ERDDAP. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGridFiles() throws Throwable {
    // String2.log("\n*** EDDGridCopy.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/.csv");
    expected =
        "Name,lastModTime,Size,Description\n"
            + //
            "subfolder/,NaN,NaN,\n"
            + //
            (results.length() > 120
                ? "erdQSwind1day_20080101_03.nc.gz,lastModTime,10478645,\n"
                : "")
            + //
            "erdQSwind1day_20080104_07.nc,lastModTime,49790172,\n";
    results = results.replaceAll(",.............,", ",lastModTime,");
    if (results.length() < expected.length()) {
      throw new Exception(results);
    }
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/");
    Test.ensureTrue(
        results.indexOf("erdQSwind1day&#x5f;20080104&#x5f;07&#x2e;nc") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf(">49790172<") > 0, "results=\n" + results);

    // download a file in root
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline(
                    "http://localhost:"
                        + PORT
                        + "/erddap/files/testGriddedNcFiles/erdQSwind1day_20080104_07.nc")
                .substring(0, 50));
    expected =
        "CDF[1][0][0][0][0][0][0][0][10]\n"
            + "[0][0][0][4][0][0][0][4]time[0][0][0][4][0][0][0][8]altitude[0][0][0][1][0][0][0][8]la[end]";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/testGriddedNcFiles/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/testGriddedNcFiles/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/testGriddedNcFiles/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** EDDGridLon0360 */

  /**
   * This tests generateDatasetsXmlFromErddapCatalog.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGenerateDatasetsXmlFromErddapCatalog0360() throws Throwable {

    // String2.log("\n*** EDDGridLon0360.testGenerateDatasetsXmlFromErddapCatalog()
    // ***\n");
    // testVerboseOn();
    int language = 0;
    String url =
        "http://localhost:" + PORT + "/erddap/"; // purposefully http:// to test if ERDDAP will
    // promote
    // to
    // https://
    // erdMH1's are -179 to 179, so will be in results
    // others are ~0 to 360 so won't be in results
    // -180 to 180 -180.0 to -110
    String regex = "(erdMH1chlamday|erdVHNchlamday|erdMWchlamday|erdMHsstnmday)";

    String results = EDDGridLon0360.generateDatasetsXmlFromErddapCatalog(url, regex) + "\n";

    String expected =
        "<dataset type=\"EDDGridLon0360\" datasetID=\"erdMH1chlamday_Lon0360\" active=\"true\">\n"
            + "    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMH1chlamday_Lon0360Child\">\n"
            + "        <!-- Chlorophyll-a, Aqua MODIS, NPP, L3SMI, Global, 4km, Science Quality, 2003-present (Monthly Composite)\n"
            + "             minLon=-179.9792 maxLon=179.9792 -->\n"
            + "        <sourceUrl>http://localhost:8080/erddap/griddap/erdMH1chlamday</sourceUrl>\n"
            + "    </dataset>\n"
            + "</dataset>\n"
            + "\n"
            + "<dataset type=\"EDDGridLon0360\" datasetID=\"erdVHNchlamday_Lon0360\" active=\"true\">\n"
            + "    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdVHNchlamday_Lon0360Child\">\n"
            + "        <!-- Chlorophyll a, North Pacific, NOAA VIIRS, 750m resolution, 2015-present (Monthly Composite)\n"
            + "             minLon=-180.03375 maxLon=-110.00625 -->\n"
            + "        <sourceUrl>http://localhost:8080/erddap/griddap/erdVHNchlamday</sourceUrl>\n"
            + "    </dataset>\n"
            + "</dataset>\n"
            + "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {"-verbose", "EDDGridLon0360FromErddapCatalog", url, regex},
                false); // doIt loop?
    Test.ensureEqual(
        gdxResults,
        results,
        "Unexpected results from GenerateDatasetsXml.doIt. results=\n" + results);
  }

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

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_LT0_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("time = [0-9]+", "time = ddd");
    expected =
        "Dataset {\n"
            + "  Float64 time[time = ddd];\n"
            + "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 11985];\n"
            + "  Float64 longitude[longitude = 9333];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 chla[time = ddd][altitude = 1][latitude = 11985][longitude = 9333];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = ddd];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 11985];\n"
            + "      Float64 longitude[longitude = 9333];\n"
            + "  } chla;\n"
            + "} test_erdVHNchlamday_Lon0360;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_LT0_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    expected =
        "longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range 180.00375, 249.99375;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 249.99375;\n"
            + "    Float64 geospatial_lon_min 180.00375;\n"
            + "    Float64 geospatial_lon_resolution 0.007500000000000001;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting 180.00375;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // lon values
    userDapQuery = "longitude[0:1000:9332]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_LT0_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, 180.00375, 187.50375, 195.00375, 202.50375, 210.00375, 217.50375, 225.00375, 232.50375, 240.00375, 247.50375, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire lon range
    userDapQuery =
        "chla[(2015-03-16)][][(89.77152):5500:(-0.10875)][(180.00375):4500:(249.99375)]"; // [-179.99625:4500:-110.00625]
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_LT0_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // from same request from cw erddap: erdVHNchlamday
        // http://localhost:" + PORT +
        // "/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(89.77125):5500:(-0.10875)%5D%5B(-179.99625):4500:(-110.00625)%5D
        // but add 360 to the lon values to get the values below
        "time,altitude,latitude,longitude,chla\n"
            + "UTC,m,degrees_north,degrees_east,mg m^-3\n"
            + "2015-03-16T00:00:00Z,0.0,89.77125,180.00375,NaN\n"
            + "2015-03-16T00:00:00Z,0.0,89.77125,213.75375,NaN\n"
            + "2015-03-16T00:00:00Z,0.0,89.77125,247.50375,NaN\n"
            + "2015-03-16T00:00:00Z,0.0,48.52125,180.00375,NaN\n"
            + "2015-03-16T00:00:00Z,0.0,48.52125,213.75375,0.29340988\n"
            + "2015-03-16T00:00:00Z,0.0,48.52125,247.50375,NaN\n"
            + "2015-03-16T00:00:00Z,0.0,7.27125,180.00375,NaN\n"
            + "2015-03-16T00:00:00Z,0.0,7.27125,213.75375,0.08209898\n"
            + "2015-03-16T00:00:00Z,0.0,7.27125,247.50375,0.12582141\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon subset
    userDapQuery = "chla[(2015-03-16)][][(10):500:(0)][(200):2000:(240)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_LT0_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // http://localhost:" + PORT +
        // "/erddap/griddap/erdVHNchlamday.htmlTable?chla%5B(2015-03-16):1:(2015-03-16)%5D%5B(0.0):1:(0.0)%5D%5B(10):500:(0)%5D%5B(-160):2000:(-120)%5D
        // but add 360 to get lon values below
        "time,altitude,latitude,longitude,chla\n"
            + "UTC,m,degrees_north,degrees_east,mg m^-3\n"
            + "2015-03-16T00:00:00Z,0.0,10.00125,199.99875,0.03348998\n"
            + "2015-03-16T00:00:00Z,0.0,10.00125,214.99875,0.06315609\n"
            + "2015-03-16T00:00:00Z,0.0,10.00125,229.99875,0.06712352\n"
            + "2015-03-16T00:00:00Z,0.0,6.25125,199.99875,0.089674756\n"
            + "2015-03-16T00:00:00Z,0.0,6.25125,214.99875,0.12793668\n"
            + "2015-03-16T00:00:00Z,0.0,6.25125,229.99875,0.15159287\n"
            + "2015-03-16T00:00:00Z,0.0,2.50125,199.99875,0.16047283\n"
            + "2015-03-16T00:00:00Z,0.0,2.50125,214.99875,0.15874723\n"
            + "2015-03-16T00:00:00Z,0.0,2.50125,229.99875,0.12635218\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "chla[(2015-03-16)][][][]&.land=under";
    String baseName = eddGrid.className() + "_NEPacificNowLon0360";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // test of /files/ system for fromErddap in local host dataset
    String2.log(
        "\n*** The following test requires test_erdVHNchlamday_Lon0360 in the localhost ERDDAP.");
    results =
        SSR.getUrlResponseStringUnchanged(
            "http://localhost:" + PORT + "/erddap/files/test_erdVHNchlamday_Lon0360/");
    expected = "VHN2015060&#x5f;2015090&#x5f;chla&#x2e;nc";
    Test.ensureTrue(results.indexOf(expected) >= 0, "results=\n" + results);

    // String2.log("\n*** EDDGridLon0360.testLT0() finished.");
  }

  /** EDDTableCopy */

  /** The basic tests of this class (erdGlobecBottle). */
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
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_Entire", ".das");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected = // see OpendapHelper.EOL for comments
        "Attributes {\n"
            + " s {\n"
            + "  cruise_id {\n"
            + "    String cf_role \"trajectory_id\";\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Cruise ID\";\n"
            + "  }\n"
            + "  ship {\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Ship\";\n"
            + "  }\n"
            + "  cast {\n"
            + "    Int16 _FillValue 32767;\n"
            + "    Int16 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 140.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Identifier\";\n"
            + "    String long_name \"Cast Number\";\n"
            + "    Int16 missing_value 32767;\n"
            + "  }\n"
            + "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float32 _FillValue 327.67;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    String axis \"X\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    Float32 missing_value 327.67;\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n"
            + "  latitude {\n"
            + "    String _CoordinateAxisType \"Lat\";\n"
            + "    Float32 _FillValue 327.67;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    String axis \"Y\";\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Latitude\";\n"
            + "    Float32 missing_value 327.67;\n"
            + "    String standard_name \"latitude\";\n"
            + "    String units \"degrees_north\";\n"
            + "  }\n"
            + "  time {\n"
            + "    String _CoordinateAxisType \"Time\";\n"
            + "    Float64 actual_range MIN, MAX;\n"
            + "    String axis \"T\";\n"
            + "    String cf_role \"profile_id\";\n"
            + "    String ioos_category \"Time\";\n"
            + "    String long_name \"Time\";\n"
            + "    String standard_name \"time\";\n"
            + "    String time_origin \"01-JAN-1970 00:00:00\";\n"
            + "    String units \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "  }\n"
            + "  bottle_posn {\n"
            + "    String _CoordinateAxisType \"Height\";\n"
            + "    Byte _FillValue 127;\n"
            + "    String _Unsigned \"false\";\n"
            + "    Int16 actual_range MIN, MAX;\n"
            + "    String axis \"Z\";\n"
            + "    Float64 colorBarMaximum 12.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Bottle Number\";\n"
            + "    Byte missing_value -128;\n"
            + "  }\n"
            + "  chl_a_total {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Chlorophyll-a\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  chl_a_10um {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Chlorophyll-a after passing 10um screen\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"concentration_of_chlorophyll_in_sea_water\";\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  phaeo_total {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Total Phaeopigments\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  phaeo_10um {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 30.0;\n"
            + "    Float64 colorBarMinimum 0.03;\n"
            + "    String colorBarScale \"Log\";\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Phaeopigments 10um\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"ug L-1\";\n"
            + "  }\n"
            + "  sal00 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical Salinity from T0 and C0 Sensors\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "  }\n"
            + "  sal11 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 37.0;\n"
            + "    Float64 colorBarMinimum 32.0;\n"
            + "    String ioos_category \"Salinity\";\n"
            + "    String long_name \"Practical Salinity from T1 and C1 Sensors\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_practical_salinity\";\n"
            + "    String units \"PSU\";\n"
            + "  }\n"
            + "  temperature0 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature from T0 Sensor\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  temperature1 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 32.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Temperature\";\n"
            + "    String long_name \"Sea Water Temperature from T1 Sensor\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"sea_water_temperature\";\n"
            + "    String units \"degree_C\";\n"
            + "  }\n"
            + "  fluor_v {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Fluorescence Voltage\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"volts\";\n"
            + "  }\n"
            + "  xmiss_v {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Optical Properties\";\n"
            + "    String long_name \"Transmissivity Voltage\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"volts\";\n"
            + "  }\n"
            + "  PO4 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 4.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Phosphate\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_phosphate_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  N_N {\n"
            + "    Float32 _FillValue -99.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Nitrate plus Nitrite\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  NO3 {\n"
            + "    Float32 _FillValue -99.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Nitrate\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_nitrate_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  Si {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 50.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Silicate\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_silicate_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  NO2 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 1.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Nitrite\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_nitrite_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  NH4 {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 5.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved Nutrients\";\n"
            + "    String long_name \"Ammonium\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"mole_concentration_of_ammonium_in_sea_water\";\n"
            + "    String units \"micromoles L-1\";\n"
            + "  }\n"
            + "  oxygen {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 10.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Dissolved O2\";\n"
            + "    String long_name \"Oxygen\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String standard_name \"volume_fraction_of_oxygen_in_sea_water\";\n"
            + "    String units \"mL L-1\";\n"
            + "  }\n"
            + "  par {\n"
            + "    Float32 _FillValue -9999999.0;\n"
            + "    Float32 actual_range MIN, MAX;\n"
            + "    Float64 colorBarMaximum 3.0;\n"
            + "    Float64 colorBarMinimum 0.0;\n"
            + "    String ioos_category \"Ocean Color\";\n"
            + "    String long_name \"Photosynthetically Active Radiation\";\n"
            + "    Float32 missing_value -9999.0;\n"
            + "    String units \"volts\";\n"
            + "  }\n"
            + " }\n"
            + "  NC_GLOBAL {\n"
            + "    String cdm_altitude_proxy \"bottle_posn\";\n"
            + "    String cdm_data_type \"TrajectoryProfile\";\n"
            + "    String cdm_profile_variables \"cast, longitude, latitude, time\";\n"
            + "    String cdm_trajectory_variables \"cruise_id, ship\";\n"
            + "    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "    Float64 Easternmost_Easting -124.1;\n"
            + "    String featureType \"TrajectoryProfile\";\n"
            + "    Float64 geospatial_lat_max 44.65;\n"
            + "    Float64 geospatial_lat_min 41.9;\n"
            + "    String geospatial_lat_units \"degrees_north\";\n"
            + "    Float64 geospatial_lon_max -124.1;\n"
            + "    Float64 geospatial_lon_min -126.2;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    // " String history \"" + today + " 2012-07-29T19:11:09Z (local files; contact
    // erd.data@noaa.gov)\n"; //date is from last created file, so varies sometimes
    // today + "
    // http://localhost:" + PORT + "/erddap/tabledap/erdGlobecBottle.das"; //\n"
    // +
    // today + " https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle\n" +
    // today + "
    // http://localhost:" + PORT + "/erddap/tabledap/rGlobecBottle.das\";\n" +
    expected2 =
        "    String infoUrl \"https://en.wikipedia.org/wiki/Global_Ocean_Ecosystem_Dynamics\";\n"
            + "    String institution \"GLOBEC\";\n"
            + "    String keywords \"10um, active, after, ammonia, ammonium, attenuation, biosphere, bottle, cast, chemistry, chlorophyll, chlorophyll-a, color, concentration, concentration_of_chlorophyll_in_sea_water, cruise, data, density, dissolved, dissolved nutrients, dissolved o2, Earth Science > Biosphere > Vegetation > Photosynthetically Active Radiation, Earth Science > Oceans > Ocean Chemistry > Ammonia, Earth Science > Oceans > Ocean Chemistry > Chlorophyll, Earth Science > Oceans > Ocean Chemistry > Nitrate, Earth Science > Oceans > Ocean Chemistry > Nitrite, Earth Science > Oceans > Ocean Chemistry > Nitrogen, Earth Science > Oceans > Ocean Chemistry > Oxygen, Earth Science > Oceans > Ocean Chemistry > Phosphate, Earth Science > Oceans > Ocean Chemistry > Pigments, Earth Science > Oceans > Ocean Chemistry > Silicate, Earth Science > Oceans > Ocean Optics > Attenuation/Transmission, Earth Science > Oceans > Ocean Temperature > Water Temperature, Earth Science > Oceans > Salinity/Density > Salinity, fluorescence, fraction, from, globec, identifier, mass, mole, mole_concentration_of_ammonium_in_sea_water, mole_concentration_of_nitrate_in_sea_water, mole_concentration_of_nitrite_in_sea_water, mole_concentration_of_phosphate_in_sea_water, mole_concentration_of_silicate_in_sea_water, moles, moles_of_nitrate_and_nitrite_per_unit_mass_in_sea_water, n02, nep, nh4, nitrate, nitrite, nitrogen, no3, number, nutrients, o2, ocean, ocean color, oceans, optical, optical properties, optics, oxygen, passing, per, phaeopigments, phosphate, photosynthetically, pigments, plus, po4, properties, radiation, rosette, salinity, screen, sea, sea_water_practical_salinity, sea_water_temperature, seawater, sensor, sensors, ship, silicate, temperature, time, total, transmission, transmissivity, unit, vegetation, voltage, volume, volume_fraction_of_oxygen_in_sea_water, water\";\n"
            + "    String keywords_vocabulary \"GCMD Science Keywords\";\n"
            + "    String license \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "    Float64 Northernmost_Northing 44.65;\n"
            + "    String sourceUrl \"(local files; contact erd.data@noaa.gov)\";\n"
            + "    Float64 Southernmost_Northing 41.9;\n"
            + "    String standard_name_vocabulary \"CF Standard Name Table v70\";\n"
            + "    String subsetVariables \"cruise_id, ship, cast, longitude, latitude, time\";\n"
            + "    String summary \"GLOBEC (GLOBal Ocean ECosystems Dynamics) NEP (Northeast Pacific)\n"
            + "Rosette Bottle Data from New Horizon Cruise (NH0207: 1-19 August 2002).\n"
            + "Notes:\n"
            + "Physical data processed by Jane Fleischbein (OSU).\n"
            + "Chlorophyll readings done by Leah Feinberg (OSU).\n"
            + "Nutrient analysis done by Burke Hales (OSU).\n"
            + "Sal00 - salinity calculated from primary sensors (C0,T0).\n"
            + "Sal11 - salinity calculated from secondary sensors (C1,T1).\n"
            + "secondary sensor pair was used in final processing of CTD data for\n"
            + "most stations because the primary had more noise and spikes. The\n"
            + "primary pair were used for cast #9, 24, 48, 111 and 150 due to\n"
            + "multiple spikes or offsets in the secondary pair.\n"
            + "Nutrient samples were collected from most bottles; all nutrient data\n"
            + "developed from samples frozen during the cruise and analyzed ashore;\n"
            + "data developed by Burke Hales (OSU).\n"
            + "Operation Detection Limits for Nutrient Concentrations\n"
            + "Nutrient  Range         Mean    Variable         Units\n"
            + "PO4       0.003-0.004   0.004   Phosphate        micromoles per liter\n"
            + "N+N       0.04-0.08     0.06    Nitrate+Nitrite  micromoles per liter\n"
            + "Si        0.13-0.24     0.16    Silicate         micromoles per liter\n"
            + "NO2       0.003-0.004   0.003   Nitrite          micromoles per liter\n"
            + "Dates and Times are UTC.\n"
            + "\n"
            + "For more information, see https://www.bco-dmo.org/dataset/2452\n"
            + "\n"
            + "Inquiries about how to access this data should be directed to\n"
            + "Dr. Hal Batchelder (hbatchelder@coas.oregonstate.edu).\";\n"
            + "    String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ\";\n"
            + "    String time_coverage_start \"2002-05-30T03:21:00Z\";\n"
            + "    String title \"GLOBEC NEP Rosette Bottle Data (2002)\";\n"
            + "    Float64 Westernmost_Easting -126.2;\n"
            + "  }\n"
            + "}\n";

    results =
        results.replaceAll("Byte actual_range [0-9]+, [0-9]+;", "Int16 actual_range MIN, MAX;");
    results =
        results.replaceAll("Int16 actual_range [0-9]+, [0-9]+;", "Int16 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float32 actual_range -?[0-9]+.[0-9]+, -?[0-9]+.[0-9]+;",
            "Float32 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "Float64 actual_range -?[0-9].[0-9]+e[+][0-9], -?[0-9].[0-9]+e[+][0-9];",
            "Float64 actual_range MIN, MAX;");
    results =
        results.replaceAll(
            "String time_coverage_end \\\"....-..-..T..:..:..Z",
            "String time_coverage_end \"yyyy-MM-ddThh:mm:ssZ");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    tPo = results.indexOf("    String infoUrl ");
    Test.ensureEqual(results.substring(tPo), expected2, "\nresults=\n" + results);

    // *** test getting dds for entire dataset
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_Entire", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    String cruise_id;\n"
            + "    String ship;\n"
            + "    Int16 cast;\n"
            + "    Float32 longitude;\n"
            + "    Float32 latitude;\n"
            + "    Float64 time;\n"
            + "    Byte bottle_posn;\n"
            + "    Float32 chl_a_total;\n"
            + "    Float32 chl_a_10um;\n"
            + "    Float32 phaeo_total;\n"
            + "    Float32 phaeo_10um;\n"
            + "    Float32 sal00;\n"
            + "    Float32 sal11;\n"
            + "    Float32 temperature0;\n"
            + "    Float32 temperature1;\n"
            + "    Float32 fluor_v;\n"
            + "    Float32 xmiss_v;\n"
            + "    Float32 PO4;\n"
            + "    Float32 N_N;\n"
            + "    Float32 NO3;\n"
            + "    Float32 Si;\n"
            + "    Float32 NO2;\n"
            + "    Float32 NH4;\n"
            + "    Float32 oxygen;\n"
            + "    Float32 par;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // *** test DAP data access form
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, "", tDir, edd.className() + "_Entire", ".html");
    results = File2.directReadFromUtf8File(tDir + tName);
    expected = "<option>.png - View a standard, medium-sized .png image file with a graph or map.";
    expected2 = "    String _CoordinateAxisType &quot;Lon&quot;;";
    Test.ensureTrue(results.indexOf(expected) > 0, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    // Test.displayInBrowser("file://" + tDir + tName);

    // *** test make data files
    String2.log("\n****************** EDDTableCopy.test make DATA FILES\n");

    // .asc
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_Data", ".asc");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 longitude;\n"
            + "    Float32 NO3;\n"
            + "    Float64 time;\n"
            + "    String ship;\n"
            + "  } s;\n"
            + "} s;\n"
            + "---------------------------------------------\n"
            + "s.longitude, s.NO3, s.time, s.ship\n"
            + "-124.4, 35.7, 1.02833814E9, \"New_Horizon\"\n";
    expected2 =
        "-124.8, -9999.0, 1.02835902E9, \"New_Horizon\"\n"; // row with missing value has source
    // missing
    // value
    expected3 = "-124.57, 19.31, 1.02939792E9, \"New_Horizon\"\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(
        results.indexOf(expected3) > 0,
        "\nresults=\n" + results); // last row in erdGlobedBottle, not
    // last
    // here

    // .csv
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_Data", ".csv");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "longitude,NO3,time,ship\n"
            + "degrees_east,micromoles L-1,UTC,\n"
            + "-124.4,35.7,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,35.48,2002-08-03T01:29:00Z,New_Horizon\n"
            + "-124.4,31.61,2002-08-03T01:29:00Z,New_Horizon\n";
    expected2 =
        "-124.8,NaN,2002-08-03T07:17:00Z,New_Horizon\n"; // row with missing value has source
    // missing value
    expected3 = "-124.57,19.31,2002-08-15T07:52:00Z,New_Horizon\n"; // last row
    Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
    Test.ensureTrue(results.indexOf(expected2) > 0, "\nresults=\n" + results);
    Test.ensureTrue(
        results.indexOf(expected3) > 0,
        "\nresults=\n" + results); // last row in erdGlobedBottle, not
    // last
    // here

    // .dds
    tName =
        edd.makeNewFileForDapQuery(
            language, null, null, userDapQuery, tDir, edd.className() + "_Data", ".dds");
    results = File2.directReadFrom88591File(tDir + tName);
    // String2.log(results);
    expected =
        "Dataset {\n"
            + "  Sequence {\n"
            + "    Float32 longitude;\n"
            + "    Float32 NO3;\n"
            + "    Float64 time;\n"
            + "    String ship;\n"
            + "  } s;\n"
            + "} s;\n";
    Test.ensureEqual(results, expected, "\nresults=\n" + results);

    // .dods
    // tName = edd.makeNewFileForDapQuery(language, null, null, userDapQuery, tDir,
    // edd.className() + "_Data", ".dods");
    // Test.displayInBrowser("file://" + tDir + tName);
    String2.log("\ndo .dods test");
    String tUrl =
        EDStatic.erddapUrl
            + // in tests, always use non-https url
            "/tabledap/"
            + edd.datasetID();
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
    Test.ensureEqual(
        tTable.globalAttributes().getString("title"), "GLOBEC NEP Rosette Bottle Data (2002)", "");
    Test.ensureEqual(tTable.columnAttributes(2).getString("units"), EDV.TIME_UNITS, "");
    Test.ensureEqual(
        tTable.getColumnNames(), new String[] {"longitude", "NO3", "time", "ship"}, "");
    Test.ensureEqual(tTable.getFloatData(0, 0), -124.4f, "");
    Test.ensureEqual(tTable.getFloatData(1, 0), 35.7f, "");
    Test.ensureEqual(tTable.getDoubleData(2, 0), 1.02833814E9, "");
    Test.ensureEqual(tTable.getStringData(3, 0), "New_Horizon", "");
    String2.log("  .dods test succeeded");

    // test .png
    String baseName = edd.className() + "_GraphM";
    tName =
        edd.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + tDir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");
  } // end of testBasic

  /** This tests the /files/ "files" system. This requires testTableCopy in the localhost ERDDAP. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testTableCopyFiles() throws Throwable {

    String2.log("\n*** EDDTableCopy.testFiles()\n");
    String tDir = EDStatic.fullTestCacheDirectory;
    String dapQuery, tName, start, query, results, expected;
    int po;

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/testTableCopy/.csv");
    expected = "Name,Last modified,Size,Description\n" + "nh0207/,NaN,NaN,\n" + "w0205/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/testTableCopy/");
    Test.ensureTrue(results.indexOf("nh0207&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("nh0207/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("w0205&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("w0205/") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/.csv");
    expected =
        "Name,lastModTime,Size,Description\n"
            + "1.nc,lastModTime,14384,\n"
            + "10.nc,lastModTime,15040,\n"
            + "100.nc,lastModTime,14712,\n";
    results = results.replaceAll(",.............,", ",lastModTime,");
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

    // download a file in root

    // download a file in subdir
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline(
                    "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/100.nc")
                .substring(0, 50));
    expected =
        "CDF[1][0][0][0][0][0][0][0][10]\n"
            + "[0][0][0][3][0][0][0][3]row[0][0][0][0][6][0][0][0][16]cruise_id_strlen[0][0][end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/testTableCopy/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/testTableCopy/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/testTableCopy/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existant subdir
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/testTableCopy/nh0207/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/testTableCopy/nh0207/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** EDDGridLongPM180 */

  /**
   * This tests generateDatasetsXmlFromErddapCatalog.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testGenerateDatasetsXmlFromErddapCatalog() throws Throwable {
    // String2.log("\n*** EDDGridLonPM180.testGenerateDatasetsXmlFromErddapCatalog()
    // ***\n");
    // testVerboseOn();
    String url =
        "http://localhost:"
            + PORT
            + "/erddap/"; // purposefully http:// to test if ERDDAP will promote
    // to https://
    // erdMH1chlamday is -179 to 179, so nothing to do, so won't be in results
    // others are good, different test cases
    String regex =
        "(erdMH1chlamday|erdPHsstamday|hawaii_d90f_20ee_c4cb|testPM180LonValidMinMax|erdMWchlamday|erdMHsstnmday|erdMBsstdmday|erdRWdhws1day)";

    String results = EDDGridLonPM180.generateDatasetsXmlFromErddapCatalog(url, regex) + "\n";

    String expected =
        "<dataset type=\"EDDGridLonPM180\" datasetID=\"erdMBsstdmday_LonPM180\" active=\"true\">\n"
            + "    <dataset type=\"EDDGridFromErddap\" datasetID=\"erdMBsstdmday_LonPM180Child\">\n"
            + "        <!-- SST, Aqua MODIS, NPP, 0.025 degrees, Pacific Ocean, Daytime, 2006-present (Monthly Composite)\n"
            + "             minLon=120.0 maxLon=320.0 -->\n"
            + "        <sourceUrl>http://localhost:"
            + PORT
            + "/erddap/griddap/erdMBsstdmday</sourceUrl>\n"
            + "    </dataset>\n"
            + "</dataset>\n"
            + "\n"
            + "<dataset type=\"EDDGridLonPM180\" datasetID=\"hawaii_d90f_20ee_c4cb_LonPM180\" active=\"true\">\n"
            + //
            "    <dataset type=\"EDDGridFromErddap\" datasetID=\"hawaii_d90f_20ee_c4cb_LonPM180Child\">\n"
            + //
            "        <!-- SODA - POP 2.2.4 Monthly Means, 1871-2010 (At Depths)\n"
            + //
            "             minLon=0.25 maxLon=359.75 -->\n"
            + //
            "        <sourceUrl>http://localhost:8080/erddap/griddap/hawaii_d90f_20ee_c4cb</sourceUrl>\n"
            + //
            "    </dataset>\n"
            + //
            "</dataset>\n"
            + //
            // "\n" +
            // "<dataset type=\"EDDGridLonPM180\" datasetID=\"erdMHsstnmday_LonPM180\"
            // active=\"true\">\n" +
            // " <dataset type=\"EDDGridFromErddap\"
            // datasetID=\"erdMHsstnmday_LonPM180Child\">\n" +
            // " <!-- SST, Aqua MODIS, NPP, Nighttime (11 microns), 2002-2013, DEPRECATED
            // OLDER VERSION (Monthly Composite)\n"
            // +
            // " minLon=0.0 maxLon=360.0 -->\n" +
            // "
            // <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdMHsstnmday</sourceUrl>\n"
            // +
            // " </dataset>\n" +
            // "</dataset>\n" +
            // "\n" +
            // "<dataset type=\"EDDGridLonPM180\" datasetID=\"erdMWchlamday_LonPM180\"
            // active=\"true\">\n" +
            // " <dataset type=\"EDDGridFromErddap\"
            // datasetID=\"erdMWchlamday_LonPM180Child\">\n" +
            // " <!-- Chlorophyll-a, Aqua MODIS, NPP, 0.0125°, West US, EXPERIMENTAL,
            // 2002-present (Monthly Composite)\n"
            // +
            // " minLon=205.0 maxLon=255.0 -->\n" +
            // " <sourceUrl>http://localhost:" + PORT +
            // "/erddap/griddap/erdMWchlamday</sourceUrl>\n" +
            // " </dataset>\n" +
            // "</dataset>\n" +
            // "\n" +
            // "<dataset type=\"EDDGridLonPM180\" datasetID=\"erdRWdhws1day_LonPM180\"
            // active=\"true\">\n" +
            // " <dataset type=\"EDDGridFromErddap\"
            // datasetID=\"erdRWdhws1day_LonPM180Child\">\n" +
            // " <!-- Coral Reef Watch, Degree Heating Weeks, 50 km, 2000-2011\n" +
            // " minLon=0.25 maxLon=359.75 -->\n" +
            // "
            // <sourceUrl>https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdRWdhws1day</sourceUrl>\n"
            // +
            // " </dataset>\n" +
            // "</dataset>\n" +
            "\n\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // GenerateDatasetsXml
    String gdxResults =
        new GenerateDatasetsXml()
            .doIt(
                new String[] {"-verbose", "EDDGridLonPM180FromErddapCatalog", url, regex},
                false); // doIt loop?
    Test.ensureEqual(
        gdxResults,
        results,
        "Unexpected results from GenerateDatasetsXml.doIt. results=\n" + results);
  }

  /**
   * This tests a dataset that is initially 120to320.
   *
   * @throws Throwable if trouble
   */
  @org.junit.jupiter.api.Test
  @TagImageComparison
  @TagJetty
  void test120to320() throws Throwable {
    // String2.log("\n****************** EDDGridLonPM180.test120to320()
    // *****************\n");
    // testVerboseOn();
    int language = 0;
    // boolean oDebugMode = debugMode;
    // debugMode = true;
    String name, tName, userDapQuery, results, expected, error;
    int po;
    String dir = EDStatic.fullTestCacheDirectory;
    EDDGrid eddGrid = null;

    // test notApplicable (dataset maxLon already <180)
    try {
      eddGrid = (EDDGrid) EDDTestDataset.getnotApplicable_LonPM180();
    } catch (Throwable t) {
      String msg = MustBe.throwableToString(t);
      if (msg.indexOf(
              "Error in EDDGridLonPM180(notApplicable_LonPM180) constructor:\n"
                  + "The child longitude axis has no values >180 (max=179.9792)!")
          < 0) throw t;
    }
    if (EDStatic.useSaxParser) {
      Test.ensureEqual(eddGrid, null, "Dataset should be null from exception during construction.");
    }

    // test120 to 320
    eddGrid = (EDDGrid) EDDTestDataset.geterdMBsstdmday_LonPM180();

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_120to320_Entire", ".dds");
    results = File2.directReadFrom88591File(dir + tName);
    results = results.replaceAll("time = [0-9]+", "time = ddd");
    expected =
        "Dataset {\n"
            + "  Float64 time[time = ddd];\n"
            + "  Float64 altitude[altitude = 1];\n"
            + "  Float64 latitude[latitude = 4401];\n"
            + "  Float64 longitude[longitude = 14400];\n"
            + "  GRID {\n"
            + "    ARRAY:\n"
            + "      Float32 sst[time = ddd][altitude = 1][latitude = 4401][longitude = 14400];\n"
            + "    MAPS:\n"
            + "      Float64 time[time = ddd];\n"
            + "      Float64 altitude[altitude = 1];\n"
            + "      Float64 latitude[latitude = 4401];\n"
            + "      Float64 longitude[longitude = 14400];\n"
            + "  } sst;\n"
            + "} erdMBsstdmday_LonPM180;\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, "", dir, eddGrid.className() + "_120to320_Entire", ".das");
    results = File2.directReadFrom88591File(dir + tName);
    String2.log(results);
    expected =
        "  longitude {\n"
            + "    String _CoordinateAxisType \"Lon\";\n"
            + "    Float64 actual_range -180.0, 179.975;\n"
            + "    String axis \"X\";\n"
            + "    String coordsys \"geographic\";\n"
            + "    Int32 fraction_digits 3;\n"
            + "    String ioos_category \"Location\";\n"
            + "    String long_name \"Longitude\";\n"
            + "    String point_spacing \"even\";\n"
            + "    String standard_name \"longitude\";\n"
            + "    String units \"degrees_east\";\n"
            + "  }\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected =
        "    Float64 geospatial_lon_max 179.975;\n"
            + "    Float64 geospatial_lon_min -180.0;\n"
            + "    Float64 geospatial_lon_resolution 0.025;\n"
            + "    String geospatial_lon_units \"degrees_east\";\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    expected = "    Float64 Westernmost_Easting -180.0;\n" + "  }\n" + "}\n";
    po = results.indexOf(expected.substring(0, 30));
    Test.ensureEqual(
        results.substring(po, po + expected.length()), expected, "results=\n" + results);

    // lon values
    // this tests correct jump across lon 0
    userDapQuery = "longitude[(-180):2057:(179.975)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_lon", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected =
        "longitude, degrees_east, -180.0, -128.575, -77.14999999999998, -25.725, 25.7, 77.125, 128.55, 179.975, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // lon values near 0
    userDapQuery = "longitude[(-.075):1:(.075)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            dir,
            eddGrid.className() + "_120to320_lonNear0",
            ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    results = String2.replaceAll(results, "\n", ", ");
    expected = "longitude, degrees_east, -0.075, -0.05, -0.025, 0.0, 0.025, 0.05, 0.075, ";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // entire new lon range
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-180):2057:(179.975)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        // erdMBsstdmday natively 120...320(-40)
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-180.0,25.8021\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-128.575,25.3075\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,77.125,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,128.55,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,179.975,25.9641\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-180.0,25.9868\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-128.575,25.3071\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,77.125,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,128.55,30.87\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,179.975,25.8123\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, left+insert+right sections
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(135)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_4", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-128.575,25.3075\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,77.125,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,128.55,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-128.575,25.3071\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,77.125,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,128.55,30.87\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just left
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(-70)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1L", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-128.575,25.3075\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-128.575,25.3071\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-77.14999999999998,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just left, 1 point
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):2057:(-120)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1Lb", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-128.575,25.3075\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-128.575,25.3071\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just insert //insert values are between -40 and 120
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-25.725):2057:(27)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1I", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,25.7,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just insert, 1 point
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-25.725):2057:(-20)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1Ib", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-25.725,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just right
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(128.55):2057:(179.98)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1R", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,128.55,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,179.975,25.9641\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,128.55,30.87\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,179.975,25.8123\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset: just right, 1 point
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(128.55):2057:(135)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_1Rb", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,128.55,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,128.55,30.87\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, left + insert
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-180):2057:(27)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_2", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-180.0,25.8021\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-128.575,25.3075\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-180.0,25.9868\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-128.575,25.3071\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-77.14999999999998,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-25.725,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,25.7,NaN\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, insert + right
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(25.7):2057:(179.98)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_3", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,77.125,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,128.55,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,179.975,25.9641\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,25.7,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,77.125,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,128.55,30.87\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,179.975,25.8123\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // subset of that, left + right (jump over insert)
    userDapQuery =
        "sst[(2008-03-15T12:00:00Z)][][(0):4:(0.1)][(-128.575):" + (2057 * 5) + ":(179.98)]";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language, null, null, userDapQuery, dir, eddGrid.className() + "_120to320_3b", ".csv");
    results = File2.directReadFrom88591File(dir + tName);
    // String2.log(results);
    expected =
        "time,altitude,latitude,longitude,sst\n"
            + //
            "UTC,m,degrees_north,degrees_east,degree_C\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,-128.575,25.3075\n"
            + //
            "2008-03-16T12:00:00Z,0.0,2.498002E-15,128.55,NaN\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,-128.575,25.3071\n"
            + //
            "2008-03-16T12:00:00Z,0.0,0.1,128.55,30.87\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // test image
    userDapQuery = "sst[(2008-03-15T12:00:00Z)][][][]&.land=under";
    String baseName = eddGrid.className() + "_test120to320";
    tName =
        eddGrid.makeNewFileForDapQuery(
            language,
            null,
            null,
            userDapQuery,
            Image2Tests.urlToAbsolutePath(Image2Tests.OBS_DIR),
            baseName,
            ".png");
    // Test.displayInBrowser("file://" + dir + tName);
    Image2Tests.testImagesIdentical(tName, baseName + ".png", baseName + "_diff.png");

    // String2.log("\n*** EDDGridLonPM180.test120to320 finished.");
    // debugMode = oDebugMode;
  }

  /** EDDGridFromNcFiles */

  /**
   * This test the speed of all types of responses. This test is in this class because the source
   * data is in a file, so it has reliable access speed. This gets a pretty big chunk of data.
   *
   * @param firstTest 0..
   * @param lastTest (inclusive) Any number greater than last available is interpreted as last
   *     available.
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
    for (int i = 0; i < 4; i++) Math2.gc("EDDGridFromNcFiles.testSpeed (between tests)", 5000);
    // boolean oReallyVerbose = reallyVerbose;
    // reallyVerbose = false;
    String outName;
    // 2017-10-13 I switched from getFile to curl
    // The advantage is: curl will detect if outputstream isn't being closed.
    // 2018-05-17 problems with curl, switched to SSR.downloadFile
    String baseRequest = "http://localhost:" + PORT + "/erddap/griddap/testGriddedNcFiles";
    String userDapQuery =
        "?"
            + SSR.minimalPercentEncode("y_wind[(2008-01-07T12:00:00Z)][0][][0:719]")
            + // 719
            // avoids
            // esriAsc
            // cross
            // lon=180
            "&.vec="; // avoid get cached response
    String baseName = "EDDGridFromNcFilesTestSpeed";
    String baseOut = EDStatic.fullTestCacheDirectory + baseName;
    ArrayList al;
    int timeOutSeconds = 120;
    String extensions[] =
        new String[] {
          ".asc",
          ".csv",
          ".csvp",
          ".csv0",
          ".das",
          ".dds",
          ".dods",
          ".esriAscii",
          ".graph",
          ".html",
          ".htmlTable", // .help not available at this level
          ".json",
          ".jsonlCSV",
          ".jsonlCSV1",
          ".jsonlKVP",
          ".mat",
          ".nc",
          ".ncHeader",
          ".nccsv",
          ".nccsvMetadata",
          ".ncoJson",
          ".odvTxt",
          ".timeGaps",
          ".tsv",
          ".tsvp",
          ".tsv0",
          ".xhtml",
          ".geotif",
          ".kml",
          ".smallPdf",
          ".pdf",
          ".largePdf",
          ".smallPng",
          ".png",
          ".largePng",
          ".transparentPng"
        };
    int expectedMs[] =
        new int[] {
          // 2017-10-13 I added 200 ms with change from getFile to curl
          // 2018-05-17 I adjusted (e.g., small files 200ms faster) with switch to
          // SSR.downloadFile
          // now Lenovo was Java 1.8/M4700 //was Java 1.6 times //was java 1.5 times
          550,
          1759,
          1635,
          1544, // 734, 6391, 6312, ? //1250, 9750, 9562, ?
          // why is esriAscii so slow??? was ~9000 for a long time. Then jumped to 23330.
          15,
          15,
          663,
          12392, // 15, 15, 109/156, 16875 //15, 15, 547, 18859
          40,
          25,
          477, // 63, 47, 2032, //93, 31, ...,
          1843,
          1568,
          1568,
          2203,
          666, // 6422, ., ., 203, //9621, ., ., 625,
          173,
          117, // 234, 250, //500, 500,
          3485,
          16,
          390,
          2446,
          13, // 9547, ? //13278, ?
          1411,
          1411,
          1411, // 6297, 6281, ?, //8766, 8844, ?,
          2204, // but really slow if hard drive is busy! //8625, //11469,
          500,
          20, // 656, 110, //687, 94, //Java 1.7 was 390r until change to new netcdf-Java
          266,
          976,
          1178, // 860, 2859, 3438, //2188, 4063, 3797, //small varies greatly
          131,
          209,
          459, // 438, 468, 1063, //438, 469, 1188, //small varies greatly
          720
        }; // 1703 //2359};
    int bytes[] =
        new int[] {
          5875592, 23734053, 23734063, 23733974, 6006, 303, 2085486, 4701074, 60787, 60196,
          11980027, 31827797, 28198736, 28198830, 54118736, 2085800, 2090600, 5285, 25961828, 5244,
          5877820, 26929890, 58, 23734053, 23734063, 23733974, 69372795, 523113, 3601, 478774,
          2189656, 2904880, 33930, 76777,
          277494, // small png flips between 26906 and 30764, updated to 33930
          335307
        };

    // warm up
    boolean tryToCompress = true;
    outName = baseOut + "Warmup.csvp.csv";
    SSR.downloadFile(
        baseRequest + ".csvp" + userDapQuery + Math2.random(1000), outName, tryToCompress);
    // was al = SSR.dosShell(baseRequest + ".csvp" + userDapQuery +
    // Math2.random(1000) +
    // " -o " + outName, timeOutSeconds);
    // String2.log(String2.toNewlineString(al.toArray()));

    outName = baseOut + "Warmup.png.png";
    SSR.downloadFile(
        baseRequest + ".png" + userDapQuery + Math2.random(1000), outName, tryToCompress);
    // al = SSR.dosShell(baseRequest + ".png" + userDapQuery + Math2.random(1000) +
    // " -o " + outName, timeOutSeconds);

    outName = baseOut + "Warmup.pdf.pdf";
    SSR.downloadFile(
        baseRequest + ".pdf" + userDapQuery + Math2.random(1000), outName, tryToCompress);
    // al = SSR.dosShell(baseRequest + ".pdf" + userDapQuery + Math2.random(1000) +
    // " -o " + outName, timeOutSeconds);

    lastTest = Math.min(lastTest, extensions.length - 1);
    for (int ext = firstTest; ext <= lastTest; ext++) {
      // String2.pressEnterToContinue("");
      // Math2.sleep(3000);
      String dotExt = extensions[ext];
      // try {
      String2.log("\n*** EDDGridFromNcFiles.testSpeed test#" + ext + ": " + dotExt + " speed\n");
      long time = 0, cLength = 0;
      int chance = 0;
      // for (int chance = 0; chance < 3; chance++) {
      Math2.gcAndWait("EDDGridFromNcFiles (between tests)"); // in a test
      time = System.currentTimeMillis();
      outName = baseOut + chance + dotExt;
      SSR.downloadFile(
          baseRequest + dotExt + userDapQuery + Math2.random(1000), outName, tryToCompress);
      // al = SSR.dosShell(baseRequest + dotExt + userDapQuery + Math2.random(1000) +
      // " -o " + outName, timeOutSeconds);

      time = System.currentTimeMillis() - time;
      cLength = File2.length(outName);
      String2.log(
          "\n*** EDDGridFromNcFiles.testSpeed test#"
              + ext
              + " chance#"
              + chance
              + ": "
              + dotExt
              + " done.\n  "
              + cLength
              + " bytes ("
              + bytes[ext]
              + ").  time="
              + time
              + "ms (expected="
              + expectedMs[ext]
              + ")\n");
      // Math2.sleep(3000);

      // if not too slow or too fast, break
      // if (time > 1.5 * Math.max(50, expectedMs[ext]) ||
      // time < (expectedMs[ext] <= 50 ? 0 : 0.5) * expectedMs[ext]) {
      // // give it another chance
      // } else {
      // break;
      // }
      // }

      // size test
      Test.ensureTrue(
          cLength > 0.9 * bytes[ext],
          "File shorter than expected.  observed="
              + cLength
              + " expected=~"
              + bytes[ext]
              + "\n"
              + outName);
      Test.ensureTrue(
          cLength < 1.1 * bytes[ext],
          "File longer than expected.  observed="
              + cLength
              + " expected=~"
              + bytes[ext]
              + "\n"
              + outName);

      // time test
      // TODO check performance in a better way
      // if (time > 1.5 * Math.max(50, expectedMs[ext]))
      // throw new SimpleException(
      // "Slower than expected. observed=" + time +
      // " expected=~" + expectedMs[ext] + " ms.");
      // if (expectedMs[ext] >= 50 && time < 0.5 * expectedMs[ext])
      // throw new SimpleException(
      // "Faster than expected! observed=" + time +
      // " expected=~" + expectedMs[ext] + " ms.");

      // display last image
      if (ext == extensions.length - 1) {
        File2.rename(outName, outName + ".png");
        // Test.displayInBrowser(outName + ".png"); // complicated to switch to
        // testImagesIdentical
      }

      // } catch (Exception e) {
      // String2.pressEnterToContinue(MustBe.throwableToString(e) +
      // "Unexpected error for Test#" + ext + ": " + dotExt + ".");
      // }
    }
    // reallyVerbose = oReallyVerbose;
  }

  /**
   * This tests the /files/ "files" system. This requires nceiPH53sstn1day in the local ERDDAP.
   *
   * <p>EDDGridFromNcFiles.testFiles() has more tests than any other testFiles().
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
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/.csv"));
    Test.ensureTrue(
        results.indexOf("Name,Last modified,Size,Description[10]") == 0, "results=\n" + results);
    Test.ensureTrue(
        results.indexOf(
                "nceiPH53sstn1day/,NaN,NaN,\"AVHRR Pathfinder Version 5.3 L3-Collated (L3C) SST, Global, 0.0417\\u00b0, 1981-present, Nighttime (1 Day Composite)\"[10]")
            > 0,
        "results=\n" + results);
    Test.ensureTrue(
        results.indexOf("testTableAscii/,NaN,NaN,The Title for testTableAscii[10]") > 0,
        "results=\n" + results);
    Test.ensureTrue(results.indexOf("documentation.html,") > 0, "results=\n" + results);

    // get /files/datasetID/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/.csv");
    expected =
        "Name,Last modified,Size,Description\n"
            + "1981/,NaN,NaN,\n"
            + "1994/,NaN,NaN,\n"
            + "2020/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/");
    Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1981/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

    // get /files/datasetID //missing trailing slash will be redirected
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day");
    Test.ensureTrue(results.indexOf("1981&#x2f;") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1981/") > 0, "results=\n" + results);
    Test.ensureTrue(results.indexOf("1994&#x2f;") > 0, "results=\n" + results);

    // get /files/datasetID/subdir/.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/1994/.csv");
    expected = "Name,Last modified,Size,Description\n" + "data/,NaN,NaN,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // get /files/datasetID/subdir/subdir.csv
    results =
        SSR.getUrlResponseStringNewline(
            "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/1994/data/.csv");
    results = results.replaceAll(",.............,", ",lastMod,");
    expected =
        "Name,lastMod,Size,Description\n"
            + "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc,lastMod,12484412,\n";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // download a file in root -- none available

    // download a file in subdir
    results =
        String2.annotatedString(
            SSR.getUrlResponseStringNewline(
                    "http://localhost:"
                        + PORT
                        + "/erddap/files/nceiPH53sstn1day/1994/data/"
                        + "19940913000030-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA09_G_1994256_night-v02.0-fv01.0.nc")
                .substring(0, 50));
    expected =
        "[137]HDF[10]\n"
            + "[26][10]\n"
            + "[2][8][8][0][0][0][0][0][0][0][0][0][255][255][255][255][255][255][255][255]<[127][190][0][0][0][0][0]0[0][0][0][0][0][0][0][199](*yOHD[end]";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query with // at start fails
    try {
      results = SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=400 for URL: http://localhost:"
            + PORT
            + "/erddap/files//.csv\n"
            + //
            "(<html>\n"
            + //
            "<head>\n"
            + //
            "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=ISO-8859-1\"/>\n"
            + //
            "<title>Error 400 Ambiguous URI empty segment</title>\n"
            + //
            "</head>\n"
            + //
            "<body>\n"
            + //
            "<h2>HTTP ERROR 400 Ambiguous URI empty segment</h2>\n"
            + //
            "<table>\n"
            + //
            "<tr><th>URI:</th><td>/badURI</td></tr>\n"
            + //
            "<tr><th>STATUS:</th><td>400</td></tr>\n"
            + //
            "<tr><th>MESSAGE:</th><td>Ambiguous URI empty segment</td></tr>\n"
            + //
            "</table>\n"
            + //
            "<hr/><a href=\"https://jetty.org/\">Powered by Jetty:// 12.0.14</a><hr/>\n"
            + //
            "\n"
            + //
            "</body>\n"
            + //
            "</html>)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query with // later fails
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day//.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=400 for URL: http://localhost:"
            + PORT
            + "/erddap/files/nceiPH53sstn1day//.csv\n"
            + //
            "(<html>\n"
            + //
            "<head>\n"
            + //
            "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=ISO-8859-1\"/>\n"
            + //
            "<title>Error 400 Ambiguous URI empty segment</title>\n"
            + //
            "</head>\n"
            + //
            "<body>\n"
            + //
            "<h2>HTTP ERROR 400 Ambiguous URI empty segment</h2>\n"
            + //
            "<table>\n"
            + //
            "<tr><th>URI:</th><td>/badURI</td></tr>\n"
            + //
            "<tr><th>STATUS:</th><td>400</td></tr>\n"
            + //
            "<tr><th>MESSAGE:</th><td>Ambiguous URI empty segment</td></tr>\n"
            + //
            "</table>\n"
            + //
            "<hr/><a href=\"https://jetty.org/\">Powered by Jetty:// 12.0.14</a><hr/>\n"
            + //
            "\n"
            + //
            "</body>\n"
            + //
            "</html>)";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // query with /../ fails
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/../");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=400 for URL: http://localhost:"
            + PORT
            + "/erddap/files/nceiPH53sstn1day/../\n"
            + "(Error {\n"
            + "    code=400;\n"
            + "    message=\"Bad Request: Query error: /../ is not allowed!\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent dataset
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent datasetID
    try {
      results =
          SSR.getUrlResponseStringNewline("http://localhost:" + PORT + "/erddap/files/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Currently unknown datasetID=gibberish\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a existent subdirectory but without trailing slash
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/GLsubdir");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/nceiPH53sstn1day/GLsubdir\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: GLsubdir .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent directory
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/gibberish/");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/nceiPH53sstn1day/gibberish/\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: Resource not found: directory=gibberish/\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in root
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/nceiPH53sstn1day/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);

    // try to download a non-existent file in existent subdir
    try {
      results =
          SSR.getUrlResponseStringNewline(
              "http://localhost:" + PORT + "/erddap/files/nceiPH53sstn1day/GLsubdir/gibberish.csv");
    } catch (Exception e) {
      results = e.toString();
    }
    expected =
        "java.io.IOException: HTTP status code=404 java.io.FileNotFoundException: http://localhost:"
            + PORT
            + "/erddap/files/nceiPH53sstn1day/GLsubdir/gibberish.csv\n"
            + "(Error {\n"
            + "    code=404;\n"
            + "    message=\"Not Found: File not found: gibberish.csv .\";\n"
            + "})";
    Test.ensureEqual(results, expected, "results=\n" + results);
  }

  /** NcHelper */

  /** An experiment with NetcdfDataset accessing a DAP sequence dataset. */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testSequence() throws Throwable {
    NetcdfDataset ncd =
        NetcdfDatasets.openDataset( // 2021: 's' is new API
            "http://localhost:" + PORT + "/erddap/tabledap/erdCAMarCatSY");
    try {
      String2.log(ncd.toString());
    } finally {
      ncd.close();
    }
  }

  /** EDDTests */
  @org.junit.jupiter.api.Test
  @TagJetty
  void testInPortXml() throws Throwable {
    String dir = EDStatic.fullTestCacheDirectory;
    String gridTable = "grid"; // grid or table
    String tDatasetID = "erdSWchlamday";
    String fileName = "ErddapToInPort_" + tDatasetID + ".xml";
    EDD edd =
        EDD.oneFromXmlFragment(
            null,
            "<dataset type=\"EDD"
                + String2.toTitleCase(gridTable)
                + "FromErddap\" datasetID=\""
                + tDatasetID
                + "\" active=\"true\">\n"
                + "    <sourceUrl>http://localhost:8080/erddap/"
                + gridTable
                + "dap/"
                + tDatasetID
                + "</sourceUrl>\n"
                + "</dataset>\n");
    String error =
        File2.writeToFileUtf8(
            dir + fileName,
            edd.getInPortXmlString(
                "No Archiving Intended",
                "",
                "This data is derived from data in an archive. "
                    + "The archives only want to archive the source data."));
    if (error.length() > 0) throw new RuntimeException(error);
    String results = File2.directReadFromUtf8File(dir + fileName);
    String expected =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<inport-metadata version=\"1.0\">\n"
            + "  <item-identification>\n"
            + "    <parent-catalog-item-id>???</parent-catalog-item-id>\n"
            + "    <catalog-item-id>???</catalog-item-id>\n"
            + "    <catalog-item-type>Data Set</catalog-item-type>\n"
            + "    <title>Chlorophyll-a, Orbview-2 SeaWiFS, 0.1Â°, Global, 1997-2010 (Monthly Composite) DEPRECATED</title>\n"
            + "    <short-name>erdSWchlamday</short-name>\n"
            + "    <status>In Work</status>\n"
            + "    <abstract>THIS VERSION IS DEPRECATED. SEE THE NEW R2018.0 VERSION IN erdSW2018chla1day. (Feb 2018) \n"
            + "NASA GSFC Ocean Color Web distributes science-quality chlorophyll-a concentration data from the Sea-viewing Wide Field-of-view Sensor (SeaWiFS) on the Orbview-2 satellite.</abstract>\n"
            + "    <purpose></purpose>\n"
            + "    <notes></notes>\n"
            + "    <other-citation-details></other-citation-details>\n"
            + "    <supplemental-information>https://coastwatch.pfeg.noaa.gov/infog/SW_chla_las.html</supplemental-information>\n"
            + "  </item-identification>\n"
            + "  <physical-location>\n"
            + //
            "    <organization>ERDDAP Jetty Install</organization>\n"
            + //
            "    <city>Nowhere</city>\n"
            + //
            "    <state-province>AK</state-province>\n"
            + //
            "    <country>USA</country>\n"
            + //
            "    <location-description></location-description>\n"
            + //
            "  </physical-location>\n"
            + "  <data-set-information>\n"
            + //
            "    <data-presentation-form>Table (digital)</data-presentation-form>\n"
            + //
            "    <data-presentation-form-other></data-presentation-form-other>\n"
            + //
            "    <instrument>SeaWiFS HRPT</instrument>\n"
            + //
            "    <platform>Orbview-2</platform>\n"
            + //
            "    <physical-collection-fishing-gear>Not Applicable</physical-collection-fishing-gear>\n"
            + //
            "  </data-set-information>\n"
            + //
            "  <support-roles mode=\"replace\">\n"
            + //
            "    <support-role>\n"
            + //
            "      <support-role-type>Metadata Contact</support-role-type>\n"
            + //
            "      <from-date>2024</from-date>\n"
            + //
            "      <person-email>nobody@example.com</person-email>\n"
            + //
            "      <organization>ERDDAP Jetty Install</organization>\n"
            + //
            "      <contact-instructions>email nobody@example.com</contact-instructions>\n"
            + //
            "    </support-role>\n"
            + //
            "    <support-role>\n"
            + //
            "      <support-role-type>Distributor</support-role-type>\n"
            + //
            "      <from-date>2024</from-date>\n"
            + //
            "      <person-email>nobody@example.com</person-email>\n"
            + //
            "      <organization>ERDDAP Jetty Install</organization>\n"
            + //
            "      <contact-instructions>email nobody@example.com</contact-instructions>\n"
            + //
            "    </support-role>\n"
            + //
            "    <support-role>\n"
            + //
            "      <support-role-type>Author</support-role-type>\n"
            + //
            "      <from-date>2024</from-date>\n"
            + //
            "      <person-email>erd.data@noaa.gov</person-email>\n"
            + //
            "      <organization></organization>\n"
            + //
            "      <contact-instructions>email erd.data@noaa.gov</contact-instructions>\n"
            + //
            "    </support-role>\n"
            + //
            "    <support-role>\n"
            + //
            "      <support-role-type>Data Set Credit</support-role-type>\n"
            + //
            "      <from-date>2024</from-date>\n"
            + //
            "      <person-email>erd.data@noaa.gov</person-email>\n"
            + //
            "      <organization></organization>\n"
            + //
            "      <contact-instructions>email erd.data@noaa.gov</contact-instructions>\n"
            + //
            "    </support-role>\n"
            + //
            "    <support-role>\n"
            + //
            "      <support-role-type>Data Steward</support-role-type>\n"
            + //
            "      <from-date>2024</from-date>\n"
            + //
            "      <person-email>erd.data@noaa.gov</person-email>\n"
            + //
            "      <organization></organization>\n"
            + //
            "      <contact-instructions>email erd.data@noaa.gov</contact-instructions>\n"
            + //
            "    </support-role>\n"
            + //
            "    <support-role>\n"
            + //
            "      <support-role-type>Point of Contact</support-role-type>\n"
            + //
            "      <from-date>2024</from-date>\n"
            + //
            "      <person-email>erd.data@noaa.gov</person-email>\n"
            + //
            "      <organization></organization>\n"
            + //
            "      <contact-instructions>email erd.data@noaa.gov</contact-instructions>\n"
            + //
            "    </support-role>\n"
            + //
            "  </support-roles>\n"
            + "  <extents mode=\"replace\">\n"
            + "    <extent>\n"
            + "      <description></description>\n"
            + "      <time-frames>\n"
            + "        <time-frame>\n"
            + "          <time-frame-type>Range</time-frame-type>\n"
            + "          <start-date-time>19970916T000000Z</start-date-time>\n"
            + "          <end-date-time>YYYYMMDDTHH0000Z</end-date-time>\n"
            + "          <description></description>\n"
            + "        </time-frame>\n"
            + "      </time-frames>\n"
            + "      <geographic-areas>\n"
            + "        <geographic-area>\n"
            + "          <west-bound>-180.0</west-bound>\n"
            + "          <east-bound>180.0</east-bound>\n"
            + "          <north-bound>90.0</north-bound>\n"
            + "          <south-bound>-90.0</south-bound>\n"
            + "          <description></description>\n"
            + "        </geographic-area>\n"
            + "      </geographic-areas>\n"
            + "    </extent>\n"
            + "  </extents>\n"
            + "  <access-information>\n"
            + "    <security-class>Unclassified</security-class>\n"
            + "    <security-classification-system></security-classification-system>\n"
            + "    <security-handling-description></security-handling-description>\n"
            + "    <data-access-policy>The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.</data-access-policy>\n"
            + "    <data-access-procedure>The data can be obtained from ERDDAP: http://localhost:8080/erddap/search/index.html?searchFor=datasetID&#37;3DerdSWchlamday</data-access-procedure>\n"
            + "    <data-access-constraints>Not Applicable</data-access-constraints>\n"
            + "    <data-use-constraints>The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.</data-use-constraints>\n"
            + "    <metadata-access-constraints>None</metadata-access-constraints>\n"
            + "    <metadata-use-constraints>None</metadata-use-constraints>\n"
            + "  </access-information>\n"
            + "  <data-quality>\n"
            + "    <representativeness></representativeness>\n"
            + "    <accuracy></accuracy>\n"
            + "    <analytical-accuracy></analytical-accuracy>\n"
            + "    <quantitation-limits></quantitation-limits>\n"
            + "    <bias></bias>\n"
            + "    <comparability></comparability>\n"
            + "    <completeness-measure></completeness-measure>\n"
            + "    <precision></precision>\n"
            + "    <analytical-precision></analytical-precision>\n"
            + "    <field-precision></field-precision>\n"
            + "    <sensitivity></sensitivity>\n"
            + "    <detection-limit></detection-limit>\n"
            + "    <completeness-report></completeness-report>\n"
            + "    <conceptual-consistency></conceptual-consistency>\n"
            + "    <quality-control-procedures>Data is checked for completeness, conceptual consistency, and reasonableness.</quality-control-procedures>\n"
            + "  </data-quality>\n"
            + "  <data-management>\n"
            + "    <resources-identified>Yes</resources-identified>\n"
            + "    <resources-budget-percentage>Unknown</resources-budget-percentage>\n"
            + "    <data-access-directive-compliant>Yes</data-access-directive-compliant>\n"
            + "    <data-access-directive-waiver></data-access-directive-waiver>\n"
            + "    <delay-collection-dissemination>0 days</delay-collection-dissemination>\n"
            + "    <delay-collection-dissemination-explanation></delay-collection-dissemination-explanation>\n"
            + "    <archive-location>No Archiving Intended</archive-location>\n"
            + "    <archive-location-explanation-other></archive-location-explanation-other>\n"
            + "    <archive-location-explanation-none>This data is derived from data in an archive. The archives only want to archive the source data.</archive-location-explanation-none>\n"
            + "    <delay-collection-archive>Not Applicable</delay-collection-archive>\n"
            + "    <data-protection-plan>The Environmental Research Department's IT Security and Contingency Plan establishes the security practices that ensure the security of the data and the plans necessary to recover and restore the data if problems occur.</data-protection-plan>\n"
            + "  </data-management>\n"
            + "  <lineage>\n"
            + "    <lineage-statement></lineage-statement>\n"
            + "    <lineage-process-steps>\n"
            + "      <lineage-process-step>\n"
            + "        <sequence-number>1</sequence-number>\n"
            + "        <description>NASA/GSFC/DAAC, GeoEye</description>\n"
            + "        <process-date-time></process-date-time>\n"
            + "        <process-contact-type></process-contact-type>\n"
            + "        <process-contact></process-contact>\n"
            + "        <process-contact-phone></process-contact-phone>\n"
            + "        <process-contact-email-address></process-contact-email-address>\n"
            + "        <source-citation></source-citation>\n"
            + "      </lineage-process-step>\n"
            + "      <lineage-process-step>\n"
            + "        <sequence-number>2</sequence-number>\n"
            + "        <description>NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD</description>\n"
            + "        <process-date-time>YYYYMMDDTHHmmssZ</process-date-time>\n"
            + "        <process-contact-type></process-contact-type>\n"
            + "        <process-contact></process-contact>\n"
            + "        <process-contact-phone></process-contact-phone>\n"
            + "        <process-contact-email-address></process-contact-email-address>\n"
            + "        <source-citation></source-citation>\n"
            + "      </lineage-process-step>\n"
            + "    </lineage-process-steps>\n"
            + "  </lineage>\n"
            + "  <downloads mode=\"replace\">\n"
            + "    <download>\n"
            + "      <download-url>http://localhost:8080/erddap/search/index.html?searchFor=datasetID&#37;3DerdSWchlamday</download-url>\n"
            + "      <file-name>erdSWchlamday</file-name>\n"
            + "      <description>This dataset is available in ERDDAP, a data server that gives you a simple, consistent way to download subsets of gridded and tabular scientific datasets in common file formats and make graphs and maps.</description>\n"
            + "      <file-date-time></file-date-time>\n"
            + "      <file-type>In ERDDAP, you can specify the file type that you want. Options include .htmlTable, OPeNDAP .das .dds or .dods, .esriAscii, .esriCsv, .mat, .nc, .odvTxt, .csv, .tsv, .json, .geoJson, .xhtml, .ncHeader, .ncml, .fgdc, .iso19115, Google Earth .kml, .geotif, .png, .transparentPng, and .pdf.</file-type>\n"
            + "      <fgdc-content-type>Live Data and Maps</fgdc-content-type>\n"
            + "      <file-size></file-size>\n"
            + "      <application-version></application-version>\n"
            + "      <compression>Uncompressed</compression>\n"
            + "      <review-status>Chked Viruses Inapp Content</review-status>\n"
            + "    </download>\n"
            + "  </downloads>\n"
            + "</inport-metadata>\n";
    results =
        results.replaceAll(
            "<end-date-time>[0-9]+T[0-9]+Z</end-date-time>",
            "<end-date-time>YYYYMMDDTHH0000Z</end-date-time>");
    results =
        results.replaceAll(
            "<process-date-time>[0-9]+T[0-9]+Z</process-date-time>",
            "<process-date-time>YYYYMMDDTHHmmssZ</process-date-time>");
    Test.ensureEqual(results, expected, "RESULTS=\n" + results);
  }

  /** OpendapHelper */

  /** This tests dapToNc DGrid. */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagFlaky // It seems if data is not cached to frequently fail for one of the sides
  // in erdQSwindmday
  void testDapToNcDGrid() throws Throwable {
    String2.log("\n\n*** OpendapHelper.testDapToNcDGrid");
    String fileName, expected, results;
    String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 10);

    // There was a bug where loading the wms page for this dataset would cause the altitude value to
    // increase by 10 every time. To verify that isn't happening, load the wms page.
    SSR.getUrlResponseStringUnchanged(
        "http://localhost:" + PORT + "/erddap/wms/erdQSwindmday_LonPM180/index.html");
    fileName = TEMP_DIR.toAbsolutePath() + "/testDapToNcDGrid.nc";
    System.out.println(fileName);
    String dGridUrl = "http://localhost:8080/erddap/griddap/erdQSwindmday";
    OpendapHelper.dapToNc(
        dGridUrl,
        // note that request for zztop is ignored (because not found)
        new String[] {"zztop", "x_wind", "y_wind"},
        "[1][0][0:200:1200][0:200:2880]", // projection
        fileName,
        false); // jplMode
    results = NcHelper.ncdump(fileName, ""); // printData
    expected =
        "netcdf testDapToNcDGrid.nc {\n"
            + "  dimensions:\n"
            + "    time = 1;\n"
            + "    altitude = 1;\n"
            + "    latitude = 7;\n"
            + "    longitude = 15;\n"
            + "  variables:\n"
            + "    double time(time=1);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = 9.348048E8, 9.3744E8; // double\n"
            + "      :axis = \"T\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Centered Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    double altitude(altitude=1);\n"
            + "      :_CoordinateAxisType = \"Height\";\n"
            + "      :_CoordinateZisPositive = \"up\";\n"
            + "      :actual_range = 10.0, 10.0; // double\n"
            + "      :axis = \"Z\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Altitude\";\n"
            + "      :positive = \"up\";\n"
            + "      :standard_name = \"altitude\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double latitude(latitude=7);\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :actual_range = -75.0, 75.0; // double\n"
            + "      :axis = \"Y\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 2; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    double longitude(longitude=15);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = 0.0, 360.0; // double\n"
            + "      :axis = \"X\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 2; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    float x_wind(time=1, altitude=1, latitude=7, longitude=15);\n"
            + "      :_FillValue = -9999999.0f; // float\n"
            + "      :colorBarMaximum = 15.0; // double\n"
            + "      :colorBarMinimum = -15.0; // double\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 1; // int\n"
            + "      :ioos_category = \"Wind\";\n"
            + "      :long_name = \"Zonal Wind\";\n"
            + "      :missing_value = -9999999.0f; // float\n"
            + "      :standard_name = \"x_wind\";\n"
            + "      :units = \"m s-1\";\n"
            + "\n"
            + "    float y_wind(time=1, altitude=1, latitude=7, longitude=15);\n"
            + "      :_FillValue = -9999999.0f; // float\n"
            + "      :colorBarMaximum = 15.0; // double\n"
            + "      :colorBarMinimum = -15.0; // double\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 1; // int\n"
            + "      :ioos_category = \"Wind\";\n"
            + "      :long_name = \"Meridional Wind\";\n"
            + "      :missing_value = -9999999.0f; // float\n"
            + "      :standard_name = \"y_wind\";\n"
            + "      :units = \"m s-1\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :composite = \"true\";\n"
            + "  :contributor_name = \"Remote Sensing Systems, Inc.\";\n"
            + "  :contributor_role = \"Source of level 2 data.\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :creator_email = \"erd.data@noaa.gov\";\n"
            + "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :creator_type = \"institution\";\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";\n"
            + "  :date_created = \"2010-07-02\";\n"
            + "  :date_issued = \"2010-07-02\";\n"
            + "  :defaultGraphQuery = \"&.draw=vectors\";\n"
            + "  :Easternmost_Easting = 360.0; // double\n"
            + "  :geospatial_lat_max = 75.0; // double\n"
            + "  :geospatial_lat_min = -75.0; // double\n"
            + "  :geospatial_lat_resolution = 0.125; // double\n"
            + "  :geospatial_lat_units = \"degrees_north\";\n"
            + "  :geospatial_lon_max = 360.0; // double\n"
            + "  :geospatial_lon_min = 0.0; // double\n"
            + "  :geospatial_lon_resolution = 0.125; // double\n"
            + "  :geospatial_lon_units = \"degrees_east\";\n"
            + "  :geospatial_vertical_max = 10.0; // double\n"
            + "  :geospatial_vertical_min = 10.0; // double\n"
            + "  :geospatial_vertical_positive = \"up\";\n"
            + "  :geospatial_vertical_units = \"m\";\n"
            + "  :history = \"Remote Sensing Systems, Inc.\n";
    // "2010-07-02T15:33:37Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
    // today + "T"; // + time "
    // https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/mday\n" +
    // today + "
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.das\";\n" +
    String expected2 =
        "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :keywords = \"altitude, atmosphere, atmospheric, coast, coastwatch, data, degrees, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Ocean Winds > Surface Winds, global, noaa, node, ocean, oceans, QSux10, quality, quikscat, science, science quality, seawinds, surface, time, wcn, west, wind, winds, x_wind, zonal\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + "  :Northernmost_Northing = 75.0; // double\n"
            + "  :origin = \"Remote Sensing Systems, Inc.\";\n"
            + "  :processing_level = \"3\";\n"
            + "  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "  :projection = \"geographic\";\n"
            + "  :projection_type = \"mapped\";\n"
            + "  :publisher_email = \"erd.data@noaa.gov\";\n"
            + "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :publisher_type = \"institution\";\n"
            + "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n"
            + "  :references = \"RSS Inc. Winds: http://www.remss.com/ .\";\n"
            + "  :satellite = \"QuikSCAT\";\n"
            + "  :sensor = \"SeaWinds\";\n"
            + "  :source = \"satellite observation: QuikSCAT, SeaWinds\";\n"
            + "  :sourceUrl = \"(local files)\";\n"
            + "  :Southernmost_Northing = -75.0; // double\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :summary = \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters. (This is a monthly composite.)\";\n"
            + "  :time_coverage_end = \"1999-09-16T00:00:00Z\";\n"
            + "  :time_coverage_start = \"1999-08-16T12:00:00Z\";\n"
            + "  :title = \"Wind, QuikSCAT SeaWinds, 0.125Â°, Global, Science Quality, 1999-2009 (Monthly)\";\n"
            + "  :Westernmost_Easting = 0.0; // double\n"
            + "\n"
            + "  data:\n"
            + "    time = \n"
            + "      {9.3744E8}\n"
            + "    altitude = \n"
            + "      {10.0}\n"
            + "    latitude = \n"
            + "      {-75.0, -50.0, -25.0, 0.0, 25.0, 50.0, 75.0}\n"
            + "    longitude = \n"
            + "      {0.0, 25.0, 50.0, 75.0, 100.0, 125.0, 150.0, 175.0, 200.0, 225.0, 250.0, 275.0, 300.0, 325.0, 350.0}\n"
            + "    x_wind = \n"
            + //
            "      {\n"
            + //
            "        {\n"
            + //
            "          {\n"
            + //
            "            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0},\n"
            + //
            "            {5.37559, 4.890142, 7.2589297, 9.556187, 8.795169, 7.448987, 5.1217284, 3.063305, 7.1047883, 8.3327, 4.615649, 4.7593627, 4.229129, 4.941825, 6.0496373},\n"
            + //
            "            {-4.8218846, -9999999.0, -4.280867, -5.7957973, -2.3290896, -9999999.0, -9999999.0, -6.2962894, -5.830912, -1.0914159, -3.277562, -2.4311755, -9999999.0, -1.9688762, -4.3181567},\n"
            + //
            "            {1.2137312, -9999999.0, 0.580993, 2.9145997, -9999999.0, -0.64948285, -3.6313703, -4.4887543, -5.22869, -4.8397746, -2.1917553, -0.028488753, -9999999.0, -5.5228443, -1.7843572},\n"
            + //
            "            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -1.8343765, -3.5090168, -4.262698, -5.7764935, -2.5673227, 1.6767642, -1.4483238, -3.166254, -5.655119, -9999999.0},\n"
            + //
            "            {2.4442203, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 3.1239662, 2.6691868, 3.1933768, 3.221914, -9999999.0, -9999999.0, -0.9400238, 5.3579793, 4.102313},\n"
            + //
            "            {1.5308881, 1.0626484, 1.5728527, 2.6770988, -9999999.0, 1.4636179, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 4.63886, -9999999.0, -0.15158409}\n"
            + //
            "          }\n"
            + //
            "        }\n"
            + //
            "      }\n"
            + //
            "    y_wind = \n"
            + //
            "      {\n"
            + //
            "        {\n"
            + //
            "          {\n"
            + //
            "            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0},\n"
            + //
            "            {-1.0215688, -2.0144277, -1.6640459, 0.20531581, -2.8249598, -2.1731012, -0.9963559, -0.27347103, 1.4820775, -0.1159739, -2.2770288, -1.9554303, 0.46956384, -0.26609817, -1.5246246},\n"
            + //
            "            {4.5096793, -9999999.0, -2.8698754, 1.8055042, 3.670552, -9999999.0, -9999999.0, 1.61813, 1.7241648, 0.72208166, 0.5931774, 3.794394, -9999999.0, -2.2662532, 2.211184},\n"
            + //
            "            {5.7195344, -9999999.0, 5.430522, 0.839178, -9999999.0, 1.5903727, 0.98022115, 1.7958285, 0.76642656, 2.768356, 1.579939, 5.841542, -9999999.0, 4.4927044, 4.5466847},\n"
            + //
            "            {-9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -1.790581, 0.23016424, -0.68734455, -0.94961494, -2.897025, 1.1826204, -1.8149276, 1.8312448, -1.619819, -9999999.0},\n"
            + //
            "            {3.5336227, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 1.8418659, 1.0235088, 0.5227146, 1.7917304, -9999999.0, -9999999.0, 3.551546, -4.5639772, 2.8214545},\n"
            + //
            "            {-0.70053107, 2.0271564, 0.66666394, 1.197742, -9999999.0, -1.0166361, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, 5.88275, -9999999.0, -6.4992795}\n"
            + //
            "          }\n"
            + //
            "        }\n"
            + //
            "      }\n"
            + "}\n";
    /*
     * From .asc request:
     * https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.asc?x_wind[5][0
     * ][0:200:1200][0:200:2880],y_wind[5][0][0:200:1200][0:200:2880]
     * x_wind.x_wind[1][1][7][15]
     * [0][0][0], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, 0.76867574, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, -9999999.0, -9999999.0
     * [0][0][1], 6.903795, 7.7432585, 8.052648, 7.375461, 8.358787, 7.5664454,
     * 4.537408, 4.349131, 2.4506109, 2.1340106, 6.4230127, 8.5656395, 5.679372,
     * 5.775274, 6.8520603
     * [0][0][2], -3.513153, -9999999.0, -5.7222853, -4.0249896, -4.6091595,
     * -9999999.0, -9999999.0, -3.9060166, -1.821446, -2.0546885, -2.349195,
     * -4.2188687, -9999999.0, -0.7905332, -3.715024
     * [0][0][3], 0.38850072, -9999999.0, -2.8492346, 0.7843591, -9999999.0,
     * -0.353197, -0.93183184, -5.3337674, -7.8715024, -5.2341905, -2.1567967,
     * 0.46681255, -9999999.0, -3.7223456, -1.3264368
     * [0][0][4], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -4.250928, -1.9779109, -2.3081408, -6.070514, -3.4209945, 2.3732827,
     * -3.4732149, -3.2282434, -3.99131, -9999999.0
     * [0][0][5], 2.3816996, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, 1.9863724, 1.746363, 5.305478, 2.3346918, -9999999.0, -9999999.0,
     * 2.0079596, 3.4320266, 1.8692436
     * [0][0][6], 0.83961326, -3.4395192, -3.1952338, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, -9999999.0, -2.9099085
     * y_wind.y_wind[1][1][7][15]
     * [0][0][0], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, 3.9745862, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, -9999999.0, -9999999.0
     * [0][0][1], -1.6358501, -2.1310546, -1.672539, -2.8083494, -1.7282568,
     * -2.5679686, -0.032763753, 0.6524638, 0.9784334, -2.4545083, 0.6344165,
     * -0.5887741, -0.6837046, -0.92711323, -1.9981208
     * [0][0][2], 3.7522712, -9999999.0, -0.04178731, 1.6603879, 5.321683,
     * -9999999.0, -9999999.0, 1.5633415, -0.50912154, -2.964269, -0.92438585,
     * 3.959174, -9999999.0, -2.2249718, 0.46982485
     * [0][0][3], 4.8992314, -9999999.0, -4.7178936, -3.2770228, -9999999.0,
     * -2.8111093, -0.9852706, 0.46997508, 0.0683085, 0.46172503, 1.2998049,
     * 3.5235379, -9999999.0, 1.1354263, 4.7139735
     * [0][0][4], -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -5.092368, -3.3667018, -0.60028434, -0.7609817, -1.114303, -3.6573937,
     * -0.934499, -0.40036556, -2.5770886, -9999999.0
     * [0][0][5], 0.56877106, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -3.2394278, 0.45922723, -0.8394715, 0.7333555, -9999999.0,
     * -9999999.0, -2.3936603, 3.725975, 0.09879057
     * [0][0][6], -6.128998, 2.379096, 7.463917, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0, -9999999.0,
     * -9999999.0, -9999999.0, -11.026609
     */
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);
    int po = results.indexOf("  :infoUrl =");
    Test.ensureEqual(results.substring(po), expected2, "results=" + results);
    File2.delete(fileName);

    // test 1D var should be ignored if others are 2+D
    String2.log("\n*** test 1D var should be ignored if others are 2+D");
    fileName = TEMP_DIR.toAbsolutePath() + "/testDapToNcDGrid1D2D.nc";
    OpendapHelper.dapToNc(
        dGridUrl,
        new String[] {"zztop", "x_wind", "y_wind", "latitude"},
        "[1][0][0:200:1200][0:200:2880]", // projection
        fileName,
        false); // jplMode
    results = NcHelper.ncdump(fileName, "-h"); // printData
    expected =
        "netcdf testDapToNcDGrid1D2D.nc {\n"
            + "  dimensions:\n"
            + "    time = 1;\n"
            + "    altitude = 1;\n"
            + "    latitude = 7;\n"
            + "    longitude = 15;\n"
            + "  variables:\n"
            + "    double time(time=1);\n"
            + "      :_CoordinateAxisType = \"Time\";\n"
            + "      :actual_range = 9.348048E8, 9.3744E8; // double\n"
            + "      :axis = \"T\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Time\";\n"
            + "      :long_name = \"Centered Time\";\n"
            + "      :standard_name = \"time\";\n"
            + "      :time_origin = \"01-JAN-1970 00:00:00\";\n"
            + "      :units = \"seconds since 1970-01-01T00:00:00Z\";\n"
            + "\n"
            + "    double altitude(altitude=1);\n"
            + "      :_CoordinateAxisType = \"Height\";\n"
            + "      :_CoordinateZisPositive = \"up\";\n"
            + "      :actual_range = 10.0, 10.0; // double\n"
            + "      :axis = \"Z\";\n"
            + "      :fraction_digits = 0; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Altitude\";\n"
            + "      :positive = \"up\";\n"
            + "      :standard_name = \"altitude\";\n"
            + "      :units = \"m\";\n"
            + "\n"
            + "    double latitude(latitude=7);\n"
            + "      :_CoordinateAxisType = \"Lat\";\n"
            + "      :actual_range = -75.0, 75.0; // double\n"
            + "      :axis = \"Y\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 2; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Latitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"latitude\";\n"
            + "      :units = \"degrees_north\";\n"
            + "\n"
            + "    double longitude(longitude=15);\n"
            + "      :_CoordinateAxisType = \"Lon\";\n"
            + "      :actual_range = 0.0, 360.0; // double\n"
            + "      :axis = \"X\";\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 2; // int\n"
            + "      :ioos_category = \"Location\";\n"
            + "      :long_name = \"Longitude\";\n"
            + "      :point_spacing = \"even\";\n"
            + "      :standard_name = \"longitude\";\n"
            + "      :units = \"degrees_east\";\n"
            + "\n"
            + "    float x_wind(time=1, altitude=1, latitude=7, longitude=15);\n"
            + "      :_FillValue = -9999999.0f; // float\n"
            + "      :colorBarMaximum = 15.0; // double\n"
            + "      :colorBarMinimum = -15.0; // double\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 1; // int\n"
            + "      :ioos_category = \"Wind\";\n"
            + "      :long_name = \"Zonal Wind\";\n"
            + "      :missing_value = -9999999.0f; // float\n"
            + "      :standard_name = \"x_wind\";\n"
            + "      :units = \"m s-1\";\n"
            + "\n"
            + "    float y_wind(time=1, altitude=1, latitude=7, longitude=15);\n"
            + "      :_FillValue = -9999999.0f; // float\n"
            + "      :colorBarMaximum = 15.0; // double\n"
            + "      :colorBarMinimum = -15.0; // double\n"
            + "      :coordsys = \"geographic\";\n"
            + "      :fraction_digits = 1; // int\n"
            + "      :ioos_category = \"Wind\";\n"
            + "      :long_name = \"Meridional Wind\";\n"
            + "      :missing_value = -9999999.0f; // float\n"
            + "      :standard_name = \"y_wind\";\n"
            + "      :units = \"m s-1\";\n"
            + "\n"
            + "  // global attributes:\n"
            + "  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n"
            + "  :cdm_data_type = \"Grid\";\n"
            + "  :composite = \"true\";\n"
            + "  :contributor_name = \"Remote Sensing Systems, Inc.\";\n"
            + "  :contributor_role = \"Source of level 2 data.\";\n"
            + "  :Conventions = \"COARDS, CF-1.6, ACDD-1.3\";\n"
            + "  :creator_email = \"erd.data@noaa.gov\";\n"
            + "  :creator_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :creator_type = \"institution\";\n"
            + "  :creator_url = \"https://www.pfeg.noaa.gov\";\n"
            + "  :date_created = \"2010-07-02\";\n"
            + "  :date_issued = \"2010-07-02\";\n"
            + "  :defaultGraphQuery = \"&.draw=vectors\";\n"
            + "  :Easternmost_Easting = 360.0; // double\n"
            + "  :geospatial_lat_max = 75.0; // double\n"
            + "  :geospatial_lat_min = -75.0; // double\n"
            + "  :geospatial_lat_resolution = 0.125; // double\n"
            + "  :geospatial_lat_units = \"degrees_north\";\n"
            + "  :geospatial_lon_max = 360.0; // double\n"
            + "  :geospatial_lon_min = 0.0; // double\n"
            + "  :geospatial_lon_resolution = 0.125; // double\n"
            + "  :geospatial_lon_units = \"degrees_east\";\n"
            + "  :geospatial_vertical_max = 10.0; // double\n"
            + "  :geospatial_vertical_min = 10.0; // double\n"
            + "  :geospatial_vertical_positive = \"up\";\n"
            + "  :geospatial_vertical_units = \"m\";\n"
            + "  :history = \"Remote Sensing Systems, Inc.\n";
    // "2010-07-02T15:33:37Z NOAA CoastWatch (West Coast Node) and NOAA SFSC ERD\n" +
    // today + "T"; // time https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/ux10/mday\n"
    // +
    // today + time "
    // https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdQSwindmday.das\";\n" +
    expected2 =
        "  :infoUrl = \"https://coastwatch.pfeg.noaa.gov/infog/QS_ux10_las.html\";\n"
            + "  :institution = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :keywords = \"altitude, atmosphere, atmospheric, coast, coastwatch, data, degrees, Earth Science > Atmosphere > Atmospheric Winds > Surface Winds, Earth Science > Oceans > Ocean Winds > Surface Winds, global, noaa, node, ocean, oceans, QSux10, quality, quikscat, science, science quality, seawinds, surface, time, wcn, west, wind, winds, x_wind, zonal\";\n"
            + "  :keywords_vocabulary = \"GCMD Science Keywords\";\n"
            + "  :license = \"The data may be used and redistributed for free but is not intended\n"
            + "for legal use, since it may contain inaccuracies. Neither the data\n"
            + "Contributor, ERD, NOAA, nor the United States Government, nor any\n"
            + "of their employees or contractors, makes any warranty, express or\n"
            + "implied, including warranties of merchantability and fitness for a\n"
            + "particular purpose, or assumes any legal liability for the accuracy,\n"
            + "completeness, or usefulness, of this information.\";\n"
            + "  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n"
            + "  :Northernmost_Northing = 75.0; // double\n"
            + "  :origin = \"Remote Sensing Systems, Inc.\";\n"
            + "  :processing_level = \"3\";\n"
            + "  :project = \"CoastWatch (https://coastwatch.noaa.gov/)\";\n"
            + "  :projection = \"geographic\";\n"
            + "  :projection_type = \"mapped\";\n"
            + "  :publisher_email = \"erd.data@noaa.gov\";\n"
            + "  :publisher_name = \"NOAA NMFS SWFSC ERD\";\n"
            + "  :publisher_type = \"institution\";\n"
            + "  :publisher_url = \"https://www.pfeg.noaa.gov\";\n"
            + "  :references = \"RSS Inc. Winds: http://www.remss.com/ .\";\n"
            + "  :satellite = \"QuikSCAT\";\n"
            + "  :sensor = \"SeaWinds\";\n"
            + "  :source = \"satellite observation: QuikSCAT, SeaWinds\";\n"
            + "  :sourceUrl = \"(local files)\";\n"
            + "  :Southernmost_Northing = -75.0; // double\n"
            + "  :standard_name_vocabulary = \"CF Standard Name Table v70\";\n"
            + "  :summary = \"Remote Sensing Inc. distributes science quality wind velocity data from the SeaWinds instrument onboard NASA's QuikSCAT satellite.  SeaWinds is a microwave scatterometer designed to measure surface winds over the global ocean.  Wind velocity fields are provided in zonal, meridional, and modulus sets. The reference height for all wind velocities is 10 meters. (This is a monthly composite.)\";\n"
            + "  :time_coverage_end = \"1999-09-16T00:00:00Z\";\n"
            + "  :time_coverage_start = \"1999-08-16T12:00:00Z\";\n"
            + "  :title = \"Wind, QuikSCAT SeaWinds, 0.125Â°, Global, Science Quality, 1999-2009 (Monthly)\";\n"
            + "  :Westernmost_Easting = 0.0; // double\n"
            + "}\n";
    Test.ensureEqual(results.substring(0, expected.length()), expected, "results=" + results);
    po = results.indexOf("  :infoUrl =");
    Test.ensureEqual(results.substring(po), expected2, "results=" + results);
    File2.delete(fileName);

    /* */
    String2.log("\n*** OpendapHelper.testDapToNcDGrid finished.");
  }

  /** This tests findVarsWithSharedDimensions. */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagFlaky // It seems if data is not cached to frequently fail for one of the sides
  // in erdQSwindmday
  void testFindVarsWithSharedDimensions() throws Throwable {
    String2.log("\n\n*** OpendapHelper.findVarsWithSharedDimensions");
    String expected, results;
    DConnect dConnect;
    DDS dds;

    /*
     * //test of Sequence DAP dataset
     * String2.log("\n*** test of Sequence DAP dataset");
     * String sequenceUrl =
     * "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecMoc1";
     * dConnect = new DConnect(sequenceUrl, true, 1, 1);
     * dds = dConnect.getDDS(DEFAULT_TIMEOUT);
     * results = String2.toCSSVString(findVarsWithSharedDimensions(dds));
     * expected =
     * "zztop";
     * Test.ensureEqual(results, expected, "results=" + results);
     */

    // test of DArray DAP dataset
    // 2018-09-13 https: works in browser by not yet in Java
    String dArrayUrl =
        "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
    String2.log("\n*** test of DArray DAP dataset\n" + dArrayUrl);
    dConnect = new DConnect(dArrayUrl, true, 1, 1);
    dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    results = String2.toCSSVString(OpendapHelper.findVarsWithSharedDimensions(dds));
    expected =
        "time, lat, lon, PL_HD, PL_CRS, DIR, PL_WDIR, PL_SPD, SPD, PL_WSPD, P, T, RH, date, time_of_day, flag";
    Test.ensureEqual(results, expected, "results=" + results);

    // ***** test of DGrid DAP dataset
    String2.log("\n*** test of DGrid DAP dataset");
    String dGridUrl = "http://localhost:8080/erddap/griddap/erdQSwindmday";
    dConnect = new DConnect(dGridUrl, true, 1, 1);
    dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    results = String2.toCSSVString(OpendapHelper.findVarsWithSharedDimensions(dds));
    expected = "x_wind, y_wind";
    Test.ensureEqual(results, expected, "results=" + results);

    /* */
    String2.log("\n*** OpendapHelper.testFindVarsWithSharedDimensions finished.");
  }

  /** This tests findAllVars. */
  @org.junit.jupiter.api.Test
  @TagJetty
  @TagThredds
  void testFindAllScalarOrMultiDimVars() throws Throwable {
    String2.log("\n\n*** OpendapHelper.testFindAllScalarOrMultiDimVars");
    String expected, results;
    DConnect dConnect;
    DDS dds;
    String url;

    /*
     * //test of Sequence DAP dataset
     * String2.log("\n*** test of Sequence DAP dataset");
     * url = "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/erdGlobecMoc1";
     * dConnect = new DConnect(url, true, 1, 1);
     * dds = dConnect.getDDS(DEFAULT_TIMEOUT);
     * results = String2.toCSSVString(findVarsWithSharedDimensions(dds));
     * expected =
     * "zztop";
     * Test.ensureEqual(results, expected, "results=" + results);
     */

    // test of DArray DAP dataset
    // 2018-09-13 https: works in browser by not yet in Java. 2019-06-28 https works
    // in Java
    url =
        "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/research/WTEP/2012/WTEP_20120128v30001.nc";
    String2.log("\n*** test of DArray DAP dataset\n" + url);
    dConnect = new DConnect(url, true, 1, 1);
    dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    results = String2.toCSSVString(OpendapHelper.findAllScalarOrMultiDimVars(dds));
    expected =
        "time, lat, lon, PL_HD, PL_CRS, DIR, PL_WDIR, PL_SPD, SPD, PL_WSPD, P, T, RH, date, time_of_day, flag, history";
    Test.ensureEqual(results, expected, "results=" + results);

    // ***** test of DGrid DAP dataset
    String2.log("\n*** test of DGrid DAP dataset");
    url = "http://localhost:8080/erddap/griddap/erdQSwindmday";
    dConnect = new DConnect(url, true, 1, 1);
    dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    results = String2.toCSSVString(OpendapHelper.findAllScalarOrMultiDimVars(dds));
    expected = "time, altitude, latitude, longitude, x_wind, y_wind";
    Test.ensureEqual(results, expected, "results=" + results);

    // ***** test of NODC template dataset
    /**
     * 2020-10-26 disabled because source is unreliable String2.log("\n*** test of NODC template
     * dataset"); url =
     * "https://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/BodegaMarineLabBuoyCombined.nc";
     * dConnect = new DConnect(url, true, 1, 1); dds = dConnect.getDDS(DEFAULT_TIMEOUT); results =
     * String2.toCSSVString(findAllScalarOrMultiDimVars(dds)); expected = "time, lat, lon, alt,
     * station_name, temperature, salinity, density, conductivity, " + "turbidity, fluorescence,
     * platform1, temperature_qc, salinity_qc, density_qc, " + "conductivity_qc, turbidity_qc,
     * fluorescence_qc, instrument1, instrument2, " + "instrument3, ht_wgs84, ht_mllw, crs";
     * Test.ensureEqual(results, expected, "results=" + results);
     */

    // ***** test of sequence dataset (no vars should be found
    String2.log("\n*** test of sequence dataset");
    url = "http://localhost:8080/erddap/tabledap/erdCAMarCatLY";
    dConnect = new DConnect(url, true, 1, 1);
    dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
    results = String2.toCSSVString(OpendapHelper.findAllScalarOrMultiDimVars(dds));
    expected = "";
    Test.ensureEqual(results, expected, "results=" + results);

    /* */
    String2.log("\n*** OpendapHelper.testFindAllScalarOrMultiDimVars finished.");
  }

  @org.junit.jupiter.api.Test
  @TagJetty
  void parserAllDatasetsTest() throws Throwable {

    TopLevelHandler topLevelHandler;
    SAXParserFactory factory;
    SAXParser saxParser;
    InputStream inputStream;
    SaxHandler saxHandler;
    SaxParsingContext context;

    context = new SaxParsingContext();

    context.setNTryAndDatasets(new int[2]);
    context.setChangedDatasetIDs(new StringArray());
    context.setOrphanIDSet(new HashSet<>());
    context.setDatasetIDSet(new HashSet<>());
    context.setDuplicateDatasetIDs(new StringArray());
    context.setDatasetsThatFailedToLoadSB(new StringBuilder());
    context.setFailedDatasetsWithErrorsSB(new StringBuilder());
    context.setWarningsFromLoadDatasets(new StringBuilder());
    context.setDatasetsThatFailedToLoadSB(new StringBuilder());
    context.settUserHashMap(new HashMap<String, Object[]>());
    context.setMajorLoad(false);
    context.setErddap(new Erddap());
    context.setLastLuceneUpdate(0);
    context.setDatasetsRegex(EDStatic.datasetsRegex);
    context.setReallyVerbose(false);

    factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(true);
    saxParser = factory.newSAXParser();
    saxHandler = new SaxHandler(context);
    topLevelHandler = new TopLevelHandler(saxHandler, context);
    saxHandler.setState(topLevelHandler);

    inputStream = File2.getBufferedInputStream("development/test/datasets.xml");
    if (inputStream == null) {
      throw new IllegalArgumentException("File not found: development/test/datasets.xml");
    }
    saxParser.parse(inputStream, saxHandler);

    EDDTableFromErddap eddTableFromErddap =
        (EDDTableFromErddap) context.getErddap().tableDatasetHashMap.get("rlPmelTaoDySst");
    assertEquals(
        "http://localhost:8080/erddap/tabledap/pmelTaoDySst", eddTableFromErddap.localSourceUrl());

    EDDTableFromEDDGrid eddTableFromEDDGrid =
        (EDDTableFromEDDGrid) context.getErddap().tableDatasetHashMap.get("erdMPOC1day_AsATable");
    assertEquals("(local files)", eddTableFromEDDGrid.localSourceUrl());
    assertEquals("String for accessibleTo", eddTableFromEDDGrid.getAccessibleTo()[0]);

    EDDGridFromDap eddGridFromDap =
        (EDDGridFromDap) context.getErddap().gridDatasetHashMap.get("hawaii_d90f_20ee_c4cb");
    assertEquals(
        "http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4",
        eddGridFromDap.localSourceUrl());

    EDDGridLonPM180 eddGridLonPM180 =
        (EDDGridLonPM180) context.getErddap().gridDatasetHashMap.get("erdQSwindmday_LonPM180");
    assertEquals(1, eddGridLonPM180.childDatasetIDs().size());
    assertEquals("erdQSwindmday", eddGridLonPM180.childDatasetIDs().get(0));
    assertEquals("(local files)", eddGridLonPM180.getChildDataset(0).localSourceUrl());

    EDDGridFromErddap eddGridFromErddap =
        (EDDGridFromErddap) context.getErddap().gridDatasetHashMap.get("testGridFromErddap");
    assertEquals(
        "http://localhost:8080/erddap/griddap/nceiPH53sstn1day",
        eddGridFromErddap.localSourceUrl());

    EDDTableFromAsciiFiles eddTableFromAsciiFiles =
        (EDDTableFromAsciiFiles)
            context.getErddap().tableDatasetHashMap.get("LiquidR_HBG3_2015_weather");
    assertEquals("weather.*\\.csv", eddTableFromAsciiFiles.fileNameRegex());

    EDDGridSideBySide eddGridSideBySide =
        (EDDGridSideBySide) context.getErddap().gridDatasetHashMap.get("erdQSwindmday");
    assertEquals(2, eddGridSideBySide.childDatasetIDs().size());

    EDDGridFromEtopo eddGridFromEtopo =
        (EDDGridFromEtopo) context.getErddap().gridDatasetHashMap.get("etopo180");
    assertEquals("etopo180", eddGridFromEtopo.datasetID());
  }
}
