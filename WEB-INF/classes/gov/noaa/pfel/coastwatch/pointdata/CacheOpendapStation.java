/*
 * CacheOpendapStation Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;
import dods.dap.*;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Given an opendap url representing one station, this creates or updates a cache of the data in a
 * 4D .nc file.
 *
 * <p>The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-07-20
 */
public class CacheOpendapStation {

  /**
   * Set this to true (by calling verbose=true in your program, not by changing the code here) if
   * you want lots of diagnostic messages sent to String2.log.
   */
  public static boolean verbose = false;

  public static boolean reallyVerbose = false;

  protected String url;
  protected String fullStationFileName;
  protected String variableNames[];

  protected PrimitiveArray depths;
  protected double lat = Double.NaN;
  protected double lon = Double.NaN;

  // opendapTimeDimensionSize is store here while this object exists
  protected int opendapTimeDimensionSize = -1;
  // but is also stored in the cache file as a global attribute with this name...
  public static final String OPENDAP_TIME_DIMENSION_SIZE = "CWOpendapTimeDimensionSize";

  /**
   * Given an opendap url representing one station with 4D numeric ([time][depth][lat][lot]) or 5D
   * String variables, this constructor sets things up but doesn't call createNewCache or
   * updateCache (which normally, the caller does right after construction).
   *
   * <p>All variables used here from a given station must use the same time values, depth values,
   * lat value and lon value.
   *
   * <p>The cached data is stored in its original form (e.g., time in ms, or s, or hours, since
   * yyyy-mm-dd).
   *
   * @param url the url of the station
   * @param fullStationFileName is dir/name (with .nc extension) for the station file cache.
   *     StationVariableNc4D converts "_" to " " to generate station name (e.g., use "MBARI_M2_adcp"
   *     to generate a station name of "MBARI M2 adcp").
   * @param variableNames a String[] of variable names.
   * @throws Exception if any of the parameters is null
   */
  public CacheOpendapStation(String url, String fullStationFileName, String variableNames[])
      throws Exception {

    this.url = url;
    this.fullStationFileName = fullStationFileName;
    this.variableNames = variableNames;

    String errorInMethod = String2.ERROR + " in CacheOpendapStation constructor";
    Test.ensureNotNull(url, errorInMethod + "url is null.");
    Test.ensureNotNull(fullStationFileName, errorInMethod + "fullStationFileName is null.");
    Test.ensureNotNull(variableNames, errorInMethod + "variableNames is null.");
    Test.ensureNotEqual(variableNames.length, 0, errorInMethod + "variableNames length is 0.");
  }

  /**
   * This returns the url (source of the cache data).
   *
   * @return the url (source of the cache data).
   */
  public String url() {
    return url;
  }

  /**
   * This returns the fullStationFileName.
   *
   * @return the fullStationFileName.
   */
  public String fullStationFileName() {
    return fullStationFileName;
  }

  /**
   * This returns the variableNames.
   *
   * @return the variableNames.
   */
  public String[] variableNames() {
    return variableNames;
  }

  /** This deletes any existing cache. */
  public void deleteCache() {
    File2.delete(fullStationFileName);
  }

