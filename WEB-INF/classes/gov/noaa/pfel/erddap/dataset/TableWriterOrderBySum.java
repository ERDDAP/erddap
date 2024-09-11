/*
 * TableWriterOrderBySum Copyright 2018, NOAA & Irish Marine Institute
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.SimpleException;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * TableWriterOrderBySum provides a way summarize the response table's rows, and just keep the row
 * where the values hold the sum. For example, you could use orderBy(\"stationID,time/1day\") to get
 * the sum daily values for each station.
 *
 * <p>This doesn't include _FillValues or missing_values in the calculations.
 *
 * <p>TableWriterOrderBySum is derived from TableWriterOrderByMean
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2018-09-11
 * @author Rob Fuller (rob.fuller@marine.ie) 2018-09-11
 * @author Adam Leadbetter (adam.leadbetter@marine.ie) 2018-09-11
 * @author Marco Alba (marco.alba@ettsolutions.com) 2021-09-10
 */
public class TableWriterOrderBySum extends TableWriterAll {

  // set by constructor
  protected final TableWriter otherTableWriter;
  public String orderBy[];
  // maintains count of the number of values in sum
  protected final Map<String, int[]> counts = new HashMap<String, int[]>();
  protected final Map<String, Integer> rowmap = new HashMap<String, Integer>();

  protected Attributes oColumnAtts[] = null; // from incoming table or edd

  private int[] keyCols;
  private String cellMethods = null;
  private BitSet isKeyCol;
  private BitSet cannotSumCol;
  private BitSet wasDecimalCol;
  private int timeCol = -1;
  private boolean configured = false;
  private Table sumsTable;
  private final Map<String, Table.Rounder> rounders = new HashMap<String, Table.Rounder>();

