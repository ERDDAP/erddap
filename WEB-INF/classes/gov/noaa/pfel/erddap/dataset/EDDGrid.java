/*
 * EDDGrid Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.NDimensionalIndex;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.ULongArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;
import com.google.common.collect.ImmutableList;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.Matlab;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.metadata.MetadataBuilder;
import gov.noaa.pfel.erddap.filetypes.DapRequestInfo;
import gov.noaa.pfel.erddap.filetypes.FileTypeInterface;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBException;
import java.awt.Color;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.opengis.metadata.Metadata;
import ucar.ma2.Array;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * This class represents a dataset where the results can be represented as a grid -- one or more EDV
 * variables sharing the same EDVGridAxis variables (in the same order). If present, the lon, lat,
 * alt, and time axisVariables allow queries to be made in standard units (alt in m above sea level
 * and time as seconds since 1970-01-01T00:00:00Z or as an ISO date/time).
 *
 * <p>Note that all variables for a given EDDGrid use the same axis variables. If there are source
 * datasets that serve variables which use different sets of axes, you have to separate them out and
 * create separate EDDGrids (one per set of axes).
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-04
 */
public abstract class EDDGrid extends EDD {

  public static final String dapProtocol = "griddap";

  /**
   * The constructor must set this. AxisVariables are counted left to right, e.g.,
   * sst[0=time][1=lat][2=lon].
   */
  protected EDVGridAxis axisVariables[];

  /**
   * This is used to test equality of axis values. 0=no testing (not recommended except for special
   * cases, e.g., S3 buckets). &gt;18 does exact test. default=20. 1-18 tests that many digets for
   * doubles and hidiv(n,2) for floats.
   */
  public static final int DEFAULT_MATCH_AXIS_N_DIGITS = 20;

  private static final List<String> publicGraphFileTypeNames;
  private static final String defaultFileTypeOption;
  private static final String defaultPublicGraphFileTypeOption;

  // wcs
  public static final String wcsServer = "server";
  public static final String wcsVersion = "1.0.0"; // , "1.1.0", "1.1.1", "1.1.2"};
  // wcsResponseFormats parallels wcsRequestFormats
  public static final ImmutableList<String> wcsRequestFormats100 =
      ImmutableList.of("GeoTIFF", "NetCDF3", "PNG");
  public static final ImmutableList<String> wcsResponseFormats100 =
      ImmutableList.of(".geotif", ".nc", ".transparentPng");
  // public final static String wcsRequestFormats112[]  = {"image/tiff", "application/x-netcdf",
  // "image/png"};
  // public final static String wcsResponseFormats112[] = {".geotif",    ".nc",
  // ".transparentPng"};
  public static final String wcsExceptions = "application/vnd.ogc.se_xml";

  // static constructor
  static {
    defaultFileTypeOption = ".htmlTable";

    List<EDDFileTypeInfo> imageTypes = EDD.getFileTypeOptions(true, true);
    publicGraphFileTypeNames = new ArrayList<String>();
    publicGraphFileTypeNames.add(".das");
    publicGraphFileTypeNames.add(".dds");
    publicGraphFileTypeNames.add(".fgdc");
    publicGraphFileTypeNames.add(".graph");
    publicGraphFileTypeNames.add(".help");
    publicGraphFileTypeNames.add(".iso19115");

    for (int i = 0; i < imageTypes.size(); i++) {
      for (int tl = 0; tl < EDStatic.messages.nLanguages; tl++)
        publicGraphFileTypeNames.add(imageTypes.get(i).getFileTypeName());
    }

    graphsAccessibleTo_fileTypeNames = new HashSet<>(); // read only, so needn't be thread-safe
    graphsAccessibleTo_fileTypeNames.addAll(publicGraphFileTypeNames);
    defaultPublicGraphFileTypeOption = ".png";
  }

  // the diagnostic tests change this just for testing
  static final int tableWriterNBufferRows = 100000;

  // *********** end of static declarations ***************************

  /**
   * The constructor should set these to indicate where the lon,lat,alt,time variables are in
   * axisVariables (or leave as -1 if not present). altIndex and depthIndex mustn't be active
   * simultaneously.
   */
  protected int lonIndex = -1, latIndex = -1, altIndex = -1, depthIndex = -1, timeIndex = -1;

  /** These are created as needed (in the constructor) from axisVariables. */
  protected String[] axisVariableSourceNames, axisVariableDestinationNames;

  protected String allDimString = null;

  protected int nThreads = -1; // interpret invalid values (like -1) as EDStatic.nGridThreads
  protected boolean dimensionValuesInMemory = true;

  /**
   * This is used by many constructors (and EDDGridFromFiles.lowUpdate) to make an EDVGridAxis
   * axisVariable.
   *
   * @param av If av >= 0, this will used to set lonIndex, latIndex, ... if appropriate. If av < 0,
   *     this does nothing.
   */
  public EDVGridAxis makeAxisVariable(
      String tParentDatasetID,
      int av,
      String tSourceName,
      String tDestName,
      Attributes tSourceAtt,
      Attributes tAddAtt,
      PrimitiveArray sourceAxisValues)
      throws Throwable {

    if (EDV.LON_NAME.equals(tDestName)) {
      if (av >= 0) lonIndex = av;
      return new EDVLonGridAxis(
          tParentDatasetID, tSourceName, tSourceAtt, tAddAtt, sourceAxisValues);
    } else if (EDV.LAT_NAME.equals(tDestName)) {
      if (av >= 0) latIndex = av;
      return new EDVLatGridAxis(
          tParentDatasetID, tSourceName, tSourceAtt, tAddAtt, sourceAxisValues);
    } else if (EDV.ALT_NAME.equals(tDestName)) {
      if (av >= 0) altIndex = av;
      return new EDVAltGridAxis(
          tParentDatasetID, tSourceName, tSourceAtt, tAddAtt, sourceAxisValues);
    } else if (EDV.DEPTH_NAME.equals(tDestName)) {
      if (av >= 0) depthIndex = av;
      return new EDVDepthGridAxis(
          tParentDatasetID, tSourceName, tSourceAtt, tAddAtt, sourceAxisValues);
    } else if (EDV.TIME_NAME.equals(tDestName)) {
      if (av >= 0) timeIndex = av;
      return new EDVTimeGridAxis(
          tParentDatasetID, tSourceName, tSourceAtt, tAddAtt, sourceAxisValues);
    } else if (EDVTimeStampGridAxis.hasTimeUnits(tSourceAtt, tAddAtt)) {
      return new EDVTimeStampGridAxis(
          tParentDatasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, sourceAxisValues);
    } else {
      EDVGridAxis edvga =
          new EDVGridAxis(
              tParentDatasetID, tSourceName, tDestName, tSourceAtt, tAddAtt, sourceAxisValues);
      edvga.setActualRangeFromDestinationMinMax();
      return edvga;
    }
  }

  /**
   * If the subclass is EDDGridFromFiles or EDDGridCopy, this returns the dirTable (or throws
   * Throwable). Other subclasses return null.
   *
   * @throws Throwable if trouble
   */
  public Table getDirTable() throws Throwable {
    return null;
  }

  /**
   * If the subclass is EDDGridFromFiles or EDDGridCopy, this returns the fileTable (or throws
   * Throwable). Other subclasses return null.
   *
   * @throws Throwable if trouble
   */
  public Table getFileTable() throws Throwable {
    return null;
  }

  /**
   * If dimensionValuesInMemory=false, the constructor should call this after ensureValid() to save
   * the dimension sourceValues in a file and tell the EDVGridAxis variables to set sourceValues to
   * null. Test of this system is EDDGridFromAudioFiles.testBasic().
   */
  protected void saveDimensionValuesInFile() throws Exception {
    if (debugMode) String2.log(">> saveDimensionValuesInFile for datasetID=" + datasetID);

    // save sourceValues in file
    int nav = axisVariables.length;
    StringArray destNames = new StringArray(); // destNames are varName safe
    PrimitiveArray pas[] = new PrimitiveArray[nav];
    for (int av = 0; av < nav; av++) {
      destNames.add(axisVariables[av].destinationName());
      pas[av] = axisVariables[av].sourceValues();
    }
    NcHelper.writePAsInNc3(datasetDir() + DIMENSION_VALUES_FILENAME, destNames, pas);

    // then: set edvga sourceValues to null
    setDimensionValuesToNull();
  }

  /**
   * If dimensionValuesInMemory=false, call this at end of saveDimensionValuesInFile,
   * respondToDapQuery, and respondToGraphQuery to set the axisVariables sourceValues to null (so
   * will be forced to read from file when next needed).
   */
  protected void setDimensionValuesToNull() {
    if (debugMode) String2.log(">> setDimensionValuesToNull for datasetID=" + datasetID);
    for (EDVGridAxis axisVariable : axisVariables) axisVariable.setSourceValuesToNull();
  }

  /**
   * This makes the searchString (mixed case) used to create searchBytes or searchDocument.
   *
   * @return the searchString (mixed case) used to create searchBytes or searchDocument.
   */
  @Override
  public String searchString() {

    // make a string to search through
    StringBuilder sb = startOfSearchString();

    // add axisVariable info
    // doing all varNames, then all attributes, treats varNames as more important
    for (EDVGridAxis variable : axisVariables) {
      sb.append("variableName=" + variable.destinationName() + "\n");
      sb.append("sourceName=" + variable.sourceName() + "\n");
      sb.append("long_name=" + variable.longName() + "\n");
      sb.append("type=" + variable.destinationDataType() + "\n");
    }
    for (EDVGridAxis axisVariable : axisVariables)
      sb.append(axisVariable.combinedAttributes().toString() + "\n");

    String2.replaceAll(sb, "\"", ""); // no double quotes (esp around attribute values)
    String2.replaceAll(sb, "\n    ", "\n"); // occurs for all attributes
    return sb.toString();
  }

  /** A string like [time][latitude][longitude] with the axis destination names. */
  public String allDimString() {
    if (allDimString == null) {
      StringBuilder sb = new StringBuilder();
      for (EDVGridAxis axisVariable : axisVariables)
        sb.append("[" + axisVariable.destinationName() + "]");
      allDimString = sb.toString(); // last thing: atomic assignment
    }
    return allDimString;
  }

  /** This indicates why the dataset isn't accessible via Make A Graph (or "" if it is). */
  @Override
  public String accessibleViaMAG() {
    if (accessibleViaMAG == null) {
      // find the axisVariables (all are always numeric) with >1 value
      boolean hasAG1V = false;
      for (EDVGridAxis axisVariable : axisVariables) {
        if (axisVariable.sourceValues().size() > 1) {
          hasAG1V = true;
          break;
        }
      }
      if (!hasAG1V) {
        accessibleViaMAG =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecause2Ar[0],
                    EDStatic.messages.magAr[0],
                    EDStatic.messages.noXxxNoAxis1Ar[0]));
      } else {

        // find the numeric dataVariables
        boolean hasNumeric = false;
        for (EDV dataVariable : dataVariables) {
          if (dataVariable.destinationDataPAType() != PAType.STRING) {
            hasNumeric = true;
            break;
          }
        }
        if (hasNumeric) accessibleViaMAG = String2.canonical("");
        else
          accessibleViaMAG =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecause2Ar[0],
                      EDStatic.messages.magAr[0],
                      EDStatic.messages.noXxxNoNonStringAr[0]));
      }
    }
    return accessibleViaMAG;
  }

  /** This indicates why the dataset isn't accessible via .subset (or "" if it is). */
  @Override
  public String accessibleViaSubset() {
    if (accessibleViaSubset == null)
      accessibleViaSubset =
          String2.canonical(
              MessageFormat.format(
                  EDStatic.messages.noXxxBecause2Ar[0],
                  EDStatic.messages.subsetAr[0],
                  EDStatic.messages.noXxxItsGriddedAr[0]));
    return accessibleViaSubset;
  }

  /** This indicates why the dataset isn't accessible via SOS (or "" if it is). */
  @Override
  public String accessibleViaSOS() {
    if (accessibleViaSOS == null) {

      if (!EDStatic.config.sosActive)
        accessibleViaSOS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0],
                    "SOS",
                    MessageFormat.format(EDStatic.messages.noXxxNotActiveAr[0], "SOS")));
      else
        accessibleViaSOS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0],
                    "SOS",
                    EDStatic.messages.noXxxItsGriddedAr[0]));
    }
    return accessibleViaSOS;
  }

  /** This indicates why the dataset isn't accessible via ESRI GeoServices REST (or "" if it is). */
  @Override
  public String accessibleViaGeoServicesRest() {
    if (accessibleViaGeoServicesRest == null) {

      if (!EDStatic.config.geoServicesRestActive) {
        accessibleViaGeoServicesRest =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0],
                    "GeoServicesRest",
                    MessageFormat.format(
                        EDStatic.messages.noXxxNotActiveAr[0], "GeoServicesRest")));
      } else if (lonIndex < 0 || latIndex < 0) {
        // must have lat and lon axes
        accessibleViaGeoServicesRest =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0],
                    "GeoServicesRest",
                    EDStatic.messages.noXxxNoLLAr[0]));
      } else {
        // must have more than one value for lat and lon axes
        EDVGridAxis lonVar = axisVariables[lonIndex];
        EDVGridAxis latVar = axisVariables[latIndex];
        if (lonVar.destinationMinDouble() == lonVar.destinationMaxDouble()
            || // only 1 value
            latVar.destinationMinDouble() == latVar.destinationMaxDouble())
          accessibleViaGeoServicesRest =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "GeoServicesRest",
                      EDStatic.messages.noXxxNoLLGt1Ar[0]));
        else if (lonVar.destinationMinDouble() >= 360
            || // unlikely
            lonVar.destinationMaxDouble() <= -180) // unlikely
        accessibleViaGeoServicesRest =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "GeoServicesRest",
                      EDStatic.messages.noXxxNoLonIn180Ar[0]));
        else if (!lonVar.isEvenlySpaced()
            || // ???Future: not necessary? draw map as appropriate.
            !latVar.isEvenlySpaced())
          accessibleViaGeoServicesRest =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "GeoServicesRest",
                      EDStatic.messages.noXxxNoLLEvenlySpacedAr[0]));

        // else {  //NO. other axes are allowed.

      }

      // ensure at least one var has colorBarMinimum/Maximum
      if (accessibleViaGeoServicesRest == null) {
        boolean ok = false;
        for (EDV dataVariable : dataVariables) {
          if (dataVariable.hasColorBarMinMax()) {
            ok = true;
            break;
          }
        }
        if (!ok)
          accessibleViaGeoServicesRest =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "GeoServicesRest",
                      EDStatic.messages.noXxxNoColorBarAr[0]));
      }

      // okay!
      if (accessibleViaGeoServicesRest == null)
        accessibleViaGeoServicesRest = String2.canonical("");
    }
    return accessibleViaGeoServicesRest;
  }

  /**
   * This indicates why the dataset isn't accessible via WCS (or "" if it is). There used to be a
   * lon +/-180 restriction, but no more.
   */
  @Override
  public String accessibleViaWCS() {
    if (accessibleViaWCS == null) {

      if (!EDStatic.config.wcsActive)
        accessibleViaWCS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0],
                    "WCS",
                    MessageFormat.format(EDStatic.messages.noXxxNotActiveAr[0], "WCS")));
      else if (lonIndex < 0 || latIndex < 0)
        // must have lat and lon axes
        accessibleViaWCS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0], "WCS", EDStatic.messages.noXxxNoLLAr[0]));
      else {
        // must have more than one value for lat and lon axes
        EDVGridAxis lonVar = axisVariables[lonIndex];
        EDVGridAxis latVar = axisVariables[latIndex];
        if (lonVar.destinationMinDouble() == lonVar.destinationMaxDouble()
            || // only 1 value
            latVar.destinationMinDouble() == latVar.destinationMaxDouble())
          accessibleViaWCS =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "WCS",
                      EDStatic.messages.noXxxNoLLGt1Ar[0]));
        else if (lonVar.destinationMinDouble() >= 360
            || // unlikely
            lonVar.destinationMaxDouble() <= -180) // unlikely
        accessibleViaWCS =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "WCS",
                      EDStatic.messages.noXxxNoLonIn180Ar[0]));
        else if (!lonVar.isEvenlySpaced()
            || // ???Future: not necessary? draw map as appropriate.
            !latVar.isEvenlySpaced())
          accessibleViaWCS =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "WCS",
                      EDStatic.messages.noXxxNoLLEvenlySpacedAr[0]));

        // else {  //NO. other axes are allowed.

        else accessibleViaWCS = String2.canonical("");
      }
    }
    return accessibleViaWCS;
  }

  /**
   * This indicates why the dataset is accessible via WMS (or "" if it is). There used to be a lon
   * +/-180 restriction, but no more.
   */
  @Override
  public String accessibleViaWMS() {
    if (accessibleViaWMS == null) {

      if (!EDStatic.config.wmsActive)
        accessibleViaWMS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0],
                    "WMS",
                    MessageFormat.format(EDStatic.messages.noXxxNotActiveAr[0], "WMS")));
      else if (lonIndex < 0 || latIndex < 0)
        accessibleViaWMS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.noXxxBecauseAr[0], "WMS", EDStatic.messages.noXxxNoLLAr[0]));
      else {
        EDVGridAxis lonVar = axisVariables[lonIndex];
        EDVGridAxis latVar = axisVariables[latIndex];
        if (lonVar.destinationMinDouble() == lonVar.destinationMaxDouble()
            || // only 1 value
            latVar.destinationMinDouble() == latVar.destinationMaxDouble())
          accessibleViaWMS =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "WMS",
                      EDStatic.messages.noXxxNoLLGt1Ar[0]));
        else if (lonVar.destinationMinDouble() >= 360
            || // unlikely
            lonVar.destinationMaxDouble() <= -180) // unlikely
        accessibleViaWMS =
              String2.canonical(
                  MessageFormat.format(
                      EDStatic.messages.noXxxBecauseAr[0],
                      "WMS",
                      EDStatic.messages.noXxxNoLonIn180Ar[0]));
        // else if (!lonVar.isEvenlySpaced() ||  //not necessary. map is drawn as appropriate.
        //    !latVar.isEvenlySpaced())
        //   accessibleViaWMS = String2.canonical(start + "???";

        else {
          String ta =
              MessageFormat.format(
                  EDStatic.messages.noXxxBecauseAr[0],
                  "WMS",
                  EDStatic.messages.noXxxNoColorBarAr[0]);
          for (EDV dataVariable : dataVariables) {
            if (dataVariable.hasColorBarMinMax()) {
              ta = ""; // set back to OK
              break;
            }
          }
          accessibleViaWMS = String2.canonical(ta);
        }
      }
    }
    return accessibleViaWMS;
  }

  /** This indicates why the dataset isn't accessible via .ncCF file type (or "" if it is). */
  @Override
  public String accessibleViaNcCF() {

    if (accessibleViaNcCF == null) {
      return accessibleViaNcCF =
          "Currently, the .ncCF and .ncCFMA file types are for tabular data only. "
              + "For this dataset, use the .nc file type.";
      // String cdmType = combinedAttributes.getString("cdm_data_type");
      // if (cdmType.equals(CDM_GRID))
      //    accessibleViaNcCDM = "";
      // else accessibleViaNcCDM = "Currently, only cdm_data_type=" + CDM_GRID +
      //    " is supported for .ncDCM.  This dataset cdm_data_type=" + cdmType + ".";
    }
    return accessibleViaNcCF;
  }

  /**
   * This returns the dapProtocol
   *
   * @return the dapProtocol
   */
  @Override
  public String dapProtocol() {
    return dapProtocol;
  }

  /**
   * This returns the dapDescription
   *
   * @param language the index of the selected language
   * @return the dapDescription
   */
  @Override
  public String dapDescription(int language) {
    return EDStatic.messages.EDDGridDapDescriptionAr[language];
  }

  /**
   * This returns the longDapDescription
   *
   * @param language the index of the selected language
   * @return the longDapDescription
   */
  public static String longDapDescription(int language, String tErddapUrl) {
    return String2.replaceAll(
        EDStatic.messages.EDDGridDapLongDescriptionAr[language], "&erddapUrl;", tErddapUrl);
  }

  /**
   * This should be used by all subclass constructors to ensure that all of the items common to all
   * EDDGrids are properly set. This also does some actual work.
   *
   * @throws Throwable if any required item isn't properly set
   */
  @Override
  public void ensureValid() throws Throwable {
    super.ensureValid();
    String errorInMethod =
        "datasets.xml/EDDGrid.ensureValid error for datasetID=" + datasetID + ":\n ";

    new StringArray(axisVariableSourceNames())
        .ensureNoDuplicates("Duplicate axisVariableSourceNames: ");
    new StringArray(axisVariableDestinationNames())
        .ensureNoDuplicates("Duplicate axisVariableDestinationNames: ");

    HashSet<String> sourceNamesHS =
        new HashSet<>(2 * (axisVariables.length + dataVariables.length));
    HashSet<String> destNamesHS = new HashSet<>(2 * (axisVariables.length + dataVariables.length));
    for (int v = 0; v < axisVariables.length; v++) {
      Test.ensureTrue(axisVariables[v] != null, errorInMethod + "axisVariable[" + v + "] is null.");
      String tErrorInMethod =
          errorInMethod
              + "for axisVariable #"
              + v
              + "="
              + axisVariables[v].destinationName()
              + ":\n";
      Test.ensureTrue(
          axisVariables[v] != null,
          tErrorInMethod + "axisVariable[" + v + "] isn't an EDVGridAxis.");
      axisVariables[v].ensureValid(tErrorInMethod);

      // ensure unique sourceNames
      String sn = axisVariables[v].sourceName();
      if (!sn.startsWith("=")) {
        if (!sourceNamesHS.add(sn))
          throw new RuntimeException(
              errorInMethod + "Two axisVariables have the same sourceName=" + sn + ".");
      }

      // ensure unique destNames
      String dn = axisVariables[v].destinationName();
      if (!destNamesHS.add(dn))
        throw new RuntimeException(
            errorInMethod + "Two axisVariables have the same destinationName=" + dn + ".");

      // standard_name is the only case-sensitive CF attribute name (see Sec 3.3)
      // All are all lower case.
      Attributes tAtts = axisVariables[v].combinedAttributes();
      String tStandardName = tAtts.getString("standard_name");
      if (String2.isSomething(tStandardName))
        tAtts.set("standard_name", tStandardName.toLowerCase());
    }

    for (EDV dataVariable : dataVariables) {
      // ensure unique sourceNames
      String sn = dataVariable.sourceName();
      if (!sn.startsWith("=")) {
        if (!sourceNamesHS.add(sn))
          throw new RuntimeException(
              errorInMethod + "Two variables have the same sourceName=" + sn + ".");
      }

      // ensure unique destNames
      String dn = dataVariable.destinationName();
      if (!destNamesHS.add(dn))
        throw new RuntimeException(
            errorInMethod + "Two variables have the same destinationName=" + dn + ".");

      // standard_name is the only case-sensitive CF attribute name (see Sec 3.3)
      // All are all lower case.
      Attributes tAtts = dataVariable.combinedAttributes();
      String tStandardName = tAtts.getString("standard_name");
      if (String2.isSomething(tStandardName))
        tAtts.set("standard_name", tStandardName.toLowerCase());
    }

    Test.ensureTrue(
        lonIndex < 0 || axisVariables[lonIndex] instanceof EDVLonGridAxis,
        errorInMethod + "axisVariable[lonIndex=" + lonIndex + "] isn't an EDVLonGridAxis.");
    Test.ensureTrue(
        latIndex < 0 || axisVariables[latIndex] instanceof EDVLatGridAxis,
        errorInMethod + "axisVariable[latIndex=" + latIndex + "] isn't an EDVLatGridAxis.");
    Test.ensureTrue(
        altIndex < 0 || axisVariables[altIndex] instanceof EDVAltGridAxis,
        errorInMethod + "axisVariable[altIndex=" + altIndex + "] isn't an EDVAltGridAxis.");
    Test.ensureTrue(
        depthIndex < 0 || axisVariables[depthIndex] instanceof EDVDepthGridAxis,
        errorInMethod + "axisVariable[depthIndex=" + depthIndex + "] isn't an EDVDepthGridAxis.");
    // some places (e.g., wms) depend on not having alt *and* depth
    Test.ensureTrue(
        altIndex <= 0 || depthIndex <= 0,
        errorInMethod + "The dataset has both an altitude and a depth axis.");
    Test.ensureTrue(
        timeIndex < 0 || axisVariables[timeIndex] instanceof EDVTimeGridAxis,
        errorInMethod + "axisVariable[timeIndex=" + timeIndex + "] isn't an EDVTimeGridAxis.");

    // add standard metadata to combinedGlobalAttributes
    // (This should always be done, so shouldn't be in an optional method...)
    String avDestNames[] = axisVariableDestinationNames();

    // lon
    combinedGlobalAttributes.remove("geospatial_lon_min");
    combinedGlobalAttributes.remove("geospatial_lon_max");
    combinedGlobalAttributes.remove("geospatial_lon_resolution");
    combinedGlobalAttributes.remove("geospatial_lon_units");
    combinedGlobalAttributes.remove("Westernmost_Easting");
    combinedGlobalAttributes.remove("Easternmost_Easting");
    int av = String2.indexOf(avDestNames, EDV.LON_NAME);
    if (av >= 0) {
      combinedGlobalAttributes.add("geospatial_lon_units", EDV.LON_UNITS);
      EDVGridAxis edvga = axisVariables[av];
      if (edvga.sourceValues().size() > 1 && edvga.isEvenlySpaced())
        combinedGlobalAttributes.add("geospatial_lon_resolution", Math.abs(edvga.averageSpacing()));

      PrimitiveArray pa = edvga.combinedAttributes().get("actual_range");
      if (pa != null) { // it should be; but it can be low,high or high,low, so
        double ttMin = Math.min(pa.getNiceDouble(0), pa.getNiceDouble(1));
        double ttMax = Math.max(pa.getNiceDouble(0), pa.getNiceDouble(1));
        combinedGlobalAttributes.add("geospatial_lon_min", ttMin);
        combinedGlobalAttributes.add("geospatial_lon_max", ttMax);
        combinedGlobalAttributes.add("Westernmost_Easting", ttMin);
        combinedGlobalAttributes.add("Easternmost_Easting", ttMax);
      }
    }

    // lat
    combinedGlobalAttributes.remove("geospatial_lat_min");
    combinedGlobalAttributes.remove("geospatial_lat_max");
    combinedGlobalAttributes.remove("geospatial_lat_resolution");
    combinedGlobalAttributes.remove("geospatial_lat_units");
    combinedGlobalAttributes.remove("Southernmost_Northing");
    combinedGlobalAttributes.remove("Northernmost_Northing");
    av = String2.indexOf(avDestNames, EDV.LAT_NAME);
    if (av >= 0) {
      combinedGlobalAttributes.add("geospatial_lat_units", EDV.LAT_UNITS);
      EDVGridAxis edvga = axisVariables[av];
      if (edvga.sourceValues().size() > 1 && edvga.isEvenlySpaced())
        combinedGlobalAttributes.add("geospatial_lat_resolution", Math.abs(edvga.averageSpacing()));

      PrimitiveArray pa = edvga.combinedAttributes().get("actual_range");
      if (pa != null) { // it should be; but it can be low,high or high,low, so
        double ttMin = Math.min(pa.getNiceDouble(0), pa.getNiceDouble(1));
        double ttMax = Math.max(pa.getNiceDouble(0), pa.getNiceDouble(1));
        combinedGlobalAttributes.add("geospatial_lat_min", ttMin);
        combinedGlobalAttributes.add("geospatial_lat_max", ttMax);
        combinedGlobalAttributes.add("Southernmost_Northing", ttMin);
        combinedGlobalAttributes.add("Northernmost_Northing", ttMax);
      }
    }

    // alt
    combinedGlobalAttributes.remove("geospatial_vertical_min");
    combinedGlobalAttributes.remove("geospatial_vertical_max");
    combinedGlobalAttributes.remove("geospatial_vertical_positive");
    combinedGlobalAttributes.remove("geospatial_vertical_resolution");
    combinedGlobalAttributes.remove("geospatial_vertical_units");
    av = String2.indexOf(avDestNames, EDV.ALT_NAME);
    if (av >= 0) {
      combinedGlobalAttributes.add("geospatial_vertical_positive", "up");
      combinedGlobalAttributes.add("geospatial_vertical_units", EDV.ALT_UNITS);
      EDVGridAxis edvga = axisVariables[av];
      if (edvga.sourceValues().size() > 1 && edvga.isEvenlySpaced())
        combinedGlobalAttributes.add(
            "geospatial_vertical_resolution", Math.abs(edvga.averageSpacing()));

      PrimitiveArray pa = edvga.combinedAttributes().get("actual_range");
      if (pa != null) { // it should be; but it can be low,high or high,low, so
        double ttMin = Math.min(pa.getNiceDouble(0), pa.getNiceDouble(1));
        double ttMax = Math.max(pa.getNiceDouble(0), pa.getNiceDouble(1));
        combinedGlobalAttributes.add("geospatial_vertical_min", ttMin);
        combinedGlobalAttributes.add("geospatial_vertical_max", ttMax);
      }
    }

    // time
    combinedGlobalAttributes.remove("time_coverage_start");
    combinedGlobalAttributes.remove("time_coverage_end");
    av = String2.indexOf(avDestNames, EDV.TIME_NAME);
    if (av >= 0) {
      PrimitiveArray pa = axisVariables[av].combinedAttributes().get("actual_range");
      if (pa != null) { // it should be; but it can be low,high or high,low, so
        double ttMin = Math.min(pa.getDouble(0), pa.getDouble(1));
        double ttMax = Math.max(pa.getDouble(0), pa.getDouble(1));
        String tp = axisVariables[av].combinedAttributes().getString(EDV.TIME_PRECISION);
        // "" unsets the attribute if dMin or dMax isNaN
        combinedGlobalAttributes.set(
            "time_coverage_start", Calendar2.epochSecondsToLimitedIsoStringT(tp, ttMin, ""));
        // for tables (not grids) will be NaN for 'present'.   Deal with this better???
        combinedGlobalAttributes.set(
            "time_coverage_end", Calendar2.epochSecondsToLimitedIsoStringT(tp, ttMax, ""));
      }

      // print time gaps greater than median
      /*if (verbose) {
          PrimitiveArray pa = axisVariables[av].destinationValues();
          DoubleArray da = pa instanceof DoubleArray?
              (DoubleArray)pa : new DoubleArray(pa);
              String2.log().findTimeGaps();
          String2.log(da.timeGaps);
      } */
    }

    // last: uses time_coverage metadata
    // make FGDC and ISO19115
    accessibleViaFGDC = null; // should be null already, but make sure so files will be created now
    accessibleViaISO19115 = null;
    accessibleViaFGDC();
    accessibleViaISO19115();

    // really last: it uses accessibleViaFGDC and accessibleViaISO19115
    // make searchString  (since should have all finished/correct metadata)
    // This makes creation of searchString thread-safe (always done in constructor's thread).
    searchString();
  }

  /**
   * The string representation of this gridDataSet (for diagnostic purposes).
   *
   * @return the string representation of this gridDataSet.
   */
  @Override
  public String toString() {
    // make this JSON format?
    StringBuilder sb = new StringBuilder();
    sb.append("//** EDDGrid " + super.toString());
    for (EDVGridAxis axisVariable : axisVariables) sb.append(axisVariable.toString());
    sb.append("\\**\n\n");
    return sb.toString();
  }

  /**
   * This returns the axis or data variable which has the specified destination name.
   *
   * @return the specified axis or data variable destinationName
   * @throws Throwable if not found
   */
  @Override
  public EDV findVariableByDestinationName(String tDestinationName) throws Throwable {
    for (EDVGridAxis axisVariable : axisVariables)
      if (axisVariable.destinationName().equals(tDestinationName)) return axisVariable;
    return findDataVariableByDestinationName(tDestinationName);
  }

  /**
   * This returns the index of the lon axisVariable (or -1 if none).
   *
   * @return the index of the lon axisVariable (or -1 if none).
   */
  public int lonIndex() {
    return lonIndex;
  }

  /**
   * This returns the index of the lat axisVariable (or -1 if none).
   *
   * @return the index of the lat axisVariable (or -1 if none).
   */
  public int latIndex() {
    return latIndex;
  }

  /**
   * This returns the index of the altitude axisVariable (or -1 if none).
   *
   * @return the index of the altitude axisVariable (or -1 if none).
   */
  public int altIndex() {
    return altIndex;
  }

  /**
   * This returns the index of the depth axisVariable (or -1 if none).
   *
   * @return the index of the depth axisVariable (or -1 if none).
   */
  public int depthIndex() {
    return depthIndex;
  }

  /**
   * This returns the index of the time axisVariable (or -1 if none).
   *
   * @return the index of the time axisVariable (or -1 if none).
   */
  public int timeIndex() {
    return timeIndex;
  }

  /**
   * This returns the axisVariables. This is the internal data structure, so don't change it.
   *
   * @return the axisVariables.
   */
  public EDVGridAxis[] axisVariables() {
    return axisVariables;
  }

  /**
   * This returns the axis variable which has the specified source name.
   *
   * @param language the index of the selected language
   * @return the specified axis variable sourceName
   * @throws SimpleException if not found
   */
  public EDVGridAxis findAxisVariableBySourceName(int language, String tSourceName) {

    int which = String2.indexOf(axisVariableSourceNames(), tSourceName);
    if (which < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              MessageFormat.format(
                  EDStatic.messages.errorNotFoundInAr[0],
                  "sourceAxisVariableName=" + tSourceName,
                  "datasetID=" + datasetID),
              MessageFormat.format(
                  EDStatic.messages.errorNotFoundInAr[language],
                  "sourceAxisVariableName=" + tSourceName,
                  "datasetID=" + datasetID)));
    return axisVariables[which];
  }

  /**
   * This returns the axis variable which has the specified destination name.
   *
   * @param language the index of the selected language
   * @return the specified axis variable destinationName
   * @throws SimpleException if not found
   */
  public EDVGridAxis findAxisVariableByDestinationName(int language, String tDestinationName) {

    int which = String2.indexOf(axisVariableDestinationNames(), tDestinationName);
    if (which < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              MessageFormat.format(
                  EDStatic.messages.errorNotFoundInAr[0],
                  "variableName=" + tDestinationName,
                  "datasetID=" + datasetID),
              MessageFormat.format(
                  EDStatic.messages.errorNotFoundInAr[language],
                  "variableName=" + tDestinationName,
                  "datasetID=" + datasetID)));
    return axisVariables[which];
  }

  /**
   * This returns a list of the axisVariables' source names.
   *
   * @return a list of the axisVariables' source names. This always returns the same internal array,
   *     so don't change it!
   */
  public String[] axisVariableSourceNames() {
    if (axisVariableSourceNames == null) {
      // do it this way to be a little more thread safe
      String tNames[] = new String[axisVariables.length];
      for (int i = 0; i < axisVariables.length; i++) tNames[i] = axisVariables[i].sourceName();
      axisVariableSourceNames = tNames;
    }
    return axisVariableSourceNames;
  }

  /**
   * This returns a list of the axisVariables' destination names.
   *
   * @return a list of the axisVariables' destination names. This always returns the same internal
   *     array, so don't change it!
   */
  public String[] axisVariableDestinationNames() {
    if (axisVariableDestinationNames == null) {
      // do it this way to be a little more thread safe
      String tNames[] = new String[axisVariables.length];
      for (int i = 0; i < axisVariables.length; i++) tNames[i] = axisVariables[i].destinationName();
      axisVariableDestinationNames = tNames;
    }
    return axisVariableDestinationNames;
  }

  /**
   * This indicates if userDapQuery is a request for one or more axis variables (vs. a request for
   * one or more data variables).
   *
   * @param userDapQuery the part after the '?', still percentEncoded (may be null).
   */
  public boolean isAxisDapQuery(String userDapQuery) throws Throwable {
    if (userDapQuery == null) return false;

    // remove any &constraints;
    String ampParts[] =
        Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")
    userDapQuery = ampParts[0];
    int qLength = userDapQuery.length();
    if (qLength == 0) return false;

    // basically, see if first thing is an axis destination name
    int tPo = userDapQuery.indexOf('[');
    int po = tPo >= 0 ? tPo : qLength;
    tPo = userDapQuery.indexOf(',');
    if (tPo >= 0) po = Math.min(tPo, po);

    // or request uses gridName.axisName notation?
    String tName = userDapQuery.substring(0, po);
    int period = tName.indexOf('.');
    if (period > 0
        && String2.indexOf(dataVariableDestinationNames(), tName.substring(0, period)) >= 0)
      tName = tName.substring(period + 1);

    // is tName an axisName?
    int tAxis = String2.indexOf(axisVariableDestinationNames(), tName);
    return tAxis >= 0;
  }

  /**
   * This parses an OPeNDAP DAP-style grid-style query for grid data (not axis) variables, e.g.,
   * var1,var2 or var1[start],var2[start] or var1[start:stop],var2[start:stop] or
   * var1[start:stride:stop][start:stride:stop][].
   *
   * <ul>
   *   <li>An ERDDAP extension of the OPeNDAP standard: If within parentheses, start and/or stop are
   *       assumed to be specified in destination units (not indices).
   *   <li>If only two values are specified for a dimension (e.g., [a:b]), it is interpreted as
   *       [a:1:b].
   *   <li>If only one value is specified for a dimension (e.g., [a]), it is interpreted as [a:1:a].
   *   <li>If 0 values are specified for a dimension (e.g., []), it is interpreted as [0:1:max].
   *   <li>Currently, if more than one variable is requested, all variables must have the same []
   *       constraints.
   *   <li>If userDapQuery is "", it is treated as a request for the entire dataset.
   *   <li>The query may also have &amp; clauses at the end. Currently, they must all start with "."
   *       (for graphics commands).
   * </ul>
   *
   * @param language the index of the selected language
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null).
   * @param destinationNames will receive the list of requested destination variable names
   * @param constraints will receive the list of constraints, stored in axisVariables.length groups
   *     of 3 int's: start0, stride0, stop0, start1, stride1, stop1, ...
   * @param repair if true, this method tries to do its best repair problems (guess at intent), not
   *     to throw exceptions
   * @throws Throwable if invalid query (0 resultsVariables is a valid query)
   */
  public void parseDataDapQuery(
      int language,
      String userDapQuery,
      StringArray destinationNames,
      IntArray constraints,
      boolean repair)
      throws Throwable {
    parseDataDapQuery(
        language, userDapQuery, destinationNames, constraints, repair, new DoubleArray());
  }

  /**
   * This parses an OPeNDAP DAP-style grid-style query for grid data (not axis) variables, e.g.,
   * var1,var2 or var1[start],var2[start] or var1[start:stop],var2[start:stop] or
   * var1[start:stride:stop][start:stride:stop][].
   *
   * <ul>
   *   <li>An ERDDAP extension of the OPeNDAP standard: If within parentheses, start and/or stop are
   *       assumed to be specified in destination units (not indices).
   *   <li>If only two values are specified for a dimension (e.g., [a:b]), it is interpreted as
   *       [a:1:b].
   *   <li>If only one value is specified for a dimension (e.g., [a]), it is interpreted as [a:1:a].
   *   <li>If 0 values are specified for a dimension (e.g., []), it is interpreted as [0:1:max].
   *   <li>Currently, if more than one variable is requested, all variables must have the same []
   *       constraints.
   *   <li>If userDapQuery is "", it is treated as a request for the entire dataset.
   *   <li>The query may also have &amp; clauses at the end. Currently, they must all start with "."
   *       (for graphics commands).
   * </ul>
   *
   * @param language the index of the selected language
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null).
   * @param destinationNames will receive the list of requested destination variable names
   * @param constraints will receive the list of constraints, stored in axisVariables.length groups
   *     of 3 int's: start0, stride0, stop0, start1, stride1, stop1, ...
   * @param repair if true, this method tries to do its best repair problems (guess at intent), not
   *     to throw exceptions
   * @param inputValues the double values parsed from the query are stored here. The initial size
   *     must be 0. These are returned in the order the user provided them, there is no correction
   *     to make sure the lower value is first. Stride is not included, just the start and stop
   *     values. If a single value is provided, it is duplicated in the inputValues. Index inputs
   *     are converted to the value for that index on the axis and the value is added to the input
   *     array (not the index). If the query contains 'last', then the input values will contain the
   *     lastDestinationValue for that axis.
   * @throws Throwable if invalid query (0 resultsVariables is a valid query)
   */
  public void parseDataDapQuery(
      int language,
      String userDapQuery,
      StringArray destinationNames,
      IntArray constraints,
      boolean repair,
      DoubleArray inputValues)
      throws Throwable {

    destinationNames.clear();
    constraints.clear();
    if (reallyVerbose) String2.log("    EDDGrid.parseDataDapQuery: " + userDapQuery);

    // split userDapQuery at '&' and decode
    String ampParts[] =
        Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")

    // ignore any &.cmd constraints
    for (int ap = 1; ap < ampParts.length; ap++) {
      if (!repair && !ampParts[ap].startsWith("."))
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0] + EDStatic.messages.queryErrorGridAmpAr[0],
                EDStatic.messages.queryErrorAr[language]
                    + EDStatic.messages.queryErrorGridAmpAr[language]));
    }
    String query = ampParts[0]; // it has been percentDecoded

    // expand query="" into request for everything
    if (query.length() == 0) {
      if (reallyVerbose) String2.log("      query=\"\" is expanded to request entire dataset.");
      query = String2.toSVString(dataVariableDestinationNames(), ",", false);
    }

    // process queries with no [], just csv list of desired dataVariables
    if (query.indexOf('[') < 0) {
      for (EDVGridAxis axisVariable : axisVariables) {
        constraints.add(0);
        constraints.add(1);
        constraints.add(axisVariable.sourceValues().size() - 1);
      }
      String destNames[] = String2.split(query, ',');
      for (String name : destNames) {
        // if gridName.gridName notation, remove "gridName."
        // This isn't exactly correct: technically, the response shouldn't include the axis
        // variables.
        String destName = name;
        int period = destName.indexOf('.');
        if (period > 0) {
          String shortName = destName.substring(0, period);
          if (destName.equals(shortName + "." + shortName)
              && String2.indexOf(dataVariableDestinationNames(), shortName) >= 0)
            destName = shortName;
        }

        // ensure destName is valid
        int tdi = String2.indexOf(dataVariableDestinationNames(), destName);
        if (tdi < 0) {
          if (repair) destName = dataVariableDestinationNames()[0];
          else {
            if (String2.indexOf(axisVariableDestinationNames(), destName) >= 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.messages.queryErrorAr[0]
                          + MessageFormat.format(
                              EDStatic.messages.queryErrorGridNoAxisVarAr[0], destName),
                      EDStatic.messages.queryErrorAr[language]
                          + MessageFormat.format(
                              EDStatic.messages.queryErrorGridNoAxisVarAr[language], destName)));
            findDataVariableByDestinationName(destName); // throws Throwable if trouble
          }
        }

        // ensure not duplicate destName
        tdi = destinationNames.indexOf(destName);
        if (tdi >= 0) {
          if (!repair)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "Variable name='"
                    + destName
                    + "' occurs twice.");
        } else {
          destinationNames.add(destName);
        }
      }
      return;
    }

    // get the destinationNames
    int po = 0;
    while (po < query.length()) {
      // after first destinationName+constraints, "," should be next char
      if (po > 0) {
        if (query.charAt(po) != ',') {
          if (repair) return; // this can only be trouble for second variable
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorExpectedAtAr[0],
                            ",\" or \"[end of query]",
                            "" + po,
                            "\"" + query.charAt(po) + "\""),
                    EDStatic.messages.queryErrorAr[language]
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorExpectedAtAr[language],
                            ",\" or \"[end of query]",
                            "" + po,
                            "\"" + query.charAt(po) + "\"")));
        }
        po++;
      }

      // get the destinationName
      // find the '['               ??? Must I require "[" ???
      int leftPo = query.indexOf('[', po);
      if (leftPo < 0) {
        if (repair) return; // this can only be trouble for second variable
        else
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorExpectedAtAr[0],
                          "[",
                          "" + po,
                          "[end of query]"),
                  EDStatic.messages.queryErrorAr[language]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorExpectedAtAr[language],
                          "[",
                          "" + po,
                          "[end of query]")));
      }
      String destinationName = query.substring(po, leftPo);

      // if gridName.gridName notation, remove "gridName."
      // This isn't exactly correct: technically, the response shouldn't include the axis variables.
      int period = destinationName.indexOf('.');
      if (period > 0) {
        String shortName = destinationName.substring(0, period);
        if (destinationName.equals(shortName + "." + shortName)
            && String2.indexOf(dataVariableDestinationNames(), shortName) >= 0)
          destinationName = shortName;
      }

      // ensure destinationName is valid
      if (reallyVerbose) String2.log("      destinationName=" + destinationName);
      int tdi = String2.indexOf(dataVariableDestinationNames(), destinationName);
      if (tdi < 0) {
        if (repair) destinationName = dataVariableDestinationNames()[0];
        else findDataVariableByDestinationName(destinationName); // throws Throwable if trouble
      }

      // ensure not duplicate destName
      tdi = destinationNames.indexOf(destinationName);
      if (tdi >= 0) {
        if (!repair)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorOccursTwiceAr[0], destinationName),
                  EDStatic.messages.queryErrorAr[language]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorOccursTwiceAr[language], destinationName)));
      } else {
        destinationNames.add(destinationName);
      }
      po = leftPo;

      // get the axis constraints
      for (int axis = 0; axis < axisVariables.length; axis++) {
        int sssp[] =
            parseAxisBrackets(language, query, destinationName, po, axis, repair, inputValues);
        int startI = sssp[0];
        int strideI = sssp[1];
        int stopI = sssp[2];
        po = sssp[3];

        if (destinationNames.size() == 1) {
          // store convert sourceStart and sourceStop to indices
          constraints.add(startI);
          constraints.add(strideI);
          constraints.add(stopI);
          // if (reallyVerbose) String2.log("      axis=" + axis +
          //    " constraints: " + startI + " : " + strideI + " : " + stopI);
        } else {
          // ensure start,stride,stop match first variable
          if (startI != constraints.get(axis * 3 + 0)
              || strideI != constraints.get(axis * 3 + 1)
              || stopI != constraints.get(axis * 3 + 2)) {
            if (!repair)
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.messages.queryErrorAr[0]
                          + MessageFormat.format(
                              EDStatic.messages.queryErrorGridNotIdenticalAr[0],
                              axisVariableDestinationNames()[axis]
                                  + "["
                                  + startI
                                  + ":"
                                  + strideI
                                  + ":"
                                  + stopI
                                  + "]",
                              destinationName,
                              axisVariableDestinationNames()[axis]
                                  + "["
                                  + constraints.get(axis * 3 + 0)
                                  + ":"
                                  + constraints.get(axis * 3 + 1)
                                  + ":"
                                  + constraints.get(axis * 3 + 2)
                                  + "]"
                                  + destinationNames.get(0)),
                      EDStatic.messages.queryErrorAr[language]
                          + MessageFormat.format(
                              EDStatic.messages.queryErrorGridNotIdenticalAr[language],
                              axisVariableDestinationNames()[axis]
                                  + "["
                                  + startI
                                  + ":"
                                  + strideI
                                  + ":"
                                  + stopI
                                  + "]",
                              destinationName,
                              axisVariableDestinationNames()[axis]
                                  + "["
                                  + constraints.get(axis * 3 + 0)
                                  + ":"
                                  + constraints.get(axis * 3 + 1)
                                  + ":"
                                  + constraints.get(axis * 3 + 2)
                                  + "]"
                                  + destinationNames.get(0))));
          }
        }
      }
    }
  }

  /**
   * This parses an OPeNDAP DAP-style grid-style query for one or more axis (not grid) variables,
   * (e.g., var1,var2, perhaps with [start:stride:stop] values). !!!This should only be called if
   * the userDapQuery starts with the name of an axis variable.
   *
   * <ul>
   *   <li>An ERDDAP extension of the OPeNDAP standard: If within parentheses, start and/or stop are
   *       assumed to be specified in destination units (not indices).
   *   <li>If only two values are specified for a dimension (e.g., [a:b]), it is interpreted as
   *       [a:1:b].
   *   <li>If only one value is specified for a dimension (e.g., [a]), it is interpreted as [a:1:a].
   *   <li>If 0 values are specified for a dimension (e.g., []), it is interpreted as [0:1:max].
   *   <li>Currently, if more than one variable is requested, all variables must have the same []
   *       constraints.
   *   <li>If userDapQuery is varName, it is treated as a request for the entire variable.
   * </ul>
   *
   * @param language the index of the selected language
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null).
   * @param destinationNames will receive the list of requested destination axisVariable names
   * @param constraints will receive the list of constraints, stored as 3 int's (for for each
   *     destinationName): start, stride, stop.
   * @param repair if true, this method tries to do its best repair problems (guess at intent), not
   *     to throw exceptions
   * @throws Throwable if invalid query (and if !repair)
   */
  public void parseAxisDapQuery(
      int language,
      String userDapQuery,
      StringArray destinationNames,
      IntArray constraints,
      boolean repair)
      throws Throwable {

    destinationNames.clear();
    constraints.clear();
    if (reallyVerbose) String2.log("    EDDGrid.parseAxisDapQuery: " + userDapQuery);

    // split userDapQuery at '&' and decode
    String ampParts[] =
        Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")

    // ensure not nothing (which is a data request)
    if (ampParts[0].length() == 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language, EDStatic.messages.queryErrorAr, EDStatic.messages.queryErrorGrid1AxisAr));

    // ignore any &.cmd constraints
    for (int ap = 1; ap < ampParts.length; ap++)
      if (!repair && !ampParts[ap].startsWith("."))
        throw new SimpleException(
            EDStatic.bilingual(
                language, EDStatic.messages.queryErrorAr, EDStatic.messages.queryErrorGridAmpAr));
    userDapQuery = ampParts[0];

    // get the destinationNames
    int po = 0;
    int qLength = userDapQuery.length();
    while (po < qLength) {
      int commaPo = userDapQuery.indexOf(',', po);
      if (commaPo < 0) commaPo = qLength;
      int leftPo = userDapQuery.indexOf('[', po);
      boolean hasBrackets = leftPo >= 0 && leftPo < commaPo;

      // get the destinationName
      String destinationName = userDapQuery.substring(po, hasBrackets ? leftPo : commaPo);
      // if gridName.axisName notation, remove "gridName."
      int period = destinationName.indexOf('.');
      if (period > 0) {
        // ensure gridName is valid
        if (!repair
            && String2.indexOf(dataVariableDestinationNames(), destinationName.substring(0, period))
                < 0)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorUnknownVariableAr[0],
                          destinationName.substring(0, period)),
                  EDStatic.messages.queryErrorAr[language]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorUnknownVariableAr[language],
                          destinationName.substring(0, period))));
        destinationName = destinationName.substring(period + 1);
      }

      // ensure destinationName is valid
      if (reallyVerbose) String2.log("      destinationName=" + destinationName);
      int axis = String2.indexOf(axisVariableDestinationNames(), destinationName);
      if (axis < 0) {
        if (repair) destinationName = axisVariableDestinationNames()[0];
        else {
          if (String2.indexOf(dataVariableDestinationNames(), destinationName) >= 0)
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridNoDataVarAr[0], destinationName),
                    EDStatic.messages.queryErrorAr[language]
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridNoDataVarAr[language],
                            destinationName)));
          findAxisVariableByDestinationName(
              language, destinationName); // throws Throwable if trouble
        }
      }

      // ensure not duplicate destName
      int tdi = destinationNames.indexOf(destinationName);
      if (tdi >= 0) {
        if (repair) return;
        else
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorOccursTwiceAr[0], destinationName),
                  EDStatic.messages.queryErrorAr[language]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorOccursTwiceAr[language], destinationName)));
      } else {
        destinationNames.add(destinationName);
      }

      if (hasBrackets) {
        // get the axis constraints
        int sssp[] =
            parseAxisBrackets(
                language, userDapQuery, destinationName, leftPo, axis, repair, new DoubleArray());
        constraints.add(sssp[0]); // start
        constraints.add(sssp[1]); // stride
        constraints.add(sssp[2]); // stop
        po = sssp[3];
        if (po != commaPo && !repair)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorNotExpectedAtAr[0],
                          userDapQuery.charAt(po),
                          "" + (po + 1)),
                  EDStatic.messages.queryErrorAr[language]
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorNotExpectedAtAr[language],
                          userDapQuery.charAt(po),
                          "" + (po + 1))));
        // if (reallyVerbose) String2.log("      axis=" + axis +
        //    " constraints: " + startI + " : " + strideI + " : " + stopI);
      } else {
        constraints.add(0); // start
        constraints.add(1); // stride
        constraints.add(axisVariables[axis].sourceValues().size() - 1); // stop
      }

      po = commaPo + 1;
    }
  }

  /**
   * Given a percentDecoded part of a userDapQuery, leftPo, and axis, this parses the contents of a
   * [ ] in userDapQuery and returns the startI, strideI, stopI, and rightPo+1.
   *
   * @param language the index of the selected language
   * @param deQuery a percentDecoded part of a userDapQuery
   * @param destinationName axis or grid variable name (for diagnostic purposes only)
   * @param leftPo the position of the "["
   * @param axis the axis number, 0..
   * @param repair if true, this tries to do its best not to throw an exception (guess at intent)
   * @param inputValues the double values parsed from the query for the current axis will be added
   *     to this DoubleArray. These are returned in the order the user provided them, there is no
   *     correction to make sure the lower value is first. Stride is not included, just the start
   *     and stop values. If a single value is provided, it is duplicated in the inputValues. Index
   *     inputs are converted to the value for that index on the axis and the value is added to the
   *     input array (not the index). If the query contains 'last', then the input values will
   *     contain the lastDestinationValue for that axis.
   * @return int[4], 0=startI, 1=strideI, 2=stopI, and 3=newPo (rightPo+1)
   * @throws Throwable if trouble
   */
  protected int[] parseAxisBrackets(
      int language,
      String deQuery,
      String destinationName,
      int leftPo,
      int axis,
      boolean repair,
      DoubleArray inputValues)
      throws Throwable {

    EDVGridAxis av = axisVariables[axis];
    int nAvSourceValues = av.sourceValues().size();
    int precision =
        av instanceof EDVTimeStampGridAxis
            ? 13
            : av.destinationDataPAType() == PAType.DOUBLE ? 9 : 5;
    String diagnostic0 =
        MessageFormat.format(
            EDStatic.messages.queryErrorGridDiagnosticAr[0],
            destinationName,
            "" + axis,
            av.destinationName());
    String diagnosticl =
        MessageFormat.format(
            EDStatic.messages.queryErrorGridDiagnosticAr[language],
            destinationName,
            "" + axis,
            av.destinationName());
    // if (reallyVerbose) String2.log("parseAxisBrackets " + diagnostic0 + ", leftPo=" + leftPo);
    int defaults[] = {0, 1, nAvSourceValues - 1, deQuery.length()};
    // and add defaults in inputValues
    int inputValuesOldSize = inputValues.size();
    inputValues.add(av.destinationMinDouble());
    inputValues.add(av.destinationMaxDouble());

    // leftPo must be '['
    int po = leftPo;
    if (po >= deQuery.length() || deQuery.charAt(po) != '[') {
      if (repair) return defaults;
      else
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + diagnostic0
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorExpectedAtAr[0],
                        "[",
                        "" + po,
                        po >= deQuery.length()
                            ? "[end of query]"
                            : "\"" + deQuery.charAt(po) + "\""),
                EDStatic.messages.queryErrorAr[language]
                    + diagnosticl
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorExpectedAtAr[language],
                        "[",
                        "" + po,
                        po >= deQuery.length()
                            ? "[end of query]"
                            : "\"" + deQuery.charAt(po) + "\"")));
    }

    // find the ']'
    // It shouldn't occur within paren values, so a simple search is fine.
    int rightPo = deQuery.indexOf(']', leftPo + 1);
    if (rightPo < 0) {
      if (repair) return defaults;
      else
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + diagnostic0
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorNotFoundAfterAr[0], "]", "" + leftPo),
                EDStatic.messages.queryErrorAr[language]
                    + diagnosticl
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorNotFoundAfterAr[language], "]", "" + leftPo)));
    }
    defaults[3] = rightPo;
    diagnostic0 +=
        " "
            + EDStatic.messages.EDDConstraintAr[0]
            + "=\""
            + deQuery.substring(leftPo, rightPo + 1)
            + "\"";
    diagnosticl +=
        " "
            + EDStatic.messages.EDDConstraintAr[language]
            + "=\""
            + deQuery.substring(leftPo, rightPo + 1)
            + "\"";
    po = rightPo + 1; // prepare for next axis constraint

    // is there anything between [ and ]?
    int startI, strideI, stopI;
    if (leftPo == rightPo - 1) {
      // [] -> 0:1:max
      startI = 0; // indices
      strideI = 1;
      stopI = nAvSourceValues - 1;
    } else {
      // find colon1
      int colon1 = -1;
      if (deQuery.charAt(leftPo + 1) == '(') {
        // seek closing )
        colon1 = deQuery.indexOf(')', leftPo + 2);
        if (colon1 >= rightPo) {
          if (repair) return defaults;
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorNotFoundAfterAr[0], ")", "" + leftPo),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorNotFoundAfterAr[language],
                            ")",
                            "" + leftPo)));
        }
        colon1++;
      } else {
        // non-paren value
        colon1 = deQuery.indexOf(':', leftPo + 1);
      }
      if (colon1 < 0 || colon1 >= rightPo)
        // just one value inside []
        colon1 = -1;

      // find colon2
      int colon2 = -1;
      if (colon1 < 0) {
        // there is no colon1, so there is no colon2
      } else if (deQuery.charAt(colon1 + 1) == '(') {
        // seek closing "
        colon2 = deQuery.indexOf(')', colon1 + 2);
        if (colon2 >= rightPo) {
          if (repair) return defaults;
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorNotFoundAfterAr[0], ")", "" + (colon2 + 2)),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorNotFoundAfterAr[language],
                            ")",
                            "" + (colon2 + 2))));
        } else {
          // next char must be ']' or ':'
          colon2++;
          if (colon2 == rightPo) {
            colon2 = -1;
          } else if (deQuery.charAt(colon2) == ':') {
            // colon2 is set correctly
          } else {
            if (repair) return defaults;
            else
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.messages.queryErrorAr[0]
                          + diagnostic0
                          + ": "
                          + MessageFormat.format(
                              EDStatic.messages.queryErrorExpectedAtAr[0],
                              "]\" or \":",
                              "" + colon2,
                              "\"" + deQuery.charAt(colon2) + "\""),
                      EDStatic.messages.queryErrorAr[language]
                          + diagnosticl
                          + ": "
                          + MessageFormat.format(
                              EDStatic.messages.queryErrorExpectedAtAr[language],
                              "]\" or \":",
                              "" + colon2,
                              "\"" + deQuery.charAt(colon2) + "\"")));
          }
        }
      } else {
        // non-paren value
        colon2 = deQuery.indexOf(':', colon1 + 1);
        if (colon2 > rightPo) colon2 = -1;
      }
      // String2.log("      " + diagnostic0 + " colon1=" + colon1 + " colon2=" + colon2);

      // extract the string values
      String startS, stopS;
      if (colon1 < 0) {
        // [start]
        startS = deQuery.substring(leftPo + 1, rightPo);
        strideI = 1;
        stopS = startS;
      } else if (colon2 < 0) {
        // [start:stop]
        startS = deQuery.substring(leftPo + 1, colon1);
        strideI = 1;
        stopS = deQuery.substring(colon1 + 1, rightPo);
      } else {
        // [start:stride:stop]
        startS = deQuery.substring(leftPo + 1, colon1);
        String strideS = deQuery.substring(colon1 + 1, colon2);
        strideI = String2.parseInt(strideS);
        stopS = deQuery.substring(colon2 + 1, rightPo);
        if (strideI < 1 || strideI == Integer.MAX_VALUE) {
          if (repair) strideI = 1;
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorInvalidAr[0],
                            EDStatic.messages.EDDGridStrideAr[0] + "=" + strideS),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorInvalidAr[language],
                            EDStatic.messages.EDDGridStrideAr[language] + "=" + strideS)));
        }
      }
      startS = startS.trim();
      stopS = stopS.trim();
      // String2.log("      startS=" + startS + " strideI=" + strideI + " stopS=" + stopS);

      // if (startS.equals("last") || startS.equals("(last)")) {
      //    startI = av.sourceValues().size() - 1;
      // } else
      if (startS.startsWith("last") || startS.startsWith("(last"))
        startS = convertLast(language, av, EDStatic.messages.EDDGridStartAr, startS);

      if (startS.startsWith("(")) {
        // convert paren startS
        startS = startS.substring(1, startS.length() - 1).trim(); // remove begin and end parens
        if (startS.length() == 0 && !repair)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + diagnostic0
                      + ": "
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorGridMissingAr[0],
                          EDStatic.messages.EDDGridStartAr[0]),
                  EDStatic.messages.queryErrorAr[language]
                      + diagnosticl
                      + ": "
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorGridMissingAr[language],
                          EDStatic.messages.EDDGridStartAr[language])));
        double startDestD =
            av.destinationToDouble(startS); // ISO 8601 times -> to epochSeconds w/millis precision
        // String2.log("\n! startS=" + startS + " startDestD=" + startDestD + "\n");
        inputValues.set(inputValuesOldSize, startDestD);
        // since closest() below makes far out values valid, need to test validity
        if (Double.isNaN(startDestD)) {
          if (repair) startDestD = av.destinationMinDouble();
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.notAllowedAr[0],
                            EDStatic.messages.EDDGridStartAr[0] + "=NaN (invalid format?)"),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.notAllowedAr[language],
                            EDStatic.messages.EDDGridStartAr[language]
                                + "=NaN (invalid format?)")));
        }

        startDestD =
            validateGreaterThanThrowOrRepair(
                precision,
                startDestD,
                startS,
                av,
                repair,
                EDStatic.messages.EDDGridStartAr,
                language,
                diagnostic0,
                diagnosticl);
        startDestD =
            validateLessThanThrowOrRepair(
                precision,
                startDestD,
                startS,
                av,
                repair,
                EDStatic.messages.EDDGridStartAr,
                language,
                diagnostic0,
                diagnosticl);

        startI = av.destinationToClosestIndex(startDestD);
        // String2.log("!ParseAxisBrackets startS=" + startS + " startD=" + startDestD + " startI="
        // + startI);

      } else {
        // it must be a >= 0 integer index
        if (!startS.matches("[0-9]+")) {
          if (repair) startS = "0";
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[0],
                            EDStatic.messages.EDDGridStartAr[0],
                            startS,
                            "" + (nAvSourceValues - 1)),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[language],
                            EDStatic.messages.EDDGridStartAr[language],
                            startS,
                            "" + (nAvSourceValues - 1))));
        }

        startI = String2.parseInt(startS);

        if (startI < 0 || startI > nAvSourceValues - 1) {
          if (repair) startI = 0;
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[0],
                            EDStatic.messages.EDDGridStartAr[0],
                            startS,
                            "" + (nAvSourceValues - 1)),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[language],
                            EDStatic.messages.EDDGridStartAr[language],
                            startS,
                            "" + (nAvSourceValues - 1))));
        }
        inputValues.set(inputValuesOldSize, av.destinationDouble(startI));
      }

      // if (startS.equals("last") || stopS.equals("(last)")) {
      //    stopI = av.sourceValues().size() - 1;
      // } else
      if (stopS.startsWith("last") || stopS.startsWith("(last"))
        stopS = convertLast(language, av, EDStatic.messages.EDDGridStopAr, stopS);

      if (stopS.startsWith("(")) {
        // convert paren stopS
        stopS = stopS.substring(1, stopS.length() - 1).trim(); // remove begin and end parens
        if (stopS.length() == 0 && !repair)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0]
                      + diagnostic0
                      + ": "
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorGridMissingAr[0],
                          EDStatic.messages.EDDGridStopAr[0]),
                  EDStatic.messages.queryErrorAr[language]
                      + diagnosticl
                      + ": "
                      + MessageFormat.format(
                          EDStatic.messages.queryErrorGridMissingAr[language],
                          EDStatic.messages.EDDGridStopAr[language])));
        double stopDestD =
            av.destinationToDouble(stopS); // ISO 8601 times -> to epochSeconds w/millis precision
        // String2.log("\n! stopS=" + stopS + " stopDestD=" + stopDestD + "\n");
        inputValues.set(inputValuesOldSize + 1, stopDestD);
        // since closest() below makes far out values valid, need to test validity
        if (Double.isNaN(stopDestD)) {
          if (repair) stopDestD = av.destinationMaxDouble();
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.notAllowedAr[0],
                            EDStatic.messages.EDDGridStopAr[0] + "=NaN (invalid format?)"),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.notAllowedAr[language],
                            EDStatic.messages.EDDGridStopAr[language] + "=NaN (invalid format?)")));
        }

        stopDestD =
            validateGreaterThanThrowOrRepair(
                precision,
                stopDestD,
                stopS,
                av,
                repair,
                EDStatic.messages.EDDGridStopAr,
                language,
                diagnostic0,
                diagnosticl);
        stopDestD =
            validateLessThanThrowOrRepair(
                precision,
                stopDestD,
                stopS,
                av,
                repair,
                EDStatic.messages.EDDGridStopAr,
                language,
                diagnostic0,
                diagnosticl);

        stopI = av.destinationToClosestIndex(stopDestD);
        // String2.log("!ParseAxisBrackets stopS=" + stopS + " stopD=" + stopDestD + " stopI=" +
        // stopI);

      } else {
        // it must be a >= 0 integer index
        stopS = stopS.trim();
        if (!stopS.matches("[0-9]+")) {
          if (repair) stopS = "" + (nAvSourceValues - 1);
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[0],
                            EDStatic.messages.EDDGridStopAr[0],
                            stopS,
                            "" + (nAvSourceValues - 1)),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[language],
                            EDStatic.messages.EDDGridStopAr[language],
                            stopS,
                            "" + (nAvSourceValues - 1))));
        }
        stopI = String2.parseInt(stopS);
        if (stopI < 0 || stopI > nAvSourceValues - 1) {
          if (repair) stopI = nAvSourceValues - 1;
          else
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    EDStatic.messages.queryErrorAr[0]
                        + diagnostic0
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[0],
                            EDStatic.messages.EDDGridStopAr[0],
                            stopS,
                            "" + (nAvSourceValues - 1)),
                    EDStatic.messages.queryErrorAr[language]
                        + diagnosticl
                        + ": "
                        + MessageFormat.format(
                            EDStatic.messages.queryErrorGridBetweenAr[language],
                            EDStatic.messages.EDDGridStopAr[language],
                            stopS,
                            "" + (nAvSourceValues - 1))));
        }
        inputValues.set(inputValuesOldSize + 1, av.destinationDouble(stopI));
      }
    }

    // fix startI > stopI requests
    if (startI > stopI) {
      int ti = startI;
      startI = stopI;
      stopI = ti;
      double d = inputValues.get(axis * 2);
      inputValues.set(inputValuesOldSize, inputValues.get(axis * 2 + 1));
      inputValues.set(inputValuesOldSize + 1, d);
    }

    // return
    return new int[] {startI, strideI, stopI, po};
  }

  /**
   * This converts an OPeNDAP Start or Stop value of "last[-n]" or "(last-x)" into "index" or
   * "(value)". Without parentheses, n is an index number. With parentheses, x is a number '+' is
   * allowed instead of '-'. Internal spaces are allowed.
   *
   * @param language the index of the selected language
   * @param av an EDVGridAxis variable
   * @param name EDStatic.messages.EDDGridStartAr ("Start") or EDStatic.messages.EDDGridStopAr
   *     ("Stop")
   * @param ssValue the start or stop value
   * @return ssValue converted to "index" or a "(value)"
   * @throws Throwable if invalid format or n is too large
   */
  public static String convertLast(int language, EDVGridAxis av, String nameAr[], String ssValue)
      throws Throwable {
    // remove parens
    String ossValue = ssValue;
    boolean hasParens = ssValue.startsWith("(");
    if (hasParens) {
      if (ssValue.endsWith(")")) ssValue = ssValue.substring(1, ssValue.length() - 1).trim();
      else
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLastEndPAr[0], nameAr[0] + "=" + ossValue),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLastEndPAr[language],
                        nameAr[language] + "=" + ossValue)));
    }

    // remove "last"
    if (ssValue.startsWith("last")) ssValue = ssValue.substring(4).trim();
    else
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(
                      EDStatic.messages.queryErrorLastExpectedAr[0], nameAr[0] + "=" + ossValue),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.queryErrorLastExpectedAr[language],
                      nameAr[language] + "=" + ossValue)));

    // done?
    int lastIndex = av.sourceValues().size() - 1;
    if (ssValue.length() == 0)
      return hasParens ? "(" + av.lastDestinationValue() + ")" : "" + lastIndex;

    // +/-
    int pm = ssValue.startsWith("-") ? -1 : ssValue.startsWith("+") ? 1 : 0;
    if (pm == 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0]
                  + MessageFormat.format(
                      EDStatic.messages.queryErrorLastUnexpectedAr[0], nameAr[0] + "=" + ossValue),
              EDStatic.messages.queryErrorAr[language]
                  + MessageFormat.format(
                      EDStatic.messages.queryErrorLastUnexpectedAr[language],
                      nameAr[language] + "=" + ossValue)));
    ssValue = ssValue.substring(1).trim();

    // parse the value
    if (hasParens) {
      double td = String2.parseDouble(ssValue);
      if (!Double.isFinite(td))
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLastPMInvalidAr[0], nameAr[0] + "=" + ossValue),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLastPMInvalidAr[language],
                        nameAr[language] + "=" + ossValue)));
      return "(" + (av.lastDestinationValue() + pm * td) + ")";
    } else {
      try {
        int ti = Integer.parseInt(ssValue); // be strict
        return "" + (lastIndex + pm * ti);
      } catch (Throwable t) {
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.queryErrorAr[0]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLastPMIntegerAr[0], nameAr[0] + "=" + ossValue),
                EDStatic.messages.queryErrorAr[language]
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorLastPMIntegerAr[language],
                        nameAr[language] + "=" + ossValue)),
            t);
      }
    }
  }

  /**
   * This builds an OPeNDAP DAP-style grid-style query, e.g.,
   * var1[start1:stop1][start2:stride2:stop2]. This is close to the opposite of parseDapQuery.
   *
   * @param destinationNames
   * @param constraints will receive the list of constraints, stored in axisVariables.length groups
   *     of 3 int's: start0, stride0, stop0, start1, stride1, stop1, ...
   * @return the array part of an OPeNDAP DAP-style grid-style query, e.g.,
   *     [start1:stop1][start2:stride2:stop2].
   * @throws Throwable if invalid query (0 resultsVariables is a valid query)
   */
  public static String buildDapQuery(StringArray destinationNames, IntArray constraints) {
    String arrayQuery = buildDapArrayQuery(constraints);
    String names[] = destinationNames.toArray();
    for (int i = 0; i < names.length; i++) names[i] += arrayQuery;
    return String2.toSVString(names, ",", false);
  }

  /**
   * This returns the array part of an OPeNDAP DAP-style grid-style query, e.g.,
   * [start1:stop1][start2:stride2:stop2]. This is close to the opposite of parseDapQuery.
   *
   * @param constraints will receive the list of constraints, stored in axisVariables.length groups
   *     of 3 int's: start0, stride0, stop0, start1, stride1, stop1, ...
   * @return the array part of an OPeNDAP DAP-style grid-style query, e.g.,
   *     [start1:stop1][start2:stride2:stop2].
   * @throws Throwable if invalid query (0 resultsVariables is a valid query)
   */
  public static String buildDapArrayQuery(IntArray constraints) {
    StringBuilder sb = new StringBuilder();
    int po = 0;
    while (po < constraints.size()) {
      int stride = constraints.get(po + 1);
      sb.append(
          "["
              + constraints.get(po)
              + ":"
              + (stride == 1 ? "" : stride + ":")
              + constraints.get(po + 2)
              + "]");
      po += 3;
    }
    return sb.toString();
  }

  /**
   * This gets data (not yet standardized) from the data source for this EDDGrid. Because this is
   * called by GridDataAccessor, the request won't be the full user's request, but will be a partial
   * request (for less than EDStatic.config.partialRequestMaxBytes).
   *
   * @param language the index of the selected language
   * @param tDirTable If EDDGridFromFiles, this MAY be the dirTable, else null.
   * @param tFileTable If EDDGridFromFiles, this MAY be the fileTable, else null.
   * @param tDataVariables EDV[] with just the requested data variables
   * @param tConstraints int[nAxisVariables*3] where av*3+0=startIndex, av*3+1=stride,
   *     av*3+2=stopIndex. AxisVariables are counted left to right, e.g., sst[0=time][1=lat][2=lon].
   * @return a PrimitiveArray[] where the first axisVariables.length elements are the axisValues and
   *     the next tDataVariables.length elements are the dataValues. Both the axisValues and
   *     dataValues are straight from the source, not modified.
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  public abstract PrimitiveArray[] getSourceData(
      int language, Table tDirTable, Table tFileTable, EDV tDataVariables[], IntArray tConstraints)
      throws Throwable;

  /**
   * This makes a sibling dataset, based on the new sourceUrl.
   *
   * @param tLocalSourceUrl
   * @param firstAxisToMatch If 0, this tests if sourceValues for axis-variable #0+ are same. If 1,
   *     this tests if sourceValues for axis-variable #1+ are same.
   * @param shareInfo if true, this ensures that the sibling's axis and data variables are basically
   *     the same as this datasets, and then makes the new dataset point to this instance's data
   *     structures to save memory. (AxisVariable #0 isn't duplicated.)
   * @return EDDGrid
   * @throws Throwable if trouble
   */
  public abstract EDDGrid sibling(
      String tLocalSourceUrl, int firstAxisToMatch, int matchAxisNDigits, boolean shareInfo)
      throws Throwable;

  /**
   * This tests if the axisVariables and dataVariables of the other dataset are similar (same
   * destination data var names, same sourceDataType, same units, same missing values).
   *
   * @param other
   * @param firstAxisToMatch If 0, this tests if sourceValues for axis-variable #0+ are same. If 1,
   *     this tests if sourceValues for axis-variable #1+ are same.
   * @param strict if !strict, this is less strict
   * @return "" if similar (same axis and data var names, same units, same sourceDataType, same
   *     missing values) or a message if not (including if other is null).
   */
  public String similar(EDDGrid other, int firstAxisToMatch, int matchAxisNDigits, boolean strict) {
    try {
      if (other == null)
        return "EDDGrid.similar: There is no 'other' dataset.  (Perhaps ERDDAP just restarted.)";
      if (reallyVerbose) String2.log("EDDGrid.similar firstAxisToMatch=" + firstAxisToMatch);
      String results = super.similar(other);
      if (results.length() > 0) return results;

      return similarAxisVariables(other, firstAxisToMatch, matchAxisNDigits, strict);
    } catch (Throwable t) {
      return MustBe.throwableToShortString(t);
    }
  }

  @Override
  public Map<String, String> snapshot() {
    Map<String, String> snapshot = super.snapshot();
    snapshot.put("nAv", "" + dataVariables.length);
    for (int av = 0; av < axisVariables.length; av++) {
      EDVGridAxis variable = axisVariables()[av];
      snapshot.put("av_" + av + "_name", variable.destinationName());
      snapshot.put("av_" + av + "_type", variable.destinationDataType());
      snapshot.put("av_" + av + "_sourceSize", "" + variable.sourceValues().size());
      snapshot.put("av_" + av + "_minValue", "" + variable.destinationCoarseMin());
      snapshot.put("av_" + av + "_maxValue", "" + variable.destinationCoarseMax());
      snapshot.put("av_" + av + "_attr", variable.combinedAttributes().toString());
    }

    return snapshot;
  }

  @Override
  public String changed(Map<String, String> oldSnapshot) {
    if (oldSnapshot == null) return super.changed(oldSnapshot); // so message is consistent

    Map<String, String> newSnapshot = snapshot();
    StringBuilder diff = new StringBuilder();
    // check most important things first
    if (!oldSnapshot.get("nAv").equals(newSnapshot.get("nAv"))) {
      diff.append(
          MessageFormat.format(
              EDStatic.messages.EDDChangedAxesDifferentNVar,
              oldSnapshot.get("nAv"),
              newSnapshot.get("nAv")));
      return diff.toString(); // because tests below assume nAv are same
    }

    int nAv = dataVariables.length;
    for (int av = 0; av < nAv; av++) {
      String nameKey = "av_" + av + "_name";
      String typeKey = "av_" + av + "_type";
      String sourceSizeKey = "av_" + av + "_sourceSize";
      String minKey = "av_" + av + "_minValue";
      String maxKey = "av_" + av + "_maxValue";
      String attrKey = "av_" + av + "_attr";
      String msg2 = "#" + av + "=" + newSnapshot.get(nameKey);
      if (!mapValueMatches(oldSnapshot, newSnapshot, nameKey)) {
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "destinationName",
                    msg2,
                    oldSnapshot.get(nameKey),
                    newSnapshot.get(nameKey))
                + "\n");
      }
      if (!mapValueMatches(oldSnapshot, newSnapshot, typeKey)) {
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "destinationDataType",
                    msg2,
                    oldSnapshot.get(typeKey),
                    newSnapshot.get(typeKey))
                + "\n");
      }
      if (!mapValueMatches(oldSnapshot, newSnapshot, sourceSizeKey)) {
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "numberOfValues",
                    msg2,
                    oldSnapshot.get(sourceSizeKey),
                    newSnapshot.get(sourceSizeKey))
                + "\n");
      }
      if (!mapValueMatches(oldSnapshot, newSnapshot, minKey)) {
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "minValue",
                    msg2,
                    oldSnapshot.get(minKey),
                    newSnapshot.get(minKey))
                + "\n");
      }
      if (!mapValueMatches(oldSnapshot, newSnapshot, maxKey)) {
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "maxValue",
                    msg2,
                    oldSnapshot.get(maxKey),
                    newSnapshot.get(maxKey))
                + "\n");
      }
      String s = String2.differentLine(oldSnapshot.get(attrKey), newSnapshot.get(attrKey));
      if (s.length() > 0) {
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes1Different, "combinedAttribute", msg2, s)
                + "\n");
      }
    }

    diff.append(super.changed(oldSnapshot));
    return diff.toString();
  }

  /**
   * This tests if 'old' is different from this in any way. <br>
   * This test is from the view of a subscriber who wants to know when a dataset has changed in any
   * way. <br>
   * So some things like onChange and reloadEveryNMinutes are not checked. <br>
   * This only lists the first change found.
   *
   * <p>EDDGrid overwrites this to also check the axis variables.
   *
   * @param old
   * @return "" if same or message if not.
   */
  @Override
  public String changed(EDD old) {
    if (old == null) return super.changed(old); // so message is consistent

    if (!(old instanceof EDDGrid)) return EDStatic.messages.EDDChangedTableToGrid + "\n";

    EDDGrid oldG = (EDDGrid) old;

    // check most important things first
    int nAv = axisVariables.length;
    StringBuilder diff = new StringBuilder();
    String oldS = "" + oldG.axisVariables().length;
    String newS = "" + nAv;
    if (!oldS.equals(newS)) {
      diff.append(
          MessageFormat.format(EDStatic.messages.EDDChangedAxesDifferentNVar, oldS, newS) + "\n");
      return diff.toString(); // because tests below assume nAv are same
    }

    for (int av = 0; av < nAv; av++) {
      EDVGridAxis oldAV = oldG.axisVariables[av];
      EDVGridAxis newAV = axisVariables[av];
      String newName = newAV.destinationName();
      String msg2 = "#" + av + "=" + newName;

      oldS = oldAV.destinationName();
      newS = newName;
      if (!oldS.equals(newS))
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different, "destinationName", msg2, oldS, newS)
                + "\n");

      oldS = oldAV.destinationDataType();
      newS = newAV.destinationDataType();
      if (!oldS.equals(newS))
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "destinationDataType",
                    msg2,
                    oldS,
                    newS)
                + "\n");

      // most import case: new time value will be displayed as an iso time
      oldS = "" + oldAV.sourceValues().size();
      newS = "" + newAV.sourceValues().size();
      if (!oldS.equals(newS))
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different, "numberOfValues", msg2, oldS, newS)
                + "\n");

      int diffIndex = newAV.sourceValues().diffIndex(oldAV.sourceValues());
      if (diffIndex >= 0)
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes2Different,
                    "destinationValues",
                    msg2,
                    "index #"
                        + diffIndex
                        + "="
                        + (diffIndex >= oldAV.sourceValues().size()
                            ? EDStatic.messages.EDDChangedNoValue
                            : oldAV.destinationToString(
                                oldAV.destinationValue(diffIndex).getDouble(0))),
                    "index #"
                        + diffIndex
                        + "="
                        + (diffIndex >= newAV.sourceValues().size()
                            ? EDStatic.messages.EDDChangedNoValue
                            : newAV.destinationToString(
                                newAV.destinationValue(diffIndex).getDouble(0))))
                + "\n");

      String s =
          String2.differentLine(
              oldAV.combinedAttributes().toString(), newAV.combinedAttributes().toString());
      if (s.length() > 0)
        diff.append(
            MessageFormat.format(
                    EDStatic.messages.EDDChangedAxes1Different, "combinedAttribute", msg2, s)
                + "\n");
    }

    // check least important things last
    diff.append(super.changed(oldG));
    return diff.toString();
  }

  /**
   * This tests if the axisVariables of the other dataset are similar (same destination data var
   * names, same sourceDataType, same units, same missing values).
   *
   * @param other
   * @param firstAxisToMatch If 0, this tests if sourceValues for axis-variable #0+ are same. If 1,
   *     this tests if sourceValues for axis-variable #1+ are same.
   * @param strict if !strict, this is less strict (including allowing different sourceDataTypes and
   *     destinationDataTypes)
   * @return "" if similar (same axis and data var names, same units, same sourceDataType, same
   *     missing values) or a message if not.
   */
  public String similarAxisVariables(
      EDDGrid other, int firstAxisToMatch, int matchAxisNDigits, boolean strict) {
    if (reallyVerbose)
      String2.log("EDDGrid.similarAxisVariables firstAxisToMatch=" + firstAxisToMatch);
    String msg = "EDDGrid.similar: The other dataset has a different ";
    int nAv = axisVariables.length;
    if (nAv != other.axisVariables.length)
      return msg + "number of axisVariables (" + nAv + " != " + other.axisVariables.length + ")";

    for (int av = 0; av < nAv; av++) {
      EDVGridAxis av1 = axisVariables[av];
      EDVGridAxis av2 = other.axisVariables[av];

      // destinationName
      String s1 = av1.destinationName();
      String s2 = av2.destinationName();
      String msg2 = " for axisVariable #" + av + "=" + s1 + " (";
      if (!s1.equals(s2)) return msg + "destinationName" + msg2 + s1 + " != " + s2 + ")";

      // sourceDataType
      // if !strict, don't care e.g., if one is float and the other is double
      if (strict) {
        s1 = av1.sourceDataType();
        s2 = av2.sourceDataType();
        if (!s1.equals(s2)) return msg + "sourceDataType" + msg2 + s1 + " != " + s2 + ")";
      }

      // destinationDataType
      if (strict) {
        s1 = av1.destinationDataType();
        s2 = av2.destinationDataType();
        if (!s1.equals(s2)) return msg + "destinationDataType" + msg2 + s1 + " != " + s2 + ")";
      }

      // units
      s1 = av1.units();
      s2 = av2.units();
      if (!s1.equals(s2)) return msg + "units" + msg2 + s1 + " != " + s2 + ")";

      // sourceMissingValue  (irrelevant, since shouldn't be any mv)
      double d1, d2;
      if (strict) {
        d1 = av1.sourceMissingValue();
        d2 = av2.sourceMissingValue();
        if (!Test.equal(d1, d2)) // says NaN==NaN is true
        return msg + "sourceMissingValue" + msg2 + d1 + " != " + d2 + ")";
      }

      // sourceFillValue  (irrelevant, since shouldn't be any mv)
      if (strict) {
        d1 = av1.sourceFillValue();
        d2 = av2.sourceFillValue();
        if (!Test.equal(d1, d2)) // says NaN==NaN is true
        return msg + "sourceFillValue" + msg2 + d1 + " != " + d2 + ")";
      }

      // test sourceValues
      if (av >= firstAxisToMatch) {
        String results = av1.sourceValues().almostEqual(av2.sourceValues(), matchAxisNDigits);
        if (results.length() > 0) return msg + "sourceValue" + msg2 + results + ")";
      }
    }
    // they are similar
    return "";
  }

  /**
   * This responds to an OPeNDAP-style query.
   *
   * @param language the index of the selected language
   * @param request may be null. Currently, it is not used. (It is passed to respondToGraphQuery,
   *     but it doesn't use it.)
   * @param response Currently, not used. It may be null.
   * @param ipAddress The IP address of the user (for statistics).
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but in unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null).
   * @param outputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. Now, this usually does call out.close() at the end if all goes well, but
   *     still assumes caller will close it (notably if trouble).
   * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
   * @param fileName the name for the 'file' (no dir, no extension), which is used to write the
   *     suggested name for the file to the response header.
   * @param fileTypeName the fileTypeName for the new file (e.g., .largePng).
   * @throws Throwable if trouble
   */
  @Override
  public void respondToDapQuery(
      int language,
      HttpServletRequest request,
      HttpServletResponse response,
      String ipAddress,
      String loggedInAs,
      String requestUrl,
      String endOfRequest,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String dir,
      String fileName,
      String fileTypeName)
      throws Throwable {

    try {
      String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
      // save data to outputStream
      switch (fileTypeName) {
        case ".graph" -> {
          respondToGraphQuery(
              language,
              request,
              loggedInAs,
              requestUrl,
              endOfRequest,
              userDapQuery,
              outputStreamSource,
              dir,
              fileName,
              fileTypeName);
          return;
        }
        case ".html" -> {
          // it is important that this use outputStreamSource so stream is compressed (if possible)
          // OPeNDAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible
          // unicode
          // With HTML 5 and for future, best to go with UTF_8.  Also, <startHeadHtml> says UTF_8.
          OutputStream out = outputStreamSource.outputStream(File2.UTF_8);
          Writer writer = File2.getBufferedWriterUtf8(out);
          try {
            writer.write(
                EDStatic.startHeadHtml(
                    language, tErddapUrl, title() + " - " + EDStatic.messages.dafAr[language]));
            writer.write("\n" + rssHeadLink());
            writer.write("\n</head>\n");
            writer.write(
                EDStatic.startBodyHtml(
                    request,
                    language,
                    loggedInAs,
                    "griddap/" + datasetID + ".html", // was endOfRequest,
                    userDapQuery));
            writer.write("\n");
            writer.write(
                HtmlWidgets.htmlTooltipScript(
                    EDStatic.imageDirUrl(
                        request, loggedInAs, language))); // this is a link to a script
            writer.write(
                HtmlWidgets.dragDropScript(
                    EDStatic.imageDirUrl(
                        request, loggedInAs, language))); // this is a link to a script
            writer.flush(); // Steve Souder says: the sooner you can send some html to user, the
            // better
            writer.write("<div class=\"standard_width\">\n");
            writer.write(
                EDStatic.youAreHereWithHelp(
                    request,
                    language,
                    loggedInAs,
                    dapProtocol,
                    EDStatic.messages.dafAr[language],
                    "<div class=\"standard_max_width\">"
                        + EDStatic.messages.dafGridTooltipAr[language]
                        + "<p>"
                        + EDStatic.messages.EDDGridDownloadDataTooltipAr[language]
                        + "</ol>\n"
                        + EDStatic.messages.dafGridBypassTooltipAr[language]
                        + "</div>"));
            writeHtmlDatasetInfo(
                request, language, loggedInAs, writer, true, false, true, true, userDapQuery, "");
            if (userDapQuery.length() == 0)
              userDapQuery =
                  defaultDataQuery(); // after writeHtmlDatasetInfo and before writeDapHtmlForm
            writeDapHtmlForm(request, language, loggedInAs, userDapQuery, writer);

            // End of page / other info

            // das (with info about this dataset)
            writer.write(
                "<hr>\n"
                    + "<h2><a class=\"selfLink\" id=\"DAS\" href=\"#DAS\" rel=\"bookmark\">"
                    + EDStatic.messages.dasTitleAr[language]
                    + "</a></h2>\n"
                    + "<pre style=\"white-space:pre-wrap;\">\n");
            writeDAS(
                File2.forceExtension(requestUrl, ".das"),
                "",
                writer,
                true); // useful so search engines find all relevant words
            writer.write("</pre>\n");

            // then dap instructions
            writer.write(
                """
                            <br>&nbsp;
                            <hr>
                            """);
            writeGeneralDapHtmlInstructions(language, tErddapUrl, writer, false);
            writer.write("</div>\n");
            writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
            writer.write("</html>");
            writer.close();

          } catch (Exception e) {
            EDStatic.rethrowClientAbortException(e); // first thing in catch{}
            writer.write(EDStatic.htmlForException(language, e));
            writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
            writer.write("</html>");
            writer.close();
            throw e;
          }
          return;
        }
      }

      if (fileTypeName.endsWith("Info")
          && (fileTypeName.equals(".smallPngInfo")
              || fileTypeName.equals(".pngInfo")
              || fileTypeName.equals(".largePngInfo")
              || fileTypeName.equals(".smallPdfInfo")
              || fileTypeName.equals(".pdfInfo")
              || fileTypeName.equals(".largePdfInfo"))) {
        // try to readPngInfo (if fromErddap, this saves it to local file)
        // (if info not available, this will fail)
        String imageFileType = fileTypeName.substring(0, fileTypeName.length() - 4);
        Object[] pngInfo = readPngInfo(loggedInAs, userDapQuery, imageFileType);
        if (pngInfo == null)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.queryErrorAr[0] + EDStatic.messages.errorFileNotFoundImageAr[0],
                  EDStatic.messages.queryErrorAr[language]
                      + EDStatic.messages.errorFileNotFoundImageAr[language]));

        // ok, copy it  (and don't close the outputStream)
        try (OutputStream out = outputStreamSource.outputStream(File2.UTF_8)) {
          if (!File2.copy(getPngInfoFileName(loggedInAs, userDapQuery, imageFileType), out))
            throw new SimpleException(String2.ERROR + " while transmitting file.");
        }
        // downloads of e.g., erddap2.css don't work right if not closed. (just if gzip'd?)
        return;
      }

      EDDFileTypeInfo fileInfo = EDD_FILE_TYPE_INFO.get(fileTypeName);
      if (fileInfo == null) {
        return;
      }
      FileTypeInterface fileTypeHandler = fileInfo.getInstance();
      DapRequestInfo requestInfo =
          new DapRequestInfo(
              language,
              this,
              null,
              outputStreamSource,
              requestUrl,
              userDapQuery,
              loggedInAs,
              endOfRequest,
              fileName,
              dir,
              fileTypeName,
              ipAddress,
              request,
              response);

      if (fileTypeHandler != null) {
        fileTypeHandler.writeGridToStream(requestInfo);
        return;
      }

    } finally {
      if (!dimensionValuesInMemory) setDimensionValuesToNull();
    }
  }

  /**
   * This deals with requests for a Make A Graph (MakeAGraph, MAG) web page for this dataset.
   *
   * @param language the index of the selected language
   * @param request may be null. Currently, this isn't used.
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but it unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery from the user (may be "" or null), still percentEncoded (shouldn't be
   *     null). If the query has missing or invalid parameters, defaults will be used. If the query
   *     has irrelevant parameters, they will be ignored.
   * @param outputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered.
   * @param dir the directory to use for temporary/cache files [currently, not used]
   * @param fileName the name for the 'file' (no dir, no extension), which is used to write the
   *     suggested name for the file to the response header. [currently, not used]
   * @param fileTypeName must be .graph [currently, not used]
   * @throws Throwable if trouble
   */
  @Override
  public void respondToGraphQuery(
      int language,
      HttpServletRequest request,
      String loggedInAs,
      String requestUrl,
      String endOfRequest,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String dir,
      String fileName,
      String fileTypeName)
      throws Throwable {

    if (reallyVerbose) String2.log("*** respondToGraphQuery");
    if (accessibleViaMAG().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr) + accessibleViaMAG());

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String formName = "f1"; // change JavaScript below if this changes
    OutputStream out = outputStreamSource.outputStream(File2.UTF_8);
    Writer writer = File2.getBufferedWriterUtf8(out);
    try {
      HtmlWidgets widgets =
          new HtmlWidgets(true, EDStatic.imageDirUrl(request, loggedInAs, language));

      // write the header
      writer.write(
          EDStatic.startHeadHtml(
              language, tErddapUrl, title() + " - " + EDStatic.messages.magAr[language]));
      writer.write("\n" + rssHeadLink());
      writer.write("\n</head>\n");
      writer.write(
          EDStatic.startBodyHtml(
              request,
              language,
              loggedInAs,
              "griddap/" + datasetID + ".graph", // was endOfRequest,
              userDapQuery));
      writer.write("\n");
      writer.write(
          HtmlWidgets.htmlTooltipScript(
              EDStatic.imageDirUrl(request, loggedInAs, language))); // this is a link to a script
      writer.write(
          HtmlWidgets.dragDropScript(
              EDStatic.imageDirUrl(request, loggedInAs, language))); // this is a link to a script
      writer.flush(); // Steve Souder says: the sooner you can send some html to user, the better
      writer.write("<div class=\"standard_width\">\n");
      writer.write(
          EDStatic.youAreHereWithHelp(
              request,
              language,
              loggedInAs,
              "griddap",
              EDStatic.messages.magAr[language],
              "<div class=\"standard_max_width\">"
                  + EDStatic.messages.magGridTooltipAr[language]
                  + "</div>"));
      writeHtmlDatasetInfo(
          request, language, loggedInAs, writer, true, true, true, false, userDapQuery, "");
      if (userDapQuery.length() == 0)
        userDapQuery =
            defaultGraphQuery(); // after writeHtmlDatasetInfo and before Table.getDapQueryParts
      writer.write(HtmlWidgets.ifJavaScriptDisabled + "\n");

      // make the big table
      writer.write("&nbsp;\n"); // necessary for the blank line before the table (not <p>)

      writer.write(widgets.beginTable("class=\"compact\"")); // the big table
      writer.write("<tr><td class=\"L T\">\n");
      // writer.write("<div class=\"leftContent\">");

      // begin the form
      writer.write(widgets.beginForm(formName, "GET", "", ""));

      // parse the query so &-separated parts are handy
      String paramName, partName, partValue, pParts[];
      String queryParts[] =
          Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")

      // find the axisVariables (all are always numeric) with >1 value
      StringArray sa = new StringArray();
      for (int av = 0; av < axisVariables.length; av++) {
        if (axisVariables[av].sourceValues().size() > 1) sa.add(axisVariableDestinationNames()[av]);
      }
      // if (sa.size() == 0)  //accessibleViaMAG tests this
      String[] avNames = sa.toArray(); // av with >1 value
      int nAvNames = avNames.length;

      // find the numeric dataVariables
      sa = new StringArray();
      for (EDV dataVariable : dataVariables) {
        if (dataVariable.destinationDataPAType() != PAType.STRING)
          sa.add(dataVariable.destinationName());
      }
      String[] dvNames = sa.toArray(); // list of dvNames
      sa.atInsert(0, "");
      String[] dvNames0 = sa.toArray(); // list of #0="" + dvNames
      sa = null;
      // if you need advNames, you will need to modify the javascript below

      // parse the query to get preferredDV0 and constraints
      StringArray tDestNames = new StringArray();
      IntArray tConstraints = new IntArray();
      parseDataDapQuery(language, userDapQuery, tDestNames, tConstraints, true);
      String preferredDV0 = tDestNames.size() > 0 ? tDestNames.get(0) : dvNames[0];
      if (reallyVerbose) String2.log("preferredDV0=" + preferredDV0);

      String gap = "&nbsp;&nbsp;&nbsp;";

      // *** set the Graph Type options
      StringArray drawsSA = new StringArray();
      // it is important for javascript below that first 3 options are the very similar (L L&M M)
      drawsSA.add("lines");
      drawsSA.add("linesAndMarkers");
      int defaultDraw = 1;
      drawsSA.add("markers");
      if (axisVariables.length >= 1 && dataVariables.length >= 2) drawsSA.add("sticks");
      if (nAvNames >= 2) {
        if ((lonIndex >= 0 && latIndex >= 0)
            || ("x".equalsIgnoreCase(avNames[nAvNames - 1])
                && "y".equalsIgnoreCase(avNames[nAvNames - 2]))) defaultDraw = drawsSA.size();
        drawsSA.add("surface");
      }
      if (lonIndex >= 0 && latIndex >= 0 && dataVariables.length >= 2) drawsSA.add("vectors");
      String draws[] = drawsSA.toArray();
      boolean preferDefaultVars = true;
      int draw = defaultDraw;
      partValue = String2.stringStartsWith(queryParts, partName = ".draw=");
      if (partValue != null) {
        draw = String2.indexOf(draws, partValue.substring(partName.length()));
        if (draw >= 0) { // valid .draw was specified
          preferDefaultVars = false;
          // but check that it is possible
          boolean trouble = draws[draw].equals("surface") && nAvNames < 2;
          if (draws[draw].equals("vectors") && (lonIndex < 0 || latIndex < 0)) trouble = true;
          if ((draws[draw].equals("sticks") || draws[draw].equals("vectors")) && dvNames.length < 2)
            trouble = true;
          if (trouble) {
            preferDefaultVars = true;
            draw = String2.indexOf(draws, "linesAndMarkers"); // safest
          }
        } else {
          preferDefaultVars = true;
          draw = defaultDraw;
        }
      }
      boolean drawLines = draws[draw].equals("lines");
      boolean drawLinesAndMarkers = draws[draw].equals("linesAndMarkers");
      boolean drawMarkers = draws[draw].equals("markers");
      boolean drawSticks = draws[draw].equals("sticks");
      boolean drawSurface = draws[draw].equals("surface");
      boolean drawVectors = draws[draw].equals("vectors");
      if (reallyVerbose)
        String2.log("draw=" + draws[draw] + " preferDefaultVars=" + preferDefaultVars);
      // if (debugMode) String2.log("respondToGraphQuery 3");

      // find default stick or vector data vars  (adjacent, starting at dv=sameUnits1)
      // heuristic: look for two adjacent dv that have same units
      int sameUnits1 = 0; // default  dv
      String units1 = null;
      String units2 = findDataVariableByDestinationName(dvNames[0]).units();
      for (int sameUnits2 = 1; sameUnits2 < dvNames.length; sameUnits2++) {
        units1 = units2;
        units2 = findDataVariableByDestinationName(dvNames[sameUnits2]).units();
        if (units1 != null && units2 != null && units1.equals(units2)) {
          sameUnits1 = sameUnits2 - 1;
          break;
        }
      }

      // set draw-related things
      int nVars = -1;
      String varLabel[], varHelp[], varOptions[][];
      String varName[] = {"", "", "", ""}; // fill from .vars, else with defaults
      String varsPartName = ".vars=";
      String varsPartValue = String2.stringStartsWith(queryParts, varsPartName);
      String varsParts[];
      if (varsPartValue == null) varsParts = new String[0];
      else varsParts = String2.split(varsPartValue.substring(varsPartName.length()), '|');
      if (drawLines) {
        nVars = 2;
        varLabel =
            new String[] {
              EDStatic.messages.magAxisXAr[language] + ":",
              EDStatic.messages.magAxisYAr[language] + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.magAxisHelpGraphXAr[language],
              EDStatic.messages.magAxisHelpGraphYAr[language]
            };
        varOptions = new String[][] {avNames, dvNames};
        varName[0] =
            (varsParts.length > 0 && String2.indexOf(avNames, varsParts[0]) >= 0)
                ? varsParts[0]
                : String2.indexOf(avNames, EDV.TIME_NAME) >= 0 ? EDV.TIME_NAME : avNames[0];
        varName[1] =
            (varsParts.length > 1 && String2.indexOf(dvNames, varsParts[1]) >= 0)
                ? varsParts[1]
                : preferredDV0;
      } else if (drawLinesAndMarkers || drawMarkers) {
        nVars = 3;
        varLabel =
            new String[] {
              EDStatic.messages.magAxisXAr[language] + ":",
              EDStatic.messages.magAxisYAr[language] + ":",
              EDStatic.messages.magAxisColorAr[language] + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.magAxisHelpGraphXAr[language],
              EDStatic.messages.magAxisHelpGraphYAr[language],
              EDStatic.messages.magAxisHelpMarkerColorAr[language]
            };
        varOptions = new String[][] {avNames, dvNames, dvNames0};
        varName[0] =
            (varsParts.length > 0 && String2.indexOf(avNames, varsParts[0]) >= 0)
                ? varsParts[0]
                : String2.indexOf(avNames, EDV.TIME_NAME) >= 0 ? EDV.TIME_NAME : avNames[0];
        varName[1] =
            (varsParts.length > 1 && String2.indexOf(dvNames, varsParts[1]) >= 0)
                ? varsParts[1]
                : preferredDV0;
        varName[2] =
            (varsParts.length > 2 && String2.indexOf(dvNames0, varsParts[2]) >= 0)
                ? varsParts[2]
                : "";
        // String2.log(">>draw.Markers varName=" + String2.toCSSVString(varName));
      } else if (drawSticks) {
        nVars = 3;
        varLabel =
            new String[] {
              EDStatic.messages.magAxisXAr[language] + ":",
              EDStatic.messages.magAxisStickXAr[language] + ":",
              EDStatic.messages.magAxisStickYAr[language] + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.magAxisHelpGraphXAr[language],
                  EDStatic.messages.magAxisHelpGraphYAr[language],
              EDStatic.messages.magAxisHelpStickXAr[language],
                  EDStatic.messages.magAxisHelpStickYAr[language]
            };
        varOptions = new String[][] {avNames, dvNames, dvNames};
        varName[0] =
            (varsParts.length > 0 && String2.indexOf(avNames, varsParts[0]) >= 0)
                ? varsParts[0]
                : String2.indexOf(avNames, EDV.TIME_NAME) >= 0 ? EDV.TIME_NAME : avNames[0];
        if (varsParts.length > 2
            && String2.indexOf(dvNames, varsParts[1]) >= 0
            && String2.indexOf(dvNames, varsParts[2]) >= 0) {
          varName[1] = varsParts[1];
          varName[2] = varsParts[2];
        } else {
          varName[1] = dvNames[sameUnits1];
          varName[2] = dvNames[sameUnits1 + 1];
        }
      } else if (drawSurface) {
        nVars = 3;
        varLabel =
            new String[] {
              EDStatic.messages.magAxisXAr[language] + ":",
              EDStatic.messages.magAxisYAr[language] + ":",
              EDStatic.messages.magAxisColorAr[language] + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.magAxisHelpMapXAr[language],
              EDStatic.messages.magAxisHelpMapYAr[language],
              EDStatic.messages.magAxisHelpSurfaceColorAr[language]
            };
        varOptions = new String[][] {avNames, avNames, dvNames};
        varName[0] =
            (varsParts.length > 0 && String2.indexOf(avNames, varsParts[0]) >= 0)
                ? varsParts[0]
                : lonIndex >= 0 ? EDV.LON_NAME : avNames[nAvNames - 1];
        varName[1] =
            (varsParts.length > 1 && String2.indexOf(avNames, varsParts[1]) >= 0)
                ? varsParts[1]
                : lonIndex >= 0 ? EDV.LON_NAME : avNames[nAvNames - 1];
        if (varName[0].equals(varName[1])) {
          if (varName[0].equals(avNames[nAvNames - 1])) varName[1] = avNames[nAvNames - 2];
          else varName[0] = avNames[nAvNames - 1];
        }
        varName[2] =
            (varsParts.length > 2 && String2.indexOf(dvNames, varsParts[2]) >= 0)
                ? varsParts[2]
                : preferredDV0;
      } else if (drawVectors) {
        nVars = 4;
        varLabel =
            new String[] {
              EDStatic.messages.magAxisXAr[language] + ":",
              EDStatic.messages.magAxisYAr[language] + ":",
              EDStatic.messages.magAxisVectorXAr[language] + ":",
              EDStatic.messages.magAxisVectorYAr[language] + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.magAxisHelpMapXAr[language],
              EDStatic.messages.magAxisHelpMapYAr[language],
              EDStatic.messages.magAxisHelpVectorXAr[language],
              EDStatic.messages.magAxisHelpVectorYAr[language]
            };
        varOptions =
            new String[][] {
              new String[] {EDV.LON_NAME}, new String[] {EDV.LAT_NAME}, dvNames, dvNames
            };
        varName[0] = EDV.LON_NAME;
        varName[1] = EDV.LAT_NAME;
        if (varsParts.length > 3
            && String2.indexOf(dvNames, varsParts[2]) >= 0
            && String2.indexOf(dvNames, varsParts[3]) >= 0) {
          varName[2] = varsParts[2];
          varName[3] = varsParts[3];
        } else {
          varName[2] = dvNames[sameUnits1];
          varName[3] = dvNames[sameUnits1 + 1];
        }
        // ??? ensure same units???
      } else
        throw new SimpleException(
            EDStatic.messages.errorInternalAr[0] + "'draw' wasn't set correctly.");
      // if (debugMode) String2.log("respondToGraphQuery 4");

      // avoid lat lon reversed (which sgtMap will reverse)
      if (varName[0].equals("latitude") && varName[1].equals("longitude")) {
        varName[0] = "longitude";
        varName[1] = "latitude";
      }
      boolean isMap = "longitude".equals(varName[0]) && "latitude".equals(varName[1]);

      // find axisVar index (or -1 if not an axis var or not index in avNames)
      int axisVarX = String2.indexOf(axisVariableDestinationNames(), varName[0]);
      int axisVarY = String2.indexOf(axisVariableDestinationNames(), varName[1]);
      if (reallyVerbose) String2.log("varName[]=" + String2.toCSSVString(varName));

      // set dimensions' start and stop
      int nAv = axisVariables.length;
      String avStart[] = new String[nAv];
      String avStop[] = new String[nAv];
      int avStartIndex[] = new int[nAv];
      int avStopIndex[] = new int[nAv];
      StringBuilder constraints = new StringBuilder();
      String sliderFromNames[] = new String[nAv];
      String sliderToNames[] = new String[nAv];
      int sliderNThumbs[] = new int[nAv];
      String sliderUserValuesCsvs[] = new String[nAv];
      int sliderInitFromPositions[] = new int[nAv];
      int sliderInitToPositions[] = new int[nAv];
      boolean showStartAndStopFields[] = new boolean[nAv];
      int sourceSize[] = new int[nAv];
      double latFirst = Double.NaN,
          lonFirst = Double.NaN,
          timeFirst = Double.NaN,
          latLast = Double.NaN,
          lonLast = Double.NaN,
          timeLast = Double.NaN,
          latStart = Double.NaN,
          lonStart = Double.NaN,
          timeStart = Double.NaN,
          latStop = Double.NaN,
          lonStop = Double.NaN,
          timeStop = Double.NaN,
          latCenter = Double.NaN,
          lonCenter = Double.NaN,
          timeCenter = Double.NaN,
          latRange = Double.NaN,
          lonRange = Double.NaN,
          timeRange = Double.NaN;
      String time_precision = null;
      int lonAscending = 0, latAscending = 0, timeAscending = 0;
      for (int av = 0; av < nAv; av++) {
        EDVGridAxis edvga = axisVariables[av];
        double defStart =
            av == timeIndex
                ? // note max vs first
                Math.max(
                    edvga.destinationMaxDouble() - 7 * Calendar2.SECONDS_PER_DAY,
                    edvga.destinationMinDouble())
                : edvga.firstDestinationValue();
        double defStop =
            av == timeIndex ? edvga.destinationMaxDouble() : edvga.lastDestinationValue();
        sourceSize[av] = edvga.sourceValues().size();
        boolean isTimeStamp = edvga instanceof EDVTimeStampGridAxis;
        int precision = isTimeStamp ? 13 : edvga.destinationDataPAType() == PAType.DOUBLE ? 9 : 5;
        showStartAndStopFields[av] = av == axisVarX || av == axisVarY;

        // find start and end
        int ti1 = tConstraints.get(av * 3 + 0);
        int ti2 = tConstraints.get(av * 3 + 2);
        int tLast = sourceSize[av] - 1;
        if (showStartAndStopFields[av] && ti2 - ti1 < 2) {
          // 1-value axis now multi-value, and axis is either of right 2 axes
          if (ti1 == ti2 && nAv - av <= 2) {
            ti1 = 0;
            ti2 = tLast; // all values
          } else { // at least 3 values
            ti2 = Math.min(ti1 + 2, tLast);
            if (ti2 - ti1 < 2) ti1 = Math.max(ti2 - 2, 0);
          }
        }
        double dStart =
            userDapQuery.length() == 0 ? defStart : edvga.destinationValue(ti1).getNiceDouble(0);
        double dStop =
            userDapQuery.length() == 0 ? defStop : edvga.destinationValue(ti2).getNiceDouble(0);

        // compare dStart and dStop to ensure valid
        if (edvga.averageSpacing() > 0) { // may be negative,  looser test than isAscending
          // ascending axis values
          if (showStartAndStopFields[av]) {
            if (Math2.greaterThanAE(precision, dStart, dStop)) dStart = defStart;
            if (Math2.greaterThanAE(precision, dStart, dStop)) dStop = defStop;
          } else {
            if (!Math2.lessThanAE(precision, dStart, dStop))
              dStart =
                  edvga.firstDestinationValue(); // not defStart; stop field is always visible, so
            // change start
            if (!Math2.lessThanAE(precision, dStart, dStop)) dStop = edvga.lastDestinationValue();
          }
        } else {
          // descending axis values
          if (showStartAndStopFields[av]) {
            if (Math2.greaterThanAE(precision, dStop, dStart)) // stop start reversed from above
            dStart = defStart;
            if (Math2.greaterThanAE(precision, dStop, dStart)) dStop = defStop;
          } else {
            if (!Math2.lessThanAE(precision, dStop, dStart))
              dStart =
                  edvga.firstDestinationValue(); // not defStart; stop field is always visible, so
            // change start
            if (!Math2.lessThanAE(precision, dStop, dStart)) dStop = edvga.lastDestinationValue();
          }
        }

        // format
        avStart[av] = edvga.destinationToString(dStart);
        avStop[av] = edvga.destinationToString(dStop);
        avStartIndex[av] = edvga.destinationToClosestIndex(dStart);
        avStopIndex[av] = edvga.destinationToClosestIndex(dStop);

        if (av == lonIndex) {
          lonAscending = edvga.isAscending() ? 1 : -1;
          lonFirst = edvga.firstDestinationValue();
          lonLast = edvga.lastDestinationValue();
          lonStart = dStart;
          lonStop = dStop;
          lonCenter = (dStart + dStop) / 2;
          lonRange = dStop - dStart;
        }
        if (av == latIndex) {
          latAscending = edvga.isAscending() ? 1 : -1;
          latFirst = edvga.firstDestinationValue();
          latLast = edvga.lastDestinationValue();
          latStart = dStart;
          latStop = dStop;
          latCenter = (dStart + dStop) / 2;
          latRange = dStop - dStart;
        }
        if (av == timeIndex) {
          timeAscending = edvga.isAscending() ? 1 : -1;
          timeFirst = edvga.firstDestinationValue();
          timeLast = edvga.lastDestinationValue();
          timeStart = dStart;
          timeStop = dStop;
          timeCenter = (dStart + dStop) / 2;
          timeRange = dStop - dStart;
          time_precision = edvga.combinedAttributes().getString(EDV.TIME_PRECISION);
        }
      }

      // zoomLatLon is for maps (click to recenter and buttons to zoom in/out)
      boolean zoomLatLon = (drawSurface || drawVectors) && isMap;
      // zoomTime is for timeseries graphs
      boolean zoomTime = varName[0].equals("time") && timeAscending == 1;
      // zoomX is for non-surface graphs
      boolean zoomX =
          !zoomLatLon
              && !drawSurface
              && axisVarX >= 0
              && sourceSize[axisVarX] > 5
              && // arbitrary, but silly if few axis values
              !(axisVariables[axisVarX] instanceof EDVTimeStampGridAxis)
              && avStopIndex[axisVarX]
                  > avStartIndex[axisVarX]; // ascending (but calculations are index based)

      // If user clicked on map, change some of the avXxx[], lonXxx, latXxx values.
      partValue =
          String2.stringStartsWith(
              queryParts, partName = ".click=?"); // ? indicates user clicked on map
      if (zoomLatLon && partValue != null) {
        try {
          String xy[] = String2.split(partValue.substring(8), ','); // e.g., 24,116

          // read pngInfo file (if available, e.g., if graph is x=lon, y=lat and image recently
          // created)
          int clickPo = userDapQuery.indexOf("&.click=?");
          Object pngInfo[] = readPngInfo(loggedInAs, userDapQuery.substring(0, clickPo), ".png");
          int graphWESN[] = null;
          if (pngInfo != null) {
            graphWESN = (int[]) pngInfo[1];
            if (reallyVerbose)
              String2.log("  pngInfo graphWESN=" + String2.toCSSVString(graphWESN));
          }

          double clickLonLat[] =
              SgtUtil.xyToLonLat(
                  String2.parseInt(xy[0]),
                  String2.parseInt(xy[1]),
                  graphWESN, // graph int extent
                  new double[] { // graph double extent
                    Math.min(lonStart, lonStop), Math.max(lonStart, lonStop),
                    Math.min(latStart, latStop), Math.max(latStart, latStop)
                  },
                  new double[] { // data extent
                    Math.min(lonFirst, lonLast), Math.max(lonFirst, lonLast),
                    Math.min(latFirst, latLast), Math.max(latFirst, latLast)
                  });

          if (clickLonLat != null) {
            EDVGridAxis lonEdvga = axisVariables[lonIndex];
            EDVGridAxis latEdvga = axisVariables[latIndex];

            // get current radius, and shrink if clickLonLat is closer to data limits
            double radius =
                Math.max(Math.abs(lonStop - lonStart), Math.abs(latStart - latStop)) / 2;
            radius = Math.min(radius, clickLonLat[0] - lonEdvga.destinationMinDouble());
            radius = Math.min(radius, lonEdvga.destinationMaxDouble() - clickLonLat[0]);
            radius = Math.min(radius, clickLonLat[1] - latEdvga.destinationMinDouble());
            radius = Math.min(radius, latEdvga.destinationMaxDouble() - clickLonLat[1]);

            // if not too close to data's limits...  success
            if (radius >= 0.01) {
              int index0, index1;

              // lon
              index0 = lonEdvga.destinationToClosestIndex(clickLonLat[0] - radius);
              index1 = lonEdvga.destinationToClosestIndex(clickLonLat[0] + radius);
              if (!lonEdvga.isAscending()) {
                int ti = index0;
                index0 = index1;
                index1 = ti;
              }
              avStartIndex[lonIndex] = index0;
              avStopIndex[lonIndex] = index1;
              lonStart = lonEdvga.destinationValue(index0).getNiceDouble(0);
              lonStop = lonEdvga.destinationValue(index1).getNiceDouble(0);
              avStart[lonIndex] = String2.genEFormat6(lonStart);
              avStop[lonIndex] = String2.genEFormat6(lonStop);
              lonCenter = (lonStart + lonStop) / 2;
              lonRange = lonStop - lonStart;

              // lat
              index0 = latEdvga.destinationToClosestIndex(clickLonLat[1] - radius);
              index1 = latEdvga.destinationToClosestIndex(clickLonLat[1] + radius);
              if (!latEdvga.isAscending()) {
                int ti = index0;
                index0 = index1;
                index1 = ti;
              }
              avStartIndex[latIndex] = index0;
              avStopIndex[latIndex] = index1;
              latStart = latEdvga.destinationValue(index0).getNiceDouble(0);
              latStop = latEdvga.destinationValue(index1).getNiceDouble(0);
              avStart[latIndex] = String2.genEFormat6(latStart);
              avStop[latIndex] = String2.genEFormat6(latStop);
              latCenter = (latStart + latStop) / 2;
              latRange = latStop - latStart;
            }
          }
        } catch (Throwable t) {
          EDStatic.rethrowClientAbortException(t); // first thing in catch{}
          String2.log("Error while trying to read &.click? value.\n" + MustBe.throwableToString(t));
        }
      }

      // zoomTime (for timeseries graphs)
      int idealTimeN = -1; // 1..100
      int idealTimeUnits = -1;
      if (zoomTime) {

        // set idealTimeN (1..100), idealTimeUnits;
        partValue = String2.stringStartsWith(queryParts, ".timeRange=");
        if (partValue != null) {
          // try to read from url params
          String parts[] = String2.split(partValue.substring(11), ',');
          if (parts.length == 2) {
            idealTimeN = String2.parseInt(parts[0]);
            idealTimeUnits = Calendar2.IDEAL_UNITS_OPTIONS.indexOf(parts[1]);
          }
        }
        // if not set, find closest
        if (reallyVerbose)
          String2.log(
              "  setup zoomTime timeRange="
                  + timeRange
                  + " idealTimeN="
                  + idealTimeN
                  + " units="
                  + idealTimeUnits);
        if (idealTimeN < 1 || idealTimeN > 100 || idealTimeUnits < 0) {

          idealTimeUnits = Calendar2.IDEAL_UNITS_OPTIONS.size() - 1;
          while (idealTimeUnits > 0
              && timeRange < Calendar2.IDEAL_UNITS_SECONDS.get(idealTimeUnits)) {
            idealTimeUnits--;
            // String2.log("  selecting timeRange=" + timeRange + " timeUnits=" + idealTimeUnits);
          }
          idealTimeN =
              Math2.minMax(
                  1,
                  100,
                  Math2.roundToInt(timeRange / Calendar2.IDEAL_UNITS_SECONDS.get(idealTimeUnits)));
        }
        if (reallyVerbose)
          String2.log(
              "  idealTimeN+Units="
                  + idealTimeN
                  + " "
                  + Calendar2.IDEAL_UNITS_SECONDS.get(idealTimeUnits));

        // make idealized timeRange
        timeRange =
            idealTimeN * Calendar2.IDEAL_UNITS_SECONDS.get(idealTimeUnits); // sometimes too low
      }

      // show Graph Type choice
      writer.write(widgets.beginTable("class=\"compact nowrap\"")); // the Graph Type and vars table
      paramName = "draw";
      writer.write(
          "<tr>\n"
              + "  <td><strong>"
              + EDStatic.messages.magGraphTypeAr[language]
              + ":&nbsp;</strong>"
              + "  </td>\n"
              + "  <td>\n");
      writer.write(
          widgets.select(
              paramName,
              "",
              1,
              draws,
              draw,
              // change->submit so form always reflects graph type
              "onChange='mySubmit("
                  + (draw < 3
                      ? "f1.draw.selectedIndex<3"
                      : // if old and new draw are <3, do send var names
                      "false")
                  + // else don't send var names
                  ");'"));
      writer.write(
          " "
              + // spacer
              EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  "<div class=\"standard_max_width\">"
                      + EDStatic.messages.magGraphTypeTooltipGridAr[language]
                      + "</div>")
              + "  </td>\n"
              + "</tr>\n");

      // pick variables
      for (int v = 0; v < nVars; v++) {
        String tvNames[] = varOptions[v];
        int vi = String2.indexOf(tvNames, varName[v]);
        // String2.log(">>v=" + v + " vi=" + vi + " varName[v]=" + varName[v] + " opts=" +
        // String2.toCSSVString(tvNames));
        if (vi < 0) {
          if (tvNames.length > v) {
            varName[v] = tvNames[v];
            vi = v;
          } else if (tvNames.length > 0) {
            varName[v] = tvNames[0];
            vi = 0;
          } else {
            throw new SimpleException(
                EDStatic.messages.errorInternalAr[0] + "No varOptions for v=" + v);
          }
        }
        // avoid duplicate with previous var
        // (there are never more than 2 axis or 2 data vars in a row)
        if (v >= 1 && varName[v - 1].equals(tvNames[vi])) vi = vi == 0 ? 1 : 0;
        varName[v] = vi < tvNames.length ? tvNames[vi] : "";

        paramName = "var" + v;
        writer.write("<tr>\n" + "  <td>" + varLabel[v] + "&nbsp;" + "  </td>\n" + "  <td>\n");
        writer.write(
            widgets.select(
                paramName,
                "",
                1,
                tvNames,
                vi,
                // change->submit so axisVar's showStartAndStop always reflects graph variables
                "onChange='mySubmit(true);'")); // true= send var names
        writer.write(
            " "
                + // spacer
                EDStatic.htmlTooltipImage(
                    request,
                    language,
                    loggedInAs,
                    MessageFormat.format(EDStatic.messages.magAxisVarHelpAr[language], varHelp[v])
                        + EDStatic.messages.magAxisVarHelpGridAr[language]));
        writer.write("""
                  </td>
                </tr>
                """);
      }

      // end the Graph Type and vars table
      writer.write(widgets.endTable());

      // *** write the Dimension Constraints table
      writer.write("&nbsp;\n"); // necessary for the blank line before start of table (not <p>)
      writer.write(widgets.beginTable("class=\"compact nowrap\" style=\"width:50%;\""));
      writer.write(
          "<tr>\n"
              + "  <th class=\"L\">"
              + EDStatic.messages.EDDGridDimensionRangesAr[language]
              + " "
              + EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  EDStatic.messages.EDDGridDimensionTooltipAr[0]
                      + "<br>"
                      + EDStatic.messages.EDDGridVarHasDimTooltipAr[language])
              + "</th>\n"
              + "  <th style=\"text-align:center;\">"
              + gap
              + EDStatic.messages.EDDGridStartAr[language]
              + " "
              + EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  EDStatic.messages.EDDGridDimensionTooltipAr[language]
                      + "<br>"
                      + EDStatic.messages.EDDGridStartStopTooltipAr[language]
                      + "<br>"
                      + EDStatic.messages.EDDGridStartTooltipAr[language])
              + "</th>\n"
              + "  <th style=\"text-align:center;\">"
              + gap
              + EDStatic.messages.EDDGridStopAr[0]
              + " "
              + EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  EDStatic.messages.EDDGridDimensionTooltipAr[0]
                      + "<br>"
                      + EDStatic.messages.EDDGridStartStopTooltipAr[language]
                      + "<br>"
                      + EDStatic.messages.EDDGridStopTooltipAr[language])
              + "</th>\n"
              + "</tr>\n");

      // show the dimension widgets
      for (int av = 0; av < nAv; av++) {
        EDVGridAxis edvga = axisVariables[av];
        String tFirst = edvga.destinationToString(edvga.firstDestinationValue());
        String edvgaTooltip = edvga.htmlRangeTooltip(language);

        String tUnits = edvga instanceof EDVTimeStampGridAxis ? "UTC" : edvga.units();
        tUnits = tUnits == null ? "" : "(" + tUnits + ") ";
        writer.write(
            "<tr>\n"
                + "  <td>"
                + edvga.destinationName()
                + " "
                + tUnits
                + EDStatic.htmlTooltipImageEDVGA(request, language, loggedInAs, edvga)
                + "</td>\n");

        for (int ss = 0; ss < 2; ss++) { // 0=start, 1=stop
          paramName = (ss == 0 ? "start" : "stop") + av;
          int tIndex = ss == 0 ? avStartIndex[av] : avStopIndex[av];
          writer.write("<td>"); // a cell in the dimensions table

          if (ss == 1 || showStartAndStopFields[av]) {
            // show start or stop field, in a table with buttons

            // generate the buttons
            int fieldSize = 24;
            StringBuilder buttons = new StringBuilder();

            // show arrowLL?
            if (tIndex >= 1 && (ss == 0 || (ss == 1 && !showStartAndStopFields[av]))) {
              buttons.append(
                  "<td class=\"B\">\n"
                      + HtmlWidgets.htmlTooltipImage(
                          EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowLL.gif",
                          "|<",
                          EDStatic.messages.magItemFirstAr[language],
                          "class=\"B\" "
                              + // vertical-align: 'b'ottom
                              "onMouseUp='f1."
                              + paramName
                              + ".value=\""
                              + tFirst
                              + "\"; mySubmit(true);'")
                      + "</td>\n");
              fieldSize -= 2;
            }
            // show -?
            if (tIndex >= 1
                && (ss == 0
                    || (ss == 1
                        && (!showStartAndStopFields[av]
                            || avStartIndex[av] + 1 < avStopIndex[av])))) {
              // bug: wrong direction if source alt values are ascending depth values
              String ts =
                  edvga.destinationToString(edvga.destinationValue(tIndex - 1).getNiceDouble(0));
              buttons.append(
                  "<td class=\"B\">\n"
                      + HtmlWidgets.htmlTooltipImage(
                          EDStatic.imageDirUrl(request, loggedInAs, language) + "minus.gif",
                          "-",
                          EDStatic.messages.magItemPreviousAr[language],
                          "class=\"B\" "
                              + // vertical-align: 'b'ottom
                              "onMouseUp='f1."
                              + paramName
                              + ".value=\""
                              + ts
                              + "\"; mySubmit(true);'")
                      + "</td>\n");
              fieldSize -= 1;
            }
            // show +?
            if (tIndex < sourceSize[av] - 1
                && ((ss == 0 && avStartIndex[av] + 1 < avStopIndex[av]) || ss == 1)) {
              String ts =
                  edvga.destinationToString(edvga.destinationValue(tIndex + 1).getNiceDouble(0));
              buttons.append(
                  "<td class=\"B\">\n"
                      + HtmlWidgets.htmlTooltipImage(
                          EDStatic.imageDirUrl(request, loggedInAs, language) + "plus.gif",
                          "+",
                          EDStatic.messages.magItemNextAr[language],
                          "class=\"B\" "
                              + // vertical-align: 'b'ottom
                              "onMouseUp='f1."
                              + paramName
                              + ".value=\""
                              + ts
                              + "\"; mySubmit(true);'")
                      + "</td>\n");
              fieldSize -= 1;
            }
            // show arrowRR?
            if (ss == 1 && (tIndex < sourceSize[av] - 1 || (av == 0 && updateEveryNMillis > 0))) {
              buttons.append(
                  "<td class=\"B\">\n"
                      + HtmlWidgets.htmlTooltipImage(
                          EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowRR.gif",
                          ">|",
                          EDStatic.messages.magItemLastAr[language],
                          // the word "last" works for all datasets
                          // and works better than tLast for updateEveryNMillis datasets
                          "class=\"B\" "
                              + // vertical-align: 'b'ottom
                              "onMouseUp='f1."
                              + paramName
                              + ".value=\"last\"; mySubmit(true);'")
                      + "</td>\n");
              fieldSize -= 2;
            }

            // show start or stop field
            writer.write(
                widgets.beginTable(
                    "class=\"compact nowrap\" style=\"width:10%;\"")); // keep it small
            writer.write("<tr><td>" + gap);
            writer.write(
                widgets.textField(
                    paramName,
                    edvgaTooltip,
                    fieldSize,
                    255,
                    ss == 0 ? avStart[av] : avStop[av],
                    ""));
            writer.write("</td>\n" + buttons + "</tr>\n");
            writer.write(widgets.endTable());
          } else {
            writer.write(
                gap
                    + "<span class=\"subduedColor\">&nbsp;"
                    + EDStatic.messages.magJust1ValueAr[language]
                    + "</span>\n");
            writer.write(widgets.hidden(paramName, "SeeStop"));
          }
          writer.write("  </td>\n");
        }

        // add avStart avStop to constraints
        if (showStartAndStopFields[av])
          constraints.append("%5B(" + avStart[av] + "):(" + avStop[av] + ")%5D");
        else constraints.append("%5B(" + avStop[av] + ")%5D");

        // *** and a slider for this axis    (Make A Graph)
        sliderFromNames[av] = formName + ".start" + av;
        sliderToNames[av] = formName + ".stop" + av;
        sliderUserValuesCsvs[av] = edvga.sliderCsvValues();
        int safeSourceSize1 = Math.max(1, sourceSize[av] - 1);
        if (showStartAndStopFields[av]) {
          // 2 thumbs
          sliderNThumbs[av] = 2;
          sliderInitFromPositions[av] =
              Math2.roundToInt((avStartIndex[av] * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
          sliderInitToPositions[av] =
              sourceSize[av] == 1
                  ? EDV.SLIDER_PIXELS - 1
                  : Math2.roundToInt(
                      (avStopIndex[av] * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
          writer.write(
              "<tr>\n"
                  + "  <td colspan=\"3\" class=\"N\">\n"
                  + widgets.dualSlider(av, EDV.SLIDER_PIXELS - 1, "")
                  + // was "style=\"text-align:left;\"") +
                  "  </td>\n"
                  + "</tr>\n");
        } else {
          // 1 thumb
          sliderNThumbs[av] = 1;
          sliderFromNames[av] = formName + ".stop" + av; // change from default
          sliderInitFromPositions[av] =
              Math2.roundToInt((avStopIndex[av] * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
          sliderInitToPositions[av] = EDV.SLIDER_PIXELS - 1;
          writer.write(
              "<tr>\n"
                  + "  <td colspan=\"3\" class=\"N\">\n"
                  + widgets.slider(av, EDV.SLIDER_PIXELS - 1, "")
                  + // was "style=\"text-align:left\"") +
                  "  </td>\n"
                  + "</tr>\n");
        }
      }
      // end of dimensions constraints table
      writer.write(widgets.endTable());

      // *** make graphQuery
      StringBuilder graphQuery = new StringBuilder();
      // add data varNames and constraints
      for (int v = 1; v < nVars; v++) {
        if (String2.indexOf(dvNames, varName[v]) >= 0) {
          if (graphQuery.length() > 0) graphQuery.append(",");
          graphQuery.append(varName[v] + constraints);
        }
      }

      // set hidden time range widgets;    zoomTime (for timeseries graphs)
      if (zoomTime) {
        writer.write(
            widgets.hidden("timeN", "" + idealTimeN)
                + widgets.hidden("timeUnits", Calendar2.IDEAL_UNITS_OPTIONS.get(idealTimeUnits)));
      }

      // add .draw and .vars to graphQuery
      graphQuery.append(
          "&.draw="
              + draws[draw]
              + "&.vars="
              + SSR.minimalPercentEncode(
                  varName[0]
                      + "|"
                      + varName[1]
                      + "|"
                      + varName[2]
                      + (nVars > 3 ? "|" + varName[3] : "")));

      // *** Graph Settings
      writer.write("&nbsp;\n"); // necessary for the blank line before start of table (not <p>)
      writer.write(widgets.beginTable("class=\"compact nowrap\""));
      writer.write(
          "  <tr><th class=\"L\" colspan=\"6\">"
              + EDStatic.messages.magGSAr[language]
              + "</th></tr>\n");
      if (drawLinesAndMarkers || drawMarkers) {
        // get Marker settings
        int mType = -1, mSize = -1;
        partValue = String2.stringStartsWith(queryParts, partName = ".marker=");
        if (partValue != null) {
          pParts = String2.split(partValue.substring(partName.length()), '|');
          if (pParts.length > 0) mType = String2.parseInt(pParts[0]);
          if (pParts.length > 1) mSize = String2.parseInt(pParts[1]); // the literal, not the index
          if (reallyVerbose) String2.log(".marker type=" + mType + " size=" + mSize);
        }

        // markerType
        paramName = "mType";
        if (mType < 0 || mType >= GraphDataLayer.MARKER_TYPES.size())
          mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
        // if (!yIsAxisVar && varName[2].length() > 0 &&
        //    GraphDataLayer.MARKER_TYPES[mType].toLowerCase().indexOf("filled") < 0)
        //    mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE; //needs "filled" marker type
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.magGSMarkerTypeAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(widgets.select(paramName, "", 1, GraphDataLayer.MARKER_TYPES, mType, ""));
        writer.write("</td>\n");

        // markerSize
        paramName = "mSize";
        String mSizes[] = {
          "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18"
        };
        mSize =
            String2.indexOf(mSizes, "" + mSize); // convert from literal 3.. to index in mSizes[0..]
        if (mSize < 0) mSize = String2.indexOf(mSizes, "" + GraphDataLayer.MARKER_SIZE_SMALL);
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.magGSSizeAr[language]
                + ":&nbsp;</td>"
                + "    <td>");
        writer.write(widgets.select(paramName, "", 1, mSizes, mSize, ""));
        writer.write(
            """
                </td>
                    <td></td>
                    <td></td>
                  </tr>
                """);

        // add to graphQuery
        graphQuery.append("&.marker=" + SSR.minimalPercentEncode(mType + "|" + mSizes[mSize]));
      }

      if (drawLines || drawLinesAndMarkers || drawMarkers || drawSticks || drawVectors) {

        // color
        paramName = "colr"; // not color, to avoid possible conflict
        partValue = String2.stringStartsWith(queryParts, partName = ".color=0x");
        int colori =
            HtmlWidgets.PALETTE17.indexOf(
                partValue == null ? "" : partValue.substring(partName.length()));
        if (colori < 0) colori = HtmlWidgets.PALETTE17.indexOf("000000");
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.magGSColorAr[language]
                + ":&nbsp;</td>\n"
                + "    <td colspan=\"5\">");
        writer.write(widgets.color17("", paramName, "", colori, ""));
        writer.write("""
                </td>
                  </tr>
                """);

        // add to graphQuery
        graphQuery.append("&.color=0x" + HtmlWidgets.PALETTE17.get(colori));
      }

      if (drawLinesAndMarkers || drawMarkers || drawSurface) {
        // color bar
        partValue = String2.stringStartsWith(queryParts, partName = ".colorBar=");
        pParts =
            partValue == null
                ? new String[0]
                : String2.split(partValue.substring(partName.length()), '|');
        if (reallyVerbose) String2.log(".colorBar=" + String2.toCSSVString(pParts));

        paramName = "p";
        String defaultPalette = "";
        // String2.log("defaultPalette=" + defaultPalette + " pParts.length=" + pParts.length);
        int palette =
            Math.max(
                0,
                String2.indexOf(
                    EDStatic.messages.palettes0, pParts.length > 0 ? pParts[0] : defaultPalette));
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.magGSColorBarAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.select(
                paramName,
                EDStatic.messages.magGSColorBarTooltipAr[language],
                1,
                EDStatic.messages.palettes0,
                palette,
                ""));
        writer.write("</td>\n");

        String conDis[] = new String[] {"", "Continuous", "Discrete"};
        paramName = "pc";
        int continuous =
            pParts.length > 1 ? (pParts[1].equals("D") ? 2 : pParts[1].equals("C") ? 1 : 0) : 0;
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.magGSContinuityAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.select(
                paramName,
                EDStatic.messages.magGSContinuityTooltipAr[language],
                1,
                conDis,
                continuous,
                ""));
        writer.write("</td>\n");

        paramName = "ps";
        String defaultScale = "";
        int scale =
            Math.max(0, EDV.VALID_SCALES0.indexOf(pParts.length > 2 ? pParts[2] : defaultScale));
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.magGSScaleAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.select(
                paramName,
                EDStatic.messages.magGSScaleTooltipAr[language],
                1,
                EDV.VALID_SCALES0,
                scale,
                ""));
        writer.write("""
                </td>
                  </tr>
                """);

        // new row
        paramName = "pMin";
        String defaultMin = "";
        String palMin = pParts.length > 3 ? pParts[3] : defaultMin;
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + gap
                + EDStatic.messages.magGSMinAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.textField(
                paramName, EDStatic.messages.magGSMinTooltipAr[language], 10, 60, palMin, ""));
        writer.write("</td>\n");

        paramName = "pMax";
        String defaultMax = "";
        String palMax = pParts.length > 4 ? pParts[4] : defaultMax;
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.magGSMaxAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.textField(
                paramName, EDStatic.messages.magGSMaxTooltipAr[language], 10, 60, palMax, ""));
        writer.write("</td>\n");

        paramName = "pSec";
        int pSections =
            Math.max(0, EDStatic.paletteSections.indexOf(pParts.length > 5 ? pParts[5] : ""));
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.magGSNSectionsAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.select(
                paramName,
                EDStatic.messages.magGSNSectionsTooltipAr[language],
                1,
                EDStatic.paletteSections,
                pSections,
                ""));
        writer.write("""
                </td>
                  </tr>
                """);

        // add to graphQuery
        graphQuery.append(
            "&.colorBar="
                + SSR.minimalPercentEncode(
                    EDStatic.messages.palettes0[palette]
                        + "|"
                        + (conDis[continuous].length() == 0 ? "" : conDis[continuous].charAt(0))
                        + "|"
                        + EDV.VALID_SCALES0.get(scale)
                        + "|"
                        + palMin
                        + "|"
                        + palMax
                        + "|"
                        + EDStatic.paletteSections.get(pSections)));
      }

      if (drawVectors) {

        // Vector Standard
        paramName = "vec";
        String vec = String2.stringStartsWith(queryParts, partName = ".vec=");
        vec = vec == null ? "" : vec.substring(partName.length());
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.magGSVectorStandardAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.textField(
                paramName,
                EDStatic.messages.magGSVectorStandardTooltipAr[language],
                10,
                30,
                vec,
                ""));
        writer.write(
            """
                        </td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                          </tr>
                        """);

        // add to graphQuery
        if (vec.length() > 0) graphQuery.append("&.vec=" + SSR.minimalPercentEncode(vec));
      }

      if (drawSurface && isMap) {
        // Draw Land
        int tLand = 0;
        partValue = String2.stringStartsWith(queryParts, partName = ".land=");
        if (partValue != null) {
          partValue = partValue.substring(6);
          tLand = Math.max(0, SgtMap.drawLandMask_OPTIONS.indexOf(partValue));
        }
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.magGSLandMaskAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.select(
                "land",
                EDStatic.messages.magGSLandMaskTooltipGridAr[language],
                1,
                SgtMap.drawLandMask_OPTIONS,
                tLand,
                ""));
        writer.write(
            """
                        </td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                        </tr>
                        """);

        // add to graphQuery
        if (tLand > 0) graphQuery.append("&.land=" + SgtMap.drawLandMask_OPTIONS.get(tLand));
      }

      // bgColor
      Color bgColor = EDStatic.config.graphBackgroundColor;
      String tBGColor = String2.stringStartsWith(queryParts, partName = ".bgColor=");
      if (tBGColor != null) {
        String tBGColorAr[] = String2.split(tBGColor.substring(partName.length()), '|');
        if (tBGColorAr.length > 0 && tBGColorAr[0].length() > 0) {
          bgColor = new Color(String2.parseInt(tBGColorAr[0]), true); // hasAlpha
        }
      }
      // always add .bgColor to graphQuery so this ERDDAP's setting overwrites remote ERDDAP's
      graphQuery.append("&.bgColor=" + String2.to0xHexString(bgColor.getRGB(), 8));

      // yRange
      String yRange[] = new String[] {"", ""};
      boolean yAscending = true;
      String yAxisScale = ""; // ""/Linear/Log
      if (true) { // ?? && !drawVector ??
        paramName = "yRange";
        String tyRange = String2.stringStartsWith(queryParts, partName = ".yRange=");
        if (tyRange != null) {
          String tyRangeAr[] = String2.split(tyRange.substring(partName.length()), '|');
          for (int i = 0; i < 2; i++) {
            if (tyRangeAr.length > i) {
              double td = String2.parseDouble(tyRangeAr[i]);
              yRange[i] = Double.isNaN(td) ? "" : String2.genEFormat10(td);
            }
          }
          if (tyRangeAr.length > 2)
            // if the param slot is there, the param determines Ascending
            yAscending = String2.parseBoolean(tyRangeAr[2]); // "" -> true(the default)
          if (tyRangeAr.length > 3) {
            // if the param slot is there, the param determines ""/Linear/Log
            yAxisScale = String2.toTitleCase(tyRangeAr[3].trim());
            if (!yAxisScale.equals("Linear") && !yAxisScale.equals("Log")) yAxisScale = "";
          }
        }
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.magGSYAxisMinAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.textField(
                "yRangeMin",
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.magGSYRangeMinTooltipAr[language]
                    + EDStatic.messages.magGSYRangeTooltipAr[language]
                    + "</div>",
                10,
                30,
                yRange[0],
                ""));
        writer.write(
            "</td>\n"
                + "    <td>&nbsp;"
                + EDStatic.messages.magGSYAxisMaxAr[language]
                + ":&nbsp;</td>\n"
                + "    <td>");
        writer.write(
            widgets.textField(
                "yRangeMax",
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.magGSYRangeMaxTooltipAr[language]
                    + EDStatic.messages.magGSYRangeTooltipAr[language]
                    + "</div>",
                10,
                30,
                yRange[1],
                ""));
        writer.write("</td>\n" + "    <td>&nbsp;");
        writer.write(
            widgets.select(
                "yRangeAscending",
                EDStatic.messages.magGSYAscendingTooltipAr[language],
                1,
                new String[] {"Ascending", "Descending"},
                yAscending ? 0 : 1,
                ""));
        writer.write(
            "</td>\n"
                + "    <td>"
                + widgets.select(
                    "yScale",
                    EDStatic.messages.magGSYScaleTooltipAr[language],
                    1,
                    new String[] {"", "Linear", "Log"},
                    yAxisScale.equals("Linear") ? 1 : yAxisScale.equals("Log") ? 2 : 0,
                    "")
                + "</td>\n"
                + "  </tr>\n");

        // add to graphQuery
        if (yRange[0].length() > 0
            || yRange[1].length() > 0
            || !yAscending
            || yAxisScale.length() > 0)
          graphQuery.append(
              "&.yRange="
                  + SSR.minimalPercentEncode(
                      yRange[0] + "|" + yRange[1] + "|" + yAscending + "|" + yAxisScale));
      }

      // *** end of form
      writer.write(widgets.endTable()); // end of Graph Settings table

      // make javascript function to generate query    \\x26=& %5B=[ %5D=] %7C=|
      writer.write(
          """
                      <script>\s
                      function makeQuery(varsToo) {\s
                        try {\s
                          var d = document;\s
                          var start, tv, c = "", q = "";\s
                      """); // c=constraint  q=query
      // gather constraints
      for (int av = 0; av < nAv; av++)
        writer.write(
            "    start = d.f1.start"
                + av
                + ".value; \n"
                + "    c += \"%5B\"; \n"
                + "    if (start != \"SeeStop\") c += \"(\" + start + \"):\"; \n"
                + // javascript uses !=, not !equals()
                "    c += \"(\" + d.f1.stop"
                + av
                + ".value + \")%5D\"; \n");
      // allow graph with 2 axes to swap axes
      if (varOptions[1] == avNames)
        writer.write(
            // if user changed var0 to var1, change var1 to var0
            "    if (d.f1.var0.selectedIndex == "
                + String2.indexOf(avNames, varName[1])
                + ") "
                + "d.f1.var1.selectedIndex = "
                + String2.indexOf(avNames, varName[0])
                + ";\n"
                +
                // if user changed var1 to var0, change var0 to var1
                "    if (d.f1.var1.selectedIndex == "
                + String2.indexOf(avNames, varName[0])
                + ") "
                + "d.f1.var0.selectedIndex = "
                + String2.indexOf(avNames, varName[1])
                + ";\n");
      // var[constraints],var[constraints]
      for (int v = 1; v < nVars; v++) {
        if (varOptions[v] == dvNames
            || varOptions[v] == dvNames0) { // simpler because advNames isn't an option
          writer.write(
              "    tv = d.f1.var"
                  + v
                  + ".options[d.f1.var"
                  + v
                  + ".selectedIndex].text; \n"
                  + "    if (tv.length > 0) { \n"
                  + "      if (q.length > 0) q += \",\"; \n"
                  + // javascript uses length, not length()
                  "      q += tv + c; \n"
                  + "    } \n");
        }
      }
      // graph settings
      writer.write("    q += \"&.draw=\" + d.f1.draw.options[d.f1.draw.selectedIndex].text; \n");
      writer.write(
          """
                          if (varsToo) {\s
                            q += "&.vars=" + d.f1.var0.options[d.f1.var0.selectedIndex].text +\s
                              "%7C" + d.f1.var1.options[d.f1.var1.selectedIndex].text;\s
                      """);
      if (nVars >= 3)
        writer.write("      q += \"%7C\" + d.f1.var2.options[d.f1.var2.selectedIndex].text; \n");
      if (nVars >= 4)
        writer.write("      q += \"%7C\" + d.f1.var3.options[d.f1.var3.selectedIndex].text; \n");
      writer.write("    } \n");
      if (drawLinesAndMarkers || drawMarkers)
        writer.write(
            """
                            q += "&.marker=" + d.f1.mType.selectedIndex + "%7C" +\s
                              d.f1.mSize.options[d.f1.mSize.selectedIndex].text;\s
                        """);
      if (drawLines || drawLinesAndMarkers || drawMarkers || drawSticks || drawVectors)
        writer.write(
            "    q += \"&.color=0x\"; \n"
                + "    for (var rb = 0; rb < "
                + HtmlWidgets.PALETTE17.size()
                + "; rb++) \n"
                + "      if (d.f1.colr[rb].checked) q += d.f1.colr[rb].value; \n"); // always: one
      // will be
      // checked
      if (drawLinesAndMarkers || drawMarkers || drawSurface)
        writer.write(
            """
                            var tpc = d.f1.pc.options[d.f1.pc.selectedIndex].text;
                            q += "&.colorBar=" + d.f1.p.options[d.f1.p.selectedIndex].text + "%7C" +\s
                              (tpc.length > 0? tpc.charAt(0) : "") + "%7C" +\s
                              d.f1.ps.options[d.f1.ps.selectedIndex].text + "%7C" +\s
                              d.f1.pMin.value + "%7C" + d.f1.pMax.value + "%7C" +\s
                              d.f1.pSec.options[d.f1.pSec.selectedIndex].text;\s
                        """);
      if (drawVectors)
        writer.write("    if (d.f1.vec.value.length > 0) q += \"&.vec=\" + d.f1.vec.value; \n");
      if (drawSurface && isMap)
        writer.write(
            "    if (d.f1.land.selectedIndex > 0) "
                + "q += \"&.land=\" + d.f1.land.options[d.f1.land.selectedIndex].text; \n");
      if (true)
        writer.write(
            """
                            var yRMin=d.f1.yRangeMin.value;\s
                            var yRMax=d.f1.yRangeMax.value;\s
                            var yRAsc=d.f1.yRangeAscending.selectedIndex;\s
                            var yScl =d.f1.yScale.options[d.f1.yScale.selectedIndex].text;\s
                            if (yRMin.length > 0 || yRMax.length > 0 || yRAsc == 1 || yScl.length > 0)
                              q += "\\x26.yRange=" + yRMin + "%7C" + yRMax + "%7C" + (yRAsc==0) + "%7C" + yScl;\s
                        """);
      if (zoomTime)
        writer.write(
            "    q += \"&.timeRange=\" + d.f1.timeN.value + \",\" + d.f1.timeUnits.value; \n");
      // always add .bgColor to graphQuery so this ERDDAP's setting overwrites remote ERDDAP's
      writer.write("    q += \"&.bgColor=" + String2.to0xHexString(bgColor.getRGB(), 8) + "\"; \n");
      writer.write(
          "    return q; \n"
              + "  } catch (e) { \n"
              + "    alert(e); \n"
              + "    return \"\"; \n"
              + "  } \n"
              + "} \n"
              + "function mySubmit(varsToo) { \n"
              + "  var q = makeQuery(varsToo); \n"
              + "  if (q.length > 0) window.location=\""
              + // javascript uses length, not length()
              tErddapUrl
              + "/griddap/"
              + datasetID
              + ".graph?\" + q;\n"
              + "} \n"
              + "</script> \n");

      // submit
      writer.write("&nbsp;\n"); // necessary for the blank line before start of table (not <p>)
      writer.write(widgets.beginTable("class=\"compact\""));
      writer.write("<tr><td>");
      writer.write(
          widgets.htmlButton(
              "button",
              "",
              "",
              EDStatic.messages.magRedrawTooltipAr[language],
              "<span style=\"font-size:large;\"><strong>"
                  + EDStatic.messages.magRedrawAr[language]
                  + "</strong></span>",
              "onMouseUp='mySubmit(true);'"));
      writer.write(" " + EDStatic.messages.patientDataAr[language] + "\n" + "</td></tr>\n");

      // Download the Data
      writer.write(
          "<tr><td>&nbsp;<br>"
              + EDStatic.messages.optionalAr[language]
              + ":"
              + "<br>"
              + EDStatic.messages.magFileTypeAr[language]
              + ":\n");
      paramName = "fType";
      boolean tAccessibleTo = isAccessibleTo(EDStatic.getRoles(loggedInAs));
      List<String> fileTypeOptions =
          tAccessibleTo
              ? EDD_FILE_TYPE_INFO.values().stream()
                  .filter(fileTypeInfo -> fileTypeInfo.getAvailableGrid())
                  .map(fileTypeInfo -> fileTypeInfo.getFileTypeName())
                  .toList()
              : publicGraphFileTypeNames;
      int defaultIndex =
          fileTypeOptions.indexOf(
              tAccessibleTo ? defaultFileTypeOption : defaultPublicGraphFileTypeOption);
      writer.write(
          widgets.select(
              paramName,
              EDStatic.messages.EDDSelectFileTypeAr[language],
              1,
              fileTypeOptions,
              defaultIndex,
              "onChange='f1.tUrl.value=\""
                  + tErddapUrl
                  + "/griddap/"
                  + datasetID
                  + "\" + f1.fType.options[f1.fType.selectedIndex].text + "
                  + "\"?"
                  + String2.replaceAll(graphQuery.toString(), "&", "&amp;")
                  + "\";'"));
      writer.write(
          " (<a rel=\"help\" href=\""
              + tErddapUrl
              + "/griddap/documentation.html#fileType\">"
              + EDStatic.messages.EDDFileTypeInformationAr[language]
              + "</a>)\n");

      writer.write("<br>and\n");
      writer.write(
          widgets.button(
              "button",
              "",
              EDStatic.messages.magDownloadTooltipAr[language]
                  + "<br>"
                  + EDStatic.messages.patientDataAr[language],
              EDStatic.messages.magDownloadAr[language],
              // "class=\"skinny\" " + //only IE needs it but only IE ignores it
              "onMouseUp='window.location=\""
                  + tErddapUrl
                  + "/griddap/"
                  + datasetID
                  + "\" + f1."
                  + paramName
                  + ".options[f1."
                  + paramName
                  + ".selectedIndex].text + \"?"
                  + XML.encodeAsHTML(graphQuery.toString())
                  + "\";'")); // or open a new window: window.open(result);\n" +
      writer.write("</td></tr>\n");

      // view the url
      String genViewHtml =
          String2.replaceAll(
              "<div class=\"standard_max_width\">"
                  + EDStatic.messages.justGenerateAndViewGraphUrlTooltipAr[language]
                  + "</div>",
              "&protocolName;",
              dapProtocol);
      writer.write("<tr><td>" + EDStatic.messages.magViewUrlAr[language] + ":\n");
      writer.write(
          widgets.textField(
              "tUrl",
              genViewHtml,
              60,
              1000,
              tErddapUrl
                  + "/griddap/"
                  + datasetID
                  + (tAccessibleTo ? defaultFileTypeOption : defaultPublicGraphFileTypeOption)
                  + "?"
                  + graphQuery.toString(),
              ""));
      writer.write(
          "<br>(<a rel=\"help\" href=\""
              + tErddapUrl
              + "/griddap/documentation.html\" "
              + "title=\"griddap documentation\">"
              + EDStatic.messages.magDocumentationAr[language]
              + "</a>\n"
              + EDStatic.htmlTooltipImage(request, language, loggedInAs, genViewHtml)
              + ")\n");
      writer.write("""
              </td></tr>
              </table>

              """);

      // end form
      writer.write(widgets.endForm());

      // *** end of left half of big table
      writer.write(
          "</td>\n"
              + "<td>"
              + gap
              + "</td>\n"
              + // gap in center
              "<td class=\"L T\">\n"); // begin right half of web page
      // writer.write(
      //    "</div>" + //end leftContent
      //    "<div class=\"rightContent\">");

      // *** zoomLatLon stuff
      if (zoomLatLon) {
        writer.write(
            EDStatic.messages.magZoomCenterAr[language]
                + "\n"
                + EDStatic.htmlTooltipImage(
                    request,
                    language,
                    loggedInAs,
                    EDStatic.messages.magZoomCenterTooltipAr[language])
                + "<br><strong>"
                + EDStatic.messages.magZoomAr[language]
                + ":</strong>\n");

        double cRadius =
            Math.max(Math.abs(lonRange), Math.abs(latRange)) / 2; // will get to max eventually
        double zoomOut = cRadius * 5 / 4;
        double zoomOut2 = cRadius * 2;
        double zoomOut8 = cRadius * 8;
        double zoomIn = cRadius * 4 / 5;
        double zoomIn2 = cRadius / 2;
        double zoomIn8 = cRadius / 8;

        // zoom out?
        boolean disableZoomOut =
            Math2.almostEqual(9, lonFirst, lonStart)
                && Math2.almostEqual(9, latFirst, latStart)
                && Math2.almostEqual(9, lonLast, lonStop)
                && Math2.almostEqual(9, latLast, latStop);

        writer.write(
            // HtmlWidgets.htmlTooltipImage(
            //    EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowDD.gif",
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.magZoomOutTooltipAr[language],
                    EDStatic.messages.magZoomOutDataAr[language]),
                EDStatic.messages.magZoomDataAr[language],
                "class=\"skinny\" "
                    + (disableZoomOut
                        ? "disabled"
                        : "onMouseUp='f1.start"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(lonFirst)
                            + "\"; "
                            + "f1.stop"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(lonLast)
                            + "\"; "
                            + "f1.start"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(latFirst)
                            + "\"; "
                            + "f1.stop"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(latLast)
                            + "\"; "
                            + "mySubmit(true);'")));

        // if zoom out, keep ranges intact by moving tCenter to safe place
        double tLonCenter, tLatCenter;
        tLonCenter = Math.max(lonCenter, Math.min(lonFirst, lonLast) + zoomOut8);
        tLonCenter = Math.min(tLonCenter, Math.max(lonFirst, lonLast) - zoomOut8);
        tLatCenter = Math.max(latCenter, Math.min(latFirst, latLast) + zoomOut8);
        tLatCenter = Math.min(tLatCenter, Math.max(latFirst, latLast) - zoomOut8);
        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(EDStatic.messages.magZoomOutTooltipAr[language], "8x"),
                MessageFormat.format(EDStatic.messages.magZoomOutAr[language], "8x"),
                "class=\"skinny\" "
                    + (disableZoomOut
                        ? "disabled"
                        : "onMouseUp='f1.start"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(tLonCenter - lonAscending * zoomOut8)
                            + "\"; "
                            + "f1.stop"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(tLonCenter + lonAscending * zoomOut8)
                            + "\"; "
                            + "f1.start"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(tLatCenter - latAscending * zoomOut8)
                            + "\"; "
                            + "f1.stop"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(tLatCenter + latAscending * zoomOut8)
                            + "\"; "
                            + "mySubmit(true);'")));

        // if zoom out, keep ranges intact by moving tCenter to safe place
        tLonCenter = Math.max(lonCenter, Math.min(lonFirst, lonLast) + zoomOut2);
        tLonCenter = Math.min(tLonCenter, Math.max(lonFirst, lonLast) - zoomOut2);
        tLatCenter = Math.max(latCenter, Math.min(latFirst, latLast) + zoomOut2);
        tLatCenter = Math.min(tLatCenter, Math.max(latFirst, latLast) - zoomOut2);
        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(EDStatic.messages.magZoomOutTooltipAr[language], "2x"),
                MessageFormat.format(EDStatic.messages.magZoomOutAr[language], "2x"),
                "class=\"skinny\" "
                    + (disableZoomOut
                        ? "disabled"
                        : "onMouseUp='f1.start"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(tLonCenter - lonAscending * zoomOut2)
                            + "\"; "
                            + "f1.stop"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(tLonCenter + lonAscending * zoomOut2)
                            + "\"; "
                            + "f1.start"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(tLatCenter - latAscending * zoomOut2)
                            + "\"; "
                            + "f1.stop"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(tLatCenter + latAscending * zoomOut2)
                            + "\"; "
                            + "mySubmit(true);'")));

        // if zoom out, keep ranges intact by moving tCenter to safe place
        tLonCenter = Math.max(lonCenter, Math.min(lonFirst, lonLast) + zoomOut);
        tLonCenter = Math.min(tLonCenter, Math.max(lonFirst, lonLast) - zoomOut);
        tLatCenter = Math.max(latCenter, Math.min(latFirst, latLast) + zoomOut);
        tLatCenter = Math.min(tLatCenter, Math.max(latFirst, latLast) - zoomOut);
        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.magZoomOutTooltipAr[language],
                    EDStatic.messages.magZoomALittleAr[language]),
                MessageFormat.format(EDStatic.messages.magZoomOutAr[language], "").trim(),
                "class=\"skinny\" "
                    + (disableZoomOut
                        ? "disabled"
                        : "onMouseUp='f1.start"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(tLonCenter - lonAscending * zoomOut)
                            + "\"; "
                            + "f1.stop"
                            + lonIndex
                            + ".value=\""
                            + String2.genEFormat6(tLonCenter + lonAscending * zoomOut)
                            + "\"; "
                            + "f1.start"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(tLatCenter - latAscending * zoomOut)
                            + "\"; "
                            + "f1.stop"
                            + latIndex
                            + ".value=\""
                            + String2.genEFormat6(tLatCenter + latAscending * zoomOut)
                            + "\"; "
                            + "mySubmit(true);'")));

        // zoom in     Math.max moves rectangular maps toward square
        writer.write(
            widgets.button(
                    "button",
                    "",
                    MessageFormat.format(
                        EDStatic.messages.magZoomInTooltipAr[language],
                        EDStatic.messages.magZoomALittleAr[language]),
                    MessageFormat.format(EDStatic.messages.magZoomInAr[language], "").trim(),
                    "class=\"skinny\" "
                        + "onMouseUp='f1.start"
                        + lonIndex
                        + ".value=\""
                        + String2.genEFormat6(lonCenter - lonAscending * zoomIn)
                        + "\"; "
                        + "f1.stop"
                        + lonIndex
                        + ".value=\""
                        + String2.genEFormat6(lonCenter + lonAscending * zoomIn)
                        + "\"; "
                        + "f1.start"
                        + latIndex
                        + ".value=\""
                        + String2.genEFormat6(latCenter - latAscending * zoomIn)
                        + "\"; "
                        + "f1.stop"
                        + latIndex
                        + ".value=\""
                        + String2.genEFormat6(latCenter + latAscending * zoomIn)
                        + "\"; "
                        + "mySubmit(true);'")
                + widgets.button(
                    "button",
                    "",
                    MessageFormat.format(EDStatic.messages.magZoomInTooltipAr[language], "2x"),
                    MessageFormat.format(EDStatic.messages.magZoomInAr[language], "2x"),
                    "class=\"skinny\" "
                        + "onMouseUp='f1.start"
                        + lonIndex
                        + ".value=\""
                        + String2.genEFormat6(lonCenter - lonAscending * zoomIn2)
                        + "\"; "
                        + "f1.stop"
                        + lonIndex
                        + ".value=\""
                        + String2.genEFormat6(lonCenter + lonAscending * zoomIn2)
                        + "\"; "
                        + "f1.start"
                        + latIndex
                        + ".value=\""
                        + String2.genEFormat6(latCenter - latAscending * zoomIn2)
                        + "\"; "
                        + "f1.stop"
                        + latIndex
                        + ".value=\""
                        + String2.genEFormat6(latCenter + latAscending * zoomIn2)
                        + "\"; "
                        + "mySubmit(true);'")
                + widgets.button(
                    "button",
                    "",
                    MessageFormat.format(EDStatic.messages.magZoomInTooltipAr[language], "8x"),
                    MessageFormat.format(EDStatic.messages.magZoomInAr[language], "8x"),
                    "class=\"skinny\" "
                        + "onMouseUp='f1.start"
                        + lonIndex
                        + ".value=\""
                        + String2.genEFormat6(lonCenter - lonAscending * zoomIn8)
                        + "\"; "
                        + "f1.stop"
                        + lonIndex
                        + ".value=\""
                        + String2.genEFormat6(lonCenter + lonAscending * zoomIn8)
                        + "\"; "
                        + "f1.start"
                        + latIndex
                        + ".value=\""
                        + String2.genEFormat6(latCenter - latAscending * zoomIn8)
                        + "\"; "
                        + "f1.stop"
                        + latIndex
                        + ".value=\""
                        + String2.genEFormat6(latCenter + latAscending * zoomIn8)
                        + "\"; "
                        + "mySubmit(true);'"));

        // trailing <br>
        writer.write("<br>");
      }

      // *** zoomTime stuff  (for timeseries graphs)
      if (zoomTime) {
        if (reallyVerbose)
          String2.log(
              "zoomTime range="
                  + Calendar2.elapsedTimeString(timeRange * 1000)
                  + " center="
                  + Calendar2.epochSecondsToLimitedIsoStringT(time_precision, timeCenter, "")
                  + "\n  first="
                  + Calendar2.epochSecondsToLimitedIsoStringT(time_precision, timeFirst, "")
                  + "  start="
                  + Calendar2.epochSecondsToLimitedIsoStringT(time_precision, timeStart, "")
                  + "\n   last="
                  + Calendar2.epochSecondsToLimitedIsoStringT(time_precision, timeLast, "")
                  + "   stop="
                  + Calendar2.epochSecondsToLimitedIsoStringT(time_precision, timeStop, ""));

        writer.write("<strong>" + EDStatic.messages.magTimeRangeAr[language] + "</strong>\n");

        String timeRangeString =
            idealTimeN + " " + Calendar2.IDEAL_UNITS_OPTIONS.get(idealTimeUnits);
        String timesVary = "<br>(" + EDStatic.messages.magTimesVaryAr[language] + ")";
        String timeRangeTip =
            EDStatic.messages.magTimeRangeTooltipAr[language]
                + EDStatic.messages.magTimeRangeTooltip2Ar[language];
        String timeGap = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n";

        // n = 1..100
        writer.write(
            widgets.select(
                "timeN",
                timeRangeTip, // keep using widgets even though this isn't part f1 form
                1,
                Calendar2.IDEAL_N_OPTIONS,
                idealTimeN - 1, // -1 so index=0 .. 99
                "onChange='f1.timeN.value=this.options[this.selectedIndex].text; "
                    + "mySubmit(true);'"));

        // timeUnits
        writer.write(
            widgets.select(
                "timeUnits",
                timeRangeTip, // keep using widgets even though this isn't part f1 form
                1,
                Calendar2.IDEAL_UNITS_OPTIONS,
                idealTimeUnits,
                "onChange='f1.timeUnits.value=this.options[this.selectedIndex].text; "
                    + "mySubmit(true);'"));

        // make idealized current centered time period
        GregorianCalendar idMinGc =
            Calendar2.roundToIdealGC(timeCenter, idealTimeN, idealTimeUnits);
        // if it rounded to later time period, shift to earlier time period
        long roundedTime = idMinGc.getTimeInMillis() / 1000;
        if (roundedTime > timeCenter)
          idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
        GregorianCalendar idMaxGc = Calendar2.newGCalendarZulu(idMinGc.getTimeInMillis());
        idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);

        // time back
        {
          // make idealized beginning time
          GregorianCalendar tidMinGc =
              Calendar2.roundToIdealGC(timeFirst, idealTimeN, idealTimeUnits);
          // if it rounded to later time period, shift to earlier time period
          long roundedTimeTid = tidMinGc.getTimeInMillis() / 1000;
          if (roundedTimeTid > timeFirst)
            tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
          GregorianCalendar tidMaxGc = Calendar2.newGCalendarZulu(tidMinGc.getTimeInMillis());
          tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);

          // always show LL button if idealTime is different from current selection
          double idRange =
              Math2.divideNoRemainder(
                  tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis(), 1000);
          double ratio = (timeStop - timeStart) / idRange;
          if (timeStart > timeFirst || ratio < 0.99 || ratio > 1.01) {
            writer.write(
                "&nbsp;"
                    + HtmlWidgets.htmlTooltipImage(
                        EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowLL.gif",
                        "|<",
                        MessageFormat.format(
                                EDStatic.messages.magTimeRangeFirstAr[language], timeRangeString)
                            + timesVary,
                        "class=\"B\" "
                            + // vertical-align: 'b'ottom
                            "onMouseUp='f1.start"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, tidMinGc)
                            + "\"; "
                            + "f1.stop"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, tidMaxGc)
                            + "\"; "
                            + "mySubmit(true);'"));
          } else {
            writer.write(timeGap);
          }

          // idealized (rounded) time shift to left
          // (show based on more strict circumstances than LL (since relative shift, not absolute))
          if (timeStart > timeFirst) {
            idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
            idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
            writer.write(
                "&nbsp;"
                    + HtmlWidgets.htmlTooltipImage(
                        EDStatic.imageDirUrl(request, loggedInAs, language) + "minus.gif",
                        "-",
                        MessageFormat.format(
                                EDStatic.messages.magTimeRangeBackAr[language], timeRangeString)
                            + timesVary,
                        "class=\"B\" "
                            + // vertical-align: 'b'ottom
                            "onMouseUp='f1.start"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, idMinGc)
                            + "\"; "
                            + "f1.stop"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, idMaxGc)
                            + "\"; "
                            + "mySubmit(true);'"));
            idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
            idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);

          } else {
            writer.write(timeGap);
          }
        }

        // time forward
        {
          // show right button
          // (show based on more strict circumstances than RR (since relative shift, not absolute))
          if (timeStop < timeLast) {
            // idealized (rounded) time shift to right
            idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
            idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
            writer.write(
                "&nbsp;"
                    + HtmlWidgets.htmlTooltipImage(
                        EDStatic.imageDirUrl(request, loggedInAs, language) + "plus.gif",
                        "+",
                        MessageFormat.format(
                                EDStatic.messages.magTimeRangeForwardAr[language], timeRangeString)
                            + timesVary,
                        "class=\"B\" "
                            + // vertical-align: 'b'ottom
                            "onMouseUp='f1.start"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, idMinGc)
                            + "\"; "
                            + "f1.stop"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, idMaxGc)
                            + "\"; "
                            + "mySubmit(true);'"));
            idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
            idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
          } else {
            writer.write(timeGap);
          }

          // make idealized end time
          GregorianCalendar tidMaxGc =
              Calendar2.roundToIdealGC(timeLast, idealTimeN, idealTimeUnits);
          // if it rounded to earlier time period, shift to later time period
          if (Math2.divideNoRemainder(tidMaxGc.getTimeInMillis(), 1000) < timeLast)
            tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
          GregorianCalendar tidMinGc = Calendar2.newGCalendarZulu(tidMaxGc.getTimeInMillis());
          tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);

          // end time
          // always show RR button if idealTime is different from current selection
          double idRange =
              Math2.divideNoRemainder(
                  tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis(), 1000);
          double ratio = (timeStop - timeStart) / idRange;
          if (timeStop < timeLast || ratio < 0.99 || ratio > 1.01) {
            writer.write(
                "&nbsp;"
                    + HtmlWidgets.htmlTooltipImage(
                        EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowRR.gif",
                        ">|",
                        MessageFormat.format(
                                EDStatic.messages.magTimeRangeLastAr[language], timeRangeString)
                            + timesVary,
                        "class=\"B\" "
                            + // vertical-align: 'b'ottom
                            "onMouseUp='f1.start"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, tidMinGc)
                            + "\"; "
                            + "f1.stop"
                            + timeIndex
                            + ".value=\""
                            + Calendar2.limitedFormatAsISODateTimeT(time_precision, tidMaxGc)
                            + "\"; "
                            + "mySubmit(true);'"));
          } else {
            writer.write(timeGap);
          }
        }

        // trailing <br>
        writer.write("<br>");
      }

      // *** zoomX stuff
      if (zoomX) {
        EDVGridAxis xedvga = axisVariables[axisVarX];
        int tStartIndex = avStartIndex[axisVarX];
        int tStopIndex = avStopIndex[axisVarX];
        int tRange = tStopIndex - tStartIndex; // will be >0
        int tRange2 = Math.max(1, tRange / 2); // will be >0
        int tRange4 = Math.max(1, tRange / 4); // will be >0
        int tSize = sourceSize[axisVarX];
        int tSize1 = tSize - 1;
        // if (reallyVerbose) System.out.println(">> zoomX start=" + tStartIndex + " stop=" +
        // tStopIndex + " tSize1=" + tSize1);
        String tGap = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
        writer.write("<strong>X range:</strong>&nbsp;\n");

        // zoom in
        if (tRange > 5) {
          int center = (tStopIndex + tStartIndex) / 2;
          int ttStartIndex = Math.max(0, center - tRange4);
          int ttStopIndex = ttStartIndex + tRange2;
          writer.write(
              widgets.htmlButton(
                      "button",
                      "zoomXzoomIn",
                      "", // value
                      EDStatic.messages.zoomInAr[language],
                      EDStatic.messages.zoomInAr[language], // tooltip, labelHtml
                      "onMouseUp='f1.start"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(ttStartIndex).getString(0)
                          + "\"; "
                          + "f1.stop"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(ttStopIndex).getString(0)
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");
        } else {
          writer.write(tGap + tGap);
        }

        // zoom out
        if (tStartIndex > 0 || tStopIndex < tSize1) {
          int center = (tStopIndex + tStartIndex) / 2;
          int ttStartIndex, ttStopIndex;
          if (center < tSize / 2) {
            // closer to left edge
            ttStartIndex = Math.max(0, center - tRange);
            ttStopIndex = Math.min(tSize1, ttStartIndex + tRange * 2);
          } else {
            // closer to right edge
            ttStopIndex = Math.min(tSize1, center + tRange);
            ttStartIndex = Math.max(0, ttStopIndex - tRange * 2);
          }
          writer.write(
              widgets.htmlButton(
                      "button",
                      "zoomXzoomOut",
                      "", // value
                      EDStatic.messages.zoomOutAr[language],
                      EDStatic.messages.zoomOutAr[language], // tooltip, labelHtml
                      "onMouseUp='f1.start"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(ttStartIndex).getString(0)
                          + "\"; "
                          + "f1.stop"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(ttStopIndex).getString(0)
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");
        }

        if (tStartIndex > 0) {
          // all the way left
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowLL.gif",
                      "|<",
                      EDStatic.messages.shiftXAllTheWayLeftAr[language],
                      "class=\"B\" "
                          + // vertical-align: 'b'ottom
                          "onMouseUp='f1.start"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(0).getString(0)
                          + "\"; "
                          + "f1.stop"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(tRange).getString(0)
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");

          // left
          int howMuch = Math.min(tRange2, tStartIndex);
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "minus.gif",
                      "-",
                      EDStatic.messages.shiftXLeftAr[language],
                      "class=\"B\" "
                          + // vertical-align: 'b'ottom
                          "onMouseUp='f1.start"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(tStartIndex - howMuch).getString(0)
                          + "\"; "
                          + "f1.stop"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(tStopIndex - howMuch).getString(0)
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");
        } else {
          writer.write(tGap);
        }

        if (tStopIndex < tSize1) {
          // right
          int howMuch = Math.min(tRange2, tSize1 - tStopIndex);
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "plus.gif",
                      "+",
                      EDStatic.messages.shiftXRightAr[0],
                      "class=\"B\" "
                          + // vertical-align: 'b'ottom
                          "onMouseUp='f1.start"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(tStartIndex + howMuch).getString(0)
                          + "\"; "
                          + "f1.stop"
                          + axisVarX
                          + ".value=\""
                          + xedvga.destinationValue(tStopIndex + howMuch).getString(0)
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");

          // all the way right
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                  EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowRR.gif",
                  ">|",
                  EDStatic.messages.shiftXAllTheWayRightAr[language],
                  "class=\"B\" "
                      + // vertical-align: 'b'ottom
                      "onMouseUp='f1.start"
                      + axisVarX
                      + ".value=\""
                      + xedvga.destinationValue(tSize1 - tRange).getString(0)
                      + "\"; "
                      + "f1.stop"
                      + axisVarX
                      + ".value=\""
                      + xedvga.destinationValue(tSize1).getString(0)
                      + "\"; "
                      + "mySubmit(true);'")
              // + "&nbsp;"  //no need
              );

        } else {
          // writer.write(tGap);  //no need
        }

        // trailing <br>
        writer.write("<br>");
      }

      // show the graph
      String aQuery = graphQuery.toString();
      if (verbose) String2.log("graphQuery=" + graphQuery);
      // don't use \n for the following lines
      if (zoomLatLon)
        writer.write(
            "<a href=\""
                + XML.encodeAsHTMLAttribute(
                    tErddapUrl + "/griddap/" + datasetID + ".graph?" + aQuery)
                + "&amp;.click=\">"); // if user clicks on image, browser adds "?x,y" to url
      writer.write(
          "<img "
              + (zoomLatLon ? "ismap " : "")
              + "width=\""
              + EDStatic.messages.imageWidths[1]
              + "\" height=\""
              + EDStatic.messages.imageHeights[1]
              + "\" "
              + "alt=\""
              + EDStatic.messages.patientYourGraphAr[language]
              + "\" "
              + "src=\""
              + XML.encodeAsHTMLAttribute(tErddapUrl + "/griddap/" + datasetID + ".png?" + aQuery)
              + "\">");
      if (zoomLatLon) writer.write("</a>");

      // *** end of right half of big table
      writer.write("\n</td></tr></table>\n");
      // writer.write(
      //    "</div>" + //end of rightContent
      //    "<div class=\"rightSpace\"></div>\n"); //rightSpace

      writer.flush(); // Steve Souder says: the sooner you can send some html to user, the better

      // *** stuff at end of document
      // writer.write(
      //    "<div class=\"standard_width\" style=\"clear:both; \">");

      // *** Things you can do with graphs
      writer.write(
          String2.replaceAll(
              MessageFormat.format(EDStatic.messages.doWithGraphsAr[language], tErddapUrl),
              "&erddapUrl;",
              tErddapUrl));
      writer.write("\n\n");

      // write das
      writer.write(
          "<hr>\n"
              + "<h2><a class=\"selfLink\" id=\"DAS\" href=\"#DAS\" rel=\"bookmark\">"
              + EDStatic.messages.dasTitleAr[language]
              + "</a></h2>\n"
              + "<pre style=\"white-space:pre-wrap;\">\n");
      writeDAS(
          "/griddap/" + datasetID + ".das",
          "",
          writer,
          true); // useful so search engines find all relevant words
      writer.write("</pre>\n");

      // then write DAP instructions
      writer.write("""
              <br>&nbsp;
              <hr>
              """);
      writeGeneralDapHtmlInstructions(language, tErddapUrl, writer, false);

      // the javascript for the sliders
      writer.write(
          widgets.sliderScript(
              sliderFromNames,
              sliderToNames,
              sliderNThumbs,
              sliderUserValuesCsvs,
              sliderInitFromPositions,
              sliderInitToPositions,
              EDV.SLIDER_PIXELS - 1));

      writer.write("</div>\n"); // standard_width
      writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
      writer.write("</html>");
      if (!dimensionValuesInMemory) setDimensionValuesToNull();

      writer.close();
    } catch (Exception e) {
      EDStatic.rethrowClientAbortException(e); // first thing in catch{}
      String2.log(
          String2.ERROR
              + " when writing web page:\n"
              + MustBe.throwableToString(e)); // before writer.write's
      writer.write(EDStatic.htmlForException(language, e));
      writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
      writer.write("</html>");
      if (!dimensionValuesInMemory) setDimensionValuesToNull();

      writer.close();
      throw e;
    }
  }

  /**
   * This writes the dataset data attributes (DAS) to the outputStream. It is always the same
   * regardless of the userDapQuery (except for history). (That's what THREDDS does -- OPeNDAP 2.0
   * 7.2.1 is vague. THREDDs doesn't even object if userDapQuery is invalid.)
   *
   * <p>E.g.,
   *
   * <pre>
   * Attributes {
   * altitude {
   * Float32 actual_range 0.0, 0.0;
   * Int32 fraction_digits 0;
   * String long_name "Altitude";
   * String standard_name "altitude";
   * String units "m";
   * String axis "Z";
   * }
   * NC_GLOBAL {
   * ....
   * }
   * }
   * </pre>
   *
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null). (Affects history only.)
   * @param writer a Writer. At the end of this method the Writer is flushed, not closed.
   * @param encodeAsHtml if true, characters like &lt; are converted to their character entities.
   * @throws Throwable if trouble.
   */
  public void writeDAS(String requestUrl, String userDapQuery, Writer writer, boolean encodeAsHtml)
      throws Throwable {

    int nAxisVariables = axisVariables.length;
    int nDataVariables = dataVariables.length;
    writer.write("Attributes {" + OpendapHelper.EOL); // see EOL definition for comments
    for (int av = 0; av < nAxisVariables; av++)
      OpendapHelper.writeToDAS(
          axisVariables[av].destinationName(),
          axisVariables[av].destinationDataPAType(),
          axisVariables[av].combinedAttributes(),
          writer,
          encodeAsHtml);
    for (int dv = 0; dv < nDataVariables; dv++)
      OpendapHelper.writeToDAS(
          dataVariables[dv].destinationName(),
          dataVariables[dv].destinationDataPAType(),
          dataVariables[dv].combinedAttributes(),
          writer,
          encodeAsHtml);

    // how do global attributes fit into opendap view of attributes?
    Attributes gAtts = new Attributes(combinedGlobalAttributes); // a copy

    // fix up global attributes  (always to a local COPY of global attributes)
    EDD.addToHistory(gAtts, publicSourceUrl());
    EDD.addToHistory(
        gAtts,
        EDStatic.config.baseUrl
            + requestUrl
            + (userDapQuery == null || userDapQuery.length() == 0 ? "" : "?" + userDapQuery));

    OpendapHelper.writeToDAS(
        "NC_GLOBAL", // .nc files say NC_GLOBAL; ncBrowse and netcdf-java treat NC_GLOBAL as special
        // case
        PAType.DOUBLE, // isUnsigned doesn't apply to global atts. double won't trigger "_Unsigned"
        gAtts,
        writer,
        encodeAsHtml);
    writer.write("}" + OpendapHelper.EOL); // see EOL definition for comments
    writer.flush(); // essential
  }

  /**
   * This gets the data for the userDapQuery and writes the grid data structure (DDS) to the
   * outputStream. E.g.
   *
   * <pre>
   * Dataset {
   * Float64 lat[lat = 180];
   * Float64 lon[lon = 360];
   * Float64 time[time = 404];
   * Grid {
   * ARRAY:
   * Int32 sst[time = 404][lat = 180][lon = 360];
   * MAPS:
   * Float64 time[time = 404];
   * Float64 lat[lat = 180];
   * Float64 lon[lon = 360];
   * } sst;
   * } weekly;
   * </pre>
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param writer an 8859-1 writer, usually already buffered, to receive the results. At the end of
   *     this method the writer is flushed, NOT closed.
   * @throws Throwable if trouble.
   */
  public void writeDDS(int language, String requestUrl, String userDapQuery, Writer writer)
      throws Throwable {

    if (reallyVerbose) String2.log("  EDDGrid.writeDDS");
    long time = System.currentTimeMillis();

    // handle axisDapQuery
    if (isAxisDapQuery(userDapQuery)) {
      // get axisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, this, requestUrl, userDapQuery);
      int nRAV = ada.nRequestedAxisVariables();

      writer.write("Dataset {" + OpendapHelper.EOL); // see EOL definition for comments
      for (int av = 0; av < nRAV; av++) {
        String destName = ada.axisVariables(av).destinationName();
        PrimitiveArray apa = ada.axisValues(av);
        writer.write(
            "  "
                + // e.g., Float64 time[time = 404];
                OpendapHelper.getAtomicType(apa.elementType())
                + " "
                + destName
                + "["
                + destName
                + " = "
                + apa.size()
                + "];"
                + OpendapHelper.EOL);
      }
      // Thredds recently started using urlEncoding the final name (and other names?). I don't.
      // Update: I think they undid this change.
      writer.write("} " + datasetID + ";" + OpendapHelper.EOL);
      writer.flush(); // essential

      // diagnostic
      if (reallyVerbose) String2.log("  EDDGrid.writeDDS axis done.");
      return;
    }

    // get gridDataAccessor first, in case of error when parsing query
    try (GridDataAccessor gridDataAccessor =
        new GridDataAccessor(
            language, this, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN
      boolean entireDataset =
          userDapQuery == null || SSR.percentDecode(userDapQuery).trim().length() == 0;

      int nAxisVariables = axisVariables.length;
      int nDataVariables = gridDataAccessor.dataVariables().length;
      writer.write("Dataset {" + OpendapHelper.EOL); // see EOL definition for comments
      String arrayDims[] = new String[nAxisVariables]; // each e.g., [time = 404]
      String dims[] = new String[nAxisVariables]; // each e.g., Float64 time[time = 404];
      StringBuilder allArrayDims = new StringBuilder();
      for (int av = 0; av < nAxisVariables; av++) {
        PrimitiveArray apa = gridDataAccessor.axisValues(av);
        arrayDims[av] = "[" + axisVariables[av].destinationName() + " = " + apa.size() + "]";
        dims[av] =
            OpendapHelper.getAtomicType(apa.elementType())
                + " "
                + axisVariables[av].destinationName()
                + arrayDims[av]
                + ";"
                + OpendapHelper.EOL;
        allArrayDims.append(arrayDims[av]);
        if (entireDataset) writer.write("  " + dims[av]);
      }
      for (int dv = 0; dv < nDataVariables; dv++) {
        String dvName = gridDataAccessor.dataVariables()[dv].destinationName();
        writer.write("  GRID {" + OpendapHelper.EOL);
        writer.write("    ARRAY:" + OpendapHelper.EOL);
        writer.write(
            "      "
                + OpendapHelper.getAtomicType(
                    gridDataAccessor.dataVariables()[dv].destinationDataPAType())
                + " "
                + dvName
                + allArrayDims
                + ";"
                + OpendapHelper.EOL);
        writer.write("    MAPS:" + OpendapHelper.EOL);
        for (int av = 0; av < nAxisVariables; av++) writer.write("      " + dims[av]);
        writer.write("  } " + dvName + ";" + OpendapHelper.EOL);
      }

      // Thredds recently started using urlEncoding the final name (and other names?).
      // I don't (yet).
      writer.write("} " + datasetID + ";" + OpendapHelper.EOL);
      writer.flush(); // essential
    }

    // diagnostic
    if (reallyVerbose)
      String2.log("  EDDGrid.writeDDS done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * Validates that the input value is less than (or approximately equal based on the precision) the
   * coarseMax value. If the check fails then this throws an error or repairs the value based on the
   * repair param.
   *
   * @param precision Significant digits to compare. 0 to 18 are allowed; 5, 9, and 14 are common
   * @param value The value to validate.
   * @param stringValue A string representation of the input, this is used in the error message.
   * @param av The grid axis that is used for the comparison. The destinationCoarseMax is used as
   *     the max value to compare against. The lastDestinationValue is used as the repairTo value.
   *     The destinationMaxString is used in the error messages.
   * @param repair If true and the less than check fails, this returns the repaired value. If false
   *     and the check fails, then an error is thrown.
   * @param idAr A String[] with translations of the item being tested.
   * @param language the index of the selected language
   * @param diagnostic0 Informational string about the axis for this check. In language 0.
   * @param diagnosticl Informational string about the axis for this check. In the language that
   *     matches the language input.
   * @return the value or the repaired value.
   */
  private double validateLessThanThrowOrRepair(
      int precision,
      double value,
      String stringValue,
      EDVGridAxis av,
      boolean repair,
      String[] idAr,
      int language,
      String diagnostic0,
      String diagnosticl) {
    return validateLessThanThrowOrRepair(
        precision,
        value,
        stringValue,
        av.destinationMaxString(),
        av.lastDestinationValue(),
        av.destinationCoarseMax(),
        av instanceof EDVTimeStampGridAxis
            ? Calendar2.epochSecondsToIsoStringTZ(av.destinationCoarseMax())
            : "" + av.destinationCoarseMax(),
        repair,
        idAr,
        language,
        diagnostic0,
        diagnosticl);
  }

  /**
   * Validates that the input value is greater than (or approximately equal based on the precision)
   * the coarseMin value. If the check fails then this throws an error or repairs the value based on
   * the repair param.
   *
   * @param precision Significant digits to compare. 0 to 18 are allowed; 5, 9, and 14 are common
   * @param value The value to validate.
   * @param stringValue A string representation of the input, this is used in the error message.
   * @param av The grid axis that is used for the comparison. The destinationCoarseMin is used as
   *     the max value to compare against. The firstDestinationValue is used as the repairTo value.
   *     The destinationMinString is used in the error messages.
   * @param repair If true and the less than check fails, this returns the repaired value. If false
   *     and the check fails, then an error is thrown.
   * @param idAr A String[] with translations of the item being tested.
   * @param language the index of the selected language
   * @param diagnostic0 Informational string about the axis for this check. In language 0.
   * @param diagnosticl Informational string about the axis for this check. In the language that
   *     matches the language input.
   * @return the value or the repaired value.
   */
  private double validateGreaterThanThrowOrRepair(
      int precision,
      double value,
      String stringValue,
      EDVGridAxis av,
      boolean repair,
      String idAr[],
      int language,
      String diagnostic0,
      String diagnosticl) {
    return validateGreaterThanThrowOrRepair(
        precision,
        value,
        stringValue,
        av.destinationMinString(),
        av.firstDestinationValue(),
        av.destinationCoarseMin(),
        av instanceof EDVTimeStampGridAxis
            ? Calendar2.epochSecondsToIsoStringTZ(av.destinationCoarseMin())
            : "" + av.destinationCoarseMin(),
        repair,
        idAr,
        language,
        diagnostic0,
        diagnosticl);
  }

  /**
   * Validates that the input value is less than (or approximately equal based on the precision) the
   * coarseMax value. If the check fails then this throws an error or repairs the value based on the
   * repair param.
   *
   * @param precision Significant digits to compare. 0 to 18 are allowed; 5, 9, and 14 are common
   * @param value The value to validate.
   * @param stringValue A string representation of the input, this is used in the error message. If
   *     the value is a dateTime, this should be the ISO 8601 representation.
   * @param stringMax The exact maximum value, as a string. This is only used in the error message.
   *     If the value is a dateTime, this should be the ISO 8601 representation.
   * @param repairTo The value to repair to if repair is true.
   * @param coarseMax The maximum value to check against.
   * @param coarseMaxString This is only used in the error message. If the value is a dateTime, this
   *     should be the ISO 8601 representation.
   * @param repair If true and the less than check fails, this returns the repaired value. If false
   *     and the check fails, then an error is thrown.
   * @param idAr A String[] with translations of the item being tested.
   * @param language the index of the selected language
   * @param diagnostic0 Informational string about the axis for this check. In language 0.
   * @param diagnosticl Informational string about the axis for this check. In the language that
   *     matches the language input.
   * @param isStart true if testing start value. false if testing stop value.
   * @return the value or the repaired value.
   */
  private double validateLessThanThrowOrRepair(
      int precision,
      double value,
      String stringValue,
      String stringMax,
      double repairTo,
      double coarseMax,
      String coarseMaxString,
      boolean repair,
      String idAr[],
      int language,
      String diagnostic0,
      String diagnosticl) {
    if (Math2.lessThanAE(precision, value, coarseMax)) {
    } else {
      if (repair) value = repairTo;
      else
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                MustBe.THERE_IS_NO_DATA
                    + " "
                    + EDStatic.messages.queryErrorAr[0]
                    + diagnostic0
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorGridGreaterMaxAr[0],
                        idAr[0],
                        stringValue,
                        stringMax,
                        coarseMaxString),
                MustBe.THERE_IS_NO_DATA
                    + " "
                    + EDStatic.messages.queryErrorAr[language]
                    + diagnosticl
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorGridGreaterMaxAr[language],
                        idAr[language],
                        stringValue,
                        stringMax,
                        coarseMaxString)));
    }
    return value;
  }

  /**
   * Validates that the input value is greater than (or approximately equal based on the precision)
   * the coarseMin value. If the check fails then this throws an error or repairs the value based on
   * the repair param.
   *
   * @param precision Significant digits to compare. 0 to 18 are allowed; 5, 9, and 14 are common
   * @param value The value to validate.
   * @param stringValue A string representation of the input, this is used in the error message. If
   *     the value is a dateTime, this should be the ISO 8601 representation.
   * @param stringMin The exact minimum value, as a string. This is only used in the error message.
   *     If the value is a dateTime, this should be the ISO 8601 representation.
   * @param repairTo The value to repair to if repair is true.
   * @param coarseMin The minimum value to check against.
   * @param coarseMinString This is only used in the error message. If the value is a dateTime, this
   *     should be the ISO 8601 representation.
   * @param repair If true and the greater than check fails, this returns the repaired value. If
   *     false and the check fails, then an error is thrown.
   * @param language the index of the selected language
   * @param diagnostic0 Informational string about the axis for this check. In language 0.
   * @param diagnosticl Informational string about the axis for this check. In the language that
   *     matches the language input.
   * @param isStart true if testing start value. false if testing stop value.
   * @return the value or the repaired value.
   */
  private double validateGreaterThanThrowOrRepair(
      int precision,
      double value,
      String stringValue,
      String stringMin,
      double repairTo,
      double coarseMin,
      String coarseMinString,
      boolean repair,
      String idAr[],
      int language,
      String diagnostic0,
      String diagnosticl) {
    if (Math2.greaterThanAE(precision, value, coarseMin)) {
    } else {
      if (repair) value = repairTo;
      else
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                MustBe.THERE_IS_NO_DATA
                    + " "
                    + EDStatic.messages.queryErrorAr[0]
                    + diagnostic0
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorGridLessMinAr[0],
                        idAr[0],
                        stringValue,
                        stringMin,
                        coarseMinString),
                MustBe.THERE_IS_NO_DATA
                    + " "
                    + EDStatic.messages.queryErrorAr[language]
                    + diagnosticl
                    + ": "
                    + MessageFormat.format(
                        EDStatic.messages.queryErrorGridLessMinAr[language],
                        idAr[language],
                        stringValue,
                        stringMin,
                        coarseMinString)));
    }
    return value;
  }

  /**
   * Validates the provided min/max lat/lon values are valid and throws SimpleException if they are
   * not. Invalid vales are if any value is outside the range of valid lat/lon values. It is also
   * invalid if no part of the requested range overlaps with the requested data set.
   *
   * @param language the index of the selected language
   * @param minX the minimum X / longitude value to validate. minX should be &lt; maxX
   * @param maxX the maximum X / longitude value to check. minX should be &lt; maxX
   * @param minY the minimum Y / latitude value to check. minY should be &lt; maxY
   * @param maxY the minimum Y / latitude value to check. minY should be &lt; maxY
   */
  public void validateLatLon(int language, double minX, double maxX, double minY, double maxY) {
    int precision = 9; // Precision 9 for doubles.
    // Note the max/min lat/lon checks are one larger/smaller than expected
    // to allow for slightly outside of range inputs.

    // X / longitude.
    EDVGridAxis av = axisVariables[lonIndex];
    String diagnostic0 =
        MessageFormat.format(
            EDStatic.messages.queryErrorGridDiagnosticAr[0],
            av.destinationName(),
            "" + lonIndex,
            av.destinationName());
    String diagnosticl =
        MessageFormat.format(
            EDStatic.messages.queryErrorGridDiagnosticAr[language],
            av.destinationName(),
            "" + lonIndex,
            av.destinationName());
    // Validate Longitude values.
    // Validate request contains some data.
    validateLessThanThrowOrRepair(
        precision,
        minX,
        "" + minX,
        av,
        false /* repair */,
        EDStatic.messages.advl_minLongitudeAr,
        language,
        diagnostic0,
        diagnosticl);
    validateGreaterThanThrowOrRepair(
        precision,
        maxX,
        "" + maxX,
        av,
        false /* repair */,
        EDStatic.messages.advl_maxLongitudeAr,
        language,
        diagnostic0,
        diagnosticl);

    // Y / Latitude.
    av = axisVariables[latIndex];
    diagnostic0 =
        MessageFormat.format(
            EDStatic.messages.queryErrorGridDiagnosticAr[0],
            av.destinationName(),
            "" + latIndex,
            av.destinationName());
    diagnosticl =
        MessageFormat.format(
            EDStatic.messages.queryErrorGridDiagnosticAr[language],
            av.destinationName(),
            "" + latIndex,
            av.destinationName());
    // Validate Latitude values.
    validateGreaterThanThrowOrRepair(
        precision,
        minY,
        "" + minY,
        "-90",
        -90 /* repairTo */,
        -91,
        "-91" /* coarseMin */,
        false /* repair */,
        EDStatic.messages.advl_minLatitudeAr,
        language,
        diagnostic0,
        diagnosticl);
    validateLessThanThrowOrRepair(
        precision,
        maxY,
        "" + maxY,
        "90",
        90 /* repairTo */,
        91,
        "91" /* coarseMax */,
        false /* repair */,
        EDStatic.messages.advl_maxLatitudeAr,
        language,
        diagnostic0,
        diagnosticl);
    // Validate request contains some data.
    validateLessThanThrowOrRepair(
        precision,
        minY,
        "" + minY,
        av,
        false /* repair */,
        EDStatic.messages.advl_minLatitudeAr,
        language,
        diagnostic0,
        diagnosticl);
    validateGreaterThanThrowOrRepair(
        precision,
        maxY,
        "" + maxY,
        av,
        false /* repair */,
        EDStatic.messages.advl_maxLatitudeAr,
        language,
        diagnostic0,
        diagnosticl);
  }

  /**
   * This writes gda's dataVariable[0] as a "matrix" to the Matlab stream.
   *
   * @param language the index of the selected language
   * @param stream the stream for the Matlab file
   * @param name usually the destinationName. But inside a Structure, use "".
   * @param gda provides access to just *one* of the data variables. gda must be column-major.
   * @param ndIndex the 2+ dimension NDimensionalIndex
   * @throws Throwable if trouble
   */
  public void writeNDimensionalMatlabArray(
      int language,
      DataOutputStream stream,
      String name,
      GridDataAccessor gda,
      NDimensionalIndex ndIndex)
      throws Throwable {

    if (gda.rowMajor())
      throw new SimpleException(
          EDStatic.messages.errorInternalAr[0]
              + "In EDDGrid.writeNDimensionalMatlabArray, the GridDataAccessor must be column-major.");

    // do the first part
    EDV edv = gda.dataVariables()[0];
    PAType elementPAType = edv.destinationDataPAType();
    if (elementPAType == PAType.STRING)
      // can't do String data because you need random access to all values
      // that could be a memory nightmare
      // so just don't allow it
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "Matlab files can't have String data.");
    int nDataBytes = Matlab.writeNDimensionalArray1(stream, name, elementPAType, ndIndex);

    // do the second part here
    // note:  calling increment() on column-major gda returns data in column-major order

    // it is important to write the data to the stream as the correct type
    PAOne paOne =
        new PAOne(
            // 2020-03-20 I think Matlab fully supports all these data types as is. See my Matlab
            // class.
            //            elementPAType == PAType.LONG    ||
            //            elementPAType == PAType.ULONG?
            //                PAType.DOUBLE : //loss of precision: because old matlab format doesn't
            // support longs
            //            elementPAType == PAType.CHAR    ||
            //            elementPAType == PAType.BYTE    ||
            //            elementPAType == PAType.UBYTE   ||
            //            elementPAType == PAType.SHORT   ||
            //            elementPAType == PAType.USHORT  ||
            //            elementPAType == PAType.INT     ||
            //            elementPAType == PAType.UINT? //trouble: matlab user probably doesn't
            // want/expect UINT stored as INT
            //                PAType.INT : //trouble: is this true: old matlab format stores smaller
            // int types as ints  ???
            elementPAType);

    while (gda.increment()) gda.getDataValueAsPAOne(0, paOne).writeToDOS(stream);

    // pad data to 8 byte boundary
    int i = nDataBytes % 8;
    while ((i++ % 8) != 0) stream.write(0); // 0 padded to 8 byte boundary
  }

  /**
   * Save the grid data in a netCDF .nc3 file. This overwrites any existing file of the specified
   * name. This makes an effort not to create a partial file if there is an error. If no exception
   * is thrown, the file was successfully created.
   *
   * @param language the index of the selected language
   * @param ncVersion either NetcdfFileFormat.NETCDF3 or NETCDF4.
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'.
   * @param userDapQuery an OPeNDAP DAP-style query string, still percentEncoded (shouldn't be
   *     null). e.g., ATssta[45:1:45][0:1:0][120:10:140][130:10:160]
   * @param fullFileName the name for the file (including directory and extension)
   * @param keepUnusedAxes if true, axes with size=1 will be stored in the file. If false, axes with
   *     size=1 will not be stored in the file (geotiff needs this).
   * @param lonAdjust the value to be added to all lon values (e.g., 0 or -360).
   * @throws Throwable
   */
  public void saveAsNc(
      int language,
      NetcdfFileFormat ncVersion,
      String ipAddress,
      String requestUrl,
      String userDapQuery,
      String fullFileName,
      boolean keepUnusedAxes,
      double lonAdjust)
      throws Throwable {
    if (reallyVerbose) String2.log("  EDDGrid.saveAsNc");
    long time = System.currentTimeMillis();

    if (ncVersion != NetcdfFileFormat.NETCDF3 && ncVersion != NetcdfFileFormat.NETCDF4)
      throw new RuntimeException("ERROR: unsupported ncVersion=" + ncVersion);
    boolean nc3Mode = ncVersion == NetcdfFileFormat.NETCDF3;

    // delete any existing file
    File2.delete(fullFileName);

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // handle axisDapQuery
    if (isAxisDapQuery(userDapQuery)) {
      // get axisDataAccessor first, in case of error when parsing query
      AxisDataAccessor ada = new AxisDataAccessor(language, this, requestUrl, userDapQuery);
      int nRAV = ada.nRequestedAxisVariables();

      // write the data
      // items determined by looking at a .nc file; items written in that order
      NetcdfFormatWriter ncWriter = null;
      try {
        NetcdfFormatWriter.Builder nc =
            NetcdfFormatWriter.createNewNetcdf3(fullFileName + randomInt);
        Group.Builder rootGroup = nc.getRootGroup();
        nc.setFill(false);

        // define the dimensions
        Array axisArrays[] = new Array[nRAV];
        Variable.Builder newAxisVars[] = new Variable.Builder[nRAV];
        for (int av = 0; av < nRAV; av++) {
          String destName = ada.axisVariables(av).destinationName();
          PrimitiveArray pa = ada.axisValues(av);
          if (nc3Mode && (pa instanceof LongArray || pa instanceof ULongArray))
            pa = new DoubleArray(pa);
          Dimension dimension = NcHelper.addDimension(rootGroup, destName, pa.size());
          axisArrays[av] =
              Array.factory(
                  NcHelper.getNc3DataType(pa.elementType()),
                  new int[] {pa.size()},
                  pa.toObjectArray());
          if (ncVersion == NetcdfFileFormat.NETCDF3) String2.log("HELP");
          newAxisVars[av] =
              NcHelper.addVariable(
                  rootGroup,
                  destName,
                  NcHelper.getNc3DataType(
                      pa.elementType()), // nc3Mode long & ulong->double done above. No Strings as
                  // axes.
                  List.of(dimension));

          // write axis attributes
          Attributes atts = new Attributes(ada.axisAttributes(av)); // use a copy
          PAType paType = pa.elementType();
          if (pa.elementType() == PAType.STRING) // never
          atts.add(File2.ENCODING, File2.ISO_8859_1);
          // disabled until there is a standard
          //                    else if (paType == PAType.CHAR)  //never
          //                        atts.add(String2.CHARSET, File2.ISO_8859_1);

          NcHelper.setAttributes(nc3Mode, newAxisVars[av], atts, paType.isUnsigned());
        }

        // write global attributes
        NcHelper.setAttributes(nc3Mode, rootGroup, ada.globalAttributes());

        // leave "define" mode
        ncWriter = nc.build();

        // write the axis values
        for (int av = 0; av < nRAV; av++)
          ncWriter.write(newAxisVars[av].getFullName(), axisArrays[av]);

        // if close throws Throwable, it is trouble
        ncWriter.close(); // it calls flush() and doesn't like flush called separately
        ncWriter = null;

        // rename the file to the specified name
        File2.rename(fullFileName + randomInt, fullFileName);

        // diagnostic
        if (reallyVerbose) String2.log("  EDDGrid.saveAsNc axis done\n");
        // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

      } catch (Throwable t) {
        String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
        if (ncWriter != null) {
          try {
            ncWriter.abort();
          } catch (Exception e9) {
          }
          File2.delete(fullFileName + randomInt);
          ncWriter = null;
        }

        throw t;
      }
      return;
    }

    // ** create gridDataAccessor first,
    // to check for error when parsing query or getting data,
    // and to check that file size < 2GB
    // This throws exception if invalid query.
    try (GridDataAccessor gda =
        new GridDataAccessor(
            language, this, requestUrl, userDapQuery, true, false)) { // rowMajor, convertToNaN

      // ensure file size < 2GB
      // ???is there a way to allow >2GB netcdf 3 files?
      // Yes: the 64-bit extension!  But this code doesn't yet use that.
      //   And even if so, what about OS limit ERDDAP is running on? and client OS?
      // Or, view this as protection against accidental requests for too much data (e.g., whole
      // dataset).
      if (gda.totalNBytes() > 2100000000) // leave some space for axis vars, etc.
      throw new SimpleException(
            Math2.memoryTooMuchData
                + "  "
                + MessageFormat.format(
                    EDStatic.messages.errorMoreThan2GBAr[0],
                    ".nc",
                    ((gda.totalNBytes() + 100000) / Math2.BytesPerMB) + " MB"));

      if (gda.totalNBytes() > 1000000000) { // 1GB
        EDStatic.tally.add("Large Request, IP address (since last Major LoadDatasets)", ipAddress);
        EDStatic.tally.add("Large Request, IP address (since last daily report)", ipAddress);
        EDStatic.tally.add("Large Request, IP address (since startup)", ipAddress);
      }

      NetcdfFormatWriter ncWriter = null;
      // ** Then get gridDataAllAccessor
      // AllAccessor so max length of String variables will be known.
      try (GridDataAllAccessor gdaa = new GridDataAllAccessor(gda)) {
        EDV tDataVariables[] = gda.dataVariables();

        // write the data
        // items determined by looking at a .nc file; items written in that order
        NetcdfFormatWriter.Builder nc =
            NetcdfFormatWriter.createNewNetcdf3(fullFileName + randomInt);
        Group.Builder rootGroup = nc.getRootGroup();
        nc.setFill(false);

        // find active axes
        IntArray activeAxes = new IntArray();
        for (int av = 0; av < axisVariables.length; av++) {
          if (keepUnusedAxes || gda.axisValues(av).size() > 1) activeAxes.add(av);
        }

        // define the dimensions
        int nActiveAxes = activeAxes.size();
        ArrayList<Dimension> axisDimensionList = new ArrayList<>();
        Array axisArrays[] = new Array[nActiveAxes];
        Variable.Builder newAxisVars[] = new Variable.Builder[nActiveAxes];
        int stdShape[] = new int[nActiveAxes];
        for (int a = 0; a < nActiveAxes; a++) {
          int av = activeAxes.get(a);
          String avName = axisVariables[av].destinationName();
          PrimitiveArray pa = gda.axisValues(av);
          // if (reallyVerbose) String2.log(" create dim=" + avName + " size=" + pa.size());
          if (nc3Mode && (pa instanceof LongArray || pa instanceof ULongArray))
            pa = new DoubleArray(pa);
          stdShape[a] = pa.size();
          Dimension tDim = NcHelper.addDimension(rootGroup, avName, pa.size());
          axisDimensionList.add(tDim);
          if (av == lonIndex) pa.scaleAddOffset(1, lonAdjust);
          axisArrays[a] =
              Array.factory(
                  NcHelper.getNc3DataType(gda.axisValues(av).elementType()),
                  new int[] {pa.size()},
                  pa.toObjectArray());
          // if (reallyVerbose) String2.log(" create var=" + avName);
          newAxisVars[a] =
              NcHelper.addVariable(
                  rootGroup,
                  avName,
                  NcHelper.getNc3DataType(
                      pa.elementType()), // nc3Mode long->double done above. No Strings as axes.
                  Collections.singletonList(axisDimensionList.get(a)));
        }

        // define the data variables
        Variable.Builder newVars[] = new Variable.Builder[tDataVariables.length];
        for (int dv = 0; dv < tDataVariables.length; dv++) {
          String destName = tDataVariables[dv].destinationName();
          PAType destPAType = tDataVariables[dv].destinationDataPAType();
          // if (reallyVerbose) String2.log(" create var=" + destName);

          // nc3 String data? need to create specially (so there's a strlen dimension for this
          // variable)
          if (nc3Mode && destPAType == PAType.STRING) {
            StringArray tsa = (StringArray) gdaa.getPrimitiveArray(dv);
            newVars[dv] =
                NcHelper.addNc3StringVariable(
                    rootGroup, destName, axisDimensionList, tsa.maxStringLength());

          } else {
            newVars[dv] =
                NcHelper.addVariable(
                    rootGroup,
                    destName,
                    NcHelper.getDataType(nc3Mode, destPAType),
                    axisDimensionList);
          }
        }

        // write global attributes
        NcHelper.setAttributes(nc3Mode, rootGroup, gda.globalAttributes);

        // write axis attributes
        for (int a = 0; a < nActiveAxes; a++) {
          int av = activeAxes.get(a);
          Attributes atts = new Attributes(gda.axisAttributes[av]); // use a copy
          PAType paType = gda.axisValues[av].elementType();
          if (paType == PAType.STRING) // never
          atts.add(File2.ENCODING, File2.ISO_8859_1);
          // disabled until there is a standard
          //                else if (paType == PAType.CHAR)  //never
          //                    atts.add(String2.CHARSET, File2.ISO_8859_1);

          NcHelper.setAttributes(nc3Mode, newAxisVars[a], atts, paType.isUnsigned());
        }

        // write data attributes
        for (int dv = 0; dv < tDataVariables.length; dv++) {
          Attributes atts = new Attributes(gda.dataAttributes[dv]); // use a copy
          PAType paType = gda.dataVariables[dv].destinationDataPAType();
          if (paType == PAType.STRING) // never
          atts.add(File2.ENCODING, File2.ISO_8859_1);
          // disabled until there is a standard
          //                else if (paType == PAType.CHAR)  //never
          //                    atts.add(String2.CHARSET, File2.ISO_8859_1);

          NcHelper.setAttributes(nc3Mode, newVars[dv], atts, paType.isUnsigned());
        }

        // leave "define" mode
        ncWriter = nc.build();

        // write the axis variables
        for (int a = 0; a < nActiveAxes; a++) {
          ncWriter.write(newAxisVars[a].getFullName(), axisArrays[a]);
        }

        // write the data variables
        for (int dv = 0; dv < tDataVariables.length; dv++) {

          EDV edv = tDataVariables[dv];
          PAType edvPAType = edv.destinationDataPAType();
          PrimitiveArray pa = gdaa.getPrimitiveArray(dv);
          if (nc3Mode && (pa instanceof LongArray || pa instanceof ULongArray))
            pa = new DoubleArray(pa);
          Array array =
              Array.factory(NcHelper.getNc3DataType(edvPAType), stdShape, pa.toObjectArray());
          Variable newVar =
              ncWriter.findVariable(
                  newVars[dv].getFullName()); // because newVars are Variable.Builder's
          if (nc3Mode && edvPAType == PAType.STRING) {
            ncWriter.writeStringDataToChar(newVar, array);
          } else {
            ncWriter.write(newVar, array);
          }
        }

        // if close throws Throwable, it is trouble
        ncWriter.close(); // it calls flush() and doesn't like flush called separately
        ncWriter = null;

        // rename the file to the specified name
        File2.rename(fullFileName + randomInt, fullFileName);

        // diagnostic
        if (reallyVerbose)
          String2.log(
              "  EDDGrid.saveAsNc done.  TIME=" + (System.currentTimeMillis() - time) + "ms\n");
        // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

      } catch (Throwable t) {
        String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
        if (ncWriter != null) {
          try {
            ncWriter.abort();
          } catch (Exception e9) {
          }
          File2.delete(fullFileName + randomInt);
          ncWriter = null;
        }

        throw t;
      }
    }
  }

  /**
   * This gets the axis data for the userDapQuery and writes the data to a tableWriter. This writes
   * the lon values as they are in the source (e.g., +-180 or 0..360). If no exception is thrown,
   * the data was successfully written.
   *
   * @param ada The source of data to be written to the tableWriter.
   * @param tw This calls tw.finish() at the end.
   * @throws Throwable if trouble.
   */
  public void saveAsTableWriter(AxisDataAccessor ada, TableWriter tw) throws Throwable {

    // make the table
    // note that TableWriter expects time values as doubles,
    //  and (sometimes) displays them as ISO 8601 strings
    Table table = new Table();
    table.globalAttributes().add(ada.globalAttributes());
    int nRAV = ada.nRequestedAxisVariables();
    for (int av = 0; av < nRAV; av++) {
      table.addColumn(
          av, ada.axisVariables(av).destinationName(), ada.axisValues(av), ada.axisAttributes(av));
    }
    table.makeColumnsSameSize();

    // write the table
    tw.writeAllAndFinish(table);
  }

  /**
   * This gets the data for the userDapQuery and writes the data to a TableWriter. This writes the
   * lon values as they are in the source (e.g., +-180 or 0..360). //note that TableWriter expects
   * time values as doubles, and displays them as ISO 8601 strings If no exception is thrown, the
   * data was successfully written.
   *
   * @param gridDataAccessor The source of data to be written to the tableWriter. Missing values
   *     should be as they are in the source. Some tableWriter's convert them to other values (e.g.,
   *     NaN).
   * @param tw This calls tw.finish() at the end.
   * @throws Throwable if trouble.
   */
  public void saveAsTableWriter(GridDataAccessor gridDataAccessor, TableWriter tw)
      throws Throwable {

    // create the table (with one dummy row of data)
    Table table = new Table();
    int nAv = axisVariables.length;
    EDV queryDataVariables[] = gridDataAccessor.dataVariables();
    int nDv = queryDataVariables.length;
    PrimitiveArray avPa[] = new PrimitiveArray[nAv];
    PrimitiveArray dvPa[] = new PrimitiveArray[nDv];
    int nBufferRows = tableWriterNBufferRows;
    PAOne avPAOne[] = new PAOne[nAv];
    for (int av = 0; av < nAv; av++) {
      EDV edv = axisVariables[av];
      PAType tPAType = edv.destinationDataPAType();
      avPAOne[av] = new PAOne(tPAType);
      avPa[av] = PrimitiveArray.factory(tPAType, nBufferRows, false);
      // ???need to remove file-specific metadata (e.g., actual_range) from Attributes clone?
      table.addColumn(
          av,
          edv.destinationName(),
          avPa[av],
          gridDataAccessor.axisAttributes(av)); // (Attributes)(edv.combinedAttributes().clone());
    }
    PAOne dvPAOne[] = new PAOne[nDv];
    for (int dv = 0; dv < nDv; dv++) {
      EDV edv = queryDataVariables[dv];
      PAType tPAType = edv.destinationDataPAType();
      dvPAOne[dv] = new PAOne(tPAType);
      dvPa[dv] = PrimitiveArray.factory(tPAType, nBufferRows, false);
      // ???need to remove file-specific metadata (e.g., actual_range) from Attributes clone?
      table.addColumn(
          nAv + dv,
          edv.destinationName(),
          dvPa[dv],
          gridDataAccessor.dataAttributes(dv)); // (Attributes)(edv.combinedAttributes().clone());
    }

    // write the data
    int tRows = 0;
    while (gridDataAccessor.increment()) {
      // add a row of data to the table
      for (int av = 0; av < nAv; av++)
        gridDataAccessor.getAxisValueAsPAOne(av, avPAOne[av]).addTo(avPa[av]);

      for (int dv = 0; dv < nDv; dv++)
        gridDataAccessor.getDataValueAsPAOne(dv, dvPAOne[dv]).addTo(dvPa[dv]);

      tRows++;

      // write the table
      if (tRows >= nBufferRows) {
        tw.writeSome(table);
        table.removeAllRows();
        tRows = 0;
        if (tw.noMoreDataPlease) {
          tw.logCaughtNoMoreDataPlease(datasetID);
          break;
        }
      }
    }
    if (tRows > 0) tw.writeSome(table);
    tw.finish();
  }

  /**
   * This writes an HTML form requesting info from this dataset (like the OPeNDAP Data Access
   * forms).
   *
   * @param request the request
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but it unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
   * @param writer
   * @throws Throwable if trouble
   */
  public void writeDapHtmlForm(
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String userDapQuery,
      Writer writer)
      throws Throwable {

    // parse userDapQuery
    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    if (userDapQuery == null) userDapQuery = "";
    userDapQuery = userDapQuery.trim();
    StringArray destinationNames = new StringArray();
    IntArray constraints = new IntArray();
    boolean isAxisDapQuery = false; // only true if userDapQuery.length() > 0 and is axisDapQuery
    if (userDapQuery.length() > 0) {
      try {
        isAxisDapQuery = isAxisDapQuery(userDapQuery);
        if (isAxisDapQuery)
          parseAxisDapQuery(language, userDapQuery, destinationNames, constraints, true);
        else parseDataDapQuery(language, userDapQuery, destinationNames, constraints, true);
      } catch (Throwable t) {
        EDStatic.rethrowClientAbortException(t); // first thing in catch{}
        String2.log(MustBe.throwableToString(t));
        userDapQuery = ""; // as if no userDapQuery
      }
    }

    // beginning of form   ("form1" is used in javascript below")
    writer.write(HtmlWidgets.ifJavaScriptDisabled + "\n");
    HtmlWidgets widgets =
        new HtmlWidgets(true, EDStatic.imageDirUrl(request, loggedInAs, language));
    String formName = "form1";
    writer.write("&nbsp;\n"); // necessary for the blank line before the form (not <p>)
    writer.write(widgets.beginForm(formName, "GET", "", ""));

    // begin table
    writer.write(widgets.beginTable("class=\"compact nowrap\""));

    // write the table's column names
    String dimHelp = EDStatic.messages.EDDGridDimensionTooltipAr[language] + "\n<br>";
    String sss = dimHelp + EDStatic.messages.EDDGridSSSTooltipAr[language] + "\n<br>";
    String startTooltip = sss + EDStatic.messages.EDDGridStartTooltipAr[language];
    String stopTooltip = sss + EDStatic.messages.EDDGridStopTooltipAr[language];
    String strideTooltip = sss + EDStatic.messages.EDDGridStrideTooltipAr[language];
    String downloadTooltip = EDStatic.messages.EDDGridDownloadTooltipAr[language];
    String gap = "&nbsp;&nbsp;&nbsp;";
    writer.write(
        "<tr>\n"
            + "  <th class=\"L\">"
            + EDStatic.messages.EDDGridDimensionAr[language]
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                dimHelp + EDStatic.messages.EDDGridVarHasDimTooltipAr[language])
            + " </th>\n"
            + "  <th class=\"L\">"
            + EDStatic.messages.EDDGridStartAr[language]
            + " "
            + EDStatic.htmlTooltipImage(request, language, loggedInAs, startTooltip)
            + " </th>\n"
            + "  <th class=\"L\">"
            + EDStatic.messages.EDDGridStrideAr[language]
            + " "
            + EDStatic.htmlTooltipImage(request, language, loggedInAs, strideTooltip)
            + " </th>\n"
            + "  <th class=\"L\">"
            + EDStatic.messages.EDDGridStopAr[language]
            + " "
            + EDStatic.htmlTooltipImage(request, language, loggedInAs, stopTooltip)
            + " </th>\n"
            +
            // "  <th class=\"L\">&nbsp;" + EDStatic.messages.EDDGridFirst + " " +
            //    EDStatic.htmlTooltipImage(language, loggedInAs,
            // EDStatic.messages.EDDGridDimensionFirstTooltip) + "</th>\n" +
            "  <th class=\"L\">&nbsp;"
            + EDStatic.messages.EDDGridNValuesAr[language]
            + " "
            + EDStatic.htmlTooltipImage(
                request, language, loggedInAs, EDStatic.messages.EDDGridNValuesHtmlAr[language])
            + "</th>\n"
            + "  <th class=\"L\">"
            + gap
            + EDStatic.messages.EDDGridSpacingAr[language]
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.EDDGridSpacingTooltipAr[language]
                    + "</div>")
            + "</th>\n"
            +
            // "  <th class=\"L\">" + gap + EDStatic.messages.EDDGridLast + " " +
            //    EDStatic.htmlTooltipImage(language, loggedInAs,
            // EDStatic.messages.EDDGridDimensionLastTooltipAr[language]) + "</th>\n" +
            "</tr>\n");

    // a row for each axisVariable
    int nAv = axisVariables.length;
    String sliderFromNames[] = new String[nAv];
    String sliderToNames[] = new String[nAv];
    int sliderNThumbs[] = new int[nAv];
    String sliderUserValuesCsvs[] = new String[nAv];
    int sliderInitFromPositions[] = new int[nAv];
    int sliderInitToPositions[] = new int[nAv];
    for (int av = 0; av < nAv; av++) {
      EDVGridAxis edvga = axisVariables[av];
      int sourceSize = edvga.sourceValues().size();
      writer.write("<tr>\n");

      // get the extra info
      String extra = edvga.units();
      if (edvga instanceof EDVTimeStampGridAxis)
        extra = "UTC"; // no longer true: "seconds since 1970-01-01..."
      if (extra == null) extra = "";
      if (showLongName(edvga.destinationName(), edvga.longName(), extra, 25))
        extra = edvga.longName() + (extra.length() == 0 ? "" : ", " + extra);
      if (extra.length() > 0) extra = " (" + extra + ")";

      // variables: checkbox destName (longName, extra)
      writer.write("  <td>\n");
      writer.write(
          widgets.checkbox(
              "avar" + av,
              downloadTooltip,
              userDapQuery.length() <= 0
                  || !isAxisDapQuery
                  || destinationNames.indexOf(edvga.destinationName()) >= 0,
              edvga.destinationName(),
              edvga.destinationName(),
              ""));

      writer.write(extra + " ");
      writer.write(EDStatic.htmlTooltipImageEDVGA(request, language, loggedInAs, edvga));
      writer.write("&nbsp;</td>\n");

      // set default start, stride, stop
      int tStarti = av == timeIndex ? sourceSize - 1 : 0;
      int tStopi = sourceSize - 1;
      double tdv =
          av == timeIndex
              ? edvga.destinationMaxDouble()
              : // yes, time max, to limit time
              edvga.firstDestinationValue();
      String tStart = edvga.destinationToString(tdv);
      String tStride = "1";
      tdv = av == timeIndex ? edvga.destinationMaxDouble() : edvga.lastDestinationValue();
      String tStop = edvga.destinationToString(tdv);

      // if possible, overwrite defaults via userDapQuery
      if (userDapQuery.length() > 0) {
        int tAv = isAxisDapQuery ? destinationNames.indexOf(edvga.destinationName()) : av;
        if (tAv >= 0) {
          tStarti = constraints.get(3 * tAv + 0);
          tStopi = constraints.get(3 * tAv + 2);
          tStart = edvga.destinationToString(edvga.destinationValue(tStarti).getNiceDouble(0));
          tStride = "" + constraints.get(3 * tAv + 1);
          tStop = edvga.destinationToString(edvga.destinationValue(tStopi).getNiceDouble(0));
        }
      }

      // start
      String edvgaTooltip = edvga.htmlRangeTooltip(language);
      writer.write("  <td>");
      // new style: textField
      writer.write(widgets.textField("start" + av, edvgaTooltip, 22, 40, tStart, ""));
      writer.write("  </td>\n");

      // stride
      writer.write("  <td>"); // + gap);
      writer.write(widgets.textField("stride" + av, edvgaTooltip, 7, 20, tStride, ""));
      writer.write(
          "  " + // gap +
              "</td>\n");

      // stop
      writer.write("  <td>");
      // new style: textField
      writer.write(widgets.textField("stop" + av, edvgaTooltip, 22, 40, tStop, ""));
      writer.write("  </td>\n");

      // n Values
      writer.write("  <td>" + gap + sourceSize + "</td>\n");

      // spacing
      writer.write("  <td>" + gap + edvga.spacingDescription(language) + "</td>\n");

      // end of row
      writer.write("</tr>\n");

      // *** and a slider for this axis  (Data Access Form)
      sliderFromNames[av] = formName + ".start" + av;
      sliderToNames[av] = formName + ".stop" + av;
      int safeSourceSize1 = Math.max(1, sourceSize - 1);
      // if (sourceSize == 1) {
      //    sliderNThumbs[av] = 0;
      //    sliderUserValuesCsvs[av] = "";
      //    sliderInitFromPositions[av] = 0;
      //    sliderInitToPositions[av] = 0;
      // } else {
      sliderNThumbs[av] = 2;
      sliderUserValuesCsvs[av] = edvga.sliderCsvValues();
      sliderInitFromPositions[av] =
          Math2.roundToInt((tStarti * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
      sliderInitToPositions[av] =
          sourceSize == 1
              ? EDV.SLIDER_PIXELS - 1
              : Math2.roundToInt((tStopi * (EDV.SLIDER_PIXELS - 1.0)) / safeSourceSize1);
      writer.write(
          "<tr>\n"
              + "  <td colspan=\"6\">\n"
              + widgets.spacer(10, 1, "")
              + // was "style=\"text-align:left\"") +
              widgets.dualSlider(av, EDV.SLIDER_PIXELS - 1, "")
              + // was "style=\"text-align:left\"") +
              "  </td>\n"
              + "</tr>\n");
      // }
    }
    writer.write(widgets.endTable());

    // table for data variables
    writer.write(
        widgets.beginTable("class=\"compact nowrap\"")
            + "<tr>\n"
            + "  <td>&nbsp;<br>"
            + EDStatic.messages.EDDGridGridVariableHtmlAr[language]
            + "&nbsp;");

    StringBuilder checkAll = new StringBuilder();
    StringBuilder uncheckAll = new StringBuilder();
    int nDv = dataVariables.length;
    for (int dv = 0; dv < nDv; dv++) {
      checkAll.append(formName + ".dvar" + dv + ".checked=true;");
      uncheckAll.append(formName + ".dvar" + dv + ".checked=false;");
    }

    writer.write(
        widgets.button(
            "button",
            "CheckAll",
            EDStatic.messages.EDDGridCheckAllTooltipAr[language],
            EDStatic.messages.EDDGridCheckAllAr[language],
            "onclick=\"" + checkAll + "\""));
    writer.write(
        widgets.button(
            "button",
            "UncheckAll",
            EDStatic.messages.EDDGridUncheckAllTooltipAr[language],
            EDStatic.messages.EDDGridUncheckAllAr[language],
            "onclick=\"" + uncheckAll + "\""));

    writer.write("</td></tr>\n");

    // a row for each dataVariable
    for (int dv = 0; dv < dataVariables.length; dv++) {
      EDV edv = dataVariables[dv];
      writer.write("<tr>\n");

      // get the extra info
      String extra = edv.units();
      if (extra == null) extra = "";
      if (showLongName(edv.destinationName(), edv.longName(), extra, 80))
        extra = edv.longName() + (extra.length() == 0 ? "" : ", " + extra);
      if (extra.length() > 0) extra = " (" + extra + ")";

      // variables: checkbox destName (longName, units)
      writer.write("  <td>\n");
      writer.write(
          widgets.checkbox(
              "dvar" + dv,
              downloadTooltip,
              (userDapQuery.length() <= 0 || !isAxisDapQuery)
                  && (userDapQuery.length() <= 0
                      || destinationNames.indexOf(edv.destinationName()) >= 0),
              edv.destinationName(),
              edv.destinationName(),
              ""));

      writer.write(extra + " ");
      writer.write(
          EDStatic.htmlTooltipImageEDVG(request, language, loggedInAs, edv, allDimString()));
      writer.write("</td>\n");

      // end of row
      writer.write("</tr>\n");
    }

    // end of table
    writer.write(widgets.endTable());

    // fileType
    writer.write(
        "<p><strong>"
            + EDStatic.messages.EDDFileTypeAr[language]
            + "</strong>\n"
            + " (<a rel=\"help\" href=\""
            + tErddapUrl
            + "/griddap/documentation.html#fileType\">"
            + EDStatic.messages.moreInformationAr[language]
            + "</a>)\n");
    List<String> fileTypeDescriptions =
        EDD_FILE_TYPE_INFO.values().stream()
            .filter(fileTypeInfo -> fileTypeInfo.getAvailableGrid())
            .map(
                fileTypeInfo ->
                    fileTypeInfo.getFileTypeName()
                        + " - "
                        + fileTypeInfo.getGridDescription(language))
            .toList();
    int defaultIndex =
        EDD_FILE_TYPE_INFO.values().stream()
            .filter(fileTypeInfo -> fileTypeInfo.getAvailableGrid())
            .map(fileTypeInfo -> fileTypeInfo.getFileTypeName())
            .toList()
            .indexOf(defaultFileTypeOption);
    writer.write(
        widgets.select(
            "fileType",
            EDStatic.messages.EDDSelectFileTypeAr[language],
            1,
            fileTypeDescriptions,
            defaultIndex,
            ""));

    // generate the javaScript
    String javaScript =
        "var result = \"\";\n"
            + "try {\n"
            + "  var ft = form1.fileType.options[form1.fileType.selectedIndex].text;\n"
            + "  var start = \""
            + tErddapUrl
            + "/griddap/"
            + datasetID
            + "\" + ft.substring(0, ft.indexOf(\" - \")) + \"?\";\n"
            + "  var sss = new Array(); var cum = \"\"; var done = false;\n"
            +

            // gather startStrideStop and cumulative sss.   %5B=[ %5D=] %7C=|
            "  for (var av = 0; av < "
            + nAv
            + "; av++) {\n"
            + "    sss[av] = \"%5B(\" + "
            + "eval(\"form1.start\"  + av + \".value\") + \"):\" + "
            +
            // "eval(\"form1.start\"  + av + \".options[form1.start\"  + av +
            // \".selectedIndex].text\") + \"):\" + " +
            "eval(\"form1.stride\" + av + \".value\") + \":(\" + "
            + "eval(\"form1.stop\"   + av + \".value\") + \")%5D\";\n"
            +
            // "eval(\"form1.stop\"   + av + \".options[form1.stop\"   + av +
            // \".selectedIndex].text\") + \")]\";\n" +
            "    cum += sss[av];\n"
            + "  }\n"
            +

            // see if any dataVars were selected
            "  for (var dv = 0; dv < "
            + dataVariables.length
            + "; dv++) {\n"
            + "    if (eval(\"form1.dvar\" + dv + \".checked\")) {\n"
            + "      if (result.length > 0) result += \",\";\n"
            + "      result += (eval(\"form1.dvar\" + dv + \".value\")) + cum; }\n"
            + "  }\n"
            +

            // else find selected axes
            "  if (result.length > 0) {\n"
            + "    result = start + result;\n"
            + "  } else {\n"
            + "    result = start;\n"
            + "    for (var av = 0; av < "
            + nAv
            + "; av++) {\n"
            + "      if (eval(\"form1.avar\" + av + \".checked\")) { \n"
            + "        if (result.length > start.length) result += \",\";\n"
            + "        result += (eval(\"form1.avar\" + av + \".value\")) + sss[av];\n"
            + "      }\n"
            + "    }\n"
            + "    if (result.length == start.length) { result = \"\";\n"
            + "      throw new Error(\"Please select (check) one of the variables.\"); }\n"
            + "  }\n"
            + "} catch (e) {alert(e);}\n"
            + // result is in 'result'  (or "" if error or nothing selected)
            "form1.tUrl.value = result;\n";

    // just generate URL
    writer.write("<br>");
    String genViewHtml =
        "<div class=\"standard_max_width\">"
            + String2.replaceAll(
                EDStatic.messages.justGenerateAndViewTooltipAr[language],
                "&protocolName;",
                dapProtocol)
            + "</div>";
    writer.write(
        widgets.button(
            "button",
            "getUrl",
            genViewHtml,
            EDStatic.messages.justGenerateAndViewAr[language],
            // "class=\"skinny\" " + //only IE needs it but only IE ignores it
            "onclick='" + javaScript + "'"));
    writer.write(
        widgets.textField(
            "tUrl", EDStatic.messages.justGenerateAndViewUrlAr[language], 60, 1000, "", ""));
    writer.write(
        "\n<br>(<a rel=\"help\" href=\""
            + tErddapUrl
            + "/griddap/documentation.html\" "
            + "title=\"griddap documentation\">Documentation&nbsp;/&nbsp;Bypass&nbsp;this&nbsp;form</a>)\n"
            + EDStatic.htmlTooltipImage(request, language, loggedInAs, genViewHtml));

    // submit
    writer.write(
        "<br>&nbsp;\n"
            + "<br>"
            + widgets.htmlButton(
                "button",
                "submit1",
                "",
                EDStatic.messages.submitTooltipAr[language],
                "<span style=\"font-size:large;\"><strong>"
                    + EDStatic.messages.submitAr[language]
                    + "</strong></span>",
                "onclick='"
                    + javaScript
                    + "if (result.length > 0) window.location=result;\n"
                    + // or open a new window: window.open(result);\n" +
                    "'")
            + " "
            + EDStatic.messages.patientDataAr[language]
            + "\n");

    // end of form
    writer.write(widgets.endForm());
    writer.write("<br>&nbsp;\n");

    // the javascript for the sliders
    writer.write(
        widgets.sliderScript(
            sliderFromNames,
            sliderToNames,
            sliderNThumbs,
            sliderUserValuesCsvs,
            sliderInitFromPositions,
            sliderInitToPositions,
            EDV.SLIDER_PIXELS - 1));

    writer.flush(); // be nice
  }

  /**
   * This writes HTML info on forming OPeNDAP DAP-style requests for this type of dataset.
   *
   * @param language the index of the selected language
   * @param tErddapUrl from EDStatic.erddapUrl(request, loggedInAs, language) (erddapUrl, or
   *     erddapHttpsUrl if user is logged in)
   * @param writer to which will be written HTML info on forming OPeNDAP DAP-style requests for this
   *     type of dataset.
   * @param complete if false, this just writes a paragraph and shows a link to
   *     [protocol]/documentation.html
   * @throws Throwable if trouble
   */
  public static void writeGeneralDapHtmlInstructions(
      int language, String tErddapUrl, Writer writer, boolean complete) throws Throwable {

    String dapBase = EDStatic.messages.EDDGridErddapUrlExample + dapProtocol + "/";
    String datasetBase = dapBase + EDStatic.messages.EDDGridIdExample;
    String ddsExample = datasetBase + ".dds";
    String dds1VarExample = datasetBase + ".dds?" + EDStatic.messages.EDDGridNoHyperExample;

    // variants encoded to be Html Examples
    String fullDimensionExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDimensionExampleHE;
    String fullIndexExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDataIndexExampleHE;
    String fullValueExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDataValueExampleHE;
    String fullTimeExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDataTimeExampleHE;
    String fullTimeCsvExampleHE =
        datasetBase + ".csv?" + EDStatic.messages.EDDGridDataTimeExampleHE;
    String fullTimeNcExampleHE = datasetBase + ".nc?" + EDStatic.messages.EDDGridDataTimeExampleHE;
    String fullMatExampleHE = datasetBase + ".mat?" + EDStatic.messages.EDDGridDataTimeExampleHE;
    String fullGraphExampleHE = datasetBase + ".png?" + EDStatic.messages.EDDGridGraphExampleHE;
    String fullGraphMAGExampleHE =
        datasetBase + ".graph?" + EDStatic.messages.EDDGridGraphExampleHE;
    String fullGraphDataExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridGraphExampleHE;
    String fullMapExampleHE = datasetBase + ".png?" + EDStatic.messages.EDDGridMapExampleHE;
    String fullMapMAGExampleHE = datasetBase + ".graph?" + EDStatic.messages.EDDGridMapExampleHE;
    String fullMapDataExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridMapExampleHE;

    // variants encoded to be Html Attributes
    String fullDimensionExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDimensionExampleHA;
    String fullIndexExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDataIndexExampleHA;
    String fullValueExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDataValueExampleHA;
    String fullTimeExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridDataTimeExampleHA;
    String fullGraphExampleHA = datasetBase + ".png?" + EDStatic.messages.EDDGridGraphExampleHA;
    String fullGraphMAGExampleHA =
        datasetBase + ".graph?" + EDStatic.messages.EDDGridGraphExampleHA;
    String fullGraphDataExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridGraphExampleHA;
    String fullMapExampleHA = datasetBase + ".png?" + EDStatic.messages.EDDGridMapExampleHA;
    String fullMapMAGExampleHA = datasetBase + ".graph?" + EDStatic.messages.EDDGridMapExampleHA;
    String fullMapDataExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDGridMapExampleHA;

    writer.write(
        "<h2><a class=\"selfLink\" id=\"instructions\" href=\"#instructions\" rel=\"bookmark\">"
            + EDStatic.messages.usingGriddapAr[language]
            + "</a></h2>\n"
            + longDapDescription(language, tErddapUrl)
            + "<p><strong>griddap request URLs must be in the form</strong>\n"
            + "<br><kbd>"
            + dapBase
            + "<i><a rel=\"help\" href=\""
            + dapBase
            + "documentation.html#datasetID\">datasetID</a></i>."
            + "<i><a rel=\"help\" href=\""
            + dapBase
            + "documentation.html#fileType\">fileType</a></i>{?"
            + "<i><a rel=\"help\" href=\""
            + dapBase
            + "documentation.html#query\">query</a></i>}</kbd>\n"
            + "<br>For example,\n"
            + "<br><a href=\""
            + fullTimeExampleHA
            + "\"><kbd>"
            + fullTimeExampleHE
            + "</kbd></a>\n"
            + "<br>Thus, the query is often a data variable name (e.g., <kbd>"
            + EDStatic.messages.EDDGridNoHyperExample
            + "</kbd>),\n"
            + "followed by <kbd>[(<i>start</i>):<i>stride</i>:(<i>stop</i>)]</kbd>\n"
            + "(or a shorter variation of that) for each of the variable's dimensions\n"
            + "(for example, <kbd>"
            + EDStatic.messages.EDDGridDimNamesExample
            + "</kbd>). \n"
            + "\n");

    if (!complete) {
      writer.write(
          "<p>For details, see the <a rel=\"help\" href=\""
              + tErddapUrl
              + "/griddap/"
              + "documentation.html\">"
              + dapProtocol
              + " Documentation</a>.\n");
      return;
    }

    // details
    writer.write(
        "<p><strong>Details:</strong><ul>\n"
            + "<li>Requests must not have any internal spaces.\n"
            + "<li>Requests are case sensitive.\n"
            + "<li>{} is notation to denote an optional part of the request.\n"
            + "  <br>&nbsp;\n"
            +

            // datasetID
            "<li><a class=\"selfLink\" id=\"datasetID\" href=\"#datasetID\" rel=\"bookmark\""
            + "><strong>datasetID</strong></a> identifies the name that ERDDAP\n"
            + "  assigned to the dataset (for example, <kbd>"
            + EDStatic.messages.EDDGridIdExample
            + "</kbd>). \n"
            + "  You can see a list of "
            + "<a rel=\"bookmark\" href=\""
            + tErddapUrl
            + "/"
            + dapProtocol
            + "/index.html\">datasetID options available via griddap</a>.\n"
            + "  <br>&nbsp;\n"
            +

            // fileType
            "<li><a class=\"selfLink\" id=\"fileType\" href=\"#fileType\" rel=\"bookmark\" \n"
            + "><strong>fileType</strong></a> specifies the type of grid data file that you want "
            + "  to download (for example, <kbd>.htmlTable</kbd>).\n"
            + "  The actual extension of the resulting file may be slightly different than the fileType (for example,\n"
            + "  <kbd>.htmlTable</kbd> returns an .html file). \n"
            +

            // Which File Type?
            "  <p>Which fileType should I use?\n"
            + "  <br>It's entirely up to you. In general, pick the fileType which meets your needs and is easiest to use. You will\n"
            + "  use different fileTypes in different situations, e.g., viewing a graph in a browser (.png) vs. viewing the data\n"
            + "  in a browser (.htmlTable) vs. downloading a data file (e.g., .nc or .csv) vs. working with some software tool\n"
            + "  (e.g., .nc or .odv). If you are going to download lots of large files, you might also want to give some weight\n"
            + "  to fileTypes that are more compact and so can be downloaded faster.\n"
            + "  <p>The fileType options for downloading gridded data are:\n"
            + "  <br>&nbsp;\n"
            + "  <table class=\"erd\" style=\"width:100%; \">\n"
            + "    <tr><th>Data<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
    List<EDDFileTypeInfo> dataFileTypes = EDD.getFileTypeOptions(true, false);
    for (int i = 0; i < dataFileTypes.size(); i++) {
      EDDFileTypeInfo curType = dataFileTypes.get(i);
      String ft = curType.getFileTypeName();
      String ft1 = ft.substring(1);
      writer.write(
          "    <tr>\n"
              + "      <td><a class=\"selfLink\" id=\"fileType_"
              + ft1
              + "\" href=\"#fileType_"
              + ft1
              + "\" rel=\"bookmark\">"
              + ft
              + "</a></td>\n"
              + "      <td>"
              + curType.getGridDescription(language)
              + "</td>\n"
              + "      <td class=\"N\">"
              + (curType.getInfoUrl().isEmpty()
                  ? "&nbsp;"
                  : "<a rel=\"help\" href=\""
                      + XML.encodeAsHTMLAttribute(curType.getInfoUrl())
                      + "\">info"
                      + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
                      + "</a>")
              + "</td>\n"
              + "      <td class=\"N\"><a rel=\"bookmark\" href=\""
              + datasetBase
              + curType.getFileTypeName()
              + "?"
              + EDStatic.messages.EDDGridDataTimeExampleHA
              + "\">example</a></td>\n"
              + "    </tr>\n");
    }
    writer.write(
        "   </table>\n"
            + "   <br>For example, here is a request URL to download data formatted as an HTML table:\n"
            + "   <br><a rel=\"bookmark\" href=\""
            + fullTimeExampleHA
            + "\"><kbd>"
            + fullTimeExampleHE
            + "</kbd></a>\n"
            + "\n"
            +

            // ArcGIS
            "<p><strong><a rel=\"bookmark\" href=\"https://www.esri.com/en-us/arcgis/about-arcgis/overview\">ArcGIS"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a><a class=\"selfLink\" id=\"ArcGIS\" href=\"#ArcGIS\" rel=\"bookmark\">&nbsp;</a>\n"
            + "     <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Esri_grid\">.esriAsc"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "   <br>.esriAsc is an old and inherently limited file format. If you have <strong>ArcGIS 10 or higher</strong>, we strongly recommend\n"
            + "   that you download gridded data from ERDDAP in a <a rel=\"help\" href=\"#nc\">NetCDF .nc file</a>,"
            + "     which can be opened directly by ArcGIS 10+\n"
            + "   using the\n"
            + "   <a rel=\"help\" href=\"https://desktop.arcgis.com/en/arcmap/latest/tools/multidimension-toolbox/make-netcdf-raster-layer.htm\">Make\n"
            + "     NetCDF Raster Layer tool in the Multidimension Tools toolbox"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "   <p>If you have <strong>ArcGIS 9.x or lower</strong>:\n"
            + "   <br>ArcGIS is a family of Geographical Information Systems (GIS) products from ESRI: ArcView, ArcEditor, and ArcInfo.\n"
            + "   To download data for use with ArcGIS 9.x or lower:\n"
            + "   in ERDDAP, choose the .esriAscii file type to save a latitude longitude subset of data for just one point in time.\n"
            + "   The file's extension will be .asc.  This file format was designed by ESRI to transfer coverage data between computers.\n"
            + "   Then, import the data file into your ArcGIS program:\n"
            + "   <ul>\n"
            + "   <li>For ArcInfo v8 or higher:\n"
            + "     <ol>\n"
            + "     <li>Open ArcInfo.\n"
            + "     <li>Use ArcToolbox.\n"
            + "     <li>Use the <kbd>Import to Raster : ASCII to Grid</kbd> command \n"
            + "       <br>&nbsp;\n"
            + "     </ol>\n"
            + "   <li>For ArcView's or ArcEditor's ArcMap:\n"
            + "     <ol>\n"
            + "     <li>Open ArcMap.\n"
            + "     <li>Choose a new empty map or load an existing map.\n"
            + "     <li>Click on the ArcToolbox icon.\n"
            + "       <ol>\n"
            + "       <li>Open <kbd>Conversion Tools : To Raster : ASCII to Raster</kbd>\n"
            + "         <ol>\n"
            + "         <li><kbd>Input Raster File</kbd> - Browse to select the .esriAscii .asc file.\n"
            + "         <li><kbd>Output Raster File</kbd> - Keep the default (or change it).\n"
            + "         <li><kbd>Output Data Type</kbd> - change to FLOAT.\n"
            + "         <li>Click on <kbd>OK</kbd>.\n"
            + "         </ol>\n"
            + "       <li>Open <kbd>Data Management Tools : Projections and Transformations : Define Projection</kbd>\n"
            + "         <ol>\n"
            + "         <li>Input Dataset or Feature Class - Browse to select the Raster (GRID) that was just\n"
            + "           created (on disk).\n"
            + "         <li>Coordinate System - click the icon to the right of the text box.\n"
            + "           That will open the Spatial Reference Properties window.\n"
            + "           <ol>\n"
            + "           <li>Click the <kbd>Select</kbd> button.\n"
            + "           <li>Choose the <kbd>Geographic Coordinate Systems</kbd> folder. (Most data in ERDDAP uses a\n"
            + "             'geographic' projection, otherwise known as unprojected data or lat/lon data).\n"
            + "           <li>Select on the <kbd>Spheroid</kbd>-based folder.\n"
            + "           <li>Select the <kbd>Clarke 1866.prj</kbd> file and click <kbd>Add</kbd>.\n"
            + "           <li>Click <kbd>OK</kbd> in the Spatial Reference Properties Window.\n"
            + "           <li>Click <kbd>OK</kbd> in the Define Projection Window.\n"
            + "             <br>&nbsp;\n"
            + "           </ol>\n"
            + "         </ol>\n"
            + "       </ol>\n"
            + "     </ol>\n"
            + "   <li>For older ArcView (v3) - You need the Spatial Analyst extension, which is sold separately.\n"
            + "     <ol>\n"
            + "     <li>Open ArcView\n"
            + "     <li>Open Spatial Analyst\n"
            + "     <li>Use <kbd>Import Data Source : ASCII Raster</kbd> \n"
            + "     </ol>\n"
            + "  </ul>\n"
            + "\n"
            + "  <p>Shapefiles - Sorry, ERDDAP currently does not distribute grid data as shapefiles.\n"
            + "\n"
            +
            // Ferret
            "  <p><strong><a rel=\"bookmark\" href=\"https://ferret.pmel.noaa.gov/Ferret/\">Ferret"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"Ferret\" href=\"#Ferret\" rel=\"bookmark\">is</a> a free program for visualizing and analyzing large and complex gridded\n"
            + "  datasets. Ferret should work well with all datasets in griddap since griddap is\n"
            + "  fully compatible with OPeNDAP. See the\n"
            + "    <a rel=\"help\" href=\"https://ferret.pmel.noaa.gov/Ferret/documentation/ferret-documentation\">Ferret documentation"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  Note that the griddap dataset's OPeNDAP base URL that you use with Ferret's\n"
            + "  <kbd>set data</kbd>, for example,\n"
            + "<br><kbd>"
            + datasetBase
            + "</kbd>\n"
            + "<br>won't ever have a file extension at the end.\n"
            + "\n"
            +
            // IDL
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.harrisgeospatial.com/Software-Technology/IDL/\">IDL"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong> - \n"
            + "    <a class=\"selfLink\" id=\"IDL\" href=\"#IDL\" rel=\"bookmark\">IDL</a> is a commercial scientific data visualization program. To get data from ERDDAP\n"
            + "  into IDL, first use ERDDAP to select a subset of data and download a .nc file.\n"
            + "  Then, use these\n"
            + "    <a rel=\"help\" href=\"https://northstar-www.dartmouth.edu/doc/idl/html_6.2/Using_Macros_to_Import_HDF_Files.html\">instructions"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    to import the data from the .nc file into IDL.\n"
            + "\n"
            +
            // json
            "  <p><strong><a rel=\"help\" href=\"https://www.json.org/\">JSON .json"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"json\" href=\"#json\" rel=\"bookmark\">files</a> are widely used to transfer data to JavaScript scripts running on web pages.\n"
            + "  All .json responses from ERDDAP (metadata, gridded data, and tabular/in-situ data) use the\n"
            + "  same basic format: a database-like table.  For data from grid datasets, ERDDAP flattens the data\n"
            + "  into a table with a column for each dimension and a column for each data variable. For example,\n"
            + "    <pre>\n"
            + "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"time\", \"latitude\", \"longitude\", \"analysed_sst\"],\n"
            + "    \"columnTypes\": [\"String\", \"float\", \"float\", \"double\"],\n"
            + "    \"columnUnits\": [\"UTC\", \"degrees_north\", \"degrees_east\", \"degree_C\"],\n"
            + "    \"rows\": [\n"
            + "      [\"2014-02-03T09:00:00Z\", 34.9969, -134.995, 16.037],\n"
            + "      [\"2014-02-03T09:00:00Z\", 34.9969, -134.984, 16.033],\n"
            + "      [\"2014-02-03T09:00:00Z\", 34.9969, -134.973, null],\n"
            + "      ...\n"
            + "      [\"2014-02-03T09:00:00Z\", 36.9965, -132.995, 15.285]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + "</pre>\n"
            + "      All .json responses from ERDDAP have\n"
            + "      <ul>\n"
            + "      <li>a <kbd>table</kbd> object (with name=value pairs).\n"
            + "      <li>a <kbd>columnNames, columnTypes,</kbd> and <kbd>columnUnits</kbd> array, with a value for each column.\n"
            + "      <li>a <kbd>rows</kbd> array of arrays with the rows and columns of data.\n"
            + "      <li><kbd>null</kbd>'s for missing values.\n"
            + "      </ul>\n"
            + "      Once you figure out how to process one ERDDAP .json table using your preferred JSON\n"
            + "      library or toolkit, it should be easy to process all other tables from ERDDAP in a similar way.\n"
            + "\n"
            +
            // jsonp
            "  <p><strong><a rel=\"help\" href=\"https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/\">JSONP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    (from <a href=\"https://www.json.org/\">.json"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>)</strong> -\n"
            + "  <a class=\"selfLink\" id=\"jsonp\" href=\"#jsonp\" rel=\"bookmark\">Jsonp</a> is an easy way for a JavaScript script on a web page to\n"
            + "  import and access data from ERDDAP.  Requests for .geoJson, .json, and .ncoJson files may include an optional\n"
            + "  jsonp request by adding <kbd>&amp;.jsonp=<i>functionName</i></kbd> to the end of the query.\n"
            + "  Basically, this just tells ERDDAP to add <kbd><i>functionName</i>(</kbd> to the beginning of the\n"
            + "  response and \")\" to the end of the response.\n"
            + "  The first character of <i>functionName</i> must be an ISO 8859 letter or \"_\".\n"
            + "  Each optional subsequent character must be an ISO 8859 letter, \"_\", a digit, or \".\".\n"
            + "  If originally there was no query, leave off the \"&amp;\" in your query.\n"
            + "  After the data download to the web page has finished, the data is accessible to the\n"
            + "  JavaScript script via that JavaScript function.\n"
            + "  Here is an example using \n"
            + "  <a rel=\"bookmark\" href=\"https://jsfiddle.net/jpatterson/0ycu1zjy/\">jsonp and Javascript with ERDDAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> (thanks to Jenn Patterson Sevadjian of PolarWatch).\n"
            + "\n"
            +
            // matlab
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.mathworks.com/products/matlab/\">MATLAB"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    <a rel=\"help\" href=\"https://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf\">.mat"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"matlab\" href=\"#matlab\" rel=\"bookmark\">users</a> can use griddap's .mat file type to download data from within MATLAB.\n"
            + "  Here is a one line example:\n"
            + "<pre>load(urlwrite('"
            + fullMatExampleHE
            + "', 'test.mat'));</pre>\n"
            + "  (You may need to <a rel=\"help\" href=\"#PercentEncoded\">percent encode</a> the query part of the URL.)\n"
            + "  The data will be in a MATLAB structure. The structure's name will be the datasetID\n"
            + "  (for example, <kbd>"
            + EDStatic.messages.EDDGridIdExample
            + "</kbd>). \n"
            + "  The structure's internal variables will have the same names as in ERDDAP,\n"
            + "  (for example, use <kbd>fieldnames("
            + EDStatic.messages.EDDGridIdExample
            + ")</kbd>). \n"
            + "  If you download a 2D matrix of data (as in the example above), you can plot it with\n"
            + "  (for example):\n"
            + "<pre>"
            + EDStatic.messages.EDDGridMatlabPlotExample
            + "</pre>\n"
            + "  The numbers at the end of the first line specify the range for the color mapping. \n"
            + "  The 'set' command flips the map to make it upright.\n"
            + "  <p>There are also Matlab\n"
            + "    <a rel=\"bookmark\" href=\"https://coastwatch.pfeg.noaa.gov/xtracto/\">Xtractomatic</a> scripts for ERDDAP,\n"
            + "    which are particularly useful for\n"
            + "  getting environmental data related to points along an animal's track (e.g.,\n"
            + "    <a rel=\"bookmark\" href=\"https://gtopp.org/\">GTOPP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> data).\n"
            + "  <p>ERDDAP stores datetime values in .mat files as \"seconds since 1970-01-01T00:00:00Z\".\n"
            + "  To display one of these values as a String in Matlab, you can use, e.g.,\n"
            + "  <br><kbd>datastr(cwwcNDBCMet.time(1)/86400 + 719529)</kbd>\n"
            + "  <br>86400 converts ERDDAP's \"seconds since\" to Matlab's \"days since\".  719529 converts\n"
            + "  ERDDAP's base time of \"1970-01-01T00:00:00Z\" to Matlab's \"0000-01-00T00:00:00Z\".\n"
            + "  <p>.mat files have a maximum length for identifier names of 32 characters.\n"
            + "  If a variable name is longer than that, ERDDAP changes the name of the variable\n"
            + "  when writing the .mat file: it generates a hash digest of the variable name and\n"
            + "  appends that after the first 25 characters. Thus, long variable names that only\n"
            + "  differ at the end will still have unique names in the .mat file.\n"
            + "  ERDDAP administrators: you can avoid this problem by specifying shorter variable\n"
            + "  destinationNames.\n"
            + "\n"
            +
            // nc
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\">NetCDF"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    <a rel=\"help\" href=\"https://github.com/Unidata/netcdf-c/blob/master/docs/file_format_specifications.md\">.nc"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    - <a class=\"selfLink\" id=\"nc\" href=\"#nc\" rel=\"bookmark\">Requests</a> for .nc files return the requested subset of the dataset in a\n"
            + "  standard, NetCDF-3, 32-bit, .nc file.\n"
            + "  <ul>\n"
            + "  <li>Since NetCDF-3 files don't support variable length strings and only support\n"
            + "    chars with 1 byte per char, all String variables in ERDDAP (variable length,\n"
            + "    2 bytes per char) will be stored as char array variables (1 byte per char)\n"
            + "    in the .nc file, using the ISO-8859-1 character set. The char variables will\n"
            + "    have an extra dimension indicating the maximum number of characters per string.\n"
            + "    Characters not in the ISO-8859-1 character set will be converted to '?'.\n"
            + "  <li>Since chars in NetCDF-3 files are only 1 byte per char, all char variables in\n"
            + "    ERDDAP (2 bytes per char) will be stored as chars (one byte per char) in the\n"
            + "    .nc file, using the ISO-8859-1 charset. Characters not in the ISO-8859-1\n"
            + "    character set will be converted to '?'.\n"
            + "  <li>Since NetCDF-3 files do not support 64-bit long integers, all long variables\n"
            + "    in ERDDAP will be stored as double variables in the .nc file. Numbers from\n"
            + "    -2^53 to 2^53 will be stored exactly. Other numbers will be stored as\n"
            + "    approximations.\n"
            + "  </ul>\n"
            + "\n"
            + "  <p><a class=\"selfLink\" id=\"netcdfjava\" href=\"#netcdfjava\" rel=\"bookmark\">If</a> you are using\n"
            + "  <a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf-java/\">NetCDF-Java"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  don't try to directly access an ERDDAP dataset or subset\n"
            + "  as a .nc file. (It won't work, mostly because that .nc file isn't a static, persistent file. It is a\n"
            + "  virtual file.)  Instead, use one of these two options:\n"
            + "  <ul>\n"
            + "  <li>Most situations: Open the ERDDAP dataset as an OPeNDAP dataset.  For example:\n"
            + "    <pre>NetcdfFile nc = NetcdfDataset.openFile(\""
            + datasetBase
            + "\", null);  //pre v5.4.1 </pre>\n"
            + "    or\n"
            + "    <pre>NetcdfFile nc = NetcdfDatasets.openFile(\""
            + datasetBase
            + "\", null); //v5.4.1+ </pre>\n"
            + "    (don't use <kbd>NetcdfFiles.open</kbd>; it is for local files only) or\n"
            + "    <pre>NetcdfDataset nc = NetcdfDataset.openDataset(\""
            + datasetBase
            + "\");  //pre v5.4.1</pre>\n"
            + "    or\n"
            + "    <pre>NetcdfDataset nc = NetcdfDatasets.openDataset(\""
            + datasetBase
            + "\");  //v5.4.1+</pre>\n"
            + "    (NetcdfFiles are a lower level approach than NetcdfDatasets.  It is your choice.)\n"
            + "    <br>Don't use a file extension (e.g., .nc) at the end of the dataset's name.\n"
            + "    <br>And don't specify a variable or a subset of the dataset at this stage.\n"
            + "  <li>Few situations: By hand in a browser or with a program like\n"
            + "    <a rel=\"help\" href=\"#curl\">curl</a>, download a .nc file\n"
            + "    with a subset of the dataset.  Then, use NetCDF-Java to open and access the data in\n"
            + "    that local file, e.g., one of these options:\n"
            + "    <pre>NetcdfFile nc = NetcdfFile.open(\"c:\\downloads\\theDownloadedFile.nc\");  //pre v5.4.1\n"
            + "NetcdfFile nc = NetcdfFiles.open(\"c:\\downloads\\theDownloadedFile.nc\");  //v5.4.1+\n"
            + "NetcdfDataset nc = NetcdfDataset.openDataset(\"c:\\downloads\\theDownloadedFile.nc\");  //pre v5.4.1\n"
            + "NetcdfDataset nc = NetcdfDatasets.openDataset(\"c:\\downloads\\theDownloadedFile.nc\");  //v5.4.1+</pre>\n"
            + "    (NetcdfFiles are a lower level approach than NetcdfDatasets.  It is your choice.)\n"
            + "    <br>This approach makes more sense if you want a local copy of the data subset, so\n"
            + "    that you can access it repeatedly (today, tomorrow, next week, ...) and quickly.\n"
            + "  </ul>\n"
            + "  <p>In both cases, you can then do what you want with the <kbd>nc</kbd> object, for example,\n"
            + "   request metadata or request a subset of a variable's data.\n"
            + "\n"
            +
            // ncHeader
            "  <p><strong>.ncHeader</strong>\n"
            + "    - <a class=\"selfLink\" id=\"ncHeader\" href=\"#ncHeader\" rel=\"bookmark\">Requests</a> for .ncHeader files will return the header information (UTF-8 text) that\n"
            + "  would be generated if you used\n"
            + "    <a rel=\"help\" href=\"https://linux.die.net/man/1/ncdump\">ncdump -h <i>fileName</i>"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    on the corresponding .nc file.\n"
            + "\n"
            +
            // odv
            "  <p><strong><a rel=\"bookmark\" href=\"https://odv.awi.de/\">Ocean Data View"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> .odvTxt</strong>\n"
            + "    - <a class=\"selfLink\" id=\"ODV\" href=\"#ODV\" rel=\"bookmark\">ODV</a> users can download data in a\n"
            + "  <a rel=\"help\" href=\"https://odv.awi.de/en/documentation/\">ODV Generic Spreadsheet Format .txt file"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    by requesting griddap's .odvTxt fileType.\n"
            + "  The dataset MUST include longitude, latitude, and time dimensions.\n"
            + "  Any longitude values (0 to 360, or -180 to 180) are fine.\n"
            + "  After saving the resulting file (with the extension .txt) in your computer:\n"
            + "  <ol>\n"
            + "  <li>Open ODV.\n"
            + "  <li>Use <kbd>File : Open</kbd>.\n"
            + "    <ul>\n"
            + "    <li>Change <kbd>Files of type</kbd> to <kbd>Data Files (*.txt *.csv *.jos *.o4x)</kbd>.\n"
            + "    <li>Browse to select the .txt file you created in ERDDAP and click on <kbd>Open</kbd>.\n"
            + "      The data locations should now be visible on a map in ODV.\n"
            + "    </ul>\n"
            + "  <li>If you downloaded a longitude, latitude slice of a dataset, use:\n"
            + "    <ul>\n"
            + "    <li>Press F12 or <kbd>View : Layout Templates : 1 SURFACE Window</kbd> to view the\n"
            + "      default isosurface variable.\n"
            + "    <li>If you want to view a different isosurface variable,\n"
            + "      <ul>\n"
            + "      <li>To define a new isosurface variable, use <kbd>View : Isosurface Variables</kbd>.\n"
            + "      <li>Right click on the map and choose <kbd>Properties : Data : Z-axis</kbd> to pick\n"
            + "        the new isosurface variable.\n"
            + "      </ul>\n"
            +
            // "  <li>To zoom in on the data, use <kbd>View : Window Properties : Maps : Domain :
            // Full Domain : OK</kbd>.\n" +
            "    </ul>\n"
            + "  <li>See ODV's <kbd>Help</kbd> menu for more help using ODV.\n"
            + "  </ol>\n"
            + "\n"
            +
            // opendapLibraries
            "  <p><strong><a class=\"selfLink\" id=\"opendapLibraries\" href=\"#opendapLibraries\" rel=\"bookmark\">OPeNDAP Libraries</a></strong> - Since ERDDAP is an\n"
            + "    <a rel=\"bookmark\" href=\"https://www.opendap.org/\">OPeNDAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>-compatible data server,\n"
            + "    you can use\n"
            + "  any OPeNDAP client library, such as\n"
            + "    <a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\">NetCDF-Java, NetCDF-C, NetCDF-Fortran, NetCDF-Perl"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  <a rel=\"bookmark\" href=\"https://www.opendap.org/deprecated-software/java-dap\">Java-DAP2"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  <a rel=\"bookmark\" href=\"https://ferret.pmel.noaa.gov/Ferret/\">Ferret"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>, or the\n"
            + "     <a rel=\"bookmark\" href=\"https://www.pydap.org/en/latest/client.html\">Pydap Client"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  to get data from an ERDDAP griddap dataset.\n"
            + "  When creating the initial connection to an ERDDAP griddap dataset from any OPeNDAP library:\n"
            + "  <ul>\n"
            + "  <li>Don't include a file extension (e.g., .nc) at the end of the dataset's name.\n"
            + "  <li>Don't specify a variable or a subset of the dataset.\n"
            + "  </ul>\n"
            + "  <br>Once you have made the connection to the dataset, you can request metadata or a subset\n"
            + "  of a variable's data.\n"
            + "\n"
            + "  <p>For example, with the NetCDF-Java library, you can use:\n"
            + "  <pre>NetcdfFile nc = NetcdfDataset.openFile(\""
            + datasetBase
            + "\", null);  //pre v5.4.1</pre>\n"
            + "  or\n"
            + "  <pre>NetcdfFile nc = NetcdfDatasets.openFile(\""
            + datasetBase
            + "\", null); //v5.4.1+</pre>\n"
            + "  (don't use <kbd>NetcdfFile.open</kbd>; it is for local files only) or\n"
            + "  <pre>NetcdfDataset nc = NetcdfDataset.openDataset(\""
            + datasetBase
            + "\");  //pre v5.4.1</pre>\n"
            + "  or\n"
            + "  <pre>NetcdfDataset nc = NetcdfDatasets.openDataset(\""
            + datasetBase
            + "\");  //v5.4.1+</pre>\n"
            + "  (NetcdfFiles are a lower level approach than NetcdfDatasets.  It is your choice.)\n"
            + "  <br>Once you have the <kbd>nc</kbd> object, you can request metadata or a subset of a\n"
            + "  variable's data.\n"
            + "\n"
            +
            // Pydap Client
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.pydap.org/en/latest/client.html\">Pydap Client"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"PydapClient\" href=\"#PydapClient\" rel=\"bookmark\">users</a>\n"
            + "    can access griddap datasets via ERDDAP's standard OPeNDAP services.\n"
            + "  See the\n"
            + "    <a rel=\"help\" href=\"https://www.pydap.org/en/latest/client.html#accessing-gridded-data\">Pydap Client instructions for accessing gridded data"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  Note that the name of a dataset in ERDDAP will always be a single word,\n"
            + "  (e.g., "
            + EDStatic.messages.EDDGridIdExample
            + " in the OPeNDAP dataset URL\n"
            + "  <br>"
            + datasetBase
            + " )\n"
            + "  and won't ever have a file extension (unlike, for example, .nc for the\n"
            + "  sample dataset in the Pydap instructions).\n"
            + "\n"
            +
            // Python
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.python.org\">Python"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"Python\" href=\"#Python\" rel=\"bookmark\">is</a> a widely-used computer language that is very popular among scientists.\n"
            + "    In addition to the <a rel=\"help\" href=\"#PydapClient\">Pydap Client</a>, you can use Python to download various files from ERDDAP\n"
            + "    as you would download other files from the web:\n"
            + "<pre>import urllib\n"
            + "urllib.urlretrieve(\"<i>https://baseHttpsUrl</i>/erddap/griddap/<i>datasetID.fileType?query</i>\", \"<i>outputFileName</i>\")</pre>\n"
            + "  (You may need to <a rel=\"help\" href=\"#PercentEncoded\">percent encode</a> the query part of the URL.)\n"
            + "  <br>Or download the content to an object instead of a file:\n"
            + "<pre>import urllib2\n"
            + "response = urllib2.open(\"<i>https://baseHttpsUrl</i>/erddap/griddap/<i>datasetID.fileType?query</i>\")\n"
            + "theContent = response.read()</pre>\n"
            + "  There are other ways to do this in Python. Search the web for more information.\n"
            + "\n"
            +
            // erddapy
            "  <p><a rel=\"bookmark\" href=\"https://github.com/ioos/erddapy#--erddapy\">erddapy"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    <a class=\"selfLink\" id=\"erddapy\" href=\"#erddapy\" rel=\"bookmark\">(ERDDAP + Python, by Filipe Pires Alvarenga Fernandes)</a> and\n"
            + "  <br><a rel=\"bookmark\" href=\"https://github.com/hmedrano/erddap-python\">erddap-python"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> (by Favio Medrano)\n"
            + "  <br>are Python libraries that \"take advantage of ERDDAP’s RESTful web services and create the\n"
            + "    ERDDAP URL for any request like searching for datasets, acquiring metadata, downloading data, etc.\"\n"
            + "    They have somewhat different programming styles and slightly different feature sets,\n"
            + "    so it might be good to experiment with both to see which you prefer.\n"
            +
            //
            // Python/Jupyter Notebook
            "  <p>\"<a rel=\"bookmark\" href=\"https://jupyter.org/\">Jupyter Notebook"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "      <a class=\"selfLink\" id=\"JupyterNotebook\" href=\"#JupyterNotebook\" rel=\"bookmark\">is</a> an open-source web application that\n"
            + "      allows you to create and\n"
            + "    share documents that contain live code, equations, visualizations and explanatory text\"\n"
            + "    using any of over 40 programming languages, including Python and R.\n"
            + "    Here are two sample Jupyter Notebooks that access ERDDAP using Python:\n"
            + "    <a rel=\"bookmark\"\n"
            + "      href=\"https://github.com/rsignell-usgs/notebook/blob/master/ERDDAP/ERDDAP_advanced_search_test.ipynb/\"\n"
            + "      >ERDDAP Advanced Search Test"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> and\n"
            + "    <a rel=\"bookmark\" \n"
            + "      href=\"https://github.com/rsignell-usgs/notebook/blob/master/ERDDAP/ERDDAP_timing.ipynb\"\n"
            + "      >ERDDAP Timing"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "    Thanks to Rich Signell.\n"
            +
            // R
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.r-project.org/\">R Statistical Package"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong> -\n"
            + "    <a class=\"selfLink\" id=\"R\" href=\"#R\" rel=\"bookmark\">R</a> is an open source statistical package for many operating systems.\n"
            + "  In R, you can download a NetCDF version 3 .nc file from ERDDAP. For example:\n"
            + "<pre>download.file(url=\""
            + fullTimeNcExampleHE
            + "\", destfile=\"/home/bsimons/test.nc\")</pre>\n"
            + "  (You may need to <a rel=\"help\" href=\"#PercentEncoded\">percent encode</a> the query part of the URL.)\n"
            + "  Then import data from that .nc file into R with the RNetCDF, ncdf, or ncdf4 packages available\n"
            + "  from <a rel=\"bookmark\" href=\"https://cran.r-project.org/\">CRAN"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "    Or, if you want the data in tabular form, download and import the data in a .csv file.\n"
            + "  For example,\n"
            + "<pre>download.file(url=\""
            + fullTimeCsvExampleHE
            + "\", destfile=\"/home/bsimons/test.csv\")\n"
            + "test&lt;-read.csv(file=\"/home/bsimons/test.csv\")</pre>\n"
            + "  There are third-party R packages designed to make it easier to work with ERDDAP from within R:\n"
            + "    <a rel=\"bookmark\" href=\"https://cran.r-project.org/web/packages/rerddap/index.html\">rerddap"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "    <a rel=\"bookmark\" href=\"https://cran.r-project.org/web/packages/rerddapXtracto/index.html\">rerddapXtracto"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>, and\n"
            + "    <a rel=\"bookmark\" href=\"https://cran.r-project.org/web/packages/plotdap/index.html\">plotdap"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  Thanks to \n"
            + "  <a rel=\"bookmark\" href=\"https://ropensci.org/\">rOpenSci<img \n"
            + "    src=\"../images/external.png\" alt=\" (external link)\" \n"
            + "    title=\"This link to an external website does not constitute an endorsement.\"></a>\n"
            + "  and Roy Mendelssohn.\n"
            + "\n"
            + "  <p>There are also R <a rel=\"bookmark\" href=\"https://coastwatch.pfeg.noaa.gov/xtracto/\">Xtractomatic</a> scripts for ERDDAP,\n"
            + "    which are particularly useful for getting\n"
            + "  environmental data related to points along an animal's track (e.g.,\n"
            + "    <a rel=\"bookmark\" href=\"https://gtopp.org/\">GTOPP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> data).\n"
            + "     <br>&nbsp;\n"
            + "\n"
            +
            // timeGaps
            "   <p><a class=\"selfLink\" id=\"timeGaps\" href=\"#timeGaps\" rel=\"bookmark\"><strong>.timeGaps</strong></a> is a response which is unique to ERDDAP. It lets you\n"
            + "     view a list of gaps in a dataset's time values which are larger than the median gap.\n"
            + "     This is useful for ERDDAP administrators and end users when they want\n"
            + "     to know if there are unexpected gaps in the time values for a dataset\n"
            + "     that is expected to have regularly spaced time values.\n"
            + "     This option ignores all request constraints.\n"
            + "     <br>&nbsp;\n"
            + "\n"
            +
            // .wav
            "  <p><strong><a rel=\"bookmark\" href=\"https://en.wikipedia.org/wiki/WAV\">.wav"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong> -\n"
            + "    <a class=\"selfLink\" id=\"wav\" href=\"#wav\" rel=\"bookmark\">ERDDAP can return data in .wav files,</a> which are uncompressed audio files.\n"
            + "  <ul>\n"
            + "  <li>You can save any numeric data in .wav files, but this file format is clearly intended to be used\n"
            + "    with <a rel=\"bookmark\" href=\"https://en.wikipedia.org/wiki/Pulse-code_modulation\">PCM"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> digitized sound waves.\n"
            + "    We do not recommend saving other types of data in .wav files.\n"
            + "  <li>All of the data variables you select to save in a .wav file must have the same data type, e.g., int.\n"
            + "    Currently, because of a bug in Java 8, ERDDAP can't save float or double data. That should\n"
            + "    change when ERDDAP is run with Java 9.\n"
            + "  <li>For gridded datasets, ERDDAP doesn't write the axis variable data to the .wav files.\n"
            + "  <li>The data variable values are just written as a long sequence of numbers (the sound wave).\n"
            + "    Unfortunately, ERDDAP must create the entire .wav file before it can begin to send the file to\n"
            + "    you, so there will be a delay from when you submit the request and when you start to get the file.\n"
            + "    There is no limit in ERDDAP to the number of numbers that may be written to a .wav file,\n"
            + "    but requests for very large files may lead to timeout error messages because of the long\n"
            + "    time needed to create the file.\n"
            + "  <li>Data is written to .wav files using either the PCM_SIGNED encoding (for integer data types)\n"
            + "    or the PCM_FLOAT encoding (for floating point data types).\n"
            + "    Since long integer values aren't supported by PCM_SIGNED, ERDDAP writes longs as ints,\n"
            + "    by writing the high 4 bytes of the long to the file.\n"
            + "    Other than long values, data values are unchanged when they are written to .wav files.\n"
            + "  <li>If the dataset's global attributes include <kbd>audioFrameRate</kbd> and/or <kbd>audioSampleRate</kbd>\n"
            + "    attributes, those values (they should be the same) will be used as the frameRate and sampleRate.\n"
            + "    Otherwise, ERDDAP assigns a rate of 10000 frames (or samples) per second.\n"
            + "  <li>The .wav files will have several attributes describing how the sound data is stored in the file.\n"
            + "    Currently, no other metadata (e.g., the global attributes) is stored in the .wav files.\n"
            + "    <br>&nbsp;\n"
            + "    <br>&nbsp;\n"
            + "  </ul>\n"
            + "\n");

    // imageFile Types, graphs and maps
    writer.write(
        "  <p><a class=\"selfLink\" id=\"imageFileTypes\" href=\"#imageFileTypes\" rel=\"bookmark\"><strong>Making an Image File with a Graph or Map of Gridded Data</strong></a>\n"
            + "  <br>If a griddap request URL specifies a subset of data which is suitable for making\n"
            + "  a graph or a map, and the fileType is an image fileType, griddap will return an image\n"
            + "  with a graph or map. \n"
            + "  griddap request URLs can include optional <a rel=\"help\" href=\"#GraphicsCommands\">graphics commands</a> which let you\n"
            + "  customize the graph or map.\n"
            + "  As with other griddap request URLs, you can create these URLs by hand or have a\n"
            + "  computer program do it.  Or, you can use the Make A Graph web pages, which simplify\n"
            + "  creating these URLs (see the \"graph\" links in the table of\n"
            + "<a rel=\"bookmark\" href=\""
            + dapBase
            + "index.html\">griddap datasets</a>). \n"
            + "\n"
            + "   <p>The fileType options for downloading images of graphs and maps of grid data are:\n"
            + "  <table class=\"erd\" style=\"width:100%; \">\n"
            + "    <tr><th>Image<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
    List<EDDFileTypeInfo> imageFileTypes = EDD.getFileTypeOptions(true, true);
    for (int i = 0; i < imageFileTypes.size(); i++) {
      EDDFileTypeInfo curType = imageFileTypes.get(i);
      String ft = curType.getFileTypeName();
      String ft1 = ft.substring(1);
      writer.write(
          "    <tr>\n"
              + "      <td><a class=\"selfLink\" id=\"fileType_"
              + ft1
              + "\" href=\"#fileType_"
              + ft1
              + "\" rel=\"bookmark\">"
              + ft
              + "</a></td>\n"
              + "      <td>"
              + curType.getGridDescription(language)
              + "</td>\n"
              + "      <td class=\"N\">"
              + (curType.getInfoUrl() == null || curType.getInfoUrl().isEmpty()
                  ? "&nbsp;"
                  : "<a rel=\"help\" href=\""
                      + XML.encodeAsHTMLAttribute(curType.getInfoUrl())
                      + "\">info"
                      + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
                      + "</a>")
              + "</td>\n"
              + // must be mapExample below because kml doesn't work with graphExample
              "      <td class=\"N\"><a rel=\"bookmark\" href=\""
              + datasetBase
              + curType.getFileTypeName()
              + "?"
              + EDStatic.messages.EDDGridMapExampleHA
              + "\">example</a></td>\n"
              + "    </tr>\n");
    }
    writer.write(
        "  </table>\n"
            + "\n"
            +
            // size
            "  <p>Image Size - \".small\" and \".large\" were ERDDAP's original system for making\n"
            + "  different-sized images. Now, for .png and .transparentPng images (not other\n"
            + "  image file types), you can also use the\n"
            + "    <a rel=\"help\" href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a>\n"
            + "    parameter to request\n"
            + "  an image of any size.\n"
            + "\n"
            +
            // transparentPng
            "  <p><a class=\"selfLink\" id=\"transparentPng\" href=\"#transparentPng\" rel=\"bookmark\">.transparentPng</a> - The .transparentPng file type will make a graph or map without\n"
            + "  the graph axes, landmask, or legend, and with a transparent (not opaque white)\n"
            + "  background.  This option can be used for any type of graph or map.\n"
            + "  When <kbd>&amp;.draw=</kbd> is set to anything other than <kbd>surface</kbd>, the default image size\n"
            + "  is 360x360 pixels. Use the <a rel=\"help\" href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a> parameter\n"
            + "     to specify a different size.\n"
            + "  When <kbd>&amp;.draw=</kbd> is set to <kbd>surface</kbd>, ERDDAP makes an image where each\n"
            + "  data point becomes one pixel, which may result in a huge image. If the request\n"
            + "  takes too long or fails (perhaps for an odd apparent reason, like a Proxy Error)\n"
            + "  either use a stride (see below) value greater than 1 (e.g., 5) for the x and y axis\n"
            + "  variables, or use the\n"
            + "  <a rel=\"help\" href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a> parameter\n"
            + "  to restrict the image size,\n"
            + "  so that the image isn't huge.\n"
            + "\n");

    // file type incompatibilities
    writer.write(
        "  <p><strong>Incompatibilities</strong>\n"
            + "  <br>Some results file types have restrictions. For example, Google Earth .kml is only\n"
            + "  appropriate for results with longitude and latitude values. If a given request is\n"
            + "  incompatible with the requested file type, griddap throws an error.\n"
            + "\n"
            +

            // curl
            "<p><a class=\"selfLink\" id=\"curl\" href=\"#curl\" rel=\"bookmark\"><strong>Command Line Downloads with curl</strong></a>\n"
            + "<br>If you want to download a series of files from ERDDAP, you don't have to request each file's\n"
            + "ERDDAP URL in your browser, sitting and waiting for each file to download. \n"
            + "If you are comfortable writing computer programs (e.g., with C, Java, Python, Matlab, r)\n"
            + "or scripts (e.g., Python, bash, tcsh, PowerShell, or Windows batch files),\n"
            + "you can write a program or script with a loop (or a series of commands)\n"
            + "that imports all of the desired data files.\n"
            + "Or, if you are comfortable running command line programs\n"
            + "(from a Linux or Windows command line, or a Mac OS Terminal), you can use curl (or a similar program like\n"
            + "  <a rel=\"bookmark\" href=\"https://www.gnu.org/software/wget/\">wget"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>)\n"
            + "to save results files from ERDDAP into files on your hard drive,\n"
            + "without using a browser or writing a computer program or script.\n"
            + "<p>ERDDAP + curl is amazingly powerful and allows you to use ERDDAP in many new ways.\n"
            + "<br>On Linux and Mac OS X, curl is probably already installed.\n"
            + "<br>On Mac OS X, to get to a command line, use \"Finder : Go : Utilities : Terminal\".\n"
            + "<br>On Windows, you need to\n"
            + "  <a rel=\"bookmark\" href=\"https://curl.haxx.se/download.html\">download curl"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "  (the \"Windows 64 - binary, the curl project\" variant worked for me on Windows 10)\n"
            + "  and install it.\n"
            + "<br>On Windows, to get to a command line, click on \"Start\" and type\n"
            + "\"cmd\" into the search text field.\n"
            + "<br><strong>Please be kind to other ERDDAP users: run just one script or curl command at a time.</strong>\n"
            + "<br>Instructions for using curl are on the \n"
            + "<a rel=\"help\" href=\"https://curl.haxx.se/download.html\">curl man page"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> and in this\n"
            + "<a rel=\"help\" href=\"https://curl.haxx.se/docs/httpscripting.html\">curl tutorial"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "<br>But here is a quick tutorial related to using curl with ERDDAP:\n"
            + "<ul>\n"
            + "<li>To download and save one file, use \n"
            + "  <br><kbd>curl --compressed -g \"<i>erddapUrl</i>\" -o <i>fileDir/fileName.ext</i></kbd>\n"
            + "  <br>where <kbd>--compressed</kbd> tells curl to request a compressed response and automatically decompress it,\n"
            + "  <br>&nbsp;&nbsp;<kbd>-g</kbd> disables curl's globbing feature,\n"
            + "  <br>&nbsp;&nbsp;<kbd><i>erddapUrl</i></kbd> is any ERDDAP URL that requests a data or image file, and\n"
            + "  <br>&nbsp;&nbsp;<kbd>-o <i>fileDir/fileName.ext</i></kbd> specifies the name for the file that will be created.\n"
            + "  <br>For example,\n"
            + "<pre>curl --compressed -g \""
            + fullGraphExampleHA
            + "\" -o "
            + EDStatic.messages.EDDGridIdExample
            + "_example.png</pre>\n"
            + "  (That example includes <kbd>--compressed</kbd> because it is a generally useful option,\n"
            + "  but there is little benefit to <kbd>--compressed</kbd> when requesting .png files\n"
            + "  because they are already compressed.)\n"
            +
            // &amp;.draw=surface&amp;.vars=longitude|latitude|sst&amp;.colorBar=|||||\" -o
            // BAssta5day20100901.png</pre>\n" +
            "  <p><a class=\"selfLink\" id=\"PercentEncoded\" href=\"#PercentEncoded\" rel=\"bookmark\">In curl, as in many other programs, the query part of the erddapUrl must be</a>\n"
            + "  <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.  To do this, you need to convert\n"
            + "  special characters (other than the initial '&amp;' and the main '=' of a constraint)\n"
            + "  into the form %HH, where HH is the 2 digit hexadecimal value of the character.\n"
            + "  Usually, you just need to convert a few of the punctuation characters: % into %25,\n"
            + "  &amp; into %26, \" into %22, &lt; into %3C, = into %3D, &gt; into %3E, + into %2B, | into %7C,\n"
            + "  space into %20, [ into %5B, ] into %5D,\n"
            + "  and convert all characters above #127 into their UTF-8 bytes and then percent encode\n"
            + "  each byte of the UTF-8 form into the %HH format (ask a programmer for help).\n"
            + "  Note that percent encoding is generally required when you access ERDDAP via\n"
            + "  software other than a browser. Browsers usually handle percent encoding for you.\n"
            + "  In some situations, you need to percent encode all characters other than\n"
            + "  A-Za-z0-9_-!.~'()* .\n"
            + "  Programming languages have tools to do this (for example, see Java's\n"
            + "  <a rel=\"help\" href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URLEncoder.html\">java.net.URLEncoder"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "     and JavaScript's\n"
            + "<a rel=\"help\" href=\"https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent\">encodeURIComponent()"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>) and there are\n"
            + "   <a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  <br>&nbsp;\n"
            + "<li>To download and save many files in one step, use curl with the globbing feature enabled:\n"
            + "  <br><kbd>curl --compressed \"<i>erddapUrl</i>\" -o <i>fileDir/fileName#1.ext</i></kbd>\n"
            + "  <br>Since the globbing feature treats the characters [, ], {, and } as special, you must\n"
            + "  <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encode"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> \n"
            + "them in the erddapURL as &#37;5B, &#37;5D, &#37;7B, &#37;7D, respectively.\n"
            + "  Then, in the erddapUrl, replace a zero-padded number (for example <kbd>01</kbd>) with a range\n"
            + "  of values (for example, <kbd>[01-15]</kbd> ),\n"
            + "  or replace a substring (for example <kbd>5day</kbd>) with a list of values (for example,\n"
            + "  <kbd>{5day,8day,mday}</kbd> ).\n"
            + "  The <kbd>#1</kbd> within the output fileName causes the current value of the range or list\n"
            + "  to be put into the output fileName.\n"
            + "  For example, \n"
            + "<pre>curl --compressed \"https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdBAssta5day.png?sst&#37;5B%282010-09-[01-05]T12:00:00Z%29&#37;5D&#37;5B&#37;5D&#37;5B&#37;5D&#37;5B&#37;5D&amp;.draw=surface&amp;.vars=longitude|latitude|sst&amp;.colorBar=|||||\" -o BAssta5day201009#1.png</pre>\n"
            + "<li>To access a password-protected, private ERDDAP dataset with curl via https, use this\n"
            + "  two-step process:\n"
            + "  <br>&nbsp;\n"
            + "  <ol>\n"
            + "  <li>Log in (authenticate) and save the certificate cookie in a cookie-jar file:\n"
            + "<pre>curl -v --data 'user=<i>myUserName</i>&amp;password=<i>myPassword</i>' -c cookies.txt -b cookies.txt -k https://<i>baseurl</i>:8443/erddap/login.html</pre>\n"
            + "  <li>Repeatedly make data requests using the saved cookie: \n"
            + "<pre>curl --compressed -v -c cookies.txt -b cookies.txt -k https://<i>baseurl</i>:8443/erddap/griddap/<i>datasetID.fileType?query</i> -o <i>outputFileName</i></pre>\n"
            + "  </ol>\n"
            + "  Some ERDDAP installations won't need the port number (:8443) in the URL.\n"
            + "  <br>(Thanks to Liquid Robotics for the starting point and Emilio Mayorga of NANOOS and\n"
            + "  <br>Paul Janecek of Spyglass Technologies for testing.)\n"
            + "  <br>&nbsp;\n"
            + "</ul>\n");

    // query
    writer.write(
        "<li><a class=\"selfLink\" id=\"query\" href=\"#query\" rel=\"bookmark\"><strong>query</strong></a> is the part of the request after the \"?\". \n"
            + "  <br>It specifies the subset of data that you want to receive.\n"
            + "  In griddap, it is an optional\n"
            + "    <a rel=\"bookmark\" href=\"https://www.opendap.org\">OPeNDAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n "
            + "    <a rel=\"help\" href=\"https://www.opendap.org/pdf/ESE-RFC-004v1.2.pdf\">DAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    <a rel=\"help\" href=\"https://opendap.github.io/documentation/UserGuideComprehensive.html#Constraint_Expressions\">projection constraint"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> query\n"
            + "  which can request:\n"
            + "   <ul>\n"
            + "   <li>One or more dimension (axis) variables, for example\n "
            + "       <br><a href=\""
            + fullDimensionExampleHA
            + "\"><kbd>"
            + fullDimensionExampleHE
            + "</kbd></a> .\n"
            + "   <li>One or more data variables, for example\n"
            + "       <br><a href=\""
            + fullIndexExampleHA
            + "\"><kbd>"
            + fullIndexExampleHE
            + "</kbd></a> .\n"
            + "       <br>To request more than one data variable, separate the desired data variable names by commas.\n"
            + "       If you do request more than one data variable, the requested subset for each variable\n"
            + "       must be identical (see below).\n"
            + "       (In griddap, all data variables within a grid dataset share the same dimensions.)\n"
            + "   <li>The entire dataset. Omitting the entire query is the same as requesting all of the data\n"
            + "     for all of the variables. Because of the large size of most datasets, this is usually\n"
            + "     only appropriate for fileTypes that return information about the dataset (for example,\n"
            + "     .das, .dds, and .html), but don't return all of the actual data values. For example,\n"
            + "     <br><a href=\""
            + XML.encodeAsHTMLAttribute(ddsExample)
            + "\"><kbd>"
            + XML.encodeAsHTMLAttribute(ddsExample)
            + "</kbd></a>\n"
            + "     <br>griddap is designed to handle requests of any size but trying to download all of the\n"
            + "     data with one request will usually fail (for example, downloads that last days usually\n"
            + "     fail at some point).  If you need all of the data, consider breaking your big request\n"
            + "     into several smaller requests. If you just need a sample of the data, use the largest\n"
            + "     acceptable stride values (see below) to minimize the download time.\n"
            + "   </ul>\n"
            + "   \n"
            + "   <p><a class=\"selfLink\" id=\"StartStrideStop\" href=\"#StartStrideStop\" rel=\"bookmark\">Using</a> <kbd>[start:stride:stop]</kbd>\n"
            + "     When requesting dimension (axis) variables or data variables, the query may\n"
            + "     specify a subset of a given dimension by identifying the <kbd>[start{{:stride}:stop}]</kbd>\n"
            + "     indices for that dimension.\n"
            + "   <ul>\n"
            + "   <li><kbd>start</kbd> is the index of the first desired value. Indices are 0-based.\n"
            + "     (0 is the first index. 1 is the second index. ...) \n"
            + "   <li><kbd>stride</kbd> indicates how many intervening values to get: 1=get every value,\n"
            + "     2=get every other value, 3=get every third value, ...\n"
            + "     Stride values are in index units (not the units of the dimension).\n"
            + "   <li><kbd>stop</kbd> is the index of the last desired value. \n"
            + "   <li>Specifying only two values for a dimension (i.e., <kbd>[start:stop]</kbd>) is interpreted\n"
            + "     as <kbd>[start:1:stop]</kbd>.\n"
            + "   <li>Specifying only one value for a dimension (i.e., <kbd>[start]</kbd>) is interpreted\n"
            + "     as <kbd>[start:1:start]</kbd>.\n"
            + "   <li>Specifying no values for a dimension (i.e., []) is interpreted as <kbd>[0:1:max]</kbd>.\n"
            + "   <li>Omitting all of the <kbd>[start:stride:stop]</kbd> values (that is, requesting the\n"
            + "     variable without the subset constraint) is equivalent to requesting the entire variable.\n"
            + "     For dimension variables (for example, longitude, latitude, and time) and for fileTypes\n"
            + "     that don't download actual data (notably, .das, .dds, .html, and all of the graph and\n"
            + "     map fileTypes) this is fine. For example,\n"
            + "     <br><a href=\""
            + XML.encodeAsHTMLAttribute(dds1VarExample)
            + "\"><kbd>"
            + XML.encodeAsHTMLAttribute(dds1VarExample)
            + "</kbd></a>\n"
            + "     <br>For data variables, the resulting data may be very large.\n"
            + "     griddap is designed to handle requests of any size. But if you try to download all of\n"
            + "     the data with one request, the request will often fail for other reasons (for example,\n"
            + "     downloads that last for days usually fail at some point). If you need all of the data,\n"
            + "     consider breaking your big request into several smaller requests.\n"
            + "     If you just need a sample of the data, use the largest acceptable stride values\n"
            + "     to minimize the download time.\n"
            + "   <li><a class=\"selfLink\" id=\"last\" href=\"#last\" rel=\"bookmark\"><kbd>last</kbd></a> - ERDDAP extends the OPeNDAP standard by interpreting \n"
            + "     a <kbd>start</kbd> or <kbd>stop</kbd> value of <kbd>last</kbd> as the last available index value.\n"
            + "     You can also use the notation <kbd>last-<i>n</i></kbd> (e.g., <kbd>last-10</kbd>)\n"
            + "     to specify the last index minus some number of indices.\n"
            + "     You can use '+' in place of '-'. The number of indices can be negative.\n"
            + "   <li><a class=\"selfLink\" id=\"parentheses\" href=\"#parentheses\" rel=\"bookmark\">griddap</a> extends the standard OPeNDAP subset syntax by allowing the start\n"
            + "     and/or stop values to be actual dimension values (for example, longitude values\n"
            + "     in degrees_east) within parentheses, instead of array indices.  \n"
            + "     This example with "
            + EDStatic.messages.EDDGridDimNamesExample
            + " dimension values \n"
            + "     <br><a href=\""
            + fullValueExampleHA
            + "\"><kbd>"
            + fullValueExampleHE
            + "</kbd></a>\n"
            + "     <br>is (at least at the time of writing this) equivalent to this example with dimension indices\n"
            + "     <br><a href=\""
            + fullIndexExampleHA
            + "\"><kbd>"
            + fullIndexExampleHE
            + "</kbd></a>\n"
            + "     <br>The value in parentheses must be within the range of values for the dimension. \n"
            + "     If the value in parentheses doesn't exactly equal one of the dimension values, the\n"
            + "     closest dimension value will be used.\n"
            + "   <li><a class=\"selfLink\" id=\"strideParentheses\" href=\"#strideParentheses\" rel=\"bookmark\">griddap</a> does not allow parentheses around stride values.\n"
            + "     The reasoning is: With the start and stop values, it is easy to convert the value in\n"
            + "     parentheses into the appropriate index value by finding the nearest dimension value.\n"
            + "     This works if the dimension values are evenly spaced or not.\n"
            + "     If the dimension values were always evenly spaced, it would be easy to use a similar\n"
            + "     technique to convert a stride value in parentheses into a stride index value.\n"
            + "     But dimension values often aren't evenly spaced. So for now, ERDDAP doesn't support the\n"
            + "     parentheses notation for stride values.\n"
            + "   <li><a class=\"selfLink\" id=\"highLow\" href=\"#highLow\" rel=\"bookmark\">[high:low] or [(high):(low)]</a> -- \n"
            + "     Starting with ERDDAP v2.17, if a user specifies the high and low values in the wrong order, e.g., [high:low] or [(high):(low)],\n"
            + "     ERDDAP will swap the specified high and low values to make the request valid.\n"
            + "     This is particularly useful for the few datasets where the latitude values in the dataset\n"
            + "     are stored high to low: previously, these datasets had to be known and handled separately by users;\n"
            + "     now, if users specify the values in the \"incorrect\" order, ERDDAP will repair the request.\n"
            + "   <li><a class=\"selfLink\" id=\"time\" href=\"#time\" rel=\"bookmark\">griddap</a> always stores date/time values as double precision floating point numbers\n"
            + "     (seconds since 1970-01-01T00:00:00Z, sometimes with some number of milliseconds).\n"
            + "     Here is an example of a query which includes date/time numbers:\n"
            + "     <br><a href=\""
            + fullValueExampleHA
            + "\"><kbd>"
            + fullValueExampleHE
            + "</kbd></a>\n"
            + "     <br>The more human-oriented fileTypes (notably, .csv, .tsv, .htmlTable, .odvTxt, and .xhtml)\n"
            + "     display date/time values as "
            + "       <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" date/time strings"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "     (e.g., 2002-08-03T12:30:00Z, but some variables include milliseconds, e.g.,\n"
            + "     2002-08-03T12:30:00.123Z).\n"
            + (EDStatic.config.convertersActive
                ? "     <br>ERDDAP has a utility to\n"
                    + "       <a rel=\"bookmark\" href=\""
                    + tErddapUrl
                    + "/convert/time.html\">Convert\n"
                    + "       a Numeric Time to/from a String Time</a>.\n"
                    + "     See also:\n"
                    + "       <a rel=\"help\" href=\""
                    + tErddapUrl
                    + "/convert/time.html#erddap\">How\n"
                    + "       ERDDAP Deals with Time</a>.\n"
                : "")
            + "   <li>For the time dimension, griddap extends the OPeNDAP standard by allowing you to specify an\n"
            + "     <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" date/time string"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "       in parentheses, which griddap then converts to the\n"
            + "     internal number (in seconds since 1970-01-01T00:00:00Z) and then to the appropriate\n"
            + "     array index.  The ISO date/time value should be in the form: <i>YYYY-MM-DD</i>T<i>hh:mm:ss.SSSZ</i>,\n"
            + "     where Z is 'Z' or a &plusmn;hh or &plusmn;hh:mm offset from the Zulu/GMT time zone. If you omit Z and the\n"
            + "     offset, the Zulu/GMT time zone is used. Separately, if you omit .SSS, :ss.SSS, :mm:ss.SSS, or\n"
            + "     Thh:mm:ss.SSS from the ISO date/time that you specify, the missing fields are assumed to be 0.\n"
            + "     In some places, ERDDAP accepts a comma (ss,sss) as the seconds decimal point, but ERDDAP\n"
            + "     always uses a period when formatting times as ISO 8601 strings.\n"
            + "     The example below is equivalent (at least at the time of writing this) to the examples above:\n"
            + "     <br><a href=\""
            + fullTimeExampleHA
            + "\"><kbd>"
            + fullTimeExampleHE
            + "</kbd></a>\n"
            + "     <br><a class=\"selfLink\" id=\"lenient\" href=\"#lenient\" rel=\"bookmark\">ERDDAP</a> is \"lenient\" when it parses date/time strings. That means that date/times\n"
            + "     with the correct format, but with month, date, hour, minute, and/or second values\n"
            + "     that are too large or too small will be rolled to the appropriate date/times.\n"
            + "     For example, ERDDAP interprets 2001-12-32 as 2002-01-01, and interprets\n"
            + "     2002-01-00 as 2001-12-31.\n"
            + "     (It's not a bug, it's a feature! We understand that you may object to this\n"
            + "     if you are not familiar with lenient parsing. We understand there are\n"
            + "     circumstances where some people would prefer strict parsing, but there are also\n"
            + "     circumstances where some people would prefer lenient parsing. ERDDAP can't\n"
            + "     have it both ways. This was a conscious choice. Lenient parsing is the default\n"
            + "     behavior in Java, the language that ERDDAP is written in and arguably the\n"
            + "     most-used computer language. Also, this behavior is consistent with ERDDAP's\n"
            + "     conversion of requested grid axis values to the nearest valid grid axis value.\n"
            + "     And this is consistent with some other places in ERDDAP that try to repair\n"
            + "     invalid input when the intention is clear, instead of just returning an error\n"
            + "     message.)\n"
            + (EDStatic.config.convertersActive
                ? "     ERDDAP has a utility to\n"
                    + "       <a rel=\"bookmark\" href=\""
                    + tErddapUrl
                    + "/convert/time.html\">Convert\n"
                    + "       a Numeric Time to/from a String Time</a>.\n"
                    + "     See also:\n"
                    + "       <a rel=\"help\" href=\""
                    + tErddapUrl
                    + "/convert/time.html#erddap\">How\n"
                    + "       ERDDAP Deals with Time</a>.\n"
                : "")
            + "   <li><a class=\"selfLink\" id=\"lastInParentheses\" href=\"#lastInParentheses\" rel=\"bookmark\"><kbd>(last)</kbd></a> - ERDDAP interprets \n"
            + "       a <kbd>start</kbd> or <kbd>stop</kbd> value of <kbd>(last)</kbd> as the last\n"
            + "     available index.\n"
            + "     You can also use the notation <kbd>(last-<i>d</i>)</kbd> (e.g., <kbd>last-10.5</kbd>) to specify\n"
            + "     the last index's value minus some number, which is then converted to the nearest\n"
            + "     index. You can use '+' in place of '-'. The number can be negative.\n"
            + "     For a time axis, the number is interpreted as some number of seconds.\n"
            +
            // polygon requests
            "   <li><a class=\"selfLink\" id=\"polygonRequests\" href=\"#polygonRequests\" rel=\"bookmark\"\n"
            + "     >Can users extract data from a polygon-defined geographic area?</a>\n"
            + "     <br>No. ERDDAP does not support that.\n"
            + "     You need to calculate the lat lon bounding box for the polygon\n"
            + "     (it is easy to calculate: the lon range will be minLon to maxLon, the lat range will be minLat to maxLat)\n"
            + "     and request that multidimensional rectangle from ERDDAP.\n"
            + "     You can then do the polygon extraction (or calculations) in your favorite analysis software (ArcGIS, Matlab, Python, R, ...).\n"
            + "     An advantage of this approach is that there is no limit to the number of points that define the polygon.\n"
            + "     Note that this is the way the OPeNDAP standard (which ERDDAP uses for these requests) works:\n"
            + "     users request a multidimensional rectangular subset of the original arrays\n"
            + "     and get a response in similar arrays (the same axis and data variable names, just smaller arrays,\n"
            + "     <br>e.g., var[minTime:maxTime][minLat:maxLat][minLon:maxLon]), so it is unclear\n"
            + "     what data structure the user would want for a polygon response.\n"
            + "     <br>&nbsp;\n"
            +

            // Graphics Commands
            "   <li><a class=\"selfLink\" id=\"GraphicsCommands\" href=\"#GraphicsCommands\" rel=\"bookmark\"><strong>Graphics Commands</strong></a> -\n"
            + "       <a class=\"selfLink\" id=\"MakeAGraph\" href=\"#MakeAGraph\" rel=\"bookmark\">griddap</a> extends the OPeNDAP standard by allowing graphics commands\n"
            + "     in the query.\n"
            + "     The Make A Graph web pages simplify the creation of URLs with these graphics commands\n"
            + "     (see the \"graph\" links in the table of <a href=\""
            + dapBase
            + "index.html\">griddap datasets</a>). \n"
            + "     So we recommend using the Make A Graph web pages to generate URLs, and then, when\n"
            + "     needed, using the information here to modify the URLs for special purposes.\n"
            + "     These commands are optional.\n"
            + "     If present, they must occur after the data request part of the query. \n"
            + "     These commands are used by griddap if you request an <a href=\"#imageFileTypes\">image fileType</a> (PNG or PDF) and are\n"
            + "     ignored if you request a data file (e.g., .asc). \n"
            + "     If relevant commands are not included in your request, griddap uses the defaults and tries\n"
            + "     its best to generate a reasonable graph or map.\n"
            + "     All of the commands are in the form <kbd>&amp;.<i>commandName</i>=<i>value</i></kbd> . \n"
            + "     If the value has sub-values, they are separated by the '|' character. \n"
            + "     The commands are:\n"
            + "     <ul>\n"
            + "     <li><kbd>&amp;.bgColor=<i>bgColor</i></kbd>\n"
            + "       <br>This specifies the background color of a graph (not a map).\n"
            + "       The color is specified as an 8 digit hexadecimal value in the form 0x<i>AARRGGBB</i>,\n"
            + "       where AA, RR, GG, and BB are the opacity, red, green and blue components, respectively.\n"
            + "       The canvas is always opaque white, so a (semi-)transparent graph background color\n"
            + "       blends into the white canvas.\n"
            + "       The color value string is not case sensitive.\n"
            + "       For example, a fully opaque (ff) greenish-blue color with red=22, green=88, blue=ee\n"
            + "       would be 0xff2288ee. Opaque white is 0xffffffff. Opaque light blue is 0xffccccff.\n"
            + "       The default on this ERDDAP is "
            + String2.to0xHexString(EDStatic.config.graphBackgroundColor.getRGB(), 8)
            + ".\n"
            + "     <li><kbd>&amp;.colorBar=<i>palette</i>|<i>continuous</i>|<i>scale</i>|<i>min</i>|<i>max</i>|<i>nSections</i></kbd> \n"
            + "       <br>This specifies the settings for a color bar.  The sub-values are:\n"
            + "        <ul>\n"
            +
            // the standard palettes are listed in 'palettes' in Bob's messages.xml,
            //        EDDTable.java, EDDGrid.java, and setupDatasetsXml.html
            "        <li><i>palette</i> - All ERDDAP installations support a standard set of palettes:\n"
            + "          BlackBlueWhite, BlackRedWhite, BlackWhite, BlueWhiteRed, LightRainbow,\n"
            + "          Ocean, OceanDepth, Rainbow, RedWhiteBlue, ReverseRainbow, Topography,\n"
            + "          TopographyDepth, WhiteBlack, WhiteBlueBlack, WhiteRedBlack.\n"
            + "          Some ERDDAP installations support additional options. See a Make A Graph\n"
            + "          web page for a complete list.\n"
            + "          The default varies based on min and max: if -1*min ~= max, the default\n"
            + "          is BlueWhiteRed; otherwise, the default is Rainbow.\n"
            + "        <li><i>continuous</i> - must be either no value (the default), <kbd>C</kbd> (for Continuous),\n"
            + "          or <kbd>D</kbd> (for Discrete). The default is different for different variables.\n"
            + "        <li><i>scale</i> - must be either no value (the default), <kbd>Linear</kbd>, or <kbd>Log</kbd>.\n"
            + "          The default is different for different variables.\n"
            + "        <li><i>min</i> - The minimum value for the color bar. \n"
            + "          The default is different for different variables.\n"
            + "        <li><i>max</i> - The maximum value for the color bar. \n"
            + "          The default is different for different variables.\n"
            + "        <li><i>nSections</i> - The preferred number of sections (for Log color bars,\n"
            + "          this is a minimum value). The default is different for different colorBar\n"
            + "          settings.\n"
            + "        </ul>\n"
            + "        If you don't specify one of the sub-values, the default for the sub-value will be used.\n"
            + "      <li><kbd>&amp;.color=<i>value</i></kbd>\n"
            + "          <br>This specifies the color for data lines, markers, vectors, etc. The value must\n"
            + "          be specified as an 0xRRGGBB value (e.g., 0xFF0000 is red, 0x00FF00 is green).\n"
            + "          The color value string is not case sensitive. The default is 0x000000 (black).\n"
            + "      <li><kbd>&amp;.draw=<i>value</i></kbd> \n"
            + "          <br>This specifies how the data will be drawn, as <kbd>lines</kbd>, <kbd>linesAndMarkers</kbd>,\n"
            + "          <kbd>markers</kbd>, <kbd>sticks</kbd>, <kbd>surface</kbd>, or <kbd>vectors</kbd>. \n"
            + "      <li><kbd>&amp;.font=<i>scaleFactor</i></kbd>\n"
            + "          <br>This specifies a scale factor for the font (e.g., 1.5 would make the font\n"
            + "          1.5 times as big as normal).\n"
            + "      <li><kbd>&amp;.land=<i>value</i></kbd>\n"
            + "        <br>This specifies how the landmask should be drawn:\n"
            + "        <br><kbd>under</kbd> (best for most variables) draws the landmask under the data.\n"
            + "        <br><kbd>over</kbd> draws the landmask over the data.\n"
            + "        <br><kbd>outline</kbd> just draws the landmask outline, political boundaries, lakes, and rivers.\n"
            + "        <br><kbd>off</kbd> doesn't draw anything.\n"
            + "        <br>The default is different for different variables (under the ERDDAP administrator's\n"
            + "        control via a drawLandMask setting in datasets.xml, or via the fallback <kbd>drawLandMask</kbd>\n"
            + "        setting in setup.xml).\n"
            + "        <kbd>under</kbd> is the best choice for most variables.\n"
            + "      <li><kbd>&amp;.legend=<i>value</i></kbd>\n"
            + "        <br>This specifies whether the legend on PNG images (not PDF's) should be at the\n"
            + "        <kbd>Bottom</kbd> (default), <kbd>Off</kbd>, or <kbd>Only</kbd> (which returns only the legend).\n"
            + "      <li><kbd>&amp;.marker=<i>markerType</i>|<i>markerSize</i></kbd>\n"
            + "        markerType is an integer: 0=None, 1=Plus, 2=X, 3=Dot, 4=Square,\n"
            + "        5=Filled Square (default), 6=Circle, 7=Filled Circle, 8=Up Triangle,\n"
            + "        9=Filled Up Triangle.\n"
            + "        markerSize is an integer from 3 to 50 (default=5)\n"
            + "      <li><kbd>&amp;.size=<i>width</i>|<i>height</i></kbd>\n"
            + "        <br>For PNG images (not PDF's), this specifies the desired size of the image, in pixels.\n"
            + "        This allows you to specify sizes other than the predefined sizes of .smallPng, .png,\n"
            + "        and .largePng, or the variable size of .transparentPng.\n"
            + "      <li><kbd>&amp;.trim=<i>trimPixels</i></kbd>\n"
            + "        <br>For PNG images (not PDF's), this tells ERDDAP to make the image shorter by removing\n"
            + "        all whitespace at the bottom, except for <i>trimPixels</i>.\n"
            + "      <li><kbd>&amp;.vars=<i>'|'-separated list</i></kbd>\n"
            + "        <br>This is a '|'-separated list of variable names. Defaults are hard to predict.\n"
            + "        The meaning associated with each position varies with the <kbd>&amp;.draw</kbd> value:\n"
            + "        <ul>\n"
            + "        <li>for lines: xAxis|yAxis\n"
            + "        <li>for linesAndMarkers: xAxis|yAxis|Color\n"
            + "        <li>for markers: xAxis|yAxis|Color\n"
            + "        <li>for sticks: xAxis|uComponent|vComponent\n"
            + "        <li>for surface: xAxis|yAxis|Color\n"
            + "        <li>for vectors: xAxis|yAxis|uComponent|vComponent\n"
            + "        </ul>\n"
            + "        If xAxis=longitude and yAxis=latitude, you get a map; otherwise, you get a graph.\n "
            + "      <li><kbd>&amp;.vec=<i>value</i></kbd>\n"
            + "        <br>This specifies the data vector length (in data units) to be scaled to the\n"
            + "        size of the sample vector in the legend. The default varies based on the data.\n"
            + "      <li><a class=\"selfLink\" id=\"xRange\" href=\"#xRange\" rel=\"bookmark\" \n"
            + "           ><kbd>&amp;.xRange=<i>min</i>|<i>max</i>|<i>ascending</i>|<i>scale</i></kbd></a>\n"
            + "        <br><kbd>&amp;.yRange=<i>min</i>|<i>max</i>|<i>ascending</i>|<i>scale</i></kbd>\n"
            + "        <br>The .xRange and .yRange commands can have any number of values.\n"
            + "        <br>If there are fewer than 4 values, the missing values are treated as default values.\n"
            + "        <br>If there are more  than 4 values, the extra values are ignored.\n"
            + "        <ul>\n"
            + "        <li><kbd>min</kbd> and <kbd>max</kbd> specify the range of the X and Y axes. They can be numeric values or\n"
            + "          nothing (the default, which tells ERDDAP to use an appropriate value based on the data).\n"
            + "        <li><i>ascending</i> specifies whether the axis is drawn the normal way (ascending), or reversed (descending, flipped).\n"
            + "          <i>ascending</i> can be nothing or <kbd>true, t, false, </kbd>or<kbd> f</kbd>. These values are case insensitive.\n"
            + "          The default is <kbd>true</kbd>.\n"
            + "          <i>ascending</i> applies to graphs only, not maps.\n"
            + "        <li><i>scale</i> specifies a preference for the axis scale.\n"
            + "          <i>scale</i> can be nothing (the default, which is dependent on the x or y axis variable's\n"
            + "          colorBarScale setting), or Linear, or Log. These values are case insensitive.\n"
            + "          <i>scale</i> applies to graphs only, not maps.\n"
            + "          <i>scale</i> will be ignored (and the axis will be linear) if the axis is a time axis,\n"
            + "          or if the minimum value on the axis is &lt;=0, or if the axis range is very narrow.\n"
            + "        </ul>\n"
            + "      </ul>\n"
            + "    <br><a class=\"selfLink\" id=\"sampleGraphURL\" href=\"#sampleGraphURL\" rel=\"bookmark\"\n"
            + "    >A sample URL to view a <strong>.png</strong> of a <strong>graph</strong> is</a> \n"
            + "    <br><a href=\""
            + fullGraphExampleHA
            + "\"><kbd>"
            + String2.replaceAll(fullGraphExampleHE, "&", "<wbr>&")
            + "</kbd></a>\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to <strong>.graph</strong>, you can see a Make A Graph\n"
            + "    web page with that request loaded:\n"
            + "    <br><a href=\""
            + fullGraphMAGExampleHA
            + "\"><kbd>"
            + String2.replaceAll(fullGraphMAGExampleHE, "&", "<wbr>&")
            + "</kbd></a>\n"
            + "    <br>That makes it easy for humans to modify an image request to make a similar graph or map.\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to a data fileType (e.g., <strong>.htmlTable</strong>),\n"
            + "    you can view or download the data that was graphed:\n"
            + "    <br><a href=\""
            + fullGraphDataExampleHA
            + "\"><kbd>"
            + String2.replaceAll(fullGraphDataExampleHE, "&", "<wbr>&")
            + "</kbd></a>\n"
            + "\n"
            + "    <p>A sample URL to view a <strong>.png</strong> of a <strong>map</strong> is \n"
            + "    <br><a href=\""
            + fullMapExampleHA
            + "\"><kbd>"
            + String2.replaceAll(fullMapExampleHE, "&", "<wbr>&")
            + "</kbd></a>\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to <strong>.graph</strong>, you can see a Make A Graph\n"
            + "    web page with that request loaded:\n"
            + "    <br><a href=\""
            + fullMapMAGExampleHA
            + "\"><kbd>"
            + String2.replaceAll(fullMapMAGExampleHE, "&", "<wbr>&")
            + "</kbd></a>\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to a data fileType (e.g., <strong>.htmlTable</strong>),\n"
            + "    you can view or download the data that was mapped:\n"
            + "    <br><a href=\""
            + fullMapDataExampleHA
            + "\"><kbd>"
            + String2.replaceAll(fullMapDataExampleHE, "&", "<wbr>&")
            + "</kbd></a>\n"
            + "  </ul>\n"
            + "</ul>\n"
            +

            // other info
            "<a class=\"selfLink\" id=\"otherInformation\" href=\"#otherInformation\" rel=\"bookmark\"><strong>Other Information</strong></a>\n"
            + "<ul>\n"
            + "<li><a class=\"selfLink\" id=\"login\" href=\"#login\" rel=\"bookmark\"><strong>Log in to access private datasets.</strong></a>\n"
            + "<br>Many ERDDAP installations don't have authentication enabled and thus\n"
            + "don't provide any way for users to login, nor do they have any private datasets.\n"
            + "<p>Some ERDDAP installations do have authentication enabled.\n"
            + "Currently, ERDDAP only supports authentication via Google-managed email accounts,\n"
            + "which includes email accounts at NOAA and many universities.\n"
            + "If an ERDDAP has authentication enabled, anyone with a Google-managed email account\n"
            + "can log in, but they will only have access to the private datasets\n"
            + "that the ERDDAP administrator has explicitly authorized them to access.\n"
            + "For instructions on logging into ERDDAP from a browser or via a script, see\n"
            + "<a rel=\"help\" href=\"https://erddap.github.io/docs/user/AccessToPrivateDatasets\">Access to Private Datasets in ERDDAP</a>.\n"
            + "\n"
            + "<li><strong><a class=\"selfLink\" id=\"longTimeRange\" href=\"#longTimeRange\" rel=\"bookmark\">Requests for a long time range</a></strong> (&gt;30 time points) from a gridded dataset\n"
            + "are prone to time out failures, which often appear as Proxy Errors, because it\n"
            + "takes significant time for ERDDAP to open all of the data files one-by-one.\n"
            + "If ERDDAP is otherwise busy during the request, the problem is more likely to occur.\n"
            + "If the dataset's files are compressed, the problem is more likely to occur,\n"
            + "although it's hard for a user to determine if a dataset's files are compressed.\n"
            + "The solution is to make several requests, each with a smaller time range.\n"
            + "How small of a time range? Start really small (~30 time points?),\n"
            + "then (approximately) double the time range until the request fails,\n"
            + "then go back one doubling.\n"
            + "Then make all the requests (each for a different chunk of time) needed to get\n"
            + "all of the data.\n"
            + "An ERDDAP administrator can lessen this problem by increasing the\n"
            + "<a rel=\"help\" href=\"https://erddap.github.io/docs/server-admin/deploy-install#apache-timeout\">Apache timeout settings</a>.\n"
            + "<br>&nbsp;\n"
            + "<li><a class=\"selfLink\" id=\"dataModel\" href=\"#dataModel\" rel=\"bookmark\"><strong>Data Model</strong></a>\n"
            + "  <br>Each griddap dataset can be represented as:\n"
            + "  <ul>\n"
            + "  <li>An ordered list of one or more 1-dimensional axis variables, each of which has an\n"
            + "      associated dimension with the same name.\n"
            + "      <ul>\n"
            + "      <li>Each axis variable has data of one specific data type.\n"
            + "        See the <a rel=\"help\" href=\"https://erddap.github.io/docs/server-admin/datasets#data-types\">Data Type documentation</a> for details.\n"
            + "        If the data source has a dimension with a size but no values, ERDDAP uses the integer values\n"
            + "        0, 1, 2, ...\n"
            + "        Axis variables can't contain char or String data.\n"
            + "      <li>The minimum number of axis variables is 1.\n"
            + "      <li>The maximum number of axis variables is 2147483647, but most datasets have 4 or fewer.\n"
            + "      <li>The maximum size of a dimension is 2147483647 values.\n"
            + "      <li>The maximum product of all dimension sizes is, in theory, about ~9e18.\n"
            + "      <li>Missing values are not allowed.\n"
            + "      <li>The values MUST be sorted in either ascending (recommended) or descending order.\n"
            + "        Unsorted values are not allowed because <kbd>[(start):(stop)]</kbd> requests must\n"
            + "        translate unambiguously into a contiguous range of indices.\n"
            + "        Tied values are not allowed because requests for a single <kbd>[(value)]</kbd> must\n"
            + "        translate unambiguously to one index. Also, the\n"
            + "        <a rel=\"help\" href=\"https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#terminology\">CF Conventions"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "        require that \"coordinate variables\", as it calls them, be \"ordered monotonically\".\n"
            + "      <li>Each axis variable has a name composed of a letter (A-Z, a-z) and then 0 or more\n"
            + "        characters (A-Z, a-z, 0-9, _).\n"
            + "      <li> Each axis variable has metadata which is a set of Key=Value pairs.\n"
            + "      </ul>\n"
            + "  <li>A set of one or more n-dimensional data variables.\n"
            + "      <ul>\n"
            + "      <li>All data variables use all of the axis variables, in order, as their dimensions.\n"
            + "      <li>Each data variable has data of one\n"
            + "        <a rel=\"help\" href=\"https://erddap.github.io/docs/server-admin/datasets#data-types\">data type</a>.\n"
            + "      <li>Missing values are allowed.\n"
            + "      <li>Each data variable has a name composed of a letter (A-Z, a-z) and then 0\n"
            + "        or more characters (A-Z, a-z, 0-9, _).\n"
            + "      <li>The maximum number of data variables in a dataset is 2147483647, but datasets with\n"
            + "        greater than about 100 data variables will be awkward for users.\n"
            + "      <li>Each data variable has metadata which is a set of Key=Value pairs.\n"
            + "      </ul>\n"
            + "  <li>The dataset has Global metadata which is a set of Key=Value pairs.\n"
            + "  <li>Note about metadata: each variable's metadata and the global metadata is a set of 0\n"
            + "      or more <kbd><i>Key</i>=<i>Value</i></kbd> pairs.\n"
            + "      <ul>\n"
            + "      <li>Each Key is a String consisting of a letter (A-Z, a-z) and then 0 or more other\n"
            + "        characters (A-Z, a-z, 0-9, '_').\n"
            + "      <li>Each Value is an array of one (usually) or more values of one\n"
            + "        <a rel=\"help\" href=\"https://erddap.github.io/docs/server-admin/datasets#data-types\">data type</a>.\n"
            + "      <br>&nbsp;\n"
            + "      </ul>\n"
            + "  </ul>\n"
            + "<li><a class=\"selfLink\" id=\"specialVariables\" href=\"#specialVariables\" rel=\"bookmark\"><strong>Special Variables</strong></a>\n"
            + "  <br>ERDDAP is aware of the spatial and temporal features of each dataset. The longitude,\n"
            + "  latitude, altitude, depth, and time axis variables (when present) always have specific\n"
            + "  names and units. This makes it easier for you to identify datasets with relevant data,\n"
            + "  to request spatial and temporal subsets of the data, to make images with maps or\n"
            + "  time-series, and to save data in geo-referenced file types (e.g., .esriAscii and .kml).\n"
            + "  <ul>\n"
            + "  <li>In griddap, a longitude axis variable (if present) always has the name \""
            + EDV.LON_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.LON_UNITS
            + "\".\n"
            + "  <li>In griddap, a latitude axis variable (if present) always has the name, \""
            + EDV.LAT_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.LAT_UNITS
            + "\".\n"
            + "  <li>In griddap, an altitude axis variable (if present) always has the name \""
            + EDV.ALT_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.ALT_UNITS
            + "\" above sea level.\n"
            + "    Locations below sea level have negative altitude values.\n"
            + "  <li>In griddap, a depth axis variable (if present) always has the name \""
            + EDV.DEPTH_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.DEPTH_UNITS
            + "\" below sea level.\n"
            + "    Locations below sea level have positive depth values.\n"
            + "  <li>In griddap, a time axis variable (if present) always has the name \""
            + EDV.TIME_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.TIME_UNITS
            + "\".\n"
            + "    If you request data and specify a start and/or stop value for the time axis,\n"
            + "    you can specify the time as a number (in seconds since 1970-01-01T00:00:00Z)\n"
            + "    or as a String value (e.g., \"2002-12-25T07:00:00Z\" in the GMT/Zulu time zone).\n"
            + "    <br>&nbsp;\n"
            + "  </ul>\n"
            + "<li><a class=\"selfLink\" id=\"incompatibilities\" href=\"#incompatibilities\" rel=\"bookmark\"><strong>Incompatibilities</strong></a>\n"
            + "  <ul>\n"
            + "  <li>File Types - Some results file types have restrictions.\n"
            + "    For example, .kml is only appropriate for results with a range of longitude and\n"
            + "    latitude values.\n"
            + "    If a given request is incompatible with the requested file type, griddap throws an error.\n"
            + "    <br>&nbsp;\n"
            + "  </ul>\n"
            + "<li>"
            + EDStatic.messages.acceptEncodingHtml(language, "h3", tErddapUrl)
            + "    <br>&nbsp;\n"
            + "<li><a class=\"selfLink\" id=\"citeDataset\" href=\"#citeDataset\" rel=\"bookmark\"><strong>How to Cite a Dataset in a Paper</strong></a>\n"
            + "<br>It is important to let readers of your paper know how you got the data that\n"
            + "you used in your paper. For each dataset that you used, please look at the\n"
            + "dataset's metadata in the Dataset Attribute Structure section at the bottom\n"
            + "of the .html page for the dataset, e.g.,\n"
            + "<br><a rel=\"help\" \n"
            + "  href=\"https://coastwatch.pfeg.noaa.gov/erddap/griddap/jplMURSST41.html\"\n"
            + "  >https://coastwatch.pfeg.noaa.gov/erddap/griddap/jplMURSST41.html</a> .\n"
            + "<br>The metadata sometimes includes a required or suggested citation format for\n"
            + "the dataset. The \"license\" metadata sometimes lists restrictions on the\n"
            + "use of the data.\n"
            + "\n"
            + "<p>To generate a citation for a dataset:\n"
            + "<br>If you think of the dataset as a scientific article, you can generate a \n"
            + "citation based on the author (see the \"creator_name\" or \"institution\" metadata),\n"
            + "the date that you downloaded the data, the title (see the \"title\" metadata),\n"
            + "and the publisher (see the \"publisher_name\" metadata).\n"
            + "If possible, please include the specific URL(s) used to download the data.\n"
            + "If the dataset's metadata includes a\n"
            + "<a rel=\"help\" \n"
            + "  href=\"https://en.wikipedia.org/wiki/Digital_object_identifier\"\n"
            + "  >Digital Object Identifier (DOI)<img \n"
            + "  src=\""
            + tErddapUrl
            + "/images/external.png\" alt=\"(external link)\"\n"
            + "  title=\"This link to an external website does not constitute an endorsement.\"></a>, please\n"
            + "include that in the citation you create.\n"
            + "<br>&nbsp;\n"
            + "<li><a class=\"selfLink\" id=\"Errors\" href=\"#Errors\" rel=\"bookmark\"><strong>Errors</strong></a>\n"
            + "<br>Like other parts of the Internet (e.g., Tomcat, Apache, routers, your browser),\n"
            + "ERDDAP uses various HTTP status/error codes to make it easy to\n"
            + "distinguish different types of errors, including:\n"
            + "  <ul>\n"
            + "  <li>400 Bad Request - for syntax errors in the request URL and other request errors that will never succeed.\n"
            + "    If you fix the stated problem, your request should succeed\n"
            + "    (or you'll get a different error message for a different problem).\n"
            + "  <li>401 Unauthorized - when the user isn't currently authorized to access a given dataset.\n"
            + "    If you get this and think it unwarranted, please \n"
            + "     <a rel=\"help\" href=\"#contact\">email this ERDDAP's administrator</a> to try to resolve the problem.\n"
            + "  <li>403 Forbidden - when the user's IP address is on this ERDDAP's blacklist.\n"
            + "    If you get this and think it unwarranted, please \n"
            + "     <a rel=\"help\" href=\"#contact\">email this ERDDAP's administrator</a> to try to resolve the problem.\n"
            + "  <li>404 Not Found - for datasetID not found (perhaps temporarily),\n"
            + "    \"Your query produced no matching results.\", or other request errors\n"
            + "    If the datasetID was not found but was there a few minutes ago, wait 15 minutes and try again.\n"
            + "  <li>408 Timeout - for timeouts. Often, these can be fixed by making a request for less data, e.g., a shorter time period.\n"
            + "  <li>413 Payload Too Large - when there isn't enough memory currently (or ever) available to process the response,\n"
            + "    or when the response is too large for the requested file type.\n"
            + "    Often, these can be fixed by making a request for less data, e.g., a shorter time period.\n"
            + "  <li>416 Range Not Satisfiable - for invalid byte range requests. Note that ERDDAP's\n"
            + "    \"files\" system does not allow byte range requests to the individual\n"
            + "    .nc, .hdf, .bz2, .gz, .gzip, .tar, .tgz, .z, and .zip files\n"
            + "    (although the exact list may vary for different ERDDAP installations)\n"
            + "    because that approach is horribly inefficient (tens/hundreds/thousands of requests and\n"
            + "    the transfer of tons of data unnecessarily) and some of the client software is buggy in a way\n"
            + "    that causes problems for ERDDAP (tens/hundreds/thousands of open/broken sockets). Instead, either download the entire\n"
            + "    file (especially if you need repeated use of different parts of the file)\n"
            + "    or use the griddap or tabledap services to request a subset of a dataset via an (OPeN)DAP request.\n"
            + "    That's what DAP was designed for and does so well. Just because you can saw a log with\n "
            + "    a steak knife (sometimes) doesn't make it a good idea -- use your chainsaw.\n"
            + "  <li>500 Internal Server Error - for errors that aren't the user's fault or responsibility.\n"
            + "    For code=500 errors that look like they should use a different code, or for\n"
            + "    errors that look like bugs in ERDDAP, please email the URL and error message to erd.data at noaa.gov .\n"
            + "  </ul>\n"
            + "<p>If the error response was generated by ERDDAP (not by some other part of the Internet,\n"
            + "  e.g., Tomcat, Apache, routers, or your browser),\n"
            + "  it will come with an (OPeN)DAPv2.0-formatted, plain text, UTF-8-encoded error message\n"
            + "  as the payload of the response, e.g.,\n"
            + "<br><kbd>Error {\n"
            + "<br>&nbsp;&nbsp;&nbsp;&nbsp;code=404;\n"
            + "<br>&nbsp;&nbsp;&nbsp;&nbsp;message=\"Not Found: Your query produced no matching results.\n"
            + "(time&gt;=2019-03-27T00:00:00Z is outside of the variable's actual_range: 1970-02-26T20:00:00Z to 2019-03-26T15:00:00Z)\";\n"
            + "<br>}</kbd>\n"
            + "<br>Notes:\n"
            + "<ul>\n"
            + "<li>The <kbd>message</kbd> is a JSON-encoded string. The first few words, e.g., Not Found,\n"
            + "  are the generic name of the HTTP error code (e.g., HTTP error 404).\n"
            + "<li>Of course, status code 200 OK (with the relevant payload)\n"
            + "  is used for completely successful requests.\n"
            + "<li>Errors generated by other parts of the Internet (e.g., Tomcat, Apache, routers, or your browser)\n"
            + "  won't return an (OPeN)DAPv2.0-formatted, plain text, error message.\n"
            + "  In fact, they may not return any payload with the HTTP error code.\n"
            + "  In a browser, errors are obvious. But in a script, the best way to check for errors\n"
            + "  is to look at the HTTP status code first (is it 200=OK ?) and only secondarily\n"
            + "  see if there is an error message payload (which may not be (OPeN)DAPv2.0-formatted).\n"
            + "<li>If you need help resolving a problem, email the URL and error message to \n"
            + "  <a rel=\"help\" href=\"#contact\">this ERDDAP's administrator</a> to try to resolve the problem.\n"
            + "  Or, you can join the <a rel=\"help\"\n"
            + "  href=\"#ERDDAPMailingList\">ERDDAP Google Group / Mailing List</a> \n"
            + "  and post the URL, the error message, and your question there.\n"
            + "<li>If you see errors that have been assigned the wrong HTTP status code\n"
            + "  (especially code=500 errors that should be 4xx errors) or might be evidence of a bug in ERDDAP,\n"
            + "  please email the URL and error message to erd.data at noaa.gov .\n"
            + "</ul>\n"
            + "<br>&nbsp;\n"
            + "</ul>\n"
            + "<h2><a class=\"selfLink\" id=\"contact\" href=\"#contact\" rel=\"bookmark\">Contact Us</a></h2>\n"
            + "If you have questions, suggestions, or comments about ERDDAP in general (not this specific\n"
            + "ERDDAP installation or its datasets), please send an email to <kbd>bob dot simons at noaa dot gov</kbd>\n"
            + "and include the ERDDAP URL directly related to your question or comment.\n"
            + "<p><a class=\"selfLink\" id=\"ERDDAPMailingList\" href=\"#ERDDAPMailingList\" rel=\"bookmark\">Or,</a> you can join the ERDDAP Google Group / Mailing List by visiting\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://groups.google.com/forum/#!forum/erddap\">https://groups.google.com/forum/#!forum/erddap<img \n"
            + "  src=\""
            + tErddapUrl
            + "/images/external.png\" alt=\" (external link)\" \n"
            + "  title=\"This link to an external website does not constitute an endorsement.\"></a> \n"
            + "and clicking on \"Apply for membership\". \n"
            + "Once you are a member, you can post your question there or search to see if the question\n"
            + "has already been asked and answered.\n"
            + "<br>&nbsp;\n");
  }

  /**
   * Get colorBarMinimum and Maximum for all grid variables in erddap. Currently, this is just set
   * up for Bob's use.
   */
  public static void suggestGraphMinMax() throws Throwable {
    String tDir = "c:/temp/griddap/";
    String tName = "datasets.tsv";

    while (true) {
      String dsName = String2.getStringFromSystemIn("Grid datasetID? ");
      if (dsName.length() == 0) dsName = "erdBAssta5day"; // hycomPacS";

      Table info = new Table();
      SSR.downloadFile(
          "https://coastwatch.pfeg.noaa.gov/erddap/info/" + dsName + "/index.tsv",
          tDir + tName,
          true);

      String response[] = File2.readFromFile88591(tDir + tName);
      Test.ensureTrue(response[0].length() == 0, response[0]);
      String2.log(
          "Dataset info (500 chars):\n"
              + response[1].substring(0, Math.min(response[1].length(), 500)));

      info.readASCII(tDir + tName);
      // String2.log(info.toString());

      // generate request for data for range of lat and lon  and middle one of other axes
      StringBuilder subset = new StringBuilder();
      StringArray dataVars = new StringArray();
      int nDim = 0;
      for (int row = 0; row < info.nRows(); row++) {
        String type = info.getStringData(0, row);
        String varName = info.getStringData(1, row);
        if (type.equals("variable")) {
          dataVars.add(varName);
          continue;
        }

        if (!type.equals("dimension")) continue;

        // deal with dimensions
        nDim++;
        String s4[] = String2.split(info.getStringData(4, row), ',');
        int nValues = String2.parseInt(String2.split(s4[0], '=')[1]);
        String2.log(varName + " " + nValues);
        if (varName.equals("longitude"))
          subset.append("[0:" + (nValues / 36) + ":" + (nValues - 1) + "]");
        else if (varName.equals("latitude"))
          subset.append("[0:" + (nValues / 18) + ":" + (nValues - 1) + "]");
        else subset.append("[" + (nValues / 2) + "]");
      }
      String2.log("subset=" + subset + "\nnDim=" + nDim + " vars=" + dataVars);

      // get suggested range for each dataVariable
      Table data = new Table();
      int ndv = dataVars.size();
      for (int v = 0; v < ndv; v++) {
        try {
          String varName = dataVars.get(v);
          SSR.downloadFile(
              "https://coastwatch.pfeg.noaa.gov/erddap/griddap/"
                  + dsName
                  + ".tsv?"
                  + varName
                  + subset,
              tDir + tName,
              true);

          response = File2.readFromFile88591(tDir + tName);
          Test.ensureTrue(response[0].length() == 0, response[0]);
          if (response[1].startsWith("<!DOCTYPE HTML")) {
            int start = response[1].indexOf("The error:");
            int stop = response[1].length();
            if (start >= 0) {
              start = response[1].indexOf("Your request URL:");
              stop = response[1].indexOf("</tr>", start);
              stop = response[1].indexOf("</tr>", stop);
              stop = response[1].indexOf("</tr>", stop);
            }
            if (start < 0) {
              start = 0;
              stop = response[1].length();
            }
            String2.log(
                "Response for varName="
                    + varName
                    + ":\n"
                    + String2.replaceAll(response[1].substring(start, stop), "<br>", "\n<br>"));
          }

          data.readASCII(tDir + tName);
          PrimitiveArray pa = data.getColumn(data.nColumns() - 1);
          double stats[] = pa.calculateStats();
          double tMin = stats[PrimitiveArray.STATS_MIN];
          double tMax = stats[PrimitiveArray.STATS_MAX];
          double range = tMax - tMin;
          double loHi[] =
              Math2.suggestLowHigh(tMin + range / 10, tMax - range / 10); // interior range
          String2.log(
              "varName="
                  + varName
                  + " min="
                  + tMin
                  + " max="
                  + tMax
                  + "\n"
                  + "                <att name=\"colorBarMinimum\" type=\"double\">"
                  + loHi[0]
                  + "</att>\n"
                  + "                <att name=\"colorBarMaximum\" type=\"double\">"
                  + loHi[1]
                  + "</att>\n");

        } catch (Throwable t) {
          String2.log("\n" + MustBe.throwableToString(t));
        }
      }
    }
  }

  /**
   * This responds by writing WCS info for this dataset.
   *
   * @param request the request
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Caller should have
   *     already checked that user has access to this dataset.
   */
  public void wcsInfo(HttpServletRequest request, int language, String loggedInAs, Writer writer)
      throws Throwable {

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + EDDGrid.wcsServer;
    String getCapabilities = wcsUrl + "?service=WCS&amp;request=GetCapabilities";
    String wcsExample = wcsUrl + "?NotYetFinished"; // EDStatic.wcsSampleStation;
    writer.write(
        EDStatic.youAreHere(request, language, loggedInAs, "Web Coverage Service (WCS)")
            + "\n"
            + "<h2>Overview</h2>\n"
            + "In addition to making data available via \n"
            + "<a rel=\"bookmark\" href=\""
            + tErddapUrl
            + "/griddap/index.html\">gridddap</a> and \n"
            + "<a rel=\"bookmark\" href=\""
            + tErddapUrl
            + "/tabledap/index.html\">tabledap</a>,\n"
            + "ERDDAP makes some datasets available via ERDDAP's Web Coverage Service (WCS) web service.\n"
            + "\n"
            + "<p>See the\n"
            + "<a rel=\"bookmark\" href=\""
            + tErddapUrl
            + "/wcs/index.html\">list of datasets available via WCS</a>\n"
            + "at this ERDDAP installation.\n"
            + "\n"
            + "<p>"
            + String2.replaceAll(
                EDStatic.messages.wcsLongDescriptionHtmlAr[language], "&erddapUrl;", tErddapUrl)
            + "\n"
            + "\n"
            + "<p>WCS clients send HTTP POST or GET requests (specially formed URLs) to the WCS service and get XML responses.\n"
            + "See this <a rel=\"bookmark\" href=\"https://en.wikipedia.org/wiki/Web_Coverage_Service#WCS_Implementations\" \n"
            + ">list of WCS clients (and servers)"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "<h2>Sample WCS Requests</h2>\n"
            + "<ul>\n"
            + "<li>You can request the list of capabilities via \n"
            + "  <a href=\""
            + getCapabilities
            + "\">"
            + getCapabilities
            + "</a>.\n"
            + "<li>A sample data request is \n"
            + "  <a href=\""
            + wcsExample
            + "\">"
            + wcsExample
            + "</a>.\n"
            + "</ul>\n");
  }

  /**
   * This returns the WCS capabilities xml for this dataset (see Erddap.doWcs). This should only be
   * called if accessibleViaWCS is true and loggedInAs has access to this dataset (so redirected to
   * login, instead of getting an error here).
   *
   * @param request the request
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in).
   * @param version Currently, only "1.0.0" is supported.
   * @param writer In the end, the writer is flushed, not closed.
   */
  public void wcsGetCapabilities(
      HttpServletRequest request, int language, String loggedInAs, String version, Writer writer)
      throws Throwable {

    if (accessibleViaWCS().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr) + accessibleViaWCS());

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + wcsServer;
    String titleXml = XML.encodeAsXML(title());
    String keywordsSA[] = keywords();
    EDVGridAxis lonEdv = axisVariables[lonIndex];
    EDVGridAxis latEdv = axisVariables[latIndex];
    EDVGridAxis timeEdv = timeIndex < 0 ? null : axisVariables[timeIndex];
    String lonLatLowerCorner = lonEdv.destinationMinString() + " " + latEdv.destinationMinString();
    String lonLatUpperCorner = lonEdv.destinationMaxString() + " " + latEdv.destinationMaxString();

    // ****  WCS 1.0.0
    // https://download.deegree.org/deegree2.2/docs/htmldocu_wcs/deegree_wcs_documentation_en.html
    // see THREDDS from July 2009:
    // https://thredds1.pfeg.noaa.gov/thredds/wcs/satellite/MH/chla/8day?request=GetCapabilities&version=1.0.0&service=WCS
    if (version == null || version.equals("1.0.0")) {
      if (version == null) version = "1.0.0";
      writer.write( // WCS 1.0.0
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<WCS_Capabilities version=\""
              + version
              + "\"\n"
              + "  xmlns=\"http://www.opengis.net/wcs\"\n"
              + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
              + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
              + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
              + "  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/wcsCapabilities.xsd\"\n"
              +
              // " updateSequence=\"" + Calendar2.getCurrentISODateTimeStringZulu() + "Z\" +
              "  >\n"
              + "  <Service>\n"
              + "    <description>"
              + XML.encodeAsXML(summary())
              + "</description>\n"
              + "    <name>"
              + datasetID
              + "</name>\n"
              + "    <label>"
              + titleXml
              + "</label>\n"
              + "    <keywords>\n");
      for (String s : keywordsSA)
        writer.write("      <keyword>" + XML.encodeAsXML(s) + "</keyword>\n");
      writer.write(
          "    </keywords>\n"
              + "    <responsibleParty>\n"
              + "      <individualName>"
              + XML.encodeAsXML(EDStatic.config.adminIndividualName)
              + "</individualName>\n"
              + "      <organisationName>"
              + XML.encodeAsXML(EDStatic.config.adminInstitution)
              + "</organisationName>\n"
              + "      <positionName>"
              + XML.encodeAsXML(EDStatic.config.adminPosition)
              + "</positionName>\n"
              + "      <contactInfo>\n"
              + "        <phone>\n"
              + "          <voice>"
              + XML.encodeAsXML(EDStatic.config.adminPhone)
              + "</voice>\n"
              + "        </phone>\n"
              + "        <address>\n"
              + "          <deliveryPoint>"
              + XML.encodeAsXML(EDStatic.config.adminAddress)
              + "</deliveryPoint>\n"
              + "          <city>"
              + XML.encodeAsXML(EDStatic.config.adminCity)
              + "</city>\n"
              + "          <administrativeArea>"
              + XML.encodeAsXML(EDStatic.config.adminStateOrProvince)
              + "</administrativeArea>\n"
              + "          <postalCode>"
              + XML.encodeAsXML(EDStatic.config.adminPostalCode)
              + "</postalCode>\n"
              + "          <country>"
              + XML.encodeAsXML(EDStatic.config.adminCountry)
              + "</country>\n"
              + "          <electronicMailAddress>"
              + XML.encodeAsXML(EDStatic.config.adminEmail)
              + "</electronicMailAddress>\n"
              + "        </address>\n"
              + "        <onlineResource xlink:href=\""
              + tErddapUrl
              + "/wcs/documentation.html\" xlink:type=\"simple\"/>\n"
              + "      </contactInfo>\n"
              + "    </responsibleParty>\n"
              + "    <fees>"
              + XML.encodeAsXML(fees())
              + "</fees>\n"
              + "    <accessConstraints>"
              + XML.encodeAsXML(accessConstraints())
              + "</accessConstraints>\n"
              + "  </Service>\n"
              + "  <Capability>\n"
              + "    <Request>\n"
              + "      <GetCapabilities>\n"
              + "        <DCPType>\n"
              + "          <HTTP>\n"
              + "            <Get>\n"
              + "              <OnlineResource xlink:href=\""
              + wcsUrl
              + "?\" xlink:type=\"simple\" />\n"
              + "            </Get>\n"
              + "          </HTTP>\n"
              + "        </DCPType>\n"
              + "      </GetCapabilities>\n"
              + "      <DescribeCoverage>\n"
              + "        <DCPType>\n"
              + "          <HTTP>\n"
              + "            <Get>\n"
              + "              <OnlineResource xlink:href=\""
              + wcsUrl
              + "?\" xlink:type=\"simple\" />\n"
              + "            </Get>\n"
              + "          </HTTP>\n"
              + "        </DCPType>\n"
              + "      </DescribeCoverage>\n"
              + "      <GetCoverage>\n"
              + "        <DCPType>\n"
              + "          <HTTP>\n"
              + "            <Get>\n"
              + "              <OnlineResource xlink:href=\""
              + wcsUrl
              + "?\" xlink:type=\"simple\" />\n"
              + "            </Get>\n"
              + "          </HTTP>\n"
              + "        </DCPType>\n"
              + "      </GetCoverage>\n"
              + "    </Request>\n"
              + "    <Exception>\n"
              + "      <Format>application/vnd.ogc.se_xml</Format>\n"
              + "    </Exception>\n"
              + "  </Capability>\n"
              + "  <ContentMetadata>\n");

      // gather info common to all variables
      StringBuilder varInfo =
          new StringBuilder(
              "      <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n"
                  + "        <gml:pos>"
                  + lonLatLowerCorner
                  + "</gml:pos>\n"
                  + "        <gml:pos>"
                  + lonLatUpperCorner
                  + "</gml:pos>\n");
      if (timeEdv != null)
        varInfo.append(
            "        <gml:timePosition>"
                + timeEdv.destinationMinString()
                + "</gml:timePosition>\n"
                + "        <gml:timePosition>"
                + timeEdv.destinationMaxString()
                + "</gml:timePosition>\n");
      varInfo.append("      </lonLatEnvelope>\n");
      String varInfoString = varInfo.toString();

      // write for each dataVariable...
      for (EDV dataVariable : dataVariables) {
        writer.write(
            "    <CoverageOfferingBrief>\n"
                + "      <name>"
                + XML.encodeAsXML(dataVariable.destinationName())
                + "</name>\n"
                + "      <label>"
                + XML.encodeAsXML(dataVariable.longName())
                + "</label>\n"
                + varInfoString);

        writer.write("    </CoverageOfferingBrief>\n");
      }

      writer.write(
          """
                </ContentMetadata>
              </WCS_Capabilities>
              """);

      // **** getCapabilities 1.1.2
      // based on WCS 1.1.2 spec, Annex C
      // also https://wiki.ucar.edu/display/NNEWD/MIT+Lincoln+Laboratory+Web+Coverage+Service
      /*} else if (version == null || version.equals("1.1.0") || version.equals("1.1.1") ||
                           version.equals("1.1.2")) {
                  if (version == null)
                      version = "1.1.2";
                  writer.write(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<Capabilities version=\"" + version + "\"\n" +
      "  xmlns=\"http://www.opengis.net/wcs/1.1\"\n" +
      "  xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n" +
      "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n" +
      "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n" +
      "  xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1 ../wcsGetCapabilities.xsd http://www.opengis.net/ows/1.1 ../../../ows/1.1.0/owsAll.xsd\"\n" +
      //"  updateSequence=\"" + Calendar2.getCurrentISODateTimeStringZulu() + "Z\" +
      "  >\n" +
      "  <ows:ServiceIdentification>\n" +
      "    <ows:Title>" + titleXml + "</ows:Title>\n" +
      "    <ows:Abstract>" + XML.encodeAsXML(summary()) + "</ows:Abstract>\n" +
      "    <ows:Keywords>\n");
                  for (int i = 0; i < keywordsSA.length; i++)
                      writer.write(
      "      <ows:keyword>" + XML.encodeAsXML(keywordsSA[i]) + "</ows:keyword>\n");
                  writer.write(
      "    </ows:Keywords>\n" +
      "    <ows:ServiceType>WCS</ows:ServiceType>\n");
                  for (int v = 0; v < wcsVersions.length; v++)
                      writer.write(
      "    <ows:ServiceTypeVersion>" + wcsVersions[v] + "</ows:ServiceTypeVersion>\n");
                  writer.write(
      "    <ows:Fees>" + XML.encodeAsXML(fees()) + "</ows:Fees>\n" +
      "    <ows:AccessConstraints>" + XML.encodeAsXML(accessConstraints()) + "</ows:AccessConstraints>\n" +
      "  </ows:ServiceIdentification>\n" +
      "  <ows:ServiceProvider>\n" +
      "    <ows:ProviderName>" + XML.encodeAsXML(EDStatic.config.adminInstitution) + "</ows:ProviderName>\n" +
      "    <ows:ProviderSite xlink:href=\"" + XML.encodeAsXML(EDStatic.erddapUrl) + "\">\n" +
      "    <ows:ServiceContact>\n" +
      "      <ows:IndividualName>" + XML.encodeAsXML(EDStatic.config.adminIndividualName) + "</ows:IndividualName>\n" +
      "      <ows:PositionName>" + XML.encodeAsXML(EDStatic.config.adminPosition) + "</ows:PositionName>\n" +
      "      <ows:ContactInfo>\n" +
      "        <ows:Phone>\n" +
      "          <ows:Voice>" + XML.encodeAsXML(EDStatic.config.adminPhone) + "</ows:Voice>\n" +
      "        </ows:Phone>\n" +
      "        <ows:Address>\n" +
      "          <ows:DeliveryPoint>" + XML.encodeAsXML(EDStatic.config.adminAddress) + "</ows:DeliveryPoint>\n" +
      "          <ows:City>" + XML.encodeAsXML(EDStatic.config.adminCity) + "</ows:City>\n" +
      "          <ows:AdministrativeArea>" + XML.encodeAsXML(EDStatic.config.adminStateOrProvince) + "</ows:AdministrativeArea>\n" +
      "          <ows:PostalCode>" + XML.encodeAsXML(EDStatic.config.adminPostalCode) + "</ows:PostalCode>\n" +
      "          <ows:Country>" + XML.encodeAsXML(EDStatic.config.adminCountry) + "</ows:Country>\n" +
      "          <ows:ElectronicMailAddress>" + XML.encodeAsXML(EDStatic.config.adminEmail) + "</ows:ElectronicMailAddress>\n" +
      "        </ows:Address>\n" +
      "      </ows:ContactInfo>\n" +
      "      <ows:Role>ERDDAP/WCS Administrator</ows:Role>\n" +
      "    </ows:ServiceContact>\n" +
      "  </ows:ServiceProvider>\n" +
      "  <ows:OperationsMetadata>\n" +
      "    <ows:Operation name=\"GetCapabilities\">\n" +
      "      <ows:DCP>\n" +
      "        <ows:HTTP>\n" +
      "          <ows:Get xlink:href=\"" + wcsUrl + "?\"/>\n" +
      //"          <ows:Post xlink:href=\"" + wcsUrl + "?\"/>\n" +
      "        </ows:HTTP>\n" +
      "      </ows:DCP>\n" +
      "    </ows:Operation>\n" +
      "    <ows:Operation name=\"GetCoverage\">\n" +
      "      <ows:DCP>\n" +
      "        <ows:HTTP>\n" +
      "          <ows:Get xlink:href=\"" + wcsUrl + "?\"/>\n" +
      //"          <ows:Post xlink:href=\"" + wcsUrl + "?\"/>\n" +
      "        </ows:HTTP>\n" +
      "      </ows:DCP>\n" +
      "      <ows:Parameter name=\"Format\">\n" +
      "        <ows:AllowedValues>\n");
                  for (int f = 0; f < wcsRequestFormats112.length; f++)
                      writer.write(
      "          <ows:Value>" + wcsRequestFormats112[f] + "</ows:Value>\n");
                  writer.write(
      "        </ows:AllowedValues>\n" +
      "      </ows:Parameter>\n" +
      "    </ows:Operation>\n" +
      //???not yet supported?
      "    <ows:Operation name=\"DescribeCoverage\">\n" +
      "      <ows:DCP>\n" +
      "        <ows:HTTP>\n" +
      "          <ows:Get xlink:href=\"" + wcsUrl + "?\"/>\n" +
      "        </ows:HTTP>\n" +
      "      </ows:DCP>\n" +
      "      <ows:Parameter name=\"Format\">\n" +
      "        <ows:AllowedValues>\n" +
      "          <ows:Value>text/xml</ows:Value>\n" +
      "        </ows:AllowedValues>\n" +
      "      </ows:Parameter>\n" +
      "    </ows:Operation>\n" +
      "  </ows:OperationsMetadata>\n" +
      "  <Contents>\n");

                  //for each dataVariable
                  for (int dv = 0; dv < dataVariables.length; dv++) {
                      String longNameXml = XML.encodeAsXML(dataVariables[dv].longName());
                      writer.write(
      "    <CoverageSummary>\n" +
      "      <ows:Title>" + longNameXml + "</ows:Title>\n" +
      "      <ows:Abstract>" + longNameXml + "</ows:Abstract>\n" +
      "      <ows:Metadata type=\"other\" xlink:href=\"" + tErddapUrl + "/info/" + datasetID + "/index.html\" />\n" +
      "      <ows:WGS84BoundingBox>\n" +  //yes: lon before lat
      "        <ows:LowerCorner>" + lonLatLowerCorner + "</ows:LowerCorner>\n" +
      "        <ows:UpperCorner>" + lonLatUpperCorner + "</ows:UpperCorner>\n" +
      "      </ows:WGS84BoundingBox>\n" +
      "      <Identifier>" + XML.encodeAsXML(dataVariables[dv].destinationName()) + "</Identifier>\n" +
      "      <Domain>\n" +
      "        <SpatialDomain>\n" +
      "          <ows:BoundingBox crs=\"urn:ogc:def:crs:OGC:6.3:WGS84\">\n" +
      "            <ows:LowerCorner>" + lonLatLowerCorner + "</ows:LowerCorner>\n" +
      "            <ows:UpperCorner>" + lonLatUpperCorner + "</ows:UpperCorner>\n" +
      "          </ows:BoundingBox>\n" +
      "        </SpatialDomain>\n");

                      if (timeEdv != null) {
                          writer.write(
      "        <TemporalDomain>\n");
                          if (timeEdv.destinationMinString().equals(timeEdv.destinationMaxString())) {
                              writer.write(
      "          <TimePosition>" + timeEdv.destinationMinString() + "</TimePosition>\n");
                          } else {
                              writer.write(
      "          <TimePeriod>\n" +
      "            <BeginTime>" + timeEdv.destinationMinString() + "</BeginTime>\n" +
      "            <EndTime>"   + timeEdv.destinationMaxString() + "</EndTime>\n" +
      "          </TimePeriod>\n");
                          }
                          writer.write(
      "        </TemporalDomain>\n");
                      }

                      writer.write(
      "      </Domain>\n");

      //???NEEDS WORK   NOT YET 1.1.2-style
      //identify default value?
                      boolean rsPrinted = false;
                      for (int av = 0; av < axisVariables.length; av++) {
                          if (av == lonIndex || av == latIndex || av == timeIndex) //catch altIndex too?
                              continue;
                          EDVGridAxis edvga = axisVariables[av];
                          if (!rsPrinted) {
                              writer.write(
      "      <Range>\n");
                              rsPrinted = true;
                          }
                          writer.write(
      "        <range>\n" +
      "          <name>" + XML.encodeAsXML(edvga.destinationName()) + "</name>\n" +
      "          <label>" + XML.encodeAsXML(edvga.longName()) + "</label>\n");
                          if (edvga.sourceValues().size() == 1) {
                              writer.write(
      "          <singleValue>" + edvga.destinationMinString() + "</singleValue>\n");
                          } else {
                              writer.write(
      "          <interval>\n" +
      "            <min>" + edvga.destinationMinString() + "</min>\n" +
      "            <max>" + edvga.destinationMaxString() + "</max>\n" +
      (edvga.isEvenlySpaced()?
      "            <res>" + Math.abs(edvga.averageSpacing()) + "</res>\n" : "") +
      "          </interval>\n");
                          }
                          writer.write(
      "        </range>\n");
                      }
                      if (rsPrinted)
                          writer.write(
      "      </Range>\n");

                      writer.write(
      "    </CoverageSummary>\n");
                  }

                  //CRS and formats
                  writer.write(   //or urn:ogc:def:crs:EPSG:4326 ???
      "    <SupportedCRS>urn:ogc:def:crs,crs:EPSG:6.3:4326</SupportedCRS>\n");
                  for (int f = 0; f < wcsRequestFormats112.length; f++)
                      writer.write(
      "    <SupportedFormat>" + wcsRequestFormats112[f] + "</SupportedFormat>\n");
                  writer.write(
      "  </Contents>\n" +
      "</Capabilities>\n");
      */
    } else {
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "version="
              + version
              + " must be \""
              + wcsVersion
              + "\".");
      // one of \"" + String2.toCSSVString(wcsVersions) + "\".");

    }

    // essential
    writer.flush();
  }

  /**
   * This returns the WCS DescribeCoverage xml for this dataset (see Erddap.doWcs). This should only
   * be called if accessibleViaWCS is true and loggedInAs has access to this dataset (so redirected
   * to login, instead of getting an error here).
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in).
   * @param version Currently, only "1.0.0" is supported and null defaults to 1.0.0.
   * @param coveragesCSV a comma-separated list of the name of the desired variables (1 or more).
   * @param writer In the end, the writer is flushed, not closed.
   */
  public void wcsDescribeCoverage(
      int language, String loggedInAs, String version, String coveragesCSV, Writer writer)
      throws Throwable {

    if (accessibleViaWCS().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr) + accessibleViaWCS());

    EDVGridAxis lonEdv = axisVariables[lonIndex];
    EDVGridAxis latEdv = axisVariables[latIndex];
    EDVGridAxis timeEdv = timeIndex < 0 ? null : axisVariables[timeIndex];
    String lonLatLowerCorner = lonEdv.destinationMinString() + " " + latEdv.destinationMinString();
    String lonLatUpperCorner = lonEdv.destinationMaxString() + " " + latEdv.destinationMaxString();
    String coverages[] = String2.split(coveragesCSV, ',');
    for (String s : coverages) {
      if (String2.indexOf(dataVariableDestinationNames(), s) < 0)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                + "coverage="
                + s
                + " isn't a valid coverage name.");
    }

    // ****  WCS 1.0.0
    // https://download.deegree.org/deegree2.2/docs/htmldocu_wcs/deegree_wcs_documentation_en.html
    // see THREDDS from July 2009:
    // https://thredds1.pfeg.noaa.gov/thredds/wcs/satellite/MH/chla/8day?request=DescribeCoverage&version=1.0.0&service=WCS&coverage=MHchla
    if (version == null || version.equals("1.0.0")) {
      if (version == null) version = "1.0.0";
      writer.write( // WCS 1.0.0
          """
                      <?xml version="1.0" encoding="UTF-8"?>
                      <CoverageDescription version="1.0.0"
                        xmlns="http://www.opengis.net/wcs"
                        xmlns:xlink="https://www.w3.org/1999/xlink"
                        xmlns:gml="http://www.opengis.net/gml"
                        xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/describeCoverage.xsd"
                        >
                      """);

      // for each requested
      //// ???
      for (String coverage : coverages) {
        EDV edv = findDataVariableByDestinationName(coverage);
        writer.write(
            "  <CoverageOffering>\n"
                + "    <name>"
                + coverage
                + "</name>\n"
                + "    <label>"
                + XML.encodeAsXML(edv.longName())
                + "</label>\n"
                + "    <lonLatEnvelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n"
                + "      <gml:pos>"
                + lonLatLowerCorner
                + "</gml:pos>\n"
                + "      <gml:pos>"
                + lonLatUpperCorner
                + "</gml:pos>\n"
                + "    </lonLatEnvelope>\n"
                + "    <domainSet>\n"
                + "      <spatialDomain>\n"
                + "        <gml:Envelope srsName=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n"
                + "          <gml:pos>"
                + lonLatLowerCorner
                + "</gml:pos>\n"
                + "          <gml:pos>"
                + lonLatUpperCorner
                + "</gml:pos>\n"
                + "        </gml:Envelope>\n");

        // thredds does it, but optional, and I encourage native coord requests (not index values)
        //              writer.write(
        // "        <gml:RectifiedGrid dimension=\"" + (altEdv == null && depthEdv == null? 2 : 3) +
        // "\">\n" +
        // "          <gml:limits>\n" +
        // "            <gml:GridEnvelope>\n" +
        // "              <gml:low>0 0" + (altEdv == null && depthEdv == null? "" : " 0") +
        // "</gml:low>\n" +
        // "              <gml:high>" + (lonEdv.sourceValues().size()-1) + " " +
        //                             (latEdv.sourceValues().size()-1) +
        //                    (  altEdv != null? " " + (  altEdv.sourceValues().size()-1) :
        //                     depthEDV != null? " " + (depthEdv.sourceValues().size() - 1) : "" ) +
        //               "</gml:high>\n" +
        // "            </gml:GridEnvelope>\n" +
        // "          </gml:limits>\n" +
        // "          <gml:axisName>x</gml:axisName>\n" +
        // "          <gml:axisName>y</gml:axisName>\n" +
        // (altEdv == null && depthEdv == null? "" : "          <gml:axisName>z</gml:axisName>\n") +
        // "          <gml:origin>\n" +
        // "            <gml:pos>" + lonEdv.destinationMinString() + " " +
        //                          latEdv.destinationMinString() +
        //                    (  altEdv != null? " " +   altEdv.destinationMinString() :
        //                     depthEdv != null? " " + depthEdv.destinationMinString() : "") +
        //               "</gml:pos>\n" +
        // "          </gml:origin>\n" +
        //// ???
        // "          <gml:offsetVector>0.04166667052552379 0.0 0.0</gml:offsetVector>\n" +
        // "          <gml:offsetVector>0.0 0.0416666635795323 0.0</gml:offsetVector>\n" +
        // "          <gml:offsetVector>0.0 0.0 0.0</gml:offsetVector>\n" +
        // "        </gml:RectifiedGrid>\n");

        writer.write("      </spatialDomain>\n");

        // time
        if (timeIndex > 0) {
          writer.write("      <temporalDomain>\n");
          // !!!For time, if lots of values (e.g., 10^6), this is SLOW (e.g., 30 seconds)!!!
          PrimitiveArray destValues = timeEdv.destinationStringValues();
          int nDestValues = destValues.size();
          for (int i = 0; i < nDestValues; i++) {
            writer.write(
                "        <gml:timePosition>" + destValues.getString(i) + "Z</gml:timePosition>\n");
          }
          writer.write("      </temporalDomain>\n");
        }

        writer.write("    </domainSet>\n");

        // if there are axes other than lon, lat, time, make a rangeSet
        boolean rsPrinted = false;
        for (int av = 0; av < axisVariables.length; av++) {
          if (av == lonIndex || av == latIndex || av == timeIndex) continue;

          if (!rsPrinted) {
            rsPrinted = true;
            writer.write(
                """
                                        <rangeSet>
                                          <RangeSet>
                                            <name>RangeSetName</name>
                                            <label>RangeSetLabel</label>
                                    """);
          }

          EDVGridAxis edvga = axisVariables[av];
          writer.write(
              "        <axisDescription>\n"
                  + "          <AxisDescription>\n"
                  + "            <name>"
                  + XML.encodeAsXML(edvga.destinationName())
                  + "</name>\n"
                  + "            <label>"
                  + XML.encodeAsXML(edvga.longName())
                  + "</label>\n"
                  + "            <values>\n");
          if (edvga.sourceValues().size() == 1) {
            writer.write(
                "              <singleValue>" + edvga.destinationMinString() + "</singleValue>\n");
          } else {
            writer.write(
                "              <interval>"
                    + "                <min>"
                    + edvga.destinationMinString()
                    + "</min>\n"
                    + "                <max>"
                    + edvga.destinationMaxString()
                    + "</max>\n");
            if (edvga.isEvenlySpaced()) {
              writer.write("                <res>" + Math.abs(edvga.averageSpacing()) + "</res>\n");
            }
            writer.write("              </interval>");
          }
          writer.write(
              // default is last value
              "              <default>"
                  + edvga.destinationMaxString()
                  + "</default>\n"
                  + "            </values>\n"
                  + "          </AxisDescription>\n"
                  + "        </axisDescription>\n");
          // "        <nullValues>\n" +  //axes never have null values
          // "          <singleValue>NaN</singleValue>\n" +
          // "        </nullValues>\n" +
        } // end of av loop

        if (rsPrinted)
          writer.write(
              """
                          </RangeSet>
                        </rangeSet>
                    """);

        writer.write(
            """
                                <supportedCRSs>
                                  <requestCRSs>urn:ogc:def:crs:EPSG:4326</requestCRSs>
                                  <responseCRSs>urn:ogc:def:crs:EPSG:4326</responseCRSs>
                                  <nativeCRSs>urn:ogc:def:crs:EPSG:4326</nativeCRSs>
                                </supportedCRSs>
                                <supportedFormats>
                            """);
        for (int f = 0; f < wcsRequestFormats100.size(); f++)
          writer.write("      <formats>" + wcsRequestFormats100.get(f) + "</formats>\n");
        writer.write(
            """
                                </supportedFormats>
                                <supportedInterpolations>
                                  <interpolationMethod>none</interpolationMethod>
                                </supportedInterpolations>
                              </CoverageOffering>
                            """);
      } // end of cov loop

      writer.write("</CoverageDescription>\n");

    } else {
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "version="
              + version
              + " must be \""
              + wcsVersion
              + "\".");
      // one of \"" + String2.toCSSVString(wcsVersions) + "\".");
    }

    // essential
    writer.flush();
  }

  /**
   * This responds to WCS query. This should only be called if accessibleViaWCS is true and
   * loggedInAs has access to this dataset (so redirected to login, instead of getting an error
   * here); this doesn't check.
   *
   * @param language the index of the selected language
   * @param ipAddress The IP address of the user (for statistics).
   * @param loggedInAs or null if not logged in
   * @param wcsQuery a getCoverage query
   * @param outputStreamSource if all goes well, this method calls out.close() at the end.
   * @throws Exception if trouble (e.g., invalid query parameter)
   */
  public void wcsGetCoverage(
      int language,
      String ipAddress,
      String loggedInAs,
      String endOfRequest,
      String wcsQuery,
      OutputStreamSource outputStreamSource)
      throws Throwable {

    if (reallyVerbose) String2.log("\nrespondToWcsQuery q=" + wcsQuery);
    String requestUrl = "/wcs/" + datasetID + "/" + wcsServer;

    if (accessibleViaWCS().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr) + accessibleViaWCS);

    // parse the wcsQuery
    String dapQuery[] = wcsQueryToDapQuery(language, EDD.userQueryHashMap(wcsQuery, true));

    // get the data
    respondToDapQuery(
        language,
        null,
        null,
        ipAddress,
        loggedInAs,
        requestUrl,
        endOfRequest,
        dapQuery[0],
        outputStreamSource,
        cacheDirectory(),
        suggestFileName(loggedInAs, dapQuery[0], dapQuery[1]),
        dapQuery[1]);

    // close the outputStream
    try {
      OutputStream out = outputStreamSource.outputStream("");
      if (out instanceof ZipOutputStream zos) zos.closeEntry();
      out.close();
    } catch (Exception e) {
    }
  }

  /**
   * This converts a WCS query into an OPeNDAP query. See WCS 1.0.0 spec (finished here), Table 9,
   * pg 32 "The GetCoverageRequest expressed as Key-Value Pairs" and WCS 1.1.2 spec (NOT FINISHED
   * HERE), Table 28, pg 49
   *
   * @param language the index of the selected language
   * @param wcsQueryMap from EDD.userQueryHashMap(sosUserQuery, true); //true=names toLowerCase
   * @return [0]=dapQuery, [1]=format (e.g., .nc)
   * @throws Exception if trouble (e.g., invalid query parameter)
   */
  public String[] wcsQueryToDapQuery(int language, Map<String, String> wcsQueryMap)
      throws Throwable {

    // parse the query and build the dapQuery

    // 1.0.0 style:
    // request=GetCoverage&version=1.0.0&service=WCS
    // &format=NetCDF3&coverage=ta
    // &time=2005-05-10T00:00:00Z&vertical=100.0&bbox=-134,11,-47,57

    // service
    String service = wcsQueryMap.get("service"); // test name.toLowerCase()
    if (service == null || !service.equals("WCS"))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "service="
              + service
              + " should have been \"WCS\".");

    // version
    String version = wcsQueryMap.get("version"); // test name.toLowerCase()
    if (!wcsVersion.equals(version)) // String2.indexOf(wcsVersions, version) < 0)
    throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "version="
              + version
              + " should have been \""
              + wcsVersion
              + "\".");
    // one of \"" + String2.toCSSVString(wcsVersions) + "\".");
    boolean version100 = version.equals("1.0.0");

    // request
    String request = wcsQueryMap.get("request"); // test name.toLowerCase()
    if (request == null || !request.equals("GetCoverage"))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "request="
              + request
              + " should have been \"GetCoverage\".");

    // format
    String requestFormat = wcsQueryMap.get("format"); // test name.toLowerCase()
    int fi = String2.caseInsensitiveIndexOf(wcsRequestFormats100, requestFormat);
    if (fi < 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "format="
              + requestFormat
              + " isn't supported.");
    String responseFormat = wcsResponseFormats100.get(fi);

    // interpolation (1.0.0)
    if (wcsQueryMap.get("interpolation") != null) // test name.toLowerCase()
    throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "'interpolation' isn't supported.");

    // GridXxx (for regridding in 1.1.2)
    if (wcsQueryMap.get("gridbasecrs") != null
        || // test name.toLowerCase()
        wcsQueryMap.get("gridtype") != null
        || // test name.toLowerCase()
        wcsQueryMap.get("gridcs") != null
        || // test name.toLowerCase()
        wcsQueryMap.get("gridorigin") != null
        || // test name.toLowerCase()
        wcsQueryMap.get("gridoffsets") != null) // test name.toLowerCase()
    throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "regridding via 'GridXxx' parameters isn't supported.");

    // exceptions    optional
    String exceptions = wcsQueryMap.get("exceptions");
    if (exceptions != null && !exceptions.equals(wcsExceptions))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + "exceptions="
              + exceptions
              + " must be "
              + wcsExceptions
              + ".");

    // store (1.1.2)
    // if (wcsQueryMap.get("store") != null)  //test name.toLowerCase()
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        "'store' isn't supported.");

    // 1.0.0 coverage or 1.1.2 identifier
    String cName = version100 ? "coverage" : "identifier"; // test name.toLowerCase()
    String coverage = wcsQueryMap.get(cName);
    if (String2.indexOf(dataVariableDestinationNames(), coverage) < 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + cName
              + "="
              + coverage
              + " isn't supported.");

    // 1.0.0 bbox or 1.1.2 BoundingBox
    // wcs requires it, but here it is optional (default to max lat lon range)
    String bboxName = version100 ? "bbox" : "boundingbox"; // test name.toLowerCase()
    String bbox = wcsQueryMap.get(bboxName);
    EDVGridAxis lonEdv = axisVariables[lonIndex];
    EDVGridAxis latEdv = axisVariables[latIndex];
    EDVGridAxis altDepthEdv =
        altIndex >= 0
            ? axisVariables[altIndex]
            : depthIndex >= 0 ? axisVariables[depthIndex] : null;
    EDVGridAxis timeEdv = timeIndex < 0 ? null : axisVariables[timeIndex];
    String minLon = lonEdv.destinationMinString();
    String maxLon = lonEdv.destinationMaxString();
    String minLat = latEdv.destinationMinString();
    String maxLat = latEdv.destinationMaxString();
    String minAlt =
        altIndex >= 0
            ? altDepthEdv.destinationMaxString()
            : depthIndex >= 0 ? "-" + altDepthEdv.destinationMinString() : null;
    if (minAlt != null && minAlt.startsWith("--")) minAlt = minAlt.substring(2);
    String maxAlt = minAlt;
    if (bbox != null) {
      String bboxSA[] = String2.split(bbox, ',');
      if (bboxSA.length < 4)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                + bboxName
                + " must have at least 4 comma-separated values.");
      minLon = bboxSA[0]; // note goofy ordering of options
      maxLon = bboxSA[2];
      minLat = bboxSA[1];
      maxLat = bboxSA[3];
      if (version100 && (altIndex >= 0 || depthIndex >= 0) && bboxSA.length >= 6) {
        minAlt = bboxSA[4];
        maxAlt = bboxSA[5];
      }
      // ??? if (!version100 && bboxSA.length > 4) ...
    }
    double minLonD = String2.parseDouble(minLon);
    double maxLonD = String2.parseDouble(maxLon);
    if (Double.isNaN(minLonD) || Double.isNaN(maxLonD) || minLonD > maxLonD)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + bboxName
              + " minLongitude="
              + minLonD
              + " must be <= maxLongitude="
              + maxLonD
              + ".");
    double minLatD = String2.parseDouble(minLat);
    double maxLatD = String2.parseDouble(maxLat);
    if (Double.isNaN(minLatD) || Double.isNaN(maxLatD) || minLatD > maxLatD)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + bboxName
              + " minLatitude="
              + minLatD
              + " must be <= maxLatitude="
              + maxLatD
              + ".");
    double minAltD = String2.parseDouble(minAlt);
    double maxAltD = String2.parseDouble(maxAlt);
    if ((altIndex >= 0 || depthIndex >= 0)
        && (Double.isNaN(minAltD) || Double.isNaN(maxAltD) || minAltD > maxAltD))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + bboxName
              + " minAltitude="
              + minAltD
              + " must be <= maxAltitude="
              + maxAltD
              + ".");

    // 1.0.0 width/height/depth, resx/y/z
    int lonStride = 1;
    int latStride = 1;
    int altStride = 1;
    if (version100) {

      // lonStride
      String n = wcsQueryMap.get("width"); // test name.toLowerCase()
      String res = wcsQueryMap.get("resx"); // test name.toLowerCase()
      int start = lonEdv.destinationToClosestIndex(minLonD);
      int stop = lonEdv.destinationToClosestIndex(maxLonD);
      if (start > stop) { // because !isAscending
        int ti = start;
        start = stop;
        stop = ti;
      }
      if (n != null) {
        int ni = String2.parseInt(n);
        if (ni == Integer.MAX_VALUE || ni <= 0)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                  + "width="
                  + n
                  + " must be > 0.");
        lonStride = DataHelper.findStride(stop - start + 1, ni);
      } else if (res != null) {
        double resD = String2.parseDouble(res);
        if (Double.isNaN(resD) || resD <= 0)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                  + "resx="
                  + res
                  + " must be > 0.");
        lonStride = Math2.minMax(1, stop - start, Math2.roundToInt((maxLonD - minLonD) / resD));
      }

      // latStride
      n = wcsQueryMap.get("height"); // test name.toLowerCase()
      res = wcsQueryMap.get("resy"); // test name.toLowerCase()
      start = latEdv.destinationToClosestIndex(minLatD);
      stop = latEdv.destinationToClosestIndex(maxLatD);
      if (start > stop) { // because !isAscending
        int ti = start;
        start = stop;
        stop = ti;
      }
      if (n != null) {
        int ni = String2.parseInt(n);
        if (ni == Integer.MAX_VALUE || ni <= 0)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                  + "height="
                  + n
                  + " must be > 0.");
        latStride = DataHelper.findStride(stop - start + 1, ni);
        // String2.log("start=" + start + " stop=" + stop + " ni=" + ni + " latStride=" +
        // latStride);
      } else if (res != null) {
        double resD = String2.parseDouble(res);
        if (Double.isNaN(resD) || resD <= 0)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                  + "resy="
                  + res
                  + " must be > 0.");
        latStride = Math2.minMax(1, stop - start, Math2.roundToInt((maxLatD - minLatD) / resD));
      }

      // altStride
      if (altIndex >= 0 || depthIndex >= 0) {
        n = wcsQueryMap.get("depth"); // test name.toLowerCase()
        res = wcsQueryMap.get("resz"); // test name.toLowerCase()
        start = altDepthEdv.destinationToClosestIndex(minAltD);
        stop = altDepthEdv.destinationToClosestIndex(maxAltD);
        if (start > stop) { // because !isAscending
          int ti = start;
          start = stop;
          stop = ti;
        }
        if (n != null) {
          int ni = String2.parseInt(n);
          if (ni == Integer.MAX_VALUE || ni <= 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "depth="
                    + n
                    + " must be > 0.");
          altStride = DataHelper.findStride(stop - start + 1, ni);
        } else if (res != null) {
          double resD = String2.parseDouble(res);
          if (Double.isNaN(resD) || resD <= 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "resz="
                    + res
                    + " must be > 0.");
          altStride = Math2.minMax(1, stop - start, Math2.roundToInt((maxAltD - minAltD) / resD));
        }
      }
    }

    // build the dapQuery
    // slightly different from standard: if no time or bbox or other axes specified
    //  defaults are used (max lat and lon range, and "last" of all other axes)
    StringBuilder dapQuery = new StringBuilder(coverage);
    for (int av = 0; av < axisVariables.length; av++) {
      // lon
      if (av == lonIndex) {
        if (lonEdv.isAscending())
          dapQuery.append("[(" + minLon + "):" + lonStride + ":(" + maxLon + ")]");
        else dapQuery.append("[(" + maxLon + "):" + lonStride + ":(" + minLon + ")]");

        // lat
      } else if (av == latIndex) {
        if (latEdv.isAscending())
          dapQuery.append("[(" + minLat + "):" + latStride + ":(" + maxLat + ")]");
        else dapQuery.append("[(" + maxLat + "):" + latStride + ":(" + minLat + ")]");

        // alt
      } else if (av == altIndex) {
        if (altDepthEdv.isAscending())
          dapQuery.append("[(" + minAlt + "):" + altStride + ":(" + maxAlt + ")]");
        else dapQuery.append("[(" + maxAlt + "):" + altStride + ":(" + minAlt + ")]");

        // depth
      } else if (av == depthIndex) {
        if (altDepthEdv.isAscending())
          dapQuery.append("[(" + minAlt + "):" + altStride + ":(" + maxAlt + ")]");
        else dapQuery.append("[(" + maxAlt + "):" + altStride + ":(" + minAlt + ")]");

        // time
      } else if (av == timeIndex) {
        String paramName = version100 ? "time" : "timesequence";
        String time = wcsQueryMap.get(paramName); // test name.toLowerCase()
        if (time == null) {
          dapQuery.append("[(last)]"); // default
        } else {
          if (time.indexOf(',') >= 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "comma-separated lists of "
                    + paramName
                    + "s are not supported.");
          String timeSA[] = String2.split(time, '/');
          // 'now', see 1.0.0 section 9.2.2.8
          for (int ti = 0; ti < timeSA.length; ti++) {
            if (timeSA[ti].equalsIgnoreCase("now")) timeSA[ti] = "last";
          }
          if (timeSA.length == 0 || timeSA[0].length() == 0) {
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "invalid "
                    + paramName
                    + "=\"\".");
          } else if (timeSA.length == 1) {
            dapQuery.append("[(" + timeSA[0] + ")]");
          } else if (timeSA.length == 2) {
            if (timeEdv.isAscending()) dapQuery.append("[(" + timeSA[0] + "):(" + timeSA[1] + ")]");
            else dapQuery.append("[(" + timeSA[1] + "):(" + timeSA[0] + ")]");
          } else {
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + paramName
                    + " resolution values are not supported.");
          }
        }

        // all other axes
      } else {
        EDVGridAxis edv = axisVariables[av];
        String dName = edv.destinationName();
        String paramName = version100 ? dName : "rangesubset"; // test name.toLowerCase()
        // ???support for rangesubset below needs help
        String val = wcsQueryMap.get(paramName);
        if (val == null) {
          dapQuery.append("[(last)]"); // default
        } else {
          if (val.indexOf(',') >= 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "comma-separated lists of "
                    + dName
                    + "'s are not supported.");
          String valSA[] = String2.split(val, '/');
          if (valSA.length == 0 || valSA[0].length() == 0) {
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "invalid "
                    + paramName
                    + "=\"\".");
          } else if (valSA.length == 1) {
            dapQuery.append("[(" + valSA[0] + ")]");
          } else if (valSA.length == 2) {
            if (edv.isAscending()) dapQuery.append("[(" + valSA[0] + "):(" + valSA[1] + ")]");
            else dapQuery.append("[(" + valSA[1] + "):(" + valSA[2] + ")]");
          } else if (valSA.length == 3) {
            double minD = String2.parseDouble(valSA[0]);
            double maxD = String2.parseDouble(valSA[1]);
            double resD = String2.parseDouble(valSA[2]);
            if (Double.isNaN(minD) || Double.isNaN(maxD) || minD > maxD)
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                      + dName
                      + " min="
                      + valSA[0]
                      + " must be <= max="
                      + valSA[1]
                      + ".");
            int start = edv.destinationToClosestIndex(minD);
            int stop = edv.destinationToClosestIndex(maxD);
            if (start < stop) { // because !isAscending
              int ti = start;
              start = stop;
              stop = ti;
            }
            if (Double.isNaN(resD) || resD <= 0)
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                      + dName
                      + " res="
                      + valSA[2]
                      + " must be > 0.");
            int stride = Math2.minMax(1, stop - start, Math2.roundToInt((maxD - minD) / resD));
            if (edv.isAscending())
              dapQuery.append("[(" + valSA[0] + "):" + stride + ":(" + valSA[1] + ")]");
            else dapQuery.append("[(" + valSA[1] + "):" + stride + ":(" + valSA[0] + ")]");
          } else {
            throw new SimpleException(
                EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                    + "number="
                    + valSA.length
                    + " of values for "
                    + dName
                    + " must be <= 3.");
          }
        }
      }
    }

    if (reallyVerbose)
      String2.log(
          "wcsQueryToDapQuery="
              + dapQuery
              + "\n  version="
              + version
              + " format="
              + responseFormat);
    return new String[] {dapQuery.toString(), responseFormat};
  }

  /**
   * This writes the /wcs/[datasetID]/index.html page to the writer, starting with youAreHere. <br>
   * Currently, this just works as a WCS 1.0.0 server. <br>
   * The caller should have already checked loggedInAs and accessibleViaWCS().
   *
   * @param request the servlet request
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). This doesn't check
   *     if eddGrid is accessible to loggedInAs. The caller should do that.
   * @param writer afterwards, the writer is flushed, not closed
   * @throws Throwable if trouble (there shouldn't be)
   */
  public void wcsDatasetHtml(
      HttpServletRequest request, int language, String loggedInAs, Writer writer) throws Throwable {

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);

    String wcsUrl = tErddapUrl + "/wcs/" + datasetID + "/" + wcsServer;
    String destName0 = dataVariables[0].destinationName();
    EDVGridAxis lonEdv = axisVariables[lonIndex];
    EDVGridAxis latEdv = axisVariables[latIndex];
    String getCap =
        XML.encodeAsHTMLAttribute(wcsUrl + "?service=WCS&version=1.0.0&request=GetCapabilities");
    String desCov =
        XML.encodeAsHTMLAttribute(
            wcsUrl + "?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=" + destName0);
    StringBuilder getCovSB = new StringBuilder(wcsUrl);
    getCovSB.append(
        "?service=WCS&version=1.0.0&request=GetCoverage&coverage="
            + destName0
            + "&bbox="
            + lonEdv.destinationMinString()
            + ","
            + latEdv.destinationMinString()
            + ","
            + lonEdv.destinationMaxString()
            + ","
            + latEdv.destinationMaxString());
    if (altIndex >= 0)
      getCovSB.append(
          ","
              + axisVariables[altIndex].destinationMaxString()
              + // max to max
              ","
              + axisVariables[altIndex].destinationMaxString());
    if (timeIndex >= 0) getCovSB.append("&time=" + axisVariables[timeIndex].destinationMaxString());
    for (int av = 0; av < axisVariables.length; av++) {
      if (av == lonIndex || av == latIndex || av == altIndex || av == timeIndex) continue;
      EDVGridAxis edvga = axisVariables[av];
      getCovSB.append(
          "&" + edvga.destinationName() + "=" + axisVariables[av].destinationMaxString());
    }
    // make height=200 and width proportional
    int width =
        Math.min(
            2000,
            Math2.roundToInt(
                ((lonEdv.destinationMaxDouble() - lonEdv.destinationMinDouble()) * 200)
                    / (latEdv.destinationMaxDouble() - latEdv.destinationMinDouble())));
    getCovSB.append("&height=200&width=" + width + "&format=PNG");
    String getCov = XML.encodeAsHTMLAttribute(getCovSB.toString());

    // *** html head
    // writer.write(EDStatic.startHeadHtml(language, tErddapUrl, title() + " - WCS"));
    // writer.write("\n" + rssHeadLink());
    // writer.write("</head>\n");
    // writer.write(EDStatic.startBodyHtml(language, loggedInAs,
    //    "wcs/" + datasetID + "/index.html", //was endOfRequest,
    //    queryString) + "\n");
    // writer.write(HtmlWidgets.htmlTooltipScript(EDStatic.imageDirUrl(request, loggedInAs,
    // language)));
    // writer.flush(); //Steve Souder says: the sooner you can send some html to user, the better

    // *** html body content
    writer.write("<div class=\"standard_width\">\n");
    writer.write(
        EDStatic.youAreHere(
            request,
            language,
            loggedInAs,
            "wcs",
            datasetID)); // wcs must be lowercase for link to work
    writeHtmlDatasetInfo(request, language, loggedInAs, writer, true, true, true, true, "", "");

    String datasetListRef =
        "<br>See the\n"
            + "  <a rel=\"bookmark\" href=\""
            + tErddapUrl
            + "/wcs/index.html\">list \n"
            + "    of datasets available via WCS</a> at this ERDDAP installation.\n";

    // What is WCS?   (for tDatasetID)
    // !!!see the almost identical documentation above
    writer.write(
        "<h2><a class=\"selfLink\" id=\"description\" href=\"#description\" rel=\"bookmark\">What</a> is WCS?</h2>\n"
            + String2.replaceAll(
                EDStatic.messages.wcsLongDescriptionHtmlAr[language], "&erddapUrl;", tErddapUrl)
            + "\n"
            + datasetListRef
            + "\n"
            + "<h2>Sample WCS Requests for this Dataset</h2>\n"
            + "WCS requests are specially formed URLs with queries. You can create these URLs yourself.\n"
            + "<br>Or, if you use WCS client software, the software will create the URLs and process the results for you.\n"
            + "<br>There are three types of WCS requests:\n"
            + "<ul>\n"
            + "<li><strong>GetCapabilities</strong> - A GetCapabilities request returns an XML document which provides\n"
            + "  <br>background information about the service and specific information about all of the data\n"
            + "  <br>available from this service. For this dataset, use\n"
            + "  <br><a href=\""
            + getCap
            + "\">\n"
            + getCap
            + "</a>\n"
            + "  <br>&nbsp;\n"
            + "<li><strong>DescribeCoverage</strong> - A DescribeCoverage request returns an XML document which provides\n"
            + "  <br>more detailed information about a specific coverage. For example,\n"
            + "  <br><a href=\""
            + desCov
            + "\"><kbd>"
            + desCov
            + "</kbd></a>\n"
            + "  <br>&nbsp;\n"
            + "<li><strong>GetCoverage</strong> - A GetCoverage request specifies the subset of data that you want:\n"
            + "    <ul>\n"
            + "    <li>The coverage name (e.g., sst).\n"
            + "    <li>The bounding box (<i>minLon,minLat,maxLon,maxLat</i>).\n"
            + "    <li>The time range.\n"
            + "    <li>The width and height, or x resolution and y resolution.\n"
            + "    <li>The file format (e.g., "
            + String2.toCSSVString(wcsRequestFormats100)
            + ").\n"
            + "    </ul>\n"
            + "  <br>The WCS service responds with a file with the requested data. A PNG example is\n"
            + "  <br><a href=\""
            + getCov
            + "\">\n"
            + getCov
            + "</a>\n"
            + "  <br>&nbsp;\n"
            + "</ul>\n");

    // client software
    // writer.write(
    //    "<h2><a class=\"selfLink\" id=\"clientSoftware\" href=\"#clientSoftware\"
    // rel=\"bookmark\">Client Software</a></h2>" +
    //    "WCS can be used directly by humans using a browser.\n" +
    //    "<br>Some of the information you need to write such software is below.\n" +
    //    "<br>For additional information, please see the\n" +
    //    "  <a rel=\"help\" href=\"https://www.opengeospatial.org/standards/wcs\">WCS standard
    // documentation" +
    //            EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>.\n" +
    //    "\n");

    wcsRequestDocumentation(language, tErddapUrl, writer, getCap, desCov, getCov);

    /*
        https://www.esri.com/software/arcgis/\">ArcGIS</a>,\n" +
        "    <a rel=\"bookmark\" href=\"http://mapserver.refractions.net/phpwms/phpwms-cvs/\">Refractions PHP WMS Client" +
                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>, and\n" +
        "    <a rel=\"bookmark\" href=\"http://udig.refractions.net//\">uDig" +
                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>. \n" +
        "  <br>To make a client work, you would install the software on your computer.\n" +
        "  <br>Then, you would enter the URL of the WMS service into the client.\n" +
        "  <br>For example, in ArcGIS (not yet fully working because it doesn't handle time!), use\n" +
        "  <br>\"Arc Catalog : Add Service : Arc Catalog Servers Folder : GIS Servers : Add WMS Server\".\n" +
        "  <br>In ERDDAP, this dataset has its own WMS service, which is located at\n" +
        "  <br>&nbsp; &nbsp; <strong>" + tErddapUrl + "/wms/" + tDatasetID + "/" + WMS_SERVER + "?</strong>\n" +
        "  <br>(Some WMS client programs don't want the <strong>?</strong> at the end of that URL.)\n" +
        datasetListRef +
    writer.write(
        "  <p><strong>In practice,</strong> we haven't found any WMS clients that properly handle dimensions\n" +
        "  <br>other than longitude and latitude (e.g., time), a feature which is specified by the WMS\n" +
        "  <br>specification and which is utilized by most datasets in ERDDAP's WMS servers.\n" +
        "  <br>You may find that using\n" +
        makeAGraphRef + "\n" +
        "    and selecting the .kml file type (an OGC standard)\n" +
        "  <br>to load images into <a rel=\"bookmark\" href=\"https://www.google.com/earth/\">Google Earth" +
                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a> provides\n" +
        "     a good (non-WMS) map client.\n" +
        makeAGraphListRef +
        "  <br>&nbsp;\n" +
        "<li> <strong>Web page authors can embed a WMS client in a web page.</strong>\n" +
        "  <br>For the map above, ERDDAP is using \n" +
        "    <a rel=\"bookmark\" href=\"https://leafletjs.com\">Leaflet" +
                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>, \n" +
        "    which is a very versatile WMS client.\n" +
        "  <br>Leaflet doesn't automatically deal with dimensions\n" +
        "    other than longitude and latitude (e.g., time),\n" +
        "  <br>so you will have to write JavaScript (or other scripting code) to do that.\n" +
        "  <br>(Adventurous JavaScript programmers can look at the Souce Code for this web page.)\n" +
        "  <br>&nbsp;\n" +
        "<li> <strong>A person with a browser or a computer program can generate special GetMap URLs\n" +
        "  and view/use the resulting image file.</strong>\n" +
        "  <br><strong>Opaque example:</strong> <a href=\"" + tWmsOpaqueExample + "\">" +
                                                    tWmsOpaqueExample + "</a>\n" +
        "  <br><strong>Transparent example:</strong> <a href=\"" + tWmsTransparentExample + "\">" +
                                                         tWmsTransparentExample + "</a>\n" +
        datasetListRef +
        "  <br><strong>For more information, see ERDDAP's \n" +
        "    <a rel=\"help\" href=\"" +tErddapUrl + "/wms/documentation.html\">WMS Documentation</a> .</strong>\n" +
        "  <p><strong>In practice, it is probably easier and more versatile to use this dataset's\n" +
        "    " + makeAGraphRef + " form</strong>\n" +
        "  <br>than to use WMS for this purpose.\n" +
        makeAGraphListRef +
        "</ol>\n" +
        "\n");
        */

    writer.write("</div>\n");
    // writer.write(EDStatic.endBodyHtml(language, tErddapUrl, loggedInAs));
    // writer.write("\n</html>\n");
    writer.flush();
  }

  /**
   * This writes the html with detailed info about WCS queries.
   *
   * @param language the index of the selected language
   * @param tErddapUrl
   * @param writer
   * @param getCapabilities a sample URL.
   * @param describeCoverage a sample URL.
   * @param getCoverage a sample URL.
   */
  public void wcsRequestDocumentation(
      int language,
      String tErddapUrl,
      Writer writer,
      String getCapabilities,
      String describeCoverage,
      String getCoverage)
      throws Throwable {
    // GetCapabilities
    writer.write(
        "<h2><a class=\"selfLink\" id=\"request\" href=\"#request\" rel=\"bookmark\">WCS</a> Requests - Detailed Description</h2>\n"
            + "WCS requests are specially formed URLs with queries.\n"
            + "You can create these URLs yourself.\n"
            + "<br>Or, if you use WCS client software, the software will create the URLs and process the results for you.\n"
            + "<br>There are three types of WCS requests: GetCapabilities, DescribeCoverage, GetCoverage.\n"
            + "<br>For detailed information, please see the\n"
            + "  <a rel=\"help\" href=\"https://www.opengeospatial.org/standards/wcs\">WCS standard documentation"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "\n"
            + "<p><strong>GetCapabilities</strong> - A GetCapabilities request returns an XML document which provides\n"
            + "  <br>background information about the service and basic information about all of the data\n"
            + "  <br>available from this service.  For this dataset, use\n"
            + "  <br><a href=\""
            + XML.encodeAsHTMLAttribute(getCapabilities)
            + "\">\n"
            + XML.encodeAsHTMLAttribute(getCapabilities)
            + "</a>\n"
            + "  <p>The parameters for a GetCapabilities request are:\n"
            + "<table class=\"erd commonBGColor nowrap\" style=\"border-spacing:4px;\">\n"
            + "  <tr>\n"
            + "    <th><i>name=value</i><sup>*</sup></th>\n"
            + "    <th>Description</th>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>service=WCS</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>version="
            + wcsVersion
            + "</td>\n"
            + "    <td>The only valid value is "
            + wcsVersion
            + " . This parameter is optional.\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>request=GetCapabilities</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  </table>\n"
            + "  <sup>*</sup> Parameter names are case-insensitive.\n"
            + "  <br>Parameter values are case sensitive and must be\n"
            + "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>:\n"
            + "   <br>all characters in query values (the parts after the '=' signs) other than A-Za-z0-9_-!.~'()* must be\n"
            + "   <br>encoded as %HH, where HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n"
            + "   <br>Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n"
            + "   <br>(ask a programmer for help). There are\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  <br>The parameters may be in any order in the URL, separated by '&amp;' .\n"
            + "  <br>&nbsp;\n"
            + "\n");

    // DescribeCoverage
    // "?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=" + destName0;
    writer.write(
        "<p><strong>DescribeSensor</strong> - A DescribeSensor request returns an XML document which provides\n"
            + "  <br>more detailed information about a specific coverage. For example,\n"
            + "  <br><a href=\""
            + XML.encodeAsHTMLAttribute(describeCoverage)
            + "\">\n"
            + XML.encodeAsHTMLAttribute(describeCoverage)
            + "</a>\n"
            + "\n"
            + "  <p>The parameters for a DescribeCoverage request are:\n"
            + "<table class=\"erd commonBGColor nowrap\" style=\"border-spacing:4px;\">\n"
            + "  <tr>\n"
            + "    <th><i>name=value</i><sup>*</sup></th>\n"
            + "    <th>Description</th>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>service=WCS</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>version="
            + wcsVersion
            + "</td>\n"
            + "    <td>The only valid value is "
            + wcsVersion
            + " .  Required.\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>request=DescribeCoverage</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>coverage=<i>coverage</i></td>\n"
            + "    <td>A comma-separated list of one or more coverage names \n"
            + "      <br>from the list in the GetCapabilities response.\n"
            + "      <br>Required.</td>\n"
            + "  </tr>\n"
            + "  </table>\n"
            + "  <sup>*</sup> Parameter names are case-insensitive.\n"
            + "  <br>Parameter values are case sensitive and must be\n"
            + "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>:\n"
            + "   <br>all characters in query values (the parts after the '=' signs) other than A-Za-z0-9_-!.~'()* must be\n"
            + "   <br>encoded as %HH, where HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n"
            + "   <br>Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n"
            + "   <br>(ask a programmer for help). There are\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  <br>The parameters may be in any order in the URL, separated by '&amp;' .\n"
            + "  <br>&nbsp;\n"
            + "\n");

    // GetCoverage
    writer.write(
        "<p><strong>GetCoverage</strong> - A GetCoverage request specifies the subset of data that you want.\n"
            + "  The WCS service responds with a file with the requested data. A PNG example is"
            + "  <br><a href=\""
            + XML.encodeAsHTMLAttribute(getCoverage)
            + "\">\n"
            + XML.encodeAsHTMLAttribute(getCoverage)
            + "</a>\n"
            + "  <p>The parameters for a GetCoverage request are:\n"
            + "<table class=\"erd commonBGColor nowrap\" style=\"border-spacing:4px;\">\n"
            + "  <tr>\n"
            + "    <th><i>name=value</i><sup>*</sup></th>\n"
            + "    <th>Description</th>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>service=WCS</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>version="
            + wcsVersion
            + "</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>request=GetCoverage</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>coverage=<i>coverage</i></td>\n"
            + "    <td>The name of a coverage from the list in GetCapabilities. Required.</td>\n"
            + "  </tr>\n"
            +
            // change this behavior???
            "  <tr>\n"
            + "    <td>crs=<i>crs</i></td>\n"
            + "    <td>This parameter is required by the WCS standard.  ERDDAP ignores this.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>response_crs=<i>response_crs</i></td>\n"
            + "    <td>This parameter is optional in the WCS standard.  ERDDAP ignores this.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>bbox=<i>minLon,minLat,maxLon,maxLat</i></td>\n"
            + "    <td>BBOX allows you to specify a longitude, latitude bounding box constraint.\n"
            + "      <br>The WCS standard requires at least one BBOX or TIME.\n"
            + "      <br>In ERDDAP, this parameter is optional and the default is always the full longitude, latitude range.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>time=<i>time</i>\n"
            + "      <br>time=<i>beginTime/endTime</i></td>\n"
            + "    <td>The time values must be in\n"
            + "      <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" format"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "      for example, <span style=\"white-space:nowrap;\">\"1985-01-02T00:00:00Z\").</span>\n"
            + "      <br>In ERDDAP, any time value specified rounds to the nearest available time.\n"
            + "      <br>Or, in the WCS standard and ERDDAP, you can use \"now\" to get the last available time.\n"
            + "      <br>The WCS standard requires at least one BBOX or TIME.\n"
            + "      <br>In ERDDAP, this parameter is optional and the default is always the last time available.\n"
            + "      <br>The WCS standard allows <i>time=beginTime,endTime,timeRes</i>.  ERDDAP doesn't allow this.\n"
            + "      <br>The WCS standard allows <i>time=time1,time2,...</i>  ERDDAP doesn't allow this.</td>\n"
            + (EDStatic.config.convertersActive
                ? "      <br>ERDDAP has a utility to\n"
                    + "        <a rel=\"bookmark\" href=\""
                    + tErddapUrl
                    + "/convert/time.html\">Convert\n"
                    + "        a Numeric Time to/from a String Time</a>.\n"
                    + "      <br>See also:\n"
                    + "        <a rel=\"help\" href=\""
                    + tErddapUrl
                    + "/convert/time.html#erddap\">How\n"
                    + "        ERDDAP Deals with Time</a>.\n"
                : "")
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td><i>parameter=value</i>\n"
            + "      <br><i>parameter=minValue/maxValue</i>\n"
            + "      <br><i>parameter=minValue/maxValue/resolution</i></td>\n"
            + "    <td>This allows you to specify values for each axis (the 'parameter') other than longitude, latitude, or time.\n"
            + "      <br>'resolution' is an optional average spacing between values (specified in axis units).\n"
            + "      <br>If 'resolution' is omitted, ERDDAP returns every value in the minValue/maxValue range.\n"
            + "      <br>The WCS standard requires this if there is no default value.\n"
            + "      <br>In ERDDAP, this parameter is optional because the default is always the last value available.\n"
            + "      <br>The WCS standard allows <i>parameter=value1,value2,...</i>  ERDDAP doesn't allow this.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>width=<i>width</i>\n"
            + "      <br>height=<i>height</i></td>\n"
            + "    <td>Requests a grid of the specified width (longitude values) or height (latitude values).\n"
            + "      <br>The WCS standard requires these or resx and resy.\n"
            + "      <br>In ERDDAP, these are optional because the default is the full width and height of the grid.\n"
            +
            // true???
            "      <br>The WCS standard presumably returns a grid of exactly the requested size.\n"
            + "      <br>In ERDDAP, the grid returned may be slightly larger than requested, because the longitude\n"
            + "      <br>and latitude values will be evenly spaced (in index space) and ERDDAP doesn't interpolate.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>resx=<i>resx</i>\n"
            + "      <br>resy=<i>resy</i></td>\n"
            + "    <td>Requests a grid where the x (longitude) values and y (latitude) values have the specified spacing.\n"
            + "      <br>The WCS standard requires these or width and height.\n"
            + "      <br>In ERDDAP, these are optional because the default is full resolution.\n"
            +
            // true???
            "      <br>The WCS standard presumably returns a grid of exactly the requested size.\n"
            + "      <br>In ERDDAP, the grid returned may be slightly larger than requested, because the longitude\n"
            + "      <br>and latitude values will be evenly spaced (in index space) and ERDDAP doesn't interpolate.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>interpolation=<i>interpolation</i></td>\n"
            + "    <td>This parameter is optional in the WCS standard.  ERDDAP does not allow or support this.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>format=<i>format</i></td>\n"
            + "    <td>In ERDDAP, this can be any one of several response formats:\n"
            + "      <br>");
    for (int f = 0; f < wcsRequestFormats100.size(); f++) {
      if (f > 0) writer.write(", ");
      writer.write("\"" + SSR.minimalPercentEncode(wcsRequestFormats100.get(f)) + "\"");
    }
    writer.write(
        ".\n"
            + "      <br>\"GeoTIFF\" and \"PNG\" only work if the request is for more than one longitude\n"
            + "      <br>and latitude value, and if the request is for just one value for all other axes.\n"
            + "      <br>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>exceptions="
            + wcsExceptions
            + "</td>\n"
            + "    <td>There is only one valid value.  This parameter is optional.</td>\n"
            + "  </tr>\n"
            + "  </table>\n"
            + "  <sup>*</sup> Parameter names are case-insensitive.\n"
            + "  <br>Parameter values are case sensitive and must be\n"
            + "    <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>:\n"
            + "   <br>all characters in query values (the parts after the '=' signs) other than A-Za-z0-9_-!.~'()* must be\n"
            + "   <br>encoded as %HH, where HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n"
            + "   <br>Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n"
            + "   <br>(ask a programmer for help). There are\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  <br>The parameters may be in any order in the URL, separated by '&amp;' .\n"
            + "  <br>&nbsp;\n"
            + "\n");
  }

  /**
   * This writes the dataset's FGDC-STD-001-1998
   * https://www.fgdc.gov/standards/projects/FGDC-standards-projects/metadata/base-metadata/index_html
   * and includes the NASA extensions for remotely sensed data FGDC-STD-012-2002 "Content Standard
   * for Digital Geospatial Metadata: Extensions for Remote Sensing Metadata" XML to the writer.
   * <br>
   * The template is initially based on a sample file from Dave Neufeld: maybe <br>
   * https://docs.unidata.ucar.edu/netcdf-java/current/userguide/ncml_overview.html <br>
   * (stored on Bob's computer as F:/programs/fgdc/258Neufeld20110830.xml). <br>
   * Made pretty via TestAll: XML.prettyXml(in, out);
   *
   * <p>This is usually just called by the dataset's constructor, at the end of
   * EDDTable/Grid.ensureValid.
   *
   * <p>See FGDC documentation at
   * https://www.fgdc.gov/standards/projects/FGDC-standards-projects/metadata/base-metadata/v2_0698.pdf
   * Bob has local copy at f:/programs/fgdc/fgdc-std-001-1998-v2_0698.pdf <br>
   * For missing String values, use "Unknown" (pg viii).
   *
   * <p>The <strong>most</strong> useful descriptive information (but somewhat cumbersome to
   * navigate; use Back button, don't hunt for their back links):
   * https://www.fgdc.gov/csdgmgraphical/index.htm
   *
   * <p>Useful documentation (in the end, I used it very little): FGDC RSE ("Content Standard for
   * Digital Geospatial Metadata: Extensions for Remote Sensing Metadata, FGDC-STD-012-2002") as
   * described in www.ncdc.noaa.gov/oa/metadata/rse-users-guide.doc Bob has a local copy at
   * f:/programs/fgdc/rse-users-guide.doc . This has the additional benefit of producing FGDC
   * suitable for the NOAA Metadata Manager Repository (NMMR). General NCDC metadata information is
   * at https://www.ncdc.noaa.gov/oa/metadata/metadataresources.html#ds The template is at
   * https://www.ncdc.noaa.gov/oa/metadata/standard-rse-template.xml
   *
   * <p>FGDC Metadata validator [was https://www.maine.edu/geolib/fgdc_metadata_validator.html ]
   *
   * <p>If getAccessibleTo()==null, the fgdc refers to http: ERDDAP links; otherwise, it refers to
   * https: ERDDAP links.
   *
   * @param language the index of the selected language
   * @param writer a UTF-8 writer
   * @throws Throwable if trouble (e.g., no latitude and longitude axis)
   */
  @Override
  protected void writeFGDC(int language, Writer writer) throws Throwable {
    // FUTURE: support datasets with x,y (and not longitude,latitude)

    // requirements
    if (lonIndex < 0 || latIndex < 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
              + EDStatic.messages.noXxxNoLLAr[language]);

    String tErddapUrl = EDStatic.preferredErddapUrl;
    String datasetUrl = tErddapUrl + "/" + dapProtocol + "/" + datasetID();
    String wcsUrl = tErddapUrl + "/wcs/" + datasetID() + "/" + wcsServer; // "?" at end?
    String wmsUrl = tErddapUrl + "/wms/" + datasetID() + "/" + WMS_SERVER; // "?" at end?
    String domain = EDStatic.config.baseUrl;
    if (domain.startsWith("http://")) domain = domain.substring(7);
    else if (domain.startsWith("https://")) domain = domain.substring(8);
    String eddCreationDate =
        String2.replaceAll(Calendar2.millisToIsoDateString(creationTimeMillis()), "-", "");
    String unknown = "Unknown"; // pg viii of FGDC document

    String acknowledgement = combinedGlobalAttributes.getString("acknowledgement"); // acdd 1.3
    if (acknowledgement == null)
      acknowledgement = combinedGlobalAttributes.getString("acknowledgment"); // acdd 1.0
    String contributorName = combinedGlobalAttributes.getString("contributor_name");
    String contributorEmail = combinedGlobalAttributes.getString("contributor_email");
    String contributorRole = combinedGlobalAttributes.getString("contributor_role");
    String creatorName = combinedGlobalAttributes.getString("creator_name");
    String creatorEmail = combinedGlobalAttributes.getString("creator_email");
    // creatorUrl: use infoUrl
    String dateCreated =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString("date_created")); // "" if trouble
    String dateIssued =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString("date_issued")); // "" if trouble
    if (dateCreated.length() > 10) dateCreated = dateCreated.substring(0, 10);
    if (dateIssued.length() > 10) dateIssued = dateIssued.substring(0, 10);
    if (dateCreated.startsWith("0000")) // year=0000 isn't valid
    dateCreated = "";
    if (dateIssued.startsWith("0000")) dateIssued = "";
    // make compact form  YYYYMMDD
    dateCreated = String2.replaceAll(dateCreated, "-", "");
    dateIssued = String2.replaceAll(dateIssued, "-", "");
    String history = combinedGlobalAttributes.getString("history");
    String infoUrl = combinedGlobalAttributes.getString("infoUrl");
    String institution = combinedGlobalAttributes.getString("institution");
    String keywords = combinedGlobalAttributes.getString("keywords");
    String keywordsVocabulary = combinedGlobalAttributes.getString("keywords_vocabulary");
    if (keywords == null) { // use the crude, ERDDAP keywords
      keywords = EDStatic.config.keywords;
      keywordsVocabulary = null;
    }
    String license = combinedGlobalAttributes.getString("license");
    String project = combinedGlobalAttributes.getString("project");
    if (project == null) project = institution;
    String references = combinedGlobalAttributes.getString("references");
    String satellite = combinedGlobalAttributes.getString("satellite");
    String sensor = combinedGlobalAttributes.getString("sensor");
    String sourceUrl = publicSourceUrl();
    String standardNameVocabulary = combinedGlobalAttributes.getString("standard_name_vocabulary");

    String adminInstitution =
        EDStatic.config.adminInstitution == null ? unknown : EDStatic.config.adminInstitution;
    String adminIndividualName =
        EDStatic.config.adminIndividualName == null ? unknown : EDStatic.config.adminIndividualName;
    String adminPosition =
        EDStatic.config.adminPosition == null ? unknown : EDStatic.config.adminPosition;
    String adminPhone = EDStatic.config.adminPhone == null ? unknown : EDStatic.config.adminPhone;
    String adminAddress =
        EDStatic.config.adminAddress == null ? unknown : EDStatic.config.adminAddress;
    String adminCity = EDStatic.config.adminCity == null ? unknown : EDStatic.config.adminCity;
    String adminStateOrProvince =
        EDStatic.config.adminStateOrProvince == null
            ? unknown
            : EDStatic.config.adminStateOrProvince;
    String adminPostalCode =
        EDStatic.config.adminPostalCode == null ? unknown : EDStatic.config.adminPostalCode;
    String adminCountry =
        EDStatic.config.adminCountry == null ? unknown : EDStatic.config.adminCountry;
    String adminEmail = EDStatic.config.adminEmail == null ? unknown : EDStatic.config.adminEmail;

    // testMinimalMetadata is useful for Bob doing tests of validity of FGDC results
    //  when a dataset has minimal metadata
    boolean testMinimalMetadata = false; // only true when testing. normally false;
    if (acknowledgement == null || testMinimalMetadata) acknowledgement = unknown;
    if (contributorName == null || testMinimalMetadata) contributorName = unknown;
    if (contributorEmail == null || testMinimalMetadata) contributorEmail = unknown;
    if (contributorRole == null || testMinimalMetadata) contributorRole = unknown;
    if (creatorName == null || testMinimalMetadata) creatorName = unknown;
    if (creatorEmail == null || testMinimalMetadata) creatorEmail = unknown;
    if (dateCreated == null || testMinimalMetadata) dateCreated = unknown;
    if (dateIssued == null || testMinimalMetadata) dateIssued = unknown;
    if (history == null || testMinimalMetadata) history = unknown;
    if (infoUrl == null || testMinimalMetadata) infoUrl = unknown;
    if (institution == null || testMinimalMetadata) institution = unknown;
    if (keywords == null || testMinimalMetadata) keywords = unknown;
    if (keywordsVocabulary == null || testMinimalMetadata) keywordsVocabulary = unknown;
    if (license == null || testMinimalMetadata) license = unknown;
    if (project == null || testMinimalMetadata) project = unknown;
    if (references == null || testMinimalMetadata) references = unknown;
    if (satellite == null || testMinimalMetadata) satellite = unknown;
    if (sensor == null || testMinimalMetadata) sensor = unknown;
    if (sourceUrl == null || testMinimalMetadata) sourceUrl = unknown;
    if (standardNameVocabulary == null || testMinimalMetadata) standardNameVocabulary = unknown;

    // notAvailable and ...Edv
    EDVLatGridAxis latEdv = (EDVLatGridAxis) axisVariables[latIndex];
    EDVLonGridAxis lonEdv = (EDVLonGridAxis) axisVariables[lonIndex];
    EDVAltGridAxis altEdv =
        (altIndex < 0 || testMinimalMetadata) ? null : (EDVAltGridAxis) axisVariables[altIndex];
    EDVDepthGridAxis depthEdv =
        (depthIndex < 0 || testMinimalMetadata)
            ? null
            : (EDVDepthGridAxis) axisVariables[depthIndex];
    EDVTimeGridAxis timeEdv =
        (timeIndex < 0 || testMinimalMetadata) ? null : (EDVTimeGridAxis) axisVariables[timeIndex];

    String minTime = ""; // compact ISO date (not time), may be ""
    String maxTime = "";
    if (timeEdv != null) {
      minTime = timeEdv.destinationMinString();
      maxTime = timeEdv.destinationMaxString();
      if (minTime.length() > 10) minTime = minTime.substring(0, 10);
      if (maxTime.length() > 10) maxTime = maxTime.substring(0, 10);
      if (minTime.startsWith("0000")) minTime = ""; // 0000 is invalid in FGDC
      if (maxTime.startsWith("0000")) maxTime = "";
      minTime = String2.replaceAll(minTime, "-", ""); // compact
      maxTime = String2.replaceAll(maxTime, "-", "");
    }

    // standardNames
    StringArray standardNames = new StringArray();
    if (!testMinimalMetadata) {
      for (EDVGridAxis axisVariable : axisVariables) {
        String sn = axisVariable.combinedAttributes().getString("standard_name");
        if (sn != null) standardNames.add(sn);
      }
      for (EDV dataVariable : dataVariables) {
        String sn = dataVariable.combinedAttributes().getString("standard_name");
        if (sn != null) standardNames.add(sn);
      }
    }

    // adminCntinfo
    String adminCntinfo =
        "            <cntinfo>\n"
            + "              <cntorgp>\n"
            + "                <cntorg>"
            + XML.encodeAsXML(adminInstitution)
            + "</cntorg>\n"
            + "                <cntper>"
            + XML.encodeAsXML(adminIndividualName)
            + "</cntper>\n"
            + "              </cntorgp>\n"
            + "              <cntpos>"
            + XML.encodeAsXML(adminPosition)
            + "</cntpos>\n"
            + "              <cntaddr>\n"
            + "                <addrtype>Mailing and Physical Address</addrtype>\n"
            + "                <address>"
            + XML.encodeAsXML(adminAddress)
            + "</address>\n"
            + "                <city>"
            + XML.encodeAsXML(adminCity)
            + "</city>\n"
            + "                <state>"
            + XML.encodeAsXML(adminStateOrProvince)
            + "</state>\n"
            + "                <postal>"
            + XML.encodeAsXML(adminPostalCode)
            + "</postal>\n"
            + "                <country>"
            + XML.encodeAsXML(adminCountry)
            + "</country>\n"
            + "              </cntaddr>\n"
            + "              <cntvoice>"
            + XML.encodeAsXML(adminPhone)
            + "</cntvoice>\n"
            +
            // "              <cntfax>" + unknown + "</cntfax>\n" +
            "              <cntemail>"
            + XML.encodeAsXML(adminEmail)
            + "</cntemail>\n"
            +
            // "              <hours>" + unknown + "</hours>\n" +
            "            </cntinfo>\n";

    // start writing xml
    writer.write( // FGDC
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<metadata xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"http://www.ngdc.noaa.gov/metadata/published/xsd/ngdcSchema/schema.xsd\" "
            + // INVALID!!!
            // http://lab.usgin.org/groups/etl-debug-blog/fgdc-xml-schema-woes
            // talks about instead using
            //  http://fgdcxml.sourceforge.net/schema/fgdc-std-012-2002/fgdc-std-012-2002.xsd
            ">\n"
            + "  <idinfo>\n"
            + "    <datsetid>"
            + XML.encodeAsXML(domain + ":" + datasetID())
            + "</datsetid>\n"
            + "    <citation>\n"
            + "      <citeinfo>\n"
            +

            // origin: from project, creator_email, creator_name, infoUrl, institution
            "        <origin>\n"
            + XML.encodeAsXML(
                (unknown.equals(project) ? "" : "Project: " + project + "\n")
                    + (unknown.equals(creatorName) ? "" : "Name: " + creatorName + "\n")
                    + (unknown.equals(creatorEmail) ? "" : "Email: " + creatorEmail + "\n")
                    + "Institution: "
                    + institution
                    + "\n"
                    + // always known
                    (unknown.equals(infoUrl) ? "" : "InfoURL: " + infoUrl + "\n")
                    + (unknown.equals(sourceUrl) ? "" : "Source URL: " + sourceUrl + "\n"))
            + "        </origin>\n"
            + "        <origin_cntinfo>\n"
            + // ngdc added?
            "          <cntinfo>\n"
            + "            <cntorgp>\n"
            + "              <cntorg>"
            + XML.encodeAsXML(institution)
            + "</cntorg>\n"
            + "              <cntper>"
            + XML.encodeAsXML(creatorName)
            + "</cntper>\n"
            + "            </cntorgp>\n"
            +
            // "            <cntaddr>\n" + //not required and ERDDAP doesn't have the info
            // "              <addrtype>Mailing and Physical Address</addrtype>\n" +
            // "              <address>" + unknown + "</address>\n" +
            // "              <city>" + unknown + "</city>\n" +
            // "              <state>" + unknown + "</state>\n" +
            // "              <postal>" + unknown + "</postal>\n" +
            // "              <country>" + unknown + "</country>\n" +
            // "            </cntaddr>\n" +
            // "            <cntvoice>" + unknown + "</cntvoice>\n" +
            // "            <cntfax>" + unknown + "</cntfax>\n" +
            "            <cntemail>"
            + XML.encodeAsXML(creatorEmail)
            + "</cntemail>\n"
            +
            // "            <hours>" + unknown + "</hours>\n" +
            "          </cntinfo>\n"
            + "        </origin_cntinfo>\n"
            + "        <pubdate>"
            + XML.encodeAsXML(unknown.equals(dateIssued) ? eddCreationDate : dateIssued)
            + "</pubdate>\n"
            + "        <title>"
            + XML.encodeAsXML(title)
            + "</title>\n"
            + "        <edition>"
            + unknown
            + "</edition>\n"
            +
            // geoform vocabulary
            // https://www.fgdc.gov/csdgmgraphical/ideninfo/citat/citinfo/type.htm
            "        <geoform>raster digital data</geoform>\n"
            +
            // "        <serinfo>\n" +  ERDDAP doesn't have serial info
            // "          <sername>NOAA Tsunami Inundation DEMs</sername>\n" +
            // "          <issue>Adak, Alaska</issue>\n" +
            // "        </serinfo>\n" +

            // publisher is ERDDAP,  use admin... information
            "        <pubinfo>\n"
            + "          <pubplace>"
            + XML.encodeAsXML(adminCity + ", " + adminStateOrProvince + ", " + adminCountry)
            + "</pubplace>\n"
            + "          <publish>"
            + XML.encodeAsXML(
                "ERDDAP, version " + EDStatic.erddapVersion + ", at " + adminInstitution)
            + "</publish>\n"
            + "          <publish_cntinfo>\n"
            + adminCntinfo
            + "          </publish_cntinfo>\n"
            + "        </pubinfo>\n");

    // online resources   .html, .graph, WMS
    writer.write(
        "        <onlink>"
            + XML.encodeAsXML(datasetUrl + ".html")
            + "</onlink>\n"
            + (accessibleViaMAG.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(datasetUrl + ".graph") + "</onlink>\n")
            + (accessibleViaWCS.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(wcsUrl) + "</onlink>\n")
            + (accessibleViaWMS.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(wmsUrl) + "</onlink>\n"));
    // ngdc extension:
    writer.write(
        "        <CI_OnlineResource>\n"
            + "          <linkage>"
            + XML.encodeAsXML(datasetUrl + ".html")
            + "</linkage>\n"
            + "          <name>Download data: "
            + XML.encodeAsXML(title)
            + "</name>\n"
            + "          <description>A web page for specifying a subset of the dataset and downloading "
            + "data in any of several file formats.</description>\n"
            + "          <function>download data</function>\n"
            + "        </CI_OnlineResource>\n"
            + (accessibleViaMAG.length() > 0
                ? ""
                : "        <CI_OnlineResource>\n"
                    + "          <linkage>"
                    + XML.encodeAsXML(datasetUrl + ".graph")
                    + "</linkage>\n"
                    + "          <name>Make a graph or map: "
                    + XML.encodeAsXML(title)
                    + "</name>\n"
                    + "          <description>A web page for creating a graph or map of the data.</description>\n"
                    + "          <function>download graph or map</function>\n"
                    + "        </CI_OnlineResource>\n")
            + "        <CI_OnlineResource>\n"
            + "          <linkage>"
            + XML.encodeAsXML(datasetUrl)
            + "</linkage>\n"
            + "          <name>OPeNDAP service: "
            + XML.encodeAsXML(title)
            + "</name>\n"
            + "          <description>The base URL for the OPeNDAP service.  "
            + "Add .html to get a web page with a form to download data. "
            + "Add .dds to get the dataset's structure. "
            + "Add .das to get the dataset's metadata. "
            + "Add .dods to download data via the OPeNDAP protocol.</description>\n"
            + "          <function>OPeNDAP</function>\n"
            + "        </CI_OnlineResource>\n"
            + (unknown.equals(infoUrl)
                ? ""
                : "        <CI_OnlineResource>\n"
                    + "          <linkage>"
                    + XML.encodeAsXML(infoUrl)
                    + "</linkage>\n"
                    + "          <name>Background information: "
                    + XML.encodeAsXML(title)
                    + "</name>\n"
                    + "          <description>Background information for the dataset.</description>\n"
                    + "          <function>background information</function>\n"
                    + "        </CI_OnlineResource>\n")
            + (accessibleViaWMS().length() > 0
                ? ""
                : "        <CI_OnlineResource>\n"
                    + "          <linkage>"
                    + XML.encodeAsXML(wmsUrl)
                    + "</linkage>\n"
                    + "          <name>WMS service: "
                    + XML.encodeAsXML(title)
                    + "</name>\n"
                    + "          <description>The base URL for the WMS service for this dataset.</description>\n"
                    + "          <function>WMS</function>\n"
                    + "        </CI_OnlineResource>\n"));

    // larger work citation: project
    if (!unknown.equals(project))
      writer.write(
          "        <lworkcit>\n"
              + "          <citeinfo>\n"
              + "            <origin>"
              + XML.encodeAsXML(project)
              + "</origin>\n"
              +
              // "            <pubdate>20081031</pubdate>\n" +
              // "            <title>NOAA Tsunami Inundation Gridding Project</title>\n" +
              // "            <geoform>Raster Digital Data</geoform>\n" +
              // "            <serinfo>\n" +
              // "              <sername>NOAA Tsunami Inundation DEMs</sername>\n" +
              // "              <issue>Named by primary coastal city, state in DEM.</issue>\n" +
              // "            </serinfo>\n" +
              // "            <pubinfo>\n" +
              // "              <pubplace>Boulder, Colorado</pubplace>\n" +
              // "              <publish>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center,
              // NESDIS, NOAA, U.S. Department of Commerce</publish>\n" +
              // "            </pubinfo>\n" +
              // "
              // <onlink>https://www.ngdc.noaa.gov/mgg/inundation/tsunami/inundation.html</onlink>\n" +
              // "            <CI_OnlineResource>\n" +
              // "
              // <linkage>https://www.ngdc.noaa.gov/mgg/inundation/tsunami/inundation.html</linkage>\n" +
              // "              <name>NOAA Tsunami Inundation Gridding Project</name>\n" +
              // "              <description>Project web page.</description>\n" +
              // "              <function>information</function>\n" +
              // "            </CI_OnlineResource>\n" +
              // "            <CI_OnlineResource>\n" +
              // "
              // <linkage>https://www.ngdc.noaa.gov/dem/squareCellGrid/map</linkage>\n" +
              // "              <name>Map Interface</name>\n" +
              // "              <description>Graphic geo-spatial search tool for locating completed
              // and planned NOAA tsunami inundation DEMs.</description>\n" +
              // "              <function>search</function>\n" +
              // "            </CI_OnlineResource>\n" +
              // "            <CI_OnlineResource>\n" +
              // "
              // <linkage>https://www.ngdc.noaa.gov/dem/squareCellGrid/search</linkage>\n" +
              // "              <name>DEM text search tool</name>\n" +
              // "              <description>Text search tool for locating completed and planned
              // NOAA tsunami inundation DEMs.</description>\n" +
              // "              <function>search</function>\n" +
              // "            </CI_OnlineResource>\n" +
              "          </citeinfo>\n"
              + "        </lworkcit>\n");

    writer.write("""
                  </citeinfo>
                </citation>
            """);

    // description
    writer.write(
        "    <descript>\n"
            + "      <abstract>"
            + XML.encodeAsXML(summary)
            + "</abstract>\n"
            + "      <purpose>"
            + unknown
            + "</purpose>\n"
            + "      <supplinf>"
            + XML.encodeAsXML(infoUrl)
            + "</supplinf>\n"
            + // OBIS uses

            // ??? ideally from "references", but it's a blob and they want components here
            /*"      <documnts>\n" +
            "        <userguid>\n" +
            "          <citeinfo>\n" +
            "            <origin>Kelly S. Carignan</origin>\n" +
            "            <origin>Lisa A. Taylor</origin>\n" +
            "            <origin>Barry W. Eakins</origin>\n" +
            "            <origin>Robin R. Warnken</origin>\n" +
            "            <origin>Elliot Lim</origin>\n" +
            "            <origin>Pamela R. Medley</origin>\n" +
            "            <origin_cntinfo>\n" +
            "              <cntinfo>\n" +
            "                <cntorgp>\n" +
            "                  <cntorg>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</cntorg>\n" +
            "                  <cntper>Lisa A. Taylor</cntper>\n" +
            "                </cntorgp>\n" +
            "                <cntaddr>\n" +
            "                  <addrtype>Mailing and Physical Address</addrtype>\n" +
            "                  <address>NOAA/NESDIS/NGDC  325 Broadway</address>\n" +
            "                  <city>Boulder</city>\n" +
            "                  <state>CO</state>\n" +
            "                  <postal>80305-3328</postal>\n" +
            "                  <country>USA</country>\n" +
            "                </cntaddr>\n" +
            "                <cntvoice>(303) 497-6767</cntvoice>\n" +
            "                <cnttdd>(303) 497-6958</cnttdd>\n" +
            "                <cntfax>(303) 497-6513</cntfax>\n" +
            "                <cntemail>Lisa.A.Taylor@noaa.gov</cntemail>\n" +
            "                <hours>7:30 - 5:00 Mountain</hours>\n" +
            "                <cntinst>Contact Data Center</cntinst>\n" +
            "              </cntinfo>\n" +
            "            </origin_cntinfo>\n" +
            "            <pubdate>200904</pubdate>\n" +
            "            <title>Digital Elevation Model of Adak, Alaska: Procedures, Data Sources, and Analysis</title>\n" +
            "            <serinfo>\n" +
            "              <sername>NOAA Technical Memorandum</sername>\n" +
            "              <issue>NESDIS NGDC-31</issue>\n" +
            "            </serinfo>\n" +
            "            <pubinfo>\n" +
            "              <pubplace>Boulder, CO</pubplace>\n" +
            "              <publish>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</publish>\n" +
            "              <publish_cntinfo>\n" +
            "                <cntinfo>\n" +
            "                  <cntorgp>\n" +
            "                    <cntorg>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce</cntorg>\n" +
            "                    <cntper>User Services</cntper>\n" +
            "                  </cntorgp>\n" +
            "                  <cntaddr>\n" +
            "                    <addrtype>Mailing and Physical Address</addrtype>\n" +
            "                    <address>NOAA/NESDIS/NGDC E/GC 325 Broadway</address>\n" +
            "                    <city>Boulder</city>\n" +
            "                    <state>CO</state>\n" +
            "                    <postal>80305-3328</postal>\n" +
            "                    <country>USA</country>\n" +
            "                  </cntaddr>\n" +
            "                  <cntvoice>(303) 497-6826</cntvoice>\n" +
            "                  <cntfax>(303) 497-6513</cntfax>\n" +
            "                  <cntemail>ngdc.info@noaa.gov</cntemail>\n" +
            "                  <hours>7:30 - 5:00 Mountain</hours>\n" +
            "                </cntinfo>\n" +
            "              </publish_cntinfo>\n" +
            "            </pubinfo>\n" +
            "            <othercit>29 pages</othercit>\n" +
            "            <onlink>https://www.ngdc.noaa.gov/dem/squareCellGrid/getReport/258</onlink>\n" +
            "            <CI_OnlineResource>\n" +
            "              <linkage>https://www.ngdc.noaa.gov/dem/squareCellGrid/getReport/258</linkage>\n" +
            "              <name>Digital Elevation Model of Adak, Alaska: Procedures, Data Sources and Analysis</name>\n" +
            "              <description>Report describing the development of the Adak, Alaska DEM</description>\n" +
            "              <function>download</function>\n" +
            "            </CI_OnlineResource>\n" +
            "          </citeinfo>\n" +
            "        </userguid>\n" +
            "      </documnts>\n" +
            */

            "    </descript>\n");

    // time range (even if no timeEdv)
    writer.write(
        "    <timeperd>\n"
            + "      <timeinfo>\n"
            + "        <rngdates>\n"
            + "          <begdate>"
            + (minTime.isEmpty() ? unknown : minTime)
            + "</begdate>\n"
            + "          <enddate>"
            + (maxTime.isEmpty() ? unknown : maxTime)
            + "</enddate>\n"
            + "        </rngdates>\n"
            + "      </timeinfo>\n"
            +
            // https://www.fgdc.gov/csdgmgraphical/ideninfo/timepd/current.htm
            "      <current>ground condition</current>\n"
            + // I think it means: that's what I see in the dataset
            "    </timeperd>\n");

    // ???  crap! FGDC requires lon to be +/-180 and has no guidance for 0 - 360 datasets
    // so just deal with some of the options
    // and use (float) to avoid float->double bruising
    // default: just the lon part already in -180 to 180.
    float lonMin = (float) Math2.minMax(-180, 180, lonEdv.destinationMinDouble());
    float lonMax = (float) Math2.minMax(-180, 180, lonEdv.destinationMaxDouble());
    // 0 to 360  -> -180 to 180
    if (lonEdv.destinationMinDouble() >= 0
        && lonEdv.destinationMinDouble() <= 20
        && lonEdv.destinationMaxDouble() >= 340) {
      lonMin = -180;
      lonMax = 180;
      // all lon >=180, so shift down 360
    } else if (lonEdv.destinationMinDouble() >= 180) {
      lonMin = (float) Math2.minMax(-180, 180, lonEdv.destinationMinDouble() - 360);
      lonMax = (float) Math2.minMax(-180, 180, lonEdv.destinationMaxDouble() - 360);
    }
    writer.write(
        "    <status>\n"
            + "      <progress>Complete</progress>\n"
            + "      <update>As needed</update>\n"
            + "    </status>\n"
            + "    <spdom>\n"
            + "      <bounding>\n"
            + "        <westbc>"
            + lonMin
            + "</westbc>\n"
            + "        <eastbc>"
            + lonMax
            + "</eastbc>\n"
            + "        <northbc>"
            + (float) Math2.minMax(-90, 90, latEdv.destinationMaxDouble())
            + "</northbc>\n"
            + "        <southbc>"
            + (float) Math2.minMax(-90, 90, latEdv.destinationMinDouble())
            + "</southbc>\n"
            + "      </bounding>\n"
            + "    </spdom>\n");

    // keywords,  from global keywords
    StringArray kar = StringArray.fromCSVNoBlanks(keywords);
    writer.write(
        "    <keywords>\n"
            + "      <theme>\n"
            + "        <themekt>"
            + (unknown.equals(keywordsVocabulary)
                ? "Uncontrolled"
                : XML.encodeAsXML(keywordsVocabulary))
            + "</themekt>\n");
    for (int i = 0; i < kar.size(); i++)
      writer.write("        <themekey>" + XML.encodeAsXML(kar.get(i)) + "</themekey>\n");
    writer.write("      </theme>\n");

    // use standardNames as keywords
    if (standardNames.size() > 0) {
      writer.write(
          "      <theme>\n"
              + "        <themekt>"
              + XML.encodeAsXML(
                  unknown.equals(standardNameVocabulary) ? "Uncontrolled" : standardNameVocabulary)
              + "</themekt>\n");
      for (int i = 0; i < standardNames.size(); i++)
        writer.write(
            "        <themekey>" + XML.encodeAsXML(standardNames.get(i)) + "</themekey>\n");
      writer.write("      </theme>\n");
    }

    writer.write("    </keywords>\n");

    // Platform and Instrument Indentification: satellite, sensor
    if (!unknown.equals(satellite) || !unknown.equals(sensor))
      writer.write(
          "    <plainsid>\n"
              + // long and short names the same since that's all I have
              "      <missname>"
              + XML.encodeAsXML(project)
              + "</missname>\n"
              + "      <platflnm>"
              + XML.encodeAsXML(satellite)
              + "</platflnm>\n"
              + "      <platfsnm>"
              + XML.encodeAsXML(satellite)
              + "</platfsnm>\n"
              + "      <instflnm>"
              + XML.encodeAsXML(sensor)
              + "</instflnm>\n"
              + "      <instshnm>"
              + XML.encodeAsXML(sensor)
              + "</instshnm>\n"
              + "    </plainsid>\n");

    // access constraints   and use constraints
    writer.write(
        "    <accconst>"
            + (getAccessibleTo() == null ? "None." : "Authorized users only")
            + "</accconst>\n"
            + "    <useconst>"
            + XML.encodeAsXML(license)
            + "</useconst>\n");

    // point of contact:   creatorName creatorEmail
    String conOrg = institution;
    String conName = creatorName;
    String conPhone = unknown;
    String conEmail = creatorEmail;
    String conPos = unknown;
    if (unknown.equals(conEmail)) {
      conOrg = adminInstitution;
      conName = adminIndividualName;
      conPhone = adminPhone;
      conEmail = adminEmail;
      conPos = adminPosition;
    }
    writer.write(
        "    <ptcontac>\n"
            + "      <cntinfo>\n"
            + "        <cntorgp>\n"
            + "          <cntorg>"
            + XML.encodeAsXML(conOrg)
            + "</cntorg>\n"
            + "          <cntper>"
            + XML.encodeAsXML(conName)
            + "</cntper>\n"
            + "        </cntorgp>\n"
            + "        <cntpos>"
            + XML.encodeAsXML(conPos)
            + "</cntpos>\n"
            + // required
            "        <cntaddr>\n"
            + "          <addrtype>Mailing and Physical Address</addrtype>\n"
            + "          <address>"
            + unknown
            + "</address>\n"
            + "          <address>"
            + unknown
            + "</address>\n"
            + "          <city>"
            + unknown
            + "</city>\n"
            + "          <state>"
            + unknown
            + "</state>\n"
            + "          <postal>"
            + unknown
            + "</postal>\n"
            + "          <country>"
            + unknown
            + "</country>\n"
            + "        </cntaddr>\n"
            + "        <cntvoice>"
            + XML.encodeAsXML(conPhone)
            + "</cntvoice>\n"
            +
            // "        <cntfax>303-497-6513</cntfax>\n" +
            "        <cntemail>"
            + XML.encodeAsXML(conEmail)
            + "</cntemail>\n"
            +
            // "        <hours>9am-5pm, M-F, Mountain Time</hours>\n" +
            // "        <cntinst>Contact NGDC&apos;s Marine Geology and Geophysics Division.
            // https://www.ngdc.noaa.gov/mgg/aboutmgg/contacts.html</cntinst>\n" +
            "      </cntinfo>\n"
            + "    </ptcontac>\n");

    // graphical view of the data
    writer.write(
        "    <browse>\n"
            + "      <browsen>"
            + XML.encodeAsXML(datasetUrl + ".graph")
            + "</browsen>\n"
            + "      <browsed>Web page to make a customized map or graph of the data</browsed>\n"
            + "      <browset>HTML</browset>\n"
            + "    </browse>\n");

    if (!unknown.equals(contributorName)
        || !unknown.equals(contributorEmail)
        || !unknown.equals(contributorRole)
        || !unknown.equals(acknowledgement))
      writer.write(
          "    <datacred>"
              + XML.encodeAsXML(
                  (unknown.equals(contributorName)
                          ? ""
                          : "Contributor Name: " + contributorName + "\n")
                      + (unknown.equals(contributorEmail)
                          ? ""
                          : "Contributor Email: " + contributorEmail + "\n")
                      + (unknown.equals(contributorRole)
                          ? ""
                          : "Contributor Role: " + contributorRole + "\n")
                      + (unknown.equals(acknowledgement)
                          ? ""
                          : "Acknowledgement: " + acknowledgement + "\n"))
              + "    </datacred>\n");

    writer.write(
        // "    <native>Microsoft Windows 2000 Version 5.2 (Build 3790) Service Pack 2; ESRI
        // ArcCatalog 9.2.4.1420</native>\n" +

        // aggregation info - if data is part of larger collection.
        // But ERDDAP encourages as much aggregation as possible for each dataset (e.g., all times).
        // "    <agginfo>\n" +
        // "      <conpckid>\n" +
        // "        <datsetid>gov.noaa.ngdc.mgg.dem:tigp</datsetid>\n" +
        // "      </conpckid>\n" +
        // "    </agginfo>\n" +
        "  </idinfo>\n");

    // data quality
    writer.write(
        "  <dataqual>\n"
            + "    <logic>"
            + unknown
            + "</logic>\n"
            + "    <complete>"
            + unknown
            + "</complete>\n"
            + "    <posacc>\n"
            + "      <horizpa>\n"
            + "        <horizpar>"
            + unknown
            + "</horizpar>\n"
            + "      </horizpa>\n"
            + "      <vertacc>\n"
            + "        <vertaccr>"
            + unknown
            + "</vertaccr>\n"
            + "      </vertacc>\n"
            + "    </posacc>\n"
            + "    <lineage>\n");

    // writer.write(
    // "      <srcinfo>\n" +
    // "        <srccite>\n" +
    // "          <citeinfo>\n" +
    // "            <origin>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center, NESDIS,
    // NOAA, U.S. Department of Commerce</origin>\n" +
    // "            <origin_cntinfo>\n" +
    // "              <cntinfo>\n" +
    // "                <cntorgp>\n" +
    // "                  <cntorg>DOC/NOAA/NESDIS/NGDC &gt; National Geophysical Data Center,
    // NESDIS, NOAA, U.S. Department of Commerce</cntorg>\n" +
    // "                  <cntper>User Services</cntper>\n" +
    // "                </cntorgp>\n" +
    // "                <cntaddr>\n" +
    // "                  <addrtype>Mailing and Physical Address</addrtype>\n" +
    // "                  <address>NOAA/NESDIS/NGDC E/GC 325 Broadway</address>\n" +
    // "                  <city>Boulder</city>\n" +
    // "                  <state>CO</state>\n" +
    // "                  <postal>80305-3328</postal>\n" +
    // "                  <country>USA</country>\n" +
    // "                </cntaddr>\n" +
    // "                <cntvoice>(303) 497-6826</cntvoice>\n" +
    // "                <cntfax>(303) 497-6513</cntfax>\n" +
    // "                <cntemail>ngdc.info@noaa.gov</cntemail>\n" +
    // "                <hours>7:30 - 5:00 Mountain</hours>\n" +
    // "              </cntinfo>\n" +
    // "            </origin_cntinfo>\n" +
    // "            <pubdate>2008</pubdate>\n" +
    // "            <title>NOS Hydrographic Surveys</title>\n" +
    // "            <onlink>https://www.ngdc.noaa.gov/mgg/bathymetry/hydro.html</onlink>\n" +
    // "            <CI_OnlineResource>\n" +
    // "              <linkage>https://www.ngdc.noaa.gov/mgg/bathymetry/hydro.html</linkage>\n" +
    // "              <name>NOS Hydrographic Survey Database</name>\n" +
    // "              <description>Digital database of NOS hydrographic surveys that date back to
    // the late 19th century.</description>\n" +
    // "              <function>download</function>\n" +
    // "            </CI_OnlineResource>\n" +
    // "          </citeinfo>\n" +
    // "        </srccite>\n" +
    // "        <typesrc>online</typesrc>\n" +
    // "        <srctime>\n" +
    // "          <timeinfo>\n" +
    // "            <rngdates>\n" +
    // "              <begdate>1933</begdate>\n" +
    // "              <enddate>2005</enddate>\n" +
    // "            </rngdates>\n" +
    // "          </timeinfo>\n" +
    // "          <srccurr>ground condition</srccurr>\n" +
    // "        </srctime>\n" +
    // "        <srccitea>NOS Hydrographic Surveys</srccitea>\n" +
    // "        <srccontr>hydrographic surveys</srccontr>\n" +
    // "      </srcinfo>\n");
    // and several other other <srcinfo>

    // process step:  lines from history
    String historyLines[] = String2.split(history, '\n');
    for (String step : historyLines) {
      String date = unknown;
      int spo = step.indexOf(' '); // date must be YYYY-MM-DD.* initially
      if (spo >= 10 && step.substring(0, 10).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
        date = String2.replaceAll(step.substring(0, 10), "-", ""); // now YYYYMMDD
      } else {
        spo = 0;
      }
      writer.write(
          "      <procstep>\n"
              + "        <procdesc>"
              + XML.encodeAsXML(step.substring(spo).trim())
              + "</procdesc>\n"
              + "        <procdate>"
              + XML.encodeAsXML(date)
              + "</procdate>\n"
              + "      </procstep>\n");
    }

    writer.write("""
                </lineage>
              </dataqual>
            """);

    // Spatial Data Organization information
    writer.write(
        "  <spdoinfo>\n"
            + "    <direct>Raster</direct>\n"
            + "    <rastinfo>\n"
            + "      <cvaltype>"
            + dataVariables[0].destinationDataType()
            + "</cvaltype>\n"
            + // cell value type
            "      <rasttype>Grid Cell</rasttype>\n"
            + "      <rowcount>"
            + latEdv.sourceValues().size()
            + "</rowcount>\n"
            + "      <colcount>"
            + lonEdv.sourceValues().size()
            + "</colcount>\n"
            + "      <vrtcount>"
            + (altEdv != null
                ? altEdv.sourceValues().size()
                : depthEdv != null ? depthEdv.sourceValues().size() : 1)
            + "</vrtcount>\n"
            + "    </rastinfo>\n"
            + "  </spdoinfo>\n");

    // Spatial Reference Information (very different for EDDTable and EDDGrid)
    writer.write(
        "  <spref>\n"
            + "    <horizsys>\n"
            + "      <geograph>\n"
            + // res is real number, so can't also indicate if (un)evenly spaced
            "        <latres>"
            + Math2.floatToDouble(Math.abs(latEdv.averageSpacing()))
            + "</latres>\n"
            + "        <longres>"
            + Math2.floatToDouble(Math.abs(lonEdv.averageSpacing()))
            + "</longres>\n"
            + "        <geogunit>Decimal degrees</geogunit>\n"
            + "      </geograph>\n"
            + "      <geodetic>\n"
            + "        <horizdn>D_WGS_1984</horizdn>\n"
            + // ??? I'm not certain this is true for all
            "        <ellips>WGS_1984</ellips>\n"
            + "        <semiaxis>6378137.000000</semiaxis>\n"
            + "        <denflat>298.257224</denflat>\n"
            + "      </geodetic>\n"
            + "    </horizsys>\n");

    if (altEdv != null || depthEdv != null) {
      writer.write(
          "    <vertdef>\n"
              + (altEdv != null
                  ? "      <altsys>\n"
                      + "        <altdatum>"
                      + unknown
                      + "</altdatum>\n"
                      + "        <altres>"
                      + (Double.isFinite(altEdv.averageSpacing())
                          ? "" + altEdv.averageSpacing()
                          : unknown)
                      +
                      // was          (altEdv.isEvenlySpaced()? "" + altEdv.averageSpacing() :
                      // unknown) +
                      "</altres>\n"
                      + // min distance between 2 adjacent values
                      "        <altunits>meters</altunits>\n"
                      + "        <altenc>Explicit elevation coordinate included with horizontal coordinates</altenc>\n"
                      + // 2012-12-28 was Unknown
                      "      </altsys>\n"
                  : depthEdv != null
                      ? "      <depthsys>\n"
                          + "        <depthdn>"
                          + unknown
                          + "</depthdn>\n"
                          + // depth datum name (from a vocabulary, e.g., "Mean sea level")
                          "        <depthres>"
                          + (depthEdv.isEvenlySpaced() ? "" + depthEdv.averageSpacing() : unknown)
                          + "</depthres>\n"
                          + // min distance between 2 adjacent values
                          "        <depthdu>meters</depthdu>\n"
                          + "        <depthem>Explicit depth coordinate included with horizontal coordinates</depthem>\n"
                          + // 2012-12-28 was Unknown
                          "      </depthsys>\n"
                      : "")
              + "    </vertdef>\n");
    }

    writer.write("  </spref>\n");

    // distribution information: admin...
    writer.write(
        "  <distinfo>\n"
            + "    <distrib>\n"
            + adminCntinfo
            + "    </distrib>\n"
            + "    <resdesc>"
            + XML.encodeAsXML(
                "ERDDAP, version "
                    + EDStatic.erddapVersion
                    + ": get metadata; download data; make graphs and maps.")
            + "</resdesc>\n"
            + "    <distliab>"
            + XML.encodeAsXML(license)
            + "</distliab>\n"
            + "    <stdorder>\n");

    // data file types
    List<EDDFileTypeInfo> dataFileTypes = EDD.getFileTypeOptions(true, false);
    for (int ft = 0; ft < dataFileTypes.size(); ft++)
      writer.write(
          "      <digform>\n"
              + "        <digtinfo>\n"
              + // digital transfer info
              "          <formname>"
              + dataFileTypes.get(ft).getFileTypeName()
              + "</formname>\n"
              + "          <formvern>1</formvern>\n"
              + "          <formspec>"
              + XML.encodeAsXML(
                  dataFileTypes.get(ft).getGridDescription(language)
                      + " "
                      + dataFileTypes.get(ft).getInfoUrl())
              + "</formspec>\n"
              +
              //           I think file decompression technique only used if file *always* encoded.
              // "          <filedec>gzip</filedec>\n" +
              "        </digtinfo>\n"
              + "        <digtopt>\n"
              + "          <onlinopt>\n"
              + "            <computer>\n"
              + "              <networka>\n"
              + "                <networkr>"
              + XML.encodeAsXML(datasetUrl + ".html")
              + "</networkr>\n"
              + "                <CI_OnlineResource>\n"
              + "                  <linkage>"
              + XML.encodeAsXML(datasetUrl + ".html")
              + "</linkage>\n"
              + "                  <name>"
              + XML.encodeAsXML(title)
              + "</name>\n"
              + "                  <description>Web page for accessing metadata and downloading data.</description>\n"
              + "                  <function>download</function>\n"
              + "                </CI_OnlineResource>\n"
              + "                <CI_OnlineResource>\n"
              + "                  <linkage>"
              + XML.encodeAsXML(datasetUrl + ".html")
              + "</linkage>\n"
              + "                  <name>"
              + XML.encodeAsXML(title)
              + "</name>\n"
              + "                  <description>Web page for accessing metadata and downloading data.</description>\n"
              + "                  <function>information</function>\n"
              + "                </CI_OnlineResource>\n"
              + "              </networka>\n"
              + "            </computer>\n"
              + "          </onlinopt>\n"
              + "        </digtopt>\n"
              + "      </digform>\n");

    // image file types
    List<EDDFileTypeInfo> imageFileTypes = EDD.getFileTypeOptions(true, true);
    for (int ft = 0; ft < imageFileTypes.size(); ft++)
      writer.write(
          "      <digform>\n"
              + "        <digtinfo>\n"
              + // digital transfer info
              "          <formname>"
              + imageFileTypes.get(ft).getFileTypeName()
              + "</formname>\n"
              + "          <formvern>1</formvern>\n"
              + "          <formspec>"
              + XML.encodeAsXML(
                  imageFileTypes.get(ft).getGridDescription(language)
                      + " "
                      + imageFileTypes.get(ft).getInfoUrl())
              + "</formspec>\n"
              +
              //           I think file decompression technique only used if file *always* encoded.
              // "          <filedec>gzip</filedec>\n" + //file decompression technique
              "        </digtinfo>\n"
              + "        <digtopt>\n"
              + "          <onlinopt>\n"
              + "            <computer>\n"
              + "              <networka>\n"
              + "                <networkr>"
              + XML.encodeAsXML(datasetUrl + ".graph")
              + "</networkr>\n"
              + "                <CI_OnlineResource>\n"
              + "                  <linkage>"
              + XML.encodeAsXML(datasetUrl + ".graph")
              + "</linkage>\n"
              + "                  <name>"
              + XML.encodeAsXML(title)
              + "</name>\n"
              + "                  <description>Web page for making a graph or map.</description>\n"
              + "                  <function>graphing</function>\n"
              + "                </CI_OnlineResource>\n"
              + "              </networka>\n"
              + "            </computer>\n"
              + "          </onlinopt>\n"
              + "        </digtopt>\n"
              + "      </digform>\n");

    writer.write(
        """
                  <fees>None</fees>
                </stdorder>
              </distinfo>
            """);

    writer.write(
        "  <metainfo>\n"
            + "    <metd>"
            + eddCreationDate
            + "</metd>\n"
            + "    <metc>\n"
            + adminCntinfo
            + "    </metc>\n"
            + "    <metstdn>Content Standard for Digital Geospatial Metadata: Extensions for Remote Sensing Metadata</metstdn>\n"
            + "    <metstdv>FGDC-STD-012-2002</metstdv>\n"
            + "    <mettc>universal time</mettc>\n"
            +
            // metadata access constraints
            "    <metac>"
            + (getAccessibleTo() == null ? "None." : "Authorized users only")
            + "</metac>\n"
            +
            // metadata use constraints
            "    <metuc>"
            + XML.encodeAsXML(license)
            + "</metuc>\n"
            + "  </metainfo>\n"
            + "</metadata>\n");
  }

  private void lower_writeISO19115(Writer writer)
      throws UnsupportedStorageException, DataStoreException, JAXBException, IOException {

    Metadata metadata =
        MetadataBuilder.buildMetadata(
            datasetID,
            creationTimeMillis(),
            combinedGlobalAttributes(),
            dataVariables(),
            axisVariables(),
            !String2.isSomething(accessibleViaWMS()),
            false);
    /*
     * By default the XML schema is the most recent version of the standard supported
     * by Apache SIS. But the legacy version published in 2007 is still in wide use.
     * The legacy version can be requested with the `METADATA_VERSION` property.
     */
    // Map<String,String> config = Map.of(org.apache.sis.xml.XML.METADATA_VERSION, "2007");

    writer.write(org.apache.sis.xml.XML.marshal(metadata));
  }

  /**
   * This writes the dataset's ISO 19115-2/19139 XML to the writer. <br>
   * The template is initially based on THREDDS ncIso output from <br>
   * https://oceanwatch.pfeg.noaa.gov/thredds/iso/satellite/MH/chla/8day <br>
   * (stored on Bob's computer as c:/programs/iso19115/threddsNcIsoMHchla8dayYYYYMM.xml). <br>
   * See also https://geo-ide.noaa.gov/wiki/index.php?title=NcISO#Questions_and_Answers
   *
   * <p>This is usually just called by the dataset's constructor, at the end of
   * EDDTable/Grid.ensureValid.
   *
   * <p>Help with schema: http://www.schemacentral.com/sc/niem21/e-gmd_contact-1.html <br>
   * List of nilReason: http://www.schemacentral.com/sc/niem21/a-gco_nilReason.html <br>
   * 2014-09-24 Example with protocols: <br>
   * http://oos.soest.hawaii.edu/pacioos/metadata/roms_hiig_forecast.xml
   *
   * <p>If getAccessibleTo()==null, the fgdc refers to http: ERDDAP links; otherwise, it refers to
   * https: ERDDAP links.
   *
   * @param language the index of the selected language
   * @param writer a UTF-8 writer
   * @throws Throwable if trouble (e.g., no latitude and longitude axes)
   */
  @Override
  public void writeISO19115(int language, Writer writer) throws Throwable {
    // FUTURE: support datasets with x,y (and not longitude,latitude)

    if (EDStatic.config.useSisISO19115) {
      lower_writeISO19115(writer);
      return;
    }

    // requirements
    if (lonIndex < 0 || latIndex < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.queryErrorAr[0] + EDStatic.messages.noXxxNoLLAr[0],
              EDStatic.messages.queryErrorAr[language] + EDStatic.messages.noXxxNoLLAr[language]));

    String tErddapUrl = EDStatic.preferredErddapUrl;
    String datasetUrl = tErddapUrl + "/griddap/" + datasetID;
    // String wcsUrl     = tErddapUrl + "/wcs/"     + datasetID() + "/" + wcsServer;  // "?" at end?
    String wmsUrl = tErddapUrl + "/wms/" + datasetID() + "/" + WMS_SERVER; // "?" at end?
    String domain = EDStatic.config.baseUrl;
    if (domain.startsWith("http://")) domain = domain.substring(7);
    else if (domain.startsWith("https://")) domain = domain.substring(8);
    String eddCreationDate = Calendar2.millisToIsoDateString(creationTimeMillis());

    String acknowledgement = combinedGlobalAttributes.getString("acknowledgement"); // acdd 1.3
    if (acknowledgement == null)
      acknowledgement = combinedGlobalAttributes.getString("acknowledgment"); // acdd 1.0
    String contributorName = combinedGlobalAttributes.getString("contributor_name");
    String contributorEmail = combinedGlobalAttributes.getString("contributor_email");
    String contributorRole = combinedGlobalAttributes.getString("contributor_role");
    String creatorName = combinedGlobalAttributes.getString("creator_name");
    String creatorEmail = combinedGlobalAttributes.getString("creator_email");
    String creatorType = combinedGlobalAttributes.getString("creator_type");
    creatorType = String2.validateAcddContactType(creatorType);
    if (!String2.isSomething2(creatorType) && String2.isSomething2(creatorName))
      creatorType = String2.guessAcddContactType(creatorName);
    if (creatorType == null) // creatorType will be something
    creatorType = "person"; // assume
    String dateCreated =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString("date_created")); // "" if trouble
    String dateIssued =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString("date_issued")); // "" if trouble
    if (dateCreated.length() > 10) dateCreated = dateCreated.substring(0, 10);
    if (dateIssued.length() > 10) dateIssued = dateIssued.substring(0, 10);
    if (dateCreated.startsWith("0000")) // year=0000 isn't valid
    dateCreated = "";
    if (dateIssued.startsWith("0000")) dateIssued = "";
    String history = combinedGlobalAttributes.getString("history");
    String infoUrl = combinedGlobalAttributes.getString("infoUrl");
    String institution = combinedGlobalAttributes.getString("institution");
    String keywords = combinedGlobalAttributes.getString("keywords");
    if (keywords == null) { // use the crude, ERDDAP keywords
      keywords = EDStatic.config.keywords;
    }
    String license = combinedGlobalAttributes.getString("license");
    String project = combinedGlobalAttributes.getString("project");
    if (project == null) project = institution;
    String standardNameVocabulary = combinedGlobalAttributes.getString("standard_name_vocabulary");

    // testMinimalMetadata is useful for Bob doing tests of validity of FGDC results
    //  when a dataset has minimal metadata
    boolean testMinimalMetadata = false; // only true when testing. normally false;
    if (testMinimalMetadata) {
      acknowledgement = null;
      contributorName = null;
      contributorEmail = null;
      contributorRole = null;
      creatorName = null;
      creatorEmail = null;
      dateCreated = null;
      dateIssued = null;
      history = null;
      // infoUrl         = null;  //ensureValid ensure that some things exist
      // institution     = null;
      keywords = null;
      license = null;
      project = null;
      standardNameVocabulary = null;
      // sourceUrl       = null;
    }

    if (!String2.isSomething(dateCreated))
      dateCreated = String2.isSomething(dateIssued) ? dateIssued : eddCreationDate;
    EDVLatGridAxis latEdv = (EDVLatGridAxis) axisVariables[latIndex];
    EDVLonGridAxis lonEdv = (EDVLonGridAxis) axisVariables[lonIndex];
    EDVTimeGridAxis timeEdv =
        timeIndex < 0 || testMinimalMetadata ? null : (EDVTimeGridAxis) axisVariables[timeIndex];
    EDVAltGridAxis altEdv =
        altIndex < 0 || testMinimalMetadata ? null : (EDVAltGridAxis) axisVariables[altIndex];
    EDVDepthGridAxis depthEdv =
        depthIndex < 0 || testMinimalMetadata ? null : (EDVDepthGridAxis) axisVariables[depthIndex];
    double minVert =
        Double.NaN; // in destination units (may be positive = up[I use] or down!? any units)
    double maxVert = Double.NaN;
    if (altEdv != null) {
      minVert = altEdv.destinationMinDouble();
      maxVert = altEdv.destinationMaxDouble();
    } else if (depthEdv != null) {
      minVert = -depthEdv.destinationMaxDouble(); // make into altitude
      maxVert = -depthEdv.destinationMinDouble();
    }
    String minTime = ""; // iso string with Z, may be ""
    String maxTime = "";
    if (timeEdv != null) {
      minTime = timeEdv.destinationMinString(); // differs from EDDGrid, may be ""
      maxTime = timeEdv.destinationMaxString();
      if (minTime.startsWith("0000")) minTime = ""; // 0000 is invalid in ISO 19115
      if (maxTime.startsWith("0000")) maxTime = "";
    }

    StringArray standardNames = new StringArray();
    if (!testMinimalMetadata) {
      for (EDVGridAxis axisVariable : axisVariables) {
        String sn = axisVariable.combinedAttributes().getString("standard_name");
        if (sn != null) standardNames.add(sn);
      }
      for (EDV dataVariable : dataVariables) {
        String sn = dataVariable.combinedAttributes().getString("standard_name");
        if (sn != null) standardNames.add(sn);
      }
    }

    // lon,lat Min/Max
    // ??? I'm not certain but I suspect ISO requires lon to be +/-180.
    //    I don't see guidance for 0 - 360 datasets.
    // so just deal with some of the options
    // and use (float) to avoid float->double bruising
    // default: just the lon part already in -180 to 180.
    float lonMin = (float) Math2.minMax(-180, 180, lonEdv.destinationMinDouble());
    float lonMax = (float) Math2.minMax(-180, 180, lonEdv.destinationMaxDouble());
    // 0 to 360  -> -180 to 180
    if (lonEdv.destinationMinDouble() >= 0
        && lonEdv.destinationMinDouble() <= 20
        && lonEdv.destinationMaxDouble() >= 340) {
      lonMin = -180;
      lonMax = 180;
      // all lon >=180, so shift down 360
    } else if (lonEdv.destinationMinDouble() >= 180) {
      lonMin = (float) Math2.minMax(-180, 180, lonEdv.destinationMinDouble() - 360);
      lonMax = (float) Math2.minMax(-180, 180, lonEdv.destinationMaxDouble() - 360);
    }
    float latMin = (float) Math2.minMax(-90, 90, latEdv.destinationMinDouble());
    float latMax = (float) Math2.minMax(-90, 90, latEdv.destinationMaxDouble());

    // write the xml
    // see https://geo-ide.noaa.gov/wiki/index.php?title=ISO_Namespaces
    writer.write( // ISO 19115
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            +
            // see
            // https://geo-ide.noaa.gov/wiki/index.php?title=ISO_Namespaces#Declaring_Namespaces_in_ISO_XML
            // This doesn't have to make sense or be correct. It is simply what their
            // system/validator deems correct.
            // 2019-08-15 from Anna Milan:
            "<gmi:MI_Metadata  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"https://www.isotc211.org/2005/gmi https://data.noaa.gov/resources/iso19139/schema.xsd\"\n"
            + "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gco=\"http://www.isotc211.org/2005/gco\"\n"
            + "  xmlns:gmd=\"http://www.isotc211.org/2005/gmd\"\n"
            + "  xmlns:gmx=\"http://www.isotc211.org/2005/gmx\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:gss=\"http://www.isotc211.org/2005/gss\"\n"
            + "  xmlns:gts=\"http://www.isotc211.org/2005/gts\"\n"
            + "  xmlns:gsr=\"http://www.isotc211.org/2005/gsr\"\n"
            + "  xmlns:gmi=\"http://www.isotc211.org/2005/gmi\"\n"
            + "  xmlns:srv=\"http://www.isotc211.org/2005/srv\">\n"
            + "  <gmd:fileIdentifier>\n"
            + "    <gco:CharacterString>"
            + datasetID()
            + "</gco:CharacterString>\n"
            + "  </gmd:fileIdentifier>\n"
            + "  <gmd:language>\n"
            + "    <gmd:LanguageCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:LanguageCode\" "
            + "codeListValue=\"eng\">eng</gmd:LanguageCode>\n"
            + "  </gmd:language>\n"
            + "  <gmd:characterSet>\n"
            + "    <gmd:MD_CharacterSetCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CharacterSetCode\" "
            + "codeListValue=\"UTF8\">UTF8</gmd:MD_CharacterSetCode>\n"
            + "  </gmd:characterSet>\n"
            + "  <gmd:hierarchyLevel>\n"
            + "    <gmd:MD_ScopeCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" "
            + "codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n"
            + "  </gmd:hierarchyLevel>\n"
            + "  <gmd:hierarchyLevel>\n"
            + "    <gmd:MD_ScopeCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" "
            + "codeListValue=\"service\">service</gmd:MD_ScopeCode>\n"
            + "  </gmd:hierarchyLevel>\n");

    // contact: use admin... ("resource provider" is last in chain responsible for metadata)
    //  (or use creator...?)
    writer.write(
        "  <gmd:contact>\n"
            + "    <gmd:CI_ResponsibleParty>\n"
            + "      <gmd:individualName>\n"
            + "        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminIndividualName)
            + "</gco:CharacterString>\n"
            + "      </gmd:individualName>\n"
            + "      <gmd:organisationName>\n"
            + "        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminInstitution)
            + "</gco:CharacterString>\n"
            + "      </gmd:organisationName>\n"
            + "      <gmd:contactInfo>\n"
            + "        <gmd:CI_Contact>\n"
            + "          <gmd:phone>\n"
            + "            <gmd:CI_Telephone>\n"
            + "              <gmd:voice>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminPhone)
            + "</gco:CharacterString>\n"
            + "              </gmd:voice>\n"
            + "            </gmd:CI_Telephone>\n"
            + "          </gmd:phone>\n"
            + "          <gmd:address>\n"
            + "            <gmd:CI_Address>\n"
            + "              <gmd:deliveryPoint>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminAddress)
            + "</gco:CharacterString>\n"
            + "              </gmd:deliveryPoint>\n"
            + "              <gmd:city>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminCity)
            + "</gco:CharacterString>\n"
            + "              </gmd:city>\n"
            + "              <gmd:administrativeArea>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminStateOrProvince)
            + "</gco:CharacterString>\n"
            + "              </gmd:administrativeArea>\n"
            + "              <gmd:postalCode>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminPostalCode)
            + "</gco:CharacterString>\n"
            + "              </gmd:postalCode>\n"
            + "              <gmd:country>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminCountry)
            + "</gco:CharacterString>\n"
            + "              </gmd:country>\n"
            + "              <gmd:electronicMailAddress>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminEmail)
            + "</gco:CharacterString>\n"
            + "              </gmd:electronicMailAddress>\n"
            + "            </gmd:CI_Address>\n"
            + "          </gmd:address>\n"
            + "        </gmd:CI_Contact>\n"
            + "      </gmd:contactInfo>\n"
            + "      <gmd:role>\n"
            + "        <gmd:CI_RoleCode codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" "
            + "codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n"
            + "      </gmd:role>\n"
            + "    </gmd:CI_ResponsibleParty>\n"
            + "  </gmd:contact>\n"
            + "  <gmd:dateStamp>\n"
            + "    <gco:Date>"
            + eddCreationDate
            + "</gco:Date>\n"
            + "  </gmd:dateStamp>\n"
            + "  <gmd:metadataStandardName>\n"
            + "    <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions "
            + "for Imagery and Gridded Data</gco:CharacterString>\n"
            + "  </gmd:metadataStandardName>\n"
            + "  <gmd:metadataStandardVersion>\n"
            + "    <gco:CharacterString>ISO 19115-2:2009(E)</gco:CharacterString>\n"
            + "  </gmd:metadataStandardVersion>\n"
            +

            // spatialRepresentation
            "  <gmd:spatialRepresentationInfo>\n"
            + "    <gmd:MD_GridSpatialRepresentation>\n"
            + "      <gmd:numberOfDimensions>\n"
            + "        <gco:Integer>"
            + axisVariables.length
            + "</gco:Integer>\n"
            + "      </gmd:numberOfDimensions>\n");

    for (int av = axisVariables.length - 1; av >= 0; av--) {
      EDVGridAxis edvGA = axisVariables[av];
      // these units are an attribute, but not a URL
      String encodedEdvGAUnits = XML.encodeAsHTMLAttribute(edvGA.ucumUnits());
      if (encodedEdvGAUnits.length() == 0)
        encodedEdvGAUnits = "1"; // shouldn't be "".  Since numeric, "1" is reasonable.

      // longitude   ("column")
      if (av == lonIndex) {
        writer.write(
            "      <gmd:axisDimensionProperties>\n"
                + "        <gmd:MD_Dimension>\n"
                + "          <gmd:dimensionName>\n"
                + "            <gmd:MD_DimensionNameTypeCode "
                + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
                + "codeListValue=\"column\">column</gmd:MD_DimensionNameTypeCode>\n"
                + "          </gmd:dimensionName>\n"
                + "          <gmd:dimensionSize>\n"
                + "            <gco:Integer>"
                + lonEdv.sourceValues().size()
                + "</gco:Integer>\n"
                + "          </gmd:dimensionSize>\n"
                + "          <gmd:resolution>\n"
                + "            <gco:Measure uom=\""
                + encodedEdvGAUnits
                + "\">"
                + Math.abs(lonEdv.averageSpacing())
                + "</gco:Measure>\n"
                + "          </gmd:resolution>\n"
                + "        </gmd:MD_Dimension>\n"
                + "      </gmd:axisDimensionProperties>\n");

        // latitude  ("row")
      } else if (av == latIndex) {
        writer.write(
            "      <gmd:axisDimensionProperties>\n"
                + "        <gmd:MD_Dimension>\n"
                + "          <gmd:dimensionName>\n"
                + "            <gmd:MD_DimensionNameTypeCode "
                + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
                + "codeListValue=\"row\">row</gmd:MD_DimensionNameTypeCode>\n"
                + "          </gmd:dimensionName>\n"
                + "          <gmd:dimensionSize>\n"
                + "            <gco:Integer>"
                + latEdv.sourceValues().size()
                + "</gco:Integer>\n"
                + "          </gmd:dimensionSize>\n"
                + "          <gmd:resolution>\n"
                + "            <gco:Measure uom=\""
                + encodedEdvGAUnits
                + "\">"
                + Math.abs(latEdv.averageSpacing())
                + "</gco:Measure>\n"
                + "          </gmd:resolution>\n"
                + "        </gmd:MD_Dimension>\n"
                + "      </gmd:axisDimensionProperties>\n");

        // vertical   ("vertical")
      } else if (av == altIndex || "depth".equals(edvGA.destinationName())) {
        if (!testMinimalMetadata)
          writer.write(
              "      <gmd:axisDimensionProperties>\n"
                  + "        <gmd:MD_Dimension>\n"
                  + "          <gmd:dimensionName>\n"
                  + "            <gmd:MD_DimensionNameTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
                  + "codeListValue=\"vertical\">vertical</gmd:MD_DimensionNameTypeCode>\n"
                  + "          </gmd:dimensionName>\n"
                  + "          <gmd:dimensionSize>\n"
                  + "            <gco:Integer>"
                  + edvGA.sourceValues().size()
                  + "</gco:Integer>\n"
                  + "          </gmd:dimensionSize>\n"
                  + (edvGA.sourceValues().size() == 1
                      ? "          <gmd:resolution gco:nilReason=\"inapplicable\"/>\n"
                      : "          <gmd:resolution>\n"
                          + "            <gco:Measure uom=\""
                          + encodedEdvGAUnits
                          + "\">"
                          + Math.abs(edvGA.averageSpacing())
                          + "</gco:Measure>\n"
                          + "          </gmd:resolution>\n")
                  + "        </gmd:MD_Dimension>\n"
                  + "      </gmd:axisDimensionProperties>\n");

        // time  ("temporal")
      } else if (av == timeIndex) {
        if (!testMinimalMetadata)
          writer.write(
              "      <gmd:axisDimensionProperties>\n"
                  + "        <gmd:MD_Dimension>\n"
                  + "          <gmd:dimensionName>\n"
                  + "            <gmd:MD_DimensionNameTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
                  + "codeListValue=\"temporal\">temporal</gmd:MD_DimensionNameTypeCode>\n"
                  + "          </gmd:dimensionName>\n"
                  + "          <gmd:dimensionSize>\n"
                  + "            <gco:Integer>"
                  + timeEdv.sourceValues().size()
                  + "</gco:Integer>\n"
                  + "          </gmd:dimensionSize>\n"
                  + (timeEdv.sourceValues().size() == 1
                      ? "          <gmd:resolution gco:nilReason=\"inapplicable\"/>\n"
                      : "          <gmd:resolution>\n"
                          + "            <gco:Measure uom=\"s\">"
                          + Math.abs(timeEdv.averageSpacing())
                          + "</gco:Measure>\n"
                          + "          </gmd:resolution>\n")
                  + "        </gmd:MD_Dimension>\n"
                  + "      </gmd:axisDimensionProperties>\n");

        // type=unknown(?!)
      } else {
        writer.write(
            "      <gmd:axisDimensionProperties>\n"
                + "        <gmd:MD_Dimension id=\""
                + edvGA.destinationName()
                + "\">\n"
                + "          <gmd:dimensionName>\n"
                + "            <gmd:MD_DimensionNameTypeCode "
                + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
                + "codeListValue=\"unknown\">unknown</gmd:MD_DimensionNameTypeCode>\n"
                + "          </gmd:dimensionName>\n"
                + "          <gmd:dimensionSize>\n"
                + "            <gco:Integer>"
                + edvGA.sourceValues().size()
                + "</gco:Integer>\n"
                + "          </gmd:dimensionSize>\n"
                + (edvGA.sourceValues().size() == 1
                    ? "          <gmd:resolution gco:nilReason=\"inapplicable\"/>\n"
                    : "          <gmd:resolution>\n"
                        + "            <gco:Measure uom=\""
                        + encodedEdvGAUnits
                        + "\">"
                        + Math.abs(edvGA.averageSpacing())
                        + "</gco:Measure>\n"
                        + "          </gmd:resolution>\n")
                + "        </gmd:MD_Dimension>\n"
                + "      </gmd:axisDimensionProperties>\n");
      }
    }

    // cellGeometry
    writer.write(
        """
                          <gmd:cellGeometry>
                            <gmd:MD_CellGeometryCode \
                    codeList="https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CellGeometryCode" \
                    codeListValue="area">area</gmd:MD_CellGeometryCode>
                          </gmd:cellGeometry>
                          <gmd:transformationParameterAvailability gco:nilReason="unknown"/>
                        </gmd:MD_GridSpatialRepresentation>
                      </gmd:spatialRepresentationInfo>
                    """);

    // *** IdentificationInfo loop
    int iiDataIdentification = 0;
    int iiERDDAP = 1;
    int iiOPeNDAP = 2;
    int iiWMS = 3;
    for (int ii = 0; ii <= iiWMS; ii++) {

      if (ii == iiWMS && accessibleViaWMS().length() > 0) continue;

      writer.write(
          "  <gmd:identificationInfo>\n"
              + (ii == iiDataIdentification
                  ? "    <gmd:MD_DataIdentification id=\"DataIdentification\">\n"
                  : ii == iiERDDAP
                      ? "    <srv:SV_ServiceIdentification id=\"ERDDAP-griddap\">\n"
                      : ii == iiOPeNDAP
                          ? "    <srv:SV_ServiceIdentification id=\"OPeNDAP\">\n"
                          : ii == iiWMS
                              ? "    <srv:SV_ServiceIdentification id=\"OGC-WMS\">\n"
                              : "    <gmd:ERROR id=\"ERROR\">\n")
              + "      <gmd:citation>\n"
              + "        <gmd:CI_Citation>\n"
              + "          <gmd:title>\n"
              + "            <gco:CharacterString>"
              + XML.encodeAsXML(title())
              + "</gco:CharacterString>\n"
              + "          </gmd:title>\n"
              + "          <gmd:date>\n"
              + "            <gmd:CI_Date>\n"
              + "              <gmd:date>\n"
              + "                <gco:Date>"
              + XML.encodeAsXML(dateCreated)
              + "</gco:Date>\n"
              + "              </gmd:date>\n"
              + "              <gmd:dateType>\n"
              + "                <gmd:CI_DateTypeCode "
              + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" "
              + "codeListValue=\"creation\">creation</gmd:CI_DateTypeCode>\n"
              + "              </gmd:dateType>\n"
              + "            </gmd:CI_Date>\n"
              + "          </gmd:date>\n"
              + (String2.isSomething(dateIssued)
                  ? "          <gmd:date>\n"
                      + "            <gmd:CI_Date>\n"
                      + "              <gmd:date>\n"
                      + "                <gco:Date>"
                      + XML.encodeAsXML(dateIssued)
                      + "</gco:Date>\n"
                      + "              </gmd:date>\n"
                      + "              <gmd:dateType>\n"
                      + "                <gmd:CI_DateTypeCode "
                      + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_DateTypeCode\" "
                      + "codeListValue=\"issued\">issued</gmd:CI_DateTypeCode>\n"
                      + "              </gmd:dateType>\n"
                      + "            </gmd:CI_Date>\n"
                      + "          </gmd:date>\n"
                  : "")
              +

              // naming_authority
              (ii == iiDataIdentification
                  ? "          <gmd:identifier>\n"
                      + "            <gmd:MD_Identifier>\n"
                      + "              <gmd:authority>\n"
                      + "                <gmd:CI_Citation>\n"
                      + "                  <gmd:title>\n"
                      + "                    <gco:CharacterString>"
                      + XML.encodeAsXML(domain)
                      + "</gco:CharacterString>\n"
                      + "                  </gmd:title>\n"
                      + "                  <gmd:date gco:nilReason=\"inapplicable\"/>\n"
                      + "                </gmd:CI_Citation>\n"
                      + "              </gmd:authority>\n"
                      + "              <gmd:code>\n"
                      + "                <gco:CharacterString>"
                      + XML.encodeAsXML(datasetID())
                      + "</gco:CharacterString>\n"
                      + "              </gmd:code>\n"
                      + "            </gmd:MD_Identifier>\n"
                      + "          </gmd:identifier>\n"
                  : "")
              + // other ii

              // citedResponsibleParty   role=originator:   from creator_email, creator_name,
              // creator_url, institution
              "          <gmd:citedResponsibleParty>\n"
              + "            <gmd:CI_ResponsibleParty>\n"
              + (String2.isSomething2(creatorName) && creatorType.matches("(person|position)")
                  ? "              <gmd:individualName>\n"
                      + "                <gco:CharacterString>"
                      + XML.encodeAsXML(creatorName)
                      + "</gco:CharacterString>\n"
                      + "              </gmd:individualName>\n"
                  : "              <gmd:individualName gco:nilReason=\"missing\"/>\n")
              + "              <gmd:organisationName>\n"
              + "                <gco:CharacterString>"
              + XML.encodeAsXML(
                  String2.isSomething2(creatorName) && creatorType.matches("(group|institution)")
                      ? creatorName
                      : institution)
              + "</gco:CharacterString>\n"
              + "              </gmd:organisationName>\n"
              +

              // originator: from creator_..., specify contactInfo
              "              <gmd:contactInfo>\n"
              + "                <gmd:CI_Contact>\n"
              + (String2.isSomething2(creatorEmail)
                  ? "                  <gmd:address>\n"
                      + "                    <gmd:CI_Address>\n"
                      + "                      <gmd:electronicMailAddress>\n"
                      + "                        <gco:CharacterString>"
                      + XML.encodeAsXML(creatorEmail)
                      + "</gco:CharacterString>\n"
                      + "                      </gmd:electronicMailAddress>\n"
                      + "                    </gmd:CI_Address>\n"
                      + "                  </gmd:address>\n"
                  : "                  <gmd:address gco:nilReason=\"missing\"/>\n")
              + "                  <gmd:onlineResource>\n"
              + "                    <gmd:CI_OnlineResource>\n"
              + "                      <gmd:linkage>\n"
              + // in ERDDAP, infoUrl is better and more reliable than creator_url
              "                        <gmd:URL>"
              + XML.encodeAsXML(infoUrl)
              + "</gmd:URL>\n"
              + "                      </gmd:linkage>\n"
              + "                      <gmd:protocol>\n"
              +
              // see list at
              // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv from
              // John Maurer
              "                        <gco:CharacterString>information</gco:CharacterString>\n"
              + "                      </gmd:protocol>\n"
              + "                      <gmd:applicationProfile>\n"
              + "                        <gco:CharacterString>web browser</gco:CharacterString>\n"
              + "                      </gmd:applicationProfile>\n"
              + "                      <gmd:name>\n"
              + "                        <gco:CharacterString>Background Information</gco:CharacterString>\n"
              + "                      </gmd:name>\n"
              + "                      <gmd:description>\n"
              + "                        <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
              + "                      </gmd:description>\n"
              + "                      <gmd:function>\n"
              + "                        <gmd:CI_OnLineFunctionCode "
              + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" "
              + "codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
              + "                      </gmd:function>\n"
              + "                    </gmd:CI_OnlineResource>\n"
              + "                  </gmd:onlineResource>\n"
              + "                </gmd:CI_Contact>\n"
              + "              </gmd:contactInfo>\n"
              + "              <gmd:role>\n"
              + "                <gmd:CI_RoleCode "
              + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" "
              + "codeListValue=\"originator\">originator</gmd:CI_RoleCode>\n"
              + "              </gmd:role>\n"
              + "            </gmd:CI_ResponsibleParty>\n"
              + "          </gmd:citedResponsibleParty>\n");

      // contributor_name (ncISO assumes it's a name; I assume it's an organisation), _role
      if (contributorName != null || contributorRole != null)
        writer.write(
            "          <gmd:citedResponsibleParty>\n"
                + "            <gmd:CI_ResponsibleParty>\n"
                + "              <gmd:individualName gco:nilReason=\"missing\"/>\n"
                + (contributorName == null
                    ? "              <gmd:organisationName gco:nilReason=\"missing\"/>\n"
                    : "              <gmd:organisationName>\n"
                        + "                <gco:CharacterString>"
                        + XML.encodeAsXML(contributorName)
                        + "</gco:CharacterString>\n"
                        + "              </gmd:organisationName>\n")
                + (contributorEmail == null
                    ? "              <gmd:contactInfo gco:nilReason=\"missing\"/>\n"
                    : "              <gmd:contactInfo>\n"
                        + "                <gmd:CI_Contact>\n"
                        + "                  <gmd:address>\n"
                        + "                    <gmd:CI_Address>\n"
                        + "                      <gmd:electronicMailAddress>\n"
                        + "                        <gco:CharacterString>"
                        + XML.encodeAsXML(contributorEmail)
                        + "</gco:CharacterString>\n"
                        + "                      </gmd:electronicMailAddress>\n"
                        + "                    </gmd:CI_Address>\n"
                        + "                  </gmd:address>\n"
                        + "                </gmd:CI_Contact>\n"
                        + "              </gmd:contactInfo>\n")
                + "              <gmd:role>\n"
                + "                <gmd:CI_RoleCode "
                + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" "
                +
                // contributor isn't in the codeList. I asked that it be added.
                // ncISO used something *not* in the list.  Isn't this a controlled vocabulary?
                "codeListValue=\"contributor\">contributor</gmd:CI_RoleCode>\n"
                + "              </gmd:role>\n"
                + "            </gmd:CI_ResponsibleParty>\n"
                + "          </gmd:citedResponsibleParty>\n"); // end of  if (contributorName !=
      // null ...

      writer.write(
          "        </gmd:CI_Citation>\n"
              + "      </gmd:citation>\n"
              +

              // abstract
              "      <gmd:abstract>\n"
              + "        <gco:CharacterString>"
              + XML.encodeAsXML(summary())
              + "</gco:CharacterString>\n"
              + "      </gmd:abstract>\n");

      // **
      if (ii == iiDataIdentification) {
        writer.write(

            // credit
            (acknowledgement == null
                    ? "      <gmd:credit gco:nilReason=\"missing\"/>\n"
                    : "      <gmd:credit>\n"
                        + "        <gco:CharacterString>"
                        + XML.encodeAsXML(acknowledgement)
                        + "</gco:CharacterString>\n"
                        + "      </gmd:credit>\n")
                +

                // pointOfContact
                "      <gmd:pointOfContact>\n"
                + "        <gmd:CI_ResponsibleParty>\n"
                + (String2.isSomething2(creatorName) && creatorType.matches("(person|position)")
                    ? "          <gmd:individualName>\n"
                        + "            <gco:CharacterString>"
                        + XML.encodeAsXML(creatorName)
                        + "</gco:CharacterString>\n"
                        + "          </gmd:individualName>\n"
                    : "          <gmd:individualName gco:nilReason=\"missing\"/>\n")
                + "          <gmd:organisationName>\n"
                + "            <gco:CharacterString>"
                + XML.encodeAsXML(
                    String2.isSomething2(creatorName) && creatorType.matches("(group|institution)")
                        ? creatorName
                        : institution)
                + "</gco:CharacterString>\n"
                + "          </gmd:organisationName>\n"
                +

                // originator: from creator_..., specify contactInfo
                "          <gmd:contactInfo>\n"
                + "            <gmd:CI_Contact>\n"
                + (creatorEmail == null
                    ? "              <gmd:address gco:nilReason=\"missing\"/>\n"
                    : "              <gmd:address>\n"
                        + "                <gmd:CI_Address>\n"
                        + "                  <gmd:electronicMailAddress>\n"
                        + "                    <gco:CharacterString>"
                        + XML.encodeAsXML(creatorEmail)
                        + "</gco:CharacterString>\n"
                        + "                  </gmd:electronicMailAddress>\n"
                        + "                </gmd:CI_Address>\n"
                        + "              </gmd:address>\n")
                + "              <gmd:onlineResource>\n"
                + "                <gmd:CI_OnlineResource>\n"
                + "                  <gmd:linkage>\n"
                + // in ERDDAP, infoUrl is better and more reliable than creator_url
                "                    <gmd:URL>"
                + XML.encodeAsXML(infoUrl)
                + "</gmd:URL>\n"
                + "                  </gmd:linkage>\n"
                + "                  <gmd:protocol>\n"
                +
                // see list at
                // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv from
                // John Maurer
                "                    <gco:CharacterString>information</gco:CharacterString>\n"
                + "                  </gmd:protocol>\n"
                + "                  <gmd:applicationProfile>\n"
                + "                    <gco:CharacterString>web browser</gco:CharacterString>\n"
                + "                  </gmd:applicationProfile>\n"
                + "                  <gmd:name>\n"
                + "                    <gco:CharacterString>Background Information</gco:CharacterString>\n"
                + "                  </gmd:name>\n"
                + "                  <gmd:description>\n"
                + "                    <gco:CharacterString>Background information from the source</gco:CharacterString>\n"
                + "                  </gmd:description>\n"
                + "                  <gmd:function>\n"
                + "                    <gmd:CI_OnLineFunctionCode "
                + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_OnLineFunctionCode\" "
                + "codeListValue=\"information\">information</gmd:CI_OnLineFunctionCode>\n"
                + "                  </gmd:function>\n"
                + "                </gmd:CI_OnlineResource>\n"
                + "              </gmd:onlineResource>\n"
                + "            </gmd:CI_Contact>\n"
                + "          </gmd:contactInfo>\n"
                + "          <gmd:role>\n"
                + "            <gmd:CI_RoleCode "
                + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" "
                + "codeListValue=\"pointOfContact\">pointOfContact</gmd:CI_RoleCode>\n"
                + "          </gmd:role>\n"
                + "        </gmd:CI_ResponsibleParty>\n"
                + "      </gmd:pointOfContact>\n");

        // keywords, from global keywords
        if (keywords != null) {
          StringArray kar = StringArray.fromCSVNoBlanks(keywords);

          // segregate gcmdKeywords and remove cf standard_names
          StringArray gcmdKeywords = new StringArray();
          for (int i = kar.size() - 1; i >= 0; i--) {
            String kari = kar.get(i);
            if (kari.indexOf(" > ") > 0) {
              gcmdKeywords.add(kari);
              kar.remove(i);

            } else if (kari.indexOf('_') > 0) {
              kar.remove(i); // a multi-word CF Standard Name

            } else if (kari.length() <= LONGEST_ONE_WORD_CF_STANDARD_NAMES
                && // quick accept if short enough
                ONE_WORD_CF_STANDARD_NAMES.indexOf(kari) >= 0) { // few, so linear search is quick
              kar.remove(i); // a one word CF Standard Name
            }
          }

          // keywords not from vocabulary
          if (kar.size() > 0) {
            writer.write(
                """
                          <gmd:descriptiveKeywords>
                            <gmd:MD_Keywords>
                    """);

            for (int i = 0; i < kar.size(); i++)
              writer.write(
                  "          <gmd:keyword>\n"
                      + "            <gco:CharacterString>"
                      + XML.encodeAsXML(kar.get(i))
                      + "</gco:CharacterString>\n"
                      + "          </gmd:keyword>\n");

            writer.write(
                """
                                      <gmd:type>
                                        <gmd:MD_KeywordTypeCode \
                            codeList="https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode" \
                            codeListValue="theme">theme</gmd:MD_KeywordTypeCode>
                                      </gmd:type>
                                      <gmd:thesaurusName gco:nilReason="unknown"/>
                                    </gmd:MD_Keywords>
                                  </gmd:descriptiveKeywords>
                            """);
          }

          // gcmd keywords
          if (gcmdKeywords.size() > 0) {
            writer.write(
                """
                          <gmd:descriptiveKeywords>
                            <gmd:MD_Keywords>
                    """);

            for (int i = 0; i < gcmdKeywords.size(); i++)
              writer.write(
                  "          <gmd:keyword>\n"
                      + "            <gco:CharacterString>"
                      + XML.encodeAsXML(gcmdKeywords.get(i))
                      + "</gco:CharacterString>\n"
                      + "          </gmd:keyword>\n");

            writer.write(
                """
                                      <gmd:type>
                                        <gmd:MD_KeywordTypeCode \
                            codeList="https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode" \
                            codeListValue="theme">theme</gmd:MD_KeywordTypeCode>
                                      </gmd:type>
                                      <gmd:thesaurusName>
                                        <gmd:CI_Citation>
                                          <gmd:title>
                                            <gco:CharacterString>GCMD Science Keywords</gco:CharacterString>
                                          </gmd:title>
                                          <gmd:date gco:nilReason="unknown"/>
                                        </gmd:CI_Citation>
                                      </gmd:thesaurusName>
                                    </gmd:MD_Keywords>
                                  </gmd:descriptiveKeywords>
                            """);
          }
        }

        // keywords (project)
        if (project != null)
          writer.write(
              "      <gmd:descriptiveKeywords>\n"
                  + "        <gmd:MD_Keywords>\n"
                  + "          <gmd:keyword>\n"
                  + "            <gco:CharacterString>"
                  + XML.encodeAsXML(project)
                  + "</gco:CharacterString>\n"
                  + "          </gmd:keyword>\n"
                  + "          <gmd:type>\n"
                  + "            <gmd:MD_KeywordTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" "
                  + "codeListValue=\"project\">project</gmd:MD_KeywordTypeCode>\n"
                  + "          </gmd:type>\n"
                  + "          <gmd:thesaurusName gco:nilReason=\"unknown\"/>\n"
                  + "        </gmd:MD_Keywords>\n"
                  + "      </gmd:descriptiveKeywords>\n");

        // keywords - variable (CF) standard_names
        if (standardNames.size() > 0) {
          writer.write(
              """
                        <gmd:descriptiveKeywords>
                          <gmd:MD_Keywords>
                  """);
          for (int sn = 0; sn < standardNames.size(); sn++)
            writer.write(
                "          <gmd:keyword>\n"
                    + "            <gco:CharacterString>"
                    + XML.encodeAsXML(standardNames.get(sn))
                    + "</gco:CharacterString>\n"
                    + "          </gmd:keyword>\n");
          writer.write(
              "          <gmd:type>\n"
                  + "            <gmd:MD_KeywordTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_KeywordTypeCode\" "
                  + "codeListValue=\"theme\">theme</gmd:MD_KeywordTypeCode>\n"
                  + "          </gmd:type>\n"
                  + "          <gmd:thesaurusName>\n"
                  + "            <gmd:CI_Citation>\n"
                  + "              <gmd:title>\n"
                  + "                <gco:CharacterString>"
                  + // if not specified, CF is a guess
                  XML.encodeAsXML(standardNameVocabulary == null ? "CF" : standardNameVocabulary)
                  + "</gco:CharacterString>\n"
                  + "              </gmd:title>\n"
                  + "              <gmd:date gco:nilReason=\"unknown\"/>\n"
                  + "            </gmd:CI_Citation>\n"
                  + "          </gmd:thesaurusName>\n"
                  + "        </gmd:MD_Keywords>\n"
                  + "      </gmd:descriptiveKeywords>\n");
        }

        // resourceConstraints  (license)
        if (license != null)
          writer.write(
              "      <gmd:resourceConstraints>\n"
                  + "        <gmd:MD_LegalConstraints>\n"
                  + "          <gmd:useLimitation>\n"
                  + "            <gco:CharacterString>"
                  + XML.encodeAsXML(license)
                  + "</gco:CharacterString>\n"
                  + "          </gmd:useLimitation>\n"
                  + "        </gmd:MD_LegalConstraints>\n"
                  + "      </gmd:resourceConstraints>\n");

        // aggregationInfo (project), larger work
        if (project != null)
          writer.write(
              "      <gmd:aggregationInfo>\n"
                  + "        <gmd:MD_AggregateInformation>\n"
                  + "          <gmd:aggregateDataSetName>\n"
                  + "            <gmd:CI_Citation>\n"
                  + "              <gmd:title>\n"
                  + "                <gco:CharacterString>"
                  + XML.encodeAsXML(project)
                  + "</gco:CharacterString>\n"
                  + "              </gmd:title>\n"
                  + "              <gmd:date gco:nilReason=\"inapplicable\"/>\n"
                  + "            </gmd:CI_Citation>\n"
                  + "          </gmd:aggregateDataSetName>\n"
                  + "          <gmd:associationType>\n"
                  + "            <gmd:DS_AssociationTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" "
                  + "codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n"
                  + "          </gmd:associationType>\n"
                  + "          <gmd:initiativeType>\n"
                  + "            <gmd:DS_InitiativeTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" "
                  + "codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n"
                  + "          </gmd:initiativeType>\n"
                  + "        </gmd:MD_AggregateInformation>\n"
                  + "      </gmd:aggregationInfo>\n");

        // aggregation, larger work       Unidata CDM  (? ncISO does this)
        if (!CDM_OTHER.equals(cdmDataType())) {
          writer.write(
              "      <gmd:aggregationInfo>\n"
                  + "        <gmd:MD_AggregateInformation>\n"
                  + "          <gmd:aggregateDataSetIdentifier>\n"
                  + "            <gmd:MD_Identifier>\n"
                  + "              <gmd:authority>\n"
                  + "                <gmd:CI_Citation>\n"
                  + "                  <gmd:title>\n"
                  + "                    <gco:CharacterString>Unidata Common Data Model</gco:CharacterString>\n"
                  + "                  </gmd:title>\n"
                  + "                  <gmd:date gco:nilReason=\"inapplicable\"/>\n"
                  + "                </gmd:CI_Citation>\n"
                  + "              </gmd:authority>\n"
                  + "              <gmd:code>\n"
                  + "                <gco:CharacterString>"
                  + cdmDataType()
                  + "</gco:CharacterString>\n"
                  + "              </gmd:code>\n"
                  + "            </gmd:MD_Identifier>\n"
                  + "          </gmd:aggregateDataSetIdentifier>\n"
                  + "          <gmd:associationType>\n"
                  + "            <gmd:DS_AssociationTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_AssociationTypeCode\" "
                  + "codeListValue=\"largerWorkCitation\">largerWorkCitation</gmd:DS_AssociationTypeCode>\n"
                  + "          </gmd:associationType>\n"
                  + "          <gmd:initiativeType>\n"
                  + "            <gmd:DS_InitiativeTypeCode "
                  + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:DS_InitiativeTypeCode\" "
                  + "codeListValue=\"project\">project</gmd:DS_InitiativeTypeCode>\n"
                  + "          </gmd:initiativeType>\n"
                  + "        </gmd:MD_AggregateInformation>\n"
                  + "      </gmd:aggregationInfo>\n");
        }

        // language
        writer.write(
            "      <gmd:language>\n"
                + "        <gco:CharacterString>eng</gco:CharacterString>\n"
                + "      </gmd:language>\n"
                +

                // see category list
                // http://www.schemacentral.com/sc/niem21/e-gmd_MD_TopicCategoryCode.html
                // options: climatologyMeteorologyAtmosphere(ncIso has), geoscientificInformation,
                // elevation, oceans, ...???
                // I chose geoscientificInformation because it is the most general. Will always be
                // true.
                "      <gmd:topicCategory>\n"
                + "        <gmd:MD_TopicCategoryCode>geoscientificInformation</gmd:MD_TopicCategoryCode>\n"
                + "      </gmd:topicCategory>\n"
                +
                // extent
                "      <gmd:extent>\n");

      } else if (ii == iiERDDAP) {
        writer.write(
            "      <srv:serviceType>\n"
                + "        <gco:LocalName>ERDDAP griddap</gco:LocalName>\n"
                + "      </srv:serviceType>\n"
                +
                // extent
                "      <srv:extent>\n");

      } else if (ii == iiOPeNDAP) {
        writer.write(
            "      <srv:serviceType>\n"
                + "        <gco:LocalName>OPeNDAP</gco:LocalName>\n"
                + "      </srv:serviceType>\n"
                +
                // extent
                "      <srv:extent>\n");

      } else if (ii == iiWMS) {
        writer.write(
            "      <srv:serviceType>\n"
                + "        <gco:LocalName>Open Geospatial Consortium Web Map Service (WMS)</gco:LocalName>\n"
                + "      </srv:serviceType>\n"
                +
                // extent
                "      <srv:extent>\n");
      }

      // all ii
      writer.write(
          "        <gmd:EX_Extent"
              + (ii == iiDataIdentification ? " id=\"boundingExtent\"" : "")
              + ">\n"
              + "          <gmd:geographicElement>\n"
              + "            <gmd:EX_GeographicBoundingBox"
              + (ii == iiDataIdentification ? " id=\"boundingGeographicBoundingBox\"" : "")
              + ">\n"
              + "              <gmd:extentTypeCode>\n"
              + "                <gco:Boolean>1</gco:Boolean>\n"
              + "              </gmd:extentTypeCode>\n"
              + "              <gmd:westBoundLongitude>\n"
              + "                <gco:Decimal>"
              + lonMin
              + "</gco:Decimal>\n"
              + "              </gmd:westBoundLongitude>\n"
              + "              <gmd:eastBoundLongitude>\n"
              + "                <gco:Decimal>"
              + lonMax
              + "</gco:Decimal>\n"
              + "              </gmd:eastBoundLongitude>\n"
              + "              <gmd:southBoundLatitude>\n"
              + "                <gco:Decimal>"
              + latMin
              + "</gco:Decimal>\n"
              + "              </gmd:southBoundLatitude>\n"
              + "              <gmd:northBoundLatitude>\n"
              + "                <gco:Decimal>"
              + latMax
              + "</gco:Decimal>\n"
              + "              </gmd:northBoundLatitude>\n"
              + "            </gmd:EX_GeographicBoundingBox>\n"
              + "          </gmd:geographicElement>\n"
              + (minTime.isEmpty() || maxTime.isEmpty()
                  ? ""
                  : "          <gmd:temporalElement>\n"
                      + "            <gmd:EX_TemporalExtent"
                      + (ii == iiDataIdentification ? " id=\"boundingTemporalExtent\"" : "")
                      + ">\n"
                      + "              <gmd:extent>\n"
                      + "                <gml:TimePeriod gml:id=\""
                      + // id is required    //ncISO has "d293"
                      (ii == iiDataIdentification
                          ? "DI"
                          : ii == iiERDDAP
                              ? "ED"
                              : ii == iiOPeNDAP ? "OD" : ii == iiWMS ? "WMS" : "ERROR")
                      + "_gmdExtent_timePeriod_id\">\n"
                      + "                  <gml:description>seconds</gml:description>\n"
                      + "                  <gml:beginPosition>"
                      + minTime
                      + "</gml:beginPosition>\n"
                      + "                  <gml:endPosition>"
                      + maxTime
                      + "</gml:endPosition>\n"
                      + "                </gml:TimePeriod>\n"
                      + "              </gmd:extent>\n"
                      + "            </gmd:EX_TemporalExtent>\n"
                      + "          </gmd:temporalElement>\n")
              + (Double.isNaN(minVert) || Double.isNaN(maxVert)
                  ? ""
                  : "          <gmd:verticalElement>\n"
                      + "            <gmd:EX_VerticalExtent>\n"
                      + "              <gmd:minimumValue><gco:Real>"
                      + minVert
                      + "</gco:Real></gmd:minimumValue>\n"
                      + "              <gmd:maximumValue><gco:Real>"
                      + maxVert
                      + "</gco:Real></gmd:maximumValue>\n"
                      +
                      // !!!needs work.   info is sometimes available e.g., in coastwatch files
                      // http://www.schemacentral.com/sc/niem21/e-gmd_verticalCRS-1.html
                      "              <gmd:verticalCRS gco:nilReason=\"missing\"/>\n"
                      + // ???
                      "            </gmd:EX_VerticalExtent>\n"
                      + "          </gmd:verticalElement>\n")
              + "        </gmd:EX_Extent>\n");

      if (ii == iiDataIdentification) {
        writer.write(
            """
                      </gmd:extent>
                    </gmd:MD_DataIdentification>
                """);
      }

      if (ii == iiERDDAP) {
        writer.write(
            "      </srv:extent>\n"
                + "      <srv:couplingType>\n"
                + "        <srv:SV_CouplingType "
                + "codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" "
                + // gone!
                "codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
                + "      </srv:couplingType>\n"
                + "      <srv:containsOperations>\n"
                + "        <srv:SV_OperationMetadata>\n"
                + "          <srv:operationName>\n"
                + "            <gco:CharacterString>ERDDAPgriddapDatasetQueryAndAccess</gco:CharacterString>\n"
                + "          </srv:operationName>\n"
                + "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
                + // Distributed Computing Platform
                "          <srv:connectPoint>\n"
                + "            <gmd:CI_OnlineResource>\n"
                + "              <gmd:linkage>\n"
                + "                <gmd:URL>"
                + XML.encodeAsXML(datasetUrl)
                + "</gmd:URL>\n"
                + "              </gmd:linkage>\n"
                + "              <gmd:protocol>\n"
                +
                // see list at
                // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv
                "                <gco:CharacterString>ERDDAP:griddap</gco:CharacterString>\n"
                + "              </gmd:protocol>\n"
                + "              <gmd:name>\n"
                + "                <gco:CharacterString>ERDDAP-griddap</gco:CharacterString>\n"
                + "              </gmd:name>\n"
                + "              <gmd:description>\n"
                + "                <gco:CharacterString>ERDDAP's griddap service (a flavor of OPeNDAP) "
                + "for gridded data. Add different extensions (e.g., .html, .graph, .das, .dds) "
                + "to the base URL for different purposes.</gco:CharacterString>\n"
                + "              </gmd:description>\n"
                + "              <gmd:function>\n"
                + "                <gmd:CI_OnLineFunctionCode "
                + "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" "
                + "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
                + "              </gmd:function>\n"
                + "            </gmd:CI_OnlineResource>\n"
                + "          </srv:connectPoint>\n"
                + "        </srv:SV_OperationMetadata>\n"
                + "      </srv:containsOperations>\n"
                + "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n"
                + "    </srv:SV_ServiceIdentification>\n");
      }

      if (ii == iiOPeNDAP) {
        writer.write(
            "      </srv:extent>\n"
                + "      <srv:couplingType>\n"
                + "        <srv:SV_CouplingType "
                + "codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" "
                + "codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
                + "      </srv:couplingType>\n"
                + "      <srv:containsOperations>\n"
                + "        <srv:SV_OperationMetadata>\n"
                + "          <srv:operationName>\n"
                + "            <gco:CharacterString>OPeNDAPDatasetQueryAndAccess</gco:CharacterString>\n"
                + "          </srv:operationName>\n"
                + "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
                + // Distributed Computing Platform
                "          <srv:connectPoint>\n"
                + "            <gmd:CI_OnlineResource>\n"
                + "              <gmd:linkage>\n"
                + "                <gmd:URL>"
                + XML.encodeAsXML(datasetUrl)
                + "</gmd:URL>\n"
                + "              </gmd:linkage>\n"
                + "              <gmd:protocol>\n"
                +
                // see list at
                // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv
                "                <gco:CharacterString>OPeNDAP:OPeNDAP</gco:CharacterString>\n"
                + "              </gmd:protocol>\n"
                + "              <gmd:name>\n"
                + "                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n"
                + "              </gmd:name>\n"
                + "              <gmd:description>\n"
                + "                <gco:CharacterString>An OPeNDAP service for gridded data. Add different extensions "
                + "(e.g., .html, .das, .dds) to the base URL for different purposes.</gco:CharacterString>\n"
                + "              </gmd:description>\n"
                + "              <gmd:function>\n"
                + "                <gmd:CI_OnLineFunctionCode "
                + "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" "
                + "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
                + "              </gmd:function>\n"
                + "            </gmd:CI_OnlineResource>\n"
                + "          </srv:connectPoint>\n"
                + "        </srv:SV_OperationMetadata>\n"
                + "      </srv:containsOperations>\n"
                + "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n"
                + "    </srv:SV_ServiceIdentification>\n");
      }

      if (ii == iiWMS) {
        writer.write(
            "      </srv:extent>\n"
                + "      <srv:couplingType>\n"
                + "        <srv:SV_CouplingType "
                + "codeList=\"https://data.noaa.gov/ISO19139/resources/codeList.xml#SV_CouplingType\" "
                + "codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
                + "      </srv:couplingType>\n"
                + "      <srv:containsOperations>\n"
                + "        <srv:SV_OperationMetadata>\n"
                + "          <srv:operationName>\n"
                + "            <gco:CharacterString>GetCapabilities</gco:CharacterString>\n"
                + "          </srv:operationName>\n"
                + "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
                + // Distributed Computing Platform
                "          <srv:connectPoint>\n"
                + "            <gmd:CI_OnlineResource>\n"
                + "              <gmd:linkage>\n"
                + "                <gmd:URL>"
                + XML.encodeAsXML(wmsUrl + "?service=WMS&version=1.3.0&request=GetCapabilities")
                + "</gmd:URL>\n"
                + "              </gmd:linkage>\n"
                + "              <gmd:protocol>\n"
                + "                <gco:CharacterString>OGC:WMS</gco:CharacterString>\n"
                + "              </gmd:protocol>\n"
                + "              <gmd:name>\n"
                + "                <gco:CharacterString>OGC-WMS</gco:CharacterString>\n"
                + "              </gmd:name>\n"
                + "              <gmd:description>\n"
                + "                <gco:CharacterString>Open Geospatial Consortium Web Map Service (WMS)</gco:CharacterString>\n"
                + "              </gmd:description>\n"
                + "              <gmd:function>\n"
                + "                <gmd:CI_OnLineFunctionCode "
                + "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" "
                + "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
                + "              </gmd:function>\n"
                + "            </gmd:CI_OnlineResource>\n"
                + "          </srv:connectPoint>\n"
                + "        </srv:SV_OperationMetadata>\n"
                + "      </srv:containsOperations>\n"
                + "      <srv:operatesOn xlink:href=\"#DataIdentification\"/>\n"
                + "    </srv:SV_ServiceIdentification>\n");
      }

      writer.write("  </gmd:identificationInfo>\n");
    } // end of ii loop

    // contentInfo  (dataVariables)    See Ted Habermann's emails 2012-05-10 and 11.
    String coverageType =
        combinedGlobalAttributes.getString("coverage_content_type"); // used by GOES-R
    String validCoverageTypes[] = { // in 19115-1
      "image",
      "thematicClassification",
      "physicalMeasurement",
      "auxiliaryInformation",
      "qualityInformation",
      "referenceInformation",
      "modelResult"
    };
    if (String2.indexOf(validCoverageTypes, coverageType) < 0)
      coverageType = "physicalMeasurement"; // default
    writer.write(
        "  <gmd:contentInfo>\n"
            + "    <gmi:MI_CoverageDescription>\n"
            + "      <gmd:attributeDescription gco:nilReason=\"unknown\"/>\n"
            + // ???
            // from http://www.schemacentral.com/sc/niem21/t-gco_CodeListValue_Type.html
            "      <gmd:contentType>\n"
            + "        <gmd:MD_CoverageContentTypeCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CoverageContentTypeCode\" "
            + "codeListValue=\""
            + coverageType
            + "\">"
            + coverageType
            + "</gmd:MD_CoverageContentTypeCode>\n"
            + "      </gmd:contentType>\n");

    // dataVariables
    for (EDV edv : dataVariables) {
      String tUnits = testMinimalMetadata ? null : edv.ucumUnits();
      writer.write(
          "      <gmd:dimension>\n"
              + // in ncIso, MHchla var is a dimension???
              "        <gmd:MD_Band>\n"
              + "          <gmd:sequenceIdentifier>\n"
              + "            <gco:MemberName>\n"
              + "              <gco:aName>\n"
              + "                <gco:CharacterString>"
              + XML.encodeAsXML(edv.destinationName())
              + "</gco:CharacterString>\n"
              + "              </gco:aName>\n"
              + "              <gco:attributeType>\n"
              + "                <gco:TypeName>\n"
              + "                  <gco:aName>\n"
              + // e.g., double   (java-style names seem to be okay)
              "                    <gco:CharacterString>"
              + edv.destinationDataType()
              + "</gco:CharacterString>\n"
              + "                  </gco:aName>\n"
              + "                </gco:TypeName>\n"
              + "              </gco:attributeType>\n"
              + "            </gco:MemberName>\n"
              + "          </gmd:sequenceIdentifier>\n"
              + "          <gmd:descriptor>\n"
              + "            <gco:CharacterString>"
              + XML.encodeAsXML(edv.longName())
              + "</gco:CharacterString>\n"
              + "          </gmd:descriptor>\n"
              +

              // ???I think units is used incorrectly, see
              // https://mvnrepository.com/artifact/org.jvnet.ogc/gml-v_3_2_1-schema/1.0.3/iso/19139/20060504/resources/uom/gmxUom.xml
              // which is really complex for derivedUnits.
              (tUnits == null
                  ? ""
                  : "          <gmd:units xlink:href=\"https://unitsofmeasure.org/ucum.html#"
                      + XML.encodeAsHTMLAttribute(SSR.minimalPercentEncode(tUnits))
                      + "\"/>\n")
              + "        </gmd:MD_Band>\n"
              + "      </gmd:dimension>\n");
    }

    writer.write(
        "    </gmi:MI_CoverageDescription>\n"
            + "  </gmd:contentInfo>\n"
            +

            // distibutionInfo: erddap treats distributionInfo same as serviceInfo: use
            // EDStatic.admin...
            //   ncISO uses creator
            "  <gmd:distributionInfo>\n"
            + "    <gmd:MD_Distribution>\n"
            + "      <gmd:distributor>\n"
            + "        <gmd:MD_Distributor>\n"
            + "          <gmd:distributorContact>\n"
            + // ncIso has "missing"
            "            <gmd:CI_ResponsibleParty>\n"
            + "              <gmd:individualName>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminIndividualName)
            + "</gco:CharacterString>\n"
            + "              </gmd:individualName>\n"
            + "              <gmd:organisationName>\n"
            + "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminInstitution)
            + "</gco:CharacterString>\n"
            + "              </gmd:organisationName>\n"
            + "              <gmd:contactInfo>\n"
            + "                <gmd:CI_Contact>\n"
            + "                  <gmd:phone>\n"
            + "                    <gmd:CI_Telephone>\n"
            + "                      <gmd:voice>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminPhone)
            + "</gco:CharacterString>\n"
            + "                      </gmd:voice>\n"
            + "                    </gmd:CI_Telephone>\n"
            + "                  </gmd:phone>\n"
            + "                  <gmd:address>\n"
            + "                    <gmd:CI_Address>\n"
            + "                      <gmd:deliveryPoint>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminAddress)
            + "</gco:CharacterString>\n"
            + "                      </gmd:deliveryPoint>\n"
            + "                      <gmd:city>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminCity)
            + "</gco:CharacterString>\n"
            + "                      </gmd:city>\n"
            + "                      <gmd:administrativeArea>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminStateOrProvince)
            + "</gco:CharacterString>\n"
            + "                      </gmd:administrativeArea>\n"
            + "                      <gmd:postalCode>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminPostalCode)
            + "</gco:CharacterString>\n"
            + "                      </gmd:postalCode>\n"
            + "                      <gmd:country>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminCountry)
            + "</gco:CharacterString>\n"
            + "                      </gmd:country>\n"
            + "                      <gmd:electronicMailAddress>\n"
            + "                        <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.config.adminEmail)
            + "</gco:CharacterString>\n"
            + "                      </gmd:electronicMailAddress>\n"
            + "                    </gmd:CI_Address>\n"
            + "                  </gmd:address>\n"
            + "                </gmd:CI_Contact>\n"
            + "              </gmd:contactInfo>\n"
            + "              <gmd:role>\n"
            + // From list, "distributor" seems best here.
            "                <gmd:CI_RoleCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:CI_RoleCode\" "
            + "codeListValue=\"distributor\">distributor</gmd:CI_RoleCode>\n"
            + "              </gmd:role>\n"
            + "            </gmd:CI_ResponsibleParty>\n"
            + "          </gmd:distributorContact>\n"
            +

            // distributorFormats are formats (says Ted Habermann)
            "          <gmd:distributorFormat>\n"
            + "            <gmd:MD_Format>\n"
            + "              <gmd:name>\n"
            + "                <gco:CharacterString>OPeNDAP</gco:CharacterString>\n"
            + "              </gmd:name>\n"
            + "              <gmd:version>\n"
            + // ncIso has unknown
            "                <gco:CharacterString>"
            + XML.encodeAsXML(EDStatic.dapVersion)
            + "</gco:CharacterString>\n"
            + "              </gmd:version>\n"
            + "            </gmd:MD_Format>\n"
            + "          </gmd:distributorFormat>\n"
            +

            // distributorTransferOptions are URLs  (says Ted Habermann)
            "          <gmd:distributorTransferOptions>\n"
            + "            <gmd:MD_DigitalTransferOptions>\n"
            + "              <gmd:onLine>\n"
            + "                <gmd:CI_OnlineResource>\n"
            + "                  <gmd:linkage>\n"
            + "                    <gmd:URL>"
            + XML.encodeAsXML(datasetUrl)
            + ".html</gmd:URL>\n"
            + "                  </gmd:linkage>\n"
            + "                  <gmd:protocol>\n"
            +
            // see list at
            // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv from
            // John Maurer
            "                    <gco:CharacterString>order</gco:CharacterString>\n"
            + "                  </gmd:protocol>\n"
            + "                  <gmd:name>\n"
            + "                    <gco:CharacterString>Data Subset Form</gco:CharacterString>\n"
            + "                  </gmd:name>\n"
            + "                  <gmd:description>\n"
            + "                    <gco:CharacterString>ERDDAP's version of the OPeNDAP .html web page for this dataset. "
            + "Specify a subset of the dataset and download the data via OPeNDAP "
            + "or in many different file types.</gco:CharacterString>\n"
            + "                  </gmd:description>\n"
            + "                  <gmd:function>\n"
            + "                    <gmd:CI_OnLineFunctionCode "
            + "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" "
            + "codeListValue=\"download\">download</gmd:CI_OnLineFunctionCode>\n"
            + "                  </gmd:function>\n"
            + "                </gmd:CI_OnlineResource>\n"
            + "              </gmd:onLine>\n"
            + "            </gmd:MD_DigitalTransferOptions>\n"
            + "          </gmd:distributorTransferOptions>\n"
            + (accessibleViaMAG().length() > 0
                ? ""
                : // from Ted Habermann's ns01agg.xml
                "          <gmd:distributorTransferOptions>\n"
                    + "            <gmd:MD_DigitalTransferOptions>\n"
                    + "              <gmd:onLine>\n"
                    + "                <gmd:CI_OnlineResource>\n"
                    + "                  <gmd:linkage>\n"
                    + "                    <gmd:URL>"
                    + XML.encodeAsXML(datasetUrl)
                    + ".graph</gmd:URL>\n"
                    + "                  </gmd:linkage>\n"
                    + "                  <gmd:protocol>\n"
                    +
                    // see list at
                    // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv
                    // from John Maurer
                    "                    <gco:CharacterString>order</gco:CharacterString>\n"
                    + "                  </gmd:protocol>\n"
                    + "                  <gmd:name>\n"
                    + "                    <gco:CharacterString>Make-A-Graph Form</gco:CharacterString>\n"
                    + "                  </gmd:name>\n"
                    + "                  <gmd:description>\n"
                    + "                    <gco:CharacterString>ERDDAP's Make-A-Graph .html web page for this dataset. "
                    + "Create an image with a map or graph of a subset of the data.</gco:CharacterString>\n"
                    + "                  </gmd:description>\n"
                    + "                  <gmd:function>\n"
                    + "                    <gmd:CI_OnLineFunctionCode "
                    + "codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_OnLineFunctionCode\" "
                    + "codeListValue=\"mapDigital\">mapDigital</gmd:CI_OnLineFunctionCode>\n"
                    + "                  </gmd:function>\n"
                    + "                </gmd:CI_OnlineResource>\n"
                    + "              </gmd:onLine>\n"
                    + "            </gmd:MD_DigitalTransferOptions>\n"
                    + "          </gmd:distributorTransferOptions>\n")
            + "        </gmd:MD_Distributor>\n"
            + "      </gmd:distributor>\n"
            + "    </gmd:MD_Distribution>\n"
            + "  </gmd:distributionInfo>\n");

    // quality
    if (history != null)
      writer.write(
          "  <gmd:dataQualityInfo>\n"
              + "    <gmd:DQ_DataQuality>\n"
              + "      <gmd:scope>\n"
              + "        <gmd:DQ_Scope>\n"
              + "          <gmd:level>\n"
              + "            <gmd:MD_ScopeCode "
              + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_ScopeCode\" "
              + "codeListValue=\"dataset\">dataset</gmd:MD_ScopeCode>\n"
              + "          </gmd:level>\n"
              + "        </gmd:DQ_Scope>\n"
              + "      </gmd:scope>\n"
              + "      <gmd:lineage>\n"
              + "        <gmd:LI_Lineage>\n"
              + "          <gmd:statement>\n"
              + "            <gco:CharacterString>"
              + XML.encodeAsXML(history)
              + "</gco:CharacterString>\n"
              + "          </gmd:statement>\n"
              + "        </gmd:LI_Lineage>\n"
              + "      </gmd:lineage>\n"
              + "    </gmd:DQ_DataQuality>\n"
              + "  </gmd:dataQualityInfo>\n");

    // metadata
    writer.write(
        "  <gmd:metadataMaintenance>\n"
            + "    <gmd:MD_MaintenanceInformation>\n"
            + "      <gmd:maintenanceAndUpdateFrequency gco:nilReason=\"unknown\"/>\n"
            + "      <gmd:maintenanceNote>\n"
            + "        <gco:CharacterString>This record was created from dataset metadata by ERDDAP Version "
            + EDStatic.erddapVersion
            + "</gco:CharacterString>\n"
            + "      </gmd:maintenanceNote>\n"
            + "    </gmd:MD_MaintenanceInformation>\n"
            + "  </gmd:metadataMaintenance>\n"
            + "</gmi:MI_Metadata>\n");
  }

  /**
   * This looks for missing files by looking for larger-than-expected time gaps in datasets.
   *
   * @param language the index of the selected language
   * @param datasetID The datasetID of a dataset that can be constructed, or the base URL of an
   *     existing ERDDAP dataset that you want to test.
   * @return a newline-separated string (with a trailing newline) with a line for each time gap &gt;
   *     the median gap or gap=NaN. If there are no gaps, this returns "".
   * @throws throwable
   */
  public static String findTimeGaps(int language, String datasetID) throws Throwable {

    DoubleArray times;
    if (String2.isUrl(datasetID)) {
      Table table = new Table();
      table.readASCII(
          datasetID,
          SSR.getBufferedUrlReader(datasetID + ".csvp?time"),
          "",
          "",
          0,
          1,
          ",",
          null,
          null,
          null,
          null,
          false);
      PrimitiveArray isoPA = table.getColumn(0);
      int n = isoPA.size();
      times = new DoubleArray(n, false);
      for (int i = 0; i < n; i++) times.add(Calendar2.isoStringToEpochSeconds(isoPA.getString(i)));

    } else {
      EDD edd = oneFromDatasetsXml(null, datasetID);
      if (edd instanceof EDDTable)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                + "datasetID="
                + datasetID
                + " isn't an EDDGrid dataset.");
      EDDGrid eddGrid = (EDDGrid) edd;
      if (eddGrid.timeIndex() < 0)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, EDStatic.messages.queryErrorAr)
                + "datasetID="
                + datasetID
                + " has no time variable.");
      PrimitiveArray pa = eddGrid.axisVariables[eddGrid.timeIndex].destinationValues();
      times =
          pa instanceof DoubleArray da
              ? da
              : // should be
              new DoubleArray(pa);
    }
    return times.findTimeGaps();
  }

  /** This returns time gap information. */
  public String findTimeGaps() {
    if (timeIndex < 0)
      return """
              Time gaps: (none, because there is no time axis variable)
              nGaps=0
              """;

    PrimitiveArray pa = axisVariables[timeIndex].destinationValues();
    DoubleArray times =
        pa instanceof DoubleArray da
            ? da
            : // should be
            new DoubleArray(pa);
    return times.findTimeGaps();
  }
}
