/* 
 * PointDataSetFromStationVariables Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
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
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
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
//import dods.dap.*;

/** 
 * This class represents one point dataset, made from several GroupVariables,
 * which have data for the same variable from different groups (stations or trajectories).
 * It can generate a table with the data within a specific X,Y,Z,T bounding box.
 *
 * The constructor searches for available data.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-07-20
 */
public class PointDataSetFromStationVariables extends PointDataSet { 

    protected GroupVariableWithDepthLevels groupVariables[];
    protected String uniqueDepthLevels[];
    protected String udUnits;
    public static boolean inDevelopment = false; //this really slows it down!

    /**
     * This is a convenience method: this is the highest level generic way to create 
     * PointDataSets and add them to activePointDataSets.
     *
     * @param pointFilesDir the points directory (to which "stationBaseName/" will be added)
     * @param stationBaseName e.g., MBARI
     * @param stationTypes [stationType]  e.g., e.g., adcp, met, ctd.  Used as part of cache file name.
     * @param variableInfo [stationType][variable] returns a "`" separated list of info
     * @param courtesy a short courtesy string (e.g., MBARI)
     * @param minX the min longitude of interest
     * @param maxX the max longitude of interest
     * @param minY the min latitude of interest
     * @param maxY the max latitude of interest
     * @param activePointDataSets the list that valid pointDataSets will be added to
     */
    public static void makeGroupsOfPointDataSets(
        String pointFilesDir, String stationBaseName, String stationTypes[],
        String variableInfo[][], String courtesy,
        double minX, double maxX, double minY, double maxY, List activePointDataSets) {

        for (int type = 0; type < stationTypes.length; type++) {
            makePointDataSets(activePointDataSets,
                stationBaseName,
                pointFilesDir + stationBaseName + "/", 
                stationBaseName + "_.+" + stationTypes[type] + "\\.nc", //file name regex   MBARI_M0adcp.nc
                variableInfo[type],
                courtesy, minX, maxX, minY, maxY); 
        }
    }

