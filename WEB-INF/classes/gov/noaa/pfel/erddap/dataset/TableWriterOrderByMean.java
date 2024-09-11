/*
 * TableWriterOrderByMean Copyright 2018, NOAA & Irish Marine Institute
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
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
 * TableWriterOrderByMean provides a way summarize the response table's rows, and just keep the row
 * where the values hold the mean. For example, you could use orderBy(\"stationID,time/1day\") to
 * get the mean daily values for each station.
 *
 * <p>This doesn't include _FillValues or missing_values in the calculations.
 *
 * <p>This uses the incremental-averaging algorithm to calculate the means, e.g.,
 * https://math.stackexchange.com/questions/106700/incremental-averageing
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2018-09-11
 * @author Rob Fuller (rob.fuller@marine.ie) 2018-09-11
 * @author Adam Leadbetter (adam.leadbetter@marine.ie) 2018-09-11
 */
public class TableWriterOrderByMean extends TableWriterAll {

  // set by constructor
  protected final TableWriter otherTableWriter;
  public String orderBy[];
  // maintains count of the number of values in average
  protected final Map<String, int[]> counts = new HashMap<String, int[]>();
  protected final Map<String, Integer> rowmap = new HashMap<String, Integer>();
  // used when calculating degree means at the end, one key for each row.
  protected final StringArray keymap = new StringArray();
  protected final Map<String, DegreesAccumulator> degreesMap =
      new HashMap<String, DegreesAccumulator>();

  protected Attributes oColumnAtts[] = null; // from incoming table or edd

  private int[] keyCols;
  private String cellMethods = null;
  private BitSet isKeyCol;
  private BitSet cannotMeanCol;
  private BitSet degreesCol;
  private BitSet degreesTrueCol;
  private BitSet wasDecimalCol;
  private int timeCol = -1;
  private boolean configured = false;
  private Table meansTable;
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
  public TableWriterOrderByMean(
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
            EDStatic.simpleBilingual(language, EDStatic.queryErrorAr) + "orderByMean: ",
            tOrderByCsv);
    orderBy = new String[cols.length];

