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
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


// from netcdfAll-x.jar
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
//import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;



/**
 * This class has static methods related to various Projects Bob works on.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2011-06-30
 */
public class Projects2  {

    /**
     * Set this to true (by calling verbose=true in your program, not by changing the code here)
     * if you want lots of diagnostic messages sent to String2.log.
     */
    public static boolean verbose = false;


    /**
     * One-time Use: Whenever possible, this grabs keyword metadata from a 
     * newly auto-generated datasets.xml and inserts it into
     * an older hand-generated datasets.xml.
     */
    public static void copyKeywords() throws Throwable {
        String handXmlName = "c:/programs/_tomcat/content/erddap/datasets2.xml";
        String autoXmlName = "c:/temp/datasets20111019.xml";
        String resultName  = "c:/programs/_tomcat/content/erddap/datasets2New.xml";

        //read handXml
        String sar[] = File2.readFromFileUtf8(handXmlName);
        if (sar[0].length() > 0) {
            String2.log("Error reading handXml: " + sar[0]);
            return;
        }
        StringBuilder handXml = new StringBuilder(sar[1]);

        //read autoXml
        sar = File2.readFromFileUtf8(autoXmlName);
        if (sar[0].length() > 0) {
            String2.log("Error reading autoXml: " + sar[0]);
            return;
        }
        StringBuilder autoXml = new StringBuilder(sar[1]);
        sar = null; //gc

        String ip[] = {
            "https://192.168.31.15/", "https://coastwatch.pfeg.noaa.gov/", 
            "https://192.168.31.18/", "https://oceanwatch.pfeg.noaa.gov/",
            "https://192.168.31.27/", "https://thredds1.pfeg.noaa.gov/"  ,
            "https://192.168.31.13/", "https://las.pfeg.noaa.gov/"       };
            //don't want to find the datasets on upwell
            //"https://192.168.31.6/", "https://upwell.pfeg.noaa.gov/"    };

        int handXmlBasePo = 0;
        while (true) {
            //find the next dataset in handXml
            int handXmlSource1Po = handXml.indexOf("<sourceUrl>", handXmlBasePo);
            if (handXmlSource1Po < 0)
                break;
            int handXmlSource2Po = handXml.indexOf("</sourceUrl>", handXmlSource1Po);
            if (handXmlSource2Po < 0)
                break;
            handXmlBasePo = handXmlSource2Po;

            String url = handXml.substring(handXmlSource1Po + 11, handXmlSource2Po);
            if (url.startsWith("https://upwell."))
                continue;
            if (url.endsWith("hdayCompress"))
                break;
            
            for (int ipi = 0; ipi < ip.length; ipi+=2) {
                if (url.startsWith(ip[ipi]))
                    url = ip[ipi + 1] + url.substring(ip[ipi].length());
            }


            //find the license for that dataset in handXml
            int handXmlLicensePo = handXml.indexOf("<att name=\"license\">", handXmlSource1Po);
            String2.log(handXmlBasePo + " " + url);


            //find that url in autoXml
            int autoXmlUrlPo = autoXml.indexOf(url, 0);
            if (autoXmlUrlPo < 0) {
                String2.log("Warning: url not found in autoXml.");
                continue;
            }
            int aaPo = autoXml.indexOf("<addAttributes>", autoXmlUrlPo);
            if (aaPo < 0) {
                String2.log("Warning: <addAttributes> not found in autoXml.");
                continue;
            }
            int keywords1Po = autoXml.indexOf("<att name=\"keywords\">", aaPo);
            if (keywords1Po < 0) {
                String2.log("Warning: keywords not found in autoXml.");
                continue;
            }
            int keywords2Po = autoXml.indexOf("</att>", keywords1Po);
            if (keywords2Po < 0) {
                String2.log("Warning: </att> not found in autoXml.");
                continue;
            }


            //insert keywords into handXml
            handXml.insert(handXmlLicensePo, 
                autoXml.substring(keywords1Po, keywords2Po + 6) + "\n        ");

        }
        autoXml = null; //gc

        //save the changes
        File2.writeToFileUtf8(resultName, handXml.toString());
        String2.log("Finished successfully");

    }

