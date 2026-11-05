package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromZarr;
import org.xml.sax.Attributes;

/** SAX Handler for EDDTableFromZarr datasets. */
public class EDDTableFromZarrHandler extends BaseTableHandler {

  private String tZarrStorePath = null;
  private String tZarrGroupName = "";
  private String tRowDimensionName = null;
  private long tChunkCacheSize = -1;
  private String tAwsRegion = null;
  private String tAwsEndpoint = null;
  private String tCacheFromUrl = null;

  public EDDTableFromZarrHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    handleAttributes(localName);
    handleDataVariables(localName);
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "zarrStorePath", "sourceUrl" -> tZarrStorePath = contentStr;
      case "zarrGroupName", "groupName" -> tZarrGroupName = contentStr;
      case "rowDimensionName", "rowDimension" -> tRowDimensionName = contentStr;
      case "chunkCacheSize" -> tChunkCacheSize = String2.parseLong(contentStr);
      case "awsRegion" -> tAwsRegion = contentStr;
      case "awsEndpoint" -> tAwsEndpoint = contentStr;
      case "cacheFromUrl" -> tCacheFromUrl = contentStr;
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    return new EDDTableFromZarr(
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
        tDataVariables,
        tReloadEveryNMinutes,
        tZarrStorePath,
        tZarrGroupName,
        tRowDimensionName,
        tChunkCacheSize,
        tAwsRegion,
        tAwsEndpoint,
        tCacheFromUrl);
  }
}
