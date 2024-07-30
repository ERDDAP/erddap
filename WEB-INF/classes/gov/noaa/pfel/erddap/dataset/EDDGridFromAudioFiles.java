/*
 * EDDGridFromAudioFiles Copyright 2017, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

/**
 * This class represents gridded data from an audio file where all files have the same number of
 * samples and where the leftmost dimension is created from the fileName. See Table.readAudio file
 * for info on which types of audio files can be read.
 * https://docs.oracle.com/javase/tutorial/sound/converters.html
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2017-09-11
 */
public class EDDGridFromAudioFiles extends EDDGridFromFiles {

  /** The constructor just calls the super constructor. */
  public EDDGridFromAudioFiles(
      String tDatasetID,
      String tAccessibleTo,
      String tGraphsAccessibleTo,
      boolean tAccessibleViaWMS,
      StringArray tOnChange,
      String tFgdcFile,
      String tIso19115File,
      String tDefaultDataQuery,
      String tDefaultGraphQuery,
      Attributes tAddGlobalAttributes,
      Object[][] tAxisVariables,
      Object[][] tDataVariables,
      int tReloadEveryNMinutes,
      int tUpdateEveryNMillis,
      String tFileDir,
      String tFileNameRegex,
      boolean tRecursive,
      String tPathRegex,
      String tMetadataFrom,
      int tMatchAxisNDigits,
      boolean tFileTableInMemory,
      boolean tAccessibleViaFiles,
      int tnThreads,
      boolean tDimensionValuesInMemory,
      String tCacheFromUrl,
      int tCacheSizeGB,
      String tCachePartialPathRegex)
      throws Throwable {

    super(
        "EDDGridFromAudioFiles",
        tDatasetID,
        tAccessibleTo,
        tGraphsAccessibleTo,
        tAccessibleViaWMS,
        tOnChange,
        tFgdcFile,
        tIso19115File,
        tDefaultDataQuery,
        tDefaultGraphQuery,
        tAddGlobalAttributes,
        tAxisVariables,
        tDataVariables,
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

    if (verbose) String2.log("\n*** constructing EDDGridFromAudioFiles(xmlReader)...");
  }

  /**
   * This gets sourceGlobalAttributes and sourceDataAttributes from the specified source file.
   *
   * @param tFullName the name of the decompressed data file
   * @param sourceAxisNames If there is a special axis0, this list will be the instances list[1 ...
   *     n-1].
   * @param sourceDataNames the names of the desired source data columns.
   * @param sourceDataTypes the data types of the desired source data columns (e.g., "String" or
   *     "float")
   * @param sourceGlobalAttributes should be an empty Attributes. It will be populated by this
   *     method
   * @param sourceAxisAttributes should be an array of empty Attributes. It will be populated by
   *     this method
   * @param sourceDataAttributes should be an array of empty Attributes. It will be populated by
   *     this method
   * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or sourceDataName not
   *     found). If there is trouble, this doesn't call addBadFile or requestReloadASAP().
   */
  @Override
  public void lowGetSourceMetadata(
      String tFullName,
      StringArray sourceAxisNames,
      StringArray sourceDataNames,
      String sourceDataTypes[],
      Attributes sourceGlobalAttributes,
      Attributes sourceAxisAttributes[],
      Attributes sourceDataAttributes[])
      throws Throwable {

    if (reallyVerbose) String2.log("getSourceMetadata " + tFullName);

    Table table = new Table();
    table.readAudioFile(tFullName, false, true); // readData? addElapsedTime?

    // globalAtts
    sourceGlobalAttributes.add(table.globalAttributes());

    // axisVariables
    if (sourceAxisNames.size() != 1)
      throw new SimpleException(
          "SourceAxisNames size=" + sourceAxisNames.size() + ", but expected=1.");
    Test.ensureEqual(sourceAxisNames.get(0), Table.ELAPSED_TIME, "Unexpected sourceAxisName[0].");
    sourceAxisAttributes[0].add(table.columnAttributes(0));

    // dataVariables
    int ndni = sourceDataNames.size();
    for (int dni = 0; dni < ndni; dni++) {
      String name = sourceDataNames.get(dni);
      int tc = table.findColumnNumber(name);
      if (tc < 0)
        throw new SimpleException(
            "There is no sourceDataName="
                + name
                + ". The available names are ["
                + table.getColumnNamesCSSVString()
                + "].");
      sourceDataAttributes[dni].add(table.columnAttributes(tc));
      Test.ensureEqual(
          table.getColumn(tc).elementTypeString(),
          sourceDataTypes[dni],
          "Unexpected source dataType for sourceDataName=" + name + ".");
    }
  }

  /**
   * This gets source axis values from one file.
   *
   * @param tFullName the name of the decompressed data file
   * @param sourceAxisNames the names of the desired source axis variables. If there is a special
   *     axis0, this will not include axis0's name.
   * @param sourceDataNames When there are unnamed dimensions, this is to find out the shape of the
   *     variable to make index values 0, 1, size-1.
   * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes). It needn't
   *     set sourceGlobalAttributes or sourceDataAttributes (but see getSourceMetadata).
   * @throws Throwable if trouble (e.g., invalid file). If there is trouble, this doesn't call
   *     addBadFile or requestReloadASAP().
   */
  @Override
  public PrimitiveArray[] lowGetSourceAxisValues(
      String tFullName, StringArray sourceAxisNames, StringArray sourceDataNames) throws Throwable {

    // for this class, only elapsedTime is available
    if (sourceAxisNames.size() != 1 || !sourceAxisNames.get(0).equals(Table.ELAPSED_TIME))
      throw new SimpleException(
          "The only sourceAxisName available from audio files is "
              + Table.ELAPSED_TIME
              + ", not ["
              + sourceAxisNames.toJsonCsvString()
              + "].");

    try {
      PrimitiveArray[] avPa = new PrimitiveArray[1];

      Table table = new Table();
      table.readAudioFile(tFullName, true, true); // readData? addElapsedTime?
      avPa[0] = table.getColumn(0);

      return avPa;

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error in EDDGridFromAudioFiles.getSourceAxisValues"
              + "\nfrom "
              + tFullName
              + "\nCause: "
              + MustBe.throwableToShortString(t),
          t);
    }
  }

