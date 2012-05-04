/* 
 * EDDTableFromNcCFFiles Copyright 2011, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.ByteArray;
//import com.cohort.array.CharArray;
import com.cohort.array.DoubleArray;
//import com.cohort.array.FloatArray;
//import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
//import com.cohort.array.ShortArray;
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
//import gov.noaa.pfel.coastwatch.util.RegexFilenameFilter;
//import gov.noaa.pfel.coastwatch.util.SSR;

import gov.noaa.pfel.erddap.GenerateDatasetsXml;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.*;

//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.FileWriter;
//import java.io.Writer;
//import java.util.Arrays;
//import java.util.BitSet;
//import java.util.HashMap;
//import java.util.HashSet;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * Get netcdf-X.X.XX.jar from http://www.unidata.ucar.edu/software/netcdf-java/index.htm
 * and copy it to <context>/WEB-INF/lib renamed as netcdf-latest.jar.
 * Get slf4j-jdk14.jar from 
 * ftp://ftp.unidata.ucar.edu/pub/netcdf-java/slf4j-jdk14.jar
 * and copy it to <context>/WEB-INF/lib.
 * Put both of these .jar files in the classpath for the compiler and for Java.
 */
//import ucar.ma2.*;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.Member;
//import ucar.nc2.*;
//import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.DataIterator;
//import ucar.nc2.dods.*;
import ucar.nc2.ft.*;
import ucar.nc2.units.*;
//import ucar.nc2.util.*;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.Station;

/** 
 * This class represents a table of data from a collection of FeatureDatasets
 * using CF Discrete Sampling Geometries (was Point Observation Conventions), 
 * https://cf-pcmdi.llnl.gov/trac/wiki/PointObservationConventions (currently out-of-date).
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2011-01-27
 */
public class EDDTableFromNcCFFiles extends EDDTableFromFiles { 


    /** 
     * The constructor just calls the super constructor. 
     *
     * @param tAccessibleTo is a comma separated list of 0 or more
     *    roles which will have access to this dataset.
     *    <br>If null, everyone will have access to this dataset (even if not logged in).
     *    <br>If "", no one will have access to this dataset.
     * @param tFileDir the base URL or file directory. 
     *    See http://www.unidata.ucar.edu/software/netcdf-java/v4.2/javadoc/index.html
     *    FeatureDatasetFactoryManager open().
     *    This may be a
     *    <ul>
     *    <li> local file (.nc or compatible)
     *    <li> thredds catalog#dataset (with a thredds: prefix)
     *    <li> cdmremote dataset (with an cdmremote: prefix)
     *    <li> collection dataset (with a collection: prefix)
     *    </ul>
     * @param tSortedColumnSourceName can't be for a char/String variable
     *   because NcHelper binary searches are currently set up for numeric vars only.
     *
     */
    public EDDTableFromNcCFFiles(boolean tIsLocal, 
        String tDatasetID, String tAccessibleTo,
        StringArray tOnChange, 
        Attributes tAddGlobalAttributes,
        double tAltMetersPerSourceUnit, 
        Object[][] tDataVariables,
        int tReloadEveryNMinutes,
        String tFileDir, boolean tRecursive, String tFileNameRegex, String tMetadataFrom,
        int tColumnNamesRow, int tFirstDataRow,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex, 
        String tColumnNameForExtract,
        String tSortedColumnSourceName, String tSortFilesBySourceNames,
        boolean tSourceNeedsExpandedFP_EQ) 
        throws Throwable {

        super("EDDTableFromNcCFFiles", 
            tIsLocal, 
            tDatasetID, tAccessibleTo, tOnChange, 
            tAddGlobalAttributes, tAltMetersPerSourceUnit, 
            tDataVariables, tReloadEveryNMinutes,
            tFileDir, tRecursive, tFileNameRegex, tMetadataFrom,
            tColumnNamesRow, tFirstDataRow,
            tPreExtractRegex, tPostExtractRegex, tExtractRegex, tColumnNameForExtract,
            "time", //sortedColumnSourceName; time's sourceName is always time; see getSourceDataFromFile2
            tSortFilesBySourceNames,
            tSourceNeedsExpandedFP_EQ);

    }

    /**
     * This gets source data from one file.
     * See documentation in EDDTableFromFiles.
     * 
     * <p>For this class, sortedColumn is always time. See constructor.
     */
    public Table lowGetSourceDataFromFile(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double sortedSpacing, double minSorted, double maxSorted, 
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        return readData(fileDir, fileName, 
            sourceDataNames, sourceDataTypes, 
            Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
            minSorted, maxSorted, 
            getMetadata, mustGetData);
    }

