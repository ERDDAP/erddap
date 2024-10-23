package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDGrid.DEFAULT_MATCH_AXIS_N_DIGITS;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridSideBySide;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridSideBySideHandler extends BaseGridHandler {
  private SaxParsingContext context;

  public EDDGridSideBySideHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private ArrayList<EDDGrid> tChildDatasets = new ArrayList<>();
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (localName.equals("dataset")) {
      String tType = attributes.getValue("type");
      if (tType == null || !tType.startsWith("EDDGrid")) {
        throw new RuntimeException(
            "The datasets defined in an " + "EDDGridSideBySide must be a subclass of EDDGrid.");
      }
      String active = attributes.getValue("active");
      String childDatasetID = attributes.getValue("datasetID");
      State state =
          HandlerFactory.getHandlerFor(
              tType, childDatasetID, active, this, saxHandler, context, false);
      saxHandler.setState(state);
    }
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "matchAxisNDigits" ->
          tMatchAxisNDigits = String2.parseInt(contentStr, DEFAULT_MATCH_AXIS_N_DIGITS);
      case "ensureAxisValuesAreEqual" ->
          tMatchAxisNDigits = String2.parseBoolean(contentStr) ? 20 : 0;
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  public void handleDataset(EDD dataset) {
    tChildDatasets.add((EDDGrid) dataset);
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    EDDGrid[] tcds = new EDDGrid[tChildDatasets.size()];
    tcds = tChildDatasets.toArray(tcds);

    return new EDDGridSideBySide(
        datasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tAccessibleViaFiles,
        tMatchAxisNDigits,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tcds,
        tnThreads,
        tDimensionValuesInMemory);
  }
}
