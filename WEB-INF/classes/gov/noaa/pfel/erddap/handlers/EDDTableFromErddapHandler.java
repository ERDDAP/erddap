package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromErddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromErddapHandler extends BaseTableHandler {

  public EDDTableFromErddapHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private boolean tAccessibleViaFiles = EDStatic.config.defaultAccessibleViaFiles;
  private boolean tSubscribeToRemoteErddapDataset = EDStatic.config.subscribeToRemoteErddapDataset;
  private boolean tRedirect = true;
  private String tLocalSourceUrl = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {}

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "subscribeToRemoteErddapDataset" ->
          tSubscribeToRemoteErddapDataset = String2.parseBoolean(contentStr);
      case "redirect" -> tRedirect = String2.parseBoolean(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDTableFromErddap(
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
        tReloadEveryNMinutes,
        tLocalSourceUrl,
        tSubscribeToRemoteErddapDataset,
        tRedirect);
  }
}
