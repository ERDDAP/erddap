/* 
 * CacheOpendapStation Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.DoubleArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.griddata.OpendapHelper;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Get netcdf-X.X.XX.jar from 
 * http://www.unidata.ucar.edu/software/thredds/current/netcdf-java/index.html
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * 2013-02-21 new netcdfAll uses Java logging, not slf4j.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;

/** The Java DAP classes.  */
import dods.dap.*;

/** 
 * Given an opendap url representing one station, 
 * this creates or updates a cache of the data in a 4D .nc file.
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-07-20
 */
public class CacheOpendapStation { 

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;
    public static boolean reallyVerbose = false;

    protected String url;
    protected String fullStationFileName;
    protected String variableNames[];

    protected PrimitiveArray depths;
    protected double lat = Double.NaN;
    protected double lon = Double.NaN;

    //opendapTimeDimensionSize is store here while this object exists
    protected int opendapTimeDimensionSize = -1;
    //but is also stored in the cache file as a global attribute with this name...
    public final static String OPENDAP_TIME_DIMENSION_SIZE = "CWOpendapTimeDimensionSize";
       

    /**
     * Given an opendap url representing one station with 4D numeric 
     * ([time][depth][lat][lot]) or 5D 
     * String variables, this constructor sets things up but doesn't call
     * createNewCache or updateCache (which normally, the caller does
     * right after construction).
     *
     * <p>All variables used here from a given station must use the same 
     *   time values, depth values, lat value and lon value.
     * <p>The cached data is stored in its original form
     *   (e.g., time in ms, or s, or hours, since yyyy-mm-dd).
     * 
     * @param url the url of the station
     * @param fullStationFileName is dir/name (with .nc extension) for the
     *    station file cache.  StationVariableNc4D converts "_" to " " to generate
     *    station name (e.g., use "MBARI_M2_adcp" to generate a station name of "MBARI M2 adcp").
     * @param variableNames a String[] of variable names.
     * @throws Exception if any of the parameters is null
     */
    public CacheOpendapStation(String url,
        String fullStationFileName, String variableNames[]) throws Exception {

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

    /**
     * This deletes any existing cache.
     */
    public void deleteCache() {
        File2.delete(fullStationFileName);
    }


    /**
     * This deletes any existing cache and creates a new cache file.
     * This ensures the time values (column 0) are ascending (needed for binary searches)
     * This won't throw an exception.
     *
     * <p>Note that in my CWBrowser setup, only one program (hence one thread),
     * CWBrowserSA, is responsible for calling createNewCache and/or updateCache.
     *
     * @return true if all went well and there is a cache file 
     */
/* //rewrite createNewCache to use JDap for input  
   //to solve the issue of the time dimension growing during the process of getting all variable data 
    public boolean createNewCache() {

        String2.log("CacheOpendapStation.createNewCache\n  fileName=" + fullStationFileName);
        boolean success = false;
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + 
            " in CacheOpendapStation.createNewCache\n  fileName=" + fullStationFileName + 
            "\n  url=" + url + "\n  ";

        //delete the existing cache
        File2.delete(fullStationFileName);

        //open the .nc file
        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE));

        try {
            //open the opendap url
            DConnect dConnect = new DConnect(url, true, 1, 1); 
            DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);

            try {
                //open the file (before 'try'); if it fails, no temp file to delete
                NetcdfFileWriter out = new NetcdfFileWriter(
                    NetcdfFileWriter.Version.netcdf3, fullStationFileName + randomInt, false);
                
                try {
//stopped here
                    //transfer global attributes  (with updated 'history' attribute)
                    List globalAttList = in.getGlobalAttributes();
                    int nGlobalAtt = globalAttList.size();
                    if (verbose) String2.log("  nGlobalAtt=" + nGlobalAtt);
                    String history = null;
                    String newHistory = "Data from " + url;
                    for (int a = 0; a < nGlobalAtt; a++) {
                        ucar.nc2.Attribute att = (ucar.nc2.Attribute)globalAttList.get(a);
                        //if (verbose) String2.log("  globalAtt name=" + att.getName());
                        Array values = att.getValues();
                        if (att.getName().equals("history")) {
                            history = PrimitiveArray.factory(DataHelper.getArray(values)).getString(0) +
                                "\n" + newHistory;
                            values = DataHelper.getNc1DArray(history);
                        }
                        out.addGroupAttribute(rootGroup, att.getName(), values);
                    }
                    if (history == null) 
                        out.addGroupAttribute(rootGroup, "history", DataHelper.getNc1DArray(newHistory));

                    //get the first variable from in
                    Variable variable0 = in.findVariable(variableNames[0]);
                    
                    //get its dimensions  
                    //and add global attribute OPENDAP_TIME_DIMENSION_SIZE
                    List dimList0 = variable0.getDimensions();
                    Dimension timeDimension = (Dimension)dimList0.get(0);
                    opendapTimeDimensionSize = timeDimension.getLength();
                    out.addGroupAttribute(rootGroup, OPENDAP_TIME_DIMENSION_SIZE, 
                        DataHelper.getNc1DArray(new int[]{opendapTimeDimensionSize}));

                    //for each variable
                    HashSet dimHashSet = new HashSet();
                    ArrayList cumulativeVariableList = new ArrayList();
                    int variableInColumn[] = new int[variableNames.length];
                    String latName = null;
                    String lonName = null;
                    String timeName = null;
                    for (int v = 0; v < variableNames.length; v++) {

                        //get the variable
                        Variable variable = in.findVariable(variableNames[v]);
                        Test.ensureNotNull(variable, errorInMethod + "variable not found: " + variableNames[v]);
                        boolean isStringVariable = variable.getDataType() == DataType.CHAR;

                        //get dimensions
                        List<Dimension> dimList = variable.getDimensions();
                        Test.ensureTrue(
                            (dimList.size() == 4 && !isStringVariable) ||
                            (dimList.size() == 5 &&  isStringVariable), 
                            errorInMethod + 
                                "  dimList.size=" + dimList.size() + 
                                " for variable=" + variable.getName() + 
                                " isStringVariable=" + isStringVariable); 
                        for (int d = 0; d < dimList.size(); d++) {
                            Dimension dim = dimList.get(d);
                            String dimName = dim.getName();

                            //first 4 dimensions must be same as for variable0
                            if (d < 4)
                                Test.ensureEqual(dimName, ((Dimension)dimList0.get(d)).getName(), 
                                    errorInMethod + "  unexpected dimension #" + d + 
                                    " for variable=" + variable.getName()); 

                            //add the dimension
                            if (!dimHashSet.contains(dimName)) {
                                dimHashSet.add(dimName);
                                out.addDimension(rootGroup, dimName, dim.getLength(), true, //shared
                                    dim.isUnlimited(), false);  //isUnknownLength

                                //add the related variable (5th/char dimension doesn't have a variable(?))
                                if (d < 4) {
                                    if (d == 0) timeName = dimName;
                                    if (d == 2) latName = dimName;
                                    if (d == 3) lonName = dimName;
                                    Variable dimVariable = in.findVariable(dimName);
                                    cumulativeVariableList.add(dimVariable);
                                    out.addVariable(rootGroup, dimName, 
                                        dimVariable.getDataType(), 
                                        Arrays.asList(dim)); 

                                    //write the related variable's attributes   (after adding the variable)
                                    List attList = dimVariable.getAttributes();
                                    int nAtt = attList.size();
                                    for (int a = 0; a < nAtt; a++) {
                                        ucar.nc2.Attribute att = (ucar.nc2.Attribute)attList.get(a);
                                        out.addVariableAttribute(dimName, att.getName(), att.getValues());                                 
                                    }

                                    //test that nLat and nLon are 0
                                    if (d == 2)
                                        Test.ensureEqual(dim.getLength(), 1, 
                                            errorInMethod + "lat dimension (" + dimName + ") length isn't 1!");
                                    if (d == 3)
                                        Test.ensureEqual(dim.getLength(), 1, 
                                            errorInMethod + "lon dimension (" + dimName + ") length isn't 1!");
                                }
                            }
                        }

                        //add the variable to 'out'
                        out.addVariable(rootGroup, variable.getName(), 
                            variable.getDataType(), dimList); 
                        cumulativeVariableList.add(variable);
                        variableInColumn[v] = cumulativeVariableList.size() - 1;
                        //ensure variableInColumn[v] == 4 + v
                        //this affects whether all columns are updated in updateCache
                        Test.ensureEqual(variableInColumn[v], 4 + v,
                            errorInMethod + "unexpected variableInColumn for v=" + v);

                        //write Attributes   (after adding the variable)
                        List attList = variable.getAttributes();
                        int nAtt = attList.size();
                        for (int a = 0; a < nAtt; a++) {
                            ucar.nc2.Attribute att = (ucar.nc2.Attribute)attList.get(a);
                            out.addVariableAttribute(variable.getName(), att.getName(), att.getValues());
                        }
                    }

                    //leave "define" mode
                    if (verbose) String2.log("  leaving 'define' mode");
                    out.create();

                    //write the data
                    for (int v = 0; v < cumulativeVariableList.size(); v++) {
                        Variable variable = (Variable)cumulativeVariableList.get(v);
                        if (verbose) String2.log("  writing data var=" + variable.getName());

                        //write the data
                        //??? what if data has been added to variable since dimensions were defined???
                        Array array = variable.read();
                        out.write(variable.getName(), array);

                        //ensure that all time values are reasonable 
                        //mbari opendap server sometimes returns gibberish values
                        if (variable.getName().equals(timeName)) {

                            //ensure time values are ascending
                            DoubleArray timeDA = new DoubleArray(DataHelper.toDoubleArray(array));
                            double dNew = timeDA.getDouble(0); //nc files have at least one row
                            Test.ensureTrue(dNew > -1e15 && dNew < 1e15, //allows for millis since 1970 and much more
                                errorInMethod + "first time value=" + dNew + " is outside +/-1e15!");
                            int nRows = timeDA.size();
                            for (int row = 1; row < nRows; row++) {
                                double dOld = dNew;
                                dNew = timeDA.getDouble(row);
                                if (!Math2.isFinite(dNew) || dNew <= dOld)
                                    Test.error(errorInMethod + "time(row=" + row + ")=" + dNew + 
                                        " is less than or equal to time(row-1)=" + dOld + 
                                        "\ntimes=" + timeDA);
                            }
                            Test.ensureTrue(dNew > -1e15 && dNew < 1e15, //allows for millis since 1970 and much more
                                errorInMethod + "last time value=" + dNew + " is outside +/-1e15!");
                        }

                        //ensure that all lat and lon values are reasonable 
                        //mbari opendap server sometimes returns gibberish values
                        if (variable.getName().equals(lonName) ||
                            variable.getName().equals(latName)) {
                            DoubleArray da = new DoubleArray(DataHelper.toDoubleArray(array));
                            double stats[] = da.calculateStats();
                            double daMin = stats[PrimitiveArray.STATS_MIN];
                            double daMax = stats[PrimitiveArray.STATS_MAX];
                            String testString = "latitude";
                            double testMin = -90;
                            double testMax = 90;
                            if (variable.getName().equals(lonName)) {
                                testString = "longitude";
                                testMin = -180;
                                testMax = 360;
                            }
                            Test.ensureTrue(daMin >= testMin && daMax <= testMax,
                                errorInMethod + testString + " values must be between " + 
                                    testMin + " and " + testMax + ".\nvalues=" + da);
                        }


                    }

                    success = true;
                } catch (Exception e) {
                    String2.log(MustBe.throwable(errorInMethod, e));
                } finally {
                    //close the out file
                    out.flush();
                    out.close();
                }

            } catch (Exception e) {
                String2.log(MustBe.throwable(errorInMethod, e));
            }

            //print diagnostics
            if (reallyVerbose) {

                //read the data as a table
                //String2.log("  pre read table " + Math2.memoryString());
                Table table = new Table();
                table.read4DNc(fullStationFileName + randomInt, null, 1);

                String2.log("  post read table nRows=" + table.nRows() + 
                    " nCols=" + table.nColumns());
                String2.log(table.toString("row", 3));
                //print column data ranges
                for (int col = 0; col < table.nColumns(); col++) 
                    String2.log("col=" + col + " " + table.getColumn(col).statsString()); 

            }

        } catch (Exception e) {
            String2.log(MustBe.throwable(errorInMethod, e));
        } 

        //rename the file to the specified name
        if (success) 
             File2.rename(fullStationFileName + randomInt, fullStationFileName); //an existing new file will be deleted
        else File2.delete(fullStationFileName + randomInt);

        //diagnostic
        String2.log("  createNewCache time=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time) +
            "  success=" + success);
        return success;
    }
    
    /**
     * This deletes any existing cache and creates a new cache file 
     * (.nc file with each of the specified variables stored as 4D array, 
     * with nLat=1 and nLon=1).
     * This ensures the time values (column 0) are ascending (needed for binary searches)
     * This won't throw an exception.
     *
     * <p>Note that in my CWBrowser setup, only one program (hence one thread),
     * CWBrowserSA, is responsible for calling createNewCache and/or updateCache.
     *
     * @return true if all went well and there is a cache file 
     */
    public boolean createNewCache() {

        String2.log("CacheOpendapStation.createNewCache\n  fileName=" + fullStationFileName);
        boolean success = false;
        long time = System.currentTimeMillis();
        String errorInMethod = String2.ERROR + 
            " in CacheOpendapStation.createNewCache\n  fileName=" + fullStationFileName + 
            "\n  url=" + url + "\n  ";

        //delete the existing cache
        File2.delete(fullStationFileName);

        //open the .nc file
        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        try {
            //open the opendap url
            DODSNetcdfFile in = new DODSNetcdfFile(url); //bug? showed no global attributes //but fixed in 2.2.16
            //NetcdfDataset in = NetcdfDataset.openDataset(url); //can't recognize mbari non-udunits

            try {
                //open the file (before 'try'); if it fails, no temp file to delete
                NetcdfFileWriter out = NetcdfFileWriter.createNew(
                    NetcdfFileWriter.Version.netcdf3, fullStationFileName + randomInt);
                
                try {
                    Group outRootGroup = out.addGroup(null, "");
                    out.setFill(false);

                    //transfer global attributes  (with updated 'history' attribute)
                    List globalAttList = in.getGlobalAttributes();
                    int nGlobalAtt = globalAttList.size();
                    if (verbose) String2.log("  nGlobalAtt=" + nGlobalAtt);
                    String history = null;
                    String newHistory = "Data from " + url;
                    for (int a = 0; a < nGlobalAtt; a++) {
                        ucar.nc2.Attribute att = (ucar.nc2.Attribute)globalAttList.get(a);
                        //if (verbose) String2.log("  globalAtt name=" + att.getName());
                        Array values = att.getValues();
                        if (att.getName().equals("history")) {
                            history = PrimitiveArray.factory(NcHelper.getArray(values)).getString(0) +
                                "\n" + newHistory;
                            out.addGroupAttribute(outRootGroup, new ucar.nc2.Attribute(att.getName(), history));
                        } else {
                            out.addGroupAttribute(outRootGroup, new ucar.nc2.Attribute(att.getName(), values));
                        }
                    }
                    if (history == null) 
                        out.addGroupAttribute(outRootGroup, new ucar.nc2.Attribute("history", newHistory));

                    //get the first variable from in
                    Variable variable0 = in.findVariable(variableNames[0]);
                    
                    //get its dimensions  
                    //and add global attribute OPENDAP_TIME_DIMENSION_SIZE
                    List inDimList0 = variable0.getDimensions();
                    Dimension timeDimension = (Dimension)inDimList0.get(0);
                    opendapTimeDimensionSize = timeDimension.getLength();
                    outRootGroup.addAttribute(new ucar.nc2.Attribute(
                        OPENDAP_TIME_DIMENSION_SIZE, 
                        new Integer(opendapTimeDimensionSize)));

                    //for each variable
                    HashSet<String> dimNameHashSet = new HashSet();
                    ArrayList<Variable> inCumulativeVariableList = new ArrayList();
                    ArrayList<Variable> outCumulativeVariableList = new ArrayList();
                    int variableInColumn[] = new int[variableNames.length];
                    String latName = null;
                    String lonName = null;
                    String timeName = null;
                    Variable newVars[] = new Variable[variableNames.length];
                    for (int v = 0; v < variableNames.length; v++) {

                        //get the variable
                        Variable inVariable = in.findVariable(variableNames[v]);
                        Test.ensureNotNull(inVariable, errorInMethod + "variable not found: " + variableNames[v]);
                        boolean isStringVariable = inVariable.getDataType() == DataType.CHAR;

                        //get dimensions
                        List<Dimension> inDimList = inVariable.getDimensions();
                        Test.ensureTrue(
                            (inDimList.size() == 4 && !isStringVariable) ||
                            (inDimList.size() == 5 &&  isStringVariable), 
                            errorInMethod + 
                                "  inDimList.size=" + inDimList.size() + 
                                " for variable=" + inVariable.getName() + 
                                " isStringVariable=" + isStringVariable); 
                        Dimension newDims[] = new Dimension[inDimList.size()];
                        Variable newDimVars[] = new Variable[inDimList.size()];
                        for (int d = 0; d < inDimList.size(); d++) {
                            Dimension dim = inDimList.get(d);
                            String dimName = dim.getName();

                            //first 4 dimensions must be same as for variable0
                            if (d < 4)
                                Test.ensureEqual(dimName, ((Dimension)inDimList0.get(d)).getName(), 
                                    errorInMethod + "  unexpected dimension #" + d + 
                                    " for variable=" + inVariable.getName()); 

                            //add the dimension
                            if (!dimNameHashSet.contains(dimName)) {
                                dimNameHashSet.add(dimName);
                                newDims[d] = out.addDimension(outRootGroup, 
                                    dimName, dim.getLength(), true, //shared
                                    dim.isUnlimited(), false);  //isUnknownLength

                                //add the related variable (5th/char dimension doesn't have a variable(?))
                                if (d < 4) {
                                    if (d == 0) timeName = dimName;
                                    if (d == 2) latName = dimName;
                                    if (d == 3) lonName = dimName;
                                    Variable inDimVariable = in.findVariable(dimName);
                                    inCumulativeVariableList.add(inDimVariable);
                                    newDimVars[d] = out.addVariable(outRootGroup, dimName, 
                                        inDimVariable.getDataType(), Arrays.asList(dim)); 
                                    outCumulativeVariableList.add(newDimVars[d]);

                                    //write the related variable's attributes   (after adding the variable)
                                    List attList = inDimVariable.getAttributes();
                                    int nAtt = attList.size();
                                    for (int a = 0; a < nAtt; a++) {
                                        ucar.nc2.Attribute att = 
                                            (ucar.nc2.Attribute)attList.get(a);
                                        newDimVars[d].addAttribute(att);                                 
                                    }

                                    //test that nLat and nLon are 1
                                    if (d == 2)
                                        Test.ensureEqual(dim.getLength(), 1, 
                                            errorInMethod + "lat dimension (" + dimName + ") length isn't 1!");
                                    if (d == 3)
                                        Test.ensureEqual(dim.getLength(), 1, 
                                            errorInMethod + "lon dimension (" + dimName + ") length isn't 1!");
                                }
                            }
                        }

                        //add the variable to 'out'
                        newVars[v] = out.addVariable(outRootGroup, inVariable.getName(), 
                            inVariable.getDataType(), inDimList); 
                        inCumulativeVariableList.add(inVariable);
                        outCumulativeVariableList.add(newVars[v]);
                        variableInColumn[v] = inCumulativeVariableList.size() - 1;
                        //ensure variableInColumn[v] == 4 + v
                        //this affects whether all columns are updated in updateCache
                        Test.ensureEqual(variableInColumn[v], 4 + v,
                            errorInMethod + "unexpected variableInColumn for v=" + v);

                        //write Attributes   (after adding the variable)
                        List attList = inVariable.getAttributes();
                        int nAtt = attList.size();
                        for (int a = 0; a < nAtt; a++) {
                            ucar.nc2.Attribute att = (ucar.nc2.Attribute)attList.get(a);
                            newVars[v].addAttribute(att);
                        }
                    }

                    //leave "define" mode
                    if (verbose) String2.log("  leaving 'define' mode");
                    out.create();

                    //write the data
                    for (int v = 0; v < inCumulativeVariableList.size(); v++) {
                        Variable inVariable  = inCumulativeVariableList.get(v);
                        Variable outVariable = outCumulativeVariableList.get(v);
                        if (verbose) String2.log("  writing data var=" + inVariable.getName());

                        //write the data
                        //??? what if data has been added to variable since dimensions were defined???
                        Array array = inVariable.read();
                        out.write(outVariable, array);

                        //ensure that all time values are reasonable 
                        //mbari opendap server sometimes returns gibberish values
                        if (inVariable.getName().equals(timeName)) {

                            //ensure time values are ascending
                            DoubleArray timeDA = new DoubleArray(NcHelper.toDoubleArray(array));
                            double dNew = timeDA.getDouble(0); //nc files have at least one row
                            Test.ensureTrue(dNew > -1e15 && dNew < 1e15, //allows for millis since 1970 and much more
                                errorInMethod + "first time value=" + dNew + " is outside +/-1e15!");
                            int nRows = timeDA.size();
                            for (int row = 1; row < nRows; row++) {
                                double dOld = dNew;
                                dNew = timeDA.getDouble(row);
                                if (!Math2.isFinite(dNew) || dNew <= dOld)
                                    Test.error(errorInMethod + "time(row=" + row + ")=" + dNew + 
                                        " is less than or equal to time(row-1)=" + dOld + 
                                        "\ntimes=" + timeDA);
                            }
                            Test.ensureTrue(dNew > -1e15 && dNew < 1e15, //allows for millis since 1970 and much more
                                errorInMethod + "last time value=" + dNew + " is outside +/-1e15!");
                        }

                        //ensure that all lat and lon values are reasonable 
                        //mbari opendap server sometimes returns gibberish values
                        if (inVariable.getName().equals(lonName) ||
                            inVariable.getName().equals(latName)) {
                            DoubleArray da = new DoubleArray(NcHelper.toDoubleArray(array));
                            double stats[] = da.calculateStats();
                            double daMin = stats[PrimitiveArray.STATS_MIN];
                            double daMax = stats[PrimitiveArray.STATS_MAX];
                            String testString = "latitude";
                            double testMin = -90;
                            double testMax = 90;
                            if (inVariable.getName().equals(lonName)) {
                                testString = "longitude";
                                testMin = -180;
                                testMax = 360;
                            }
                            Test.ensureTrue(daMin >= testMin && daMax <= testMax,
                                errorInMethod + testString + " values must be between " + 
                                    testMin + " and " + testMax + ".\nvalues=" + da);
                        }


                    }

                    //I care about exception from this 
                    out.close();

                } catch (Exception e) {
                    try {
                        out.close();
                    } catch (Exception e2) {
                        //don't care
                    }
                    throw e;
                }
 
                //I care about exception from this
                in.close();

                success = true;

            } catch (Exception e) {

                try {
                    in.close();
                } catch (Exception e2) {
                    //don't care
                }
                throw e;
            }

            //print diagnostics
            if (reallyVerbose) {

                //read the data as a table
                //String2.log("  pre read table " + Math2.memoryString());
                Table table = new Table();
                table.read4DNc(fullStationFileName + randomInt, null, 1, null, -1);

                String2.log("  post read table nRows=" + table.nRows() + 
                    " nCols=" + table.nColumns());
                String2.log(table.toString("row", 3));
                //print column data ranges
                for (int col = 0; col < table.nColumns(); col++) 
                    String2.log("col=" + col + " " + table.getColumn(col).statsString()); 

            }

        } catch (Exception e) {
            String2.log(MustBe.throwable(errorInMethod, e));
        } 

        //rename the file to the specified name
        if (success) 
             File2.rename(fullStationFileName + randomInt, fullStationFileName); //an existing new file will be deleted
        else File2.delete(fullStationFileName + randomInt);

        //diagnostic
        String2.log("  createNewCache time=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time) +
            "  success=" + success);
        return success;
    }
    
    
    /**
     * This updates the cache file (if there is new opendap data)
     * or calls createNewCache if the cache file doesn't exist.
     * This won't throw an exception.
     *
     * <p>Note that in my CWBrowser setup, only one program (hence one thread),
     * CWBrowserSA, is responsible for calling createNewCache and/or updateCache.
     *
     * @return true if all went well and there is a cache file 
     */
    public boolean updateCache() {

        //need to create cache?
        if (!File2.isFile(fullStationFileName)) {
            return createNewCache();
        }

        long time = System.currentTimeMillis();
        String2.log("CacheOpendapStation.updateCache\n  fileName=" + fullStationFileName);
        String errorInMethod = String2.ERROR + 
            " in CacheOpendapStation.updateCache\n  fileName=" + fullStationFileName + "\n";
        boolean success = false;
        int nNewRows = 0;

        //open the .nc file
        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        try {

            //opendap get time dimension  -- is there new data?
            //String2.log("  pre read dodsNetcdf ");
            long inTime = System.currentTimeMillis();
            DConnect dConnect = new DConnect(url, true, 1, 1); 
            DDS dds = dConnect.getDDS(OpendapHelper.DEFAULT_TIMEOUT);
            String2.log("  post create dConnect,DDS  time=" + 
                (System.currentTimeMillis() - inTime));

            //are there more time values than before?
            inTime = System.currentTimeMillis();
            DGrid dGrid = (DGrid)dds.getVariable(variableNames[0]); 
            DArray timeDArray = (DArray)dGrid.getVar(1); //0 is data, 1 is time
            String2.log("  timeDArray name=" + timeDArray.getName());
            DArrayDimension timeDimension = timeDArray.getDimension(0); 
            String2.log("  timeDimension name=" + timeDimension.getName());
            int newOpendapTimeDimensionSize = timeDimension.getSize();
            inTime = System.currentTimeMillis() - inTime;

            //get old opendapTimeDimensionSize, if not known
            if (opendapTimeDimensionSize < 0) {
                NetcdfFile stationNcFile = NcHelper.openFile(fullStationFileName);
                try {
                    //add the opendapTimeDimensionSize
                    PrimitiveArray otdsPa = NcHelper.getGlobalAttribute(
                        stationNcFile, OPENDAP_TIME_DIMENSION_SIZE);
                    opendapTimeDimensionSize = otdsPa == null? 0 : otdsPa.getInt(0);
                    if (verbose) String2.log("  opendapTimeDimensionSize=" + opendapTimeDimensionSize);

                    //I care about this exception
                    stationNcFile.close();

                } catch (Exception e) {
                    try {
                        stationNcFile.close(); //make sure it is explicitly closed
                    } catch (Exception e2) {
                        //don't care
                    }
                    throw e;
                }
            }
            if (verbose) String2.log("  timeDim length old=" + opendapTimeDimensionSize + 
                " new=" + newOpendapTimeDimensionSize + " time=" + inTime + " ms");

            if (newOpendapTimeDimensionSize > opendapTimeDimensionSize) {

                //read the data from the cache file
                String2.log("  read4DNc...");
                //String2.log("  pre read table ");
                Table table = new Table();
                table.read4DNc(fullStationFileName, null, 1, null, -1);
                int oldNRows = table.nRows(); //will be different from opendapTimeDimensionSize because flattened
                PrimitiveArray timeColumn = table.getColumn(3);
                String2.log("  table from cache file nRows=" + oldNRows + 
                    " nCols=" + table.nColumns());

                //get the depths, lat, and lon from the file (if don't have already).
                //Getting from the file is good. It ensures that the
                //original depth, lat, and lon values are returned with each update.
                if (Double.isNaN(lon)) {
                    lon = table.getColumn(0).getDouble(0);
                    if (verbose) String2.log("  file lon=" + lon);
                }
                if (Double.isNaN(lat)) {
                    lat = table.getColumn(1).getDouble(0);
                    if (verbose) String2.log("  file lat=" + lat);
                }
                if (depths == null) {
                    IntArray ranks = new IntArray(); //just holds intermediate values
                    depths = table.getColumn(2).makeIndices(ranks);
                    if (verbose) String2.log("  file depths=" + depths);
                }

                //get new time values from opendap for each of the 4D variables
                //!!Be sure to use same time constraint for all opendap requests below,
                // because it takes time to get all the data and more time values
                // may be added in the process (don't get them yet!)
                String constraint = 
                    "[" + opendapTimeDimensionSize + ":1:" + (newOpendapTimeDimensionSize - 1) + "]";
                String2.log(url + "?" + table.getColumnName(3) + constraint);
                PrimitiveArray newTimes = OpendapHelper.getPrimitiveArray(dConnect, 
                    "?" + table.getColumnName(3) + constraint); 
                Test.ensureEqual(newTimes.size(), newOpendapTimeDimensionSize - opendapTimeDimensionSize,
                    errorInMethod + "Unexpected newTimes.size.");
                //ensure times are greater than previous and ascending
                double newTime = timeColumn.getDouble(timeColumn.size() - 1); //the last time before this data reading
                for (int i = 0; i < newTimes.size(); i++) {
                    //don't use Test.ensure since it would generate lots of strings needlessly
                    double oldTime = newTime;
                    newTime = newTimes.getDouble(i);
                    if (!Math2.isFinite(newTime) || newTime > 1e15 || newTime <= oldTime)
                        Test.error(errorInMethod + "newTime(" + newTime + 
                            ") is less than or equal to previous time(" + oldTime + 
                            ") when i=" + i);
                }
                if (verbose) String2.log(
                    (reallyVerbose? "times=" + newTimes : "nTimes=" + newTimes.size()) +
                    "\ndepths=" + depths +
                    "\nlat=" + lat + " lon=" + lon);

                //get the new data for each variable
                //!!This takes some time
                constraint += //time constraint already done
                    "[0:1:" + (depths.size() - 1) + "]" +
                    "[0:1:0]" +
                    "[0:1:0]";
                PrimitiveArray dataPA[] = new PrimitiveArray[variableNames.length];
                for (int var = 0; var < variableNames.length; var++) {
                    if (verbose) String2.log("  getting data from opendap for " + variableNames[var]
                        + " url=\n" + url + "?" + variableNames[var] + constraint
                        );
                    PrimitiveArray pas[] = OpendapHelper.getPrimitiveArrays(dConnect, 
                        "?" + variableNames[var] + constraint); 

                    //ensure times, depths, lat, lon are as expected
                    //This is very important, as I have seen gibberish values from mbari opendap server.
                    Test.ensureEqual(pas[1], newTimes,    //1,2,3,4 since 0 is data, 1=time, 2=depths, ...
                        errorInMethod + "Unexpected times for " + variableNames[var]);
                    Test.ensureEqual(pas[2], depths,    
                        errorInMethod + "Unexpected depths for " + variableNames[var]);
                    Test.ensureEqual(pas[3].getDouble(0), lat, 
                        errorInMethod + "Unexpected lat for " + variableNames[var]);
                    Test.ensureEqual(pas[4].getDouble(0), lon, 
                        errorInMethod + "Unexpected lon for " + variableNames[var]);

                    dataPA[var] = pas[0]; 
                    Test.ensureEqual(dataPA[var].size(), 
                        (newOpendapTimeDimensionSize - opendapTimeDimensionSize) * depths.size(), 
                        errorInMethod + "Unexpected dataPA size for var=" + var);
                }
               
                //add rows to table
                int po = 0;
                for (int t = 0; t < newOpendapTimeDimensionSize - opendapTimeDimensionSize; t++) {
                    for (int depth = 0; depth < depths.size(); depth++) {
                        table.getColumn(0).addDouble(lon);
                        table.getColumn(1).addDouble(lat);
                        table.getColumn(2).addDouble(depths.getDouble(depth));
                        table.getColumn(3).addDouble(newTimes.getDouble(t));
                        for (int v = 0; v < variableNames.length; v++) 
                            //4+v is validated by variableInColumn test in createCache
                            table.getColumn(4 + v).addDouble(dataPA[v].getDouble(po));
                    }
                    po++;
                }

                //save table    with very minimal changes to attributes
                opendapTimeDimensionSize = newOpendapTimeDimensionSize;
                String2.log("  storing newOpendapTimeDimensionSize=" + newOpendapTimeDimensionSize);
                table.globalAttributes().set(OPENDAP_TIME_DIMENSION_SIZE, 
                    newOpendapTimeDimensionSize);
                table.saveAs4DNc(fullStationFileName, 0, 1, 2, 3, null, null, null);
                nNewRows = table.nRows() - oldNRows;
                if (verbose) {
                    //String2.log(table.toString("row", 3));
                    //print column data ranges
                    //for (int col = 0; col < table.nColumns(); col++) 
                    //    String2.log("col=" + col + " " + table.getColumn(col).statsString()); 
                }

            }
            success = true;


        } catch (Exception e) {
            String2.log(MustBe.throwable(errorInMethod, e));
        }


        String2.log("  updateCache finished; time=" + 
            Calendar2.elapsedTimeString(System.currentTimeMillis() - time) +
            " nNewRows=" + nNewRows + " success=" + success);
        return success;
    }      

   
    /** This test the mbari opendap server for reliability. 
     * Basically, this ensures that the response has the right depth, lat and lon values.
     */
    public static void testMbariOpendapReliability() throws Exception {
        verbose = true;
        DataHelper.verbose = true;
        Table.verbose = true;
        StationVariableNc4D.verbose = true;
        PointDataSet.verbose = true;

        Random random = new Random();
        String url, response, match;
        int rep = 0;


        while (true) {
            //adcp stations chosen because I think they give gibberish answers sometimes (ascii and opendap)
            String2.log("\n*** CacheOpendapStation.test rep=" + rep + " " + 
                Calendar2.getCurrentISODateTimeStringLocal());
            if (rep > 0) Math2.incgc(5000); //5 seconds    //in a test
            //first time, ensure there are at least 3000 time points; then pick time randomly
            int randomInt = rep == 0? 3000 : random.nextInt(3000); 
            rep++;

            //***************
            //M0: get ascii response
            url = "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc.ascii?" + 
                "u_component_uncorrected[" + randomInt + ":1:" + randomInt + "][0:1:1][0:1:0][0:1:0]," +
                "v_component_uncorrected[" + randomInt + ":1:" + randomInt + "][0:1:1][0:1:0][0:1:0]";
            String2.log("url=" + url);
            try { 
                response = SSR.getUrlResponseString(url);
                String2.log("response=" + response);
                match = "u_component_uncorrected, [1][2][1][1]\n" +
                    "[0][0][0], ";
                Test.ensureTrue(response.startsWith(match), 
                    "Start of M0 response doesn't match: " + match);
                match = "depth, [2]\n" +
                    "6, 10\n" +
                    "adcp_latitude, [1]\n" +
                    "36.833333\n" +
                    "adcp_longitude, [1]\n" +
                    "-121.903333\n" +
                    "\n" +
                    "v_component_uncorrected, [1][2][1][1]\n" +
                    "[0][0][0],";
                Test.ensureTrue(response.indexOf(match) > 0, "Middle of M0 doesn't match: " + match);
                match = "depth, [2]\n" +
                    "6, 10\n" +
                    "adcp_latitude, [1]\n" +
                    "36.833333\n" +
                    "adcp_longitude, [1]\n" +
                    "-121.903333\n" +
                    "\n";
                Test.ensureTrue(response.endsWith(match), "End of M0 doesn't match: " + match);
            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }

            //****************
            //M1: get ascii response
            url = "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc.ascii?" + 
                "u_component_uncorrected[" + randomInt + ":1:" + randomInt + "][0:1:1][0:1:0][0:1:0]," +
                "v_component_uncorrected[" + randomInt + ":1:" + randomInt + "][0:1:1][0:1:0][0:1:0]";
            String2.log("url=" + url);
            try {
                response = SSR.getUrlResponseString(url);
                String2.log("response=" + response);
                match = "u_component_uncorrected, [1][2][1][1]\n" +
                    "[0][0][0], ";
                Test.ensureTrue(response.startsWith(match), 
                    "Start of M1 response doesn't match: " + match);
                match = "depth, [2]\n" +
                    "15.04, 23.04\n" +
                    "adcp_latitude, [1]\n" +
                    "36.764\n" +
                    "adcp_longitude, [1]\n" +
                    "-122.046\n" +
                    "\n" +
                    "v_component_uncorrected, [1][2][1][1]\n" +
                    "[0][0][0],";
                Test.ensureTrue(response.indexOf(match) > 0, "Middle of M1 doesn't match: " + match);
                match = "depth, [2]\n" +
                    "15.04, 23.04\n" +
                    "adcp_latitude, [1]\n" +
                    "36.764\n" +
                    "adcp_longitude, [1]\n" +
                    "-122.046\n" +
                    "\n";
                Test.ensureTrue(response.endsWith(match), "End of M1 doesn't match: " + match);
            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }


            //****************
            //M2: get ascii response
            url = "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc.ascii?" + 
                "u_component_uncorrected[" + randomInt + ":1:" + randomInt + "][0:1:1][0:1:0][0:1:0]," +
                "v_component_uncorrected[" + randomInt + ":1:" + randomInt + "][0:1:1][0:1:0][0:1:0]";
            String2.log("url=" + url);
            try {
                response = SSR.getUrlResponseString(url);
                String2.log("response=" + response);
                match = "u_component_uncorrected, [1][2][1][1]\n" +
                    "[0][0][0], ";
                Test.ensureTrue(response.startsWith(match), 
                    "Start of M2 response doesn't match: " + match);
                match = "depth, [2]\n" +
                    "15.04, 23.04\n" +
                    "adcp_latitude, [1]\n" +
                    "36.69\n" +
                    "adcp_longitude, [1]\n" +
                    "-122.338\n" +
                    "\n" +
                    "v_component_uncorrected, [1][2][1][1]\n" +
                    "[0][0][0],";
                Test.ensureTrue(response.indexOf(match) > 0, "Middle of M2 doesn't match: " + match);
                match = "depth, [2]\n" +
                    "15.04, 23.04\n" +
                    "adcp_latitude, [1]\n" +
                    "36.69\n" +
                    "adcp_longitude, [1]\n" +
                    "-122.338\n" +
                    "\n";
                Test.ensureTrue(response.endsWith(match), "End of M2 doesn't match: " + match);
            } catch (Exception e) {
                String2.log(MustBe.throwableToString(e));
            }

        }
    }

    /** This tests using this class to create a cache file. */
    public static void test() throws Exception {
        verbose = true;
        DataHelper.verbose = true;
        Table.verbose = true;
        StationVariableNc4D.verbose = true;
        PointDataSet.verbose = true;

        String fileName;
        CacheOpendapStation cos;
        String response;
        Table table = new Table();


        //adcp stations chosen because I think they give gibberish answers sometimes (ascii and opendap)
        String2.log("\n*** CacheOpendapStation.test");

        //***************
        //M0: get ascii response
        try {
            response = SSR.getUrlResponseString(
                "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc.ascii?" + 
                "u_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0],v_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0]");
            Test.ensureEqual(response,
/* pre 6/22/2007, was
                "u_component_uncorrected, [1][2][1][1]\n" +
                "[0][0][0], 20.3\n" +
                "[0][1][0], 6.9\n" +
                "time, [1]\n" +
                "1154396111\n" +
                "depth, [2]\n" +
                "6, 10\n" +
                "latitude, [1]\n" + //was adcp_latitude
                "36.833333\n" +
                "longitude, [1]\n" + //was adcp_longitude
                "-121.903333\n" +
                "\n" +
                "v_component_uncorrected, [1][2][1][1]\n" +
                "[0][0][0], -24.3\n" +
                "[0][1][0], -8.2\n" +
                "time, [1]\n" +
                "1154396111\n" +
                "depth, [2]\n" +
                "6, 10\n" +
                "latitude, [1]\n" + //was adcp_latitude
                "36.833333\n" +
                "longitude, [1]\n" + //was adcp_longitude
                "-121.903333\n" +
                "\n", */
//post 2010-07-19 is
"Dataset: m0_adcp1267_20060731.nc\n" +
"u_component_uncorrected.longitude, -121.903333\n" +
"u_component_uncorrected.u_component_uncorrected[u_component_uncorrected.time=1154396111][u_component_uncorrected.depth=6][u_component_uncorrected.latitude=36.833333], 20.3\n" +
"u_component_uncorrected.u_component_uncorrected[u_component_uncorrected.time=1154396111][u_component_uncorrected.depth=10][u_component_uncorrected.latitude=36.833333], 6.9\n" +
"v_component_uncorrected.longitude, -121.903333\n" +
"v_component_uncorrected.v_component_uncorrected[v_component_uncorrected.time=1154396111][v_component_uncorrected.depth=6][v_component_uncorrected.latitude=36.833333], -24.3\n" +
"v_component_uncorrected.v_component_uncorrected[v_component_uncorrected.time=1154396111][v_component_uncorrected.depth=10][v_component_uncorrected.latitude=36.833333], -8.2\n",
                "response=" + response);
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e)); 
        }

