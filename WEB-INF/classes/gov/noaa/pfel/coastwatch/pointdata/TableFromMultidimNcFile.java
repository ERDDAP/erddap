package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.array.ULongArray;
import com.cohort.util.Math2;
import com.cohort.util.String2;
import com.cohort.util.Test;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class TableFromMultidimNcFile {

  private Table table;
  private VarData cachedVarData[];
  private boolean haveConstraints;
  private String warningInMethod;
  private HashSet<Dimension> notStringLengthDims;
  private Dimension tDimsAs[][];
  private NetcdfFile ncFile;
  private StringArray loadVarNames;
  private int standardizeWhat;
  private Attributes gridMappingAtts = null;

  private class VarData {
    public PrimitiveArray pa;
    public Attributes atts;
    public List<Dimension> dims;
    public int nDims;
    public boolean isCharArray;

    private void loadDims(TableFromMultidimNcFile tableMultidim, Variable tVar) {
      dims = tVar.getDimensions();
      isCharArray =
          tVar.getDataType() == DataType.CHAR
              && dims.size() > 0
              && !tableMultidim.notStringLengthDims.contains(dims.get(dims.size() - 1));
      nDims = dims.size() - (isCharArray ? 1 : 0);
    }

    private void loadArrayAndAttributes(TableFromMultidimNcFile tableMultidim, Variable tVar)
        throws Exception {
      pa = NcHelper.getPrimitiveArray(tVar, isCharArray);
      if (pa instanceof StringArray t) {
        t.trimEndAll();
      }
      atts = new Attributes();
      NcHelper.getVariableAttributes(tVar, atts);
      pa = atts.standardizeVariable(tableMultidim.standardizeWhat, tVar.getFullName(), pa);
    }

    public static VarData dimsFromVariable(TableFromMultidimNcFile tableMultidim, Variable tVar) {
      VarData data = tableMultidim.new VarData();
      data.loadDims(tableMultidim, tVar);
      return data;
    }

    public static VarData fromVariableIfDimsMatch(
        TableFromMultidimNcFile tableMultidim, Variable tVar, VarData other, int nd0)
        throws Exception {
      int index = tableMultidim.loadVarNames.indexOf(tVar.getFullName());
      if (index > -1 && tableMultidim.cachedVarData[index] != null) {
        VarData data = tableMultidim.cachedVarData[index];
        if (!tableMultidim.doDimsMatch(nd0, data.nDims, data.dims, other.nDims, other.dims)) {
          return null;
        }
        return data;
      }
      VarData data = tableMultidim.new VarData();
      data.loadDims(tableMultidim, tVar);

      if (!tableMultidim.doDimsMatch(nd0, data.nDims, data.dims, other.nDims, other.dims)) {
        return null;
      }
      data.loadArrayAndAttributes(tableMultidim, tVar);
      if (index > -1) {
        tableMultidim.cachedVarData[index] = data;
      }
      return data;
    }

    public static VarData fromVariable(TableFromMultidimNcFile tableMultidim, Variable tVar)
        throws Exception {
      int index = tableMultidim.loadVarNames.indexOf(tVar.getFullName());
      if (index > -1 && tableMultidim.cachedVarData[index] != null) {
        return tableMultidim.cachedVarData[index];
      }
      VarData data = tableMultidim.new VarData();
      data.loadDims(tableMultidim, tVar);
      data.loadArrayAndAttributes(tableMultidim, tVar);
      if (index > -1) {
        tableMultidim.cachedVarData[index] = data;
      }
      return data;
    }
  }

  public TableFromMultidimNcFile(Table table) {
    this.table = table;
  }

  /**
   * This reads and flattens a group of variables which share dimensions from a multidimensional .nc
   * file. (A new alternative to readNDNc().) One difference between using this and readNcCF: this
   * doesn't require/expect that the file follows the nc CF DSG MA standard. <br>
   * This does not unpack the values or convert to standardMissingValues. <br>
   * For strings, this always calls String2.trimEnd(s)
   *
   * @param fullName This may be a local file name, an "http:" address of a .nc file, an .ncml file
   *     (which must end with ".ncml"), or an opendap url.
   *     <p>If the fullName is an http address, the name needs to start with "http://" or "https://"
   *     (upper or lower case) and the server needs to support "byte ranges" (see
   *     ucar.nc2.NetcdfFile documentation). But this is very slow, so not recommended.
   * @param loadVarNames If loadVarNames is specified, those variables will be loaded. If
   *     loadVarNames isn't specified, this method reads vars which use the specified loadDimNames
   *     and scalar vars. <br>
   *     If a specified var isn't in the file, there won't be a column in the results table for it
   *     and it isn't an error.
   * @param loadDimNames. If loadVarNames is specified, this is ignored. If loadDimNames is used,
   *     all variables using any of these dimensions (and dimension-less variables) will be loaded,
   *     plus all scalar vars. Don't include string-length dimensions. Just include the last
   *     treatDimensionsAs dimension (if any). Almost always, there will be 1+ variables which use
   *     all of these dimensions. If a given dimension isn't it the file, it is removed from the
   *     list. If loadDimNames isn't specified (or size=0), this method finds the var which uses the
   *     most dimensions, and uses for loadDimNames. So if you want to get just the scalar vars,
   *     request a nonexistent dimension (e.g., ZZTOP).
   * @param treatDimensionsAs Lists of dimension names that should be treated as another dimension
   *     (the last in each list). Within a list, all dimensions that are in the file must be the
   *     same length. E.g. "Lat,Lon,Time" says to treat Lat and Lon as if they were Time.
   * @param getMetadata if true, global and variable metadata is read
   * @param standardizeWhat see Attributes.unpackVariable's standardizeWhat
   * @param removeMVRows This removes any block of rows at the end of a group where all the values
   *     are missing_value, _FillValue, or the CoHort ...Array native missing value (or char=#32 for
   *     CharArrays). This is for the CF DSG Multidimensional Array file type and similar files. If
   *     true, this does the proper test and so always loads all the max dim variables, so it may
   *     take extra time.
   * @param conVars the names of the constraint variables. May be null. It is up to this method how
   *     much they will be used. Currently, the constraints are just used for *quick* tests to see
   *     if the file has no matching data. If a conVar isn't in the loadVarNames (provided or
   *     derived), then the constraint isn't used. If standardizeWhat != 0, the constraints are
   *     applied to the unpacked variables.
   * @param conOps the operators for the constraints. All ERDDAP ops are supported. May be null.
   * @param conVals the values of the constraints. May be null.
   * @throws Exception if unexpected trouble. But if none of the specified loadVariableNames are
   *     present or a requested dimension's size=0, it is not an error and it returns an empty
   *     table.
   */
  public void readMultidimNc(
      String fullName,
      StringArray loadVarNames,
      StringArray loadDimNames,
      String treatDimensionsAs[][], // will be null if not used
      boolean
          getMetadata, // before 2016-11-29, this had a boolean trimStrings parameter, now it always
      // trimEnd's all strings
      int standardizeWhat,
      boolean removeMVRows,
      StringArray conVars,
      StringArray conOps,
      StringArray conVals)
      throws Exception {

    // clear the table
    this.table.clear();
    if (loadVarNames == null) loadVarNames = new StringArray();
    if (loadDimNames == null) loadDimNames = new StringArray();
    if (standardizeWhat != 0 || removeMVRows) getMetadata = true;
    warningInMethod = "TableFromMultidimNcFile.readMultidimNc read " + fullName + ":\n";
    haveConstraints =
        conVars != null
            && conVars.size() > 0
            && conOps != null
            && conOps.size() == conVars.size()
            && conVals != null
            && conVals.size() == conVars.size();
    if (treatDimensionsAs == null || treatDimensionsAs.length == 0) treatDimensionsAs = null;
    int nd0 = treatDimensionsAs == null ? 0 : treatDimensionsAs.length;
    if (nd0 > 0) {
      validateTreatDimensionsAs(treatDimensionsAs, nd0);
    }
    this.loadVarNames = loadVarNames;
    this.standardizeWhat = standardizeWhat;

    // read the file
    ncFile = NcHelper.openFile(fullName);

    try {
      // load the global metadata
      if (getMetadata)
        NcHelper.getGroupAttributes(ncFile.getRootGroup(), this.table.globalAttributes());

      tDimsAs = processTreatDimensionsAs(treatDimensionsAs, nd0);

      List<Variable> allVars = ncFile.getVariables();
      int nAllVars = allVars.size();

      notStringLengthDims = findNonStringLengthDims(allVars, nAllVars);

      // *** first half: make loadVars
      ArrayList<Variable> loadVars = new ArrayList<>(); // which we will actually load
      ArrayList<Dimension> loadDims = new ArrayList<>(); // which we actually need
      loadVars(loadVarNames, loadDimNames, loadVars, loadDims, nd0, nAllVars, allVars);

      // loadVars is known and only uses loadDims
      // loadDims is known and only has dims used by loadVars
      // if (debugMode) String2.log(
      // ">> loadVars=" + loadVarNames +
      // "\n>> loadDims=" + loadDimNames);
      int nLoadVars = loadVars.size();

      List<Pair<VarData, BitSet>> varToKeep = new ArrayList<>();

      // *** quick reject file? (by testing constraints on small (scalar and 1D) vars)
      // maintain separate keep bitsets for each 1D var
      // (and for scalars) so that the constraints are cumulative for each dimension.
      this.cachedVarData = new VarData[nLoadVars];
      if (haveConstraints) {
        int nCons = conVars.size();

        // go through the load vars looking for 0D or 1D vars that have constraints
        for (int v = 0; v < nLoadVars; v++) {
          // is there at least 1 constraint of this var?
          String varName = loadVarNames.get(v);
          int con1 = conVars.indexOf(varName);
          if (con1 < 0) continue;

          // is this a 0D or 1D var?
          Variable tVar = loadVars.get(v);
          VarData data = new VarData();
          data.loadDims(this, tVar);
          if (data.nDims > 1) {
            continue;
          }
          data.loadArrayAndAttributes(this, tVar);
          cachedVarData[v] = data;

          BitSet keep = getKeepForVar(data, nd0, varToKeep);
          // test constraints
          for (int con = con1; con < nCons; con++) {
            if (!conVars.get(con).equals(varName)) continue;
            if (data.pa.applyConstraint(
                    false, // less precise, so more likely to pass the test
                    keep,
                    conOps.get(con),
                    conVals.get(con))
                == 0) {
              // if (verbose) String2.log(warningInMethod +
              // "Returning an empty table because var=" + varName +
              // " failed its constraints, including " +
              // conOps.get(con) + conVals.get(con) +
              // ". time=" + (System.currentTimeMillis() - time) + "ms");
              return;
            }
          }
        }
      }
      // *** second half: load the loadVars

      // find vars with all of the loadDims
      // If loadDims size=0, this finds scalar vars
      BitSet loaded = new BitSet(nLoadVars); // all false
      int shape[] = new int[loadDims.size()];
      // find first var with all of the load dims
      VarData firstVar = null;
      for (int v = 0; v < nLoadVars; v++) {
        Variable tVar = loadVars.get(v);
        firstVar = new VarData();
        firstVar.loadDims(this, tVar);
        if (firstVar.nDims != loadDims.size()) {
          continue;
        }
        if (this.table.nColumns() == 0) {
          // first var with all dims: set loadDims to be in that order
          for (int d = 0; d < firstVar.nDims; d++) {
            Dimension dim = firstVar.dims.get(d);
            dim = convertDimension(nd0, dim);
            loadDims.set(d, dim); // perhaps change loadDims to different order
            loadDimNames.set(d, dim.getName());
            shape[d] = dim.getLength();
            if (shape[d] == 0) {
              // if (verbose) String2.log(warningInMethod +
              // "Returning an empty table because dim=" + dim.getName() +
              // "'s length=0! " +
              // "time=" + (System.currentTimeMillis() - time));
              return;
            }
          }
        }
        if (cachedVarData[v] != null) {
          firstVar = cachedVarData[v];
        } else {
          // yes, load this var, it has all of the dimensions in the expected order
          firstVar.loadArrayAndAttributes(this, tVar);
          // knownPAs[v] = null;
          // knownAtts[v] = null;
          cachedVarData[v] = firstVar;
        }
        addColumnToTable(getMetadata, loaded, firstVar, v, tVar, this.table);
        break;
      }
      if (firstVar != null) {
        loadDimMatchedVars(
            loadVarNames,
            standardizeWhat,
            nd0,
            loadVars,
            loadDims,
            nLoadVars,
            loaded,
            this.table,
            firstVar,
            getMetadata);
      }
      // if (debugMode) String2.log(Math2.memoryString() + "\n" +
      // ">> this table after load varsWithAllDims:\n" +
      // dataToString(5));

      // if loadDims size is 0, we're done because all scalars have been read
      if (loadDims.size() == 0) {
        if (haveConstraints) {
          BitSet keep = new BitSet();
          keep.set(0, this.table.nRows()); // should be just 1 row, all true
          int nAfter = this.table.tryToApplyConstraints(-1, conVars, conOps, conVals, keep);
          if (nAfter == 0) {
            // if (verbose) String2.log(warningInMethod +
            // "Returning an empty table after applying constraints to scalars. " +
            // "time=" + (System.currentTimeMillis() - time));
            this.table.clear();
            return;
          } // else: no need to justKeep() because there is 1 row and it is valid
        }
        return; // empty table if no scalars
      }

      // make a table with index columns for all indices
      if (this.table.nColumns() == 0) {
        // no vars have all loadDims
        // if (debugMode) String2.log(Math2.memoryString() + "\n" +
        // ">> no vars have all loadDims");
        for (int d = 0; d < loadDims.size(); d++) {
          Dimension dim = loadDims.get(d);
          shape[d] = dim.getLength();
          if (shape[d] == 0) {
            // if (verbose) String2.log(warningInMethod +
            // "Returning an empty table because dim=" + dim.getName() +
            // "'s length=0! " +
            // "time=" + (System.currentTimeMillis() - time));
            return;
          }
        }
      }
      Table allIndicesTable = new Table();
      allIndicesTable.addIndexColumns(shape);
      // if (debugMode) String2.log(Math2.memoryString() + "\n" +
      // ">> allIndicesTable=" +
      // allIndicesTable.dataToString(5));

      // apply constraints
      int onRows = this.table.nRows();
      BitSet keep;
      if (firstVar != null) {
        keep = getKeepForVar(firstVar, nd0, varToKeep);
      } else {
        keep = new BitSet();
      }
      keep.set(0, onRows); // all true
      // *** removeMVRows
      if (removeMVRows && this.table.nColumns() > 0) {
        // ensure all vars that use all loadDims are loaded
        int onCols = this.table.nColumns();
        for (int v = 0; v < nAllVars; v++) {
          Variable tVar = allVars.get(v);
          if (this.table.findColumnNumber(tVar.getFullName()) >= 0) {
            continue;
          } // already in the table
          VarData data = new VarData();
          data.loadDims(this, tVar);
          if (!doDimsMatch(nd0, data.nDims, data.dims, loadDims.size(), loadDims)) {
            continue;
          }
          // yes, load this var TEMPORARILY, it has all of the dimensions in the expected
          // order
          // don't use knownPAs here: different vars and different v's.
          data.loadArrayAndAttributes(this, tVar);
          this.table.addColumn(this.table.nColumns(), tVar.getFullName(), data.pa, data.atts);
        }

        // move all the allIndices columns into the main table
        int nLoadDims = loadDims.size();
        for (int d = 0; d < nLoadDims; d++)
          this.table.addColumn(
              d,
              allIndicesTable.getColumnName(d),
              allIndicesTable.getColumn(d),
              allIndicesTable.columnAttributes(d));
        int nColumns = this.table.nColumns(); // including indicesColumns
        if (onRows != this.table.nRows()) {
          throw new Exception("Row count mismatch, can't do contraints first");
        }

        // gather the missing_value and _FillValue values for each column
        boolean isDouble[] = new boolean[nColumns];
        boolean isULong[] = new boolean[nColumns];
        boolean isLong[] = new boolean[nColumns];
        boolean isChar[] = new boolean[nColumns];
        double doubleMvs[] = new double[nColumns];
        double doubleFvs[] = new double[nColumns];
        BigInteger ulongMvs[] = new BigInteger[nColumns];
        BigInteger ulongFvs[] = new BigInteger[nColumns];
        long longMvs[] = new long[nColumns];
        long longFvs[] = new long[nColumns];
        for (int c = nLoadDims; c < nColumns; c++) {
          PrimitiveArray pa = this.table.columns.get(c);
          isDouble[c] = pa instanceof FloatArray || pa instanceof DoubleArray;
          isULong[c] = pa instanceof ULongArray;
          isLong[c] = pa.isIntegerType() && !(pa instanceof ULongArray);
          isChar[c] = pa instanceof CharArray;
          if (isDouble[c]) {
            doubleMvs[c] = this.table.columnAttributes(c).getDouble("missing_value");
            doubleFvs[c] = this.table.columnAttributes(c).getDouble("_FillValue");
          } else if (isULong[c]) {
            ulongMvs[c] = this.table.columnAttributes(c).getULong("missing_value");
            ulongFvs[c] = this.table.columnAttributes(c).getULong("_FillValue");
          } else if (isLong[c]) {
            longMvs[c] = this.table.columnAttributes(c).getLong("missing_value");
            longFvs[c] = this.table.columnAttributes(c).getLong("_FillValue");
          }
        }

        // walk backwards. Work within each cycle of the last dim.

        PrimitiveArray lastDimCol = this.table.columns.get(nLoadDims - 1);
        for (int row = onRows - 1; row >= 0; row--) {
          boolean hasData = false;
          for (int c = nLoadDims; c < nColumns; c++) {
            // if (debugMode && row > onRows - 200)
            // String2.log(">> row=" + row + " col=" + c + " " +
            // (isDouble[c]) + " " + (isLong[c]) + " " + (isChar[c]) + " " +
            // " val=" + columns.get(c).getString(row));
            if (isDouble[c]) {
              double d = this.table.columns.get(c).getDouble(row);
              if (Double.isNaN(d)
                  || Math2.almostEqual(5, d, doubleMvs[c])
                  || Math2.almostEqual(5, d, doubleFvs[c])) {
              } else {
                hasData = true;
                break;
              }
            } else if (isULong[c]) {
              BigInteger ul = this.table.columns.get(c).getULong(row);
              if (ul.equals(ULongArray.MAX_VALUE)
                  || // trouble: should test maxIsMV
                  ul.equals(ulongMvs[c])
                  || ul.equals(ulongFvs[c])) {
              } else {
                hasData = true;
                break;
              }
            } else if (isLong[c]) {
              long tl = this.table.columns.get(c).getLong(row);
              if (tl == Long.MAX_VALUE
                  || // trouble: should test maxIsMV
                  tl == longMvs[c]
                  || tl == longFvs[c]) {
              } else {
                hasData = true;
                break;
              }
            } else if (isChar[c]) {
              int tc = this.table.columns.get(c).getInt(row);
              if (tc == 0 || tc == 32 || tc == Integer.MAX_VALUE) { // trouble: should test maxIsMV
              } else {
                hasData = true;
                break;
              }
            } else {
              // nc allows strings to be 0-terminated or padded with spaces, so always trimEnd
              String s = String2.trimEnd(this.table.columns.get(c).getString(row));
              if (s.length() > 0) {
                hasData = true;
                break;
              }
            }
          }
          if (hasData) {
            // jump to next group
            while (lastDimCol.getInt(row) > 0) row--; // the loop's row-- will get to next group
          } else {
            keep.clear(row);
          }
        }
        // if (debugMode) { String2.log(">> removeMVRows nRows before=" + onRows +
        // " after=" + keep.cardinality());
        // //one time debugging:
        // if (false) {
        // PrimitiveArray pa = getColumn(nLoadDims);
        // for (int row = 0; row < onRows; row++) {
        // if (keep.get(row) && pa.getDouble(row) == -99999)
        // String2.log(">> remaining row with mv:\n" + //in debugMode
        // dataToString(row-1, row+2));
        // }
        // }
        // }

        // remove index columns and data columns just added for the MV testing
        this.table.removeColumns(0, nLoadDims);
        this.table.removeColumns(onCols, this.table.nColumns());
      }

      if ((haveConstraints || removeMVRows) && this.table.nColumns() > 0) {
        // apply constraints to vars that have all loadDims
        this.table.tryToApplyConstraints(-1, conVars, conOps, conVals, keep);
        // if (debugMode) String2.log(
        // ">> removeMVRows + constraints nRows before=" + onRows +
        // " after=" + keep.cardinality());

        // if (debugMode)
        // String2.log(Math2.memoryString() + "\n" +
        // ">> after bigVar constraints, justKeep nRows before=" +
        // onRows + " after=" + nRows());
        if (keep.cardinality() == 0) {
          // if (verbose) String2.log(warningInMethod +
          // "Returning an empty table after removeMVRows/constraints. " +
          // "time=" + (System.currentTimeMillis() - time));
          this.table.clear();
          return;
        }
        this.table.justKeep(keep);
        allIndicesTable.justKeep(keep);
      }
      // read all of the other variables:
      // repeatedly, read batches of vars with same dimensions, and JOIN to main table
      // Load constrained variables first.
      if (haveConstraints) {
        for (int v = 0; v < nLoadVars; v++) {
          if (loaded.get(v)) {
            continue;
          }
          Table lut = new Table(); // look up table which will be JOINed into main table
          // is there at least 1 constraint of this var?
          String varName = loadVarNames.get(v);
          int con1 = conVars.indexOf(varName);
          if (con1 < 0) {
            continue;
          }
          Variable tVar = loadVars.get(v);
          VarData data = VarData.fromVariable(this, tVar);
          addVarAndIndicies(
              nd0, loadDims, loaded, allIndicesTable, lut, getMetadata, data, v, tVar);
          loadDimMatchedVars(
              loadVarNames,
              standardizeWhat,
              nd0,
              loadVars,
              loadDims,
              nLoadVars,
              loaded,
              lut,
              data,
              getMetadata);

          // If we ran constraints on this var earlier, load it.
          BitSet lutkeep = getKeepForVar(data, nd0, varToKeep);
          // If we've already applied the constraints, use that previous bitset
          if (lutkeep.cardinality() == data.pa.size()) {
            int nAfter = lut.tryToApplyConstraints(-1, conVars, conOps, conVals, lutkeep);
            if (nAfter == 0) {
              // if (verbose) String2.log(warningInMethod +
              // "Returning an empty table after applying constraints to lut. " +
              // "time=" + (System.currentTimeMillis() - time));
              this.table.clear();
              return;
            }
          }
          lut.justKeep(lutkeep);
          // if (debugMode)
          // String2.log(Math2.memoryString() + "\n" +
          // ">> after lut constraints, justKeep lut.nRows before=" +
          // onLutRows + " after=" + nAfter);

          joinLutToTable(lut, data, allIndicesTable);
        }
      }
      while (loaded.cardinality() < nLoadVars) {
        Table lut = new Table(); // look up table which will be JOINed into main table
        VarData varData =
            findVarToLoad(
                loadVarNames,
                standardizeWhat,
                nd0,
                loadVars,
                loadDims,
                nLoadVars,
                loaded,
                allIndicesTable,
                lut,
                getMetadata);

        loadDimMatchedVars(
            loadVarNames,
            standardizeWhat,
            nd0,
            loadVars,
            loadDims,
            nLoadVars,
            loaded,
            lut,
            varData,
            getMetadata);

        // all constraints checked above so we just need to join this data in.
        joinLutToTable(lut, varData, allIndicesTable);
      }
      // and Bob's your uncle! we have all of the data

      // this will be either the order that was requested, or their order in the file
      this.table.reorderColumns(loadVarNames, false); // discardOthers=false, should be irrelevant

      this.table.decodeCharsAndStrings();
      this.table.convertToUnsignedPAs();

      // if (reallyVerbose)
      // String2.log(msg +
      // " finished. nRows=" + nRows() + " nCols=" + nColumns() +
      // " time=" + (System.currentTimeMillis() - time) + "ms");
    } finally {
      try {
        if (ncFile != null) ncFile.close();
      } catch (Exception e9) {
      }
    }
  }

  private void joinLutToTable(Table lut, VarData varData, Table allIndicesTable) {
    // JOIN lut into main table
    // if (debugMode) String2.log(">> lut=\n" + lut.dataToString(5));
    int nMatchingCols = Math.max(1, varData.nDims); // even scalars have 1 matching column
    BitSet lutkeep = this.table.join(nMatchingCols, 0, "", lut); // "" = mvKey not needed
    // remove the index columns from the main table
    this.table.removeColumns(0, nMatchingCols);
    // if (debugMode) String2.log(">> this table after join:\n" + dataToString(5));

    // remove unmatched rows
    int tnRows = lutkeep.cardinality();
    if (tnRows == 0) {
      this.table.clear();
      // if (verbose) String2.log(warningInMethod +
      // "Returning an empty table after a join. " +
      // "time=" + (System.currentTimeMillis() - time));
      return;
    }
    this.table.justKeep(lutkeep);
    allIndicesTable.justKeep(lutkeep);
  }

  private BitSet getKeepForVar(VarData data, int nd0, List<Pair<VarData, BitSet>> varToKeep) {
    for (int i = 0; i < varToKeep.size(); i++) {
      VarData inList = varToKeep.get(i).getLeft();
      if (doDimsMatch(nd0, data.nDims, data.dims, inList.nDims, inList.dims)) {
        return varToKeep.get(i).getRight();
      }
    }
    BitSet keep = new BitSet();
    keep.set(0, data.pa.size());
    varToKeep.add(Pair.of(data, keep));
    return keep;
  }

  private void addColumnToTable(
      boolean getMetadata, BitSet loaded, VarData varData, int v, Variable tVar, Table table) {
    loaded.set(v);
    table.addColumn(table.nColumns(), tVar.getFullName(), varData.pa, varData.atts);
    // does this var point to the pseudo-data var with CF grid_mapping (projection)
    // information?
    if (getMetadata && gridMappingAtts == null) {
      gridMappingAtts = NcHelper.getGridMappingAtts(ncFile, varData.atts.getString("grid_mapping"));
      if (gridMappingAtts != null) {
        table.globalAttributes.add(gridMappingAtts);
      }
    }
  }

  private Dimension convertDimension(int nd0, Dimension dim) {
    for (int d0 = 0; d0 < nd0; d0++) {
      if (String2.indexOfObject(tDimsAs[d0], dim) >= 0) {
        // convert to the 'as' dimension
        dim = tDimsAs[d0][tDimsAs[d0].length - 1];
        break;
      }
    }
    return dim;
  }

  private VarData findVarToLoad(
      StringArray loadVarNames,
      int standardizeWhat,
      int nd0,
      ArrayList<Variable> loadVars,
      ArrayList<Dimension> loadDims,
      int nLoadVars,
      BitSet loaded,
      Table allIndicesTable,
      Table lut,
      boolean getMetadata)
      throws Exception {
    VarData varData = null;
    for (int v = 0; v < nLoadVars; v++) {
      if (loaded.get(v)) continue;
      // if (debugMode) {
      // String2.log(">> v=" + v + " cDims==null?" + (cDims==null) +
      // " lut: nCols=" + lut.nColumns() + " nRows=" + lut.nRows());
      // String2.log(">> lut=" + lut.dataToString(5));
      // }

      // look for an unloaded var (and other vars with same dimensions)
      Variable tVar = loadVars.get(v);

      varData = VarData.fromVariable(this, tVar);

      addVarAndIndicies(nd0, loadDims, loaded, allIndicesTable, lut, getMetadata, varData, v, tVar);
      return varData;
    }
    return varData;
  }

  private void addVarAndIndicies(
      int nd0,
      ArrayList<Dimension> loadDims,
      BitSet loaded,
      Table allIndicesTable,
      Table lut,
      boolean getMetadata,
      VarData varData,
      int v,
      Variable tVar) {
    int cShape[] = new int[varData.nDims];
    for (int d = 0; d < varData.nDims; d++) {
      // which dim is it in loadDims?
      Dimension cDim = varData.dims.get(d);
      cDim = convertDimension(nd0, cDim);
      cShape[d] = cDim.getLength();
      int whichDim = loadDims.indexOf(cDim);
      // insert that index in main table
      this.table.addColumn(
          d,
          "_index_" + whichDim,
          allIndicesTable.getColumn(whichDim)); // will throw error if whichDim=-1
    }

    // insert index columns in lut
    if (varData.nDims == 0) {
      // if scalar vars, make key columns with 0's
      // in lut
      lut.addColumn(0, "_scalar_", new IntArray(new int[] {0}), new Attributes());
      // and in main table
      IntArray ia = new IntArray(this.table.nRows(), false);
      ia.addN(this.table.nRows(), 0);
      // String2.log("nRows=" + nRows() + " ia.size=" + ia.size());
      this.table.addColumn(0, "_scalar_", ia, new Attributes());
    } else {
      lut.addIndexColumns(cShape);
    }

    // read this var into lut
    // knownPAs[v] = null;
    // knownAtts[v] = null;
    addColumnToTable(getMetadata, loaded, varData, v, tVar, lut);
  }

  private void loadDimMatchedVars(
      StringArray loadVarNames,
      int standardizeWhat,
      int nd0,
      ArrayList<Variable> loadVars,
      ArrayList<Dimension> loadDims,
      int nLoadVars,
      BitSet loaded,
      Table table,
      VarData matchDims,
      boolean getMetadata)
      throws Exception {
    // extra check on loaded?? verify this isn't a problem
    for (int v = 0; v < nLoadVars; v++) {
      if (loaded.get(v)) continue;
      // if (debugMode) {
      // String2.log(">> v=" + v + " cDims==null?" + (cDims==null) +
      // " lut: nCols=" + lut.nColumns() + " nRows=" + lut.nRows());
      // String2.log(">> lut=" + lut.dataToString(5));
      // }

      // look for an unloaded var (and other vars with same dimensions)
      Variable tVar = loadVars.get(v);
      VarData data = VarData.fromVariableIfDimsMatch(this, tVar, matchDims, nd0);
      if (data == null) {
        continue;
      }
      // read this var into lut
      addColumnToTable(getMetadata, loaded, data, v, tVar, table);
    }
  }

  private boolean doDimsMatch(
      int nd0, int ntDims, List<Dimension> tDims, int ncDims, List<Dimension> cDims) {
    // does this var have the exact same dimensions, in same order?
    if (ntDims != ncDims) {
      return false;
    }
    for (int d = 0; d < ncDims; d++) {
      Dimension dim = tDims.get(d);
      dim = convertDimension(nd0, dim);
      if (!cDims.get(d).equals(dim)) {
        return false;
      }
    }
    return true;
  }

  private HashSet<Dimension> findNonStringLengthDims(List<Variable> allVars, int nAllVars) {
    HashSet<Dimension> notStringLengthDims = new HashSet<>();
    for (int v = 0; v < nAllVars; v++) {
      Variable tVar = allVars.get(v);
      List<Dimension> tDims = tVar.getDimensions(); // won't be null
      int tnDims = tDims.size();
      // here, assume the last dim of any multiDim char var
      // is the string length dimension, so skip it
      if (tVar.getDataType() == DataType.CHAR) tnDims--;
      for (int d = 0; d < tnDims; d++) notStringLengthDims.add(tDims.get(d));
    }
    return notStringLengthDims;
  }

  private Dimension[][] processTreatDimensionsAs(String[][] treatDimensionsAs, int nd0) {
    String msg;
    Dimension[][] tDimsAs = null;
    if (nd0 > 0) {
      tDimsAs = new Dimension[nd0][];
      for (int d0 = 0; d0 < nd0; d0++) {
        int nd1 = treatDimensionsAs[d0].length;
        tDimsAs[d0] = new Dimension[nd1];
        int tDimsSize = -1;
        for (int d1 = 0; d1 < nd1; d1++) {
          tDimsAs[d0][d1] = ncFile.findDimension(treatDimensionsAs[d0][d1]);
          if (tDimsAs[d0][d1] == null) {
            msg =
                warningInMethod
                    + "treatDimensionAs["
                    + d0
                    + "]["
                    + d1
                    + "]="
                    + treatDimensionsAs[d0][d1]
                    + " isn't in the file.";
            if (d1 == nd1 - 1) // the 'to' dim must be in the file
            throw new RuntimeException(msg);
            // if (debugMode) String2.log(msg);
            continue;
          }
          if (tDimsSize < 0) tDimsSize = tDimsAs[d0][d1].getLength();
          else
            Test.ensureEqual(
                tDimsAs[d0][d1].getLength(),
                tDimsSize,
                warningInMethod
                    + "All of the treatDimensionsAs dimensions ("
                    + String2.toCSSVString(treatDimensionsAs[d0])
                    + ") must be the same length (["
                    + d0
                    + "]["
                    + d1
                    + "]).");
        }
      }
    }
    return tDimsAs;
  }

  private void validateTreatDimensionsAs(String[][] treatDimensionsAs, int nd0) {
    for (int d0 = 0; d0 < nd0; d0++) {
      if (treatDimensionsAs[d0] == null)
        throw new RuntimeException(warningInMethod + "treatDimensionAs[" + d0 + "] is null!");
      else if (treatDimensionsAs[d0].length < 2)
        throw new RuntimeException(
            warningInMethod
                + "treatDimensionAs["
                + d0
                + "].length="
                + treatDimensionsAs[d0].length
                + " must be >1: "
                + String2.toCSSVString(treatDimensionsAs[d0]));
      // if (debugMode)
      // msg +=" treatDimensionsAs[" + d0 + "]=" +
      // String2.toCSSVString(treatDimensionsAs[d0]);
    }
  }

  private void loadVars(
      StringArray loadVarNames,
      StringArray loadDimNames,
      ArrayList<Variable> loadVars,
      ArrayList<Dimension> loadDims,
      int nd0,
      int nAllVars,
      List<Variable> allVars) {
    if (loadVarNames.size() > 0) {
      // loadVarNames was specified

      // gather the loadVars and loadDims (not including the aliases)
      loadDimNames.clear();
      for (int v = 0; v < loadVarNames.size(); v++) {
        Variable var = ncFile.findVariable(loadVarNames.get(v));
        if (var == null) {
          loadVarNames.remove(v--); // var not in file, so don't try to load it
        } else {
          loadVars.add(var);
          VarData data = VarData.dimsFromVariable(this, var);
          for (int d = 0; d < data.nDims; d++) {
            Dimension tDim = data.dims.get(d);
            if (loadDims.indexOf(tDim) < 0) { // not yet in the list
              tDim = convertDimension(nd0, tDim);
              if (loadDims.indexOf(tDim) < 0) { // possibly different tDim not yet in the list
                loadDims.add(tDim);
                loadDimNames.add(tDim.getName());
              }
            }
          }
        }
      }
      if (loadVars.size() == 0) {
        // if (verbose) String2.log(warningInMethod +
        // "Returning an empty table because none of the requested variables are in the
        // file. " +
        // "time=" + (System.currentTimeMillis() - time));
        return;
      }

    } else {
      // loadVarNames wasn't specified

      if (loadDimNames.size() == 0) {
        // loadDimNames wasn't specified either

        // find var(s) that use the most dimensions
        try {
          Variable tVars[] =
              NcHelper.findMaxDVariables(ncFile, ""); // throws Exception if no vars with dimensions

          // gather loadDims from the first of those vars
          // (so it won't include aliases)
          Variable tVar = tVars[0];
          VarData data = VarData.dimsFromVariable(this, tVar);
          for (int d = 0; d < data.nDims; d++) {
            Dimension dim = data.dims.get(d);
            loadDims.add(dim);
            loadDimNames.add(dim.getName());
          }
        } catch (Exception e) {
          // FUTURE: read all static variables
          String2.log("Table.readMultidimNc caught: " + e.toString());
        }

      } else {
        // loadDimNames was specified (it doesn't include aliases)
        // gather the specified loadDims
        for (int d = 0; d < loadDimNames.size(); d++) {
          String dimName = loadDimNames.get(d);
          Dimension dim = ncFile.findDimension(dimName);
          if (dim == null) {
            String2.log("Removing dimName=" + dimName + ": it isn't in the file.");
            loadDimNames.remove(d--);
          } else {
            loadDims.add(dim);
          }
        }
        if (loadDimNames.size() == 0)
          String2.log("None of the requested loadDimNames is in the file.");
      }

      // now, loadDims is known, but loadVars isn't
      // find vars that use any subset of loadDims (and no others)
      // including scalar vars
      boolean dimUsed[] = new boolean[loadDims.size()];
      LOADVARS_V:
      for (int v = 0; v < nAllVars; v++) {
        Variable var = allVars.get(v);
        VarData data = VarData.dimsFromVariable(this, var);
        for (int d = 0; d < data.nDims; d++) {
          Dimension tDim = data.dims.get(d);
          int whichDim = loadDims.indexOf(tDim);
          if (whichDim < 0) {
            // is it one of the aliases?
            for (int d0 = 0; d0 < nd0; d0++) {
              if (String2.indexOfObject(tDimsAs[d0], tDim) >= 0) {
                // change to the 'as' dimension?
                whichDim = loadDims.indexOf(tDimsAs[d0][tDimsAs[d0].length - 1]);
                if (whichDim >= 0) break;
              }
            }
          }
          if (whichDim < 0) continue LOADVARS_V;
          dimUsed[whichDim] = true;
        }
        loadVars.add(var);
        loadVarNames.add(var.getFullName());
      }
      if (loadVars.size() == 0) {
        // if (verbose) String2.log(warningInMethod +
        // "Returning an empty table because there are no scalar variables " +
        // "and no variables in the file use any of these dimensions: " +
        // loadDimNames + ". " +
        // "time=" + (System.currentTimeMillis() - time));
        return;
      }

      // remove unused dimensions
      for (int d = loadDims.size() - 1; d >= 0; d--) { // backwards since may delete
        if (!dimUsed[d]) loadDims.remove(d);
      }
      if (loadDims.size() == 0) String2.log("After analysis, loadDims.size is now 0!");
    }
  }
}
