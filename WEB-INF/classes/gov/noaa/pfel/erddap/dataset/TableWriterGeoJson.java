/* 
 * TableWriterGeoJson Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.util.Calendar2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;

import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * TableWriterGeoJson provides a way to write a longitude,latitude,otherColumns 
 * table to a GeoJSON (http://wiki.geojson.org/Main_Page and specifically
 * http://wiki.geojson.org/GeoJSON_draft_version_5)
 * outputStream in chunks so that the whole table doesn't have to be in memory 
 * at one time.
 * If the results table is just longitude and latitude,
 * the geoJson is a MultiPoint object; otherwise it is a FeatureCollection.
 * This is used by EDDTable.
 * The outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (bob.simons@noaa.gov) 2008-03-04
 */
public class TableWriterGeoJson extends TableWriter {

    //set by constructor
    protected String jsonp;

    //set by firstTime
    protected int lonColumn = -1, latColumn = -1, altColumn = -1;
    protected boolean isString[];
    protected boolean isTimeStamp[];
    protected String time_precision[];
    protected BufferedWriter writer;
    protected double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
    protected double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
    protected double minAlt = Double.MAX_VALUE, maxAlt = -Double.MAX_VALUE;

    //other
    protected boolean rowsWritten = false;

    public long totalNRows = 0;

    /**
     * The constructor.
     *
     * @param tOutputStreamSource  the source of an outputStream that receives the 
     *     results, usually already buffered.
     *     The ouputStream is not procured until there is data to be written.
     * @param tJsonp the not-percent-encoded jsonp functionName to be prepended to the results (or null if none).
     *     See https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/
     *     and http://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/
     *     and http://www.insideria.com/2009/03/what-in-the-heck-is-jsonp-and.html .
     *     A SimpleException will be thrown if tJsonp is not null but isn't String2.isVariableNameSafe.
     */
    public TableWriterGeoJson(EDD tEdd, String tNewHistory, 
        OutputStreamSource tOutputStreamSource, String tJsonp) {

        super(tEdd, tNewHistory, tOutputStreamSource);
        jsonp = tJsonp;
        if (jsonp != null && !String2.isJsonpNameSafe(jsonp))
            throw new SimpleException(EDStatic.errorJsonpFunctionName);
    }


