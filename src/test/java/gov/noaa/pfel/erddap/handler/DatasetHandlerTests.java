package gov.noaa.pfel.erddap.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cohort.array.StringArray;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.EDDGridFromDap;
import gov.noaa.pfel.erddap.dataset.EDDGridLonPM180;
import gov.noaa.pfel.erddap.dataset.EDDTableFromEDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTableFromErddap;
import gov.noaa.pfel.erddap.handlers.*;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.BeforeAll;
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
    saxHandler.setState(topLevelHandler);

    inputStream =
        TopLevelHandlerTests.class.getResourceAsStream("/datasets/datasetHandlerTest.xml");
    if (inputStream == null) {
      throw new IllegalArgumentException("File not found: /datasets/datasetHandlerTest.xml");
    }
  }

  @Test
  void parserAllDatasets() throws IOException, SAXException {
    saxParser.parse(inputStream, saxHandler);

    EDDTableFromErddap eddTableFromErddap =
        (EDDTableFromErddap) context.getErddap().tableDatasetHashMap.get("cwwcNDBCMet");
    assertEquals(
        "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet",
        eddTableFromErddap.localSourceUrl());

    EDDTableFromEDDGrid eddTableFromEDDGrid =
        (EDDTableFromEDDGrid)
            context.getErddap().tableDatasetHashMap.get("erdMH1cflh1day_AsATable");
    assertEquals(
        "http://localhost:8080/erddap/griddap/erdMH1cflh1day",
        eddTableFromEDDGrid.localSourceUrl());

    EDDGridFromDap eddGridFromDap =
        (EDDGridFromDap) context.getErddap().gridDatasetHashMap.get("hawaii_d90f_20ee_c4cb");
    assertEquals(
        "http://apdrc.soest.hawaii.edu/dods/public_data/SODA/soda_pop2.2.4",
        eddGridFromDap.localSourceUrl());

    EDDGridLonPM180 eddGridLonPM180 =
        (EDDGridLonPM180) context.getErddap().gridDatasetHashMap.get("erdTAssh1day_LonPM180");
    assertEquals("person1", eddGridLonPM180.getAccessibleTo()[0]);
  }
}
