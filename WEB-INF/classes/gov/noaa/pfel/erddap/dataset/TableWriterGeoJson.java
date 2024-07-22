/*
 * TableWriterGeoJson Copyright 2007, NOAA.
 * See the LICENSE.txt file in this file's directory.
 */
package gov.noaa.pfel.erddap.dataset;

import com.cohort.array.Attributes;
import com.cohort.array.PAType;
import com.cohort.util.Calendar2;
import com.cohort.util.File2;
import com.cohort.util.Math2;
import com.cohort.util.MustBe;
import com.cohort.util.SimpleException;
import com.cohort.util.String2;
import gov.noaa.pfel.coastwatch.pointdata.Table;
import gov.noaa.pfel.erddap.util.EDStatic;
import gov.noaa.pfel.erddap.variable.EDV;
import java.io.BufferedWriter;

/**
 * TableWriterGeoJson provides a way to write a longitude,latitude,otherColumns table to a GeoJSON
 * (http://wiki.geojson.org/Main_Page and specifically
 * http://wiki.geojson.org/GeoJSON_draft_version_5 Ratified in Aug 2016:
 * https://tools.ietf.org/html/rfc7946 outputStream in chunks so that the whole table doesn't have
 * to be in memory at one time. If the results table is just longitude and latitude, the geoJson is
 * a MultiPoint object; otherwise it is a FeatureCollection. This is used by EDDTable. The
 * outputStream isn't obtained until the first call to writeSome().
 *
 * @author Bob Simons (was bob.simons@noaa.gov, now BobSimons2.00@gmail.com) 2008-03-04
 */
public class TableWriterGeoJson extends TableWriter {

  // set by constructor
  protected String jsonp;

  // set by firstTime
  protected int lonColumn = -1, latColumn = -1, altColumn = -1;
  protected boolean isChar[];
  protected boolean isString[];
  protected boolean isTimeStamp[];
  protected String time_precision[];
  protected BufferedWriter writer;
  protected double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
  protected double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
  protected double minAlt = Double.MAX_VALUE, maxAlt = -Double.MAX_VALUE;

  // other
  protected boolean rowsWritten = false;

  public long totalNRows = 0;

  /**
   * The constructor.
   *
   * @param tLanguage the index of the selected language
   * @param tOutputStreamSource the source of an outputStream that receives the results, usually
   *     already buffered. The ouputStream is not procured until there is data to be written.
   * @param tJsonp the not-percent-encoded jsonp functionName to be prepended to the results (or
   *     null if none). See https://niryariv.wordpress.com/2009/05/05/jsonp-quickly/ and
   *     https://bob.pythonmac.org/archives/2005/12/05/remote-json-jsonp/ and
   *     https://www.raymondcamden.com/2014/03/12/Reprint-What-in-the-heck-is-JSONP-and-why-would-you-use-it/
   *     . A SimpleException will be thrown if tJsonp is not null but isn't
   *     String2.isVariableNameSafe.
   */
  public TableWriterGeoJson(
      int tLanguage,
      EDD tEdd,
      String tNewHistory,
      OutputStreamSource tOutputStreamSource,
      String tJsonp) {

    super(tLanguage, tEdd, tNewHistory, tOutputStreamSource);
    jsonp = tJsonp;
    if (jsonp != null && !String2.isJsonpNameSafe(jsonp))
      throw new SimpleException(
          EDStatic.bilingual(
              language,
              EDStatic.queryErrorAr[0] + EDStatic.errorJsonpFunctionNameAr[0],
              EDStatic.queryErrorAr[language] + EDStatic.errorJsonpFunctionNameAr[language]));
  }