    /**
     * This adds the current contents of table (a chunk of data) to the OutputStream.
     * This calls ensureCompatible each time it is called.
     * If this is the first time this is called, this does first time things
     *   (e.g., call OutputStreamSource.outputStream() and write file header).
     * The number of columns, the column names, and the types of columns 
     *   must be the same each time this is called.
     *
     * <p>The table should have missing values stored as destinationMissingValues
     * or destinationFillValues.
     * This implementation converts them to NaNs and stores them as nulls.
     *
     * @param table with destinationValues
     * @throws Throwable if trouble
     */
    public void writeSome(Table table) throws Throwable {
        if (table.nRows() == 0) 
            return;

        //ensure the table's structure is the same as before
        boolean firstTime = columnNames == null;
        ensureCompatible(table);

        //do firstTime stuff
        int nColumns = table.nColumns();
        if (firstTime) {
            lonColumn = table.findColumnNumber(EDV.LON_NAME);
            latColumn = table.findColumnNumber(EDV.LAT_NAME);
            if (lonColumn < 0 || latColumn < 0) 
                throw new SimpleException("Error: " +
                    "Requests for GeoJSON data must include the longitude and latitude variables.");
            //it is unclear to me if specification supports altitude in coordinates info...
            altColumn = -1; //table.findColumnNumber(EDV.ALT_NAME); 
            isTimeStamp = new boolean[nColumns];
            time_precision = new String[nColumns];
            for (int col = 0; col < nColumns; col++) {
                Attributes catts = table.columnAttributes(col);
                String u = catts.getString("units");
                isTimeStamp[col] = u != null && 
                    (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
                if (isTimeStamp[col]) {
                    //just keep time_precision if it includes fractional seconds 
                    String tp = catts.getString(EDV.TIME_PRECISION);
                    if (tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) 
                        tp = null; //default
                    time_precision[col] = tp;
                }
            }

            //write the header
            writer = new BufferedWriter(new OutputStreamWriter(
                outputStreamSource.outputStream("UTF-8"), "UTF-8"));
            if (jsonp != null) 
                writer.write(jsonp + "(");

            //create the containing object
            isString = new boolean[nColumns];
            if (nColumns == 2) {
                //write as MultiPoint
                writer.write(
                    "{\n" +
                    "  \"type\": \"MultiPoint\",\n" +
                    "  \"coordinates\": [\n" + 
                    "\n");
            } else {
                //write as FeatureCollection
                writer.write(
                    "{\n" +
                    "  \"type\": \"FeatureCollection\",\n" +
                    "  \"propertyNames\": [");
                boolean somethingWritten = false;
                for (int col = 0; col < nColumns; col++) {
                    isString[col] = table.getColumn(col).elementClass() == String.class;
                    if (col != lonColumn && col != latColumn) {
                        if (somethingWritten) writer.write(", "); else somethingWritten = true;
                        writer.write(String2.toJson(table.getColumnName(col)));
                    }
                }
                writer.write(
                    "],\n" +
                    "  \"propertyUnits\": [");
                somethingWritten = false;
                for (int col = 0; col < nColumns; col++) {
                    if (col != lonColumn && col != latColumn) {
                        if (somethingWritten) writer.write(", "); else somethingWritten = true;
                        String units = isTimeStamp[col]? "UTC" : //not seconds since...
                            table.columnAttributes(col).getString("units");
                        writer.write(String2.toJson(units));
                    }
                }
                writer.write(
                    "],\n" +
                    "  \"features\": [\n" + 
                    "\n");
            }

        }

        //*** do everyTime stuff
        convertToStandardMissingValues(table);  //NaNs; not the method in Table, so metadata is unchanged

        //avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
        int nRows = table.nRows();
        totalNRows += nRows;
        EDStatic.ensureArraySizeOkay(totalNRows, "GeoJson");

        //write the data
        if (nColumns == 2) {
            /* if nColumns = 2, just write points in a multipoint    example:
            {
              "type": "MultiPoint",
              "coordinates": [
            [100.0, 0.0], 
            [101.0, 1.0]
              ]
            }
            */
            for (int row = 0; row < nRows; row++) {
                if (rowsWritten) writer.write(",\n"); //end previous row
                double tLon = table.getNiceDoubleData(lonColumn, row);
                double tLat = table.getNiceDoubleData(latColumn, row);
                if (!Double.isNaN(tLon) && !Double.isNaN(tLat)) {
                    //if no lon or lat, don't write the row
                    //on the theory that data without lon or lat is invalid in wfs client...
                    minLon = Math.min(minLon, tLon);
                    minLat = Math.min(minLat, tLat);
                    maxLon = Math.max(maxLon, tLon);
                    maxLat = Math.max(maxLat, tLat);                  
                    writer.write("[" + tLon + ", " + tLat + "]"); //no comma or new line
                    rowsWritten = true;
                }
            }       

        } else {
            /*write as Feature in featureCollection    example:
            {"type": "Feature",
              "id": "id0",         seems to be optional
              "geometry": {
                "type": "Point",
                "coordinates": [100.0, 0.0] },
              "properties": {
                "prop0": "value0",
                "prop1": "value1" }
            },
            */
            for (int row = 0; row < nRows; row++) {
                double tLon = table.getNiceDoubleData(lonColumn, row);
                double tLat = table.getNiceDoubleData(latColumn, row);
                if (!Double.isNaN(tLon) && !Double.isNaN(tLat)) {
                    //if no lon or lat, don't write the row
                    //on the theory that data without lon or lat is invalid in wfs client...
                    minLon = Math.min(minLon, tLon);
                    minLat = Math.min(minLat, tLat);
                    maxLon = Math.max(maxLon, tLon);
                    maxLat = Math.max(maxLat, tLat);                  
                    if (rowsWritten) writer.write(",\n"); //end previous row
                    writer.write(
                        "{\"type\": \"Feature\",\n" + //begin feature
                            //id?  seems to be not required. I could use cumulativeRowNumber. 
                        "  \"geometry\": {\n" +
                        "    \"type\": \"Point\",\n" +
                        "    \"coordinates\": [" + tLon + ", " + tLat + "] },\n" +
                        "  \"properties\": {\n");

                    boolean colWritten = false;
                    for (int col = 0; col < nColumns; col++) {
                        if (col == lonColumn || col == latColumn) 
                            continue;
                        if (colWritten)
                            writer.write(",\n");
                        else colWritten = true;

                        String s;
                        if (isTimeStamp[col]) {
                            double d = table.getDoubleData(col, row);
                            s = Double.isNaN(d)? "null" : 
                                "\"" + Calendar2.epochSecondsToLimitedIsoStringT(
                                    time_precision[col], d, "") + "\"";
                        } else if (isString[col]) {
                            s = String2.toJson(table.getStringData(col, row));
                        } else { //numeric
                            s = table.getStringData(col, row);
                            //represent NaN as null? yes, that is what json library does
                            if (s.length() == 0)
                                s = "null"; 
                        }
                        writer.write(
                            "    " + String2.toJson(table.getColumnName(col)) + ": " + s);
                    }
                    writer.write(" }\n"); //end properties
                    writer.write("}"); //end feature   //no comma or newline
                    rowsWritten = true;
                }
            }       
        }

        //ensure it gets to user right away
        if (nRows > 1) //some callers work one row at a time; avoid excessive flushing
            writer.flush(); 

    }

    
    /**
     * This writes any end-of-file info to the stream and flushes the stream.
     * If ignoreFinish=true, nothing will be done.
     *
     * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
     */
    public void finish() throws Throwable {
        if (ignoreFinish) 
            return;

        //check for MustBe.THERE_IS_NO_DATA
        if (writer == null)
            throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

        //end of big array
        writer.write(
            "\n" + //for end line (no comma) of last coordinate or feature
            "\n" + //gap
            "  ],\n" + //end of features array
            "  \"bbox\": [" + 
                minLon + ", " + minLat + 
                (altColumn >= 0? ", " + minAlt : "") + ", " +
                maxLon + ", " + maxLat + 
                (altColumn >= 0? ", " + maxAlt : "") + "]\n" + //2009-07-31 removed ',' after ]
            "}\n"); //end of features collection
        if (jsonp != null) 
            writer.write(")");
        writer.flush(); //essential

        //diagnostic
        if (verbose)
            String2.log("TableWriterGeoJson done. TIME=" + 
                (System.currentTimeMillis() - time) + "\n");

    }


    
    /**
     * This is a convenience method to write an entire table in one step.
     *
     * @throws Throwable if trouble  (no columns is trouble; no rows is not trouble)
     */
    public static void writeAllAndFinish(EDD tEdd, String tNewHistory, Table table, 
        OutputStreamSource outputStreamSource, String tJsonp) throws Throwable {

        TableWriterGeoJson twgj = new TableWriterGeoJson(tEdd, tNewHistory, 
            outputStreamSource, tJsonp);
        twgj.writeAllAndFinish(table);
    }

}



