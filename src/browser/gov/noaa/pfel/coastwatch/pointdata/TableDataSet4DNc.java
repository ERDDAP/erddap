/* 
 * TableDataSet4DNc Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.pointdata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import java.util.ArrayList;

// from netcdfAll-x.jar
import ucar.nc2.*;
import ucar.ma2.*;

/** 
 * This class represents a TableDataSet with data stored in 4D Nc files.
 * Each matching file in the directory corresponds to an 
 * individual (e.g., a station or trajectory).
 * 
 * The constructor searches for available data.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2007-03-06
 */
public class TableDataSet4DNc extends TableDataSet { 

    protected String directory;
    protected String fileNames[]; //[individual]
    protected String lonNameInFile;  
    protected String latNameInFile;
    protected String depthNameInFile;
    protected String timeNameInFile;
    protected double timeBaseSeconds, timeFactorToGetSeconds; //see griddata/Opendap
    protected boolean zIsUp;

    //protected double minLon[][]; //[individual]
    //protected double maxLon[][]; //[individual]
    //protected double minLat[][]; //[individual]
    //protected double maxLat[][]; //[individual]


    /**
     * The constructor for TableDataSet4DNc
     * <ul>
     * <li> The individuals (files) will be in alphabetical order.
     * <li> This tries to reject bad files and just keep good files.
     * <li> The files must use the same names for the coordinate variables.
     * <li> The files should have the same data variables, but there can be 
     *     variability -- this makes a list of all valid variable names 
     *     present in any of the files.
     * <li> For a given variable name, the files must use the same units
     *     and only one copy of the attributes is maintained.
     * <li> The time values in the files must be in ascending sorted order.
     * </ul>
     *
     * @param internalName e.g., 4NBmeto
     * @param datasetName e.g., "NDBC Meteorological"
     * @param directory the directory (with slash at the end).
     *    All of the regex-matching .nc files in this directory must be valid 4D .nc files.
     *    The 4D variables must have coordinate axes in correct order: time, depth, lat, lon.
     * @param regex the regex (e.g., ".+\\.nc") to find the relevant files in the directory.
     * @throws Exception if trouble
     */
    public TableDataSet4DNc(String internalName, String datasetName, 
            String directory, String regex) throws Exception {
//should constructor allow limits to lon and lat range of interest?

        if (verbose) String2.log("\nTableDataSet4DNc constructor. internalName=" + 
            internalName + 
            "\n  datasetName=" + datasetName + 
            "\n  directory=" + directory + " regex=" + regex);
        String errorInMethod = "TableDataSet4DNc(" + internalName + ")";
        if (verbose) String2.log(errorInMethod);
        errorInMethod = String2.ERROR + " in " + errorInMethod + ":\n";
        long time = System.currentTimeMillis();
        this.internalName = internalName;
        this.datasetName = datasetName;
        this.directory = directory;

        //and each variable's information
        StringArray activeDataVariableNames = new StringArray();
        dataElementType = new ArrayList();
        dataAttributes = new ArrayList();
        makeStandardIDAttributes();

        //get a list of matching file names
        String tFileNames[] = RegexFilenameFilter.list(directory, regex);

        //get each individual's information
        StringArray tActiveFileNames = new StringArray();
        StringArray tActiveIndividuals = new StringArray();
        for (int f = 0; f < tFileNames.length; f++) {

            //try to reject bad files and just keep good files
            try {
                //open the file
                NetcdfFile ncFile = NcHelper.openFile(directory + tFileNames[f]);
                try { //simple error messages within this try/catch

                    //get each variable's information
                    //Try to reject bad variables and just keep good ones.
                    Variable v4DVars[] = NcHelper.find4DVariables(ncFile, null);
                    for (int v = 0; v < v4DVars.length; v++) {
                        Variable variable = v4DVars[v];
                        String name = variable.getFullName();
                        String reject = "  " + tFileNames[f] + " reject variable=" + name + 
                                " because\n";

                        //already have it?
                        if (activeDataVariableNames.indexOf(name) >= 0)
                            continue;

                        //ensure it is numeric
                        DataType dataType = variable.getDataType();
                        if (dataType == DataType.CHAR ||
                            dataType == DataType.STRING ||
                            dataType == DataType.STRUCTURE) {
                            if (verbose) String2.log(reject + 
                                "dataType=" + NcHelper.getElementPAType(variable).toString());
                            continue;
                        }

                        //get/verify coordinate info
                        if (activeDataVariableNames.size() == 0) {
                            //get time info
                            Dimension dim = variable.getDimension(0);
                            String dimName = dim.getName();
                            if (!dimName.toLowerCase().startsWith("time")) {
                                if (verbose) String2.log(reject + 
                                    "timeName=" + dimName + " doesn't start with 'time'.");
                                continue;
                            }
                            timeNameInFile = dimName;
                            Variable dimVariable = ncFile.findVariable(dimName);
                            timeAttributes = new Attributes();
                            NcHelper.getVariableAttributes(dimVariable, timeAttributes);

                            //interpret time units (e.g., "days since 1985-01-01" or "days since 1985-1-1")
                            //it must be: <units> since <isoDate>   or exception thrown
                            //FUTURE: need to catch time zone information
                            String tsUnits = timeAttributes.getString("units");
                            double timeBaseAndFactor[] = Calendar2.getTimeBaseAndFactor(tsUnits); //throws exception if trouble
                            timeBaseSeconds = timeBaseAndFactor[0];
                            timeFactorToGetSeconds = timeBaseAndFactor[1];

                            //get depth info
                            dim = variable.getDimension(1);
                            dimName = dim.getName();
                            if (dimName.toLowerCase().startsWith("altitude")) {
                                zIsUp = true;
                            } else if (dimName.toLowerCase().startsWith("depth")) {
                                zIsUp = false;
                            } else {
                                if (verbose) String2.log(reject + 
                                    "depthName=" + dimName + " doesn't start with 'altitude' or 'depth'.");
                                continue;
                            }
                            depthNameInFile = dimName;
                            dimVariable = ncFile.findVariable(dimName);
                            depthAttributes = new Attributes();
                            NcHelper.getVariableAttributes(dimVariable, depthAttributes);
                            String zUnits = depthAttributes.getString("units");
                            if (zUnits == null ||
                                (!zUnits.toLowerCase().equals("meters") && 
                                 !zUnits.toLowerCase().equals("meter") && 
                                 !zUnits.toLowerCase().equals("m"))) {
                                if (verbose) String2.log(reject + 
                                    "depth units (" + zUnits + ") aren't 'meters', 'meter', or 'm'.");
                                continue;
                            }
                            //zIsUp = getString("positive");
                            // = globalAttributes.getString("geospatial_vertical_positive")
                            //     .equals("up");

                            //get lat info
                            dim = variable.getDimension(2);
                            dimName = dim.getName();
                            if (!dimName.toLowerCase().startsWith("lat")) {
                                if (verbose) String2.log(reject + 
                                    "latName=" + dimName + " doesn't start with 'lat'.");
                                continue;
                            }
                            latNameInFile = dimName;
                            dimVariable = ncFile.findVariable(dimName);
                            latAttributes = new Attributes();
                            NcHelper.getVariableAttributes(dimVariable, latAttributes);
                            String tUnits = latAttributes.getString("units").toLowerCase();
                            if (!tUnits.equals("degrees_north") &&
                                !tUnits.equals("degrees")) {
                                if (verbose) String2.log(reject + 
                                    "lat units=" + tUnits + " isn't 'degrees_north' or 'degrees'.");
                                continue;
                            }

                            //get lon info
                            dim = variable.getDimension(3);
                            dimName = dim.getName();
                            if (!dimName.toLowerCase().startsWith("lon")) {
                                if (verbose) String2.log(reject + 
                                    "lonName=" + dimName + " doesn't start with 'lon'.");
                                continue;
                            }
                            lonNameInFile = dimName;
                            dimVariable = ncFile.findVariable(dimName);
                            lonAttributes = new Attributes();
                            NcHelper.getVariableAttributes(dimVariable, lonAttributes);
                            tUnits = lonAttributes.getString("units").toLowerCase();
                            if (!tUnits.equals("degrees_east") &&
                                !tUnits.equals("degrees")) {
                                if (verbose) String2.log(reject + 
                                    "lon units=" + tUnits + " isn't 'degrees_east' or 'degrees'.");
                                continue;
                            }
                            

                            //things are looking good, get global attributes
                            globalAttributes = new Attributes();
                            NcHelper.getGroupAttributes(ncFile.getRootGroup(), globalAttributes);
                            courtesy = globalAttributes.getString("contributor_name");
                            if (courtesy == null)
                                courtesy = globalAttributes.getString("acknowledgement"); //ACDD
                            if (courtesy == null)
                                courtesy = globalAttributes.getString("creator_name");
                            if (courtesy == null) courtesy = "";

                        } else {
                            //ensure 4D variable has correct coordinates
                            String tName = variable.getDimension(0).getName();
                            if (!tName.equals(timeNameInFile)) {
                                if (verbose) String2.log(reject + "dim0 name=" + 
                                    tName + " isn't " + timeNameInFile + ".");
                                continue;
                            }
                            tName = variable.getDimension(1).getName();
                            if (!tName.equals(depthNameInFile)) {
                                if (verbose) String2.log(reject + "dim1 name=" + 
                                    tName + " isn't " + depthNameInFile + ".");
                                continue;
                            }
                            tName = variable.getDimension(2).getName();
                            if (!tName.equals(latNameInFile)) {
                                if (verbose) String2.log(reject + "dim2 name=" + 
                                    tName + " isn't " + latNameInFile + ".");
                                continue;
                            }
                            tName = variable.getDimension(3).getName();
                            if (!tName.equals(lonNameInFile)) {
                                if (verbose) String2.log(reject + "dim3 name=" + 
                                    tName + " isn't " + lonNameInFile + ".");
                                continue;
                            }
                        }


                        //ok. add the variable and it's attributes
                        activeDataVariableNames.add(String2.canonical(name));
                        Attributes tAttributes = new Attributes();
                        NcHelper.getVariableAttributes(variable, tAttributes);
                        dataAttributes.add(tAttributes);
                        dataElementType.add(NcHelper.getElementPAType(variable));
                    }

                    //ok, save this fileName
                    //if (verbose) String2.log("accept " + tFileNames[f]);                                       
                    tActiveFileNames.add(tFileNames[f]);
                    tActiveIndividuals.add( String2.replaceAll(
                        File2.getNameNoExtension(tFileNames[f]), '_', ' '));
                } catch (Exception e) {
                    if (verbose) String2.log("  rejecting " + tFileNames[f] + 
                        ": " + e.toString()); //no need for stack trace
                } finally {
                    try {if (ncFile != null) ncFile.close(); } catch (Exception e9) {}
                }
            } catch (Exception e) {
                if (verbose) String2.log("  rejecting " + tFileNames[f] + ": " + e.toString()); //no need for stack trace
            }
        }

        //keep this group?
        if (verbose) String2.log("  nActiveFiles=" + tActiveFileNames.size());
        Test.ensureTrue(tActiveFileNames.size() > 0, errorInMethod + "No valid files found.");
        fileNames = tActiveFileNames.toArray();
        individuals = tActiveIndividuals.toArray();

        //store the dataVariable info in permanent data structures
        //dataAttributes already set
        dataVariableNames = activeDataVariableNames.toArray();
        extractDataVariableInfo();

        //last thing: ensureValid
        ensureValid();
        if (verbose) String2.log(toString() + "\n  TableDataSet4DNc constructor done. TIME=" + 
            (System.currentTimeMillis() - time) + "\n");
    }


