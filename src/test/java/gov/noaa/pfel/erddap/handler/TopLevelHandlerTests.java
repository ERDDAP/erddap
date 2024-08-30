package gov.noaa.pfel.erddap.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cohort.array.StringArray;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.SaxHandler;
import gov.noaa.pfel.erddap.handlers.SaxParsingContext;
import gov.noaa.pfel.erddap.handlers.TopLevelHandler;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import testDataset.Initialization;

public class TopLevelHandlerTests {

  private static TopLevelHandler topLevelHandler;
  private static SAXParserFactory factory;
  private static SAXParser saxParser;
  private static InputStream inputStream;
  private static SaxHandler saxHandler;
  private static SaxParsingContext context;

  @BeforeAll
  static void initAll() throws Throwable {
    Initialization.edStatic();
    context = new SaxParsingContext();

    context.setNTryAndDatasets(new int[2]);
    context.setChangedDatasetIDs(new StringArray());
    context.setOrphanIDSet(new HashSet<>());
    context.setDatasetIDSet(new HashSet<>());
    context.setDuplicateDatasetIDs(new StringArray());
    context.setWarningsFromLoadDatasets(new StringBuilder());
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
  }

  @BeforeEach
  void init() throws IOException, SAXException {
    inputStream =
        TopLevelHandlerTests.class.getResourceAsStream("/datasets/topLevelHandlerTest.xml");
    if (inputStream == null) {
      throw new IllegalArgumentException("File not found: /datasets/topLevelHandlerTest.xml");
    }
    saxParser.parse(inputStream, saxHandler);
  }

  @Test
  void convertToPublicSourceUrlTest() {
    assertEquals(
        EDStatic.convertToPublicSourceUrl.get("http://example.com/"), "http://public.example.com/");
  }

  @Test
  void angularDegreeUnitsTest() {
    assertEquals(
        EDStatic.angularDegreeUnitsSet.toString(), "[angular, for, degree, units, content]");
  }

  @Test
  void unusualActivityTest() {
    assertEquals(EDStatic.unusualActivity, 25);
  }

  @Test
  void userTest() {
    Object[] user1Data = context.gettUserHashMap().get("user1");
    assertEquals("pass1", user1Data[0]);
  }

  @Test
  void datasetTest() {
    assertEquals(2, context.getNTryAndDatasets()[1]);
  }
}
