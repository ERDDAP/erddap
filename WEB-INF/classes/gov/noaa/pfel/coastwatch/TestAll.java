/* 
 * TestAll Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.*;
import com.cohort.ema.*;
import com.cohort.util.*;

import gov.noaa.pfel.coastwatch.*;
import gov.noaa.pfel.coastwatch.griddata.*;
import gov.noaa.pfel.coastwatch.hdf.*;
import gov.noaa.pfel.coastwatch.netcheck.*;
import gov.noaa.pfel.coastwatch.pointdata.*;
import gov.noaa.pfel.coastwatch.sgt.*;
import gov.noaa.pfel.coastwatch.util.*;
import gov.noaa.pfel.erddap.*;
import gov.noaa.pfel.erddap.dataset.*;
import gov.noaa.pfel.erddap.util.*;
import gov.noaa.pfel.erddap.variable.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.apache.commons.logging.impl.*;
import org.json.JSONObject;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.*;
import ucar.nc2.dods.*;
import ucar.nc2.util.*;
import ucar.ma2.*;
/**
 * This is a very important class -- main() calls all of the unit tests relevant 
 * to CWBrowser and ERDDAP.
 * Also, compiling this class forces compilation of all the classes that need to
 * be deployed, so this class is compiled and and main() is run prior to
 * calling makeCWExperimentalWar (and ultimately makeCoastWatchWar) or makeErddapWar and
 * deploying the .war file to Tomcat.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2005-10-06
 */
public class TestAll  {

