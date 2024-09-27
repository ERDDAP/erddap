package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridLon0360;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridLon0360Handler extends BaseGridHandler {
  private SaxParsingContext context;

  public EDDGridLon0360Handler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private EDDGrid tChildDataset = null;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private int tUpdateEveryNMillis = Integer.MAX_VALUE;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (localName.equals("dataset")) {
      if (tChildDataset == null) {
        String tType = attributes.getValue("type");
        if (tType == null || !tType.startsWith("EDDGrid")) {
          throw new RuntimeException(
              "Datasets.xml error: "
                  + "The dataset defined in an "
                  + "EDDGridLonPM360 must be a subclass of EDDGrid.");
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
                + "EDDGridLonPM360 <dataset>.");
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
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDGridLon0360(
        context.getErddap(),
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
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tChildDataset,
        tnThreads,
        tDimensionValuesInMemory);
  }
}