  /**
   * The constructor.
   *
   * @param tLanguage the index of the selected language
   * @param tDir a private cache directory for storing the intermediate files, usually
   *     cacheDirectory(datasetID)
   * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
   *     A random number will be added to it for safety.
   * @param tOtherTableWriter the tableWriter that will receive the unique rows found by this
   *     tableWriter.
   * @param tOrderByCsv the names of the columns to sort by (most to least important)
   */
  public TableWriterOrderBySum(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      String tDir,
      String tFileNameNoExt,
      TableWriter tOtherTableWriter,
      String tOrderByCsv) {

    super(tLanguage, tEdd, tNewHistory, tDir, tFileNameNoExt);
    otherTableWriter = tOtherTableWriter;
    final String[] cols =
        Table.parseOrderByColumnNamesCsvString(
            EDStatic.simpleBilingual(language, EDStatic.queryErrorAr) + "orderBySum: ",
            tOrderByCsv);
    orderBy = new String[cols.length];

    for (int col = 0; col < cols.length; col++) {
      orderBy[col] = Table.deriveActualColumnName(cols[col]);
      if (orderBy[col].equals(cols[col])) {
        if (cols[col].equals("time")) cellMethods = "time: sum";
      } else {
        rounders.put(orderBy[col], Table.createRounder("orderBySum", cols[col]));
        Matcher m = Calendar2.TIME_N_UNITS_PATTERN.matcher(cols[col]);
        if (m.matches())
          cellMethods =
              "time: sum (interval: "
                  + m.group(1)
                  + " "
                  + m.group(2)
                  + ")"; // hard to include other info
      }
    }
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation converts them to
   *     NaNs for processing, then back to destinationMV and FV when finished.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    int nRows = table.nRows();
    if (nRows == 0) return;
    if (!configured) {
      configured = configure(table); // throws Exception
    }

    int nCols = table.nColumns();
    for (int col = 0; col < nCols; col++) {
      // table atts may not have info so get from columnAttributes[].
      Attributes atts = oColumnAtts[col];
      PrimitiveArray fv = atts.get("_FillValue");
      PrimitiveArray mv = atts.get("missing_value");
      if (fv != null || mv != null)
        table
            .getColumn(col)
            .convertToStandardMissingValues(
                fv == null ? null : fv.getString(0), mv == null ? null : mv.getString(0));
      // note that metadata hasn't been changed yet
    }

    StringBuilder sbKey = new StringBuilder();
    double[] roundedValue = new double[nCols];
    BitSet isRounded = new BitSet(nCols);
    ROW:
    for (int row = 0; row < nRows; row++) {
      sbKey.setLength(0);
      for (int i = 0; i < keyCols.length; i++) {
        int col = keyCols[i];
        PrimitiveArray column = table.getColumn(col);
        String columnName = table.getColumnName(col);
        if (column.isFloatingPointType() || column.isIntegerType()) {
          double value = column.getNiceDouble(row);
          if (rounders.containsKey(columnName)) {
            if (Double.isNaN(value)) {
              // No value, cannot group by this...
              continue ROW;
            }
            value = rounders.get(columnName).round(value);
            isRounded.set(col);
            roundedValue[col] = value;
          }
          sbKey.append(value);
        } else {
          sbKey.append(column.getString(row));
        }
        sbKey.append(":");
      }
      String key = sbKey.toString();
      int[] tCounts = counts.get(key);
      if (tCounts == null) {
        tCounts = new int[nCols];
        int idx = counts.size();
        counts.put(key, tCounts);
        for (int col = 0; col < nCols; col++) {
          PrimitiveArray column = table.getColumn(col);
          String value =
              (column.isFloatingPointType() || column.isIntegerType())
                  ? ""
                  : // numeric types should start with NaN
                  column.getRawString(row);
          // if (table.getColumnName(col).equals("wd"))  String2.log(">>wd addString=" + value);
          sumsTable.getColumn(col).addString(value);
        }
        rowmap.put(key, idx);
      }
      int idx = rowmap.get(key);
      for (int col = 0; col < nCols; col++) {
        PrimitiveArray column = table.getColumn(col);
        if (cannotSumCol.get(col)) {
          // Keep the value only if all rows are the same.
          String value = column.getRawString(row);
          String prev = sumsTable.getColumn(col).getRawString(idx);
          if (!("".equals(prev) || prev.equals(value))) {
            sumsTable.setStringData(col, idx, "");
          }
          continue;
        }
        if (!(column.isFloatingPointType() || column.isIntegerType())) {
          sumsTable.setStringData(col, idx, column.getRawString(row));
          continue;
        }
        double value = isRounded.get(col) ? roundedValue[col] : table.getNiceDoubleData(col, row);
        // if (table.getColumnName(col).equals("wd")) String2.log(">>wd value=" + value + "\n" +
        // sumsTable.dataToString());
        if (Double.isNaN(value)) continue;
        tCounts[col] += 1;
        if (tCounts[col] == 1) {
          sumsTable.setDoubleData(col, idx, value);
          continue;
        }
        double sum = sumsTable.getDoubleData(col, idx);
        if (isTimeColumn(sumsTable, col)) sum += (value - sum) / tCounts[col];
        else sum += value;

        sumsTable.setDoubleData(col, idx, sum);
      }
    }
  }

  private boolean isTimeColumn(Table table, int col) {
    String units = table.columnAttributes(col).getString("units");
    return "time".equals(table.getColumnName(col)) || EDV.TIME_UNITS.equals(units);
  }

