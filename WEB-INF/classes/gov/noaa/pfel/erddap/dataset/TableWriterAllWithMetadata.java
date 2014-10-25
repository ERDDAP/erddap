/* 
 * TableWriterAllWithMetadata Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.DoubleArray;
import com.cohort.array.FloatArray;
import com.cohort.array.IntArray;
import com.cohort.array.PrimitiveArray;
import com.cohort.array.ShortArray;
import com.cohort.array.StringArray;
import com.cohort.util.Calendar2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import com.cohort.util.XML;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * TableWriterAllWithMetadata is a subclass of TableWriterAll that 
 * cleans up the final metadata (e.g., actual_range).
 * This variant of TableWriter is useful when you need all of the results 
 * (including metadata) before writing the beginning of the file (e.g., .nc or .mat).
 *
 * <p>This is different from most TableWriters in that finish() doesn't 
 * write the data anywhere (to an outputStream or to another tableWriter), 
 * it just makes all of the data available.
 *
 * <p>This is also the only way to get correct actual_range
 * metadata for a subset of data. (It is set in finish().)
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2007-08-23
 */
public class TableWriterAllWithMetadata extends TableWriterAll {

    //TableWriter has String[] columnNames; and columnName()
    //TableWriter has Class[] columnTypes; and columnType()
    //TableWriter has Attributes globalAttributes;
    //TableWriter has Attributes[] columnAttributes;
    protected int[] columnMaxStringLength;
    protected double[] columnMinValue;  //NaN if no valid values
    protected double[] columnMaxValue;

    /**
     * The constructor.
     *
     * @param tDir a private cache directory for storing the intermediate files,
     *    usually cacheDirectory(datasetID)
     * @param tFileNameNoExt is the fileName without dir or extension (used as basis for temp files).
     *     A random number will be added to it for safety.
     */
    public TableWriterAllWithMetadata(String tDir, String tFileNameNoExt) {
        super(tDir, tFileNameNoExt);
    }


    /**
     * This adds the current contents of table (a chunk of data) to the columnStreams.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., open the columnStreams).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * <p>The table should have missing values stored as destinationMissingValues
     * or destinationFillValues.
     * This implementation doesn't change them.
     *
     * @param table  with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
        if (table.nRows() == 0) 
            return;
        super.writeSome(table);
        gatherMetadata(table);
    }

    
    /**
     * This is called to close the column streams and clean up the metadata.
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        //close the column streams
        super.finish();
        finishMetadata();
    }



    /** This gathers metadata from a partial or cumulativeTable. */
    private void gatherMetadata(Table table) {
        if (table.nRows() == 0)
            return;

        int nColumns = table.nColumns();
        boolean firstTime = columnMinValue == null;
        if (firstTime) {
            columnMaxStringLength = new int[nColumns]; //initially 0's
            columnMinValue = new double[nColumns]; //initially 0's
            columnMaxValue = new double[nColumns]; //initially 0's
            Arrays.fill(columnMinValue, Double.NaN);
            Arrays.fill(columnMaxValue, Double.NaN);
        }

        for (int col = 0; col < nColumns; col++) {
            PrimitiveArray pa = table.getColumn(col);
            if (pa instanceof StringArray)
                columnMaxStringLength[col] = Math.max(columnMaxStringLength[col], ((StringArray)pa).maxStringLength());

            //update columnMinValue and columnMaxValue
            if (!(pa instanceof StringArray)) {
                //deal with missingValues
                double destinationMV = columnAttributes[col].getDouble("missing_value");
                double destinationFV = columnAttributes[col].getDouble("_FillValue");
                double tMin = Double.MAX_VALUE, tMax = -Double.MAX_VALUE;
                if (Double.isNaN(destinationMV) && Double.isNaN(destinationFV)) {
                    double stats[] = pa.calculateStats();
                    if (stats[PrimitiveArray.STATS_N] > 0) {
                        tMin = stats[PrimitiveArray.STATS_MIN];
                        tMax = stats[PrimitiveArray.STATS_MAX];
                    }
                } else {
                    //don't use calculateStats because need to utilize 
                    //destinationMissingValue and/or destinationFillValue
                    int n = pa.size();                
                    for (int i = 0; i < n; i++) {
                        double td = pa.getNiceDouble(i);
                        if (Double.isNaN(td) ||
                            Math2.almostEqual(5, destinationMV, td) ||
                            Math2.almostEqual(5, destinationFV, td)) 
                            continue;
                        //String2.log("TableWriterAll " + destinationMV + " " + destinationFV + " td=" + td);
                        tMin = Math.min(tMin, td);
                        tMax = Math.max(tMax, td);
                    }
                }
                if (tMin != Double.MAX_VALUE) {
                    //there were some valid values
                    if (Double.isNaN(columnMinValue[col])) {
                        columnMinValue[col] = tMin;
                        columnMaxValue[col] = tMax;
                    } else {
                        columnMinValue[col] = Math.min(columnMinValue[col], tMin);
                        columnMaxValue[col] = Math.max(columnMaxValue[col], tMax);
                    }
                }
            }
        }
    }

