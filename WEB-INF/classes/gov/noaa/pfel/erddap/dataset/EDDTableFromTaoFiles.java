/* 
 * EDDTableFromTaoFiles Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;

import gov.noaa.pfel.coastwatch.griddata.NcHelper;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.coastwatch.sgt.SgtUtil;
import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

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
 * This class represents a table of data from a collection of TAO .cdf data files.
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2011-07-21
 */
public class EDDTableFromTaoFiles extends EDDTableFromNcFiles { 


    /** 
     * The constructor just calls the super constructor. 
     *
     * <p>The sortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tFgdcFile This should be the fullname of a file with the FGDC
     *    that should be used for this dataset, or "" (to cause ERDDAP not
     *    to try to generate FGDC metadata for this dataset), or null (to allow
     *    ERDDAP to try to generate FGDC metadata for this dataset).
     * @param tIso19115 This is like tFgdcFile, but for the ISO 19119-2/19139 metadata.
     */
    public EDDTableFromTaoFiles(String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, String tFgdcFile, String tIso19115File, 
        String tDefaultDataQuery, String tDefaultGraphQuery, 
        Attributes tAddGlobalAttributes,
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        String tCharset, int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ, boolean tFileTableInMemory) 
        throws Throwable {

        super("EDDTableFromTaoFiles", true, tDatasetID, tAccessibleTo, 
            tOnChange, tFgdcFile, tIso19115File, 
            tDefaultDataQuery, tDefaultGraphQuery,
            tAddGlobalAttributes, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tCharset, tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            tSortedColumnSourceName, tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ, tFileTableInMemory);

    }

    //no "met": met is a composite of several other taoTypes, with different depth vars for each!
    public static final String taoTypes[] = {
        "adcp", "airt", "bf", "bp", "cur",   //0..
        "d", "dyn", "emp", "evap", "heat",   //5..
        "iso", "lw", "lwnet", "pos", "qlat", //10..
        "qnet", "qsen", "rad", "rain", "rf", //15..   daily uses "rain"; other use "r"
        "rh", "s", "ssd", "sss", "sst",      //20..
        "swnet", "t", "tau", "w"};           //25..
    public static final String taoTimePeriods[]        = new String[]{"daily", "5day", "monthly", "quarterly"};
    public static final String taoFileTimePeriods[]    = new String[]{"dy",    "5day", "mon",     "qrt"};  //file name
    public static final String taoRequestTimePeriods[] = new String[]{"dy",    "hf",   "lf",      "qt"};  //html request
    public static final int    taoRequestBackDays[]    = new int[]   {30,      60,     360,       360};


    /**
     * This overwrites the default implementation of getFileNames, which
     * gets file names from a local directory.
     * This is called in the middle of the constructor.
     * Some subclasses override this (e.g., EDDTableFromTaoFiles updates the data files).
     *
     * @param recursive true if the file search should also search subdirectories
     * @returns an array with a list of full file names 
     * @throws Throwable if trouble
     */
    public String[] getFileNames(String fileDir, String fileNameRegex,
        boolean recursive) throws Throwable {

        //unusual, super-simple approach to quickRestart:
        //If quickRestart, just don't even check if it is time (after 9am) to get new files.
        //It will be checked next time dataset is updated.
        //(There was already a quickRestart-like file: updated.txt.)
        //No need to set creationTimeMillis specially.
        if (EDStatic.quickRestart && 
            EDStatic.initialLoadDatasets()) {
            if (verbose) 
                String2.log("  quickRestart datasetID=" + datasetID);

        } else {
            try {
                //just update the datafiles once per day, 
                //  ASAP after 09am local (usually updated 7:30-8:30am)
                //  (sometimes files are dated e.g., 7:37am but actually appear about 8:30am)
                //So find last 9:00 local that has occurred (today or yesterday)
                GregorianCalendar last9L = Calendar2.newGCalendarLocal();
                int hr = last9L.get(Calendar2.HOUR_OF_DAY);
                //String2.log("current hr=" + hr);
                if (hr < 9)  //0..23
                    last9L.add(Calendar2.DATE, -1); 
                Calendar2.clearSmallerFields(last9L, Calendar2.DATE);
                last9L.set(Calendar2.HOUR_OF_DAY, 9);           
                hr = last9L.get(Calendar2.HOUR_OF_DAY); //encourage 'set' to take effect
                long last9LMillis = last9L.getTimeInMillis(); //not Zulu

                //when was dataset last updated with data requested from PMEL TAO website?
                long lastUpdatedMillis = 
                    File2.getLastModified(fileDir + "updated.txt"); //returns 0 if fileNotFound

                //is lastUpdated < last9L   (i.e., is it out-of-date?)
                if (verbose) String2.log(
                    "lastUpdated=" + Calendar2.formatAsISODateTimeT(
                                     Calendar2.newGCalendarLocal(lastUpdatedMillis)) + " local" +
                    "  last9L="    + Calendar2.formatAsISODateTimeT(last9L) + " local" +
                    "  needsUpdate=" + (lastUpdatedMillis < last9LMillis));
                if (lastUpdatedMillis < last9LMillis) {
                    
                    //get taoType from <preExtractRegex>adcp
                    String taoType = preExtractRegex;
                    if (taoType.equals("r")) 
                        taoType = "rain";
                    Test.ensureTrue(String2.indexOf(taoTypes, taoType) >= 0, 
                        "Unsupported taoType=" + taoType + " (from preExtractRegex).");

                    //get fileTimePeriod from <postExtractRegex>_dy\.cdf
                    String fileTimePeriod = postExtractRegex.substring(1);
                    int slashPo = fileTimePeriod.indexOf('\\');
                    if (slashPo <= 0)
                        throw new RuntimeException("Missing \\ in postExtractRegex=" + postExtractRegex +
                            " needed to find fileTimePeriod, e.g., dy.");
                    fileTimePeriod = fileTimePeriod.substring(0, slashPo);
                    Test.ensureTrue(String2.indexOf(taoFileTimePeriods, fileTimePeriod) >= 0, 
                        "Unsupported fileTimePeriod=" + fileTimePeriod + " (from postExtractRegex).");

                    //update the datafiles for this subset of TAO
                    updateOneTaoDataset(fileDir, fileTimePeriod, taoType);
                }
            } catch (Throwable t) {
                String2.log("ERROR in getFileNames for datasetID=" + datasetID + "\n" +
                    MustBe.throwableToString(t)); 
            }
        }


        //get the fileNames         
        return super.getFileNames(fileDir, fileNameRegex, recursive);
    }