        //M0: make cache file 
        try {
            fileName = "c:/temp/MBARI_M0_NRT_adcp.nc";
            cos = new CacheOpendapStation( //throws exception if trouble
                "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc",
                fileName,
                new String[]{"u_component_uncorrected", "v_component_uncorrected"});
            Test.ensureTrue(cos.createNewCache(), "m0 adcp"); 

            //M0: compare first part of cache file to ascii response
            //***THE TEST WILL CHANGE IF THEY THROW OUT OLD NRT DATA.
            table.clear();
            table.read4DNc(fileName, null, 1, null, -1);
            //String2.log(table.toString("row", 10));
            Test.ensureEqual(table.nColumns(), 6, "");
            Test.ensureEqual(table.getColumnName(0), "longitude", ""); //was adcp_longitude
            Test.ensureEqual(table.getColumnName(1), "latitude", ""); //was adcp_latitude
            Test.ensureEqual(table.getColumnName(2), "depth", "");
            Test.ensureEqual(table.getColumnName(3), "time", "");
            Test.ensureEqual(table.getColumnName(4), "u_component_uncorrected", "");
            Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
            Test.ensureEqual(table.getFloatData(0,0), -121.903333f, "");
            Test.ensureEqual(table.getFloatData(1,0), 36.833333f, "");
            Test.ensureEqual(table.getFloatData(2,0), 6f, "");
            Test.ensureEqual(table.getDoubleData(3,0), 1154396111, "");
            Test.ensureEqual(table.getFloatData(4,0), 20.3f, "");
            Test.ensureEqual(table.getFloatData(5,0), -24.3f, "");

            Test.ensureEqual(table.getFloatData(0,1), -121.903333f, "");
            Test.ensureEqual(table.getFloatData(1,1), 36.833333f, "");
            Test.ensureEqual(table.getFloatData(2,1), 10f, "");
            Test.ensureEqual(table.getDoubleData(3,1), 1154396111, "");
            Test.ensureEqual(table.getFloatData(4,1), 6.9f, "");
            Test.ensureEqual(table.getFloatData(5,1), -8.2f, "");
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e)); 
        }

