package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDD.DEFAULT_RELOAD_EVERY_N_MINUTES;

import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromEDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridFromEDDTableHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private SaxParsingContext context;
  private String datasetID;

  public EDDGridFromEDDTableHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.context = context;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private boolean tAccessibleViaWMS = true;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private ArrayList<Object[]> tAxisVariables = new ArrayList();
  private ArrayList<Object[]> tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
  private int tUpdateEveryNMillis = 0;
  private String tLocalSourceUrl = null;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private int tnThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
  private boolean tDimensionValuesInMemory = true;
  private EDDTable tEDDTable = null;
  private int tGapThreshold = 1000;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    switch (localName) {
      case "dataset" -> {
        if (tEDDTable != null) {
          throw new SimpleException("Cannot Have multiple Child datasets for" + datasetID);
        }
        String tType = attributes.getValue("type");
        if (tType == null || !tType.startsWith("EDDTable")) {
          throw new SimpleException(
              "type=\""
                  + tType
                  + "\" is not allowed for the dataset within the EDDGridFromEDDTable. "
                  + "The type MUST start with \"EDDTable\".");
        }
        String tableDatasetID = attributes.getValue("datasetID");
        if (datasetID == null || tableDatasetID == null || datasetID.equals(tableDatasetID)) {
          throw new SimpleException(
              "The inner eddTable datasetID must be different from the "
                  + "outer EDDGridFromEDDTable datasetID.");
        }
        String active = attributes.getValue("active");
        State state =
            HandlerFactory.getHandlerFor(
                tType, tableDatasetID, active, this, saxHandler, context, false);
        saxHandler.setState(state);
      }
      case "addAttributes" -> {
        State state = new AddAttributesHandler(saxHandler, tGlobalAttributes, this);
        saxHandler.setState(state);
      }
      case "altitudeMetersPerSourceUnit" ->
          throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
      case "axisVariable" -> {
        State state = new AxisVariableHandler(saxHandler, tAxisVariables, this);
        saxHandler.setState(state);
      }
      case "dataVariable" -> {
        State state = new DataVariableHandler(saxHandler, tDataVariables, this);
        saxHandler.setState(state);
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
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
      case "gapThreshold" -> tGapThreshold = String2.parseInt(contentStr);
      case "dataset" -> {
        int nav = tAxisVariables.size();
        Object[][] ttAxisVariables = nav == 0 ? null : new Object[nav][];
        ttAxisVariables = tAxisVariables.toArray(ttAxisVariables);
        int ndv = tDataVariables.size();
        Object[][] ttDataVariables = new Object[ndv][];
        ttDataVariables = tDataVariables.toArray(ttDataVariables);

        EDD dataset =
            new EDDGridFromEDDTable(
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
                tGlobalAttributes,
                ttAxisVariables,
                ttDataVariables,
                tReloadEveryNMinutes,
                tUpdateEveryNMillis,
                tGapThreshold,
                tEDDTable,
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
    tEDDTable = (EDDTable) dataset;
  }
}
