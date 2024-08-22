package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDGrid.DEFAULT_MATCH_AXIS_N_DIGITS;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridAggregateExistingDimension;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridAggregateExistingDimensionHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;
  private SaxParsingContext context;

  public EDDGridAggregateExistingDimensionHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.context = context;
  }

  private EDDGrid firstChild = null;
  private StringArray tLocalSourceUrls = new StringArray();
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private boolean tAccessibleViaWMS = true;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
  private boolean tDimensionValuesInMemory = true;

  private String tSUServerType = null;
  private String tSURegex = null;
  private boolean tSURecursive = true;
  private String tSUPathRegex = ".*";
  private String tSU = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    switch (localName) {
      case "dataset" -> {
        if (firstChild == null) {
          String tType = attributes.getValue("type");
          if (tType == null || !tType.startsWith("EDDGrid")) {
            throw new RuntimeException(
                "Datasets.xml error: "
                    + "The dataset defined in an "
                    + "EDDGridAggregateExistingDimension must be a subclass of EDDGrid.");
          }

          String active = attributes.getValue("active");
          String childDatasetID = attributes.getValue("datasetID");
          State state =
              HandlerFactory.getHandlerFor(
                  tType, childDatasetID, active, this, saxHandler, context, false);
          saxHandler.setState(state);
        } else {
          throw new RuntimeException(
              "Datasets.xml error: "
                  + "There can be only one <dataset> defined within an "
                  + "EDDGridAggregateExistingDimension <dataset>.");
        }
      }
      case "sourceUrls" -> {
        tSUServerType = attributes.getValue("serverType");
        tSURegex = attributes.getValue("regex");
        String tr = attributes.getValue("recursive");
        tSUPathRegex = attributes.getValue("pathRegex");
        tSURecursive = tr == null || String2.parseBoolean(tr);
      }
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
      case "sourceUrl" -> tLocalSourceUrls.add(contentStr);
      case "sourceUrls" -> tSU = contentStr;
      case "matchAxisNDigits" ->
          tMatchAxisNDigits = String2.parseInt(contentStr, DEFAULT_MATCH_AXIS_N_DIGITS);
      case "ensureAxisValuesAreEqual" ->
          tMatchAxisNDigits = String2.parseBoolean(contentStr) ? 20 : 0;
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
      case "dataset" -> {
        EDD dataset =
            new EDDGridAggregateExistingDimension(
                datasetID,
                tAccessibleTo,
                tGraphsAccessibleTo,
                tAccessibleViaWMS,
                tAccessibleViaFiles,
                tOnChange,
                tFgdcFile,
                tIso19115File,
                tDefaultDataQuery,
                tDefaultGraphQuery,
                firstChild,
                tLocalSourceUrls.toArray(),
                tSUServerType,
                tSURegex,
                tSURecursive,
                tSUPathRegex,
                tSU,
                tMatchAxisNDigits,
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
    firstChild = (EDDGrid) dataset;
  }
}
