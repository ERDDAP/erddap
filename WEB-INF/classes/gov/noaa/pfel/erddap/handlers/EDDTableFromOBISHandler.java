package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromOBIS;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromOBISHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;

  public EDDTableFromOBISHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private String tLocalSourceUrl = null, tSourceCode = null;
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private double tLongitudeSourceMinimum = Double.NaN;
  private double tLongitudeSourceMaximum = Double.NaN;
  private double tLatitudeSourceMinimum = Double.NaN;
  private double tLatitudeSourceMaximum = Double.NaN;
  private double tAltitudeSourceMinimum = Double.NaN;
  private double tAltitudeSourceMaximum = Double.NaN;
  private String tTimeSourceMinimum = "";
  private String tTimeSourceMaximum = "";
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private String tAddVariablesWhere = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    if (localName.equals("addAttributes")) {
      State state = new AddAttributesHandler(saxHandler, tGlobalAttributes, this);
      saxHandler.setState(state);
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
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "sourceCode" -> tSourceCode = contentStr;
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
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
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "dataset" -> {
        EDD dataset =
            new EDDTableFromOBIS(
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

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