/* M1 and M2 tests work, but slow and no need to do all the time
        //****************
        //M1: get ascii response
        //***THE TEST WILL CHANGE IF THEY THROW OUT OLD NRT DATA.
        response = String2.toNewlineString(SSR.getUrlResponse(
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc.ascii?" + 
            "u_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0],v_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0]"));
        Test.ensureEqual(response,
            "u_component_uncorrected, [1][2][1][1]\n" +
            "[0][0][0], -20.1\n" +
            "[0][1][0], -18.6\n" +
            "time, [1]\n" +
            "1129838400\n" +
            "depth, [2]\n" +
            "15.04, 23.04\n" +
            "adcp_latitude, [1]\n" +
            "36.764\n" +
            "adcp_longitude, [1]\n" +
            "-122.046\n" +
            "\n" +
            "v_component_uncorrected, [1][2][1][1]\n" +
            "[0][0][0], 0.2\n" +
            "[0][1][0], -10.9\n" +
            "time, [1]\n" +
            "1129838400\n" +
            "depth, [2]\n" +
            "15.04, 23.04\n" +
            "adcp_latitude, [1]\n" +
            "36.764\n" +
            "adcp_longitude, [1]\n" +
            "-122.046\n" +
            "\n", "");

        //M1: make cache file 
        fileName = "c:/temp/MBARI_M1_NRT_adcp.nc";
        cos = new CacheOpendapStation(
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc",
            fileName,
            new String[]{"u_component_uncorrected", "v_component_uncorrected"});
        Test.ensureTrue(cos.createNewCache(), "m1 adcp"); 

        //M1: compare first part of cache file to ascii response
        table.clear();
        table.read4DNc(fileName, null, 1);
        //String2.log(table.toString("row", 10));
        Test.ensureEqual(table.nColumns(), 6, "");
        Test.ensureEqual(table.getColumnName(0), "adcp_longitude", "");
        Test.ensureEqual(table.getColumnName(1), "adcp_latitude", "");
        Test.ensureEqual(table.getColumnName(2), "depth", "");
        Test.ensureEqual(table.getColumnName(3), "time", "");
        Test.ensureEqual(table.getColumnName(4), "u_component_uncorrected", "");
        Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
        Test.ensureEqual(table.getFloatData(0,0), -122.046f, "");
        Test.ensureEqual(table.getFloatData(1,0), 36.764f, "");
        Test.ensureEqual(table.getFloatData(2,0), 15.04f, "");
        Test.ensureEqual(table.getDoubleData(3,0), 1129838400, "");
        Test.ensureEqual(table.getFloatData(4,0), -20.1f, "");
        Test.ensureEqual(table.getFloatData(5,0), 0.2f, "");

        Test.ensureEqual(table.getFloatData(0,1), -122.046f, "");
        Test.ensureEqual(table.getFloatData(1,1), 36.764f, "");
        Test.ensureEqual(table.getFloatData(2,1), 23.04f, "");
        Test.ensureEqual(table.getDoubleData(3,1), 1129838400, "");
        Test.ensureEqual(table.getFloatData(4,1), -18.6f, "");
        Test.ensureEqual(table.getFloatData(5,1), -10.9f, "");

        //****************
        //M2: get ascii response
        //***THE TEST WILL CHANGE IF THEY THROW OUT OLD NRT DATA.
        response = String2.toNewlineString(SSR.getUrlResponse(
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc.ascii?" + 
            "u_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0],v_component_uncorrected[0:1:0][0:1:1][0:1:0][0:1:0]"));
        Test.ensureEqual(response,
            "u_component_uncorrected, [1][2][1][1]\n" +
            "[0][0][0], -10.8\n" +
            "[0][1][0], -10\n" +
            "time, [1]\n" +
            "1143752400\n" +
            "depth, [2]\n" +
            "15.04, 23.04\n" +
            "adcp_latitude, [1]\n" +
            "36.69\n" +
            "adcp_longitude, [1]\n" +
            "-122.338\n" +
            "\n" +
            "v_component_uncorrected, [1][2][1][1]\n" +
            "[0][0][0], -18.8\n" +
            "[0][1][0], -12.9\n" +
            "time, [1]\n" +
            "1143752400\n" +
            "depth, [2]\n" +
            "15.04, 23.04\n" +
            "adcp_latitude, [1]\n" +
            "36.69\n" +
            "adcp_longitude, [1]\n" +
            "-122.338\n" +
            "\n", "");

        //M2: make cache file 
        fileName = "c:/temp/MBARI_M1_NRT_adcp.nc";
        cos = new CacheOpendapStation(
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc",
            fileName,
            new String[]{"u_component_uncorrected", "v_component_uncorrected"});
        Test.ensureTrue(cos.createNewCache(), "m2 adcp"); 

        //M2: compare first part of cache file to ascii response
        table.clear();
        table.read4DNc(fileName, null, 1);
        //String2.log(table.toString("row", 10));
        Test.ensureEqual(table.nColumns(), 6, "");
        Test.ensureEqual(table.getColumnName(0), "adcp_longitude", "");
        Test.ensureEqual(table.getColumnName(1), "adcp_latitude", "");
        Test.ensureEqual(table.getColumnName(2), "depth", "");
        Test.ensureEqual(table.getColumnName(3), "time", "");
        Test.ensureEqual(table.getColumnName(4), "u_component_uncorrected", "");
        Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
        Test.ensureEqual(table.getFloatData(0,0), -122.338f, "");
        Test.ensureEqual(table.getFloatData(1,0), 36.69f, "");
        Test.ensureEqual(table.getFloatData(2,0), 15.04f, "");
        Test.ensureEqual(table.getDoubleData(3,0), 1143752400, "");
        Test.ensureEqual(table.getFloatData(4,0), -10.8f, "");
        Test.ensureEqual(table.getFloatData(5,0), -18.8f, "");

        Test.ensureEqual(table.getFloatData(0,1), -122.338f, "");
        Test.ensureEqual(table.getFloatData(1,1), 36.69f, "");
        Test.ensureEqual(table.getFloatData(2,1), 23.04f, "");
        Test.ensureEqual(table.getDoubleData(3,1), 1143752400, "");
        Test.ensureEqual(table.getFloatData(4,1), -10f, "");
        Test.ensureEqual(table.getFloatData(5,1), -12.9f, "");

*/    
    }



}
