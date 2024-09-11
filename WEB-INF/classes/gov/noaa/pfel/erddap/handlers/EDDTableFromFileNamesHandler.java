package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromFileNames;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromFileNamesHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;

  public EDDTableFromFileNamesHandler(
      SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private ArrayList<Object[]> tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private String tFileDir = null;
  private String tFileNameRegex = null;
  private boolean tRecursive = false;
  private String tPathRegex = ".*";
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
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
      case "accessibleTo" -> tAccessibleTo = contentStr;
      case "graphsAccessibleTo" -> tGraphsAccessibleTo = contentStr;
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "fileDir" -> tFileDir = contentStr;
      case "fileNameRegex" -> tFileNameRegex = contentStr;
      case "recursive" -> tRecursive = String2.parseBoolean(contentStr);
      case "pathRegex" -> tPathRegex = contentStr;
      case "dataset" -> {
        Object[][] ttDataVariables = new Object[tDataVariables.size()][];
        ttDataVariables = tDataVariables.toArray(ttDataVariables);

        EDD dataset =
            new EDDTableFromFileNames(
                datasetID,
                tAccessibleTo,
                tGraphsAccessibleTo,
                tOnChange,
                tFgdcFile,
                tIso19115File,
                tDefaultDataQuery,
                tDefaultGraphQuery,
                tAddVariablesWhere,
                tGlobalAttributes,
                ttDataVariables,
                tReloadEveryNMinutes,
                tFileDir,
                tFileNameRegex,
                tRecursive,
                tPathRegex);

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
