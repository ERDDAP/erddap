/*
 * EDDTable Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.LongArray;
import com.cohort.array.PAOne;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.ULongArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.Script2;
import com.cohort.util.ScriptRow;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.Units2;
import com.cohort.util.XML;
import com.google.common.collect.ImmutableList;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.GraphDataLayer;
import gov.noaa.pfel.coastwatch.sgt.SgtMap;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.erddap.dataset.metadata.LocalizedAttributes;
import gov.noaa.pfel.erddap.dataset.metadata.MetadataBuilder;
import gov.noaa.pfel.erddap.filetypes.DapRequestInfo;
import gov.noaa.pfel.erddap.filetypes.FileTypeInterface;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.util.EDMessages.Message;
import gov.noaa.pfel.erddap.variable.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBException;
import java.awt.Color;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.opengis.metadata.Metadata;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * This class represents a dataset where the results can be presented as a table.
 *
 * <p>There is no saveAsNCML() because NCML's data model is dimensions and multidimensional
 * variables. But tabular dimensions have no "row" dimension (isUnlimited="true") used by the
 * dataVariables. To say so would invite software to query it that way. OR: NCML is a convenient and
 * reasonable way to represent the data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-06-08
 */
public abstract class EDDTable extends EDD {

  public static final String dapProtocol = "tabledap";
  public static final String SEQUENCE_NAME = Table.SEQUENCE_NAME; // same for all datasets

  /**
   * This is a list of all operator symbols (for my convenience in parseUserDapQuery: 2 letter ops
   * are first). Note that all variables (numeric or String) can be constrained via a ERDDAP
   * constraint using any of these operators. If the source can't support the operator (e.g., no
   * source supports numeric =~ testing, and no source supports string &lt; &lt;= &gt; &gt;=
   * testing), ERDDAP will just get all the relevant data and do the test itself.
   */
  public static final ImmutableList<String> OPERATORS = Table.OPERATORS;

  // EDDTableFromFiles.isOK relies on this order
  // "!=", PrimitiveArray.REGEX_OP, "<=", ">=",
  // "=", "<", ">"};
  /** Partially Percent Encoded Operators ('=' is not encoded) */
  public static final ImmutableList<String> PPE_OPERATORS =
      ImmutableList.of(
          // EDDTableFromFiles.isOK relies on this order
          "!=", PrimitiveArray.REGEX_OP, "%3C=", "%3E=", "=", "%3C", "%3E");

  private static final int opDefaults[] = {OPERATORS.indexOf(">="), OPERATORS.indexOf("<=")};

  /**
   * These parallel OPERATORS, but &gt; and &lt; operators are reversed. These are used when
   * converting altitude constraints to depth constraints.
   */
  public static final ImmutableList<String> REVERSED_OPERATORS =
      ImmutableList.of("!=", PrimitiveArray.REGEX_OP, ">=", "<=", "=", ">", "<");

  /**
   * A list of the greater-than less-than operators, which some sources can't handle for strings.
   */
  public static final ImmutableList<String> GTLT_OPERATORS = ImmutableList.of("<=", ">=", "<", ">");

  /** The index of REGEX_OP in OPERATORS. */
  public static final int REGEX_OP_INDEX = OPERATORS.indexOf(PrimitiveArray.REGEX_OP);

  /**
   * The orderBy options. The order/positions may change as new ones are added. No codes depends on
   * the specific order/positions, except [0]="".
   */
  public static final ImmutableList<String> orderByOptions =
      ImmutableList.of(
          "",
          "orderBy",
          "orderByDescending",
          "orderByClosest",
          "orderByCount",
          "orderByLimit",
          "orderByMax",
          "orderByMin",
          "orderByMinMax",
          "orderByMean",
          "orderBySum");

  public static final String DEFAULT_ORDERBYLIMIT = "100";

  /** These are used on web pages when a user changes orderBy. They parallel the orderByOptions. */
  public static final ImmutableList<String> orderByExtraDefaults =
      ImmutableList.of("", "", "", "", "", DEFAULT_ORDERBYLIMIT, "", "", "", "", "");

  /**
   * This is the minimum number of orderBy variables that must be specified (not counting the
   * orderByExtra item).
   */
  public static final ImmutableList<Byte> minOrderByVariables =
      ImmutableList.of(
          (byte) 0, (byte) 1, (byte) 1, (byte) 1, (byte) 0, (byte) 0, (byte) 1, (byte) 1, (byte) 1,
          (byte) 0, (byte) 0);

  /** This is used in many file types as the row identifier. */
  public static final String ROW_NAME = "row"; // see also Table.ROW_NAME

  private static final List<String> publicGraphFileTypeNames;
  private static final String defaultFileTypeOption;
  private static final String defaultPublicGraphFileTypeOption;

  /** SOS static values. */
  public static final String sosServer = "server";

  public static final String sosVersion = "1.0.0";
  public static final String sosNetworkOfferingType = "network";

  /** sosDSOutputFormat is for DescribeSensor. Note that it needs to be XML.encodeAsXML in a url. */
  public static final String sosDSOutputFormat = "text/xml;subtype=\"sensorML/1.0.1\"";

  public static final String sosPhenomenaDictionaryUrl = "phenomenaDictionary.xml";

  /** These are the mime types allowed for SOS requestFormat. */
  public static final int sosDefaultDataResponseFormat = 13; // tsv

  public static final int sosOostethysDataResponseFormat = 2;
  public static final ImmutableList<String> sosDataResponseFormats =
      ImmutableList.of(
          "text/xml;schema=\"ioos/0.6.1\"", // two names for the same IOOS SOS XML format
          "application/ioos+xml;version=0.6.1", // "ioos" in the names is detected by
          // OutputStreamFromHttpResponse
          "text/xml; subtype=\"om/1.0.0\"", // Oostethys-style  pre 2010-04-30 was
          // application/com-xml
          "text/csv",
          "application/json;subtype=geojson",
          "text/html", // no way to distinguish an html table vs html document in general
          "application/json",
          "application/x-matlab-mat", // ??? made up, x=nonstandard
          "application/x-netcdf", // see https://www.wussu.com/various/mimetype.htm
          "application/x-netcdf", // ncCF, see https://www.wussu.com/various/mimetype.htm
          "application/x-netcdf", // ncCFMA
          "application/json", // ncoJson
          "text/plain", // odv
          "text/tab-separated-values",
          "application/xhtml+xml");

  /** These are the corresponding tabledap dataFileTypeNames, not the final file extensions. */
  public static final ImmutableList<String> sosTabledapDataResponseTypes =
      ImmutableList.of(
          // *** ADDING A FORMAT? Add support for it in sosGetObservation().
          // see isIoosSosXmlResponseFormat()
          "text/xml;schema=\"ioos/0.6.1\"", // needed by OutputStreamFromHttpResponse
          "application/ioos+xml;version=0.6.1", // needed by OutputStreamFromHttpResponse
          // see isOosethysXmlResponseFormat()
          "text/xml; subtype=\"om/1.0.0\"", // Oostethys-style //checked for by
          // OutputStreamFromHttpResponse, was application/com-xml
          ".csvp",
          ".geoJson",
          ".htmlTable",
          ".json",
          ".mat",
          ".nc",
          ".ncCF",
          ".ncCFMA",
          ".ncoJson",
          ".odvTxt",
          ".tsvp",
          ".xhtml");

  // ** And there are analogous arrays for image formats (enable these when variables can be
  // accessed separately)
  public static final ImmutableList<String> sosImageResponseFormats =
      ImmutableList.of(OutputStreamFromHttpResponse.KML_MIME_TYPE, "application/pdf", "image/png");
  public static final ImmutableList<String> sosTabledapImageResponseTypes =
      ImmutableList.of(".kml", ".pdf", ".png");

  protected Table minMaxTable;

  /**
   * minMaxTable has a col for each dv; row0=min for all files, row1=max for all files. The values
   * are straight from the source; scale_factor and add_offset haven't been applied. Even time is
   * stored as raw source values; see "//EEEK!!!" in EDDTableFromFiles.
   *
   * <p>Currently, only EDDTableFromFiles returns a table. For other subclasses, this will be null.
   * Just read from this. Don't change the values.
   */
  public Table minMaxTable() {
    return minMaxTable;
  }

  /**
   * When inactive, these will be null. addVariablesWhereAttValues parallels
   * addVariablesWhereAttNames.
   */
  protected StringArray addVariablesWhereAttNames = null; // [0] is always ""

  protected ArrayList<StringArray> addVariablesWhereAttValues =
      null; // [0] in each StringArray is always ""

  // static constructor
  static {
    int nSDRF = sosDataResponseFormats.size();
    Test.ensureEqual(
        nSDRF,
        sosTabledapDataResponseTypes.size(),
        "'sosDataResponseFormats.length' not equal to 'sosTabledapDataResponseTypes.length'.");
    defaultFileTypeOption = ".htmlTable";

    List<EDDFileTypeInfo> imageTypes = EDD.getFileTypeOptions(false, true);

    publicGraphFileTypeNames = new ArrayList<String>();
    publicGraphFileTypeNames.add(".das");
    publicGraphFileTypeNames.add(".dds");
    publicGraphFileTypeNames.add(".fgdc");
    publicGraphFileTypeNames.add(".graph");
    publicGraphFileTypeNames.add(".help");
    publicGraphFileTypeNames.add(".iso19115");

    // construct allFileTypeOptionsAr

    for (int i = 0; i < imageTypes.size(); i++) {
      publicGraphFileTypeNames.add(imageTypes.get(i).getFileTypeName());
    }

    graphsAccessibleTo_fileTypeNames = new HashSet<>(); // read only, so needn't be thread-safe
    graphsAccessibleTo_fileTypeNames.addAll(publicGraphFileTypeNames);
    defaultPublicGraphFileTypeOption = ".png";

    Test.ensureEqual(
        sosDataResponseFormats.size(),
        sosTabledapDataResponseTypes.size(),
        "'sosDataResponseFormats.length' not equal to 'sosTabledapDataResponseTypes.length'.");
    Test.ensureEqual(
        sosImageResponseFormats.size(),
        sosTabledapImageResponseTypes.size(),
        "'sosImageResponseFormats.length' not equal to 'sosTabledapImageResponseTypes.length'.");
  }

  // *********** end of static declarations ***************************

  /**
   * These specifies which variables the source can constrain (either via constraints in the query
   * or in the getSourceData method). <br>
   * For example, DAPPER lets you constrain the lon,lat,depth,time but not dataVariables
   * (https://www.pmel.noaa.gov/epic/software/dapper/dapperdocs/conventions/ and see the Constraints
   * heading). <br>
   * It is presumed that no source can handle regex's for numeric variables.
   *
   * <p>CONSTRAIN_PARTIAL indicates the constraint will be partly handled by getSourceData and fully
   * handled by standarizeResultsTable. The constructor must set these.
   */
  public static final int CONSTRAIN_NO = 0, CONSTRAIN_PARTIAL = 1, CONSTRAIN_YES = 2;

  public static final ImmutableList<String> CONSTRAIN_NO_PARTIAL_YES =
      ImmutableList.of("no", "partial", "yes");
  protected int sourceCanConstrainNumericData = -1; // not set
  protected int sourceCanConstrainStringData = -1; // not set

  /**
   * Some sources don't handle floating point =, !=, &gt;=, &lt;= correctly and need these
   * constraints expanded to &gt;= and/or &lt;= +/-0.001 (fudge).
   */
  protected boolean sourceNeedsExpandedFP_EQ = false; // but some subclasses default to true

  /**
   * Some sources can constrain String regexes (use =~ or ~=); some can't (use "").
   * standardizeResultsTable always (re)tests regex constraints (so =~ here acts like PARTIAL). See
   * PrimitiveArray.REGEX_OP.
   */
  protected String sourceCanConstrainStringRegex = "";

  protected int sourceCanOrderBy = CONSTRAIN_NO; // an exception is databases, if admin says so
  protected int sourceCanDoDistinct = CONSTRAIN_NO; // an exception is databases, if admin says so

  /**
   * Given a constrain string (no, partial, or yes), this returns the appropriate CONSTRAIN integer,
   * or -1 if null, "" or no match.
   */
  public static int getNoPartialYes(String s) {
    s = String2.isSomething(s) ? s.trim().toLowerCase() : "";
    return CONSTRAIN_NO_PARTIAL_YES.indexOf(s);
  }

  /**
   * If present, these dataVariables[xxxIndex] values are set by the constructor. alt and depth
   * mustn't both be active.
   */
  protected int lonIndex = -1, latIndex = -1, altIndex = -1, depthIndex = -1, timeIndex = -1;

  protected int profile_idIndex = -1,
      timeseries_idIndex = -1,
      trajectory_idIndex = -1; // see accessibleViaNcCF
  protected String requiredCfRequestVariables[]; // each data request must include all of these
  // a list of all profile|timeseries|trajectory variables
  // each data request must include at least one variable *not* on this list
  protected String outerCfVariables[];

  /**
   * The list of dataVariable destinationNames for use by .subset and SUBSET_FILENAME (or null if
   * not yet set up or String[0] if unused).
   */
  private String subsetVariables[] = null;

  public static final String DEFAULT_SUBSET_VIEWS = // viewDistinctData will default to 1000
      "&.viewDistinctMap=true";

  /**
   * EDDTable.subsetVariables relies on this name ending to know which cached subset files to delete
   * when a dataset is reloaded.
   */
  public static final String SUBSET_FILENAME = "subset.nc";

  public static final String DISTINCT_SUBSET_FILENAME = "distinct.nc";

  /**
   * These are parallel data structures for use by setSosOfferingTypeAndIndex(). If cdmDataType is
   * sosCdmDataTypes[t], then sosOfferingType is sosOfferingTypes[t] and sosOfferingIndex is var
   * with cf_role=sosCdmCfRoles[t], !!!FOR NOW, cdm_data_type must be TimeSeries and so
   * sosOfferingType must be Station (?Check with Derrick)
   */
  public static final ImmutableList<String> sosCdmDataTypes = ImmutableList.of("TimeSeries");

  public static final ImmutableList<String> sosCdmOfferingTypes = ImmutableList.of("Station");
  public static final ImmutableList<String> sosCdmCfRoles = ImmutableList.of("timeseries_id");

  /**
   * This is usually set by setSosOfferingTypeAndIndex(). This will be null if the dataset is not
   * available via ERDDAP's SOS server.
   */
  public String sosOfferingType = null;

  /**
   * The index of the dataVariable with the sosOffering outer var (e.g. with cf_role=timeseries_id).
   * This is usually set by setSosOfferingTypeAndIndex(). This will be -1 if the dataset is not
   * available via ERDDAP's SOS server.
   */
  public int sosOfferingIndex = -1;

  /**
   * To make a dataset accessibleViaSOS, the constructor must set these parallel PrimitiveArrays
   * with the relevant *destination* values for each offering (e.g., timeseries(station),
   * trajectory, or profile).
   *
   * <ul>
   *   <li>These will be null if the dataset is not available via ERDDAP's SOS server.
   *   <li>EDDTable subclasses that have all this info available (e.g., EDDTableFromSOS) should set
   *       these right before ensureValid(). ensureValid() also tries to set these for other
   *       datasets (without trying to get min/max time) if the information is available from
   *       subsetVariablesDataTable.
   *   <li>sosOfferings has the original/short offering names, e.g., station names. See also
   *       sosOfferingPrefix below. It is almost always a StringArray.
   *   <li>MinTime and MaxTime are epochSeconds and can have NaNs. They are almost always
   *       DoubleArrays.
   *   <li>min/max/Lon/Lat can't have NaNs. They are almost always DoubleArrays or FloatArrays.
   *   <li>Consumers of the information shouldn't change the values.
   * </ul>
   */
  public PrimitiveArray sosOfferings,
      sosMinTime,
      sosMaxTime,
      sosMinLat,
      sosMaxLat,
      sosMinLon,
      sosMaxLon;

  /**
   * sosOfferingPrefix (e.g., "urn:ioos:station:NOAA.NOS.CO-OPS:") is used to convert the short
   * sosOfferings (e.g., station) name (e.g., 1612340) name into the long name (e.g.,
   * urn:ioos:station:NOAA.NOS.CO-OPS:1612340).
   */
  public String sosOfferingPrefix;

  /**
   * This tries to set sosOfferingType and sosOfferingIndex and returns true if successful. Use this
   * near end of constructor -- it needs combinedAttributes and dataVariables[].
   *
   * @return true if sosOfferingType and sosOfferingIndex were set.
   */
  public boolean setSosOfferingTypeAndIndex() {
    // Is the cdm_data_type one of the compatible types?
    String cdmType =
        combinedGlobalAttributes().getString(EDMessages.DEFAULT_LANGUAGE, "cdm_data_type");
    int type = sosCdmDataTypes.indexOf(cdmType);
    if (type < 0) {
      if (debugMode)
        String2.log(
            "setSosOfferingTypeAndIndex=false.  cdm_data_type=" + cdmType + " is incompatible.");
      return false;
    }
    sosOfferingType = sosCdmOfferingTypes.get(type);

    // look for the var with the right cf_role
    String sosCdmCfRole = sosCdmCfRoles.get(type);
    sosOfferingIndex = -1;
    for (int dv = 0; dv < dataVariables.length; dv++) {
      if (sosCdmCfRole.equals(
          dataVariables[dv]
              .combinedAttributes()
              .getString(EDMessages.DEFAULT_LANGUAGE, "cf_role"))) {
        sosOfferingIndex = dv;
        break;
      }
    }
    if (sosOfferingIndex >= 0) {
      if (debugMode) String2.log("setSosOfferingTypeAndIndex=true");
      return true;
    } else {
      sosOfferingType = null;
      if (debugMode)
        String2.log("setSosOfferingTypeAndIndex=false.  No var has cf_role=" + sosCdmCfRole);
      return false;
    }
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
    return EDStatic.messages.get(Message.EDD_TABLE_DAP_DESCRIPTION, language);
  }

  public String[] requiredCfRequestVariables() {
    if (requiredCfRequestVariables == null) {
      accessibleViaNcCF();
    }
    return requiredCfRequestVariables;
  }

  public String[] outerCfVariables() {
    if (outerCfVariables == null) {
      accessibleViaNcCF();
    }
    return outerCfVariables;
  }

  /**
   * This returns the longDapDescription
   *
   * @param language the index of the selected language
   * @return the longDapDescription
   */
  public static String longDapDescription(int language, String tErddapUrl) {
    return String2.replaceAll(
        EDStatic.messages.get(Message.EDD_TABLE_DAP_LONG_DESCRIPTION, language),
        "&erddapUrl;",
        tErddapUrl);
  }

  /**
   * EDDTable constructors call this after dataVariables are constructed and right before
   * ensureValid to process an addVariablesWhere csv list of attNames.
   *
   * @param tAddVariablesWhere a CSV list of attribute names.
   */
  protected void makeAddVariablesWhereAttNamesAndValues(String tAddVariablesWhere) {
    addVariablesWhereAttNames = null; // not necessary, but be safe
    addVariablesWhereAttValues = null; // not necessary, but be safe
    if (String2.isSomething(tAddVariablesWhere)) {
      int ndv = dataVariables.length;
      String attNames[] = StringArray.arrayFromCSV(tAddVariablesWhere);
      for (String attName : attNames) {
        HashSet<String> validAttValues = new HashSet<>();
        for (int dv = 0; dv < ndv; dv++) {
          String ts =
              dataVariables[dv]
                  .combinedAttributes()
                  .getString(EDMessages.DEFAULT_LANGUAGE, attName);
          if (String2.isSomething(ts)) validAttValues.add(ts);
        }
        if (!validAttValues.isEmpty()) {
          if (addVariablesWhereAttNames == null) {
            addVariablesWhereAttNames =
                new StringArray(new String[] {""}); // with "" initial option
            addVariablesWhereAttValues = new ArrayList<>();
            addVariablesWhereAttValues.add(new StringArray(new String[] {""}));
          }
          validAttValues.add("");
          StringArray tValues = new StringArray(validAttValues.iterator());
          tValues.sortIgnoreCase();
          addVariablesWhereAttNames.add(attName);
          addVariablesWhereAttValues.add(tValues);
        } else {
          String2.log(
              String2.WARNING
                  + " when creating addVariablesWhere hashmap: no variables have attName="
                  + attName);
        }
      }
    }
  }

  /**
   * This MUST be used by all subclass constructors to ensure that all of the items common to all
   * EDDTables are properly set. This also does some actual work.
   *
   * @throws Throwable if any required item isn't properly set
   */
  @Override
  public void ensureValid() throws Throwable {
    super.ensureValid();

    int language = EDMessages.DEFAULT_LANGUAGE;
    String errorInMethod =
        "datasets.xml/EDDTable.ensureValid error for datasetID=" + datasetID + ":\n ";

    HashSet<String> sourceNamesHS = new HashSet<>(2 * dataVariables.length);
    HashSet<String> destNamesHS = new HashSet<>(2 * dataVariables.length);
    for (EDV dataVariable : dataVariables) {
      // ensure unique sourceNames
      String sn = dataVariable.sourceName();
      if (!sn.startsWith("=")) {
        if (!sourceNamesHS.add(sn))
          throw new RuntimeException(
              errorInMethod + "Two dataVariables have the same sourceName=" + sn + ".");
      }

      // ensure unique destNames
      String dn = dataVariable.destinationName();
      if (!destNamesHS.add(dn))
        throw new RuntimeException(
            errorInMethod + "Two dataVariables have the same destinationName=" + dn + ".");

      // standard_name is the only case-sensitive CF attribute name (see Sec 3.3)
      // All are all lower case.
      LocalizedAttributes tAtts = dataVariable.combinedAttributes();
      String tStandardName = tAtts.getString(language, "standard_name");
      if (String2.isSomething(tStandardName))
        tAtts.set(language, "standard_name", tStandardName.toLowerCase());
    }

    Test.ensureTrue(
        lonIndex < 0 || dataVariables[lonIndex] instanceof EDVLon,
        errorInMethod + "dataVariable[lonIndex=" + lonIndex + "] isn't an EDVLon.");
    Test.ensureTrue(
        latIndex < 0 || dataVariables[latIndex] instanceof EDVLat,
        errorInMethod + "dataVariable[latIndex=" + latIndex + "] isn't an EDVLat.");
    Test.ensureTrue(
        altIndex < 0 || dataVariables[altIndex] instanceof EDVAlt,
        errorInMethod + "dataVariable[altIndex=" + altIndex + "] isn't an EDVAlt.");
    Test.ensureTrue(
        depthIndex < 0 || dataVariables[depthIndex] instanceof EDVDepth,
        errorInMethod + "dataVariable[depthIndex=" + depthIndex + "] isn't an EDVDepth.");
    // some places depend on not having alt *and* depth
    Test.ensureTrue(
        altIndex <= 0 || depthIndex <= 0,
        errorInMethod + "The dataset has both an altitude and a depth variable.");
    Test.ensureTrue(
        timeIndex < 0 || dataVariables[timeIndex] instanceof EDVTime,
        errorInMethod + "dataVariable[timeIndex=" + timeIndex + "] isn't an EDVTime.");

    Test.ensureTrue(
        sourceCanConstrainNumericData >= 0 && sourceCanConstrainNumericData <= 2,
        errorInMethod
            + "sourceCanConstrainNumericData="
            + sourceCanConstrainNumericData
            + " must be 0, 1, or 2.");
    Test.ensureTrue(
        sourceCanConstrainStringData >= 0 && sourceCanConstrainStringData <= 2,
        errorInMethod
            + "sourceCanConstrainStringData="
            + sourceCanConstrainStringData
            + " must be 0, 1, or 2.");
    Test.ensureTrue(
        sourceCanConstrainStringRegex.equals("=~")
            || sourceCanConstrainStringRegex.equals("~=")
            || sourceCanConstrainStringRegex.isEmpty(),
        errorInMethod
            + "sourceCanConstrainStringRegex=\""
            + sourceCanConstrainStringData
            + "\" must be \"=~\", \"~=\", or \"\".");

    // *Before* add standard metadata below...
    // Make subsetVariablesDataTable and distinctSubsetVariablesDataTable(loggedInAs=null).
    // if accessibleViaSubset() and dataset is available to public.
    // This isn't required (ERDDAP was/could be lazy), but doing it makes initial access fast
    //  and makes it thread-safe (always done in constructor's thread).
    if (accessibleViaSubset().length() == 0 && accessibleTo == null) {

      // If this throws exception, dataset initialization will fail.  That seems fair.
      Table table = distinctSubsetVariablesDataTable(0, null, null);
      // it calls subsetVariablesDataTable(0, null);

      // now go back and set destinationMin/Max
      // see EDDTableFromDap.testSubsetVariablesRange()
      int nCol = table.nColumns();
      for (int col = 0; col < nCol; col++) {
        String colName = table.getColumnName(col);
        int which = String2.indexOf(dataVariableDestinationNames(), colName);
        if (which < 0)
          throw new SimpleException(
              String2.ERROR
                  + ": column="
                  + colName
                  + " in subsetVariables isn't in dataVariables=\""
                  + String2.toCSSVString(dataVariableDestinationNames())
                  + "\".");
        EDV edv = dataVariables[which];
        PrimitiveArray pa = table.getColumn(col);
        // note that time is stored as doubles in distinctValues table
        // String2.log("distinct col=" + colName + " " + pa.elementTypeString());
        if (pa instanceof StringArray) {
          continue;
        }
        // If the destination has max is MV, this should also have it, otherwise the
        // calculated max value will be wrong.
        if (edv.destinationMaxIsMV()) {
          pa.setMaxIsMV(true);
        }
        int nMinMax[] = pa.getNMinMaxIndex();
        if (nMinMax[0] == 0) continue;
        edv.setDestinationMinMax(new PAOne(pa, nMinMax[1]), new PAOne(pa, nMinMax[2]));
        edv.setActualRangeFromDestinationMinMax(language);
      }
    }

    // add standard metadata to combinedGlobalAttributes
    // (This should always be done, so shouldn't be in an optional method...)
    // lon
    // String2.log(">> lonIndex=" + lonIndex);
    if (lonIndex >= 0) {
      combinedGlobalAttributes.set(language, "geospatial_lon_units", EDV.LON_UNITS);
      PrimitiveArray pa =
          dataVariables[lonIndex].combinedAttributes().get(language, "actual_range");
      if (pa != null) {
        combinedGlobalAttributes.set(language, "geospatial_lon_min", pa.getNiceDouble(0));
        combinedGlobalAttributes.set(language, "geospatial_lon_max", pa.getNiceDouble(1));
        combinedGlobalAttributes.set(language, "Westernmost_Easting", pa.getNiceDouble(0));
        combinedGlobalAttributes.set(language, "Easternmost_Easting", pa.getNiceDouble(1));
      }
    }

    // lat
    if (latIndex >= 0) {
      combinedGlobalAttributes.set(language, "geospatial_lat_units", EDV.LAT_UNITS);
      PrimitiveArray pa =
          dataVariables[latIndex].combinedAttributes().get(language, "actual_range");
      if (pa != null) {
        combinedGlobalAttributes.set(language, "geospatial_lat_min", pa.getNiceDouble(0));
        combinedGlobalAttributes.set(language, "geospatial_lat_max", pa.getNiceDouble(1));
        combinedGlobalAttributes.set(language, "Southernmost_Northing", pa.getNiceDouble(0));
        combinedGlobalAttributes.set(language, "Northernmost_Northing", pa.getNiceDouble(1));
      }
    }

    // alt
    if (altIndex >= 0) {
      combinedGlobalAttributes.set(language, "geospatial_vertical_positive", "up");
      combinedGlobalAttributes.set(language, "geospatial_vertical_units", EDV.ALT_UNITS);
      PrimitiveArray pa =
          dataVariables[altIndex].combinedAttributes().get(language, "actual_range");
      if (pa != null) {
        combinedGlobalAttributes.set(language, "geospatial_vertical_min", pa.getNiceDouble(0));
        combinedGlobalAttributes.set(language, "geospatial_vertical_max", pa.getNiceDouble(1));
      }
    } else if (depthIndex >= 0) {
      combinedGlobalAttributes.set(language, "geospatial_vertical_positive", "down");
      combinedGlobalAttributes.set(language, "geospatial_vertical_units", EDV.DEPTH_UNITS);
      PrimitiveArray pa =
          dataVariables[depthIndex].combinedAttributes().get(language, "actual_range");
      if (pa != null) {
        combinedGlobalAttributes.set(language, "geospatial_vertical_min", pa.getNiceDouble(0));
        combinedGlobalAttributes.set(language, "geospatial_vertical_max", pa.getNiceDouble(1));
      }
    }

    // time
    if (timeIndex >= 0) {
      LocalizedAttributes catts = dataVariables[timeIndex].combinedAttributes();
      PrimitiveArray pa = catts.get(language, "actual_range");
      if (pa != null) {
        String tp = catts.getString(language, EDV.TIME_PRECISION);
        // "" unsets the attribute if min or max isNaN
        combinedGlobalAttributes.set(
            language,
            "time_coverage_start",
            Calendar2.epochSecondsToLimitedIsoStringT(tp, pa.getDouble(0), ""));
        // for tables (not grids) will be NaN for 'present'.   Deal with this better???
        combinedGlobalAttributes.set(
            language,
            "time_coverage_end",
            Calendar2.epochSecondsToLimitedIsoStringT(tp, pa.getDouble(1), ""));
      }
    }

    // set featureType from cdm_data_type (if it is a type ERDDAP supports)
    String cdmType = combinedGlobalAttributes.getString(language, "cdm_data_type");
    if (cdmType != null) {
    } else if (CDM_POINT.equals(cdmType)
        || CDM_PROFILE.equals(cdmType)
        || CDM_TRAJECTORY.equals(cdmType)
        || CDM_TRAJECTORYPROFILE.equals(cdmType)
        || CDM_TIMESERIES.equals(cdmType)
        || CDM_TIMESERIESPROFILE.equals(cdmType))
      combinedGlobalAttributes.set(
          language, "featureType", cdmType.substring(0, 1).toLowerCase() + cdmType.substring(1));

    // This either completes the subclasses' SOS setup, or does a genericSosSetup,
    // or fails to do the setup.
    // This sets accessibleViaSOS "" (if accessible) or to some error message.
    // This MUST be done AFTER subsetVariablesDataTable has been created.
    accessibleViaSOS = makeAccessibleViaSOS();

    // last: uses time_coverage metadata
    // make FGDC and ISO19115
    accessibleViaFGDC = null; // should be null already, but make sure so files will be created now
    accessibleViaISO19115 = null;
    accessibleViaFGDC();
    accessibleViaISO19115();

    // really last: it uses accessibleViaFGDC and accessibleViaISO19115
    // make searchString  (since should have all finished/correct metadata)
    // This makes creation of searchString thread-safe (always done in constructor's thread).
    searchString(language);
  }

  /**
   * The index of the longitude variable (-1 if not present).
   *
   * @return the index of the longitude variable (-1 if not present)
   */
  public int lonIndex() {
    return lonIndex;
  }

  /**
   * The index of the latitude variable (-1 if not present).
   *
   * @return the index of the latitude variable (-1 if not present)
   */
  public int latIndex() {
    return latIndex;
  }

  /**
   * The index of the altitude variable (-1 if not present).
   *
   * @return the index of the altitude variable (-1 if not present)
   */
  public int altIndex() {
    return altIndex;
  }

  /**
   * The index of the depth variable (-1 if not present).
   *
   * @return the index of the depth variable (-1 if not present)
   */
  public int depthIndex() {
    return depthIndex;
  }

  /**
   * The index of the time variable (-1 if not present).
   *
   * @return the index of the time variable (-1 if not present)
   */
  public int timeIndex() {
    return timeIndex;
  }

  /**
   * The string representation of this tableDataSet (for diagnostic purposes).
   *
   * @return the string representation of this tableDataSet.
   */
  @Override
  public String toString() {
    // make this JSON format?
    return "//** EDDTable "
        + super.toString()
        + "\nsourceCanConstrainNumericData="
        + sourceCanConstrainNumericData
        + "\nsourceCanConstrainStringData="
        + sourceCanConstrainStringData
        + "\nsourceCanConstrainStringRegex=\""
        + sourceCanConstrainStringRegex
        + "\""
        + "\n\\**\n\n";
  }

  /**
   * This makes the searchString (mixed case) used to create searchBytes or searchDocument.
   *
   * @return the searchString (mixed case) used to create searchBytes or searchDocument.
   */
  @Override
  public String searchString(int language) {

    StringBuilder sb = startOfSearchString(language);

    String2.replaceAll(sb, "\"", ""); // no double quotes (esp around attribute values)
    String2.replaceAll(sb, "\n    ", "\n"); // occurs for all attributes
    return sb.toString();
  }

  /**
   * This returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles)
   * or false if it doesn't (e.g., EDDTableFromDatabase).
   *
   * @returns true if this EDDTable knows each variable's actual_range (e.g., EDDTableFromFiles) or
   *     false if it doesn't (e.g., EDDTableFromDatabase).
   */
  public abstract boolean knowsActualRange();

  /**
   * This returns 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or 2=CONSTRAIN_YES (completely),
   * indicating if the source can handle all non-regex constraints on numericDataVariables (either
   * via constraints in the query or in the getSourceData method).
   *
   * <p>CONSTRAIN_NO indicates that sourceQueryFromDapQuery should remove these constraints, because
   * the source doesn't even want to see them. <br>
   * CONSTRAIN_YES indicates that standardizeResultsTable needn't do the tests for these
   * constraints.
   *
   * <p>Note that CONSTRAIN_PARTIAL is very useful. It says that the source would like to see all of
   * the constraints (e.g., from sourceQueryFromDapQuery) so that it can then further prune the
   * constraints as needed and use them or not. Then, standardizeResultsTable will test all of the
   * constraints. It is a flexible and reliable approach.
   *
   * <p>It is assumed that no source can handle regex for numeric data, so this says nothing about
   * regex.
   *
   * @return 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or 2=CONSTRAIN_YES (completely)
   *     indicating if the source can handle all non-regex constraints on numericDataVariables
   *     (ignoring regex, which it is assumed none can handle).
   */
  public int sourceCanConstrainNumericData() {
    return sourceCanConstrainNumericData;
  }

  /**
   * This returns 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or 2=CONSTRAIN_YES (completely),
   * indicating if the source can handle all non-regex constraints on stringDataVariables (either
   * via constraints in the query or in the getSourceData method).
   *
   * <p>CONSTRAIN_NO indicates that sourceQueryFromDapQuery should remove these constraints, because
   * the source doesn't even want to see them. <br>
   * CONSTRAIN_YES indicates that standardizeResultsTable needn't do the tests for these
   * constraints.
   *
   * <p>Note that CONSTRAIN_PARTIAL is very useful. It says that the source would like to see all of
   * the constraints (e.g., from sourceQueryFromDapQuery) so that it can then further prune the
   * constraints as needed and use them or not. Then, standardizeResultsTable will test all of the
   * constraints. It is a flexible and reliable approach.
   *
   * <p>PARTIAL and YES say nothing about regex; see sourceCanConstrainStringRegex. So this=YES +
   * sourceCanConstrainStringReges="" is a valid setup.
   *
   * @return 0=CONSTRAIN_NO (not at all), 1=CONSTRAIN_PARTIAL, or 2=CONSTRAIN_YES (completely)
   *     indicating if the source can handle all non-regex constraints on stringDataVariables.
   */
  public int sourceCanConstrainStringData() {
    return sourceCanConstrainStringData;
  }

  /**
   * This returns "=~" or "~=" (indicating the operator that the source uses) if the source can
   * handle some source regex constraints, or "" if it can't. <br>
   * sourceCanConstrainStringData must be PARTIAL or YES if this isn't "". <br>
   * If this isn't "", the implication is always that the source can only handle the regex
   * PARTIAL-ly (i.e., it is always also checked in standardizeResultsTable).
   *
   * @return "=~" or "~=" if the source can handle some source regex constraints, or "" if it can't.
   */
  public String sourceCanConstrainStringRegex() {
    return sourceCanConstrainStringRegex;
  }

  /**
   * This is a convenience for the subclass' getDataForDapQuery methods. <br>
   * This is the first step in converting a userDapQuery into a sourceDapQuery. It converts a
   * userDapQuery into a parsed, generic, source query. <br>
   * The userDapQuery refers to destinationNames and allows any constraint op on any variable in the
   * dataset. <br>
   * The sourceDapQuery refers to sourceNames and utilizes the sourceCanConstraintXxx settings to
   * move some constraintVariables into resultsVariables so they can be tested by
   * standardizeResultsTable (and by the source, or not). <br>
   * This removes constraints which can't be handled by the source (e.g., regex for numeric source
   * variables). <br>
   * But the subclass will almost always need to fine-tune the results from this method to PRUNE out
   * additional constraints that the source can't handle (e.g., constraints on inner variables in an
   * OPeNDAP sequence).
   *
   * <p>Users can do a numeric test for "=NaN" or "!=NaN" (and it works as they expect), but the NaN
   * must always be represented by "NaN" (not just an invalid number).
   *
   * @param language the index of the selected language
   * @param userDapQuery the query from the user after the '?', still percentEncoded (shouldn't be
   *     null) (e.g., var1,var2&var3%3C=40) referring to destinationNames (e.g., LAT), not
   *     sourceNames.
   * @param resultsVariables will receive the list of variables that it should request. <br>
   *     If a constraintVariable isn't in the original resultsVariables list that the user
   *     submitted, it will be added here.
   * @param constraintVariables will receive the constraint source variables that the source has
   *     indicated (via sourceCanConstrainNumericData and sourceCanConstrainStringData) that it
   *     would like to see.
   * @param constraintOps will receive the source operators corresponding to the constraint
   *     variables. <br>
   *     REGEX_OP remains REGEX_OP (it isn't converted to sourceCanConstrainRegex string). It is
   *     only useful if destValuesEqualSourceValues(). <br>
   *     For non-regex ops, if edv.scale_factor is negative, then operators which include &lt; or
   *     &gt; will be reversed to be &gt; or &lt;.
   * @param constraintValues will receive the source values corresponding to the constraint
   *     variables. <br>
   *     Note that EDVTimeStamp values for non-regex ops will be returned as epoch seconds (and NaN
   *     for missing_value), not in the source format. <br>
   *     For non-regex ops, edv.scale_factor and edv.add_offset will have been applied in reverse so
   *     that the constraintValues are appropriate for the source.
   * @throws Throwable if trouble (e.g., improper format or unrecognized variable, or impossible
   *     constraint) E.g., More than 1 distinct() or orderBy...() constraint is an error (caught by
   *     call to parseUserDapQuery(false)).
   */
  public void getSourceQueryFromDapQuery(
      int language,
      String userDapQuery,
      StringArray resultsVariables,
      StringArray constraintVariables,
      StringArray constraintOps,
      StringArray constraintValues)
      throws Throwable {
    if (reallyVerbose) String2.log("\nEDDTable.getSourceQueryFromDapQuery...");

    // pick the query apart
    resultsVariables.clear();
    constraintVariables.clear();
    constraintOps.clear();
    constraintValues.clear();
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
        false);
    if (debugMode) String2.log(">> constraintValues=" + constraintValues);

    // remove any fixedValue variables from resultsVariables (source can't provide their data
    // values)
    // work backwards since I may remove some variables
    for (int rv = resultsVariables.size() - 1; rv >= 0; rv--) {
      EDV edv = findDataVariableByDestinationName(resultsVariables.get(rv));
      if (edv.isFixedValue()) resultsVariables.remove(rv);
    }

    // make source query (which the source can handle, and which gets all
    //  the data needed to do all the constraints and make the final table)

    // Deal with each of the constraintVariables.
    // Work backwards because I often delete the current one.
    // !!!!! VERY IMPORTANT, COMPLEX CODE. THINK IT THROUGH CAREFULLY.
    // !!!!! THE IF/ELSE STRUCTURE HERE IS EXACTLY THE SAME AS IN standardizeResultsTable.
    // !!!!! SO IF YOU MAKE A CHANGE HERE, MAKE A CHANGE THERE.
    for (int cv = constraintVariables.size() - 1; cv >= 0; cv--) {
      String constraintVariable = constraintVariables.get(cv);
      int dv = String2.indexOf(dataVariableDestinationNames(), constraintVariable);
      EDV edv = dataVariables[dv];
      PAType sourcePAType = edv.sourceDataPAType();
      PAType destPAType = edv.destinationDataPAType();
      boolean isTimeStamp = edv instanceof EDVTimeStamp;

      String constraintOp = constraintOps.get(cv);
      String constraintValue =
          constraintValues.get(cv); // non-regex EDVTimeStamp conValues will be ""+epochSeconds
      double constraintValueD = String2.parseDouble(constraintValue);
      // Only valid numeric constraintValue for NaN is "NaN".
      // Test this because it helps discover other constraint syntax errors.
      if ((destPAType != PAType.CHAR && destPAType != PAType.STRING)
          && !constraintOp.equals(PrimitiveArray.REGEX_OP)
          && Double.isNaN(constraintValueD)) {
        if (constraintValue.equals("NaN") || (isTimeStamp && constraintValue.isEmpty())) {
          // not an error
        } else {
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0)
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.QUERY_ERROR_CONSTRAINT_NAN, 0),
                          constraintValue),
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.QUERY_ERROR_CONSTRAINT_NAN, language),
                          constraintValue)));
        }
      }
      if (reallyVerbose)
        String2.log(
            "  Looking at constraint#"
                + cv
                + ": "
                + constraintVariable
                + " "
                + constraintOp
                + " "
                + String2.toJson(constraintValue, 256));

      // constraintVariable is a fixedValue
      if (edv.isFixedValue()) {
        // it's a fixedValue; do constraint now
        // pass=that's nice    fail=NO DATA
        boolean tPassed = false;
        if (edv.sourceDataPAType() == PAType.STRING || constraintOp.equals(PrimitiveArray.REGEX_OP))
          tPassed =
              PrimitiveArray.testValueOpValue(edv.fixedValue(), constraintOp, constraintValue);
        else if (edv.sourceDataPAType() == PAType.FLOAT)
          tPassed =
              PrimitiveArray.testValueOpValue(
                  String2.parseFloat(edv.fixedValue()), constraintOp, constraintValueD);
        else
          tPassed =
              PrimitiveArray.testValueOpValue(
                  String2.parseDouble(edv.fixedValue()), constraintOp, constraintValueD);

        if (!tPassed)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  MustBe.THERE_IS_NO_DATA
                      + " "
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.NO_DATA_FIXED_VALUE, 0),
                          edv.destinationName(),
                          edv.fixedValue() + constraintOp + constraintValueD),
                  MustBe.THERE_IS_NO_DATA
                      + " "
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.NO_DATA_FIXED_VALUE, language),
                          edv.destinationName(),
                          edv.fixedValue() + constraintOp + constraintValueD)));

        // It passed, so remove the constraint from list passed to source.
        constraintVariables.remove(cv);
        constraintOps.remove(cv);
        constraintValues.remove(cv);
        if (reallyVerbose) String2.log("    The fixedValue constraint passes.");

        // constraint source is String
      } else if (sourcePAType == PAType.STRING) {
        if (sourceCanConstrainStringData == CONSTRAIN_NO
            || (constraintOp.equals(PrimitiveArray.REGEX_OP)
                && sourceCanConstrainStringRegex.length() == 0)
            || (constraintValue.length() == 0
                && edv.safeStringMissingValue().length()
                    > 0)) { // not possible to expand "" constraints to include stringMissingValue
          // and stringFillValue
          // remove from constraints and constrain after the fact in standardizeResultsTable
          constraintVariables.remove(cv);
          constraintOps.remove(cv);
          constraintValues.remove(cv);
        } else {
          // source will (partially) handle it
          if (reallyVerbose)
            String2.log("    The string constraint will be (partially) handled by source.");
        }

        // if source handling is NO or PARTIAL, data is needed to do the test in
        // standardizeResultsTable
        if (sourceCanConstrainStringData == CONSTRAIN_NO
            || sourceCanConstrainStringData == CONSTRAIN_PARTIAL
            || constraintOp.equals(
                PrimitiveArray.REGEX_OP)) { // regex always (also) done in standardizeResultsTable
          if (resultsVariables.indexOf(constraintVariable) < 0)
            resultsVariables.add(constraintVariable);
        }

        // constraint source is numeric
      } else {
        if (sourceCanConstrainNumericData == CONSTRAIN_NO
            || constraintOp.equals(PrimitiveArray.REGEX_OP)) {
          // remove from constraints and constrain after the fact in standardizeResultsTable
          constraintVariables.remove(cv);
          constraintOps.remove(cv);
          constraintValues.remove(cv);
        } else {
          // numeric variable

          // source will (partially) handle it
          if (reallyVerbose)
            String2.log("    The numeric constraint will be (partially) handled by source.");

          if (sourceNeedsExpandedFP_EQ
              && (sourcePAType == PAType.FLOAT || sourcePAType == PAType.DOUBLE)
              && !Double.isNaN(constraintValueD)
              && constraintValueD != Math2.roundToDouble(constraintValueD)) { // if not int

            double fudge =
                Math.abs(constraintValueD) * (sourcePAType == PAType.FLOAT ? 1e-7 : 1e-16);
            if (reallyVerbose && constraintOp.indexOf('=') >= 0)
              String2.log(
                  "    The constraint with '=' is being expanded because sourceNeedsExpandedFP_EQ=true.");
            switch (constraintOp) {
              case ">=" -> constraintValues.set(cv, "" + (constraintValueD - fudge));
              case "<=" -> constraintValues.set(cv, "" + (constraintValueD + fudge));
              case "=" -> {
                constraintVariables.atInsert(cv + 1, constraintVariable);
                constraintOps.set(cv, ">=");
                constraintOps.atInsert(cv + 1, "<=");
                constraintValues.set(cv, "" + (constraintValueD - fudge));
                constraintValues.atInsert(cv + 1, "" + (constraintValueD + fudge));
              }
            }
          }
        }

        // if source handling is NO or PARTIAL, data is needed to do the test below
        if (sourceCanConstrainNumericData == CONSTRAIN_NO
            || sourceCanConstrainNumericData == CONSTRAIN_PARTIAL
            || constraintOp.equals(PrimitiveArray.REGEX_OP)) {
          if (resultsVariables.indexOf(constraintVariable) < 0)
            resultsVariables.add(constraintVariable);
        }
      }
    }

    // gather info about constraints
    int nConstraints = constraintVariables.size();
    EDV constraintEdv[] = new EDV[nConstraints];
    boolean constraintVarIsString[] = new boolean[nConstraints];
    boolean constraintOpIsRegex[] = new boolean[nConstraints];
    double constraintValD[] = new double[nConstraints];
    boolean constraintValDIsNaN[] = new boolean[nConstraints];
    for (int cv = 0; cv < nConstraints; cv++) {
      EDV edv = findDataVariableByDestinationName(constraintVariables.get(cv));
      constraintEdv[cv] = edv;
      constraintVarIsString[cv] = edv.sourceDataPAType() == PAType.STRING;
      constraintOpIsRegex[cv] = constraintOps.get(cv).equals(PrimitiveArray.REGEX_OP);
      constraintValD[cv] = constraintValues.getDouble(cv);
      constraintValDIsNaN[cv] = Double.isNaN(constraintValD[cv]);
    }

    // Before convert to source names and values, test for conflicting (so impossible) constraints.
    // n^2 test, but should go quickly    (tests are quick and there are never a huge number of
    // constraints)
    for (int cv1 = 0; cv1 < constraintVariables.size(); cv1++) {
      if (constraintVarIsString[cv1] || constraintOpIsRegex[cv1]) continue;
      EDV edv1 = constraintEdv[cv1];
      String op1 = constraintOps.get(cv1);
      if (op1.equals(PrimitiveArray.REGEX_OP)) continue;
      char ch1 = op1.charAt(0);
      double val1 = constraintValD[cv1];
      boolean isNaN1 = constraintValDIsNaN[cv1];

      // is this test impossible by itself?
      if (isNaN1 && (op1.equals("<") || op1.equals(">")))
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.messages.get(Message.QUERY_ERROR, 0)
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.QUERY_ERROR_NEVER_TRUE, 0),
                        edv1.destinationName() + op1 + val1),
                EDStatic.messages.get(Message.QUERY_ERROR, language)
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.QUERY_ERROR_NEVER_TRUE, language),
                        edv1.destinationName() + op1 + val1)));

      for (int cv2 = cv1 + 1; cv2 < constraintVariables.size(); cv2++) {
        if (constraintVarIsString[cv2] || constraintOpIsRegex[cv2]) continue;
        EDV edv2 = constraintEdv[cv2];
        if (edv1 != edv2) // yes, fastest to do simple test of inequality of pointers
        continue;

        // we now know constraints use same, non-string var, and neither op is REGEX_OP
        // We don't have to catch every impossible query,
        // but DON'T flag any possible queries as impossible!
        String op2 = constraintOps.get(cv2);
        char ch2 = op2.charAt(0);
        double val2 = constraintValD[cv2];
        boolean isNaN2 = constraintValDIsNaN[cv2];
        boolean possible = true;
        // deal with isNaN and !isNaN
        // deal with ops:  < <= > >= = !=
        if (isNaN1) {
          if (ch1 == '!') {
            if (isNaN2) {
              // impossible: a!=NaN & a<=|>=|=NaN
              if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
            }

          } else {
            // test1 must be a<=|>=|=NaN, which really must be a=NaN
            if (isNaN2) {
              // impossible: a=NaN & a!=NaN
              if (ch2 == '!') possible = false;
            } else {
              // impossible: a=NaN & a<|<=|>|>=|=3
              if (ch2 != '!') possible = false;
            }
          }

        } else {
          // val1 is finite
          if (ch1 == '<') {
            if (isNaN2) {
              // impossible: a<|<=2 & a<=|>=|=NaN
              if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
            } else {
              // impossible: a<|<=2 & a>|>=|=3
              if ((ch2 == '>' || ch2 == '=') && val1 < val2) possible = false;
            }
          } else if (ch1 == '>') {
            if (isNaN2) {
              // impossible: a>|>=2 & a<=|>=|=NaN
              if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
            } else {
              // impossible: a>|>=3 & a<|<=|=2
              if ((ch2 == '<' || ch2 == '=') && val1 > val2) possible = false;
            }
          } else if (ch1 == '=') {
            if (isNaN2) {
              // impossible: a=2 & a<=|>=|=NaN
              if (op2.equals("<=") || op2.equals(">=") || ch2 == '=') possible = false;
            } else {
              if (ch2 == '>') {
                // impossible: a=3 & a>|>=4
                if (val1 < val2) possible = false;
              } else if (ch2 == '<') {
                // impossible: a=3 & a<|<=2
                if (val1 > val2) possible = false;
              } else if (ch2 == '=') {
                // impossible: a=2 & a=3
                if (val1 != val2) possible = false; // can do simple != test
              } else if (ch2 == '!') {
                // impossible: a=2 & a!=2
                if (val1 == val2) possible = false; // can do simple == test
              }
            }
          }
        }
        if (!possible)
          throw new SimpleException(
              EDStatic.bilingual(
                  language,
                  EDStatic.messages.get(Message.QUERY_ERROR, 0)
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.QUERY_ERROR_NEVER_BOTH_TRUE, 0),
                          edv1.destinationName() + op1 + val1,
                          edv2.destinationName() + op2 + val2),
                  EDStatic.messages.get(Message.QUERY_ERROR, language)
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.QUERY_ERROR_NEVER_BOTH_TRUE, language),
                          edv1.destinationName() + op1 + val1,
                          edv2.destinationName() + op2 + val2)));
      }
    }

    // Convert resultsVariables and constraintVariables to sourceNames.
    // Do last because sourceNames may not be unique (e.g., for derived axes, e.g., "=0")
    // And cleaner and safer to do all at once.
    for (int rv = 0; rv < resultsVariables.size(); rv++) {
      EDV edv = findDataVariableByDestinationName(resultsVariables.get(rv));
      resultsVariables.set(rv, edv.sourceName());
    }
    for (int cv = 0; cv < nConstraints; cv++) {
      EDV edv = constraintEdv[cv];
      constraintVariables.set(cv, edv.sourceName());

      // and if constaintVariable isn't in resultsVariables, add it
      if (resultsVariables.indexOf(edv.sourceName()) < 0) resultsVariables.add(edv.sourceName());

      if (!constraintOpIsRegex[cv]
          && // regex constraints are unchanged
          !(edv instanceof EDVTimeStamp)
          && // times stay as epochSeconds
          edv.scaleAddOffset()) { // need to apply scale and add_offset

        // apply scale_factor and add_offset to non-regex constraintValues
        // sourceValue = (destintationValue - addOffset) / scaleFactor;
        double td = (constraintValD[cv] - edv.addOffset()) / edv.scaleFactor();
        PAType tPAType = edv.sourceDataPAType();
        if (PAType.isIntegerType(tPAType)) constraintValues.set(cv, "" + Math2.roundToLong(td));
        else if (tPAType == PAType.FLOAT) constraintValues.set(cv, "" + (float) td);
        else constraintValues.set(cv, "" + td);
        // String2.log(">>after scaleAddOffset applied constraintVal=" + td);

        // *if* scale_factor < 0, reverse > and < operators
        if (edv.scaleFactor() < 0) {
          int copi = OPERATORS.indexOf(constraintOps.get(cv));
          constraintOps.set(cv, REVERSED_OPERATORS.get(copi));
        }
      }
    }

    if (reallyVerbose) String2.log("getSourceQueryFromDapQuery done");
    if (debugMode) String2.log(">> constraintValues=" + constraintValues);
  }

  /**
   * This is a convenience for the subclass' getData methods. This converts the table of data from
   * the source OPeNDAP query into the finished, standardized, results table. If need be, it tests
   * constraints that the source couldn't test.
   *
   * <p>This always does all regex tests.
   *
   * <p>Coming into this method, the column names are sourceNames. Coming out, they are
   * destinationNames.
   *
   * <p>Coming into this method, missing values in the table will be the original
   * edv.sourceMissingValues or sourceFillValues. Coming out, they will be
   * edv.destinationMissingValues or destinationFillValues.
   *
   * <p>This removes any global or column attributes from the table. This then adds a copy of the
   * combinedGlobalAttributes, and adds a copy of each variable's combinedAttributes.
   *
   * <p>This doesn't call setActualRangeAndBoundingBox (since you need the entire table to get
   * correct values). In fact, all the file-specific metadata is removed (e.g., actual_range) here.
   * Currently, the only file types that need file-specific metadata are .nc, .ncHeader, .ncCF,
   * .ncCFHeader, .ncCFMA, and .ncCFMAHeader. (.das always shows original metadata, not subset
   * metadata). Currently the only place the file-specific metadata is added is in TableWriterAll
   * (which has access to all the data).
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery the OPeNDAP DAP-style query from the user, after the '?', still
   *     percentEncoded (shouldn't be null). (e.g., var1,var2&var3&lt;40) referring to variable's
   *     destinationNames (e.g., LAT), instead of the sourceNames. Presumably, getSourceDapQuery has
   *     ensured that this is a valid query and has tested the constraints on fixedValue axes.
   * @param table the table from the source query which will be modified extensively to become the
   *     finished standardized table. It must have all the requested source resultsVariables (using
   *     sourceNames), and variables needed for constraints that need to be handled here. If it
   *     doesn't, the variables are assumed to be all missing values. It may have additional
   *     variables. It doesn't need to already have globalAttributes or variable attributes. If
   *     nRows=0, this throws SimpleException(MustBe.THERE_IS_NO_DATA).
   * @throws Throwable if trouble (e.g., improper format or unrecognized variable). Because this may
   *     be called separately for each station, this doesn't throw exception if no data at end;
   *     tableWriter.finish will catch such problems. But it does throw exception if no data at
   *     beginning.
   */
  public void standardizeResultsTable(
      int language, String requestUrl, String userDapQuery, Table table) throws Throwable {
    if (table.nRows() == 0)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (pre-standardize: nRows = 0)");
    StringBuilder msg =
        new StringBuilder(
            "    standardizeResultsTable incoming cols=" + table.getColumnNamesCSSVString());
    // String2.log(">> standardizeResultsTable incoming table=\n" + table.toString());
    // String2.log("DEBUG " + MustBe.getStackTrace());

    // pick the query apart
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
        false);
    // String2.log("> srt 1");

    // set the globalAttributes (this takes care of title, summary, ...)
    setResponseGlobalAttributes(language, requestUrl, userDapQuery, table);
    // String2.log("> srt 2");

    // Change all table columnNames from sourceName to destinationName.
    // Do first because sourceNames in Erddap may not be unique
    // (e.g., for derived vars, e.g., "=0").
    // But all table columnNames are unique because they are direct from source.
    // And cleaner and safer to do all at once.
    for (int col = table.nColumns() - 1;
        col >= 0;
        col--) { // work backward since may remove some cols

      // These are direct from source, so no danger of ambiguous fixedValue source names.
      String columnSourceName = table.getColumnName(col);
      int dv = String2.indexOf(dataVariableSourceNames(), columnSourceName);
      if (dv < 0) {
        // remove unexpected column
        if (debugMode)
          msg.append("\n      removing unexpected source column=").append(columnSourceName);
        table.removeColumn(col);
        continue;
      }

      // set the attributes
      // String2.log("  col=" + col + " dv=" + dv + " nRows=" + nRows);
      EDV edv = dataVariables[dv];
      table.setColumnName(col, edv.destinationName());
      table.columnAttributes(col).clear(); // remove any existing atts
      table
          .columnAttributes(col)
          .set(edv.combinedAttributes().toAttributes(language)); // make a copy

      // convert source values to destination values
      // (source fill&missing values become destination fill&missing values)
      // String2.log(">> srt 3a col=" + col + "=" + columnSourceName);
      table.setColumn(col, edv.toDestination(table.getColumn(col))); // sets maxIsMV
      // String2.log(">> srt 3b col=" + col + "=" + columnSourceName + " edv.maxIsMV=" +
      // edv.destinationMaxIsMV());
    }
    // String2.log(">> standardizeResultsTable after toDestination table=\n" + table.toString());
    if (reallyVerbose) String2.log(msg.toString());

    // apply the constraints and finish up
    applyConstraints(
        language,
        table,
        false, // apply CONSTRAIN_YES constraints?
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues);
    // String2.log(">> standardizeResultsTable after applyConstraints table=\n" + table.toString());

    if (debugMode) String2.log(table.toString(2));
  }

  /**
   * This is used by standardizeResultsTable (and places that bypass standardizeResultsTable) to
   * update the globalAttributes of a response table.
   *
   * @param language the index of the selected language
   */
  public void setResponseGlobalAttributes(
      int language, String requestUrl, String userDapQuery, Table table) {

    // set the globalAttributes (this takes care of title, summary, ...)
    table.globalAttributes().clear(); // remove any existing atts
    table.globalAttributes().set(combinedGlobalAttributes.toAttributes(language)); // make a copy

    // fix up global attributes  (always to a local COPY of global attributes)
    table.globalAttributes().set("history", getNewHistory(language, requestUrl, userDapQuery));
  }

  /**
   * Given a table that contains the destination values (and destinationMV) for the resultsVariables
   * and constraintVariables, this applies the constraints (removing some rows of the table),
   * removes non-resultsVariables variables, sets results variable attributes, and unsets
   * actual_range metadata.
   *
   * @param table which contains the destination values (and destinationMV) for the resultsVariables
   *     and constraintVariables and will be modified. If a given variable is missing, it is assumed
   *     to be all missing values.
   * @param applyAllConstraints if true, all constraints will be applied. If false, CONSTRAIN_YES
   *     constraints are not applied.
   * @param resultsVariables destinationNames
   * @param constraintVariables destinationNames
   * @param constraintOps any ops are ok.
   * @param constraintValues
   * @throws
   */
  public void applyConstraints(
      int language,
      Table table,
      boolean applyAllConstraints,
      StringArray resultsVariables,
      StringArray constraintVariables,
      StringArray constraintOps,
      StringArray constraintValues)
      throws Throwable {

    // move the requested resultsVariables columns into place   and add metadata
    StringBuilder sb = new StringBuilder();
    int nRows = table.nRows();
    for (int rv = 0; rv < resultsVariables.size(); rv++) {

      int dv = String2.indexOf(dataVariableDestinationNames(), resultsVariables.get(rv));
      EDV edv = dataVariables[dv];
      int col = table.findColumnNumber(edv.destinationName());

      if (col < 0) {
        // the column isn't present

        if (edv.isFixedValue()) {
          // edv is a fixedValue
          // if the column isn't present, add in correct place
          table.addColumn(
              rv,
              resultsVariables.get(rv),
              PrimitiveArray.factory(edv.destinationDataPAType(), nRows, edv.fixedValue()),
              edv.combinedAttributes().toAttributes(language)); // make a copy
        } else {
          // not found, so add new col with all missing values in correct place
          if (verbose)
            sb.append(
                "Note: " + "variable=" + edv.destinationName() + " not found in results table.\n");
          col = table.nColumns();
          double dmv = edv.safeDestinationMissingValue();
          table.addColumn(
              rv,
              edv.destinationName(),
              PrimitiveArray.factory(
                  edv.destinationDataPAType(), nRows, (Double.isNaN(dmv) ? "" : "" + dmv)),
              edv.combinedAttributes().toAttributes(language));
        }
      } else if (col < rv) {
        throw new SimpleException(
            "Error: "
                + "variable="
                + edv.destinationName()
                + " is in two columns ("
                + col
                + " and "
                + rv
                + ") in the results table.\n"
                + "colNames="
                + String2.toCSSVString(table.getColumnNames()));
      } else { // col >= rv
        // move the column into place
        table.moveColumn(col, rv);
        table
            .columnAttributes(rv)
            .set(edv.combinedAttributes().toAttributes(language)); // make a copy
      }
    }
    // String2.log("table after rearrange cols=\n" + table.toString());

    // deal with the constraints one-by-one
    // !!!!! VERY IMPORTANT CODE. THINK IT THROUGH CAREFULLY.
    // !!!!! THE IF/ELSE STRUCTURE HERE IS ALMOST EXACTLY THE SAME AS IN getSourceDapQuery.
    // Difference: above, only CONSTRAIN_NO items are removed from what goes to source
    //            below, CONSTRAIN_NO and CONSTRAIN_PARTIAL items are tested here
    // !!!!! SO IF YOU MAKE A CHANGE HERE, MAKE A CHANGE THERE.
    BitSet keep = new BitSet();
    keep.set(0, nRows, true);
    if (reallyVerbose) sb.append("    initial nRows=" + nRows);
    for (int cv = 0; cv < constraintVariables.size(); cv++) {
      String constraintVariable = constraintVariables.get(cv);
      String constraintOp = constraintOps.get(cv);
      String constraintValue = constraintValues.get(cv);
      int dv = String2.indexOf(dataVariableDestinationNames(), constraintVariable);
      EDV edv = dataVariables[dv];
      PAType sourcePAType = edv.sourceDataPAType();

      if (applyAllConstraints) {
        // applyAllConstraints
      } else {
        // skip this constraint if the source has handled it

        // is constraintVariable a fixedValue?
        if (edv.isFixedValue()) {
          // it's a fixedValue axis; always numeric; test was done by getSourceDapQuery
          continue;

          // constraint source is String
        } else if (sourcePAType == PAType.STRING) {
          if (sourceCanConstrainStringData == CONSTRAIN_NO
              || sourceCanConstrainStringData == CONSTRAIN_PARTIAL
              || constraintOp.equals(PrimitiveArray.REGEX_OP)
              || // always do all regex tests here (perhaps in addition to source)
              (constraintValue.length() == 0
                  && edv.safeStringMissingValue().length()
                      > 0)) { // not possible to expand "" constraints to include stringMissingValue
            // and stringFillValue
            // fall through to test below
          } else {
            // source did the test
            continue;
          }

          // constraint source is numeric
        } else {
          double constraintValueD = String2.parseDouble(constraintValue);
          if (sourceCanConstrainNumericData == CONSTRAIN_NO
              || sourceCanConstrainNumericData == CONSTRAIN_PARTIAL
              || constraintOp.equals(PrimitiveArray.REGEX_OP)
              || // always do all regex tests here
              (sourceNeedsExpandedFP_EQ
                  && // source did an expanded query. Check it here.
                  (sourcePAType == PAType.FLOAT || sourcePAType == PAType.DOUBLE)
                  && !Double.isNaN(constraintValueD)
                  && constraintValueD != Math2.roundToDouble(constraintValueD))) { // if not int
            // fall through to test below
          } else {
            // source did the test
            continue;
          }
        }
      }

      // The constraint needs to be tested here. Test it now.
      // Note that Timestamp and Alt values have been converted to standardized units above.
      PrimitiveArray dataPa = table.findColumn(constraintVariable); // throws Throwable if not found
      // chars and strings aren't converted
      // String2.log(">> pre  convert dataPa=" + dataPa.toString());
      int nSwitched =
          dataPa.convertToStandardMissingValues(
              "" + edv.destinationFillValue(), "" + edv.destinationMissingValue());
      // String2.log(">> post convert  dataPa=" + dataPa.toString());
      // String2.log("    nSwitched=" + nSwitched);

      int nStillGood =
          dataPa.applyConstraint(edv instanceof EDVTimeStamp, keep, constraintOp, constraintValue);
      if (reallyVerbose)
        sb.append(
            "\n    Handled constraint #"
                + cv
                + " here (nStillGood="
                + nStillGood
                + "): "
                + constraintVariable
                + " "
                + constraintOp
                + " "
                + String2.toJson(constraintValue));
      if (nStillGood == 0) break;
      if (nSwitched > 0) dataPa.switchNaNToFakeMissingValue(edv.safeStringMissingValue());

      // if (cv == constraintVariables.size() - 1 && nStillGood > 0) String2.log(table.toString());

    }
    if (reallyVerbose) String2.log(sb.toString());

    // discard excess columns (presumably constraintVariables not handled by source)
    table.removeColumns(resultsVariables.size(), table.nColumns());

    // remove the non-keep rows
    table.justKeep(keep);

    // unsetActualRangeAndBoundingBox  (see comments in method javadocs above)
    // (see TableWriterAllWithMetadata which adds them back)
    table.unsetActualRangeAndBoundingBox();
  }

  /**
   * This gets the data (chunk by chunk) from this EDDTable for the OPeNDAP DAP-style query and
   * writes it to the TableWriter.
   *
   * <p>This method allows any constraint on any variable. Since many data sources just support
   * constraints on certain variables (e.g., Dapper only supports axis variable constraints), the
   * implementations here must separate constraints that the server can handle and constraints that
   * must be handled separately afterwards. See getSourceQueryFromDapQuery.
   *
   * <p>This method does NOT call handleViaFixedOrSubsetVariables. You will usually want to call
   * handleViaFixedOrSubsetVariables before calling this to see if the subsetVariables can handle
   * this request (since that data is cached and so the response is fast).
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in). Although this
   *     is included, this method does NOT check if loggedInAs has access to this dataset. The
   *     exceptions are fromPOST datasets, where loggedInAs is used to determine access to each row
   *     of data.
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery the OPeNDAP DAP-style query from the user after the '?', still
   *     percentEncoded (may be null). (e.g., var1,var2&var3%3C=40) referring to destination
   *     variable names. See parseUserDapQuery.
   * @param tableWriter
   * @throws Throwable if trouble (notably, WaitThenTryAgainException)
   */
  public abstract void getDataForDapQuery(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable;

  /**
   * This is a convenience method to get all of the data for a userDapQuery in a
   * TableWriterAllWithMetadata. You can easily get the entire table (if it fits in memory) via
   * Table table = getTwawmForDapQuery(...).cumulativeTable();
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but in unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata. Use "" if not important (e.g.,
   *     for tests).
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null).
   * @return twawm with all of the data already read. twawm always adds a randomInt to the temporary
   *     file names. If the twawm needed to be enclosed (e.g., orderByMax), it was and the final
   *     results are now in twawm
   */
  public TableWriterAllWithMetadata getTwawmForDapQuery(
      int language, String loggedInAs, String requestUrl, String userDapQuery) throws Throwable {

    long tTime = System.currentTimeMillis();
    String dir = cacheDirectory(); // dir is created by EDD.ensureValid
    String fileName = suggestFileName(loggedInAs, userDapQuery, ".twawm");
    if (reallyVerbose) String2.log("getTwawmForDapQuery " + dir + fileName);
    TableWriterAllWithMetadata twawm =
        new TableWriterAllWithMetadata(
            language,
            userDapQuery.indexOf("&distinct") >= 0
                    || // if other TableWriters, they will provide the metadata
                    userDapQuery.indexOf("&orderBy") >= 0
                    || userDapQuery.indexOf("&units") >= 0
                ? null
                : this,
            getNewHistory(language, requestUrl, userDapQuery),
            dir,
            fileName);
    TableWriter tableWriter =
        encloseTableWriter(
            language,
            true, // alwaysDoAll
            dir,
            fileName,
            twawm,
            requestUrl,
            userDapQuery);
    if (handleViaFixedOrSubsetVariables(
        language, loggedInAs, requestUrl, userDapQuery, tableWriter)) {
      // it's done
    } else {
      getDataForDapQuery(language, loggedInAs, requestUrl, userDapQuery, tableWriter); // do it
    }
    if (reallyVerbose)
      String2.log(
          "getTwawmForDapQuery finished. time=" + (System.currentTimeMillis() - tTime) + "ms");

    return twawm;
  }

  /**
   * This is used by subclasses getDataForDapQuery to write a chunk of source table data to a
   * tableWriter if nCells &gt; partialRequestMaxCells or finish==true. <br>
   * This converts columns to expected sourceTypes (if they weren't already). <br>
   * This calls standardizeResultsTable.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery after the '?', still percentEncoded (shouldn't be null).
   * @param table with a chunk of the total source data.
   * @param tableWriter to which the table's data will be written
   * @param finish If true, the will be written to the tableWriter. If false, the data will only be
   *     written to tableWriter if there are a sufficient number of cells of data.
   * @return true Returns true if it wrote the table data to tableWriter (table was changed, so
   *     should be thrown away). Returns false if the data wasn't written -- add some more rows of
   *     data and call this again.
   */
  protected boolean writeChunkToTableWriter(
      int language,
      String requestUrl,
      String userDapQuery,
      Table table,
      TableWriter tableWriter,
      boolean finish)
      throws Throwable {

    // write to tableWriter?
    // String2.log("writeChunkToTableWriter table.nRows=" + table.nRows());
    if (table.nRows() > 1000
        || (table.nRows() * (long) table.nColumns() > EDStatic.config.partialRequestMaxCells)
        || finish) {

      if (table.nRows() == 0 && finish) {
        tableWriter.finish();
        return true;
      }

      // String2.log("\ntable at end of getSourceData=\n" + table.toString("row", 10));
      // convert columns to stated type
      for (int col = 0; col < table.nColumns(); col++) {
        String colName = table.getColumnName(col);
        EDV edv = findDataVariableBySourceName(colName);
        PAType shouldBeType = edv.sourceDataPAType();
        PAType isType = table.getColumn(col).elementType();
        if (shouldBeType != isType) {
          // if (reallyVerbose) String2.log("  converting col=" + col +
          //    " from=" + isType + " to="   + shouldBeType);
          PrimitiveArray newPa = PrimitiveArray.factory(shouldBeType, 1, false);
          newPa.append(table.getColumn(col));
          table.setColumn(col, newPa);
        }
      }

      // standardize the results table
      if (table.nRows() > 0) {
        standardizeResultsTable(
            language, requestUrl, userDapQuery, table); // changes sourceNames to destinationNames
        tableWriter.writeSome(table);
      }

      // done?
      if (finish) tableWriter.finish();

      return true;
    }

    return false;
  }

  /** This is a variant of parseUserDapQuery where processAddVariablesWhere is true. */
  public void parseUserDapQuery(
      int language,
      String userDapQuery,
      StringArray resultsVariables,
      StringArray constraintVariables,
      StringArray constraintOps,
      StringArray constraintValues,
      boolean repair)
      throws Throwable {
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues,
        repair,
        true); // processAddVariablesWhere
  }

  /**
   * This parses a PERCENT ENCODED OPeNDAP DAP-style query. This checks the validity of the
   * resultsVariable and constraintVariable names.
   *
   * <p>Unofficially (e.g., for testing) the query can be already percent decoded if there are no
   * %dd in the decoded query.
   *
   * <p>There can be a constraints on variables that aren't in the user-specified results variables.
   * This procedure adds those variables to the returned resultsVariables.
   *
   * <p>If the constraintVariable is time, the value can be numeric ("seconds since 1970-01-01") or
   * String (ISO format, e.g., "1996-01-31T12:40:00", at least YYYY-MM) or
   * now(+|-)(seconds|minutes|hours|days|months|years). All variants are converted to epochSeconds.
   *
   * @param language the index of the selected language
   * @param userDapQuery is the opendap DAP-style query, after the '?', still percentEncoded
   *     (shouldn't be null), e.g., <tt>var1,var2,var3&amp;var4=value4&amp;var5%3E=value5</tt> .
   *     <br>
   *     Values for String variable constraints should be in double quotes. <br>
   *     A more specific example is
   *     <tt>genus,species&amp;genus="Macrocystis"&amp;LAT%3C=53&amp;LAT%3C=54</tt> . <br>
   *     If no results variables are specified, all will be returned. See OPeNDAP specification,
   *     section 6.1.1. <br>
   *     Note that each constraint's left hand side must be a variable and its right hand side must
   *     be a value. <br>
   *     The valid operators (for numeric and String variables) are "=", "!=", "&lt;", "&lt;=",
   *     "&gt;", "&gt;=", and "=~" (REGEX_OP, which looks for data values matching the regular
   *     expression on the right hand side), but in percent encoded form. (see
   *     https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Selecting_Data:_Using_Constraint_Expressions).
   *     <br>
   *     If an &amp;-separated part is "distinct()", "orderBy("...")", "orderBy...("...")",
   *     "units("...")", it is ignored. <br>
   *     If an &amp;-separated part starts with ".", it is ignored. It can't be a variable name.
   *     &amp;.[param]=value is used to pass special values (e.g., &amp;.colorBar=...).
   * @param resultsVariables to be appended with the results variables' destinationNames, e.g.,
   *     {var1,var2,var3}. <br>
   *     If a variable is needed for constraint testing in standardizeResultsTable but is not among
   *     the user-requested resultsVariables, it won't be added to resultsVariables.
   * @param constraintVariables to be appended with the constraint variables' destinationNames,
   *     e.g., {var4,var5}. This method makes sure they are valid. These will not be percent
   *     encoded.
   * @param constraintOps to be appended with the constraint operators, e.g., {"=", "&gt;="}. These
   *     will not be percent encoded.
   * @param constraintValues to be appended with the constraint values, e.g., {value4,value5}. These
   *     will not be percent encoded. Non-regex EDVTimeStamp constraintValues will be returned as
   *     epochSeconds.
   * @param repair if true, this method tries to do its best repair problems (guess at intent), not
   *     to throw exceptions
   * @param processAddVariablesWhere if false, this ignores addVariablesWhere functions.
   * @throws Throwable if invalid query (0 resultsVariables is a valid query -- it is expanded to be
   *     all vars) A query can't have more than on orderBy... or distinct() constraint (but if
   *     repair=true, this is ignored).
   */
  public void parseUserDapQuery(
      int language,
      String userDapQuery,
      StringArray resultsVariables,
      StringArray constraintVariables,
      StringArray constraintOps,
      StringArray constraintValues,
      boolean repair,
      boolean processAddVariablesWhere)
      throws Throwable {
    // !!! Table.parseDapQuery and EDDTable.parseUserDapQuery ARE ALMOST IDENTICAL!!!
    // IF YOU MAKE CHANGES TO ONE, MAKE CHANGES TO THE OTHER.

    // split userDapQuery into parts at &'s
    String parts[] =
        Table.getDapQueryParts(
            userDapQuery); // results are decoded.  always at least 1 part (it may be "")
    resultsVariables.clear();
    constraintVariables.clear();
    constraintOps.clear();
    constraintValues.clear();

    // look at part0 with comma-separated vars
    // expand no resultsVariables (or entire sequence) into all results variables
    if (parts[0].length() == 0 || parts[0].equals(SEQUENCE_NAME)) {
      if (debugMode)
        String2.log(
            "  userDapQuery parts[0]=\"" + parts[0] + "\" is expanded to request all variables.");
      for (EDV dataVariable : dataVariables) {
        resultsVariables.add(dataVariable.destinationName());
      }
    } else {
      String cParts[] = String2.split(parts[0], ','); // list of results variables
      for (String cPart : cParts) {

        // request uses sequence.dataVarName notation?
        String tVar = cPart.trim();
        int period = tVar.indexOf('.');
        if (period > 0 && tVar.substring(0, period).equals(SEQUENCE_NAME))
          tVar = tVar.substring(period + 1);

        // is it a valid destinationName?
        int po = String2.indexOf(dataVariableDestinationNames(), tVar);
        if (po < 0) {
          if (!repair) {
            if (tVar.equals(SEQUENCE_NAME))
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "If "
                      + SEQUENCE_NAME
                      + " is requested, it must be the only requested variable.");
            for (int op = 0; op < OPERATORS.size(); op++) {
              int opPo = tVar.indexOf(OPERATORS.get(op));
              if (opPo >= 0)
                throw new SimpleException(
                    EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                        + "All constraints (including \""
                        + tVar.substring(0, opPo + OPERATORS.get(op).length())
                        + "...\") must be preceded by '&'.");
            }
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "Unrecognized variable=\""
                    + tVar
                    + "\".");
          }
        } else {
          // it's valid; is it a duplicate?
          if (resultsVariables.indexOf(tVar) >= 0) {
            if (!repair)
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "variable="
                      + tVar
                      + " is listed twice in the results variables list.");
          } else {
            resultsVariables.add(tVar);
          }
        }
      }
    }
    // String2.log("resultsVariables=" + resultsVariables);

    // get the constraints
    for (int p = 1; p < parts.length; p++) {
      // deal with one constraint at a time
      String constraint = parts[p];
      int quotePo = constraint.indexOf('"');
      String constraintBeforeQuotes = quotePo >= 0 ? constraint.substring(0, quotePo) : constraint;

      // special case: ignore constraint starting with "."
      // (can't be a variable name)
      // use for e.g., .colorBar=...
      if (constraint.startsWith(".")) continue;

      if (constraint.endsWith("\")")
          && (constraint.startsWith("orderBy(\"")
              || constraint.startsWith("orderByDescending(\""))) {
        // ensure all orderBy vars are in resultsVariables
        // TableWriters for orderBy... do additional checking
        if (!repair) {
          int ppo = constraint.indexOf("(\"");
          StringArray obv =
              StringArray.fromCSV(constraint.substring(ppo + 2, constraint.length() - 2));
          for (int obvi = 0; obvi < obv.size(); obvi++) {
            if (resultsVariables.indexOf(obv.get(obvi)) < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.messages.get(Message.QUERY_ERROR, 0)
                          + MessageFormat.format(
                              EDStatic.messages.get(Message.QUERY_ERROR_ORDER_BY_VARIABLE, 0),
                              obv.get(obvi)),
                      EDStatic.messages.get(Message.QUERY_ERROR, language)
                          + MessageFormat.format(
                              EDStatic.messages.get(
                                  Message.QUERY_ERROR_ORDER_BY_VARIABLE, language),
                              obv.get(obvi))));
          }
        }
        continue;
      }

      // special case: server-side functions: standard orderBySomething (varCSV)
      if (constraint.endsWith("\")")
          && (constraint.startsWith("orderByCount(\"")
              || constraint.startsWith("orderByMax(\"")
              || constraint.startsWith("orderByMin(\"")
              || constraint.startsWith("orderByMinMax(\""))) {
        // orderByClosest(... and orderByLimit( are below

        // ensure all orderBy vars are in resultsVariables
        // TableWriters for orderBy... do additional checking
        if (!repair) {
          int ppo = constraint.indexOf("(\"");
          StringArray obv =
              StringArray.fromCSV(constraint.substring(ppo + 2, constraint.length() - 2));
          for (int obvi = 0; obvi < obv.size(); obvi++) {
            String[] obvParts = obv.get(obvi).split("\\s*/\\s*");
            String item = obvParts[0];
            if (item.length() > 0 && resultsVariables.indexOf(item) < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                      language,
                      EDStatic.messages.get(Message.QUERY_ERROR, 0)
                          + MessageFormat.format(
                              EDStatic.messages.get(Message.QUERY_ERROR_ORDER_BY_VARIABLE, 0),
                              obv.get(obvi)),
                      EDStatic.messages.get(Message.QUERY_ERROR, language)
                          + MessageFormat.format(
                              EDStatic.messages.get(
                                  Message.QUERY_ERROR_ORDER_BY_VARIABLE, language),
                              obv.get(obvi))));
          }
        }
        continue;
      }

      // special case: server-side functions: special orderByClosest(varCSV, nTimeUnits)
      if (constraint.endsWith("\")") && constraint.startsWith("orderByClosest(\"")) {

        // ensure all orderBy vars are in resultsVariables
        if (!repair) {
          int ppo = constraint.indexOf("(\"");
          StringArray obv =
              StringArray.fromCSV(constraint.substring(ppo + 2, constraint.length() - 2));

          // bob added, but is not sure: allow csv[last] to be e.g., time/1day, instead of time,1day
          if (obv.size() >= 1) {
            String obvLast = obv.get(obv.size() - 1);
            int po = obvLast.indexOf('/');
            if (po > 0) {
              obv.set(obv.size() - 1, obvLast.substring(0, po));
              obv.add(obvLast.substring(po + 1));
            }
          }

          if (obv.size() < 2)
            throw new SimpleException(
                EDStatic.bilingual(
                        language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_CLOSEST)
                    + (language == 0 ? " " : "\n")
                    + "csv.length<2.");
          for (int obvi = 0; obvi < obv.size() - 1; obvi++) { // -1 since last item is interval
            String[] obvParts = obv.get(obvi).split("\\s*/\\s*");
            String item = obvParts[0];
            if (item.length() > 0 && resultsVariables.indexOf(item) < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                          language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_CLOSEST)
                      + (language == 0 ? " " : "\n")
                      + "col="
                      + obv.get(obvi)
                      + " is not in results variables.");
          }
          // ??? verify that next to last item is numeric?
          // ??? verify that last item is interval?
        }
        continue;
      }

      // special case: server-side functions: special orderByLimit(varCSV, limitN)
      if (constraint.endsWith("\")") && constraint.startsWith("orderByLimit(\"")) {

        // ensure all orderBy vars are in resultsVariables
        if (!repair) {
          int ppo = constraint.indexOf("(\"");
          StringArray obv =
              StringArray.fromCSV(constraint.substring(ppo + 2, constraint.length() - 2));
          if (obv.size() == 0)
            throw new SimpleException(
                EDStatic.bilingual(
                        language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_LIMIT)
                    + (language == 0 ? " " : "\n")
                    + "csv.length=0.");
          for (int obvi = 0; obvi < obv.size() - 1; obvi++) { // -1 since last item is limitN
            String[] obvParts = obv.get(obvi).split("\\s*/\\s*");
            String item = obvParts[0];
            if (item.length() > 0 && resultsVariables.indexOf(item) < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                          language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_LIMIT)
                      + (language == 0 ? " " : "\n")
                      + "col="
                      + obv.get(obvi)
                      + " is not in results variables.");
          }
          // ??? verify that next to last item is numeric?
          // ??? verify that last item is interval?
        }
        continue;
      }

      // special case: server-side functions: special orderByMean(varCSV)
      if (constraint.endsWith("\")") && constraint.startsWith("orderByMean(\"")) {
        // ensure all orderBy vars are in resultsVariables
        if (!repair) {
          int ppo = constraint.indexOf("(\"");
          StringArray obv =
              StringArray.fromCSV(constraint.substring(ppo + 2, constraint.length() - 2));
          int last = obv.size(); // last may be time interval...
          for (int obvi = 0; obvi < last; obvi++) {
            String[] obvParts = obv.get(obvi).split("\\s*/\\s*");
            String item = obvParts[0];
            if (item.length() > 0 && resultsVariables.indexOf(item) < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                          language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_MEAN)
                      + (language == 0 ? " " : "\n")
                      + "col="
                      + obv.get(obvi)
                      + " is not in results variables.");
          }
        }
        continue;
      }

      // special case: server-side functions: special orderBySum(varCSV)
      if (constraint.endsWith("\")") && constraint.startsWith("orderBySum(\"")) {
        // ensure all orderBy vars are in resultsVariables
        if (!repair) {
          int ppo = constraint.indexOf("(\"");
          StringArray obv =
              StringArray.fromCSV(constraint.substring(ppo + 2, constraint.length() - 2));
          int last = obv.size(); // last may be time interval...
          for (int obvi = 0; obvi < last; obvi++) {
            String[] obvParts = obv.get(obvi).split("\\s*/\\s*");
            String item = obvParts[0];
            if (item.length() > 0 && resultsVariables.indexOf(item) < 0)
              throw new SimpleException(
                  EDStatic.bilingual(
                          language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_SUM)
                      + (language == 0 ? " " : "\n")
                      + "col="
                      + obv.get(obvi)
                      + " is not in results variables.");
          }
        }
        continue;
      }

      // special case: server-side function
      if (constraint.equals("distinct()")) continue;

      // special case: server-side function
      if (constraint.endsWith("\")") && constraint.startsWith("units(\"")) continue;

      // special case: server-side function: addVariablesWhere(attName, attValue)
      if (constraint.endsWith("\")") && constraint.startsWith("addVariablesWhere(\"")) {
        if (!processAddVariablesWhere) continue;
        int ppo = constraint.indexOf("(\"");
        StringArray obv =
            StringArray.fromCSV(constraint.substring(ppo + 1, constraint.length() - 1));
        if (obv.size() != 2
            || !String2.isSomething(obv.get(0))
            || !String2.isSomething(obv.get(1))) {
          String tmsg =
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "&addVariablesWhere() MUST have 2 parameters: attributeName and attributeValue.";
          if (repair)
            // ignore the problem
            String2.log(String2.WARNING + ": " + tmsg);
          else throw new SimpleException(tmsg);
          continue;
        }
        String tAttName = obv.get(0);
        String tAttValue = obv.get(1);

        // go through vars looking for attName=attValue
        for (EDV edv : dataVariables) {
          if (tAttValue.equals(edv.combinedAttributes().getString(language, tAttName))) {
            // add var to resultsVariables if not already there
            if (resultsVariables.indexOf(edv.destinationName()) < 0)
              resultsVariables.add(edv.destinationName());
          }
        }
        continue;
      }

      // look for == (common mistake, but not allowed)
      int eepo = constraintBeforeQuotes.indexOf("==");
      if (eepo >= 0) {
        if (repair) {
          constraint = constraint.substring(0, eepo) + constraint.substring(eepo + 1);
          quotePo = constraint.indexOf('"');
          constraintBeforeQuotes = quotePo >= 0 ? constraint.substring(0, quotePo) : constraint;
        } else {
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "Use '=' instead of '==' in constraints.");
        }
      }

      // look for ~= (common mistake, but not allowed)
      if (constraintBeforeQuotes.indexOf("~=") >= 0) {
        if (repair) {
          constraint = String2.replaceAll(constraint, "~=", "=~");
          quotePo = constraint.indexOf('"');
          constraintBeforeQuotes = quotePo >= 0 ? constraint.substring(0, quotePo) : constraint;
        } else {
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "Use '=~' instead of '~=' in constraints.");
        }
      }

      // find the valid op within constraintBeforeQuotes
      int op = 0;
      int opPo = -1;
      while (op < OPERATORS.size()
          && (opPo = constraintBeforeQuotes.indexOf(OPERATORS.get(op))) < 0) op++;
      if (opPo < 0) {
        if (repair) continue; // was IllegalArgumentException
        else
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "No operator found in constraint=\""
                  + constraint
                  + "\".");
      }

      // request uses sequenceName.dataVarName notation?
      String tName = constraint.substring(0, opPo);
      int period = tName.indexOf('.');
      if (period > 0 && tName.substring(0, period).equals(SEQUENCE_NAME))
        tName = tName.substring(period + 1);

      // is it a valid destinationName?
      int dvi = String2.indexOf(dataVariableDestinationNames(), tName);
      if (dvi < 0) {
        if (repair) continue;
        else
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "Unrecognized constraint variable=\""
                  + tName
                  + "\"."); // + "\nValid=" + String2.toCSSVString(dataVariableDestionNames())
      }

      EDV conEdv = dataVariables[dvi];
      boolean conEdvIsTimeStamp = conEdv instanceof EDVTimeStamp;
      constraintVariables.add(tName);
      constraintOps.add(OPERATORS.get(op));
      String tValue = constraint.substring(opPo + OPERATORS.get(op).length());
      constraintValues.add(tValue);
      double conValueD = Double.NaN;

      if (debugMode) String2.log(">> constraint: " + tName + OPERATORS.get(op) + tValue);

      if (!PrimitiveArray.REGEX_OP.equals(OPERATORS.get(op))
          && (tValue.startsWith("min(") || tValue.startsWith("max("))) {
        // min() and max()
        try {
          String mmString = tValue.substring(0, 3);
          int cpo = tValue.indexOf(')');
          if (cpo < 0)
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "')' not found after \""
                    + mmString
                    + "(\".");
          String mVarName = tValue.substring(4, cpo);
          EDV mVar = findDataVariableByDestinationName(mVarName); // throws Exception
          boolean mVarIsTimestamp = mVar instanceof EDVTimeStamp;
          conValueD =
              tValue.startsWith("min(")
                  ? mVar.destinationMinDouble()
                  : // time will be epochSeconds
                  mVar.destinationMaxDouble();
          if (Double.isNaN(conValueD))
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "\""
                    + mmString
                    + "("
                    + mVarName
                    + ")\" is not allowed because the "
                    + mmString
                    + "imum value isn't known.");
          if (cpo != tValue.length() - 1) {
            // there's more, e.g., min(var1)-25days
            conValueD =
                Calendar2.parseMinMaxString( // throws Exception
                    tValue, conValueD, mVarIsTimestamp);
          }
          if (verbose)
            String2.log(
                "  "
                    + tValue
                    + " -> "
                    + (mVarIsTimestamp
                        ? Calendar2.safeEpochSecondsToIsoStringT3Z(conValueD, "NaN")
                        : conValueD));
          constraintValues.set(constraintValues.size() - 1, "" + conValueD);
        } catch (Exception me) {
          if (repair) {
            String2.log("repairing conValue=\"" + tValue + "\" by discarding it");
            constraintVariables.remove(constraintVariables.size() - 1);
            constraintOps.remove(constraintOps.size() - 1);
            constraintValues.remove(constraintValues.size() - 1);
            continue;
          } else {
            throw me;
          }
        }

      } else if (conEdvIsTimeStamp) {
        // convert <time><op><isoString> to <time><op><epochSeconds>
        // this isn't precise!!!   should it be required??? or forbidden???
        if (debugMode) String2.log(">> isTimeStamp=true");
        if (tValue.startsWith("\"") && tValue.endsWith("\"")) {
          tValue = String2.fromJsonNotNull(tValue);
          constraintValues.set(constraintValues.size() - 1, tValue);
        }

        // if not for regex, convert isoString to epochSeconds
        if (!PrimitiveArray.REGEX_OP.equals(OPERATORS.get(op))) {
          if (Calendar2.isIsoDate(tValue)) {
            conValueD =
                repair
                    ? Calendar2.safeIsoStringToEpochSeconds(tValue)
                    : Calendar2.isoStringToEpochSeconds(tValue);
            constraintValues.set(constraintValues.size() - 1, "" + conValueD);
            if (debugMode)
              String2.log(
                  ">> TIME CONSTRAINT converted in parseUserDapQuery: "
                      + tValue
                      + " -> "
                      + conValueD);

          } else if (tValue.toLowerCase().startsWith("now")) {
            if (repair)
              conValueD =
                  Calendar2.safeNowStringToEpochSeconds(
                      tValue, (double) Math2.hiDiv(System.currentTimeMillis(), 1000));
            else conValueD = Calendar2.nowStringToEpochSeconds(tValue);
            constraintValues.set(constraintValues.size() - 1, "" + conValueD);

          } else {
            // it must be a number (epochSeconds)
            // test that value=NaN must use NaN or "", not just an invalidly formatted number
            conValueD = String2.parseDouble(tValue);
            if (!Double.isFinite(conValueD) && !tValue.equals("NaN") && !tValue.isEmpty()) {
              if (repair) {
                tValue = "NaN";
                conValueD = Double.NaN;
                constraintValues.set(constraintValues.size() - 1, tValue);
              } else {
                throw new SimpleException(
                    EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                        + "Test for missing time values must use \"NaN\" or \"\", not value=\""
                        + tValue
                        + "\".");
              }
            }
          }
        }

      } else if (conEdv.destinationDataPAType() == PAType.CHAR
          || conEdv.destinationDataPAType() == PAType.STRING) {

        // String variables must have " around constraintValues
        if ((tValue.startsWith("\"") && tValue.endsWith("\"")) || repair) {
          // repair if needed
          if (!tValue.startsWith("\"")) tValue = "\"" + tValue;
          if (!tValue.endsWith("\"")) tValue = tValue + "\"";

          // decode
          tValue = String2.fromJsonNotNull(tValue);
          constraintValues.set(constraintValues.size() - 1, tValue);

        } else {
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "For constraints of String variables, the right-hand-side value must be surrounded by double quotes. Bad constraint: "
                  + constraint);
        }

      } else {
        // numeric variables

        // if op=regex, value must have "'s around it
        if (PrimitiveArray.REGEX_OP.equals(OPERATORS.get(op))) {
          if ((tValue.startsWith("\"") && tValue.endsWith("\"")) || repair) {
            // repair if needed
            if (!tValue.startsWith("\"")) tValue = "\"" + tValue;
            if (!tValue.endsWith("\"")) tValue = tValue + "\"";

            // decode
            tValue = String2.fromJsonNotNull(tValue);
            constraintValues.set(constraintValues.size() - 1, tValue);
          } else {
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "For =~ constraints of numeric variables, the right-hand-side value must be surrounded by double quotes. Bad constraint: "
                    + constraint);
          }

        } else {
          // if op!=regex, numbers must NOT have "'s around them
          if (tValue.startsWith("\"") || tValue.endsWith("\"")) {
            if (repair) {
              if (tValue.startsWith("\"")) tValue = tValue.substring(1);
              if (tValue.endsWith("\"")) tValue = tValue.substring(0, tValue.length() - 1);
              constraintValues.set(constraintValues.size() - 1, tValue);
            } else {
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "For non =~ constraints of numeric variables, the right-hand-side value must not be surrounded by double quotes. Bad constraint: "
                      + constraint);
            }
          }

          // test of value=NaN must use "NaN", not something just a badly formatted number
          conValueD = String2.parseDouble(tValue);
          if (!Double.isFinite(conValueD) && !tValue.equals("NaN")) {
            if (repair) {
              conValueD = Double.NaN;
              tValue = "NaN";
              constraintValues.set(constraintValues.size() - 1, tValue);
            } else {
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "Numeric tests of NaN must use \"NaN\", not value=\""
                      + tValue
                      + "\".");
            }
          }
        }
      }

      // If numeric conValueD is finite, test against destinationMin and destinationMax, if known.
      // added 2014-01-22
      // pass=that's nice    fail=NO DATA
      // This presumes that destMin and Max are reliable values and
      //  won't have changed before this data request.
      // For timestamp variables, all numeric tests here are epochSeconds.
      // This DOESN'T (changed 2015-02-24) presume that timestamp's destMax
      //  is NaN for times close to NOW
      //  (since it changes often), e.g., cwwcNDBCMet data from (changing) files.
      // This presumes that other variables in near-real-time datasets
      //  are unlikely to have just now exceeded the previous
      //  destMin and destMax values (e.g., atemp in cwwcNDBCMet).
      if (Double.isFinite(conValueD)) {
        boolean tPassed = true;
        double destMin = conEdv.destinationMinDouble();
        double destMax = conEdv.destinationMaxDouble();
        // if edvTimeStamp and destMax is recent, treat destMax as NaN
        if (conEdvIsTimeStamp
            && // destMax is epochSeconds
            destMax > System.currentTimeMillis() / 1000.0 - 2.0 * Calendar2.SECONDS_PER_DAY) {
          // maxTime is within last 48hrs, so setting maxTime to NaN (i.e., Now).
          destMax = Double.NaN;
        }
        String constraintOp = OPERATORS.get(op);
        // non-timestamp tests are troubled by float/double/slight differences
        if (tPassed && Double.isFinite(destMin)) {
          if (constraintOp.equals("=") || constraintOp.startsWith("<")) {
            tPassed =
                conEdvIsTimeStamp
                    ? conValueD >= destMin - 1
                    : // precise test, but -1 is 1 second fudge
                    Math2.greaterThanAE(5, conValueD, destMin); // loose test with bias towards pass
            if (!tPassed && verbose)
              String2.log(
                  "failed conVal=" + conValueD + " " + constraintOp + " destMin=" + destMin);
          }
        }
        if (tPassed && Double.isFinite(destMax)) {
          if (constraintOp.equals("=") || constraintOp.startsWith(">")) {
            tPassed =
                conEdvIsTimeStamp
                    ? conValueD <= destMax + 1
                    : // precise test, but +1 is 1 second fudge
                    Math2.lessThanAE(5, conValueD, destMax); // loose test with bias towards pass
            if (!tPassed && verbose)
              String2.log(
                  "failed conVal=" + conValueD + " " + constraintOp + " destMax=" + destMax);
          }
        }
        if (!tPassed) {
          String tv =
              conEdvIsTimeStamp
                  ? ((EDVTimeStamp) conEdv).destinationToString(conValueD)
                  : // show times as ISO strings
                  tValue;
          String msg0 =
              MessageFormat.format(
                  EDStatic.messages.get(Message.QUERY_ERROR_ACTUAL_RANGE, 0),
                  tName + constraintOp + tv,
                  Double.isFinite(destMin) ? conEdv.destinationMinString() : "NaN",
                  Double.isFinite(destMax) ? conEdv.destinationMaxString() : "NaN");
          String msgl =
              MessageFormat.format(
                  EDStatic.messages.get(Message.QUERY_ERROR_ACTUAL_RANGE, language),
                  tName + constraintOp + tv,
                  Double.isFinite(destMin) ? conEdv.destinationMinString() : "NaN",
                  Double.isFinite(destMax) ? conEdv.destinationMaxString() : "NaN");
          if (repair) {
            // get rid of this constraint
            if (verbose) String2.log("  Repair: removed constraint: " + msg0);
            constraintVariables.remove(constraintVariables.size() - 1);
            constraintOps.remove(constraintOps.size() - 1);
            constraintValues.remove(constraintValues.size() - 1);
          } else {
            throw new SimpleException(
                EDStatic.bilingual(
                    language,
                    MustBe.THERE_IS_NO_DATA + " (" + msg0 + ")",
                    MustBe.THERE_IS_NO_DATA + " (" + msgl + ")"));
          }
        }
      }
    }

    if (debugMode) {
      String2.log(
          "  Output from parseUserDapQuery:"
              + "\n    resultsVariables="
              + resultsVariables
              + "\n    constraintVariables="
              + constraintVariables
              + "\n    constraintOps="
              + constraintOps
              + "\n    constraintValues="
              + constraintValues);
    }
  }

  /**
   * As a service to getDataForDapQuery implementations, given the userDapQuery (the DESTINATION
   * (not source) request), this fills in the requestedMin and requestedMax arrays [0=lon, 1=lat,
   * 2=alt|depth, 3=time] (in destination units, may be NaN). [2] will be altIndex if >=0 or
   * depthIndex if >= 0.
   *
   * <p>If a given variable (e.g., altIndex and depthIndex) isn't defined, the requestedMin/Max will
   * be NaN.
   *
   * @param language the index of the selected language
   * @param userDapQuery a userDapQuery (usually including longitude,latitude, altitude,time
   *     variables), after the '?', still percentEncoded (shouldn't be null).
   * @param useVariablesDestinationMinMax if true, the results are further constrained by the LLAT
   *     variable's destinationMin and destinationMax.
   * @param requestedMin will catch the minimum LLAT constraints; NaN if a given var doesn't exist
   *     or no info or constraints.
   * @param requestedMax will catch the maximum LLAT constraints; NaN if a given var doesn't exist
   *     or no info or constraints.
   * @throws Throwable if trouble (e.g., a requestedMin &gt; a requestedMax)
   */
  public void getRequestedDestinationMinMax(
      int language,
      String userDapQuery,
      boolean useVariablesDestinationMinMax,
      double requestedMin[],
      double requestedMax[])
      throws Throwable {

    Arrays.fill(requestedMin, Double.NaN);
    Arrays.fill(requestedMax, Double.NaN);

    // parseUserDapQuery
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
        false);

    // try to get the LLAT variables
    EDV edv[] =
        new EDV[] {
          lonIndex >= 0 ? dataVariables[lonIndex] : null,
          latIndex >= 0 ? dataVariables[latIndex] : null,
          altIndex >= 0
              ? dataVariables[altIndex]
              : depthIndex >= 0 ? dataVariables[depthIndex] : null,
          timeIndex >= 0 ? dataVariables[timeIndex] : null
        };

    // go through the constraints
    int nConstraints = constraintVariables.size();
    for (int constraint = 0; constraint < nConstraints; constraint++) {

      String destName = constraintVariables.get(constraint);
      int conDVI = String2.indexOf(dataVariableDestinationNames(), destName);
      if (conDVI < 0)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "constraint variable="
                + destName
                + " wasn't found.");
      String op = constraintOps.get(constraint);
      String conValue = constraintValues.get(constraint);
      double conValueD = String2.parseDouble(conValue); // ok for times: they are epochSeconds

      // constraint affects which of LLAT
      int index4 = -1;
      if (conDVI == lonIndex) index4 = 0;
      else if (conDVI == latIndex) index4 = 1;
      else if (conDVI == altIndex) index4 = 2;
      else if (conDVI == depthIndex) index4 = 2;
      else if (conDVI == timeIndex) index4 = 3;
      // String2.log("index4:" + index4 + " " + op + " " + conValueD);

      if (index4 >= 0) {
        if (op.equals("=") || op.equals(">=") || op.equals(">"))
          requestedMin[index4] =
              Double.isNaN(requestedMin[index4])
                  ? conValueD
                  : Math.max(requestedMin[index4], conValueD); // yes, max
        if (op.equals("=") || op.equals("<=") || op.equals("<"))
          requestedMax[index4] =
              Double.isNaN(requestedMax[index4])
                  ? conValueD
                  : Math.min(requestedMax[index4], conValueD); // yes, min
      }
    }

    // invalid user constraints?
    for (int i = 0; i < 4; i++) {
      if (edv[i] == null) continue;
      String tName = edv[i].destinationName();
      if (reallyVerbose)
        String2.log("  " + tName + " requested min=" + requestedMin[i] + " max=" + requestedMax[i]);
      if (!Double.isNaN(requestedMin[i]) && !Double.isNaN(requestedMax[i])) {
        if (requestedMin[i] > requestedMax[i]) {
          String minS =
              i == 3 ? Calendar2.epochSecondsToIsoStringTZ(requestedMin[3]) : "" + requestedMin[i];
          String maxS =
              i == 3 ? Calendar2.epochSecondsToIsoStringTZ(requestedMax[3]) : "" + requestedMax[i];
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "The requested "
                  + tName
                  + " min="
                  + minS
                  + " is greater than the requested max="
                  + maxS
                  + ".");
        }
      }
    }

    // use the variable's destinationMin, max?
    if (!useVariablesDestinationMinMax) return;

    for (int i = 0; i < 4; i++) {
      if (edv[i] != null) {
        double tMin = edv[i].destinationMinDouble();
        if (Double.isNaN(tMin)) {
        } else {
          if (Double.isNaN(requestedMin[i])) requestedMin[i] = tMin;
          else requestedMin[i] = Math.max(tMin, requestedMin[i]);
        }
        double tMax = edv[i].destinationMaxDouble();
        if (Double.isNaN(tMax)) {
        } else {
          if (Double.isNaN(requestedMax[i])) requestedMax[i] = tMax;
          else requestedMax[i] = Math.min(tMax, requestedMax[i]);
        }
      }
    }
    if (Double.isNaN(requestedMax[3]))
      requestedMax[3] =
          Calendar2.gcToEpochSeconds(Calendar2.newGCalendarZulu())
              + Calendar2.SECONDS_PER_HOUR; // now + 1 hr
    // ???is this trouble for models which predict future?  (are any models EDDTables?)

    // recheck. If invalid now, it's No Data due to variable's destinationMin/Max
    for (int i = 0; i < 4; i++) {
      if (edv[i] == null) continue;
      String tName = edv[i].destinationName();
      if (reallyVerbose)
        String2.log("  " + tName + " requested min=" + requestedMin[i] + " max=" + requestedMax[i]);
      if (!Double.isNaN(requestedMin[i]) && !Double.isNaN(requestedMax[i])) {
        if (requestedMin[i] > requestedMax[i]) {
          String minS =
              i == 3 ? Calendar2.epochSecondsToIsoStringTZ(requestedMin[3]) : "" + requestedMin[i];
          String maxS =
              i == 3 ? Calendar2.epochSecondsToIsoStringTZ(requestedMax[3]) : "" + requestedMax[i];
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + tName
                  + " min="
                  + minS
                  + " is greater than max="
                  + maxS);
        }
      }
    }
  }

  /**
   * For diagnostic purposes only, this formats the resultsVariables and constraints as an OPeNDAP
   * DAP-style query. This does no checking of the validity of the resultsVariable or
   * constraintVariable names, so, e.g., axisVariables may be referred to by the sourceNames or the
   * destinationNames, so this method can be used in various ways.
   *
   * @param resultsVariables
   * @param constraintVariables
   * @param constraintOps are usually the standard OPERATORS (or ~=, the non-standard regex op)
   * @param constraintValues
   * @return the OPeNDAP DAP-style query,
   *     <tt>var1,var2,var3&amp;var4=value4&amp;var5&amp;gt;=value5</tt>. See parseUserDapQuery.
   */
  public static String formatAsDapQuery(
      String resultsVariables[],
      String constraintVariables[],
      String constraintOps[],
      String constraintValues[]) {

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < resultsVariables.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(resultsVariables[i]);
    }

    // do I need to put quotes around String constraintValues???
    for (int i = 0; i < constraintVariables.length; i++) {
      String cv = constraintValues[i];
      if (!String2.isNumber(cv)) // good, not perfect
      cv = String2.toJson(cv, 256);
      sb.append("&" + constraintVariables[i] + constraintOps[i] + cv);
    }
    return sb.toString();
  }

  /** This is an alternate formatAsDapQuery that works with StringArrays. */
  public static String formatAsDapQuery(
      StringArray resultsVariables,
      StringArray constraintVariables,
      StringArray constraintOps,
      StringArray constraintValues) {
    return formatAsDapQuery(
        resultsVariables.toArray(),
        constraintVariables.toArray(),
        constraintOps.toArray(),
        constraintValues.toArray());
  }

  /** This is similar to formatAsDapQuery, but properly SSR.percentEncodes the parts. */
  public static String formatAsPercentEncodedDapQuery(
      String resultsVariables[],
      String constraintVariables[],
      String constraintOps[],
      String constraintValues[])
      throws Throwable {

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < resultsVariables.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(SSR.minimalPercentEncode(resultsVariables[i]));
    }

    for (int i = 0; i < constraintVariables.length; i++)
      sb.append(
          "&"
              + SSR.minimalPercentEncode(constraintVariables[i])
              + PPE_OPERATORS.get(OPERATORS.indexOf(constraintOps[i]))
              + SSR.minimalPercentEncode(
                  constraintValues[
                      i])); // !!!bug??? Strings should be String2.toJson() so within " "

    return sb.toString();
  }

  /** This is overwritten by EDDTableFromHttpGet and used to insert/delete data in the dataset. */
  public void insertOrDelete(
      int language,
      int command,
      HttpServletRequest request,
      HttpServletResponse response,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      OutputStreamSource outputStreamSource,
      String fileTypeName)
      throws Throwable {

    throw new SimpleException(
        EDStatic.bilingual(
            language,
            EDStatic.messages.get(Message.QUERY_ERROR, 0)
                + MessageFormat.format(
                    EDStatic.messages.get(Message.QUERY_ERROR_FILE_TYPE, 0), fileTypeName),
            EDStatic.messages.get(Message.QUERY_ERROR, language)
                + MessageFormat.format(
                    EDStatic.messages.get(Message.QUERY_ERROR_FILE_TYPE, language), fileTypeName)));
  }

  /**
   * This responds to an OPeNDAP-style query.
   *
   * @param language the index of the selected language
   * @param request may be null. Currently, it isn't used. (It is passed to respondToGraphQuery, but
   *     not used).
   * @param response may be used by .subset to redirect the response (if not .subset request, it may
   *     be null).
   * @param ipAddress The IP address of the user (for statistics).
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but in unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded
   *     (shouldn't be null).
   * @param outputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. Now, this usually does call out.close() at the end if all goes well, but
   *     still assumes caller will close it (notably if trouble).
   * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
   * @param fileName the name for the 'file' (no dir, no extension), which is used to write the
   *     suggested name for the file to the response header.
   * @param fileTypeName the fileTypeName for the new file (e.g., .largePng)
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

    if (reallyVerbose)
      String2.log(
          "\n//** EDDTable.respondToDapQuery..."
              + "\n  datasetID="
              + datasetID
              + "\n  userDapQuery="
              + userDapQuery
              + "\n  dir="
              + dir
              + "\n  fileName="
              + fileName
              + "\n  fileTypeName="
              + fileTypeName);
    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);

    // special EDDTableFromHttpGet file types
    if (this instanceof EDDTableFromHttpGet etfhg
        && (fileTypeName.equals(".insert") || fileTypeName.equals(".delete"))) {
      if (!EDStatic.config.developmentMode && loggedInAs == null)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + fileTypeName
                + " requests must be made to the https URL.");

      String jsonResponse =
          etfhg.insertOrDelete(
              language, // throws exception if trouble
              fileTypeName.equals(".insert")
                  ? EDDTableFromHttpGet.INSERT_COMMAND
                  : EDDTableFromHttpGet.DELETE_COMMAND,
              userDapQuery);
      Writer writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
      writer.write(jsonResponse);
      writer.flush(); // essential

      return;
    }

    // Handle rendering type queries
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
        // first
        Table table =
            makeEmptyDestinationTable(language, requestUrl, "", true); // das is as if no constraint
        // it is important that this use outputStreamSource so stream is compressed (if possible)
        // DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible unicode
        // With HTML 5 and for future, best to go with UTF_8. Also, <startHeadHtml> says UTF_8.
        OutputStream out = outputStreamSource.outputStream(File2.UTF_8);
        Writer writer = File2.getBufferedWriterUtf8(out);
        try {
          writer.write(
              EDStatic.startHeadHtml(
                  language,
                  tErddapUrl,
                  title(language) + " - " + EDStatic.messages.get(Message.DAF, language)));
          writer.write("\n" + rssHeadLink(language));
          writer.write("\n</head>\n");
          writer.write(
              EDStatic.startBodyHtml(
                  request,
                  language,
                  loggedInAs,
                  "tabledap/" + datasetID + ".html", // was endOfRequest,
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
          writer
              .flush(); // Steve Souder says: the sooner you can send some html to user, the better
          writer.write("<div class=\"standard_width\">\n");
          writer.write(
              EDStatic.youAreHereWithHelp(
                  request,
                  language,
                  loggedInAs,
                  dapProtocol,
                  EDStatic.messages.get(Message.DAF, language),
                  "<div class=\"standard_max_width\">"
                      + EDStatic.messages.get(Message.DAF_TABLE_TOOLTIP, language)
                      + "<p>"
                      + EDStatic.messages.get(Message.EDD_TABLE_DOWNLOAD_DATA_TOOLTIP, language)
                      + "</ol>\n"
                      + EDStatic.messages.get(Message.DAF_TABLE_BYPASS_TOOLTIP, language)
                      + "</div>"));
          writeHtmlDatasetInfo(
              request, language, loggedInAs, writer, true, false, true, true, userDapQuery, "");
          if (userDapQuery.length() == 0)
            userDapQuery =
                defaultDataQuery(); // after writeHtmlDatasetInfo and before writeDapHtmlForm
          writeDapHtmlForm(request, language, loggedInAs, userDapQuery, writer);

          // info at end of page

          // then das (with info about this dataset
          writer.write("<hr>\n");
          writer.write(
              "<h2><a class=\"selfLink\" id=\"DAS\" href=\"#DAS\" rel=\"bookmark\">"
                  + EDStatic.messages.get(Message.DAS_TITLE, language)
                  + "</a></h2>\n"
                  + "<pre style=\"white-space:pre-wrap;\">\n");
          table.writeDAS(
              writer, SEQUENCE_NAME, true); // useful so search engines find all relevant words
          writer.write("</pre>\n");

          // then dap instructions
          writer.write("<br>&nbsp;\n");
          writer.write("<hr>\n");
          writeGeneralDapHtmlInstructions(language, tErddapUrl, writer, false);
          writer.write("</div>\n");
          writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
          writer.write("\n</html>\n");
          writer.flush(); // essential
          return;

        } catch (Exception e) {
          EDStatic.rethrowClientAbortException(e); // first thing in catch{}
          String2.log(
              String2.ERROR
                  + " when writing web page:\n"
                  + MustBe.throwableToString(e)); // before writer.write's
          writer.write(EDStatic.htmlForException(language, e));
          writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
          writer.write("\n</html>\n");
          writer.flush(); // essential
          throw e;
        }
      }
      case ".subset" -> {
        respondToSubsetQuery(
            language,
            request,
            response,
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
                EDStatic.messages.get(Message.QUERY_ERROR, 0)
                    + EDStatic.messages.get(Message.ERROR_FILE_NOT_FOUND_IMAGE, 0),
                EDStatic.messages.get(Message.QUERY_ERROR, language)
                    + EDStatic.messages.get(Message.ERROR_FILE_NOT_FOUND_IMAGE, language)));

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
            getNewHistory(language, requestUrl, userDapQuery),
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
      fileTypeHandler.writeTableToStream(requestInfo);
      return;
    }
  }

  /**
   * If a part of the userDapQuery is a function (e.g., distinct(), orderBy("...")) this wraps
   * tableWriter in (e.g.) TableWriterDistinct in the order they occur in userDapQuery.
   *
   * @param language the index of the selected language
   * @param alwaysDoAll if true, this ignores sourceCanDoDistinct and sourceCanOrderBy and always
   *     encloses with all the needed tableWriters (e.g., for handling
   *     handleViaFixedOrSubsetVariables()).
   * @param dir a private cache directory for storing the intermediate files
   * @param fileName is the fileName without dir or extension (used as basis for temp files).
   * @param tableWriter the final tableWriter (e.g., TableWriterJson)
   * @param userDapQuery
   * @return the (possibly) wrapped tableWriter (or the same tableWriter if no need to enclose it,
   *     e.g., no orderByMax).
   */
  public TableWriter encloseTableWriter(
      int language,
      boolean alwaysDoAll,
      String dir,
      String fileName,
      TableWriter tableWriter,
      String requestUrl,
      String userDapQuery)
      throws Throwable {

    String tNewHistory = getNewHistory(language, requestUrl, userDapQuery);
    String[] parts = Table.getDapQueryParts(userDapQuery); // decoded

    // identify skipPart (if any) and firstActiveDistinctOrOrderBy (if any)
    // the logic here is complex!   So this is written for clarity, not fewest operations.
    int skipPart =
        -1; // skip if first distinct() or orderBy...() AND will be handled by source  -1=still
    // looking -2=no longer looking
    int firstActiveDistinctOrOrderBy =
        -1; // it gets metadata from edd, others from first addSome table
    for (int part = 0; part < parts.length; part++) {
      String p = parts[part];

      if (p.equals("distinct()")) {
        if (skipPart == -1) {
          if (!alwaysDoAll && sourceCanDoDistinct == CONSTRAIN_YES) skipPart = part;
          else skipPart = -2; // no longer looking
        }
        if (skipPart != part && firstActiveDistinctOrOrderBy == -1) {
          firstActiveDistinctOrOrderBy = part;
          break;
        }
      } else if (p.startsWith("orderBy(") && p.endsWith(")")) { // just simple sort
        if (skipPart == -1) {
          if (!alwaysDoAll && sourceCanOrderBy == CONSTRAIN_YES) skipPart = part;
          else skipPart = -2; // no longer looking
        }
        if (skipPart != part && firstActiveDistinctOrOrderBy == -1) {
          firstActiveDistinctOrOrderBy = part;
          break;
        }
      } else if ((p.startsWith("orderBy") || p.startsWith("units("))
          && // other orderBy options or units()
          p.endsWith(")")) {
        if (skipPart == -1) skipPart = -2; // no longer looking
        if (firstActiveDistinctOrOrderBy == -1) {
          firstActiveDistinctOrOrderBy = part;
          break;
        }
      }
    }
    // String2.pressEnterToContinue(">>> firstDistinctOrOrderBy=" + firstDistinctOrOrderBy);

    // work backwards through parts so tableWriters are added in correct order
    for (int part = parts.length - 1; part >= 0; part--) {
      if (part == skipPart) continue;
      String p = parts[part];
      if (p.equals("distinct()")) {
        tableWriter =
            new TableWriterDistinct(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter);

      } else if (p.startsWith("orderBy(\"") && p.endsWith("\")")) {
        TableWriterOrderBy twob =
            new TableWriterOrderBy(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(9, p.length() - 2));
        tableWriter = twob;
        // minimal test: ensure orderBy columns are valid column names
        for (int ob = 0; ob < twob.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twob.orderBy[ob]) < 0) {
            twob.close();
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "'orderBy' variable="
                    + twob.orderBy[ob]
                    + " isn't in the dataset.");
          }
        }
      } else if (p.startsWith("orderByDescending(\"") && p.endsWith("\")")) {
        TableWriterOrderByDescending twobd =
            new TableWriterOrderByDescending(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(19, p.length() - 2));
        tableWriter = twobd;
        // minimal test: ensure orderBy columns are valid column names
        for (int ob = 0; ob < twobd.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobd.orderBy[ob]) < 0) {
            twobd.close();
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "'orderByDescending' variable="
                    + twobd.orderBy[ob]
                    + " isn't in the dataset.");
          }
        }
      } else if (p.startsWith("orderByClosest(\"") && p.endsWith("\")")) {
        TableWriterOrderByClosest twobc =
            new TableWriterOrderByClosest(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(16, p.length() - 2));
        tableWriter = twobc;
        // minimal test: ensure orderBy columns (except last) are valid column names
        for (int ob = 0; ob < twobc.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobc.orderBy[ob].split("/")[0])
              < 0) {
            twobc.close();
            throw new SimpleException(
                EDStatic.bilingual(
                        language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_CLOSEST)
                    + "\nunknown column name="
                    + twobc.orderBy[ob]
                    + ".");
          }
        }

      } else if (p.startsWith("orderByCount(\"") && p.endsWith("\")")) {
        TableWriterOrderByCount twobc =
            new TableWriterOrderByCount(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(14, p.length() - 2));
        tableWriter = twobc;
        // minimal test: ensure orderBy columns are valid column names
        for (int ob = 0; ob < twobc.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobc.orderBy[ob].split("/")[0])
              < 0) {
            twobc.close();
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "'orderByCount' variable="
                    + twobc.orderBy[ob]
                    + " isn't in the dataset.");
          }
        }

      } else if (p.startsWith("orderByLimit(\"") && p.endsWith("\")")) {
        TableWriterOrderByLimit twobl =
            new TableWriterOrderByLimit(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(14, p.length() - 2));
        tableWriter = twobl;
        // minimal test: ensure orderBy columns (except last) are valid column names
        for (int ob = 0; ob < twobl.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobl.orderBy[ob].split("/")[0])
              < 0) {
            twobl.close();
            throw new SimpleException(
                EDStatic.bilingual(
                        language, Message.QUERY_ERROR, Message.QUERY_ERROR_ORDER_BY_LIMIT)
                    + "\nunknown column name="
                    + twobl.orderBy[ob]
                    + ".");
          }
        }
      } else if (p.startsWith("orderByMean(\"") && p.endsWith("\")")) {
        tableWriter =
            new TableWriterOrderByMean(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(13, p.length() - 2));
      } else if (p.startsWith("orderBySum(\"") && p.endsWith("\")")) {
        tableWriter =
            new TableWriterOrderBySum(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(12, p.length() - 2));
      } else if (p.startsWith("orderByMax(\"") && p.endsWith("\")")) {
        TableWriterOrderByMax twobm =
            new TableWriterOrderByMax(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(12, p.length() - 2));
        tableWriter = twobm;
        // minimal test: ensure orderBy columns are valid column names
        for (int ob = 0; ob < twobm.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobm.orderBy[ob].split("/")[0])
              < 0) {
            twobm.close();
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "'orderByMax' variable="
                    + twobm.orderBy[ob]
                    + " isn't in the dataset.");
          }
        }

      } else if (p.startsWith("orderByMin(\"") && p.endsWith("\")")) {
        TableWriterOrderByMin twobm =
            new TableWriterOrderByMin(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(12, p.length() - 2));
        tableWriter = twobm;
        // minimal test: ensure orderBy columns are valid column names
        for (int ob = 0; ob < twobm.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobm.orderBy[ob].split("/")[0])
              < 0) {
            twobm.close();
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "'orderByMin' variable="
                    + twobm.orderBy[ob]
                    + " isn't in the dataset.");
          }
        }
      } else if (p.startsWith("orderByMinMax(\"") && p.endsWith("\")")) {
        TableWriterOrderByMinMax twobm =
            new TableWriterOrderByMinMax(
                language,
                part == firstActiveDistinctOrOrderBy ? this : null,
                tNewHistory,
                dir,
                fileName,
                tableWriter,
                p.substring(15, p.length() - 2));
        tableWriter = twobm;
        // minimal test: ensure orderBy columns are valid column names
        for (int ob = 0; ob < twobm.orderBy.length; ob++) {
          if (String2.indexOf(dataVariableDestinationNames(), twobm.orderBy[ob].split("/")[0])
              < 0) {
            twobm.close();
            throw new SimpleException(
                EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                    + "'orderByMinMax' variable="
                    + twobm.orderBy[ob]
                    + " isn't in the dataset.");
          }
        }
      } else if (p.startsWith("units(\"") && p.endsWith("\")")) {
        String fromUnits = EDStatic.config.units_standard;
        String toUnits = p.substring(7, p.length() - 2);
        TableWriterUnits.checkFromToUnits(language, fromUnits, toUnits);
        if (!fromUnits.equals(toUnits)) {
          tableWriter =
              new TableWriterUnits(
                  language,
                  part == firstActiveDistinctOrOrderBy ? this : null,
                  tNewHistory,
                  tableWriter,
                  fromUnits,
                  toUnits);
        }
      }
    }

    return tableWriter;
  }

  /**
   * This makes an empty table (with destination columns, but without rows) corresponding to the
   * request.
   *
   * @param language the index of the selected language
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery the part after the '?', still percentEncoded (shouldn't be null).
   * @param withAttributes
   * @return an empty table (with columns, but without rows) corresponding to the request
   * @throws Throwable if trouble
   */
  public Table makeEmptyDestinationTable(
      int language, String requestUrl, String userDapQuery, boolean withAttributes)
      throws Throwable {
    if (reallyVerbose) String2.log("  makeEmptyDestinationTable...");

    // pick the query apart
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
        false);

    // make the table
    // attributes are added in standardizeResultsTable
    Table table = new Table();
    if (withAttributes) {
      setResponseGlobalAttributes(language, requestUrl, userDapQuery, table);

      // pre 2013-05-28 was
      // table.globalAttributes().set(combinedGlobalAttributes); //make a copy
      //// fix up global attributes  (always to a local COPY of global attributes)
      // EDD.addToHistory(table.globalAttributes(), publicSourceUrl());
      // EDD.addToHistory(table.globalAttributes(),
      //    EDStatic.config.baseUrl + requestUrl); //userDapQuery is irrelevant
    }

    // add the columns
    for (int col = 0; col < resultsVariables.size(); col++) {
      int dv = String2.indexOf(dataVariableDestinationNames(), resultsVariables.get(col));
      EDV edv = dataVariables[dv];
      Attributes atts = new Attributes();
      if (withAttributes) atts.set(edv.combinedAttributes().toAttributes(language)); // make a copy
      table.addColumn(
          col,
          edv.destinationName(),
          PrimitiveArray.factory(edv.destinationDataPAType(), 1, false),
          atts);
    }

    // can't setActualRangeAndBoundingBox because no data
    // so remove actual_range and bounding box attributes???
    //  no, they are dealt with in standardizeResultsTable

    if (reallyVerbose) String2.log("  makeEmptyDestinationTable done.");
    return table;
  }

  /**
   * Convert JEXL script columns into data columns. If there are errors doing the conversions, the
   * first error per scriptName will be logged.
   *
   * @param fullFileName The full name (perhaps a URL) of the current file, or "" if the source is
   *     not file-like.
   * @param table The raw table from the source to which scriptName columns will be added.
   * @param scriptNames the sourceNames of the script columns to be added. If this is null, nothing
   *     will be done.
   * @param scriptTypes the dataTypes which parallel the scriptNames
   * @param scriptNeedsColumns The map which provides information about which scriptName needs which
   *     actual source columns
   */
  public static void convertScriptColumnsToDataColumns(
      String fullFileName,
      Table table,
      StringArray scriptNames,
      StringArray scriptTypes,
      Map<String, Set<String>> scriptNeedsColumns) {

    if (scriptNames != null) {
      // if (debugMode) String2.log(">> raw table:\n" + table.dataToString(5));
      int nRows = table.nRows();
      for (int sni = 0; sni < scriptNames.size(); sni++) {
        PrimitiveArray pa =
            PrimitiveArray.factory(
                PAType.fromCohortString(scriptTypes.get(sni)), nRows, false); // active?
        JexlScript jscript = Script2.jexlEngine().createScript(scriptNames.get(sni).substring(1));
        MapContext jcontext = Script2.jexlMapContext();
        ScriptRow scriptRow = new ScriptRow(fullFileName, table);
        jcontext.set("row", scriptRow);
        boolean firstError = true;

        if (scriptNeedsColumns.get(scriptNames.get(sni)).size() == 0) {
          // script doesn't refer to any columns (e.g., =10.0),
          // so just parse once and duplicate that value.
          // scriptRow.setRow(0); //already done
          Object o = null;
          try {
            o = jscript.execute(jcontext);
          } catch (Exception e2) {
            if (firstError) {
              String2.log(
                  "Caught: first script error (for col="
                      + String2.toJson(scriptNames.get(sni))
                      + " row[0]):\n"
                      + MustBe.throwableToString(e2));
              firstError = false;
            }
            o = null;
          }
          for (int row = 0; row < nRows; row++) pa.addObject(o);

        } else {
          for (int row = 0; row < nRows; row++) {
            scriptRow.setRow(row);
            Object o = null;
            try {
              o = jscript.execute(jcontext);
            } catch (Exception e2) {
              if (firstError) {
                String2.log(
                    "Caught: first script error (for col="
                        + String2.toJson(scriptNames.get(sni))
                        + " row["
                        + row
                        + "]):\n"
                        + MustBe.throwableToString(e2));
                firstError = false;
              }
              o = null;
            }
            pa.addObject(o);
            // if (debugMode && row < 5) String2.log(">> row[" + row + "] o.class().getName()=" +
            // o.getClass().getName() + " value=" + (o instanceof Number? ((Number)o).doubleValue()
            // : o.toString()));
          }
        }
        table.addColumn(scriptNames.get(sni), pa);
      }
    }
  }

  /**
   * Save this table of data as a flat netCDF .nc file (a column for each variable, all referencing
   * one dimension) using the currently available attributes. <br>
   * The data are written as separate variables, sharing a common dimension "observation", not as a
   * Structure. <br>
   * The data values are written as their current data type (e.g., float or int). <br>
   * This writes the lon values as they are currently in this table (e.g., +-180 or 0..360). <br>
   * This overwrites any existing file of the specified name. <br>
   * This makes an effort not to create a partial file if there is an error. <br>
   * If no exception is thrown, the file was successfully created. <br>
   * !!!The file must have at least one row, or an Exception will be thrown (nc dimensions can't be
   * 0 length). <br>
   * !!!Missing values should be destinationMissingValues or destinationFillValues and aren't
   * changed.
   *
   * @param language the index of the selected language
   * @param ncVersion either NetcdfFileFormat.NETCDF3 or NETCDF4.
   * @param fullName the full file name (dir+name+ext)
   * @param twawm provides access to all of the data
   * @throws Throwable if trouble (e.g., TOO_MUCH_DATA)
   */
  public void saveAsFlatNc(
      int language, NetcdfFileFormat ncVersion, String fullName, TableWriterAllWithMetadata twawm)
      throws Throwable {
    if (reallyVerbose) String2.log("  EDDTable.saveAsFlatNc " + fullName);
    long time = System.currentTimeMillis();

    if (ncVersion != NetcdfFileFormat.NETCDF3 && ncVersion != NetcdfFileFormat.NETCDF4)
      throw new RuntimeException("ERROR: unsupported ncVersion=" + ncVersion);
    boolean nc3Mode = ncVersion == NetcdfFileFormat.NETCDF3;

    // delete any existing file
    File2.delete(fullName);

    // test for too much data   //??? true for nc4?
    if (twawm.nRows() >= Integer.MAX_VALUE)
      throw new SimpleException(
          Math2.memoryTooMuchData
              + "  "
              + MessageFormat.format(
                  EDStatic.messages.get(Message.ERROR_MORE_THAN_2GB, 0),
                  ".nc",
                  twawm.nRows() + " " + EDStatic.messages.get(Message.ROWS, 0)));
    int nRows = (int) twawm.nRows(); // safe since checked above
    int nColumns = twawm.nColumns();
    if (nRows == 0)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (at start of saveAsFlatNc)");

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFormatWriter ncWriter = null;
    try {
      NetcdfFormatWriter.Builder nc =
          ncVersion == NetcdfFileFormat.NETCDF3
              ? NetcdfFormatWriter.createNewNetcdf3(fullName + randomInt)
              : NetcdfFormatWriter.createNewNetcdf4(
                  ncVersion, fullName + randomInt, null); // null=default chunker
      nc.setFill(false);
      Group.Builder rootGroup = nc.getRootGroup();

      // items determined by looking at a .nc file; items written in that order

      // define the dimensions
      Dimension rowDimension = NcHelper.addDimension(rootGroup, ROW_NAME, nRows);
      // javadoc says: if there is an unlimited dimension, all variables that use it are in a
      // structure
      // Dimension rowDimension  = new Dimension("row", nRows, true, true, false); //isShared,
      // isUnlimited, isUnknown
      // String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

      // add the variables
      Variable.Builder newVars[] = new Variable.Builder[nColumns];
      for (int col = 0; col < nColumns; col++) {
        PAType type = twawm.columnType(col);
        String tColName = twawm.columnName(col);
        if (nc3Mode && type == PAType.STRING) {
          int max =
              Math.max(
                  1,
                  twawm.columnMaxStringLength(
                      col)); // nc libs want at least 1; 0 happens if no data
          newVars[col] =
              NcHelper.addNc3StringVariable(rootGroup, tColName, List.of(rowDimension), max);
        } else {
          newVars[col] =
              NcHelper.addVariable(
                  rootGroup, tColName, NcHelper.getDataType(nc3Mode, type), rowDimension);
        }
      }

      // if no id, set id=datasetID
      // currently, only the .nc saveAs types use attributes!
      Attributes globalAttributes = twawm.globalAttributes();
      if (globalAttributes.get("id") == null)
        globalAttributes.set("id", datasetID); // 2019-05-07 File2.getNameNoExtension(fullName));

      // set the attributes
      NcHelper.setAttributes(nc3Mode, rootGroup, globalAttributes);
      for (int col = 0; col < nColumns; col++) {
        Attributes tAtts = new Attributes(twawm.columnAttributes(col)); // use a copy
        PAType paType = twawm.columnType(col);
        if (paType == PAType.STRING) tAtts.add(File2.ENCODING, File2.ISO_8859_1);
        // disabled until there is a standard
        //                else if (paType == PAType.CHAR)
        //                    tAtts.add(String2.CHARSET, File2.ISO_8859_1);

        NcHelper.setAttributes(nc3Mode, newVars[col], tAtts, paType.isUnsigned());
      }

      // leave "define" mode
      ncWriter = nc.build();

      // write the data
      for (int col = 0; col < nColumns; col++) {
        // missing values are already destinationMissingValues or destinationFillValues
        int nToGo = nRows;
        int ncOffset = 0;
        int bufferSize = EDStatic.config.partialRequestMaxCells;
        PrimitiveArray pa = null;
        try (DataInputStream dis = twawm.dataInputStream(col)) {
          PAType colType = twawm.columnType(col);
          Array array;

          while (nToGo > 0) {
            bufferSize = Math.min(nToGo, bufferSize); // actual number to be transferred
            // use of != below (not <) lets toObjectArray below return internal array since
            // size=capacity
            if (pa == null || pa.elementType() != twawm.columnType(col)) {
              pa = twawm.columnEmptyPA(col);
              pa.ensureCapacity(bufferSize);
            }
            pa.clear();
            pa.setMaxIsMV(twawm.columnMaxIsMV(col)); // reset it after clear()
            pa.readDis(dis, bufferSize);
            if (debugMode)
              String2.log(
                  ">> col="
                      + col
                      + " nToGo="
                      + nToGo
                      + " ncOffset="
                      + ncOffset
                      + " bufferSize="
                      + bufferSize
                      + " pa.capacity="
                      + pa.capacity()
                      + " pa.size="
                      + pa.size());
            Variable newVar =
                ncWriter.findVariable(
                    newVars[col].getFullName()); // because newVars are Variable.Builder's
            if (nc3Mode && pa instanceof StringArray tsa) {
              // pa is temporary, so ok to change strings
              array =
                  Array.factory(
                      NcHelper.getNc3DataType(colType),
                      new int[] {bufferSize},
                      tsa.toIso88591().toObjectArray());
              ncWriter.writeStringDataToChar(newVar, new int[] {ncOffset}, array);

            } else {
              if (nc3Mode && (pa instanceof LongArray || pa instanceof ULongArray)) {
                // String2.log(">> saveAsFlatNc twawm=" + twawm + " colName=" +
                // twawm.columnName(col) + " maxIsMV=" + pa.getMaxIsMV() + " sourcePA=" +
                // pa.toString());
                pa = new DoubleArray(pa);
                // String2.log(">> saveAsFlatNc twawm=" + twawm + " colName=" +
                // twawm.columnName(col) + " doublePA=" + pa.toString());
              }

              if (pa instanceof CharArray ca) {
                // pa is temporary, so ok to change chars
                array =
                    Array.factory(
                        DataType.CHAR, new int[] {bufferSize}, ca.toIso88591().toObjectArray());
              } else {
                array =
                    Array.factory(
                        NcHelper.getNc3DataType(colType),
                        new int[] {bufferSize},
                        pa.toObjectArray());
              }
              ncWriter.write(newVar, new int[] {ncOffset}, array);
            }

            nToGo -= bufferSize;
            ncOffset += bufferSize;
            // String2.log("col=" + col + " bufferSize=" + bufferSize + " isString?" + (colType ==
            // PAType.STRING));
          }
        }
      }

      // if close throws Throwable, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name
      File2.rename(fullName + randomInt, fullName);

      // diagnostic
      if (reallyVerbose)
        String2.log(
            "  EDDTable.saveAsFlatNc done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
      // String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

    } catch (Throwable t) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullName + randomInt);
        ncWriter = null;
      }

      throw t;
    }
  }

  /** Ensure a conventions string includes "CF-1.10" (or already >=1.10). */
  public static String ensureAtLeastCF110(String conv) {
    if (conv == null || conv.length() == 0) {
      conv = "CF-1.10";
    } else if (conv.indexOf("CF-") < 0) {
      conv += ", CF-1.10";
    } else if (String2.extractRegex(conv, "CF[ \\-]\\d\\.\\d\\d", 0)
        == null) { // doesn't already have CF-1.xx
      conv = conv.replaceFirst("CF[ \\-]\\d\\.\\d", "CF-1.10"); // replace 1.x with 1.10
    }
    return conv;
  }

  /**
   * Create a Point .ncCF file (Discrete Sampling Geometries). Specifically: <br>
   * "Point Data" at
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries
   * <br>
   * This assumes cdm_data_type=Point.
   *
   * <p>This also handles .ncCFMA. No changes needed.
   *
   * @param language the index of the selected language
   * @param ncCFName the complete name of the file that will be created
   * @param twawm
   * @throws Throwable if trouble
   */
  public void saveAsNcCF0(int language, String ncCFName, TableWriterAllWithMetadata twawm)
      throws Throwable {
    if (reallyVerbose) String2.log("EDDTable.convertFlatNcToNcCF0 " + ncCFName);

    // delete any existing file
    File2.delete(ncCFName);

    // double check that this dataset supports .ncCF (but it should have been tested earlier)
    if (accessibleViaNcCF.length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaNcCF);
    String cdmType = combinedGlobalAttributes.getString(language, "cdm_data_type");
    if (!CDM_POINT.equals(cdmType)) // but already checked before calling this method
    throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "cdm_data_type for convertFlatNcToNcCF0() must be Point, not "
              + cdmType
              + ".");

    // add metadata
    Attributes globalAtts = twawm.globalAttributes();
    globalAtts.set("featureType", "Point");
    globalAtts.set("Conventions", ensureAtLeastCF110(globalAtts.getString("Conventions")));

    // set the variable attributes
    // section 9.1.1 says "The coordinates attribute must identify the variables
    //  needed to geospatially position and time locate the observation.
    //  The lat, lon and time coordinates must always exist; a vertical
    //  coordinate may exist."
    // so always include time, lat, lon, even if they use obs dimension.
    String ncColNames[] = twawm.columnNames();
    int ncNCols = ncColNames.length;
    boolean isCoordinateVar[] = new boolean[ncNCols]; // all false
    isCoordinateVar[String2.indexOf(ncColNames, "longitude")] = true;
    isCoordinateVar[String2.indexOf(ncColNames, "latitude")] = true;
    isCoordinateVar[String2.indexOf(ncColNames, "time")] = true;
    String coordinates = "time latitude longitude"; // spec doesn't specify a specific order
    String proxy = combinedGlobalAttributes.getString(language, "cdm_altitude_proxy");
    if (altIndex >= 0) {
      coordinates += " altitude";
      isCoordinateVar[String2.indexOf(ncColNames, "altitude")] = true;
    } else if (depthIndex >= 0) {
      coordinates += " depth";
      isCoordinateVar[String2.indexOf(ncColNames, "depth")] = true;
    } else if (proxy != null) {
      // it is in requiredCfRequestVariables
      coordinates += " " + proxy;
      isCoordinateVar[String2.indexOf(ncColNames, proxy)] = true;
    }
    for (int col = 0; col < ncNCols; col++) {
      Attributes atts = twawm.columnAttributes(col);
      if (!isCoordinateVar[col]) atts.add("coordinates", coordinates);
    }

    // save as .nc (it writes to randomInt temp file, then to fullName)
    saveAsFlatNc(language, NetcdfFileFormat.NETCDF3, ncCFName, twawm);
  }

  /**
   * Create a one-level TimeSeries, Trajectory, or Profile .ncCF or .ncCFMA file (Discrete Sampling
   * Geometries).
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries
   * <br>
   * This assumes cdm_data_type=TimeSeries|Trajectory|Profile and cdm_timeseries_variables,
   * cdm_trajectory_variables, or cdm_profile_variables are defined correctly.
   *
   * @param language the index of the selected language
   * @param nodcMode If true, data will be saved in .ncCFMA file conforming to NODC Templates
   *     https://www.ncei.noaa.gov/netcdf-templates with Multidimensional Array Representation. If
   *     false, data will be saved in .ncCF file with Contiguous Ragged Array Representation.
   * @param ncCFName the complete name of the file that will be created
   * @param twawm
   * @throws Throwable if trouble
   */
  public void saveAsNcCF1(
      int language, boolean nodcMode, String ncCFName, TableWriterAllWithMetadata twawm)
      throws Throwable {
    if (reallyVerbose) String2.log("EDDTable.convertFlatNcToNcCF1 " + ncCFName);
    long time = System.currentTimeMillis();
    String rowSizeName = "rowSize";
    boolean nc3Mode = true;

    // delete any existing file
    File2.delete(ncCFName);

    // double check that this dataset supports .ncCF (but it should have been tested earlier)
    if (accessibleViaNcCF.length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaNcCF);
    String cdmType = combinedGlobalAttributes.getString(language, "cdm_data_type");
    String lcCdmType = cdmType == null ? null : cdmType.toLowerCase();

    // ensure profile_id|timeseries_id|trajectory_id variable is defined
    // and that cdmType is valid
    int idDVI = -1;
    if (CDM_PROFILE.equals(cdmType)) idDVI = profile_idIndex;
    else if (CDM_TIMESERIES.equals(cdmType)) idDVI = timeseries_idIndex;
    else if (CDM_TRAJECTORY.equals(cdmType)) idDVI = trajectory_idIndex;
    else {
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + // but already checked before calling this method
              "For convertFlatNcToNcCF1(), cdm_data_type must be TimeSeries, Profile, or Trajectory, not "
              + cdmType
              + ".");
    }

    if (idDVI < 0) // but already checked by accessibleViaNcCF
    throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "No variable has a cf_role="
              + lcCdmType
              + "_id attribute.");
    String idName = dataVariableDestinationNames()[idDVI]; // var name of ..._id var

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    NetcdfFormatWriter ncWriter = null;
    try {
      // get info from twawm
      // Note: this uses "feature" as stand-in for profile|timeseries|trajectory
      String ncColNames[] = twawm.columnNames();
      // String2.log("  ncColNames=" + String2.toCSSVString(ncColNames));
      int ncNCols = twawm.nColumns();
      boolean isFeatureVar[] = new boolean[ncNCols];
      String featureVars[] =
          combinedGlobalAttributes.getStringsFromCSV(language, "cdm_" + lcCdmType + "_variables");
      EDV colEdv[] = new EDV[ncNCols];
      for (int col = 0; col < ncNCols; col++) {
        isFeatureVar[col] = String2.indexOf(featureVars, ncColNames[col]) >= 0;
        colEdv[col] = findVariableByDestinationName(ncColNames[col]);
      }

      // ensure profile_id|timeseries_id|trajectory_id variable is in flatNc file
      int idPo = String2.indexOf(ncColNames, idName);
      if (idPo < 0) // but already checked before calling this method
      throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "The .nc file must have "
                + idName);

      // read id PA from flatNc file
      PrimitiveArray idPA = twawm.column(idPo);
      // String2.log("  idPA=" + idPA.toString());

      // rank id rows
      // !!! this assumes already sorted by time/depth/... and will stay sorted when rearranged.
      //  PrimitiveArray.rank promises stable sort.
      ArrayList<PrimitiveArray> tTable = new ArrayList<>();
      tTable.add(idPA);
      int rank[] = PrimitiveArray.rank(tTable, new int[] {0}, new boolean[] {true});
      tTable = null;
      // String2.log("  rank=" + String2.toCSSVString(rank));

      // find unique features (e.g., station|profile|trajectory):
      int totalNObs = idPA.size();
      // first row has first feature (first row in CF file for each feature)
      IntArray featureFirstRow = new IntArray(); // 'row' after data is sorted
      featureFirstRow.add(0);
      int prevFirstRow = 0; // 'row' after data is sorted
      IntArray featureNRows = new IntArray();
      BitSet featureKeep = new BitSet(); // initially all false
      featureKeep.set(0); // 'row' after data is sorted
      // 'row' after data is sorted
      for (int row = 1; row < totalNObs; row++) { // 1 since comparing to previous row
        if (idPA.compare(rank[row - 1], rank[row]) != 0) {
          // found a new feature
          featureFirstRow.add(row); // for this feature
          featureNRows.add(row - prevFirstRow); // for the previous feature
          featureKeep.set(row);
          prevFirstRow = row;
        }
      }
      featureNRows.add(totalNObs - prevFirstRow);
      int maxFeatureNRows =
          Math2.roundToInt(featureNRows.calculateStats()[PrimitiveArray.STATS_MAX]);
      int nFeatures = featureFirstRow.size();
      if (reallyVerbose)
        String2.log(
            "  nFeatures="
                + nFeatures
                + "\n  featureFirstRow="
                + featureFirstRow
                + "\n  featureNRows="
                + featureNRows
                + "\n  maxFeatureNRows="
                + maxFeatureNRows);
      // conserve memory
      idPA = null;

      // do a fileSize check here.  <2GB?

      // ** Create the .ncCF file
      NetcdfFormatWriter.Builder ncCF = NetcdfFormatWriter.createNewNetcdf3(ncCFName + randomInt);
      Group.Builder rootGroup = ncCF.getRootGroup();
      ncCF.setFill(false);
      // define the dimensions
      Dimension featureDimension = NcHelper.addDimension(rootGroup, lcCdmType, nFeatures);
      Dimension obsDimension =
          NcHelper.addDimension(rootGroup, "obs", nodcMode ? maxFeatureNRows : totalNObs);

      // add the feature variables, then the obs variables
      Variable.Builder newVars[] = new Variable.Builder[ncNCols];
      Variable.Builder rowSizeVar = null;
      for (int so = 0; so < 2; so++) { // 0=feature 1=obs
        for (int col = 0; col < ncNCols; col++) {
          if ((isFeatureVar[col] && so == 0)
              || // is var in currently desired group?
              (!isFeatureVar[col] && so == 1)) {
            // keep going
          } else {
            continue;
          }
          String tColName = ncColNames[col];
          PAType type = colEdv[col].destinationDataPAType();
          int maxStringLength = 1; // nclib wants at least 1
          if (type == PAType.STRING)
            maxStringLength = Math.max(1, twawm.columnMaxStringLength(col));
          if (type == PAType.LONG) {
            // store longs as doubles since nc3 doesn't support longs
            type = PAType.DOUBLE;
            // maxStringLength = NcHelper.LONG_MAXSTRINGLENGTH;  //was Strings
          }
          ArrayList<Dimension> tDimsList = new ArrayList<>();
          if (nodcMode) {
            if (isFeatureVar[col]) {
              tDimsList.add(featureDimension);
            } else {
              // obsVar[feature][obs]
              tDimsList.add(featureDimension);
              tDimsList.add(obsDimension);
            }
          } else {
            tDimsList.add(isFeatureVar[col] ? featureDimension : obsDimension);
          }
          if (type == PAType.STRING) {
            newVars[col] =
                NcHelper.addNc3StringVariable(rootGroup, tColName, tDimsList, maxStringLength);
          } else {
            newVars[col] =
                NcHelper.addVariable(rootGroup, tColName, NcHelper.getNc3DataType(type), tDimsList);
          }

          // nodcMode: ensure all numeric obs variables have missing_value or _FillValue
          // (String vars would have "", but that isn't a valid attribute)
          if (nodcMode && so == 1 && type != PAType.STRING) {
            Attributes atts = twawm.columnAttributes(col);
            if (atts.getString("missing_value") == null
                && // getString so distinguish null and NaN
                atts.getString("_FillValue") == null) {
              // "" will become native numeric MV, e.g., 127 for ByteArray
              atts.set("_FillValue", PrimitiveArray.factory(type, 1, ""));
            }
          }
        }

        // at end of feature vars, add the rowSize variable
        if (!nodcMode && so == 0) {
          rowSizeVar =
              NcHelper.addVariable(
                  rootGroup,
                  rowSizeName,
                  NcHelper.getNc3DataType(PAType.INT),
                  List.of(featureDimension));
          // String2.log("  rowSize variable added");
        }
      }

      // set the global attributes
      // currently, only the .nc saveAs file types use attributes!
      Attributes cdmGlobalAttributes = twawm.globalAttributes();
      cdmGlobalAttributes.remove("observationDimension"); // obsDim is for a flat file and OBSOLETE
      cdmGlobalAttributes.set("featureType", cdmType);
      if (cdmGlobalAttributes.get("id") == null)
        cdmGlobalAttributes.set(
            "id",
            datasetID); // 2019-05-07 was File2.getNameNoExtension(ncCFName)); //id attribute = file
      // name
      cdmGlobalAttributes.set(
          "Conventions", ensureAtLeastCF110(cdmGlobalAttributes.getString("Conventions")));
      NcHelper.setAttributes(nc3Mode, rootGroup, cdmGlobalAttributes);

      // set the variable attributes
      // section 9.1.1 says "The coordinates attribute must identify the variables
      //  needed to geospatially position and time locate the observation.
      //  The lat, lon and time coordinates must always exist; a vertical
      //  coordinate may exist."
      // so always include time, lat, lon, even if they use obs dimension.
      boolean isCoordinateVar[] = new boolean[ncNCols]; // all false
      isCoordinateVar[String2.indexOf(ncColNames, "longitude")] = true;
      isCoordinateVar[String2.indexOf(ncColNames, "latitude")] = true;
      isCoordinateVar[String2.indexOf(ncColNames, "time")] = true;
      String coordinates = "time latitude longitude"; // spec doesn't specify a specific order
      String proxy = combinedGlobalAttributes.getString(language, "cdm_altitude_proxy");
      if (altIndex >= 0) {
        coordinates += " altitude";
        isCoordinateVar[String2.indexOf(ncColNames, "altitude")] = true;
      } else if (depthIndex >= 0) {
        coordinates += " depth";
        isCoordinateVar[String2.indexOf(ncColNames, "depth")] = true;
      } else if (proxy != null) {
        // it is in requiredCfRequestVariables
        coordinates += " " + proxy;
        isCoordinateVar[String2.indexOf(ncColNames, proxy)] = true;
      }
      for (int col = 0; col < ncNCols; col++) {
        Attributes tAtts = new Attributes(twawm.columnAttributes(col)); // use a copy
        if (!isFeatureVar[col] && !isCoordinateVar[col]) tAtts.add("coordinates", coordinates);

        PAType paType = twawm.columnType(col);
        if (paType == PAType.STRING) tAtts.add(File2.ENCODING, File2.ISO_8859_1);
        // disabled until there is a standard
        //                else if (paType == PAType.CHAR)
        //                    tAtts.add(String2.CHARSET, File2.ISO_8859_1);

        NcHelper.setAttributes(nc3Mode, newVars[col], tAtts, paType.isUnsigned());
      }

      // set the rowSize attributes
      if (!nodcMode) {
        NcHelper.setAttributes(
            nc3Mode,
            rowSizeVar,
            new Attributes()
                .add("ioos_category", "Identifier")
                .add("long_name", "Number of Observations for this " + cdmType)
                .add("sample_dimension", "obs"),
            false); // isUnsigned
      }

      // ** leave "define" mode
      // If offset to last var is >2GB, netcdf-java library will throw an exception here.
      ncWriter = ncCF.build();

      // write the variables
      // One var at a time makes it reasonably memory efficient.
      // This needs to reorder the values, which needs all values at once, so can't do in chunks.
      for (int col = 0; col < ncNCols; col++) {
        EDV tEdv = colEdv[col];
        double tSafeMV = tEdv.safeDestinationMissingValue();
        Variable newVar =
            ncWriter.findVariable(
                newVars[col].getFullName()); // because newVars has Variable.Builders
        PrimitiveArray pa = twawm.column(col);
        // first re-order
        pa.reorder(rank);

        // convert LongArray to DoubleArray (since nc3 doesn't support longs)
        if (pa instanceof LongArray || pa instanceof ULongArray)
          pa = new DoubleArray(pa); //    pa = new StringArray(pa);

        if (isFeatureVar[col]) {
          // write featureVar[feature]
          pa.justKeep(featureKeep);
          NcHelper.write(nc3Mode, ncWriter, newVar, 0, pa);

        } else if (nodcMode) {
          // write nodc obsVar[feature][obs]
          int origin[] = {0, 0};
          PrimitiveArray subsetPa = null;
          for (int feature = 0; feature < nFeatures; feature++) {
            // write the data for this feature
            origin[0] = feature;
            origin[1] = 0;
            int firstRow = featureFirstRow.get(feature);
            int tNRows = featureNRows.get(feature);
            int stopRow = firstRow + tNRows - 1;
            subsetPa = pa.subset(subsetPa, firstRow, 1, stopRow);
            NcHelper.write(nc3Mode, ncWriter, newVar, origin, new int[] {1, tNRows}, subsetPa);

            // and write missing values
            if (tNRows < maxFeatureNRows) {
              origin[1] = tNRows;
              // String2.log("  writeMVs: feature=" + feature + " origin[1]=" + origin[1] +
              //  " tNRows=" + tNRows + " maxFeatureNRows=" + maxFeatureNRows);
              tNRows = maxFeatureNRows - tNRows;
              subsetPa.clear();
              // if (tEdv.destinationDataPAType() == PAType.LONG)
              //    subsetPa.addNStrings(tNRows, "" + tSafeMV);
              // else
              if (subsetPa instanceof StringArray) subsetPa.addNStrings(tNRows, "");
              else subsetPa.addNDoubles(tNRows, tSafeMV);
              NcHelper.write(nc3Mode, ncWriter, newVar, origin, new int[] {1, tNRows}, subsetPa);
            }
          }

        } else {
          // write obsVar[obs]
          NcHelper.write(nc3Mode, ncWriter, newVar, 0, pa);
        }
      }

      // write the rowSize values
      if (!nodcMode)
        NcHelper.write(
            nc3Mode, ncWriter, ncWriter.findVariable(rowSizeVar.getFullName()), 0, featureNRows);

      // if close throws Throwable, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name
      File2.rename(ncCFName + randomInt, ncCFName);

      // diagnostic
      if (reallyVerbose)
        String2.log(
            "  EDDTable.convertFlatNcToNcCF1 done. TIME="
                + (System.currentTimeMillis() - time)
                + "ms\n");
      // String2.log("  .ncCF File created:\n" + NcHelper.ncdump(ncCFName, "-h"));

    } catch (Throwable t) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(ncCFName + randomInt);
        ncWriter = null;
      }

      throw t;
    }
  } // end of saveAsNcCF1

  /**
   * Create a two-level TimeSeriesProfile or TrajectoryProfile .ncCF or .ncCFMA file (Discrete
   * Sampling Geometries).
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries
   * <br>
   * This assumes cdm_data_type=TimeSeriesProfile|TrajectoryProfile and cdm_timeseries_variables,
   * cdm_trajectory_variables, and/or cdm_profile_variables are defined correctly.
   *
   * @param language the index of the selected language
   * @param nodcMode If true, data will be saved in .ncCFMA file conforming to NODC Templates
   *     https://www.ncei.noaa.gov/netcdf-templates with Multidimensional Array Representation. If
   *     false, data will be saved in .ncCF file with Contiguous Ragged Array Representation.
   * @param ncCFName the complete name of the file that will be created
   * @param twawm
   * @throws Throwable if trouble
   */
  public void saveAsNcCF2(
      int language, boolean nodcMode, String ncCFName, TableWriterAllWithMetadata twawm)
      throws Throwable {

    if (reallyVerbose) String2.log("EDDTable.saveAsNcCF2 " + ncCFName);
    long time = System.currentTimeMillis();
    String rowSizeName = "rowSize";
    boolean nc3Mode = true;

    // delete any existing file
    File2.delete(ncCFName);

    // double check that this dataset supports .ncCF (but it should have been tested earlier)
    if (accessibleViaNcCF.length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaNcCF);
    String cdmType = combinedGlobalAttributes.getString(language, "cdm_data_type");

    // ensure profile_id and timeseries_id|trajectory_id variable is defined
    // and that cdmType is valid
    int oidDVI = -1; // 'o'ther = timeseries|trajectory
    int pidDVI = profile_idIndex; // 'p'rofile
    String olcCdmName;
    if (CDM_TIMESERIESPROFILE.equals(cdmType)) {
      oidDVI = timeseries_idIndex;
      olcCdmName = "timeseries";
    } else if (CDM_TRAJECTORYPROFILE.equals(cdmType)) {
      oidDVI = trajectory_idIndex;
      olcCdmName = "trajectory";
    } else
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + // but already checked before calling this method
              "For convertFlatNcToNcCF2(), cdm_data_type must be TimeSeriesProfile or TrajectoryProfile, not "
              + cdmType
              + ".");
    if (oidDVI < 0) // but already checked by accessibleViaNcCF
    throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "No variable has a cf_role="
              + olcCdmName
              + "_id attribute.");
    if (pidDVI < 0) // but already checked by accessibleViaNcCF
    throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "No variable has a cf_role=profile_id attribute.");
    String oidName = dataVariableDestinationNames()[oidDVI]; // var name of ..._id var
    String pidName = dataVariableDestinationNames()[pidDVI];
    String indexName = olcCdmName + "Index";

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    NetcdfFormatWriter ncWriter = null;
    try {
      // get info from twawm
      // Note: this uses 'feature' as stand-in for timeseries|trajectory
      String ncColNames[] = twawm.columnNames();
      // String2.log("  ncColNames=" + String2.toCSSVString(ncColNames));
      int ncNCols = twawm.nColumns();
      boolean isFeatureVar[] = new boolean[ncNCols];
      boolean isProfileVar[] = new boolean[ncNCols];
      String featureVars[] =
          combinedGlobalAttributes.getStringsFromCSV(language, "cdm_" + olcCdmName + "_variables");
      String profileVars[] =
          combinedGlobalAttributes.getStringsFromCSV(language, "cdm_profile_variables");
      EDV colEdv[] = new EDV[ncNCols];
      for (int col = 0; col < ncNCols; col++) {
        isFeatureVar[col] = String2.indexOf(featureVars, ncColNames[col]) >= 0;
        isProfileVar[col] = String2.indexOf(profileVars, ncColNames[col]) >= 0;
        colEdv[col] = findVariableByDestinationName(ncColNames[col]);
      }

      // String2.log("  isFeatureVar=" + String2.toCSSVString(isFeatureVar));
      // String2.log("  isProfileVar=" + String2.toCSSVString(isProfileVar));

      // ensure profile_id and timeseries_id|trajectory_id variable is in flatNc file
      int oidPo = String2.indexOf(ncColNames, oidName);
      int pidPo = String2.indexOf(ncColNames, pidName);
      if (oidPo < 0) // but already checked before calling this method
      throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "The .nc file must have "
                + oidName);
      if (pidPo < 0) // but already checked before calling this method
      throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "The .nc file must have "
                + pidName);

      // read oid ('feature'_id) and pid (profile_id) PA from flatNc file
      PrimitiveArray oidPA = twawm.column(oidPo);
      PrimitiveArray pidPA = twawm.column(pidPo);
      // String2.log("  oidPA=" + oidPA.toString());
      // String2.log("  pidPA=" + pidPA.toString());

      // rank oid+pid rows
      // !!! this assumes already sorted by time/depth/... and will stay sorted when re-sorted.
      //  PrimitiveArray.rank promises stable sort.
      ArrayList<PrimitiveArray> tTable = new ArrayList<>();
      tTable.add(oidPA);
      tTable.add(pidPA);
      int rank[] = PrimitiveArray.rank(tTable, new int[] {0, 1}, new boolean[] {true, true});
      tTable = null;
      int tTableNRows = oidPA.size();
      // String2.log("  rank=" + String2.toCSSVString(rank));

      // find unique features (timeseries|trajectory):
      // featureFirstRow (first row in CF file for each feature)
      IntArray featureIndex = new IntArray(); // 'feature' number for each profile
      featureIndex.add(0);
      IntArray profileIndexInThisFeature = new IntArray(); // 0,1,2, 0,1,2,3, ...
      int tProfileIndexInThisFeature = 0; // first is #0
      profileIndexInThisFeature.add(tProfileIndexInThisFeature);
      BitSet featureKeep = new BitSet(); // initially all false
      featureKeep.set(0); // 'row' after data is sorted
      int nFeatures = 1;
      IntArray nProfilesPerFeature = new IntArray();
      nProfilesPerFeature.add(1); // for the initial row

      // find unique profiles:
      // profileFirstRow (first row in CF file for each profile)
      IntArray profileFirstObsRow = new IntArray(); // 'row' after data is sorted
      profileFirstObsRow.add(0); // first row has first profile
      int prevFirstRow = 0; // 'row' after data is sorted
      IntArray nObsPerProfile = new IntArray();
      BitSet profileKeep = new BitSet(); // initially all false
      profileKeep.set(0); // 'row' after data is sorted

      // 'row' after data is sorted
      for (int row = 1; row < tTableNRows; row++) { // 1 since comparing to previous row
        // keeping the tests separate and using featureChanged allows
        //   profile_id to be only unique within an 'feature'
        //   (it doesn't have to be globally unique)
        boolean featureChanged = false;
        if (oidPA.compare(rank[row - 1], rank[row]) != 0) {
          // found a new feature
          featureChanged = true;
          featureKeep.set(row);
          nFeatures++;
          nProfilesPerFeature.add(0);
          tProfileIndexInThisFeature = -1;
        }
        if (featureChanged || pidPA.compare(rank[row - 1], rank[row]) != 0) {
          // found a new profile
          featureIndex.add(nFeatures - 1); // yes, one for each profile
          tProfileIndexInThisFeature++;
          profileIndexInThisFeature.add(tProfileIndexInThisFeature);
          nProfilesPerFeature.set(nFeatures - 1, tProfileIndexInThisFeature + 1);
          profileFirstObsRow.add(row); // for this profile
          nObsPerProfile.add(row - prevFirstRow); // for the previous profile
          profileKeep.set(row);
          prevFirstRow = row;
        }
      }
      nObsPerProfile.add(tTableNRows - prevFirstRow);
      int nProfiles = profileFirstObsRow.size();
      int maxObsPerProfile =
          Math2.roundToInt(nObsPerProfile.calculateStats()[PrimitiveArray.STATS_MAX]);
      int maxProfilesPerFeature =
          Math2.roundToInt(nProfilesPerFeature.calculateStats()[PrimitiveArray.STATS_MAX]);
      if (reallyVerbose)
        String2.log(
            "  nFeatures="
                + nFeatures
                +
                // "\n  nProfilesPerFeature=" + nProfilesPerFeature.toString() +  //these can be
                // really long
                "\n  nProfiles="
                + nProfiles);
      // "\n  featureIndex="        + featureIndex.toString() +
      // "\n  profileFirstObsRow="  + profileFirstObsRow.toString() +
      // "\n  nObsPerProfile="      + nObsPerProfile.toString());
      // conserve memory
      oidPA = null;
      pidPA = null;

      // ** Create the .ncCF file
      NetcdfFormatWriter.Builder ncCF = NetcdfFormatWriter.createNewNetcdf3(ncCFName + randomInt);
      Group.Builder rootGroup = ncCF.getRootGroup();
      ncCF.setFill(false);
      // define the dimensions
      Dimension featureDimension = NcHelper.addDimension(rootGroup, olcCdmName, nFeatures);
      Dimension profileDimension =
          NcHelper.addDimension(rootGroup, "profile", nodcMode ? maxProfilesPerFeature : nProfiles);
      Dimension obsDimension =
          NcHelper.addDimension(rootGroup, "obs", nodcMode ? maxObsPerProfile : tTableNRows);

      // add the feature variables, then profile variables, then obs variables
      Variable.Builder<?> newVars[] = new Variable.Builder[ncNCols];
      Variable.Builder<?> indexVar = null;
      Variable.Builder<?> rowSizeVar = null;
      for (int opo = 0; opo < 3; opo++) { // 0=feature 1=profile 2=obs
        for (int col = 0; col < ncNCols; col++) {
          if ((isFeatureVar[col] && opo == 0)
              || // is var in currently desired group?
              (isProfileVar[col] && opo == 1)
              || (!isFeatureVar[col] && !isProfileVar[col] && opo == 2)) {
            // keep going
          } else {
            continue;
          }
          String tColName = ncColNames[col];
          // String2.log(" opo=" + opo + " col=" + col + " name=" + tColName);
          ArrayList<Dimension> tDimsList = new ArrayList<>();
          if (nodcMode) {
            if (isFeatureVar[col]) {
              // featureVar[feature]
              tDimsList.add(featureDimension);
            } else if (isProfileVar[col]) {
              // profileVar[feature][profile]
              tDimsList.add(featureDimension);
              tDimsList.add(profileDimension);
            } else {
              // obsVar[feature][profile][obs]
              tDimsList.add(featureDimension);
              tDimsList.add(profileDimension);
              tDimsList.add(obsDimension);
            }
          } else {
            if (isFeatureVar[col]) {
              // featureVar[feature]
              tDimsList.add(featureDimension);
            } else if (isProfileVar[col]) {
              // profileVar[profile]
              tDimsList.add(profileDimension);
            } else {
              // obsVar[obs]
              tDimsList.add(obsDimension);
            }
          }
          PAType type = colEdv[col].destinationDataPAType();
          int maxStringLength = 1;
          if (type == PAType.STRING) {
            maxStringLength =
                Math.max(1, twawm.columnMaxStringLength(col)); // nclib wants at least 1
          } else if (type == PAType.LONG) {
            // store longs as doubles since nc3 doesn't support longs
            type = PAType.DOUBLE;
            // maxStringLength = NcHelper.LONG_MAXSTRINGLENGTH;  was strings
          }

          if (type == PAType.STRING) {
            newVars[col] =
                NcHelper.addNc3StringVariable(rootGroup, tColName, tDimsList, maxStringLength);
          } else {
            newVars[col] =
                NcHelper.addVariable(rootGroup, tColName, NcHelper.getNc3DataType(type), tDimsList);
          }

          // nodcMode: ensure all numeric obs variables have missing_value or _FillValue
          // (String vars would have "", but that isn't a valid attribute)
          if (nodcMode && opo == 2 && type != PAType.STRING) {
            Attributes atts = twawm.columnAttributes(col);
            if (atts.getString("missing_value") == null
                && // getString so distinguish null and NaN
                atts.getString("_FillValue") == null) {
              // "" will become native numeric MV, e.g., 127 for ByteArray
              atts.set("_FillValue", PrimitiveArray.factory(type, 1, ""));
            }
          }
        }

        // at end of profile vars, add 'feature'Index and rowSize variables
        if (!nodcMode && opo == 1) {
          indexVar =
              NcHelper.addVariable(
                  rootGroup,
                  indexName,
                  NcHelper.getNc3DataType(PAType.INT),
                  List.of(profileDimension)); // yes, profile, since there is one for each profile
          rowSizeVar =
              NcHelper.addVariable(
                  rootGroup,
                  rowSizeName,
                  NcHelper.getNc3DataType(PAType.INT),
                  List.of(profileDimension));
          // String2.log("  featureIndex and rowSize variable added");
        }
      }

      // set the global attributes
      // currently, only the .nc saveAs types use attributes!
      Attributes cdmGlobalAttributes = twawm.globalAttributes();
      cdmGlobalAttributes.remove("observationDimension"); // obsDim is for a flat file and OBSOLETE
      cdmGlobalAttributes.set("featureType", cdmType);
      if (cdmGlobalAttributes.get("id") == null)
        cdmGlobalAttributes.set(
            "id",
            datasetID); // 2019-05-07 was File2.getNameNoExtension(ncCFName)); //id attribute = file
      // name
      cdmGlobalAttributes.set(
          "Conventions", ensureAtLeastCF110(cdmGlobalAttributes.getString("Conventions")));
      NcHelper.setAttributes(nc3Mode, rootGroup, cdmGlobalAttributes);

      // set the variable attributes
      // section 9.1.1 says "The coordinates attribute must identify the variables
      //  needed to geospatially position and time locate the observation.
      //  The lat, lon and time coordinates must always exist; a vertical
      //  coordinate may exist."
      // so always include time, lat, lon, even if they use obs dimension.
      boolean isCoordinateVar[] = new boolean[ncNCols]; // all false
      isCoordinateVar[String2.indexOf(ncColNames, "longitude")] = true;
      isCoordinateVar[String2.indexOf(ncColNames, "latitude")] = true;
      isCoordinateVar[String2.indexOf(ncColNames, "time")] = true;
      String coordinates = "time latitude longitude"; // spec doesn't specify a specific order
      String proxy = combinedGlobalAttributes.getString(language, "cdm_altitude_proxy");
      if (altIndex >= 0) {
        coordinates += " altitude";
        isCoordinateVar[String2.indexOf(ncColNames, "altitude")] = true;
      } else if (depthIndex >= 0) {
        coordinates += " depth";
        isCoordinateVar[String2.indexOf(ncColNames, "depth")] = true;
      } else if (proxy != null) {
        // it is in requiredCfRequestVariables
        coordinates += " " + proxy;
        isCoordinateVar[String2.indexOf(ncColNames, proxy)] = true;
      }
      for (int col = 0; col < ncNCols; col++) {
        Attributes tAtts = new Attributes(twawm.columnAttributes(col)); // use a copy
        if (!isFeatureVar[col] && !isProfileVar[col] && !isCoordinateVar[col])
          tAtts.add("coordinates", coordinates);

        PAType paType = twawm.columnType(col);
        if (paType == PAType.STRING) tAtts.add(File2.ENCODING, File2.ISO_8859_1);
        // disabled until there is a standard
        //                else if (paType == PAType.CHAR)
        //                    tAtts.add(String2.CHARSET, File2.ISO_8859_1);

        NcHelper.setAttributes(nc3Mode, newVars[col], tAtts, paType.isUnsigned());
      }

      if (!nodcMode) {
        // set the index attributes
        NcHelper.setAttributes(
            nc3Mode,
            indexVar,
            new Attributes()
                .add("ioos_category", "Identifier")
                .add("long_name", "The " + olcCdmName + " to which this profile is associated.")
                .add("instance_dimension", olcCdmName),
            false); // isUnsigned

        // set the rowSize attributes
        NcHelper.setAttributes(
            nc3Mode,
            rowSizeVar,
            new Attributes()
                .add("ioos_category", "Identifier")
                .add("long_name", "Number of Observations for this Profile")
                .add("sample_dimension", "obs"),
            false); // isUnsigned
      }

      // ** leave "define" mode
      // If offset to last var is >2GB, netcdf-java library will throw an exception here.
      ncWriter = ncCF.build();

      // write the variables
      // One var at a time makes it reasonably memory efficient.
      // This needs to reorder the values, which needs all values at once, so can't do in chunks.
      for (int col = 0; col < ncNCols; col++) {
        Variable newVar =
            ncWriter.findVariable(
                newVars[col].getFullName()); // because newVars has Variable.Builders
        PrimitiveArray pa = twawm.column(col);
        EDV tEdv = colEdv[col];
        double tSafeMV = tEdv.safeDestinationMissingValue();

        // first re-order
        pa.reorder(rank);
        if (pa instanceof LongArray || pa instanceof ULongArray)
          pa = new DoubleArray(pa); // was StringArray(pa);

        if (isFeatureVar[col]) {
          // write featureVar[feature]
          pa.justKeep(featureKeep);
          NcHelper.write(nc3Mode, ncWriter, newVar, 0, pa);

        } else if (isProfileVar[col]) {
          pa.justKeep(profileKeep);
          if (nodcMode) {
            // write nodc profileVar[feature][profile]
            int origin[] = new int[2];
            int firstRow;
            int lastRow = -1;
            PrimitiveArray tpa = null;
            for (int feature = 0; feature < nFeatures; feature++) {
              // write data
              origin[0] = feature;
              origin[1] = 0;
              int tnProfiles = nProfilesPerFeature.get(feature);
              firstRow = lastRow + 1;
              lastRow = firstRow + tnProfiles - 1;
              tpa = pa.subset(tpa, firstRow, 1, lastRow);
              NcHelper.write(nc3Mode, ncWriter, newVar, origin, new int[] {1, tnProfiles}, tpa);

              // write fill values
              int nmv = maxProfilesPerFeature - tnProfiles;
              if (nmv > 0) {
                origin[1] = tnProfiles;
                tpa.clear();
                // if (tEdv.destinationDataPAType() == PAType.LONG)
                //    tpa.addNStrings(nmv, "" + tSafeMV);
                // else
                if (tpa instanceof StringArray) tpa.addNStrings(nmv, "");
                else tpa.addNDoubles(nmv, tSafeMV);
                NcHelper.write(nc3Mode, ncWriter, newVar, origin, new int[] {1, nmv}, tpa);
              }
            }
          } else {
            // write ragged array var[profile]
            NcHelper.write(nc3Mode, ncWriter, newVar, new int[] {0}, new int[] {pa.size()}, pa);
          }

        } else if (nodcMode) {
          // write nodc obsVar[feature][profile][obs] as MultidimensionalArray
          int origin[] = {0, 0, 0};

          // fill with mv
          PrimitiveArray subsetPa =
              PrimitiveArray.factory(
                  pa.elementType(),
                  maxObsPerProfile,
                  pa.elementType() == PAType.STRING ? "" : "" + tSafeMV);
          int shape[] = {1, 1, maxObsPerProfile};
          for (int feature = 0; feature < nFeatures; feature++) {
            origin[0] = feature;
            for (int profile = 0; profile < maxProfilesPerFeature; profile++) {
              origin[1] = profile;
              NcHelper.write(nc3Mode, ncWriter, newVar, origin, shape, subsetPa);
            }
          }

          // write obsVar[feature][profile][obs] as MultidimensionalArray
          for (int profile = 0; profile < nProfiles; profile++) {
            origin[0] = featureIndex.get(profile); // feature#
            origin[1] = profileIndexInThisFeature.get(profile);
            origin[2] = 0;
            int firstRow = profileFirstObsRow.get(profile);
            int tNRows = nObsPerProfile.get(profile);
            int stopRow = firstRow + tNRows - 1;
            subsetPa = pa.subset(subsetPa, firstRow, 1, stopRow);
            shape[2] = tNRows;
            NcHelper.write(nc3Mode, ncWriter, newVar, origin, shape, subsetPa);
          }

        } else {
          // write obsVar[obs] as Contiguous Ragged Array
          NcHelper.write(nc3Mode, ncWriter, newVar, 0, pa);
        }
      }

      if (!nodcMode) {
        // write the featureIndex and rowSize values
        NcHelper.write(
            nc3Mode, ncWriter, ncWriter.findVariable(indexVar.getFullName()), 0, featureIndex);
        NcHelper.write(
            nc3Mode, ncWriter, ncWriter.findVariable(rowSizeVar.getFullName()), 0, nObsPerProfile);
      }

      // if close throws Throwable, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name
      File2.rename(ncCFName + randomInt, ncCFName);

      // diagnostic
      if (reallyVerbose)
        String2.log(
            "  EDDTable.convertFlatNcToNcCF2 done. TIME="
                + (System.currentTimeMillis() - time)
                + "ms\n");
      // String2.log("  .ncCF File:\n" + NcHelper.ncdump(ncCFName, "-h"));

    } catch (Throwable t) {
      String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(ncCFName + randomInt);
        ncWriter = null;
      }

      throw t;
    }
  } // end of saveAsNcCF2

  /**
   * This is used by administrators to get the empirical min and max for all non-String variables by
   * doing a request for a time range (sometimes one time point). This could be used by constructors
   * (at the end), but the results vary with different isoDateTimes, and would be wasteful to do
   * this every time a dataset is constructed if results don't change.
   *
   * @param language the index of the selected language
   * @param minTime the ISO 8601 min time to check (use null or "" if no min limit)
   * @param maxTime the ISO 8601 max time to check (use null or "" if no max limit)
   * @param makeChanges if true, the discovered values are used to set the variable's min and max
   *     values if they aren't already set
   * @param stringsToo if true, this determines if the String variables have any values
   * @throws Throwable if trouble
   */
  public void getEmpiricalMinMax(
      int language,
      String loggedInAs,
      String minTime,
      String maxTime,
      boolean makeChanges,
      boolean stringsToo)
      throws Throwable {
    if (verbose)
      String2.log(
          "\nEDDTable.getEmpiricalMinMax for "
              + datasetID
              + ", "
              + minTime
              + " to "
              + maxTime
              + " total nVars="
              + dataVariables.length);
    long downloadTime = System.currentTimeMillis();

    StringBuilder query = new StringBuilder();
    int first = 0; // change temporarily for bmde to get e.g., 0 - 100, 100 - 200, ...
    int last = dataVariables.length; // change temporarily for bmde
    for (int dv = first; dv < last; dv++) {
      // get vars
      if (stringsToo || !dataVariables[dv].destinationDataType().equals("String"))
        query.append("," + dataVariables[dv].destinationName());
    }
    if (query.length() == 0) {
      String2.log("All variables are String variables.");
      return;
    }
    query.deleteCharAt(0); // first comma
    if (minTime != null && minTime.length() > 0) query.append("&time>=" + minTime);
    if (maxTime != null && maxTime.length() > 0) query.append("&time<=" + maxTime);

    // query
    TableWriterAllWithMetadata twawm =
        getTwawmForDapQuery(language, loggedInAs, "", query.toString());
    Table table = twawm.cumulativeTable();
    twawm.releaseResources();
    twawm.close();
    String2.log("  downloadTime=" + (System.currentTimeMillis() - downloadTime) + "ms");
    String2.log("  found nRows=" + table.nRows());
    for (int col = 0; col < table.nColumns(); col++) {
      String destName = table.getColumnName(col);
      EDV edv = findDataVariableByDestinationName(destName); // error if not found
      if (edv.destinationDataType().equals("String")) {
        String2.log(
            "  "
                + destName
                + ": is String variable; maxStringLength found = "
                + twawm.columnMaxStringLength(col)
                + "\n");
      } else {
        String tMin = twawm.columnMinValue[col].getString();
        String tMax = twawm.columnMaxValue[col].getString();
        if (verbose) {
          double loHi[] =
              Math2.suggestLowHigh(
                  twawm.columnMinValue[col].getDouble(), twawm.columnMaxValue[col].getDouble());
          String2.log(
              "  "
                  + destName
                  + ":\n"
                  + "                <att name=\"actual_range\" type=\""
                  + edv.destinationDataType().toLowerCase()
                  + "List\">"
                  + tMin
                  + " "
                  + tMax
                  + "</att>\n"
                  + "                <att name=\"colorBarMinimum\" type=\"double\">"
                  + loHi[0]
                  + "</att>\n"
                  + "                <att name=\"colorBarMaximum\" type=\"double\">"
                  + loHi[1]
                  + "</att>\n");
        }
        if (makeChanges
            && edv.destinationMin().isMissingValue()
            && edv.destinationMax().isMissingValue()) {

          edv.setDestinationMinMax(
              PAOne.fromDouble(
                  twawm.columnMinValue[col].getDouble() * edv.scaleFactor() + edv.addOffset()),
              PAOne.fromDouble(
                  twawm.columnMaxValue[col].getDouble() * edv.scaleFactor() + edv.addOffset()));
          edv.setActualRangeFromDestinationMinMax(language);
        }
      }
    }
    if (verbose) String2.log("\ntotal nVars=" + dataVariables.length);
  }

  /**
   * This is used by administrator to get min,max time by doing a request for lon,lat. This could be
   * used by constructors (at the end). The problem is that the results vary with different
   * isoDateTimes, and wasteful to do this every time constructed (if results don't change).
   *
   * @param language the index of the selected language
   * @param lon the lon to check
   * @param lat the lat to check
   * @param dir the directory (on this computer's hard drive) to use for temporary/cache files
   * @param fileName the name for the 'file' (no dir, no extension), which is used to write the
   *     suggested name for the file to the response header.
   * @throws Throwable if trouble
   */
  public void getMinMaxTime(
      int language, String loggedInAs, double lon, double lat, String dir, String fileName)
      throws Throwable {
    if (verbose) String2.log("\nEDDTable.getMinMaxTime for lon=" + lon + " lat=" + lat);

    StringBuilder query = new StringBuilder();
    // get all vars since different vars at different altitudes
    for (EDV dataVariable : dataVariables) query.append("," + dataVariable.destinationName());
    query.deleteCharAt(0); // first comma
    query.append("&" + EDV.LON_NAME + "=" + lon + "&" + EDV.LAT_NAME + "=" + lat);

    // auto get source min/max time   for that one lat,lon location
    TableWriterAllWithMetadata twawm =
        getTwawmForDapQuery(language, loggedInAs, "", query.toString());
    Table table = twawm.makeEmptyTable(); // no need for twawm.cumulativeTable();
    twawm.releaseResources();
    twawm.close();
    int timeCol = table.findColumnNumber(EDV.TIME_NAME);
    PAOne tMin = twawm.columnMinValue(timeCol);
    PAOne tMax = twawm.columnMaxValue(timeCol);
    if (verbose)
      String2.log(
          "  found time min="
              + tMin
              + "="
              + (tMin.isMissingValue() ? "" : Calendar2.epochSecondsToIsoStringTZ(tMin.getDouble()))
              + " max="
              + tMax
              + "="
              + (tMax.isMissingValue()
                  ? ""
                  : Calendar2.epochSecondsToIsoStringTZ(tMax.getDouble())));
    dataVariables[timeIndex].setDestinationMinMax(
        tMin, tMax); // scaleFactor,addOffset not supported
    dataVariables[timeIndex].setActualRangeFromDestinationMinMax(language);
  }

  /**
   * This writes an HTML form requesting info from this dataset (like the OPeNDAP Data Access Forms,
   * DAF).
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
      throws Throwable, Exception {

    HtmlWidgets widgets =
        new HtmlWidgets(true, EDStatic.imageDirUrl(request, loggedInAs, language));

    // parse userDapQuery
    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    if (userDapQuery == null) userDapQuery = "";
    userDapQuery = userDapQuery.trim();
    String queryParts[] =
        Table.getDapQueryParts(userDapQuery); // decoded.  always at least 1 part (may be "")
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    if (userDapQuery == null) userDapQuery = "";
    try {
      parseUserDapQuery(
          language,
          userDapQuery,
          resultsVariables,
          constraintVariables,
          constraintOps,
          constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
          true,
          false); // repair, processAddVariablesWhere
    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}
      String2.log(MustBe.throwableToString(t));
      userDapQuery = ""; // as if no userDapQuery
    }

    // get the distinctOptionsTable
    String distinctOptions[][] = null;
    if (accessibleViaSubset().length() == 0)
      distinctOptions = distinctOptionsTable(language, loggedInAs);

    // beginning of form
    writer.write("&nbsp;\n"); // necessary for the blank line before the form (not <p>)
    String formName = "form1";
    String docFormName = "document.form1";
    writer.write(HtmlWidgets.ifJavaScriptDisabled);
    writer.write(widgets.beginForm(formName, "GET", "", ""));
    writer.write(HtmlWidgets.PERCENT_ENCODE_JS);

    // begin table
    writer.write(widgets.beginTable("class=\"compact nowrap\""));

    // write the table's column names
    String gap = "&nbsp;&nbsp;&nbsp;";
    writer.write(
        "<tr>\n"
            + "  <th class=\"L\">"
            + EDStatic.messages.get(Message.EDD_TABLE_VARIABLE, language)
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.get(Message.EDD_TABLE_TABULAR_DATASET_TOOLTIP, language)
                    + "</div>"));

    StringBuilder checkAll = new StringBuilder();
    StringBuilder uncheckAll = new StringBuilder();
    int nDv = dataVariables.length;
    for (int dv = 0; dv < nDv; dv++) {
      checkAll.append(formName + ".varch" + dv + ".checked=true;");
      uncheckAll.append(formName + ".varch" + dv + ".checked=false;");
    }
    writer.write(
        widgets.button(
            "button",
            "CheckAll",
            EDStatic.messages.get(Message.EDD_TABLE_CHECK_ALL_TOOLTIP, language),
            EDStatic.messages.get(Message.EDD_TABLE_CHECK_ALL, language),
            "onclick=\"" + checkAll + "\""));
    writer.write(
        widgets.button(
            "button",
            "UncheckAll",
            EDStatic.messages.get(Message.EDD_TABLE_UNCHECK_ALL_TOOLTIP, language),
            EDStatic.messages.get(Message.EDD_TABLE_UNCHECK_ALL, language),
            "onclick=\"" + uncheckAll + "\""));

    writer.write(
        "  &nbsp;</th>\n"
            + "  <th colspan=\"2\">"
            + EDStatic.messages.get(Message.EDD_TABLE_OPT_CONSTRAINT1_HTML, language)
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.get(Message.EDD_TABLE_CONSTRAINT_TOOLTIP, language)
                    + "</div>")
            + "</th>\n"
            + "  <th colspan=\"2\">"
            + EDStatic.messages.get(Message.EDD_TABLE_OPT_CONSTRAINT2_HTML, language)
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.get(Message.EDD_TABLE_CONSTRAINT_TOOLTIP, language)
                    + "</div>")
            + "</th>\n"
            + "  <th>"
            + gap
            + EDStatic.messages.get(Message.EDD_MINIMUM, language)
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                EDStatic.messages.get(Message.EDD_TABLE_MINIMUM_TOOLTIP, language))
            + (distinctOptions == null
                ? "<br>&nbsp;"
                : "<br>"
                    + gap
                    + EDStatic.messages.get(Message.OR_A_LIST_OF_VALUES, language)
                    + " "
                    + // 2014-07-17 was Distinct Values
                    EDStatic.htmlTooltipImage(
                        request,
                        language,
                        loggedInAs,
                        "<div class=\"narrow_max_width\">"
                            + EDStatic.messages.get(Message.DISTINCT_VALUES_TOOLTIP, language)
                            + "</div>"))
            + "</th>\n"
            + "  <th>"
            + gap
            + EDStatic.messages.get(Message.EDD_MAXIMUM, language)
            + " "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                EDStatic.messages.get(Message.EDD_TABLE_MAXIMUM_TOOLTIP, language))
            + "<br>&nbsp;"
            + "</th>\n"
            + "</tr>\n");

    // a row for each dataVariable
    String sliderFromNames[] = new String[nDv];
    String sliderToNames[] = new String[nDv];
    int sliderNThumbs[] = new int[nDv];
    String sliderUserValuesCsvs[] = new String[nDv];
    int sliderInitFromPositions[] = new int[nDv];
    int sliderInitToPositions[] = new int[nDv];
    Table tMinMaxTable = minMaxTable;
    for (int dv = 0; dv < nDv; dv++) {
      EDV edv = dataVariables[dv];
      double tMax = edv.destinationMaxDouble();
      boolean isTime = dv == timeIndex;
      boolean isTimeStamp = edv instanceof EDVTimeStamp;
      boolean isChar = edv.destinationDataPAType() == PAType.CHAR;
      boolean isString = edv.destinationDataPAType() == PAType.STRING;
      String tTime_precision = isTimeStamp ? ((EDVTimeStamp) edv).time_precision() : null;

      writer.write("<tr>\n");

      // get the extra info
      String extra = edv.units();
      if (isTimeStamp) extra = "UTC"; // no longer true: "seconds since 1970-01-01..."
      if (extra == null) extra = "";
      if (showLongName(edv.destinationName(), edv.longName(), extra, 30))
        extra = edv.longName() + (extra.length() == 0 ? "" : ", " + extra);
      if (extra.length() > 0) extra = " (" + extra + ")";

      // variables: checkbox varname (longName, units)
      writer.write("  <td>");
      writer.write(
          widgets.checkbox(
              "varch" + dv,
              EDStatic.messages.get(Message.EDD_TABLE_CHECK_THE_VARIABLES, language),
              userDapQuery.length() == 0 || (resultsVariables.indexOf(edv.destinationName()) >= 0),
              edv.destinationName(),
              edv.destinationName()
                  + extra
                  + " "
                  + EDStatic.htmlTooltipImageEDV(request, language, loggedInAs, edv),
              ""));
      writer.write("  &nbsp;</td>\n");

      // get default constraints
      String[] tOp = {">=", "<="};
      double[] tValD = {Double.NaN, Double.NaN};
      String[] tValS = {null, null};
      if (userDapQuery.length() > 0) {
        // get constraints from userDapQuery?
        boolean done0 = false;
        // find first 2 constraints on this var
        for (int con = 0; con < 2; con++) {
          int tConi = constraintVariables.indexOf(edv.destinationName());
          if (tConi >= 0) {
            String cOp = constraintOps.get(tConi);
            int putIn = cOp.startsWith("<") || done0 ? 1 : 0; // prefer 0 first
            if (putIn == 0) done0 = true;
            tOp[putIn] = cOp;
            tValS[putIn] = constraintValues.get(tConi);
            tValD[putIn] = String2.parseDouble(tValS[putIn]);
            if (isTimeStamp) {
              // time constraint will be stored as double (then as a string)
              tValS[putIn] =
                  Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, tValD[putIn], "");
            }
            constraintVariables.remove(tConi);
            constraintOps.remove(tConi);
            constraintValues.remove(tConi);
          }
        }
      } else {
        if (isTime) {
          if (Double.isFinite(tMax)) {
            // only set max request if tMax is known
            tValD[1] = tMax;
            tValS[1] = Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, tMax, "");
          }
          tValD[0] = Calendar2.backNDays(7, tMax); // NaN -> now
          tValS[0] = Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, tValD[0], "");
        }
      }

      // write constraints html
      String valueWidgetName = "val" + dv + "_";
      String tTooltip =
          isTimeStamp
              ? EDStatic.messages.get(Message.EDD_TABLE_TIME_CONSTRAINT_TOOLTIP, language)
              : isString || isChar
                  ? EDStatic.messages.get(Message.EDD_TABLE_STRING_CONSTRAINT_TOOLTIP, language)
                  : EDStatic.messages.get(Message.EDD_TABLE_NUMERIC_CONSTRAINT_TOOLTIP, language);
      if (edv.destinationMinString().length() > 0)
        tTooltip +=
            "<br>&nbsp;<br>"
                + MessageFormat.format(
                    EDStatic.messages.get(Message.RANGES_FROM_TO, language),
                    edv.destinationName(),
                    edv.destinationMinString(),
                    edv.destinationMaxString().length() > 0 ? edv.destinationMaxString() : "(?)");
      for (int con = 0; con < 2; con++) {
        writer.write("  <td>" + (con == 0 ? "" : gap));
        writer.write(
            widgets.select(
                "op" + dv + "_" + con,
                EDStatic.messages.get(Message.EDD_TABLE_SELECT_AN_OPERATOR, language),
                1,
                OPERATORS,
                PPE_OPERATORS,
                OPERATORS.indexOf(tOp[con]),
                "",
                false,
                ""));
        writer.write("  </td>\n");
        writer.write("  <td>");
        String tVal = tValS[con];
        if (tVal == null) {
          tVal = "";
        } else if (tOp[con].equals(PrimitiveArray.REGEX_OP) || isChar || isString) {
          tVal = String2.toJson(tVal); // enclose in "
        }
        writer.write(widgets.textField(valueWidgetName + con, tTooltip, 19, 255, tVal, ""));
        writer.write("  </td>\n");
      }

      // distinct options   or min and max
      int whichDTCol =
          distinctOptions == null ? -1 : String2.indexOf(subsetVariables, edv.destinationName());
      if (whichDTCol >= 0) {
        // show the distinct options
        writer.write( // left justified (the default) is best if the distinct values are really long
            "  <td colspan=\"2\">"
                + widgets.beginTable("class=\"compact nowrap\"")
                + // table for distinctOptions
                "  <tr>\n"
                + "  <td>&nbsp;&nbsp;");
        String tVal0 = tValS[0];
        if (tVal0 == null) {
          tVal0 = "";
        } else if (isChar || isString) {
          tVal0 = String2.replaceAll(String2.toJson(tVal0), '\u00A0', ' '); // enclose in "
        }
        int tWhich = String2.indexOf(distinctOptions[whichDTCol], tVal0);
        // String2.log("*** tVal0=" + String2.annotatedString(tVal0) +
        //         " a disOption=" + String2.annotatedString(distinctOptions[whichDTCol][2]) +
        //         " match=" + tWhich);
        writer.write(
            widgets.select(
                    "dis" + dv,
                    "",
                    1,
                    distinctOptions[whichDTCol],
                    Math.max(0, tWhich),
                    "onChange='"
                        +
                        // "if (this.selectedIndex>0) {" +
                        " "
                        + docFormName
                        + ".op"
                        + dv
                        + "_0.selectedIndex="
                        + OPERATORS.indexOf("=")
                        + ";"
                        + " "
                        + docFormName
                        + ".val"
                        + dv
                        + "_0.value=this.value;"
                        + "'",
                    true)
                + // encodeSpaces solves the problem with consecutive internal spaces
                "\n"
                + "  </td>\n");

        // +- buttons
        writer.write(
            "  <td class=\"B\">"
                + // vertical-align: 'b'ottom
                "<img class=\"B\" src=\""
                + widgets.imageDirUrl
                + "minus.gif\""
                + // vertical-align: 'b'ottom
                "  "
                + widgets.completeTooltip(EDStatic.messages.get(Message.SELECT_PREVIOUS, language))
                + " alt=\"-\"\n"
                +
                // onMouseUp works much better than onClick and onDblClick
                "  onMouseUp=\"var dis="
                + docFormName
                + ".dis"
                + dv
                + ";\n"
                + "    if (dis.selectedIndex>0) {"
                + "      dis.selectedIndex--;\n"
                + "      "
                + docFormName
                + ".op"
                + dv
                + "_0.selectedIndex="
                + OPERATORS.indexOf("=")
                + ";\n"
                + "      "
                + docFormName
                + ".val"
                + dv
                + "_0.value="
                + "dis.options[dis.selectedIndex].text;\n"
                + "    }\" >\n"
                + "  </td>\n"
                + "  <td class=\"B\">"
                + // vertical-align: 'b'ottom
                "<img class=\"B\" src=\""
                + widgets.imageDirUrl
                + "plus.gif\""
                + // vertical-align: 'b'ottom
                "  "
                + widgets.completeTooltip(EDStatic.messages.get(Message.SELECT_NEXT, language))
                + " alt=\"+\"\n"
                +
                // onMouseUp works much better than onClick and onDblClick
                "  onMouseUp=\"var dis="
                + docFormName
                + ".dis"
                + dv
                + ";\n"
                + "    if (dis.selectedIndex<"
                + (distinctOptions[whichDTCol].length - 1)
                + ") {"
                + "      dis.selectedIndex++;\n"
                + "      "
                + docFormName
                + ".op"
                + dv
                + "_0.selectedIndex="
                + OPERATORS.indexOf("=")
                + ";\n"
                + "      "
                + docFormName
                + ".val"
                + dv
                + "_0.value="
                + "dis.options[dis.selectedIndex].text;\n"
                + "    }\" >\n"
                + "  </td>\n"
                + "  <td>&nbsp;"
                + EDStatic.htmlTooltipImage(
                    request,
                    language,
                    loggedInAs,
                    "<div class=\"narrow_max_width\">"
                        + EDStatic.messages.get(
                            Message.EDD_TABLE_SELECT_CONSTRAINT_TOOLTIP, language)
                        + "</div>")
                + "  </tr>\n"
                + "  </table>\n"
                + "</td>\n");

      } else {
        // show min and max
        String mins = edv.destinationMinString();
        String maxs = edv.destinationMaxString();
        if (tMinMaxTable != null
            && tMinMaxTable.nRows() >= 2
            && edv.destinationDataPAType() == PAType.STRING) {
          // Time variables are numeric,
          // and Strings vars never have scale_factor or add_offset,
          // so String var sourceValue=destinationValue.
          mins = tMinMaxTable.getStringData(dv, 0);
          maxs = tMinMaxTable.getStringData(dv, 1);
          if (mins == null) mins = "";
          if (maxs == null) maxs = "";
          if (mins.length() > 20) mins = mins.substring(0, 18) + "...";
          if (maxs.length() > 20) maxs = maxs.substring(0, 18) + "...";
          // web page is UTF-8, so it/browser will deal with all characters
          mins = String2.toJson(mins, 65536, true); // encodeNewline
          maxs = String2.toJson(maxs, 65536, true); // encodeNewline
        }
        writer.write("  <td>" + gap + mins + "</td>\n" + "  <td>" + gap + maxs + "</td>\n");
      }

      // end of row
      writer.write("</tr>\n");

      // *** and a slider for this dataVariable    (Data Access Form)
      if ((dv != lonIndex && dv != latIndex && dv != altIndex && dv != depthIndex && !isTime)
          || !Double.isFinite(edv.destinationMinDouble())
          || (!Double.isFinite(edv.destinationMaxDouble()) && !isTime)) {

        // no slider
        sliderNThumbs[dv] = 0;
        sliderFromNames[dv] = "";
        sliderToNames[dv] = "";
        sliderUserValuesCsvs[dv] = "";
        sliderInitFromPositions[dv] = 0;
        sliderInitToPositions[dv] = 0;

      } else {
        // slider
        sliderNThumbs[dv] = 2;
        sliderFromNames[dv] = formName + "." + valueWidgetName + "0";
        sliderToNames[dv] = formName + "." + valueWidgetName + "1";

        sliderUserValuesCsvs[dv] =
            whichDTCol >= 0
                ? // has distinctOptions
                EDV.getSliderCSVFromDistinctOptions(distinctOptions[whichDTCol])
                : edv.sliderCsvValues();
        // System.out.println(">>sliderCSV#" + dv + " in use=" + sliderUserValuesCsvs[dv] + "\n>>
        // suggested=" + edv.sliderCsvValues());
        sliderInitFromPositions[dv] = edv.closestSliderPosition(tValD[0]);
        sliderInitToPositions[dv] = edv.closestSliderPosition(tValD[1]);
        if (sliderInitFromPositions[dv] == -1) sliderInitFromPositions[dv] = 0;
        if (sliderInitToPositions[dv] == -1) sliderInitToPositions[dv] = EDV.SLIDER_PIXELS - 1;
        writer.write(
            "<tr>\n"
                + "  <td colspan=\"5\">\n"
                + widgets.spacer(10, 1, "")
                + // was "style=\"text-align:left\"") +
                widgets.dualSlider(dv, EDV.SLIDER_PIXELS - 1, "")
                + // was "style=\"text-align:left\"") +
                "  </td>\n"
                + "  <td colspan=\"2\"></td>\n"
                + "</tr>\n");
      }
    }

    // end of table
    writer.write(widgets.endTable());

    // addVariablesWhere
    if (addVariablesWhereAttNames != null) {
      // for each addVariableWhereAttributeName...
      writer.write(widgets.beginTable("class=\"compact nowrap\""));
      for (int attNamei = 1;
          attNamei < addVariablesWhereAttNames.size();
          attNamei++) { // 1 since [0] is ""
        // is there an addVariablesWhere(attName, attValue) in the query?
        // If there are 2+, only the first is used.
        String attName = addVariablesWhereAttNames.get(attNamei);
        String startsWith = "addVariablesWhere(\"" + attName + "\",";
        String sStartsWith = String2.stringStartsWith(queryParts, startsWith);
        int attValuei = 0;
        if (sStartsWith != null && sStartsWith.endsWith("\")")) {
          String avwAr[] =
              StringArray.arrayFromCSV(sStartsWith.substring(18, sStartsWith.length() - 1));
          if (avwAr.length == 2) {
            String attValue = avwAr[1];
            attValuei =
                Math.max(
                    0,
                    addVariablesWhereAttValues
                        .get(attNamei)
                        .indexOf(attValue)); // a valid number even if not a match
          }
        }

        writer.write(
            "<tr>\n"
                + "<td>Add variables where "
                + XML.encodeAsHTML(attName)
                +
                // make hidden widget with attName
                widgets.hidden("avwan" + attNamei, attName)
                + "<td>&nbsp;=&nbsp;"
                +
                // make the Select widget
                widgets.select(
                    "avwav" + attNamei,
                    EDStatic.messages.get(Message.ADD_VAR_WHERE_ATT_VALUE, language),
                    1,
                    addVariablesWhereAttValues.get(attNamei).toArray(),
                    attValuei,
                    "")
                + "\n"
                + EDStatic.htmlTooltipImage(
                    request,
                    language,
                    loggedInAs,
                    EDStatic.messages.get(Message.ADD_VAR_WHERE, language))
                + "</tr>\n");
      }
      writer.write(widgets.endTable());
    }

    // functions
    int nOrderByComboBox = 5;
    writeFunctionHtml(
        request, language, loggedInAs, queryParts, writer, widgets, formName, nOrderByComboBox);

    // fileType
    writer.write(
        "<p><strong>"
            + EDStatic.messages.get(Message.EDD_FILE_TYPE, language)
            + "</strong>\n"
            + " (<a rel=\"help\" href=\""
            + tErddapUrl
            + "/tabledap/documentation.html#fileType\">"
            + EDStatic.messages.get(Message.MORE_INFORMATION, language)
            + "</a>)\n");
    List<String> fileTypeDescriptions =
        EDD_FILE_TYPE_INFO.values().stream()
            .filter(fileTypeInfo -> fileTypeInfo.getAvailableTable())
            .map(
                fileTypeInfo ->
                    fileTypeInfo.getFileTypeName()
                        + " - "
                        + fileTypeInfo.getTableDescription(language))
            .toList();
    int defaultIndex =
        EDD_FILE_TYPE_INFO.values().stream()
            .filter(fileTypeInfo -> fileTypeInfo.getAvailableTable())
            .map(fileTypeInfo -> fileTypeInfo.getFileTypeName())
            .toList()
            .indexOf(defaultFileTypeOption);
    writer.write(
        widgets.select(
            "fileType",
            EDStatic.messages.get(Message.EDD_SELECT_FILE_TYPE, language),
            1,
            fileTypeDescriptions,
            defaultIndex,
            ""));

    // generate the javaScript
    String javaScript =
        "var result = \"\";\n"
            + "try {\n"
            + "  var d = document;\n"
            + "  var rv = [];\n"
            + // list of resultsVariables
            "  var q2 = \"\";\n"
            + // &constraints
            "  for (var dv = 0; dv < "
            + nDv
            + "; dv++) {\n"
            + "    var tVar = eval(\"d."
            + formName
            + ".varch\" + dv);\n"
            + "    if (tVar.checked) rv.push(tVar.value);\n"
            + "    var tOp  = eval(\"d."
            + formName
            + ".op\"  + dv + \"_0\");\n"
            + "    var tVal = eval(\"d."
            + formName
            + ".val\" + dv + \"_0\");\n"
            + "    if (tVal.value.length > 0) q2 += \"\\x26\" + tVar.value + tOp.value + percentEncode(tVal.value);\n"
            + "    tOp  = eval(\"d."
            + formName
            + ".op\"  + dv + \"_1\");\n"
            + "    tVal = eval(\"d."
            + formName
            + ".val\" + dv + \"_1\");\n"
            + "    if (tVal.value.length > 0) q2 += \"\\x26\" + tVar.value + tOp.value + percentEncode(tVal.value);\n"
            + "  }\n"
            + (addVariablesWhereAttNames == null
                ? ""
                : "  for (var avi = 1; avi < "
                    + addVariablesWhereAttNames.size()
                    + "; avi++) {\n"
                    + // 1 since [0] is ""
                    "    var avwanv = eval(\"d."
                    + formName
                    + ".avwan\" + avi + \".value\");\n"
                    + "    var avwavv = eval(\"d."
                    + formName
                    + ".avwav\" + avi + \".value\");\n"
                    + "    if (avwanv.length > 0 && avwavv.length > 0)\n"
                    + "      q2 += \"\\x26addVariablesWhere(%22\" + percentEncode(avwanv) + \"%22%2C%22\" + percentEncode(avwavv) + \"%22)\";\n"
                    + "  }\n")
            + functionJavaScript(formName, nOrderByComboBox)
            + "  var ft = d."
            + formName
            + ".fileType.value;\n"
            + "  result = \""
            + tErddapUrl
            + "/tabledap/"
            + datasetID
            + "\" + ft.substring(0, ft.indexOf(\" - \")) + \"?\" + percentEncode(rv.toString()) + q2;\n"
            + // if all is well, make result
            "} catch (e) {alert(e);}\n"
            + // result is in 'result'  (or "" if error)
            "d."
            + formName
            + ".tUrl.value = result;\n";

    // just generate URL
    writer.write("<br>");
    String genViewHtml =
        "<div class=\"standard_max_width\">"
            + String2.replaceAll(
                EDStatic.messages.get(Message.JUST_GENERATE_AND_VIEW_TOOLTIP, language),
                "&protocolName;",
                dapProtocol)
            + "</div>";
    writer.write(
        widgets.button(
            "button",
            "getUrl",
            genViewHtml,
            EDStatic.messages.get(Message.JUST_GENERATE_AND_VIEW, language),
            // "class=\"skinny\" " + //only IE needs it but only IE ignores it
            "onclick='" + javaScript + "'"));
    writer.write(
        widgets.textField(
            "tUrl",
            EDStatic.messages.get(Message.JUST_GENERATE_AND_VIEW_URL, language),
            60,
            1000,
            "",
            ""));
    writer.write(
        "\n<br>(<a rel=\"help\" href=\""
            + tErddapUrl
            + "/tabledap/documentation.html\" "
            + "title=\"tabledap documentation\">Documentation&nbsp;/&nbsp;Bypass&nbsp;this&nbsp;form</a>\n"
            + EDStatic.htmlTooltipImage(request, language, loggedInAs, genViewHtml)
            + ")\n");

    // submit
    writer.write(
        "<br>&nbsp;\n"
            + "<br>"
            + widgets.htmlButton(
                "button",
                "submit1",
                "",
                EDStatic.messages.get(Message.SUBMIT_TOOLTIP, language),
                "<span style=\"font-size:large;\"><strong>"
                    + EDStatic.messages.get(Message.SUBMIT, language)
                    + "</strong></span>",
                "onclick='"
                    + javaScript
                    + "if (result.length > 0) window.location=result;\n"
                    + // or open a new window: window.open(result);\n" +
                    "'")
            + " "
            + EDStatic.messages.get(Message.PATIENT_DATA, language)
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

    // be nice
    writer.flush();
  }

  /**
   * This writes the Function options for the Make A Graph and Data Access Form web pages.
   *
   * @param language the index of the selected language
   * @param nOrderByComboBox The number of variable list comboBoxes must be 4 or 5.
   */
  void writeFunctionHtml(
      HttpServletRequest request,
      int language,
      String loggedInAs,
      String[] queryParts,
      Writer writer,
      HtmlWidgets widgets,
      String formName,
      int nOrderByComboBox)
      throws Throwable {

    writer.write("\n&nbsp;\n"); // necessary for the blank line before the table (not <p>)
    writer.write(widgets.beginTable("class=\"compact W\"")); // not nowrap
    writer.write(
        "<tr><td><strong>"
            + EDStatic.messages.get(Message.FUNCTIONS, language)
            + "</strong> "
            + EDStatic.htmlTooltipImage(
                request,
                language,
                loggedInAs,
                "<div class=\"narrow_max_width\">"
                    + EDStatic.messages.get(Message.FUNCTION_TOOLTIP, language)
                    + "</div>")
            + "</td></tr>\n");

    // distinct function
    writer.write("<tr><td>");
    writer.write(
        widgets.checkbox(
            "distinct",
            EDStatic.messages.get(Message.FUNCTION_DISTINCT_CHECK, language),
            String2.indexOf(queryParts, "distinct()") >= 0,
            "true",
            "distinct()",
            ""));
    writer.write(
        EDStatic.htmlTooltipImage(
            request,
            language,
            loggedInAs,
            "<div class=\"narrow_max_width\">"
                + EDStatic.messages.get(Message.FUNCTION_DISTINCT_TOOLTIP, language)
                + "</div>"));
    writer.write("</td></tr>\n");

    // orderBy... function
    StringArray dvNames0 = new StringArray(dataVariableDestinationNames);
    dvNames0.atInsert(0, "");
    String dvList0[] = dvNames0.toArray();
    String important[] =
        nOrderByComboBox == 4
            ?
            // "most", "second most", "third most", ["fourth most",] "least"};
            new String[] {
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT1, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT2, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT3, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT_LEAST, language)
            }
            : new String[] {
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT1, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT2, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT3, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT4, language),
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT_LEAST, language)
            };
    // find first part that uses orderBy...
    String obPart = null;
    int whichOb = 0;
    for (int ob = 1; ob < orderByOptions.size(); ob++) { // 1 because option[0] is ""
      obPart = String2.stringStartsWith(queryParts, orderByOptions.get(ob) + "(\"");
      if (obPart != null) {
        if (obPart.endsWith("\")")) {
          whichOb = ob;
          break;
        } else {
          obPart = null;
        }
      }
    }
    // String2.log("obPart=" + obPart); //orderBy("station,time/1day")
    StringArray obsa =
        whichOb > 0
            ? StringArray.fromCSV(
                obPart.substring(orderByOptions.get(whichOb).length() + 2, obPart.length() - 2))
            : // the part inside the ""
            new StringArray();
    obsa.trimAll();
    obsa.removeIfNothing();

    writer.write("\n<tr><td>"); // not nowrap.  allow wrapping if varNames are long
    writer.write(widgets.select("orderBy", "", 1, orderByOptions, whichOb, ""));
    writer.write(
        EDStatic.htmlTooltipImage(
            request,
            language,
            loggedInAs,
            EDStatic.messages.get(Message.FUNCTION_ORDER_BY_TOOLTIP, language)));
    writer.write("(\"");
    for (int ob = 0; ob < nOrderByComboBox; ob++) {
      // if (ob > 0) writer.write(",\n");
      String tooltip =
          MessageFormat.format(
              EDStatic.messages.get(Message.FUNCTION_ORDER_BY_SORT, language), important[ob]);
      writer.write(
          widgets.comboBox(
              language,
              formName,
              "orderByComboBox" + ob,
              tooltip,
              18,
              120,
              ob < obsa.size() ? obsa.get(ob) : "",
              dvList0,
              "",
              null));
      writer.write("<wbr>");
    }
    writer.write("\")");
    writer.write("</td></tr>\n");

    writer.write(widgets.endTable());
  }

  /**
   * This writes the JavaScript for the Function options for the Make A Graph and Data Access Form
   * web pages. rv holds the list of results variables. q2 holds the &amp; queries.
   */
  String functionJavaScript(String formName, int nOrderByComboBox) throws Throwable {

    return "\n"
        + "    if (d."
        + formName
        + ".distinct.checked) q2 += \"\\x26distinct()\";\n"
        + "\n"
        + "    var nActiveOb = 0;\n"
        + "    var ObOS = d."
        + formName
        + ".orderBy;\n"
        + // the OrderBy... option Select widget
        "    if (ObOS.selectedIndex > 0) {\n"
        + // 0 is ""
        "      var tq2 = \"\\x26\" + ObOS.value + \"(%22\";\n"
        + // &orderBy...("
        "      for (var ob = 0; ob < "
        + nOrderByComboBox
        + "; ob++) {\n"
        + "        var tOb = eval(\"d."
        + formName
        + ".orderByComboBox\" + ob);\n"
        + "        var obVal = tOb.value.trim();\n"
        + "        if (obVal != \"\") {\n"
        + "          tq2 += (nActiveOb++ == 0? \"\" : \"%2C\") + obVal;\n"
        + // obVal
        "          var obVar = obVal.split(\"/\")[0].trim();\n"
        + // obVar  (no interval)
        //             last item in orderByLimit is just a number  '0'=48 '9'=57. If number, don't
        // include in list of vars.
        "          if (obVar.length > 0 && (obVar.charCodeAt(0) < 48 || obVar.charCodeAt(0) > 57) && rv.indexOf(obVar) < 0) rv.push(obVar);\n"
        + // make sure var is in list of results variables
        "        }\n"
        + "      }\n"
        + "      q2 += tq2 + \"%22)\";\n"
        + //  ")
        "    }\n"
        + "\n";
  }

  /**
   * This write HTML info on forming OPeNDAP DAP-style requests for this type of dataset.
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

    String dapBase = EDStatic.messages.EDDTableErddapUrlExample + dapProtocol + "/";
    String datasetBase = dapBase + EDStatic.messages.EDDTableIdExample;

    // all of the fullXxx examples are encoded for Html-Example or Html-Attribute-Encoded
    String fullDdsExample = datasetBase + ".dds";

    // variants encoded to be Html Examples
    String fullValueExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableDataValueExampleHE;
    String fullTimeExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableDataTimeExampleHE;
    String fullTimeCsvExampleHE =
        datasetBase + ".csv?" + EDStatic.messages.EDDTableDataTimeExampleHE;
    String fullTimeMatExampleHE = datasetBase + ".mat?" + EDStatic.messages.EDDTableGraphExampleHE;
    String fullGraphExampleHE = datasetBase + ".png?" + EDStatic.messages.EDDTableGraphExampleHE;
    String fullGraphMAGExampleHE =
        datasetBase + ".graph?" + EDStatic.messages.EDDTableGraphExampleHE;
    String fullGraphDataExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableGraphExampleHE;
    String fullMapExampleHE = datasetBase + ".png?" + EDStatic.messages.EDDTableMapExampleHE;
    String fullMapMAGExampleHE = datasetBase + ".graph?" + EDStatic.messages.EDDTableMapExampleHE;
    String fullMapDataExampleHE =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableMapExampleHE;

    // variants encoded to be Html Attributes
    String fullValueExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableDataValueExampleHA;
    String fullTimeExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableDataTimeExampleHA;
    String fullGraphExampleHA = datasetBase + ".png?" + EDStatic.messages.EDDTableGraphExampleHA;
    String fullGraphMAGExampleHA =
        datasetBase + ".graph?" + EDStatic.messages.EDDTableGraphExampleHA;
    String fullGraphDataExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableGraphExampleHA;
    String fullMapExampleHA = datasetBase + ".png?" + EDStatic.messages.EDDTableMapExampleHA;
    String fullMapMAGExampleHA = datasetBase + ".graph?" + EDStatic.messages.EDDTableMapExampleHA;
    String fullMapDataExampleHA =
        datasetBase + ".htmlTable?" + EDStatic.messages.EDDTableMapExampleHA;

    GregorianCalendar daysAgo7Gc = Calendar2.newGCalendarZulu();
    daysAgo7Gc.add(Calendar2.DATE, -7);
    String daysAgo7 = Calendar2.formatAsISODate(daysAgo7Gc);

    writer.write(
        "<h2><a class=\"selfLink\" id=\"instructions\" href=\"#instructions\" rel=\"bookmark\"\n"
            + "      >"
            + EDStatic.messages.get(Message.USING_TABLEDAP, language)
            + "</a></h2>\n"
            + longDapDescription(language, tErddapUrl)
            + "<p><strong>Tabledap request URLs must be in the form</strong>\n"
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
            + "<br>Thus, the query is often a comma-separated list of desired variable names,\n"
            + "   followed by a collection of\n"
            + "  constraints (e.g., <kbd><i>variable</i>&lt;<i>value</i></kbd>),\n"
            + "  each preceded by '&amp;' (which is interpreted as \"AND\").\n"
            + "\n");

    if (!complete) {
      writer.write(
          "<p>For details, see the <a rel=\"help\" href=\""
              + tErddapUrl
              + "/tabledap/"
              + "documentation.html\">"
              + dapProtocol
              + " Documentation</a>.\n");
      return;
    }

    // details
    writer.write(
        "<p><strong>Details:</strong>\n"
            + "<ul>\n"
            + "<li>Requests must not have any internal spaces.\n"
            + "<li>Requests are case sensitive.\n"
            + "<li>{} is notation to denote an optional part of the request.\n"
            + "  <br>&nbsp;\n"
            +

            // datasetID
            "<li><a class=\"selfLink\" id=\"datasetID\" href=\"#datasetID\" rel=\"bookmark\"><strong>datasetID</strong></a>\n"
            + "      identifies the name that ERDDAP\n"
            + "  assigned to the source website and dataset\n"
            + "  (for example, <kbd>"
            + EDStatic.messages.EDDTableIdExample
            + "</kbd>). You can see a list of\n"
            + "    <a rel=\"bookmark\" href=\""
            + EDStatic.phEncode(tErddapUrl + "/" + dapProtocol)
            + "/index.html\">datasetID options available via tabledap</a>.\n"
            + "  <br>&nbsp;\n"
            +

            // fileType
            "<li><a class=\"selfLink\" id=\"fileType\" href=\"#fileType\" rel=\"bookmark\"><strong>fileType</strong></a>\n"
            + "       specifies the type of table data file that you "
            + "  want to download (for example, <kbd>.htmlTable</kbd>).\n"
            + "  The actual extension of the resulting file may be slightly different than the fileType (for example,\n"
            + "  <kbd>.htmlTable</kbd> returns an .html file).\n"
            +

            // Which File Type?
            "  <p>Which fileType should I use?\n"
            + "  <br>It's entirely up to you. In general, pick the fileType which meets your needs and is easiest to use. You will\n"
            + "  use different fileTypes in different situations, e.g., viewing a graph in a browser (.png) vs. viewing the data\n"
            + "  in a browser (.htmlTable) vs. downloading a data file (e.g., .nc or .csv) vs. working with some software tool\n"
            + "  (e.g., .nc or .odv). If you are going to download lots of large files, you might also want to give some weight\n"
            + "  to fileTypes that are more compact and so can be downloaded faster.\n"
            + "  <p>The fileType options for downloading tabular data are:\n"
            + "  <br>&nbsp;\n"
            + "  <table class=\"erd\" style=\"width:100%; \">\n"
            + "    <tr><th>Data<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
    List<EDDFileTypeInfo> dataFileTypes = EDD.getFileTypeOptions(false, false);
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
              + curType.getTableDescription(language)
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
              + EDStatic.messages.EDDTableDataTimeExampleHA
              + "\">example</a></td>\n"
              + "    </tr>\n");
    }
    writer.write(
        "   </table>\n"
            + "   <br>For example, here is a complete request URL to download data formatted as an HTML table:\n"
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
            + "</a>\n"
            + "  <a rel=\"help\" href=\"https://desktop.arcgis.com/en/arcmap/latest/manage-data/tables/adding-an-ascii-or-text-file-table.htm\">.esriCsv"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "  - <a class=\"selfLink\" id=\"ArcGIS\" href=\"#ArcGIS\" rel=\"bookmark\">ArcGIS</a>\n"
            + "      is a family of Geographical Information Systems (GIS) products from ESRI:\n"
            + "  ArcView, ArcEditor, and ArcInfo.  To get data from ERDDAP into your ArcGIS program (version 9.x and below):\n"
            + "  <ol>\n"
            + "  <li>In ERDDAP, save some data (which must include longitude and latitude) in an .esriCsv file\n"
            + "    (which will have the extension .csv) in the directory where you usually store ArcGIS data files.\n"
            + "    In the file:\n"
            + "    <ul>\n"
            + "    <li> Column names have been changed to be 10 characters or less\n"
            + "     (sometimes with A, B, C, ... at the end to make them unique).\n"
            + "    <li> <kbd>longitude</kbd> is renamed <kbd>X</kbd>. <kbd>latitude</kbd> is renamed\n"
            + "      <kbd>Y</kbd> to make them the default coordinate fields.\n"
            + "    <li> Missing numeric values are written as -9999.\n"
            + "    <li> Double quotes in strings are double quoted.\n"
            + "    <li> Timestamp columns are separated into date (ISO 8601) and time (am/pm) columns.\n"
            + "    </ul>\n"
            + "  <li>In ArcMap, navigate to <kbd>Tools : Add XY Data</kbd> and select the file.\n"
            + "  <li>Click on <kbd>Edit</kbd> to open the Spatial Reference Properties dialog box.\n"
            + "  <li>Click on <kbd>Select</kbd> to select a coordinate system.\n"
            + "  <li>Open the <kbd>World</kbd> folder and select <kbd>WGS_1984.prj</kbd>.\n"
            + "  <li>Click a <kbd>Add, Apply</kbd> and <kbd>OK</kbd> on the Spatial Reference Properties dialog box.\n"
            + "  <li>Click <kbd>OK</kbd> on the Add XY Data dialog box.\n"
            + "  <li>Optional: save the data as a shapefile or into a geodatabase by right clicking\n"
            + "    on the data set and choosing <kbd>Data : Export Data</kbd> ...\n"
            + "    <br>&nbsp;\n"
            + "  </ol>\n"
            + "  The points will be drawn as an Event theme in ArcMap.\n"
            + "  (These instructions are a modified version of\n"
            + "    <a rel=\"help\" href=\"https://desktop.arcgis.com/en/arcmap/latest/manage-data/tables/adding-an-ascii-or-text-file-table.htm\"\n"
            + "      >ESRI's instructions"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.)\n"
            + "\n"
            +
            // Ferret
            "  <p><strong><a rel=\"bookmark\" href=\"https://ferret.pmel.noaa.gov/Ferret/\">Ferret"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong> \n"
            + "    <a class=\"selfLink\" id=\"Ferret\" href=\"#Ferret\" rel=\"bookmark\">is</a>\n"
            + "    a free program for visualizing and analyzing large and complex\n"
            + "  <strong>gridded</strong> datasets.  Because tabledap's <strong>tabular</strong> datasets are very different\n"
            + "  from gridded datasets, it is necessary to use Ferret in a very specific way to\n"
            + "  avoid serious problems and misunderstandings:<ol>\n"
            + "    <li>Use the ERDDAP web pages or a program like\n"
            + "      <a rel=\"help\" href=\"#curl\">curl</a> to download a subset of a\n"
            + "      dataset in a plain .nc file.\n"
            + "    <li>Open that local file (which has the data in a gridded format) in Ferret.  The data\n"
            + "      isn't in an ideal format for Ferret, but you should be able to get it to work.\n"
            + "    <br>&nbsp;\n"
            + "    </ol>\n"
            + "  WARNING: Ferret won't like profile data (and perhaps other data), because the time\n"
            + "  variable will have the same time value for all of the rows of data for a given\n"
            + "  profile.  In fact, Ferret will add some number of seconds to the time values\n"
            + "  to make them all unique! So treat these time values accordingly.\n"
            + "\n"
            +
            // IDL
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.harrisgeospatial.com/Software-Technology/IDL/\">IDL"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong> - \n"
            + "    <a class=\"selfLink\" id=\"IDL\" href=\"#IDL\" rel=\"bookmark\">IDL</a>\n"
            + "    is a commercial scientific data visualization program. To get data from\n"
            + "  ERDDAP into IDL, first use ERDDAP to select a subset of data and download a .nc file.\n"
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
            + "    <a class=\"selfLink\" id=\"json\" href=\"#json\" rel=\"bookmark\">files</a>\n"
            + "    are widely used to transfer data to JavaScript scripts running on web pages.\n"
            + "  All .json responses from ERDDAP (metadata, gridded data, and tabular/in-situ data) use the\n"
            + "  same basic format: a database-like table.  Since data from EDDTable datasets is already a table\n"
            + "  (with a column for each requested variable), ERDDAP can easily store the data in a .json file.\n"
            + "  For example,\n"
            + "    <pre>\n"
            + "{\n"
            + "  \"table\": {\n"
            + "    \"columnNames\": [\"longitude\", \"latitude\", \"time\", \"bottle_posn\", \"temperature1\"],\n"
            + "    \"columnTypes\": [\"float\", \"float\", \"String\", \"byte\", \"float\"],\n"
            + "    \"columnUnits\": [\"degrees_east\", \"degrees_north\", \"UTC\", null, \"degree_C\"],\n"
            + "    \"rows\": [\n"
            + "      [-124.82, 42.95, \"2002-08-17T00:49:00Z\", 1, 8.086],\n"
            + "      [-124.82, 42.95, \"2002-08-17T00:49:00Z\", 2, 8.585],\n"
            + "      [-124.82, 42.95, \"2002-08-17T00:49:00Z\", 3, 8.776],\n"
            + "      ...\n"
            + "      [-124.1, 44.65, \"2002-08-19T20:18:00Z\", 3, null]\n"
            + "    ]\n"
            + "  }\n"
            + "}\n"
            + "</pre>\n"
            + "      All .json responses from ERDDAP have\n"
            + "      <ul>\n"
            + "      <li>a <kbd>table</kbd> object (with name=value pairs).\n"
            + "      <li>a <kbd>columnNames, columnTypes,</kbd> and <kbd>columnUnits</kbd> array,\n"
            + "        with a value for each column.\n"
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
            + "</a> and\n"
            + "    <a rel=\"help\" href=\"http://wiki.geojson.org/Main_Page\">.geoJson"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>)</strong> -\n"
            + "  <a class=\"selfLink\" id=\"jsonp\" href=\"#jsonp\" rel=\"bookmark\">Jsonp</a>\n"
            + "       is an easy way for a JavaScript script on a web\n"
            + "  page to import and access data from ERDDAP.  Requests for .geoJson, .json, and .ncoJson files may\n"
            + "  include an optional jsonp request by adding <kbd>&amp;.jsonp=<i>functionName</i></kbd>\n"
            + "  to the end of the query.\n"
            + "  Basically, this just tells ERDDAP to add <kbd><i>functionName</i>(</kbd>\n"
            + "  to the beginning of the response\n"
            + "  and <kbd>\")\"</kbd> to the end of the response.\n"
            + "  The functionName must be a series of 1 or more (period-separated) words.\n"
            + "  For each word, the first character of <i>functionName</i> must be an ISO 8859 letter or \"_\".\n"
            + "  Each optional subsequent character must be an ISO 8859 letter, \"_\", or a digit.\n"
            + "  If originally there was no query, leave off the \"&amp;\" in your query.\n"
            + "  After the data download to the web page has finished, the data is accessible to the JavaScript\n"
            + "  script via that JavaScript function.\n"
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
            + "     <a rel=\"help\" href=\"https://www.mathworks.com/help/pdf_doc/matlab/matfile_format.pdf\">.mat"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "   - <a class=\"selfLink\" id=\"matlab\" href=\"#matlab\" rel=\"bookmark\">Matlab</a>\n"
            + "      users can use tabledap's .mat file type to download data from within\n"
            + "  MATLAB.  Here is a one line example:<pre>\n"
            + "load(urlwrite('"
            + fullTimeMatExampleHE
            + "', 'test.mat'));</pre>\n"
            + "  (You may need to <a rel=\"help\" href=\"#PercentEncoded\">percent encode</a> the query part of the URL.)\n"
            + "  The data will be in a MATLAB structure. The structure's name will be the datasetID\n"
            + "  (for example, <kbd>"
            + EDStatic.messages.EDDTableIdExample
            + "</kbd>). \n"
            + "  The structure's internal variables will be column vectors with the same names\n"
            + "  as in ERDDAP \n"
            + "    (for example, use <kbd>fieldnames("
            + EDStatic.messages.EDDTableIdExample
            + ")</kbd>). \n"
            + "  You can then make a scatterplot of any two columns. For example:<pre>\n"
            + EDStatic.messages.EDDTableMatlabPlotExample
            + "</pre>\n"
            + "  <p>ERDDAP stores datetime values in .mat files as \"seconds since 1970-01-01T00:00:00Z\".\n"
            + "  To display one of these values as a String in Matlab, you can use, e.g.,\n"
            + "  <kbd>datastr(cwwcNDBCMet.time(1)/86400 + 719529)</kbd>\n"
            + "  86400 converts ERDDAP's \"seconds since\" to Matlab's \"days since\". 719529 converts\n"
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
            + "     - <a class=\"selfLink\" id=\"nc\" href=\"#nc\" rel=\"bookmark\">Requests</a>\n"
            + "      for .nc files will always return the data in a table-like, NetCDF-3,\n"
            + "  32-bit, .nc file:\n"
            + "  <ul>\n"
            + "  <li>All variables will use the file's \"row\" dimension.\n"
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
            + "  <p>Don't use NetCDF-Java, NetCDF-C, NetCDF-Fortran, NetCDF-Perl, or Ferret to try to\n"
            + "  access a remote ERDDAP .nc file.  It won't work.  Instead, use\n"
            + "  <a href=\"#netcdfjava\">this approach</a>.\n"
            + "\n"
            +
            /*            //nc4
                        "  <p><strong><a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\">NetCDF" +
                                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>\n" +
                        "    <a rel=\"help\" href=\"https://www.unidata.ucar.edu/software/netcdf/docs/file_format_specifications.html\">.nc4" +
                                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a></strong>\n" +
                        "     - <a class=\"selfLink\" id=\"nc4\" href=\"#nc4\" rel=\"bookmark\">Requests</a>\n" +
                        "       for .nc4 files will always return the data in a table-like, NetCDF-4,\n" +
                        "  64-bit, .nc file:\n" +
                        "  <ul>\n" +
                        "  <li>All variables will use the file's \"row\" dimension.\n" +
                        "  <li>All String variables will stored as true Strings in the .nc file.\n" +
                        "  <li>All long variables will be stored as longs in the .nc file.\n" +
                        "  <li>Only the low bytes of char variables will be stored, using the ISO-8859-1 charset.\n" +
                        "  </ul>\n" +
                        "  <p>Don't use NetCDF-Java, NetCDF-C, NetCDF-Fortran, NetCDF-Perl, or Ferret to try to\n" +
                        "  access a remote ERDDAP .nc file.  It won't work.  Instead, use\n" +
                        "  <a href=\"#netcdfjava\">this approach</a>.\n" +
                        "\n" +
                        //nc4Header
                        "  <p><strong>.ncHeader</strong> and <strong>.nc4Header</strong>\n" +
                        "    - <a class=\"selfLink\" id=\"ncHeader\" href=\"#ncHeader\" rel=\"bookmark\">Requests</a>\n" +
                        "      for .ncHeader and .nc4Header files will return the header information (UTF-8 text)\n" +
                        "  that would be generated if you used\n" +
                        "    <a rel=\"help\" href=\"https://linux.die.net/man/1/ncdump\"\n" +
                        "      >ncdump -h <i>fileName</i>" +
                                EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>\n" +
                        "    on the corresponding NetCDF-3 or NetCDF-4 .nc file.\n" +
                        "\n" +
            */
            // ncCF
            "  <p><a class=\"selfLink\" id=\"ncCF\" href=\"#ncCF\" rel=\"bookmark\"><strong>.ncCF</strong></a>\n"
            + "     - Requests for a .ncCF file will return a version 3, 32-bit,\n"
            + "  <a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\">NetCDF .nc"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "  file with the\n"
            + "  Contiguous Ragged Array Representation associated with the dataset's cdm_data_type,\n"
            + "  as defined in the\n"
            + "    <a rel=\"help\" href=\"https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html\">CF"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    <a rel=\"help\" href=\"https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries\"\n"
            + "      >Discrete Geometries"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> conventions\n"
            + "  (which were previously named \"CF Point Observation Conventions\").\n"
            + "  <ul>\n"
            + "  <li>Point - Appendix H.1 Point Data\n"
            + "  <li>TimeSeries - Appendix H.2.4 Contiguous ragged array representation of timeSeries\n"
            + "  <li>Profile - Appendix H.3.4 Contiguous ragged array representation of profiles\n"
            + "  <li>Trajectory - Appendix H.4.3 Contiguous ragged array representation of trajectories\n"
            + "  <li>TimeSeriesProfile - Appendix H.5.3 Ragged array representation of timeSeriesProfiles\n"
            + "  <li>TrajectoryProfile - Appendix H.6.3 Ragged array representation of trajectoryProfiles\n"
            + "    <br>&nbsp;\n"
            + "  </ul>\n"
            + "  A request will succeed only if the dataset has a cdm_data_type other than \"Other\"\n"
            + "  and if the request includes at least one data variable (not just the outer, descriptive variables).\n"
            + "  The file will include longitude, latitude, time, and other required descriptive variables, even if\n"
            + "  you don't request them.\n"
            + "\n"
            +
            // ncCFHeader
            "  <p><strong>.ncCFHeader</strong>\n"
            + "    - <a class=\"selfLink\" id=\"ncCFHeader\" href=\"#ncCFHeader\" rel=\"bookmark\">Requests</a>\n"
            + "      for .ncCFHeader files will return the header information (text) that\n"
            + "  would be generated if you used\n"
            + "    <a rel=\"help\" href=\"https://linux.die.net/man/1/ncdump\">ncdump -h <i>fileName</i>"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    on the corresponding .ncCF file.\n"
            + "\n"
            +
            // ncCFMA
            "  <p><a class=\"selfLink\" id=\"ncCFMA\" href=\"#ncCFMA\" rel=\"bookmark\"><strong>.ncCFMA</strong></a>\n"
            + "     - Requests for a .ncCFMA file will return a version 3, 32-bit,\n"
            + "    <a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\">NetCDF .nc"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> file\n"
            + "   with the Complete or Incomplete, depending on the data, Multidimensional Array Representation\n"
            + "   associated with the dataset's cdm_data_type, as defined in the\n"
            + "     <a rel=\"help\" href=\"https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html\">CF"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "     <a rel=\"help\" href=\"https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries\"\n"
            + "      >Discrete Sampling Geometries"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "   conventions, which were previously named \"CF Point Observation Conventions\".\n"
            + "   This is the file type used by the <a rel=\"help\" href=\"https://www.ncei.noaa.gov/netcdf-templates\">NODC Templates"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "   A request will succeed only if the dataset has a cdm_data_type other than \"Other\"\n"
            + "   and if the request includes at least one data variable (not just the outer, descriptive variables).\n"
            + "   The file will include longitude, latitude, time, and other required descriptive variables, even if\n"
            + "   you don't request them.\n"
            + "\n"
            +
            // ncCFMAHeader
            "  <p><strong>.ncCFMAHeader</strong>\n"
            + "    - <a class=\"selfLink\" id=\"ncCFMAHeader\" href=\"#ncCFMAHeader\" rel=\"bookmark\">Requests</a>\n"
            + "      for .ncCFMAHeader files will return the header information (text) that\n"
            + "  would be generated if you used\n"
            + "    <a rel=\"help\" href=\"https://linux.die.net/man/1/ncdump\">ncdump -h <i>fileName</i>"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "    on the corresponding .ncCFMA file.\n"
            + "\n"
            +
            // netcdfjava
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\"\n"
            + "      >NetCDF-Java, NetCDF-C, NetCDF-Fortran, and NetCDF-Perl"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "  <a class=\"selfLink\" id=\"netcdfjava\" href=\"#netcdfjava\" rel=\"bookmark\">-</a>\n"
            + "  Don't try to access an ERDDAP tabledap dataset URL directly with a NetCDF library or tool\n"
            + "  (by treating the tabledap dataset as an OPeNDAP dataset or by creating a URL with the .nc file extension).\n"
            + "  It won't work.\n"
            + "    <ol>\n"
            + "    <li>These libraries and tools just work with OPeNDAP gridded data.\n"
            + "      Tabledap offers data as OPeNDAP sequence data, which is a different data structure.\n"
            + "    <li>These libraries and tools just work with actual, static, .nc files.\n"
            + "      But an ERDDAP URL with the .nc file extension is a virtual file. It doesn't actually exist.\n"
            + "    </ol>\n"
            + "  <p>Fortunately, there is a two step process that does work:\n"
            + "    <ol>\n"
            + "    <li>Use the ERDDAP web pages or a program like\n"
            + "      <a rel=\"help\" href=\"#curl\">curl</a> to download a .nc file with a subset of the dataset.\n"
            + "    <li>Open that local .nc file (which has the data in a grid format)\n"
            + "      with one of the NetCDF libraries or tools.\n"
            + "      With NetCDF-Java for example, use:\n"
            + "      <br><kbd>NetcdfFile nc = NetcdfFile.open(\"c:\\downloads\\theDownloadedFile.nc\");  //pre v5.4.1</kbd>\n"
            + "      <br><kbd>NetcdfFile nc = NetcdfFiles.open(\"c:\\downloads\\theDownloadedFile.nc\"); //v5.4.1+</kbd>\n"
            + "      <br><kbd>NetcdfDataset nc = NetcdfDataset.openDataset(\"c:\\downloads\\theDownloadedFile.nc\");  //pre v5.4.1</kbd>\n"
            + "      <br>or\n"
            + "      <br><kbd>NetcdfDataset nc = NetcdfDatasets.openDataset(\"c:\\downloads\\theDownloadedFile.nc\"); //v5.4.1+</kbd>\n"
            + "      <br>(NetcdfFiles are a lower level approach than NetcdfDatasets.  It is your choice.)\n"
            + "      In both cases, you can then do what you want with the <kbd>nc</kbd> object, for example,\n"
            + "      request metadata or request a subset of a variable's data as you would with any other\n"
            + "      .nc file.\n"
            + "    </ol>\n"
            + "\n"
            +
            // odv
            "  <p><strong><a rel=\"bookmark\" href=\"https://odv.awi.de/\">Ocean Data View"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> .odvTxt</strong>\n"
            + "   - <a class=\"selfLink\" id=\"ODV\" href=\"#ODV\" rel=\"bookmark\">ODV</a> users can download data in a\n"
            + "    <a rel=\"help\" href=\"https://odv.awi.de/en/documentation/\">ODV Generic Spreadsheet Format .txt file"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "  by requesting tabledap's .odvTxt fileType.\n"
            + "  The selected data MUST include longitude, latitude, and time variables.\n"
            + "  Any longitude values (0 to 360, or -180 to 180) are fine.\n"
            + "  After saving the resulting file (with the extension .txt) in your computer:\n"
            + "  <ol>\n"
            + "  <li>Open ODV.\n"
            + "  <li>Use <kbd>File : Open</kbd>.\n"
            + "    <ul>\n"
            + "    <li>Change <kbd>Files of type</kbd> to <kbd>Data Files (*.txt *.csv *.jos *.o4x)</kbd>.\n"
            + "    <li>Browse to select the .txt file you created in ERDDAP and click on <kbd>Open</kbd>.\n"
            + "      The data should now be visible on a map in ODV.\n"
            + "    </ul>\n"
            + "  <li>See ODV's <kbd>Help</kbd> menu for more help using ODV.\n"
            + "  </ol>\n"
            + "\n"
            +
            // opendapLibraries
            "  <p><strong><a class=\"selfLink\" id=\"opendapLibraries\" href=\"#opendapLibraries\" rel=\"bookmark\"\n"
            + "      >OPeNDAP Libraries</a></strong> - Although ERDDAP is an\n"
            + "    <a rel=\"bookmark\" href=\"https://www.opendap.org/\">OPeNDAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>-compatible data server,\n"
            + "    you can't use\n"
            + "  most OPeNDAP client libraries, including\n"
            + "  <a rel=\"bookmark\" href=\"https://www.unidata.ucar.edu/software/netcdf/\"\n"
            + "      >NetCDF-Java, NetCDF-C, NetCDF-Fortran, NetCDF-Perl"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  or\n"
            + "  <a rel=\"bookmark\" href=\"https://ferret.pmel.noaa.gov/Ferret/\">Ferret"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  to get data directly from an ERDDAP tabledap dataset because those libraries don't\n"
            + "  support the OPeNDAP Selection constraints that tabledap datasets use for requesting\n"
            + "  subsets of the dataset, nor do they support the sequence data structure in the response.\n"
            + "  (But see this <a href=\"#netcdfjava\">other approach</a> that works with NetCDF libraries.)\n"
            + "  But you can use the <a href=\"#PydapClient\">Pydap Client</a> or\n"
            + "  <a rel=\"bookmark\" href=\"https://www.opendap.org/deprecated-software/java-dap\">Java-DAP2"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "  because they both support Selection\n"
            + "  constraints.  With both the Pydap Client and Java-DAP2, when creating the initial\n"
            + "  connection to an ERDDAP table dataset, use the tabledap dataset's base URL, e.g.,\n"
            + "  <br><kbd>"
            + datasetBase
            + "</kbd>\n"
            + "  <ul>\n"
            + "  <li>Don't include a file extension (e.g., .nc) at the end of the dataset's name.\n"
            + "  <li>Don't specify a variable or a Selection-constraint.\n"
            + "    <br>&nbsp;\n"
            + "  </ul>\n"
            + "  Once you have made the connection to the dataset, you can request metadata or a\n"
            + "  subset of a variable's data via a Selection-constraint.\n"
            + "\n"
            +
            // Pydap Client
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.pydap.org/en/latest/client.html\">Pydap Client"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"PydapClient\" href=\"#PydapClient\" rel=\"bookmark\">users</a>\n"
            + "    can access tabledap datasets via ERDDAP's standard OPeNDAP services.\n"
            + "  See the\n"
            + "    <a rel=\"help\" href=\"https://www.pydap.org/en/latest/client.html#accessing-sequential-data\"\n"
            + "      >Pydap Client instructions for accessing sequential data"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  Note that the name of a dataset in tabledap will always be a single word, e.g.,\n"
            + EDStatic.messages.EDDTableIdExample
            + "\n"
            + "  in the OPeNDAP dataset URL\n"
            + "  <br><kbd>"
            + datasetBase
            + "</kbd>\n"
            + "  and won't ever have a file extension (unlike, for example, .cdp for the sample dataset in the\n"
            + "  Pydap instructions).  Also, the name of the sequence in tabledap datasets\n"
            + "  will always be \""
            + SEQUENCE_NAME
            + "\"\n"
            + "  (unlike \"location\" for the sample dataset in the Pydap instructions).\n"
            + "\n"
            +
            // Python
            "  <p><strong><a rel=\"bookmark\" href=\"https://www.python.org\">Python"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong>\n"
            + "    <a class=\"selfLink\" id=\"Python\" href=\"#Python\" rel=\"bookmark\">is</a>\n"
            + "    a widely-used computer language that is very popular among scientists.\n"
            + "    In addition to the <a rel=\"help\" href=\"#PydapClient\">Pydap Client</a>,\n"
            + "    you can use Python to download various files from ERDDAP\n"
            + "    as you would download other files from the web:\n"
            + "<pre>import urllib\n"
            + "urllib.urlretrieve(\"<i>https://baseUrl</i>/erddap/tabledap/<i>datasetID.fileType?query</i>\", \"<i>outputFileName</i>\")</pre>\n"
            + "  (You may need to <a rel=\"help\" href=\"#PercentEncoded\">percent encode</a> the query part of the URL.)\n"
            + "  <br>Or download the content to an object instead of a file:\n"
            + "<pre>import urllib2\n"
            + "response = urllib2.open(\"<i>https://baseUrl</i>/erddap/tabledap/<i>datasetID.fileType?query</i>\")\n"
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
            + "      <a class=\"selfLink\" id=\"JupyterNotebook\" href=\"#JupyterNotebook\" rel=\"bookmark\">is</a>\n"
            + "      an open-source web application that\n"
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
            + "    <a class=\"selfLink\" id=\"R\" href=\"#R\" rel=\"bookmark\">R</a>\n"
            + "    is an open source statistical package for many operating systems.\n"
            + "  In R, you can download a .csv file from ERDDAP\n"
            + "    and then import data from that .csv file\n"
            + "  into an R structure (e.g., <kbd>test</kbd>).  For example:<pre>\n"
            + "  download.file(url=\""
            + fullTimeCsvExampleHE
            + "\", destfile=\"/home/bsimons/test.csv\")\n"
            + "  test&lt;-read.csv(file=\"/home/bsimons/test.csv\")</pre>\n"
            + "  (You may need to <a rel=\"help\" href=\"#PercentEncoded\">percent encode</a> the query part of the URL.)\n"
            + "  <p>There are third-party R packages designed to make it easier to work with ERDDAP from within R:\n"
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
            + "\n"
            +
            // .wav
            "  <p><strong><a rel=\"bookmark\" href=\"https://en.wikipedia.org/wiki/WAV\">.wav"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></strong> -\n"
            + "    <a class=\"selfLink\" id=\"wav\" href=\"#wav\" rel=\"bookmark\">ERDDAP can return data in .wav files,</a>\n"
            + "    which are uncompressed audio files.\n"
            + "  <ul>\n"
            + "  <li>You can save any numeric data in .wav files, but this file format is clearly intended to be used\n"
            + "    with <a rel=\"bookmark\" href=\"https://en.wikipedia.org/wiki/Pulse-code_modulation\">PCM"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> digitized sound waves.\n"
            + "    We do not recommend saving other types of data in .wav files.\n"
            + "  <li>All of the data variables you select to save in a .wav file must have the same data type, e.g., int.\n"
            + "    Currently, because of a bug in Java 8, ERDDAP can't save float or double data in .wav files. That should\n"
            + "    change when ERDDAP is run with Java 9.\n"
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
        "  <p><a class=\"selfLink\" id=\"imageFileTypes\" href=\"#imageFileTypes\" rel=\"bookmark\"\n"
            + "    ><strong>Making an Image File with a Graph or Map of Tabular Data</strong></a>\n"
            + "    If a tabledap request URL specifies a subset of data which is suitable for making a graph or\n"
            + "    a map, and the fileType is an image fileType, tabledap will return an image with a graph or map. \n"
            + "    tabledap request URLs can include optional\n"
            + "    <a rel=\"help\" href=\"#GraphicsCommands\">graphics commands</a> which let you\n"
            + "    customize the graph or map.\n"
            + "    As with other tabledap request URLs, you can create these URLs by hand or have a computer\n"
            + "    program do it. \n"
            + "    Or, you can use the Make A Graph web pages, which simplify creating these URLs (see the\n"
            + "    \"graph\" links in the table of <a href=\""
            + dapBase
            + "index.html\">tabledap datasets</a>). \n"
            + "\n"
            + "   <p>The fileType options for downloading images of graphs and maps of table data are:\n"
            + "  <table class=\"erd\" style=\"width:100%; \">\n"
            + "    <tr><th>Image<br>fileTypes</th><th>Description</th><th>Info</th><th>Example</th></tr>\n");
    List<EDDFileTypeInfo> imageFileTypes = EDD.getFileTypeOptions(false, true);
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
              + curType.getTableDescription(language)
              + "</td>\n"
              + "      <td class=\"N\">"
              + (curType.getInfoUrl() == null || curType.getInfoUrl().isEmpty()
                  ? "&nbsp;"
                  : "<a rel=\"help\" href=\""
                      + curType.getInfoUrl()
                      + "\">info"
                      + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
                      + "</a>")
              + "</td>\n"
              + // must be mapExample below because kml doesn't work with graphExample
              "      <td class=\"N\"><a rel=\"bookmark\" href=\""
              + datasetBase
              + curType.getFileTypeName()
              + "?"
              + EDStatic.messages.EDDTableMapExampleHA
              + "\">example</a></td>\n"
              + "    </tr>\n");
    }
    writer.write(
        "   </table>\n"
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
            "  <p><a class=\"selfLink\" id=\"transparentPng\" href=\"#transparentPng\" rel=\"bookmark\"\n"
            + "  >.transparentPng</a> - The .transparentPng file type will make a graph or map without\n"
            + "  the graph axes, landmask, or legend, and with a transparent (not opaque white)\n"
            + "  background.  This file type can be used for any type of graph or map.\n"
            + "  For graphs and maps, the default size is 360x360 pixels.\n"
            + "  Or, you can use the <a rel=\"help\" href=\"#GraphicsCommands\">&amp;.size=<i>width</i>|<i>height</i></a>\n"
            + "    parameter to request an image of any size.\n"
            + "\n");

    // file type incompatibilities
    writer.write(
        "  <p><strong>Incompatibilities</strong>\n"
            + "  <br>Some results file types have restrictions. For example, Google Earth .kml is only\n"
            + "  appropriate for results with longitude and latitude values. If a given request is\n"
            + "  incompatible with the requested file type, tabledap throws an error.\n"
            + "\n"
            +

            // curl
            "<p><a class=\"selfLink\" id=\"curl\" href=\"#curl\" rel=\"bookmark\"\n"
            + "><strong>Command Line Downloads with curl</strong></a>\n"
            + "<br>If you want to download a series of files from ERDDAP, you don't have to request each file's\n"
            + "ERDDAP URL in your browser, sitting and waiting for each file to download.\n"
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
            + "<li>To download and save one file, use\n"
            + "  <br><kbd>curl --compressed -g \"<i>erddapUrl</i>\" -o <i>fileDir/fileName.ext</i></kbd>\n"
            + "  <br>where <kbd>--compressed</kbd> tells curl to request a compressed response and automatically decompress it,\n"
            + "  <br>&nbsp;&nbsp;<kbd>-g</kbd> disables curl's globbing feature,\n"
            + "  <br>&nbsp;&nbsp;<kbd><i>erddapUrl</i></kbd> is any ERDDAP URL that requests a data or image file, and\n"
            + "  <br>&nbsp;&nbsp;<kbd>-o <i>fileDir/fileName.ext</i></kbd> specifies the name for the file that will be created.\n"
            + "  <br>For example,\n"
            + "<pre>curl --compressed -g \"https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet.png?time,atmp&amp;time%3E=2010-09-03T00:00:00Z&amp;time%3C=2010-09-06T00:00:00Z&amp;station=%22TAML1%22&amp;.draw=linesAndMarkers&amp;.marker=5|5&amp;.color=0x000000&amp;.colorBar=|||||\" -o NDBCatmpTAML1.png</pre>\n"
            + "  <a class=\"N selfLink\" id=\"PercentEncoded\" href=\"#PercentEncoded\" rel=\"bookmark\"\n"
            + "  >In curl, as in many other programs, the query part of the erddapUrl must be</a>\n"
            + "  <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.  To do this, you need to convert\n"
            + "  special characters (other than the initial '&amp;' and the main '=') in all constraints\n"
            + "  into the form %HH, where HH is the 2 digit hexadecimal value of the character.\n"
            + "  Usually, you just need to convert a few of the punctuation characters: % into %25,\n"
            + "  &amp; into %26, \" into %22, &lt; into %3C, = into %3D, &gt; into %3E, + into %2B, | into %7C,\n"
            + "  space into %20, [ into %5B, ] into %5D,\n"
            + "  and convert all characters above #127 into their UTF-8 bytes and then percent encode\n"
            + "  each byte of the UTF-8 form into the %HH format (ask a programmer for help).\n"
            + "  For example, <kbd>&amp;stationID&gt;=\"41004\"</kbd>\n"
            + "  becomes <kbd>&amp;stationID%3E=%2241004%22</kbd>\n"
            + "  Note that percent encoding is generally required when you access ERDDAP via\n"
            + "  software other than a browser. Browsers usually handle percent encoding for you.\n"
            + "  In some situations, you need to percent encode all characters other than\n"
            + "  A-Za-z0-9_-!.~'()* .\n"
            + "  Programming languages have tools to do this (for example, see Java's\n"
            + "  <a rel=\"help\" href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URLEncoder.html\"\n"
            + "      >java.net.URLEncoder"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "     and JavaScript's\n"
            + "<a rel=\"help\" href=\"https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent\"\n"
            + "      >encodeURIComponent()"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>) and there are\n"
            + "  <a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "  <br>&nbsp;\n"
            + "<li>To download and save many files in one step, use curl with the globbing feature enabled:\n"
            + "  <br><kbd>curl --compressed \"<i>erddapUrl</i>\" -o <i>fileDir/fileName#1.ext</i></kbd>\n"
            + "  <br>Since the globbing feature treats the characters [, ], {, and } as special, you must also\n"
            + "  <br><a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encode"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> \n"
            + "them in the erddapURL as &#37;5B, &#37;5D, &#37;7B, &#37;7D, respectively.\n"
            + "  Fortunately, these are rare in tabledap URLs.\n"
            + "  Then, in the erddapUrl, replace a zero-padded number (for example <kbd>01</kbd>) with a range\n"
            + "  of values (for example, <kbd>[01-15]</kbd> ),\n"
            + "  or replace a substring (for example <kbd>TAML1</kbd>) with a list of values (for example,\n"
            + "  <kbd>{TAML1,41009,46088}</kbd> ).\n"
            + "  The <kbd>#1</kbd> within the output fileName causes the current value of the range or list\n"
            + "  to be put into the output fileName.\n"
            + "  For example, \n"
            + "<pre>curl --compressed \"https://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet.png?time,atmp&amp;time%3E=2010-09-03T00:00:00Z&amp;time%3C=2010-09-06T00:00:00Z&amp;station=%22TAML1%22&amp;.draw=linesAndMarkers&amp;.marker=5|5&amp;.color=0x000000&amp;.colorBar=|||||\" -o NDBCatmp#1.png</pre>\n"
            + "  (That example includes <kbd>--compressed</kbd> because it is a generally useful option,\n"
            + "  but there is little benefit to <kbd>--compressed</kbd> when requesting .png files\n"
            + "  because they are already compressed internally.)\n"
            + "<li>To access a password-protected, private ERDDAP dataset with curl via https, use this\n"
            + "  two-step process:\n"
            + "  <br>&nbsp;\n"
            + "  <ol>\n"
            + "  <li>Log in (authenticate) and save the certificate cookie in a cookie-jar file:\n"
            + "<pre>curl -v --data 'user=<i>myUserName</i>&amp;password=<i>myPassword</i>' -c cookies.txt -b cookies.txt -k https://<i>baseurl</i>:8443/erddap/login.html</pre>\n"
            + "  <li>Repeatedly make data requests using the saved cookie: \n"
            + "<pre>curl --compressed -v -c cookies.txt -b cookies.txt -k https://<i>baseurl</i>:8443/erddap/tabledap/<i>datasetID.fileType?query</i> -o <i>outputFileName</i></pre>\n"
            + "  </ol>\n"
            + "  Some ERDDAP installations won't need the port number (:8443) in the URL.\n"
            + "  <br>(Thanks to Liquid Robotics for the starting point and Emilio Mayorga of NANOOS and\n"
            + "  Paul Janecek of Spyglass Technologies for testing.)\n"
            + "  <br>&nbsp;\n"
            + "</ul>\n");

    // query
    writer.write(
        "<li><a class=\"selfLink\" id=\"query\" href=\"#query\" rel=\"bookmark\"><strong>query</strong></a>\n"
            + "  is the part of the request after the \"?\".\n"
            + "  It specifies the subset of data that you want to receive.\n"
            + "  In tabledap, it is an \n"
            + "  <a rel=\"bookmark\" href=\"https://www.opendap.org/\">OPeNDAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> "
            + "  <a rel=\"help\" href=\"https://www.opendap.org/pdf/ESE-RFC-004v1.2.pdf\">DAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "  <a rel=\"help\" href=\"https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Constraint_Expressions\"\n"
            + "      >selection constraint"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> query"
            + "  in the form: <kbd>{<i>resultsVariables</i>}{<i>constraints</i>}</kbd> .\n "
            + "  For example,\n"
            + "  <br><kbd>"
            + XML.encodeAsHTML(
                EDStatic.messages.EDDTableVariablesExample
                    + EDStatic.messages.EDDTableConstraintsExample)
            + "</kbd>\n"
            + "  <ul>\n"
            + "  <li><i><strong>resultsVariables</strong></i> is an optional comma-separated list of variables\n"
            + "        (for example,\n"
            + "    <br><kbd>"
            + XML.encodeAsHTML(EDStatic.messages.EDDTableVariablesExample)
            + "</kbd>).\n"
            + "    <br>For each variable in resultsVariables, there will be a column in the \n"
            + "    results table, in the same order.\n"
            + "    If you don't specify any results variables, the results table will include\n"
            + "    columns for all of the variables in the dataset.\n"
            + "  <li><i><strong>constraints</strong></i> is an optional list of constraints, each preceded by &amp;\n"
            + "    (for example,\n"
            + "    <br><kbd>"
            + XML.encodeAsHTML(EDStatic.messages.EDDTableConstraintsExample)
            + "</kbd>).\n"
            + "    <ul>\n"
            + "    <li>The constraints determine which rows of data from the original table\n"
            + "      table are included in the results table. \n"
            + "      The constraints are applied to each row of the original table.\n"
            + "      If all the constraints evaluate to <kbd>true</kbd> for a given row,\n"
            + "      that row is included in the results table.\n"
            + "      Thus, \"&amp;\" can be roughly interpreted as \"and\".\n"
            + "    <li>If you don't specify any constraints, all rows from the original table\n"
            + "      will be included in the results table.\n"
            + "      For the fileTypes that return information about the dataset (notably,\n"
            + "      .das, .dds, and .html), but don't return actual data, it is fine not\n"
            + "      to specify constraints. For example,\n"
            + "      <br><a href=\""
            + EDStatic.phEncode(fullDdsExample)
            + "\"><kbd>"
            + String2.replaceAll(XML.encodeAsHTML(fullDdsExample), "/", "<wbr>/")
            + "</kbd></a>\n"
            + "      <br>For the fileTypes that do return data, not specifying any constraints\n"
            + "      may result in a very large results table.\n"
            + "    <li>tabledap constraints are consistent with \n"
            + "      <a rel=\"bookmark\" href=\"https://www.opendap.org/\">OPeNDAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> "
            + "      <a rel=\"help\" href=\"https://www.opendap.org/pdf/ESE-RFC-004v1.2.pdf\">DAP"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "      <a rel=\"help\" href=\"https://docs.opendap.org/index.php/UserGuideOPeNDAPMessages#Constraint_Expressions\"\n"
            + "      >selection constraints"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "      but with a few additional features.\n"
            + "    <li>Each constraint is in the form <kbd><i>VariableOperatorValue</i></kbd>\n"
            + "      (for example, <kbd>latitude&gt;45</kbd>).\n"
            + "    <li>The valid operators are =, != (not equals),\n"
            + "       =~ (a <a rel=\"help\" href=\"#regularExpression\">regular expression</a> test),\n"
            + "       &lt;, &lt;=, &gt;, and &gt;= .\n"
            + "    <li>tabledap extends the OPeNDAP standard to allow any operator to be used with any data type.\n"
            + "      (Standard OPeNDAP selections don't allow &lt;, &lt;=, &gt;, or &gt;= \n"
            + "      to be used with string variables\n"
            + "      and don't allow =~ to be used with numeric variables.)\n"
            + "      <ul>\n"
            + "      <li>For String variables, all operators act in a case-sensitive manner.\n"
            + "      <li>For String variables, queries for =\"\" and !=\"\" should work correctly for almost all\n"
            + "        datasets.\n"
            + "    <li><a class=\"selfLink\" id=\"QuoteStrings\" href=\"#QuoteStrings\" rel=\"bookmark\">For</a>\n"
            + "        <a class=\"selfLink\" id=\"backslashEncoded\" href=\"#backslashEncoded\" rel=\"bookmark\">all</a>\n"
            + "        constraints of String variables and for regex constraints of numeric variables,\n"
            + "        the right-hand-side value MUST be enclosed in double quotes (e.g., <kbd>id=\"NDBC41201\"</kbd>)\n"
            + "        and any internal special characters must be backslash encoded: \\ into \\\\, \" into \\\",\n"
            + "        newline into \\n, and tab into \\t.\n"
            + "    <li><a class=\"selfLink\" id=\"PercentEncode\" href=\"#PercentEncode\" rel=\"bookmark\">For</a>\n"
            + "         all constraints, the value part of the <kbd><i>VariableOperatorValue</i></kbd>\n"
            + "        MUST be <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>:\n"
            + "        special characters in the query values (the parts after the '=' signs) are encoded as %HH, where\n"
            + "        HH is the 2 digit hexadecimal value of the character (for example, ' ' is replaced with \"%20\").\n"
            + "        Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be\n"
            + "        percent encoded. Normally, when you use a browser, the browser takes care of this for you.\n"
            + "        But if your computer program or script generates the URLs, it probably needs to do the\n"
            + "        percent encoding itself.  If so, you need to encode all characters other than A-Za-z0-9_-!.~'()*\n"
            + "        in all query values. Programming languages have tools to do this (for example, see Java's\n"
            + "<a rel=\"help\" href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URLEncoder.html\">java.net.URLEncoder"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> and JavaScript's\n"
            + "<a rel=\"help\" href=\"https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent\"\n"
            + "        >encodeURIComponent()"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>) and there are\n"
            + "       <a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "      <li><a class=\"selfLink\" id=\"comparingFloats\" href=\"#comparingFloats\" rel=\"bookmark\">WARNING:</a>\n"
            + "          Numeric queries involving =, !=, &lt;=, or &gt;= may not work as desired with\n"
            + "          floating point numbers. For example, a search for <kbd>longitude=220.2</kbd> may\n"
            + "          fail if the value is stored as 220.20000000000001. This problem arises because\n"
            + "          floating point numbers are "
            + "          <a rel=\"help\" href=\"https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/\"\n"
            + "          >not represented exactly within computers"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "          When ERDDAP performs these tests, it allows for minor variations and tries\n"
            + "          to avoid the problem. But it is possible that some datasets will still\n"
            + "          have problems with these queries and return unexpected and incorrect results.\n"
            + "      <li><a rel=\"help\" href=\"https://en.wikipedia.org/wiki/NaN\">NaN (Not-a-Number)"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> -\n"
            + "          <a class=\"selfLink\" id=\"NaN\" href=\"#NaN\" rel=\"bookmark\">Many</a>\n"
            + "          numeric variables have an attribute which identifies a\n"
            + "          number (e.g., -99) as a missing_value or a _FillValue. When ERDDAP tests\n"
            + "          constraints, it always treats these values as NaN's. So:\n"
            + "          <br>Don't create constraints like <kbd>temperature!=-99</kbd> .\n"
            + "          <br>Do&nbsp;&nbsp;&nbsp;&nbsp; create constraints like <kbd>temperature!=NaN</kbd> .\n"
            + "        <ul>\n"
            + "        <li>For numeric variables, tests of <kbd><i>variable</i>=NaN</kbd> (Not-a-Number) and <kbd><i>variable</i>!=NaN</kbd>\n"
            + "          will usually work as expected.\n"
            + "          <br><strong>WARNING: numeric queries with =NaN and !=NaN may not work as desired</strong>\n"
            + "          since many data sources don't offer native support for these queries and\n"
            + "          ERDDAP can't always work around this problem.\n"
            + "          For some datasets, queries with =NaN or !=NaN may fail with an error message\n"
            + "          (insufficient memory or timeout) or erroneously report\n"
            + "          <kbd>"
            + MustBe.THERE_IS_NO_DATA
            + "</kbd>\n"
            + "        <li>For numeric variables, tests of <kbd><i>variable</i>&lt;NaN</kbd> (Not-a-Number) (or &lt;=, &gt;, &gt;=) will\n"
            + "          return false for any value of the variable, even NaN.  NaN isn't a number so these\n"
            + "          tests are nonsensical.\n"
            + "          Similarly, tests of <kbd><i>variable</i>&lt;<i>aNonNaNValue</i></kbd> (or &lt;=, &gt;, &gt;=) will return false\n"
            + "          whenever the variable's value is NaN.\n"
            + "        </ul>\n"
            + "      <li><a class=\"selfLink\" id=\"regularExpression\" href=\"#regularExpression\" rel=\"bookmark\"\n"
            + "          ><i>variable</i>=~\"<i>regularExpression</i>\"</a>"
            + "          tests if the value from the variable on the left matches the \n"
            + "          <a rel=\"help\" href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html\"\n"
            + "          >regular expression"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "         on the right.\n"
            + "        <ul>\n"
            + "        <li>tabledap uses the same \n"
            + "          <a rel=\"help\" href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html\"\n"
            + "          >regular expression syntax"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "             (<a rel=\"help\" href=\"https://www.vogella.com/tutorials/JavaRegularExpressions/article.html\">tutorial"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>) as is used by Java.\n"
            + "        <li>A common use of regular expressions is to provide the equivalent of an OR constraint,\n"
            + "          which DAP doesn't support.\n"
            + "          For example, if you want to request data from station=\"Able\" OR station=\"Baker\" OR station=\"Charlie\",\n"
            + "          you can use a capture group in a regular expression (which is the =~ operator in DAP constraints).\n"
            + "          A capture group is surrounded by parentheses. \n"
            + "          The options within a capture group are separated by \"|\", which you can read/interpret as \"OR\". \n"
            + "          Since the constraint is a string, it must be surrounded by quotes.\n"
            + "          So the regular expression constraint for those stations is:\n"
            + "          <span class=\"N\"><kbd>&amp;station=~\"(Able|Baker|Charlie)\"</kbd></span>\n"
            + "        <li><strong>WARNING</strong> - For numeric variables, the =~ test is performed on the String representation\n"
            + "          of the variable's source values.  So if the variable's source uses scale_factor\n"
            + "          and/or add_offset metadata (which ERDDAP uses to unpack the data) or if the variable\n"
            + "          is an altitude variable that ERDDAP has converted from a depth value, the =~ test\n"
            + "          will be applied to the raw source values.  This is not a good situation, but there\n"
            + "          is no easy way to deal with it. It is fine to try =~ tests of numeric variables,\n"
            + "          but be very skeptical of the results.\n"
            + "        <li><strong>WARNING: queries with =~ may not work as desired</strong> since many data sources\n"
            + "          don't offer native support for these queries and ERDDAP can't always work\n"
            + "          around this problem.\n"
            + "          ERDDAP sometimes has to get the entire dataset to do the test itself.\n"
            + "          For some datasets, queries with =~ may fail with an error message\n"
            + "          (insufficient memory or timeout) or erroneously report\n"
            + "          <kbd>"
            + MustBe.THERE_IS_NO_DATA
            + "</kbd>\n"
            + "          .\n"
            + "        </ul>\n"
            + "      </ul>\n"
            + "    <li>tabledap extends the OPeNDAP standard to allow constraints (selections) to be applied to any variable\n"
            + "      in the dataset. The constraint variables don't have to be included in the resultsVariables.\n"
            + "      (Standard OPeNDAP implementations usually only allow constraints to be applied\n"
            + "      to certain variables. The limitations vary with different implementations.)\n"
            + "    <li>Although tabledap extends the OPeNDAP selection standard (as noted above),\n"
            + "      these extensions (notably \"=~\" being applicable to numeric variables)\n"
            + "      sometimes aren't practical because tabledap\n"
            + "      may need to download lots of extra data from the source (which takes time)\n"
            + "      in order to test the constraint.\n"
            + "    <li><a class=\"selfLink\" id=\"timeConstraints\" href=\"#timeConstraints\" rel=\"bookmark\">tabledap</a>\n"
            + "      always stores date/time values as double precision floating point numbers\n"
            + "      (seconds since 1970-01-01T00:00:00Z, sometimes with some number of milliseconds).\n"
            + "      Here is an example of a query which includes date/time numbers:\n"
            + "      <br><a href=\""
            + fullValueExampleHA
            + "\"><kbd>"
            + String2.replaceAll(
                String2.replaceAll(fullValueExampleHE, "&", "<wbr>&"), "/", "<wbr>/")
            + "</kbd></a>\n"
            + "      <br>The more human-oriented fileTypes (notably, .csv, .tsv, .htmlTable, .odvTxt, and .xhtml)\n"
            + "      display date/time values as \n"
            + "        <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" date/time strings"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "      (e.g., <kbd>2002-08-03T12:30:00Z</kbd>, but some time variables in some datasets include\n"
            + "      milliseconds, e.g., <kbd>2002-08-03T12:30:00.123Z</kbd>).\n"
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
            + "    <li>tabledap extends the OPeNDAP standard to allow you to specify time values in the\n"
            + "      ISO 8601 date/time format (<kbd><i>YYYY-MM-DD</i>T<i>hh:mm:ss.SSSZ</i></kbd>, where Z is 'Z' or a &plusmn;hh\n"
            + "      or &plusmn;hh:mm offset from the Zulu/GMT time zone. If you omit Z and the offset, the\n"
            + "      Zulu/GMT time zone is used. Separately, if you omit .SSS, :ss.SSS, :mm:ss.SSS, or\n"
            + "      Thh:mm:ss.SSS from the ISO date/time that you specify, the missing fields are assumed\n"
            + "      to be 0. In some places, ERDDAP accepts a comma (ss,SSS) as the seconds decimal point,\n"
            + "      but ERDDAP always uses a period when formatting times as ISO 8601 strings.\n"
            + "      Here is an example of a query which includes ISO date/time values:\n"
            + "      <br><a href=\""
            + fullTimeExampleHA
            + "\"><kbd>"
            + String2.replaceAll(
                String2.replaceAll(fullTimeExampleHE, "&", "<wbr>&"), "/", "<wbr>/")
            + "</kbd></a>\n"
            + "      <br><a class=\"selfLink\" id=\"lenient\" href=\"#lenient\" rel=\"bookmark\">ERDDAP</a>\n"
            + "      is \"lenient\" when it parses date/time strings. That means that date/times\n"
            + "      with the correct format, but with month, date, hour, minute, and/or second values\n"
            + "      that are too large or too small will be rolled to the appropriate date/times.\n"
            + "      For example, ERDDAP interprets 2001-12-32 as 2002-01-01, and interprets\n"
            + "      2002-01-00 as 2001-12-31.\n"
            + "      (It's not a bug, it's a feature! We understand that you may object to this\n"
            + "      if you are not familiar with lenient parsing. We understand there are\n"
            + "      circumstances where some people would prefer strict parsing, but there are also\n"
            + "      circumstances where some people would prefer lenient parsing. ERDDAP can't\n"
            + "      have it both ways. This was a conscious choice. Lenient parsing is the default\n"
            + "      behavior in Java, the language that ERDDAP is written in and arguably the\n"
            + "      most-used computer language. Also, this behavior is consistent with ERDDAP's\n"
            + "      conversion of requested grid axis values to the nearest valid grid axis value.\n"
            + "      And this is consistent with some other places in ERDDAP that try to repair\n"
            + "      invalid input when the intention is clear, instead of just returning an error\n"
            + "      message.)\n"
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
            + "    <li><a class=\"selfLink\" id=\"now\" href=\"#now\" rel=\"bookmark\">tabledap</a>\n"
            + "      extends the OPeNDAP standard to allow you to specify constraints for\n"
            + "      time and timestamp variables relative to <kbd>now</kbd>. The constraint can be simply,\n"
            + "      for example, <kbd>time&lt;now</kbd>, but usually the constraint is in the form\n"
            + "      <br><kbd>now(+| |-)<i>positiveInteger</i>(second|seconds|minute|minutes|\n"
            + "      hour|hours|day|days|month|months|year|years)</kbd>\n"
            + "      <br>for example, <kbd>now-7days</kbd>.\n"
            + "      <br>Months and years are interpreted as calendar months and years (not\n"
            + "      UDUNITS-like constant values).\n"
            + "      This feature is especially useful when creating a URL for an &lt;img&gt; (image)\n"
            + "      tag on a web page, since it allows you to specify that the image will always\n"
            + "      show, for example, the last 7 days worth of data (<kbd>&amp;time&gt;now-7days</kbd>).\n"
            + "      Note that a '+' in the URL is percent-decoded as ' ', so you should\n"
            + "      percent-encode '+' as %2B.  However, ERDDAP interprets <kbd>\"now \"</kbd> as <kbd>\"now+\"</kbd>,\n"
            + "      so in practice you don't have to percent-encode it.\n"
            + "    <li><a class=\"selfLink\" id=\"min\" href=\"#min\" rel=\"bookmark\">tabledap</a>\n"
            + "      <a class=\"selfLink\" id=\"max\" href=\"#max\" rel=\"bookmark\"\n"
            + "      >extends</a> the OPeNDAP standard to allow you to refer to <kbd>min(<i>variableName</i>)</kbd> or\n"
            + "      <kbd>max(<i>variableName</i>)</kbd> in the right hand part of a constraint.\n"
            + "      <ul>\n"
            + "      <li>If <i>variableName</i> is a timestamp variable, the constraint must be in the form:\n"
            + "        <br><kbd>min|max(<i>variableName</i>)[+|-<i>positiveInteger</i>[millis|seconds<wbr>|minutes|hours|days<wbr>|months|years]]</kbd>\n"
            + "        <br>(or singular units). If time units aren't supplied, \"seconds\" are assumed.\n"
            + "        For example, <kbd>&amp;time&gt;max(time)-10minutes</kbd>\n"
            + "      <li>If <i>variableName</i> is a non-timestamp variable, the constraint must be in the form:\n"
            + "        <br><kbd>min|max(<i>variableName</i>)[+|-<i>positiveNumber</i>]</kbd>\n"
            + "        <br>For example, <kbd>&amp;pressure&lt;min(pressure)+10.5</kbd>\n"
            + "      <li>Usually, <i>variableName</i> will be the same as the variable you are constraining,\n"
            + "        but it doesn't have to be.\n"
            + "      <li>If <i>variableName</i>'s minimum (or maximum) isn't known, min() (or max()) will throw an error.\n"
            + "      <li>min() and max() will almost always be used to constrain numeric (including timestamp)\n"
            + "        variables, but it is technically possible to constrain string variables.\n"
            + "      <li>min() and max() can't be used with regex constraints (=~).\n"
            + "        <br>&nbsp;\n"
            + "      </ul>\n"
            +

            // polygon requests
            "   <li><a class=\"selfLink\" id=\"polygonRequests\" href=\"#polygonRequests\" rel=\"bookmark\"\n"
            + "     >Can users extract data from a polygon-defined geographic area?</a>\n"
            + "     <br>No. ERDDAP does not support that.\n"
            + "     You need to calculate the lat lon bounding box for the polygon\n"
            + "     (it is easy to calculate: the lon range will be minLon to maxLon, the lat range will be minLat to maxLat)\n"
            + "     and request that data from ERDDAP using the constraint system described above.\n"
            + "     You can then do the polygon extraction (or calculations) in your favorite analysis software (ArcGIS, Matlab, Python, R, ...).\n"
            + "     An advantage of this approach is that there is no limit to the number of points that define the polygon.\n"
            + "     Note that this is the way the OPeNDAP standard (which ERDDAP uses for these requests) works:\n"
            + "     users request a subset of the original data based on simple constraints.\n"
            + "     <br>&nbsp;\n"
            + "    </ul>\n"
            + "  <li><a class=\"selfLink\" id=\"functions\" href=\"#functions\" rel=\"bookmark\"\n"
            + "      ><strong>Server-side Functions</strong></a> - The OPeNDAP standard supports the idea of\n"
            + "    server-side functions, but doesn't define any.\n"
            + "    TableDAP supports some server-side functions that modify the results table before\n"
            + "    the results table is sent to you.\n"
            + "    Server-side functions are OPTIONAL.  Other than 'distinct', they are rarely needed.\n"
            + "    Currently, each of the functions takes the results table as input and\n"
            + "    returns a table with the same columns in the same order (but the rows may\n"
            + "    be altered). If you use more than one function, they will be applied in the order\n"
            + "    that they appear in the request. Most of these options appear near the bottom\n"
            + "    of the Data Access Form and Make A Graph web page for each dataset.\n"
            + "    <br>&nbsp;\n"
            + "    <ul>\n"
            + "    <li><a class=\"selfLink\" id=\"addVariablesWhere\" href=\"#addVariablesWhere\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;addVariablesWhere(\"<i>attributeName</i>\",\"<i>attributeValue</i>\")</kbd></a>\n"
            + "      <br>is different than the other server-side functions\n"
            + "      in that it doesn't filter the results table. Instead, if you add it to\n"
            + "      a query, ERDDAP will add all of the variables in the dataset which have\n"
            + "      <kbd><i>attributeName=attributeValue</i></kbd>\n"
            + "      to the list of requested variables. For example, if you add\n"
            + "      <kbd>&amp;addVariablesWhere(\"ioos_category\",\"Wind\")</kbd> to a query, ERDDAP\n"
            + "      will add all of the variables in the dataset that have an <kbd>ioos_category=Wind</kbd> attribute\n"
            + "      to the list of requested variables (for example, windSpeed, windDirection, windGustSpeed).\n"
            + "      <i>attributeName</i> and <i>attributeValue</i> are case-sensitive.\n"
            + "      You can specify 0 or more <kbd>&amp;addVariablesWhere()</kbd> functions to the request.\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"distinct\" href=\"#distinct\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;distinct()</kbd></a>\n"
            + "      <br>If you add <kbd>&amp;distinct()</kbd> to the end of a query, ERDDAP will sort all of the\n"
            + "      rows in the results table (starting with the first requested variable, then using the\n"
            + "      second requested variable if the first variable has a tie, ...), then remove all\n"
            + "      non-unique rows of data.\n"
            + "      For example, the query <kbd>stationType,stationID&amp;distinct()</kbd> will return a\n"
            + "      sorted list of stationIDs associated with each stationType.\n"
            + "      In many situations, ERDDAP can return distinct() values quickly and efficiently.\n"
            + "      But in some cases, ERDDAP must look through all rows of the source dataset. \n"
            + "      If the data set isn't local, this may be VERY slow, may return only some of the\n"
            + "      results (without an error), or may throw an error.\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderBy\" href=\"#orderBy\" rel=\"bookmark\" "
            + "><kbd>&amp;orderBy(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 1 or more variable names\n"
            + "      lets you specify how the results table will be sorted\n"
            + "      (starting with the first variable in the CSV list, then using the second variable if the first\n"
            + "      variable has a tie, ...).\n"
            + "      All of the orderBy variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderBy CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>Without orderBy, the rows of data in the response table are in the order they arrived from\n"
            + "      the data source, which may or may not be a nice logical order.\n"
            + "      Thus, orderBy allows you to request that the results table be sorted in a specific way.\n"
            + "      For example, use the query\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderBy(\"stationID,time\")</pre>\n"
            + "      to get the results sorted by stationID, then time.\n"
            + "      Or use the query\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderBy(\"time,stationID\")</pre>\n"
            + "      to get the results sorted by time first, then stationID.\n"
            + "      <p><kbd>orderBy()</kbd> doesn't support\n"
            + "      the divisor options for numeric variables, i.e.,\n"
            + "      <br><kbd>numericVariable[/number[timeUnits][:offset]]</kbd>\n"
            + "      <br>because they would be nonsensical.\n"
            + "    <li><a class=\"selfLink\" id=\"orderByDescending\" href=\"#orderByDescending\" rel=\"bookmark\" "
            + "><kbd>&amp;orderByDescending(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>orderByDescending is just like orderBy, except the rows are sorted in descending, instead of ascending, order.\n"
            + "    <li><a class=\"selfLink\" id=\"orderByClosest\" href=\"#orderByClosest\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByClosest(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 1 or more variable names\n"
            + "      lets you specify how the results table will be sorted.\n"
            + "      For orderByClosest, the last item in the CSV list must be a numeric variable with a divisor.\n"
            + "      (See <a rel=\"help\" href=\"#orderByDivisorOptions\">divisor details</a>.)\n"
            + "      Other variables in the CSV list may not have divisors.\n"
            + "      <br>All of the orderByClosest variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderByClosest CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>For orderByClosest, for each group, ERDDAP will return just the row where the\n"
            + "      value of the last CSV variable is closest to the divisor (interval).  For example,\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderByClosest(\"stationID,time/2hours\")</pre>\n"
            + "      will sort by stationID and time, but only return the rows for each stationID where\n"
            + "      the last orderBy column (time) are closest to 2hour intervals (12am, 2am, 4am, ...).\n"
            + "      For numeric variables in orderByMax CSV list, for each group,\n"
            + "      ERDDAP will return the exact value (e.g., the exact time) at\n"
            + "      which the closest value occurred.\n"
            + "      This is the closest thing in tabledap to stride values in a griddap request.\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderByCount\" href=\"#orderByCount\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByCount(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 0 or more variable names\n"
            + "      lets you specify how the results table will be sorted and grouped.\n"
            + "      Numeric variables in the CSV list usually have an optional\n"
            + "      divisor to identify groupings, e.g., time/1day .\n"
            + "      (See <a rel=\"help\" href=\"#orderByDivisorOptions\">divisor&nbsp;details</a>.)\n"
            + "      <br>All of the orderByCount variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderByCount CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>For orderByCount, for each group, ERDDAP will return just one row\n"
            + "      with the count of the number of non-NaN values for each variable not in the CSV list.\n"
            + "      For example, use the query\n"
            + "      <pre>?stationID,time,temperature,windspeed&amp;time&gt;"
            + daysAgo7
            + "&amp;orderByCount(\"stationID,time/1day\")</pre>\n"
            + "      to get a count of the number of non-NaN temperature and windspeed values for each stationID, for each day\n"
            + "      (for stations with data from after "
            + daysAgo7
            + ").\n"
            + "\n"
            + "      <p><a class=\"selfLink\" id=\"orderByDivisorOptions\" href=\"#orderByDivisorOptions\" rel=\"bookmark\"\n"
            + "      >Divisors</a> - All orderBy options\n"
            + "      (other than the plain <kbd>orderBy()</kbd> and <kbd>orderByClosest()</kbd>) support\n"
            + "      divisor options for any of the numeric variables in the orderBy... CSV list, in the form\n"
            + "      <br><kbd>numericVariable[/number[timeUnits][:offset]]</kbd>\n"
            + "      <br>Some examples are: time/10, time/2days, depth/10, depth/0.5, depth/10:5.\n"
            + "      <ul>\n"
            + "      <li>The divisor number must be a positive value.\n"
            + "        If timeUnits aren't month or year, the number may be a floating point number.\n"
            + "        The number is interpreted to have the same units as the variable;\n"
            + "        for timestamp variables, unless modified by timeUnits, the number is interpreted as units=seconds.\n"
            + "      <li>The optional timeUnits is only allowed if the numericVariable is a timestamp variable.\n"
            + "        All common English time units are supported (singular or plural) and with many abbreviations:\n"
            + "        <br>ms, msec, msecs, millis, millisec, millisecs, millisecond, milliseconds,\n"
            + "        <br>s, sec, secs, second, seconds, m, min, mins, minute, minutes, h, hr, hrs, hour, hours,\n"
            + "        <br>d, day, days, week, weeks, mon, mons, month, months, yr, yrs, year, or years.\n"
            + "      <li>The optional :offset is only allowed if timeUnits isn't specified.\n"
            + "      <li>If numericVariable is a timestamp variable and the timeUnits are months or years,\n"
            + "        the number must be a positive integer and ERDDAP will increment by calendar\n"
            + "        months or years (not a fixed length of time).\n"
            + "        If timeUnits is month, number must be 1, 2, 3, 4, or 6.\n"
            + "        <br>&nbsp;\n"
            + "      </ul>\n"
            + "      \n"
            + "    <li><a class=\"selfLink\" id=\"orderByLimit\" href=\"#orderByLimit\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByLimit(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 0 or more variable names plus a limit number\n"
            + "      lets you specify how the results table will be sorted and grouped.\n"
            + "      Numeric variables in the CSV list usually have an optional\n"
            + "      divisor to identify groupings, e.g., time/1day .\n"
            + "      (See <a rel=\"help\" href=\"#orderByDivisorOptions\">divisor&nbsp;details</a>.)\n"
            + "      <br>All of the orderByLimit variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderByLimit CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>For orderByLimit, the last value in the CSV list must be the limit number (e.g., 10).\n"
            + "      Within each sort group, only the first 'limit' rows will\n"
            + "      be kept.\n"
            + "      For example,\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderByLimit(\"stationID,time/1day,10\")</pre>\n"
            + "      will sort by stationID and time, but only return the first 10 rows for each stationID per day.\n"
            + "      This will usually return the same rows as the first n rows per group of a similar\n"
            + "      request with no orderByLimit, but not always.\n"
            + "      This is similar to SQL's LIMIT clause.\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderByMax\" href=\"#orderByMax\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByMax(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 1 or more variable names\n"
            + "      lets you specify how the results table will be sorted and grouped.\n"
            + "      Numeric variables in the CSV list usually have an optional\n"
            + "      divisor to identify groupings, e.g., time/1day .\n"
            + "      (See <a rel=\"help\" href=\"#orderByDivisorOptions\">divisor&nbsp;details</a>.)\n"
            + "      <br>For orderByMax, the last variable name in the CSV list may\n"
            + "      be a string or a numeric variable (if numeric, it must not include a divisor).\n"
            + "      <br>All of the orderByMax variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderByMax CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>orderByMax will sort results into groups based on values of the variables\n"
            + "      in the CSV list except the last variable, then just keep the\n"
            + "      row within each group where the last CSV list variable has the highest value.\n"
            + "      (If there are two or more rows which have the same highest value, ERDDAP may return any of them.)\n"
            + "      For example, use the query\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderByMax(\"stationID,time/1day,temperature\")</pre>\n"
            + "      to get just the rows of data with each station's maximum\n"
            + "      temperature value for each day (for stations\n"
            + "      with data from after "
            + daysAgo7
            + ").\n"
            + "      For numeric variables in orderByMax CSV list, for each group,\n"
            + "      ERDDAP will return the exact value (e.g., the exact time) at\n"
            + "      which the max value (e.g., temperature) occurred.\n"
            + "      This is the closest thing in tabledap to griddap's allowing requests for the <kbd>[last]</kbd>\n"
            + "      axis value.\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderByMin\" href=\"#orderByMin\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByMin(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>orderByMin is exactly like <a href=\"#orderByMax\">orderByMax</a>, except that it\n"
            + "      returns the row within each group which has the minimum value of the last variable in the CSV list.\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderByMinMax\" href=\"#orderByMinMax\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByMinMax(\"<i>comma-separated list of 1 or more variable names</i>\")</kbd></a>\n"
            + "      <br>orderByMinMax is like <a href=\"#orderByMax\">orderByMax</a>, except that it returns\n"
            + "      the two rows within each group: the row which has the minimum value of the last variable in the CSV list\n"
            + "      and the row which has the maximum value of the last variable in the CSV list.\n"
            + "      For example, use the query\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderByMinMax(\"stationID,time/1day,temperature\")</pre>\n"
            + "      to get just the rows of data with each station's minimum temperature value and each station's\n"
            + "      maximum time value for each day (for stations with data from after "
            + daysAgo7
            + ").\n"
            + "      If there is only one row of data for a given combination (e.g., stationID), there will\n"
            + "      still be two rows in the output (with identical data).\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderByMean\" href=\"#orderByMean\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderByMean(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 0 or more variable names\n"
            + "      lets you specify how the results table will be sorted and grouped.\n"
            + "      Numeric variables in the CSV list usually have an optional\n"
            + "      divisor to identify groupings, e.g., time/1day .\n"
            + "      (See <a rel=\"help\" href=\"#orderByDivisorOptions\">divisor&nbsp;details</a>.)\n"
            + "      <br>All of the orderByMean variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderByMean CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>For orderByMean, for each group, ERDDAP will return the mean\n"
            + "      of each of the variables not in the CSV list.\n"
            + "      You can use the same <a href=\"#orderByDivisorOptions\" rel=\"bookmark\" \n"
            + "        >divisor options</a> as other orderBy options (e.g., time/1day or depth/10).\n"
            + "      For example,\n"
            + "      <pre>?stationID,time,temperature&amp;time&gt;"
            + daysAgo7
            + "&amp;orderByMean(\"stationID,time/1day\")</pre>\n"
            + "      will sort by stationID and time, but only return the mean temperature value for each stationID for each day.\n"
            + "      <ul>\n"
            + "      <li>The mean values are returned as doubles, with _FillValue=NaN used to represent empty cells\n"
            + "        (groups with no finite values for a given variable).\n"
            + "      <li>If a column has degree-related units, the mean is calculated differently:\n"
            + "        by converting the angle to x and y components, calculating the means of the x and y components,\n"
            + "        and then converting those back to the mean degrees value.\n"
            + "        If the column's units are degree_true (or a variant), the returned mean will be in the range 0 to 360.\n"
            + "        For other degree units (e.g., degree_east, degree_north), the returned mean will be in the range -180 to 180.\n"
            + "      <li>Any requested string or character variables not in the orderByMean list are discarded\n"
            + "        (unless the value does not vary) since asking for the mean of a string variable is meaningless.\n"
            + "      </ul>\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"orderBySum\" href=\"#orderBySum\" rel=\"bookmark\"\n"
            + "      ><kbd>&amp;orderBySum(\"<i>comma-separated list of variable names</i>\")</kbd></a>\n"
            + "      <br>The comma-separated (CSV) list of 0 or more variable names\n"
            + "      lets you specify how the results table will be sorted and grouped.\n"
            + "      Numeric variables in the CSV list usually have an optional\n"
            + "      divisor to identify groupings, e.g., time/1day .\n"
            + "      (See <a rel=\"help\" href=\"#orderByDivisorOptions\">divisor&nbsp;details</a>.)\n"
            + "      <br>All of the orderBySum variables MUST be included in the list of requested variables in\n"
            + "      the query as well as in the orderBySum CSV list of variables (the .html and .graph\n"
            + "      forms will do this for you).\n"
            + "      <p>For orderBySum, for each group, ERDDAP will return the sum\n"
            + "      of each of the variables not in the CSV list.\n"
            + "      You can use the same <a href=\"#orderByDivisorOptions\" rel=\"bookmark\" \n"
            + "        >divisor options</a> as other orderBy options (e.g., time/1day or depth/10).\n"
            + "      For example,\n"
            + "      <pre>?stationID,time,rainfall&amp;time&gt;"
            + daysAgo7
            + "&amp;orderBySum(\"stationID,time/1day\")</pre>\n"
            + "      will sort by stationID and time, but only return the sum of rainfall values for each stationID for each day.\n"
            + "      <ul>\n"
            + "      <li>The sum values are returned as doubles, with _FillValue=NaN used to represent empty cells\n"
            + "        (groups with no finite values for a given variable).\n"
            + "      <li>This will calculate the sums for all numeric variables. For many types of data\n"
            + "        (e.g., time variables or variables with degree-related units), the calculated sums won't make any scientific sense.\n"
            + "        It is up to you to determine the appropriateness of using the calculated values.\n"
            + "      <li>Any requested string or character variables not in the orderBySum list are discarded\n"
            + "        (unless the value does not vary) since asking for the sum of a string variable is meaningless.\n"
            + "      </ul>\n"
            + "      <br>&nbsp;\n"
            + "    <li><a class=\"selfLink\" id=\"units\" href=\"#units\" rel=\"bookmark\"><kbd>&amp;units(\"<i>value</i>\")</kbd></a>\n"
            + "      <br>If you add <kbd>&amp;units(\"UDUNITS\")</kbd> to the end of a query, the units will be described\n"
            + "      via the\n"
            + "        <a rel=\"help\" href=\"https://www.unidata.ucar.edu/software/udunits/\">UDUNITS"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> standard (for example, <kbd>degree_C</kbd>).\n"
            + "      <br>If you add <kbd>&amp;units(\"UCUM\")</kbd> to the end of a query, the units will be described\n"
            + "      via the\n"
            + "        <a rel=\"help\" href=\"https://unitsofmeasure.org/ucum.html\">UCUM"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a> standard (for example, <kbd>Cel</kbd>).\n"
            + "      <br>On this ERDDAP, the default for most/all datasets is "
            + EDStatic.config.units_standard
            + ".\n"
            + (EDStatic.config.convertersActive
                ? "      <br>See also ERDDAP's <a rel=\"bookmark\" href=\""
                    + tErddapUrl
                    + "/convert/units.html\"\n"
                    + "      >units converter</a>.\n"
                : "")
            + "      <br>&nbsp;\n"
            + "    </ul>\n"
            +

            // Graphics Commands
            "  <li><a class=\"selfLink\" id=\"GraphicsCommands\" href=\"#GraphicsCommands\" rel=\"bookmark\"\n"
            + "    ><strong>Graphics Commands</strong></a> -\n"
            + "    <a class=\"selfLink\" id=\"MakeAGraph\" href=\"#MakeAGraph\" rel=\"bookmark\"\n"
            + "    >tabledap</a> extends the OPeNDAP standard by allowing graphics commands\n"
            + "    in the query. \n"
            + "    The Make A Graph web pages simplify the creation of URLs with these graphics commands\n"
            + "    (see the \"graph\" links in the table of <a href=\""
            + dapBase
            + "index.html\">tabledap datasets</a>). \n"
            + "    So we recommend using the Make A Graph web pages to generate URLs, and then, when\n"
            + "    needed, using the information here to modify the URLs for special purposes.\n"
            + "    These commands are optional.\n"
            + "    If present, they must occur after the data request part of the query. \n"
            + "    These commands are used by tabledap if you request an\n"
            + "    <a rel=\"bookmark\" href=\"#imageFileTypes\">image fileType</a> (.png or .pdf)\n"
            + "    and are ignored if you request a data file (e.g., .asc). \n"
            + "    If relevant commands are not included in your request, tabledap uses the defaults and\n"
            + "    tries its best to generate a reasonable graph or map. \n"
            + "    All of the commands are in the form &amp;.<i>commandName</i>=<i>value</i> . \n"
            + "    If the value has sub-values, they are separated by the '|' character. \n"
            + "    The commands are:\n"
            + "    <ul>\n"
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
            + "    <li><kbd>&amp;.colorBar=<i>palette</i>|<i>continuous</i>|<i>scale</i>|<i>min</i>|<i>max</i>|<i>nSections</i></kbd> \n"
            + "      <br>This specifies the settings for a color bar.  The sub-values are:\n"
            + "      <ul>\n"
            +
            // the standard palettes are listed in 'palettes' in Bob's messages.xml,
            //        EDDTable.java, EDDGrid.java, and setupDatasetsXml.html
            "      <li><i>palette</i> - All ERDDAP installations support a standard set of palettes:\n"
            + "        BlackBlueWhite, BlackRedWhite, BlackWhite, BlueWhiteRed, LightRainbow,\n"
            + "        Ocean, OceanDepth, Rainbow, RedWhiteBlue, ReverseRainbow, Topography,\n"
            + "        TopographyDepth, WhiteBlack, WhiteBlueBlack, WhiteRedBlack.\n"
            + "        Some ERDDAP installations support additional options.\n"
            + "        See a Make A Graph web page for a complete list.\n"
            + "        The default (no value) varies based on min and max: if -1*min ~= max,\n"
            + "        the default is BlueWhiteRed; otherwise, the default is Rainbow.\n"
            + "      <li><i>continuous</i> - must be either no value (the default), 'C' (for Continuous),\n"
            + "        or 'D' (for Discrete). The default is different for different datasets.\n"
            + "      <li><i>scale</i> - must be either no value (the default), <kbd>Linear</kbd>, or <kbd>Log</kbd>.\n"
            + "        The default is different for different datasets.\n"
            + "      <li><i>min</i> - The minimum value for the color bar.\n"
            + "        The default is different for different datasets.\n"
            + "      <li><i>max</i> - The maximum value for the color bar.\n"
            + "        The default is different for different datasets.\n"
            + "      <li><i>nSections</i> - The preferred number of sections (for Log color bars,\n"
            + "        this is a minimum value). The default is different for different datasets.\n"
            + "      </ul>\n"
            + "      If you don't specify one of the sub-values, the default for the sub-value will be used.\n"
            + "    <li><kbd>&amp;.color=<i>value</i></kbd>\n"
            + "        <br>This specifies the color for data lines, markers, vectors, etc.  The value must\n"
            + "        be specified as an 0xRRGGBB value (e.g., 0xFF0000 is red, 0x00FF00 is green).\n"
            + "        The color value string is not case sensitive. The default is 0x000000 (black).\n"
            + "    <li><kbd>&amp;.draw=<i>value</i></kbd> \n"
            + "        <br>This specifies how the data will be drawn, as <kbd>lines</kbd>,\n"
            + "        <kbd>linesAndMarkers</kbd>, <kbd>markers</kbd> (default),\n"
            + "        <kbd>sticks</kbd>, or <kbd>vectors</kbd>. \n"
            + "    <li><kbd>&amp;.font=<i>scaleFactor</i></kbd>\n"
            + "        <br>This specifies a scale factor for the font\n"
            + "        (e.g., 1.5 would make the font 1.5 times as big as normal).\n"
            + "    <li><kbd>&amp;.land=<i>value</i></kbd>\n"
            + "      <br>This is only relevant if the first two results variables are longitude and latitude,\n"
            + "      so that the graph is a map.\n"
            + "      The default is different for different datasets (under the ERDDAP administrator's\n"
            + "      control via a drawLandMask in datasets.xml, or via the fallback <kbd>drawLandMask</kbd>\n"
            + "      setting in setup.xml).\n"
            + "      <br><kbd>over</kbd> makes the land mask on a map visible (land appears as a uniform gray area).\n"
            + "      <br><kbd>under</kbd> makes the land mask invisible (topography information is displayed\n"
            + "      for ocean and land areas).\n"
            + "      <br><kbd>outline</kbd> just draws the landmask outline, political boundaries, lakes, and rivers.\n"
            + "      <br><kbd>off</kbd> doesn't draw anything.\n"
            + "    <li><kbd>&amp;.legend=<i>value</i></kbd>\n"
            + "      <br>This specifies whether the legend on .png images (not .pdf's) should be at the\n"
            + "      <kbd>Bottom</kbd> (default), <kbd>Off</kbd>, or <kbd>Only</kbd> (which returns only the legend).\n"
            + "    <li><kbd>&amp;.marker=<i>markerType</i>|<i>markerSize</i></kbd>\n"
            + "      <br>markerType is an integer: 0=None, 1=Plus, 2=X, 3=Dot, 4=Square,\n"
            + "      5=Filled Square (default), 6=Circle, 7=Filled Circle, 8=Up Triangle,\n"
            + "      9=Filled Up Triangle, 10=Borderless Filled Square, 11=Borderless Filled Circle,\n"
            + "      12=Borderless Filled Up Triangle.\n"
            + "      markerSize is an integer from 3 to 50 (default=5)\n"
            + "    <li><kbd>&amp;.size=<i>width</i>|<i>height</i></kbd>\n"
            + "      <br>For .png images (not .pdf's), this specifies the desired size of the image, in pixels.\n"
            + "      This allows you to specify sizes other than the predefined sizes of .smallPng, .png, and\n"
            + "      .largePng.\n"
            + "    <li><kbd>&amp;.trim=<i>trimPixels</i></kbd>\n"
            + "      <br>For .png images (not .pdf's), this tells ERDDAP to make the image shorter by removing\n"
            + "      all whitespace at the bottom, except for <i>trimPixels</i>.\n"
            + "    <li>There is no <kbd>&amp;.vars=</kbd> command. Instead, the results variables from the main part\n"
            + "      of the query are used.\n"
            + "      The meaning associated with each position varies with the <kbd>&amp;.draw</kbd> value:\n"
            + "      <ul>\n"
            + "      <li>for lines: xAxis,yAxis\n"
            + "      <li>for linesAndMarkers: xAxis,yAxis,Color\n"
            + "      <li>for markers: xAxis,yAxis,Color\n"
            + "      <li>for sticks: xAxis,uComponent,vComponent\n"
            + "      <li>for vectors: xAxis,yAxis,uComponent,vComponent\n"
            + "      </ul>\n"
            + "      If xAxis=longitude and yAxis=latitude, you get a map; otherwise, you get a graph.\n "
            + "    <li><kbd>&amp;.vec=<i>value</i></kbd>\n"
            + "      <br>This specifies the data vector length (in data units) to be scaled to the size\n"
            + "      of the sample vector in the legend. The default varies based on the data.\n"
            + "    <li><a class=\"selfLink\" id=\"xRange\" href=\"#xRange\" rel=\"bookmark\" \n"
            + "       ><kbd>&amp;.xRange=<i>min</i>|<i>max</i>|<i>ascending</i>|<i>scale</i></kbd></a>\n"
            + "    <br><kbd>&amp;.yRange=<i>min</i>|<i>max</i>|<i>ascending</i>|<i>scale</i></kbd>\n"
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
            + "    </ul>\n"
            + "    <br><a class=\"selfLink\" id=\"sampleGraphURL\" href=\"#sampleGraphURL\" rel=\"bookmark\"\n"
            + "    >A sample URL to view a <strong>.png</strong> of a <strong>graph</strong> is</a> \n"
            + "    <br><a href=\""
            + fullGraphExampleHA
            + "\">"
            + fullGraphExampleHE
            + "</a>\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to <strong>.graph</strong>,\n"
            + "    you can see a Make A Graph web page with that request loaded:\n"
            + "    <br><a href=\""
            + fullGraphMAGExampleHA
            + "\">"
            + fullGraphMAGExampleHE
            + "</a>\n"
            + "    <br>That makes it easy for humans to modify an image request to make a\n"
            + "    similar graph or map.\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to a data fileType\n"
            + "    (e.g., <strong>.htmlTable</strong>), you can view or download the data that was graphed:\n"
            + "    <br><a href=\""
            + fullGraphDataExampleHA
            + "\">"
            + fullGraphDataExampleHE
            + "</a>\n"
            + "\n"
            + "    <p>A sample URL to view a <strong>.png</strong> of a <strong>map</strong> is \n"
            + "    <br><a href=\""
            + fullMapExampleHA
            + "\">"
            + fullMapExampleHE
            + "</a>\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to <strong>.graph</strong>,\n"
            + "    you can see a Make A Graph web page with that request loaded:\n"
            + "    <br><a href=\""
            + fullMapMAGExampleHA
            + "\">"
            + fullMapMAGExampleHE
            + "</a>\n"
            + "\n"
            + "    <p>Or, if you change the fileType in the URL from .png to a data fileType\n"
            + "    (e.g., <strong>.htmlTable</strong>), you can view or download the data that was mapped:\n"
            + "    <br><a href=\""
            + fullMapDataExampleHA
            + "\">"
            + fullMapDataExampleHE
            + "</a>\n"
            + "</ul>\n"
            +

            // other info
            "<a class=\"selfLink\" id=\"otherInformation\" href=\"#otherInformation\" rel=\"bookmark\"\n"
            + "      ><strong>Other Information</strong></a>\n"
            + "<ul>\n"
            + "<li><a class=\"selfLink\" id=\"login\" href=\"#login\" rel=\"bookmark\"\n"
            + "      ><strong>Log in to access private datasets.</strong></a>\n"
            + "<br>Many ERDDAP installations don't have authentication enabled and thus\n"
            + "don't provide any way for users to login, nor do they have any private datasets.\n"
            + "<p>Some ERDDAP installations do have authentication enabled.\n"
            + "Currently, ERDDAP only supports authentication via Google-managed email accounts,\n"
            + "which includes email accounts at NOAA and many universities.\n"
            + "If an ERDDAP has authentication enabled, anyone with a Google-managed email account\n"
            + "can log in, but they will only have access to the private datasets\n"
            + "that the ERDDAP administrator has explicitly authorized them to access.\n"
            + "For instructions on logging into ERDDAP from a browser or via a script, see\n"
            + "<a rel=\"help\" href=\"https://erddap.github.io/docs/user/AccessToPrivateDatasets\"\n"
            + "      >Access to Private Datasets in ERDDAP</a>.\n"
            + "\n"
            + "<li><a class=\"selfLink\" id=\"dataModel\" href=\"#dataModel\" rel=\"bookmark\"><strong>Data Model</strong></a>\n"
            + "<br>Each tabledap dataset can be represented as a table with one\n"
            + "or more rows and one or more columns of data.\n"
            + "  <ul>\n"
            + "  <li>Each column is also known as a \"data variable\" (or just a \"variable\").\n"
            + "    Each data variable has data of one\n"
            + "    <a rel=\"help\" href=\"https://erddap.github.io/docs/server-admin/datasets#data-types\">data type</a>.\n"
            + "    Each data variable has a name composed of a letter (A-Z, a-z) and then 0 or more\n"
            + "    characters (A-Z, a-z, 0-9, _).\n"
            + "    Each data variable has metadata which is a set of Key=Value pairs.\n"
            + "    The maximum number of columns is 2147483647, but datasets with greater than\n"
            + "    about 100 columns will be awkward for users.\n"
            + "  <li>The maximum number of rows for a dataset is probably about 9e18.\n"
            + "    In most cases, individual sources within a dataset (e.g., files) are limited to\n"
            + "    2147483647 rows.\n"
            + "  <li>Any cell in the table may have no value (i.e., a missing value).\n"
            + "  <li>Each dataset has global metadata which is a set of Key=Value pairs.\n"
            + "  <li>Note about metadata: each variable's metadata and the global metadata is a set of 0\n"
            + "    or more Key=Value pairs.\n"
            + "    <ul>\n"
            + "    <li>Each Key is a String consisting of a letter (A-Z, a-z) and then 0 or more other\n"
            + "      characters (A-Z, a-z, 0-9, '_').\n"
            + "    <li>Each Value is an array of one (usually) or more values of one\n"
            + "      <a rel=\"help\" href=\"https://erddap.github.io/docs/server-admin/datasets#data-types\">data type</a>.\n"
            + "      <br>&nbsp;\n"
            + "    </ul>\n"
            + "  </ul>\n"
            + "<li><a class=\"selfLink\" id=\"specialVariables\" href=\"#specialVariables\" rel=\"bookmark\"\n"
            + "      ><strong>Special Variables</strong></a>\n"
            + "  <br>ERDDAP is aware of the spatial and temporal features of each dataset. The longitude,\n"
            + "  latitude, altitude, depth, and time axis variables (when present) always have specific\n"
            + "  names and units. This makes it easier for you to identify datasets with relevant data,\n"
            + "  to request spatial and temporal subsets of the data, to make images with maps or\n"
            + "  time-series, and to save data in geo-referenced file types (e.g., .esriAscii and .kml).\n"
            + "  <ul>\n"
            + "  <li>In tabledap, a longitude variable (if present) always has the name \""
            + EDV.LON_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.LON_UNITS
            + "\".\n"
            + "  <li>In tabledap, a latitude variable (if present) always has the name \""
            + EDV.LAT_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.LAT_UNITS
            + "\".\n"
            + "  <li>In tabledap, an altitude variable (if present) always has the name \""
            + EDV.ALT_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.ALT_UNITS
            + "\" above sea level.\n"
            + "    Locations below sea level have negative altitude values.\n"
            + "  <li>In tabledap, a depth variable (if present) always has the name \""
            + EDV.DEPTH_NAME
            + "\"\n"
            + "    and the units \""
            + EDV.DEPTH_UNITS
            + "\" below sea level.\n"
            + "    Locations below sea level have positive depth values.\n"
            + "  <li>In tabledap, a time variable (if present) always has the name \""
            + EDV.TIME_NAME
            + "\" and\n"
            + "    the units \""
            + EDV.TIME_UNITS
            + "\".\n"
            + "    If you request data and specify a time constraint, you can specify the time as\n"
            + "    a number (seconds since 1970-01-01T00:00:00Z) or as a\n"
            + "    String value (e.g., \"2002-12-25T07:00:00Z\" in the UTC/GMT/Zulu time zone).\n"
            + "  <li>In tabledap, other variables can be timeStamp variables, which act like the time\n"
            + "      variable but have a different name.\n"
            + "      <br>&nbsp;\n"
            + "  </ul>\n"
            + "<li><a class=\"selfLink\" id=\"incompatibilities\" href=\"#incompatibilities\" rel=\"bookmark\"\n"
            + "      ><strong>Incompatibilities</strong></a>\n"
            + "  <ul>\n"
            + "  <li>Constraints - Different types of data sources support different types of constraints.\n"
            + "    Most data sources don't support all of the types of constraints that tabledap\n"
            + "    advertises. For example, most data sources can't test <kbd><i>variable=~\"RegularExpression\"</i></kbd>.\n"
            + "    When a data source doesn't support a given type of constraint, tabledap\n"
            + "    gets extra data from the source, then does that constraint test itself.\n"
            + "    Sometimes, getting the extra data takes a lot of time or causes the remote\n"
            + "    server to throw an error.\n"
            + "  <li>File Types - Some results file types have restrictions.\n"
            + "    For example, .kml is only appropriate for results with longitude and latitude\n"
            + "    values. If a given request is incompatible with the requested file type,\n"
            + "    tabledap throws an error.\n"
            + "    <br>&nbsp;\n"
            + "  </ul>\n"
            + "<li>"
            + EDStatic.messages.acceptEncodingHtml(language, "h3", tErddapUrl)
            + "    <br>&nbsp;\n"
            + "<li><a class=\"selfLink\" id=\"citeDataset\" href=\"#citeDataset\" rel=\"bookmark\"\n"
            + "      ><strong>How to Cite a Dataset in a Paper</strong></a>\n"
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
            + "    If you get this and think it unwarranted, please email the URL and error message to\n"
            + "     <a rel=\"help\" href=\"#contact\">this ERDDAP's administrator</a> to try to resolve the problem.\n"
            + "  <li>403 Forbidden - when the user's IP address is on this ERDDAP's blacklist.\n"
            + "    If you get this and think it unwarranted, please email the URL and error message to\n"
            + "     <a rel=\"help\" href=\"#contact\">this ERDDAP's administrator</a> to try to resolve the problem.\n"
            + "  <li>404 Not Found - for datasetID not found (perhaps temporarily),\n"
            + "    \"Your query produced no matching results.\", or other request errors.\n"
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
            + "  please email the URL and error to erd.data at noaa.gov .\n"
            + "</ul>\n"
            + "<br>&nbsp;\n"
            + "</ul>\n"
            + "<h2><a class=\"selfLink\" id=\"contact\" href=\"#contact\" rel=\"bookmark\">Contact Us</a></h2>\n"
            + "If you have questions, suggestions, or comments about ERDDAP in general (not this specific\n"
            + "ERDDAP installation or its datasets), please send an email to <kbd>bob dot simons at noaa dot gov</kbd>\n"
            + "and include the ERDDAP URL directly related to your question or comment.\n"
            + "<p><a class=\"selfLink\" id=\"ERDDAPMailingList\" href=\"#ERDDAPMailingList\" rel=\"bookmark\"\n"
            + ">Or,</a> you can join the ERDDAP Google Group / Mailing List by visiting\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://groups.google.com/forum/#!forum/erddap\"\n"
            + ">https://groups.google.com/forum/#!forum/erddap<img \n"
            + "  src=\""
            + tErddapUrl
            + "/images/external.png\" alt=\" (external link)\" \n"
            + "  title=\"This link to an external website does not constitute an endorsement.\"></a> \n"
            + "and clicking on \"Apply for membership\". \n"
            + "Once you are a member, you can post your question there or search to see if the question\n"
            + "has already been asked and answered.\n");

    writer.flush();
  }

  /**
   * This deals with requests for a Make A Graph (MakeAGraph, MAG) web page for this dataset.
   *
   * @param language the index of the selected language
   * @param request may be null. Currently, it isn't used.
   * @param loggedInAs the name of the logged in user (or null if not logged in). Normally, this is
   *     not used to test if this edd is accessibleTo loggedInAs, but it unusual cases
   *     (EDDTableFromPost?) it could be. Normally, this is just used to determine which erddapUrl
   *     to use (http vs https).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery after the '?', still percentEncoded (shouldn't be null). If the query has
   *     missing or invalid parameters, defaults will be used. If the query has irrelevant
   *     parameters, they will be ignored.
   * @param outputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. This doesn't call out.close at the end. The caller must!
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

    if (accessibleViaMAG().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaMAG());

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    if (userDapQuery == null) userDapQuery = "";
    if (reallyVerbose) String2.log("*** respondToGraphQuery");
    if (debugMode) String2.log("respondToGraphQuery 1");

    String formName = "f1"; // change JavaScript below if this changes

    // find the numeric dataVariables
    StringArray sa = new StringArray();
    StringArray nonLLSA = new StringArray();
    StringArray axisSA = new StringArray();
    StringArray nonAxisSA = new StringArray();
    String time_precision[] = new String[dataVariables.length];
    for (int dv = 0; dv < dataVariables.length; dv++) {
      EDV edv = dataVariables[dv];
      if (edv.destinationDataPAType() != PAType.STRING) {
        String dn = edv.destinationName();
        if (dv == lonIndex) {
          axisSA.add(dn);
        } else if (dv == latIndex) {
          axisSA.add(dn);
        } else if (dv == altIndex) {
          axisSA.add(dn);
        } else if (dv == depthIndex) {
          axisSA.add(dn);
        } else if (dv == timeIndex) {
          axisSA.add(dn);
        } else {
          nonAxisSA.add(dn);
        }

        if (edv instanceof EDVTimeStamp)
          time_precision[dv] = edv.combinedAttributes().getString(language, EDV.TIME_PRECISION);

        if (dv != lonIndex && dv != latIndex) nonLLSA.add(dn);

        sa.add(dn);
      }
    }
    String[] nonAxis = nonAxisSA.toArray();
    nonAxisSA = null;
    String[] axis = axisSA.toArray();
    axisSA = null;
    nonLLSA = null;
    String[] numDvNames = sa.toArray();
    sa.atInsert(0, "");
    String[] numDvNames0 = sa.toArray();
    sa = null;
    String[] dvNames0 =
        new String[dataVariableDestinationNames.length + 1]; // all, including string vars
    dvNames0[0] = "";
    System.arraycopy(
        dataVariableDestinationNames, 0, dvNames0, 1, dataVariableDestinationNames.length);

    // gather LLAT range
    StringBuilder llatRange = new StringBuilder();
    int llat[] = {lonIndex, latIndex, altIndex >= 0 ? altIndex : depthIndex, timeIndex};
    for (int llati = 0; llati < 4; llati++) {
      int dv = llat[llati];
      if (dv < 0) continue;
      EDV edv = dataVariables[dv];
      if (Double.isNaN(edv.destinationMinDouble()) && Double.isNaN(edv.destinationMaxDouble())) {
      } else {
        String tMin =
            Double.isNaN(edv.destinationMinDouble())
                ? "(?)"
                : llati == 3
                    ? edv.destinationMinString()
                    : "" + ((float) edv.destinationMinDouble());
        String tMax =
            Double.isNaN(edv.destinationMaxDouble())
                ? (llati == 3 ? "(now?)" : "(?)")
                : llati == 3
                    ? edv.destinationMaxString()
                    : "" + ((float) edv.destinationMaxDouble());
        llatRange.append(
            edv.destinationName()
                + "&nbsp;=&nbsp;"
                + tMin
                + "&nbsp;"
                + EDStatic.messages.get(Message.MAG_RANGE_TO, language)
                + "&nbsp;"
                + tMax
                + (llati == 0 ? "&deg;E" : llati == 1 ? "&deg;N" : llati == 2 ? "m" : "")
                + ", ");
      }
    }
    String otherRows = "";
    if (llatRange.length() > 0) {
      llatRange.setLength(llatRange.length() - 2); // remove last ", "
      otherRows =
          "  <tr><td>"
              + EDStatic.messages.get(Message.MAG_RANGE, language)
              + ":&nbsp;</td>"
              + "<td>"
              + llatRange
              + "</td></tr>\n";
    }

    // *** write the header
    OutputStream out = outputStreamSource.outputStream(File2.UTF_8);
    Writer writer = File2.getBufferedWriterUtf8(out);
    try {
      HtmlWidgets widgets =
          new HtmlWidgets(true, EDStatic.imageDirUrl(request, loggedInAs, language));
      writer.write(
          EDStatic.startHeadHtml(
              language,
              tErddapUrl,
              title(language) + " - " + EDStatic.messages.get(Message.MAG, language)));
      writer.write("\n" + rssHeadLink(language));
      writer.write("\n</head>\n");
      writer.write(
          EDStatic.startBodyHtml(
              request,
              language,
              loggedInAs,
              "tabledap/" + datasetID + ".graph", // was endOfRequest,
              userDapQuery));
      writer.write("\n");
      writer.write(
          HtmlWidgets.htmlTooltipScript(
              EDStatic.imageDirUrl(request, loggedInAs, language))); // this is a link to a script
      writer.flush(); // Steve Souder says: the sooner you can send some html to user, the better
      writer.write("<div class=\"standard_width\">\n");
      writer.write(
          EDStatic.youAreHereWithHelp(
              request,
              language,
              loggedInAs,
              "tabledap",
              EDStatic.messages.get(Message.MAG, language),
              "<div class=\"standard_max_width\">"
                  + EDStatic.messages.get(Message.MAG_TABLE_TOOLTIP, language)
                  + "</div>"));
      writeHtmlDatasetInfo(
          request, language, loggedInAs, writer, true, true, true, false, userDapQuery, otherRows);
      if (userDapQuery.length() == 0)
        userDapQuery =
            defaultGraphQuery(); // after writeHtmlDatasetInfo and before parseUserDapQuery
      writer.write(HtmlWidgets.ifJavaScriptDisabled + "\n");

      // make the big table
      writer.write("&nbsp;\n"); // necessary for the blank line between the table (not <p>)
      writer.write(widgets.beginTable("class=\"compact\""));
      writer.write("<tr><td class=\"T\">\n");

      // begin the form
      writer.write(widgets.beginForm(formName, "GET", "", ""));

      if (debugMode) String2.log("respondToGraphQuery 2");

      // parse the query to get the constraints
      StringArray resultsVariables = new StringArray();
      StringArray constraintVariables = new StringArray();
      StringArray constraintOps = new StringArray();
      StringArray constraintValues = new StringArray();
      int conPo;
      parseUserDapQuery(
          language,
          userDapQuery,
          resultsVariables,
          constraintVariables,
          constraintOps,
          constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
          true);
      // String2.log("conVars=" + constraintVariables);
      // String2.log("conOps=" + constraintOps);
      // String2.log("conVals=" + constraintValues);

      // some cleaning (needed by link from Subset)
      // remove if invalid or String
      for (int rv = resultsVariables.size() - 1; rv >= 0; rv--) {
        int dv = String2.indexOf(dataVariableDestinationNames(), resultsVariables.get(rv));
        if (dv < 0 || (dv >= 0 && dataVariables[dv].destinationDataPAType() == PAType.STRING))
          resultsVariables.remove(rv);
      }

      // parse the query so &-separated parts are handy
      String paramName, partName, partValue, pParts[];
      String queryParts[] =
          Table.getDapQueryParts(userDapQuery); // always at least 1 part (may be "")
      // String2.log(">>queryParts=" + String2.toCSSVString(queryParts));

      // try to setup things for zoomLatLon
      // lon/lat/Min/Max reflect active lon/lat constraints (explicit &longitude>=, or implicit
      // (click))
      double lonMin = Double.NaN, lonMax = Double.NaN;
      double latMin = Double.NaN, latMax = Double.NaN;
      double timeMin = Double.NaN, timeMax = Double.NaN;

      // maxExtentWESN is world-like limits (not data min/max)
      // ??? how know if +/-180 or 0...360, or 0...400 (that's the real problem)???
      double maxExtentWESN[] = {-180, 400, -90, 90};

      // remove &.click &.zoom from userDapQuery (but they are still in queryParts)
      // so userDapQuery is now the same one used to generate the image the user clicked or zoomed
      // on.
      int clickPo = userDapQuery.indexOf("&.click=");
      if (clickPo >= 0) userDapQuery = userDapQuery.substring(0, clickPo);
      int zoomPo = userDapQuery.indexOf("&.zoom=");
      if (zoomPo >= 0) userDapQuery = userDapQuery.substring(0, zoomPo);

      Object pngInfo[] = null;
      double pngInfoDoubleWESN[] = null;
      int pngInfoIntWESN[] = null;
      // if user clicked or zoomed, read pngInfo file
      // (if available, e.g., if graph is x=lon, y=lat and image recently created)
      if ((clickPo >= 0 || zoomPo >= 0) && lonIndex >= 0 && latIndex >= 0) {
        // okay if not available
        pngInfo = readPngInfo(loggedInAs, userDapQuery, ".png");
        if (pngInfo != null) {
          pngInfoDoubleWESN = (double[]) pngInfo[0];
          pngInfoIntWESN = (int[]) pngInfo[1];
        }
      }

      // if lon min max is known, use it to adjust maxExtentWESN[0,1]
      if (lonIndex >= 0) {
        EDV edv = dataVariables[lonIndex];
        double dMin = edv.destinationMinDouble();
        double dMax = edv.destinationMaxDouble();
        if (!Double.isNaN(dMin) && !Double.isNaN(dMax)) {
          if (dMax > 360) {
            double lh[] = Math2.suggestLowHigh(Math.min(dMin, 0), dMax);
            maxExtentWESN[0] = lh[0];
            maxExtentWESN[1] = lh[1];
          } else if (dMax <= 180) { // preference to this
            maxExtentWESN[0] = -180; // data in 0..180 put here arbitrarily
            maxExtentWESN[1] = 180;
          } else {
            double lh[] = Math2.suggestLowHigh(Math.min(dMin, 0), Math.max(dMax, 360));
            maxExtentWESN[0] = lh[0];
            maxExtentWESN[1] = lh[1];
          }
        }

        if (reallyVerbose) String2.log("  maxExtentWESN=" + String2.toCSSVString(maxExtentWESN));
      }

      // note explicit longitude/longitude/time>= <= constraints
      String tpStart;
      int tpi;
      // longitude
      tpStart = "longitude>=";
      tpi = String2.lineStartsWith(queryParts, tpStart);
      if (tpi < 0) {
        tpStart = "longitude>";
        tpi = String2.lineStartsWith(queryParts, tpStart);
      }
      if (tpi >= 0) {
        double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
        // ignore dVal isNaN, but fix if out of range
        if (dVal < maxExtentWESN[0]) {
          dVal = maxExtentWESN[0];
          queryParts[tpi] = tpStart + dVal;
        }
        lonMin = dVal; // may be NaN
      }
      tpStart = "longitude<=";
      tpi = String2.lineStartsWith(queryParts, tpStart);
      if (tpi < 0) {
        tpStart = "longitude<";
        tpi = String2.lineStartsWith(queryParts, tpStart);
      }
      if (tpi >= 0) {
        // special: fixup maxExtentWESN[0]    //this is still imperfect and may be always
        if (lonMin == -180) maxExtentWESN[1] = 180;
        double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
        // ignore dVal isNaN, but fix if out of range
        if (dVal > maxExtentWESN[1]) {
          dVal = maxExtentWESN[1];
          queryParts[tpi] = tpStart + dVal;
        }
        lonMax = dVal; // may be NaN
      }
      // latitude
      tpStart = "latitude>=";
      tpi = String2.lineStartsWith(queryParts, tpStart);
      if (tpi < 0) {
        tpStart = "latitude>";
        tpi = String2.lineStartsWith(queryParts, tpStart);
      }
      if (tpi >= 0) {
        double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
        // ignore dVal isNaN, but fix if out of range
        if (dVal < maxExtentWESN[2]) {
          dVal = maxExtentWESN[2];
          queryParts[tpi] = tpStart + dVal;
        }
        latMin = dVal; // may be NaN
      }
      tpStart = "latitude<=";
      tpi = String2.lineStartsWith(queryParts, tpStart);
      if (tpi < 0) {
        tpStart = "latitude<";
        tpi = String2.lineStartsWith(queryParts, tpStart);
      }
      if (tpi >= 0) {
        double dVal = String2.parseDouble(queryParts[tpi].substring(tpStart.length()));
        // ignore dVal isNaN, but fix if out of range
        if (dVal > maxExtentWESN[3]) {
          dVal = maxExtentWESN[3];
          queryParts[tpi] = tpStart + dVal;
        }
        latMax = dVal; // may be NaN
      }

      if (pngInfoDoubleWESN == null
          && !Double.isNaN(lonMin)
          && !Double.isNaN(lonMax)
          && !Double.isNaN(latMin)
          && !Double.isNaN(latMax))
        pngInfoDoubleWESN = new double[] {lonMin, lonMax, latMin, latMax};

      // time
      tpStart = "time>=";
      tpi = String2.lineStartsWith(queryParts, tpStart);
      if (tpi < 0) {
        tpStart = "time>";
        tpi = String2.lineStartsWith(queryParts, tpStart);
      }
      if (tpi >= 0) {
        String sVal = queryParts[tpi].substring(tpStart.length());
        timeMin =
            sVal.toLowerCase().startsWith("now")
                ? Calendar2.safeNowStringToEpochSeconds(sVal, timeMin)
                : Calendar2.isIsoDate(sVal)
                    ? Calendar2.isoStringToEpochSeconds(sVal)
                    : String2.parseDouble(sVal); // may be NaN
      }
      tpStart = "time<=";
      tpi = String2.lineStartsWith(queryParts, tpStart);
      if (tpi < 0) {
        tpStart = "time<";
        tpi = String2.lineStartsWith(queryParts, tpStart);
      }
      if (tpi >= 0) {
        String sVal = queryParts[tpi].substring(tpStart.length());
        timeMax =
            sVal.toLowerCase().startsWith("now")
                ? Calendar2.safeNowStringToEpochSeconds(sVal, timeMax)
                : Calendar2.isIsoDate(sVal)
                    ? Calendar2.isoStringToEpochSeconds(sVal)
                    : String2.parseDouble(sVal); // may be NaN
      }
      if (reallyVerbose)
        String2.log(
            "  constraint lon>="
                + lonMin
                + " <="
                + lonMax
                + "\n  constraint lat>="
                + latMin
                + " <="
                + latMax
                + "\n  constraint time>="
                + Calendar2.safeEpochSecondsToIsoStringTZ(timeMin, "NaN")
                + " <="
                + Calendar2.safeEpochSecondsToIsoStringTZ(timeMax, "NaN"));

      // If user clicked on map, change lon/lat/Min/Max
      partValue =
          String2.stringStartsWith(
              queryParts, partName = ".click=?"); // ? indicates user clicked on map
      if (partValue != null && pngInfo != null) {
        if (reallyVerbose) String2.log("  processing " + partValue);
        try {
          String xy[] = String2.split(partValue.substring(8), ','); // e.g., 24,116
          double clickLonLat[] =
              SgtUtil.xyToLonLat(
                  String2.parseInt(xy[0]),
                  String2.parseInt(xy[1]),
                  pngInfoIntWESN,
                  pngInfoDoubleWESN,
                  maxExtentWESN);
          if (clickLonLat == null) {
            if (verbose) String2.log("  clickLonLat is null!");
          } else {
            if (verbose) String2.log("  click center=" + clickLonLat[0] + ", " + clickLonLat[1]);

            // get current radius, and shrink if clickLonLat is closer to maxExtent
            double radius =
                Math.max(
                        pngInfoDoubleWESN[1] - pngInfoDoubleWESN[0],
                        pngInfoDoubleWESN[3] - pngInfoDoubleWESN[2])
                    / 2;

            int nPlaces = lonLatNPlaces(radius);
            clickLonLat[0] = Math2.roundTo(clickLonLat[0], nPlaces);
            clickLonLat[1] = Math2.roundTo(clickLonLat[1], nPlaces);

            radius = Math.min(radius, clickLonLat[0] - maxExtentWESN[0]);
            radius = Math.min(radius, maxExtentWESN[1] - clickLonLat[0]);
            radius = Math.min(radius, clickLonLat[1] - maxExtentWESN[2]);
            radius = Math.min(radius, maxExtentWESN[3] - clickLonLat[1]);
            radius = Math2.roundTo(radius, nPlaces);
            if (verbose) String2.log("  postclick radius=" + radius);

            // if not too close to data's limits...  success
            if (radius >= 0.01) {
              lonMin = clickLonLat[0] - radius;
              lonMax = clickLonLat[0] + radius;
              latMin = clickLonLat[1] - radius;
              latMax = clickLonLat[1] + radius;
            }
          }
        } catch (Throwable t) {
          EDStatic.rethrowClientAbortException(t); // first thing in catch{}
          String2.log("Error while trying to read &.click? value.\n" + MustBe.throwableToString(t));
        }
      }

      // If user clicked on a zoom button, change lon/lat/Min/Max
      partValue = String2.stringStartsWith(queryParts, partName = ".zoom=");
      if (partValue != null && pngInfoDoubleWESN != null) {
        if (reallyVerbose) String2.log("  processing " + partValue);
        try {
          partValue = partValue.substring(6);
          // use lonlatMinMax from pngInfo
          lonMin = pngInfoDoubleWESN[0];
          lonMax = pngInfoDoubleWESN[1];
          latMin = pngInfoDoubleWESN[2];
          latMax = pngInfoDoubleWESN[3];
          double lonRange = lonMax - lonMin;
          double latRange = latMax - latMin;
          double lonCenter = (lonMin + lonMax) / 2;
          double latCenter = (latMin + latMax) / 2;
          double cRadius = Math.max(Math.abs(lonRange), Math.abs(latRange)) / 2;
          double zoomOut = cRadius * 5 / 4;
          double zoomOut2 = cRadius * 2;
          double zoomOut8 = cRadius * 8;
          double zoomIn = cRadius * 4 / 5;
          double zoomIn2 = cRadius / 2;
          double zoomIn8 = cRadius / 8;
          // using nPlaces = 3 makes zoomIn/Out cleanly repeatable (lonLatNPlaces doesn't)
          int nPlaces = 3; // lonLatNPlaces(zoomOut8);

          double tZoom =
              partValue.equals("out8")
                  ? zoomOut8
                  : partValue.equals("out2") ? zoomOut2 : partValue.equals("out") ? zoomOut : 0;
          if (tZoom != 0) {
            // if zoom out, keep ranges intact by moving tCenter to safe place
            double tLonCenter, tLatCenter;
            tLonCenter = Math.max(lonCenter, maxExtentWESN[0] + tZoom);
            tLonCenter = Math.min(tLonCenter, maxExtentWESN[1] - tZoom);
            tLatCenter = Math.max(latCenter, maxExtentWESN[2] + tZoom);
            tLatCenter = Math.min(tLatCenter, maxExtentWESN[3] - tZoom);
            lonMin = Math2.roundTo(tLonCenter - tZoom, nPlaces);
            lonMax = Math2.roundTo(tLonCenter + tZoom, nPlaces);
            latMin = Math2.roundTo(tLatCenter - tZoom, nPlaces);
            latMax = Math2.roundTo(tLatCenter + tZoom, nPlaces);
            lonMin = Math.max(maxExtentWESN[0], lonMin);
            lonMax = Math.min(maxExtentWESN[1], lonMax);
            latMin = Math.max(maxExtentWESN[2], latMin);
            latMax = Math.min(maxExtentWESN[3], latMax);

          } else {
            tZoom =
                partValue.equals("in8")
                    ? zoomIn8
                    : partValue.equals("in2") ? zoomIn2 : partValue.equals("in") ? zoomIn : 0;
            if (tZoom != 0) {
              lonMin = Math2.roundTo(lonCenter - tZoom, nPlaces);
              lonMax = Math2.roundTo(lonCenter + tZoom, nPlaces);
              latMin = Math2.roundTo(latCenter - tZoom, nPlaces);
              latMax = Math2.roundTo(latCenter + tZoom, nPlaces);
            }
          }

        } catch (Throwable t) {
          EDStatic.rethrowClientAbortException(t); // first thing in catch{}
          String2.log(
              "Error while trying to process &.zoom="
                  + partValue
                  + "\n"
                  + MustBe.throwableToString(t));
        }
      }

      // if lon/lat/Min/Max were changed, change constraints that are affected
      boolean wesnFound[] = new boolean[4]; // default=all false
      for (int c = 0; c < constraintVariables.size(); c++) {
        String var = constraintVariables.get(c);
        String op = constraintOps.get(c);
        if (var.equals("longitude")) {
          if (op.startsWith(">")) {
            wesnFound[0] = true;
            if (!Double.isNaN(lonMin)) constraintValues.set(c, String2.genEFormat6(lonMin));
          } else if (op.startsWith("<")) {
            wesnFound[1] = true;
            if (!Double.isNaN(lonMax)) constraintValues.set(c, String2.genEFormat6(lonMax));
          }
        } else if (var.equals("latitude")) {
          if (op.startsWith(">")) {
            wesnFound[2] = true;
            if (!Double.isNaN(latMin)) constraintValues.set(c, String2.genEFormat6(latMin));
          } else if (op.startsWith("<")) {
            wesnFound[3] = true;
            if (!Double.isNaN(latMax)) constraintValues.set(c, String2.genEFormat6(latMax));
          }
        }
      }
      // if user clicked on map when no lat lon constraints, I need to add lat lon constraints
      if (!wesnFound[0] && !Double.isNaN(lonMin)) {
        constraintVariables.add("longitude");
        constraintOps.add(">=");
        constraintValues.add(String2.genEFormat6(lonMin));
      }
      if (!wesnFound[1] && !Double.isNaN(lonMax)) {
        constraintVariables.add("longitude");
        constraintOps.add("<=");
        constraintValues.add(String2.genEFormat6(lonMax));
      }
      if (!wesnFound[2] && !Double.isNaN(latMin)) {
        constraintVariables.add("latitude");
        constraintOps.add(">=");
        constraintValues.add(String2.genEFormat6(latMin));
      }
      if (!wesnFound[3] && !Double.isNaN(latMax)) {
        constraintVariables.add("latitude");
        constraintOps.add("<=");
        constraintValues.add(String2.genEFormat6(latMax));
      }

      // * write all of the distinct options arrays
      String distinctOptions[][] = null;
      if (accessibleViaSubset().length() == 0) {
        distinctOptions = distinctOptionsTable(language, loggedInAs, 60); // maxChar=60
        writer.write(
            """
                <script>
                var distinctOptions=[
                """);
        for (int sv = 0; sv < subsetVariables.length; sv++) {
          // encodeSpaces as nbsp solves the problem with consecutive internal spaces
          int ndo = distinctOptions[sv].length;
          for (int doi = 0; doi < ndo; doi++)
            distinctOptions[sv][doi] = XML.minimalEncodeSpaces(distinctOptions[sv][doi]);

          writer.write(" [");
          writer.write(new StringArray(distinctOptions[sv]).toJsonCsvString());
          writer.write("]" + (sv == subsetVariables.length - 1 ? "" : ",") + "\n");
        }
        writer.write("""
                ];
                </script>
                """);
      }

      // ** write the table
      StringBuilder graphQuery = new StringBuilder();
      StringBuilder graphQueryNoLatLon = new StringBuilder();
      StringBuilder graphQueryNoTime = new StringBuilder();
      String gap = "&nbsp;&nbsp;&nbsp;";

      // set the Graph Type    //eeek! what if only 1 var?
      StringArray drawsSA = new StringArray();
      // it is important for javascript below that first 3 options are the very similar (L L&M M)
      drawsSA.add("lines");
      drawsSA.add("linesAndMarkers");
      drawsSA.add("markers");
      int defaultDraw = 2;
      if (axis.length >= 1 && nonAxis.length >= 2) drawsSA.add("sticks");
      if (lonIndex >= 0 && latIndex >= 0 && nonAxis.length >= 2) drawsSA.add("vectors");
      String draws[] = drawsSA.toArray();
      int draw = defaultDraw;
      partValue = String2.stringStartsWith(queryParts, partName = ".draw=");
      if (partValue != null) {
        draw = String2.indexOf(draws, partValue.substring(partName.length()));
        if (draw < 0) draw = defaultDraw;
      }
      boolean drawLines = draws[draw].equals("lines");
      boolean drawLinesAndMarkers = draws[draw].equals("linesAndMarkers");
      boolean drawMarkers = draws[draw].equals("markers");
      boolean drawSticks = draws[draw].equals("sticks");
      boolean drawVectors = draws[draw].equals("vectors");
      writer.write(widgets.beginTable("class=\"compact nowrap\""));
      paramName = "draw";
      writer.write(
          "<tr>\n"
              + "  <td><strong>"
              + EDStatic.messages.get(Message.MAG_GRAPH_TYPE, language)
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
              "onChange='mySubmit("
                  + // change->submit so form always reflects graph type
                  (draw < 3
                      ? "document.f1.draw.selectedIndex<3"
                      : // if old and new draw are <3, do send var names
                      "false")
                  + // else don't send var names
                  ");'"));
      writer.write(
          EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  "<div class=\"standard_max_width\">"
                      + EDStatic.messages.get(Message.MAG_GRAPH_TYPE_TOOLTIP_TABLE, language)
                      + "</div>")
              + "  </td>\n"
              + "</tr>\n");
      if (debugMode) String2.log("respondToGraphQuery 3");

      // heuristic: look for two adjacent nonAxis vars that have same units
      int sameUnits1 = 0; // default = first nonAxis var
      if (nonAxis.length >= 2) {
        String units1 = null;
        String units2 = findDataVariableByDestinationName(nonAxis[0]).units();
        for (int sameUnits2 = 1; sameUnits2 < nonAxis.length; sameUnits2++) {
          units1 = units2;
          units2 = findDataVariableByDestinationName(nonAxis[sameUnits2]).units();
          if (units1 != null && units2 != null && units1.equals(units2)) {
            sameUnits1 = sameUnits2 - 1;
            // String2.log("useDefaultVars sameUnits1=" + sameUnits1;
            break;
          }
        }
      }

      // if no userDapQuery, set default variables
      boolean useDefaultVars = userDapQuery.length() == 0 || userDapQuery.startsWith("&");
      if (useDefaultVars) resultsVariables.clear(); // parse() set resultsVariables to all variables

      // set draw-related things
      int nVars = -1, variablesShowPM = 0;
      String varLabel[], varHelp[], varOptions[][];
      if (reallyVerbose)
        String2.log("pre  set draw-related; resultsVariables=" + resultsVariables.toString());
      if (drawLines) {
        nVars = 2;
        variablesShowPM = 1;
        varLabel =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_X, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_Y, language) + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_HELP_GRAPH_X, language),
              EDStatic.messages.get(Message.MAG_AXIS_HELP_GRAPH_Y, language)
            };
        varOptions = new String[][] {numDvNames, numDvNames};
        if (useDefaultVars || resultsVariables.size() < 2) {
          resultsVariables.clear();
          if (lonIndex >= 0 && latIndex >= 0) {
            resultsVariables.add(EDV.LON_NAME);
            resultsVariables.add(EDV.LAT_NAME);
          } else if (timeIndex >= 0 && nonAxis.length >= 1) {
            resultsVariables.add(EDV.TIME_NAME);
            resultsVariables.add(nonAxis[0]);
          } else if (nonAxis.length >= 2) {
            resultsVariables.add(nonAxis[0]);
            resultsVariables.add(nonAxis[1]);
          } else {
            resultsVariables.add(numDvNames[0]);
            if (numDvNames.length > 0) resultsVariables.add(numDvNames[1]);
          }
        }
      } else if (drawLinesAndMarkers || drawMarkers) {
        nVars = 3;
        variablesShowPM = 2;
        varLabel =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_X, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_Y, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_COLOR, language) + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_HELP_GRAPH_X, language),
              EDStatic.messages.get(Message.MAG_AXIS_HELP_GRAPH_Y, language),
              EDStatic.messages.get(Message.MAG_AXIS_HELP_MARKER_COLOR, language)
            };
        varOptions = new String[][] {numDvNames, numDvNames, numDvNames0};
        if (useDefaultVars || resultsVariables.size() < 2) {
          resultsVariables.clear();
          if (lonIndex >= 0 && latIndex >= 0) {
            resultsVariables.add(EDV.LON_NAME);
            resultsVariables.add(EDV.LAT_NAME);
            if (nonAxis.length >= 1) resultsVariables.add(nonAxis[0]);
          } else if (timeIndex >= 0 && nonAxis.length >= 1) {
            resultsVariables.add(EDV.TIME_NAME);
            resultsVariables.add(nonAxis[0]);
          } else if (nonAxis.length >= 2) {
            resultsVariables.add(nonAxis[0]);
            resultsVariables.add(nonAxis[1]);
          } else {
            resultsVariables.add(numDvNames[0]);
            if (numDvNames.length > 0) resultsVariables.add(numDvNames[1]);
          }
        }
      } else if (drawSticks) {
        nVars = 3;
        variablesShowPM = 1;
        varLabel =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_X, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_STICK_X, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_STICK_Y, language) + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_HELP_GRAPH_X, language),
              EDStatic.messages.get(Message.MAG_AXIS_HELP_STICK_X, language),
              EDStatic.messages.get(Message.MAG_AXIS_HELP_STICK_Y, language)
            };
        varOptions = new String[][] {numDvNames, numDvNames, numDvNames};
        if (useDefaultVars || resultsVariables.size() < 3) {
          // x var is an axis (prefer: time)
          resultsVariables.clear();
          resultsVariables.add(timeIndex >= 0 ? EDV.TIME_NAME : axis[0]);
          resultsVariables.add(nonAxis[sameUnits1]);
          resultsVariables.add(nonAxis[sameUnits1 + 1]);
        }
      } else if (drawVectors) {
        nVars = 4;
        variablesShowPM = 100;
        varLabel =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_X, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_Y, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_VECTOR_X, language) + ":",
              EDStatic.messages.get(Message.MAG_AXIS_VECTOR_Y, language) + ":"
            };
        varHelp =
            new String[] {
              EDStatic.messages.get(Message.MAG_AXIS_HELP_MAP_X, language),
                  EDStatic.messages.get(Message.MAG_AXIS_HELP_MAP_Y, language),
              EDStatic.messages.get(Message.MAG_AXIS_HELP_VECTOR_X, language),
                  EDStatic.messages.get(Message.MAG_AXIS_HELP_VECTOR_Y, language)
            };
        varOptions =
            new String[][] {
              new String[] {EDV.LON_NAME}, new String[] {EDV.LAT_NAME}, nonAxis, nonAxis
            };
        if (useDefaultVars || resultsVariables.size() < 4) {
          resultsVariables.clear();
          resultsVariables.add(EDV.LON_NAME);
          resultsVariables.add(EDV.LAT_NAME);
          resultsVariables.add(nonAxis[sameUnits1]);
          resultsVariables.add(nonAxis[sameUnits1 + 1]);
        }
      } else
        throw new SimpleException(
            EDStatic.messages.get(Message.ERROR_INTERNAL, 0) + "No drawXxx value was set.");
      if (debugMode) String2.log("respondToGraphQuery 4");
      if (reallyVerbose)
        String2.log("post set draw-related; resultsVariables=" + resultsVariables.toString());

      // collect info for zoomX below if found
      EDV zoomXEdv = null;
      int zoomXCv = -1; // constraint row not yet known
      double zoomXMin = Double.NaN; // will be set if known
      double zoomXMax = Double.NaN;

      // pick variables
      for (int v = 0; v < nVars; v++) {
        String tDvNames[] = varOptions[v];
        int vi =
            v >= resultsVariables.size()
                ? 0
                : Math.max(0, String2.indexOf(tDvNames, resultsVariables.get(v)));
        if (v >= resultsVariables.size()) resultsVariables.add(tDvNames[vi]);
        else resultsVariables.set(v, tDvNames[vi]);
        if (v == 0) zoomXEdv = findVariableByDestinationName(tDvNames[vi]);
        paramName = "var" + v;
        writer.write("<tr>\n" + "  <td>" + varLabel[v] + "&nbsp;" + "  </td>\n" + "  <td>\n");
        writer.write(
            widgets.select(
                paramName,
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_AXIS_VAR_HELP, language), varHelp[v]),
                v >= variablesShowPM ? HtmlWidgets.BUTTONS_1 : 1, // was 1
                tDvNames,
                vi,
                "",
                false,
                "mySubmit(true);")); // was ""));
        writer.write("""
                  </td>
                </tr>
                """);
      }
      boolean zoomLatLon =
          resultsVariables.get(0).equals(EDV.LON_NAME)
              && resultsVariables.get(1).equals(EDV.LAT_NAME);

      // end of variables table
      writer.write(widgets.endTable());

      if (debugMode) String2.log("respondToGraphQuery 5");

      // *** write the constraint table
      writer.write("&nbsp;\n"); // necessary for the blank line before the table (not <p>)
      writer.write(widgets.beginTable("class=\"compact nowrap\""));
      writer.write(
          "<tr>\n"
              + "  <th>"
              + EDStatic.messages.get(Message.EDD_TABLE_CONSTRAINTS, language)
              + " "
              + EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  EDStatic.messages.get(Message.MAG_CONSTRAINT_HELP, language))
              + "</th>\n"
              + "  <th style=\"text-align:center;\" colspan=\"2\">"
              + EDStatic.messages.get(Message.EDD_TABLE_OPT_CONSTRAINT1_HTML, language)
              + " "
              + EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  "<div class=\"narrow_max_width\">"
                      + EDStatic.messages.get(Message.EDD_TABLE_CONSTRAINT_TOOLTIP, language)
                      + "</div>")
              + "</th>\n"
              + "  <th style=\"text-align:center;\" colspan=\"2\">"
              + EDStatic.messages.get(Message.EDD_TABLE_OPT_CONSTRAINT2_HTML, language)
              + " "
              + EDStatic.htmlTooltipImage(
                  request,
                  language,
                  loggedInAs,
                  "<div class=\"narrow_max_width\">"
                      + EDStatic.messages.get(Message.EDD_TABLE_CONSTRAINT_TOOLTIP, language)
                      + "</div>")
              + "</th>\n"
              + "</tr>\n");

      // a row for each constrained variable
      int nConRows = 5;
      int conVar[] = new int[nConRows];
      int conOp[][] = new int[nConRows][2];
      String conVal[][] = new String[nConRows][2]; // initially nulls
      // INACTIVE: StringBuilder onloadSB = new StringBuilder();
      for (int cv = 0; cv < nConRows; cv++) {
        writer.write("<tr>\n");
        String tConVala = "";

        // get constraints from query
        // !!! since each constraintVar/Op/Val is removed after processing,
        //    always work on constraintVar/Op/Val #0
        if (constraintVariables.size() > 0) {
          // parse() ensured that all constraintVariables and Ops are valid
          conVar[cv] = String2.indexOf(dvNames0, constraintVariables.get(0)); // yes, 0 (see above)
          boolean use1 = constraintOps.get(0).startsWith("<");
          conOp[cv][use1 ? 1 : 0] = OPERATORS.indexOf(constraintOps.get(0));
          conVal[cv][use1 ? 1 : 0] = constraintValues.get(0);
          constraintVariables.remove(0); // yes, 0 (see above)
          constraintOps.remove(0);
          constraintValues.remove(0);
          // another constraint for same var?
          conPo = constraintVariables.indexOf(dvNames0[conVar[cv]]);
          if (conPo >= 0) {
            conOp[cv][use1 ? 0 : 1] = OPERATORS.indexOf(constraintOps.get(conPo));
            conVal[cv][use1 ? 0 : 1] = constraintValues.get(conPo);
            constraintVariables.remove(conPo);
            constraintOps.remove(conPo);
            constraintValues.remove(conPo);
          } else {
            conOp[cv][use1 ? 0 : 1] = opDefaults[use1 ? 0 : 1];
            conVal[cv][use1 ? 0 : 1] = null; // still
          }
        } else { // else blanks
          conVar[cv] = 0;
          conOp[cv][0] = opDefaults[0];
          conOp[cv][1] = opDefaults[1];
          conVal[cv][0] = null; // still
          conVal[cv][1] = null; // still
        }
        EDV conEdv = null;
        if (conVar[cv] > 0) // >0 since option 0=""
        conEdv = dataVariables()[conVar[cv] - 1];

        for (int con = 0; con < 2; con++) {
          String ab = con == 0 ? "a" : "b";

          // ensure conVal[cv][con] is valid
          if (conVal[cv][con] == null) {
          } else if (conVal[cv][con].length() > 0) {
            if (conEdv instanceof EDVTimeStamp ts) {
              // convert numeric time (from parseUserDapQuery) to iso
              conVal[cv][con] = ts.destinationToString(String2.parseDouble(conVal[cv][con]));
            } else if (dataVariables[conVar[cv] - 1].destinationDataPAType() == PAType.CHAR
                || dataVariables[conVar[cv] - 1].destinationDataPAType() == PAType.STRING) {
              // -1 above since option 0=""
              // leave String constraint values intact
            } else if (conVal[cv][con].equalsIgnoreCase("nan")) { // NaN  (or clean up to "NaN")
              conVal[cv][con] = "NaN";
            } else if (Double.isNaN(String2.parseDouble(conVal[cv][con]))) {
              // it should be numeric, but NaN, set to null
              conVal[cv][con] = null;
              // else leave as it
            }
          }

          if (cv == 0 && userDapQuery.length() == 0 && timeIndex >= 0) {
            // suggest a time constraint: last week's worth of data
            conVar[cv] = timeIndex + 1; // +1 since option 0=""
            conEdv = dataVariables()[conVar[cv] - 1];
            double d = Calendar2.backNDays(-1, conEdv.destinationMaxDouble()); // coming midnight
            if (con == 0) d = Calendar2.backNDays(7, d);
            conVal[cv][con] =
                Calendar2.epochSecondsToLimitedIsoStringT(
                    conEdv.combinedAttributes().getString(language, EDV.TIME_PRECISION), d, "");
            if (con == 0) timeMin = d;
            else timeMax = d;
          }

          // variable list
          if (con == 0) {
            // make javascript
            StringBuilder cjs = null;
            if (distinctOptions != null) {
              cjs = new StringBuilder(1024);
              // The constraint variable onChange causes the distinct options row to be updated
              cjs.append(
                  "onChange='"
                      +
                      // make the row and select widget invisible
                      " document.body.style.cursor=\"wait\";"
                      + // no effect in Firefox
                      " var tDisRow=document.getElementById(\"disRow"
                      + cv
                      + "\");"
                      + " tDisRow.style.display=\"none\";"
                      + // hides/closes up the row
                      " var tDis=document.getElementById(\"dis"
                      + cv
                      + "\");"
                      + " tDis.selected=0;"
                      + " tDis.options.length=0;"
                      + " document."
                      + formName
                      + ".cVal"
                      + cv
                      + "a.value=\"\";"
                      + " document."
                      + formName
                      + ".cVal"
                      + cv
                      + "b.value=\"\";"
                      +
                      // is newly selected var a subsetVar?
                      " var vName=this.options[this.selectedIndex].text;"
                      + " var sv=-1;"
                      + " if (vName==\""
                      + subsetVariables[0]
                      + "\") sv=0;");
              for (int sv = 1; sv < subsetVariables.length; sv++)
                cjs.append(" else if (vName==\"" + subsetVariables[sv] + "\") sv=" + sv + ";");
              cjs.append(
                  // change the operator
                  " document."
                      + formName
                      + ".cOp"
                      + cv
                      + "a.selectedIndex="
                      + "(sv>=0?"
                      + OPERATORS.indexOf("=")
                      + ":"
                      + OPERATORS.indexOf(">=")
                      + ");"
                      + " if (sv>=0) {"
                      +
                      // clear distinct values, populate select widget, and make tDisRow visible
                      "  var n=distinctOptions[sv].length;"
                      + "  var tDisOptions = tDis.options;"
                      + // as few lookups as possible in loop
                      "  var dosv = distinctOptions[sv];"
                      + // as few lookups as possible in loop
                      "  for (var i=0; i<n; i++) tDisOptions.add(new Option(dosv[i]));"
                      + // [i]faster than add() in FF, but unique_tag_id not sorted!
                      "  tDisRow.style.display=\"\";"
                      + // make it visible: "" defaults to "block"; don't use "block" to avoid
                      // problems in firefox
                      "}"
                      + " document.body.style.cursor=\"default\";"
                      + // no effect in Firefox
                      "'");
            }

            writer.write("  <td>");
            writer.write(
                widgets.select(
                    "cVar" + cv,
                    EDStatic.messages.get(Message.EDD_TABLE_OPT_CONSTRAINT_VAR, language),
                    1,
                    dvNames0,
                    conVar[cv],
                    cjs == null ? "" : cjs.toString()));
            writer.write("</td>\n");
          }

          // make the constraintTooltip
          // ConstraintHtml below is worded in generic way so works with number and time, too
          String constraintTooltip =
              EDStatic.messages.get(Message.EDD_TABLE_STRING_CONSTRAINT_TOOLTIP, language);
          // String2.log("cv=" + cv + " conVar[cv]=" + conVar[cv]);
          if (conEdv != null && conEdv.destinationMinString().length() > 0) {
            constraintTooltip +=
                "<br>&nbsp;<br>"
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.RANGES_FROM_TO, language),
                        conEdv.destinationName(),
                        conEdv.destinationMinString(),
                        conEdv.destinationMaxString().length() > 0
                            ? conEdv.destinationMaxString()
                            : "(?)");
          }

          // display the op and value
          int tOp = conOp[cv][con];
          writer.write("  <td class=\"R\">" + gap);
          writer.write(
              widgets.select(
                  "cOp" + cv + ab,
                  EDStatic.messages.get(Message.EDD_TABLE_SELECT_AN_OPERATOR, language),
                  1,
                  OPERATORS,
                  PPE_OPERATORS,
                  tOp,
                  "",
                  false,
                  ""));
          writer.write("</td>\n");
          String tVal = conVal[cv][con];
          if (tVal == null) tVal = "";
          else if (tOp == REGEX_OP_INDEX
              || (conEdv != null
                  && (conEdv.destinationDataPAType() == PAType.STRING
                      || conEdv.destinationDataPAType() == PAType.CHAR))) {
            tVal = String2.toJson(tVal); // enclose in "
          }
          if (con == 0) tConVala = tVal;
          writer.write("  <td>");
          writer.write(widgets.textField("cVal" + cv + ab, constraintTooltip, 19, 255, tVal, ""));
          writer.write("</td>\n");

          // add to graphQuery
          if (conVar[cv] > 0 && tVal.length() > 0) {
            String tq =
                "&"
                    + dvNames0[conVar[cv]]
                    + PPE_OPERATORS.get(conOp[cv][con])
                    + SSR.minimalPercentEncode(tVal);
            graphQuery.append(tq);
            if (!(conEdv instanceof EDVLat) && !(conEdv instanceof EDVLon))
              graphQueryNoLatLon.append(tq);
            if (!(conEdv instanceof EDVTime)) graphQueryNoTime.append(tq);

            if (conEdv != null
                && conEdv == zoomXEdv
                && // this constraint is for the x axis variable
                (zoomXCv < 0 || zoomXCv == cv)) { // both constraints must be from 1 constraints row
              zoomXCv = cv;
              String top = OPERATORS.get(conOp[cv][con]);
              if (top.indexOf('>') >= 0) zoomXMin = String2.parseDouble(tVal);
              else if (top.indexOf('<') >= 0) zoomXMax = String2.parseDouble(tVal);
            }
          }
        }

        // end of constraint row
        writer.write("</tr>\n");

        // Display distinctOptions?
        if (distinctOptions != null) {
          int whichSV =
              conEdv == null ? -1 : String2.indexOf(subsetVariables, conEdv.destinationName());

          writer.write(
              "<tr id=\"disRow"
                  + cv
                  + "\" "
                  + (whichSV < 0 ? "style=\"display:none;\" " : "")
                  + // hide the row?
                  ">\n"
                  + "  <td>&nbsp;</td>\n"
                  + "  <td>&nbsp;</td>\n"
                  + "  <td colspan=\"3\">\n"
                  + "    "
                  + widgets.beginTable("class=\"compact\"")
                  + "    <tr>\n"
                  + "      <td>");
          writer.write(
              widgets.select(
                  "dis" + cv,
                  "<div class=\"narrow_max_width\">"
                      + EDStatic.messages.get(Message.EDD_TABLE_SELECT_CONSTRAINT_TOOLTIP, language)
                      + "</div>",
                  1,
                  whichSV < 0 ? new String[] {""} : distinctOptions[whichSV],
                  whichSV < 0
                      ? 0
                      : Math.max(
                          0,
                          String2.indexOf(
                              distinctOptions[whichSV], XML.minimalEncodeSpaces(tConVala))),
                  // INACTIVE. 2 lines above were:
                  //  new String[]{""}, 0, //initially, just the 0="" element to hold place
                  // onChange pushes selected value to textField above
                  "id=\"dis"
                      + cv
                      + "\" onChange='"
                      + " document."
                      + formName
                      + ".cVal"
                      + cv
                      + "a.value=this.options[this.selectedIndex].text;"
                      + "'"));
          // not encodeSpaces=true here because all distinctOptions[] were encoded above

          /* INACTIVE. I used to load the distinctOptions below. But it is much faster to just load the select normally.
              (test with cPostSurg3, unique_tag_id)
          if (whichSV >= 0) {
              //populate dis here (instead of above, so info only transmitted once)
              onloadSB.append(
                  "\n" +
                  " cDis=document." + formName + ".dis" + cv + ";" + //as few lookups as possible in loop
                  " cDisOptions=cDis.options;" +                     //as few lookups as possible in loop
                  " dosv=distinctOptions[" + whichSV + "];" +        //as few lookups as possible in loop
                  " for (var i=1; i<" + distinctOptions[whichSV].length + "; i++)" +  //0 already done
                  "  cDisOptions.add(new Option(dosv[i]));\n" +  //[i]= is faster than add() in FF, but not sorted order!
                  " cDis.selectedIndex=" + (Math.max(0, String2.indexOf(distinctOptions[whichSV], tConVala))) + ";" + //Math.max causes not found -> 0=""
                  " document.getElementById(\"disRow" + cv + "\").style.display=\"\";");  //show the row
          } */
          // write buttons
          writer.write(
              "      </td>\n"
                  + "      <td class=\"B\">\n"
                  + // vertical-align: 'b'ottom
                  "<img class=\"B\" src=\""
                  + widgets.imageDirUrl
                  + "minus.gif\" "
                  + // vertical-align: 'b'ottom
                  widgets.completeTooltip(
                      EDStatic.messages.get(Message.MAG_ITEM_PREVIOUS, language))
                  + "  alt=\"-\"\n"
                  +
                  // onMouseUp works much better than onClick and onDblClick
                  "  onMouseUp=\"\n"
                  + "   var sel=document."
                  + formName
                  + ".dis"
                  + cv
                  + ";\n"
                  + "   if (sel.selectedIndex>1) {\n"
                  + // don't go to item 0 (it would lose the variable selection)
                  "    document."
                  + formName
                  + ".cVal"
                  + cv
                  + "a.value=sel.options[--sel.selectedIndex].text;\n"
                  + "    mySubmit(true);\n"
                  + "   }\" >\n"
                  + "      </td>\n"
                  + "      <td class=\"B\">\n"
                  + // vertical-align: 'b'ottom
                  "<img class=\"B\" src=\""
                  + widgets.imageDirUrl
                  + "plus.gif\" "
                  + // vertical-align: 'b'ottom
                  widgets.completeTooltip(EDStatic.messages.get(Message.MAG_ITEM_NEXT, language))
                  + "  alt=\"+\"\n"
                  +
                  // onMouseUp works much better than onClick and onDblClick
                  "  onMouseUp=\"\n"
                  + "   var sel=document."
                  + formName
                  + ".dis"
                  + cv
                  + ";\n"
                  + "   if (sel.selectedIndex<sel.length-1) {\n"
                  + // no action if at last item
                  "    document."
                  + formName
                  + ".cVal"
                  + cv
                  + "a.value=sel.options[++sel.selectedIndex].text;\n"
                  + "    mySubmit(true);\n"
                  + "   }\" >\n"
                  + "      </td>\n"
                  + "    </tr>\n"
                  + "    "
                  + widgets.endTable()
                  + "  </td>\n"
                  + "</tr>\n");
        }
      }
      if (debugMode) String2.log("respondToGraphQuery 6");

      // end of constraint table
      writer.write(widgets.endTable());

      // INACTIVE. I used to load the distinctOptions here.
      // But it is much faster to just load the select normally.
      //  (test with cPostSurg3, unique_tag_id)
      // write out the onload code
      // this way, loading options is unobtrusive
      //  (try with cPostSurg3 unique_tag_id as a constraint)
      /*if (onloadSB.length() > 0) {
          writer.write(
              "<script>\n" +
              "window.onload=function(){\n" +
              " document.body.style.cursor=\"wait\";" + //no effect in Firefox
              " var cDis, cDisOptions, dosv;\n");
          writer.write(
              onloadSB.toString());
          writer.write(
              " document.body.style.cursor=\"default\";" + //no effect in Firefox
              "}\n" +
              "</script>\n");
      }*/

      // *** function options
      int nOrderByComboBox = 4; // to be narrower than Data Access Form
      writeFunctionHtml(
          request, language, loggedInAs, queryParts, writer, widgets, formName, nOrderByComboBox);
      if (String2.indexOf(queryParts, "distinct()") >= 0) {
        graphQuery.append("&distinct()");
        graphQueryNoLatLon.append("&distinct()");
        graphQueryNoTime.append("&distinct()");
      }
      // find first orderBy in queryParts
      for (int i = 1; i < orderByOptions.size(); i++) { // 1 since [0]=""
        String start = orderByOptions.get(i) + "(\"";
        String obPart = String2.stringStartsWith(queryParts, start);
        if (obPart == null || !obPart.endsWith("\")")) continue;
        StringArray names =
            StringArray.fromCSV(obPart.substring(start.length(), obPart.length() - 2));
        StringArray goodNames = new StringArray();
        boolean usesExtra = orderByExtraDefaults.get(i).length() > 0; // e.g., orderByLimit
        int nNames = names.size() - (usesExtra ? 1 : 0); // if uses extra, don't check last item
        if (nNames < minOrderByVariables.get(i)) continue; // too few items in csv
        for (int n = 0; n < nNames; n++) {
          String tName = names.get(n);
          int divPo = tName.indexOf('/');
          if (divPo > 0) tName = tName.substring(0, divPo);
          if (String2.indexOf(dataVariableDestinationNames, tName) < 0) continue;
          goodNames.add(names.get(n));
          if (resultsVariables.indexOf(tName) < 0) resultsVariables.add(tName);
        }
        if (goodNames.size() >= minOrderByVariables.get(i)) {
          // success
          String tq =
              "&"
                  + SSR.minimalPercentEncode(
                      start
                          + String2.replaceAll(goodNames.toString(), " ", "")
                          + (usesExtra
                              ? (goodNames.size() > 0 ? "," : "") + names.get(names.size() - 1)
                              : "")
                          + "\")");
          graphQuery.append(tq);
          graphQueryNoLatLon.append(tq);
          graphQueryNoTime.append(tq);
          break; // don't look for another orderBy
        }
      }

      // add resultsVariables to graphQuery
      while (true) {
        int blank = resultsVariables.indexOf("");
        if (blank < 0) break;
        resultsVariables.remove(blank);
      }
      graphQuery.insert(0, String2.replaceAll(resultsVariables.toString(), " ", ""));
      graphQueryNoLatLon.insert(0, String2.replaceAll(resultsVariables.toString(), " ", ""));
      graphQueryNoTime.insert(0, String2.replaceAll(resultsVariables.toString(), " ", ""));

      // *** Graph Settings
      // add draw to graphQuery
      graphQuery.append("&.draw=" + draws[draw]);
      graphQueryNoLatLon.append("&.draw=" + draws[draw]);
      graphQueryNoTime.append("&.draw=" + draws[draw]);

      writer.write("&nbsp;\n"); // necessary for the blank line before the table (not <p>)
      writer.write(widgets.beginTable("class=\"compact nowrap\""));
      writer.write(
          "  <tr><th class=\"L\" colspan=\"6\">"
              + EDStatic.messages.get(Message.MAG_GS, language)
              + "</th></tr>\n");

      // get Marker settings
      int mType = -1, mSize = -1;
      String mSizes[] = {
        "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18"
      };
      if (drawLinesAndMarkers || drawMarkers) {
        partValue = String2.stringStartsWith(queryParts, partName = ".marker=");
        if (partValue != null) {
          pParts = String2.split(partValue.substring(partName.length()), '|');
          if (pParts.length > 0) mType = String2.parseInt(pParts[0]);
          if (pParts.length > 1) mSize = String2.parseInt(pParts[1]); // the literal, not the index
          if (reallyVerbose) String2.log("  .marker type=" + mType + " size=" + mSize);
        }

        // markerType
        paramName = "mType";
        if (mType < 0 || mType >= GraphDataLayer.MARKER_TYPES.size())
          mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE;
        // if (varName[2].length() > 0 &&
        //    GraphDataLayer.MARKER_TYPES[mType].toLowerCase().indexOf("filled") < 0)
        //    mType = GraphDataLayer.MARKER_TYPE_FILLED_SQUARE; //needs "filled" marker type
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.get(Message.MAG_GS_MARKER_TYPE, language)
                + ":&nbsp;</td>\n"
                + "    <td>"
                + widgets.select(paramName, "", 1, GraphDataLayer.MARKER_TYPES, mType, "")
                + "</td>\n");

        // markerSize
        paramName = "mSize";
        mSize =
            String2.indexOf(mSizes, "" + mSize); // convert from literal 3.. to index in mSizes[0..]
        if (mSize < 0) mSize = String2.indexOf(mSizes, "" + GraphDataLayer.MARKER_SIZE_SMALL);
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.get(Message.MAG_GS_SIZE, language)
                + ":&nbsp;</td>\n"
                + "    <td>"
                + widgets.select(paramName, "", 1, mSizes, mSize, "")
                + "</td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "  </tr>\n");

        // add to graphQuery
        String tq = "&.marker=" + SSR.minimalPercentEncode(mType + "|" + mSizes[mSize]);
        graphQuery.append(tq);
        graphQueryNoLatLon.append(tq);
        graphQueryNoTime.append(tq);
      }
      if (debugMode) String2.log("respondToGraphQuery 7");

      // color
      paramName = "colr";
      partValue = String2.stringStartsWith(queryParts, partName = ".color=0x");
      int colori =
          HtmlWidgets.PALETTE17.indexOf(
              partValue == null ? "" : partValue.substring(partName.length()));
      if (colori < 0) colori = HtmlWidgets.PALETTE17.indexOf("000000");
      writer.write(
          "  <tr>\n"
              + "    <td>"
              + EDStatic.messages.get(Message.MAG_GS_COLOR, language)
              + ":&nbsp;</td>\n"
              + "    <td colspan=\"5\">"
              + widgets.color17("", paramName, "", colori, "")
              + "</td>\n"
              + "  </tr>\n");

      // add to graphQuery
      graphQuery.append("&.color=0x" + HtmlWidgets.PALETTE17.get(colori));
      graphQueryNoLatLon.append("&.color=0x" + HtmlWidgets.PALETTE17.get(colori));
      graphQueryNoTime.append("&.color=0x" + HtmlWidgets.PALETTE17.get(colori));

      // color bar
      int palette = 0, continuous = 0, scale = 0, pSections = 0;
      String palMin = "", palMax = "";
      if (drawLinesAndMarkers || drawMarkers) {
        partValue = String2.stringStartsWith(queryParts, partName = ".colorBar=");
        pParts =
            partValue == null
                ? new String[0]
                : String2.split(partValue.substring(partName.length()), '|');
        if (reallyVerbose) String2.log("  .colorBar=" + String2.toCSSVString(pParts));

        paramName = "p";
        String defaultPalette = "";
        palette =
            Math.max(
                0,
                String2.indexOf(
                    EDStatic.messages.palettes0, pParts.length > 0 ? pParts[0] : defaultPalette));
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.get(Message.MAG_GS_COLOR_BAR, language)
                + ":&nbsp;</td>"
                + "    <td>"
                + widgets.select(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_COLOR_BAR_TOOLTIP, language),
                    1,
                    EDStatic.messages.palettes0,
                    palette,
                    "")
                + "</td>\n");

        String conDisAr[] = new String[] {"", "Continuous", "Discrete"};
        paramName = "pc";
        continuous =
            pParts.length > 1 ? (pParts[1].equals("D") ? 2 : pParts[1].equals("C") ? 1 : 0) : 0;
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.get(Message.MAG_GS_CONTINUITY, language)
                + ":&nbsp;</td>"
                + "    <td>"
                + widgets.select(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_CONTINUITY_TOOLTIP, language),
                    1,
                    conDisAr,
                    continuous,
                    "")
                + "</td>\n");

        paramName = "ps";
        String defaultScale = "";
        scale =
            Math.max(0, EDV.VALID_SCALES0.indexOf(pParts.length > 2 ? pParts[2] : defaultScale));
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.get(Message.MAG_GS_SCALE, language)
                + ":&nbsp;</td>"
                + "    <td>"
                + widgets.select(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_SCALE_TOOLTIP, language),
                    1,
                    EDV.VALID_SCALES0,
                    scale,
                    "")
                + "</td>\n"
                + "  </tr>\n");

        // new row
        paramName = "pMin";
        String defaultMin = "";
        palMin = pParts.length > 3 ? pParts[3] : defaultMin;
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + gap
                + EDStatic.messages.get(Message.MAG_GS_MIN, language)
                + ":&nbsp;</td>"
                + "    <td>"
                + widgets.textField(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_MIN_TOOLTIP, language),
                    10,
                    60,
                    palMin,
                    "")
                + "</td>\n");

        paramName = "pMax";
        String defaultMax = "";
        palMax = pParts.length > 4 ? pParts[4] : defaultMax;
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.get(Message.MAG_GS_MAX, language)
                + ":&nbsp;</td>"
                + "    <td>"
                + widgets.textField(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_MAX_TOOLTIP, language),
                    10,
                    60,
                    palMax,
                    "")
                + "</td>\n");

        paramName = "pSec";
        pSections =
            Math.max(0, EDStatic.paletteSections.indexOf(pParts.length > 5 ? pParts[5] : ""));
        writer.write(
            "    <td>&nbsp;"
                + EDStatic.messages.get(Message.MAG_GS_N_SECTIONS, language)
                + ":&nbsp;</td>"
                + "    <td>"
                + widgets.select(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_N_SECTIONS_TOOLTIP, language),
                    1,
                    EDStatic.paletteSections,
                    pSections,
                    "")
                + "</td>\n"
                + "  </tr>\n");

        // add to graphQuery
        String tq =
            "&.colorBar="
                + SSR.minimalPercentEncode(
                    EDStatic.messages.palettes0[palette]
                        + "|"
                        + (conDisAr[continuous].length() == 0 ? "" : conDisAr[continuous].charAt(0))
                        + "|"
                        + EDV.VALID_SCALES0.get(scale)
                        + "|"
                        + palMin
                        + "|"
                        + palMax
                        + "|"
                        + EDStatic.paletteSections.get(pSections));
        graphQuery.append(tq);
        graphQueryNoLatLon.append(tq);
        graphQueryNoTime.append(tq);
      }

      // currentDrawLandMask/drawLandMask?
      boolean showLandOption =
          resultsVariables.size() >= 2
              && resultsVariables.get(0).equals(EDV.LON_NAME)
              && resultsVariables.get(1).equals(EDV.LAT_NAME);
      if (showLandOption) {
        // Draw Land
        int tLand = 0; // don't translate
        partValue = String2.stringStartsWith(queryParts, partName = ".land=");
        if (partValue != null) {
          partValue = partValue.substring(6);
          tLand = Math.max(0, SgtMap.drawLandMask_OPTIONS.indexOf(partValue));
        }
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.get(Message.MAG_GS_LAND_MASK, language)
                + ":&nbsp;</td>\n"
                + "    <td>"
                + widgets.select(
                    "land",
                    EDStatic.messages.get(Message.MAG_GS_LAND_MASK_TOOLTIP_TABLE, language),
                    1,
                    SgtMap.drawLandMask_OPTIONS,
                    tLand,
                    "")
                + "</td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "  </tr>\n");

        // add to graphQuery
        if (tLand > 0) {
          String tq = "&.land=" + SgtMap.drawLandMask_OPTIONS.get(tLand);
          graphQuery.append(tq);
          graphQueryNoLatLon.append(tq);
          graphQueryNoTime.append(tq);
        }
      } else {
        partValue = String2.stringStartsWith(queryParts, partName = ".land=");
        if (partValue != null) {
          // pass it through
          graphQuery.append(partValue);
          graphQueryNoLatLon.append(partValue);
          graphQueryNoTime.append(partValue);
        }
      }

      // Vector Standard
      String vec = "";
      if (drawVectors) {
        paramName = "vec";
        vec = String2.stringStartsWith(queryParts, partName = ".vec=");
        vec = vec == null ? "" : vec.substring(partName.length());
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.get(Message.MAG_GS_VECTOR_STANDARD, language)
                + ":&nbsp;</td>\n"
                + "    <td>"
                + widgets.textField(
                    paramName,
                    EDStatic.messages.get(Message.MAG_GS_VECTOR_STANDARD_TOOLTIP, language),
                    10,
                    30,
                    vec,
                    "")
                + "</td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "    <td></td>\n"
                + "  </tr>\n");

        // add to graphQuery
        if (vec.length() > 0) {
          graphQuery.append("&.vec=" + vec);
          graphQueryNoLatLon.append("&.vec=" + vec);
          graphQueryNoTime.append("&.vec=" + vec);
        }
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
      String bgcs = "&.bgColor=" + String2.to0xHexString(bgColor.getRGB(), 8);
      graphQuery.append(bgcs);
      graphQueryNoLatLon.append(bgcs);
      graphQueryNoTime.append(bgcs);

      // yRange
      String yRange[] = new String[] {"", ""};
      boolean yAscending = true;
      String yAxisScale = "";
      if (true) {
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
            // if the param slot is there, the param determines Linear/Log
            // if the param slot is there, the param determines ""/Linear/Log
            yAxisScale = String2.toTitleCase(tyRangeAr[3].trim());
            if (!yAxisScale.equals("Linear") && !yAxisScale.equals("Log")) yAxisScale = "";
          }
        }
        writer.write(
            "  <tr>\n"
                + "    <td>"
                + EDStatic.messages.get(Message.MAG_GS_Y_AXIS_MIN, language)
                + ":&nbsp;</td>\n"
                + "    <td>"
                + widgets.textField(
                    "yRangeMin",
                    "<div class=\"narrow_max_width\">"
                        + EDStatic.messages.get(Message.MAG_GS_Y_RANGE_MIN_TOOLTIP, language)
                        + EDStatic.messages.get(Message.MAG_GS_Y_RANGE_TOOLTIP, language)
                        + "</div>",
                    10,
                    30,
                    yRange[0],
                    "")
                + "</td>\n"
                + "    <td>&nbsp;"
                + EDStatic.messages.get(Message.MAG_GS_Y_AXIS_MAX, language)
                + ":&nbsp;</td>\n"
                + "    <td>"
                + widgets.textField(
                    "yRangeMax",
                    "<div class=\"narrow_max_width\">"
                        + EDStatic.messages.get(Message.MAG_GS_Y_RANGE_MAX_TOOLTIP, language)
                        + EDStatic.messages.get(Message.MAG_GS_Y_RANGE_TOOLTIP, language)
                        + "</div>",
                    10,
                    30,
                    yRange[1],
                    "")
                + "</td>\n"
                + "    <td>&nbsp;"
                + widgets.select(
                    "yRangeAscending",
                    EDStatic.messages.get(Message.MAG_GS_Y_ASCENDING_TOOLTIP, language),
                    1,
                    new String[] {"Ascending", "Descending"},
                    yAscending ? 0 : 1,
                    "")
                + "</td>\n"
                + "    <td>"
                + widgets.select(
                    "yScale",
                    EDStatic.messages.get(Message.MAG_GS_Y_SCALE_TOOLTIP, language),
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
            || yAxisScale.length() > 0) {
          String ts =
              "&.yRange="
                  + SSR.minimalPercentEncode(
                      yRange[0] + "|" + yRange[1] + "|" + yAscending + "|" + yAxisScale);
          graphQuery.append(ts);
          graphQueryNoLatLon.append(ts);
          graphQueryNoTime.append(ts);
        }
      }

      // end of graph settings table
      writer.write(widgets.endTable());

      // make javascript function to generate query   \\x26=&  %7C=|
      // rv holds resultVariables list
      // q2 holds &queries
      writer.write(
          HtmlWidgets.PERCENT_ENCODE_JS
              + "<script> \n"
              + "function makeQuery(varsToo) { \n"
              + "  try { \n"
              + "    var d = document; \n"
              + "    var rv = [];\n"
              + // list of results variables
              "    var tVar, c1 = \"\", c2 = \"\", q2 = \"\"; \n"
              + // constraint 1|2, constraints string
              "    if (varsToo) { \n");
      // vars
      for (int v = 0; v < nVars; v++) {
        writer.write(
            "      tVar = d.f1.var"
                + v
                + ".options[d.f1.var"
                + v
                + ".selectedIndex].text; \n"
                + "      if (rv.indexOf(tVar) < 0) rv.push(tVar);\n");
      }
      writer.write("    } \n");
      // constraints
      for (int v = 0; v < nConRows; v++) {
        writer.write(
            "    tVar = d.f1.cVar"
                + v
                + ".options[d.f1.cVar"
                + v
                + ".selectedIndex].text; \n"
                + "    if (tVar.length > 0) { \n"
                + "      c1 = d.f1.cVal"
                + v
                + "a.value; \n"
                + "      c2 = d.f1.cVal"
                + v
                + "b.value; \n"
                + "      if (c1.length > 0) q2 += \"\\x26\" + tVar + \n"
                + "        d.f1.cOp"
                + v
                + "a.options[d.f1.cOp"
                + v
                + "a.selectedIndex].value + percentEncode(c1); \n"
                + "      if (c2.length > 0) q2 += \"\\x26\" + tVar + \n"
                + "        d.f1.cOp"
                + v
                + "b.options[d.f1.cOp"
                + v
                + "b.selectedIndex].value + percentEncode(c2); \n"
                + "    } \n");
      }

      // functions
      writer.write(functionJavaScript(formName, nOrderByComboBox));

      // graph settings
      writer.write(
          "    q2 += \"\\x26.draw=\" + d.f1.draw.options[d.f1.draw.selectedIndex].text; \n");
      if (drawLinesAndMarkers || drawMarkers)
        writer.write(
            """
                            q2 += "\\x26.marker=" + d.f1.mType.selectedIndex + "%7C" +\s
                              d.f1.mSize.options[d.f1.mSize.selectedIndex].text;\s
                        """);
      writer.write(
          "    q2 += \"\\x26.color=0x\"; \n"
              + "    for (var rb = 0; rb < "
              + HtmlWidgets.PALETTE17.size()
              + "; rb++) \n"
              + "      if (d.f1.colr[rb].checked) q2 += d.f1.colr[rb].value; \n"); // always: one
      // will be
      // checked
      if (drawLinesAndMarkers || drawMarkers)
        writer.write(
            """
                            var tpc = d.f1.pc.options[d.f1.pc.selectedIndex].text;
                            q2 += "\\x26.colorBar=" + d.f1.p.options[d.f1.p.selectedIndex].text + "%7C" +\s
                              (tpc.length > 0? tpc.charAt(0) : "") + "%7C" +\s
                              d.f1.ps.options[d.f1.ps.selectedIndex].text + "%7C" +\s
                              d.f1.pMin.value + "%7C" + d.f1.pMax.value + "%7C" +\s
                              d.f1.pSec.options[d.f1.pSec.selectedIndex].text;\s
                        """);
      if (drawVectors)
        writer.write(
            "    if (d.f1.vec.value.length > 0) q2 += \"\\x26.vec=\" + d.f1.vec.value; \n");
      if (showLandOption)
        writer.write(
            """
                            if (d.f1.land.selectedIndex > 0)
                              q2 += "\\x26.land=" + d.f1.land.options[d.f1.land.selectedIndex].text;\s
                        """);
      if (true)
        writer.write(
            """
                            var yRMin=d.f1.yRangeMin.value;\s
                            var yRMax=d.f1.yRangeMax.value;\s
                            var yRAsc=d.f1.yRangeAscending.selectedIndex;\s
                            var yScl =d.f1.yScale.options[d.f1.yScale.selectedIndex].text;\s
                            if (yRMin.length > 0 || yRMax.length > 0 || yRAsc == 1 || yScl.length > 0)
                              q2 += "\\x26.yRange=" + yRMin + "%7C" + yRMax + "%7C" + (yRAsc==0) + "%7C" + yScl;\s
                        """);
      // always add .bgColor to graphQuery so this ERDDAP's setting overwrites remote ERDDAP's
      writer.write(
          "    q2 += \"&.bgColor=" + String2.to0xHexString(bgColor.getRGB(), 8) + "\"; \n");
      writer.write(
          "    return percentEncode(rv.toString()) + q2; \n"
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
              + "/tabledap/"
              + datasetID
              + ".graph?\" + q;\n"
              + "} \n"
              + "</script> \n");

      // submit
      writer.write("&nbsp;\n"); // necessary for the blank line between the table (not <p>)
      writer.write(widgets.beginTable("class=\"compact\""));
      writer.write("<tr><td>");
      writer.write(
          widgets.htmlButton(
              "button",
              "",
              "",
              EDStatic.messages.get(Message.MAG_REDRAW_TOOLTIP, language),
              "<span style=\"font-size:large;\"><strong>"
                  + EDStatic.messages.get(Message.MAG_REDRAW, language)
                  + "</strong></span>",
              "onMouseUp='mySubmit(true);'"));
      writer.write(
          " " + EDStatic.messages.get(Message.PATIENT_DATA, language) + "\n" + "</td></tr>\n");

      // Download the Data
      writer.write(
          "<tr><td>&nbsp;<br>"
              + EDStatic.messages.get(Message.OPTIONAL, language)
              + ":"
              + "<br>"
              + EDStatic.messages.get(Message.MAG_FILE_TYPE, language)
              + ":\n");
      paramName = "fType";
      String htmlEncodedGraphQuery = XML.encodeAsHTMLAttribute(graphQuery.toString());
      boolean tAccessibleTo = isAccessibleTo(EDStatic.getRoles(loggedInAs));
      List<String> fileTypeOptions =
          tAccessibleTo
              ? EDD_FILE_TYPE_INFO.values().stream()
                  .filter(fileTypeInfo -> fileTypeInfo.getAvailableTable())
                  .map(fileTypeInfo -> fileTypeInfo.getFileTypeName())
                  .toList()
              : publicGraphFileTypeNames;
      int defaultIndex =
          fileTypeOptions.indexOf(
              tAccessibleTo ? defaultFileTypeOption : defaultPublicGraphFileTypeOption);
      writer.write(
          widgets.select(
              paramName,
              EDStatic.messages.get(Message.EDD_SELECT_FILE_TYPE, language),
              1,
              fileTypeOptions,
              defaultIndex,
              "onChange='document.f1.tUrl.value=\""
                  + tErddapUrl
                  + "/tabledap/"
                  + datasetID
                  + "\" + document.f1.fType.options[document.f1.fType.selectedIndex].text + "
                  + "\"?"
                  + htmlEncodedGraphQuery
                  + "\";'"));
      writer.write(
          " (<a rel=\"help\" href=\""
              + tErddapUrl
              + "/tabledap/documentation.html#fileType\">"
              + EDStatic.messages.get(Message.EDD_FILE_TYPE_INFORMATION, language)
              + "</a>)\n");
      writer.write("<br>and\n");
      writer.write(
          widgets.button(
              "button",
              "",
              EDStatic.messages.get(Message.MAG_DOWNLOAD_TOOLTIP, language)
                  + "<br>"
                  + EDStatic.messages.get(Message.PATIENT_DATA, language),
              EDStatic.messages.get(Message.MAG_DOWNLOAD, language),
              // "class=\"skinny\" " + //only IE needs it but only IE ignores it
              "onMouseUp='window.location=\""
                  + tErddapUrl
                  + "/tabledap/"
                  + datasetID
                  + "\" + document.f1."
                  + paramName
                  + ".options[document.f1."
                  + paramName
                  + ".selectedIndex].text + \"?"
                  + htmlEncodedGraphQuery
                  + "\";'")); // or open a new window: window.open(result);\n" +
      writer.write("</td></tr>\n");

      // view the url
      writer.write("<tr><td>" + EDStatic.messages.get(Message.MAG_VIEW_URL, language) + ":\n");
      String genViewHtml =
          String2.replaceAll(
              "<div class=\"standard_max_width\">"
                  + EDStatic.messages.get(
                      Message.JUST_GENERATE_AND_VIEW_GRAPH_URL_TOOLTIP, language)
                  + "</div>",
              "&protocolName;",
              dapProtocol);
      writer.write(
          widgets.textField(
              "tUrl",
              genViewHtml,
              60,
              1000,
              tErddapUrl
                  + "/tabledap/"
                  + datasetID
                  + (tAccessibleTo ? defaultFileTypeOption : defaultPublicGraphFileTypeOption)
                  + "?"
                  + graphQuery.toString(),
              ""));
      writer.write(
          "<br>(<a rel=\"help\" href=\""
              + tErddapUrl
              + "/tabledap/documentation.html\" "
              + "title=\"tabledap documentation\">"
              + EDStatic.messages.get(Message.MAG_DOCUMENTATION, language)
              + "</a>\n"
              + EDStatic.htmlTooltipImage(request, language, loggedInAs, genViewHtml)
              + ")\n");

      if (debugMode) String2.log("respondToGraphQuery 8");
      writer.write("""
              </td></tr>
              </table>

              """);

      // end form
      writer.write(widgets.endForm());

      // end of left half of big table
      writer.write(
          "</td>\n"
              + "<td>"
              + gap
              + "</td>\n"
              + // gap in center
              "<td class=\"L T\">\n"); // begin right half of screen

      // *** zoomLatLon stuff
      if (zoomLatLon) {

        String gqz =
            "onMouseUp='window.location=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".graph?"
                + htmlEncodedGraphQuery
                + "&amp;.zoom=";
        String gqnll =
            "onMouseUp='window.location=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".graph?"
                + XML.encodeAsHTMLAttribute(graphQueryNoLatLon.toString());

        writer.write(
            EDStatic.messages.get(Message.MAG_ZOOM_CENTER, language)
                + "\n"
                + EDStatic.htmlTooltipImage(
                    request,
                    language,
                    loggedInAs,
                    EDStatic.messages.get(Message.MAG_ZOOM_CENTER_TOOLTIP, language))
                + "<br><strong>"
                + EDStatic.messages.get(Message.MAG_ZOOM, language)
                + ":&nbsp;</strong>\n");

        // zoom out?
        boolean disableZoomOut =
            !Double.isNaN(lonMin)
                && !Double.isNaN(lonMax)
                && !Double.isNaN(latMin)
                && !Double.isNaN(latMax)
                && lonMin <= maxExtentWESN[0]
                && lonMax >= maxExtentWESN[1]
                && latMin <= maxExtentWESN[2]
                && latMax >= maxExtentWESN[3];

        // if zoom out, keep ranges intact by moving tCenter to safe place
        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_OUT_TOOLTIP, language), "8x"),
                MessageFormat.format(EDStatic.messages.get(Message.MAG_ZOOM_OUT, language), "8x"),
                "class=\"skinny\" " + (disableZoomOut ? "disabled" : gqz + "out8\";'")));

        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_OUT_TOOLTIP, language), "2x"),
                MessageFormat.format(EDStatic.messages.get(Message.MAG_ZOOM_OUT, language), "2x"),
                "class=\"skinny\" " + (disableZoomOut ? "disabled" : gqz + "out2\";'")));

        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_OUT_TOOLTIP, language),
                    EDStatic.messages.get(Message.MAG_ZOOM_A_LITTLE, language)),
                MessageFormat.format(EDStatic.messages.get(Message.MAG_ZOOM_OUT, language), "")
                    .trim(),
                "class=\"skinny\" " + (disableZoomOut ? "disabled" : gqz + "out\";'")));

        // zoom data
        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_OUT_TOOLTIP, language),
                    EDStatic.messages.get(Message.MAG_ZOOM_OUT_DATA, language)),
                EDStatic.messages.get(Message.MAG_ZOOM_DATA, language),
                "class=\"skinny\" " + gqnll + "\";'"));

        // zoom in
        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_IN_TOOLTIP, language),
                    EDStatic.messages.get(Message.MAG_ZOOM_A_LITTLE, language)),
                MessageFormat.format(EDStatic.messages.get(Message.MAG_ZOOM_IN, language), "")
                    .trim(),
                "class=\"skinny\" " + gqz + "in\";'"));

        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_IN_TOOLTIP, language), "2x"),
                MessageFormat.format(EDStatic.messages.get(Message.MAG_ZOOM_IN, language), "2x"),
                "class=\"skinny\" " + gqz + "in2\";'"));

        writer.write(
            widgets.button(
                "button",
                "",
                MessageFormat.format(
                    EDStatic.messages.get(Message.MAG_ZOOM_IN_TOOLTIP, language), "8x"),
                MessageFormat.format(EDStatic.messages.get(Message.MAG_ZOOM_IN, language), "8x"),
                "class=\"skinny\" " + gqz + "in8\";'"));

        // trailing <br>
        writer.write("<br>");
      }

      // *** zoomX
      boolean zoomX =
          !zoomLatLon
              && zoomXEdv != null
              && !(zoomXEdv instanceof EDVTimeStamp)
              && zoomXCv >= 0
              && Double.isFinite(zoomXMin)
              && Double.isFinite(zoomXMax)
              && zoomXMin < zoomXMax; // already has > and < constraints
      boolean zoomTime = timeIndex >= 0 && !Double.isNaN(timeMin) && !Double.isNaN(timeMax);

      // make controls table?
      if (zoomX || zoomTime) writer.write("<table class=\"compact\">");

      if (zoomX) {

        // does var have known min and max?
        double zoomXEdvMin = zoomXEdv.destinationMinDouble(); // may be NaN
        double zoomXEdvMax = zoomXEdv.destinationMaxDouble();
        double zoomXEdvRange = zoomXEdvMax - zoomXEdvMin; // may be NaN
        double zoomXCenter = (zoomXMin + zoomXMax) / 2;
        double zoomXRange = zoomXMax - zoomXMin;
        double zoomXRange2 = zoomXRange / 2;
        double zoomXRange4 = zoomXRange / 4;
        String formTextField = formName + ".cVal" + zoomXCv; // + 'a' or 'b'
        String tGap = "&nbsp;&nbsp;&nbsp;&nbsp;";

        // beginning of row in controls table
        writer.write("<tr><td><strong>X range:</strong>&nbsp;</td>\n" + "<td>");

        // zoom in
        if (zoomXRange > 1e-10) {
          writer.write(
              widgets.htmlButton(
                  "button",
                  "zoomXzoomIn",
                  "", // value
                  EDStatic.messages.get(Message.ZOOM_IN, language),
                  EDStatic.messages.get(Message.ZOOM_IN, language), // tooltip, labelHtml
                  "onMouseUp='"
                      + formTextField
                      + "a.value=\""
                      + (zoomXCenter - zoomXRange4)
                      + "\"; "
                      + formTextField
                      + "b.value=\""
                      + (zoomXCenter + zoomXRange4)
                      + "\"; "
                      + "mySubmit(true);'"));
        } else {
          writer.write(tGap + tGap + tGap);
        }

        // zoom out
        {
          writer.write(
              widgets.htmlButton(
                      "button",
                      "zoomXzoomOut",
                      "", // value
                      EDStatic.messages.get(Message.ZOOM_OUT, language),
                      EDStatic.messages.get(Message.ZOOM_OUT, language), // tooltip, labelHtml
                      "onMouseUp='"
                          + formTextField
                          + "a.value=\""
                          + (zoomXCenter - zoomXRange)
                          + "\"; "
                          + formTextField
                          + "b.value=\""
                          + (zoomXCenter + zoomXRange)
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");
        }
        writer.write("</td>\n" + "<td>");

        if (Double.isFinite(zoomXEdvRange)) {
          // all the way left
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowLL.gif",
                      "|<",
                      EDStatic.messages.get(Message.SHIFT_X_ALL_THE_WAY_LEFT, language),
                      "class=\"B\" "
                          + // vertical-align: 'b'ottom
                          "onMouseUp='"
                          + formTextField
                          + "a.value=\""
                          + zoomXEdvMin
                          + "\"; "
                          + formTextField
                          + "b.value=\""
                          + (zoomXEdvMin + Math.min(zoomXEdvRange, zoomXRange))
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");
        } else {
          writer.write(tGap);
        }
        writer.write("</td>\n" + "<td>");

        // left
        writer.write(
            HtmlWidgets.htmlTooltipImage(
                    EDStatic.imageDirUrl(request, loggedInAs, language) + "minus.gif",
                    "-",
                    EDStatic.messages.get(Message.SHIFT_X_LEFT, language),
                    "class=\"B\" "
                        + // vertical-align: 'b'ottom
                        "onMouseUp='"
                        + formTextField
                        + "a.value=\""
                        + (zoomXMin - zoomXRange2)
                        + "\"; "
                        + formTextField
                        + "b.value=\""
                        + (zoomXMax - zoomXRange2)
                        + "\"; "
                        + "mySubmit(true);'")
                + "&nbsp;");
        writer.write("</td>\n" + "<td>");

        // right
        writer.write(
            HtmlWidgets.htmlTooltipImage(
                    EDStatic.imageDirUrl(request, loggedInAs, language) + "plus.gif",
                    "+",
                    EDStatic.messages.get(Message.SHIFT_X_RIGHT, language),
                    "class=\"B\" "
                        + // vertical-align: 'b'ottom
                        "onMouseUp='"
                        + formTextField
                        + "a.value=\""
                        + (zoomXMin + zoomXRange2)
                        + "\"; "
                        + formTextField
                        + "b.value=\""
                        + (zoomXMax + zoomXRange2)
                        + "\"; "
                        + "mySubmit(true);'")
                + "&nbsp;");
        writer.write("</td>\n" + "<td>");

        if (Double.isFinite(zoomXEdvRange)) {
          // all the way right
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowRR.gif",
                      ">|",
                      EDStatic.messages.get(Message.SHIFT_X_ALL_THE_WAY_RIGHT, language),
                      "class=\"B\" "
                          + // vertical-align: 'b'ottom
                          "onMouseUp='"
                          + formTextField
                          + "a.value=\""
                          + (zoomXEdvMax - Math.min(zoomXEdvRange, zoomXRange))
                          + "\"; "
                          + formTextField
                          + "b.value=\""
                          + zoomXEdvMax
                          + "\"; "
                          + "mySubmit(true);'")
                  + "&nbsp;");

        } else {
          // writer.write(tGap);  //no need
        }

        // end of row in tiny table
        writer.write("</td></tr>");
      }

      // *** zoomTime stuff
      if (zoomTime) {

        EDVTimeStamp edvTime = (EDVTimeStamp) dataVariables[timeIndex];
        double edvTimeMin = edvTime.destinationMinDouble(); // may be NaN
        double edvTimeMax = edvTime.destinationMaxDouble(); // may be NaN
        String tTime_precision = edvTime.time_precision();
        if (!Double.isNaN(edvTimeMin) && Double.isNaN(edvTimeMax))
          edvTimeMax = Calendar2.backNDays(-1, edvTimeMax);

        // timeMin and timeMax are not NaN
        double timeRange = timeMax - timeMin;
        if (reallyVerbose)
          String2.log(
              "zoomTime timeMin="
                  + Calendar2.epochSecondsToIsoStringTZ(timeMin)
                  + "  timeMax="
                  + Calendar2.epochSecondsToIsoStringTZ(timeMax)
                  + "\n  range="
                  + Calendar2.elapsedTimeString(timeRange * 1000)
                  + "\n  edvTimeMin="
                  + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, edvTimeMin, "NaN")
                  + "  max="
                  + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, edvTimeMax, "NaN"));

        // set idealTimeN (1..100), idealTimeUnits
        int idealTimeN = -1; // 1..100
        int idealTimeUnits = -1;
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
        // if (reallyVerbose)
        //    String2.log("  idealTimeN=" + idealTimeN + " units=" + idealTimeUnits);
        if (idealTimeN < 1 || idealTimeN > 100 || idealTimeUnits < 0) {

          idealTimeUnits = Calendar2.IDEAL_UNITS_OPTIONS.size() - 1;
          while (idealTimeUnits > 0
              && timeRange < Calendar2.IDEAL_UNITS_SECONDS.get(idealTimeUnits)) {
            idealTimeUnits--;
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

        // make idealized minTime
        GregorianCalendar idMinGc = Calendar2.roundToIdealGC(timeMin, idealTimeN, idealTimeUnits);
        GregorianCalendar idMaxGc = Calendar2.newGCalendarZulu(idMinGc.getTimeInMillis());
        idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);

        String gqnt =
            "window.location=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".graph?"
                + XML.encodeAsHTML(graphQueryNoTime.toString());

        // beginning of row in controls table
        writer.write(
            "<tr><td><strong>"
                + EDStatic.messages.get(Message.MAG_TIME_RANGE, language)
                + "</strong>&nbsp;</td>\n"
                + "<td>");

        String timeRangeString =
            idealTimeN + " " + Calendar2.IDEAL_UNITS_OPTIONS.get(idealTimeUnits);
        String timeRangeParam =
            "&amp;.timeRange="
                + idealTimeN
                + ","
                + Calendar2.IDEAL_UNITS_OPTIONS.get(idealTimeUnits);

        // n = 1..100
        writer.write(
            widgets.select(
                "timeN", // keep using widgets even though this isn't part of f1 form
                EDStatic.messages.get(Message.MAG_TIME_RANGE_TOOLTIP, language),
                1,
                Calendar2.IDEAL_N_OPTIONS,
                idealTimeN - 1, // -1 so index=0 .. 99
                "onChange='"
                    + gqnt
                    + "&amp;time%3E="
                    + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMin, "")
                    + "&amp;time%3C"
                    + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMax, "")
                    + "&amp;.timeRange=\" + this.options[this.selectedIndex].text + \","
                    + Calendar2.IDEAL_UNITS_OPTIONS.get(idealTimeUnits)
                    + "\";'"));

        // timeUnits
        writer.write(
            widgets.select(
                "timeUnits", // keep using widgets even though this isn't part of f1 form
                EDStatic.messages.get(Message.MAG_TIME_RANGE_TOOLTIP, language),
                1,
                Calendar2.IDEAL_UNITS_OPTIONS,
                idealTimeUnits,
                "onChange='"
                    + gqnt
                    + "&amp;time%3E="
                    + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMin, "")
                    + "&amp;time%3C"
                    + Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, timeMax, "")
                    + "&amp;.timeRange="
                    + idealTimeN
                    + ",\" + "
                    + "this.options[this.selectedIndex].text;'"));
        writer.write("&nbsp;</td>\n" + "<td>");

        // beginning time
        if (!Double.isNaN(edvTimeMin)) {

          // make idealized beginning time
          GregorianCalendar tidMinGc =
              Calendar2.roundToIdealGC(edvTimeMin, idealTimeN, idealTimeUnits);
          // if it rounded to later time period, shift to earlier time period
          if (Math2.divideNoRemainder(tidMinGc.getTimeInMillis(), 1000) > edvTimeMin)
            tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
          GregorianCalendar tidMaxGc = Calendar2.newGCalendarZulu(tidMinGc.getTimeInMillis());
          tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);

          // always show button if idealTime is different from current selection
          double idRange =
              Math2.divideNoRemainder(
                  tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis(), 1000);
          double ratio = (timeMax - timeMin) / idRange;

          if (timeMin > edvTimeMin || ratio < 0.99 || ratio > 1.01) {
            writer.write(
                HtmlWidgets.htmlTooltipImage(
                        EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowLL.gif",
                        "|<",
                        MessageFormat.format(
                            EDStatic.messages.get(Message.MAG_TIME_RANGE_FIRST, language),
                            timeRangeString),
                        "class=\"B\" onMouseUp='"
                            + gqnt
                            + // vertical-align: 'b'ottom
                            "&amp;time%3E="
                            + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMinGc)
                            + "&amp;time%3C"
                            + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMaxGc)
                            + timeRangeParam
                            + "\";'")
                    + "&nbsp;");
          } else {
            writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
          }
        } else {
          writer.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        writer.write("</td>\n" + "<td>");

        // time to left
        if (Double.isNaN(edvTimeMin) || (!Double.isNaN(edvTimeMin) && timeMin > edvTimeMin)) {

          // idealized (rounded) time shift
          idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
          idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "minus.gif",
                      "-",
                      MessageFormat.format(
                          EDStatic.messages.get(Message.MAG_TIME_RANGE_BACK, language),
                          timeRangeString),
                      "class=\"B\" onMouseUp='"
                          + gqnt
                          + // vertical-align: 'b'ottom
                          "&amp;time%3E="
                          + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMinGc)
                          + "&amp;time%3C"
                          + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMaxGc)
                          + timeRangeParam
                          + "\";'")
                  + "&nbsp;");
          idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
          idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);

        } else {
          writer.write("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        writer.write("</td>\n" + "<td>");

        // time to right
        if (Double.isNaN(edvTimeMax) || (!Double.isNaN(edvTimeMax) && timeMax < edvTimeMax)) {

          // idealized (rounded) time shift
          idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
          idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
          String equals =
              (!Double.isNaN(edvTimeMax) && idMaxGc.getTimeInMillis() >= edvTimeMax * 1000)
                  ? "="
                  : "";
          writer.write(
              HtmlWidgets.htmlTooltipImage(
                      EDStatic.imageDirUrl(request, loggedInAs, language) + "plus.gif",
                      "+",
                      MessageFormat.format(
                          EDStatic.messages.get(Message.MAG_TIME_RANGE_FORWARD, language),
                          timeRangeString),
                      "class=\"B\" onMouseUp='"
                          + gqnt
                          + // vertical-align: 'b'ottom
                          "&amp;time%3E="
                          + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMinGc)
                          + "&amp;time%3C"
                          + equals
                          + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, idMaxGc)
                          + timeRangeParam
                          + "\";'")
                  + "&nbsp;");
          idMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);
          idMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);

        } else {
          writer.write("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
        writer.write("</td>\n" + "<td>");

        // end time
        if (!Double.isNaN(edvTimeMax)) {
          // make idealized end time
          GregorianCalendar tidMaxGc =
              Calendar2.roundToIdealGC(edvTimeMax, idealTimeN, idealTimeUnits);
          // if it rounded to earlier time period, shift to later time period
          if (Math2.divideNoRemainder(tidMaxGc.getTimeInMillis(), 1000) < edvTimeMax)
            tidMaxGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), idealTimeN);
          GregorianCalendar tidMinGc = Calendar2.newGCalendarZulu(tidMaxGc.getTimeInMillis());
          tidMinGc.add(Calendar2.IDEAL_UNITS_FIELD.get(idealTimeUnits), -idealTimeN);

          // always show button if idealTime is different from current selection
          double idRange =
              Math2.divideNoRemainder(
                  tidMaxGc.getTimeInMillis() - tidMinGc.getTimeInMillis(), 1000);
          double ratio = (timeMax - timeMin) / idRange;

          if (timeMax < edvTimeMax || ratio < 0.99 || ratio > 1.01) {
            writer.write(
                HtmlWidgets.htmlTooltipImage(
                    EDStatic.imageDirUrl(request, loggedInAs, language) + "arrowRR.gif",
                    ">|",
                    MessageFormat.format(
                        EDStatic.messages.get(Message.MAG_TIME_RANGE_LAST, language),
                        timeRangeString),
                    "class=\"B\" onMouseUp='"
                        + gqnt
                        + // vertical-align: 'b'ottom
                        "&amp;time%3E="
                        + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMinGc)
                        + "&amp;time%3C="
                        + Calendar2.limitedFormatAsISODateTimeT(tTime_precision, tidMaxGc)
                        + // yes, =
                        timeRangeParam
                        + "\";'")
                // + "&nbsp;" //not needed
                );
          } else {
            // writer.write("&nbsp;&nbsp;&nbsp;&nbsp;"); //not needed
          }
        } else {
          // writer.write("&nbsp;&nbsp;&nbsp;&nbsp;"); //not needed
        }

        // end of row in tiny table
        writer.write("</td></tr>");
      }
      if (zoomX || zoomTime) writer.write("</table>\n");

      // display the graph
      if (reallyVerbose) String2.log("  graphQuery=" + graphQuery.toString());
      if (zoomLatLon)
        writer.write(
            "<a href=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".graph?"
                + htmlEncodedGraphQuery
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
              + EDStatic.messages.get(Message.PATIENT_YOUR_GRAPH, language)
              + "\" "
              + "src=\""
              + tErddapUrl
              + "/tabledap/"
              + // don't use \n for the following lines
              datasetID
              + ".png?"
              + htmlEncodedGraphQuery
              + "\">"); // no img end tag
      if (zoomLatLon) writer.write("</a>");

      // *** end of right half of big table
      writer.write("\n</td></tr></table>\n");
      writer.flush();

      // *** stuff at end of page

      // do things with graphs
      writer.write(
          String2.replaceAll(
              MessageFormat.format(
                  EDStatic.messages.get(Message.DO_WITH_GRAPHS, language), tErddapUrl),
              "&erddapUrl;",
              tErddapUrl));
      writer.write("\n\n");

      // *** .das
      writer.write(
          "<hr>\n"
              + "<h2><a class=\"selfLink\" id=\"DAS\" href=\"#DAS\" rel=\"bookmark\">"
              + EDStatic.messages.get(Message.DAS_TITLE, language)
              + "</a></h2>\n"
              + "<pre style=\"white-space:pre-wrap;\">\n");
      Table table =
          makeEmptyDestinationTable(
              language, "/tabledap/" + datasetID + ".das", "", true); // as if no constraints
      table.writeDAS(
          writer, SEQUENCE_NAME, true); // useful so search engines find all relevant words
      writer.write("</pre>\n");

      // then write DAP instructions
      writer.write("""
              <br>&nbsp;
              <hr>
              """);
      writeGeneralDapHtmlInstructions(language, tErddapUrl, writer, false);

      writer.write("</div>\n");

      writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
      writer.write("\n</html>\n");
      writer.flush(); // essential
      // *** end of document

    } catch (Exception e) {
      EDStatic.rethrowClientAbortException(e); // first thing in catch{}
      writer.write(EDStatic.htmlForException(language, e));
      writer.write("</div>\n");
      writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
      writer.write("\n</html>\n");
      writer.flush(); // essential
      throw e;
    }
  }

  /**
   * This suggests nPlaces precision for a given lat lon radius (used in Make A Graph).
   *
   * @param radius
   * @return nPlaces precision for a given lat lon radius (used in Make A Graph).
   */
  public static int lonLatNPlaces(double radius) {
    return radius > 100 ? 0 : radius > 10 ? 1 : radius > 1 ? 2 : radius > 0.1 ? 3 : 4;
  }

  /** This looks for var,op in constraintVariable,constraintOp (or returns -1). */
  protected static int indexOf(
      StringArray constraintVariables, StringArray constraintOps, String var, String op) {
    int n = constraintVariables.size();
    for (int i = 0; i < n; i++) {
      if (constraintVariables.get(i).equals(var) && constraintOps.get(i).equals(op)) return i;
    }
    return -1;
  }

  /**
   * This returns the name of the file in datasetDir() which has all of the distinct data
   * combinations for the current subsetVariables. The file is deleted by setSubsetVariablesCSV(),
   * which is called whenever the dataset is reloaded. See also EDDTable.subsetVariables(), which
   * deletes these files.
   *
   * @param loggedInAs POST datasets overwrite this method and use loggedInAs since data for each
   *     loggedInAs is different; others don't use this
   * @throws Exception if trouble
   */
  public String subsetVariablesFileName(String loggedInAs) throws Exception {
    return SUBSET_FILENAME;
  }

  /**
   * This returns the name of the file in datasetDir() which has just the distinct values for the
   * current subsetVariables. The file is deleted by setSubsetVariablesCSV(), which is called
   * whenever the dataset is reloaded. See also EDDTable, which deletes these files.
   *
   * @param loggedInAs POST datasets overwrite this method and use loggedInAs since data for each
   *     loggedInAs is different; others don't use this
   * @throws Exception if trouble
   */
  public String distinctSubsetVariablesFileName(String loggedInAs) throws Exception {
    return DISTINCT_SUBSET_FILENAME;
  }

  /**
   * Generate the Subset Variables subsetVariables .subset HTML form.
   *
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in)
   * @param userQuery post "?", still percentEncoded, may be null.
   * @throws Throwable if trouble
   */
  public void respondToSubsetQuery(
      int language,
      HttpServletRequest request,
      HttpServletResponse response,
      String loggedInAs,
      String requestUrl,
      String endOfRequest,
      String userQuery,
      OutputStreamSource outputStreamSource,
      String dir,
      String fileName,
      String fileTypeName)
      throws Throwable {

    // BOB: this is similar to Erddap.doPostSubset()
    // Here, time is like a String variable (not reduced to Year)

    // constants
    String ANY = "(ANY)";
    boolean fixedLLRange = false; // true until 2011-02-08

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String subsetUrl = tErddapUrl + "/tabledap/" + datasetID + ".subset";
    userQuery =
        userQuery == null
            ? ""
            : String2.replaceAll(userQuery, "|", "%7C"); // crude extra percentEncode insurance
    String tNewHistory = getNewHistory(language, requestUrl, userQuery);

    // !!!Important: this use of subsetVariables with () and accessibleViaSubset()
    // insures that subsetVariables has been created.
    if (subsetVariables().length == 0 || accessibleViaSubset().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaSubset());
    String subsetVariablesCSV = String2.toSVString(subsetVariables, ",", false);

    // if present, the lastP parameter is not used for bigTable, but is used for smallTable
    String queryParts[] =
        Table.getDapQueryParts(userQuery); // decoded.  userQuery="" returns String[1]  with #0=""
    String start = ".last=";
    String val = String2.stringStartsWith(queryParts, start);
    if (val != null) val = val.substring(start.length());
    int lastP = String2.indexOf(subsetVariables, val); // pName of last changed select option

    // parse the userQuery to find the param specified for each subsetVariable
    //  with the required String for each subsetVariable (or null)
    String param[] = new String[subsetVariables.length];
    String firstNumericSubsetVar = String2.indexOf(subsetVariables, "time") >= 0 ? "time" : null;
    for (int p = 0; p < subsetVariables.length; p++) {
      String pName = subsetVariables[p];
      start = pName + "="; // this won't catch lonLatConstraints like longitude>=123
      param[p] =
          String2.stringStartsWith(queryParts, start); // may be null, ...="ANY", or ...="aString"
      if (param[p] != null) param[p] = param[p].substring(start.length());
      // if last selection was ANY, last is irrelevant
      if (param[p] == null || param[p].equals(ANY) || param[p].equals("\"" + ANY + "\"")) {
        param[p] = null;
      } else if (param[p].length() >= 2
          && param[p].charAt(0) == '"'
          && param[p].charAt(param[p].length() - 1) == '"') {
        // remove begin/end quotes
        param[p] = param[p].substring(1, param[p].length() - 1);
      }
      if (p == lastP && param[p] == null) lastP = -1;

      // firstNumericSubsetVar
      if (firstNumericSubsetVar == null
          && !pName.equals("longitude")
          && !pName.equals("latitude")) {
        EDV edv = findVariableByDestinationName(pName);
        if (edv.destinationDataPAType() != PAType.STRING) firstNumericSubsetVar = pName;
      }
    }

    // firstNumericVar (not necessarily in subsetVariables)
    String firstNumericVar =
        String2.indexOf(dataVariableDestinationNames(), "time") >= 0 ? "time" : null;
    if (firstNumericVar == null) {
      for (EDV edv : dataVariables) {
        if (edv.destinationName().equals("longitude")
            || edv.destinationName().equals("latitude")
            || edv.destinationDataPAType() == PAType.STRING) continue;
        firstNumericSubsetVar = edv.destinationName();
        break;
      }
    }

    // make/read all of the subsetVariable data
    Table subsetTable = subsetVariablesDataTable(language, loggedInAs);

    // if either map is possible, make consistent lonLatConstraints (specifies map extent)
    boolean distinctMapIsPossible =
        String2.indexOf(subsetVariables, "longitude") >= 0
            && String2.indexOf(subsetVariables, "latitude") >= 0;
    boolean relatedMapIsPossible = !distinctMapIsPossible && lonIndex >= 0 && latIndex >= 0;
    String lonLatConstraints = ""; // (specifies map extent)
    if (distinctMapIsPossible && fixedLLRange) {
      // Don't get raw lon lat from variable's destinationMin/Max.
      // It isn't very reliable.

      // get min/max lon lat from subsetTable
      PrimitiveArray pa = subsetTable.findColumn("longitude");
      double stats[] = pa.calculateStats();
      double minLon = stats[PrimitiveArray.STATS_MIN];
      double maxLon = stats[PrimitiveArray.STATS_MAX];

      pa = subsetTable.findColumn("latitude");
      stats = pa.calculateStats();
      double minLat = stats[PrimitiveArray.STATS_MIN];
      double maxLat = stats[PrimitiveArray.STATS_MAX];
      if (reallyVerbose)
        String2.log(
            "  minMaxLonLat from bigTable: minLon="
                + minLon
                + " maxLon="
                + maxLon
                + " minLat="
                + minLat
                + " maxLat="
                + maxLat);

      // calculate lonLatConstraints
      double minMaxLon[] = Math2.suggestLowHigh(minLon, maxLon);
      double minMaxLat[] = Math2.suggestLowHigh(minLat, maxLat);
      if (minLon >= -180) minMaxLon[0] = Math.max(-180, minMaxLon[0]);
      if (maxLon <= 180) minMaxLon[1] = Math.min(180, minMaxLon[1]);
      if (maxLon <= 360) minMaxLon[1] = Math.min(360, minMaxLon[1]);
      double lonCenter = (minMaxLon[0] + minMaxLon[1]) / 2;
      double latCenter = (minMaxLat[0] + minMaxLat[1]) / 2;
      double range =
          Math.max(
              minMaxLon[1] - minMaxLon[0],
              Math.max(1, minMaxLat[1] - minMaxLat[0])); // at least 1 degree
      minLon = lonCenter - range / 2;
      maxLon = lonCenter + range / 2;
      minLat = Math.max(-90, latCenter - range / 2);
      maxLat = Math.min(90, latCenter + range / 2);
      lonLatConstraints =
          "&longitude%3E="
              + minLon
              + // no need to round, they should be round numbers already from suggestLowHigh
              "&longitude%3C="
              + maxLon
              + "&latitude%3E="
              + minLat
              + "&latitude%3C="
              + maxLat;
    }

    // reduce subsetTable to "bigTable"  (as if lastP param was set to ANY)
    int nRows = subsetTable.nRows();
    BitSet keep = new BitSet(nRows);
    keep.set(0, nRows); // set all to true
    for (int p = 0; p < subsetVariables.length; p++) {
      String tParam = param[p];
      if (tParam == null || p == lastP) // don't include lastP param in bigTable
      continue;

      EDV edv = findDataVariableByDestinationName(subsetVariables[p]);
      EDVTimeStamp edvTimeStamp = edv instanceof EDVTimeStamp t ? t : null;
      String tTime_precision = edvTimeStamp == null ? null : edvTimeStamp.time_precision();
      PrimitiveArray pa = subsetTable.findColumn(subsetVariables[p]);
      if (edvTimeStamp == null && !(pa instanceof StringArray) && tParam.equals("NaN"))
        tParam = ""; // e.g., doubleArray.getString() for NaN returns ""
      for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
        String value =
            edvTimeStamp == null
                ? pa.getString(row)
                : Calendar2.epochSecondsToLimitedIsoStringT(
                    tTime_precision, pa.getDouble(row), "NaN");
        keep.set(row, tParam.equals(value)); // tParam isn't null; pa.getString might be
      }
    }
    subsetTable.justKeep(keep);
    nRows = subsetTable.nRows(); // valid params should always yield at least 1, but don't sometimes
    if (reallyVerbose) String2.log("  bigTable nRows=" + nRows);

    // save lastP column  in a different PrimitiveArray
    PrimitiveArray lastPPA =
        lastP < 0 ? null : (PrimitiveArray) subsetTable.findColumn(subsetVariables[lastP]).clone();

    // reduce subsetTable to "smallTable" (using lastP param to reduce the table size)
    if (lastP >= 0) {
      String tParam = param[lastP];
      keep = new BitSet(nRows);
      keep.set(0, nRows); // set all to true

      EDV edv = findDataVariableByDestinationName(subsetVariables[lastP]);
      EDVTimeStamp edvTimeStamp = edv instanceof EDVTimeStamp ts ? ts : null;
      String tTime_precision = edvTimeStamp == null ? null : edvTimeStamp.time_precision();

      PrimitiveArray pa = subsetTable.findColumn(subsetVariables[lastP]);
      if (edvTimeStamp == null && !(pa instanceof StringArray) && tParam.equals("NaN"))
        tParam = ""; // e.g., doubleArray.getString() for NaN returns ""
      for (int row = keep.nextSetBit(0); row >= 0; row = keep.nextSetBit(row + 1)) {
        String value =
            edvTimeStamp == null
                ? pa.getString(row)
                : Calendar2.epochSecondsToLimitedIsoStringT(
                    tTime_precision, pa.getDouble(row), "NaN");
        // if (value.startsWith(" ")) String2.log("value=\"" + value + "\"");
        keep.set(row, tParam.equals(value)); // tParam isn't null; pa.getString might be
      }
      subsetTable.justKeep(keep);
      nRows =
          subsetTable.nRows(); // valid params should always yield at least 1, but don't sometimes
      if (reallyVerbose)
        String2.log(
            "  smallTable " + subsetVariables[lastP] + "=\"" + tParam + "\" nRows=" + nRows);
    }

    String clickPart =
        String2.stringStartsWith(queryParts, ".click=?"); // browser added '?' when user clicked
    boolean userClicked = clickPart != null;
    boolean lonAndLatSpecified =
        String2.stringStartsWith(queryParts, "longitude=") != null
            && String2.stringStartsWith(queryParts, "latitude=") != null;
    if (userClicked) {
      // lonAndLatSpecified may or may not have been specified
      try {
        // &.view marks end of graphDapQuery  (at least viewDistinctMap is true)
        // see graphDapQuery and url referenced in <a href><img></a> for view[0] below
        int tpo = userQuery.indexOf("&.view");
        Test.ensureTrue(tpo >= 0, "\"&.view\" wasn't found in the userQuery.");
        String imageQuery = userQuery.substring(0, tpo);

        // extract the &.view info from userQuery
        String viewQuery = userQuery.substring(tpo);
        tpo = viewQuery.indexOf("&.click="); // end of &.view...= info
        Test.ensureTrue(tpo >= 0, "\"&.click=\" wasn't found at end of viewQuery=" + viewQuery);
        viewQuery = viewQuery.substring(0, tpo);

        // get pngInfo -- it should exist since user clicked on the image
        Object pngInfo[] = readPngInfo(loggedInAs, imageQuery, ".png");
        if (pngInfo == null) {
          String2.log(
              "Unexpected: userClicked, but no .pngInfo for tabledap/"
                  + datasetID
                  + ".png?"
                  + imageQuery);
          userClicked = false;
        } else {
          double pngInfoDoubleWESN[] = (double[]) pngInfo[0];
          int pngInfoIntWESN[] = (int[]) pngInfo[1];

          // generate a no Lat Lon Query
          String noLLQuery = userQuery;
          // remove var name list from beginning
          tpo = noLLQuery.indexOf("&");
          if (tpo >= 0) // it should be
          noLLQuery = noLLQuery.substring(tpo);
          if (fixedLLRange) {
            // remove lonLatConstraints
            // (longitude will appear before lat in the query -- search for it)
            tpo = noLLQuery.indexOf("&longitude>=");
            if (tpo < 0) tpo = noLLQuery.indexOf("&longitude%3E=");
            if (tpo >= 0) // should be
            noLLQuery = noLLQuery.substring(0, tpo); // delete everything after it
            else if (verbose)
              String2.log("\"&longitude>%3E=\" not found in noLLQuery=" + noLLQuery);
          } else {
            // no LonLatConstraint? search for &distinct()
            tpo = noLLQuery.indexOf("&distinct()");
            if (tpo < 0) tpo = noLLQuery.indexOf("&distinct%28%29");
            if (tpo >= 0) {
              noLLQuery = noLLQuery.substring(0, tpo); // delete everything after it
            }
          }
          // and search for lon=
          tpo = noLLQuery.indexOf("&longitude=");
          if (tpo >= 0) {
            int tpo2 = noLLQuery.indexOf('&', tpo + 1);
            noLLQuery = noLLQuery.substring(0, tpo) + (tpo2 > 0 ? noLLQuery.substring(tpo2) : "");
          }
          // and search for lat=
          tpo = noLLQuery.indexOf("&latitude=");
          if (tpo >= 0) {
            int tpo2 = noLLQuery.indexOf('&', tpo + 1);
            noLLQuery = noLLQuery.substring(0, tpo) + (tpo2 > 0 ? noLLQuery.substring(tpo2) : "");
          }

          // convert userClick x,y to clickLonLat
          String xy[] = String2.split(clickPart.substring(8), ','); // e.g., 24,116
          int cx = String2.parseInt(xy[0]);
          int cy = String2.parseInt(xy[1]);
          if (lonAndLatSpecified) {
            if (cx < pngInfoIntWESN[0]
                || cx > pngInfoIntWESN[1]
                || cy > pngInfoIntWESN[2]
                || cy < pngInfoIntWESN[3]) {

              // reset lon and lat to ANY
              noLLQuery =
                  subsetVariablesCSV
                      + noLLQuery
                      + // already percent encoded
                      viewQuery
                      + "&distinct()";

              if (verbose)
                String2.log(
                    "userClicked outside graph, so redirecting to " + subsetUrl + "?" + noLLQuery);
              response.sendRedirect(subsetUrl + "?" + noLLQuery);
              return;
            }

          } else { // !lonAndLatSpecified
            double clickLonLat[] =
                SgtUtil.xyToLonLat(
                    cx,
                    cy,
                    pngInfoIntWESN,
                    pngInfoDoubleWESN,
                    pngInfoDoubleWESN); // use pngInfoDoubleWESN for maxExtentWESN (insist on click
            // within visible map)
            Test.ensureTrue(clickLonLat != null, "clickLonLat is null!");
            if (verbose) String2.log("clickLon=" + clickLonLat[0] + " lat=" + clickLonLat[1]);

            // find closestLon and closestLat
            PrimitiveArray lonPa = subsetTable.findColumn("longitude");
            PrimitiveArray latPa = subsetTable.findColumn("latitude");
            int tn = lonPa.size();
            String closestLon = null, closestLat = null;
            double minDist2 = Double.MAX_VALUE;
            for (int row = 0; row < tn; row++) {
              double tLon = lonPa.getDouble(row);
              double tLat = latPa.getDouble(row);
              if (!Double.isNaN(tLon) && !Double.isNaN(tLat)) {
                double dist2 =
                    Math.pow(tLon - clickLonLat[0], 2) + Math.pow(tLat - clickLonLat[1], 2);
                if (dist2 <= minDist2) {
                  minDist2 = dist2;
                  closestLon = lonPa.getString(row);
                  closestLat = latPa.getString(row);
                }
              }
            }
            Test.ensureTrue(closestLon != null, "closestLon=null.  No valid lon, lat?");

            String newQuery =
                subsetVariablesCSV
                    + "&longitude="
                    + closestLon
                    + "&latitude="
                    + closestLat
                    + noLLQuery
                    + // already percent encoded
                    viewQuery
                    + "&distinct()";

            if (verbose)
              String2.log("userClicked, so redirecting to " + subsetUrl + "?" + newQuery);
            response.sendRedirect(subsetUrl + "?" + newQuery);
            return;
          }
        }

      } catch (Exception e) {
        String2.log(
            String2.ERROR + " while decoding \"&.click=\":\n" + MustBe.throwableToString(e));
        userClicked = false;
      }
    }

    // show the .html response/form
    HtmlWidgets widgets =
        new HtmlWidgets(
            true, EDStatic.imageDirUrl(request, loggedInAs, language)); // true=htmlTooltips
    widgets.enterTextSubmitsForm = true;
    OutputStream out = outputStreamSource.outputStream(File2.UTF_8);
    Writer writer = File2.getBufferedWriterUtf8(out);
    try {
      writer.write(
          EDStatic.startHeadHtml(
              language,
              tErddapUrl,
              title(language) + " - " + EDStatic.messages.get(Message.SUBSET, language)));
      writer.write("\n" + rssHeadLink(language));
      writer.write("\n</head>\n");
      writer.write(
          EDStatic.startBodyHtml(
              request,
              language,
              loggedInAs,
              "tabledap/" + datasetID + ".subset", // was endOfRequest,
              userQuery));
      writer.write("\n");
      writer.write("<div class=\"standard_width\">\n");
      writer.write(
          HtmlWidgets.htmlTooltipScript(
              EDStatic.imageDirUrl(request, loggedInAs, language))); // this is a link to a script
      writer.flush(); // Steve Souder says: the sooner you can send some html to user, the better
      writer.write(
          EDStatic.youAreHereWithHelp(
              request,
              language,
              loggedInAs,
              dapProtocol,
              EDStatic.messages.get(Message.SUBSET, language),
              "<div class=\"narrow_max_width\">"
                  + EDStatic.messages.get(Message.SUBSET_TOOLTIP, language)
                  + "</div>"));

      StringBuilder diQuery = new StringBuilder();
      for (int qpi = 1; qpi < queryParts.length; qpi++) { // 1 because part0 is var names
        String qp = queryParts[qpi];
        if (!qp.startsWith(".")
            && // lose e.g., &.viewDistinct...
            !qp.equals("distinct()")) { // &distinct() is misleading/trouble on MAG
          // keep it
          int epo = qp.indexOf('=');
          if (epo >= 0)
            qp = qp.substring(0, epo + 1) + SSR.minimalPercentEncode(qp.substring(epo + 1));
          else qp = SSR.minimalPercentEncode(qp);
          diQuery.append("&" + qp);
        }
      }
      writeHtmlDatasetInfo(
          request, language, loggedInAs, writer, false, true, true, true, diQuery.toString(), "");
      writer.write(HtmlWidgets.ifJavaScriptDisabled);

      // if noData/invalid request, tell user and reset all
      if (nRows == 0) {
        // notably, this happens if a selected value has a leading or trailing space
        // because the <option> on the html form trims the values when submitting the form (or
        // before).
        // but trying to fix this 2012-04-18
        String2.log(
            "WARNING: EDDTable.respondToSubsetQuery found no matching data for\n"
                + requestUrl
                + "?"
                + userQuery);

        // error message?
        writer.write(
            "<span class=\"warningColor\">"
                + MustBe.THERE_IS_NO_DATA
                + " "
                + EDStatic.messages.get(Message.RESET_THE_FORM_WAS, language)
                + "</span>\n");

        // reset all
        Arrays.fill(param, ANY);
        subsetTable = subsetVariablesDataTable(language, loggedInAs); // reload all subset data
        lastP = -1;
        lastPPA = null;
      }
      String lastPName = lastP < 0 ? null : subsetVariables[lastP];

      // Select a subset
      writer.write(
          widgets.beginForm("f1", "GET", subsetUrl, "")
              + "\n"
              + "<p><strong>"
              + EDStatic.messages.get(Message.SUBSET_SELECT, language)
              + ":</strong>\n"
              + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class=\"subduedColor\">"
              + "("
              + MessageFormat.format(
                  EDStatic.messages.get(Message.SUBSET_N_MATCHING, language),
                  "" + subsetTable.nRows())
              + ")</span>\n"
              + "  <br>"
              + EDStatic.messages.get(Message.SUBSET_INSTRUCTIONS, language)
              + "\n"
              + widgets.beginTable("class=\"compact nowrap\""));

      StringBuilder countsVariables = new StringBuilder();
      StringBuilder newConstraintsNoLL = new StringBuilder();
      StringBuilder newConstraints = new StringBuilder(); // for url
      StringBuilder newConstraintsNLP = new StringBuilder(); // for url, w/out lastP variable
      StringBuilder newConstraintsNLPHuman = new StringBuilder(); // for human consumption
      boolean allData = true;
      for (int p = 0; p < subsetVariables.length; p++) {
        String pName = subsetVariables[p];
        EDV edv = findDataVariableByDestinationName(pName);
        EDVTimeStamp edvTimeStamp = edv instanceof EDVTimeStamp ts ? ts : null;
        String tTime_precision = edvTimeStamp == null ? null : edvTimeStamp.time_precision();

        // work on a copy
        PrimitiveArray pa =
            p == lastP
                ? (PrimitiveArray) lastPPA.clone()
                : (PrimitiveArray) subsetTable.findColumn(pName).clone();
        if (edvTimeStamp != null) {
          int paSize = pa.size();
          StringArray ta = new StringArray(paSize, false);
          for (int row = 0; row < paSize; row++)
            ta.add(
                Calendar2.epochSecondsToLimitedIsoStringT(
                    tTime_precision, pa.getDouble(row), "NaN"));
          pa = ta;
        }
        pa.sortIgnoreCase();
        // int nBefore = pa.size();
        pa.removeDuplicates();
        // String2.log("  " + pName + " nBefore=" + nBefore + " nAfter=" + pa.size());
        pa.atInsertString(
            0, ""); // place holder for ANY (but pa may be numeric so can't take it now)
        String pasa[] = pa.toStringArray();
        pasa[0] = ANY;
        // use NaN for numeric missing value
        // String2.log("pName=" + pName + " last pasa value=" + pasa[pasa.length - 1]);
        if (pa instanceof StringArray) {
          if (pasa[pasa.length - 1].length() == 0) pasa[pasa.length - 1] = "NaN";
        }
        int which = param[p] == null ? 0 : String2.indexOf(pasa, param[p]);
        if (which < 0) which = 0;
        if (which > 0) {
          allData = false;
          if (countsVariables.length() > 0) countsVariables.append(',');
          countsVariables.append(SSR.minimalPercentEncode(pName));
          if (pa instanceof StringArray || edvTimeStamp != null) {
            String tq =
                "&"
                    + SSR.minimalPercentEncode(pName)
                    + "="
                    + SSR.minimalPercentEncode("\"" + param[p] + "\"");
            newConstraints.append(tq);
            newConstraintsNoLL.append(tq);
            if (p != lastP) {
              newConstraintsNLP.append(tq);
              newConstraintsNLPHuman.append(
                  (newConstraintsNLPHuman.length() > 0 ? " and " : "")
                      + pName
                      + "=\""
                      + param[p]
                      + "\"");
            }
          } else { // numeric
            String tq = "&" + SSR.minimalPercentEncode(pName) + "=" + param[p];
            newConstraints.append(tq);
            if (!pName.equals("longitude") && !pName.equals("latitude"))
              newConstraintsNoLL.append(tq);
            if (p != lastP) {
              newConstraintsNLP.append(tq);
              newConstraintsNLPHuman.append(
                  (newConstraintsNLPHuman.length() > 0 ? " and " : "") + pName + "=" + param[p]);
            }
          }
        }

        // String2.log("pName=" + pName + " which=" + which);
        writer.write(
            "<tr>\n"
                + "  <td>&nbsp;&nbsp;&nbsp;&nbsp;"
                + pName
                + "\n"
                + EDStatic.htmlTooltipImageEDV(request, language, loggedInAs, edv)
                + "  </td>\n"
                + "  <td>\n");

        if (p == lastP)
          writer.write(
              "  "
                  + widgets.beginTable("class=\"compact nowrap\"")
                  + "\n"
                  + "  <tr>\n"
                  + "  <td>\n");

        writer.write(
            "  &nbsp;=\n"
                + widgets.select(
                    pName,
                    "",
                    1,
                    pasa,
                    which,
                    "onchange='mySubmit(\"" + pName + "\");'",
                    true)); // encodeSpaces solves the problem with consecutive internal spaces

        String tUnits = edv instanceof EDVTimeStamp ? null : edv.units();
        tUnits =
            tUnits == null
                ? ""
                : "&nbsp;<span class=\"subduedColor\">" + tUnits + "</span>&nbsp;\n";

        if (p == lastP) {
          writer.write(
              "      </td>\n"
                  + "      <td>\n"
                  + "<img class=\"B\" src=\""
                  + widgets.imageDirUrl
                  + "minus.gif\"\n"
                  + // vertical-align: 'b'ottom
                  "  "
                  + widgets.completeTooltip(
                      EDStatic.messages.get(Message.SELECT_PREVIOUS, language))
                  + "  alt=\"-\" "
                  +
                  // onMouseUp works much better than onClick and onDblClick
                  "  onMouseUp='\n"
                  + "   var sel=document.f1."
                  + pName
                  + ";\n"
                  + "   if (sel.selectedIndex>1) {\n"
                  + // 1: if go to 0=(Any), lastP is lost. Stick to individual items.
                  "    sel.selectedIndex--;\n"
                  + "    mySubmit(\""
                  + pName
                  + "\");\n"
                  + "   }' >\n"
                  + "      </td>\n"
                  + "      <td>\n"
                  + "<img class=\"B\" src=\""
                  + widgets.imageDirUrl
                  + "plus.gif\"\n"
                  + // vertical-align: 'b'ottom
                  "  "
                  + widgets.completeTooltip(EDStatic.messages.get(Message.SELECT_NEXT, language))
                  + "  alt=\"+\" "
                  +
                  // onMouseUp works much better than onClick and onDblClick
                  "  onMouseUp='\n"
                  + "   var sel=document.f1."
                  + pName
                  + ";\n"
                  + "   if (sel.selectedIndex<sel.length-1) {\n"
                  + // no action if at last item
                  "    sel.selectedIndex++;\n"
                  + "    mySubmit(\""
                  + pName
                  + "\");\n"
                  + "   }' >\n"
                  + "      </td>\n"
                  + "      <td>"
                  + tUnits
                  + "</td>\n"
                  + "    </tr>\n"
                  + "    "
                  + widgets.endTable());
        } else {
          writer.write(tUnits);
        }

        writer.write(

            // write hidden last widget?
            (p == lastP ? widgets.hidden("last", pName) : "")
                +

                // end of select td (or its table)
                "      </td>\n");

        // n options
        writer.write(
            (which == 0 || p == lastP
                    ? "  <td>&nbsp;<span class=\"subduedColor\">"
                        + (pa.size() - 1)
                        + " "
                        + (pa.size() == 2
                            ? EDStatic.messages.get(Message.SUBSET_OPTION, language)
                                + ": "
                                +
                                // encodeSpaces so only option displays nicely
                                XML.minimalEncodeSpaces(XML.encodeAsHTML(pa.getString(1)))
                            : EDStatic.messages.get(Message.SUBSET_OPTIONS, language))
                        + "</span></td>\n"
                    : "")
                + "</tr>\n");
      }

      writer.write("""
              </table>

              """);

      // View the graph and/or data?
      writer.write(
          "<p><a class=\"selfLink\" id=\"View\" href=\"#View\" rel=\"bookmark\"><strong>"
              + EDStatic.messages.get(Message.SUBSET_VIEW, language)
              + ":</strong></a>\n");
      // !!! If changing these, change DEFAULT_SUBSET_VIEWS above.
      String viewParam[] = {
        "viewDistinctMap",
        "viewRelatedMap",
        "viewDistinctDataCounts",
        "viewDistinctData",
        "viewRelatedDataCounts",
        "viewRelatedData"
      };
      String viewTitle[] = {
        EDStatic.messages.get(Message.SUBSET_VIEW_DISTINCT_MAP, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_MAP, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_DISTINCT_DATA_COUNTS, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_DISTINCT_DATA, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_DATA_COUNTS, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_DATA, language)
      };
      String viewTooltip[] = {
        EDStatic.messages.get(Message.SUBSET_VIEW_DISTINCT_MAP_TOOLTIP, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_MAP_TOOLTIP, language)
            + EDStatic.messages.get(Message.SUBSET_WARN, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_DISTINCT_DATA_COUNTS_TOOLTIP, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_DISTINCT_DATA_TOOLTIP, language)
            + EDStatic.messages.get(Message.SUBSET_WARN_10000, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_DATA_COUNTS_TOOLTIP, language)
            + EDStatic.messages.get(Message.SUBSET_WARN, language),
        EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_DATA_TOOLTIP, language)
            + EDStatic.messages.get(Message.SUBSET_WARN_10000, language)
      };

      boolean useCheckbox[] = {true, true, true, false, true, false};
      // for checkboxes, viewDefault: 0=false 1=true
      int viewDefault[] = {1, 0, 0, 1000, 0, 0};
      int viewValue[] = new int[viewParam.length]; // will be set below
      StringBuilder viewQuery = new StringBuilder();
      String intOptions[] = // 1 is not an option
          new String[] {"0", "10", "100", "1000", "10000", "100000"};

      for (int v = 0; v < viewParam.length; v++) {
        viewTooltip[v] = "<div class=\"narrow_max_width\">" + viewTooltip[v] + "</div>";
        start = "." + viewParam[v] + "="; // e.g., &.viewDistinctMap=
        if (useCheckbox[v]) {
          // checkboxes
          // some or all maps not possible
          if (v == 0 && !distinctMapIsPossible) {
            viewValue[v] = 0;
            continue; // don't even show the checkbox
          }

          if (v == 1 && !relatedMapIsPossible) {
            viewValue[v] = 0;
            continue; // don't even show the checkbox
          }

          // show the checkbox
          String part =
              (userQuery == null || userQuery.length() == 0)
                  ? start + (viewDefault[v] == 1)
                  : String2.stringStartsWith(queryParts, start);
          val = part == null ? "false" : part.substring(start.length());
          viewValue[v] = "true".equals(val) ? 1 : 0;
          writer.write(
              "&nbsp;&nbsp;&nbsp;\n"
                  + // \n allows for line break
                  "<span class=\"N\">"
                  + widgets.checkbox(
                      viewParam[v],
                      "",
                      viewValue[v] == 1,
                      "true",
                      viewTitle[v],
                      "onclick='mySubmit(null);'")
                  + // IE doesn't trigger onchange for checkbox
                  EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[v])
                  + "</span>");
          if (viewValue[v] == 1) viewQuery.append("&." + viewParam[v] + "=true");

        } else {

          // select 0/10/100/1000/10000/100000
          String part =
              (userQuery == null || userQuery.length() == 0)
                  ? null
                  : String2.stringStartsWith(queryParts, start);
          val = part == null ? null : part.substring(start.length());
          if (val != null && val.equals("true")) // from when it was a checkbox
          viewValue[v] = 1000;
          else if (val != null && val.equals("false")) // from when it was a checkbox
          viewValue[v] = 0;
          else viewValue[v] = String2.parseInt(val);
          if (String2.indexOf(intOptions, "" + viewValue[v]) < 0) viewValue[v] = viewDefault[v];
          writer.write(
              "&nbsp;&nbsp;&nbsp;\n"
                  + // \n allows for line break
                  viewTitle[v]
                  + "&nbsp;"
                  + widgets.select(
                      viewParam[v],
                      "",
                      1,
                      intOptions,
                      String2.indexOf(intOptions, "" + viewValue[v]),
                      "onchange='mySubmit(null);'")
                  + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[v]));
          viewQuery.append("&." + viewParam[v] + "=" + viewValue[v]);
        }
      }

      // mySubmit
      // (greatly reduces the length of the query -- just essential info --
      //  and makes query like normal tabledap request)
      writer.write(
          HtmlWidgets.PERCENT_ENCODE_JS
              + "<script> \n"
              + "function mySubmit(tLast) { \n"
              + "  try { \n"
              + "    var d = document; \n"
              + "    var q = \""
              + subsetVariablesCSV
              + "\"; \n"
              + "    var w; \n");
      for (int p = 0; p < subsetVariables.length; p++) {
        String pName = subsetVariables[p];
        String quotes = (subsetTable.getColumn(p) instanceof StringArray) ? "\\\"" : "";
        writer.write(
            "    w = d.f1."
                + pName
                + ".selectedIndex; \n"
                + "    if (w > 0) q += \"\\x26"
                + pName
                + "=\" + percentEncode(\""
                + quotes
                + "\" + d.f1."
                + pName
                + ".options[w].text + \""
                + quotes
                + "\"); \n");
      }
      for (int v = 0; v < viewParam.length; v++) {
        String pName = viewParam[v];
        if (useCheckbox[v]) {
          if (v == 0 && !distinctMapIsPossible) continue;
          if (v == 1 && !relatedMapIsPossible) continue;
          writer.write("    if (d.f1." + pName + ".checked) q += \"\\x26." + pName + "=true\"; \n");
        } else {
          writer.write(
              "    q += \"\\x26."
                  + pName
                  + "=\" + d.f1."
                  + pName
                  + ".options[d.f1."
                  + pName
                  + ".selectedIndex].text; \n");
        }
      }
      writer.write("    q += \"\\x26distinct()\"; \n");

      // last
      writer.write(
          "    if ((tLast == null || tLast == undefined) && d.f1.last != undefined) tLast = d.f1.last.value; \n"
              + // get from hidden widget?
              "    if (tLast != null && tLast != undefined) q += \"\\x26.last=\" + tLast; \n");

      // query must be something, else checkboxes reset to defaults
      writer.write(
          "    if (q.length == 0) q += \""
              + viewParam[0]
              + "=false\"; \n"); // javascript uses length, not length()

      // submit the query
      writer.write(
          "    window.location=\""
              + subsetUrl
              + "?\" + q;\n"
              + "  } catch (e) { \n"
              + "    alert(e); \n"
              + "    return \"\"; \n"
              + "  } \n"
              + "} \n"
              + "</script> \n");

      // endForm
      writer.write(widgets.endForm() + "\n");

      // set initial focus to lastP select widget    throws error
      if (lastP >= 0)
        writer.write("<script>document.f1." + subsetVariables[lastP] + ".focus();</script>\n");

      // *** RESULTS
      // 0 = viewDistinctMap
      int current = 0;
      if (distinctMapIsPossible) {
        // the ordering of parts of graphDapQuery is used above to decode url if userClicked (see
        // above)
        String graphDapQuery =
            "longitude,latitude"
                + (firstNumericSubsetVar == null ? "" : "," + firstNumericSubsetVar)
                + newConstraints.toString()
                + lonLatConstraints
                + // map extent, e.g., lon>= <=  lat>= <=
                "&distinct()"
                + "&.draw=markers&.marker=1%7C5&.color=0xFF9900&.colorBar=%7CD%7C%7C%7C%7C"; // |
        writer.write(
            "\n"
                + "&nbsp;<hr>\n"
                + "<a class=\"selfLink\" id=\"DistinctMap\" href=\"#DistinctMap\" rel=\"bookmark\"><strong>"
                + viewTitle[current]
                + "</strong></a>\n"
                + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[current])
                + "&nbsp;&nbsp;(<a href=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".graph?"
                + XML.encodeAsHTML(graphDapQuery)
                + "\">"
                + EDStatic.messages.get(Message.SUBSET_REFINE_MAP_DOWNLOAD, language)
                + "</a>)\n"
                + "<br>");

        if (viewValue[current] == 1) {

          if (lonAndLatSpecified)
            writer.write(
                "(<a href=\""
                    + tErddapUrl
                    + "/tabledap/"
                    + datasetID
                    + ".subset?"
                    + XML.encodeAsHTMLAttribute(
                        subsetVariablesCSV
                            + newConstraintsNoLL.toString()
                            + viewQuery.toString()
                            + // &.view marks end of graphDapQuery
                            "&distinct()")
                    + "\">"
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.SUBSET_CLICK_RESET_LL, language), ANY)
                    + "</a>)\n");
          else
            writer.write(
                EDStatic.messages.get(Message.SUBSET_CLICK_RESET_CLOSEST, language) + "\n");
          // EDStatic.htmlTooltipImage(language, loggedInAs,
          //    "Unfortunately, after you click, some data sources respond with" +
          //    "<br><kbd>" + MustBe.THERE_IS_NO_DATA + "</kbd>" +
          //    "<br>because they can't handle requests for specific longitude and" +
          //    "<br>latitude values, but there will still be useful information above."));

          writer.write(
              "<br><a href=\""
                  + subsetUrl
                  + "?"
                  + XML.encodeAsHTMLAttribute(
                      graphDapQuery
                          + // userClicked above needs to be able to extract exact same
                          // graphDapQuery
                          viewQuery.toString()
                          + // &.view marks end of graphDapQuery
                          "&.click=")
                  + "\">"
                  + // if user clicks on image, browser adds "?x,y" to url
                  "<img ismap "
                  + "width=\""
                  + EDStatic.messages.imageWidths[1]
                  + "\" height=\""
                  + EDStatic.messages.imageHeights[1]
                  + "\" "
                  + "alt=\""
                  + XML.encodeAsHTML(viewTitle[current])
                  + "\" "
                  + "src=\""
                  + XML.encodeAsHTMLAttribute(
                      tErddapUrl + "/tabledap/" + datasetID + ".png?" + graphDapQuery)
                  + "\">\n"
                  + "</a>\n");
        } else {
          writer.write(
              "<p><span class=\"subduedColor\">"
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_VIEW_CHECK, language),
                      "<kbd>"
                          + EDStatic.messages.get(Message.SUBSET_VIEW, language)
                          + " : "
                          + viewTitle[current]
                          + "</kbd>")
                  + "</span>\n");
        }
      }

      // 1 = viewRelatedMap
      current = 1;
      if (relatedMapIsPossible) {
        String graphDapQuery =
            "longitude,latitude"
                + (firstNumericVar == null ? "" : "," + firstNumericVar)
                + newConstraints.toString()
                + // ?? use lonLatConstraints?
                // not &distinct()
                "&.draw=markers&.colorBar=%7CD%7C%7C%7C%7C"; // |
        writer.write(
            "\n"
                + "&nbsp;<hr>\n"
                + "<a class=\"selfLink\" id=\"RelatedMap\" href=\"#RelatedMap\" rel=\"bookmark\"><strong>"
                + viewTitle[current]
                + "</strong></a>\n"
                + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[current])
                + "&nbsp;&nbsp;(<a href=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".graph?"
                + XML.encodeAsHTMLAttribute(graphDapQuery)
                + "\">"
                + EDStatic.messages.get(Message.SUBSET_REFINE_MAP_DOWNLOAD, language)
                + "</a>)\n");

        if (viewValue[current] == 1) {
          if (allData) {
            writer.write(
                "\n<p><span class=\"subduedColor\">"
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.SUBSET_VIEW_CHECK_1, language),
                        viewTitle[current],
                        ANY)
                    + "</span>\n");
          } else {
            writer.write(
                "<br><img width=\""
                    + EDStatic.messages.imageWidths[1]
                    + "\" height=\""
                    + EDStatic.messages.imageHeights[1]
                    + "\" "
                    + "alt=\""
                    + EDStatic.messages.get(Message.PATIENT_YOUR_GRAPH, language)
                    + "\" "
                    + "src=\""
                    + XML.encodeAsHTMLAttribute(
                        tErddapUrl + "/tabledap/" + datasetID + ".png?" + graphDapQuery)
                    + "\">\n");
          }
        } else {
          writer.write(
              "<p><span class=\"subduedColor\">"
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_VIEW_CHECK, language),
                      "<kbd>"
                          + EDStatic.messages.get(Message.SUBSET_VIEW, language)
                          + " : "
                          + viewTitle[current]
                          + "</kbd>")
                  + "</span>\n"
                  + String2.replaceAll(
                      EDStatic.messages.get(Message.SUBSET_WARN, language), "<br>", " ")
                  + "\n");
        }
      }

      // 2 = viewDistinctDataCounts
      current = 2;
      writer.write(
          "\n"
              + "<br>&nbsp;<hr>\n"
              + "<p><a class=\"selfLink\" id=\"DistinctDataCounts\" href=\"#DistinctDataCounts\" rel=\"bookmark\"><strong>"
              + viewTitle[current]
              + "</strong></a>\n"
              + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[current]));
      if (viewValue[current] == 1 && lastP >= 0 && lastPPA != null) { // lastPPA shouldn't be null
        writer.write(
            "&nbsp;&nbsp;\n"
                + "(<a href=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".html?"
                + XML.encodeAsHTMLAttribute(
                    countsVariables.toString() + newConstraintsNLP.toString() + "&distinct()")
                + "\">"
                + EDStatic.messages.get(Message.SUBSET_REFINE_SUBSET_DOWNLOAD, language)
                + "</a>)\n");

        try {
          // get the distinct data; work on a copy
          int n = lastPPA.size();
          EDV edv = findDataVariableByDestinationName(subsetVariables[lastP]);
          PrimitiveArray varPA = (PrimitiveArray) lastPPA.clone();
          Table countTable = new Table();
          countTable.addColumn(
              0, lastPName, varPA, edv.combinedAttributes().toAttributes(language));

          // sort, count, remove duplicates
          varPA.sortIgnoreCase();
          IntArray countPA = new IntArray(n, false);
          countTable.addColumn(EDStatic.messages.get(Message.SUBSET_COUNT, language), countPA);
          int lastCount = 1;
          countPA.add(lastCount);
          keep = new BitSet(n);
          keep.set(0, n); // initially, all set
          for (int i = 1; i < n; i++) {
            if (varPA.compare(i - 1, i) == 0) {
              keep.clear(i - 1);
              lastCount++;
            } else {
              lastCount = 1;
            }
            countPA.add(lastCount);
          }
          countTable.justKeep(keep);

          // calculate percents
          double stats[] = countPA.calculateStats();
          double total = stats[PrimitiveArray.STATS_SUM];
          n = countPA.size();
          DoubleArray percentPA = new DoubleArray(n, false);
          countTable.addColumn(EDStatic.messages.get(Message.SUBSET_PERCENT, language), percentPA);
          for (int i = 0; i < n; i++) percentPA.add(Math2.roundTo(countPA.get(i) * 100 / total, 2));

          // write results
          String countsCon = newConstraintsNLPHuman.toString();
          writer.write(
              "<br>"
                  + (countsCon.length() == 0
                      ? EDStatic.messages.get(Message.SUBSET_WHEN_NO_CONSTRAINTS, language)
                      : MessageFormat.format(
                          EDStatic.messages.get(Message.SUBSET_WHEN, language),
                          XML.encodeAsHTML(countsCon)))
                  + (countsCon.length() < 40 ? " " : "\n<br>")
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_WHEN_COUNTS, language),
                      EDStatic.messages.get(Message.SUBSET_VIEW_SELECT_DISTINCT_COMBOS, language),
                      "" + countTable.nRows(),
                      XML.encodeAsHTML(lastPName))
                  + "\n");
          writer.flush(); // essential, since creating and using another writer to write the
          // countTable
          TableWriterHtmlTable.writeAllAndFinish(
              request,
              language,
              this,
              tNewHistory,
              loggedInAs,
              countTable,
              endOfRequest,
              userQuery,
              new OutputStreamSourceSimple(out),
              false,
              "",
              false,
              "",
              "",
              true,
              false,
              -1);
          writer.write(
              "<p>"
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_TOTAL_COUNT, language),
                      "" + Math2.roundToLong(total))
                  + "\n");
        } catch (Throwable t) {
          EDStatic.rethrowClientAbortException(t); // first thing in catch{}
          String message = MustBe.getShortErrorMessage(t);
          String2.log(
              String2.ERROR
                  + ":\n"
                  + MustBe.throwableToString(t)); // log full message with stack trace
          writer.write(
              // "<p>An error occurred while getting the data:\n" +
              "<pre>" + XML.encodeAsPreHTML(message, 100) + "</pre>\n");
        }

      } else {
        writer.write(
            "<p><span class=\"subduedColor\">"
                + MessageFormat.format(
                    EDStatic.messages.get(Message.SUBSET_VIEW_SELECT, language),
                    EDStatic.messages.get(Message.SUBSET_VIEW_SELECT_DISTINCT_COMBOS, language),
                    EDStatic.messages.get(Message.SUBSET_VIEW, language),
                    viewTitle[current])
                + "</span>\n");
      }

      // 3 = viewDistinctData
      current = 3;
      writer.write(
          "\n"
              + "<br>&nbsp;<hr>\n"
              + "<p><a class=\"selfLink\" id=\"DistinctData\" href=\"#DistinctData\" rel=\"bookmark\"><strong>"
              + viewTitle[current]
              + "</strong></a>\n"
              + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[current])
              + "&nbsp;&nbsp;(<a href=\""
              + tErddapUrl
              + "/tabledap/"
              + datasetID
              + ".das\">"
              + EDStatic.messages.get(Message.SUBSET_METADATA, language)
              + "</a>)\n");
      writer.write(
          "&nbsp;&nbsp;\n"
              + "(<a href=\""
              + tErddapUrl
              + "/tabledap/"
              + datasetID
              + ".html?"
              + XML.encodeAsHTMLAttribute(
                  subsetVariablesCSV + newConstraints.toString() + "&distinct()")
              + "\">"
              + EDStatic.messages.get(Message.SUBSET_REFINE_SUBSET_DOWNLOAD, language)
              + "</a>)\n");
      int onRows = subsetTable.nRows();
      if (viewValue[current] > 0) {
        // this is last use of local copy of subsetTable, so okay to remove rows at end
        if (onRows > viewValue[current]) subsetTable.removeRows(viewValue[current], onRows);
        writer
            .flush(); // essential, since creating and using another writer to write the subsetTable
        TableWriterHtmlTable.writeAllAndFinish(
            request,
            language,
            this,
            tNewHistory,
            loggedInAs,
            subsetTable,
            endOfRequest,
            userQuery,
            new OutputStreamSourceSimple(out),
            false,
            "",
            false,
            "",
            "",
            true,
            true,
            -1);
      }
      writer.write(
          "<p>"
              + MessageFormat.format(
                  EDStatic.messages.get(Message.SUBSET_N_VARIABLE_COMBOS, language), "" + onRows)
              + "\n");
      if (onRows > viewValue[current]) {
        writer.write(
            "<br><span class=\"warningColor\">"
                + MessageFormat.format(
                    EDStatic.messages.get(Message.SUBSET_SHOWING_N_ROWS, language),
                    "" + Math.min(onRows, viewValue[current]),
                    "" + (onRows - viewValue[current]))
                + "</span>\n");
      } else {
        writer.write(EDStatic.messages.get(Message.SUBSET_SHOWING_ALL_ROWS, language) + "\n");
      }
      writer.write(
          "<br>"
              + MessageFormat.format(
                  EDStatic.messages.get(Message.SUBSET_CHANGE_SHOWING, language),
                  "<kbd>"
                      + EDStatic.messages.get(Message.SUBSET_VIEW, language)
                      + " : "
                      + viewTitle[current]
                      + "</kbd>")
              + "\n");

      // 4 = viewRelatedDataCounts
      current = 4;
      writer.write(
          "\n"
              + "<br>&nbsp;<hr>\n"
              + "<p><a class=\"selfLink\" id=\"RelatedDataCounts\" href=\"#RelatedDataCounts\" rel=\"bookmark\"><strong>"
              + viewTitle[current]
              + "</strong></a>\n"
              + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[current]));
      if (viewValue[current] == 1 && lastP >= 0) {
        writer.write(
            "&nbsp;&nbsp;\n"
                + "(<a href=\""
                + tErddapUrl
                + "/tabledap/"
                + datasetID
                + ".html?"
                + XML.encodeAsHTMLAttribute(newConstraintsNLP.toString())
                + // no vars, not distinct
                "\">"
                + EDStatic.messages.get(Message.SUBSET_REFINE_SUBSET_DOWNLOAD, language)
                + "</a>)\n");

        TableWriterAll twa = null;
        try {
          // get the raw data
          String fullCountsQuery = lastPName + newConstraintsNLP.toString();
          twa =
              new TableWriterAll(
                  language,
                  this,
                  tNewHistory,
                  cacheDirectory(),
                  suggestFileName(loggedInAs, fullCountsQuery, ".twa"));
          getDataForDapQuery(
              language,
              loggedInAs,
              "",
              fullCountsQuery, // not distinct()
              twa);
          PrimitiveArray varPA = twa.column(0);
          Table countTable = new Table();
          EDV edv = findDataVariableByDestinationName(subsetVariables[lastP]);
          countTable.addColumn(
              0, lastPName, varPA, edv.combinedAttributes().toAttributes(language));

          // sort, count, remove duplicates
          varPA.sortIgnoreCase();
          int n = varPA.size();
          IntArray countPA = new IntArray(n, false);
          countTable.addColumn(EDStatic.messages.get(Message.SUBSET_COUNT, language), countPA);
          int lastCount = 1;
          countPA.add(lastCount);
          keep = new BitSet(n);
          keep.set(0, n); // initially, all set
          for (int i = 1; i < n; i++) {
            if (varPA.compare(i - 1, i) == 0) {
              keep.clear(i - 1);
              lastCount++;
            } else {
              lastCount = 1;
            }
            countPA.add(lastCount);
          }
          countTable.justKeep(keep);

          // calculate percents
          double stats[] = countPA.calculateStats();
          double total = stats[PrimitiveArray.STATS_SUM];
          n = countPA.size();
          DoubleArray percentPA = new DoubleArray(n, false);
          countTable.addColumn(EDStatic.messages.get(Message.SUBSET_PERCENT, language), percentPA);
          for (int i = 0; i < n; i++) percentPA.add(Math2.roundTo(countPA.get(i) * 100 / total, 2));

          // write results
          String countsConNLP = newConstraintsNLPHuman.toString();
          writer.write(
              "<br>"
                  + (countsConNLP.length() == 0
                      ? EDStatic.messages.get(Message.SUBSET_WHEN_NO_CONSTRAINTS, language)
                      : MessageFormat.format(
                          EDStatic.messages.get(Message.SUBSET_WHEN, language),
                          XML.encodeAsHTML(countsConNLP)))
                  + (countsConNLP.length() < 40 ? " " : "\n<br>")
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_WHEN_COUNTS, language),
                      EDStatic.messages.get(Message.SUBSET_VIEW_SELECT_RELATED_COUNTS, language),
                      "" + countTable.nRows(),
                      XML.encodeAsHTML(lastPName))
                  + "\n");
          writer.flush(); // essential, since creating and using another writer to write the
          // countTable
          TableWriterHtmlTable.writeAllAndFinish(
              request,
              language,
              this,
              tNewHistory,
              loggedInAs,
              countTable,
              endOfRequest,
              userQuery,
              new OutputStreamSourceSimple(out),
              false,
              "",
              false,
              "",
              "",
              true,
              false,
              -1);
          writer.write(
              "<p>"
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_TOTAL_COUNT, language),
                      "" + Math2.roundToLong(total))
                  + "\n");
        } catch (Throwable t) {
          EDStatic.rethrowClientAbortException(t); // first thing in catch{}
          String message = MustBe.getShortErrorMessage(t);
          String2.log(
              String2.ERROR
                  + ":\n"
                  + MustBe.throwableToString(t)); // log full message with stack trace
          writer.write(
              // "<p>An error occurred while getting the data:\n" +
              "<pre>" + XML.encodeAsPreHTML(message, 100) + "</pre>\n");
        } finally {
          if (twa != null) {
            twa.releaseResources();
          }
        }

      } else {
        writer.write(
            "<p><span class=\"subduedColor\">"
                + MessageFormat.format(
                    EDStatic.messages.get(Message.SUBSET_VIEW_SELECT, language),
                    EDStatic.messages.get(Message.SUBSET_VIEW_SELECT_RELATED_COUNTS, language),
                    EDStatic.messages.get(Message.SUBSET_VIEW, language),
                    viewTitle[current])
                + "</span>\n"
                + String2.replaceAll(
                    EDStatic.messages.get(Message.SUBSET_WARN, language), "<br>", " ")
                + "\n");
      }

      // 5 = viewRelatedData
      current = 5;
      writer.write(
          "\n"
              + "<br>&nbsp;<hr>\n"
              + "<p><a class=\"selfLink\" id=\"RelatedData\" href=\"#RelatedData\" rel=\"bookmark\"><strong>"
              + viewTitle[current]
              + "</strong></a>\n"
              + EDStatic.htmlTooltipImage(request, language, loggedInAs, viewTooltip[current])
              + "&nbsp;&nbsp;(<a href=\""
              + tErddapUrl
              + "/tabledap/"
              + datasetID
              + ".das\">"
              + EDStatic.messages.get(Message.SUBSET_METADATA, language)
              + "</a>)\n"
              + "&nbsp;&nbsp;\n"
              + "(<a href=\""
              + tErddapUrl
              + "/tabledap/"
              + datasetID
              + ".html"
              + (newConstraints.length() == 0
                  ? ""
                  : "?" + XML.encodeAsHTMLAttribute(newConstraints.toString()))
              + // no vars specified, and not distinct()
              "\">"
              + EDStatic.messages.get(Message.SUBSET_REFINE_SUBSET_DOWNLOAD, language)
              + "</a>)\n");
      if (viewValue[current] > 0) {
        if (allData) {
          writer.write(
              "\n<p><span class=\"subduedColor\">"
                  + MessageFormat.format(
                      EDStatic.messages.get(Message.SUBSET_VIEW_CHECK_1, language),
                      viewTitle[current],
                      ANY)
                  + "</span>\n");

        } else {
          writer.flush(); // essential, since creating and using another writer to write the
          // subsetTable
          try {
            // this shows only the first viewValue[current] rows of related data
            TableWriterHtmlTable twht =
                new TableWriterHtmlTable(
                    request,
                    language,
                    this,
                    tNewHistory,
                    loggedInAs,
                    endOfRequest,
                    userQuery,
                    new OutputStreamSourceSimple(out),
                    false,
                    "",
                    false,
                    "",
                    "",
                    true,
                    true,
                    viewValue[current],
                    EDStatic.imageDirUrl(request, loggedInAs, language)
                        + EDStatic.messages.questionMarkImageFile);
            if (handleViaFixedOrSubsetVariables(
                language,
                loggedInAs,
                requestUrl,
                newConstraints.toString(), // no vars specified, and not distinct()
                twht)) {
            } else
              getDataForDapQuery(
                  language,
                  loggedInAs,
                  "",
                  newConstraints.toString(), // no vars specified, and not distinct()
                  twht);

            // counts
            writer.write(
                "<p>"
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.SUBSET_N_ROWS_RELATED_DATA, language),
                        "" + twht.totalRows())
                    + "\n");
            if (twht.totalRows() > viewValue[current])
              writer.write(
                  "<br><span class=\"warningColor\">"
                      + MessageFormat.format(
                          EDStatic.messages.get(Message.SUBSET_SHOWING_N_ROWS, language),
                          "" + Math.min(twht.totalRows(), viewValue[current]),
                          "" + (twht.totalRows() - viewValue[current]))
                      + "</span>\n");
            else
              writer.write(EDStatic.messages.get(Message.SUBSET_SHOWING_ALL_ROWS, language) + "\n");
            writer.write(
                "<br>"
                    + MessageFormat.format(
                        EDStatic.messages.get(Message.SUBSET_CHANGE_SHOWING, language),
                        "<kbd>"
                            + EDStatic.messages.get(Message.SUBSET_VIEW, language)
                            + " : "
                            + viewTitle[current]
                            + "</kbd>")
                    + "\n");

            /*
            //(simple, but inefficient since it gets all data then writes some)
            //(more efficient: add stopAfterNRows to TableWriter.* and all users of it,
            //   but that would be a lot of work)
            TableWriterAllWithMetadata twa = new TableWriterAllWithMetadata(this,
                cacheDirectory(), "subsetRelatedData");
            if (handleViaFixedOrSubsetVariables(language, loggedInAs,
                newConstraints.toString(), //no vars specified, and not distinct()
                twa)) {}
            else getDataForDapQuery(language, loggedInAs, "",
                newConstraints.toString(), //no vars specified, and not distinct()
                twa);

            //create a table with desired number of rows (in a memory efficient way)
            long twaNRows = twa.nRows();
            Table relatedTable = new Table();
            String[] columnNames = twa.columnNames();
            for (int col = 0; col < columnNames.length; col++)
                relatedTable.addColumn(col, columnNames[col],
                    twa.column(col, viewValue[current]),
                    twa.columnAttributes(col));

            //write table to HtmlTable
            TableWriterHtmlTable.writeAllAndFinish(this, loggedInAs,
                relatedTable, new OutputStreamSourceSimple(out),
                false, "", false, "", "",
                true, true, -1);

            //counts
            writer.write("<p>In total, there are " + twaNRows +
                " rows of related data.\n");
            if (twaNRows > viewValue[current])
                writer.write("<br><span class=\"warningColor\">" +
                    "The first " + Math.min(twaNRows, viewValue[current]) + " rows are shown above. " +
                    "The remaining " + (twaNRows - viewValue[current]) + " rows aren't being shown.</span>\n");
            else writer.write("All of the rows are shown above.\n");
            writer.write(
                "<br>To change the maximum number of rows displayed, change <kbd>View : " +
                viewTitle[current] + "</kbd> above.\n");
            */

          } catch (Throwable t) {
            EDStatic.rethrowClientAbortException(t); // first thing in catch{}
            String message = MustBe.getShortErrorMessage(t);
            String2.log(
                String2.ERROR
                    + ":\n"
                    + MustBe.throwableToString(t)); // log full message with stack trace
            writer.write(
                // "<p>An error occurred while getting the data:\n" +
                "<pre>" + XML.encodeAsPreHTML(message, 100) + "</pre>\n");
          }
        }
      } else {
        writer.write(
            "<p><span class=\"subduedColor\">"
                + MessageFormat.format(
                    EDStatic.messages.get(Message.SUBSET_VIEW_RELATED_CHANGE, language),
                    "<kbd>"
                        + EDStatic.messages.get(Message.SUBSET_VIEW, language)
                        + " : "
                        + viewTitle[current]
                        + "</kbd>")
                + "</span>\n"
                + String2.replaceAll(
                    EDStatic.messages.get(Message.SUBSET_WARN, language), "<br>", " ")
                + "\n");
      }

      writer.write("</div>\n");
      writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
      writer.write("\n</html>\n");
      writer.flush(); // essential
    } catch (Throwable t) {
      EDStatic.rethrowClientAbortException(t); // first thing in catch{}
      try {
        writer.write(EDStatic.htmlForException(language, t));
        writer.write("</div>\n");
        writer.write(EDStatic.endBodyHtml(request, language, tErddapUrl, loggedInAs));
        writer.write("\n</html>\n");
        writer.flush(); // essential
      } catch (Exception e) {
        String2.log(
            "Error while cleaning up from exception (error rethrown after): " + e.getMessage());
      }
      throw t;
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
          String2.log(
              "Error while cleaning up from exception (error rethrown after): " + e.getMessage());
        }
      }
    }
  }

  /**
   * This returns the subsetVariables data table.
   *
   * <p>Data from EDVTimeStamp variables are stored as epochSeconds in the subsetVariables .nc file
   * and data table.
   *
   * <p>This is thread-safe because the constructor (one thread) calls
   * distinctSubsetVariablesDataTable() which calls this and creates .nc file.
   *
   * @param language the index of the selected language
   * @param loggedInAs This is used, e.g., for POST data (where the distinct subsetVariables table
   *     is different for each loggedInAs!) and for EDDTableFromAllDatasets.
   * @return a table with the distinct() combinations of the subsetVariables. The table will have
   *     full metadata.
   * @throws Throwable if trouble (e.g., not accessibleViaSubset())
   */
  public Table subsetVariablesDataTable(int language, String loggedInAs) throws Throwable {
    String subsetFileName = subsetVariablesFileName(loggedInAs);

    // The .subset.nc file is made by the constructor.
    //  The file may be made needlessly, but no delay for first user.

    // read subsetTable from cached file?
    Table table = null;
    if (File2.isFile(datasetDir() + subsetFileName)) {
      table = new Table();
      int enhVer = table.readEnhancedFlatNc(datasetDir() + subsetFileName, subsetVariables);
      if (enhVer == Table.ENHANCED_VERSION) {
        // String2.log(">>subsetVariablesDataTable as read:\n" + table.toCSVString());
        return table;
      }
      // trouble. Fall through to creating a new table and storing in a file.
      String2.log(
          "The existing subsetVariablesDataTable has unsupported enhancedVersion=" + enhVer + ".");
      File2.delete(datasetDir() + subsetFileName);
    }

    // is this dataset accessibleViaSubset?
    if (accessibleViaSubset().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaSubset());

    // Make the subsetVariablesDataTable.
    // If this fails, dataset won't load.  Locally, throws exception if failure.
    // Now: since subset file is made in the constructor, failure cases dataset to not load. That's
    // okay.
    long time = System.currentTimeMillis();
    File2.makeDirectory(datasetDir());

    String tSubsetVars[] = subsetVariables();
    String subsetVariablesCSV = String2.toSVString(tSubsetVars, ",", false);

    // are the combinations available in a .json or .csv file created by ERDDAP admin?
    // <contentDir>subset/datasetID.json
    String adminSubsetFileName = EDStatic.config.contentDirectory + "subset/" + datasetID;
    if (File2.isFile(adminSubsetFileName + ".json")) {
      // json
      if (reallyVerbose)
        String2.log(
            "* "
                + datasetID
                + " is making subsetVariablesDataTable(loggedInAs="
                + loggedInAs
                + ")\n"
                + "from file="
                + adminSubsetFileName
                + ".json");
      table = new Table();
      table.readJson(adminSubsetFileName + ".json"); // throws Exception if trouble

      // put column names in correct order
      // and ensure all of the desired columns are present
      for (int col = 0; col < tSubsetVars.length; col++) {
        int tCol = table.findColumnNumber(tSubsetVars[col]);
        if (tCol < 0)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "subsetVariable="
                  + tSubsetVars[col]
                  + " wasn't found in "
                  + adminSubsetFileName
                  + ".json .");
        if (tCol > col) table.moveColumn(tCol, col);

        // convert EDVTimeStamp ISO Strings to epochSeconds
        EDV edv = findDataVariableByDestinationName(tSubsetVars[col]);
        if (edv instanceof EDVTimeStamp) {
          PrimitiveArray oldPa = table.getColumn(col);
          int nRows = oldPa.size();
          DoubleArray newPa = new DoubleArray(nRows, false);
          for (int row = 0; row < nRows; row++)
            newPa.add(Calendar2.safeIsoStringToEpochSeconds(oldPa.getString(row)));
          table.setColumn(col, newPa);
        }
      }
      // remove excess columns
      table.removeColumns(tSubsetVars.length, table.nColumns());

    } else if (File2.isFile(adminSubsetFileName + ".csv")) {
      // csv
      if (reallyVerbose)
        String2.log(
            "* "
                + datasetID
                + " is making subsetVariablesDataTable(loggedInAs="
                + loggedInAs
                + ")\n"
                + "from file="
                + adminSubsetFileName
                + ".csv");
      table = new Table();
      table.readASCII(
          adminSubsetFileName + ".csv",
          File2.ISO_8859_1,
          "",
          "",
          0,
          1,
          "", // throws Exception if trouble
          null,
          null,
          null, // no tests
          tSubsetVars, // file may have additional columns, but must have these
          false); // simplify (force below)

      // force data types to be as expected
      for (int col = 0; col < tSubsetVars.length; col++) {
        EDV edv = findDataVariableByDestinationName(tSubsetVars[col]);
        PAType destPAType = edv.destinationDataPAType();
        if (edv instanceof EDVTimeStamp) {
          PrimitiveArray oldPa = table.getColumn(col);
          int nRows = oldPa.size();
          DoubleArray newPa = new DoubleArray(nRows, false);
          for (int row = 0; row < nRows; row++)
            newPa.add(Calendar2.safeIsoStringToEpochSeconds(oldPa.getString(row)));
          table.setColumn(col, newPa);
        } else if (destPAType != PAType.STRING) {
          PrimitiveArray newPa = PrimitiveArray.factory(destPAType, 1, false);
          newPa.append(table.getColumn(col));
          table.setColumn(col, newPa);
        }
      }
    } else {
      // See if subsetVariables are all fixedValue variables
      table = new Table();
      boolean justFixed = true;
      for (String tSubsetVar : tSubsetVars) {
        EDV edv = findDataVariableByDestinationName(tSubsetVar);
        if (edv.isFixedValue()) {
          table.addColumn(
              table.nColumns(),
              edv.destinationName(),
              PrimitiveArray.factory(edv.destinationDataPAType(), 1, edv.sourceName().substring(1)),
              edv.combinedAttributes().toAttributes(language));
        } else {
          justFixed = false;
          break;
        }
      }

      if (justFixed) {
        if (reallyVerbose)
          String2.log(
              "* "
                  + datasetID
                  + " made subsetVariablesDataTable("
                  + loggedInAs
                  + ") from just fixedValue variables.");
      } else {
        table = null;
      }
    }

    // if the table was created from a file created by ERDDAP admin or has just fixedValue variables
    // ...
    if (table != null) {
      // at this point, we know the admin-supplied file has just the
      // correct columns in the correct order.

      // ensure sorted (in correct order), then remove duplicates
      int cols[] = new IntArray(0, tSubsetVars.length - 1).toArray();
      boolean order[] = new boolean[tSubsetVars.length];
      Arrays.fill(order, true);
      table.sortIgnoreCase(cols, order);
      table.removeDuplicates();
      table.globalAttributes().add(combinedGlobalAttributes().toAttributes(language));

      // set maxIsMV (assume true)
      for (int col = 0; col < table.nColumns(); col++) table.getColumn(col).setMaxIsMV(true);

      // save it as subset file
      table.saveAsEnhancedFlatNc(datasetDir() + subsetFileName);
      if (verbose)
        String2.log(
            "* "
                + datasetID
                + " made subsetVariablesDataTable(loggedInAs="
                + loggedInAs
                + ") in time="
                + (System.currentTimeMillis() - time)
                + "ms");
      return table;
    }

    // avoid recursion leading to stack overflow from
    //  repeat{getDataForDapQuery -> get subsetVariablesDataTable}
    // if subsetVariables are defined and that data is only available from an admin-made table
    // (which is absent) (and data is not available from data source).
    String stack = MustBe.getStackTrace();
    // String2.log("stack=\n" + stack);
    int count =
        String2.countAll(
            stack, "at gov.noaa.pfel.erddap.dataset.EDDTable.subsetVariablesDataTable(");
    if (count >= 2)
      throw new RuntimeException(
          """
                      Unable to create subsetVariablesDataTable. (in danger of stack overflow)
                      (Perhaps the original, admin-provided subset file in \
                      [tomcat]/content/erddap/subset/
                      wasn't found or couldn't be read.)""");

    // else request the data for the subsetTable from the source
    if (reallyVerbose)
      String2.log(
          "* "
              + datasetID
              + " is making subsetVariablesDataTable("
              + loggedInAs
              + ") from source data...");
    String svDapQuery = subsetVariablesCSV + "&distinct()";
    // don't use getTwawmForDapQuery() since it tries to handleViaFixedOrSubsetVariables
    //  since this method is how subsetVariables gets its data!
    String tNewHistory = combinedGlobalAttributes.getString(language, "history");
    TableWriterAllWithMetadata twawm =
        new TableWriterAllWithMetadata(
            language, this, tNewHistory, cacheDirectory(), subsetFileName);
    TableWriterDistinct twd =
        new TableWriterDistinct(
            language, this, tNewHistory, cacheDirectory(), subsetFileName, twawm);
    getDataForDapQuery(language, loggedInAs, "", svDapQuery, twd);
    table = twawm.cumulativeTable();
    table.convertToStandardMissingValues();

    // set maxIsMV (assume true)
    for (int col = 0; col < table.nColumns(); col++) table.getColumn(col).setMaxIsMV(true);

    // save it
    table.saveAsEnhancedFlatNc(datasetDir() + subsetFileName);
    if (verbose)
      String2.log(
          "* "
              + datasetID
              + " made subsetVariablesDataTable(loggedInAs="
              + loggedInAs
              + ").  time="
              + (System.currentTimeMillis() - time)
              + "ms");

    // done
    return table;
  }

  /**
   * This returns the distinct subsetVariables data table. NOTE: the columns are unrelated! Each
   * column is sorted separately! NOTE: this fully supports all data types (including 2byte chars,
   * long, and Unicode Strings)
   *
   * <p>This is thread-safe because the constructor (one thread) calls
   * distinctSubsetVariablesDataTable() which calls this and makes .nc file.
   *
   * @param language the index of the selected language
   * @param loggedInAs This is used, e.g., for POST data (where the distinct subsetVariables table
   *     is different for each loggedInAs!) and for EDDTableFromAllDatasets.
   * @param loadVars the specific destinationNames to be loaded (or null for all subsetVariables)
   * @return the table with one column for each subsetVariable. The columns are sorted separately
   *     and have DIFFERENT SIZES! So it isn't a valid table! The table will have full metadata.
   * @throws Throwable if trouble (e.g., not accessibleViaSubset())
   */
  public Table distinctSubsetVariablesDataTable(int language, String loggedInAs, String loadVars[])
      throws Throwable {

    String fullDistinctFileName = datasetDir() + distinctSubsetVariablesFileName(loggedInAs);
    Table distinctTable = null;

    // read from cached distinct.nc file?
    if (File2.isFile(fullDistinctFileName)) {
      distinctTable = new Table();
      StringArray varNames = new StringArray();
      // this fully supports all data types (including 2byte chars, longs, unsigned, and Unicode
      // Strings)
      PrimitiveArray pas[] = NcHelper.readPAsInNc3(fullDistinctFileName, loadVars, varNames);
      for (int v = 0; v < varNames.size(); v++)
        distinctTable.addColumn(
            v,
            varNames.get(v),
            pas[v],
            findDataVariableByDestinationName(varNames.get(v))
                .combinedAttributes()
                .toAttributes(language));
      distinctTable.globalAttributes().add(combinedGlobalAttributes().toAttributes(language));
      return distinctTable;
    }

    // is this dataset accessibleViaSubset?
    String tSubsetVars[] = subsetVariables();
    if (tSubsetVars.length == 0 || accessibleViaSubset().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaSubset());

    // create the table and store it in the file
    // **** NOTE: the columns are unrelated!  Each column is sorted separately!
    if (reallyVerbose)
      String2.log(
          "* "
              + datasetID
              + " is making distinctSubsetVariablesDataTable(loggedInAs="
              + loggedInAs
              + ")");
    long time = System.currentTimeMillis();

    // read the combinations table
    // this will throw exception if trouble
    // * this also ensures datasetDir() exists
    distinctTable = subsetVariablesDataTable(language, loggedInAs);

    // find the distinct values
    PrimitiveArray distinctPAs[] = new PrimitiveArray[tSubsetVars.length];
    for (int v = 0; v < tSubsetVars.length; v++) {
      distinctPAs[v] = distinctTable.getColumn(v);
      distinctPAs[v].sortIgnoreCase();
      distinctPAs[v].removeDuplicates(false);
    }

    // store in file
    int randomInt = Math2.random(Integer.MAX_VALUE);
    try {
      // this fully supports all data types (including 2byte chars, long, Unicode Strings, and
      // unsigned types)
      NcHelper.writePAsInNc3(
          fullDistinctFileName + randomInt, new StringArray(tSubsetVars), distinctPAs);
      // sway into place (atomically, in case other threads want this info)
      File2.rename(fullDistinctFileName + randomInt, fullDistinctFileName);
    } catch (Throwable t) {
      // delete any partial file
      File2.delete(fullDistinctFileName + randomInt);
      throw t;
    }
    if (verbose)
      String2.log(
          "* "
              + datasetID
              + " made distinctSubsetVariablesDataTable(loggedInAs="
              + loggedInAs
              + ").  time="
              + (System.currentTimeMillis() - time)
              + "ms (includes subsetVariablesDataTable() time)");

    // *** look for and print out values with \r or \n
    // do this after saving the file (so it is quickly accessible to other threads)
    StringBuilder results = new StringBuilder();
    boolean datasetIdPrinted = false;
    for (int v = 0; v < tSubsetVars.length; v++) {
      PrimitiveArray pa = distinctPAs[v];
      if (pa.elementType() != PAType.STRING) continue;
      boolean columnNamePrinted = false;
      int nRows = pa.size();
      for (int row = 0; row < nRows; row++) {
        String s = pa.getString(row);
        if (s.length() == 0) continue;
        if (s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {

          // troubled value found!
          if (!datasetIdPrinted) {
            results.append(
                "WARNING: for datasetID="
                    + datasetID
                    + "\n"
                    + "(sourceUrl="
                    + publicSourceUrl(language)
                    + "),\n"
                    + "EDDTable.distinctSubsetVariablesDataTable found carriageReturns ('\\r' below)\n"
                    + "or newlines ('\\n' below) in some of the data values.\n"
                    + "Since this makes data searches error-prone and causes ERDDAP's .subset web page\n"
                    + "to have problems, it would be great if you could remove the offending characters.\n"
                    + "Thank you for looking into this.\n\n");
            datasetIdPrinted = true;
          }
          if (!columnNamePrinted) {
            results.append(
                "sourceName="
                    + findDataVariableByDestinationName(tSubsetVars[v]).sourceName()
                    + "\n");
            columnNamePrinted = true;
          }
          // print the troubled value
          results.append("  " + String2.toJson(s) + "\n");
        }
      }
      if (columnNamePrinted) results.append('\n');
    }
    if (results.length() > 0) {
      String subject = "WARNING: datasetID=" + datasetID + " has carriageReturns or newlines";
      String2.log(subject);
      String2.log(results.toString());
      // EDStatic.email(EDStatic.config.emailEverythingToCsv, subject,
      //    results.toString());
    }

    // add original units
    // for (int v = 0; v < nCols; v++) {
    //    EDV edv = findDataVariableByDestinationName(distinctTable.getColumnName(v));
    //    distinctTable.columnAttributes(v).set("units", edv.units());
    // }

    // just the loadVars
    if (loadVars != null)
      distinctTable.reorderColumns(new StringArray(loadVars), true); // discardOthers

    return distinctTable;
  }

  /**
   * This returns a table with the specified columns and 0 rows active, suitable for catching source
   * results. There are no attributes, since attributes are added in standardizeResultsTable.
   *
   * @param resultsDVIs the indices of the results dataVariables.
   * @param capacityNRows
   * @return an empty table with the specified columns and 0 rows active, suitable for catching
   *     source results.
   */
  public Table makeEmptySourceTable(int resultsDVIs[], int capacityNRows) {
    int nDV = resultsDVIs.length;
    EDV resultsEDVs[] = new EDV[nDV];
    for (int rv = 0; rv < nDV; rv++) resultsEDVs[rv] = dataVariables[resultsDVIs[rv]];
    return makeEmptySourceTable(resultsEDVs, capacityNRows);
  }

  /**
   * This returns a table with the specified columns and 0 rows active, suitable for catching source
   * results. There are no attributes, since attributes are added in standardizeResultsTable.
   *
   * @param resultsEDVs
   * @param capacityNRows
   * @return an empty table with the specified columns and 0 rows active, suitable for catching
   *     source results.
   */
  public Table makeEmptySourceTable(EDV resultsEDVs[], int capacityNRows) {
    Table table = new Table();
    for (EDV edv : resultsEDVs) {
      table.addColumn(
          edv.sourceName(), PrimitiveArray.factory(edv.sourceDataPAType(), capacityNRows, false));
    }
    // String2.log("\n>>makeEmptySourceTable cols=" + table.getColumnNamesCSVString());
    return table;
  }

  /**
   * This is like distinctOptionsTable, but doesn't place a limit on the max length of the options.
   */
  public String[][] distinctOptionsTable(int language, String loggedInAs) throws Throwable {
    return distinctOptionsTable(language, loggedInAs, Integer.MAX_VALUE);
  }

  /**
   * This is like distinctSubsetVariablesDataTable, but the values are ready to be used as options
   * in a constraint (e.g., String values are in quotes).
   *
   * @param language the index of the selected language
   * @param loggedInAs null if user not logged in.
   * @param maxChar options that are longer than maxChar are removed (and if so, "[Some really long
   *     options aren't shown.]" is added at the end).
   * @return the left array has a value for each subsetVariable. Times are ISO 8601 string times.
   * @throws Throwable if trouble (e.g., dataset isn't accessibleViaSubset)
   */
  public String[][] distinctOptionsTable(int language, String loggedInAs, int maxChar)
      throws Throwable {
    // get the distinctTable and convert to distinctOptions (String[]'s with #0="")
    Table distinctTable = distinctSubsetVariablesDataTable(language, loggedInAs, subsetVariables());
    String distinctOptions[][] = new String[subsetVariables.length][];
    for (int sv = 0; sv < subsetVariables.length; sv++) {
      EDV edv = findDataVariableByDestinationName(subsetVariables[sv]);
      PrimitiveArray pa = distinctTable.getColumn(sv);
      PAType paType = pa.elementType();

      // avoid excess StringArray conversions (which canonicalize the Strings)
      if (paType == PAType.CHAR || paType == PAType.STRING) {

        StringArray sa = paType == PAType.CHAR ? new StringArray(pa) : (StringArray) pa;
        int n = sa.size();

        // remove strings longer than maxChar
        if (maxChar > 0 && maxChar < Integer.MAX_VALUE) {
          BitSet bitset = new BitSet(n);
          for (int i = 0; i < n; i++) bitset.set(i, sa.get(i).length() <= maxChar);
          sa.justKeep(bitset);
          if (bitset.nextClearBit(0)
              < n) // if don't keep 1 or more... (nextClearBit returns n if none)
          sa.add(EDStatic.messages.get(Message.SUBSET_LONG_NOT_SHOWN, language));
          n = sa.size();
        }

        // convert String variable options to JSON strings
        distinctOptions[sv] = new String[n + 1];
        distinctOptions[sv][0] = "";
        for (int i = 0; i < n; i++) { // 1.. since 0 is ""
          distinctOptions[sv][i + 1] = String2.toJson(sa.get(i), 65536);
        }

      } else if (edv instanceof EDVTimeStamp tts) {

        // convert epochSeconds to iso Strings
        String tTime_precision = tts.time_precision();
        int n = pa.size();
        distinctOptions[sv] = new String[n + 1];
        distinctOptions[sv][0] = "";
        for (int i = 0; i < n; i++) {
          distinctOptions[sv][i + 1] =
              Calendar2.epochSecondsToLimitedIsoStringT(tTime_precision, pa.getDouble(i), "NaN");
        }

      } else {
        // convert numeric options to Strings
        int n = pa.size();
        distinctOptions[sv] = new String[n + 1];
        distinctOptions[sv][0] = "";
        for (int i = 0; i < n; i++) distinctOptions[sv][i + 1] = pa.getString(i);

        // convert final NaN (if present) from "" to NaN
        if (distinctOptions[sv][n].length() == 0) distinctOptions[sv][n] = "NaN";
      }
    }

    return distinctOptions;
  }

  /**
   * This sees if the request can be handled by just fixed values, or the cached subsetVariables or
   * distinct subsetVariables table,
   *
   * <p>This ignores constraints like orderBy() that are handled by the construction of the
   * tableWriter (by making a chain or tableWriters).
   *
   * @param language the index of the selected language
   * @param loggedInAs the user's login name if logged in (or null if not logged in).
   * @param requestUrl the part of the user's request, after EDStatic.config.baseUrl, before '?'. I
   *     think it's currently just used to add to "history" metadata.
   * @param userDapQuery the part of the user's request after the '?', still percentEncoded, may be
   *     null.
   * @param tableWriter
   * @return true if it was handled
   * @throws Throwable e.g., if it should have been handled, but failed. Or if no data.
   */
  public boolean handleViaFixedOrSubsetVariables(
      int language,
      String loggedInAs,
      String requestUrl,
      String userDapQuery,
      TableWriter tableWriter)
      throws Throwable {

    if (userDapQuery == null) userDapQuery = "";

    // parse the query
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();
    parseUserDapQuery(
        language,
        userDapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues, // non-regex EDVTimeStamp conValues will be ""+epochSeconds
        false); // don't repair

    // can query be handled by just fixed value variables?
    // If so, the response is just one row with fixed values
    //  (if constraints don't make it 0 rows).
    boolean justFixed = true;
    Table table = null;
    for (int rv = 0; rv < resultsVariables.size(); rv++) {
      EDV edv = findDataVariableByDestinationName(resultsVariables.get(rv));
      if (edv.isFixedValue()) {
        if (table == null) table = new Table();
        table.addColumn(
            table.nColumns(),
            edv.destinationName(),
            PrimitiveArray.factory(edv.destinationDataPAType(), 1, edv.fixedValue()),
            edv.combinedAttributes().toAttributes(language));
      } else {
        justFixed = false;
        break;
      }
    }
    if (justFixed) {
      BitSet keep = new BitSet();
      keep.set(0);
      for (int cv = 0; cv < constraintVariables.size(); cv++) {
        EDV edv = findDataVariableByDestinationName(constraintVariables.get(cv));
        if (edv.isFixedValue()) {
          // is it true?
          PrimitiveArray pa =
              PrimitiveArray.factory(edv.destinationDataPAType(), 1, edv.sourceName().substring(1));
          if (pa.applyConstraint(
                  edv instanceof EDVTimeStamp,
                  keep,
                  constraintOps.get(cv),
                  constraintValues.get(cv))
              == 0)
            throw new SimpleException(
                MustBe.THERE_IS_NO_DATA
                    + " (after "
                    + constraintVariables.get(cv)
                    + constraintOps.get(cv)
                    + constraintValues.get(cv)
                    + ")");
        } else {
          justFixed = false;
          break;
        }
      }

      // succeeded?
      if (justFixed) {
        setResponseGlobalAttributes(language, requestUrl, userDapQuery, table);
        tableWriter.writeAllAndFinish(table);
        if (verbose)
          String2.log(
              datasetID
                  + ".handleViaFixedOrSubsetVariables got the data from fixedValue variables.");
        return true;
      }
    }

    // *** Handle request via subsetVariables?
    // quick check: ensure &distinct() is in the query?
    // No, assume: if request is just subsetVariables, distinct() is implied?
    // if (userDapQuery.indexOf("&distinct()") < 0)
    //    return false;
    table = null;

    // quick check: ensure subsetVariables system is active
    String tSubsetVars[] = subsetVariables();
    if (tSubsetVars.length == 0 || accessibleViaSubset().length() > 0) return false;

    // ensure query can be handled completely by just subsetVars
    for (int rv = 0; rv < resultsVariables.size(); rv++)
      if (String2.indexOf(tSubsetVars, resultsVariables.get(rv)) < 0) return false;
    for (int cv = 0; cv < constraintVariables.size(); cv++)
      if (String2.indexOf(tSubsetVars, constraintVariables.get(cv)) < 0) return false;

    // OK. This method will handle the request.
    long time = System.currentTimeMillis();

    // If this just involves 1 column, get distinctSubsetVariablesDataTable (it's smaller)
    // else get subsetVariablesDataTable
    boolean justOneVar = resultsVariables.size() == 1;
    if (justOneVar) {
      String varName = resultsVariables.get(0);
      for (int con = 0; con < constraintVariables.size(); con++) {
        if (!varName.equals(constraintVariables.get(con))) {
          justOneVar = false;
          break;
        }
      }
    }
    if (justOneVar) {
      table =
          distinctSubsetVariablesDataTable(
              language, loggedInAs, new String[] {resultsVariables.get(0)});
    } else {
      table = subsetVariablesDataTable(language, loggedInAs);
    }

    // apply constraints, rearrange columns, add metadata
    applyConstraints(
        language,
        table,
        true, // applyAllConstraints
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues);
    setResponseGlobalAttributes(language, requestUrl, userDapQuery, table);

    // write to tableWriter
    tableWriter.writeAllAndFinish(table);
    if (verbose)
      String2.log(
          datasetID
              + ".handleViaFixedOrSubsetVariables got the data from the "
              + (justOneVar ? "distinctSubset" : "subset")
              + " file.  time="
              + (System.currentTimeMillis() - time)
              + "ms");

    return true;
  }

  /**
   * This sets accessibleViaSubset and returns the array of dataVariable destinationNames for use by
   * .subset (or String[0] if unused).
   *
   * <p>NOTE: The first call to this is the first access to this system for this dataset. So, it
   * causes the cached subset file to be deleted.
   */
  public String[] subsetVariables() {
    if (subsetVariables == null || accessibleViaSubset == null) {

      String tAccessibleViaSubset = "";
      String tSubsetVariables[];
      String subsetVariablesCSV =
          combinedGlobalAttributes().getString(EDMessages.DEFAULT_LANGUAGE, "subsetVariables");
      if (subsetVariablesCSV == null || subsetVariablesCSV.length() == 0) {
        tSubsetVariables = new String[0];
      } else {
        StringArray sa =
            subsetVariablesCSV.indexOf(',') > 0
                ? StringArray.fromCSV(subsetVariablesCSV)
                : StringArray.wordsAndQuotedPhrases(subsetVariablesCSV);
        int n = sa.size();
        tSubsetVariables = new String[n];
        for (int i = 0; i < n; i++) {
          int po = String2.indexOf(dataVariableDestinationNames(), sa.get(i));
          if (po >= 0) tSubsetVariables[i] = sa.get(i);
          else {
            // tAccessibleViaSubset =
            String2.log("destinationNames=" + String2.toCSSVString(dataVariableDestinationNames()));
            throw new SimpleException(
                "<subsetVariables> wasn't set up correctly: \""
                    + sa.get(i)
                    + "\" isn't a valid variable destinationName.");

            // String2.log(String2.ERROR + " for datasetID=" + datasetID + ": " +
            // tAccessibleViaSubset);
            // tSubsetVariables = new String[0];
            // break;
          }
        }
      }
      if (tSubsetVariables.length == 0)
        tAccessibleViaSubset = EDStatic.messages.get(Message.SUBSET_NOT_SET_UP, 0);

      if ((className.equals("EDDTableFromDapSequence") || className.equals("EDDTableFromErddap"))
          && EDStatic.config.quickRestart
          && EDStatic.initialLoadDatasets()
          && File2.isFile(quickRestartFullFileName())) {

        // don't delete subset and distinct files  (to reuse them)
        if (verbose) String2.log("  quickRestart: reusing subset and distinct files");

      } else if ((this instanceof EDDTableFromFiles)
          && EDStatic.config.quickRestart
          && EDStatic.initialLoadDatasets()) {

        // 2022-07-26 don't delete subset and distinct files  (to reuse them)
        // !! It would be better if this ensured that subset.nc has same variables as
        // tSubsetVariables
        if (verbose) String2.log("  quickRestart: reusing subset and distinct files");

      } else {

        // delete the subset and distinct files if they exist (to force recreation)
        if (File2.isDirectory(datasetDir())) {

          // DELETE the cached distinct combinations file.
          // the subsetVariables may have changed (and the dataset probably just reloaded)
          //  so delete the file (or files for POST datasets) with all of the distinct combinations
          // see subsetVariablesFileName());
          RegexFilenameFilter.regexDelete(
              datasetDir(),
              ".*"
                  + // .* handles POST files which have loggedInAs in here
                  String2.replaceAll(DISTINCT_SUBSET_FILENAME, ".", "\\."),
              false);

          // DELETE the new-style cached .nc subset table files in the subdir of /datasetInfo/
          RegexFilenameFilter.regexDelete(
              datasetDir(),
              ".*"
                  + // .* handles POST files which have loggedInAs in here
                  String2.replaceAll(SUBSET_FILENAME, ".", "\\."),
              false);
        }
      }

      // LAST: set in an instant  [Now this is always done by constructor -- 1 thread]
      subsetVariables = tSubsetVariables;
      accessibleViaSubset = String2.canonical(tAccessibleViaSubset);
    }

    return subsetVariables;
  }

  /** This indicates why the dataset isn't accessible via .subset (or "" if it is). */
  @Override
  public String accessibleViaSubset() {
    if (accessibleViaSubset == null)
      subsetVariables(); // it sets accessibleViaSubset, so it can be returned below

    return accessibleViaSubset;
  }

  /**
   * This is called by EDDTable.ensureValid by subclass constructors, to go through the metadata of
   * the dataVariables and gather all unique sosObservedProperties.
   */
  /*protected void gatherSosObservedProperties() {
      HashSet<String> set = new HashSet(Math2.roundToInt(1.4 * dataVariables.length));
      for (int dv = 0; dv < dataVariables.length; dv++) {
          String s = dataVariables[dv].combinedAttributes().getString("observedProperty");
          if (s != null)
             set.add(s);
      }
      sosObservedProperties = set.toArray(new String[0]);
      Arrays.sort(sosObservedProperties);
  }*/

  /** This indicates why the dataset isn't accessible via Make A Graph (or "" if it is). */
  @Override
  public String accessibleViaMAG() {
    if (accessibleViaMAG == null) {
      int nNumeric = 0;
      for (EDV dataVariable : dataVariables) {
        // String2.log(">> accessibleViaMAG #" + dv + " " + dataVariables[dv].destinationName() + "
        // " + dataVariables[dv].destinationDataPAType());
        if (dataVariable.destinationDataPAType() != PAType.STRING) nNumeric++;
      }
      if (nNumeric == 0)
        accessibleViaMAG =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(
                        Message.NO_XXX_BECAUSE_2,
                        0), // language=0 because only 1 value of accessibleViaMAG
                    EDStatic.messages.get(Message.MAG, 0),
                    EDStatic.messages.get(Message.NO_XXX_NO_NON_STRING, 0)));
      else if (nNumeric == 1)
        accessibleViaMAG =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE_2, 0),
                    EDStatic.messages.get(Message.MAG, 0),
                    EDStatic.messages.get(Message.NO_XXX_NO_2_NON_STRING, 0)));
      else accessibleViaMAG = String2.canonical("");
    }
    return accessibleViaMAG;
  }

  /**
   * This returns an error message regarding why the dataset isn't accessible via SOS (or "" if it
   * is). ???No restriction on lon +/-180 vs. 0-360? ???Currently, sosOffering must be "station"
   * (because of assumption of 1 lat,lon in getObservations.
   *
   * <p>Unlike other accessibleViaXxx(), this doesn't set this value if currently null. I just
   * returns the current value (which may be null). The constructor should call
   * setAccessibleViaSOS().
   *
   * @return the current value of accessibleViaSOS (which may be null). After setAccessibleViaSOS(),
   *     this will return "" (yes, it is accessible) or some error message saying why not.
   */
  @Override
  public String accessibleViaSOS() {
    return accessibleViaSOS;
  }

  /**
   * This does the basic tests to ensure this dataset may be accessibleViaSOS. This just doesn't
   * test that sosOfferings, sosMinLat, sosMaxLat, ... were all set.
   *
   * @return "" if the dataset is compatible with being accessibleViaSOS, or some error message
   *     explaining why not.
   */
  public String preliminaryAccessibleViaSOS() {

    // easy tests make for a better error message
    if (!EDStatic.config.sosActive)
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS",
              MessageFormat.format(EDStatic.messages.get(Message.NO_XXX_NOT_ACTIVE, 0), "SOS")));

    if (lonIndex < 0 || latIndex < 0 || timeIndex < 0)
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS",
              EDStatic.messages.get(Message.NO_XXX_NO_LLT, 0)));

    String cdt = combinedGlobalAttributes().getString(EDMessages.DEFAULT_LANGUAGE, "cdm_data_type");
    int type = sosCdmDataTypes.indexOf(cdt);
    if (type < 0)
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS",
              MessageFormat.format(
                  EDStatic.messages.get(Message.NO_XXX_NO_CDM_DATA_TYPE, 0), cdt)));

    if (!setSosOfferingTypeAndIndex())
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS",
              EDStatic.messages.get(Message.NO_XXX_NO_STATION_ID, 0)));

    // this just doesn't check that sosOfferings, sosMinLat, sosMaxLat, ... were all set.

    // so far so good
    return String2.canonical("");
  }

  /**
   * This sees if the EDDTable subclass' constructor set up this dataset for ERDDAP's SOS server. If
   * not, this tries to do a generic SOS setup by extracting information from the
   * subsetVariablesDataTable.
   *
   * <p>This must be called *after* subsetVariablesDataTable has been created. The disadvantage of
   * the generic SOS setup is that it always uses the dataset's entire time range for each offering
   * (e.g., station), not each offering's time range.
   *
   * @return "" if dataset is accessibleViaSOS, or some error message explaining why the dataset
   *     isn't accessibleViaSOS.
   */
  public String makeAccessibleViaSOS() throws Throwable {

    // check for simple reasons not accessibleViaSOS
    String prelim = preliminaryAccessibleViaSOS(); // calls setSosOfferingTypeAndIndex()
    if (!prelim.isEmpty()) return prelim;

    // so far so good
    // already set up for SOS? e.g., by subsclass constructor
    if (sosOfferingPrefix == null) sosOfferingPrefix = String2.canonical("");
    if ((sosMinLon != null && sosMaxLon != null)
        || (sosMinLat != null && sosMaxLat != null)
        || (sosMinTime != null && sosMaxTime != null)
        || sosOfferings != null)
      // we're successfully done!
      return prelim; // ""

    // is there a subsetVariables table?
    if (subsetVariables == null || subsetVariables.length == 0)
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS", // language=0 in this method because only 1 version of accessibleViaSOS
              EDStatic.messages.get(Message.NO_XXX_NO_SUBSET_VARIABLES, 0)));

    // are sosOfferingIndex, lon, lat in subsetVariablesDataTable?
    // This will only be true if lon and lat are point per offering.
    String sosOfferingDestName = dataVariableDestinationNames[sosOfferingIndex];
    if (String2.indexOf(subsetVariables, sosOfferingDestName) < 0
        || String2.indexOf(subsetVariables, "longitude") < 0
        || String2.indexOf(subsetVariables, "latitude") < 0)
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS",
              EDStatic.messages.get(Message.NO_XXX_NO_OLL_SUBSET_VARIABLES, 0)));

    // Request the distinct() offerings (station names), lon, lat data
    //  from the subsetVariables data table.
    // This should succeed. So don't protect against failure.
    //  So failure causes dataset to be not loaded.
    Table table = subsetVariablesDataTable(0, EDStatic.loggedInAsSuperuser); // throws Throwable
    table.reorderColumns(
        StringArray.fromCSV(sosOfferingDestName + ",longitude,latitude"), true); // discardOthers
    table.leftToRightSortIgnoreCase(3);

    // if subsetVariables includes observations variables (it may), there will be duplicates
    table.removeDuplicates();

    // but then there shouldn't be duplicate offeringNames
    //  (i.e., multiple lat,lon for a given offering)
    if (table.getColumn(0).removeDuplicates() > 0)
      return String2.canonical(
          MessageFormat.format(
              EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
              "SOS",
              "In the subsetVariables table, some offerings (stations) have "
                  + ">1 longitude,latitude."));

    // copy the results to the sos PAs
    sosOfferings = table.getColumn(0);
    sosMinLon = table.getColumn(1);
    sosMaxLon = sosMinLon;
    sosMinLat = table.getColumn(2);
    sosMaxLat = sosMinLat;

    // try to get time range from time variable

    sosMinTime = PrimitiveArray.factory(PAType.DOUBLE, 8, true); // unknown
    sosMaxTime = sosMinTime; // unknown

    // success
    return prelim; // ""
  }

  /** This indicates why the dataset isn't accessible via ESRI GeoServices REST (or "" if it is). */
  @Override
  public String accessibleViaGeoServicesRest() {
    if (accessibleViaGeoServicesRest == null) {

      if (!EDStatic.config.geoServicesRestActive)
        accessibleViaGeoServicesRest =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
                    "GeoServicesRest", // language=0 here because only 1 value of accessibleVia...
                    MessageFormat.format(
                        EDStatic.messages.get(Message.NO_XXX_NOT_ACTIVE, 0), "GeoServicesRest")));
      else
        accessibleViaGeoServicesRest =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
                    "GeoServicesRest",
                    EDStatic.messages.get(Message.NO_XXX_ITS_TABULAR, 0)));
    }
    return accessibleViaGeoServicesRest;
  }

  /** This indicates why the dataset isn't accessible via WCS (or "" if it is). */
  @Override
  public String accessibleViaWCS() {
    if (accessibleViaWCS == null) {

      if (!EDStatic.config.wcsActive)
        accessibleViaWCS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
                    "WCS", // language=0 here because only 1 value of accessibleVia...
                    MessageFormat.format(
                        EDStatic.messages.get(Message.NO_XXX_NOT_ACTIVE, 0), "WCS")));
      else
        accessibleViaWCS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
                    "WCS",
                    EDStatic.messages.get(Message.NO_XXX_ITS_TABULAR, 0)));
    }
    return accessibleViaWCS;
  }

  /** This indicates why the dataset isn't accessible via WMS (or "" if it is). */
  @Override
  public String accessibleViaWMS() {
    if (accessibleViaWMS == null)
      if (!EDStatic.config.wmsActive)
        accessibleViaWMS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
                    "WMS", // language=0 here because only 1 value of accessibleVia...
                    MessageFormat.format(
                        EDStatic.messages.get(Message.NO_XXX_NOT_ACTIVE, 0), "WMS")));
      else
        accessibleViaWMS =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE, 0),
                    "WMS",
                    EDStatic.messages.get(Message.NO_XXX_ITS_TABULAR, 0)));
    return accessibleViaWMS;
  }

  /**
   * This indicates why the dataset isn't accessible via .ncCF and .ncCFMA file type (or "" if it
   * is). Currently, this is only for some of the Discrete Sampling Geometries (was
   * PointObservationConventions) cdm_data_type representations at
   * https://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#discrete-sampling-geometries
   *
   * @throws SimpleException if trouble (e.g., something set up wrong).
   */
  @Override
  public String accessibleViaNcCF() {

    if (accessibleViaNcCF == null) {
      int language = EDMessages.DEFAULT_LANGUAGE;
      // !!! For thread safety, don't use temporary value of accessibleViaNcCF.
      // Just set it with the final value; don't change it.
      String cdmType = combinedGlobalAttributes.getString(language, "cdm_data_type");
      if (cdmType == null || cdmType.length() == 0)
        throw new SimpleException("cdm_data_type must be specified in globalAttributes.");
      // if (!cdmType.equals(CDM_POINT) &&
      //    !cdmType.equals(CDM_PROFILE) &&
      //    !cdmType.equals(CDM_TIMESERIES) &&
      //    !cdmType.equals(CDM_TIMESERIESPROFILE) &&
      //    !cdmType.equals(CDM_TRAJECTORY) &&
      //    !cdmType.equals(CDM_TRAJECTORYPROFILE)) {

      String startCdm = "cdm_data_type=" + cdmType + ", but ";
      StringArray requiredRequestVars = new StringArray();
      // a list of all cdm_profile+timeseries+trajectory_variables
      // each data request must include at least one variable *not* on this list
      StringArray outerVars = new StringArray();

      // set cdmUses  (e.g., TimeSeriesProfile uses timeseries and profile)
      String cdmLCNames[] = new String[] {"profile", "timeseries", "trajectory"};
      boolean cdmUses[] = new boolean[3];
      for (int cdmi = 0; cdmi < 3; cdmi++)
        cdmUses[cdmi] = cdmType.toLowerCase().indexOf(cdmLCNames[cdmi]) >= 0;

      // look for the cf_role=timeseries_id (and profile_id and trajectory_id) columns
      //  (or ensure not present if wrong cdmType)
      int cdmIdIndex[] = new int[] {-1, -1, -1};
      String cdmIdName[] = new String[] {null, null, null};
      int ndv = dataVariables.length;
      for (int dv = 0; dv < ndv; dv++) {
        // if (debugMode && dataVariables[dv].destinationName().equals("time"))
        //    String2.log(">>time:\n" + dataVariables[dv].combinedAttributes());
        String cfRole = dataVariables[dv].combinedAttributes().getString(language, "cf_role");
        if (cfRole != null) {
          for (int cdmi = 0; cdmi < 3; cdmi++) {
            if ((cdmLCNames[cdmi] + "_id").equals(cfRole)) {
              if (cdmUses[cdmi]) {
                // already set!
                if (cdmIdIndex[cdmi] >= 0)
                  throw new SimpleException(
                      "cf_role="
                          + cdmLCNames[cdmi]
                          + "_id must be defined for *only* one variable (see "
                          + dataVariables[cdmIdIndex[cdmi]].destinationName()
                          + " and "
                          + dataVariables[dv].destinationName()
                          + ").");
                // set it
                cdmIdIndex[cdmi] = dv;
                cdmIdName[cdmi] = dataVariables[dv].destinationName();
              } else {
                // shouldn't be defined
                throw new SimpleException(
                    "For cdm_data_type="
                        + cdmType
                        + ", no variable should have cf_role="
                        + cdmLCNames[cdmi]
                        + "_id (see "
                        + dataVariables[dv].destinationName()
                        + ").");
              }
            }
          }
        }
      }
      // ensure the required cf_role=xxx_id were set
      for (int cdmi = 0; cdmi < 3; cdmi++) {
        if (cdmUses[cdmi] && cdmIdIndex[cdmi] < 0)
          throw new SimpleException(
              "For cdm_data_type="
                  + cdmType
                  + ", one variable must have cf_role="
                  + cdmLCNames[cdmi]
                  + "_id. (Currently, none have that cf_role.)");
      }
      profile_idIndex = cdmIdIndex[0];
      timeseries_idIndex = cdmIdIndex[1];
      trajectory_idIndex = cdmIdIndex[2];
      // ???set old sosOfferingIndex to one of these? How is that used?

      // look/validate for cdm_xxx_variables   (or ensure they aren't present if they shouldn't be)
      String cdmVars[][] = new String[3][]; // subarray may be size=0, won't be null
      for (int cdmi = 0; cdmi < 3; cdmi++) {
        cdmVars[cdmi] =
            combinedGlobalAttributes.getStringsFromCSV(
                language, "cdm_" + cdmLCNames[cdmi] + "_variables");
        if (cdmVars[cdmi] == null) cdmVars[cdmi] = new String[0];
        if (cdmUses[cdmi]) {
          if (cdmVars[cdmi].length == 0)
            // should be set, but isn't
            throw new SimpleException(
                "For cdm_data_type="
                    + cdmType
                    + ", the global attribute cdm_"
                    + cdmLCNames[cdmi]
                    + "_variables must be set."
                // + "\nsourceGlobalAtts=" + sourceGlobalAttributes.toString()
                );
          for (int var = 0; var < cdmVars[cdmi].length; var++) {
            // unknown variable
            if (String2.indexOf(dataVariableDestinationNames(), cdmVars[cdmi][var]) < 0)
              throw new SimpleException(
                  "cdm_"
                      + cdmLCNames[cdmi]
                      + "_variables #"
                      + var
                      + "="
                      + cdmVars[cdmi][var]
                      + " isn't one of the data variable destinationNames.");
          }

          for (int cdmi2 = 0; cdmi2 < 3; cdmi2++) {
            if (cdmi == cdmi2) {
              // ensure the e.g., cf_role=timeseries_id variable is in the cdm_timeseries_variables
              // list
              if (String2.indexOf(cdmVars[cdmi], cdmIdName[cdmi2]) < 0)
                throw new SimpleException(
                    "For cdm_data_type="
                        + cdmType
                        + ", the variable with cf_role="
                        + cdmLCNames[cdmi]
                        + "_id ("
                        + cdmIdName[cdmi2]
                        + ") must be in the cdm_"
                        + cdmLCNames[cdmi]
                        + "_variables list.");

              // ensure the other e.g., cf_role=profile_id variable isn't in the
              // cdm_timeseries_variables list
            } else if (cdmIdName[cdmi2] != null
                && String2.indexOf(cdmVars[cdmi], cdmIdName[cdmi2]) >= 0)
              throw new SimpleException(
                  "For cdm_data_type="
                      + cdmType
                      + ", the variable with cf_role="
                      + cdmLCNames[cdmi2]
                      + "_id ("
                      + cdmIdName[cdmi2]
                      + ") must not be in the cdm_"
                      + cdmLCNames[cdmi]
                      + "_variables list.");
          }

        } else if (cdmVars[cdmi].length > 0) {
          // is set, but shouldn't be
          throw new SimpleException(
              "For cdm_data_type="
                  + cdmType
                  + ", the global attribute cdm_"
                  + cdmLCNames[cdmi]
                  + "_variables must *not* be set.");
        }
      }

      // ensure profile datasets have altitude, depth or cdm_altitude_proxy
      String proxy = combinedGlobalAttributes.getString(language, "cdm_altitude_proxy");
      int proxyDV = String2.indexOf(dataVariableDestinationNames(), proxy);
      if (altIndex >= 0) {
        if (proxy != null && !proxy.equals("altitude"))
          throw new SimpleException(
              "If the dataset has an altitude variable, "
                  + "don't define the global attribute cdm_altitude_proxy.");
      } else if (depthIndex >= 0) {
        if (proxy != null && !proxy.equals("depth"))
          throw new SimpleException(
              "If the dataset has a depth variable, "
                  + "don't define the global attribute cdm_altitude_proxy.");
      } else if (proxyDV >= 0) {
        // okay
        EDV proxyEDV = dataVariables[proxyDV];
        proxyEDV.addAttributes().set(language, "_CoordinateAxisType", "Height");
        proxyEDV.combinedAttributes().set(language, "_CoordinateAxisType", "Height");
        proxyEDV.addAttributes().set(language, "axis", "Z");
        proxyEDV.combinedAttributes().set(language, "axis", "Z");

      } else if (cdmUses[0]) {
        throw new SimpleException(
            "For cdm_data_type="
                + cdmType
                + ", when there is no altitude or depth variable, "
                + "you MUST define the global attribute cdm_altitude_proxy.");
      }

      // ensure there is no overlap of cdm_xxx_variables
      // test profile and timeseries
      StringArray sa1 = new StringArray(cdmVars[0].clone());
      StringArray sa2 = new StringArray(cdmVars[1].clone());
      sa1.sort();
      sa2.sort();
      sa1.inCommon(sa2);
      if (sa1.size() > 0)
        throw new SimpleException(
            "There must not be any variables common to cdm_profile_variables and "
                + "cdm_timeseries_variables. Please fix these: "
                + sa1
                + ".");

      // test timeseries and trajectory   -- actually, they shouldn't coexist
      if (cdmVars[1].length > 0 && cdmVars[2].length > 0)
        throw new SimpleException(
            "cdm_timeseries_variables and cdm_trajectory_variables must never both be defined.");

      // test trajectory and profile
      sa1 = new StringArray(cdmVars[2].clone());
      sa2 = new StringArray(cdmVars[0].clone());
      sa1.sort();
      sa2.sort();
      sa1.inCommon(sa2);
      if (sa1.size() > 0)
        throw new SimpleException(
            "There must not be any variables common to cdm_profile_variables and "
                + "cdm_trajectory_variables. Please fix these: "
                + sa1
                + ".");

      // check the cdmType's requirements
      if (accessibleViaNcCF != null) {
        // error message already set

      } else if (cdmType == null || cdmType.length() == 0) {
        throw new SimpleException("cdm_data_type wasn't specified in globalAttributes.");

        // Point
      } else if (cdmType.equals(CDM_POINT)) {
        // okay!

        // Profile
      } else if (cdmType.equals(CDM_PROFILE)) {
        // okay!
        requiredRequestVars.add(dataVariableDestinationNames()[profile_idIndex]);
        outerVars.add(cdmVars[0]);

        // TimeSeries
      } else if (cdmType.equals(CDM_TIMESERIES)) {
        // okay!
        requiredRequestVars.add(dataVariableDestinationNames()[timeseries_idIndex]);
        outerVars.add(cdmVars[1]);

        // Trajectory
      } else if (cdmType.equals(CDM_TRAJECTORY)) {
        // okay!
        requiredRequestVars.add(dataVariableDestinationNames()[trajectory_idIndex]);
        outerVars.add(cdmVars[2]);

        // TimeSeriesProfile
      } else if (cdmType.equals(CDM_TIMESERIESPROFILE)) {
        // okay!
        requiredRequestVars.add(dataVariableDestinationNames()[timeseries_idIndex]);
        requiredRequestVars.add(dataVariableDestinationNames()[profile_idIndex]);
        outerVars.add(cdmVars[0]);
        outerVars.add(cdmVars[1]);

        // TrajectoryProfile
      } else if (cdmType.equals(CDM_TRAJECTORYPROFILE)) {
        // okay!
        requiredRequestVars.add(dataVariableDestinationNames()[trajectory_idIndex]);
        requiredRequestVars.add(dataVariableDestinationNames()[profile_idIndex]);
        outerVars.add(cdmVars[0]);
        outerVars.add(cdmVars[2]);

        // the cdmType (e.g., Other) doesn't support .ncCF/.ncCFMA
      } else {
        accessibleViaNcCF =
            String2.canonical(
                MessageFormat.format(
                    EDStatic.messages.get(Message.NO_XXX_BECAUSE_2, 0),
                    ".ncCF/.ncCFMA",
                    MessageFormat.format(
                        EDStatic.messages.get(Message.NO_XXX_NO_CDM_DATA_TYPE, 0), cdmType)));
      }

      // no errors so far
      if (accessibleViaNcCF == null) {
        if (lonIndex < 0) {
          throw new SimpleException(startCdm + "the dataset doesn't have a longitude variable.");
        } else if (latIndex < 0) {
          throw new SimpleException(startCdm + "the dataset doesn't have a latitude variable.");
        } else if (timeIndex < 0) {
          throw new SimpleException(startCdm + "the dataset doesn't have a time variable.");
        } else {
          // it passes!
          if (requiredRequestVars.indexOf("longitude") < 0) requiredRequestVars.add("longitude");
          if (requiredRequestVars.indexOf("latitude") < 0) requiredRequestVars.add("latitude");
          if (requiredRequestVars.indexOf("time") < 0) requiredRequestVars.add("time");
          if (altIndex >= 0) {
            if (requiredRequestVars.indexOf("altitude") < 0) requiredRequestVars.add("altitude");
          } else if (depthIndex >= 0) {
            if (requiredRequestVars.indexOf("depth") < 0) requiredRequestVars.add("depth");
          } else if (proxy != null) {
            if (requiredRequestVars.indexOf(proxy) < 0) requiredRequestVars.add(proxy);
          }
          requiredCfRequestVariables = requiredRequestVars.toArray();

          outerCfVariables = outerVars.toArray();
          accessibleViaNcCF = String2.canonical(""); // do last
        }
      }
    }
    return accessibleViaNcCF;
  }

  /**
   * This returns the start of the SOS GML offering/procedure name, e.g.,
   * urn:ioos:sensor:noaa.nws.ndbc:41004:adcp (but for ERDDAP, the station = the sensor)
   * urn:ioos:station:noaa.nws.ndbc:41004: urn:ioos:network:noaa.nws.ndbc:all (IOOS NDBC)
   * urn:ioos:network:noaa.nws.ndbc:[datasetID] (ERDDAP) (after : is (datasetID|stationID)[:varname]
   *
   * @param language the index of the selected language
   * @param tSosOfferingType usually this EDDTable's sosOfferingType (e.g., Station), but sometimes
   *     sosNetworkOfferingType ("network") or "sensor".
   * @return sosGmlNameStart
   */
  public String getSosGmlNameStart(int language, String tSosOfferingType) {
    return sosUrnBase(language)
        + ":"
        + tSosOfferingType
        + ":"
        + EDStatic.config.sosBaseGmlName
        + "."
        + datasetID
        + ":"; // + shortOffering
  }

  /**
   * This returns the SOS capabilities xml for this dataset (see Erddap.doSos). This should only be
   * called if accessibleViaSOS is true and loggedInAs has access to this dataset (so redirected to
   * login, instead of getting an error here).
   *
   * <p>This seeks to comply with the IOOS SOS Template, v1.0
   * https://code.google.com/p/ioostech/source/browse/trunk/templates/Milestone1.0/OM-GetCapabilities.xml
   * and to mimic IOOS SOS servers like 52N (the OGC reference implementation)
   * https://sensorweb.demo.52north.org/52nSOSv3.2.1/sos and ndbcSosWind
   * https://sdf.ndbc.noaa.gov/sos/ .
   *
   * @param language the index of the selected language
   * @param queryMap the parts of the query, stored as a name=value map, where all of the names have
   *     been made lowercase. See OGC 06-121r3 Table 3 and especially 4. This assumes (doesn't
   *     check) that this includes the required service=SOS and request=GetCapabilities. Currently,
   *     this ignores the optional acceptVersions= and always returns a version 1.0.0-style
   *     document.
   * @param writer This must be a UTF-8 writer. In the end, the writer is flushed, not closed.
   * @param loggedInAs the name of the logged in user (or null if not logged in).
   */
  public void sosGetCapabilities(
      HttpServletRequest request,
      int language,
      Map<String, String> queryMap,
      Writer writer,
      String loggedInAs)
      throws Throwable {

    if (accessibleViaSOS().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaSOS());

    // validate section=    values are case-sensitive
    String section_ServiceIdentification = "ServiceIdentification";
    String section_ServiceProvider = "ServiceProvider";
    String section_OperationsMetadata = "OperationsMetadata";
    // !!!52N supports Filter_Capabilities
    String section_Contents = "Contents";
    String section_All = "All";
    String sectionOptions[] = {
      section_ServiceIdentification,
      section_ServiceProvider,
      section_OperationsMetadata,
      section_Contents,
      section_All
    };
    String tSections = queryMap.get("sections");
    StringArray sections =
        StringArray.fromCSVNoBlanks(tSections == null ? section_All : tSections); // default=All
    boolean aValidSection = false;
    for (int i = 0; i < sections.size(); i++) {
      // I'm simply ignoring invalid parts of the request.
      if (String2.indexOf(sectionOptions, sections.get(i)) >= 0) {
        aValidSection = true;
        break;
      } // else throw error? no. Practice forgiveness.
    }
    if (!aValidSection) {
      // throw error? no. Practice forgiveness.
      sections.clear();
      sections.add(section_All); // return all
    }
    boolean allSections = String2.indexOf(sectionOptions, section_All) >= 0;

    // don't validate the optional acceptVersions=.  Always return 1.0.0.

    // gather other information
    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String sensorGmlNameStart = getSosGmlNameStart(language, "sensor");
    String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
    String datasetObservedProperty = datasetID;
    String minAllTimeString = " indeterminatePosition=\"unknown\">";
    String maxAllTimeString = minAllTimeString;
    if (timeIndex >= 0) {
      EDVTime timeEdv = (EDVTime) dataVariables[timeIndex];
      if (timeEdv.destinationMinString().length() > 0)
        minAllTimeString = ">" + timeEdv.destinationMinString();
      if (timeEdv.destinationMaxString().length() > 0)
        maxAllTimeString = ">" + timeEdv.destinationMaxString();
    }
    EDVLat edvLat = (EDVLat) dataVariables[latIndex];
    EDVLon edvLon = (EDVLon) dataVariables[lonIndex];

    // 2013-11-19 This is patterned after IOOS 1.0 templates
    // https://code.google.com/p/ioostech/source/browse/trunk/templates/Milestone1.0/SOS-GetCapabilities.xml
    // previously
    // http://sdftest.ndbc.noaa.gov/sos/
    // http://sdftest.ndbc.noaa.gov/sos/server.php?request=GetCapabilities&service=SOS
    //    which is c:/programs/sos/ndbc_capabilities_100430.xml
    // less developed: Coops
    //    https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/
    //
    // https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/SOS?service=SOS&request=GetCapabilities
    //        which is c:/programs/sos/coops_capabilities_100430.xml
    // and [was
    // http://www.gomoos.org/cgi-bin/sos/V1.0/oostethys_sos.cgi?service=SOS&request=GetCapabilities
    // ]
    //    which is C:/programs/sos/gomoos_capabilities_100430.xml
    writer.write( // SOS GetCapabilities
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + // don't specify encoding (http does that)?
            "<Capabilities\n"
            + "  xmlns:sos=\"http://www.opengis.net/sos/1.0\"\n"
            + "  xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/sos/1.0.0 "
            + "http://schemas.opengis.net/sos/1.0.0/sosAll.xsd\" "
            + "version=\"1.0.0\">\n");
    // "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n" +  //pre 2013-11-19 had these
    // "  xmlns=\"http://www.opengis.net/sos/1.0\"\n" +
    // "  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" +
    // "  xmlns:tml=\"http://www.opengis.net/tml\"\n" +
    // "  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n" +
    // "  xmlns:myorg=\"http://www.myorg.org/features\"\n" +
    // "  version=\"" + sosVersion + "\">\n" +

    // section_ServiceIdentification
    if (allSections || String2.indexOf(sectionOptions, section_ServiceIdentification) >= 0) {
      writer.write(
          "  <ows:ServiceIdentification>\n"
              + "    <ows:Title>"
              + XML.encodeAsXML(title(language))
              + "</ows:Title>\n"
              + "    <ows:Abstract>"
              + XML.encodeAsXML(summary(language))
              + "</ows:Abstract>\n"
              + "    <ows:Keywords>\n");
      String keywordsSA[] = keywords(language);
      for (String s : keywordsSA)
        writer.write("      <ows:Keyword>" + XML.encodeAsXML(s) + "</ows:Keyword>\n");
      writer.write(
          "    </ows:Keywords>\n"
              + "    <ows:ServiceType codeSpace=\"http://opengeospatial.net\">OGC:SOS</ows:ServiceType>\n"
              + "    <ows:ServiceTypeVersion>"
              + sosVersion
              + "</ows:ServiceTypeVersion>\n"
              + "    <ows:Fees>"
              + XML.encodeAsXML(fees(language))
              + "</ows:Fees>\n"
              + "    <ows:AccessConstraints>"
              + XML.encodeAsXML(accessConstraints(language))
              + "</ows:AccessConstraints>\n"
              + "  </ows:ServiceIdentification>\n");
    } // end of section_ServiceIdentification

    // section_ServiceProvider
    if (allSections || String2.indexOf(sectionOptions, section_ServiceProvider) >= 0) {
      writer.write(
          "  <ows:ServiceProvider>\n"
              + "    <ows:ProviderName>"
              + XML.encodeAsXML(EDStatic.config.adminInstitution)
              + "</ows:ProviderName>\n"
              + "    <ows:ProviderSite xlink:href=\""
              + XML.encodeAsXML(tErddapUrl)
              + "\"/>\n"
              + "    <ows:ServiceContact>\n"
              + "      <ows:IndividualName>"
              + XML.encodeAsXML(EDStatic.config.adminIndividualName)
              + "</ows:IndividualName>\n"
              + "      <ows:ContactInfo>\n"
              + "        <ows:Phone>\n"
              + "          <ows:Voice>"
              + XML.encodeAsXML(EDStatic.config.adminPhone)
              + "</ows:Voice>\n"
              + "        </ows:Phone>\n"
              + "        <ows:Address>\n"
              + "          <ows:DeliveryPoint>"
              + XML.encodeAsXML(EDStatic.config.adminAddress)
              + "</ows:DeliveryPoint>\n"
              + "          <ows:City>"
              + XML.encodeAsXML(EDStatic.config.adminCity)
              + "</ows:City>\n"
              + "          <ows:AdministrativeArea>"
              + XML.encodeAsXML(EDStatic.config.adminStateOrProvince)
              + "</ows:AdministrativeArea>\n"
              + "          <ows:PostalCode>"
              + XML.encodeAsXML(EDStatic.config.adminPostalCode)
              + "</ows:PostalCode>\n"
              + "          <ows:Country>"
              + XML.encodeAsXML(EDStatic.config.adminCountry)
              + "</ows:Country>\n"
              + "          <ows:ElectronicMailAddress>"
              + XML.encodeAsXML(EDStatic.config.adminEmail)
              + "</ows:ElectronicMailAddress>\n"
              + "        </ows:Address>\n"
              + "      </ows:ContactInfo>\n"
              + "    </ows:ServiceContact>\n"
              + "  </ows:ServiceProvider>\n");
    } // end of section_ServiceProvider

    // section_OperationsMetadata
    if (allSections || String2.indexOf(sectionOptions, section_OperationsMetadata) >= 0) {
      writer.write(
          "  <ows:OperationsMetadata>\n"
              +

              // GetCapabilities
              "    <ows:Operation name=\"GetCapabilities\">\n"
              + "      <ows:DCP>\n"
              + "        <ows:HTTP>\n"
              + "          <ows:Get xlink:href=\""
              + sosUrl
              + "?\"/>\n"
              + // 52N has ? at end of Get URL
              "          <ows:Post xlink:href=\""
              + sosUrl
              + "\"/>\n"
              + // erddap supports POST???
              "        </ows:HTTP>\n"
              + "      </ows:DCP>\n"
              + "      <ows:Parameter name=\"service\">\n"
              + // IOOS template and 52N server list this below, as a common parameter.
              "        <ows:AllowedValues>\n"
              + "          <ows:Value>SOS</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              +
              // IOOS template and 52N server incorrectly list "version" below, as a common
              // parameter.
              // The OGC SOS spec Tables 3, 4, and 5 list "AcceptVersions", not "version".
              "      <ows:Parameter name=\"AcceptVersions\">\n"
              + // ioos template doesn't have this!!!  52N does.
              "        <ows:AllowedValues>\n"
              + "          <ows:Value>1.0.0</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"Sections\">\n"
              + "        <ows:AllowedValues>\n"
              + "          <ows:Value>ServiceIdentification</ows:Value>\n"
              + "          <ows:Value>ServiceProvider</ows:Value>\n"
              + "          <ows:Value>OperationsMetadata</ows:Value>\n"
              +
              // 52N has <ows:Value>Filter_Capabilities</ows:Value>
              "          <ows:Value>Contents</ows:Value>\n"
              + "          <ows:Value>All</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "    </ows:Operation>\n"
              +

              // DescribeSensor
              "    <ows:Operation name=\"DescribeSensor\">\n"
              + "      <ows:DCP>\n"
              + "        <ows:HTTP>\n"
              + "          <ows:Get xlink:href=\""
              + sosUrl
              + "?\"/>\n"
              + // 52N has ? at end of Get URL
              "          <ows:Post xlink:href=\""
              + sosUrl
              + "\"/>\n"
              + // allow???
              "        </ows:HTTP>\n"
              + "      </ows:DCP>\n"
              + "      <ows:Parameter name=\"service\">\n"
              + // IOOS template and 52N server list this below, as a common parameter.
              "        <ows:AllowedValues>\n"
              + "          <ows:Value>SOS</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"version\">\n"
              + // IOOS template and 52N server incorrectly list this below, as a common parameter.
              "        <ows:AllowedValues>\n"
              + "          <ows:Value>1.0.0</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"outputFormat\">\n"
              + "        <ows:AllowedValues>\n"
              + // value specified by IOOS SOS template
              "          <ows:Value>text/xml;subtype=\"sensorML/1.0.1/profiles/ioos_sos/1.0\"</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              +
              // as of 2010-05-02, GetCapabilities from http://sdftest.ndbc.noaa.gov/sos/ doesn't
              // list DescribeSensor procedures!
              "      <ows:Parameter name=\"procedure\">\n"
              + "        <ows:AllowedValues>\n");

      for (int i = 0; i < sosOfferings.size(); i++)
        writer.write(
            "          <ows:Value>"
                + sensorGmlNameStart
                + sosOfferings.getString(i)
                + "</ows:Value>\n");

      writer.write(
          "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "    </ows:Operation>\n"
              +

              // GetObservation
              "    <ows:Operation name=\"GetObservation\">\n"
              + "      <ows:DCP>\n"
              + "        <ows:HTTP>\n"
              + "          <ows:Get xlink:href=\""
              + sosUrl
              + "?\"/>\n"
              + // 52N has ? at end of Get URL
              "          <ows:Post xlink:href=\""
              + sosUrl
              + "\"/>\n"
              + // allow???
              "        </ows:HTTP>\n"
              + "      </ows:DCP>\n"
              + "      <ows:Parameter name=\"service\">\n"
              + // IOOS template and 52N server list this below, as a common parameter.
              "        <ows:AllowedValues>\n"
              + "          <ows:Value>SOS</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"version\">\n"
              + // IOOS template and 52N server incorrectly list this below, as a common parameter.
              "        <ows:AllowedValues>\n"
              + "          <ows:Value>1.0.0</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"observedProperty\">\n"
              + "        <ows:AllowedValues>\n"
              + "          <ows:Value>"
              + datasetObservedProperty
              + "</ows:Value>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"responseFormat\">\n"
              + "        <ows:AllowedValues>\n");
      for (int i = 0; i < sosDataResponseFormats.size(); i++)
        writer.write("          <ows:Value>" + sosDataResponseFormats.get(i) + "</ows:Value>\n");
      writer.write(
          "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"eventTime\">\n"
              + "        <ows:AllowedValues>\n"
              + "          <ows:Range>\n"
              + "            <ows:MinimumValue"
              + minAllTimeString
              + "</ows:MinimumValue>\n"
              + "            <ows:MaximumValue>now</ows:MaximumValue>\n"
              + // ???
              "          </ows:Range>\n"
              + "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              + "      <ows:Parameter name=\"procedure\">\n"
              + "        <ows:AllowedValues>\n");

      for (int i = 0; i < sosOfferings.size(); i++)
        writer.write(
            "          <ows:Value>"
                + sensorGmlNameStart
                + sosOfferings.getString(i)
                + "</ows:Value>\n");

      writer.write(
          "        </ows:AllowedValues>\n"
              + "      </ows:Parameter>\n"
              +
              // "      <ows:Parameter name=\"resultModel\">\n" +
              // "        <ows:AllowedValues>\n" +
              // "          <ows:Value>om:Observation</ows:Value>\n" +
              // "        </ows:AllowedValues>\n" +
              // "      </ows:Parameter>\n" +
              "    </ows:Operation>\n"
              +

              // IOOS template and 52N server list "service" and "version" Parameters here, as if
              // common to all.
              // But version is not common to all. I list service and version explicitly above
              // (where appropriate).

              // IOOS Template adds this. I'm not entirely convinced. But okay.
              /* 2017-03-22 Link is broken. They use GitHub now.  New link????
              "    <ows:ExtendedCapabilities>\n" +
              "      <gml:metaDataProperty xlink:title=\"ioosTemplateVersion\" " +
                      "xlink:href=\"http://code.google.com/p/ioostech/source/browse/#svn%2Ftrunk%2Ftemplates%2FMilestone1.0\">\n" +
              "        <gml:version>1.0</gml:version>\n" +
              "      </gml:metaDataProperty>\n" +
              "    </ows:ExtendedCapabilities>\n" +
              */
              // IOOS template has <ows:ExtendedCapabilities> here to point to
              // "ioosTemplateVersion".
              // That may or may not be a good idea.
              // I'm not doing it: It isn't essential to list the template.
              //  The template provides no additional information.
              //  The template isn't actually a service. The template isn't a service of *this*
              // server.
              "  </ows:OperationsMetadata>\n");
    } // end of section_OperationsMetadata

    // !!!52N supports a section here called Filter_Capabilities

    // section_Contents
    if (allSections || String2.indexOf(sectionOptions, section_Contents) >= 0) {
      writer.write(
          """
                <Contents>
                  <ObservationOfferingList>
              """);

      // IOOS Template says to offer network-all, not individual stations
      writer.write(
          "      <sos:ObservationOffering gml:id=\"network-all\">\n"
              + "        <gml:description>All stations</gml:description>\n"
              + "        <gml:name>urn:ioos:network:"
              + datasetID
              + ":all</gml:name>\n"
              + // AUTHORITY!!!???
              "        <gml:srsName>EPSG:4326</gml:srsName>\n"
              +
              // "        <!-- Always use EPSG:4326 as CRS for 2D coordinates -->\n" +
              "        <gml:boundedBy>\n"
              + "          <gml:Envelope srsName=\"http://www.opengis.net/def/crs/EPSG/0/4326\">\n"
              + "            <gml:lowerCorner>"
              + // !!! This assumes unknown lon range is still +/-180, not 0 - 360!!!
              (Double.isFinite(edvLon.destinationMinDouble()) ? edvLon.destinationMinString() : -90)
              + " "
              + (Double.isFinite(edvLat.destinationMinDouble())
                  ? edvLat.destinationMinString()
                  : -180)
              + "</gml:lowerCorner>\n"
              + "            <gml:upperCorner>"
              + (Double.isFinite(edvLon.destinationMaxDouble())
                  ? edvLon.destinationMaxString()
                  : 90)
              + " "
              + (Double.isFinite(edvLat.destinationMaxDouble())
                  ? edvLat.destinationMaxString()
                  : 180)
              + "</gml:upperCorner>\n"
              + "          </gml:Envelope>\n"
              + "        </gml:boundedBy>\n"
              + "        <sos:time>\n"
              + "          <gml:TimePeriod>\n"
              +
              // "            <!-- Time period allows a variety of fixed (ISO 8601) and evaluated
              // (now) expressions -->\n" +
              "            <gml:beginPosition"
              + minAllTimeString
              + "</gml:beginPosition>\n"
              + "            <gml:endPosition"
              + maxAllTimeString
              + "</gml:endPosition>\n"
              + "          </gml:TimePeriod>\n"
              + "        </sos:time>\n"
              +
              // "        <!-- SOS 2.0 only allows one procedure. List only one for future
              // compatibility -->\n" +
              "        <sos:procedure xlink:href=\"urn:ioos:network:"
              + datasetID
              + ":all\"/>\n"); // AUTHORITY!!!???

      // observedProperties are part of what is requested.
      for (int v = 0; v < dataVariables.length; v++) {
        if (v != latIndex && v != lonIndex && v != altIndex && v != depthIndex && v != timeIndex) {
          EDV edv = dataVariables[v];
          String stdName = edv.combinedAttributes().getString(language, "standard_name");
          if (stdName != null)
            writer.write(
                "        <sos:observedProperty xlink:href=\"http://mmisw.org/ont/cf/parameter/"
                    + String2.modifyToBeFileNameSafe(stdName)
                    + // just to be safe
                    "\"/>\n");
        }
      }

      // "        <sos:observedProperty
      // xlink:href=\"http://mmisw.org/ont/cf/parameter/COMPOSITE_VALUE\"/>\n" +

      writer.write(
          "        <sos:featureOfInterest xlink:href=\"FEATURE\"/>\n"
              + // FEATURE!!!???
              "        <sos:responseFormat>text/xml;schema=\"om/1.0.0/profiles/ioos_sos/1.0\"</sos:responseFormat>\n"
              + "        <sos:resultModel>om:ObservationCollection</sos:resultModel>\n"
              + "        <sos:responseMode>inline</sos:responseMode>\n"
              + "      </sos:ObservationOffering>\n");

      // note that accessibleViaSOS() has insured
      //  sosMinLon, sosMaxLon, sosMinLat, sosMaxLat, sosMinTime, sosMaxTime,
      //  and sosOfferings are valid

      /* pre 2013-11-20 system with ObservationOffering for each station.
              //go through the offerings, e.g. 100 stations.    -1 is for sosNetworkOfferingType [datasetID]
              int nOfferings = sosOfferings.size();
              for (int offering = -1; offering < nOfferings; offering++) {
                  String offeringType = offering == -1? sosNetworkOfferingType : sosOfferingType;
                  String offeringName = offering == -1? datasetID : sosOfferings.getString(offering);
                  String gmlName = (offering == -1? networkGmlNameStart : stationGmlNameStart) + offeringName;
                  //datasetID is name of 'sensor' at that station
                  String sensorName = sensorGmlNameStart + offeringName; // + ":" + datasetID;

                  double minLon  = Double.NaN, maxLon  = Double.NaN,
                         minLat  = Double.NaN, maxLat  = Double.NaN,
                         minTime = Double.NaN, maxTime = Double.NaN;
                  if (offering == -1) {
                      minLon  = dataVariables[lonIndex].destinationMinDouble();
                      maxLon  = dataVariables[lonIndex].destinationMaxDouble();
                      minLat  = dataVariables[latIndex].destinationMinDouble();
                      maxLat  = dataVariables[latIndex].destinationMaxDouble();
                  } else {
                      minLon  = sosMinLon.getNiceDouble(offering);
                      maxLon  = sosMaxLon.getNiceDouble(offering);
                      minLat  = sosMinLat.getNiceDouble(offering);
                      maxLat  = sosMaxLat.getNiceDouble(offering);
                      minTime = sosMinTime.getDouble(offering);
                      maxTime = sosMaxTime.getDouble(offering);
                  }
                  if (Double.isNaN(minTime)) minTime = dataVariables[timeIndex].destinationMinDouble();
                  if (Double.isNaN(maxTime)) maxTime = dataVariables[timeIndex].destinationMaxDouble();

                  String minTimeString = Double.isNaN(minTime)?
                      " indeterminatePosition=\"unknown\">" :
                      ">" + Calendar2.epochSecondsToIsoStringTZ(minTime);
                  String maxTimeString =
                      Double.isNaN(maxTime)?
                          " indeterminatePosition=\"unknown\">" :
                      System.currentTimeMillis() - maxTime < 3 * Calendar2.MILLIS_PER_DAY? //has data within last 3 days
                          " indeterminatePosition=\"now\">" :
                          ">" + Calendar2.epochSecondsToIsoStringTZ(maxTime);

                  writer.write(
      "      <ObservationOffering gml:id=\"" + offeringType + "-" +
                  offeringName + "\">\n" +
      "        <gml:description>" + offeringType + " " + offeringName + "</gml:description>\n" +  //a better description would be nice
      "        <gml:name>" + gmlName + "</gml:name>\n" +
      "        <gml:srsName>urn:ogc:def:crs:epsg::4326</gml:srsName>\n" +  //gomoos doesn't have this
      "        <gml:boundedBy>\n" +
      "          <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n" + //gomoos has ...EPSG:6.5:4326
      "            <gml:lowerCorner>" + minLat + " " + minLon + "</gml:lowerCorner>\n" +
      "            <gml:upperCorner>" + maxLat + " " + maxLon + "</gml:upperCorner>\n" +
      "          </gml:Envelope>\n" +
      "        </gml:boundedBy>\n" +
      "        <time>\n" +
      "          <gml:TimePeriod xsi:type=\"gml:TimePeriodType\">\n" +
      "            <gml:beginPosition" + minTimeString + "</gml:beginPosition>\n" +
      "            <gml:endPosition"   + maxTimeString + "</gml:endPosition>\n" +
      "          </gml:TimePeriod>\n" +
      "        </time>\n");

                   if (offering == -1) {
                       //procedures are names of all stations
                       for (int offering2 = 0; offering2 < nOfferings; offering2++)
                           writer.write(
      "        <procedure xlink:href=\"" +
                               XML.encodeAsXML(stationGmlNameStart + sosOfferings.getString(offering2)) + "\"/>\n");

                   } else {
                       //procedure is name of sensor.
                       writer.write(
      "        <procedure xlink:href=\"" + XML.encodeAsXML(sensorName) + "\"/>\n");
                   }

                   writer.write(
      "        <observedProperty xlink:href=\"" +
                       XML.encodeAsXML(fullPhenomenaDictionaryUrl + "#" + datasetObservedProperty) + "\"/>\n");
                   for (int dv = 0; dv < dataVariables.length; dv++) {
                       if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                           dv == timeIndex || dv == sosOfferingIndex)
                           continue;
                       writer.write(
      "        <observedProperty xlink:href=\"" +
                           XML.encodeAsXML(fullPhenomenaDictionaryUrl + "#" + dataVariables[dv].destinationName()) + "\"/>\n");
                   }

                   writer.write(
      "        <featureOfInterest xlink:href=\"" + XML.encodeAsXML(sosFeatureOfInterest()) + "\"/>\n");
                   for (int rf = 0; rf < sosDataResponseFormats.length; rf++)
                       writer.write(
      "        <responseFormat>" + XML.encodeAsXML(sosDataResponseFormats[rf]) + "</responseFormat>\n");
                   for (int rf = 0; rf < sosImageResponseFormats.length; rf++)
                       writer.write(
      "        <responseFormat>" + XML.encodeAsXML(sosImageResponseFormats[rf]) + "</responseFormat>\n");
                   writer.write(
      //???different for non-SOS-XML, e.g., csv???
      "        <resultModel>om:Observation</resultModel>\n" +
      //"        <responseMode>inline</responseMode>\n" +
      //"        <responseMode>out-of-band</responseMode>\n" +
      "      </ObservationOffering>\n");
              }
              End of old system of observationOffering each station */

      writer.write(
          """
                  </ObservationOfferingList>
                </Contents>
              """);
    } // end of section_Contents

    // end of getCapabilities
    writer.write("</Capabilities>\n");

    // essential
    writer.flush();
  }

  /**
   * This returns the SOS phenomenaDictionary xml for this dataset, i.e., the response to
   * [tErddapUrl]/sos/[datasetID]/[sosPhenomenaDictionaryUrl]. This should only be called if
   * accessibleViaSOS is true and loggedInAs has access to this dataset (so redirected to login,
   * instead of getting an error here).
   *
   * <p>This seeks to mimic IOOS SOS servers like 52N (the OGC reference implementation)
   * https://sensorweb.demo.52north.org/52nSOSv3.2.1/sos and ndbcSosWind
   * https://sdf.ndbc.noaa.gov/sos/ in particular https://ioos.github.io/sos-dif/dif/welcome.html
   * https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd
   *
   * @param language the index of the selected language
   * @param writer In the end, the writer is flushed, not closed.
   */
  public void sosPhenomenaDictionary(int language, Writer writer) throws Throwable {

    // if (accessibleViaSOS().length() > 0)
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        accessibleViaSOS());

    // String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    // String dictUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl;

    String codeSpace = getSosGmlNameStart(language, "phenomena");
    codeSpace = codeSpace.substring(0, codeSpace.length() - 2); // remove ::
    String destNames[] = dataVariableDestinationNames();
    int ndv = destNames.length;

    writer.write( // sosPhenomenaDictionary
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<gml:Dictionary gml:id=\"PhenomenaDictionary0.6.1\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.2\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + // 2009-08-26 I changed from ../.. links to actual links for xsd's below
            "  xsi:schemaLocation=\"http://www.opengis.net/swe/1.0.1 http://schemas.opengis.net/sweCommon/1.0.1/ \n"
            +
            // was http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/OGC/sweCommon/1.0.2/phenomenon.xsd
            // //GONE!
            "http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\"\n"
            +
            // was "http://www.csc.noaa.gov/ioos/schema/IOOS-DIF/OGC/gml/3.2.1/gml.xsd\"\n" +
            // //GONE!
            "  >\n"
            + "  <gml:description>Dictionary of observed phenomena for "
            + datasetID
            + ".</gml:description>\n"
            + "  <gml:identifier codeSpace=\""
            + codeSpace
            + "\">PhenomenaDictionary</gml:identifier>\n");

    // the variables
    int count = 0;
    for (int dv = 0; dv < ndv; dv++) {
      if (dv == lonIndex
          || dv == latIndex
          || dv == altIndex
          || dv == depthIndex
          || dv == timeIndex
          || dv == sosOfferingIndex) continue;
      count++;
      String standardName =
          dataVariables[dv].combinedAttributes().getString(language, "standard_name");
      boolean hasSN = standardName != null && standardName.length() > 0;
      writer.write(
          "  <gml:definitionMember >\n"
              + "    <swe:Phenomenon gml:id=\""
              + destNames[dv]
              + "\">\n"
              + "      <gml:description>"
              + XML.encodeAsXML(dataVariables[dv].longName())
              + "</gml:description>\n"
              + "      <gml:identifier codeSpace=\""
              + (hasSN
                  ? sosStandardNamePrefix(language)
                      .substring(0, sosStandardNamePrefix(language).length() - 1)
                  : codeSpace)
              + "\">"
              + (hasSN ? standardName : destNames[dv])
              + "</gml:identifier>\n"
              + "    </swe:Phenomenon>\n"
              + "  </gml:definitionMember>\n");
    }

    // the composite
    writer.write(
        "  <gml:definitionMember >\n"
            + "    <swe:CompositePhenomenon gml:id=\""
            + datasetID
            + "\" dimension=\""
            + count
            + "\">\n"
            + "      <gml:description>"
            + XML.encodeAsXML(title(language))
            + "</gml:description>\n"
            + "      <gml:identifier codeSpace=\""
            + codeSpace
            + "\">"
            + datasetID
            + "</gml:identifier>\n");
    for (int dv = 0; dv < ndv; dv++) {
      if (dv == lonIndex
          || dv == latIndex
          || dv == altIndex
          || dv == depthIndex
          || dv == timeIndex
          || dv == sosOfferingIndex) continue;
      writer.write("      <swe:component xlink:href=\"#" + destNames[dv] + "\"/>\n");
    }

    writer.write(
        """
                        </swe:CompositePhenomenon>
                      </gml:definitionMember>
                    </gml:Dictionary>
                    """);

    writer.flush();
  }

  /**
   * This responds to a sos describeSensor request. This should only be called if accessibleViaSOS
   * is true and loggedInAs has access to this dataset (so redirected to login, instead of getting
   * an error here).
   *
   * <p>This seeks to mimic IOOS SOS servers like 52N (the OGC reference implementation)
   * https://sensorweb.demo.52north.org/52nSOSv3.2.1/sos and IOOS SOS servers like
   * https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/get/describesensor/getstation.jsp
   *
   * @param language the index of the selected language
   * @param loggedInAs
   * @param shortName the short "procedure" name (from the query, but shortened): the network name
   *     (the datasetID, for all) or a station name (41004). It must be already checked for
   *     validity.
   * @param writer In the end, the writer is flushed, not closed.
   */
  public void sosDescribeSensor(
      HttpServletRequest request, int language, String loggedInAs, String shortName, Writer writer)
      throws Throwable {

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);

    // get sensor info
    boolean isNetwork = shortName.equals(datasetID);
    double minTimeD = Double.NaN, maxTimeD = Double.NaN;
    String offeringType, minLon, maxLon, minLat, maxLat;
    if (isNetwork) {
      offeringType = sosNetworkOfferingType;
      EDV lonEdv = dataVariables[lonIndex];
      EDV latEdv = dataVariables[latIndex];
      minLon = lonEdv.destinationMinString();
      maxLon = lonEdv.destinationMaxString();
      minLat = latEdv.destinationMinString();
      maxLat = latEdv.destinationMaxString();
    } else {
      offeringType = sosOfferingType;
      int which = sosOfferings.indexOf(shortName);
      if (which < 0)
        // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
        // Erddap section
        // "deal
        // with SOS error"
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "procedure="
                + shortName
                + " isn't a valid sensor name.");
      minLon = sosMinLon.getString(which);
      maxLon = sosMaxLon.getString(which);
      minLat = sosMinLat.getString(which);
      maxLat = sosMaxLat.getString(which);
      minTimeD = sosMinTime.getDouble(which);
      maxTimeD = sosMaxTime.getDouble(which);
    }
    if (Double.isNaN(minTimeD)) minTimeD = dataVariables[timeIndex].destinationMinDouble();
    if (Double.isNaN(maxTimeD)) maxTimeD = dataVariables[timeIndex].destinationMaxDouble();
    String minTimeString =
        Double.isNaN(minTimeD)
            ? " indeterminatePosition=\"unknown\">"
            : ">" + Calendar2.epochSecondsToIsoStringTZ(minTimeD);
    String maxTimeString =
        Double.isNaN(maxTimeD)
            ? " indeterminatePosition=\"unknown\">"
            : System.currentTimeMillis() - maxTimeD < 3 * Calendar2.MILLIS_PER_DAY
                ? // has data within last 3 days
                " indeterminatePosition=\"now\">"
                : ">" + Calendar2.epochSecondsToIsoStringTZ(maxTimeD);

    String tType = isNetwork ? sosNetworkOfferingType : offeringType;
    String name2 = tType + "-" + shortName;
    String gmlName = getSosGmlNameStart(language, tType) + shortName;

    // based on
    // https://opendap.co-ops.nos.noaa.gov/ioos-dif-sos-test/get/describesensor/getstation.jsp
    writer.write( // sosDescribeSensor
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<sml:SensorML\n"
            + "  xmlns:sml=\"http://www.opengis.net/sensorML/1.0.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/sensorML/1.0.1 http://schemas.opengis.net/sensorML/1.0.1/sensorML.xsd\"\n"
            + "  version=\"1.0.1\"\n"
            + // the sensorML version, not the sosVersion
            "  >\n"
            + "  <!-- This SOS server is an EXPERIMENTAL WORK-IN-PROGRESS. -->\n"
            + "  <sml:member>\n"
            + "    <sml:System gml:id=\""
            + name2
            + "\">\n"
            + "      <gml:description>"
            + XML.encodeAsXML(title(language) + ", " + name2)
            + "</gml:description>\n"
            + "      <sml:keywords>\n"
            + "        <sml:KeywordList>\n"); // codeSpace=\"https://wiki.earthdata.nasa.gov/display/CMR/GCMD+Keyword+Access\">\n" +
    String keywordsSA[] = keywords(language);
    for (String s : keywordsSA)
      writer.write("          <sml:keyword>" + XML.encodeAsXML(s) + "</sml:keyword>\n");
    writer.write(
        "        </sml:KeywordList>\n"
            + "      </sml:keywords>\n"
            + "\n"
            +

            // identification
            "      <sml:identification>\n"
            + "        <sml:IdentifierList>\n"
            + "          <sml:identifier name=\"Station ID\">\n"
            + // true for "network", too?
            "            <sml:Term definition=\"urn:ioos:station:organization:stationID:\">\n"
            + "              <sml:codeSpace xlink:href=\""
            + tErddapUrl
            + "\" />\n"
            + "              <sml:value>"
            + gmlName
            + "</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            + "          <sml:identifier name=\"Short Name\">\n"
            + "            <sml:Term definition=\"urn:ioos:identifier:organization:shortName\">\n"
            + "              <sml:value>"
            + shortName
            + "</sml:value>\n"
            + "            </sml:Term>\n"
            + "          </sml:identifier>\n"
            +
            // "          <sml:identifier name=\"Long Name\">\n" +
            // "            <sml:Term definition=\"urn:ogc:def:identifier:OGC:longName\">\n" +
            // "              <sml:value>Providence</sml:value>\n" +
            // "            </sml:Term>\n" +
            // "          </sml:identifier>\n" +
            // "          <sml:identifier name=\"Vertical Datum MHW\">\n" +
            // "            <sml:Term definition=\"urn:ioos:datum:noaa:MHW:\">\n" +
            // "              <sml:value>2.464</sml:value>\n" +
            // "            </sml:Term>\n" +
            // "          </sml:identifier>\n" +
            //           and vertical datums for other sensors
            "        </sml:IdentifierList>\n"
            + "      </sml:identification>\n"
            + "\n"
            +

            // classification
            "      <sml:classification>\n"
            + "        <sml:ClassifierList>\n");

    if (!isNetwork)
      writer.write(
          "          <sml:classifier name=\"Parent Network\">\n"
              + "            <sml:Term definition=\"urn:ioos:classifier:NOAA:parentNetwork\">\n"
              + "              <sml:codeSpace xlink:href=\""
              + tErddapUrl
              + "\" />\n"
              + "              <sml:value>"
              + getSosGmlNameStart(language, sosNetworkOfferingType)
              + datasetID
              + "</sml:value>\n"
              + "            </sml:Term>\n"
              + "          </sml:classifier>\n");

    writer.write(
        // "          <sml:classifier name=\"System Type Name\">\n" +
        // "            <sml:Term definition=\"urn:ioos:classifier:NOAA:systemTypeName\">\n" +
        // "              <sml:codeSpace xlink:href=\"http://tidesandcurrents.noaa.gov\" />\n" +
        // "              <sml:value>CO-OPS Observational System</sml:value>\n" +
        // "            </sml:Term>\n" +
        // "          </sml:classifier>\n" +
        "          <sml:classifier name=\"System Type Identifier\">\n"
            + "            <sml:Term definition=\"urn:ioos:classifier:NOAA:systemTypeID\">\n"
            + "              <sml:codeSpace xlink:href=\"http://mmisw.org/ont/mmi/platform/\" />\n"
            + "              <sml:value>Platform</sml:value>\n"
            + // ERDDAP treats all datasets and stations as 'platforms'
            "            </sml:Term>\n"
            + "          </sml:classifier>\n"
            + "        </sml:ClassifierList>\n"
            + "      </sml:classification>\n"
            + "\n"
            +

            // time
            "      <sml:validTime>\n"
            + "        <gml:TimePeriod gml:id=\"MetadataApplicabilityTime\">\n"
            + "          <gml:beginPosition"
            + minTimeString
            + "</gml:beginPosition>\n"
            + "          <gml:endPosition"
            + maxTimeString
            + "</gml:endPosition>\n"
            + "        </gml:TimePeriod>\n"
            + "      </sml:validTime>\n"
            + "\n"
            +

            // contact
            "      <sml:contact xlink:role=\"urn:ogc:def:classifiers:OGC:contactType:provider\">\n"
            + // was owner
            "        <sml:ResponsibleParty>\n"
            +
            // bob added:
            "          <sml:individualName>"
            + XML.encodeAsXML(EDStatic.config.adminIndividualName)
            + "</sml:individualName>\n"
            + "          <sml:organizationName>"
            + XML.encodeAsXML(EDStatic.config.adminInstitution)
            + "</sml:organizationName>\n"
            + "          <sml:contactInfo>\n"
            + "            <sml:phone>\n"
            + "              <sml:voice>"
            + XML.encodeAsXML(EDStatic.config.adminPhone)
            + "</sml:voice>\n"
            + "            </sml:phone>\n"
            + "            <sml:address>\n"
            + "              <sml:deliveryPoint>"
            + XML.encodeAsXML(EDStatic.config.adminAddress)
            + "</sml:deliveryPoint>\n"
            + "              <sml:city>"
            + XML.encodeAsXML(EDStatic.config.adminCity)
            + "</sml:city>\n"
            + "              <sml:administrativeArea>"
            + XML.encodeAsXML(EDStatic.config.adminStateOrProvince)
            + "</sml:administrativeArea>\n"
            + "              <sml:postalCode>"
            + XML.encodeAsXML(EDStatic.config.adminPostalCode)
            + "</sml:postalCode>\n"
            + "              <sml:country>"
            + XML.encodeAsXML(EDStatic.config.adminCountry)
            + "</sml:country>\n"
            +
            // bob added:
            "              <sml:electronicMailAddress>"
            + XML.encodeAsXML(EDStatic.config.adminEmail)
            + "</sml:electronicMailAddress>\n"
            + "            </sml:address>\n"
            + "            <sml:onlineResource xlink:href=\""
            + tErddapUrl
            + "\" />\n"
            + "          </sml:contactInfo>\n"
            + "        </sml:ResponsibleParty>\n"
            + "      </sml:contact>\n"
            + "\n"
            +

            // documentation
            "      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:webPage\">\n"
            + "        <sml:Document>\n"
            + "          <gml:description>Web page with background information from the source of this dataset</gml:description>\n"
            + "          <sml:format>text/html</sml:format>\n"
            + "          <sml:onlineResource xlink:href=\""
            + XML.encodeAsXML(infoUrl(language))
            + "\" />\n"
            +
            // "          <sml:onlineResource xlink:href=\"" + tErddapUrl + "/info/" + datasetID +
            // ".html\" />\n" +
            "        </sml:Document>\n"
            + "      </sml:documentation>\n"
            + "\n");

    //            writer.write(
    // "      <sml:documentation xlink:arcrole=\"urn:ogc:def:role:objectImage\">\n" +
    // "        <sml:Document>\n" +
    // "          <gml:description>Photo or diagram illustrating the system (or a representative
    // instance) described in this SensorML record</gml:description>\n" +
    // "          <sml:format>image/jpg</sml:format>\n" +
    // "          <sml:onlineResource
    // xlink:href=\"http://tidesandcurrents.noaa.gov/images/photos/8454000a.jpg\" />\n" +
    // "        </sml:Document>\n" +
    // "      </sml:documentation>\n" +
    // "\n");

    // location
    if (minLon.equals(maxLon) && minLat.equals(maxLat))
      writer.write(
          "      <sml:location>\n"
              + "        <gml:Point srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\""
              + offeringType
              + "_LatLon\">\n"
              + "          <gml:coordinates>"
              + minLat
              + " "
              + minLon
              + "</gml:coordinates>\n"
              + "        </gml:Point>\n"
              + "      </sml:location>\n"
              + "\n");
    //            else writer.write(
    //                //bob tried to add, but gml:Box is invalid; must be Point or _Curve.
    //                //xml validator says sml:location isn't required
    // "      <sml:location>\n" +
    // "        <gml:Box srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\"" + offeringType +
    // "_Box\">\n" +  //Box was "location"
    // "          <gml:coordinates>" + minLat + "," + minLon + " " + maxLat + "," + maxLon +
    // "</gml:coordinates>\n" +
    // "        </gml:Box>\n" +
    // "      </sml:location>\n" +
    // "\n");

    // components   (an sml:component for each sensor)
    writer.write(
        """
                  <sml:components>
                    <sml:ComponentList>
            """);

    // a component for each variable
    /*            {
                    for (int dv = 0; dv < dataVariables.length; dv++) {
                        if (dv == lonIndex  || dv == latIndex || dv == altIndex || dv == depthIndex ||
                            dv == timeIndex || dv == sosOfferingIndex)
                            continue;
                        EDV edv = dataVariables[dv];
                        String destName = edv.destinationName();
                        writer.write(
    "          <sml:component name=\"" + destName + " Instrument\">\n" +
    "            <sml:System gml:id=\"sensor-" + name2 + "-" + destName + "\">\n" +
    "              <gml:description>" + name2 + "-" + destName + " Sensor</gml:description>\n" +
    "              <sml:identification xlink:href=\"" + gmlName + ":" + destName + "\" />\n" +  //A1:wspd
    "              <sml:documentation xlink:href=\"" + tErddapUrl + "/info/" + datasetID + ".html\" />\n" +
    "              <sml:outputs>\n" +
    "                <sml:OutputList>\n");
                        String codeSpace = getSosGmlNameStart("phenomena");
                        String stdName = edv.combinedAttributes().getString("standard_name");
                        boolean hasSN = stdName != null && stdName.length() > 0;
                        writer.write(
    "                  <sml:output name=\"" + destName + "\">\n" + //but NOS has longName
    "                    <swe:Quantity definition=\"" +
                         (hasSN? sosStandardNamePrefix() + stdName : codeSpace + destName) +
                         "\">\n");
                        if (edv.ucumUnits() != null && edv.ucumUnits().length() > 0)
                            writer.write(
    "                      <swe:uom code=\"" + edv.ucumUnits() + "\" />\n");
                        writer.write(
    "                    </swe:Quantity>\n" +
    "                  </sml:output>\n" +
    "                </sml:OutputList>\n" +
    "              </sml:outputs>\n" +
    "            </sml:System>\n" +
    "          </sml:component>\n");
                    }
                }
    */

    // component for all variables
    {
      writer.write(
          "          <sml:component name=\""
              + datasetID
              + " Instrument\">\n"
              + "            <sml:System gml:id=\"sensor-"
              + name2
              + "-"
              + datasetID
              + "\">\n"
              + "              <gml:description>"
              + name2
              + " Platform</gml:description>\n"
              + "              <sml:identification xlink:href=\""
              + XML.encodeAsXML(gmlName)
              + // ":" + datasetID +  //41004:cwwcNDBCMet
              "\" />\n"
              + "              <sml:documentation xlink:href=\""
              + tErddapUrl
              + "/info/"
              + datasetID
              + ".html\" />\n"
              + "              <sml:outputs>\n"
              + "                <sml:OutputList>\n");
      String codeSpace = getSosGmlNameStart(language, "phenomena");
      for (int dv = 0; dv < dataVariables.length; dv++) {
        if (dv == lonIndex
            || dv == latIndex
            || dv == altIndex
            || dv == depthIndex
            || dv == timeIndex
            || dv == sosOfferingIndex) continue;
        EDV edv = dataVariables[dv];
        String stdName = edv.combinedAttributes().getString(language, "standard_name");
        boolean hasSN = stdName != null && stdName.length() > 0;
        writer.write(
            "                  <sml:output name=\""
                + edv.destinationName()
                + "\">\n"
                + // but NOS has longName
                "                    <swe:Quantity definition=\""
                + (hasSN
                    ? sosStandardNamePrefix(language) + stdName
                    : codeSpace + edv.destinationName())
                + "\">\n");
        if (edv.ucumUnits() != null && edv.ucumUnits().length() > 0)
          writer.write("                      <swe:uom code=\"" + edv.ucumUnits() + "\" />\n");
        writer.write(
            """
                                    </swe:Quantity>
                                  </sml:output>
                """);
      }
      writer.write(
          """
                                      </sml:OutputList>
                                    </sml:outputs>
                                  </sml:System>
                                </sml:component>
                      """);
    }

    writer.write(
        """
                            </sml:ComponentList>
                          </sml:components>
                        </sml:System>
                      </sml:member>
                    </sml:SensorML>
                    """);

    /*   * <p>This seeks to mimic IOOS SOS servers like ndbcSosWind.
         * See https://sdf.ndbc.noaa.gov/sos/ .
         * The url might be something like
         *    https://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor&service=SOS
         *    &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
         *    &procedure=urn:ioos:sensor:noaa.nws.ndbc:41012:adcp0

            writer.write(
    //???this was patterned after C:/programs/sos/ndbcSosCurrentsDescribeSensor90810.xml
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<sml:Sensor \n" +
    "  xmlns:sml=\"https://www.w3.org/2001/XMLSchema\" \n" +
    "  xmlns:xlink=\"https://www.w3.org/1999/xlink\" \n" +
    "  xmlns:gml=\"http://www.opengis.net/gml/3.2\" \n" +
    "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\" \n" + //ogc doesn't show 1.0.2
    "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\">\n" +
    "<!-- This is a PROTOTYPE service.  The information in this response is NOT complete. -->\n" +
    "  <sml:identification>\n" +
    "    <sml:IdentifierList/>\n" +
    "  </sml:identification>\n" +
    "  <sml:inputs>\n" +
    "    <sml:InputList/>\n" +
    "  </sml:inputs>\n" +
    "  <sml:outputs>\n" +
    "    <sml:OutputList/>\n" +
    "  </sml:outputs>\n" +
    "  <sml:positions>\n" +
    "    <sml:PositionList>\n" +
    "      <sml:position name=\"southwestCorner\">\n" +
    "        <swe:GeoLocation>\n" +
    "          <swe:latitude>\n" +
    "            <swe:Quantity>" + minLat + "</swe:Quantity>\n" +
    "          </swe:latitude>\n" +
    "          <swe:longitude>\n" +
    "            <swe:Quantity>" + minLon + "</swe:Quantity>\n" +
    "          </swe:longitude>\n" +
    "        </swe:GeoLocation>\n" +
    "      </sml:position>\n" +
    "      <sml:position name=\"southeastCorner\">\n" +
    "        <swe:GeoLocation>\n" +
    "          <swe:latitude>\n" +
    "            <swe:Quantity>" + minLat + "</swe:Quantity>\n" +
    "          </swe:latitude>\n" +
    "          <swe:longitude>\n" +
    "            <swe:Quantity>" + maxLon + "</swe:Quantity>\n" +
    "          </swe:longitude>\n" +
    "        </swe:GeoLocation>\n" +
    "      </sml:position>\n" +
    "      <sml:position name=\"northeastCorner\">\n" +
    "        <swe:GeoLocation>\n" +
    "          <swe:latitude>\n" +
    "            <swe:Quantity>" + maxLat + "</swe:Quantity>\n" +
    "          </swe:latitude>\n" +
    "          <swe:longitude>\n" +
    "            <swe:Quantity>" + maxLon + "</swe:Quantity>\n" +
    "          </swe:longitude>\n" +
    "        </swe:GeoLocation>\n" +
    "      </sml:position>\n" +
    "      <sml:position name=\"northwestCorner\">\n" +
    "        <swe:GeoLocation>\n" +
    "          <swe:latitude>\n" +
    "            <swe:Quantity>" + maxLat + "</swe:Quantity>\n" +
    "          </swe:latitude>\n" +
    "          <swe:longitude>\n" +
    "            <swe:Quantity>" + minLon + "</swe:Quantity>\n" +
    "          </swe:longitude>\n" +
    "        </swe:GeoLocation>\n" +
    "      </sml:position>\n" +
    "    </sml:PositionList>\n" +
    "  </sml:positions>\n" +
    "</sml:Sensor>\n");
    */
    // essential
    writer.flush();
  }

  /**
   * This returns the fileTypeName (e.g., .csvp) which corresponds to the data or image
   * responseFormat.
   *
   * @param responseFormat
   * @return returns the fileTypeName (e.g., .csvp) which corresponds to the data or image
   *     responseFormat (or null if no match).
   */
  public static String sosResponseFormatToFileTypeName(String responseFormat) {
    if (responseFormat == null) return null;
    int po = sosDataResponseFormats.indexOf(responseFormat);
    if (po >= 0) return sosTabledapDataResponseTypes.get(po);
    po = sosImageResponseFormats.indexOf(responseFormat);
    if (po >= 0) return sosTabledapImageResponseTypes.get(po);
    return null;
  }

  /**
   * This indicates if the responseFormat (from a sos GetObservation request) is one of the IOOS SOS
   * XML format response types.
   *
   * @param language the index of the selected language
   * @param responseFormat
   * @throws SimpleException if trouble (e.g., not a valid responseFormat)
   */
  public static boolean isIoosSosXmlResponseFormat(int language, String responseFormat) {
    String tabledapType = sosResponseFormatToFileTypeName(responseFormat);
    if (tabledapType == null)
      // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
      // Erddap section
      // "deal with
      // SOS error"
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "responseFormat="
              + responseFormat
              + " is invalid.");
    // true if valid (tested above) and "ioos" is in the name
    return responseFormat.indexOf("ioos") >= 0;
  }

  /**
   * This indicates if the responseFormat (from a sos GetObservation request) is the Oostethys SOS
   * XML format response types.
   *
   * @param language the index of the selected language
   * @param responseFormat
   * @throws SimpleException if trouble (e.g., not a valid responseFormat)
   */
  public static boolean isOostethysSosXmlResponseFormat(int language, String responseFormat) {
    return sosDataResponseFormats.get(sosOostethysDataResponseFormat).equals(responseFormat);
  }

  /**
   * This responds to a SOS query. This should only be called if accessibleViaSOS is "" and
   * loggedInAs has access to this dataset (so redirected to login, instead of getting an error
   * here); this doesn't check.
   *
   * <p>This seeks to mimic IOOS SOS servers like 52N (the OGC reference implementation)
   * https://sensorweb.demo.52north.org/52nSOSv3.2.1/sos and ndbcSosWind
   * https://sdf.ndbc.noaa.gov/sos/ .
   *
   * @param request the request
   * @param language the index of the selected language
   * @param sosQuery
   * @param ipAddress The user's IP address (for statistics).
   * @param loggedInAs or null if not logged in
   * @param outputStreamSource if all goes well, this method calls out.close() at the end.
   * @param dir the directory to use for a cache file
   * @param fileName the suggested name for a file
   * @throws Exception if trouble (e.g., invalid query parameter)
   */
  public void sosGetObservation(
      HttpServletRequest request,
      int language,
      String endOfRequest,
      String sosQuery,
      String ipAddress,
      String loggedInAs,
      OutputStreamSource outputStreamSource,
      String dir,
      String fileName)
      throws Throwable {

    if (reallyVerbose) String2.log("\nrespondToSosQuery q=" + sosQuery);
    String requestUrl = "/sos/" + datasetID + "/" + sosServer;

    if (accessibleViaSOS().length() > 0)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + accessibleViaSOS());

    // parse the sosQuery
    String dapQueryAr[] = sosQueryToDapQuery(request, language, loggedInAs, sosQuery);
    String dapQuery = dapQueryAr[0];
    String offeringType = dapQueryAr[1]; // "network" or e.g., "station"),
    String offeringName = dapQueryAr[2]; // datasetID (all) or 41004
    String responseFormat = dapQueryAr[3];
    // String responseMode   = dapQueryAr[4];
    if (reallyVerbose) String2.log("dapQueryAr=" + String2.toCSSVString(dapQueryAr));

    // find the fileTypeName (e.g., .tsvp) (sosQueryToDapQuery ensured responseFormat was valid)
    String fileTypeName = sosResponseFormatToFileTypeName(responseFormat);

    OutputStream out = null;
    try {
      if (isIoosSosXmlResponseFormat(language, responseFormat)
          || isOostethysSosXmlResponseFormat(language, responseFormat)) {

        // *** handle the IOOS or OOSTETHYS SOS XML responses
        // get the data
        // Unfortunately, this approach precludes streaming the results.
        TableWriterAllWithMetadata twawm =
            getTwawmForDapQuery(language, loggedInAs, requestUrl, dapQuery);
        try {
          if (twawm.nRows() == 0)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (from source)");

          // convert UDUNITS to UCUM units  (before direct use of twawm or get cumulativeTable)
          if (EDStatic.config.units_standard.equals("UDUNITS")) {
            int nColumns = twawm.nColumns();
            for (int col = 0; col < nColumns; col++) {
              Attributes atts = twawm.columnAttributes(col);
              String units = atts.getString("units");
              if (units != null) atts.set("units", Units2.safeUdunitsToUcum(units));
            }
          }

          // get the cumulativeTable
          // It will be reused in sosObservationsXml.
          // Do this now, so if not enough memory,
          //    error will be thrown before get outputStream.
          twawm.ensureMemoryForCumulativeTable();

          // write the results
          // all likely errors are above, so it is now ~safe to get outputstream
          out = outputStreamSource.outputStream(File2.UTF_8);
          Writer writer = File2.getBufferedWriterUtf8(out);
          if (isIoosSosXmlResponseFormat(language, responseFormat))
            sosObservationsXmlInlineIoos(
                request, language, offeringType, offeringName, twawm, writer, loggedInAs);
          else if (isOostethysSosXmlResponseFormat(language, responseFormat))
            sosObservationsXmlInlineOostethys(
                request, language, offeringType, offeringName, twawm, writer, loggedInAs);
        } finally {
          try {
            twawm.releaseResources();
            twawm.close();
          } catch (Exception e) {
          }
        }
      } else {
        // *** handle all other file types
        // add the units function to dapQuery?
        if (!EDStatic.config.units_standard.equals("UCUM")) dapQuery += "&units(%22UCUM%22)";

        respondToDapQuery(
            language,
            null,
            null,
            ipAddress,
            loggedInAs,
            requestUrl,
            endOfRequest,
            dapQuery,
            outputStreamSource,
            dir,
            fileName,
            fileTypeName);
      }
    } finally {
      try {
        if (out != null) {
          if (out instanceof ZipOutputStream zos) zos.closeEntry();
          out.close();
        }
      } catch (Exception e) {
      }
    }
  }

  /**
   * This converts a SOS GetObservation query into an OPeNDAP user query. See
   * http://www.oostethys.org/best-practices/best-practices-get [GONE]
   *
   * @param the request
   * @param language the index of the selected language
   * @param loggedInAs
   * @param sosQuery
   * @return [0]=dapQuery (percent encoded), [1]=requestOfferingType (e.g., "network" or e.g.,
   *     "station"), [2]=requestShortOfferingName (short name: e.g., all or 41004)
   *     [3]=responseFormat (in request) //[4]=responseMode (e.g., inline) [5]=requestedVars (csv of
   *     short observedProperties)
   * @throws Exception if trouble (e.g., invalid query parameter)
   */
  public String[] sosQueryToDapQuery(
      HttpServletRequest request, int language, String loggedInAs, String sosQuery)
      throws Throwable {

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    Map<String, String> sosQueryMap = userQueryHashMap(sosQuery, true);

    // parse the query and build the dapQuery
    // https://sdf.ndbc.noaa.gov/sos/server.php?service=SOS&version=1.0.0
    // &request=GetObservation
    // &offering=urn:ioos:station:noaa.nws.ndbc:41004:
    // or:                      network                all or datasetID
    // &observedProperty=Winds
    // &responseFormat=text/xml;schema=%22ioos/0.6.1%22
    // &eventTime=2008-08-01T00:00:00Z/2008-08-01T04:00:00Z
    // or
    // featureOfInterest=BBOX:<min_lon>,<min_lat>,<max_lon>,<max_lat>
    StringBuilder dapQuery = new StringBuilder();

    // service   required
    String service = sosQueryMap.get("service"); // test name.toLowerCase()
    if (service == null || !service.equals("SOS"))
      // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
      // Erddap section
      // "deal with
      // SOS error"
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "service="
              + service
              + " should have been \"SOS\".");

    // version    required
    String version = sosQueryMap.get("version"); // test name.toLowerCase()
    if (version == null || !version.equals(sosVersion))
      // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
      // Erddap section
      // "deal with
      // SOS error"
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "version="
              + version
              + " should have been \""
              + sosVersion
              + "\".");

    // request    required
    String sosRequest = sosQueryMap.get("request"); // test name.toLowerCase()
    if (sosRequest == null || !sosRequest.equals("GetObservation"))
      // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
      // Erddap section
      // "deal with
      // SOS error"
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "request="
              + sosRequest
              + " should have been \"GetObservation\".");

    // srsName  SOS required; erddap optional (default=4326, the only one supported)
    String srsName = sosQueryMap.get("srsname"); // test name.toLowerCase()
    if (srsName != null && !srsName.endsWith(":4326"))
      // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
      // Erddap section
      // "deal with
      // SOS error"
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "srsName="
              + srsName
              + " should have been \"urn:ogc:def:crs:epsg::4326\".");

    // observedProperty      (spec: 1 or more required; erddap: csv list or none (default=all vars)
    //   long (phenomenaDictionary#...) or short (datasetID or edvDestinationName)
    // don't add vars to dapQuery now. do it later.
    String dictStart = tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl + "#";
    String obsProp = sosQueryMap.get("observedproperty"); // test name.toLowerCase()
    if (obsProp == null || obsProp.length() == 0) obsProp = datasetID;
    String properties[] = String2.split(obsProp, ',');
    StringArray requestedVars = new StringArray();
    for (String s : properties) {
      String property = s;
      if (property.startsWith(dictStart)) property = property.substring(dictStart.length());
      if (property.equals(datasetID)) {
        requestedVars.clear();
        break;
      } else {
        int dv = String2.indexOf(dataVariableDestinationNames, property);
        if (dv < 0
            || dv == lonIndex
            || dv == latIndex
            || dv == altIndex
            || dv == depthIndex
            || dv == timeIndex
            || dv == sosOfferingIndex)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "observedProperty="
                  + property
                  + " is invalid.");
        requestedVars.add(property);
      }
    }

    // offering    1, required
    String requestOffering = sosQueryMap.get("offering"); // test name.toLowerCase()
    if (requestOffering == null) requestOffering = "";
    String stationGmlNameStart = getSosGmlNameStart(language, sosOfferingType);
    String networkGmlName = getSosGmlNameStart(language, sosNetworkOfferingType) + datasetID;
    if (reallyVerbose)
      String2.log(
          "stationGmlNameStart=" + stationGmlNameStart + "\nnetworkGmlName=" + networkGmlName);
    String requestOfferingType, requestShortOfferingName;
    // long or short network name
    int whichOffering = -1;
    if (requestOffering.equals(networkGmlName) || requestOffering.equals(datasetID)) {
      requestOfferingType = sosNetworkOfferingType;
      requestShortOfferingName = datasetID;
      whichOffering = -1;

      // long or short station name
    } else {
      requestOfferingType = sosOfferingType;
      requestShortOfferingName = requestOffering;
      // make station offering short
      if (requestShortOfferingName.startsWith(stationGmlNameStart)
          && requestShortOfferingName.lastIndexOf(':') == stationGmlNameStart.length() - 1)
        requestShortOfferingName =
            requestShortOfferingName.substring(stationGmlNameStart.length()); // e.g., 41004

      whichOffering = sosOfferings.indexOf(requestShortOfferingName);
      if (whichOffering < 0)
        // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
        // Erddap section
        // "deal
        // with SOS error"
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "offering="
                + requestOffering
                + " is invalid.");
      dapQuery.append(
          "&"
              + dataVariables[sosOfferingIndex].destinationName()
              + "="
              + SSR.minimalPercentEncode("\"" + requestShortOfferingName + "\""));
    }

    // procedure    forbidden
    String procedure = sosQueryMap.get("procedure"); // test name.toLowerCase()
    if (procedure != null)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "\"procedure\" is not supported.");

    // result    forbidden
    String result = sosQueryMap.get("result"); // test name.toLowerCase()
    if (result != null)
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR) + "\"result\" is not supported.");

    // responseMode  SOS & erddap optional (default="inline")
    // String responseMode = sosQueryMap.get("responsemode"); //test name.toLowerCase()
    // if (responseMode == null)
    //    responseMode = "inline";
    // if (!responseMode.equals("inline") && !responseMode.equals("out-of-band"))
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        "responseMode=" + responseMode + " should have been \"inline\" or \"out-of-band\".");

    // responseFormat
    String responseFormat = sosQueryMap.get("responseformat"); // test name.toLowerCase()
    String fileTypeName = sosResponseFormatToFileTypeName(responseFormat);
    if (fileTypeName == null)
      // this format EDStatic.messages.get(Message.QUERY_ERROR, language) + "xxx=" is parsed by
      // Erddap section
      // "deal with
      // SOS error"
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "responseFormat="
              + responseFormat
              + " is invalid.");
    // if ((isIoosSosXmlResponseFormat(responseFormat) ||
    //     isOostethysSosXmlResponseFormat(responseFormat)) &&
    //    responseMode.equals("out-of-band"))
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        "For responseFormat=" + responseFormat + ", responseMode=" + responseMode + " must be
    // \"inline\".");

    // resultModel    optional, must be om:Observation
    String resultModel = sosQueryMap.get("resultmodel"); // test name.toLowerCase()
    if (resultModel != null && !resultModel.equals("om:Observation"))
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "resultModel="
              + resultModel
              + " should have been \"om:Observation\".");

    // eventTime   optional start or start/end               spec allow 0 or many
    String eventTime = sosQueryMap.get("eventtime"); // test name.toLowerCase()
    if (eventTime == null) {
      // if not specified, get latest time's data (within last week)
      double maxTime =
          whichOffering == -1
              ? dataVariables[timeIndex].destinationMaxDouble()
              : sosMaxTime.getDouble(whichOffering);
      double minTime = Calendar2.backNDays(7, maxTime);
      dapQuery.append(
          "&time>="
              + Calendar2.epochSecondsToIsoStringTZ(minTime)
              + "&orderByMax"
              + SSR.minimalPercentEncode("(\"time\")"));
    } else {
      int spo = eventTime.indexOf('/');
      if (spo < 0) dapQuery.append("&time=" + eventTime);
      else {
        int lpo = eventTime.lastIndexOf('/');
        if (lpo != spo)
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "ERDDAP doesn't support '/resolution' in eventTime=beginTime/endTime/resolution.");
        dapQuery.append(
            "&time>=" + eventTime.substring(0, spo) + "&time<=" + eventTime.substring(spo + 1));
      }
    }

    // featureOfInterest  optional BBOX:minLon,minLat,maxLon,maxLat
    // spec allows 0 or more, advertised or a constraint
    String foi = sosQueryMap.get("featureofinterest"); // test name.toLowerCase()
    if (foi == null) {
    } else if (foi.toLowerCase().startsWith("bbox:")) { // test.toLowerCase
      String bboxSA[] = String2.split(foi.substring(5), ',');
      if (bboxSA.length != 4)
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "featureOfInterest=BBOX must have 4 comma-separated values.");

      double d1 = String2.parseDouble(bboxSA[0]);
      double d2 = String2.parseDouble(bboxSA[2]);
      if (d1 == d2) { // NaN==NaN? returns false
        dapQuery.append("&longitude=" + d1);
      } else {
        if (!Double.isNaN(d1)) dapQuery.append("&longitude>=" + d1);
        if (!Double.isNaN(d2)) dapQuery.append("&longitude<=" + d2);
      }

      d1 = String2.parseDouble(bboxSA[1]);
      d2 = String2.parseDouble(bboxSA[3]);
      if (d1 == d2) { // NaN==NaN? returns false
        dapQuery.append("&latitude=" + d1);
      } else {
        if (!Double.isNaN(d1)) dapQuery.append("&latitude>=" + d1);
        if (!Double.isNaN(d2)) dapQuery.append("&latitude<=" + d2);
      }
    } else {
      throw new SimpleException(
          EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
              + "featureOfInterest="
              + foi
              + " is invalid. Only BBOX is allowed.");
    }

    // add dataVariables to start of dapQuery
    String requestedVarsCsv = String2.replaceAll(requestedVars.toString(), " ", "");
    if (responseFormat.equals(OutputStreamFromHttpResponse.KML_MIME_TYPE)) {

      // GoogleEarth -- always all variables

    } else if (sosImageResponseFormats.indexOf(responseFormat) >= 0) {

      // Note that any requestedVars will be dv names (not the dataset name, which leads to
      // requestedVars="")

      // responseFormat is an image type
      if (requestedVars.size() == 0) {
        throw new SimpleException(
            EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                + "For image responseFormats (e.g., "
                + responseFormat
                + "), observedProperty must list one or more simple observedProperties.");

      } else if (requestedVars.size() == 1) {

        EDV edv1 = findDataVariableByDestinationName(requestedVars.get(0));
        if (edv1.destinationDataPAType().equals(PAType.STRING))
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "For image responseFormats, observedProperty="
                  + requestedVars.get(0)
                  + " can't be a String phenomena.");

        if (whichOffering == -1) {
          // network -> map
          dapQuery.insert(0, "longitude,latitude," + requestedVars.get(0));
          dapQuery.append("&.draw=markers&.marker=5|4"); // filled square|smaller
        } else {
          // 1 station, 1 var -> time series
          dapQuery.insert(0, "time," + requestedVars.get(0));
          dapQuery.append("&.draw=linesAndMarkers&.marker=5|4&.color=0xFF9900");
        }

      } else {
        // 1 station, >=2 vars -> use first 2 vars
        EDV edv1 = findDataVariableByDestinationName(requestedVars.get(0));
        EDV edv2 = findDataVariableByDestinationName(requestedVars.get(1));
        if (edv1.destinationDataPAType().equals(PAType.STRING))
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "For image responseFormats, observedProperty="
                  + requestedVars.get(0)
                  + " can't be a String phenomena.");
        if (edv2.destinationDataPAType().equals(PAType.STRING))
          throw new SimpleException(
              EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                  + "For image responseFormats, observedProperty="
                  + requestedVars.get(1)
                  + " can't be a String phenomena.");

        if (edv1.ucumUnits() != null
            && edv2.ucumUnits() != null
            && edv1.ucumUnits().equals(edv2.ucumUnits())) {

          // the 2 variables have the same ucumUnits
          if (whichOffering == -1) {
            // network -> map with vectors
            dapQuery.insert(
                0, "longitude,latitude," + requestedVars.get(0) + "," + requestedVars.get(1));
            dapQuery.append("&.draw=vectors&.color=0xFF9900");
            String colorBarMax =
                edv1.combinedAttributes()
                    .getString(language, "colorBarMaximum"); // string avoids rounding problems
            if (!Double.isNaN(String2.parseDouble(colorBarMax)))
              dapQuery.append("&.vec=" + colorBarMax);
          } else {
            // 1 station, 2 compatible var -> sticks
            dapQuery.insert(0, "time," + requestedVars.get(0) + "," + requestedVars.get(1));
            dapQuery.append("&.draw=sticks&.color=0xFF9900");
          }
        } else {
          // the 2 variables don't have compatible ucumUnits
          if (whichOffering == -1) {
            // network -> map with markers
            dapQuery.insert(0, "longitude,latitude," + requestedVars.get(0));
            dapQuery.append("&.draw=markers&.marker=5|4"); // filled square
          } else {
            // 1 station, 2 incompatible vars -> var vs. var, with time as color
            dapQuery.insert(0, requestedVars.get(0) + "," + requestedVars.get(1) + ",time");
            dapQuery.append("&.draw=linesAndMarkers&.marker=5|4&.color=0xFF9900");
          }

          // leftover?
          // dapQuery.insert(0, "time," + requestedVars.get(0));
          // dapQuery.append("&.draw=linesAndMarkers&.marker=5|3&.color=0xFF9900");
        }
      }

    } else {

      // responseFormat is a data type  (not IOOS XML; it is excluded above)
      if (requestedVars.size() > 0) {
        // specific variables
        String llatiVars =
            "longitude,latitude"
                + (altIndex >= 0 ? ",altitude" : depthIndex >= 0 ? ",depth" : "")
                + ",time,"
                + dataVariables[sosOfferingIndex].destinationName()
                + ",";
        dapQuery.insert(0, llatiVars + requestedVarsCsv);
      } // else "" gets all variables
    }

    if (reallyVerbose)
      String2.log(
          "sosQueryToDapQuery="
              + dapQuery
              + "\n  requestOfferingType="
              + requestOfferingType
              + " requestShortOfferingName="
              + requestShortOfferingName);
    return new String[] {
      dapQuery.toString(),
      requestOfferingType,
      requestShortOfferingName,
      responseFormat,
      null, // responseMode,
      requestedVars.toString()
    };
  }

  /**
   * This returns a SOS *out-of-band* response. See Observations and Measurements schema OGC
   * 05-087r3 section 7.3.2.
   *
   * @param request the request
   * @param language the index of the selected language
   * @param requestShortOfferingName the short version
   * @param fileTypeName e.g., .csv or .smallPng?
   * @param dapQuery percent-encoded
   * @param requestedVars csv of short observedProperties, or "" if all
   */
  public void sosObservationsXmlOutOfBand(
      HttpServletRequest request,
      int language,
      String requestOfferingType,
      String requestShortOfferingName,
      String fileTypeName,
      String dapQuery,
      String requestedVars,
      Writer writer,
      String loggedInAs)
      throws Throwable {

    // if (accessibleViaSOS().length() > 0)
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        accessibleViaSOS());

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String stationGmlNameStart = getSosGmlNameStart(language, sosOfferingType);
    String sensorGmlNameStart = getSosGmlNameStart(language, "sensor");
    String requestedVarAr[] =
        requestedVars.length() == 0 ? new String[0] : String2.split(requestedVars, ',');

    // parse the dapQuery
    String minLon = null,
        maxLon = null,
        minLat = null,
        maxLat = null,
        minTime = null,
        maxTime = null;
    String parts[] =
        Table.getDapQueryParts(dapQuery); // decoded.  always at least 1 part (may be "")
    for (String part : parts) {
      if (part.startsWith("longitude>=")) {
        minLon = part.substring(11);
      } else if (part.startsWith("longitude<=")) {
        maxLon = part.substring(11);
      } else if (part.startsWith("longitude=")) {
        minLon = part.substring(10);
        maxLon = part.substring(10);
      } else if (part.startsWith("latitude>=")) {
        minLat = part.substring(10);
      } else if (part.startsWith("latitude<=")) {
        maxLat = part.substring(10);
      } else if (part.startsWith("latitude=")) {
        minLat = part.substring(9);
        maxLat = part.substring(9);
      } else if (part.startsWith("time>=")) {
        minTime = part.substring(6);
      } else if (part.startsWith("time<=")) {
        maxTime = part.substring(6);
      } else if (part.startsWith("time=")) {
        minTime = part.substring(5);
        maxTime = part.substring(5);
      }
    }

    // find stations which match
    boolean allStations = requestShortOfferingName.equals(datasetID);
    int nSosOfferings = sosOfferings.size();
    BitSet keepBitset = new BitSet(nSosOfferings); // all clear
    if (allStations) {
      keepBitset.set(0, nSosOfferings); // all set
      if (minLon != null && maxLon != null) { // they should both be null, or both be not null
        double minLonD = String2.parseDouble(minLon);
        double maxLonD = String2.parseDouble(maxLon);
        int offering = keepBitset.nextSetBit(0);
        while (offering >= 0) {
          if (sosMaxLon.getDouble(offering) < minLonD || sosMinLon.getDouble(offering) > maxLonD)
            keepBitset.clear(offering);
          offering = keepBitset.nextSetBit(offering + 1);
        }
      }
      if (minLat != null && maxLat != null) { // they should both be null, or both be not null
        double minLatD = String2.parseDouble(minLat);
        double maxLatD = String2.parseDouble(maxLat);
        int offering = keepBitset.nextSetBit(0);
        while (offering >= 0) {
          if (sosMaxLat.getDouble(offering) < minLatD || sosMinLat.getDouble(offering) > maxLatD)
            keepBitset.clear(offering);
          offering = keepBitset.nextSetBit(offering + 1);
        }
      }
      if (minTime != null && maxTime != null) { // they should both be null, or both be not null
        double minTimeD = String2.parseDouble(minTime);
        double maxTimeD = String2.parseDouble(maxTime);
        int offering = keepBitset.nextSetBit(0);
        double days3 = 3 * Calendar2.SECONDS_PER_DAY;
        while (offering >= 0) {
          // time is different: min or max may be NaN
          double stationMinTime = sosMinTime.getDouble(offering);
          double stationMaxTime =
              sosMaxTime.getDouble(offering) + days3; // pretend it has 3 more days of data
          if ((!Double.isNaN(stationMaxTime) && stationMaxTime < minTimeD)
              || (!Double.isNaN(stationMinTime) && stationMinTime > maxTimeD))
            keepBitset.clear(offering);
          offering = keepBitset.nextSetBit(offering + 1);
        }
      }
    } else {
      keepBitset.set(sosOfferings.indexOf(requestShortOfferingName));
    }
    int nValidOfferings = keepBitset.cardinality();
    if (nValidOfferings == 0)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (There are no valid offerings.)");

    // write the response
    writer.write( // sosObservationsXmlOutOfBand
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:CompositeObservation xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\"\n"
            + // ogc doesn't show 1.0.2
            "  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n"
            + "  gml:id=\""
            + datasetID
            + // was WindsPointCollection
            "TimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>"
            + datasetID
            + // was Winds
            " observations at a series of times</gml:description>\n"
            + "  <gml:name>"
            + XML.encodeAsXML(title(language))
            + // was NOAA.NWS.NDBC
            ", "
            + requestOfferingType
            + " "
            + requestShortOfferingName
            + // was  observations at station ID 41004
            "</gml:name>\n");

    if (minLon != null && minLat != null && maxLon != null && maxLat != null) {
      writer.write(
          "  <gml:boundedBy>\n"
              + "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
              + "      <gml:lowerCorner>"
              + minLat
              + " "
              + minLon
              + "</gml:lowerCorner>\n"
              + // lat lon
              "      <gml:upperCorner>"
              + maxLat
              + " "
              + maxLon
              + "</gml:upperCorner>\n"
              + "    </gml:Envelope>\n"
              + "  </gml:boundedBy>\n");
    }

    if (minTime != null && maxTime != null) {
      writer.write(
          "  <om:samplingTime>\n"
              + "    <gml:TimePeriod gml:id=\"ST\">\n"
              + "      <gml:beginPosition>"
              + minTime
              + "</gml:beginPosition>\n"
              + "      <gml:endPosition>"
              + maxTime
              + "</gml:endPosition>\n"
              + "    </gml:TimePeriod>\n"
              + "  </om:samplingTime>\n");
    }

    // procedure
    writer.write(
        "  <om:procedure>\n"
            + "    <om:Process>\n"
            + "      <ioos:CompositeContext gml:id=\"SensorMetadata\""
            +
            // ??? Jeff says recDef may change or go away
            // "
            // recDef=\"https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/recordDefinitions/PointSensorMetadataRecordDefinition.xml\"" +  //GONE
            ">\n"
            + "        <gml:valueComponents>\n"
            + "          <ioos:Count name=\"NumberOf"
            + sosOfferingType
            + "s\">"
            + nValidOfferings
            + // plural=s goofy but simple
            "</ioos:Count>\n"
            + "          <ioos:ContextArray gml:id=\""
            + sosOfferingType
            + "Array\">\n"
            + "            <gml:valueComponents>\n");

    // for each station
    int count = 0;
    int offering = keepBitset.nextSetBit(0);
    while (offering >= 0) {
      // write station info
      count++;
      writer.write(
          "              <ioos:CompositeContext gml:id=\""
              + sosOfferingType
              + count
              + "Info\">\n"
              + "                <gml:valueComponents>\n"
              +
              // ???ioos hard codes "Station"
              "                  <ioos:StationName>"
              + sosOfferingType
              + " - "
              + sosOfferings.getString(offering)
              + "</ioos:StationName>\n"
              + "                  <ioos:Organization>"
              + institution(language)
              + "</ioos:Organization>\n"
              + "                  <ioos:StationId>"
              + stationGmlNameStart
              + sosOfferings.getString(offering)
              + "</ioos:StationId>\n");

      if (sosMinLon.getString(offering).equals(sosMaxLon.getString(offering)))
        writer.write(
            // Already reported to Jeff: one lat lon for all time?! but some stations move a little!
            "                  <gml:Point gml:id=\""
                + sosOfferingType
                + count
                + "LatLon\">\n"
                + "                    <gml:pos>"
                + sosMinLon.getString(offering)
                + " "
                + sosMinLat.getString(offering)
                + "</gml:pos>\n"
                + "                  </gml:Point>\n");
      else
        writer.write(
            "                  <gml:boundedBy>\n"
                + "                    <gml:Box srsName=\"urn:ogc:def:crs:epsg::4326\" gml:id=\""
                + requestOfferingType
                + "_location\">\n"
                + "                      <gml:coordinates>"
                + sosMinLat.getString(offering)
                + ","
                + sosMinLon.getString(offering)
                + " "
                + sosMaxLat.getString(offering)
                + ","
                + sosMaxLon.getString(offering)
                + "</gml:coordinates>\n"
                + "                    </gml:Box>\n"
                + "                  </gml:boundedBy>\n");

      writer.write(
          // vertical crs: http://www.oostethys.org/best-practices/verticalcrs [GONE]
          // ???Eeek. I don't keep this info     allow it to be specified in
          // combinedGlobalAttributes???
          // But it may be different for different stations (e.g., buoy vs affixed to dock)
          //  or even within a dataset (ndbcMet: wind/air measurements vs water measurements)
          "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n"
              +
              // ???one vertical position!!!     for now, always missing value
              "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n"
              + "                  <ioos:Count name=\""
              + sosOfferingType
              + count
              + "NumberOfSensors\">"
              + (requestedVarAr.length == 0 ? 1 : requestedVarAr.length)
              + "</ioos:Count>\n"
              + "                  <ioos:ContextArray gml:id=\""
              + sosOfferingType
              + count
              + "SensorArray\">\n"
              + "                    <gml:valueComponents>\n");

      // these are the sensors in the response (not the available sensors)
      if (requestedVarAr.length == 0) {
        // sensor for entire dataset
        writer.write(
            "                      <ioos:CompositeContext gml:id=\""
                + sosOfferingType
                + count
                + "Sensor1Info\">\n"
                + "                        <gml:valueComponents>\n"
                + "                          <ioos:SensorId>"
                + sensorGmlNameStart
                + sosOfferings.getString(offering)
                +
                // ":" + datasetID +
                "</ioos:SensorId>\n"
                +
                // IOOS sos had <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
                "                        </gml:valueComponents>\n"
                + "                      </ioos:CompositeContext>\n");
      } else {

        // sensor for each non-LLATId var
        for (int rv = 0; rv < requestedVarAr.length; rv++) {
          writer.write(
              "                      <ioos:CompositeContext gml:id=\""
                  + sosOfferingType
                  + count
                  + "Sensor"
                  + (rv + 1)
                  + "Info\">\n"
                  + "                        <gml:valueComponents>\n"
                  + "                          <ioos:SensorId>"
                  + sensorGmlNameStart
                  + sosOfferings.getString(offering)
                  + ":"
                  + requestedVarAr[rv]
                  + "</ioos:SensorId>\n"
                  +
                  // IOOS sos had <ioos:SamplingRate> <ioos:ReportingInterval>
                  // <ioos:ProcessingLevel>
                  "                        </gml:valueComponents>\n"
                  + "                      </ioos:CompositeContext>\n");
        }
      }

      writer.write(
          """
                                          </gml:valueComponents>
                                        </ioos:ContextArray>
                                      </gml:valueComponents>
                                    </ioos:CompositeContext>
                      """);

      offering = keepBitset.nextSetBit(offering + 1);
    } // end of offering loop

    writer.write(
        """
                                </gml:valueComponents>
                              </ioos:ContextArray>
                            </gml:valueComponents>
                          </ioos:CompositeContext>
                        </om:Process>
                      </om:procedure>
                    """);

    writer.write(
        // use a local phenomenaDictionary
        // "  <om:observedProperty
        // xlink:href=\"https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml#Winds\"/>\n" +
        "  <om:observedProperty xlink:href=\""
            + tErddapUrl
            + "/sos/"
            + datasetID
            + "/"
            + sosPhenomenaDictionaryUrl
            + "#"
            + datasetID
            + "\"/>\n"
            + "  <om:featureOfInterest xlink:href=\""
            + XML.encodeAsXML(sosFeatureOfInterest(language))
            + "\"/>\n"
            + "  <om:result xlink:href=\""
            + XML.encodeAsXML(tErddapUrl + "/tabledap/" + datasetID + fileTypeName + "?" + dapQuery)
            + "\""
            +
            // ???role?
            //    " xlink:role=\"application/xmpp\" +
            //    " xsi:type=\"gml:ReferenceType\" +
            "/>\n"
            + "</om:CompositeObservation>\n");

    // essential
    writer.flush();
  }

  /**
   * This returns the IOOS SOS *inline* observations xml for this dataset (see Erddap.doSos). This
   * should only be called if accessibleViaSOS is "" and loggedInAs has access to this dataset (so
   * redirected to login, instead of getting an error here); this doesn't check.
   *
   * <p>This seeks to mimic IOOS SOS servers like ndbcSosWind. See https://sdf.ndbc.noaa.gov/sos/ .
   *
   * @param request the request
   * @param language the index of the selected language
   * @param requestOfferingType e.g., network or e.g., station
   * @param requestShortOfferingName e.g., all or 41004 or ...
   * @param tw with all of the results data. Units should be UCUM already.
   * @param writer In the end, the writer is flushed, not closed.
   * @param loggedInAs the name of the logged in user (or null if not logged in).
   */
  public void sosObservationsXmlInlineIoos(
      HttpServletRequest request,
      int language,
      String requestOfferingType,
      String requestShortOfferingName,
      TableWriterAllWithMetadata tw,
      Writer writer,
      String loggedInAs)
      throws Throwable {

    // if (accessibleViaSOS().length() > 0)
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        accessibleViaSOS());

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String stationGmlNameStart = getSosGmlNameStart(language, sosOfferingType);
    String sensorGmlNameStart = getSosGmlNameStart(language, "sensor");
    String datasetObservedProperty = datasetID;

    // It's quick and easy to write out the info if cumulativeTable is available.
    // It's very cumbersome any other way.
    // It may need a lot of memory, but hopefully just for a short time (tested by caller).
    Table table = tw.cumulativeTable();
    table.convertToStandardMissingValues(); // so missing_values are NaNs

    // ensure lon, lat, time, id are present  (and alt if eddTable has alt)
    String idName = dataVariableDestinationNames[sosOfferingIndex];
    int tLonIndex = table.findColumnNumber(EDV.LON_NAME);
    int tLatIndex = table.findColumnNumber(EDV.LAT_NAME);
    int tAltIndex = table.findColumnNumber(EDV.ALT_NAME);
    int tDepthIndex = table.findColumnNumber(EDV.DEPTH_NAME);
    int tTimeIndex = table.findColumnNumber(EDV.TIME_NAME);
    int tIdIndex = table.findColumnNumber(idName);
    if (tLonIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'longitude' isn't in the response table.");
    if (tLatIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'latitude' isn't in the response table.");
    if (altIndex >= 0 && tAltIndex < 0) // yes, first one is alt, not tAlt
    throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'altitude' isn't in the response table.");
    if (depthIndex >= 0 && tDepthIndex < 0) // yes, first one is depth, not tDepth
    throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'depth' isn't in the response table.");
    if (tTimeIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0) + "'time' isn't in the response table.");
    if (tIdIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'"
              + idName
              + "' isn't in the response table.");

    // Ensure it is sorted by tIdIndex, tTimeIndex, [tAltIndex|tDepthIndex] (it usually is, but make
    // sure; it is assumed below)
    int sortIndices[] =
        tAltIndex >= 0
            ? new int[] {tIdIndex, tTimeIndex, tAltIndex}
            : tDepthIndex >= 0
                ? new int[] {tIdIndex, tTimeIndex, tDepthIndex}
                : new int[] {tIdIndex, tTimeIndex};
    boolean sortAscending[] = new boolean[sortIndices.length];
    Arrays.fill(sortAscending, true);
    table.sort(sortIndices, sortAscending);
    PrimitiveArray tIdPA = table.getColumn(tIdIndex);
    PrimitiveArray tTimePA = table.getColumn(tTimeIndex);
    PrimitiveArray tLatPA = table.getColumn(tLatIndex);
    PrimitiveArray tLonPA = table.getColumn(tLonIndex);
    PAOne minLon = tw.columnMinValue(tLonIndex);
    PAOne maxLon = tw.columnMaxValue(tLonIndex);
    PAOne minLat = tw.columnMinValue(tLatIndex);
    PAOne maxLat = tw.columnMaxValue(tLatIndex);
    int nRows = tIdPA.size();
    int nCols = table.nColumns();

    // count n stations
    String prevID = "\uFFFF";
    int nStations = 0;
    for (int row = 0; row < nRows; row++) {
      String currID = tIdPA.getString(row);
      if (!currID.equals(prevID)) {
        nStations++;
        prevID = currID;
      }
    }

    // gather column names and ucumUnits
    String xmlColNames[] = new String[nCols];
    String xmlColUcumUnits[] = new String[nCols]; // ready to use
    for (int col = 0; col < nCols; col++) {
      xmlColNames[col] = XML.encodeAsXML(table.getColumnName(col));
      String u = table.columnAttributes(col).getString("units");
      if (EDV.TIME_UCUM_UNITS.equals(u)) u = "UTC";
      xmlColUcumUnits[col] =
          u == null || u.length() == 0 ? "" : " uom=\"" + XML.encodeAsXML(u) + "\"";
    }
    int nSensors = nCols - 4 - (tAltIndex >= 0 || tDepthIndex >= 0 ? 1 : 0); // 4=LLTI
    boolean compositeSensor = nCols == dataVariables.length;

    // write start of xml content for IOOS response
    writer.write( // sosObservationsXmlInlineIoos
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:CompositeObservation xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml/3.2\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om/1.0\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/1.0.1\""
            + // ogc doesn't show 1.0.2
            "  xmlns:ioos=\"http://www.noaa.gov/ioos/0.6.1\"\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xsi:schemaLocation=\"http://www.opengis.net/om/1.0 https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/schemas/ioosObservationSpecializations.xsd\"\n"
            + "  gml:id=\""
            + datasetID
            + // was WindsPointCollection
            "TimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>"
            + datasetID
            + // was Winds
            " observations at a series of times</gml:description>\n"
            + "  <gml:name>"
            + XML.encodeAsXML(title(language))
            + ", "
            + requestOfferingType
            + " "
            + requestShortOfferingName
            + // was  observations at station ID 41004
            "</gml:name>\n"
            + "  <gml:boundedBy>\n"
            + "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
            + "      <gml:lowerCorner>"
            + minLat.getString()
            + " "
            + minLon.getString()
            + "</gml:lowerCorner>\n"
            + // lat lon
            "      <gml:upperCorner>"
            + maxLat.getString()
            + " "
            + maxLon.getString()
            + "</gml:upperCorner>\n"
            + "    </gml:Envelope>\n"
            + "  </gml:boundedBy>\n"
            + "  <om:samplingTime>\n"
            + "    <gml:TimePeriod gml:id=\"ST\">\n"
            + "      <gml:beginPosition>"
            + Calendar2.epochSecondsToIsoStringTZ(tw.columnMinValue(tTimeIndex).getDouble())
            + "</gml:beginPosition>\n"
            + "      <gml:endPosition>"
            + Calendar2.epochSecondsToIsoStringTZ(tw.columnMaxValue(tTimeIndex).getDouble())
            + "</gml:endPosition>\n"
            + "    </gml:TimePeriod>\n"
            + "  </om:samplingTime>\n"
            + "  <om:procedure>\n"
            + "    <om:Process>\n"
            + "      <ioos:CompositeContext gml:id=\"SensorMetadata\""
            +
            // ??? Jeff says recDef may change or go away
            // "
            // recDef=\"https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/recordDefinitions/PointSensorMetadataRecordDefinition.xml\"" +
            ">\n"
            + "        <gml:valueComponents>\n"
            + "          <ioos:Count name=\"NumberOf"
            + sosOfferingType
            + "s\">"
            + nStations
            + "</ioos:Count>\n"
            + // plural=s goofy but simple
            "          <ioos:ContextArray gml:id=\""
            + sosOfferingType
            + "Array\">\n"
            + "            <gml:valueComponents>\n");

    // for each station
    // find firstStationRow for next station (then write info for previous station)
    int stationNumber = 0;
    int firstStationRow = 0;
    prevID = tIdPA.getString(0);
    for (int nextFirstStationRow = 1;
        nextFirstStationRow <= nRows;
        nextFirstStationRow++) { // 1,<= because we're looking back
      String currID =
          nextFirstStationRow == nRows ? "\uFFFF" : tIdPA.getString(nextFirstStationRow);
      if (prevID.equals(currID)) continue;

      // a new station!
      // write station info
      stationNumber++; // firstStationRow and prevID are changed at end of loop
      writer.write(
          "              <ioos:CompositeContext gml:id=\""
              + sosOfferingType
              + stationNumber
              + "Info\">\n"
              + "                <gml:valueComponents>\n"
              +
              // ???ioos hard codes "Station"
              "                  <ioos:StationName>"
              + sosOfferingType
              + " - "
              + prevID
              + "</ioos:StationName>\n"
              + "                  <ioos:Organization>"
              + institution(language)
              + "</ioos:Organization>\n"
              + "                  <ioos:StationId>"
              + stationGmlNameStart
              + prevID
              + "</ioos:StationId>\n"
              + "                  <gml:Point gml:id=\""
              + sosOfferingType
              + stationNumber
              + "LatLon\">\n"
              +
              // Already reported to Jeff: one lat lon for all time?! but some stations move a
              // little!
              // And what about non-stations, e.g., trajectories?
              "                    <gml:pos>"
              + tLatPA.getNiceDouble(firstStationRow)
              + " "
              + tLonPA.getNiceDouble(firstStationRow)
              + "</gml:pos>\n"
              + "                  </gml:Point>\n"
              +
              // vertical crs: http://www.oostethys.org/best-practices/verticalcrs [GONE]
              // ???Eeek. I don't keep this info     allow it to be specified in
              // combinedGlobalAttributes???
              // But it may be different for different stations (e.g., buoy vs affixed to dock)
              //  or even within a dataset (ndbcMet: wind/air measurements vs water measurements)
              "                  <ioos:VerticalDatum>urn:ogc:def:datum:epsg::5113</ioos:VerticalDatum>\n"
              +
              // ???one vertical position!!!     for now, always missing value
              "                  <ioos:VerticalPosition xsi:nil=\"true\" nilReason=\"missing\"/>\n");

      if (compositeSensor) {
        // observedProperty was datasetID: platform's sensor
        writer.write(
            // composite observedProperty treats each station as having one observedProperty
            // and one "sensor"
            "                  <ioos:Count name=\""
                + sosOfferingType
                + stationNumber
                + "NumberOfSensors\">1</ioos:Count>\n"
                + "                  <ioos:ContextArray gml:id=\""
                + sosOfferingType
                + stationNumber
                + "SensorArray\">\n"
                + "                    <gml:valueComponents>\n"
                + "                      <ioos:CompositeContext gml:id=\""
                + sosOfferingType
                + stationNumber
                + "Sensor1Info\">\n"
                + "                        <gml:valueComponents>\n"
                + "                          <ioos:SensorId>"
                + sensorGmlNameStart
                + prevID
                + ":"
                + datasetObservedProperty
                + "</ioos:SensorId>\n"
                +
                // ioos has <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
                "                        </gml:valueComponents>\n"
                + "                      </ioos:CompositeContext>\n"
                + "                    </gml:valueComponents>\n"
                + "                  </ioos:ContextArray>\n");
      } else {
        // observedProperty was csv variable list: separate sensors
        writer.write(
            "                  <ioos:Count name=\""
                + sosOfferingType
                + stationNumber
                + "NumberOfSensors\">"
                + nSensors
                + "</ioos:Count>\n"
                + "                  <ioos:ContextArray gml:id=\""
                + sosOfferingType
                + stationNumber
                + "SensorArray\">\n"
                + "                    <gml:valueComponents>\n");
        int sensori = 0;
        for (int c = 0; c < nCols; c++) {
          if (c == tLonIndex
              || c == tLatIndex
              || c == tAltIndex
              || c == tDepthIndex
              || c == tTimeIndex
              || c == tIdIndex) continue;
          sensori++;
          writer.write(
              "                      <ioos:CompositeContext gml:id=\""
                  + sosOfferingType
                  + stationNumber
                  + "Sensor"
                  + sensori
                  + "Info\">\n"
                  + "                        <gml:valueComponents>\n"
                  + "                          <ioos:SensorId>"
                  + sensorGmlNameStart
                  + prevID
                  + ":"
                  + table.getColumnName(c)
                  + "</ioos:SensorId>\n"
                  +
                  // ioos has <ioos:SamplingRate> <ioos:ReportingInterval> <ioos:ProcessingLevel>
                  "                        </gml:valueComponents>\n"
                  + "                      </ioos:CompositeContext>\n");
        }
        writer.write(
            """
                                            </gml:valueComponents>
                                          </ioos:ContextArray>
                        """);
      }

      writer.write(
          """
                                      </gml:valueComponents>
                                    </ioos:CompositeContext>
                      """);

      firstStationRow = nextFirstStationRow;
      prevID = currID;
    }

    writer.write(
        "            </gml:valueComponents>\n"
            + "          </ioos:ContextArray>\n"
            + "        </gml:valueComponents>\n"
            + "      </ioos:CompositeContext>\n"
            + "    </om:Process>\n"
            + "  </om:procedure>\n"
            +

            // use a local phenomenaDictionary
            // "  <om:observedProperty
            // xlink:href=\"https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/dictionaries/phenomenaDictionary.xml#Winds\"/>\n" +
            "  <om:observedProperty xlink:href=\""
            + tErddapUrl
            + "/sos/"
            + datasetID
            + "/"
            + sosPhenomenaDictionaryUrl
            + "#"
            + datasetID
            + "\"/>\n"
            + "  <om:featureOfInterest xlink:href=\""
            + XML.encodeAsXML(sosFeatureOfInterest(language))
            + "\"/>\n");

    writer.write(
        "  <om:result>\n"
            +
            // ???Jeff says recordDefinitions may change or go away
            "    <ioos:Composite "
            + // recDef=\"https://ioos.github.io/sos-dif/gml/IOOS/0.6.1/recordDefinitions/WindsPointDataRecordDefinition.xml\" " +
            "gml:id=\""
            + datasetID
            + "PointCollectionTimeSeriesDataObservations\">\n"
            + "      <gml:valueComponents>\n"
            + "        <ioos:Count name=\"NumberOfObservationsPoints\">"
            + nStations
            + "</ioos:Count>\n"
            + "        <ioos:Array gml:id=\""
            + datasetID
            + "PointCollectionTimeSeries\">\n"
            + "          <gml:valueComponents>\n");

    // for each station
    // find firstStationRow for next station (then write info for previous station)
    // and nTimes for this station
    stationNumber = 0;
    firstStationRow = 0;
    prevID = tIdPA.getString(0);
    int nTimes = 0;
    double prevTime = tTimePA.getDouble(0);
    for (int nextFirstStationRow = 1;
        nextFirstStationRow <= nRows;
        nextFirstStationRow++) { // 1,<= because we're looking back
      // nTimes will be too low by 1 until nextStation found, then correct.
      double currTime =
          nextFirstStationRow == nRows ? Double.MAX_VALUE : tTimePA.getDouble(nextFirstStationRow);
      if (prevTime != currTime) {
        nTimes++;
        prevTime = currTime;
      }

      String currID =
          nextFirstStationRow == nRows ? "\uFFFF" : tIdPA.getString(nextFirstStationRow);
      if (prevID.equals(currID)) continue;

      // a new station!
      // write station info
      stationNumber++; // firstStationRow and prevID are changed at end of loop
      String stationN = sosOfferingType + stationNumber;
      writer.write(
          "            <ioos:Composite gml:id=\""
              + stationN
              + "TimeSeriesRecord\">\n"
              + "              <gml:valueComponents>\n"
              + "                <ioos:Count name=\""
              + stationN
              + "NumberOfObservationsTimes\">"
              + nTimes
              + "</ioos:Count>\n"
              + "                <ioos:Array gml:id=\""
              + stationN
              + (nTimes == nextFirstStationRow - firstStationRow ? "" : "Profile")
              + "TimeSeries\">\n"
              + "                  <gml:valueComponents>\n");

      // rows for one station
      int nthTime = 0;
      int sTimeStartRow = firstStationRow;
      for (int sRow = firstStationRow; sRow < nextFirstStationRow; sRow++) {
        double tTimeD = tTimePA.getDouble(sRow);
        if (sRow < nextFirstStationRow - 1 && tTimeD == tTimePA.getDouble(sRow + 1)) // nextTime
        continue;

        // for this station, this is the last row with this time
        nthTime++; // this is the nth time for this station (1..)
        int nTimesThisTime = sRow - sTimeStartRow + 1; // n rows share this time

        String stationNTN = sosOfferingType + stationNumber + "T" + nthTime;
        String tTimeS =
            Double.isNaN(tTimeD)
                ? " indeterminatePosition=\"unknown\">"
                : // ???correct???
                ">" + Calendar2.epochSecondsToIsoStringTZ(tTimeD);

        writer.write(
            "                    <ioos:Composite gml:id=\""
                + stationNTN
                + (nTimesThisTime == 1 ? "Point" : "Profile")
                + "\">\n"
                + "                      <gml:valueComponents>\n"
                + "                        <ioos:CompositeContext gml:id=\""
                + stationNTN
                + "ObservationConditions\" "
                + "processDef=\"#"
                + stationN
                + "Info\">\n"
                + "                          <gml:valueComponents>\n"
                + "                            <gml:TimeInstant gml:id=\""
                + stationNTN
                + "Time\">\n"
                + "                              <gml:timePosition"
                + tTimeS
                + "</gml:timePosition>\n"
                + "                            </gml:TimeInstant>\n"
                + "                          </gml:valueComponents>\n"
                + "                        </ioos:CompositeContext>\n");

        if (nTimesThisTime > 1) {
          writer.write(
              "                        <ioos:Count name=\""
                  + stationNTN
                  + "NumberOfBinObservations\">"
                  + nTimesThisTime
                  + "</ioos:Count>\n"
                  + "                        <ioos:ValueArray gml:id=\""
                  + stationNTN
                  + "ProfileObservations\">\n"
                  + "                          <gml:valueComponents>\n");
        }
        String tIndent = nTimesThisTime == 1 ? "" : "    ";

        // show the result rows that have same time
        String lastAltVal = "\u0000";
        for (int timeRow = sTimeStartRow; timeRow <= sRow; timeRow++) {
          if (compositeSensor) {

            if (nTimesThisTime == 1) {
              writer.write(
                  "                        <ioos:CompositeValue gml:id=\""
                      + stationNTN
                      + "PointObservation\" "
                      + "processDef=\"#"
                      + stationN
                      + "Sensor1Info\">\n"
                      + "                          <gml:valueComponents>\n");
            } else if (tAltIndex < 0 && tDepthIndex < 0) {
              // withinTimeIndex may not be tAltIndex or tDepthIndex,
              // so ensure there is an tAltIndex or tDepthIndex
              throw new SimpleException(
                  EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                      + "This dataset is not suitable for SOS: there are multiple values for one time, but there is no altitude or depth variable.");
            } else {
              // withinTimeIndex may not be tAltIndex or tDepthIndex,
              // so ensure tAlt value changed
              int tAltDepthIndex = tAltIndex >= 0 ? tAltIndex : tDepthIndex;
              String val = table.getStringData(tAltDepthIndex, timeRow);
              if (val.equals(lastAltVal))
                throw new SimpleException(
                    EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                        + "This dataset is not suitable for SOS: there are multiple values for one time, but not because of the altitude or depth variable.");
              lastAltVal = val;

              writer.write(
                  "                            <ioos:CompositeValue gml:id=\""
                      + stationNTN
                      + "Bin"
                      + (timeRow - sTimeStartRow + 1)
                      + "Obs\" processDef=\"#"
                      + stationN
                      + "Sensor1Info\">\n"
                      + "                              <gml:valueComponents>\n"
                      + "                                <ioos:Context name=\""
                      + xmlColNames[tAltDepthIndex]
                      + "\""
                      + xmlColUcumUnits[tAltDepthIndex]);
              writer.write(
                  val.length() == 0
                      ? " xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
                      : ">" + XML.encodeAsXML(val) + "</ioos:Context>\n");
            }

            // write the values on this row
            // e.g., <ioos:Quantity name=\"WindDirection\" uom=\"deg\">229.0</ioos:Quantity>
            // e.g., <ioos:Quantity name=\"WindVerticalVelocity\" uom=\"m/s\" xsi:nil=\"true\"
            // nilReason=\"unknown\"/>
            for (int col = 0; col < nCols; col++) {
              // skip the special columns
              if (col == tLonIndex
                  || col == tLatIndex
                  || col == tAltIndex
                  || col == tDepthIndex
                  || col == tTimeIndex
                  || col == tIdIndex) continue;

              writer.write(
                  tIndent
                      + "                            <ioos:Quantity name=\""
                      + xmlColNames[col]
                      + "\""
                      + xmlColUcumUnits[col]);
              String val = table.getStringData(col, timeRow);
              writer.write(
                  val.length() == 0
                      ? " xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
                      : ">" + XML.encodeAsXML(val) + "</ioos:Quantity>\n");
            }

            // end of observations on this row
            writer.write(
                tIndent
                    + "                          </gml:valueComponents>\n"
                    + tIndent
                    + "                        </ioos:CompositeValue>\n");

          } else {
            // not composite sensor: separate sensors
            int sensori = 0;
            for (int col = 0; col < nCols; col++) {
              // skip the special columns
              if (col == tLonIndex
                  || col == tLatIndex
                  || col == tAltIndex
                  || col == tDepthIndex
                  || col == tTimeIndex
                  || col == tIdIndex) continue;

              sensori++;
              if (nTimesThisTime == 1) {
                writer.write(
                    "                        <ioos:CompositeValue gml:id=\""
                        + stationNTN
                        + "Sensor"
                        + sensori
                        + "PointObservation\" "
                        + "processDef=\"#"
                        + stationN
                        + "Sensor"
                        + sensori
                        + "Info\">\n"
                        + "                          <gml:valueComponents>\n");
              } else if (tAltIndex < 0 && tDepthIndex < 0) {
                // withinTimeIndex may not be tAltIndex or tDepthIndex,
                // so ensure there is an tAltIndex or tDepthIndex
                throw new SimpleException(
                    EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                        + "This dataset is not suitable for SOS: there are multiple values for one time, but there is no altitude variable.");
              } else {
                // withinTimeIndex may not be tAltIndex or tDepthIndex,
                // so ensure tAlt value changed
                int tAltDepthIndex = tAltIndex >= 0 ? tAltIndex : tDepthIndex;
                String val = table.getStringData(tAltDepthIndex, timeRow);
                if (val.equals(lastAltVal))
                  throw new SimpleException(
                      EDStatic.simpleBilingual(language, Message.QUERY_ERROR)
                          + "This dataset is not suitable for SOS: there are multiple values for one time, but not because of the altitude or depth variable.");
                lastAltVal = val;

                writer.write(
                    "                            <ioos:CompositeValue gml:id=\""
                        + stationNTN
                        + "Bin"
                        + (timeRow - sTimeStartRow + 1)
                        + "Obs\" processDef=\"#"
                        + stationN
                        + "Sensor1Info\">\n"
                        + "                              <gml:valueComponents>\n"
                        + "                                <ioos:Context name=\""
                        + xmlColNames[tAltDepthIndex]
                        + "\""
                        + xmlColUcumUnits[tAltDepthIndex]);
                writer.write(
                    val.length() == 0
                        ? " xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
                        : ">" + XML.encodeAsXML(val) + "</ioos:Context>\n");
              }

              // write the value on this row for this "sensor" (column)
              // e.g., <ioos:Quantity name=\"WindDirection\" uom=\"deg\">229.0</ioos:Quantity>
              // e.g., <ioos:Quantity name=\"WindVerticalVelocity\" uom=\"m/s\" xsi:nil=\"true\"
              // nilReason=\"unknown\"/>
              writer.write(
                  tIndent
                      + "                            <ioos:Quantity name=\""
                      + xmlColNames[col]
                      + "\""
                      + xmlColUcumUnits[col]);
              String val = table.getStringData(col, timeRow);
              writer.write(
                  val.length() == 0
                      ? " xsi:nil=\"true\" nilReason=\"unknown\"/>\n"
                      : ">" + XML.encodeAsXML(val) + "</ioos:Quantity>\n");

              // end of observations on this row + this column
              writer.write(
                  tIndent
                      + "                          </gml:valueComponents>\n"
                      + tIndent
                      + "                        </ioos:CompositeValue>\n");
            } // end of col loop
          } // end of treating cols as separate sensors
        } // end timeRow loop

        // end of observations for this time
        if (nTimesThisTime > 1)
          writer.write(
              """
                                                    </gml:valueComponents>
                                                  </ioos:ValueArray">
                          """);

        writer.write(
            """
                                              </gml:valueComponents>
                                            </ioos:Composite>
                        """);

        // prepare for next time value
        sTimeStartRow = sRow + 1;
      } // end of sRow loop

      // end of station info
      writer.write(
          """
                                        </gml:valueComponents>
                                      </ioos:Array>
                                    </gml:valueComponents>
                                  </ioos:Composite>
                      """);

      firstStationRow = nextFirstStationRow;
      prevID = currID;
    } // end of station loop

    writer.write(
        """
                              </gml:valueComponents>
                            </ioos:Array>
                          </gml:valueComponents>
                        </ioos:Composite>
                      </om:result>
                    </om:CompositeObservation>
                    """);

    // temporary
    // if (reallyVerbose) String2.log(table.saveAsCsvASCIIString());

    // essential
    writer.flush();
  }

  /**
   * This returns the Oostethys SOS *inline* observations xml for this dataset (see Erddap.doSos).
   * This should only be called if accessibleViaSOS is "" and loggedInAs has access to this dataset
   * (so redirected to login, instead of getting an error here); this doesn't check.
   *
   * <p>This seeks to mimic Oostethys SOS servers like gomoosBuoy datasource [was
   * http://www.gomoos.org/cgi-bin/sos/oostethys_sos.cgi ].
   *
   * @param request the request
   * @param language the index of the selected language
   * @param requestOfferingType e.g., network or e.g., station
   * @param requestShortOfferingName e.g., all or 41004 or ...
   * @param tw with all of the results data. Units should be UCUM already.
   * @param writer In the end, the writer is flushed, not closed.
   * @param loggedInAs the name of the logged in user (or null if not logged in).
   */
  public void sosObservationsXmlInlineOostethys(
      HttpServletRequest request,
      int language,
      String requestOfferingType,
      String requestShortOfferingName,
      TableWriterAllWithMetadata tw,
      Writer writer,
      String loggedInAs)
      throws Throwable {

    // if (accessibleViaSOS().length() > 0)
    //    throw new SimpleException(EDStatic.simpleBilingual(language,
    // EDStatic.messages.queryErrorAr) +
    //        accessibleViaSOS());

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String sensorGmlNameStart = getSosGmlNameStart(language, "sensor");
    String fullPhenomenaDictionaryUrl =
        tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl;

    // It's quick and easy to write out the info if cumulativeTable is available.
    // It's very cumbersome any other way.
    // It may need a lot of memory, but hopefully just for a short time (tested by caller).
    Table table = tw.cumulativeTable();
    table.convertToStandardMissingValues(); // so missing_values are NaNs

    // ensure lon, lat, time, id are present in the table (and alt if eddTable has alt)
    String idName = dataVariableDestinationNames[sosOfferingIndex];
    int tLonIndex = table.findColumnNumber(EDV.LON_NAME);
    int tLatIndex = table.findColumnNumber(EDV.LAT_NAME);
    int tAltIndex = table.findColumnNumber(EDV.ALT_NAME);
    int tDepthIndex = table.findColumnNumber(EDV.DEPTH_NAME);
    int tTimeIndex = table.findColumnNumber(EDV.TIME_NAME);
    int tIdIndex = table.findColumnNumber(idName);
    if (tLonIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'longitude' isn't in the response table.");
    if (tLatIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'latitude' isn't in the response table.");
    if (altIndex >= 0 && tAltIndex < 0) // yes, first one is alt, not tAlt
    throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'altitude' isn't in the response table.");
    if (depthIndex >= 0 && tDepthIndex < 0) // yes, first one is depth, not tDepth
    throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'depth' isn't in the response table.");
    if (tTimeIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0) + "'time' isn't in the response table.");
    if (tIdIndex < 0)
      throw new RuntimeException(
          EDStatic.messages.get(Message.ERROR_INTERNAL, 0)
              + "'"
              + idName
              + "' isn't in the response table.");

    // Ensure it is sorted by tIdIndex, tTimeIndex, [tAltIndex|tDepthIndex] (it usually is, but make
    // sure; it is assumed below)
    int sortIndices[] =
        tAltIndex >= 0
            ? new int[] {tIdIndex, tTimeIndex, tAltIndex}
            : tDepthIndex >= 0
                ? new int[] {tIdIndex, tTimeIndex, tDepthIndex}
                : new int[] {tIdIndex, tTimeIndex};
    boolean sortAscending[] = new boolean[sortIndices.length];
    Arrays.fill(sortAscending, true);
    table.sort(sortIndices, sortAscending);
    PrimitiveArray tIdPA = table.getColumn(tIdIndex);
    PrimitiveArray tTimePA = table.getColumn(tTimeIndex);
    PrimitiveArray tAltPA = tAltIndex >= 0 ? table.getColumn(tAltIndex) : null;
    PrimitiveArray tLatPA = table.getColumn(tLatIndex);
    PrimitiveArray tLonPA = table.getColumn(tLonIndex);
    int nRows = tIdPA.size();
    int nCols = table.nColumns();

    // count n stations
    String prevID = "\uFFFF";
    int nStations = 0;
    for (int row = 0; row < nRows; row++) {
      String currID = tIdPA.getString(row);
      if (!currID.equals(prevID)) {
        nStations++;
        prevID = currID;
      }
    }

    // gather column names and units
    String xmlColNames[] = new String[nCols];
    String xmlColUcumUnits[] = new String[nCols];
    boolean isStringCol[] = new boolean[nCols];
    boolean isTimeStampCol[] = new boolean[nCols];
    for (int col = 0; col < nCols; col++) {
      xmlColNames[col] = XML.encodeAsXML(table.getColumnName(col));
      String u = table.columnAttributes(col).getString("units");
      isTimeStampCol[col] = EDV.TIME_UCUM_UNITS.equals(u);
      if (isTimeStampCol[col]) u = "UTC";
      xmlColUcumUnits[col] = u == null || u.length() == 0 ? "" : XML.encodeAsXML(u);
      isStringCol[col] = table.getColumn(col) instanceof StringArray;
    }
    boolean compositeSensor = nCols == dataVariables.length;
    PAOne minLon = tw.columnMinValue(tLonIndex);
    PAOne maxLon = tw.columnMaxValue(tLonIndex);
    PAOne minLat = tw.columnMinValue(tLatIndex);
    PAOne maxLat = tw.columnMaxValue(tLatIndex);
    PAOne minTime = tw.columnMinValue(tTimeIndex);
    PAOne maxTime = tw.columnMaxValue(tTimeIndex);

    // write start of xml content for Oostethys response
    writer.write( // sosObservationsXmlInlineOostethys
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<om:Observation\n"
            + "  xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\"\n"
            + "  xmlns:swe=\"http://www.opengis.net/swe/0\"\n"
            + "  xmlns:gml=\"http://www.opengis.net/gml\"\n"
            + "  xmlns:om=\"http://www.opengis.net/om\"\n"
            + "  xmlns:xlink=\"https://www.w3.org/1999/xlink\"\n"
            +
            // ???
            "  xsi:schemaLocation=\"http://www.opengis.net/om ../../../../../../ows4/schema0/swe/branches/swe-ows4-demo/om/current/commonObservation.xsd\"\n"
            + "  gml:id=\""
            + datasetID
            + "TimeSeriesObservation\">\n"
            + "<!-- This is ERDDAP's PROTOTYPE SOS service.  The information in this response is NOT complete. -->\n"
            + "  <gml:description>"
            + datasetID
            + " observations at a series of times</gml:description>\n"
            + "  <gml:name>"
            + XML.encodeAsXML(title(language))
            + ", "
            + requestOfferingType
            + " "
            + requestShortOfferingName
            + "</gml:name>\n"
            + "  <gml:location>\n");
    if (minLon.compareTo(maxLon) == 0 && minLat.compareTo(maxLat) == 0)
      writer.write(
          "    <gml:Point gml:id=\"OBSERVATION_LOCATION\" srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
              +
              // "            <!-- use lat lon depth in deg deg m -->\n" +    //Oostethys had depth,
              // too
              "      <gml:coordinates>"
              + minLat.getString()
              + " "
              + minLon.getString()
              + "</gml:coordinates>\n"
              + "    </gml:Point>\n");
    else
      writer.write(
          "    <gml:Envelope srsName=\"urn:ogc:def:crs:epsg::4326\">\n"
              + "      <gml:lowerCorner>"
              + minLat.getString()
              + " "
              + minLon.getString()
              + "</gml:lowerCorner>\n"
              + // lat lon
              "      <gml:upperCorner>"
              + maxLat.getString()
              + " "
              + maxLon.getString()
              + "</gml:upperCorner>\n"
              + "    </gml:Envelope>\n");
    writer.write("""
              </gml:location>
              <om:time>
            """);
    if (minTime.compareTo(maxTime) == 0)
      writer.write(
          // use TimeInstant for a single measurement
          "    <gml:TimeInstant gml:id=\"DATA_TIME\">\n"
              + "      <gml:timePosition"
              + Calendar2.epochSecondsToIsoStringTZ(minTime.getDouble())
              + "</gml:timePosition>\n"
              + "    </gml:TimeInstant>\n");
    else
      writer.write(
          // use TimePeriod for a series of data
          "    <gml:TimePeriod gml:id=\"DATA_TIME\">\n"
              + "      <gml:beginPosition>"
              + Calendar2.epochSecondsToIsoStringTZ(minTime.getDouble())
              + "</gml:beginPosition>\n"
              + "      <gml:endPosition>"
              + Calendar2.epochSecondsToIsoStringTZ(maxTime.getDouble())
              + "</gml:endPosition>\n"
              + "    </gml:TimePeriod>\n");
    writer.write("  </om:time>\n");

    // Procedure
    writer.write(
        "    <om:procedure xlink:href=\""
            + XML.encodeAsXML(
                sensorGmlNameStart
                    + (nStations > 1 ? datasetID : tIdPA.getString(0))
                    + ":"
                    + datasetID)
            + // ???individual sensors?
            "\"/>\n");

    // the property(s) measured
    if (compositeSensor) {
      writer.write(
          "    <om:observedProperty xlink:href=\""
              + fullPhenomenaDictionaryUrl
              + "#"
              + datasetID
              + "\"/>\n");
    } else {
      for (int col = 0; col < nCols; col++) {
        if (col == tLonIndex
            || col == tLatIndex
            || col == tAltIndex
            || col == tDepthIndex
            || col == tTimeIndex
            || col == tIdIndex) continue;
        writer.write(
            "    <om:observedProperty xlink:href=\""
                + fullPhenomenaDictionaryUrl
                + "#"
                + xmlColNames[col]
                + "\"/>\n");
      }
    }

    writer.write(
        // "    <!-- Feature Of Interest -->\n" +
        "    <om:featureOfInterest xlink:href=\""
            + XML.encodeAsXML(sosFeatureOfInterest(language))
            + "\"/>\n"
            +
            // "    <!-- Result Structure and Encoding -->\n" +
            "    <om:resultDefinition>\n"
            + "        <swe:DataBlockDefinition>\n"
            + "            <swe:components name=\""
            + datasetID
            + "DataFor"
            + (nStations == 1 ? tIdPA.getString(0) : sosNetworkOfferingType)
            + // stationID : "network"
            "\">\n"
            + "                <swe:DataRecord>\n"
            + "                    <swe:field name=\"time\">\n"
            + "                        <swe:Time definition=\"urn:ogc:phenomenon:time:iso8601\"/>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"latitude\">\n"
            + "                        <swe:Quantity definition=\"urn:ogc:phenomenon:latitude:wgs84\">\n"
            +
            // ???or like  <swe:uom code=\"m s-1\" /> ?
            "                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n"
            + "                    <swe:field name=\"longitude\">\n"
            + "                        <swe:Quantity definition=\"urn:ogc:phenomenon:longitude:wgs84\">\n"
            + "                            <swe:uom xlink:href=\"urn:ogc:unit:degree\"/>\n"
            + "                        </swe:Quantity>\n"
            + "                    </swe:field>\n");

    if (tAltIndex >= 0) {
      writer.write(
          "                    <swe:field name=\"altitude\">\n"
              + // gomoos had 'depth'
              "                        <swe:Quantity definition=\"urn:ogc:phenomenon:altitude\">\n"
              + // gomoos had 'depth'
              // ???or like  <swe:uom code=\"m\" /> ?
              "                            <swe:uom xlink:href=\"urn:ogc:unit:meter\"/>\n"
              + "                        </swe:Quantity>\n"
              + "                    </swe:field>\n");
    }

    if (tDepthIndex >= 0) {
      writer.write(
          "                    <swe:field name=\"depth\">\n"
              + // gomoos had 'depth'
              "                        <swe:Quantity definition=\"urn:ogc:phenomenon:depth\">\n"
              + // gomoos had 'depth'
              // ???or like  <swe:uom code=\"m\" /> ?
              "                            <swe:uom xlink:href=\"urn:ogc:unit:meter\"/>\n"
              + "                        </swe:Quantity>\n"
              + "                    </swe:field>\n");
    }

    for (int col = 0; col < nCols; col++) {
      if (col == tLonIndex
          || col == tLatIndex
          || col == tAltIndex
          || col == tDepthIndex
          || col == tTimeIndex
          || col == tIdIndex) continue;
      writer.write(
          "                    <swe:field name=\""
              + xmlColNames[col]
              + "\">\n"
              + "                        <swe:Quantity definition=\""
              + fullPhenomenaDictionaryUrl
              + "#"
              + xmlColNames[col]
              + "\">\n");
      if (xmlColUcumUnits[col].length() > 0) {
        // ???units!!!
        // ???or like  <swe:uom code=\"m.s-1\" /> ?
        // gomoos had <swe:uom xlink:href="urn:mm.def:units#psu"/>
        writer.write(
            "                            <swe:uom xlink:href=\"urn:erddap.def:units#"
                + xmlColUcumUnits[col]
                + "\"/>\n");
      }
      writer.write(
          """
                                              </swe:Quantity>
                                          </swe:field>
                      """);
    }
    writer.write(
        """
                                    </swe:DataRecord>
                                </swe:components>
                                <swe:encoding>
                                    <swe:AsciiBlock tokenSeparator="," blockSeparator=" " decimalSeparator="."/>
                                </swe:encoding>
                            </swe:DataBlockDefinition>
                        </om:resultDefinition>
                        <om:result>\
                    """);

    // rows of data
    String mvString = ""; // 2009-09-21 Eric Bridger says Oostethys servers use ""
    for (int row = 0; row < nRows; row++) {
      if (row > 0) writer.write(' '); // blockSeparator

      // TLLA
      double tTime = tTimePA.getDouble(row);
      String tLat = tLatPA.getString(row);
      String tLon = tLonPA.getString(row);
      String tAlt = tAltPA == null ? null : tAltPA.getString(row);
      writer.write(
          Calendar2.safeEpochSecondsToIsoStringTZ(tTime, mvString)
              + ","
              + (tLat.length() == 0 ? mvString : tLat)
              + ","
              + (tLon.length() == 0 ? mvString : tLon)
              + // no end comma
              (tAlt == null
                  ? ""
                  : tAlt.length() == 0 ? "," + mvString : "," + tAlt)); // no end comma

      // data columns
      // 2007-07-04T00:00:00Z,43.7813,-69.8891,1,29.3161296844482
      // 2007-07-04T00:00:00Z,43.7813,-69.8891,20,31.24924659729 ...   no end space
      for (int col = 0; col < nCols; col++) {
        if (col == tLonIndex
            || col == tLatIndex
            || col == tAltIndex
            || col == tDepthIndex
            || col == tTimeIndex
            || col == tIdIndex) continue;
        writer.write(',');
        String tVal = table.getStringData(col, row);
        if (isStringCol[col]) writer.write(String2.toJson(tVal));
        else if (isTimeStampCol[col])
          writer.write(
              tVal.length() == 0
                  ? mvString
                  : Calendar2.epochSecondsToIsoStringTZ(table.getDoubleData(col, row)));
        else writer.write(tVal.length() == 0 ? mvString : tVal);
      }
    }
    writer.write(
        "</om:result>\n"
            + // no preceding \n or space
            "</om:Observation>\n");

    // essential
    writer.flush();
  }

  /**
   * This writes the /sos/[datasetID]/index.html page to the writer. <br>
   * Currently, this just works as a SOS 1.0.0 server. <br>
   * The caller should have already checked loggedInAs and accessibleViaSOS().
   *
   * @param request the request
   * @param language the index of the selected language
   * @param loggedInAs the name of the logged in user (or null if not logged in). This doesn't check
   *     if eddTable is accessible to loggedInAs. The caller should do that.
   * @param writer This doesn't write the header. <br>
   *     This starts with youAreHere. <br>
   *     This doesn't write the footer. <br>
   *     Afterwards, the writer is flushed, not closed.
   * @throws Throwable if trouble (there shouldn't be)
   */
  public void sosDatasetHtml(
      HttpServletRequest request, int language, String loggedInAs, Writer writer) throws Throwable {

    String tErddapUrl = EDStatic.erddapUrl(request, loggedInAs, language);
    String sosUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosServer;
    String dictionaryUrl = tErddapUrl + "/sos/" + datasetID + "/" + sosPhenomenaDictionaryUrl;
    int whichOffering =
        Math.max(0, sosOfferings.indexOf("41004")); // cheap trick for cwwcNDBCMet demo
    String shortOfferingi = sosOfferings.getString(whichOffering);
    String sensori = getSosGmlNameStart(language, "sensor") + shortOfferingi; // + ":" + datasetID;
    String offeringi = getSosGmlNameStart(language, sosOfferingType) + shortOfferingi;
    String networkOffering = getSosGmlNameStart(language, sosNetworkOfferingType) + datasetID;
    EDV lonEdv = dataVariables[lonIndex];
    EDV latEdv = dataVariables[latIndex];

    // suggest time range for offeringi
    double maxTime = sosMaxTime.getDouble(whichOffering);
    GregorianCalendar gc =
        Double.isNaN(maxTime) ? Calendar2.newGCalendarZulu() : Calendar2.epochSecondsToGc(maxTime);
    String maxTimeS = Calendar2.formatAsISODateTimeTZ(gc); // use gc, not maxTime
    double minTime = Calendar2.backNDays(7, maxTime);
    String minTimeS = Calendar2.epochSecondsToIsoStringTZ(minTime);
    double networkMinTime = Calendar2.backNDays(1, maxTime);
    String networkMinTimeS = Calendar2.epochSecondsToIsoStringTZ(networkMinTime);

    // GetCapabilities
    String getCapabilitiesHtml =
        XML.encodeAsHTMLAttribute(
            sosUrl + "?service=SOS&version=" + sosVersion + "&request=GetCapabilities");

    // DescribeSensor
    // https://sdf.ndbc.noaa.gov/sos/server.php?request=DescribeSensor&service=SOS
    // &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.0%22
    // &procedure=urn:ioos:sensor:noaa.nws.ndbc:41012:
    String describeSensorHtml =
        XML.encodeAsHTMLAttribute(
            sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=DescribeSensor"
                + "&outputFormat="
                + SSR.minimalPercentEncode(sosDSOutputFormat)
                + "&procedure="
                + SSR.minimalPercentEncode(sensori));

    // find the first numeric non-LLATI var
    int fdv =
        String2.indexOf(dataVariableDestinationNames, "wtmp"); // cheap trick for cwwcNDBCMet demo
    if (fdv < 0) {
      for (int dv = 0; dv < dataVariables.length; dv++) {
        if (dv == lonIndex
            || dv == latIndex
            || dv == altIndex
            || dv == depthIndex
            || dv == timeIndex
            || dv == sosOfferingIndex
            || dataVariables[dv].destinationDataPAType() == PAType.STRING) continue;
        fdv = dv;
        break;
      }
    }
    String fdvName = fdv < 0 ? null : dataVariableDestinationNames[fdv];

    // find u,v variables (adjacent vars, same units), if possible
    int dvu = -1;
    int dvv = -1;
    for (int dv1 = dataVariables.length - 2;
        dv1 >= 0;
        dv1--) { // working backwards works well with cwwcNDBCMet
      int dv2 = dv1 + 1;
      if (dv1 == lonIndex
          || dv1 == latIndex
          || dv1 == altIndex
          || dv1 == depthIndex
          || dv1 == timeIndex
          || dv2 == lonIndex
          || dv2 == latIndex
          || dv2 == altIndex
          || dv2 == depthIndex
          || dv2 == timeIndex
          || dv1 == sosOfferingIndex
          || dataVariables[dv1].destinationDataPAType() == PAType.STRING
          || dv2 == sosOfferingIndex
          || dataVariables[dv2].destinationDataPAType() == PAType.STRING
          || dataVariables[dv1].ucumUnits() == null
          || dataVariables[dv2].ucumUnits() == null
          || !dataVariables[dv1].ucumUnits().equals(dataVariables[dv2].ucumUnits())) continue;
      dvu = dv1;
      dvv = dv2;
      break;
    }

    // find a(from fdv),b variables (different units), if possible
    int dva = -1;
    int dvb = -1;
    if (fdv >= 0) {
      for (int dv2 = 0; dv2 < dataVariables.length; dv2++) {
        if (fdv == dv2
            || fdv == lonIndex
            || fdv == latIndex
            || fdv == altIndex
            || fdv == depthIndex
            || fdv == timeIndex
            || dv2 == lonIndex
            || dv2 == latIndex
            || dv2 == altIndex
            || dv2 == depthIndex
            || dv2 == timeIndex
            || fdv == sosOfferingIndex
            || dataVariables[fdv].destinationDataPAType() == PAType.STRING
            || dv2 == sosOfferingIndex
            || dataVariables[dv2].destinationDataPAType() == PAType.STRING
            || dataVariables[fdv].ucumUnits() == null
            || dataVariables[dv2].ucumUnits() == null
            || dataVariables[fdv].ucumUnits().equals(dataVariables[dv2].ucumUnits())) continue;
        dva = fdv;
        dvb = dv2;
        break;
      }
    }

    // GetObservatioon
    // "service=SOS&version=1.0.0&request=GetObservation" +
    // "&offering=urn:ioos:station:1.0.0.127.cwwcNDBCMet:41004:" +
    // "&observedProperty=cwwcNDBCMet" +
    // "&responseFormat=text/xml;schema=%22ioos/0.6.1%22" +
    // "&eventTime=2008-08-01T00:00:00Z/2008-08-01T04:00:00Z";
    String getObs =
        sosUrl
            + "?service=SOS&version="
            + sosVersion
            + "&request=GetObservation"
            + "&offering="
            + SSR.minimalPercentEncode(offeringi)
            + "&observedProperty="
            + datasetID
            + "&eventTime="
            + minTimeS
            + "/"
            + maxTimeS
            + "&responseFormat=";
    String getObservationHtml =
        XML.encodeAsHTML(
            getObs
                + SSR.minimalPercentEncode(
                    sosDataResponseFormats.get(sosDefaultDataResponseFormat)));
    String bbox =
        "&featureOfInterest=BBOX:"
            + // show upper left quadrant
            lonEdv.destinationMinString()
            + ","
            + latEdv.destinationMinString()
            + ","
            + lonEdv.destinationMaxString()
            + ","
            + latEdv.destinationMaxString();
    String getImageObsAllStations1Var =
        fdv < 0
            ? null
            : sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=GetObservation"
                + "&offering="
                + datasetID
                + "&observedProperty="
                + fdvName
                + "&eventTime="
                + minTimeS
                + "/"
                + maxTimeS
                + bbox
                + "&responseFormat=";
    String getImageObsAllStationsVector =
        dvu < 0
            ? null
            : sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=GetObservation"
                + "&offering="
                + datasetID
                + "&observedProperty="
                + dataVariableDestinationNames[dvu]
                + ","
                + dataVariableDestinationNames[dvv]
                + "&eventTime="
                + minTimeS
                + bbox
                + "&responseFormat=";
    String getImageObs1Station1Var =
        fdv < 0
            ? null
            : sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=GetObservation"
                + "&offering="
                + SSR.minimalPercentEncode(offeringi)
                + "&observedProperty="
                + fdvName
                + "&eventTime="
                + minTimeS
                + "/"
                + maxTimeS
                + "&responseFormat=";
    String getImageObs1StationVector =
        dvu < 0
            ? null
            : sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=GetObservation"
                + "&offering="
                + SSR.minimalPercentEncode(offeringi)
                + "&observedProperty="
                + dataVariableDestinationNames[dvu]
                + ","
                + dataVariableDestinationNames[dvv]
                + "&eventTime="
                + minTimeS
                + "/"
                + maxTimeS
                + "&responseFormat=";
    String getImageObs1Station2Var =
        dva < 0
            ? null
            : sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=GetObservation"
                + "&offering="
                + SSR.minimalPercentEncode(offeringi)
                + "&observedProperty="
                + dataVariableDestinationNames[dva]
                + ","
                + dataVariableDestinationNames[dvb]
                + "&eventTime="
                + minTimeS
                + "/"
                + maxTimeS
                + "&responseFormat=";
    String parameterHelp =
        "<br>The parameters may be in any order in the URL, separated by '&amp;' .\n"
            + "<br>Parameter values are case sensitive and must be\n"
            + "  <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>:\n"
            + "<br>all characters in query values (the parts after the '=' signs) other than A-Za-z0-9_-!.~'()* must be\n"
            + "<br>encoded as %HH, where HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n"
            + "<br>Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n"
            + "<br>(ask a programmer for help). There are\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "\n";

    // network
    String getNetworkObservationHtml =
        XML.encodeAsHTMLAttribute(
            sosUrl
                + "?service=SOS&version="
                + sosVersion
                + "&request=GetObservation"
                + "&offering="
                + SSR.minimalPercentEncode(networkOffering)
                + "&observedProperty="
                + datasetID
                + "&eventTime="
                + networkMinTimeS
                + "/"
                + maxTimeS
                + bbox
                + "&responseFormat="
                + SSR.minimalPercentEncode(
                    sosDataResponseFormats.get(sosDefaultDataResponseFormat)));

    // observedPropertyOptions
    StringBuilder opoSB =
        new StringBuilder(
            "the datasetID (for this dataset <kbd>"
                + datasetID
                + "</kbd>) (for all phenomena)\n"
                + "    <br>or a comma-separated list of simple phenomena (for this dataset, any subset of\n<br><kbd>");
    int charsWritten = 0;
    for (int dv = 0; dv < dataVariables.length; dv++) {
      if (dv == lonIndex
          || dv == latIndex
          || dv == altIndex
          || dv == depthIndex
          || dv == timeIndex
          || dv == sosOfferingIndex) continue;
      if (charsWritten > 0) opoSB.append(",");
      String ts = dataVariableDestinationNames()[dv];
      if (charsWritten + ts.length() > 80) {
        opoSB.append("\n    <br>");
        charsWritten = 0;
      }
      opoSB.append(ts);
      charsWritten += ts.length();
    }
    opoSB.append("</kbd>)");
    String observedPropertyOptions = opoSB.toString();

    // *** html head
    // writer.write(EDStatic.startHeadHtml(language, tErddapUrl, title() + " - SOS"));
    // writer.write("\n" + rssHeadLink());
    // writer.write("</head>\n");
    // writer.write(EDStatic.startBodyHtml(language, loggedInAs,
    //    "/sos/" + datasetID + "/index.html", //was endOfRequest,
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
            "sos",
            datasetID)); // sos must be lowercase for link to work
    writeHtmlDatasetInfo(request, language, loggedInAs, writer, true, true, true, true, "", "");

    String datasetListRef =
        "<p>See the\n"
            + "  <a href=\""
            + tErddapUrl
            + "/sos/index.html\">list \n"
            + "    of datasets available via SOS</a> at this ERDDAP installation.\n";

    // *** What is SOS?   (for tDatasetID)
    // !!!see the almost identical documentation above
    writer.write(
        "<h2><a class=\"selfLink\" id=\"description\" href=\"#description\" rel=\"bookmark\">What</a> is SOS?</h2>\n"
            + String2.replaceAll(
                EDStatic.messages.get(Message.SOS_LONG_DESCRIPTION_HTML, language),
                "&erddapUrl;",
                tErddapUrl)
            + "\n"
            + datasetListRef
            + "\n"
            + "\n");

    // *** client software
    writer.write(
        "<h2><a class=\"selfLink\" id=\"clientSoftware\" href=\"#clientSoftware\" rel=\"bookmark\">Client Software</a></h2>"
            + "It is our impression that SOS is not intended to be used directly by humans using a browser.\n"
            + "<br>Instead, SOS seems to be intended for computer-to-computer transfer of data using SOS client\n"
            + "<br>software or SOS software libraries.\n"
            + "<ul>\n"
            + "<li>If you have SOS client software, give it this dataset's SOS server url\n"
            + "  <br><a href=\""
            + sosUrl
            + "\">"
            + sosUrl
            + "</a>\n"
            + "  <br>and the client software will try to create the URLs to request data and process the results\n"
            + "  <br>for you.\n"
            + "<li>Unfortunately, we are not yet aware of any SOS client software or libraries that can get data\n"
            + "  <br>from IOOS SOS servers (like ERDDAP's SOS server), other than ERDDAP(!) (which makes the data\n"
            + "  <br>available via OPeNDAP). \n"
            +
            // "<li>There may be clients for\n" +
            // "  <a href=\"http://www.oostethys.org/\">OOSTethys" +   [GONE]
            //        EDStatic.externalLinkHtml(language, tErddapUrl) + "</a>-style SOS servers.\n"
            // +
            "<li>Different SOS servers work slightly differently so it would be hard to write client software\n"
            + "  <br>that works with different types of SOS servers without customizing it for each server type.\n"
            + "<li>So the current recommendation seems to be that one should write a custom computer program\n"
            + "  <br>or script to format the requests and to parse and process the XML output from a given server.\n"
            + "  <br>Some of the information you need to write such software is below. Good luck.\n"
            + "<li>For additional information, please see the\n"
            + "  <a rel=\"help\" href=\"https://www.opengeospatial.org/standards/sos\">OGC's SOS Specification"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "</ul>\n"
            + "\n");

    // *** sample SOS requests
    writer.write(
        "<h2>Sample SOS Requests for this Dataset and Details from the \n"
            + "<a rel=\"help\" href=\"https://www.opengeospatial.org/standards/sos\">SOS Specification"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a></h2>\n"
            + "SOS requests are specially formed URLs with queries.\n"
            + "<br>You can create these URLs yourself or write software to do it for you.\n"
            + "<br>There are three types of SOS requests:\n"
            + "  <a rel=\"help\" href=\"#GetCapabilities\">GetCapabilities</a>,\n"
            + "  <a rel=\"help\" href=\"#DescribeSensor\">DescribeSensor</a>,\n"
            + "  <a rel=\"help\" href=\"#GetObservation\">GetObservation</a>.\n"
            + "\n");

    // *** GetCapabilities
    writer.write(
        "<p><a class=\"selfLink\" id=\"GetCapabilities\" href=\"#GetCapabilities\" rel=\"bookmark\"><strong>GetCapabilities</strong></a>\n"
            + " - A GetCapabilities request returns an XML document which provides\n"
            + "  <br>background information about the service and specific information about all of the data\n"
            + "  <br>available from this service.  For this dataset, use\n"
            + "  <br><a href=\""
            + getCapabilitiesHtml
            + "\">\n"
            + getCapabilitiesHtml
            + "</a>\n"
            + "  <br>The GetCapabilities document refers to the "
            + "<a href=\""
            + XML.encodeAsHTMLAttribute(dictionaryUrl)
            + "\">"
            + XML.encodeAsHTML(sosPhenomenaDictionaryUrl)
            + "</a> for this dataset.\n"
            + "\n");

    // getCapabilities specification
    writer.write(
        "<p>The parameters that ERDDAP supports for a GetCapabilities request are:\n"
            + "<table class=\"erd commonBGColor nowrap\" style=\"border-spacing:4px;\">\n"
            + "  <tr>\n"
            + "    <th><i>name=value</i><sup>*</sup></th>\n"
            + "    <th>Description</th>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>service=SOS</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>request=GetCapabilities</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>AcceptVersions="
            + sosVersion
            + "</td>\n"
            + "    <td> This parameter is optional. The only valid value is "
            + sosVersion
            + " .\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>Sections=<i>a comma-separated list</i>"
            + sosVersion
            + "</td>\n"
            + "    <td> This parameter is optional. The default is \"All\".\n"
            + "      <br>This is a comma-separated list of one or more values. The valid values are:\n"
            + "      <br>ServiceIdentification, ServiceProvider, OperationsMetadata, Contents, All.\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "</table>\n"
            + "<sup>*</sup> Parameter names are case-insensitive.\n"
            + parameterHelp);

    // *** DescribeSensor
    writer.write(
        "<p><a class=\"selfLink\" id=\"DescribeSensor\" href=\"#DescribeSensor\" rel=\"bookmark\"><strong>DescribeSensor</strong></a>\n"
            + "  - A DescribeSensor request returns an XML document which provides\n"
            + "  <br>more detailed information about a specific sensor. For example,\n"
            + "  <br><a href=\""
            + describeSensorHtml
            + "\">\n"
            + describeSensorHtml
            + "</a>\n"
            + "\n");

    // start of DescribeSensor Specification
    // &version=1.0.0&outputformat=text/xml;subtype=%22sensorML/1.0.1%22
    // &sensorID=urn:ioos:sensor:noaa.nws.ndbc:41012:
    writer.write(
        "<p>The parameters that ERDDAP supports for a DescribeSensor request are:\n"
            + "<table class=\"erd commonBGColor nowrap\" style=\"border-spacing:4px;\">\n"
            + "  <tr>\n"
            + "    <th><i>name=value</i><sup>*</sup></th>\n"
            + "    <th>Description</th>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>service=SOS</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>version="
            + sosVersion
            + "</td>\n"
            + "    <td>The only valid value is "
            + sosVersion
            + " .  Required.\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>request=DescribeSensor</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>procedure=<i>sensorID</i></td>\n"
            + "    <td>See the sensorID's listed in the GetCapabilities response.\n"
            + "      <br>ERDDAP allows the long or the short form (the last part)\n"
            + "      <br>of the sensorID.  Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>outputFormat=\n"
            + "      <br>&nbsp;&nbsp;"
            + SSR.minimalPercentEncode(sosDSOutputFormat)
            + "</td>\n"
            + "    <td>The only valid value (in\n"
            + "      <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>\n"
            + "      form) is"
            + "       <br>\""
            + SSR.minimalPercentEncode(sosDSOutputFormat)
            + "\".\n"
            + "      <br>Required.\n"
            + "  </tr>\n"
            + "</table>\n"
            + parameterHelp);

    // *** GetObservation
    writer.write(
        "<p><a class=\"selfLink\" id=\"GetObservation\" href=\"#GetObservation\" rel=\"bookmark\"><strong>GetObservation</strong></a>\n"
            + "  - A GetObservation request specifies the subset of data that you want:\n"
            + "<ul>\n"
            + "  <li>The offering: the datasetID (for this dataset <kbd>"
            + datasetID
            + "</kbd>) (for all stations) or one specific station\n"
            + "    <br>(e.g., <kbd>"
            + XML.encodeAsHTML(SSR.minimalPercentEncode(offeringi))
            + "</kbd>)\n"
            + "  <li>The observedProperty:\n"
            + "    <br>"
            + observedPropertyOptions
            + "\n"
            + "  <li>The time range\n"
            + "  <li>(Optional) the bounding box (BBOX:<i>minLon,minLat,maxLon,maxLat</i>)\n"
            + "  <li>The response type (e.g., text/xml;schema=\"ioos/0.6.1\" or text/csv)\n"
            + "</ul>\n"
            + "The SOS service responds with a file with the requested data. An example which returns a SOS XML file is"
            + "<br><a href=\""
            + getObservationHtml
            + "\">\n"
            + getObservationHtml
            + "</a>\n"
            + "<br>(This example was computer-generated, so it may not work well.)\n"
            + "\n"
            +

            // responseFormat
            "<p><a class=\"selfLink\" id=\"responseFormat\" href=\"#responseFormat\" rel=\"bookmark\">responseFormat</a>"
            + "  - By changing the responseFormat, you can get the results in a different file formats.\n"
            + "  <br>The data file formats are:");
    for (int f = 0; f < sosDataResponseFormats.size(); f++)
      writer.write(
          (f == 0 ? "\n" : ",\n")
              + (f % 4 == 2 ? "<br>" : "")
              + "<a href=\""
              + XML.encodeAsHTMLAttribute(
                  getObs + SSR.minimalPercentEncode(sosDataResponseFormats.get(f)))
              + "\">"
              + XML.encodeAsHTML(sosDataResponseFormats.get(f))
              + "</a>");
    writer.write(".\n");
    writer.write("""
            <br>The image file formats are:
            <ul>
            """);

    // image: kml
    writer.write(
        "<li><a class=\"selfLink\" id=\"kml\" href=\"#kml\" rel=\"bookmark\"><strong>Google Earth .kml</strong></a>"
            + "  - If <kbd>responseFormat="
            + XML.encodeAsHTML(sosImageResponseFormats.getFirst())
            + "</kbd>,\n"
            + "<br>the response will be a .kml file with all of the data (regardless of <kbd>observedProperty</kbd>)\n"
            + "<br>for the latest time available (within the time range you specify).\n");
    if (fdv < 0) {
      writer.write("  <br>Sorry, there are no examples for this dataset.\n");
    } else {
      writer.write(
          "  <br>For example:\n"
              + "  <a href=\""
              + XML.encodeAsHTMLAttribute(
                  getImageObsAllStations1Var
                      + SSR.minimalPercentEncode(sosImageResponseFormats.getFirst()))
              + "\">"
              + XML.encodeAsHTML(sosImageResponseFormats.getFirst())
              + "</a>");
      writer.write(
          """
                      .
                        <br>(For many datasets, requesting data for all stations will time out,
                        <br>or fail for some other reason.)
                      """);
    }

    // image: all stations, 1 var
    writer.write(
        "<li><a class=\"selfLink\" id=\"map\" href=\"#map\" rel=\"bookmark\"><strong>Map</strong></a>"
            + "  - If the request's <kbd>offering</kbd> is "
            + datasetID
            + " (all of the stations)\n"
            + "  <br>and the <kbd>observedProperty</kbd> is one simple phenomenon,\n"
            + "  <br>you will get a map showing the latest data (within the time range you request).\n");
    if (fdv < 0) {
      writer.write("  <br>Sorry, there are no examples for this dataset.\n");
    } else {
      writer.write("  <br>For example:");
      for (int f = 1; f < sosImageResponseFormats.size(); f++) // skip 0=kml
      writer.write(
            (f == 1 ? "\n" : ",\n")
                + "<a href=\""
                + XML.encodeAsHTMLAttribute(
                    getImageObsAllStations1Var
                        + SSR.minimalPercentEncode(sosImageResponseFormats.get(f)))
                + "\">"
                + XML.encodeAsHTML(sosImageResponseFormats.get(f))
                + "</a>");
      writer.write(
          """
                      .
                        <br>(For many datasets, requesting data for all stations will time out,
                        <br>or fail for some other reason.)
                      """);
    }

    // image: all stations, 2 var: vectors
    writer.write(
        "<li><a class=\"selfLink\" id=\"vectors\" href=\"#vectors\" rel=\"bookmark\"><strong>Map with Vectors</strong></a>"
            + "  - If the request's <kbd>offering</kbd> is "
            + datasetID
            + " (all of the stations),\n"
            + "  <br>the <kbd>observedProperty</kbd> lists two simple phenomenon with the same units\n"
            + "  <br>(hopefully representing the x and y components of a vector), \n"
            + "  <br>and the <kbd>eventTime</kbd> is a single time, you will get a map with vectors.\n"
            + "  <br>(If the <kbd>eventTime</kbd> is a time range, you will get multiple vectors for each station,\n"
            + "  <br>which is a mess).\n");
    if (dvu < 0) writer.write("  <br>Sorry, there are no examples for this dataset.\n");
    else {
      writer.write("  <br>For example:");
      for (int f = 1; f < sosImageResponseFormats.size(); f++) // skip 0=kml
      writer.write(
            (f == 1 ? "\n" : ",\n")
                + "<a href=\""
                + XML.encodeAsHTMLAttribute(
                    getImageObsAllStationsVector
                        + SSR.minimalPercentEncode(sosImageResponseFormats.get(f)))
                + "\">"
                + XML.encodeAsHTML(sosImageResponseFormats.get(f))
                + "</a>");
      writer.write(
          """
                      .
                        <br>(For many datasets, requesting data for all stations will timeout,
                        <br>or fail for some other reason.)
                      """);
    }

    // image: 1 station, 1 var
    writer.write(
        """
                    <li><a class="selfLink" id="timeSeries" href="#timeSeries" rel="bookmark"><strong>Time Series</strong></a>
                      - If the request's <kbd>offering</kbd> is a specific station
                      <br>and the <kbd>observedProperty</kbd> is one simple phenomenon,
                      <br>you will get a time series graph (with data from the time range you request).
                    """);
    if (fdv < 0) {
      writer.write("  <br>Sorry, there are no examples for this dataset.\n");
    } else {
      writer.write("  <br>For example:");
      for (int f = 1; f < sosImageResponseFormats.size(); f++) // skip 0=kml
      writer.write(
            (f == 1 ? "\n" : ",\n")
                + "<a href=\""
                + XML.encodeAsHTMLAttribute(
                    getImageObs1Station1Var
                        + SSR.minimalPercentEncode(sosImageResponseFormats.get(f)))
                + "\">"
                + XML.encodeAsHTML(sosImageResponseFormats.get(f))
                + "</a>");
      writer.write(".\n");
    }

    // image: 1 station, scatterplot
    writer.write(
        """
                    <li><a class="selfLink" id="scatterplot" href="#scatterplot" rel="bookmark"><strong>Scatter Plot / Phase Plot</strong></a>
                      - If the request's <kbd>offering</kbd> is a specific station
                      <br>and the <kbd>observedProperty</kbd> lists two simple phenomenon with the different units,
                      <br>you will get a scatter plot (with data from the time range you request).
                    """);
    if (dvu < 0) writer.write("  <br>Sorry, there are no examples for this dataset.\n");
    else {
      writer.write("  <br>For example:");
      for (int f = 1; f < sosImageResponseFormats.size(); f++) // skip 0=kml
      writer.write(
            (f == 1 ? "\n" : ",\n")
                + "<a href=\""
                + XML.encodeAsHTMLAttribute(
                    getImageObs1Station2Var
                        + SSR.minimalPercentEncode(sosImageResponseFormats.get(f)))
                + "\">"
                + XML.encodeAsHTML(sosImageResponseFormats.get(f))
                + "</a>");
      writer.write(".\n");
    }

    // image: 1 station, 2 var - sticks
    writer.write(
        """
                    <li><a class="selfLink" id="sticks" href="#sticks" rel="bookmark"><strong>Sticks</strong></a>\
                      - If the request's <kbd>offering</kbd> is a specific station
                      <br>and the <kbd>observedProperty</kbd> lists two simple phenomenon with the same units
                      <br>(representing the x and y components of a vector),
                      <br>you will get a sticks graph (with data from the time range you request).
                      <br>Each stick is a vector associated with one time point.
                    """);
    if (dvu < 0) writer.write("  <br>Sorry, there are no examples for this dataset.\n");
    else {
      writer.write("  <br>For example:");
      for (int f = 1; f < sosImageResponseFormats.size(); f++) // skip 0=kml
      writer.write(
            (f == 1 ? "\n" : ",\n")
                + "<a href=\""
                + XML.encodeAsHTMLAttribute(
                    getImageObs1StationVector
                        + SSR.minimalPercentEncode(sosImageResponseFormats.get(f)))
                + "\">"
                + XML.encodeAsHTML(sosImageResponseFormats.get(f))
                + "</a>");
      writer.write(".\n");
    }

    writer.write(
        """
                    </ul>
                    (These examples were computer-generated, so they may not work well.)

                    """);

    /*
    //responseMode=out-of-band
    writer.write(
        "<p><a class=\"selfLink\" id=\"outOfBand\" href=\"#outOfBand\" rel=\"bookmark\">responseMode=out-of-band</a>" +
        "  - By changing the responseFormat and setting responseMode=out-of-band,\n" +
        "  <br>you can get a SOS XML file with a <strong>link</strong> to the results in a different file format.\n" +
        "  <br>[For now, there is not much point to using out-of-band since you might as will get the response directly (inline).]\n" +
        "  <br>The data file formats are:");
    for (int f = 2; f < sosDataResponseFormats.length; f++)   //first 2 are standard SOS xml formats
        writer.write((f <= 2? "\n" : ",\n") +
            "<a href=\"" +
            XML.encodeAsHTMLAttribute(getObs + SSR.minimalPercentEncode(sosDataResponseFormats[f]) +
                "&responseMode=out-of-band") +
            "\">" +
            XML.encodeAsHTML(sosDataResponseFormats[f]) + "</a>");
    writer.write(".\n");
    if (fdv >= 0) {
        writer.write(
        "<br>The image file formats are:");
        for (int f = 0; f < sosImageResponseFormats.length; f++)
            writer.write((f == 0? "\n" : ",\n") +
                "<a href=\"" +
                XML.encodeAsHTMLAttribute(getImageObs1Station1Var +
                    SSR.minimalPercentEncode(sosImageResponseFormats[f]) +
                    "&responseMode=out-of-band") +
                "\">" +
                XML.encodeAsHTML(sosImageResponseFormats[f]) + "</a>");
        writer.write(".\n");
    }
    writer.write(
        "<br>(These examples were computer-generated, so they may not work well.)\n");
    */

    // featureOfInterest=BBOX
    writer.write(
        "<p><a class=\"selfLink\" id=\"bbox\" href=\"#bbox\" rel=\"bookmark\">featureOfInterest=BBOX</a>\n"
            + "  - To get data from all stations within a rectangular bounding box,\n"
            + "  <br>you can request data for the network offering (all stations) and include a\n"
            + "  <br>minLon,minLat,maxLon,maxLat bounding box constraint in the request.\n"
            + "  <br>An example which returns a SOS XML file is\n"
            + "  <br><a href=\""
            + getNetworkObservationHtml
            + "\">\n"
            + getNetworkObservationHtml
            + "</a>\n"
            + "  <br>(This example was computer-generated, so it may not work well.)\n"
            + "  <br>For remote datasets, this example may respond slowly or fail because it takes too long.\n"
            + "\n");

    // start of GetObservation Specification
    writer.write(
        "<p>The parameters that ERDDAP supports for a GetObservation request are:\n"
            + "<table class=\"erd commonBGColor nowrap\" style=\"border-spacing:4px;\">\n"
            + "  <tr>\n"
            + "    <th><i>name=value</i><sup>*</sup></th>\n"
            + "    <th>Description</th>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>service=SOS</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>version="
            + sosVersion
            + "</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>request=GetObservation</td>\n"
            + "    <td>Required.</td>\n"
            + "  </tr>\n"
            +
            // "service=SOS&version=1.0.0&request=GetObservation" +
            // "&offering=urn:ioos:station:1.0.0.127.cwwcNDBCMet:41004:" +
            // "&observedProperty=cwwcNDBCMet" +
            // "&responseFormat=text/xml;schema=%22ioos/0.6.1%22" +
            // "&eventTime=2008-08-01T00:00:00Z/2008-08-01T04:00:00Z";
            "  <tr>\n"
            + "    <td>srsName=<i>srsName</i></td>\n"
            + "    <td>This is required by the SOS standard, but optional in ERDDAP.\n"
            + "      <br>In ERDDAP, if it is present, it must be \"urn:ogc:def:crs:epsg::4326\",\n"
            + "      <br>the only supported SRS.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>offering=<i>offering</i></td>\n"
            + "    <td>The name of an <kbd>observationOffering</kbd> from GetCapabilities.\n"
            + "      <br>ERDDAP allows either the long or the short form (the datasetID\n"
            + "      <br>or the station name at the end) of the offering name.\n"
            + "      <br>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>eventTime=<i>beginTime</i>\n"
            + "    <br>eventTime=<i>beginTime/endTime</i></td>\n"
            + "    <td>The beginTime and endTime must be in\n"
            + "      <a rel=\"help\" href=\"https://en.wikipedia.org/wiki/ISO_8601\">ISO 8601:2004 \"extended\" format\n"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>,\n"
            + "      <br>for example, \"1985-01-02T00:00:00Z\".\n"
            + "      <br>This parameter is optional for the SOS standard and ERDDAP.\n"
            + "      <br>For IOOS SOS, the default is the last value.\n"
            + "      <br>For ERDDAP, the default is the last value (but perhaps nothing if\n"
            + "      <br>there is no data for the last week).\n"
            + "      <br>If offering="
            + datasetID
            + ", the default returns data from all of\n"
            + "      <br>the stations which have data for the most recent time.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>procedure=<i>procedure</i></td>\n"
            + "    <td>The SOS standard allows 0 or more comma-separated procedures.\n"
            + "      <br>ERDDAP doesn't currently support this.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>observedProperty=<i>observedProperty</i></td>\n"
            + "    <td>The SOS standard requires 1 or more comma-separated observedProperties.\n"
            + "      <br>ERDDAP allows the long form (e.g.,\n"
            + "      <br>"
            + tErddapUrl
            + "/sos/"
            + datasetID
            + "/"
            + sosPhenomenaDictionaryUrl
            + "#"
            + datasetID
            + ")\n"
            + "      <br>or the short form (e.g., "
            + datasetID
            + ") of the observedProperty.\n"
            + "      <br>ERDDAP allows 0 or more comma-separated <kbd>observedProperty</kbd>'s\n"
            + "      <br>from GetCapabilities (default="
            + datasetID
            + "). In ERDDAP, the options are:\n"
            + "      <br>"
            + observedPropertyOptions
            + ".\n"
            + "    </td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>featureOfInterest=\n"
            + "      <br>&nbsp;&nbsp;BBOX:<i>minLon,minLat,maxLon,maxLat</i></td>\n"
            + "    <td>BBOX allows you to specify a longitude,latitude bounding box constraint.\n"
            + "      <br>This parameter is optional for the SOS standard.\n"
            + "      <br>This parameter is optional in ERDDAP and only really useful for the\n"
            + "      <br>\"network\" offering.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>result=<i>result</i></td>\n"
            + "    <td>This is optional in the SOS standard and currently not supported in ERDDAP.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>responseFormat=<i>mimeType</i></td>\n"
            + "    <td>In ERDDAP, this can be any one of several response formats.\n"
            + "      <br>The value must be <a class=\"N\" rel=\"help\" href=\"https://en.wikipedia.org/wiki/Percent-encoding\">percent encoded"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>:\n"
            + "      <br>all characters in query values (the parts after the '=' signs) other than A-Za-z0-9_-!.~'()* must be\n"
            + "      <br>encoded as %HH, where HH is the 2 digit hexadecimal value of the character, for example, space becomes %20.\n"
            + "      <br>Characters above #127 must be converted to UTF-8 bytes, then each UTF-8 byte must be percent encoded\n"
            + "      <br>(ask a programmer for help). There are\n"
            + "<a class=\"N\" rel=\"help\" href=\"https://www.url-encode-decode.com\">websites that percent encode/decode for you"
            + EDStatic.messages.externalLinkHtml(language, tErddapUrl)
            + "</a>.\n"
            + "      <br>The data responseFormats are:<br>");
    charsWritten = 0;
    for (int f = 0; f < sosDataResponseFormats.size(); f++) {
      if (f > 0) writer.write(", ");
      String ts = "\"" + SSR.minimalPercentEncode(sosDataResponseFormats.get(f)) + "\"";
      if (charsWritten + ts.length() > 60) {
        writer.write("\n      <br>");
        charsWritten = 0;
      }
      writer.write(ts);
      charsWritten += ts.length();
    }
    writer.write(
        """
                          <br>(The first two data responseFormat options are synonyms for the
                          <br>IOOS SOS XML format.)
                          <br>The image responseFormats are:
                          <br>\
                    """);
    for (int f = 0; f < sosImageResponseFormats.size(); f++) {
      if (f > 0) writer.write(", ");
      writer.write("\"" + SSR.minimalPercentEncode(sosImageResponseFormats.get(f)) + "\"");
    }
    writer.write(
        ".\n"
            + "      <br>If you specify an image responseFormat, you must specify simple\n"
            + "      <br>observedProperties (usually 1 or 2). See the <a href=\"#responseFormat\">examples</a> above.\n"
            + "      <br>Required.</td>\n"
            + "  </tr>\n"
            + "  <tr>\n"
            + "    <td>resultModel=<i>resultModel</i></td>\n"
            + "    <td>This is optional in the SOS standard and ERDDAP.\n"
            + "      <br>If present, it must be \"om:Observation\" (the default).</td>\n"
            + "  </tr>\n"
            +
            // "  <tr>\n" +
            // "    <td>responseMode=<i>responseMode</i></td>\n" +
            // "    <td>This is optional in the SOS standard and ERDDAP (default=\"inline\").\n" +
            // "      <br>Use \"inline\" to get the responseFormat directly.\n" +
            // "      <br>Use \"out-of-band\" to get a short SOS XML response with a link to the
            // responseFormat response.</td>\n" +
            // "  </tr>\n" +
            "</table>\n"
            + parameterHelp);

    writer.write("</div>\n");
    // writer.write(EDStatic.endBodyHtml(language, tErddapUrl, loggedInAs));
    // writer.write("\n</html>\n");
    writer.flush();
  }

  /** This writes the Google Visualization Query Language documentation .html page. */
  /*
     Google defined the services that a Google Data Source (GDS) needs to provide.
     It is a sophisticated, SQL-like query system.
     Unfortunately, the OPeNDAP query system that ERDDAP supports is not as
     sophisticated as the GDS system.
     Similarly, none of the remote data sources (with the exception of databases)
     have a query system as sophisticated as GDS.
     So ERDDAP can't process a GDS request by simply converting it into an OPeNDAP request,
     or by passing the GDS request to the remote server.

     Google recognized that it would be hard for people to create servers
     that could handle the GDS requests.
     So Google kindly released Java code to simplify creating a GDS server.
     It works by having the server program (e.g., ERDDAP) feed in the entire
     dataset in order to process each request.
     It then does all of the hard work to filter and process the request.
     This works great for the vast majority of datasets on the web,
     where the dataset is small (a few hundred rows) and can be accessed quickly (i.e., is stored locally).
     Unfortunately, most datasets in ERDDAP are large (10's or 100's of thousands of rows)
     and are accessed slowly (because they are remote).
     Downloading the entire dataset just to process one request just isn't feasible.

     For now, ERDDAP's compromise solution is to just accept a subset of
     the Google Query Language commands.
     This way, ERDDAP can pass most of the constraints on to the remote data source
     (as it always has) and let the remote data source do most of the subset selection.
     Then ERDDAP can do the final subset selection and processing and
     return the response data table to the client.
     This is not ideal, but there has to be some compromise to allow ERDDAP's
     large, remote datasets (accessible via the OPeNDAP query system)
     to be accessible as Google Data Sources.

  */

  /**
   * This writes the dataset's FGDC-STD-001-1998 "Content Standard for Digital Geospatial Metadata"
   * https://www.fgdc.gov/standards/projects/FGDC-standards-projects/metadata/base-metadata/index_html
   * (unlike EDDGrid, not "Extensions for Remote Sensing Metadata") XML to the writer. <br>
   * I started with EDDGrid version, but modified using FGDC example from OBIS.
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
   * <p>FGDC Metadata validator https://www.maine.edu/geolib/fgdc_metadata_validator.html
   *
   * <p>If getAccessibleTo()==null, the fgdc refers to http: ERDDAP links; otherwise, it refers to
   * https: ERDDAP links.
   *
   * @param language the index of the selected language
   * @param writer a UTF-8 writer
   * @throws Throwable if trouble (e.g., no latitude and longitude variables)
   */
  @Override
  protected void writeFGDC(int language, Writer writer) throws Throwable {
    // FUTURE: support datasets with x,y (and not longitude,latitude)

    // requirements
    if (lonIndex < 0 || latIndex < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + EDStatic.messages.get(Message.NO_XXX_NO_LL, 0),
              EDStatic.messages.get(Message.QUERY_ERROR, language)
                  + EDStatic.messages.get(Message.NO_XXX_NO_LL, language)));

    String tErddapUrl = EDStatic.preferredErddapUrl;
    String datasetUrl = tErddapUrl + "/" + dapProtocol + "/" + datasetID();
    String sosUrl = tErddapUrl + "/sos/" + datasetID() + "/" + sosServer; // "?" at end?
    String wmsUrl = tErddapUrl + "/wms/" + datasetID() + "/" + WMS_SERVER; // "?" at end?
    String domain = EDStatic.config.baseUrl;
    if (domain.startsWith("http://")) domain = domain.substring(7);
    else if (domain.startsWith("https://")) domain = domain.substring(8);
    String eddCreationDate =
        String2.replaceAll(Calendar2.millisToIsoDateString(creationTimeMillis()), "-", "");
    String unknown = "Unknown"; // pg viii of FGDC document

    String acknowledgement =
        combinedGlobalAttributes.getString(language, "acknowledgement"); // acdd 1.3
    if (acknowledgement == null)
      acknowledgement = combinedGlobalAttributes.getString(language, "acknowledgment"); // acdd 1.0
    String cdmDataType = combinedGlobalAttributes.getString(language, "cdm_data_type");
    String contributorName = combinedGlobalAttributes.getString(language, "contributor_name");
    String contributorEmail = combinedGlobalAttributes.getString(language, "contributor_email");
    String contributorRole = combinedGlobalAttributes.getString(language, "contributor_role");
    String creatorName = combinedGlobalAttributes.getString(language, "creator_name");
    String creatorEmail = combinedGlobalAttributes.getString(language, "creator_email");
    // creatorUrl: use infoUrl
    String dateCreated =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString(language, "date_created")); // "" if trouble
    String dateIssued =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString(language, "date_issued")); // "" if trouble
    if (dateCreated.length() > 10) dateCreated = dateCreated.substring(0, 10);
    if (dateIssued.length() > 10) dateIssued = dateIssued.substring(0, 10);
    if (dateCreated.startsWith("0000")) // year=0000 isn't valid
    dateCreated = "";
    if (dateIssued.startsWith("0000")) dateIssued = "";
    // make compact form  YYYYMMDD
    dateCreated = String2.replaceAll(dateCreated, "-", "");
    dateIssued = String2.replaceAll(dateIssued, "-", "");
    String history = combinedGlobalAttributes.getString(language, "history");
    String infoUrl = combinedGlobalAttributes.getString(language, "infoUrl");
    String institution = combinedGlobalAttributes.getString(language, "institution");
    String keywords = combinedGlobalAttributes.getString(language, "keywords");
    String keywordsVocabulary = combinedGlobalAttributes.getString(language, "keywords_vocabulary");
    if (keywords == null) { // use the crude, ERDDAP keywords
      keywords = EDStatic.config.keywords;
      keywordsVocabulary = null;
    }
    String license = combinedGlobalAttributes.getString(language, "license");
    String project = combinedGlobalAttributes.getString(language, "project");
    if (project == null) project = institution;
    String references = combinedGlobalAttributes.getString(language, "references");
    String satellite = combinedGlobalAttributes.getString(language, "satellite");
    String sensor = combinedGlobalAttributes.getString(language, "sensor");
    String sourceUrl = publicSourceUrl(language);
    String standardNameVocabulary =
        combinedGlobalAttributes.getString(language, "standard_name_vocabulary");

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
    if (cdmDataType == null || testMinimalMetadata) cdmDataType = CDM_OTHER;
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
    EDVLat latEdv = (EDVLat) dataVariables[latIndex];
    EDVLon lonEdv = (EDVLon) dataVariables[lonIndex];
    EDVTime timeEdv =
        (timeIndex < 0 || testMinimalMetadata) ? null : (EDVTime) dataVariables[timeIndex];
    String minTime = ""; // compact ISO date (not time), may be ""
    String maxTime = "";
    if (timeEdv != null) {
      minTime = timeEdv.destinationMinString(); // differs from EDDGrid in that it may be ""
      maxTime = timeEdv.destinationMaxString(); // "
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
      for (EDV dataVariable : dataVariables) {
        String sn = dataVariable.combinedAttributes().getString(language, "standard_name");
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
            + // or ISO-8859-1 charset???
            "<metadata xmlns:xsi=\"https://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"http://fgdcxml.sourceforge.net/schema/fgdc-std-012-2002/fgdc-std-012-2002.xsd\" "
            +
            // http://lab.usgin.org/groups/etl-debug-blog/fgdc-xml-schema-woes
            // talks about using this
            ">\n"
            + "  <idinfo>\n"
            +
            // EDDGrid has ngdc's <datasetid> here
            "    <citation>\n"
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
            +
            // EDDGrid has ngdc's <origin_cntinfo> here
            "        <pubdate>"
            + XML.encodeAsXML(unknown.equals(dateIssued) ? eddCreationDate : dateIssued)
            + "</pubdate>\n"
            + "        <title>"
            + XML.encodeAsXML(title(language))
            + "</title>\n"
            + "        <edition>"
            + unknown
            + "</edition>\n"
            +
            // geoform vocabulary
            // https://www.fgdc.gov/csdgmgraphical/ideninfo/citat/citinfo/type.htm
            "        <geoform>tabular digital data</geoform>\n"
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
            +
            // EDDGrid has ngdc's <publish_cntinfo> here
            "        </pubinfo>\n");

    // online resources   .html, .graph, WMS
    writer.write(
        "        <onlink>"
            + XML.encodeAsXML(datasetUrl + ".html")
            + "</onlink>\n"
            + (accessibleViaMAG.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(datasetUrl + ".graph") + "</onlink>\n")
            + (accessibleViaSubset.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(datasetUrl + ".subset") + "</onlink>\n")
            + (accessibleViaSOS.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(sosUrl) + "</onlink>\n")
            + (accessibleViaWMS.length() > 0
                ? ""
                : "        <onlink>" + XML.encodeAsXML(wmsUrl) + "</onlink>\n"));
    // EDDGrid has  <CI_OnlineResource> here

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
            + XML.encodeAsXML(summary(language))
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
    String conEmail = creatorEmail;
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
            + XML.encodeAsXML(unknown)
            + "</cntpos>\n"
            + // required
            "        <cntaddr>\n"
            + "          <addrtype>Mailing and Physical Address</addrtype>\n"
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
            + XML.encodeAsXML(unknown)
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

    // Spatial Data Organization information  (very different for EDDTable and EDDGrid)
    writer.write(
        "  <spdoinfo>\n"
            +
            // high level description, see list at
            // https://www.fgdc.gov/csdgmgraphical/sdorg/dirspref/dirsprm.htm
            "    <direct>Point</direct>\n"
            + "    <ptvctinf>\n"
            + "      <sdtstrm>\n"
            +
            // see
            // https://www.fgdc.gov/csdgmgraphical/sdorg/dirspref/objinfo/pntveco/sdtstrm/sdtstyp.htm
            "        <sdtstyp>"
            + XML.encodeAsXML(
                cdmDataType.equals(CDM_PROFILE)
                    ? "Point"
                    : cdmDataType.equals(CDM_RADIALSWEEP)
                        ? "Node, network"
                        : // ???
                        cdmDataType.equals(CDM_TIMESERIES)
                            ? "Point"
                            : cdmDataType.equals(CDM_TIMESERIESPROFILE)
                                ? "Point"
                                : cdmDataType.equals(CDM_SWATH)
                                    ? "Node, network"
                                    : // ???
                                    cdmDataType.equals(CDM_TRAJECTORY)
                                        ? "String"
                                        : // ???
                                        cdmDataType.equals(CDM_TRAJECTORYPROFILE)
                                            ? "String"
                                            : // ???
                                            "Point")
            + // CDM_POINT or CDM_OTHER
            "</sdtstyp>\n"
            + "      </sdtstrm>\n"
            + "    </ptvctinf>\n"
            + "  </spdoinfo>\n");

    // Spatial Reference Information
    // since res info is never known and since "Unknown" not allowed, skip this section
    /*writer.write(
    "  <spref>\n" +
    "    <horizsys>\n" +
    "      <geograph>\n" +
    "        <latres>"  + unknown + "</latres>\n" +
    "        <longres>" + unknown + "</longres>\n" +
    "        <geogunit>Decimal degrees</geogunit>\n" +
    "      </geograph>\n" +
    "      <geodetic>\n" +
    "        <horizdn>D_WGS_1984</horizdn>\n" +  //??? I'm not certain this is true for all
    "        <ellips>WGS_1984</ellips>\n" +
    "        <semiaxis>6378137.000000</semiaxis>\n" +
    "        <denflat>298.257224</denflat>\n" +
    "      </geodetic>\n" +
    "    </horizsys>\n");

    int depthIndex = String2.indexOf(dataVariableDestinationNames, "depth");
    if (altEdv != null || depthEdv != null) {
        writer.write(
    "    <vertdef>\n" +

        (altEdv == null? "" :
    "      <altsys>\n" +
    "        <altdatum>" + unknown + "</altdatum>\n" +
    "        <altres>" + unknown + "</altres>\n" +
    "        <altunits>meters</altunits>\n" +
    "        <altenc>" + unknown + "</altenc>\n" +
    "      </altsys>\n") +

        (depthEdv == null? "" :
    "      <depthsys>\n" +
    "        <depthdn>" + unknown + "</depthdn>\n" +
    "        <depthres>" + unknown + "</depthres>\n" +
    "        <depthdu>meters</depthdu>\n" +
    "        <depthem>" + unknown + "</depthem>\n" +
    "      </depthsys>\n") +

    "    </vertdef>\n");
    }

    writer.write(
    "  </spref>\n");
    */

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
    List<EDDFileTypeInfo> dataFileTypes = EDD.getFileTypeOptions(false, false);
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
                  dataFileTypes.get(ft).getTableDescription(language)
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
              +
              // EDDGrid has <CI_OnlineResource> here
              "              </networka>\n"
              + "            </computer>\n"
              + "          </onlinopt>\n"
              + "        </digtopt>\n"
              + "      </digform>\n");

    // image file types
    List<EDDFileTypeInfo> imageFileTypes = EDD.getFileTypeOptions(false, true);
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
                  imageFileTypes.get(ft).getTableDescription(language)
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
              +
              // EDDGrid has  <CI_OnlineResource> here
              "              </networka>\n"
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
            +
            // EDDGrid metstdn has ": Extensions for Remote Sensing Metadata"
            "    <metstdn>Content Standard for Digital Geospatial Metadata</metstdn>\n"
            + "    <metstdv>FGDC-STD-012-2002</metstdv>\n"
            + "    <mettc>universal time</mettc>\n"
            +
            // metadata access constraints
            "    <metac>"
            + XML.encodeAsXML(getAccessibleTo() == null ? "None." : "Authorized users only")
            + "</metac>\n"
            +
            // metadata use constraints
            "    <metuc>"
            + XML.encodeAsXML(license)
            + "</metuc>\n"
            + "  </metainfo>\n"
            + "</metadata>\n");
  }

  private void lower_writeISO19115(int language, Writer writer)
      throws UnsupportedStorageException, DataStoreException, JAXBException, IOException {

    Metadata metadata =
        MetadataBuilder.buildMetadata(
            language,
            datasetID,
            creationTimeMillis(),
            combinedGlobalAttributes(),
            dataVariables(),
            !String2.isSomething(accessibleViaWMS()),
            !String2.isSomething(accessibleViaSubset()));
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
   * The template is initially based on EDDGrid.writeISO19115 <br>
   * modified as warranted by looking at <br>
   * http://oos.soest.hawaii.edu/pacioos/metadata/iso/PACN_FISH_TRANSECT.xml <br>
   * Made pretty via TestAll: XML.prettyXml(in, out) and other small changes <br>
   * and stored on Bob's computer as <br>
   * c:/programs/iso19115/PACN_FISH_TRANSECTpretty.xml <br>
   * I note that: <br>
   * * PACN_FISH uses an ISO resources list, whereas ncISO uses one from NGDC.
   *
   * <p>Probably better example (from Ted Habermann) is <br>
   * from Nearshore Sensor 1 dataset from <br>
   * http://oos.soest.hawaii.edu/thredds/idd/nss_hioos.html <br>
   * and stored on Bob's computer as F:/programs/iso19115/ns01agg.xml <br>
   * 2014-09-24 Example with protocols: <br>
   * http://oos.soest.hawaii.edu/pacioos/metadata/roms_hiig_forecast.xml <br>
   * See also https://geo-ide.noaa.gov/wiki/index.php?title=NcISO#Questions_and_Answers
   *
   * <p>This is usually just called by the dataset's constructor, at the end of
   * EDDTable/Grid.ensureValid.
   *
   * <p>Help with schema: http://www.schemacentral.com/sc/niem21/e-gmd_contact-1.html <br>
   * List of nilReason: http://www.schemacentral.com/sc/niem21/a-gco_nilReason.html
   *
   * @param language the index of the selected language
   * @param writer a UTF-8 writer
   * @throws Throwable if trouble (e.g., no latitude and longitude variables)
   */
  @Override
  public void writeISO19115(int language, Writer writer) throws Throwable {
    // FUTURE: support datasets with x,y (and not longitude,latitude)?

    if (EDStatic.config.useSisISO19115) {
      lower_writeISO19115(language, writer);
      return;
    }

    if (lonIndex < 0 || latIndex < 0)
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.messages.get(Message.QUERY_ERROR, 0)
                  + EDStatic.messages.get(Message.NO_XXX_NO_LL, 0),
              EDStatic.messages.get(Message.QUERY_ERROR, language)
                  + EDStatic.messages.get(Message.NO_XXX_NO_LL, language)));

    String tErddapUrl = EDStatic.preferredErddapUrl;
    String datasetUrl = tErddapUrl + "/tabledap/" + datasetID;
    // String wcsUrl     = tErddapUrl + "/wcs/"     + datasetID() + "/" + wcsServer;  // "?" at end?
    String wmsUrl = tErddapUrl + "/wms/" + datasetID() + "/" + WMS_SERVER; // "?" at end?
    String domain = EDStatic.config.baseUrl;
    if (domain.startsWith("http://")) domain = domain.substring(7);
    else if (domain.startsWith("https://")) domain = domain.substring(8);
    String eddCreationDate = Calendar2.millisToIsoDateString(creationTimeMillis());

    String acknowledgement =
        combinedGlobalAttributes.getString(language, "acknowledgement"); // acdd 1.3
    if (acknowledgement == null)
      acknowledgement = combinedGlobalAttributes.getString(language, "acknowledgment"); // acdd 1.0
    String contributorName = combinedGlobalAttributes.getString(language, "contributor_name");
    String contributorEmail = combinedGlobalAttributes.getString(language, "contributor_email");
    String contributorRole = combinedGlobalAttributes.getString(language, "contributor_role");
    String creatorName = combinedGlobalAttributes.getString(language, "creator_name");
    String creatorEmail = combinedGlobalAttributes.getString(language, "creator_email");
    String creatorType = combinedGlobalAttributes.getString(language, "creator_type");
    creatorType = String2.validateAcddContactType(creatorType);
    if (!String2.isSomething2(creatorType) && String2.isSomething2(creatorName))
      creatorType = String2.guessAcddContactType(creatorName);
    if (creatorType == null) // creatorType will be something
    creatorType = "person"; // assume
    String dateCreated =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString(language, "date_created")); // "" if trouble
    String dateIssued =
        Calendar2.tryToIsoString(
            combinedGlobalAttributes.getString(language, "date_issued")); // "" if trouble
    if (dateCreated.length() > 10) dateCreated = dateCreated.substring(0, 10);
    if (dateIssued.length() > 10) dateIssued = dateIssued.substring(0, 10);
    if (dateCreated.startsWith("0000")) // year=0000 isn't valid
    dateCreated = "";
    if (dateIssued.startsWith("0000")) dateIssued = "";
    String history = combinedGlobalAttributes.getString(language, "history");
    String infoUrl = combinedGlobalAttributes.getString(language, "infoUrl");
    String institution = combinedGlobalAttributes.getString(language, "institution");
    String keywords = combinedGlobalAttributes.getString(language, "keywords");
    if (keywords == null) { // use the crude, ERDDAP keywords
      keywords = EDStatic.config.keywords;
    }
    String license = combinedGlobalAttributes.getString(language, "license");
    String project = combinedGlobalAttributes.getString(language, "project");
    if (project == null) project = institution;
    String standardNameVocabulary =
        combinedGlobalAttributes.getString(language, "standard_name_vocabulary");

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
    EDVLat latEdv = (EDVLat) dataVariables[latIndex];
    EDVLon lonEdv = (EDVLon) dataVariables[lonIndex];
    EDVTime timeEdv =
        timeIndex < 0 || testMinimalMetadata ? null : (EDVTime) dataVariables[timeIndex];
    EDVAlt altEdv = altIndex < 0 || testMinimalMetadata ? null : (EDVAlt) dataVariables[altIndex];
    EDVDepth depthEdv =
        depthIndex < 0 || testMinimalMetadata ? null : (EDVDepth) dataVariables[depthIndex];

    String minTime = ""; // iso string with Z, may be ""
    String maxTime = "";
    if (timeEdv != null) {
      minTime = timeEdv.destinationMinString(); // differs from EDDGrid, may be ""
      maxTime = timeEdv.destinationMaxString();
      if (minTime.startsWith("0000")) minTime = ""; // 0000 is invalid in ISO 19115
      if (maxTime.startsWith("0000")) maxTime = "";
    }

    double minVert =
        Double.NaN; // in destination units (may be positive = up[I use] or down?! any units)
    double maxVert = Double.NaN;
    if (altEdv != null) {
      minVert = altEdv.destinationMinDouble();
      maxVert = altEdv.destinationMaxDouble();
    } else if (depthEdv != null) {
      minVert = -depthEdv.destinationMaxDouble(); // make into alt
      maxVert = -depthEdv.destinationMinDouble();
    }

    StringArray standardNames = new StringArray();
    if (!testMinimalMetadata) {
      for (EDV dataVariable : dataVariables) {
        String sn = dataVariable.combinedAttributes().getString(language, "standard_name");
        if (sn != null) standardNames.add(sn);
      }
    }

    // lon,lat Min/Max
    // ??? I'm not certain but I suspect ISO requires lon to be +/-180.
    //    I don't see guidance for 0 - 360 datasets.
    // so just deal with some of the options
    // and use (float) to avoid float->double bruising
    // default: just the lon part already in -180 to 180.
    // EDDGrid doesn't allow for NaN
    float lonMin = (float) lonEdv.destinationMinDouble();
    float lonMax = (float) lonEdv.destinationMaxDouble();
    if (!Float.isNaN(lonMin) && !Float.isNaN(lonMax)) {
      lonMin = (float) Math2.minMax(-180, 180, lonMin);
      lonMax = (float) Math2.minMax(-180, 180, lonMax);
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
    }
    float latMin = (float) latEdv.destinationMinDouble();
    float latMax = (float) latEdv.destinationMaxDouble();
    if (!Float.isNaN(latMin)) latMin = (float) Math2.minMax(-90, 90, latMin);
    if (!Float.isNaN(latMax)) latMax = (float) Math2.minMax(-90, 90, latMax);

    // write the xml
    writer.write( // ISO19115
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
            +
            // Ted Haberman says 19115-2 is correct.  Part 2 has improvements for lots of
            // things, not just "Imagery and Gridded Data"
            // PacN dataset had "with Biological Extensions" from NGDC, but I'm not using those.
            "    <gco:CharacterString>ISO 19115-2 Geographic Information - Metadata Part 2 Extensions "
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
            + (2 + (timeEdv == null ? 0 : 1) + (altEdv == null && depthEdv == null ? 0 : 1))
            + "</gco:Integer>\n"
            + "      </gmd:numberOfDimensions>\n"
            +
            // longitude is "column" dimension
            "      <gmd:axisDimensionProperties>\n"
            + "        <gmd:MD_Dimension>\n"
            + "          <gmd:dimensionName>\n"
            + "            <gmd:MD_DimensionNameTypeCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
            + "codeListValue=\"column\">column</gmd:MD_DimensionNameTypeCode>\n"
            + "          </gmd:dimensionName>\n"
            + "          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n"
            + "        </gmd:MD_Dimension>\n"
            + "      </gmd:axisDimensionProperties>\n"
            +
            // latitude is "row" dimension
            "      <gmd:axisDimensionProperties>\n"
            + "        <gmd:MD_Dimension>\n"
            + "          <gmd:dimensionName>\n"
            + "            <gmd:MD_DimensionNameTypeCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode\" "
            + "codeListValue=\"row\">row</gmd:MD_DimensionNameTypeCode>\n"
            + "          </gmd:dimensionName>\n"
            + "          <gmd:dimensionSize gco:nilReason=\"unknown\"/>\n"
            + "        </gmd:MD_Dimension>\n"
            + "      </gmd:axisDimensionProperties>\n"
            +

            // altitude or depth is "vertical" dimension
            (altEdv == null && depthEdv == null
                ? ""
                : """
                          <gmd:axisDimensionProperties>
                            <gmd:MD_Dimension>
                              <gmd:dimensionName>
                                <gmd:MD_DimensionNameTypeCode \
                    codeList="https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode" \
                    codeListValue="vertical">vertical</gmd:MD_DimensionNameTypeCode>
                              </gmd:dimensionName>
                              <gmd:dimensionSize gco:nilReason="unknown"/>
                            </gmd:MD_Dimension>
                          </gmd:axisDimensionProperties>
                    """)
            +

            // time is "temporal" dimension
            (timeEdv == null
                ? ""
                : """
                          <gmd:axisDimensionProperties>
                            <gmd:MD_Dimension>
                              <gmd:dimensionName>
                                <gmd:MD_DimensionNameTypeCode \
                    codeList="https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_DimensionNameTypeCode" \
                    codeListValue="temporal">temporal</gmd:MD_DimensionNameTypeCode>
                              </gmd:dimensionName>
                              <gmd:dimensionSize gco:nilReason="unknown"/>
                            </gmd:MD_Dimension>
                          </gmd:axisDimensionProperties>
                    """)
            +

            // cellGeometry
            // ??? Ted Habermann has no answer for cellGeometry for tabular data
            "      <gmd:cellGeometry>\n"
            + "        <gmd:MD_CellGeometryCode codeList=\"\" codeListValue=\"\" codeSpace=\"\" />\n"
            + "      </gmd:cellGeometry>\n"
            + "      <gmd:transformationParameterAvailability gco:nilReason=\"unknown\"/>\n"
            + "    </gmd:MD_GridSpatialRepresentation>\n"
            + "  </gmd:spatialRepresentationInfo>\n");

    // metadataExtensionIfo for taxomony would go here

    // *** IdentificationInfo loop
    int iiDataIdentification = 0;
    int iiERDDAP = 1;
    int iiOPeNDAP = 2;
    int iiWMS = 3;
    int iiSubset = 4;
    for (int ii = 0; ii <= iiSubset; ii++) {

      // currently, no tabular datasets are accessibleViaWMS
      if (ii == iiWMS && accessibleViaWMS().length() > 0) continue;

      // not all datasets are accessible via .subset
      if (ii == iiSubset && accessibleViaSubset().length() > 0) continue;

      writer.write(
          "  <gmd:identificationInfo>\n"
              + (ii == iiDataIdentification
                  ? "    <gmd:MD_DataIdentification id=\"DataIdentification\">\n"
                  : ii == iiERDDAP
                      ? "    <srv:SV_ServiceIdentification id=\"ERDDAP-tabledap\">\n"
                      : ii == iiOPeNDAP
                          ? "    <srv:SV_ServiceIdentification id=\"OPeNDAP\">\n"
                          : ii == iiWMS
                              ? "    <srv:SV_ServiceIdentification id=\"OGC-WMS\">\n"
                              : ii == iiSubset
                                  ? "    <srv:SV_ServiceIdentification id=\"search\">\n"
                                  : "    <gmd:ERROR id=\"ERROR\">\n")
              + "      <gmd:citation>\n"
              + "        <gmd:CI_Citation>\n"
              + "          <gmd:title>\n"
              + "            <gco:CharacterString>"
              + XML.encodeAsXML(title(language))
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
              // infoUrl, institution
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
              + XML.encodeAsXML(summary(language))
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
        if (!CDM_OTHER.equals(cdmDataType(language))) {
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
                  + cdmDataType(language)
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
                + "        <gco:LocalName>ERDDAP tabledap</gco:LocalName>\n"
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

      } else if (ii == iiSubset) {
        writer.write(
            "      <srv:serviceType>\n"
                + "        <gco:LocalName>ERDDAP Subset</gco:LocalName>\n"
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
              + (Float.isNaN(lonMin)
                  ? // differs from EDDGrid since it may be NaN
                  "              <gmd:westBoundLongitude gco:nilReason=\"unknown\" />\n"
                  : "              <gmd:westBoundLongitude>\n"
                      + "                <gco:Decimal>"
                      + lonMin
                      + "</gco:Decimal>\n"
                      + "              </gmd:westBoundLongitude>\n")
              + (Float.isNaN(lonMax)
                  ? "              <gmd:eastBoundLongitude gco:nilReason=\"unknown\" />\n"
                  : "              <gmd:eastBoundLongitude>\n"
                      + "                <gco:Decimal>"
                      + lonMax
                      + "</gco:Decimal>\n"
                      + "              </gmd:eastBoundLongitude>\n")
              + (Float.isNaN(latMin)
                  ? "              <gmd:southBoundLatitude gco:nilReason=\"unknown\" />\n"
                  : "              <gmd:southBoundLatitude>\n"
                      + "                <gco:Decimal>"
                      + latMin
                      + "</gco:Decimal>\n"
                      + "              </gmd:southBoundLatitude>\n")
              + (Float.isNaN(latMax)
                  ? "              <gmd:northBoundLatitude gco:nilReason=\"unknown\" />\n"
                  : "              <gmd:northBoundLatitude>\n"
                      + "                <gco:Decimal>"
                      + latMax
                      + "</gco:Decimal>\n"
                      + "              </gmd:northBoundLatitude>\n")
              + "            </gmd:EX_GeographicBoundingBox>\n"
              + "          </gmd:geographicElement>\n"
              + (minTime.length() == 0
                  ? ""
                  : // differs from EDDGrid
                  "          <gmd:temporalElement>\n"
                      + "            <gmd:EX_TemporalExtent"
                      + (ii == iiDataIdentification ? " id=\"boundingTemporalExtent\"" : "")
                      + ">\n"
                      + "              <gmd:extent>\n"
                      + "                <gml:TimePeriod gml:id=\""
                      + // id is required    //ncISO has "d293e106"
                      (ii == iiDataIdentification
                          ? "DI"
                          : ii == iiERDDAP
                              ? "ED"
                              : ii == iiOPeNDAP
                                  ? "OD"
                                  : ii == iiWMS ? "WMS" : ii == iiSubset ? "SUB" : "ERROR")
                      + "_gmdExtent_timePeriod_id\">\n"
                      + "                  <gml:description>seconds</gml:description>\n"
                      + "                  <gml:beginPosition>"
                      + minTime
                      + "</gml:beginPosition>\n"
                      + (maxTime.length() == 0
                          ?
                          // http://marinemetadata.org is GONE! See
                          // http://mmisw.org/ont/cf/parameter/...
                          // now? see
                          // https://marinemetadata.org/workshops/mmiworkshop06/materials/track1/sensorml/EXAMPLES/Garmin_GPS_SensorML
                          "                  <gml:endPosition indeterminatePosition=\"now\" />\n"
                          : "                  <gml:endPosition>"
                              + maxTime
                              + "</gml:endPosition>\n")
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
                + "codeListValue=\"tight\">tight</srv:SV_CouplingType>\n"
                + "      </srv:couplingType>\n"
                + "      <srv:containsOperations>\n"
                + "        <srv:SV_OperationMetadata>\n"
                + "          <srv:operationName>\n"
                + "            <gco:CharacterString>ERDDAPtabledapDatasetQueryAndAccess</gco:CharacterString>\n"
                + "          </srv:operationName>\n"
                + "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
                + // ??? Distributed Computing Platform
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
                "                <gco:CharacterString>ERDDAP:tabledap</gco:CharacterString>\n"
                + "              </gmd:protocol>\n"
                + "              <gmd:name>\n"
                + "                <gco:CharacterString>ERDDAP-tabledap</gco:CharacterString>\n"
                + "              </gmd:name>\n"
                + "              <gmd:description>\n"
                + "                <gco:CharacterString>ERDDAP's tabledap service (a flavor of OPeNDAP) "
                + "for tabular (sequence) data. Add different extensions "
                + "(e.g., .html, .graph, .das, .dds) to the base URL for different purposes.</gco:CharacterString>\n"
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
                + // ??? Distributed Computing Platform
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
                + "                <gco:CharacterString>An OPeNDAP service for tabular (sequence) data. "
                + "Add different extensions (e.g., .html, .das, .dds) to the "
                + "base URL for different purposes.</gco:CharacterString>\n"
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
                +
                // see list at
                // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv from
                // John Maurer
                "                <gco:CharacterString>OGC:WMS</gco:CharacterString>\n"
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

      if (ii == iiSubset) {
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
                + "            <gco:CharacterString>ERDDAP_Subset</gco:CharacterString>\n"
                + "          </srv:operationName>\n"
                + "          <srv:DCP gco:nilReason=\"unknown\"/>\n"
                + // Distributed Computing Platform
                "          <srv:connectPoint>\n"
                + "            <gmd:CI_OnlineResource>\n"
                + "              <gmd:linkage>\n"
                + "                <gmd:URL>"
                + XML.encodeAsXML(datasetUrl)
                + ".subset</gmd:URL>\n"
                + "              </gmd:linkage>\n"
                + "              <gmd:protocol>\n"
                +
                // see list at
                // https://github.com/OSGeo/Cat-Interop/blob/master/LinkPropertyLookupTable.csv from
                // John Maurer
                "                <gco:CharacterString>search</gco:CharacterString>\n"
                + "              </gmd:protocol>\n"
                + "              <gmd:name>\n"
                + "                <gco:CharacterString>Subset</gco:CharacterString>\n"
                + "              </gmd:name>\n"
                + "              <gmd:description>\n"
                + "                <gco:CharacterString>Web page to facilitate selecting subsets of the dataset</gco:CharacterString>\n"
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

    // contentInfo  (dataVariables)
    writer.write(
        "  <gmd:contentInfo>\n"
            + "    <gmi:MI_CoverageDescription>\n"
            + "      <gmd:attributeDescription gco:nilReason=\"unknown\"/>\n"
            + // ???
            // from http://www.schemacentral.com/sc/niem21/t-gco_CodeListValue_Type.html
            "      <gmd:contentType>\n"
            + "        <gmd:MD_CoverageContentTypeCode "
            + "codeList=\"https://data.noaa.gov/resources/iso19139/schema/resources/Codelist/gmxCodelists.xml#gmd:MD_CoverageContentTypeCode\" "
            + "codeListValue=\"physicalMeasurement\">physicalMeasurement</gmd:MD_CoverageContentTypeCode>\n"
            + "      </gmd:contentType>\n");

    // dataVariables
    for (EDV edv : dataVariables) {
      if (edv == lonEdv || edv == latEdv || edv == altEdv || edv == depthEdv) continue;
      String tUnits = testMinimalMetadata ? null : edv.ucumUnits();
      writer.write(
          "      <gmd:dimension>\n"
              + // in ncIso, a dimension???
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
                      + XML.encodeAsHTMLAttribute(SSR.minimalPercentEncode(edv.ucumUnits()))
                      + "\"/>\n")
              + "        </gmd:MD_Band>\n"
              + "      </gmd:dimension>\n");
    }
    writer.write(
        """
                </gmi:MI_CoverageDescription>
              </gmd:contentInfo>
            """);

    // distibutionInfo: erddap treats distributionInfo same as serviceInfo: use EDStatic.admin...
    //   ncISO uses creator
    writer.write(
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
   * Given a source table (e.g., from one file), this tries to guess which columns should be in
   * subsetVariables.
   *
   * <p>This is an impossible task unless you look at the entire dataset. With the info from only
   * one table, there is no way to calculate the true nDistinctValues for a column. This is very
   * conservative.
   *
   * <p>This i
   *
   * @param sourceTable a sample source table that has some data. This table won't be changed.
   * @param destTable a table with columns paralleling sourceTable, but with the destinationNames
   *     for the columns. This table won't be changed. It is a RuntimeException if
   *     sourceTable.nColumns != destTable.nColumns.
   * @param oneFilePerDataset This signals the unusual case where there is probably just this one
   *     table/file for the whole dataset, e.g., EML, BCO-DMO.
   * @return a CSSV string with the suggested subsetVariables destination column names.
   */
  public static String suggestSubsetVariables(
      Table sourceTable, Table destTable, boolean oneFilePerDataset) {
    int nRows = sourceTable.nRows();
    if (nRows <= 1) return "";
    int nCols = sourceTable.nColumns();
    Test.ensureEqual(nCols, destTable.nColumns(), "sourceTable.nColumns != destTable.nColumns");
    StringArray suggest = new StringArray();
    for (int col = 0; col < nCols; col++) {
      PrimitiveArray pa = (PrimitiveArray) sourceTable.getColumn(col).clone();
      pa.sort();
      if (pa.elementType() != PAType.STRING) {
        // find missing value
        PAOne tmv = pa.tryToFindNumericMissingValue();
        if (tmv != null) {

          BitSet keep = new BitSet();
          keep.set(0, nRows);
          pa.applyConstraint(
              false, // morePrecise,
              keep, "!=", "" + tmv);
          pa.justKeep(keep);

          // then add 1 copy back in (if any removed)
          if (pa.size() < nRows) pa.addPAOne(tmv);
        }
      }
      int tnRows = pa.size();
      pa.removeDuplicates(); // don't worry about mv's: they reduce to 1 value
      int nUnique = Math.max(1, pa.size());
      double nDup = tnRows / (double) nUnique; // average number of duplicates (1.0=no duplicates)
      if (debugMode)
        String2.log(
            ">> "
                + sourceTable.getColumnName(col)
                + " isString?"
                + (pa instanceof StringArray)
                + " nRows="
                + nRows
                + " tnRows="
                + tnRows
                + " pa.size="
                + pa.size()
                + " nDup="
                + nDup);
      if (pa.size() == 1
          || (oneFilePerDataset
              && nUnique < 10000
              && nDup > 4
              && nUnique
                  < (pa instanceof StringArray
                      ? 1000 * nDup
                      : // allow more if lots of dups
                      pa.isIntegerType() ? 500 * nDup : 200 * nDup))) { // floating point

        // if long strings skip it.
        if (pa instanceof StringArray sa) {
          if (sa.maxStringLength() > 100) continue;
        }

        // tName may not be the final destTable name (e.g., may not be valid)
        // This is imperfect (won't catch LLAT), but better than nothing.
        String tName = destTable.getColumnName(col);
        Attributes sourceAtts = sourceTable.columnAttributes(col);
        Attributes destAtts = destTable.columnAttributes(col);
        String tUnits = destAtts.getString("units");
        if (tUnits == null) tUnits = sourceAtts.getString("units");
        tName =
            suggestDestinationName(
                tName, sourceAtts, destAtts, tUnits, null, Float.NaN, false); // tryToFindLLAT
        suggest.add(tName);
      }
    }
    // String2.log(">> " + suggest.toString());
    return suggest.toString();
  }
}