    /**
     * This updates all TAO datasets: it gets recent data from the TAO website
     * and uses it to update existing TAO files.
     *
     * @param fileBaseDirectory (e.g., f:/data/tao/tao/) to which is added (e.g., daily/adcp/)
     */
    public static void updateAllTaoDatasets(String baseDir) {

        //for each taoTimePeriod, e.g., daily, 5day, ... 
        for (int tPeriod = 0; tPeriod < taoTimePeriods.length; tPeriod++) { 

            //for each taoType, e.g., adcp, airt, ...
            for (int tType = 0; tType < taoTypes.length; tType++) {
                String taoDir = taoTypes[tType];
                if (tPeriod > 0 && taoDir.equals("rain"))
                    taoDir = "r";
                updateOneTaoDataset(
                    baseDir + taoTimePeriods[tPeriod] + "/" + taoDir + "/", 
                    taoFileTimePeriods[tPeriod], taoTypes[tType]);
            }
        }
    }

    /**
     * This updates one TAO dataset: it gets recent data from the TAO website
     * and uses it to update existing TAO files.
     * This mimics form at http://www.pmel.noaa.gov/tao/data_deliv/deliv-nojava.html.
     *
     * @param fileDir with existing .cdf files
     * @param fileTimePeriod one of the taoFileTimePeriods (above), e.g., dy
     * @param taoType  one of the taoTypes (above), e.g., adcp
     * @param fileDir e.g.  f:/data/tao/tao/daily/adcp/
     */
    public static void updateOneTaoDataset(String fileDir, 
        String fileTimePeriod, String taoType) {

        if (verbose) String2.log("updateOneTaoDataset(" + fileDir + ", " + 
            fileTimePeriod + ", " + taoType + ")");
        long time = System.currentTimeMillis();
        byte[] buffer = new byte[1024];
        String tempDir = EDStatic.fullTestCacheDirectory;

        //timePeriod variants
        int tPeriod = String2.indexOf(taoFileTimePeriods, fileTimePeriod);
        if (tPeriod < 0)
            throw new RuntimeException("Invalid fileTimePeriod=" + fileTimePeriod);
        String timePeriod = taoTimePeriods[tPeriod];
        String requestTimePeriod = taoRequestTimePeriods[tPeriod];

        //endDate is today  (last available data should be for yesterday)
        String endDate = Calendar2.getCurrentISODateTimeStringZulu();
        String beginDate;
        try {
            beginDate = Calendar2.epochSecondsToIsoStringT(
                Calendar2.backNDays(taoRequestBackDays[tPeriod], Double.NaN));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        //some not supported by the form but may be supported by the service. 
        //Form doesn't list:
        //  "bf", "emp", "evap", "heat", "lwnet", "qlat", "qnet", 
        //  "rf", "ssd", "sss", "swnet", "swn", "tau"
        //By trying, only adcp fails.
        if (taoType.equals("adcp")) {
            if (verbose) String2.log("taoType=" + taoType + " can't be updated.");
            return;
        }
        if (taoType.equals("pos") && tPeriod > 0) {
            if (verbose) String2.log("For taoType=" + taoType + ", only daily data can be updated.");
            return;
        }
        String fileTaoType = taoType;
        if (tPeriod > 0 && fileTaoType.equals("rain")) //"rain" for daily
            fileTaoType = "r";                         //"r" for others
        String requestTaoType = taoType.equals("w")? "wind" : taoType;

        //things to keep track of
        int nTotalUpdated = 0;
        int nTotalNoExistingFile = 0;
        StringArray noExistingFile = new StringArray();
        String existingFiles[] = RegexFilenameFilter.list(fileDir, ".*\\.cdf");
        int nFiles = existingFiles.length;
        HashSet existingFilesNotUpdated = new HashSet(Math2.roundToInt(1.4 * nFiles));
        for (int f = 0; f < nFiles; f++)
            existingFilesNotUpdated.add(existingFiles[f]);
        existingFiles = null; //gc it

        //for each region
        for (int region = 0; region < 3; region++) {

            int nInRegionUpdated = 0;
            int nRegionNoExistingFile = 0;
            TarArchiveInputStream tais = null;
            try {

                ///Compare printouts of the web pages for the different regions
                //http://www.pmel.noaa.gov/tao/data_deliv/deliv-nojava.html
                //with the current web pages to see if there are new stations
                String url = "http://www.pmel.noaa.gov/cgi-tao/cover.cgi?";
                if (region == 0) 
                    url +=                            
                    "p30=8n137e&p31=8n156e&p32=8n165e&p33=8n180w&p34=8n170w" +
                    "&p35=8n155w&p36=9n140w&p37=8n125w&p38=8n110w&p39=8n95w" +
                    "&p42=5n137e&p43=5n147e&p44=5n156e&p45=5n165e&p46=5n180w" +
                    "&p47=5n170w&p48=5n155w&p49=5n140w&p50=5n125w&p51=5n110w" +
                    "&p52=5n95w&p55=2n137e&p56=2n147e&p57=2n156e&p58=2n165e" +
                    "&p59=2n180w&p60=2n170w&p61=2n155w&p62=2n140w&p63=2n125w" +
                    "&p64=2n110w&p65=2n95w&p69=0n147e&p70=0n156e&p71=0n165e" +
                    "&p72=0n180w&p73=0n170w&p74=0n155w&p75=0n140w&p76=0n125w" +
                    "&p77=0n110w&p78=0n95w&p81=2s156e&p82=2s165e&p83=2s180w" +
                    "&p84=2s170w&p85=2s155w&p86=2s140w&p87=2s125w&p88=2s110w" +
                    "&p89=2s95w&p92=5s156e&p93=5s165e&p94=5s180w&p95=5s170w" +
                    "&p96=5s155w&p97=5s140w&p98=5s125w&p99=5s110w&p100=5s95w" +
                    "&p102=8s165e&p103=8s180w&p104=8s170w&p105=8s155w&p106=8s125w" +
                    "&p107=8s110w&p108=8s95w" +
                    "&P18=137&P19=265&P20=-8&P21=9";
                else if (region == 1) 
                    url +=
                    "p23=21n23w&p24=20n38w&p25=15n38w&p26=12n38w&p27=12n23w" +
                    "&p28=8n38w&p29=4n38w&p30=4n23w&p31=2n10w&p32=0n35w&p33=0n23w" +
                    "&p34=0n10w&p35=0n0e&p36=2s10w&p37=6s10w&p38=6s8e&p39=8s30w" +
                    "&p40=10s10w&p41=14s32w&p42=19s34w" +
                    "&P18=322&P19=368&P20=-19&P21=21";
                else if (region == 2) 
                    url +=
                    "p30=15n90e&p31=12n90e&p32=8n90e&p33=4n90e&p34=1.5n80.5e" +
                    "&p35=1.5n90e&p36=0n80.5e&p37=0n90e&p38=1.5s80.5e&p39=1.5s90e" +
                    "&p40=4s67e&p41=4s80.5e&p42=5s95e&p43=8s55e&p44=8s67e&p45=8s80.5e" +
                    "&p46=8s95e&p47=8s100e&p48=12s55e&p49=12s67e&p50=12s80.5e" +
                    "&p51=12s93e&p53=16s55e&p54=16s80.5e" +
                    "&p18=40&p19=120&p20=-30&p21=30";

                url +=
                    "&P1=deliv" +
                    "&P2=" + requestTaoType + "&P3=" + requestTimePeriod +
                    "&P4=" + beginDate.substring(0, 4) + 
                    "&P5=" + String2.parseInt(beginDate.substring(5, 7)) +  //e.g., 1
                    "&P6=" + beginDate.substring(8, 10) +   //01
                    "&P7=" + endDate.substring(0, 4) + 
                    "&P8=" + String2.parseInt(endDate.substring(5, 7)) +  //e.g., 1
                    "&P9=" + endDate.substring(8, 10) +   //01
                    "&P10=buoytar&P11=cf4" +  //tar with a file for each site
                    "&P12=zip&P13=tt&P14=Bob+Simons&P16=bob.simons%40noaa.gov" +  //zip=gzip(!)
                    "&P15=NOAA+SWFSC+ERD&NOTHING=&P17=ERDDAP+update&p22=html&script=disdel%2Fnojava.csh";

                //request that data.tar.gz file be made
                if (reallyVerbose) String2.log("region=" + region + " url=" + url);
                String response = SSR.getUncompressedUrlResponseString(url);
                //look for <A href=" /cache-tao/sf1/deliv/data22153/data.tar.gz "> data.tar.gz </A><br>
                int dataPo = response.indexOf("data.tar.gz");
                if (dataPo < 0)
                    throw new RuntimeException("data.tar.gz not found.  End of response=\n" +
                        response.substring(Math.max(0, response.length() - 2000)));
                int qPo = response.lastIndexOf('"', dataPo);

                url = "http://www.pmel.noaa.gov" +
                    response.substring(qPo + 1, dataPo).trim() + "data.tar.gz";
                //request the data.tar.gz file
                String2.log("url=" + url);
                InputStream tfis;
                if (false) {
                    //FOR DEBUGGING ONLY
                    String tempFile = "c:/data/tao/data.tar.gz";
                    //SSR.downloadFile(url, tempFile, false); //one time
                    tfis = new FileInputStream(tempFile);
                } else {
                    //don't try to use compression, because the response is already compressed
                    tfis = SSR.getUncompressedUrlInputStream(url); 
                }
                tfis = new GZIPInputStream(tfis);
                tais = new TarArchiveInputStream(tfis);

                //for each entry in tar
                while (true) {
                    TarArchiveEntry entry = tais.getNextTarEntry();
                    if (entry == null)        break;
                    if (entry.isDirectory())  continue;
                
                    //create .cdf.gz file
                    String gzName = File2.getNameAndExtension(entry.getName());
                    if (debugMode) String2.log("  gzName=\"" + gzName + "\"");
                    if (!gzName.endsWith(".cdf.gz")) continue;
                    String cdfName = gzName.substring(0, gzName.length() - 3);
                    if (debugMode) String2.log("  cdfName=\"" + cdfName + "\"");
                    if (!File2.isFile(fileDir + cdfName)) {
                        nRegionNoExistingFile++;
                        nTotalNoExistingFile++;
                        noExistingFile.add(cdfName);
                        String2.log("WARNING: file doesn't exist: " + fileDir + cdfName +
                            "\n  Why is there no existing file for the available new data?");
                        continue;
                    }
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(tempDir + gzName);
                        long fileSize = entry.getSize();
                        while (true) {
                            int bytesRead = tais.read(buffer);
                            if (bytesRead == -1) break;
                            fos.write(buffer, 0, bytesRead);
                        }
                        fos.close(); fos = null;

                        //create .cdf file
                        InputStream fis = null;
                        try {
                            fis = new FileInputStream(tempDir + gzName);
                            fis = new GZIPInputStream(fis);
                            fos = new FileOutputStream(tempDir + cdfName);
                            while (true) {
                                int bytesRead = fis.read(buffer);
                                if (bytesRead == -1) break;
                                fos.write(buffer, 0, bytesRead);
                            }
                            fis.close();  fis = null;
                            fos.close();  fos = null;

                            //read .cdf file into table
                            Table newTable = ingestOneTaoFile(tempDir + cdfName);
                            //String2.log(newTable.toCSSVString(10));

                            //update
                            Table oldTable = new Table();
                            oldTable.readNDNc(fileDir + cdfName, null,
                                null, 0, 0, true);
                            int rowCol = oldTable.findColumnNumber(ROW_NAME);
                            if (rowCol >= 0) 
                                oldTable.removeColumn(rowCol);
                            String keyColumns[] = new String[]{"time", "depth"};
                            oldTable.update(keyColumns, newTable);
                            oldTable.ascendingSort(keyColumns);
                            oldTable.saveAsFlatNc(fileDir + cdfName, ROW_NAME, false);  //it uses randomInt
                            nTotalUpdated++;
                            nInRegionUpdated++;
                            existingFilesNotUpdated.remove(cdfName);

                        } catch (Throwable t) {
                            String2.log(MustBe.throwableToString(t));
                            if (fis != null) { fis.close(); fis = null; }
                            if (fos != null) { fos.close(); fos = null; }
                        }  
                        File2.delete(tempDir + cdfName);

                    } catch (Throwable t) {
                        String2.log(MustBe.throwableToString(t));
                        if (fos != null) { fos.close(); fos = null; }
                    }  
                    File2.delete(tempDir + gzName);
                }
                tais.close();                   

            } catch (Throwable t) {
                String2.log("ERROR in updateOneTaoDataset: " + MustBe.throwableToString(t));
                if (tais != null) {
                    try {tais.close();} catch (Throwable t2) {}
                    tais = null;
                }
            }
            if (verbose) String2.log("\nregion=" + region + " nUpdated=" + nInRegionUpdated +
                " nNoExistingFile=" + nRegionNoExistingFile);
        }

        //write update.txt file 
        String2.writeToFile(fileDir + "updated.txt", endDate);

        if (verbose) {
            Object oar[] = existingFilesNotUpdated.toArray();
            String2.log("updateOneTaoDataset(" + fileDir + ", " + 
                fileTimePeriod + ", " + taoType + ")" +
                "\nfinished.  nUpdated=" + nTotalUpdated + " of " + nFiles +  
                  ", time=" + (System.currentTimeMillis() - time) +
                "\n" + noExistingFile.size() + " noExistingFiles=" + noExistingFile.toString() +
                "\n" + oar.length + " existingFilesNotUpdated=\n  " + 
                    String2.noLongLines(String2.toCSSVString(oar), 80, "  "));
        }
    }

