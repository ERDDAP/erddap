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
//    String2.log(Calendar2.epochSecondsToIsoStringT(0));
//    String2.log(Calendar2.epochSecondsToIsoStringT(1000* 86400L));
//    String2.log(Calendar2.epochSecondsToIsoStringT(10000* 86400L));
//    String2.log(Calendar2.epochSecondsToIsoStringT(100000* 86400L));
//    String2.log(Calendar2.epochSecondsToIsoStringT(-10000* 86400L));
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

//    Table.debug = true; DasDds.main(new String[]{"dominic2", "-verbose"});
//    String2.log(DigirHelper.getObisInventoryString(
//        "http://iobis.marine.rutgers.edu/digir2/DiGIR.php", 
//        "OBIS-SEAMAP", 
//        "darwin:ScientificName"));
//        //"darwin:Genus");
//    DigirHelper.test(); //tests all...
//    DigirHelper.testGetMetadata();
//    DigirHelper.testGetInventory();
//    DigirHelper.testObis();
//    DigirHelper.testOpendapStyleObis();
//    new DigirObisTDSP();
//    new DigirIobisTDSP();

//    EDD tedd = EDD.oneFromDatasetXml("nmspWcosTemp"); System.out.println(tedd.toString());
//    tedd = EDD.oneFromDatasetXml("cPostDet3"); System.out.println(tedd.toString());
//    (EDDTable)EDD.oneFromDatasetXml("pmelTao")).getEmpiricalMinMax("2008-10-05", "2008-10-10", false, false);
//    String2.log(((EDDTable)EDD.oneFromDatasetXml("nwioosAdcp2003")).toString());
//    String2.log(EDD.testDasDds("thierry")); 
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

//    EDDGridAggregateExistingDimension.testGetDodsIndexUrls();
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

//    String2.log("\n" + EDDGridFromDap.generateDatasetsXml(false, //directions
//"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/MH1/nflh/1day",
//"http://disc2.nascom.nasa.gov/thredds/dodsC/aggregation/TRMM_CSH/TRMM_CSH_Aggregation.ncml.ncml",
//        null, null, null, -1, null));
//    EDDGridFromDap.generateErdThreddsDatasetXml("Satellite/aggregsat", "satellite"));
//    EDDGridFromDap.generateErdThreddsDatasetXml("Hfradar/aggreghfradar", "satellite"));
//    EDDGridFromDap.testAccessibleTo();
//    EDDGridFromDap.testAddToHyraxUrlList();
//    EDDGridFromDap.testBasic1();
//    EDDGridFromDap.testBasic2();
//    EDDGridFromDap.testBigRequest(2); //~21.6MB/timePoint  2=1request, 6=6requests, 96=~2070000000 Bytes
//    EDDGridFromDap.testClimatologyTime();
//    EDDGridFromDap.testGridWithAltitude();
//    EDDGridFromDap.testGridWithDepth2();
//    EDDGridFromDap.testDescendingLat(true); //true=testGraphics
//    EDDGridFromDap.testDescendingAxisGeotif();
//    EDDGridFromDap.testForEllyn();
//    EDDGridFromDap.testGenerateDatasetsXml();
//    EDDGridFromDap.testGenerateDatasetsXml2();
//    EDDGridFromDap.testGenerateDatasetsXml3();
//    EDDGridFromDap.testGenerateDatasetsXmlFromThreddsCatalog();
//    EDDGridFromDap.testGeotif();

//    EDDGridFromDap.generateDatasetsXmlFromThreddsCatalog(
//        "c:/temp/fromThreddsCatalog.xml",
//        //one catalog.xml URL:
//          "http://oceanwatch.pfeg.noaa.gov/thredds/catalog/catalog.xml",
//          "http://thredds.jpl.nasa.gov/thredds/podaac_catalogs/AQUARIUS_L3_SMI_V20_catalog.xml", 
//          "http://osmc.noaa.gov/thredds/catalog/catalog.xml",
//        ".*", -1);
//    String2.toNewlineString(EDDGridFromDap.getUrlsFromThreddsCatalog(
//        "http://osmc.noaa.gov/thredds/catalog/catalog.xml", 
//        ".*", true));
        
//    crawl UAF clean catalog      
//      Results file is /temp/datasetsUAF{uafi}_{dateTime}.xml          
//      Log file is     /temp/datasetsUAF{uafi}_{dateTime}.xml.log.txt  
//        Look at problems. Make improvements.
//        Look for "error", "unable to get axis", "unsorted", "no colorBarMin/Max", 
//            "ioos_category=Unknown for"
//        Sort it. 
//    2012-10-17  pfeg (crashes us?)  ecowatch down
//    2013-11-06, 2012-12-09 new catalog, 2014-03-31
//    EDDGridFromDap.testUAFSubThreddsCatalog(0); //entire official clean catalog  ~4hrs
//    EDDGridFromDap.testUAFSubThreddsCatalog(17);  //test one sub catalog
//    for (int uafi = 6; uafi < EDDGridFromDap.UAFSubThreddsCatalogs.length; uafi++) {
//        String2.log("\n\n************************************* UAFI=" + uafi);
//        EDDGridFromDap.testUAFSubThreddsCatalog(uafi);  //test one sub catalog
//    }

