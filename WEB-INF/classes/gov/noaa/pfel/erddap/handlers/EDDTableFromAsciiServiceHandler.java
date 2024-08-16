package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromAsciiServiceNOS;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromAsciiServiceHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;
  private String datasetType;

  public EDDTableFromAsciiServiceHandler(
      SaxHandler saxHandler, String datasetID, State completeState, String datasetType) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.datasetType = datasetType;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private ArrayList<Object[]> tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private String tLocalSourceUrl = null;
  private String tBeforeData[] = new String[11];
  private String tAfterData = null;
  private String tNoData = null;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private String tAddVariablesWhere = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    switch (localName) {
      case "addAttributes" -> {
        State state = new AddAttributesHandler(saxHandler, tGlobalAttributes, this);
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
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "beforeData1",
              "beforeData2",
              "beforeData3",
              "beforeData4",
              "beforeData5",
              "beforeData6",
              "beforeData7",
              "beforeData8",
              "beforeData9",
              "beforeData10" ->
          tBeforeData[String2.parseInt(localName.substring(10, localName.length() - 1))] =
              contentStr;
      case "afterData" -> tAfterData = contentStr;
      case "noData" -> tNoData = contentStr;
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "dataset" -> {
        int ndv = tDataVariables.size();
        Object[][] ttDataVariables = new Object[ndv][];
        ttDataVariables = tDataVariables.toArray(ttDataVariables);

        EDD dataset = getDataset(ttDataVariables);

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
  }

  private EDD getDataset(Object[][] ttDataVariables) throws Throwable {
    EDD dataset;

    if (datasetType.equals("\"EDDTableFromAsciiServiceNOS\"")) {
      dataset =
          new EDDTableFromAsciiServiceNOS(
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
              ttDataVariables,
              tReloadEveryNMinutes,
              tLocalSourceUrl,
              tBeforeData,
              tAfterData,
              tNoData);
    } else {
      throw new Exception(
          "type=\""
              + datasetType
              + "\" needs to be added to EDDTableFromAsciiService.fromXml at end.");
    }
    return dataset;
  }
}
