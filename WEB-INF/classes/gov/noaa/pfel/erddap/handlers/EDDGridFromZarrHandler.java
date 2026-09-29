package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromZarr;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import org.xml.sax.Attributes;

/**
 * SAX Handler for EDDGridFromZarr datasets.
 */
public class EDDGridFromZarrHandler extends BaseGridHandler {

  private String tZarrStorePath = null;
  private String tZarrGroupName = "";
  private long tChunkCacheSize = -1;
  private int tUpdateEveryNMillis = 0;

  public EDDGridFromZarrHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    handleAttributes(localName);
    handleAxisVariable(localName);
    handleDataVariables(localName);
    if ("altitudeMetersPerSourceUnit".equals(localName)) {
      throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
    }
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "zarrStorePath", "sourceUrl" -> tZarrStorePath = contentStr;
      case "zarrGroupName", "groupName" -> tZarrGroupName = contentStr;
      case "chunkCacheSize" -> tChunkCacheSize = String2.parseLong(contentStr);
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDGridFromZarr(
        datasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tGlobalAttributes,
        tAxisVariables,
        tDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tZarrStorePath,
        tZarrGroupName,
        tChunkCacheSize,
        tnThreads,
        tDimensionValuesInMemory);
  }
}