//    EDDGridFromDap.testGetUrlsFromHyraxCatalog();
//    EDDGridFromDap.testGetUrlsFromThreddsCatalog();
//    EDDGridFromDap.testGraphics();
//    EDDGridFromDap.testKml();
//    EDDGridFromDap.testMap74to434();
//    EDDGridFromDap.testMapAntialiasing();
//    EDDGridFromDap.testNcml();
//    EDDGridFromDap.testScaleFactor();
//    EDDGridFromDap.testNetcdfJava();
//    EDDGridFromDap.testNoAxisVariable();
//    EDDGridFromDap.testPmelOscar();
//    EDDGridFromDap.testQuickRestart();
//    EDDGridFromDap.testSliderCsv();
//    EDDGridFromDap.testSpeedDAF();
//    EDDGridFromDap.testSpeedMAG();
//    EDDGridFromErddap.testDataVarOrder(); 
//    EDDGridFromErddap.testGridNoIoosCat();
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://coastwatch.pfeg.noaa.gov/erddap")); 
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://upwell.pfeg.noaa.gov/erddap")); 
//    String2.log(EDDGridFromErddap.generateDatasetsXml("http://oos.soest.hawaii.edu/erddap")); 
//    EDDGridFromNcFiles.testCwHdf(true);
//    EDDGridFromNcFiles.testGenerateDatasetsXml2();
//    EDDGridFromNcFiles.testGrib_43(true);  //42 or 43 for netcdfAll 4.2- or 4.3+
//    EDDGridFromNcFiles.testGrib2_43(true); //42 or 43 for netcdfAll 4.2- or 4.3+
//    EDDGridFromNcFiles.testNc(false);
//    String2.log(EDDGridFromNcFiles.generateDatasetsXml(
//        "/erddapTest/", "simpleTest\\.nc", 
//        "/erddapTest/simpleTest.nc",
//        90, null));
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
    
//    String2.log(NcHelper.dumpString("/erddapTest/dominic2/IT_MAG-L1b-GEOF_G16_s20140201000030_e20140201000059_c00000000000000.nc", "IB_time"));
//    String2.log(NcHelper.dumpString("/u00/data/points/scrippsGlidersIoos1/sp031-20140412T155500.nc", false));
//    String2.log(NcHelper.dumpString("c:/u00/satellite/VH/pic/8day/V20120012012008.L3m_8D_NPP_PIC_pic_4km", false));
//    String2.log(NcHelper.dumpString("/u00/data/points/tao/daily/airt0n110w_dy.cdf", "AT_21"));
//    String2.log(NcHelper.dumpString("/u00/data/points/tao/5day/airt0n0e_5day.cdf", false));
//    String2.log(NcHelper.dumpString("/u00/data/points/tao/realtime/airt0n0e_dy.cdf", false));
//    String2.log(NcHelper.dumpString("c:/data/kerfoot/ru29-RTOFS-20131110T1400_trajectoryProfile.nc", "trajectory"));
//    String2.log(NcHelper.dumpString("C:/u00/cwatch/erddap2/copy/ioosGliderNCSUSalacia/salacia-20130916T160356_rt0.nc", false));
//    String2.log(NcHelper.dumpString("C:/data/gtspp/bestNcConsolidated/2014/06/45E_0N.nc", false));
//    String2.log(NcHelper.dumpString("C:/data/MH/A2014016.L3m_DAY_CHL_chlor_a_4km", false));
//    String2.log(String2.noLongLines(NcHelper.dumpString("/u00/data/points/gtsppNcCF/199001a.nc", "depth"), 80, ""));
//Table table = new Table();
//table.readNDNc("/u00/data/points/taoOriginal/realtime/airt0n110w_dy.cdf",
               //"/u00/data/points/taoOriginal/daily/airt0n110w_dy.cdf", 
//    null, null, 0, 0, true);
//String2.log("nRows=" + table.nRows());
//table.removeRows(2, table.nRows() - 30); 
//String2.log(table.toCSVString());
//    String2.log(Projects.dumpTimeLatLon("/u00/data/viirs/MappedDaily4km/d4.ncml"));
//    String2.log(NcHelper.dumpString("/u00/data/viirs/MappedDaily4km/d42013074.ncml", false));
//    *** Monthly
//    String2.log(NcHelper.dumpString("/u00/data/viirs/MappedMonthly4km/V20120012012031.L3m_MO_NPP_CHL_chlor_a_4km", false));
//    String2.log(Projects.dumpTimeLatLon("/u00/data/viirs/MappedMonthly4km/m4.ncml");
//    String2.log(NcHelper.dumpString("/data/gtspp/gtspp_103329_xb_112.nc", false));
//    String2.log(NcHelper.dumpString("/data/roy/modis_sst_monthly_anomaly_200207.nc", false));
//    EDDGridFromNcFiles.testNcml();
      //make e.g., <netcdf location="V2013074.L3m_DAY_NPP_CHL_chlor_a_4km" coordValue="15779"/>
//    Projects.makeNcmlCoordValues("V*.L3m_DAY_NPP_CHL_chlor_a_4km", "2012-01-02", "2013-12-31", 1, Calendar.DAY_OF_YEAR); 

//    EDDGridFromNcFiles.testSpeed(-1);  //-1 for all
//    EDDGridSideBySide.testTransparentPng();