    /**
     * One-time Use: For usgs_waterservices datasets, this replaces existing
     * keywords with new auto-generated keywords.
     */
    public static void copyKeywordsUsgs() throws Throwable {
        String handXmlName = "c:/programs/_tomcat/content/erddap/datasets2.xml";
        String resultName  = "c:/programs/_tomcat/content/erddap/datasets2New.xml";

        //read handXml
        String sar[] = File2.readFromFileUtf8(handXmlName);
        if (sar[0].length() > 0) {
            String2.log("Error reading handXml: " + sar[0]);
            return;
        }
        StringBuilder handXml = new StringBuilder(sar[1]);

        int handXmlBasePo = 0;
        while (true) {
            //find the next dataset in handXml
            int handXmlDataset1Po = handXml.indexOf("datasetID=\"usgs_waterservices", handXmlBasePo);
            if (handXmlDataset1Po < 0)
                break;
            int handXmlDataset2Po = handXml.indexOf("\"", handXmlDataset1Po + 29);
            if (handXmlDataset2Po < 0)
                break;
            String datasetID = handXml.substring(handXmlDataset1Po + 11, handXmlDataset2Po);

            //find the "license"
            int handXmlLicensePo = handXml.indexOf("<att name=\"license\">", handXmlDataset2Po);
            if (handXmlLicensePo < 0)
                break;
            handXmlBasePo = handXmlLicensePo;

            //try to make the new keywords
            String newKeywords = getKeywords(datasetID).trim();
            if (newKeywords.length() == 0)
                continue;

            //insert keywords into handXml
            handXml.insert(handXmlLicensePo, newKeywords + 
                "\n        " +
                "<att name=\"keywords_vocabulary\">GCMD Science Keywords</att>" + 
                "\n        ");

        }

        //save the changes
        File2.writeToFileUtf8(resultName, handXml.toString());
        String2.log("Finished successfully");

    }

    /**
     * This tries to make the keywords for one existing dataset.
     * If successful, it puts the recommended "keywords" on the clipboard.
     */
    public static String getKeywords(String datasetID) throws Throwable {
        String servers[] = {
            "https://coastwatch.pfeg.noaa.gov/erddap/griddap/",
            "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/",
            "https://upwell.pfeg.noaa.gov/erddap/griddap/",
            "https://upwell.pfeg.noaa.gov/erddap/tabledap/"};

        String keywords = "";
        String2.log("datasetID=" + datasetID);
        for (int serv = 0; serv < servers.length; serv++) {
            try {
                //try EDDGrid
                String s = EDDGridFromDap.generateDatasetsXml( 
                    servers[serv] + datasetID, 
                    null, null, null, EDD.DEFAULT_RELOAD_EVERY_N_MINUTES, new Attributes());

                int aaPo = s.indexOf("<addAttributes>", 0);
                if (aaPo < 0) {
                    String2.log("Warning: <addAttributes> not found in xml.");
                    continue;
                }
                int keywords1Po = s.indexOf("<att name=\"keywords\">", aaPo);
                if (keywords1Po < 0) {
                    String2.log("Warning: keywords not found in addAttributes xml.");
                    keywords1Po = s.indexOf("<att name=\"keywords\">", 0);
                    if (keywords1Po < 0) {
                        String2.log("Warning: keywords not found in sourceAttributes xml.");
                        continue;
                    }
                }
                int keywords2Po = s.indexOf("</att>", keywords1Po);
                if (keywords2Po < 0) {
                    String2.log("Warning: </att> not found in xml.");
                    continue;
                }

                //keywords
                keywords = s.substring(keywords1Po, keywords2Po + 6);
                String2.setClipboardString("        " + keywords + "\n");
                String2.log("\n" + keywords);

                //success!
                break;

            } catch (Throwable t) {
                String2.log(MustBe.throwableToString(t));
            }
        }
        return keywords;
    }

    /**
     * This repeatedly asks for the datasetID of a coastwatch erddap dataset,
     * generates the ready-to-use datasets.xml for it, and
     * puts the recommended "keywords" on the clipboard.
     */
    public static void getKeywords() throws Throwable {

        while (true) {
            String datasetID = String2.getStringFromSystemIn("\n\ndatasetID? ");
            getKeywords(datasetID);
        }
    }


    
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
            table.readFlat0Nc(inDir + fileName, null, 1, -1); //standardizeWhat=1, -1=read all rows
            int nRows = table.nRows();
            String2.log(table.toString());

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
     * @param urlDir  e.g., https://data.nodc.noaa.gov/opendap/wod/monthly/  
     *    which has contents.html
     * @param localDir  e.g., F:/data/wod/monthly/
     */
    public static void copyHyraxFiles(String urlDir, String fileNameRegex, 
        String localDir, String pathRegex, String logFileName)  throws Throwable {

        if (logFileName != null && logFileName.length() > 0)
            String2.setupLog(true, false, logFileName, true, 1000000000);
        String2.log("*** Projects2.copyHyraxFiles " + Calendar2.getCurrentISODateTimeStringLocalTZ() +
            "\nlogFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage() + "\n  " + 
            urlDir + "\n  " + localDir);
        
        //parse the hyrax catalog
        StringArray childUrls = new StringArray();
        DoubleArray lastModified = new DoubleArray();
        LongArray fSize = new LongArray();
        FileVisitorDNLS.addToHyraxUrlList(
            urlDir + "contents.html", fileNameRegex, true, pathRegex, false, //recursive, dirsToo
            childUrls, lastModified, fSize);

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
        String2.log("*** Projects2.copyHyraxFiles finished successfully at " + 
            Calendar2.getCurrentISODateTimeStringLocalTZ() + "\n");
        if (logFileName != null && logFileName.length() > 0)
            String2.returnLoggingToSystemOut();
    }

}

