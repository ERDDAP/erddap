package gov.noaa.pfel.erddap.handlers;

import static gov.noaa.pfel.erddap.dataset.EDDGrid.DEFAULT_MATCH_AXIS_N_DIGITS;
import static gov.noaa.pfel.erddap.dataset.EDDGridFromFiles.MF_LAST;

import com.cohort.array.StringArray;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDVAlt;
import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class EDDGridFromFilesHandler extends StateWithParent {
  private String datasetID;
  private StringBuilder content = new StringBuilder();
  private String datasetType;

  public EDDGridFromFilesHandler(
      SaxHandler saxHandler, String datasetID, State completeState, String datasetType) {
    super(saxHandler, completeState);
    this.datasetID = datasetID;
    this.datasetType = datasetType;
  }

  private String tAccessibleTo = null;
  private String tGraphsAccessibleTo = null;
  private boolean tAccessibleViaWMS = true;
  private StringArray tOnChange = new StringArray();
  private boolean tFileTableInMemory = false;
  private String tFgdcFile = null;
  private String tIso19115File = null;
  private com.cohort.array.Attributes tGlobalAttributes = new com.cohort.array.Attributes();
  private ArrayList<Object[]> tAxisVariables = new ArrayList();
  private ArrayList<Object[]> tDataVariables = new ArrayList();
  private int tReloadEveryNMinutes = Integer.MAX_VALUE;
  private int tUpdateEveryNMillis = 0;
  private String tFileDir = null;
  private String tFileNameRegex = ".*";
  private boolean tRecursive = false;
  private String tPathRegex = ".*";
  private boolean tAccessibleViaFiles = EDStatic.defaultAccessibleViaFiles;
  private String tMetadataFrom = MF_LAST;
  private int tMatchAxisNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;
  private String tDefaultDataQuery = null;
  private String tDefaultGraphQuery = null;
  private int tnThreads = -1;
  private boolean tDimensionValuesInMemory = true;
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
      case "axisVariable" -> {
        State state = new AxisVariableHandler(saxHandler, tAxisVariables, this);
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
      case "accessibleViaWMS" -> tAccessibleViaWMS = String2.parseBoolean(contentStr);
      case "reloadEveryNMinutes" -> tReloadEveryNMinutes = String2.parseInt(contentStr);
      case "updateEveryNMillis" -> tUpdateEveryNMillis = String2.parseInt(contentStr);
      case "fileDir" -> tFileDir = contentStr;
      case "fileNameRegex" -> tFileNameRegex = contentStr;
      case "recursive" -> tRecursive = String2.parseBoolean(contentStr);
      case "pathRegex" -> tPathRegex = contentStr;
      case "accessibleViaFiles" -> tAccessibleViaFiles = String2.parseBoolean(contentStr);
      case "metadataFrom" -> tMetadataFrom = contentStr;
      case "fileTableInMemory" -> tFileTableInMemory = String2.parseBoolean(contentStr);
      case "matchAxisNDigits" ->
          tMatchAxisNDigits = String2.parseInt(contentStr, DEFAULT_MATCH_AXIS_N_DIGITS);
      case "ensureAxisValuesAreEqual" ->
          tMatchAxisNDigits = String2.parseBoolean(contentStr) ? 20 : 0;
      case "onChange" -> tOnChange.add(contentStr);
      case "fgdcFile" -> tFgdcFile = contentStr;
      case "iso19115File" -> tIso19115File = contentStr;
      case "defaultDataQuery" -> tDefaultDataQuery = contentStr;
      case "defaultGraphQuery" -> tDefaultGraphQuery = contentStr;
      case "nThreads" -> tnThreads = String2.parseInt(contentStr);
      case "dimensionValuesInMemory" -> tDimensionValuesInMemory = String2.parseBoolean(contentStr);
      case "cacheFromUrl" -> tCacheFromUrl = contentStr;
      case "cacheSizeGB" -> tCacheSizeGB = String2.parseInt(contentStr);
      case "cachePartialPathRegex" -> tCachePartialPathRegex = contentStr;
      case "dataset" -> {
        Object[][] ttAxisVariables = new Object[tAxisVariables.size()][];
        ttAxisVariables = tAxisVariables.toArray(ttAxisVariables);

        Object[][] ttDataVariables = new Object[tDataVariables.size()][];
        ttDataVariables = tDataVariables.toArray(ttDataVariables);

        EDD dataset = getDataset(ttAxisVariables, ttDataVariables);

        this.completeState.handleDataset(dataset);
        saxHandler.setState(this.completeState);
      }
      default -> String2.log("Unexpected end tag: " + localName);
    }
    content.setLength(0);
  }

  private EDD getDataset(Object[][] ttAxisVariables, Object[][] ttDataVariables) throws Throwable {
    EDD dataset;

    if (datasetType.equals("EDDGridFromAudioFiles")) {
      dataset =
          new EDDGridFromAudioFiles(
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
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              tMetadataFrom,
              tMatchAxisNDigits,
              tFileTableInMemory,
              tAccessibleViaFiles,
              tnThreads,
              tDimensionValuesInMemory,
              tCacheFromUrl,
              tCacheSizeGB,
              tCachePartialPathRegex);
    } else if (datasetType.equals("EDDGridFromNcFiles")) {
      dataset =
          new EDDGridFromNcFiles(
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
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              tMetadataFrom,
              tMatchAxisNDigits,
              tFileTableInMemory,
              tAccessibleViaFiles,
              tnThreads,
              tDimensionValuesInMemory,
              tCacheFromUrl,
              tCacheSizeGB,
              tCachePartialPathRegex);
    } else if (datasetType.equals("EDDGridFromNcFilesUnpacked")) {
      dataset =
          new EDDGridFromNcFilesUnpacked(
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
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              tMetadataFrom,
              tMatchAxisNDigits,
              tFileTableInMemory,
              tAccessibleViaFiles,
              tnThreads,
              tDimensionValuesInMemory,
              tCacheFromUrl,
              tCacheSizeGB,
              tCachePartialPathRegex);
    } else if (datasetType.equals("EDDGridFromMergeIRFiles")) {
      dataset =
          new EDDGridFromMergeIRFiles(
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
              tFileDir,
              tFileNameRegex,
              tRecursive,
              tPathRegex,
              tMetadataFrom,
              tMatchAxisNDigits,
              tFileTableInMemory,
              tAccessibleViaFiles,
              tnThreads,
              tDimensionValuesInMemory,
              tCacheFromUrl,
              tCacheSizeGB,
              tCachePartialPathRegex);
    } else {
      throw new Exception(
          "type=\"" + datasetType + "\" needs to be added to EDDGridFromFiles.fromXml at end.");
    }
    return dataset;
  }
}