    /**
     * This gets source data from one source (e.g., a file).
     * See documentation in EDDTableFromFiles.
     *
     * @param sourceDataNames If null, all available will be returned
     * @param sourceDataTypes If null, they will be returned with their native dataTypes
     * @param minTime is epochSeconds
     * @param maxTime is epochSeconds
     * @return table with the data.   Time will be in epochSeconds.
     */
    public static Table readData(String fileDir, String fileName, 
        StringArray sourceDataNames, String sourceDataTypes[],
        double minLon, double maxLon,
        double minLat, double maxLat,
        double minAlt, double maxAlt,
        double minTime, double maxTime,
        boolean getMetadata, boolean mustGetData) 
        throws Throwable {

        String2.log("\n*** readData(" + fileDir + fileName + ")");
String2.log(NcHelper.dumpString(fileDir + fileName, true));
/*
        //make dateRange 
        if (Double.isNaN(minTime)) minTime = -6.2167392E10; //Calendar2.isoStringToEpochSeconds("0000-01-01");
        if (Double.isNaN(maxTime)) maxTime = 3.250368E10;   //Calendar2.isoStringToEpochSeconds("3000-01-01");
String2.log("minTime=" + minTime + " maxTime=" + maxTime);
        DateRange dateRange = 
            null;
//!this should work but doesn't!
            //new DateRange(
            //    new Date(Math.round(minTime * 1000.0)), 
            //    new Date(Math.round(maxTime * 1000.0)));

        //make latLonRange
        //lon always +-180 ???
        //http://www.unidata.ucar.edu/software/netcdf-java/v2.2.22/javadoc/ucar/unidata/geoloc/LatLonPoint.html
        if (Double.isNaN(minLon)) minLon = -180;
        if (Double.isNaN(maxLon)) maxLon = 360;  // !
        if (Double.isNaN(minLat)) minLat = -90;
        if (Double.isNaN(maxLat)) maxLat = 90;

        LatLonRect latLonRect = 
//            null;
            new LatLonRect(
                new LatLonPointImpl(minLat, minLon), 
                new LatLonPointImpl(maxLat, maxLon)); 

        //make the starter table
        int sv = sourceDataNames == null? -1 : sourceDataNames.indexOf("longitude");
        PrimitiveArray lonPA = sv >= 0?
            PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[sv]), 128, false) :
            new DoubleArray(128, false);
        sv = sourceDataNames == null? -1 : sourceDataNames.indexOf("latitude");
        PrimitiveArray latPA = sv >= 0?
            PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[sv]), 128, false) :
            new DoubleArray(128, false);
        sv = sourceDataNames == null? -1 : sourceDataNames.indexOf("altitude");
        PrimitiveArray altPA = sv >= 0?
            PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[sv]), 128, false) :
            new DoubleArray(128, false);
        sv = sourceDataNames == null? -1 : sourceDataNames.indexOf("time");
        PrimitiveArray timePA = sv >= 0?
            PrimitiveArray.factory(PrimitiveArray.elementStringToClass(sourceDataTypes[sv]), 128, false) :
            new DoubleArray(128, false);
        Table table = new Table();
        table.addColumn("time", timePA);
        table.addColumn("longitude", lonPA);
        table.addColumn("latitude",  latPA);
        table.addColumn("altitude",  altPA);

        //make the FeatureDatasetFile
        Formatter errlog = new Formatter();
        FeatureDataset dataset = FeatureDatasetFactoryManager.open(null, //wantFeatureType,
            fileDir + fileName, null, //CancelTask
            errlog);
        if (verbose) {
            String err = errlog.toString();
            if (err.length() > 0)
                String2.log("errlog for " + fileDir + fileName + ":\n" + 
                    err);
        }
        if (dataset == null) 
            throw new SimpleException("FeatureDatasetFactoryManager can't make a dataset from " +
                fileDir + fileName);
        NcHelper.getGlobalAttributes(dataset.getGlobalAttributes(), table.globalAttributes());
        String2.log("implementationName=" + dataset.getImplementationName());
        String2.log("featureType isPointFeatureType=" + dataset.getFeatureType().isPointFeatureType());

        FeatureDatasetPoint fdp = (FeatureDatasetPoint)dataset;
        List<FeatureCollection> fcl = fdp.getPointFeatureCollectionList();
        for (FeatureCollection fc : fcl) {

            //ProfileFeatureCollection   
            if (fc instanceof ProfileFeatureCollection) {
                if (verbose) String2.log("fc is instanceof ProfileFeatureCollection");
                ProfileFeatureCollection pfc = (ProfileFeatureCollection)fc;

                //!!! flatten doesn't work right -- 
                //  response is sorted wrong 
                //  data only appears on some rows (others have NaNs)
//non-null latLonRect or dateRange fails
latLonRect = null;
dateRange = null;
                PointFeatureCollection pfc2 = pfc.flatten(latLonRect, dateRange); 
                addDataFromPointFeatureCollection(pfc2, table,    
                    latLonRect, dateRange,
                    minLon,  maxLon,
                    minLat,  maxLat,
                    minAlt,  maxAlt,
                    minTime, maxTime);
                
                
                /* if subset(latLonRect), the new pfc refuses resetIteration() below
                    pfc = pfc.subset(latLonRect);
                if (pfc == null) {
                    String2.log("ProfileFeatureCollection has no stations in latLonRect.");
                    continue;
                }*/
                /* This returns no data. 
                //pfc.resetIteration(); //required?
                while (pfc.hasNext()) {
                    addDataFromPointFeatureCollection(pfc.next(), table,    
                        latLonRect, dateRange,
                        minLon,  maxLon,
                        minLat,  maxLat,
                        minAlt,  maxAlt,
                        minTime, maxTime);
                }
                */
/*
            //StationTimeSeriesCollection       
            } else if (fc instanceof StationTimeSeriesFeatureCollection) {
                if (verbose) String2.log("fc is instanceof StationTimeSeriesFeatureCollection");
                StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection)fc;
                //sfc = sfc.subset(latLonRect);
                
                //flatten???
//trouble with data lon not +-180?
latLonRect = null;
                PointFeatureCollection pfc2 = sfc.flatten(latLonRect, dateRange);
                addDataFromPointFeatureCollection(pfc2, table,    
                    latLonRect, dateRange,
                    minLon,  maxLon,
                    minLat,  maxLat,
                    minAlt,  maxAlt,
                    minTime, maxTime);

            //TrajectoryFeatureCollection   
            } else if (fc instanceof TrajectoryFeatureCollection) {
                if (verbose) String2.log("fc is instanceof TrajectoryFeatureCollection");
                TrajectoryFeatureCollection tfc = (TrajectoryFeatureCollection)fc;
                //!!! NO WAY TO SUBSET tfc E.G., apply latLonRect or dateRange

                //!!! flatten doesn't work right
//trouble with data lon not +-180?
latLonRect = null;
                PointFeatureCollection pfc2 = tfc.flatten(latLonRect, dateRange);
                addDataFromPointFeatureCollection(pfc2, table,    
                    latLonRect, dateRange,
                    minLon,  maxLon,
                    minLat,  maxLat,
                    minAlt,  maxAlt,
                    minTime, maxTime);

            //TimeSeriesProfileFeatureCollection
            } else if (fc instanceof TimeSeriesProfileFeatureCollection) {
                if (verbose) String2.log("fc is instanceof StationProfileFeatureCollection");
                StationProfileFeatureCollection spfc = (StationProfileFeatureCollection)fc;

                //apply latLonRect
                List<Station> stations = spfc.getStations(latLonRect); 
                if (stations == null) {
                    String2.log("StationProfileFeatureCollection has no stations in latLonRect.");
                    continue;
                }
                for (Station station : stations) {
                    StationProfileFeature spf = spfc.getStationProfileFeature(station);
                    // spf = spf.subset(dateRange); This should work but doesn't -- returns null.
                    //spf.resetIteration(); //required?
                    while (spf.hasNext()) { //next profile
                        addDataFromPointFeatureCollection(spf.next(), table,    
                            latLonRect, dateRange,
                            minLon,  maxLon,
                            minLat,  maxLat,
                            minAlt,  maxAlt,
                            minTime, maxTime);
                    }
                }

            //SectionFeatureCollection   aka TrajectoryProfile
            } else if (fc instanceof SectionFeatureCollection) {
                if (verbose) String2.log("fc is instanceof SectionFeatureCollection");
                SectionFeatureCollection sfc = (SectionFeatureCollection)fc;

                //flatten supported?
                PointFeatureCollection pfc = sfc.flatten(null, null);
                addDataFromPointFeatureCollection(pfc, table,    
                    latLonRect, dateRange,
                    minLon,  maxLon,
                    minLat,  maxLat,
                    minAlt,  maxAlt,
                    minTime, maxTime);

*//*                //NO WAY TO SUBSET, E.G., apply latLonRect or dateRange
                //sfc.resetIteration();  //required?
                while (sfc.hasNext()) { //next trajectory
                    SectionFeature sf = sfc.next();
                    //NO WAY TO SUBSET, E.G., apply latLonRect or dateRange
                    //sf.resetIteration(); //required?
                    while (sf.hasNext()) { //next profile
                        addDataFromPointFeatureCollection(sf.next(), table,    
                            latLonRect, dateRange,
                            minLon,  maxLon,
                            minLat,  maxLat,
                            minAlt,  maxAlt,
                            minTime, maxTime);
                    }
                }
                */
/*
            //PointFeatureCollection
            } else if (fc instanceof PointFeatureCollection) {
                if (verbose) String2.log("fc is instanceof PointFeatureCollection");

                addDataFromPointFeatureCollection((PointFeatureCollection)fc, table,  
                    latLonRect, dateRange,
                    minLon,  maxLon,
                    minLat,  maxLat,
                    minAlt,  maxAlt,
                    minTime, maxTime);

            //NestedPointFeatureCollection
            } else if (fc instanceof NestedPointFeatureCollection) {
                if (verbose) String2.log("fc is instanceof NestedPointFeatureCollection");
                NestedPointFeatureCollection npfc = (NestedPointFeatureCollection)fc;

                //1st approach, didn't work. StationProfileFeatureCollection doesn't implement this method.
                ////flatten it   
                //PointFeatureCollection pfc = npfc.flatten(latLonRect, dateRange); 
                //if (pfc == null) //perhaps no data in the dateRange
                //    return table;   

                //2nd approach
                if (npfc.isMultipleNested()) {
                    NestedPointFeatureCollectionIterator npfci = 
                        npfc.getNestedPointFeatureCollectionIterator(-1); //default size
                    while (npfci.hasNext()) { 
                        NestedPointFeatureCollection npfc2 = npfci.next();
                        if (npfc2.isMultipleNested()) {
                            throw new SimpleException("npfc2.isMultipleNexted isn't supported yet.");
                        } else {
                            PointFeatureCollectionIterator pfci = 
                                npfc.getPointFeatureCollectionIterator(-1); //default size
                            while (pfci.hasNext()) { 
                                addDataFromPointFeatureCollection(pfci.next(), table,  
                                    latLonRect, dateRange,
                                    minLon,  maxLon,
                                    minLat,  maxLat,
                                    minAlt,  maxAlt,
                                    minTime, maxTime);
                            }
                        }
                    }

                } else {
                    PointFeatureCollectionIterator pfci = 
                        npfc.getPointFeatureCollectionIterator(-1); //default size
                    while (pfci.hasNext()) { 
                        addDataFromPointFeatureCollection(pfci.next(), table,  
                            latLonRect, dateRange,
                            minLon,  maxLon,
                            minLat,  maxLat,
                            minAlt,  maxAlt,
                            minTime, maxTime);
                    }                    
                }
            } else {
                throw new SimpleException("fc isn't instanceof PointFeatureCollection or NestedPointFeatureCollection!");
            }
        }

        String2.log("EDDTableFromNcCFFiles.getSourceDataFromFile table.nRows=" + table.nRows());
String2.log(table.dataToCsvString(10));
        return table;
*/      return new Table();
    }