    /**
     * Make a Table with a specific subset of the data.
     * See the superclass javadocs for details.
     */
    public Table makeSubset(String isoMinTime, String isoMaxTime, 
        String desiredIndividuals[], String[] desiredDataVariableNames) throws Exception {

        if (verbose) String2.log("TableDataSet4DNc.makeSubset " + internalName);
        long time = System.currentTimeMillis();

        //makeEmptyTable
        Table table = makeEmptyTable(desiredDataVariableNames);

        //add the data  
        //storedTime is source time
        if (isoMinTime == null || isoMinTime.length() == 0) isoMinTime = "0001";
        if (isoMaxTime == null || isoMaxTime.length() == 0) isoMaxTime = "3000";
        double minEpochSeconds = Calendar2.isoStringToEpochSeconds(isoMinTime);
        double maxEpochSeconds = Calendar2.isoStringToEpochSeconds(isoMaxTime);
        double minTimeInFile = Calendar2.epochSecondsToUnitsSince(timeBaseSeconds, 
            timeFactorToGetSeconds, minEpochSeconds);
        double maxTimeInFile = Calendar2.epochSecondsToUnitsSince(timeBaseSeconds, 
            timeFactorToGetSeconds, maxEpochSeconds);
        for (int indi = 0; indi < desiredIndividuals.length; indi++) {
            int whichIndividual = whichIndividual(desiredIndividuals[indi]); //throws Exception if not found

            //open the file
            NetcdfFile ncFile = NcHelper.openFile(directory + fileNames[whichIndividual]);
            try {

                //find valid time indices  (the time variable must exist)
                Variable timeVariable = ncFile.findVariable(timeNameInFile);
                int startTimeIndex = NcHelper.binaryFindClosest(timeVariable, minTimeInFile);
                int endTimeIndex = NcHelper.binaryFindClosest(timeVariable, maxTimeInFile);

                //if just getting one time point and >=1 hour away, forget it
                double endEpSecInFile = Calendar2.unitsSinceToEpochSeconds(timeBaseSeconds, 
                    timeFactorToGetSeconds, NcHelper.getDouble(timeVariable, endTimeIndex));
                if (startTimeIndex == endTimeIndex &&
                    Math.abs(endEpSecInFile - maxEpochSeconds) >= Calendar2.SECONDS_PER_HOUR) {
                    continue; //ncFile closed in finally {} below
                }

                //get all the x, y, z, t values
                DoubleArray tLonArray = NcHelper.getNiceDoubleArray(
                    ncFile.findVariable(lonNameInFile), 0, -1);
                DoubleArray tLatArray = NcHelper.getNiceDoubleArray(
                    ncFile.findVariable(latNameInFile), 0, -1);
                DoubleArray tDepthArray = NcHelper.getNiceDoubleArray(
                    ncFile.findVariable(depthNameInFile), 0, -1);
                DoubleArray tTimeArray = NcHelper.getNiceDoubleArray( 
                    timeVariable, startTimeIndex, endTimeIndex);

                //convert altitude to depth 
                if (zIsUp) tDepthArray.scaleAddOffset(-1, 0); 

                //convert time values in file to epochSeconds
                int nTime = tTimeArray.size();
                for (int timei = 0; timei < nTime; timei++)
                    tTimeArray.set(timei,
                        Calendar2.unitsSinceToEpochSeconds(timeBaseSeconds, 
                            timeFactorToGetSeconds, tTimeArray.get(timei)));

                //add capacity to columns of table
                int nt = tTimeArray.size(); 
                int nz = tDepthArray.size(); 
                int ny = tLatArray.size(); 
                int nx = tLonArray.size(); 
                int baseRow = table.nRows();
                long nxyzt = nx * (long)ny * nz * nt;
                for (int v = 0; v < table.nColumns(); v++)
                    table.getColumn(v).ensureCapacity(baseRow + nxyzt);
                
                //add the x,y,z,t,id values
                //!!!ORDER OF t,z,y,x LOOPS MATCHES column.append BELOW
                for (int t = 0; t < nt; t++) { //t first, so file sorted by t
                    for (int z = 0; z < nz; z++) {
                        for (int y = 0; y < ny; y++) {
                            for (int x = 0; x < nx; x++) {
                                table.getColumn(0).addDouble(tLonArray.array[x]);
                                table.getColumn(1).addDouble(tLatArray.array[y]);
                                table.getColumn(2).addDouble(tDepthArray.array[z]);
                                table.getColumn(3).addDouble(tTimeArray.array[t]);
                                table.getColumn(4).addString(desiredIndividuals[indi]);
                            }
                        }
                    }
                }                              

                //add the data values
                for (int v = 0; v < desiredDataVariableNames.length; v++) {
                    Variable tVariable = ncFile.findVariable(desiredDataVariableNames[v]);
                    PrimitiveArray column = table.getColumn(5 + v);

                    if (tVariable == null) {
                        //the variable not in this file; just add nxyzt NaNs
                        for (int xyzt = 0; xyzt < nxyzt; xyzt++)
                            column.addDouble(Double.NaN);
                    } else {
                        //get the data and add it
                        //!!!ORDER of get4DValues MATCHES t,z,y,x LOOPS ABOVE
                        column.append(NcHelper.get4DValues(tVariable, 
                            0, nx, 0, ny, 0, nz, startTimeIndex, nt));
                    }
                }
            } finally {
                try {if (ncFile != null) ncFile.close(); } catch (Exception e9) {}
            }
        }

        //clean up the dataVariables
        cleanUpDataVariablesData(table, desiredDataVariableNames);

        //setAttributes 
        setAttributes(table);

        //return the results
        if (verbose) String2.log("TableDataSet4DNc.makeSubset " + internalName + 
            " done. nRows=" + table.nRows() + 
            " TIME=" + (System.currentTimeMillis() - time));
        return table;
    }

}