    /**
     * This is a convenience method: Given several opendap data sources, 
     * each representing a station with several variables, 
     * this makes several PointDataSetFromStationVariables 
     * (several PointDataSets each holding several StationVariables), 
     * one for each inFileVariableName.
     *
     * <p>All variables used here from a given station must use the same 
     *   time values, depth values, lat value and lon value.
     * <br>All stations must have all the variables used here.
     * <br>Time must have standard udUnits units, e.g., "seconds since 1970-01-01 00:00:00Z".
     * <br>The range of time values for each station can vary.
     * <br>Z must have "meters", "meter", or "m" (upper or lower case) units.
     *    Z should (but doesn't have to) have an attribute 'positive' with a value of 'up' or 'down', 
     *     but I will store z as positive=down values.
     *    Alternatively, globalAttributes "geospatial_vertical_positive" can be specified.
     * <br>Y must have units that contains "degrees_north" or a name
     *    that contains ("latitude").
     * <br>X must have units that contains "degrees_east" or a name
     *    that contains ("longitude").
     * 
     * @param activePointDataSets the list to which PointDataSetFromStationVariables will be added
     * @param userDataSetBaseName the name common to all these datasets 
     *    that will be shown to users e.g., "MBARI" 
     * @param stationDirectory is the directory containing the station data files.
     *    This should have a slash at the end, e.g., "c:/data/Nc4D/".
     * @param stationFileNameRegex is the regex used to identify the desired subset of 
     *    files in the stationDirectory, e.g., ".+\\.nc" for all .nc files,
     *    or "(31201.nc|46088.nc|TAML1.nc)" for just those 3 files.
     *    (See JavaDocs for java.util.regex.Pattern and Matcher.)
     *    The names of the files (minus the file's
     *    extension) will be used as the names of the stations.
     * @param variableInfo has a String for each variable, internally separated by "`",
     *    e.g., "AIR_TEMPERATURE_HR`  PMBatmp` Air Temperature`   Rainbow`      Linear` 1` -10`  40` degree_C".
     *    Its components are:
     *    <ol>
     *    <li> inFileVariableNames the names of the variables, each of which is in 
     *      each station's file (e.g., "AIR_TEMPERATURE_HR", )
     *    <li> internalVariableNames the 4 character internal variable names
     *      (e.g., "atmp")
     *    <li> userVariableNames the names of the variables that will be shown to users
     *      (e.g., "Air Temperature", ...).
     *      It is useful if they match values for other similar pointDatasets (e.g, from NDBC).
     *    <li> variablePalette the palette for each variable (e.g., "Rainbow")
     *      (must be one of palettes available to PointDataSets in the browser)
     *    <li> variableScale the scale for the colorbar for each variable 
     *      (e.g., "Linear" or "Log")
     *    <li> variableFactor the factor needed to get the raw data into standard udUnits.
     *    <li> variablePaletteMin the low value for the colorbar for each variable.
     *      It is useful if they match values for other similar pointDatasets (e.g, from NDBC).
     *    <li> variablePaletteMax the high value for the colorbar for each variable.
     *      It is useful if they match values for other similar pointDatasets (e.g, from NDBC).
     *    <li> variableUdUnits the udUnits for each variable.
     *       They could be read from the file,
     *       but variableFactor may be not 1 or the file may have non-udUnits units.
     *       Also, useful if the units exactly match the udUnits for other similar 
     *       pointDatasets (e.g, NDBC).
     *       See http://www.unidata.ucar.edu/software/udunits/udunits.txt .
     *    </ol>
     * @param courtesy  the group to credit for this data (usually 25 char or less)
     * @param minStationX the minimum acceptable station lon  (may be 0 - 360 or -180 - 180).
     *    min/maxStationX/Y only restrict which stations are kept.
     *    Data can still be accessed with lon values of 0 - 360 or -180 - 180.
     * @param maxStationX the maximum acceptable station lon  (may be 0 - 360 or -180 - 180).
     * @param minStationY the minimum acceptable station lat.
     * @param maxStationY the maximum acceptable station lat.
     * @throws Exception if procedural trouble (like incorrect number of items
     *     in variableInfo).
     *     This doesn't throw Exception if 0 files.
     */
    public static void makePointDataSets(
        List activePointDataSets, String userDataSetBaseName, 
        String stationDirectory, String stationFileNameRegex,  
        String variableInfo[], String courtesy, 
        double minStationX, double maxStationX, double minStationY, double maxStationY) {

        int originalListSize = activePointDataSets.size();
        if (verbose) String2.log("\n*** start PointDataSetFromStationVariables.makePointDataSets");
        String errorInMethod = String2.ERROR + 
            " in PointDataSetFromStationVariables.makePointDataSets(" + 
            userDataSetBaseName + "):\n";

        //get the stationFileNames and stationNames
        String stationFileNames[] = RegexFilenameFilter.fullNameList(stationDirectory, 
            stationFileNameRegex);
        int nStations = stationFileNames.length;
        String stationNames[] = new String[nStations]; //individual are set to null if inactive, e.g., invalid or x,y out of range
        for (int station = 0; station < nStations; station++) {
            stationNames[station] = File2.getNameNoExtension(stationFileNames[station]);
            stationNames[station] = String2.replaceAll(stationNames[station], '_', ' ');
            stationNames[station] = String2.combineSpaces(stationNames[station]);
        }
        if (verbose) String2.log("  nStations=" + nStations);
        if (nStations == 0)
            return;

        //parse the variableInfo
        //a String[] of, e.g.: "AIR_TEMPERATURE_HR`  atmp` Air Temperature`   Rainbow`      Linear` 1` -10`  40` degree_C"
        int nVars = variableInfo.length;
        String inFileVariableNames[]   = new String[nVars];
        String internalVariableNames[] = new String[nVars]; 
        String userVariableNames[]     = new String[nVars]; 
        String variablePalette[]       = new String[nVars];
        String variablePaletteScale[]  = new String[nVars]; 
        double variableFactor[]        = new double[nVars];
        double variablePaletteMin[]    = new double[nVars]; 
        double variablePaletteMax[]    = new double[nVars];
        String variableUdUnits[]       = new String[nVars]; 
        for (int var = 0; var < nVars; var++) {
            int nExpectedItems = 9; 
            String tList[] = String2.split(variableInfo[var], '`');
            Test.ensureEqual(tList.length, nExpectedItems, 
                errorInMethod + "variableInfo[" + var + "] doesn't have " + nExpectedItems + " items:\n" +
                    variableInfo[var]);
            for (int i = 0; i < nExpectedItems; i++)
                Test.ensureNotEqual(tList[i].length(), 0, 
                    errorInMethod + "variableInfo[" + var + "] has no info for item " + i + ".\ninfo=" +
                    variableInfo[var]);
            inFileVariableNames[var]   = tList[0];
            internalVariableNames[var] = tList[1]; 
            userVariableNames[var]     = tList[2]; 
            variablePalette[var]       = tList[3];
            variablePaletteScale[var]  = tList[4]; 
            variableFactor[var]        = String2.parseDouble(tList[5]); 
            variablePaletteMin[var]    = String2.parseDouble(tList[6]); //bad values will be caught
            variablePaletteMax[var]    = String2.parseDouble(tList[7]);
            variableUdUnits[var]       = tList[8]; 
        }

        long time = System.currentTimeMillis();


        //*** for each station
        double timeBaseSeconds[] = new double[nStations];
        double timeFactorToGetSeconds[] = new double[nStations];
        double timeIncrementInSeconds[] = new double[nStations];
        Arrays.fill(timeIncrementInSeconds, Double.NaN);
        double minT[] = new double[nStations];
        double maxT[] = new double[nStations];
        boolean stationZUp[] = new boolean[nStations];
        double stationY[] = new double[nStations]; 
        double stationX[] = new double[nStations]; //in-file x, not adjusted
        //each [station][var] may have different mv
        float sourceMissingValue[][] = new float[nStations][]; 
        //mbari files have different missing_value and _FillValue
        float sourceFillValue[][] = new float[nStations][]; 
        //each [station] may have different depthLevels (stored in m, positive down, same element order as in file)
        DoubleArray stationDepthLevels[] = new DoubleArray[nStations]; 
        Attributes globalAttributes = new Attributes();
        Attributes tAttributes = new Attributes();
        Attributes zAttributes = new Attributes();
        Attributes yAttributes = new Attributes();
        Attributes xAttributes = new Attributes();
        Attributes idAttributes = new Attributes();
        Attributes dataAttributes[] = new Attributes[nVars];
        Class elementClasses[] = new Class[nVars];
        for (int station = 0; station < nStations; station++) {
            long stationTime = System.currentTimeMillis();
            String tErrorInMethod = 
                "    (station baseName=" + userDataSetBaseName + " name=" + stationNames[station] + 
                "\n    " + stationFileNames[station] + ")";
            if (reallyVerbose) String2.log("\n" + tErrorInMethod);  
            sourceMissingValue[station] = new float[nVars];
            sourceFillValue[station] = new float[nVars];
            Arrays.fill(sourceMissingValue[station], Float.NaN);
            Arrays.fill(sourceFillValue[station], Float.NaN);

            try {
                //open the file
                //tTime = System.currentTimeMillis();
                NetcdfFile ncFile = NcHelper.openFile(stationFileNames[station]);
                //time ~150ms first time; 0 if in cache  if (reallyVerbose) String2.log("    get dGrid time=" + (System.currentTimeMillis() - tTime));  

                try {

                    //*** get the first dataVariable  and its dimensions
                    //(other variables use this info and must match this info)
                    //tTime = System.currentTimeMillis();
                    Variable dataVariable = ncFile.findVariable(inFileVariableNames[0]);  //null if not found
                    Test.ensureNotNull(dataVariable, //errorInMethod added below
                        inFileVariableNames[0] + 
                        " not found in " + stationFileNames[station]);
                    List dimensions = dataVariable.getDimensions();
                    Test.ensureEqual(dimensions.size(), 4, //errorInMethod added below 
                        "nDimensions wasn't 4 for " + inFileVariableNames[0] +
                        "\nin " + stationFileNames[station]);
                    if (station == 0) {
                        //just for first station
                        dataAttributes[0] = new Attributes();
                        NcHelper.getVariableAttributes(dataVariable, dataAttributes[0]); //other var atts read below
                        elementClasses[0] = NcHelper.getElementClass(dataVariable.getDataType());
                    }

                    //get the sourceMissingValue for var0
                    //for cacheOpendapStations, these are always -1e32
                    PrimitiveArray fvPa = NcHelper.getVariableAttribute(dataVariable, "_FillValue");
                    PrimitiveArray mvPa = NcHelper.getVariableAttribute(dataVariable, "missing_value");
                    if (fvPa != null) sourceFillValue[   station][0] = fvPa.getFloat(0);
                    if (mvPa != null) sourceMissingValue[station][0] = mvPa.getFloat(0);
                    if (inDevelopment && fvPa == null && mvPa == null) {
                        String2.log("WARNING: no missing_value or _FillValue metadata for data variable\n" + 
                            inFileVariableNames[0] + " for " + stationFileNames[station]);
                    }

                    //get the dimensions
                    Dimension tDimension = (Dimension)dimensions.get(0);
                    Dimension zDimension = (Dimension)dimensions.get(1);
                    Dimension yDimension = (Dimension)dimensions.get(2);
                    Dimension xDimension = (Dimension)dimensions.get(3);

                    //get the dimension names
                    String tDimensionName = tDimension.getName();
                    String zDimensionName = zDimension.getName();
                    String yDimensionName = yDimension.getName();
                    String xDimensionName = xDimension.getName();
                    if (reallyVerbose) String2.log(
                        "    xDimName=" + xDimensionName + 
                        " y=" + yDimensionName + 
                        " z=" + zDimensionName + 
                        " t=" + tDimensionName);

                    //get the associated variable
                    Variable tVariable = ncFile.findVariable(tDimensionName);  //null if not found
                    Variable zVariable = ncFile.findVariable(zDimensionName);  //null if not found
                    Variable yVariable = ncFile.findVariable(yDimensionName);  //null if not found
                    Variable xVariable = ncFile.findVariable(xDimensionName);  //null if not found

                    //get the attributes 
                    //Yes, they may be slightly different for each station.
                    //Each station's attributes will be tested for validity below.
                    tAttributes.clear();
                    zAttributes.clear();
                    yAttributes.clear();
                    xAttributes.clear();
                    NcHelper.getVariableAttributes(tVariable, tAttributes);
                    NcHelper.getVariableAttributes(zVariable, zAttributes);
                    NcHelper.getVariableAttributes(yVariable, yAttributes);
                    NcHelper.getVariableAttributes(xVariable, xAttributes);
                    if (station == 0) 
                        NcHelper.getGlobalAttributes(ncFile, globalAttributes); 

                    //rarely used but useful diagnostics...
                    if (inDevelopment) {
                        String2.log("\n    station=" + station);
                        PrimitiveArray localPa = NcHelper.getPrimitiveArray(xVariable); 
                        String2.log("    x  stats: " + localPa.statsString());
                        localPa = NcHelper.getPrimitiveArray(yVariable); 
                        String2.log("    y  stats: " + localPa.statsString());
                        localPa = NcHelper.getPrimitiveArray(zVariable); 
                        String2.log("    z  stats: " + localPa.statsString());
                        localPa = NcHelper.getPrimitiveArray(tVariable); 
                        String2.log("    t  stats: " + localPa.statsString());

                        localPa = NcHelper.getPrimitiveArray(dataVariable); 
                        localPa.switchFakeMissingValueToNaN(sourceFillValue[   station][0]);
                        localPa.switchFakeMissingValueToNaN(sourceMissingValue[station][0]);
                        String2.log("    v0 stats: " + localPa.statsString());
                        Math2.gcAndWait(); //within a diagnostic
                    }
                    
                    //*** get the t information
                    String timeUnits = tAttributes.getString("units");
                    Test.ensureTrue(
                        tDimensionName.toLowerCase().indexOf("time") >= 0 ||
                        Calendar2.isNumericTimeUnits(timeUnits),
                        //errorInMethod added below 
                        "The time variable's name (" + tDimensionName +
                            ") doesn't contain \"time\".");
                    int nTimes = tDimension.getLength();
                    if (reallyVerbose) String2.log("    nTimes=" + nTimes);

                    //check if time point_spacing is "even".   ('even'ness is crudely checked below)
                    String tEven = tAttributes.getString("point_spacing");
                    boolean timeIsEven = tEven != null && tEven.equals("even");
                    if (reallyVerbose) String2.log("    time point_spacing even=" + timeIsEven);            

                    //how is time encoded?
                    //it must be: <units> since <isoDate>   or exception thrown
                    double bAndF[] = Calendar2.getTimeBaseAndFactor(timeUnits); 
                    timeBaseSeconds[station] = bAndF[0];
                    timeFactorToGetSeconds[station] = bAndF[1];
                    if (reallyVerbose) String2.log("    timeBaseSeconds=" + timeBaseSeconds[station] + 
                        " timeFactorToGetSeconds=" + timeFactorToGetSeconds[station]);

                    //get the first 2 and the last time values
                    long tTime = System.currentTimeMillis();
                    PrimitiveArray pa = NcHelper.getPrimitiveArray(tVariable, 0, 1); //not 'nice'
                    minT[station] = pa.getDouble(0); 
                    double t1     = pa.getDouble(1);
                    maxT[station] = NcHelper.getDouble(tVariable, nTimes-1);
                    minT[station] = Calendar2.unitsSinceToEpochSeconds(
                        timeBaseSeconds[station], timeFactorToGetSeconds[station], minT[station]);
                    t1            = Calendar2.unitsSinceToEpochSeconds(
                        timeBaseSeconds[station], timeFactorToGetSeconds[station], t1);
                    maxT[station] = Calendar2.unitsSinceToEpochSeconds(
                        timeBaseSeconds[station], timeFactorToGetSeconds[station], maxT[station]);
                    Test.ensureTrue(Math2.isFinite(minT[station]) && minT[station] < 1e10, //secondsSinceEpoch  year ~2040
                        //errorInMethod added below
                        "minT=" + minT[station] + " for station=" + station + 
                        " is >= 1e10!");
                    Test.ensureTrue(Math2.isFinite(maxT[station]) && maxT[station] < 1e10,
                        //errorInMethod added below
                        "maxT=" + maxT[station] + " for station=" + station + 
                        " is >= 1e10!");
                    Test.ensureTrue(minT[station] <= maxT[station],
                        //errorInMethod added below
                        "minT=" + minT[station] + " < maxT=" + maxT[station] + 
                        " for station=" + station + "!");
                    timeIncrementInSeconds[station] = timeIsEven? t1 - minT[station] : Double.NaN;
                    if (verbose) 
                        String2.log(
                        "    station=" + stationFileNames[station] +
                        "\n      minT=" + Calendar2.epochSecondsToIsoStringT(minT[station]) +
                           " maxT=" + Calendar2.epochSecondsToIsoStringT(maxT[station]));  

                    //check "even" by checking that time.length is appropriate for 0th, 1st, and last times
                    if (timeIsEven) {
                        double expectedNTimes = ((maxT[station] - minT[station]) / timeIncrementInSeconds[station]) + 1;
                        if (nTimes != expectedNTimes) { 
                            timeIncrementInSeconds[station] = Double.NaN;
                            timeIsEven = false;
                            String2.log("times not evenly spaced! nTimes(" + nTimes + 
                                ") != expectedNTimes(" + expectedNTimes + 
                                ").  minT=" + minT[station] + " maxT=" + maxT[station] + 
                                " timeIncrementInSeconds=" + timeIncrementInSeconds[station]);
                        }
                    }

                    //*** get the z information
                    String zUnits = zAttributes.getString("units");
                    String zPositive = zAttributes.getString("positive");
                    Test.ensureTrue(
                        zUnits != null &&
                        (zUnits.toLowerCase().equals("meters") || 
                         zUnits.toLowerCase().equals("meter") || 
                         zUnits.toLowerCase().equals("m")), 
                        //errorInMethod added below  
                            "the z units (" + zUnits + ") aren't 'meters', 'meter', or 'm', " +
                            "or there is no zPositive attribute (" + zPositive + ").");
                    if (zPositive == null) 
                        zPositive = globalAttributes.getString("geospatial_vertical_positive");
                    stationZUp[station] = !(zPositive != null && zPositive.equals("down"));
                    if (reallyVerbose) String2.log("    stationZUp=" + stationZUp[station]);
                    
                    //get the z values
                    stationDepthLevels[station] = NcHelper.getNiceDoubleArray(zVariable, 0, -1);
                    if (stationZUp[station])
                        stationDepthLevels[station].scaleAddOffset(-1, 0);


                    //*** get the y information
                    String yUnits = yAttributes.getString("units");
                    Test.ensureTrue(
                        yDimensionName.toLowerCase().indexOf("latitude") >= 0 ||
                        (yUnits != null && yUnits.indexOf("degrees_north") >= 0), 
                        //errorInMethod added below  
                        "y units (" + yUnits + ") doesn't start with 'degrees_north'.");

                    //get the first y value (should be the only one)
                    stationY[station] = NcHelper.getNiceDouble(yVariable, 0);

                    //*** get the x information
                    String xUnits = xAttributes.getString("units");
                    Test.ensureTrue(
                        xDimensionName.toLowerCase().indexOf("longitude") >= 0 ||
                        (xUnits != null && xUnits.indexOf("degrees_east") >= 0), 
                        //errorInMethod added below  
                        "the x units (" + xUnits + ") doesn't start with 'degrees_east'.");

                    //get the first x value (should be the only one)
                    stationX[station] = NcHelper.getNiceDouble(xVariable, 0);
                    //String2.log(" CHECK stationX=" + stationX[station] + " stationY=" + stationY[station]);
                    //Math2.sleep(3000);

                    //reject the station based on min/maxStationX/Y?
                    double adjustX = Double.NaN;
                    if (stationX[station] >= minStationX && stationX[station] <= maxStationX)
                        adjustX = 0;
                    else if (stationX[station] + 360 >= minStationX && stationX[station] + 360 <= maxStationX)
                        adjustX = 360;
                    else if (stationX[station] - 360 >= minStationX && stationX[station] - 360 <= maxStationX)
                        adjustX = -360;
                    if (Double.isNaN(adjustX) ||
                        stationY[station] < minStationY || stationY[station] > maxStationY) {
                        //reject
                        if (reallyVerbose) String2.log("rejecting station " + stationNames[station] + 
                            " because stationX=" + stationX[station] + " stationY=" + stationY[station]);
                        stationNames[station] = null;
                    }

                    //*** for all the other variables
                    for (int var = 1; var < nVars; var++) { //0 already done, so start at 1

                        Variable dataVariable2 = ncFile.findVariable(inFileVariableNames[var]);  //null if not found
                        List dimensions2 = dataVariable.getDimensions();

                        //ensure nDimensions = 4
                        Test.ensureEqual(dimensions2.size(), 4, 
                            //errorInMethod added below 
                            "nDimensions for " + inFileVariableNames[var] + " not 4.");

                        //verify that the dimensions are same as for variable 0
                        Dimension tDim2 = (Dimension)dimensions2.get(0);
                        Dimension zDim2 = (Dimension)dimensions2.get(1);
                        Dimension yDim2 = (Dimension)dimensions2.get(2);
                        Dimension xDim2 = (Dimension)dimensions2.get(3);
                        //errorInMethod added below
                        Test.ensureEqual(tDim2.getName(), tDimensionName, "Unexpected time dimension name.");
                        Test.ensureEqual(zDim2.getName(), zDimensionName, "Unexpected z dimension name.");
                        Test.ensureEqual(yDim2.getName(), yDimensionName, "Unexpected y dimension name.");
                        Test.ensureEqual(xDim2.getName(), xDimensionName, "Unexpected x dimension name.");

                        if (station == 0) {
                            dataAttributes[var] = new Attributes();
                            NcHelper.getVariableAttributes(dataVariable2, 
                                dataAttributes[var]); 
                            elementClasses[var] = NcHelper.getElementClass(dataVariable2.getDataType());
                        }

                        //get the sourceMissingValue for this var
                        //for cacheOpendapStation, these are always -1e32
                        fvPa = NcHelper.getVariableAttribute(dataVariable2, "_FillValue");
                        mvPa = NcHelper.getVariableAttribute(dataVariable2, "missing_value");
                        if (fvPa != null) sourceFillValue[   station][var] = fvPa.getFloat(0);
                        if (mvPa != null) sourceMissingValue[station][var] = mvPa.getFloat(0);
                        if (inDevelopment && fvPa == null && mvPa == null) {
                            String2.log("WARNING: no missing_value or _FillValue metadata for data variable\n" + 
                                inFileVariableNames[var] + " for " + stationFileNames[station]);
                        }
                        if (inDevelopment) {
                            PrimitiveArray localPa = NcHelper.getPrimitiveArray(dataVariable2); 
                            localPa.switchFakeMissingValueToNaN(sourceFillValue[   station][var]);
                            localPa.switchFakeMissingValueToNaN(sourceMissingValue[station][var]);
                            String2.log("    v" + var + " stats: " + localPa.statsString());
                            Math2.gcAndWait(); //within a debug diagnostic
                        }

                    }

                    if (reallyVerbose)
                        String2.log("    station " + stationNames[station] + 
                            " x=" + stationX[station] + " y=" + stationY[station] + 
                            "\n    zLevels=" + stationDepthLevels[station] +
                            "\n    totalTime=" + (System.currentTimeMillis() - stationTime));  

                    //I care about this exception
                    ncFile.close();

                } catch (Exception e) {
                    try {
                        ncFile.close(); //make sure it is explicitly closed
                    } catch (Exception e2) {
                        //don't care
                    }
                    throw e;
                }

            } catch (Exception e) {
                String2.log(errorInMethod + tErrorInMethod + "\n" +
                    "    (So station " + station + " won't be used.)\n" +
                    (station == 0? "    (Since it's station 0, no related point datasets will be created.)\n" : "") +
                    MustBe.throwableToString(e));

                //flag -- don't use this station
                stationNames[station] = null;
            }
        }

        //modify the attributes
        globalAttributes.remove("observationDimension");

        xAttributes.set("_CoordinateAxisType", "Lon");
        xAttributes.set("long_name", "Longitude"); 
        xAttributes.set("standard_name", "longitude"); 
        xAttributes.set("units", "degrees_east");
        xAttributes.remove("modulo");
        xAttributes.remove("point_spacing");

        yAttributes.set("_CoordinateAxisType", "Lat");
        yAttributes.set("long_name", "Latitude"); 
        yAttributes.set("standard_name", "latitude"); 
        yAttributes.set("units", "degrees_north");
        yAttributes.remove("modulo");
        yAttributes.remove("point_spacing");

        zAttributes.set("_CoordinateAxisType", "Height");
        zAttributes.set("long_name", "Depth"); 
        zAttributes.set("standard_name", "depth"); 
        zAttributes.set("positive", "down"); //special
        zAttributes.set("units", "meters");
        zAttributes.remove("point_spacing");

        tAttributes.set("_CoordinateAxisType", "Time");
        tAttributes.set("long_name", "Time"); 
        tAttributes.set("standard_name", "time"); 
        tAttributes.set("units", Calendar2.SECONDS_SINCE_1970);
        tAttributes.remove("time_origin");
        tAttributes.remove("point_spacing");

        idAttributes.set("long_name", "Station Identifier"); 
        idAttributes.set("units", DataHelper.UNITLESS);


        //*** for each, variable
        for (int var = 0; var < nVars; var++) {
            //if trouble, dataAttributes[var] wasn't created
            if (dataAttributes[var] == null || elementClasses[var] == null)
                continue;
            dataAttributes[var].set("long_name", userVariableNames[var]);

            //gather the stationVariables for this variable for all stations
            ArrayList stationVariables = new ArrayList();
            for (int station = 0; station < nStations; station++) {
                try {
                    if (stationNames[station] == null) 
                        continue;

                    //make the stationVariable (holds info for one variable from one station)
                    StationVariableNc4D stationVariable = 
                        new StationVariableNc4D(stationFileNames[station],
                            inFileVariableNames[var], 
                            userVariableNames[var], 
                            stationNames[station],
                            variableFactor[var],
                            stationX[station], stationY[station], stationDepthLevels[station], 
                            stationZUp[station], 
                            timeBaseSeconds[station], timeFactorToGetSeconds[station],
                            timeIncrementInSeconds[station], 
                            minT[station], maxT[station], 
                            sourceFillValue[station][var],
                            sourceMissingValue[station][var]);

                    //if all went well, add the stationVariable to the arrayList
                    stationVariables.add(stationVariable);

                } catch (Exception e) {
                    String2.log(errorInMethod + MustBe.throwableToString(e));
                }
            }

            //make a PointDataSet for this variable
            if (stationVariables.size() > 0) {
                try {
                    //gather the stationVariables in an array
                    StationVariableNc4D stationVariablesAr[] = 
                        new StationVariableNc4D[stationVariables.size()];
                    for (int i = 0; i < stationVariables.size(); i++) 
                        stationVariablesAr[i] = 
                            (StationVariableNc4D)stationVariables.get(i);
                
                    //make a PointDataSet
                    PointDataSet pointDataSet = 
                        new PointDataSetFromStationVariables( //throws Exception if trouble
                            internalVariableNames[var],
                            inFileVariableNames[var],
                            stationVariablesAr, 
                            userVariableNames[var] + " (" + userDataSetBaseName + ")", 
                            courtesy,
                            variablePalette[var], 
                            variablePaletteMin[var], 
                            variablePaletteMax[var], 
                            variablePaletteScale[var], 
                            variableUdUnits[var],
                            globalAttributes, xAttributes, yAttributes,
                            zAttributes, tAttributes, idAttributes,
                            dataAttributes[var],
                            elementClasses[var]); 
                    pointDataSet.ensureValid(); //throws Exception if trouble
                    activePointDataSets.add(pointDataSet);
                } catch (Exception e) {
                    String2.log(errorInMethod + MustBe.throwableToString(e));
                }               
            }
        }

        if (verbose) String2.log("  finished successfully  nPointDataSets=" + 
            (activePointDataSets.size() - originalListSize) + 
            " time=" + (System.currentTimeMillis() - time));  
    }
    
