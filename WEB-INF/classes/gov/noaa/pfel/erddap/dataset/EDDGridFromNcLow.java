/*
 * EDDGridFromNcLow Copyright 2015, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Units2;
import com.cohort.util.XML;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.FileVisitorDNLS;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ucar.ma2.*;
import ucar.nc2.*;
// import ucar.nc2.dods.*;
import ucar.nc2.util.*;

/**
 * This class represents gridded data aggregated from a collection of NetCDF .nc
 * (https://www.unidata.ucar.edu/software/netcdf/), GRIB .grb (https://en.wikipedia.org/wiki/GRIB),
 * (and related) data files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2009-01-05
 */
public abstract class EDDGridFromNcLow extends EDDGridFromFiles {

  /** subclasses have different subClassNames. */
  public String subClassName() {
    return null;
  }

  /**
   * Subclasses overwrite this: EDDGridFromNcFilesUnpacked applies scale_factor and add_offset,
   * converts times variables to epochSeconds at a low level (when it reads each file), and
   * standardizes the units. Also the &lt;dataType&gt; is applied.
   */
  public boolean unpack() {
    return false;
  }

  /** Used by Bob only. Don't set this to true here -- do it in the calling code. */
  public static boolean generateDatasetsXmlCoastwatchErdMode = false;

  /** The constructor just calls the super constructor. */
  public EDDGridFromNcLow(
      String subclassname,
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
        subclassname,
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
  }

