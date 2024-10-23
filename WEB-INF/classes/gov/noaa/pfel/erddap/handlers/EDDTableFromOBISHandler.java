package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromOBIS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromOBISHandler extends BaseTableHandler {

  public EDDTableFromOBISHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private String tLocalSourceUrl = null, tSourceCode = null;
  private double tLongitudeSourceMinimum = Double.NaN;
  private double tLongitudeSourceMaximum = Double.NaN;
  private double tLatitudeSourceMinimum = Double.NaN;
  private double tLatitudeSourceMaximum = Double.NaN;
  private double tAltitudeSourceMinimum = Double.NaN;
  private double tAltitudeSourceMaximum = Double.NaN;
  private String tTimeSourceMinimum = "";
  private String tTimeSourceMaximum = "";
  private boolean tSourceNeedsExpandedFP_EQ = true;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    handleAttributes(localName);
  }

  @Override
  protected boolean handleEndElement(String contentStr, String localName) {
    if (super.handleEndElement(contentStr, localName)) {
      return true;
    }
    switch (localName) {
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "sourceCode" -> tSourceCode = contentStr;
      case "longitudeSourceMinimum" -> tLongitudeSourceMinimum = String2.parseDouble(contentStr);
      case "longitudeSourceMaximum" -> tLongitudeSourceMaximum = String2.parseDouble(contentStr);
      case "latitudeSourceMinimum" -> tLatitudeSourceMinimum = String2.parseDouble(contentStr);
      case "latitudeSourceMaximum" -> tLatitudeSourceMaximum = String2.parseDouble(contentStr);
      case "altitudeSourceMinimum" -> tAltitudeSourceMinimum = String2.parseDouble(contentStr);
      case "altitudeSourceMaximum" -> tAltitudeSourceMaximum = String2.parseDouble(contentStr);
      case "timeSourceMinimum" -> tTimeSourceMinimum = contentStr;
      case "timeSourceMaximum" -> tTimeSourceMaximum = contentStr;
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
    return new EDDTableFromOBIS(
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
        tLocalSourceUrl,
        tSourceCode,
        tReloadEveryNMinutes,
        tLongitudeSourceMinimum,
        tLongitudeSourceMaximum,
        tLatitudeSourceMinimum,
        tLatitudeSourceMaximum,
        tAltitudeSourceMinimum,
        tAltitudeSourceMaximum,
        tTimeSourceMinimum,
        tTimeSourceMaximum,
        tSourceNeedsExpandedFP_EQ);
  }
}
