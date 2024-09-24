package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromCassandra;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromCassandraHandler extends BaseTableHandler {
  public EDDTableFromCassandraHandler(
      SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

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
  private String tPartitionKeyCSV = null;

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
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return new EDDTableFromCassandra(
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
  }
}
