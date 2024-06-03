/* 
 * TwoGrids Copyright 2006, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.*;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

//import gov.noaa.pfel.coastwatch.hdf.HdfConstants;
//import gov.noaa.pfel.coastwatch.hdf.HdfScientificData;
//import gov.noaa.pfel.coastwatch.hdf.SdsReader;
//import gov.noaa.pfel.coastwatch.hdf.SdsWriter;
import gov.noaa.pfel.coastwatch.util.DataStream;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.GregorianCalendar;

// from netcdfAll-x.jar
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.geotiff.GeotiffWriter;
import ucar.nc2.util.*;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * This class has static methods which take two similar grids
 * (e.g., zonal and meridional data) and store the data in one file.
 *
 * <p>My reading of the ESRI .asc format 
 * (https://en.wikipedia.org/wiki/Esri_grid) is that it is only
 * for one grid of data (not two). So that format is not supported here.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-10-23
 *
 */
public class TwoGrids  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;



    /**
     * This saves the data in a Grid as an HDF file with 
     *     whatever attributes are currently set (use GridDataSet.setAttributes)
     * This method is self-contained; 
     * it does not require external lookup tables with data,
     * but instead uses fileNameUtility to get information from the .grd file name.
     *
     * <p> The data is stored in the hdf file as doubles.
     *  Missing values are stored as DataHelper.FAKE_MISSING_VALUE, not NaN.
     *
     * <p>This calls setStatsAttributes(true).
     *
     * <p>This relies on a pure Java implemtation, not NCSA's libraries.
     * See comments in hdf/SdsWriter.java for reasoning.
     *
     * <p>See documentation that comes with CoastWatch Utilities
     * e.g., c:/programs/cwutilities/doc/Metadata.html.
     *
     * <p>This method is vastly faster (9 s web page time for SST 1km) 
     * than Dave's script (69 s).
     * And I think the Matlab approach is odd/flawed because it makes
     * 2D lat and lon arrays, which appear as datasets in CDAT.
     * 
     * <p>The grid needs to hold gridded data with longitude +-180.
     *     (If not +-180, the data won't be in the right place for 
     *     CDAT to display coastline and political lines.)
     *     The full range and resolution of the grid data will be saved in the hdf file.
     *
     * @param hdfFileName the full name for the new hdf file, but without the 
     *    .hdf extension (it will be added by this method).
     *    Because fileNameUtility is used to generate the metadata based
     *    on the file name, the file name must follow the CWBrowser file
     *    naming convention (see FileNameUtility).    
     * @param fileNameUtility is used to generate all of the metadata based on the file name
     * @throws Exception if trouble. If there is trouble, there should be no 
     *    partially created .hdf file.
     */
    /* NOT YET DONE
    public void saveAsHDF(String hdfFileName, FileNameUtility fileNameUtility) throws Exception {

        File2.delete(hdfFileName + ".hdf");

        //make sure there is data
        ensureThereIsData();

        try {
            if (verbose) String2.log("Grid.saveAsHDF " + hdfFileName + "\n" + Math2.memoryString());
            String errorIn = String2.ERROR + " in Grid.saveAsHDF: ";

            //gather the data
            //A new array is needed because of different order and different type.
            int nLon = lon.length;
            int nLat = lat.length;
            int nData = data.length;
            double tData[] = new double[nData];
            int po = 0;
            for (int tLat = 0; tLat < nLat; tLat++) {
                for (int tLon = 0; tLon < nLon; tLon++) {
                    double d = getData(tLon, nLat-1 - tLat); //up side down
                    tData[po++] = Double.isNaN(d)? DataHelper.FAKE_MISSING_VALUE : d;
                }
            }
  
            //set the attributes
            setStatsAttributes(true);  //save as double

            String name = File2.getNameAndExtension(hdfFileName);

            //create the file
            SdsWriter.create(hdfFileName + ".hdf", //must be the correct name, since it is stored in the file
                new double[][]{lon, lat}, //order is important and tied to desired order of data in array
                new String[]{"Longitude", "Latitude"},
                new Attributes[]{lonAttributes, latAttributes},
                tData,                
                FileNameUtility.get6CharName(name),
                dataAttributes,
                globalAttributes);
        } catch (Exception e) {
            File2.delete(hdfFileName + ".hdf");
            throw e;
        }
        
    } */

    /**
     * This is a unit test for saveAsHDF and readHDF; it makes .../coastwatch/griddata/<testName>Test.hdf.
     *
     * @param fileNameUtility is used to generate all of the metadata based on the file name
     * @throws Exception if trouble
     */