    /** 
     * This is used by readData to read the data from a PointFeatureCollection
     * (a subset of the requested data).
     * 
     * @param table
     * @param minLon and all other min,max values will be finite values
     */
    private static void addDataFromPointFeatureCollection(PointFeatureCollection pfc, Table table,
        LatLonRect latLonRect, DateRange dateRange,
        double minLon, double maxLon,
        double minLat, double maxLat,
        double minAlt, double maxAlt,
        double minTime, double maxTime) 
        throws Throwable {

        if (pfc == null) {
            String2.log("addDataFromPointFeatureCollection: initial pfc=null.");
            return;
        }

        /* //subset by latLonRect and dateRange
        //??? or is this inefficient!?
        pfc = pfc.subset(latLonRect, dateRange);
        if (pfc == null) {
            String2.log("addDataFromPointFeatureCollection: pfc has no PointFeatures in latLonRect and dateRange.");
            return;
        }*/

        //find lon,lat,alt,time PA's  
        PrimitiveArray timePA = table.findColumn("time");
        PrimitiveArray lonPA  = table.findColumn("longitude");
        PrimitiveArray latPA  = table.findColumn("latitude");
        PrimitiveArray altPA  = table.findColumn("altitude");

        //store data in table
        //pfc.resetIteration(); //required?
        while (pfc.hasNext()) {
            PointFeature pf = pfc.next();

            //get lon,lat,alt,time
            EarthLocation earthLoc = pf.getLocation();
            double tLon = earthLoc.getLongitude();
            double tLat = earthLoc.getLatitude();
            double tAlt = earthLoc.getAltitude();
            double tTime = pf.getObservationTimeAsDate().getTime() / 1000.0; 

            //reject based on lon,lat,alt,time?  (redundant since tested above?)
            if (tLon < minLon) return;
            if (tLon > maxLon) return;
            if (tLat < minLat) return;
            if (tLat > maxLat) return;
            if (!Double.isNaN(tAlt)) {
                if (tAlt < minAlt) return;
                if (tAlt > maxAlt) return;
            }
            if (tTime < minTime) return;
            if (tTime > maxTime) return;

            //store data in table   (a member holds a column's data)
            int oldNRows = table.nRows();
            StructureData structure = pf.getData();
            List<StructureMembers.Member> members = structure.getMembers();
            for (StructureMembers.Member member : members) {
                Array array = member.getDataArray();
                if (array == null) { //altitude is often null
                    String2.log("! member=" + member.getName() + " getDataArray() returned null!");
                    continue;
                }
                PrimitiveArray pa = NcHelper.getPrimitiveArray(array);
                int col = table.findColumnNumber(member.getName());
                if (col < 0)
                    //make a column with mv's for previous data
                    col = table.addColumn(member.getName(), 
                        PrimitiveArray.factory(pa.elementClass(), oldNRows, "")); 
//String2.log(member.getName() + " size=" + pa.size());
                table.getColumn(col).append(pa);
            }

            //make columns same size 
            //this affects lon,lat,alt,time (will be MVs)
            table.makeColumnsSameSize();

            //set lon,lat,alt,time  (removeRange then addNDoubles is very efficient)
            int newNRows = table.nRows();
            int addNRows = newNRows - oldNRows;
            lonPA.removeRange( oldNRows, newNRows);
            latPA.removeRange( oldNRows, newNRows);
            altPA.removeRange( oldNRows, newNRows);
            timePA.removeRange(oldNRows, newNRows);
            lonPA.addNDoubles( addNRows, tLon);
            latPA.addNDoubles( addNRows, tLat);
            altPA.addNDoubles( addNRows, tAlt);
            timePA.addNDoubles(addNRows, tTime); 
        }
    }

//!!!need to do get directory (handle files or thredds directory)

