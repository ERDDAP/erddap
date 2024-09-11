package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromSOS;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromSOSHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;

  public EDDTableFromSOSHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
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
  private ArrayList<Object[]> tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private String tLocalSourceUrl = null, tObservationOfferingIdRegex = null;
  private boolean tRequestObservedPropertiesSeparately = false;
  private String tResponseFormat = null;
  private String tBBoxOffering = null;
  private String tBBoxParameter = null;
  private String tSosVersion = null;
  private boolean tSourceNeedsExpandedFP_EQ = true;
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
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "observationOfferingIdRegex" -> tObservationOfferingIdRegex = contentStr;
      case "requestObservedPropertiesSeparately" ->
          tRequestObservedPropertiesSeparately = contentStr.equals("true");
      case "responseFormat" -> tResponseFormat = contentStr;
      case "bboxOffering" -> tBBoxOffering = contentStr;
      case "bboxParameter" -> tBBoxParameter = contentStr;
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "dataset" -> {
        Object[][] ttDataVariables = new Object[tDataVariables.size()][];
        ttDataVariables = tDataVariables.toArray(ttDataVariables);

        EDD dataset =
            new EDDTableFromSOS(
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

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