//    ((EDDTable)EDD.oneFromDatasetXml("erdGlobecBottle")).makeNewFileForDapQuery(null, null, 
//        "", 
//        "c:/downloads/", "erdGlobecBottle", ".iso19115"); 
//    EDDTable.testSosNdbcMet();
//    EDDTable.testSosCurrents();
//    EDDTable.testSosGomoos();
//    EDDTable.testSosOostethys();
//    EDDTableCopy.testBasic();
//    EDDTableCopy.testCPostDet(true);  //out-of-date? defaultCheckSourceData 
//    EDDTableCopy.testCPostDet(false); //out-of-date? defaultCheckSourceData (faster)
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
//        "/data/austin/", "ADB.csv",
//        "/data/austin/ADB.csv",
//        "", 1, 2, -1,
//        "", "", "", "", "",
//        "", // tSortFilesBySourceNames, 
//        "", "", "", "", null));
//    EDDTableFromAsciiFiles.testFixedValue();
//    EDDTableFromAsciiServiceNOS.makeNosCoopsWLSubsetFiles(false);  //reloadStationsFile?
//    EDDTableFromAsciiServiceNOS.makeNosCoopsMetSubsetFiles(false); //reloadStationsFile?
//      EDDTableFromAsciiServiceNOS.makeNosActiveCurrentsSubsetFile();
//    EDDTableFromAsciiServiceNOS.testNosCoops("nosCoopsMWT"); //"nosCoopsWLTP60");  //a regex
//    EDDTable gtspp = (EDDTable)EDD.oneFromDatasetXml("pmelGtsppa");
//        gtspp.getEmpiricalMinMax(null, "2005-06-01", "2005-06-08", false, false);
//    EDDTableFromAwsXmlFiles.testGenerateDatasetsXml();
//    EDDTableFromAwsXmlFiles.testBasic(true);
//    String2.log(EDDTableFromDapSequence.generateDatasetsXml(
//        "http://www.ifremer.fr/oceanotron/OPENDAP/INS_CORIOLIS_GLO_TS_NRT_OBS_PROFILE_LATEST",
//        "http://opendap.co-ops.nos.noaa.gov/dods/IOOS/Raw_Water_Level", 
//        "http://gisweb.wh.whoi.edu:8080/dods/whoi/drift_data",
//        180, null));
//    EDDTableFromDapSequence.testArgo();
//    EDDTableFromDapSequence.testArgoTime();
//    EDDTableFromDapSequence.testBasic(); //tests globecBottle
//    EDDTableFromDapSequence.testCalcofi();
//    EDDTableFromDapSequence.testDapErdlasNewportCtd();
//    EDDTableFromDapSequence.testDapper(false);
//    EDDTableFromDapSequence.testErdlasCalCatch();
//    EDDTableFromDapSequence.testErdlasNewportCtd();
//    EDDTableFromDapSequence.testGenerateDatasetsXml();
//    EDDTableFromDapSequence.testGlobecBirds();
//    EDDTableFromDapSequence.testGraphics(false);
//    EDDTableFromDapSequence.testKml();
//    EDDTableFromDapSequence.testNosCoopsRWL();
//    EDDTableFromDapSequence.testOneTime();
//    EDDTableFromDapSequence.testLatLon();
//    EDDTableFromDapSequence.testPsdac();
//    EDDTableFromDapSequence.testReadDas();
//    EDDTableFromDapSequence.testReadPngInfo();
//    EDDTableFromDapSequence.testSourceNeedsExpandedFP_EQ();
//    EDDTableFromDapSequence.testSubsetVariablesGraph();
//    EDDTableFromDapSequence.testSubsetVariablesRange();
//    EDDTableFromDapSequence.testTimeStamp();
//    String2.log(EDDTableFromDatabase.getCSV("erdRole2"));
//    EDDTableFromDatabase.testGenerateDatasetsXml();
//    EDDTableFromDatabase.testBasic();
//    EDDTableFromDatabase.testNonExistentVariable();
//    EDDTableFromDatabase.testNonExistentTable();

//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://coastwatch.pfeg.noaa.gov/erddap", true)); //keep original datasetID?
//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://oceanview.pfeg.noaa.gov/erddap", true)); 
//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://oos.soest.hawaii.edu/erddap", false)); //keep original datasetID?
//    String2.log(EDDTableFromErddap.generateDatasetsXml("http://osmc.noaa.gov/erddap", true)); //keep original datasetID?
//    EDDTableFromErddap.testApostrophe();
//    EDDTableFromErddap.testBasic(true);
//    EDDTableFromErddap.testFromErddapFromErddap();
//    EDDTableFromErddap.testDegreesSignAttribute();
//    EDDTableFromErddap.testTableNoIoosCat();
//    EDDTableFromEDDGrid.testBasic();
//    EDDTableFromEDDGrid.testTableFromGriddap();
//    EDDTableFromFiles.testIsOK();
//    EDDTableFromFiles.testRegex();
//    String2.log(EDDTableFromHyraxFiles.generateDatasetsXml(
//        "http://data.nodc.noaa.gov/opendap/wod/monthly/APB/201103-201103/", 
//        "wod_01345934.O\\.nc", 
//        "http://data.nodc.noaa.gov/opendap/wod/monthly/APB/201103-201103/wod_013459340O.nc", 
//        10080, 
//        "", "", "", "",  //columnFromFileName
//        "time", //String tSortedColumnSourceName,
//        "time", //tSortFilesBySourceNames,
//        null)); //externalAddAttributes) 

