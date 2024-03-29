/* 
 * SaveOpendap Copyright 2005, NOAA.
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

//import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// from netcdfAll-x.jar
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.nc2.write.NetcdfFormatWriter;

/** The Java DAP classes.  */
//import dods.dap.*;


/**
 * This class has some static methods to read opendap data sources and
 * save the data in different types of files.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2006-08-15
 */
public class SaveOpendap  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * Save tries to save all the variables (attributes and data) from
     * an opendap source to an nc file.
     * Currently, it doesn't work with structures.
     * 
     * @param url the opendap url
     * @param fullName The full file name (dir + name + ext (usually .nc))
     * @throws Exception 
     */
    public static void asNc(String url, String fullName) throws Exception {
        long time = System.currentTimeMillis();
        if (verbose) String2.log("SaveOpendap.asNc" +
            "\n  url=" + url +
            "\n  fullName=" + fullName);


        //delete any existing file
        File2.delete(fullName);

        //POLICY: because this procedure may be used in more than one thread,
        //do work on unique temp files names using randomInt, then rename to proper file name.
        //If procedure fails half way through, there won't be a half-finished file.
        int randomInt = Math2.random(Integer.MAX_VALUE);

        //open the file (before 'try'); if it fails, no temp file to delete
        //DODSNetcdfFile in = new DODSNetcdfFile(url); //bug? showed no global attributes //but fixed in 2.2.16
        //NetcdfFile in = NetcdfFiles.open(url);  //this fails: The server does not support byte ranges.
        NetcdfDataset in = NetcdfDatasets.openDataset(url);     //2021: 's' is new API
        try {

            NetcdfFormatWriter ncWriter = null;
            
            try {
                NetcdfFormatWriter.Builder out = NetcdfFormatWriter.createNewNetcdf3(
                    fullName + randomInt);
                Group.Builder outRootGroup = out.getRootGroup();
                out.setFill(false);

                //get the list of dimensions
                Group inRootGroup = in.getRootGroup();
                List globalDimList = inRootGroup.getDimensions();

                //add the dimensions to 'out'
                int nGlobalDim = globalDimList.size();
                if (verbose) String2.log("  nGlobalDim=" + nGlobalDim);
                for (int i = 0; i < nGlobalDim; i++) {
                    Dimension dim = (Dimension)globalDimList.get(i);
                    String dimName = dim.getName();
                    Dimension tDim = NcHelper.addDimension(outRootGroup, dimName, dim.getLength(), 
                        true, dim.isUnlimited(), false);  //isShared, ..., isVariableLength
                    if (verbose) String2.log("    dimName" + i + "=" + dimName);
                }

                //get the list of opendap variables
                List inVarList = in.getVariables();

                //add the variables to .nc
                int nInVars = inVarList.size();
                if (verbose) String2.log("  nInVars=" + nInVars);
                Variable.Builder newVars[] = new Variable.Builder[nInVars];
                for (int v = 0; v < nInVars; v++) { 
                    //get the variable
                    Variable inVar = (Variable)inVarList.get(v);
                    String varName = inVar.getFullName();
                    if (verbose) String2.log("    varName" + v + "=" + varName);

                    //get nDimensions
                    List tDimList = inVar.getDimensions();
                    int nTDim = tDimList.size();

                    //create dimension[] 
                    ArrayList<Dimension> dimList = new ArrayList();
                    for (int d = 0; d < nTDim; d++) 
                        dimList.add((Dimension)tDimList.get(d));

                    //add the variable to 'out'
                    newVars[v] = NcHelper.addVariable(outRootGroup, varName, 
                        inVar.getDataType(), dimList); 

                    //write Attributes   (after adding variables)
                    AttributeContainer attList = inVar.attributes();
                    Iterator it = attList.iterator();
                    while (it.hasNext())
                        newVars[v].addAttribute((ucar.nc2.Attribute)it.next());
                }

                //global attributes
                List globalAttList = in.getGlobalAttributes();
                int nGlobalAtt = globalAttList.size();
                if (verbose) String2.log("  nGlobalAtt=" + nGlobalAtt);
                for (int a = 0; a < nGlobalAtt; a++) {
                    Attribute att = (Attribute)globalAttList.get(a);
                    if (verbose) String2.log("  globalAtt name=" + att.getName());
                    outRootGroup.addAttribute(att);
                }

                //leave "define" mode
                if (verbose) String2.log("  leaving 'define' mode");
                ncWriter = out.build();

                //write the data
                for (int v = 0; v < nInVars; v++) {
                    Variable inVar = (Variable)inVarList.get(v);
                    if (verbose) String2.log("  writing data inVar=" + inVar.getFullName());

                    //write the data
                    ncWriter.write(newVars[v].getFullName(), inVar.read());
                }

                //I care about this exception
                ncWriter.close();
                ncWriter = null;

            } catch (Exception e) {
                String2.log(NcHelper.ERROR_WHILE_CREATING_NC_FILE + MustBe.throwableToString(e));
                if (ncWriter != null) {
                    try {ncWriter.abort(); } catch (Exception e9) {}
                    File2.delete(fullName + randomInt); 
                    ncWriter = null;
                }
                throw e;
            }

            //I care about this exception
            in.close();
            in = null;
 
            //rename the file to the specified name
            File2.rename(fullName + randomInt, fullName);

            //success!

        } catch (Exception e) {
            try {
                if (in != null)
                    in.close(); //explicitly close it
            } catch (Exception e2) {
                //don't care
            }

            File2.delete(fullName + randomInt);

            throw e;

        }


        //diagnostic
        if (verbose) 
            String2.log("SaveOpendap.asNc done. created in " + 
                Calendar2.elapsedTimeString(System.currentTimeMillis() - time) + "\n  fileName=" + fullName);
        //String2.log(NcHelper.ncdump(directory + name + ext, "-h"));

    }


    /**
     * This downloads the MBARI Mx station data.
     */
    public static void downloadMbariData() throws Exception {
        //one time transfer of MBARI data
        verbose = true;
        String dir = "c:/data/mbari/";
        String2.log("\n*** One time transfer of MBARI data to " + dir);

        for (int i = 0; i < 3; i++)  //roughly ~4, ~57, ~37 minutes (crude remembrance)
            asNc("http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM" + i + ".nc", 
                dir + "M" + i + ".nc");
    }


    /**
     * This tests the methods in this class.
     */
    public static void basicTest() throws Exception {
        String2.log("\n*** SaveOpendap.basicTest...");
        verbose = true;
        //String dir = "c:/temp/"; 
        String dir = File2.getSystemTempDirectory();
        String name;

        //test ndbc       It would be better test if it actually tested the data.
        name = "SaveOpendapAsNcNDBC.nc";
        asNc("https://dods.ndbc.noaa.gov/thredds/dodsC/data/stdmet/31201/31201h2005.nc", dir + name);
        String info = NcHelper.ncdump(dir + name, "-h");
        int po = info.indexOf("{");
        info = info.substring(po);
String shouldBe =         
"{\n" +
"  dimensions:\n" +
"    time = UNLIMITED;   // (803 currently)\n" +
"    latitude = 1;\n" +
"    longitude = 1;\n" +
"  variables:\n" +
"    int wind_dir(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Wind Direction\";\n" +
"      :short_name = \"wdir\";\n" +
"      :standard_name = \"wind_from_direction\";\n" +
"      :units = \"degrees_true\";\n" +
"      :_FillValue = 999; // int\n" +
"\n" +
"    float wind_spd(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Wind Speed\";\n" +
"      :short_name = \"wspd\";\n" +
"      :standard_name = \"wind_speed\";\n" +
"      :units = \"meters/second\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    float gust(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Wind Gust Speed\";\n" +
"      :short_name = \"gst\";\n" +
"      :standard_name = \"gust\";\n" +
"      :units = \"meters/second\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    float wave_height(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Significant Wave Height\";\n" +
"      :short_name = \"wvht\";\n" +
"      :standard_name = \"significant_height_of_wave\";\n" +
"      :units = \"meters\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    float dominant_wpd(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Dominant Wave Period\";\n" +
"      :short_name = \"dpd\";\n" +
"      :standard_name = \"dominant_wave_period\";\n" +
"      :units = \"seconds\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    float average_wpd(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Average Wave Period\";\n" +
"      :short_name = \"apd\";\n" +
"      :standard_name = \"average_wave_period\";\n" +
"      :units = \"seconds\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    int mean_wave_dir(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Mean Wave Direction\";\n" +
"      :short_name = \"mwd\";\n" +
"      :standard_name = \"mean_wave_direction\";\n" +
"      :units = \"degrees_true\";\n" +
"      :_FillValue = 999; // int\n" +
"\n" +
"    float air_pressure(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Sea Level Pressure\";\n" +
"      :short_name = \"pres\";\n" +
"      :standard_name = \"air_pressure_at_sea_level\";\n" +
"      :units = \"hPa\";\n" +
"      :_FillValue = 9999.0f; // float\n" +
"\n" +
"    float air_temperature(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Air Temperature\";\n" +
"      :short_name = \"atmp\";\n" +
"      :standard_name = \"air_temperature\";\n" +
"      :units = \"degree_Celsius\";\n" +
"      :_FillValue = 999.0f; // float\n" +
"\n" +
"    float sea_surface_temperature(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Sea Surface Temperature\";\n" +
"      :short_name = \"wtmp\";\n" +
"      :standard_name = \"sea_surface_temperature\";\n" +
"      :units = \"degree_Celsius\";\n" +
"      :_FillValue = 999.0f; // float\n" +
"\n" +
"    float dewpt_temperature(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Dew Point Temperature\";\n" +
"      :short_name = \"dewp\";\n" +
"      :standard_name = \"dew_point_temperature\";\n" +
"      :units = \"degree_Celsius\";\n" +
"      :_FillValue = 999.0f; // float\n" +
"\n" +
"    float visibility(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Visibility\";\n" +
"      :short_name = \"vis\";\n" +
"      :standard_name = \"visibility_in_air\";\n" +
"      :units = \"US_statute_miles\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    float water_level(time=803, latitude=1, longitude=1);\n" +
"      :_CoordinateAxes = \"time latitude longitude \";\n" +
"      :long_name = \"Tide Water Level\";\n" +
"      :short_name = \"tide\";\n" +
"      :standard_name = \"water_level\";\n" +
"      :units = \"feet\";\n" +
"      :_FillValue = 99.0f; // float\n" +
"\n" +
"    int time(time=803);\n" +
"      :long_name = \"Epoch Time\";\n" +
"      :short_name = \"time\";\n" +
"      :standard_name = \"time\";\n" +
"      :units = \"seconds since 1970-01-01 00:00:00 UTC\";\n" +
"      :calendar = \"gregorian\";\n" + //appeared with switch to netcdf-java 4.6.4
"      :_CoordinateAxisType = \"Time\";\n" +
"\n" +
"    float latitude(latitude=1);\n" +
"      :long_name = \"Latitude\";\n" +
"      :short_name = \"latitude\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"\n" +
"    float longitude(longitude=1);\n" +
"      :long_name = \"Longitude\";\n" +
"      :short_name = \"longitude\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"\n" +
"  // global attributes:\n" +
"  :institution = \"NOAA National Data Buoy Center and Participators in Data Assembly Center\";\n" +
"  :url = \"http://dods.ndbc.noaa.gov\";\n" +
"  :quality = \"Automated QC checks with manual editing and comprehensive monthly QC\";\n" +
"  :conventions = \"COARDS\";\n" +
"  :station = \"31201\";\n" +
"  :comment = \"Floripa, Brazil (109)\";\n" +
"  :location = \"27.70 S 48.13 W \";\n" +
"  :_CoordSysBuilder = \"ucar.nc2.internal.dataset.conv.CoardsConventions\";\n" + //2021-01-07 changed with netcdf-java 5.4.1
//"  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.COARDSConvention\";\n" + //2013-02-21 reappeared. 2012-07-30 disappeared
"}\n";
        Test.ensureEqual(info, shouldBe, "info=" + info);
        File2.delete(dir + name);

        /* 
        //THIS WORKS BUT ITS TOO MUCH DATA FROM SOMEONE ELSE TO TEST ROUTINELY.
        //test MBARI M0: it has no structures, a unlimited dimension, global metadata,
        //  and 1D and 4D variables with metadata
        //in browser, see http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc.html
        name = "SaveOpendapAsNcMBARI.nc";
        //asNc("dods://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc", dir + name);  //doesn't solve problem
        asNc("http://dods.mbari.org/cgi-bin/nph-nc/data/OASISdata/netcdf/hourlyM0.nc", dir + name);
        String2.log(NcHelper.ncdump(dir + name, "-h"));
        File2.delete(dir + name);
        */

        /* doesn't work yet
        //test an opendap sequence (see Table.testConvert)
        name = "sequence.nc";
        asNc("https://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"", 
            dir + name);
        //String outName = testDir + "convert.nc";
        //convert(inName, READ_OPENDAP_SEQUENCE, outName, SAVE_AS_NC, "row", false);
        //Table table = new Table();
        //table.readFlatNc(outName, null, 0); //standardizeWhat=0; it should be already unpacked
        //String2.log(table.toString(3));
        //Test.ensureEqual(table.nColumns(), 2, "");
        //Test.ensureEqual(table.nRows(), 190, "");
        //Test.ensureEqual(table.getColumnName(0), "t0", "");
        //Test.ensureEqual(table.getColumnName(1), "oxygen", "");
        //Test.ensureEqual(table.columnAttributes(0).getString("long_name"), "Temperature T0", "");
        //Test.ensureEqual(table.columnAttributes(1).getString("long_name"), "Oxygen", "");
        //Test.ensureEqual(table.getDoubleData(0, 0), 12.1185, "");
        //Test.ensureEqual(table.getDoubleData(0, 1), 12.1977, "");
        //Test.ensureEqual(table.getDoubleData(1, 0), 6.56105, "");
        //Test.ensureEqual(table.getDoubleData(1, 1), 6.95252, "");
        //File2.delete(outName);
        String2.log(NcHelper.ncdump(dir + name, ""));
        //File2.delete(dir + name);
        */
       
    } 

    /**
     * This runs all of the interactive or not interactive tests for this class.
     *
     * @param errorSB all caught exceptions are logged to this.
     * @param interactive  If true, this runs all of the interactive tests; 
     *   otherwise, this runs all of the non-interactive tests.
     * @param doSlowTestsToo If true, this runs the slow tests, too.
     * @param firstTest The first test to be run (0...).  Test numbers may change.
     * @param lastTest The last test to be run, inclusive (0..., or -1 for the last test). 
     *   Test numbers may change.
     */
    public static void test(StringBuilder errorSB, boolean interactive, 
        boolean doSlowTestsToo, int firstTest, int lastTest) {
        if (lastTest < 0)
            lastTest = interactive? -1 : 0;
        String msg = "\n^^^ SaveOpendap.test(" + interactive + ") test=";

        for (int test = firstTest; test <= lastTest; test++) {
            try {
                long time = System.currentTimeMillis();
                String2.log(msg + test);
            
                if (interactive) {
                    //if (test ==  0) ...;

                } else {
                    if (test ==  0) basicTest();
                }

                String2.log(msg + test + " finished successfully in " + (System.currentTimeMillis() - time) + " ms.");
            } catch (Throwable testThrowable) {
                String eMsg = msg + test + " caught throwable:\n" + 
                    MustBe.throwableToString(testThrowable);
                errorSB.append(eMsg);
                String2.log(eMsg);
                if (interactive) 
                    String2.pressEnterToContinue("");
            }
        }
    }


    /**
     * NOT YET FINISHED. A method to get data from an opendap source
     * and save it in a file.
     *
     * @param args 
     * @throws Exception if trouble
     */
    public static void main(String args[]) throws Exception {

        
        //done
        String2.log("\n***** SaveOpendap.main finished successfully");
        Math2.incgc("SaveOpendap.main (between tests)", 2000); //in a test

    }


}
