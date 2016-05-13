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
import java.util.List;

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
//import dods.dap.*;


/**
 * This class has some static methods to read opendap data sources and
 * save the data in different types of files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2006-08-15
 */
public class SaveOpendap  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;

    /**
     * This generates a String with a dump of the contents of a netcdf file.
     * WARNING: if the file is big, this can be very slow.
     * WARNING: if printData is true, this may not show the data if there is lots of data. 
     *
     * @param fullFileName
     * @param printData if true, all of the data values are printed, too.
     * @return a String with the dump text
     */
    public static String ncDumpString(String fullFileName, 
            boolean printData) throws Exception {
        //changed with switch to netcdf-java 4.6.4
        StringWriter writer = new StringWriter();
        NCdumpW.print(fullFileName, writer,             
            printData, false /*print only coord variables*/, false /*ncml*/, false, //strict
            "" /*varNames*/, null /*cancel*/);
        return String2.replaceAll(writer.toString(), "\r", "");
    }

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

        //DODSNetcdfFile in = new DODSNetcdfFile(url); //bug? showed no global attributes //but fixed in 2.2.16
        //NetcdfFile in = NetcdfFile.open(url);  //this fails: The server does not support byte ranges.
        NetcdfDataset in = NetcdfDataset.openDataset(url);  
        try {

            //open the file (before 'try'); if it fails, no temp file to delete
            NetcdfFileWriter out = NetcdfFileWriter.createNew(
                NetcdfFileWriter.Version.netcdf3, fullName + randomInt);
            
            try {
                Group outRootGroup = out.addGroup(null, "");
                out.setFill(false);

                //get the list of dimensions
    //getDimensions is deprecated
                List globalDimList = in.getDimensions();

                //add the dimensions to 'out'
                int nGlobalDim = globalDimList.size();
                if (verbose) String2.log("  nGlobalDim=" + nGlobalDim);
                for (int i = 0; i < nGlobalDim; i++) {
                    Dimension dim = (Dimension)globalDimList.get(i);
                    String dimName = dim.getName();
                    out.addDimension(outRootGroup, dimName, dim.getLength(), true, //shared
                        dim.isUnlimited(), false);  //isUnknownLength
                    if (verbose) String2.log("    dimName" + i + "=" + dimName);
                }

                //get the list of opendap variables
                List varList = in.getVariables();

                //add the variables to .nc
                int nVars = varList.size();
                if (verbose) String2.log("  nVars=" + nVars);
                Variable newVars[] = new Variable[nVars];
                for (int v = 0; v < nVars; v++) {
                    //get the variable
                    Variable var = (Variable)varList.get(v);
                    String varName = var.getName();
                    if (verbose) String2.log("    varName" + v + "=" + varName);

                    //get nDimensions
                    List tDimList = var.getDimensions();
                    int nTDim = tDimList.size();

                    //create dimension[] 
                    ArrayList<Dimension> dimList = new ArrayList();
                    for (int d = 0; d < nTDim; d++) 
                        dimList.add((Dimension)tDimList.get(d));

                    //add the variable to 'out'
                    newVars[v] = out.addVariable(outRootGroup, varName, 
                        var.getDataType(), dimList); 

                    //write Attributes   (after adding variables)
                    List attList = var.getAttributes();
                    int nAtt = attList.size();
                    for (int a = 0; a < nAtt; a++) {
                        Attribute att = (Attribute)attList.get(a);
                        newVars[v].addAttribute(att);
                    }
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
                out.create();

                //write the data
                for (int v = 0; v < nVars; v++) {
                    Variable var = (Variable)varList.get(v);
                    if (verbose) String2.log("  writing data var=" + var.getName());

                    //write the data
                    out.write(newVars[v], var.read());
                }

                //I care about this exception
                out.close();

            } catch (Exception e) {
                try {
                    out.close(); //explicitly close it
                } catch (Exception e2) {
                    //don't care
                }
                throw e;
            }

            //I care about this exception
            in.close();
 
             //rename the file to the specified name
             File2.rename(fullName + randomInt, fullName);

             //success!

        } catch (Exception e) {
            try {
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
        //ncDump("End of SaveOpendap.asNc", directory + name + ext, false);

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
    public static void test() throws Exception {
        String2.log("\n*** SaveOpendap.asNc...");
        verbose = true;
        try {
            //String dir = "c:/temp/"; 
            String dir = File2.getSystemTempDirectory();
            String name;

            //test ndbc       It would be better test if it actually tested the data.
            name = "SaveOpendapAsNcNDBC.nc";
            asNc("http://dods.ndbc.noaa.gov/thredds/dodsC/data/stdmet/31201/31201h2005.nc", dir + name);
            String info = ncDumpString(dir + name, false);
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
"  :_CoordSysBuilder = \"ucar.nc2.dataset.conv.COARDSConvention\";\n" + //2013-02-21 reappeared. 2012-07-30 disappeared
" data:\n" +
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
            String2.log(ncDumpString(dir + name, false));
            File2.delete(dir + name);
            */

            /* doesn't work yet
            //test an opendap sequence (see Table.testConvert)
            name = "sequence.nc";
            asNc("http://oceanwatch.pfeg.noaa.gov/opendap/GLOBEC/GLOBEC_bottle?t0,oxygen&month=\"5\"", 
                dir + name);
            //String outName = testDir + "convert.nc";
            //convert(inName, READ_OPENDAP_SEQUENCE, outName, SAVE_AS_NC, "row", false);
            //Table table = new Table();
            //table.readFlatNc(outName, null, 0); //should be already unpacked
            //String2.log(table.toString("row", 3));
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
            String2.log(ncDumpString(dir + name, true));
            //File2.delete(dir + name);
            */
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "\nUnexpected error.");
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
        Math2.incgc(2000); //in a test

    }


}
