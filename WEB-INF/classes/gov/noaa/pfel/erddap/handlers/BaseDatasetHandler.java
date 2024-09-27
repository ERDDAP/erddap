package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import java.util.ArrayList;
import org.xml.sax.SAXException;

public abstract class BaseDatasetHandler extends StateWithParent {
  protected StringBuilder content = new StringBuilder();
  protected String datasetID;

  public BaseDatasetHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
  }

  protected int tReloadEveryNMinutes = Integer.MAX_VALUE;
  protected String tAccessibleTo = null;
  protected String tGraphsAccessibleTo = null;
  protected StringArray tOnChange = new StringArray();
  protected String tFgdcFile = null;
  protected String tIso19115File = null;
  protected String tDefaultDataQuery = null;
  protected String tDefaultGraphQuery = null;

  protected com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  protected ArrayList<Object[]> tDataVariables = new ArrayList<>();

  protected void handleAttributes(String localName) {
    if ("addAttributes".equals(localName)) {
      State state = new AddAttributesHandler(saxHandler, tGlobalAttributes, this);
      saxHandler.setState(state);
    }
  }

  protected void handleDataVariables(String localName) {
    if ("dataVariable".equals(localName)) {
      State state = new DataVariableHandler(saxHandler, tDataVariables, this);
      saxHandler.setState(state);
    }
  }

  protected Object[][] convertDataVariablesToArray() {
    Object[][] ttDataVariables = new Object[tDataVariables.size()][];
    ttDataVariables = tDataVariables.toArray(ttDataVariables);
    return ttDataVariables;
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
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "dataset" -> {
        EDD dataset = this.buildDataset();
        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> {
        if (!handleEndElement(contentStr, localName)) {
          String2.log("Unexpected end tag: " + localName);
        }
      }
    }
    content.setLength(0);
  }

  protected abstract boolean handleEndElement(String contentStr, String localName);

  protected abstract EDD buildDataset() throws Throwable;
}
