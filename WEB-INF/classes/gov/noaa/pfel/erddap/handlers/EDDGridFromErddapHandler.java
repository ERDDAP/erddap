package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromErddap;
import gov.noaa.pfel.erddap.util.EDStatic;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridFromErddapHandler extends BaseGridHandler {
  public EDDGridFromErddapHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private int tUpdateEveryNMillis = 0;
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private boolean tSubscribeToRemoteErddapDataset = EDStatic.subscribeToRemoteErddapDataset;
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
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "subscribeToRemoteErddapDataset" ->
          tSubscribeToRemoteErddapDataset = String2.parseBoolean(contentStr);
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "redirect" -> tRedirect = String2.parseBoolean(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDGridFromErddap(
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
        tLocalSourceUrl,
        tSubscribeToRemoteErddapDataset,
        tRedirect,
        tnThreads,
        tDimensionValuesInMemory);
  }
}