    /** This takes the gathered metadata and makes changes to the stored global and column metadata */
    private void finishMetadata() {
        //check for MustBe.THERE_IS_NO_DATA
        if (columnMinValue == null)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

        int lonCol   = String2.indexOf(columnNames, EDV.LON_NAME);
        int latCol   = String2.indexOf(columnNames, EDV.LAT_NAME);
        int altCol   = String2.indexOf(columnNames, EDV.ALT_NAME);
        int depthCol = String2.indexOf(columnNames, EDV.DEPTH_NAME);
        int timeCol  = String2.indexOf(columnNames, EDV.TIME_NAME);
        for (int col = 0; col < columnNames.length; col++) {

            //*** FIX UP THE FILE-SPECIFIC METADATA
            //setActualRangeAndBoundingBox  (see comments in method javadocs above)
            //if no data, don't specify range
            double dMin = columnMinValue[col];
            double dMax = columnMaxValue[col];
            //actual_range is type-specific
            PrimitiveArray minMax = PrimitiveArray.factory(columnTypes[col], 2, false);
            minMax.addDouble(dMin);
            minMax.addDouble(dMax);

            if (Double.isNaN(dMin)) 
                 columnAttributes(col).remove("actual_range");
            else columnAttributes(col).set("actual_range", minMax);

            //set acdd-style and google-style bounding box
            float fMin = Math2.doubleToFloatNaN(dMin);
            float fMax = Math2.doubleToFloatNaN(dMax);
            int   iMin = Math2.roundToInt(dMin);
            int   iMax = Math2.roundToInt(dMax);
            if (col == lonCol) {
                if (Double.isNaN(dMin)) {
                    globalAttributes.remove("geospatial_lon_min");
                    globalAttributes.remove("geospatial_lon_max");
                    globalAttributes.remove("Westernmost_Easting");
                    globalAttributes.remove("Easternmost_Easting");
                } else if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_lon_min",  fMin);
                    globalAttributes.set("geospatial_lon_max",  fMax);
                    globalAttributes.set("Westernmost_Easting", fMin);
                    globalAttributes.set("Easternmost_Easting", fMax);
                } else {
                    globalAttributes.set("geospatial_lon_min",  dMin);
                    globalAttributes.set("geospatial_lon_max",  dMax);
                    globalAttributes.set("Westernmost_Easting", dMin);
                    globalAttributes.set("Easternmost_Easting", dMax);
                }
            } else if (col == latCol) {
                if (Double.isNaN(dMin)) {
                    globalAttributes.remove("geospatial_lat_min");
                    globalAttributes.remove("geospatial_lat_max");
                    globalAttributes.remove("Southernmost_Northing");
                    globalAttributes.remove("Northernmost_Northing");
                } else if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_lat_min", fMin);
                    globalAttributes.set("geospatial_lat_max", fMax);
                    globalAttributes.set("Southernmost_Northing", fMin);
                    globalAttributes.set("Northernmost_Northing", fMax);
                } else {
                    globalAttributes.set("geospatial_lat_min", dMin);
                    globalAttributes.set("geospatial_lat_max", dMax);
                    globalAttributes.set("Southernmost_Northing", dMin);
                    globalAttributes.set("Northernmost_Northing", dMax);
                } 
            } else if (col == altCol || col == depthCol) {
                if (Double.isNaN(dMin)) {
                    globalAttributes.remove("geospatial_vertical_min"); //unidata-related
                    globalAttributes.remove("geospatial_vertical_max");
                } else if (minMax instanceof FloatArray) {
                    globalAttributes.set("geospatial_vertical_min", fMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", fMax);
                } else if (minMax instanceof IntArray ||
                           minMax instanceof ShortArray) {
                    globalAttributes.set("geospatial_vertical_min", iMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", iMax);
                } else {
                    globalAttributes.set("geospatial_vertical_min", dMin); //unidata-related
                    globalAttributes.set("geospatial_vertical_max", dMax);
                }
            } else if (col == timeCol) {
                if (Double.isNaN(dMin)) {
                    globalAttributes.remove("time_coverage_start");   //unidata-related
                    globalAttributes.remove("time_coverage_end");
                } else {  //always iso string
                    String tp = columnAttributes(col).getString(EDV.TIME_PRECISION);
                    //"" unsets the attribute if min or max isNaN
                    globalAttributes.set("time_coverage_start", 
                        Calendar2.epochSecondsToLimitedIsoStringT(tp, dMin, ""));
                    //for tables (not grids) will be NaN for 'present'.   Deal with this better???
                    globalAttributes.set("time_coverage_end", 
                        Calendar2.epochSecondsToLimitedIsoStringT(tp, dMax, ""));
                }
            }
        }
    }

    /**
     * This returns the maxStringLength of a StringArray column (or 0 if not a StringArray).
     * Call this after finish() or writeAllAndFinish() as part of getting the results.
     *
     * @param col   0..
     * @return the maxStringLength of a StringArray column (or 0 if not a StringArray).
     */
    public int columnMaxStringLength(int col) {return columnMaxStringLength[col];}

    /**
     * This returns the minimum value in a column (or NaN if no values or a StringArray).
     * Call this after finish() or writeAllAndFinish() as part of getting the results.
     *
     * @param col   0..
     * @return the minimum value in a column (or NaN if no values or a StringArray).
     */
    public double columnMinValue(int col) {return columnMinValue[col];}

    /**
     * This returns the maximum value in a column (or NaN if no values or a StringArray).
     * Call this after finish() or writeAllAndFinish() as part of getting the results.
     *
     * @param col   0..
     * @return the maximum value in a column (or NaN if no values or a StringArray).
     */
    public double columnMaxValue(int col) {return columnMaxValue[col];}



}