  /**
   * This deletes any existing cache and creates a new cache file (.nc file with each of the
   * specified variables stored as 4D array, with nLat=1 and nLon=1). This ensures the time values
   * (column 0) are ascending (needed for binary searches) This won't throw an exception.
   *
   * <p>Note that in my CWBrowser setup, only one program (hence one thread), CWBrowserSA, is
   * responsible for calling createNewCache and/or updateCache.
   *
   * @return true if all went well and there is a cache file
   */
  public boolean createNewCache() {

    String2.log("CacheOpendapStation.createNewCache\n  fileName=" + fullStationFileName);
    boolean success = false;
    long time = System.currentTimeMillis();
    String errorInMethod =
        String2.ERROR
            + " in CacheOpendapStation.createNewCache\n  fileName="
            + fullStationFileName
            + "\n  url="
            + url
            + "\n  ";

    // delete the existing cache
    File2.delete(fullStationFileName);

    // open the .nc file
    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    try {
      // open the opendap url
      // DODSNetcdfFile in = new DODSNetcdfFile(url); //bug? showed no global attributes //but fixed
      // in 2.2.16  //2022-01-24 DODSNetcdfFile gone in netcdf-java 5.5.2
      NetcdfDataset in =
          NetcdfDatasets.openDataset(
              url); // can't recognize mbari non-udunits  //2021: 's' is new API

      try {
        // open the file (before 'try'); if it fails, no temp file to delete
        NetcdfFormatWriter ncWriter = null;

        try {
          NetcdfFormatWriter.Builder out =
              NetcdfFormatWriter.createNewNetcdf3(fullStationFileName + randomInt);
          Group.Builder outRootGroup = out.getRootGroup();
          out.setFill(false);

          // transfer global attributes  (with updated 'history' attribute)
          List globalAttList = in.getGlobalAttributes();
          int nGlobalAtt = globalAttList.size();
          if (verbose) String2.log("  nGlobalAtt=" + nGlobalAtt);
          String history = null;
          String newHistory = "Data from " + url;
          for (int a = 0; a < nGlobalAtt; a++) {
            ucar.nc2.Attribute att = (ucar.nc2.Attribute) globalAttList.get(a);
            String attName = att.getName();
            // if (verbose) String2.log("  globalAtt name=" + att.getName());
            Array values = att.getValues();
            if (attName.equals("history")) {
              history = NcHelper.getPrimitiveArray(values).getString(0) + "\n" + newHistory;
              outRootGroup.addAttribute(new ucar.nc2.Attribute(attName, history));
            } else {
              outRootGroup.addAttribute(NcHelper.newAttribute(attName, values));
            }
          }
          if (history == null)
            outRootGroup.addAttribute(new ucar.nc2.Attribute("history", newHistory));

          // get the first variable from in
          Variable variable0 = in.findVariable(variableNames[0]);

          // get its dimensions
          // and add global attribute OPENDAP_TIME_DIMENSION_SIZE
          List inDimList0 = variable0.getDimensions();
          Dimension timeDimension = (Dimension) inDimList0.get(0);
          opendapTimeDimensionSize = timeDimension.getLength();
          outRootGroup.addAttribute(
              new ucar.nc2.Attribute(
                  OPENDAP_TIME_DIMENSION_SIZE, Integer.valueOf(opendapTimeDimensionSize)));

          // for each variable
          HashSet<String> dimNameHashSet = new HashSet();
          ArrayList<Variable> inCumulativeVariableList = new ArrayList();
          ArrayList<Variable.Builder> outCumulativeVariableList = new ArrayList();
          int variableInColumn[] = new int[variableNames.length];
          String latName = null;
          String lonName = null;
          String timeName = null;
          Variable.Builder newVars[] = new Variable.Builder[variableNames.length];
          for (int v = 0; v < variableNames.length; v++) {

            // get the variable
            Variable inVariable = in.findVariable(variableNames[v]);
            Test.ensureNotNull(
                inVariable, errorInMethod + "variable not found: " + variableNames[v]);
            boolean isCharArrayVariable = inVariable.getDataType() == DataType.CHAR;

            // get dimensions
            List<Dimension> inDimList = inVariable.getDimensions();
            Test.ensureTrue(
                (inDimList.size() == 4 && !isCharArrayVariable)
                    || (inDimList.size() == 5 && isCharArrayVariable),
                errorInMethod
                    + "  inDimList.size="
                    + inDimList.size()
                    + " for variable="
                    + inVariable.getFullName()
                    + " isCharArrayVariable="
                    + isCharArrayVariable);
            Dimension newDims[] = new Dimension[inDimList.size()];
            Variable.Builder newDimVars[] = new Variable.Builder[inDimList.size()];
            for (int d = 0; d < inDimList.size(); d++) {
              Dimension dim = inDimList.get(d);
              String dimName = dim.getName();

              // first 4 dimensions must be same as for variable0
              if (d < 4)
                Test.ensureEqual(
                    dimName,
                    ((Dimension) inDimList0.get(d)).getName(),
                    errorInMethod
                        + "  unexpected dimension #"
                        + d
                        + " for variable="
                        + inVariable.getFullName());

              // add the dimension
              if (!dimNameHashSet.contains(dimName)) {
                dimNameHashSet.add(dimName);
                newDims[d] =
                    NcHelper.addDimension(
                        outRootGroup,
                        dimName,
                        dim.getLength(),
                        true, // isShared
                        dim.isUnlimited(),
                        false); // isVariableLength

                // add the related variable (5th/char dimension doesn't have a variable(?))
                if (d < 4) {
                  if (d == 0) timeName = dimName;
                  if (d == 2) latName = dimName;
                  if (d == 3) lonName = dimName;
                  Variable inDimVariable = in.findVariable(dimName);
                  inCumulativeVariableList.add(inDimVariable);
                  newDimVars[d] =
                      NcHelper.addVariable(
                          outRootGroup, dimName, inDimVariable.getDataType(), Arrays.asList(dim));
                  outCumulativeVariableList.add(newDimVars[d]);

                  // write the related variable's attributes   (after adding the variable)
                  AttributeContainer attList = inDimVariable.attributes();
                  Iterator it = attList.iterator();
                  while (it.hasNext()) newDimVars[d].addAttribute((ucar.nc2.Attribute) it.next());

                  // test that nLat and nLon are 1
                  if (d == 2)
                    Test.ensureEqual(
                        dim.getLength(),
                        1,
                        errorInMethod + "lat dimension (" + dimName + ") length isn't 1!");
                  if (d == 3)
                    Test.ensureEqual(
                        dim.getLength(),
                        1,
                        errorInMethod + "lon dimension (" + dimName + ") length isn't 1!");
                }
              }
            }

            // add the variable to 'out'
            newVars[v] =
                NcHelper.addVariable(
                    outRootGroup, inVariable.getFullName(), inVariable.getDataType(), inDimList);
            inCumulativeVariableList.add(inVariable);
            outCumulativeVariableList.add(newVars[v]);
            variableInColumn[v] = inCumulativeVariableList.size() - 1;
            // ensure variableInColumn[v] == 4 + v
            // this affects whether all columns are updated in updateCache
            Test.ensureEqual(
                variableInColumn[v],
                4 + v,
                errorInMethod + "unexpected variableInColumn for v=" + v);

            // write Attributes   (after adding the variable)
            AttributeContainer attList = inVariable.attributes();
            Iterator it = attList.iterator();
            while (it.hasNext()) newVars[v].addAttribute((ucar.nc2.Attribute) it.next());
          }

          // leave "define" mode
          if (verbose) String2.log("  leaving 'define' mode");
          ncWriter = out.build();

          // write the data
          for (int v = 0; v < inCumulativeVariableList.size(); v++) {
            Variable inVariable = inCumulativeVariableList.get(v);
            Variable outVariable =
                ncWriter.findVariable(
                    outCumulativeVariableList
                        .get(v)
                        .getFullName()); // because list has Variable.Builders
            if (verbose) String2.log("  writing data var=" + inVariable.getFullName());

            // write the data
            // ??? what if data has been added to variable since dimensions were defined???
            Array array = inVariable.read();
            ncWriter.write(outVariable, array);

            // ensure that all time values are reasonable
            // mbari opendap server sometimes returns gibberish values
            if (inVariable.getFullName().equals(timeName)) {

              // ensure time values are ascending
              DoubleArray timeDA = new DoubleArray(NcHelper.toDoubleArray(array));
              double dNew = timeDA.getDouble(0); // nc files have at least one row
              Test.ensureTrue(
                  dNew > -1e15 && dNew < 1e15, // allows for millis since 1970 and much more
                  errorInMethod + "first time value=" + dNew + " is outside +/-1e15!");
              int nRows = timeDA.size();
              for (int row = 1; row < nRows; row++) {
                double dOld = dNew;
                dNew = timeDA.getDouble(row);
                if (!Double.isFinite(dNew) || dNew <= dOld)
                  Test.error(
                      errorInMethod
                          + "time(row="
                          + row
                          + ")="
                          + dNew
                          + " is less than or equal to time(row-1)="
                          + dOld
                          + "\ntimes="
                          + timeDA);
              }
              Test.ensureTrue(
                  dNew > -1e15 && dNew < 1e15, // allows for millis since 1970 and much more
                  errorInMethod + "last time value=" + dNew + " is outside +/-1e15!");
            }

            // ensure that all lat and lon values are reasonable
            // mbari opendap server sometimes returns gibberish values
            if (inVariable.getFullName().equals(lonName)
                || inVariable.getFullName().equals(latName)) {
              DoubleArray da = new DoubleArray(NcHelper.toDoubleArray(array));
              double stats[] = da.calculateStats();
              double daMin = stats[PrimitiveArray.STATS_MIN];
              double daMax = stats[PrimitiveArray.STATS_MAX];
              String testString = "latitude";
              double testMin = -90;
              double testMax = 90;
              if (inVariable.getFullName().equals(lonName)) {
                testString = "longitude";
                testMin = -180;
                testMax = 360;
              }
              Test.ensureTrue(
                  daMin >= testMin && daMax <= testMax,
                  errorInMethod
                      + testString
                      + " values must be between "
                      + testMin
                      + " and "
                      + testMax
                      + ".\nvalues="
                      + da);
            }
          }

          // I care about exception from this
          ncWriter.close();
          ncWriter = null;

        } catch (Exception e) {
          String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
          if (ncWriter != null) {
            try {
              ncWriter.abort();
            } catch (Exception e9) {
            }
            File2.delete(fullStationFileName + randomInt);
            ncWriter = null;
          }
          throw e;
        }

        // I care about exception from this
        in.close();

        success = true;

      } catch (Exception e) {

        try {
          in.close();
        } catch (Exception e2) {
          // don't care
        }
        throw e;
      }

      // print diagnostics
      if (reallyVerbose) {

        // read the data as a table
        // String2.log("  pre read table " + Math2.memoryString());
        Table table = new Table();
        table.read4DNc(fullStationFileName + randomInt, null, 1, null, -1); // standardizeWhat=1

        String2.log("  post read table nRows=" + table.nRows() + " nCols=" + table.nColumns());
        String2.log(table.toString(3));
        // print column data ranges
        for (int col = 0; col < table.nColumns(); col++)
          String2.log("col=" + col + " " + table.getColumn(col).statsString());
      }

    } catch (Exception e) {
      String2.log(MustBe.throwable(errorInMethod, e));
    }

    // rename the file to the specified name
    if (success)
      File2.rename(
          fullStationFileName + randomInt,
          fullStationFileName); // an existing new file will be deleted
    else File2.delete(fullStationFileName + randomInt);

    // diagnostic
    String2.log(
        "  createNewCache time="
            + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
            + "  success="
            + success);
    return success;
  }

