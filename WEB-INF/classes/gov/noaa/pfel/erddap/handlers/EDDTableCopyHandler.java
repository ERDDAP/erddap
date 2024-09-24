package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDTableCopy.defaultCheckSourceData;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableCopy;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableCopyHandler extends BaseTableHandler {
  private SaxParsingContext context;

  public EDDTableCopyHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private EDDTable tSourceEdd = null;
  private String tExtractDestinationNames = "";
  private String tOrderExtractBy = "";
  private boolean checkSourceData = defaultCheckSourceData;
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private boolean tFileTableInMemory = false;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private int tStandardizeWhat = Integer.MAX_VALUE;
  private int tnThreads = -1;

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
            "WARNING!!! checkSourceData is false, so EDDTableCopy datasetID="
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
  public void handleDataset(EDD dataset) {
    tSourceEdd = (EDDTable) dataset;
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "extractDestinationNames" -> tExtractDestinationNames = contentStr;
      case "orderExtractBy" -> tOrderExtractBy = contentStr;
      case "checkSourceData" -> checkSourceData = String2.parseBoolean(contentStr);
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);
      case "fileTableInMemory" -> tFileTableInMemory = String2.parseBoolean(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "standardizeWhat" -> tStandardizeWhat = String2.parseInt(contentStr);
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDTableCopy(
        datasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tReloadEveryNMinutes,
        tStandardizeWhat,
        tExtractDestinationNames,
        tOrderExtractBy,
        tSourceNeedsExpandedFP_EQ,
        tSourceEdd,
        tFileTableInMemory,
        tAccessibleViaFiles,
        tnThreads);
  }
}
