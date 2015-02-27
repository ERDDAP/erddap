/* 
 * EDDGridFromMergeIRFiles Copyright 2014, R.Tech.
 * See the LICENSE.txt file in this file's directory.
 */
//JL
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.StringArray;
import com.cohort.util.File2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import dods.dap.Server.InvalidParameterException;

import gov.noaa.pfel.erddap.variable.*;
import java.io.FileInputStream;
import java.nio.file.FileSystemException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * This class represents gridded data aggregated from a collection of NCEP/CPC
 * 4km Global (60N - 60S) IR Dataset data files.
 * (http://www.cpc.ncep.noaa.gov/products/global_precip/html/README)
 *
 * @author
 */
public class EDDGridFromMergeIRFiles extends EDDGridFromFiles {


    private static final int NLON = 9896;
    private static final int NLAT = 3298;
    
    private static final double SLON = 0.036378335;
    private static final double SLAT = 0.036383683;
    
    private static final double SIGMA = 5.67E-8;
    /**
     * The constructor just calls the super constructor.
     */
    public EDDGridFromMergeIRFiles(String tDatasetID, String tAccessibleTo,
            StringArray tOnChange, String tFgdcFile, String tIso19115File,
            String tDefaultDataQuery, String tDefaultGraphQuery,
            Attributes tAddGlobalAttributes,
            Object[][] tAxisVariables,
            Object[][] tDataVariables,
            int tReloadEveryNMinutes, int tUpdateEveryNMillis,
            String tFileDir, boolean tRecursive, String tFileNameRegex, 
            String tMetadataFrom, boolean tEnsureAxisValuesAreExactlyEqual, 
            boolean tFileTableInMemory, boolean tAccessibleViaFiles)
            throws Throwable {

        super("EDDGridFromMergeIRFiles", tDatasetID, tAccessibleTo,
                tOnChange, tFgdcFile, tIso19115File,
                tDefaultDataQuery, tDefaultGraphQuery,
                tAddGlobalAttributes,
                tAxisVariables,
                tDataVariables,
                tReloadEveryNMinutes, tUpdateEveryNMillis,
                tFileDir, tRecursive, tFileNameRegex, 
                tMetadataFrom, tEnsureAxisValuesAreExactlyEqual, 
                tFileTableInMemory, tAccessibleViaFiles);

        if (verbose) String2.log("\n*** constructing EDDGridFromMergeIRFiles(xmlReader)...");

        
    }

    /**
     * This gets sourceGlobalAttributes and sourceDataAttributes from the
     * specified source file.
     *
     * @param fileDir
     * @param fileName
     * @param sourceAxisNames
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
    public void getSourceMetadata(String fileDir, String fileName,
            StringArray sourceAxisNames,
            StringArray sourceDataNames, 
            String sourceDataTypes[],
            Attributes sourceGlobalAttributes,
            Attributes sourceAxisAttributes[],
            Attributes sourceDataAttributes[]) throws Throwable {
        
        if (verbose) String2.log("in getSourceMetadata");
        
                String getWhat = "globalAttributes";
        try {
            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                if (verbose) String2.log("axisAttributes for avi=" + avi + " name=" + sourceAxisNames.get(avi));
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
                if (verbose) String2.log("dataAttributes for avi=" + avi + " name=" + sourceDataNames.get(avi));
                switch(sourceDataNames.get(avi)) {
                    case "ir" :
                        sourceDataAttributes[avi].add("ioos_category", "Other");                        
                        sourceDataAttributes[avi].add("standard_name", "ir");
                        sourceDataAttributes[avi].add("units", "degreeK");
                        break;       
                    case "flux" :
                        sourceDataAttributes[avi].add("ioos_category", "Other");                        
                        sourceDataAttributes[avi].add("standard_name", "flux");
                        sourceDataAttributes[avi].add("units", "W/m^2");
                        break;          
                    default : 
                        sourceAxisAttributes[avi].add("standard_name", "noName");
                        break;
                }
            }

        } catch (Throwable t) {            
            throw new RuntimeException("Error in EDDGridFromNcFiles.getSourceMetadata" +
                "\nwhile getting " + getWhat + 
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
     * @return a PrimitiveArray[] with the results (with the requested
     * sourceDataTypes). It needn't set sourceGlobalAttributes or
     * sourceDataAttributes (but see getSourceMetadata).
     * @throws Throwable if trouble (e.g., invalid file). If there is trouble,
     * this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] getSourceAxisValues(String fileDir, String fileName,
            StringArray sourceAxisNames) throws Throwable {

        String getWhat = "globalAttributes";
        
        try {
            PrimitiveArray[] avPa = new PrimitiveArray[sourceAxisNames.size()];

            for (int avi = 0; avi < sourceAxisNames.size(); avi++) {
                String avName = sourceAxisNames.get(avi);
                getWhat = "axisAttributes for variable=" + avName;

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
                        if (verbose) String2.log("case time with " + fileName);
                        String sdate = fileName.replaceAll("merg_", "").replaceAll("_4km-pixel.gz", "");
                        if (verbose) String2.log("date = " + sdate + " (" + sdate.length() + ")");

                        if (sdate.length() != 10) {
                            if (verbose) String2.log("myException will thow");                                                    
                            throw new RuntimeException("File name (" + fileName + ")must contains a date encoded on 10 char");
                        }

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
                        if (verbose) String2.log(getWhat);

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
     * customized for this file.
     * @return a PrimitiveArray[] with an element for each tDataVariable with
     * the dataValues.
     * <br>The dataValues are straight from the source, not modified.
     * <br>The primitiveArray dataTypes are usually the sourceDataTypeClass, but
     * can be any type. EDDGridFromFiles will convert to the
     * sourceDataTypeClass.
     * <br>Note the lack of axisVariable values!
     * @throws Throwable if trouble (notably, WaitThenTryAgainException). If
     * there is trouble, this doesn't call addBadFile or requestReloadASAP().
     */
    public PrimitiveArray[] getSourceDataFromFile(String fileDir, String fileName,
            EDV tDataVariables[], IntArray tConstraints) throws Throwable {
        
        if (verbose) String2.log("in getSourceDataFromFile(" + fileDir + ", " + fileName + ", " + tDataVariables + ", " + tConstraints + ")");
        
         //make the selection spec  and get the axis values
         int nbAxisAvriable = axisVariables.length;         
         int nbDataVariable = tDataVariables.length;
         PrimitiveArray[] paa = new PrimitiveArray[nbDataVariable];
         StringBuilder selectionSB = new StringBuilder();
         
         int minTime = 0, maxTime = 0, strideTime = 0;               
         int minLat = 0, maxLat = 0, strideLat = 0;     
         int minLon = 0, maxLon = 0, strideLon = 0;         
         
         for (int avi = 0; avi < nbAxisAvriable; avi++) {
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

        if(maxLat == minLat) {
            maxLat = minLat + 1;
        } else {
            maxLat++;
        }
        int nbLat = (int)((maxLat - minLat) / strideLat);     
        if(maxLon == minLon) {
            maxLon = minLon + 1;
        } else {
            maxLon++;
        }
        int nbLon = (int)((maxLon - minLon) / strideLon);    
        if(maxTime == minTime) {
            maxTime = minTime + 1;
        }
        else {
            maxTime++;
        }
        int nbTime = (int)((maxTime - minTime) / strideTime);
        
        int total = nbLat * nbLon * nbTime;//data length
        
        if (verbose) String2.log("contrains : " + selection);
        if (verbose) String2.log("minLat : " + minLat + ", maxLat : " + maxLat + ", strideLat : " + strideLat + ", nb : " + nbLat);
        if (verbose) String2.log("minLon : " + minLon + ", maxLon : " + maxLon + ", strideLon : " + strideLon + ", nb : " + nbLon);
        if (verbose) String2.log("minTime : " + minTime + ", maxTime : " + maxTime + ", strideTime : " + strideTime + ", nb : " + nbTime);
        if (verbose) String2.log("total size : " + total);
         
	int indexOut = 0;//index in data array
        
        if (verbose) String2.log("Opening file " + fileDir + fileName);
         GZIPInputStream gzFile = new GZIPInputStream(new FileInputStream(fileDir + fileName));//may throw exception
         try {
             
            byte[] in = new byte[NLON * NLAT * 2];
            short[] out1 = new short[total];         
            double[] out2 = new double[total];
            
            //entire read of the file
            if (verbose) String2.logNoNewline("Reading entire file...");
            long t0 = System.currentTimeMillis();
            int indexIn = 0;
            byte[] buff = new byte[NLAT];
            int n = -2;
            while((n = gzFile.read(buff, 0, buff.length)) != -1) {
                System.arraycopy(buff, 0, in, indexIn, n);
                indexIn += n;
            }
            if(indexIn != in.length) {
                throw new FileSystemException("Merge file seems to be corrupted because size is : " + indexIn + " and should be " + in.length);
            }
            if (verbose) String2.log("Done in " + (System.currentTimeMillis() - t0) + "ms");
            if (verbose) String2.logNoNewline("Closing file...");
            gzFile.close();//I care about this exception                        
            if (verbose) String2.log("Done");
            
            if (verbose) String2.logNoNewline("Copy filtered data...");
            t0 = System.currentTimeMillis();
            for(int t = minTime; t < maxTime; t+= strideTime) {//[0 - 1]
                int offsetTime = NLON * NLAT * t;
                for (int la = minLat; la < maxLat; la += strideLat) {
                    int offsetLat = NLON * la;
                    for (int lo = minLon; lo < maxLon; lo += strideLon) {      
                        short value = (short)(in[offsetTime + offsetLat + lo] & 0xff);
                        value += 75;
                        out1[indexOut] = value;
                        out2[indexOut] = ((double)Math.round(T2F(value) * 10.)) / 10.;
                        indexOut++;
                    }                       
                }                                       
            }
            if (verbose) {
                String2.log("Done in " + (System.currentTimeMillis() - t0) + "ms");
                if(total >= 10) {
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
            if(nbDataVariable == 2) {
                paa[0] = PrimitiveArray.factory(out1);            
                paa[1] = PrimitiveArray.factory(out2);
            } else if(nbDataVariable == 1) {
                if(tDataVariables[0].sourceName().equalsIgnoreCase("ir")) {
                    paa[0] = PrimitiveArray.factory(out1);
                } else {
                    paa[0] = PrimitiveArray.factory(out2);
                }
            }//else 0
            
         } catch (Throwable t) {
            //make sure it is explicitly closed
            try {   
               gzFile.close();    
            } catch (Throwable t2) {
               if (verbose) String2.log("Error while trying to close " + fileDir + fileName +
               "\n" + MustBe.throwableToShortString(t2));
            }
            throw t;
        }
         return paa;
    }
    
    private static double T2F(double pT)
{ 
    // relation to go from brightness temperature to flux
    //return (T-162.)*(300./(330.-162.));
    //return T;
    //return SIGMA*T*T*T*T;
    //return (T-120)*(325.0/(330-75.0));
    //return (T-140)*(400.0/(330-75.0));
    boolean model2007=true;
    if (model2007)
    {
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
    public EDDGrid sibling(String tLocalSourceUrl, int ensureAxisValuesAreEqual,
            boolean shareInfo) throws Throwable {
        throw new SimpleException("Error: "
                + "EDDGridFromNcFiles doesn't support method=\"sibling\".");

    }

    /**
     * This does its best to generate a clean, ready-to-use datasets.xml entry
     * for an EDDGridFromNcFiles. The XML can then be edited by hand and added
     * to the datasets.xml file.
     *
     * <p>
     * This can't be made into a web service because it would allow any user to
     * looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex the regex that each filename (no directory info)
     * must match (e.g., ".*\\.nc") (usually only 1 backslash; 2 here since it
     * is Java code).
     * @return a suggested chunk of xml for this dataset for use in datasets.xml
     * @throws Throwable if trouble, e.g., if no Grid or Array variables are
     * found. If no trouble, then a valid dataset.xml chunk has been returned.
     */
    public static String generateDatasetsXml(
            String tFileDir, String tFileNameRegex,
            int tReloadEveryNMinutes) throws Throwable {

        
         String2.log("EDDGridFromMergeIRFiles.generateDatasetsXml");
         tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash

         StringBuilder sb = new StringBuilder();
         //gather the results 
         String tDatasetID = "mergeIR";
         sb.append(directionsForGenerateDatasetsXml());

         sb.append( "!!! The source for " + tDatasetID + " has nGridVariables=" + 2 +".\n");
         sb.append(
                    "-->\n\n" +
                    "<dataset type=\"EDDGridFromNcFiles\" datasetID=\"" + tDatasetID +  "\" active=\"true\">\n" +
                    "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
                    "    <fileDir>" + tFileDir + "</fileDir>\n" +
                    "    <recursive>false</recursive>\n" +
                    "    <fileNameRegex>" + tFileNameRegex + "</fileNameRegex>\n" +
                    "    <metadataFrom>last</metadataFrom>\n" +
                    "    <fileTableInMemory>false</fileTableInMemory>\n");
         
          sb.append("<addAttributes>\n" +
                "        <att name=\"cdm_data_type\">Grid</att>\n" +
                "        <att name=\"Conventions\">?</att>\n" +
                "        <att name=\"infoUrl\">?</att>\n" +
                "        <att name=\"institution\">NOAA</att>\n" +
                "        <att name=\"keywords\">ir</att>\n" +
                "        <att name=\"keywords_vocabulary\">GCMD Science Keywords</att>\n" +
                "        <att name=\"license\">[standard]</att>\n" +
                "        <att name=\"summary\">NCEP/CPC 4km Global (60N - 60S) IR Dataset.</att>\n" +
                "        <att name=\"title\">NCEP/CPC 4km Global (60N - 60S) IR Dataset</att>\n" +
                "    </addAttributes>");
          
          sb.append("<axisVariable>\n" +
                "        <sourceName>time</sourceName>\n" +
                "		<destinationName>time</destinationName>\n" +
                "        <addAttributes>\n" +
                "            <att name=\"axis\">T</att>\n" +
                "            <att name=\"delta_t\">0000-00-00 00:30:00</att>\n" +
                "            <att name=\"long_name\">Time</att>\n" +
                "            <att name=\"standard_name\">time</att>\n" +
                "            <att name=\"units\">seconds since 1970-01-01T00:00:00Z</att>\n" +
                "         </addAttributes>\n" +
                "    </axisVariable>\n" +
                "    <axisVariable>\n" +
                "        <sourceName>latitude</sourceName>\n" +
                "    </axisVariable>\n" +
                "    <axisVariable>\n" +
                "        <sourceName>longitude</sourceName>\n" +
                "    </axisVariable>");
           sb.append("<dataVariable>\n" +
                "       <sourceName>ir</sourceName>\n" +
                "	<dataType>short</dataType>\n" +
                "    </dataVariable>\n" +
                "    <dataVariable>\n" +
                "       <sourceName>flux</sourceName>\n" +
                "	<dataType>double</dataType>\n" +
                "    </dataVariable>\n");
         
         sb.append("</dataset>\n");

         return sb.toString();

    }

}

