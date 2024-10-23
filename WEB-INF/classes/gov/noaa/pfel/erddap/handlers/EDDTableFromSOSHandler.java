package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromSOS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromSOSHandler extends BaseTableHandler {
  public EDDTableFromSOSHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private String tSosServerType = "";
  private String tStationIdSourceName = "station_id";
  private String tLongitudeSourceName = null;
  private String tLatitudeSourceName = null;
  private String tAltitudeSourceName = null;
  private double tAltitudeMetersPerSourceUnit = 1;
  private double tAltitudeSourceMinimum = Double.NaN;
  private double tAltitudeSourceMaximum = Double.NaN;
  private String tTimeSourceName = null;
  String tTimeSourceFormat = null;

  private String tLocalSourceUrl = null, tObservationOfferingIdRegex = null;
  private boolean tRequestObservedPropertiesSeparately = false;
  private String tResponseFormat = null;
  private String tBBoxOffering = null;
  private String tBBoxParameter = null;
  private String tSosVersion = null;
  private boolean tSourceNeedsExpandedFP_EQ = true;

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
      case "sosVersion" -> tSosVersion = contentStr;
      case "sosServerType" -> tSosServerType = contentStr;
      case "stationIdSourceName" -> tStationIdSourceName = contentStr;
      case "longitudeSourceName" -> tLongitudeSourceName = contentStr;
      case "latitudeSourceName" -> tLatitudeSourceName = contentStr;
      case "altitudeSourceName" -> tAltitudeSourceName = contentStr;
      case "altitudeSourceMinimum" -> tAltitudeSourceMinimum = String2.parseDouble(contentStr);
      case "altitudeSourceMaximum" -> tAltitudeSourceMaximum = String2.parseDouble(contentStr);
      case "altitudeMetersPerSourceUnit" ->
          tAltitudeMetersPerSourceUnit = String2.parseDouble(contentStr);
      case "timeSourceName" -> tTimeSourceName = contentStr;
      case "timeSourceFormat" -> tTimeSourceFormat = contentStr;
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "observationOfferingIdRegex" -> tObservationOfferingIdRegex = contentStr;
      case "requestObservedPropertiesSeparately" ->
          tRequestObservedPropertiesSeparately = contentStr.equals("true");
      case "responseFormat" -> tResponseFormat = contentStr;
      case "bboxOffering" -> tBBoxOffering = contentStr;
      case "bboxParameter" -> tBBoxParameter = contentStr;
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);

      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return new EDDTableFromSOS(
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
        tSosServerType,
        tStationIdSourceName,
        tLongitudeSourceName,
        tLatitudeSourceName,
        tAltitudeSourceName,
        tAltitudeSourceMinimum,
        tAltitudeSourceMaximum,
        tAltitudeMetersPerSourceUnit,
        tTimeSourceName,
        tTimeSourceFormat,
        ttDataVariables,
        tReloadEveryNMinutes,
        tLocalSourceUrl,
        tSosVersion,
        tObservationOfferingIdRegex,
        tRequestObservedPropertiesSeparately,
        tResponseFormat,
        tBBoxOffering,
        tBBoxParameter,
        tSourceNeedsExpandedFP_EQ);
  }
}
