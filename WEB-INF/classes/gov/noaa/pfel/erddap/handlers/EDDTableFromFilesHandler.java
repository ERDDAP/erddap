package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDTableFromFiles.MF_LAST;

import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.EDD;
import gov.noaa.pfel.erddap.dataset.EDDTableFromAsciiFiles;
import gov.noaa.pfel.erddap.dataset.EDDTableFromNcFiles;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDTableFromFilesHandler extends State {
  private String datasetID;
  private State completeState;
  private StringBuilder content = new StringBuilder();
  private String datasetType;

  public EDDTableFromFilesHandler(
      SaxHandler saxHandler, String datasetID, State completeState, String datasetType) {
    super(saxHandler);
    this.datasetID = datasetID;
    this.completeState = completeState;
    this.datasetType = datasetType;
  }

  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private ArrayList tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private int tUpdateEveryNMillis = 0;
  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private StringArray tOnChange = new StringArray();
  private boolean tFileTableInMemory = false;
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private String tSosOfferingPrefix = null;
  private String tFileDir = null;
  private String tFileNameRegex = ".*";
  private boolean tRecursive = false;
  private String tPathRegex = ".*";
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private String tMetadataFrom = MF_LAST;
  private String tPreExtractRegex = "", tPostExtractRegex = "", tExtractRegex = "";
  private String tColumnNameForExtract = "";
  private String tSortedColumnSourceName = "";
  private String tSortFilesBySourceNames = "";
  private boolean tRemoveMVRows = true;
  private int tStandardizeWhat = Integer.MAX_VALUE;
  private String tSpecialMode = "";
  private String tCharset = null;
  private String tSkipHeaderToRegex = "";
  private String tSkipLinesRegex = "";
  private int tColumnNamesRow = 1, tFirstDataRow = 2;
  private String tColumnSeparator = "";
  private boolean tSourceNeedsExpandedFP_EQ = true;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private String tAddVariablesWhere = null;
  private int tNThreads = -1;
  private String tCacheFromUrl = null;
  private int tCacheSizeGB = -1;
  private String tCachePartialPathRegex = null;

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    switch (localName) {
      case "addAttributes" -> {
        State state = new AddAttributesHandler(saxHandler, tGlobalAttributes, this);
        saxHandler.setState(state);
      }
      case "altitudeMetersPerSourceUnit" ->
          throw new SimpleException(EDVAlt.stopUsingAltitudeMetersPerSourceUnit);
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
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "fileDir" -> tFileDir = contentStr;
      case "fileNameRegex" -> tFileNameRegex = contentStr;
      case "recursive" -> tRecursive = String2.parseBoolean(contentStr);
      case "pathRegex" -> tPathRegex = contentStr;
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "metadataFrom" -> tMetadataFrom = contentStr;
      case "preExtractRegex" -> tPreExtractRegex = contentStr;
      case "postExtractRegex" -> tPostExtractRegex = contentStr;
      case "extractRegex" -> tExtractRegex = contentStr;
      case "columnNameForExtract" -> tColumnNameForExtract = contentStr;
      case "sortedColumnSourceName" -> tSortedColumnSourceName = contentStr;
      case "sortFilesBySourceNames" -> tSortFilesBySourceNames = contentStr;
      case "charset" -> tCharset = contentStr;
      case "skipHeaderToRegex" -> tSkipHeaderToRegex = contentStr;
      case "skipLinesRegex" -> tSkipLinesRegex = contentStr;
      case "columnNamesRow" -> tColumnNamesRow = String2.parseInt(contentStr);
      case "firstDataRow" -> tFirstDataRow = String2.parseInt(contentStr);
      case "columnSeparator" -> tColumnSeparator = contentStr;
      case "sourceNeedsExpandedFP_EQ" ->
          tSourceNeedsExpandedFP_EQ = String2.parseBoolean(contentStr);
      case "specialMode" -> tSpecialMode = contentStr;
      case "fileTableInMemory" -> tFileTableInMemory = String2.parseBoolean(contentStr);
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "sosOfferingPrefix" -> tSosOfferingPrefix = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "addVariablesWhere" -> tAddVariablesWhere = contentStr;
      case "removeMVRows" -> tRemoveMVRows = String2.parseBoolean(contentStr);
      case "standardizeWhat" -> tStandardizeWhat = String2.parseInt(contentStr);
      case "nThreads" -> tNThreads = String2.parseInt(contentStr);
      case "cacheFromUrl" -> tCacheFromUrl = contentStr;
      case "cacheSizeGB" -> tCacheSizeGB = String2.parseInt(contentStr);
      case "cachePartialPathRegex" -> tCachePartialPathRegex = contentStr;
      case "dataset" -> {
        int ndv = tDataVariables.size();
        Object[][] ttDataVariables = new Object[ndv][];
        for (int i = 0; i < tDataVariables.size(); i++) {
          ttDataVariables[i] = (Object[]) tDataVariables.get(i);
        }

        EDD dataset = getDataset(ttDataVariables);

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }

  private EDD getDataset(Object[][] ttDataVariables) throws Throwable {
    EDD dataset = null;

    if (datasetType.equals("EDDTableFromAsciiFiles")) {
      dataset =
          new EDDTableFromAsciiFiles(
              datasetID,
              tAccessibleTo,
              tGraphsAccessibleTo,
              tOnChange,
              tFgdcFile,
              tIso19115File,
              tSosOfferingPrefix,
              tDefaultDataQuery,
              tDefaultGraphQuery,
              tGlobalAttributes,
              ttDataVariables,
              tReloadEveryNMinutes,
              tUpdateEveryNMillis,
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              tMetadataFrom,
              tCharset,
              tSkipHeaderToRegex,
              tSkipLinesRegex,
              tColumnNamesRow,
              tFirstDataRow,
              tColumnSeparator,
              tPreExtractRegex,
              tPostExtractRegex,
              tExtractRegex,
              tColumnNameForExtract,
              tSortedColumnSourceName,
              tSortFilesBySourceNames,
              tSourceNeedsExpandedFP_EQ,
              tFileTableInMemory,
              tAccessibleViaFiles,
              tRemoveMVRows,
              tStandardizeWhat,
              tNThreads,
              tCacheFromUrl,
              tCacheSizeGB,
              tCachePartialPathRegex,
              tAddVariablesWhere);
    } else if (datasetType.equals("EDDTableFromNcFiles")) {
      dataset =
          new EDDTableFromNcFiles(
              datasetID,
              tAccessibleTo,
              tGraphsAccessibleTo,
              tOnChange,
              tFgdcFile,
              tIso19115File,
              tSosOfferingPrefix,
              tDefaultDataQuery,
              tDefaultGraphQuery,
              tGlobalAttributes,
              ttDataVariables,
              tReloadEveryNMinutes,
              tUpdateEveryNMillis,
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              tMetadataFrom,
              tCharset,
              tSkipHeaderToRegex,
              tSkipLinesRegex,
              tColumnNamesRow,
              tFirstDataRow,
              tColumnSeparator,
              tPreExtractRegex,
              tPostExtractRegex,
              tExtractRegex,
              tColumnNameForExtract,
              tSortedColumnSourceName,
              tSortFilesBySourceNames,
              tSourceNeedsExpandedFP_EQ,
              tFileTableInMemory,
              tAccessibleViaFiles,
              tRemoveMVRows,
              tStandardizeWhat,
              tNThreads,
              tCacheFromUrl,
              tCacheSizeGB,
              tCachePartialPathRegex,
              tAddVariablesWhere);
    }
    return dataset;
  }
}
