package gov.noaa.pfel.erddap.handlers;

import com.cohort.array.StringArray;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromDatabase;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromDatabaseHandler extends BaseTableHandler {

  public EDDTableFromDatabaseHandler(SaxHandler saxHandler, String datasetID, State completeState) {
    super(saxHandler, datasetID, completeState);
  }

  private String tDataSourceName = null;
  private String tLocalSourceUrl = null;
  private String tDriverName = null;
  private String tCatalogName = "";
  private String tSchemaName = "";
  private String tTableName = null;
  private String tColumnNameQuotes = "\"";
  private String[] tOrderBy = new String[0];
  private StringArray tConnectionProperties = new StringArray();
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private String tSourceCanOrderBy = "no";
  private String tSourceCanDoDistinct = "no";

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
      case "dataSourceName" -> tDataSourceName = contentStr;
      case "driverName" -> tDriverName = contentStr;
      case "connectionProperty" -> tConnectionProperties.add(contentStr);
      case "catalogName" -> tCatalogName = contentStr;
      case "schemaName" -> tSchemaName = contentStr;
      case "tableName" -> tTableName = contentStr;
      case "columnNameQuotes" -> tColumnNameQuotes = contentStr;
      case "orderBy" -> {
        if (!contentStr.isEmpty()) tOrderBy = String2.split(contentStr, ',');
      }
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);
      case "sourceCanOrderBy" -> tSourceCanOrderBy = contentStr;
      case "sourceCanDoDistinct" -> tSourceCanDoDistinct = contentStr;
      default -> {
        return false;
      }
    }
    return true;
  }

  @Override
  protected EDD buildDataset() throws Throwable {
    Object[][] ttDataVariables = convertDataVariablesToArray();
    return new EDDTableFromDatabase(
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
        tDataSourceName,
        tLocalSourceUrl,
        tDriverName,
        tConnectionProperties.toArray(),
        tCatalogName,
        tSchemaName,
        tTableName,
        tColumnNameQuotes,
        tOrderBy,
        tSourceNeedsExpandedFP_EQ,
        tSourceCanOrderBy,
        tSourceCanDoDistinct);
  }
}