/* NOT YET DONE
    public static void testHDF(FileNameUtility fileNameUtility) throws Exception {
        String2.log("\n*** Grid.testHDF");

        //load the data
        Grid grid1 = new Grid();
        grid1.readGrd(testDir + testName + ".grd", 
            true); //+-180; put longitude into Atlanticentric reference

        //set some attributes
        grid1.globalAttributes().set("title", "Wind, QuikSCAT Seawinds, Composite, Zonal");
        grid1.latAttributes().set("units", FileNameUtility.getLatUnits());
        grid1.lonAttributes().set("units", FileNameUtility.getLonUnits());
        grid1.dataAttributes().set("units", "m s-1");

        //save the data
        grid1.saveAsHDF(testDir + testName + "Test", fileNameUtility);
         
        //now try to read it
        Grid grid2 = new Grid();
        grid2.readHDF(testDir + testName + "Test.hdf", 
            HdfConstants.DFNT_FLOAT64);
        //makeLonPM180(true);

        //look for differences
        Test.ensureTrue(grid1.equals(grid2), "testHDF");

        //attributes
        Test.ensureEqual(grid1.globalAttributes().getString("title"), "Wind, QuikSCAT Seawinds, Composite, Zonal", "");
        Test.ensureEqual(grid1.latAttributes().getString("units"), FileNameUtility.getLatUnits(), "");
        Test.ensureEqual(grid1.lonAttributes().getString("units"), FileNameUtility.getLonUnits(), "");
        Test.ensureEqual(grid1.dataAttributes().getString("units"), "m s-1", "");

        //delete the test file
        Test.ensureTrue(File2.delete(testDir + testName + "Test.hdf"), 
            String2.ERROR + " in Grid.testHDF: unable to delete " + 
            testDir + testName + "Test.hdf");
    }
*/

    /**
     * Save this grid data as a Matlab .mat file.
     * This writes the lon values as they are currently in this grid
     *    (e.g., +-180 or 0..360).
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * 
     * @param grid1 a Grid with the same lat and lon values as grid2
     * @param grid2 a Grid with the same lat and lon values as grid1
     * @param directory with a slash at the end
     * @param name The file name with out the extension (e.g., myFile).
     *    The extension ".mat" will be added.
     * @param varName1 the name to use for the variable in grid1 (e.g., QNux10).
     *    If it isn't variableNameSafe, it will be made so.
     * @param varName2 the name to use for the variable in grid2 (e.g., QNuy10).
     *    If it isn't variableNameSafe, it will be made so.
     * @throws Exception 
     */
    public static void saveAsMatlab(Grid grid1, Grid grid2, String directory, 
        String name, String varName1, String varName2) throws Exception {

        String errorInMethod = String2.ERROR + " in TwoGrids.saveAsMatlab:\n";
        varName1 = String2.modifyToBeVariableNameSafe(varName1);
        varName2 = String2.modifyToBeVariableNameSafe(varName2);

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //delete any existing file
        String ext = Grid.SAVE_AS_EXTENSIONS[Grid.SAVE_AS_MATLAB];
        File2.delete(directory + name + ext);

        //make sure there is data
        grid1.ensureThereIsData();
        grid2.ensureThereIsData();

        //ensure lat and lon are the same 
        Test.ensureEqual(grid1.lat, grid2.lat, errorInMethod + "The lat values of the two grids aren't identical.");
        Test.ensureEqual(grid1.lon, grid2.lon, errorInMethod + "The lon values of the two grids aren't identical.");

        //open a dataOutputStream 
        DataOutputStream dos = DataStream.getDataOutputStream(directory + randomInt);
        try {
            //write the header
            Matlab.writeMatlabHeader(dos);

            //first: write the lon array 
            Matlab.writeDoubleArray(dos, "lon", grid1.lon);

            //second: make the lat array
            Matlab.writeDoubleArray(dos, "lat", grid1.lat);

            //make an array of the data[row][col]
            int nLat = grid1.lat.length;
            int nLon = grid1.lon.length;
            double ar[][] = new double[nLat][nLon];
            for (int row = 0; row < nLat; row++)
                for (int col = 0; col < nLon; col++) 
                    ar[row][col] = grid1.getData(col, row);
            Matlab.write2DDoubleArray(dos, varName1, ar);

            for (int row = 0; row < nLat; row++)
                for (int col = 0; col < nLon; col++) 
                    ar[row][col] = grid2.getData(col, row);
            Matlab.write2DDoubleArray(dos, varName2, ar);

            //this doesn't write attributes.   should it?
            //setStatsAttributes(true); //true = double
            //write the attributes...

        } finally { 
            dos.close();
        }

        //rename the file to the specified name     
        File2.rename(directory, randomInt + "", name + ext);

    }

    /**
     * Save this grid data as a 4D netCDF .nc file using the currently
     * available globalAttributes, latAttributes, lonAttributes,
     * and dataAttributes.
     * This writes the lon values as they are currently in this grid
     *    (e.g., +-180 or 0..360).
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * The lat, lon, altitude, and data values are written as floats (see comments
     *   for DataHelper.FAKE_MISSING_VALUE).
     * The time data values are written as doubles (seconds since 1970-01-01T00:00:00Z).
     * The lat variable will always be named "lat".
     * The lon variable will always be named "lon".
     * The altitude variable will always be named "altitude".
     * The time variable will always be named "time".
     * The time value will be the centered value, calculated from the ISO 8601 string 
     * in the global attributes "time_coverage_start" and  "time_coverage_end".
     * 
     * @param grid1 a Grid with the same lat and lon values as grid2
     * @param grid2 a Grid with the same lat and lon values as grid1
     * @param directory with a slash at the end
     * @param name The file name with out the extension (e.g., myFile).
     *    The extension ".nc" will be added.
     *    The name does not have to be a CWBrowser-style name.
     * @param dataName1 The name for the data variable for grid1 (e.g., QNux10).
     * @param dataName2 The name for the data variable for grid1 (e.g., QNuy10).
     * @throws Exception 
     */
    public static void saveAsNetCDF(Grid grid1, Grid grid2, 
        String directory, String name, String dataName1, String dataName2) throws Exception {

        String errorInMethod = String2.ERROR + " in TwoGrids.saveAsNetCDF:\n";
        long time = System.currentTimeMillis();

        //delete any existing file
        String ext = Grid.SAVE_AS_EXTENSIONS[Grid.SAVE_AS_NETCDF];
        File2.delete(directory + name + ext);

        //make sure there is data
        grid1.ensureThereIsData();
        grid2.ensureThereIsData();

        //ensure lat and lon are the same 
        Test.ensureEqual(grid1.lat, grid2.lat, errorInMethod + "The lat values of the two grids aren't identical.");
        Test.ensureEqual(grid1.lon, grid2.lon, errorInMethod + "The lon values of the two grids aren't identical.");

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //write the data
        //items determined by looking at a .nc file; items written in that order 
        String2.log("tFileName=" + directory + randomInt + ".nc");
        //createNew( , false) says: create a new file and don't fill with missing_values
        boolean nc3Mode = true;
        NetcdfFormatWriter ncWriter = null;
        try {
            NetcdfFormatWriter.Builder nc = NetcdfFormatWriter.createNewNetcdf3(
                directory + randomInt + ".nc");
            Group.Builder rootGroup = nc.getRootGroup();
            nc.setFill(false);

            int nLat = grid1.lat.length;
            int nLon = grid1.lon.length;

            //define the dimensions
            Dimension timeDimension     = NcHelper.addDimension(rootGroup, "time", 1);
            Dimension altitudeDimension = NcHelper.addDimension(rootGroup, "altitude", 1);
            Dimension latDimension      = NcHelper.addDimension(rootGroup, "lat", nLat);
            Dimension lonDimension      = NcHelper.addDimension(rootGroup, "lon", nLon);

            //create the variables (and gather the data) 
            ArrayDouble.D1 tTime = new ArrayDouble.D1(1);
            String startString  = grid1.globalAttributes().getString("time_coverage_start");
            String endString    = grid1.globalAttributes().getString("time_coverage_end");
            double startSeconds = Calendar2.isoStringToEpochSeconds(startString); //throws exception if trouble
            double endSeconds   = Calendar2.isoStringToEpochSeconds(endString);   //throws exception if trouble
            double centerSeconds = (startSeconds + endSeconds) / 2;
            tTime.set(0, centerSeconds);
            Variable.Builder timeVar = NcHelper.addVariable(rootGroup, "time", DataType.DOUBLE, 
                Arrays.asList(timeDimension)); 
            
            ArrayDouble.D1 tAltitude = new ArrayDouble.D1(1);
            tAltitude.set(0, 0);
            Variable.Builder altitudeVar = NcHelper.addVariable(rootGroup, "altitude", DataType.DOUBLE, 
                Arrays.asList(altitudeDimension)); 
            
            ArrayDouble.D1 tLat = new ArrayDouble.D1(nLat);
            for (int i = 0; i < nLat; i++)
                tLat.set(i, grid1.lat[i]);
            Variable.Builder latVar = NcHelper.addVariable(rootGroup, "lat", DataType.DOUBLE, 
                Arrays.asList(latDimension)); 

            ArrayDouble.D1 tLon = new ArrayDouble.D1(nLon);
            for (int i = 0; i < nLon; i++)
                tLon.set(i, grid1.lon[i]);
            Variable.Builder lonVar = NcHelper.addVariable(rootGroup, "lon", DataType.DOUBLE, 
                Arrays.asList(lonDimension)); 

            Variable.Builder data1Var = NcHelper.addVariable(rootGroup, dataName1, DataType.FLOAT, 
                Arrays.asList(timeDimension, altitudeDimension, latDimension, lonDimension)); 
            Variable.Builder data2Var = NcHelper.addVariable(rootGroup, dataName2, DataType.FLOAT, 
                Arrays.asList(timeDimension, altitudeDimension, latDimension, lonDimension)); 

            //setStatsAttributes
            grid1.setStatsAttributes(false); //false -> save data as floats
            grid2.setStatsAttributes(false); //false -> save data as floats

            //write Attributes
            String names[] = grid1.globalAttributes().getNames();
            for (int i = 0; i < names.length; i++) { 
                //et_affine needs to be modified, since .nc has data right-side-up
                //I suspect CDAT georeferences this correctly but with coast and data upside down. 
                if (names[i].equals("et_affine")) {
                    // lon = a*row + c*col + e
                    // lat = b*row + d*col + f
                    double matrix[] = {0, grid1.latSpacing, grid1.lonSpacing, 
                                       0, grid1.lon[0],     grid1.lat[0]}; //right side up
                    rootGroup.addAttribute(NcHelper.newAttribute("et_affine", 
                        NcHelper.get1DArray(matrix, false))); //float64[] {a, b, c, d, e, f}
                } else {
                    rootGroup.addAttribute(NcHelper.newAttribute(true, //nc3Mode
                        names[i], grid1.globalAttributes().get(names[i])));
                }
            }

            //time attributes
            timeVar.addAttribute(NcHelper.newAttribute("actual_range", NcHelper.get1DArray(new double[]{centerSeconds, centerSeconds}, false)));     
            timeVar.addAttribute(new Attribute("axis",                 "T"));
            timeVar.addAttribute(new Attribute("fraction_digits",      Integer.valueOf(0)));     
            timeVar.addAttribute(new Attribute("long_name",            "Centered Time"));
            timeVar.addAttribute(new Attribute("standard_name",        "time"));
            timeVar.addAttribute(new Attribute("units",                Calendar2.SECONDS_SINCE_1970));
            timeVar.addAttribute(new Attribute("_CoordinateAxisType",  "Time"));

            //altitude attributes
            altitudeVar.addAttribute(NcHelper.newAttribute("actual_range",   NcHelper.get1DArray(new double[]{0, 0}, false)));     
            altitudeVar.addAttribute(new Attribute("axis",                   "Z"));
            altitudeVar.addAttribute(new Attribute("fraction_digits",        Integer.valueOf(0)));     
            altitudeVar.addAttribute(new Attribute("long_name",              "Altitude"));
            altitudeVar.addAttribute(new Attribute("positive",               "up"));
            altitudeVar.addAttribute(new Attribute("standard_name",          "altitude"));
            altitudeVar.addAttribute(new Attribute("units",                  "m"));
            altitudeVar.addAttribute(new Attribute("_CoordinateAxisType",    "Height"));
            altitudeVar.addAttribute(new Attribute("_CoordinateZisPositive", "up"));

            //lat
            NcHelper.setAttributes(nc3Mode, latVar, grid1.latAttributes(), false); //isUnsigned(DOUBLE)
            latVar.addAttribute(new Attribute("axis", "Y"));

            //lon
            NcHelper.setAttributes(nc3Mode, lonVar, grid1.lonAttributes(), false); //isUnsigned(DOUBLE)
            lonVar.addAttribute(new Attribute("axis", "X"));

            //data1
            NcHelper.setAttributes(nc3Mode, data1Var, grid1.dataAttributes(), false); //isUnsigned(FLOAT)

            //data2
            NcHelper.setAttributes(nc3Mode, data2Var, grid2.dataAttributes(), false); //isUnsigned(FLOAT)

            //leave "define" mode
            ncWriter = nc.build();

            //then add data
            ncWriter.write(timeVar.getFullName(),     tTime);
            ncWriter.write(altitudeVar.getFullName(), tAltitude);
            ncWriter.write(latVar.getFullName(),      tLat);
            ncWriter.write(lonVar.getFullName(),      tLon);

            //write grid1 values to ArrayFloat.D4
            ArrayFloat.D4 tGrid = new ArrayFloat.D4(1, 1, nLat, nLon);
            for (int iLat = 0; iLat < nLat; iLat++) {
                for (int iLon = 0; iLon < nLon; iLon++) {
                    float tData = (float)grid1.getData(iLon, iLat); //for cdat, was nLat-1 - iLat); //up side down
                    tGrid.set(0, 0, iLat, iLon, Float.isNaN(tData)? (float)DataHelper.FAKE_MISSING_VALUE : tData);
                }
            }
            ncWriter.write(data1Var.getFullName(), tGrid);

            //write grid2 values to ArrayFloat.D4
            for (int iLat = 0; iLat < nLat; iLat++) {
                for (int iLon = 0; iLon < nLon; iLon++) {
                    float tData = (float)grid2.getData(iLon, iLat); //for cdat, was nLat-1 - iLat); //up side down
                    tGrid.set(0, 0, iLat, iLon, Float.isNaN(tData)? (float)DataHelper.FAKE_MISSING_VALUE : tData);
                }
            }
            ncWriter.write(data2Var.getFullName(), tGrid);

            //if close throws exception, it is trouble
            ncWriter.close(); //it calls flush() and doesn't like flush called separately
            ncWriter = null;

            //rename the file to the specified name
            File2.rename(directory, randomInt + ".nc", name + ext);

            //diagnostic
            if (verbose)
                String2.log("TwoGrids.saveAsNetCDF done. created " + 
                    directory + name + ext + 
                    " in " + (System.currentTimeMillis() - time) + " ms.");
            //String2.log(ncdump(directory + name + ext, "-h"));

        } catch (Exception e) {
            String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
            if (ncWriter != null) {
                try {ncWriter.abort(); } catch (Exception e9) {}
                File2.delete(directory + randomInt + ".nc"); 
                ncWriter = null;
            }

            throw e;
        }

    }

    /**
     * Save this grid data as a tab-separated XYZ ASCII file.
     * This writes the lon values as they are currently in the grids
     *    (e.g., +-180 or 0..360). Note the GMT seems to want the values as 0..360.
     * This overwrites any existing file of the specified name.
     * This makes an effort not to create a partial file if there is an error.
     * If no exception is thrown, the file was successfully created.
     * **Currently, the lat, lon, and data values are written as floats.
     * 
     * @param grid1 a Grid with the same lat and lon values as grid2
     * @param grid2 a Grid with the same lat and lon values as grid1
     * @param directory with a slash at the end
     * @param name The file name with out the extension (e.g., myFile).
     *    The extension ".xyz" will be added.
     * @param NaNString is the String to write for NaN's. 
     * @throws Exception 
     */
    public static void saveAsXyz(Grid grid1, Grid grid2, String directory, String name, 
            String NaNString) throws Exception {

        String errorInMethod = String2.ERROR + " in TwoGrids.saveAsMatlab:\n";

        //delete any existing file
        String ext = Grid.SAVE_AS_EXTENSIONS[Grid.SAVE_AS_XYZ];
        File2.delete(directory + name);

        //make sure there is data
        grid1.ensureThereIsData();
        grid2.ensureThereIsData();

        //ensure lat and lon are the same 
        Test.ensureEqual(grid1.lat, grid2.lat, errorInMethod + "The lat values of the two grids aren't identical.");
        Test.ensureEqual(grid1.lon, grid2.lon, errorInMethod + "The lon values of the two grids aren't identical.");

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the temp file
        //(I tried with Buffer/FileOutputStream. No faster.)
        BufferedWriter bufferedWriter = File2.getBufferedFileWriter88591(directory + randomInt);
        try {

            //write the data
            int nLat = grid1.lat.length;
            int nLon = grid1.lon.length;
            //write values from row to row, bottom to top 
            for (int tLat = 0; tLat < nLat; tLat++) {
                for (int tLon = 0; tLon < nLon; tLon++) {
                    float f1 = (float)grid1.getData(tLon, tLat);
                    float f2 = (float)grid2.getData(tLon, tLat);
                    bufferedWriter.write(
                        String2.genEFormat10(grid1.lon[tLon]) + "\t" + 
                        String2.genEFormat10(grid1.lat[tLat]) + "\t" + 
                        (Float.isNaN(f1)? NaNString + '\t': f1 + "\t") +
                        (Float.isNaN(f2)? NaNString + '\n': f2 + "\n"));
                }
            }
        } finally {
            bufferedWriter.close();
        }

        //rename the file to the specified name
        File2.rename(directory, randomInt + "", name + ext);

        //diagnostic
        if (false) {
            String[] rff = File2.readFromFile88591(directory + name + ext);
            if (rff[0].length() > 0)
                throw new Exception(String2.ERROR + ":\n" + rff[0]);
            String2.log("grid.saveAsXYZ: " + directory + name + ext + " contains:\n" +
                String2.annotatedString(
                    rff[1].substring(0, Math.min(rff[1].length(), 200))));
        }
    }

    /**
     * This saves the current grid in some type of file using information
     * from the file name (must be CWBrowser style, see FileNameUtility)
     * and the fileNameUtility object).
     * If the file already exists, it is touched, and nothing else is done.
     * This does not add attributes (other than calling addStatsAttributes ()).
     *
     * @param directory the directory for the resulting file (with a slash at the end)
     * @param fileName the name for the resulting file (without any extension)
     * @param fileNameUtility which is used to setAttributes for some of the
     *    file types based on the fileName
     * @param saveAsType one of the SAVE_AS constants
     * @param zipIt If true, creates a .zip file and deletes the
     *    intermediate file (e.g., .asc). If false, the specified
     *    saveAsType is created.
     * @throws Exception if trouble
     */