  /**
   * This adds the current contents of table (a chunk of data) to the OutputStream. This calls
   * ensureCompatible each time it is called. If this is the first time this is called, this does
   * first time things (e.g., call OutputStreamSource.outputStream() and write file header). The
   * number of columns, the column names, and the types of columns must be the same each time this
   * is called.
   *
   * @param table with destinationValues. The table should have missing values stored as
   *     destinationMissingValues or destinationFillValues. This implementation converts them to
   *     NaNs and stores them as nulls. If I go to https://jsonlint.com/ and enter [1, 2.0, 1e30],
   *     it says it is valid. If I enter [1, 2.0, NaN, 1e30], it says NaN is not valid.
   * @throws Throwable if trouble
   */
  @Override
  public void writeSome(Table table) throws Throwable {
    if (table.nRows() == 0) return;

    // ensure the table's structure is the same as before
    boolean firstTime = columnNames == null;
    ensureCompatible(table);

    // do firstTime stuff
    int nColumns = table.nColumns();
    if (firstTime) {
      lonColumn = table.findColumnNumber(EDV.LON_NAME);
      latColumn = table.findColumnNumber(EDV.LAT_NAME);
      altColumn = table.findColumnNumber(EDV.ALT_NAME);
      if (lonColumn < 0 || latColumn < 0)
        throw new SimpleException(
            EDStatic.bilingual(
                language,
                EDStatic.queryErrorAr[0]
                    + "Requests for GeoJSON data must include the longitude and latitude variables.",
                EDStatic.queryErrorAr[language]
                    + "Requests for GeoJSON data must include the longitude and latitude variables."));
      // it is unclear to me if specification supports altitude in coordinates info...
      isTimeStamp = new boolean[nColumns];
      time_precision = new String[nColumns];
      for (int col = 0; col < nColumns; col++) {
        Attributes catts = table.columnAttributes(col);
        String u = catts.getString("units");
        isTimeStamp[col] = u != null && (u.equals(EDV.TIME_UNITS) || u.equals(EDV.TIME_UCUM_UNITS));
        if (isTimeStamp[col]) {
          // just keep time_precision if it includes fractional seconds
          String tp = catts.getString(EDV.TIME_PRECISION);
          if (tp != null && !tp.startsWith("1970-01-01T00:00:00.0")) tp = null; // default
          time_precision[col] = tp;
        }
      }

      // write the header
      writer = File2.getBufferedWriterUtf8(outputStreamSource.outputStream(File2.UTF_8));
      if (jsonp != null) writer.write(jsonp + "(");

      // create the containing object
      isChar = new boolean[nColumns];
      isString = new boolean[nColumns];
      if (nColumns == 2 || (nColumns == 3 && altColumn >= 0)) {
        // write as MultiPoint
        writer.write("{\n" + "  \"type\": \"MultiPoint\",\n" + "  \"coordinates\": [\n");
      } else {
        // write as FeatureCollection
        writer.write("{\n" + "  \"type\": \"FeatureCollection\",\n" + "  \"propertyNames\": [");
        boolean somethingWritten = false;
        for (int col = 0; col < nColumns; col++) {
          PAType cPAType = table.getColumn(col).elementType();
          isChar[col] = cPAType == PAType.CHAR;
          isString[col] = cPAType == PAType.STRING;
          if (col != lonColumn && col != latColumn && col != altColumn) {
            if (somethingWritten) writer.write(", ");
            else somethingWritten = true;
            writer.write(String2.toJson(table.getColumnName(col)));
          }
        }
        writer.write("],\n" + "  \"propertyUnits\": [");
        somethingWritten = false;
        for (int col = 0; col < nColumns; col++) {
          if (col != lonColumn && col != latColumn && col != altColumn) {
            if (somethingWritten) writer.write(", ");
            else somethingWritten = true;
            String units =
                isTimeStamp[col]
                    ? "UTC"
                    : // not seconds since...
                    table.columnAttributes(col).getString("units");
            writer.write(String2.toJson(units));
          }
        }
        writer.write("],\n" + "  \"features\": [\n");
      }
    }

    // *** do everyTime stuff
    table.convertToStandardMissingValues(); // to NaNs

    // avoid writing more data than can be reasonable processed (Integer.MAX_VALUES rows)
    int nRows = table.nRows();
    boolean flushAfterward = totalNRows == 0; // flush initial chunk so info gets to user quickly
    totalNRows += nRows;
    Math2.ensureArraySizeOkay(totalNRows, "GeoJson");

    // write the data
    if (nColumns == 2 || (nColumns == 3 && altColumn >= 0)) {
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
        double tLon = table.getNiceDoubleData(lonColumn, row);
        double tLat = table.getNiceDoubleData(latColumn, row);
        double tAlt = (altColumn >= 0 ? table.getNiceDoubleData(altColumn, row) : Double.NaN);
        if (Double.isNaN(tLon) || Double.isNaN(tLat)) continue;
        minLon = Math.min(minLon, tLon);
        maxLon = Math.max(maxLon, tLon);
        minLat = Math.min(minLat, tLat);
        maxLat = Math.max(maxLat, tLat);
        if (!Double.isNaN(tAlt)) {
          minAlt = Math.min(minAlt, tAlt);
          maxAlt = Math.max(maxAlt, tAlt);
        }
        writer.write(
            (rowsWritten ? ",\n" : "")
                + // end previous row
                "["
                + tLon
                + ", "
                + tLat
                + (Double.isNaN(tAlt) ? "" : ", " + tAlt)
                + "]"); // no comma or new line
        rowsWritten = true;
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
        double tAlt = (altColumn >= 0 ? table.getNiceDoubleData(altColumn, row) : Double.NaN);
        if (Double.isNaN(tLon) || Double.isNaN(tLat)) continue;
        minLon = Math.min(minLon, tLon);
        maxLon = Math.max(maxLon, tLon);
        minLat = Math.min(minLat, tLat);
        maxLat = Math.max(maxLat, tLat);
        if (!Double.isNaN(tAlt)) {
          minAlt = Math.min(minAlt, tAlt);
          maxAlt = Math.max(maxAlt, tAlt);
        }

        writer.write(
            (rowsWritten ? ",\n" : "")
                + // end previous row
                "{\"type\": \"Feature\",\n"
                + // begin feature
                // id?  seems to be not required. I could use cumulativeRowNumber.
                "  \"geometry\": {\n"
                + "    \"type\": \"Point\",\n"
                + "    \"coordinates\": ["
                + tLon
                + ", "
                + tLat
                + (Double.isNaN(tAlt) ? "" : ", " + tAlt)
                + "] },\n"
                + "  \"properties\": {\n");

        boolean colWritten = false;
        for (int col = 0; col < nColumns; col++) {
          if (col == lonColumn || col == latColumn || col == altColumn) continue;
          if (colWritten) writer.write(",\n");
          else colWritten = true;

          String s;
          if (isTimeStamp[col]) {
            double d = table.getDoubleData(col, row);
            s =
                Double.isNaN(d)
                    ? "null"
                    : "\""
                        + Calendar2.epochSecondsToLimitedIsoStringT(time_precision[col], d, "")
                        + "\"";
          } else if (isChar[col] || isString[col]) {
            s = String2.toJson(table.getStringData(col, row));
          } else { // numeric
            s = table.getStringData(col, row);
            // represent NaN as null? yes, that is what json library does
            // If I go to https://jsonlint.com/ and enter [1, 2.0, 1e30], it says it is valid.
            // If I enter [1, 2.0, NaN, 1e30], it says NaN is not valid.
            if (s.length() == 0) s = "null";
          }
          writer.write("    " + String2.toJson(table.getColumnName(col)) + ": " + s);
        }
        writer.write(" }\n"); // end properties
        writer.write("}"); // end feature   //no comma or newline
        rowsWritten = true;
      }
    }

