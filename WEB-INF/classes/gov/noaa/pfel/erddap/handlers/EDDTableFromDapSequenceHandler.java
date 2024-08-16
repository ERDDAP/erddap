package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromDapSequence;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromDapSequenceHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;

  public EDDTableFromDapSequenceHandler(
      SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private ArrayList<Object[]> tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private String tLocalSourceUrl = null;
  private String tOuterSequenceName = null;
  private String tInnerSequenceName = null;
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private boolean tSourceCanConstrainStringEQNE = true;
  private boolean tSourceCanConstrainStringGTLT = true;
  private String tSourceCanConstrainStringRegex = null;
  private boolean tSkipDapperSpacerRows = false;
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
      case "sourceUrl" -> tLocalSourceUrl = contentStr;
      case "outerSequenceName" -> tOuterSequenceName = contentStr;
      case "innerSequenceName" -> tInnerSequenceName = contentStr;
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);
      case "sourceCanConstrainStringEQNE" ->
          tSourceCanConstrainStringEQNE = String2.parseBoolean(contentStr);
      case "sourceCanConstrainStringGTLT" ->
          tSourceCanConstrainStringGTLT = String2.parseBoolean(contentStr);
      case "sourceCanConstrainStringRegex" -> tSourceCanConstrainStringRegex = contentStr;
      case "skipDapperSpacerRows" -> tSkipDapperSpacerRows = String2.parseBoolean(contentStr);
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
            new EDDTableFromDapSequence(
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
                ttDataVariables,
                tReloadEveryNMinutes,
                tLocalSourceUrl,
                tOuterSequenceName,
                tInnerSequenceName,
                tSourceNeedsExpandedFP_EQ,
                tSourceCanConstrainStringEQNE,
                tSourceCanConstrainStringGTLT,
                tSourceCanConstrainStringRegex,
                tSkipDapperSpacerRows);

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