//    String2.log(EDDTableFromNcCFFiles.generateDatasetsXml(
//        "/data/kerfoot/", ".*\\.nc", 
//        "/data/kerfoot/ru29-20131110T1400.ncCF.nc4.nc", 360, 
//        "", "", "", 
//        "", "", 
//        "", "", "", "", new Attributes()));
//    EDDTableFromNcCFFiles.bobMakeTestReadDataFiles();
//    EDDTableFromNcCFFiles.testNoAttName(); 
//    EDDTableFromNcCFFiles.testBridger(); 
//    EDDTableFromNcCFFiles.testReadData(0); 
//    EDDTableFromNcCFFiles.testKevin20130109();    
//
//Table tTable = new Table();
//tTable.readNDNc("C:/u00/cwatch/erddap2/copy/ioosGliderNCSUSalacia/salacia-20130916T160356_rt0.nc", 
//    null, "", Double.NaN, Double.NaN, true);
//String2.log(tTable.toCSVString());
//
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
//        "/data/scrippsGliders/", ".*\\.nc", 
//        "/data/scrippsGliders/sp031-20140409T132200.nc", null, 360, 
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
//    EDDTableFromNcFiles.makeTestFiles(); //one time
//    EDDTableFromNcFiles.test24Hours();
//    EDDTableFromNcFiles.test4D(false);
//    EDDTableFromNcFiles.testAltitude();
//    EDDTableFromNcFiles.testBigRequest();
//    EDDTableFromNcFiles.testCAMarCat();
//    EDDTableFromNcFiles.testDistinct();
//    EDDTableFromNcFiles.testEqualsNaN();
//    EDDTableFromNcFiles.testGenerateBreakUpPostDatasetsXml();
//    EDDTableFromNcFiles.testGenerateDatasetsXml();
//    EDDTableFromNcFiles.testGenerateDatasetsXml2();
//    EDDTableFromNcFiles.testGlobal();
//    EDDTableFromNcFiles.testId();
//    EDDTableFromNcFiles.testLegend(); 
//    EDDTableFromNcFiles.testManyYears();
//    EDDTableFromNcFiles.testMV();
//    EDDTableFromNcFiles.testNcCF1a();
//    EDDTableFromNcFiles.testNcCFMA1b();
//    EDDTableFromNcFiles.testNcCFMA2b();
//    EDDTableFromNcFiles.testNcCFTrajectoryProfile();
//    EDDTableFromNcFiles.testOrderBy();
//    EDDTableFromNcFiles.testOrderByMax();
//    EDDTableFromNcFiles.testOrderByMin();
//    EDDTableFromNcFiles.testOrderByMinMax();
//    EDDTableFromNcFiles.testSpeed(21);  //-1 for all
//    EDDTableFromNcFiles.testSpeedDAF();
//    EDDTableFromNcFiles.testSpeedMAG();
//    EDDTableFromNcFiles.testSpeedSubset();
//    EDDTableFromNcFiles.testStationLonLat();
//    EDDTableFromNcFiles.testStationLonLat2();
//    EDDTableFromNcFiles.testTableWithAltitude();
//    EDDTableFromNcFiles.testTableWithDepth();
//    EDDTableFromNcFiles.testTimeAxis();
//    EDDTableFromNcFiles.testModTime();
//    EDDTableFromNcFiles.testTransparentPng();

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
//       EDDTableFromNcFiles.bobConsolidateGtsppTgz(2014, 3, 2014, 9, false);  //first/last year(1990..)/month(1..), testMode
//       log file is c:/data/gtspp/log.txt 
//      2b) Email the "good" but "impossible" stations to Charles Sun
//       [was Melanie Hamilton, now retired]
//       [start in 2011? but not longer valid 2012-10-19 Meilin.Chen@noaa.gov]
//      2c) Undo changes to McAfee scanner
//    3) Update the dates in history and summary for erdGtsppBestNc in datasets2.xml 
//       and the query dates for erdGtsppBest in datasets2.xml and datasetsFEDCW.xml
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
//       EDDTableFromNcFiles.bobCreateGtsppNcCFFiles(2014, 3, 2014, 9); //e.g., first/last year(1990..)/month(1..)
//       String2.log(NcHelper.dumpString("/u00/data/points/gtsppNcCF/201406a.nc", false));
//    7) * Load erdGtsppBest in localHost ERDDAP.  (long time if lots of files changed)
//       * Generate .json file from
//         http://127.0.0.1:8080/cwexperimental/tabledap/erdGtsppBest.json?trajectory,org,type,platform,cruise&distinct()
//         and save it as [tomcat]/content/erddap/subset/erdGtsppBest.json
//       * Reload ERDDAP to ensure it loads quickly.
//    8) Test the .ncCF dataset:
//       EDDTableFromNcFiles.testErdGtsppBest("erdGtsppBest");
//    8) If copying all to coastwatch, temporarily rename dir to /u00/data/points/gtsppNcCFtemp/
//       * Copy the newly consolidated .ncCF files
//         from laptop   /u00/data/points/gtsppNcCF/
//         to coastwatch /u00/data/points/gtsppNcCF/
//       * Copy from local     [tomcat]/content/erddap/subset/erdGtsppBest.json
//              to coastwatch  [tomcat]/content/erddap/subset/erdGtsppBest.json
//              to   upwell [UAFtomcat]/content/erddap/subset/erdGtsppBest.json
//    8) Copy datasetsFEDCW.xml to coastwatch and rename to datasets.xml
//    9) Ping the gtspp flag url on ERDDAP (it is in "flag" bookmarks)
//       http://coastwatch.pfeg.noaa.gov/erddap/setDatasetFlag.txt?datasetID=erdGtsppBest&flagKey=2369414249
//       and make sure the new data and metadata are visible (hence, new dataset has loaded)

      //used to make NWIS Daily Value datasets (in order built/used)
//NWISDV is INACTIVE as of 2011-12-16
//    EDDTableFromNWISDV.testAvoidStackOverflow();
//    EDDTableFromNWISDV.testGetValuesTable();
//    EDDTableFromNWISDV.bobScrapeNWISStationNames("USAID", "304800061460000", "370500069280000");
//    EDDTableFromNWISDV.bobScrapeNWISStations(null, null, null);
//    EDDTableFromNWISDV.bobListUnique();
//    EDDTableFromNWISDV.bobMakeNWISDatasets();  //10 hours
//    EDDTableFromNWISDV.testGenerateDatasetsXml();
//    EDDTableFromNWISDV.bobGenerateNWISDVDatasetsXml();  //20 minutes
      //copy NWISDVDatasets....xml into datasets2.xml and a few into datasetsUAF.xml
      //delete any old C:/programs/tomcat/content/erddap/subset/usgs_waterservices*.json 
      //copy the selected usgs_waterservices*.json to upwell or (?)
//    EDDTableFromNWISDV.testBasic();