    /**
     * The constructor.
     *
     * @param tInternalName the 7 character internal name, e.g. PNBssta
     * @param tGroupVariables 
     * @param tBoldTitle the title for the drop down list of PointDataSets
     *   and for the legend (e.g., "Relative Humidity (MBARI Moorings)").
     * @param tCourtesy  The courtesy line for the legend, e.g., NOAA NESDIS OSDPD. 
     * @param tPalette e.g., Rainbow. 
     * @param tPaletteMin The default min for the palette range for standard units. 
     * @param tPaletteMax The default max for the palette range for standard units. 
     * @param tPaletteScale The name of the default palette scale, e.g., Linear or Log. 
     * @param udUnits The UDUnits for the standard units.
     * @param globalAttributes
     * @param xAttributes
     * @param yAttributes
     * @param zAttributes
     * @param tAttributes
     * @param idAttributes
     * @param dataAttributes
     * @param elementClass e.g., double.class
     */
    public PointDataSetFromStationVariables(String tInternalName, 
        String tInFileVarName,
        GroupVariableWithDepthLevels tGroupVariables[], String tBoldTitle, String tCourtesy,
        String tPalette, double tPaletteMin, double tPaletteMax, String tPaletteScale, 
        String tUdUnits,
        Attributes globalAttributes, Attributes xAttributes, Attributes yAttributes, 
        Attributes zAttributes, Attributes tAttributes, Attributes idAttributes, 
        Attributes dataAttributes, Class elementClass) throws Exception {

        String errorInMethod = String2.ERROR + " in PointDataSetGroupVariables.constructor:\n";
        groupVariables = tGroupVariables;
        this.globalAttributes = globalAttributes;
        this.xAttributes = xAttributes;
        this.yAttributes = yAttributes;
        this.zAttributes = zAttributes;
        this.tAttributes = tAttributes;
        this.idAttributes = idAttributes;
        this.dataAttributes = dataAttributes;
        this.elementClass = elementClass;

        //change the mv attributes
        //make subset currently stores all data as floats
        //this will be overwritten when file is overwritten
        dataAttributes.set("missing_value", Float.NaN);
        dataAttributes.set("_FillValue", Float.NaN);

        //determine minT and maxT of the groupVariables
        double minT = Double.MAX_VALUE;
        double maxT = -Double.MAX_VALUE;
        for (int i = 0; i < groupVariables.length; i++) {
            minT = Math.min(minT, groupVariables[i].minT);
            maxT = Math.max(maxT, groupVariables[i].maxT);
        }
        String2.log("  minT=" + Calendar2.epochSecondsToIsoStringT(minT) + 
                     " maxT=" + Calendar2.epochSecondsToIsoStringT(maxT));

        //set the standard attributes
        altScaleFactor = 1.0; 
        altOffset = 0.0; 
        //altPalette, altPaletteMin/Max not used
        boldTitle = tBoldTitle; 
        courtesy = tCourtesy;
        defaultUnits = 'S'; //for now, never 'A'
        firstTime = Calendar2.newGCalendarZulu(Math2.roundToLong(minT) * 1000); //convert s to ms
        inFileVarName = tInFileVarName;
        internalName = tInternalName; 
        lastTime = Calendar2.newGCalendarZulu(Math2.roundToLong(maxT) * 1000);
        daysTillDataAccessAllowed = -1;
        option = tBoldTitle;
        palette = tPalette; 
        paletteMin = tPaletteMin; 
        paletteMax = tPaletteMax; 
        paletteScale = tPaletteScale; 
        tooltip = tBoldTitle; 
        udUnits = tUdUnits;
        unitsOptions = new String[]{DataHelper.makeUdUnitsReadable(tUdUnits)}; //just standard, no alternate

        //ensure these values are valid
        ensureValid();

        //generate the list of unique depth levels
        HashSet hashSet = new HashSet();
        for (int i = 0; i < groupVariables.length; i++) {
            DoubleArray tDepthLevels = groupVariables[i].depthLevels();
            for (int level = 0; level < tDepthLevels.size(); level++) {
                hashSet.add(tDepthLevels.getString(level));
            }
        }
        //sort as doubles
        Object objects[] = hashSet.toArray();
        double tUniqueDepthLevels[] = new double[objects.length];
        for (int i = 0; i < objects.length; i++)
            tUniqueDepthLevels[i] = String2.parseDouble((String)objects[i]);
        objects = null;
        Arrays.sort(tUniqueDepthLevels);      
        //store as strings
        uniqueDepthLevels = new String[tUniqueDepthLevels.length];
        for (int i = 0; i < tUniqueDepthLevels.length; i++)
            uniqueDepthLevels[i] = String2.genEFormat6(tUniqueDepthLevels[i]);
        String2.log("PointDataSetFromStationVariables " + tInternalName + 
            " was created. nDepths=" + uniqueDepthLevels.length +
            //"\n  uniqueDepthLevels=" + String2.toCSSVString(uniqueDepthLevels) +
            "\n");

    } 

    /**
     * This gets the minTime (seconds since epoch) for one of the stations.
     *
     * @param stationID e.g., "M2" or "31201"
     * @return  the minTime (seconds since epoch) for one of the stations
     *    (or Double.NaN if stationID not found).
     */
    public double getStationMinTime(String stationID) {
        for (int i = 0; i < groupVariables.length; i++) 
            if (groupVariables[i].groupName().equals(stationID))
                return groupVariables[i].minT();
        return Double.NaN;
    }

    /**
     * Get the depthLevels which the various stations use.
     * Not all stations may support all depths.
     *
     * @return the depthLevels which the various stations use.
     */
    public String[] depthLevels() {
        return uniqueDepthLevels;
    }

