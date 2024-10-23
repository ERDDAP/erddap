package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromEDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridFromEDDTableHandler extends BaseGridHandler {
  private SaxParsingContext context;

  public EDDGridFromEDDTableHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private int tUpdateEveryNMillis = 0;
  private EDDTable tEDDTable = null;
  private int tGapThreshold = 1000;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    handleAttributes(localName);
    handleDataVariables(localName);
    handleAxisVariable(localName);
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
      case "altitudeMetersPerSourceUnit" ->
          throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
    }
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "gapThreshold" -> tGapThreshold = String2.parseInt(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  public void handleDataset(EDD dataset) {
    tEDDTable = (EDDTable) dataset;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttAxisVariables = convertAxisVariablesToArray();
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return new EDDGridFromEDDTable(
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
  }
}