  /** Find the key columns and possibly the time column. */
  private boolean configure(Table table) throws SimpleException {
    int nKeyCols = orderBy.length;
    int ncols = table.nColumns();
    ArrayList<Integer> tKeyCols = new ArrayList<Integer>();
    isKeyCol = new BitSet(ncols);
    cannotSumCol = new BitSet(ncols);
    // degreesCol     = new BitSet(ncols);
    // degreesTrueCol = new BitSet(ncols);
    wasDecimalCol = new BitSet(ncols);
    for (int k = 0; k < nKeyCols; k++) {
      int col = table.findColumnNumber(orderBy[k]);
      if (col < 0)
        throw new SimpleException(
            EDStatic.bilingual(language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderBySumAr)
                + (language == 0 ? " " : "\n")
                + "Unknown orderBy column="
                + orderBy[k]
                + ".");
      tKeyCols.add(col);
      isKeyCol.set(col);
    }
    rounders
        .keySet()
        .forEach(
            (columnName) -> {
              PrimitiveArray column = table.getColumn(columnName);
              if (!(column.isIntegerType() || column.isFloatingPointType())) {
                throw new SimpleException(
                    EDStatic.bilingual(
                            language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderBySumAr)
                        + (language == 0 ? " " : "\n")
                        + "Cannot group numerically for column="
                        + columnName
                        + ".");
              }
            });
    keyCols = tKeyCols.stream().mapToInt(i -> i).toArray();
    String colName[] = new String[ncols];
    String dataType[] = new String[ncols];
    oColumnAtts = new Attributes[ncols];
    for (int col = 0; col < ncols; col++) {
      colName[col] = table.getColumnName(col);

      // get oColumnAtts
      if (edd == null) {
        oColumnAtts[col] = table.columnAttributes(col);
      } else {
        EDV edv = edd.findDataVariableByDestinationName(colName[col]); // exception if not found
        oColumnAtts[col] = new Attributes(edv.combinedAttributes());
      }

      PrimitiveArray column = table.getColumn(col);
      if (isKeyCol.get(col)) {
        dataType[col] = column.elementTypeString();
      } else {
        if (column.isFloatingPointType()) {
          wasDecimalCol.set(col);
        }
        if (column.isIntegerType() || column.isFloatingPointType()) {
          dataType[col] = "double";
        } else if (col != timeCol) {
          // include this in the output only if a single value
          dataType[col] =
              column.elementType() == PAType.CHAR
                  ? "char"
                  : column.elementType() == PAType.BYTE ? "byte" : "String";
          cannotSumCol.set(col);
        }
      }
    }
    sumsTable = Table.makeEmptyTable(colName, dataType);
    // String2.log(">> make sumsTable colNames=" + String2.toCSVString(colName) + " dataTypes=" +
    // String2.toCSVString(dataType));
    return true;
  }

  /*
   * Sometimes it makes sense to change the column type back to integer,
   * For example year and month. Only do this if all the values are integers.
   */
  private void useIntegersWhereSensible() {
    // Currently disabled. Much debate: I think it is better to always present
    // results as doubles: for consistency. User can look at sum and decide to
    // ceil/floor/round it as desired.
    // Integers also cause problems with unexpected missing values (e.g., 32767)
    // for groups that have no data values.
    /*
    int ncols = sumsTable.nColumns();
    for (int col = 0; col < ncols; col++) {
        if (isKeyCol.get(col))  //never change data type of key columns
            continue;
        PrimitiveArray column = sumsTable.getColumn(col);
        if (wasDecimalCol.get(col) || !column.isFloatingPointType())
            continue;
        if (areAllValuesIntegers(column))
            sumsTable.setColumn(col, PrimitiveArray.factory(PAType.INT, column));
    }
    */
  }

  /**
   * This finishes orderBySum and writes results to otherTableWriter If ignoreFinish=true, nothing
   * will be done.
   *
   * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    if (keyCols != null) {
      useIntegersWhereSensible();
      if (keyCols.length > 0) sumsTable.sort(keyCols);
      super.writeSome(sumsTable);
    }
    super.finish(); // this ensures there is data and thus configured=true

    Table cumulativeTable = cumulativeTable();
    releaseResources();

    // improve metadata
    int nColumns = cumulativeTable.nColumns();
    for (int col = 0; col < nColumns; col++) {
      if (!isKeyCol.get(col)) {
        // convert mv fv to new data types
        PAType tPAType = cumulativeTable.getColumn(col).elementType();
        Attributes atts = cumulativeTable.columnAttributes(col);
        // String2.log(">> colName=" + cumulativeTable.getColumnName(col) + " tPAType=" + tPAType);
        atts.set("_FillValue", PrimitiveArray.factory(tPAType, 1, ""));
        if (cellMethods != null) atts.set("cell_methods", cellMethods);
        atts.remove("cf_role");
        atts.remove("missing_value");
      }
    }

    otherTableWriter.writeAllAndFinish(cumulativeTable);

    // clean up
    sumsTable = null;
    counts.clear();
    rowmap.clear();
  }

  /**
   * If caller has the entire table, use this instead of repeated writeSome() + finish(). This
   * overwrites the superclass method.
   *
   * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void writeAllAndFinish(Table tCumulativeTable) throws Throwable {
    writeSome(tCumulativeTable);
    if (ignoreFinish) return;
    finish();
  }
}
