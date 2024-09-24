package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromFileNames;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromFileNamesHandler extends BaseTableHandler {

  public EDDTableFromFileNamesHandler(
      SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private String tFileDir = null;
  private String tFileNameRegex = null;
  private boolean tRecursive = false;
  private String tPathRegex = ".*";

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    handleAttributes(localName);
    handleDataVariables(localName);
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "fileDir" -> tFileDir = contentStr;
      case "fileNameRegex" -> tFileNameRegex = contentStr;
      case "recursive" -> tRecursive = String2.parseBoolean(contentStr);
      case "pathRegex" -> tPathRegex = contentStr;
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttDataVariables = convertDataVariablesToArray();

    return new EDDTableFromFileNames(
        datasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddVariablesWhere,
        tGlobalAttributes,
        ttDataVariables,
        tReloadEveryNMinutes,
        tFileDir,
        tFileNameRegex,
        tRecursive,
        tPathRegex);
  }
}
