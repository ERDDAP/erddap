package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromCassandra;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromCassandraHandler extends StateWithParent {
  private StringBuilder content = new StringBuilder();
  private String datasetID;

  public EDDTableFromCassandraHandler(
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
  private String tKeyspace = null;
  private String tTableName = null;
  private String tPartitionKeySourceNames = null;
  private String tClusterColumnSourceNames = null;
  private String tIndexColumnSourceNames = null;
  private double tMaxRequestFraction = 1;
  private String tColumnNameQuotes = "";
  private StringArray tConnectionProperties = new StringArray();
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private String tAddVariablesWhere = null;
  private String tPartitionKeyCSV = null;

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
      case "connectionProperty" -> tConnectionProperties.add(contentStr);
      case "keyspace" -> tKeyspace = contentStr;
      case "tableName" -> tTableName = contentStr;
      case "partitionKeySourceNames" -> tPartitionKeySourceNames = contentStr;
      case "clusterColumnSourceNames" -> tClusterColumnSourceNames = contentStr;
      case "indexColumnSourceNames" -> tIndexColumnSourceNames = contentStr;
      case "maxRequestFraction" -> tMaxRequestFraction = String2.parseDouble(contentStr);
      case "columnNameQuotes" -> tColumnNameQuotes = contentStr;
      case "partitionKeyCSV" -> tPartitionKeyCSV = contentStr;
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
            new EDDTableFromCassandra(
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
                tConnectionProperties.toArray(),
                tKeyspace,
                tTableName,
                tPartitionKeySourceNames,
                tClusterColumnSourceNames,
                tIndexColumnSourceNames,
                tPartitionKeyCSV,
                tMaxRequestFraction,
                tColumnNameQuotes,
                tSourceNeedsExpandedFP_EQ);

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }
}