  /**
   * This updates the cache file (if there is new opendap data) or calls createNewCache if the cache
   * file doesn't exist. This won't throw an exception.
   *
   * <p>Note that in my CWBrowser setup, only one program (hence one thread), CWBrowserSA, is
   * responsible for calling createNewCache and/or updateCache.
   *
   * @return true if all went well and there is a cache file
   */
  public boolean updateCache() {

    // need to create cache?
    if (!File2.isFile(fullStationFileName)) {
      return createNewCache();
    }

    long time = System.currentTimeMillis();
    String2.log("CacheOpendapStation.updateCache\n  fileName=" + fullStationFileName);
    String errorInMethod =
        String2.ERROR
            + " in CacheOpendapStation.updateCache\n  fileName="
            + fullStationFileName
            + "\n";
    boolean success = false;
    int nNewRows = 0;

    // open the .nc file
    // POLICY: because this procedure may be used in more than one thread,
    // do work on unique temp files names using randomInt, then rename to proper file name.
    // If procedure fails half way through, there won't be a half-finished file.
    int randomInt = Math2.random(Integer.MAX_VALUE);

    try {

      // opendap get time dimension  -- is there new data?
      // String2.log("  pre read dodsNetcdf ");
      long inTime = System.currentTimeMillis();
      DConnect dConnect = new DConnect(url, true, 1, 1);
      DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
      String2.log("  post create dConnect,DDS  time=" + (System.currentTimeMillis() - inTime));

      // are there more time values than before?
      inTime = System.currentTimeMillis();
      DGrid dGrid = (DGrid) dds.getVariable(variableNames[0]);
      DArray timeDArray = (DArray) dGrid.getVar(1); // 0 is data, 1 is time
      String2.log("  timeDArray name=" + timeDArray.getName());
      DArrayDimension timeDimension = timeDArray.getDimension(0);
      String2.log("  timeDimension name=" + timeDimension.getName());
      int newOpendapTimeDimensionSize = timeDimension.getSize();
      inTime = System.currentTimeMillis() - inTime;

      // get old opendapTimeDimensionSize, if not known
      if (opendapTimeDimensionSize < 0) {
        NetcdfFile stationNcFile = NcHelper.openFile(fullStationFileName);
        try {
          // add the opendapTimeDimensionSize
          Group rootGroup = stationNcFile.getRootGroup();
          PrimitiveArray otdsPa =
              NcHelper.getGroupAttribute(rootGroup, OPENDAP_TIME_DIMENSION_SIZE);
          opendapTimeDimensionSize = otdsPa == null ? 0 : otdsPa.getInt(0);
          if (verbose) String2.log("  opendapTimeDimensionSize=" + opendapTimeDimensionSize);
        } finally {
          try {
            if (stationNcFile != null) stationNcFile.close();
          } catch (Exception e9) {
          }
        }
      }
      if (verbose)
        String2.log(
            "  timeDim length old="
                + opendapTimeDimensionSize
                + " new="
                + newOpendapTimeDimensionSize
                + " time="
                + inTime
                + " ms");

      if (newOpendapTimeDimensionSize > opendapTimeDimensionSize) {

        // read the data from the cache file
        String2.log("  read4DNc...");
        // String2.log("  pre read table ");
        Table table = new Table();
        table.read4DNc(fullStationFileName, null, 1, null, -1); // standardizeWhat=1
        int oldNRows =
            table.nRows(); // will be different from opendapTimeDimensionSize because flattened
        PrimitiveArray timeColumn = table.getColumn(3);
        String2.log("  table from cache file nRows=" + oldNRows + " nCols=" + table.nColumns());

        // get the depths, lat, and lon from the file (if don't have already).
        // Getting from the file is good. It ensures that the
        // original depth, lat, and lon values are returned with each update.
        if (Double.isNaN(lon)) {
          lon = table.getColumn(0).getDouble(0);
          if (verbose) String2.log("  file lon=" + lon);
        }
        if (Double.isNaN(lat)) {
          lat = table.getColumn(1).getDouble(0);
          if (verbose) String2.log("  file lat=" + lat);
        }
        if (depths == null) {
          IntArray ranks = new IntArray(); // just holds intermediate values
          depths = table.getColumn(2).makeIndices(ranks);
          if (verbose) String2.log("  file depths=" + depths);
        }

        // get new time values from opendap for each of the 4D variables
        // !!Be sure to use same time constraint for all opendap requests below,
        // because it takes time to get all the data and more time values
        // may be added in the process (don't get them yet!)
        String constraint =
            "[" + opendapTimeDimensionSize + ":1:" + (newOpendapTimeDimensionSize - 1) + "]";
        String2.log(url + "?" + table.getColumnName(3) + constraint);
        PrimitiveArray newTimes =
            OpendapHelper.getPrimitiveArray(dConnect, "?" + table.getColumnName(3) + constraint);
        Test.ensureEqual(
            newTimes.size(),
            newOpendapTimeDimensionSize - opendapTimeDimensionSize,
            errorInMethod + "Unexpected newTimes.size.");
        // ensure times are greater than previous and ascending
        double newTime =
            timeColumn.getDouble(timeColumn.size() - 1); // the last time before this data reading
        for (int i = 0; i < newTimes.size(); i++) {
          // don't use Test.ensure since it would generate lots of strings needlessly
          double oldTime = newTime;
          newTime = newTimes.getDouble(i);
          if (!Double.isFinite(newTime) || newTime > 1e15 || newTime <= oldTime)
            Test.error(
                errorInMethod
                    + "newTime("
                    + newTime
                    + ") is less than or equal to previous time("
                    + oldTime
                    + ") when i="
                    + i);
        }
        if (verbose)
          String2.log(
              (reallyVerbose ? "times=" + newTimes : "nTimes=" + newTimes.size())
                  + "\ndepths="
                  + depths
                  + "\nlat="
                  + lat
                  + " lon="
                  + lon);

        // get the new data for each variable
        // !!This takes some time
        constraint += // time constraint already done
            "[0:1:" + (depths.size() - 1) + "]" + "[0:1:0]" + "[0:1:0]";
        PrimitiveArray dataPA[] = new PrimitiveArray[variableNames.length];
        for (int var = 0; var < variableNames.length; var++) {
          if (verbose)
            String2.log(
                "  getting data from opendap for "
                    + variableNames[var]
                    + " url=\n"
                    + url
                    + "?"
                    + variableNames[var]
                    + constraint);
          PrimitiveArray pas[] =
              OpendapHelper.getPrimitiveArrays(dConnect, "?" + variableNames[var] + constraint);

          // ensure times, depths, lat, lon are as expected
          // This is very important, as I have seen gibberish values from mbari opendap server.
          Test.ensureEqual(
              pas[1],
              newTimes, // 1,2,3,4 since 0 is data, 1=time, 2=depths, ...
              errorInMethod + "Unexpected times for " + variableNames[var]);
          Test.ensureEqual(
              pas[2], depths, errorInMethod + "Unexpected depths for " + variableNames[var]);
          Test.ensureEqual(
              pas[3].getDouble(0), lat, errorInMethod + "Unexpected lat for " + variableNames[var]);
          Test.ensureEqual(
              pas[4].getDouble(0), lon, errorInMethod + "Unexpected lon for " + variableNames[var]);

          dataPA[var] = pas[0];
          Test.ensureEqual(
              dataPA[var].size(),
              (newOpendapTimeDimensionSize - opendapTimeDimensionSize) * depths.size(),
              errorInMethod + "Unexpected dataPA size for var=" + var);
        }

        // add rows to table
        int po = 0;
        for (int t = 0; t < newOpendapTimeDimensionSize - opendapTimeDimensionSize; t++) {
          for (int depth = 0; depth < depths.size(); depth++) {
            table.getColumn(0).addDouble(lon);
            table.getColumn(1).addDouble(lat);
            table.getColumn(2).addDouble(depths.getDouble(depth));
            table.getColumn(3).addDouble(newTimes.getDouble(t));
            for (int v = 0; v < variableNames.length; v++)
              // 4+v is validated by variableInColumn test in createCache
              table.getColumn(4 + v).addDouble(dataPA[v].getDouble(po));
          }
          po++;
        }

        // save table    with very minimal changes to attributes
        opendapTimeDimensionSize = newOpendapTimeDimensionSize;
        String2.log("  storing newOpendapTimeDimensionSize=" + newOpendapTimeDimensionSize);
        table.globalAttributes().set(OPENDAP_TIME_DIMENSION_SIZE, newOpendapTimeDimensionSize);
        table.saveAs4DNc(fullStationFileName, 0, 1, 2, 3, null, null, null);
        nNewRows = table.nRows() - oldNRows;
        if (verbose) {
          // String2.log(table.toString(3));
          // print column data ranges
          // for (int col = 0; col < table.nColumns(); col++)
          //    String2.log("col=" + col + " " + table.getColumn(col).statsString());
        }
      }
      success = true;

    } catch (Exception e) {
      String2.log(MustBe.throwable(errorInMethod, e));
    }

    String2.log(
        "  updateCache finished; time="
            + Calendar2.elapsedTimeString(System.currentTimeMillis() - time)
            + " nNewRows="
            + nNewRows
            + " success="
            + success);
    return success;
  }
}