  /**
   * This gets source data from one file.
   *
   * @param fullFileName
   * @param tDataVariables the desired data variables
   * @param tConstraints For each axis variable, there will be 3 numbers (startIndex, stride,
   *     stopIndex). !!! If there is a special axis0, this will not include constraints for axis0.
   * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues. <br>
   *     The dataValues are straight from the source, not modified. <br>
   *     The primitiveArray dataTypes are usually the sourceDataPAType, but can be any type.
   *     EDDGridFromFiles will convert to the sourceDataPAType. <br>
   *     Note the lack of axisVariable values!
   * @throws Throwable if trouble (notably, WaitThenTryAgainException). If there is trouble, this
   *     doesn't call addBadFile or requestReloadASAP().
   */
  @Override
  public PrimitiveArray[] lowGetSourceDataFromFile(
      String fullFileName, EDV tDataVariables[], IntArray tConstraints) throws Throwable {

    if (verbose)
      String2.log(
          "getSourceDataFromFile("
              + fullFileName
              + ", ["
              + String2.toCSSVString(tDataVariables)
              + "], "
              + tConstraints
              + ")");

    // for this class, elapsedTime must be constrained
    if (tConstraints.size() != 3)
      throw new SimpleException(
          "The only sourceAxisName available from audio files is "
              + Table.ELAPSED_TIME
              + ", not ["
              + sourceAxisNames.toJsonCsvString()
              + "].");
    int start = tConstraints.get(0);
    int stride = tConstraints.get(1);
    int stop = tConstraints.get(2);
    int howManyRows = PrimitiveArray.strideWillFind(stop - start + 1, stride);

    Table table = new Table();
    table.readAudioFile(fullFileName, true, false); // readData? addElapsedTime?
    int ndv = tDataVariables.length;
    PrimitiveArray paa[] = new PrimitiveArray[ndv];
    for (int dvi = 0; dvi < ndv; dvi++) {
      EDV edv = tDataVariables[dvi];
      int col = table.findColumnNumber(edv.sourceName());
      if (col >= 0) {
        paa[dvi] = table.getColumn(col).subset(start, stride, stop);
        table.setColumn(col, new IntArray(1, false)); // small, to save memory
      } else {
        // make a pa with missing values
        double mv = edv.sourceFillValue();
        if (Double.isNaN(mv)) mv = edv.sourceMissingValue();
        paa[dvi] = PrimitiveArray.factory(edv.sourceDataPAType(), howManyRows, "" + mv);
      }
    }
    return paa;
  }

