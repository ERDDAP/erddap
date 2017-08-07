/* 
 * EDDGridFromMergeIRFiles Copyright 2014, R.Tech.
 * See the LICENSE.txt file in this file's directory.
 */
//JL
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.Test;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.griddata.DataHelper;
import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.FileSystemException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
//import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * This class represents gridded data aggregated from a collection of NCEP/CPC
 * 4km Global (60N - 60S) IR Dataset data files.
 * Project info and file structure: http://www.cpc.ncep.noaa.gov/products/global_precip/html/README
 * Source data files: ftp://disc2.nascom.nasa.gov/data/s4pa/TRMM_ANCILLARY/MERG/
 * (which works for Bob in a browser but not yet in FileZilla)
 *
 * @author
 */
public class EDDGridFromMergeIRFiles extends EDDGridFromFiles {


    private static final int NLON = 9896;
    private static final int NLAT = 3298;
    
    private static final double SLON = 0.036378335;
    private static final double SLAT = 0.036383683;
    
    private static final double SIGMA = 5.67E-8;

    private static final short IR_MV = (short)330;
    private static final double FLUX_MV = 470.0;

    /**
     * The constructor just calls the super constructor.
     */
    public EDDGridFromMergeIRFiles(String tDatasetID, 
            String tAccessibleTo, String tGraphsAccessibleTo, boolean tAccessibleViaWMS,
            StringArray tOnChange, String tFgdcFile, String tIso19115File,
            String tDefaultDataQuery, String tDefaultGraphQuery,
            Attributes tAddGlobalAttributes,
            Object[][] tAxisVariables,
            Object[][] tDataVariables,
            int tReloadEveryNMinutes, int tUpdateEveryNMillis, String tFileDir, 
            String tFileNameRegex, boolean tRecursive, String tPathRegex, 
            String tMetadataFrom, int tMatchAxisNDigits, 
            boolean tFileTableInMemory, boolean tAccessibleViaFiles)
            throws Throwable {

        super("EDDGridFromMergeIRFiles", tDatasetID, 
                tAccessibleTo, tGraphsAccessibleTo, tAccessibleViaWMS,
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery,
                tAddGlobalAttributes,
                tAxisVariables,
                tDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis,
                tFileDir, tFileNameRegex, tRecursive, tPathRegex, 
                tMetadataFrom, tMatchAxisNDigits, 
                tFileTableInMemory, tAccessibleViaFiles);

        if (verbose) String2.log("\n*** constructing EDDGridFromMergeIRFiles(xmlReader)...");        
    }

    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the
     * specified source file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames If special axis0, this list will be the instances list[1 ... n-1].
     * @param sourceDataNames the names of the desired source data columns.
     * @param sourceDataTypes the data types of the desired source columns
     * (e.g., "String" or "float")
     * @param sourceGlobalAttributes should be an empty Attributes. It will be
     * populated by this method
     * @param sourceAxisAttributes should be an array of empty Attributes. It
     * will be populated by this method
     * @param sourceDataAttributes should be an array of empty Attributes. It
     * will be populated by this method
     * @throws Throwable if trouble (e.g., invalid file, or a sourceAxisName or
     * sourceDataName not found). If there is trouble, this doesn't call
     * addBadFile or requestReloadASAP().
     */
    public void lowGetSourceMetadata(String fileDir, String fileName,
            StringArray sourceAxisNames,
            StringArray sourceDataNames, 
            String sourceDataTypes[],
            Attributes sourceGlobalAttributes,
            Attributes sourceAxisAttributes[],
            Attributes sourceDataAttributes[]) throws Throwable {
        
        if (reallyVerbose) String2.log("getSourceMetadata " + fileDir + fileName);
        
        try {
            //globalAtts
            sourceGlobalAttributes.add("Conventions", "COARDS, CF-1.6, ACDD-1.3");
            sourceGlobalAttributes.add("creator_name", "Bob Joyce");
            sourceGlobalAttributes.add("creator_email", "robert.joyce@noaa.gov");
            sourceGlobalAttributes.add("creator_type", "person");
            sourceGlobalAttributes.add("creator_url", "http://www.cpc.ncep.noaa.gov/");
            sourceGlobalAttributes.add("infoUrl", "http://www.cpc.ncep.noaa.gov/products/global_precip/html/README");
            sourceGlobalAttributes.add("institution", "NOAA NWS NCEP CPC");
            sourceGlobalAttributes.add("keywords", "4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature");
            sourceGlobalAttributes.add("keywords_vocabulary", "GCMD Science Keywords");
            sourceGlobalAttributes.add("summary", 
"The Climate Prediction Center/NCEP/NWS is now making available\n" +
"globally-merged (60N-60S) pixel-resolution IR brightness\n" +
"temperature data (equivalent blackbody temps), merged from all\n" +
"available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n" +
"GMS).  The availability of data from METEOSAT-7, which is\n" +
"located at 57E at the present time, yields a unique opportunity\n" +
"for total global (60N-60S) coverage.");
            sourceGlobalAttributes.add("title", "NCEP/CPC 4km Global (60N - 60S) IR Dataset");

            //This is cognizant of special axis0         
            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                if (reallyVerbose) String2.log("axisAttributes for avi=" + avi + " name=" + sourceAxisNames.get(avi));
                switch(sourceAxisNames.get(avi)) {
                    case "longitude" :
                        sourceAxisAttributes[avi].add("axis", "Y"); 
                        sourceAxisAttributes[avi].add("ioos_category", "Location");
                        sourceAxisAttributes[avi].add("units", "degrees_east");
                        break;
                    case "latitude" :
                        sourceAxisAttributes[avi].add("axis", "X"); 
                        sourceAxisAttributes[avi].add("ioos_category", "Location");
                        sourceAxisAttributes[avi].add("units", "degrees_north");
                        break;
                    case "time" :
                        sourceAxisAttributes[avi].add("axis", "T"); 
                        sourceAxisAttributes[avi].add("delta_t", "0000-00-00 00:30:00");
                        sourceAxisAttributes[avi].add("long_name", "Time");                           
                        sourceAxisAttributes[avi].add("standard_name", "time");                     
                        sourceAxisAttributes[avi].add("units", "seconds since 1970-01-01T00:00:00Z");
                        break;                        
                    default : 
                        sourceAxisAttributes[avi].add("units", "count"); //"count" is udunits;  "index" isn't, but better?
                        break;
                }
            }
            for (int avi = 0; avi < sourceDataNames.size(); avi++) {
                if (reallyVerbose) String2.log("dataAttributes for avi=" + avi + " name=" + sourceDataNames.get(avi));
                switch(sourceDataNames.get(avi)) {
                    case "ir" :
                        sourceDataAttributes[avi].add("colorBarMinimum", 170);
                        sourceDataAttributes[avi].add("colorBarMaximum", 330);
                        sourceDataAttributes[avi].add("ioos_category", "Heat Flux");                        
                        sourceDataAttributes[avi].add("long_name", "IR Brightness Temperature");
                        sourceDataAttributes[avi].add("missing_value", IR_MV);                        
                        sourceDataAttributes[avi].add("standard_name", "brightness_temperature");
                        sourceDataAttributes[avi].add("units", "degreeK");
                        break;       
                    case "flux" :
                        sourceDataAttributes[avi].add("colorBarMinimum", 0.0);
                        sourceDataAttributes[avi].add("colorBarMaximum", 500.0);
                        sourceDataAttributes[avi].add("ioos_category", "Heat Flux");                        
                        sourceDataAttributes[avi].add("long_name", "Flux");
                        sourceDataAttributes[avi].add("missing_value", FLUX_MV);                        
                        sourceDataAttributes[avi].add("standard_name", "surface_upwelling_shortwave_flux");
                        sourceDataAttributes[avi].add("units", "W/m^2");
                        break;          
                    default : 
                        sourceAxisAttributes[avi].add("ioos_category", "Unknown");
                        break;
                }
            }

        } catch (Throwable t) {            
            throw new RuntimeException("Error in EDDGridFromMergeIRFiles.getSourceMetadata" +
                "\nfrom " + fileDir + fileName + 
                "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }

    /**
     * This gets source axis values from one file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames the names of the desired source axis variables.
     *   If special axis0, this will not include axis0's name.
     * @return a PrimitiveArray[] with the results (with the requested
     *   sourceDataTypes). It needn't set sourceGlobalAttributes or
     *   sourceDataAttributes (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file). If there is trouble,
     * this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceAxisValues(String fileDir, String fileName,
            StringArray sourceAxisNames) throws Throwable {

        String getWhat = "";
        
        try {
            PrimitiveArray[] avPa = new PrimitiveArray[sourceAxisNames.size()];

            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                String avName = sourceAxisNames.get(avi);
                getWhat = "axisValues for variable=" + avName;

                switch (avName) {
                    case "longitude":
                        FloatArray lonBounds = new FloatArray(NLON, true);
                        float lon = 0.0182f;
                        for (int i = 0; i < NLON; i++) {
                            lonBounds.setFloat(i, lon);
                            lon += SLON;
                        }
                        avPa[avi] = lonBounds;
                        break;
                    case "latitude":
                        FloatArray latBounds = new FloatArray(NLAT, true);
                        float lat = -59.982f;
                        for (int i = 0; i < NLAT; i++) {
                            latBounds.setFloat(i, lat);
                            lat += SLAT;
                        }
                        avPa[avi] = latBounds;
                        break;
                    case "time": {
                        if (reallyVerbose) String2.log("case time with " + fileName);
                        String sdate = File2.getNameNoExtension(fileName)
                            .replaceAll("merg_", "").replaceAll("_4km-pixel", "");
                        if (reallyVerbose) String2.log("date = " + sdate + " (" + sdate.length() + ")");

                        if (sdate.length() != 10) 
                            throw new RuntimeException("File name (" + fileName + ") must contain a date encoded as 10 characters.");

                        //format fdjksdfljk_2014010102
                        String year = sdate.substring(0, 4);
                        String month = sdate.substring(4, 6);
                        String day = sdate.substring(6, 8);
                        String hour = sdate.substring(8, 10);                        
                        
                        java.text.SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String fileTime = month + "/" + day + "/" + year + " " + hour + ":00:00";
                        Date date = sdf.parse(fileTime);
                        
                        //calculate bounds 
                        long d0 = date.getTime() / 1000;
                        long d1 = d0 + 1800;
                                               
                        //log
                        getWhat += "fileTime = " + fileTime + " --> date = \"" + date.toString() + "\" (d0=" + d0 + ", d1=" + d1 + ")";
                        if (reallyVerbose) String2.log(getWhat);

                        //set result
                        DoubleArray ret = new DoubleArray(2, true);
                        ret.set(0, d0); 
                        ret.set(1, d1);

                        avPa[avi] = ret;
                    }
                    break;
                    default:
                        throw new Exception("case : " + avName);
                }
            }
            return avPa;

        } catch (Throwable t) {
            throw new RuntimeException("Error in EDDGridFromMergeIRFiles.getSourceAxisValues"
                + "\nwhile getting " + getWhat
                + "\nfrom " + fileDir + fileName
                + "\nCause: " + MustBe.throwableToShortString(t),
                t);
        }
    }

    /**
     * This gets source data from one file.
     *
     * @param fileDir
     * @param fileName
     * @param tDataVariables the desired data variables
     * @param tConstraints where the first axis variable's constraints have been
     *   customized for this file.
     *   !!! If special axis0, then will not include constraints for axis0.
     * @return a PrimitiveArray[] with an element for each tDataVariable with
     *   the dataValues.
     *   <br>The dataValues are straight from the source, not modified.
     *   <br>The primitiveArray dataTypes are usually the sourceDataTypeClass, but
     *   can be any type. EDDGridFromFiles will convert to the
     *   sourceDataTypeClass.
     *   <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (notably, WaitThenTryAgainException). If
     *   there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] lowGetSourceDataFromFile(String fileDir, String fileName,
            EDV tDataVariables[], IntArray tConstraints) throws Throwable {
        
        if (verbose) String2.log("getSourceDataFromFile(" + fileDir + ", " + fileName + ", " + tDataVariables + ", " + tConstraints + ")");
        
         //make the selection spec  and get the axis values
         int nbAxisVariable = axisVariables.length;         
         int nbDataVariable = tDataVariables.length;
         PrimitiveArray[] paa = new PrimitiveArray[nbDataVariable];
         StringBuilder selectionSB = new StringBuilder();
         
         int minTime = 0, maxTime = 0, strideTime = 0;               
         int minLat = 0, maxLat = 0, strideLat = 0;     
         int minLon = 0, maxLon = 0, strideLon = 0;         
         
         for (int avi = 0; avi < nbAxisVariable; avi++) {
             switch(axisVariables[avi].sourceName()) {
                 case "latitude" :
                     minLat = tConstraints.get(avi*3);
                     strideLat = tConstraints.get(avi*3 + 1);
                     maxLat = tConstraints.get(avi*3 + 2);
                     break;                     
                 case "longitude" :
                     minLon = tConstraints.get(avi*3);
                     strideLon = tConstraints.get(avi*3 + 1);
                     maxLon = tConstraints.get(avi*3 + 2);
                     break;
                 case "time" :
                     minTime = tConstraints.get(avi*3);
                     strideTime = tConstraints.get(avi*3 + 1);
                     maxTime = tConstraints.get(avi*3 + 2);
                     break;
             }
            selectionSB.append((avi == 0? "" : ",") +
            axisVariables[avi].sourceName() + "{" +
            tConstraints.get(avi*3  ) + ":" + 
            tConstraints.get(avi*3+1) + ":" + 
            tConstraints.get(avi*3+2) + "}"); //start:stop:stride !
        }
        String selection = selectionSB.toString();

        int nbLat  = DataHelper.strideWillFind(maxLat  - minLat  + 1, strideLat);     
        int nbLon  = DataHelper.strideWillFind(maxLon  - minLon  + 1, strideLon);         
        int nbTime = DataHelper.strideWillFind(maxTime - minTime + 1, strideTime);                
        int total = nbLat * nbLon * nbTime;//data length
        
        if (reallyVerbose) String2.log(
            "constraints : " + selection +
            "\nnb: lat=" + nbLat + ", lon=" + nbLon + ", time=" + nbTime + ", total size=" + total);         
        int indexOut = 0;//index in data array
        
        BufferedInputStream bis = new BufferedInputStream( //because it supports "marks"
            new FileInputStream(fileDir + fileName));//may throw exception
        InputStream inStream;
        String ext = File2.getExtension(fileName);
        if (ext.equals(""))
            inStream = bis;
        else inStream = new CompressorStreamFactory().createCompressorInputStream(
            bis);  //This inputStream must support "marks".
        //was just for .gz: else inStream = new GZIPInputStream(bis);

        try {
             
            byte[] in = new byte[NLON * NLAT * 2];
            short[] out1 = new short[total];         
            double[] out2 = new double[total];
            
            //entire read of the file
            long t0 = System.currentTimeMillis();
            int indexIn = 0;
            byte[] buff = new byte[NLAT];
            int n = -2;
            while((n = inStream.read(buff, 0, buff.length)) != -1) {
                System.arraycopy(buff, 0, in, indexIn, n);
                indexIn += n;
            }
            if (indexIn != in.length) {
                throw new FileSystemException("Merge file seems to be corrupted because size is : " + indexIn + " and should be " + in.length);
            }
            if (verbose) String2.log("read file in " + (System.currentTimeMillis() - t0) + "ms");
            if (reallyVerbose) String2.logNoNewline("Closing file...");
            inStream.close();//I care about this exception                        
            inStream = null; //indicate it closed successfully
            if (reallyVerbose) String2.log("Done");
            
            if (reallyVerbose) String2.logNoNewline("Copy filtered data...");
            t0 = System.currentTimeMillis();
            for (int t = minTime; t <= maxTime; t += strideTime) {//[0 - 1]
                int offsetTime = NLON * NLAT * t;
                for (int la = minLat; la <= maxLat; la += strideLat) {
                    int offsetLat = NLON * la;
                    for (int lo = minLon; lo <= maxLon; lo += strideLon) {      
                        short value = (short)(in[offsetTime + offsetLat + lo] & 0xff);
                        value += 75;
                        out1[indexOut] = value;
                        out2[indexOut] = ((double)Math.round(T2F(value) * 10.)) / 10.;
                        indexOut++;
                    }                       
                }                                       
            }
            if (reallyVerbose) {
                String2.log("Done in " + (System.currentTimeMillis() - t0) + "ms");
                if (total >= 10) {
                    String2.log("Log the 10 last values :");
                    int i = 0;
                    while(i != 10) {
                        int j = total - 1 - i;
                        String2.log("\tT[" + j + "] = " + out1[j] + "F[" + j + "] = " + out2[j]);
                        i++;
                    }
                }
            }

            in = null;
            if (nbDataVariable == 2) {
                paa[0] = PrimitiveArray.factory(out1);            
                paa[1] = PrimitiveArray.factory(out2);
            } else if (nbDataVariable == 1) {
                if (tDataVariables[0].sourceName().equalsIgnoreCase("ir")) {
                    paa[0] = PrimitiveArray.factory(out1);
                } else {
                    paa[0] = PrimitiveArray.factory(out2);
                }
            }//else 0
            
        } catch (Throwable t) {
            //make sure it is explicitly closed
            if (inStream != null) {
                try {   
                    inStream.close();    
                } catch (Throwable t2) {
                    if (verbose) String2.log("2nd attempt to close also failed:\n" +
                        MustBe.throwableToShortString(t2));
                }
            }
            if (verbose) String2.log("Error while reading " + fileDir + fileName);
            throw t;
        }
        return paa;
    }
    
    private static double T2F(double pT) { 
        // relation to go from brightness temperature to flux
        //return (T-162.)*(300./(330.-162.));
        //return T;
        //return SIGMA*T*T*T*T;
        //return (T-120)*(325.0/(330-75.0));
        //return (T-140)*(400.0/(330-75.0));
        boolean model2007=true;
        if (model2007) {
            double t2 =pT-40.;
            return SIGMA*t2*t2*t2*t2 + 69. ;
        }
        return (pT-160)*(500.0/(330-75.0));
    }

    /**
     * This makes a sibling dataset, based on the new sourceUrl.
     *
     * @throws Throwable always (since this class doesn't support sibling())
     */
    public EDDGrid sibling(String tLocalSourceUrl, int firstAxisToMatch,
        int matchAxisNDigits, boolean shareInfo) throws Throwable {
        throw new SimpleException(
            "Error: EDDGridFromMergeIRFiles doesn't support method=\"sibling\".");

    }

    /**
     * This does its best to generate a clean, ready-to-use datasets.xml entry
     * for an EDDGridFromMergeIRFiles dataset. The XML can then be edited by hand 
     * and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to look at (possibly) private files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex the regex that each filename (no directory info)
     *   must match (e.g., ".*\\.gz") (usually only 1 backslash; 2 here since it
     *   is Java code).
     * @return a suggested chunk of xml for this dataset for use in datasets.xml
     * @throws Throwable if trouble. 
     *   If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(String tFileDir, String tFileNameRegex,
        int tReloadEveryNMinutes) throws Throwable {
        
        String2.log("\n*** EDDGridFromMergeIRFiles.generateDatasetsXml" +
            "\nfileDir=" + tFileDir + " fileNameRegex=" + tFileNameRegex +
            " reloadEveryNMinutes=" + tReloadEveryNMinutes);
        if (!String2.isSomething(tFileDir))
            throw new IllegalArgumentException("fileDir wasn't specified.");
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        if (tReloadEveryNMinutes < 0 || tReloadEveryNMinutes == Integer.MAX_VALUE)
            tReloadEveryNMinutes = 1440; //daily. More often than usual default.

        StringBuilder sb = new StringBuilder();
        //gather the results 
        String tDatasetID = "mergeIR";
        sb.append(directionsForGenerateDatasetsXml());

        //???
        //sb.append( "!!! The source for " + tDatasetID + " has nGridVariables=" + 2 +".\n");

        sb.append(
            "-->\n\n" +
            "<dataset type=\"EDDGridFromMergeIRFiles\" datasetID=\"" + tDatasetID +  "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <updateEveryNMillis>10000</updateEveryNMillis>\n" +  
            "    <fileDir>" + XML.encodeAsXML(tFileDir) + "</fileDir>\n" +
            "    <fileNameRegex>" + XML.encodeAsXML(tFileNameRegex) + "</fileNameRegex>\n" +
            "    <recursive>true</recursive>\n" +
            "    <pathRegex>.*</pathRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <fileTableInMemory>false</fileTableInMemory>\n");
         
        sb.append(
            "    <addAttributes>\n" +
            "        <att name=\"cdm_data_type\">Grid</att>\n" +
            "        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
            "        <att name=\"creator_name\">Bob Joyce</att>\n" +
            "        <att name=\"creator_email\">robert.joyce@noaa.gov</att>\n" +
            "        <att name=\"creator_url\">http://www.cpc.ncep.noaa.gov/</att>\n" +
            "        <att name=\"drawLandMask\">under</att>\n" +
            "        <att name=\"infoUrl\">http://www.cpc.ncep.noaa.gov/products/global_precip/html/README</att>\n" +
            "        <att name=\"institution\">NOAA NWS NCEP CPC</att>\n" +
            "        <att name=\"keywords\">4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature</att>\n" +
            "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
            "        <att name=\"license\">[standard]</att>\n" +
            "        <att name=\"summary\">" +
"The Climate Prediction Center/NCEP/NWS is now making available\n" +
"globally-merged (60N-60S) pixel-resolution IR brightness\n" +
"temperature data (equivalent blackbody temps), merged from all\n" +
"available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n" +
"GMS).  The availability of data from METEOSAT-7, which is\n" +
"located at 57E at the present time, yields a unique opportunity\n" +
"for total global (60N-60S) coverage.</att>\n" +
            "        <att name=\"title\">NCEP/CPC 4km Global (60N - 60S) IR Dataset</att>\n" +
            "    </addAttributes>\n");
          
        sb.append(
            "    <axisVariable>\n" +
            "        <sourceName>time</sourceName>\n" +
            "        <destinationName>time</destinationName>\n" +
            "        <addAttributes>\n" +
            "            <att name=\"axis\">T</att>\n" +
            "            <att name=\"delta_t\">0000-00-00 00:30:00</att>\n" +
            "            <att name=\"long_name\">Time</att>\n" +
            "            <att name=\"standard_name\">time</att>\n" +
            "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
            "        </addAttributes>\n" +
            "    </axisVariable>\n" +
            "    <axisVariable>\n" +
            "        <sourceName>latitude</sourceName>\n" +
            "        <addAttributes>\n" +
            "            <att name=\"units\">" + EDV.LAT_UNITS +"</att>\n" +
            "        </addAttributes>\n" +
            "    </axisVariable>\n" +
            "    <axisVariable>\n" +
            "        <sourceName>longitude</sourceName>\n" +
            "        <addAttributes>\n" +
            "            <att name=\"units\">" + EDV.LON_UNITS +"</att>\n" +
            "        </addAttributes>\n" +
            "    </axisVariable>\n");

        sb.append(
            "    <dataVariable>\n" +
            "        <sourceName>ir</sourceName>\n" +
            "        <dataType>short</dataType>\n" +
            "        <!-- sourceAttributes>\n" +
            "            <att name=\"colorBarMaximum\" type=\"int\">170</att>\n" +
            "            <att name=\"colorBarMinimum\" type=\"int\">330</att>\n" +
            "            <att name=\"ioos_cateory\">Heat Flux</att>\n" +
            "            <att name=\"long_name\">IR Brightness Temperature</att>\n" +
            "            <att name=\"missing_value\" type=\"short\">" + IR_MV + "</att>\n" +
            "            <att name=\"standard_name\">brightness_temperature</att>\n" +
            "            <att name=\"units\">degreeK</att>\n" +
            "        </sourceAttributes -->\n" +
            "        <addAttributes>\n" +
            "        </addAttributes>\n" +
            "    </dataVariable>\n" +
            "    <dataVariable>\n" +
            "        <sourceName>flux</sourceName>\n" +
            "        <dataType>double</dataType>\n" +
            "        <!-- sourceAttributes>\n" +
            "            <att name=\"colorBarMaximum\" type=\"double\">500.0</att>\n" +
            "            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
            "            <att name=\"ioos_cateory\">Heat Flux</att>\n" +
            "            <att name=\"long_name\">Flux</att>\n" +
            "            <att name=\"missing_value\" type=\"double\">" + FLUX_MV + "</att>\n" +
            "            <att name=\"standard_name\">surface_upwelling_shortwave_flux</att>\n" +
            "            <att name=\"units\">W/m^2</att>\n" +
            "        </sourceAttributes -->\n" +
            "        <addAttributes>\n" +
            "        </addAttributes>\n" +
            "    </dataVariable>\n");

        sb.append(
            "</dataset>\n" +
            "\n");

        return sb.toString();
    }

    /** This tests generateDatasetsXml. */
    public static void testGenerateDatasetsXml() throws Throwable {

        String2.log("\n*** EDDGridFromMergeIRFiles.testGenerateDatasetsXml");

        String results = generateDatasetsXml(
            EDStatic.unitTestDataDir + "mergeIR/", "merg_[0-9]{10}_4km-pixel\\.gz", -1) + "\n";

        //GenerateDatasetsXml
        String gdxResults = (new GenerateDatasetsXml()).doIt(new String[]{"-verbose", 
            "EDDGridFromMergeIRFiles", EDStatic.unitTestDataDir + "mergeIR/", 
            "merg_[0-9]{10}_4km-pixel\\.gz", "-1"}, //default reloadEvery
            false); //doIt loop?
        Test.ensureEqual(gdxResults, results, 
            "Unexpected results from GenerateDatasetsXml.doIt. " + 
            gdxResults.length() + " " + results.length());

        String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"\n" +
"<dataset type=\"EDDGridFromMergeIRFiles\" datasetID=\"mergeIR\" active=\"true\">\n" +
"    <reloadEveryNMinutes>1440</reloadEveryNMinutes>\n" +
"    <updateEveryNMillis>10000</updateEveryNMillis>\n" +
"    <fileDir>" + String2.unitTestDataDir + "mergeIR/</fileDir>\n" +
"    <fileNameRegex>merg_[0-9]{10}_4km-pixel\\.gz</fileNameRegex>\n" +
"    <recursive>true</recursive>\n" +
"    <pathRegex>.*</pathRegex>\n" +
"    <metadataFrom>last</metadataFrom>\n" +
"    <fileTableInMemory>false</fileTableInMemory>\n" +
"    <addAttributes>\n" +
"        <att name=\"cdm_data_type\">Grid</att>\n" +
"        <att name=\"Conventions\">COARDS, CF-1.6, ACDD-1.3</att>\n" +
"        <att name=\"creator_name\">Bob Joyce</att>\n" +
"        <att name=\"creator_email\">robert.joyce@noaa.gov</att>\n" +
"        <att name=\"creator_url\">http://www.cpc.ncep.noaa.gov/</att>\n" +
"        <att name=\"drawLandMask\">under</att>\n" +
"        <att name=\"infoUrl\">http://www.cpc.ncep.noaa.gov/products/global_precip/html/README</att>\n" +
"        <att name=\"institution\">NOAA NWS NCEP CPC</att>\n" +
"        <att name=\"keywords\">4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature</att>\n" +
"        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
"        <att name=\"license\">[standard]</att>\n" +
"        <att name=\"summary\">The Climate Prediction Center/NCEP/NWS is now making available\n" +
"globally-merged (60N-60S) pixel-resolution IR brightness\n" +
"temperature data (equivalent blackbody temps), merged from all\n" +
"available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n" +
"GMS).  The availability of data from METEOSAT-7, which is\n" +
"located at 57E at the present time, yields a unique opportunity\n" +
"for total global (60N-60S) coverage.</att>\n" +
"        <att name=\"title\">NCEP/CPC 4km Global (60N - 60S) IR Dataset</att>\n" +
"    </addAttributes>\n" +
"    <axisVariable>\n" +
"        <sourceName>time</sourceName>\n" +
"        <destinationName>time</destinationName>\n" +
"        <addAttributes>\n" +
"            <att name=\"axis\">T</att>\n" +
"            <att name=\"delta_t\">0000-00-00 00:30:00</att>\n" +
"            <att name=\"long_name\">Time</att>\n" +
"            <att name=\"standard_name\">time</att>\n" +
"            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>latitude</sourceName>\n" +
"        <addAttributes>\n" +
"            <att name=\"units\">degrees_north</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <axisVariable>\n" +
"        <sourceName>longitude</sourceName>\n" +
"        <addAttributes>\n" +
"            <att name=\"units\">degrees_east</att>\n" +
"        </addAttributes>\n" +
"    </axisVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>ir</sourceName>\n" +
"        <dataType>short</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"int\">170</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"int\">330</att>\n" +
"            <att name=\"ioos_cateory\">Heat Flux</att>\n" +
"            <att name=\"long_name\">IR Brightness Temperature</att>\n" +
"            <att name=\"missing_value\" type=\"short\">" + IR_MV + "</att>\n" +                        
"            <att name=\"standard_name\">brightness_temperature</att>\n" +
"            <att name=\"units\">degreeK</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"    <dataVariable>\n" +
"        <sourceName>flux</sourceName>\n" +
"        <dataType>double</dataType>\n" +
"        <!-- sourceAttributes>\n" +
"            <att name=\"colorBarMaximum\" type=\"double\">500.0</att>\n" +
"            <att name=\"colorBarMinimum\" type=\"double\">0.0</att>\n" +
"            <att name=\"ioos_cateory\">Heat Flux</att>\n" +
"            <att name=\"long_name\">Flux</att>\n" +
"            <att name=\"missing_value\" type=\"double\">" + FLUX_MV + "</att>\n" +                        
"            <att name=\"standard_name\">surface_upwelling_shortwave_flux</att>\n" +
"            <att name=\"units\">W/m^2</att>\n" +
"        </sourceAttributes -->\n" +
"        <addAttributes>\n" +
"        </addAttributes>\n" +
"    </dataVariable>\n" +
"</dataset>\n" +
"\n\n";
        Test.ensureEqual(results, expected, results.length() + " " + expected.length() + 
            "\nresults=\n" + results);

        //ensure it is ready-to-use by making a dataset from it
        EDD edd = oneFromXmlFragment(null, results);
        Test.ensureEqual(edd.className(), "EDDGridFromMergeIRFiles", "className");
        Test.ensureEqual(edd.title(), "NCEP/CPC 4km Global (60N - 60S) IR Dataset", "title");
        Test.ensureEqual(String2.toCSSVString(edd.dataVariableDestinationNames()), 
            "ir, flux", "dataVariableDestinationNames");

        String2.log("\nEDDGridFromMergeIRFiles.testGenerateDatasetsXml passed the test.");
    }


    /** This tests this class. */
    public static void testMergeIR() throws Throwable {

        String2.log("\n*** testMergeIRgz\n");
        testVerboseOn();
        //String2.log(NcHelper.dumpString("/erddapTest/mergeIR/merg_20150101_4km-pixel", false));
        EDDGrid edd   = (EDDGrid)oneFromDatasetsXml(null, "mergeIR");   //from uncompressed files
        EDDGrid eddZ  = (EDDGrid)oneFromDatasetsXml(null, "mergeIRZ");  //from .Z files
        EDDGrid eddgz = (EDDGrid)oneFromDatasetsXml(null, "mergeIRgz"); //from .gz files
        String dir = EDStatic.fullTestCacheDirectory;
        String tName, results, expected, dapQuery;
        int po;

        //.dds
        expected = 
"Dataset {\n" +
"  Float64 time[time = 4];\n" +
"  Float32 latitude[latitude = 3298];\n" +
"  Float32 longitude[longitude = 9896];\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Int16 ir[time = 4][latitude = 3298][longitude = 9896];\n" +
"    MAPS:\n" +
"      Float64 time[time = 4];\n" +
"      Float32 latitude[latitude = 3298];\n" +
"      Float32 longitude[longitude = 9896];\n" +
"  } ir;\n" +
"  GRID {\n" +
"    ARRAY:\n" +
"      Float64 flux[time = 4][latitude = 3298][longitude = 9896];\n" +
"    MAPS:\n" +
"      Float64 time[time = 4];\n" +
"      Float32 latitude[latitude = 3298];\n" +
"      Float32 longitude[longitude = 9896];\n" +
"  } flux;\n" +
"} mergeIR;\n";
        //uncompressed
        tName = edd.makeNewFileForDapQuery(null, null, "", 
            dir, edd.className() + "_", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results, expected, "results=\n" + results);

        //Z
        expected = String2.replaceAll(expected, "mergeIR;", "mergeIRZ;");
        tName = eddZ.makeNewFileForDapQuery(null, null, "", 
            dir, eddZ.className() + "_Z", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results, expected, "Z results=\n" + results);

        //gz
        expected = String2.replaceAll(expected, "mergeIRZ;", "mergeIRgz;");
        tName = eddgz.makeNewFileForDapQuery(null, null, "", 
            dir, eddgz.className() + "_gz", ".dds"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results, expected, "gz results=\n" + results);


        //*** .das
        expected = 
"Attributes {\n" +
"  time {\n" +
"    String _CoordinateAxisType \"Time\";\n" +
"    Float64 actual_range 1.4200704e+9, 1.4200758e+9;\n" +
"    String axis \"T\";\n" +
"    String delta_t \"0000-00-00 00:30:00\";\n" +
"    String ioos_category \"Time\";\n" +
"    String long_name \"Time\";\n" +
"    String standard_name \"time\";\n" +
"    String time_origin \"01-JAN-1970 00:00:00\";\n" +
"    String units \"seconds since 1970-01-01T00:00:00Z\";\n" +
"  }\n" +
"  latitude {\n" +
"    String _CoordinateAxisType \"Lat\";\n" +
"    Float32 actual_range -59.982, 59.97713;\n" +
"    String axis \"Y\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Latitude\";\n" +
"    String standard_name \"latitude\";\n" +
"    String units \"degrees_north\";\n" +
"  }\n" +
"  longitude {\n" +
"    String _CoordinateAxisType \"Lon\";\n" +
"    Float32 actual_range 0.0182, 359.9695;\n" +
"    String axis \"X\";\n" +
"    String ioos_category \"Location\";\n" +
"    String long_name \"Longitude\";\n" +
"    String standard_name \"longitude\";\n" +
"    String units \"degrees_east\";\n" +
"  }\n" +
"  ir {\n" +
"    Int32 colorBarMaximum 330;\n" +
"    Int32 colorBarMinimum 170;\n" +
"    String ioos_category \"Heat Flux\";\n" +
"    String long_name \"IR Brightness Temperature\";\n" +
"    Int16 missing_value " + IR_MV + ";\n" +                        
"    String standard_name \"brightness_temperature\";\n" +
"    String units \"degreeK\";\n" +
"  }\n" +
"  flux {\n" +
"    Float64 colorBarMaximum 500.0;\n" +
"    Float64 colorBarMinimum 0.0;\n" +
"    String ioos_category \"Heat Flux\";\n" +
"    String long_name \"Flux\";\n" +
"    Float64 missing_value " + FLUX_MV + ";\n" +                        
"    String standard_name \"surface_upwelling_shortwave_flux\";\n" +
"    String units \"W/m^2\";\n" +
"  }\n" +
"  NC_GLOBAL {\n" +
"    String cdm_data_type \"Grid\";\n" +
"    String Conventions \"COARDS, CF-1.6, ACDD-1.3\";\n" +
"    String creator_email \"robert.joyce@noaa.gov\";\n" +
"    String creator_name \"Bob Joyce\";\n" +
"    String creator_type \"person\";\n" +
"    String creator_url \"http://www.cpc.ncep.noaa.gov/\";\n" +
"    Float64 Easternmost_Easting 359.9695;\n" +
"    Float64 geospatial_lat_max 59.97713;\n" +
"    Float64 geospatial_lat_min -59.982;\n" +
"    Float64 geospatial_lat_resolution 0.03638432817713073;\n" +
"    String geospatial_lat_units \"degrees_north\";\n" +
"    Float64 geospatial_lon_max 359.9695;\n" +
"    Float64 geospatial_lon_min 0.0182;\n" +
"    Float64 geospatial_lon_resolution 0.03637708943911066;\n" +
"    String geospatial_lon_units \"degrees_east\";\n" +
"    String history \""; 

//2015-03-20T17:28:57Z (local files)\n" +
//"2015-03-20T17:28:57Z 
String expected2 = 
"http://localhost:8080/cwexperimental/griddap/mergeIR.das\";\n" +
"    String infoUrl \"http://www.cpc.ncep.noaa.gov/products/global_precip/html/README\";\n" +
"    String institution \"NOAA NWS NCEP CPC\";\n" +
"    String keywords \"4km, brightness, cpc, flux, global, ir, merge, ncep, noaa, nws, temperature\";\n" +
"    String keywords_vocabulary \"GCMD Science Keywords\";\n" +
"    String license \"The data may be used and redistributed for free but is not intended\n" +
"for legal use, since it may contain inaccuracies. Neither the data\n" +
"Contributor, ERD, NOAA, nor the United States Government, nor any\n" +
"of their employees or contractors, makes any warranty, express or\n" +
"implied, including warranties of merchantability and fitness for a\n" +
"particular purpose, or assumes any legal liability for the accuracy,\n" +
"completeness, or usefulness, of this information.\";\n" +
"    Float64 Northernmost_Northing 59.97713;\n" +
"    String sourceUrl \"(local files)\";\n" +
"    Float64 Southernmost_Northing -59.982;\n" +
"    String summary \"The Climate Prediction Center/NCEP/NWS is now making available\n" +
"globally-merged (60N-60S) pixel-resolution IR brightness\n" +
"temperature data (equivalent blackbody temps), merged from all\n" +
"available geostationary satellites (GOES-8/10, METEOSAT-7/5 and\n" +
"GMS).  The availability of data from METEOSAT-7, which is\n" +
"located at 57E at the present time, yields a unique opportunity\n" +
"for total global (60N-60S) coverage.\";\n" +
"    String time_coverage_end \"2015-01-01T01:30:00Z\";\n" +
"    String time_coverage_start \"2015-01-01T00:00:00Z\";\n" +
"    String title \"NCEP/CPC 4km Global (60N - 60S) IR Dataset\";\n" +
"    Float64 Westernmost_Easting 0.0182;\n" +
"  }\n" +
"}\n";
        //uncompressed
        tName = edd.makeNewFileForDapQuery(null, null, "", 
            dir, edd.className() + "_", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results.substring(0, expected.length()), expected, "results=\n" + results);

        po = results.indexOf(expected2.substring(0, 20));
        Test.ensureEqual(results.substring(po), expected2, "results=\n" + results);

        //Z
        expected2 = String2.replaceAll(expected2, "mergeIR.das", "mergeIRZ.das");
        tName = eddZ.makeNewFileForDapQuery(null, null, "", 
            dir, eddZ.className() + "_Z", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results.substring(0, expected.length()), expected, "Z results=\n" + results);

        po = results.indexOf(expected2.substring(0, 20));
        Test.ensureEqual(results.substring(po), expected2, "Z results=\n" + results);

        //gz
        expected2 = String2.replaceAll(expected2, "mergeIRZ.das", "mergeIRgz.das");
        tName = eddgz.makeNewFileForDapQuery(null, null, "", 
            dir, eddgz.className() + "_gz", ".das"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results.substring(0, expected.length()), expected, "gz results=\n" + results);

        po = results.indexOf(expected2.substring(0, 20));
        Test.ensureEqual(results.substring(po), expected2, "gz results=\n" + results);


        //*** data
        dapQuery = "ir[0][0:1000:3200][0:1000:9800],flux[0][0:1000:3200][0:1000:9800]";
        expected = 
"time,latitude,longitude,ir,flux\n" +
"UTC,degrees_north,degrees_east,degreeK,W/m^2\n" +
"2015-01-01T00:00:00Z,-59.982,0.0182,237,154.4\n" +
"2015-01-01T00:00:00Z,-59.982,36.396515,273,236.1\n" +
"2015-01-01T00:00:00Z,-59.982,72.77347,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,109.15042,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,145.52737,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,181.90433,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,218.28128,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,254.65823,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,291.0352,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-59.982,327.41214,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-23.597416,0.0182,271,230.4\n" +
"2015-01-01T00:00:00Z,-23.597416,36.396515,296,312.5\n" +
"2015-01-01T00:00:00Z,-23.597416,72.77347,285,273.3\n" +
"2015-01-01T00:00:00Z,-23.597416,109.15042,283,266.7\n" +
"2015-01-01T00:00:00Z,-23.597416,145.52737,NaN,NaN\n" +
"2015-01-01T00:00:00Z,-23.597416,181.90433,293,301.3\n" +
"2015-01-01T00:00:00Z,-23.597416,218.28128,294,305.0\n" +
"2015-01-01T00:00:00Z,-23.597416,254.65823,242,163.4\n" +
"2015-01-01T00:00:00Z,-23.597416,291.0352,294,305.0\n" +
"2015-01-01T00:00:00Z,-23.597416,327.41214,284,270.0\n" +
"2015-01-01T00:00:00Z,12.786415,0.0182,283,266.7\n" +
"2015-01-01T00:00:00Z,12.786415,36.396515,208,114.2\n" +
"2015-01-01T00:00:00Z,12.786415,72.77347,290,290.5\n" +
"2015-01-01T00:00:00Z,12.786415,109.15042,295,308.7\n" +
"2015-01-01T00:00:00Z,12.786415,145.52737,294,305.0\n" +
"2015-01-01T00:00:00Z,12.786415,181.90433,282,263.5\n" +
"2015-01-01T00:00:00Z,12.786415,218.28128,296,312.5\n" +
"2015-01-01T00:00:00Z,12.786415,254.65823,288,283.5\n" +
"2015-01-01T00:00:00Z,12.786415,291.0352,245,169.1\n" +
"2015-01-01T00:00:00Z,12.786415,327.41214,264,211.7\n" +
"2015-01-01T00:00:00Z,49.170914,0.0182,277,247.9\n" +
"2015-01-01T00:00:00Z,49.170914,36.396515,269,224.9\n" +
"2015-01-01T00:00:00Z,49.170914,72.77347,276,244.9\n" +
"2015-01-01T00:00:00Z,49.170914,109.15042,275,241.9\n" +
"2015-01-01T00:00:00Z,49.170914,145.52737,279,254.0\n" +
"2015-01-01T00:00:00Z,49.170914,181.90433,264,211.7\n" +
"2015-01-01T00:00:00Z,49.170914,218.28128,262,206.7\n" +
"2015-01-01T00:00:00Z,49.170914,254.65823,281,260.3\n" +
"2015-01-01T00:00:00Z,49.170914,291.0352,282,263.5\n" +
"2015-01-01T00:00:00Z,49.170914,327.41214,250,179.3\n";
        //uncompressed
        tName = edd.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, edd.className() + "_", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results, expected, "results=\n" + results);

        //Z
        tName = eddZ.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddZ.className() + "_Z", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results, expected, "Z results=\n" + results);

        //gz
        tName = eddgz.makeNewFileForDapQuery(null, null, dapQuery, 
            dir, eddgz.className() + "_gz", ".csv"); 
        results = new String((new ByteArray(dir + tName)).toArray());
        Test.ensureEqual(results, expected, "gz results=\n" + results);

        String2.log("\n*** testMergeIR() finished successfully");
    }

    /** This tests this class. */
    public static void test() throws Throwable {
/* for releases, this line should have open/close comment */
        testGenerateDatasetsXml();
        testMergeIR();
    }

}

