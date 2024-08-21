package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDGrid.DEFAULT_MATCH_AXIS_N_DIGITS;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridSideBySide;
import gov.noaa.pfel.erddap.util.EDStatic;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridSideBySideHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;
  private SaxParsingContext context;

  public EDDGridSideBySideHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.completeState = completeState;
    this.context = context;
  }

  private ArrayList<EDDGrid> tChildDatasets = new ArrayList();
  private StringBuilder messages = new StringBuilder();
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private boolean tAccessibleViaWMS = true;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private int tnThreads = -1;
  private boolean tDimensionValuesInMemory = true;

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
  public void characters(char[] ch, int start, int length) throws SAXException {
    content.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws Throwable {
    String contentStr = content.toString().trim();

    switch (localName) {
      case "onChange" -> tOnChange.add(contentStr);
      case "matchAxisNDigits" ->
          tMatchAxisNDigits = String2.parseInt(contentStr, DEFAULT_MATCH_AXIS_N_DIGITS);
      case "ensureAxisValuesAreEqual" ->
          tMatchAxisNDigits = String2.parseBoolean(contentStr) ? 20 : 0;
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
      case "dataset" -> {
        EDDGrid[] tcds = new EDDGrid[tChildDatasets.size()];
        tcds = tChildDatasets.toArray(tcds);

        EDD dataset =
            new EDDGridSideBySide(
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
        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }

  @Override
  public void handleDataset(EDD dataset) {
    tChildDatasets.add((EDDGrid) dataset);
  }
}
