/*
 * Table Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.*;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.FileNameUtility;
import gov.noaa.pfel.coastwatch.griddata.Matlab;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.pointdata.parquet.ParquetWriterBuilder;
import gov.noaa.pfel.coastwatch.util.HtmlWidgets;
import gov.noaa.pfel.coastwatch.util.SSR;
import gov.noaa.pfel.coastwatch.util.SimpleXMLReader;
import gov.noaa.pfel.coastwatch.util.Tally;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.ParquetReadOptions;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.LocalInputFile;
import org.apache.parquet.io.LocalOutputFile;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.LogicalTypeAnnotation.TimeUnit;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types.MessageTypeBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * This class holds tabular data as
 *
 * <ul>
 *   <li>globalAttributes (a list of (String)name=(PrimitiveArray)value)
 *   <li>columnAttributes - an ArrayList of Attributes, one for each column (each is a list of
 *       (String)name=(PrimitiveArray)value).
 *   <li>columnNames - a StringArray
 *   <li>the columns of data - an ArrayList of PrimitiveArray
 * </ul>
 *
 * This class is used by CWBrowser as a way to read tabular data from ASCII and .nc files and
 * Opendap, store the data in a standard in-memory format, and write the data to several types of
 * files (ASCII, MatLab, .nc, ...). Since there is often a lot of data, these objects are usually
 * short-lived.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-12-05
 */
public class Table {

  public static interface Rounder {
    double round(Double value) throws Exception;
  }

  private static interface WithColumnNames {
    void apply(String[] columnNames) throws Exception;
  }

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  /**
   * Set this to true (by calling reallyVerbose=true in your program, not by changing the code here)
   * if you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean reallyVerbose = false;

  /**
   * Set this to true (by calling debugMode=true in your program, not by changing the code here) if
   * you want lots and lots of diagnostic messages sent to String2.log.
   */
  public static boolean debugMode = false;

  /**
   * If true, readASCII allows data lines to have varying numbers of values and assumes that the
   * missing values are from the end columns.
   */
  public boolean allowRaggedRightInReadASCII = false;

  /** If true, readOpendap requestes compressed data. I think this should always be true. */
  public boolean opendapAcceptDeflate = true;

  /**
   * Since users use these numbers (not names) from the command line, the value for a given option
   * shouldn't ever change.
   */
  public static final int READ_ASCII = 0,
      READ_FLAT_NC = 1,
      READ_OPENDAP_SEQUENCE = 2,
      READ_4D_NC = 3;

  // the values here won't change as new file types are supported
  public static final int SAVE_AS_TABBED_ASCII = 0;
  public static final int SAVE_AS_FLAT_NC = 1;
  public static final int SAVE_AS_4D_NC = 2;
  public static final int SAVE_AS_MATLAB = 3;
  // public static final int SAVE_AS_HDF = 4;
  public static final String SAVE_AS_EXTENSIONS[] = {".asc", ".nc", ".nc", ".mat"};

  public static String BGCOLOR = "#ffffcc";

  // this will only change if changes are made that aren't backwards and forwards compatible
  public static final int ENHANCED_VERSION = 4;

  // related to ERDDAP
  /**
   * This is a list of all operator symbols (for my convenience in Table.parseDapQuery and
   * EDDTable.parseUserDapQuery): 2 letter ops are first). Note that all variables (numeric or
   * String) can be constrained via a ERDDAP constraint using any of these operators. If the source
   * can't support the operator (e.g., no source supports numeric =~ testing, and no source supports
   * string &lt; &lt;= &gt; &gt;= testing), ERDDAP will just get all the relevant data and do the
   * test itself.
   */
  public static final String OPERATORS[] =
  // EDDTableFromFiles.isOK relies on this order
  {"!=", PrimitiveArray.REGEX_OP, "<=", ">=", "=", "<", ">"};

  public static final String SEQUENCE_NAME = "s";

  public static String QUERY_ERROR = "Query error: ";

  public static String NOT_FOUND_EOF = " not found before end-of-file.";
  public static String ELAPSED_TIME = "elapsedTime";
  public static String WARNING_BAD_LINE_OF_DATA_IN = String2.WARNING + ": Bad line(s) of data in ";

  /**
   * Igor Text File File reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/II-09%20Data%20Import%20Export.pdf <br>
   * Command reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/V-01%20Reference.pdf
   */
  public static final String IgorCharset =
      File2.ISO_8859_1; // they are vague, but it is 1-byte, not UTF variant

  public static final String IgorNanString = "NaN"; // Igor Text File Format: use "NaN"
  public static final String IgorEndOfLine =
      "\r"; // Igor Text File Format: "a carriage return at the end of the line"
  // Igor Text File Format: "use the standard Igor date format (number of seconds since 1/1/1904)"
  public static final double IgorBaseSeconds = -2.0828448E9;
  public static final double IgorFactorToGetSeconds = 1;

  /**
   * Igor reserved names are selected names from "Built-In Operations by Category" from
   * https://www.wavemetrics.net/doc/igorman/IgorMan.pdf
   */
  public static final String[] IgorReservedNamesSA = { // sorted for simplicity (not required)
    "abs",
    "acos",
    "AddLitItem",
    "airyA",
    "airyAD",
    "airyB",
    "alog",
    "AnnotationInfo",
    "AnnotationList",
    "area",
    "asin",
    "Append",
    "areaXY",
    "atan",
    "AxisInfo",
    "AxisList",
    "BackgroundInfo",
    "Besseli",
    "Besselj",
    "Besselk",
    "Bessely",
    "bessI",
    "bessJ",
    "bessK",
    "bessY",
    "beta",
    "betai",
    "binomial",
    "BoundingBall",
    "break",
    "BrowseURL",
    "cabs",
    "CaptureHistory",
    "catch",
    "cd",
    "ceil",
    "cequal",
    "Chart",
    "char2num",
    "CheckBox",
    "CheckName",
    "CleanupName",
    "cmplx",
    "cmpstr",
    "ColorScale",
    "Concatenate",
    "conj",
    "Constant",
    "continue",
    "ContourInfo",
    "ContourZ",
    "ControlBar",
    "ConvexHull",
    "Convolve",
    "Correlate",
    "CountObjects",
    "cos",
    "cot",
    "coth",
    "cpowi",
    "CreationDate",
    "Cross",
    "csc",
    "CsrInfo",
    "CsrWave",
    "CsrWaveRef",
    "CTabList",
    "Cursor",
    "CurveFit",
    "CWT",
    "DataFolderDir",
    "DataFolderExists",
    "date",
    "date2secs",
    "date2Julian",
    "DateTime",
    "dawson",
    "default",
    "DefaultFont",
    "deltax",
    "DFREF",
    "Differentiate",
    "digamma",
    "dimDelta",
    "DimOffset",
    "DimSize",
    "Dir",
    "Display",
    "Duplicate",
    "DWT",
    "e",
    "EdgeStats",
    "Edit",
    "ei",
    "End",
    "EndMacro",
    "enoise",
    "erf",
    "erfc",
    "erfcw",
    "exists",
    "exp",
    "ExperimentModified",
    "expInt",
    "expnoise",
    "Extract",
    "factorial",
    "FakeData",
    "FastGaussTransform",
    "faverage",
    "FFT",
    "FilterIIR",
    "FindLevel",
    "FindListItem",
    "FindValue",
    "FitFunc",
    "floor",
    "FontList",
    "FStatus",
    "FTPDownload",
    "FTPUpload",
    "FUNCREF",
    "FuncRefInfo",
    "Function",
    "FunctionInfo",
    "FunctionList",
    "FunctionPath",
    "gamma",
    "gammaInc",
    "gammaln",
    "gammaNoise",
    "gammp",
    "gammq",
    "Gauss",
    "Gauss2D",
    "gcd",
    "gnoise",
    "Graph",
    "GraphMarquee",
    "GraphStyle",
    "GridStyle",
    "GuideInfo",
    "Hanning",
    "Hash",
    "hcsr",
    "hermite",
    "hide",
    "HilbertTransform",
    "Histogram",
    "i",
    "IFFT",
    "ilim",
    "imag",
    "ImageFilter",
    "ImageHistogram",
    "ImageInfo",
    "ImageNameList",
    "ImageRotate",
    "ImageSave",
    "ImageStats",
    "ImageThreshold",
    "ImageTransform",
    "ImageWindow",
    "IndexedDir",
    "IndexedFile",
    "IndexSort",
    "Inf",
    "InsertPoints",
    "Integrate",
    "IntegrateID",
    "interp",
    "Interp2D",
    "Interp3D",
    "inverseErf",
    "inverseErfc",
    "ItemsInList",
    "j",
    "jlim",
    "JulianToDate",
    "Label",
    "laguerre",
    "Layout",
    "LayoutInfo",
    "Legend",
    "limit",
    "ListMatch",
    "ln",
    "LoadData",
    "Loess",
    "log",
    "LowerStr",
    "Macro",
    "magsqr",
    "Make",
    "MakeIndex",
    "MatrixDet",
    "MatrixDot",
    "MatrixFilter",
    "MatrixRank",
    "MatrixTrace",
    "max",
    "mean",
    "Menu",
    "min",
    "mod",
    "modDate",
    "Modify",
    "ModuleName",
    "norm",
    "note",
    "Note",
    "Notebook",
    "num2char",
    "num2str",
    "numpnts",
    "numtype",
    "NVAR",
    "Open",
    "OperationList",
    "Optimize",
    "Override",
    "p",
    "p2rect",
    "PadString",
    "Panel",
    "ParamIsDefault",
    "PathInfo",
    "PathList",
    "PCA",
    "pcsr",
    "Pi",
    "PICTList",
    "Picture",
    "PlaySound",
    "pnt2x",
    "Point",
    "poly",
    "poly2D",
    "PolygonArea",
    "popup",
    "Preferences",
    "Print",
    "Proc",
    "ProcGlobal",
    "ProcedureText",
    "Project",
    "Prompt",
    "PulseStats",
    "pwd",
    "q",
    "qcsr",
    "Quit",
    "r",
    "r2polar",
    "real",
    "Rect",
    "Redimension",
    "Remove",
    "Rename",
    "Resample",
    "return",
    "Reverse",
    "RGBColor",
    "rightx",
    "root",
    "Rotate",
    "round",
    "rtGlobals",
    "s",
    "Save",
    "SaveData",
    "ScreenResolution",
    "sec",
    "Secs2Date",
    "Secs2Time",
    "SelectNumber",
    "SelectString",
    "SetAxis",
    "SetBackground",
    "SetDimLabel",
    "SetDrawLayer",
    "SetRandomSeed",
    "SetScale",
    "sign",
    "sin",
    "sinc",
    "sinh",
    "Sleep",
    "Slow",
    "Smooth",
    "Sort",
    "SortList",
    "SpecialDirPath",
    "SplitString",
    "sqrt",
    "Stack",
    "startMSTimer",
    "Static",
    "stopMSTimer",
    "str2num",
    "Strconstant",
    "String",
    "StringByKey",
    "StringCRC",
    "StringFromList",
    "StringList",
    "StringMatch",
    "strlen",
    "strsearch",
    "STRUCT",
    "Structure",
    "Submenu",
    "sum",
    "SVAR",
    "t",
    "Table",
    "TableInfo",
    "TableStyle",
    "Tag",
    "TagVal",
    "tan",
    "tanh",
    "TextBox",
    "TextFile",
    "ticks",
    "Tile",
    "TileWindows",
    "time",
    "TraceInfo",
    "trunc",
    "UniqueName",
    "Unwrap",
    "UpperStr",
    "URLDecode",
    "URLEncode",
    "Variable",
    "VariableList",
    "Variance",
    "vcsr",
    "version",
    "WAVE",
    "WaveCRC",
    "WaveDims",
    "WaveExists",
    "WaveMeanStdv",
    "WaveMax",
    "WaveMin",
    "WaveName",
    "WaveStats",
    "WaveTransform",
    "WaveType",
    "WaveUnits",
    "WhichListItem",
    "Window",
    "WinName",
    "wnoise",
    "x",
    "x2pnt",
    "xcsr",
    "y",
    "z",
    "zcsr"
  };

  public static final HashSet<String> IgorReservedNames = new HashSet();

  static {
    for (String s : IgorReservedNamesSA) IgorReservedNames.add(String2.canonical(s));
  }

  /**
   * A link to erddap2.css. HTML allows link or inline. XHTML only allows link (and no close link
   * tag!). See https://en.wikibooks.org/wiki/Cascading_Style_Sheets/Applying_CSS_to_HTML_and_XHTML
   */
  public static String ERD_TABLE_CSS =
      "<link href=\"https://coastwatch.pfeg.noaa.gov/erddap/images/erddap2.css\" rel=\"stylesheet\" type=\"text/css\">";

  // this is used to find out if all readNcCF code is tested: cc=code coverage
  // Bits are set to true when chunk of code is tested.
  public static BitSet ncCFcc = null; // null=inactive, new BitSet() = active

  /** An arrayList to hold 0 or more PrimitiveArray's with data. */
  protected ArrayList<PrimitiveArray> columns = new ArrayList<>();

  /** An arrayList to hold the column names. */
  protected StringArray columnNames = new StringArray();

  /**
   * This holds the global attributes ((String)name = (PrimitiveArray)value). Although a HashTable
   * is more appropriate for name=value pairs, this uses ArrayList to preserve the order of the
   * attributes. This may be null if not in use.
   */
  protected Attributes globalAttributes = new Attributes();

  /**
   * This holds the column Attributes ((String)name = (PrimitiveArray)value) in an arrayList.
   * Although a HashTable is more appropriate for name=value pairs, this uses ArrayList to preserve
   * the order of the attributes. This may be null if not in use.
   */
  protected ArrayList<Attributes> columnAttributes = new ArrayList<>();

  /** The one known valid url for readIobis. */
  public static final String IOBIS_URL = "http://www.iobis.org/OBISWEB/ObisControllerServlet";

  /** */
  public Table() {}

  /** A constructor that uses tGlobalAttributes as the global attributes (directly, not a clone). */
  public Table(Attributes tGlobalAttributes) {
    globalAttributes = tGlobalAttributes;
  }

  /**
   * Makes a table with the specified columnNames and dataTypes.
   *
   * @param dataType if dataType == null, the table will have all String columns Otherwise, it
   *     should be the same length as colName
   * @return a new table
   */
  public static Table makeEmptyTable(String colName[], String dataType[]) {
    Table table = new Table();
    int n = colName.length;
    for (int i = 0; i < n; i++) {
      table.addColumn(
          colName[i],
          PrimitiveArray.factory(
              PAType.fromCohortString(dataType == null ? "String" : dataType[i]), 8, false));
    }
    return table;
  }

  /** This clears everything. */
  public void clear() {
    columns.clear();
    columnNames.clear();

    globalAttributes.clear();
    columnAttributes.clear();
  }

  /**
   * This reads and ignores lines until it finds a line that matches skipHeaderToRegex.
   *
   * @param skipHeaderToRegex the regex to be matched (or null or "" if nothing to be done)
   * @param sourceName for an error message. Usually the name of the file.
   * @param linesReader the bufferedReader that is the data source
   * @return the number of lines read (0 if skipHeaderToRegex is null).
   * @throws Exception if trouble
   */
  public static int skipHeaderToRegex(
      String skipHeaderToRegex, String sourceName, BufferedReader linesReader) throws IOException {
    int linesRead = 0;
    if (skipHeaderToRegex != null && !skipHeaderToRegex.equals("")) {
      Pattern shtp = Pattern.compile(skipHeaderToRegex);
      while (true) {
        String s = linesReader.readLine(); // null if end. exception if trouble
        linesRead++;
        if (s == null)
          throw new SimpleException(
              MustBe.THERE_IS_NO_DATA
                  + " (skipHeaderToRegex="
                  + String2.toJson(skipHeaderToRegex)
                  + " not found in "
                  + (String2.isSomething(sourceName) ? sourceName : "[unknown]")
                  + ")");
        if (shtp.matcher(s).matches()) break;
      }
    }
    return linesRead;
  }

  /**
   * This converts the specified column from epochSeconds doubles to ISO 8601 Strings.
   *
   * @param timeIndex
   */
  public void convertEpochSecondsColumnToIso8601(int timeIndex) {
    int tnRows = nRows();
    PrimitiveArray pa = getColumn(timeIndex);
    StringArray sa = new StringArray(tnRows, false);
    for (int row = 0; row < tnRows; row++)
      sa.add(Calendar2.safeEpochSecondsToIsoStringTZ(pa.getDouble(row), ""));
    setColumn(timeIndex, sa);

    if (String2.isSomething(columnAttributes(timeIndex).getString("units")))
      columnAttributes(timeIndex).set("units", Calendar2.ISO8601TZ_FORMAT);
  }

  /**
   * This makes a deep clone of the current table (data and attributes).
   *
   * @param startRow
   * @param stride
   * @param endRow (inclusive) e.g., nRows()-1
   * @return a new Table.
   */
  public Table subset(int startRow, int stride, int endRow) {
    Table tTable = new Table();

    int n = columns.size();
    for (int i = 0; i < n; i++) tTable.columns.add(columns.get(i).subset(startRow, stride, endRow));

    tTable.columnNames = (StringArray) columnNames.clone();

    tTable.globalAttributes = (Attributes) globalAttributes.clone();

    for (int col = 0; col < columnAttributes.size(); col++)
      tTable.columnAttributes.add((Attributes) columnAttributes.get(col).clone());

    return tTable;
  }

  /**
   * This makes a deep clone of the entire current table (data and attributes).
   *
   * @return a new Table.
   */
  @Override
  public Object clone() {
    return subset(0, 1, nRows() - 1);
  }

  /**
   * This returns the current number of columns.
   *
   * @return the current number of columns
   */
  public int nColumns() {
    return columns.size();
  }

  /**
   * This returns the PrimitiveArray for a specific column.
   *
   * @param col (0..)
   * @return the corresponding PrimitiveArray
   * @throws Exception if col is invalid
   */
  public PrimitiveArray getColumn(int col) {
    if (col < 0 || col >= columns.size())
      throw new IllegalArgumentException(
          String2.ERROR
              + " in Table.getColumn: col="
              + col
              + " must be 0 ... "
              + (columns.size() - 1)
              + ".");
    return columns.get(col);
  }

  /**
   * This returns the PrimitiveArray for a specific column.
   *
   * @param columnName
   * @return the corresponding PrimitiveArray
   * @throws IllegalArgumentException if columnName is invalid
   */
  public PrimitiveArray getColumn(String columnName) {
    int col = findColumnNumber(columnName);
    if (col < 0)
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.getColumn: columnName=" + columnName + " not found.");
    return columns.get(col);
  }

  /**
   * This sets the PrimitiveArray for a specific column.
   *
   * @param col (0.. size-1)
   * @param pa the corresponding PrimitiveArray
   * @throws Exception if col is invalid
   */
  public void setColumn(int col, PrimitiveArray pa) {
    if (col >= columns.size())
      throw new IllegalArgumentException(
          String2.ERROR
              + " in Table.setColumn: col ("
              + col
              + ") is >= size ("
              + columns.size()
              + ").");
    if (pa == null)
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.setColumn(col=" + col + ", pa): pa is null.");
    columns.set(col, pa);
  }

  /**
   * This returns the PrimitiveArrays for all of the columns.
   *
   * @return the PrimitiveArray[]
   * @throws Exception if col is invalid
   */
  public PrimitiveArray[] getColumns() {
    PrimitiveArray pa[] = new PrimitiveArray[columns.size()];
    for (int i = 0; i < columns.size(); i++) pa[i] = getColumn(i);
    return pa;
  }

  /**
   * This simplifies (changes the datatype to the simplest possible type) a column.
   *
   * @param col the column to be simplified, 0...
   */
  public void simplify(int col) {
    columns.set(col, getColumn(col).simplify(getColumnName(col)));
  }

  /** This simplifies (changes the datatype to the simplest possible type) all columns. */
  public void simplify() {
    int nColumns = columns.size();
    for (int col = 0; col < nColumns; col++) simplify(col);
  }

  /**
   * This converts columns with _Unsigned=true attributes to be unsigned PA's (e.g., UByteArray). In
   * any case, the _Unsigned attribute is removed.
   */
  public void convertToUnsignedPAs() {
    /* 2020-07-22 This is no longer needed because NcHelper calls attributes.convertSomeSignedToUnsigned() and removes _Unsigned att.

            String attsToCheck[] = {"_FillValue", "missing_value", "actual_range", "data_min", "data_max"};
            int nColumns = columns.size();
            for (int col = 0; col < nColumns; col++) {
                Attributes atts = columnAttributes(col);
                String unsigned = atts.getString("_Unsigned");
                atts.remove("_Unsigned");
                if ("true".equals(unsigned)) {
                    PrimitiveArray pa = getColumn(col);
                    PAType paType = pa.elementType();
                    if (paType == PAType.BYTE ||
                        paType == PAType.SHORT ||
                        paType == PAType.INT ||
                        paType == PAType.LONG) {
                        setColumn(col, pa.makeUnsignedPA());
                        for (int i = 0; i < attsToCheck.length; i++) {
                            PrimitiveArray tAtt = atts.get(attsToCheck[i]);
                            if (tAtt != null && tAtt.elementType() == paType)
                                atts.set(attsToCheck[i], tAtt.makeUnsignedPA());
                        }
                    }
                }
            }
    */
  }

  /**
   * This converts columns with unsigned PA's (e.g. UByteArray) to have _Unsigned=true attribute and
   * signed PA's (e.g., for right before storing in nc file).
   */
  /*    public void convertToSignedPAs() {
          String attsToCheck[] = {"_FillValue", "missing_value", "actual_range", "data_min", "data_max"};
          int nColumns = columns.size();
          for (int col = 0; col < nColumns; col++) {
              PrimitiveArray pa = getColumn(col);
              if (pa.isUnsigned()) {
                  PAType paType = pa.elementType();
                  if (paType == PAType.UBYTE ||
                      paType == PAType.USHORT ||
                      paType == PAType.UINT ||
                      paType == PAType.ULONG) {
                      setColumn(col, pa.makeSignedPA());
                      Attributes atts = columnAttributes(col);
                      atts.set("_Unsigned", true);
                      for (int i = 0; i < attsToCheck.length; i++) {
                          PrimitiveArray tAtt = atts.get(attsToCheck[i]);
                          if (tAtt != null && tAtt.elementType() == paType)
                              atts.set(attsToCheck[i], tAtt.makeSignedPA());
                      }
                  }
              }
          }
      }
  */

  /**
   * This runs StringArray.convertIsSomething2 (change all e.g., "N/A" to "") on the specified
   * column, if it is a StringArray column.
   */
  public int convertIsSomething2(int col) {
    PrimitiveArray pa = getColumn(col);
    if (pa.elementType() == PAType.STRING) return ((StringArray) pa).convertIsSomething2();
    return 0;
  }

  /**
   * This runs StringArray.convertIsSomething2 (change all e.g., "N/A" to "") on all StringArray
   * columns.
   */
  public void convertIsSomething2() {
    int nColumns = columns.size();
    for (int col = 0; col < nColumns; col++) convertIsSomething2(col);
  }

  /**
   * This copies the values from one row to another already extant row (without affecting any other
   * rows).
   *
   * @param from the 'from' row
   * @param to the 'to' row
   */
  public void copyRow(int from, int to) {
    PrimitiveArray.copyRow(columns, from, to);
  }

  /**
   * This writes a row to a DataOutputStream.
   *
   * @param row
   * @param dos
   */
  public void writeRowToDOS(int row, DataOutputStream dos) throws Exception {
    int nColumns = columns.size();
    for (int col = 0; col < nColumns; col++) columns.get(col).writeDos(dos, row);
  }

  /**
   * This reads/appends a row from a DataInputStream.
   *
   * @param dis
   */
  public void readRowFromDIS(DataInputStream dis) throws Exception {
    int nColumns = columns.size();
    for (int col = 0; col < nColumns; col++) columns.get(col).readDis(dis, 1); // read 1 value
  }

  /**
   * This returns an estimate of the number of bytes per row (assuming 20 per String column, which
   * is probably low).
   */
  public int estimatedBytesPerRow() {
    int sum = 0;
    for (int col = 0; col < columnAttributes.size(); col++) sum += getColumn(col).elementSize();
    return sum;
  }

  /**
   * This inserts a column in the table.
   *
   * @param position 0..
   * @param name the name for the column If name is null, the name will be "Column<#>" (0..)
   * @param pa the data for the column. The data is not copied. This primitiveArray will continue to
   *     be used to store the data. This doesn't ensure that it has the correct size().
   * @param attributes the attributes (used directly, not a copy) for the column.
   * @return the column's number
   */
  public int addColumn(int position, String name, PrimitiveArray pa, Attributes attributes) {
    if (pa == null) throw new SimpleException(String2.ERROR + " in Table.addColumn: pa is null.");
    if (attributes == null)
      throw new SimpleException(String2.ERROR + " in Table.addColumn: attributes is null.");
    int size = columns.size();
    if (position > size)
      throw new IllegalArgumentException(
          String2.ERROR
              + " in Table.addColumn: position ("
              + position
              + ") is beyond size ("
              + size
              + ").");
    if (name == null) name = "Column" + position;
    columnNames.atInsert(position, name);
    columns.add(position, pa);
    columnAttributes.add(position, attributes);
    return columns.size() - 1;
  }

  /**
   * This inserts a column in the table. The column's will initially have 0 attributes.
   *
   * @param position 0..
   * @param name the name for the column If name is null, the name will be "Column<#>" (0..)
   * @param pa the data for the column. The data is not copied. This primitiveArray will continue to
   *     be used to store the data.
   * @return the column's number
   */
  public int addColumn(int position, String name, PrimitiveArray pa) {
    return addColumn(position, name, pa, new Attributes());
  }

  /**
   * This adds a column to the table and the end. The column's will initially have 0 attributes.
   *
   * @param name the name for the column If name is null, the name will be "Column<#>" (0..)
   * @param pa the data for the column. The data is not copied. This primitiveArray will continue to
   *     be used to store the data.
   * @return the column's number
   */
  public int addColumn(String name, PrimitiveArray pa) {
    return addColumn(columnNames.size(), name, pa);
  }

  /**
   * This removes a column (and any associated columnName and attributes).
   *
   * @param col (0..)
   * @throws Exception if col is invalid
   */
  public void removeColumn(int col) {
    columnNames.remove(col);
    columns.remove(col);
    columnAttributes.remove(col);
  }

  /**
   * This removes a column (and any associated columnName and attributes).
   *
   * @param columnName
   * @throws IllegalArgumentException if columnName is invalid
   */
  public void removeColumn(String columnName) {
    int col = findColumnNumber(columnName);
    if (col < 0)
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.removeColumn: columnName=" + columnName + " not found.");
    removeColumn(col);
  }

  /**
   * This removes a range of columns (and any associated columnName and attributes).
   *
   * @param from (0..)
   * @param to exclusive (0..)
   * @throws Exception if col is invalid
   */
  public void removeColumns(int from, int to) {
    for (int col = to - 1; col >= from; col--) removeColumn(col);
  }

  /** This removes all columns. */
  public void removeAllColumns() {
    removeColumns(0, nColumns());
  }

  /**
   * This moves a column.
   *
   * @param from
   * @param to
   * @throws Exception if 'from' or 'to' is not valid.
   */
  public void moveColumn(int from, int to) {
    if (from == to) return;
    addColumn(to, getColumnName(from), getColumn(from), columnAttributes(from));

    // the 'from' column may now be in a different position
    if (from > to) from++;
    removeColumn(from);
  }

  /**
   * This moves the columns into the desired order. If a column in desiredOrder isn't in the table,
   * it is ignored. If a column in the table isn't in desiredOrder, it will be pushed to the right.
   *
   * @param desiredOrder a list of column names
   * @param discardOthers if true, columns not in desiredOrder list will be removed
   * @return the number of desiredOrder columns that were found
   */
  public int reorderColumns(StringArray desiredOrder, boolean discardOthers) {
    int nFound = 0;
    int doSize = desiredOrder.size();
    for (int i = 0; i < desiredOrder.size(); i++) {
      int from = findColumnNumber(desiredOrder.get(i));
      if (from >= 0) moveColumn(from, nFound++);
    }
    if (discardOthers && nFound < nColumns()) removeColumns(nFound, nColumns());
    return nFound;
  }

  /**
   * This returns the columnName for a specific column.
   *
   * @param col (0..)
   * @return the corresponding column name (or "Column#<col>", where col is 0..).
   * @throws Exception if col not valid.
   */
  public String getColumnName(int col) {
    if (col < 0 || col >= nColumns())
      throw new IllegalArgumentException(
          String2.ERROR
              + " in Table.getColumnName: nColumns="
              + nColumns()
              + ". There is no col #"
              + col
              + ".");
    return columnNames.get(col);
  }

  /**
   * This returns a String[] with all of the column names.
   *
   * @return a String[] with all of the column names.
   */
  public String[] getColumnNames() {
    return columnNames.toArray();
  }

  /**
   * This returns the number of the column named columnName.
   *
   * @param columnName
   * @return the corresponding column number (or -1 if not found).
   */
  public int findColumnNumber(String columnName) {
    return columnNames.indexOf(columnName, 0);
  }

  /**
   * This returns the number of the column named columnName (case-insensitive).
   *
   * @param columnName
   * @return the corresponding column number (or -1 if not found).
   */
  public int findColumnNumberIgnoreCase(String columnName) {
    return columnNames.indexOfIgnoreCase(columnName);
  }

  /**
   * This returns the number of the first column with the specified attName=attValue.
   *
   * @param attName e.g., cf_role (case sensitive)
   * @param attValue e.g., timeseries_id (case insensitive)
   * @return the corresponding column number (or -1 if not found).
   */
  public int findColumnNumberWithAttributeValue(String attName, String attValue) {
    attValue = attValue.toLowerCase();
    int nCol = nColumns();
    for (int col = 0; col < nCol; col++) {
      if (attValue.equalsIgnoreCase(columnAttributes(col).getString(attName))) return col;
    }
    return -1;
  }

  /**
   * This returns the column named columnName.
   *
   * @param columnName
   * @return the corresponding column
   * @throws IllegalArgumentException if not found
   */
  public PrimitiveArray findColumn(String columnName) {
    int col = findColumnNumber(columnName);
    if (col < 0)
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.findColumn: columnName=" + columnName + " not found.");
    return getColumn(col);
  }

  /**
   * This sets the columnName for a specific column.
   *
   * @param col (0..)
   * @param newName the new name for the column
   * @throws Exception if columnNames is null, or col not valid.
   */
  public void setColumnName(int col, String newName) {
    if (col < 0 || col >= nColumns())
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.setColumnName: col " + col + " is invalid.");
    columnNames.set(col, newName);
  }

  /**
   * This returns the columnName for a specific column with internal spaces replaced with '_'s.
   *
   * @param col (0..)
   * @return the corresponding column name (or "Column_" + col, or "" if col is invalid)
   */
  public String getColumnNameWithoutSpaces(int col) {
    return String2.replaceAll(getColumnName(col), " ", "_");
  }

  /** Test of speed and memory efficiency of ucar.ma package. */
  /*public static void testMA() {
      String2.log("testMA");
      Math2.incgc(200); //in a test
      Math2.incgc(200); //in a test
      int n = 10000000;
      int j;
      double d;

      //test ArrayInt.D1
      long oMemory = Math2.getUsingMemory();
      ArrayInt.D1 arrayInt = new ArrayInt.D1(n, false); //isUnsigned
      String2.log("ArrayInt.D1 bytes/int  = " + ((Math2.getUsingMemory() - oMemory) / (double)n));
      long time = System.currentTimeMillis();
      Index1D index = new Index1D(new int[]{n});
      for (int i = 0; i < n; i++)
          d = arrayInt.getDouble(index.set(i));
      String2.log("ArrayInt.D1 read time=" + (System.currentTimeMillis() - time) + "ms"); //157

      //test int[]
      oMemory = Math2.getUsingMemory();
      int iar[] = new int[n];
      String2.log("int[] bytes/int  = " + ((Math2.getUsingMemory() - oMemory) / (double)n));
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++)
          d = iar[i];
      String2.log("int[] read time=" + (System.currentTimeMillis() - time) + "ms"); //32

      //test int[] as object
      Object o = iar;
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++)
          d = getDouble(o, i);
      String2.log("(int[])o read time=" + (System.currentTimeMillis() - time) + "ms"); //172

      //test Integer[]
      oMemory = Math2.getUsingMemory();
      Number nar[] = new Integer[n];
      String2.log("Integer[] bytes/int  = " + ((Math2.getUsingMemory() - oMemory) / (double)n));
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++)
          nar[i] = Integer.valueOf(i);
      String2.log("Integer[] create time=" + (System.currentTimeMillis() - time) + "ms"); //2271!
      time = System.currentTimeMillis();
      for (int i = 0; i < n; i++)
          d = nar[i].doubleValue();
      String2.log("Integer[] read time=" + (System.currentTimeMillis() - time) + "ms"); //110

      Math2.sleep(30000);

  } */

  /**
   * This returns the current number of rows.
   *
   * @return the current number of rows
   */
  public int nRows() {
    if (nColumns() == 0) return 0;
    return getColumn(0).size();
  }

  /**
   * This inserts a blank row.
   *
   * @param index 0..size
   * @throws Exception if trouble
   */
  public void insertBlankRow(int index) throws Exception {
    if (index < 0 || index > nRows())
      throw new Exception("index=" + index + " must be between 0 and " + nRows() + ".");
    int nCols = nColumns();
    for (int col = 0; col < nCols; col++) columns.get(col).atInsertString(index, "");
  }

  /**
   * Moves rows 'first' through 'last' (inclusive) to 'destination', shifting intermediate values to
   * fill the gap.
   *
   * @param first the first to be move
   * @param last (exclusive)
   * @param destination the destination, can't be in the range 'first+1..last-1'.
   */
  public void moveRows(int first, int last, int destination) {
    int nCols = nColumns();
    for (int col = 0; col < nCols; col++) columns.get(col).move(first, last, destination);
  }

  /**
   * This removes 1 row.
   *
   * @param row 0 ... size-1.
   * @throws Exception if trouble
   */
  public void removeRow(int row) {
    removeRows(row, row + 1);
  }

  /**
   * This removes a range of rows. This does the best it can to not throw an exception (e.g., to
   * help clean up a damaged table with columns with different numbers of rows).
   *
   * @param from the first element to be removed, 0 ... size
   * @param to one after the last element to be removed, from ... size (or use Integer.MAX_VALUE to
   *     remove to the end).
   * @throws Exception if trouble
   */
  public void removeRows(int from, int to) {
    int nCols = nColumns();
    for (int col = 0; col < nCols; col++) {
      PrimitiveArray pa = columns.get(col);
      int nRows = pa.size();
      if (from < nRows) pa.removeRange(from, Math.min(nRows, to));
    }
  }

  /** This removes all rows of data, but leaves the columns intact. */
  public void removeAllRows() {
    int nCols = nColumns();
    for (int col = 0; col < nCols; col++) columns.get(col).clear();
  }

  /**
   * This finds all rows of data that have data (not all ("missing_value", "_FillValue", or the
   * PrimitiveArray's native MV).
   *
   * <p>CF 1.6 Discrete Sampling Geometry and Incomplete Multidimensional Arrays: the spec doesn't
   * say which attribute is to be used: missing_value or _FillValue. It just says "missing value"
   * (in the generic sense) repeatedly.
   *
   * @return a BitSet with bit=true for each row that has data (not all missing-values).
   */
  public BitSet rowsWithData() {
    int tnRows = nRows();
    int tnCols = nColumns();
    BitSet keep = new BitSet(tnRows); // all false
    int keepN = 0;
    for (int col = 0; col < tnCols; col++) {
      // this is very similar to lastRowWithData
      PrimitiveArray pa = columns.get(col);
      PAType paType = pa.elementType();
      Attributes atts = columnAttributes(col);
      if (paType == PAType.STRING) {
        String mv = atts.getString("missing_value"); // may be null
        String fv = atts.getString("_FillValue");
        if (mv == null) mv = "";
        if (fv == null) fv = "";
        for (int row = keep.nextClearBit(0); row < tnRows; row = keep.nextClearBit(row + 1)) {
          String t = pa.getString(row);
          if (t != null && t.length() > 0 && !mv.equals(t) && !fv.equals(t)) {
            keepN++;
            keep.set(row);
          }
        }
      } else if (paType == PAType.DOUBLE) {
        double mv = atts.getDouble("missing_value"); // may be NaN
        double fv = atts.getDouble("_FillValue");
        for (int row = keep.nextClearBit(0); row < tnRows; row = keep.nextClearBit(row + 1)) {
          double t = pa.getDouble(row);
          if (Double.isFinite(t)
              && // think carefully
              !Math2.almostEqual(9, t, mv)
              && // if mv=NaN, !M.ae will be true
              !Math2.almostEqual(9, t, fv)) {
            keepN++;
            keep.set(row);
          }
        }
      } else if (paType == PAType.FLOAT) {
        float mv = atts.getFloat("missing_value"); // may be NaN
        float fv = atts.getFloat("_FillValue");
        for (int row = keep.nextClearBit(0); row < tnRows; row = keep.nextClearBit(row + 1)) {
          float t = pa.getFloat(row);
          if (Double.isFinite(t)
              && // think carefully
              !Math2.almostEqual(5, t, mv)
              && // if mv=NaN, !M.ae will be true
              !Math2.almostEqual(5, t, fv)) {
            keepN++;
            keep.set(row);
          }
        }
      } else if (paType == PAType.ULONG) {
        BigInteger mv = atts.getULong("missing_value"); // may be null
        BigInteger fv = atts.getULong("_FillValue");
        for (int row = keep.nextClearBit(0); row < tnRows; row = keep.nextClearBit(row + 1)) {
          BigInteger t = pa.getULong(row);
          if (t == null || t.equals(mv) || t.equals(fv)) {
          } else {
            keepN++;
            keep.set(row);
          }
        }
      } else {
        long mv = atts.getLong("missing_value"); // may be Long.MAX_VALUE
        long fv = atts.getLong("_FillValue");
        for (int row = keep.nextClearBit(0); row < tnRows; row = keep.nextClearBit(row + 1)) {
          float t = pa.getLong(row);
          if (t < Long.MAX_VALUE
              && t != mv
              && t != fv) { // trouble: for LongArray, this assumes maxIsMV
            keepN++;
            keep.set(row);
          }
        }
      }
      // if (debugMode)
      //    String2.log("  rowsWithData after col#" + col + " keepN=" + keepN);
      if (keepN == tnRows) return keep;
    }
    return keep;
  }

  /**
   * This removes all rows of data that have just missing_values ("missing_value", "_FillValue", or
   * the PrimitiveArray's native MV).
   *
   * <p>CF 1.6 Discrete Sampling Geometry and Incomplete Multidimensional Arrays: the spec doesn't
   * say which attribute is to be used: missing_value or _FillValue. It just says "missing value"
   * (in the generic sense) repeatedly.
   *
   * @return the number of rows remaining
   */
  public int removeRowsWithoutData() {
    justKeep(rowsWithData());
    return nRows();
  }

  /**
   * This finds the last row with data in some column (not missing_value or _FillValue or the
   * PrimitiveArray's native MV).
   *
   * <p>CF 1.6 Discrete Sampling Geometry and Incomplete Multidimensional Arrays: the spec doesn't
   * say which attribute is to be used: missing_value or _FillValue. It just says "missing value"
   * (in the generic sense) repeatedly.
   *
   * @return the last row with data (perhaps -1)
   */
  public int lastRowWithData() {
    int tnRows = nRows();
    int tnCols = nColumns();
    int lastRowWithData = -1;
    for (int col = 0; col < tnCols; col++) {
      // this is very similar to rowsWithData
      PrimitiveArray pa = columns.get(col);
      PAType paType = pa.elementType();
      Attributes atts = columnAttributes(col);
      if (paType == PAType.STRING) {
        String mv = atts.getString("missing_value"); // may be null
        String fv = atts.getString("_FillValue");
        if (mv == null) mv = "";
        if (fv == null) fv = "";
        for (int row = tnRows - 1; row > lastRowWithData; row--) {
          String t = pa.getString(row);
          if (t != null && t.length() > 0 && !mv.equals(t) && !fv.equals(t)) {
            lastRowWithData = row;
            break;
          }
        }
      } else if (paType == PAType.DOUBLE) {
        double mv = atts.getDouble("missing_value"); // may be NaN
        double fv = atts.getDouble("_FillValue");
        for (int row = tnRows - 1; row > lastRowWithData; row--) {
          double t = pa.getDouble(row);
          if (Double.isFinite(t)
              && // think carefully
              !Math2.almostEqual(9, t, mv)
              && // if mv=NaN, !M.ae will be true
              !Math2.almostEqual(9, t, fv)) {
            lastRowWithData = row;
            break;
          }
        }
      } else if (paType == PAType.FLOAT) {
        float mv = atts.getFloat("missing_value"); // may be NaN
        float fv = atts.getFloat("_FillValue");
        for (int row = tnRows - 1; row > lastRowWithData; row--) {
          float t = pa.getFloat(row);
          if (Float.isFinite(t)
              && // think carefully
              !Math2.almostEqual(5, t, mv)
              && // if mv=NaN, !M.ae will be true
              !Math2.almostEqual(5, t, fv)) {
            lastRowWithData = row;
            break;
          }
        }
      } else if (paType == PAType.ULONG) {
        BigInteger mv = atts.getULong("missing_value"); // may be null
        BigInteger fv = atts.getULong("_FillValue");
        boolean paMaxIsMV = pa.getMaxIsMV();
        for (int row = tnRows - 1; row > lastRowWithData; row--) {
          BigInteger t = pa.getULong(row);
          if (t == null || t.equals(mv) || t.equals(fv)) {
          } else {
            lastRowWithData = row;
            break;
          }
        }
      } else {
        long mv = atts.getLong("missing_value"); // may be Long.MAX_VALUE
        long fv = atts.getLong("_FillValue");
        for (int row = tnRows - 1; row > lastRowWithData; row--) {
          long t = pa.getLong(row);
          if (t != Long.MAX_VALUE
              && t != mv
              && t != fv) { // trouble: for LongArray, this works as if maxIsMV=true
            lastRowWithData = row;
            break;
          }
        }
      }
      // if (debugMode)
      //    String2.log("  lastRowWithData=" + lastRowWithData + " after col#" + col);
      if (lastRowWithData == tnRows - 1) return lastRowWithData;
    }
    return lastRowWithData;
  }

  /**
   * This removes all rows of data at the end of the table that have just missing_value or
   * _FillValue or the PrimitiveArray's native MV.
   *
   * <p>CF 1.6 Discrete Sampling Geometry and Incomplete Multidimensional Arrays: the spec doesn't
   * say which attribute is to be used: missing_value or _FillValue.
   *
   * @return the number of rows remaining
   */
  public int removeRowsAtEndWithoutData() {
    int nRemain = lastRowWithData() + 1;
    removeRows(nRemain, nRows());
    return nRemain;
  }

  /**
   * This returns the value of one datum as a String.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as a String.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public String getStringData(int col, int row) {
    return getColumn(col).getString(row);
  }

  /**
   * This returns the value of one datum as a PAOne.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as a PAOne.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public PAOne getPAOneData(int col, int row) {
    return getColumn(col).getPAOne(row);
  }

  /**
   * This returns the value of one datum as a float.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as a float.
   * @throws Exception if trouble (e.g., row or col out of range) Strings return the parsed value of
   *     the string (Double.NaN if not a number).
   */
  public float getFloatData(int col, int row) {
    return getColumn(col).getFloat(row);
  }

  /**
   * This returns the value of one datum as a double.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as a double.
   * @throws Exception if trouble (e.g., row or col out of range) Strings return the parsed value of
   *     the string (Double.NaN if not a number).
   */
  public double getDoubleData(int col, int row) {
    return getColumn(col).getDouble(row);
  }

  /**
   * This returns the value of one datum as a double.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as a nice double (rounded to 7 significant digits, so not
   *     bruised).
   * @throws Exception if trouble (e.g., row or col out of range) Strings return the parsed value of
   *     the string (Double.NaN if not a number).
   */
  public double getNiceDoubleData(int col, int row) {
    return getColumn(col).getNiceDouble(row);
  }

  /**
   * This returns the value of one datum as a long.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as a long.
   * @throws Exception if trouble (e.g., row or col out of range) Strings return the parsed value of
   *     the string (Long.MAX_VALUE if not a number).
   */
  public long getLongData(int col, int row) {
    return getColumn(col).getLong(row);
  }

  /**
   * This returns the value of one datum as an int.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @return the value of one datum as an int.
   * @throws Exception if trouble (e.g., row or col out of range) Strings return the parsed value of
   *     the string (Integer.MAX_VALUE if not a number).
   */
  public int getIntData(int col, int row) {
    return getColumn(col).getInt(row);
  }

  /**
   * This sets the value of one datum as a String.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @param s the value of one datum as a String.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void setStringData(int col, int row, String s) {
    getColumn(col).setString(row, s);
  }

  /**
   * This sets the value of one datum as a float.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @param d the value of one datum as a float.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void setFloatData(int col, int row, float d) {
    getColumn(col).setFloat(row, d);
  }

  /**
   * This sets the value of one datum as a double.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @param d the value of one datum as a double.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void setDoubleData(int col, int row, double d) {
    getColumn(col).setDouble(row, d);
  }

  /**
   * This sets the value of one datum as an int.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param row the row number (0 ... nRows-1 )
   * @param i the value of one datum as an int.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void setIntData(int col, int row, int i) {
    getColumn(col).setInt(row, i);
  }

  /**
   * This add the value of one datum as a String to one of the columns, thereby increasing the
   * number of rows in that column.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param s the value of one datum as a String.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void addStringData(int col, String s) {
    getColumn(col).addString(s);
  }

  /**
   * This add the value of one datum as a float to one of the columns, thereby increasing the number
   * of rows in that column.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param d the value of one datum as a float.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void addFloatData(int col, float d) {
    getColumn(col).addFloat(d);
  }

  /**
   * This add the value of one datum as a double to one of the columns, thereby increasing the
   * number of rows in that column.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param d the value of one datum as a double.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void addDoubleData(int col, double d) {
    getColumn(col).addDouble(d);
  }

  /**
   * This add the value of one datum as an int to one of the columns, thereby increasing the number
   * of rows in that column.
   *
   * @param col the column number (0 ... nColumns-1 )
   * @param d the value of one datum as an int.
   * @throws Exception if trouble (e.g., row or col out of range)
   */
  public void addIntData(int col, int d) {
    getColumn(col).addInt(d);
  }

  /**
   * This tests that the value in a column is as expected.
   *
   * @throws Exception if trouble
   */
  public void test1(String columnName, int row, String expected) {
    String observed = findColumn(columnName).getString(row);
    // ensureEqual deals with nulls
    Test.ensureEqual(observed, expected, "colName=" + columnName + " row=" + row);
  }

  /**
   * This tests that the value in a column is as expected.
   *
   * @throws Exception if trouble
   */
  public void test1(String columnName, int row, int expected) {
    int observed = findColumn(columnName).getInt(row);
    if (observed != expected) throw new RuntimeException("colName=" + columnName + " row=" + row);
  }

  /**
   * This tests that the value in a column is as expected.
   *
   * @throws Exception if trouble
   */
  public void test1(String columnName, int row, float expected) {
    float observed = findColumn(columnName).getFloat(row);
    // ensureEqual does fuzzy test
    Test.ensureEqual(observed, expected, "colName=" + columnName + " row=" + row);
  }

  /**
   * This tests that the value in a column is as expected.
   *
   * @throws Exception if trouble
   */
  public void test1(String columnName, int row, double expected) {
    double observed = findColumn(columnName).getDouble(row);
    // ensureEqual does fuzzy test
    Test.ensureEqual(observed, expected, "colName=" + columnName + " row=" + row);
  }

  /**
   * This tests that the value in a column is as expected. The table's column should have epoch
   * seconds values.
   *
   * @param expected value formatted with Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "")
   * @throws Exception if trouble
   */
  public void test1Time(String columnName, int row, String expected) {
    double seconds = findColumn(columnName).getDouble(row);
    String observed = Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "");
    if (!observed.equals(expected))
      throw new RuntimeException("colName=" + columnName + " row=" + row);
  }

  /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
  public String check1(String columnName, int row, String expected) {
    String observed = findColumn(columnName).getString(row);
    // Test.equal deals with nulls
    return (Test.equal(observed, expected) ? "PASS" : "FAIL")
        + ": col="
        + String2.left(columnName, 15)
        + " row="
        + String2.left("" + row, 2)
        + " observed="
        + observed
        + " expected="
        + expected;
  }

  /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
  public String check1(String columnName, int row, int expected) {
    int observed = findColumn(columnName).getInt(row);
    return (Test.equal(observed, expected) ? "PASS" : "FAIL")
        + ": col="
        + String2.left(columnName, 15)
        + " row="
        + String2.left("" + row, 2)
        + " observed="
        + observed
        + " expected="
        + expected;
  }

  /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
  public String check1(String columnName, int row, float expected) {
    float observed = findColumn(columnName).getFloat(row);
    return (Test.equal(observed, expected) ? "PASS" : "FAIL")
        + ": col="
        + String2.left(columnName, 15)
        + " row="
        + String2.left("" + row, 2)
        + " observed="
        + observed
        + " expected="
        + expected;
  }

  /** This checks that the value in a column is as expected and prints PASS/FAIL and the test. */
  public String check1(String columnName, int row, double expected) {
    double observed = findColumn(columnName).getDouble(row);
    return (Test.equal(observed, expected) ? "PASS" : "FAIL")
        + ": col="
        + String2.left(columnName, 15)
        + " row="
        + String2.left("" + row, 2)
        + " observed="
        + observed
        + " expected="
        + expected;
  }

  /**
   * This checks that the value in a column is as expected and prints PASS/FAIL and the test. The
   * table's column should have epoch seconds values.
   *
   * @param expected value formatted with Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "")
   */
  public String check1Time(String columnName, int row, String expected) {
    double seconds = findColumn(columnName).getDouble(row);
    String observed = Calendar2.safeEpochSecondsToIsoStringTZ(seconds, "");
    return (Test.equal(observed, expected) ? "PASS" : "FAIL")
        + ": col="
        + String2.left(columnName, 15)
        + " row="
        + String2.left("" + row, 2)
        + " observed="
        + observed
        + " expected="
        + expected;
  }

  /**
   * This prints a header in a format that pretty closely mimics the C version of ncdump (starting
   * with the "{") and acts as if the file will be stored as an .nc file.
   *
   * @param dimensionName the name for the rows (e.g., "time", "row", "station", "observation")
   * @return a string representation of this grid
   */
  public String getNCHeader(String dimensionName) {
    // dimensions
    StringBuilder sb =
        new StringBuilder(
            // this pretty closely mimics the C version of ncdump
            // (number formats are a little different)
            // and acts as if the file will be stored as an .nc file
            "{\n" + "dimensions:\n" + "\t" + dimensionName + " = " + nRows() + " ;\n");
    int nColumns = nColumns();
    for (int col = 0; col < nColumns; col++) {
      PrimitiveArray pa = columns.get(col);
      if (pa instanceof StringArray sa) {
        // String2.log(">>getNcHeader sa=" + sa.toNccsv255AttString());
        sb.append(
            "\t"
                + getColumnName(col)
                + NcHelper.StringLengthSuffix
                + " = "
                + sa.maxStringLength()
                + " ;\n");
      }
    }

    // variables
    sb.append("variables:\n");
    for (int col = 0; col < nColumns; col++) {
      PrimitiveArray pa = columns.get(col);
      String columnName = getColumnName(col);
      if (pa instanceof StringArray sa) {
        sb.append(
            "\tchar "
                + columnName
                + "("
                + dimensionName
                + ", "
                + columnName
                + NcHelper.StringLengthSuffix
                + ") ;\n");
      } else {
        sb.append("\t" + pa.elementTypeString() + " " + columnName + "(" + dimensionName + ") ;\n");
      }
      sb.append(columnAttributes(col).toNcString("\t\t" + columnName + ":", " ;"));
    }
    sb.append("\n// global attributes:\n");
    sb.append(globalAttributes.toNcString("\t\t:", " ;"));
    sb.append("}\n");

    return sb.toString();
  }

  /** This prints the metadata and the data to a CSV table. This shows row numbers. */
  @Override
  public String toString() {
    return toString(Integer.MAX_VALUE);
  }

  /**
   * This returns a string CSV representation of this data.
   *
   * @param showFirstNRows use Integer.MAX_VALUE for all rows.
   * @return a string representation of this point data
   */
  public String toString(int showFirstNRows) {
    ensureValid(); // throws Exception if not
    return getNCHeader("row") + dataToString(showFirstNRows);
  }

  /** This is convenience for dataToString(Integer.MAX_VALUE). */
  public String dataToString() {
    return dataToString(Integer.MAX_VALUE);
  }

  /**
   * This is convenience for dataToString(int showFirstNRows, showRowNumber=true). This shows row
   * numbers.
   *
   * @param showFirstNRows use Integer.MAX_VALUE for all rows. If not all rows are shown, this adds
   *     a "..." line to the output.
   */
  public String dataToString(int showFirstNRows) {
    return dataToString(0, showFirstNRows);
  }

  /**
   * This prints a range of rows to a CSV table.
   *
   * @param start the first row to be included
   * @param stop one past the last row to be included. If not all rows are shown, this adds a "..."
   *     line to the output.
   */
  public String dataToString(int start, int stop) {
    ensureValid();
    start = Math.max(start, 0);
    stop = Math.min(stop, nRows());
    StringBuilder sb = new StringBuilder();
    int nCols = nColumns();
    sb.append(getColumnNamesCSVString() + "\n");
    for (int row = start; row < stop; row++) {
      for (int col = 0; col < nCols; col++) {
        sb.append(columns.get(col).getNccsv127DataString(row));
        if (col == nCols - 1) sb.append('\n');
        else sb.append(",");
      }
    }
    if (stop < nRows()) sb.append("...\n");
    return sb.toString();
  }

  /**
   * This returns a Comma Separated Value (CSV) string with the names of the columns.
   *
   * @return a csv string with the column names.
   */
  public String getColumnNamesCSVString() {
    return columnNames.toCSVString();
  }

  /**
   * This returns a Comma Space Separated Value (CSSV) string with the names of the columns.
   *
   * @return a csv string with the column names.
   */
  public String getColumnNamesCSSVString() {
    return columnNames.toString();
  }

  /**
   * This returns the Attributes with the global attributes.
   *
   * @return the Attributes with global attributes.
   */
  public Attributes globalAttributes() {
    return globalAttributes;
  }

  /**
   * This gets the Attributes for a given column. Use this with care; this is the actual data
   * structure, not a clone.
   *
   * @param column
   * @return the ArrayList with the attributes for the column
   */
  public Attributes columnAttributes(int column) {
    return columnAttributes.get(column);
  }

  /**
   * This gets the Attributes for a given column. Use this with care; this is the actual data
   * structure, not a clone.
   *
   * @param columnName
   * @return the ArrayList with the attributes for the column
   * @throws IllegalArgumentException if columnName not found
   */
  public Attributes columnAttributes(String columnName) {
    int col = findColumnNumber(columnName);
    if (col < 0)
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.getColumn: columnName=" + columnName + " not found.");
    return columnAttributes(col);
  }

  /**
   * This adds the standard attributes, including calling setActualRangeAndBoundingBox (which sets
   * the coordinate variables' axis, long_name, standard_name, and units). If a string parameter is
   * null, nothing is done; the attribute is not set and not cleared. If a string parameter is "",
   * that attributes is cleared.
   *
   * <p>This does not call convertToFakeMissingValues(), nor should that (generally speaking) be
   * called before or after calling this. This procedure expects NaN's as the missing values.
   * convertToFakeMissingValues and convertToStandardMissingValues are called internally by the
   * saveAsXxx methods to temporarily switch to fakeMissingValue.
   *
   * <p>This does not set the data columns' attributes, notably: "long_name" (boldTitle?),
   * "standard_name", "units" (UDUnits).
   *
   * <p>NOW OBSOLETE [This sets most of the metadata needed to comply with Unidata Observation
   * Dataset Conventions
   * (https://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html) [GONE!]
   * To fully comply, you may need to add the global attribute observationDimension (see
   * saveAsFlatNc). ]
   *
   * @param lonIndex identifies the longitude column (or -1 if none)
   * @param latIndex identifies the latitude column (or -1 if none)
   * @param depthIndex identifies the depth column (or -1 if none)
   * @param timeIndex identifies the time column (or -1 if none)
   * @param boldTitle a descriptive title for this data
   * @param cdmDataType "Grid", "Image", "Station", "Point", "Trajectory", or "Radial"
   * @param creatorEmail usually already set, e.g., DataHelper.CS_CREATOR_EMAIL, "erd.data@noaa.gov"
   * @param creatorName usually already set, e.g., DataHelper.CS_CREATOR_NAME, "NOAA CoastWatch,
   *     West Coast Node"
   * @param creatorUrl usually already set, e.g., DataHelper.CS_CREATOR_URL,
   *     "https://coastwatch.pfeg.noaa.gov"
   * @param project usually already set, e.g., DataHelper.CS_PROJECT
   * @param id a unique string identifying this table
   * @param keywordsVocabulary e.g., "GCMD Science Keywords"
   * @param keywords e.g., a keyword string from
   *     http://gcmd.gsfc.nasa.gov/Resources/valids/gcmd_parameters.html e.g., "Oceans > Ocean
   *     Temperature > Sea Surface Temperature"
   * @param references
   * @param summary a longer description of this data
   * @param courtesy e.g., "Channel Islands National Park (David Kushner)"
   * @param timeLongName "Time" (the default if this is null), or a better description of time,
   *     e.g., "Centered Time", "Centered Time of 1 Day Averages"
   */
  public void setAttributes(
      int lonIndex,
      int latIndex,
      int depthIndex,
      int timeIndex,
      String boldTitle,
      String cdmDataType,
      String creatorEmail,
      String creatorName,
      String creatorUrl,
      String project,
      String id,
      String keywordsVocabulary,
      String keywords,
      String references,
      String summary,
      String courtesy,
      String timeLongName) {

    String currentDateTimeZ = Calendar2.getCurrentISODateTimeStringZulu() + "Z";
    trySet(globalAttributes, "acknowledgement", "NOAA NESDIS COASTWATCH, NOAA SWFSC ERD");
    trySet(globalAttributes, "cdm_data_type", cdmDataType);
    trySet(globalAttributes, "contributor_name", courtesy);
    trySet(globalAttributes, "contributor_role", "Source of data.");
    trySet(globalAttributes, "Conventions", "COARDS, CF-1.6, ACDD-1.3"); // unidata-related
    trySet(globalAttributes, "creator_email", creatorEmail);
    trySet(globalAttributes, "creator_name", creatorName);
    trySet(globalAttributes, "creator_url", creatorUrl);
    trySet(globalAttributes, "date_created", currentDateTimeZ);
    trySet(globalAttributes, "date_issued", currentDateTimeZ);
    String oldHistory = globalAttributes.getString("history");
    if (oldHistory == null) oldHistory = courtesy;
    trySet(globalAttributes, "history", DataHelper.addBrowserToHistory(oldHistory));
    // String2.log("Table.setAttributes new history=" + globalAttributes.getString("history") +
    //    "\nstack=" + MustBe.stackTrace());
    trySet(globalAttributes, "id", id);
    trySet(globalAttributes, "institution", creatorName);
    if (keywords != null && keywords.length() > 0) {
      trySet(globalAttributes, "keywords_vocabulary", keywordsVocabulary);
      trySet(globalAttributes, "keywords", keywords);
    }
    trySet(
        globalAttributes,
        "license",
        "The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.");
    trySet(globalAttributes, "naming_authority", "gov.noaa.pfel.coastwatch"); // for generating id
    // trySet(globalAttributes, "processing_level", "3"); //appropriate for satellite data
    trySet(globalAttributes, "project", project);
    trySet(globalAttributes, "references", references);
    trySet(
        globalAttributes, "standard_name_vocabulary", FileNameUtility.getStandardNameVocabulary());
    trySet(globalAttributes, "summary", summary);
    trySet(globalAttributes, "title", boldTitle); // Time series from ... ?

    // remove some commonly set, but no longer relevant, attributes
    globalAttributes.remove(CacheOpendapStation.OPENDAP_TIME_DIMENSION_SIZE);
    globalAttributes.remove("Unlimited_Dimension"); // e.g., MBARI has this

    // setActualRangeAndBoundingBox
    setActualRangeAndBoundingBox(lonIndex, latIndex, depthIndex, -1, timeIndex, timeLongName);
  }

  /**
   * This is called by setAttributes to calculate and set each column's "actual_range" attributes
   * and set the THREDDS ACDD-style geospatial_lat_min and max, ... and time_coverage_start and end
   * global attributes, and the Google Earth-style Southernmost_Northing, ... Easternmost_Easting.
   * This also sets column attributes for the lon, lat, depth, and time variables (if the index
   * isn't -1).
   *
   * <p>OBSOLETE [For Unidata Observation Dataset Conventions (e.g., _Coordinate), see
   * https://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html .] [GONE!]
   *
   * @param lonIndex identifies the longitude column (or -1 if none)
   * @param latIndex identifies the latitude column (or -1 if none)
   * @param depthIndex identifies the depth column (or -1 if none)
   * @param altIndex identifies the altitude column (or -1 if none). There shouldn't be both a depth
   *     and an altitude column.
   * @param timeIndex identifies the time column (or -1 if none)
   * @param timeLongName "Time" (the default, if this is null), or a better description of time,
   *     e.g., "Centered Time", "Centered Time of 1 Day Averages"
   */
  public void setActualRangeAndBoundingBox(
      int lonIndex,
      int latIndex,
      int depthIndex,
      int altIndex,
      int timeIndex,
      String timeLongName) {
    // set actual_range
    for (int col = 0; col < nColumns(); col++) {
      setActualRange(col);

      // set acdd-style and google-style bounding box
      PrimitiveArray range = columnAttributes(col).get("actual_range");
      if (col == lonIndex) {
        columnAttributes(col).set("_CoordinateAxisType", "Lon"); // unidata-related
        columnAttributes(col).set("axis", "X");
        columnAttributes(col).set("long_name", "Longitude");
        columnAttributes(col).set("standard_name", "longitude");
        columnAttributes(col).set("units", "degrees_east");

        globalAttributes.set("geospatial_lon_units", "degrees_east");
        if (range == null) {
          globalAttributes.remove("geospatial_lon_min");
          globalAttributes.remove("geospatial_lon_max");
          globalAttributes.remove("Westernmost_Easting");
          globalAttributes.remove("Easternmost_Easting");
        } else if (range instanceof FloatArray) {
          globalAttributes.set("geospatial_lon_min", range.getFloat(0));
          globalAttributes.set("geospatial_lon_max", range.getFloat(1));
          globalAttributes.set("Westernmost_Easting", range.getFloat(0));
          globalAttributes.set("Easternmost_Easting", range.getFloat(1));
        } else {
          globalAttributes.set("geospatial_lon_min", range.getDouble(0));
          globalAttributes.set("geospatial_lon_max", range.getDouble(1));
          globalAttributes.set("Westernmost_Easting", range.getDouble(0));
          globalAttributes.set("Easternmost_Easting", range.getDouble(1));
        }
      } else if (col == latIndex) {
        columnAttributes(col).set("_CoordinateAxisType", "Lat"); // unidata-related
        columnAttributes(col).set("axis", "Y");
        columnAttributes(col).set("long_name", "Latitude");
        columnAttributes(col).set("standard_name", "latitude");
        columnAttributes(col).set("units", "degrees_north");

        globalAttributes.set("geospatial_lat_units", "degrees_north");
        if (range == null) {
          globalAttributes.remove("geospatial_lat_min");
          globalAttributes.remove("geospatial_lat_max");
          globalAttributes.remove("Southernmost_Northing");
          globalAttributes.remove("Northernmost_Northing");
        } else if (range instanceof FloatArray) {
          globalAttributes.set("geospatial_lat_min", range.getFloat(0)); // unidata-related
          globalAttributes.set("geospatial_lat_max", range.getFloat(1));
          globalAttributes.set("Southernmost_Northing", range.getFloat(0));
          globalAttributes.set("Northernmost_Northing", range.getFloat(1));
        } else {
          globalAttributes.set("geospatial_lat_min", range.getDouble(0)); // unidata-related
          globalAttributes.set("geospatial_lat_max", range.getDouble(1));
          globalAttributes.set("Southernmost_Northing", range.getDouble(0));
          globalAttributes.set("Northernmost_Northing", range.getDouble(1));
        }
      } else if (col == depthIndex) {
        columnAttributes(col).set("_CoordinateAxisType", "Height"); // unidata
        columnAttributes(col).set("_CoordinateZisPositive", "down"); // unidata
        columnAttributes(col).set("axis", "Z");
        columnAttributes(col).set("long_name", "Depth"); // this is a commitment to Depth
        columnAttributes(col)
            .set("positive", "down"); // this is a commitment to Depth, //unidata-related
        columnAttributes(col).set("standard_name", "depth"); // this is a commitment to Depth
        columnAttributes(col).set("units", "m"); // CF standard names says canonical units are "m"

        if (range == null) {
          globalAttributes.remove("geospatial_vertical_min"); // unidata-related
          globalAttributes.remove("geospatial_vertical_max");
        } else if (range instanceof DoubleArray) {
          globalAttributes.set("geospatial_vertical_min", range.getDouble(0));
          globalAttributes.set("geospatial_vertical_max", range.getDouble(1));
        } else if (range instanceof FloatArray) {
          globalAttributes.set("geospatial_vertical_min", range.getFloat(0));
          globalAttributes.set("geospatial_vertical_max", range.getFloat(1));
        } else {
          globalAttributes.set("geospatial_vertical_min", range.getInt(0));
          globalAttributes.set("geospatial_vertical_max", range.getInt(1));
        }
        globalAttributes.set("geospatial_vertical_units", "m");
        globalAttributes.set(
            "geospatial_vertical_positive", "down"); // this is a commitment to Depth
      } else if (col == altIndex) {
        columnAttributes(col).set("_CoordinateAxisType", "Height"); // unidata
        columnAttributes(col).set("_CoordinateZisPositive", "up"); // unidata
        columnAttributes(col).set("axis", "Z");
        columnAttributes(col).set("long_name", "Altitude"); // this is a commitment to Altitude
        columnAttributes(col)
            .set("positive", "up"); // this is a commitment to Altitude, //unidata-related
        columnAttributes(col).set("standard_name", "altitude"); // this is a commitment to Altitude
        columnAttributes(col).set("units", "m"); // CF standard names says canonical units are "m"

        if (range == null) {
          globalAttributes.remove("geospatial_vertical_min"); // unidata-related
          globalAttributes.remove("geospatial_vertical_max");
        } else if (range instanceof DoubleArray) {
          globalAttributes.set("geospatial_vertical_min", range.getDouble(0));
          globalAttributes.set("geospatial_vertical_max", range.getDouble(1));
        } else if (range instanceof FloatArray) {
          globalAttributes.set("geospatial_vertical_min", range.getFloat(0));
          globalAttributes.set("geospatial_vertical_max", range.getFloat(1));
        } else {
          globalAttributes.set("geospatial_vertical_min", range.getInt(0));
          globalAttributes.set("geospatial_vertical_max", range.getInt(1));
        }
        globalAttributes.set("geospatial_vertical_units", "m");
        globalAttributes.set(
            "geospatial_vertical_positive", "up"); // this is a commitment to Altitude
      } else if (col == timeIndex) {
        columnAttributes(col).set("_CoordinateAxisType", "Time"); // unidata-related
        columnAttributes(col).set("axis", "T");
        if (timeLongName == null || timeLongName.length() == 0) timeLongName = "Time";
        columnAttributes(col).set("long_name", timeLongName);
        columnAttributes(col).set("standard_name", "time");
        // LAS Intermediate files wants time_origin "01-Jan-1970 00:00:00" ;
        // https://ferret.pmel.noaa.gov/LASdoc/serve/cache/90.html
        columnAttributes(col).set("time_origin", "01-JAN-1970 00:00:00");
        columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);

        // this range is a little misleading for averaged data
        if (range == null) {
          globalAttributes.remove("time_coverage_start"); // unidata-related
          globalAttributes.remove("time_coverage_end");
        } else {
          globalAttributes.set(
              "time_coverage_start", Calendar2.epochSecondsToIsoStringTZ(range.getDouble(0)));
          globalAttributes.set(
              "time_coverage_end", Calendar2.epochSecondsToIsoStringTZ(range.getDouble(1)));
        }
        // this doesn't set tableGlobalAttributes.set("time_coverage_resolution", "P1H");
      }
    }
  }

  /**
   * Call this to remove file-specific attributes (which are wrong if applied to an aggregated
   * dataset): each column's "actual_range" attributes and the THREDDS ACDD-style geospatial_lat_min
   * and max, ... and time_coverage_start and end global attributes, and the Google Earth-style
   * Southernmost_Northing, ... Easternmost_Easting.
   *
   * <p>OBSOLETE [For Unidata Observation Dataset Conventions (e.g., _Coordinate), see
   * https://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html .] [GONE!]
   */
  public void unsetActualRangeAndBoundingBox() {

    // remove acdd-style and google-style bounding box
    globalAttributes.remove("geospatial_lon_min");
    globalAttributes.remove("geospatial_lon_max");
    globalAttributes.remove("Westernmost_Easting");
    globalAttributes.remove("Easternmost_Easting");
    globalAttributes.remove("geospatial_lat_min");
    globalAttributes.remove("geospatial_lat_max");
    globalAttributes.remove("Southernmost_Northing");
    globalAttributes.remove("Northernmost_Northing");
    globalAttributes.remove("geospatial_vertical_min"); // unidata-related
    globalAttributes.remove("geospatial_vertical_max");
    globalAttributes.remove("time_coverage_start"); // unidata-related
    globalAttributes.remove("time_coverage_end");

    // remove actual_range
    for (int col = 0; col < nColumns(); col++) {
      Attributes atts = columnAttributes(col);
      atts.remove("actual_min");
      atts.remove("actual_max");
      atts.remove("actual_range");
      atts.remove("data_min");
      atts.remove("data_max");
    }
  }

  /**
   * This is called by setActualRangeAndBoundingBox to set (or revise) the actual_range metadata.
   * This works on the current (unpacked; not scaled) data. So call this when the data is unpacked.
   * If the column is a String column, nothing will be done.
   *
   * @param column
   */
  public void setActualRange(int column) {
    PrimitiveArray pa = getColumn(column);
    if (pa instanceof StringArray) return;
    double stats[] = pa.calculateStats();
    double min = stats[PrimitiveArray.STATS_MIN];
    double max = stats[PrimitiveArray.STATS_MAX];

    // if no data, don't specify range
    if (Double.isNaN(min)) {
      columnAttributes(column).remove("actual_range");
      return;
    }

    PrimitiveArray minMax = PrimitiveArray.factory(pa.elementType(), 2, false);
    minMax.addDouble(min);
    minMax.addDouble(max);
    columnAttributes(column).set("actual_range", minMax);
  }

  /**
   * If attValue is null, this does nothing. If attValue is "", this removes the attribute (if
   * present).' Otherwise, this sets the attribute.
   */
  private void trySet(Attributes attributes, String attName, String attValue) {
    if (attValue == null) return;
    if (attValue.length() == 0) attributes.remove(attName);
    attributes.set(attName, attValue);
  }

  /**
   * This tests if this object is valid (e.g., each column in 'data' has the same number of rows,
   * and there are no duplicate column names). This also checks the validity of globalAttribute or
   * columnAttribute. (?)
   *
   * @throws a SimpleException if table not valid
   */
  public void ensureValid() {

    // check that all columns have the same size
    int nRows = nRows(); // from column[0]
    int nColumns = nColumns();
    for (int col = 1; col < nColumns; col++)
      if (columns.get(col).size() != nRows)
        throw new SimpleException(
            "Invalid Table: "
                + "column["
                + col
                + "="
                + getColumnName(col)
                + "].size="
                + columns.get(col).size()
                + " != column[0="
                + getColumnName(0)
                + "].size="
                + nRows);

    ensureNoDuplicateColumnNames("");
  }

  /**
   * This throws a SimpleException if there are duplicate column names.
   *
   * @param msg addition message, e.g., "AxisVariable source names: "
   * @throws a SimpleException if there are duplicate column names.
   */
  public void ensureNoDuplicateColumnNames(String msg) {
    columnNames.ensureNoDuplicates("Invalid Table: " + msg + "Duplicate column names: ");
  }

  /** This makes the column names unique by adding _2, _3, ... as needed. */
  public void makeColumnNamesUnique() {
    columnNames.makeUnique();
  }

  /**
   * This adds missingValues (NaN) to columns as needed so all columns have the same number of rows.
   */
  public void makeColumnsSameSize() {

    // columns may have different numbers of rows (some may not have had data)
    // find maxNRows
    int maxNRows = 0;
    int tNCol = nColumns();
    for (int col = 0; col < tNCol; col++) maxNRows = Math.max(columns.get(col).size(), maxNRows);

    // ensure all columns have correct maxNRows
    if (maxNRows > 0) {
      for (int col = 0; col < tNCol; col++) {
        PrimitiveArray pa = columns.get(col);
        pa.addNDoubles(maxNRows - pa.size(), Double.NaN);
      }
    }
  }

  /** This replicates the last value as needed so all columns have the same number of rows. */
  public void ensureColumnsAreSameSize_LastValue() {

    // columns may have different numbers of rows (some may not have had data)
    // find maxNRows
    int maxNRows = 0;
    int tNCol = nColumns();
    for (int col = 0; col < tNCol; col++) maxNRows = Math.max(columns.get(col).size(), maxNRows);

    // ensure all columns have correct maxNRows
    if (maxNRows > 0) {
      for (int col = 0; col < tNCol; col++) {
        PrimitiveArray pa = columns.get(col);
        String s = pa.size() == 0 ? "" : pa.getString(pa.size() - 1);
        pa.addNStrings(maxNRows - pa.size(), s);
      }
    }
  }

  /**
   * This tests if o is a Table with the same data and columnNames as this table. (Currently) This
   * doesn't test globalAttribute or columnAttribute. This requires that the column types be
   * identical.
   *
   * @param o (usually) a Table object
   * @return true if o is a Table object and has column types and data values that equal this Table
   *     object.
   */
  @Override
  public boolean equals(Object o) {
    return equals(o, true);
  }

  /**
   * This tests if o is a Table with the same data and columnNames as this table. (Currently) This
   * doesn't test globalAttribute or columnAttribute.
   *
   * @param o (usually) a Table object
   * @param ensureColumnTypesEqual
   * @return true if o is a Table object and has column types and data values that equal this Table
   *     object.
   */
  public boolean equals(Object o, boolean ensureColumnTypesEqual) {

    String errorInMethod = String2.ERROR + " in Table.equals while testing ";
    try {

      Table table2 = (Table) o;
      ensureValid(); // throws Exception if not
      table2.ensureValid();

      int nRows = nRows();
      int nColumns = nColumns();
      Test.ensureEqual(nRows, table2.nRows(), errorInMethod + "nRows");
      Test.ensureEqual(
          nColumns(),
          table2.nColumns(),
          errorInMethod
              + "nColumns\nthis table: "
              + columnNames.toString()
              + "\nother table: "
              + table2.columnNames.toString());

      for (int col = 0; col < nColumns; col++) {
        Test.ensureEqual(
            getColumnName(col),
            table2.getColumnName(col),
            errorInMethod + "column=" + col + " names.");
        PrimitiveArray array1 = columns.get(col);
        PrimitiveArray array2 = table2.getColumn(col);
        if (ensureColumnTypesEqual)
          Test.ensureEqual(
              array1.elementTypeString(),
              array2.elementTypeString(),
              errorInMethod + "column=" + col + " types.");
        boolean a1String = array1 instanceof StringArray;
        boolean a2String = array2 instanceof StringArray;
        for (int row = 0; row < nRows; row++) {
          String s1 = array1.getString(row);
          String s2 = array2.getString(row);
          if (!s1.equals(s2)) {
            // deal with NaN in long column not simplified to LongArray
            //  so left as NaN in String column
            // or char array missing value ?
            if (a1String && "NaN".equals(s1)) s1 = "";
            if (a2String && "NaN".equals(s2)) s2 = "";
            if (!s1.equals(s2))
              Test.ensureEqual(
                  s1,
                  s2,
                  errorInMethod
                      + "data(col="
                      + col
                      + " ("
                      + array1.elementTypeString()
                      + " vs. "
                      + array2.elementTypeString()
                      + "), row="
                      + row
                      + ").");
          }
        }
      }

      return true;
    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int nColumns = nColumns();
    int nRows = nRows();
    for (int col = 0; col < nColumns; col++) {
      PrimitiveArray array1 = columns.get(col);
      hash = 31 * hash + array1.elementTypeString().hashCode();
      for (int row = 0; row < nRows; row++) {
        String s1 = array1.getString(row);
        hash = 31 * hash + s1.hashCode();
      }
    }
    return hash;
  }

  /**
   * This makes a table with 2 String columns with the keys (sorted) and values from the map.
   *
   * @param map if it needs to be thread-safe, use ConcurrentHashMap
   * @param keysName
   * @param valuesName
   */
  public void readMap(Map map, String keysName, String valuesName) {
    // create the empty table
    clear();
    StringArray keys = new StringArray();
    StringArray values = new StringArray();
    addColumn(keysName, keys);
    addColumn(valuesName, values);

    // get the keys and values
    Set entrySet = map.entrySet();
    Iterator it = entrySet.iterator();
    while (it.hasNext()) {
      Map.Entry me = (Map.Entry) it.next();
      keys.add(me.getKey().toString());
      values.add(me.getValue().toString());
    }
    leftToRightSort(1);
  }

  /**
   * This reads data from an ASCII file. <br>
   * The lineSeparator can be \n, \r\n, or \r. <br>
   * See readASCII(lines, nHeaderLines) for other details. <br>
   * This does simplify the columns.
   *
   * @param fullFileName
   * @param charset e.g., ISO-8859-1 (used if charset is null or "") or UTF-8.
   * @param columnNamesLine (0.., or -1 if no names)
   * @param dataStartLine (0..)
   * @param testColumns the names of the columns to be tested (null = no tests). All of the test
   *     columns must use the same, one, dimension that the loadColumns use. Ideally, the first
   *     tests will greatly restrict the range of valid rows.
   * @param testMin the minimum allowed value for each testColumn (null = no tests)
   * @param testMax the maximum allowed value for each testColumn (null = no tests)
   * @param loadColumns the names of the columns to be loaded (perhaps in different order than in
   *     the file). If loadColumns is null, this will read all of the data columns.
   * @param simplify
   * @throws Exception if trouble
   */
  public void readASCII(
      String fullFileName,
      String charset,
      String skipHeaderToRegex,
      String skipLinesRegex,
      int columnNamesLine,
      int dataStartLine,
      String tColSeparator,
      String testColumns[],
      double testMin[],
      double testMax[],
      String loadColumns[],
      boolean simplify)
      throws Exception {

    readASCII(
        fullFileName,
        File2.getDecompressedBufferedFileReader(fullFileName, charset),
        skipHeaderToRegex,
        skipLinesRegex,
        columnNamesLine,
        dataStartLine,
        tColSeparator,
        testColumns,
        testMin,
        testMax,
        loadColumns,
        simplify);
  }

  /**
   * Another variant. This uses simplify=true.
   *
   * @throws Exception if trouble
   */
  public void readASCII(String fullFileName, int columnNamesLine, int dataStartLine)
      throws Exception {

    readASCII(
        fullFileName,
        File2.ISO_8859_1,
        "",
        "",
        columnNamesLine,
        dataStartLine,
        null,
        null,
        null,
        null,
        null,
        true);
  }

  /**
   * Another variant. This uses columnNamesLine=0, dataStartLine=1, simplify=true.
   *
   * @throws Exception if trouble
   */
  public void readASCII(String fullFileName) throws Exception {

    readASCII(fullFileName, File2.ISO_8859_1, "", "", 0, 1, null, null, null, null, null, true);
  }

  /**
   * This reads data from an array of tab, comma, or space-separated ASCII Strings.
   *
   * <ul>
   *   <li>If no exception is thrown, the file was successfully read.
   *   <li>The item separator in the file can be tab, comma, or 1 or more spaces.
   *   <li>Missing values for tab- and comma-separated files can be "" or "." or "NaN".
   *   <li>Missing values for space-separated files can be "." or "NaN".
   *   <li>Normally, all data rows must have the same number of data items, or the row is skipped.
   *       However, you can set the instance's allowRaggedRightInReadASCII to true to allow missing
   *       values at the end of a row.
   * </ul>
   *
   * @param fileName for diagnostic messages only
   * @param linesReader the array of ASCII strings with the info from the file. This will always
   *     close the reader.
   * @param columnNamesLine (0.., or -1 if no names). If there are no columnNames, names in the form
   *     "Column#<col>" (where col is 0 .. nColumns) will be created.
   * @param dataStartLine (0..) Usually 1 or 2.
   * @param tColSeparator the character that separates the columns. Use "" or null to have this
   *     method guess. Otherwise, the first character of this string will be used.
   * @param testColumns the names of the columns to be tested (null = no tests). All of the test
   *     columns must use the same, one, dimension that the loadColumns use. The tests are done as
   *     if the testColumns were doubles. Ideally, the first tests will greatly restrict the range
   *     of valid rows.
   * @param testMin the minimum allowed value for each testColumn (null = no tests)
   * @param testMax the maximum allowed value for each testColumn (null = no tests)
   * @param loadColumns the names of the columns to be loaded (perhaps in different order than in
   *     the file). If null, this will read all variables.
   * @param simplify The data is initially read as Strings. If this is set to 'true', the columns
   *     are simplified to their simplest type (e.g., to doubles, ... or bytes) so they store the
   *     data compactly. Date strings are left as strings. If this is set to 'false', the columns
   *     are left as strings.
   * @throws Exception if trouble (e.g., a specified testColumn or loadColumn not found)
   */
  public void readASCII(
      String fileName,
      BufferedReader linesReader,
      String skipHeaderToRegex,
      String skipLinesRegex,
      int columnNamesLine,
      int dataStartLine,
      String tColSeparator,
      String testColumns[],
      double testMin[],
      double testMax[],
      String loadColumns[],
      boolean simplify)
      throws Exception {

    try {

      // clear everything
      clear();

      // validate parameters
      // if (reallyVerbose) String2.log("Table.readASCII " + fileName);
      long time = System.currentTimeMillis();
      String errorInMethod = String2.ERROR + " in Table.readASCII(" + fileName + "):\n";
      if (testColumns == null) testColumns = new String[0];
      else {
        Test.ensureEqual(
            testColumns.length,
            testMin.length,
            errorInMethod + "testColumns.length != testMin.length.");
        Test.ensureEqual(
            testColumns.length,
            testMax.length,
            errorInMethod + "testColumns.length != testMax.length.");
        Test.ensureTrue(
            columnNamesLine < dataStartLine,
            errorInMethod
                + "columnNamesLine="
                + columnNamesLine
                + " must be less than dataStartLine="
                + dataStartLine
                + ".");
        Test.ensureTrue(
            dataStartLine >= 0, errorInMethod + "dataStartLine=" + dataStartLine + " must be >=0.");
      }
      Pattern skipLinesPattern =
          skipLinesRegex != null && !skipLinesRegex.equals("")
              ? Pattern.compile(skipLinesRegex)
              : null;

      // skipHeaderToRegex
      int row =
          skipHeaderToRegex(
              skipHeaderToRegex, fileName, linesReader); // the number of rows read (1..)

      // try to read up to 3rd row of data (ignoring skipLines)
      // this method caches initial lines read to enable re-reading some lines near the start
      ArrayList<String> linesCache = new ArrayList();
      int nonSkipLines = 0;
      while (true) {
        String s = linesReader.readLine(); // null if end. exception if trouble
        if (s == null) break;
        linesCache.add(s);
        if (skipLinesPattern == null || skipLinesPattern.matcher(s).matches()) nonSkipLines++;
        if (nonSkipLines == dataStartLine + 2) // both 0-based
        break;
      }
      int linesCacheSize = linesCache.size();

      // determine column separator
      // look for separator that appears the most and on in 3 test lines
      // FUTURE also look for separator that appears the same number of times on the 3 test lines.
      String oneLine;
      char colSeparator = ',';
      if (tColSeparator == null || tColSeparator.length() == 0) {
        int nTab = 1;
        int nComma = 1;
        int nSemi = 1;
        int nSpace = 1;
        int tRow = 0;
        for (int cacheRow = 0; cacheRow < linesCacheSize; cacheRow++) {
          oneLine = linesCache.get(cacheRow);
          if (skipLinesPattern != null && skipLinesPattern.matcher(oneLine).matches()) continue;
          if (tRow++ < dataStartLine) // both are 0..
          continue;
          nTab *=
              String2.countAll(
                  oneLine,
                  "\t"); // * (not +) puts emphasis on every line having several of the separator
          nComma *= String2.countAll(oneLine, ",");
          nSemi *= String2.countAll(oneLine, ";");
          nSpace *= String2.countAll(oneLine, " "); // lines with lots of text can fool this
        }
        colSeparator =
            nTab >= 1 && nTab >= Math.max(nComma, nSemi)
                ? '\t'
                : nComma >= 1 && nComma >= Math.max(nTab, nSemi)
                    ? ','
                    : nSemi >= 1
                        ? ';'
                        : nSpace >= 1
                            ? ' '
                            : '\u0000'; // only one datum per line; colSeparator irrelevant
        if (debugMode)
          String2.log(
              ">> separator=#"
                  + (int) colSeparator
                  + " nTab="
                  + nTab
                  + " nComma="
                  + nComma
                  + " nSemi="
                  + nSemi
                  + " nSpace="
                  + nSpace);
      } else {
        colSeparator = tColSeparator.charAt(0);
      }

      // read the file's column names
      StringArray fileColumnNames = new StringArray();
      int expectedNItems = -1;
      int nextLinesCache = 0;
      int logicalLine = -1; // 0-based line as if skipHeader and skipLines were removed
      if (columnNamesLine >= 0) {
        while (true) {
          oneLine = linesCache.get(nextLinesCache++);
          row++;
          if (skipLinesPattern != null && skipLinesPattern.matcher(oneLine).matches()) continue;
          if (++logicalLine == columnNamesLine) // both are 0-based
          break;
        }

        oneLine = oneLine.trim();
        // break the line into items
        String items[];
        if (colSeparator == '\u0000') items = new String[] {oneLine.trim()};
        else if (colSeparator == ' ') items = oneLine.split(" +"); // regex for one or more spaces
        else if (colSeparator == ',')
          items = StringArray.arrayFromCSV(oneLine); // does handle "'d phrases
        else items = String2.split(oneLine, colSeparator);
        // store the fileColumnNames
        expectedNItems = items.length;
        for (int col = 0; col < expectedNItems; col++) {
          fileColumnNames.add(items[col]);
        }
        // if (reallyVerbose) String2.log("fileColumnNames=" + fileColumnNames);
      }

      // get the data
      int testColumnNumbers[] = null;
      int loadColumnNumbers[] = null;
      StringArray loadColumnSA[] = null;
      boolean missingItemNoted = false;
      StringBuilder warnings = new StringBuilder();
      ArrayList<String> items = new ArrayList(16);
      while (true) {
        oneLine = null;
        if (nextLinesCache < linesCacheSize) {
          oneLine = linesCache.get(nextLinesCache);
          linesCache.set(nextLinesCache, null);
          nextLinesCache++;
        } else {
          oneLine = linesReader.readLine();
          if (oneLine == null) break; // end of linesReader content
        }
        // actual row number
        row++;
        // then check skipLines
        if (skipLinesPattern != null && skipLinesPattern.matcher(oneLine).matches()) continue;
        // then check dataStartLine
        if (++logicalLine < dataStartLine) continue;

        // if (debugMode && row % 1000000 == 0) {
        //    Math2.gcAndWait("Table.readAscii (debugMode)");
        //    String2.log(Math2.memoryString() + "\n" + String2.canonicalStatistics());
        // }

        try {
          // break the lines into items
          if (colSeparator == ',') {
            StringArray.arrayListFromCSV(
                oneLine, ",", true, true,
                items); // trim=true keep=true   //does handle "'d phrases, but leaves them quoted
          } else if (colSeparator == ' ') {
            StringArray.wordsAndQuotedPhrases(oneLine, items); // items are trim'd
          } else if (colSeparator == '\u0000') {
            items.clear();
            items.add(oneLine.trim());
          } else {
            String2.splitToArrayList(oneLine, colSeparator, true, items); // trim=true
          }
          // if (debugMode && logicalLine-dataStartLine<5) String2.log(">> row=" + row + " nItems="
          // + items.length + "\nitems=" + String2.toCSSVString(items));
        } catch (Exception e) {
          warnings.append(String2.WARNING + ": line #" + row + ": " + e.getMessage() + "\n");
          continue;
        }
        int nItems = items.size();

        // one time things
        if (logicalLine == dataStartLine) {
          if (expectedNItems < 0) expectedNItems = nItems;

          // make column names (if not done already)
          for (int col = fileColumnNames.size(); col < nItems; col++)
            fileColumnNames.add("Column#" + col);

          // identify the testColumnNumbers
          testColumnNumbers = new int[testColumns.length];
          for (int col = 0; col < testColumns.length; col++) {
            int po = fileColumnNames.indexOf(testColumns[col], 0);
            if (po < 0)
              throw new IllegalArgumentException(
                  errorInMethod + "testColumn '" + testColumns[col] + "' not found.");
            testColumnNumbers[col] = po;
          }

          // loadColumnNumbers[sourceColumn#] -> outputColumn#
          //  (-1 if a var not in this file)
          if (loadColumns == null) {
            // load all
            loadColumnNumbers = new int[fileColumnNames.size()];
            loadColumnSA = new StringArray[fileColumnNames.size()];
            for (int col = 0; col < fileColumnNames.size(); col++) {
              loadColumnNumbers[col] = col;
              loadColumnSA[col] = new StringArray();
              addColumn(fileColumnNames.get(col), loadColumnSA[col]);
            }
          } else {
            loadColumnNumbers = new int[loadColumns.length];
            loadColumnSA = new StringArray[loadColumns.length];
            for (int col = 0; col < loadColumns.length; col++) {
              loadColumnNumbers[col] = fileColumnNames.indexOf(loadColumns[col], 0);
              loadColumnSA[col] = new StringArray();
              addColumn(loadColumns[col], loadColumnSA[col]);
            }
          }
          // if (reallyVerbose) String2.log("loadColumnNumbers=" +
          // String2.toCSSVString(loadColumnNumbers));
        }

        // ensure nItems is correct
        if (nItems == 0) continue; // silent error
        if (nItems > expectedNItems
            || (nItems < expectedNItems
                && !allowRaggedRightInReadASCII)) { // if allow..., it is noted below once
          warnings.append(
              String2.WARNING
                  + ": skipping line #"
                  + row
                  + ": unexpected number of items (observed="
                  + nItems
                  + ", expected="
                  + expectedNItems
                  + "). [a]\n");
          continue;
        }

        // do the tests
        boolean ok = true;
        for (int test = 0; test < testColumnNumbers.length; test++) {
          int which = testColumnNumbers[test];
          if (which < 0 || which >= nItems) // value treated as NaN. NaN will fail any test.
          continue;
          double d = String2.parseDouble(items.get(which));
          if (d >= testMin[test] && d <= testMax[test]) { // NaN will fail this test
            continue;
          } else {
            ok = false;
            if (debugMode)
              String2.log(">> skipping row=" + row + " because it failed test #" + test);
            break;
          }
        }
        if (!ok) continue;

        // store the data items
        for (int col = 0; col < loadColumnNumbers.length; col++) {
          int itemNumber = loadColumnNumbers[col];
          if (itemNumber < 0) {
            // request col is not in the file
            loadColumnSA[col].add("");
          } else if (itemNumber < nItems) {
            loadColumnSA[col].add(String2.fromNccsvString(items.get(itemNumber)));
          } else if (allowRaggedRightInReadASCII) {
            // it is a bad idea to allow this (who knows which value is missing?),
            // but some buoy files clearly lack the last value,
            // see NdbcMeteorologicalStation.java
            if (!missingItemNoted) {
              warnings.append(
                  "NOTE: skipping line #"
                      + row
                      + " (and others?): unexpected number of items (observed="
                      + nItems
                      + ", expected="
                      + expectedNItems
                      + ") starting on this line. [allowRaggedRightInReadASCII=true]\n");
              missingItemNoted = true;
            }
            loadColumnSA[col].add(""); // missing value
          } // else incorrect nItems added to warnings above
        }
      }
      // if (debugMode) String2.log(">> partial table:\n" + dataToString(4));

      if (warnings.length() > 0)
        String2.log(
            WARNING_BAD_LINE_OF_DATA_IN + "readASCII(" + fileName + "):\n" + warnings.toString());

      // no data?
      if (loadColumnNumbers == null)
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (loadColumns not found)");

      // simplify the columns
      if (simplify) simplify();

      // if (debugMode) String2.log(">> partial table2:\n" + dataToString(4));
      if (reallyVerbose)
        String2.log(
            "  Table.readASCII "
                + fileName
                + " finished. nCols="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");

      // ensure the linesReader is closed
    } finally {
      try {
        linesReader.close();
      } catch (Exception e2) {
        String2.log("Caught: " + MustBe.throwableToString(e2));
      }
    }
  }

  /**
   * This is like the other readStandardTabbedASCII, but this one actually reads the data from the
   * file.
   *
   * @throws Exception if trouble
   */
  public void readStandardTabbedASCII(String fullFileName, String loadColumns[], boolean simplify)
      throws Exception {

    readStandardTabbedASCII(
        fullFileName,
        File2.getDecompressedBufferedFileReader(fullFileName, null),
        loadColumns,
        simplify);
  }

  /**
   * This reads data from an array of tab-separated ASCII Strings. This differs from readASCII in
   * that a given field (but not the last field) can have internal newlines and thus span multiple
   * lines in the file. Also, column names *must* all be on the first line and data *must* start on
   * the second line. (There is no units line.)
   *
   * <ul>
   *   <li>If no exception is thrown, the file was successfully read.
   *   <li>Missing values can be "" or "." or "NaN".
   *   <li>All data rows must have the same number of data items.
   * </ul>
   *
   * @param fileName for diagnostic messages only
   * @param linesReader to get the info from the data file.
   * @param loadColumns the names of the columns to be loaded (perhaps in different order than in
   *     the file). If null, this will read all variables.
   * @param simplify The data is initially read as Strings. If this is set to 'true', the columns
   *     are simplified to their simplest type (e.g., to doubles, ... or bytes) so they store the
   *     data compactly. Date strings are left as strings. If this is set to 'false', the columns
   *     are left as strings.
   * @throws Exception if trouble (e.g., a specified loadColumn wasn't found, or unexpected number
   *     of items on a row)
   */
  public void readStandardTabbedASCII(
      String fileName, BufferedReader linesReader, String loadColumns[], boolean simplify)
      throws Exception {
    try {
      if (reallyVerbose) String2.log("Table.readStandardTabbedASCII " + fileName);
      long time = System.currentTimeMillis();
      String errorInMethod =
          String2.ERROR + " in Table.readStandardTabbedASCII(" + fileName + "):\n";

      char colSeparator = '\t';
      int columnNamesLine = 0; // must be >=0 or code needs to be changed
      int dataStartLine = 1;

      // clear the table
      clear();

      // read the file's column names (must be on one line)
      StringArray fileColumnNames = new StringArray();
      int linei = 0; // line# of next line to be read
      String oneLine = null;
      while (linei <= columnNamesLine) {
        oneLine = linesReader.readLine();
        linei++;
        if (oneLine == null)
          throw new SimpleException(
              errorInMethod
                  + "unexpected end-of-file on line#"
                  + (linei - 1)
                  + ", before columnNamesLine="
                  + columnNamesLine
                  + ".");
      }
      oneLine = oneLine.trim();
      // break the lines into items
      String items[] = String2.split(oneLine, colSeparator);
      int nItems = items.length;
      int expectedNItems = nItems;
      for (int col = 0; col < nItems; col++) fileColumnNames.add(items[col]);
      // if (reallyVerbose) String2.log("fileColumnNames=" + fileColumnNames);

      // identify the loadColumnNumbers
      int loadColumnNumbers[];
      if (loadColumns == null) {
        // load all
        loadColumnNumbers = new int[fileColumnNames.size()];
        for (int col = 0; col < fileColumnNames.size(); col++) loadColumnNumbers[col] = col;
      } else {
        loadColumnNumbers = new int[loadColumns.length];
        for (int col = 0; col < loadColumns.length; col++) {
          int po = fileColumnNames.indexOf(loadColumns[col], 0);
          if (po < 0)
            throw new IllegalArgumentException(
                errorInMethod + "loadColumn '" + loadColumns[col] + "' not found.");
          loadColumnNumbers[col] = po;
        }
        // if (reallyVerbose) String2.log("loadColumnNumbers=" +
        // String2.toCSSVString(loadColumnNumbers));
      }

      // generate the Table's columns which will be loaded
      // and create the primitiveArrays for loaded data
      StringArray loadColumnSA[] = new StringArray[loadColumnNumbers.length];
      for (int col = 0; col < loadColumnNumbers.length; col++) {
        loadColumnSA[col] = new StringArray();
        addColumn(fileColumnNames.get(loadColumnNumbers[col]), loadColumnSA[col]);
      }

      // jump to dataStartLine
      while (linei < dataStartLine) {
        oneLine = linesReader.readLine();
        linei++;
        if (oneLine == null)
          throw new SimpleException(
              errorInMethod
                  + "unexpected end-of-file on line#"
                  + (linei - 1)
                  + ", before dataStartLine="
                  + dataStartLine
                  + ".");
      }

      // get the data
      int row = 0;
      if (debugMode) String2.log("expectedNItems=" + expectedNItems);
      if (debugMode) String2.log("row=" + row);
      READ_ROWS:
      while (true) { // read rows of data
        items = null;
        nItems = 0;
        while (nItems < expectedNItems) {
          oneLine = linesReader.readLine();
          linei++;
          row++;
          if (oneLine == null) { // end-of-file
            if (nItems == 0) break READ_ROWS;
            else
              throw new SimpleException(
                  errorInMethod
                      + "unexpected end-of-file on line#"
                      + (linei - 1)
                      + ". Perhaps last row (and others?) had incorrect number of items.");
          }

          // break the lines into items
          String tItems[] = String2.split(oneLine, colSeparator);
          int ntItems = tItems.length;
          if (debugMode)
            String2.log(
                "row="
                    + (row - 1)
                    + " ntItems="
                    + ntItems
                    + " tItems="
                    + String2.toCSSVString(tItems));

          if (items == null) {
            items = tItems;
          } else {
            // append next line
            String ttItems[] = new String[nItems + ntItems - 1];
            System.arraycopy(items, 0, ttItems, 0, nItems);
            // combine last field old line + newline + first field new line
            ttItems[nItems - 1] += "\n" + tItems[0];
            if (ntItems > 1) System.arraycopy(tItems, 1, ttItems, nItems, ntItems - 1);
            items = ttItems;
          }
          nItems = items.length;
        }

        // too many items?
        if (nItems > expectedNItems)
          throw new RuntimeException(
              errorInMethod
                  + "unexpected number of items ending on line #"
                  + (dataStartLine + row + 1)
                  + " (observed="
                  + nItems
                  + ", expected="
                  + expectedNItems
                  + "). [c]");

        // store the data items
        for (int col = 0; col < loadColumnNumbers.length; col++)
          loadColumnSA[col].add(loadColumnNumbers[col] < 0 ? "" : items[loadColumnNumbers[col]]);
      }

      // simplify the columns
      if (simplify) simplify();

      if (reallyVerbose)
        String2.log(
            "  Table.readStandardTabbedASCII done. fileName="
                + fileName
                + " nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      linesReader.close();
    }
  }

  /**
   * This is like readColumnarASCII, but this one actually reads the data from the file.
   *
   * @param charset ISO-8859-1 (used if charset is null or "") or UTF-8
   * @throws Exception if trouble
   */
  public void readColumnarASCIIFile(
      String fullFileName,
      String charset,
      String skipHeaderToRegex,
      String skipLinesRegex,
      int dataStartLine,
      String loadColumns[],
      int startPo[],
      int endPo[],
      PAType columnPAType[])
      throws Exception {

    BufferedReader br = File2.getDecompressedBufferedFileReader(fullFileName, charset);
    readColumnarASCII(
        fullFileName,
        br,
        skipHeaderToRegex,
        skipLinesRegex,
        dataStartLine,
        loadColumns,
        startPo,
        endPo,
        columnPAType);
  }

  /**
   * This reads data from fixed length ASCII Strings. I.e., each data variable is stored in a
   * specific, fixed substring of each row. On a given line, if a variable's startPo is greater or
   * equal to a line's length, that variable is set to NaN or "". So blank lines and ragged right
   * lines are acceptable. Lines at the end of the file with length=0 are ignored (e.g., a trailing
   * line). If no exception is thrown, the file was successfully read. Any previous columns/conent
   * in the table is thrown away.
   *
   * @param fileName for diagnostic messages only
   * @param reader
   * @param dataStartLine (0..)
   * @param loadColumns the names of the columns to be loaded (perhaps in different order than in
   *     the file). The values won't be changed.
   * @param startPo the substring startPo for each loadColumn (0..) The values won't be changed.
   * @param endPo the substring endPo (exclusive) for each loadColumn (0..) The values won't be
   *     changed.
   * @param columnPAType the class (e.g., PAType.DOUBLE, PAType.STRING) for each column (or null to
   *     have ERDDAP auto simplify the columns). String values are trim'd. PAType.BOOLEAN is a
   *     special case: the string data will be parsed via String2.parseBoolean()? 1 : 0. If already
   *     specified, the values won't be changed.
   * @throws Exception if trouble
   */
  public void readColumnarASCII(
      String fileName,
      BufferedReader reader,
      String skipHeaderToRegex,
      String skipLinesRegex,
      int dataStartLine,
      String loadColumns[],
      int startPo[],
      int endPo[],
      PAType columnPAType[])
      throws Exception {

    String msg = "  Table.readColumnarASCII " + fileName;
    long time = System.currentTimeMillis();
    String errorInMethod = String2.ERROR + " in" + msg + ":\n";

    // clear everything
    clear();

    try {
      // validate parameters
      dataStartLine = Math.max(0, dataStartLine);
      int nCols = loadColumns.length;
      Test.ensureEqual(
          loadColumns.length,
          startPo.length,
          errorInMethod + "loadColumns.length != startPo.length.");
      Test.ensureEqual(
          loadColumns.length, endPo.length, errorInMethod + "loadColumns.length != endPo.length.");
      boolean simplify = columnPAType == null;
      if (simplify) {
        columnPAType = new PAType[nCols];
        Arrays.fill(columnPAType, PAType.STRING);
      }
      Test.ensureEqual(
          loadColumns.length,
          columnPAType.length,
          errorInMethod + "loadColumns.length != columnPAType.length.");
      for (int col = 0; col < nCols; col++) {
        if (startPo[col] < 0)
          throw new RuntimeException(
              errorInMethod
                  + "For sourceName="
                  + loadColumns[col]
                  + ", startPo["
                  + col
                  + "]="
                  + startPo[col]
                  + ". It must be >=0.");
        if (startPo[col] >= endPo[col])
          throw new RuntimeException(
              errorInMethod
                  + "For sourceName="
                  + loadColumns[col]
                  + ", startPo["
                  + col
                  + "]="
                  + startPo[col]
                  + " >= "
                  + "endPo["
                  + col
                  + "]="
                  + endPo[col]
                  + ". startPo must be less than endPo.");
      }
      Pattern skipLinesPattern =
          skipLinesRegex != null && !skipLinesRegex.equals("")
              ? Pattern.compile(skipLinesRegex)
              : null;

      // create the columns
      PrimitiveArray pa[] = new PrimitiveArray[nCols];
      ByteArray arBool[] = new ByteArray[nCols]; // ByteArray (from boolean) if boolean, else null
      for (int col = 0; col < nCols; col++) {
        pa[col] =
            PrimitiveArray.factory(
                columnPAType[col] == PAType.BOOLEAN ? PAType.BYTE : columnPAType[col], 128, false);
        addColumn(loadColumns[col], pa[col]);
        arBool[col] = columnPAType[col] == PAType.BOOLEAN ? (ByteArray) pa[col] : null;
      }

      // skipHeaderToRegex
      int row =
          skipHeaderToRegex(skipHeaderToRegex, fileName, reader); // the number of rows read (1..)

      // get the data
      int logicalLine = -1; // 0-based line as if skipHeader and skipLines were removed
      boolean firstErrorLogged = false;
      while (true) {
        String tLine = reader.readLine();
        row++;
        if (tLine == null) // end of file
        break;
        if (skipLinesPattern != null && skipLinesPattern.matcher(tLine).matches()) continue;
        logicalLine++;
        if (logicalLine < dataStartLine) continue;

        int tLength = tLine.length();
        if (tLength > 0) {
          for (int col = 0; col < nCols; col++) {
            String s =
                tLength > startPo[col]
                    ? tLine.substring(startPo[col], Math.min(tLength, endPo[col])).trim()
                    : "";
            if (columnPAType[col] == PAType.BOOLEAN)
              arBool[col].addInt(String2.parseBooleanToInt(s));
            else pa[col].addString(s);
            // if (row < dataStartLine + 3) String2.log(">> row=" + row + " col=" + col + " s=" +
            // s);
          }
        } else if (!firstErrorLogged) {
          String2.log("  WARNING: row #" + row + " had no data. (There may be other such rows.)");
          firstErrorLogged = true;
        }
      }

      // simplify
      if (simplify) simplify();

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + row
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");

      try {
        reader.close();
      } catch (Exception e2) {
      }
      reader = null;

    } catch (Exception e) {
      try {
        reader.close();
      } catch (Exception e2) {
      }
      if (!reallyVerbose) String2.log(msg + " failed.");
      throw e;
    }
  }

  /**
   * This reads an NCCSV .csv file from a URL or a file. See https://erddap.github.io/NCCSV.html .
   * *SCALAR* and dataType attributes are processed and removed. This just calls readNccsv(fullName,
   * true).
   *
   * @param fullName a URL or the name of a file
   * @throws Exception if trouble
   */
  public void readNccsv(String fullName) throws Exception {
    readNccsv(fullName, true);
  }

  /**
   * This reads an NCCSV .csv file from a URL or a file. *SCALAR* and dataType attributes are
   * processed and removed.
   *
   * @param fullName a URL or the name of a file
   * @param readData If false, the PA for *SCALAR* vars will have 1 value; all others will have 0
   *     values.
   * @throws Exception if trouble
   */
  public void readNccsv(String fullName, boolean readData) throws Exception {
    BufferedReader bufferedReader =
        String2.isRemote(fullName)
            ? SSR.getBufferedUrlReader(fullName)
            : // handles AWS S3.  It assumes UTF-8.
            new BufferedReader(new InputStreamReader(new FileInputStream(fullName), File2.UTF_8));
    try {
      lowReadNccsv(fullName, readData, bufferedReader);
    } finally {
      bufferedReader.close();
    }
  }

  /**
   * This reads an NCCSV .csv file. See https://erddap.github.io/NCCSV.html . *SCALAR* and
   * *DATA_TYPE* attributes are processed and removed.
   *
   * @param fullName for error messages only
   * @param reader from a file or URL
   * @throws SimpleException if trouble (but doesn't close the reader)
   */
  public void lowReadNccsv(String fullName, boolean readData, BufferedReader reader) {
    long time = System.currentTimeMillis();
    long lineNumber = 0;
    clear();
    String conventionsNotFound =
        String2.NCCSV_GLOBAL + ",Conventions,\"..., NCCSV-...\" not found on line 1.";

    try {

      // read the header
      String s;
      HashMap<String, Attributes> varNameAtts = new HashMap();
      HashSet<String> expectedDCols = new HashSet();

      while ((s = reader.readLine()) != null) {
        lineNumber++;
        if (s.startsWith(String2.NCCSV_END_METADATA)) // extra commas are ignored
        break;

        // split the csv line
        StringArray sa = StringArray.simpleFromNccsv(s);
        if (sa.size() < 3) {
          if (lineNumber == 1) throw new SimpleException(conventionsNotFound);
          continue; // e.g., blank line or ignore content
        }
        String varName = String2.fromNccsvString(sa.get(0));
        String attName = String2.fromNccsvString(sa.get(1));
        sa.removeRange(0, 2);
        if (lineNumber == 1) {
          // ensure first line is as expected
          if (!varName.equals(String2.NCCSV_GLOBAL)
              || !attName.equals("Conventions")
              || sa.get(0).indexOf("NCCSV-") < 0) throw new SimpleException(conventionsNotFound);
          globalAttributes.add(attName, String2.fromNccsvString(sa.get(0)));
          continue;
        }
        if (sa.removeEmptyAtEnd() == 0) // extra commas are ignored
        continue;

        // save the attributes
        PrimitiveArray pa = PrimitiveArray.parseNccsvAttributes(sa);
        if (varName.equals(String2.NCCSV_GLOBAL)) {
          globalAttributes.add(attName, pa);
        } else {
          Attributes atts = varNameAtts.get(varName);
          if (atts == null) {
            // create a new column with StringArray capacity=0
            // as a marker for a dummyPA which needs to be set by
            // *DATA_TYPE* or *SCALAR*
            atts = new Attributes();
            varNameAtts.put(varName, atts);
            addColumn(nColumns(), varName, new StringArray(0, false), atts);
          }

          if (String2.NCCSV_DATATYPE.equals(attName)) {
            // if *SCALAR* and *DATA_TYPE* specified, ignore *DATA_TYPE*
            int col = findColumnNumber(varName); // it will exist
            if (columns.get(col).capacity() == 0) // i.e., the dummy pa
              // new PrimitiveArray with capacity=1024
              setColumn(
                  col,
                  PrimitiveArray.factory(
                      PAType.fromCohortStringCaseInsensitive(sa.get(0)), 1024, false)); // active?
            // String2.log(">> " + getColumnName(col) + " " + getColumn(col).elementType());
            expectedDCols.add(varName);
          } else if (String2.NCCSV_SCALAR.equals(attName)) {
            if (pa.size() != 1)
              throw new SimpleException(
                  "There must be just 1 value for a *SCALAR*. varName="
                      + varName
                      + " has "
                      + pa.size()
                      + ".");
            setColumn(findColumnNumber(varName), pa);
          } else {
            // most common case is very fast
            atts.add(attName, pa);
          }
        }
      }
      if (s == null) throw new SimpleException(String2.NCCSV_END_METADATA + NOT_FOUND_EOF);

      // check that all *DATA_TYPE*s were set
      int nc = nColumns();
      for (int c = 0; c < nc; c++) {
        // if (getColumn(c) instanceof CharArray) String2.log(">> col=" + c + " is a CharArray");
        if (columns.get(c).capacity() == 0)
          throw new SimpleException(
              "Neither *SCALAR* nor *DATA_TYPE* were specified for column=" + getColumnName(c));
      }

      // don't readData?
      if (!readData) return;

      // read the column names in the data section
      s = reader.readLine();
      lineNumber++;
      if (s == null) throw new SimpleException("Column names" + NOT_FOUND_EOF);
      StringArray sa = StringArray.simpleFromNccsv(s);
      if (sa.removeEmptyAtEnd() == 0)
        throw new SimpleException("No column names found names at start of data section.");
      sa.fromNccsv(); // un enquote any quoted strings
      int nDataCol = sa.size();
      PrimitiveArray dpa[] = new PrimitiveArray[nDataCol]; // so fast below
      boolean dpaIsLongArray[] = new boolean[nDataCol];
      boolean dpaIsULongArray[] = new boolean[nDataCol];
      boolean dpaIsCharArray[] = new boolean[nDataCol];
      boolean dpaIsStringArray[] = new boolean[nDataCol];
      for (int dcol = 0; dcol < nDataCol; dcol++) {
        String varName = sa.get(dcol);
        if (!String2.isVariableNameSafe(varName))
          throw new SimpleException("varName=" + varName + " is not a valid variableName.");
        int col = findColumnNumber(varName);
        if (col < 0)
          throw new SimpleException(
              "No attributes were specified for varName="
                  + varName
                  + ". *DATA_TYPE* must be specified.");
        dpa[dcol] = columns.get(col);
        // is this a scalar column?!
        if (dpa[dcol].size() == 1)
          throw new SimpleException(
              "*SCALAR* variable=" + varName + " must not be in the data section.");
        // is this column name in csv section twice?
        if (!expectedDCols.remove(varName))
          throw new SimpleException("varName=" + varName + " occurs twice in the data section.");
        dpaIsLongArray[dcol] = dpa[dcol] instanceof LongArray;
        dpaIsULongArray[dcol] = dpa[dcol] instanceof ULongArray;
        dpaIsCharArray[dcol] = dpa[dcol] instanceof CharArray;
        // if (dpaIsCharArray[dcol]) String2.log(">> dcol=" + dcol + " is CharArray");
        dpaIsStringArray[dcol] = dpa[dcol] instanceof StringArray;
      }
      // all expectedDCols were found?
      if (expectedDCols.size() > 0)
        throw new SimpleException(
            "Some variables are missing in the data section: "
                + String2.toCSSVString(expectedDCols.toArray()));

      // read the data
      StringBuilder warnings = new StringBuilder();
      while ((s = reader.readLine()) != null) {
        lineNumber++;
        if (s.startsWith(String2.NCCSV_END_DATA)) // extra commas are ignored
        break;
        try {
          sa = StringArray.simpleFromNccsv(s);
        } catch (Exception e) {
          warnings.append("  line #" + lineNumber + ": " + e.getMessage() + "\n");
          continue;
        }
        if (sa.size() < nDataCol) { // extra commas are ignored
          warnings.append(
              String2.WARNING
                  + ": skipping line #"
                  + lineNumber
                  + ": unexpected number of items (observed="
                  + sa.size()
                  + ", expected="
                  + nDataCol
                  + "). [d]\n");
          continue;
        }
        for (int dcol = 0; dcol < nDataCol; dcol++) {
          String ts = sa.get(dcol);
          if (dpaIsStringArray[dcol]) {
            dpa[dcol].addString(String2.fromNccsvString(ts));
          } else if (dpaIsCharArray[dcol]) {
            ((CharArray) dpa[dcol]).add(String2.fromNccsvChar(ts));
          } else if (dpaIsLongArray[dcol]) {
            if (ts.endsWith("L")) ts = ts.substring(0, ts.length() - 1);
            dpa[dcol].addString(ts);
          } else if (dpaIsULongArray[dcol]) {
            if (ts.endsWith("uL")) ts = ts.substring(0, ts.length() - 2);
            dpa[dcol].addString(ts);
          } else {
            dpa[dcol].addString(ts);
          }
          // String2.log(">> dcol=" + dcol + " " + dpa[dcol].elementType() + " ts=" + ts + " -> " +
          // dpa[dcol].getString(dpa[dcol].size() - 1));
        }
      }
      // if (s == null)  //NCCSV_END_DATA now optional
      //    throw new SimpleException(String2.NCCSV_END_DATA + NOT_FOUND_EOF);

      if (warnings.length() > 0)
        String2.log(
            WARNING_BAD_LINE_OF_DATA_IN + "readNccsv(" + fullName + "):\n" + warnings.toString());

      // expand scalars
      ensureColumnsAreSameSize_LastValue();

      String2.log(
          "readNccsv("
              + fullName
              + ") finished successfully.  nColumns="
              + nColumns()
              + " nRows="
              + nRows()
              + " time="
              + (System.currentTimeMillis() - time)
              + "ms");

    } catch (Exception e) {
      String2.log(MustBe.throwableToString(e));
      throw new SimpleException(
          String2.ERROR
              + " on line #"
              + lineNumber
              + " in readNccsv("
              + fullName
              + "): "
              + e.getMessage());
    }
  }

  /** This is like saveAsNccsv(true, true, Integer.MAX_VALUE) */
  public String saveAsNccsv() throws Exception {
    return saveAsNccsv(true, true, 0, Integer.MAX_VALUE);
  }

  /**
   * This saves this table in an NCCSV .csv file. See https://erddap.github.io/NCCSV.html . This can
   * be a metadata table -- where scalar vars have 1 value and others have 0 values. This doesn't
   * close the writer at the end.
   *
   * @param firstDataRow 0..
   * @param lastDataRow exclusive (use Integer.MAX_VALUE for all rows). If first = last,
   *     *END_METADATA* is the last thing in the file.
   * @throws Exception if trouble. No_data is not an error.
   */
  public String saveAsNccsv(
      boolean catchScalars, boolean writeMetadata, int firstDataRow, int lastDataRow)
      throws Exception {

    StringWriter sw = new StringWriter(1024 + nColumns() * nRows() * 10);
    saveAsNccsv(catchScalars, writeMetadata, firstDataRow, lastDataRow, sw);
    return sw.toString();
  }

  /**
   * This writes this table to an nccsv file. This writes to a temp file, then renames it into
   * place.
   */
  public void saveAsNccsvFile(
      boolean catchScalars,
      boolean writeMetadata,
      int firstDataRow,
      int lastDataRow,
      String fullFileName)
      throws Exception {

    BufferedWriter bw = null;
    int randomInt = Math2.random(Integer.MAX_VALUE);
    String msg = "  Table.saveAsNccsvFile " + fullFileName;
    long time = System.currentTimeMillis();

    try {
      bw = File2.getBufferedFileWriterUtf8(fullFileName + randomInt);
      saveAsNccsv(catchScalars, writeMetadata, firstDataRow, lastDataRow, bw);
      bw.close();
      bw = null;
      File2.rename(fullFileName + randomInt, fullFileName); // throws Exception if trouble
      if (reallyVerbose)
        String2.log(msg + " finished. TIME=" + (System.currentTimeMillis() - time));

    } catch (Exception e) {
      if (bw != null) {
        try {
          bw.close();
        } catch (Throwable t2) {
        }
      }
      File2.delete(fullFileName + randomInt);
      File2.delete(fullFileName);
      String2.log(msg);
      throw e;
    }
  }

  /**
   * This saves this table as an NCCSV .csv file. This can be a metadata table -- where scalar vars
   * have 1 value and others have 0 values. This doesn't change representation of time (e.g., as
   * seconds or as String). This doesn't close the writer at the end.
   *
   * @param catchScalars If true, this looks at the data for scalars (just 1 value).
   * @param writeMetadata If true, this writes the metadata section. This adds a *DATA_TYPE* or
   *     *SCALAR* attribute to each column.
   * @param firstDataRow 0..
   * @param lastDataRow exclusive. Use Integer.MAX_VALUE to write all.
   * @param writer A UTF-8 writer. At the end it is flushed, not closed.
   * @throws Exception if trouble. No_data is not an error.
   */
  public void saveAsNccsv(
      boolean catchScalars, boolean writeMetadata, int firstDataRow, int lastDataRow, Writer writer)
      throws Exception {

    // figure out what's what
    int nc = nColumns();
    int nr = Integer.MAX_VALUE; // shortest non-scalar pa (may be scalars have 1, others 0 or many)
    boolean isLong[] = new boolean[nc];
    boolean isULong[] = new boolean[nc];
    boolean isScalar[] = new boolean[nc];
    boolean allScalar = true;
    int firstNonScalar = nc;
    for (int c = 0; c < nc; c++) {
      PrimitiveArray pa = columns.get(c);
      isLong[c] = pa.elementType() == PAType.LONG;
      isULong[c] = pa.elementType() == PAType.ULONG;
      isScalar[c] = catchScalars && pa.size() > 0 && pa.allSame();
      if (!isScalar[c]) {
        nr = Math.min(nr, pa.size());
        allScalar = false;
        if (firstNonScalar == nc) firstNonScalar = c;
      }
    }

    // write metadata
    if (writeMetadata) {
      writer.write(globalAttributes.toNccsvString(String2.NCCSV_GLOBAL));

      for (int c = 0; c < nc; c++) {
        // scalar
        if (isScalar[c]) {
          writer.write(
              String2.toNccsvDataString(getColumnName(c))
                  + ","
                  + String2.NCCSV_SCALAR
                  + ","
                  + columns.get(c).subset(0, 1, 0).toNccsvAttString()
                  + "\n");
        } else {
          writer.write(
              String2.toNccsvDataString(getColumnName(c))
                  + ","
                  + String2.NCCSV_DATATYPE
                  + ","
                  + columns.get(c).elementTypeString()
                  + "\n");
        }
        writer.write(columnAttributes(c).toNccsvString(getColumnName(c)));
      }
      writer.write("\n" + String2.NCCSV_END_METADATA + "\n");
      writer.flush(); // important
    }

    if (firstDataRow >= lastDataRow) return;

    // write the non-scalar column data
    if (!allScalar) {
      // column names
      for (int c = firstNonScalar; c < nc; c++) {
        if (isScalar[c]) continue;
        if (c > firstNonScalar) writer.write(',');
        writer.write(String2.toNccsvAttString(getColumnName(c)));
      }
      writer.write("\n");

      // csv data
      int tnr = Math.min(nr, lastDataRow);
      for (int r = firstDataRow; r < tnr; r++) {
        for (int c = firstNonScalar; c < nc; c++) {
          if (isScalar[c]) continue;

          if (c > firstNonScalar) writer.write(',');
          String ts = columns.get(c).getNccsvDataString(r);
          writer.write(ts);
          if (isLong[c] && ts.length() > 0)
            writer.write('L'); // special case not handled by getNccsvDataString
          else if (isULong[c] && ts.length() > 0)
            writer.write("uL"); // special case not handled by getNccsvDataString
        }
        writer.write("\n");
      }
    }
    writer.write(String2.NCCSV_END_DATA + "\n");
    writer.flush(); // important
  }

  /**
   * This saves this table as an NCCSV DataOutputStream. This doesn't change representation of time
   * (e.g., as seconds or as String). This never calls dos.flush();
   *
   * @param catchScalars If true, this looks at the data for scalars (just 1 value).
   * @param writeMetadata If true, this writes the metadata section. This adds a *DATA_TYPE* or
   *     *SCALAR* attribute to each column.
   * @param writeDataRows This is the maximum number of data rows to write. Use Integer.MAX_VALUE to
   *     write all.
   * @throws Exception if trouble. No_data is not an error.
   */
  /* project not finished or tested
      public void writeNccsvDos(DataOutputStream dos,   //should be Writer to a UTF-8 file
          boolean catchScalars,
          boolean writeMetadata, int writeDataRows) throws Exception {

          //figure out what's what
          int nc = nColumns();
          int nr = Integer.MAX_VALUE;  //shortest non-scalar pa (may be scalars have 1, others 0 or many)
          boolean isScalar[] = new boolean[nc];
          boolean allScalar = true;
          int firstNonScalar = nc;
          for (int c = 0; c < nc; c++) {
              PrimitiveArray pa = columns.get(c);
              isScalar[c] = catchScalars && pa.size() > 0 && pa.allSame();
              if (!isScalar[c]) {
                  nr = Math.min(nr, pa.size());
                  allScalar = false;
                  if (firstNonScalar == nc)
                      firstNonScalar = c;
              }
          }

          //write metadata
          if (writeMetadata) {
              globalAttributes.writeNccsvDos(dos, String2.NCCSV_GLOBAL);

              for (int c = 0; c < nc; c++) {
                  //scalar
                  if (isScalar[c]) {
                      String2.writeNccsvDos(dos, getColumnName(c));
                      String2.writeNccsvDos(dos, String2.NCCSV_SCALAR);
                      columns.get(c).subset(0, 1, 0).writeNccsvDos(dos);
                  } else {
                      String2.writeNccsvDos(dos, getColumnName(c));
                      String2.writeNccsvDos(dos, String2.NCCSV_DATATYPE);
                      StringArray sa = new StringArray();
                      sa.add(columns.get(c).elementTypeString());
                      sa.writeNccsvDos(dos);
                  }
                  columnAttributes(c).writeNccsvDos(dos, getColumnName(c));
              }
              String2.writeNccsvDos(dos, String2.NCCSV_END_METADATA);
          }

          if (writeDataRows <= 0)
              return;

          //write the non-scalar column data
          if (!allScalar) {
              //column names
              for (int c = firstNonScalar; c < nc; c++) {
                  if (isScalar[c])
                      continue;
                  String2.writeNccsvDos(dos, getColumnName(c));
              }

              //csv data
              int tnr = Math.min(nr, writeDataRows);
              for (int r = 0; r < tnr; r++) {
                  for (int c = firstNonScalar; c < nc; c++) {
                      if (isScalar[c])
                          continue;

                      columns.get(c).writeNccsvDos(dos, r);
                  }
              }
          }
          //String2.writeNccsvDos(dos, String2.NCCSV_END_DATA);
      }
  */

  /**
   * This gets data from the IOBIS website (http://www.iobis.org) by mimicing the Advanced Search
   * form (http://www.iobis.org/OBISWEB/ObisControllerServlet) which has access to a cached version
   * of all the data from all of the obis data providers/resources. So it lets you get results from
   * all data providers/resources with one request. This calls setObisAttributes().
   *
   * @param url I think the only valid url is IOBIS_URL.
   * @param genus case sensitive, use null or "" for no preference
   * @param species case sensitive, use null or "" for no preference
   * @param west the west boundary specified in degrees east, -180 to 180; use null or "" for no
   *     preference; west must be &lt;= east.
   * @param east the east boundary specified in degrees east, -180 to 180; use null or "" for no
   *     preference;
   * @param south the south boundary specified in degrees north; use null or "" for no preference;
   *     south must be &lt;= north.
   * @param north the north boundary specified in degrees north; use null or "" for no preference;
   * @param minDepth in meters; use null or "" for no preference; positive equals down; minDepth
   *     must be &lt;= maxDepth. Depth has few values, so generally good not to specify minDepth or
   *     maxDepth.
   * @param maxDepth in meters; use null or "" for no preference;
   * @param startDate an ISO date string (optional time, space or T connected), use null or "" for
   *     no preference; startDate must be &lt;= endDate. The time part of the string is used.
   * @param endDate
   * @param loadColumns which will follow LON,LAT,DEPTH,TIME,ID (which are always loaded) (use null
   *     to load all). <br>
   *     DEPTH is from Minimumdepth <br>
   *     TIME is from Yearcollected|Monthcollected|Daycollected|Timeofday. <br>
   *     ID is Institutioncode:Collectioncode:Catalognumber. <br>
   *     The available column names are slightly different from obis schema (and without namespace
   *     prefixes)!
   *     <p>The loadColumns storing data as Strings are: "Res_name", "Scientificname",
   *     "Institutioncode", "Catalognumber", "Collectioncode", "Datelastmodified", "Basisofrecord",
   *     "Genus", "Species", "Class", "Kingdom", "Ordername", "Phylum", "Family", "Citation",
   *     "Source", "Scientificnameauthor", "Recordurl", "Collector", "Locality", "Country",
   *     "Fieldnumber", "Notes", "Ocean", "Timezone", "State", "County", "Collectornumber",
   *     "Identifiedby", "Lifestage", "Depthrange", "Preparationtype", "Subspecies", "Typestatus",
   *     "Sex", "Subgenus", "Relatedcatalogitem", "Relationshiptype", "Previouscatalognumber",
   *     "Samplesize".
   *     <p>The loadColumns storing data as doubles are "Latitude", "Longitude", "Minimumdepth",
   *     "Maximumdepth", "Slatitude", "Starttimecollected", "Endtimecollected", "Timeofday",
   *     "Slongitude", "Coordinateprecision", "Seprecision", "Observedweight", "Elatitude",
   *     "Elongitude", "Temperature", "Starttimeofday", "Endtimeofday".
   *     <p>The loadColumns storing data as ints are "Yearcollected", "Monthcollected",
   *     "Daycollected", "Startyearcollected", "Startmonthcollected", "Startdaycollected",
   *     "Julianday", "Startjulianday", "Endyearcollected", "Endmonthcollected", "Enddaycollected",
   *     "Yearidentified", "Monthidentified", "Dayidentified", "Endjulianday", "Individualcount",
   *     "Observedindividualcount".
   * @throws Exception if trouble
   */
  public void readIobis(
      String url,
      String genus,
      String species,
      String west,
      String east,
      String south,
      String north,
      String minDepth,
      String maxDepth,
      String startDate,
      String endDate,
      String loadColumns[])
      throws Exception {

    String errorInMethod = String2.ERROR + " in Table.readIobis: ";
    clear();
    if (genus == null) genus = "";
    if (species == null) species = "";
    if (startDate == null) startDate = "";
    if (endDate == null) endDate = "";
    if (minDepth == null) minDepth = "";
    if (maxDepth == null) maxDepth = "";
    if (south == null) south = "";
    if (north == null) north = "";
    if (west == null) west = "";
    if (east == null) east = "";
    // The website javascript tests that the constraints are supplied in pairs
    // (and NESW must be all or none).
    // I verified: if e.g., leave off north value, south is ignored.
    // So fill in missing values where needed.
    if (west.length() == 0) west = "-180";
    if (east.length() == 0) east = "180";
    if (south.length() == 0) south = "-90";
    if (north.length() == 0) north = "90";
    if (minDepth.length() > 0 && maxDepth.length() == 0) maxDepth = "10000";
    if (minDepth.length() == 0 && maxDepth.length() > 0) minDepth = "0";
    if (startDate.length() > 0 && endDate.length() == 0) endDate = "2100-01-01";
    if (startDate.length() == 0 && endDate.length() > 0) startDate = "1500-01-01";
    // I HAVE NEVER GOTTEN STARTDATE ENDDATE REQUESTS TO WORK,
    // SO I DO THE TEST MANUALLY (BELOW) AFTER GETTING THE DATA.
    // convert iso date time format to their format e.g., 1983/12/31
    // if (startDate.length() > 10) startDate = startDate.substring(0, 10);
    // if (endDate.length()   > 10) endDate   = endDate.substring(0, 10);
    // startDate = String2.replaceAll(startDate, "-", "/");
    // endDate   = String2.replaceAll(endDate,   "-", "/");

    // submit the request
    String userQuery = // the part specified by the user
        "&genus="
            + genus
            + "&species="
            + species
            + "&date1="
            + ""
            + // startDate +
            "&date2="
            + ""
            + // endDate +
            "&depth1="
            + minDepth
            + "&depth2="
            + maxDepth
            + "&south="
            + south
            + "&ss=N"
            + "&north="
            + north
            + "&nn=N"
            + "&west="
            + west
            + "&ww=E"
            + "&east="
            + east
            + "&ee=E";
    String glue = "?site=null&sbox=null&searchCategory=/AdvancedSearchServlet";
    if (reallyVerbose) String2.log("iobis url=" + url + glue + userQuery);
    String response = SSR.getUrlResponseStringUnchanged(url + glue + userQuery);
    // String2.log(response);

    // in the returned web page, search for .txt link, read into tTable
    int po2 =
        response.indexOf(".txt'); return false;\">.TXT</a>"); // changed just before 2007-09-10
    int po1 = po2 == -1 ? -1 : response.lastIndexOf("http://", po2);
    if (po1 < 0) {
      String2.log(
          errorInMethod
              + ".txt link not found in OBIS response; "
              + "probably because no data was found.\nresponse="
              + response);
      return;
    }
    String url2 = response.substring(po1, po2 + 4);
    if (reallyVerbose) String2.log("url2=" + url2);

    // get the .txt file
    String dataLines = SSR.getUrlResponseStringUnchanged(url2);
    dataLines = String2.replaceAll(dataLines, '|', '\t');

    // read the data into a temporary table
    Table tTable = new Table();
    tTable.readASCII(
        url2,
        new BufferedReader(new StringReader(dataLines)),
        "",
        "",
        0,
        1,
        "", // skipHeaderToRegex, skipLinesRegex, columnNamesLine, dataStartLine, colSeparator
        null,
        null,
        null, // constraints
        null,
        false); // just load all the columns, and don't simplify
    dataLines = null;

    // immediatly remove 'index'
    if (tTable.getColumnName(0).equals("index")) tTable.removeColumn(0);

    // convert double columns to doubles, int columns to ints
    int nRows = tTable.nRows();
    int nCols = tTable.nColumns();
    // I wasn't super careful with assigning to Double or Int
    String doubleColumns[] = {
      "Latitude",
      "Longitude",
      "Minimumdepth",
      "Maximumdepth",
      "Slatitude",
      "Starttimecollected",
      "Endtimecollected",
      "Timeofday",
      "Slongitude",
      "Coordinateprecision",
      "Seprecision",
      "Observedweight",
      "Elatitude",
      "Elongitude",
      "Temperature",
      "Starttimeofday",
      "Endtimeofday"
    };
    String intColumns[] = {
      "Yearcollected",
      "Monthcollected",
      "Daycollected",
      "Startyearcollected",
      "Startmonthcollected",
      "Startdaycollected",
      "Julianday",
      "Startjulianday",
      "Endyearcollected",
      "Endmonthcollected",
      "Enddaycollected",
      "Yearidentified",
      "Monthidentified",
      "Dayidentified",
      "Endjulianday",
      "Individualcount",
      "Observedindividualcount"
    };
    for (int col = 0; col < nCols; col++) {
      String s = tTable.getColumnName(col);
      if (String2.indexOf(doubleColumns, s) >= 0)
        tTable.setColumn(col, new DoubleArray(tTable.getColumn(col)));
      if (String2.indexOf(intColumns, s) >= 0)
        tTable.setColumn(col, new IntArray(tTable.getColumn(col)));
    }

    // create and add x,y,z,t,id columns    (numeric cols forced to be doubles)
    addColumn(DataHelper.TABLE_VARIABLE_NAMES[0], new DoubleArray(tTable.findColumn("Longitude")));
    addColumn(DataHelper.TABLE_VARIABLE_NAMES[1], new DoubleArray(tTable.findColumn("Latitude")));
    addColumn(
        DataHelper.TABLE_VARIABLE_NAMES[2], new DoubleArray(tTable.findColumn("Minimumdepth")));
    DoubleArray tPA = new DoubleArray(nRows, false);
    addColumn(DataHelper.TABLE_VARIABLE_NAMES[3], tPA);
    StringArray idPA = new StringArray(nRows, false);
    addColumn(DataHelper.TABLE_VARIABLE_NAMES[4], idPA);
    PrimitiveArray yearPA = tTable.findColumn("Yearcollected");
    PrimitiveArray monthPA = tTable.findColumn("Monthcollected");
    PrimitiveArray dayPA = tTable.findColumn("Daycollected");
    PrimitiveArray timeOfDayPA = tTable.findColumn("Timeofday");
    //        PrimitiveArray timeZonePA = findColumn("Timezone"); //deal with ???
    // obis schema says to construct id as
    // "URN:catalog:[InstitutionCode]:[CollectionCode]:[CatalogNumber]"  but their example is more
    // terse than values I see
    PrimitiveArray insPA = tTable.findColumn("Institutioncode");
    PrimitiveArray colPA = tTable.findColumn("Collectioncode");
    PrimitiveArray catPA = tTable.findColumn("Catalognumber");
    for (int row = 0; row < nRows; row++) {
      // make the t value
      double seconds = Double.NaN;
      StringBuilder sb = new StringBuilder(yearPA.getString(row));
      if (sb.length() > 0) {
        String tMonth = monthPA.getString(row);
        if (tMonth.length() > 0) {
          sb.append("-" + tMonth); // month is 01 - 12
          String tDay = dayPA.getString(row);
          if (tDay.length() > 0) {
            sb.append("-" + tDay);
          }
        }
        try {
          seconds = Calendar2.isoStringToEpochSeconds(sb.toString());
          String tTime = timeOfDayPA.getString(row); // decimal hours since midnight
          int tSeconds = Math2.roundToInt(String2.parseDouble(tTime) * Calendar2.SECONDS_PER_HOUR);
          if (tSeconds < Integer.MAX_VALUE) seconds += tSeconds;
        } catch (Exception e) {
          if (verbose) String2.log("Table.readObis unable to parse date=" + sb);
        }
      }
      tPA.add(seconds);

      // make the id value
      idPA.add(insPA.getString(row) + ":" + colPA.getString(row) + ":" + catPA.getString(row));
    }

    // if loadColumns == null, make loadColumns with all the original column names ('index' already
    // removed)
    if (loadColumns == null) loadColumns = tTable.getColumnNames();

    // add the loadColumns
    for (int col = 0; col < loadColumns.length; col++)
      addColumn(loadColumns[col], tTable.findColumn(loadColumns[col]));

    // no more need for tTable
    tTable = null;

    // do startDate endDate test (if one is specified, both are)
    if (startDate.length() > 0)
      subset(
          new int[] {3},
          new double[] {Calendar2.isoStringToEpochSeconds(startDate)},
          new double[] {Calendar2.isoStringToEpochSeconds(endDate)});

    // remove time=NaN rows? no, it gets rid of otherwise intesting data rows

    // setAttributes  (this sets the coordinate variables' axis, long_name, standard_name, and
    // units)
    // add column attributes from DigirDarwin.properties and DigirObis.properties?
    setObisAttributes(0, 1, 2, 3, url, new String[] {"AdvancedQuery"}, userQuery);
  }

  /**
   * This sets obis attributes to this table. Currently, this is more geared to the
   * http://www.iobis.org/ portal than it should be -- but I'm don't see how to generalize for all
   * possible DiGIR/Darwin/OBIS providers without a bigger infrastructure for metadata info. This
   * correctly deals with attributes that have already been set.
   *
   * @param lonColumn or -1 if none
   * @param latColumn or -1 if none
   * @param depthColumn or -1 if none
   * @param timeColumn or -1 if none
   * @param url the url that was queried
   * @param resources the resources that were queried
   * @param querySummary a summary of the query
   */
  public void setObisAttributes(
      int lonColumn,
      int latColumn,
      int depthColumn,
      int timeColumn,
      String url,
      String resources[],
      String querySummary) {

    String courtesy =
        "OBIS, and the Darwin and OBIS Data Providers ("
            + url
            + " : "
            + String2.toCSSVString(resources)
            + ")";
    String disCit =
        "users acknowledge the OBIS disclaimer (http://www.iobis.org/data/policy/disclaimer/) "
            + "and agree to follow the OBIS citation policy (http://www.iobis.org/data/policy/citation/).";
    String agree =
        // not appropriate if a non-iobis site is the source
        // but the intent and basic info is always appropriate
        "  By using data accessed from " + courtesy + ", " + disCit;
    setAttributes(
        lonColumn,
        latColumn,
        depthColumn,
        timeColumn,
        "Ocean Biogeographic Information System", // boldTitle
        "Point", // cdmDataType
        DataHelper.ERD_CREATOR_EMAIL,
        DataHelper.ERD_CREATOR_NAME,
        DataHelper.ERD_CREATOR_URL,
        DataHelper.ERD_PROJECT,
        "OBIS_" + String2.md5Hex12(url + String2.toCSSVString(resources) + querySummary),
        "GCMD Science Keywords",
        "Oceans > Marine Biology", // not correct if a darwin provider searched for non-oceanography
        // data
        "http://www.iobis.org/ and "
            + url
            + " ("
            + String2.toCSSVString(resources)
            + ")", // references,
        // summary  from http://www.iobis.org/about/     //not appropriate if non-obis
        "The Ocean Biogeographic Information System (OBIS) is the information "
            + "component of the Census of Marine Life (CoML), a growing network of "
            + "more than 1000 researchers in 73 nations engaged in a 10-year initiative "
            + "to assess and explain the diversity, distribution, and abundance of "
            + "life in the oceans - past, present, and future.  OBIS is a web-based "
            + "provider of global geo-referenced information on marine species. "
            + "We contain expert species level and habitat level databases and "
            + "provide a variety of spatial query tools for visualizing relationships "
            + "among species and their environment. OBIS strives to assess and "
            + "integrate biological, physical, and chemical oceanographic data from "
            + "multiple sources. Users of OBIS, including researchers, students, "
            + "and environmental managers, will gain a dynamic view of the "
            + "multi-dimensional oceanic world. You can explore this constantly "
            + "expanding and developing facility through the OBIS Portal (http://www.iobis.org/).",
        courtesy,
        null); // timeLongName

    // customize a little more
    globalAttributes.set(
        "history",
        DataHelper.addBrowserToHistory(
            Calendar2.getCurrentISODateStringZulu() + " " + courtesy + " (" + querySummary + ")"));

    String license = globalAttributes.getString("license");
    if (license.indexOf(disCit) < 0) // agree may change but disCit ending is constant
    globalAttributes.set("license", license + agree);

    String ack = globalAttributes.getString("acknowledgement");
    if (ack.indexOf(courtesy) < 0) globalAttributes.set("acknowledgement", ack + ", " + courtesy);

    String lasConvention = "LAS Intermediate netCDF File";
    String con = globalAttributes.getString("Conventions");
    if (con.indexOf(lasConvention) < 0)
      globalAttributes.set("Conventions", con + ", " + lasConvention);
  }

  /**
   * This reads (and flattens) an xml document and populates this table. See TableXmlHandler for
   * details.
   *
   * @param xml the BufferedReader with access to the xml information
   * @param validate indicates if the XML parser should validate the xml against the .dtd specified
   *     by DOCTYPE in the file (see https://www.w3.org/TR/REC-xml#proc-types). true or false, the
   *     XMLReader always insists that the document be well formed. true or false, the XMLReader
   *     doesn't validate against a schema. The validate parameter will be ignored if the XMLReader
   *     doesn't support validation. (It is a good sign that, on Windows, the XMLReader that comes
   *     with Java seems to support validation, or at least doesn't object to being told to validate
   *     the xml.)
   * @param rowElementXPath the element (XPath style) identifying a row, e.g.,
   *     /response/content/record.
   * @param rowElementAttributes are the attributes of the row element (e.g., "name") which will be
   *     noted and stored in columns of the table. May be null if none. (Other element's attributes
   *     are ignored.) E.g., <row name="Nate"> will cause a column called name to be created (with
   *     value "Nate" for this example row).
   * @param simplify 'true' simplifies the columns to their simplest type (without regard for what
   *     any schema says); 'false' leaves the columns as StringArrays.
   * @throws Exception if trouble
   */
  public void readXml(
      Reader xml,
      boolean validate,
      String rowElementXPath,
      String rowElementAttributes[],
      boolean simplify)
      throws Exception {

    // I had written an JDom + XPath version of readXml. It took 22 s! for a large file.
    // This SAX-based version takes 140 ms!
    // And this version requires a fraction of the memory
    //  (JDom version holds entire doc in place and with zillions of objects
    //  pointing to parts).

    long time = System.currentTimeMillis();
    clear();

    // get the XMLReader
    XMLReader xr =
        TableXmlHandler.getXmlReader(this, validate, rowElementXPath, rowElementAttributes);
    xr.parse(new InputSource(xml));

    // split gml:Point into gml:PointLat gml:pointLon
    //  <gml:Point>
    //    <gml:pos>38.796918000000062 -81.81363499999992</gml:pos>
    //  </gml:Point>
    // see https://en.wikipedia.org/wiki/Geography_Markup_Language#Coordinates
    int nr = nRows();
    String try1 = "/gml:Point/gml:pos"; // space separated
    String try2 = "/gml:Point/gml:coordinates"; // comma separated
    for (int col = nColumns() - 1; col >= 0; col--) {
      String colName = getColumnName(col);
      String match = colName.endsWith(try1) ? try1 : colName.endsWith(try2) ? try2 : null;
      if (match != null) {
        StringArray lat = (StringArray) getColumn(col);
        StringArray lon = new StringArray(nr, false);
        for (int row = 0; row < nr; row++) {
          StringArray tsa = StringArray.wordsAndQuotedPhrases(lat.get(row));
          lat.set(row, tsa.size() >= 1 ? tsa.get(0) : "");
          lon.add(tsa.size() >= 2 ? tsa.get(1) : "");
        }
        colName =
            colName.substring(0, colName.length() - (match.length() - 11)); // after 2nd / of match
        addColumn(col + 1, colName + "longitude", lon, new Attributes());
        setColumnName(col, colName + "latitude");
      }
    }

    // simplify the columns
    if (simplify) simplify();

    if (reallyVerbose)
      String2.log(
          "  Table.readXml done. nColumns="
              + nColumns()
              + " nRows="
              + nRows()
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * This ensure the column exists and has the right name, then adds the value to the column, on
   * currentRow. If there are two or more tags for the same value, this won't throw an error and
   * will keep the last value.
   *
   * @param colName (e.g., aws:ob-date)
   * @param currentRow 0..
   * @param value
   * @param atts If the column already exists, oldAtts.equals(atts) must be true or it throws a
   *     runtimeException.
   */
  private void addAwsColumnValue(String colName, int currentRow, String value, Attributes atts) {

    if (colName.startsWith("aws:")) colName = colName.substring(4);
    int col = findColumnNumber(colName);
    PrimitiveArray pa = null;
    if (col < 0) {
      // add the column
      col = nColumns();
      pa = new StringArray();
      addColumn(col, colName, pa, atts);
    } else {
      pa = getColumn(col);
      // ensure atts are same as oldAtts
      Attributes oAtts = columnAttributes(col);
      String errorMsg = oAtts.testEquals(atts);
      if (errorMsg.length() > 0)
        throw new SimpleException(
            "The attributes for column=" + colName + " aren't consistent:\n" + errorMsg);
    }

    int panRows = pa.size();
    int nToAdd = currentRow - panRows + 1;
    if (nToAdd > 0) pa.addNStrings(nToAdd, "");
    pa.setString(currentRow, value);
  }

  /**
   * This reads all data from one Automatic Weather Station (AWS) file. All columns will be String
   * columns. !!! Note, some units in the file have HTML character entities. ERDDAP converts them to
   * ASCII / UDUnits.
   *
   * @param fullFileName
   * @throws an exception if trouble (file not found, too much data, etc.). This won't throw an
   *     exception if no data.
   */
  public void readAwsXmlFile(String fullFileName) throws Exception {

    long time = System.currentTimeMillis();
    clear(); // clear the table
    String msg = "  Table.readAwsXmlFile " + fullFileName;
    SimpleXMLReader xmlReader =
        new SimpleXMLReader(File2.getDecompressedBufferedInputStream(fullFileName), "aws:weather");
    try {
      GregorianCalendar gc = null;
      int currentRow = -1;
      Attributes atts = null;

      while (true) {
        xmlReader.nextTag();
        int nTags = xmlReader.stackSize();
        String tags = xmlReader.allTags();
        String content = xmlReader.content();

        if (tags.startsWith("<aws:weather><aws:ob>")) {
          String endTags = tags.substring(21);

          //  nTags == 2
          if (nTags == 2) {
            // This is the start of a new row of data.
            currentRow++;
            continue;
          }

          //  nTags == 3
          if (nTags == 3) {
            String tag2 = xmlReader.tag(2);
            boolean isStartTag = !tag2.startsWith("/");

            if (isStartTag) {
              gc = null;
              atts = xmlReader.attributes(); // must make a *new* Attributes object!

              // attributes other than "units" become their own columns
              // e.g., <aws:city-state zip="94123">  becomes city-state-zip
              String[] attNames = xmlReader.attributeNames();
              for (int ani = 0; ani < attNames.length; ani++) {
                if (attNames[ani].equals("units")) {
                  // fix common units problems in AWS xml files
                  String tUnits = atts.getString("units");
                  tUnits = String2.replaceAll(tUnits, "&deg;", "degree_");
                  // String2.pressEnterToContinue("tUnits=" + String2.annotatedString(tUnits));
                  if (tUnits.indexOf('\u0094') >= 0) { // a Windows special "
                    if (tag2.indexOf("pressure") >= 0)
                      tUnits = String2.replaceAll(tUnits, "\u0094", "inch_Hg");
                    else tUnits = String2.replaceAll(tUnits, "\u0094", "inches");
                  }
                  if (tUnits.indexOf('"') >= 0) {
                    if (tag2.indexOf("pressure") >= 0)
                      tUnits = String2.replaceAll(tUnits, "\"", "inch_Hg");
                    else tUnits = String2.replaceAll(tUnits, "\"", "inches");
                  }
                  atts.set("units", tUnits);
                  continue;
                }
                String s = atts.getString(attNames[ani]);
                atts.remove(attNames[ani]);
                if (attNames[ani].equals("xmlns:aws")) {
                  // date start tags have this.  Just remove it.
                } else {
                  addAwsColumnValue(tag2 + "-" + attNames[ani], currentRow, s, new Attributes());
                }
              }

            } else { // is endTag
              String value = xmlReader.content();
              if (gc != null) {
                value = "" + Calendar2.gcToEpochSeconds(gc);
                atts.add("units", "seconds since 1970-01-01T00:00:00Z");
              }
              addAwsColumnValue(
                  tag2.substring(1), // remove leading /
                  currentRow,
                  value,
                  atts);
            }
            continue;
          }

          //  nTags == 4
          if (nTags == 4) {
            // dates
            // <aws:ob-date>
            //  <aws:year number="2012" />
            String tag3 = xmlReader.tag(3);
            if (tag3.startsWith("/")) continue;
            if (tag3.equals("aws:year")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("number"));
              if (ti != Integer.MAX_VALUE) gc.set(Calendar2.YEAR, ti);
            } else if (tag3.equals("aws:month")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("number"));
              if (ti != Integer.MAX_VALUE) gc.set(Calendar2.MONTH, ti - 1); // 0..
            } else if (tag3.equals("aws:day")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("number"));
              if (ti != Integer.MAX_VALUE) gc.set(Calendar2.DATE, ti); // of month
            } else if (tag3.equals("aws:hour")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("hour-24"));
              if (ti != Integer.MAX_VALUE) gc.set(Calendar2.HOUR, ti);
            } else if (tag3.equals("aws:minute")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("number"));
              if (ti != Integer.MAX_VALUE) gc.set(Calendar2.MINUTE, ti);
            } else if (tag3.equals("aws:second")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("number"));
              if (ti != Integer.MAX_VALUE) gc.set(Calendar2.SECOND, ti);
            } else if (tag3.equals("aws:time-zone")) {
              if (gc == null) gc = Calendar2.newGCalendarZulu(0);
              int ti = String2.parseInt(xmlReader.attributeValue("offset"));
              if (ti != Integer.MAX_VALUE) gc.add(Calendar2.HOUR, -ti);
            }
            continue;
          }

        } else if (nTags == 0 || tags.equals("</aws:weather>")) {
          xmlReader.close();
          xmlReader = null;
          break;
        }
      }
      makeColumnsSameSize();
      if (reallyVerbose)
        msg +=
            " finished, nRows=" + nRows() + " time=" + (System.currentTimeMillis() - time) + "ms";
      if (xmlReader != null) {
        xmlReader.close();
        xmlReader = null;
      }
      if (reallyVerbose) String2.log(msg);

    } catch (Throwable t) {
      if (!reallyVerbose) String2.log(msg);
      if (xmlReader != null) xmlReader.close();
      throw t;
    }
  }

  /**
   * This writes the table's data attributes (as if it were a DODS Sequence) to the outputStream as
   * an DODS DAS (see www.opendap.org, DAP 2.0, 7.2.1). Note that the table does needs columns (and
   * their attributes), but it doesn't need any rows of data. See writeDAS.
   *
   * <p>CharArray columns appear as String columns in DAP.
   *
   * @param outputStream the outputStream to receive the results (will be encoded as ISO-8859-1).
   *     Afterwards, it is flushed, not closed.
   * @param sequenceName e.g., "bottle_data_2002"
   * @throws Exception if trouble.
   */
  public void saveAsDAS(OutputStream outputStream, String sequenceName) throws Exception {

    if (reallyVerbose) String2.log("  Table.saveAsDAS");
    long time = System.currentTimeMillis();
    // DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
    Writer writer = File2.getBufferedWriter88591(outputStream);
    writeDAS(writer, sequenceName, false);

    // diagnostic
    if (reallyVerbose)
      String2.log("  Table.saveAsDAS done. TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * This writes the table's data attributes (as if it were a DODS Sequence) to the outputStream as
   * an DODS DAS (see www.opendap.org, DAP 2.0, 7.2.1). Note that the table does needs columns (and
   * their attributes), but it doesn't need any rows of data. E.g. from
   * https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.das
   *
   * <pre>
   * Attributes {
   * bottle_data_2002 {
   * date {
   * String long_name "Date";
   * }
   * ship {
   * String long_name "Ship";
   * }
   * day {
   * String long_name "Day";
   * }
   * year {
   * String long_name "Year";
   * }
   * lat {
   * String long_name "Latitude";
   * }
   * lon {
   * String long_name "Longitude";
   * }
   * chl_a_total {
   * String long_name "Chlorophyll-a";
   * }
   * }
   * }
   * </pre>
   *
   * <p>CharArray columns appear as String columns in DAP.
   *
   * @param writer the Writer to receive the results. Afterwards, it is flushed, not closed.
   * @param sequenceName e.g., "bottle_data_2002"
   * @param encodeAsHtml if true, characters like &lt; are converted to their character entities.
   * @throws Exception if trouble.
   */
  public void writeDAS(Writer writer, String sequenceName, boolean encodeAsHtml) throws Exception {

    writer.write("Attributes {" + OpendapHelper.EOL); // see EOL definition for comments
    writer.write(
        " "
            + XML.encodeAsHTML(sequenceName, encodeAsHtml)
            + " {"
            + OpendapHelper.EOL); // see EOL definition for comments
    for (int v = 0; v < nColumns(); v++)
      OpendapHelper.writeToDAS(
          getColumnName(v), getColumn(v).elementType(), columnAttributes(v), writer, encodeAsHtml);
    writer.write(" }" + OpendapHelper.EOL); // see EOL definition for comments

    // how do global attributes fit into opendap view of attributes?
    OpendapHelper.writeToDAS(
        "NC_GLOBAL", // DAP 2.0 spec doesn't talk about global attributes, was "GLOBAL"; ncBrowse
        // and netcdf-java treat NC_GLOBAL as special case
        PAType.DOUBLE, // isUnsigned doesn't apply to global atts. double won't trigger "_Unsigned"
        globalAttributes,
        writer,
        encodeAsHtml);
    writer.write("}" + OpendapHelper.EOL); // see EOL definition for comments
    writer.flush(); // essential
  }

  /**
   * This writes the table's data structure (as if it were a DODS Sequence) to the outputStream as
   * an DODS DDS (see www.opendap.org, DAP 2.0, 7.2.2). Note that the table does needs columns (and
   * their attributes), but it doesn't need any rows of data. E.g. from
   * https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.dds
   *
   * <pre>
   * Dataset {
   * Sequence {
   * String date;
   * String ship;
   * Byte month;
   * Byte day;
   * Int16 year;
   * Int16 time;
   * Float64 lat;
   * Float64 lon;
   * Float64 chl_a_total;
   * } bottle_data_2002;
   * } bottle_data_2002; </pre>
   *
   * <p>CharArray columns appear as String columns in DAP.
   *
   * @param outputStream the outputStream to receive the results. Afterwards, it is flushed, not
   *     closed.
   * @param sequenceName e.g., "bottle_data_2002"
   * @throws Exception if trouble.
   */
  public void saveAsDDS(OutputStream outputStream, String sequenceName) throws Exception {

    if (reallyVerbose) String2.log("  Table.saveAsDDS");
    long time = System.currentTimeMillis();
    // DAP 2.0 section 3.2.3 says US-ASCII (7bit), so might as well go for compatible common 8bit
    Writer writer = File2.getBufferedWriter88591(outputStream);

    int nColumns = nColumns();
    writer.write("Dataset {" + OpendapHelper.EOL); // see EOL definition for comments
    writer.write("  Sequence {" + OpendapHelper.EOL); // see EOL definition for comments
    for (int v = 0; v < nColumns; v++) {
      PrimitiveArray pa = getColumn(v);
      writer.write(
          "    "
              + OpendapHelper.getAtomicType(pa.elementType())
              + " "
              + getColumnName(v)
              + ";"
              + OpendapHelper.EOL); // see EOL definition for comments
    }
    writer.write(
        "  } " + sequenceName + ";" + OpendapHelper.EOL); // see EOL definition for comments
    writer.write("} " + sequenceName + ";" + OpendapHelper.EOL); // see EOL definition for comments
    writer.flush(); // essential

    if (reallyVerbose)
      String2.log("  Table.saveAsDDS done. TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * This writes the table's data structure (as if it were a DODS Sequence) to the outputStream as
   * DODS ASCII data (which is not defined in DAP 2.0, but which is very close to saveAsDODS below).
   * This mimics
   * https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle.asc?lon,ship,cast,t0,NO3&lon<-125.7
   *
   * <p>This sends missing values as is. This doesn't call convertToFakeMissingValues. Do it
   * beforehand if you need to.
   *
   * @param outputStream the outputStream to receive the results. Afterwards, it is flushed, not
   *     closed.
   * @param sequenceName e.g., "erd_opendap_globec_bottle"
   * @throws Exception if trouble.
   */
  public void saveAsDodsAscii(OutputStream outputStream, String sequenceName) throws Exception {

    if (reallyVerbose) String2.log("  Table.saveAsDodsAscii");
    long time = System.currentTimeMillis();

    // write the dds    //DAP 2.0, 7.2.3
    saveAsDDS(outputStream, sequenceName);

    // write the connector
    Writer writer = File2.getBufferedWriter88591(outputStream);
    writer.write(
        "---------------------------------------------"
            + OpendapHelper.EOL); // see EOL definition for comments

    // write the column names
    int nColumns = nColumns();
    int nRows = nRows();
    boolean isCharOrString[] = new boolean[nColumns];
    for (int col = 0; col < nColumns; col++) {
      isCharOrString[col] =
          getColumn(col).elementType() == PAType.CHAR
              || getColumn(col).elementType() == PAType.STRING;
      writer.write(getColumnName(col) + (col == nColumns - 1 ? OpendapHelper.EOL : ", "));
    }

    // write the data  //DAP 2.0, 7.3.2.3
    // write elements of the sequence, in dds order
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        String s = getColumn(col).getString(row);
        if (isCharOrString[col])
          // see DODS Appendix A, quoted-string.
          // It just talks about " to \", but Json format is implied and better
          s = String2.toJson(s);
        writer.write(s + (col == nColumns - 1 ? OpendapHelper.EOL : ", "));
      }
    }

    writer.flush(); // essential

    if (reallyVerbose)
      String2.log(
          "  Table.saveAsDodsAscii done. TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * This writes the table's data structure (as if it were a DODS Sequence) to the outputStream as
   * an DODS DataDDS (see www.opendap.org, DAP 2.0, 7.2.3).
   *
   * <p>This sends missing values as is. This doesn't call convertToFakeMissingValues. Do it
   * beforehand if you need to.
   *
   * @param outputStream the outputStream to receive the results. Afterwards, it is flushed, not
   *     closed.
   * @param sequenceName e.g., "erd_opendap_globec_bottle"
   * @throws Exception if trouble.
   */
  public void saveAsDODS(OutputStream outputStream, String sequenceName) throws Exception {
    if (reallyVerbose) String2.log("  Table.saveAsDODS");
    long time = System.currentTimeMillis();

    // write the dds    //DAP 2.0, 7.2.3
    saveAsDDS(outputStream, sequenceName);

    // write the connector  //DAP 2.0, 7.2.3
    // see EOL definition for comments
    outputStream.write((OpendapHelper.EOL + "Data:" + OpendapHelper.EOL).getBytes());

    // write the data  //DAP 2.0, 7.3.2.3
    // write elements of the sequence, in dds order
    int nColumns = nColumns();
    int nRows = nRows();
    DataOutputStream dos = new DataOutputStream(outputStream);
    for (int row = 0; row < nRows; row++) {
      dos.writeInt(0x5A << 24); // start of instance
      for (int col = 0; col < nColumns; col++) getColumn(col).externalizeForDODS(dos, row);
    }
    dos.writeInt(0xA5 << 24); // end of sequence; so if nRows=0, this is all that is sent

    dos.flush(); // essential

    if (reallyVerbose)
      String2.log("  Table.saveAsDODS done. TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * This is like the other saveAsHtml, but saves to a UTF-8 file (not just an outputStream).
   *
   * @param fullFileName the complete file name (including directory and extension, usually ".htm"
   *     or ".html").
   */
  public void saveAsHtml(
      String fullFileName,
      String preTableHtml,
      String postTableHtml,
      String otherClasses,
      String bgColor,
      int border,
      boolean writeUnits,
      int timeColumn,
      boolean needEncodingAsHtml,
      boolean allowWrap)
      throws Exception {

    if (reallyVerbose) String2.log("Table.saveAsHtml " + fullFileName);
    long time = System.currentTimeMillis();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(fullFileName + randomInt));

    try {
      // saveAsHtml(outputStream, ...)
      saveAsHtml(
          bos,
          File2.getNameNoExtension(fullFileName),
          preTableHtml,
          postTableHtml,
          otherClasses,
          bgColor,
          border,
          writeUnits,
          timeColumn,
          needEncodingAsHtml,
          allowWrap);
      bos.close();
      bos = null;

      // rename the file to the specified name, instantly replacing the original file
      File2.rename(fullFileName + randomInt, fullFileName); // throws Exception if trouble

    } catch (Exception e) {
      try {
        if (bos != null) bos.close();
      } catch (Exception e2) {
      }
      File2.delete(fullFileName + randomInt);
      File2.delete(fullFileName);
      throw e;
    }

    // diagnostic
    if (reallyVerbose)
      String2.log(
          "Table.saveAsHtml done. fileName="
              + fullFileName
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * Write this data as a table to an outputStream. See saveAsHtmlTable (which this calls) for some
   * of the details.
   *
   * @param outputStream There is no need for it to be already buffered. Afterwards, it is flushed,
   *     not closed.
   * @param fileNameNoExt is the fileName without dir or extension (only used for the document
   *     title).
   * @param preTableHtml is html text to be inserted at the start of the body of the document,
   *     before the table tag (or "" if none).
   * @param postTableHtml is html text to be inserted at the end of the body of the document, after
   *     the table tag (or "" if none).
   * @param otherClasses a space separated list of other (HTML style) CSS classes (or null or "")
   * @param bgColor the backgroundColor, e.g., BGCOLOR, "#f1ecd8" or null (for none defined)
   * @param border the line width of the cell border lines (e.g., 0, 1, 2)
   * @param writeUnits if true, the table's second row will be units (from columnAttributes "units")
   * @param timeColumn the column with epoch seconds which should be written as ISO formatted date
   *     times; if <0, this is ignored.
   * @param needEncodingAsHtml if true, the cell contents will be encodedAsHtml (i.e., they contain
   *     plain text); otherwise, they are written as is (i.e., they already contain html-encoded
   *     text).
   * @param allowWrap if true, data may be broken into different lines so the table is only as wide
   *     as the screen.
   * @throws Exception if trouble. But if no data, it makes a simple html file.
   */
  public void saveAsHtml(
      OutputStream outputStream,
      String fileNameNoExt,
      String preTableHtml,
      String postTableHtml,
      String otherClasses,
      String bgColor,
      int border,
      boolean writeUnits,
      int timeColumn,
      boolean needEncodingAsHtml,
      boolean allowWrap)
      throws Exception {

    if (reallyVerbose) String2.log("Table.saveAsHtml");
    long time = System.currentTimeMillis();

    // write the header
    BufferedWriter writer = File2.getBufferedWriterUtf8(outputStream);
    writer.write(
        "<!DOCTYPE HTML>\n"
            + "<html lang=\"en-US\">\n"
            + "<head>\n"
            + "  <title>"
            + fileNameNoExt
            + "</title>\n"
            + "  <meta charset=\"UTF-8\">\n"
            + "  "
            + ERD_TABLE_CSS
            + "\n"
            + "</head>\n"
            + "<body>\n");

    writer.write(preTableHtml);
    // write the actual table
    saveAsHtmlTable(
        writer,
        otherClasses,
        bgColor,
        border,
        writeUnits,
        timeColumn,
        needEncodingAsHtml,
        allowWrap);

    // close the document
    writer.write(postTableHtml);
    writer.write("</body>\n" + "</html>\n");

    writer.flush(); // essential

    // diagnostic
    if (reallyVerbose)
      String2.log(
          "Table.saveAsHtml done. fileName="
              + fileNameNoExt
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * Save this data as an html table (not a complete html document. <br>
   * This writes the lon values as they are currently in this table (e.g., +-180 or 0..360). <br>
   * If no exception is thrown, the table was successfully written. <br>
   * Currently, all values are written as double. <br>
   * NaN's are written as "NaN". <br>
   * The table will be assigned class="erd". <br>
   * This is an HTML table, not quite a valid XHTML table.
   *
   * @param writer usually already buffered
   * @param otherClasses a space separated list of other (HTML style) CSS classes (or null or "")
   * @param bgColor the backgroundColor, e.g., BGCOLOR, "#f1ecd8" or null (for none defined)
   * @param border the line width of the cell border lines (e.g., 0, 1, 2)
   * @param writeUnits if true, the table's second row will be units (from columnAttributes "units")
   * @param timeColumn the column with epoch seconds which should be written as ISO formatted date
   *     times; if <0, this is ignored.
   * @param needEncodingAsHtml if true, the cell contents will be encodedAsHtml (i.e., they contain
   *     plain text); otherwise, they are written as is (i.e., they already contain html-encoded
   *     text).
   * @param allowWrap if true, data may be broken into different lines so the table is only as wide
   *     as the screen.
   * @throws Exception if trouble. But if no data, it makes a simple html file.
   */
  public void saveAsHtmlTable(
      Writer writer,
      String otherClasses,
      String bgColor,
      int border,
      boolean writeUnits,
      int timeColumn,
      boolean needEncodingAsHtml,
      boolean allowWrap)
      throws Exception {

    if (reallyVerbose) String2.log("  Table.saveAsHtmlTable");
    long time = System.currentTimeMillis();
    String s;

    // no data?
    if (nRows() == 0) {
      writer.write(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");
    } else {
      // this assumes that the class options defined in ERD_TABLE_CSS are available.
      writer.write(
          "<table class=\"erd"
              + // has  empty-cells:show;
              (allowWrap ? "" : " nowrap")
              + (otherClasses == null || otherClasses.length() == 0 ? "" : " " + otherClasses)
              + "\" "
              + (bgColor == null ? "" : "style=\"background-color:" + bgColor + ";\" ")
              + ">\n");

      // write the column names
      writer.write("<tr>\n");
      int nColumns = nColumns();
      String fileAccessBaseUrl[] = new String[nColumns];
      String fileAccessSuffix[] = new String[nColumns];
      if (columnNames != null && columnNames.size() == nColumns) {
        boolean somethingWritten = false;
        for (int col = 0; col < nColumns; col++) {
          s = getColumnName(col);
          if (needEncodingAsHtml) s = XML.encodeAsHTML(s);

          // ensure something written on each row (else row is very narrow)
          if (somethingWritten) {
          } else if (s.trim().length() > 0) {
            somethingWritten = true;
          } else if (col == nColumns - 1) {
            s = "&nbsp;";
          }

          writer.write(
              "<th>"
                  + s
                  +
                  // "</th>" //HTML doesn't require it, so save bandwidth
                  "\n");

          // gather fileAccess attributes
          Attributes catts = columnAttributes(col);
          fileAccessBaseUrl[col] = catts.getString("fileAccessBaseUrl"); // null if none
          fileAccessSuffix[col] = catts.getString("fileAccessSuffix"); // null if none
          if (!String2.isSomething(fileAccessBaseUrl[col])) fileAccessBaseUrl[col] = "";
          if (!String2.isSomething(fileAccessSuffix[col])) fileAccessSuffix[col] = "";
        }
      }
      writer.write("</tr>\n");

      // write the units
      if (writeUnits) {
        writer.write("<tr>\n");
        if (columnNames != null && columnNames.size() == nColumns) {
          boolean somethingWritten = false;
          for (int col = 0; col < nColumns; col++) {
            String tUnits = columnAttributes(col).getString("units");
            if (col == timeColumn) tUnits = "UTC"; // no longer true: "seconds since 1970-01-01..."
            if (tUnits == null) tUnits = "";
            if (needEncodingAsHtml) tUnits = XML.encodeAsHTML(tUnits);

            // ensure something written on each row (else row is very narrow)
            if (somethingWritten) {
            } else if (tUnits.trim().length() > 0) {
              somethingWritten = true;
            } else if (col == nColumns - 1) {
              tUnits = "&nbsp;";
            }

            writer.write(
                "<th>"
                    + tUnits
                    +
                    // "</th>" //HTML doesn't require it, so save bandwidth
                    "\n");
          }
        }
        writer.write("</tr>\n");
      }

      // write the data
      int nRows = nRows();
      for (int row = 0; row < nRows; row++) {
        writer.write("<tr>\n");
        boolean somethingWritten = false;
        for (int col = 0; col < nColumns; col++) {
          writer.write("<td>");
          if (col == timeColumn) {
            double d = getDoubleData(col, row);
            s = Calendar2.safeEpochSecondsToIsoStringTZ(d, "");
          } else {
            s = getStringData(col, row);

            if (fileAccessBaseUrl[col].length() > 0 || fileAccessSuffix[col].length() > 0) {
              // display as a link
              String ts = needEncodingAsHtml ? s : XML.decodeEntities(s); // now decoded
              String url =
                  XML.encodeAsHTMLAttribute(fileAccessBaseUrl[col] + ts + fileAccessSuffix[col]);
              s =
                  "<a href=\""
                      + url
                      + "\">"
                      + (needEncodingAsHtml ? XML.encodeAsHTML(s) : s)
                      + // just the fileName
                      "</a>";
            } else if (needEncodingAsHtml && String2.isUrl(s)) {
              s =
                  "<a href=\""
                      + XML.encodeAsHTMLAttribute(s)
                      + "\">"
                      + XML.encodeAsHTML(s)
                      + "</a>";
            } else if (!needEncodingAsHtml && String2.isUrl(XML.decodeEntities(s))) {
              s = XML.decodeEntities(s);
              s =
                  "<a href=\""
                      + XML.encodeAsHTMLAttribute(s)
                      + "\">"
                      + XML.encodeAsHTML(s)
                      + "</a>";
            } else if (String2.isEmailAddress(s)) {
              // to improve security, convert "@" to " at "
              s = needEncodingAsHtml ? s : XML.decodeEntities(s); // now decoded
              s = XML.encodeAsHTML(String2.replaceAll(s, "@", " at "));
            } else if (needEncodingAsHtml) {
              s = XML.encodeAsHTML(s);
            }
          }

          // ensure something written on each row (else row is very narrow)
          if (somethingWritten) {
          } else if (s.trim().length() > 0) {
            somethingWritten = true;
          } else if (col == nColumns - 1) {
            s = "&nbsp;";
          }

          writer.write(s);
          writer.write(
              // "</td>" + //HTML doesn't require it, so save bandwidth
              "\n");
        }
        writer.write("</tr>\n");
      }

      // close the table
      writer.write("</table>\n");
    }

    // diagnostic
    if (reallyVerbose)
      String2.log(
          "    Table.saveAsHtmlTable done. TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /** This actually reads the file, then reads the HTML table in the file. */
  public void readHtml(
      String fullFileName, int skipNTables, boolean secondRowHasUnits, boolean simplify)
      throws Exception {

    String sar[] = File2.readFromFile(fullFileName, File2.UTF_8, 2);
    Test.ensureEqual(sar[0].length(), 0, sar[0]); // check that there was no error
    // String2.log(String2.annotatedString(sar[1]));
    readHtml(fullFileName, sar[1], skipNTables, secondRowHasUnits, simplify);
  }

  /**
   * This reads a standard HTML table of data from an HTML file.
   *
   * <ul>
   *   <li>Currently, this isn't very precise about HTML syntax.
   *   <li>Currently, this doesn't deal with table-like tags in CDATA. They remain as is.
   *   <li>And it doesn't deal with colspan or rowspan.
   *   <li>Column Names are taken from first row.
   *   <li>If secondRowHasUnits, data should start on 3rd row (else 2nd row).
   *   <li>It is okay if a row has fewer columns or more columns than expected. If table.debugMode =
   *       true, a message will be logged about this.
   *   <li>nRows=0 and nColumns=0 is not an error.
   *   <li>XML.decodeEntities is applied to data in String columns, so there may be HTML tags if the
   *       data is HTML. Common entities (&amp;amp; &amp;lt; &amp;gt; &amp;quot;) are converted to
   *       the original characters. &amp;nbsp; is converted to a regular space.
   * </ul>
   *
   * @param fullFileName just for diagnostics
   * @param html the html text
   * @param skipNTables the number of start &lt;table@gt; tags to be skipped. The tables may be
   *     separate or nested.
   * @param secondRowHasUnits (ERDDAP tables do; others usually don't)
   * @param simplify if the columns should be simplified
   * @throws Exception
   */
  public void readHtml(
      String fullFileName,
      String html,
      int skipNTables,
      boolean secondRowHasUnits,
      boolean simplify)
      throws Exception {
    // This reads data from String, not BufferedReader.
    // But the nature of html means rows of data (and even individual items)
    // may span multiple rows of the file.
    // So hard to handle via BufferedReader.

    if (reallyVerbose) String2.log("Table.readHtml " + fullFileName);
    long time = System.currentTimeMillis();
    String startError = "Error while reading HTML table from " + fullFileName + "\nInvalid HTML: ";

    clear();

    // skipNTables
    int po = 0; // next po to look at
    for (int skip = 0; skip < skipNTables; skip++) {
      po = String2.indexOfIgnoreCase(html, "<table", po);
      if (po < 0)
        throw new RuntimeException(startError + "unable to skip " + skipNTables + " <table>'s.");
      po += 6;
    }

    // find main table
    po = String2.indexOfIgnoreCase(html, "<table", po);
    if (po < 0) throw new RuntimeException(startError + "missing main <table>.");
    po += 6;
    int endTable = String2.indexOfIgnoreCase(html, "</table", po);
    if (endTable < 0) throw new RuntimeException(startError + "missing </table>.");

    // read a row
    int nRows = 0;
    int nCols = 0;
    while (po < endTable) {
      int potr = String2.indexOfIgnoreCase(html, "<tr", po);
      if (potr < 0 || potr > endTable) break;
      // </tr> isn't required, so look for next <tr
      int endRow = String2.indexOfIgnoreCase(html, "<tr", potr + 3);
      if (endRow < 0) endRow = endTable;
      String row = html.substring(potr, endRow);

      // process the row
      int poInRow = 2;
      int tCol = 0;
      while (true) {
        int poth = String2.indexOfIgnoreCase(row, "<th", poInRow);
        int potd = String2.indexOfIgnoreCase(row, "<td", poInRow);
        // ensure at least one was found
        if (poth < 0 && potd < 0) break;
        if (poth < 0) poth = row.length();
        if (potd < 0) potd = row.length();
        int gtPo = row.indexOf('>', Math.min(poth, potd) + 3);
        if (gtPo < 0)
          throw new RuntimeException(
              startError
                  + "missing '>' after final "
                  + (poth < potd ? "<th" : "<td")
                  + " on table row#"
                  + nRows
                  + " on or before line #"
                  + (1 + String2.countAll(html.substring(0, endRow), "\n"))
                  + ".");
        // </th> and </td> aren't required, so look for next tag's <
        int endTagPo = row.indexOf('<', gtPo + 1);
        if (endTagPo < 0) endTagPo = row.length();
        String datum = XML.decodeEntities(row.substring(gtPo + 1, endTagPo).trim());
        // String2.log(">> row=" + nRows + " col=" + tCol + " datum=" +
        // String2.annotatedString(datum));
        if ("\u00a0".equals(datum)) // nbsp
        datum = "";

        if (nRows == 0) {
          // if first row, add a column
          addColumn(datum, new StringArray());
          nCols++;
        } else if (tCol < nCols) {
          if (secondRowHasUnits && nRows == 1) {
            // store the units
            columnAttributes(tCol).add("units", datum);
          } else {
            // append the data
            ((StringArray) getColumn(tCol)).add(datum);
          }
        } else {
          // ignore this and subsequent columns on this row
          if (debugMode) String2.log("!!!Extra columns were found on row=" + nRows);
          break;
        }

        poInRow = endTagPo; // "endTag" might be next <th or <td
        tCol++;
      }

      // add blanks so all columns are same length
      if (tCol < nCols) {
        if (debugMode) String2.log("!!!Too few columns were found on row=" + nRows);
        makeColumnsSameSize();
      }
      nRows++;
      po = endRow;
    }

    // simplify the columns
    if (simplify) simplify();

    // diagnostic
    if (reallyVerbose)
      String2.log(
          "    Table.readHtml done. fileName="
              + fullFileName
              + " nRows="
              + nRows
              + " nCols="
              + nCols
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /** This encodes the values of Attributes before saveAsEnhancedFlatNc. */
  void encodeEnhancedAttributes(Attributes atts) {
    String names[] = atts.getNames();
    int n = names.length;
    for (int i = 0; i < n; i++) {
      PrimitiveArray pa = atts.get(names[i]);
      if (pa instanceof CharArray ca) {
        atts.remove(names[i]);
        atts.set("_encodedCharArray_" + names[i], ShortArray.fromCharArrayBytes(ca));

      } else if (pa instanceof UByteArray ua) {
        atts.remove(names[i]);
        atts.set("_encodedUByteArray_" + names[i], new ByteArray(ua.toArray()));

      } else if (pa instanceof UShortArray ua) {
        atts.remove(names[i]);
        atts.set("_encodedUShortArray_" + names[i], new ShortArray(ua.toArray()));

      } else if (pa instanceof UIntArray ua) {
        atts.remove(names[i]);
        atts.set("_encodedUIntArray_" + names[i], new IntArray(ua.toArray()));

      } else if (pa instanceof LongArray) {
        atts.remove(names[i]);
        atts.set("_encodedLongArray_" + names[i], new StringArray(new String[] {pa.toString()}));

      } else if (pa instanceof ULongArray) {
        atts.remove(names[i]);
        atts.set("_encodedULongArray_" + names[i], new StringArray(new String[] {pa.toString()}));

        // Even nc3 saves attributes via utf-8
        // } else if (pa instanceof StringArray) {
        //    atts.remove(names[i]);
        //    atts.set("_encodedStringArray_" + names[i],
        //        (new StringArray(pa)).toJson()); //change a copy of pa
      }
    }
  }

  /** This decodes the values of an Attributes after readEnhancedFlatNc. */
  void decodeEnhancedAttributes(int sourceVersion, Attributes atts) {
    String names[] = atts.getNames();
    int n = names.length;
    for (int i = 0; i < n; i++) {
      if (names[i].startsWith("_encoded")) {
        PrimitiveArray pa = atts.get(names[i]);
        PAType paType = pa.elementType();
        if (paType == PAType.SHORT && names[i].startsWith("_encodedCharArray_")) {
          atts.remove(names[i]);
          atts.set(names[i].substring(18), CharArray.fromShortArrayBytes((ShortArray) pa));

        } else if (paType == PAType.BYTE && names[i].startsWith("_encodedUByteArray_")) {
          atts.remove(names[i]);
          atts.set(names[i].substring(19), new UByteArray(((ByteArray) pa).toArray()));

        } else if (paType == PAType.SHORT && names[i].startsWith("_encodedUShortArray_")) {
          atts.remove(names[i]);
          atts.set(names[i].substring(20), new UShortArray(((ShortArray) pa).toArray()));

        } else if (paType == PAType.INT && names[i].startsWith("_encodedUIntArray_")) {
          atts.remove(names[i]);
          atts.set(names[i].substring(18), new UIntArray(((IntArray) pa).toArray()));

        } else if (paType == PAType.STRING && names[i].startsWith("_encodedLongArray_")) {
          atts.remove(names[i]);
          atts.set(names[i].substring(18), PrimitiveArray.csvFactory(PAType.LONG, pa.getString(0)));

        } else if (paType == PAType.STRING && names[i].startsWith("_encodedULongArray_")) {
          atts.remove(names[i]);
          atts.set(
              names[i].substring(19), PrimitiveArray.csvFactory(PAType.ULONG, pa.getString(0)));

          // Even nc3 saves attributes via utf-8
          // } else if (paType == PAType.STRING &&
          //    names[i].startsWith("_encodedStringArray_")) {
          //    atts.remove(names[i]);
          //    atts.set(names[i].substring(20),
          //        ((StringArray)pa).fromJson()); //actually a newline separated string
        }
      }
    }
  }

  /**
   * This writes this table as a Bob enhanced flatNc file (supports longs, 2-byte chars, and utf-8
   * strings, for data and attribute values). The table is temporarily modified, but all changes are
   * undone when this is finished.
   *
   * @throws exception if trouble
   */
  public void saveAsEnhancedFlatNc(String fullName) throws Exception {

    // Important: make a new table and make changes to it (even if temporary).
    //  Some other thread may be using this table.

    // encode things
    Table newTable = new Table();
    newTable.globalAttributes().add(globalAttributes);
    newTable.globalAttributes().add("_enhanced_version_", ENHANCED_VERSION);
    encodeEnhancedAttributes(newTable.globalAttributes());

    int nCols = nColumns();
    int nRows = nRows();
    for (int col = 0; col < nCols; col++) {
      PrimitiveArray pa = getColumn(col);
      PAType paType = pa.elementType();
      Attributes atts = (Attributes) columnAttributes(col).clone();
      newTable.addColumn(col, getColumnName(col), pa, atts);

      // for backwardcompatibility, always set it
      if (pa.supportsMaxIsMV()) atts.set("_MaxIsMV", "" + pa.getMaxIsMV());

      if (paType == PAType.CHAR) {
        atts.set("_encoded_", "fromChar");
        newTable.setColumn(col, ShortArray.fromCharArrayBytes((CharArray) pa));

      } else if (paType == PAType.LONG) { // otherwise, stored as doubles (lossy)
        atts.set("_encoded_", "fromLong");
        newTable.setColumn(col, new StringArray(pa));

      } else if (paType == PAType.ULONG) { // otherwise, stored as doubles (lossy)
        atts.set("_encoded_", "fromULong");
        newTable.setColumn(col, new StringArray(pa));

      } else if (paType == PAType.STRING) {
        atts.set(
            "_encoded_", String2.JSON); // not _Encoding because encodeEnhancedAtts will change it
        newTable.setColumn(col, new StringArray(pa).toJson()); // change a copy of pa
      }

      encodeEnhancedAttributes(atts);
    }

    // save newTable
    newTable.saveAsFlatNc(fullName, "row", false); // convertToStandardMissingValues
  }

  /**
   * This reads Bob's enhanced flatNc file (supports longs, 2-byte chars, and utf-8 strings for data
   * and attribute values) into this table (replacing current contents).
   *
   * @param loadColumns a list of column names, or null for all
   * @return source enhancedVersion This is for informational purposes. Normally, all the enhanced
   *     encoding/decoding is dealt with here. This returns <br>
   *     &lt; ENHANCED_VERSION (e.g., -1, i.e. out-of-date) if error because not enhanced or not
   *     convertible to current standards. <br>
   *     ENHANCED_VERSION if all okay (including if from previous version that this was able to deal
   *     with). <br>
   *     &gt;ENHANCED_VERSION if trouble because it's from a future version (with implied
   *     significant changes that aren't caught here).
   * @throws exception if trouble
   */
  public int readEnhancedFlatNc(String fullName, String loadColumns[]) throws Exception {

    lowReadFlatNc(
        fullName,
        loadColumns,
        0,
        false,
        -1); // standardizeWhat=0. doAltStandardization=false (rare!)

    // String2.log(">>After lowReadFlatNc:\n" + toString());

    // check version #
    int sourceVersion = globalAttributes.getInt("_enhanced_version_");
    if (sourceVersion == Integer.MAX_VALUE) sourceVersion = 0;
    if (sourceVersion < 3 || sourceVersion > ENHANCED_VERSION) // currently=4.  ver 3 is readable
    return sourceVersion;
    globalAttributes.remove("_enhanced_version_");

    // decode globalAtts
    decodeEnhancedAttributes(sourceVersion, globalAttributes);

    int nCols = nColumns();
    int nRows = nRows();
    for (int col = 0; col < nCols; col++) {
      PrimitiveArray pa = getColumn(col);
      Attributes atts = columnAttributes(col);
      decodeEnhancedAttributes(sourceVersion, atts);

      // maxIsMV
      PrimitiveArray tpa = atts.remove("_MaxIsMV");
      if (pa.supportsMaxIsMV())
        pa.setMaxIsMV(
            tpa == null
                || tpa.size() == 0
                || // for backwards compatibility
                "true".equals(tpa.getString(0))); // will be ignored if DoubleArray or FloatArray

      if (pa instanceof CharArray) {
        // trouble! significant info loss. There shouldn't be any CharArray
        String2.log(
            String2.ERROR
                + ": Table.readEnhancedFlatNc("
                + fullName
                + ") contained a CharArray variable.");
        return -1; // trouble

      } else if (sourceVersion == 3
          && pa instanceof ShortArray sa
          && "true".equals(atts.getString("_Unsigned"))) { // netcdf recommendation
        // convert sourceVersion=3 unsigned short to char
        atts.remove("_Unsigned");
        setColumn(col, CharArray.fromShortArrayBytes(sa));

      } else if (sourceVersion >= 4
          && pa instanceof ShortArray sa
          && "fromChar".equals(atts.getString("_encoded_"))) {
        atts.remove("_encoded_");
        setColumn(col, CharArray.fromShortArrayBytes(sa));

      } else if (pa instanceof StringArray) {
        String enc = atts.getString("_encoded_");
        atts.remove("_encoded_");
        if ("fromLong".equals(enc)) {
          // convert longs encoded as Strings back to longs
          setColumn(col, new LongArray(pa));
        } else if ("fromULong".equals(enc)) {
          // convert ulongs encoded as Strings back to ulongs
          setColumn(col, new ULongArray(pa));
        } else if (String2.JSON.equals(enc)) {
          // convert UTF-8 back to Java char-based strings
          ((StringArray) pa).fromJson();
        } else {
          // unexpected encoding
          String2.log(
              String2.ERROR
                  + ": Table.readEnhancedFlatNc("
                  + fullName
                  + ") contained an unexpected _encoded_ StringArray: "
                  + enc);
          return -1; // unexpected trouble
        }
      }
    }

    // then...
    convertToUnsignedPAs(); // which looks for attributes like _FillValue and adjusts them
    // convertToStandardMissingValues();

    return ENHANCED_VERSION; // successfully read (and perhaps converted to current version)
  }

  /**
   * This reads all rows of all of the specified columns in a flat .nc file or an http:<ncFile>
   * (several 1D variables (columns), all referencing the same dimension). This also reads global
   * and variable attributes.
   *
   * <p>If the fullName is an http address, the name needs to start with "http:\\" (upper or lower
   * case) and the server needs to support "byte ranges" (see ucar.nc2.NetcdfFile documentation).
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file, or an opendap
   *     url.
   * @param loadColumns if null, this searches for the (pseudo)structure variables
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat, or 0 to do nothing, or
   *     -1 to convertToStandardMissingValues (e.g., -999 to Int.MAX_VALUE)
   * @throws Exception if trouble
   */
  public void readFlatNc(String fullName, String loadColumns[], int standardizeWhat)
      throws Exception {
    lowReadFlatNc(fullName, loadColumns, standardizeWhat, true, -1);
  }

  /**
   * This is like readFlatNc, but just reads the first row of data and all the metadata (useful to
   * get the file info).
   *
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   */
  public void readFlatNcInfo(String fullName, String loadColumns[], int standardizeWhat)
      throws Exception {
    lowReadFlatNc(fullName, loadColumns, standardizeWhat, true, 0);
  }

  /**
   * The low level workhorse for readFlatNc and readFlatNcInfo.
   *
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param doAltStandardization If true (normally) and if standardizeWhat == 0, converts
   *     fakeMissingValues (e.g., -999) to standard PrimitiveArray mv's (e.g., 32767). This always
   *     creates unsigned PAs when _Unsigned=true.
   * @param lastRow the last row to be read (inclusive). If lastRow = -1, the entire var is read.
   */
  public void lowReadFlatNc(
      String fullName,
      String loadColumns[],
      int standardizeWhat,
      boolean doAltStandardization,
      int lastRow)
      throws Exception {

    // get information
    String msg = "  Table.readFlatNc " + fullName;
    long time = System.currentTimeMillis();
    Attributes gridMappingAtts = null;
    NetcdfFile netcdfFile = NcHelper.openFile(fullName);
    try {
      Variable loadVariables[] = NcHelper.findVariables(netcdfFile, loadColumns);

      // fill the table
      clear();
      appendNcRows(loadVariables, 0, lastRow);
      NcHelper.getGroupAttributes(netcdfFile.getRootGroup(), globalAttributes());
      for (int col = 0; col < loadVariables.length; col++) {
        NcHelper.getVariableAttributes(loadVariables[col], columnAttributes(col));

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null) {
          gridMappingAtts =
              NcHelper.getGridMappingAtts(
                  netcdfFile, columnAttributes(col).getString("grid_mapping"));
          if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);
        }
      }

      // unpack
      decodeCharsAndStrings();
      if (standardizeWhat > 0) {
        convertToUnsignedPAs();
        standardize(standardizeWhat);
      } else if (doAltStandardization) {
        convertToUnsignedPAs();
        convertToStandardMissingValues();
      }

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This makes a table global with global attributes and columns with attributes, but nRows=0.
   *
   * @param fullName of the nc file
   * @param sourceColumnNames the list of columns to be loaded. Thus must be specified. If one isn't
   *     found, it's okay; the column will still be in the results, but with no metadata.
   * @param sourceDataTypes
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat This always returns
   *     _Unsigned=true vars and related atts as unsigned vars and atts.
   */
  public void readNcMetadata(
      String fullName, String sourceColumnNames[], String sourceDataTypes[], int standardizeWhat)
      throws Exception {

    // get information
    String msg = "  Table.readNcMetadata " + fullName;
    long time = System.currentTimeMillis();
    Attributes gridMappingAtts = null;
    NetcdfFile netcdfFile = NcHelper.openFile(fullName);
    try {
      // fill the table
      clear();
      NcHelper.getGroupAttributes(netcdfFile.getRootGroup(), globalAttributes());
      for (int col = 0; col < sourceColumnNames.length; col++) {
        Attributes atts = new Attributes();
        addColumn(
            col,
            sourceColumnNames[col],
            PrimitiveArray.factory(PAType.fromCohortString(sourceDataTypes[col]), 0, false),
            atts);
        Variable var = netcdfFile.findVariable(sourceColumnNames[col]);
        if (var == null) {
          if (verbose)
            String2.log(
                String2.WARNING
                    + " in Table.readNcMetadata: variableName="
                    + sourceColumnNames[col]
                    + " not found in "
                    + fullName);
          continue;
        }

        NcHelper.getVariableAttributes(var, atts);

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null) {
          gridMappingAtts = NcHelper.getGridMappingAtts(netcdfFile, atts.getString("grid_mapping"));
          if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);
        }
      }

      // unpack
      decodeCharsAndStrings();
      if (standardizeWhat > 0) {
        convertToUnsignedPAs();
        standardize(standardizeWhat);
      }
      if (reallyVerbose)
        msg +=
            " finished. nColumns="
                + nColumns()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms";
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This is commonly used by nc readers to decode any UTF-8 encoded strings before returning the
   * table.
   *
   * <p>There is similar code in GridDataAccessor and Table.decodeCharsAndStrings().
   */
  public void decodeCharsAndStrings() {
    int nc = nColumns();
    for (int col = 0; col < nc; col++) {
      PrimitiveArray pa = getColumn(col);
      Attributes atts = columnAttributes(col);
      String enc = atts.getString(File2.ENCODING);
      atts.remove(File2.ENCODING);
      // disabled until there is a standard
      //            String charset = atts.getString(String2.CHARSET);
      //            atts.remove(String2.CHARSET);

      // charset
      //            if (String2.isSomething(charset)) {
      //                //check that it is CharArray and 8859-1
      //                if (pa.elementType() != PAType.CHAR)
      //                    setColumn(col, new CharArray(pa));  //too bold?
      //                if (!charset.toLowerCase().equals(File2.ISO_8859_1_LC))
      //                    String2.log("col=" + getColumnName(col) + " has unexpected " +
      //                        String2.CHARSET + "=" + charset);
      //                continue;
      //            }

      // encoding
      if (pa.elementType() != PAType.STRING || !String2.isSomething(enc)) continue;
      enc = enc.toLowerCase();

      // decode
      if (enc.toLowerCase().equals(File2.UTF_8_LC)) {
        // UTF-8
        // String2.log(">> before decode: " + pa);
        ((StringArray) pa).fromUTF8();
        // String2.log(">> after decode: " + pa);

      } else if (enc.toLowerCase().equals(File2.ISO_8859_1_LC)) {
        // unchanged ISO-8859-1 becomes the first page of unicode encoded strings

      } else {
        String2.log("col=" + getColumnName(col) + " has unexpected " + File2.ENCODING + "=" + enc);
      }

      // currently, OTHER ENCODINGS ARE NOT HANDLED
      // JUST LEAVE THE ATTRIBUTE AND VALUE
    }
  }

  /**
   * This reads the 1D variables from a .nc file *and* the scalar (0D) values (duplicated to have
   * the same number of rows).
   *
   * <p>This always converts to standard missing values (MAX_INT, ..., NaN).
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file, or an opendap
   *     url.
   * @param loadColumns The 1D variables to be loaded. If null, this searches for the
   *     (pseudo)structure variables. (The scalar variables are always loaded.)
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat, or 0 to do nothing, or
   *     -1 to convertToStandardMissingValues (e.g., -999 to Int.MAX_VALUE)
   * @param lastRow the last row to be read (inclusive). If lastRow = -1, the entire var is read.
   * @throws Exception if trouble
   */
  public void readFlat0Nc(String fullName, String loadColumns[], int standardizeWhat, int lastRow)
      throws Exception {

    // read the 1D variables
    long time = System.currentTimeMillis();
    lowReadFlatNc(fullName, loadColumns, standardizeWhat, true, lastRow); // doAltStandardization
    int tnRows = nRows();
    String msg = "  Table.readFlat0Nc " + fullName;

    // read the scalar variables
    // getGridMappingAtts() handled by lowReadFlatNc above
    int insertAt = 0;
    NetcdfFile netcdfFile = NcHelper.openFile(fullName);
    try {
      Group rootGroup = netcdfFile.getRootGroup();
      List rootGroupVariables = rootGroup.getVariables();
      int nv = rootGroupVariables.size();
      for (int v = 0; v < nv; v++) {
        Variable var = (Variable) rootGroupVariables.get(v);
        boolean isChar = var.getDataType() == DataType.CHAR;
        if (var.getRank() + (isChar ? -1 : 0) == 0) {
          PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
          // unpack is done at end of method
          // nc allows strings to be 0-terminated or padded with spaces, so always trimEnd
          if (pa instanceof StringArray) pa.setString(0, String2.trimEnd(pa.getString(0)));
          if (tnRows > 1) {
            if (pa instanceof StringArray) pa.addNStrings(tnRows - 1, pa.getString(0));
            else pa.addNDoubles(tnRows - 1, pa.getDouble(0));
          }

          Attributes atts = new Attributes();
          NcHelper.getVariableAttributes(var, atts);
          addColumn(insertAt++, var.getShortName(), pa, atts);
        }
      }

      // unpack
      decodeCharsAndStrings();
      if (standardizeWhat > 0) {
        convertToUnsignedPAs();
        standardize(standardizeWhat);
      } else if (standardizeWhat == 0) {
        // do nothing
      } else {
        convertToUnsignedPAs();
        convertToStandardMissingValues();
      }

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This reads all specified 4D numeric (or 5D char) variables into a flat table with columns
   * x,y,z,t (with type matching the type in the file) + loadColumns. This works with true 4D
   * numeric (or 5D char) variables (not just length=1 x,y,z axes). It does not check for metadata
   * standards compliance or ensure that axes correspond to lon, lat, depth, time. This also reads
   * global and variable attributes.
   *
   * <p>If the fullName is an http address, the name needs to start with "http:\\" (upper or lower
   * case) and the server needs to support "byte ranges" (see ucar.nc2.NetcdfFile documentation).
   *
   * <p>This supports an optional stringVariable which is read from the file as a 1D char array, but
   * used to fill a String column. Dapper/DChart prefers this to a 4D array for the ID info.
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file, or an opendap
   *     url.
   * @param loadColumns if null, this searches for a 4D numeric (or 5D char) variable and loads all
   *     variables using the same 4 axes. If no variables are found, the table will have 0 rows and
   *     columns. All dimension columns are automatically loaded. Dimension names in loadColumns are
   *     ignored (since they will be loaded automatically).
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat, or 0 to do nothing, or
   *     -1 to convertToStandardMissingValues (e.g., -999 to Int.MAX_VALUE)
   * @param stringVariableName the name of the stringVariable (or null if not used)
   * @param stringVariableColumn the columnNumber for the stringVariable (or -1 for the end)
   * @throws Exception if trouble
   */
  public void read4DNc(
      String fullName,
      String loadColumns[],
      int standardizeWhat,
      String stringVariableName,
      int stringVariableColumn)
      throws Exception {

    long time = System.currentTimeMillis();
    String msg = "  Table.read4DNc " + fullName;
    String errorInMethod = String2.ERROR + " in" + msg;
    // get information
    Attributes gridMappingAtts = null;
    NetcdfFile ncFile = NcHelper.openFile(fullName);
    try {
      Variable loadVariables[] = NcHelper.find4DVariables(ncFile, loadColumns);

      // clear the table
      clear();

      // load the variables
      Group group = ncFile.getRootGroup();
      Dimension dimensions[] = new Dimension[4]; // all the 4D arrays are 0=t,1=z,2=y,3=x
      String dimensionNames[] = new String[4];
      Variable axisVariables[] = new Variable[4];
      PrimitiveArray axisPA[] = new PrimitiveArray[4];
      Attributes axisAtts[] = new Attributes[4];
      int xLength, yLength, zLength, tLength;
      boolean needToSetUpAxes = true;
      for (int v = 0; v < loadVariables.length; v++) {
        Variable variable = loadVariables[v];
        if (variable.getRank() < 2) continue;

        // if first variable, set up axis columns
        if (needToSetUpAxes) {
          // get axes
          needToSetUpAxes = false;
          List dimList = variable.getDimensions();
          if (variable.getDataType() != DataType.CHAR && dimList.size() != 4)
            throw new SimpleException(
                errorInMethod
                    + "nDimensions not 4 for numeric variable: "
                    + variable.getFullName());

          if (variable.getDataType() == DataType.CHAR && dimList.size() != 5)
            throw new SimpleException(
                errorInMethod + "nDimensions not 5 for char variable: " + variable.getFullName());

          for (int i = 0; i < 4; i++) {
            dimensions[i] = (Dimension) dimList.get(i);
            dimensionNames[i] = dimensions[i].getName();
            axisVariables[i] = ncFile.findVariable(dimensionNames[i]);
            if (axisVariables[i] == null)
              throw new SimpleException(
                  errorInMethod + "dimension variable not found: " + dimensionNames[i]);
            axisPA[i] = NcHelper.getPrimitiveArray(axisVariables[i]);
            // unpack is done at end of method
            axisAtts[i] = new Attributes();
            NcHelper.getVariableAttributes(axisVariables[i], axisAtts[i]);
          }

          // make axes columns (x,y,z,t) to hold flattened axis values
          xLength = axisPA[3].size();
          yLength = axisPA[2].size();
          zLength = axisPA[1].size();
          tLength = axisPA[0].size();
          int totalNValues = tLength * zLength * yLength * xLength;
          PrimitiveArray xColumn =
              PrimitiveArray.factory(
                  NcHelper.getElementPAType(axisVariables[3]), totalNValues, false);
          PrimitiveArray yColumn =
              PrimitiveArray.factory(
                  NcHelper.getElementPAType(axisVariables[2]), totalNValues, false);
          PrimitiveArray zColumn =
              PrimitiveArray.factory(
                  NcHelper.getElementPAType(axisVariables[1]), totalNValues, false);
          PrimitiveArray tColumn =
              PrimitiveArray.factory(
                  NcHelper.getElementPAType(axisVariables[0]), totalNValues, false);
          addColumn(0, dimensionNames[3], xColumn, axisAtts[3]);
          addColumn(1, dimensionNames[2], yColumn, axisAtts[2]);
          addColumn(2, dimensionNames[1], zColumn, axisAtts[1]);
          addColumn(3, dimensionNames[0], tColumn, axisAtts[0]);

          // populate the columns
          PrimitiveArray axisPA0 = axisPA[0];
          PrimitiveArray axisPA1 = axisPA[1];
          PrimitiveArray axisPA2 = axisPA[2];
          PrimitiveArray axisPA3 = axisPA[3];
          for (int t = 0; t < tLength; t++) {
            for (int z = 0; z < zLength; z++) {
              for (int y = 0; y < yLength; y++) {
                for (int x = 0; x < xLength; x++) {
                  xColumn.addFromPA(axisPA3, x);
                  yColumn.addFromPA(axisPA2, y);
                  zColumn.addFromPA(axisPA1, z);
                  tColumn.addFromPA(axisPA0, t);
                }
              }
            }
          }
        }

        // get data
        PrimitiveArray pa = NcHelper.getPrimitiveArray(variable.read());
        // unpack is done at end of method
        // String2.log("Table.read4DNc v=" + v + ": " + pa);

        // store data
        addColumn(variable.getFullName(), pa);
        NcHelper.getVariableAttributes(variable, columnAttributes(nColumns() - 1));

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null)
          gridMappingAtts =
              NcHelper.getGridMappingAtts(
                  ncFile, columnAttributes(nColumns() - 1).getString("grid_mapping"));
      }

      // load the stringVariable
      if (stringVariableName != null) {
        Variable variable = ncFile.findVariable(stringVariableName);
        PrimitiveArray pa = NcHelper.getPrimitiveArray(variable.read());
        // unpack is done at end of method
        if (debugMode) String2.log("  stringVariableName values=" + pa);
        Attributes atts = new Attributes();
        NcHelper.getVariableAttributes(variable, atts);

        // store data
        if (stringVariableColumn < 0 || stringVariableColumn > nColumns())
          stringVariableColumn = nColumns();
        String sar[] = new String[nRows()];
        Arrays.fill(sar, pa.getString(0));
        addColumn(stringVariableColumn, stringVariableName, new StringArray(sar), atts);
      }

      // load the global metadata
      NcHelper.getGroupAttributes(ncFile.getRootGroup(), globalAttributes());
      if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);

      // unpack
      decodeCharsAndStrings();
      if (standardizeWhat > 0) {
        convertToUnsignedPAs();
        standardize(standardizeWhat);
      } else if (standardizeWhat == 0) {
        // do nothing
      } else {
        convertToUnsignedPAs();
        convertToStandardMissingValues();
      }

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This reads and flattens all specified nDimensional (1 or more) variables (which must have
   * shared dimensions) into a table. <br>
   * Axis vars can't be String variables. <br>
   * Axis vars can be just dimensions (not actual variables).
   *
   * <p>If the fullName is an http address, the name needs to start with "http:\\" (upper or lower
   * case) and the server needs to support "byte ranges" (see ucar.nc2.NetcdfFile documentation).
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file, an .ncml file
   *     (which must end with ".ncml"), or an opendap url.
   * @param loadVariableNames if null or length 0, all vars are read. <br>
   *     If a specified var isn't in the file, there won't be a column in the results file for it.
   *     <br>
   *     All dimension vars are automatically loaded. <br>
   *     0D and 1D vars in loadVariables are ignored (since they will be loaded automatically).
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param constraintAxisVarName or null if none. <br>
   *     This works if constraintAxisVar is sorted ascending (it is tested here); otherwise the
   *     constraint is ignored. <br>
   *     This uses precision of 5 digits, so the constraint is very crude (but valid) for time
   *     (e.g., seconds since 1970-01-01).
   * @param constraintMin if variable is packed, this is packed.
   * @param constraintMax if variable is packed, this is packed.
   * @throws Exception if trouble. But if none of the specified loadVariableNames are present, it is
   *     not an error and it returns an empty table.
   */
  public void readNDNc(
      String fullName,
      String loadVariableNames[],
      int standardizeWhat,
      String constraintAxisVarName,
      double constraintMin,
      double constraintMax)
      throws Exception {

    // clear the table
    clear();
    if (loadVariableNames == null) loadVariableNames = new String[0];
    String msg = "  Table.readNDNC " + fullName;
    if (debugMode)
      String2.log(
          msg
              + "  loadVars:"
              + String2.toCSSVString(loadVariableNames)
              + (constraintAxisVarName == null
                  ? ""
                  : "\n  constrain:"
                      + constraintAxisVarName
                      + " >="
                      + constraintMin
                      + " <="
                      + constraintMax));
    long time = System.currentTimeMillis();
    String errorInMethod = String2.ERROR + " in Table.readNDNc " + fullName + ":\n";
    // get information
    Attributes gridMappingAtts = null;
    StringArray varsNotFound = new StringArray();
    NetcdfFile ncFile = NcHelper.openFile(fullName);
    try {
      // load the global metadata
      NcHelper.getGroupAttributes(ncFile.getRootGroup(), globalAttributes());

      // load the variables
      Variable loadVariables[] = null;
      Dimension loadDims[] = null;
      if (loadVariableNames.length == 0) {
        loadVariables =
            NcHelper.findMaxDVariables(ncFile, ""); // throws exception if no vars with dimensions
      } else {
        ArrayList<Variable> varList = new ArrayList();
        ArrayList<Dimension> dimList = new ArrayList(); // just dims that aren't also variables
        for (int i = 0; i < loadVariableNames.length; i++) {
          Variable variable = ncFile.findVariable(loadVariableNames[i]);
          if (variable == null) {
            Dimension dim = ncFile.findDimension(loadVariableNames[i]);
            if (dim == null) {
              if (verbose) varsNotFound.add(loadVariableNames[i]);
            } else {
              dimList.add(dim);
            }
          } else {
            varList.add(variable);
          }
        }
        loadVariables = NcHelper.variableListToArray(varList);
        loadDims = NcHelper.dimensionListToArray(dimList);
        if (loadVariables.length == 0) {
          int nDims = loadDims.length;
          if (nDims == 0) {
            if (verbose && varsNotFound.size() > 0)
              String2.log("  vars not found: " + varsNotFound.toString());
            return; // empty table
          }
          // just load dimensions that aren't variables
          int shape[] = new int[nDims];
          IntArray iaa[] = new IntArray[nDims];
          for (int i = 0; i < nDims; i++) {
            shape[i] = loadDims[i].getLength();
            iaa[i] = new IntArray();
            addColumn(loadDims[i].getShortName(), iaa[i]); // no variable metadata
          }
          NDimensionalIndex ndi = new NDimensionalIndex(shape);
          int current[] = ndi.getCurrent();
          while (ndi.increment()) {
            for (int i = 0; i < nDims; i++) iaa[i].add(current[i]);
          }
          decodeCharsAndStrings();
          convertToUnsignedPAs();
          // no metadata so no unpack
          if (verbose && varsNotFound.size() > 0)
            String2.log("  vars not found: " + varsNotFound.toString());
          return;
        }
      }

      // go through the variables
      int nAxes = -1; // not set up yet
      int readOrigin[] = null;
      int axisLengths[] = null;
      for (int v = 0; v < loadVariables.length; v++) {
        Variable variable = loadVariables[v];
        boolean isChar = variable.getDataType() == DataType.CHAR;
        if (debugMode) String2.log("var#" + v + "=" + variable.getFullName());

        // is it a 0D variable?
        if (variable.getRank() + (isChar ? -1 : 0) == 0) {
          if (debugMode) String2.log("  skipping 0D var");
          continue;
        }

        // is it an axis variable?
        if (!isChar
            && variable.getRank() == 1
            && variable
                .getDimension(0)
                .getName()
                .equals(variable.getFullName())) { // varName = dimName
          if (debugMode) String2.log("  skipping axisVariable");
          continue;
        }

        // if first non-axis variable, set up axis columns
        if (nAxes < 0) {

          // set up dim variables
          nAxes = variable.getRank() - (isChar ? 1 : 0);
          PrimitiveArray axisPAs[] = new PrimitiveArray[nAxes];
          PrimitiveArray columnPAs[] = new PrimitiveArray[nAxes];
          axisLengths = new int[nAxes];

          List axisList = variable.getDimensions();
          for (int a = 0; a < nAxes; a++) {
            Dimension dimension = (Dimension) axisList.get(a);
            String axisName = dimension.getName();
            axisLengths[a] = dimension.getLength();
            if (debugMode) String2.log("  found axisName=" + axisName + " size=" + axisLengths[a]);
            Attributes atts = new Attributes();
            Variable axisVariable = ncFile.findVariable(axisName);
            if (axisVariable == null) {
              // that's ok; set up dummy 0,1,2,3...
              axisPAs[a] =
                  axisLengths[a] < Byte.MAX_VALUE - 1
                      ? new ByteArray(0, axisLengths[a] - 1)
                      : axisLengths[a] < Short.MAX_VALUE - 1
                          ? new ShortArray(0, axisLengths[a] - 1)
                          : new IntArray(0, axisLengths[a] - 1);
            } else {
              axisPAs[a] = NcHelper.getPrimitiveArray(axisVariable);
              NcHelper.getVariableAttributes(axisVariable, atts);
            }
            columnPAs[a] = PrimitiveArray.factory(axisPAs[a].elementType(), 1, false);
            addColumn(a, axisName, columnPAs[a], atts);
            standardizeColumn(standardizeWhat, a);
          }
          readOrigin = new int[nAxes]; // all 0's
          // readShape = axisLengths

          // deal with constraintAxisVarName
          int constraintCol =
              constraintAxisVarName == null ? -1 : findColumnNumber(constraintAxisVarName);
          int constraintFirst = -1;
          int constraintLast = -1;
          if (constraintCol >= 0 && !Double.isNaN(constraintMin) && !Double.isNaN(constraintMax)) {
            PrimitiveArray cpa = axisPAs[constraintCol];
            String asc = cpa.isAscending();
            if (asc.length() == 0) {
              constraintFirst =
                  cpa.binaryFindFirstGAE(0, cpa.size() - 1, PAOne.fromDouble(constraintMin), 5);
              if (constraintFirst >= cpa.size()) constraintFirst = -1;
              else
                constraintLast =
                    cpa.binaryFindLastLAE(
                        constraintFirst, cpa.size() - 1, PAOne.fromDouble(constraintMax), 5);
              if (debugMode)
                String2.log(
                    "  constraintAxisVar="
                        + constraintAxisVarName
                        + " is ascending.  first="
                        + constraintFirst
                        + " last(inclusive)="
                        + constraintLast);
              if (constraintFirst >= 0 && constraintLast >= constraintFirst) {
                // ok, use it
                readOrigin[constraintCol] = constraintFirst;
                axisLengths[constraintCol] = constraintLast - constraintFirst + 1;
                cpa.removeRange(constraintLast + 1, cpa.size());
                cpa.removeRange(0, constraintFirst);
              }
            } else {
              if (debugMode)
                String2.log(
                    "  constraintAxisVar=" + constraintAxisVarName + " isn't ascending: " + asc);
            }
          }

          // populate the axes columns
          NDimensionalIndex ndi = new NDimensionalIndex(axisLengths);
          Math2.ensureArraySizeOkay(ndi.size(), "Table.readNDNc");
          int nRows = (int) ndi.size(); // safe since checked above
          int current[] = ndi.getCurrent();
          for (int a = 0; a < nAxes; a++) columnPAs[a].ensureCapacity(nRows);
          while (ndi.increment()) {
            for (int a = 0; a < nAxes; a++) {
              // String2.log("  a=" + a + " current[a]=" + current[a] +
              //    " axisPAs[a].size=" + axisPAs[a].size());
              // getDouble not getString since axisVars aren't strings, double is faster
              columnPAs[a].addDouble(axisPAs[a].getDouble(current[a]));
            }
          }
        }

        // ensure axes are as expected
        if (isChar) {
          Test.ensureEqual(
              variable.getRank(),
              nAxes + 1,
              errorInMethod
                  + "Unexpected nDimensions for String variable: "
                  + variable.getFullName());
        } else {
          Test.ensureEqual(
              variable.getRank(),
              nAxes,
              errorInMethod
                  + "Unexpected nDimensions for numeric variable: "
                  + variable.getFullName());
        }
        for (int a = 0; a < nAxes; a++)
          Test.ensureEqual(
              variable.getDimension(a).getName(),
              getColumnName(a),
              errorInMethod + "Unexpected axis#" + a + " for variable=" + variable.getFullName());

        // get the data
        int tReadOrigin[] = readOrigin;
        int tReadShape[] = axisLengths;
        if (isChar && variable.getRank() == nAxes + 1) {
          tReadOrigin = new int[nAxes + 1]; // all 0's
          tReadShape = new int[nAxes + 1];
          System.arraycopy(readOrigin, 0, tReadOrigin, 0, nAxes);
          System.arraycopy(axisLengths, 0, tReadShape, 0, nAxes);
          tReadOrigin[nAxes] = 0;
          tReadShape[nAxes] = variable.getDimension(nAxes).getLength();
        }
        Array array = variable.read(tReadOrigin, tReadShape);
        PrimitiveArray pa = NcHelper.getPrimitiveArray(array);
        Test.ensureEqual(
            pa.size(),
            nRows(),
            errorInMethod + "Unexpected nRows for " + variable.getFullName() + ".");

        // store data
        addColumn(variable.getFullName(), pa);
        NcHelper.getVariableAttributes(variable, columnAttributes(nColumns() - 1));

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null) {
          gridMappingAtts =
              NcHelper.getGridMappingAtts(
                  ncFile, columnAttributes(nColumns() - 1).getString("grid_mapping"));
          if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);
        }
      }

      // if the request is only for axis variables, set up axis columns
      // Note that request must have been for specific vars, since all vars would have found
      // non-axis vars.
      if (nAxes < 0) { // no non-axis vars were requested; all vars are axis vars
        // This isn't quite right.  There may be other axes which weren't requested.
        // But in that case, this will at least return all distinct combinations
        //  of the requested axis vars.

        // ensure names are available dimensions
        //  !!they could be nDimensional vars that aren't in this file
        ArrayList<Dimension> dimensions = new ArrayList();
        for (int v = 0; v < loadVariableNames.length; v++) {
          String axisName = loadVariableNames[v];
          Dimension dimension = ncFile.findDimension(axisName);
          if (dimension != null) dimensions.add(dimension);
        }

        if (dimensions.size() > 0) {

          // set up dim variables
          nAxes = dimensions.size();
          PrimitiveArray axisPAs[] = new PrimitiveArray[nAxes];
          PrimitiveArray columnPAs[] = new PrimitiveArray[nAxes];
          axisLengths = new int[nAxes];

          for (int a = 0; a < nAxes; a++) {
            Dimension dimension = (Dimension) dimensions.get(a);
            String axisName = dimension.getName();
            Attributes atts = new Attributes();
            axisLengths[a] = dimension.getLength();
            if (debugMode) String2.log("  found axisName=" + axisName + " size=" + axisLengths[a]);
            Variable axisVariable = ncFile.findVariable(axisName);
            if (axisVariable == null) {
              // that's ok; set up dummy 0,1,2,3...
              axisPAs[a] =
                  axisLengths[a] < Byte.MAX_VALUE - 1
                      ? new ByteArray(0, axisLengths[a] - 1)
                      : axisLengths[a] < Short.MAX_VALUE - 1
                          ? new ShortArray(0, axisLengths[a] - 1)
                          : new IntArray(0, axisLengths[a] - 1);
            } else {
              axisPAs[a] = NcHelper.getPrimitiveArray(axisVariable);
              NcHelper.getVariableAttributes(axisVariable, atts);
            }
            columnPAs[a] = PrimitiveArray.factory(axisPAs[a].elementType(), 1, false);
            addColumn(a, axisName, columnPAs[a], atts);
            standardizeColumn(standardizeWhat, a);
          }
          readOrigin = new int[nAxes]; // all 0's
          // readShape = axisLengths

          // deal with constraintAxisVarName
          int constraintCol =
              constraintAxisVarName == null ? -1 : findColumnNumber(constraintAxisVarName);
          int constraintFirst = -1;
          int constraintLast = -1;
          if (constraintCol >= 0 && !Double.isNaN(constraintMin) && !Double.isNaN(constraintMax)) {
            PrimitiveArray cpa = axisPAs[constraintCol];
            String asc = cpa.isAscending();
            if (asc.length() == 0) {
              constraintFirst =
                  cpa.binaryFindFirstGAE(0, cpa.size() - 1, PAOne.fromDouble(constraintMin), 5);
              if (constraintFirst >= cpa.size()) constraintFirst = -1;
              else
                constraintLast =
                    cpa.binaryFindLastLAE(
                        constraintFirst, cpa.size() - 1, PAOne.fromDouble(constraintMax), 5);
              if (debugMode)
                String2.log(
                    "  constraintAxisVar="
                        + constraintAxisVarName
                        + " is ascending.  first="
                        + constraintFirst
                        + " last(inclusive)="
                        + constraintLast);
              if (constraintFirst >= 0 && constraintLast >= constraintFirst) {
                // ok, use it
                readOrigin[constraintCol] = constraintFirst;
                axisLengths[constraintCol] = constraintLast - constraintFirst + 1;
                cpa.removeRange(constraintLast + 1, cpa.size());
                cpa.removeRange(0, constraintFirst);
              }
            } else {
              if (debugMode)
                String2.log(
                    "  constraintAxisVar=" + constraintAxisVarName + " isn't ascending: " + asc);
            }
          }

          // populate the axes columns
          NDimensionalIndex ndi = new NDimensionalIndex(axisLengths);
          Math2.ensureArraySizeOkay(ndi.size(), "Table.readNDNc");
          int nRows = (int) ndi.size(); // safe since checked above
          int current[] = ndi.getCurrent();
          for (int a = 0; a < nAxes; a++) columnPAs[a].ensureCapacity(nRows);
          while (ndi.increment()) {
            for (int a = 0; a < nAxes; a++) {
              // String2.log("  a=" + a + " current[a]=" + current[a] +
              //    " axisPAs[a].size=" + axisPAs[a].size());
              columnPAs[a].addFromPA(axisPAs[a], current[a]);
            }
          }
        }
      }

      // load the 0D variables
      Group rootGroup = ncFile.getRootGroup();
      List rootGroupVariables = rootGroup.getVariables();
      int tnRows = nRows();
      for (int v = 0; v < rootGroupVariables.size(); v++) {
        Variable var = (Variable) rootGroupVariables.get(v);
        boolean isChar = var.getDataType() == DataType.CHAR;
        if (var.getRank() + (isChar ? -1 : 0) == 0) {
          // if loadVariableNames specified, skip var because not explicitly requested?
          if (loadVariableNames.length > 0
              && String2.indexOf(loadVariableNames, var.getShortName()) < 0) continue;

          // read it
          PrimitiveArray pa = NcHelper.getPrimitiveArray(var);
          // nc allows strings to be 0-terminated or padded with spaces, so always trimEnd
          if (pa instanceof StringArray) pa.setString(0, String2.trimEnd(pa.getString(0)));
          if (tnRows > 1) {
            if (pa instanceof StringArray) pa.addNStrings(tnRows - 1, pa.getString(0));
            else pa.addNDoubles(tnRows - 1, pa.getDouble(0));
          }

          Attributes atts = new Attributes();
          NcHelper.getVariableAttributes(var, atts);
          addColumn(nColumns(), var.getShortName(), pa, atts);
          standardizeLastColumn(standardizeWhat);
        }
      }
      decodeCharsAndStrings();
      convertToUnsignedPAs();
      if (verbose && varsNotFound.size() > 0)
        String2.log("  vars not found: " + varsNotFound.toString());
      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nRows="
                + nRows()
                + " nCols="
                + nColumns()
                + " time="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /** This standardizes every column. See Attributes.unpackVariable for details. */
  public void standardize(int standardizeWhat) throws Exception {
    int nCols = nColumns();
    for (int col = 0; col < nCols; col++) standardizeColumn(standardizeWhat, col);
  }

  /** This unpacks one column. See Attributes.unpackVariable for details. */
  public void standardizeColumn(int standardizeWhat, int col) throws Exception {
    if (standardizeWhat > 0)
      setColumn(
          col,
          columnAttributes(col)
              .standardizeVariable(standardizeWhat, getColumnName(col), getColumn(col)));
  }

  public void standardizeLastColumn(int standardizeWhat) throws Exception {
    standardizeColumn(standardizeWhat, nColumns() - 1);
  }

  /**
   * This inserts columns starting at column #0 with the indices for the specified shape (0, 1, 2,
   * ..., for each dimension in shape[]).
   *
   * @param shape
   * @throws RuntimeException if this requires &gt;= Integer.MAX_VALUE rows.
   */
  public void addIndexColumns(int shape[]) {
    if (shape == null || shape.length == 0) return;
    int nDims = shape.length;
    IntArray indexPAs[] = new IntArray[nDims];
    NDimensionalIndex ndIndex = new NDimensionalIndex(shape);
    long totalSizeL = ndIndex.size();
    if (totalSizeL >= Integer.MAX_VALUE) throw new RuntimeException("Too many rows of data.");
    int totalSize = Math2.narrowToInt(totalSizeL);
    for (int d = 0; d < nDims; d++) {
      indexPAs[d] = new IntArray(totalSize, false);
      addColumn(d, "_index_" + d, indexPAs[d], new Attributes());
    }
    int current[] = ndIndex.getCurrent();
    while (ndIndex.increment()) {
      for (int d = 0; d < nDims; d++) indexPAs[d].add(current[d]);
    }
  }

  /**
   * 2021: NOT FINISHED. This reads and flattens a group of variables in a sequence or nested
   * sequence in a .nc or .bufr file. <br>
   * For strings, this always calls String2.trimEnd(s)
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc or .bufr file, an
   *     .ncml file (which must end with ".ncml"), or an opendap url.
   *     <p>If the fullName is an http address, the name needs to start with "http://" or "https://"
   *     (upper or lower case) and the server needs to support "byte ranges" (see
   *     ucar.nc2.NetcdfFile documentation). But this is very slow, so not recommended.
   * @param loadVarNames Use the format sequenceName.name or sequenceName.sequenceName.name. If
   *     loadVarNames is specified, those variables will be loaded. If loadVarNames isn't specified,
   *     this method reads vars which use the specified loadDimNames and scalar vars. <br>
   *     If a specified var isn't in the file, there won't be a column in the results table for it
   *     and it isn't an error.
   * @param loadSequenceNames. If loadVarNames is specified, this is ignored. If loadSequenceNames
   *     is used, only this outer sequence and/or nested sequence is read. If loadDimNames isn't
   *     specified (or size=0), this method finds the first sequence (and nested sequence). So if
   *     you want to get just the scalar vars, request a nonexistent dimension (e.g., ZZTOP).
   * @param getMetadata if true, global and variable metadata is read
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param conVars the names of the constraint variables. May be null. It is up to this method how
   *     much they will be used. Currently, the constraints are just used for *quick* tests to see
   *     if the file has no matching data. If a conVar isn't in the loadVarNames (provided or
   *     derived), then the constraint isn't used. If standardizeWhat != 0, the constaints are
   *     applied to the unpacked variables.
   * @param conOps the operators for the constraints. All ERDDAP ops are supported. May be null.
   * @param conVals the values of the constraints. May be null.
   * @throws Exception if unexpected trouble. But if none of the specified loadVariableNames are
   *     present or a requested dimension's size=0, it is not an error and it returns an empty
   *     table.
   */
  /*    public void readNcSequence(String fullName,
          StringArray loadVarNames,
          StringArray loadSequenceNames,
          boolean getMetadata,
          int standardizeWhat,
          StringArray conVars, StringArray conOps, StringArray conVals) throws Exception {

          //clear the table
          clear();
          HashSet<String> loadVarNamesSet = null;
          if (loadVarNames != null && loadVarNames.size() > 0) {
              loadVarNamesSet = new HashSet();
              for (int i = 0; i < loadVarNames.size(); i++)
                  loadVarNamesSet.add(loadVarNames.get(i));
          }
          HashSet<String> loadSequenceNamesSet = null;
          if (loadSequenceNames != null && loadSequenceNames.size() > 0) {
              loadSequenceNamesSet = new HashSet();
              for (int i = 0; i < loadSequenceNames.size(); i++)
                  loadSequenceNamesSet.add(loadSequenceNames.get(i));
          }
          if (standardizeWhat != 0)
              getMetadata = true;
          String msg = "  Table.readNcSequence " + fullName +
              "\n  loadVars=" + loadVarNames;
          long time = System.currentTimeMillis();
          String warningInMethod = "Table.readNcSequence read " + fullName + ":\n";
          boolean haveConstraints =
              conVars != null && conVars.size() > 0 &&
              conOps  != null && conOps.size() == conVars.size() &&
              conVals != null && conVals.size() == conVars.size();

          //read the file
          Attributes gridMappingAtts = null;
          NetcdfFile ncFile = NcHelper.openFile(fullName);
          try {

              //load the global metadata
              if (getMetadata)
                  NcHelper.getGroupAttributes(ncFile.getRootGroup(), globalAttributes());

              //go through all the variables
              List<Variable> allVars = ncFile.getVariables();
              int nAllVars = allVars.size();
              StringArray seqNames = new StringArray();
              boolean printSeq3Error = true;
              for (Variable outerVar : allVars) {

                  String outerVarName = outerVar.getFullName();
                  String2.log("outerVar=" + outerVarName);


                  //is it a sequence???
                  if (outerVar instanceof Sequence seq1) {
  // new attempt
                      ArraySequence arSeq = (ArraySequence)seq1.read(); //reads all, in memory
                      List<StructureMembers.Member> memberList1 = arSeq.getMembers();
                      for (StructureMembers.Member mem1 : memberList1) {
                          String memName1 = outerVarName + "." + mem1.getFullName();
                          if (loadVarNamesSet == null || loadVarNamesSet.contains(memName1)) {
                              //add it
                              Array tar = arSeq.extractMemberArray(mem1);
   String2.log("mem1=" + memName1 + "=" + tar.getClass().getCanonicalName());
                              if (tar instanceof ArrayObject.D1 seq2) {
                                  //member is a sequence
                                  String2.log("[0]=" + seq2.get(0).getClass().getCanonicalName());


                              } else {
                                  //simple member
                                  addColumn(nColumns(), memName1,
                                      NcHelper.getPrimitiveArray(tar, false, tar.isUnsigned()), //buildStringFromChar?, tar in .nc4 may be unsigned
                                      new Attributes());
                              }
                          }
                      }




  /*
                      //e.g., "obs" in the test file
                      StructureDataIterator seqIter1 = seq1.getStructureIterator(65536); //go through the rows
                      int rowNum1 = -1;
                      try {
                          while (seqIter1.hasNext()) {
                              StructureData sd1 = seqIter1.next();  //a row
                              rowNum1++;
                              int memberNum1 = -1;
                              ArrayList<PrimitiveArray> pas1 = new ArrayList(); //the pa in this table for each member (or null)
                              for (Iterator sdIter1 = sd1.getMembers().iterator(); sdIter1.hasNext(); ) { //go through members/columns
                                  StructureMembers.Member m1 = (StructureMembers.Member)sdIter1.next();
                                  memberNum1++;
                                  String mFullName1 = outerVarName + "." + m1.getFullName(); //getFullName isn't full name
      String2.log("mFullName1=" + mFullName1);

                                  //is it a sequence???
                                  if (m1 instanceof Sequence seq2) {
                                      //e.g., "seq1" in the test file
                                      StructureDataIterator seqIter2 = seq2.getStructureIterator(65536); //go through the rows
                                      int rowNum2 = -1;
                                      try {
                                          while (seqIter2.hasNext()) {
                                              StructureData sd2 = seqIter2.next();  //a row
                                              rowNum2++;
                                              int memberNum2 = -1;
                                              ArrayList<PrimitiveArray> pas2 = new ArrayList(); //the pa in this table for each member (or null)
                                              for (Iterator sdIter2 = sd2.getMembers().iterator(); sdIter2.hasNext(); ) { //go through members/columns
                                                  StructureMembers.Member m2 = (StructureMembers.Member)sdIter2.next();
                                                  memberNum2++;

                                                  //is it a sequence???
                                                  if (m2 instanceof Sequence)
                                                      throw new RuntimeException("3+ levels of sequences isn't supported.");

                                                  if (rowNum2 == 0) {
                                                      //first row of the seq2
                                                      String mFullName2 = mFullName1 + "." + m2.getFullName(); //getFullName isn't full name
                  String2.log("mFullName2=" + mFullName2);
                                                      PrimitiveArray tpa2 = NcHelper.getPrimitiveArray(sd2.getArray(m2), false); //buildStringsFromChar

                                                      int col = findColumnNumber(mFullName2);
                                                      if (col >= 0) {
                                                          PrimitiveArray pa = getColumn(col);
                                                          pa.append(tpa2);
                                                          pas2.add(pa);
                                                      } else if (loadVarNamesSet == null || loadVarNamesSet.contains(mFullName1)) {
                                                          //add it
                                                          addColumn(nColumns(), mFullName2, tpa2, new Attributes());
                                                          pas2.add(tpa2);
                                                      } else {
                                                          //don't store this member's data
                                                          pas2.add(null);
                                                      }

                                                  } else {
                                                      //subsequent rows: append the data to known pa
                                                      PrimitiveArray pa = pas2.get(memberNum2);
                                                      if (pa != null)
                                                          pa.append(NcHelper.getPrimitiveArray(m2.getDataArray(), false)); //buildStringsFromChar
                                                  }
                                              } //end sdIter2 for loop
                                          } //end seqIter2 while loop
                                      } finally {
                                          seqIter2.close();
                                      }

                                  } else { //m1 isn't a sequence
                                      if (rowNum1 == 0) {
                                          //first row of the seq1
                                          PrimitiveArray tpa1 = NcHelper.getPrimitiveArray(sd1.getArray(m1), false); //buildStringsFromChar

                                          int col = findColumnNumber(mFullName1);
                                          if (col >= 0) {
                                              PrimitiveArray pa = getColumn(col);
                                              pa.append(tpa1);
                                              pas1.add(pa);
                                          } else if (loadVarNamesSet == null || loadVarNamesSet.contains(mFullName1)) {
                                              //add it
                                              addColumn(nColumns(), mFullName1, tpa1, new Attributes());
                                              pas1.add(tpa1);
                                          } else {
                                              //don't store this member's data
                                              pas1.add(null);
                                          }

                                      } else {
                                          //subsequent rows: append the data to known pa
                                          PrimitiveArray pa = pas1.get(memberNum1);
                                          if (pa != null)
                                              pa.append(NcHelper.getPrimitiveArray(m1.getDataArray(), false)); //buildStringsFromChar
                                      }
                                  }
                              } //end sdIter1 for loop
                          } //end seqIter1 while loop
                      } finally {
                          seqIter1.close();
                      }
  */
  /*
                  } else { //outerVar isn't a sequence
                      if (loadVarNamesSet == null || loadVarNamesSet.contains(outerVarName)) {
                          PrimitiveArray tpa = NcHelper.getPrimitiveArray(outerVar.read(), false, NcHelper.isUnsigned(outerVar)); //buildStringsFromChar
                          addColumn(nColumns(), outerVarName, tpa, new Attributes());
                      }
                  }

                  ensureColumnsAreSameSize_LastValue();
                  String2.log(toString());
              }

              if (reallyVerbose)
                  String2.log(msg +
                      " finished. nRows=" + nRows() + " nCols=" + nColumns() +
                      " time=" + (System.currentTimeMillis() - time) + "ms");
          } finally {
              try {if (ncFile != null) ncFile.close(); } catch (Exception e9) {}
          }
      }
  */

  /** Used by readNcCF */
  private int checkConsistent(String errorInMethod, String varName, int oldValue, int newValue) {
    if (oldValue >= 0 && oldValue != newValue)
      throw new SimpleException(
          errorInMethod
              + "The dimensions of variable="
              + varName
              + " are inconsistent with previous variables.");
    return newValue;
  }

  /**
   * This reads and flattens all specified variables from a .nc CF DSG file into a table. <br>
   * This does not unpack the values or convert to standardMissingValues.
   *
   * @param fullName The full name of a local file.
   * @param loadVariableNames if null or length 0, all vars are read. <br>
   *     Any specified name must be the fullName, e.g., group1/var1 <br>
   *     !!!!! conNames MUST be included in loadVariableNames. This code is written to handle them
   *     if not included, but the number of tests to verify is astronomical. !!!!! If
   *     loadVariableNames lists var names, then an error will be thrown if a conName isn't in
   *     loadVariableNames. Thankfully, ERDDAP specified loadVariableNames. !!!!! But if
   *     loadVariableNames aren't listed, THIS METHOD DOESN'T CHECK! <br>
   *     The results will only include loadVariableNames columns, in the specified order. <br>
   *     If a loadVariableName var isn't in the file, there won't be a column in the results file
   *     for it. <br>
   *     Don't include the indexVar or rowSizeVar in loadVariableNames.
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param conNames the source names for the e.g., profile_id, lon, lat, time variables that will
   *     be constrained. (Or null or size()==0 if no constraints.)
   * @param conOps The corresponding operators. Remember that regex constraints will be tested on
   *     the source values!
   * @param conValues The corresponding values.
   * @throws Exception if trouble. No matching data is not an error and returns an empty table (0
   *     rows and 0 columns).
   */
  public void readNcCF(
      String fullName,
      StringArray loadVariableNames,
      int standardizeWhat,
      StringArray conNames,
      StringArray conOps,
      StringArray conValues)
      throws Exception {
    // FUTURE optimization: instead of reading all of 1D obs variables,
    // find first and last set bit in obsKeep, just read a range of values,
    // then apply obsKeep.

    if (loadVariableNames == null) loadVariableNames = new StringArray();
    if (conNames == null) conNames = new StringArray();
    String msg = "  Table.readNcCF " + fullName;
    if (debugMode) msg += "  loadVars: " + loadVariableNames.toString();
    // String2.log("DEBUG:\n" + NcHelper.ncdump(fullName, "-h"));
    long time = System.currentTimeMillis();
    String errorInMethod = String2.ERROR + " in Table.readNcCF " + fullName + ":\n";
    int nCon = conNames.size();
    if (nCon > 0) {
      if (conOps == null || conOps.size() != nCon || conValues == null || conValues.size() != nCon)
        throw new SimpleException(
            errorInMethod
                + "constraints parameters have different sizes: "
                + "conNames=("
                + conNames
                + "), "
                + "conOps=("
                + conOps
                + "), "
                + "conValues=("
                + conValues
                + ").");
      if (debugMode)
        msg +=
            "  Debug: "
                + nCon
                + " constraints: "
                + conNames
                + " "
                + conOps
                + " "
                + conValues
                + "\n";
    } else {
      if (debugMode) msg += "  Debug: 0 constraints\n";
    }
    if (ncCFcc != null) ncCFcc.set(0);

    // clear the table
    clear();

    // if loadVariableNames was specified,
    if (loadVariableNames.size() > 0) {
      // ENSURE all conNames are in loadVariableNames
      HashSet<String> loadVarHS = loadVariableNames.toHashSet();
      for (int c = 0; c < conNames.size(); c++) {
        if (!loadVarHS.contains(conNames.get(c)))
          throw new RuntimeException(
              errorInMethod
                  + "All constraint varNames must be in loadVariableNames. \""
                  + conNames.get(c)
                  + "\" isn't in "
                  + loadVariableNames.toString()
                  + ".");
      }
    }

    Attributes gridMappingAtts = null;
    String readAs = null;
    NetcdfFile ncFile = NcHelper.openFile(fullName);
    try {
      /*
      //2012-07 CURRENTLY THE NETCDF-JAVA featureDataset APPROACH ISN'T WORKING.
      //I EMAILED JOHN CARON.
      //Approach: Rely on netcdf-java FeatureDataset to "understand", read,
      //and flatten the data.
      ncDataset = new NetcdfDataset(ncFile, false);  //enhance
      Formatter formatter = new Formatter();
      PointDatasetStandardFactory pdsFactory = new PointDatasetStandardFactory();
      Object analyser = pdsFactory.isMine(
          FeatureType.ANY_POINT, ncDataset, formatter); //throws IOException if trouble
      fDataset = pdsFactory.open(FeatureType.ANY_POINT, ncDataset, analyser, null, formatter);
      //...

      //I do care if this throws exception
      fDataset.close();
      */

      // My approach: low level.  I parse and analyze the files.

      // Get featureType from globalAttributes.
      // Match it to values in CF standard table 9.1.

      Group rootGroup = ncFile.getRootGroup();
      NcHelper.getGroupAttributes(rootGroup, globalAttributes());
      String featureType = globalAttributes().getString("featureType");
      if (featureType == null) // cdm allows these aliases
      featureType = globalAttributes().getString("CF:featureType");
      if (featureType == null) featureType = globalAttributes().getString("CF:feature_type");
      featureType = featureType == null ? "null" : featureType.toLowerCase(); // case insensitive
      boolean pointType = featureType.equals("point");
      boolean profileType = featureType.equals("profile");
      boolean timeSeriesType = featureType.equals("timeseries");
      boolean trajectoryType = featureType.equals("trajectory");
      boolean timeSeriesProfileType = featureType.equals("timeseriesprofile");
      boolean trajectoryProfileType = featureType.equals("trajectoryprofile");
      int nLevels =
          pointType
              ? 0
              : profileType || timeSeriesType || trajectoryType
                  ? 1
                  : timeSeriesProfileType || trajectoryProfileType ? 2 : -1;
      if (nLevels == -1)
        throw new SimpleException(
            errorInMethod
                + "featureType="
                + featureType
                + " isn't a valid CF featureType."
                + String2.annotatedString(featureType));

      // make ERDDAP-preferred capitalization, e.g., cdm_data_type = TimeSeriesProfile
      String cdmName = Character.toUpperCase(featureType.charAt(0)) + featureType.substring(1);
      cdmName = String2.replaceAll(cdmName, "series", "Series");
      cdmName = String2.replaceAll(cdmName, "profile", "Profile");
      globalAttributes.set("cdm_data_type", cdmName);

      // make ERDDAP-style cdm_..._variables
      String cdmOuterName =
          "cdm_"
              + (timeSeriesProfileType || trajectoryProfileType
                  ? featureType.substring(0, featureType.length() - 7)
                  : featureType)
              + "_variables";
      String cdmInnerName =
          timeSeriesProfileType || trajectoryProfileType ? "cdm_profile_variables" : null;

      // deal with pointType
      if (pointType) {
        if (ncCFcc != null) ncCFcc.set(1);
        ncFile.close();
        if (debugMode) msg += "PointType.  loadVars=" + loadVariableNames + "\n";
        StringArray loadCon = new StringArray(loadVariableNames);
        if (loadCon.size() > 0) // if loadVars specified, then add conNames
        loadCon.append(conNames);
        // readNDNc always includes scalar vars
        readNDNc(
            fullName,
            loadCon.toHashSet().toArray(new String[0]),
            0, // standardizeWhat=0
            null,
            0,
            0);

        // finish up
        tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
        if (nRows() == 0) removeAllColumns();
        else if (loadVariableNames.size() > 0)
          reorderColumns(loadVariableNames, true); // discard others
        if (reallyVerbose)
          msg +=
              " finished (nLevels=0, pointType)."
                  + " nRows="
                  + nRows()
                  + " nCols="
                  + nColumns()
                  + " time="
                  + (System.currentTimeMillis() - time)
                  + "ms";
        if (debugMode) ensureValid();
        decodeCharsAndStrings();
        convertToUnsignedPAs();
        return;
      }

      // if loadVariableNames was specified,
      if (loadVariableNames.size() > 0) {
        // remove any vars not in this file
        BitSet keepV = new BitSet();
        for (int v = 0; v < loadVariableNames.size(); v++)
          keepV.set(v, ncFile.findVariable(loadVariableNames.get(v)) != null);
        if (keepV.cardinality() == 0) {
          msg +=
              "\nNone of the loadVariableNames are in this file: "
                  + loadVariableNames.toString()
                  + ".";
          return; // return an empty table
        }
        loadVariableNames.justKeep(keepV);
      }

      // find all dimensions
      List dimsList = rootGroup.getDimensions();
      int nDims = dimsList.size();
      String dimNames[] = new String[nDims];
      for (int d = 0; d < nDims; d++) {
        dimNames[d] = ((Dimension) dimsList.get(d)).getName(); // may be null
        if (dimNames[d] == null) dimNames[d] = "";
      }
      // outerDim and obsDim are always used.  innerDim only used for nLevels=2
      int outerDim = -1, innerDim = -1, obsDim = -1;

      // find out about all vars
      List varsList = ncFile.getVariables(); // all vars in all groups
      int nVars = varsList.size();
      Variable vars[] = new Variable[nVars];
      String varNames[] = new String[nVars];
      boolean varInLoadOrConVariables[] = new boolean[nVars];
      boolean varIsChar[] = new boolean[nVars];
      int varNDims[] = new int[nVars]; // not counting nchars dimension
      boolean varUsesDim[][] = new boolean[nVars][nDims + 1]; // all are false  (+1 for scalarDim)
      // pseudo dimension for scalar variables (used in level 2 ragged files if indexVar is missing)
      int scalarDim = nDims;
      boolean hasScalarVars = false;
      Attributes varAtts[] = new Attributes[nVars];
      int rowSizeVar = -1; // e.g., in level 1 contiguous and level 2 ragged files
      boolean rowSizeVarIsRequired = false; // is current rowSizeVar required for a loadVar?
      int indexVar = -1; // e.g., in level 1 indexed and most level 2 ragged files
      boolean loadVariableNamesWasEmpty = loadVariableNames.size() == 0;
      int nLoadOrConVariablesInFile = 0;
      String firstSampleDimName = null;
      // if (debugMode) String2.log("Debug: nVars=" + nVars);
      for (int v = 0; v < nVars; v++) {
        if (ncCFcc != null) ncCFcc.set(2);
        vars[v] = (Variable) varsList.get(v);
        varNames[v] = vars[v].getFullName();
        varIsChar[v] = vars[v].getDataType() == DataType.CHAR;
        int rank = vars[v].getRank();
        varNDims[v] = rank - (varIsChar[v] ? 1 : 0);
        varAtts[v] = new Attributes();
        NcHelper.getVariableAttributes(vars[v], varAtts[v]);
        // String2.log("Debug: loadVarNames.see v[" + v + "]=" + varNames[v] + "  nNonCharDims=" +
        // varNDims[v]);
        // 2016-06-07 scalar continue was here

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null) {
          gridMappingAtts =
              NcHelper.getGridMappingAtts(ncFile, varAtts[v].getString("grid_mapping"));
          if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);
        }

        // add to nLoadOrConVariablesInFile
        // !!!Note that I don't detect vars with unexpected dimensions
        // partly because file type isn't known at this point.
        // But var dimensions are always checked when the var is read,
        // so other vars will simply be skipped. (Better if warning/error.)
        if (loadVariableNamesWasEmpty) {
          // String2.log("Debug: loadVarNames.add v[" + v + "]=" + varNames[v]);
          loadVariableNames.add(varNames[v]);
          varInLoadOrConVariables[v] = true;
        } else {
          varInLoadOrConVariables[v] =
              loadVariableNames.indexOf(varNames[v]) >= 0 || conNames.indexOf(varNames[v]) >= 0;
        }
        if (varInLoadOrConVariables[v]) nLoadOrConVariablesInFile++;

        // scalars
        if (varNDims[v] <= 0) {
          if (ncCFcc != null) ncCFcc.set(3);
          hasScalarVars = true;
          varNDims[v] = 1;
          varUsesDim[v][scalarDim] = true;
          continue;
        }

        // go through the dimensions
        for (int d = 0; d < varNDims[v]; d++) {
          if (ncCFcc != null) ncCFcc.set(4);
          int whichDim = dimsList.indexOf(vars[v].getDimension(d));
          if (whichDim >= 0) { // a shared dim
            if (ncCFcc != null) ncCFcc.set(12);
            varUsesDim[v][whichDim] = true;
          }

          // detect multiDim dimensions
          if (nLevels == 1 && varNDims[v] == 2) {
            if (ncCFcc != null) ncCFcc.set(10);
            if (d == 0) {
              if (ncCFcc != null) ncCFcc.set(5);
              outerDim = checkConsistent(errorInMethod, varNames[v], outerDim, whichDim);
            }
            if (d == 1) {
              if (ncCFcc != null) ncCFcc.set(6);
              obsDim = checkConsistent(errorInMethod, varNames[v], obsDim, whichDim);
            }
          } else if (nLevels == 2 && varNDims[v] == 3) {
            if (ncCFcc != null) ncCFcc.set(11);
            if (d == 0) {
              if (ncCFcc != null) ncCFcc.set(7);
              outerDim = checkConsistent(errorInMethod, varNames[v], outerDim, whichDim);
            }
            if (d == 1) {
              if (ncCFcc != null) ncCFcc.set(8);
              innerDim = checkConsistent(errorInMethod, varNames[v], innerDim, whichDim);
            }
            if (d == 2) {
              if (ncCFcc != null) ncCFcc.set(9);
              obsDim = checkConsistent(errorInMethod, varNames[v], obsDim, whichDim);
            }
          }
        }

        // isContiguous? look for rowSizeVar with sample_dimension attribute
        // If file has multiple sample_dimension's,
        //  use the one used by the loadVars (if specified),
        //  else use the first one found (and skip the others).
        //  See testReadNcCF7SampleDims()
        String sd = varAtts[v].getString("sample_dimension");
        // if (firstSampleDimName == null)
        //    firstSampleDimName = sd;

        // determine if this particular sample_dimension isRequired for a loadVar
        boolean isRequired = false;
        if (sd != null && !loadVariableNamesWasEmpty) { // loadVars was specified
          // if already have one, don't keepGoing/ skip this one
          if (debugMode) msg += "\nDebug:          sample_dimension=" + sd;
          // is this sample_dimension used (and thus, required) by any of the loadVars?
          for (int lvi = 0; lvi < loadVariableNames.size(); lvi++) {
            Variable var = ncFile.findVariable(loadVariableNames.get(lvi)); // won't be null
            if (var.getRank() > 0 && sd.equals(var.getDimension(0).getName())) {
              if (debugMode)
                msg +=
                    "\nDebug: sample_dimension="
                        + sd
                        + " isRequired by loadVar="
                        + loadVariableNames.get(lvi);
              isRequired = true;
              break; // lvi
            }
          }
        }

        if (rowSizeVarIsRequired && isRequired) {
          // 2 different sample_dimensions are required by loadVariableNames!
          throw new SimpleException(
              errorInMethod
                  + "Invalid request: loadVariables includes variables "
                  + "that use two different sample_dimension's ("
                  + (obsDim >= 0 ? dimNames[obsDim] : "null")
                  + // null shouldn't happen
                  " and "
                  + sd
                  + ").");
        } else if (sd != null && !isRequired && rowSizeVar >= 0) {
          // skip this sample_dimension
          if (debugMode) msg += "\nDebug: skipping sample_dimension=" + sd;
          sd = null;

        } else if (sd != null && (isRequired || rowSizeVar < 0)) {
          // keep this sample_dimension info  (If !isRequired, then may be temporary.)
          if (debugMode) msg += "\nDebug:  keeping sample_dimension=" + sd;

          if (ncCFcc != null) ncCFcc.set(13);
          rowSizeVar = v;
          rowSizeVarIsRequired = isRequired;
          // this is an internal variable. Request can't include it or constrain it.
          if (varInLoadOrConVariables[v]) {
            varInLoadOrConVariables[v] = false;
            nLoadOrConVariablesInFile--;
            int i = loadVariableNames.indexOf(varNames[v]);
            if (i >= 0) loadVariableNames.remove(i);
            i = conNames.indexOf(varNames[v]);
            if (i >= 0) {
              conNames.remove(i);
              conOps.remove(i);
              conValues.remove(i);
            }
          }

          // sample_dimension
          obsDim = String2.indexOf(dimNames, sd);
          if (obsDim < 0)
            throw new SimpleException(
                errorInMethod
                    + "Invalid file: file says sample_dimension is "
                    + sd
                    + ", but there is no dimension with that name.");

          // outerDim or innerDim
          int whichDim = dimsList.indexOf(vars[v].getDimension(0));
          if (whichDim < 0)
            throw new SimpleException(
                errorInMethod
                    + "variable="
                    + varNames[v]
                    + "'s dimension #0 isn't a shared dimension.");
          if (nLevels == 1) outerDim = whichDim;
          else innerDim = whichDim; // nLevels == 2
        }

        // isIndexed? look for indexVar with instance_dimension attribute
        String id = varAtts[v].getString("instance_dimension");
        if (id != null) {
          if (indexVar >= 0)
            throw new SimpleException(
                errorInMethod
                    + "Invalid file: two variables ("
                    + varNames[indexVar]
                    + " and "
                    + varNames[v]
                    + ") have an instance_dimension attribute.");
          // this is an internal variable in the file. Request can't request it or constrain it.
          if (ncCFcc != null) ncCFcc.set(14);
          indexVar = v;
          if (varInLoadOrConVariables[v]) {
            if (ncCFcc != null) ncCFcc.set(15);
            varInLoadOrConVariables[v] = false;
            nLoadOrConVariablesInFile--;
            int i = loadVariableNames.indexOf(varNames[v]);
            if (i >= 0) loadVariableNames.remove(i);
            i = conNames.indexOf(varNames[v]);
            if (i >= 0) {
              conNames.remove(i);
              conOps.remove(i);
              conValues.remove(i);
            }
          }

          // intance_dimension
          outerDim = String2.indexOf(dimNames, id);
          if (outerDim < 0)
            throw new SimpleException(
                errorInMethod
                    + "Invalid file: file says instance_dimension is "
                    + id
                    + ", but there is no dimension with that name.");

          // its dim
          int whichDim = dimsList.indexOf(vars[v].getDimension(0));
          if (whichDim < 0)
            throw new SimpleException(
                errorInMethod
                    + "variable="
                    + varNames[v]
                    + "'s dimension #0 isn't a shared dimension.");
          if (nLevels == 1) obsDim = whichDim;
          else innerDim = whichDim; // nLevels == 2
        }
      }

      if (debugMode && loadVariableNamesWasEmpty)
        msg += "\n  Debug: #1 loadVars (was empty): " + loadVariableNames.toString();

      Dimension outerDimDim = outerDim < 0 ? null : (Dimension) dimsList.get(outerDim);
      Dimension innerDimDim = innerDim < 0 ? null : (Dimension) dimsList.get(innerDim);
      Dimension obsDimDim = obsDim < 0 ? null : (Dimension) dimsList.get(obsDim);
      String outerDimName = outerDim < 0 ? "" : outerDimDim.getName(); // may be null
      String innerDimName = innerDim < 0 ? "" : innerDimDim.getName();
      String obsDimName = obsDim < 0 ? "" : obsDimDim.getName();
      String scalarDimName = "scalar";
      int outerDimSize = outerDimDim == null ? -1 : outerDimDim.getLength();
      int innerDimSize = innerDimDim == null ? -1 : innerDimDim.getLength();
      int obsDimSize = obsDimDim == null ? -1 : obsDimDim.getLength();
      int scalarDimSize = 1;

      // if outerDim not found, try using scalarDim (scalar vars)
      if (outerDim == -1 && hasScalarVars) {
        if (ncCFcc != null) { // some things removed, so 3 flags are set
          ncCFcc.set(16);
          ncCFcc.set(17);
          ncCFcc.set(18);
        }
        outerDim = scalarDim;
        outerDimName = scalarDimName;
        outerDimSize = scalarDimSize;
      }

      // Deal with nLevels=1 or 2, outerDim=scalarDim:
      //  find obsDim (and innerDim for nLevels=2)
      if (rowSizeVar < 0 && indexVar < 0 && outerDim == scalarDim && innerDim < 0 && obsDim < 0) {

        // if nLevels=1, read via readNDNc
        if (ncCFcc != null) ncCFcc.set(19);
        if (nLevels == 1) {
          if (debugMode) String2.log("  Debug: nLevels=1, outerDim=scalarDim, read via readNDNc");
          if (ncCFcc != null) ncCFcc.set(20);
          ncFile.close();
          StringArray loadCon = new StringArray(loadVariableNames);
          if (loadCon.size() > 0) // if loadVars specified, then add conNames
          loadCon.append(conNames);
          // readNDNc always includes scalar vars
          readNDNc(
              fullName,
              loadCon.toHashSet().toArray(new String[0]),
              0, // standardizeWhat=0
              null,
              0,
              0);

          // finish up
          tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
          if (nRows() == 0) removeAllColumns();
          else reorderColumns(loadVariableNames, true); // discard others
          if (reallyVerbose)
            String2.log(
                "  readNcCF finished (nLevels=1, readNDNc). "
                    + " fileName="
                    + fullName
                    + " nRows="
                    + nRows()
                    + " nCols="
                    + nColumns()
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          if (debugMode) ensureValid();
          decodeCharsAndStrings();
          convertToUnsignedPAs();
          return;
        }

        // if nLevels == 2
        if (debugMode) String2.log("  Debug: nLevels=2, outerDim=scalarDim");
        for (int v = 0; v < nVars; v++) {
          // first: go through the dimensions
          if (ncCFcc != null) ncCFcc.set(21);
          for (int d = 0; d < varNDims[v]; d++) {
            if (ncCFcc != null) ncCFcc.set(22);
            int whichDim = dimsList.indexOf(vars[v].getDimension(d));
            if (whichDim >= 0) { // not scalarDim
              if (varNDims[v] == 2) { // var[innerDim][obsDim]
                if (ncCFcc != null) ncCFcc.set(23);
                if (d == 0) {
                  if (ncCFcc != null) ncCFcc.set(24);
                  innerDim = checkConsistent(errorInMethod, varNames[v], innerDim, whichDim);
                }
                if (d == 1) {
                  if (ncCFcc != null) ncCFcc.set(25);
                  obsDim = checkConsistent(errorInMethod, varNames[v], obsDim, whichDim);
                }
              }
            }
          }

          // second: trick code below into adding outerDim=scalarDim to all vars
          //  (scalar vars already have it)(vars using other dims don't)
          if (!varUsesDim[v][scalarDim]) {
            if (ncCFcc != null) ncCFcc.set(26);
            varUsesDim[v][scalarDim] = true;
            varNDims[v]++;
          }
        }

        if (innerDim < 0 || outerDim < 0)
          throw new SimpleException(
              errorInMethod
                  + "Invalid file: nLevels=2, outerDim=scalarDim, but can't find "
                  + "variable[innerDim][obsDim].  innerDim="
                  + innerDim
                  + " obsDim="
                  + obsDim);
        obsDimDim = (Dimension) dimsList.get(obsDim);
        obsDimName = obsDimDim.getName();
        obsDimSize = obsDimDim.getLength();
        innerDimDim = (Dimension) dimsList.get(innerDim);
        innerDimName = innerDimDim.getName();
        innerDimSize = innerDimDim.getLength();

        // now that innerDim and outerDim are known, find 1D vars that use them
        if (loadVariableNamesWasEmpty) {
          if (debugMode) String2.log("  Debug: find 1D vars that use inner or outerDim");
          for (int v = 0; v < nVars; v++) {
            // first: go through the dimensions
            if (varNDims[v] != 1) continue;
            int whichDim = dimsList.indexOf(vars[v].getDimension(0));
            if (whichDim == innerDim || whichDim == outerDim) { // not scalarDim
              if (loadVariableNames.indexOf(varNames[v]) < 0) {
                loadVariableNames.add(varNames[v]);
                if (!varInLoadOrConVariables[v]) {
                  varInLoadOrConVariables[v] = true;
                  nLoadOrConVariablesInFile++;
                }
              }

              // second: trick code below into adding outerDim=scalarDim to all vars
              //  (scalar vars already have it)(vars using other dims don't)
              if (!varUsesDim[v][scalarDim]) {
                varUsesDim[v][scalarDim] = true;
                varNDims[v]++;
              }
            }
          }
        }
      }

      if (debugMode) {
        if (loadVariableNamesWasEmpty)
          String2.log("  Debug: #2 loadVars (was empty): " + loadVariableNames.toString());
        String2.log(
            "  Debug: nTotalVarsInFile="
                + nVars
                + " nLoadOrConVarsInFile="
                + nLoadOrConVariablesInFile
                + " vars: rowSize="
                + (rowSizeVar < 0 ? "" : varNames[rowSizeVar])
                + " index="
                + (indexVar < 0 ? "" : varNames[indexVar])
                + "\n"
                + "    dims: outer="
                + outerDimName
                + "["
                + outerDimSize
                + "]"
                + " inner="
                + innerDimName
                + "["
                + innerDimSize
                + "]"
                + " obs="
                + obsDimName
                + "["
                + obsDimSize
                + "]");
      }
      if (ncCFcc != null) ncCFcc.set(27);
      if (nLoadOrConVariablesInFile == 0) {
        if (verbose)
          String2.log(
              "  readNcCF fileName="
                  + fullName
                  + ": "
                  + MustBe.THERE_IS_NO_DATA
                  + " (no requested vars in the file)"
                  + " time="
                  + (System.currentTimeMillis() - time)
                  + "ms");
        removeAllColumns();
        return;
      }

      // ensure file is valid
      String notFound = null;
      if (outerDim == -1) notFound = "outer";
      else if (nLevels == 2 && innerDim == -1) notFound = "inner";
      else if (obsDim == -1) notFound = "observation";
      if (notFound != null)
        throw new SimpleException(
            errorInMethod + "Invalid file: Unable to find the " + notFound + " dimension.");
      if (nLevels == 1 && rowSizeVar >= 0 && indexVar >= 0)
        throw new SimpleException(
            errorInMethod
                + "Invalid file: the file should have a variable with instance_dimension "
                + "or a variable with sample_dimension, not both.");
      boolean multidimensional = indexVar < 0 && rowSizeVar < 0;

      // If (constraintVar!="" or constraintVar=(non-Number)) and var not in file,
      //  we know there can be no matching data in this file.
      // I would like to also reject if constraintVar=(finiteNumber), but it is
      //  complicated because mv's are usually represented as finite values.
      //  so a missing var might get converted to all -999.
      for (int con = 0; con < nCon; con++) {
        // if conName is in the file, can't quick reject, so continue;
        if (ncCFcc != null) ncCFcc.set(28);
        String tConName = conNames.get(con);
        int v = String2.indexOf(varNames, tConName);
        if (v >= 0) {
          if (ncCFcc != null) ncCFcc.set(29);
          continue;
        }

        // There is no way to tell if a var not in the file is String or numeric.
        String tConOp = conOps.get(con);
        String tConValue = conValues.get(con);
        boolean rejectFile = false;

        // test e.g.,  station!=""         (NaN would be NaN)
        if ("!=".equals(tConOp)) {
          if ("".equals(tConValue)) rejectFile = true;

          // test e.g.,  station=WXUSP      (if numeric, hard to test)
        } else if ("=".equals(tConOp)) {
          if (tConValue.length() > 0 && !String2.isNumber(tConValue)) rejectFile = true;
        }

        if (rejectFile) {
          if (ncCFcc != null) ncCFcc.set(30);
          if (verbose)
            String2.log(
                "  readNcCF "
                    + MustBe.THERE_IS_NO_DATA
                    + " fileName="
                    + fullName
                    + " (var not in file: "
                    + tConName
                    + tConOp
                    + tConValue
                    + ")"
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          removeAllColumns();
          return;
        }
      }

      // *** read outerDim variables into a table: all nLevels, all featureTypes
      if (debugMode)
        String2.log("  Debug: read outerDim variables into a table: all nLevels, all featureTypes");
      Table outerTable = new Table();
      int nLoadOrConVariablesInOuterTable = 0;
      StringArray subsetVars = new StringArray();
      for (int v = 0; v < nVars; v++) {
        // normally, get just varInLoadOrConVariables vars
        // but if multidimensional, get ALL outerDim vars !!!
        // if (debugMode) String2.log("  Debug: read outerTable v=" + varNames[v] +
        //    " inLoadOrConVars=" + varInLoadOrConVariables[v] +
        //    " varNDims[v]=" + varNDims[v] +
        //    " varUsesDim[v][outerDim]=" + varUsesDim[v][outerDim]);
        if (ncCFcc != null) ncCFcc.set(31);
        if ((varInLoadOrConVariables[v] || multidimensional)
            && varNDims[v] == 1
            && varUsesDim[v][outerDim]) { // ensure correct dim
          if (ncCFcc != null) ncCFcc.set(32);
          if (varInLoadOrConVariables[v]) {
            if (ncCFcc != null) ncCFcc.set(33);
            nLoadOrConVariablesInOuterTable++;
            if (varAtts[v].get("instance_dimension") == null)
              // don't include if dimension, since var isn't in results table
              subsetVars.add(varNames[v]); // so in file's order; that's consistent; that's good
          }
          outerTable.addColumn(
              outerTable.nColumns(), varNames[v], NcHelper.getPrimitiveArray(vars[v]), varAtts[v]);
          outerTable.standardizeLastColumn(standardizeWhat);
        }
      }

      // add scalar vars 2016-06-06
      int ttNRows = Math.max(1, outerTable.nRows());
      for (int v = 0; v < nVars; v++) {
        if (varNDims[v] == 1
            && varUsesDim[v][scalarDim]
            && // scalars are stored with odd info
            (loadVariableNamesWasEmpty || varInLoadOrConVariables[v])
            && outerTable.findColumnNumber(varNames[v]) < 0) { // not already in table
          nLoadOrConVariablesInOuterTable++;
          if (varAtts[v].get("instance_dimension") == null)
            // don't include if dimension, since var isn't in results table
            subsetVars.add(varNames[v]); // so in file's order; that's consistent; that's good
          PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
          // make doubly sure it is 1 element
          if (pa.size() == 0) pa.addString("");
          else pa.removeRange(1, pa.size());
          pa =
              varAtts[v].standardizeVariable(
                  standardizeWhat, varNames[v], pa); // before duplicating the strings
          // duplicate the scalar to nRows
          pa.addNStrings(ttNRows - 1, pa.getString(0));
          outerTable.addColumn(outerTable.nColumns(), varNames[v], pa, varAtts[v]);
        }
      }

      if (debugMode)
        String2.log(
            "  Debug: outerTable(nRows="
                + outerTable.nRows()
                + ") nLoadOrConVariablesInOuterTable="
                + nLoadOrConVariablesInOuterTable
                + " First <=3 rows:\n"
                + outerTable.dataToString(3));
      globalAttributes.set(cdmOuterName, subsetVars.toString()); // may be "", that's okay
      if (cdmInnerName != null)
        globalAttributes.set(cdmInnerName, ""); // nLevel=2 will set it properly below
      globalAttributes.set(
          "subsetVariables", subsetVars.toString()); // nLevel=2 will set it properly below

      // apply constraints  (if there is data)
      BitSet outerKeep = null; // implies outerTable.nColumns = 0, so assume all are good
      int outerNGood = -1; // implies not tested, so assume all are good
      if (outerTable.nColumns() > 0) { // it will be for multidimensional

        if (ncCFcc != null) ncCFcc.set(34);
        if (multidimensional) {
          if (ncCFcc != null) ncCFcc.set(35);
          outerKeep = outerTable.rowsWithData();
        } else {
          if (ncCFcc != null) ncCFcc.set(36);
          outerKeep = new BitSet();
          outerKeep.set(0, outerTable.nRows());
        }

        // apply user constraints
        outerNGood = outerTable.tryToApplyConstraints(-1, conNames, conOps, conValues, outerKeep);
        if (outerNGood == 0) {
          if (ncCFcc != null) ncCFcc.set(37);
          if (verbose)
            String2.log(
                "  readNcCF "
                    + MustBe.THERE_IS_NO_DATA
                    + " fileName="
                    + fullName
                    + " (outerNGood=0)"
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          removeAllColumns();
          return;
        }
        // order of rows is important, so *don't* justKeep(outerKeep)

        // Are we done? Are those all the variables we need that are in the file?
        if (debugMode)
          String2.log(
              "Debug: nLoadOrCon inFile="
                  + nLoadOrConVariablesInFile
                  + " inOuterTable="
                  + nLoadOrConVariablesInOuterTable);
        if (nLoadOrConVariablesInFile == nLoadOrConVariablesInOuterTable) {
          outerTable.justKeep(outerKeep);
          // globalAttributes already set
          // copy outerTable to this table
          if (ncCFcc != null) ncCFcc.set(38);
          int noc = outerTable.nColumns();
          for (int c = 0; c < noc; c++)
            addColumn(
                c,
                outerTable.getColumnName(c),
                outerTable.getColumn(c),
                outerTable.columnAttributes(c));

          // finish up
          tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
          if (nRows() == 0) removeAllColumns();
          else reorderColumns(loadVariableNames, true); // discard others
          if (reallyVerbose)
            String2.log(
                "  readNcCF finished (nLevels=1, outerTable vars only)."
                    + " fileName="
                    + fullName
                    + " nRows="
                    + nRows()
                    + " nCols="
                    + nColumns()
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          if (debugMode) ensureValid();
          decodeCharsAndStrings();
          convertToUnsignedPAs();
          return;
        }
      } // else if no outerTable columns, all outerTable features are considered good
      int outerTableNColumns = outerTable.nColumns();
      int outerTableNRows = outerTable.nRows();

      if (debugMode)
        String2.log(
            "  Debug: outerTable has nCols="
                + outerTableNColumns
                + " nRows="
                + outerTableNRows
                + " nKeepRows="
                + outerNGood
                + (outerTableNRows == 0 ? "" : "\n" + outerTable.dataToString(5)));

      // *** read nLevels=1 obs data
      if (nLevels == 1) {
        if (debugMode) String2.log("  Debug: read nLevels=1 obs data");

        // request is for obs vars only (not feature data), so read all of the data
        if (ncCFcc != null) ncCFcc.set(39);
        if (outerTableNColumns == 0 && !multidimensional) { // and so outerKeep=null
          if (debugMode)
            String2.log("  Debug: obs vars only (not feature data), so read all of the data");
          readAs = "obs vars only";
          if (ncCFcc != null) ncCFcc.set(40);
          for (int v = 0; v < nVars; v++) {
            if (varInLoadOrConVariables[v]) {
              if (ncCFcc != null) ncCFcc.set(41);
              int dim0 = dimsList.indexOf(vars[v].getDimension(0));
              if (dim0 == obsDim) { // ensure correct dim.  obsDim can't be scalardim
                PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
                addColumn(nColumns(), varNames[v], pa, varAtts[v]);
                standardizeLastColumn(standardizeWhat);
              } else {
                if (verbose)
                  String2.log(
                      "  !!! nLevels=1 readAs="
                          + readAs
                          + ": Unexpected dimension for "
                          + varNames[v]);
              }
            }
          }

          // read nLevels=1 indexed, contiguous, multi-dimensional obs variables
        } else {

          // nLevels=1 indexed ragged array
          if (ncCFcc != null) ncCFcc.set(42);
          if (indexVar >= 0) {
            if (debugMode) String2.log("  Debug: read nLevels=1 indexed ragged");
            readAs = "indexed ragged";
            if (ncCFcc != null) ncCFcc.set(43);

            // insert the indexVar (which is the keyColumn) at col=0
            PrimitiveArray indexVarPA = NcHelper.getPrimitiveArray(vars[indexVar]);
            addColumn(0, varNames[indexVar], indexVarPA, varAtts[indexVar]);
            standardizeColumn(standardizeWhat, 0);
            int indexMV = varAtts[indexVar].getInt("missing_value"); // MAX_VALUE if not defined
            int indexFV = varAtts[indexVar].getInt("_FillValue"); // MAX_VALUE if not defined

            // make obsKeep
            // obsKeep Approach: indexVar makes this the only reasonable approach.
            int tnRows = indexVarPA.size();
            BitSet obsKeep = new BitSet(tnRows); // all are false
            for (int row = 0; row < tnRows; row++) {
              // Index should be 0..n-1.
              // Files with index 1..n shouldn't be a silent failure,
              // since all index 1..n-1 will be used INCORRECTLY and
              // thus return incorrect results.
              // So check for this
              int index = indexVarPA.getInt(row);
              if (index >= 0 && index < outerTableNRows) {
                if (outerKeep.get(index)) // outerKeep=null handled above
                obsKeep.set(row);
              } else if (index == indexMV || index == indexFV) {
                // that's the right way to reserve space
              } else {
                throw new SimpleException(
                    errorInMethod
                        + "Invalid file: The index values must be 0 - "
                        + (outerTableNRows - 1)
                        + ", but "
                        + varNames[indexVar]
                        + "["
                        + row
                        + "]="
                        + index
                        + ".");
              }
            }

            indexVarPA.justKeep(obsKeep);
            indexVarPA.trimToSize();
            if (debugMode)
              String2.log("  Debug: nObsRows=" + tnRows + " nObsKeep=" + indexVarPA.size());

            // read all of requested variable[obs]
            // With indexed, we have to read entire var then apply obsKeep.
            for (int v = 0; v < nVars; v++) {
              if (varInLoadOrConVariables[v]
                  && varNDims[v] == 1
                  && varUsesDim[v][obsDim]) { // ensure correct dim
                PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
                pa.justKeep(obsKeep); // as each var read in, to save memory
                pa.trimToSize();
                addColumn(nColumns(), varNames[v], pa, varAtts[v]);
                standardizeLastColumn(standardizeWhat);
              }
            }

            // nLevels=1 contiguous ragged array     //read data for keep=true features
          } else if (rowSizeVar >= 0) {
            if (debugMode) String2.log("  Debug: nLevels=1 contiguous ragged array");
            readAs = "contiguous ragged";
            if (ncCFcc != null) ncCFcc.set(44);

            // read the rowSizesPA
            PrimitiveArray rowSizesPA = NcHelper.getPrimitiveArray(vars[rowSizeVar]);
            // it's row sizes, so no need to unpack
            int rowSizesMV = varAtts[rowSizeVar].getInt("missing_value");
            int rowSizesFV = varAtts[rowSizeVar].getInt("_FillValue");
            boolean hasMVFV = rowSizesMV != Integer.MAX_VALUE || rowSizesFV != Integer.MAX_VALUE;

            // make keyColumn (with row#'s in outerTable) and obsKeep
            // obsKeep Approach: optimize for situation that takes longest
            //  (outerKeep all true).  This is also a very simple approach.
            // This table is currently empty.
            IntArray keyColumnPA = new IntArray();
            addColumn(0, "keyColumn", keyColumnPA, new Attributes());
            int startRow, endRow = 0; // endRow is exclusive
            BitSet obsKeep = new BitSet(obsDimSize); // all are false
            String firstRowWithMVFV = null; // Missing Value or Fill Value
            boolean warningWritten = false;
            for (int outerRow = 0; outerRow < outerTableNRows; outerRow++) {
              startRow = endRow;
              int getNRows = rowSizesPA.getInt(outerRow);
              if (hasMVFV) {
                if (getNRows == rowSizesMV) {
                  // They should be 0's. Treat as 0.
                  if (firstRowWithMVFV == null)
                    firstRowWithMVFV =
                        "The rowSizes variable ("
                            + varNames[rowSizeVar]
                            + ") has a missing_value ["
                            + outerRow
                            + "]="
                            + rowSizesMV;
                  continue;
                } else if (getNRows == rowSizesFV) {
                  // They should be 0's. Treat as 0.
                  if (firstRowWithMVFV == null)
                    firstRowWithMVFV =
                        "The rowSizes variable ("
                            + varNames[rowSizeVar]
                            + ") has a _FillValue ["
                            + outerRow
                            + "]="
                            + rowSizesFV;
                  continue;
                } else if (firstRowWithMVFV != null) {
                  // mvfv already observed and this value isn't an mv or fv!
                  // So the previous mvfv wasn't at the end!
                  if (!warningWritten) {
                    String2.log(
                        "WARNING for contiguous ragged .nc CF file: "
                            + firstRowWithMVFV
                            + " and then a valid value ["
                            + outerRow
                            + "]="
                            + getNRows
                            + "!  ERDDAP interprets missing_values and _FillValues as 0, "
                            + "which is what the file should have, not missing_values or _FillValues!");
                    warningWritten = true;
                  }
                }
              }
              endRow += getNRows;
              if (outerKeep.get(outerRow)) { // outerKeep=null handled above
                keyColumnPA.addNInts(getNRows, outerRow); // so obsKeep already applied
                obsKeep.set(startRow, endRow);
              }
            }
            if (endRow < obsDimSize) {
              String2.log(
                  "WARNING for contiguous ragged file: "
                      + "The sum of the values in the rowSizes variable ("
                      + varNames[rowSizeVar]
                      + " sum="
                      + endRow
                      + ") is less than the size of the observationDimension ("
                      + obsDimName
                      + " size="
                      + obsDimSize
                      + ").\n"
                      + "I hope that is just unused extra space for future observations!");
            } else if (endRow > obsDimSize) {
              throw new SimpleException(
                  errorInMethod
                      + "Invalid contiguous ragged file: The sum of the values in the rowSizes variable ("
                      + varNames[rowSizeVar]
                      + " sum="
                      + endRow
                      + ") is greater than the size of the observationDimension ("
                      + obsDimName
                      + " size="
                      + obsDimSize
                      + ").");
            }

            // read the keep rows of requested variable[obs]
            for (int v = 0; v < nVars; v++) {
              // String2.log("var[" + v + "]=" + varNames[v] + " ndim=" + varNDims[v] + "
              // usesObsDim=" + varUsesDim[v][obsDim]);
              if (varInLoadOrConVariables[v]
                  && varNDims[v] == 1
                  && varUsesDim[v][obsDim]) { // ensure correct dim
                PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
                pa.justKeep(obsKeep); // as each var read in, to save memory
                pa.trimToSize();
                addColumn(nColumns(), varNames[v], pa, varAtts[v]);
                standardizeLastColumn(standardizeWhat);
              }
            }
            if (debugMode) {
              String2.log(
                  "  Debug: keyColumnPA.size="
                      + keyColumnPA.size()
                      + " obsKeep.cardinality="
                      + obsKeep.cardinality());
              ensureValid();
            }

            // nLevels=1 multidimensional       //read data for keep=true features
          } else {
            if (debugMode) String2.log("  Debug: nLevels=1 multidimensional");
            readAs = "multidim";
            if (ncCFcc != null) ncCFcc.set(45);

            // see unitTestDataDir/CFPointConventions/timeSeries/
            //    timeSeries-Orthogonal-Multidimenstional-MultipleStations-H.2.1
            //  which has outer=time[time],
            //    inner=lat,lon,alt,stationName[station], and
            //    temperature[time][station]
            Table innerTable = new Table();
            int nLoadOrConVariablesInInnerTable = 0;
            StringArray cdmInnerVars = new StringArray();
            for (int v = 0; v < nVars; v++) {
              // multidim: load all variable[obs]
              if (varNDims[v] == 1 && varUsesDim[v][obsDim]) { // ensure correct dim
                if (varInLoadOrConVariables[v]) {
                  nLoadOrConVariablesInInnerTable++;
                  cdmInnerVars.add(
                      varNames[v]); // so in file's order; that's consistent; that's good
                }
                innerTable.addColumn(
                    innerTable.nColumns(),
                    varNames[v],
                    NcHelper.getPrimitiveArray(vars[v]),
                    varAtts[v]);
                innerTable.standardizeLastColumn(standardizeWhat);
              }
            }
            int innerTableNColumns = innerTable.nColumns();
            int innerTableNRows = innerTable.nRows();
            BitSet innerKeep = null; // if null, assume all innerKeep are true
            if (debugMode)
              String2.log(
                  "  Debug: innerTable nLoadOrConVarsInInnerTable="
                      + nLoadOrConVariablesInInnerTable
                      + " nCols="
                      + innerTableNColumns
                      + " ("
                      + innerTable.getColumnNamesCSVString()
                      + ") nRows="
                      + innerTableNRows);
            if (innerTableNColumns > 0) {
              if (ncCFcc != null) ncCFcc.set(46);

              // apply constraints to innerTable  (but keep all innerTable rows)
              innerKeep = innerTable.rowsWithData();
              int innerNGood =
                  innerTable.tryToApplyConstraints(-1, conNames, conOps, conValues, innerKeep);
              if (innerNGood == 0) {
                if (verbose)
                  String2.log(
                      "  readNcCF "
                          + MustBe.THERE_IS_NO_DATA
                          + " fileName="
                          + fullName
                          + " (innerNGood=0)"
                          + " time="
                          + (System.currentTimeMillis() - time)
                          + "ms");
                removeAllColumns();
                return;
              }

              // just keep the loadVariables columns of innerTable
              innerTable.reorderColumns(loadVariableNames, true); // discard others
              innerTableNColumns = innerTable.nColumns();

              // Are we done?  just outerTable + innerTable vars?
              if (debugMode)
                String2.log(
                    "  Debug: nLoadOrConVariables InFile="
                        + nLoadOrConVariablesInFile
                        + " InOuter="
                        + nLoadOrConVariablesInOuterTable
                        + " InInner="
                        + nLoadOrConVariablesInInnerTable
                        + "\n    innerTable nColumns="
                        + innerTable.nColumns()
                        + ": "
                        + innerTable.getColumnNamesCSVString());
              if (nLoadOrConVariablesInFile
                  == nLoadOrConVariablesInOuterTable + nLoadOrConVariablesInInnerTable) {
                // user requested e.g., outer=station[10] and inner=time[810740],
                //  but user didn't request observations[station][time]
                if (ncCFcc != null) ncCFcc.set(47);

                // justKeep good rows of innerTable
                innerTable.justKeep(innerKeep);
                innerTableNRows = innerTable.nRows();

                // make columns in this table paralleling innerTable
                boolean justInnerTable =
                    nLoadOrConVariablesInFile == nLoadOrConVariablesInInnerTable;
                for (int col = 0; col < innerTableNColumns; col++) {
                  addColumn(
                      nColumns(),
                      innerTable.getColumnName(col),
                      justInnerTable
                          ? innerTable.getColumn(col)
                          : // copy data
                          PrimitiveArray.factory( // don't copy data
                              innerTable.getColumn(col).elementType(), 1024, false),
                      innerTable.columnAttributes(col));
                }

                // just want vars in innerTable?
                if (justInnerTable) {
                  // finish up
                  if (ncCFcc != null) ncCFcc.set(48);
                  tryToApplyConstraintsAndKeep(
                      -1, conNames, conOps, conValues); // may be 0 rows left
                  if (nRows() == 0) removeAllColumns();
                  else reorderColumns(loadVariableNames, true); // discard others
                  if (reallyVerbose)
                    String2.log(
                        "  readNcCF finished (nLevels=1, readAs="
                            + readAs
                            + ", just innerTable vars) fileName="
                            + fullName
                            + " nRows="
                            + nRows()
                            + " nCols="
                            + nColumns()
                            + " time="
                            + (System.currentTimeMillis() - time)
                            + "ms");
                  if (debugMode) ensureValid();
                  decodeCharsAndStrings();
                  convertToUnsignedPAs();
                  return;
                }

                if (nLoadOrConVariablesInOuterTable > 0) {
                  // join justKeep rows of outerTable (e.g., stations)
                  if (ncCFcc != null) ncCFcc.set(49);
                  if (outerKeep != null) {
                    if (ncCFcc != null) ncCFcc.set(50);
                    outerTable.justKeep(outerKeep);
                  }
                  outerTableNRows = outerTable.nRows();
                  if (debugMode) {
                    String2.log(
                        "  Debug: nRows outerTable="
                            + outerTableNRows
                            + " innerTable="
                            + innerTableNRows
                            + " this="
                            + nRows()
                            + "\n"
                            + "    cols outer="
                            + outerTable.getColumnNamesCSVString()
                            + " inner="
                            + innerTable.getColumnNamesCSVString()
                            + " this="
                            + getColumnNamesCSVString());
                    outerTable.ensureValid();
                    innerTable.ensureValid();
                    ensureValid();
                  }

                  // make outerTableNRows copies of innerTable (e.g., time)
                  // and the index to outerTable
                  IntArray outerIndexPA = new IntArray();
                  for (int oRow = 0; oRow < outerTableNRows; oRow++) {
                    if (ncCFcc != null) ncCFcc.set(51);
                    for (int iCol = 0; iCol < innerTableNColumns; iCol++)
                      getColumn(iCol).append(innerTable.getColumn(iCol));
                    outerIndexPA.addN(innerTableNRows, oRow); // 2015-05-26 add->addN !
                  }
                  addColumn(0, "outerIndex", outerIndexPA, new Attributes());
                  if (debugMode) ensureValid();

                  // join with outerTable (all constraints already applied)
                  // insert row number in outerTable (to be the key column)
                  PrimitiveArray keyPA = new IntArray(0, outerTableNRows - 1);
                  outerTable.addColumn(0, "keyCol", keyPA, new Attributes());
                  join(1, 0, "", outerTable); // outerTable is lookUpTable
                  removeColumn(0); // remove the outerIndex
                } // else no outer table columns needed

                // finish up
                tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
                if (nRows() == 0) removeAllColumns();
                else reorderColumns(loadVariableNames, true); // discard others
                if (reallyVerbose)
                  String2.log(
                      "  readNcCF finished (nLevels=1, readAs="
                          + readAs
                          + ", just outerTable+innerTable vars) fileName="
                          + fullName
                          + " nRows="
                          + nRows()
                          + " nCols="
                          + nColumns()
                          + " time="
                          + (System.currentTimeMillis() - time)
                          + "ms");
                if (debugMode) ensureValid();
                decodeCharsAndStrings();
                convertToUnsignedPAs();
                return;
              }
            } // below, innerTable may have 0 or more columns

            // make obsKeep (with outerKeep and innerKeep info) and
            // make outerKeyColumn (with row#'s in outerTable) and
            // make innerKeyColumn (with row#'s in innerTable)
            if (ncCFcc != null) ncCFcc.set(52);
            BitSet obsKeep = new BitSet(outerDimSize * obsDimSize); // all are false
            IntArray outerKeyColumnPA = new IntArray(outerDimSize * obsDimSize, false);
            IntArray innerKeyColumnPA = new IntArray(outerDimSize * obsDimSize, false);
            int obsRow = 0;
            for (int outerRow = 0; outerRow < outerDimSize; outerRow++) {
              boolean oKeep = outerKeep == null || outerKeep.get(outerRow);
              outerKeyColumnPA.addN(obsDimSize, outerRow);
              for (int innerRow = 0; innerRow < obsDimSize; innerRow++) {
                if (oKeep && (innerKeep == null || innerKeep.get(innerRow))) obsKeep.set(obsRow);
                innerKeyColumnPA.add(innerRow);
                obsRow++;
              }
            }
            if (debugMode)
              String2.log(
                  "  Debug: outerKeep="
                      + String2.noLongerThanDots(
                          outerKeep == null ? "null" : outerKeep.toString(), 60)
                      + "\n    innerKeep="
                      + String2.noLongerThanDots(
                          innerKeep == null ? "null" : innerKeep.toString(), 60)
                      + "\n    obsKeep="
                      + String2.noLongerThanDots(obsKeep.toString(), 60));

            if (obsKeep.isEmpty()) {
              if (verbose)
                String2.log(
                    "  readNcCF "
                        + MustBe.THERE_IS_NO_DATA
                        + " (nLevels=1, readAs="
                        + readAs
                        + ", no match outer+inner constraints)"
                        + " fileName="
                        + fullName
                        + " time="
                        + (System.currentTimeMillis() - time)
                        + "ms");
              removeAllColumns();
              return;
            }

            // apply obsKeep to outerKeyColumnPA and innerKeyColumnPA
            outerKeyColumnPA.justKeep(obsKeep);
            outerKeyColumnPA.trimToSize();
            innerKeyColumnPA.justKeep(obsKeep);
            innerKeyColumnPA.trimToSize();
            if (debugMode)
              String2.log(
                  "  Debug: outerKeySize="
                      + outerKeyColumnPA.size()
                      + "  innerKeySize="
                      + innerKeyColumnPA.size());
            // read the keep rows of requested variable[outer][obs]
            for (int v = 0; v < nVars; v++) {
              if (ncCFcc != null) ncCFcc.set(53);
              if (varInLoadOrConVariables[v]
                  && varNDims[v] == 2
                  && varUsesDim[v][outerDim]
                  && varUsesDim[v][obsDim]) { // dim order checked above
                if (ncCFcc != null) ncCFcc.set(54);
                PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
                if (debugMode)
                  String2.log("  Debug: read var=" + varNames[v] + " pa.size=" + pa.size());
                pa.justKeep(obsKeep); // as each var read in, to save memory
                pa.trimToSize();
                if (debugMode) String2.log("    trimmed pa.size=" + pa.size());
                addColumn(nColumns(), varNames[v], pa, varAtts[v]);
                standardizeLastColumn(standardizeWhat);
              }
            }

            // Remove rows where all obs data is MV
            // Rows with only outerTable or innerTable MVs have been removed by obsKeep above.
            if (debugMode)
              String2.log(
                  "  Debug: before remove rows where all obs data is MV (nRows="
                      + nRows()
                      + "):\n"
                      + dataToString(3));
            obsKeep = rowsWithData();
            addColumn(0, "outerKeyColumn", outerKeyColumnPA, new Attributes());
            addColumn(1, "innerKeyColumn", innerKeyColumnPA, new Attributes());
            justKeep(obsKeep);
            // String2.log("after read vars\n" + dataToString());
            if (debugMode) {
              String2.log("  Debug: after removeRowsWithJustMVs nRows=" + nRows());
              ensureValid(); // throws Exception if not
            }
            if (nRows() == 0) {
              if (verbose)
                String2.log(
                    "  readNcCF "
                        + MustBe.THERE_IS_NO_DATA
                        + " (nLevels=1, readAs="
                        + readAs
                        + ", after removeMVRows)"
                        + " fileName="
                        + fullName
                        + " time="
                        + (System.currentTimeMillis() - time)
                        + "ms");
              removeAllColumns();
              return;
            }

            // if innerTable.nColumns > 0, join it  (it has its original rows)
            if (innerTable.nColumns() > 0) {
              // insert row number in innerTable (to be the key column)
              if (ncCFcc != null) ncCFcc.set(55);
              PrimitiveArray keyPA = new IntArray(0, innerTable.nRows() - 1);
              innerTable.addColumn(0, "innerKeyColumn", keyPA, new Attributes());
              join(1, 1, "", innerTable); // innerTable is lookUpTable
            }
            removeColumn(1); // remove the innerTable keyColumn
          }
          // here, this table col#0 is outerKeyColumn
          // and outerTable has all original rows

          // join to add the outerTable columns
          // rearrange the outerTable columns to the loadVariables order
          if (ncCFcc != null) ncCFcc.set(57);
          outerTable.reorderColumns(loadVariableNames, true); // true, remove unrequested columns
          outerTableNColumns = outerTable.nColumns();
          outerTableNRows = outerTable.nRows();
          if (outerTableNColumns > 0) {
            // insert row number in outerTable (to be the key column)
            if (ncCFcc != null) ncCFcc.set(56);
            PrimitiveArray keyPA = new IntArray(0, outerTable.nRows() - 1);
            outerTable.addColumn(0, "keyColumn", keyPA, new Attributes());
            join(1, 0, "", outerTable); // outerTable is lookUpTable
          }
          removeColumn(0); // remove the outerTable keyColumn
        }

        // finish up all nLevels=1 files
        if (nColumns() == 0) {
          if (verbose)
            String2.log(
                "  readNcCF "
                    + MustBe.THERE_IS_NO_DATA
                    + " (nLevels=1, readAs="
                    + readAs
                    + ", nColumns=0)"
                    + " fileName="
                    + fullName
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          return;
        }

        // finish up
        if (ncCFcc != null) ncCFcc.set(58);
        tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
        if (nRows() == 0) removeAllColumns();
        else reorderColumns(loadVariableNames, true); // discard others
        if (reallyVerbose)
          String2.log(
              "  readNcCF finished (nLevels=1, readAs="
                  + readAs
                  + ", all). fileName="
                  + fullName
                  + " nRows="
                  + nRows()
                  + " nCols="
                  + nColumns()
                  + " time="
                  + (System.currentTimeMillis() - time)
                  + "ms");
        if (debugMode) ensureValid();
        decodeCharsAndStrings();
        convertToUnsignedPAs();
        return;
      }

      // *** nLevels=2 files
      Table innerTable = new Table(); // only gets varInLoadOrConVariables vars
      int innerTableNColumns, innerTableNRows; // may be 0

      // * read nLevels=2 ragged array files
      if ((indexVar >= 0 || outerDim == scalarDim) && rowSizeVar >= 0) {
        if (debugMode) String2.log("  Debug: nLevels=2 files, ragged");
        readAs = "ragged";
        if (ncCFcc != null) ncCFcc.set(59);

        // read variable[innerDim] into innerTable
        StringArray cdmInnerVars = new StringArray();
        for (int v = 0; v < nVars; v++) {
          if (ncCFcc != null) ncCFcc.set(60);
          if (varInLoadOrConVariables[v]
              && varNDims[v] == 1
              && varUsesDim[v][innerDim]) { // ensure correct dim
            cdmInnerVars.add(varNames[v]); // so in file's order; that's consistent; that's good
            innerTable.addColumn(
                innerTable.nColumns(),
                varNames[v],
                NcHelper.getPrimitiveArray(vars[v]),
                varAtts[v]);
            innerTable.standardizeLastColumn(standardizeWhat);
          }
        }
        innerTableNColumns = innerTable.nColumns(); // It has no index columns
        innerTableNRows = innerTable.nRows();
        globalAttributes.set(cdmInnerName, cdmInnerVars.toString()); // may be "", that's okay
        subsetVars.append(cdmInnerVars);
        globalAttributes.set("subsetVariables", subsetVars.toString()); // may be "", that's okay

        // read the outerIndexPA from vars[indexVar] and ensure valid
        // next 3 lines: as if no indexVar (outerDim == scalarDim)
        PrimitiveArray outerIndexPA = PrimitiveArray.factory(PAType.INT, innerDimSize, "0");
        int indexMV = Integer.MAX_VALUE;
        int indexFV = Integer.MAX_VALUE;
        if (indexVar >= 0) {
          // then replace if indexVar exists
          if (ncCFcc != null) ncCFcc.set(61);
          outerIndexPA = NcHelper.getPrimitiveArray(vars[indexVar]);
          // no need to unpack the index var
          indexMV = varAtts[indexVar].getInt("missing_value"); // MAX_VALUE if not defined
          indexFV = varAtts[indexVar].getInt("_FillValue"); // MAX_VALUE if not defined
        }
        int outerIndexSize = outerIndexPA.size();
        if (outerTableNRows > 0) {
          if (ncCFcc != null) ncCFcc.set(62);
          for (int row = 0; row < outerIndexSize; row++) {
            int index = outerIndexPA.getInt(row);
            if (index >= 0 && index < outerTableNRows) {
              // it's an index
            } else if (index == indexMV || index == indexFV) {
              // that's the right way to reserve space
            } else {
              throw new SimpleException(
                  errorInMethod
                      + "Invalid file: The index values must be 0 - "
                      + (outerTableNRows - 1)
                      + ", but "
                      + varNames[indexVar]
                      + "["
                      + row
                      + "]="
                      + index
                      + ".");
            }
          }
        }

        // set up innerKeep with outerIndex info.
        BitSet innerKeep = new BitSet(innerDimSize); // includes outerKeep info
        if (outerKeep == null) {
          if (ncCFcc != null) ncCFcc.set(63);
          innerKeep.set(0, innerDimSize);
        } else {
          if (ncCFcc != null) ncCFcc.set(64);
          for (int i = 0; i < innerDimSize; i++)
            if (outerKeep.get(outerIndexPA.getInt(i))) innerKeep.set(i);
        }

        // apply innerTable constraints
        int keepNInner = -1;
        if (innerTableNColumns > 0) {
          if (ncCFcc != null) ncCFcc.set(65);
          keepNInner = innerTable.tryToApplyConstraints(-1, conNames, conOps, conValues, innerKeep);
          if (keepNInner == 0) {
            if (ncCFcc != null) ncCFcc.set(66);
            if (verbose)
              String2.log(
                  "  readNcCF "
                      + MustBe.THERE_IS_NO_DATA
                      + " (nLevels=2, readAs="
                      + readAs
                      + ", keepNInner=0)"
                      + " fileName="
                      + fullName
                      + " time="
                      + (System.currentTimeMillis() - time)
                      + "ms");
            removeAllColumns();
            return;
          }
          // order of rows is important, so *don't* justKeep(innerKeep)
        }
        if (debugMode)
          String2.log(
              "  Debug: ragged innerTable has nCols="
                  + innerTableNColumns
                  + " nRows="
                  + innerTableNRows
                  + " nKeepRows="
                  + keepNInner
                  + "\n"
                  + innerTable.dataToString());

        // Are we done? Are those all the variables we need that are in the file?
        if (nLoadOrConVariablesInFile == nLoadOrConVariablesInOuterTable + innerTableNColumns) {
          // join with outerTable
          if (ncCFcc != null) ncCFcc.set(67);
          if (outerTableNColumns > 0) {
            if (ncCFcc != null) ncCFcc.set(68);
            innerTable.addColumn(0, "outerIndex", outerIndexPA, new Attributes());
            // insert row number in outerTable (to be the key column)
            PrimitiveArray keyPA = new IntArray(0, outerTableNRows - 1);
            outerTable.addColumn(0, "keyCol", keyPA, new Attributes());
            innerTable.join(1, 0, "", outerTable); // outerTable is lookUpTable
            innerTable.removeColumn(0); // remove the outerIndex
          }

          // justKeep
          innerTable.justKeep(innerKeep); // includes outer and inner info

          // globalAttributes already set
          // copy innerTable to this table
          int nic = innerTable.nColumns();
          for (int c = 0; c < nic; c++)
            addColumn(
                c,
                innerTable.getColumnName(c),
                innerTable.getColumn(c),
                innerTable.columnAttributes(c));

          // finish up
          if (ncCFcc != null) ncCFcc.set(69);
          tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
          if (nRows() == 0) removeAllColumns();
          else reorderColumns(loadVariableNames, true); // discard others
          if (reallyVerbose)
            String2.log(
                "  readNcCF finished (nLevels=2, readAs="
                    + readAs
                    + ", just outerTable+innerTable vars)."
                    + " fileName="
                    + fullName
                    + " nRows="
                    + nRows()
                    + " nCols="
                    + nColumns()
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          if (debugMode) ensureValid();
          decodeCharsAndStrings();
          convertToUnsignedPAs();
          return;
        }

        // make col#0=outerIndexColumn
        // and  col#1=innerIndexColumn (with row#'s in innerTable from rowSizesPA)
        // and make obsKeep.
        // obsKeep Approach: optimize for situation that takes longest
        //  (outerKeep and innerKeep all true).  This is also a very simple approach.
        if (ncCFcc != null) ncCFcc.set(70);
        PrimitiveArray rowSizesPA = NcHelper.getPrimitiveArray(vars[rowSizeVar]);
        // no need to unpack rowSizes
        IntArray outerIndexColumnPA = new IntArray();
        IntArray innerIndexColumnPA = new IntArray();
        addColumn(0, "outerIndexCol", outerIndexColumnPA, new Attributes());
        addColumn(1, "innerIndexCol", innerIndexColumnPA, new Attributes());
        int startRow, endRow = 0; // endRow is exclusive
        BitSet obsKeep = new BitSet(obsDimSize); // all are false
        int nRowSizes = rowSizesPA.size();
        for (int innerRow = 0; innerRow < nRowSizes; innerRow++) { // not innerTableNRows
          startRow = endRow;
          int getNRows = rowSizesPA.getInt(innerRow);
          endRow += getNRows;
          if (innerKeep.get(innerRow)) { // innerKeep always exists
            // innerKeep always exists and includes outerKeep info
            // outer/innerIndexColumnPA already have obsKeep applied
            outerIndexColumnPA.addNInts(getNRows, outerIndexPA.getInt(innerRow));
            innerIndexColumnPA.addNInts(getNRows, innerRow);

            // obsKeep has a bit for each obs row
            obsKeep.set(startRow, endRow);
          }
        }
        if (debugMode)
          String2.log(
              "  Debug: outerIndexCol[obs]="
                  + outerIndexColumnPA.toString()
                  + "\ninnerIndexCol[obs]="
                  + innerIndexColumnPA.toString());

        // read the obsKeep rows of requested variable[obs]
        for (int v = 0; v < nVars; v++) {
          if (ncCFcc != null) ncCFcc.set(71);
          if (varInLoadOrConVariables[v]
              && varNDims[v] == 1
              && varUsesDim[v][obsDim]) { // ensure correct dim
            PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
            pa.justKeep(obsKeep); // as each var read in, to save memory
            pa.trimToSize();
            addColumn(nColumns(), varNames[v], pa, varAtts[v]);
            standardizeLastColumn(standardizeWhat);
          }
        }

        // *** read nLevels=2 multidimensional files
      } else if (multidimensional) {
        if (debugMode) String2.log("  Debug: nLevels=2 files, multidimensional");
        readAs = "multidim";
        if (ncCFcc != null) ncCFcc.set(72);

        // create outerIndexPA and innerIndexPA, both [outerDim][innerDim]
        int outerXInnerDimSize = outerDimSize * innerDimSize;
        IntArray outerIndexPA = new IntArray(outerXInnerDimSize, false);
        BitSet innerKeep = new BitSet(outerXInnerDimSize); // includes outerKeep info
        int tRow = 0;
        for (int outer = 0; outer < outerDimSize; outer++) {
          outerIndexPA.addN(innerDimSize, outer);
          if (outerKeep == null || outerKeep.get(outer)) innerKeep.set(tRow, tRow + innerDimSize);
          tRow += innerDimSize;
        }

        // read ALL variable[outerDim][innerDim] into innerTable
        int nLoadOrConVariablesInInnerTable = 0;
        StringArray cdmInnerVars = new StringArray();
        for (int v = 0; v < nVars; v++) {
          // read ALL innerTable variables, not just varInLoadOrConVariables
          // because their all-mv rows determine which chunks of obs table to ignore
          if (ncCFcc != null) ncCFcc.set(73);
          if (varNDims[v] == 2 && varUsesDim[v][outerDim] && varUsesDim[v][innerDim]) {
            // dim order not checked above, so check it here
            // It's complicated if outerDim is scalarDim.
            if (ncCFcc != null) ncCFcc.set(74);
            int dim0 =
                outerDim == scalarDim ? scalarDim : dimsList.indexOf(vars[v].getDimension(0));
            int dim1 = dimsList.indexOf(vars[v].getDimension(outerDim == scalarDim ? 0 : 1));
            if (dim0 == outerDim && dim1 == innerDim) {
              if (ncCFcc != null) ncCFcc.set(75);
              if (varInLoadOrConVariables[v]) {
                if (ncCFcc != null) ncCFcc.set(76);
                nLoadOrConVariablesInInnerTable++;
                cdmInnerVars.add(varNames[v]); // so in file's order; that's consistent; that's good
              }
              innerTable.addColumn(
                  innerTable.nColumns(),
                  varNames[v],
                  NcHelper.getPrimitiveArray(vars[v]),
                  varAtts[v]);
              innerTable.standardizeLastColumn(standardizeWhat);
            } else {
              if (reallyVerbose)
                String2.log(
                    "  !!! nLevels=2 readAs="
                        + readAs
                        + ": Unexpected dimension order for "
                        + varNames[v]);
            }
            if (ncCFcc != null) ncCFcc.set(77); // duplicate of 74
          }
        }
        innerTableNColumns = innerTable.nColumns(); // It has no index columns
        innerTableNRows = innerTable.nRows();
        globalAttributes.set(cdmInnerName, cdmInnerVars.toString()); // may be "", that's okay
        subsetVars.append(cdmInnerVars);
        globalAttributes.set("subsetVariables", subsetVars.toString()); // may be "", that's okay

        // trouble?  look for truly orthogonal: just variable[innerDim]
        if (innerTableNColumns == 0) {
          if (debugMode) String2.log("  Debug: innerTableNColumns=0");
          if (ncCFcc != null) ncCFcc.set(78);

          // read ALL variable[innerDim] into innerTable
          nLoadOrConVariablesInInnerTable = 0; // should be already
          for (int v = 0; v < nVars; v++) {
            // read ALL innerTable variables, not just varInLoadVariables and varInConstraints
            // because their all-mv rows determine which chunks of obs table to ignore
            if (ncCFcc != null) ncCFcc.set(79);
            if (varNDims[v] == 1 && varUsesDim[v][innerDim]) { // ensure correct dim
              if (ncCFcc != null) ncCFcc.set(80);
              if (varInLoadOrConVariables[v]) nLoadOrConVariablesInInnerTable++;
              innerTable.addColumn(
                  innerTable.nColumns(),
                  varNames[v],
                  NcHelper.getPrimitiveArray(vars[v]),
                  varAtts[v]);
              innerTable.standardizeLastColumn(standardizeWhat);
            }
          }

          innerTableNColumns = innerTable.nColumns(); // It has no index columns
          innerTableNRows = innerTable.nRows();

          if (innerTableNColumns == 0) {
            if (verbose)
              String2.log(
                  "  readNcCF "
                      + MustBe.THERE_IS_NO_DATA
                      + " (nLevels=2, readAs="
                      + readAs
                      + ", no variable["
                      + outerDimName
                      + "]["
                      + innerDimName
                      + "] "
                      + " or variable["
                      + innerDimName
                      + "])"
                      + " fileName="
                      + fullName
                      + " time="
                      + (System.currentTimeMillis() - time)
                      + "ms");
            removeAllColumns();
            return;
          }

          // make outerDimSize-1 duplicates of the rows of the innerTable
          // so it becomes innerTable with variable[outerDim][innerDim]
          for (int col = 0; col < innerTableNColumns; col++) {
            if (ncCFcc != null) ncCFcc.set(81);
            PrimitiveArray pa = innerTable.getColumn(col);
            PrimitiveArray clone = (PrimitiveArray) pa.clone();
            for (int copy = 1; copy < outerDimSize; copy++) pa.append(clone); // efficient
          }
          innerTableNRows = innerTable.nRows();
        }
        // String2.log("  innerTable=\n" + innerTable.dataToString());

        // rowsWithData   (but don't remove any rows)
        // Note that innerTable MUST exist.
        BitSet keepNonMVs = innerTable.rowsWithData();
        // String2.log("  innerTable keepNonMVs=" + keepNonMVs.toString());
        innerKeep.and(keepNonMVs);

        // apply user constraints in innerTable (but don't remove any rows)
        int nInnerGood =
            innerTable.tryToApplyConstraints(-1, conNames, conOps, conValues, innerKeep);
        if (nInnerGood == 0) {
          if (ncCFcc != null) ncCFcc.set(82);
          if (verbose)
            String2.log(
                "  readNcCF "
                    + MustBe.THERE_IS_NO_DATA
                    + " (nLevels=2, readAs="
                    + readAs
                    + ", nInnerGood=0)"
                    + " fileName="
                    + fullName
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          removeAllColumns();
          return;
        }
        // order of rows is important, so *don't* justKeep(innerKeep)
        if (debugMode)
          String2.log(
              "  Debug: multidim innerTable has nCols="
                  + innerTableNColumns
                  + " nRows="
                  + innerTableNRows
                  + " nKeepRows="
                  + nInnerGood);
        // String2.log("outer=\n" + outerTable.dataToString() + "\ninner=\n" +
        // innerTable.dataToString());

        // Are we done? Are those all the variables we need that are in the file?
        if (nLoadOrConVariablesInFile
            == nLoadOrConVariablesInOuterTable + nLoadOrConVariablesInInnerTable) {

          // join to add the outerTable columns
          // rearrange the outerTable columns to the loadVariables order
          if (ncCFcc != null) ncCFcc.set(83);
          outerTable.reorderColumns(loadVariableNames, true); // true, remove unrequested columns
          outerTableNColumns = outerTable.nColumns();
          outerTableNRows = outerTable.nRows();
          if (outerTableNColumns > 0) {
            // join with outerTable
            if (ncCFcc != null) ncCFcc.set(84);
            innerTable.addColumn(0, "outerIndex", outerIndexPA, new Attributes());
            // insert row number in outerTable (to be the key column)
            PrimitiveArray keyPA = new IntArray(0, outerTableNRows - 1);
            outerTable.addColumn(0, "keyColumn", keyPA, new Attributes());
            innerTable.join(1, 0, "", outerTable); // outerTable is lookUpTable
            innerTable.removeColumn(0); // remove the outerIndex
          }

          // justKeep
          innerTable.justKeep(innerKeep); // includes outer info, inner info, mv info

          // globalAttributes already set
          // copy innerTable to this table
          int nic = innerTable.nColumns();
          for (int c = 0; c < nic; c++)
            addColumn(
                c,
                innerTable.getColumnName(c),
                innerTable.getColumn(c),
                innerTable.columnAttributes(c));

          // finish up
          if (ncCFcc != null) ncCFcc.set(85);
          tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
          if (nRows() == 0) removeAllColumns();
          else reorderColumns(loadVariableNames, true); // discard others
          if (reallyVerbose)
            String2.log(
                "  readNcCF finished (nLevels=2, readAs="
                    + readAs
                    + ", just outerTable+innerTable vars)."
                    + " fileName="
                    + fullName
                    + " nRows="
                    + nRows()
                    + " nCols="
                    + nColumns()
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          if (debugMode) ensureValid();
          decodeCharsAndStrings();
          convertToUnsignedPAs();
          return;
        }

        // * Make interiorTable with var[obs] and var[scalar][obs]?  some files have them
        if (debugMode) String2.log("  Debug: make interiorTable with variable[obs]?");
        if (ncCFcc != null) ncCFcc.set(86);
        Table interiorTable = new Table();
        int nLoadOrConVariablesInInteriorTable = 0;
        for (int v = 0; v < nVars; v++) {
          if (!varInLoadOrConVariables[v]) continue;
          if ((varNDims[v] == 1 && varUsesDim[v][obsDim])
              || (varNDims[v] == 2
                  && varUsesDim[v][obsDim]
                  && varUsesDim[v][scalarDim])) { // always?
            nLoadOrConVariablesInInteriorTable++;
            interiorTable.addColumn(
                interiorTable.nColumns(),
                varNames[v],
                NcHelper.getPrimitiveArray(vars[v]),
                varAtts[v]);
            interiorTable.standardizeLastColumn(standardizeWhat);
          }
        }
        int interiorTableNColumns = interiorTable.nColumns(); // It has no index columns
        int interiorTableNRows = interiorTable.nRows();

        // do things if the interiorTable exists
        BitSet interiorKeep = null; // will be null if no interiorTable columns
        if (interiorTableNColumns > 0) {
          // apply constraints (but keep all the rows)
          if (ncCFcc != null) ncCFcc.set(87);
          if (debugMode) String2.log("  Debug: interiorTable exists");
          interiorKeep = new BitSet();
          interiorKeep.set(0, interiorTableNRows, true);
          int interiorNKeep =
              interiorTable.tryToApplyConstraints(-1, conNames, conOps, conValues, interiorKeep);
          if (interiorNKeep == 0) {
            if (ncCFcc != null) ncCFcc.set(88);
            if (verbose)
              String2.log(
                  "  readNcCF "
                      + MustBe.THERE_IS_NO_DATA
                      + " (nLevels=2, readAs="
                      + readAs
                      + ", interiorNKeep=0)"
                      + " fileName="
                      + fullName
                      + " time="
                      + (System.currentTimeMillis() - time)
                      + "ms");
            removeAllColumns();
            return;
          }
          if (debugMode) String2.log("  Debug: interiorTable=\n" + interiorTable.dataToString(5));

          // are we done?
          if (nLoadOrConVariablesInFile == nLoadOrConVariablesInInteriorTable) {

            // justKeep
            if (ncCFcc != null) ncCFcc.set(89);
            interiorTable.justKeep(interiorKeep); // includes outer info, inner info, mv info

            // globalAttributes already set
            // copy interiorTable to this table
            int nic = interiorTable.nColumns();
            for (int c = 0; c < nic; c++)
              addColumn(
                  c,
                  interiorTable.getColumnName(c),
                  interiorTable.getColumn(c),
                  interiorTable.columnAttributes(c));

            // finish up
            tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
            if (nRows() == 0) removeAllColumns();
            else reorderColumns(loadVariableNames, true); // discard others
            if (reallyVerbose)
              String2.log(
                  "  readNcCF finished (nLevels=2, readAs="
                      + readAs
                      + ", just interiorTable vars)."
                      + " fileName="
                      + fullName
                      + " nRows="
                      + nRows()
                      + " nCols="
                      + nColumns()
                      + " time="
                      + (System.currentTimeMillis() - time)
                      + "ms");
            if (debugMode) ensureValid();
            decodeCharsAndStrings();
            convertToUnsignedPAs();
            return;
          }
        }

        // read the rowSizesPA
        // make   outerIndexColumnPA (with row#'s in outerTable)    (but don't add to this table
        // yet)
        // and    innerIndexColumnPA (with row#'s in innerTable)    (but don't add to this table
        // yet)
        // and interiorIndexColumnPA (with row#'s in interiorTable) (but don't add to this table
        // yet)
        // and make obsKeep
        // obsKeep Approach: optimize for situation that takes longest
        //  (outerKeep and innerKeep all true).  This is also a very simple approach.
        if (debugMode) String2.log("  Debug: read rowSizesPA and make many IndexColumnPAs");
        if (ncCFcc != null) ncCFcc.set(90);
        IntArray outerIndexColumnPA = new IntArray();
        IntArray innerIndexColumnPA = new IntArray();
        IntArray interiorIndexColumnPA = new IntArray();
        BitSet obsKeep = new BitSet(outerXInnerDimSize * obsDimSize); // all false
        int oiRow = 0; // outer inner
        int oioRow = 0; // outer inner obs
        for (int outer = 0; outer < outerDimSize; outer++) {
          for (int inner = 0; inner < innerDimSize; inner++) {
            if (innerKeep.get(oiRow)) { // innerKeep always exists and includes outerKeep info
              for (int obs = 0; obs < obsDimSize; obs++) {
                boolean tiKeep = interiorKeep == null || interiorKeep.get(obs);
                if (tiKeep) { // for all: innerKeep and interiorKeep already applied
                  outerIndexColumnPA.add(outer); // row in outerTable
                  innerIndexColumnPA.add(oiRow); // row in innerTable
                  interiorIndexColumnPA.add(obs); // row in interiorTable
                  obsKeep.set(oioRow);
                }
                oioRow++;
              }
            } else {
              oioRow += obsDimSize;
            }
            oiRow++;
          }
        }

        // read the obsKeep rows of requested variable[outerDim][innerDim][obs]
        for (int v = 0; v < nVars; v++) {
          if (ncCFcc != null) ncCFcc.set(91);
          if (varInLoadOrConVariables[v]
              && varNDims[v] == 3
              && varUsesDim[v][outerDim]
              && // dim order checked above when dims detected
              varUsesDim[v][innerDim]
              && varUsesDim[v][obsDim]) {
            if (ncCFcc != null) ncCFcc.set(92);
            PrimitiveArray pa = NcHelper.getPrimitiveArray(vars[v]);
            pa.justKeep(obsKeep); // as each var read in, to save memory
            pa.trimToSize();
            addColumn(nColumns(), varNames[v], pa, varAtts[v]);
            standardizeLastColumn(standardizeWhat);
          }
        }

        // remove rows at end with all MV
        int preNRows = nRows();
        obsKeep = rowsWithData();
        addColumn(0, "outerIndexCol", outerIndexColumnPA, new Attributes());
        addColumn(1, "innerIndexCol", innerIndexColumnPA, new Attributes());
        addColumn(2, "interiorIndexCol", interiorIndexColumnPA, new Attributes());
        // String2.log("  obs before justKeep(obsKeep):\n" + dataToString());
        justKeep(obsKeep);
        if (debugMode) {
          String2.log(
              "  Debug: main table nRows before="
                  + preNRows
                  + ", nRows after removeRowsWithJustMVs="
                  + nRows());
          ensureValid(); // throws Exception if not
        }
        if (nRows() == 0) {
          if (verbose)
            String2.log(
                "  readNcCF "
                    + MustBe.THERE_IS_NO_DATA
                    + " (nLevels=1, readAs="
                    + readAs
                    + ", after removeMVRows)"
                    + " fileName="
                    + fullName
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms");
          removeAllColumns();
          return;
        }

        // join interiorTable
        if (nLoadOrConVariablesInInteriorTable > 0) {
          // insert row number in interiorTable (to be the key column)
          if (ncCFcc != null) ncCFcc.set(93);
          PrimitiveArray keyPA = new IntArray(0, interiorTableNRows - 1);
          interiorTable.addColumn(0, "keyColumn", keyPA, new Attributes());
          join(1, 2, "", interiorTable);
        }
        removeColumn(2); // interiorIndexCol

        // if nLoadOrConVariablesInOuterTable == 0, no need to join it below
        // rearrange the outerTable columns to the loadVariables order
        outerTable.reorderColumns(loadVariableNames, true); // true, remove unrequested columns
        outerTableNColumns = outerTable.nColumns();
        outerTableNRows = outerTable.nRows();

        // if nLoadOrConVariablesInInnerTable == 0, no need to join it below
        // rearrange the innerTable columns to the loadVariables order
        innerTable.reorderColumns(loadVariableNames, true); // true, remove unrequested columns
        innerTableNColumns = innerTable.nColumns();
        innerTableNRows = innerTable.nRows();

        // unknown nLevels=2 file structure
      } else {
        throw new SimpleException(
            errorInMethod
                + "Invalid file (unknown nLevels=2 file structure: indexVar="
                + indexVar
                + ", rowSizeVar="
                + rowSizeVar
                + ").");
      }

      // *** finish up nLevels=2 files
      // first 2 cols of this table are outerTableIndex and innerTableIndex
      if (debugMode) String2.log("  Debug: finish up nLevels=2 files");
      if (ncCFcc != null) ncCFcc.set(94);

      // apply constraints to obs variables
      // (not outer and inner variables, since they were constrained earlier)
      BitSet keep = new BitSet();
      keep.set(0, nRows());
      int cardinality = nRows();
      for (int con = 0; con < nCon; con++) {
        if (ncCFcc != null) ncCFcc.set(95);
        int v = findColumnNumber(conNames.get(con));
        if (v >= 2) { // an obs variable
          cardinality =
              tryToApplyConstraint(
                  -1, conNames.get(con), conOps.get(con), conValues.get(con), keep);
          if (cardinality == 0) {
            if (verbose)
              String2.log(
                  "  readNcCF "
                      + MustBe.THERE_IS_NO_DATA
                      + " (nLevels=2, readAs="
                      + readAs
                      + ", after constraints applied)"
                      + " fileName="
                      + fullName
                      + " time="
                      + (System.currentTimeMillis() - time)
                      + "ms");
            removeAllColumns();
            return;
          }
        }
      }
      if (cardinality < nRows()) {
        if (ncCFcc != null) ncCFcc.set(96);
        justKeep(keep);
      }

      // join to add the innerTable columns
      if (innerTableNColumns > 0) {
        // insert row number in innerTable (to be the key column)
        if (ncCFcc != null) ncCFcc.set(97);
        PrimitiveArray keyPA = new IntArray(0, innerTable.nRows() - 1);
        innerTable.addColumn(0, "keyColumn", keyPA, new Attributes());
        join(1, 1, "", innerTable); // innerTable is lookUpTable
      }
      removeColumn(1); // remove the keyColumn

      // join to add the outerTable columns
      if (outerTableNColumns > 0) {
        // insert row number in outerTable (to be the key column)
        if (ncCFcc != null) ncCFcc.set(98);
        PrimitiveArray keyPA = new IntArray(0, outerTableNRows - 1);
        outerTable.addColumn(0, "keyColumn", keyPA, new Attributes());
        join(1, 0, "", outerTable); // outerTable is lookUpTable
      }
      removeColumn(0); // remove the keyColumn

      // finish up
      tryToApplyConstraintsAndKeep(-1, conNames, conOps, conValues); // may be 0 rows left
      reorderColumns(loadVariableNames, true); // discard others
      decodeCharsAndStrings();
      convertToUnsignedPAs();

      if (ncCFcc != null) ncCFcc.set(99);
      if (reallyVerbose)
        String2.log(
            msg
                + " finished (nLevels=2, readAs="
                + readAs
                + "). nRows="
                + nRows()
                + " nCols="
                + nColumns()
                + " time="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This reads an NCEI-style invalid CF DSG Contiguous Ragged Array .nc file. Suitable sample files
   * are from https://data.nodc.noaa.gov/thredds/catalog/ncei/wod/ See local copies in
   * /erddapTestBig/nccf/wod/
   *
   * @param colNames If empty, this reads all columns. If conNames... specified and this is
   *     specified, this includes all of the conNames.
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param conNames may be null or size=0;
   * @throws Exception if trouble. No matching data is not an error and returns an empty table (0
   *     rows and 0 columns).
   */
  public void readInvalidCRA(
      String fullName,
      StringArray colNames,
      int standardizeWhat,
      StringArray conNames,
      StringArray conOps,
      StringArray conVals)
      throws Exception {

    String msg = "  Table.readInvalidCRA " + fullName;
    long time = System.currentTimeMillis();
    clear();
    if (colNames == null) colNames = new StringArray(1, false);
    // String2.log(NcHelper.ncdump(fullName, "-h"));
    Attributes gridMappingAtts = null;
    NetcdfFile ncFile = NcHelper.openFile(fullName);
    try {

      NcHelper.getGroupAttributes(ncFile.getRootGroup(), globalAttributes());
      Attributes gatts = globalAttributes();

      // ensure featureType=Profile (other single level types could be supported -- need examples)
      String featureType = gatts.getString("featureType");
      featureType = featureType == null ? "" : String2.toTitleCase(featureType);
      Test.ensureEqual(featureType, "Profile", "Unexpected featureType.");

      // find vars with sample_dimension, find outerDimName, largestInnerDim
      String profileIDVarName = null;
      int profileIDVarNumber = -1;
      String outerDimName = null; // e.g., cast
      Dimension outerDim = null;
      String largestInnerDimName = null; // e.g., z_obs
      int largestInnerDimSize = -1;
      List<Variable> varList = ncFile.getVariables();
      int nVars = varList.size();
      String vNames[] = new String[nVars];
      Attributes vatts[] = new Attributes[nVars];
      PAType varPATypes[] = new PAType[nVars];
      boolean isCharArray[] = new boolean[nVars];
      int nDims[] = new int[nVars];
      int realNDims[] = new int[nVars];
      String varMissingValues[] = new String[nVars];
      boolean varHasSampleDimensionAtt[] = new boolean[nVars];
      HashMap<String, PrimitiveArray> rowSizesHM =
          new HashMap(); // e.g., sample_dimension="z_obs" -> z_row_size PA
      for (int v = 0; v < nVars; v++) {
        Variable var = varList.get(v);
        vNames[v] = var.getFullName();
        vatts[v] = new Attributes();
        NcHelper.getVariableAttributes(var, vatts[v]);

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null) {
          gridMappingAtts = NcHelper.getGridMappingAtts(ncFile, vatts[v].getString("grid_mapping"));
          if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);
        }

        varPATypes[v] = NcHelper.getElementPAType(var); // exception if trouble
        nDims[v] = var.getRank();
        isCharArray[v] = nDims[v] > 0 && varPATypes[v] == PAType.CHAR;
        if (varPATypes[v] == PAType.CHAR)
          varPATypes[v] = PAType.STRING; // assume all char -> string
        realNDims[v] = nDims[v] - (isCharArray[v] ? 1 : 0);
        varMissingValues[v] = vatts[v].getString("_FillValue");
        if (varMissingValues[v] == null) varMissingValues[v] = vatts[v].getString("missing_value");
        if (varMissingValues[v] == null) varMissingValues[v] = "";

        // profile_id?
        String cfRole = vatts[v].getString("cf_role");
        if (cfRole != null) cfRole = cfRole.toLowerCase();
        if ("profile_id".equals(cfRole)) {
          if (debugMode) msg += "\n>> found cf_role=profile_id for var=" + vNames[v];
          if (profileIDVarName != null)
            throw new RuntimeException(
                "Two variables have the attribute cf_role=profile_id: "
                    + profileIDVarName
                    + " and "
                    + vNames[v]
                    + ".");
          profileIDVarName = vNames[v];
          profileIDVarNumber = v;

          Test.ensureEqual(realNDims[v], 1, "Unexpected number of dimensions for var=" + vNames[v]);
          Dimension tOuterDim = var.getDimension(0);
          String tOuterDimName = tOuterDim.getName();
          Test.ensureNotNull(tOuterDimName, "Unexpected dimensionName=null for var=" + vNames[v]);
          if (outerDimName == null) {
            outerDimName = tOuterDimName;
            outerDim = tOuterDim;
          } else {
            Test.ensureEqual(
                tOuterDimName, outerDimName, "Unexpected dimension for var=" + vNames[v]);
          }
        }

        // gather sample_dimension atts and row_size PAs
        // INVALID FILES: Primary_Investigator_rowsize doesn't have sample_dimension: so fix it
        //  See "numberofpis" below for additional, related fixes.
        String sampleDimension =
            vNames[v].equals("Primary_Investigator_rowsize")
                ? "numberofpis"
                : vatts[v].getString("sample_dimension");
        if (sampleDimension != null) {
          if (debugMode)
            msg += "\n>> found sample_dimension=" + sampleDimension + " for var=" + vNames[v];
          varHasSampleDimensionAtt[v] = true;

          // get/verify outerDimName, e.g., casts
          Test.ensureEqual(realNDims[v], 1, "Unexpected number of dimensions for var=" + vNames[v]);
          Dimension tOuterDim = var.getDimension(0);
          String tOuterDimName = tOuterDim.getName();
          Test.ensureNotNull(tOuterDimName, "Unexpected dimensionName=null for var=" + vNames[v]);
          if (outerDimName == null) {
            outerDimName = tOuterDimName;
            outerDim = tOuterDim;
          } else {
            Test.ensureEqual(
                tOuterDimName, outerDimName, "Unexpected dimension for var=" + vNames[v]);
          }

          // get rowSizes
          rowSizesHM.put(sampleDimension, NcHelper.getPrimitiveArray(var));
          // no need to unpack rowSizes

          // is it the largest sample_dimension (e.g., z_obs)?
          Dimension dim = ncFile.findDimension(sampleDimension);
          int tSize = dim.getLength();
          if (tSize > largestInnerDimSize) {
            largestInnerDimSize = tSize;
            largestInnerDimName = sampleDimension;
          }
        }
      }
      Test.ensureNotNull(
          profileIDVarName, "No variable was found with a cf_role=profile_id attribute.");
      Test.ensureTrue(outerDimName != null, "No outer dimension (e.g., casts) found!");
      Test.ensureTrue(
          largestInnerDimSize > 0,
          "No row_size variables with a sample_dimension attribute (e.g., z_obs) were found!");
      int outerDimSize = outerDim.getLength();

      // read the outerDim vars (including scalars and primary_investigator)
      if (debugMode) msg += "\n>> read the outerDim";
      Table outerTable = new Table();
      StringArray cdm_profile_variables = new StringArray();
      for (int v = 0; v < nVars; v++) {
        Variable var = varList.get(v);
        Attributes vatt = vatts[v];

        // if varName isn't in colNames, skip it
        if (colNames.size() > 0 && colNames.indexOf(vNames[v]) < 0) continue;

        // scalar?   e.g., crs
        if (realNDims[v] == 0) {
          if (debugMode) msg += "\n>> found scalar var=" + vNames[v];

          if ("crs".equals(vNames[v])) {
            // if crs, ignore value and promote to vatts to gatt
            String[] vattNames = vatt.getNames();
            int vattSize = vattNames.length;
            for (int va = 0; va < vattSize; va++)
              gatts.add(vNames[v] + "_" + vattNames[va], vatt.get(vattNames[va]));
          } else {
            // add to outerTable
            outerTable.addColumn(
                outerTable.nColumns(), vNames[v], NcHelper.getPrimitiveArray(var), vatt);
            outerTable.standardizeLastColumn(standardizeWhat);
          }
          continue;

        } else if (realNDims[v] == 1) {
          Dimension dim = var.getDimension(0);
          String dimName = dim.getName();

          // FIX flaws related to Primary_Investigator / numberofpis.
          // Treat numberofpis differently than other dimNames:
          //  convert PI's for each cast into /-separated string since some have internal ';'.
          if (dimName.equals("numberofpis")) {
            PrimitiveArray rowSizesPA = rowSizesHM.get(dimName);
            Test.ensureNotNull(
                rowSizesPA, "No row_size info for dim=" + dimName + " for var=" + vNames[v]);
            PrimitiveArray varPA = NcHelper.getPrimitiveArray(var);
            StringArray newPA = new StringArray(outerDimSize, false);
            PrimitiveArray tSubset = null;
            int thisPo = 0;
            for (int outer = 0; outer < outerDimSize; outer++) {
              int thisChunkSize = rowSizesPA.getInt(outer);
              tSubset = varPA.subset(tSubset, thisPo, 1, thisPo + thisChunkSize - 1);
              // usually just 0 or 1 pi's, gld has more
              String tts = String2.toSVString(tSubset.toStringArray(), "/", false);
              newPA.add(tts);
              thisPo += thisChunkSize;
            }
            // add to outerTable
            outerTable.addColumn(outerTable.nColumns(), vNames[v], newPA, vatt);
            outerTable.standardizeLastColumn(standardizeWhat);
            continue;
          }

          if (!outerDimName.equals(dimName)) continue;
          if (varHasSampleDimensionAtt[v])
            // it's a row_size var with a sample_dimension att
            continue;

          // add to outerTable
          outerTable.addColumn(
              outerTable.nColumns(),
              vNames[v],
              NcHelper.getPrimitiveArray(var),
              vatt); // char -> String
          outerTable.standardizeLastColumn(standardizeWhat);
        }
      }

      // expand scalars to same number of rows as outerDim
      ensureColumnsAreSameSize_LastValue();

      // set cdm_profile_variables
      String cpv = outerTable.getColumnNamesCSSVString();
      cpv = String2.replaceAll(cpv, ", lat,", ", latitude,");
      cpv = String2.replaceAll(cpv, ", lon,", ", longitude,");
      cpv = String2.replaceAll(cpv, ", z,", ", depth,");
      // the numeric time variable is called time
      if (cpv.length() > 0) gatts.set("cdm_profile_variables", cpv);

      // apply constraints, but keep all rows
      if (debugMode) String2.log(">> apply constraints");
      BitSet keepOuter = new BitSet();
      keepOuter.set(0, outerDimSize);
      if (outerTable.nRows() > 0
          && outerTable.tryToApplyConstraints(
                  profileIDVarNumber, conNames, conOps, conVals, keepOuter)
              == 0) {
        // No matching data is not an error and returns an empty table (0 rows and 0 columns).
        if (reallyVerbose)
          msg +=
              " finished. "
                  + MustBe.THERE_IS_NO_DATA
                  + " Just outer table. time"
                  + (System.currentTimeMillis() - time)
                  + "ms";
        return;
      }
      boolean keepAllOuter = keepOuter.nextClearBit(0) == -1;

      // are we done?
      if (debugMode) msg += "\n>> are we done because just outer columns?";
      if (colNames.size() > 0) {
        // are all colNames in outerTable?
        boolean done = true;
        for (int col = 0; col < colNames.size(); col++) {
          if (outerTable.findColumnNumber(colNames.get(col)) < 0) {
            done = false;
            break;
          }
        }

        if (done) {
          ncFile.close();
          outerTable.justKeep(keepOuter);
          outerTable.reorderColumns(colNames, true);
          outerTable.decodeCharsAndStrings();
          outerTable.convertToUnsignedPAs();

          // copy outerTable to this table
          for (int col = 0; col < outerTable.nColumns(); col++)
            addColumn(
                col,
                outerTable.getColumnName(col),
                outerTable.getColumn(col),
                outerTable.columnAttributes(col));

          if (reallyVerbose)
            msg +=
                " finished. Just outer table. nRows="
                    + nRows()
                    + " nCols="
                    + nColumns()
                    + " time="
                    + (System.currentTimeMillis() - time)
                    + "ms";
          return;
        }
      }

      // find nActiveInnerRows and largestLargestChunkSize
      PrimitiveArray largestRowSizes = rowSizesHM.get(largestInnerDimName);
      int checkNTotalInnerRows = 0;
      int nActiveInnerRows = 0;
      int largestLargestChunkSize = 0;
      for (int outer = 0; outer < outerDimSize; outer++) {
        int tChunkSize = largestRowSizes.getInt(outer);
        checkNTotalInnerRows += tChunkSize;
        if (!keepOuter.get(outer)) // skip this outer
        continue;
        nActiveInnerRows += tChunkSize;
        largestLargestChunkSize = Math.max(largestLargestChunkSize, tChunkSize);
      }
      if (checkNTotalInnerRows != largestInnerDimSize)
        throw new RuntimeException(
            "Invalid file: The sum of the row sizes for "
                + largestInnerDimName
                + " ("
                + checkNTotalInnerRows
                + ") doesn't equal the size of dimension="
                + largestInnerDimName
                + " ("
                + largestInnerDimSize
                + ").");

      // expand the kept outerTable rows in this table
      for (int col = 0; col < outerTable.nColumns(); col++) {
        String tName = outerTable.getColumnName(col);

        // if varName isn't in colNames, skip it
        if (colNames.size() > 0 && colNames.indexOf(tName) < 0) continue;
        if (debugMode) msg += "\n>> expand outerTable into this table, var=" + tName;

        PrimitiveArray otpa = outerTable.getColumn(col);
        PrimitiveArray newPA = PrimitiveArray.factory(otpa.elementType(), nActiveInnerRows, false);
        addColumn(nColumns(), tName, newPA, outerTable.columnAttributes(col));

        for (int outer = 0; outer < outerDimSize; outer++) {

          if (!keepOuter.get(outer)) {
            // skip this outer, e.g., this cast
            continue;
          }

          // addN of that value
          newPA.addNStrings(largestRowSizes.getInt(outer), otpa.getString(outer));
        }
      }
      outerTable = null; // encourage gc

      // make the innerTable in this table
      for (int v = 0; v < nVars; v++) {
        Variable var = varList.get(v);
        Attributes vatt = vatts[v];

        // if varName isn't in colNames, skip it
        if (colNames.size() > 0 && colNames.indexOf(vNames[v]) < 0) continue;

        // find 1D vars where dim isn't outerDimName, e.g., Temperature
        if (realNDims[v] != 1) continue;
        Dimension dim = var.getDimension(0);
        String dimName = dim.getName();
        if (outerDimName.equals(dimName)
            || // already in outer table?
            dimName.equals("numberofpis")) continue;

        // Let's do this!
        if (debugMode) msg += "\n>> make the innerTable in this table, v[" + v + "]=" + vNames[v];

        // get the row_size info
        PrimitiveArray rowSizesPA = rowSizesHM.get(dimName);
        Test.ensureNotNull(
            rowSizesPA, "No row_size info for dim=" + dimName + " for var=" + vNames[v]);

        // get the var's data
        PrimitiveArray varPA = NcHelper.getPrimitiveArray(var);
        varPA = vatts[v].standardizeVariable(standardizeWhat, vNames[v], varPA);

        // can we use varPA as is?
        if (keepAllOuter && varPA.size() == largestInnerDimSize) {
          addColumn(nColumns(), vNames[v], varPA, vatts[v]);
          continue;
        }

        // build newPA, with correct size for each cast, and just keep the keepOuter chunks
        PrimitiveArray newPA = PrimitiveArray.factory(varPATypes[v], nActiveInnerRows, false);

        int thisPo = 0;
        PrimitiveArray tSubset = null;
        for (int outer = 0; outer < outerDimSize; outer++) {
          int largestChunkSize = largestRowSizes.getInt(outer);
          int thisChunkSize = rowSizesPA.getInt(outer);

          if (!keepOuter.get(outer)) {
            // skip this outer
            thisPo += thisChunkSize;
            continue;
          }

          if (thisChunkSize == 0) {
            // This deals with the main invalid part of these files.
            // add missing values to newPA
            newPA.addNStrings(largestChunkSize, varMissingValues[v]);

          } else if (thisChunkSize == largestChunkSize) {
            // copy values into newPA
            tSubset = varPA.subset(tSubset, thisPo, 1, thisPo + thisChunkSize - 1);
            newPA.append(tSubset);

            // only need to do once, but not extant till here
            tSubset.clear(); // speeds up ensureCapacity
            tSubset.ensureCapacity(largestLargestChunkSize);

          } else {
            throw new RuntimeException(
                "For "
                    + outerDimName
                    + "["
                    + outer
                    + "], the number of "
                    + dimName
                    + " values ("
                    + thisChunkSize
                    + ") doesn't equal the number of "
                    + largestInnerDimName
                    + " values ("
                    + largestChunkSize
                    + ").");
          }
          thisPo += thisChunkSize;
        }
        if (thisPo != varPA.size())
          throw new RuntimeException(
              "Invalid file: The sum of the row sizes for "
                  + dimName
                  + " ("
                  + thisPo
                  + ") doesn't equal the size of dimension="
                  + dimName
                  + " ("
                  + varPA.size()
                  + ").");

        addColumn(nColumns(), vNames[v], newPA, vatts[v]);
      }

      // finish up
      // apply all constraints
      tryToApplyConstraintsAndKeep(
          findColumnNumber(profileIDVarName), // may be -1
          conNames,
          conOps,
          conVals); // may be 0 rows left

      // put in colNames order
      if (colNames.size() > 0) reorderColumns(colNames, true);

      // we're done
      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nRows="
                + nRows()
                + " nCols="
                + nColumns()
                + " time="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * This reads data from an audio file (AudioFileFormat.Type: AU, WAVE, SND (not tested), and
   * AIFF/AIFF-C (should work but aren't supported)). See
   * https://docs.oracle.com/javase/tutorial/sound/converters.html There may be other classes/jars
   * from other groups that support other file types.
   *
   * @param readData This method always reads metadata. If readData=true, this also reads the data.
   * @param addElapsedTimeColumn if true, column[0] will be elapsedTime with double values in
   *     seconds (starting at 0)
   * @throws Exception if trouble
   */
  public void readAudioFile(String fullName, boolean readData, boolean addElapsedTimeColumn)
      throws Exception {
    // FUTURE: read audio waveform data by using Java methods to convert other formats to PCM
    // See also https://howlerjs.com/

    int totalFramesRead = 0;

    clear();
    AudioInputStream audioInputStream = null;
    DataInputStream dis = null;
    String msg = "  Table.readAudioFile " + fullName;
    String errorWhile = String2.ERROR + " in" + msg + ": ";
    try {
      long startTime = System.currentTimeMillis();

      java.io.File audioFile = new java.io.File(fullName);
      if (debugMode) {
        AudioFileFormat aff = AudioSystem.getAudioFileFormat(audioFile);
        msg += "\naff.properties()=" + aff.properties().toString();
      }

      audioInputStream = AudioSystem.getAudioInputStream(audioFile);

      // get the metadata
      AudioFormat af = audioInputStream.getFormat();

      int bytesPerFrame = af.getFrameSize();
      if (bytesPerFrame == AudioSystem.NOT_SPECIFIED || bytesPerFrame <= 0) {
        // some audio formats may have unspecified frame size
        // in that case we may read any amount of bytes
        bytesPerFrame = 1;
      }
      int nChannels = af.getChannels();
      int nBits = af.getSampleSizeInBits();
      // AudioFormat.Encoding: ALAW, PCM_FLOAT, PCM_SIGNED, PCM_UNSIGNED, ULAW
      String encoding = af.getEncoding().toString();
      boolean isALAW = encoding.equals("ALAW");
      boolean isULAW = encoding.equals("ULAW");
      boolean isPcmFloat = encoding.equals("PCM_FLOAT");
      boolean isPcmUnsigned = encoding.equals("PCM_UNSIGNED");
      boolean isPcmSigned = encoding.equals("PCM_SIGNED");
      boolean isBigEndian = af.isBigEndian();
      boolean is24Bit = nBits == 24;
      if (!isALAW && !isULAW && !isPcmFloat && !isPcmUnsigned && !isPcmSigned) {
        throw new SimpleException(errorWhile + "Unsupported audioSampleSizeInBits=" + nBits + ".");
      }
      int frameSize = af.getFrameSize();
      globalAttributes.set("audioBigEndian", "" + isBigEndian);
      globalAttributes.set("audioChannels", nChannels);
      globalAttributes.set("audioEncoding", encoding);
      globalAttributes.set(
          "audioFrameRate", af.getFrameRate()); // may be different than sampleRate if compressed
      globalAttributes.set("audioFrameSize", frameSize); // bytes
      globalAttributes.set("audioSampleRate", af.getSampleRate());
      globalAttributes.set("audioSampleSizeInBits", nBits);
      Map props = af.properties();
      Iterator it = props.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry pair = (Map.Entry) it.next();
        // use prefix=audio_ to distinguish these props from method values above
        // and to avoid clash if same name
        // but map common properties to CF terms if possible
        // (see list in Javadocs for AudioFileFormat)
        String key = pair.getKey().toString();
        if (key.equals("author")) key = "creator_name";
        else if (key.equals("comment")) key = "summary";
        else if (key.equals("copyright")) key = "license";
        else if (key.equals("date")) key = "date_created";
        else if (key.equals("title")) key = "title";
        else key = "audio_" + key;
        globalAttributes.set(key, PrimitiveArray.factory(pair.getValue()));
      }

      // BUG in java:
      //  https://bugs.openjdk.java.net/browse/JDK-8038138
      //  And this causes incorrect getFrameLength()
      //  https://bugs.openjdk.java.net/browse/JDK-8038139
      //  with String2.unitTestDataDir + "audio/wav/aco_acoustic.20141119_000000.wav"
      //  file has 24000 samples/sec * 300 sec = 7,200,000 samples and 4 bytes/sample,
      //  but lNFrames is 28,000,000!  (should be 7,200,000)
      // so treat it as nBytes but this is TROUBLE!
      long lNFrames = audioInputStream.getFrameLength();
      if (lNFrames * frameSize > File2.length(fullName)) // a more emprical test than isPcmFloat
      lNFrames /= frameSize;
      if (lNFrames >= Integer.MAX_VALUE)
        throw new SimpleException(
            errorWhile
                + "The file's nFrames="
                + lNFrames
                + " is greater than maxAllowed="
                + (Integer.MAX_VALUE - 1)
                + ".");
      int nFrames = Math2.narrowToInt(lNFrames);

      // make the columns
      PrimitiveArray pa[] = new PrimitiveArray[nChannels];
      int tSize = readData ? nFrames : 1;
      for (int c = 0; c < nChannels; c++) {
        if (isPcmFloat && af.isBigEndian()) { // java is bigEndian
          // if littleEndian, store in integer type (below), then reverseBytes, then convert to
          // float
          if (nBits == 32) pa[c] = new FloatArray(tSize, false);
          else if (nBits == 64) pa[c] = new DoubleArray(tSize, false);
          else
            throw new SimpleException(
                errorWhile + "Unsupported audioSampleSizeInBits=" + nBits + ".");
        } else {
          if (nBits == 8) pa[c] = new ByteArray(tSize, false);
          // 12 is not supported yet because each value is 1.5 bytes -- hard to deal with when >1
          // channel in a dis
          else if (nBits == 16) pa[c] = new ShortArray(tSize, false);
          else if (nBits == 24) pa[c] = new IntArray(tSize, false);
          else if (nBits == 32) pa[c] = new IntArray(tSize, false);
          else if (nBits == 64) pa[c] = new LongArray(tSize, false);
          else
            throw new SimpleException(
                errorWhile + "Unsupported audioSampleSizeInBits=" + nBits + ".");
        }
        addColumn(
            c,
            "channel_" + (c + 1),
            pa[c],
            new Attributes().add("long_name", "Channel " + (c + 1)));
      }

      Attributes elapsedTimeAtts = null;
      if (addElapsedTimeColumn)
        elapsedTimeAtts = new Attributes().add("long_name", "Elapsed Time").add("units", "seconds");

      // return with just metadata?
      if (!readData) {
        audioInputStream.close();
        audioInputStream = null;

        // make simple versions of changes below
        for (int c = 0; c < nChannels; c++) {
          if (isALAW || isULAW) {
            setColumn(c, new ShortArray());
          } else if (!af.isBigEndian() && isPcmFloat) { // 24 bit was done when read
            if (nBits <= 32) setColumn(c, new FloatArray());
            else if (nBits <= 64) setColumn(c, new DoubleArray());
          }
        }
        if (addElapsedTimeColumn) addColumn(0, ELAPSED_TIME, new DoubleArray(), elapsedTimeAtts);
        return;
      }

      // get the data
      dis = new DataInputStream(new BufferedInputStream(audioInputStream));
      if (is24Bit) {
        if (nChannels == 1) {
          ((IntArray) pa[0]).read24BitDisAudio(dis, nFrames, isBigEndian);
        } else {
          IntArray ia[] = new IntArray[nChannels];
          for (int c = 0; c < nChannels; c++) ia[c] = (IntArray) pa[c];
          for (int f = 0; f < nFrames; f++)
            for (int c = 0; c < nChannels; c++) ia[c].read24BitDisAudio(dis, 1, isBigEndian);
        }

      } else if (nChannels == 1) {
        pa[0].readDis(dis, nFrames);

      } else {
        for (int f = 0; f < nFrames; f++) for (int c = 0; c < nChannels; c++) pa[c].readDis(dis, 1);
      }

      // clean up the data
      // 2020-08-11 I tried reading directly into unsigned PAs, but trouble (didn't pursue it), so I
      // reverted
      if (isPcmUnsigned) {
        // what were read as signed:    0  127 -128  -1
        // should become   unsigned: -128   -1    0 127
        for (int c = 0; c < nChannels; c++) pa[c].changeSignedToFromUnsigned();
      }

      if (isALAW) {
        // convert encoded byte to short
        // see
        // https://stackoverflow.com/questions/26824663/how-do-i-use-audio-sample-data-from-java-sound
        // I'M NOT CONVINCED THIS IS EXACTLY RIGHT. IT SOUNDS BETTER DIRECTLY IN BROWSER.
        // FUTURE: USE JAVA AUDIO STREAMS TO DO THE CONVERSION TO PCM
        double d0 = 1.0 + Math.log(87.7);
        double d1 = 1.0 / d0;
        double d2 = d0 / 87.7;
        double sm1 = (Short.MAX_VALUE * 16) - 1; // *16 because too small otherwise (???!!!)
        for (int c = 0; c < nChannels; c++) {
          ByteArray ba = (ByteArray) pa[c];
          ShortArray sa = new ShortArray(nFrames, true);
          short sar[] = sa.array;
          setColumn(c, sa);
          long lmin = Long.MAX_VALUE;
          long lmax = Long.MIN_VALUE;
          int min = Integer.MAX_VALUE;
          int max = Integer.MIN_VALUE;
          for (int f = 0; f < nFrames; f++) {
            long tl = (ba.array[f] & 0xffL) ^ 0x55L;
            if ((tl & 0x80L) == 0x80L) tl = -(tl ^ 0x80L);
            lmin = Math.min(lmin, tl);
            lmax = Math.max(lmax, tl);
            double sample = tl / 255.0;
            double signum = Math.signum(sample);
            sample = Math.abs(sample);
            if (sample < d1) sample = sample * d2;
            else sample = Math.exp((sample * d0) - 1.0) / 87.7;
            sample = signum * sample;
            sar[f] = Math2.roundToShort(sm1 * sample); // sample is +/-1
            min = Math.min(min, sar[f]);
            max = Math.max(max, sar[f]);
          }
          if (reallyVerbose)
            msg +=
                "\n col="
                    + c
                    + " inMin="
                    + lmin
                    + " inMax="
                    + lmax
                    + " outMin="
                    + min
                    + " outMax="
                    + max;
        }

      } else if (isULAW) {
        // convert encoded byte to short
        // see
        // https://stackoverflow.com/questions/26824663/how-do-i-use-audio-sample-data-from-java-sound
        // I'M NOT CONVINCED THIS IS EXACTLY RIGHT. IT SOUNDS BETTER DIRECTLY IN BROWSER.
        // FUTURE: USE JAVA AUDIO STREAMS TO DO THE CONVERSION TO PCM
        double d0 = 1.0 / 255.0;
        double sm1 = (Short.MAX_VALUE * 16) - 1; // *16 because too small otherwise (???!!!)
        for (int c = 0; c < nChannels; c++) {
          ByteArray ba = (ByteArray) pa[c];
          ShortArray sa = new ShortArray(nFrames, true);
          short sar[] = sa.array;
          setColumn(c, sa);
          long lmin = Long.MAX_VALUE;
          long lmax = Long.MIN_VALUE;
          int min = Integer.MAX_VALUE;
          int max = Integer.MIN_VALUE;
          for (int f = 0; f < nFrames; f++) {
            long tl = (ba.array[f] & 0xffL) ^ 0xffL;
            if ((tl & 0x80L) == 0x80L) tl = -(tl ^ 0x80L);
            lmin = Math.min(lmin, tl);
            lmax = Math.max(lmax, tl);
            double sample = tl / 255.0;
            sample = Math.signum(sample) * d0 * (Math.pow(256.0, Math.abs(sample)) - 1.0);
            sar[f] = Math2.roundToShort(sm1 * sample);
            min = Math.min(min, sar[f]);
            max = Math.max(max, sar[f]);
          }
          if (reallyVerbose)
            msg +=
                "\n  col="
                    + c
                    + " inMin="
                    + lmin
                    + " inMax="
                    + lmax
                    + " outMin="
                    + min
                    + " outMax="
                    + max;
        }

      } else if (!af.isBigEndian() && !is24Bit) { // 24 bit was done when read
        // reverse the bytes?
        for (int c = 0; c < nChannels; c++) {
          pa[c].reverseBytes(); // this works because they are only integer-type arrays

          // if littleEndian and float, convert to float
          if (isPcmFloat) {
            // if littleEndian, store in integer type, then reverseBytes, then convert to float
            if (nBits <= 32) {
              FloatArray fa = new FloatArray(nFrames, true);
              float far[] = fa.array;
              for (int f = 0; f < nFrames; f++) far[f] = Float.intBitsToFloat(pa[c].getInt(f));
              pa[c] = fa;
              setColumn(c, fa);
            } else if (nBits <= 64) {
              DoubleArray da = new DoubleArray(nFrames, true);
              double dar[] = da.array;
              for (int f = 0; f < nFrames; f++) dar[f] = Double.longBitsToDouble(pa[c].getLong(f));
              pa[c] = da;
              setColumn(c, da);
            }
          }
        }
      }

      // add elapsedTime
      if (addElapsedTimeColumn) {
        double sampleRate = af.getSampleRate(); // double so calculations below as double
        DoubleArray da = new DoubleArray(nFrames, true);
        double dar[] = da.array;
        for (int f = 0; f < nFrames; f++)
          dar[f] = f / sampleRate; // that's the most precise way to calculate it
        addColumn(0, ELAPSED_TIME, da, elapsedTimeAtts);
      }

      // close dis
      dis.close();
      dis = null;
      audioInputStream = null;
      if (reallyVerbose)
        msg +=
            " finished. nRows="
                + nFrames
                + " nChannels="
                + nChannels
                + " encoding="
                + encoding
                + " isBigEndian="
                + isBigEndian
                + " nBits="
                + nBits
                + " isPcmFloat="
                + isPcmFloat
                + " time="
                + (System.currentTimeMillis() - startTime)
                + "ms";

    } catch (Exception e) {
      try {
        if (dis != null) {
          dis.close();
          dis = null;
          audioInputStream = null;
        }
      } catch (Exception e2) {
      }
      try {
        if (audioInputStream != null) audioInputStream.close();
      } catch (Exception e2) {
      }

      // clear()?
      if (!reallyVerbose) String2.log(msg);
      throw e;

    } finally {
      if (reallyVerbose) String2.log(msg);
    }
  }

  /**
   * This writes the data to a PCM_SIGNED (for int types) or PCM_FLOAT (for float types) WAVE file.
   * All columns must be of the same type. This writes to a temporary file, then renames to correct
   * name. Long data is always written as int's because wav doesn't seem to support it. UNTIL JAVA
   * 9, BECAUSE OF JAVA BUG, this writes float/double data as int's.
   *
   * @param fullOutName the dir + name + .wav for the .wav file
   * @throws Exception if trouble. If trouble, this makes an effort not to leave any (partial) file.
   */
  public void writeWaveFile(String fullOutName) throws Exception {

    // this method creates file with the data
    //  and at the end it calls the other writeWaveFile method to create fullOutName
    long startTime = System.currentTimeMillis();
    String errorWhile = String2.ERROR + " while writing .wav file=" + fullOutName + ": ";
    int nCol = nColumns();
    int nRow = nRows();
    Test.ensureTrue(nCol > 0 && nRow > 0, errorWhile + "There is no data.");
    String tPAType = getColumn(0).elementTypeString();
    int randomInt = Math2.random(Integer.MAX_VALUE);
    String fullInName = fullOutName + ".data" + randomInt;
    File2.delete(fullOutName);

    // gather pa[] and just deal with that (so changes don't affect the table)
    Test.ensureTrue(
        !tPAType.equals("String") && !tPAType.equals("char"),
        errorWhile + "All data columns must be numeric.");
    PrimitiveArray pa[] = new PrimitiveArray[nCol];
    for (int c = 0; c < nCol; c++) {
      pa[c] = columns.get(c);
      Test.ensureEqual(
          tPAType,
          pa[c].elementTypeString(),
          errorWhile + "All data columns must be of the same data type.");
    }

    // TEMPORARY: DEAL WITH JAVA 1.8 BUG:
    // Java code can't write PCM_FLOAT: says it is PCM_SIGNED instead
    // https://bugs.openjdk.java.net/browse/JDK-8064800
    // This is said to be fixed in Java 9
    boolean java8 = System.getProperty("java.version").startsWith("1.8.");
    if (java8 && (tPAType.equals("long") || tPAType.equals("float") || tPAType.equals("double"))) {

      // get the range of all columns
      double min = Double.MAX_VALUE;
      double max = -Double.MAX_VALUE; // min_value is close to 0
      for (int c = 0; c < nCol; c++) {
        // get range
        PrimitiveArray da = pa[c];
        double[] stats = da.calculateStats();
        min = Math.min(min, stats[PrimitiveArray.STATS_MIN]);
        max = Math.max(max, stats[PrimitiveArray.STATS_MAX]);
      }
      double range = max - min;

      // save as int (scaled into int range)
      for (int c = 0; c < nCol; c++) {
        PrimitiveArray da = pa[c];
        IntArray ia = new IntArray(nRow, false);
        for (int row = 0; row < nRow; row++) {
          double d = da.getDouble(row);
          if (Double.isNaN(d)) ia.add(0);
          else ia.add(Math2.roundToInt(-2000000000.0 + ((d - min) / range) * 4000000000.0));
        }
        pa[c] = ia;
      }
      tPAType = "int";
    }

    // write the data to a file (slower, but saving memory is important here)
    // FUTURE: isn't there a way to open the wav file, write data to it, then close it?
    //  very low level:
    // https://stackoverflow.com/questions/5810164/how-can-i-write-a-wav-file-from-byte-array-in-java
    DataOutputStream dos =
        new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fullInName)));
    try {
      for (int row = 0; row < nRow; row++)
        for (int col = 0; col < nCol; col++) pa[col].writeDos(dos, row);
      dos.close();
      dos = null;

    } catch (Exception e) {
      if (dos != null) {
        dos.close();
        File2.delete(fullInName);
      }
      throw e;
    }

    // make the wav file
    writeWaveFile(
        fullInName,
        nCol,
        nRow,
        tPAType,
        globalAttributes,
        randomInt,
        fullOutName,
        startTime); // throws Exception

    // delete the temp in file
    File2.delete(fullInName);
  }

  /**
   * This STATIC method writes the data to a PCM_SIGNED (for integer types) or PCM_FLOAT (for float
   * types) WAVE file. All columns must be of the same type. This writes to a temporary file, then
   * renames to correct name. If neither the audioFrameRate or audioSampleRate are in
   * globalAttributes, a sample rate of 10000Hz will be assumed.
   *
   * @param fullInName the dir + name + ext for the data stream file, ready to go (columns
   *     interspersed).
   * @param nCol
   * @param nRow
   * @param tPAType a numeric type. UNTIL JAVA 9, BECAUSE OF JAVA BUG, this must be an integer type.
   * @param globalAtts
   * @param randomInt
   * @param fullOutName the dir + name + .wav for the .wav file
   * @throws Exception if trouble. If trouble, this makes an effort not to leave any (partial) file.
   */
  public static void writeWaveFile(
      String fullInName,
      int nCol,
      long nRow,
      String tPAType,
      Attributes globalAtts,
      int randomInt,
      String fullOutName,
      long startTime)
      throws Exception {

    File2.delete(fullOutName);
    String msg = "  Table.writeWaveFile " + fullOutName;
    String errorWhile = String2.ERROR + " while writing .wav file=" + fullOutName + ": ";
    boolean isFloat = tPAType.equals("double") || tPAType.equals("float");
    Test.ensureTrue(
        !tPAType.equals("String") && !tPAType.equals("char"),
        errorWhile + "All data columns must be numeric.");

    // gather info. Ensure all columns are same type.

    // gather required metadata and properties (from globalAtts)
    float frameRate = Float.NaN;
    float sampleRate = Float.NaN;
    String keys[] = globalAtts.getNames();
    HashMap<String, Object> props = new HashMap();
    for (int ki = 0; ki < keys.length; ki++) {
      String k = keys[ki];
      if (k.equals("audioBigEndian")) {
      } // don't pass through
      else if (k.equals("audioChannels")) {
      } // don't pass through
      else if (k.equals("audioEncoding")) {
      } // don't pass through
      else if (k.equals("audioFrameRate")) frameRate = globalAtts.getFloat(k);
      else if (k.equals("audioFrameSize")) {
      } // don't pass through
      else if (k.equals("audioSampleRate")) sampleRate = globalAtts.getFloat(k);
      else if (k.equals("audioSampleSizeInBits")) {
      } // don't pass through
      else {
        String k2 = k;
        if (k.startsWith("audio_")) k2 = k.substring(6);
        PrimitiveArray pa = globalAtts.get(k);
        PAType paPAType = pa.elementType();
        if (paPAType.equals(PAType.BYTE)) props.put(k2, Math2.narrowToByte(globalAtts.getInt(k)));
        else if (paPAType.equals(PAType.SHORT))
          props.put(k2, Math2.narrowToShort(globalAtts.getInt(k)));
        else if (paPAType.equals(PAType.INT)) props.put(k2, globalAtts.getInt(k));
        else if (paPAType.equals(PAType.LONG)) props.put(k2, globalAtts.getLong(k));
        else if (paPAType.equals(PAType.FLOAT)) props.put(k2, globalAtts.getFloat(k));
        else if (paPAType.equals(PAType.DOUBLE)) props.put(k2, globalAtts.getDouble(k));
        else props.put(k2, globalAtts.getString(k));
      }
    }

    if (frameRate < 0 || !Float.isFinite(frameRate)) frameRate = sampleRate;
    if (sampleRate < 0 || !Float.isFinite(sampleRate)) sampleRate = frameRate;
    if (sampleRate < 0 || !Float.isFinite(sampleRate)) {
      frameRate = 10000; // a low frame/sampleRate, and a round number
      sampleRate = 10000;
    }

    // https://stackoverflow.com/questions/10991391/saving-audio-byte-to-a-wav-file
    AudioFormat.Encoding encoding =
        isFloat ? AudioFormat.Encoding.PCM_FLOAT : AudioFormat.Encoding.PCM_SIGNED;
    int nBits = PAType.elementSize(tPAType) * 8; // bits per element
    AudioFormat af =
        new AudioFormat(
            encoding,
            sampleRate,
            nBits,
            nCol,
            (nCol * nBits) / 8,
            frameRate,
            true, // bigEndian. My data is bigEndian, but I think Java always swaps and writes
            // littleEndian
            props);

    DataInputStream dis = new DataInputStream(File2.getDecompressedBufferedInputStream(fullInName));
    try {

      // create the .wav
      AudioInputStream ais = new AudioInputStream(dis, af, nRow); // nFrames
      try {
        AudioSystem.write(
            ais, AudioFileFormat.Type.WAVE, new java.io.File(fullOutName + randomInt));
      } finally {
        ais.close();
        ais = null;
      }

      dis.close();
      dis = null;
      File2.rename(fullOutName + randomInt, fullOutName); // throws Exception if trouble

      if (reallyVerbose)
        String2.log(
            msg
                + " finished.  nRows="
                + nRow
                + " nChannels="
                + nCol
                + " encoding="
                + encoding.toString()
                + " isBigEndian=true"
                + " nBits="
                + nBits
                + " isFloat="
                + isFloat
                + " time="
                + (System.currentTimeMillis() - startTime)
                + "ms");

    } catch (Exception e) {
      String2.log(msg);
      if (dis != null)
        try {
          dis.close();
        } catch (Throwable t) {
        }
      ;
      File2.delete(fullOutName + randomInt);
      throw e;
    }
  }

  /**
   * This is a minimalist readNetcdf which just reads specified rows from specified variables from
   * an .nc file and appends them to the current data columns. This doesn't read global attributes
   * or variable attributes. This doesn't unpack packed variables. If the table initially has no
   * columns, this creates columns and reads column names; otherwise it doesn't read column names.
   *
   * @param loadVariables the variables in the .nc file which correspond to the columns in this
   *     table. They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the same, one,
   *     dimension as the first dimension. If this is null or empty, nothing is done.
   * @param firstRow the first row to be appended.
   * @param lastRow the last row to be appended (inclusive). If lastRow is -1, firstRow is ignored
   *     and all of the data will be appended.
   * @throws Exception if trouble
   */
  public void appendNcRows(Variable loadVariables[], int firstRow, int lastRow) throws Exception {

    // if (reallyVerbose) String2.log("Table.appendNcRows firstRow=" + firstRow +
    //    " lastRow=" + lastRow);
    if (loadVariables == null || loadVariables.length == 0) {
      if (verbose) String2.log("Table.appendNcRows: nVariables=0 so nothing done");
      return;
    }

    boolean needToAddColumns = nColumns() == 0;
    for (int col = 0; col < loadVariables.length; col++) {

      // get the data
      Variable variable = loadVariables[col];
      PrimitiveArray pa =
          lastRow == -1
              ? NcHelper.getPrimitiveArray(variable)
              : NcHelper.getPrimitiveArray(variable, firstRow, lastRow);
      // 2016-05-20 If chars in ArrayChar.D1 were read as 1 string,
      //  convert the 1 string into a CharArray
      if (variable.getDataType() == DataType.CHAR
          && variable.getRank() == 1
          && pa instanceof StringArray
          && pa.size() == 1) {
        pa = new CharArray(pa.getString(0).toCharArray());
      }
      if (needToAddColumns) {
        addColumn(variable.getShortName(), pa);
      } else getColumn(col).append(pa);
    }
  }

  /**
   * This appends the okRows from a .nc file. This doesn't read global attributes or variable
   * attributes. This doesn't unpack packed variables. If the table initially has no columns, this
   * creates columns and reads column names; otherwise it doesn't read column names.
   *
   * @param loadVariables the variables to be loaded. They must all be ArrayXxx.D1 or ArrayChar.D2
   *     variables and use the same, one, dimension as the first dimension. This must not be null or
   *     empty. If you have variable names, use ncFile.findVariable(name);
   * @param okRows
   * @throws Exception if trouble
   */
  public void appendNcRows(Variable loadVariables[], BitSet okRows) throws Exception {
    // this is tested in PointSubset

    String errorInMethod = String2.ERROR + " in appendNcRows: ";
    long time = System.currentTimeMillis();

    // get the desired rows   (first call adds pa's to data and adds columnNames)
    int n = okRows.size();
    int firstRow = okRows.nextSetBit(0);
    int nAppendCalls = 0;
    while (firstRow >= 0) {
      // find end of sequence of okRows
      int endRow = firstRow == n - 1 ? n : okRows.nextClearBit(firstRow + 1);
      if (endRow == -1) endRow = n;

      // get the data
      appendNcRows(loadVariables, firstRow, endRow - 1);
      nAppendCalls++;

      // first start of next sequence of okRows
      if (endRow >= n - 1) firstRow = -1;
      else firstRow = okRows.nextSetBit(endRow + 1);
    }
    String2.log(
        "Table.appendNcRows done. nAppendCalls="
            + nAppendCalls
            + " nRows="
            + nRows()
            + " TIME="
            + (System.currentTimeMillis() - time)
            + "ms");
  }

  /**
   * This appends okRows rows from a .nc file by doing one big read and then removing unwanted rows
   * -- thus it may be more suited for opendap since it may avoid huge numbers of separate reads.
   * This doesn't read global attributes or variable attributes. This doesn't unpack packed
   * variables. If the table initially has no columns, this creates columns and reads column names;
   * otherwise it doesn't read column names.
   *
   * @param loadVariables the variables to be loaded. They must all be ArrayXxx.D1 or ArrayChar.D2
   *     variables and use the same, one, dimension as the first dimension. This must not be null or
   *     empty.
   * @param okRows
   * @throws Exception if trouble
   */
  public void blockAppendNcRows(Variable loadVariables[], BitSet okRows) throws Exception {
    // this is tested in PointSubset

    String errorInMethod = String2.ERROR + " in blockAppendNcRows: ";
    long time = System.currentTimeMillis();

    // !!****THIS HASN'T BEEN MODIFIED TO DO BLOCK READ YET

    // get the desired rows   (first call adds pa's to data and adds columnNames)
    int n = okRows.size();
    int firstRow = okRows.nextSetBit(0);
    int nAppendCalls = 0;
    while (firstRow >= 0) {
      // find end of sequence of okRows
      int endRow = firstRow == n - 1 ? n : okRows.nextClearBit(firstRow + 1);
      if (endRow == -1) endRow = n;

      // get the data
      appendNcRows(loadVariables, firstRow, endRow - 1);
      nAppendCalls++;

      // first start of next sequence of okRows
      if (endRow >= n - 1) firstRow = -1;
      else firstRow = okRows.nextSetBit(endRow + 1);
    }
    String2.log(
        "Table.blockAppendNcRows done. nAppendCalls="
            + nAppendCalls
            + " nRows="
            + nRows()
            + " TIME="
            + (System.currentTimeMillis() - time)
            + "ms");
  }

  /**
   * This calls convertToStandardMissingValues for all columns.
   *
   * <p>!!!This is used inside the readXxx methods. And this is used inside saveAsXxx methods to
   * convert fake missing values back to NaNs. It is rarely called elsewhere.
   */
  public void convertToStandardMissingValues() {
    int nColumns = nColumns();
    for (int col = 0; col < nColumns; col++) convertToStandardMissingValues(col);
  }

  /**
   * If this column has _FillValue and/or missing_value attributes, those values are converted to
   * standard PrimitiveArray-style missing values and the attributes are removed.
   *
   * <p>!!!This is used inside the readXxx methods. And this is used inside saveAsXxx methods to
   * convert fake missing values back to NaNs. It is rarely called elsewhere.
   *
   * @param column
   */
  public void convertToStandardMissingValues(int column) {
    Attributes colAtt = columnAttributes(column);
    // double mv = colAtt.getDouble("missing_value");
    // String2.log(">> Table.convert " + getColumnName(column) + "\n" + colAtt.toString());
    int nSwitched =
        getColumn(column)
            .convertToStandardMissingValues(
                colAtt.getString("_FillValue"), colAtt.getString("missing_value"));
    // if (!Double.isNaN(mv)) String2.log("  convertToStandardMissingValues mv=" + mv + " n=" +
    // nSwitched);

    // remove current attributes or define new _FillValue
    colAtt.remove("missing_value");
    PrimitiveArray pa = getColumn(column);
    PAType paPAType = pa.elementType();
    if (pa.isFloatingPointType() || paPAType == PAType.STRING) {
      colAtt.remove("_FillValue");
    } else { // integer or char
      colAtt.set("_FillValue", PrimitiveArray.factory(paPAType, 1, ""));
    }
  }

  /**
   * This calls convertToFakeMissingValues for all columns. !!!This is used inside the saveAsXxx
   * methods to temporarily convert to fake missing values. It is rarely called elsewhere.
   */
  public void convertToFakeMissingValues() {
    int nColumns = nColumns();
    for (int col = 0; col < nColumns; col++) convertToFakeMissingValues(col);
  }

  /**
   * This sets (or revises) the missing_value and _FillValue metadata to
   * DataHelper.FAKE_MISSING_VALUE for FloatArray and DoubleArray and the standard missing value
   * (e.g., Xxx.MAX_VALUE) for other PrimitiveArrays. This works on the current (possibly packed)
   * data. So call this when the data is packed. If the column is a StringArray or CharArray column,
   * nothing will be done.
   *
   * <p>!!!This is used inside the saveAsXxx methods to temporarily convert to fake missing values.
   * It is rarely called elsewhere.
   *
   * @param column
   */
  public void convertToFakeMissingValues(int column) {
    // String2.log("Table.convertToFakeMissingValues column=" + column);
    PrimitiveArray pa = getColumn(column);
    PAType paType = pa.elementType();
    if (paType == PAType.STRING || paType == PAType.CHAR) return;
    // boolean removeMVF = false;  //commented out 2010-10-26 so NDBC files have consistent
    // _FillValue
    if (paType == PAType.CHAR) {
      columnAttributes(column).set("missing_value", Character.MAX_VALUE);
      columnAttributes(column).set("_FillValue", Character.MAX_VALUE);
      // removeMVF = ((CharArray)pa).indexOf(Character.MAX_VALUE, 0) < 0;
    } else if (paType == PAType.BYTE) {
      columnAttributes(column).set("missing_value", Byte.MAX_VALUE);
      columnAttributes(column).set("_FillValue", Byte.MAX_VALUE);
      // removeMVF = ((ByteArray)pa).indexOf(Byte.MAX_VALUE, 0) < 0;
    } else if (paType == PAType.UBYTE) {
      columnAttributes(column).set("missing_value", UByteArray.MAX_VALUE);
      columnAttributes(column).set("_FillValue", UByteArray.MAX_VALUE);
    } else if (paType == PAType.SHORT) {
      columnAttributes(column).set("missing_value", Short.MAX_VALUE);
      columnAttributes(column).set("_FillValue", Short.MAX_VALUE);
      // removeMVF = ((ShortArray)pa).indexOf(Short.MAX_VALUE, 0) < 0;
    } else if (paType == PAType.USHORT) {
      columnAttributes(column).set("missing_value", UShortArray.MAX_VALUE);
      columnAttributes(column).set("_FillValue", UShortArray.MAX_VALUE);
    } else if (paType == PAType.INT) {
      columnAttributes(column).set("missing_value", Integer.MAX_VALUE);
      columnAttributes(column).set("_FillValue", Integer.MAX_VALUE);
      // removeMVF = ((IntArray)pa).indexOf(Integer.MAX_VALUE, 0) < 0;
    } else if (paType == PAType.UINT) {
      columnAttributes(column).set("missing_value", UIntArray.MAX_VALUE);
      columnAttributes(column).set("_FillValue", UIntArray.MAX_VALUE);
    } else if (paType == PAType.LONG) {
      columnAttributes(column).set("missing_value", Long.MAX_VALUE);
      columnAttributes(column).set("_FillValue", Long.MAX_VALUE);
      // removeMVF = ((LongArray)pa).indexOf(Long.MAX_VALUE, 0) < 0;
    } else if (paType == PAType.ULONG) {
      columnAttributes(column).set("missing_value", ULongArray.MAX_VALUE);
      columnAttributes(column).set("_FillValue", ULongArray.MAX_VALUE);
    } else if (paType == PAType.FLOAT) {
      columnAttributes(column).set("missing_value", (float) DataHelper.FAKE_MISSING_VALUE);
      columnAttributes(column).set("_FillValue", (float) DataHelper.FAKE_MISSING_VALUE);
      pa.switchFromTo("", "" + DataHelper.FAKE_MISSING_VALUE);
      // removeMVF = ((FloatArray)pa).indexOf((float)DataHelper.FAKE_MISSING_VALUE, 0) < 0; //safer
      // to search, in case values were already fake mv
    } else if (paType == PAType.DOUBLE) {
      columnAttributes(column).set("missing_value", DataHelper.FAKE_MISSING_VALUE);
      columnAttributes(column).set("_FillValue", DataHelper.FAKE_MISSING_VALUE);
      pa.switchFromTo("", "" + DataHelper.FAKE_MISSING_VALUE);
      // removeMVF = ((DoubleArray)pa).indexOf(DataHelper.FAKE_MISSING_VALUE, 0) < 0; //safer to
      // search, in case values were already fake mv
    } else return; // do nothing if e.g., StringArray

    // if there are no mv's, remove missing_value and _FillValue attributes.
    // For example, coordinate columns (lon, lat, depth, time) should have
    // no missing values and hence no missing_value information.
    // Also, there is trouble with new FAKE_MISSING_VALUE (-9999999) being used for
    // climatology time column: it is a valid time.
    // if (removeMVF) {
    //    columnAttributes(column).remove("missing_value");
    //    columnAttributes(column).remove("_FillValue");
    // }

  }

  /**
   * This does the equivalent of a left outer join
   * (https://en.wikipedia.org/wiki/Join_%28SQL%29#Left_outer_join) -- by matching a keyColumn(s) in
   * this table and the first column(s) (the key(s)) in the lookUpTable, the other columns in the
   * lookUpTable are inserted right after keyColumn(s) in this table with the values from the
   * matching row with the matching key(s).
   *
   * <p>The names and attributes of this table's key columns are not changed.
   *
   * <p>The keys are matched via their string representation, so they can have different
   * elementPAType as long as their strings match. E.g., byte, short, int, long, String are usually
   * compatible. Warning: But double = int will probably fail because "1.0" != "1".
   *
   * <p>If the keyCol value(s) isn't found in the lookUpTable, the missing_value attribute (or
   * _FillValue, or "") for the new columns provides the mv.
   *
   * <p>If you don't want to keep the original keyCol, just removeColumn(keyCol) afterwards.
   *
   * <p>If reallyVerbose, this tallies the not matched items and displays the top 50 to String2.log.
   *
   * @param nKeys 1 or more
   * @param keyCol the first key column in this table. If nKeys&gt;1, the others must immediately
   *     follow it.
   * @param mvKey if the key found in this table is "", this mvKey is used (or "" or null if not
   *     needed). E.g., if key="" is found, you might want to treat it as if key="0". If nKeys&gt;1,
   *     mvKey has the parts stored tab separated.
   * @param lookUpTable with its keyCol(s) as column 0+, in the same order as in this table (but not
   *     necessarily with identical columnNames). This method won't change the lookUpTable.
   * @return a 'keep' BitSet indicating which rows were matched (set bits) and which rows had no
   *     match in the lookUpTable (clear bits).
   * @throws RuntimeException if trouble (e.g., nKeys < 1, keyCol is invalid, or lookUpTable is null
   */
  public BitSet join(int nKeys, int keyCol, String mvKey, Table lookUpTable) {
    if (debugMode) {
      String2.log(
          "  Debug: pre join(nKeys="
              + nKeys
              + " keyCol=#"
              + keyCol
              + "="
              + getColumnName(keyCol)
              + " mvKey="
              + mvKey
              + "\n"
              + "    this table:  nColumns="
              + nColumns()
              + ": "
              + getColumnNamesCSSVString());
      ensureValid();
      String2.log(
          "    lookUpTable: nColumns="
              + lookUpTable.nColumns()
              + ": "
              + lookUpTable.getColumnNamesCSSVString());
      lookUpTable.ensureValid();
    }
    if (nKeys < 1)
      throw new RuntimeException(
          String2.ERROR + " in Table.join: nKeys=" + nKeys + " must be at least 1.");
    if (keyCol < 0 || keyCol + nKeys > nColumns())
      throw new RuntimeException(
          String2.ERROR
              + " in Table.join, in this table: nColumns="
              + nColumns()
              + ". keyCol=#"
              + keyCol
              + " + nKeys="
              + nKeys
              + " refers to non-existent columns.");
    if (nKeys == lookUpTable.nColumns()) {
      String2.log(
          "WARNING: in Table.join: nKeys=lookUpTable.nColumns="
              + nKeys
              + ",\n"
              + "  so there are no subsequent columns to be inserted in this table,\n"
              + "  so there is nothing to be done, so returning immediately.\n"
              + "  this table:  nColumns="
              + nColumns()
              + ": "
              + getColumnNamesCSSVString()
              + "\n"
              + "  lookUpTable: nColumns="
              + lookUpTable.nColumns()
              + ": "
              + lookUpTable.getColumnNamesCSSVString());
      // for some tests: throw new RuntimeException("For testing, that Warning is upgraded to be an
      // ERROR.");
    }
    if (nKeys > lookUpTable.nColumns())
      throw new RuntimeException(
          String2.ERROR
              + " in Table.join, in lookUpTable: nColumns="
              + lookUpTable.nColumns()
              + ". nKeys="
              + nKeys
              + ", so lookUpTable has no related information.");
    if (mvKey == null) mvKey = "";
    long time = System.currentTimeMillis();
    Tally notMatchedTally = null;
    if (debugMode) notMatchedTally = new Tally();

    // gather keyPA's
    PrimitiveArray keyPA[] = new PrimitiveArray[nKeys];
    PrimitiveArray lutKeyPA[] = new PrimitiveArray[nKeys];
    for (int key = 0; key < nKeys; key++) {
      keyPA[key] = getColumn(keyCol + key);
      lutKeyPA[key] = lookUpTable.getColumn(key);
    }

    // make hashtable of keys->Integer.valueOf(row#) in lookUpTable
    // so join is fast with any number of rows in lookUpTable
    int lutNRows = lutKeyPA[0].size();
    HashMap<String, Integer> hashMap = new HashMap(Math2.roundToInt(1.4 * lutNRows));
    for (int row = 0; row < lutNRows; row++) {
      StringBuilder sb = new StringBuilder(lutKeyPA[0].getString(row));
      for (int key = 1; key < nKeys; key++) sb.append("\t" + lutKeyPA[key].getString(row));
      hashMap.put(sb.toString(), Integer.valueOf(row));
    }

    // insert columns to be filled
    int nRows = nRows();
    int lutNCols = lookUpTable.nColumns();
    PrimitiveArray lutPA[] = new PrimitiveArray[lutNCols]; // from the lut
    PrimitiveArray newPA[] = new PrimitiveArray[lutNCols]; // paralleling the lutPA
    String mvString[] = new String[lutNCols];
    StringArray insertedColumnNames = new StringArray();
    for (int lutCol = nKeys; lutCol < lutNCols; lutCol++) {
      lutPA[lutCol] = lookUpTable.getColumn(lutCol);
      PAType tPAType = lutPA[lutCol].elementType();
      Attributes lutAtts = lookUpTable.columnAttributes(lutCol);
      mvString[lutCol] = lutAtts.getString("missing_value"); // preferred
      if (mvString[lutCol] == null) {
        mvString[lutCol] = lutAtts.getString("_FillValue"); // second choice
        if (mvString[lutCol] == null) mvString[lutCol] = ""; // default
      }

      newPA[lutCol] = PrimitiveArray.factory(tPAType, nRows, mvString[lutCol]);
      String colName = lookUpTable.getColumnName(lutCol);
      insertedColumnNames.add(colName);
      addColumn(keyCol + lutCol, colName, newPA[lutCol], (Attributes) lutAtts.clone());
    }

    // fill the new columns by matching the looking up the key value
    int nMatched = 0;
    BitSet matched = new BitSet();
    matched.set(0, nRows); // all true
    for (int row = 0; row < nRows; row++) {
      StringBuilder sb = new StringBuilder(keyPA[0].getString(row));
      for (int key = 1; key < nKeys; key++) sb.append("\t" + keyPA[key].getString(row));
      String s = sb.toString();
      if (s.length() == nKeys - 1) // just tabs separating ""
      s = mvKey;
      Integer obj = hashMap.get(s);
      if (obj == null) {
        // don't change the missing values already in the pa
        matched.clear(row);
        if (debugMode) notMatchedTally.add("NotMatched", s);
      } else {
        // copy values from lutPA's to newPA's
        nMatched++;
        int fRow = obj.intValue();
        for (int lutCol = nKeys; lutCol < lutNCols; lutCol++)
          newPA[lutCol].setFromPA(row, lutPA[lutCol], fRow);
      }
    }
    if (reallyVerbose)
      String2.log(
          "  Table.join(nKeys="
              + nKeys
              + " keyCol=#"
              + keyCol
              + "="
              + getColumnName(keyCol)
              + " insertedColumns="
              + insertedColumnNames.toString()
              + ") nMatched="
              + nMatched
              + " nNotMatched="
              + (nRows - nMatched)
              + " time="
              + (System.currentTimeMillis() - time)
              + "ms");
    if (debugMode) {
      if (nRows - nMatched > 0)
        String2.log("  Debug: NotMatched tally:\n" + notMatchedTally.toString(50));
      ensureValid();
    }

    return matched;
  }

  /**
   * This updates the data in this table with better data from otherTable by matching rows based on
   * the values in key columns which are in both tables (like a batch version of SQL's UPDATE
   * https://www.w3schools.com/sql/sql_update.asp). Afterwards, this table will have rows for *all*
   * of the values of the key columns from both tables. This is very fast and efficient, but may
   * need lots of memory.
   *
   * <p>Values are grabbed from the other table by matching column names, so otherTable's values
   * have precedence. The columns in the two tables need not be the same, nor in the same order. If
   * otherTable doesn't have a matching column, the current value (if any) isn't changed, or the new
   * value will be the missing_value (or _FillValue) for the column (or "" if none).
   *
   * <p>The names and attributes of this table's columns won't be changed. The initial rows in
   * thisTable will be in the same order. New rows (for new key values) will be at the end (in their
   * order from otherTable).
   *
   * <p>The key values are matched via their string representation, so they can have different
   * elementPATypes as long as their strings match. E.g., byte, short, int, long, String are usually
   * compatible. Warning: But double = int will probably fail because "1.0" != "1".
   *
   * @param keyNames these columns must be present in this table and otherTable
   * @param otherTable
   * @return the number of existing rows that were matched. The number of new rows =
   *     otherTable.nRows() - nMatched.
   * @throws RuntimeException if trouble (e.g., keyCols not found or lookUpTable is null)
   */
  public int update(String keyNames[], Table otherTable) {
    String msg = String2.ERROR + " in Table.update: ";
    long time = System.currentTimeMillis();
    int nRows = nRows();
    int nOtherRows = otherTable.nRows();
    int nCols = nColumns();
    int nKeyCols = keyNames.length;
    if (nKeyCols < 1)
      throw new RuntimeException(msg + "nKeys=" + nKeyCols + " must be at least 1.");
    int keyCols[] = new int[nKeyCols];
    int otherKeyCols[] = new int[nKeyCols];
    PrimitiveArray keyPAs[] = new PrimitiveArray[nKeyCols];
    PrimitiveArray otherKeyPAs[] = new PrimitiveArray[nKeyCols];
    for (int key = 0; key < nKeyCols; key++) {
      keyCols[key] = findColumnNumber(keyNames[key]);
      otherKeyCols[key] = otherTable.findColumnNumber(keyNames[key]);
      if (keyCols[key] < 0)
        throw new RuntimeException(msg + "keyName=" + keyNames[key] + " not found in this table.");
      if (otherKeyCols[key] < 0)
        throw new RuntimeException(msg + "keyName=" + keyNames[key] + " not found in otherTable.");
      keyPAs[key] = getColumn(keyCols[key]);
      otherKeyPAs[key] = otherTable.getColumn(otherKeyCols[key]);
    }

    // make hashmap of this table's key values to row#
    HashMap rowHash = new HashMap(Math2.roundToInt(1.4 * nRows));
    for (int row = 0; row < nRows; row++) {
      StringBuilder sb = new StringBuilder();
      for (int key = 0; key < nKeyCols; key++) sb.append(keyPAs[key].getString(row) + "\n");
      rowHash.put(sb.toString(), Integer.valueOf(row));
    }

    // find columns in otherTable which correspond to the columns in this table
    int otherCols[] = new int[nCols];
    PrimitiveArray otherPAs[] = new PrimitiveArray[nCols];
    String missingValues[] = new String[nCols];
    Arrays.fill(missingValues, "");
    int nColsMatched = 0;
    StringArray colsNotMatched = new StringArray();
    for (int col = 0; col < nCols; col++) {
      otherCols[col] = otherTable.findColumnNumber(getColumnName(col));
      if (otherCols[col] >= 0) {
        nColsMatched++;
        otherPAs[col] = otherTable.getColumn(otherCols[col]);
      } else {
        colsNotMatched.add(getColumnName(col));
      }

      // collect missing values
      Attributes atts = columnAttributes(col);
      String mv = atts.getString("missing_value");
      if (mv == null) mv = atts.getString("_FillValue");
      if (mv != null) missingValues[col] = mv;
    }

    // go through rows of otherTable
    int nNewRows = 0;
    int nRowsMatched = 0;
    for (int otherRow = 0; otherRow < nOtherRows; otherRow++) {
      // for each, find the matching row in this table (or not)
      StringBuilder sb = new StringBuilder();
      for (int key = 0; key < nKeyCols; key++)
        sb.append(otherKeyPAs[key].getString(otherRow) + "\n");
      String sbString = sb.toString();
      Object thisRowI = rowHash.get(sbString);
      if (thisRowI == null) {
        // add blank row at end
        nNewRows++;
        rowHash.put(sbString, Integer.valueOf(nRows++));
        for (int col = 0; col < nCols; col++) {
          if (otherPAs[col] == null) getColumn(col).addString(missingValues[col]);
          else getColumn(col).addFromPA(otherPAs[col], otherRow);
        }
      } else {
        // replace current values
        nRowsMatched++;
        for (int col = 0; col < nCols; col++) {
          // if otherTable doesn't have matching column, current value isn't changed
          if (otherPAs[col] != null)
            getColumn(col).setFromPA(((Integer) thisRowI).intValue(), otherPAs[col], otherRow);
        }
      }
    }
    if (reallyVerbose)
      String2.log(
          "Table.update finished."
              + " nColsMatched="
              + nColsMatched
              + " of "
              + nCols
              + (nColsMatched == nCols ? "" : " (missing: " + colsNotMatched.toString() + ")")
              + ", nRowsMatched="
              + nRowsMatched
              + ", nNewRows="
              + nNewRows
              + ", time="
              + (System.currentTimeMillis() - time)
              + "ms");
    return nRowsMatched;
  }

  /* *  THIS IS INACTIVE.
   * This reads a NetCDF file with no structure or groups, but with
   * at least one dimension and at least 1 1D array variable that uses that
   * dimension, and populates the public variables.
   * Suitable files include LAS Intermediate NetCDF files.
   *
   * <p>If there is a time variable with attribute "units"="seconds",
   *   and storing seconds since 1970-01-01T00:00:00Z.
   *   See [COARDS] "Time or date dimension".
   * <p>The file may have a lat variable with attribute "units"="degrees_north"
   *   to identify the latitude variable. See [COARDS] "Latitude Dimension".
   *   It can be of any numeric data type.
   * <p>The file may have a lon variable with attribute "units"="degrees_east"
   *   to identify the longitude variable. See [COARDS] "Longitude Dimension".
   *   It can be of any numeric data type.
   *
   * <p>netcdf files are read with code in
   * netcdf-X.X.XX.jar which is part of the
   * <a href="https://www.unidata.ucar.edu/software/netcdf-java/"
   * >NetCDF Java Library</a>
   * renamed as netcdf-latest.jar.
   * Put it in the classpath for the compiler and for Java.
   *
   * <p>This sets globalAttributes and columnAttributes.
   *
   * @param fullFileName the full name of the file, for diagnostic messages.
   * @param ncFile an open ncFile
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param okRows indicates which rows should be kept.
   *   This is used as the starting point for the tests (the tests may reject
   *   rows which are initially ok) or can be used without tests. It may be null.
   * @param testColumns the names of the columns to be tested (null = no tests).
   *   All of the test columns must use the same, one, dimension that the
   *   loadColumns use.
   *   Ideally, the first tests will greatly restrict the range of valid rows.
   * @param testMin the minimum allowed value for each testColumn (null = no tests)
   * @param testMax the maximum allowed value for each testColumn (null = no tests)
   * @param loadColumns the names of the columns to be loaded.
   *     They must all be ArrayXxx.D1 or ArrayChar.D2 variables and use the
   *     same, one, dimension as the first dimension.
   *     If loadColumns is null, this will read all of the variables in the
   *     main group which use the biggest rootGroup dimension as their
   *     one and only dimension.
   * @throws Exception if trouble
   */
  /*    public void readNetCDF(String fullFileName, NetcdfFile ncFile,
      int standardizeWhat, BitSet okRows,
      String testColumns[], double testMin[], double testMax[],
      String loadColumns[]) throws Exception {

      //if (reallyVerbose) String2.log(File2.hexDump(fullFileName, 300));
      if (reallyVerbose) String2.log("Table.readNetCDF" +
          "\n  testColumns=" + String2.toCSSVString(testColumns) +
          "\n  testMin=" + String2.toCSSVString(testMin) +
          "\n  testMax=" + String2.toCSSVString(testMax) +
          "\n  loadColumns=" + String2.toCSSVString(loadColumns));

      //setup
      long time = System.currentTimeMillis();
      clear();
      String errorInMethod = String2.ERROR + " in Table.readNetCDF(" + fullFileName + "):\n";

      //*** ncdump  //this is very slow for big files
      //if (reallyVerbose) String2.log(NcHelper.ncdump(fullFileName, "-h"));

      //read the globalAttributes
      if (reallyVerbose) String2.log("  read the globalAttributes");
      globalAttributes = new ArrayList();
      List globalAttList = ncFile.globalAttributes();
      for (int att = 0; att < globalAttList.size(); att++) {
          Attribute gAtt = (Attribute)globalAttList.get(att);
          globalAttributes.add(gAtt.getShortName());
          globalAttributes.add(PrimitiveArray.factory(
              DataHelper.getArray(gAtt.getValues())));
      }

      //find the mainDimension
      Dimension mainDimension = null;
      if (loadColumns == null) {
          //assume mainDimension is the biggest dimension
          //FUTURE: better to look for 1d arrays and find the largest?
          //   Not really, because lat and lon could have same number
          //   but they are different dimension.
          List dimensions = ncFile.getDimensions(); //next nc version: rootGroup.getDimensions();
          if (dimensions.size() == 0)
              throw new SimpleException(errorInMethod + "the file has no dimensions.");
          mainDimension = (Dimension)dimensions.get(0);
          if (!mainDimension.isUnlimited()) {
              for (int i = 1; i < dimensions.size(); i++) {
                  if (reallyVerbose) String2.log("  look for biggest dimension, check " + i);
                  Dimension tDimension = (Dimension)dimensions.get(i);
                  if (tDimension.isUnlimited()) {
                      mainDimension = tDimension;
                      break;
                  }
                  if (tDimension.getLength() > mainDimension.getLength())
                      mainDimension = tDimension;
              }
          }
      } else {
          //if loadColumns was specified, get mainDimension from loadColumns[0]
          if (reallyVerbose) String2.log("  get mainDimension from loadColumns[0]");
          Variable v = ncFile.findVariable(loadColumns[0]);
          mainDimension = v.getDimension(0);
      }


      //make a list of the needed variables (loadColumns and testColumns)
      ArrayList<Variable> allVariables = new ArrayList();
      if (loadColumns == null) {
          //get a list of all variables which use just mainDimension
          List variableList = ncFile.getVariables();
          for (int i = 0; i < variableList.size(); i++) {
              if (reallyVerbose) String2.log("  get all variables which use mainDimension, check " + i);
              Variable tVariable = (Variable)variableList.get(i);
              List tDimensions = tVariable.getDimensions();
              int nDimensions = tDimensions.size();
              if (reallyVerbose) String2.log("i=" + i + " name=" + tVariable.getFullName() +
                  " type=" + tVariable.getDataType());
              if ((nDimensions == 1 && tDimensions.get(0).equals(mainDimension)) ||
                  (nDimensions == 2 && tDimensions.get(0).equals(mainDimension) &&
                       tVariable.getDataType() == DataType.CHAR)) {
                      allVariables.add(tVariable);
              }
          }
      } else {
          //make the list from the loadColumns and testColumns
          for (int i = 0; i < loadColumns.length; i++) {
              if (reallyVerbose) String2.log("  getLoadColumns " + i);
              allVariables.add(ncFile.findVariable(loadColumns[i]));
          }
          if (testColumns != null) {
              for (int i = 0; i < testColumns.length; i++) {
                  if (String2.indexOf(loadColumns, testColumns[i]) < 0) {
                      if (reallyVerbose) String2.log("  getTestColumns " + i);
                      allVariables.add(ncFile.findVariable(testColumns[i]));
                  }
              }
          }
      }
      if (reallyVerbose) String2.log("  got AllVariables " + allVariables.size());

      //get the data
      getNetcdfSubset(errorInMethod, allVariables, standardizeWhat, okRows,
          testColumns, testMin, testMax, loadColumns);

      if (reallyVerbose)
          String2.log("Table.readNetCDF nColumns=" + nColumns() +
              " nRows=" + nRows() + " time=" + (System.currentTimeMillis() - time) + "ms");

  }


  /**  THIS IS NOT YET FINISHED.
   * This reads all rows of all of the specified columns from an opendap
   * dataset.
   * This also reads global and variable attributes.
   * The data is always unpacked.
   *
   * <p>If the fullName is an http address, the name needs to start with "http:\\"
   * (upper or lower case) and the server needs to support "byte ranges"
   * (see ucar.nc2.NetcdfFile documentation).
   *
   * @param fullName This may be a local file name, an "http:" address of a
   *    .nc file, or an opendap url.
   * @param loadColumns if null, this searches for the (pseudo)structure variables
   * @throws Exception if trouble
   */
  public void readOpendap(String fullName, String loadColumns[]) throws Exception {

    // get information
    String msg = "  Table.readOpendap " + fullName;
    long time = System.currentTimeMillis();
    Attributes gridMappingAtts = null;
    NetcdfFile netcdfFile = NcHelper.openFile(fullName);
    try {
      Variable loadVariables[] = NcHelper.findVariables(netcdfFile, loadColumns);

      // fill the table
      clear();
      appendNcRows(loadVariables, 0, -1);
      NcHelper.getGroupAttributes(netcdfFile.getRootGroup(), globalAttributes());
      for (int col = 0; col < loadVariables.length; col++) {
        NcHelper.getVariableAttributes(loadVariables[col], columnAttributes(col));

        // does this var point to the pseudo-data var with CF grid_mapping (projection) information?
        if (gridMappingAtts == null) {
          gridMappingAtts =
              NcHelper.getGridMappingAtts(
                  netcdfFile, columnAttributes(col).getString("grid_mapping"));
          if (gridMappingAtts != null) globalAttributes.add(gridMappingAtts);
        }
      }

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      try {
        if (netcdfFile != null) netcdfFile.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * For compatibility with older programs, this calls readOpendapSequence(url, false). 2016-12-07:
   * With versions of Tomcat somewhere after 8.0, the url must be stongly percent-encoded.
   *
   * @param url the url, already SSR.percentEncoded as needed
   */
  public void readOpendapSequence(String url) throws Exception {
    readOpendapSequence(url, false);
  }

  /**
   * This populates the table from an opendap one level or two-level (Dapper-style) sequence
   * response. See Opendap info: https://www.opendap.org/pdf/dap_2_data_model.pdf . See Dapper
   * Conventions: https://www.pmel.noaa.gov/epic/software/dapper/dapperdocs/conventions/ .
   * 2016-12-07: With versions of Tomcat somewhere after 8.0, the url must be stongly
   * percent-encoded.
   *
   * <p>A typical dapper-style two-level nested sequence is:
   *
   * <pre>
   * Dataset {
   * Sequence {
   * Float32 lat;
   * Float64 time;
   * Float32 lon;
   * Int32 _id;
   * Sequence {
   * Float32 depth;
   * Float32 temp;
   * Float32 salinity;
   * Float32 pressure;
   * } profile;
   * } location;
   * } northAtlantic;
   * </pre>
   *
   * The resulting flat table created by this method has a column for each variable (lat, time, lon,
   * _id, depth, temp, salinity, pressure) and a row for each measurement.
   *
   * <p>Following the JDAP getValue return type, DUInt16 is converted to a Java short and DUInt32 is
   * converted to a Java int.
   *
   * <p>!!! Unlike the DAPPER convention, this method only supports one inner sequence; otherwise,
   * the response couldn't be represented as a simple table.
   *
   * <p>!!! Two-level sequences from DAPPER always seems to have data from different inner sequences
   * separated by a row of NaNs (in the inner sequence variables) to mark the end of the upper inner
   * sequence's info. I believe Dapper is doing this to mark the transition from one inner sequence
   * to the next.
   *
   * <p>!!! Dapper doesn't return variables in the order that you requested them. It is unclear (to
   * me) what determines the order.
   *
   * <p>Remember that Dapper doesn't support constraints on non-axis variables!
   *
   * <p>This clears the table before starting. This sets the global and data attributes (see
   * javadocs for DataHelper.getAttributesFromOpendap).
   *
   * <p>!!!I don't know if the opendap server auto-unpacks (scale, offset) the variables. I have no
   * test cases. This method just passes through what it gets.
   *
   * @param url This may include a constraint expression at the end e.g.,
   *     ?latitude,longitude,time,WTMP&time>=1124463600. The url (pre "?") should have no extension,
   *     e.g., .dods, at the end. The query should already be SSR.percentEncoded as needed.
   * @param skipDapperSpacerRows if true, this skips the last row of each innerSequence other than
   *     the last innerSequence (because Dapper puts NaNs in the row to act as a spacer).
   * @throws Exception if trouble
   */
  public void readOpendapSequence(String url, boolean skipDapperSpacerRows) throws Exception {

    if (reallyVerbose) String2.log("Table.readOpendapSequence url=\n" + url);
    String errorInMethod = String2.ERROR + " in Table.readOpendapSequence(" + url + "):\n";
    long time = System.currentTimeMillis();
    clear();
    DConnect dConnect = new DConnect(url, opendapAcceptDeflate, 1, 1);
    DAS das = dConnect.getDAS(OpendapHelper.DEFAULT_TIMEOUT);
    OpendapHelper.getAttributes(das, "GLOBAL", globalAttributes());

    // get the outerSequence information
    DataDDS dataDds = dConnect.getData(null); // null = no statusUI
    if (reallyVerbose)
      String2.log("  dConnect.getData time=" + (System.currentTimeMillis() - time) + "ms");
    BaseType firstVariable = (BaseType) dataDds.getVariables().nextElement();
    if (!(firstVariable instanceof DSequence))
      throw new Exception(
          errorInMethod
              + "firstVariable not a DSequence: name="
              + firstVariable.getName()
              + " type="
              + firstVariable.getTypeName());
    DSequence outerSequence = (DSequence) firstVariable;
    int nOuterRows = outerSequence.getRowCount();
    int nOuterColumns = outerSequence.elementCount();
    AttributeTable outerAttributeTable =
        das.getAttributeTable(outerSequence.getLongName()); // I think getLongName == getName() here
    // String2.log("outerAttributeTable=" + outerAttributeTable);

    // create the columns
    int innerSequenceColumn = -1; // the outerCol with the inner sequence (or -1 if none)
    int nInnerColumns = 0; // 0 important if no innerSequenceColumn
    for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {
      // create the columns
      BaseType obt =
          outerSequence.getVar(outerCol); // this doesn't have data, just description of obt
      if (obt instanceof DByte) addColumn(obt.getName(), new ByteArray());
      else if (obt instanceof DFloat32) addColumn(obt.getName(), new FloatArray());
      else if (obt instanceof DFloat64) addColumn(obt.getName(), new DoubleArray());
      else if (obt instanceof DInt16) addColumn(obt.getName(), new ShortArray());
      else if (obt instanceof DUInt16) addColumn(obt.getName(), new ShortArray());
      else if (obt instanceof DInt32) addColumn(obt.getName(), new IntArray());
      else if (obt instanceof DUInt32) addColumn(obt.getName(), new IntArray());
      else if (obt instanceof DBoolean)
        addColumn(
            obt.getName(), new ByteArray()); // .nc doesn't support booleans, so store byte=0|1
      else if (obt instanceof DString) addColumn(obt.getName(), new StringArray());
      else if (obt instanceof DSequence) {
        // *** Start Dealing With InnerSequence
        // Ensure this is the first innerSequence.
        // If there are two, the response can't be represented as a simple table.
        if (innerSequenceColumn != -1)
          throw new Exception(
              errorInMethod
                  + "The response has more than one inner sequence: "
                  + getColumnName(innerSequenceColumn)
                  + " and "
                  + obt.getName()
                  + ".");
        innerSequenceColumn = outerCol;
        if (reallyVerbose) String2.log("  innerSequenceColumn=" + innerSequenceColumn);

        // deal with the inner sequence
        DSequence innerSequence = (DSequence) obt;
        nInnerColumns = innerSequence.elementCount();
        AttributeTable innerAttributeTable = das.getAttributeTable(innerSequence.getName());
        // String2.log("innerAttributeTable=" + innerAttributeTable);
        for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {

          // create the columns
          BaseType ibt =
              innerSequence.getVar(innerCol); // this doesn't have data, just description of ibt
          if (ibt instanceof DByte) addColumn(ibt.getName(), new ByteArray());
          else if (ibt instanceof DFloat32) addColumn(ibt.getName(), new FloatArray());
          else if (ibt instanceof DFloat64) addColumn(ibt.getName(), new DoubleArray());
          else if (ibt instanceof DInt16) addColumn(ibt.getName(), new ShortArray());
          else if (ibt instanceof DUInt16) addColumn(ibt.getName(), new ShortArray());
          else if (ibt instanceof DInt32) addColumn(ibt.getName(), new IntArray());
          else if (ibt instanceof DUInt32) addColumn(ibt.getName(), new IntArray());
          else if (ibt instanceof DBoolean)
            addColumn(
                ibt.getName(), new ByteArray()); // .nc doesn't support booleans, so store byte=0|1
          else if (ibt instanceof DString) addColumn(ibt.getName(), new StringArray());
          else
            throw new Exception(
                errorInMethod
                    + "Unexpected inner variable type="
                    + ibt.getTypeName()
                    + " for name="
                    + ibt.getName());

          // get the ibt attributes
          // (some servers return innerAttributeTable, some don't -- see test cases)
          if (innerAttributeTable == null) {
            // Dapper needs this approach
            // note use of getLongName here
            Attributes tAtt = columnAttributes(nColumns() - 1);
            OpendapHelper.getAttributes(das, ibt.getLongName(), tAtt);
            if (tAtt.size() == 0) OpendapHelper.getAttributes(das, ibt.getName(), tAtt);
          } else {
            // note use of getName in this section
            int tCol = nColumns() - 1; // the table column just created
            // String2.log("try getting attributes for inner " + getColumnName(col));
            dods.dap.Attribute attribute = innerAttributeTable.getAttribute(ibt.getName());
            // it should be a container with the attributes for this column
            if (attribute == null) {
              String2.log(
                  errorInMethod + "Unexpected: no attribute for innerVar=" + ibt.getName() + ".");
            } else if (attribute.isContainer()) {
              OpendapHelper.getAttributes(attribute.getContainer(), columnAttributes(tCol));
            } else {
              String2.log(
                  errorInMethod
                      + "Unexpected: attribute for innerVar="
                      + ibt.getName()
                      + " not a container: "
                      + attribute.getName()
                      + "="
                      + attribute.getValueAt(0));
            }
          }
        }
        // *** End Dealing With InnerSequence

      } else
        throw new Exception(
            errorInMethod
                + "Unexpected outer variable type="
                + obt.getTypeName()
                + " for name="
                + obt.getName());

      // get the obt attributes
      // (some servers return outerAttributeTable, some don't -- see test cases)
      if (obt instanceof DSequence) {
        // it is the innerSequence, so attributes already read
      } else if (outerAttributeTable == null) {
        // Dapper needs this approach
        // note use of getLongName here
        Attributes tAtt = columnAttributes(nColumns() - 1);
        OpendapHelper.getAttributes(das, obt.getLongName(), tAtt);
        // drds needs this approach
        if (tAtt.size() == 0) OpendapHelper.getAttributes(das, obt.getName(), tAtt);
      } else {
        // note use of getName in this section
        int tCol = nColumns() - 1; // the table column just created
        // String2.log("try getting attributes for outer " + getColumnName(col));
        dods.dap.Attribute attribute = outerAttributeTable.getAttribute(obt.getName());
        // it should be a container with the attributes for this column
        if (attribute == null) {
          String2.log(
              errorInMethod + "Unexpected: no attribute for outerVar=" + obt.getName() + ".");
        } else if (attribute.isContainer()) {
          OpendapHelper.getAttributes(attribute.getContainer(), columnAttributes(tCol));
        } else {
          String2.log(
              errorInMethod
                  + "Unexpected: attribute for outerVar="
                  + obt.getName()
                  + " not a container: "
                  + attribute.getName()
                  + "="
                  + attribute.getValueAt(0));
        }
      }
    }
    // if (reallyVerbose) String2.log("  columns were created.");

    // Don't ensure that an innerSequence was found
    // so that this method can be used for 1 or 2 level sequences.
    // String2.log("nOuterRows=" + nOuterRows);
    // String2.log(toString());

    // *** read the data (row-by-row, as it wants)
    for (int outerRow = 0; outerRow < nOuterRows; outerRow++) {
      Vector outerVector = outerSequence.getRow(outerRow);
      int col;

      // get data from innerSequence first (so nInnerRows is known)
      int nInnerRows = 1; // 1 is important if no innerSequence
      if (innerSequenceColumn >= 0) {
        DSequence innerSequence = (DSequence) outerVector.get(innerSequenceColumn);
        nInnerRows = innerSequence.getRowCount();
        if (skipDapperSpacerRows && outerRow < nOuterRows - 1) nInnerRows--;
        // if (reallyVerbose) String2.log("  nInnerRows=" + nInnerRows + " nInnerCols=" +
        // nInnerColumns);
        Test.ensureEqual(
            nInnerColumns,
            innerSequence.elementCount(),
            errorInMethod + "Unexpected nInnerColumns for outer row #" + outerRow);
        col = innerSequenceColumn;
        for (int innerRow = 0; innerRow < nInnerRows; innerRow++) {
          Vector innerVector = innerSequence.getRow(innerRow);
          for (int innerCol = 0; innerCol < nInnerColumns; innerCol++) {
            // if (reallyVerbose) String2.log("  OR=" + outerRow + " OC=" + col + " IR=" + innerRow
            // + " IC=" + innerCol);
            BaseType ibt = (BaseType) innerVector.get(innerCol);
            if (ibt instanceof DByte t) ((ByteArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DFloat32 t)
              ((FloatArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DFloat64 t)
              ((DoubleArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DInt16 t)
              ((ShortArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DUInt16 t)
              ((ShortArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DInt32 t)
              ((IntArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DUInt32 t)
              ((IntArray) columns.get(col + innerCol)).add(t.getValue());
            else if (ibt instanceof DBoolean t)
              ((ByteArray) columns.get(col + innerCol))
                  .add(
                      (byte)
                          (t.getValue()
                              ? 1
                              : 0)); // .nc doesn't support booleans, so store byte=0|1
            else if (ibt instanceof DString t)
              ((StringArray) columns.get(col + innerCol)).add(t.getValue());
            else
              throw new Exception(
                  errorInMethod
                      + "Unexpected inner variable type="
                      + ibt.getTypeName()
                      + " for name="
                      + ibt.getName());
          }
        }
      }

      // process the other outerCol
      // if (reallyVerbose) String2.log("  process the other outer col");
      col = 0; // restart at 0
      for (int outerCol = 0; outerCol < nOuterColumns; outerCol++) {
        // innerSequenceColumn already processed above
        if (outerCol == innerSequenceColumn) {
          col += nInnerColumns;
          continue;
        }

        // note addN (not add)
        // I tried storing type of column to avoid instanceof, but no faster.
        BaseType obt = (BaseType) outerVector.get(outerCol);
        if (obt instanceof DByte t) ((ByteArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DFloat32 t)
          ((FloatArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DFloat64 t)
          ((DoubleArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DInt16 t)
          ((ShortArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DUInt16 t)
          ((ShortArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DInt32 t)
          ((IntArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DUInt32 t)
          ((IntArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else if (obt instanceof DBoolean t)
          ((ByteArray) columns.get(col++))
              .addN(
                  nInnerRows,
                  (byte) (t.getValue() ? 1 : 0)); // .nc doesn't support booleans, so store byte=0|1
        else if (obt instanceof DString t)
          ((StringArray) columns.get(col++)).addN(nInnerRows, t.getValue());
        else
          throw new Exception(
              errorInMethod
                  + "Unexpected outer variable type="
                  + obt.getTypeName()
                  + " for name="
                  + obt.getName());
      }
    }

    // since addition of data to different columns is helter-skelter,
    // ensure resulting table is valid (it's a fast test)
    ensureValid(); // throws Exception if not

    if (reallyVerbose)
      String2.log(
          "  Table.readOpendapSequence done. url="
              + url
              + " nColumns="
              + nColumns()
              + " nRows="
              + nRows()
              + " TOTAL TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * This populates the table from an opendap sequence. The test information and loadColumns will be
   * used generate a new url with a constraint expression. Opendap sequences support constraint
   * expressions; many other opendap datasets do not. The testColumns and loadColumns must be in the
   * same sequence.
   *
   * <p>!!!I don't know if this auto-unpacks (scale, offset) the variables. I have no test cases.
   *
   * @param url The url may not include a constraint expression, since them method generates one to
   *     do the tests
   * @param testColumns the names of the columns to be tested. If testColumns is null, no tests will
   *     be done.
   * @param testMin the minimum allowed value for each testColumn. Ignored if testColumns is null.
   * @param testMax the maximum allowed value for each testColumn. Ignored if testColumns is null.
   * @param loadColumns The columns to be downloaded for the rows were the test result is 'true'. If
   *     null, all columns will be loaded.
   * @param skipDapperSpacerRows if true, this skips the last row of each innerSequence (other than
   *     the last innerSequence (because Dapper puts NaNs in the row to act as a spacer).
   * @throws Exception if trouble
   */
  public void readOpendapSequence(
      String url,
      String testColumns[],
      double testMin[],
      double testMax[],
      String loadColumns[],
      boolean skipDapperSpacerRows)
      throws Exception {

    if (reallyVerbose) String2.log("Table.readOpendapSequence with restrictions...");
    StringBuilder qSB = new StringBuilder();
    if (loadColumns != null && loadColumns.length > 0) {
      qSB.append(loadColumns[0]);
      for (int col = 1; col < loadColumns.length; col++) qSB.append("," + loadColumns[col]);
    }
    if (testColumns != null) {
      for (int col = 0; col < testColumns.length; col++) {
        qSB.append("&" + testColumns[col] + "%3E=" + testMin[col]);
        qSB.append("&" + testColumns[col] + "%3C=" + testMax[col]);
      }
    }
    readOpendapSequence(url + "?" + qSB.toString(), skipDapperSpacerRows);
  }

  /**
   * This populates the table from an opendap response.
   *
   * @param url This may include a constraint expression e.g.,
   *     ?latitude,longitude,time,WTMP&time>==1124463600
   * @param loadColumns The columns from the response to be saved in the table (use null for have
   *     the methods search for all variables in a (pseudo)structure). These columns are not
   *     appended to the url.
   * @throws Exception if trouble
   */
  /*    public void readOpendap(String url, String loadColumns[]) throws Exception {

          //get information
          String msg = "  Table.readOpendap " + url;
          long time = System.currentTimeMillis();
          NetcdfFile netcdfFile = NetcdfDatasets.openDataset(url); //NetcdfDataset needed for opendap.   //2021: 's' is new API
          try {
              List loadVariables = findNcVariables(netcdfFile, loadColumns);

              //fill the table
              clear();
              globalAttributes = getNcGlobalAttributes(netcdfFile);
              columnAttributes = getNcVariableAttributes(loadVariables);
              appendNcRows(loadVariables, 0, -1);
              if (netcdfFile != null) try {netcdfFile.close(); } catch (Throwable t) {};
              if (reallyVerbose) String2.log(msg +
                  " finished. nColumns=" + nColumns() + " nRows=" + nRows() +
                  " TIME=" + (System.currentTimeMillis() - time) + "ms");

          } catch (Throwable t) {
              String2.log(msg);
              if (netcdfFile != null) try {netcdfFile.close(); } catch (Throwable t) {};
              throw t;
          }
      }
  */

  /**
   * This forces the values in lonAr to be +/-180 or 0..360. THIS ONLY WORKS IF MINLON AND MAXLON
   * ARE BOTH WESTERN OR EASTERN HEMISPHERE.
   *
   * @param lonArray
   * @param pm180 If true, lon values are forced to be +/-180. If false, lon values are forced to be
   *     0..360.
   */
  public static void forceLonPM180(PrimitiveArray lonArray, boolean pm180) {
    double stats[] = lonArray.calculateStats();
    int nRows = lonArray.size();
    String2.log("forceLon stats=" + String2.toCSSVString(stats));
    if (pm180 && stats[PrimitiveArray.STATS_MAX] > 180) {
      String2.log("  force >");
      for (int row = 0; row < nRows; row++) {
        lonArray.setDouble(row, Math2.looserAnglePM180(lonArray.getDouble(row)));
      }
    } else if (!pm180 && stats[PrimitiveArray.STATS_MIN] < 0) {
      String2.log("  force <");
      for (int row = 0; row < nRows; row++) {
        lonArray.setDouble(row, Math2.looserAngle0360(lonArray.getDouble(row)));
      }
    }
  }

  /**
   * Make a subset of this data by retaining only the rows which pass the test. For each column
   * number in testColumns, there is a corresponding min and max. Rows of data will be kept if, for
   * the value in each testColumns, the value isn't NaN and is &gt;= min and &lt;= max. This doesn't
   * touch xxxAttributes.
   *
   * @param testColumnNumbers the column numbers to be tested
   * @param min the corresponding min allowed value
   * @param max the corresponding max allowed value
   */
  public void subset(int testColumnNumbers[], double min[], double max[]) {

    int nRows = nRows();
    int nColumns = nColumns();
    int nTestColumns = testColumnNumbers.length;
    PrimitiveArray testColumns[] = new PrimitiveArray[nTestColumns];
    for (int col = 0; col < nTestColumns; col++) {
      testColumns[col] = getColumn(testColumnNumbers[col]);
    }

    // go through the data looking for valid rows
    // copy successes to position nGood
    int nGood = 0;
    for (int row = 0; row < nRows; row++) {
      boolean ok = true;
      for (int testCol = 0; testCol < nTestColumns; testCol++) {
        double d = testColumns[testCol].getDouble(row);
        if (Double.isFinite(d) && d >= min[testCol] && d <= max[testCol]) {
        } else {
          ok = false;
          break;
        }
      }
      if (ok) {
        for (int col = 0; col < nColumns; col++) getColumn(col).copy(row, nGood);
        nGood++;
      }
    }

    // remove excess at end of column
    for (int col = 0; col < nColumns; col++)
      getColumn(col).removeRange(nGood, getColumn(col).size());
  }

  /**
   * This is like tryToApplyConstraints, but just keeps the constraints=true rows (which may be 0).
   *
   * @param idCol For reallyVerbose only: rejected rows will log the value in this column (e.g.,
   *     stationID) (or row= if idCol < 0).
   * @param conNames may be null or size 0
   * @return the number of rows remaining in the table (may be 0)
   */
  public int tryToApplyConstraintsAndKeep(
      int idCol, StringArray conNames, StringArray conOps, StringArray conVals) {

    // no constraints
    if (conNames == null || conNames.size() == 0) return nRows();

    // try to apply constraints
    BitSet keep = new BitSet();
    keep.set(0, nRows());
    int cardinality = tryToApplyConstraints(idCol, conNames, conOps, conVals, keep);
    if (cardinality == 0) removeAllRows();
    else justKeep(keep);

    return cardinality;
  }

  /**
   * This is like tryToApplyConstraint, but works on multiple constraints. The table won't be
   * changed.
   *
   * @param idCol For reallyVerbose only: rejected rows will log the value in this column (e.g.,
   *     stationID) (or row# if idCol < 0).
   * @param keep the bitset that will be modified. If you are setting up keep for this, set the bit
   *     for each row to true. Rows where the bit is initially false won't even be checked.
   * @return the number of keep=true rows (may be 0). No rows are actually removed.
   */
  public int tryToApplyConstraints(
      int idCol, StringArray conNames, StringArray conOps, StringArray conVals, BitSet keep) {

    // no constraints
    if (conNames == null || conNames.size() == 0) return keep.cardinality();

    // try to apply constraints
    int cardinality = -1; // it will be set below
    for (int i = 0; i < conNames.size(); i++) {
      cardinality =
          lowApplyConstraint(false, idCol, conNames.get(i), conOps.get(i), conVals.get(i), keep);
      if (cardinality == 0) return 0;
    }
    return cardinality;
  }

  /**
   * This tries to clear bits of keep (each representing a row), based on a constraint. The table
   * won't be changed.
   *
   * @param idCol For reallyVerbose only: rejected rows will log the value in this column (e.g.,
   *     stationID) (or row= if idCol < 0).
   * @param conName perhaps a column name in the table. If not, this constraint is ignored. This
   *     correctly deals with float, double, and other data types.
   * @param conOp an EDDTable-operator, e.g., "="
   * @param conVal
   * @param keep the bitset that will be modified. If you are setting up keep for this, set the bit
   *     for each row to true. Rows where the bit is initially false won't even be checked.
   * @return keep.cardinality()
   */
  public int tryToApplyConstraint(
      int idCol, String conName, String conOp, String conVal, BitSet keep) {

    return lowApplyConstraint(false, idCol, conName, conOp, conVal, keep);
  }

  /**
   * This is like tryToApplyConstraint, but requires that the constraintVariable exist in the table.
   *
   * @param keep the bitset that will be modified. If you are setting up keep for this, set the bit
   *     for each row to true. Rows where the bit is initially false won't even be checked.
   * @return keep.cardinality()
   */
  public int applyConstraint(int idCol, String conName, String conOp, String conVal, BitSet keep) {

    return lowApplyConstraint(true, idCol, conName, conOp, conVal, keep);
  }

  /**
   * The low level method for tryToApplyConstraint and applyConstraint
   *
   * @param keep the bitset that will be modified. If you are setting up keep for this, set the bit
   *     for each row to true. Rows where the bit is initially false won't even be checked.
   * @return keep.cardinality()
   */
  public int lowApplyConstraint(
      boolean requireConName, int idCol, String conName, String conOp, String conVal, BitSet keep) {

    if (keep.isEmpty()) return 0;

    // if (debugMode) String2.log("\n    tryToApplyConstraint " + conName + conOp + conVal);

    // PrimitiveArray idPa = idCol >= 0? getColumn(idCol) : null;

    // is conName in the table?
    int conNameCol = findColumnNumber(conName);
    if (conNameCol < 0) {
      if (requireConName || reallyVerbose) {
        String msg =
            (requireConName ? String2.ERROR + ": " : "")
                + "applyConstraint: constraintVariable="
                + conName
                + " isn't in the table ("
                + getColumnNamesCSVString()
                + ").";
        if (requireConName) throw new RuntimeException(msg);
        if (debugMode) String2.log("    " + msg);
      }
      return keep.cardinality(); // unfortunate that time is perhaps wasted to calculate this
    }

    // test the keep=true rows for this constraint
    PrimitiveArray conPa = getColumn(conNameCol);
    int nKeep = conPa.applyConstraint(false, keep, conOp, conVal);

    if (reallyVerbose)
      String2.log(
          "    applyConstraint: after "
              + conName
              + conOp
              + "\""
              + conVal
              + "\", "
              + nKeep
              + " rows remain");
    return nKeep;
  }

  /**
   * This is like ApplyConstraint, but actually removes the rows that don't match the constraint.
   *
   * @return table.nRows()
   */
  public int oneStepApplyConstraint(int idCol, String conName, String conOp, String conVal) {

    BitSet keep = new BitSet();
    keep.set(0, nRows());
    lowApplyConstraint(true, idCol, conName, conOp, conVal, keep);
    justKeep(keep);
    return nRows();
  }

  /**
   * Make a subset of this data by retaining only the keep(row)==true rows.
   *
   * @param keep This method won't modify the values in keep.
   * @return nRows
   */
  public void justKeep(BitSet keep) {
    int nColumns = nColumns();
    for (int col = 0; col < nColumns; col++) getColumn(col).justKeep(keep);
  }

  /**
   * This returns list of &amp;-separated parts, in their original order, from a percent encoded
   * dapQuery. This is like split('&amp;'), but smarter. This accepts:
   *
   * <ul>
   *   <li>connecting &amp;'s already visible (within a part, &amp;'s must be percent-encoded
   *       (should be) or within double quotes)
   *   <li>connecting &amp;'s are percent encoded (they shouldn't be!) (within a part, &amp;'s must
   *       be within double quotes).
   * </ul>
   *
   * @param dapQuery the part after the '?', still percentEncoded, may be null.
   * @return a String[] with the percentDecoded parts, in their original order, without the
   *     connecting &amp;'s. This part#0 is always the varnames (or "" if none). A null or ""
   *     dapQuery will return String[1] with #0=""
   * @throws Throwable if trouble (e.g., invalid percentEncoding)
   */
  public static String[] getDapQueryParts(String dapQuery) throws Exception {
    if (dapQuery == null || dapQuery.length() == 0) return new String[] {""};

    boolean stillEncoded = true;
    if (dapQuery.indexOf('&') < 0) {
      // perhaps user percentEncoded everything, even the connecting &'s, so decode everything right
      // away
      dapQuery = SSR.percentDecode(dapQuery);
      stillEncoded = false;
    }
    // String2.log(">> dapQuery=" + dapQuery);

    // one way or another, connecting &'s should now be visible
    dapQuery += "&"; // & triggers grabbing final part
    int dapQueryLength = dapQuery.length();
    int start = 0;
    boolean inQuotes = false;
    StringArray parts = new StringArray();
    for (int po = 0; po < dapQueryLength; po++) {
      char ch = dapQuery.charAt(po);
      // String2.log(">> ch=" + ch);
      if (ch == '\\') { // next char is \\ escaped
        if (po < dapQueryLength) po++;
      } else if (ch == '"') {
        inQuotes = !inQuotes;
      } else if (ch == '&' && !inQuotes) {
        String part = dapQuery.substring(start, po);
        parts.add(stillEncoded ? SSR.percentDecode(part) : part);
        // String2.log(">> part=" + parts.get(parts.size() - 1));
        start = po + 1;
      }
    }
    if (inQuotes) throw new SimpleException(QUERY_ERROR + "A closing doublequote is missing.");
    // String2.log(">> parts=" + parts.toNewlineString());
    return parts.toArray();
  }

  /**
   * This parses a PERCENT ENCODED OPeNDAP DAP-style query intended to subset this table. This
   * checks the validity of the resultsVariable and constraintVariable names.
   *
   * <p>Unofficially (e.g., for testing) the query can be already percent decoded if there are no
   * %dd in the query.
   *
   * <p>There can be a constraints on variables that aren't in the user-specified results variables.
   * This procedure adds those variables to the returned resultsVariables.
   *
   * <p>If the constraintVariable is time, the value can be numeric ("seconds since 1970-01-01") or
   * String (ISO format, e.g., "1996-01-31T12:40:00", at least yyyy-MM) or
   * now(+|-)(seconds|minutes|hours|days|months|years). All variants are converted to epochSeconds.
   *
   * <p>Different from ERDDAP: To work as expected, timestamp columns are assumed to have double
   * epoch seconds values and have Calendar2.isTimeUnits(units) metadata.
   *
   * @param dapQuery is the opendap DAP-style query, after the '?', still percentEncoded (shouldn't
   *     be null), e.g., <tt>var1,var2,var3&amp;var4=value4&amp;var5%3E=value5</tt> . <br>
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
   *     If an &amp;-separated part is "distinct()", "orderBy("...")", "orderByDescending("...")",
   *     "orderByMax("...")", "orderByMin("...")", "orderByMinMax("...")", "orderByCount("...")",
   *     "orderByClosest("...")", "orderByLimit("...")", "units("...")", it is ignored. <br>
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
   * @throws Throwable if invalid query (0 resultsVariables is a valid query)
   */
  public void parseDapQuery(
      String dapQuery,
      StringArray resultsVariables,
      StringArray constraintVariables,
      StringArray constraintOps,
      StringArray constraintValues,
      boolean repair)
      throws Exception {
    // !!! Table.parseDapQuery and EDDTable.parseUserDapQuery ARE ALMOST IDENTICAL!!!
    // IF YOU MAKE CHANGES TO ONE, MAKE CHANGES TO THE OTHER.

    // parse dapQuery into parts
    String parts[] = getDapQueryParts(dapQuery); // decoded.  always at least 1 part (may be "")
    resultsVariables.clear();
    constraintVariables.clear();
    constraintOps.clear();
    constraintValues.clear();

    // expand no resultsVariables (or entire sequence) into all results variables
    // look at part0 with comma-separated vars
    int nCols = nColumns();
    if (parts[0].length() == 0 || parts[0].equals(SEQUENCE_NAME)) {
      if (debugMode)
        String2.log(
            "  dapQuery parts[0]=\"" + parts[0] + "\" is expanded to request all variables.");
      for (int v = 0; v < nCols; v++) {
        resultsVariables.add(columnNames.get(v));
      }
    } else {
      String cParts[] = String2.split(parts[0], ',');
      for (int cp = 0; cp < cParts.length; cp++) {

        // request uses sequence.dataVarName notation?
        String tVar = cParts[cp].trim();
        int period = tVar.indexOf('.');
        if (period > 0 && tVar.substring(0, period).equals(SEQUENCE_NAME))
          tVar = tVar.substring(period + 1);

        // is it a valid destinationName?
        int po = columnNames.indexOf(tVar);
        if (po < 0) {
          if (!repair) {
            if (tVar.equals(SEQUENCE_NAME))
              throw new SimpleException(
                  QUERY_ERROR
                      + "If "
                      + SEQUENCE_NAME
                      + " is requested, it must be the only requested variable.");
            for (int op = 0; op < OPERATORS.length; op++) {
              int opPo = tVar.indexOf(OPERATORS[op]);
              if (opPo >= 0)
                throw new SimpleException(
                    QUERY_ERROR
                        + "All constraints (including \""
                        + tVar.substring(0, opPo + OPERATORS[op].length())
                        + "...\") must be preceded by '&'.");
            }
            throw new SimpleException(QUERY_ERROR + "Unrecognized variable=\"" + tVar + "\".");
          }
        } else {
          // it's valid; is it a duplicate?
          if (resultsVariables.indexOf(tVar) >= 0) {
            if (!repair)
              throw new SimpleException(
                  QUERY_ERROR
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
      int constraintLength = constraint.length();
      // String2.log("constraint=" + constraint);
      int quotePo = constraint.indexOf('"');
      String constraintBeforeQuotes = quotePo >= 0 ? constraint.substring(0, quotePo) : constraint;

      // special case: ignore constraint starting with "."
      // (can't be a variable name)
      // use for e.g., .colorBar=...
      if (constraint.startsWith(".")) continue;

      // special case: server-side functions
      if (constraint.equals("distinct()")
          || (constraint.endsWith("\")")
              && (constraint.startsWith("orderBy(\"")
                  || constraint.startsWith("orderByDescending(\"")
                  || constraint.startsWith("orderByClosest(\"")
                  || constraint.startsWith("orderByCount(\"")
                  || constraint.startsWith("orderByLimit(\"")
                  || constraint.startsWith("orderByMax(\"")
                  || constraint.startsWith("orderByMin(\"")
                  || constraint.startsWith("orderByMinMax(\"")
                  || constraint.startsWith("units(\"")))) continue;

      // look for == (common mistake, but not allowed)
      int eepo = constraintBeforeQuotes.indexOf("==");
      if (eepo >= 0) {
        if (repair) {
          constraint = constraint.substring(0, eepo) + constraint.substring(eepo + 1);
          quotePo = constraint.indexOf('"');
          constraintBeforeQuotes = quotePo >= 0 ? constraint.substring(0, quotePo) : constraint;
        } else {
          throw new SimpleException(QUERY_ERROR + "Use '=' instead of '==' in constraints.");
        }
      }

      // look for ~= (common mistake, but not allowed)
      if (constraintBeforeQuotes.indexOf("~=") >= 0) {
        if (repair) {
          constraint = String2.replaceAll(constraint, "~=", "=~");
          quotePo = constraint.indexOf('"');
          constraintBeforeQuotes = quotePo >= 0 ? constraint.substring(0, quotePo) : constraint;
        } else {
          throw new SimpleException(QUERY_ERROR + "Use '=~' instead of '~=' in constraints.");
        }
      }

      // find the valid op within constraintBeforeQuotes
      int op = 0;
      int opPo = -1;
      while (op < OPERATORS.length && (opPo = constraintBeforeQuotes.indexOf(OPERATORS[op])) < 0)
        op++;
      if (opPo < 0) {
        if (repair) continue; // was IllegalArgumentException
        else
          throw new SimpleException(
              QUERY_ERROR + "No operator found in constraint=\"" + constraint + "\".");
      }

      // request uses sequenceName.dataVarName notation?
      String tName = constraint.substring(0, opPo);
      int period = tName.indexOf('.');
      if (period > 0 && tName.substring(0, period).equals(SEQUENCE_NAME))
        tName = tName.substring(period + 1);

      // is it a valid destinationName?
      int dvi = columnNames.indexOf(tName);
      if (dvi < 0) {
        if (repair) continue;
        else
          throw new SimpleException(
              QUERY_ERROR + "Unrecognized constraint variable=\"" + tName + "\"."
              // + "\nValid=" + String2.toCSSVString(dataVariableDestionNames())
              );
      }

      constraintVariables.add(tName);
      constraintOps.add(OPERATORS[op]);
      String tValue = constraint.substring(opPo + OPERATORS[op].length());
      constraintValues.add(tValue);
      double conValueD = Double.NaN;

      if (debugMode) String2.log(">> constraint: " + tName + OPERATORS[op] + tValue);

      // convert <time><op><isoString> to <time><op><epochSeconds>
      boolean constrainTimeStamp = Calendar2.isTimeUnits(columnAttributes(dvi).getString("units"));
      if (constrainTimeStamp) {
        // this isn't precise!!!   should it be required??? or forbidden???
        if (debugMode) String2.log(">> isTimeStamp=true");
        if (tValue.startsWith("\"") && tValue.endsWith("\"")) {
          tValue = String2.fromJsonNotNull(tValue);
          constraintValues.set(constraintValues.size() - 1, tValue);
        }

        // if not for regex, convert isoString to epochSeconds
        if (OPERATORS[op] != PrimitiveArray.REGEX_OP) {
          if (Calendar2.isIsoDate(tValue)) {
            conValueD =
                repair
                    ? Calendar2.safeIsoStringToEpochSeconds(tValue)
                    : Calendar2.isoStringToEpochSeconds(tValue);
            constraintValues.set(constraintValues.size() - 1, "" + conValueD);
            if (debugMode)
              String2.log(
                  ">> TIME CONSTRAINT converted in parseDapQuery: " + tValue + " -> " + conValueD);

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
            if (!Double.isFinite(conValueD) && !tValue.equals("NaN") && !tValue.equals("")) {
              if (repair) {
                tValue = "NaN";
                conValueD = Double.NaN;
                constraintValues.set(constraintValues.size() - 1, tValue);
              } else {
                throw new SimpleException(
                    QUERY_ERROR
                        + "Test for missing time values must use \"NaN\" or \"\", not value=\""
                        + tValue
                        + "\".");
              }
            }
          }
        }

      } else if (getColumn(dvi) instanceof StringArray) {

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
              QUERY_ERROR
                  + "For constraints of String variables, "
                  + "the right-hand-side value must be surrounded by double quotes.\n"
                  + "Bad constraint: "
                  + constraint);
        }

      } else {
        // numeric variables

        // if op=regex, value must have "'s around it
        if (OPERATORS[op] == PrimitiveArray.REGEX_OP) {
          if ((tValue.startsWith("\"") && tValue.endsWith("\"")) || repair) {
            // repair if needed
            if (!tValue.startsWith("\"")) tValue = "\"" + tValue;
            if (!tValue.endsWith("\"")) tValue = tValue + "\"";

            // decode
            tValue = String2.fromJsonNotNull(tValue);
            constraintValues.set(constraintValues.size() - 1, tValue);
          } else {
            throw new SimpleException(
                QUERY_ERROR
                    + "For =~ constraints of numeric variables, "
                    + "the right-hand-side value must be surrounded by double quotes.\n"
                    + "Bad constraint: "
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
                  QUERY_ERROR
                      + "For non =~ constraints of numeric variables, "
                      + "the right-hand-side value must not be surrounded by double quotes.\n"
                      + "Bad constraint: "
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
                  QUERY_ERROR
                      + "Numeric tests of NaN must use \"NaN\", not value=\""
                      + tValue
                      + "\".");
            }
          }
        }
      }
    }

    if (debugMode) {
      String2.log(
          "  Output from parseDapQuery:"
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
   * This converts the missing values to standard (e.g., NaN) missing values, but doesn't erase the
   * missing_value and _FillValue metadata so the table can be reused. This is a convenience for
   * most orderBy() variants.
   *
   * @param keys Only the keys columns are converted.
   * @return true if some values were converted
   */
  public boolean temporarilyConvertToStandardMissingValues(int keys[]) {
    boolean someConverted = false;
    int nKeys = keys.length;
    for (int key = 0; key < nKeys; key++) {
      if (temporarilyConvertToStandardMissingValues(keys[key])) someConverted = true;
    }
    return someConverted;
  }

  /**
   * This converts the missing values to standard (e.g., NaN) missing values, but doesn't erase the
   * missing_value and _FillValue metadata so the table can be reused. This is a convenience for
   * most orderBy() variants.
   *
   * @param col
   * @return true if some values were converted
   */
  public boolean temporarilyConvertToStandardMissingValues(int col) {
    boolean someConverted = false;
    PrimitiveArray pa = getColumn(col);
    if (pa.elementType() == PAType.STRING) return someConverted;
    Attributes colAtt = columnAttributes(col);
    if (pa.convertToStandardMissingValues(
            colAtt.getString("_FillValue"), colAtt.getString("missing_value"))
        > 0) someConverted = true;
    return someConverted;
  }

  /** This converts all columns. */
  public boolean temporarilyConvertToStandardMissingValues() {
    int nCols = nColumns();
    int keys[] = new int[nCols];
    for (int col = 0; col < nCols; col++) keys[col] = col;
    return temporarilyConvertToStandardMissingValues(keys);
  }

  /**
   * This converts standard (e.g., NaN) missing values to the variable's missing_value or _FillValue
   * (preferred). This is a convenience for most orderBy() variants.
   *
   * @param keys Only the keys columns are converted.
   * @return true if some values were converted
   */
  public boolean temporarilySwitchNaNToFakeMissingValues(int keys[]) {
    boolean someConverted = false;
    int nKeys = keys.length;
    for (int key = 0; key < nKeys; key++) {
      if (temporarilySwitchNaNToFakeMissingValues(keys[key])) someConverted = true;
    }
    return someConverted;
  }

  /**
   * This converts standard (e.g., NaN) missing values to the variable's missing_value or _FillValue
   * (preferred). This is a convenience for most orderBy() variants.
   *
   * @param col
   * @return true if some values were converted
   */
  public boolean temporarilySwitchNaNToFakeMissingValues(int col) {
    PrimitiveArray pa = getColumn(col);
    if (pa.elementType() == PAType.STRING) return false;
    Attributes colAtt = columnAttributes(col);
    String safeMV = colAtt.getString("_FillValue"); // fill has precedence
    if (safeMV == null || safeMV.equals("NaN")) safeMV = colAtt.getString("missing_value");
    if (safeMV == null || safeMV.equals("NaN")) {
      // no term was defined
      // for integerType and CharArray, if there are missing values...
      if (pa.getMaxIsMV()) {
        int which = pa.indexOf("");
        if (which >= 0) {
          colAtt.set("_FillValue", pa.missingValue().pa());
        } else {
          // there are no missing values, so maxIsMV is set unnecessarily
          pa.setMaxIsMV(false);
        }
      }
      return false;
    }
    return pa.switchNaNToFakeMissingValue(safeMV) > 0;
  }

  /** This converts all columns. */
  public boolean temporarilySwitchNaNToFakeMissingValues() {
    int nCols = nColumns();
    int keys[] = new int[nCols];
    for (int col = 0; col < nCols; col++) keys[col] = col;
    return temporarilySwitchNaNToFakeMissingValues(keys);
  }

  /**
   * Reduce the table to a subset via a PERCENT-ENCODED DAP query. A variable may be needed only for
   * a constraint; if so, it won't be in the results.
   *
   * @param dapQuery A PERCENT-ENCODED DAP query. Unofficially (e.g., for testing) the query can be
   *     already percent decoded if there are no %dd in the decoded query. This supports filters:
   *     distinct(), orderBy(), orderBy...(), but not orderByMean(), orderBySum() or units().
   * @return the number of rows remaining (may be 0!).
   */
  public int subsetViaDapQuery(String dapQuery) throws Exception {
    String[] parts = getDapQueryParts(dapQuery); // always at least 1: ""
    int nParts = parts.length;
    BitSet keep = null;
    // make SAs to catch results
    StringArray resultsVariables = new StringArray();
    StringArray constraintVariables = new StringArray();
    StringArray constraintOps = new StringArray();
    StringArray constraintValues = new StringArray();

    parseDapQuery(
        dapQuery,
        resultsVariables,
        constraintVariables,
        constraintOps,
        constraintValues,
        false); // repair
    int nCon = constraintVariables.size();
    if (nCon > 0) {
      keep = new BitSet();
      keep.set(0, nRows());
      for (int con = 0; con < nCon; con++) {
        String conName = constraintVariables.get(con);
        String conOp = constraintOps.get(con);
        String conVal = constraintValues.get(con);
        int idCol = findColumnNumber(conName);
        int nRemain = lowApplyConstraint(true, idCol, conName, conOp, conVal, keep);
        if (debugMode) String2.log(">> nRemain=" + nRemain + " after " + conName + conOp + conVal);
        if (nRemain == 0) {
          removeAllRows();
          keep = null;
          break;
        }
      }
    }

    // prune to just requested columns in requested order
    reorderColumns(resultsVariables, true); // discardOthers

    // finally remove unwanted rows
    if (keep != null) justKeep(keep);
    if (nRows() == 0) return 0;

    // deal with filters (e.g., orderBy()) the same way the TableWriters do
    for (int parti = 1; parti < nParts; parti++) {
      String part = parts[parti];
      int partL = part.length();
      if (part.equals("distinct()")) {
        leftToRightSort(nColumns());
        removeDuplicates();
      } else if (part.startsWith("orderBy(\"") && part.endsWith("\")")) {
        ascendingSort(StringArray.arrayFromCSV(part.substring(9, partL - 2)));
      } else if (part.startsWith("orderByDescending(\"") && part.endsWith("\")")) {
        descendingSort(StringArray.arrayFromCSV(part.substring(9, partL - 2)));
      } else if (part.startsWith("orderByClosest(\"") && part.endsWith("\")")) {
        orderByClosest(part.substring(16, partL - 2));
      } else if (part.startsWith("orderByCount(\"") && part.endsWith("\")")) {
        orderByCount(StringArray.arrayFromCSV(part.substring(14, partL - 2)));
      } else if (part.startsWith("orderByLimit(\"") && part.endsWith("\")")) {
        orderByLimit(part.substring(14, partL - 2));
      } else if (part.startsWith("orderByMin(\"") && part.endsWith("\")")) {
        orderByMin(StringArray.arrayFromCSV(part.substring(12, partL - 2)));
      } else if (part.startsWith("orderByMax(\"") && part.endsWith("\")")) {
        orderByMax(StringArray.arrayFromCSV(part.substring(12, partL - 2)));
      } else if (part.startsWith("orderByMinMax(\"") && part.endsWith("\")")) {
        orderByMinMax(StringArray.arrayFromCSV(part.substring(15, partL - 2)));
        // orderByMean isn't supported here! It is just in TableWriterOrderByMean.
        // orderBySum isn't supported here! It is just in TableWriterOrderBySum.
      } // else it's a constraint. If error, parseDapQuery would have caught it.
      // units() is ignored here
    }

    return nRows();
  }

  /**
   * If two or more adjacent rows are duplicates, this removes the duplicates (leaving the original
   * row). Presumably, the file is sorted in a way to make identical rows adjacent, so simple sort()
   * or sortIgnoreCase() are both okay.
   *
   * @return the number of duplicates removed
   */
  public int removeDuplicates() {
    return PrimitiveArray.removeDuplicates(columns);
  }

  /**
   * This moves the specified columns into place and removes other columns.
   *
   * @param colNames
   * @param extraErrorMessage (may be "")
   * @throws IllegalArgumentException if a colName not found.
   */
  public void justKeepColumns(String colNames[], String extraErrorMessage) {
    for (int col = 0; col < colNames.length; col++) {
      int tCol = findColumnNumber(colNames[col]);
      if (tCol < 0)
        throw new IllegalArgumentException(
            "column name=" + colNames[col] + " wasn't found." + extraErrorMessage);
      moveColumn(tCol, col);
    }

    removeColumns(colNames.length, nColumns());
  }

  /**
   * This moves the specified columns into place and removes other columns. This variant creates
   * columns (with missing values) if they didn't exist.
   *
   * @param colNames the desired columnNames
   * @param classes the class for each desired columnName
   */
  public void justKeepColumns(String colNames[], PAType paTypes[]) {
    for (int col = 0; col < colNames.length; col++) {
      int tCol = findColumnNumber(colNames[col]);
      if (tCol >= 0) {
        moveColumn(tCol, col);
        setColumn(col, PrimitiveArray.factory(paTypes[tCol], getColumn(col)));
      } else {
        addColumn(
            col,
            colNames[col],
            PrimitiveArray.factory(paTypes[col], nRows(), ""),
            new Attributes());
      }
    }
    int tnCols = nColumns();
    if (tnCols > colNames.length) {
      if (reallyVerbose) {
        StringArray sa = new StringArray();
        for (int col = colNames.length; col < tnCols; col++) sa.add(getColumnName(col));
        String2.log("Table.justKeepColumns removing excess columns: " + sa.toString());
      }
      removeColumns(colNames.length, tnCols);
    }
  }

  /**
   * This removes rows in which the value in 'column' is less than the value in the previous row.
   * Rows with values of NaN or bigger than 1e300 are also removed. !!!Trouble: one erroneous big
   * value will cause all subsequent valid values to be tossed.
   *
   * @param column the column which should be ascending
   * @return the number of rows removed
   */
  public int ensureAscending(int column) {
    return PrimitiveArray.ensureAscending(columns, column);
  }

  /**
   * The adds the data from each column in other to the end of each column in this. If old column is
   * simpler than new column, old column is upgraded. This column's metadata is unchanged.
   *
   * @param other another table with columns with the same meanings as this table
   */
  public void append(Table other) {
    int n = Math.min(nColumns(), other.nColumns());
    for (int col = 0; col < n; col++) {

      // if needed, make a new wider PrimitiveArray in table1
      PrimitiveArray pa1 = this.getColumn(col);
      PrimitiveArray pa2 = other.getColumn(col);

      PAType needPAType = pa1.needPAType(pa2.elementType());
      if (pa1.elementType() != needPAType) {
        PrimitiveArray newPa1 =
            PrimitiveArray.factory(needPAType, pa1.size() + pa2.size(), false); // active?
        newPa1.append(pa1);
        pa1 = newPa1;
        this.setColumn(col, pa1);
      }

      // append the data from other
      pa1.append(pa2);
    }
  }

  /**
   * This ranks the rows of data in the table by some key columns (each of which can be sorted
   * ascending or descending).
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   * @param ascending try if a given key column should be sorted ascending
   * @return an int[] with values (0 ... size-1) which points to the row number for a row with a
   *     specific rank (e.g., result[0].intValue() is the row number of the first item in the sorted
   *     list, result[1].intValue() is the row number of the second item in the sorted list, ...).
   */
  public int[] rank(int keyColumns[], boolean ascending[]) {
    return PrimitiveArray.rank(columns, keyColumns, ascending);
  }

  /**
   * Like rank, but StringArrays are ranked in a case-insensitive way.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   * @param ascending try if a given key column should be ranked ascending
   */
  public int[] rankIgnoreCase(int keyColumns[], boolean ascending[]) {
    return PrimitiveArray.rankIgnoreCase(columns, keyColumns, ascending);
  }

  /**
   * This sorts the rows of data in the table by some key columns (each of which can be sorted
   * ascending or descending).
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   * @param ascending try if a given key column should be sorted ascending
   */
  public void sort(int keyColumns[], boolean ascending[]) {

    // convert missingValues and _FillValue to NaNs
    // (so mvs sorted consistently, not dependent on mv being a low or high value)
    // boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    PrimitiveArray.sort(columns, keyColumns, ascending);

    // convert NaNs back to _FillValues
    // if (someConverted)
    //    temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /**
   * This sorts the rows of data in the table by some key columns (each of which can be sorted
   * ascending or descending).
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   */
  public void sort(int keyColumns[]) {
    boolean ascending[] = new boolean[keyColumns.length];
    Arrays.fill(ascending, true);

    sort(keyColumns, ascending); // handles missingValues and _FillValues
  }

  /** Like the other sort, but assumes are are ascending. */
  public void sort(String keyNames[]) {
    sort(keyColumnNamesToNumbers("sort", keyNames)); // handles missingValues and _FillValues
  }

  /** Like the other sort, but you can specify ascending. */
  public void sort(String keyNames[], boolean ascending[]) {
    sort(
        keyColumnNamesToNumbers("sort", keyNames),
        ascending); // handles missingValues and _FillValues
  }

  /**
   * Like sort, but StringArrays are sorted in a case-insensitive way. This is more sophisticated
   * than Java's String.CASE_INSENSITIVE_ORDER. E.g., all charAt(0) A's will sort by for all
   * charAt(0) a's (e.g., AA, Aa, aA, aa).
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   * @param ascending try if a given key column should be sorted ascending
   */
  public void sortIgnoreCase(int keyColumns[], boolean ascending[]) {
    // convert missingValues and _FillValue to NaNs
    // (so mvs sorted consistently, not dependent on mv being a low or high value)
    // boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    PrimitiveArray.sortIgnoreCase(columns, keyColumns, ascending);

    // convert NaNs back to _FillValues
    // if (someConverted)
    //    temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /** Like sortIgnoreCase, but based on key column's names. */
  public void sortIgnoreCase(String keyNames[], boolean ascending[]) {
    sortIgnoreCase(keyColumnNamesToNumbers("sortIgnoreCase", keyNames), ascending);
  }

  /**
   * This sorts the rows of data in the table by some key columns (each of which is sorted
   * ascending). This handles missingValues and _FillValues.
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   */
  public void ascendingSort(int keyColumns[]) {
    boolean ascending[] = new boolean[keyColumns.length];
    Arrays.fill(ascending, true);
    sort(keyColumns, ascending);
  }

  /** Like ascendingSort, but based on key column's names. */
  public void ascendingSort(String keyNames[]) {
    ascendingSort(keyColumnNamesToNumbers("orderBy", keyNames));
  }

  public void descendingSort(int keyColumns[]) {
    boolean ascending[] = new boolean[keyColumns.length]; // all false
    sort(keyColumns, ascending);
  }

  /** Like descendingSort, but based on key column's names. */
  public void descendingSort(String keyNames[]) {
    descendingSort(keyColumnNamesToNumbers("orderByDescending", keyNames));
  }

  /**
   * Like ascendingSort, but StringArrays are sorted in a case-insensitive way. This is more
   * sophisticated than Java's String.CASE_INSENSITIVE_ORDER. E.g., all charAt(0) A's will sort by
   * for all charAt(0) a's (e.g., AA, Aa, aA, aa).
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   */
  public void ascendingSortIgnoreCase(int keyColumns[]) {
    boolean ascending[] = new boolean[keyColumns.length];
    Arrays.fill(ascending, true);
    sortIgnoreCase(keyColumns, ascending);
  }

  /** Like ascendingSortIgnoreCase, but based on key column's names. */
  public void ascendingSortIgnoreCase(String keyNames[]) {
    ascendingSortIgnoreCase(keyColumnNamesToNumbers("ascendingSortIgnoreCase", keyNames));
  }

  /**
   * This sorts based on the leftmost nSortColumns (leftmost is most important, all ascending).
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param nSortColumns (leftmost is most important)
   */
  public void leftToRightSort(int nSortColumns) {
    boolean ascending[] = new boolean[nSortColumns];
    Arrays.fill(ascending, true);
    sort(new IntArray(0, nSortColumns - 1).toArray(), ascending);
  }

  /**
   * Like leftToRightSort, but StringArrays are sorted in a case-insensitive way. This is more
   * sophisticated than Java's String.CASE_INSENSITIVE_ORDER. E.g., all charAt(0) A's will sort by
   * for all charAt(0) a's (e.g., AA, Aa, aA, aa).
   *
   * <p>This sort is stable: equal elements will not be reordered as a result of the sort.
   *
   * @param nSortColumns (leftmost is most important)
   */
  public void leftToRightSortIgnoreCase(int nSortColumns) {
    boolean ascending[] = new boolean[nSortColumns];
    Arrays.fill(ascending, true);
    sortIgnoreCase(new IntArray(0, nSortColumns - 1).toArray(), ascending);
  }

  /**
   * This sorts the table by the keyColumns. Then, it replaces rows where the values in the
   * keyColumns are equal, by one row with their average. If the number of rows is reduced to 1/2 or
   * less, this calls trimToSize on each of the PrimitiveArrays. If there are no non-NaN values to
   * average, the average is NaN.
   *
   * @param keyColumns the numbers of the key columns (first is most important)
   */
  public void average(int keyColumns[]) {
    int nRows = nRows();
    if (nRows == 0) return;

    // sort
    sort(keyColumns);

    averageAdjacentRows(keyColumns);
  }

  /**
   * This combines (averages) adjacent rows where the values of the keyColumns are equal. If the
   * number of rows is reduced to 1/2 or less, this calls trimToSize on each of the PrimitiveArrays.
   * If there are no non-NaN values to average, the average is NaN.
   *
   * @param keyColumns the numbers of the key columns
   */
  public void averageAdjacentRows(int keyColumns[]) {
    int nRows = nRows();
    if (nRows == 0) return;

    // make a bitset of sortColumnNumbers
    BitSet isSortColumn = new IntArray(keyColumns).toBitSet();

    // gather sort columns
    int nSortColumns = keyColumns.length;
    PrimitiveArray sortColumns[] = new PrimitiveArray[nSortColumns];
    for (int col = 0; col < nSortColumns; col++) sortColumns[col] = getColumn(keyColumns[col]);

    // go through the data looking for groups of rows with constant values in the sortColumnNumbers
    int nColumns = nColumns();
    int nGood = 0;
    int firstRowInGroup = 0;
    while (firstRowInGroup < nRows) {
      // find lastRowInGroup
      int lastRowInGroup = firstRowInGroup;
      ROW_LOOP:
      for (int row = lastRowInGroup + 1; row < nRows; row++) {
        for (int col = 0; col < nSortColumns; col++) {
          if (sortColumns[col].compare(firstRowInGroup, row) != 0) break ROW_LOOP;
        }
        lastRowInGroup = row;
      }
      // if (reallyVerbose) String2.log("Table.average: first=" + firstRowInGroup +
      //    " last=" + lastRowInGroup);

      // average values in group and store in row nGood
      if (nGood != lastRowInGroup) { // so, the group is not one row, already in place
        for (int col = 0; col < nColumns; col++) {
          PrimitiveArray pa = getColumn(col);
          if (firstRowInGroup == lastRowInGroup
              || // only one row in group
              isSortColumn.get(col)) { // values in sortColumnNumbers in a group are all the same
            pa.copy(firstRowInGroup, nGood);
          } else {
            double sum = 0;
            int count = 0;
            for (int row = firstRowInGroup; row <= lastRowInGroup; row++) {
              double d = pa.getDouble(row);
              if (!Double.isNaN(d)) {
                count++;
                sum += pa.getDouble(row);
              }
            }
            pa.setDouble(nGood, count == 0 ? Double.NaN : sum / count);
          }
        }
      }

      firstRowInGroup = lastRowInGroup + 1;
      nGood++;
    }

    // remove excess at end of column
    for (int col = 0; col < nColumns; col++)
      getColumn(col).removeRange(nGood, getColumn(col).size());

    // trimToSize
    if (nGood <= nRows / 2) {
      for (int col = 0; col < nColumns; col++) getColumn(col).trimToSize();
    }
    if (reallyVerbose)
      String2.log("Table.averageAdjacentRows done. old nRows=" + nRows + " new nRows=" + nGood);
  }

  /**
   * This sorts the table by keyColumns (all ascending) then, for each block where the nKeyColumns-1
   * values are constant, this removes all rows except the one with the last/max value of the last
   * keyColumn. For example, orderByMax([stationID, time/1day, wtmp]) would sort by stationID,
   * time/1day, wtmp, then just keep the row with the max wtmp value for each stationID and
   * time/1day.
   *
   * <p>Rows with missing values for the last column are removed.
   *
   * @param keyColumns 1 or more column numbers (0..).
   * @throws Exception if trouble (e.g., invalid column number). 0 rows is not an error, but returns
   *     0 rows.
   */
  public void orderByMax(int keyColumns[]) throws Exception {

    int nRows = nRows();
    if (nRows <= 1) return;
    int nKeyColumns = keyColumns.length;
    if (nKeyColumns == 0)
      throw new IllegalArgumentException(
          String2.ERROR + " in Table.orderByMax: You must specify at least one keyColumn.");
    int lastKeyColumn = keyColumns[nKeyColumns - 1];

    // Important: temporarily convert keyColumns to standard mv, so missingValues and _FillValues
    // will be handled properly.
    // Convert back below (but just to one of missing_value or _FillValue -- 2 is impossible and
    // confusing).
    boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    String lastKCMV = getColumn(lastKeyColumn).elementType() == PAType.STRING ? "" : "NaN";
    // remove rows with mv for last keyColumn
    nRows =
        tryToApplyConstraintsAndKeep(
            lastKeyColumn,
            new StringArray(new String[] {getColumnName(lastKeyColumn)}),
            new StringArray(new String[] {"!="}),
            new StringArray(new String[] {lastKCMV}));
    if (nRows == 0) return;

    // sort based on keys
    ascendingSort(keyColumns);

    // walk through the table, often marking previous row to be kept
    BitSet keep = new BitSet(nRows); // all false
    keep.set(nRows - 1); // always
    if (nKeyColumns > 1) {
      PrimitiveArray keyCols[] = new PrimitiveArray[nKeyColumns - 1];
      for (int kc = 0; kc < nKeyColumns - 1; kc++) keyCols[kc] = getColumn(keyColumns[kc]);
      for (int row = 1; row < nRows; row++) { // 1 because I'm looking backwards

        // did keyColumns 0 ... n-2 change?
        // work backwards since last most likely to have changed
        // -2 since -1 is the one that is changing (e.g., time)
        for (int kc = nKeyColumns - 2; kc >= 0; kc--) {
          if (keyCols[kc].compare(row - 1, row) != 0) {
            keep.set(row - 1); // something changed, so keep the *previous* row
            break;
          }
        }
      }
    }
    justKeep(keep);

    // convert back to fake missing values
    if (someConverted) temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /**
   * This sorts the table by keyColumns (all ascending) then, for each block where the nKeyColumns-1
   * values are constant, this removes all rows except the one with the first/min value of the last
   * keyColumn. For example, orderByMin([stationID, time]) would sort by stationID and time, then
   * for each stationID, remove all rows except the one for the first time for that stationID.
   *
   * <p>Missing values are treated as however sort() treats them. So they may sort low. So a missing
   * value in the last keyColumn may be the min value. To just get non-missing values, remove
   * missing values ahead of time (e.g., via an ERDDAP query).
   *
   * @param keyColumns 1 or more column numbers (0..).
   * @throws Exception if trouble (e.g., invalid column number) 0 rows is not an error, but returns
   *     0 rows.
   */
  public void orderByMin(int keyColumns[]) throws Exception {

    int nRows = nRows();
    if (nRows <= 1) return;
    int nKeyColumns = keyColumns.length;
    if (nKeyColumns == 0)
      throw new IllegalArgumentException(
          QUERY_ERROR + "in Table.orderByMin: You must specify at least one keyColumn.");
    int lastKeyColumn = keyColumns[nKeyColumns - 1];

    // Important: temporarily convert keyColumns to standard mv, so missingValues and _FillValues
    // will be handled properly.
    // Convert back below (but just to one of missing_value or _FillValue -- 2 is impossible and
    // confusing).
    boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    String lastKCMV = getColumn(lastKeyColumn).elementType() == PAType.STRING ? "" : "NaN";
    // remove rows with mv for last keyColumn
    nRows =
        tryToApplyConstraintsAndKeep(
            lastKeyColumn,
            new StringArray(new String[] {getColumnName(lastKeyColumn)}),
            new StringArray(new String[] {"!="}),
            new StringArray(new String[] {lastKCMV}));
    if (nRows == 0) return;

    // sort based on keys
    ascendingSort(keyColumns);

    // walk through the table, often marking current row to be kept
    BitSet keep = new BitSet(nRows); // all false
    keep.set(0); // always
    if (nKeyColumns > 1) {
      PrimitiveArray keyCols[] = new PrimitiveArray[nKeyColumns - 1];
      for (int kc = 0; kc < nKeyColumns - 1; kc++) keyCols[kc] = getColumn(keyColumns[kc]);
      for (int row = 1; row < nRows; row++) { // 1 because I'm looking backwards

        // did keyColumns 0 ... n-2 change?
        // work backwards since last most likely to have changed
        // n-2 since n-1 is the one that is changing (e.g., time)
        for (int kc = nKeyColumns - 2; kc >= 0; kc--) {
          if (keyCols[kc].compare(row - 1, row) != 0) {
            keep.set(row); // something changed, so keep *this* row
            break;
          }
        }
      }
    }
    justKeep(keep);

    // convert back to fake missing values
    if (someConverted) temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /**
   * This sorts the table by keyColumns (all ascending) then, for each block where the nKeyColumns-1
   * values are constant, this removes all rows except the one with the first/min value and the one
   * with the last/max value of the last keyColumn. For example, orderByMax([stationID, time]) would
   * sort by stationID and time, then for each stationID, remove all rows except the ones for the
   * first and last time for that stationID.
   *
   * <p>If an nKeyColumns-1 combo has only one row, it will be duplicated. Hence, the resulting
   * table will always have just pairs of rows.
   *
   * <p>Missing values are treated as however sort() treats them. So they may sort low or high. So a
   * missing value in the last keyColumn may be the min or max value. To just get non-missing
   * values, remove missing values ahead of time (e.g., via an ERDDAP query).
   *
   * @param keyColumns 1 or more column numbers (0..).
   * @throws Exception if trouble (e.g., invalid column number). 0 rows is not an error, but returns
   *     0 rows.
   */
  public void orderByMinMax(int keyColumns[]) throws Exception {

    int nRows = nRows();
    if (nRows == 0) return;
    int nKeyColumns = keyColumns.length;
    if (nKeyColumns == 0)
      throw new IllegalArgumentException(
          QUERY_ERROR + "in Table.orderByMinMax: You must specify at least one keyColumn.");
    int lastKeyColumn = keyColumns[nKeyColumns - 1];

    // Important: temporarily convert keyColumns to standard mv, so missingValues and _FillValues
    // will be handled properly.
    // Convert back below (but just to one of missing_value or _FillValue -- 2 is impossible and
    // confusing).
    boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    String lastKCMV = getColumn(lastKeyColumn).elementType() == PAType.STRING ? "" : "NaN";
    // remove rows with mv for last keyColumn
    nRows =
        tryToApplyConstraintsAndKeep(
            lastKeyColumn,
            new StringArray(new String[] {getColumnName(lastKeyColumn)}),
            new StringArray(new String[] {"!="}),
            new StringArray(new String[] {lastKCMV}));
    if (nRows == 0) return;

    // sort based on keys
    ascendingSort(keyColumns);

    // minMax
    // walk through the table, often marking previous and current row to be kept
    BitSet keep = new BitSet(nRows); // all false
    keep.set(0); // always
    keep.set(nRows - 1); // always

    // and note which rows need to be duplicated
    ByteArray dupMe = (ByteArray) PrimitiveArray.factory(PAType.BYTE, nRows, "0"); // false
    addColumn("dupMe", dupMe);
    int nDupMe = 0;

    if (nKeyColumns > 1) {
      PrimitiveArray keyCols[] = new PrimitiveArray[nKeyColumns - 1];
      for (int kc = 0; kc < nKeyColumns - 1; kc++)
        keyCols[kc] = getColumn(keyColumns[kc]); // throws exception if not found
      int lastDifferentRow = 0;
      for (int row = 1; row < nRows; row++) { // 1 because I'm looking backwards

        // did keyColumns 0 ... n-2 change?
        // work backwards since last most likely to have changed
        // -2 since -1 is the one that is changing (e.g., time)
        for (int kc = nKeyColumns - 2; kc >= 0; kc--) {
          if (keyCols[kc].compare(row - 1, row) != 0) {
            keep.set(row - 1); // something changed, so keep the *previous* row
            keep.set(row); //  and the *current* row
            if (lastDifferentRow == row - 1) {
              dupMe.set(row - 1, (byte) 1); // true
              nDupMe++;
            }
            lastDifferentRow = row;
            break;
          }
        }
      }
      if (lastDifferentRow == nRows - 1) {
        dupMe.set(nRows - 1, (byte) 1); // true
        nDupMe++;
      }
    } else {
      if (nRows == 1) {
        dupMe.set(0, (byte) 1); // true
        nDupMe++;
      }
    }
    justKeep(keep);
    nRows = nRows();
    removeColumn(nColumns() - 1); // the dupMe column

    // now duplicate the dupMe rows
    if (nDupMe > 0) {
      if (debugMode) String2.log(">> nDupMe=" + nDupMe);

      // do columns one at a time to save memory
      int nCols = nColumns(); // dupMe has been removed
      for (int col = 0; col < nCols; col++) {
        PrimitiveArray oldCol = getColumn(col);
        PrimitiveArray newCol =
            PrimitiveArray.factory(
                oldCol.elementType(), nRows + nDupMe, false); // the elements are not active

        for (int row = 0; row < nRows; row++) {
          newCol.addFromPA(oldCol, row);
          if (dupMe.get(row) == 1) newCol.addFromPA(oldCol, row);
        }

        if (reallyVerbose && col == 0)
          Test.ensureEqual(newCol.size(), nRows + nDupMe, "newSize != expectedNewSize");
        // swap the newCol into place
        setColumn(col, newCol);
      }
    }

    // convert back to fake missing values
    if (someConverted) temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /**
   * This converts an array of column names to column numbers.
   *
   * @param responsible The name of the caller (for an error message).
   * @param keyColumnNames
   * @return an array of column numbers
   * @throws SimpleException if name not found
   */
  public int[] keyColumnNamesToNumbers(String responsible, String[] keyColumnNames) {
    // find the key column numbers
    int keys[] = new int[keyColumnNames.length];
    for (int kc = 0; kc < keyColumnNames.length; kc++) {
      keys[kc] = findColumnNumber(keyColumnNames[kc]);
      if (keys[kc] < 0)
        throw new SimpleException(
            QUERY_ERROR
                + "\""
                + responsible
                + "\" column="
                + keyColumnNames[kc]
                + " isn't in the results table ("
                + columnNames.toString()
                + ").");
    }
    return keys;
  }

  /** keyColumnNameToNumber for one keyColumnName. */
  public int keyColumnNameToNumber(final String responsible, final String keyColumnName) {
    return keyColumnNamesToNumbers(responsible, new String[] {keyColumnName})[0];
  }

  /**
   * Like the other orderByCount, but based on keyColumnNames.
   *
   * @param keyColumnNames 1 or more column names.
   * @throws Exception if trouble (e.g., a keyColumnName not found)
   */
  public void orderByCount(String keyColumnNames[]) throws Exception {
    withRounding(
        "orderByCount",
        keyColumnNames,
        true,
        (keyColNames) -> orderByCount(keyColumnNamesToNumbers("orderByCount", keyColNames)));
  }

  /**
   * Like the other orderByMax, but based on keyColumnNames.
   *
   * @param keyColumnNames 1 or more column names.
   * @throws Exception if trouble (e.g., a keyColumnName not found)
   */
  public void orderByMax(String keyColumnNames[]) throws Exception {
    withRounding(
        "orderByMax",
        keyColumnNames,
        false,
        (keyColNames) -> orderByMax(keyColumnNamesToNumbers("orderByMax", keyColNames)));
  }

  /**
   * Like the other orderByMin, but based on keyColumnNames.
   *
   * @param keyColumnNames 1 or more column names.
   * @throws Exception if trouble (e.g., a keyColumnName not found)
   */
  public void orderByMin(String keyColumnNames[]) throws Exception {
    withRounding(
        "orderByMin",
        keyColumnNames,
        false,
        (keyColNames) -> orderByMin(keyColumnNamesToNumbers("orderByMin", keyColNames)));
  }

  /**
   * Like the other orderByMinMax, but based on keyColumnNames.
   *
   * @param keyColumnNames 1 or more column names.
   * @throws Exception if trouble (e.g., a keyColumnName not found)
   */
  public void orderByMinMax(String keyColumnNames[]) throws Exception {
    withRounding(
        "orderByMinMax",
        keyColumnNames,
        false,
        (keyColNames) -> orderByMinMax(keyColumnNamesToNumbers("orderByMinMax", keyColNames)));
  }

  /**
   * Apply any rounding defined in the keyColumnNames, eg time/1day. Method is private as temporary
   * columns may be created and must be cleaned up afterwards by calling
   * this.removeTempOrderByColumns();
   *
   * @param responsible name of the function responsible
   * @param keyColumnNames an array of the column names used as keys, each maybe having rounding
   * @return the keyColumnNames without rounding directive.
   */
  private void withRounding(
      final String responsible,
      String[] keyColumnNames,
      final boolean isOutputRounded,
      final WithColumnNames action)
      throws Exception {

    final List<Integer> tempOrderByCols = new ArrayList<Integer>();
    final String[] sortKeyColumnNames = deriveActualColumnNames(keyColumnNames);
    final int nRows = nRows();
    final int nKeyColumnNames = keyColumnNames.length;
    for (int i = 0; i < nKeyColumnNames; i++) {
      if (sortKeyColumnNames[i] == keyColumnNames[i].trim()) {
        continue;
      }

      // Bob added
      if (i == nKeyColumnNames - 1
          && keyColumnNames[i].indexOf('/') >= 0
          && (responsible.equals("orderByMax")
              || responsible.startsWith("orderByMin"))) // catches orderByMinMax too
      throw new IllegalArgumentException(
            QUERY_ERROR
                + responsible
                + " cannot apply rounding to "
                + keyColumnNames[i]
                + " because it is the last variable in the CSV list.");

      final int srcColNumber = keyColumnNameToNumber(responsible, sortKeyColumnNames[i]);
      final PrimitiveArray srcColumn = getColumn(srcColNumber);
      int targetColNumber = srcColNumber;
      if (!isOutputRounded) {
        final String keyColumnName =
            keyColumnNames[i].replaceAll("\\W", "."); // eg time/1day -> time.1day
        targetColNumber = findColumnNumber(keyColumnName); // eg time/1day

        // Bob added
        if (!(srcColumn.isFloatingPointType() || srcColumn.isIntegerType())) {
          // cannot apply rounding to this.
          throw new IllegalArgumentException(
              QUERY_ERROR
                  + responsible
                  + " cannot apply rounding to "
                  + keyColumnNames[i]
                  + " because it is not a numeric data type.");
        }

        DoubleArray roundedArray = new DoubleArray(srcColumn);
        if (targetColNumber < 0) {
          targetColNumber = this.addColumn(keyColumnName, roundedArray);
          tempOrderByCols.add(0, targetColNumber);
        } else {
          this.setColumn(targetColNumber, roundedArray);
        }
        sortKeyColumnNames[i] = keyColumnName;
      }
      final PrimitiveArray targetColumn = getColumn(targetColNumber);
      if (!(targetColumn.isFloatingPointType() || targetColumn.isIntegerType())) {
        // cannot apply rounding to this.
        throw new IllegalArgumentException(
            QUERY_ERROR
                + responsible
                + " cannot apply rounding to "
                + keyColumnNames[i]
                + " because it is not a numeric data type.");
      }
      final Rounder rounder = createRounder(responsible, keyColumnNames[i]);
      for (int row = 0; row < nRows; row++) {
        double value = targetColumn.getNiceDouble(row);
        if (!Double.isNaN(value)) {
          try {
            final double rounded = rounder.round(value);
            if (rounded != value) {
              targetColumn.setDouble(row, rounded);
            }
          } catch (Exception e) {
            throw new SimpleException(
                responsible
                    + " problem rounding "
                    + keyColumnNames[i]
                    + " for value="
                    + value
                    + " because "
                    + e,
                e);
          }
        }
      }
    }
    try {
      action.apply(sortKeyColumnNames);
    } finally {
      while (tempOrderByCols.size() > 0) {
        this.removeColumn(tempOrderByCols.remove(0));
      }
    }
  }

  /**
   * This is a higher level orderByClosest that takes the csv string with the names of the orderBy
   * columns plus the interval (e.g., "10 minutes" becomes 600 seconds).
   */
  public void orderByClosest(String orderByCSV) throws Exception {

    if (orderByCSV == null || orderByCSV.trim().length() == 0)
      throw new SimpleException(QUERY_ERROR + "orderByClosest: no csv.");
    String csv[] = String2.split(orderByCSV, ',');
    if (csv.length < 2) throw new SimpleException(QUERY_ERROR + "orderByClosest: csv.length<2.");

    int nKeyCols = csv.length - 1;
    int keyCols[] = new int[nKeyCols];
    for (int k = 0; k < nKeyCols; k++) {
      keyCols[k] = findColumnNumber(csv[k]);
      if (keyCols[k] < 0)
        throw new SimpleException(
            QUERY_ERROR + "orderByClosest: unknown orderBy column=" + csv[k] + ".");
    }

    double numberTimeUnits[] = Calendar2.parseNumberTimeUnits(csv[nKeyCols]); // throws Exception

    orderByClosest(keyCols, numberTimeUnits);
  }

  /** This is a higher level orderByClosest. */
  public void orderByClosest(String orderBy[], double numberTimeUnits[]) throws Exception {

    int nKeyCols = orderBy.length;
    int keyCols[] = new int[nKeyCols];
    for (int k = 0; k < nKeyCols; k++) {
      keyCols[k] = findColumnNumber(orderBy[k]);
      if (keyCols[k] < 0)
        throw new SimpleException(
            QUERY_ERROR + "orderByClosest: unknown orderBy column=" + orderBy[k] + ".");
    }

    orderByClosest(keyCols, numberTimeUnits);
  }

  /**
   * This sorts by keyColumnNames (the last of which must be a timestamp's doubles / epoch seconds),
   * and then just keeps rows which are closest to the time interval (e.g., 10 minutes). Rows with
   * time=NaN are not kept, so this may return 0 rows.
   *
   * @param keyColumns 1 or more column numbers (0..).
   * @param numberTimeUnits e.g., 10 minutes is represented as [numer=10, timeUnits=60] timeUnits
   *     are from Calendar2.factorToGetSeconds. Note that Jan is the 0th month: so 2 months rounds
   *     to Jan 1, Mar 1, May 1, .... When the last keyColumn isn't a time variable, use
   *     TimeUnits=1. This handles timeUnits for Month (30*SECONDS_PER_DAY) and Year
   *     (360*SECONDS_PER_DAY) specially (as calendar months and years).
   * @throws Exception if trouble (e.g., a keyColumnName not found)
   */
  public void orderByClosest(int keyColumns[], double numberTimeUnits[]) throws Exception {

    // just 0 rows?
    int nRows = nRows();
    if (nRows == 0) return;

    int nKeyColumns = keyColumns.length;
    if (nKeyColumns == 0)
      throw new SimpleException(QUERY_ERROR + "orderByClosest: orderBy.length=0.");
    int lastKeyColumn = keyColumns[nKeyColumns - 1];
    PrimitiveArray lastKeyCol = getColumn(lastKeyColumn);

    // ensure lastKeyCol is numeric
    if (lastKeyCol instanceof StringArray)
      throw new IllegalArgumentException(
          QUERY_ERROR
              + "orderByClosest: The last orderBy column="
              + getColumnName(lastKeyColumn)
              + " isn't numeric.");

    // just 1 row?
    if (nRows == 1) {
      if (Double.isNaN(lastKeyCol.getDouble(0))) removeRow(0);
      return;
    }

    // interval
    if (numberTimeUnits == null || numberTimeUnits.length != 2)
      throw new IllegalArgumentException(
          QUERY_ERROR + "orderByClosest: numberTimeUnits.length must be 2.");
    if (!Double.isFinite(numberTimeUnits[0]) || !Double.isFinite(numberTimeUnits[1]))
      throw new IllegalArgumentException(
          QUERY_ERROR + "orderByClosest: numberTimeUnits values can't be NaNs.");
    if (numberTimeUnits[0] <= 0 || numberTimeUnits[1] <= 0)
      throw new IllegalArgumentException(
          QUERY_ERROR + "orderByClosest: numberTimeUnits values must be positive numbers.");
    double simpleInterval = numberTimeUnits[0] * numberTimeUnits[1];
    int field =
        numberTimeUnits[1] == 30 * Calendar2.SECONDS_PER_DAY
            ? Calendar2.MONTH
            : numberTimeUnits[1] == 360 * Calendar2.SECONDS_PER_DAY
                ? Calendar2.YEAR
                : // but see getYear below
                Integer.MAX_VALUE;
    int intNumber = Math2.roundToInt(numberTimeUnits[0]); // used for Month and Year
    if (field != Integer.MAX_VALUE && (intNumber < 1 || intNumber != numberTimeUnits[0]))
      throw new IllegalArgumentException(
          QUERY_ERROR
              + "orderByClosest: The number of months or years must be a positive integer.");
    if (field == Calendar2.MONTH && intNumber > 6)
      throw new IllegalArgumentException(
          QUERY_ERROR + "orderByClosest: The number of months must be 1 ... 6.");

    // handle missing_value and _FillValue
    boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    // sort based on keys
    ascendingSort(keyColumns);

    // walk through the table, within a group:
    //  keep either this row or previous row
    BitSet keep = new BitSet(); // all false
    keep.set(0, nRows); // now keep all / all true
    PrimitiveArray keyCols[] = new PrimitiveArray[nKeyColumns - 1]; // not including time
    for (int kc = 0; kc < nKeyColumns - 1; kc++) keyCols[kc] = getColumn(keyColumns[kc]);
    ROW:
    for (int row = 1; row < nRows; row++) { // 1 because I'm looking backwards

      // are we at start of a new group / did keyColumns 0 ... n-2 change?
      //  If so, continue to next row (no changes to current or previous row's keep value)
      // work backwards since last most likely to have changed
      // -2 since -1 is the one that is changing (e.g., time)
      for (int kc = nKeyColumns - 2; kc >= 0; kc--) {
        if (keyCols[kc].compare(row - 1, row) != 0) {
          if (Double.isNaN(lastKeyCol.getDouble(row))) keep.clear(row);
          continue ROW; // use a label because we are in a local loop
        }
      }

      // if prev or this row is NaN, continue to next row
      double prevRT = lastKeyCol.getDouble(row - 1);
      double thisRT = lastKeyCol.getDouble(row);
      // check isNaN(thisRT) first
      if (Double.isNaN(thisRT)) { // prev has already been checked/cleared.
        keep.clear(row);
        continue;
      }
      if (Double.isNaN(prevRT)) continue;

      // now both prev and this are in same orderBy group and finite
      if (field == Integer.MAX_VALUE) {
        // use simpleInterval
        // if prev and this resolve to different roundTo, continue to next row
        prevRT /= simpleInterval;
        thisRT /= simpleInterval;
        double prevRint = Math.rint(prevRT);
        double thisRint = Math.rint(thisRT);
        if (prevRint != thisRint) continue;

        // now both prev and this are in same group, finite, and roundTo same int
        // clear the further of this or previous
        // > vs >= is arbitrary
        keep.clear(Math.abs(prevRT - prevRint) > Math.abs(thisRT - thisRint) ? row - 1 : row);

      } else { // month or year
        // month

        // prev
        // Finding floor is hard because of BC time and YEAR field being year within era
        //  (so I using getYear(gc) not gc.get(YEAR))
        // I'm sure there is a more efficient way, but this is quick, easy, correct.
        // This is only inefficient when intNumber is big which is unlikely for month and year.
        GregorianCalendar gc = Calendar2.epochSecondsToGc(prevRT);
        Calendar2.clearSmallerFields(gc, field);
        while ((field == Calendar2.YEAR ? Calendar2.getYear(gc) : gc.get(field)) % intNumber != 0
            || Calendar2.gcToEpochSeconds(gc) > prevRT) gc.add(field, -1);
        double prevFloor = Calendar2.gcToEpochSeconds(gc);
        gc.add(field, intNumber);
        double prevCeil = Calendar2.gcToEpochSeconds(gc);
        // < vs <= is arbitrary
        double prevClosest =
            Math.abs(prevRT - prevFloor) < Math.abs(prevRT - prevCeil) ? prevFloor : prevCeil;

        // this
        gc = Calendar2.epochSecondsToGc(thisRT);
        Calendar2.clearSmallerFields(gc, field);
        // String2.log(">> YEAR=" + Calendar2.getYear(gc));
        while ((field == Calendar2.YEAR ? Calendar2.getYear(gc) : gc.get(field)) % intNumber != 0
            || Calendar2.gcToEpochSeconds(gc) > thisRT) gc.add(field, -1);
        double thisFloor = Calendar2.gcToEpochSeconds(gc);
        if (debugMode)
          String2.log(
              ">> this="
                  + Calendar2.safeEpochSecondsToIsoStringTZ(thisRT, "")
                  + " floor="
                  + Calendar2.safeEpochSecondsToIsoStringTZ(thisFloor, "")
                  + " YEAR="
                  + Calendar2.getYear(gc));
        gc.add(field, intNumber);
        double thisCeil = Calendar2.gcToEpochSeconds(gc);
        // < vs <= is arbitrary
        double thisClosest =
            Math.abs(thisRT - thisFloor) < Math.abs(thisRT - thisCeil) ? thisFloor : thisCeil;

        // if prev and this resolve to different roundTo, continue to next row
        if (prevClosest != thisClosest) continue;

        // now both prev and this are in same group, finite, and roundTo same int
        // clear the further of this or previous
        // > vs >= is arbitrary
        keep.clear(Math.abs(prevRT - prevClosest) > Math.abs(thisRT - thisClosest) ? row - 1 : row);
      }
    }
    // String2.log("\nkeep=" + keep.toString() + "\n" + dataToString());
    justKeep(keep);

    // convert back to fake missing values
    if (someConverted) temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /**
   * This sorts the table by keyColumns (all ascending) then, for each block where the nKeyColumns-1
   * values are constant, makes just one row with the count of all non-NaN, non-missingValue,
   * non-_FillValue values of each variable. For example, orderByCount([stationID]) would sort by
   * stationID, then for each stationID, just return a count of the number of non-NaN values for
   * each other variable.
   *
   * <p>Missing values for the keyColumns are treated as however sort() treats them. So they may
   * sort low. So a missing value in the last keyColumn may be the min value.
   *
   * <p>If there are 0 rows: <br>
   * There must still be all keyCols. <br>
   * The non-keyCols will be changed to be IntArrays with size=0.
   *
   * @param keyColumns 0 or more column numbers (0..).
   * @throws Exception if trouble (e.g., invalid column number). 0 rows is not an error, but returns
   *     0 rows.
   */
  public void orderByCount(int keyCols[]) throws Exception {
    if (reallyVerbose) String2.log("* orderByCount(" + String2.toCSSVString(keyCols) + ")");
    int nRows = nRows();
    int nCols = nColumns();
    int nKeyCols = keyCols.length;

    // note which are keyCol
    boolean isKeyCol[] = new boolean[nCols]; // all false
    for (int kc = 0; kc < nKeyCols; kc++) isKeyCol[keyCols[kc]] = true;

    // important: convert all vars to standard mv, so missingValues and _FillValues will be caught
    // do keyCols in reversible way
    boolean someConverted = false;
    for (int col = 0; col < nCols; col++) {
      if (isKeyCol[col]) {
        if (temporarilyConvertToStandardMissingValues(col)) someConverted = true;
      } else {
        convertToStandardMissingValues(col);
      }
    }

    // sort based on keys
    if (nKeyCols > 0) ascendingSort(keyCols);
    // String2.log(dataToString());

    // make resultPAs for count columns (IntArrays)
    // and set units to "count"
    PrimitiveArray resultPAs[] = new PrimitiveArray[nCols];
    for (int col = 0; col < nCols; col++) {
      if (!isKeyCol[col]) {
        resultPAs[col] = new IntArray(32, false);
        columnAttributes(col).set("units", "count");
      }
    }

    // walk through the table
    int resultsRow = -1;
    BitSet keep = new BitSet(nRows); // all false
    for (int row = 0; row < nRows; row++) {

      // isNewGroup?
      boolean isNewGroup = true;
      if (row > 0) {
        isNewGroup = false;
        if (nKeyCols > 0) {
          for (int kc = nKeyCols - 1;
              kc >= 0;
              kc--) { // count down more likely to find change sooner
            if (columns.get(keyCols[kc]).compare(row - 1, row) != 0) {
              isNewGroup = true;
              break;
            }
          }
        }
      }

      if (isNewGroup) {
        resultsRow++;
        keep.set(row);
        // add a row to resultsPAs
        for (int col = 0; col < nCols; col++) {
          if (!isKeyCol[col]) resultPAs[col].addInt(0);
        }
      }

      // increment count?
      for (int col = 0; col < nCols; col++) {
        // String2.log("row=" + row + " col=" + col + "value=\"" + columns.get(col).getString(row) +
        // "\"");
        if (!isKeyCol[col] && String2.isSomething(columns.get(col).getString(row))) {
          resultPAs[col].setInt(resultsRow, resultPAs[col].getInt(resultsRow) + 1);
        }
      }
    }

    // just keep new group
    justKeep(keep);

    // swap resultPAs into place, and remove any missing_value or _FillValue
    for (int col = 0; col < nCols; col++) {
      if (!isKeyCol[col]) {
        columns.set(col, resultPAs[col]);
        Attributes atts = columnAttributes(col);
        atts.remove("missing_value");
        atts.remove("_FillValue");
      }
    }

    // convert keyColumns back
    if (someConverted) temporarilySwitchNaNToFakeMissingValues(keyCols);
  }

  /**
   * This is a higher level orderByLimit that takes the csv string with the names of the orderBy
   * columns (may be none) plus the limitN (e.g., "10").
   */
  public void orderByLimit(String orderByCSV) throws Exception {

    if (orderByCSV == null || orderByCSV.trim().length() == 0)
      throw new SimpleException(QUERY_ERROR + "orderByLimit: no csv.");
    String csv[] = String2.split(orderByCSV, ',');
    if (csv.length == 0) throw new SimpleException(QUERY_ERROR + "orderByLimit: csv.length=0.");

    int nKeyCols = csv.length - 1;
    int limitN = String2.parseInt(csv[nKeyCols]);
    String[] keyColumnNames = new String[nKeyCols];
    System.arraycopy(csv, 0, keyColumnNames, 0, nKeyCols);

    orderByLimit(keyColumnNames, limitN);
    // withRounding("orderByLimit", keyColumnNames, false,
    //    (keyColNames) -> orderByLimit(keyColumnNamesToNumbers("orderByLimit", keyColNames),
    // limitN)
    // );
  }

  /** This is a higher level orderByLimit. */
  public void orderByLimit(String orderBy[], int limitN) throws Exception {

    withRounding(
        "orderByLimit",
        orderBy,
        false,
        (keyColNames) ->
            orderByLimit(keyColumnNamesToNumbers("orderByLimit", keyColNames), limitN));
  }

  /**
   * This sorts by keyColumnNames (may be none) and then just keeps at most limitN rows from each
   * group.
   *
   * @param keyColumns 1 or more column numbers (0..).
   * @param limitN a positive integer, e.g., 10
   * @throws Exception if trouble (e.g., a keyColumnName not found)
   */
  public void orderByLimit(int keyColumns[], int limitN) throws Exception {

    // limitN
    if (limitN < 0 || limitN == Integer.MAX_VALUE)
      throw new IllegalArgumentException(
          QUERY_ERROR + "orderByLimit: limitN=" + limitN + " must be a positive integer.");

    // just 0 or 1 rows?
    int nRows = nRows();
    if (nRows <= 1) return;

    // Important: temporarily convert keyColumns to standard mv, so missingValues and _FillValues
    // will be handled properly.
    // Convert back below (but just to one of missing_value or _FillValue -- 2 is impossible and
    // confusing).
    boolean someConverted = temporarilyConvertToStandardMissingValues(keyColumns);

    // sort based on keys
    int nKeyColumns = keyColumns.length;
    if (nKeyColumns > 0) ascendingSort(keyColumns);

    // walk through the table, within a group:
    //  keep either this row or previous row
    BitSet keep = new BitSet(); // all false
    keep.set(0, nRows); // now keep all / all true
    PrimitiveArray keyCols[] = new PrimitiveArray[nKeyColumns];
    for (int kc = 0; kc < nKeyColumns; kc++) keyCols[kc] = getColumn(keyColumns[kc]);
    int count = 1; // since starting on row 1
    ROW:
    for (int row = 1; row < nRows; row++) { // 1 because I'm looking backwards

      // are we at start of a new group / did keyColumns 0 ... n-1 change?
      //  If so, continue to next row (no changes to current or previous row's keep value)
      // work backwards since last most likely to have changed
      for (int kc = nKeyColumns - 1; kc >= 0; kc--) {
        if (keyCols[kc].compare(row - 1, row) != 0) {
          count = 1; // this is first row in new group
          continue ROW; // use a label because we are in a local loop
        }
      }

      // is count > limitN?
      if (++count > limitN) keep.clear(row);
    }
    // String2.log("\nkeep=" + keep.toString() + "\n" + dataToString());
    justKeep(keep);

    // convert back to fake missing values
    if (someConverted) temporarilySwitchNaNToFakeMissingValues(keyColumns);
  }

  /**
   * This is like the other saveAsMatlab, but writes to a file.
   *
   * @param fullName The full file name (dir + name + ext (usually .mat))
   * @param varName if varName isn't a valid Matlab variable name, it will be made so via
   *     String2.modifyToBeVariableNameSafe().
   */
  public void saveAsMatlab(String fullName, String varName) throws Exception {
    if (reallyVerbose) String2.log("Table.saveAsMatlab " + fullName);
    long time = System.currentTimeMillis();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // write to dataOutputStream
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fullName + randomInt));

    try {
      saveAsMatlab(bos, varName); // it calls modifyToBeVariableNameSafe
      bos.close();
      bos = null;

      // rename the file to the specified name, instantly replacing the original file
      File2.rename(fullName + randomInt, fullName); // throws Exception if trouble

    } catch (Exception e) {
      // try to close the file
      try {
        if (bos != null) bos.close();
      } catch (Exception e2) {
        // don't care
      }

      // delete the partial file
      File2.delete(fullName + randomInt);
      // delete any existing file
      File2.delete(fullName);

      throw e;
    }

    // Old way relies on script which calls Matlab.
    // This relies on a proprietary program, so good to remove it.
    // cShell("/u00/chump/grdtomatlab " + fullGrdName + " " +
    //    fullResultName + randomInt + ".mat " + varName);

    if (reallyVerbose)
      String2.log(
          "  Table.saveAsMatlab done. fileName="
              + fullName
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * Save this table data as a Matlab .mat file. This writes the lon values as they are currently in
   * this table (e.g., +-180 or 0..360). This overwrites any existing file of the specified name.
   * This makes an effort not to create a partial file if there is an error. If no exception is
   * thrown, the file was successfully created.
   *
   * @param outputStream usually already buffered. Afterwards, it is flushed, not closed.
   * @param varName the name to use for the variable which holds all of the data, usually the
   *     dataset's internal name.
   * @throws Exception
   */
  /* commented out 2008-02-07
      public void saveAsMatlab(OutputStream outputStream, String varName)
          throws Exception {
          if (reallyVerbose) String2.log("Table.saveAsMatlab outputStream");
          long time = System.currentTimeMillis();

          String errorInMethod = String2.ERROR + " in Table.saveAsMatlab:\n";

          //make sure there is data
          if (nRows() == 0)
              throw new SimpleException(errorInMethod + MustBe.THERE_IS_NO_DATA);

          //open a dataOutputStream
          DataOutputStream dos = new DataOutputStream(outputStream);

          //write the header
          Matlab.writeMatlabHeader(dos);

          //make an array of the data[row][col]
          int tnRows = nRows();
          int tnCols = nColumns();
          double ar[][] = new double[tnRows][tnCols];
          for (int col = 0; col < tnCols; col++) {
              PrimitiveArray pa = getColumn(col);
              if (pa.elementType() == PAType.STRING) {
                  for (int row = 0; row < tnRows; row++) {
                      ar[row][col] = Double.NaN; //can't store strings in a double array
                  }
              } else {
                  for (int row = 0; row < tnRows; row++)
                      ar[row][col] = pa.getNiceDouble(row);
              }
          }
          Matlab.write2DDoubleArray(dos, varName, ar);

          //this doesn't write attributes because .mat files don't store attributes
          //setStatsAttributes(true); //true = double
          //write the attributes...

          dos.flush(); //essential

          if (reallyVerbose) String2.log("  Table.saveAsMatlab done. TIME=" +
              (System.currentTimeMillis() - time) + "ms");
      }
  */
  /**
   * Save this table data as a Matlab .mat file. This writes the lon values as they are currently in
   * this table (e.g., +-180 or 0..360). This overwrites any existing file of the specified name.
   * This makes an effort not to create a partial file if there is an error. If no exception is
   * thrown, the file was successfully created. This maintains the data types (Strings become
   * char[][]). Missing values should already be stored as NaNs (perhaps via
   * convertToStandardMissingValues()).
   *
   * <p>If the columns have different lengths, they will be stored that way in the file(!).
   *
   * @param outputStream usually already buffered. Afterwards, it is flushed, not closed.
   * @param structureName the name to use for the Matlab structure which holds all of the data,
   *     usually the dataset's internal name. If varName isn't a valid Matlab variable name, it will
   *     be made so via String2.encodeMatlabNameSafe().
   * @throws Exception
   */
  public void saveAsMatlab(OutputStream outputStream, String structureName) throws Exception {
    if (reallyVerbose) String2.log("Table.saveAsMatlab outputStream");
    long time = System.currentTimeMillis();
    structureName = String2.encodeMatlabNameSafe(structureName);

    String errorInMethod = String2.ERROR + " in Table.saveAsMatlab:\n";

    // make sure there is data
    if (nRows() == 0)
      throw new SimpleException(errorInMethod + MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // calculate the size of the structure
    int nCols = nColumns();
    byte structureNameInfo[] = Matlab.nameInfo(structureName);
    NDimensionalIndex ndIndex[] = new NDimensionalIndex[nCols];
    long cumSize = // see 1-32
        16
            + // for array flags
            16
            + // my structure array is always 2 dimensions
            structureNameInfo.length
            + 8
            + // field name length (for all fields)
            8
            + nCols * 32L; // field names
    for (int col = 0; col < nCols; col++) {
      // String ndIndex takes time to make; so make it and store it for use below
      ndIndex[col] = Matlab.make2DNDIndex(getColumn(col));
      // if (reallyVerbose) String2.log("  " + getColumnName(col) + " " + ndIndex[col]);
      // add size of each cell
      cumSize +=
          8
              + // type and size
              Matlab.sizeOfNDimensionalArray( // throws exception if too big for Matlab
                  "", // without column names (they're stored separately)
                  getColumn(col),
                  ndIndex[col]);
    }
    if (cumSize >= Integer.MAX_VALUE - 1000)
      throw new RuntimeException(
          "Too much data: Matlab structures must be < Integer.MAX_VALUE bytes.");

    // open a dataOutputStream
    DataOutputStream stream = new DataOutputStream(outputStream);

    // ******** Matlab Structure
    // *** THIS CODE MIMICS EDDTable.saveAsMatlab. If make changes here, make them there, too.
    //    The code in EDDGrid.saveAsMatlab is similar, too.
    // write the header
    Matlab.writeMatlabHeader(stream);

    // *** write Matlab Structure  see 1-32
    // write the miMatrix dataType and nBytes
    stream.writeInt(Matlab.miMATRIX); // dataType
    stream.writeInt((int) cumSize); // safe since checked above

    // write array flags
    stream.writeInt(Matlab.miUINT32); // dataType
    stream.writeInt(8); // fixed nBytes of data
    stream.writeInt(Matlab.mxSTRUCT_CLASS); // array flags
    stream.writeInt(0); // reserved; ends on 8 byte boundary

    // write structure's dimension array
    stream.writeInt(Matlab.miINT32); // dataType
    stream.writeInt(2 * 4); // nBytes
    // matlab docs have 2,1, octave has 1,1.
    // Think of structure as one row of a table, where elements are entire arrays:  e.g., sst.lon
    // sst.lat sst.sst.
    // Having multidimensions (e.g., 2 here) lets you have additional rows, e.g., sst(2).lon
    // sst(2).lat sst(2).sst.
    // So 1,1 makes sense.
    stream.writeInt(1);
    stream.writeInt(1);

    // write structure name
    stream.write(structureNameInfo, 0, structureNameInfo.length);

    // write length for all field names (always 32)  (short form)
    stream.writeShort(4); // nBytes
    stream.writeShort(Matlab.miINT32); // dataType
    stream.writeInt(32); // nBytes per field name

    // write the field names (each 32 bytes)
    stream.writeInt(Matlab.miINT8); // dataType
    stream.writeInt(nCols * 32); // nBytes per field name
    String nulls = String2.makeString('\u0000', 32);
    for (int col = 0; col < nCols; col++)
      stream.write(
          String2.toByteArray(
              String2.noLongerThan(String2.encodeMatlabNameSafe(getColumnName(col)), 31) + nulls),
          0,
          32); // EEEK! better not be longer!!!

    // write the structure's elements (one for each col)
    for (int col = 0; col < nCols; col++)
      Matlab.writeNDimensionalArray(
          stream,
          "", // without column names (they're stored separately)
          getColumn(col),
          ndIndex[col]);

    // this doesn't write attributes because .mat files don't store attributes

    stream.flush(); // essential

    if (reallyVerbose)
      String2.log("  Table.saveAsMatlab done. TIME=" + (System.currentTimeMillis() - time) + "ms");
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
   * !!!The table should initially have missing values stored as NaNs. NaN's are converted to
   * DataHelper.FAKE_MISSING_VALUE temporarily.
   *
   * @param fullName The full file name (dir + name + ext (usually .nc))
   * @param dimensionName the name for the rows dimension, e.g., usually "time", "station",
   *     "observation", "trajectory", "row", or ...?
   *     <p>OBSOLETE [To conform to the Unidata Observation Dataset Conventions
   *     (https://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html): This
   *     sets the global attribute observationDimension={dimensionName}.]
   * @throws Exception
   */
  public void saveAsFlatNc(String fullName, String dimensionName) throws Exception {
    saveAsFlatNc(fullName, dimensionName, true);
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
   * LongArray columns are saved as DoubleArray.
   *
   * @param fullName The full file name (dir + name + ext (usually .nc))
   * @param dimensionName the name for the rows dimension, e.g., usually "time", "station",
   *     "observation", "trajectory", "row", or ...?
   *     <p>OBSOLETE [To conform to the Unidata Observation Dataset Conventions
   *     (https://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html):
   *     [GONE!] This sets the global attribute observationDimension={dimensionName}.]
   * @param convertToFakeMissingValues if true, NaN's are converted to DataHelper.FAKE_MISSING_VALUE
   *     temporarily.
   * @throws Exception
   */
  public void saveAsFlatNc(
      String fullName, String dimensionName, boolean convertToFakeMissingValues) throws Exception {
    String msg = "  Table.saveAsFlatNc " + fullName;
    long time = System.currentTimeMillis();

    // String2.log("saveAsFlatNc first 5 rows:" + toString(5));

    // this method checks validity because it is used as a fundamental part
    // of EDDGridFromFiles and EDDTableFromFiles fileTable
    // and because code below throws null pointer exception (not descriptive or helpful)
    // if 2 vars have same name
    ensureValid();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFormatWriter ncWriter = null;
    boolean nc3Mode = true;
    try {
      NetcdfFormatWriter.Builder nc = NetcdfFormatWriter.createNewNetcdf3(fullName + randomInt);
      Group.Builder rootGroup = nc.getRootGroup();
      nc.setFill(false);

      // items determined by looking at a .nc file; items written in that order

      int nRows = nRows();
      int nColumns = nColumns();
      if (nRows == 0) {
        throw new Exception(
            String2.ERROR + " in" + msg + ":\n" + MustBe.THERE_IS_NO_DATA + " (nRows = 0)");
      }
      PrimitiveArray tPA[] = new PrimitiveArray[nColumns];

      // define the dimensions
      Dimension dimension = NcHelper.addDimension(rootGroup, dimensionName, nRows);
      // javadoc says: if there is an unlimited dimension, all variables that use it are in a
      // structure
      // Dimension rowDimension = new Dimension("row", nRows, true, true, false); //isShared,
      // isUnlimited, isUnknown
      // rootGroup.addDimension(dimension);
      // String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

      // add the variables
      Variable.Builder colVars[] = new Variable.Builder[nColumns];
      for (int col = 0; col < nColumns; col++) {
        String tColName = getColumnNameWithoutSpaces(col);
        PrimitiveArray pa = getColumn(col);
        tPA[col] = pa;
        PAType type = pa.elementType();
        if (type == PAType.STRING) {
          int max =
              Math.max(
                  1,
                  ((StringArray) pa)
                      .maxStringLength()); // nc libs want at least 1; 0 happens if no data
          Dimension lengthDimension =
              NcHelper.addDimension(rootGroup, tColName + NcHelper.StringLengthSuffix, max);
          colVars[col] =
              NcHelper.addVariable(
                  rootGroup, tColName, DataType.CHAR, Arrays.asList(dimension, lengthDimension));
        } else {
          colVars[col] =
              NcHelper.addVariable(
                  rootGroup, tColName, NcHelper.getNc3DataType(type), Arrays.asList(dimension));
        }
        // nc.addMemberVariable(recordStructure, nc.findVariable(tColName));
      }

      // boolean bool = nc.addRecordStructure(); //creates a structure variable called "record"
      // String2.log("addRecordStructure: " + bool);
      // Structure recordStructure = (Structure)nc.findVariable("record");

      // set id attribute = file name
      // currently, only the .nc saveAs types use attributes!
      if (globalAttributes.get("id") == null)
        globalAttributes.set("id", File2.getNameNoExtension(fullName));

      // set the globalAttributes
      NcHelper.setAttributes(nc3Mode, rootGroup, globalAttributes);

      for (int col = 0; col < nColumns; col++) {
        // convert to fake MissingValues   (in time to write attributes)
        PAType tc = tPA[col].elementType();
        if (convertToFakeMissingValues) convertToFakeMissingValues(col);

        Attributes tAtts = new Attributes(columnAttributes(col)); // use a copy
        // String2.log(">> saveAsFlatNc col=" + tPA[col].elementTypeString() + " enc=" +
        // tAtts.getString(File2.ENCODING));
        if (tc == PAType.STRING
            && tAtts.getString(File2.ENCODING) == null) // don't change if already specified
        tAtts.add(File2.ENCODING, File2.ISO_8859_1);
        // disabled until there is a standard
        //                else if (tc == PAType.CHAR)
        //                    tAtts.add(String2.CHARSET, File2.ISO_8859_1);

        NcHelper.setAttributes(nc3Mode, colVars[col], tAtts, tc.isUnsigned());
      }

      // leave "define" mode
      ncWriter = nc.build();

      // write the data
      for (int col = 0; col < nColumns; col++) {
        // String2.log("writing col=" + col + " " + getColumnName(col) +
        //    " colVars[col]=" + colVars[col] +
        //    " size=" + tPA[col].size() + " tPA[col]=" + tPA[col]);
        ncWriter.write(colVars[col].getFullName(), NcHelper.get1DArray(tPA[col]));

        // convert back to standard MissingValues
        if (convertToFakeMissingValues) convertToStandardMissingValues(col);
      }

      // if close throws exception, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name, instantly replacing the original file
      File2.rename(fullName + randomInt, fullName); // throws Exception if trouble

      // diagnostic
      if (reallyVerbose)
        String2.log(
            msg
                + " done. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
      // String2.log(NcHelper.ncdump(directory + name + ext, "-h");

    } catch (Exception e) {
      String2.log(
          NcHelper.ERROR_WHILE_CREATING_NC_FILE + "\n" + msg + "\n" + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
        File2.delete(fullName + randomInt);
      }

      // delete any existing file
      File2.delete(fullName);
      if (!reallyVerbose) String2.log(msg);

      throw e;
    }
  }

  /**
   * THIS IS NOT FINISHED. This creates or appends the data to a flat .nc file with an unlimited
   * dimension. If the file doesn't exist, it will be created. If the file does exist, the data will
   * be appended.
   *
   * <p>If the file is being created, the attributes are used (otherwise, they aren't). String
   * variables should have an integer "strlen" attribute which specifies the maximum number of
   * characters for the column (otherwise, strlen will be calculated from the longest String in the
   * current chunk of data and written as the strlen attribute).
   *
   * <p>If the file exists and a colName in this table isn't in it, the column is ignored.
   *
   * <p>If the file exists and this table lacks a column in it, the column will be filled with the
   * file-defined _FillValue (first choice) or missing_value or the standard PrimitiveArray missing
   * value (last choice).
   *
   * @param fileName
   * @param dimName e.g., "time" or
   * @throws Exception if trouble (but the file will be closed in all cases)
   */
  /*    public static void saveAsUnlimitedNc(String fileName, String dimName) throws Exception {

          String msg = "  Table.saveAsUnlimited " + fileName;
          long time = System.currentTimeMillis();
          int nCols = nColumns();
          int nRows = nRows();
          int strlens[] = new int[nCols];  //all 0's
          boolean fileExists = File2.isFile(fileName);
          NetcdfFormatWriter ncWriter = null;
          Dimension dim;

          try {
              NetcdfFormatWriter.Builder file = null;
              Group.Builder rootGroup = null;

              if (fileExists) {
                  file = NetcdfFormatWriter.openExisting(fileName);
                  rootGroup = file.getRootGroup();
                  dim = file.findDimension(dimName);

              } else {
                  //create the file
                  file = NetcdfFormatWriter.createNewNetcdf3(fileName);
                  boolean nc3Mode = true;
                  rootGroup = file.getRootGroup();

                  NcHelper.setAttributes(rootGroup, globalAttributes());

                  //define unlimited dimension
                  dim = file.addUnlimitedDimension(dimName);
                  ArrayList<Dimension> dims = new ArrayList();
                  dims.add(dim);

                  //define Variables
                  Variable.Builder colVars[] = new Variable.Builder[nCols];
                  for (int col = 0; col < nCols; col++) {
                      String colName = getColumnName(col);
                      PrimitiveArray pa = column(col);
                      Attributes atts = new Attributes(columnAttributes(col)); //use a copy
                      if (pa.elementType() == PAType.STRING) {
                          //create a string variable
                          int strlen = atts.getInt("strlen");
                          if (strlen <= 0 || strlen == Integer.MAX_VALUE) {
                              strlen = Math.max(1, ((StringArray)pa). maximumLength());
                              atts.set("strlen", strlen);
                          }
                          strlens[col] = strlen;
                          Dimension tDim = file.addUnlimitedDimension(colName + "_strlen");
                          ArrayList<Dimension> tDims = new ArrayList();
                          tDims.add(dim);
                          tDims.add(tDim);
                          colVars[col] = NcHelper.addNc3StringVariable(rootGroup, colName, dims, strlen);

                      } else {
                          //create a non-string variable
                          colVars[col] = NcHelper.addVariable(rootGroup, colName,
                              NcHelper.getNc3DataType(pa.elementType()), dims);
                      }

                      if (pa.elementType() == PAType.CHAR)
                          atts.add(String2.CHARSET, File2.ISO_8859_1);
                      else if (pa.elementType() == PAType.STRING)
                          atts.add(File2.ENCODING, File2.ISO_8859_1);
                      NcHelper.setAttributes(colVars[col], atts);
                  }

                  //switch to create mode
                  ncWriter = file.build();
              }

              //add the data
              int fileNRows = dim.getLength();
              int[] origin1 = new int[] {row};
              int[] origin2 = new int[] {row, 0};
              vars
              while (...) {

                  Variable var = vars.get  ;
                  class elementPAType = NcHelper.  var.
                  //ArrayList<Dimension> tDims = var.getDimensions();
                  String colName = var.getFullName();
                  Attributes atts = new Attributes();
                  NcHelper.getAttributes(colName, atts);
                  int col = findColumnNumber(colName);
                  PrimitiveArray pa = null;
                  if (col < 0) {
                      //the var has nothing comparable in this table,
                      //so make a pa filled with missing values
                      if (ndims > 1) {
                          //string vars always use "" as mv
                          continue;
                      }
                      //make a primitive array
                      PrimitiveArray pa = PrimitiveArray.factory(type, 1, false);
                      String mv = atts.getString("_FillValue");
                      if (mv == null)
                          mv = atts.getString("missing_value");
                      if (mv == null)
                          mv = pa.getMV();
                      pa.addNStrings(nRows, mv);

                  } else {
                      //get data from this table
                      pa = getColumn(col);
                  }

                  //write the data
                  if (pa.elementType() == PAType.STRING) {
                      //write string data
                      if (fileExists) {
                          ..just get one att from file
                          Attributes atts = columnAttributes(col);
                          strlens[col] = NcHelper.getAttribute ts.getInt("strlen");
                      }
                      if (strlens[col] <= 0 || strlens[col] == Integer.MAX_VALUE)
                          throw new SimpleException("\"strlen\" attribute not found for variable=" + colName);

                      ArrayChar.D2 ac = new ArrayChar.D2(2, strlens[col]);
                      int n = pa.size();
                      for (int i = 0; i < n; i++)
                          ac.setString(i, pa.getString(i));
                      ncWriter.write(colVars[col].getFullName(), origin2, ac);

                  } else {
                      //write non-string data
                      ncWriter.write(colVars[col].getFullName(), origin1, Array.factory(pa.toArray()));
                  }
              }
              ncWriter.close();
              ncWriter = null;

              if (reallyVerbose) msg +=
                  " finished. nColumns=" + nColumns() + " nRows=" + nRows() +
                  " TIME=" + (System.currentTimeMillis() - time) + "ms";

          } catch (Throwable t) {
              String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(t));
              if (ncWriter != null) {
                  try {ncWriter.abort(); } catch (Exception e9) {}
                  File2.delete(fileName);
                  ncWriter = null;
              }

              if (!reallyVerbose) String2.log(msg);
              throw t;

          }
      }
  */

  /** This is like saveAs4DNc but with no StringVariable option. */
  public void saveAs4DNc(String fullName, int xColumn, int yColumn, int zColumn, int tColumn)
      throws Exception {

    saveAs4DNc(fullName, xColumn, yColumn, zColumn, tColumn, null, null, null);
  }

  /**
   * This is like saveAs4DNc but removes stringVariableColumn (often column 4 = "ID", which must
   * have just 1 value, repeated) and saves it as a stringVariable in the 4DNc file, and then
   * reinserts the stringVariableColumn. For files with just 1 station's data, Dapper and DChart
   * like this format.
   *
   * @param stringVariableColumn is the column (
   */
  public void saveAs4DNcWithStringVariable(
      String fullName, int xColumn, int yColumn, int zColumn, int tColumn, int stringVariableColumn)
      throws Exception {

    // remove ID column
    String tName = getColumnName(stringVariableColumn);
    Attributes tIdAtt = columnAttributes(stringVariableColumn);
    PrimitiveArray tIdPa = getColumn(stringVariableColumn);
    removeColumn(stringVariableColumn);

    // save as 4DNc
    saveAs4DNc(fullName, xColumn, yColumn, zColumn, tColumn, tName, tIdPa.getString(0), tIdAtt);

    // reinsert ID column
    addColumn(stringVariableColumn, tName, tIdPa, tIdAtt);
  }

  /**
   * Save this table of data as a 4D netCDF .nc file using the currently available attributes. This
   * method uses the terminology x,y,z,t, but does require that the data represent lon,lat,alt,time.
   * All columns other than the 4 dimension related columns are stored as 4D arrays. This will sort
   * the values t (primary key), then z, then y, then x (least important key). The data values are
   * written as their current data type (e.g., float or int). This writes the lon values as they are
   * currently in this table (e.g., +-180 or 0..360). This overwrites any existing file of the
   * specified name. This makes an effort not to create a partial file if there is an error. If no
   * exception is thrown, the file was successfully created. !!!The file must have at least one row,
   * or an Exception will be thrown (nc dimensions can't be 0 length). This tries to look like it
   * works instantaneously: it writes to a temp file then renames it to correct name.
   *
   * <p>This supports an optional stringVariable which is written to the file as a 1D char array.
   * Dapper/DChart prefers this to a 4D array for the ID info.
   *
   * @param fullName The full file name (dir + name + ext (usually .nc))
   * @param xColumn the column with lon info.
   * @param yColumn the column with lat info.
   * @param zColumn the column with alt info.
   * @param tColumn the column with time info.
   * @param stringVariableName the name for the optional 1D String variable (or null to not use this
   *     feature)
   * @param stringVariableValue the value for the optional 1D String variable (must be non-null
   *     non-"" if stringVariableName isn't null)
   * @param stringVariableAttributes the attributes for the optional 1D String variable (must be
   *     non-null if stringVariableName isn't null)
   * @throws Exception
   */
  public void saveAs4DNc(
      String fullName,
      int xColumn,
      int yColumn,
      int zColumn,
      int tColumn,
      String stringVariableName,
      String stringVariableValue,
      Attributes stringVariableAttributes)
      throws Exception {
    String msg = "  Table.saveAs4DNc " + fullName;
    long time = System.currentTimeMillis();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // ensure there is data
    String errorInMethod = String2.ERROR + " in" + msg + ":\n";
    if (stringVariableName != null) {
      Test.ensureNotEqual(
          stringVariableName.length(), 0, errorInMethod + "stringVariableName is \"\".");
      if (stringVariableValue == null)
        throw new SimpleException(errorInMethod + "stringVariableValue is null.");
      Test.ensureNotEqual(
          stringVariableValue.length(), 0, errorInMethod + "stringVariableValue is \"\".");
    }
    if (nRows() == 0) {
      throw new Exception(errorInMethod + MustBe.THERE_IS_NO_DATA + " (nRows = 0)");
    }

    // open the file (before 'try'); if it fails, no temp file to delete
    NetcdfFormatWriter ncWriter = null;
    long make4IndicesTime = -1;
    boolean nc3Mode = true;

    try {
      NetcdfFormatWriter.Builder nc = NetcdfFormatWriter.createNewNetcdf3(fullName + randomInt);
      Group.Builder rootGroup = nc.getRootGroup();
      nc.setFill(false);

      // if (reallyVerbose) {
      //    String2.log("Table.saveAs4DNc" +
      //        "\n     raw X " + getColumn(xColumn).statsString() +
      //        "\n     raw Y " + getColumn(yColumn).statsString() +
      //        "\n     raw Z " + getColumn(zColumn).statsString() +
      //        "\n     raw T " + getColumn(tColumn).statsString());
      // }

      // sort
      PrimitiveArray.sort(
          columns,
          new int[] {tColumn, zColumn, yColumn, xColumn},
          new boolean[] {true, true, true, true});

      // add axis attributes
      columnAttributes(xColumn).set("axis", "X");
      columnAttributes(yColumn).set("axis", "Y");
      columnAttributes(zColumn).set("axis", "Z");
      columnAttributes(tColumn).set("axis", "T");

      // make the indices
      make4IndicesTime = System.currentTimeMillis();
      IntArray xIndices = new IntArray();
      IntArray yIndices = new IntArray();
      IntArray zIndices = new IntArray();
      IntArray tIndices = new IntArray();
      PrimitiveArray uniqueX = getColumn(xColumn).makeIndices(xIndices);
      PrimitiveArray uniqueY = getColumn(yColumn).makeIndices(yIndices);
      PrimitiveArray uniqueZ = getColumn(zColumn).makeIndices(zIndices);
      PrimitiveArray uniqueT = getColumn(tColumn).makeIndices(tIndices);
      make4IndicesTime = System.currentTimeMillis() - make4IndicesTime;
      // if (reallyVerbose) {
      //    String2.log("Table.saveAs4DNc" +
      //        "\n  unique X " + uniqueX.statsString() +
      //        "\n  unique Y " + uniqueY.statsString() +
      //        "\n  unique Z " + uniqueZ.statsString() +
      //        "\n  unique T " + uniqueT.statsString());
      //    //String2.pressEnterToContinue();
      // }

      int nX = uniqueX.size();
      int nY = uniqueY.size();
      int nZ = uniqueZ.size();
      int nT = uniqueT.size();

      // items determined by looking at a .nc file; items written in that order
      int nRows = nRows();
      int nColumns = nColumns();
      if (nRows == 0) {
        throw new Exception(String2.ERROR + " in Table.saveAs4DNc:\nThe table has no data.");
      }
      int stringLength[] = new int[nColumns];

      // define the dimensions
      Dimension xDimension =
          NcHelper.addDimension(rootGroup, getColumnNameWithoutSpaces(xColumn), nX);
      Dimension yDimension =
          NcHelper.addDimension(rootGroup, getColumnNameWithoutSpaces(yColumn), nY);
      Dimension zDimension =
          NcHelper.addDimension(rootGroup, getColumnNameWithoutSpaces(zColumn), nZ);
      Dimension tDimension =
          NcHelper.addDimension(rootGroup, getColumnNameWithoutSpaces(tColumn), nT);
      // javadoc says: if there is an unlimited dimension, all variables that use it are in a
      // structure
      // Dimension rowDimension  = new Dimension("row", nRows, true, true, false); //isShared,
      // isUnlimited, isUnknown
      // String2.log("unlimitied dimension exists: " + (nc.getUnlimitedDimension() != null));

      // add the variables
      Variable.Builder colVars[] = new Variable.Builder[nColumns];
      for (int col = 0; col < nColumns; col++) {

        // for x/y/z/t make a 1D variable
        PrimitiveArray pa = null;
        if (col == xColumn || col == yColumn || col == zColumn || col == tColumn) {
          Dimension aDimension = null;
          if (col == xColumn) {
            pa = uniqueX;
            aDimension = xDimension;
          } else if (col == yColumn) {
            pa = uniqueY;
            aDimension = yDimension;
          } else if (col == zColumn) {
            pa = uniqueZ;
            aDimension = zDimension;
          } else if (col == tColumn) {
            pa = uniqueT;
            aDimension = tDimension;
          }
          PAType type = pa.elementType();
          String tColName = getColumnNameWithoutSpaces(col);
          if (type == PAType.STRING) {
            int max =
                Math.max(
                    1,
                    ((StringArray) pa)
                        .maxStringLength()); // nc libs want at least 1; 0 happens if no data
            stringLength[col] = max;
            Dimension lengthDimension =
                NcHelper.addDimension(rootGroup, tColName + NcHelper.StringLengthSuffix, max);
            colVars[col] =
                NcHelper.addVariable(
                    rootGroup, tColName, DataType.CHAR, Arrays.asList(aDimension, lengthDimension));
          } else {
            colVars[col] =
                NcHelper.addVariable(
                    rootGroup, tColName, NcHelper.getNc3DataType(type), Arrays.asList(aDimension));
          }
        } else {

          // for other columns, make a 4D array
          pa = getColumn(col);
          PAType type = pa.elementType();
          String tColName = getColumnNameWithoutSpaces(col);
          if (type == PAType.STRING) {
            int max =
                Math.max(
                    1,
                    ((StringArray) pa)
                        .maxStringLength()); // nc libs want at least 1; 0 happens if no data
            stringLength[col] = max;
            Dimension lengthDimension =
                NcHelper.addDimension(rootGroup, tColName + NcHelper.StringLengthSuffix, max);
            colVars[col] =
                NcHelper.addVariable(
                    rootGroup,
                    tColName,
                    DataType.CHAR,
                    Arrays.asList(tDimension, zDimension, yDimension, xDimension, lengthDimension));
          } else {
            colVars[col] =
                NcHelper.addVariable(
                    rootGroup,
                    tColName,
                    NcHelper.getNc3DataType(type),
                    Arrays.asList(tDimension, zDimension, yDimension, xDimension));
          }

          // convert to fake MissingValues
          convertToFakeMissingValues(col);
        }
      }
      // nc.addMemberVariable(recordStructure, nc.findVariable(tColName));

      // boolean bool = nc.addRecordStructure(); //creates a structure variable called "record"
      // String2.log("addRecordStructure: " + bool);
      // Structure recordStructure = (Structure)nc.findVariable("record");

      // if no id, set id=file name
      // currently, only the .nc saveAs types use attributes!
      if (globalAttributes.get("id") == null)
        globalAttributes.set("id", File2.getNameNoExtension(fullName));

      // write Attributes   (after adding variables since mv's and related attributes adjusted)
      NcHelper.setAttributes(nc3Mode, rootGroup, globalAttributes);
      for (int col = 0; col < nColumns; col++) {
        Attributes tAtts = new Attributes(columnAttributes(col)); // use a copy
        PAType paType = getColumn(col).elementType();
        if (paType == PAType.STRING) tAtts.add(File2.ENCODING, File2.ISO_8859_1);
        // disabled until there is a standard
        //                else if (getColumn(col).elementType() == PAType.CHAR)
        //                    tAtts.add(String2.CHARSET, File2.ISO_8859_1);

        NcHelper.setAttributes(nc3Mode, colVars[col], tAtts, paType.isUnsigned());
      }

      // create the stringVariable
      Variable.Builder stringVar = null;
      if (stringVariableName != null) {
        stringVariableName = String2.replaceAll(stringVariableName, " ", "_");

        Dimension lengthDimension =
            NcHelper.addDimension(
                rootGroup,
                stringVariableName + NcHelper.StringLengthSuffix,
                Math.max(1, stringVariableValue.length())); // nclib wants at least 1
        stringVar =
            NcHelper.addVariable(
                rootGroup, stringVariableName, DataType.CHAR, Arrays.asList(lengthDimension));

        // save the attributes
        Attributes tAtts = new Attributes(stringVariableAttributes); // use a copy
        tAtts.add(File2.ENCODING, File2.ISO_8859_1);

        NcHelper.setAttributes(
            nc3Mode, stringVar, tAtts, false); // unsigned=false because it is a string var
      }

      // leave "define" mode
      // if (String2.OSIsWindows) Math2.sleep(100);
      ncWriter = nc.build();

      // write the data
      for (int col = 0; col < nColumns; col++) {
        // String2.log("  writing col=" + col);
        Array ar = null;
        PrimitiveArray pa = getColumn(col);

        if (col == xColumn) {
          ar = NcHelper.get1DArray(uniqueX);
        } else if (col == yColumn) {
          ar = NcHelper.get1DArray(uniqueY);
        } else if (col == zColumn) {
          ar = NcHelper.get1DArray(uniqueZ);
        } else if (col == tColumn) {
          ar = NcHelper.get1DArray(uniqueT);
        } else {
          // other columns are 4D arrays
          if (pa instanceof StringArray sa) {
            ArrayChar.D5 tar = new ArrayChar.D5(nT, nZ, nY, nX, stringLength[col]);
            ucar.ma2.Index index = tar.getIndex();
            for (int row = 0; row < nRows; row++)
              tar.setString(
                  index.set(
                      tIndices.array[row],
                      zIndices.array[row],
                      yIndices.array[row],
                      xIndices.array[row],
                      0),
                  sa.get(row));
            ar = tar;
          } else {
            if (pa instanceof LongArray || pa instanceof ULongArray) pa = new DoubleArray(pa);
            ar =
                Array.factory(
                    NcHelper.getNc3DataType(pa.elementType()),
                    new int[] {nT, nZ, nY, nX},
                    pa.toObjectArray());
          }
        }

        // write the data
        ncWriter.write(colVars[col].getFullName(), ar);

        // undo fakeMissingValue
        if (col != xColumn && col != yColumn && col != zColumn && col != tColumn) {

          // convert back to standard MissingValues
          convertToStandardMissingValues(col);
        }
      }

      // write the stringVariable
      if (stringVariableName != null) {
        // ArrayChar.D1 ar = new ArrayChar.D1(stringVariableValue.length());
        // ar.setString(stringVariableValue);
        ncWriter.write(stringVar.getFullName(), NcHelper.get1DArray(stringVariableValue, false));
      }

      // if close throws exception, it is trouble
      ncWriter.close(); // it calls flush() and doesn't like flush called separately
      ncWriter = null;

      // rename the file to the specified name, instantly replacing the original file
      File2.rename(fullName + randomInt, fullName); // throws Exception if trouble

      // diagnostic
      if (reallyVerbose)
        String2.log(
            msg
                + " done. make4IndicesTime="
                + make4IndicesTime
                + " total TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
      // String2.log(NcHelper.ncdump(fullName, "-h"));

    } catch (Exception e) {
      String2.log(
          NcHelper.ERROR_WHILE_CREATING_NC_FILE + "\n" + msg + "\n" + MustBe.throwableToString(e));
      if (ncWriter != null) {
        try {
          ncWriter.abort();
        } catch (Exception e9) {
        }
      }

      // delete the destination file, if any
      File2.delete(fullName + randomInt);
      File2.delete(fullName);

      throw e;
    }
  }

  /**
   * This populates the table with the data from a sql resultsSet. See readSql.
   *
   * @param rs
   * @throws Exception if trouble
   */
  public void readSqlResultSet(ResultSet rs) throws Exception {

    if (reallyVerbose) String2.log("  Table.readSqlResultSet");
    long time = System.currentTimeMillis();
    clear();

    // set up columns in table
    ResultSetMetaData metadata = rs.getMetaData();
    int nCol = metadata.getColumnCount();
    PrimitiveArray paArray[] = new PrimitiveArray[nCol];
    boolean getString[] = new boolean[nCol];
    boolean getInt[] = new boolean[nCol];
    boolean getLong[] = new boolean[nCol];
    boolean getDouble[] = new boolean[nCol];
    boolean getDate[] = new boolean[nCol]; // read as String, then convert to seconds since epoch
    for (int col = 0; col < nCol; col++) {
      // note that sql counts 1...
      int colType = metadata.getColumnType(1 + col);
      paArray[col] = PrimitiveArray.sqlFactory(colType);
      PAType tPAType = paArray[col].elementType();
      if (tPAType == PAType.STRING) getString[col] = true;
      else if (colType == Types.DATE || colType == Types.TIMESTAMP) getDate[col] = true;
      else if (tPAType == PAType.DOUBLE) getDouble[col] = true;
      else if (tPAType == PAType.LONG) getLong[col] = true;
      else getInt[col] = true;

      // actually add the column
      String colName = metadata.getColumnName(1 + col); // getColumnName == getColumnLabel
      addColumn(colName, paArray[col]);
      // if (reallyVerbose) String2.log("    col=" + col + " name=" + metadata.getColumnName(1 +
      // col));
    }

    // process the rows of data
    while (rs.next()) {
      for (int col = 0; col < nCol; col++) {
        if (getString[col]) {
          String ts = rs.getString(1 + col);
          paArray[col].addString(ts == null ? "" : ts);
        } else if (getInt[col]) paArray[col].addInt(rs.getInt(1 + col));
        else if (getLong[col]) ((LongArray) paArray[col]).add(rs.getLong(1 + col));
        else if (getDouble[col]) paArray[col].addDouble(rs.getDouble(1 + col));
        // date string is always in form yyyy-mm-dd
        // timestamp string is always in form yyyy-mm-dd hh:mm:ss.fffffffff
        else if (getDate[col]) {
          // convert timestamp and date to epochSeconds
          Timestamp ts = rs.getTimestamp(1 + col);
          paArray[col].addDouble(ts == null ? Double.NaN : ts.getTime() / 1000.0);
        } else
          throw new SimpleException(
              String2.ERROR
                  + " in Table.readSqlResultSet: process unknown column("
                  + col
                  + ") type.");
      }
    }

    if (reallyVerbose)
      String2.log(
          "    Table.readSqlResultSet done. nColumns="
              + nColumns()
              + " nRows="
              + nRows()
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /**
   * This reads data from the resultsSet from an sql query using jdbc. !!!WARNING - THIS APPROACH
   * OFFERS NO PROTECTION FROM SQL INJECTION. ONLY USE THIS IF YOU, NOT SOME POSSIBLY MALICIOUS
   * USER, SPECIFIED THE QUERY.
   *
   * <p>Examples of things done to prepare to use this method:
   *
   * <ul>
   *   <li>Class.forName("org.postgresql.Driver");
   *   <li>String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest"; //database name
   *   <li>String user = "postadmin";
   *   <li>String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
   *   <li>Connection con = DriverManager.getConnection(url, user, password);
   * </ul>
   *
   * @param con a Connection (these are sometimes pooled to save time)
   * @param query e.g., "SELECT * FROM names WHERE id = 3"
   * @throws Exception if trouble
   */
  public void readSql(Connection con, String query) throws Exception {

    String msg = "  Table.readSql " + query;
    long time = System.currentTimeMillis();
    clear();

    // create the statement and execute the query
    Statement statement = con.createStatement();
    try {
      readSqlResultSet(statement.executeQuery(query));
      statement.close();
      statement = null;
      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");

    } catch (Throwable t) {
      String2.log(msg);
      if (statement != null)
        try {
          statement.close();
        } catch (Throwable t9) {
        }
      throw t;
    }
  }

  /**
   * This inserts the rows of data in this table to a sql table, using jdbc.
   *
   * <ul>
   *   <li>The column names in this table must match the column names in the database table.
   *   <li>If createTable is true, the column names won't be changed (e.g., spaces in column names
   *       will be retained.)
   *   <li>If createTable is false, the column names in this table don't have to be all of the
   *       column names in the database table, or in the same order.
   *   <li>This assumes all columns (except primary key) accept nulls or have defaults defined. If a
   *       value in this table is missing, the default value will be put in the database table.
   *   <li>The database timezone (in pgsql/data/postgresql.conf) should be set to -0 (UTC) so that
   *       dates and times are interpreted as being in UTC time zone.
   * </ul>
   *
   * <p>Examples of things done to prepare to use this method:
   *
   * <ul>
   *   <li>Class.forName("org.postgresql.Driver"); //to load the jdbc driver
   *   <li>String url = "jdbc:postgresql://otter.pfeg.noaa.gov/posttest"; //database name
   *   <li>String user = "postadmin";
   *   <li>String password = String2.getPasswordFromSystemIn("Password for '" + user + "'? ");
   *   <li>Connection con = DriverManager.getConnection(url, user, password);
   * </ul>
   *
   * @param con a Connection (these are sometimes pooled to save time)
   * @param createTable if createTable is true, a new table will be created with all columns (except
   *     primaryKeyCol) allowing nulls. If you need more flexibility when creating the table, create
   *     it separately, then use this method to insert data into it. If createTable is true and a
   *     table by the same name exists, it will be deleted. If createTable is false, it must already
   *     exist.
   * @param tableName the database's name for the table that this table's data will be inserted
   *     into, e.g., "myTable" (equivalent in postgres to "public.myTable") or "mySchema.myTable".
   * @param primaryKeyCol is the primary key column (0..., or -1 if none). This is ignored if
   *     createTable is false.
   * @param dateCols a list of columns (0..) with dates, stored as seconds since epoch in
   *     DoubleArrays.
   * @param timestampCols a list of columns (0..) with timestamps (date + time), stored as seconds
   *     since epoch in DoubleArrays. Here, timestamp precision (decimal digits for seconds value)
   *     is always 0. If createTable is true, these columns show up (in postgresql) as "timestamp
   *     without time zone".
   * @param timeCols a list of columns (0..) with times (without dates), stored as strings in
   *     StringArrays (with format "hh:mm:ss", e.g., "23:59:59" with implied time zone of UTC),
   *     which will be stored as sql TIME values. Here, time precision (decimal digits for seconds
   *     value) is always 0. Missing values can be stored as "" or null. Improperly formatted time
   *     values throw an exception. If createTable is true, these columns show up (in postgresql) as
   *     "time without time zone".
   * @param stringLengthFactor for StringArrays, this is the factor (typically 1.5) to be multiplied
   *     by the current max string length (then rounded up to a multiple of 10) to estimate the
   *     varchar length.
   * @throws Exception if trouble. If exception thrown, table may or may not have been created, but
   *     no data rows have been inserted. If no exception thrown, table was created (if requested)
   *     and all data was inserted.
   */
  public void saveAsSql(
      Connection con,
      boolean createTable,
      String tableName,
      int primaryKeyCol,
      int dateCols[],
      int timestampCols[],
      int timeCols[],
      double stringLengthFactor)
      throws Exception {

    //    * @param timeZoneOffset this identifies the time zone associated with the
    //    *    time columns.  (The Date and Timestamp columns are already UTC.)

    String msg = "  Table.saveAsSql " + tableName;
    String errorInMethod = String2.ERROR + " in" + msg + ":\n";
    long elapsedTime = System.currentTimeMillis();
    if (dateCols == null) dateCols = new int[0];
    if (timestampCols == null) timestampCols = new int[0];
    if (timeCols == null) timeCols = new int[0];
    int nCols = nColumns();
    int nRows = nRows();

    // make a local 'table' for faster access
    PrimitiveArray paArray[] = new PrimitiveArray[nCols];
    String sqlType[] = new String[nCols];
    for (int col = 0; col < nCols; col++) {
      paArray[col] = getColumn(col);
      sqlType[col] = paArray[col].getSqlTypeString(stringLengthFactor);
    }

    // swap in the dateCols
    boolean isDateCol[] = new boolean[nCols];
    for (int col = 0; col < dateCols.length; col++) {
      int tCol = dateCols[col];
      isDateCol[tCol] = true;
      sqlType[tCol] = "date";
    }

    // swap in the timestampCols
    boolean isTimestampCol[] = new boolean[nCols];
    for (int col = 0; col < timestampCols.length; col++) {
      int tCol = timestampCols[col];
      isTimestampCol[tCol] = true;
      sqlType[tCol] = "timestamp";
    }

    // identify timeCols
    boolean isTimeCol[] = new boolean[nCols];
    for (int col = 0; col < timeCols.length; col++) {
      int tCol = timeCols[col];
      isTimeCol[tCol] = true;
      sqlType[tCol] = "time";
    }

    // *** create the table   (in postgres, default for columns is: allow null)
    if (createTable) {
      // delete the table (if it exists)
      dropSqlTable(con, tableName, true);

      Statement statement = con.createStatement();
      try {
        StringBuilder create =
            new StringBuilder(
                "CREATE TABLE "
                    + tableName
                    + " ( \""
                    + getColumnName(0)
                    + "\" "
                    + sqlType[0]
                    + (primaryKeyCol == 0 ? " PRIMARY KEY" : ""));
        for (int col = 1; col < nCols; col++)
          create.append(
              ", \""
                  + getColumnName(col)
                  + "\" "
                  + sqlType[col]
                  + (primaryKeyCol == col ? " PRIMARY KEY" : ""));
        create.append(" )");
        if (reallyVerbose) msg += "\n  create=" + create;
        statement.executeUpdate(create.toString());
      } finally {
        statement.close();
      }
    }

    // *** insert the rows of data into the table
    // There may be no improved efficiency from statement.executeBatch
    // as postgres may still be doing commands one at a time
    // (http://archives.free.net.ph/message/20070115.122431.93092975.en.html#pgsql-jdbc)
    // and it is more memory efficient to just do one at a time.
    // BUT batch is more efficient on other databases and
    // most important, it allows us to rollback.
    // See batch info:
    // http://www.jguru.com/faq/view.jsp?EID=5079
    // and  http://www.onjava.com/pub/a/onjava/excerpt/javaentnut_2/index3.html?page=2 (no error
    // checking/rollback).

    // timezones
    // jdbc setTime setDate setTimestamp normally works with local time only.
    // To specify UTC timezone, you need to call setDate, setTime, setTimestamp
    //   with Calendar object which has the time zone used to interpret the date/time.
    // see http://www.idssoftware.com/faq-j.html  see J15
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    // make the prepared statement
    StringBuilder prep = new StringBuilder();
    prep.append("INSERT INTO " + tableName + " ( \"" + getColumnName(0) + "\"");
    for (int col = 1; col < nCols; col++) prep.append(", \"" + getColumnName(col) + "\"");
    prep.append(") VALUES (?");
    for (int col = 1; col < nCols; col++) prep.append(", ?");
    prep.append(")");
    if (reallyVerbose) msg += "\n  preparedStatement=" + prep;
    PreparedStatement pStatement = con.prepareStatement(prep.toString());

    // add each row's data to the prepared statement
    for (int row = 0; row < nRows; row++) {
      // clear parameters, to ensure previous row's data is cleared and new values are set
      pStatement.clearParameters();

      // add this row's values
      for (int col = 0; col < nCols; col++) {
        PrimitiveArray pa = paArray[col];
        PAType et = pa.elementType();
        // col+1 because sql counts columns as 1...
        // check for date, timestamp, time columns before double and String
        if (isDateCol[col]) {
          double d = pa.getDouble(row);
          if (Double.isFinite(d)) {
            Date date = new Date(Math.round(d * 1000)); // set via UTC millis
            pStatement.setDate(col + 1, date, cal); // cal specifies the UTC timezone
          } else pStatement.setDate(col + 1, null);
        } else if (isTimestampCol[col]) {
          double d = pa.getDouble(row);
          if (Double.isFinite(d)) {
            Timestamp timestamp = new Timestamp(Math.round(d * 1000)); // set via UTC millis
            pStatement.setTimestamp(col + 1, timestamp, cal); // cal specifies the UTC timezone
          } else pStatement.setTimestamp(col + 1, null);
        } else if (isTimeCol[col]) {
          // data already a time string
          String s = pa.getString(row);
          if (s == null || s.length() == 0) {
            pStatement.setTime(col + 1, null);
          } else if ( // ensure that format is HH:MM:SS
          s.length() == 8
              && String2.isDigit(s.charAt(0))
              && String2.isDigit(s.charAt(1))
              && s.charAt(2) == ':'
              && String2.isDigit(s.charAt(3))
              && String2.isDigit(s.charAt(4))
              && s.charAt(5) == ':'
              && String2.isDigit(s.charAt(6))
              && String2.isDigit(s.charAt(7))) {
            // Time documentation says Time is java Date object with date set to 1970-01-01
            try {
              double d =
                  Calendar2.isoStringToEpochSeconds(
                      "1970-01-01T" + s); // throws exception if trouble
              // String2.log("date=" + s + " -> " + Calendar2.epochSecondsToIsoStringTZ(d));
              Time time = new Time(Math.round(d * 1000));
              pStatement.setTime(col + 1, time, cal); // cal specifies the UTC timezone
            } catch (Exception e) {
              pStatement.setTime(col + 1, null);
            }
          } else {
            throw new SimpleException(
                errorInMethod
                    + "Time format must be HH:MM:SS. Bad value="
                    + s
                    + " in row="
                    + row
                    + " col="
                    + col);
          }
          // for integer types, there seems to be no true null, so keep my missing value, e.g.,
          // Byte.MAX_VALUE
        } else if (et == PAType.BYTE) {
          pStatement.setByte(col + 1, ((ByteArray) pa).get(row));
        } else if (et == PAType.SHORT) {
          pStatement.setShort(col + 1, ((ShortArray) pa).get(row));
        } else if (et == PAType.INT) {
          pStatement.setInt(col + 1, pa.getInt(row));
        } else if (et == PAType.LONG) {
          pStatement.setLong(col + 1, pa.getLong(row));
          // for double and float, NaN is fine
        } else if (et == PAType.FLOAT) {
          pStatement.setFloat(col + 1, pa.getFloat(row));
        } else if (et == PAType.DOUBLE) {
          pStatement.setDouble(col + 1, pa.getDouble(row));
        } else if (et == PAType.STRING || et == PAType.CHAR) {
          pStatement.setString(col + 1, pa.getString(row)); // null is ok
        } else
          throw new SimpleException(
              errorInMethod + "Process column(" + col + ") unknown type=" + pa.elementTypeString());
      }

      // add this row's data to batch
      pStatement.addBatch();
    }

    // try to executeBatch
    // setAutoCommit(false) seems to perform an implicit postgresql
    // BEGIN command to start a transaction.
    // (Do this after all preparation in case exception thrown there.)
    con.setAutoCommit(false); // must be false for rollback to work
    int[] updateCounts = null;
    Exception caughtException = null;
    try {
      // NOTE that I don't try/catch the stuff above.
      //  If exception in preparation, no need to roll back
      //  (and don't want to roll back previous statement).
      //  But exception in executeBatch needs to be rolled back.

      // process the batch
      updateCounts = pStatement.executeBatch();

      // got here without exception? save the changes
      con.commit();

    } catch (Exception e) {
      // get the caught exception
      caughtException = e;
      try {
        if (e instanceof BatchUpdateException t) {
          // If e was BatchUpdateException there is additional information.
          // Try to combine the e exception (identifies bad row's data)
          // and bue.getNextException (says what the problem was).
          Exception bue2 = t.getNextException();
          caughtException =
              new Exception(
                  errorInMethod
                      + "[BU ERROR] "
                      + MustBe.throwableToString(e)
                      + "[BU ERROR2] "
                      + bue2.toString());
        } else {
        }
      } catch (Exception e2) {
        // oh well, e is best I can get
      }

      // since there was a failure, try to rollback the whole transaction.
      try {
        con.rollback();
      } catch (Exception rbe) {
        // hopefully won't happen
        caughtException =
            new Exception(
                errorInMethod
                    + "[C ERROR] "
                    + MustBe.throwableToString(caughtException)
                    + "[RB ERROR] "
                    + rbe.toString());
        msg += "\nsmall ERROR during rollback:\n" + MustBe.throwableToString(caughtException);
      }
    }

    // other clean up
    try {
      // pStatement.close(); //not necessary?
      // go back to autoCommit; this signals end of transaction
      con.setAutoCommit(true);
    } catch (Exception e) {
      // small potatoes
      msg += "\nsmall ERROR during cleanup:\n" + MustBe.throwableToString(e);
    }

    // rethrow the big exception, so caller knows there was trouble
    if (caughtException != null) throw caughtException;

    // all is well, print diagnostics
    if (reallyVerbose) {
      IntArray failedRows = new IntArray();
      for (int i = 0; i < updateCounts.length; i++) {
        if (updateCounts[i] != 1) {
          failedRows.add(i);
        }
      }
      if (failedRows.size() > 0) msg += "\n  failedRows(0..)=" + failedRows.toString();
      String2.log(
          msg
              + "\n  done. nColumns="
              + nColumns()
              + " nRowsSucceed="
              + (nRows - failedRows.size())
              + " nRowsFailed="
              + failedRows.size()
              + " TIME="
              + (System.currentTimeMillis() - elapsedTime)
              + "ms");
    }
  }

  /**
   * Get a connection to an Access .mdb file. MS Access not needed.
   *
   * @param fileName (forward slash in example)
   * @param user use "" if none specified
   * @param password use "" if none specified
   */
  public static Connection getConnectionToMdb(String fileName, String user, String password)
      throws Exception {

    // from Sareth's answer at
    // https://stackoverflow.com/questions/9543722/java-create-msaccess-database-file-mdb-0r-accdb-using-java
    Class.forName("sun.jdbc.odbc.JdbcOdbcDriver"); // included in Java distribution
    return DriverManager.getConnection(
        "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb, *.accdb)};"
            + "DBQ="
            + fileName, // ";DriverID=22;READONLY=true}",
        "",
        ""); // user, password
  }

  /**
   * This returns a list of schemas (subdirectories of this database) e.g., "public"
   *
   * @return a list of schemas (subdirectories of this database) e.g., "public". Postgres always
   *     returns all lowercase names.
   * @throws Exception if trouble
   */
  public static StringArray getSqlSchemas(Connection con) throws Exception {

    DatabaseMetaData dm = con.getMetaData();
    Table schemas = new Table();
    schemas.readSqlResultSet(dm.getSchemas());
    return (StringArray) schemas.getColumn(0);
  }

  /**
   * This returns a list of tables of a certain type or types.
   *
   * @param con
   * @param schema a specific schema (a subdirectory of the database) e.g., "public" (not null)
   * @param types null (for any) or String[] of one or more of "TABLE", "VIEW", "SYSTEM TABLE",
   *     "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
   * @return a StringArray of matching table names. Postgres always returns all lowercase names.
   * @throws Exception if trouble
   */
  public static StringArray getSqlTableNames(Connection con, String schema, String types[])
      throws Exception {

    if (schema == null)
      throw new SimpleException(String2.ERROR + " in Table.getSqlTableList: schema is null.");

    // getTables(catalogPattern, schemaPattern, tableNamePattern, String[] types)
    // "%" means match any substring of 0 or more characters, and
    // "_" means match any one character.
    // If a search pattern argument is set to null, that argument's criterion will be dropped from
    // the search.
    DatabaseMetaData dm = con.getMetaData();
    Table tables = new Table(); // works with "posttest", "public", "names", null
    tables.readSqlResultSet(dm.getTables(null, schema.toLowerCase(), null, types));
    return (StringArray) tables.getColumn(2); // table name is always col (0..) 2
  }

  /**
   * Determines the type of a table (or if the table exists).
   *
   * @param schema a specific schema (a subdirectory of the database) e.g., "public" (not null)
   * @param tableName a specific tableName (can't be null)
   * @return the table type: "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY",
   *     "ALIAS", "SYNONYM", or null if the table doesn't exist.
   * @throws Exception if trouble
   */
  public static String getSqlTableType(Connection con, String schema, String tableName)
      throws Exception {

    if (schema == null)
      throw new SimpleException(String2.ERROR + " in Table.getSqlTableType: schema is null.");
    if (tableName == null)
      throw new SimpleException(String2.ERROR + " in Table.getSqlTableType: tableName is null.");

    // getTables(catalogPattern, schemaPattern, tableNamePattern, String[] types)
    // "%" means match any substring of 0 or more characters, and
    // "_" means match any one character.
    // If a search pattern argument is set to null, that argument's criterion will be dropped from
    // the search.
    DatabaseMetaData dm = con.getMetaData();
    Table tables = new Table(); // works with "posttest", "public", "names", null
    tables.readSqlResultSet(
        dm.getTables(null, schema.toLowerCase(), tableName.toLowerCase(), null));
    if (tables.nRows() == 0) return null;
    return tables.getStringData(3, 0); // table type is always col (0..) 3
  }

  /**
   * Drops (deletes) an sql table (if it exists).
   *
   * @param tableName a specific table name (not null), e.g., "myTable" (equivalent in postgres to
   *     "public.myTable") or "myShema.myTable".
   * @param cascade This is only relevant if tableName is referenced by a view. If so, and cascade
   *     == true, the table will be deleted. If so, and cascade == false, the table will not be
   *     deleted.
   * @throws Exception if trouble (e.g., the table existed but couldn't be deleted, perhaps because
   *     connection's user doesn't have permission)
   */
  public static void dropSqlTable(Connection con, String tableName, boolean cascade)
      throws Exception {

    // create the statement and execute the query
    // DROP TABLE [ IF EXISTS ] name [, ...] [ CASCADE | RESTRICT ]
    Statement statement = con.createStatement();
    try {
      statement.executeUpdate(
          "DROP TABLE IF EXISTS "
              + tableName
              + // case doesn't matter here
              (cascade ? " CASCADE" : ""));
    } finally {
      statement.close();
    }
  }

  /**
   * THIS IS NOT YET FINISHED. This converts the specified String column with date (with e.g.,
   * "2006-01-02"), time (with e.g., "23:59:59"), or timestamp values (with e.g., "2006-01-02
   * 23:59:59" with any character between the date and time) into a double column with
   * secondSinceEpoch (1970-01-01 00:00:00 UTC time zone). No metadata is changed by this method.
   *
   * @param col the number of the column (0..) with the date, time, or timestamp strings.
   * @param type indicates the type of data in the column: 0=date, 1=time, 2=timestamp.
   * @param timeZoneOffset this identifies the time zone associated with col (e.g., 0 if already
   *     UTC, -7 for California in summer (DST), and -8 for California in winter, so that the data
   *     can be converted to UTC timezone.
   * @param strict If true, this throws an exception if a value is improperly formatted. (Missing
   *     values of "" or null are allowed.) If false, improperly formatted values are silently
   *     converted to missing values (Double.NaN). Regardless of 'strict', the method rolls
   *     components as needed, for example, Jan 32 becomes Feb 1.
   * @return the number of valid values.
   * @throws Exception if trouble (and no changes will have been made)
   */
  public int isoStringToEpochSeconds(int col, int type, int timeZoneOffset, boolean strict)
      throws Exception {

    String errorInMethod = String2.ERROR + " in Table.isoStringToEpochSeconds(col=" + col + "):\n";
    Test.ensureTrue(
        type >= 0 && type <= 2, errorInMethod + "type=" + type + " must be between 0 and 2.");
    String isoDatePattern = "[1-2][0-9]{3}\\-[0-1][0-9]\\-[0-3][0-9]";
    String isoTimePattern = "[0-2][0-9]\\:[0-5][0-9]\\:[0-5][0-9]";
    String stringPattern =
        type == 0
            ? isoDatePattern
            : type == 1 ? isoTimePattern : isoDatePattern + "." + isoTimePattern;
    Pattern pattern = Pattern.compile(stringPattern);
    StringArray sa = (StringArray) getColumn(col);
    int n = sa.size();
    DoubleArray da = new DoubleArray(n, true);
    int nGood = 0;
    int adjust = timeZoneOffset * Calendar2.SECONDS_PER_HOUR;
    for (int row = 0; row < n; row++) {
      String s = sa.get(row);

      // catch allowed missing values
      if (s == null || s.length() == 0) {
        da.array[row] = Double.NaN;
        continue;
      }

      // catch improperly formatted values (stricter than Calendar2.isoStringToEpochSeconds below)
      if (strict && !pattern.matcher(s).matches())
        throw new SimpleException(
            errorInMethod + "value=" + s + " on row=" + row + " is improperly formatted.");

      // parse the string
      if (type == 1) s = "1970-01-01 " + s;
      double d = Calendar2.isoStringToEpochSeconds(s); // throws exception
      if (!Double.isNaN(d)) {
        nGood++;
        d -= adjust;
      }
      da.array[row] = d;
    }
    setColumn(col, da);
    return nGood;
  }

  /**
   * This is like the other saveAsTabbedASCII that writes to a file, but this uses the ISO-8859-1
   * charset.
   *
   * @param fullFileName the complete file name (including directory and extension, usually ".asc").
   *     An existing file with this name will be overwritten.
   * @throws Exception
   */
  public void saveAsTabbedASCII(String fullFileName) throws Exception {
    saveAsTabbedASCII(fullFileName, File2.ISO_8859_1);
  }

  /**
   * This is like the other saveAsTabbedASCII, but writes to a file. The second line has units.
   *
   * @param fullFileName the complete file name (including directory and extension, usually ".asc").
   *     An existing file with this name will be overwritten.
   * @param charset e.g., ISO-8859-1 (default, used if charset is null or "") or UTF-8.
   * @throws Exception
   */
  public void saveAsTabbedASCII(String fullFileName, String charset) throws Exception {
    if (reallyVerbose) String2.log("Table.saveAsTabbedASCII " + fullFileName);
    long time = System.currentTimeMillis();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the file (before 'try'); if it fails, no temp file to delete
    OutputStream os = new BufferedOutputStream(new FileOutputStream(fullFileName + randomInt));

    try {
      saveAsTabbedASCII(os, charset);
      os.close();
      os = null;

      // rename the file to the specified name, instantly replacing the original file
      File2.rename(fullFileName + randomInt, fullFileName); // throws Exception if trouble

    } catch (Exception e) {
      if (os != null) {
        try {
          os.close();
        } catch (Exception e2) {
        }
      }

      File2.delete(fullFileName + randomInt);
      // delete any existing file
      File2.delete(fullFileName);

      throw e;
    }
  }

  /**
   * Save this data as a tab-separated ASCII outputStream. The second line has units. This writes
   * the lon values as they are currently in this table (e.g., +-180 or 0..360). If no exception is
   * thrown, the data was successfully written. NaN's are written as "NaN".
   *
   * @param outputStream There is no need for it to be buffered. Afterwards, it is flushed, not
   *     closed.
   * @throws Exception
   */
  public void saveAsTabbedASCII(OutputStream outputStream) throws Exception {
    saveAsSeparatedAscii(outputStream, null, "\t");
  }

  public void saveAsTabbedASCII(OutputStream outputStream, String charset) throws Exception {
    saveAsSeparatedAscii(outputStream, charset, "\t");
  }

  /**
   * This is like the other saveAsCsvASCII, but writes to a file.
   *
   * @param fullFileName the complete file name (including directory and extension, usually ".csv").
   *     An existing file with this name will be overwritten.
   * @throws Exception
   */
  public void saveAsCsvASCII(String fullFileName) throws Exception {
    if (reallyVerbose) String2.log("Table.saveAsCsvASCII " + fullFileName);
    long time = System.currentTimeMillis();

    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    // open the file (before 'try'); if it fails, no temp file to delete
    OutputStream os = new BufferedOutputStream(new FileOutputStream(fullFileName + randomInt));

    try {
      saveAsCsvASCII(os);
      os.close();
      os = null;

      // rename the file to the specified name, instantly replacing the original file
      File2.rename(fullFileName + randomInt, fullFileName); // throws Exception if trouble

    } catch (Exception e) {
      try {
        if (os != null) os.close();
      } catch (Exception e2) {
      }

      File2.delete(fullFileName + randomInt);
      // delete any existing file
      File2.delete(fullFileName);

      throw e;
    }
  }

  /**
   * This returns the table as a CSV String. The second line has units. This is generally used for
   * diagnostics, as the String will be large if the table is large.
   */
  public String saveAsCsvASCIIString() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    saveAsCsvASCII(baos);
    return baos.toString();
  }

  /**
   * Save this data as a comma-separated ASCII file. The second line has units. This writes the lon
   * values as they are currently in this table (e.g., +-180 or 0..360). If no exception is thrown,
   * the data was successfully written. NaN's are written as "NaN".
   *
   * @param outputStream There is no need for it to be buffered. Afterwards, it is flushed, not
   *     closed.
   * @throws Exception
   */
  public void saveAsCsvASCII(OutputStream outputStream) throws Exception {
    saveAsSeparatedAscii(outputStream, null, ",");
  }

  public void saveAsCsvASCII(OutputStream outputStream, String charset) throws Exception {
    saveAsSeparatedAscii(outputStream, charset, ",");
  }

  /**
   * Save this data as a separated value ASCII outputStream. The second line has units. This writes
   * the lon values as they are currently in this table (e.g., +-180 or 0..360). If no exception is
   * thrown, the data was successfully written. NaN's are written as "NaN".
   *
   * @param outputStream There is no need for it to be buffered. Afterwards, it is flushed, not
   *     closed.
   * @param charset e.g., ISO-8859-1 (default, used if charset is null or "") or UTF-8.
   * @param separator usually a tab or a comma
   * @throws Exception
   */
  public void saveAsSeparatedAscii(OutputStream outputStream, String charset, String separator)
      throws Exception {

    // ensure there is data
    if (nRows() == 0) {
      throw new Exception(
          String2.ERROR
              + " in Table.saveAsSeparatedAscii:\n"
              + MustBe.THERE_IS_NO_DATA
              + " (nRows = 0)");
    }

    long time = System.currentTimeMillis();
    if (charset == null || charset.length() == 0) charset = File2.ISO_8859_1;
    BufferedWriter writer = File2.getBufferedWriter(outputStream, charset);

    // write the column names
    boolean csvMode = separator.equals(",");
    int nColumns = nColumns();

    if (columnNames != null && columnNames.size() == nColumns) { // isn't this always true???
      for (int col = 0; col < nColumns; col++) {
        String s = columnNames.getSVString(col);
        if (csvMode) s = String2.replaceAll(s, "\\\"", "\"\"");
        writer.write(s + (col == nColumns - 1 ? "\n" : separator));
      }
    }

    // write the units
    for (int col = 0; col < nColumns; col++) {
      String s = columnAttributes(col).getString("units");
      s = String2.toSVString(s, 127);
      if (csvMode) s = String2.replaceAll(s, "\\\"", "\"\"");
      writer.write(s + (col == nColumns - 1 ? "\n" : separator));
    }

    // get columnTypes
    boolean isCharArray[] = new boolean[nColumns];
    for (int col = 0; col < nColumns; col++) {
      isCharArray[col] = getColumn(col).elementType() == PAType.STRING;
    }

    // write the data
    int nRows = nRows();
    for (int row = 0; row < nRows; row++) {
      for (int col = 0; col < nColumns; col++) {
        String s = getColumn(col).getSVString(row);
        if (csvMode) s = String2.replaceAll(s, "\\\"", "\"\"");
        writer.write(s + (col == nColumns - 1 ? "\n" : separator));
      }
    }

    writer.flush(); // essential

    // diagnostic
    if (reallyVerbose)
      String2.log(
          "  Table.saveAsSeparatedASCII done. TIME=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * Save this data in this table as a json file. <br>
   * Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
   *
   * @param fileName the full file name
   * @param timeColumn the column number of the column that has seconds since 1970-01-01 double data
   *     that should be saved as an ISO string (or -1 if none) An existing file with this name will
   *     be overwritten.
   * @param writeUnits if true, columnUnits will be written with the "units" attribute for each
   *     column Note that timeColumn will have units "UTC".
   * @throws Exception (no error if there is no data)
   */
  public void saveAsJson(String fileName, int timeColumn, boolean writeUnits) throws Exception {
    OutputStream fos = new BufferedOutputStream(new FileOutputStream(fileName));
    try {
      saveAsJson(fos, timeColumn, writeUnits);
    } finally {
      fos.close();
    }
  }

  /**
   * Save this file as a json string. <br>
   * This is usually just used for diagnostics, since the string might be very large. <br>
   * Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
   *
   * @param timeColumn
   * @param writeUnits
   */
  public String saveAsJsonString(int timeColumn, boolean writeUnits) throws Exception {
    StringWriter sw = new StringWriter();
    saveAsJson(sw, timeColumn, writeUnits);
    return sw.toString();
  }

  /**
   * Save this data in this table as a json outputStream. <br>
   * There is no standard way to do this (that I am aware of), so I just made one up. <br>
   * This writes the lon values as they are currently in this table (e.g., +-180 or 0..360). <br>
   * If no exception is thrown, the data was successfully written. <br>
   * Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
   * <br>
   * NaN's are written as "null" (to match the json library standard).
   *
   * @param outputStream There is no need for it to be buffered. A UTF-8 OutputStreamWriter is
   *     generated from it temporarily. Afterwards, it is flushed, not closed.
   * @param timeColumn the column number of the column that has seconds since 1970-01-01 double data
   *     that should be saved as an ISO string (or -1 if none)
   * @param writeUnits if true, columnUnits will be written with the "units" attribute for each
   *     column Note that timeColumn will have units "UTC".
   * @throws Exception (no error if there is no data)
   */
  public void saveAsJson(OutputStream outputStream, int timeColumn, boolean writeUnits)
      throws Exception {

    BufferedWriter writer = File2.getBufferedWriterUtf8(outputStream);
    saveAsJson(writer, timeColumn, writeUnits);
  }

  /**
   * Save this table as a json writer. <br>
   * The writer is flushed, but not closed at the end. <br>
   * nRows and nColumns may be 0. <br>
   * Missing values should already be stored as NaNs (perhaps via convertToStandardMissingValues()).
   * <br>
   * NaN's are written as "null" (to match the json library standard).
   */
  public void saveAsJson(Writer writer, int timeColumn, boolean writeUnits) throws Exception {

    long time = System.currentTimeMillis();

    // write the column names
    int nColumns = nColumns();
    int nRows = nRows();
    boolean isCharOrString[] = new boolean[nColumns];
    writer.write(
        "{\n"
            + "  \"table\": {\n"
            + // begin main structure
            "    \"columnNames\": [");
    for (int col = 0; col < nColumns; col++) {
      isCharOrString[col] =
          getColumn(col).elementType() == PAType.CHAR
              || getColumn(col).elementType() == PAType.STRING;
      writer.write(String2.toJson(getColumnName(col)));
      writer.write(col == nColumns - 1 ? "],\n" : ", ");
    }

    // write the types
    writer.write("    \"columnTypes\": [");
    for (int col = 0; col < nColumns; col++) {
      String s = getColumn(col).elementTypeString();
      if (col == timeColumn) s = "String"; // not "double"
      writer.write(String2.toJson(s)); // nulls written as: null
      writer.write(col == nColumns - 1 ? "],\n" : ", ");
    }

    // write the units
    if (writeUnits) {
      writer.write("    \"columnUnits\": [");
      for (int col = 0; col < nColumns; col++) {
        String s = columnAttributes(col).getString("units");
        if (col == timeColumn) s = "UTC"; // not "seconds since 1970-01-01..."
        writer.write(String2.toJson(s)); // nulls written as: null
        writer.write(col == nColumns - 1 ? "],\n" : ", ");
      }
    }

    // write the data
    writer.write("    \"rows\": [\n");
    for (int row = 0; row < nRows; row++) {
      writer.write("      ["); // beginRow
      for (int col = 0; col < nColumns; col++) {
        if (col == timeColumn) {
          double d = getDoubleData(col, row);
          String s =
              Double.isNaN(d) ? "null" : "\"" + Calendar2.epochSecondsToIsoStringTZ(d) + "\"";
          writer.write(s);
        } else if (isCharOrString[col]) {
          String s = getStringData(col, row);
          writer.write(String2.toJson(s));
        } else {
          String s = getStringData(col, row);
          // represent NaN as null? yes, that is what json library does
          writer.write(s.length() == 0 ? "null" : s);
        }
        if (col < nColumns - 1) writer.write(", ");
      }
      writer.write(row < nRows - 1 ? "],\n" : "]"); // endRow
    }

    // end of big array
    writer.write(
        "\n" + "    ]\n" + // end of rows array
            "  }\n" + // end of table
            "}\n"); // end of main structure
    writer.flush();

    if (reallyVerbose)
      String2.log("Table.saveAsJson done. time=" + (System.currentTimeMillis() - time) + "ms");
  }

  /**
   * This reads data from json table (of the type written by saveAsJson).
   *
   * <ul>
   *   <li>If no exception is thrown, the file was successfully read.
   *   <li>If there is a String column with units="UTC", the ISO 8601 values in the column are
   *       converted to doubles (seconds since 1970-01-01).
   * </ul>
   *
   * @param fileName the full file name. The file is assumed to be a UTF-8 file. If the file is
   *     compressed (e.g., .zip), it will be decompresssed on-the-fly.
   * @throws Exception if trouble
   */
  public void readJson(String fileName) throws Exception {
    readJson(fileName, File2.getDecompressedBufferedFileReaderUtf8(fileName));
  }

  /**
   * This reads data from json table (of the type written by saveAsJson). This is a little stricter
   * about the format that a JSON file has to be (e.g.,
   * columnNames/columnTypes/columnUnits/rowOfData, if present, must be on one line).
   *
   * <ul>
   *   <li>If no exception is thrown, the file was successfully read.
   *   <li>If there is a String column with units="UTC", the ISO 8601 values in the column are
   *       converted to doubles (seconds since 1970-01-01).
   * </ul>
   *
   * @param fileName for diagnostic messages only
   * @param bufferedReader the json info as a bufferedReader
   * @throws Exception if trouble
   */
  public void readJson(String fileName, BufferedReader bufferedReader) throws Exception {
    if (reallyVerbose) String2.log("Table.readJson " + fileName);
    long time = System.currentTimeMillis();
    final String note = "In Table.readJson(" + fileName + "): ";
    final String errorInMethod =
        String2.ERROR + " in Table.readJson(" + fileName + "): JSON syntax error: ";

    // clear everything
    clear();
    ArrayList<String> cNames = null;
    ArrayList<String> cTypes = null;
    ArrayList<String> cUnits = null;
    PrimitiveArray pas[] = null;
    boolean endFound = false;

    // {
    //  "table": {
    //    "columnNames": ["longitude", "latitude", "time", "sea_surface_temperature"],
    //    "columnTypes": ["float", "float", "String", "float"],
    //    "columnUnits": ["degrees_east", "degrees_north", "UTC", "degree_C"],
    //    "rows": [
    //      [180.099, 0.032, "2007-10-04T12:00:00Z", 27.66],
    //      [180.099, 0.032, null, null],
    //      [189.971, -7.98, "2007-10-04T12:00:00Z", 29.08]
    //    ]
    //  }
    // }
    try {
      String line = bufferedReader.readLine();
      if (line == null || !line.trim().equals("{"))
        throw new IOException(
            errorInMethod + "First line of file should have been '{'.\nline=" + line);
      line = bufferedReader.readLine();
      if (!line.trim().equals("\"table\": {"))
        throw new IOException(
            errorInMethod + "Second line of file should have been '\"table\": {'.\nline=" + line);
      while ((line = bufferedReader.readLine()) != null) {
        line = line.trim();
        if (debugMode) String2.log("line=" + line);

        if (line.startsWith("\"columnNames\": [")) {
          if (!line.endsWith("],"))
            throw new IOException(
                errorInMethod + "columnNames line should have ended with '],'.\nline=" + line);
          cNames = new ArrayList();
          StringArray.arrayListFromCSV(
              line.substring(16, line.length() - 2), ",", true, true, cNames); // trim, keepNothing

        } else if (line.startsWith("\"columnTypes\": [")) {
          if (!line.endsWith("],"))
            throw new IOException(
                errorInMethod + "columnTypes line should have ended with '],'.\nline=" + line);
          cTypes = new ArrayList();
          StringArray.arrayListFromCSV(
              line.substring(16, line.length() - 2), ",", true, true, cTypes); // trim, keepNothing

        } else if (line.startsWith("\"columnUnits\": [")) {
          if (!line.endsWith("],"))
            throw new IOException(
                errorInMethod + "columnUnits line should have ended with '],'.\nline=" + line);
          cUnits = new ArrayList();
          StringArray.arrayListFromCSV(
              line.substring(16, line.length() - 2), ",", true, true, cUnits); // trim, keepNothing
          for (int i = 0; i < cUnits.size(); i++)
            if ("null".equals(cUnits.get(i))) cUnits.set(i, null);

        } else if (line.equals("\"rows\": [")) {
          // build the table
          // cNames is required
          if (cNames == null || cNames.size() == 0)
            throw new IOException(errorInMethod + "columnNames line not found.");
          int nCol = cNames.size();
          // cTypes and cUnits are optional
          if (cTypes != null && cTypes.size() != nCol)
            throw new IOException(
                errorInMethod + "columnTypes size=" + cTypes.size() + " should be " + nCol + ".");
          if (cUnits != null && cUnits.size() != nCol)
            throw new IOException(
                errorInMethod + "columnUnits size=" + cUnits.size() + " should be " + nCol + ".");
          pas = new PrimitiveArray[nCol];
          // need isString since null in numeric col is NaN, but null in String col is the word
          // null.
          boolean isString[] =
              new boolean[nCol]; // all false  (includes UTC times -- initially Strings)
          boolean isUTC[] = new boolean[nCol]; // all false
          ArrayList<String> sal = new ArrayList(nCol);
          for (int col = 0; col < nCol; col++) {
            PAType elementPAType =
                cTypes == null ? PAType.STRING : PAType.fromCohortString(cTypes.get(col));
            isString[col] = elementPAType == PAType.STRING;
            Attributes atts = new Attributes();
            if (cUnits != null) {
              isUTC[col] = isString[col] && "UTC".equals(cUnits.get(col));
              if (isUTC[col]) {
                elementPAType = PAType.DOUBLE;
                cUnits.set(col, Calendar2.SECONDS_SINCE_1970);
              }
              atts.add("units", cUnits.get(col));
            }
            pas[col] = PrimitiveArray.factory(elementPAType, 128, false);
            addColumn(col, cNames.get(col), pas[col], atts);
          }

          // read the rows of data
          // I can use StringArray.arrayFromCSV to process a row of data
          //  even though it doesn't distinguish a null String from the word "null" in a String
          // column
          //  because ERDDAP never has null Strings (closest thing is "").
          //  If this becomes a problem, switch to grabbing the nCol items separately
          //     utilizing the specifics of the json syntax.
          while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (line.equals("]")) // no more data
            break;

            if (!line.startsWith("["))
              throw new IOException(
                  errorInMethod
                      + "Data lines must start with '['.\ndata line #"
                      + pas[0].size()
                      + "="
                      + line);

            if (line.endsWith("],")) {
              line = line.substring(1, line.length() - 2);
            } else if (line.endsWith("]")) { // last data line
              line = line.substring(1, line.length() - 1);
            } else {
              throw new IOException(
                  errorInMethod
                      + "Data lines must end with '],'.\ndata line #"
                      + pas[0].size()
                      + "="
                      + line);
            }

            StringArray.arrayListFromCSV(
                line, ",", true, true, sal); // trim, keepNothing    This accepts json CSV strings.
            if (sal.size() != nCol)
              throw new IOException(
                  errorInMethod
                      + "incorrect number of data values: "
                      + sal.size()
                      + "!="
                      + nCol
                      + ".\ndata line #"
                      + pas[0].size()
                      + "="
                      + line);
            for (int col = 0; col < nCol; col++) {
              String ts =
                  sal.get(col); // will be "null" for empty cells.  Note that this treats null and
              // "null" in file as empty cell.
              // String2.log(">> col=" + col + " ts=" + String2.annotatedString(ts));
              if (isUTC[col])
                pas[col].addDouble(
                    Calendar2.safeIsoStringToEpochSeconds(ts)); // returns NaN if trouble

              // For both null and "null, arrayFromCSV returns "null"!
              // String columns will treat null (shouldn't be any) and "null" as the word "null",
              //  numeric columns will treat null as NaN.  So all is well.
              else pas[col].addString(ts.equals("null") ? "" : ts);
            }
          }

        } else if (line.equals("}")) {
          // end of table. no need to look at rest of file
          endFound = true;
          break;

        } else if (line.equals("")) {
          // allowed and ignored blank line

        } else {
          String2.log(note + "Unexpected/ignored line: " + line);
          // it better all be on this line.
        }
      }
      if (pas == null)
        throw new IOException(errorInMethod + "The line with '\"rows\": [' wasn't found.");
      if (!endFound)
        throw new IOException(
            errorInMethod + "The '}' line marking the end of the table wasn't found.");

      // current ch should be final }, but don't insist on it.

      // simplify
      if (cTypes == null) simplify();

      /*
      //convert times to epoch seconds  (after simplify, so dates are still Strings)
      int tnRows = nRows();
      if (cUnits != null) {
          for (int col = 0; col < nCol; col++) {
              String ttUnits = cUnits[col];
              if ((pas[col] instanceof StringArray) &&
                  ttUnits != null && ttUnits.equals("UTC")) {
                  sa = (StringArray)pas[col];
                  DoubleArray da = new DoubleArray(tnRows, false);
                  for (int row = 0; row < tnRows; row++) {
                      String iso = sa.get(row);
                      da.add((iso == null || iso.length() == 0)?
                          Double.NaN :
                          Calendar2.isoStringToEpochSeconds(iso));
                  }
                  setColumn(col, da);
                  columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
              }
          }
      }*/

      // String2.log(" place3 nColumns=" + nColumns() + " nRows=" + nRows() + " nCells=" +
      // (nColumns() * nRows()));
      // String2.log(toString(10));
      if (reallyVerbose)
        String2.log(
            "  Table.readJson done. fileName="
                + fileName
                + " nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");
    } finally {
      bufferedReader.close();
    }
  }

  /**
   * This reads a table from a jsonLinesCsv file.
   *
   * @throws Exception if serious trouble
   */
  public void readJsonlCSV(
      String fullFileName, StringArray colNames, String[] colTypes, boolean simplify)
      throws Exception {
    clear();
    BufferedReader reader = File2.getDecompressedBufferedFileReader(fullFileName, File2.UTF_8);
    try {
      readJsonlCSV(reader, fullFileName, colNames, colTypes, simplify);
    } finally {
      reader.close();
    }
  }

  /**
   * This reads a table from a Reader from a jsonLinesCsv file (or other source).
   * https://jsonlines.org/examples/ The first line must be column names. The second line must be
   * data values. This stops (perhaps with error) at the first line that doesn't start with [ and
   * end with ] and have same number of columns as the first line. This doesn't simplify the columns
   * at all. null values appear as empty strings.
   *
   * @param reader This is usually a BufferedReader. This method doesn't close the reader.
   * @param fullFileName This is just used for error messages.
   * @param colNames If null, all columns will be loaded. If not null, only these columns will be
   *     loaded (if found) and the returned table will be in this order.
   * @param colTypes If not null, this parallels colNames to specify the data types. If null, all
   *     columns will be StringArray with simply parsed values (e.g., json strings with surrounding
   *     double quotes). "boolean" says to interpret as boolean (true|false) but convert to byte
   *     (1|0).
   * @param simplify If colTypes=null and simplify=true, this tries to simplify (determine the
   *     column data types).
   * @throws Exception if serious trouble
   */
  public void readJsonlCSV(
      Reader reader, String fullFileName, StringArray colNames, String[] colTypes, boolean simplify)
      throws Exception {
    clear();
    StringBuilder sb = new StringBuilder();
    long time = System.currentTimeMillis();

    // read the column names
    // This doesn't just JsonTokener because it resolves to different data types,
    //  but I don't need that and don't want that (for one variable,
    //  one line might have an int and the next a long).
    int line = 1;
    int ch = reader.read();
    while (ch != '\n' && ch != -1) {
      sb.append((char) ch);
      ch = reader.read();
    }
    String s = sb.toString();
    StringArray sa = StringArray.simpleFromJsonArray(s);
    int nc = sa.size();
    PrimitiveArray pas[] = new PrimitiveArray[nc];
    boolean fromJsonString[] = new boolean[nc]; // all false
    boolean isBoolean[] = new boolean[nc]; // all false
    for (int c = 0; c < nc; c++) {
      String tColName = sa.get(c);
      tColName = tColName.equals("null") ? "" : String2.fromJson(tColName);
      PrimitiveArray pa = null;
      if (colNames == null) {
        pa = new StringArray();
      } else {
        int which = colNames.indexOf(tColName);
        if (which >= 0) {
          if (colTypes == null) {
            pa = new StringArray();
          } else {
            PAType tPAType = PAType.fromCohortString(colTypes[which]); // it handles boolean
            pa = PrimitiveArray.factory(tPAType, 8, false);
            fromJsonString[c] = tPAType == PAType.STRING || tPAType == PAType.CHAR;
            isBoolean[c] = "boolean".equals(colTypes[which]);
          }
        }
      }
      if (pa != null) {
        pas[c] = pa;
        addColumn(tColName, pa);
      }
    }
    if (ch == -1) // eof
    return;

    // read rows of data
    StringBuilder warnings = new StringBuilder();
    while (true) {
      line++;
      sb.setLength(0);
      ch = reader.read();
      while (ch != '\n' && ch != -1) {
        sb.append((char) ch);
        ch = reader.read();
      }
      s = sb.toString().trim();
      if (ch == -1 && s.length() == 0) break; // eof
      try {
        sa = StringArray.simpleFromJsonArray(s); // throws SimpleException
        if (sa.size() == nc) {
          for (int c = 0; c < nc; c++) {
            if (pas[c] != null) {
              String tValue = sa.get(c);
              if (tValue.equals("null")) tValue = "";
              else if (fromJsonString[c]) tValue = String2.fromJson(tValue);
              else if (isBoolean[c]) tValue = "true".equals(tValue) ? "1" : "0";
              // else leave as is
              pas[c].addString(tValue);
            }
          }
        } else {
          warnings.append(
              String2.WARNING
                  + ": skipping line #"
                  + line
                  + ": unexpected number of items (observed="
                  + sa.size()
                  + ", expected="
                  + nc
                  + "). [e]\n");
        }
      } catch (Exception e) {
        warnings.append("  line #" + line + ": " + e.getMessage() + "\n");
      }
      if (ch == -1) // eof
      break;
    }

    if (warnings.length() > 0)
      String2.log(
          WARNING_BAD_LINE_OF_DATA_IN
              + "readJsonlCSV("
              + fullFileName
              + "):\n"
              + warnings.toString());

    if (colNames != null) reorderColumns(colNames, false);

    if (colTypes == null && simplify) {
      nc = nColumns(); // may be different from previous use of nc / fewer columns
      int nr = nRows();
      for (int c = 0; c < nc; c++) {
        PrimitiveArray pa = getColumn(c);

        // are they all quoted strings?
        boolean isString = true;
        for (int r = 0; r < nr; r++) {
          s = pa.getString(r);
          if (s.length() > 0 && s.charAt(0) != '\"') {
            isString = false;
            break;
          }
        }
        if (isString) {
          ((StringArray) pa).fromJson();

        } else {
          // are they all true|false?
          boolean tIsBoolean = true;
          for (int r = 0; r < nr; r++) {
            s = pa.getString(r);
            if (s.length() > 0 && !"true".equals(s) && !"false".equals(s)) {
              tIsBoolean = false;
              break;
            }
          }

          if (tIsBoolean) {
            setColumn(c, ByteArray.toBooleanToByte(pa));

          } else {
            // It has numbers. It shouldn't have any quoted strings.
            simplify(c);
          }
        }
      }
    }

    if (reallyVerbose)
      String2.log(
          "  Table.readJsonlCSV("
              + fullFileName
              + ") done. nColumns="
              + nColumns()
              + " nRows="
              + nRows()
              + " TIME="
              + (System.currentTimeMillis() - time)
              + "ms");
  }

  /** This writes the table to a jsonlines CSV file. */
  public void writeJsonlCSV(String fullFileName) throws Exception {
    writeJsonlCSV(fullFileName, false);
  }

  /**
   * This writes a table to a jsonlCSV UTF-8 file. https://jsonlines.org/examples/
   *
   * @param fullFileName This is just used for error messages.
   * @param append If false, any existing file is deleted and a new file is created. If true, if the
   *     file exists, it will be appended to. If it doesn't exist, it will be created and column
   *     names written.
   * @throws Exception if trouble, including observed nItems != expected nItems.
   */
  public void writeJsonlCSV(String fullFileName, boolean append) throws Exception {
    String msg = "  Table.writeJsonlCSV " + fullFileName;
    long time = System.currentTimeMillis();

    // this is focused on fast writing of file
    // this is not for multithreaded use with writers and readers
    //    (if so, change to write to ByteArrayOutputStream
    //    then write all in one blast to file, no bufferedWriter)
    BufferedWriter bw = null;
    int randomInt = Math2.random(Integer.MAX_VALUE);
    boolean writeColumnNames = !append || !File2.isFile(fullFileName);

    try {
      bw =
          File2.getBufferedWriterUtf8(
              new FileOutputStream(fullFileName + (append ? "" : randomInt), append));

      // write the col names
      int nc = nColumns();
      if (writeColumnNames) {
        for (int c = 0; c < nc; c++) {
          bw.write(c == 0 ? '[' : ',');
          bw.write(String2.toJson(columnNames.get(c)));
        }
        bw.write("]\n");
      }

      // write the data
      int nr = nRows();
      for (int r = 0; r < nr; r++) {
        for (int c = 0; c < nc; c++) {
          bw.write(c == 0 ? '[' : ',');
          bw.write(columns.get(c).getJsonString(r));
        }
        bw.write("]\n");
      }

      bw.close();
      bw = null;
      if (!append) // replace the existing file
      File2.rename(fullFileName + randomInt, fullFileName); // throws Exception if trouble

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");

    } catch (Exception e) {
      if (bw != null) {
        try {
          bw.close();
        } catch (Throwable t2) {
        }
      }
      File2.delete(fullFileName + randomInt);
      File2.delete(fullFileName);

      String2.log(msg);
      throw e;
    }
  }

  /**
   * This reads a table from a parquet file. Currently this does not support compressed files.
   *
   * @throws Exception if serious trouble
   */
  public void readParquet(
      String fullFileName, StringArray colNames, String[] colTypes, boolean simplify)
      throws Exception {
    clear();
    InputFile parquetFile = new LocalInputFile(java.nio.file.Path.of(fullFileName));
    ParquetFileReader fileReader =
        new ParquetFileReader(parquetFile, ParquetReadOptions.builder().build());
    try {
      MessageType schema = fileReader.getFileMetaData().getSchema();

      List<Type> fields = schema.getFields();
      int numFields = fields.size();
      PrimitiveArray pas[] = new PrimitiveArray[numFields];
      boolean isBoolean[] = new boolean[numFields]; // all false
      for (int c = 0; c < numFields; c++) {
        Type field = fields.get(c);
        String tFieldName = field.getName();
        tFieldName = tFieldName.equals("null") ? "" : tFieldName;
        PrimitiveArray pa = null;
        // POTENTIAL IMPROVEMENT Could this be more efficient if we used field type to make a
        // properly typed PrimitiveArray?
        if (colNames == null) {
          pa = new StringArray();
        } else {
          int which = colNames.indexOf(tFieldName);
          if (which >= 0) {
            if (colTypes == null) {
              pa = new StringArray();
            } else {
              PAType tPAType = PAType.fromCohortString(colTypes[which]); // it handles boolean
              pa = PrimitiveArray.factory(tPAType, 8, false);
              isBoolean[c] = "boolean".equals(colTypes[which]);
            }
          }
        }
        if (pa != null) {
          pas[c] = pa;
          addColumn(tFieldName, pa);
        }
      }

      PageReadStore pages = null;

      StringBuilder warnings = new StringBuilder();
      while (null != (pages = fileReader.readNextRowGroup())) {
        try {
          final long rows = pages.getRowCount();
          // POTENTIAL IMPROVEMENT can we use filters to prevent reading clumns we don't care about?
          final MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
          final RecordReader<org.apache.parquet.example.data.Group> recordReader =
              columnIO.getRecordReader(pages, new GroupRecordConverter(schema));
          for (int i = 0; i < rows; i++) {
            final org.apache.parquet.example.data.Group g = recordReader.read();
            int fieldCount = g.getType().getFieldCount();
            int countInRow = -1;
            for (int field = 0; field < fieldCount; field++) {
              countInRow = Math.max(g.getFieldRepetitionCount(field), countInRow);
            }
            for (int field = 0; field < fieldCount; field++) {
              int valueCount = g.getFieldRepetitionCount(field);
              for (int index = 0; index < valueCount; index++) {
                if (pas[field] != null) {
                  // POTENTIAL IMPROVEMENT use field types to avoid going to string and back?
                  String tValue = g.getValueToString(field, index);
                  if (tValue.equals("null")) tValue = "";
                  else if (isBoolean[field]) tValue = "true".equals(tValue) ? "1" : "0";
                  // else leave as is
                  pas[field].addString(tValue);
                }
              }
              // This is adding missing values to a column to ensure all columns have the
              // proper number of rows at the end.
              for (int index = valueCount; index < countInRow; index++) {
                if (pas[field] != null) {
                  pas[field].addString("");
                }
              }
            }
          }
        } catch (Exception e) {
          warnings.append("  rowIndex #" + pages.getRowIndexes() + ": " + e.getMessage() + "\n");
        }
      }
      if (warnings.length() > 0)
        String2.log(
            WARNING_BAD_LINE_OF_DATA_IN
                + "readParquet("
                + fullFileName
                + "):\n"
                + warnings.toString());
    } finally {
      fileReader.close();
    }

    if (colNames != null) reorderColumns(colNames, false);

    if (colTypes == null && simplify) {
      int nc = nColumns(); // may be different from previous use of nc / fewer columns
      int nr = nRows();
      String s;
      for (int c = 0; c < nc; c++) {
        PrimitiveArray pa = getColumn(c);

        // are they all quoted strings?
        boolean isString = true;
        for (int r = 0; r < nr; r++) {
          s = pa.getString(r);
          if (s != null && s.length() > 0 && s.charAt(0) != '\"') {
            isString = false;
            break;
          }
        }
        if (isString) {
          ((StringArray) pa).fromJson();

        } else {
          // are they all true|false?
          boolean tIsBoolean = true;
          for (int r = 0; r < nr; r++) {
            s = pa.getString(r);
            if (s != null && s.length() > 0 && !"true".equals(s) && !"false".equals(s)) {
              tIsBoolean = false;
              break;
            }
          }

          if (tIsBoolean) {
            setColumn(c, ByteArray.toBooleanToByte(pa));

          } else {
            // It has numbers. It shouldn't have any quoted strings.
            simplify(c);
          }
        }
      }
    }
  }

  private boolean isTimeColumn(int col) {
    return "time".equalsIgnoreCase(getColumnName(col))
        && Calendar2.SECONDS_SINCE_1970.equals(columnAttributes.get(col).getString("units"));
  }

  private MessageType getParquetSchemaForTable() {
    MessageTypeBuilder schemaBuilder = org.apache.parquet.schema.Types.buildMessage();
    for (int j = 0; j < nColumns(); j++) {
      String columnName = getColumnName(j);
      if (isTimeColumn(j)) {
        schemaBuilder
            .optional(PrimitiveTypeName.INT64)
            .as(LogicalTypeAnnotation.timestampType(true, TimeUnit.MILLIS))
            .named(columnName);
        continue;
      }
      switch (getColumn(j).elementType()) {
        case BYTE:
          schemaBuilder.optional(PrimitiveTypeName.INT32).named(columnName);
          break;
        case SHORT:
          schemaBuilder.optional(PrimitiveTypeName.INT32).named(columnName);
          break;
        case CHAR:
          schemaBuilder
              .optional(PrimitiveTypeName.BINARY)
              .as(LogicalTypeAnnotation.stringType())
              .named(columnName);
          break;
        case INT:
          schemaBuilder.optional(PrimitiveTypeName.INT32).named(columnName);
          break;
        case LONG:
          schemaBuilder.optional(PrimitiveTypeName.INT64).named(columnName);
          break;
        case FLOAT:
          schemaBuilder.optional(PrimitiveTypeName.FLOAT).named(columnName);
          break;
        case DOUBLE:
          schemaBuilder.optional(PrimitiveTypeName.DOUBLE).named(columnName);
          break;
        case STRING:
          schemaBuilder
              .optional(PrimitiveTypeName.BINARY)
              .as(LogicalTypeAnnotation.stringType())
              .named(columnName);
          break;
        case UBYTE:
          schemaBuilder.optional(PrimitiveTypeName.INT32).named(columnName);
          break;
        case USHORT:
          schemaBuilder.optional(PrimitiveTypeName.INT32).named(columnName);
          break;
        case UINT:
          schemaBuilder.optional(PrimitiveTypeName.INT64).named(columnName);
          break;
        case ULONG:
          schemaBuilder.optional(PrimitiveTypeName.DOUBLE).named(columnName);
          break;
        case BOOLEAN:
          schemaBuilder.optional(PrimitiveTypeName.BOOLEAN).named(columnName);
          break;
      }
    }
    return schemaBuilder.named("m");
  }

  private void addMetadata(Map<String, String> metadata, Attributes attributes, String prefix) {
    String names[] = attributes.getNames();
    for (int ni = 0; ni < names.length; ni++) {
      String tName = names[ni];
      if (!String2.isSomething(tName)) {
        continue;
      }
      PrimitiveArray tValue = attributes.get(tName);
      if ("time_".equalsIgnoreCase(prefix)
          && Calendar2.SECONDS_SINCE_1970.equals(attributes.getString(tName))) {
        metadata.put(prefix + tName, Calendar2.MILLISECONDS_SINCE_1970);
      } else {
        metadata.put(prefix + tName, tValue == null ? null : tValue.toString());
      }
    }
  }

  /**
   * This writes a table to a Parquet UTF-8 file.
   *
   * @param fullFileName This is just used for error messages.
   * @throws Exception if trouble, including observed nItems != expected nItems.
   */
  public void writeParquet(String fullFileName, boolean fullMetadata) throws Exception {
    String msg = "  Table.writeParquet " + fullFileName;
    long time = System.currentTimeMillis();
    convertToStandardMissingValues();

    int randomInt = Math2.random(Integer.MAX_VALUE);
    MessageType schema = getParquetSchemaForTable();

    Map<String, String> metadata = new HashMap<>();
    if (fullMetadata) {
      addMetadata(metadata, globalAttributes, "");
      for (int col = 0; col < nColumns(); col++) {
        Attributes colAttributes = columnAttributes.get(col);
        if (colAttributes == null) {
          continue;
        }
        addMetadata(metadata, colAttributes, getColumnName(col) + "_");
      }
    }
    String columnNames = "";
    String columnUnits = "";
    for (int col = 0; col < nColumns(); col++) {
      Attributes colAttributes = columnAttributes.get(col);
      if (colAttributes == null) {
        continue;
      }
      if (columnNames.length() > 0) {
        columnNames += ",";
        columnUnits += ",";
      }
      columnNames += getColumnName(col);
      if (isTimeColumn(col)) {
        columnUnits += Calendar2.MILLISECONDS_SINCE_1970;
      } else {
        columnUnits += colAttributes.getString("units");
      }
    }
    metadata.put("column_names", columnNames);
    metadata.put("column_units", columnUnits);
    try (ParquetWriter<List<PAOne>> writer =
        new ParquetWriterBuilder(
                schema,
                new LocalOutputFile(java.nio.file.Path.of(fullFileName + randomInt)),
                metadata)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
            .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
            .withConf(new Configuration())
            .withValidation(false)
            .withDictionaryEncoding(false)
            .build()) {

      for (int row = 0; row < nRows(); row++) {
        ArrayList<PAOne> record = new ArrayList<>();
        for (int j = 0; j < nColumns(); j++) {
          if (isTimeColumn(j)) {
            // Convert from seconds since epoch to millis since epoch.
            record.add(getPAOneData(j, row).multiply(PAOne.fromInt(1000)));
          } else {
            record.add(getPAOneData(j, row));
          }
        }
        writer.write(record);
      }
      writer.close();

      File2.rename(fullFileName + randomInt, fullFileName); // throws Exception if trouble

      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - time)
                + "ms");

    } catch (Exception e) {
      File2.delete(fullFileName + randomInt);
      File2.delete(fullFileName);

      String2.log(msg);
      throw e;
    }
  }

  /**
   * This returns an Igor-safe column name which doesn't match a name already in IgorReservedNames
   * or the cNamesHashset. The new name will be added to colNamesHashset.
   *
   * @param colName The raw, initial column name.
   * @param colNamesHashset
   * @return the safe name.
   */
  public static String makeUniqueIgorColumnName(String colName, HashSet<String> colNamesHashset) {
    colName = String2.encodeMatlabNameSafe(colName);
    for (int i = 1; i < 1000000; i++) {
      String tColName = colName + (i == 1 ? "" : "" + i);
      if (!IgorReservedNames.contains(tColName)
          && colNamesHashset.add(tColName)) // true if not already present
      return tColName;
    }
    return colName + "ShouldntHappen";
  }

  /**
   * This writes one column (pa) to a Igor Text File .itx.
   *
   * @param colName This should be a name returned by makeUniqueIgorColumnName().
   * @param dimInfo The dimension info if multidimensional (e.g., "/N=(nY,nX,nZ,nT)", note flipped
   *     x,y order!) or null or "" if not.
   * @param pa If multidimensional, it is in standard ERDDAP row-major order. This should be already
   *     done: convertToStandardMissingValues() atts.getDouble("_FillValue"),
   *     atts.getDouble("missing_value"));
   * @param writer At the end, it is flushed, not closed.
   * @param isTimeStamp if true, pa should be DoubleArray with epochSeconds values.
   * @param setScaleForDims ready-to-use SetScale info for the dimensions, or "".
   * @throws Exception if IO trouble.
   */
  public static void writeIgorWave(
      Writer writer,
      String colName,
      String dimInfo,
      PrimitiveArray pa,
      String units,
      boolean isTimeStamp,
      String setScaleForDims)
      throws Exception {

    int nRows = pa.size();
    double stats[] = pa.calculateStats(); // okay because using standardMissingValues
    double colMin = stats[PrimitiveArray.STATS_MIN]; // may be NaN
    double colMax = stats[PrimitiveArray.STATS_MAX]; // may be NaN

    PAType paType = pa.elementType();
    String safeColName = String2.encodeMatlabNameSafe(colName);
    if (dimInfo == null) dimInfo = "";
    String it = // igor data type    //see page II-159 in File Reference document
        paType == PAType.BYTE
            ? "B"
            : paType == PAType.SHORT
                ? "W"
                : paType == PAType.INT
                    ? "I"
                    : paType == PAType.LONG
                        ? "T"
                        : // -> text. Not good, but no loss of precision.
                        paType == PAType.FLOAT
                            ? "S"
                            : paType == PAType.DOUBLE
                                ? "D"
                                : paType == PAType.UBYTE
                                    ? "B/U"
                                    : // U=unsigned
                                    paType == PAType.USHORT
                                        ? "W/U"
                                        : paType == PAType.UINT
                                            ? "I/U"
                                            : paType == PAType.ULONG
                                                ? "T"
                                                : // -> text. Not good, but no loss of precision.
                                                paType == PAType.CHAR
                                                    ? "T"
                                                    : "T"; // String and unexpected
    boolean asString = it.equals("T");

    writer.write(
        "WAVES/"
            + it
            + dimInfo
            +
            // don't use /O to overwrite existing waves. If conflict, user will be asked.
            " "
            + safeColName
            + IgorEndOfLine
            + "BEGIN"
            + IgorEndOfLine);

    // write the data
    if (asString) {
      for (int row = 0; row < nRows; row++) {
        // String data written as json strings (in double quotes with \ encoded chars)
        writer.write(String2.toJson(pa.getString(row)));
        writer.write(IgorEndOfLine);
      }
    } else if (isTimeStamp) {
      for (int row = 0; row < nRows; row++) {
        // igor stores datetime as seconds since 1/1/1904
        double d = pa.getDouble(row);
        if (Double.isFinite(d)) {
          d = Calendar2.epochSecondsToUnitsSince(IgorBaseSeconds, IgorFactorToGetSeconds, d);
          writer.write("" + d);
        } else {
          writer.write(IgorNanString);
        }
        writer.write(IgorEndOfLine);
      }
    } else { // numeric
      for (int row = 0; row < nRows; row++) {
        String s = pa.getString(row); // ints will be formatted as ints
        writer.write(s.length() == 0 ? IgorNanString : s);
        writer.write(IgorEndOfLine);
      }
    }

    // END
    writer.write("END" + IgorEndOfLine);

    // SetScale
    if (!asString) {
      if (units == null) units = "";
      if (isTimeStamp) {
        units = "dat"; // special case in igor
        colMin =
            Calendar2.epochSecondsToUnitsSince(IgorBaseSeconds, IgorFactorToGetSeconds, colMin);
        colMax =
            Calendar2.epochSecondsToUnitsSince(IgorBaseSeconds, IgorFactorToGetSeconds, colMax);
      }
      writer.write(
          "X SetScale d "
              + // d=specifying data full scale, ',' after d???
              (Double.isNaN(colMin)
                  ? "0,0"
                  : // ???no non-MV data values; 0,0 is used in their examples
                  pa instanceof DoubleArray || pa instanceof ULongArray
                      ? colMin + "," + colMax
                      : pa instanceof FloatArray
                          ? Math2.doubleToFloatNaN(colMin) + "," + Math2.doubleToFloatNaN(colMax)
                          : Math2.roundToLong(colMin) + "," + Math2.roundToLong(colMax))
              + ", "
              + String2.toJson(units)
              + ", "
              + safeColName
              + IgorEndOfLine);
    }
    if (String2.isSomething(setScaleForDims)) writer.write(setScaleForDims);

    writer.write(IgorEndOfLine);
    writer.flush(); // essential (at the end)
  }

  /**
   * Save the table's data as an Igor Text File .itx file. <br>
   * File reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/II-09%20Data%20Import%20Export.pdf <br>
   * Command reference: in Bob's /programs/igor/ or
   * https://www.wavemetrics.net/doc/igorman/V-01%20Reference.pdf <br>
   * The file extension should be .itx <br>
   * Timestamp columns should have epochSeconds vaules. <br>
   * This assumes missing values haven't yet been standardized (e.g., to NaN). <br>
   * This is tested by EDDGridFromNcFiles.testIgor() since EDDGrid.saveAsIgor uses this to save
   * axisVars-only response.
   *
   * @param writer It is always closed at the end.
   * @throws Throwable if trouble
   */
  public void saveAsIgor(Writer writer) throws Throwable {

    String msg = "  Table.saveAsIgor";
    long time = System.currentTimeMillis();
    try {

      if (nRows() == 0)
        throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (at start of saveAsIgor)");

      // open an OutputStream
      writer.write("IGOR" + IgorEndOfLine);

      // write each col as a wave separately, so data type is preserved
      HashSet<String> colNamesHashset = new HashSet();
      int nCols = nColumns();
      for (int col = 0; col < nCols; col++) {
        Attributes atts = columnAttributes(col);
        String units = atts.getString("units");
        boolean isTimeStamp =
            units != null
                && (units.equals(Calendar2.SECONDS_SINCE_1970)
                    || // EDV.TIME_UNITS
                    units.equals("s{since 1970-01-01T00:00:00Z}")); // EDV.TIME_UCUM_UNITS

        PrimitiveArray pa = getColumn(col);
        pa.convertToStandardMissingValues(
            atts.getString("_FillValue"), atts.getString("missing_value"));

        writeIgorWave(
            writer,
            makeUniqueIgorColumnName(getColumnName(col), colNamesHashset),
            "",
            pa,
            units,
            isTimeStamp,
            "");
      }

      writer.flush(); // essential (but done by close?)
      writer.close(); // it should call flush
      // done!
      if (reallyVerbose)
        String2.log(msg + " finished. TIME=" + (System.currentTimeMillis() - time) + "ms");

    } catch (Throwable t) {
      String2.log(msg);
      if (writer != null)
        try {
          writer.close();
        } catch (Throwable t9) {
        }
      ;

      throw t;
    }
  }

  /**
   * This reads a table structure (everything but data) from an ERDDAP info url for a TableDap
   * dataset.
   *
   * <ul>
   *   <li>If no exception is thrown, the file was successfully read.
   * </ul>
   *
   * @param url e.g., http://localhost:8080/cwexperimental/info/pmelTaoDySst/index.json . It MUST be
   *     already percentEncoded as needed.
   * @throws Exception if trouble
   */
  public void readErddapInfo(String url) throws Exception {

    String msg = "  Table.readErddapInfo " + url;
    if (reallyVerbose) String2.log(msg);
    String errorInMethod = String2.ERROR + " in" + msg + ":\n";

    // clear everything
    clear();

    // read the json source
    // "columnNames": ["Row Type", "Variable Name", "Attribute Name", "Java Type", "Value"],
    Table infoTable = new Table();
    infoTable.readJson(url, SSR.getBufferedUrlReader(url));
    String tColNames =
        " column not found in colNames=" + String2.toCSSVString(infoTable.getColumnNames());
    int nRows = infoTable.nRows();
    int rowTypeCol = infoTable.findColumnNumber("Row Type");
    int variableNameCol = infoTable.findColumnNumber("Variable Name");
    int attributeNameCol = infoTable.findColumnNumber("Attribute Name");
    int javaTypeCol = infoTable.findColumnNumber("Data Type");
    int valueCol = infoTable.findColumnNumber("Value");
    Test.ensureTrue(rowTypeCol != -1, errorInMethod + "'Row Type'" + tColNames);
    Test.ensureTrue(variableNameCol != -1, errorInMethod + "'Variable Name'" + tColNames);
    Test.ensureTrue(attributeNameCol != -1, errorInMethod + "'Attribute Name'" + tColNames);
    Test.ensureTrue(javaTypeCol != -1, errorInMethod + "'Data Type'" + tColNames);
    Test.ensureTrue(valueCol != -1, errorInMethod + "'Value'" + tColNames);
    for (int row = 0; row < nRows; row++) {
      String rowType = infoTable.getStringData(rowTypeCol, row);
      String variableName = infoTable.getStringData(variableNameCol, row);
      String javaType = infoTable.getStringData(javaTypeCol, row);
      PAType tPAType = PAType.fromCohortString(javaType);
      if (rowType.equals("attribute")) {
        String attributeName = infoTable.getStringData(attributeNameCol, row);
        String value = infoTable.getStringData(valueCol, row);
        PrimitiveArray pa =
            tPAType == PAType.STRING
                ? new StringArray(String2.splitNoTrim(value, '\n'))
                : PrimitiveArray.csvFactory(tPAType, value);
        if (variableName.equals("GLOBAL") || variableName.equals("NC_GLOBAL"))
          globalAttributes.add(attributeName, pa);
        else columnAttributes(variableName).add(attributeName, pa);
      } else if (rowType.equals("variable")) {
        addColumn(variableName, PrimitiveArray.factory(tPAType, 1, false));
      } else throw new Exception("Unexpected rowType=" + rowType);
    }
  }

  /**
   * This mimics the a simple directory listing web page created by Apache. <br>
   * It mimics https://www1.ncdc.noaa.gov/pub/data/cmb/ersst/v4/netcdf/ stored on Bob's computer as
   * c:/programs/apache/listing3.html <br>
   * ***WARNING*** The URL that got the user to this page MUST be a directoryURL ending in '/', or
   * the links don't work (since they are implied to be relative to the current URL, not explicit)!
   * <br>
   * This just writes the part inside the 'body' tag. <br>
   * If there is a parentDirectory, its link will be at the top of the list. <br>
   * The table need not be sorted initially. This method handles sorting. <br>
   * The table should have 4 columns: "Name" (String, dir names should end in /), "Last modified"
   * (long, in milliseconds, directories/unknown should have Long.MAX_VALUE), "Size" (long,
   * directories/unknown should have Long.MAX_VALUE), and "Description" (String) <br>
   * The displayed Last Modified time will be Zulu timezone. <br>
   * The displayed size will be some number of bytes [was: truncated to some number of K (1024), M
   * (1024^2), G (1024^3), or T (1024^4)]
   *
   * @param localDir the actual local directory, so this can determine image size (or null if not a
   *     real dir or you don't want to do that)
   * @param showUrlDir the part of the URL directory name to be displayed (with trailing slash).
   *     This is for display only.
   * @param userQuery may be null. Still percent encoded. <br>
   *     The parameter options are listed at
   *     https://httpd.apache.org/docs/2.0/mod/mod_autoindex.html <br>
   *     C=N sorts the directory by file name <br>
   *     C=M sorts the directory by last-modified date, then file name <br>
   *     C=S sorts the directory by size, then file name <br>
   *     C=D sorts the directory by description, then file name <br>
   *     O=A sorts the listing in Ascending Order <br>
   *     O=D sorts the listing in Descending Order <br>
   *     F=0 formats the listing as a simple list (not FancyIndexed) <br>
   *     F=1 formats the listing as a FancyIndexed list <br>
   *     F=2 formats the listing as an HTMLTable FancyIndexed list <br>
   *     V=0 disables version sorting <br>
   *     V=1 enables version sorting <br>
   *     P=pattern lists only files matching the given pattern <br>
   *     The default is "C=N;O=A". <br>
   *     Currently, only C= and O= parameters are supported.
   * @param iconUrlDir the public URL directory (with slash at end) with the icon files
   * @param addParentDir if true, this shows a link to the parent directory
   * @param dirNames is the list of subdirectories in the directory just the individual words (and
   *     without trailing '/'), not the whole URL. It will be sorted within directoryListing.
   * @param dirDescriptions may be null
   */
  public String directoryListing(
      String localDir,
      String showUrlDir,
      String userQuery,
      String iconUrlDir,
      String questionMarkImageUrl,
      boolean addParentDir,
      StringArray dirNames,
      StringArray dirDescriptions)
      throws Exception {

    // String2.log("Table.directoryListing("showUrlDir=" + showUrlDir +
    //    "\n  userQuery=" + userQuery +
    //    "\n  iconUrlDir=" + iconUrlDir +
    //    "\n  nDirNames=" + dirNames.size() + " table.nRows=" + nRows());
    // but this doesn't matter: localDir is no longer used below
    if (localDir != null) {
      if (!File2.isDirectory(localDir)) {
        String2.log("Warning: directoryListing localDir=" + localDir + " isn't a directory.");
        localDir = null;
      }
    }

    // en/sure column names are as expected
    String ncssv = getColumnNamesCSSVString();
    String ncssvMust = "Name, Last modified, Size, Description";
    if (!ncssvMust.equals(ncssv))
      throw new SimpleException(
          String2.ERROR
              + " in directoryListing(), the table's column\n"
              + "names must be \""
              + ncssvMust
              + "\".\n"
              + "The names are \""
              + ncssv
              + "\".");
    PrimitiveArray namePA = getColumn(0);
    PrimitiveArray modifiedPA = getColumn(1);
    PrimitiveArray sizePA = getColumn(2);
    PrimitiveArray descriptionPA = getColumn(3);
    modifiedPA.setMaxIsMV(true);
    sizePA.setMaxIsMV(true);

    // ensure column types are as expected
    String tcssv =
        getColumn(0).elementTypeString()
            + ", "
            + getColumn(1).elementTypeString()
            + ", "
            + getColumn(2).elementTypeString()
            + ", "
            + getColumn(3).elementTypeString();
    String tcssvMust = "String, long, long, String";
    if (!tcssvMust.equals(tcssv))
      throw new SimpleException(
          String2.ERROR
              + " in directoryListing(), the table's column\n"
              + "types must be \""
              + tcssvMust
              + "\".\n"
              + "The types are \""
              + tcssv
              + "\".");

    // parse userQuery (e.g., C=N;O=A)
    userQuery =
        userQuery == null
            ? ""
            : ";" + SSR.percentDecode(userQuery) + ";"; // ";" make it easy to search below
    int keyColumns[] = // see definitions in javadocs above
        userQuery.indexOf(";C=N;") >= 0
            ? new int[] {0, 1}
            : // 2nd not needed, but be consistent
            userQuery.indexOf(";C=M;") >= 0
                ? new int[] {1, 0}
                : userQuery.indexOf(";C=S;") >= 0
                    ? new int[] {2, 0}
                    : userQuery.indexOf(";C=D;") >= 0 ? new int[] {3, 0} : new int[] {0, 1};
    boolean ascending[] = // see definitions in javadocs above
        userQuery.indexOf(";O=D;") >= 0 ? new boolean[] {false, false} : new boolean[] {true, true};
    // Order=A|D in column links will be 'A',
    //  except currently selected column will offer !currentAscending
    char linkAD[] = {'A', 'A', 'A', 'A'};
    linkAD[keyColumns[0]] = ascending[0] ? 'D' : 'A'; // !currentAscending

    // and sort the table (while lastModified and size are still the raw values)
    sortIgnoreCase(keyColumns, ascending);

    int tnRows = nRows();
    /*
    //convert LastModified to string  (after sorting)
    StringArray newModifiedPA = new StringArray(tnRows, false);
    for (int row = 0; row < tnRows; row++) {
        String newMod = "";
        try {
            long tl = modifiedPA.getLong(row);
            newMod = tl == Long.MAX_VALUE? "" : //show hh:mm, not more
                Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(tl)).substring(0, 17);
        } catch (Throwable t) {
        }
        newModifiedPA.add(newMod);
    }
    modifiedPA = newModifiedPA;
    */

    // convert sizes
    /*
    StringArray newSizePA = new StringArray(tnRows, false);
    for (int row = 0; row < tnRows; row++) {
        String newSize = "";
        try {
            //lim ensures the displayed number will be lim ... lim*1000-1
           //(values of 1...lim-1 are not as precise)
            int lim = 6;
            long tl = sizePA.getLong(row);
            newSize = tl == Long.MAX_VALUE? "" :
                (tl >= lim * Math2.BytesPerPB? (tl / Math2.BytesPerPB) + "P" :
                 tl >= lim * Math2.BytesPerTB? (tl / Math2.BytesPerTB) + "T" :
                 tl >= lim * Math2.BytesPerGB? (tl / Math2.BytesPerGB) + "G" :
                 tl >= lim * Math2.BytesPerMB? (tl / Math2.BytesPerMB) + "M" :
                 tl >= lim * Math2.BytesPerKB? (tl / Math2.BytesPerKB) + "K" : tl + "");
        } catch (Throwable t) {
        }
        newSizePA.add(newSize);
    }
    sizePA = newSizePA;
    */

    // <table><tr><th><img src="/icons/blank.gif" alt="[ICO]"></th>
    // <th><a href="?C=N;O=D">Name</a></th>
    // <th><a href="?C=M;O=A">Last modified</a></th>
    // <th><a href="?C=S;O=A">Size</a></th>
    // <th><a href="?C=D;O=A">Description</a></th></tr>
    // <tr><th colspan="5"><hr></th></tr>
    // <tr><td valign="top"><img src="/icons/folder.gif" alt="[DIR]"></td><td><a
    // href="OEDV/">OEDV/</a></td><td align="right">26-Feb-2017 13:22  </td><td align="right">  -
    // </td><td>&nbsp;</td></tr>

    // write showUrlDir
    StringBuilder sb = new StringBuilder();
    sb.append(HtmlWidgets.htmlTooltipScript(File2.getDirectory(questionMarkImageUrl)));

    // write column names and hr
    String iconStyle = ""; // " style=\"vertical-align:middle;\"";
    sb.append(
        "<table class=\"compact nowrap\" style=\"border-collapse:separate; border-spacing:12px 0px;\">\n"
            + "<tr><th><img class=\"B\" src=\""
            + iconUrlDir
            + "blank.gif\" alt=\"[ICO]\""
            + iconStyle
            + "></th>"
            + "<th><a href=\"?C=N;O="
            + linkAD[0]
            + "\">Name</a></th>"
            + "<th><a href=\"?C=M;O="
            + linkAD[1]
            + "\">Last modified</a></th>"
            + "<th><a href=\"?C=S;O="
            + linkAD[2]
            + "\">Size</a></th>"
            + "<th><a href=\"?C=D;O="
            + linkAD[3]
            + "\">Description</a></th></tr>\n"
            + "<tr><th colspan=\"5\"><hr></th></tr>\n");

    // display the directories
    // if shown, parentDir always at top
    if (dirDescriptions == null) {
      dirNames.sortIgnoreCase();
    } else {
      Table dirTable = new Table();
      dirTable.addColumn("names", dirNames);
      dirTable.addColumn("desc", dirDescriptions);
      dirTable.leftToRightSortIgnoreCase(1);
    }
    if (keyColumns[0] == 0 && !ascending[0]) { // if sorted by Names, descending order
      dirNames.reverse();
      if (dirDescriptions != null) dirDescriptions.reverse();
    }
    if (addParentDir && dirNames.indexOf("..") < 0) { // .. always at top
      dirNames.atInsert(0, "..");
      if (dirDescriptions != null) dirDescriptions.atInsert(0, "");
    }
    int nDir = dirNames.size();
    for (int row = 0; row < nDir; row++) {
      try {
        String dirName = dirNames.get(row);
        if (!dirName.equals("..")) dirName += "/";
        String dirDes = dirDescriptions == null ? "" : dirDescriptions.get(row);
        String showDirName = dirName;
        String iconFile = "dir.gif"; // default
        String iconAlt = "DIR";
        if (dirName.equals("..")) {
          showDirName = "Parent Directory";
          iconFile = "back.gif";
        }
        sb.append(
            "<tr>"
                + "<td><img class=\"B\" src=\""
                + iconUrlDir
                + iconFile
                + "\" alt=\"["
                + iconAlt
                + "]\""
                + iconStyle
                + "></td>"
                + "<td><a href=\""
                + XML.encodeAsHTMLAttribute(dirName)
                + "\">"
                + XML.encodeAsXML(showDirName)
                + "</a></td>"
                + "<td class=\"R\">-</td>"
                + // lastMod
                "<td class=\"R\">-</td>"
                + // size
                "<td>"
                + XML.encodeAsXML(dirDes)
                + "</td>"
                + "</tr>\n");

      } catch (Throwable t) {
        String2.log(
            String2.ERROR
                + " for directoryListing("
                + showUrlDir
                + ")\n"
                + MustBe.throwableToString(t));
      }
    }

    // display the files
    for (int row = 0; row < tnRows; row++) {
      try {
        String fileName = namePA.getString(row);
        String fileNameLC = fileName.toLowerCase();
        String encodedFileName = XML.encodeAsHTMLAttribute(fileName);

        // very similar code in Table.directoryListing and TableWriterHtmlTable.
        int whichIcon = File2.whichIcon(fileName);
        String iconFile = File2.ICON_FILENAME.get(whichIcon);
        String iconAlt = File2.ICON_ALT.get(whichIcon); // always 3 characters
        String extLC = File2.getExtension(fileNameLC);

        // make HTML for a viewer?
        String viewer = "";
        String imgStyle = "";
        if (iconAlt.equals("SND")) {
          // viewer = HtmlWidgets.htmlAudioControl(encodedFileName);
          viewer = HtmlWidgets.cssTooltipAudio(questionMarkImageUrl, "?", imgStyle, fileName);

          // } else if (iconAlt.equals("IMG") && localDir != null) {
          //    //this system has to open the local file to get the image's size
          //    viewer = HtmlWidgets.imageInTooltip(localDir + fileName,
          //        encodedFileName, questionMarkImageUrl);

        } else if (iconAlt.equals("IMG")) {
          // this system doesn't need to know the size ahead of time
          viewer =
              HtmlWidgets.cssTooltipImage(
                  questionMarkImageUrl, "?", imgStyle, fileName, "img" + row);

        } else if (iconAlt.equals("MOV")) {
          viewer = HtmlWidgets.cssTooltipVideo(questionMarkImageUrl, "?", imgStyle, fileName);
        }

        // make DDMonYYYY HH:MM formatted lastModified time
        String newMod = "";
        long tl = modifiedPA.getLong(row);
        try {
          newMod =
              tl == Long.MAX_VALUE
                  ? ""
                  : // show hh:mm, not more
                  Calendar2.formatAsDDMonYYYY(Calendar2.newGCalendarZulu(tl)).substring(0, 17);
        } catch (Throwable t) {
          String2.log(
              "Caught throwable while dealing with tl=" + tl + ":\n" + MustBe.throwableToString(t));
        }

        String sizePAs = sizePA.getString(row);
        sb.append(
            "<tr>"
                + "<td><img class=\"B\" src=\""
                + iconUrlDir
                + iconFile
                + "\" alt=\"["
                + iconAlt
                + "]\""
                + iconStyle
                + ">"
                + (viewer.length() > 0 ? "&nbsp;" + viewer : "")
                + "</td>"
                + "<td><a rel=\"bookmark\" href=\""
                + encodedFileName
                + "\">"
                + encodedFileName
                + "</a></td>"
                + "<td class=\"R\">"
                + newMod
                + "</td>"
                + "<td class=\"R\">"
                + (sizePAs.length() == 0 ? "-" : sizePAs)
                + "</td>"
                + "<td>"
                + XML.encodeAsXML(descriptionPA.getString(row))
                + "</td>"
                + "</tr>\n");
      } catch (Throwable t) {
        String2.log(
            String2.ERROR
                + " for directoryListing("
                + showUrlDir
                + ")\n"
                + MustBe.throwableToString(t));
      }
    }

    sb.append(
        "<tr><th colspan=\"5\"><hr></th></tr>\n"
            + // <hr>
            "</table>\n"
            + nDir
            + (nDir == 1 ? " directory, " : " directories, ")
            + tnRows
            + (tnRows == 1 ? " file " : " files")
            + "\n\n");
    return sb.toString();
  }

  /**
   * This saves the current table in some type of file. If the file already exists, it is touched,
   * and nothing else is done.
   *
   * @param fullFileName including directory and extension (e.g., ".asc"), but not including ".zip"
   *     if you want it zipped.
   * @param saveAsType one of the SAVE_AS constants. If SAVE_AS_4D_NC, it is assumed that column
   *     1=lon, 2=lat, 3=depth, 4=time.
   * @param dimensionName usually "row", but e.g., may be "time", "station", or "observation", or
   *     ...?
   * @param zipIt If true, creates a .zip file and deletes the intermediate file (e.g., .asc). If
   *     false, the specified saveAsType is created.
   * @throws Exception if trouble
   */
  public void saveAs(String fullFileName, int saveAsType, String dimensionName, boolean zipIt)
      throws Exception {

    if (reallyVerbose)
      String2.log("Table.saveAs(name=" + fullFileName + " type=" + saveAsType + ")");
    if (saveAsType != SAVE_AS_TABBED_ASCII
        && saveAsType != SAVE_AS_FLAT_NC
        && saveAsType != SAVE_AS_4D_NC
        && saveAsType != SAVE_AS_MATLAB)
      throw new RuntimeException(
          String2.ERROR + " in Table.saveAs: invalid saveAsType=" + saveAsType);

    String ext = SAVE_AS_EXTENSIONS[saveAsType];

    // does the file already exist?
    String finalName = fullFileName + (zipIt ? ".zip" : "");
    if (File2.touch(finalName)) {
      String2.log("Table.saveAs reusing " + finalName);
      return;
    }

    // save as ...
    long time = System.currentTimeMillis();
    if (saveAsType == SAVE_AS_TABBED_ASCII) saveAsTabbedASCII(fullFileName);
    else if (saveAsType == SAVE_AS_FLAT_NC) saveAsFlatNc(fullFileName, dimensionName);
    else if (saveAsType == SAVE_AS_4D_NC) saveAs4DNc(fullFileName, 0, 1, 2, 3);
    else if (saveAsType == SAVE_AS_MATLAB)
      saveAsMatlab(fullFileName, getColumnName(nColumns() - 1));

    if (zipIt) {
      // zip to a temporary zip file, -j: don't include dir info
      SSR.zip(fullFileName + ".temp.zip", new String[] {fullFileName}, 20);

      // delete the file that was zipped
      File2.delete(fullFileName);

      // if all successful, rename to final name
      File2.rename(
          fullFileName + ".temp.zip", fullFileName + ".zip"); // throws Exception if trouble
    }
  }

  /**
   * This reads an input table file (or 1- or 2-level opendap sequence) and saves it in a file
   * (optionally zipped). A test which reads data from an opendap 1-level sequence and writes it to
   * an .nc file:
   * convert("https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"",
   * 2, testDir + "convert.nc", 1, false);
   *
   * @param inFullName the full name of the file (with the extension .zip if it is zipped) or
   *     opendap sequence url (optionally with a query).
   *     <ul>
   *       <li>2016-12-07: With versions of Tomcat somewhere after 8.0, the url must be stongly
   *           percent-encoded.
   *       <li>If it is zipped, the data file should be the only file in the .zip file and the data
   *           file's name should be inFullName minus the directory and the ".zip" at the end.
   *       <li>All of the data in the file will be read.
   *       <li>If the data is packed (e.g., scale_factor, add_offset), this will not unpack it.
   *       <li>ASCII files must have column names on the first line and data starting on the second
   *           line.
   *           <ul>
   *             <li>The item separator on each line can be tab, comma, or 1 or more spaces.
   *             <li>Missing values for tab- and comma-separated files can be "" or "." or "NaN".
   *             <li>Missing values for space-separated files can be "." or "NaN".
   *             <li>All data rows must have the same number of data items.
   *             <li>The data is initially read as Strings. Then columns are simplified (e.g., to
   *                 doubles, ... or bytes) so they store the data compactly.
   *             <li>Currently, date strings are left as strings.
   *           </ul>
   *       <li>For opendap sequence details, see readOpendapSequence (query must already
   *           SSR.percentEncoded as needed).
   *     </ul>
   *
   * @param inType the type of input file: READ_FLAT_NC, READ_ASCII, READ_OPENDAP_SEQUENCE
   * @param outFullName the full name of the output file (but without the .zip if you want it
   *     zipped).
   * @param outType the type of output file: SAVE_AS_MAT, SAVE_AS_TABBED_ASCII, SAVE_AS_FLAT_NC, or
   *     SAVE_AS_4D_NC.
   * @param dimensionName usually "row", but, e.g., maybe "time", "station", or "observation", or
   *     ...?
   * @param zipIt true if you want the file to be zipped
   * @throws Exception if trouble
   */
  public static void convert(
      String inFullName,
      int inType,
      String outFullName,
      int outType,
      String dimensionName,
      boolean zipIt)
      throws Exception {

    if (reallyVerbose)
      String2.log(
          "Table.convert in="
              + inFullName
              + " inType="
              + inType
              + "  out="
              + outFullName
              + " outType="
              + outType
              + " zipIt="
              + zipIt);

    // unzip inFullName
    boolean unzipped = inFullName.toLowerCase().endsWith(".zip");
    if (unzipped) {
      String tempDir = File2.getSystemTempDirectory();
      if (reallyVerbose) String2.log("unzipping to systemTempDir = " + tempDir);

      SSR.unzip(inFullName, tempDir, true, 10000, null);
      inFullName =
          tempDir
              + File2.getNameAndExtension(inFullName)
                  .substring(0, inFullName.length() - 4); // without .zip at end
    }

    // read the file
    Table table = new Table();
    if (inType == READ_ASCII) table.readASCII(inFullName);
    else if (inType == READ_FLAT_NC) table.readFlatNc(inFullName, null, 0); // standardizeWhat=0
    else if (inType == READ_OPENDAP_SEQUENCE) table.readOpendapSequence(inFullName, false);
    else throw new Exception(String2.ERROR + " in Table.convert: unrecognized inType: " + inType);

    // if input file was unzipped, delete the unzipped file
    if (unzipped) File2.delete(inFullName);

    // save the file (and zipIt?)
    table.saveAs(outFullName, outType, dimensionName, zipIt);
  }

  /** This rearranges the columns to be by case-insensitive alphabetical column name. */
  public void sortColumnsByName() {
    StringArray tColNames = new StringArray(columnNames);
    tColNames.sortIgnoreCase();
    reorderColumns(tColNames, false);
  }

  /**
   * THIS IS NOT FINISHED.
   *
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   */
  public void readArgoProfile(String fileName, int standardizeWhat) throws Exception {

    String msg = "  Table.readArgoProfile " + fileName;
    long tTime = System.currentTimeMillis();
    // Attributes gridMappingAtts = null; //method is unfinished
    NetcdfFile nc = NcHelper.openFile(fileName);
    try {
      //   DATE_TIME = 14;
      //   N_PROF = 632;
      //   N_PARAM = 3;
      //   N_LEVELS = 71;
      //   N_CALIB = 1;
      //   N_HISTORY = UNLIMITED;   // (0 currently)
      Variable var;
      PrimitiveArray pa;
      int col;
      NcHelper.getGroupAttributes(nc.getRootGroup(), globalAttributes);

      // The plan is: make minimal changes here. Change metadata etc in ERDDAP.

      var = nc.findVariable("DATA_TYPE");
      if (var != null) {
        col = addColumn("dataType", NcHelper.getPrimitiveArray(var));
        NcHelper.getVariableAttributes(var, columnAttributes(col));
      }

      // skip char FORMAT_VERSION(STRING4=4);   :comment = "File format version";

      var = nc.findVariable("HANDBOOK_VERSION");
      if (var != null) {
        col = addColumn("handbookVersion", NcHelper.getPrimitiveArray(var));
        NcHelper.getVariableAttributes(var, columnAttributes(col));
      }

      var = nc.findVariable("REFERENCE_DATE_TIME"); // "YYYYMMDDHHMISS";
      if (var != null) {
        pa = NcHelper.getPrimitiveArray(var);
        double time = Double.NaN;
        try {
          time = Calendar2.gcToEpochSeconds(Calendar2.parseCompactDateTimeZulu(pa.getString(0)));
        } catch (Exception e) {
          String2.log(e.getMessage());
        }
        col = addColumn("time", PrimitiveArray.factory(new double[] {time}));
        NcHelper.getVariableAttributes(var, columnAttributes(col));
        columnAttributes(col).set("units", Calendar2.SECONDS_SINCE_1970);
      }

      var = nc.findVariable("PLATFORM_NUMBER");
      if (var != null) {
        col = addColumn("platformNumber", NcHelper.getPrimitiveArray(var));
        NcHelper.getVariableAttributes(var, columnAttributes(col));
      }

      var = nc.findVariable("PROJECTPLATFORM_NUMBER");
      if (var != null) {
        col = addColumn("platformNumber", NcHelper.getPrimitiveArray(var));
        NcHelper.getVariableAttributes(var, columnAttributes(col));
      }

      /*   char PROJECT_NAME(N_PROF=632, STRING64=64);
        :comment = "Name of the project";
        :_FillValue = " ";
      char PI_NAME(N_PROF=632, STRING64=64);
        :comment = "Name of the principal investigator";
        :_FillValue = " ";
      char STATION_PARAMETERS(N_PROF=632, N_PARAM=3, STRING16=16);
        :long_name = "List of available parameters for the station";
        :conventions = "Argo reference table 3";
        :_FillValue = " ";
      int CYCLE_NUMBER(N_PROF=632);
        :long_name = "Float cycle number";
        :conventions = "0..N, 0 : launch cycle (if exists), 1 : first complete cycle";
        :_FillValue = 99999; // int
      char DIRECTION(N_PROF=632);
        :long_name = "Direction of the station profiles";
        :conventions = "A: ascending profiles, D: descending profiles";
        :_FillValue = " ";
      char DATA_CENTRE(N_PROF=632, STRING2=2);
        :long_name = "Data centre in charge of float data processing";
        :conventions = "Argo reference table 4";
        :_FillValue = " ";
      char DATE_CREATION(DATE_TIME=14);
        :comment = "Date of file creation";
        :conventions = "YYYYMMDDHHMISS";
        :_FillValue = " ";
      char DATE_UPDATE(DATE_TIME=14);
        :long_name = "Date of update of this file";
        :conventions = "YYYYMMDDHHMISS";
        :_FillValue = " ";
      char DC_REFERENCE(N_PROF=632, STRING32=32);
        :long_name = "Station unique identifier in data centre";
        :conventions = "Data centre convention";
        :_FillValue = " ";
      char DATA_STATE_INDICATOR(N_PROF=632, STRING4=4);
        :long_name = "Degree of processing the data have passed through";
        :conventions = "Argo reference table 6";
        :_FillValue = " ";
      char DATA_MODE(N_PROF=632);
        :long_name = "Delayed mode or real time data";
        :conventions = "R : real time; D : delayed mode; A : real time with adjustment";
        :_FillValue = " ";
      char INST_REFERENCE(N_PROF=632, STRING64=64);
        :long_name = "Instrument type";
        :conventions = "Brand, type, serial number";
        :_FillValue = " ";
      char WMO_INST_TYPE(N_PROF=632, STRING4=4);
        :long_name = "Coded instrument type";
        :conventions = "Argo reference table 8";
        :_FillValue = " ";
      double JULD(N_PROF=632);
        :long_name = "Julian day (UTC) of the station relative to REFERENCE_DATE_TIME";
        :units = "days since 1950-01-01 00:00:00 UTC";
        :conventions = "Relative julian days with decimal part (as parts of day)";
        :_FillValue = 999999.0; // double
      char JULD_QC(N_PROF=632);
        :long_name = "Quality on Date and Time";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      double JULD_LOCATION(N_PROF=632);
        :long_name = "Julian day (UTC) of the location relative to REFERENCE_DATE_TIME";
        :units = "days since 1950-01-01 00:00:00 UTC";
        :conventions = "Relative julian days with decimal part (as parts of day)";
        :_FillValue = 999999.0; // double
      double LATITUDE(N_PROF=632);
        :long_name = "Latitude of the station, best estimate";
        :units = "degree_north";
        :_FillValue = 99999.0; // double
        :valid_min = -90.0; // double
        :valid_max = 90.0; // double
      double LONGITUDE(N_PROF=632);
        :long_name = "Longitude of the station, best estimate";
        :units = "degree_east";
        :_FillValue = 99999.0; // double
        :valid_min = -180.0; // double
        :valid_max = 180.0; // double
      char POSITION_QC(N_PROF=632);
        :long_name = "Quality on position (latitude and longitude)";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      char POSITIONING_SYSTEM(N_PROF=632, STRING8=8);
        :long_name = "Positioning system";
        :_FillValue = " ";
      char PROFILE_PRES_QC(N_PROF=632);
        :long_name = "Global quality flag of PRES profile";
        :conventions = "Argo reference table 2a";
        :_FillValue = " ";
      char PROFILE_TEMP_QC(N_PROF=632);
        :long_name = "Global quality flag of TEMP profile";
        :conventions = "Argo reference table 2a";
        :_FillValue = " ";
      char PROFILE_PSAL_QC(N_PROF=632);
        :long_name = "Global quality flag of PSAL profile";
        :conventions = "Argo reference table 2a";
        :_FillValue = " ";
      float PRES(N_PROF=632, N_LEVELS=71);
        :long_name = "SEA PRESSURE";
        :_FillValue = 99999.0f; // float
        :units = "decibar";
        :valid_min = 0.0f; // float
        :valid_max = 12000.0f; // float
        :comment = "In situ measurement, sea surface = 0";
        :C_format = "%7.1f";
        :FORTRAN_format = "F7.1";
        :resolution = 0.1f; // float
      char PRES_QC(N_PROF=632, N_LEVELS=71);
        :long_name = "quality flag";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      float PRES_ADJUSTED(N_PROF=632, N_LEVELS=71);
        :long_name = "SEA PRESSURE";
        :_FillValue = 99999.0f; // float
        :units = "decibar";
        :valid_min = 0.0f; // float
        :valid_max = 12000.0f; // float
        :comment = "In situ measurement, sea surface = 0";
        :C_format = "%7.1f";
        :FORTRAN_format = "F7.1";
        :resolution = 0.1f; // float
      char PRES_ADJUSTED_QC(N_PROF=632, N_LEVELS=71);
        :long_name = "quality flag";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      float PRES_ADJUSTED_ERROR(N_PROF=632, N_LEVELS=71);
        :long_name = "SEA PRESSURE";
        :_FillValue = 99999.0f; // float
        :units = "decibar";
        :comment = "Contains the error on the adjusted values as determined by the delayed mode QC process.";
        :C_format = "%7.1f";
        :FORTRAN_format = "F7.1";
        :resolution = 0.1f; // float
      float TEMP(N_PROF=632, N_LEVELS=71);
        :long_name = "SEA TEMPERATURE IN SITU ITS-90 SCALE";
        :_FillValue = 99999.0f; // float
        :units = "degree_Celsius";
        :valid_min = -2.0f; // float
        :valid_max = 40.0f; // float
        :comment = "In situ measurement";
        :C_format = "%9.3f";
        :FORTRAN_format = "F9.3";
        :resolution = 0.001f; // float
      char TEMP_QC(N_PROF=632, N_LEVELS=71);
        :long_name = "quality flag";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      float TEMP_ADJUSTED(N_PROF=632, N_LEVELS=71);
        :long_name = "SEA TEMPERATURE IN SITU ITS-90 SCALE";
        :_FillValue = 99999.0f; // float
        :units = "degree_Celsius";
        :valid_min = -2.0f; // float
        :valid_max = 40.0f; // float
        :comment = "In situ measurement";
        :C_format = "%9.3f";
        :FORTRAN_format = "F9.3";
        :resolution = 0.001f; // float
      char TEMP_ADJUSTED_QC(N_PROF=632, N_LEVELS=71);
        :long_name = "quality flag";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      float TEMP_ADJUSTED_ERROR(N_PROF=632, N_LEVELS=71);
        :long_name = "SEA TEMPERATURE IN SITU ITS-90 SCALE";
        :_FillValue = 99999.0f; // float
        :units = "degree_Celsius";
        :comment = "Contains the error on the adjusted values as determined by the delayed mode QC process.";
        :C_format = "%9.3f";
        :FORTRAN_format = "F9.3";
        :resolution = 0.001f; // float
      float PSAL(N_PROF=632, N_LEVELS=71);
        :long_name = "PRACTICAL SALINITY";
        :_FillValue = 99999.0f; // float
        :units = "psu";
        :valid_min = 0.0f; // float
        :valid_max = 42.0f; // float
        :comment = "In situ measurement";
        :C_format = "%9.3f";
        :FORTRAN_format = "F9.3";
        :resolution = 0.001f; // float
      char PSAL_QC(N_PROF=632, N_LEVELS=71);
        :long_name = "quality flag";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      float PSAL_ADJUSTED(N_PROF=632, N_LEVELS=71);
        :long_name = "PRACTICAL SALINITY";
        :_FillValue = 99999.0f; // float
        :units = "psu";
        :valid_min = 0.0f; // float
        :valid_max = 42.0f; // float
        :comment = "In situ measurement";
        :C_format = "%9.3f";
        :FORTRAN_format = "F9.3";
        :resolution = 0.001f; // float
      char PSAL_ADJUSTED_QC(N_PROF=632, N_LEVELS=71);
        :long_name = "quality flag";
        :conventions = "Argo reference table 2";
        :_FillValue = " ";
      float PSAL_ADJUSTED_ERROR(N_PROF=632, N_LEVELS=71);
        :long_name = "PRACTICAL SALINITY";
        :_FillValue = 99999.0f; // float
        :units = "psu";
        :comment = "Contains the error on the adjusted values as determined by the delayed mode QC process.";
        :C_format = "%9.3f";
        :FORTRAN_format = "F9.3";
        :resolution = 0.001f; // float
        */
      if (reallyVerbose)
        String2.log(
            msg
                + " finished. nColumns="
                + nColumns()
                + " nRows="
                + nRows()
                + " TIME="
                + (System.currentTimeMillis() - tTime)
                + "ms");
    } finally {
      try {
        if (nc != null) nc.close();
      } catch (Exception e9) {
      }
    }
  }

  /**
   * Parse the orderByCsv string into an array of strings. If the final string begins with a number
   * (eg. 2days) it is appended to the last field eg, time/2days.
   *
   * @param errorMessage (with a space at the end).
   * @param tOrderByCsv from the query
   * @return an array of strings representing the column names.
   */
  public static String[] parseOrderByColumnNamesCsvString(
      final String errorMessage, final String tOrderByCsv) {
    if ((tOrderByCsv == null || tOrderByCsv.trim().length() == 0))
      throw new SimpleException(errorMessage + "no csv.");
    String[] cols = String2.split(tOrderByCsv, ',');
    // filter out the blanks.
    cols =
        Arrays.stream(cols)
            .filter(value -> value.trim().length() > 0)
            .toArray(size -> new String[size]);
    if (cols.length == 0) throw new SimpleException(errorMessage + "csv.length=0.");

    // support the old format where interval was the last field.
    if (cols.length > 1 && cols[cols.length - 1].trim().matches("^\\d")) {
      cols[cols.length - 2] += "/" + cols[cols.length - 1];
      cols = Arrays.copyOf(cols, cols.length - 1);
    }
    return cols;
  }

  private static String[] splitColNameForRounders(String colName) {
    String split[] = colName.split("/", 2); // split on '/'
    return Arrays.stream(split)
        .map(s -> s.trim()) // remove outer whitespace
        .filter(s -> s.length() > 0)
        .toArray(size -> new String[size]); // discard blanks.
  }

  /**
   * Derive the actual column names by removing the rounding part eg time/1day =&gt; time.
   *
   * @param keyColumnNames
   * @return
   */
  public static String[] deriveActualColumnNames(String[] keyColumnNames) {
    final String[] actualKeyColumnNames = new String[keyColumnNames.length];
    for (int i = 0; i < keyColumnNames.length; i++) {
      actualKeyColumnNames[i] = deriveActualColumnName(keyColumnNames[i]);
    }
    return actualKeyColumnNames;
  }

  public static String deriveActualColumnName(String colName) {
    return splitColNameForRounders(colName)[0];
  }

  private static final Pattern alphaPattern = Pattern.compile("\\p{Alpha}");

  public static Table.Rounder createRounder(final String responsible, final String param) {
    String[] split = splitColNameForRounders(param);
    if (split.length == 1) {
      return (d) -> d; // nothing to be done.
    }
    String str = split[1];
    try {
      str = str.replaceAll("\\s", "");
      String[] parts = str.split(":", 2);
      Matcher alphaMatcher = alphaPattern.matcher(parts[0]);
      if (alphaMatcher.find()) { // eg: 2days.
        if (parts.length == 2) {
          throw new IllegalArgumentException(
              QUERY_ERROR
                  + responsible
                  + " could not parse "
                  + param
                  + ". (Offset not allowed with date intervals.)");
        }
        return createTimeRounder(responsible, str, param);
      } else {
        final double numberOfUnits = Double.parseDouble(parts[0]);
        if (parts.length == 2) {
          final double offset = Double.parseDouble(parts[1]);
          return (d) -> Math.floor((d - offset) / numberOfUnits);
        }
        return (d) -> Math.floor(d / numberOfUnits);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Throwable t) {
      throw new IllegalArgumentException(
          QUERY_ERROR
              + responsible
              + " could not parse "
              + param
              + ". (Format should be variable[/interval[:offset]] )");
    }
  }

  private static Table.Rounder createTimeRounder(
      final String responsible, String str, String param) {
    final double[] numberTimeUnits =
        Calendar2.parseNumberTimeUnits(str); // throws RuntimeException if trouble
    if (numberTimeUnits[0] <= 0 || numberTimeUnits[1] <= 0)
      throw new IllegalArgumentException(
          QUERY_ERROR
              + responsible
              + " could not parse "
              + param
              + ". (numberTimeUnits values must be positive numbers)");
    final double simpleInterval = numberTimeUnits[0] * numberTimeUnits[1];
    final int field =
        numberTimeUnits[1] == 30 * Calendar2.SECONDS_PER_DAY
            ? Calendar2.MONTH
            : numberTimeUnits[1] == 360 * Calendar2.SECONDS_PER_DAY
                ? Calendar2.YEAR
                : // but see getYear below
                Integer.MAX_VALUE;
    final int intNumber = Math2.roundToInt(numberTimeUnits[0]); // used for Month and Year
    if (field != Integer.MAX_VALUE && (intNumber < 1 || intNumber != numberTimeUnits[0]))
      throw new IllegalArgumentException(
          QUERY_ERROR
              + responsible
              + " could not parse "
              + param
              + ". (The number of months or years must be a positive integer.)");
    if (field == Calendar2.MONTH && (intNumber == 5 || intNumber > 6))
      throw new IllegalArgumentException(
          QUERY_ERROR
              + responsible
              + " could not parse "
              + param
              + ". (The number of months must be one of 1,2,3,4, or 6.)");

    if (field == Integer.MAX_VALUE) {
      return (d) -> d - d % simpleInterval;
    } else {
      return (d) -> {
        GregorianCalendar gc = Calendar2.epochSecondsToGc(d);
        Calendar2.clearSmallerFields(gc, field);
        while ((field == Calendar2.YEAR ? Calendar2.getYear(gc) : gc.get(field)) % intNumber != 0)
          gc.add(field, -1);
        return Calendar2.gcToEpochSeconds(gc);
      };
    }
  }
}
