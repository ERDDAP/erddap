package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDD.DEFAULT_RELOAD_EVERY_N_MINUTES;

import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableAggregateRows;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableAggregateRowsHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;
  private SaxParsingContext context;

  public EDDTableAggregateRowsHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.context = context;
  }

  private ArrayList<EDDTable> tChildren = new ArrayList();
  private com.cohort.array.Attributes tAddGlobalAttributes = new com.cohort.array.Attributes();
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
  private int tUpdateEveryNMillis = 0;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private String tAddVariablesWhere = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    switch (localName) {
      case "dataset" -> {
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
      case "addAttributes" -> {
        State state = new AddAttributesHandler(saxHandler, tAddGlobalAttributes, this);
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
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "dataset" -> {
        EDDTable[] ttChildren = new EDDTable[tChildren.size()];
        ttChildren = tChildren.toArray(ttChildren);

        EDD dataset =
            new EDDTableAggregateRows(
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
                tAddGlobalAttributes,
                tReloadEveryNMinutes,
                tUpdateEveryNMillis,
                ttChildren);
        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }

  @Override
  public void handleDataset(EDD dataset) {
    tChildren.add((EDDTable) dataset);
  }
}
