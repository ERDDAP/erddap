package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDD.DEFAULT_RELOAD_EVERY_N_MINUTES;

import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDGridFromDap;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridFromDapHandler extends State {
  private StringBuilder content = new StringBuilder();
  private String datasetID;
  private State completeState;

  public EDDGridFromDapHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler);
    this.datasetID = datasetID;
    this.completeState = completeState;
  }

  public com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  public String tAccessibleTo = null;
  public String tGraphsAccessibleTo = null;
  public boolean tAccessibleViaWMS = true;
  public StringArray tOnChange = new StringArray();
  public String tFgdcFile = null;
  public String tIso19115File = null;
  public ArrayList tAxisVariables = new ArrayList();
  public ArrayList tDataVariables = new ArrayList();
  public int tReloadEveryNMinutes = DEFAULT_RELOAD_EVERY_N_MINUTES;
  public int tUpdateEveryNMillis = 0;
  public String tLocalSourceUrl = null;
  public String tDefaultDataQuery = null;
  public String tDefaultGraphQuery = null;
  public int tnThreads = -1;
  public boolean tDimensionValuesInMemory = true;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    switch (localName) {
      case "addAttributes" -> {
        State state = new AddAttributesHandler(saxHandler, tGlobalAttributes, this);
        saxHandler.setState(state);
      }
      case "altitudeMetersPerSourceUnit" ->
          throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
      case "axisVariable" -> {
        State state = new AxisVariableHandler(saxHandler, tAxisVariables, this);
        saxHandler.setState(state);
      }
      case "dataVariable" -> {
        State state = new DataVariableHandler(saxHandler, tDataVariables, this);
        saxHandler.setState(state);
      }
      default -> String2.log("Unexpected start tag: " + localName);
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
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
      case "dataset" -> {
        int nav = tAxisVariables.size();
        Object[][] ttAxisVariables = nav == 0 ? null : new Object[nav][];
        for (int i = 0; i < tAxisVariables.size(); i++) {
          ttAxisVariables[i] = (Object[]) tAxisVariables.get(i);
        }
        int ndv = tDataVariables.size();
        Object[][] ttDataVariables = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++) {
          ttDataVariables[i] = (Object[]) tDataVariables.get(i);
        }

        EDD dataset =
            new EDDGridFromDap(
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

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
