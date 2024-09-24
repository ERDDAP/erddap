package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromDap;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import org.xml.sax.Attributes;

public class EDDGridFromDapHandler extends BaseGridHandler {
  public EDDGridFromDapHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private int tUpdateEveryNMillis = 0;
  private String tLocalSourceUrl = null;

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
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttAxisVariables = convertAxisVariablesToArray();
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return new EDDGridFromDap(
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
        ttAxisVariables,
        ttDataVariables,
        tReloadEveryNMinutes,
        tUpdateEveryNMillis,
        tLocalSourceUrl,
        tnThreads,
        tDimensionValuesInMemory);
  }
}