/* NOT YET 
    public void saveAs(String directory, String fileName, 
            FileNameUtility fileNameUtility, int saveAsType,
            boolean zipIt) throws Exception {

        if (verbose) String2.log("Grid.saveAs(name=" + fileName + " type=" + saveAsType + ")"); 
        if (saveAsType != SAVE_AS_ASCII &&
            saveAsType != SAVE_AS_GEOTIFF &&
            saveAsType != SAVE_AS_GRD &&
            saveAsType != SAVE_AS_HDF &&
            saveAsType != SAVE_AS_MATLAB &&
            saveAsType != SAVE_AS_NETCDF &&
            saveAsType != SAVE_AS_XYZ)
            throw new RuntimeException(String2.ERROR + " in Grid.saveAs: invalid saveAsType=" + saveAsType);
       
        String ext = SAVE_AS_EXTENSIONS[saveAsType];

        //does the file already exist?
        String finalName = directory + fileName + ext + (zipIt? ".zip" : "");
        if (File2.touch(finalName)) { 
            if (verbose) String2.log("Grid.saveAs reusing " + finalName);
            return;
        }
     
        //save as ...
        long time = System.currentTimeMillis();
        if      (saveAsType == SAVE_AS_ASCII)   saveAsASCII(  directory, fileName);
        else if (saveAsType == SAVE_AS_GEOTIFF) 
            saveAsGeotiff(directory, fileName, FileNameUtility.get6CharName(fileName));
        else if (saveAsType == SAVE_AS_GRD)     saveAsGrd(    directory, fileName);
        else if (saveAsType == SAVE_AS_HDF)     saveAsHDF(    directory + fileName, fileNameUtility);
        else if (saveAsType == SAVE_AS_MATLAB)  saveAsMatlab( directory, fileName, 
            FileNameUtility.get6CharName(fileName));
        else if (saveAsType == SAVE_AS_NETCDF)  {
            saveAsNetCDF(directory, fileName, FileNameUtility.get6CharName(fileName));
        } else if (saveAsType == SAVE_AS_XYZ)   saveAsXYZ(    directory, fileName);

        if (zipIt) {
            //zip to a temporary zip file, -j: don't include dir info
            SSR.zip(         directory + fileName + ext + ".temp.zip",
                new String[]{directory + fileName + ext}, 20); 

            //delete the file that was zipped
            File2.delete(directory + fileName + ext); 

            //if all successful, rename to final name
            File2.rename(directory, fileName + ext + ".temp.zip", fileName + ext + ".zip");
        }
    }
*/
}