    /** 
     * This generates a ready-to-use datasets.xml entry for an EDDTableFromNcFiles.
     * The XML can then be edited by hand and added to the datasets.xml file.
     *
     * <p>This can't be made into a web service because it would allow any user
     * to looks at (possibly) private .nc files on the server.
     *
     * @param tFileDir the starting (parent) directory for searching for files
     * @param tFileNameRegex  the regex that each filename (no directory info) must match 
     *    (e.g., ".*\\.nc")  (usually only 1 backslash; 2 here since it is Java code). 
     *    If null or "", it is generated to catch the same extension as the sampleFileName
     *    (usually ".*\\.nc").
     * @param sampleFileName the full file name of one of the files in the collection
     * @param useDimensionsCSV If null or "", this finds the group of variables sharing the
     *    highest number of dimensions. Otherwise, it find the variables using
     *    these dimensions (plus related char variables).
     * @param tReloadEveryNMinutes  e.g., 10080 for weekly
     * @param tPreExtractRegex       part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tPostExtractRegex      part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tExtractRegex          part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tColumnNameForExtract  part of info for extracting e.g., stationName from file name. Set to "" if not needed.
     * @param tSortedColumnSourceName   use "" if not known or not needed. 
     * @param tSortFilesBySourceNames   This is useful, because it ultimately determines default results order.
     * @param tInfoUrl       or "" if in externalAddGlobalAttributes or if not available
     * @param tInstitution   or "" if in externalAddGlobalAttributes or if not available
     * @param tSummary       or "" if in externalAddGlobalAttributes or if not available
     * @param tTitle         or "" if in externalAddGlobalAttributes or if not available
     * @param externalAddGlobalAttributes  These attributes are given priority.  Use null in none available.
     * @throws Throwable if trouble
     */
    public static String generateDatasetsXml(
        String tFileDir, String tFileNameRegex, String sampleFileName, 
        String useDimensionsCSV,
        int tReloadEveryNMinutes,
        String tPreExtractRegex, String tPostExtractRegex, String tExtractRegex,
        String tColumnNameForExtract, String tSortedColumnSourceName,
        String tSortFilesBySourceNames, 
        String tInfoUrl, String tInstitution, String tSummary, String tTitle,
        Attributes externalAddGlobalAttributes) throws Throwable {
/*
        String2.log("EDDTableFromNcFiles.generateDatasetsXml" +
            "\n  sampleFileName=" + sampleFileName);
        tFileDir = File2.addSlash(tFileDir); //ensure it has trailing slash
        String[] useDimensions = StringArray.arrayFromCSV(useDimensionsCSV);

        //*** basically, make a table to hold the sourceAttributes 
        //and a parallel table to hold the addAttributes
        Table dataSourceTable = new Table();
        Table dataAddTable = new Table();
        NetcdfFile ncFile = NcHelper.openFile(sampleFileName);
        int maxDim = 0;
        try {
            List variables = ncFile.getVariables();
            for (int dv = 0; dv < variables.size(); dv++) {
                Variable var = (Variable)variables.get(dv);
                Class elementClass = NcHelper.getElementClass(var.getDataType());
                if (elementClass == boolean.class) elementClass = byte.class;  //NcHelper.getArray converts booleans to bytes
                else if (elementClass == char.class) elementClass = String.class;
                PrimitiveArray pa = PrimitiveArray.factory(elementClass, 1, false);
                int tDim = var.getRank() - (pa instanceof StringArray? 1 : 0);

                if (useDimensions.length > 0) {
                    //use useDimensions
                    //Do the vars dimensions match useDimensions? Reject var if not.
                    if (tDim != useDimensions.length)
                        continue;
                    for (int d = 0; d < tDim; d++)
                        if (!var.getDimension(d).getName().equals(useDimensions[d]))
                            continue;

                } else {
                    //don't use useDimensions
                    if (tDim < maxDim)
                        continue;

                    //Do the vars dimensions match the currently in-use dimensions? Reject var if not.
                    //If maxDim == 0 or tDim>maxDim, it's better! Let if fall through.
                    if (maxDim > 0 && tDim == maxDim) { 
                        for (int d = 0; d < tDim; d++)
                            if (!var.getDimension(d).getName().equals(
                                dataSourceTable.getColumnName(d)))
                                continue;
                    }
                }

                //first var using the dimensions?
                if (tDim > maxDim) {
                    //reset and store this var's dimensions
                    dataSourceTable.clear();
                    dataAddTable.clear();
                    maxDim = tDim;
                    for (int av = 0; av < maxDim; av++) {
                        Dimension dim = var.getDimension(av);
                        Variable dimVar = ncFile.findVariable(dim.getName());
                        //if it is just a counter like 'row', skip it
                        if (dimVar == null)
                            continue;
                        Attributes sourceAtts = new Attributes();
                        PrimitiveArray dimPa = null;
                        if (dimVar == null) {
                            dimPa = dim.getLength() < 125?   new ByteArray() : 
                                 dim.getLength() < 30000? new ShortArray() : 
                                 new IntArray();
                        } else {
                            NcHelper.getVariableAttributes(dimVar, sourceAtts);
                            Class dimElementType = NcHelper.getElementClass(dimVar.getDataType());
                            if (dimElementType == boolean.class) dimElementType = byte.class;  //NcHelper.getArray converts booleans to bytes
                            else if (dimElementType == char.class) dimElementType = String.class;
                            dimPa = PrimitiveArray.factory(dimElementType, 1, false);
                        }
                        dataSourceTable.addColumn(av, dimVar.getName(), dimPa, 
                            sourceAtts);
                        dataAddTable.addColumn(av, dimVar.getName(), dimPa, 
                            makeReadyToUseAddVariableAttributesForDatasetsXml(
                                sourceAtts, dimVar.getName(), true)); //addColorBarMinMax
                    }
                }

                if (dataAddTable.findColumnNumber(var.getName()) >= 0) 
                    continue; //e.g., time(time) is already in the dataAddTable

                //load this var
                Attributes sourceAtts = new Attributes();
                NcHelper.getVariableAttributes(var, sourceAtts);
                dataSourceTable.addColumn(dataSourceTable.nColumns(), var.getName(), pa, 
                    sourceAtts);
                dataAddTable.addColumn(   dataAddTable.nColumns(),    var.getName(), pa,
                    makeReadyToUseAddVariableAttributesForDatasetsXml(
                        sourceAtts, var.getName(), true)); //addColorBarMinMax

                //if a variable has timeUnits, files are likely sorted by time
                //and no harm if files aren't sorted that way
                if (tSortedColumnSourceName.length() == 0 && 
                    EDVTimeStamp.hasTimeUnits(sourceAtts, null))
                    tSortedColumnSourceName = var.getName();
            }

            //load the global metadata
            NcHelper.getGlobalAttributes(ncFile, dataSourceTable.globalAttributes());

            //I do care if this throws Throwable
            ncFile.close(); 

        } catch (Throwable t) {
            //make sure ncFile is explicitly closed
            try {
                ncFile.close(); 
            } catch (Throwable t2) {
                //don't care
            }

            throw t;
        }


        //globalAttributes
        if (externalAddGlobalAttributes == null)
            externalAddGlobalAttributes = new Attributes();
        if (tInfoUrl     != null && tInfoUrl.length()     > 0) externalAddGlobalAttributes.add("infoUrl",     tInfoUrl);
        if (tInstitution != null && tInstitution.length() > 0) externalAddGlobalAttributes.add("institution", tInstitution);
        if (tSummary     != null && tSummary.length()     > 0) externalAddGlobalAttributes.add("summary",     tSummary);
        if (tTitle       != null && tTitle.length()       > 0) externalAddGlobalAttributes.add("title",       tTitle);
        externalAddGlobalAttributes.setIfNotAlreadySet("sourceUrl", "(local files)");
        //externalAddGlobalAttributes.setIfNotAlreadySet("subsetVariables", "???");
        dataAddTable.globalAttributes().set(
            makeReadyToUseAddGlobalAttributesForDatasetsXml(
                dataSourceTable.globalAttributes(), 
                //another cdm_data_type could be better; this is ok
                probablyHasLonLatTime(dataAddTable)? "Point" : "Other",
                tFileDir, externalAddGlobalAttributes));

        //add the columnNameForExtract variable
        if (tColumnNameForExtract.length() > 0) {
            Attributes atts = new Attributes();
            atts.add("ioos_category", "Identifier");
            atts.add("long_name", EDV.suggestLongName(null, tColumnNameForExtract, null));
            //no units or standard_name
            dataSourceTable.addColumn(0, tColumnNameForExtract, new StringArray(), new Attributes());
            dataAddTable.addColumn(   0, tColumnNameForExtract, new StringArray(), atts);
        }

        //write the information
        StringBuilder sb = new StringBuilder();
        String suggestedRegex = (tFileNameRegex == null || tFileNameRegex.length() == 0)? 
            ".*\\" + File2.getExtension(sampleFileName) :
            tFileNameRegex;
        if (tSortFilesBySourceNames.length() == 0)
            tSortFilesBySourceNames = tColumnNameForExtract + 
                (tSortedColumnSourceName.length() == 0? "" : " " + tSortedColumnSourceName);
        sb.append(
            directionsForGenerateDatasetsXml() +
            "-->\n\n" +
            "<dataset type=\"EDDTableFromNcFiles\" datasetID=\"" + 
                suggestDatasetID(tFileDir + suggestedRegex) +  //dirs can't be made public
                "\" active=\"true\">\n" +
            "    <reloadEveryNMinutes>" + tReloadEveryNMinutes + "</reloadEveryNMinutes>\n" +  
            "    <fileDir>" + tFileDir + "</fileDir>\n" +
            "    <recursive>true</recursive>\n" +
            "    <fileNameRegex>" + suggestedRegex + "</fileNameRegex>\n" +
            "    <metadataFrom>last</metadataFrom>\n" +
            "    <preExtractRegex>" + tPreExtractRegex + "</preExtractRegex>\n" +
            "    <postExtractRegex>" + tPostExtractRegex + "</postExtractRegex>\n" +
            "    <extractRegex>" + tExtractRegex + "</extractRegex>\n" +
            "    <columnNameForExtract>" + tColumnNameForExtract + "</columnNameForExtract>\n" +
            "    <sortedColumnSourceName>" + tSortedColumnSourceName + "</sortedColumnSourceName>\n" +
            "    <sortFilesBySourceNames>" + tSortFilesBySourceNames + "</sortFilesBySourceNames>\n" +
            "    <altitudeMetersPerSourceUnit>1</altitudeMetersPerSourceUnit>\n");
        sb.append(writeAttsForDatasetsXml(false, dataSourceTable.globalAttributes(), "    "));
        sb.append(cdmSuggestion());
        sb.append(writeAttsForDatasetsXml(true,     dataAddTable.globalAttributes(), "    "));

        //last 3 params: includeDataType, tryToCatchLLAT, questionDestinationName
        sb.append(writeVariablesForDatasetsXml(dataSourceTable, dataAddTable, 
            "dataVariable", true, true, false));
        sb.append(
            "</dataset>\n" +
            "\n");

        String2.log("\n\n*** generateDatasetsXml finished successfully.\n\n");
        return sb.toString();
  */return "(commented out)";      
    }


