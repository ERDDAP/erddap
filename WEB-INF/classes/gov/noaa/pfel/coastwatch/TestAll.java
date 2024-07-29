/*
 * TestAll Copyright 2005, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.coastwatch;

import com.cohort.array.*;
import com.cohort.util.*;
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
import org.apache.commons.logging.impl.*;
import org.json.JSONObject;
import ucar.ma2.*;
import ucar.nc2.*;
// import ucar.nc2.dods.*;
import ucar.nc2.dataset.*;
import ucar.nc2.util.*;

/**
 * This is a very important class -- main() calls all of the unit tests relevant to CWBrowser and
 * ERDDAP. Also, compiling this class forces compilation of all the classes that need to be
 * deployed, so this class is compiled and and main() is run prior to calling makeCWExperimentalWar
 * (and ultimately makeCoastWatchWar) or makeErddapWar and deploying the .war file to Tomcat.
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2005-10-06
 */
public class TestAll {

  /**
   * This forces compilation of all the classes that need to be deployed and calls all of the unit
   * tests relevant to CWBrowser.
   *
   * @param args is ignored
   * @throws Throwable if trouble
   */
  public static void main(String args[]) throws Throwable {

    String s;

    // always setup commons logging
    String2.setupCommonsLogging(-1);

    // set log file to <bigParentDir>/logs/TestAll.out
    EDStatic.quickRestart = false; // also, this forces EDStatic instantiation when running TestAll
    String2.setupLog(
        true,
        false, // output to system.out and a file:
        EDStatic.fullLogsDirectory + "TestAll.log",
        false,
        1000000000); // append?
    EDD.testVerboseOn();
    String2.log(
        "*** Starting TestAll "
            + Calendar2.getCurrentISODateTimeStringLocalTZ()
            + "\n"
            + "logFile="
            + String2.logFileName()
            + "\n"
            + String2.standardHelpAboutMessage()
            + "\n"
            + "This must be run from a command line window because the SFTP and email tests ask for passwords.\n");

    // this might cause small problems for a public running erddap
    // but Bob only uses this on laptop, with private erddap.
    File2.deleteAllFiles(
        EDStatic.fullPublicDirectory, true, false); // recursive, deleteEmptySubdirectories
    File2.deleteAllFiles(EDStatic.fullCacheDirectory, true, false);

    // make it appear that initialLoadDatasets() is not true
    EDStatic.majorLoadDatasetsTimeSeriesSB.append("\n");

    // INDIVIDUAL TESTS (for convenience) -- ~alphabetical by class name

    // TestUtil.testString2();

    /*
    ByteArray.basicTest();
    UByteArray.basicTest();
    ShortArray.basicTest();
    UShortArray.basicTest();
    IntArray.basicTest();
    UIntArray.basicTest();
    LongArray.basicTest();
    ULongArray.basicTest();
    CharArray.basicTest();
    FloatArray.basicTest();
    DoubleArray.basicTest();
    StringArray.basicTest();
    PrimitiveArray.basicTest();
    PrimitiveArray.testNccsv();
    Attributes.basicTest();
    /* */

    //      String2.log(String2.fileDigest(true, "SHA-256",
    //       "/programs/_tomcat/webapps/cwexperimental/images/wz_dragdrop.js"));

    // "-h" (header), "-c" (coord. vars), "-vall" (default), "-v var1;var2", "-v var1(0:1,:,12)"
    //      String tFileName =
    // "/u00/satellite/MH1/chla/1day/AQUA_MODIS.20220301.L3m.DAY.CHL.chlor_a.4km.NRT.nc";
    //      String2.log(NcHelper.ncdump(tFileName, "-h"));

    //      DasDds.main(new String[]{"testMediaBob", "-verbose"});

    //      String2.log(EDDTableFromAsciiFiles.generateDatasetsXml("c:/data/bob/testMedia.csv",
    // ".*\\.csv",
    //        "", "", 1, 2, ",", 1000000000, "", "", "", "", "", "", "myInfo", "myInstitution",
    // "mySummary", "myTitle",
    //          0, "", null));
    //      GenerateDatasetsXml.main(new String[0]); //interactive

    //    { //find file in dataset with insane min_time
    //        Table table = new Table();
    //        table.readFlatNc("/downloads/fileTable.nc", null, 0); //it logs fileName and nRows=.
    // 0=don't unpack
    //        String2.log(table.getColumnNamesCSVString());
    //      //  table.justKeepColumns(new String[]{"fileList","min"}, "");
    //        table.tryToApplyConstraintsAndKeep(0,
    //            StringArray.fromCSV("min"),
    //            StringArray.fromCSV("="),
    //            StringArray.fromCSV("1480854360"));
    //        String2.log(table.dataToString());
    //    }

    //      Erddap.makeErddapContentZip("c:/programs/_tomcat/samples/", "c:/backup/");

    /*
    String c9 = //SSR.getUrlResponseStringNewline(
        //"https://podaac-opendap.jpl.nasa.gov/opendap/allData/ghrsst/data/GDS2/L4/GLOB/JPL/MUR/v4.1/2019/154/contents.html");
        "<time itemprop=\"dateModified\" datetime=\"2019-06-04T11:11:00\">2019-06-04T11:11:00</time>";
    String2.log(c9);
    Pattern dateTimePattern = Pattern.compile("itemprop=\"dateModified\" datetime=\"(.{19})\">");
    String lastModS = String2.extractCaptureGroup(c9, dateTimePattern, 1);
    String2.log("lastModS=" + lastModS);
    GregorianCalendar gc = Calendar2.parseISODateTimeZulu(lastModS);
    TimeZone pacTimeZone = TimeZone.getTimeZone("US/Pacific");
    gc = Calendar2.parseISODateTimeZulu(lastModS);
    long modTime = gc.getTimeInMillis();
    modTime += pacTimeZone.getOffset(modTime);
    String2.log("converted=" + Calendar2.epochSecondsToIsoStringT(modTime/1000));
    /* */

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

    //    String tFileName = String2.unitTestBigDataDir + "nccf/wod/wod_xbt_2005.nc";
    //    File2.writeToFileUtf8(tFileName + ".ncdump.txt", NcHelper.ncdump(tFileName, "-h"));

    //
    // String2.log(EDDGrid.findTimeGaps("https://coastwatch.pfeg.noaa.gov/erddap/griddap/nceiPH53sstn1day"));

    // try to validate ERDDAP's ISO19115 output in
    // https://xmlvalidation.com/
    /*{
        String dirName = "c:/downloads/test.xml";
        Writer writer = File2.getBufferedFileWriterUtf8(dirName);
        //EDD.oneFromDatasetsXml(null, "erdMHchla8day").writeFGDC(writer, null);
        EDD.oneFromDatasetsXml(null, "erdMHchla8day").writeISO19115(language, writer, null);
        writer.close();
        Test.displayInBrowser("file://" + dirName); //.xml
    }*/

    //      CCMP
    //      String2.log(EDDGridAggregateExistingDimension.generateDatasetsXml("hyrax",
    // there are alternatives to L3.5a
    //          "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/contents.html",
    // //flk (no llk data)
    //            "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.0/data/flk/contents.html",
    // //alternate start
    //          "analysis.*flk\\.nc\\.gz",   //analysis, pentad or month    //flk (no llk data)
    //          true, 1000000)); //recursive
    //      String2.log(EDDGridAggregateExistingDimension.generateDatasetsXml("thredds",
    //          "http://ourocean.jpl.nasa.gov:8080/thredds/dodsC/g1sst/catalog.xml",
    //          ".*\\.nc", true, 120)); //recursive
    //    String2.log(EDD.suggestDatasetID(
    //      "http://dods.jpl.nasa.gov/opendap/ocean_wind/ccmp/L3.5a/data/flk/contents.html?" +
    //      "month.*flk\\.nc\\.gz"));

    //    EDD.debugMode = true;
    //
    // String2.log(NcHelper.ncdump("/data/goes16/20190101000000-STAR-L3C_GHRSST-SSTsubskin-ABI_G16-ACSPO_V2.70-v02.0-fv01.0.nc",
    //        "-v sea_surface_temperature(0,0:10,0:10)")); //2nd param, e.g., "LAT;LON"));
    //    s = EDDGridFromDap.generateDatasetsXml(
    //
    // "http://opendap.oceanbrowser.net/thredds/dodsC/data/emodnet-domains/Coastal_areas/Northeast_Atlantic_Ocean_-_Loire_River/Water_body_silicate.nc",
    //      null, null, null, //new String[]{"time","altitude","lat","lon"}, //dimensions (or null)
    //      -1, null);
    //      String2.setClipboardString(s); String2.log(s);
    /* */
    //    DasDds.main(new String[]{"jplOISSSmm1_mday", "-verbose"});

    //    String tFileName =
    // "/programs/_tomcat/webapps/cwexperimental/download/setupDatasetsXml.html";
    //    String ts = File2.directReadFromUtf8File(tFileName + "Old");

    //
    //    Crawl UAF clean catalog:
    //      done 2012-10-17, 2012-12-09, 2013-11-06, 2014-03-31, 2014-12-18,
    //           2015-10-22, 2016-04-19, 2016-08-26, 2017-04-16 (fail)
    //           2017-06-19 (fail),
    //           2017-11-08/20 (new netcdf-based crawler, several crawls for various changes)
    //           2017-12-02 (resolution and timeRange in title, and small changes)
    //           2017-12-08 (small changes)
    //           2018-01-26 for testOutOfDate, failed: too many datasets missing
    //           2018-01-30 new catalog from Roland
    //           2018-04-16 try again
    //           2018-07-12 new catalog
    //           2018-08-24 esrl changed a lot
    //      0) 2017-11-14 UAF clean catalog STILL refers to http:// urls when it could
    //         refer to https:// urls, e.g.,
    //
    // https://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/catalog/ncom/ncom_reg1_agg/catalog.html?dataset=ncom.ncom_reg1_agg.NCOM_Region_1_Aggregation_best.ncd
    //         I have told Roland before. He hasn't responded or made changes so don't
    //         notify him again.
    //         GenerateDatasetsXml converts to https when possible via EDD.updateUrls.
    //      1) run:
    //           EDD.verbose = true;
    //           EDDGridFromDap.testUAFSubThreddsCatalog(0); //0= entire official clean catalog
    // ~16hrs
    //      2) Results file is /erddapBPD/logs/UAFdatasets[uafi]_[dateTime].xml
    //         Log file is     /erddapBPD/logs/UAFdatasets[uafi]_[dateTime].xml.log.txt
    //      3) Look at problems, creator_, title, .... Make improvements.
    //         from /erddapBPD/logs:
    //         grep "SimpleException: Error while getting DAS from"
    //         grep "unable to get axis"
    //         grep "unsorted axis"
    //         grep "no colorBarMin/Max"
    //         grep "ioos_category=Unknown for"
    //         grep "! Calendar2.tryToIsoString was unable to find a format for"
    //         Look for "error", ,
    //         Sort it.
    // Next time: improve standardization of 'institution' in EDD.makeReadyToUseAddGlobalAttributes.
    //
    //    EDDGridFromDap.testUAFSubThreddsCatalog(17);  //test one sub catalog
    //    for (int uafi = 6; uafi < EDDGridFromDap.UAFSubThreddsCatalogs.length; uafi++) {
    //        String2.log("\n\n************************************* UAFI=" + uafi);
    //        EDDGridFromDap.testUAFSubThreddsCatalog(uafi);  //test one sub catalog
    //    }
    //    extract acronyms from a text file
    //      Object ar[] = String2.findAcronyms(
    //          File2.readFromFile("/Temp/datasetsUAF0_20150505161356.xml")[1]).toArray();
    //      Arrays.sort(ar);
    //      String2.log(String2.toNewlineString(ar));

    // create an invalid .nc file
    //    byte tb[] = SSR.getFileBytes("/u00/satellite/MW/cdom/1day/MW2012072_2012072_cdom.nc");
    //    OutputStream os = new BufferedOutputStream(new
    // FileOutputStream("/erddapTest/nc/invalidShortened2.nc"));
    //    os.write(tb, 0, tb.length / 10000);
    //    os.close();
    //
    //    s = EDDGridFromNcFilesUnpacked.generateDatasetsXml(
    //        "/data/andy/", //10mWinds 20mWinds 500MbHeight pressure
    //        "pilot.*\\.bufr", //US.*wnd_ucmp,  or vcmp, or ""
    //        "", "", "", //sampleFile, group, dimensionsCSV
    //        -1, "", null);
    //    String2.setClipboardString(s); String2.log(s);
    //    DasDds.main(new String[]{"erdNavgem05DPres", "-verbose"});

    // tests of _Unsigned=true, maxIsMV, addFillValueAttributes, EDD.addMvFvAttsIfNeeded,
    // EDV.suggestAddFillValue:
    //  and of course all of the tests of GenerateDatasetsXml.
    //    EDD.testAddMvFvAttsIfNeeded();     //in service to GenerateDatasetsXml
    //    EDD.testAddFillValueAttributes();  //add to datasets.xml
    //    EDDGridFromDap.testGenerateDatasetsXmlUInt16();
    //    EDDGridFromNcFiles.testUInt16File();
    //    EDDGridFromNcFiles.testUnsignedGrid();
    //    EDDGridFromNcFiles.testGenerateDatasetsXmlGroups();
    //    EDDGridFromNcFilesUnpacked.testUInt16File();
    //    EDDTableFromDapSequence.testGenerateDatasetsXml2(); //requires testNccsvScalar in
    // localhost ERDDAP
    //    EDDTableFromNccsvFiles.testBasic(true);
    //    EDDTableFromNccsvFiles.testChar();
    //    EDDTableFromNcFiles.testSimpleTestNc2Table(); //suggestAddFillValue
    //    //known problems that are not my problem, so still good tests:
    //    EDDGridFromNcFiles.testSimpleTestNc();        //suggestAddFillValue  //Known Problem  Sgt
    // doesn't support 2 time axes
    //    EDDGridFromDap.testUInt16Dap(); //trouble with source 255 or 65535
    //    EDDGridFromNcFilesUnpacked.testMissingValue();

    //    String2.log(String2.noLongLines(NcHelper.ncdump(
    //        "/u00/satellite/SW1/1day/S1998002.L3m_DAY_CHL_chlor_a_9km.nc",
    //        "-h"), 80, ""));
    // "lat"), 80, ""));
    //    String2.log(EDD.testDasDds("erdMBsstd1day"));
    //    while (ds.length() > 0) {
    //        ds = String2.getStringFromSystemIn("datasetID?");
    //        String2.log(EDD.testDasDds(ds));
    //    }

    //    s = EDDGridFromNcFiles.generateDatasetsXml("/data/raju/", "O2_.*.hdf", "", "",
    // "Scans,Pixels", -1, "", null);
    //    String2.setClipboardString(s);
    //    String2.log(s);
    //    DasDds.main(new String[]{"raju", "-verbose"});

    /*  //testFiles
        EDDGridFromNcFiles.testFiles();         //requires nceiPH53sstn1day in localhost ERDDAP
        EDDTableFromAsciiFiles.testFiles();     //testTableAscii
        EDDGridFromErddap.testFiles();          //nceiPH53sstn1day and testGridFromErddap
        EDDTableFromErddap.testFiles();         //testTableAscii and testTableFromErddap
        EDDGridCopy.testFiles();                //testGridCopy
        EDDTableCopy.testFiles();               //testTableCopy
        EDDGridLonPM180.testFiles();            //erdMWchlamday and erdMWchlamday_LonPM180
        EDDGridFromEDDTable.testFiles();        //testGridFromTable
        EDDTableFromEDDGrid.testFiles();        //erdMBsstdmday_AsATable and erdMBsstdmday
        EDDGridSideBySide.testFiles();          //erdTAgeo1day
        EDDGridAggregateExistingDimension.testFiles(); //nceiOisst2Agg
        EDDGridFromEtopo.testFiles("etopo180"); //etopo180
        EDDGridFromEtopo.testFiles("etopo360"); //etopo360
    */

    //    s = Projects.makeAwsS3FilesDatasets(".*"); //all: .*   tests: nrel-pds-wtk\\.yaml,
    // silo\\.yaml (has ?Name: but ? doesn't show in EditPlus)
    //    File2.writeToFileUtf8(EDStatic.bigParentDirectory + "logs/awsS3Files.txt", s);
    //    String2.log(EDD.testDasDds("radar_vola"));

    /*
        s = EDDGridFromNcFiles.generateDatasetsXml(
        "/u00/satellite/MH1/sstMask/1day/", "AQUA_MODIS.\\d{8}.L3m.DAY.SSTMasked.sst.4km.NRT.nc", "",
        -1, "", null);
        String2.setClipboardString(s);
        String2.log(s);
    /* */
    //    String2.log(EDD.testDasDds("erdMH1sstNotMasked1day"));
    //    Projects.tallyGridValues(
    //        "/u00/satellite/PH53/1981/data/" +
    //
    // "19810826023552-NCEI-L3C_GHRSST-SSTskin-AVHRR_Pathfinder-PFV5.3_NOAA07_G_1981238_night-v02.0-fv01.0.nc",
    //        "sea_surface_temperature", 0.01);
    //    Projects.findUnfinishedRequests("I:/logArchivedAt2022-09-27T12.52.00_shed0.txt");

    //    *** Daily
    //    Projects.viirsLatLon(true); //create

    //    s = EDDGridLonPM180.generateDatasetsXmlFromErddapCatalog(
    //        "https://coastwatch.pfeg.noaa.gov/erddap/", ".*");
    //    String2.setClipboardString(s);
    //    String2.log(s);
    //    EDDGridLonPM180.testHardFlag();

    //    s = EDDGridLon0360.generateDatasetsXmlFromErddapCatalog(
    //        "https://coastwatch.pfeg.noaa.gov/erddap/", ".*");
    //    String2.setClipboardString(s);
    //    String2.log(s);

    //    String2.log(EDDTableFromAsciiFiles.generateDatasetsXml(
    //        "/u00/data/points/ndbcMet2Csv/", ".*\\.csv", "",
    //        "", 1, 3, ",", 10080, //colNamesRow, firstDataRow, colSeparator, reloadEvery
    //        "", "", "", "", "",  //regex
    //        "", // tSortFilesBySourceNames,
    //        "", "", "", "", Integer.MAX_VALUE, "", null));  //info, institution, summary, title,
    // standardizeWhat=0, cacheFromUrl, atts

    //    Sync with various remote directories
    //    FileVisitorDNLS.reallyVerbose = true; FileVisitorDNLS.debugMode = true;
    //    FileVisitorDNLS.sync(
    //        "someUrl",  //omit /catalog.html
    //        "g:/",
    //        ".*\\.xml", false, ".*", true).dataToString(); //fileRegex, recursive, pathRegex,
    // doAll (or just 1)

    /*
    //This downloads AWS S3 directory into to a jsonlCSV file
    //The lower level copy (written to file as it is downloaded) is in erddapBPD/datasets/_GenerateDatasetsXml
    FileVisitorDNLS.verbose=true;
    FileVisitorDNLS.reallyVerbose=true;
    FileVisitorDNLS.debugMode=true;
    String outName =
        "/data/s3/awsS3NoaaGoes17partial.jsonlCSV";
        //"/data/s3/awsS3NoaaGoes16.jsonlCSV";
        //"/data/s3/awsS3NasanexNexDcp30rlilpl.jsonlCSV";
        //"/data/s3/awsS3NasanexNexDcp30Conus.jsonlCSV";
    FileVisitorDNLS.oneStep(    //throws IOException if "Too many open files"
        "https://noaa-goes17.s3.us-east-1.amazonaws.com/", //ABI-L1b-RadC/2018/338/",
        //"https://noaa-goes16.s3.us-east-1.amazonaws.com/",
        //"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/",
        //"https://nasanex.s3.us-west-2.amazonaws.com/NEX-DCP30/BCSD/rcp26/mon/atmos/tasmin/r1i1p1/v1.0/CONUS/",
        ".*\\.nc", true, ".*", true).writeJsonlCSV(outName); //dirsToo=false
    String2.log(File2.directReadFrom88591File(outName));
    String2.log("The Results are in " + outName + "\n" +
        "The lower level (partial?) copy (written to file as it is downloaded)\n" +
        "is in erddapBPD/datasets/_GenerateDatasetsXml .");
    /* */

    //    Do this periodically to update the local cache of InPort xml files
    //      Last done: 2017-08-09, now /inport-xml/
    //        was 2016-09-22 /inport/
    //      Local files that aren't on server aren't deleted.
    //        Delete by hand if desired. Or, delete local directory before running this.
    //    FileVisitorDNLS.sync("https://inport.nmfs.noaa.gov/inport-metadata/",
    //        "/u00/data/points/inportXml/",
    //        ".*\\.xml", //fileRegex, test was "1797.\\.xml"
    //        true, //recursive
    //        ".*/NOAA/(|NMFS/)(|[^/]+/)(|inport-xml/|fgdc/|iso19115/)(|xml/)", //pathRegex is
    // tricky! initial test: ".*/NOAA/(|NMFS/)(|NWFSC/)(|inport/)(|xml/)".
    //        true); //doIt

    //    Generate tallies of values in InPort files
    //    FileVisitorDNLS.findFileWith("/u00/data/points/inportXml/", ".*\\.xml", true, ".*",
    // //pathRegex
    //        ".*<catalog-item-type>(.*)</catalog-item-type>.*", 1, -1, 100); //lineRegex, capture
    // group#, interactiveNLines?
    //          ".*<data-set type=\"(.*)\".*", 1, -1, 100); //get an attribute
    //        ".*(<.*physical.*>).*", 1, -1, 100); //find a tag with a word
    //    FileVisitorDNLS.testAWSS3();
    //    FileVisitorDNLS.testReduceDnlsTableToOneDir();

    //    EDD.testInPortXml();
    //         types: Entity: 4811  (67%), Data Set: 2065  (29%), Document: 168  (2%), Procedure:
    // 113  (2%), Project: 29  (0%)
    //    String typesRegex = "(Entity|Data Set)";
    //    String ibdd = "/u00/data/points/inportData/";
    //    String2.log("\n" + EDDTableFromAsciiFiles.generateDatasetsXmlFromInPort(
    //         params: url, dirForXml, typeRegex, dirForData, fileName);
    //         "/u00/data/points/inportXml/NOAA/NMFS/AFSC/inport-xml/xml/36615.xml",  "",
    // typesRegex, 1, ibdd, "")); //a child table 36616
    //         "https://inport.nmfs.noaa.gov/inport-xml/item/12866/inport-xml",  "", typesRegex, 0,
    // ibdd, ""));
    //
    // "https://inport.nmfs.noaa.gov/inport-metadata/NOAA/NMFS/AFSC/inport-xml/xml/17336.xml", "",
    // typesRegex, 0, ibdd, ""));
    //    String2.log(EDDTableFromAsciiFiles.generateDatasetsXmlFromBCODMO(true, //tryToUseLocal?
    //       "https://www.bco-dmo.org/erddap/datasets", "/u00/data/points/bcodmo/", "(644080)"));
    //    Table.debugMode = true; DasDds.main(new String[]{"bcodmo549122_20150217", "-verbose"});

    // UPDATE nosCoops every 3 months: true,
    //  then copy /subset/nosCoops*.json files to coastwatch and UAF,
    //  and flag all the nosCoops datasets on coastwatch
    //        EDDTableFromAsciiServiceNOS.makeSubsetFiles(true);  //reloadStationFiles

    //    s = EDDTableFromDapSequence.generateDatasetsXml(
    //        "http://localhost:8080/cwexperimental/tabledap/testNccsvScalar.html",
    //        "https://opendap.co-ops.nos.noaa.gov/dods/IOOS/Raw_Water_Level",
    //        "http://www.neracoos.org/erddap/tabledap/D03_sbe_pres_all",
    //        10040, null);
    //        String2.setClipboardString(s); String2.log(s);

    /*   //tallyXml
       String tfn = EDStatic.fullLogsDirectory + "tallyLterSbsStorageUnitsMV.log";
       File2.writeToFileUtf8(tfn,
           FileVisitorDNLS.tallyXml(
           "/u00/data/points/lterSbc/", "knb-lter-sbc\\.\\d+", false,
           new String[]{
               "<eml:eml><dataset><dataTable><attributeList><attribute><measurementScale><dateTime></formatString>",
               //"<eml:eml><dataset><dataTable><attributeList><attribute></attributeName>"
               //"<eml:eml><dataset><dataTable><attributeList><attribute></storageType>",
               //"<eml:eml><dataset><dataTable><attributeList><attribute><measurementScale><nominal>",
               //"<eml:eml><dataset><dataTable><attributeList><attribute><measurementScale><ratio><numericDomain></numberType>"
               //"<eml:eml><dataset><dataTable><attributeList><attribute><missingValueCode></code>"
               //"<eml:eml><dataset><dataTable><physical><dataFormat><complex><textFixed>"
               }
               ).toString());
       Test.displayInBrowser("file://" + tfn);  //.log
    /* */
    // EDDTableFromDapSequence.testGenerateDatasetsXml2();

    /* //find files with a specific value for a specific tag
       FileVisitorDNLS.findMatchingContentInXml(
           "/u00/data/points/lterSbc/", "knb-lter-sbc\\.\\d+", false,
           "<eml:eml><dataset><dataTable><attributeList><attribute></attributeName>",
           ".*[Mm]atlab.*"); //matchRegex
    /* */

    //    EDDTableFromColumnarAsciiFiles.generateDatasetsXmlFromEML(false, //pauseForErrors
    //        "/u00/data/points/eml/",
    //
    // "https://knb.ecoinformatics.org/knb/d1/mn/v2/object/urn%3Auuid%3A5e80945d-fb96-4a74-a50a-08b0369c636c",
    //        true, "null", "US/Central"); //useLocalFiles, accessibleTo, localTimeZone
    //    Table.debugMode = true; DasDds.main(new String[]{"NTL_DEIMS_5672_t1", "-verbose"});

    //    make flag files for all knb datasets
    //    ArrayList<String> tsa = File2.readLinesFromFile("/downloads/allKnb.txt", File2.ISO_8859_1,
    // 1);
    //    int nTsa = tsa.size();
    //    String2.log("allKnb n=" + nTsa);
    //    for (int tsai = 0; tsai < nTsa; tsai++)
    //        File2.writeToFileUtf8("/flag/" + tsa.get(tsai), "flag");

    //    EDDTableFromHttpGet.testStatic();
    //    String2.log(EDDTableFromHyraxFiles.generateDatasetsXml(
    //        "https://data.nodc.noaa.gov/opendap/wod/monthly/APB/201103-201103/",
    //        "wod_01345934.O\\.nc",
    //        "https://data.nodc.noaa.gov/opendap/wod/monthly/APB/201103-201103/wod_013459340O.nc",
    //        10080,
    //        "", "", "", "",  //columnFromFileName
    //        "time", //String tSortedColumnSourceName,
    //        "time", //tSortFilesBySourceNames,
    //        null)); //externalAddAttributes)

    //    s = EDDTableFromNcCFFiles.generateDatasetsXml(
    //        "/data/andy/", "pilot.*\\.bufr",
    //        "", 1440, //sample file
    //        "", "", "",
    //        "", "",
    //        "", "", "", "", 0, "", new Attributes());
    //    String2.setClipboardString(s);  String2.log(s);
    //      Table.debugMode = true; DasDds.main(new String[]{"bridger2", "-verbose"});

    //    Table.debugMode = true;
    //    Table tTable = new Table();
    //        tTable.readNDNc(fileDir + fileName, sourceDataNames.toArray(),
    //            sortedSpacing >= 0 && !Double.isNaN(minSorted)? sortedColumnSourceName : null,
    //                minSorted, maxSorted,
    //            getMetadata);

    // String2.log(tTable.toCSVString());
    //
    //    String2.log(EDDTableFromNcFiles.generateDatasetsXml(
    //        "/data/andy/", "pilot.*\\.bufr",
    //        "",
    //        "", 1440,
    //        "", "", "",
    //        "", "",
    //        "",
    //        "", "", "", "", 0, "", new Attributes()));
    // String tFileDir, String tFileNameRegex, String sampleFileName, int tReloadEveryNMinutes,
    // String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
    // String tColumnNameForExtract, String tSortedColumnSourceName,
    // String tSortFilesBySourceNames,
    // String tInfoUrl, String tInstitution, String tSummary, String tTitle,
    // Attributes externalAddGlobalAttributes) throws Throwable {

    //    String2.log(NcHelper.ncdump("/u00/data/points/caricoos/181p1_historic.nc", "-v
    // metaStationLatitude;metaStationLongitude"));
    /*
      s = EDDTableFromMultidimNcFiles.generateDatasetsXml(
        "/data/bodc/", "Cabot.*\\.nc", "",
        "", //dims
        1440,
        "", "", "", "", //pre, post, extract, varname
        true, //removeMVRows  //often true
        "", //sort files by    profile_time
        "", "", "", "",
        0, //standardizeWhat 1+2(numericTime)+256(catch numeric mv)+4096(units)
        "", //treatDimensionsAs
        "", //cacheFromUrl  /catalog.html
        null) + "\n";
    String2.setClipboardString(s);  String2.log(s);
    // */
    //    Table.debugMode = true; DasDds.main(new String[]{"sarah3", "-verbose"});

    //    *** To update GTSPP (~10th of every month):
    // Don't add source_id or stream_ident: they are usually (always?) empty
    //    1) By hand (thanks (not), NCEI!), download newly changed files
    //      from https://www.ncei.noaa.gov/data/oceans/gtspp/bestcopy/netcdf/?C=M;O=D
    //          (2021-11-23 was: ftp.nodc.noaa.gov (name=anonymous  pwd=[my email address]),
    //          now see README at https://www.ncei.noaa.gov/data/oceans/gtspp/
    //          was from GTSPP dir: /nodc/data/gtspp/bestcopy/netcdf
    //          was /pub/data.nodc/gtspp/bestcopy/netcdf
    //      to my local: c:/data/gtspp/bestNcZip
    //      !!! Be sure do pre-delete older files, so you don't download as filename(1) .
    //      !!! Note that older files are reprocessed sometimes.
    //      !!! So sort by lastModified time to check if "older" files have a recent
    // last-modified-time.
    //      ??? THIS COULD BE AUTOMATED with FileVisitorDNLS!

    //    1b) Ensure Ramdisk r: exists.
    //      If not, run c:/Program Files/ImDisk/RamDiskUI.exe to create 100MB ram disk

    //    2) Overnight (or over lunch if just a few months of data) (still! because it's still
    // sluggish)
    //       unzip and consolidate the profiles
    //       Full run takes 16 hours with ramdisk (about 5 min to process 1 month's data,
    //           or 132K source files (in one tgz) in ~200 seconds = ~ 660 files/second)
    //           was 36 hours(?) then multiple days on Dell M4700 before ramdisk,
    //           was 2 days 14 hours on old Dell Opti).
    //       !!! CLOSE all other windows, even EditPlus.
    //       !!! EMPTY Recycle Bin
    //       !!! CHANGE "Run TestAll" MEMORY SETTING to 7GB
    //       EDDTableFromNcFiles.bobConsolidateGtsppTgz(2022, 3, 2022,  9, false);  //first/last
    // year(1985..)/month(1..), testMode  1985,02 is first time
    //       log file is c:/data/gtspp/logYYYYMMDD.txt
    //      2b) Email the "good" but "impossible" stations to Tim Boyer <tim.boyer@noaa.gov>,
    //         and "Christopher Paver - NOAA Federal (christopher.paver@noaa.gov)"
    // <christopher.paver@noaa.gov>
    //       [was Charles Sun, retired 2018-12]
    //       [was Melanie Hamilton, now retired]
    //       [start in 2011? but not longer valid 2012-10-19 Meilin.Chen@noaa.gov]
    //    3) In datasets2.xml, for erdGtsppBestNc, update the dates to the END processing date:
    //         (2 in history, 1 in summary)
    //       and in datasets2.xml and datasetsFEDCW.xml
    //         update the 2 history dates for erdGtsppBest
    //       to the date I started processing in step 2 above.
    //       (If processed in chunks, use date of start of last chunk.)
    //    4) Run:  (should fail at current calendar month)
    //          EDDTableFromNcFiles.testGtspp15FilesExist(1990, 2022);
    //    5) * In [tomcat]/content/erddap/subset/
    //          delete erdGtsppBestNc.json and erdGtsppBest.json
    //       * Load erdGtsppBestNc in localHost ERDDAP.     (20-40 minutes)
    //            or use this to see details of trouble:
    //            EDD tedd = EDD.oneFromDatasetsXml(null, "erdGtsppBestNc");
    // System.out.println(tedd.toString());
    //       * Look at emails to see if any Bad Files reported
    //       * Generate .json file from
    //
    // http://localhost:8080/cwexperimental/tabledap/erdGtsppBestNc.json?trajectory,org,type,platform,cruise&distinct()
    //         and save it as [tomcat]/content/erddap/subset/erdGtsppBestNc.json
    //       * Reload ERDDAP to ensure it loads quickly.
    //    6) Run and update this test:
    //       //one time: File2.touch("c:/data/gtspp/bestNcConsolidated/2011/09/2011-09_0E_0N.nc");
    // //one time
    //       //one time: EDDTableFromNcFiles.bobFindGtsppDuplicateCruises();
    //       EDDTableFromNcFiles.testErdGtsppBest("erdGtsppBestNc");  //~5 minutes
    //    7) Create ncCF files with the same date range as 2a) above:
    //       It takes ~20 seconds per month processed.
    //       It uses a local version of the dataset, not the one in localhost erddap.
    //       !!! CHANGE TestAll MEMORY SETTING to 7GB   //2016-10 is huge//
    //       EDDTableFromNcFiles.bobCreateGtsppNcCFFiles(2022, 3, 2022,  9); //e.g., first/last
    // year(1985..)/month(1..)
    //       String2.log(NcHelper.ncdump("/u00/data/points/gtsppNcCF/201406a.nc", "-h"));
    //    8) Run:  (should fail at current calendar month)
    //       EDDTableFromNcFiles.testGtsppabFilesExist(1990, 2022);
    //    9) * Load erdGtsppBest in localHost ERDDAP.  (1-10 minutes)
    //       * Generate .json file from
    //
    // http://localhost:8080/cwexperimental/tabledap/erdGtsppBest.json?trajectory,org,type,platform,cruise&distinct()
    //         and save it as [tomcat]/content/erddap/subset/erdGtsppBest.json
    //       * Reload ERDDAP to ensure it loads quickly.
    //    10) Test the .ncCF dataset:
    //        EDDTableFromNcFiles.testErdGtsppBest("erdGtsppBest");  //1 minute
    //    11) If copying all to coastwatch, temporarily rename dir to
    // /u00/data/points/gtsppNcCFtemp/
    //       * Copy the newly consolidated .ncCF files
    //         from laptop   /u00/data/points/gtsppNcCF/
    //         to coastwatch /u00/data/points/gtsppNcCF/
    //       * Copy from local     [tomcat]/content/erddap/subset/erdGtsppBest.json
    //              to coastwatch  [tomcat]/content/erddap/subset/erdGtsppBest.json
    //              to          [UAFtomcat]/content/erddap/subset/erdGtsppBest.json
    //    12) Update rtofs (Python/ #2=updateDatasetsXml.py),
    //        then copy datasetsFEDCW.xml to coastwatch and rename to datasets.xml
    //    13) Ping the gtspp flag url on ERDDAP (it is in "flag" bookmarks)
    //       and make sure the new data files and metadata are visible (hence, new dataset has
    // loaded).
    //       Metadata: https://coastwatch.pfeg.noaa.gov/erddap/info/erdGtsppBest/index.html

    //    String2.log(EDDTableFromSOS.generateDatasetsXml(
    //        "http://data.gcoos.org:8080/52nSOS/sos/kvp", "1.0.0", "IOOS_52N"));
    //    EDDTableFromSOS.testNosSosWTemp("");

    // Run the GenerateDatasetsXml program in interactive mode:
    //    GenerateDatasetsXml.main(null);
    /*
             //the tests below need
             //  <datasetsRegex>(etopo.*|testNccsvScalar|rGlobecBottle|erdGlobecBottle|erdBAssta5day|rMHchla8day|testGridWav|erdMWchla1day)</datasetsRegex>
             //  and cassandra running.
             EDD.testAddMvFvAttsIfNeeded();

             EDDGridAggregateExistingDimension.testGenerateDatasetsXml();  //after EDDGridFromDap
             EDDGridFromAudioFiles.testGenerateDatasetsXml();
             EDDGridFromAudioFiles.testGenerateDatasetsXml2();
             EDDGridFromDap.testGenerateDatasetsXml();  //often not accessible
             EDDGridFromDap.testGenerateDatasetsXml2();
             //EDDGridFromDap.testGenerateDatasetsXml3(); //source is gone
             EDDGridFromDap.testGenerateDatasetsXml4();
             EDDGridFromDap.testGenerateDatasetsXml5();
             EDDGridFromDap.testGenerateDatasetsXmlUInt16();  //source has error. I reported to podaac
             EDDGridFromEDDTable.testGenerateDatasetsXml();
             EDDGridFromErddap.testGenerateDatasetsXml();  //requires localhost erddap with |testGridWav|erdBAssta5day|rMHchla8day
             EDDGridFromMergeIRFiles.testGenerateDatasetsXml();
             EDDGridFromNcFiles.testGenerateDatasetsXml();
             EDDGridFromNcFiles.testGenerateDatasetsXml2();
             EDDGridFromNcFiles.testGenerateDatasetsXml3();
             EDDGridFromNcFiles.testGenerateDatasetsXml4();
             EDDGridFromNcFiles.testGenerateDatasetsXmlLong2();

             EDDGridFromNcFiles.testGenerateDatasetsXmlAwsS3();  //slow!
             EDDGridFromNcFiles.testGenerateDatasetsXmlCopy();  //requires erdMWchla1day in localhost erddap
             //EDDGridFromNcFiles.testGenerateDatasetsXmlWithRemoteThreddsFiles();  //inactive, because source is unreliable
             EDDGridFromNcFiles.testGenerateDatasetsXml5();
             EDDGridFromNcFilesUnpacked.testGenerateDatasetsXml();
             EDDGridLonPM180.testGenerateDatasetsXmlFromErddapCatalog();

             EDDTableFromAsciiFiles.testGenerateDatasetsXml();
             EDDTableFromAsciiFiles.testGenerateDatasetsXml2();
             EDDTableFromAsciiFiles.testGenerateDatasetsXmlFromBCODMO();
             EDDTableFromAsciiFiles.testGenerateDatasetsXmlFromInPort();
             EDDTableFromAsciiFiles.testGenerateDatasetsXmlFromInPort2();
             EDDTableFromAsciiFiles.testGenerateDatasetsXmlWithMV();
             EDDTableFromAudioFiles.testGenerateDatasetsXml();
             EDDTableFromAwsXmlFiles.testGenerateDatasetsXml();
             //EDDTableFromCassandra.testGenerateDatasetsXml();    //requires cassandra
             EDDTableFromColumnarAsciiFiles.testGenerateDatasetsXml();
             EDDTableFromColumnarAsciiFiles.testGenerateDatasetsXmlFromEML();
             //EDDTableFromDapSequence.testGenerateDatasetsXml();   //source is gone
             EDDTableFromDapSequence.testGenerateDatasetsXml2();
             EDDTableFromDatabase.testGenerateDatasetsXml();
             EDDTableFromErddap.testGenerateDatasetsXml();     //requires erdGlobecBottle in localhost erddap
             EDDTableFromFileNames.testGenerateDatasetsXml();
             //EDDTableFromFileNames.testGenerateDatasetsXmlAwsS3(); //slow!
             EDDTableFromHttpGet.testGenerateDatasetsXml();
             //EDDTableFromHttpGet2.testGenerateDatasetsXml();
             EDDTableFromHyraxFiles.testGenerateDatasetsXml();
             //EDDTableFromHyraxFiles.testGenerateDatasetsXml2(); //not yet working
             EDDTableFromInvalidCRAFiles.testGenerateDatasetsXml();
             EDDTableFromJsonlCSVFiles.testGenerateDatasetsXml();
             EDDTableFromMultidimNcFiles.testGenerateDatasetsXml();
             EDDTableFromMultidimNcFiles.testGenerateDatasetsXmlSeaDataNet();
             EDDTableFromMultidimNcFiles.testGenerateDatasetsXmlDimensions();
             EDDTableFromNcCFFiles.testGenerateDatasetsXml();
             EDDTableFromNcCFFiles.testGenerateDatasetsXml2();
             EDDTableFromNccsvFiles.testGenerateDatasetsXml();
             EDDTableFromNcFiles.testGenerateDatasetsXml();
             EDDTableFromNcFiles.testGenerateDatasetsXml2();
             EDDTableFromNcFiles.testGenerateDatasetsXmlNcdump();
             EDDTableFromNcFiles.testCopyFilesGenerateDatasetsXml();
             //EDDTableFromNWISDV.testGenerateDatasetsXml(); //inactive
             EDDTableFromOBIS.testGenerateDatasetsXml();
             EDDTableFromSOS.testGenerateDatasetsXml(true); //useCachedInfo);
             EDDTableFromSOS.testGenerateDatasetsXmlFromOneIOOS(true); //useCachedInfo);
             EDDTableFromSOS.testGenerateDatasetsXmlFromIOOS(true); //useCachedInfo);
             //EDDTableFromThreddsFiles.testGenerateDatasetsXml();  //source is unreliable
             EDDTableFromWFSFiles.testGenerateDatasetsXml(true);  //developmentMode (read from file, not source)
    /* */

    // 2018-09-13 https: works in browser by not yet in Java
    //    String2.log(EDDTableFromThreddsFiles.generateDatasetsXml(
    //        "https://tds.coaps.fsu.edu/thredds/catalog/samos/data/research/WTEP/2012/catalog.xml",
    //          "WTEP_20120215.*",
    //
    // "https://tds.coaps.fsu.edu/thredds/dodsC/samos/data/quick/WTEP/2012/WTEP_20120215v10002.nc",
    //
    // "https://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/catalog.xml",
    //          "BodegaMarineLabBuoyCombined.nc",
    //
    // "https://data.nodc.noaa.gov/thredds/dodsC/testdata/netCDFTemplateExamples/timeSeries/BodegaMarineLabBuoyCombined.nc",
    //        60,
    //        "", "", "", "", "",
    //        "time", null));

    //    FileVisitorDNLS.sync(
    //        "http://hydrology.nws.noaa.gov/aorc-historic/AORC_ABRFC_4km/",
    //        "/u00/data/points/AORC_ABRFC_4km/",
    //        ".*", false, ".*", true);  //recursive?  fullSync?
    //    FileVisitorDNLS.findFileWith("/Temp/access_logs/", ".*", //dir, fileNameRegex
    //        true, ".*",   //recursive, pathRegex
    // lines below:  //lineRegex, tallyWhich, interactiveNLines, showTopN
    //        "([0-9\\.]+) \\- .*/erddap/.*", 1, -1, 100);       //apache access_log uniqueIP
    //        ".*(/erddap/).*", 1, -1, 100);                     //apache access_log totalNRequests
    //        ".*/erddap/.*(\\.[a-zA-Z0-9]+)\\?.*", 1, -1, 100); //apache access_log file extensions
    // related to subset requests

    //    GenerateDatasetsXml.main(new String[]{"-verbose"});  //interactive
    //     DasDds.main(new String[]{"-verbose"});  //interactive

    //    test if a string matches a regex
    //        Pattern p = Pattern.compile(".*waiting=(\\d+), inotify=(\\d+), other=(\\d+).*");
    // //regex
    //        Matcher m = p.matcher("Number of threads: Tomcat-waiting=6, inotify=1, other=23");
    // //string
    //        String2.log("matches=" + m.matches());
    //
    //    NDBC MONTHLY UPDATES.   NEXT TIME: be stricter and remove 99.9 and 98.7 data values.
    //      !!!check pxoc1. make historic file if needed.
    //    NdbcMetStation.main(null);   //in gov/noaa/pfel/coastwatch/pointdata/

    //    String2.log(NcHelper.ncdump("C:/data/socat/06AQ20110715.nc", "-h"));
    //    String2.log(NcHelper.dds("c:/data/nodcTemplates/pointKachemakBay.nc"));
    //    test validity of a file:
    //        NetcdfDataset ncd = NetcdfDatasets.openDataset( //file or DAP baseUrl    //2021: 's'
    // is new API
    //
    // "http://oos.soest.hawaii.edu/thredds/dodsC/hioos/roms_forec/hiog/ROMS_Oahu_Regional_Ocean_Model_best.ncd");
    //        System.out.println("netcdfDataset=" + ncd.toString());
    //        System.out.println("featureType=" +
    // FeatureDatasetFactoryManager.findFeatureType(ncd).toString());
    //        ncd.close();
    //    NetCheck.verbose = true;
    //    NetCheck nc = new NetCheck("c:/content/bat/NetCheck.xml", true); //testmode
    //    Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    //        Matcher matcher = pattern.matcher("test 2009-01-02 12:13:14abcaaaaab");
    //        if (matcher.find(1)) String2.log("matched at start=" + matcher.start());
    //        else String2.log("didn't match");
    //
    //    DasDds.main(new String[]{"erdMWpp3day", "-verbose"});

    //    SimpleXMLReader.testValidity(
    //        "/programs/_tomcat/content/erddap/datasetsFEDCW.xml", "erddapDatasets");

    //    SSR.downloadFile(
    //        "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0",
    //        "/downloads/testUnidata.xml", true); //tryToUseCompression
    //    String2.log(File2.directReadFromUtf8File("/downloads/testUnidata.xml"));

    //    Projects.makeNetcheckErddapTests( //results are on clipboard
    //        "https://coastwatch.pfeg.noaa.gov/erddap/");
    //        "https://upwell.pfeg.noaa.gov/erddap/");
    //        "http://75.101.155.155/erddap/");

    // !!! This runs much faster if Windows Explorer windows are all closed!
    //    Projects.splitOBIS("S:\\occurrence.csv.gz", "S:\\obisOccurrence\\"); //next time: try
    // using .zip directly   //~3 hours

    //    Test.displayInBrowser("file://" + tName);
    //    SSR.downloadFile("",
    //            String fullFileName, boolean tryToUseCompression);  //throws Exception
    //    String2.log(SSR.getURLResponseStringUnchanged(
    //
    // "https://coastwatch.pfeg.noaa.gov/erddap/tabledap/pmelTao.csv?&time>=2008-11-13T00:00:00Z"));
    //        "https://oceandata.sci.gsfc.nasa.gov/VIIRS/Mapped/Daily/4km/CHL_chlor_a/2016/"));
    //        "http://localhost/cwexperimental/tabledap/rPmelTao.csv?&time>=2008-11-13T00:00:00Z"));
    //
    // String2.log(SSR.getURLResponseStringUnchanged("https://coastwatch.pfeg.noaa.gov:8443/erddap2/griddap/etopo180.htmlTable?altitude[(-90.0):1000:(90.0)][(-180.0):1000:(180.0)]"));
    //
    // String2.log(SSR.minimalPercentEncode("sst[(1870-01-01):1:(2011-07-01T00:00:00Z)][(29.5):1:(29.5)][(-179.5):1:(179.5)]"));
    //
    // SSR.testForBrokenLinks("http://localhost:8080/cwexperimental/images/erddapTalk/erdData.html");
    //    SSR.testForBrokenLinks("g:/NOAA-Navy-SanctSound_CI01_01_BB_1h.xml");
    //
    //    StringArray.repeatedDiff("c:/downloads/f1.txt", "c:/downloads/f2.txt", File2.UTF_8);
    //    XML.prettyXml("c:/programs/mapserver/WVBoreholeResponse.xml",
    //                  "c:/programs/mapserver/WVBoreholeResponsePretty.xml");

    //    Table.testReadNcSequence();

    /*
          // Supported Mime Types: https://cloud.google.com/translate/docs/supported-formats
          TranslateTextRequest request =
              TranslateTextRequest.newBuilder()
                  .setParent(parent.toString())
                  .setMimeType("text/plain")
                  .setTargetLanguageCode(targetLanguage)
                  .addContents(text)
                  .build();

          TranslateTextResponse response = client.translateText(request);

          // Display the translation for each input text provided
          for (Translation translation : response.getTranslationsList()) {
            System.out.printf("Translated text: %s\n", translation.getTranslatedText());
          }
        }
    */

    // Force compilation of all the classes that need to be deployed.
    // Almost all of these are compiled automatically if you recompile everything,
    // but it is useful to have them here: During development, if you change a
    // lower level class that isn't listed in TestAll and tell the compiler to
    // recompile TestAll, the compiler may not notice the changes to the lower
    // level class and so won't recompile it.  Mentioning the class here solves
    // the problem.
    Attributes att;
    AttributedString2 as2;
    Boundaries boun;
    ByteArray ba;
    Calendar2 calendar2;
    CharArray chara;
    CompoundColorMap ccm;
    CompoundColorMapLayerChild ccmlc;
    dods.dap.DConnect dConnect;
    dods.dap.DFloat64 dFloat64;
    dods.dap.DInt16 dInt16;
    dods.dap.DString dString;
    dods.dap.parser.DASParser dasParser;
    dods.dap.parser.DDSParser ddsParser;
    DataHelper dh;
    DigirHelper dh2;
    dods.dap.DSequence dseq;
    DoubleArray doublea;
    EDDTableFromAllDatasets etfad;
    File2 f2;
    FileNameUtility fnu;
    FileVisitorDNLS fvdnls;
    FileVisitorSubdir fvsd;
    FilledMarkerRenderer fmr;
    FloatArray floata;
    GenerateThreddsXml gtx;
    GraphDataLayer gdl;
    Grid grid;
    GSHHS gshhs;
    HashDigest hd;
    Image2 i2;
    IntArray inta;
    JSONObject jo;
    org.json.JSONTokener jt;
    LongArray la;
    Math2 m2;
    Matlab matlab;
    MustBe mb;
    NcHelper ncHelper;
    NetCheck netCheck;
    OpendapHelper opendapHelper;
    PAOne paOne;
    PauseTest pt;
    PlainAxis2 sgtpa2;
    PrimitiveArray primitiveArray;
    RegexFilenameFilter rff;
    ResourceBundle2 rb2;
    RowComparator rc;
    RowComparatorIgnoreCase rcic;
    SaveOpendap so;
    SdsReader sr;
    SgtGraph sgtGraph;
    SgtMap sgtMap;
    SgtUtil sgtUtil;
    gov.noaa.pfel.coastwatch.sgt.PathCartesianRenderer sgtptcr;
    gov.noaa.pmel.sgt.AnnotationCartesianRenderer sgtacr;
    gov.noaa.pmel.sgt.AxisTransform sgtat;
    gov.noaa.pmel.sgt.CartesianGraph sgtcg;
    gov.noaa.pmel.sgt.CartesianRenderer sgtcr;
    gov.noaa.pmel.sgt.CenturyAxis sgtca;
    gov.noaa.pmel.sgt.contour.Contour sgtcc;
    gov.noaa.pmel.sgt.contour.ContourLine sgtccl;
    gov.noaa.pmel.sgt.DayMonthAxis sgtdma;
    gov.noaa.pmel.sgt.DecadeAxis sgtda;
    gov.noaa.pmel.sgt.dm.SGTGrid sgtsgdtg;
    gov.noaa.pmel.sgt.dm.SGTImage sgti;
    gov.noaa.pmel.sgt.dm.SGTLine sgtl;
    gov.noaa.pmel.sgt.dm.SGTPoint sgtp;
    gov.noaa.pmel.sgt.dm.SGTVector sgtsgdtv;
    gov.noaa.pmel.sgt.GridAttribute sgtga;
    gov.noaa.pmel.sgt.GridCartesianRenderer sgtgcr;
    gov.noaa.pmel.sgt.Graph sgtg;
    gov.noaa.pmel.sgt.HourDayAxis sgthda;
    gov.noaa.pmel.sgt.JPane sgtj;
    gov.noaa.pmel.sgt.LabelDrawer1 ld1;
    gov.noaa.pmel.sgt.LabelDrawer2 ld2;
    gov.noaa.pmel.sgt.Layer sgtla;
    gov.noaa.pmel.sgt.LayerChild sgtlc;
    gov.noaa.pmel.sgt.LineCartesianRenderer sgtlcr;
    gov.noaa.pmel.sgt.LogAxis sgtloga;
    gov.noaa.pmel.sgt.MilliSecondAxis sgtmsa;
    gov.noaa.pmel.sgt.MinuteHourAxis sgtmha;
    gov.noaa.pmel.sgt.MonthYearAxis sgtmya;
    gov.noaa.pmel.sgt.PaneProxy sgtpp;
    gov.noaa.pmel.sgt.PlainAxis sgtpa;
    gov.noaa.pmel.sgt.PointCartesianRenderer sgtpcr;
    gov.noaa.pmel.sgt.SecondMinuteAxis sgtsma;
    gov.noaa.pmel.sgt.TimeAxis sgtta;
    gov.noaa.pmel.sgt.VectorCartesianRenderer sgtvcr;
    gov.noaa.pmel.sgt.YearDecadeAxis sgtyda;
    gov.noaa.pmel.util.GeoDate geodate;
    Script2 script2;
    ScriptCalendar2 sc2;
    ScriptMath sm;
    ScriptMath2 sm2;
    ScriptRow srow;
    ScriptString2 ss2;
    ShortArray sha;
    SimpleXMLReader sxr;
    SSR ssr;
    String2 s2;
    String2LogFactory s2lf;
    StringArray sa;
    StringComparatorIgnoreCase scic;
    StringHolder sh;
    StringHolderComparator shc;
    StringHolderComparatorIgnoreCase shcic;
    Table myTable;
    TableXmlHandler txh;
    Tally tally;
    Test test;
    Touch touch;
    UByteArray uba;
    UIntArray uia;
    ULongArray ula;
    Units2 u2;
    UShortArray usa;
    gov.noaa.pmel.sgt.VectorCartesianRenderer vcr;
    VectorPointsRenderer vpr;
    WatchDirectory wdir;
    XML xml;

    // ERDDAP-related
    ArchiveADataset aad;
    AxisDataAccessor ada;
    DasDds dd;
    EDStatic es;
    EDD edd;
    EDDGrid eddGrid;
    EDDGridAggregateExistingDimension eddaed;
    EDDGridCopy eddgc;
    // EDDGridFromBinaryFile eddgfbf;  //not active
    EDDGridFromDap eddgfd;
    EDDGridFromEDDTable eddgfet;
    EDDGridFromErddap eddgfed;
    EDDGridFromEtopo eddgfe;
    EDDGridFromFiles eddgff;
    EDDGridFromNcFilesUnpacked eddgfncu;
    EDDGridFromNcFiles eddgfncf;
    EDDGridFromNcLow eddgfncl;
    EDDGridSideBySide eddgsbs;
    EDDTable eddTable;
    EDDTableCopy eddtc;
    // EDDTableCopyPost eddtcp;  //inactive
    EDDTableFromAsciiService eddtfas;
    EDDTableFromAsciiServiceNOS eddtfasn;
    // EDDTableFromBMDE eddtfb; //inactive
    EDDTableFromCassandra eddtfc;
    EDDTableFromDapSequence eddtfds;
    EDDTableFromDatabase eddtfdb;
    EDDTableFromEDDGrid eddtfeg;
    EDDTableFromErddap eddtfed;
    EDDTableFromFileNames eddtffn;
    EDDTableFromFiles eddtff;
    EDDTableFromFilesCallable eddtffc;
    EDDTableFromAsciiFiles eddtfaf;
    EDDTableFromColumnarAsciiFiles eddtffaf;
    EDDTableFromHttpGet eddtfhg;
    EDDTableFromHyraxFiles eddtfhf;
    // EDDTableFromMWFS eddtfm;  //INACTIVE
    EDDTableFromMultidimNcFiles eddtfmdnf;
    EDDTableFromNcFiles eddtfnf;
    EDDTableFromNccsvFiles eddtfnccsvf;
    // EDDTableFromNWISDV eddtfnwisdv; //INACTIVE
    EDDTableFromOBIS eddtfo;
    // EDDTableFromPostDatabase eddtfpdb;  //INACTIVE
    // EDDTableFromPostNcFiles eddtfpnf;  //INACTIVE
    EDDTableFromSOS eddtfs;
    EDDTableFromThreddsFiles eddtftf;
    // EDStatic above
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
    EmailThread et;
    Erddap erddap;
    ErddapRedirect erddapRedirect;
    FindDuplicateTime findDuplicateTime;
    GenerateDatasetsXml gdx;
    GridDataAccessor gda;
    GridDataAllAccessor gdaacc;
    GridDataRandomAccessor gdracc;
    GridDataRandomAccessorInMemory gdraccim;
    HtmlWidgets hw;
    LoadDatasets ld;
    NoMoreDataPleaseException nmdpe;
    OpendapHelper oh;
    OutputStreamSource oss;
    OutputStreamFromHttpResponse osfhr;
    OutputStreamFromHttpResponseViaAwsS3 osfhrvas;
    OutputStreamViaAwsS3 osvas;
    PersistentTable pert;
    RunLoadDatasets rld;

    Subscriptions sub;
    TableWriter tw;
    TableWriterAll twa;
    TableWriterAllReduceDnlsTable twardt;
    TableWriterAllReduceDnlsTableNLevels twardtnl;
    TableWriterAllWithMetadata twawm;
    TableWriterDataTable twdt;
    TableWriterDistinct twdis;
    TableWriterDods twd;
    TableWriterDodsAscii twda;
    TableWriterEsriCsv twec;
    TableWriterGeoJson twgj;
    TableWriterHtmlTable twht;
    TableWriterJson twj;
    TableWriterJsonl twjl;
    TableWriterNccsv twn;
    TableWriterOrderBy twob;
    TableWriterOrderByClosest twobc;
    TableWriterOrderByCount twobcount;
    TableWriterOrderByDescending twod;
    TableWriterOrderByLimit twobl;
    TableWriterOrderByMax twobm;
    TableWriterOrderByMean twobmean;
    TableWriterOrderBySum twobsum;
    TableWriterOrderByMin twobmin;
    TableWriterOrderByMinMax twobmm;
    TableWriterSeparatedValue twsv;
    TableWriterUnits twu;
    TaskThread tt;
    TouchThread tt2;
    TranslateMessages translateMessages;
    WaitThenTryAgainException wttae;

    StringBuilder errorSB = new StringBuilder();
    boolean interactive = false;
    boolean doSlowTestsToo = false;

    /* for releases, this line should have open/close comment */
    // and all tests should be "0, -1"

    // *** All of the unit tests for CWBrowsers and ERDDAP.

    // data

    // need tests of data.Grid2DDataSet classes
    // hdf.SdsWriter.main(null); //needs work
    // GridDataSetCWOpendap.test(     errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE. The
    // test files are no longer available since we are moving to thredds

    // other
    // Browser.test(                    errorSB, interactive, doSlowTestsToo, 0, -1); //INACTIVE.
    // The cwbrowsers are no longer supported.

    // ERDDAP

    // EDDGrid
    // EDDGridFromBinaryFile.test(    errorSB, interactive, doSlowTestsToo, 0, -1);  class not
    // finished / not in use

    // EDDTable
    // EDDTableFromWFSFiles.test(     errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE
    // 2021-06-25 because test server is gone
    // EDDTableFromMWFS.test(         errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE as
    // of 2009-01-14
    // EDDTableFromNOS.test(          errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE as
    // of 2010-09-08
    // EDDTableFromNWISDV.test(       errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE as
    // of 2011-12-16
    // EDDTableFromBMDE.test(         errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE
    // EDDTableFromPostDatabase.test( errorSB, interactive, doSlowTestsToo, 0, -1);  //INACTIVE.
    // very slow?
    // EDDTableCopyPost.test(-1, false);                                             //INACTIVE
    // which, reallyVerbose?

    // NetCheckTests
    // NetCheck.unitTest(); which does 3 tests:
    // HttpTest.unitTest(); 2016-02-23 needs work
    // OpendapTest.unitTest();
    // SftpTest.unitTest(); //orpheus Shell authentication started failing ~2010-06

    // a test of oceanwatch THREDDS   (should run great)
    // try {
    //     int nTimes = 0; //0 to disable, 5 for a full test
    //     for (int i = 0; i < nTimes; i++) {
    //         long time9 = System.currentTimeMillis();
    //         Opendap.doOceanWatchSpeedTests(false, false); //dotTest, asciiTest
    //         time9 = System.currentTimeMillis() - time9;
    //         if (i > 0 && time9 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from
    // outside
    //             String2.pressEnterToContinue("OceanWatch Thredds too slow: " + time9);
    //     }
    //     //don't run often
    //     //Opendap.threddsTunnelTest(10,  //200 for a good test
    //     //    "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/CM/usfc/hday",
    //     //    "CMusfc");
    // } catch (Exception e) {
    //     String2.pressEnterToContinue(MustBe.throwableToString(e) +
    //         "\nUnexpected oceanwatch error.");
    // }

    // INACTIVE: a test of thredds1 THREDDS 8081
    // try {
    //    int nTimes = 0; //0 to disable, 5 for a full test
    //    for (int i = 0; i < nTimes; i++) {
    //        long time9 = System.currentTimeMillis();
    //        Opendap.doThredds1_8081SpeedTests(false, true); //dotTest, asciiTest
    //        time9 = System.currentTimeMillis() - time9;
    //        if (i > 0 && time9 > 10000) //2014-08 was 1000 in ERD building. Now 10000 from outside
    //            String2.pressEnterToContinue("Thredds1 8081 Thredds too slow: " + time9);
    //    }
    //    //don't run often
    //    Opendap.threddsTunnelTest(10,  //200 for a good test
    //        "https://thredds1.pfeg.noaa.gov:8081/thredds/dodsC/satellite/CM/usfc/hday",
    //        "CMusfc");
    // } catch (Exception e) {
    //    String2.pressEnterToContinue(MustBe.throwableToString(e) +
    //        "\nUnexpected THREDD1 8081 ERROR.");
    // }

    // a test of erddap
    // try {
    //     int nTimes = 0; //0 to disable, 5 for a full test
    //     for (int i = 0; i < nTimes; i++) {
    //         long time9 = System.currentTimeMillis();
    //         Opendap.doErddapSpeedTests(false, false); //dotTest, asciiTest
    //         time9 = System.currentTimeMillis() - time9;
    //         if (i > 0 && time9 > 3000) //2014-08 was 1000 in ERD building. Now 3000 from outside
    //             String2.pressEnterToContinue("Erddap too slow: " + time9);
    //     }
    //     //don't run often
    //     Opendap.threddsTunnelTest(10,  //200 for a good test
    //         "https://coastwatch.pfeg.noaa.gov/erddap/griddap/erdCMsfc",
    //         "eastCurrent");
    // } catch (Exception e) {
    //     String2.pressEnterToContinue(MustBe.throwableToString(e) +
    //         "\nUnexpected erddap error.");
    // }

    // Touch.thredds();  //run as needed to tell thredds to cache PISCO datasets?   or usually runs
    // really fast?

    // INACTIVE make ErdJava.zip
    // for distribution of GridSaveAs, NetCheck, ConvertTable, and GenerateThreddsXml
    // see /classes/gov/noaa/pfel/coastwatch/pointData/MakeErdJavaZip
    // MakeErdJavaZip.main(null); //see C:\programs\_tomcat\webapps\cwexperimental\ErdJava.zip
    // and the smaller version
    // MakeErdJavaZip.makeConvertTableJar("C:/pmelsvn/WebContent/WEB-INF/lib/"); //only do when
    // working on LAS stuff

    // INACTIVE because not needed, but works: make EMA.war
    // MakeEmaWar.main(null);

    String2.log(
        "\n"
            + "*** Before a release, spell check (copy to EditPlus, then spellcheck)\n"
            + "and validate HTML the main web pages!\n");

    // */
    // AFTER deploying browsers: test the experimental browser
    // TestBrowsers.testAll();

    // TestBrowsers.doGraphicalGetTests(TestBrowsers.experimentalBaseUrl + "CWBrowser.jsp"); //part
    // of testAll

    if (errorSB != null && errorSB.length() > 0) {
      String fileName = EDStatic.fullLogsDirectory + "/TestAllErrorSB.txt";
      String2.log(
          File2.writeToFileUtf8(
              fileName,
              "errorSB from TestAll which finished at "
                  + Calendar2.getCurrentISODateTimeStringLocalTZ()
                  + "\n"
                  + errorSB.toString()));
      Test.displayInBrowser("file://" + fileName); // .txt
    }

    String2.returnLoggingToSystemOut();
    String2.log("*** Press ^C to exit.");
  }
}
