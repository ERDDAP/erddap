package gov.noaa.pfel.erddap.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cohort.array.StringArray;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.handlers.*;
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

public class DatasetHandlerTests {
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
    context.settUserHashMap(new HashMap());
    context.setMajorLoad(false);
    context.setErddap(new Erddap());
    context.setLastLuceneUpdate(0);
    context.setDatasetsRegex(EDStatic.datasetsRegex);
    context.setReallyVerbose(false);

    factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(true);
    saxParser = factory.newSAXParser();
    saxHandler = new SaxHandler();
    topLevelHandler = new TopLevelHandler(saxHandler, context);
  }

  @BeforeEach
  void init() {
    inputStream =
        TopLevelHandlerTests.class.getResourceAsStream("/datasets/topLevelHandlerTest.xml");
    if (inputStream == null) {
      throw new IllegalArgumentException("File not found: /datasets/topLevelHandlerTest.xml");
    }
  }

  @Test
  void EDDTableFromErddapHandlerTest() throws IOException, SAXException {
    var eddTableFromErddapHandler =
        new EDDTableFromErddapHandler(saxHandler, "cwwcNDBCMet", topLevelHandler, context);
    saxHandler.setState(eddTableFromErddapHandler);
    saxParser.parse(inputStream, saxHandler);
    assertEquals(
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet",
        eddTableFromErddapHandler.tLocalSourceUrl);
  }
}