    if (flushAfterward) writer.flush();
  }

  /**
   * This writes any end-of-file info to the stream and flushes the stream. If ignoreFinish=true,
   * nothing will be done.
   *
   * @throws Throwable if trouble (e.g., MustBe.THERE_IS_NO_DATA if there is no data)
   */
  @Override
  public void finish() throws Throwable {
    if (ignoreFinish) return;

    // check for MustBe.THERE_IS_NO_DATA
    if (writer == null || !rowsWritten)
      throw new SimpleException(MustBe.THERE_IS_NO_DATA + " (nRows = 0)");

    // end of big array
    // if rowsWritten, then min/max/Lat/Lon should be finite
    boolean writeAlt = altColumn >= 0 && minAlt != Double.MAX_VALUE && maxAlt != -Double.MAX_VALUE;
    writer.write(
        "\n"
            + // for end line (no comma) of last coordinate or feature
            "  ],\n"
            + // end of features array
            "  \"bbox\": ["
            + (minLon == Double.MAX_VALUE ? "null" : minLon)
            + ", "
            + (minLat == Double.MAX_VALUE ? "null" : minLat)
            + (writeAlt ? ", " + minAlt : "")
            + ", "
            + (maxLon == -Double.MAX_VALUE ? "null" : maxLon)
            + ", "
            + (maxLat == -Double.MAX_VALUE ? "null" : maxLat)
            + (writeAlt ? ", " + maxAlt : "")
            + "]\n"
            + "}\n"); // end of features collection
    if (jsonp != null) writer.write(")");
    writer.flush(); // essential

    // diagnostic
    if (verbose)
      String2.log("TableWriterGeoJson done. TIME=" + (System.currentTimeMillis() - time) + "ms\n");
  }

  /**
   * This is a convenience method to write an entire table in one step.
   *
   * @throws Throwable if trouble (no columns is trouble; no rows is not trouble)
   */
  public static void writeAllAndFinish(
      int language,
      EDD tEdd,
      String tNewHistory,
      Table table,
      OutputStreamSource outputStreamSource,
      String tJsonp)
      throws Throwable {

    TableWriterGeoJson twgj =
        new TableWriterGeoJson(language, tEdd, tNewHistory, outputStreamSource, tJsonp);
    twgj.writeAllAndFinish(table);
  }
}