//    EDDTableFromNWISDV.bobGetWqxStationInfo(); //inactive
//    EDDTableFromNWISDV.testGetWqxSitesTable(); //inactive
//    EDDTableFromPostDatabase.testPostSurg3();
//    EDDTableFromPostDatabase.testPostDet3();
//    EDDTableFromPostDatabase.testPostTag();  
//  EDDTableFromPostDatabase.testPostSurg3Direct();
//    EDDTableFromSOS.testGenerateDatasetsXmlForIOOS();
//String2.log(EDDTableFromSOS.generateDatasetsXmlFromIOOS(
//        "http://opendap.co-ops.nos.noaa.gov/ioos-dif-sos/SOS"));
//    String2.log(EDDTableFromSOS.generateDatasetsXml(
//        "http://mvcodata.whoi.edu:8080/q2o/adcp", "1.0"));
//    EDDTableFromSOS.testErddapSos();
//    EDDTableFromSOS.testGetStationTable();
//    EDDTableFromSOS.testOostethys();
//    EDDTableFromSOS.testNeracoos();
//    EDDTableFromSOS.testNdbcSosCurrents("");
//    EDDTableFromSOS.testNdbcSosSalinity("");  
//    EDDTableFromSOS.testNdbcSosLongTime("");
//    EDDTableFromSOS.testNdbcSosWaves("");
//    EDDTableFromSOS.testNdbcSosWind("");   //BROKEN 2010-06-07  incorrect # columns
//    EDDTableFromSOS.testNdbcSosWLevel(""); //sea_floor_depth_below_sea_surface 
//    EDDTableFromSOS.testNdbcSosWTemp("");  
//    EDDTableFromSOS.testGenerateDatasetsXmlFromOneIOOS();
//    EDDTableFromSOS.testGenerateDatasetsXmlFromIOOS();
//    EDDTableFromSOS.testNdbcTestServer("");
//    EDDTableFromSOS.testNdbcSosBig("");
//    EDDTableFromSOS.testNosSosATempStationList("");
//    EDDTableFromSOS.testNosSosCurrents("");
//    EDDTableFromSOS.testNosSosSalinity("");
//    EDDTableFromSOS.testNosSosWind("");
//    EDDTableFromSOS.testNosSosWTemp("");
//    EDDTableFromSOS.testNosTestServer("");
//    EDDTableFromSOS.testTamu();
//    EDDTableFromSOS.testWhoiSos(); 

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
//    EDDTableFromThreddsFiles.testGenerateDatasetsXml();
//    EDDTableFromThreddsFiles.testGetThreddsFileInfo();
//    EDDTableFromThreddsFiles.testShipWTEP(false); //deleteCachedInfo
//    EDDTableFromWFSFiles.testGenerateDatasetsXml();
//    EDDTableFromWFSFiles.testBasic();

//    EDDTableReplicate.testReplicatePostDet(false);  //defaultCheckSourceData (faster)
//    EDStatic.test();
//    EDUnits.testUdunitsToUcum();
//    EDUnits.testUcumToUdunits();
//    EDUnits.testRoundTripConversions();
//    EDUnits.checkUdunits2File("c:/programs/udunits-2.1.9/lib/udunits2-derived.xml");  //one time
//    EDUnits.makeCrudeUcumToUdunits();  //one time

//    String2.log(File2.hexDump("c:/downloads/sendaiFail.dods", 1000000000));

      //Run the GenerateDatasetsXml program in interactive mode:
//    GenerateDatasetsXml.main(null);
      /*  NOT MAINTAINED:
          EDDGridFromDap.testOldGenerateDatasetsXml();
          EDDGridFromNcFiles.testOldGenerateDatasetsXml();
          EDDTableFromAsciiFiles.testOldGenerateDatasetsXml();
          EDDTableFromDapSequence.testOldGenerateDatasetsXml();
          EDDTableFromDatabase.testOldGenerateDatasetsXml();
          //EDDTableFromHyraxFiles.testOldGenerateDatasetsXml();  //Class is inactive
          EDDTableFromNcFiles.testOldGenerateDatasetsXml();
          //obis   see new version        
           EDDTableFromThreddsFiles.testOldGenerateDatasetsXml();
      */
      /*
         EDDGridFromDap.testGenerateDatasetsXml();
         EDDGridAggregateExistingDimension.testGenerateDatasetsXml();  //after EDDGridFromDap
         EDDGridFromErddap.testGenerateDatasetsXml();  
         EDDGridFromNcFiles.testGenerateDatasetsXml();
         EDDTableFromAsciiFiles.testGenerateDatasetsXml();
         EDDTableFromDapSequence.testGenerateDatasetsXml();
         EDDTableFromDatabase.testGenerateDatasetsXml();
         EDDTableFromErddap.testGenerateDatasetsXml(); 
         EDDTableFromHyraxFiles.testGenerateDatasetsXml(); 
         EDDTableFromNcFiles.testGenerateDatasetsXml();
         EDDTableFromOBIS.testGenerateDatasetsXml();
         EDDTableFromSOS.testGenerateDatasetsXml();
         EDDTableFromSOS.testGenerateDatasetsXmlFromOneIOOS();
         EDDTableFromSOS.testGenerateDatasetsXmlFromIOOS();
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
//    Grid.testGrd(); //    test first since others rely on it
//    Grid.testReadGrdSubset();
//    GridDataSetThredds.main(null);
//    GridDataSetThredds.quickTest("GA", "ssta");
//    GridSaveAs.main(new String[]{"c:/u00/data/AG/1day/grd/.grd.zip", "C:/u00/data/AG/1day/nc/.nc"}); //one time set up for GenerateThreddsDataSetHtml
//    GridSaveAs.main(new String[]{"C:/temp/dave/PH2006001_2006008_ssta.grd", "C:/temp/dave/PH2006001_2006008_ssta.nc"}); 
//    GridSaveAs.main(new String[]{"c:/u00/data/MC/mday/grd/.grd", "C:/u00/data/MC/mday/nc/.nc"});
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
//    NetcdfDataset in = NetcdfDataset.openDataset(
//        //"http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/HadleyCenter/HadISST");
//        "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdHadISST");
//    System.out.println("netcdfDataset=" + in.toString());
//    FeatureType featureType = FeatureDatasetFactoryManager.findFeatureType(in);
//    System.out.println(featureType.toString());

//    NetCheck.verbose = true;
//    NetCheck nc = new NetCheck("c:/content/bat/NetCheck.xml", true); //testmode
//    OpendapHelper.testAllDapToNc(-1); //-1 for all tests, or 0... for just one
//    OpendapHelper.testGetAttributes();
//    OpendapHelper.testParseStartStrideStop();
//    OpendapHelper.testFindAllScalarOrMultiDimVars();
//    OpendapHelper.testFindVarsWithSharedDimensions();
//    OpendapHelper.testDapToNcDArray();
//    Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
//        Matcher matcher = pattern.matcher("test 2009-01-02 12:13:14abcaaaaab");
//        if (matcher.find(1)) String2.log("matched at start=" + matcher.start());
//        else String2.log("didn't match");
//    post.TestJdbc.test();
//    PrimitiveArray.testTestValueOpValue();
//    Projects.calcofiBio();
//    Projects.calcofiSub();
//    Projects.calcofiSur();
//    Projects.channelIslands();
//    String2.log(File2.hexDump("c:/temp/ndbc/NDBC_46023_met.asc", 300));
//    Projects.convertCchdoBottle();  //woce
//    Projects.convertFedCalLandings();
//    Projects.nodcPJJU(
//        "c:/data/nodcPJJU/",  //files curl'd from source
//        "c:/u00/data/points/nodcPJJU/");  //destination
//    DasDds.main(new String[]{"erdHadISST", "-verbose"});
//    DasDds.main(new String[]{"nodcWOD", "-verbose"});
//    Projects.splitRockfish();
//    Projects.convertGlobecCsvToNc();
//    Projects.convertNewportCTD();
//    Projects.convertPrbo201001();
//    Projects.convertRockfish20130328();
//    Projects.convertRockfish20130409(false);  //headerMode
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
//    Projects.makeCRWNcml34("2000-12-02", 3, "2000-12-05", "dhw"); //sst, anomaly, dhw, hotspot, baa
//    Projects.makeCRWToMatch("baa");
//    Projects.makeCRWNcml34("2013-12-19", 4, "2014-12-31", "baa"); //sst, anomaly, dhw, hotspot, baa
//    Projects.makeSimpleTestNc();
//    Projects.makeVH1dayNcmlFiles(2012, 2035);
//    Projects.makeVH8dayNcmlFiles(2012, 2035);
//    Projects.makeVHmdayNcmlFiles(2012, 2035);
//    Projects.tallyUafAggregations("c:/programs/tomcat/content/erddap/datasetsUAF.xml");

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
//    Projects.fixKeywords("c:/programs/tomcat/content/erddap/datasetsUAF.xml");
//    Projects.getCAMarCatShort();
//    Projects.getCAMarCatLong();
//    Projects.getChristinaShowsData();
//    Projects.kfm3();
//    Projects.kfmBiological();
//    Projects.kfmBiological200801();
//    Projects.kfmFishTransect200801();
//    Projects.kfmSizeFrequency200801();
//    Projects.kfmSpeciesNameConversion200801();
//    Projects.kfmTemperature200801();
//    Projects.kushner();
//    Projects.makeIsaacNPH();
//    Projects.makeIsaacPCUI();
//    Projects.makeNetcheckErddapTests(
//        "http://coastwatch.pfeg.noaa.gov/erddap/");
//        "http://upwell.pfeg.noaa.gov/erddap/");
//        "http://75.101.155.155/erddap/");
//    Projects.processCalcofi2012();
//    Projects.ssc();
//    Projects.soda("1.4.2", "c:/SODA_1.4.2/", "c:/SODA_1.4.2/");
//    Projects.soda("1.4.3", "c:/SODA_1.4.3/", "c:/SODA_1.4.3/");
//    Projects.soda("2.0.2", "c:/soda.2.0.2/", "c:/soda.2.0.2/");
//    Projects.soda("2.0.2", "c:/soda.2.0.3/", "c:/soda.2.0.3/"); //yes 2.0.2
//    Projects.soda("2.0.4", "\\\\Xserve\\pfel_share\\Dave2Roy\\", "\\\\Xserve\\pfel_share\\BobSimons\\soda204\\"); 
//    Projects.soda("2.0.4", "c:\\temp\\sodain\\", "c:\\temp\\sodain\\"); 
//    Projects.testGetNcGrids();
//    Projects.testHashFunctions();  
//    Projects.testHashMaps();  
//    Projects.testHdf4();
//    Projects.testJanino();
//     :8081 led to out-of-date oceanwatch dataset!!  but now disabled
//    Projects.testOpendapAvailability("http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday", 
//        "CMusfc", 5, 1, true); //nIter, maxSec
//    Projects.testOpendapAvailability("http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdCMsfc", 
//        "eastCurrent", 20, 5*60, false); //nIter, maxSec
//    Projects.testOpendapAvailability("http://thredds1.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday", 
//        "CMusfc", 20, 5*60, true); //nIter, maxSec
//    File2.deleteAllFiles("C:/temp/tmptmp", true, true);
//    Projects.testWobblyLonLat();
//    Projects.touchUrls();
 
//    Projects2.copyKeywords();
//    Projects2.copyKeywordsUsgs();
//    Projects2.getKeywords("erdGtsppBest");
//    Projects2.touchUsgs();


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
//    SaveOpendap.test(); 
//    SgtGraph.test();
//    SgtGraph.testForMemoryLeak();
//    SgtMap.createBathymetryMatlabFile(-135, -114, 29, 50, 0.025, "c:/temp/luke/");
//    SgtMap.makeAdvSearchMapBig();
//    SgtMap.test(true, true); 
//    SgtMap.testCreateTopographyGrid();
//    SgtMap.testMakeCleanMap(0, 6);  //all: 0, 6
//    SgtMap.testOceanPalette(0, 7); //all: 0, 7
//    SgtMap.testBathymetry();
//    SgtMap.testTopography(0, 12); //all: 0, 11
//    SgtMap.testRegionsMap(0, 360, -90, 90);
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
//(new Table()).readASCII("c:/data/ndbc/ndbcMetHistoricalTxt/4f887h2009.txt"); //really small
//(new Table()).readASCII("c:/data/ndbc/ndbcMetHistoricalTxt/41009h1988.txt"); //small
//(new Table()).readASCII("c:/data/ndbc/ndbcMetHistoricalTxt/41009h1989.txt");
//(new Table()).readASCII("c:/data/ndbc/ndbcMetHistoricalTxt/41009h1990.txt"); //large      
//Table table = new Table();
//table.readASCII("C:/data/regina/DCU5.nc");
//String2.log(table.toCSVString());
//    Table.test4DNc(); 
//    Table.testHtml(); 
//    Table.testIobis();
//    Table.testJoin();
//    Table.testJson();
//    Table taoTable = new Table();
//        taoTable.readJson("tao", SSR.getUrlResponseString(
//            EDStatic.erddapUrl + "/tabledap/pmel_dapper/tao.json?longitude,latitude,altitude,time,station_id,sea_surface_temperature,sea_surface_temperature_quality,air_temperature,air_temperature_quality,relative_humidity,relative_humidity_quality,relative_humidity_source,wind_to_direction,wind_direction_quality,wind_direction_source,wind_speed,wind_speed_quality,wind_speed_source,eastward_wind,northward_wind&time>=2007-08-01&time<=2007-10-01"));
//        Math2.gcAndWait(); String2.log(" done " + Math2.memoryString());
//    Table.testLastRowWithData();
//    Table.testMdb();
//    Table.testOrderByMinMax();
//    Table.testReadAwsXmlFile();
//    boolean pauseAfterEach = false;
//      Table table = new Table(); table.readNcCF(
//          "/data/glider/GP05MOAS-Glider-001-profiles.nc3.nc",
//          null, //StringArray.fromCSV(""),
//          null, null, null);
//      table.leftToRightSort(5);
//      String2.log(table.toCSVString(10));
//    Table.testReadNcCF1(pauseAfterEach);
//    Table.testReadNcCF2(pauseAfterEach);
//    Table.testReadNcCFASAProfile(pauseAfterEach);
//    Table.testReadNcCFASATimeSeries(pauseAfterEach);
//    Table.testReadNcCFASATrajectory(pauseAfterEach);
//    Table.testReadNcCFASATimeSeriesProfile(pauseAfterEach);
//    Table.testReadNcCFASATrajectoryProfile(pauseAfterEach);
//    Table.testReadNcCFMATimeSeriesReversed();
//    Table.testReadNDNc();
//    Table.testReadNDNc2();
//    Table.testReadNDNcSpeed();
//    Table.testReadASCIISpeed();  
//    Table.testReadJsonSpeed(); 
//    Table.testReadNDNcSpeed();
//    Table.testReadOpendapSequenceSpeed();
//    Table.testReadStandardTabbedASCII();  
//    Table.testReorderColumns();
//    Table.testSaveAsSpeed();
//    Table.testSortColumnsByName(); 
//    Table.testUpdate();
//    Table.testXml();
//    TestListFiles.main(new String[]{"c:/"});
//    TestNCDump.main(new String[]{"c:/temp/CM2006171_230000h_u25h.nc"});
//    TestSSR.runNonUnixTests();
//    TestSSR.testEmail();
//    TestSSR.testEmail("bob.simons@noaa.gov", "");  //remove password after testing!!!
//    TestUtil.testCalendar2();
//    TestUtil.testFile2();
//    TestUtil.testFileWriteSpeed();
//    TestUtil.testTest();
//    TestUtil.testWriteToFileSpeed();
//    TestUtil.testReadFromFileSpeed();
//    TestUtil.testMustBe();
//    TestUtil.testMath2();
//    TestUtil.testString2();
//    TestUtil.testString2canonical();
//    TestUtil.testString2utf8();
//    TestUtil.timeCurrentTimeMillis();
//    TestUtil.timeString2Log();
//    TestUtil.testString2LogOutputStream();
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
dods.dap.DSequence ds;
DoubleArray doublea;
EDDTableFromAllDatasets etfad;
EmaAttribute ea;
EmaClass ec;
EmaColor ecolor;
File2 f2;
FileNameUtility fnu;
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
EDDGridFromDap eddgfd;  
//EDDGridFromBinaryFile eddgfbf;  //not active
EDDGridFromErddap eddgfed;  
EDDGridFromEtopo eddgfe;  
EDDGridFromFiles eddgff;  
EDDGridFromNcFiles eddgfncf;  
EDDGridSideBySide eddgsbs;  
EDDTable eddTable; 
EDDTableCopy eddtc;
EDDTableCopyPost eddtcp;
EDDTableFromAsciiService eddtfas;
EDDTableFromAsciiServiceNOS eddtfasn;
//EDDTableFromBMDE eddtfb; //inactive
EDDTableFromDapSequence eddtfds; 
EDDTableFromDatabase eddtfdb; 
EDDTableFromEDDGrid eddtfeg; 
EDDTableFromErddap eddtfed;
EDDTableFromFiles eddtff; 
EDDTableFromAsciiFiles eddtfaf;
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
        RegexFilenameFilter.main(null);  
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
            String2.getStringFromSystemIn(
                MustBe.throwableToString(e) +
                "\nPress ^C to stop or Enter to continue..."); 
        }
        Math2.gcAndWait(); 

        //data
        DataStream.main(null);
        SimpleXMLReader.main(null);
        TimePeriods.test();
        FileNameUtility.main(null);
        ParseJSON.test();

        //test validity of DataSet.properties
        try {
            ValidateDataSetProperties.main(null);
        } catch (Exception e) {
            String2.getStringFromSystemIn(MustBe.throwableToString(e) +
                "\nPress ^C to stop or Enter to continue..."); 
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
            String2.log(MustBe.throwableToString(e));
            String2.getStringFromSystemIn("\nRecover from opendap failure? Press 'Enter' to continue or ^C to stop...");
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

        Table.main(null);  
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
        // SgtGraph.testSurface();  //not finished!
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
        boolean doGraphicsTests = true;
        boolean doLongTest = false;

        EDDGridFromDap.test(false); //doGraphicsTests);
        // EDDGridFromDap.testGraphics(); //do just before releases    
        //EDDGridFromBinaryFile.test(); not finished
        EDDGridFromErddap.test(); 
        EDDGridFromEtopo.test(true);
        //EDDGridAggregateExistingDimension.test();  //don't usually run...very slow
        EDDGridAggregateExistingDimension.testGenerateDatasetsXml();
        EDDGridFromNcFiles.test(true);
        EDDGridCopy.test();
        EDDGridSideBySide.test(true); //doGraphicsTests);  //the best grid graphics tests are here

        EDDTableFromFiles.test(); 
        EDDTableFromNcFiles.test(true); //doGraphicsTests); //the best table graphics tests are always done
        EDDTableFromNcCFFiles.test();  
        EDDTableFromHyraxFiles.test(); 
        EDDTableFromEDDGrid.test();
        EDDTableFromDapSequence.test(); 
        //EDDTableFromDapSequence.testMemory(); //don't usually run...very slow
        EDDTableFromDatabase.test();     
        //EDDTableFromPostDatabase.test(); //INACTIVE.    very slow?
        EDDTableFromAsciiServiceNOS.test(false); 
        EDDTableFromErddap.test(); 
        EDDTableFromAsciiFiles.test(false); //rarely: true=delete cached info
        EDDTableFromAwsXmlFiles.test();
        EDDTableFromThreddsFiles.test(false); //rarely: true=delete cached info
        //EDDTableFromMWFS.test(false); //doLongTest); //as of 2009-01-14 no longer active
        //EDDTableFromNOS.test(false); //doLongTest); //as of 2010-09-08 no longer active
        //EDDTableFromNWISDV.test();  //INACTIVE as of 2011-12-16.
        EDDTableFromOBIS.test();
        //EDDTableFromBMDE.test(); //INACTIVE
        EDDTableFromSOS.test();
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
                    String2.getStringFromSystemIn("OceanWatch Thredds too slow: " + time9 +
                        "\nPress ^C to stop or Enter to continue..."); 
            }
            //don't run often
            Opendap.threddsTunnelTest(200, 
                "http://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday",
                "CMusfc"); 
        } catch (Exception e) {
            String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
                "\nUnexpected oceanwatch error: Press ^C to stop or Enter to continue..."); 
        }
        
        //INACTIVE: a test of thredds1 THREDDS 8081
        //try {
        //    for (int i = 0; i < 5; i++) {
        //        long time9 = System.currentTimeMillis();
        //        Opendap.doThredds1_8081SpeedTests(false, true); //dotTest, asciiTest
        //        time9 = System.currentTimeMillis() - time9;
        //        if (i > 0 && time9 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
        //            String2.getStringFromSystemIn("Thredds1 8081 Thredds too slow: " + time9 +
        //                "\nPress ^C to stop or Enter to continue..."); 
        //    }
        //    //don't run often
        //    Opendap.threddsTunnelTest(200, 
        //        "http://thredds1.pfeg.noaa.gov:8081/thredds/dodsC/satellite/CM/usfc/hday",
        //        "CMusfc"); 
        //} catch (Exception e) {
        //    String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
        //        "\nUnexpected THREDD1 8081 ERROR. Press ^C to stop or Enter to continue..."); 
        //}

        //INACTIVE: a test of otter THREDDS 8081  (should run great)
        //try {
        //    for (int i = 0; i < 5; i++) {
        //        long time8 = System.currentTimeMillis();
        //        Opendap.doOtterSpeedTests(false, false, 8081); //dotTest, asciiTest
        //        time8 = System.currentTimeMillis() - time8;
        //        if (i > 0 && time8 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
        //            String2.getStringFromSystemIn("Otter Thredds 8081 too slow: " + time8 +
        //                "\nPress ^C to stop or Enter to continue..."); 
        //    }
        //    //don't run often
        //    Opendap.threddsTunnelTest(200, 
        //        "http://161.55.17.243:8081/thredds/dodsC/satellite/CM/usfc/hday", //otter
        //        "CMusfc"); 
        //} catch (Exception e) {
        //    String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
        //        "\nUnexpected otter 8081 error: Press ^C to stop or Enter to continue..."); 
        //}

        //INACTIVE:  a test of otter THREDDS 8087
        //try {
        //    for (int i = 0; i < 5; i++) {
        //        long time8 = System.currentTimeMillis();
        //        Opendap.doOtterSpeedTests(false, false, 8087);  //dotTest, asciiTest
        //        if (true) throw new Exception("SHOULDN'T GET HERE.");
        //        time8 = System.currentTimeMillis() - time8;
        //        if (i > 0 && time8 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
        //            String2.getStringFromSystemIn("Otter Thredds 8087 too slow: " + time8 +
        //                "\nPress ^C to stop or Enter to continue..."); 
        //    }
        //    //don't run often
        //    Opendap.threddsTunnelTest(200, 
        //        "http://161.55.17.243:8087/thredds/dodsC/satellite/CM/usfc/hday", //otter
        //        "CMusfc"); 
        //} catch (Exception e) {
        //    String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
        //        "\nOTTER 8081 STARTED CAN'T CONNECT ERROR TO AGssta3day ON 2009-09-10" +
        //        "\nUnexpected otter 8087 error: Press ^C to stop or Enter to continue..."); 
        //}

        //a test of erddap
        try {
            for (int i = 0; i < 5; i++) {
                long time9 = System.currentTimeMillis();
                Opendap.doErddapSpeedTests(false, false); //dotTest, asciiTest 
                time9 = System.currentTimeMillis() - time9;
                if (i > 0 && time9 > 3000) //2014-08 was 1000 in ERD building. Now 3000 from outside
                    String2.getStringFromSystemIn("Erddap too slow: " + time9 +
                        "\nPress ^C to stop or Enter to continue..."); 
            }
            //don't run often
            Opendap.threddsTunnelTest(200, 
                "http://coastwatch.pfeg.noaa.gov/erddap/griddap/erdCMsfc",
                "eastCurrent"); 
        } catch (Exception e) {
            String2.getStringFromSystemIn(MustBe.throwableToString(e) + 
                "\nUnexpected erddap error: Press ^C to stop or Enter to continue..."); 
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

