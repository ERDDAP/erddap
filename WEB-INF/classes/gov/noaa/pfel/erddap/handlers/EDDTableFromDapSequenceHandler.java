package gov.noaa.pfel.erddap.handlers;

import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromDapSequence;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromDapSequenceHandler extends BaseTableHandler {

  public EDDTableFromDapSequenceHandler(
      SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private String tLocalSourceUrl = null;
  private String tOuterSequenceName = null;
  private String tInnerSequenceName = null;
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private boolean tSourceCanConstrainStringEQNE = true;
  private boolean tSourceCanConstrainStringGTLT = true;
  private String tSourceCanConstrainStringRegex = null;
  private boolean tSkipDapperSpacerRows = false;

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
      default -> {
        return true;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return new EDDTableFromDapSequence(
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
  }
}