    /**
     * Make a Table with a specific subset of the data.
     *
     * @param minX the minimum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param maxX the maximum acceptable longitude (degrees_east, may be  -180 to 180 or 0 to 360)
     * @param minY the minimum acceptable latitude (degrees_north)
     * @param maxY the maximum acceptable latitude (degrees_north)
     * @param minDepth the minimum acceptable depth (meters, down is positive)
     * @param maxDepth the maximum acceptable depth (meters, down is positive)
     * @param isoMinT an ISO format date/time for the minimum ok time.
     *    isoMinT and isoMaxT are rounded to be a multiple of the frequency 
     *    of the data's collection.  For example, if the data is hourly, 
     *    they are rounded to the nearest hour.
     * @param isoMaxT an ISO format date/time for the maximum ok time
     * @return a Table with 6 columns: 
     *    <br>1) "LON" (units=degrees_east, with values made relevant to the desired minX maxX), 
     *    <br>2) "LAT" (units=degrees_north), 
     *    <br>3) "DEPTH" (units=meters, positive=down), 
     *    <br>4) "TIME" (units=seconds since 1970-01-01T00:00:00Z), 
     *    <br>5) "ID" (String data), 
     *    <br>6) inFileVarName with data (unpacked, in standard units).
     *   <br>LON, LAT, DEPTH and TIME will be DoubleArrays; ID will be a StringArray; 
     *      the data column will be a numeric PrimitiveArray (not necessarily DoubleArray).
     *   <br>Rows with missing values are NOT removed.
     *   <br>The metadata (e.g., actual_range) will be correct (as correct as I can make it). 
     *   <br>The table will have the proper columns but may have 0 rows.
     *   <br>If a station is in the x,y,z range, but not the t range,
     *     a row with data=mv is added for that station.
     * @throws Exception if trouble (e.g., ill-formed isoMinT, or minX > maxX)
     */
    public Table makeSubset(double minX, double maxX,
            double minY, double maxY, double minDepth, double maxDepth,
            String isoMinT, String isoMaxT) throws Exception {

        //WARNING: this presumes attributes (e.g., missing_value) 
        //for the current variable are consistent for all stations.
        long time = System.currentTimeMillis();
        String msg = "PointDataSetFromStationVariables.makeSubset '" + 
            groupVariables[0].variableName() + 
            "' station#0=" + groupVariables[0].groupName() + "\n " +
            "  minX=" + String2.genEFormat10(minX) + 
            " maxX=" + String2.genEFormat10(maxX) + 
            " minY=" + String2.genEFormat10(minY) + 
            " maxY=" + String2.genEFormat10(maxY) + 
            " minDepth=" + String2.genEFormat10(minDepth) + 
            " maxDepth=" + String2.genEFormat10(maxDepth) + "\n " +
            "  isoMinT=" + isoMinT + 
            " isoMaxT=" + isoMaxT; 
        if (verbose) String2.log(msg);

        String errorInMethod = String2.ERROR + " in " + msg + ":\n";

        //validate input 
        double minT = Calendar2.isoStringToEpochSeconds(isoMinT); //throws exception if trouble
        double maxT = Calendar2.isoStringToEpochSeconds(isoMaxT); //throws exception if trouble
        Test.ensureNotEqual(minX, Double.NaN, errorInMethod + "minX is NaN.");
        Test.ensureNotEqual(minY, Double.NaN, errorInMethod + "minY is NaN.");
        Test.ensureNotEqual(minDepth, Double.NaN, errorInMethod + "minDepth is NaN.");
        Test.ensureTrue(minX <= maxX, errorInMethod + "minX (" + minX + ") is greater than maxX (" + maxX + ").");
        Test.ensureTrue(minY <= maxY, errorInMethod + "minY (" + minY + ") is greater than maxY (" + maxY + ").");
        Test.ensureTrue(minDepth <= maxDepth, errorInMethod + "minDepth (" + minDepth + ") is greater than maxDepth (" + maxDepth + ").");
        Test.ensureTrue(minT <= maxT, errorInMethod + "minT (" + minT + ") is greater than maxT (" + maxT + ").");

        double incr = groupVariables[0].timeIncrementInSeconds();

        //round to nearest timeIncrementInSeconds
        if (!Double.isNaN(incr)) {
            minT = Math.rint(minT / incr) * incr;
            maxT = Math.rint(maxT / incr) * incr;
            //if (verbose) String2.log("  data evenly spaced, so rounded minT=" + 
            //    Calendar2.epochSecondsToIsoStringT(minT) + " maxT=" +
            //    Calendar2.epochSecondsToIsoStringT(maxT));
        }

        //make the table
        Table table = new Table();
        globalAttributes.copyTo(table.globalAttributes());
        table.addColumn(0, "LON",   new DoubleArray(), (Attributes)xAttributes.clone());
        table.addColumn(1, "LAT",   new DoubleArray(), (Attributes)yAttributes.clone());
        table.addColumn(2, "DEPTH", new DoubleArray(), (Attributes)zAttributes.clone());
        table.addColumn(3, "TIME",  new DoubleArray(), (Attributes)tAttributes.clone());
        table.addColumn(4, "ID",    new StringArray(), (Attributes)idAttributes.clone());
        table.addColumn(5, inFileVarName, 
            PrimitiveArray.factory(elementClass, 8, false), (Attributes)dataAttributes.clone());

        //add the data
        //String2.log("PointDataSetFromStationVariables.makeSubset nGroupVariables=" + groupVariables.length);
        for (int i = 0; i < groupVariables.length; i++) 
            groupVariables[i].addToSubset(minX, maxX, minY, maxY, minDepth, maxDepth,
                minT, maxT, table);

        //adjust the metadata
        //set Attributes    ('null' says make no changes  (don't use ""))
        table.setAttributes(0, 1, 2, 3, boldTitle, 
            null, //cdmDataType,   
            DataHelper.CW_CREATOR_EMAIL, //who is creating this file...
            DataHelper.CW_CREATOR_NAME,
            DataHelper.CW_CREATOR_URL,
            DataHelper.CW_PROJECT,       
            null, //id, 
            null, //keywordsVocabulary,
            null, //keywords, 
            null, //references, 
            null, //summary, 
            courtesy, //who is source of data...
            "Time");
        table.columnAttributes(3).remove("point_spacing");
        //setting udUnits is important because addToSubset uses standardUnitsFactor
        //to convert raw data to standardUnits (e.g., m s-1). So units attribute already
        //in source's metadata may be wrong (e.g., cm/s) and in the wrong format.
        table.columnAttributes(5).set("units", udUnits); 
        table.columnAttributes(5).remove("_coordinateSystem"); //MBARI sets this. I don't know where else to remove it.

        //return the results
        String2.log("PointDataSetFromStationVariables.makeSubset done. nRows=" + 
            table.nRows() + " TIME=" + (System.currentTimeMillis() - time));
        return table;
    }

    /**
     * This appends data about stations 
     * (which have data within an x,y,z,t bounding box)
     * to a table of stations.
     *
     * <p>Typical use of this is:
     * <ol>
     * <li> Table stations = PointDataSet.getEmptyStationTable(
     *     0, 360, -90, 90, -100, 100, "1900-01-01", "3000-01-01");
     * <li> pointDataSets[i].addStationsToTable(
            0, 360, -90, 90, -100, 100, "1900-01-01", "3000-01-01", stations);
     * <li> stations.setActualRangeAndBoundingBox(0,1,2,-1,3);
     * </ol>
     *
     * @param minX  the minimum acceptable longitude (degrees_east).
     *    minX and maxX may be -180 to 180, or 0 to 360.
     * @param maxX  the maximum acceptable longitude (degrees_east)
     * @param minY  the minimum acceptable latitude (degrees_north)
     * @param maxY  the maximum acceptable latitude (degrees_north)
     * @param minDepth  the minumum acceptable depth (meters, positive=down)
     * @param maxDepth  the maxumum acceptable depth (meters, positive=down)
     * @param isoMinT an ISO format date/time for the minimum acceptable time  
     * @param isoMaxT an ISO format date/time for the maximum acceptable time  
     * @param stations a Table with 4 columns (LON, LAT, DEPTH, ID),
     *    where lat is in degrees_east adjusted to be in the minX maxX range,
     *    lon is in degrees_north, depth is in meters down,
     *    and ID is a string suitable for sorting (e.g., MBARI MO).
     * @throws Exception if trouble (e.g., invalid isoMinT)
     */
    public void addStationsToTable(double minX, double maxX,
        double minY, double maxY, double minDepth, double maxDepth, 
        String isoMinT, String isoMaxT, Table stations) throws Exception {

        //calculate and validate min/maxT
        double minT = Calendar2.isoStringToEpochSeconds(isoMinT); //throws Exception if trouble
        double maxT = Calendar2.isoStringToEpochSeconds(isoMaxT); //throws Exception if trouble

        //get the matching stations
        PrimitiveArray lonColumn    = stations.getColumn(0);
        PrimitiveArray latColumn    = stations.getColumn(1);
        PrimitiveArray depthColumn  = stations.getColumn(2);
        PrimitiveArray idColumn     = stations.getColumn(3);
        for (int i = 0; i < groupVariables.length; i++) {
            GroupVariable groupVariable = groupVariables[i];

            //see if x is in range with various adjustments
            double xAdjust = Double.NaN;
            if (groupVariable.maxX >= minX && groupVariable.minX <= maxX)
                xAdjust = 0;
            else if (groupVariable.maxX + 360 >= minX && groupVariable.minX + 360 <= maxX)
                xAdjust = 360;
            else if (groupVariable.maxX - 360 >= minX && groupVariable.minX - 360 <= maxX)
                xAdjust = -360;
        
            //test all the constraints
            if (!Double.isNaN(xAdjust) &&
                groupVariable.maxY >= minY && groupVariable.minY <= maxY &&
                groupVariable.maxDepth >= minDepth && groupVariable.minDepth <= maxDepth &&
                groupVariable.maxT >= minT && groupVariable.minT <= maxT) { //draw diagram to see why
                
                //add this station to the table
                lonColumn.addDouble(groupVariable.minX + xAdjust);  //for stations, minX=maxX
                latColumn.addDouble(groupVariable.minY);  //for stations, minY=maxY
                depthColumn.addDouble(groupVariable.minDepth);    //for stations, minDepth=maxDepth
                idColumn.addString(groupVariable.groupName);
            }
        }
    }

    /**
     * This is a convenience method to make CacheOpendapStations and PointDataSets.
     *
     * @param pointFilesDir  with slash at end, e.g., f:/data/
     * @param stationBaseName e.g., MBARI  (best if no spaces)
     * @param stationNames  e.g., M0, M1, M2. 
     * @param stationTypes  e.g., adcp, met. 
     * @param stationUrls  [stationType][stationNames]
     * @param variableInfo [stationType][variable]       
     * @param courtesy a short courtesy string (e.g., MBARI)
     * @param minX the minimum lon value of interest for stations
     * @param maxX the maximum lon value of interest for stations
     * @param minY the minimum lat value of interest for stations
     * @param maxY the maximum lat value of interest for stations
     * @param clearCache if true, any existing caches will be deleted and recreated.
     *     This applies regardless of whether station is accepted/rejected based 
     *     on min/max/X/Y.
     * @param ensureUpToDate if true, this makes sure that the caches are up-to-date.
     *     This applies regardless of whether station is accepted/rejected based 
     *     on min/max/X/Y.
     *     If clearCache=true, this will be forced to be true 
     *     (otherwise, the datasets won't be made).
     * @param throwExceptionIfAnyTrouble if true, an Exception will 
     *     be thrown for small problems, too
     * @param cacheOpendapStation the list that new cacheOpendapStations will be added to.
     *    This may be null.
     * @param activePointDataSets the list of pointDataSets that will be added to
     * @throws Exception for serious problems (e.g., can't make directory)
     */
    public static void makeCachesAndDataSets(String pointFilesDir, 
        String stationBaseName, String stationNames[], String stationTypes[],
        String stationUrls[][], String variableInfo[][], String courtesy,
        double minX, double maxX, double minY, double maxY, 
        boolean clearCache,  boolean ensureUpToDate, 
        boolean throwExceptionIfAnyTrouble,
        List cacheOpendapStations, List activePointDataSets) throws Exception {

        if (clearCache)
            ensureUpToDate = true;

        //ensure pointFilesDir for this data exists
        String errorInMethod = String2.ERROR + " in PointDataSetFromStationVariables.makeCachesAndDataSets:\n";
        String dir = pointFilesDir + stationBaseName + "/";
        if (!File2.isDirectory(dir)) {
            File tDir = new File(dir);
            Test.ensureTrue(tDir.mkdir(), errorInMethod + "Can't make directory: " + dir);
        }

        for (int type = 0; type < stationTypes.length; type++) {

            //gather the variable names
            int nVar = variableInfo[type].length;
            String[] variableNames = new String[nVar];
            for (int var = 0; var < nVar; var++) {
                String ta[] = String2.split(variableInfo[type][var], '`');
                variableNames[var] = ta[0];
            }

            //try to make the cacheOpendapStations
            for (int station = 0; station < stationUrls[type].length; station++) {
                try {
                    CacheOpendapStation cos = new CacheOpendapStation(
                        stationUrls[type][station], 
                        //e.g. file name ...MBARI_M0_NRT_adcp.nc -> user station name "MBARI M0 NRT adcp"
                        dir + stationBaseName + "_" + stationNames[station] + "_" +
                            stationTypes[type] + ".nc", 
                        variableNames);
                    if (clearCache)
                        cos.deleteCache();
                    if (ensureUpToDate) {
                        boolean success = cos.updateCache();
                        //trouble? try to create a new cache
                        if (!clearCache && !success) {
                            String2.log(String2.ERROR + ": Updating " + stationUrls[type][station] + " failed,\n" +
                                "so trying createNewCache.");
                            success = cos.createNewCache();
                        }
                        if (throwExceptionIfAnyTrouble && !success) 
                            Test.error(String2.ERROR + ": Updating " + stationUrls[type][station] + " failed.");
                    }
                    if (cacheOpendapStations != null) 
                        cacheOpendapStations.add(cos);
                } catch (Exception e) {
                    if (throwExceptionIfAnyTrouble)
                        throw e;
                    String2.log(MustBe.throwableToString(e));
                }
            }
        }

        //make the pointDataSets 
        makeGroupsOfPointDataSets(
            pointFilesDir, stationBaseName, stationTypes,
            variableInfo, courtesy, minX, maxX, minY, maxY, activePointDataSets);

    }

    public final static String mbariStationBaseName = "MBARI";
    public final static String mbariCourtesy = "MBARI";

