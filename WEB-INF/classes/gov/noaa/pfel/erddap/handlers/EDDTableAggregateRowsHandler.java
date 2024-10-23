package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableAggregateRows;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableAggregateRowsHandler extends BaseTableHandler {
  private SaxParsingContext context;

  public EDDTableAggregateRowsHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, datasetID, completeState);
    this.context = context;
  }

  private ArrayList<EDDTable> tChildren = new ArrayList<>();
  private int tUpdateEveryNMillis = 0;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    handleAttributes(localName);
    if ("dataset".equals(localName)) {
      String tType = attributes.getValue("type");
      if (tType == null || !tType.startsWith("EDDTable")) {
        throw new SimpleException(
            "type=\""
                + tType
                + "\" is not allowed for the dataset within the EDDTableAggregateRows. "
                + "The type MUST start with \"EDDTable\".");
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
  public void handleDataset(EDD dataset) {
    tChildren.add((EDDTable) dataset);
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    EDDTable[] ttChildren = new EDDTable[tChildren.size()];
    ttChildren = tChildren.toArray(ttChildren);
    return new EDDTableAggregateRows(
        context.getErddap(),
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
        tGlobalAttributes,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        ttChildren);
  }
}
