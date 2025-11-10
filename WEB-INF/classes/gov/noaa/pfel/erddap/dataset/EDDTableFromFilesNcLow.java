package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.FileVariableMetadata;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.util.EDMessages;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.DataVariableInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public abstract class EDDTableFromFilesNcLow extends EDDTableFromFiles {

  public EDDTableFromFilesNcLow(
      String tClassName,
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tSosOfferingPrefix,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      LocalizedAttributes tAddGlobalAttributes,
      List<DataVariableInfo> tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      String tCharset,
      String tSkipHeaderToRegex,
      String tSkipLinesRegex,
      int tColumnNamesRow,
      int tFirstDataRow,
      String tColumnSeparator,
      String tPreExtractRegex,
      String tPostExtractRegex,
      String tExtractRegex,
      String tColumnNameForExtract,
      String tSortedColumnSourceName,
      String tSortFilesBySourceNames,
      boolean tSourceNeedsExpandedFP_EQ,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      boolean tRemoveMVRows,
      int tStandardizeWhat,
      int tNThreads,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex,
      String tAddVariablesWhere)
      throws Throwable {
    super(
        tClassName,
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tSosOfferingPrefix,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddGlobalAttributes,
        tDataVariables,
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

  protected PrimitiveArray loadSingleVar(Variable var, Attributes atts, String fileName)
      throws Exception {
    PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
    pa.convertToStandardMissingValues(
        atts.getString("_FillValue"), atts.getString("missing_value"));
    // remove current attributes or define new _FillValue
    atts.remove("missing_value");
    PAType paPAType = pa.elementType();
    if (pa.isFloatingPointType() || paPAType == PAType.STRING) {
      atts.remove("_FillValue");
    } else { // integer or char
      atts.set("_FillValue", PrimitiveArray.factory(paPAType, 1, ""));
    }
    return pa;
  }

  private String regexExtractMetadata(String dvName, PAType paType, String inputString) {
    String csv[] = StringArray.arrayFromCSV(dvName.substring(12), ',');
    String value = null;

    if (csv.length == 2) {
      Matcher matcher = Pattern.compile(csv[0]).matcher(inputString);
      if (matcher.matches()) {
        value = matcher.group(String2.parseInt(csv[1]));
      }
    }
    return value;
  }

  /** Processes a sourceName that extracts a value from a string (e.g., fileName) using a regex. */
  private FileVariableMetadata processRegexExtractMetadata(
      String dvName, PAType paType, String inputString) {
    String value = regexExtractMetadata(dvName, paType, inputString);

    if (value == null) { // No match or bad config
      return new FileVariableMetadata(); // existsInFile = false
    }

    Attributes atts = new Attributes();
    if (FileVariableMetadata.treatAsString(paType)) {
      return new FileVariableMetadata(atts, paType, 1, 1, value, value);
    } else {
      double dVal = String2.parseDouble(value);
      return new FileVariableMetadata(atts, paType, 1, 1, dVal, dVal, false, "n/a", "n/a");
    }
  }

  private Pair<PrimitiveArray, Attributes> getColumn(
      String columnName,
      String fullDir,
      String fileName,
      Attributes globalAttributes,
      NetcdfFile netcdfFile,
      Map<String, Pair<PrimitiveArray, Attributes>> columnCache)
      throws Exception {

    Pair<PrimitiveArray, Attributes> cachedInfo = columnCache.get(columnName);
    if (cachedInfo != null) {
      return cachedInfo;
    }
    int dv = sourceDataNames.indexOf(columnName);
    String dvType = sourceDataTypes[dv];
    PAType paType = PAType.fromCohortString(dvType);
    Attributes atts = new Attributes();
    Pair<PrimitiveArray, Attributes> result;

    if (columnName.equals(columnNameForExtract)) {
      String value = extractFromFileName(fileName);
      result = Pair.of(PrimitiveArray.factory(paType, 1, value), atts);
    } else if (columnName.startsWith("***fileName,")) {
      String value = regexExtractMetadata(columnName, paType, fileName);
      result = Pair.of(PrimitiveArray.factory(paType, 1, value), atts);
    } else if (columnName.startsWith("***pathName,")) {
      String value = regexExtractMetadata(columnName, paType, fullDir + fileName);
      result = Pair.of(PrimitiveArray.factory(paType, 1, value), atts);
    } else if (columnName.startsWith("global:")) {
      String attName = columnName.substring(7);
      PrimitiveArray pa = globalAttributes.get(attName);
      result = Pair.of(pa, atts);
    } else if (columnName.startsWith("variable:")) {
      PrimitiveArray pa = getVariableAttribute(columnName, paType, netcdfFile);
      result = Pair.of(pa, atts);
    } else {
      Variable var = netcdfFile.findVariable(columnName);
      if (var == null) {
        String2.log("Script dependency '" + columnName + "' not in file. Cannot calc metadata.");
        return null;
      }
      NcHelper.getVariableAttributes(var, atts);

      // Apply dataset.xml overrides if we found the dependency
      if (dv > -1) {
        if (!Double.isNaN(addAttFillValue[dv])) {
          atts.set("_FillValue", addAttFillValue[dv]);
        }
        if (!Double.isNaN(addAttMissingValue[dv])) {
          atts.set("missing_value", addAttMissingValue[dv]);
        }
      }

      // Load the full data for the dependency
      PrimitiveArray pa = loadSingleVar(var, atts, netcdfFile.getLocation());
      result = Pair.of(pa, atts);
    }

    // Add to cache before returning
    columnCache.put(columnName, result);
    return result;
  }

  /** Processes a script variable (=) by loading all dependencies and executing the script. */
  private FileVariableMetadata processScriptMetadata(
      String dvName,
      String dvType,
      PAType paType,
      NetcdfFile netcdfFile,
      Attributes globalAttributes,
      String fullDir,
      String fileName,
      Map<String, Pair<PrimitiveArray, Attributes>> columnCache)
      throws Throwable {

    Pair<PrimitiveArray, Attributes> cachedInfo = columnCache.get(dvName);
    if (cachedInfo != null) {
      // We have a cached result for this script, just calculate stats
      PrimitiveArray resultPA = cachedInfo.getLeft();
      Attributes atts = cachedInfo.getRight();
      if (FileVariableMetadata.treatAsString(paType)) {
        String nMinMax[] = resultPA.getNMinMax();
        int n = String2.parseInt(nMinMax[0]);
        String min = (n > 0) ? nMinMax[1] : null;
        String max = (n > 0) ? nMinMax[2] : null;
        return new FileVariableMetadata(atts, paType, n, resultPA.size(), min, max);
      } else {
        double stats[] = resultPA.calculateStats();
        int n = Math2.roundToInt(stats[PrimitiveArray.STATS_N]);
        double min = (n > 0) ? stats[PrimitiveArray.STATS_MIN] : Double.NaN;
        double max = (n > 0) ? stats[PrimitiveArray.STATS_MAX] : Double.NaN;
        return new FileVariableMetadata(
            atts, paType, n, resultPA.size(), min, max, false, "n/a", "n/a");
      }
    }
    Table scriptInputTable = new Table();
    scriptInputTable.globalAttributes().add(globalAttributes);

    Set<String> neededCols = scriptNeedsColumns.get(dvName);

    boolean dependenciesMissing = false;
    if (neededCols != null) {
      for (String colName : neededCols) {

        Pair<PrimitiveArray, Attributes> columnInfo =
            getColumn(colName, fullDir, fileName, globalAttributes, netcdfFile, columnCache);
        if (columnInfo == null) {
          dependenciesMissing = true;
          break;
        }

        scriptInputTable.addColumn(colName, columnInfo.getLeft());
        scriptInputTable.columnAttributes(colName).add(columnInfo.getRight());
      }
    }

    if (dependenciesMissing) {
      return FileVariableMetadata.createNoData(paType);
    }
    try {
      StringArray scriptNames = new StringArray(new String[] {dvName});
      StringArray scriptTypes = new StringArray(new String[] {dvType});
      convertScriptColumnsToDataColumns(
          fullDir + fileName, // file name for error logging
          scriptInputTable,
          scriptNames,
          scriptTypes,
          scriptNeedsColumns);
    } catch (Throwable t) {
      String2.log("Script " + dvName + " failed during metadata generation: " + t.getMessage());
      return FileVariableMetadata.createNoData(paType);
    }

    // Script succeeded, get the resulting column
    PrimitiveArray resultPA = scriptInputTable.getColumn(dvName);
    if (resultPA == null) {
      String2.log("Script " + dvName + " ran but didn't produce a column.");
      return FileVariableMetadata.createNoData(paType);
    }
    Attributes atts = new Attributes();

    columnCache.put(dvName, Pair.of(resultPA, atts));
    // Finally, calculate stats on the result
    if (FileVariableMetadata.treatAsString(paType)) {
      String nMinMax[] = resultPA.getNMinMax();
      int n = String2.parseInt(nMinMax[0]);
      String min = (n > 0) ? nMinMax[1] : null;
      String max = (n > 0) ? nMinMax[2] : null;
      return new FileVariableMetadata(atts, paType, n, resultPA.size(), min, max);
    } else {
      double stats[] = resultPA.calculateStats();
      int n = Math2.roundToInt(stats[PrimitiveArray.STATS_N]);
      double min = (n > 0) ? stats[PrimitiveArray.STATS_MIN] : Double.NaN;
      double max = (n > 0) ? stats[PrimitiveArray.STATS_MAX] : Double.NaN;
      return new FileVariableMetadata(
          atts, paType, n, resultPA.size(), min, max, false, "n/a", "n/a");
    }
  }

  private FileVariableMetadata processGlobalMetadata(
      String dvName, PAType paType, Attributes globalAttributes) {
    String attName = dvName.substring(7);
    PrimitiveArray pa = globalAttributes.get(attName);

    if (pa == null || pa.size() == 0) {
      String2.log("global attribute: " + dvName + " not found");
      // Attribute not found in file.
      return FileVariableMetadata.createNoData(paType);
    } else {
      // Attribute found. It has 1 value. Min == Max == value.
      Attributes atts = new Attributes();
      if (FileVariableMetadata.treatAsString(paType)) {
        String val = pa.getString(0);
        return new FileVariableMetadata(atts, paType, 1, 1, val, val);
      } else {
        double val = pa.getDouble(0);
        return new FileVariableMetadata(atts, paType, 1, 1, val, val, false, "n/a", "n/a");
      }
    }
  }

  private PrimitiveArray getVariableAttribute(String dvName, PAType paType, NetcdfFile netcdfFile) {
    String s = dvName.substring(9);
    int cpo = s.indexOf(':');
    if (cpo <= 0) {
      // Invalid format, treat as non-existent
      return null;
    }
    String varName = s.substring(0, cpo);
    String attName = s.substring(cpo + 1);

    Variable var = netcdfFile.findVariable(varName);

    if (var == null) {
      // The *source variable* doesn't exist.
      return null;
    } else {
      // Variable exists, check for its attribute.
      Attributes varAtts = new Attributes();
      NcHelper.getVariableAttributes(var, varAtts);
      return varAtts.get(attName);
    }
  }

  private FileVariableMetadata processVariableMetadata(
      String dvName, PAType paType, NetcdfFile netcdfFile) {
    PrimitiveArray pa = getVariableAttribute(dvName, paType, netcdfFile);

    if (pa == null || pa.size() == 0) {
      // The *attribute* doesn't exist.
      return FileVariableMetadata.createNoData(paType);
    } else {
      // Attribute found. Min == Max == value.
      Attributes atts = new Attributes();
      if (FileVariableMetadata.treatAsString(paType)) {
        // Handle string array attributes (e.g., actual_range "0.0, 94.0")
        String val = (pa.size() > 1) ? pa.toString() : pa.getString(0);
        return new FileVariableMetadata(atts, paType, 1, 1, val, val);
      } else {
        double val = pa.getDouble(0);
        return new FileVariableMetadata(atts, paType, 1, 1, val, val, false, "n/a", "n/a");
      }
    }
  }

  private FileVariableMetadata processFileVariableMetadata(
      String dvName,
      PAType paType,
      NetcdfFile netcdfFile,
      int dv,
      Map<String, Pair<PrimitiveArray, Attributes>> columnCache)
      throws Exception {

    Pair<PrimitiveArray, Attributes> cachedInfo = columnCache.get(dvName);
    if (cachedInfo != null) {
      // We have a cached result for this script, just calculate stats
      PrimitiveArray resultPA = cachedInfo.getLeft();
      Attributes atts = cachedInfo.getRight();
      if (FileVariableMetadata.treatAsString(paType)) {
        String nMinMax[] = resultPA.getNMinMax();
        int n = String2.parseInt(nMinMax[0]);
        String min = (n > 0) ? nMinMax[1] : null;
        String max = (n > 0) ? nMinMax[2] : null;
        return new FileVariableMetadata(atts, paType, n, resultPA.size(), min, max);
      } else {
        double stats[] = resultPA.calculateStats();
        int n = Math2.roundToInt(stats[PrimitiveArray.STATS_N]);
        double min = (n > 0) ? stats[PrimitiveArray.STATS_MIN] : Double.NaN;
        double max = (n > 0) ? stats[PrimitiveArray.STATS_MAX] : Double.NaN;
        return new FileVariableMetadata(
            atts, paType, n, resultPA.size(), min, max, false, "n/a", "n/a");
      }
    }

    Variable var = netcdfFile.findVariable(dvName);

    // Case 1: Variable not in file
    if (var == null) {
      Dimension dim = netcdfFile.findDimension(dvName);

      if (dim != null) {
        // It's a dimension. Now, find the 1D variable that uses it.
        // This is the manual way to find a coordinate variable.
        for (Variable testVar : netcdfFile.getVariables()) {

          // Check if it's a 1D variable and its first (only) dimension matches
          if (testVar.getRank() == 1 && testVar.getDimensions().get(0).equals(dim)) {
            var = testVar; // Found it.
            break; // Stop searching
          }
        }
      }
    }
    if (var == null) {
      return new FileVariableMetadata();
    }
    Attributes atts = new Attributes();
    // Get metadata from our pre-filled metaTable
    NcHelper.getVariableAttributes(var, atts);
    paType = NcHelper.getElementPAType(var.getDataType());
    int size = (int) var.getSize(); // Total size of the variable

    // Apply the user-defined addAtt missing values, just like the
    // base implementation did. This is crucial for correctly
    // processing the sorted column.
    if (!Double.isNaN(addAttFillValue[dv])) {
      atts.set("_FillValue", addAttFillValue[dv]);
    }
    if (!Double.isNaN(addAttMissingValue[dv])) {
      atts.set("missing_value", addAttMissingValue[dv]);
    }

    // Check for standard 'actual_range' attribute
    PrimitiveArray actualRange = atts.get("actual_range");
    boolean hasMissingAtt =
        !Double.isNaN(atts.getDouble("_FillValue"))
            || !Double.isNaN(atts.getDouble("missing_value"));

    if (FileVariableMetadata.treatAsString(paType)) {

      String min = null;
      String max = null;
      int n = size;
      if (actualRange != null && actualRange.size() >= 2) {
        min = actualRange.getString(0);
        max = actualRange.getString(1);
        // Assume no valid data
        if ("".equals(min) && "".equals(max)) {
          n = 0;
        } else {
          // Heuristic: If range is given, assume valid data.
          // 'n' is a guess to determine hasNaN.
          n = hasMissingAtt ? size - 1 : size;
          if (n < 0) {
            n = 0;
          }
        }
      } else {
        PrimitiveArray pa = loadSingleVar(var, atts, netcdfFile.getLocation());
        String nMinMax[] = pa.getNMinMax();
        n = String2.parseInt(nMinMax[0]);
        min = (n > 0) ? nMinMax[1] : "";
        max = (n > 0) ? nMinMax[2] : "";
        size = pa.size();
        columnCache.put(dvName, Pair.of(pa, atts));
      }
      return new FileVariableMetadata(atts, paType, n, size, min, max);
    } else {
      // Numeric types
      double min = Double.NaN;
      double max = Double.NaN;
      int n = 0;
      String asc = "n/a";
      String evn = "n/a";
      boolean isSorted = (dv == sortedDVI);

      if (isSorted) {
        // This is the ONE variable we read fully.
        // This is acceptable as it's almost always a small axis var.
        PrimitiveArray pa = loadSingleVar(var, atts, netcdfFile.getLocation());
        double stats[] = pa.calculateStats();
        n = Math2.roundToInt(stats[PrimitiveArray.STATS_N]);
        min = (n > 0) ? stats[PrimitiveArray.STATS_MIN] : Double.NaN;
        max = (n > 0) ? stats[PrimitiveArray.STATS_MAX] : Double.NaN;
        size = pa.size();
        if (n > 0) {
          asc = pa.isAscending();
          if (n > 1 && asc.length() == 0) {
            evn = pa.isEvenlySpaced();
          }
        }
        columnCache.put(dvName, Pair.of(pa, atts));
      } else if (actualRange != null && actualRange.size() >= 2) {
        // Use 'actual_range' for all other numeric vars
        min = actualRange.getDouble(0);
        max = actualRange.getDouble(1);
        // Assume no valid data
        if (Double.isNaN(min) && Double.isNaN(max)) {
          n = 0;
        } else {
          // Heuristic: If range is given, assume valid data.
          // 'n' is a guess to determine hasNaN.
          n = hasMissingAtt ? size - 1 : size;
          if (n < 0) n = 0;
        }
      } else {
        PrimitiveArray pa = loadSingleVar(var, atts, netcdfFile.getLocation());
        double stats[] = pa.calculateStats();
        n = Math2.roundToInt(stats[PrimitiveArray.STATS_N]);
        min = (n > 0) ? stats[PrimitiveArray.STATS_MIN] : Double.NaN;
        max = (n > 0) ? stats[PrimitiveArray.STATS_MAX] : Double.NaN;
        size = pa.size();
        columnCache.put(dvName, Pair.of(pa, atts));
      }

      return new FileVariableMetadata(atts, paType, n, size, min, max, isSorted, asc, evn);
    }
  }

  /**
   * Gathers the required metadata for all source variables from a single file.
   *
   * <p>This overridden implementation is <b>efficient for NetCDF files</b>. It reads only the
   * file's metadata (headers) and relies on the 'actual_range' attribute for min/max values.
   *
   * <p>It only reads the full data for the single 'sortedColumn' (if one is specified) to verify
   * its spacing and order.
   *
   * @param fullDir the directory
   * @param fileName the file name
   * @param varNames list of source variable names
   * @param varTypes list of source variable types (as cohort strings)
   * @param sortedDVI the index of the sorted variable, or -1
   * @return A Map linking variable names to their extracted metadata.
   * @throws Throwable
   */
  @Override
  protected Map<String, FileVariableMetadata> getFileMetadata(String fullDir, String fileName)
      throws Throwable {
    if (isZarr() || !EDStatic.config.useNcMetadataForFileTable) {
      return super.getFileMetadata(fullDir, fileName);
    }

    Map<String, Pair<PrimitiveArray, Attributes>> columnCache = new HashMap<>();
    Map<String, FileVariableMetadata> metadataMap = new HashMap<>();

    // Decompress the file if needed (borrowed from lowGetSourceDataFromFile)
    String decompFullName =
        FileVisitorDNLS.decompressIfNeeded(
            fullDir + fileName,
            fileDir, // Assumes fileDir is a class member
            decompressedDirectory(), // Assumes this is a class member
            EDStatic.decompressedCacheMaxGB,
            true); // reuseExisting

    Attributes globalAttributes = addGlobalAttributes.toAttributes(EDMessages.DEFAULT_LANGUAGE);

    try (NetcdfFile netcdfFile = NcHelper.openFile(decompFullName)) {
      NcHelper.getGroupAttributes(netcdfFile.getRootGroup(), globalAttributes);

      int ndv = sourceDataNames.size();
      for (int dv = 0; dv < ndv; dv++) {
        String dvName = sourceDataNames.get(dv);
        String dvType = sourceDataTypes[dv];
        PAType paType = PAType.fromCohortString(dvType);

        if (dvName.equals(columnNameForExtract)) {
          Attributes atts = new Attributes();
          String value = extractFromFileName(fileName);
          if (FileVariableMetadata.treatAsString(paType)) {
            metadataMap.put(dvName, new FileVariableMetadata(atts, paType, 1, 1, value, value));
          } else {
            double dVal = String2.parseDouble(value);
            metadataMap.put(
                dvName,
                new FileVariableMetadata(atts, paType, 1, 1, dVal, dVal, false, "n/a", "n/a"));
          }
        } else if (dvName.startsWith("***fileName,")) {
          metadataMap.put(dvName, processRegexExtractMetadata(dvName, paType, fileName));
        } else if (dvName.startsWith("***pathName,")) {
          metadataMap.put(dvName, processRegexExtractMetadata(dvName, paType, fullDir + fileName));
        } else if (dvName.startsWith("=")) {
          metadataMap.put(
              dvName,
              processScriptMetadata(
                  dvName,
                  dvType,
                  paType,
                  netcdfFile,
                  globalAttributes,
                  fullDir,
                  fileName,
                  columnCache));
        } else if (dvName.startsWith("global:")) {
          metadataMap.put(dvName, processGlobalMetadata(dvName, paType, globalAttributes));
        } else if (dvName.startsWith("variable:")) {
          metadataMap.put(dvName, processVariableMetadata(dvName, paType, netcdfFile));
        } else {
          metadataMap.put(
              dvName, processFileVariableMetadata(dvName, paType, netcdfFile, dv, columnCache));
        }
      }
    }
    return metadataMap;
  }
}