    /** These are the mbari near real time files which are currently being updated
        from http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/ .
        [stationType][stationNames]
        See plots of the data via cimt: http://cimt.ucsc.edu/m1_current_contour.htm
        */
    public final static String[][] mbariNrtStationUrls = {
        { //metsys
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/current_netCDFs/metsys.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/current_netCDFs/metsys.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/current_netCDFs/metsys.nc"
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200706/m0_metsys_20070716_original.nc",
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_metsys_20060801_original.nc", retired 11/07
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_metsys_20060731_original.nc", retired
            //bad "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200710/m1_metsys_20071020_original.nc",
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200610/m1_metsys_20061012_original.nc", retired 11/07
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_metsys_20051101_original.nc", retired
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200704/m2_metsys_20070807_original.nc"
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_metsys_20060415_original.nc" retired 11/07
            },
        { //adcp
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/current_netCDFs/adcp1406.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/current_netCDFs/adcp1353.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/current_netCDFs/adcp1417.nc"
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200706/m0_adcp1406_20070621.nc",
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_adcp1267_20060731.nc", retired 11/07
            //bad "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200711/m1_adcp1352_20071106.nc",
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200610/m1_adcp1353_20061012.nc", retired 11/07
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_adcp1417_20051020.nc", retired
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200704/m2_adcp1417_20070425.nc"
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_adcp1352_20060330.nc" retired
            },
        { //ctd      ctd0001 are near the surface
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/current_netCDFs/ctd0001.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/current_netCDFs/ctd0001.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/current_netCDFs/ctd0000.nc"
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200706/m0_ctd0001_20070621_original.nc",
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m0/200607/m0_ctd0000_20060731_original.nc", retired 11/07
            //bad "http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200710/m1_ctd0001_20071020_original.nc",
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200610/m1_ctd0000_20061012_original.nc", retired 11/07
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m1/200510/m1_ctd0000_20051020_original.nc", retired
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200704/m2_ctd0000_20070425_original.nc"
            //"http://dods.mbari.org/cgi-bin/nph-nc/data/ssdsdata/deployments/m2/200603/m2_ctd0000_20060330_original.nc"  retired 11/07
            }};

    /** These are the mbari science quality files which are currently being updated.
        [stationType][stationNames]*/
    public final static String[][] mbariSqStationUrls = {
        //these have to be in separate groups because the different data variables use different lon,lat,depth columns.
        { //metsys
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM1.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM2.nc"},
        { //adcp
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM1.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM2.nc"},
        { //ctd      ctd0000 are at the surface
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM1.nc",
            "http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM2.nc"}};

    /** The station types (the first dimension for mbariNrtUrls). (use "_" instead of " ".)*/
    public final static String[] mbariNrtStationTypes = {"NRT_met", "NRT_adcp", "NRT_ctd"};

    /** The station types (the first dimension for mbariSqUrls). (use "_" instead of " ".)*/
    public final static String[] mbariSqStationTypes = {"SQ_met", "SQ_adcp", "SQ_ctd"};

    /** The station names (the second dimension for mbariNrtUrls. */
    public final static String[] mbariNrtStationNames = {"M0", "M1", "M2"};

    /** The station names (the second dimension for mbariSqUrls. */
    public final static String[] mbariSqStationNames = {"M0", "M1", "M2"};

    public final static float knotsToMetersPerSecond = 1f/1.943861f; //knots to m s-1  from DataSet.properties
    private final static String k = "" + knotsToMetersPerSecond;

    /** The variable information for the MBARI nrt variables. [stationType][variable] 
     * RE: uncorrected: mccann@mbari.org in 10/20/06 email said:
<p>All '_uncorrected' means is that mooring motion has not been 
subtracted from the velocity components.  We did this in 
earlier deployments using GPS data as you can see on our
LAS (http://dods.mbari.org/lasOASIS) for data up through 
1998.

<p>I've observed that the correction is very minor, so we haven't
continued the effort to subtract mooring motion.  There is 
some documentation on the LAS site - but I confess that it's
not very polished. 

<p>-Mike     
*/
    public final static String[][] mbariNrtVariableInfo = {
        { //metsys
            //name, palette info and suggested range should match ndbc when possible
            //units must be from http://www.unidata.ucar.edu/software/udunits/udunits.txt 
            //unofficial system: 4th letter n=near real time   s=science quality 
            //inFileVarName           varName  title                                     palette  paletteScale  factor min max  udUnits   
            "AirPressure`             PMBaprn` Air Pressure, Near Real Time`             Rainbow`      Linear`    1` 960` 1040` hPa",  //metsys files have metadata
            "AirTemperature`          PMBatmn` Air Temperature, Near Real Time`          Rainbow`      Linear`    1` -10`   40` degree_C",
            "RelativeHumidity`        PMBrhmn` Relative Humidity, Near Real Time`        Rainbow`      Linear`    1`   0`  100` percent",
            "WindSpd_Windbird`        PMBwswn` Wind Speed, Windbird, Near Real Time`     BlueWhiteRed` Linear`"+k+"` -20`   20` m s^-1",
            "WindDir_Windbird`        PMBwdwn` Wind Direction, Windbird, Near Real Time` BlueWhiteRed` Linear`    1`   0`  360` degrees_north"},
        { //adcp
            "u_component_uncorrected` PMBcrun` Current, Near Real Time, Zonal`           BlueWhiteRed` Linear`  .01` -.5`   .5` m s-1",  //.01 = convert cm/s to m/s
            "v_component_uncorrected` PMBcrvn` Current, Near Real Time, Meridional`      BlueWhiteRed` Linear`  .01` -.5`   .5` m s-1"},  //.01 = convert cm/s to m/s
        { //ctd
            "Conductivity`            PMBcndn` Conductivity, Near Real Time`             Rainbow`      Linear`    1`   0`  100` Siemens m-1", //range?
            //"Pressure`                PMBsprs` Sea Pressure, Near Real Time`             Rainbow`      Linear`    1`   0`  100` db", //this is at sea level, so no sea pressure readings
            "Salinity`                PMBsaln` Salinity, Near Real Time`                 Rainbow`      Linear`    1`   0`  100` PSU", //range?
            "Temperature`             PMBwtmn` Sea Temperature, Near Real Time`          Rainbow`      Linear`    1`   8`   32` degree_C"}};

   /** The Science Quality variableInfo for the 3 groups of mbari station data 
       (each group uses different dimensions for the variables).*/
   public final static String[][] mbariSqVariableInfo = {
        {   //name, palette info and suggested range should match ndbc when possible
            //units must be from http://www.unidata.ucar.edu/software/udunits/udunits.txt 
            //unofficial system: 4th letter n=near real time   s=science quality 
            //inFileVarName           varName  title                                    palette  paletteScale  factor min max   udUnits
            "AIR_PRESS_HR`            PMBaprs` Air Pressure, Science Quality`           Rainbow`      Linear`   1` 960` 1040` hPa",
            "AIR_TEMPERATURE_HR`      PMBatms` Air Temperature, Science Quality`        Rainbow`      Linear`   1` -10`   40` degree_C",
            "RELATIVE_HUMIDITY_HR`    PMBrhms` Relative Humidity, Science Quality`      Rainbow`      Linear`   1`   0`  100` percent",
            "WIND_U_COMPONENT_HR`     PMBwsus` Wind Speed, Science Quality, Zonal`      BlueWhiteRed` Linear`   1` -20`   20` m s^-1",
            "WIND_V_COMPONENT_HR`     PMBwsvs` Wind Speed, Science Quality, Meridional` BlueWhiteRed` Linear`   1` -20`   20` m s^-1"},
        {   //name, palette info and suggested range should match ndbc when possible
            //units must be from http://www.unidata.ucar.edu/software/udunits/udunits.txt 
            "U_COMPONENT_UNCORR_HR`   PMBcrus` Current, Science Quality, Zonal`         BlueWhiteRed` Linear` .01` -.5`   .5` m s-1",  //.01 = convert cm/s to m/s
            "V_COMPONENT_UNCORR_HR`   PMBcrvs` Current, Science Quality, Meridional`    BlueWhiteRed` Linear` .01` -.5`   .5` m s-1"},  //.01 = convert cm/s to m/s
            //"ECHO_INTENSITY_BEAM1_HR` PMBei1s` Echo Intensity, Science Quality, Beam 1` Rainbow`      Linear`   1`   0`  100` counts", //range?
            //"ECHO_INTENSITY_BEAM2_HR` PMBei2s` Echo Intensity, Science Quality, Beam 2` Rainbow`      Linear`   1`   0`  100` counts", //range?
            //"ECHO_INTENSITY_BEAM3_HR` PMBei3s` Echo Intensity, Science Quality, Beam 3` Rainbow`      Linear`   1`   0`  100` counts", //range?
            //"ECHO_INTENSITY_BEAM4_HR` PMBei4s` Echo Intensity, Science Quality, Beam 4` Rainbow`      Linear`   1`   0`  100` counts"}, //range?
       {    //the lat/lon/depth MET variables
            //name, palette info and suggested range should match ndbc when possible
            //units must be from http://www.unidata.ucar.edu/software/udunits/udunits.txt 
            "CONDUCTIVITY_HR`         PMBcnds` Conductivity, Science Quality`           Rainbow`      Linear`   1`   0`  100` Siemens m-1", //range?
            "PRESSURE_HR`             PMBsprs` Sea Pressure, Science Quality`           Rainbow`      Linear`   1`   0`  100` db", //range?
            "SALINITY_HR`             PMBsals` Salinity, Science Quality`               Rainbow`      Linear`   1`   0`  100` PSU", //range?
            "TEMPERATURE_HR`          PMBwtms` Sea Temperature, Science Quality`        Rainbow`      Linear`   1`   8`   32` degree_C"}};


    /**
     * This ensures the MBARI near real time cache files exist
     * and adds pointDataSets to activePointDataSets.
     *
     * @param pointsDir the points directory (to which mbari/ will be added)
     * @param minX the min longitude of interest
     * @param maxX the max longitude of interest
     * @param minY the min latitude of interest
     * @param maxY the max latitude of interest
     * @param ensureUpToDate if true, this makes sure that the caches are up-to-date.
     * @param activeCacheOpendapStations the list of valid cacheOpendapStations that will be added to
     * @param activePointDataSets the list of valid pointDataSets that will be added to
     * @throws Exception if trouble
     */
    public static void makeMbariNrtCachesAndDataSets(
        String pointsDir, double minX, double maxX, double minY, double maxY,
        boolean ensureUpToDate, boolean throwExceptionIfAnyTrouble, 
        List activeCacheOpendapStations, List activePointDataSets) throws Exception {

        makeCachesAndDataSets(
            pointsDir, mbariStationBaseName, mbariNrtStationNames, mbariNrtStationTypes,
            mbariNrtStationUrls, mbariNrtVariableInfo, mbariCourtesy,
            minX, maxX, minY, maxY, false, //clearCache
            ensureUpToDate, throwExceptionIfAnyTrouble, 
            activeCacheOpendapStations, activePointDataSets); 
    }

    /**
     * This ensures the MBARI science quality cache files exist 
     * and adds pointDataSets to activePointDataSets.
     *
     * @param pointsDir the points directory (to which mbari/ will be added)
     * @param minX the min longitude of interest
     * @param maxX the max longitude of interest
     * @param minY the min latitude of interest
     * @param maxY the max latitude of interest
     * @param ensureUpToDate if true, this makes sure that the caches are up-to-date.
     * @param activeCacheOpendapStations the list of valid cacheOpendapStations that will be added to
     * @param activePointDataSets the list of valid pointDataSets that will be added to
     * @throws Exception if trouble
     */
    public static void makeMbariSqCachesAndDataSets(
        String pointsDir, double minX, double maxX, double minY, double maxY,
        boolean ensureUpToDate, boolean throwExceptionIfAnyTrouble, 
        List activeCacheOpendapStations, List activePointDataSets) throws Exception {

        makeCachesAndDataSets(
            pointsDir, mbariStationBaseName, mbariSqStationNames, mbariSqStationTypes,
            mbariSqStationUrls, mbariSqVariableInfo, mbariCourtesy,
            minX, maxX, minY, maxY, false, //clearCache
            ensureUpToDate, throwExceptionIfAnyTrouble, 
            activeCacheOpendapStations, activePointDataSets); 
    }

    /**
     * This remakes the caches of the mbari nrt and sq station data in f:/data/MBARI.
     */
    public static void remakeMbariCachesAndDataSets() throws Exception {

        File2.deleteAllFiles("c:/data/MBARI");

        ArrayList cacheOpendapStations = new ArrayList();
        ArrayList pointDataSets = new ArrayList();
        makeMbariNrtCachesAndDataSets(
            "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
            true, //throwExceptionIfAnyTrouble    (this commonly true just here for testing)
            cacheOpendapStations, pointDataSets);

        makeMbariSqCachesAndDataSets(
            "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
            true, //throwExceptionIfAnyTrouble    (this commonly true just here for testing) 
            cacheOpendapStations, pointDataSets);
    }


    /** This tests using this class to make caches and pointDataSets of mbari nrt station data. 
     This also tests PointVectors.
     */
    public static void testMakeCachesAndPointDataSets(boolean clearCache) throws Exception {
        verbose = true;
        DataHelper.verbose = true;
        Table.verbose = true;
        Table.reallyVerbose = true;
        StationVariableNc4D.verbose = true;
        PointDataSet.verbose = true;

        PointDataSet pds = null;
        Table table = null;
        String2.log("\n*** PointDataSetFromStationVariables.testMakeCachesAndPointDataSets(" + clearCache + ")");

        try {
            //make mbari nrt PointDataSet
            ArrayList cacheOpendapStations = new ArrayList();
            ArrayList pointDataSets = new ArrayList();
            makeMbariNrtCachesAndDataSets(
                "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
                true, //throwExceptionIfAnyTrouble
                cacheOpendapStations, pointDataSets);

            //*** M0 current, zonal: do comparisons to info returned by ascii requests in test() above
            PointDataSet pdsu = null;
            String tName = "Current, Near Real Time, Zonal";
            for (int i = 0; i < pointDataSets.size(); i++) {
                PointDataSet tpds = (PointDataSet)pointDataSets.get(i);
                if (tpds.boldTitle.startsWith(tName)) {
                    pdsu = tpds;
                    break;
                }
            }
            if (pdsu == null) {
                String2.log(tName + " not found in ");
                for (int i = 0; i < pointDataSets.size(); i++) {
                    PointDataSet tpds = (PointDataSet)pointDataSets.get(i);
                    String2.log("  " + tpds.boldTitle);
                }
                Test.error("");
            }
//    Row             LON            LAT          DEPTH           TIME             ID u_component_un
//      0       -121.9031       36.83338             18     1194293891 MBARI M0 NRT a         -0.031
//      1       -121.9031       36.83338             18     1194294491 MBARI M0 NRT a         -0.035
            table = pdsu.makeSubset(-121.91, -121.90, 36.83, 36.84, 15, 20,
                Calendar2.epochSecondsToIsoStringT(1194293891),
                Calendar2.epochSecondsToIsoStringT(1194294491));
            String2.log(table.toString());
            Test.ensureEqual(table.nRows(), 2, "");
            Test.ensureEqual(table.nColumns(), 6, "");
            Test.ensureEqual(table.getColumnName(0), "LON", "");
            Test.ensureEqual(table.getColumnName(1), "LAT", "");
            Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
            Test.ensureEqual(table.getColumnName(3), "TIME", "");
            Test.ensureEqual(table.getColumnName(4), "ID", "");
            Test.ensureEqual(table.getColumnName(5), "u_component_uncorrected", "");
            Test.ensureEqual(table.getFloatData(0,0), -121.9031f, "");
            Test.ensureEqual(table.getFloatData(1,0), 36.83338f, "");
            Test.ensureEqual(table.getFloatData(2,0), 18f, "");
            Test.ensureEqual(table.getDoubleData(3,0), 1194293891, "");
            Test.ensureEqual(table.getStringData(4,0), "MBARI M0 NRT adcp", "");
            Test.ensureEqual(table.getFloatData(5,0), -.031f, ""); //dataset returns m/s (not original cm/s)

            Test.ensureEqual(table.getFloatData(0,1), -121.9031f, "");
            Test.ensureEqual(table.getFloatData(1,1), 36.83338f, "");
            Test.ensureEqual(table.getFloatData(2,1), 18f, "");
            Test.ensureEqual(table.getDoubleData(3,1), 1194294491, "");
            Test.ensureEqual(table.getStringData(4,1), "MBARI M0 NRT adcp", "");
            Test.ensureEqual(table.getFloatData(5,1), -.035f, ""); //dataset returns m/s (not original cm/s)

            //M0 current, meridional
            PointDataSet pdsv = null;
            tName = "Current, Near Real Time, Meridional";
            for (int i = 0; i < pointDataSets.size(); i++) {
                PointDataSet tpds = (PointDataSet)pointDataSets.get(i);
                if (tpds.boldTitle.indexOf(tName) >= 0) {
                    pdsv = tpds;
                    break;
                }
            }
            if (pdsv == null) {
                String2.log(tName + " not found in ");
                for (int i = 0; i < pointDataSets.size(); i++) {
                    PointDataSet tpds = (PointDataSet)pointDataSets.get(i);
                    String2.log("  " + tpds.boldTitle);
                }
                Test.error("");
            }
            table = pdsv.makeSubset(-121.91, -121.90, 36.83, 36.84, 15, 20,
                Calendar2.epochSecondsToIsoStringT(1194293891),
                Calendar2.epochSecondsToIsoStringT(1194294491));
            String2.log(table.toString());
            Test.ensureEqual(table.nRows(), 2, "");
            Test.ensureEqual(table.nColumns(), 6, "");
            Test.ensureEqual(table.getColumnName(0), "LON", "");
            Test.ensureEqual(table.getColumnName(1), "LAT", "");
            Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
            Test.ensureEqual(table.getColumnName(3), "TIME", "");
            Test.ensureEqual(table.getColumnName(4), "ID", "");
            Test.ensureEqual(table.getColumnName(5), "v_component_uncorrected", "");
            Test.ensureEqual(table.getFloatData(0,0), -121.9031f, "");
            Test.ensureEqual(table.getFloatData(1,0), 36.83338f, "");
            Test.ensureEqual(table.getFloatData(2,0), 18f, "");
            Test.ensureEqual(table.getDoubleData(3,0), 1194293891, "");
            Test.ensureEqual(table.getStringData(4,0), "MBARI M0 NRT adcp", "");
            Test.ensureEqual(table.getFloatData(5,0), .145f, ""); //dataset returns m/s (not original cm/s)

            Test.ensureEqual(table.getFloatData(0,1), -121.9031f, "");
            Test.ensureEqual(table.getFloatData(1,1), 36.83338f, "");
            Test.ensureEqual(table.getFloatData(2,1), 18f, "");
            Test.ensureEqual(table.getDoubleData(3,1), 1194294491, "");
            Test.ensureEqual(table.getStringData(4,1), "MBARI M0 NRT adcp", "");
            Test.ensureEqual(table.getFloatData(5,1), .139f, ""); //dataset returns m/s (not original cm/s)

            //** do equivalent test of PointVectors.makeAveragedTimeSeries
            table = PointVectors.makeAveragedTimeSeries(pdsu, pdsv,
                -121.91, -121.90, 36.83, 36.84, 15, 20,
                Calendar2.epochSecondsToIsoStringT(1194293891),
                Calendar2.epochSecondsToIsoStringT(1194294491), "pass");
            String2.log(table.toString());
            Test.ensureEqual(table.nRows(), 2, "");
            Test.ensureEqual(table.nColumns(), 7, "");
            Test.ensureEqual(table.getColumnName(0), "LON", "");
            Test.ensureEqual(table.getColumnName(1), "LAT", "");
            Test.ensureEqual(table.getColumnName(2), "DEPTH", "");
            Test.ensureEqual(table.getColumnName(3), "TIME", "");
            Test.ensureEqual(table.getColumnName(4), "ID", "");
            Test.ensureEqual(table.getColumnName(5), "u_component_uncorrected", "");
            Test.ensureEqual(table.getColumnName(6), "v_component_uncorrected", "");
            Test.ensureEqual(table.getFloatData(0,0), -121.9031f, "");
            Test.ensureEqual(table.getFloatData(1,0), 36.83338f, "");
            Test.ensureEqual(table.getFloatData(2,0), 18f, "");
            Test.ensureEqual(table.getDoubleData(3,0), 1194293891, "");
            Test.ensureEqual(table.getStringData(4,0), "MBARI M0 NRT adcp", "");
            Test.ensureEqual(table.getFloatData(5,0), -.031f, ""); //dataset returns m/s (not original cm/s)
            Test.ensureEqual(table.getFloatData(6,0), .145f, ""); //dataset returns m/s (not original cm/s)

            Test.ensureEqual(table.getFloatData(0,1), -121.9031f, "");
            Test.ensureEqual(table.getFloatData(1,1), 36.83338f, "");
            Test.ensureEqual(table.getFloatData(2,1), 18f, "");
            Test.ensureEqual(table.getDoubleData(3,1), 1194294491, "");
            Test.ensureEqual(table.getStringData(4,1), "MBARI M0 NRT adcp", "");
            Test.ensureEqual(table.getFloatData(5,1), -.035f, ""); //dataset returns m/s (not original cm/s)
            Test.ensureEqual(table.getFloatData(6,1), .139f, ""); //dataset returns m/s (not original cm/s)
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e)); 
        }


    }