    /**
     * testGenerateDatasetsXml
     */
    public static void testGenerateDatasetsXml() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                1440,
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", "", null);

            //GenerateDatasetsXml
            GenerateDatasetsXml.doIt(new String[]{"-verbose", 
                "EDDTableFromNcFiles",
                "C:/u00/data/points/ndbcMet", "",
                "C:/u00/data/points/ndbcMet/NDBC_41004_met.nc",
                "",
                "1440",
                "^.{5}", ".{7}$", ".*", "stationID", //just for test purposes; station is already a column in the file
                "TIME", "stationID TIME", 
                "", "", "", ""},
                false); //doIt loop?
            String gdxResults = String2.getClipboardString();
            Test.ensureEqual(gdxResults, results, "Unexpected results from GenerateDatasetsXml.doIt.");

String expected = 
directionsForGenerateDatasetsXml() +
"-->\n" +
"zztop\n";

            Test.ensureEqual(results, expected, "results=\n" + results);
            //Test.ensureEqual(results.substring(0, Math.min(results.length(), expected.length())), 
            //    expected, "");

            //ensure it is ready-to-use by making a dataset from it
            //with one small change to addAttributes:
            results = String2.replaceAll(results, 
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n",
                "        <att name=\"infoUrl\">http://coastwatch.pfeg.noaa.gov</att>\n" +
                "        <att name=\"cdm_data_type\">Other</att>\n");
            String2.log(results);

            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "ndbcMet_5df7_b363_ad99", "");
            Test.ensureEqual(edd.title(), "NOAA NDBC Standard Meteorological", "");
            Test.ensureEqual(String2.toCSVString(edd.dataVariableDestinationNames()), 
                "stationID, time, DEPTH, latitude, longitude, WD, WSPD, GST, WVHT, " +
                "DPD, APD, MWD, BAR, ATMP, WTMP, DEWP, VIS, PTDY, TIDE, WSPU, WSPV", 
                "");

        } catch (Throwable t) {
            String2.getStringFromSystemIn(MustBe.throwableToString(t) + 
                "\nError using generateDatasetsXml." + 
                "\nPress ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * testGenerateDatasetsXml2
     */
    public static void testGenerateDatasetsXml2() throws Throwable {
        testVerboseOn();

        try {
            String results = generateDatasetsXml(
        "f:/data/ngdcJasonSwath/", ".*\\.nc", 
        "f:/data/ngdcJasonSwath/JA2_OPN_2PcS088_239_20101201_005323_20101201_025123.nc", 
        "time",  //not "time, meas_ind"
        10080, 
        "", "", "", 
        "", "", 
        "time", 
        "", "", "", "", new Attributes());

            EDD edd = oneFromXmlFragment(results);
            Test.ensureEqual(edd.datasetID(), "ngdcJasonSwath_2743_941d_6d6c", "");
            Test.ensureEqual(edd.title(), "OGDR - Standard dataset", "");
            Test.ensureEqual(String2.toCSVString(edd.dataVariableDestinationNames()), 
"zztop", 
                "");

        } catch (Throwable t) {
            String2.getStringFromSystemIn(
                MustBe.throwableToString(t) + 
                "\nUnexpected error. Press ^C to stop or Enter to continue..."); 
        }

    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test1(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test1() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        String id = "erdCinpKfmSFNH";
        if (deleteCachedDatasetInfo) {
            File2.delete(datasetInfoDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + BADFILE_TABLE_FILENAME);
        }
        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 


        //.csv    for one lat,lon,time
        userDapQuery = "" +
            "&longitude=-119.05&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1Station", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude, latitude, altitude, time, id, common_name, species_name, size\n" +
"degrees_east, degrees_north, m, UTC, , , , mm\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 57\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 41\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 55\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 60\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Purple sea urchin, Strongylocentrotus purpuratus, 15\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Purple sea urchin, Strongylocentrotus purpuratus, 23\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Purple sea urchin, Strongylocentrotus purpuratus, 19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


        //.csv    for one lat,lon,time      via lon > <
        userDapQuery = "" +
            "&longitude>-119.06&longitude<=-119.04&latitude=33.46666666666&time=2005-07-01T00:00:00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_1StationGTLT", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"longitude, latitude, altitude, time, id, common_name, species_name, size\n" +
"degrees_east, degrees_north, m, UTC, , , , mm\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 57\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 41\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 55\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Bat star, Asterina miniata, 60\n";
        Test.ensureEqual(results.substring(0, expected.length()), expected, "\nresults=\n" + results);
        expected = //last 3 lines
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Purple sea urchin, Strongylocentrotus purpuratus, 15\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Purple sea urchin, Strongylocentrotus purpuratus, 23\n" +
"-119.05, 33.4666666666667, -14.0, 2005-07-01T00:00:00Z, Santa Barbara (Webster's Arch), Purple sea urchin, Strongylocentrotus purpuratus, 19\n";
        Test.ensureEqual(results.substring(results.length() - expected.length()), expected, "\nresults=\n" + results);


    }

    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test2(boolean deleteCachedDatasetInfo) throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.test2() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String error = "";
        EDV edv;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        //the test files were made with makeTestFiles();
        String id = "testNc2D";       
        if (deleteCachedDatasetInfo) {
            File2.delete(datasetInfoDir(id) + DIR_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + FILE_TABLE_FILENAME);
            File2.delete(datasetInfoDir(id) + BADFILE_TABLE_FILENAME);
        }

        //touch a good and a bad file, so they are checked again
        File2.touch("c:/u00/data/points/nc2d/NDBC_32012_met.nc");
        File2.touch("c:/u00/data/points/nc2d/NDBC_4D_met.nc");

        EDDTable eddTable = (EDDTable)oneFromDatasetXml(id); 

        //*** test make data files
        String2.log("\n****************** EDDTableFromNcFiles.test2 make DATA FILES\n");       

        //.csv
        //from NdbcMetStation.test31201
        //YYYY MM DD hh mm  WD WSPD  GST  WVHT   DPD   APD MWD  BARO   ATMP  WTMP  DEWP  VIS  TIDE
        //2005 04 19 00 00 999 99.0 99.0  1.40  9.00 99.00 999 9999.0 999.0  24.4 999.0 99.0 99.00 first available
        //double seconds = Calendar2.isoStringToEpochSeconds("2005-04-19T00");
        //int row = table.getColumn(timeIndex).indexOf("" + seconds, 0);
        //Test.ensureEqual(table.getStringData(idIndex, row), "31201", "");
        //Test.ensureEqual(table.getFloatData(latIndex, row), -27.7f, "");
        //Test.ensureEqual(table.getFloatData(lonIndex, row), -48.13f, "");

        userDapQuery = "latitude,time,station,wvht,dpd,wtmp,dewp" +
            "&latitude=-27.7&time=2005-04-19T00";
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_Data1", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"latitude, time, station, wvht, dpd, wtmp, dewp\n" +
"degrees_north, UTC, , m, s, degree_C, degree_C\n" +
"-27.7, 2005-04-19T00:00:00Z, 31201, 1.4, 9.0, 24.4, NaN\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);

    }


    /**
     * This test &amp;distinct().
     *
     * @throws Throwable if trouble
     */
    public static void testDistinct() throws Throwable {
        String2.log("\n****************** EDDTableFromNcFiles.testDistinct() *****************\n");
        testVerboseOn();
        String name, tName, results, tResults, expected, userDapQuery, tQuery;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        EDDTable eddTable = (EDDTable)oneFromDatasetXml("cwwcNDBCMet"); 

        //time constraints force erddap to get actual data, (not just station variables)
        //  and order of variables says to sort by lon first
        userDapQuery = "longitude,latitude,station&station=~\"5.*\"&time>=2008-03-11&time<2008-03-12&distinct()"; 
        tName = eddTable.makeNewFileForDapQuery(null, null, userDapQuery, EDStatic.fullTestCacheDirectory, 
            eddTable.className() + "_distincts", ".csv"); 
        results = new String((new ByteArray(EDStatic.fullTestCacheDirectory + tName)).toArray());
        //String2.log(results);
        expected = 
"zztop\n";
        Test.ensureEqual(results, expected, "\nresults=\n" + results);


    }

    /** Bob uses this sometimes to remake the testReadData test files. */
    public static void bobMakeTestReadDataFiles() throws Throwable {
        String2.log("\n*** bobMakeTestReadDataFiles()");
        String dir = "f:/data/nccf/";
        EDD edd;
        String fileName;

        edd = EDD.oneFromDatasetXml("nwioosCoral");
        fileName = edd.makeNewFileForDapQuery(null, null,
            "&time=1980-01-01T00:00:00Z",
            dir, "Point", ".ncCF");

        edd = EDD.oneFromDatasetXml("pmelWOD5np");
        fileName = edd.makeNewFileForDapQuery(null, null,
            "id,longitude,latitude,time,altitude,temperature,temperature_qc&time>=2005-02-07T00:00:00Z&time<=2005-02-07T12:48:00Z",
            dir, "Profile", ".ncCF");

        edd = EDD.oneFromDatasetXml("cwwcNDBCMet");
        fileName = edd.makeNewFileForDapQuery(null, null,
            "&latitude>=63.3&time>=2010-01-01&time<=2010-01-01T02",
            dir, "Station", ".ncCF");

        edd = EDD.oneFromDatasetXml("erdGlobecMoc1");
        fileName = edd.makeNewFileForDapQuery(null, null,
            "&d_n_flag=%22DAY%22&genus_species=%22AMPHIPODA%22&water_depth=90",
            dir, "Trajectory", ".ncCF");

        edd = EDD.oneFromDatasetXml("erdNewportCtd");
        fileName = edd.makeNewFileForDapQuery(null, null,
            "&plain_station=%22NH05%22",
            dir, "StationProfile", ".ncCF");

        edd = EDD.oneFromDatasetXml("erdGlobecBottle");
        fileName = edd.makeNewFileForDapQuery(null, null,
            "&cruise_id=%22nh0207%22&latitude=44.0",
            dir, "TrajectoryProfile", ".ncCF");
       
        String2.log("\n*** bobMakeTestReadDataFiles() finished successfully");
    }

    /** Test getSourceData  
     *
     * @param whichTest   -1 for all
     */
    public static void testReadData(int whichTest) throws Throwable {       
        String2.log("\n*** EDDTableFromNcCFFiles.testReadData(" + whichTest + ")");
        Table table; 
        String results, expected;
        String today = Calendar2.getCurrentISODateTimeStringLocal().substring(0, 10);

        if (whichTest < 0 || whichTest == 0) {
            table = readData(
                "F:/data/nccf/", "epa+seamap_04-08_WD23.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = "zztop";
            //Test.ensureEqual(results, expected, 
            //    "results0=\n" + String2.replaceAll(results, "\t", "\\t"));
        }

        if (whichTest < 0 || whichTest == 1) {
            table = readData(
                "F:/data/nccf/", "Point.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = 
"zztop";
            //Test.ensureEqual(results, expected, 
            //    "results1=\n" + String2.replaceAll(results, "\t", "\\t"));
        }

        if (whichTest < 0 || whichTest == 2) {
            table = readData(
                "F:/data/nccf/", "Profile.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = 
"zztop";
            //Test.ensureEqual(results, expected, 
            //    "results2=\n" + String2.replaceAll(results, "\t", "\\t"));
        }

        if (whichTest < 0 || whichTest == 3) {
            table = readData(
                "F:/data/nccf/", "Station.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = "zztop";
            //Test.ensureEqual(results, expected, 
            //    "results3=\n" + String2.replaceAll(results, "\t", "\\t"));
        }

        if (whichTest < 0 || whichTest == 4) {
            table = readData(
                "F:/data/nccf/", "Trajectory.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = "zztop";
            //Test.ensureEqual(results, expected, 
            //    "results4=\n" + String2.replaceAll(results, "\t", "\\t"));
        }

        if (whichTest < 0 || whichTest == 5) {
            table = readData(
                "F:/data/nccf/", "StationProfile.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = "zztop";
            //Test.ensureEqual(results, expected, 
            //    "results5=\n" + String2.replaceAll(results, "\t", "\\t"));
        }

        if (whichTest < 0 || whichTest == 6) {
            table = readData(
                "F:/data/nccf/", "TrajectoryProfile.nc", 
                null, null, //sourceDataNames, sourceDataTypes[],
                Double.NaN, Double.NaN, //min/MaxLon 
                Double.NaN, Double.NaN, //min/MaxLat 
                Double.NaN, Double.NaN, //min/MaxAlt 
                Double.NaN, Double.NaN, //min/MaxTime 
                true, true); //getMetadata, mustGetData
            results = table.toCsvString(10);
            expected = "zztop";
            //Test.ensureEqual(results, expected, 
            //    "results6=\n" + String2.replaceAll(results, "\t", "\\t"));
        }
    }

    
    /**
     * This tests the methods in this class.
     *
     * @throws Throwable if trouble
     */
    public static void test() throws Throwable {
        testReadData(-1);
        //test1(false); //deleteCachedDatasetInfo
        //test2(true); 
        //testGenerateDatasetsXml();
        //testGenerateDatasetsXml2();

        //not usually run
    }
}

