package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromAsciiServiceNOS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromAsciiServiceHandler extends BaseTableHandler {
  private String datasetType;

  public EDDTableFromAsciiServiceHandler(
      SaxHandler saxHandler, String datasetID, State completeState, String datasetType) {
    super(saxHandler, datasetID, completeState);
    this.datasetType = datasetType;
  }

  private String tLocalSourceUrl = null;
  private String tBeforeData[] = new String[11];
  private String tAfterData = null;
  private String tNoData = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    handleAttributes(localName);
    handleDataVariables(localName);
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

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
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
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return getDataset(ttDataVariables);
  }
}