  /**
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @throws Throwable always (since this class doesn't support sibling())
   */
  @Override
  public EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable {
    throw new SimpleException("Error: EDDGridFromAudioFiles doesn't support method=\"sibling\".");
  }

  /**
   * This does its best to generate a clean, ready-to-use datasets.xml entry for an
   * EDDGridFromAudioFiles. The XML can then be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to looks at (possibly)
   * private audio files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code).
   * @param sampleFileName full file name of one of the files in the collection. If "", this will
   *     pick a file.
   * @param extractFileNameRegex The regex to extract info from the fileName to create
   *     axisVariable[0].
   * @param extractDataType This may be a data type (eg, int, double, String) or a timeFormat (e.g.,
   *     yyyyMMddHHmmss).
   * @param extractColumnName the name for the column created by the fileName extract, e.g., time.
   * @param tReloadEveryNMinutes,
   * @param externalAddGlobalAttributes These are given priority. Use null if none available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String tFileDir,
      String tFileNameRegex,
      String sampleFileName,
      String extractFileNameRegex,
      String extractDataType,
      String extractColumnName,
      int tReloadEveryNMinutes,
      String tCacheFromUrl,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** EDDGridFromAudioFiles.generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + " sampleFileName="
            + sampleFileName
            + "\nreloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    if (!String2.isSomething(tFileDir))
      throw new IllegalArgumentException("fileDir wasn't specified.");
    if (!String2.isSomething(extractFileNameRegex))
      throw new IllegalArgumentException("extractFileNameRegex wasn't specified.");
    if (!String2.isSomething(extractDataType))
      throw new IllegalArgumentException("extractDataType wasn't specified.");
    if (!String2.isSomething(extractColumnName))
      throw new IllegalArgumentException("extractColumnName wasn't specified.");

    tFileDir = File2.addSlash(tFileDir);
    tFileNameRegex = String2.isSomething(tFileNameRegex) ? tFileNameRegex.trim() : ".*";
    if (String2.isRemote(tCacheFromUrl))
      FileVisitorDNLS.sync(
          tCacheFromUrl, tFileDir, tFileNameRegex, true, ".*", false); // not fullSync

    if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
      tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis

    if (!String2.isSomething(sampleFileName))
      String2.log(
          "Found/using sampleFileName="
              + (sampleFileName =
                  FileVisitorDNLS.getSampleFileName(
                      tFileDir, tFileNameRegex, true, ".*"))); // recursive, pathRegex

    String2.log("Let's see if we get the structure of the sample file:");
    Table table = new Table();
    String decomSampleFileName =
        FileVisitorDNLS.decompressIfNeeded(
            sampleFileName,
            tFileDir,
            EDStatic.fullDecompressedGenerateDatasetsXmlDirectory,
            EDStatic.decompressedCacheMaxGB,
            false); // reuseExisting
    table.readAudioFile(decomSampleFileName, false, true); // getData, addElapsedTimeColumn
    String2.log(table.toString(0));
    int nCols = table.nColumns();

    // make tables to hold info
    Table axisSourceTable = new Table();
    Table dataSourceTable = new Table();
    Table axisAddTable = new Table();
    Table dataAddTable = new Table();
    StringBuilder sb = new StringBuilder();

    // get source global Attributes
    Attributes globalSourceAtts = table.globalAttributes();

    // add axisVariables
    String varName = extractColumnName;
    Attributes sourceAtts = new Attributes();
    Attributes addAtts = new Attributes().add("units", "seconds since 1970-01-01T00:00:00Z");
    PAType tPAType = null;
    if (extractDataType.startsWith("timeFormat=")) {
      tPAType = PAType.STRING;
    } else {
      tPAType = PAType.fromCohortString(extractDataType);
    }
    PrimitiveArray pa = PrimitiveArray.factory(tPAType, 1, false);
    axisSourceTable.addColumn(
        0,
        // ***fileName,dataType,extractRegex,captureGroupNumber
        "***fileName,"
            + String2.toJson(extractDataType)
            + ","
            + String2.toJson(extractFileNameRegex)
            + ",1",
        pa,
        sourceAtts);
    axisAddTable.addColumn(0, varName, pa, addAtts);

    // default queries
    String et = Table.ELAPSED_TIME;
    String defGraph =
        "channel_1[0][(0):(1)]" + "&.draw=lines&.vars=" + et + "|" + extractColumnName;
    String defData = "&time=min(time)";

    varName = table.getColumnName(0); // Table.ELAPSED_TIME
    sourceAtts = table.columnAttributes(0);
    addAtts = new Attributes();
    if (EDStatic.variablesMustHaveIoosCategory) addAtts.set("ioos_category", "Time");
    pa = table.getColumn(0);
    axisSourceTable.addColumn(1, varName, pa, sourceAtts);
    axisAddTable.addColumn(1, varName, pa, addAtts);

    // add the dataVariables
    for (int col = 1; col < nCols; col++) {
      varName = table.getColumnName(col);
      pa = table.getColumn(col);
      sourceAtts = table.columnAttributes(col);
      addAtts = new Attributes();
      if (EDStatic.variablesMustHaveIoosCategory) addAtts.set("ioos_category", "Other");
      if (pa.isIntegerType()) {
        addAtts
            .add("colorBarMinimum", Math2.niceDouble(-pa.missingValueAsDouble(), 2))
            .add("colorBarMaximum", Math2.niceDouble(pa.missingValueAsDouble(), 2));
      }
      dataSourceTable.addColumn(col - 1, varName, pa, sourceAtts);
      dataAddTable.addColumn(col - 1, varName, pa, addAtts);

      // add missing_value and/or _FillValue if needed
      addMvFvAttsIfNeeded(varName, pa, sourceAtts, addAtts);
    }

    // after dataVariables known, add global attributes in the axisAddTable
    Attributes globalAddAtts = axisAddTable.globalAttributes();
    globalAddAtts.set(
        makeReadyToUseAddGlobalAttributesForDatasetsXml(
            globalSourceAtts,
            "Grid", // another cdm type could be better; this is ok
            tFileDir,
            externalAddGlobalAttributes,
            EDD.chopUpCsvAndAdd(
                axisAddTable.getColumnNamesCSVString(),
                suggestKeywords(dataSourceTable, dataAddTable))));
    if ("Data from a local source.".equals(globalAddAtts.getString("summary")))
      globalAddAtts.set("summary", "Audio data from a local source.");
    if ("Data from a local source.".equals(globalAddAtts.getString("title")))
      globalAddAtts.set("title", "Audio data from a local source.");

    // gather the results
    String tDatasetID = suggestDatasetID(tFileDir + tFileNameRegex);
    boolean accViaFiles = false;
    int tMatchNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

    ensureValidNames(dataSourceTable, dataAddTable);

    // write results
    sb.append(
        "<dataset type=\"EDDGridFromAudioFiles\" datasetID=\""
            + tDatasetID
            + "\" active=\"true\">\n"
            + "    <defaultGraphQuery>"
            + XML.encodeAsXML(defGraph)
            + "</defaultGraphQuery>\n"
            + "    <defaultDataQuery>"
            + XML.encodeAsXML(defData)
            + "</defaultDataQuery>\n"
            + "    <reloadEveryNMinutes>"
            + tReloadEveryNMinutes
            + "</reloadEveryNMinutes>\n"
            + (String2.isUrl(tCacheFromUrl)
                ? "    <cacheFromUrl>" + XML.encodeAsXML(tCacheFromUrl) + "</cacheFromUrl>\n"
                : "    <updateEveryNMillis>"
                    + suggestUpdateEveryNMillis(tFileDir)
                    + "</updateEveryNMillis>\n")
            + "    <fileDir>"
            + XML.encodeAsXML(tFileDir)
            + "</fileDir>\n"
            + "    <fileNameRegex>"
            + XML.encodeAsXML(tFileNameRegex)
            + "</fileNameRegex>\n"
            + "    <recursive>true</recursive>\n"
            + "    <pathRegex>.*</pathRegex>\n"
            + "    <metadataFrom>last</metadataFrom>\n"
            + "    <matchAxisNDigits>"
            + tMatchNDigits
            + "</matchAxisNDigits>\n"
            + "    <dimensionValuesInMemory>false</dimensionValuesInMemory>\n"
            + "    <fileTableInMemory>false</fileTableInMemory>\n");

    sb.append(writeAttsForDatasetsXml(false, globalSourceAtts, "    "));
    sb.append(writeAttsForDatasetsXml(true, globalAddAtts, "    "));

    // last 2 params: includeDataType, questionDestinationName
    sb.append(
        writeVariablesForDatasetsXml(axisSourceTable, axisAddTable, "axisVariable", false, false));
    sb.append(
        writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));
    sb.append("</dataset>\n" + "\n");

    String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");

    return sb.toString();
  }
}
