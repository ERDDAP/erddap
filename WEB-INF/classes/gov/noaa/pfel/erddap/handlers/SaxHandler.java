package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.Erddap;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SaxHandler extends DefaultHandler {
  private State state;
  SaxParsingContext context;

  public SaxHandler(SaxParsingContext context) {
    this.context = context;
  }

  public void setState(State state) {
    this.state = state;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    try {
      this.state.startElement(uri, localName, qName, attributes);
    } catch (Throwable e) {
      context.getWarningsFromLoadDatasets().append(e.getMessage());
      context.getDatasetsThatFailedToLoadSB().append(EDStatic.cldDatasetID).append(" ");
      context
          .getFailedDatasetsWithErrorsSB()
          .append(EDStatic.cldDatasetID)
          .append(": ")
          .append(e.getMessage())
          .append("\n");
      String2.log(e.getMessage());
      this.state.popState();
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      this.state.characters(ch, start, length);
    } catch (Throwable e) {
      context.getWarningsFromLoadDatasets().append(e.getMessage());
      context.getDatasetsThatFailedToLoadSB().append(EDStatic.cldDatasetID).append(" ");
      context
          .getFailedDatasetsWithErrorsSB()
          .append(EDStatic.cldDatasetID)
          .append(": ")
          .append(e.getMessage())
          .append("\n");
      String2.log(e.getMessage());
      this.state.popState();
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    try {
      this.state.endElement(uri, localName, qName);
    } catch (Throwable e) {
      context.getWarningsFromLoadDatasets().append(e.getMessage());
      context.getDatasetsThatFailedToLoadSB().append(EDStatic.cldDatasetID).append(", ");
      context
          .getFailedDatasetsWithErrorsSB()
          .append(EDStatic.cldDatasetID)
          .append(": ")
          .append(e.getMessage())
          .append("\n");
      String2.log(e.getMessage());
      this.state.popState();
    }
  }

  public static EDD parseOneDataset(InputStream inputStream, String datasetId, Erddap erddap)
      throws ParserConfigurationException, SAXException, IOException {
    var context = new SaxParsingContext();

    context.setNTryAndDatasets(new int[2]);
    context.setChangedDatasetIDs(new StringArray());
    context.setOrphanIDSet(new HashSet<>());
    context.setDatasetIDSet(new HashSet<>());
    context.setDuplicateDatasetIDs(new StringArray());
    context.setDatasetsThatFailedToLoadSB(new StringBuilder());
    context.setFailedDatasetsWithErrorsSB(new StringBuilder());
    context.setWarningsFromLoadDatasets(new StringBuilder());
    context.settUserHashMap(new HashMap<String, Object[]>());
    context.setMajorLoad(false);
    context.setErddap(erddap);
    context.setLastLuceneUpdate(System.currentTimeMillis());
    context.setDatasetsRegex(datasetId);
    context.setReallyVerbose(false);

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setXIncludeAware(true);
    factory.setNamespaceAware(true);

    SAXParser saxParser = factory.newSAXParser();
    SaxHandler saxHandler = new SaxHandler(context);

    TopLevelDatasetCapture topLevelHandler = new TopLevelDatasetCapture(saxHandler, context);

    saxHandler.setState(topLevelHandler);
    saxParser.parse(inputStream, saxHandler);

    return topLevelHandler.getDataset();
  }

  public static void parse(
      InputStream inputStream,
      int[] nTryAndDatasets,
      StringArray changedDatasetIDs,
      HashSet<String> orphanIDSet,
      HashSet<String> datasetIDSet,
      StringArray duplicateDatasetIDs,
      StringBuilder datasetsThatFailedToLoadSB,
      StringBuilder failedDatasetsWithErrors,
      StringBuilder warningsFromLoadDatasets,
      HashMap<String, Object[]> tUserHashMap,
      boolean majorLoad,
      Erddap erddap,
      long lastLuceneUpdate,
      String datasetsRegex,
      boolean reallyVerbose)
      throws ParserConfigurationException, SAXException, IOException {

    var context = new SaxParsingContext();

    context.setNTryAndDatasets(nTryAndDatasets);
    context.setChangedDatasetIDs(changedDatasetIDs);
    context.setOrphanIDSet(orphanIDSet);
    context.setDatasetIDSet(datasetIDSet);
    context.setDuplicateDatasetIDs(duplicateDatasetIDs);
    context.setDatasetsThatFailedToLoadSB(datasetsThatFailedToLoadSB);
    context.setFailedDatasetsWithErrorsSB(failedDatasetsWithErrors);
    context.setWarningsFromLoadDatasets(warningsFromLoadDatasets);
    context.settUserHashMap(tUserHashMap);
    context.setMajorLoad(majorLoad);
    context.setErddap(erddap);
    context.setLastLuceneUpdate(lastLuceneUpdate);
    context.setDatasetsRegex(datasetsRegex);
    context.setReallyVerbose(reallyVerbose);

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setXIncludeAware(true);
    factory.setNamespaceAware(true);

    SAXParser saxParser = factory.newSAXParser();
    SaxHandler saxHandler = new SaxHandler(context);

    TopLevelHandler topLevelHandler = new TopLevelHandler(saxHandler, context);

    saxHandler.setState(topLevelHandler);
    saxParser.parse(inputStream, saxHandler);
  }
}