    for (int col = 0; col < cols.length; col++) {
      orderBy[col] = Table.deriveActualColumnName(cols[col]);
      if (orderBy[col].equals(cols[col])) {
        if (cols[col].equals("time")) cellMethods = "time: mean";
      } else {
        rounders.put(orderBy[col], Table.createRounder("orderByMean", cols[col]));
        Matcher m = Calendar2.TIME_N_UNITS_PATTERN.matcher(cols[col]);
        if (m.matches())
          cellMethods =
              "time: mean (interval: "
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
          String value = column.getRawString(row);
          meansTable.getColumn(col).addString(value);
        }
        rowmap.put(key, idx);
        keymap.add(key);
      }
      int idx = rowmap.get(key);
      for (int col = 0; col < nCols; col++) {
        PrimitiveArray column = table.getColumn(col);
        if (cannotMeanCol.get(col)) {
          // Keep the value only if all rows are the same.
          String value = column.getRawString(row);
          String prev = meansTable.getColumn(col).getRawString(idx);
          if (!("".equals(prev) || prev.equals(value))) {
            meansTable.setStringData(col, idx, "");
          }
          continue;
        }
        if (!(column.isFloatingPointType() || column.isIntegerType())) {
          meansTable.setStringData(col, idx, column.getRawString(row));
          continue;
        }
        double value = isRounded.get(col) ? roundedValue[col] : table.getNiceDoubleData(col, row);
        if (Double.isNaN(value)) {
          continue;
        }
        // String2.log(">> row=" + row + " col=" + col + " val=" + value + " mean=" + mean);
        if (degreesTrueCol.get(col)) {
          accumulateDegreesTrue(key + ":" + col, value);
          continue;
        }
        if (degreesCol.get(col)) {
          accumulateDegrees(key + ":" + col, value);
          continue;
        }
        tCounts[col] += 1;
        if (tCounts[col] == 1) {
          meansTable.setDoubleData(col, idx, value);
          continue;
        }
        double mean = meansTable.getDoubleData(col, idx);
        mean += (value - mean) / tCounts[col];
        meansTable.setDoubleData(col, idx, mean);
      }
    }
  }

  private boolean isDegreeUnitsColumn(Table table, int col) {
    String units = table.columnAttributes(col).getString("units");
    return units != null && EDStatic.angularDegreeUnitsSet.contains(units);
  }

  private boolean isDegreeTrueUnitsColumn(Table table, int col) {
    String units = table.columnAttributes(col).getString("units");
    return units != null && EDStatic.angularDegreeTrueUnitsSet.contains(units);
  }

  /*
   * Find the key columns and possibly the time column.
   */
  private boolean configure(Table table) throws SimpleException {
    int nKeyCols = orderBy.length;
    int ncols = table.nColumns();
    ArrayList<Integer> tKeyCols = new ArrayList<Integer>();
    isKeyCol = new BitSet(ncols);
    cannotMeanCol = new BitSet(ncols);
    degreesCol = new BitSet(ncols);
    degreesTrueCol = new BitSet(ncols);
    wasDecimalCol = new BitSet(ncols);
    for (int k = 0; k < nKeyCols; k++) {
      int col = table.findColumnNumber(orderBy[k]);
      if (col < 0)
        throw new SimpleException(
            EDStatic.bilingual(language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderByMeanAr)
                + (language == 0 ? " " : "\n")
                + "unknown orderBy column="
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
                            language, EDStatic.queryErrorAr, EDStatic.queryErrorOrderByMeanAr)
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
        if (isDegreeTrueUnitsColumn(table, col)) {
          degreesTrueCol.set(col);
        } else if (isDegreeUnitsColumn(table, col)) {
          degreesCol.set(col);
        }
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
          cannotMeanCol.set(col);
        }
      }
    }
    meansTable = Table.makeEmptyTable(colName, dataType);
    return true;
  }

  /*
   * Sometimes it makes sense to change the column type back to integer,
   * For example year and month. Only do this if all the values are integers.
   */
  private void useIntegersWhereSensible() {
    // Currently disabled. Much debate: I think it is better to always present
    // results as doubles: for consistency. Use can look at mean and decide to
    // ceil/floor/round it as desired.
    // Integers also cause problems with unexpected missing values (e.g., 32767)
    // for groups that have no data values.
    /*
    int ncols = meansTable.nColumns();
    for (int col = 0; col < ncols; col++) {
        if (isKeyCol.get(col))  //never change data type of key columns
            continue;
        PrimitiveArray column = meansTable.getColumn(col);
        if (wasDecimalCol.get(col) || !column.isFloatingPointType())
            continue;
        if (areAllValuesIntegers(column))
            meansTable.setColumn(col, PrimitiveArray.factory(PAType.INT, column));
    }
    */
  }

  private void accumulateDegrees(String key, double value) {
    DegreesAccumulator accum = degreesMap.get(key);
    if (accum == null) {
      accum = new DegreesAccumulator(false); // not degrees true
      degreesMap.put(key, accum);
    }
    // String2.log(">> accumulateDegrees " + key + " value=" + value);
    accum.add(value);
  }

  private void accumulateDegreesTrue(String key, double value) {
    DegreesAccumulator accum = degreesMap.get(key);
    if (accum == null) {
      accum = new DegreesAccumulator(true); // is degrees true
      degreesMap.put(key, accum);
    }
    // String2.log(">> accumulateDegrees " + key + " value=" + value);
    accum.add(value);
  }

  private void calculateDegreeMeans() {
    if (meansTable == null || (degreesCol.isEmpty() && degreesTrueCol.isEmpty())) {
      return;
    }
    int ncols = meansTable.nColumns();
    for (int col = 0; col < ncols; col++) {
      if (degreesCol.get(col) || degreesTrueCol.get(col)) {
        int nRows = meansTable.nRows();
        for (int row = 0; row < nRows; row++) {
          String key = keymap.get(row) + ":" + col;
          DegreesAccumulator accum = degreesMap.get(key); // will be null if 0 values for that group
          meansTable.setDoubleData(col, row, accum == null ? Double.NaN : accum.getMean());
          // String2.log(">> " + key + " row=" + row + (accum == null? " null" : " mean=" +
          // accum.getMean()));
          // if (accum != null)
          //    meansTable.setDoubleData(col, row, accum.getMean());
        }
      }
    }
  }

  private static class DegreesAccumulator {
    boolean isDegreesTrue;
    boolean allSame = true;
    double deg = Double.NaN; // the value if allSame
    double meanx = Double.NaN;
    double meany = Double.NaN;
    int count = 0;

    // constructor
    DegreesAccumulator(boolean tIsDegreesTrue) {
      isDegreesTrue = tIsDegreesTrue;
    }

    void add(double angleDegrees) {
      count++;
      double angleR = Math.toRadians(angleDegrees);
      if (count == 1) {
        deg = angleDegrees;
        meanx = Math.cos(angleR);
        meany = Math.sin(angleR);
      } else {
        if (allSame && angleDegrees != deg) allSame = false;
        meanx += (Math.cos(angleR) - meanx) / count;
        meany += (Math.sin(angleR) - meany) / count;
      }
    }

    public double getMean() {
      if (count == 0) return Double.NaN;
      double d = allSame ? deg : Math.toDegrees(Math.atan2(meany, meanx));
      return isDegreesTrue ? Math2.angle0360(d) : Math2.anglePM180(d);
    }
  }

  /**
   * This finishes orderByMean and writes results to otherTableWriter If ignoreFinish=true, nothing
   * will be done.
   *
   * @throws Throwable if trouble (e.g., EDStatic.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    if (keyCols != null) {
      calculateDegreeMeans();
      useIntegersWhereSensible();
      if (keyCols.length > 0) meansTable.sort(keyCols);
      super.writeSome(meansTable);
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
    meansTable = null;
    degreesMap.clear();
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