    /** 
     * Used by ingestAllTaoFiles to read a source TAO file, convert time to epochSeconds,
     * and convert quality vars to byteArrays. 
     *
     * @return table   may be null if trouble
     */
    public static Table ingestOneTaoFile(String fullName) throws Exception {
        //read a 4D .nc file 
        Table table = new Table();
        table.readNDNc(fullName, null, "", 0, 0, true);
        int nRows = table.nRows();
        int nCols = table.nColumns();

        //convert time to epochSeconds (since different source files have diff 'since' times)
        int timeCol = table.findColumnNumber("time");
        PrimitiveArray timePa = table.getColumn(timeCol);
        String units = table.columnAttributes(timeCol).getString("units");
        if (units == null || units.indexOf(" since ") < 0) {
            String2.log("ERROR: no 'since' in time's units=" + units+ " in " + fullName);
            return null;
        } else {
            double bf[] = Calendar2.getTimeBaseAndFactor(units);
            DoubleArray esTimePa = new DoubleArray(nRows, false);
            for (int row = 0; row < nRows; row++)
                esTimePa.add(Calendar2.unitsSinceToEpochSeconds(
                    bf[0], bf[1], timePa.getDouble(row)));
            table.setColumn(timeCol, esTimePa);
            table.columnAttributes(timeCol).set("units", Calendar2.SECONDS_SINCE_1970);
        }
        
        //convert QUALITY, SOURCE, INSTRUMENT CODE vars to bytes
        for (int c = 0; c < nCols; c++) {
            String ln = table.columnAttributes(c).getString("long_name");
            if (ln != null && 
                (ln.indexOf("QUALITY") >= 0 || 
                 ln.indexOf("SOURCE") >= 0 || 
                 ln.indexOf("INSTRUMENT CODE") >= 0)) {
                //verify just integers -9 (mv?) - 99
                PrimitiveArray qpa = table.getColumn(c);
                boolean just999 = true;
                for (int row = 0; row < nRows; row++) {
                    float val = qpa.getFloat(row);
                    if (val == Math2.roundToInt(val) &&
                        (val >= -9 && val <= 99)) {
                    } else if (val == 1e35f) {
                        qpa.setInt(row, -9);
                    } else {
                        String2.log(val + " in " + fullName);
                        just999 = false;
                        break;
                    }
                }
                if (just999) {
                    table.setColumn(c, new ByteArray(qpa));
                    table.columnAttributes(c).set("missing_value", (byte)-9);
                } else {
                    table.columnAttributes(c).set("missing_value", -9f);
                }
            } else if (table.getColumn(c) instanceof FloatArray) {
                table.columnAttributes(c).set("missing_value", 1e35f);
            }
        }

        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);
        table.globalAttributes().add("history",
            today + " Most recent downloading and reformatting of all cdf/sites/... files from " +
            "PMEL TAO's FTP site by bob.simons at noaa.gov.\n" +
            "Since then, recent data has been updated every day.");