    /**
     * This forces compilation of all the classes that need to
     * be deployed and calls all of the unit tests relevant to CWBrowser.
     *
     * @param args is ignored
     * @throws Throwable if trouble
     */
    public static void main(String args[]) throws Throwable {

        //always setup commons logging
        String2.setupCommonsLogging(-1);

        //set log file to <bigParentDir>/logs/TestAll.out
        EDStatic.quickRestart = false; //also, this forces EDStatic instantiation when running TestAll
        String2.setupLog(true, false, //output to system.out and a file:
            EDStatic.fullLogsDirectory + "TestAll.log", 
            false, false, Integer.MAX_VALUE); 
        EDD.testVerboseOn();
        String2.log("*** Starting TestAll " + 
            Calendar2.getCurrentISODateTimeStringLocal() + "\n" + 
            "logFile=" + String2.logFileName() + "\n" +
            String2.standardHelpAboutMessage() + "\n" +
            "This must be run from a command line window because the SFTP and email tests ask for passwords.\n");

        //this might cause small problems for a public running erddap
        //but Bob only uses this on laptop, with private erddap.
        File2.deleteAllFiles(EDStatic.fullPublicDirectory, true, false);  //recursive, deleteEmptySubdirectories 
        File2.deleteAllFiles(EDStatic.fullCacheDirectory,  true, false);  

//ONE TIME TESTS -- ~alphabetical by class name

        /* 
        //convert isoDate to/from epoch seconds (a common utility I need)
        String2.log("Enter an ISO date/time or secondsSinceEpoch or YYYYDDD (or \"\" to stop)...");
        while (true) {
            String dateTime = String2.getStringFromSystemIn("? ");
            if (dateTime.length() == 0)
                break;
            int dashPo = dateTime.indexOf("-");
            int dashPo2 = dateTime.indexOf("-", Math.max(dashPo, 0));
            GregorianCalendar gc;
            try {
                if (dateTime.length() == 7)  gc = Calendar2.parseYYYYDDDZulu(dateTime);     //throws Exception if trouble
                else if (dashPo2 > 0)        gc = Calendar2.parseISODateTimeZulu(dateTime); //throws Exception if trouble
                else                         gc = Calendar2.newGCalendarZulu((long)(String2.parseDouble(dateTime)*1000));
                String2.log(
                    "iso=" + Calendar2.formatAsISODateTimeT(gc) +
                    "  seconds=" + String2.genEFormat6(Math2.floorDiv(gc.getTimeInMillis(), 1000)) + 
                    "  YYYYDDD=" + Calendar2.formatAsYYYYDDD(gc));            
            } catch (Exception e) {
                String2.log(e.toString());
            }
        }
        /* */

//      Boundaries.bobConvertAll();
//      Boundaries.test();

//    Calendar2
//    String2.log(Calendar2.epochSecondsToIsoStringT(1000* 86400L));
//    GregorianCalendar tgc = Calendar2.parseISODateTimeZulu("0000-01-01");
//    String2.log("year 0 is leap year? " + tgc.isLeapYear(0) + " " + Calendar2.formatAsISODate(tgc));
//    tgc.set(Calendar.MONTH, 3);
//    String2.log("0000-03-01 day = " + tgc.get(Calendar.DAY_OF_YEAR));
//    tgc.set(Calendar2.YEAR, 2000);
//    String2.log("year 2000 is leap year? " + tgc.isLeapYear(0) + " " + Calendar2.formatAsISODate(tgc));
//    String2.log("" + (-1.25 % 1.0));  //answer is -.25
//    long tl = Calendar2.newGCalendarZulu(1858, 10, 17).getTimeInMillis();
//    double td = tl;
//    String2.log("tl=" + tl + " td=" + td); 
//    
//    Table.testReadNcCFASAProfile(false);

//    Table.debugMode = true; DasDds.main(new String[]{"swfscTrinCTD", "-verbose"});

/*    if (false) { //one time fixup of scrippsGliders
        String dir = "/u00/data/points/scrippsGliders/batch2/";
        File file = new File(dir);
        String tList[] = file.list();
        for (int i = 0; i < tList.length; i++) {
            SSR.dosOrCShell(
                "c:\\programs\\nco\\ncatted -O -a standard_name,salinity,o,c,sea_water_practical_salinity " + dir + tList[i], 60).toArray(new String[0]);
            SSR.dosOrCShell(
                "c:\\programs\\nco\\ncatted -O -a units,salinity,o,c,1 " + dir + tList[i], 60).toArray(new String[0]);
        }
    } /* */

//    String2.log(NcHelper.dumpString("/u00/data/points/eb/CoralSea_CS140116.nc", true));

//    Table table = new Table();
////    table.readNDNc("c:/data/rutgers/NOAA_COOPS_WIND_STATIONS_SUBSET.nc", 
////        new String[]{"longitude","latitude","station","time"}, //loadVars
////        "", Double.NaN, Double.NaN, true);
//    table.readNcCF("c:/data/rutgers/NOAA_COOPS_WIND_STATIONS_SUBSET.nc", 
//       StringArray.fromCSV("longitude,latitude,station,time"), //loadVars
//       StringArray.fromCSV(""),  //names
//       StringArray.fromCSV(""),  //ops
//       StringArray.fromCSV(""));  //values
//    table.saveAsDAS(System.out, "s");
//    table.saveAsDDS(System.out, "s");

//    String2.log(DigirHelper.getObisInventoryString(
//        "http://iobis.marine.rutgers.edu/digir2/DiGIR.php", 
//        "OBIS-SEAMAP", 
//        "darwin:ScientificName"));
//        //"darwin:Genus");
//    DigirHelper.testOpendapStyleObis();

//    EDD tedd = EDD.oneFromDatasetXml("nmspWcosTemp"); System.out.println(tedd.toString());
//    tedd = EDD.oneFromDatasetXml("cPostDet3"); System.out.println(tedd.toString());
//    (EDDTable)EDD.oneFromDatasetXml("pmelTao")).getEmpiricalMinMax("2008-10-05", "2008-10-10", false, false);
//    String2.log(((EDDTable)EDD.oneFromDatasetXml("nwioosAdcp2003")).toString());
//    String2.log(EDD.testDasDds("thierry")); 
//    String2.log(EDD.generateDatasetsXmlFromFiles("/u00/data/points/tao")); ///u00/data/points/tao
//    EDDGrid.verbose = true; 
//    EDDGrid.reallyVerbose = true; 
//    EDDGrid.suggestGraphMinMax();
//    EDDGrid.testWcsBAssta();
//    ((EDDGrid)EDD.oneFromDatasetXml("erdBAssta5day")).makeNewFileForDapQuery(null, null, 
//        "", 
//        "c:/downloads/", "erdBAssta5day", ".iso19115"); 
//String2.log("made " + ((EDDGrid)EDD.oneFromDatasetXml("ndbcHfrW2")).makeNewFileForDapQuery(null, null,
//      "u[(2010-10-06T12:00:00Z)][(36.07552):(37.37008)][(-122.9058):(-121.6148)]," +
//      "v[(2010-10-06T12:00:00Z)][(36.07552):(37.37008)][(-122.9058):(-121.6148)]" +
//      "&.draw=vectors&.vars=longitude|latitude|u|v&.color=0x000000",
//      "c:/downloads/", "screwy", ".transparentPng"));

    //try to validate ERDDAP's ISO19115 output in
    //http://www.validome.org/xml/validate/
    /*{
        String dirName = "c:/downloads/test.xml";
        Writer writer = new OutputStreamWriter(new FileOutputStream(dirName, false), "UTF-8");
        //EDD.oneFromDatasetXml("erdMHchla8day").writeFGDC(writer, null); 
        EDD.oneFromDatasetXml("erdMHchla8day").writeISO19115(writer, null); 
        //EDD.oneFromDatasetXml("pmelTaoDyAirt").writeFGDC(writer, null); 
        //EDD.oneFromDatasetXml("pmelTaoDyAirt").writeISO19115(writer, null); 
        writer.close();
        SSR.displayInBrowser("file://" + dirName);
    }*/

//    EDDGridAggregateExistingDimension.testUnpackedValidRange();
//    EDDGridAggregateExistingDimension.testGenerateDatasetsXml();
//      CCMP 
//      String2.log(EDDGridAggregateExistingDimension.generateDatasetsXml("hyrax",
          //there are alternatives to L3.5a
//          "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/contents.html", //flk (no llk data)
//            "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.0/data/flk/contents.html", //alternate start
//          "analysis.*flk\\.nc\\.gz",   //analysis, pentad or month    //flk (no llk data)
//          true, 1000000)); //recursive
//      String2.log(EDDGridAggregateExistingDimension.generateDatasetsXml("thredds",
//          "http://ourocean.jpl.nasa.gov:8080/thredds/dodsC/g1sst/catalog.xml",
//          ".*\\.nc", true, 120)); //recursive
//    String2.log(EDD.suggestDatasetID(
//      "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/contents.html?" +
//      "month.*flk\\.nc\\.gz"));
//    EDDGridAggregateExistingDimension.testRtofs();
//    EDDGridCopy.testBasic(true); //  defaultCheckSourceData 
//
//    String ftcName = EDStatic.fullLogsDirectory + "fromThreddsCatalog" + 
//        Calendar2.getCompactCurrentISODateTimeStringLocal() + ".xml";
//    EDDGridFromDap.generateDatasetsXmlFromThreddsCatalog(ftcName,
//        //one catalog.xml URL:
//          "http://oceanwatch.pfeg.noaa.gov/thredds/catalog/catalog.xml",
//          "http://thredds.jpl.nasa.gov/thredds/podaac_catalogs/AQUARIUS_L3_SMI_V20_catalog.xml", 
//          "http://opendap-uat.jpl.nasa.gov/thredds/catalog.xml",
//          "http://www.esrl.noaa.gov/psd/thredds/dodsC/Datasets/nodc.woa94/salt.mnltm.nc",
//        ".*", -1);
//    String2.log(String2.readFromFile(ftcName)[1]);
//
//    String2.writeToFile("/temp/urlsFromUAF.txt", 
//        String2.toNewlineString(EDDGridFromDap.getUrlsFromThreddsCatalog(
//            "http://ferret.pmel.noaa.gov/geoide/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/catalog/ncom/ncom_reg1_agg/catalog.html?dataset=ncom.ncom_reg1_agg.NCOM_Region_1_Aggregation_best.ncd", 
////            "http://opendap-uat.jpl.nasa.gov/thredds/catalog.xml", 
//            ".*", true)));
//
//    EDD.debugMode = true;
//    String2.log("\n" + EDDGridFromDap.generateDatasetsXml(false, //directions
//        "http://data1.gfdl.noaa.gov:8380/thredds3/dodsC/ipcc_ar4_CM2.0_R2_20C3M-0_annual_ocean_interp_18610101-20001231",
//         String2.readLinesFromFile("/temp/urlsFromUAF.txt", "", 1)[2342], //one URL
//         String2.readLinesFromFile("/temp/urlsFromPodaac.txt", "", 1)[161], //one URL
//        null, null, null, -1, null));
//        
//    crawl UAF clean catalog      
//      done 2012-10-17, 2012-12-09 new catalog, 2013-11-06, 2014-03-31, 2014-12-18
//      Results file is /temp/datasetsUAF{uafi}_{dateTime}.xml          
//      Log file is     /u00/cwatch/erddap2/logs/TestAll.log  /
//        Look at problems, creator_, title, .... Make improvements.
//        Look for "error", "unable to get axis", "unsorted axis", "no colorBarMin/Max", 
//            "ioos_category=Unknown for"
//        Sort it. 
//    EDDGridFromDap.testUAFSubThreddsCatalog(0); //entire official clean catalog  ~4hrs
//    EDDGridFromDap.testUAFSubThreddsCatalog(17);  //test one sub catalog
//    for (int uafi = 6; uafi < EDDGridFromDap.UAFSubThreddsCatalogs.length; uafi++) {
//        String2.log("\n\n************************************* UAFI=" + uafi);
//        EDDGridFromDap.testUAFSubThreddsCatalog(uafi);  //test one sub catalog
//    }
//    extract acronyms from a text file
//      Object ar[] = String2.findAcronyms(
//          String2.readFromFile("/Temp/datasetsUAF0_20150505161356.xml")[1]).toArray();
//      Arrays.sort(ar);
//      String2.log(String2.toNewlineString(ar)); 

//    EDDGridFromDap.testValidMinMax();
//    EDDGridFromErddap.testDataVarOrder(); 
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://coastwatch.pfeg.noaa.gov/erddap", 
//        true)); //keepOriginalID   (true for generating UAF datasets)
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://oceanview.pfeg.noaa.gov/erddap", true)); 
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://upwell.pfeg.noaa.gov/erddap")); 
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://oos.soest.hawaii.edu/erddap", true)); 
//    EDDGridFromNcFiles.testAwsS3(false);
//        String opt[] = {
//            "_BNU-ESM_","_CCSM4_","_CESM1-CAM5_","_CSIRO-Mk3-6-0_","_CanESM2_","_FGOALS-g2_",
//            "_FIO-ESM_","_GFDL-CM3_","_GFDL-ESM2G_","_GFDL-ESM2M_","_GISS-E2-R_","_HadGEM2-AO_","_IPSL-CM5A-LR_",
//            "_IPSL-CM5A-MR_","_MIROC-ESM-CHEM_","_MIROC-ESM_","_MIROC5_","_MPI-ESM-LR_","_MPI-ESM-MR_",
//            "_MRI-CGCM3_","_NorESM1-M_","_bcc-csm1-1_"};
//        //File2.delete("/Temp/AWSDatasets.txt");
//        for (int opti = 0; opti < opt.length; opti++) {
//            try {
//            String2.appendFile("/Temp/AWSDatasets.txt", 
//                EDDGridFromNcFiles.generateDatasetsXml(
//        "http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/", 
//        ".*" + opt[opti] + ".*\\.nc", 
//        "http://nasanex.s3.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/tasmin_amon_BCSD_rcp26_r1i1p1_CONUS" + opt[opti] + "200601-201012.nc",
//        1000000, null));
//            } catch (Throwable t) {
//                String2.appendFile("/Temp/AWSDatasets.txt", MustBe.throwableToString(t));
//            }
//        }

/*
    //Move CoastWatch ERDDAP datasets from THREDDS to new RAID files.
    String ds = "QS";    //2 letter dataset code  (exceptions: MPIC, MPOC)
    String vn = "uy10";  //4 letter var name, e.g., ssta, but "" for MPIC MPOC
    String cl = "m";  //composite length, e.g., h, 1, 3, 8, 14, m
    String tf = "QS1999213_1999243_uy10.nc"; //test .nc (or .ncml) fileName

    EDDGridFromNcFiles.generateDatasetsXmlCoastwatchErdMode = true;
    String tdir = "/u00/satellite/" + ds + "/" + 
        (vn.length() == 0? "" : vn + "/") + cl + "day/";
    String gx = EDDGridFromNcFiles.generateDatasetsXml(
        tdir, ".*_" + (vn.length() == 0? "4km\\.ncml" : vn + "\\.nc"), 
        tdir + tf, 1440, null);
    String2.log(gx);
    String2.setClipboardString(gx);    
    String2.pressEnterToContinue("Paste results into datasets2.xml, change destName and long_name.");
    
    File2.delete("/u00/cwatch/erddap2/dataset/ay/erd" + ds + vn + cl + "day/fileTable.nc");
    String2.log(EDD.testDasDds("erd" + ds + vn + cl + "day")); 
/* */
    //while (ds.length() > 0) {
    //    ds = String2.getStringFromSystemIn("datasetID?");
    //    String2.log(EDD.testDasDds(ds)); 
    //}

        /*         
        StringBuilder sb = new StringBuilder();
        //for (int i1 = 0; i1 < 4320; i1++) 
        //    sb.append(" " + (-89.979166666666666666666666 + i1 * 0.0416666666666666666));
        for (int i1 = 0; i1 < 8640; i1++) 
            sb.append(" " + (-179.979166666666666666666666 + i1 * 0.0416666666666666666));
        String2.setClipboardString(sb.toString());
        String2.log("clipboard was set");
        /* */
//    *** Daily
//    Projects.viirsLatLon(true); //create
    
//    String2.log(NcHelper.dumpString("c:/data/rutgers/NOAA_COOPS_WIND_STATIONS_SUBSET.nc", false));
//    String2.log(NcHelper.dumpString("/u00/data/points/eb/TrinidadHeadLine_CoralSea_CS140116.nc", false));
//    String2.log(String2.noLongLines(NcHelper.dumpString("/data/tao/sst0n147e_dy.cdf", "time"), 80, ""));
//Table table = new Table();
//table.readNDNc("/u00/data/points/taoOriginal/realtime/airt0n110w_dy.cdf",
               //"/u00/data/points/taoOriginal/daily/airt0n110w_dy.cdf", 
//    null, null, 0, 0, true);
//String2.log("nRows=" + table.nRows());
//table.removeRows(2, table.nRows() - 30); 
//String2.log(table.toCSVString());
//    String2.log(Projects.dumpTimeLatLon("/u00/data/viirs/MappedDaily4km/d4.ncml"));
//    String2.log(NcHelper.dumpString("/u00/data/viirs/MappedDaily4km/d42013074.ncml", false));
//    EDDGridFromNcFiles.testNcml();
      //make e.g., <netcdf location="V2013074.L3m_DAY_NPP_CHL_chlor_a_4km" coordValue="15779"/>
//    Projects.makeNcmlCoordValues("V*.L3m_DAY_NPP_CHL_chlor_a_4km", "2012-01-02", "2013-12-31", 1, Calendar.DAY_OF_YEAR); 
//    EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteThreddsFiles();
//    EDDGridFromNcFiles.testSpeed(-1);  //-1 for all
//    EDDGridSideBySide.testTransparentPng();

//    ((EDDTable)EDD.oneFromDatasetXml("erdGlobecBottle")).makeNewFileForDapQuery(null, null, 
//        "", 
//        "c:/downloads/", "erdGlobecBottle", ".iso19115"); 
//    EDDTable.testSosNdbcMet();
//    EDDTableCopy.testBasic();
      //for testPostTag, you have to change datasets2.xml to create separate
      //<dataset type="EDDTableFromPostDatabase" datasetID="testPostDet3">

      //*** To re-copy POST data, update post data, recache post data reload post data:  
      //In datasets2.xml, for cPostDet3, set checkSourceData TO *true* 
      //Delete the cPost* files       in c:/u00/cwatch/erddap2/datasetInfo
      //Delete the cPost* directories in c:/u00/cwatch/erddap2/copy
//    EDDTableCopyPost.copyPostSurg3(); //Get the data. Run this twice! get Issue #18 messages 2nd time.      
//    EDDTableCopyPost.copyPostDet3();  //Get the data. Run this twice! in case glitch in getting some tags.
      //In datasets2.xml, for cPostDet3, set checkSourceData TO *false* 
      //To release data to POST:  see notes.txt "release POST data"

//NEEDS WORK:
//    EDDTableCopyPost.testCopyPostDet3(false, true);  //test the data

      //Test the POST data     
      //Issues 1, 2, 3, 4, 5
      //2010-03-25 surg3 nTags=3471, det3 nTags=3467, in surg not in det: 1060334,5,6, and 3563
      //2010-04-18 Issues 1,2,3,4,5 are not a problem with this release.
      //2010-05-16 Issues 1,2,3,5 OK;  issue #4[?] has 100's of bad values
      //2010-07-05 Issue #1b (surgery lon=lat) has 100's of bad tags, issue #4 has 6 bad values (surg tags not found in det)
      //2010-07-11 Issues 1,2,3,4,5 OK
      //2010-08-17 Issues 1,2,3,5 OK.  4: thousands of tags have different surgery time in surg3 and det3 tables
      //2010-08-20 Issues 1,2,3,4,5 OK
      //2010-09-16 Issues 1,2,3,4,5 OK except detections for tag 146_A69-1005_ couldn't be downloaded. too much data? we removed it.
//    EDD.testVerboseOff(); EDDTableCopyPost.testForInconsistencies();
      //Issue #18 Invalid Name, Password, or Role
      //2010-05-16 POST: nValidUsers=27 nInvalidUsers=77  nValidRoles=52 nInvalidRoles=89
      //2010-07-13 many invalid passwords (usually length=0) and roles (no corresponding name) (OK)
      //2010-08-17 many invalid passwords (usually length=0) and roles (no corresponding name) (OK)
//    EDDTableCopyPost.testRole("310850_A69-1303_1065433");
      //a useful tool:
//    EDDTableCopyPost.testOneDetectionMap("21305_A69-1303_1052213");  //shows map and detection data
//    EDDTableCopyPost.printOneSurgery("25431_A69-1303_1068271"); 
      //2010-03-17 3716 tags; 2010-03-22 surg has 3471 tags, 3002 are public
      //2010-03-22 Jose says he set them aside for data quality issues, to be resolved later
      //2010-03-25 surg3 has 3471 tags
      //2010-04-11 surg3 public+private n unique_tag_id=11559 nDuplicates=64
      //2010-01-18? nTags=11020 nDuplicates=0
      //2010-05-16  loggedInAs= superuser? n=11017 nDuplicates=0
      //2010-07-05  loggedInAs= superuser? n=11579 nDuplicates=0
      //2010-07-11  loggedInAs= superuser? n=11388 nDuplicates=0
      //2010-08-17  loggedInAs= superuser? n=15100 nDuplicates=0
      //2010-08-20  loggedInAs= superuser? n=15024 nDuplicates=0
      //2010-09-16  loggedInAs= superuser? n=15023 nDuplicates=0
      //2010-09-19  loggedInAs= superuser? n=15023 nDuplicates=0
//    EDD.testVerboseOff(); EDDTableCopyPost.findTags(EDStatic.loggedInAsSuperuser, true); 
      //Issue #6   (Jose says nNaN is unfixable - PIs didn't provide surgery time)
      //2010-03-17 nNaN=654 nBefore=0  
      //2010-03-22 nNaN=519 nBefore=0   same on 2010-03-25
      //2010-04-11 nNaN=1667 nBefore=2455
      //2010-01-18? nNaN=1428 nBefore=0
      //2010-05-16 nNaN = 678 nBefore=0  but 100's have have >=2 surgery times.
      //2010-07-05 nNaN = 1753 nBefore=0   (I don't see any >=2 surgery times)
      //2010-07-11 nMultiple=0 nNaN=1568 nBefore=0
      //2010-08-17 nMultiple=1948 nNaN=3332 nBefore=0
      //2010-08-20 nMultiple=0    nNaN=1562 nBefore=0
      //2010-09-16 nMultiple=0    nNaN=1562 nBefore=0
      //2010-09-20 nMultiple=0    nNaN=1562 nBefore=0
//      EDD.testVerboseOff(); EDDTableCopyPost.findDetectionBeforeSurgery(EDStatic.loggedInAsSuperuser); 
      //Issue #7
      //2010-03-17 nRows=3716  nSurgNaN=654 nActNaN=23 nA>S=0
      //2010-03-22 nRows=3471  nSurgNaN=523 nActNaN=6 nA>S=0   same on 2010-03-25
      //2010-04-11 nRows=      nSurgNaN=1671 nActNaN=276 nA>S=183
      //2010-04-18 nRows=11020 nSurgNaN=1428 nActNaN=181 nA>S=0
      //2010-05-16 nRows=11017 nSurgNaN=1427 nActNaN=181 nA>S=0
      //2010-07-05 nRows=11579 nSurgNaN=1756 nActNaN=214 nA>S=0
      //2010-07-11 nRows=11388 nSurgNaN=1568 nActNaN=213 nA>S=0
      //2010-08-17 nRows=15100 nSurgNaN=1579 nActNaN=262 nA>S=0
      //2010-08-20 nRows=15024 nSurgNaN-1562 nActNaN=261 nA>S=0
      //2010-09-16 nRows=15023 nSurgNaN-1562 nActNaN=260 nA>S=0
      //2010-09-19 nRows=15023 nSurgNaN-1562 nActNaN=260 nA>S=0
      //2010-09-20 nRows=15023 nSurgNaN-1562 nActNaN=260 nA>S=0
//      EDD.testVerboseOff(); EDDTableCopyPost.findSurgeryBeforeActivation(EDStatic.loggedInAsSuperuser);        
      //differentDataPublic is just my curiousity. It isn't an issue. Don't report to Jose
      //2010-03-22 0   same on 2010-03-25
      //2010-04-12 n date_public=NaN = 0    n >1 date_public = ~30 but only ~5 are significantly different
      //2010-04-18 n date_public=NaN = 0  n >1 date_public = 0
      //2010-05-16 n date_public=NaN = 0  n >1 date_public = 0
      //2010-07-05 n date_public=NaN = 0  n >1 date_public = 4591
      //2010-08-17 n date_public=NaN = 0  n >1 date_public = 0
      //2010-08-20 n date_public=NaN = 0  n >1 date_public = 0
      //2010-09-16 n date_public=NaN = 0  n >1 date_public = 677
      //2010-09-19 n date_public=NaN = 0  n >1 date_public = 272
      //2010-09-20 n date_public=NaN = 0  n >1 date_public = 0
//      EDD.testVerboseOff(); EDDTableCopyPost.findDifferentDatePublic(EDStatic.loggedInAsSuperuser);
      //Issue #8 Fast Swimmers 
      //   Issue #9,10 out >4 years and returned, and suspicious
      //   Issue #17 (Young) Fast Swimmers 
      //2010-04-12 nFastSwimmers = 34
      //2010-04-19 nFastSwimmers = 47  nYoungFastSwimmers = 930       
      //2010-05-18 nFastSwimmers = 80  nYoungFastSwimmers = 754
      //2010-07-05 nFastSwimmers = 130 nYoungFastSwimmers = 622
      //2010-07-12 nFastSwimmers = 112 nYoungFastSwimmers = 620
      //2010-08-17 nFS=75  nYFS=763, nLDist DIDN'T=1020 DID=56, nLTime DIDN'T=26 DID=7, nLDistTime DIDN'T=24 DID=7
      //2010-08-20 nFS=0   nYFS=714  nLDist DIDN'T=980  DID=43, nLTime DIDN'T=24 DID=7, nLDistTime DIDN'T=22 DID=7
      //2010-09-16 nFS=0   nYFS=714  nLDist DIDN'T=980  DID=43, nLTime DIDN'T=24 DID=7, nLDistTime DIDN'T=22 DID=7
      //2010-09-20 nFS=0   nYFS=714  nLDist DIDN'T=980  DID=43, nLTime DIDN'T=24 DID=7, nLDistTime DIDN'T=22 DID=7
//    EDD.testVerboseOff(); EDDTableCopyPost.findFastSwimmers(EDStatic.loggedInAsSuperuser, 2.0, 5, 4);  //deg/day, longDegrees, longYears
      //interesting, test >3 years
//    EDD.testVerboseOff(); EDDTableCopyPost.findFastSwimmers(EDStatic.loggedInAsSuperuser, 2.0, 5, 3);  //deg/day, longDegrees, longYears
      //Issue #11 Bad surgery3 release_longitute (>-110); test by looking at surgery3 .subset
      //2010-04-13?  many
      //2010-04-18 nBad=0
      //Issue #12 Test If Surgery Data Is Detection First Row
      //2010-04-13 nAFTER=4165  nMissing=373, nBEFORE=6247, nLat=686, nLon=0, nOK=147 
      //2010-04-18 nTags=11020 nAfterGT1day=1751 nAFTER=1751 nMissing=0 nBEFORE=6420 nLat=1815 nLon=0  nOK=1034
      //2010-05-16 nTags=11017 nAfterGT1day=0    nNaN=0  nNoDetection=0 nBEFORE=0    nLat=19   nLon=0  nOK=10998
      //2010-07-05 nTags=11579 nAfterGT1day=0    nNaN=0  nNoDetection=6 nBEFORE=0    nLat=0    nLon=0  nOK=11573
      //2010-07-12 nTags=11388 nNaN=0 nAfter=0 nBefore=0 nLat=0 nLon=214 nNoDetection=0 nOK=11174
//***??? does this test lon=lat?
      //2010-08-17 nTags=15100 nNaN=0 nAfter=0 nBefore=0 nLat=0 nLon=0 nNoDetection=1 nOK=15099
      //2010-08-20 nTags=15024 nNaN=0 nAfter=0 nBefore=0 nLat=0 nLon=0 nNoDetection=0 nOK=15024
      //2010-09-16 nTags=15023 nNaN=0 nAfter=0 nBefore=0 nLat=0 nLon=0 nNoDetection=0 nOK=15022 //1 bad temp file, probably fluke, for 2296_A69-1204_1030750
      //2010-09-20 nTags=15023 nNaN=0 nAfter=0 nBefore=0 nLat=0 nLon=0 nNoDetection=0 nOK=15023
//    EDD.testVerboseOff(); EDDTableCopyPost.testIfSurgeryDataIsDetectionFirstRow();
      //Issue #13,14,15 Leading/trailing spaces and Uncapitalized Data
      //2010-04-13 several values
      //2010-04-18 several values [I didn't check, but I think these results are the same as last time.]
      //2010-05-16 several values [I didn't check, but I think these results are the same as last time.]
      //2010-07-05 several values (I think some are different. space at start of PI name!)
      //2010-07-12 several values. I added test: none have space at start, but 2 have space at end!
      //2010-08-17 nWithSpaces=2  nUncapitalized=3
      //2010-08-20 nWithSpaces=0  nUncapitalized=0
      //2010-09-16 nWithSpaces=0  nUncapitalized=0
//    EDD.testVerboseOff(); EDDTableCopyPost.testLowerCase();
      //Issue #24 Test absolute time.
      //2010-07-13 Fail!
      //2010-08-17 nPass=5 nFail=0
      //2010-09-16 nPass=5 nFail=0
//      EDD.testVerboseOff(); EDDTableCopyPost.testAbsoluteTime();
      //*** The following tests are NOT routinely done.
      //This shows me *lots* of maps
      //2010-05-24 some already reported, 1 new unlikely, 1 new impossible
//    EDD.testVerboseOff(); EDDTableCopyPost.findUpstream(EDStatic.loggedInAsSuperuser);
      //This shows me *lots* of maps
      //2010-05-25 9 found;  oddest is 1779D (1 detection:  degrees= 20.719324 years=5.7820168)
//    EDD.testVerboseOff(); EDDTableCopyPost.findLongest(EDStatic.loggedInAsSuperuser, 1000, 5);  //degrees, years
      //This shows me *lots* of maps
      //2010-05-25 many. Interesting: lots with initial detection(s), then silence, then 1 in AK
//    EDD.testVerboseOff(); EDDTableCopyPost.findLongest(EDStatic.loggedInAsSuperuser, 5, 1000);  //degrees, years
      //no longer used here?
//    EDD.testVerboseOff(); EDDTableCopyPost.findPublicTags(null, false);  //call this with 'true' if new data

//EDDTableCopyPost.run(-1); //-1=allTests, 0..6

//    String2.log(EDDTableFromAsciiFiles.generateDatasetsXml(
//        "/u00/data/points/LiquidR_HBG3_2015/", "weather.csv",
//        "/u00/data/points/LiquidR_HBG3_2015/weather.csv",
//        "", 1, 2, -1,
//        "", "", "", "", "",
//        "", // tSortFilesBySourceNames, 
//        "", "", "", "", null));
//    EDDTableFromAsciiFiles.testBasic2();
//    EDDTableFromAsciiServiceNOS.testNosCoops(".*"); //".*", "nosCoopsWLTP60");  //a regex
//    EDDTable gtspp = (EDDTable)EDD.oneFromDatasetXml("pmelGtsppa");
//        gtspp.getEmpiricalMinMax(null, "2005-06-01", "2005-06-08", false, false);
//    EDDTableFromAwsXmlFiles.testGenerateDatasetsXml();
//    EDDTableFromAwsXmlFiles.testBasic(true);
//String2.log(">>> " + Calendar2.epochSecondsToLimitedIsoStringT("1970-01-01Z", 123456789, ""));
//    EDDTableFromCassandra.testGenerateDatasetsXml(); 
//    EDDTableFromCassandra.testBasic(false); //pauseBetweenTests to read the stats
//    EDDTableFromCassandra.testMaxRequestFraction(false);
//    EDDTableFromCassandra.testCass1Device(false); //pauseBetweenTests to read the stats
//    String2.log(EDDTableFromDapSequence.generateDatasetsXml(
//        "http://www.ifremer.fr/oceanotron/OPENDAP/INS_CORIOLIS_GLO_TS_NRT_OBS_PROFILE_LATEST",
//        "http://opendap.co-ops.nos.noaa.gov/dods/IOOS/Raw_Water_Level", 
//        "http://gisweb.wh.whoi.edu:8080/dods/whoi/drift_data",
//        180, null));
//    EDDTableFromDapSequence.testArgo();
//    EDDTableFromDatabase.testNonExistentTable();

//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://coastwatch.pfeg.noaa.gov/erddap", true)); //keep original datasetID?
//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://oceanview.pfeg.noaa.gov/erddap", true)); 
//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://oos.soest.hawaii.edu/erddap", false)); //keep original datasetID?
//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://osmc.noaa.gov/erddap", true)); //keep original datasetID?
//    EDDTableFromErddap.testApostrophe();
//    EDDTableFromEDDGrid.testTableFromGriddap();
//    EDDTableFromFiles.testIsOK();
//      String2.log(EDDTableFromFileNames.generateDatasetsXml(
//        "http://nasanex.s3.amazonaws.com/", ".*", true, 
//        10080, 
//        "","","","",null));
//    String2.log(EDDTableFromHyraxFiles.generateDatasetsXml(
//        "http://data.nodc.noaa.gov/opendap/wod/monthly/APB/201103-201103/", 
//        "wod_01345934.O\\.nc", 
//        "http://data.nodc.noaa.gov/opendap/wod/monthly/APB/201103-201103/wod_013459340O.nc", 
//        10080, 
//        "", "", "", "",  //columnFromFileName
//        "time", //String tSortedColumnSourceName,
//        "time", //tSortFilesBySourceNames,
//        null)); //externalAddAttributes) 
//    EDDTableFromHyraxFiles.testJpl(true); //deleteCachedInfoAndOneFile

//    String2.log(EDDTableFromNcCFFiles.generateDatasetsXml(
//        "/u00/data/points/eb/", "TrinidadHeadLine_CoralSea_.*\\.nc", 
//        "/u00/data/points/eb/TrinidadHeadLine_CoralSea_CS140116.nc", 1440, 
//        "", "", "", 
//        "", "", 
//        "", "", "", "", new Attributes()));
//    EDDTableFromNcCFFiles.testNoAttName(); 
//
//    Table.debugMode = true;
//    Table tTable = new Table();
//        tTable.readNDNc(fileDir + fileName, sourceDataNames.toArray(),
//            sortedSpacing >= 0 && !Double.isNaN(minSorted)? sortedColumnSourceName : null,
//                minSorted, maxSorted, 
//            getMetadata);

//String2.log(tTable.toCSVString());
//
//    EDDTableFromNcFiles.testNewTime(); 
//    NOT FINISHED  EDDTableFromNcFiles.bobConsolidateWOD("APB", "1960-01-01"); 
//      EDDTableFromNcFiles.getAllSourceVariableNames(
//          "c:/data/wod/monthly/APB/", ".*\\.nc"); //201103-201103/
//    Table.verbose = false;
//    Table.reallyVerbose = false;
//    EDDTableFromNcFiles.displayAttributeFromFiles(
//        "c:/u00/cwatch/erddap2/copy/fsuResearchShipVLHJ/", 
//        ".*\\.nc", 
//        new String[]{   
//            "PL_WDIR",  "SPD",  "PL_WSPD",  "DIR",  "P",  "T",  "TS",  "RH",  "PRECIP",  "RRATE", 
//            "PL_WDIR2", "SPD2", "PL_WSPD2", "DIR2", "P2", "T2", "TS2", "RH2", "PRECIP2", "RRATE2",
//            "PL_WDIR3", "SPD3", "PL_WSPD3", "DIR3", "P3", "T3", "TS3", "RH3", "PRECIP3", "RRATE3"},
//        "long_name");
//    String2.log(EDDTableFromNcFiles.generateDatasetsXml(
//        "/u00/data/points/eb/", "TrinidadHeadLine_CoralSea.*\\.nc", 
//        "/u00/data/points/eb/TrinidadHeadLine_CoralSea_CS140116.nc", 
//        "", 1440, 
//        "", "", "", 
//        "", "", 
//        "", 
//        "", "", "", "", new Attributes()));
        //String tFileDir, String tFileNameRegex, String sampleFileName, int tReloadEveryNMinutes,
        //String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        //String tColumnNameForExtract, String tSortedColumnSourceName,
        //String tSortFilesBySourceNames, 
        //String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        //Attributes externalAddGlobalAttributes) throws Throwable {

//    *** To update GTSPP (~10th of every month):
      //Don't add source_id or stream_ident: they are usually (always?) empty
//    1) (overnight?) Use FileZilla to download newly changed files 
//      from ftp.nodc.noaa.gov (name=anonymous  pwd=bob.simons@noaa.gov)
//      from GTSPP dir: /pub/gtspp/best_nc to my local: c:/data/gtspp/bestNcZip
//      !!! Note that older files are reprocessed sometimes. 
//      !!! So sort by lastModified time to check if "older" files have a recent last-modified-time.
//    2) Overnight (still! because it's still sluggish and programming interrupts the log file), 
//       unzip and consolidate the profiles 
//       (full run takes 36 hours(?) on Dell M4700, was 2 days 14 hours on old Dell Opti).
//       Great speed up, but no longer under my control:
//         Temporarily switching off parts of McAfee : Virus Scan Console  (2X speedup!)
//           On Access Scanner : All Processes
//             Scan Items: check: specified file types only (instead of usual All Files) 
//     EDDTableFromNcFiles.bobConsolidateGtsppTgz(2012, 12, 2015, 7, false);  //first/last year(1990..)/month(1..), testMode
//       log file is c:/data/gtspp/log.txt 
//      2b) Email the "good" but "impossible" stations to Charles Sun
//       [was Melanie Hamilton, now retired]
//       [start in 2011? but not longer valid 2012-10-19 Meilin.Chen@noaa.gov]
//      2c) Undo changes to McAfee scanner
//    3) Update the dates 
//       (2 in defaultGraphQuery, 2 in history, 1 in summary) for erdGtsppBestNc in datasets2.xml 
//       and the 2 history dates for erdGtsppBest in datasets2.xml and datasetsFEDCW.xml
//       to the date I started processing in step 2 above. (If in chunks, date of start of last chunk.)
//    4) * In [tomcat]/content/erddap/subset/
//          delete erdGtsppBestNc.json and erdGtsppBest.json
//       * Load erdGtsppBestNc in localHost ERDDAP.  (long time if lots of files changed)
//       * Generate .json file from
//         http://127.0.0.1:8080/cwexperimental/tabledap/erdGtsppBestNc.json?trajectory,org,type,platform,cruise&distinct()
//         and save it as [tomcat]/content/erddap/subset/erdGtsppBestNc.json
//       * Reload ERDDAP to ensure it loads quickly.
//    5) Run and update this test:
//       //one time: File2.touch("c:/data/gtspp/bestNcConsolidated/2011/09/2011-09_0E_0N.nc"); //one time
//       //one time: EDDTableFromNcFiles.bobFindGtsppDuplicateCruises();
//       EDDTableFromNcFiles.testErdGtsppBest("erdGtsppBestNc");
//    6) Create ncCF files with the same date range as 2a) above: 
//       !!!! HIDE THE WINDOW !!! IT WILL RUN MUCH FASTER!!!  takes ~2 minutes per month processed
//       EDDTableFromNcFiles.bobCreateGtsppNcCFFiles(2012, 12, 2015, 7); //e.g., first/last year(1990..)/month(1..)
//       String2.log(NcHelper.dumpString("/u00/data/points/gtsppNcCF/201406a.nc", false));
//    7) * Load erdGtsppBest in localHost ERDDAP.  (long time if lots of files changed)
//       * Generate .json file from
//         http://127.0.0.1:8080/cwexperimental/tabledap/erdGtsppBest.json?trajectory,org,type,platform,cruise&distinct()
//         and save it as [tomcat]/content/erddap/subset/erdGtsppBest.json
//       * Reload ERDDAP to ensure it loads quickly.
//    8) Test the .ncCF dataset:
//       EDDTableFromNcFiles.testErdGtsppBest("erdGtsppBest");
//    9) If copying all to coastwatch, temporarily rename dir to /u00/data/points/gtsppNcCFtemp/
//       * Copy the newly consolidated .ncCF files
//         from laptop   /u00/data/points/gtsppNcCF/
//         to coastwatch /u00/data/points/gtsppNcCF/
//       * Copy from local     [tomcat]/content/erddap/subset/erdGtsppBest.json
//              to coastwatch  [tomcat]/content/erddap/subset/erdGtsppBest.json
//              to   upwell [UAFtomcat]/content/erddap/subset/erdGtsppBest.json
//    10) Update rtofs (Python/ #2=updateDatasetsXml.py), 
//        then copy datasetsFEDCW.xml to coastwatch and rename to datasets.xml
//    11) Ping the gtspp flag url on ERDDAP (it is in "flag" bookmarks)
//       http://coastwatch.pfeg.noaa.gov/erddap/setDatasetFlag.txt?datasetID=erdGtsppBest&flagKey=2369414249
//       and make sure the new data and metadata are visible (hence, new dataset has loaded)

//    String2.log(EDDTableFromSOS.generateDatasetsXml(
//        "http://data.gcoos.org:8080/52nSOS/sos/kvp", "1.0.0", "IOOS_52N"));
//    EDDTableFromSOS.testErddapSos();

//** To update pmelTAO data on/after 9am 2nd day of every month:
// In datasets2.xml, see MONTHLY TAO UPDATE 
//??? Test lots of things, including: are there new stations?  (about 2 minutes)
//     Always takes longer than I expect (because datasets ftp latest data when they load?).
//     Daily is longest.   30 minutes total for all datasets?
//Email ERDDAP log info ("tabledap DatasetID (since startup)") for pmelTao.* datasets
//   (and the Current Time and Startup Time) to Dai.C.Mcclurg@noaa.gov
//ERDDAP TAO monthly update
//I ftp downloaded all of the TAO cdf/sites data files yesterday 12:09 through 13:44 PST. I processed them this morning and installed them on ERDDAP's computer. Each dataset's history metadata now indicates that the data was completely refreshed yesterday.
//
//The usage statistics below from ERDDAP cover this range of time:
//Startup was at  ??? local time
//Current time is ??? local time
//
//If you have any questions, please let me know.
//Best wishes.
//
//The page hits and data requests are:
//[from "tabledap DatasetID (since startup)"]


//String2.log(EDDTableFromThreddsFiles.generateDatasetsXml(
//        "http://tds.gliders.ioos.us/thredds/catalog/North-Caroline-State-University_salacia-20130916T1603_Files/catalog.xml",
//        "salacia.*\\.nc", 
//        "http://tds.gliders.ioos.us/thredds/dodsC/North-Caroline-State-University_salacia-20130916T1603_Files/salacia-20131003T114710_rt0.nc",
//        1440, "", "", "", "", "",
//        "Time", null)); 
//    EDDTableFromThreddsFiles.testShipWTEP(false); //deleteCachedInfo
//    EDDTableFromWFSFiles.testGenerateDatasetsXml();

//    EDDTableReplicate.testReplicatePostDet(false);  //defaultCheckSourceData (faster)
//    EDStatic.test();
//    EDUnits.testUdunitsToUcum();
//    String2.log(File2.hexDump("c:/downloads/sendaiFail.dods", 1000000000));

      //Run the GenerateDatasetsXml program in interactive mode:
//    GenerateDatasetsXml.main(null);
/*
         EDDGridAggregateExistingDimension.testGenerateDatasetsXml();  //after EDDGridFromDap
         EDDGridFromDap.testGenerateDatasetsXml();
         EDDGridFromDap.testGenerateDatasetsXml2(); 
         //EDDGridFromDap.testGenerateDatasetsXml3(); //source is gone
         EDDGridFromEDDTable.testGenerateDatasetsXml();  
         EDDGridFromErddap.testGenerateDatasetsXml();  
         EDDGridFromMergeIRFiles.testGenerateDatasetsXml();  
         EDDGridFromNcFiles.testGenerateDatasetsXml();
         EDDGridFromNcFiles.testGenerateDatasetsXml2();
         EDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3();
         EDDTableFromAsciiFiles.testGenerateDatasetsXml();
         EDDTableFromAwsXmlFiles.testGenerateDatasetsXml();
         EDDTableFromCassandra.testGenerateDatasetsXml();
         EDDTableFromColumnarAsciiFiles.testGenerateDatasetsXml();
         EDDTableFromDapSequence.testGenerateDatasetsXml();
         EDDTableFromDatabase.testGenerateDatasetsXml();
         EDDTableFromErddap.testGenerateDatasetsXml(); 
         EDDTableFromFileNames.testGenerateDatasetsXml(); 
         EDDTableFromFileNames.testGenerateDatasetsXmlAwsS3(); 
         EDDTableFromHyraxFiles.testGenerateDatasetsXml(); 
         //EDDTableFromHyraxFiles.testGenerateDatasetsXml2(); //not yet working
         EDDTableFromNcCFFiles.testGenerateDatasetsXml();
         EDDTableFromNcFiles.testGenerateDatasetsXml();
         EDDTableFromNcFiles.testGenerateDatasetsXml2();
         //EDDTableFromNWISDV.testGenerateDatasetsXml(); //inactive
         EDDTableFromOBIS.testGenerateDatasetsXml();
         EDDTableFromSOS.testGenerateDatasetsXml(true); //useCachedInfo); 
         EDDTableFromSOS.testGenerateDatasetsXmlFromOneIOOS(true); //useCachedInfo); 
         EDDTableFromSOS.testGenerateDatasetsXmlFromIOOS(true); //useCachedInfo); 
         EDDTableFromThreddsFiles.testGenerateDatasetsXml();
         EDDTableFromWFSFiles.testGenerateDatasetsXml();
/* */

//    String2.log(EDDTableFromThreddsFiles.generateDatasetsXml(
//        "http://coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/2012/catalog.xml", 
//          "WTEP_20120215.*",
//          "http://coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2012/WTEP_20120215v10002.nc",
//        "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/catalog.xml",
//          "BodegaMarineLabBuoyCombined.nc",
//          "http://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/BodegaMarineLabBuoyCombined.nc",
//        60,
//        "", "", "", "", "",
//        "time", null)); 

//    Erddap.makeErddapContentZip("c:/programs/tomcat/samples/", "c:/backup/");
//    Erddap.testHammerGetDatasets();
//    File2.touch("c:/u00/cwatch/erddap2/copy/nmspWcosTemp/ANO001/2005/ANO001_021MTBD020R00_20051105.nc");
//    FileVisitorDNLS.testHyrax();
//    FileVisitorDNLS.testThredds();
//    FileVisitorSubdir.testAWSS3();
      //FishBase datasets
//    FishBase.convertHtmlToNc("ABNORM");

//    Grid.davesSaveAs
//        String source = "c:/u00/satellite/MB/sstd/5day/MB2006301_2006305_sstd.nc"; //Xmx500 ok; 300 not ok
//        //String dest   = "C:/temp/MB2006301_2006305_sstd.nc";
//        //String source = "c:/u00/satellite/AT/ssta/1day/AT2006005_2006005_ssta.nc";
//        //String dest   = "C:/temp/AT2006005_2006005_ssta.nc";
//        String source = "c:/data/kevin/interpolated_gld.20120620_045152_meta_2.nc";
//        String source = "C:/data/tao/sites/daily/airt2s125w_dy.cdf";
//        String dest   = "C:/temp/CM2006171_230000h_u25h.nc";        
//        String2.log(NcHelper.dumpString(source, false));
//        Grid.davesSaveAs(new String[]{source, dest}, new FileNameUtility("gov.noaa.pfel.coastwatch.CWBrowser"));
//        String2.log(NcHelper.dumpString(dest, false));
//    Grid.testReadGrdSubset();
//    GridDataSetThredds.quickTest("GA", "ssta");
//    GridSaveAs.main(new String[]{"c:/u00/data/SC/mday/grd/.grd.zip", "C:/u00/data/SC/mday/nc/.nc"});
//    String2.log(File2.hexDump("C:/programs/gshhs/2009v7/gshhs_c.b", 1000));
//    GSHHS.test();

//    String2.log("ImageIO Readers: " + String2.toCSSVString(ImageIO.getReaderFormatNames()) +
//        "\nImageIO Writers: " + String2.toCSSVString(ImageIO.getWriterFormatNames()));
//    LRUCache.test();
//    MakeErdJavaZip.makeCwhdfToNcZip();
//
//    NDBC MONTHLY UPDATES.   NEXT TIME: be stricter and remove 99.9 and 98.7 data values.  
//      !!!check pxoc1. make historic file if needed.
//    NdbcMetStation.main(null); 
//    String2.log(NcHelper.dumpString("C:/data/socat/06AQ20110715.nc", 
//        false)); //print data
//    NcHelper.testSequence();
//    NcHelper.testUnlimited();
//    String2.log(NcHelper.dumpString("c:/downloads/MLMLseawater.nc", false)); //false=don't print data
//    String2.log(NcHelper.dumpString("/u00/data/viirs/MappedDaily4km/d4.ncml", false)); 
//    String2.log(NcHelper.dumpString("c:/programs/seadas/MODIS.2007219.074906.gcoos.seadas_sst.hdf", false));
//    NcHelper.test();
//    NcHelper.testJplG1SST();
//    String2.log(NcHelper.dds("c:/data/nodcTemplates/pointKachemakBay.nc"));
//    test validity of a file:
//        NetcdfDataset ncd = NetcdfDataset.openDataset( //file or DAP baseUrl
//            "/data/VH2/par/1day/V2014131.L3m_DAY_NPP_PAR_par_4km.nc");
//        System.out.println("netcdfDataset=" + ncd.toString());
//        System.out.println("featureType=" + FeatureDatasetFactoryManager.findFeatureType(ncd).toString());
//        ncd.close();
//    NetCheck.verbose = true;
//    NetCheck nc = new NetCheck("c:/content/bat/NetCheck.xml", true); //testmode
//    OpendapHelper.testDapToNcDArray();
//    Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
//        Matcher matcher = pattern.matcher("test 2009-01-02 12:13:14abcaaaaab");
//        if (matcher.find(1)) String2.log("matched at start=" + matcher.start());
//        else String2.log("didn't match");
//    post.TestJdbc.test();
//    PrimitiveArray.testTestValueOpValue();
//    DasDds.main(new String[]{"erdMWpp3day", "-verbose"});
//    Projects.dapToNc("http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdBAsstamday", 
//        new String[]{"sst"}, "[112:112][0:0][750:760][1800:1820]",
//        "c:/downloads/test.nc");
//    Determine the last date for each of the erd.* gridded datasets in ERDDAP:
//      The list of datasets in the file below is from 
//      http://coastwatch.pfeg.noaa.gov/erddap/search/advanced.html?searchFor=datasetid%3Derd&protocol=griddap
//      then all but last field removed by an EditPlus recorded tool
//    Projects.getTabularFileVarNamesAndTypes(
//        "/u00/data/points/tao/daily/", "airt.*_dy\\.cdf");
//    Projects.lastTime("http://coastwatch.pfeg.noaa.gov/erddap/griddap/",
//      StringArray.fromFile("c:/content/scripts/erdGridDatasets.csv"));

/* 
    //Run to update jplG1SST  
    String2.log("\n*** jplG1SST update");
    String localDir = "c:/data/jplG1SST/";
    String jplFileUrl = "http://ourocean.jpl.nasa.gov/thredds/dodsC/g1sst/";
    for (int tr = 0; tr < Integer.MAX_VALUE; tr++) {
        try {    
            GregorianCalendar gc = new GregorianCalendar();
            String2.log("\nupdate jplG1SST " + Calendar2.formatAsISODateTimeT(gc));

            //get the jpl file list
            String jplList = SSR.getUrlResponseString(jplFileUrl);

            //get the local file list
            String localList[] = (new File(localDir)).list();

            //check the last n (7?) days
            int lastN = 7;
            gc.add(Calendar2.DATE, -(lastN+1));
            for (int d = 0; d < lastN; d++) {
                gc.add(Calendar2.DATE, 1);
                String date = Calendar2.formatAsCompactDateTime(gc).substring(0, 8);
                String fileName = "sst_" + date + ".nc";
                //String2.log("  test date=" + date);
                if (String2.indexOf(localList, fileName) >= 0) {
                    //String2.log("    local file already exists");
                    continue;
                }

                if (jplList.indexOf(fileName) >= 0) {
                    //String2.log("    should download");                    
                    try {
                        //get the file
                        OpendapHelper.dapToNc(jplFileUrl + fileName, 
                            new String[]{"SST"}, "[0][0:15999][0:35999]",
                            localDir + fileName, true);
                    } catch (Throwable t) {
                        String2.log(MustBe.throwableToString(t));
                    }
                }
            }
        } catch (Throwable t) {
            String2.log(MustBe.throwableToString(t));
        }

        String2.log("  sleeping for 30 minutes");
        Math2.sleep(30 * Calendar2.MILLIS_PER_MINUTE);
    }
    /* */
            //NOT WORKING  ftp the file to upwell
            //String commands = 
            //    "cd /Volumes/ServerStorage/u00/data/points/jplG1SST\n" +
            //    "lcd c:\\data\\jplG1SST\n" +
            //    "put " + jplFiles[jf];
            //SSR.sftp("upwell.pfeg.noaa.gov", "ERDadmin", password, commands);
     

    // set jplG1SST flags !!!!! 
    //SSR.touchUrl(
    //    "http://upwell.pfeg.noaa.gov/erddap/setDatasetFlag.txt?datasetID=jplG1SST&flagKey=1879976078",
    //    60000);
    
    //while email systems are down...
    //Math2.sleep(60000);
    //SSR.touchUrl(
    //  "http://coastwatch.pfeg.noaa.gov/erddap/setDatasetFlag.txt?datasetID=jplG1SST&flagKey=336447934",
    //  60000); 
    
    //Math2.sleep(60000);
    //SSR.touchUrl(
    //    "http://75.101.155.155/erddap/setDatasetFlag.txt?datasetID=jplG1SST&flagKey=3057856376",
    //    60000);

//    Projects.erddapTunnelTest();
//    Projects.makeNetcheckErddapTests(
//        "http://coastwatch.pfeg.noaa.gov/erddap/");
//        "http://upwell.pfeg.noaa.gov/erddap/");
//        "http://75.101.155.155/erddap/");
//     :8081 led to out-of-date oceanwatch dataset!!  but now disabled
//    Projects.testOpendapAvailability("http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday", 
//        "CMusfc", 5, 1, true); //nIter, maxSec
//    Projects.touchUrls();
//    Projects2.copyKeywords();

//String2.log(String2.extractRegex("abc>2011-06-30T04:43:09<def",
//      ">\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}<", 0));
    //">\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}<", 0));

//    WOD
//                    0      1      2      3      4      5      6      7      8      9
//    String dirs[] = {"APB", "CTD", "DRB", "GLD", "MBT", "MRB", "OSD", "PFL", "UOR", "XBT"};
//    for (int i = 9; i < 10; i++)
//        Projects2.copyHyraxFiles(
//            "http://data.nodc.noaa.gov/opendap/wod/" + dirs[i] + "/", 
//            ".*\\.nc", 
//            "c:/data/wod/monthly/" + dirs[i] + "/",
//            "c:/data/wod/copyHyraxFiles20110713b.log"); 

//    Projects2.nodcWOD(
//        "c:/data/wod/monthly/APB/201103-201103/",  //files curl'd from source
//        "c:/data/wod/flat/APB/");  //destination

//    String2.log(String2.toNewlineString(RegexFilenameFilter.fullNameList("c:/temp/incoming/", ".+hdf")));
//    SaveOpendap.downloadMbariData();
//    SgtGraph.testForMemoryLeak();
//    SgtMap.createBathymetryMatlabFile(-135, -114, 29, 50, 0.025, "c:/temp/luke/");
//    SgtMap.main(new String[]{"c:/temp/cwsamples/2008_112_34E.nc"});

//SSR.downloadFile(url...,
//    toFile "c:/data/tao/response.html", false);

//    SSR.displayInBrowser("file://" + tName);
//    for (int pe = 0; pe < 1000000; pe++) {
//        long peTime = System.currentTimeMillis();
//        SSR.downloadFile(
//            //"http://coastwatch.pfeg.noaa.gov/erddap/tabledap/cwwcNDBCMet.pngInfo",
//            "http://oceanwatch.pfeg.noaa.gov/thredds/catalog/Satellite/aggregsatMY/k490/catalog.xml",
//            //"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QS/vekm/7day.das",
//            "c:/downloads/peTest", true);
//        String2.log("Attempt #" + pe + " time=" + (System.currentTimeMillis() - peTime));
//    }
//    SSR.downloadFile("",
//            String fullFileName, boolean tryToUseCompression);
//    String2.log(SSR.getUrlResponseString(
//        "http://coastwatch.pfeg.noaa.gov/erddap/tabledap/pmelTao.csv?&time>=2008-11-13T00:00:00Z"));
//        "http://127.0.0.1:8080/cwexperimental/index.html"));
//        "http://127.0.0.1:8080/cwexperimental/tabledap/rPmelTao.csv?&time>=2008-11-13T00:00:00Z"));
//    String2.log(SSR.getUrlResponseString("https://coastwatch.pfeg.noaa.gov:8443/erddap2/griddap/etopo180.htmlTable?altitude[(-90.0):1000:(90.0)][(-180.0):1000:(180.0)]"));
//      String2.log(SSR.minimalPercentEncode("sst[(1870-01-01):1:(2011-07-01T00:00:00Z)][(29.5):1:(29.5)][(-179.5):1:(179.5)]"));
//    SSR.testPost();
//
//    String touchThese[] = {
//    };
//    for (int i = 0; i < touchThese.length; i++)
//        SSR.touchUrl(touchThese[i], 60000);
//
//    String2.log(String2.annotatedString(SSR.getFileString(
//        "/u00/data/points/LiquidR_HBG3_2015/ais.csv")));
//    SSR.zipEach("c:/temp/codarsf/");
//    String.matches dddd_ddd_dddd_add
//        String regex="[0-9]{4}_[0-9]{3}_[0-9]{4}_[a-zA-Z][0-9]{2}";
//        String2.log("match=" + ("1234_567_1234_k00".matches(regex))); 
//    while (true) {
//          String ans = String2.getStringFromSystemIn("String to be annotated?");
//          if (ans.length() == 0 || ans.equals("exit")) break;
//          String2.log(String2.annotatedString(ans));
//      }
//    String2.log(String2.readFromFile(tName)[1]);
//    String2.log(String2.getKeysAndValuesString(System.getProperties()));
//    String2.log(String2.utf8ToString(EDD.oneFromDatasetXml("rMHchla8day").searchString()));
//    StringArray.repeatedDiff("c:/downloads/httpd.conf", 
//                             "c:/downloads/httpd.confOrig");
//    StringArray.test();
/* */
//(new Table()).readASCII("c:/data/ndbc/ndbcMetHistoricalTxt/41009h1990.txt"); //large      
//Table table = new Table();
//table.readASCII("C:/data/regina/DCU5.nc");
//String2.log(table.toCSVString());
//    Table taoTable = new Table();
//        taoTable.readJson("tao", SSR.getUrlResponseString(
//            EDStatic.erddapUrl + "/tabledap/pmel_dapper/tao.json?longitude,latitude,altitude,time,station_id,sea_surface_temperature,sea_surface_temperature_quality,air_temperature,air_temperature_quality,relative_humidity,relative_humidity_quality,relative_humidity_source,wind_to_direction,wind_direction_quality,wind_direction_source,wind_speed,wind_speed_quality,wind_speed_source,eastward_wind,northward_wind&time>=2007-08-01&time<=2007-10-01"));
//        Math2.gcAndWait(); String2.log(" done " + Math2.memoryString());  //in a test
//    boolean pauseAfterEach = false;
//      Table table = new Table(); table.readNcCF(
//          "c:/data/kerfoot/unit_236-20121005T2349.ncCF.nc3.nc",
//          null, //StringArray.fromCSV(""),
//          null, null, null);
//      table.leftToRightSort(5);
//      String2.log(table.toCSVString(10));
//    Table.testParseDapQuery();
//    Table.testSubsetViaDapQuery();
//    TestListFiles.main(new String[]{"c:/"});
//    TestNCDump.main(new String[]{"c:/temp/CM2006171_230000h_u25h.nc"});
//    TestSSR.testEmail();
//    TestSSR.testEmail("bob.simons@noaa.gov", "");  //remove password after testing!!!
//    TestUtil.testString2();
//    TestUtil.testCalendar2();
//    Touch.getPiscoUrls();
//    XML.prettyXml("c:/programs/mapserver/WVBoreholeResponse.xml", 
//                  "c:/programs/mapserver/WVBoreholeResponsePretty.xml");
//    XML.prettyXml(
//        "c:/programs/iso19115/sst-aerosol-aggregation20110520.xml", 
//        "c:/programs/iso19115/bobSST.xml");


//Force compilation of all the classes that need to be deployed.
//Almost all of these are compiled automatically if you recompile everything,
//but it is useful to have them here: During development, if you change a 
//lower level class that isn't listed in TestAll and tell the compiler to 
//recompile TestAll, the compiler may not notice the changes to the lower 
//level class and so won't recompile it.  Mentioning the class here solves 
//the problem.
Attributes att;       
AttributedString2 as2;
Boundaries boun;
Browser browser;
ByteArray ba;
Calendar2 calendar2; 
CharArray chara;
CompoundColorMap ccm;
CompoundColorMapLayerChild ccmlc;
ContourScreen cons;
CWUser cwUser;
CWBrowser cwBrowser;
CWBrowserHAB cwBrowserHAB;
CWBrowserAK cwBrowserAK;
CWBrowserSA cwBrowserSA;
CWBrowserWW180 cwBrowserWW180;
CWBrowserWW360 cwBrowserWW360;
CWDataBrowser cwDataBrowser;
dods.dap.DConnect dConnect;
DataHelper dh;
DigirHelper dh2;
dods.dap.DSequence dseq;
DoubleArray doublea;
EDDTableFromAllDatasets etfad;
EmaAttribute ea;
EmaClass ec;
EmaColor ecolor;
File2 f2;
FileNameUtility fnu;
FileVisitorDNLS fvdnls;
FileVisitorSubdir fvsd;
FilledMarkerRenderer fmr;
FloatArray floata;
GDateTime gdt;
GenerateThreddsXml gtx;
GraphDataLayer gdl;
Grid grid;
GridDataSet gds;
GridDataSetAnomaly gdsa;
GridDataSetOpendap gdso;
GridDataSetThredds gdst;
GridScreen gs;
GSHHS gshhs;
Image2 i2;
IntArray inta;
JSONObject jo;
org.json.JSONTokener jt;
LongArray la;
MakeErdJavaZip mejz;
MapScreen mapScreen;
Math2 m2;
Matlab matlab;     
MustBe mb;
NcHelper ncHelper;
NetCheck netCheck;
OneOf oneOf;
OpendapHelper oh;  
ParseJSON parseJSON;
PauseTest pt;
PointScreen ps;
PointVectorScreen pvs;
PrimitiveArray primitiveArray; 
Projects projects;
RegexFilenameFilter rff;
ResourceBundle2 rb2;
RowComparator rc;
RowComparatorIgnoreCase rcic;
SdsReader sr;
SgtGraph sgtGraph; 
SgtMap sgtMap;     
SgtUtil sgtUtil;
Shared shared;
gov.noaa.pfel.coastwatch.sgt.PathCartesianRenderer sgtptcr;

gov.noaa.pmel.sgt.AnnotationCartesianRenderer sgtacr;
gov.noaa.pmel.sgt.AxisTransform sgtat;
gov.noaa.pmel.sgt.CartesianGraph sgtcg;
gov.noaa.pmel.sgt.CartesianRenderer sgtcr;
gov.noaa.pmel.sgt.CenturyAxis sgtca;
gov.noaa.pmel.sgt.contour.Contour sgtcc;
gov.noaa.pmel.sgt.contour.ContourLine sgtccl;
gov.noaa.pmel.sgt.DecadeAxis sgtda;
gov.noaa.pmel.sgt.dm.SGT3DVector sgtsg3dv;
gov.noaa.pmel.sgt.dm.SGTFull3DVector sgtsgf3dv;
gov.noaa.pmel.sgt.dm.SGTGrid sgtsgdtg;
gov.noaa.pmel.sgt.dm.SGTImage sgti;
gov.noaa.pmel.sgt.dm.SGTLine sgtl;
gov.noaa.pmel.sgt.dm.SGTPoint sgtp;
gov.noaa.pmel.sgt.dm.SGTTuple sgtt;
gov.noaa.pmel.sgt.dm.SGTVector sgtsgdtv;
gov.noaa.pmel.sgt.GridAttribute sgtga;
gov.noaa.pmel.sgt.GridCartesianRenderer sgtgcr;
gov.noaa.pmel.sgt.Graph sgtg;
gov.noaa.pmel.sgt.JPane sgtj;
gov.noaa.pmel.sgt.LabelDrawer1 ld1;
gov.noaa.pmel.sgt.LabelDrawer2 ld2;
gov.noaa.pmel.sgt.Layer sgtla;
gov.noaa.pmel.sgt.LayerChild sgtlc;
gov.noaa.pmel.sgt.LineCartesianRenderer sgtlcr;
gov.noaa.pmel.sgt.MilliSecondAxis sgtms;
gov.noaa.pmel.sgt.PaneProxy sgtpp;
gov.noaa.pmel.sgt.PointCartesianRenderer sgtpcr;
gov.noaa.pmel.sgt.SecondMinuteAxis sgtsm;
gov.noaa.pmel.sgt.TimeAxis sgtta;
gov.noaa.pmel.sgt.VectorCartesianRenderer sgtvcr;
gov.noaa.pmel.sgt.YearDecadeAxis sgtyda;
gov.noaa.pmel.util.SoTRange sotr;
ShortArray sha;
SimpleXMLReader sxr;
SSR ssr;
String2 s2;
StringArray sa;
StringComparatorIgnoreCase scic;
Table myTable;
TableXmlHandler txh;
Tally tally;
Test test;
TestSSR tssr;
TrajectoryScreen trajs;
gov.noaa.pmel.sgt.VectorCartesianRenderer vcr;
VectorPointsRenderer vpr;
VectorScreen vs;
XML xml;


//ERDDAP-related
AxisDataAccessor ada;
DasDds dd;
EDStatic es;
EDD edd;            
EDDGrid eddGrid;   
EDDGridAggregateExistingDimension eddaed;  
EDDGridCopy eddgc;
//EDDGridFromBinaryFile eddgfbf;  //not active
EDDGridFromDap eddgfd;  
EDDGridFromEDDTable eddgfet;  
EDDGridFromErddap eddgfed;  
EDDGridFromEtopo eddgfe;  
EDDGridFromFiles eddgff;  
EDDGridFromNcFiles eddgfncf;  
EDDGridSideBySide eddgsbs;  
EDDTable eddTable; 
EDDTableCopy eddtc;
//EDDTableCopyPost eddtcp;  //inactive
EDDTableFromAsciiService eddtfas;
EDDTableFromAsciiServiceNOS eddtfasn;
//EDDTableFromBMDE eddtfb; //inactive
EDDTableFromCassandra eddtfc; 
EDDTableFromDapSequence eddtfds; 
EDDTableFromDatabase eddtfdb; 
EDDTableFromEDDGrid eddtfeg; 
EDDTableFromErddap eddtfed;
EDDTableFromFileNames eddtffn; 
EDDTableFromFiles eddtff; 
EDDTableFromAsciiFiles eddtfaf;
EDDTableFromColumnarAsciiFiles eddtffaf;
EDDTableFromHyraxFiles eddtfhf;
//EDDTableFromMWFS eddtfm; 
EDDTableFromNcFiles eddtfnf; 
EDDTableFromNWISDV eddtfnwisdv;
EDDTableFromOBIS eddtfo; 
EDDTableFromPostDatabase eddtfpdb; 
EDDTableFromPostNcFiles eddtfpnf; 
EDDTableFromSOS eddtfs;
EDDTableFromThreddsFiles eddtftf;
//EDStatic above
EDUnits edu;
EDV edv;
EDVAlt edva;
EDVAltGridAxis edvaga;
EDVGridAxis edvga;
EDVLat edvl;
EDVLatGridAxis edvlga;
EDVLon edvlon;
EDVLonGridAxis edvlonga;
EDVTime edvt;
EDVTimeGridAxis edvtga;
EDVTimeStamp edvts;
EDVTimeStampGridAxis edvtsga;
Erddap erddap;       
ErddapRedirect erddapRedirect;       
FishBase fb;
GenerateDatasetsXml gdx;
GridDataAccessor gda;
GridDataAllAccessor gdaacc;
GridDataRandomAccessor gdracc;
HtmlWidgets hw;
LoadDatasets ld;
OutputStreamSource oss;
OutputStreamFromHttpResponse osfhr;
PersistentTable pert;
Projects2 proj2;
RunLoadDatasets rld;
Subscriptions sub;
TableWriter tw;
TableWriterAll twa;
TableWriterAllWithMetadata twawm;
TableWriterDistinct twdis;
TableWriterDods twd;
TableWriterDodsAscii twda;
TableWriterEsriCsv twec;
TableWriterGeoJson twgj;
TableWriterHtmlTable twht;
TableWriterJson twj;
TableWriterOrderBy twob;
TableWriterOrderByMax twobm;
TableWriterSeparatedValue twsv;
TableWriterUnits twu;
TaskThread tt;
WaitThenTryAgainException wttae;

/* */
        //*** All of the unit tests for CWBrowsers and ERDDAP.

        //low level utilities
        TestUtil.main(null);
        Image2.test();  
        XML.test();
        ByteArray.test();
        CharArray.test();
        ShortArray.test();
        IntArray.test();
        LongArray.test();
        FloatArray.test();
        DoubleArray.test();
        StringArray.test();
        PrimitiveArray.test();
        Attributes.test();
        ResourceBundle2.test();

        //test that requires running from a command line
        TestSSR.main(null);
        RegexFilenameFilter.test();  
        Tally.test();
        PersistentTable.test();

        //test that THREDDS is up  (use ip name here, not numeric ip)
        try {
            OneOf.ensureDataServerIsUp( 
                "http://oceanwatch.pfeg.noaa.gov/thredds/catalog.html",
                String2.split("ERD THREDDS Data Server`Satellite Datasets`HF Radio-derived Currents Datasets",
                    '`'),
                true);
            Opendap.doOceanWatchSpeedTests(false, false); //dotTest, asciiTest

        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e)); 
        }
        Math2.gcAndWait();  //in a test

        //data
        DataStream.main(null);
        SimpleXMLReader.test();
        TimePeriods.test();
        FileNameUtility.main(null);
        ParseJSON.test();

        //test validity of DataSet.properties
        try {
            ValidateDataSetProperties.main(null);
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e)); 
        }

        //ensure all of the datasets used in each browser are in DataSet.properties validDataSets.
        if (true) {
            String propNames[] = {
                "CWBrowser",
                "CWBrowserAK",
                "CWBrowserSA",
                "CWBrowserWW180",
                "CWBrowserWW360",
                "CWBrowserHAB"};
            StringArray validDataSets = null;
            for (int pni = 0; pni < propNames.length; pni++) {
                String2.log("\nTesting " + propNames[pni]);
                fnu = new FileNameUtility("gov.noaa.pfel.coastwatch." + propNames[pni]);
                String tDataSetList[] = String2.split(fnu.classRB2().getString("dataSetList", null), '`');
                int nDataSets = tDataSetList.length;
                if (validDataSets == null) {
                    String ts = fnu.dataSetRB2().getString("validDataSets", null);
                    String[] tsa = String2.split(ts, '`');
                    validDataSets = new StringArray(tsa);
                }
                for (int i = OneOf.N_DUMMY_GRID_DATASETS; i < nDataSets; i++) {  //"2" in order to skip 0=OneOf.NO_DATA and 1=BATHYMETRY
                    if (validDataSets.indexOf(tDataSetList[i], 0) == -1) {
                        Test.error("In " + propNames[pni] + ".properties, [" + i + "]=" + 
                            tDataSetList[i] + " not found in DataSet.properties validDataSets:\n" +
                            validDataSets);
                    }
                }
            }
        }
        
        Matlab.main(null);
        Table.testSaveAsMatlab();
        try {
            //this fails if opendap server is down
            Opendap.main(null); 
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "Recover from opendap failure?");
        }
        //need tests of data.Grid2DDataSet classes
        //hdf.SdsWriter.main(null); //needs work
        DataHelper.test();  
        NcHelper.test();  
        OpendapHelper.test();  //few tests. relies on testing in classes that use it.
        Grid.main(null); 
        //GridDataSetCWOpendap.test();  //the files are no longer available since we are moving to thredds
        GridDataSetThredds.test(); 
        GridDataSetThredds.testGetTimeSeries();
        GridDataSetOpendap.test();
        GridDataSetOpendap.testGetTimeSeries();
        SaveOpendap.test();
        TwoGrids.test();
        GridDataSetAnomaly.test();
        DoubleCenterGrids.test();

        //long test, not necessary to do every time; good for testing changes to shared and for memory tests.
        //and see method's comments for proper setup
        //Shared.test(); 

        Table.test();  
        //Table.testSql();
        DigirHelper.test();
        //PointSubsetScaled.main(null); //inactive: use PointIndex
        //PointSubsetFull.main(null);   //inactive: use PointIndex
        //Index.main(null);             //inactive: use PointIndex
        PointIndex.main(null); 
        StoredIndex.main(null); 
        //NdbcMetStation  //see tests in PointDataSetStationVariables
        //DrifterDummy.main(null);
        
        //CacheOpendapStation.testMbariOpendapReliability(); //don't run routinely; runs forever
        CacheOpendapStation.test(); 
        //PointDataSetFromStationVariables.remakeMbariCachesAndDataSets(); //run only when needed
        PointDataSetFromStationVariables.test(); //several tests
        TableDataSet4DNc.test();

        GenerateThreddsXml.testShortenBoldTitles();
        GenerateThreddsXml.test();

        //other
        GSHHS.test();
        Boundaries.test();
        Browser.test();
        DecimalDegreeFormatter.main(null);  
        DegreeMinuteFormatter.main(null);  
        CompoundColorMap.test();
        SgtMap.testCreateTopographyGrid();
        SgtMap.testBathymetry(0, 12);   //0, 12   9 is imperfect but unreasonable request
        SgtMap.testTopography(0, 12);   //0, 12   9 is imperfect but unreasonable request
        SgtMap.testRegionsMap(-180, 180, -90, 90);
        SgtMap.testRegionsMap(0, 360, -90, 90);
        SgtUtil.test(); 
        SgtMap.test(true, true); 
        SgtMap.testMakeCleanMap(0, 5); //all
        CartesianProjection.test();
        SgtGraph.test();  
        NDimensionalIndex.test();

        //ERDDAP
        HtmlWidgets.test();
        CfToFromGcmd.test();
        EDStatic.test();
        EDV.test();
        EDVTimeStamp.test();
        EDUnits.test();
        Table.testXml();
        Subscriptions.test(); 
        FileVisitorDNLS.test(false); //doBigTest
        FileVisitorSubdir.test(); 
        WatchDirectory.test(true); //doInteractiveTest
        boolean doGraphicsTests = true;
        boolean doLongTest = false;

        EDDGridFromDap.test(false); //doGraphicsTests);
        // EDDGridFromDap.testGraphics(); //do just before releases    
        //EDDGridFromBinaryFile.test(); not finished
        EDDGridFromErddap.test(); 
        EDDGridFromEtopo.test(true);
        //EDDGridAggregateExistingDimension.test();  //don't usually run...very slow
        EDDGridAggregateExistingDimension.testGenerateDatasetsXml();
        EDDGridFromNcFiles.test(true); //deleteCachedInfo
        EDDGridFromMergeIRFiles.test();
        EDDGridFromEDDTable.test();
        EDDGridCopy.test();
        EDDGridSideBySide.test(true); //doGraphicsTests);  //the best grid graphics tests are here

        EDDTableFromFiles.test(); 
        EDDTableFromNcFiles.test(true); //doGraphicsTests); //the best table graphics tests are always done
        EDDTableFromNcCFFiles.test();  
        EDDTableFromHyraxFiles.test(); 
        EDDTableFromFileNames.test(); 
        EDDTableFromEDDGrid.test();
        EDDTableFromDapSequence.test(); 
        //EDDTableFromDapSequence.testMemory(); //don't usually run...very slow
        EDDTableFromDatabase.test();     
        //EDDTableFromPostDatabase.test(); //INACTIVE.    very slow?        
        EDDTableFromCassandra.test();     
        //UPDATE nosCoops every 3 months: true, true, 
        //  then copy /subset/nosCoops*.json files to coastwatch and UAF 
        EDDTableFromAsciiServiceNOS.test(false, false);  //makeSubsetFiles, reloadStationFiles
        EDDTableFromErddap.test(); 
        EDDTableFromAsciiFiles.test(false); //rarely: true=delete cached info
        EDDTableFromColumnarAsciiFiles.test(); 
        EDDTableFromAwsXmlFiles.test();
        EDDTableFromThreddsFiles.test(false); //rarely: true=delete cached info
        //EDDTableFromMWFS.test(false); //doLongTest); //as of 2009-01-14 INACTIVE
        //EDDTableFromNOS.test(false); //doLongTest); //as of 2010-09-08 INACTIVE
        //EDDTableFromNWISDV.test();  //INACTIVE as of 2011-12-16.
        EDDTableFromOBIS.test();
        //EDDTableFromBMDE.test(); //INACTIVE
        EDDTableFromSOS.test(true); //useCachedInfo
        EDDTableFromWFSFiles.test();
        EDDTableCopy.test();
        //EDDTableCopyPost.test(); INACTIVE
        EDDTable.test(); //mostly SOS server tests
        Erddap.test(); 

        //NetCheckTests
        //NetCheck.unitTest(); which does 3 tests:
        HttpTest.unitTest();
        OpendapTest.unitTest(); 
        //SftpTest.unitTest(); //orpheus Shell authentication started failing ~2010-06


        //a test of oceanwatch THREDDS   (should run great)
        try {
            for (int i = 0; i < 5; i++) {
                long time9 = System.currentTimeMillis();
                Opendap.doOceanWatchSpeedTests(false, false); //dotTest, asciiTest 
                time9 = System.currentTimeMillis() - time9;
                if (i > 0 && time9 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
                    String2.pressEnterToContinue("OceanWatch Thredds too slow: " + time9); 
            }
            //don't run often
            Opendap.threddsTunnelTest(200, 
                "http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday",
                "CMusfc"); 
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "\nUnexpected oceanwatch error."); 
        }
        
        //INACTIVE: a test of thredds1 THREDDS 8081
        //try {
        //    for (int i = 0; i < 5; i++) {
        //        long time9 = System.currentTimeMillis();
        //        Opendap.doThredds1_8081SpeedTests(false, true); //dotTest, asciiTest
        //        time9 = System.currentTimeMillis() - time9;
        //        if (i > 0 && time9 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
        //            String2.pressEnterToContinue("Thredds1 8081 Thredds too slow: " + time9); 
        //    }
        //    //don't run often
        //    Opendap.threddsTunnelTest(200, 
        //        "http://thredds1.pfeg.noaa.gov:8081/thredds/dodsC/satellite/CM/usfc/hday",
        //        "CMusfc"); 
        //} catch (Exception e) {
        //    String2.pressEnterToContinue(MustBe.throwableToString(e) + 
        //        "\nUnexpected THREDD1 8081 ERROR."); 
        //}

        //INACTIVE: a test of otter THREDDS 8081  (should run great)
        //try {
        //    for (int i = 0; i < 5; i++) {
        //        long time8 = System.currentTimeMillis();
        //        Opendap.doOtterSpeedTests(false, false, 8081); //dotTest, asciiTest
        //        time8 = System.currentTimeMillis() - time8;
        //        if (i > 0 && time8 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
        //            String2.pressEnterToContinue("Otter Thredds 8081 too slow: " + time8); 
        //    }
        //    //don't run often
        //    Opendap.threddsTunnelTest(200, 
        //        "http://161.55.17.243:8081/thredds/dodsC/satellite/CM/usfc/hday", //otter
        //        "CMusfc"); 
        //} catch (Exception e) {
        //    String2.pressEnterToContinue(MustBe.throwableToString(e) + 
        //        "\nUnexpected otter 8081 error."); 
        //}

        //INACTIVE:  a test of otter THREDDS 8087
        //try {
        //    for (int i = 0; i < 5; i++) {
        //        long time8 = System.currentTimeMillis();
        //        Opendap.doOtterSpeedTests(false, false, 8087);  //dotTest, asciiTest
        //        if (true) throw new Exception("SHOULDN'T GET HERE.");
        //        time8 = System.currentTimeMillis() - time8;
        //        if (i > 0 && time8 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
        //            String2.pressEnterToContinue("Otter Thredds 8087 too slow: " + time8); 
        //    }
        //    //don't run often
        //    Opendap.threddsTunnelTest(200, 
        //        "http://161.55.17.243:8087/thredds/dodsC/satellite/CM/usfc/hday", //otter
        //        "CMusfc"); 
        //} catch (Exception e) {
        //    String2.pressEnterToContinue(MustBe.throwableToString(e) + 
        //        "\nOTTER 8081 STARTED CAN'T CONNECT ERROR TO AGssta3day ON 2009-09-10" +
        //        "\nUnexpected otter 8087 error:"); 
        //}

        //a test of erddap
        try {
            for (int i = 0; i < 5; i++) {
                long time9 = System.currentTimeMillis();
                Opendap.doErddapSpeedTests(false, false); //dotTest, asciiTest 
                time9 = System.currentTimeMillis() - time9;
                if (i > 0 && time9 > 3000) //2014-08 was 1000 in ERD building. Now 3000 from outside
                    String2.pressEnterToContinue("Erddap too slow: " + time9); 
            }
            //don't run often
            Opendap.threddsTunnelTest(200, 
                "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdCMsfc",
                "eastCurrent"); 
        } catch (Exception e) {
            String2.pressEnterToContinue(MustBe.throwableToString(e) + 
                "\nUnexpected erddap error."); 
        }

        //Touch.thredds();  //run as needed to tell thredds to cache PISCO datasets?   or usually runs really fast?

        //make ErdJava.zip  
        //for distribution of GridSaveAs, NetCheck, ConvertTable, and GenerateThreddsXml
        MakeErdJavaZip.main(null); //see C:\programs\tomcat\webapps\cwexperimental\ErdJava.zip
        //MakeErdJavaZip.makeConvertTableJar("C:/pmelsvn/WebContent/WEB-INF/lib/"); //only do when working on LAS stuff

        //make EMA.war
        MakeEmaWar.main(null);

        String2.log(
            "\n" +
            "*** Before a release, spell check (copy to EditPlus, then spellcheck)\n" +
            "and validate HTML the main web pages!\n");

// */
        //AFTER deploying browsers: test the experimental browser
        //TestBrowsers.testAll();

        //TestBrowsers.doGraphicalGetTests(TestBrowsers.experimentalBaseUrl + "CWBrowser.jsp"); //part of testAll
        String2.log("\n*** TestAll finished successfully.");
        String2.returnLoggingToSystemOut();
        String2.log("*** Press ^C to exit.");
    }
   
    
}