    /**
     * This runs a test of this class and StationVariableNc4D.addToSubset.
     *
     * @throws Exception if trouble or no data
     */
    public static void testMbariSqStations() throws Exception {
        String2.log("\n*** test PointDataSetFromStationVariables.testMbariSqStations");
        CacheOpendapStation.verbose = true;
        CacheOpendapStation.reallyVerbose = true;
        DataHelper.verbose = true;
        PointDataSet.verbose = true;
        PointDataSet.reallyVerbose = true;
        GroupVariable.verbose = false;
        Table.verbose = true;
        Table.reallyVerbose = true;

        String dir = "c:/data/"; 
        //to view, use, e.g.,:
        //\programs\nc361\ncdump -v DEPTH_HR f:\data\MBARI\MBARI_M0_SQ_adcp.nc
        //http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?TIME_HR

        ArrayList list = new ArrayList();
        PointDataSetFromStationVariables pointDataSet = null;
        Table table;
        double seconds;

        try {
            //make the pointDataSets
            makeMbariSqCachesAndDataSets(dir,
                -180, 180, -90, 90, true, //ensureUpToDate
                true, //throwExceptionIfAnyTrouble
                null, list);
            Test.ensureEqual(list.size(), 11, "");

            //find PMBucur
            for (int i = 0; i < list.size(); i++) {
                pointDataSet = (PointDataSetFromStationVariables)list.get(i);
                if (pointDataSet.internalName.equals("PMBcrus"))
                    break;
            }
            Test.ensureNotNull(pointDataSet, "pointDataSet is null");
       
            //PMBucur combines various depthLevels to make uniqueDepthLevels
            //m0 DEPTH_HR = 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55 
            //m1 DEPTH_HR = 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80,
            //    85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155,
            //    160, 165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225,
            //    230, 235, 240, 245, 250, 255, 260, 265, 270, 275, 280, 285, 290, 295,
            //    300, 305, 310, 315, 320, 325, 330, 335, 340, 345, 350, 355, 360, 365,
            //    370, 375, 380, 385, 390, 395, 400, 405, 410, 415, 420, 425, 430, 435,
            //    440, 445, 450, 455, 460, 465, 470, 475, 480, 485, 490, 495, 500 ;
            Test.ensureEqual(String2.toCSSVString(pointDataSet.uniqueDepthLevels), 
                "5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, " +
                "85, 90, 95, 100, 105, 110, 115, 120, 125, 130, 135, 140, 145, 150, 155, 160, " +
                "165, 170, 175, 180, 185, 190, 195, 200, 205, 210, 215, 220, 225, 230, 235, 240, " +
                "245, 250, 255, 260, 265, 270, 275, 280, 285, 290, 295, 300, 305, 310, 315, 320, " +
                "325, 330, 335, 340, 345, 350, 355, 360, 365, 370, 375, 380, 385, 390, 395, 400, " +
                "405, 410, 415, 420, 425, 430, 435, 440, 445, 450, 455, 460, 465, 470, 475, 480, " +
                "485, 490, 495, 500", "");

            Test.ensureEqual(pointDataSet.getStationMinTime("MBARI M0 SQ adcp"), 
                1086285600, "");  //from opendap
            Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(pointDataSet.getStationMinTime("MBARI M0 SQ adcp")), 
                "2004-06-03T18:00:00", "");  //converted to iso format
            String oneDayLater  = "2004-06-04T18:00:00";
            String oneDayLater1 = "2004-06-04T19:00:00";

            //test m0 ucur   1 day later
            String2.log("\ntest m0");
            //http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?U_COMPONENT_UNCORR_HR[24:1:25][1:1:2][0:1:0][0:1:0]
            //reply:
            //  U_COMPONENT_UNCORR_HR, [2][2][1][1]
            //  [0][0][0], 2.91875
            //  [0][1][0], 3.06146
            //  [1][0][0], 4.20833
            //  [1][1][0], 3.36562
            //  TIME_HR, [2]
            //  1086372000, 1086375600
            //  DEPTH_HR, [2]
            //  10, 15
            //  LATITUDE_HR, [1]
            //  36.8339996337891
            //  LONGITUDE_HR, [1]
            //  238.100006103516        
            table = pointDataSet.makeSubset(238.1, 238.1, 36.834, 36.834, 
                9, 16, //the second and 3rd depth values,  10 and 15
                oneDayLater, oneDayLater1);
            Test.ensureEqual(table.nRows(), 4, "");
            Test.ensureEqual(table.nColumns(), 6, "");

            //test m0 ucur  1 day later, 2nd z
            seconds = Calendar2.isoStringToEpochSeconds(oneDayLater);
            String2.log(oneDayLater + " (oneDayLater) converts to " + seconds);
            Test.ensureEqual(table.getFloatData(0, 0), 238.1f, "");
            Test.ensureEqual(table.getFloatData(1, 0), 36.834f, "");
            Test.ensureEqual(table.getDoubleData(2, 0), 10, "");
            Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
            Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 SQ adcp", "");
            Test.ensureEqual(table.getFloatData(5, 0), .0291875f, ""); //ucur

            //test m0 ucur   1 day + 1 hour later, 2nd z
            seconds = Calendar2.isoStringToEpochSeconds(oneDayLater1);
            String2.log(oneDayLater1 + " (oneDayLater1) converts to " + seconds);
            Test.ensureEqual(table.getFloatData(0, 1), 238.1f, "");
            Test.ensureEqual(table.getFloatData(1, 1), 36.834f, "");
            Test.ensureEqual(table.getDoubleData(2, 1), 10, "");
            Test.ensureEqual(table.getDoubleData(3, 1), seconds, "");
            Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 SQ adcp", "");
            Test.ensureEqual(table.getFloatData(5, 1), .0420833f, ""); //ucur

            //test m0 ucur  1 day later, 3rd z
            seconds = Calendar2.isoStringToEpochSeconds(oneDayLater);
            Test.ensureEqual(table.getFloatData(0, 2), 238.1f, "");
            Test.ensureEqual(table.getFloatData(1, 2), 36.834f, "");
            Test.ensureEqual(table.getDoubleData(2, 2), 15, "");
            Test.ensureEqual(table.getDoubleData(3, 2), seconds, "");
            Test.ensureEqual(table.getStringData(4, 2), "MBARI M0 SQ adcp", "");
            Test.ensureEqual(table.getFloatData(5, 2), .0306146f, ""); //ucur

            //test m0 ucur   1 day + 1 hour later, 3rd z
            seconds = Calendar2.isoStringToEpochSeconds(oneDayLater1);
            Test.ensureEqual(table.getFloatData(0, 3), 238.1f, "");
            Test.ensureEqual(table.getFloatData(1, 3), 36.834f, "");
            Test.ensureEqual(table.getDoubleData(2, 3), 15, "");
            Test.ensureEqual(table.getDoubleData(3, 3), seconds, "");
            Test.ensureEqual(table.getStringData(4, 3), "MBARI M0 SQ adcp", "");
            Test.ensureEqual(table.getFloatData(5, 3), .03365625f, ""); //ucur


            //*** and the related tests of makeAveragedTimeSeries  (with 2 z values!)
            table = pointDataSet.makeAveragedTimeSeries(238.1, 238.1, 36.834, 36.834, 
                9, 16, //the second and 3rd depth values,  10 and 15
                "2004-06-04", "2004-06-04", "1 day");  
            Test.ensureEqual(table.nRows(), 2, "");
            Test.ensureEqual(table.nColumns(), 6, "");

            //test m0 ucur  1 day later, 2nd z
            //0 time is 18:00:00 so start of next day is element 6
            //http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?U_COMPONENT_UNCORR_HR[6:1:29][1:1:1][0:1:0][0:1:0]
            String values="-6.70208, -3.53021, 0.135417, 3.94375, 2.28646, 2.90729, 2.51354, -0.00624998, -3.62604, -8.55, " +
                "-11.5198, -10.8156, -12.4094, -9.25833, -7.99271, -5.2, -3.44896, 0.759375, 2.91875, 4.20833" +
                ", 5.21667, 6.64792, 2.33646, 0.494792";
            DoubleArray da = new DoubleArray(String2.csvToDoubleArray(values));
            da.scaleAddOffset(.01, 0);  //convert cm/s to m/s
            double stats[] = da.calculateStats();
            seconds = Calendar2.isoStringToEpochSeconds("2004-06-04 12:00:00");
            Test.ensureEqual(table.getFloatData(0, 0), 238.1f, "");
            Test.ensureEqual(table.getFloatData(1, 0), 36.834f, "");
            Test.ensureEqual(table.getDoubleData(2, 0), 10, "");
            Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
            Test.ensureEqual(table.getStringData(4, 0), "MBARI M0 SQ adcp", "");
            Test.ensureEqual(table.getFloatData(5, 0), (float)(stats[PrimitiveArray.STATS_SUM]/stats[PrimitiveArray.STATS_N]), ""); //ucur

            //test m0 ucur   1 day + 1 hour later, 3rd z
            //http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.ascii?U_COMPONENT_UNCORR_HR[6:1:29][2:1:2][0:1:0][0:1:0]
            values="-5.79792, -3.39375, 2.05208, 4.26875, 2.09687, 1.73854, 0.369792, -0.767708" +
                ", -1.71354, -4.38646, -7.70208, -9.68125, -14.2104, -11.6458, -9.92083, -6.72604" +
                ", -3.97604, 0.278125, 3.06146, 3.36562, 2.60313, 2.70417, 0.308333, 0.01875";
            da = new DoubleArray(String2.csvToDoubleArray(values));
            da.scaleAddOffset(.01, 0);  //convert cm/s to m/s
            stats = da.calculateStats();
            Test.ensureEqual(table.getFloatData(0, 1), 238.1f, "");
            Test.ensureEqual(table.getFloatData(1, 1), 36.834f, "");
            Test.ensureEqual(table.getDoubleData(2, 1), 15, ""); //yes, it averages depth levels separately
            Test.ensureEqual(table.getDoubleData(3, 1), seconds, "");
            Test.ensureEqual(table.getStringData(4, 1), "MBARI M0 SQ adcp", "");
            Test.ensureEqual(table.getFloatData(5, 1), (float)(stats[PrimitiveArray.STATS_SUM]/stats[PrimitiveArray.STATS_N]), ""); //ucur
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e)); 
        }

    }

    /**
     * This runs a test of this class and StationVariableNc4D.makeSubset with NDBC stations.
     *
     * @throws Exception if trouble or no data
     */
    public static void testNc4DMakeSubset() throws Exception {
        String2.log("\n*** test PointDataSetFromStationVariables.testNc4DMakeSubset");
        DataHelper.verbose = true;
        PointDataSet.verbose = true;
        GroupVariable.verbose = false;
        NdbcMetStation.verbose = true;

        //String2.log(DataHelper.ncDumpString("c:/data/ndbcMet4D/TAML1.nc", false));

        ArrayList list = new ArrayList();
        PointDataSet pointDataSet = null;
        Table table;
        double seconds;

        //option 1) more direct test of makePointDataSets
        //list.clear();
        //makePointDataSets(list,
        //    "PN2", //String internalDataSetBaseName, 
        //    "NDBC Meteorological", //String userDataSetBaseName, 
        //    "c:/data/ndbcMet/",
        //    //"(31201.nc|46088.nc|TAML1.nc)",  //just 3 files
        //    ".+\\.nc",                        //all files
        //    new String[]{ //variableInfo[]
        //        "WTMP` wtmp` Water Temperature` Rainbow`      Linear`   1`  8`  32` degree_C` NaN"},
        //        //"WSPU` wspu` Zonal Wind Speed`  BlueWhiteRed` Linear` 1` -20`  20` m s^-1` NaN",
        //        //"WSPV` wspv` Merid. Wind Speed` BlueWhiteRed` Linear` 1` -20`  20` m s^-1` NaN"},
        //    NdbcMetStation.courtesy, -180, 180, -90, 90); 
        //Test.ensureEqual(list.size(), 1, "");
        //pointDataSet = (PointDataSet)list.get(0);

        //option 2) test of NdbcMetStation.addPointDataSets
        list.clear();
        NdbcMetStation.addPointDataSets(list, "c:/data/ndbcMet/", -180, 180, -90, 90);
        for (int i = 0; i < list.size(); i++) {
            pointDataSet = (PointDataSet)list.get(i);
            if (pointDataSet.internalName.equals("PNBwtmp"))
                break;
        }
        Test.ensureNotNull(pointDataSet, "pointDataSet is null");


        //test 31201
        String2.log("\n*** whole world: test 31201");
        //table is x,y,z,t,id,data
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 25 18 00 999 99.0 99.0  3.90  8.00 99.00 999 9999.0 999.0  23.9 999.0 99.0 99.00
        //use of floats ensures that makeSubset() can handle imprecise requests
        table = pointDataSet.makeSubset(311.87f, 311.87f, -27.7f, -27.7f, 0, 0,
            "2005-04-25T18:00:00", "2005-04-25T18:00:00");
        seconds = Calendar2.isoStringToEpochSeconds("2005-04-25T18:00:00");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.nColumns(), 6, "");
        Test.ensureEqual(table.getDoubleData(0, 0), 311.87, "");
        Test.ensureEqual(table.getDoubleData(1, 0), -27.7, "");
        Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
        Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
        Test.ensureEqual(table.getStringData(4, 0), "NDBC 31201 met", "");
        Test.ensureEqual(table.getFloatData(5, 0), 23.9f, ""); //wtmp

        //test 46088    test lon 0..360
        String2.log("\n*** whole world: test 46088");
        //YYYY MM DD hh mm  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2005 12 31 23 30  11  1.9  2.2 99.00 99.00 99.00 999  987.7   8.5   8.6   7.4 99.0 99.00
        table = pointDataSet.makeSubset( -123.17f, -123.17f, 48.33f, 48.33f, 0, 0, 
            "2006-01-01T00:00:00", "2006-01-01T00:00:00");
        seconds = Calendar2.isoStringToEpochSeconds("2006-01-01T00:00:00");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.nColumns(), 6, "");
        Test.ensureEqual(table.getDoubleData(0, 0), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1, 0), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2, 0), 0f, "");
        Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
        Test.ensureEqual(table.getStringData(4, 0), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5, 0), 8.6f, ""); //wtmp
        
        //test TAML1
        String2.log("\n*** whole world: test TAML1");
        //YYYY MM DD hh  WD  WSPD GST  WVHT  DPD   APD  MWD  BAR    ATMP  WTMP  DEWP  VIS  TIDE
        //2004 12 31 23  90  5.1  6.2 99.00 99.00 99.00 999 1022.9  16.3  13.7  15.2 99.0 -0.04  //last row of first year
        table = pointDataSet.makeSubset( -90.67f, -90.67f, 
            29.19f, 29.19f, 0, 0, "2004-01-01T01:00:00", "2004-01-01T01:00:00");
        seconds = Calendar2.isoStringToEpochSeconds("2004-01-01T01:00:00");
        Test.ensureEqual(table.nRows(), 1, "");
        Test.ensureEqual(table.nColumns(), 6, "");
        Test.ensureEqual(table.getDoubleData(0, 0), -90.67, "");
        Test.ensureEqual(table.getDoubleData(1, 0), 29.19, "");
        Test.ensureEqual(table.getDoubleData(2, 0), 0, "");
        Test.ensureEqual(table.getDoubleData(3, 0), seconds, "");
        Test.ensureEqual(table.getStringData(4, 0), "NDBC TAML1 met", "");
        Test.ensureEqual(table.getFloatData(5, 0), 13.7f, ""); //wtmp

        //all stations, 1 time     ? ms
        String2.log("\n*** whole world: all stations, 1 time, lonPM180");
        table = pointDataSet.makeSubset(
            -180, 180, -90, 90, 0, 0, "2004-01-07", "2004-01-07");
        //String2.log(tTable.toString(Integer.MAX_VALUE));                      
        int tn = table.nRows();
        //this changes, but it is good to keep the test in case the number changes badly (e.g., smaller)
        Test.ensureEqual(table.nRows(), 658, "all sta, 1 time, nRows");
        Test.ensureEqual(table.nColumns(), 6, "all sta, 1 time, nColumns");
        Test.ensureEqual(Calendar2.isoStringToEpochSeconds("2004-01-07"), 1073433600, "time check");
        for (int i = 0; i < tn; i++) {
            Test.ensureEqual(table.getDoubleData(3, i), 1073433600, "all sta, 1 time, time, row " + i);
        }
        int row = table.getColumn(0).indexOf("-72.66", 0);
        Test.ensureEqual(table.getDoubleData( 0, row), -72.66,  "all sta, 1 time, lon row 0");
        Test.ensureEqual(table.getDoubleData( 1, row), 34.68,   "all sta, 1 time, lat row 0");
        Test.ensureEqual(table.getFloatData( 5, row), 20.7f,    "all sta, 1 time, wtmp row 0");
        row = table.getColumn(0).indexOf("-75.35", 0);
        Test.ensureEqual(table.getDoubleData( 0, row), -75.35,  "all sta, 1 time, lon row 1");
        Test.ensureEqual(table.getDoubleData( 1, row), 32.31,   "all sta, 1 time, lat row 1");
        Test.ensureEqual(table.getFloatData( 5, row), 23.3f,    "all sta, 1 time, wtmp row 1");


        //westus, 2 time points       ? ms
        String2.log("\n*** westus, 2 time points");
        table = pointDataSet.makeSubset(
            -135, -105, 22, 50, 0, 0, "2004-01-07T00", "2004-01-07T01");
        //String2.log(table.toString(1000));
        //nRows changes a little sometimes...
        Test.ensureEqual(table.nRows(), 155, "westus, 2 time points, nRows"); 
        Test.ensureEqual(table.nColumns(), 6, "westus, 2 time points, nColumns");
        row = table.getColumn(0).indexOf("-118", 0);
        row = table.getColumn(3).indexOf("1073433600", row);
        Test.ensureEqual(table.getDoubleData(0, row), -118, "westus, 2 time points, lon row 50");
        Test.ensureEqual(table.getDoubleData(1, row), 32.5, "westus, 2 time points, lat row 50");
        Test.ensureEqual(table.getDoubleData(3, row), 1073433600,  "westus, 2 time points, time row 50");
        Test.ensureEqual(table.getFloatData(5, row), 15.6f, "westus, 2 time points, wtmp row 50");
        row = table.getColumn(0).indexOf("-118", 0);
        row = table.getColumn(3).indexOf("1073437200", row);
        Test.ensureEqual(table.getDoubleData(0, row), -118, "westus, 2 time points, lon row 51");
        Test.ensureEqual(table.getDoubleData(1, row), 32.5, "westus, 2 time points, lat row 51");
        Test.ensureEqual(table.getDoubleData(3, row),        1073437200,  "westus, 2 time points, time row 51");
        Test.ensureEqual(table.getFloatData(5, row), 15.4f, "westus, 2 time points, wtmp row 51");


        //all stations 1 month, lonPM180
        String2.log("\n*** whole world: all stations, 1 month, lonPM180");
        table = pointDataSet.makeSubset(
            -180, 180, -90, 90, 0, 0, "2004-01-01", "2004-02-01");
        //String2.log(table.toString(1000));
        table.convertToFakeMissingValues(); //so I see what file will look like
        String2.log(table.toString("row", 1));
        tn = table.nRows();
        for (int i = 0; i < tn; i++) {
            double tLon = table.getDoubleData(0, i);
            Test.ensureTrue(tLon >= -180 && tLon < 180, "all sta, 1 month, lonPM180, avg, row" + i + "=" + tLon); 
        }
        //Test.ensureEqual(table.nRows(), 130227, "all sta, 1 month, lonPM180, avg, nRows"); //circular logic, changes a little sometimes
        Test.ensureEqual(table.nColumns(), 6, "all sta, 1 month, lonPM180, avg, nColumns"); 
    }


    /**
     * This runs a test of this class and PointDataSet.makeAveragedTimeSeries with NDBC stations.
     *
     * @throws Exception if trouble or no data
     */
    public static void testNc4DMakeAveragedTimeSeries() throws Exception {
        String2.log("\n*** test PointDataSetFromStationVariables.testNc4DmakeAveragedTimeSeries");
        DataHelper.verbose = true;
        PointDataSet.verbose = true;
        GroupVariable.verbose = false;
        NdbcMetStation.verbose = true;

        //String2.log(DataHelper.ncDumpString("c:/data/ndbcMet4D/TAML1.nc", false));

        ArrayList list = new ArrayList();
        PointDataSet pointDataSet = null;
        Table table;
        double seconds;


        //*** test of NdbcMetStation.addPointDataSets  WITH RANGE LIMITATION 
        list.clear();
        NdbcMetStation.addPointDataSets(list, "c:/data/ndbcMet/", 
            -135+360, -105+360, 22, 50);  //set range limitation to westus in 0..360
        pointDataSet = null;
        for (int i = 0; i < list.size(); i++) {
            pointDataSet = (PointDataSet)list.get(i);
            if (pointDataSet.internalName.equals("PNBwtmp"))
                break;
        }
        Test.ensureNotNull(pointDataSet, "pointDataSet is null");

        //westus, 2 time points       ? ms
        String2.log("\n*** westus: 2 time points");
        table = pointDataSet.makeSubset(
            -135, -105, 22, 50, 0, 0, "2004-01-07T00", "2004-01-07T01");   //then ask for data in -180..180
        //String2.log(table.toString(1000));
        //nRows changes sometimes...
        Test.ensureEqual(table.nRows(), 155, "westus, 2 time points, nRows");
        Test.ensureEqual(table.nColumns(), 6, "westus, 2 time points, nColumns");
        int row = table.getColumn(0).indexOf("-118", 0);
        row = table.getColumn(3).indexOf("1073433600", row);
        Test.ensureEqual(table.getDoubleData(0, row), -118, "westus, 2 time points, lon row");
        Test.ensureEqual(table.getDoubleData(1, row), 32.5, "westus, 2 time points, lat row");
        Test.ensureEqual(table.getDoubleData(3, row), 1073433600,  "westus, 2 time points, time row");
        Test.ensureEqual(table.getFloatData(5, row), 15.6f, "westus, 2 time points, wtmp row");
        Test.ensureEqual(table.getDoubleData(0, row+1), -118, "westus, 2 time points, lon row+1");
        Test.ensureEqual(table.getDoubleData(1, row+1), 32.5, "westus, 2 time points, lat row+1");
        Test.ensureEqual(table.getDoubleData(3, row+1), 1073437200,  "westus, 2 time points, time row+1");
        Test.ensureEqual(table.getFloatData(5, row+1), 15.4f, "westus, 2 time points, wtmp row+1");

        //*** test of makeAveragedTimeSeries    -123.17 48.33 is station 46088
        //1 observation
        table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0,0,
            "2005-10-01", "2005-10-02", "1 observation");
        Test.ensureEqual(table.nRows(), 25, "");
        Test.ensureEqual(table.getDoubleData(0,0), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,0), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,0), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,0)), "2005-10-01T00:00:00", "");
        Test.ensureEqual(table.getStringData(4,0), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,0), 11.4f, "");

        Test.ensureEqual(table.getDoubleData(0,1), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,1), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,1), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,1)), "2005-10-01T01:00:00", "");
        Test.ensureEqual(table.getStringData(4,1), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,1), 11.4f, "");

        Test.ensureEqual(table.getDoubleData(0,24), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,24), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,24), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,24)), "2005-10-02T00:00:00", "");
        Test.ensureEqual(table.getStringData(4,24), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,24), 11.9f, "");

        //1 day
        table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0,0,
            "2005-10-01", "2005-11-01", "1 day");
        Test.ensureEqual(table.nRows(), 32, "");
        Test.ensureEqual(table.getDoubleData(0,0), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,0), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,0), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,0)), "2005-10-01T12:00:00", "");
        Test.ensureEqual(table.getStringData(4,0), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,0), 11.2375f, "");
        double average = pointDataSet.calculateAverage(    //independent test
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-10-01 00:00:00", "2005-10-01 23:00:00");
        Test.ensureEqual((float)average, 11.2375f, "");

        Test.ensureEqual(table.getDoubleData(0,1), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,1), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,1), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,1)), "2005-10-02T12:00:00", "");
        Test.ensureEqual(table.getStringData(4,1), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,1), 11.154166f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-10-02 00:00:00", "2005-10-02 23:00:00");
        Test.ensureEqual((float)average, 11.154166f, "");

        Test.ensureEqual(table.getDoubleData(0,31), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,31), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,31), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,31)), "2005-11-01T12:00:00", "");
        Test.ensureEqual(table.getStringData(4,31), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,31), 10.025001f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-11-01 00:00:00", "2005-11-01 23:00:00");
        Test.ensureEqual((float)average, 10.025001f, "");

        //8 day
        table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0,0,
            "2005-10-01", "2005-11-01", "8 day");
        Test.ensureEqual(table.nRows(), 32, "");
        Test.ensureEqual(table.getDoubleData(0,0), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,0), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,0), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,0)), "2005-10-01T00:00:00", "");
        Test.ensureEqual(table.getStringData(4,0), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,0), 11.261979f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-09-27 00:00:00", "2005-10-04 23:00:00");
        Test.ensureEqual((float)average, 11.261979f, "");

        Test.ensureEqual(table.getDoubleData(0,1), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,1), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,1), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,1)), "2005-10-02T00:00:00", "");
        Test.ensureEqual(table.getStringData(4,1), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,1), 11.144271f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-09-28 00:00:00", "2005-10-05 23:00:00");
        Test.ensureEqual((float)average, 11.144271f, "");

        Test.ensureEqual(table.getDoubleData(0,31), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,31), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,31), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,31)), "2005-11-01T00:00:00", "");
        Test.ensureEqual(table.getStringData(4,31), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,31), 10.145312f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-10-28 00:00:00", "2005-11-04 23:00:00");
        Test.ensureEqual((float)average, 10.145312f, "");

        //1 month
        table = pointDataSet.makeAveragedTimeSeries(-123.17, -123.17, 48.33, 48.33, 0,0,
            "2004-10-01", "2005-10-01", "1 month");
        Test.ensureEqual(table.nRows(), 13, "");
        Test.ensureEqual(table.getDoubleData(0,0), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,0), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,0), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,0)), "2004-10-16T12:00:00", "");
        Test.ensureEqual(table.getStringData(4,0), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,0), 10.540457f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2004-10-01 00:00:00", "2004-10-31 23:00:00");
        Test.ensureEqual((float)average, 10.540457f, "");

        Test.ensureEqual(table.getDoubleData(0,1), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,1), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,1), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,1)), "2004-11-16T00:00:00", "");
        Test.ensureEqual(table.getStringData(4,1), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,1), 9.550487f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2004-11-01 00:00:00", "2004-11-30 23:00:00");
        Test.ensureEqual((float)average, 9.550487f, "");

        Test.ensureEqual(table.getDoubleData(0,12), -123.17, "");
        Test.ensureEqual(table.getDoubleData(1,12), 48.33, "");
        Test.ensureEqual(table.getDoubleData(2,12), 0, "");
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(table.getDoubleData(3,12)), "2005-10-16T12:00:00", "");
        Test.ensureEqual(table.getStringData(4,12), "NDBC 46088 met", "");
        Test.ensureEqual(table.getFloatData(5,12), 10.479704f, "");
        average = pointDataSet.calculateAverage(
            -123.17, -123.17, 48.33, 48.33, 0,0, "2005-10-01 00:00:00", "2005-10-31 23:00:00");
        Test.ensureEqual((float)average, 10.479704f, "");

        String2.log("\n  end PointDataSetFromStationVariables.testNc4DmakeAveragedTimeSeries");
    }



    /**
     * This runs a test of this class and StationVariableOpendap4D.
     *
     * @throws Exception if trouble or no data
     */
    public static void test() throws Exception {
        //PointDataSet.reallyVerbose = true;
        //inDevelopment = true;
/*
        //based on ndbc data
        testNc4DMakeSubset();
        testNc4DMakeAveragedTimeSeries();
        //String2.pressEnterToContinue("Check things over.");
*/
        /** MBARI datasets are inactive as of 2008-07-07
        //mbari data
        //testMakeCachesAndPointDataSets(false); //clearCache
        //testMbariSqStations();

        //test mbari nrt data
        try {
            ArrayList cacheOpendapStations = new ArrayList();
            ArrayList pointDataSets = new ArrayList();
            makeMbariNrtCachesAndDataSets(
                "c:/data/", -180, 180, -90, 90, true, //ensureUpToDate
                true, //throwExceptionIfAnyTrouble  (this commonly true just here for testing)
                cacheOpendapStations, pointDataSets);
            Test.ensureEqual(cacheOpendapStations.size(), 9, "");
            Test.ensureEqual(pointDataSets.size(), 10, "");
        } catch (Exception e) {
            String2.log(MustBe.throwableToString(e));
            String2.pressEnterToContinue("\nRecover from mbari nrt failure?");
        }
        */

    }
     

}