        return table;
    }


    /** 
     * This ingests all source TAO datafiles (all TaoTypes, all timePeriods): 
     * read 4D .nc file, convert time to epochSeconds, convert quality vars to bytes,
     * insert lat,lon POS variables, save as 1D .nc file in c:/data/tao/flat/daily|....
     * I download the source data every month from their ftp site: 
     *   from ftp://taopmelftp@ftp.pmel.noaa.gov  cdf/sites/daily|5day|monthly|quarterly 
     *   to f:/data/tao/sites/daily|...
     * Resulting files suitable for new pmelTaoXxx datasets.
     *
     * @param inBaseDir e.g., f:/data/tao/sites/   
     * @param outBaseDir e.g., c:/u00/data/points/tao/   
     *     If !testMode, all existing dirs and files will be deleted.
     *
     */
    public static void ingestAllTaoFiles(boolean testMode, String inBaseDir,
        String outBaseDir) throws Throwable {

        Table.verbose = false;
        Table.reallyVerbose = false;

        for (int tPeriod = 0; tPeriod < (testMode? 1 : taoTimePeriods.length); tPeriod++) { 

            String timePeriod = taoTimePeriods[tPeriod];
            String fileTimePeriod = taoFileTimePeriods[tPeriod];

            for (int tType = 0; tType < (testMode? 1 : taoTypes.length); tType++) {  

                try {
                    String taoType = taoTypes[tType];
                    if (taoType.equals("rain") && tPeriod > 0) 
                        taoType = "r";

                    //get list of files for one taoType e.g., adcp
                    String inDir  = inBaseDir + timePeriod + "/";
                    String outDir = outBaseDir + timePeriod + "/" + taoType + "/";
                    File2.makeDirectory(outDir);
                    File2.deleteAllFiles(outDir);
                    String[] names = RegexFilenameFilter.list(inDir, 
                        taoType + "[0-9].*_" + fileTimePeriod + "\\.cdf");        
                    String2.log("ingestAllTaoFiles timePeriod=" + timePeriod + 
                        " taoType=" + taoType + " nFiles=" + names.length);
                    for (int name = 0; name < names.length; name++) {

                        //read a 4D .nc file 
                        Table table = ingestOneTaoFile(inDir + names[name]);
                        if (table == null)
                            continue;
                        int nRows = table.nRows();

                        //ensure all columns have some data
                        int nCols = table.nColumns();
                        for (int c = 0; c < nCols; c++) {
                            double stats[] = table.getColumn(c).calculateStats();
                            if (stats[PrimitiveArray.STATS_N] == 0) {
                                String2.log("No " + table.getColumnName(c) + 
                                    " values in " + names[name]);
                            }
                        }
                        table.ascendingSort(new String[]{"time", "depth"});
                        
                        //save as 1D .nc file in f:/data/tao/flat/daily|...
                        table.saveAsFlatNc(outDir + names[name], ROW_NAME, false);  
                    }                       
                } catch (Throwable t) {
                    String2.log("ERROR: " + MustBe.throwableToString(t));
                }
            }
        }
    }

    /** 
     * Experiments to learn about the TAO data files 2011-07-13. 
     * I downloaded the data from their ftp site: 
     *   from ftp://taopmelftp@ftp.pmel.noaa.gov  cdf/sites/daily|5day|monthly|quarterly 
     *   to f:/data/tao/sites/daily|...
     */
    public static void testTao() throws Throwable {
        Table.verbose = false;
        Table.reallyVerbose = false;
        for (int i = 0; i < taoTypes.length; i++) {
            String dir = "c:/data/tao/sites/daily/";

            //generate datasets.xml
            if (false) {
                String2.log(EDDTableFromNcFiles.generateDatasetsXml(
                    dir, taoTypes[i] + "[0-9].*_dy\\.cdf", 
                    dir + taoTypes[i] + "0n140w_dy.cdf", "", 360, 
                    taoTypes[i], "_dy\\.cdf", ".*",
                    "station", "time",
                    "station, time", 
                    "", "", "", "", new Attributes()));
                Table table = new Table();
                table.readNDNc(dir + taoTypes[i] + "0n140w_dy.cdf",
                   new String[]{"depth"}, "", 0, 0, false);
                String2.log("depth:\n" + table.dataToCSVString(3));
            }
            

            //Do all taoTypes[i] files use the same depth values?
            //  No!  Some do. Some don't.  
            //  So don't try to merge taoTypes that have the simplistically have 
            //  the same depth values, because vars do for a given taoType.
            if (false) {
                String2.log("\nTesting for depth consistency in " + taoTypes[i]);
                String[] names = RegexFilenameFilter.list(dir, taoTypes[i] + "[0-9].*_dy\\.cdf");        
                String standardDepths = null;
                boolean consistent = true;
                for (int f = 0; f < names.length; f++) {
                    Table table = new Table();
                    table.readNDNc(dir + names[f],
                       new String[]{"depth"}, "", 0, 0, false);
                    String newDepths = table.dataToCSVString(10);
                    if (standardDepths == null) 
                        standardDepths = newDepths;            
                    if (!newDepths.equals(standardDepths)) {
                        consistent = false;
                        String2.log("  standardDepths=" + standardDepths +
                            "\n  newDepths#" + f + "=" + newDepths);
                        break;
                    }
                }
                if (consistent)
                    String2.log("  All consistent!");
            }

            //Do all QUALITY variables have just integers 0 - 5, -9?  YES!
            if (false) {
                String2.log("\nTesting QUALITY variable values in " + taoTypes[i]);
                String[] names = RegexFilenameFilter.list(dir, taoTypes[i] + "[0-9].*_dy\\.cdf");        
                boolean consistent = true;
                for (int f = 0; f < names.length; f++) {
                    Table table = new Table();
                    table.readNDNc(dir + names[f], null, "", 0, 0, false);

                    for (int v = 0; v < table.nColumns(); v++) {
                        String longName = table.columnAttributes(v).getString("long_name");
                        if (longName != null && longName.indexOf("QUALITY") >= 0) {
                            PrimitiveArray pa = table.getColumn(v);
                            int nRows = pa.size();
                            for (int row = 0; row < nRows; row++) {
                                float val = pa.getFloat(row);
                                if (val == Math2.roundToInt(val) &&
                                    ((val >=0 && val <= 5) || val == -9)) {
                                } else {
                                    String2.log(val + " in " + dir + names[f]);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (consistent)
                    String2.log("  All QUALITY values are integers 0 - 5, -9!");
            }
           
            //Do all SOURCE variables have just integers 0 - 7?  
            if (false) {
                String2.log("\nTesting SOURCE variable values in " + taoTypes[i]);
                String[] names = RegexFilenameFilter.list(dir, taoTypes[i] + "[0-9].*_dy\\.cdf");        
                boolean consistent = true;
                for (int f = 0; f < names.length; f++) {
                    Table table = new Table();
                    table.readNDNc(dir + names[f], null, "", 0, 0, false);

                    for (int v = 0; v < table.nColumns(); v++) {
                        String longName = table.columnAttributes(v).getString("long_name");
                        if (longName != null && longName.indexOf("QUALITY") >= 0) {
                            PrimitiveArray pa = table.getColumn(v);
                            int nRows = pa.size();
                            for (int row = 0; row < nRows; row++) {
                                float val = pa.getFloat(row);
                                if (val == Math2.roundToInt(val) &&
                                    ((val >=0 && val <= 7))) {
                                } else {
                                    String2.log(val + " in " + dir + names[f]);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (consistent)
                    String2.log("  All SOURCE values are integers 0 - 7!");
            }

            //Do all INSTRUMENT CODE variables have just integers 0 - 7?  
            if (true) {
                String2.log("\nTesting INSTRUMENT CODE variable values in " + taoTypes[i]);
                String[] names = RegexFilenameFilter.list(dir, taoTypes[i] + "[0-9].*_dy\\.cdf");        
                boolean consistent = true;
                for (int f = 0; f < names.length; f++) {
                    Table table = new Table();
                    table.readNDNc(dir + names[f], null, "", 0, 0, false);

                    for (int v = 0; v < table.nColumns(); v++) {
                        String longName = table.columnAttributes(v).getString("long_name");
                        if (longName != null && longName.indexOf("INSTRUMENT CODE") >= 0) {
                            PrimitiveArray pa = table.getColumn(v);
                            int nRows = pa.size();
                            for (int row = 0; row < nRows; row++) {
                                float val = pa.getFloat(row);
                                if (val == Math2.roundToInt(val) &&
                                    ((val >=0 && val <= 99))) {
                                } else {
                                    String2.log(val + " in " + dir + names[f]);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (consistent)
                    String2.log("  All INSTRUMENT CODE values are integers 0 - 99!");
            }
        }
    }


    /**
     * Test pmelTaoAirT against web site.
     */
    public static void testAirt() throws Throwable {

        String2.log("\n*** EDDTableFromTaoFiles.testAirt");
        EDDTable tedd = (EDDTable)oneFromDatasetXml("pmelTaoDyAirt"); //should work
        String tName, error, results, tResults, expected;
        int po;
        String today = Calendar2.getCurrentISODateTimeStringZulu().substring(0, 14); //14 is enough to check hour. Hard to check min:sec.

        //*** .das
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "testAirt", ".das"); 
        results = new String((new ByteArray(
            EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected =   //2013-01-04 several changes related to new array and wmo_platform_code
"Attributes {\n" +
" s {\n" +
"  array {\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Array\";\n" +
"  }\n" +
"  station {\n" +
"    String cf_role \"timeseries_id\";\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"Station\";\n" +
"  }\n" +
"  wmo_platform_code {\n" +
"    Int32 actual_range 0, 56055;\n" +
"    String ioos_category \"Identifier\";\n" +
"    String long_name \"WMO Platform Code\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0, 350.0;\n" +
"    String axis \"X\";\n" +
"    Int32 epic_code 502;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String standard_name \"longitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -25.0, 21.0;\n" +  //before 2012-09-06 was -19, before 2012-03-20 was 38
"    String axis \"Y\";\n" +
"    Int32 epic_code 500;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String standard_name \"latitude\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +            
"    Float64 actual_range 2.476656e+8, 1.3732848e+9;\n" + //2nd number changes daily   
"    String axis \"T\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Centered Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  depth {\n" +
"    String _CoordinateAxisType \"Height\";\n" +
"    String _CoordinateZisPositive \"down\";\n" +
"    Float32 actual_range -8.0, -3.0;\n" +
"    String axis \"Z\";\n" +
"    Int32 epic_code 3;\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Depth\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String positive \"down\";\n" +
"    String standard_name \"depth\";\n" +
"    String type \"EVEN\";\n" +
"    String units \"m\";\n" +
"  }\n" +
"  AT_21 {\n" +
"    Float32 actual_range 17.05, 34.14;\n" + //before 2012-09-06 was 0.03, before 2012-03-20 was 2.59, 41.84;\n" +
"    Float64 colorBarMaximum 40.0;\n" +
"    Float64 colorBarMinimum -10.0;\n" +
"    Int32 epic_code 21;\n" +
"    String generic_name \"atemp\";\n" +
"    String ioos_category \"Temperature\";\n" +
"    String long_name \"Air Temperature\";\n" +
"    Float32 missing_value 1.0E35;\n" +
"    String name \"AT\";\n" +
"    String standard_name \"air_temperature\";\n" +
"    String units \"degree_C\";\n" +
"  }\n" +
"  QAT_5021 {\n" +
"    Byte actual_range 0, 5;\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 6.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String description \"Quality: 0=missing data, 1=highest, 2=standard, 3=lower, 4=questionable, 5=bad, -9=contact contact Dai.C.McClurg@noaa.gov.  To get probably valid data only, request QAT_5021>=1 and QAT_5021<=3.\";\n" +
"    Int32 epic_code 5021;\n" +
"    String generic_name \"qat\";\n" +
"    String ioos_category \"Quality\";\n" +
"    String long_name \"Air Temperature Quality\";\n" +
"    Byte missing_value -9;\n" +
"    String name \"QAT\";\n" +
"  }\n" +
"  SAT_6021 {\n" +
"    Byte actual_range 0, 6;\n" +
"    String colorBarContinuous \"false\";\n" +
"    Float64 colorBarMaximum 8.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String description \"Source Codes:\n" +
"0 = No Sensor, No Data\n" +
"1 = Real Time (Telemetered Mode)\n" +
"2 = Derived from Real Time\n" +
"3 = Temporally Interpolated from Real Time\n" +
"4 = Source Code Inactive at Present\n" +
"5 = Recovered from Instrument RAM (Delayed Mode)\n" +
"6 = Derived from RAM\n" +
"7 = Temporally Interpolated from RAM\";\n" +
"    Int32 epic_code 6021;\n" +
"    String generic_name \"sat\";\n" +
"    String ioos_category \"Other\";\n" +
"    String long_name \"Air Temperature Source\";\n" +
"    Byte missing_value -9;\n" +
"    String name \"SAT\";\n" +
"  }\n" +
" }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"TimeSeries\";\n" +
"    String cdm_timeseries_variables \"array, station, wmo_platform_code, longitude, latitude\";\n" +
"    String Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    String CREATION_DATE \"07:06  6-JUN-2013\";\n" +  //changes monthly
"    String creator_email \"Dai.C.McClurg@noaa.gov\";\n" +
"    String creator_name \"Dai. C. McClurg\";\n" +
"    String creator_url \"http://www.pmel.noaa.gov/tao/proj_over/proj_over.html\";\n" +
"    String Data_info \"Contact Paul Freitag: 206-526-6727\";\n" +
"    String Data_Source \"TAO Project Office/NOAA/PMEL\";\n" +
"    Float64 Easternmost_Easting 350.0;\n" +
"    String featureType \"TimeSeries\";\n" +
"    String File_info \"Contact: Dai.C.McClurg@noaa.gov\";\n" +
"    Float64 geospatial_lat_max 21.0;\n" +
"    Float64 geospatial_lat_min -25.0;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 350.0;\n" +
"    Float64 geospatial_lon_min 0.0;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +  
"    Float64 geospatial_vertical_max -3.0;\n" +
"    Float64 geospatial_vertical_min -8.0;\n" +
"    String geospatial_vertical_positive \"down\";\n" +
"    String geospatial_vertical_units \"m\";\n" + //date on line below changes monthly
"    String history \"2013-06-06 Most recent downloading and reformatting of all cdf/sites/... files from PMEL TAO's FTP site by bob.simons at noaa.gov.\n" +
"Since then, recent data has been updated every day.\n" +
today;
        tResults = results.substring(0, Math.min(results.length(), expected.length()));
        Test.ensureEqual(tResults, expected, "\nresults=\n" + results);

//+ " (local files)\n" +
//today + " http://127.0.0.1:8080/cwexperimental/
expected = 
"tabledap/pmelTaoDyAirt.das\";\n" +
"    String infoUrl \"http://www.pmel.noaa.gov/tao/proj_over/proj_over.html\";\n" +
"    String institution \"NOAA PMEL, TAO/TRITON, RAMA, PIRATA\";\n" +
"    String keywords \"Atmosphere > Atmospheric Temperature > Air Temperature,\n" +
"Atmosphere > Atmospheric Temperature > Surface Air Temperature,\n" +
"air, air_temperature, atmosphere, atmospheric, buoys, centered, daily, depth, identifier, noaa, pirata, pmel, quality, rama, source, station, surface, tao, temperature, time, triton\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    String Metadata_Conventions \"COARDS, CF-1.6, Unidata Dataset Discovery v1.0\";\n" +
"    Float64 Northernmost_Northing 21.0;\n" + 
"    String project \"TAO/TRITON, RAMA, PIRATA\";\n" +
"    String Request_for_acknowledgement \"If you use these data in publications or presentations, please acknowledge the TAO Project Office of NOAA/PMEL. Also, we would appreciate receiving a preprint and/or reprint of publications utilizing the data for inclusion in our bibliography. Relevant publications should be sent to: TAO Project Office, NOAA/Pacific Marine Environmental Laboratory, 7600 Sand Point Way NE, Seattle, WA 98115\";\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -25.0;\n" +
"    String standard_name_vocabulary \"CF-12\";\n" +
"    String subsetVariables \"array, station, wmo_platform_code, longitude, latitude\";\n" +
"    String summary \"This dataset has daily Air Temperature data from the\n" +
"TAO/TRITON (Pacific Ocean, http://www.pmel.noaa.gov/tao/),\n" +
"RAMA (Indian Ocean, http://www.pmel.noaa.gov/tao/rama/), and\n" +
"PIRATA (Atlantic Ocean, http://www.pmel.noaa.gov/pirata/)\n" +
"arrays of moored buoys which transmit oceanographic and meteorological data to shore in real-time via the Argos satellite system.  These buoys are major components of the CLIVAR climate analysis project and the GOOS, GCOS, and GEOSS observing systems.  Daily averages are computed starting at 00:00Z and are assigned an observation 'time' of 12:00Z.  For more information, see\n" +
"http://www.pmel.noaa.gov/tao/proj_over/proj_over.html .\";\n" +
"    String time_coverage_end \"2013-07-08T12:00:00Z\";\n" +  //changes daily
"    String time_coverage_start \"1977-11-06T12:00:00Z\";\n" + //before 2012-03-20 was 1980-03-07T12:00:00
"    String title \"TAO/TRITON, RAMA, and PIRATA Buoys, Daily, Air Temperature\";\n" +
"    Float64 Westernmost_Easting 0.0;\n" +
"  }\n" +
"}\n";
        int tpo = results.indexOf(expected.substring(0, 17));
        if (tpo < 0) 
            String2.log("results=\n" + results);
        Test.ensureEqual(
            results.substring(tpo, Math.min(results.length(), tpo + expected.length())),
            expected, "results=\n" + results);
        

        //*** .dds
        tName = tedd.makeNewFileForDapQuery(null, null, "", EDStatic.fullTestCacheDirectory, 
            "testAirt", ".dds"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        expected = 
"Dataset {\n" +
"  Sequence {\n" +
"    String array;\n" +
"    String station;\n" +
"    Int32 wmo_platform_code;\n" +
"    Float32 longitude;\n" +
"    Float32 latitude;\n" +
"    Float64 time;\n" +
"    Float32 depth;\n" +
"    Float32 AT_21;\n" +
"    Byte QAT_5021;\n" +
"    Byte SAT_6021;\n" +
"  } s;\n" +
"} s;\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


        //Are there new stations??? 
        //  Check C:/u00/data/points/tao/daily/airt
        //  Currently 139 files: 138 .cdf and 1 update.txt.
        //  When new stations appear, need to modify 
        //    EDDTableFromTao.updateOneTaoDataset and the stations specified by the URLS.
        //  (It is unfortunate that this is hard-coded.)
        tName = tedd.makeNewFileForDapQuery(null, null, "station&distinct()", 
            EDStatic.fullTestCacheDirectory, "testAirtStations", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        //good test of salinity values: I hand checked this from 1989427.nc in gtspp_at199505.zip
        expected = 
/* changes noticed 2012-03-20   new stations and stations which disappeared:
Dai McClurg says the new stations are old and currently inactive but valid.
and the removed stations were temporary and aren't part of their public distribution.
So the changes seem good.
new 0.7n110w
new 0.7s110w
new 0n108w
new 0n110.5w
new 0n152w
new 0n155e
new 0n85w
new 1n153w
new 1s153w
new 6n150w
new 7n150w
new 8n150w
lost 15n115e
lost 18n116e
lost 38n146.5e

before 2012-03-20 was ...
"station\n" +  
"\n" +
"0n0e\n" +
"0n10w\n" +
"0n110w\n" +
"0n125w\n" +
"0n137e\n" +
"0n140w\n" +
"0n143e\n" +
"0n147e\n" +
"0n154e\n" +
"0n155w\n" +
"0n156e\n" +
"0n158e\n" +
"0n161e\n" +
"0n165e\n" +
"0n170e\n" +
"0n170w\n" +
"0n176w\n" +
"0n180w\n" +
"0n23w\n" +
"0n35w\n" +
"0n80.5e\n" +
"0n90e\n" +
"0n95w\n" +
"1.5n80.5e\n" +
"1.5n90e\n" +
"1.5s80.5e\n" +
"1.5s90e\n" +
"10n95w\n" +
"10s10w\n" +
"12n23w\n" +
"12n38w\n" +
"12n90e\n" +
"12n95w\n" +
"12s55e\n" +
"12s67e\n" +
"12s80.5e\n" +
"12s93e\n" +
"13n114e\n" +
"14s32w\n" +
"15n115e\n" +
"15n38w\n" +
"15n90e\n" +
"16s55e\n" +
"16s80.5e\n" +
"18n116e\n" +
"19s34w\n" +
"1n155e\n" +
"1s167e\n" +
"20n117e\n" +
"20n38w\n" +
"21n23w\n" +
"2n10w\n" +
"2n110w\n" +
"2n125w\n" +
"2n137e\n" +
"2n140w\n" +
"2n147e\n" +
"2n155w\n" +
"2n156e\n" +
"2n157w\n" +
"2n165e\n" +
"2n170w\n" +
"2n180w\n" +
"2n95w\n" +
"2s10w\n" +
"2s110w\n" +
"2s125w\n" +
"2s140w\n" +
"2s155w\n" +
"2s156e\n" +
"2s165e\n" +
"2s170w\n" +
"2s180w\n" +
"2s95w\n" +
"3.5n95w\n" +
"38n146.5e\n" +
"4n23w\n" +
"4n38w\n" +
"4n90e\n" +
"4n95w\n" +
"4s67e\n" +
"4s80.5e\n" +
"5n110w\n" +
"5n125w\n" +
"5n137e\n" +
"5n140w\n" +
"5n147e\n" +
"5n155w\n" +
"5n156e\n" +
"5n165e\n" +
"5n170w\n" +
"5n180w\n" +
"5n95w\n" +
"5s10w\n" +
"5s110w\n" +
"5s125w\n" +
"5s140w\n" +
"5s155w\n" +
"5s156e\n" +
"5s165e\n" +
"5s170w\n" +
"5s180w\n" +
"5s95e\n" +
"5s95w\n" +
"6s10w\n" +
"6s8e\n" +
"7n132w\n" +
"7n137e\n" +
"7n140w\n" +
"7n147w\n" +
"8n110w\n" +
"8n125w\n" +
"8n130e\n" +
"8n137e\n" +
"8n155w\n" +
"8n156e\n" +
"8n165e\n" +
"8n167e\n" +
"8n168e\n" +
"8n170w\n" +
"8n180w\n" +
"8n38w\n" +
"8n90e\n" +
"8n95w\n" +
"8s100e\n" +
"8s110w\n" +
"8s125w\n" +
"8s155w\n" +
"8s165e\n" +
"8s170w\n" +
"8s180w\n" +
"8s30w\n" +
"8s55e\n" +
"8s67e\n" +
"8s80.5e\n" +
"8s95e\n" +
"8s95w\n" +
"9n140w\n"; */
"station\n" +
"\n" +
"0.7n110w\n" +
"0.7s110w\n" +
"0n0e\n" +
"0n108w\n" +
"0n10w\n" +
"0n110.5w\n" +
"0n110w\n" +
"0n125w\n" +
"0n137e\n" +
"0n140w\n" +
"0n143e\n" +
"0n147e\n" +
"0n152w\n" +
"0n154e\n" +
"0n155e\n" +
"0n155w\n" +
"0n156e\n" +
"0n158e\n" +
"0n161e\n" +
"0n165e\n" +
"0n170e\n" +
"0n170w\n" +
"0n176w\n" +
"0n180w\n" +
"0n23w\n" +
"0n35w\n" +
"0n80.5e\n" +
"0n85w\n" +
"0n90e\n" +
"0n95w\n" +
"1.5n80.5e\n" +
"1.5n90e\n" +
"1.5s80.5e\n" +
"1.5s90e\n" +
"10n95w\n" +
"10s10w\n" +
"12n23w\n" +
"12n38w\n" +
"12n90e\n" +
"12n95w\n" +
"12s55e\n" +
"12s67e\n" +
"12s80.5e\n" +
"12s93e\n" +
"13n114e\n" +
"14s32w\n" +
"15n38w\n" +
"15n90e\n" +
"16s55e\n" +
"16s80.5e\n" +
"19s34w\n" +
"1n153w\n" +
"1n155e\n" +
"1s153w\n" +
"1s167e\n" +
"20n117e\n" +
"20n38w\n" +
"21n23w\n" +
"25s100e\n" + //added 2012-09-06
"2n10w\n" +
"2n110w\n" +
"2n125w\n" +
"2n137e\n" +
"2n140w\n" +
"2n147e\n" +
"2n155w\n" +
"2n156e\n" +
"2n157w\n" +
"2n165e\n" +
"2n170w\n" +
"2n180w\n" +
"2n95w\n" +
"2s10w\n" +
"2s110w\n" +
"2s125w\n" +
"2s140w\n" +
"2s155w\n" +
"2s156e\n" +
"2s165e\n" +
"2s170w\n" +
"2s180w\n" +
"2s95w\n" +
"3.5n95w\n" +
"4n23w\n" +
"4n38w\n" +
"4n90e\n" +
"4n95w\n" +
"4s67e\n" +
"4s80.5e\n" +
"5n110w\n" +
"5n125w\n" +
"5n137e\n" +
"5n140w\n" +
"5n147e\n" +
"5n155w\n" +
"5n156e\n" +
"5n165e\n" +
"5n170w\n" +
"5n180w\n" +
"5n95w\n" +
"5s10w\n" +
"5s110w\n" +
"5s125w\n" +
"5s140w\n" +
"5s155w\n" +
"5s156e\n" +
"5s165e\n" +
"5s170w\n" +
"5s180w\n" +
"5s95e\n" +
"5s95w\n" +
"6n150w\n" +
"6s10w\n" +
"6s8e\n" +
"7n132w\n" +
"7n137e\n" +
"7n140w\n" +
"7n147w\n" +
"7n150w\n" +
"8n110w\n" +
"8n125w\n" +
"8n130e\n" +
"8n137e\n" +
"8n150w\n" +
"8n155w\n" +
"8n156e\n" +
"8n165e\n" +
"8n167e\n" +
"8n168e\n" +
"8n170w\n" +
"8n180w\n" +
"8n38w\n" +
"8n90e\n" +
"8n95w\n" +
"8s100e\n" +
"8s110w\n" +
"8s125w\n" +
"8s155w\n" +
"8s165e\n" +
"8s170w\n" +
"8s180w\n" +
"8s30w\n" +
"8s55e\n" +
"8s67e\n" +
"8s80.5e\n" +
"8s95e\n" +
"8s95w\n" +
"9n140w\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);



        //data for station=2s180w
        //the initial run of this test tests whole file download 2011-07-19 updated on 2011-07-25.  
        //Does ERDDAP update match results from web site on 2011-07-25?
        //using http://www.pmel.noaa.gov/tao/data_deliv/deliv-nojava.html
        //to generate
        //http://www.pmel.noaa.gov/cgi-tao/cover.cgi?p83=2s180w&P18=137&P19=265&P20=-8&P21=9&P1=deliv&P2=airt&P3=dy&P4=2011&P5=7&P6=10&P7=2011&P8=7&P9=25&P10=buoy&P11=ascii&P12=None&P13=tt&P14=anonymous&P16=anonymous&P15=&NOTHING=&P17=&p22=html&script=disdel%2Fnojava.csh
        //yields
//Location:  2S 180W  10 Jul 2011 to 24 Jul 2011 (   15 times,  1 blocks) Gen. Date Jul 25 2011
// Units: Air Temperature (C), -9.99 = missing
// Time: 1200 10 Jul 2011 to 1200 24 Jul 2011 (index     1 to    15,   15 times)
// Depth (M):       -3 QUALITY SOURCE
// YYYYMMDD HHMM  AIRT Q S
//...
// 20110714 1200 28.34 2 1
// 20110715 1200 28.53 2 1
// 20110716 1200 27.84 2 1
// 20110717 1200 28.30 2 1
// 20110718 1200 28.42 2 1
// 20110719 1200 28.21 2 1
// 20110720 1200 28.41 2 1
// 20110721 1200 28.38 2 1
// 20110722 1200 28.06 2 1
// 20110723 1200 28.02 2 1
// 20110724 1200 28.31 2 1
        tName = tedd.makeNewFileForDapQuery(null, null, 
            "&station=\"2s180w\"&time>2011-07-14&time<2011-07-25", 
            EDStatic.fullTestCacheDirectory, "testAirtData", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"array,station,wmo_platform_code,longitude,latitude,time,depth,AT_21,QAT_5021,SAT_6021\n" +
",,,degrees_east,degrees_north,UTC,m,degree_C,,\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-14T12:00:00Z,-3.0,28.34,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-15T12:00:00Z,-3.0,28.53,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-16T12:00:00Z,-3.0,27.84,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-17T12:00:00Z,-3.0,28.3,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-18T12:00:00Z,-3.0,28.43,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-19T12:00:00Z,-3.0,28.21,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-20T12:00:00Z,-3.0,28.41,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-21T12:00:00Z,-3.0,28.38,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-22T12:00:00Z,-3.0,28.06,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-23T12:00:00Z,-3.0,28.02,1,5\n" +
"TAO/TRITON,2s180w,52312,180.0,-2.0,2011-07-24T12:00:00Z,-3.0,28.31,1,5\n";

        Test.ensureEqual(results.substring(0, expected.length()), expected, 
            "\nresults=\n" + results);
    }


    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        testAirt();
    }
}

