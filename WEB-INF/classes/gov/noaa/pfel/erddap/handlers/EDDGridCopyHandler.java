package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDGrid.DEFAULT_MATCH_AXIS_N_DIGITS;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGrid;
import gov.noaa.pfel.erddap.dataset.EDDGridCopy;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridCopyHandler extends BaseGridHandler {
  private SaxParsingContext context;

  public EDDGridCopyHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private EDDGrid tSourceEdd = null;
  private int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
  private boolean checkSourceData = EDDGridCopy.defaultCheckSourceData;
  private boolean tFileTableInMemory = false;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private String tOnlySince = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (localName.equals("dataset")) {
      if (checkSourceData) {
        if (tSourceEdd != null) {
          throw new SimpleException("Cannot Have multiple Child datasets for" + datasetID);
        }
        String tType = attributes.getValue("type");
        String active = attributes.getValue("active");
        String childDatasetID = attributes.getValue("datasetID");
        State state =
            HandlerFactory.getHandlerFor(
                tType, childDatasetID, active, this, saxHandler, context, false);
        saxHandler.setState(state);
      } else {
        String2.log(
            "WARNING!!! checkSourceData is false, so EDDGridCopy datasetID="
                + datasetID
                + " is not checking the source dataset!");
        tSourceEdd = null;
        // skip the child dataset
        State state = new SkipDatasetHandler(saxHandler, this);
        saxHandler.setState(state);
      }
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
          tMatchAxisNDigits = String2.parseBoolean(contentStr) ? 20 : 0; // deprecated
      case "checkSourceData" -> checkSourceData = String2.parseBoolean(contentStr);
      case "fileTableInMemory" -> tFileTableInMemory = String2.parseBoolean(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "onlySince" -> tOnlySince = contentStr;
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  public void handleDataset(EDD dataset) {
    tSourceEdd = (EDDGrid) dataset;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDGridCopy(
        datasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tMatchAxisNDigits,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tReloadEveryNMinutes,
        tSourceEdd,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tOnlySince,
        tnThreads,
        tDimensionValuesInMemory);
  }
}