  /**
   * This gets sourceGlobalAttributes and sourceDataAttributes from the specified source file.
   *
   * @param tFullName the name of the decompressed data file
   * @param sourceAxisNames If there is a special axis0, this list will be the instances list[1 ...
   *     n-1]. If in a group, the name must be the fullName.
   * @param sourceDataNames the names of the desired source data columns. If in a group, the name
   *     must be the fullName.
   * @param sourceDataTypes the data types of the desired source columns (e.g., "String" or "float")
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

    String getWhat = "globalAttributes";
    String group = "";
    int groupSlashCount = 0;
    NetcdfFile ncFile = NcHelper.openFile(tFullName);
    try {

      // This is cognizant of special axis0
      for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
        getWhat = "axisAttributes for avi=" + avi + " name=" + sourceAxisNames.get(avi);

        // try to find the lowest level group (most levels)
        String tGroup = File2.getDirectory(sourceAxisNames.get(avi));
        if (tGroup.length() > 0) {
          int tGroupSlashCount = String2.countAll(tGroup, "/");
          if (group.length() == 0 || tGroupSlashCount > groupSlashCount) {
            group = tGroup;
            groupSlashCount = tGroupSlashCount;
          }
        }

        Variable var = ncFile.findVariable(sourceAxisNames.get(avi));
        Attributes tAtts = sourceAxisAttributes[avi];
        if (var == null) {
          // it will be null for dimensions without corresponding coordinate axis variable
          tAtts.add("units", "count"); // "count" is udunits;  "index" isn't, but better?
        } else {
          NcHelper.getVariableAttributes(var, tAtts);

          // unpack?
          if (unpack()) {
            Units2.unpackVariableAttributes(
                tAtts, var.getFullName(), NcHelper.getElementPAType(var));
            // shouldn't be any mv or fv
          }
        }
      }

      for (int dvi = 0; dvi < sourceDataNames.size(); dvi++) {
        getWhat = "dataAttributes for dvi=" + dvi + " name=" + sourceDataNames.get(dvi);

        // try to find the lowest level group (most levels)
        String tGroup = File2.getDirectory(sourceDataNames.get(dvi));
        if (tGroup.length() > 0) {
          int tGroupSlashCount = String2.countAll(tGroup, "/");
          if (group.length() == 0 || tGroupSlashCount > groupSlashCount) {
            group = tGroup;
            groupSlashCount = tGroupSlashCount;
          }
        }

        // if structure member, get atts for structure
        String tName = sourceDataNames.get(dvi);
        int spo = tName.indexOf(STRUCTURE_MEMBER_SEPARATOR);
        if (spo >= 0) tName = tName.substring(0, spo);
        Variable var = ncFile.findVariable(tName); // null if not found
        if (var == null) {
          String2.log("  var not in file: " + getWhat);
        } else {
          Attributes tAtts = sourceDataAttributes[dvi];
          NcHelper.getVariableAttributes(var, tAtts);

          // unpack?
          if (unpack())
            Units2.unpackVariableAttributes(
                tAtts, var.getFullName(), NcHelper.getElementPAType(var));
        }
      }

      // get group atts and all higher groups (up to root)
      NcHelper.getGroupAttributes(ncFile.findGroup(group), sourceGlobalAttributes);

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error in "
              + subClassName()
              + ".getSourceMetadata"
              + "\nwhile getting "
              + getWhat
              + "\nfrom "
              + tFullName
              + "\nCause: "
              + MustBe.throwableToShortString(t),
          t);
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This gets source axis values from one file.
   *
   * @param tFullName the full file name
   * @param sourceAxisNames the names of the desired source axis variables. If there is a special
   *     axis0, this will not include axis0's name.
   * @param sourceDataNames When there are unnamed dimensions, this is to find out the shape of the
   *     variable to make index values 0, 1, size-1.
   * @return a PrimitiveArray[] with the results (with the requested sourceDataTypes). The values
   *     will be unpacked if unpack()=true. It needn't set sourceGlobalAttributes or
   *     sourceDataAttributes (but see getSourceMetadata).
   * @throws Throwable if trouble (e.g., invalid file). If there is trouble, this doesn't call
   *     addBadFile or requestReloadASAP().
   */
  @Override
  public PrimitiveArray[] lowGetSourceAxisValues(
      String tFullName, StringArray sourceAxisNames, StringArray sourceDataNames) throws Throwable {

    String getWhat = "?";
    NetcdfFile ncFile = NcHelper.openFile(tFullName);
    try {
      PrimitiveArray[] avPa = new PrimitiveArray[sourceAxisNames.size()];

      // try to find 1 dataVariable in case needed below
      Variable dataVariable = null;
      for (int dvi = 0; dvi < sourceDataNames.size(); dvi++) {
        String sdn = sourceDataNames.get(dvi);
        int spo = sdn.indexOf(STRUCTURE_MEMBER_SEPARATOR);
        if (spo >= 0) sdn = sdn.substring(0, spo);
        dataVariable = ncFile.findVariable(sdn);
        if (dataVariable != null) break;
      }

      for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
        String avName = sourceAxisNames.get(avi);
        getWhat = "axisAttributes for variable=" + avName;
        Variable var = ncFile.findVariable(avName); // null if not found
        // String2.log(">> " + getWhat + "  var==null?" + (var == null));
        if (var == null) {
          // if there is no corresponding coordinate variable, make pa of indices, 0...
          Dimension dim = ncFile.findDimension(avName);
          int dimSize1 =
              dim == null
                  ? dataVariable.getShape(avi) - 1
                  : // no dimension (in hdf)
                  dim.getLength() - 1; // unnamed dimension
          avPa[avi] =
              avi > 0 && dimSize1 < 32000 ? new ShortArray(0, dimSize1) : new IntArray(0, dimSize1);
        } else {
          avPa[avi] = NcHelper.getPrimitiveArray(var);
          if (unpack())
            avPa[avi] =
                NcHelper.unpackPA(var, avPa[avi], true, true); // lookForStringTime, lookForUnsigned
        }
      }

      return avPa;

    } catch (Throwable t) {
      throw new RuntimeException(
          "Error in "
              + subClassName()
              + ".getSourceAxisValues"
              + "\nwhile getting "
              + getWhat
              + "\nfrom "
              + tFullName
              + "\nCause: "
              + MustBe.throwableToShortString(t),
          t);
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This gets source data from one file.
   *
   * @param tFullName of the file
   * @param tDataVariables the desired data variables
   * @param tConstraints For each axis variable, there will be 3 numbers (startIndex, stride,
   *     stopIndex). !!! If there is a special axis0, this will not include constraints for axis0.
   * @return a PrimitiveArray[] with an element for each tDataVariable with the dataValues. <br>
   *     The values will be unpacked if unpack()=true. <br>
   *     The primitiveArray dataTypes are usually the sourceDataPAType, but can be any type.
   *     EDDGridFromFiles will convert to the sourceDataPAType. <br>
   *     Note the lack of axisVariable values!
   * @throws Throwable if trouble (notably, WaitThenTryAgainException). If there is trouble, this
   *     doesn't call addBadFile or requestReloadASAP().
   */
  @Override
  public PrimitiveArray[] lowGetSourceDataFromFile(
      String tFullName, EDV tDataVariables[], IntArray tConstraints) throws Throwable {

    // make the selection spec  and get the axis values
    int nav = tConstraints.size() / 3; // deals with special axis0
    int ndv = tDataVariables.length;
    PrimitiveArray[] paa = new PrimitiveArray[ndv];
    StringBuilder selectionSB = new StringBuilder();
    for (int avi = 0; avi < nav; avi++) {
      selectionSB.append(
          (avi == 0 ? "" : ",")
              + tConstraints.get(avi * 3)
              + ":"
              + tConstraints.get(avi * 3 + 2)
              + ":"
              + tConstraints.get(avi * 3 + 1)); // start:STOP:stride !
    }
    String selection = selectionSB.toString();
    int nValues = -1; // not yet calculated
    EDV edv = null;

    NetcdfFile ncFile = NcHelper.openFile(tFullName);
    try {

      for (int dvi = 0; dvi < ndv; dvi++) {
        edv = tDataVariables[dvi];

        // is it in a structure?
        int po = edv.sourceName().indexOf(NcHelper.STRUCTURE_MEMBER_SEPARATOR);
        if (po > 0) {
          // Reading 1 member at a time isn't the most efficient approach,
          // but it is the simplist solution by far when a request may
          // include data from multiple structures and variables.
          try {
            paa[dvi] =
                NcHelper.readStructure(
                    ncFile,
                    edv.sourceName().substring(0, po),
                    new String[] {edv.sourceName().substring(po + 1)},
                    tConstraints)[0]; // [0] the first and only pa returned

          } catch (Exception e) {
            // and paa[dvi] will be null, which will be dealt with below
            String2.log(
                "Caught exception will reading sourceName="
                    + edv.sourceName()
                    + ":\n"
                    + MustBe.throwableToString(e));
          }

        } else {

          // is it a regular variable?
          Variable var = ncFile.findVariable(edv.sourceName());
          if (var != null) {
            String tSel = selection;
            if (edv.sourceDataPAType() == PAType.STRING)
              tSel += ",0:" + (var.getShape(var.getRank() - 1) - 1);
            paa[dvi] = NcHelper.getPrimitiveArray(var.read(tSel), true, NcHelper.isUnsigned(var));
            // 2020-02-27 WARNING: in netcdf-java 5+, when reading nc3 file,
            //  variable with _Unsigned="true" behaves in raw way
            /*
            String2.log(">> EDDGridFrimNcFilesLow.getSourceDataFromFile " + edv.sourceName() +
                " sourceDataPAType()=" + edv.sourceDataPAType() +
                " var.getDataType()=" + var.getDataType() +                //returns raw (signed) dataType
                " dataType.isUnsigned=" + var.getDataType().isUnsigned() + //returns false
                " pa.elementType()=" + paa[dvi].elementType() +
                " pa.isUnsigned=" + paa[dvi].isUnsigned() );
                //    "[" + selection + "]\n" + paa[dvi].toString());
            /* */

            if (unpack())
              paa[dvi] =
                  NcHelper.unpackPA(
                      var, paa[dvi], true,
                      true); // lookForStringTime, lookForUnsigned (which changes type, eg unsigned
            // byte to signed short)

            nValues = paa[dvi].size();
          }
        }

        // this var isn't in this file: return array of missing_values
        if (paa[dvi] == null) {
          if (nValues == -1) {
            nValues = 1;
            for (int avi = 0; avi < nav; avi++) {
              nValues *=
                  OpendapHelper.calculateNValues(
                      tConstraints.get(avi * 3),
                      tConstraints.get(avi * 3 + 1), // stride
                      tConstraints.get(avi * 3 + 2));
            }
          }
          PAType tPAType = edv.sourceDataPAType(); // appropriate even if unpacked
          if (tPAType == null) {
            String2.log("source file=" + tFullName);
            throw new RuntimeException(
                "ERROR: The destinationName="
                    + edv.destinationName()
                    + " variable isn't in one of the source files and "
                    + " the variable's sourceDataType wasn't specified.");
          }
          paa[dvi] = PrimitiveArray.factory(tPAType, nValues, false); // active?
          paa[dvi].addNDoubles(
              nValues,
              !Double.isNaN(edv.sourceFillValue())
                  ? edv.sourceFillValue()
                  : edv.sourceMissingValue());
        }
      }

      // I care about this exception
      return paa;

    } catch (Throwable t) {
      String2.log(
          "ERROR: while reading sourceName="
              + (edv == null ? "null" : edv.sourceName())
              + "["
              + selection
              + "] (start:STOP:stride).");
      throw t;
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
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
    throw new SimpleException("Error: " + subClassName() + " doesn't support method=\"sibling\".");
  }

  /**
   * This does its best to generate a clean, ready-to-use datasets.xml entry for an
   * EDDGridFromNcFiles. The XML can then be edited by hand and added to the datasets.xml file.
   *
   * <p>This can't be made into a web service because it would allow any user to looks at (possibly)
   * private .nc files on the server.
   *
   * @param tFileDir the starting (parent) directory for searching for files
   * @param tFileNameRegex the regex that each filename (no directory info) must match (e.g.,
   *     ".*\\.nc") (usually only 1 backslash; 2 here since it is Java code).
   * @param sampleFileName full file name of one of the files in the collection
   * @param tGroup the name of the group to be used (else "" for all/any or "[root]" for just root)
   *     (without trailing slash)
   * @param externalAddGlobalAttributes These are given priority. Use null if none available.
   * @return a suggested chunk of xml for this dataset for use in datasets.xml
   * @throws Throwable if trouble, e.g., if no Grid or Array variables are found. If no trouble,
   *     then a valid dataset.xml chunk has been returned.
   */
  public static String generateDatasetsXml(
      String subclassname,
      String tFileDir,
      String tFileNameRegex,
      String sampleFileName,
      String tGroup,
      String tDimensionsCSV,
      int tReloadEveryNMinutes,
      String tCacheFromUrl,
      Attributes externalAddGlobalAttributes)
      throws Throwable {

    String2.log(
        "\n*** "
            + subclassname
            + ".generateDatasetsXml"
            + "\nfileDir="
            + tFileDir
            + " fileNameRegex="
            + tFileNameRegex
            + " sampleFileName="
            + sampleFileName
            + "\ngroup="
            + tGroup
            + " dimensionsCSV="
            + tDimensionsCSV
            + " reloadEveryNMinutes="
            + tReloadEveryNMinutes
            + "\nexternalAddGlobalAttributes="
            + externalAddGlobalAttributes);
    boolean tUnpack = "EDDGridFromNcFilesUnpacked".equals(subclassname);
    if (!String2.isSomething(tFileDir))
      throw new IllegalArgumentException("fileDir wasn't specified.");
    if (tFileDir.endsWith("/catalog.html")) // thredds catalog
    tFileDir = tFileDir.substring(0, tFileDir.length() - 12);
    else if (tFileDir.endsWith("/catalog.xml")) // thredds catalog
    tFileDir = tFileDir.substring(0, tFileDir.length() - 11);
    else if (tFileDir.endsWith("/contents.html")) // hyrax catalog
    tFileDir = tFileDir.substring(0, tFileDir.length() - 13);
    else tFileDir = File2.addSlash(tFileDir); // otherwise, assume tFileDir is missing final slash
    tFileNameRegex = String2.isSomething(tFileNameRegex) ? tFileNameRegex.trim() : ".*";
    if (String2.isRemote(tCacheFromUrl))
      FileVisitorDNLS.sync(
          tCacheFromUrl,
          tFileDir,
          tFileNameRegex,
          true,
          ".*",
          false); // not fullSync, so just get 1

    if (!String2.isSomething(sampleFileName))
      String2.log(
          "Found/using sampleFileName="
              + (sampleFileName =
                  FileVisitorDNLS.getSampleFileName(
                      tFileDir, tFileNameRegex, true, ".*"))); // recursive, pathRegex

    String decomSampleFileName =
        FileVisitorDNLS.decompressIfNeeded(
            sampleFileName,
            tFileDir,
            EDStatic.fullDecompressedGenerateDatasetsXmlDirectory,
            EDStatic.decompressedCacheMaxGB,
            false); // reuseExisting
    String2.log("Let's see if netcdf-java can tell us the structure of the sample file:");
    String2.log(NcHelper.ncdump(decomSampleFileName, "-h"));

    StringBuilder sb = new StringBuilder();
    Attributes gridMappingAtts = null;
    NetcdfFile ncFile = NcHelper.openFile(decomSampleFileName);
    try {

      // make table to hold info
      Table axisSourceTable = new Table();
      Table dataSourceTable = new Table();
      Table axisAddTable = new Table();
      Table dataAddTable = new Table();
      double maxTimeES = Double.NaN; // epoch seconds

      // standardize tDimensionsCSV (useful for suggestDatasetID below)
      tDimensionsCSV =
          String2.isSomething(tDimensionsCSV) ? String2.replaceAll(tDimensionsCSV, " ", "") : "";
      // find axisVariables
      List<Dimension> useDims = new ArrayList();
      if (String2.isSomething(tDimensionsCSV)) {
        StringArray tDimNames = StringArray.fromCSVNoBlanks(tDimensionsCSV);
        for (int i = 0; i < tDimNames.size(); i++) {
          Dimension tDim = ncFile.findDimension(tDimNames.get(i));
          if (tDim == null)
            throw new RuntimeException(
                String2.ERROR + ": dimension=" + tDimNames.get(i) + " not found in the file!");
          useDims.add(tDim);
        }
      } else {
        Variable maxDVariables[] =
            NcHelper.findMaxDVariables(
                ncFile,
                tGroup); // handles "[root]". throws exception if no such group or no vars with
        // dimensions
        useDims = maxDVariables[0].getDimensions(); // what is getDimensionsAll()?
        if (!String2.isSomething(tGroup)) {
          // look for a group (so global atts includes that group below)
          for (int i = 0; i < maxDVariables.length; i++) {
            tGroup = File2.getDirectory(maxDVariables[0].getFullName());
            if (String2.isSomething(tGroup)) {
              tGroup = File2.removeSlash(tGroup);
              break;
            }
          }
        }
      }
      int nUseDims = useDims.size();
      if (tGroup.equals("[root]")) tGroup = "";

      // get source global Attributes
      Attributes globalSourceAtts = axisSourceTable.globalAttributes();
      NcHelper.getGroupAttributes(ncFile.findGroup(tGroup), globalSourceAtts);

      // create the axisVariables for those dimensions
      StringArray dimNames = new StringArray();
      for (int avi = 0; avi < nUseDims; avi++) {
        Dimension tDim = useDims.get(avi);
        // 2021-01-07 I'm not sure that it is always true that I can use tGroup as prefix
        //  If trouble, make sure NcHelper.findMaxDVariables() above always returns
        //  vars from same group.
        String dimName = tDim.getName(); // getName is the short name! (full name not available)
        // dimName will be null (or ""?) for unnamed dimensions, e.g., in .hdf files
        // String2.pressEnterToContinue(">> avi#" + avi + " name=" + dimName);
        String axisName =
            String2.ifSomethingConcat(
                tGroup, "/", String2.isSomething(dimName) ? dimName : "axis" + avi);
        Attributes sourceAtts = new Attributes();
        if (String2.isSomething(dimName)) { // dimName, not axisName which is always something
          Variable axisVar = ncFile.findVariable(axisName);
          if (axisVar != null) { // it will be null for dimension without same-named coordinate axis
            // variable
            NcHelper.getVariableAttributes(axisVar, sourceAtts);
            if (tUnpack)
              Units2.unpackVariableAttributes(
                  sourceAtts, axisVar.getFullName(), NcHelper.getElementPAType(axisVar));

            // if time, try to get maxTimeES
            String tUnits = sourceAtts.getString("units");
            if (Calendar2.isNumericTimeUnits(tUnits)) {
              try {
                double tbf[] = Calendar2.getTimeBaseAndFactor(tUnits); // throws exception
                PrimitiveArray tpa = NcHelper.getPrimitiveArray(axisVar);
                maxTimeES =
                    Calendar2.unitsSinceToEpochSeconds(
                        tbf[0], tbf[1], tpa.getDouble(tpa.size() - 1));
              } catch (Throwable t) {
                String2.log("caught while trying to get maxTimeES: " + MustBe.throwableToString(t));
              }
            }
          }
        }
        dimNames.add(axisName);
        axisSourceTable.addColumn(
            avi,
            axisName,
            new DoubleArray(), // type doesn't matter
            sourceAtts);
        axisAddTable.addColumn(
            avi,
            axisName,
            new DoubleArray(), // type doesn't matter
            makeReadyToUseAddVariableAttributesForDatasetsXml(
                globalSourceAtts,
                sourceAtts,
                null,
                axisName,
                true, // tryToAddStandardName
                false,
                true)); // addColorBarMinMax, tryToFindLLAT
      }

      // add all the data (non-axis) variables which use those dimensions
      List allVariables = ncFile.getVariables();
      int nGridsAtSource = 0;
      for (int v = 0; v < allVariables.size(); v++) {
        Variable var = (Variable) allVariables.get(v);
        String varName = var.getFullName();
        String groupName = File2.removeSlash(File2.getDirectory(varName));

        // does it use the same dimensions?
        List<Dimension> dimensions = var.getDimensions();
        if (dimensions == null || dimensions.size() < 1) continue;
        // is this an axis variable?
        if (dimensions.size() == 1 && nUseDims == 1 && varName.equals(dimNames.get(0))) continue;

        // is this a structure?
        if (var instanceof Structure struct) {

          // does this structure have the expected dimensions
          int nDim =
              dimensions
                  .size(); // assume no Char->String in nc4 files   // - (tPAType == PAType.CHAR? 1
          // : 0);
          if (nDim > 1) // don't skip if nDim==1, since dataset might serve it.
          nGridsAtSource++;
          if (nDim != nUseDims) continue;
          boolean allMatch = true;
          for (int avi = 0; avi < nUseDims; avi++) {
            if (debugMode)
              String2.log(
                  ">> varName="
                      + varName
                      + " dim check: "
                      + useDims.get(avi).getName()
                      + " == "
                      + dimensions.get(avi).getName()
                      + " ?"); // the short name
            // .equals says true for unnamed dims with same size. That's trouble
            // but it is better to find too many vars (which might all be desired), than to find 0
            if (!useDims.get(avi).equals(dimensions.get(avi))) {
              allMatch = false;
              break;
            }
          }
          if (!allMatch) continue;

          // Okay. Add the members of this structure.

          // Structures (a subclass of Variable) have atts, StructureMembers don't
          Attributes sourceAtts = new Attributes();
          NcHelper.getVariableAttributes(var, sourceAtts);

          // does this var point to a pseudo-data grid_mapping variable?
          if (gridMappingAtts == null)
            gridMappingAtts =
                NcHelper.getGridMappingAtts(ncFile, sourceAtts.getString("grid_mapping"));

          // add each of the members
          StructureMembers sm = struct.makeStructureMembers();
          // System.out.println("sm=" + sm);
          List<StructureMembers.Member> memList = sm.getMembers();
          int nMembers = memList.size();
          for (int m = 0; m < nMembers; m++) {

            StructureMembers.Member smm = memList.get(m);
            String smFullName = varName + STRUCTURE_MEMBER_SEPARATOR + smm.getName();
            PAType tPAType = NcHelper.getElementPAType(smm.getDataType());

            // now switch to type that will be used for PrimitiveArray
            if (tPAType == PAType.CHAR) tPAType = PAType.STRING;
            else if (tPAType == PAType.BOOLEAN) tPAType = PAType.BYTE;
            PrimitiveArray sourcePA = PrimitiveArray.factory(tPAType, 1, false);

            // add the dataVariable
            if (tUnpack) {
              sourcePA =
                  sourceAtts.unpackPA(
                      smFullName, sourcePA, true, true); // lookForStringTime, lookForUnsigned
              Units2.unpackVariableAttributes(
                  sourceAtts, // after unpackPA
                  smFullName,
                  NcHelper.getElementPAType(var));
            }
            dataSourceTable.addColumn(dataSourceTable.nColumns(), smFullName, sourcePA, sourceAtts);
            PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
            Attributes destAtts =
                makeReadyToUseAddVariableAttributesForDatasetsXml(
                    globalSourceAtts,
                    sourceAtts,
                    null,
                    smFullName,
                    destPA.elementType() != PAType.STRING, // tryToAddStandardName
                    destPA.elementType() != PAType.STRING, // addColorBarMinMax
                    false); // tryToFindLLAT
            dataAddTable.addColumn(dataAddTable.nColumns(), smFullName, destPA, destAtts);

            // add missing_value and/or _FillValue if needed
            addMvFvAttsIfNeeded(
                smFullName, sourcePA, sourceAtts, destAtts); // sourcePA since strongly typed
          }

        } else { // var is a regular variable

          PAType tPAType = NcHelper.getElementPAType(var);
          int nDim = dimensions.size() - (tPAType == PAType.CHAR ? 1 : 0);
          if (nDim > 1) // don't skip if nDim==1, since dataset might serve it.
          nGridsAtSource++;
          if (nDim != nUseDims) continue;
          // now switch to type that will be used for PrimitiveArray
          if (tPAType == PAType.CHAR) tPAType = PAType.STRING;
          else if (tPAType == PAType.BOOLEAN) tPAType = PAType.BYTE;
          PrimitiveArray sourcePA = PrimitiveArray.factory(tPAType, 1, false);
          boolean allMatch = true;
          for (int avi = 0; avi < nUseDims; avi++) {
            if (debugMode)
              String2.log(
                  ">> varName="
                      + varName
                      + " dim check: "
                      + useDims.get(avi).getName()
                      + " == "
                      + dimensions.get(avi).getName()
                      + " ?"); // the short name
            // .equals says true for unnamed dims with same size. That's trouble
            // but it is better to find too many vars (which might all be desired), than to find 0
            if (!useDims.get(avi).equals(dimensions.get(avi))) {
              allMatch = false;
              break;
            }
          }
          if (!allMatch) continue;

          // add the dataVariable
          Attributes sourceAtts = new Attributes();
          NcHelper.getVariableAttributes(var, sourceAtts);

          // does this var point to a pseudo-data grid_mapping variable?
          if (gridMappingAtts == null)
            gridMappingAtts =
                NcHelper.getGridMappingAtts(ncFile, sourceAtts.getString("grid_mapping"));

          if (tUnpack) {
            sourcePA =
                sourceAtts.unpackPA(
                    var.getFullName(), sourcePA, true, true); // lookForStringTime, lookForUnsigned
            Units2.unpackVariableAttributes(
                sourceAtts, // after unpackPA
                var.getFullName(),
                NcHelper.getElementPAType(var));
          }
          dataSourceTable.addColumn(dataSourceTable.nColumns(), varName, sourcePA, sourceAtts);
          PrimitiveArray destPA = makeDestPAForGDX(sourcePA, sourceAtts);
          Attributes destAtts =
              makeReadyToUseAddVariableAttributesForDatasetsXml(
                  globalSourceAtts,
                  sourceAtts,
                  null,
                  varName,
                  destPA.elementType() != PAType.STRING, // tryToAddStandardName
                  destPA.elementType() != PAType.STRING, // addColorBarMinMax
                  false); // tryToFindLLAT
          dataAddTable.addColumn(dataAddTable.nColumns(), varName, destPA, destAtts);

          // add missing_value and/or _FillValue if needed
          addMvFvAttsIfNeeded(
              varName, sourcePA, sourceAtts, destAtts); // sourcePA since strongly typed
        }
      }

      if (dataAddTable.nColumns() == 0)
        throw new RuntimeException(
            "No dataVariables found which match dimensions: " + tDimensionsCSV);

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
      if (gridMappingAtts != null) globalAddAtts.add(gridMappingAtts);

      // gather the results
      String tDatasetID =
          suggestDatasetID(
              tGroup
                  + (String2.isSomething(tGroup) ? "/" : "")
                  + tFileDir
                  + tFileNameRegex
                  + tDimensionsCSV);
      int tMatchNDigits = DEFAULT_MATCH_AXIS_N_DIGITS;

      if (generateDatasetsXmlCoastwatchErdMode) {
        tMatchNDigits = 15;
        //  /u00/satellite/AT/ssta/1day/
        Pattern pattern = Pattern.compile("/u00/satellite/([^/]+)/([^/]+)/([^/]+)day/");
        Matcher matcher = pattern.matcher(tFileDir);
        String m1, m12, m1_2; // ATssta  AT_ssta
        String cl; // composite length
        if (matcher.matches()) {
          m1 = matcher.group(1);
          m12 = matcher.group(1) + matcher.group(2);
          m1_2 = matcher.group(1) + "_" + matcher.group(2);
          cl = matcher.group(3);
        } else {
          //  /u00/satellite/MPIC/1day/
          pattern = Pattern.compile("/u00/satellite/([^/]+)/([^/]+)day/");
          matcher = pattern.matcher(tFileDir);
          if (matcher.matches()) {
            m1 = matcher.group(1);
            m12 = matcher.group(1);
            m1_2 = m12;
            cl = matcher.group(2);
          } else {
            throw new RuntimeException(tFileDir + " doesn't match the pattern!");
          }
        }

        tDatasetID = "erd" + m12 + cl + "day";
        if (!"MH1".equals(m1)) {
          globalAddAtts.set("creator_name", "NOAA NMFS SWFSC ERD");
          globalAddAtts.set("creator_email", "erd.data@noaa.gov");
          globalAddAtts.set("creator_url", "https://www.pfeg.noaa.gov");
          globalAddAtts.set("institution", "NOAA NMFS SWFSC ERD");
        }
        globalAddAtts.set("publisher_name", "NOAA NMFS SWFSC ERD");
        globalAddAtts.set("publisher_email", "erd.data@noaa.gov");
        globalAddAtts.set("publisher_url", "https://www.pfeg.noaa.gov");
        globalAddAtts.set("id", tDatasetID); // 2019-05-07 was "null");
        globalAddAtts.set(
            "infoUrl", "https://coastwatch.pfeg.noaa.gov/infog/" + m1_2 + "_las.html");
        globalAddAtts.set("license", "[standard]");
        globalAddAtts.remove("summary");
        globalAddAtts.set(
            "title",
            globalSourceAtts.getString("title")
                + " ("
                + (cl.equals("h")
                    ? "Single Scan"
                    : cl.equals("m") ? "Monthly Composite" : cl + " Day Composite")
                + ")");

        for (int dv = 0; dv < dataSourceTable.nColumns(); dv++) {
          dataAddTable.columnAttributes(dv).set("long_name", "!!! FIX THIS !!!");
          if (dataSourceTable.columnAttributes(dv).get("actual_range") != null)
            dataAddTable.columnAttributes(dv).set("actual_range", "null");
        }
      }

      // String2.log(">> nGridsAtSource=" + nGridsAtSource);
      if (nGridsAtSource > dataAddTable.nColumns())
        sb.append(
            generateDatasetsXmlCoastwatchErdMode
                ? ""
                : "<!-- NOTE! The source for this dataset has nGridVariables="
                    + nGridsAtSource
                    + ",\n"
                    + "  but this dataset will only serve "
                    + dataAddTable.nColumns()
                    + " because the others use different dimensions. -->\n");

      // tryToFindLLAT
      tryToFindLLAT(axisSourceTable, axisAddTable); // just axisTables
      ensureValidNames(dataSourceTable, dataAddTable);

      // use maxTimeES
      if (tReloadEveryNMinutes <= 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
        tReloadEveryNMinutes = 1440; // 1440 works well with suggestedUpdateEveryNMillis

      String tTestOutOfDate =
          EDD.getAddOrSourceAtt(globalAddAtts, globalSourceAtts, "testOutOfDate", null);
      if (Double.isFinite(maxTimeES) && !String2.isSomething(tTestOutOfDate)) {
        tTestOutOfDate = suggestTestOutOfDate(maxTimeES);
        if (String2.isSomething(tTestOutOfDate)) globalAddAtts.set("testOutOfDate", tTestOutOfDate);
      }

      // write results
      sb.append(
          "<dataset type=\""
              + subclassname
              + "\" datasetID=\""
              + tDatasetID
              + "\" active=\"true\">\n"
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
              + "    <fileTableInMemory>false</fileTableInMemory>\n");

      sb.append(writeAttsForDatasetsXml(false, globalSourceAtts, "    "));
      sb.append(writeAttsForDatasetsXml(true, globalAddAtts, "    "));

      // last 2 params: includeDataType, questionDestinationName
      sb.append(
          writeVariablesForDatasetsXml(
              axisSourceTable, axisAddTable, "axisVariable", false, false));
      sb.append(
          writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, "dataVariable", true, false));
      sb.append("</dataset>\n" + "\n");

      String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
    return sb.toString();
  }
}
