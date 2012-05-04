/* 
 * Projects2 Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.util;

import com.cohort.array.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.hdf.*;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.util.*;
import gov.noaa.pfel.erddap.dataset.*;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;



/**
 * This class has static methods related to various Projects Bob works on.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2011-06-30
 */
public class Projects2  {

    /**
     * Set this to true (by calling verbose=true in your program, not but changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;


    /** "ERROR" is defined here (from String2.ERROR) so that it is consistent in log files. */
    public final static String ERROR = String2.ERROR; 


    /** This processes NODC WOD data files from source to make consistent for ERDDAP.
     * 2011-04-25
     */
    public static void nodcWOD(
            String inDir, String outDir) throws Throwable {
        String[] fileNames = RegexFilenameFilter.list(inDir, ".*\\.nc");
        int col;
        Attributes atts;
        
        for (int f = 0; f < fileNames.length; f++) {
            //*** read file into Table
            String fileName = fileNames[f];
            String2.log("fileName=" + fileName);
            Table table = new Table();
            table.readFlat0Nc(inDir + fileName, null, 1, -1); //1=unpack, -1=read all rows
            int nRows = table.nRows();
            String2.log(table.toCsvString());

            //reject if no data
            double d = table.getColumn("time").getDouble(0);
            String2.log("  time=" + d);
            if (Double.isNaN(d) || d > 1e36) {
                String2.log("  ! time is invalid. So reject this file.");
                continue;
            }

            //***save file
            table.saveAsFlatNc(outDir + fileName, "row");
        }
    }

    /** This recursively duplicates/updates the files from a Hyrax 
     * (assuming Hyrax's HTTP server is enabled),
     * including duplicating the file's date.  
     *
     * @param urlDir  e.g., http://data.nodc.noaa.gov/opendap/wod/monthly/  
     *    which has contents.html
     * @param localDir  e.g., F:/data/wod/monthly/
     */
    public static void copyHyraxFiles(String urlDir, String fileNameRegex, String localDir,
        String logFileName)  throws Throwable {

        if (logFileName != null && logFileName.length() > 0)
            String2.setupLog(true, false, logFileName, false, true, Integer.MAX_VALUE);

        String2.log("Projects2.copyHyraxFiles " + Calendar2.getCurrentISODateTimeStringLocal() +
            "\n  " + urlDir + "\n  " + localDir);
        
        //parse the hyrax catalog
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        EDDGridFromDap.addToHyraxUrlList(urlDir + "contents.html", fileNameRegex, 
            true, childUrls, lastModified);

        //copy the files
        int n = childUrls.size();
        String2.log("nUrls found=" + n);
        int urlDirLength = urlDir.length();
        for (int i = 0; i < n; i++) {
            String url = childUrls.get(i);
            long millis = Double.isNaN(lastModified.get(i))? Long.MAX_VALUE : 
                Math.round(lastModified.get(i) * 1000);
            if (url.startsWith(urlDir)) { //it should
                String2.log("file #" + i + " of " + n + ": " + url.substring(urlDirLength));
                String fullFileName = localDir + url.substring(urlDirLength);

                //does file already exist?
                boolean downloadIt = !File2.isFile(fullFileName);  //doesn't exist
                if (!downloadIt &&                                 //if file exists
                    millis != Long.MAX_VALUE &&                    // and url time is known
                    millis != File2.getLastModified(fullFileName)) // and times are different
                    downloadIt = true;                             //then download it
                if (downloadIt) {
                    try {
                        //makeDir
                        File2.makeDirectory(File2.getDirectory(fullFileName));

                        //copy the file
                        String2.log("  download it");
                        SSR.downloadFile(url, fullFileName, true);
                        if (millis != Long.MAX_VALUE)
                            File2.setLastModified(fullFileName, millis);
                    } catch (Throwable t) {
                        String2.log(MustBe.throwableToString(t));
                    }
                } else {
                    String2.log("  file already exists");
                }
            }
        }

    }


}

