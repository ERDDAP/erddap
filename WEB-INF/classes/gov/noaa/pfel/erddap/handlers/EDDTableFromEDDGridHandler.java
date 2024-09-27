package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDTableFromEDDGrid;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;

public class EDDTableFromEDDGridHandler extends BaseTableHandler {
  private SaxParsingContext context;

  public EDDTableFromEDDGridHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private EDDGrid tChildDataset = null;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    handleAttributes(localName);
    if ("dataset".equals(localName)) {
      if (tChildDataset == null) {
        String tType = attributes.getValue("type");
        if (tType == null || !tType.startsWith("EDDGrid")) {
          throw new SimpleException(
              "type=\""
                  + tType
                  + "\" is not allowed for the dataset within the EDDTableFromEDDGrid. "
                  + "The type MUST start with \"EDDGrid\".");
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
                + "EDDGridFromEDDTable <dataset>.");
      }
    }
  }

  @Override
  public void handleDataset(EDD dataset) {
    tChildDataset = (EDDGrid) dataset;
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDTableFromEDDGrid(
        context.getErddap(),
        datasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaFiles,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        tReloadEveryNMinutes,
        tChildDataset);
  }
}
