/* 
 * CenterGrids Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch.griddata;

import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;

import java.io.File;
import java.util.HashSet;

/**
 * This class is designed to be a stand-alone program to 
 * duplicate a directory and all its subdirectories
 * and .nc files (using GridSaveAs to revise the file and center the time
 * when the file is copied to the new directory). 
 * It uses info in the gov/noaa/pfel/coastwatch/DataSet.properties file.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-01-22
 */
public class DoubleCenterGrids {


    /**
     * This class is designed to be a stand-alone program to 
     * duplicate a directory and all its subdirectories
     * and .nc files (using GridSaveAs to revise the file by centering the time
     * AND converting lat, lon, altitude information to doubles 
     * when the file is copied to the new directory)
     * It uses information in the gov/noaa/pfel/coastwatch/DataSet.properties file.
     *
     * <p>A log file (CenterGrids.log) will be created in the same directory
     * as the "classes" directory. A list of files which failed to convert
     * (e.g., invalid .nc file) are printed at the end.
     * If the log gets bigger than 10 MB, it is closed and renamed to 
     * CenterGrids.log.previous, and CenterGrids.log is restarted.
     *
     * <p>There need to be two or threee parameters in this order:
     * <ul>
     * <li>'oldEndTimeDir' the directory with old, uncentered .nc files,
     *    e.g., /u00/satellite/avhrr_hrpt/ .
     *    Use the actual directory name, not the symbolic link name.
     * <li>'newCenteredTimeDir' the directory (which may or may not already exist)
     *    which will be made to mimic oldEndTimeDir
     *    (except that the .nc files will have centered times),
     *    e.g., /u00/satellite/centeredavhrr_hrpt .
     *    Use the actual directory name, not the symbolic link name.
     *    If a given .nc file already exists, this won't convert it again.
     * <li>-fast   If present, this program runs as fast as possible.
     *    If absent, the program will sleep between files and will sleep
     *    periodically for a whole minute.
     * </ul>
     *
     * @throws Exception if trouble (e.g., attempt to create a directory fails).
     *    Individual files that don't get converted (e.g., invalid .nc file)
     *    don't throw Exception and are listed at the end.
     */
    public static void main(String args[]) throws Exception {

        //setup commons logging
        String2.setupCommonsLogging(-1);

        //set up the log file
        String logDir = String2.getClassPath(); //with / separator and / at the end
        logDir = logDir.substring(0, logDir.length() - 1); //remove last "/"
        logDir = File2.getDirectory(logDir); //remove "classes"
        String2.setupLog(true, false, //tLogToSystemOut, tLogToSystemErr,
            logDir + "DoubleCenterGrids.log", false, //logToStringBuilder, 
            true, 10000000);  //append
        String2.log(
            "\n**************************************************************" +
            "\nDoubleCenterGrids " + Calendar2.getCurrentISODateTimeStringLocal() +
            "\nlogFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage());

        //proper number of arguments?
        if (args.length < 2) 
            Test.error(
                "This method duplicates a directory and all its subdirectories\n" +
                "and .nc files (using GridSaveAs to revise the file and center the time\n" +
                "when a file is copied to the new directory). \n" +
                "It uses infomation in the gov/noaa/pfel/coastwatch/DataSet.properties file.\n" +
                "\n" +
                "It must be called with two or three parameters, in this order:\n" +
                "* 'oldEndTimeDir' is the directory with old, uncentered .nc files,\n" +
                "    e.g., /u00/satellite/avhrr_hrpt/ .\n" +
                "    Use the actual directory name, not the symbolic link name.\n" +
                "    Use the actual directory name, not the symbolic link name.\n" +
                "* 'newCenteredTimeDir' is the directory (which may or may not already exist)\n" +
                "    which will be made to mimic oldEndTimeDir\n" +
                "    (except that the .nc files will have centered times),\n" +
                "    e.g., /u00/satellite/centeredavhrr_hrpt .\n" +
                "    Use the actual directory name, not the symbolic link name.\n" +
                "    If a given .nc file already exists, this won't convert it again.\n" +
                "* '-fast' If present, this program runs as fast as possible.\n" +
                "    If absent, the program will sleep between files and will sleep\n" +
                "    periodically for a whole minute.");

        long time = System.currentTimeMillis();

        //get fileNameUtility
        FileNameUtility fileNameUtility = new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser");
        
        //validate oldBaseDir and newBaseDir
        String oldBaseDir = args[0];
        String newBaseDir = args[1];
        File2.addSlash(oldBaseDir);
        File2.addSlash(newBaseDir);
        String2.log("(oldBaseDir=" + oldBaseDir + " newBaseDir=" + newBaseDir + ")");

        boolean fast = false;
        if (args.length >= 3 && args[2].equals("-fast"))
            fast = true;

        if (oldBaseDir.equals(newBaseDir))
            Test.error(String2.ERROR + ": oldBaseDir can't equal newBaseDir.");

        //ensure oldBaseDir exists
        if (!File2.isDirectory(oldBaseDir))
            Test.error(String2.ERROR + ": oldBaseDir doesn't exist!");
        File oldBaseDirFile = new File(oldBaseDir);

        //ensure newBaseDir exists
        File newBaseDirFile = new File(newBaseDir);
        if (!File2.isDirectory(newBaseDir)) 
            Test.ensureTrue(newBaseDirFile.mkdirs(),  //mkdirs makes parents, too, if needed
                String2.ERROR + ": Unable to create " + newBaseDir); 

        //get all the directories and file names
        String [] oldFiles = RegexFilenameFilter.recursiveFullNameList(
            oldBaseDir, ".+\\.nc", true);
        String [] newFiles = RegexFilenameFilter.recursiveFullNameList(
            newBaseDir, ".+\\.nc", true);

        //store all the new directories and file names (relative to the newBaseDir) 
        //in a hashSet 
        //so I can quickly find out if they already exist
        int oldBaseDirLength = oldBaseDir.length();
        int newBaseDirLength = newBaseDir.length();
        HashSet newFilesSet = new HashSet(Math2.roundToInt(1.4 * newFiles.length));
        for (int i = 0; i < newFiles.length; i++)
            newFilesSet.add(newFiles[i].substring(newBaseDirLength));

        //ensure all the old directories and files exist in the new dir
        long cumSleep = 0;
        int nFilesAlreadyExist = 0;
        int nFilesConverted = 0;
        StringArray filesFailed = new StringArray();
        for (int i = 0; i < oldFiles.length; i++) {
            String2.log("");
            String oldName = oldFiles[i];
            String relativeName = oldName.substring(oldBaseDirLength);
            String newName = newBaseDir + relativeName;
            //String2.log("oldName=" + oldName);
            //String2.log("newName=" + newName);

            //does file or directory already exist?
            if (newFilesSet.contains(relativeName)) {
                String2.log("Already exists: " + newName);
                nFilesAlreadyExist++;
                continue;
            }

            //make the new directory
            char lastChar = oldName.charAt(oldName.length() - 1);
            if (lastChar == '\\' || lastChar == '/') {
                String2.log("Creating directory: " + newName);
                File newDir = new File(newName);
                //throw exception if can't create directory
                Test.ensureTrue(newDir.mkdirs(), 
                    String2.ERROR + ": unable to create directory " + newName); 
                continue;
            }
                
            //make the new file
            try {

                //GridSaveAs
                //String2.log("Creating file: " + newName); //davesSaveAs says this info
                long tTime = System.currentTimeMillis();
                StringArray tsa = Grid.davesSaveAs(new String[]{oldName, newName}, fileNameUtility);
                if (tsa.size() > 0)
                    filesFailed.append(tsa);
                else nFilesConverted++;

                //sleep?
                if (!fast) {
                    long tSleep = Math.max(100, System.currentTimeMillis() - tTime); 
                    Math2.sleep(tSleep);
                    cumSleep += tSleep;
                    if (cumSleep >= 60000) {
                        String2.log("Sleeping for 1 minute...");
                        cumSleep -= 60000;
                        Math2.gc(60000); //sleep for 1 minute
                    }
                }

            } catch (Exception e) {
                String2.log(String2.ERROR + " in DoubleCenterGrids while converting\n" +
                    "oldName=" + oldName + "\n" + 
                    "newName=" + newName + "\n" + 
                    MustBe.throwableToString(e)); 
                filesFailed.add(oldName);
            }

        }

        //done
        String2.log(
            "\n*** List of files that couldn't be converted (e.g., invalid .nc file):\n" +
            filesFailed.toNewlineString() + 
            "\n*** DoubleCenterGrids done. nFilesAlreadyExist=" + nFilesAlreadyExist + 
                " nFilesConverted=" + nFilesConverted + 
            "\n  nFilesFailed=" + filesFailed.size() + 
            "  TOTAL TIME=" + Calendar2.elapsedTimeString(System.currentTimeMillis() - time));
        String2.returnLoggingToSystemOut();
    }


    /**
     * This tests main() on Bob's Windows computer.
     *
     * @throws Exception
     */
    public static void test() throws Exception {


        //delete old files
        String newDir = "c:/u00/centeredSatellite/AG";
        RegexFilenameFilter.recursiveDelete(newDir);

        //copy a dir and subdirs and files
        main(new String[]{"c:/u00/satellite/AG", newDir});

        //do tests
        String ncDump = NcHelper.dumpString(
            "c:/u00/centeredSatellite/AG/ssta/1day/AG2005040_2005040_ssta.nc", false);
        //ensure time is centered correctly
        Test.ensureEqual(Calendar2.epochSecondsToIsoStringT(1.1079504E9), //# from time actual_range
            "2005-02-09T12:00:00", "");
        String reference = 
"netcdf AG2005040_2005040_ssta.nc {\n" +
"  dimensions:\n" +
"    time = 1;\n" +   // (has coord.var)\n" + //changed when switched to netcdf-java 4.0, 2009-02-23
"    altitude = 1;\n" +   // (has coord.var)\n" +
"    lat = 91;\n" +   // (has coord.var)\n" +
"    lon = 91;\n" +   // (has coord.var)\n" +
"  variables:\n" +
"    double time(time=1);\n" +
"      :actual_range = 1.1079504E9, 1.1079504E9; // double\n" +
"      :fraction_digits = 0; // int\n" +
"      :long_name = \"Centered Time\";\n" +
"      :units = \"seconds since 1970-01-01T00:00:00Z\";\n" +
"      :standard_name = \"time\";\n" +
"      :axis = \"T\";\n" +
"      :_CoordinateAxisType = \"Time\";\n" +
"\n" +
"    double altitude(altitude=1);\n" +
"      :actual_range = 0.0, 0.0; // double\n" +
"      :fraction_digits = 0; // int\n" +
"      :long_name = \"Altitude\";\n" +
"      :positive = \"up\";\n" +
"      :standard_name = \"altitude\";\n" +
"      :units = \"m\";\n" +
"      :axis = \"Z\";\n" +
"      :_CoordinateAxisType = \"Height\";\n" +
"      :_CoordinateZisPositive = \"up\";\n" +
"\n" +
"    double lat(lat=91);\n" +
"      :_CoordinateAxisType = \"Lat\";\n" +
"      :actual_range = 33.5, 42.5; // double\n" +
"      :axis = \"Y\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :long_name = \"Latitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"latitude\";\n" +
"      :units = \"degrees_north\";\n" +
"\n" +
"    double lon(lon=91);\n" +
"      :_CoordinateAxisType = \"Lon\";\n" +
"      :actual_range = 230.5, 239.5; // double\n" +
"      :axis = \"X\";\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :long_name = \"Longitude\";\n" +
"      :point_spacing = \"even\";\n" +
"      :standard_name = \"longitude\";\n" +
"      :units = \"degrees_east\";\n" +
"\n" +
"    float AGssta(time=1, altitude=1, lat=91, lon=91);\n" +
"      :_FillValue = -9999999.0f; // float\n" +
"      :actual_range = 9.7f, 14.5f; // float\n" +
"      :coordsys = \"geographic\";\n" +
"      :fraction_digits = 1; // int\n" +
"      :long_name = \"SST, NOAA POES AVHRR, GAC, 0.1 degrees, Global, Day and Night\";\n" +
"      :missing_value = -9999999.0f; // float\n" +
"      :numberOfObservations = 591; // int\n" +
"      :percentCoverage = 0.07136819224731313; // double\n" +
"      :standard_name = \"sea_surface_temperature\";\n" +
"      :units = \"degree_C\";\n" +
"\n" +
"  // global attributes:\n" +
"  :acknowledgement = \"NOAA NESDIS COASTWATCH, NOAA SWFSC ERD\";\n" +
"  :cdm_data_type = \"Grid\";\n" +
"  :cols = 91; // int\n" +
"  :composite = \"true\";\n" +
"  :contributor_name = \"NOAA NESDIS OSDPD\";\n" +
"  :contributor_role = \"Source of level 2 data.\";\n" +
"  :Conventions = \"COARDS, CF-1.6, ACDD-1.3, CWHDF\";\n" +
"  :creator_email = \"dave.foley@noaa.gov\";\n" +
"  :creator_name = \"NOAA CoastWatch, West Coast Node\";\n" +
"  :creator_url = \"http://coastwatch.pfeg.noaa.gov\";\n" +
"  :cwhdf_version = \"3.4\";\n";
        
//" :date_created = \"2007-01-23Z\";\n" +
//" :date_issued = \"2007-01-23Z\";\n" +
String reference2=
"  :Easternmost_Easting = 239.5; // double\n" +
"  :et_affine = 0.0, 0.1, 0.1, 0.0, 230.5, 33.5; // double\n" +
"  :gctp_datum = 12; // int\n" +
"  :gctp_parm = 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0; // double\n" +
"  :gctp_sys = 0; // int\n" +
"  :gctp_zone = 0; // int\n" +
"  :geospatial_lat_max = 42.5; // double\n" +
"  :geospatial_lat_min = 33.5; // double\n" +
"  :geospatial_lat_resolution = 0.1; // double\n" +
"  :geospatial_lat_units = \"degrees_north\";\n" +
"  :geospatial_lon_max = 239.5; // double\n" +
"  :geospatial_lon_min = 230.5; // double\n" +
"  :geospatial_lon_resolution = 0.1; // double\n" +
"  :geospatial_lon_units = \"degrees_east\";\n" +
"  :geospatial_vertical_max = 0.0; // double\n" +
"  :geospatial_vertical_min = 0.0; // double\n" +
"  :geospatial_vertical_positive = \"up\";\n" +
"  :geospatial_vertical_units = \"m\";\n" +
"  :history = \"NOAA NESDIS OSDPD\n" +
"20";   //start of a date

//"2007-01-23T19:30:17Z NOAA CoastWatch, West Coast Node\";\n" +
String reference3=
"  :id = \"LAGsstaS1day_20050209120000\";\n" +
"  :institution = \"NOAA CoastWatch, West Coast Node\";\n" +
"  :keywords = \"EARTH SCIENCE > Oceans > Ocean Temperature > Sea Surface Temperature\";\n" +
"  :keywords_vocabulary = \"GCMD Science Keywords\";\n" +
"  :license = \"The data may be used and redistributed for free but is not intended for legal use, since it may contain inaccuracies. Neither the data Contributor, CoastWatch, NOAA, nor the United States Government, nor any of their employees or contractors, makes any warranty, express or implied, including warranties of merchantability and fitness for a particular purpose, or assumes any legal liability for the accuracy, completeness, or usefulness, of this information.\";\n" +
"  :naming_authority = \"gov.noaa.pfeg.coastwatch\";\n" +
"  :Northernmost_Northing = 42.5; // double\n" +
"  :origin = \"NOAA NESDIS OSDPD\";\n" +
"  :pass_date = 12823; // int\n" +
"  :polygon_latitude = 33.5, 42.5, 42.5, 33.5, 33.5; // double\n" +
"  :polygon_longitude = 230.5, 230.5, 239.5, 239.5, 230.5; // double\n" +
"  :processing_level = \"3 (projected)\";\n" +
"  :project = \"CoastWatch (http://coastwatch.noaa.gov/)\";\n" +
"  :projection = \"geographic\";\n" +
"  :projection_type = \"mapped\";\n" +
"  :references = \"NOAA POES satellites information: http://www.oso.noaa.gov/poes/index.htm . Processing link: http://www.osdpd.noaa.gov/PSB/PSB.html . Processing reference: Walton C. C., W. G. Pichel, J. F. Sapper, D. A. May. The development and operational application of nonlinear algorithms for the measurement of sea surface temperatures with the NOAA polar-orbiting environmental satellites. J.G.R., 103: (C12) 27999-28012, 1998. Cloudmask reference: Stowe, L. L., P. A. Davis, and E. P. McClain.  Scientific basis and initial evaluation of the CLAVR-1 global clear/cloud classification algorithm for the advanced very high resolution radiometer.  J. Atmos. Oceanic Technol., 16, 656-681. 1999. Calibration and Validation: Li, X., W. Pichel, E. Maturi, P. Clemente-Colon, and J. Sapper. Deriving the operational nonlinear multi-channel sea surface temperature algorithm coefficients for NOAA-15 AVHRR/3. International Journal of Remote Sensing, Volume 22, No. 4, 699 - 704, March 2001a. Calibration and Validation: Li, X, W. Pichel, P. Clemente-Colon, V. Krasnopolsky, and J. Sapper. Validation of coastal sea and lake surface temperature measurements derived from NOAA/AVHRR Data. International Journal of Remote Sensing, Vol. 22, No. 7, 1285-1303, 2001b.\";\n" +
"  :rows = 91; // int\n" +
"  :satellite = \"POES\";\n" +
"  :sensor = \"AVHRR GAC\";\n" +
"  :source = \"satellite observation: POES, AVHRR GAC\";\n" +
"  :Southernmost_Northing = 33.5; // double\n" +
"  :standard_name_vocabulary = \"CF Standard Name Table v27\";\n" +
"  :start_time = 0.0; // double\n" +
"  :summary = \"NOAA CoastWatch provides sea surface temperature (SST) products derived from NOAA's Polar Operational Environmental Satellites (POES).  This data provides global area coverage at 0.1 degrees resolution.  Measurements are gathered by the Advanced Very High Resolution Radiometer (AVHRR) instrument, a multiband radiance sensor carried aboard the NOAA POES satellites.\";\n" +
"  :time_coverage_end = \"2005-02-10T00:00:00Z\";\n" +
"  :time_coverage_start = \"2005-02-09T00:00:00Z\";\n" +
"  :title = \"SST, NOAA POES AVHRR, GAC, 0.1 degrees, Global, Day and Night\";\n" +
"  :Westernmost_Easting = 230.5; // double\n" +
" data:\n" +
"}\n";
        //String2.log(ncDump);
        Test.ensureEqual(ncDump.substring(0, reference.length()), reference, "ncDump=" + ncDump + "\nreference=" + reference);
        int po = ncDump.indexOf("  :Easternmost_Easting");
        Test.ensureEqual(ncDump.substring(po, po + reference2.length()), reference2, "ncDump=" + ncDump);
        po = ncDump.indexOf("  :id = \"LAGsstaS1day");
        Test.ensureEqual(ncDump.substring(po), reference3, "ncDump=" + ncDump);

        //just ensure the other file exists
        Test.ensureTrue(File2.isFile(
            "c:/u00/centeredSatellite/AG/ssta/1day/AG2005041_2005041_ssta.nc"),
            "");

        //delete the log file

        File2.delete("c:/programs/tomcat/webapps/cwexperimental/WEB-INF/DoubleCenterGrids.log");

    }
}
