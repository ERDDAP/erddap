package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDTableCopy.defaultCheckSourceData;

import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTable;
import gov.noaa.pfel.erddap.dataset.EDDTableCopy;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableCopyHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;
  private SaxParsingContext context;

  public EDDTableCopyHandler(
      SaxHandler saxHandler, String datasetID, State completeState, SaxParsingContext context) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.context = context;
  }

  private EDDTable tSourceEdd = null;
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private String tExtractDestinationNames = "";
  private String tOrderExtractBy = "";
  private boolean checkSourceData = defaultCheckSourceData;
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private boolean tFileTableInMemory = false;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private String tAddVariablesWhere = null;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private int tStandardizeWhat = Integer.MAX_VALUE;
  private int tnThreads = -1;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (localName.equals("dataset")) {
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
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "extractDestinationNames" -> tExtractDestinationNames = contentStr;
      case "orderExtractBy" -> tOrderExtractBy = contentStr;
      case "checkSourceData" -> checkSourceData = String2.parseBoolean(contentStr);
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);
      case "fileTableInMemory" -> tFileTableInMemory = String2.parseBoolean(contentStr);
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "standardizeWhat" -> tStandardizeWhat = String2.parseInt(contentStr);
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dataset" -> {
        EDD dataset =
            new EDDTableCopy(
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

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }

  @Override
  public void handleDataset(EDD dataset) {
    tSourceEdd = (EDDTable) dataset;
  }
}
